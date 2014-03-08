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
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
public final class FutureFilter<F> extends FutureConsumer<F, List<F>> {
    private final Predicate<F> filter;
    private final List<F> results;

    public FutureFilter(Predicate<F> filter) {
        this.filter = filter;
        this.results = new LinkedList<>();
    }

    @Override
    protected void process(F result) {
        if (filter.apply(result)) {
            results.add(result);
        }
    }

    @Override
    protected List<F> getResult() {
        return results;
    }
}

