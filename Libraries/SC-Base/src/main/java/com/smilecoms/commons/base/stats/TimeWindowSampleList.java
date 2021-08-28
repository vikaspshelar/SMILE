/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.base.stats;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class TimeWindowSampleList {

    private final LinkedList<ListEntry> backing = new LinkedList<>();
    private final long windowMs;
    private final Lock myLock = new ReentrantLock();
    private long total;

    public TimeWindowSampleList(long windowMs) {
        this.windowMs = windowMs;
    }

    public void add(long val) {
        myLock.lock();
        try {
            total += val;
            backing.offerLast(new ListEntry(val));
        } finally {
            myLock.unlock();
        }
    }

    StatsData getStats() {
        StatsData ret = new StatsData();
        myLock.lock();
        try {
            reap();
            if (backing.isEmpty()) {
                return null;
            } else {
                double windowMillis = System.currentTimeMillis() - backing.getFirst().getTimeStamp();
                if (windowMillis == 0) {
                    windowMillis = 1;
                }
                double avg = (double) total / (double) backing.size();
                ret.setAverage(avg);
                ret.setMostRecentSample(backing.getLast().getValue());
                ret.setSumPerSecond(1000d * ((double) total / windowMillis));
                ret.setTps(1000d * ((double) backing.size() / windowMillis));
                ret.setSampleCount(backing.size());
                ret.setPercent(avg * 100.0d);
            }
        } finally {
            myLock.unlock();
        }
        return ret;
    }

    private void reap() {
        long now = System.currentTimeMillis();
        Iterator<ListEntry> it = backing.iterator();
        while (it.hasNext()) {
            ListEntry entry = it.next();
            long entryAge = now - entry.getTimeStamp();

            /*
            -5 means this window is daily stat so we remove the stat if its entry date is not the same date as now date
             */
            if (windowMs == -5) {
                //check if entry age is same day as current day
                if (!isDateToday(new Date(entry.getTimeStamp()))) {
                    total -= entry.getValue();
                    it.remove();
                }
            } else if (entryAge > windowMs) {
                total -= entry.getValue();
                it.remove();
            } else {
                break;
            }
        }
    }

    private static boolean areDatesOnSameDay(Date d1, Date d2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(d1);
        cal2.setTime(d2);
        return (cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
    }

    private static boolean isDateToday(Date d) {
        return areDatesOnSameDay(d, new Date());
    }
}

class ListEntry {

    private final long timestamp;
    private final long val;

    public ListEntry(long val) {
        this.val = val;
        timestamp = System.currentTimeMillis();
    }

    public long getTimeStamp() {
        return timestamp;
    }

    public long getValue() {
        return val;
    }
}

class StatsData {

    private long mostRecentSample;
    private double sumPerSecond;
    private double tps;
    private double average;
    private long sampleCount;
    private double percent;
    private static final Logger log = LoggerFactory.getLogger(StatsData.class);

    private double round(double d, int decimalPlace) {
        // see the Javadoc about why we use a String in the constructor
        // http://java.sun.com/j2se/1.5.0/docs/api/java/math/BigDecimal.html#BigDecimal(double)
        try {
            BigDecimal bd = new BigDecimal(Double.toString(d));
            bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_EVEN);
            return bd.doubleValue();
        } catch (Exception e) {
            log.warn("Error rounding [{}]", d, e);
            return d;
        }
    }

    public long getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(long sampleCount) {
        this.sampleCount = sampleCount;
    }

    public long getMostRecentSample() {
        return mostRecentSample;
    }

    public void setMostRecentSample(long mostRecentSample) {
        this.mostRecentSample = mostRecentSample;
    }

    public double getSumPerSecond() {
        return sumPerSecond;
    }

    public void setSumPerSecond(double sumPerSecond) {
        this.sumPerSecond = round(sumPerSecond, 2);
    }

    public double getTps() {
        return tps;
    }

    public void setTps(double tps) {
        this.tps = round(tps, 2);
    }

    public double getAverage() {
        return average;
    }

    public void setAverage(double average) {
        this.average = round(average, 2);
    }

    public double getPercent() {
        return percent;
    }

    public void setPercent(double percent) {
        this.percent = percent;
    }

}
