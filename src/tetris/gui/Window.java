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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.util.Iterator;
import java.util.concurrent.Executors;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
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
import tetris.util.functional.SwingPropertyChangeListener;

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
    private BoardPane boardPane;
    private JPanel contentPane;
    private JPanel sidebarPane;
    private NewAIExecutor aiExecutor;
    private ControlsPanel controlsPanel;
    private JLabel removeLinesLabel;
    private JLabel removeLinesValue;
    private JLabel scoreLabel;
    private JLabel scoreValue;
    private JLabel blocksDroppedLabel;
    private JLabel blocksDroppedValue;
    
    public Window() {
        this(MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(6)));
    }

    public Window(ListeningExecutorService executor) {
        this.executor = executor;
        initializeTetris();
        initializeMenu();
        initializeComponents();
        this.engine.setState(tetris.generic.TetrisEngine.GameState.PLAYING);
        this.engine.startengine();
        this.pack();
    }
    
    private void initializeTetris() {
        this.velocities = defaultVelocities.iterator();
        this.engine = new TetrisEngine();
        this.ai = new TetrisAI(this.executor);
        this.aiExecutor = new NewAIExecutor(100, ai, engine);

        this.engine.addPropertyChangeListener("blocks", new SwingPropertyChangeListener() {
            @Override
            public void onPropertyChange(PropertyChangeEvent evt) {
                boardPane.repaint();
            }
        });
        this.engine.addPropertyChangeListener("nextblock", new SwingPropertyChangeListener() {
            @Override
            public void onPropertyChange(PropertyChangeEvent evt) {
                previewPane.setPiece(Window.this.engine.getNextblock());
            }
        });
        this.engine.addPropertyChangeListener("score", new SwingPropertyChangeListener() {
            @Override
            protected void onPropertyChange(PropertyChangeEvent evt) {
                Score score = engine.getScore();
                scoreValue.setText(String.format("%06d", score.getScore()));
                removeLinesValue.setText(String.format("%06d", score.getLinesRemoved()));
                blocksDroppedValue.setText(String.format("%06d", score.getBlocksDropped()));
            }
        });
    }

    private void initializeMenu() {
        boolean isMac = System.getProperty("os.name").toLowerCase().indexOf("mac") != -1;
        
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
        this.sidebarPane.add(Box.createRigidArea(new Dimension(0, 5)));
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
        {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
            panel.add(Box.createHorizontalGlue());
            panel.add(new JLabel("Controls"));
            panel.add(Box.createHorizontalGlue());
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
        
        this.sidebarPane.add(Box.createVerticalGlue());
        this.sidebarPane.add(Box.createRigidArea(new Dimension(0, 5)));

        this.boardPane = new BoardPane(this.drawer, this.engine);
        this.boardPane.setPreferredSize(new Dimension(this.engine.defs.width*25, this.engine.defs.height*25));
        

        this.contentPane = new JPanel();
        this.contentPane.setLayout(new BoxLayout(this.contentPane, BoxLayout.LINE_AXIS));
        this.contentPane.add(Box.createRigidArea(new Dimension(5, 0)));
        this.contentPane.add(this.boardPane);
        this.contentPane.add(Box.createRigidArea(new Dimension(10, 0)));
        this.contentPane.add(this.sidebarPane);
        this.contentPane.add(Box.createRigidArea(new Dimension(5, 0)));

        this.add(this.contentPane);
        
        
        this.setTitle("Tetris");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.addKeyListener(new KeyAdapterImpl());
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
                // Board.this.setGameStatus("konami!");
                System.out.println("konami");
            }
            // if (!Board.this.isRunning || Board.this.curPiece.getShape() == Tetrominoes.NoShape) {
            //     return;
            // }

            switch (e.getKeyCode()) {
                case KeyEvent.VK_A:
                    if (aiExecutor.isRunning()) {
                        controlsPanel.setControlLabel(5, "Activate AI");
                        for (int i = 1; i < 5; i++) {
                            controlsPanel.enableControl(i);
                        }
                        controlsPanel.disableControl(6);
                        aiExecutor.stop();
                    } else {
                        controlsPanel.setControlLabel(5, "Deactivate AI");
                        for (int i = 1; i < 5; i++) {
                            controlsPanel.disableControl(i);
                        }
                        controlsPanel.enableControl(6);
                        velocities = defaultVelocities.iterator();
                        aiExecutor.setDelay(velocities.next());
                        aiExecutor.start();
                    }
                    break;
                case KeyEvent.VK_V:
                    if (aiExecutor.isRunning()) {
                        aiExecutor.setDelay(velocities.next());
                    }
                    break;
                case KeyEvent.VK_P:
                    if (engine.getState() != GameState.PLAYING) {
                        controlsPanel.setControlLabel(0, "Pause");
                        engine.setState(GameState.PLAYING);
                    } else {
                        controlsPanel.setControlLabel(0, "Resume");
                        engine.setState(GameState.PAUSED);
                    }
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
