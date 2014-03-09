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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
public class CachedIterator<T> implements Iterable<T> {
    private final List<T> cache;
    private final Iterator<T> iterator;

    public CachedIterator(Iterator<T> iterator) {
        this.iterator = iterator;
        this.cache = new ArrayList<>();
    }

    @Override
    public Iterator<T> iterator() {
        return new It();
    }

    private class It implements Iterator<T> {
        private int position;

        It() {
            position = 0;
        }

        @Override
        public boolean hasNext() {
            return position < cache.size() || iterator.hasNext();
        }

        @Override
        public T next() {
            if (position < cache.size()) {
                return cache.get(position++);
            } else {
                T result = iterator.next();
                position++;
                cache.add(result);
                return result;
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    
}
