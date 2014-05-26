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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
public final class MyThread {
    
    private enum Status {
        STARTING,
        STARTED,
        PAUSING,
        PAUSED,
        STOPING,
        STOPED
    }
    
    private Status status;
    private static final Logger logger = Logger.getLogger(MyThread.class.getName());
    private final Function<ThreadControl, Void> function;
    private final Thread thread;

    public MyThread(Function<ThreadControl, Void> function, String name) {
        this.status = Status.STOPED;
        this.function = function;
        this.thread = new Thread(new Runnable() {

            @Override
            public void run() {
                synchronized (MyThread.this) {
                    MyThread.this.status = Status.STARTED;
                }
                
                try {
                    MyThread.this.function.apply(new ThreadControl());
                } finally {
                    synchronized (MyThread.this) {
                        MyThread.this.status = Status.STOPED;
                    }
                }
            }
        }, name);
    }
    
    public synchronized void start() {
        assert this.status == Status.STOPED;
        this.status = Status.STARTING;
        this.thread.start();
    }
    
    public void stop() {
        boolean shouldJoin = false;
        synchronized (this) {
            if (this.status == Status.STARTED) {
                shouldJoin = true;
                Status previous = this.status;
                this.status = Status.STOPING;
                if (previous == Status.PAUSED) {
                    this.notify();
                }
            } else if (this.status == Status.STOPED) {
            } else {
                throw new RuntimeException();
            }
        }
        if (shouldJoin) {
            try {
                this.thread.join();
            } catch (InterruptedException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public synchronized void pause() {
        this.status = Status.PAUSING;
    }

    public synchronized void resume() {
        switch (this.status) {
            case PAUSING:
                this.status = Status.STARTED;
                break;
            case PAUSED:
                this.status = Status.STARTED;
                this.notify();
                break;
            case STARTED:
            case STARTING:
                // do nothing
                break;
            case STOPED:
            case STOPING:
            default:
                throw new IllegalStateException();
        }
    }
    
    public synchronized void startOrResume() {
        switch (this.status) {
            case STOPED:
            case STOPING:
                this.start();
                break;
            case PAUSING:
            case PAUSED:
                this.resume();
                break;
            case STARTED:
            case STARTING:
                // do nothing;
                break;
            default:
                throw new IllegalStateException();
        }
    }

    public class ThreadControl {

        private ThreadControl() {
        }
        
        public boolean check() {
            synchronized (MyThread.this) {
                while (MyThread.this.status == Status.PAUSING) {
                    try {
                        MyThread.this.wait();
                        MyThread.this.status = Status.PAUSED;
                    } catch (InterruptedException ex) {
                        logger.log(Level.SEVERE, null, ex);
                        return false;
                    }
                }
                if (status == Status.STOPING) {
                    return false;
                } else if (status == Status.STARTED) {
                    return true;
                } else {
                    throw new RuntimeException();
                }
            }
        }
    }
}
