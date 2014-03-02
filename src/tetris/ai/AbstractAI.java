package tetris.ai;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.util.ArrayList;
import java.util.List;
import tetris.ProjectConstants.GameState;
import tetris.generic.TetrisEngine;
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

    // Takes a int array and calculates how many blocks of free spaces are there
    // on the left and right. The return value is a 2 digit integer.
    protected static int freeSpaces(byte[][] in) {
        // It's free if all of them are zero, and their sum is zero.
        boolean c1free = in[0][0] + in[1][0] + in[2][0] + in[3][0] == 0;
        boolean c2free = in[0][1] + in[1][1] + in[2][1] + in[3][1] == 0;
        boolean c3free = in[0][2] + in[1][2] + in[2][2] + in[3][2] == 0;
        boolean c4free = in[0][3] + in[1][3] + in[2][3] + in[3][3] == 0;

        int lfree = 0;
        // Meh, I'm too lazy to code a loop for this.
        if (c1free) {
            lfree++;
            if (c2free) {
                lfree++;
                if (c3free) {
                    lfree++;
                    if (c4free) {
                        lfree++;
                    }
                }
            }
        }

        int rfree = 0;
        if (c4free) {
            rfree++;
            if (c3free) {
                rfree++;
                if (c2free) {
                    rfree++;
                    if (c1free) {
                        rfree++;
                    }
                }
            }
        }

        return lfree * 10 + rfree;
    }

    // List of all the possible fits.
    protected static List<BlockPosition> getPossibleFits(tetris.generic.TetrisEngine ge, Tetromino.Type type) {
        byte[][][] rotations = TetrisEngine.blockdef[type.ordinal()];
        int nrots = rotations.length;

        List<BlockPosition> posfits = new ArrayList<>();

        int free, freeL, freeR, minX, maxX;
        for (int i = 0; i < nrots; i++) {
            free = freeSpaces(rotations[i]);
            freeL = free / 10;
            freeR = free % 10;
            minX = 0 - freeL;
            maxX = (ge.WIDTH - 4) + freeR;
            // now loop through each position for a rotation.
            for (int j = minX; j <= maxX; j++) {
                BlockPosition put = new BlockPosition();
                put.bx = (byte) j;
                put.rot = (byte) i;
                posfits.add(put);
            }
        }

        return posfits;
    }
}
