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
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.concurrent.Executors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import tetris.ai.AbstractAI;
import tetris.ai.TetrisAI;
import tetris.generic.Score;
import tetris.generic.TetrisEngine;
import tetris.generic.TetrisEngine.GameState;
import tetris.util.functional.PropertyListeners;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
public class Window extends JFrame {
    private static final Iterable<Integer> defaultVelocities = Iterables.cycle(99, 66, 33, 0);
    
    private Iterator<Integer> velocities; 
    private Drawer drawer;
    private PreviewPiece previewPane;
    private JMenuBar menuBar;
    private JMenu jogoMenu;
    private JMenuItem pauseItem;
    private JMenuItem restartItem;
    private TetrisEngine engine;
    private AbstractAI ai;
    private ListeningExecutorService executor;
    private BoardPane board;
    private JPanel lineContentPanel;
    private JPanel sidebarPane;
    private AIExecutor aiExecutor;
    private ControlsPanel controlsPanel;
    
    private JLabel mainLabel;
    private JLabel removeLinesLabel;
    private JLabel removeLinesValue;
    private JLabel scoreLabel;
    private JLabel scoreValue;
    private JLabel blocksDroppedLabel;
    private JLabel blocksDroppedValue;
    
    private JPanel aiPanel;
    private JLabel aiLabel;
    private JLabel aiVelocityLabel;
    private JLabel aiVelocityValue;
    private JPanel pageContentPanel;

    public Window() {
        this(MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(6)));
    }

    public Window(ListeningExecutorService executor) {
        this.executor = executor;
        initializeTetris();
        initializeMenu();
        initializeComponents();
        this.engine.startengine();
        this.pack();
    }
    
    private void initializeTetris() {
        this.engine = new TetrisEngine();
        this.ai = new TetrisAI(this.executor);
        this.aiExecutor = new AIExecutor(100, ai, engine);

        this.engine.addPropertyChangeListener("blocks", PropertyListeners.alwaysInSwing(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                board.repaint();
            }
        }));
        this.engine.addPropertyChangeListener("nextblock", PropertyListeners.alwaysInSwing(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                previewPane.setPiece(Window.this.engine.getNextblock());
            }
        }));
        this.engine.addPropertyChangeListener("score", PropertyListeners.alwaysInSwing(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                Score score = engine.getScore();
                scoreValue.setText(String.format("%06d", score.getScore()));
                removeLinesValue.setText(String.format("%06d", score.getLinesRemoved()));
                blocksDroppedValue.setText(String.format("%06d", score.getBlocksDropped()));
            }
        }));
        this.engine.addPropertyChangeListener("state", PropertyListeners.alwaysInSwing(new PropertyChangeListener() {
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

    private void initializeMenu() {
        boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");
        
        this.menuBar = new JMenuBar();
	this.jogoMenu = new JMenu("Jogo");
	this.restartItem = new JMenuItem("Reiniciar");
        this.restartItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, isMac ? Event.META_MASK : Event.CTRL_MASK));
        this.restartItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //boardPane.start();
            }
        });
        this.pauseItem = new JMenuItem("Pausar");
        this.pauseItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
