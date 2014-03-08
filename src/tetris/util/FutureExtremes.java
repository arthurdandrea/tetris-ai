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

import tetris.util.functional.SubmitFunction;
import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
public class FutureExtremes<F> extends FutureConsumer<F, F> {
    public enum Extreme {
        MIN, MAX
    }
    
    public static <F extends Comparable> ListenableFuture<F> calculate(Iterator<ListenableFuture<F>> iterator, Executor executor, Extreme direction) {
        return calculate(iterator, executor, (Comparator<F>) Ordering.natural(), direction);
    }

    public static <F> ListenableFuture<F> calculate(Iterator<ListenableFuture<F>> iterator, Executor executor, Comparator<F> comparator, Extreme direction) {
        Objects.requireNonNull(iterator);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(comparator);
        Objects.requireNonNull(direction);

        FutureExtremes<F> extremes = new FutureExtremes<>(comparator, direction == Extreme.MAX);
        while (iterator.hasNext()) {
            extremes.consume(iterator.next(), executor);
        }
        return extremes.end();
    }
    
    public static <T, F> ListenableFuture<F> calculate(Iterator<T> iterator, Function<T, F> function, ListeningExecutorService executor, Comparator<F> comparator, Extreme direction) {
        Objects.requireNonNull(iterator);
        Objects.requireNonNull(function);
        Objects.requireNonNull(executor);
        return calculate(Iterators.transform(iterator, new SubmitFunction(executor, function)), executor, comparator, direction);
    }

    public static <T, F extends Comparable> ListenableFuture<F> calculate(Iterator<T> iterator, Function<T, F> function, ListeningExecutorService executor, Extreme direction) {
        return calculate(iterator, function, executor, (Comparator<F>) Ordering.natural(), direction);
    }
    
    public static <T, F extends Comparable> ListenableFuture<F> calculate(Iterable<T> iterable, Function<T, F> function, ListeningExecutorService executor, Extreme direction) {
        return calculate(iterable.iterator(), function, executor, direction);
    }

    public static <F extends Comparable> FutureExtremes<F> max() {
        return new FutureExtremes<>((Comparator<F>) Ordering.natural(), true);
    }

    public static <F> FutureExtremes<F> max(Comparator<F> comparator) {
        return new FutureExtremes<>(comparator, true);
    }

    public static <F extends Comparable> FutureExtremes<F> min() {
        return new FutureExtremes<>((Comparator<F>) Ordering.natural(), false);
    }
    
    public static <F> FutureExtremes<F> min(Comparator<F> comparator) {
        return new FutureExtremes<>(comparator, false);
    }

    private final Comparator<F> comparator;
    private final boolean isMax;
    private F current;

    private FutureExtremes(Comparator<F> comparator, boolean isMax) {
        this.comparator = comparator;
        this.current = null;
        this.isMax = isMax;
    }

    @Override
    protected void process(F result) {
        Objects.requireNonNull(result);
        if (current == null) {
            current = result;
        } else {
            int diff = comparator.compare(current, result);
            if (isMax ? diff < 0 : diff > 0) {
                current = result;
            }
        }
    }

    @Override
    protected F getResult() {
        return this.current;
    }
}
