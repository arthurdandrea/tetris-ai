/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tetris;

import java.awt.Color;
import tetris.generic.Tetromino;

/**
 *
 * @author arthur
 */
public class ColorTheme {
    public final Color[] colors;
    public final Color emptyColor;

    private ColorTheme(Color[] colors, Color emptyColor) {
        this.colors = colors;
        this.emptyColor = emptyColor;
    }
    
    public static ColorThemeRegister create() {
        return new ColorThemeRegister();
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
