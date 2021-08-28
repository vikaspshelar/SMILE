/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.util;

import com.smilecoms.commons.base.BaseUtils;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to lock a presentity irrespective of its prefix e.g. tel: sip: etc as
 * MySQL cannot provide for this kind of fuzziness without getting deadlocks
 *
 * @author PCB
 */
public final class NamedLock {

    private static final Logger log = LoggerFactory.getLogger(NamedLock.class);
    private static final Map<String, Lock> locks = new ConcurrentHashMap<>();
    private final String lockname;
    private Lock lock;

    public NamedLock(final String lockname) {
        this.lockname = lockname;
        executeInNamedLock(lockname, new Runnable() {
            @Override
            public void run() {
                lock = locks.get(lockname);
                if (lock == null) {
                    lock = new ReentrantLock();
                }
                log.debug("Getting lock for [{}]", lockname);
                lock.lock();
                log.debug("Got lock for [{}]", lockname);
                // Make sure that no matter what if we have a locked lock then its in the map
                // Cause another thread could have removed it when it had the lock
                locks.put(lockname, lock);
            }
        });
        if (locks.size() > 20) {
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "SC-Utils", "We have " + locks.size() + " named locks. Something may be leaking locks!");
        }
    }

    public void unlock() {
        locks.remove(lockname);
        if (log.isDebugEnabled()) {
            log.debug("Removed lock from map for [{}]. Map size is [{}]", lockname, locks.size());
        }
        log.debug("Unlocking [{}]", lockname);
        lock.unlock();
    }

    private void executeInNamedLock(String lockName, Runnable runnable) {
        lockName = "NamedLock." + lockName;
        synchronized (lockName.intern()) {
            runnable.run();
        }
    }

}
