package tetris.generic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import tetris.ProjectConstants.GameState;

/*
 * This class calculates most of the block positions, rotations, etc, although
 * the TetrisPanel object still keeps track of the concrete block coordinates.
 * This class will change variables in the TetrisPanel class.
 */
public final class TetrisEngine {
    //---------------VARIABLES--------------//
    /*
     * Bunch of hardcoded blocks and their rotations. Code them high up in the
     * array so that when you get a new one it appears in the highest spot 
     * possible.
     */
    public static final byte[][][][] blockdef = {{
    // 0 = I block.
        {
            {1, 1, 1, 1},
            {0, 0, 0, 0},
            {0, 0, 0, 0},
            {0, 0, 0, 0}},
        {
            {0, 1, 0, 0},
            {0, 1, 0, 0},
            {0, 1, 0, 0},
            {0, 1, 0, 0}}},
    // 1 = O block
    {
        {
            {0, 1, 1, 0},
            {0, 1, 1, 0},
            {0, 0, 0, 0},
            {0, 0, 0, 0}}},
    // 2 = L block
    {
        {
            {0, 1, 0, 0},
            {0, 1, 0, 0},
            {0, 1, 1, 0},
            {0, 0, 0, 0}},
        {
            {0, 0, 1, 0},
            {1, 1, 1, 0},
            {0, 0, 0, 0},
            {0, 0, 0, 0}},
        {
            {1, 1, 0, 0},
            {0, 1, 0, 0},
            {0, 1, 0, 0},
            {0, 0, 0, 0}},
        {
            {1, 1, 1, 0},
            {1, 0, 0, 0},
            {0, 0, 0, 0},
            {0, 0, 0, 0}}},
    // 3 = J block
    {
        {
            {0, 0, 1, 0},
            {0, 0, 1, 0},
            {0, 1, 1, 0},
            {0, 0, 0, 0}},
        {
            {1, 1, 1, 0},
            {0, 0, 1, 0},
            {0, 0, 0, 0},
            {0, 0, 0, 0}},
        {
            {0, 1, 1, 0},
            {0, 1, 0, 0},
            {0, 1, 0, 0},
            {0, 0, 0, 0}},
        {
            {1, 0, 0, 0},
            {1, 1, 1, 0},
            {0, 0, 0, 0},
            {0, 0, 0, 0}}},
    // 4 = T block
    {
        {
            {0, 1, 0, 0},
            {1, 1, 1, 0},
            {0, 0, 0, 0},
            {0, 0, 0, 0}},
        {
            {0, 1, 0, 0},
            {0, 1, 1, 0},
            {0, 1, 0, 0},
            {0, 0, 0, 0}},
        {
            {1, 1, 1, 0},
            {0, 1, 0, 0},
            {0, 0, 0, 0},
            {0, 0, 0, 0}},
        {
            {0, 1, 0, 0},
            {1, 1, 0, 0},
            {0, 1, 0, 0},
            {0, 0, 0, 0}}
    },
    // 5 = S block
    {
        {
            {0, 1, 1, 0},
            {1, 1, 0, 0},
            {0, 0, 0, 0},
            {0, 0, 0, 0}},
        {
            {0, 1, 0, 0},
            {0, 1, 1, 0},
            {0, 0, 1, 0},
            {0, 0, 0, 0}}
    },
    // 6 = Z block
    {
        {
            {0, 1, 1, 0},
            {0, 0, 1, 1},
            {0, 0, 0, 0},
            {0, 0, 0, 0}},
        {
            {0, 0, 1, 0},
            {0, 1, 1, 0},
            {0, 1, 0, 0},
            {0, 0, 0, 0}
        }
    }};

    private final ReadWriteLock rwLock;
    private final Random rdm;

    private Block[][] blocks;
    private Score score;
    private GameState state;
    private List<TetrisEngineListener> listeners;
    private Tetromino activeblock;
    private Tetromino nextblock;
    public final TetrisGameDefinitions defs;

    public TetrisEngine() {
        this(6, 20);
    }

