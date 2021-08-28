/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.platform;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.ExceptionManager;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class PlatformEventWorker implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(PlatformEventWorker.class);
    private boolean mustStop = false;
    private boolean isStopped = false;
    LinkedBlockingQueue<EventData> workQueue;

    PlatformEventWorker(LinkedBlockingQueue<EventData> workQueue) {
        this.workQueue = workQueue;
    }

    boolean isStopped() {
        return isStopped;
    }

    @Override
    public void run() {
        log.warn("Platforms Async event sending event worker [{}] has started", Thread.currentThread().getName());
        while (!mustStop) {
            try {
                process(100);
            } catch (Exception e) {
                log.warn("Error: ", e);
                log.warn("\"Platforms Async event sending event worker thread: [{}]", e.toString());
                new ExceptionManager(log).reportError(e);
            }
        }

        // Try our best to empty out the queue if we are exiting and the queue is not empty
        // If after 100000 writes its still not empty, then just exit anyway
        int cnt = 0;
        while (!workQueue.isEmpty() && cnt < 100000) {
            cnt++;
            log.warn("Processing the last bit of the Platforms Async sending Event queue. Count is [{}]", cnt);
            process(0);
        }
        if (!workQueue.isEmpty()) {
            log.warn("Exiting Platforms Event worker thread while the queue still has requests in it. Queue size is [{}]", workQueue.size());
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "SC-Platform", "Platforms Async Event Sending worker exited without queue being empty");
        }
        isStopped = true;
        log.warn("Platforms Event Worker [{}] has stopped", Thread.currentThread().getName());
    }

    public void shutDown() {
        mustStop = true;
    }

    private void process(int waitMillis) {
        try {
            EventData eventData = workQueue.poll(waitMillis, TimeUnit.MILLISECONDS);
            if (eventData != null) {
                processEvent(eventData);
            }
        } catch (Exception e) {
            log.warn("Error trying to process sending async event [{}]", e.toString());
            new ExceptionManager(log).reportError(e);
        }
    }

    private void processEvent(EventData eventData) {
        log.debug("Processing async event [{}]", eventData.getData());
        PlatformEventManager.createEventSync(eventData.getType(), eventData.getSubType(), eventData.getEventKey(), eventData.getData(), eventData.getUniqueKey(), eventData.getDate());
    }

    
}
