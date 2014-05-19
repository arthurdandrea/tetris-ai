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
import com.google.common.collect.Iterators;
import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
public class Util {
    private Util() {}
    
    public static <T> T min(Iterator<T> iterator, Comparator<T> comparator) {
        T min, current;
        min = null;
        while (iterator.hasNext()) {
            current = iterator.next();
            if (min == null || current == null || comparator.compare(min, current) > 0) {
                min = current;
            }
        }
        return min;
    }
    
    public static <T extends Comparable> T min(Iterator<T> iterator) {
        return min(iterator, (Comparator<T>) Ordering.natural());
    }

    
    public static <T> T max(Iterator<T> iterator, Comparator<T> comparator) {
        T max, current;
        max = null;
        while (iterator.hasNext()) {
            current = iterator.next();
            if (max == null || current == null || comparator.compare(max, current) < 0) {
                max = current;
            }
        }
        return max;
    }
    
    public static <T extends Comparable> T max(Iterator<T> iterator) {
        return max(iterator, (Comparator<T>) Ordering.natural());
    }
        
    public static <F, T> ListenableFuture<T> maxAsync(Iterator<F> iterator, Function<F, T> function, ListeningExecutorService executor, Comparator<T> comparator) {
        return minMaxAsync(Iterators.transform(iterator, new FunctionImpl(executor, function)), executor, comparator, true);
    }
    
    public static <F, T extends Comparable> ListenableFuture<T> maxAsync(Iterator<F> iterator, Function<F, T> function, ListeningExecutorService executor) {
        return maxAsync(iterator, function, executor, (Comparator<T>)Ordering.natural());
    }

    public static <F, T> ListenableFuture<T> minAsync(Iterator<F> iterator, Function<F, T> function, ListeningExecutorService executor, Comparator<T> comparator) {
        return minMaxAsync(Iterators.transform(iterator, new FunctionImpl(executor, function)), executor, comparator, false);
    }
    
    public static <F, T extends Comparable> ListenableFuture<T> minAsync(Iterator<F> iterator, Function<F, T> function, ListeningExecutorService executor) {
        return minAsync(iterator, function, executor, (Comparator<T>)Ordering.natural());
    }

    private static <T> ListenableFuture<T> minMaxAsync(Iterator<ListenableFuture<T>> iterator, ListeningExecutorService executor, Comparator<T> comparator, boolean isMax) {
        Objects.requireNonNull(iterator);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(comparator);

        FutureExtremes<T> extremes = new FutureExtremes<>(iterator, comparator, executor, isMax);
        return extremes.start();
    }


    private static class FunctionImpl<F, T> implements Function<F, ListenableFuture<T>> {
        private final ListeningExecutorService executor;
        private final Function<F, T> function;

        public FunctionImpl(ListeningExecutorService executor, Function<F, T> function) {
            this.executor = executor;
            this.function = function;
        }

        @Override
        public ListenableFuture<T> apply(F input) {
            return this.executor.submit(new CallableImpl<>(this.function, input));
        }
    }
    
    private static class CallableImpl<F, T> implements Callable<T> {
        private final Function<F, T> function;
        private final F input;

        public CallableImpl(Function<F, T> function, F input) {
            this.input = input;
            this.function = function;
        }

        @Override
        public T call() throws Exception {
            return this.function.apply(this.input);
        }
    }
}
