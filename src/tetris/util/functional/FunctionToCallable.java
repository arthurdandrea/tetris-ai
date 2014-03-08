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
import java.util.concurrent.Callable;

/**
 *
 * @author Arthur D'Andréa Alemar
 * @param <I> Input
 * @param <O> Output
 */
public final class FunctionToCallable<I, O> implements Callable<O> {
    private final Function<I, O> function;
    private final I input;
    
    public FunctionToCallable(Function<I, O> function, I input) {
        this.function = function;
        this.input = input;
    }

    @Override
    public O call() throws Exception {
        return this.function.apply(input);
    }
}
