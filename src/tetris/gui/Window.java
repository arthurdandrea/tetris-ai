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

import com.google.common.collect.Iterables;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import tetris.generic.TetrisEngine.GameState;
import tetris.net.Network;
import tetris.net.Network.ConnectionState;
import tetris.net.TCPNetwork;
import tetris.util.functional.PropertyListeners;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
public class Window extends JFrame {
    private static final Logger logger = Logger.getLogger(Window.class.getName());
    private static final Iterable<Integer> defaultVelocities = Iterables.cycle(99, 66, 33, 0);
    
    private Iterator<Integer> velocities;

    private GamePanel gameRight;
    private GamePanel gameLeft;
        
    private JPanel lineContentPanel;
    private ControlsPanel controlsPanel;
    
    private JLabel mainLabel;
    
    private JPanel aiPanel;
    private JLabel aiLabel;
    private JLabel aiVelocityLabel;
    private JLabel aiVelocityValue;
    private JPanel pageContentPanel;
    private Network network;
    private NetworkInfoPanel networkLabel;
    private ChatPanel chatPanel;

    public Window() {
        this.initializeTetris();
        this.initializeComponents();
        this.gameRight.engine.startengine();
        this.gameLeft.engine.startengine();
        this.pack();
        //this.gameLeft.aiExecutor.start();
    }
    
