package tetris.generic;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Random;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import tetris.ProjectConstants.GameState;

/**
 * This class calculates the block positions, rotations, moves across the board.
 * Leaving to the gui and ai classes to only interface with this class.
 * It is thread safe and uses a ReadWriteLock.
 * It also has observable properties: score, state, blocks, nextblock.
 */
public final class TetrisEngine {

    /**
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

    /**
     * Copies an array, but runs in n^2 time.
     * 
     * @param in a Block[][] matrix to copy
     * @return a Block[][] matrix copy
     */
    public static Block[][] copy2D(Block[][] in) {
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

    /**
     * Function to convert byte[][] to Block[][]
     * 
     * @param b the byte[][] matrix
     * @param type the block type to set in the return matrix
     * @return a Block[][] matrix
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

    /**
     * Function to convert Block[][] to byte[][]
     * 
     * @param b the Block[][] matrix
     * @return a byte[][] matrix
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

    private final PropertyChangeSupport propertyChangeSupport;
    private final ReadWriteLock rwLock;
    private final Random rdm;

    private Block[][] blocks;
    private Score score;
    private GameState state;
    private Tetromino activeblock;
    private Tetromino nextblock;

    /**
     * The game definitions for this engine
     */
    public final TetrisGameDefinitions defs;

    /**
     * Remember to call startengine() or else this won't do
     * anything!
     */
    public TetrisEngine() {
        this(new TetrisGameDefinitions(6, 20));
    }

    /**
     * Remember to call startengine() or else this won't do
     * anything!
     * @param width the width of the board
     * @param height the height of the board
     */
    public TetrisEngine(int width, int height) {
        this(new TetrisGameDefinitions(width, height));
    }
    
    /**
     * Remember to call startengine() or else this won't do
     * anything!
     * @param defs the definitions for the game
     */
    public TetrisEngine(TetrisGameDefinitions defs) {
        this.defs = defs;
        this.propertyChangeSupport = new PropertyChangeSupport(this);
        this.rwLock = new ReentrantReadWriteLock();
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
            GameState oldValue = this.state;
            this.state = newValue;
            this.propertyChangeSupport.firePropertyChange("state", oldValue, newValue);
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

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener propertyChangeListener) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, propertyChangeListener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
    }

    /**
     * Called when the RIGHT key is pressed.
     * 
     * @return true if the move was successful
     */
    public boolean keyright() {
        this.rwLock.writeLock().lock();
        try {
            if (activeblock == null || state != GameState.PLAYING) {
                return false;
            }

            activeblock.x++;

            //Failsafe: Revert XPosition.
            if (!copy()) {
                activeblock.x--;
                return false;
            }
            return true;
        } finally {
            this.rwLock.writeLock().unlock();
        }
    }

    /**
     * Called when the LEFT key is pressed.
     * 
     * @return true if the move was successful
     */
    public boolean keyleft() {
        this.rwLock.writeLock().lock();
        try {
            if (activeblock == null || state != GameState.PLAYING) {
                return false;
            }

            activeblock.x--;

            //Failsafe: Revert XPosition.
            if (!copy()) {
                activeblock.x++;
                return false;
            }
            return true;
        } finally {
            this.rwLock.writeLock().unlock();
        }
    }

    /**
     * Called when the DOWN key is pressed.
     * 
     * @return true if the move was successful
     */
    public boolean keydown() {
        this.rwLock.writeLock().lock();
        try {
            if (state != GameState.PLAYING) {
                return false;
            }
            //if (activeblock == null || state != GameState.PLAYING) {
            //    return;
            //}

            return step();
        } finally {
            this.rwLock.writeLock().unlock();
        }
    }

    /**
     * Called when rotate key is called (Z or UP)
     * 
     * @return true if the move was successful
     */
    public boolean keyrotate() {
        this.rwLock.writeLock().lock();
        try {
            if (activeblock == null || activeblock.array == null || state != GameState.PLAYING) {
                return false;
            }

            Tetromino lastBlock = activeblock;
            activeblock = lastBlock.rotate();
            if (activeblock == lastBlock) {
                return false;
            }
            //Failsafe revert.
            if (!copy()) {
                activeblock = lastBlock;
                return false;
            }
            return true;
        } finally {
            this.rwLock.writeLock().unlock();
        }
    }

