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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.hamcrest.Matcher;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
public class ExpectedLog implements TestRule {
    private final List<Matcher> matchers;
    private final List<Matcher> waitingForMatchers;
    private final Logger logger;

    public ExpectedLog(Logger logger) {
        this.logger = logger;
        this.matchers = new LinkedList<>();
        this.waitingForMatchers = new LinkedList<>();
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new ExpectedLogStatement(base);
    }

    public synchronized void expectEntry(Matcher<String> matcher) throws InterruptedException {
        this.matchers.add(matcher);
    }

    private synchronized void check(MemoryHandler memory) {
        if (matchers.isEmpty()) { return; }

        for (Matcher matcher : matchers) {
            boolean found = false;
            for (LogRecord record : memory.records) {
                if (matcher.matches(record.getMessage())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new AssertionError("vixe");
            }
        }
    }

    private static class MemoryHandler extends Handler {
        public final List<LogRecord> records;
        
        MemoryHandler() {
            this.records = new ArrayList<>();
        }
        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
            
        }

        @Override
        public void close() throws SecurityException {
            
        }
    }
    
    private class ExpectedLogStatement extends Statement {
        private final Statement base;
        
        ExpectedLogStatement(Statement base) {
            this.base = base;
        }
        @Override
        public void evaluate() throws Throwable {
            MemoryHandler memory = new MemoryHandler();
            logger.addHandler(memory);
            try {
                this.base.evaluate();
            } finally {
                logger.removeHandler(memory);
                memory.close();
            }
            check(memory);
        }
        
    }
}
