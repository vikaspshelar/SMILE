package com.smilecoms.commons.base.lifecycle;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.hazelcast.HazelcastHelper;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class Async {

    private static final Logger log = LoggerFactory.getLogger(Async.class);
    // An executor where a request can be put in the queue even if a thread is not available to process it
    private static final ExecutorService queueingExecutor = new ThreadPoolExecutor(3, 100, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1000, true), new AsyncThreadFactory("Smile-Base-Queueing-Executor-"));
    private static final ExecutorService nonqueueingExecutor = new ThreadPoolExecutor(3, 100, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new AsyncThreadFactory("Smile-Base-NonQueueing-Executor-"));
    private static final ScheduledExecutorService scheduledExecutor = new ScheduledThreadPoolExecutor(5, new AsyncThreadFactory("Smile-Base-Scheduled-Executor-"));
    private static final Random random = new Random(System.currentTimeMillis());
    private static final Set<String> successfulOnceOnlyIds = new HashSet<String>();
    private static final Lock runOnceLock = new ReentrantLock();

    // Wont block the caller if no threads are available in the pool
    public static <T> Future<T> submit(Callable<T> call) {
        return queueingExecutor.submit(call);
    }

    // Will block the caller if no threads are available in the pool
    public static <T> Future<T> submitImmediate(Callable<T> call) {
        return nonqueueingExecutor.submit(call);
    }

    // Wont block the caller if no threads are available in the pool
    public static void execute(Runnable run) {
        queueingExecutor.execute(makeSafeRunnable(run));
    }

    // Will block the caller if no threads are available in the pool
    public static void executeImmediate(Runnable run) {
        nonqueueingExecutor.execute(makeSafeRunnable(run));
    }

    public static void shutDown() {
        try {
            queueingExecutor.shutdown();
        } catch (Exception e) {
            log.warn("Error shutting down executor services: ", e);
        }
        try {
            nonqueueingExecutor.shutdown();
        } catch (Exception e) {
            log.warn("Error shutting down executor services: ", e);
        }
        try {
            scheduledExecutor.shutdown();
        } catch (Exception e) {
            log.warn("Error shutting down executor services: ", e);
        }
    }

    private static Runnable makeSafeRunnable(final Runnable run) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    String statName = null;
                    long start = 0;
                    if (run instanceof SmileBaseRunnable) {
                        statName = ((SmileBaseRunnable) run).getName();
                        start = System.currentTimeMillis();
                    }
                    if (BaseUtils.IN_SHUTDOWN) {
                        log.warn("A runnable wants to run during system shutdown. Denying [{}]", statName == null ? "Unknown" : statName);
                        return;
                    }
                    run.run();
                    if (statName != null) {
                        long latency = System.currentTimeMillis() - start;
                        BaseUtils.addStatisticSample("ASYNC_" + statName, BaseUtils.STATISTIC_TYPE.latency, latency);
                    }

                } catch (Throwable e) {
                    log.warn("Error running runnable: ", e);
                }
            }

        };
    }

    public static ScheduledFuture scheduleAtFixedRate(Runnable run, int startDelayMillis, int startToStartWaitMillis) {
        return scheduledExecutor.scheduleAtFixedRate(makeSafeRunnable(run), startDelayMillis, startToStartWaitMillis, TimeUnit.MILLISECONDS);
    }

    public static ScheduledFuture scheduleWithFixedDelay(Runnable run, int startDelayMillis, int finishedToStartWaitMillis) {
        return scheduledExecutor.scheduleWithFixedDelay(makeSafeRunnable(run), startDelayMillis, finishedToStartWaitMillis, TimeUnit.MILLISECONDS);
    }

    public static ScheduledFuture scheduleAtFixedRate(Runnable run, int startDelayMillis, int startToStartWaitMillisRandomFromIncl, int startToStartWaitMillisRandomToExcl) {
        return scheduledExecutor.scheduleAtFixedRate(makeSafeRunnable(run), startDelayMillis, getRandomNumber(startToStartWaitMillisRandomFromIncl, startToStartWaitMillisRandomToExcl), TimeUnit.MILLISECONDS);
    }

    public static ScheduledFuture scheduleWithFixedDelay(Runnable run, int startDelayMillis, int finishedToStartWaitMillisRandomFromIncl, int finishedToStartWaitMillisRandomToExcl) {
        return scheduledExecutor.scheduleWithFixedDelay(makeSafeRunnable(run), startDelayMillis, getRandomNumber(finishedToStartWaitMillisRandomFromIncl, finishedToStartWaitMillisRandomToExcl), TimeUnit.MILLISECONDS);
    }

    public static ScheduledFuture submitInTheFuture(Runnable run, int delayMillis) {
        return scheduledExecutor.schedule(makeSafeRunnable(run), delayMillis, TimeUnit.MILLISECONDS);
    }

    public static void cancel(ScheduledFuture future) {
        if (future != null) {
            future.cancel(false);
        }
    }

    public static void makeHappenThenScheduleWithFixedDelay(final Runnable run, final int failureDelayMS, final int finishedToStartWaitMillis) {
        makeHappenOnceOnlyInThisJVM(new Runnable() {
            @Override
            public void run() {
                run.run();
                scheduleWithFixedDelay(run, finishedToStartWaitMillis, finishedToStartWaitMillis);
            }

        }, UUID.randomUUID().toString(), failureDelayMS);
    }

    public static void makeHappen(final Runnable run, final int failureDelayMS) {
        makeHappenOnceOnlyInThisJVM(run, UUID.randomUUID().toString(), failureDelayMS);
    }

    /**
     * Run the runnable over and over with the specified delay until the
     * runnable method does not throw and exception Ensures that it will only
     * run successfully once and only once Totally thread safe
     *
     * @param run
     * @param uniqueId
     * @param failureDelayMS
     */
    public static void makeHappenOnceOnlyInThisJVM(final Runnable run, final String uniqueId, final int failureDelayMS) {
        if (successfulOnceOnlyIds.contains(uniqueId)) {
            log.debug("This has been submitted");
            return;
        }
        runOnceLock.lock();
        try {
            if (successfulOnceOnlyIds.contains(uniqueId)) {
                log.debug("This has been submitted");
                return;
            }
            successfulOnceOnlyIds.add(uniqueId);
            execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        run.run();
                    } catch (Exception e) {
                        log.warn("Runnable for [{}] in makeHappenOnceOnly got an exception. Will try again in [{}]ms: [{}]", new Object[]{uniqueId, failureDelayMS, e.toString()});
                        recurseOnFailureWithDelay(run, uniqueId, failureDelayMS);
                    }
                }
            });
        } finally {
            runOnceLock.unlock();
        }

    }

    private static void recurseOnFailureWithDelay(final Runnable run, final String uniqueId, final int failureDelayMS) {
        submitInTheFuture(new Runnable() {
            @Override
            public void run() {
                try {
                    run.run();
                } catch (Exception e) {
                    log.warn("Runnable for [{}] in recurseOnFailureWithDelay got an exception. Will try again in [{}]ms: [{}]", new Object[]{uniqueId, failureDelayMS, e.toString()});
                    recurseOnFailureWithDelay(run, uniqueId, failureDelayMS);
                }
            }
        }, failureDelayMS);
    }

    public static void runOnceAcrossAllJVMs(final Runnable run, final String uniqueId, final int gapMillis) {
        Lock lock = HazelcastHelper.getBaseHazelcastInstance().getLock(uniqueId);
        if (lock.tryLock()) {
            try {
                log.debug("Got the global lock for [{}]", uniqueId);
                Map<String, Object> state = HazelcastHelper.getBaseHazelcastInstance().<String, Object>getMap("ASYNC_GLOBAL_STATE");
                String key = "ASYNC_LAST_SUCCESS_" + uniqueId;
                Object lastSuccess = state.get(key);
                long lastSuccessTimestamp;
                if (lastSuccess == null) {
                    lastSuccessTimestamp = 0;
                } else {
                    if (gapMillis < 0) {
                        log.debug("This has run before and must only run once [{}]", uniqueId);
                        return;
                    }
                    lastSuccessTimestamp = (long) lastSuccess;
                }
                long now = System.currentTimeMillis();
                long ms = now - lastSuccessTimestamp;
                if (ms > gapMillis) {
                    log.debug("Running [{}] as it last ran [{}]ms ago", uniqueId, ms);
                    run.run();
                    state.put(key, now);
                } else {
                    log.debug("[{}] ran [{}]ms ago so not running now", uniqueId, ms);
                }
            } finally {
                lock.unlock();
            }
        } else {
            log.debug("Not running [{}] as someone else has the global lock", uniqueId);
        }
    }

    private static int getRandomNumber(int startInclusive, int endExclusive) {
        return random.nextInt(endExclusive - startInclusive) + startInclusive;
    }
}
