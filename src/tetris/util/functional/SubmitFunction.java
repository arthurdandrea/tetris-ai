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

package tetris.util.functional;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
public class SubmitFunction<X, Y> implements Function<X, ListenableFuture<Y>>, AsyncFunction<X, Y> {    
    private final ListeningExecutorService executor;
    private final Function<X, Y> function;

    public SubmitFunction(ListeningExecutorService executor, Function<X, Y> function) {
        this.executor = executor;
        this.function = function;
    }

    @Override
    public ListenableFuture<Y> apply(X input) {
        return this.executor.submit(new FunctionToCallable<>(function, input));
    }
}