    /**
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
    /**
     * Should be called AFTER swing initialization. This is so the first block
     * doesn't appear halfway down the screen.
     */
    public void startengine() {
        this.rwLock.writeLock().lock();
        try {
            this.step();
        } finally {
            this.rwLock.writeLock().unlock();
        }
    }

    /**
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
        this.propertyChangeSupport.firePropertyChange("blocks", null, null);
        this.propertyChangeSupport.firePropertyChange("nextblock", null, null); // FIXME
    }

    /**
     * Done the current block and changes all active blocks to filled.
     */
    private void donecurrent() {
        for (Block[] blocks : this.blocks) {
            for (Block block : blocks) {
                if (block.getState() == Block.ACTIVE) {
                    block.setState(Block.FILLED);
                }
            }
        }
        // Threading fix?
        this.activeblock = null;

        // Don't care about fading
        // Now actually remove the blocks.
        this.checkforclears();
        this.newblock();
        this.propertyChangeSupport.firePropertyChange("blocks", null, null);
    }

    /**
     * Copies the position of the active block into the abstract block grid.
     * 
     * @return false if a block already exists under it, true otherwise.
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
                    buffer[i][r].setType(null);
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
                    buffer[xr][yi].setType(activeblock.type);
                }
            }
        }

        this.blocks = copy2D(buffer);
        this.propertyChangeSupport.firePropertyChange("blocks", null, null);
        return true;
    }

    /**
     * Steps into the next phase if possible.
     * 
     * @return true if the active block moved down without touching another block
     */
    private boolean step() {
        if (this.activeblock == null) {// step() gives you a random block if none is available.
            newblock();
            return false;
        }

        //move 1 down.
        this.activeblock.y++;

        if (!this.copy()) {
            this.donecurrent();
            return false;
        }
        return true;

    }

    /**
     * As expected this function checks whether there are any clears.
     */
    private void checkforclears() {
        int whichline = -1;
        int clearedLines = 0;
        int old;
        do {
            old = clearedLines;
            
            // Loops to find any row that has every block filled.
            // If one block is not filled, the loop breaks.
            ML:
            for (int i = this.defs.height - 1; i >= 0; i--) {
                for (int y = 0; y < this.defs.width; y++) {
                    if (blocks[y][i].getState() != Block.FILLED) {
                        continue ML;
                    }
                }
                clearedLines++;
                whichline = i;
                break;
            }

            // If this recursive step produced more clears:
            if (clearedLines > old) {
                for (int i = whichline; i > 0; i--) {
                    /* Iterate and copy the state of the block on top of itself
                       to its location. */
                    for (int y = 0; y < blocks.length; y++) {
                        blocks[y][i] = blocks[y][i - 1];
                    }
                }
            }
        } while(clearedLines > old);
        if (clearedLines > 0) {
            Score oldValue = this.score.Clone();
            this.score.addRemovedLines(clearedLines);
            this.propertyChangeSupport.firePropertyChange("score", oldValue, this.score.Clone());
        }
    }

    /*
     * Generates a random block , in a random rotation.
     */
    private void newblock() {
        assert this.activeblock == null;

        if (this.nextblock == null) {
            this.activeblock = this.getRandBlock();
            this.nextblock = this.getRandBlock();
        } else {
            /* Next block becomes the active block
               next block gets randomly generated */
            this.activeblock = this.nextblock.clone();
            this.nextblock = this.getRandBlock();
        }

        if (!this.copy()) {
            this.setState(GameState.GAMEOVER);
        }
        
        Score oldValue = this.score.Clone();
        this.score.addDroppedBlock();
        this.propertyChangeSupport.firePropertyChange("score", oldValue, this.score.Clone());
        this.propertyChangeSupport.firePropertyChange("nextblock", null, null);
    }

    /**
     * Create and return a random block.
     * 
     * @return a random block
     */
    private Tetromino getRandBlock() {
        Tetromino block = Tetromino.getRandom(rdm);
        block.x = this.defs.width / 2 - 2;
        block.y = 0;
        return block;
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

    /**
     * Create a mock grid based on the current state of this engine
     * 
     * @return a byte matrix
     */
    public byte[][] createMockGrid() {
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
            if (activeblock == null) {
                return null;
            } else {
                return activeblock.clone();
            }
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
            if (nextblock == null) {
                return null;
            } else {
                return nextblock.clone();
            }
        } finally {
            this.rwLock.readLock().unlock();
        }
    }
}
