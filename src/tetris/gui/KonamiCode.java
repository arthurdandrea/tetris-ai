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

import java.awt.event.KeyEvent;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
public final class KonamiCode {
    static final char[] THE_CODE = new char[] {'u', 'u', 'd', 'd', 'l', 'r', 'l', 'r', 'b', 'a'};

    private int index;

    public KonamiCode() {
        this.index = 0;
    }
    
    public boolean consume(KeyEvent e) {
        char code = decodeKeyCode(e.getKeyCode());
        if (KonamiCode.THE_CODE[this.index] == code) {
            this.index++;
            if (this.index == KonamiCode.THE_CODE.length) {
                this.index = 0;
                return true;
            }
            return false;
        } else {
            this.index = 0;
            return false;
        }
    }
    
    private static char decodeKeyCode(int keycode) {
        switch(keycode) {
            case KeyEvent.VK_LEFT:
                return 'l';
            case KeyEvent.VK_RIGHT:
                return 'r';
            case KeyEvent.VK_DOWN:
                return 'd';
            case KeyEvent.VK_UP:
                return 'u';
            case 'B':
            case 'b':
                return 'b';
            case 'A':
            case 'a':
                return 'a';
            default:
                return '\0';
        }
    }
}

