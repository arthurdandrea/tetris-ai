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

package tetris;

import java.awt.Color;
import tetris.generic.Tetromino;

/**
 *
 * @author arthur
 */
public class ColorTheme {
    
    public static ColorThemeRegister create() {
        return new ColorThemeRegister();
    }
    public final Color[] colors;
    public final Color emptyColor;

    private ColorTheme(Color[] colors, Color emptyColor) {
        this.colors = colors;
        this.emptyColor = emptyColor;
    }
    
    public Color getColor(Tetromino.Type type) {
        return type == null ? emptyColor : colors[type.ordinal()];
    }
    
    public static class ColorThemeRegister {
        private final Color[] colors;
        private Color emptyColor;

        private ColorThemeRegister() {
            this.colors = new Color[Tetromino.Type.values().length];
        }
        
        public ColorThemeRegister set(Tetromino.Type type, Color color) {
            this.colors[type.ordinal()] = color;
            return this;
        }
        
        public ColorThemeRegister setEmpty(Color color) {
            this.emptyColor = color;
            return this;
        }
        
        public ColorTheme finish() {
            return new ColorTheme(colors, emptyColor);
        }
    }
}
