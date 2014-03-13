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
