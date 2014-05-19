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

import java.util.Objects;
import java.util.logging.Logger;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
public class BlockMover {
    private static final Logger logger = Logger.getLogger(BlockMover.class.getName());

    private final int initialRotation;
    private final BlockPosition position;
    private final TetrisEngine engine;
    private boolean positionEnd;
    private boolean end;

    public BlockMover(TetrisEngine engine, BlockPosition position) {
        Objects.requireNonNull(engine);
        Objects.requireNonNull(position);
        this.engine = engine;
        this.position = position;
        this.positionEnd = false;
        this.end = false;
        /* we're going to make another failsafe here: if at any time we rotate
        it or move it and it doesn't move then it's stuck and we give up. */
        this.initialRotation = engine.getActiveblock().rot;
    }

    public boolean hasMoreMoves() {
        return !this.end;
    }

    public void move() {
        if (this.end) {
            
        } else if (this.positionEnd) {
            if (!engine.keydown()) {
                this.end = true;
            }
        } else {
            positionTheBlock();
        }
    }

    private void positionTheBlock() {
        Tetromino activeblock = engine.getActiveblock();
        // Rotate first so we don't get stuck in the edges.
        if (activeblock.rot != position.rot) {
            // Check if it worked
            if (!engine.keyrotate() || initialRotation == engine.getActiveblock().rot) {
                this.end = true;
                logger.warning(String.format("could not rotate active block to rot=%d", position.rot));
                engine.keyslam();
            }
            return;
        }
        if (activeblock.x != position.bx) {
            // Check if it worked
            if (activeblock.x < position.bx ? !engine.keyright() : !engine.keyleft()) {
                this.end = true;
                logger.warning(String.format("could not move active block to x=%d", position.bx));
                engine.keyslam();
            }
            return;
        }
        this.positionEnd = true;
    }

    public void slam() {
        if (this.end) return;
        while (!this.positionEnd && !this.end) {
            this.positionTheBlock();
        }
        this.engine.keyslam();
        this.end = true;
    }
    
}
