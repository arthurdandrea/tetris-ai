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
import tetris.generic.Block;
import tetris.generic.Tetromino;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
public class PreviewPiece extends JComponent {
    private final Drawer drawer;
    private Tetromino piece;

    public PreviewPiece() {
        this(new Drawer());
    }

    public PreviewPiece(Drawer drawer) {
        this.drawer = drawer;
    }

    public Tetromino getPiece() {
        return piece;
    }
    
    public void setPiece(Tetromino piece) {
        this.piece = piece;
        this.repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Block[][] array = this.piece == null ? null : this.piece.array;
        if (array != null) {
            array = transposeMatrix(array);
        }
        this.drawer.draw(g, this.getSize(), 4, 4, array);
    }
    
    private static Block[][] transposeMatrix(Block [][] m){
        Block[][] temp = new Block[m[0].length][m.length];
        for (int i = 0; i < m.length; i++)
            for (int j = 0; j < m[0].length; j++)
            temp[j][i] = m[i][j];
        return temp;
    }
}

