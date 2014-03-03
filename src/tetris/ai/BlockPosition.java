package tetris.ai;

public class BlockPosition {

    byte bx;
    byte rot;

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof BlockPosition)) {
            return false;
        }
        BlockPosition other = (BlockPosition) obj;
        if (this.bx != other.bx) {
            return false;
        }
        return this.rot == other.rot;
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
