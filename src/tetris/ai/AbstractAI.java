package tetris.ai;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import tetris.ProjectConstants.GameState;
import tetris.generic.TetrisEngine;
import tetris.generic.TetrisGameDefinitions;
import tetris.generic.TetrisGameDefinitions.FreeSpaces;
import tetris.generic.Tetromino;

public abstract class AbstractAI {
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
                if (temp != null) {
                    int elx = temp.bx;
                    int erot = temp.rot;
                    movehere(engine, elx, erot);
                }
                return null;
            }
        });
    }

    protected static void movehere(TetrisEngine engine, int finx, int finrot) {
        // we're going to make another failsafe here: if at any time we rotate it
        // or move it and it doesn't move then it's stuck and we give up.
        int init_state = engine.getActiveblock().rot;
        int prev_state = init_state;
        while (engine.getActiveblock().rot != finrot) {
            // Rotate first so we don't get stuck in the edges.
            engine.keyrotate();
            // Now wait.
            if (prev_state == engine.getActiveblock().rot || init_state == engine.getActiveblock().rot) {
                engine.keyslam();
                return;
            } else {
                prev_state = engine.getActiveblock().rot;

            }
        }
        prev_state = engine.getActiveblock().x;
        while (engine.getActiveblock().x != finx) {
            //Now nudge the block.
            if (engine.getActiveblock().x < finx) {
                engine.keyright();
            } else if (engine.getActiveblock().x > finx) {
                engine.keyleft();
            }
            if (prev_state == engine.getActiveblock().x) {
                engine.keyslam();
                return;
            } else {
                prev_state = engine.getActiveblock().x;
            }
        }
        engine.keyslam();
    }
}
