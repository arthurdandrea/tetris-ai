package tetris.generic;


/*
 * More concrete representation of a block.
 */
public class Block implements Cloneable {
    public static final int EMPTY = 0, FILLED = 1, ACTIVE = 2;

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

    /*
     * State of the block.
     */
    private volatile int state = EMPTY;

    private Tetromino.Type type;

    /*
     * Initializing constructor.
     */
    public Block(int state, Tetromino.Type type) {
        this.state = state;
        this.type = type;
    }

    /*
     * Implements the Clonable interface.
     */
    public Block clone() {
        return new Block(state, type);
    }

    public byte toByte() {
        switch (state) {
            case EMPTY:
            case FILLED:
            case ACTIVE:
                return (byte) state;
            default:
                return -1;
        }
    }

    /**
     * @return the state
     */
    public int getState() {
        return state;
    }

    /**
     * @param state the state to set
     */
    public void setState(int state) {
        this.state = state;
    }

    /**
     * @return the type
     */
    public Tetromino.Type getType() {
        return this.state == EMPTY ? null : this.type;
    }

    /**
     * @param type the type to set
     */
    public void setType(Tetromino.Type type) {
        this.type = type;
    }
}
