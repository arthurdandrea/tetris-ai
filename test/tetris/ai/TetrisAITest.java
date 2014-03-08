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

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import org.hamcrest.Matcher;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import org.hamcrest.collection.IsIterableContainingInOrder;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import tetris.ProjectConstants;
import tetris.generic.Score;
import tetris.generic.TetrisEngine;
import tetris.generic.Tetromino;
import tetris.util.ExecutorServiceRule;

/**
 *
 * @author arthur
 */
public class TetrisAITest {
    @Rule
    public ExecutorServiceRule executorRule = new ExecutorServiceRule(Executors.newScheduledThreadPool(4));

    public ListeningScheduledExecutorService executor;

    private TetrisAI ai;
    private TetrisEngine engine;

    @Before
    public void setUp() {
        executor = executorRule.get();

        engine = new TetrisEngine();
        ai = new TetrisAI(executor);
        engine.startengine();
        engine.setState(ProjectConstants.GameState.PLAYING);
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