    /**
     * Remember to call startengine() or else this won't do
     * anything!
     * @param width
     * @param height
     */
    public TetrisEngine(int width, int height) {
        this.defs = new TetrisGameDefinitions(width, height);
        this.rwLock = new ReentrantReadWriteLock();
        this.listeners = new ArrayList<>();
        this.rdm = new Random();
        this.blocks = new Block[this.defs.width][this.defs.height];
        this.score = new Score();
        this.reset();
    }

    /**
     * @return the current state of the game
     */
    public GameState getState() {
        return this.state;
    }

    public void setState(GameState newValue) {
        this.rwLock.writeLock().lock();
        try {
            if (this.state == GameState.GAMEOVER) {
                this.reset();
                this.step();
                this.score = new Score();
            }
            this.state = newValue;

            List<TetrisEngineListener> listenersCopy = new ArrayList<>(this.listeners);
            for (TetrisEngineListener listener : listenersCopy) {
                listener.onGameStateChange(this);
            }
        } finally {
            this.rwLock.writeLock().unlock();
        }
    }

    /**
     * @return the current score of the game
     */
    public Score getScore() {
        this.rwLock.readLock().lock();
        try {
            return score.Clone();
        } finally {
            this.rwLock.readLock().unlock();
        }

    }

    public void addListener(TetrisEngineListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(TetrisEngineListener listener) {
        this.rwLock.writeLock().lock();
        try {
            this.listeners.remove(listener);
        } finally {
            this.rwLock.writeLock().unlock();
        }
    }


    /*
     * Called when the RIGHT key is pressed.
     */
    public void keyright() {
        this.rwLock.writeLock().lock();
        try {
            if (activeblock == null || state != GameState.PLAYING) {
                return;
            }

            activeblock.x++;

            //Failsafe: Revert XPosition.
            if (!copy()) {
                activeblock.x--;
            }
        } finally {
            this.rwLock.writeLock().unlock();
        }
    }

    /*
     * Called when the LEFT key is pressed.
     */
    public void keyleft() {
        this.rwLock.writeLock().lock();
        try {
            if (activeblock == null || state != GameState.PLAYING) {
                return;
            }

            activeblock.x--;

            //Failsafe: Revert XPosition.
            if (!copy()) {
                activeblock.x++;
            }
        } finally {
            this.rwLock.writeLock().unlock();
        }
    }

    /*
     * Called when the DOWN key is pressed.
     */
    public void keydown() {
        this.rwLock.writeLock().lock();
        try {
            if (state != GameState.PLAYING) {
                return;
            }
            //if (activeblock == null || state != GameState.PLAYING) {
            //    return;
            //}

            step();
        } finally {
            this.rwLock.writeLock().unlock();
        }
    }

    /*
     * Called when rotate key is called (Z or UP)
     */
    public void keyrotate() {
        this.rwLock.writeLock().lock();
        try {
            if (activeblock == null || activeblock.array == null || state != GameState.PLAYING) {
                return;
            }

            Tetromino lastBlock = activeblock;
            activeblock = lastBlock.rotate();
            if (activeblock == lastBlock) {
                return;
            }
            //Failsafe revert.
            if (!copy()) {
                activeblock = lastBlock;
            }
        } finally {
            this.rwLock.writeLock().unlock();
        }
    }

    /*
     * Called when slam key (SPACE) is pressed.
     */
    public void keyslam() {
        this.rwLock.writeLock().lock();
        try {
            if (activeblock == null || state != GameState.PLAYING) {
                return;
            }
            //This will game over pretty damn fast!
            if (activeblock.array == null) {
                newblock();
            }
            while (true) {
                activeblock.y++;

                if (!copy()) {
                    donecurrent();
                    return;
                }
            }
        } finally {
            this.rwLock.writeLock().unlock();
        }
    }

    //I'm bored so here's an ASCII rendering of TETRIS..
    ///////////////////////////////////////////////////////////////////
    //                                                               //
    //  ///////////   ////////  //////////  /////     //   ///////   //
    //      //       //            //      //   //   //   //         //
    //     //       ////////      //      ///////   //   ////////    //
    //    //       //            //      //  //    //         //     //
    //   //       ////////      //      ///  //   //   ////////      //
    //                                                               //
    ///////////////////////////////////////////////////////////////////
    /*
     * Should be called AFTER swing initialization. This is so the first block
     * doesn't appear halfway down the screen.
     */
    public void startengine() {
        this.step();
    }

    /*
     * Fully resets everything.
     */
    private void reset() {
        this.activeblock = null;
        this.nextblock = null;

        for (int i = 0; i < this.defs.width; i++) {
            for (int j = 0; j < this.defs.height; j++) {
                blocks[i][j] = new Block(Block.EMPTY, null);
            }
        }
    }

    /*
     * Done the current block; plays the FALL sound and changes all active
     * blocks to filled.
     */
    private void donecurrent() {
        for (Block[] blocks : this.blocks) {
            for (Block block : blocks) {
                if (block.getState() == Block.ACTIVE) {
                    block.setState(Block.FILLED);
                }
            }
        }
        checkforclears();//Moving this here.
    }

    /*
     * Copies the position of the active block into the abstract block grid.
     * Returns false if a block already exists under it, true otherwise.
     *
     */
    private boolean copy() {
        if (activeblock == null || activeblock.array == null) {
            return false;
        }
        int x = activeblock.x;
        int y = activeblock.y;

        // Check if any blocks already have a block under them.
        // If yes, immediately return.
        for (int i = 0; i < 4; i++) {
            for (int r = 0; r < 4; r++) {
                int xi = x + i;
                int yr = y + r;

                if (activeblock.array[r][i].getState() == Block.ACTIVE
                        && (xi < 0 || yr < 0 || xi >= this.defs.width || yr >= this.defs.height
                        || blocks[xi][yr].getState() == Block.FILLED)) {
                    return false;
                }
            }
        }
        Block[][] buffer = copy2D(blocks);

        //First remove all active blocks.
        for (int i = 0; i < this.defs.width; i++) {
            for (int r = 0; r < this.defs.height; r++) {
                if (buffer[i][r].getState() == Block.ACTIVE) {
                    buffer[i][r].setState(Block.EMPTY);
                    buffer[i][r].type = null;
                }
            }
        }

        //Then fill in blocks from the new position.
        for (int i = 0; i < 4; i++) {
            for (int r = 0; r < 4; r++) {
                if (activeblock.array[i][r].getState() == Block.ACTIVE) {
                    int xr = x + r;
                    int yi = y + i;
                    buffer[xr][yi].setState(Block.ACTIVE);
                    buffer[xr][yi].type = activeblock.type;
                }
            }
        }

        this.blocks = copy2D(buffer);
        return true;
    }

    /*
     * Steps into the next phase if possible.
     */
    public void step() {
        if (this.activeblock == null) {//step() gives you a random block if none is available.
            newblock();
            return;
        }

        //move 1 down.
        this.activeblock.y++;

        if (!this.copy()) {
            this.donecurrent();
        }

    }

    private void checkforclears() {
        //Threading fix?
        this.activeblock = null;

        //Don't care about fading
        //Now actually remove the blocks.
        this.checkforclears(0);
        this.newblock();
    }

    /*
     * As expected this function checks whether there are any clears. Uses
     * recursion if more than one line can be cleared. Don't run this on the EDT!
     */
    private void checkforclears(int alreadycleared) {
        int whichline = -1;
        int old = alreadycleared;

        //Loops to find any row that has every block filled.
        // If one block is not filled, the loop breaks.
        ML:
        for (int i = this.defs.height - 1; i >= 0; i--) {
            for (int y = 0; y < this.defs.width; y++) {
                if (!(blocks[y][i].getState() == Block.FILLED)) {
                    continue ML;
                }
            }

            alreadycleared++;
            whichline = i;
            break;
        }

        //If this recursive step produced more clears:
        if (alreadycleared > old) {
            for (int i = whichline; i > 0; i--) {//Iterate and copy the state of the block on top of itself
                //to its location.
                for (int y = 0; y < blocks.length; y++) {
                    blocks[y][i] = blocks[y][i - 1];
                }
            }

            //TODO Find a better way to fix StackOverflowError
            if (alreadycleared < 5) {
                //Recursion step! Necessary if you want to clear more than
                //1 line at a time!
                checkforclears(alreadycleared);
            }
        } else if (alreadycleared > 0) {
            score.addRemovedLines(alreadycleared);
        }
    }

    /*
     * Generates a random block , in a random rotation.
     */
    private void newblock() {
        assert this.activeblock == null;

        if (this.nextblock == null) {
            this.nextblock = this.getRandBlock();
        }
        /* Next block becomes the active block
           next block gets randomly generated */
        this.activeblock = this.nextblock.clone();
        this.nextblock = this.getRandBlock();

        if (!this.copy()) {
            this.setState(GameState.GAMEOVER);
        }
        this.score.addDroppedBlock();
    }

    /*
     * Create and return a random block.
     */
    private Tetromino getRandBlock() {
        Tetromino.Type blockType = Tetromino.Type.getRandom(rdm);
        int rotation = 0;
        if (blockdef[blockType.ordinal()].length == 1) {
            rotation = rdm.nextInt(blockdef[blockType.ordinal()].length);
        }
        Tetromino ret = new Tetromino(blockType, rotation);
        ret.x = this.defs.width / 2 - 2;
        ret.y = 0;
        return ret;
    }

    /*
     * Copies an array, but runs in n^2 time.
     */
    public static Block[][] copy2D(Block[][] in) {
        //if(in == null) return null;
        Block[][] ret = new Block[in.length][in[0].length];

        for (int i = 0; i < in.length; i++) {
            for (int j = 0; j < in[0].length; j++) {
                if (in[i][j] == null) {
                    ret[i][j] = null;
                } else {
                    ret[i][j] = in[i][j].clone();
                }
            }
        }

        return ret;
    }

    /*
     * Function to convert byte[][] to Block[][]
     */
    public static Block[][] toBlock2D(byte[][] b, Tetromino.Type type) {
        if (b == null) {
            return null;
        }
        Block[][] ret = new Block[b.length][b[0].length];
        for (int i = 0; i < b.length; i++) {
            for (int j = 0; j < b[0].length; j++) {
                switch (b[i][j]) {
                case 1:
                    ret[i][j] = new Block(Block.ACTIVE, type);
                    break;
                default:
                    ret[i][j] = new Block(Block.EMPTY, type);
                }
            }
        }
        return ret;
    }

    /*
     * Function to convert Block[][] to byte[][]
     */
    public static byte[][] toByte2D(Block[][] b) {
        if (b == null) {
            return null;
        }
        byte[][] ret = new byte[b.length][b[0].length];
        for (int i = 0; i < b.length; i++) {
            for (int j = 0; j < b[0].length; j++) {
                ret[i][j] = b[i][j].toByte();
            }
        }

        return ret;
    }

    /**
     * @return the blocks
     */
    public Block[][] getBlocks() {
        this.rwLock.readLock().lock();
        try {
            return copy2D(blocks);
        } finally {
            this.rwLock.readLock().unlock();
        }
    }
    
    public byte[][] getMockGrid() {
        this.rwLock.readLock().lock();
        try {
            byte[][] mockgrid = new byte[this.defs.width][this.defs.height];
            for (int i = 0; i < this.defs.width; i++) {
                for (int j = 0; j < this.defs.height; j++) {
                    byte s = (byte) blocks[i][j].getState();
                    if (s == 2) {
                        s = 0;
                    }
                    mockgrid[i][j] = s;
                }
            }
            return mockgrid;
        } finally {
            this.rwLock.readLock().unlock();
        }
    }

    /**
     * @return the activeblock
     */
    public Tetromino getActiveblock() {
        this.rwLock.readLock().lock();
        try {
            return activeblock.clone();
        } finally {
            this.rwLock.readLock().unlock();
        }
    }

    /**
     * @return the nextblock
     */
    public Tetromino getNextblock() {
        this.rwLock.readLock().lock();
        try {
            return nextblock.clone();
        } finally {
            this.rwLock.readLock().unlock();
        }
    }
}
