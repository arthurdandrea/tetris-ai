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
import com.google.common.util.concurrent.ListeningExecutorService;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Timer;
import tetris.ai.AbstractAI;
import tetris.generic.BlockMover;
import tetris.generic.TetrisEngine;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
public final class AIExecutor {
    private static final Logger logger = Logger.getLogger(AIExecutor.class.getName());

    private final ActionListenerImpl actionListenerImpl = new ActionListenerImpl();
    private final FutureCallbackImpl futureCallbackImpl = new FutureCallbackImpl();
    
    private final AbstractAI ai;
    private final TetrisEngine engine;
    private final Timer timer;
    private BlockMover mover;
    private boolean running;
    private int delay;

    public AIExecutor(int delay, AbstractAI ai, TetrisEngine engine) {
        this.running = false;
        this.mover = null;
        
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
            if (running && mover != null && mover.hasMoreMoves()) {
                if (delay == 0) {
                    mover.slam();
                } else {
                    mover.move();
                }
            } else {
                mover = null;
                timer.stop();
                if (running) {
                    Futures.addCallback(ai.process(engine), futureCallbackImpl);
                }
            }
        }
    }
    
    private class FutureCallbackImpl implements FutureCallback<BlockMover> {
        @Override
        public void onSuccess(BlockMover mover) {
            if (running) {
                if (mover != null && delay == 0) {
                    mover.slam();
                } else {
                    AIExecutor.this.mover = mover;
                }
                timer.restart();
            }
        }

        @Override
        public void onFailure(Throwable t) {
            logger.log(Level.SEVERE, "error while processing ai", t);
        }
    }

}