    private void initializeTetris() {
        this.gameRight = new GamePanel();
        this.gameLeft = new GamePanel();
        this.network = new TCPNetwork(this.gameRight.engine, this.gameLeft.engine);
        this.network.start();
        this.chatPanel = new ChatPanel(this.network);
        this.network.addPropertyChangeListener("connectionState", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                ConnectionState state = (ConnectionState) evt.getNewValue();
                if (state == ConnectionState.CONNECTED) {
                    chatPanel.clear();
                    chatPanel.setVisible(true);
                    pack();
                } else if (state == ConnectionState.DISCONNECTED) {
                    chatPanel.clear();
                    chatPanel.setVisible(false);
                    pack();
                }
            }
        });
        
        
        this.gameRight.engine.addPropertyChangeListener("state", PropertyListeners.alwaysInSwing(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                GameState state = (GameState) evt.getNewValue();
                switch (state) {
                    case GAMEOVER:
                        mainLabel.setText("Tetris - GAME OVER");
                        controlsPanel.setControlLabel(0, "Restart");
                        break;
                    case PAUSED:
                        mainLabel.setText("Tetris - PAUSED");
                        controlsPanel.setControlLabel(0, "Resume");
                        break;
                    case PLAYING:
                        mainLabel.setText("Tetris");
                        controlsPanel.setControlLabel(0, "Pause");
                        break;
                }
            }
        }));
    }

    private void initializeComponents() {
        this.gameLeft.sidebarPane.add(Box.createRigidArea(new Dimension(0, 10)));
        this.chatPanel.setVisible(false);
        this.gameLeft.sidebarPane.add(this.chatPanel);
        
        this.gameRight.sidebarPane.add(Box.createRigidArea(new Dimension(0, 10)));
        {
            JLabel label = new JLabel("Controls");
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
            panel.add(label);
            this.gameRight.sidebarPane.add(panel);
        }
        
        String labels[][] = {
            {"Pause",       "P"},
            {"Move Left",   "←"},
            {"Move Right",  "→"},
            {"Rotate",      "↑"},
            {"Drop",        "␣"},
            {"Activate AI", "a"},
            {"AI velocity", "v"}
        };

        this.controlsPanel = new ControlsPanel();
        for (String[] label : labels) {
            this.controlsPanel.addControl(label[1], label[0]);
        }
        this.controlsPanel.disableControl(5);
        this.controlsPanel.disableControl(6);

        this.gameRight.sidebarPane.add(this.controlsPanel);

        this.aiLabel = new JLabel("AI");
        this.aiLabel.setVisible(false);
        this.aiLabel.setFont(this.aiLabel.getFont().deriveFont(Font.BOLD));
        this.aiVelocityLabel = new JLabel("AI Velocity");
        this.aiVelocityValue = new JLabel();

        this.aiPanel = new JPanel();
        this.aiPanel.setLayout(new BoxLayout(this.aiPanel, BoxLayout.LINE_AXIS));
        this.aiPanel.setVisible(false);

        this.aiPanel.add(this.aiVelocityLabel);
        this.aiPanel.add(Box.createHorizontalGlue());
        this.aiPanel.add(this.aiVelocityValue);

        this.gameRight.sidebarPane.add(Box.createRigidArea(new Dimension(0, 10)));
        this.gameRight.sidebarPane.add(this.aiLabel);
        this.gameRight.sidebarPane.add(this.aiPanel);

        this.mainLabel = new JLabel("Tetris");
        this.mainLabel.setFont(this.mainLabel.getFont().deriveFont(this.mainLabel.getFont().getSize2D() * 2.0f));

        this.gameRight.board.setFocusable(true);
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                gameRight.board.requestFocusInWindow();
            }
        };
        this.gameRight.board.addMouseListener(mouseAdapter);
        this.gameRight.sidebarPane.addMouseListener(mouseAdapter);
        this.gameRight.board.requestFocusInWindow();
        this.gameRight.board.addKeyListener(new KeyAdapterImpl());

        this.lineContentPanel = new JPanel();
        this.lineContentPanel.setLayout(new BoxLayout(this.lineContentPanel, BoxLayout.LINE_AXIS));
        this.lineContentPanel.add(this.gameLeft.board);
        this.lineContentPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        this.lineContentPanel.add(this.gameLeft.sidebarPane);
        this.lineContentPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        this.lineContentPanel.add(this.gameRight.board);
        this.lineContentPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        this.lineContentPanel.add(this.gameRight.sidebarPane);

        this.networkLabel = new NetworkInfoPanel(network);
        this.pageContentPanel = new JPanel();
        this.pageContentPanel.setLayout(new BoxLayout(this.pageContentPanel, BoxLayout.PAGE_AXIS));
        this.pageContentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        this.pageContentPanel.add(createCentralizedPanel(this.mainLabel));
        this.pageContentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        this.pageContentPanel.add(this.lineContentPanel);
        this.pageContentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        this.pageContentPanel.add(this.networkLabel);
        this.add(this.pageContentPanel);

        this.setTitle("Tetris");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    
    private static JPanel createCentralizedPanel(JLabel label) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        panel.add(Box.createHorizontalGlue());
        panel.add(label);
        panel.add(Box.createHorizontalGlue());
        return panel;
    }
        
    private class KeyAdapterImpl extends KeyAdapter {
        private final KonamiCode konamiCode;

        public KeyAdapterImpl() {
            this.konamiCode = new KonamiCode();
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (konamiCode.consume(e)) {
                controlsPanel.enableControl(5);
            }

            switch (e.getKeyCode()) {
                case KeyEvent.VK_A:
                    if (controlsPanel.isEnabledControl(5)) {
                        if (gameRight.aiExecutor.isRunning()) {
                            aiPanel.setVisible(false);
                            aiLabel.setVisible(false);

                            controlsPanel.setControlLabel(5, "Activate AI");
                            for (int i = 1; i < 5; i++) {
                                controlsPanel.enableControl(i);
                            }
                            controlsPanel.disableControl(6);
                            gameRight.aiExecutor.stop();
                        } else {
                            aiPanel.setVisible(true);
                            aiLabel.setVisible(true);

                            controlsPanel.setControlLabel(5, "Deactivate AI");
                            for (int i = 1; i < 5; i++) {
                                controlsPanel.disableControl(i);
                            }
                            controlsPanel.enableControl(6);
                            velocities = defaultVelocities.iterator();
                            setNextAIVelocity();
                            gameRight.aiExecutor.start();
                        }
                    }
                    break;
                case KeyEvent.VK_C:
                    String address = JOptionPane.showInputDialog(Window.this, "Digite o ip e a porta do outro jogador");
                    if (address == null || address.isEmpty()) {
                        break;
                    }
                    URI uri = TCPNetwork.parseHostPort(address);
                    gameLeft.aiExecutor.stop();
                    try {
                        network.connect(InetAddress.getByName(uri.getHost()), uri.getPort());
                    } catch (UnknownHostException ex) {
                        logger.log(Level.SEVERE, null, ex);
                        gameLeft.aiExecutor.start();
                    }
                    break;
                case KeyEvent.VK_V:
                    if (gameRight.aiExecutor.isRunning()) {
                        setNextAIVelocity();
                    }
                    break;
                case KeyEvent.VK_P:
                    gameRight.engine.tooglePause();
                    break;
                case KeyEvent.VK_M:
                    chatPanel.setFocusToTextArea();
                    break;
                case KeyEvent.VK_LEFT:
                    if (gameRight.engine.getState() == GameState.PLAYING && !gameRight.aiExecutor.isRunning()) {
                        gameRight.engine.keyleft();
                    }
                    break;
                case KeyEvent.VK_RIGHT:
                    if (gameRight.engine.getState() == GameState.PLAYING && !gameRight.aiExecutor.isRunning()) {
                        gameRight.engine.keyright();
                    }
                    break;
                case KeyEvent.VK_DOWN:
                    if (gameRight.engine.getState() == GameState.PLAYING && !gameRight.aiExecutor.isRunning()) {
                        gameRight.engine.keydown();
                    }
                    break;
                case KeyEvent.VK_UP:
                    if (gameRight.engine.getState() == GameState.PLAYING && !gameRight.aiExecutor.isRunning()) {
                        gameRight.engine.keyrotate();
                    }
                    break;
                case KeyEvent.VK_SPACE:
                    if (gameRight.engine.getState() == GameState.PLAYING && !gameRight.aiExecutor.isRunning()) {
                        gameRight.engine.keyslam();
                    }
                    break;
            }
        }
    }

    private void setNextAIVelocity() {
        int velocity = velocities.next();
        aiVelocityValue.setText(Integer.toString(velocity));
        gameRight.aiExecutor.setDelay(velocity);
    }

    public static void main(String[] args) {
        try {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Test");
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException |
                IllegalAccessException | UnsupportedLookAndFeelException e) {
            System.out.println(e);
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Window window = new Window();
                window.setLocationRelativeTo(null);
                window.setVisible(true);
            }
        });
    }
}
