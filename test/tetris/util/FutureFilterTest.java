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

import com.google.common.base.Predicate;
import com.google.common.util.concurrent.Callables;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 *
 * @author José Dias Neto
 */
@RunWith(JUnit4.class)
public class FutureFilterTest {
    
    @Rule
    public ExecutorServiceRule executorRule = new ExecutorServiceRule(Executors.newScheduledThreadPool(4));

    @Rule
    public ExpectedException exception = ExpectedException.none();
    
    @Rule
    public ExpectedLog logger = new ExpectedLog(Logger.getLogger(FutureConsumer.class.getName()));

    public ListeningScheduledExecutorService executor;
    
    @Before
    public void setUp() {
        this.executor = executorRule.get();
    }

    @Test
    public void testBasic() throws InterruptedException, ExecutionException {
        FutureFilter<Integer> filter = new FutureFilter<>(new GreaterThan(3));
        for (int i = 1; i <= 10; i++) {
            filter.consume(executor.submit(Callables.returning(i)), executor);
        }
        List<Integer> results = filter.end().get();
        assertThat(results, containsInAnyOrder(4, 5, 6, 7, 8, 9, 10));
    }

    @Test
    public void testDuplicatedValues() throws InterruptedException, ExecutionException {
        FutureFilter<Integer> filter = new FutureFilter<>(new GreaterThan(30));
        filter.consume(executor.submit(Callables.returning(-10)), executor);
        filter.consume(executor.submit(Callables.returning(20)), executor);
        filter.consume(executor.submit(Callables.returning(30)), executor);
        filter.consume(executor.submit(Callables.returning(31)), executor);
        filter.consume(executor.submit(Callables.returning(31)), executor);
        filter.consume(executor.submit(Callables.returning(100)), executor);
        filter.consume(executor.submit(Callables.returning(101)), executor);
        List<Integer> results = filter.end().get();
        assertThat(results, containsInAnyOrder(101, 100, 31, 31));
    }

    @Test
    public void testException() throws InterruptedException, ExecutionException {
        FutureFilter<Integer> filter = new FutureFilter<>(new GreaterThan(3));
        for (int i = 1; i <= 10; i++) {
            filter.consume(executor.submit(Callables.returning(i)), executor);
        }
        filter.consume(executor.schedule(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }, 500, TimeUnit.MILLISECONDS), executor);
        ListenableFuture<List<Integer>> future = filter.end();
        exception.expect(ExecutionException.class);
        exception.expectCause(isA(UnsupportedOperationException.class));
        exception.expectCause(hasMessage(is("Not supported yet.")));
        future.get();
    }

    @Test
    public void testMultipleException() throws InterruptedException, ExecutionException {
        FutureFilter<Integer> filter = new FutureFilter<>(new GreaterThan(3));
        for (int i = 1; i <= 10; i++) {
            filter.consume(executor.submit(Callables.returning(i)), executor);
        }
        filter.consume(executor.schedule(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }, 100, TimeUnit.MILLISECONDS), executor);
        ListenableScheduledFuture<Integer> future1 = executor.schedule(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                throw new IndexOutOfBoundsException();
            }
        }, 500, TimeUnit.MILLISECONDS);
        filter.consume(future1, executor);
        ListenableFuture<List<Integer>> future = filter.end();
        try {
            future1.get();
        } catch (InterruptedException | ExecutionException ex) {
            // do nothing
        }
        exception.expect(ExecutionException.class);
        exception.expectCause(isA(UnsupportedOperationException.class));
        exception.expectCause(hasMessage(is("Not supported yet.")));
        logger.expectEntry(is("god help us all"));
        future.get();
    }

    private static class GreaterThan implements Predicate<Integer> {
        private final int top;

        public GreaterThan(int top) {
            this.top = top;
        }

        @Override
        public boolean apply(Integer input) {
            return input > top;
        }
    }
}

