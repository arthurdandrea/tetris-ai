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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author Arthur D'Andréa Alemar
 */
final class FutureExtremes<T> {
    private static final Logger logger = Logger.getLogger(FutureExtremes.class.getName());

    private final SettableFuture<T> future;

    private int length;
    private boolean haveEnded;
    private int index;

    private final Comparator<T> comparator;
    private final boolean isMax;
    private T current;
    private final Iterator<ListenableFuture<T>> iterator;
    private final ListeningExecutorService executor;

    FutureExtremes(Iterator<ListenableFuture<T>> iterator, Comparator<T> comparator, ListeningExecutorService executor, boolean isMax) {
        this.future = SettableFuture.create();
        this.iterator = iterator;
        this.executor = executor;
        this.comparator = comparator;
        this.isMax = isMax;
        
        this.current = null;
        this.haveEnded = false;
        this.index = 0;
        this.length = 0;
    }
    
    synchronized ListenableFuture<T> start() {
        this.consume(1);
        return this.future;
    }

    private synchronized void consume(int times) {
        assert times > 0;
        while (this.iterator.hasNext() && times > 0) {
            this.consume(this.iterator.next());
            times--;
        }
        if (!this.iterator.hasNext()) {
            this.end();
        }
    }

    private synchronized void consume(ListenableFuture<T> f) {
        assert !this.future.isDone();

        Futures.addCallback(f, new FutureCallbackImpl());
        this.length++;
    }

    private synchronized void end() {
        this.haveEnded = true;
        if (this.index == this.length && !this.future.isDone()) {
            this.future.set(this.current);
        }
    }
    
    private synchronized void onFailure(Throwable t) {
        this.index++;
        if (!future.setException(t)) {
            logger.log(Level.WARNING, "god help us all", t);
        }
    }

    private synchronized void onSuccess(T result) {
        this.index++;
        if (!future.isDone()) {
            this.process(result);
            if (this.haveEnded && this.index == this.length) {
                this.future.set(this.current);
            } else {
                this.consume(1);
            }
        }
    }

    private void process(T result) {
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
    
    private class FutureCallbackImpl implements FutureCallback<T> {
        @Override
        public void onSuccess(T result) {
            FutureExtremes.this.onSuccess(result);
        }

        @Override
        public void onFailure(Throwable t) {
            FutureExtremes.this.onFailure(t);
        }
    } 

}
