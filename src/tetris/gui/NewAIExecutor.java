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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Timer;
import tetris.ai.AbstractAI;
import tetris.generic.TetrisEngine;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
public final class NewAIExecutor {
    private static final Logger logger = Logger.getLogger(NewAIExecutor.class.getName());

    private final ActionListenerImpl actionListenerImpl = new ActionListenerImpl();
    private final FutureCallbackImpl futureCallbackImpl = new FutureCallbackImpl();
    private final FutureCallbackIteratorImpl futureCallbackIteratorImpl = new FutureCallbackIteratorImpl();
    
    private final AbstractAI ai;
    private final TetrisEngine engine;
    private final Timer timer;
    private Iterator<Void> iterator;
    private boolean running;
    private int delay;

    public NewAIExecutor(int delay, AbstractAI ai, TetrisEngine engine) {
        this.running = false;
        this.iterator = null;
        
        this.timer = new Timer(delay, actionListenerImpl);
        this.ai = ai;
        this.engine = engine;
        this.setDelay(delay);
    }
    
    public synchronized void setDelay(int delay) {
        this.delay = delay;
        if (delay == 0) {
            this.timer.setInitialDelay(33);
            this.timer.setDelay(33);
        } else {
            this.timer.setInitialDelay(delay);
            this.timer.setDelay(delay);
        }
    }

    public synchronized void start() {
        if (!this.running) {
            this.running = true;
            this.timer.restart();
        }
    }

    public synchronized boolean isRunning() {
        return this.timer.isRunning();
    }

    public synchronized void stop() {
        if (this.running) {
            this.running = false;
            this.timer.stop();
        }
    }

    private class ActionListenerImpl implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (running && iterator != null && iterator.hasNext()) {
                iterator.next();
            } else {
                iterator = null;
                timer.stop();
                if (running) {
                    if (delay == 0) {
                        Futures.addCallback(ai.process(engine), futureCallbackImpl);
                    } else {
                        Futures.addCallback(ai.processIterator(engine), futureCallbackIteratorImpl);
                    }
                }
            }
        }
    }
    
    private class FutureCallbackImpl implements FutureCallback<Void> {
        @Override
        public void onSuccess(Void iterator) {
            if (running) {
                timer.restart();
            }
        }

        @Override
        public void onFailure(Throwable t) {
            logger.log(Level.SEVERE, "error while processing ai", t);
        }

    }
    
    private class FutureCallbackIteratorImpl implements FutureCallback<Iterator<Void>> {

        @Override
        public void onSuccess(Iterator<Void> iterator) {
            if (running) {
                NewAIExecutor.this.iterator = iterator;
                timer.restart();
            }
        }

        @Override
        public void onFailure(Throwable t) {
            logger.log(Level.SEVERE, "error while processing ai", t);
        }

    }

}
