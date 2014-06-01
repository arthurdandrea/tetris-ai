/*
 * Copyright (C) 2014 Arthur D'Andréa Alemar
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package tetris.generic;

import com.google.common.collect.Iterators;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
public class Definitions {
    /**
     * Bunch of hardcoded blocks and their rotations. Code them high up in the
     * array so that when you get a new one it appears in the highest spot
     * possible.
     */
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
    
    private static final FreeSpaces[][] freeSpaces = calculateFreeSpaces();

    
    /**
     * Return the amount of free columns right and left of a block definition
     * for each rotation of the block
     * 
     * @param type the type of the tetromino block
     * @return an array of free spaces for each rotation of the block
     */
    public static FreeSpaces[] getFreeSpaces(Tetromino.Type type) {
        Objects.requireNonNull(type);
        return freeSpaces[type.ordinal()];
    }
    
    private static FreeSpaces[][] calculateFreeSpaces() {
        FreeSpaces[][] result = new FreeSpaces[blockdef.length][];
        for (int i = 0; i < blockdef.length; ++i) {
            result[i] = new FreeSpaces[blockdef[i].length];
            for (int j = 0; j < blockdef[i].length; j++) {
                result[i][j] = calculateFreeSpaces(blockdef[i][j]);
            }
        }
        return result;
    }

    private static FreeSpaces calculateFreeSpaces(byte[][] in) {
        // It's free if all of them are zero, and their sum is zero.
        boolean[] column = new boolean[4];
        for (int j = 0; j < 4; j++) {
            column[j] = true;
            for (int i = 0; i < 4; i++) {
                if (in[i][j] != 0) {
                    column[j] = false;
                    break;
                }
            }
        }
        int freeOnLeft = 0;
        for (int i = 0; i < 4; i++) {
            if (!column[i]) {
                break;
            }
            freeOnLeft++;
        }
        int freeOnRight = 0;
        for (int i = 3; i >= 0; i--) {
            if (!column[i]) {
                break;
            }
            freeOnRight++;
        }
        return new FreeSpaces(freeOnLeft, freeOnRight);
    }
    
    /**
     * The height of the board
     */
    public final int height;

    /**
     * The width of the board
     */
    public final int width;
    private final BlockPosition[][] possibleFits;

    
    public static Definitions create(int width, int height) {
        return new Definitions(width, height);
    }

    public Definitions(int width, int height) {
        assert width > 0;
        assert height > 0;

        this.width = width;
        this.height = height;
        this.possibleFits = new BlockPosition[blockdef.length][];
        for (int i = 0; i < blockdef.length; i++) {
            this.possibleFits[i] = Iterators.toArray(new GetPossibleFits(Tetromino.Type.values()[i]), BlockPosition.class);
        }
    }

    /**
     * Return all the possible fits of a block type on an empty board, combining
     * every rotations and every horizontal position.
     * 
     * @param type the type of the tetromino block
     * @return an array of possible positions
     */
    public BlockPosition[] getPossibleFits(Tetromino.Type type) {
        Objects.requireNonNull(type);
        return this.possibleFits[type.ordinal()];
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Definitions) {
            Definitions other = (Definitions) obj;
            return this.width == other.width && this.height == other.height;
        } else {
            return false;
        }

    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + this.height;
        hash = 67 * hash + this.width;
        return hash;
    }

    public static final class FreeSpaces {
        public final int left;
        public final int right;

        public FreeSpaces(int left, int right) {
            this.left = left;
            this.right = right;
        }
    }

    // List of all the possible fits.
    private class GetPossibleFits implements Iterator<BlockPosition> {
        private final Tetromino.Type type;
        private final FreeSpaces[] rotations;

        private int maxX;
        private int currentRotation;
        private int currX;
        private boolean end;
        
        GetPossibleFits(Tetromino.Type type) {
            this.type = type;
            this.rotations = getFreeSpaces(this.type);
            
            this.currentRotation = -1;
            this.maxX = 0;
            this.end = false;
        }

        @Override
        public boolean hasNext() {
            if (this.end) {
                return false;
            }
            if ((this.maxX == 0 || this.currX > this.maxX) && (this.currentRotation + 1) >= this.rotations.length) {
                this.end = true;
                return false;
            }
            return true;
        }

        @Override
        public BlockPosition next() {
            if (this.end) {
                throw new NoSuchElementException();
            }
            if (this.maxX == 0 || this.currX > this.maxX) {
                this.maxX = 0;
                this.currentRotation++;
            }
            if (this.currentRotation >= this.rotations.length) {
                this.end = true;
                throw new NoSuchElementException();
            }
            if (maxX == 0) {
                FreeSpaces free = rotations[currentRotation];
                currX = 0 - free.left;
                maxX = (width - 4) + free.right;
            }
            return new BlockPosition(currX++, currentRotation, this.type);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
