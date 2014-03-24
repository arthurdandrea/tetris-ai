package tetris.generic;

import java.util.Objects;
import tetris.generic.Tetromino.Type;

public class BlockPosition {
    public final Type type;
    public final byte bx;
    public final byte rot;

    public BlockPosition(int bx, int rot, Type type) {
        this((byte) bx, (byte) rot, type);
    }

    public BlockPosition(byte bx, byte rot, Type type) {
        Objects.requireNonNull(type);

        this.type = type;
        this.bx = bx;
        this.rot = rot;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof BlockPosition)) {
            return false;
        }
        BlockPosition other = (BlockPosition) obj;
        return this.bx == other.bx && this.rot == other.rot;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 71 * hash + this.bx;
        hash = 71 * hash + this.rot;
        return hash;
    }

    @Override
    public String toString() {
        return "BlockPosition{" + "bx=" + bx + ", rot=" + rot + '}';
    }
}
