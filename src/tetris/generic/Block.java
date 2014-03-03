package tetris.generic;


/*
 * More concrete representation of a block.
 */
public class Block implements Cloneable {
    public static final int EMPTY = 0, FILLED = 1, ACTIVE = 2;

    /*
     * State of the block.
     */
    private volatile int state = EMPTY;

    public Tetromino.Type type;

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

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
