package tetris.ai;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.util.Objects;
import tetris.ProjectConstants.GameState;
import tetris.generic.TetrisEngine;
import tetris.generic.Tetromino;

public abstract class AbstractAI {

    protected static void movehere(TetrisEngine engine, BlockPosition position) {
        Objects.requireNonNull(engine);
        Objects.requireNonNull(position);

        /* we're going to make another failsafe here: if at any time we rotate
           it or move it and it doesn't move then it's stuck and we give up. */
        Tetromino activeblock = engine.getActiveblock();
        int init_state = activeblock.rot;
        int prev_state = init_state;
        while (activeblock.rot != position.rot) {
            // Rotate first so we don't get stuck in the edges.
            engine.keyrotate();
            activeblock = engine.getActiveblock(); // refresh activeblock
            // Now check if it worked
            if (prev_state == activeblock.rot || init_state == activeblock.rot) {
                engine.keyslam();
                return;
            } else {
                prev_state = activeblock.rot;
            }
        }
        prev_state = activeblock.x;
        while (activeblock.x != position.bx) {
            // Now nudge the block.
            if (activeblock.x < position.bx) {
                engine.keyright();
            } else if (activeblock.x > position.bx) {
                engine.keyleft();
            }
            activeblock = engine.getActiveblock(); // refresh activeblock
            // Now check if it worked
            if (prev_state == activeblock.x) {
                engine.keyslam();
                return;
            } else {
                prev_state = activeblock.x;
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
