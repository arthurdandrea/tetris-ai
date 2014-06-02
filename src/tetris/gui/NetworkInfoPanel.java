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

package tetris.gui;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import tetris.net.ConnectionListener;
import tetris.net.Network;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
public class NetworkInfoPanel extends JPanel {
    private static final Logger logger = Logger.getLogger(NetworkInfoPanel.class.getName());
    private final Network network;
    private final JLabel label;
    private String ipPort;

    public NetworkInfoPanel(Network network) {
        this.label = new JLabel();
        this.ipPort = null;
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (ipPort == null || ipPort.isEmpty()) {
                    return;
                }
                StringSelection data = new StringSelection(ipPort);
                Clipboard clipboard = 
                     Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(data, data);
            }
        });

        this.network = network;
        this.network.addConnectionListener(new ConnectionListener() {
            @Override
            public void onConnected() {
                updateDisplay();
            }

            @Override
            public void onDisconnected() {
                updateDisplay();
            }
        });
        
        this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        this.add(Box.createHorizontalGlue());
        this.add(this.label);
        this.add(Box.createHorizontalGlue());
        this.updateDisplay();
    }

    private void updateDisplay() {
        SocketAddress remoteAddress = this.network.getRemoteAddress();
        if (remoteAddress != null) {
            this.label.setText("Você está conectado a " + remoteAddress);
            this.ipPort = null;
        } else {
            String hostAddress = null;
            try {
                hostAddress = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
            this.ipPort = hostAddress + ":" + Integer.toString(this.network.getPort());
            this.label.setText("Seu IP e porta para conexão: " + ipPort);
        }

    } 
}
