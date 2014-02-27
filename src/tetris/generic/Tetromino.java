package tetris.generic;

import java.awt.Color;

/*
 * Object representation of a tetromino.
 */
public class Tetromino implements Cloneable {

    public enum Type {

        Long, Box, L, J, T, S, Z
    }
    private static final Type[] TypeValues = Type.values();

    void setType(int rnd1) {
        this.type = TypeValues[rnd1];
    }

    /*
     * Contents (Block array)
     */
    public Block[][] array;
    /*
     * Position, rotation, type, etc
     */
    public volatile int x, y, rot;
    public volatile Type type;
    /*
     * Color.
     */
    public volatile Color color;

    /*
     * Copy.
     */
    @Override
    public Tetromino clone() {
        Tetromino ret = new Tetromino();
        ret.array = array.clone();
        ret.x = x;
        ret.y = y;
        ret.rot = rot;
        ret.type = type;
        ret.color = color;
        return ret;
    }

    /*
     * String representation.
     */
    @Override
    public String toString() {
        switch (type) {
            case Long:
                return "Long";
            case Box:
                return "Box";
            case L:
                return "L";
            case J:
                return "J";
            case T:
                return "T";
            case S:
                return "S";
            case Z:
                return "Z";
            default:
                return "NULL";
        }
    }
}
