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
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Arthur D'Andréa Alemar
 * @author Natali Silva Honda
 */
public class NetworkTest {

    /**
     * Test of start and stop methods, of class Network.
     */
    @Test
    public void testStart() throws InterruptedException {
        Network network = new Network();

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
        Network serverNetwork = new Network();
        Network clientNetwork = new Network();

        try {
            serverNetwork.start();
            clientNetwork.start();
            clientNetwork.connect(InetAddress.getByName("localhost"), serverNetwork.getPort());
        } finally {
            serverNetwork.stop();
            clientNetwork.stop();
        }
    }    

}
