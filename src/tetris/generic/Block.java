package tetris.generic;


/*
 * More concrete representation of a block.
 */
public final class Block implements Cloneable {
    public static final int EMPTY = 0, FILLED = 1, ACTIVE = 2;

    /**
     * Function to convert byte[][] to Block[][]
     *
     * @param source the byte[][] matrix
     * @param type the block type to set in the return matrix
     * @return a Block[][] matrix
     */
    public static Block[][] toBlock2D(byte[][] source, Tetromino.Type type) {
        if (source == null) {
            return null;
        }
        Block[][] result = new Block[source.length][];
        for (int i = 0; i < source.length; i++) {
            result[i] = new Block[source[i].length];
            for (int j = 0; j < source[i].length; j++) {
                result[i][j] = source[i][j] == 1 ?
                        new Block(ACTIVE, type) :
                        new Block(EMPTY, type);
            }
        }
        return result;
    }

    /**
     * Function to convert Block[][] to byte[][]
     *
     * @param source the Block[][] matrix
     * @return a byte[][] matrix
     */
    public static byte[][] toByte2D(Block[][] source) {
        if (source == null) {
            return null;
        }
        byte[][] result = new byte[source.length][];
        for (int i = 0; i < source.length; i++) {
            result[i] = new byte[source[i].length];
            for (int j = 0; j < source[i].length; j++) {
                result[i][j] = source[i][j].toByte();
            }
        }
        return result;
    }

    /**
     * Copies an array, but runs in n^2 time.
     *
     * @param source a Block[][] matrix to copy
     * @return a Block[][] matrix copy
     */
    public static Block[][] copy2D(Block[][] source) {
        if (source == null) {
            return null;
        }
        Block[][] result = new Block[source.length][];
        for (int i = 0; i < source.length; i++) {
            result[i] = new Block[source[i].length];
            for (int j = 0; j < source[i].length; j++) {
                result[i][j] = source[i][j] == null ? null : source[i][j].clone();
            }
        }
        return result;
    }

    /*
     * State of the block.
     */
    private volatile int state;

    private Tetromino.Type type;

    /*
     * Initializing constructor.
     */
    public Block(int state, Tetromino.Type type) {
        this.state = state;
        this.type = type;
    }

    /**
     * Implements the Clonable interface.
     * @return a block just equal to this block\
     */
    @Override
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
