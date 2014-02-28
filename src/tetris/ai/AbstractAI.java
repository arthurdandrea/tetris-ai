package tetris.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import tetris.ProjectConstants.GameState;
import static tetris.ProjectConstants.sleep_;
import tetris.generic.TetrisEngine;
import tetris.generic.TetrisEngineListener;
import tetris.generic.Tetromino;

public abstract class AbstractAI {
    public enum State { Stoped, Working };

    protected final TetrisEngine engine;

    private boolean isWaiting;
    private Thread thread;
    private State state;


    /*
     * Time (ms) AbstractAI has to wait per keypress.
     * (for maximum speed without crashing, set waittime = 1, do_drop on)
     */
    public static final int waittime = 10;

    /*
     * Do we use hard drops?
     */
    public static final boolean do_drop = true;

    public AbstractAI(TetrisEngine engine) {
        this.isWaiting = false;
        this.engine = engine;
        this.engine.addListener(new EngineListener());
        this.thread = new Thread(new ThreadRunnable(), "AIThread");
        this.state = State.Stoped;
    }
    
    public Thread getThread() {
        return this.thread;
    }
 
    public synchronized State getState() {
        return this.state;
    }

    public synchronized void start() {
        if (this.state == State.Stoped) {
            this.state = State.Working;
            this.thread.start();
        }
    }
    
    public void stop() {
        if (this.state == State.Working) {
            synchronized(this) {
                this.state = State.Stoped;
                if (this.isWaiting) {
                    this.notify();
                }
            }
            if (Thread.currentThread() != this.thread) {
                try {
                    this.thread.join();
                } catch (InterruptedException ex) {
                    Logger.getLogger(AbstractAI.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    protected abstract BlockPosition computeBestFit(TetrisEngine engine);

    public byte[][] mockGrid(TetrisEngine ge) {
        byte[][] mockgrid = new byte[ge.WIDTH][ge.HEIGHT];
        for (int i = 0; i < ge.WIDTH; i++) {
            for (int j = 0; j < ge.HEIGHT; j++) {
                byte s = (byte) ge.blocks[i][j].getState();
                if (s == 2) {
                    s = 0;
                }
                mockgrid[i][j] = s;
            }
        }
        return mockgrid;
    }

    private class ThreadRunnable implements Runnable {
        @Override
        public void run() {
            threadMain();
        }
    }
    
    private class EngineListener implements TetrisEngineListener {
        @Override
        public void onGameStateChange(TetrisEngine engine) {
            AbstractAI.this.onGameStateChange(engine);
        }

        @Override
        public void onGameOver(TetrisEngine engine, int lastScore, int lastLines) {
        }

        @Override
        public void onNewBlock(TetrisEngine engine) {
        }
    }
    
    protected void onGameStateChange(TetrisEngine engine) {
        if (this.isWaiting) {
            synchronized (this) {
                this.notify();
            }
        }
    }
    
    protected void threadMain() {
        while (this.state == State.Working) {
            //If it's merely paused, do nothing; if it's actually game over
            //then break loop entirely.
            if (this.engine.getState() == GameState.PLAYING) {
                if (this.engine.activeblock == null) {
                    continue;
                }

                BlockPosition temp = computeBestFit(engine);
                if (this.engine.getState() == GameState.PLAYING) {
                    int elx = temp.bx;
                    int erot = temp.rot;

                    //Move it!
                    this.movehere(elx, erot);
                }
                //safety
                sleep_(waittime);
            } else {
                this.isWaiting = true;
                try {
                    synchronized(this) {
                        this.wait();
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(AbstractAI.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    this.isWaiting = false;
                }
            }
        }
    }

    protected void movehere(int finx, int finrot) {
        int st_blocksdropped = engine.blocksdropped;
        // we're going to make another failsafe here: if at any time we rotate it
        // or move it and it doesn't move then it's stuck and we give up.
        int init_state = engine.activeblock.rot;
        int prev_state = init_state;
        while (state == State.Working && engine.activeblock.rot != finrot) {
            // Rotate first so we don't get stuck in the edges.
            engine.keyrotate();
            // Now wait.
            if (prev_state == engine.activeblock.rot || init_state == engine.activeblock.rot) {
                engine.keyslam();
                return;
            } else {
                prev_state = engine.activeblock.rot;

            }
        }
        prev_state = engine.activeblock.x;
        while (state == State.Working && engine.activeblock.x != finx) {
            //Now nudge the block.
            if (engine.activeblock.x < finx) {
                engine.keyright();
            } else if (engine.activeblock.x > finx) {
                engine.keyleft();
            }
            sleep_(waittime);
            if (prev_state == engine.activeblock.x) {
                engine.keyslam();
                return;
            } else {
                prev_state = engine.activeblock.x;
            }
        }
        if (state == State.Working && do_drop) {
            engine.keyslam();
            return;
        }
        while (state == State.Working && engine.blocksdropped == st_blocksdropped) {
            // Now move it down until it drops a new block.
            engine.keydown();
        }
    }

    // Takes a int array and calculates how many blocks of free spaces are there
    // on the left and right. The return value is a 2 digit integer.
    static int freeSpaces(byte[][] in) {

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
    protected List<BlockPosition> getPossibleFits(tetris.generic.TetrisEngine ge, Tetromino.Type type) {
        byte[][][] rotations = TetrisEngine.blockdef[type.ordinal()];
        int nrots = rotations.length;

        List<BlockPosition> posfits = new ArrayList<BlockPosition>();

        for (int i = 0; i < nrots; i++) {
            byte[][] trotation = rotations[i];
            int free = freeSpaces(trotation);
            int freeL = free / 10;
            int freeR = free % 10;
            int minX = 0 - freeL;
            int maxX = (ge.WIDTH - 4) + freeR;
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
