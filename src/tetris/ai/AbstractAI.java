package tetris.ai;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.logging.Logger;
import tetris.generic.BlockPosition;
import tetris.generic.TetrisEngine;
import tetris.generic.TetrisEngine.GameState;
import tetris.generic.Tetromino;

public abstract class AbstractAI {
    private static final Logger logger = Logger.getLogger(AbstractAI.class.getName());

    protected static void movehere(TetrisEngine engine, BlockPosition position) {
        Objects.requireNonNull(engine);
        Objects.requireNonNull(position);

        /* we're going to make another failsafe here: if at any time we rotate
           it or move it and it doesn't move then it's stuck and we give up. */
        Tetromino activeblock = engine.getActiveblock();
        int init_state = activeblock.rot;
        // Rotate first so we don't get stuck in the edges.
        for (; activeblock.rot != position.rot; activeblock = engine.getActiveblock()) {
            // Now check if it worked
            if (!engine.keyrotate() || init_state == engine.getActiveblock().rot) {
                logger.warning(String.format("could not rotate active block to rot=%d", position.rot));
                engine.keyslam();
                return;
            }
        }
        
        for (; activeblock.x != position.bx; activeblock = engine.getActiveblock()) {
            if (activeblock.x < position.bx ? !engine.keyright() : !engine.keyleft()) {
                logger.warning(String.format("could not move active block to x=%d", position.bx));
                engine.keyslam();
                return;
            }
        }
        engine.keyslam();
    }
    protected final ListeningExecutorService executor;

    public AbstractAI(ListeningExecutorService executor) {
        this.executor = executor;
    }

    protected abstract ListenableFuture<BlockPosition> computeBestFit(TetrisEngine engine);
        
    public ListenableFuture<Void> process(final TetrisEngine engine) {
        if (engine.getState() != GameState.PLAYING) {
            return Futures.immediateFuture(null);
        }
        return Futures.transform(this.computeBestFit(engine), new Function<BlockPosition, Void>() {
            @Override
            public Void apply(BlockPosition temp) {
                movehere(engine, temp);
                return null;
            }
        });
    }
    
    public ListenableFuture<Iterator<Void>> processIterator(final TetrisEngine engine) {
        if (engine.getState() != GameState.PLAYING) {
            return Futures.immediateFuture(null);
        }
        return Futures.transform(this.computeBestFit(engine), new Function<BlockPosition, Iterator<Void>>() {
            @Override
            public Iterator<Void> apply(BlockPosition temp) {
                return new MoveHere(engine, temp);
            }
        });
    }

    private static class MoveHere implements Iterator<Void> {
        private final int initialRotation;
        private final BlockPosition position;
        private final TetrisEngine engine;
        private boolean positionEnd;
        private boolean end;

        MoveHere(TetrisEngine engine, BlockPosition position) {
            Objects.requireNonNull(engine);
            Objects.requireNonNull(position);
            this.engine = engine;
            this.position = position;
            this.positionEnd = false;
            this.end = false;
            
            /* we're going to make another failsafe here: if at any time we rotate
               it or move it and it doesn't move then it's stuck and we give up. */
            this.initialRotation = engine.getActiveblock().rot;
        }
        
        @Override
        public boolean hasNext() {
            return !this.end;
        }

        @Override
        public Void next() {
            if (this.end) {
                throw new NoSuchElementException();
            } else if (this.positionEnd) {
                if (!engine.keydown()) {
                    this.end = true;
                }
            } else {
                positionTheBlock();
            }
            return null;
        }
        
        private void positionTheBlock() {
            Tetromino activeblock = engine.getActiveblock();
            // Rotate first so we don't get stuck in the edges.
            if (activeblock.rot != position.rot) {
                // Check if it worked
                if (!engine.keyrotate() || initialRotation == engine.getActiveblock().rot) {
                    this.end = true;
                    logger.warning(String.format("could not rotate active block to rot=%d", position.rot));
                    engine.keyslam();
                }
                return;
            }
            
            if (activeblock.x != position.bx) {
                // Check if it worked
                if (activeblock.x < position.bx ? !engine.keyright() : !engine.keyleft()) {
                    this.end = true;
                    logger.warning(String.format("could not move active block to x=%d", position.bx));
                    engine.keyslam();
                }
                return;
            }
            this.positionEnd = true;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Remove is not supported.");
        }
    }

}
