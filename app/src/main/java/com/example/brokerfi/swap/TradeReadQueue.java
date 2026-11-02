package com.example.brokerfi.swap;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** One queue for Trade preview/preflight batches. Never queues or retries transaction sends. */
final class TradeReadQueue {
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private static final ThreadPoolExecutor WORK = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
            new PriorityBlockingQueue<>(), r -> {
        Thread thread = new Thread(r, "trade-chain-reads");
        thread.setDaemon(true);
        return thread;
    });
    private static final class ReadTask implements Runnable, Comparable<ReadTask> {
        final Runnable work;
        final int priority;
        final long sequence = SEQUENCE.getAndIncrement();
        ReadTask(Runnable work, int priority) { this.work = work; this.priority = priority; }
        public void run() { work.run(); }
        public int compareTo(ReadTask other) {
            int order = Integer.compare(priority, other.priority);
            return order != 0 ? order : Long.compare(sequence, other.sequence);
        }
    }
    private TradeReadQueue() {}
    static void execute(Runnable work) { WORK.execute(new ReadTask(work, 1)); }
    // Does not interrupt an in-flight read and never queues financial sends here.
    static void executeUser(Runnable work) { WORK.execute(new ReadTask(work, 0)); }
}
