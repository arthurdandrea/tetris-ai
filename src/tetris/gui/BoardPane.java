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

import java.awt.Graphics;
import javax.swing.JComponent;
import tetris.generic.TetrisEngine;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
public class BoardPane extends JComponent {
    private final int width;
    private final int height;

    private final Drawer drawer;
    private final TetrisEngine engine;
    
    public BoardPane() {
        this.width = 6;
        this.height = 22;
        this.drawer = new Drawer();
        this.engine = null;
        this.setFocusable(true);
    }

    public BoardPane(TetrisEngine engine) {
        this(new Drawer(), engine);
    }

    public BoardPane(Drawer drawer, TetrisEngine engine) {
        this.width = engine.defs.width;
        this.height = engine.defs.height;
        this.drawer = drawer;
        this.engine = engine;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        this.drawer.draw(g, this.getSize(), this.width,
                         this.height, this.engine != null ? this.engine.getBlocks() : null);
    }
    
}
