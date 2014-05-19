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

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.Executors;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import tetris.ai.AbstractAI;
import tetris.ai.TetrisAI;
import tetris.generic.Score;
import tetris.generic.TetrisEngine;
import tetris.util.functional.PropertyListeners;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
class GamePanel {
    private static final Dimension DEFAULT_SIZE = new Dimension(20 * 4, 20 * 4);

    final JPanel sidebarPane;
    final TetrisEngine engine;
    final BoardPane board;
    
    private final AbstractAI ai;
    final AIExecutor aiExecutor;
    private final ListeningExecutorService executor;

    private final Drawer drawer;
    private final PreviewPiece previewPane;
    private final JLabel removeLinesLabel;
    private final JLabel removeLinesValue;
    private final JLabel scoreLabel;
    private final JLabel scoreValue;
    private final JLabel blocksDroppedLabel;
    private final JLabel blocksDroppedValue;

    GamePanel() {
        this.drawer = new Drawer();
        this.engine = new TetrisEngine();
        
        this.executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));
        this.ai = new TetrisAI(this.executor);
        this.aiExecutor = new AIExecutor(100, ai, engine);

        this.previewPane = new PreviewPiece(drawer);
        this.previewPane.setPreferredSize(DEFAULT_SIZE);
        this.previewPane.setMinimumSize(DEFAULT_SIZE);
        this.previewPane.setMaximumSize(DEFAULT_SIZE);
        this.scoreLabel = new JLabel("Score");
        this.scoreValue = new JLabel();
        this.removeLinesLabel = new JLabel("Removed Lines");
        this.removeLinesValue = new JLabel();
        this.blocksDroppedLabel = new JLabel("Blocks Dropped");
        this.blocksDroppedValue = new JLabel();
        this.board = new BoardPane(drawer, this.engine);
        this.board.setPreferredSize(new Dimension(this.engine.defs.width * 25, this.engine.defs.height * 25));
        this.sidebarPane = new JPanel();
        this.sidebarPane.setMaximumSize(new Dimension(DEFAULT_SIZE.width, Short.MAX_VALUE));
        this.sidebarPane.setLayout(new BoxLayout(this.sidebarPane, BoxLayout.PAGE_AXIS));
        this.sidebarPane.add(this.previewPane);
        this.sidebarPane.add(createLinePanel(this.scoreLabel, this.scoreValue));
        this.sidebarPane.add(createLinePanel(this.removeLinesLabel, this.removeLinesValue));
        this.sidebarPane.add(createLinePanel(this.blocksDroppedLabel, this.blocksDroppedValue));
        this.engine.addPropertyChangeListener("blocks", PropertyListeners.alwaysInSwing(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                board.repaint();
            }
        }));
        this.engine.addPropertyChangeListener("nextblock", PropertyListeners.alwaysInSwing(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                previewPane.setPiece(engine.getNextblock());
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
    }
    
    private static JPanel createLinePanel(JLabel label, JLabel value) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(10, 0)));
        panel.add(Box.createHorizontalGlue());
        panel.add(value);
        return panel;
    }
}
