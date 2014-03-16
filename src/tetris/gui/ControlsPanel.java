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

import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
public class ControlsPanel extends JPanel {
    private final List<Control> controls;

    public ControlsPanel() {
        this.controls = new ArrayList<>();
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    }

    public int addControl(String key, String label) {
        Control control = new Control(key, label);
        this.add(control.panel);
        this.controls.add(control);
        return controls.size() - 1;
    }

    public void setControlLabel(int i, String label) {
        Control control = this.controls.get(i);
        control.setLabel(label);
    }

    public void disableControl(int i) {
        Control control = this.controls.get(i);
        control.panel.setVisible(false);
    }

    public void enableControl(int i) {
        Control control = this.controls.get(i);
        control.panel.setVisible(true);
    }
        
    private static class Control {
        private final JPanel panel;
        private final JLabel label;
        private final JLabel key;


        Control(String key, String label) {
            this.label = new JLabel(label);
            this.key = new JLabel(key);
            
            this.panel = new JPanel();
            this.panel.setLayout(new BoxLayout(this.panel, BoxLayout.LINE_AXIS));
            this.panel.add(this.label);
            this.panel.add(Box.createHorizontalGlue());
            this.panel.add(this.key);
        }
        
        void setLabel(String label) {
            this.label.setText(label);
        }
        
        void setKey(String label) {
            this.key.setText(label);
        }
    }
}
