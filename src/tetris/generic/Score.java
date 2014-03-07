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

/**
 *
 * @author arthur
 */
public class Score {
    private int score;

    /*
     * Lines cleared
     */
    private int linesRemoved;
    
    /*
     * How many blocks were dropped so far?
     */
    private int blocksDropped;

    public Score() {
        this.score = 0;
        this.linesRemoved = 0;
        this.blocksDropped = 0;
    }

    /**
     * @return the score
     */
    public int getScore() {
        return this.score;
    }

    /**
     * @return the linesRemoved
     */
    public int getLinesRemoved() {
        return this.linesRemoved;
    }

    /**
     * @return the blocksDropped
     */
    public int getBlocksDropped() {
        return this.blocksDropped;
    }
    
    public void addRemovedLines(int removedNow) {
        assert removedNow > 0;
        switch (removedNow) {
        case 1:
            this.score += 40;
            break;
        case 2:
            this.score += 100;
            break;
        case 3:
            this.score += 300;
            break;
        case 4:
            this.score += 1200;
            break;
        }
        this.linesRemoved += removedNow;
    }
    
    public void addDroppedBlock() {
        this.blocksDropped += 1;
        this.score += 1;
    }

    public Score Clone() {
        Score newScore = new Score();
        newScore.blocksDropped = this.blocksDropped;
        newScore.linesRemoved = this.linesRemoved;
        newScore.score = this.score;
        return newScore;
    }
}
