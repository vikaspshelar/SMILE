/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.util;

import org.slf4j.Logger;

/**
 *
 * @author paul
 */
public class Stopwatch {

    private static final ThreadLocal<Long> startTimers = new ThreadLocal<Long>();
    private static final ThreadLocal<Long> endTimers = new ThreadLocal<Long>();

    public static void start() {
        startTimers.set(System.nanoTime());
    }

    public static void stop() {
        endTimers.set(System.nanoTime());
    }

    public static float nanos() {
        try {
            long end = endTimers.get();
            long start = startTimers.get();
            long res = end - start;
            return (res < 0 ? -1 : res);
        } catch (Exception e) {
            return -1;
        }
    }

    public static String millisString() {
        return String.format("%.3f", nanos() / 1000000f) + "ms";
    }
    
    public static float millis() {
        return nanos() / 1000000f;
    }

    public static void logTimer(Logger log) {
        if (!log.isDebugEnabled()) {
            return;
        }
        log.debug("Stopwatch measured: {}", millisString());
    }

    public static void logTimer(Logger log, String description) {
        if (!log.isDebugEnabled()) {
            return;
        }
        log.debug("{} took: {}", description, millisString());
    }

    public static void stopAndLogTimer(Logger log) {
        if (!log.isDebugEnabled()) {
            return;
        }
        stop();
        log.debug("Stopwatch measured: {}", millisString());
    }

    public static void stopAndLogTimer(Logger log, String description) {
        if (!log.isDebugEnabled()) {
            return;
        }
        stop();
        log.debug("{} took: {}", description, millisString());
    }
}
