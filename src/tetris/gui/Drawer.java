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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import tetris.generic.Block;
import tetris.generic.Tetromino;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
public final class Drawer {
    private static final Color DefaultColors[] = {
        new Color(255, 255, 255), // NoShape
        new Color(204, 102, 102), // ZShape
        new Color(102, 204, 102), // SShape
        new Color(102, 102, 204), // LineShape
        new Color(204, 204, 102), // TShape
        new Color(204, 102, 204), // SquareShape
        new Color(102, 204, 204), // LShape
        new Color(218, 170, 0)    // MirroredLShape
    };
    
    private Color colors[];
    private Color darkerColors[];
    private Color brighterColors[];

    public Drawer() {
        this.setColors(DefaultColors);
    }
    
    public Dimension getSquareDimension(Dimension size, int BoardWidth, int BoardHeight) {
        Dimension square = new Dimension();
        square.width = size.width / BoardWidth;
        square.height = size.height / BoardHeight;
        if (square.width > square.height) {
            square.width = square.height;
        } else if (square.width < square.height) {
            square.height = square.width;
        }
        return square;
    }

    public void setColors(Color[] colors) {
        assert colors.length == 8;

        this.colors = new Color[8];
        this.darkerColors = new Color[8];
        this.brighterColors = new Color[8];
        for (int i = 0; i < colors.length; i++) {
            this.colors[i] = colors[i];
            this.darkerColors[i] = colors[i].darker();
            this.brighterColors[i] = colors[i].brighter();
        }
    }
    
    public void draw(Graphics graphic, Dimension size, int BoardWidth, int BoardHeight, Block[][] array) {
        Dimension square = this.getSquareDimension(size, BoardWidth, BoardHeight);
        int deslocx = (size.width - (square.width * BoardWidth)) / 2;
        int deslocy = (size.height - (square.height * BoardHeight)) / 2;
        Tetromino.Type type;
        //assert BoardWidth == array.length;
        int x, xLinha, y, yLinha, ordinal = 0;
        for (int i = 0; i < BoardWidth; ++i) {
            //assert BoardHeight == array[i].length;
            x = deslocx + (i * square.width);
            xLinha = deslocx + ((i + 1) * square.width) - 1;
            for (int j = 0; j < BoardHeight; ++j) {
                y = deslocy + (j * square.height);
                yLinha = deslocy + ((j + 1) * square.height) - 1;
                
                if (array != null) {
                    type = array[i][j].getType();
                    ordinal = type == null ? 0 : type.ordinal() + 1;
                }

                graphic.setColor(colors[ordinal]);
                graphic.fillRect(x + 1, y + 1, square.width - 2, square.height - 2);

                graphic.setColor(brighterColors[ordinal]);
                graphic.drawLine(x, yLinha, x, y);

                graphic.drawLine(x, y, xLinha, y);

                graphic.setColor(darkerColors[ordinal]);
                graphic.drawLine(x + 1, yLinha, xLinha, yLinha);
                graphic.drawLine(xLinha, yLinha, xLinha, y + 1);
            }
        }
    }

    public void drawSquare(Graphics graphic, int j, int i, Tetromino.Type shape, Dimension square) {
        this.drawSquare(graphic, j, i, 0, 0, shape, square);
    }
    public void drawSquare(Graphics graphic, int i, int j, int deslocx, int deslocy, Tetromino.Type shape, Dimension square) {
        final int x = deslocx + (i * square.width);
        final int y = deslocy + (j * square.height);
        final int yLinha = deslocy + ((j + 1) * square.height) - 1;
        final int xLinha = deslocx + ((i + 1) * square.width) - 1;
        final int ordinal = shape == null ? 0 : shape.ordinal() + 1;

        graphic.setColor(colors[ordinal]);
        graphic.fillRect(x + 1, y + 1, square.width - 2, square.height - 2);

        graphic.setColor(brighterColors[ordinal]);
        graphic.drawLine(x, yLinha, x, y);
        
        graphic.drawLine(x, y, xLinha, y);

        graphic.setColor(darkerColors[ordinal]);
        graphic.drawLine(x + 1, yLinha, xLinha, yLinha);
        graphic.drawLine(xLinha, yLinha, xLinha, y + 1);
    }
}
