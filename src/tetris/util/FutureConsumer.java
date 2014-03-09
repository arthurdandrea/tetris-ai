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
import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
abstract class FutureConsumer<F, T> {
    private static final Logger logger = Logger.getLogger(FutureConsumer.class.getName());
    private final SettableFuture<?> finalFuture;
    private final SettableFuture<T> future;

    private int length;
    private boolean haveEnded;
    private int index;

    FutureConsumer() {
        this.future = SettableFuture.create();
        this.finalFuture = SettableFuture.create();
        this.haveEnded = false;
        this.index = 0;
        this.length = 0;
    }
    
    public final synchronized void consume(ListenableFuture<F> f, Executor executor) {
        if (!this.future.isDone()) {
            Futures.addCallback(f, new FutureCallbackImpl(), executor);
            this.length++;
        }
    }

    public final synchronized ListenableFuture<T> end() {
        this.haveEnded = true;
        if (this.index == this.length && !this.future.isDone()) {
            this.future.set(this.getResult());
        }
        return this.future;
    }

    public final void waitUntilTheEnd() throws ExecutionException, InterruptedException {
        this.finalFuture.get();
    }

    private synchronized void onFailure(Throwable t) {
        this.index++;
        if (!future.setException(t)) {
            logger.log(Level.WARNING, "god help us all", t);
        }
        if (this.haveEnded && this.index == this.length) {
            this.finalFuture.set(null);
        }
    }

    private synchronized void onSuccess(F result) {
        this.index++;
        if (!future.isDone()) {
            this.process(result);
            if (this.haveEnded && this.index == this.length) {
                this.future.set(getResult());
            }
        }
        if (this.haveEnded && this.index == this.length) {
            this.finalFuture.set(null);
        }
    }
    protected abstract void process(F result);
    protected abstract T getResult();

    private class FutureCallbackImpl implements FutureCallback<F> {
        @Override
        public void onSuccess(F result) {
            FutureConsumer.this.onSuccess(result);
        }

        @Override
        public void onFailure(Throwable t) {
            FutureConsumer.this.onFailure(t);
        }
    } 
}
