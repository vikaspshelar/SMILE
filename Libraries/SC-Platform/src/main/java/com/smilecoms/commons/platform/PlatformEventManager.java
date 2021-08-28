package com.smilecoms.commons.platform;

import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.ExceptionManager;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Startup
public class PlatformEventManager implements BaseListener {

    private static final Logger log = LoggerFactory.getLogger(PlatformEventManager.class);
    private static final LinkedBlockingQueue<EventData> workQueue = new LinkedBlockingQueue<>();

    private static void enqueueForWriting(EventData eventData) {
        try {
            workQueue.add(eventData);
                    
            if(eventData.getType().equalsIgnoreCase("BI") && eventData.getSubType().contains("FI-ext")) {
                log.warn("AddedVoiceCall event with eventKey: {} ", eventData.getEventKey());
            }
            if (log.isDebugEnabled()) {
                log.debug("Platforms Event queue size is now [{}]", workQueue.size());
            }
            if (workQueue.size() > 10000) {
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "SC-Platform", "Platforms Event queue has exceeded 10000 records");
            }
        } catch (Exception e) {
            log.warn("Error enqueueing event for writing: [{}]", e.toString());
            new ExceptionManager(log).reportError(e);
        }
    }

    @PostConstruct
    public void startUp() {
    }

    @Override
    public void propsAreReadyTrigger() {

    }

    @PreDestroy
    public void shutDown() {
        BaseUtils.deregisterForPropsChanges(this);
        shutdownWorker();
    }

    @Override
    public void propsHaveChangedTrigger() {
    }

    private static PlatformEventWorker worker;
    private static boolean started = false;

    private synchronized static void startupWorker() {
        if (started) {
            return;
        }
        started = true;
        log.warn("Starting Platforms Event worker");
        worker = new PlatformEventWorker(workQueue);
        Thread t = new Thread(worker);
        t.setName("Smile-Plat-AsyncEventSendingWorker");
        t.start();
        log.debug("Started Platforms Event worker thread [{}]", t.getName());
    }

    private void shutdownWorker() {
        if (!started || worker == null) {
            return;
        }
        log.warn("Platforms Event Daemon is shutting down worker...");
        worker.shutDown();
        boolean isStopped = false;
        int loops = 0;
        while (!isStopped && loops < 1000) {
            loops++;
            isStopped = worker.isStopped();
            if (!isStopped) {
                Utils.sleep(10);
            }
        }
        log.warn("Platforms Event Daemon worker shutdown complete. Did it stop? [{}]", isStopped);
    }

    public static boolean isOn() {
        return BaseUtils.getBooleanPropertyFailFast("env.eventmanager.write.events", false);
    }

    public static boolean createEvent(String type, String subType, String eventKey, Object soapObject) {
        if (!isOn()) {
            return false;
        }
        String data = Utils.marshallSoapObjectToString(soapObject);
        if (data == null) {
            log.debug("Not called via SOAP");
            data = "Request not sent via SOAP";
        }
        if (data.length() > 10000) {
            data = data.substring(0, 10000) + "...TOO LARGE TO MARSHAL FULLY...";
        }
        return createEvent(type, subType, eventKey, data, null, null);
    }

    public static boolean createEvent(String type, String subType, String eventKey, String data) {
        if (!isOn()) {
            return false;
        }
        return createEvent(type, subType, eventKey, data, null, null);
    }

    public static boolean createEvent(String type, String subType, String eventKey, String data, Date date) {
        if (!isOn()) {
            return false;
        }
        return createEvent(type, subType, eventKey, data, null, date);
    }

    public static boolean createEvent(String type, String subType, String eventKey, String data, String uniqueKey) {
        if (!isOn()) {
            return false;
        }
        return createEvent(type, subType, eventKey, data, uniqueKey, null);
    }

    public static boolean createEvent(String type, String subType, String eventKey, String data, String uniqueKey, Date date) {
        if (BaseUtils.getBooleanPropertyFailFast("env.eventmanager.create.all.events.async", false)) {
            log.debug("All events must be sent async. Sending this even into the queue");
            createEventAsync(type, subType, eventKey, data, uniqueKey, date);
            return true;
        } else {
            return createEventSync(type, subType, eventKey, data, uniqueKey, date);
        }
    }

    public static void createEventAsync(String type, String subType, String eventKey, String data) {
        createEventAsync(type, subType, eventKey, data, null, null);
    }
    
    public static void createEventAsync(String type, String subType, String eventKey, String data, String uniqueKey, Date date) {
        if (!started) {
            startupWorker();
        }
        EventData eventData = new EventData(type, subType, eventKey, data, uniqueKey, date);
        enqueueForWriting(eventData);
    }

    /**
     * Use own entity manager with own transaction
     *
     * @param type
     * @param subType
     * @param eventKey
     * @param data
     * @param uniqueKey
     * @param date
     */
    protected static boolean createEventSync(String type, String subType, String eventKey, String data, String uniqueKey, Date date) {
        if (!isOn()) {
            return false;
        }

        EntityManager em = null;
        try {
            em = JPAUtils.getEM(Platform.getSPEMF());
            JPAUtils.beginTransaction(em);
            boolean eventAdded = createEventInternal(em, type, subType, eventKey, data, uniqueKey, date);
            if (eventAdded) {
                JPAUtils.commitTransaction(em);
            } else {
                JPAUtils.rollbackTransaction(em);
            }
            return eventAdded;
        } finally {
            try {
                JPAUtils.closeEM(em);
            } catch (Exception ex) {
                log.warn("Failed to close em after adding event : " + ex.toString());
            }
        }
    }

    // Unique key prevents the same event from being inserted twice. Useful for stuff you only want to happen once
    private static boolean createEventInternal(EntityManager em, String type, String subType, String eventKey, String data, String uniqueKey, Date date) {
        boolean eventAdded = false;
        if (!isOn()) {
            return eventAdded;
        }
        if (log.isDebugEnabled()) {
            log.debug("In createEvent");
        }
        try {
            Query q = em.createNativeQuery("insert into event_data (TYPE,SUB_TYPE,EVENT_KEY,UNIQUE_KEY,EVENT_TIMESTAMP,DATA,STATUS) values(?,?,?,?,?,?,'N')");
            q.setParameter(1, type);
            q.setParameter(2, subType);
            q.setParameter(3, eventKey);
            q.setParameter(4, uniqueKey);
            q.setParameter(5, date == null ? new Date() : date);
            q.setParameter(6, data);

            if (log.isDebugEnabled()) {
                log.debug("About to persist new row in event_data table");
            }
            try {
                q.executeUpdate();
                eventAdded = true;
                if (log.isDebugEnabled()) {
                    log.debug("Persisted new row in event_data");
                }
            } catch (Exception sql) {
                if (log.isDebugEnabled()) {
                    log.debug("Duplicate event with unique key [{}] : [{}]", uniqueKey, sql.toString());
                }
            }
        } catch (Exception e) {
            log.warn("Error persisting new event in DB : " + e.toString());
        } finally {
            if (log.isDebugEnabled()) {
                log.debug("Finished createEvent");
            }
        }
        return eventAdded;
    }
}
