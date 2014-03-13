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
import tetris.ai.BlockPosition;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
public class TetrisGameDefinitions {

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
    public final int height;
    public final int width;
    private final FreeSpaces[][] freeSpaces;
    private final BlockPosition[][] possibleFits;

    public TetrisGameDefinitions(int width, int height) {
        this.width = width;
        this.height = height;
        this.freeSpaces = new FreeSpaces[TetrisEngine.blockdef.length][];
        for (int i = 0; i < TetrisEngine.blockdef.length; ++i) {
            this.freeSpaces[i] = new FreeSpaces[TetrisEngine.blockdef[i].length];
            for (int j = 0; j < TetrisEngine.blockdef[i].length; j++) {
                this.freeSpaces[i][j] = calculateFreeSpaces(TetrisEngine.blockdef[i][j]);
            }
        }
        this.possibleFits = new BlockPosition[TetrisEngine.blockdef.length][];
        for (int i = 0; i < TetrisEngine.blockdef.length; i++) {
            this.possibleFits[i] = Iterators.toArray(new GetPossibleFits(this, Tetromino.Type.values()[i]), BlockPosition.class);
        }
    }
    
    public BlockPosition[] getPossibleFits(Tetromino.Type type) {
        return this.possibleFits[type.ordinal()];
    }

    public FreeSpaces[] getFreeSpaces(Tetromino.Type type) {
        return this.freeSpaces[type.ordinal()];
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
    private static class GetPossibleFits implements Iterator<BlockPosition> {
        private final Tetromino.Type type;
        private final TetrisGameDefinitions definitions;
        private final FreeSpaces[] rotations;

        private int maxX;
        private int currentRotation;
        private int currX;
        private boolean end;
        
        GetPossibleFits(TetrisGameDefinitions definitions, Tetromino.Type type) {
            this.type = type;
            this.definitions = definitions;
            this.rotations = definitions.getFreeSpaces(this.type);
            
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
                maxX = (definitions.width - 4) + free.right;
            }
            return new BlockPosition(currX++, currentRotation, this.type);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
