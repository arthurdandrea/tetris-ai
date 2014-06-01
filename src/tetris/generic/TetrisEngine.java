package tetris.generic;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class calculates the block positions, rotations, moves across the board.
 * Leaving to the gui and ai classes to only interface with this class.
 * It is thread safe and uses a ReadWriteLock.
 * It also has observable properties: score, state, blocks, nextblock.
 */
public final class TetrisEngine {
    
    public enum Move {
        RIGHT, LEFT, ROTATE, DOWN, SLAM
    }

    /**
     * Enum representation of the current game's state.
     */
    public enum GameState {

        /**
         * The game is on.
         */
        PLAYING,

        /**
         * Time for a break.
         */
        PAUSED,

        /**
         * Damn.
         */
        GAMEOVER;
    }
    
    private final List<TetrisMoveListener> moveListeners = new ArrayList<>();
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
    public final Definitions defs;

    /**
     * Remember to call startengine() or else this won't do
     * anything!
     */
    public TetrisEngine() {
        this(Definitions.create(6, 20));
    }

    /**
     * Remember to call startengine() or else this won't do
     * anything!
     * @param width the width of the board
     * @param height the height of the board
     */
    public TetrisEngine(int width, int height) {
        this(Definitions.create(width, height));
    }
    
    /**
     * Remember to call startengine() or else this won't do
     * anything!
     * @param defs the definitions for the game
     */
    public TetrisEngine(Definitions defs) {
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
        this.rwLock.readLock().lock();
        try {
            return this.state;
        } finally {
            this.rwLock.readLock().unlock();
        }
    }
    
