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

package tetris.util.functional;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.SwingUtilities;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
public final class PropertyListeners  {
    
    public static PropertyChangeListener alwaysInSwing(PropertyChangeListener inner) {
        return new SwingListener(inner);
    }
    
    private static class SwingListener implements PropertyChangeListener {
        private final PropertyChangeListener inner;
        SwingListener(PropertyChangeListener inner) {
            this.inner = inner;
        }
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (SwingUtilities.isEventDispatchThread()) {
                inner.propertyChange(evt);
            } else {
                SwingUtilities.invokeLater(new RunnableImpl(inner, evt));
            }
        }
    }
    
    private static class RunnableImpl implements Runnable {
        private final PropertyChangeListener inner;
        private final PropertyChangeEvent evt;

        public RunnableImpl(PropertyChangeListener inner, PropertyChangeEvent evt) {
            this.inner = inner;
            this.evt = evt;
        }
        
        @Override
        public void run() {
            inner.propertyChange(evt);
        }
    }
}
