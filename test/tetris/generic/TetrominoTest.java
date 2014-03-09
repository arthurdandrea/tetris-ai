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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import tetris.generic.Tetromino.Type;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
public class TetrominoTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testConstructorNull() {
        exception.expect(NullPointerException.class);
        Tetromino t = new Tetromino(null, 0);
    }
    
    @Test
    public void testConstructorInvalidRotation() {
        exception.expect(IndexOutOfBoundsException.class);
        exception.expectMessage(startsWith("rotation"));
        Tetromino t = new Tetromino(Type.Box, 1);
    }

    /**
     * Test of toString method, of class Tetromino.
     */
    @Test
    public void testToString() {
        Tetromino.Type[] values = Tetromino.Type.values();
        for (Tetromino.Type type : values) {
            byte[][][] blockdef = TetrisEngine.blockdef[type.ordinal()];
            for (int rotation = 0; rotation < blockdef.length; rotation++) {
                Tetromino tetro = new Tetromino(type, rotation);
                assertThat(tetro.toString(), startsWith("Tetromino"));
                assertThat(tetro.toString(), containsString(type.toString()));
                assertThat(tetro.toString(), containsString(Integer.toString(rotation)));
            }
        }
    }
    
}
