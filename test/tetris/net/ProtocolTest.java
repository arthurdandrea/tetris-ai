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

package tetris.net;

import org.junit.Test;
import static org.junit.Assert.*;
import tetris.generic.TetrisEngine;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
public class ProtocolTest {
    @Test
    public void testSerialization() {
        Protocol protocol = Protocol.create();
        TetrisEngine engine1 = new TetrisEngine();
        engine1.startengine();
        
        TetrisEngine engine2 = new TetrisEngine();
        engine2.startengine();
        
        engine2.loadCompleteState(protocol.decodeCompleteState(protocol.encodeCompleteState(engine1.dumpCompleteState())));
        
        assertTrue(engine1.equals(engine2));
        assertTrue(engine2.equals(engine1));
    }
}
