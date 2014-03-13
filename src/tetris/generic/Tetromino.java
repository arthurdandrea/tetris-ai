package tetris.generic;

import java.util.Objects;
import java.util.Random;

/*
 * Object representation of a tetromino.
 */
public final class Tetromino implements Cloneable {

    public static Tetromino getRandom(Random random) {
        Tetromino.Type type = Tetromino.Type.getRandom(random);
        byte[][][] blockdef = Definitions.blockdef[type.ordinal()];
        int rotation;
        if (blockdef.length == 1) {
            rotation = 0;
        } else {
            rotation = random.nextInt(blockdef.length);
        }
        Tetromino tetromino = new Tetromino();
        tetromino.type = type;
        tetromino.rot = rotation;
        tetromino.array = Block.toBlock2D(blockdef[rotation], type);
        return tetromino;
    }

    public Block[][] array;
    public int x, y, rot;
    public Type type;

    private Tetromino() {
        this.x = 0;
        this.y = 0;
    }

    public Tetromino(Type blockType, int rotation) {
        this();
        
        Objects.requireNonNull(blockType);
        byte[][][] blockdef = Definitions.blockdef[blockType.ordinal()];
        if (rotation < 0 || rotation >= blockdef.length) {
            throw new IndexOutOfBoundsException("rotation is out of bounds");
        }

        this.type = blockType;
        this.rot = rotation;
        this.array = Block.toBlock2D(blockdef[this.rot], this.type);
    }
    
    public Tetromino rotate() {
        byte[][][] blockdef = Definitions.blockdef[this.type.ordinal()];
        if (blockdef.length == 1) {
            return this;
        }
        Tetromino other = new Tetromino();
        other.x = this.x;
        other.y = this.y;
        other.type = this.type;
        if (this.rot == blockdef.length - 1) {
            other.rot = 0;
        } else {
            other.rot = this.rot + 1;
        }
        other.array = Block.toBlock2D(blockdef[other.rot], this.type);
        return other;
    }

    @Override
    public Tetromino clone() {
        Tetromino ret = new Tetromino();
        ret.array = array.clone();
        ret.x = x;
        ret.y = y;
        ret.rot = rot;
        ret.type = type;
        return ret;
    }

    @Override
    public String toString() {
        return String.format("Tetromino[type:%s,x=%d,y=%d,rot=%d]",
                             this.type, this.x, this.y, this.rot);
    }
    public enum Type {
        Long, Box, L, J, T, S, Z;
        
        public static Type getRandom(Random random) {
            return Type.values()[random.nextInt(Type.values().length)];
        }
    }
}
