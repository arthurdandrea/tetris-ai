/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tetris.generic;

/**
 *
 * @author arthur
 */
public class Score implements Cloneable {
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

    @Override
    protected Object clone() throws CloneNotSupportedException {
        Score newScore = new Score();
        newScore.blocksDropped = this.blocksDropped;
        newScore.linesRemoved = this.linesRemoved;
        newScore.score = this.score;
        return newScore;
    }
}
