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

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import java.util.ArrayList;
import java.util.List;
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
public class UtilTest {

    @Rule
    public ExecutorServiceRule executorRule = new ExecutorServiceRule(Executors.newScheduledThreadPool(4));

    public ListeningScheduledExecutorService executor;

    @Before
    public void setUp() {
        executor = executorRule.get();
    }

    /**
     * Test of max(Iterator, Comparator) method, of class Util.
     */
    @Test
    public void testMaxWithComparator() {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        Integer result = Util.max(list.iterator(), Ordering.explicit(4, 3, 2, 1, 0));
        assertThat(result, is(1));
    }

    /**
     * Test of max(Iterator) method, of class Util.
     */
    @Test
    public void testMaxWithComparable() {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        assertThat(Util.max(list.iterator()), is(3));
    }

    /**
     * Test of min(Iterator, Comparator) method, of class Util.
     */
    @Test
    public void testMinWithComparator() {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        Integer result = Util.min(list.iterator(), Ordering.explicit(4, 3, 2, 1, 0));
        assertThat(result, is(3));
    }

    /**
     * Test of min(Iterator) method, of class Util.
     */
    @Test
    public void testMinWithComparable() {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        assertThat(Util.min(list.iterator()), is(1));
    }

    /**
     * Test of maxAsync(Iterator, Function, Executor) method, of class Util.
     */
    @Test
    public void testMaxAsyncWithComparable() throws InterruptedException, ExecutionException {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        Function identity = Functions.identity();
        Integer result = Util.maxAsync(list.iterator(), (Function<Integer, Integer>) identity, executor).get();
        assertThat(result, is(3));
    }

    /**
     * Test of maxAsync(Iterator, Function, Executor, Comparator) method, of class Util.
     */
    @Test
    public void testMaxAsyncWithComparator() throws InterruptedException, ExecutionException {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        Function identity = Functions.identity();
        Integer result = Util.maxAsync(list.iterator(), (Function<Integer, Integer>) identity, executor, Ordering.explicit(4, 3, 2, 1, 0)).get();
        assertThat(result, is(1));
    }

    /**
     * Test of minAsync(Iterator, Function, Executor) method, of class Util.
     */
    @Test
    public void testMinAsyncWithComparable() throws InterruptedException, ExecutionException {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        Function identity = Functions.identity();
        Integer result = Util.minAsync(list.iterator(), (Function<Integer, Integer>) identity, executor).get();
        assertThat(result, is(1));
    }

    /**
     * Test of minAsync(Iterator, Function, Executor, Comparator) method, of class Util.
     */
    @Test
    public void testMinAsyncWithComparator() throws InterruptedException, ExecutionException {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        Function identity = Functions.identity();
        Integer result = Util.minAsync(list.iterator(), (Function<Integer, Integer>) identity, executor, Ordering.explicit(4, 3, 2, 1, 0)).get();
        assertThat(result, is(3));
    }
}
