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

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import tetris.util.functional.CartesianProduct.Pair;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
public class CartesianProduct<T> implements Iterator<Pair<T>> {
    private T cache;
    
    private final Iterable<T> innerIterable;
    private final Iterator<T> outerIterator;
    private Iterator<T> innerIterator;
    private boolean end;

    public CartesianProduct(T[] array1, T[] array2) {
        this(Arrays.asList(array1), Arrays.asList(array2));
    }

    public CartesianProduct(Iterable<T> iterable1, Iterator<T> iterator2) {
        this(iterable1.iterator(), iterator2);
    }

    public CartesianProduct(Iterator<T> iterator1, Iterator<T> iterator2) {
        this(iterator1, new CachedIterator(iterator2));
    }

    public CartesianProduct(Iterable<T> iterable1, Iterable<T> iterable2) {
        this(iterable1.iterator(), iterable2);
    }

    public CartesianProduct(Iterator<T> iterator1, Iterable<T> iterable2) {
        this.outerIterator = iterator1;
        this.innerIterable = iterable2;
        this.innerIterator = iterable2.iterator();
        if (this.outerIterator.hasNext() && innerIterator.hasNext()) {
            this.end = false;
            this.cache = this.outerIterator.next();
        } else {
            this.end = true;
            this.cache = null;
        }
    }
    
    @Override
    public synchronized boolean hasNext() {
        return !end && (this.innerIterator.hasNext() || this.outerIterator.hasNext());
    }

    @Override
    public Pair<T> next() {
        if (end) { throw new NoSuchElementException(); }
        if (!this.innerIterator.hasNext()) {
            this.cache = this.outerIterator.next();
            this.innerIterator = this.innerIterable.iterator();
        }
        return new Pair<>(this.cache, this.innerIterator.next());
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported.");
    }

    public static class Pair<T> {
        public final T first;
        public final T second;

        public Pair(T first, T second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Pair)) {
                return false;
            }
            Pair other = (Pair) obj;
            return this.first.equals(other.first) && this.second.equals(other.second);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 23 * hash + Objects.hashCode(this.first);
            hash = 23 * hash + Objects.hashCode(this.second);
            return hash;
        }

        @Override
        public String toString() {
            return String.format("(%s %s)", this.first, this.second);
        }
    }
}
