/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tetris.ai;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import tetris.ProjectConstants;
import tetris.generic.Score;
import tetris.generic.TetrisEngine;

/**
 *
 * @author arthur
 */
public class TetrisAITest {
    private TetrisAI ai;
    private TetrisEngine engine;
    private ListeningExecutorService executor;

    @Before
    public void setUp() {
        engine = new TetrisEngine();
        executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));
        ai = new TetrisAI(executor);
        engine.startengine();
        engine.setState(ProjectConstants.GameState.PLAYING);
    }
    
    @After
    public void tearDown() throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void testProcess() throws InterruptedException, ExecutionException {
        int i;
        for (i = 0; i < 1000 && engine.getActiveblock() != null; i++) {
            ai.process(engine).get();
        }
        Score score = engine.getScore();
        System.out.printf("iterations: %d\nscore: %d\nlinesRemoved: %d\n", i, score.getScore(), score.getLinesRemoved());
        assertTrue("score should be greater than 0", score.getScore() > 0);
        assertTrue("linesRemoved should be greater than 0", score.getLinesRemoved() > 0);
    }
}
