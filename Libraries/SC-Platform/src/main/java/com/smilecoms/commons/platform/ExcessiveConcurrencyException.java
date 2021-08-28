/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.platform;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.Utils;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author PCB
 */
class ExcessiveConcurrencyException extends RuntimeException {

    private static long lastThreadDump = 0;
    private static final Logger log = LoggerFactory.getLogger(ExcessiveConcurrencyException.class);
    private static final Lock lock = new ReentrantLock();

    public ExcessiveConcurrencyException(String msg) {
        super(msg);
        if (lock.tryLock()) {
            try {
                long now = System.currentTimeMillis();
                if (!BaseUtils.IN_GRACE_PERIOD && now - lastThreadDump > 120000) {
                    lastThreadDump = now;
                    log.warn("We have an ExcessiveConcurrencyException and last thread dump was done more than 2 minutes ago. Going to do one now");
                    log.warn(Utils.getFullThreadDump());
                }
            } finally {
                lock.unlock();
            }
        }
    }

}
