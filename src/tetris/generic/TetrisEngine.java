package tetris.generic;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
    //<editor-fold defaultstate="collapsed" desc="blockdef">
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
//</editor-fold>

    private List<TetrisEngineListener> listeners;

    /*
     * Random object used to generate new blocks.
     */
    private final Random rdm;

    /*
     * Primitive representation of active block.
     */
    public volatile Tetromino activeblock;

    /*
     * Next block.
     */
    public volatile Tetromino nextblock;

    public final int WIDTH = 6;
    public final int HEIGHT = 20;

    /*
     * DBlock array representation of the gamefield. Blocks are counted X first
     * starting from the top left: blocks[5][3] would be a block 5 left and 3
     * down from (0,0).
     */
    public Block[][] blocks;

    /*
     * Score
     */
    private int score = 0;

    /*
     * Lines cleared
     */
    public int lines = 0;

    /*
     * How many blocks were dropped so far?
     */
    public int blocksdropped = 0;

    /*
     * Current state of the game (PLAYING, PAUSED, etc.)
     */
    private GameState state;

    /*
     * Public constructor. Remember to call startengine() or else this won't do
     * anything! @param p TetrisPanel.
     */
    public TetrisEngine(TetrisEngineListener listener) {
        this();
        if (listener != null) {
            this.listeners.add(listener);
        }
    }

    public TetrisEngine() {
        this.listeners = new ArrayList<>();
        this.rdm = new Random();
        this.blocks = new Block[WIDTH][HEIGHT];
        this.reset();
    }

    public GameState getState() {
        return this.state;
    }

    public void setState(GameState newValue) {
        this.state = newValue;
        List<TetrisEngineListener> listenersCopy = new ArrayList<>(this.listeners);
        for (TetrisEngineListener listener : listenersCopy) {
            listener.onGameStateChange(this);
        }
    }

    /**
     * @return the score
     */
    public int getScore() {
        return score;
    }

    public synchronized void addListener(TetrisEngineListener listener) {
        this.listeners.add(listener);
    }


    /*
     * Called when the RIGHT key is pressed.
     */
    public void keyright() {
        if (activeblock == null || state != GameState.PLAYING) {
            return;
        }

        activeblock.x++;

        //Failsafe: Revert XPosition.
        if (!copy()) {
            activeblock.x--;
        }

    }

    /*
     * Called when the LEFT key is pressed.
     */
    public void keyleft() {
        if (activeblock == null || state != GameState.PLAYING) {
            return;
        }

        activeblock.x--;

        //Failsafe: Revert XPosition.
        if (!copy()) {
            activeblock.x++;
        }
    }

    /*
     * Called when the DOWN key is pressed.
     */
    public void keydown() {
        if (activeblock == null || state != GameState.PLAYING) {
            return;
        }

        step();
    }

    /*
     * Called when rotate key is called (Z or UP)
     */
    public void keyrotate() {
        if (activeblock == null || activeblock.array == null || state != GameState.PLAYING) {
            return;
        }

        Block[][] lastblock = copy2D(activeblock.array);
        int lastrot = activeblock.rot;

        //Next rotation in array.
        if (activeblock.rot == blockdef[activeblock.type.ordinal()].length - 1) {
            activeblock.rot = 0;
        } else {
            activeblock.rot++;
        }

        activeblock.array = toBlock2D(blockdef[activeblock.type.ordinal()][activeblock.rot]);

        //Failsafe revert.
        if (!copy()) {
            activeblock.array = lastblock;
            activeblock.rot = lastrot;
        }
    }

    /*
     * Called when slam key (SPACE) is pressed.
     */
    public void keyslam() {
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
    public synchronized void startengine() {
        this.step();
    }

    /*
     * Resets the blocks but keeps everything else.
     */
    public synchronized void clear() {
        for (int i = 0; i < blocks.length; i++) {
            for (int j = 0; j < blocks[i].length; j++) {
                blocks[i][j] = new Block(Block.EMPTY);
            }
        }
    }

    /*
     * Fully resets everything.
     */
    public synchronized void reset() {
        this.score = 0;
        this.lines = 0;
        this.activeblock = null;
        this.nextblock = null;

        this.clear();
    }

    /*
     * Done the current block; plays the FALL sound and changes all active
     * blocks to filled.
     */
    private synchronized void donecurrent() {
        for (int i = 0; i < blocks.length; i++) {
            for (int r = 0; r < blocks[i].length; r++) {
                if (blocks[i][r].getState() == Block.ACTIVE) {
                    blocks[i][r].setState(Block.FILLED);
                }
            }
        }

        checkforclears();//Moving this here.
    }

    /*
     * Called when Game Over (Blocks stacked so high that copy() fails)
     */
    public synchronized void gameover() {
        //Check first.
        if (this.state == GameState.GAMEOVER) {
            return;
        }

        this.setState(GameState.GAMEOVER);
        int lastlines = lines;
        int lastscore = getScore();
        this.reset();
        List<TetrisEngineListener> listenersCopy = new ArrayList<>(this.listeners);
        for (TetrisEngineListener listener : listenersCopy) {
            listener.onGameOver(this, lastscore, lastlines);
        }
    }

    /*
     * Copies the position of the active block into the abstract block grid.
     * Returns false if a block already exists under it, true otherwise.
     *
     * This method isn't very efficient. Thus, it must be
     * synchronized.
     */
    private synchronized boolean copy() {
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

                if (activeblock.array[r][i].getState() == Block.ACTIVE && 
                        (xi >= WIDTH || yr >= HEIGHT ||
                        blocks[xi][yr].getState() == Block.FILLED)) {
                    return false;
                }
            }
        }
        Block[][] buffer = copy2D(blocks);

        //First remove all active blocks.
        for (int i = 0; i < WIDTH; i++) {
            for (int r = 0; r < HEIGHT; r++) {
                if (buffer[i][r].getState() == Block.ACTIVE) {
                    buffer[i][r].setState(Block.EMPTY);
                    buffer[i][r].setColor(Block.emptycolor);
                }
            }
        }

        //Then fill in blocks from the new position.
        for (int i = 0; i < 4; i++) {
            for (int r = 0; r < 4; r++) {
                if (activeblock.array[i][r].getState() == Block.ACTIVE) {
                    buffer[x + r][y + i].setState(Block.ACTIVE);

                    //facepalm.
                    buffer[x + r][y + i].setColor(activeblock.color);
                }
            }
        }

        this.blocks = copy2D(buffer);
        return true;
    }

    /*
     * Steps into the next phase if possible.
     */
    public synchronized void step() {
        if (activeblock == null) {//step() gives you a random block if none is available.
            newblock();

            return;
        }

        //move 1 down.
        activeblock.y++;

        if (!copy()) {
            donecurrent();
        }

    }

    /*
     * Runs the checkforclears() on a seperate thread. Also performs the fade
     * out effect.
     */
    private synchronized void checkforclears() {
        //Threading fix?
        activeblock = null;

        //Don't care about fading
        //Now actually remove the blocks.
        checkforclears(0);
        newblock();
    }

    /*
     * As expected this function checks whether there are any clears. Uses
     * recursion if more than one line can be cleared. Don't run this on the EDT!
     */
    private synchronized void checkforclears(int alreadycleared) {
        int whichline = -1;
        int old = alreadycleared;

        //Loops to find any row that has every block filled.
        // If one block is not filled, the loop breaks.
        ML:
        for (int i = HEIGHT - 1; i >= 0; i--) {
            for (int y = 0; y < WIDTH; y++) {
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
            // Use Nintendo's original scoring system.
            switch (alreadycleared) {
            case 1:
                score += 40;
                break;
            case 2:
                score += 100;
                break;
            case 3:
                score += 300;
                break;
            case 4:
                score += 1200;
                break;
            }

            lines += alreadycleared;
        }
    }

    /*
     * Generates a random block , in a random rotation.
     */
    private synchronized void newblock() {
        // Check:
        if (activeblock != null) {
            return;
        }
        if (nextblock == null) {
            nextblock = getRandBlock();
        }

        //Next block becomes this block.
        activeblock = nextblock.clone();

        //Generate random block.
        nextblock = getRandBlock();

        if (!copy()) {
            gameover();
        }

        //Bonus?
        score += 1;

        //Successfully dropped 1 block, here.
        blocksdropped += 1;

        List<TetrisEngineListener> listenersCopy = new ArrayList<>(this.listeners);
        for (TetrisEngineListener listener : listenersCopy) {
            listener.onNewBlock(this);
        }
    }

    /*
     * Create and return a random block.
     */
    private synchronized Tetromino getRandBlock() {
        int blockType = rdm.nextInt(blockdef.length);
        int rotation = blockdef[blockType].length == 1 ? 0 : rdm.nextInt(blockdef[blockType].length);

        Tetromino ret = new Tetromino();
        ret.rot = rotation;
        ret.setType(blockType);
        ret.array = toBlock2D(blockdef[blockType][rotation]);

        ret.x = WIDTH / 2 - 2;
        ret.y = 0;

        Color bcolor = Block.colors[blockType];
        ret.color = bcolor;
        for (Block[] lineOfBlocks : ret.array) {
            for (Block block : lineOfBlocks) {
                if (block.getState() == Block.ACTIVE) {
                    block.setColor(ret.color);
                }
            }
        }
        return ret;
    }

    /*
     * Copies an array, but runs in n^2 time.
     */
    static Block[][] copy2D(Block[][] in) {
        //if(in == null) return null;
        Block[][] ret = new Block[in.length][in[0].length];

        for (int i = 0; i < in.length; i++) {
            for (int j = 0; j < in[0].length; j++) {
                ret[i][j] = in[i][j].clone();
            }
        }

        return ret;
    }

    /*
     * Function to convert byte[][] to Block[][]
     */
    static Block[][] toBlock2D(byte[][] b) {
        if (b == null) {
            return null;
        }
        Block[][] ret = new Block[b.length][b[0].length];
        for (int i = 0; i < b.length; i++) {
            for (int j = 0; j < b[0].length; j++) {
                switch (b[i][j]) {
                case 1:
                    ret[i][j] = new Block(Block.ACTIVE);
                    break;
                default:
                    ret[i][j] = new Block(Block.EMPTY);
                }
            }
        }
        return ret;
    }

    /*
     * Function to convert Block[][] to byte[][]
     */
    static byte[][] toByte2D(Block[][] b) {
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
}
