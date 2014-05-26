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

import java.net.InetAddress;
import java.net.UnknownHostException;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import tetris.generic.TetrisEngine;

/**
 *
 * @author Arthur D'Andréa Alemar
 * @author Natali Silva Honda
 */
public class NetworkTest {
    //@Rule
    //public Timeout globalTimeout = new Timeout(3000); // 10 seconds max per method tested

    /**
     * Test of start and stop methods, of class Network.
     */
    @Test
    public void testStart() throws InterruptedException {
        TetrisEngine localEngine = new TetrisEngine();
        TetrisEngine remoteEngine = new TetrisEngine();
        Network network = new Network(localEngine, remoteEngine);

        try {
            network.start();
        } finally {
            network.stop();
        }
    }    
    
    /**
     * Test of start and stop methods, of class Network.
     */
    @Test
    public void testConnect() throws UnknownHostException {
        TetrisEngine localEngine1 = new TetrisEngine();
        TetrisEngine remoteEngine1 = new TetrisEngine();
        TetrisEngine localEngine2 = new TetrisEngine();
        TetrisEngine remoteEngine2 = new TetrisEngine();

        localEngine1.startengine();
        localEngine2.startengine();
        remoteEngine1.startengine();
        remoteEngine2.startengine();

        Network localNetwork = new Network(localEngine1, remoteEngine1);
        Network remoteNetwork = new Network(localEngine2, remoteEngine2);

        try {
            localNetwork.start();
            remoteNetwork.start();
            remoteNetwork.connect(InetAddress.getByName("localhost"), localNetwork.getPort());
            localEngine2.keyslam();
            localEngine2.keyslam();
            localEngine1.keyslam();
            localEngine1.keyslam();
        } finally {
            localNetwork.stop();
            remoteNetwork.stop();
        }
    }    

}
