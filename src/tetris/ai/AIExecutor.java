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

package tetris.ai;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import tetris.generic.TetrisEngine;
import tetris.generic.TetrisEngine.GameState;

/**
 *
 * @author arthur
 */
public class AIExecutor {

    private State state;
    private final ListeningExecutorService executor;
    private final AbstractAI ai;
    private final TetrisEngine engine;

    public AIExecutor(AbstractAI ai, TetrisEngine engine, ListeningExecutorService executor) {
        this.executor = executor;
        this.engine = engine;
        this.engine.addPropertyChangeListener("state", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                AIExecutor.this.onGameStateChange();
            }
        });
        this.ai = ai;
        this.state = State.STOPED;
    }
    
    private synchronized void onGameStateChange() {
        if (this.state == State.STOPED) {
            this.state = State.RUNNING;
            this.executor.submit(new AiExecutorRunnable());
        }
    }
    private enum State {STOPED, RUNNING};
    
    private class AiExecutorRunnable implements Runnable, FutureCallback<Void> {
        @Override
        public void onSuccess(Void result) {
            this.run();
        }

        @Override
        public void onFailure(Throwable t) {
            Logger.getLogger(AIExecutor.class.getName()).log(Level.SEVERE, null, t);
            synchronized (AIExecutor.this) {
                state = State.STOPED;
            }
        }

        @Override
        public void run() {
            if (engine.getState() == GameState.PLAYING) {
                Futures.addCallback(ai.process(engine), this, executor);
            } else {
                synchronized (AIExecutor.this) {
                    state = State.STOPED;
                }
            }
        }
    }
}