    public void tooglePause() {
        this.rwLock.writeLock().lock();
        try {
            if (this.state == GameState.GAMEOVER) {
                this.reset();
                this.step();
                this.score = new Score();
            }
            GameState oldValue = this.state;
            this.state = this.state != GameState.PLAYING ? GameState.PLAYING : GameState.PAUSED;
            this.propertyChangeSupport.firePropertyChange("state", oldValue, this.state);
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
        return this.tryMove(Move.RIGHT);
    }

    /**
     * Called when the LEFT key is pressed.
     * 
     * @return true if the move was successful
     */
    public boolean keyleft() {
        return this.tryMove(Move.LEFT);
    }

    /**
     * Called when the DOWN key is pressed.
     * 
     * @return true if the move was successful
     */
    public boolean keydown() {
        return this.tryMove(Move.DOWN);
    }

    /**
     * Called when rotate key is called (Z or UP)
     * 
     * @return true if the move was successful
     */
    public boolean keyrotate() {
        return this.tryMove(Move.ROTATE);
    }

    /**
     * Called when slam key (SPACE) is pressed.
     */
    public void keyslam() {
        this.tryMove(Move.SLAM);
    }
    
    public static class MoveResult {
        public final Move move;
        public final boolean successful;
        public final boolean fallEnded;
        public final Tetromino nextblock;

        private MoveResult(Move move, boolean successful, boolean fallEnded) {
            this.move = move;
            this.successful = successful;
            this.fallEnded = fallEnded;
            this.nextblock = null;
            assert this.fallEnded == false;
        }

        public MoveResult(Move move, boolean successful, Tetromino nextblock) {
            this.move = move;
            this.successful = successful;
            this.fallEnded = nextblock != null;
            this.nextblock = nextblock;
        }
    }
    public boolean tryMove(Move move) {
        return this.tryMove(move, null);
    }
    
    public boolean tryMove(Move move, Tetromino nextblock_candidate) {
        this.rwLock.writeLock().lock();
        try {
            if (this.state != GameState.PLAYING) {
                return false;
            }

            boolean fallEnded = false;
            boolean successful = false;
            switch (move) {
            case RIGHT:
                if (this.activeblock != null) {
                    this.activeblock.x++;
                    // Failsafe: revert x position
                    if (!this.copy()) {
                        this.activeblock.x--;
                    } else {
                        successful = true;
            }
            }
                break;
            case LEFT:
                if (activeblock != null) {
                    this.activeblock.x--;
                    // Failsafe: revert x position
                    if (!this.copy()) {
                        this.activeblock.x++;
                    } else {
                        successful = true;
        }
    }
                break;
            case ROTATE:
                if (this.activeblock != null) {
                    Tetromino lastBlock = this.activeblock;
                    this.activeblock = lastBlock.rotate();
                    if (this.activeblock != lastBlock) {
                        //Failsafe revert.
                        if (!this.copy()) {
                            this.activeblock = lastBlock;

                        } else {
                            successful = true;
                        }
            }
            }

                break;
            case DOWN:
                fallEnded = !this.step(nextblock_candidate);
                successful = true;
                break;
            case SLAM:
                while (this.step(nextblock_candidate)) {
                }
                fallEnded = true;
                successful = true;
                break;
            default:
                throw new AssertionError();
            }
            MoveResult result;
            if (fallEnded) {
                result = new MoveResult(move, successful, this.nextblock);
            } else {
                result = new MoveResult(move, successful, null);
                }
            if (successful) {
                for (TetrisMoveListener listener : moveListeners) {
                    listener.sucessfulMove(result);
            }
            }
            if (move == Move.DOWN || move == Move.SLAM) {
                return fallEnded;
            }
            return true;
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
            
            this.reset();
        } finally {
            this.rwLock.writeLock().unlock();
        }
    }

    /**
     * Fully resets everything.
     */
    public void reset() {
        this.rwLock.writeLock().lock();
        try {
        this.reset(null, null, null);
        } finally {
            this.rwLock.writeLock().unlock();
        }
    }

    private void reset(Tetromino activeblock, Tetromino nextblock, Block[][] blocks) {
        this.activeblock = activeblock == null ? Tetromino.getRandom(rdm) : activeblock;
        this.nextblock =   nextblock == null ? Tetromino.getRandom(rdm) : nextblock;
        this.state = GameState.PLAYING;
        this.score = new Score();
        this.propertyChangeSupport.firePropertyChange("score", null, null);
        if (blocks == null) {
        for (int i = 0; i < this.defs.width; i++) {
            for (int j = 0; j < this.defs.height; j++) {
                    this.blocks[i][j] = new Block(Block.EMPTY, null);
                }
            }
        } else {
            this.blocks = blocks;
        }
        this.copy();
        this.propertyChangeSupport.firePropertyChange("blocks", null, null);
        this.propertyChangeSupport.firePropertyChange("nextblock", null, null); // FIXME
    }

    private void donecurrent() {
        this.donecurrent(null);
    }
    /**
     * Done the current block and changes all active blocks to filled.
     */
    private void donecurrent(Tetromino nextblock_candidate) {
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
        this.clearFullLines();
        this.newblock(nextblock_candidate);
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
        Block[][] buffer = Block.copy2D(blocks);

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

        this.blocks = Block.copy2D(buffer);
        this.propertyChangeSupport.firePropertyChange("blocks", null, null);
        return true;
    }

    /**
     * Steps into the next phase if possible.
     * 
     * @return true if the active block moved down without touching another block
     */
    private boolean step(Tetromino nextblock_candidate) {
        if (this.activeblock == null) {// step() gives you a random block if none is available.
            this.newblock(nextblock_candidate);
            return false;
        }

        //move 1 down.
        this.activeblock.y++;

        if (!this.copy()) {
            this.donecurrent(nextblock_candidate);
            return false;
        }
        return true;
    }

    private boolean step() {
        return this.step(null);
    }

    /**
     * As expected this function checks whether there are any full lines and clears them.
     */
    private void clearFullLines() {
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
    private void newblock(Tetromino nextblock_candidate) {
        assert this.activeblock == null;

        if (nextblock_candidate == null) {
            nextblock_candidate = this.getRandBlock();
        } 
        if (this.nextblock == null) {
            this.activeblock = this.getRandBlock();
            this.nextblock = nextblock_candidate;
        } else {
            /* Next block becomes the active block
               next block gets randomly generated */
            this.activeblock = this.nextblock.clone();
            this.nextblock = nextblock_candidate;
        }

        if (!this.copy()) {
            GameState oldValue = this.state;
            this.state = GameState.GAMEOVER;
            this.propertyChangeSupport.firePropertyChange("state", oldValue, this.state);
        } else {
            Score oldValue = this.score.Clone();
            this.score.addDroppedBlock();
            this.propertyChangeSupport.firePropertyChange("score", oldValue, this.score.Clone());
        }
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
            return Block.copy2D(blocks);
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

    public void addMoveListener(TetrisMoveListener listener) {
        this.rwLock.writeLock().lock();
        try {
            this.moveListeners.add(listener);
        } finally {
            this.rwLock.writeLock().unlock();
        }
    }
    
    public static class CompleteState {
        public Block[][] blocks;
        public Tetromino activeblock;
        public Tetromino nextblock;
        public Definitions definitions;
    }
    public CompleteState dumpCompleteState() {
        this.rwLock.readLock().lock();
        try {
            CompleteState state = new CompleteState();
            state.definitions = this.defs;
            state.blocks = this.blocks.clone();
            state.activeblock = this.activeblock.clone();
            state.nextblock = this.nextblock.clone();
            return state;
        } finally {
            this.rwLock.readLock().unlock();
        }
    }

    public void loadCompleteState(CompleteState state) {
        this.rwLock.writeLock().lock();
        try {
            this.reset(state.activeblock, state.nextblock, state.blocks);
        } finally {
            this.rwLock.writeLock().unlock();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TetrisEngine) {
            TetrisEngine other = (TetrisEngine) obj;

            if (!this.defs.equals(other.defs) || !this.activeblock.equals(other.activeblock) || !this.nextblock.equals(other.nextblock)) return false;

            for (int i = 0; i < blocks.length; i++) {
                for (int j = 0; j < blocks[i].length; j++) {
                    if (!blocks[i][j].equals(other.blocks[i][j])) {
                        return false;
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }
}
