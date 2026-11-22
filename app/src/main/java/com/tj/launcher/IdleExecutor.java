package com.tj.launcher;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * C6 (CW1): a single-thread executor whose thread ENDS when it has been idle for a while (the pattern of {@code RecentsFilter.Labels}), so a logging queue that is busy for a moment at a lock or an unlock does not
 * keep a native stack and its thread-local data for the whole life of the process. Order is the same as {@code Executors.newSingleThreadExecutor}: one worker, FIFO; a worker only ends when the queue is empty
 * ({@link ThreadPoolExecutor} never lets an idle worker go while work is queued, and restarts one if work arrived as it went), the next task starts a new thread (microseconds).
 */
final class IdleExecutor {
    /** The keep-alive of the app's executors. */
    static final long IDLE_MS = 10_000L;

    private IdleExecutor() {}

    static ThreadPoolExecutor single(final String threadName, long idleMs) {
        ThreadPoolExecutor e = new ThreadPoolExecutor(1, 1, idleMs, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
            @Override public Thread newThread(Runnable r) { Thread t = new Thread(r, threadName); t.setDaemon(true); return t; }
        });
        e.allowCoreThreadTimeOut(true);
        return e;
    }

    static ThreadPoolExecutor single(String threadName) { return single(threadName, IDLE_MS); }
}
