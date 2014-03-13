package tetris.ai;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.util.Objects;
import java.util.logging.Logger;
import tetris.ProjectConstants.GameState;
import tetris.generic.TetrisEngine;
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
}
