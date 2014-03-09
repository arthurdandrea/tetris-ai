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

package tetris.util;

import com.google.common.util.concurrent.Callables;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
/**
 *
 * @author Arthur D'Andréa Alemar
 */
public class FutureExtremesTest {
    
    @Rule
    public ExecutorServiceRule executorRule = new ExecutorServiceRule(Executors.newScheduledThreadPool(4));

    public ListeningScheduledExecutorService executor;

    @Before
    public void setUp() {
        executor = executorRule.get();
    }

    @Test
    public void testMaxBasic() throws InterruptedException, ExecutionException {
        FutureExtremes<Integer> max = FutureExtremes.max();
        max.consume(executor.submit(Callables.returning(10)), executor);
        max.consume(executor.submit(Callables.returning(5)), executor);
        max.consume(executor.submit(Callables.returning(15)), executor);
        Integer result = max.end().get();
        assertThat(result, is(15));
    }

    @Test
    public void testMaxWithDuplicatedValues() throws InterruptedException, ExecutionException {
        FutureExtremes<Integer> max = FutureExtremes.max();
        max.consume(executor.submit(Callables.returning(-10)), executor);
        max.consume(executor.submit(Callables.returning(15)), executor);
        max.consume(executor.submit(Callables.returning(5)), executor);
        max.consume(executor.submit(Callables.returning(15)), executor);
        Integer result = max.end().get();
        assertThat(result, is(15));
    }

    @Test
    public void testMinBasic() throws InterruptedException, ExecutionException {
        FutureExtremes<Integer> min = FutureExtremes.min();
        min.consume(executor.submit(Callables.returning(10)), executor);
        min.consume(executor.submit(Callables.returning(5)), executor);
        min.consume(executor.submit(Callables.returning(15)), executor);
        Integer result = min.end().get();
        assertThat(result, is(5));
    }

    @Test
    public void testMinWithDuplicatedValues() throws InterruptedException, ExecutionException {
        FutureExtremes<Integer> min = FutureExtremes.min();
        min.consume(executor.submit(Callables.returning(-10)), executor);
        min.consume(executor.submit(Callables.returning(15)), executor);
        min.consume(executor.submit(Callables.returning(5)), executor);
        min.consume(executor.submit(Callables.returning(15)), executor);
        Integer result = min.end().get();
        assertThat(result, is(-10));
    }
}

