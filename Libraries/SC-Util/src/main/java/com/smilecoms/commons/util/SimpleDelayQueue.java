/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class SimpleDelayQueue {

    private DelayQueue qLow = null;
    private DelayQueue qMedium = null;
    private DelayQueue qHigh = null;
    private static final Logger log = LoggerFactory.getLogger(SimpleDelayQueue.class);

    public enum Priority {
        LOW, MEDIUM, HIGH
    }

    public SimpleDelayQueue() {
        qLow = new DelayQueue();
        qMedium = new DelayQueue();
        qHigh = new DelayQueue();
    }

    @SuppressWarnings(value = "unchecked")
    public void put(Object o, long millisDelay, Priority p) {
        DelayQueueItem item = new DelayQueueItem(o, millisDelay);
        switch (p) {
            case LOW:
                qLow.put(item);
                break;
            case MEDIUM:
                qMedium.put(item);
                break;
            case HIGH:
                qHigh.put(item);
        }
    }

    @SuppressWarnings(value = "unchecked")
    public void put(Object o, Priority p) {
        put(o, 0, p);
    }

    public Object poll(long timeout, TimeUnit unit) throws InterruptedException {
        try {
            DelayQueueItem item;
            item = (DelayQueueItem) qHigh.poll();
            if (item != null) {
                return item.getObject();
            }
            item = (DelayQueueItem) qMedium.poll();
            if (item != null) {
                return item.getObject();
            }
            item = (DelayQueueItem) qLow.poll();
            if (item != null) {
                return item.getObject();
            }

            int reps = 10;
            timeout = timeout / (reps * 3);
            log.debug("Starting poll loop");
            for (int i = 0; i < reps; i++) {
                item = (DelayQueueItem) qHigh.poll(timeout, unit);
                if (item != null) {
                    return item.getObject();
                }
                item = (DelayQueueItem) qMedium.poll(timeout, unit);
                if (item != null) {
                    return item.getObject();
                }
                item = (DelayQueueItem) qLow.poll(timeout, unit);
                if (item != null) {
                    return item.getObject();
                }
            }
            return null;
        } finally {
            log.debug("Finished poll loop");
        }
    }

    public int size() {
        return qLow.size() + qMedium.size() + qHigh.size();
    }

    public void clear() {
        qLow.clear();
        qMedium.clear();
        qHigh.clear();
    }

    public boolean contains(Object o) {
        boolean ret;
        ret = qLow.contains(o);
        if (ret) {
            return true;
        }
        ret = qMedium.contains(o);
        if (ret) {
            return true;
        }
        return qHigh.contains(o);
    }

    public Object[] getCopyOfAllObjectsInQueue() {
        Object[] itemsLow = qLow.toArray();
        Object[] itemsMedium = qMedium.toArray();
        Object[] itemsHigh = qHigh.toArray();
        Object[] ret = new Object[itemsLow.length + itemsMedium.length + itemsHigh.length];
        int index = 0;
        for (Object item : itemsHigh) {
            ret[index] = ((DelayQueueItem) item).getObject();
            index++;
        }
        for (Object item : itemsMedium) {
            ret[index] = ((DelayQueueItem) item).getObject();
            index++;
        }
        for (Object item : itemsLow) {
            ret[index] = ((DelayQueueItem) item).getObject();
            index++;
        }
        return ret;
    }

    /**
     * Removes all the items from the queue and returns them along with the
     * remaining milliseconds delay each one has
     *
     * @return
     */
    public Map<Object, Long> getAllForced() {
        Map<Object, Long> ret = new HashMap<>();
        while (true) {
            DelayQueueItem item = (DelayQueueItem) qHigh.peek();
            if (item != null) {
                qHigh.remove(item);
                ret.put(item.getObject(), item.getDelay(TimeUnit.MILLISECONDS));
                continue;
            }

            item = (DelayQueueItem) qMedium.peek();
            if (item != null) {
                qMedium.remove(item);
                ret.put(item.getObject(), item.getDelay(TimeUnit.MILLISECONDS));
                continue;
            }

            item = (DelayQueueItem) qLow.peek();
            if (item != null) {
                qLow.remove(item);
                ret.put(item.getObject(), item.getDelay(TimeUnit.MILLISECONDS));
                continue;
            }
            break;
        }
        return ret;
    }
}

class DelayQueueItem implements Delayed, Serializable {

    private final Object object;
    private final long delayMillis;
    private final long queueInsertTime = System.currentTimeMillis();

    public DelayQueueItem(Object object, long delayMillis) {
        this.object = object;
        this.delayMillis = delayMillis;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long queueTime = System.currentTimeMillis() - queueInsertTime;
        long timeLeft = delayMillis - queueTime;
        long ret = unit.convert(timeLeft, TimeUnit.MILLISECONDS);
        return ret;
    }

    @Override
    public int compareTo(Delayed o) {
        long otherOnesMillis = o.getDelay(TimeUnit.MILLISECONDS);
        long myMillis = getDelay(TimeUnit.MILLISECONDS);
        return (int) (myMillis - otherOnesMillis);
    }

    public Object getObject() {
        return object;
    }
}
