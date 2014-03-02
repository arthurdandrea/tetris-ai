/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tetris.ai;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import tetris.ProjectConstants;
import tetris.generic.TetrisEngine;
import tetris.generic.TetrisEngineListener;

/**
 *
 * @author arthur
 */
public class AIExecutor {
    private enum State {STOPED, RUNNING};

    private State state;
    private final ListeningExecutorService executor;
    private final AbstractAI ai;
    private final TetrisEngine engine;

    public AIExecutor(AbstractAI ai, TetrisEngine engine, ListeningExecutorService executor) {
        this.executor = executor;
        this.engine = engine;
        this.engine.addListener(new TetrisEngineListenerImpl());
        this.ai = ai;
        this.state = State.STOPED;
    }
    
    private synchronized void onGameStateChange() {
        if (this.state == State.STOPED) {
            this.state = State.RUNNING;
            this.executor.submit(new AiExecutorRunnable());
        }
    }
    
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
            if (engine.getState() == ProjectConstants.GameState.PLAYING) {
                Futures.addCallback(ai.process(engine), this, executor);
            } else {
                synchronized (AIExecutor.this) {
                    state = State.STOPED;
                }
            }
        }
    }

    private class TetrisEngineListenerImpl implements TetrisEngineListener {

        public TetrisEngineListenerImpl() {
        }

        @Override
        public void onGameStateChange(TetrisEngine engine) {
            AIExecutor.this.onGameStateChange();
        }
    }
}
