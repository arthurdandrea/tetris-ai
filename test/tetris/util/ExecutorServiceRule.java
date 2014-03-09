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

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
public class ExecutorServiceRule implements TestRule {
    private final ListeningScheduledExecutorService executor;

    public ExecutorServiceRule(ListeningScheduledExecutorService executor) {
        this.executor = executor;
    }

    public ExecutorServiceRule(ScheduledExecutorService executor) {
        this.executor = MoreExecutors.listeningDecorator(executor);
    }

    public ListeningScheduledExecutorService get() {
        return this.executor;
    }
    
    @Override
    public Statement apply(Statement base, Description description) {
        return new StatementImpl(base);
    }

    private class StatementImpl extends Statement {
        private final Statement base;
        
        StatementImpl(Statement base) {
            this.base = base;
        }

        @Override
        public void evaluate() throws Throwable {
            try {
                base.evaluate();
            } finally {
                executor.shutdown();
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    throw new Exception("!!!");
                }
            }            
        }
    }
    
}