//                if (timer.isRunning()) {
//                    timer.stop();
//                } else {
//                    timer.start();
//                }
            }
        });
	this.jogoMenu.add(this.restartItem);
        this.jogoMenu.add(this.pauseItem);
	this.menuBar.add(this.jogoMenu);
	this.setJMenuBar(this.menuBar);
    }

    private void initializeComponents() {
        this.drawer = new Drawer();

        Dimension size = new Dimension(20*4, 20*4);
        this.previewPane = new PreviewPiece(this.drawer);
        this.previewPane.setPreferredSize(size);
        this.previewPane.setMinimumSize(size);
        this.previewPane.setMaximumSize(size);

        this.sidebarPane = new JPanel();
        this.sidebarPane.setMaximumSize(new Dimension(size.width, Short.MAX_VALUE));
        this.sidebarPane.setLayout(new BoxLayout(this.sidebarPane, BoxLayout.PAGE_AXIS));
        this.sidebarPane.add(this.previewPane);
        this.scoreLabel = new JLabel("Score");
        this.scoreValue = new JLabel();
        this.removeLinesLabel = new JLabel("Removed Lines");
        this.removeLinesValue = new JLabel();
        this.blocksDroppedLabel = new JLabel("Blocks Dropped");
        this.blocksDroppedValue = new JLabel();
        
        this.sidebarPane.add(createLinePanel(this.scoreLabel, this.scoreValue));
        this.sidebarPane.add(createLinePanel(this.removeLinesLabel, this.removeLinesValue));
        this.sidebarPane.add(createLinePanel(this.blocksDroppedLabel, this.blocksDroppedValue));
        this.sidebarPane.add(Box.createRigidArea(new Dimension(0, 10)));
        {
            JLabel label = new JLabel("Controls");
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
            panel.add(label);
            this.sidebarPane.add(panel);
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
        this.controlsPanel.disableControl(6);

        this.sidebarPane.add(this.controlsPanel);

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

        this.sidebarPane.add(Box.createRigidArea(new Dimension(0, 10)));
        this.sidebarPane.add(this.aiLabel);
        this.sidebarPane.add(this.aiPanel);

        this.sidebarPane.add(Box.createVerticalGlue());

        this.mainLabel = new JLabel("Tetris");
        this.mainLabel.setFont(this.mainLabel.getFont().deriveFont(this.mainLabel.getFont().getSize2D() * 2.0f));

        this.board = new BoardPane(this.drawer, this.engine);
        this.board.setPreferredSize(new Dimension(this.engine.defs.width*25, this.engine.defs.height*25));
        this.board.addKeyListener(new KeyAdapterImpl());
        this.board.setFocusable(true);

        this.lineContentPanel = new JPanel();
        this.lineContentPanel.setLayout(new BoxLayout(this.lineContentPanel, BoxLayout.LINE_AXIS));
        this.lineContentPanel.add(this.board);
        this.lineContentPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        this.lineContentPanel.add(this.sidebarPane);

        this.pageContentPanel = new JPanel();
        this.pageContentPanel.setLayout(new BoxLayout(this.pageContentPanel, BoxLayout.PAGE_AXIS));
        this.pageContentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        this.pageContentPanel.add(createCentralizedPanel(this.mainLabel));
        this.pageContentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        this.pageContentPanel.add(this.lineContentPanel);
        this.add(this.pageContentPanel);

        this.setTitle("Tetris");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    
    private JPanel createCentralizedPanel(JLabel label) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        panel.add(Box.createHorizontalGlue());
        panel.add(label);
        panel.add(Box.createHorizontalGlue());
        return panel;
    }
    
    private JPanel createLinePanel(JLabel label, JLabel value) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(10, 0)));
        panel.add(Box.createHorizontalGlue());
        panel.add(value);
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
                System.out.println("konami");
            }

            switch (e.getKeyCode()) {
                case KeyEvent.VK_A:
                    if (aiExecutor.isRunning()) {
                        aiPanel.setVisible(false);
                        aiLabel.setVisible(false);
                        
                        controlsPanel.setControlLabel(5, "Activate AI");
                        for (int i = 1; i < 5; i++) {
                            controlsPanel.enableControl(i);
                        }
                        controlsPanel.disableControl(6);
                        aiExecutor.stop();
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
                        aiExecutor.start();
                    }
                    break;
                case KeyEvent.VK_V:
                    if (aiExecutor.isRunning()) {
                        setNextAIVelocity();
                    }
                    break;
                case KeyEvent.VK_P:
                    engine.tooglePause();
                    break;
                case KeyEvent.VK_LEFT:
                    if (engine.getState() == GameState.PLAYING && !aiExecutor.isRunning()) {
                        engine.keyleft();
                    }
                    break;
                case KeyEvent.VK_RIGHT:
                    if (engine.getState() == GameState.PLAYING && !aiExecutor.isRunning()) {
                        engine.keyright();
                    }
                    break;
                case KeyEvent.VK_DOWN:
                    if (engine.getState() == GameState.PLAYING && !aiExecutor.isRunning()) {
                        engine.keydown();
                    }
                    break;
                case KeyEvent.VK_UP:
                    if (engine.getState() == GameState.PLAYING && !aiExecutor.isRunning()) {
                        engine.keyrotate();
                    }
                    break;
                case KeyEvent.VK_SPACE:
                    if (engine.getState() == GameState.PLAYING && !aiExecutor.isRunning()) {
                        engine.keyslam();
                    }
                    break;
            }
        }
    }

    private void setNextAIVelocity() {
        int velocity = velocities.next();
        aiVelocityValue.setText(Integer.toString(velocity));
        aiExecutor.setDelay(velocity);
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
