/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.engine;

import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.sd.ServiceDiscoveryAgent;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.direct.bm.ChargingRequest;
import com.smilecoms.commons.sca.direct.bm.UsedServiceUnit;
import com.smilecoms.commons.sca.direct.et.EventSubscription;
import com.smilecoms.commons.sca.direct.et.PlatformContext;
import com.smilecoms.commons.sca.direct.et.SubscriptionField;
import com.smilecoms.commons.sca.direct.et.SubscriptionFieldList;
import com.smilecoms.commons.sca.direct.mm.EngineMessage;
import com.smilecoms.commons.util.ExceptionManager;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.SimpleDelayQueue;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.mm.db.model.DAO;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManagerFactory;
import org.slf4j.*;

/**
 *
 * @author paul
 */
@Singleton
@Startup
@Local(BaseListener.class)
public class DeliveryEngine implements Runnable, BaseListener {

    private static final Logger log = LoggerFactory.getLogger(DeliveryEngine.class);
    private static DeliveryEngine singleton;
    private static EngineQueue deliveryQueue;
    private static DeliveryEngineWorker[] deliveryWorkers;
    private static final Map<String, Map<String, BaseMessage>> pluginCallbackLookupMap = new ConcurrentHashMap<>();
    private static final Map<String, Class> pluginClasses = new ConcurrentHashMap<>();
    private static boolean shutdown = false;
    private static EntityManagerFactory emf = null;
    private static ClassLoader myClassLoader;
    private static final Lock emfInitLock = new ReentrantLock();

    public static enum DeliveryReportStatus {
        EXPIRED,
        SUCCESS,
        FAILED
    }

    @PostConstruct
    public void startUp() {
        singleton = this;
        myClassLoader = this.getClass().getClassLoader();
        BaseUtils.registerForPropsAvailability(this);
        if (emf == null) {
            emfInitLock.lock();
            try {
                if (emf == null) {
                    emf = JPAUtils.getEMF("MMPU_RL");
                }
            } finally {
                emfInitLock.unlock();
            }
        }
    }

    @Override
    public void propsAreReadyTrigger() {
        if (emf == null) {
            emfInitLock.lock();
            try {
                if (emf == null) {
                    emf = JPAUtils.getEMF("MMPU_RL");
                }
            } finally {
                emfInitLock.unlock();
            }
        }
        BaseUtils.deregisterForPropsAvailability(this);
        BaseUtils.registerForPropsChanges(this);
        deliveryQueue = new EngineQueue(emf, "DELIVERY");
        initialisePlugins();
        startThreadPool();
    }

    @PreDestroy
    public void shutDown() {
        BaseUtils.deregisterForPropsChanges(this);
        shutdownThreadPool();
        if (deliveryQueue != null) {
            deliveryQueue.shutDown();
        }
        log.warn("Closing EMF on Delivery Engine");
        JPAUtils.closeEMF(emf);
    }

    public static DeliveryEngine getInstance() {
        return singleton;
    }

    /**
     * Called by workers to do main processing of a message
     *
     * @param msg
     */
    void pipelineMessage(BaseMessage msg) {
        log.debug("Message retrieved from queue. Message is being pipelined for delivery processing: [{}]. I am thread [{}]", msg, Thread.currentThread().getName());

        Date now = new Date();
        if (msg.getExpiryDate() != null && now.after(msg.getExpiryDate())) {
            log.debug("Dropping message because the message vailidity has expired [{}]", msg.getExpiryDate());
            msg.getDeliveryContext().setMustCharge(false);
            commitCharge(msg);
            processDeliveryReport(msg, DeliveryReportStatus.EXPIRED);
            //update SMS stored - update status
            log.debug("Message has expired so we assume it was unsuccessful");
            DAO.updateShortMessage(emf, msg.getMessageId(), "UNSUCCESSFUL");
            return;
        }

        for (String plugName : msg.getDeliveryContext().getPendingPluginNames()) {
            DeliveryPluginResult result;
            try {
                msg.getDeliveryContext().setPluginProcessing(plugName);
                DeliveryPipelinePlugin plug = getPluginByName(plugName);
                log.debug("Calling plugin [{}] to process Message [{}]", plugName, msg);
                result = plug.processMessage(msg, this);
            } catch (RerouteException reroute) {
                log.debug("This message must be rerouted to [{}]", reroute.getRouteToHost());
                try {
                    msg.getDeliveryContext().prepareForRerouting();
                    // We are a platform so we should be a hazelcast server
                    String suffix = ServiceDiscoveryAgent.getInstance().getAvailableService("MM").getURL().split(":")[2];
                    byte[] msgSerialised = EngineQueue.serialize(msg);
                    String endPoint = "http://" + reroute.getRouteToHost() + ":" + suffix;
                    EngineMessage engineMsg = new EngineMessage();
                    engineMsg.setSerialisedMessageAsBase64(Utils.encodeBase64(msgSerialised));
                    log.debug("Sending to engine at [{}]", endPoint);
                    SCAWrapper.getAdminInstance().submitToEngine_Direct(engineMsg, endPoint);
                    log.debug("Sent to engine at [{}]", endPoint);
                    return;
                } catch (Exception e) {
                    log.warn("Error rerouting message. Will retry instead", e);
                    result = new FinalDeliveryPluginResult();
                    ((FinalDeliveryPluginResult) result).setMustRetry(true);
                    ((FinalDeliveryPluginResult) result).setPluginClassName(plugName);
                }
            } catch (Throwable e) {
                log.warn("Error: ", e);
                log.warn("Error processing instance of plugin [{}] [{}]. Going to process as though the plugin failed", plugName, e.toString());
                result = new FinalDeliveryPluginResult();
                ((FinalDeliveryPluginResult) result).setMustRetry(true);
                ((FinalDeliveryPluginResult) result).setPluginClassName(plugName);
            }

            if (result instanceof InitialDeliveryPluginResult) {
                String callBackId = ((InitialDeliveryPluginResult) result).getCallBackId();
                log.debug("Plugin will be processing this message asynchronously and call back with ID [{}]", callBackId);
                Map<String, BaseMessage> pluginCallbackStates = pluginCallbackLookupMap.get(plugName);
                pluginCallbackStates.put(callBackId, msg);
                msg.getDeliveryContext().setExpiryRelative(BaseUtils.getIntProperty("env.mm.deliveryengine.msgcallback.timeout.millis", 50000));
            } else if (result instanceof FinalDeliveryPluginResult) {
                log.debug("Plugin has returned synchronously. Will call processPluginResult immediately");
                ((FinalDeliveryPluginResult) result).setPluginClassName(plugName);
                processPluginResult((FinalDeliveryPluginResult) result, msg);
            }
            log.debug("Finished calling plugin [{}] to process Message [{}]", plugName, msg);
        }
        log.debug("Message pipeline finished for: [{}]", msg);
    }

    private BaseMessage getMessageForCallBackId(String plugin, String id) {
        Map<String, BaseMessage> pluginCallbackStates = pluginCallbackLookupMap.get(plugin);
        BaseMessage msg = null;
        int retries = 0;
        while (msg == null && retries < 10) {
            msg = pluginCallbackStates.get(id);
            if (msg == null) {
                retries++;
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Trying again to get a message by callback id [{}] and plugin [{}] from map with size [{}]", new Object[]{id, plugin, pluginCallbackStates.size()});
                    }
                    Thread.sleep(1);
                } catch (Exception e) {
                }
            }
        }
        if (msg == null) {
            log.debug("Message has been timed out or result already processed for callback id [{}]", id);
        }
        return msg;
    }

    private void removeMessageForCallbackId(String plugin, String id) {
        log.debug("Removing callback id [{}] for plugin [{}]", id, plugin);
        Map<String, BaseMessage> pluginCallbackStates = pluginCallbackLookupMap.get(plugin);
        pluginCallbackStates.remove(id);
    }

    public void processPluginResult(FinalDeliveryPluginCallBack res) {
        log.debug("Processing plugin result from delivery plugin [{}] and ID [{}]", res.getPluginClassName(), res.getCallbackId());
        BaseMessage msg = getMessageForCallBackId(res.getPluginClassName(), res.getCallbackId());
        if (msg == null) {
            log.debug("Missing message for callback ID [{}]", res.getCallbackId());
            return;
        }
        try {
            msg.getDeliveryContext().lock();
            // After getting lock, verify nothing else has dealt with this message in the callback queue (e.g. timeout thread)
            if (getMessageForCallBackId(res.getPluginClassName(), res.getCallbackId()) == null) {
                log.debug("Something else has already processed this callback [{}]", msg);
                return;
            }
            removeMessageForCallbackId(res.getPluginClassName(), res.getCallbackId());
        } finally {
            msg.getDeliveryContext().unlock();
        }
        processPluginResult(res, msg);
    }

    public void processPluginResult(FinalDeliveryPluginResult res, BaseMessage msg) {
        DeliveryReportStatus reportStatus = DeliveryReportStatus.FAILED;
        try {
            // Lock message for synchronised state management
            msg.getDeliveryContext().lock();

            if (res.mustRetry()) {
                log.debug("Plugin says delivery failed and must be retried");
                if (res.getRetryTrigger() != null) {
                    log.debug("Plugin has provided a retry trigger event of [{}] and will use this in addition to a retry schedule", res.getRetryTrigger());
                }
                msg.getDeliveryContext().setPluginFailure(res.getPluginClassName(), res.getRetryTrigger());
            } else {
                log.debug("Plugin says delivery succeeded or need not be retried");
                msg.getDeliveryContext().setPluginSuccess(res.getPluginClassName());
                if (!res.mustCharge()) {
                    log.debug("The plugin says we must not charge so setting must charge to false for the message");
                    msg.getDeliveryContext().setMustCharge(false);
                }
            }

            // Check status of state machine
            switch (msg.getDeliveryContext().getMessageStatus()) {
                case DeliveryMessageContext.FINISHED:
                    log.debug("Message has been successfully processed by all plugins. Will process charging...");

                    //update SMS stored - update status
                    if (res.isPermanentDeliveryFailure()) {
                        log.debug("Result says this is a permanent delivery failure so we assume this was unsuccessful");
                        DAO.updateShortMessage(emf, msg.getMessageId(), "UNSUCCESSFUL");
                    } else {
                        log.debug("Result is not permanent delivery failure so assume this was successful");
                        DAO.updateShortMessage(emf, msg.getMessageId(), "SUCCESSFUL");
                        reportStatus = DeliveryReportStatus.SUCCESS;
                    }

                    commitCharge(msg);
                    processDeliveryReport(msg, reportStatus);
                    log.debug("Message has been fully processed [{}].", msg);
                    if (msg.getDeliveryContext().getCurrentFailureCount() == 0) {
                        // Dont let messages being sent to subscribers offline influence the stats
                        BaseUtils.addStatisticSample("Plat_MM.MessageRoundtrip", BaseUtils.STATISTIC_TYPE.latency, msg.getMessageAge(), 120000);
                    }
                    break;

                case DeliveryMessageContext.READY_FOR_RETRY:
                    long delay = getNextSubmissionDelayMillis(msg);
                    if (delay >= 0) {
                        log.debug("Resubmitting for retry of delivery as the retry millis is not negative");
                        msg.getDeliveryContext().prepareForRequeue();
                        enqueueMessage(msg, delay, msg.getDeliveryContext().getRetryTriggers());
                    } else {
                        log.warn("Not resubmitting for retry of delivery as delay is negative [{}]. Committing charge (will release reservation)", msg);
                        msg.getDeliveryContext().setMustCharge(false);
                        commitCharge(msg);
                        processDeliveryReport(msg, reportStatus);
                        //update SMS stored - update status
                        log.debug("Message retry limit has been reached so we assume this was unsuccessful");
                        DAO.updateShortMessage(emf, msg.getMessageId(), "UNSUCCESSFUL");
                    }
                    break;

                default:
                    log.debug("Message is still being processed in delivery");
            }

        } catch (Exception e) {
            log.warn("Error in processPluginResult [{}]. Printing stack trace:", e.toString());
            log.warn("Error: ", e);
        } finally {
            msg.getDeliveryContext().unlock();
        }

    }

    private DeliveryPipelinePlugin getPluginByName(String plugin) throws Exception {
        log.debug("Getting plugin for name [{}]", plugin);
        Class pluginClass = pluginClasses.get(plugin);
        return (DeliveryPipelinePlugin) pluginClass.newInstance();
    }

    private void injectPipelineConfig(BaseMessage msg) {
        if (!msg.getDeliveryContext().hasConfig()) {
            List<String> plugins = getPluginList(msg);
            msg.getDeliveryContext().setPipelineConfig(plugins);
        }
    }

    public void enqueueMessage(BaseMessage msg, long nextRedeliveryDelayMillis, List<RetryTrigger> popTriggers) {

        enqueueMessage(msg, nextRedeliveryDelayMillis);
        /**
         * Send event notifications to ET. When they fire, ET must call MM over
         * SOAP with the message ID and MM will pull the message from the queue
         * and process it immediately. If the event never fires, then next
         * redelivery delay will be processed anyway.
         *
         * If one beats the other and the message is no longer on the queue,
         * then it can be ignored.
         */
        if (!popTriggers.isEmpty()) {
            log.debug("This message has triggers that must be subscribed to. When any one of the tiggers fires, the message will be popped from the queue if its still there");
        }

        for (RetryTrigger trigger : popTriggers) {
            log.debug("Sending ET subscription for trigger [{}]", trigger);
            // Subscribe to ET            
            try {
                EventSubscription es = new EventSubscription();
                es.setPlatformContext(new PlatformContext());
                es.getPlatformContext().setOriginatingIP(BaseUtils.getIPAddress());
                es.getPlatformContext().setOriginatingIdentity("MM");
                es.getPlatformContext().setTxId(msg.getMessageId());

                switch (trigger.getTriggerType()) {
                    case RetryTrigger.SIP_REGISTER:
                        prepareEventSubscriptionForSIP_REGISTER(es, trigger.getTriggerKey(), msg.getMessageId());
                        break;
                }
                log.debug("Calling ET to subscribe for the event of type [{}] and subtype [{}]", es.getEventType(), es.getEventSubType());
                SCAWrapper.getAdminInstance().createEventSubscription_Direct(es);
                log.debug("Finished calling ET to subscribe for the event of type [{}] and subtype [{}]", es.getEventType(), es.getEventSubType());
            } catch (Exception e) {
                log.warn("Error sending event subscription: [{}]", e.toString());
            }
        }
    }

    public void onRetryTriggerFiring(String messageId) {
        // Wait a bit before sending so that the UE can complete booting fully
        deliveryQueue.popToMemory(messageId, BaseUtils.getIntProperty("global.mm.retrytrigger.send.delay.secs") * 1000);
    }

    public void enqueueMessage(BaseMessage msg, long nextRedeliveryDelayMillis) {        
        log.debug("In DeliveryEngine enqueMessage with [{}]", msg);
        if (deliveryQueue == null) {
            throw new RuntimeException("Delivery engine is not ready to receive messages");
        }
        try {
            if (msg.getDeliveryContext().isOnlyPendingDeliveryReport()) {
                log.debug("This message only has a delivery report outstanding");
                processDeliveryReport(msg, (msg.getDeliveryContext().getMessageStatus() == DeliveryMessageContext.FINISHED) ? DeliveryReportStatus.SUCCESS : DeliveryReportStatus.FAILED);
                return;
            }

            if (msg.getDeliveryContext().getMessageStatus() != DeliveryMessageContext.NEW
                    && msg.getDeliveryContext().getMessageStatus() != DeliveryMessageContext.RETRY) {
                log.warn("Incorrect message delivery state for submission to delivery queue. [{}]", msg);
                return;
            }
            injectPipelineConfig(msg);

            if (deliveryQueue.contains(msg)) {
                log.warn("This message is already in the delivery queue.  [{}]", msg);
                return;
            }
            
            log.debug("Message is being put in delivery queue: [{}] with delay [{}]", msg, nextRedeliveryDelayMillis);
            if (msg.getPriority() == null) {
                log.warn("Message has no priority. Defaulting to MEDIUM");
                msg.setPriority(BaseMessage.Priority.MEDIUM);
            }
            switch (msg.getPriority()) {
                case LOW:
                    deliveryQueue.put(msg, nextRedeliveryDelayMillis, SimpleDelayQueue.Priority.LOW);
                    break;
                case MEDIUM:
                    deliveryQueue.put(msg, nextRedeliveryDelayMillis, SimpleDelayQueue.Priority.MEDIUM);
                    break;
                case HIGH:
                    deliveryQueue.put(msg, nextRedeliveryDelayMillis, SimpleDelayQueue.Priority.HIGH);
            }
            
            log.debug("Message has now been put in delivery queue: [{}] Delay: [{}]ms", msg, nextRedeliveryDelayMillis);
        } catch (Exception ex) {
            log.warn("Error enqueueing message", ex);
        }
    }

    public void enqueueMessage(BaseMessage msg) {
        enqueueMessage(msg, 0);
    }

    public void enqueueMessage(byte[] msg) throws Exception {
        BaseMessage baseMsg = EngineQueue.deserialize(msg);
        enqueueMessage(baseMsg, 0);
    }

    @Override
    public void run() {
        while (!shutdown) {
            try {
                long now = System.currentTimeMillis();
                for (Map<String, BaseMessage> map : pluginCallbackLookupMap.values()) {
                    for (String callBackID : map.keySet()) {
                        BaseMessage msg = map.get(callBackID);
                        if (msg != null) {
                            if (msg.getDeliveryContext().expiresBefore(now)) {
                                log.warn("Message [{}] has timed out: [{}]. Callback Map size is [{}]", new Object[]{msg, callBackID, map.size()});
                                try {
                                    log.debug("Locking message for resubmission processing");
                                    msg.getDeliveryContext().lock();
                                    // double check its still processing after getting the lock
                                    if (msg.getDeliveryContext().getMessageStatus() == DeliveryMessageContext.PROCESSING) {
                                        log.debug("Removing the message from the callback map and preparing it for resubmission after timeout");
                                        map.remove(callBackID);
                                        try {
                                            msg.getDeliveryContext().prepareForRequeueAfterTimeout();
                                            log.debug("Putting message back into queue due to timeout of one or more of the plugins");
                                            enqueueMessage(msg);
                                        } catch (Exception e) {
                                            log.warn("Max timeout retries exceeded. Message will be discarded and not charged for [{}]", msg);
                                            String alertmsg = "A message has timed out trying to call a plugin and max retries has been reached. Please investigate why the plugin is not doing a callback within "
                                                    + BaseUtils.getIntProperty("env.mm.deliveryengine.msgcallback.timeout.millis", 50000) + "ms. Message details: " + msg.toString()
                                                    + " Callback Id: " + callBackID;
                                            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "MM", alertmsg);
                                            msg.getDeliveryContext().setMustCharge(false);
                                            commitCharge(msg);
                                            processDeliveryReport(msg, DeliveryReportStatus.FAILED);
                                        }
                                    }
                                } finally {
                                    msg.getDeliveryContext().unlock();
                                }
                            }
                        }
                        if (shutdown) {
                            break;
                        }
                    }
                    if (shutdown) {
                        break;
                    }
                }
                //Sleep for 10 seconds but check every second for a shutdown
                int cnt = 0;
                while (!shutdown && cnt <= 10) {
                    Thread.sleep(1000);
                    cnt++;
                }
                log.debug("Delivery memory queue size is [{}]", getMemoryQueueSize());
            } catch (Exception e) {
                log.warn("Error cleaning callback map [{}]", e.toString());
            }
        }
        log.warn("Callback Timeout Cleaner thread stopped");
    }

    public int getMemoryQueueSize() {
        if (deliveryQueue == null) {
            return 0;
        }
        return deliveryQueue.getMemoryQueueSize();
    }

    private List<String> getPluginList(BaseMessage msg) {
        List<String> returnList = new ArrayList<>();
        List<String> pluginMappings = BaseUtils.getPropertyAsList("env.mm.delivery.plugin.mappings");
        String msgType = msg.getClass().getSimpleName();
        for (String pluginLine : pluginMappings) {
            String[] bits = pluginLine.split("=");
            if (bits[0].equals(msgType)) {
                returnList.add(bits[1]);
            }
        }
        return returnList;
    }

    private long getNextSubmissionDelayMillis(BaseMessage msg) {
        long delay = -1;
        int failures = msg.getDeliveryContext().getCurrentFailureCount();
        try {
            List<String> timings;
            if (BaseUtils.getBooleanProperty("global.mm.campaign.delivery.retry.timings.on", false)
                    && msg.getCampaignId() != null
                    && !msg.getCampaignId().isEmpty()) {
                timings = BaseUtils.getPropertyAsList("global.mm.campaign.delivery.retry.timings.secs");
                log.debug("Got global retry timings for msg with campaign id [{}] [{}]", msg.getCampaignId(), timings);
            } else {
                timings = BaseUtils.getPropertyAsList("global.mm.delivery.retry.timings.secs");
            }
            /**
             * Property is carriage return list of pipe delimited strings. Each
             * line is "up to retries"|"delay seconds" e.g. property could be:
             * This will retry for 5 days and 5 minutes 5|60 = every sec for 5
             * Minutes 29|3600 = every hour for 24 Hours 77|7200 = every 2 hours
             * for 4 days
             *
             */
            for (String timePeriod : timings) {
                String[] rowArray = timePeriod.split("\\|");
                if (rowArray.length == 2) {
                    int upto = Integer.parseInt(rowArray[0]);
                    if (failures <= upto) {
                        delay = Long.parseLong(rowArray[1]) * 1000;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error calculating next redelivery delay: " + e.toString());
        } finally {
            log.debug("Returning delivery retry delay of [{}]ms for [{}] failures", delay, failures);
        }
        return delay;
    }

    @Override
    public void propsHaveChangedTrigger() {
        try {
            if (deliveryWorkers.length != BaseUtils.getIntProperty("global.mm.thread.pool.size.delivery")) {
                log.warn("Delivery Engine thread pool size configuration has changed from [{}] to [{}] so pool will be restarted", deliveryWorkers.length, BaseUtils.getIntProperty("global.mm.thread.pool.size.delivery"));
                shutdownThreadPool();
                startThreadPool();
            }
            for (String plugin : pluginClasses.keySet()) {
                log.debug("Telling plugin [{}] that properties have changed", plugin);
                try {
                    DeliveryPipelinePlugin plug = getPluginByName(plugin);
                    plug.propertiesChanged();
                } catch (Exception e) {
                    log.warn("Error telling plugin that properties changed", e);
                }
            }
            initialisePlugins();
        } catch (Exception e) {
            log.warn("Error dealing woth property changes: ", e);
        }
    }

    private void startThreadPool() {
        shutdown = false;
        int deliveryWorkerThreadPoolSize = BaseUtils.getIntProperty("global.mm.thread.pool.size.delivery");
        log.debug("Starting delivery worker thread pool of size [{}]", deliveryWorkerThreadPoolSize);
        deliveryWorkers = new DeliveryEngineWorker[deliveryWorkerThreadPoolSize];
        for (int i = 0; i < deliveryWorkerThreadPoolSize; i++) {
            deliveryWorkers[i] = new DeliveryEngineWorker(deliveryQueue, this);
            Thread t = new Thread(deliveryWorkers[i]);
            t.setContextClassLoader(myClassLoader);
            t.setName("Smile-MM-DeliveryEngine-" + i);
            t.start();
            log.debug("Started thread [{}]", t.getName());
        }
        log.warn("Delivery Engine workers startup complete with [{}] threads", deliveryWorkerThreadPoolSize);
        Thread t = new Thread(this);
        t.setName("Smile-MM-DeliveryEngineCleaner");
        t.start();
        log.warn("Delivery Engine Callback Timeout Cleaner thread started");

    }

    private void shutdownThreadPool() {
        try {
            shutdown = true;
            log.debug("Stopping delivery thread pool");
            if (deliveryWorkers != null) {
                for (DeliveryEngineWorker worker : deliveryWorkers) {
                    worker.stop();
                }
            }

            for (String plugin : pluginClasses.keySet()) {
                try {
                    DeliveryPipelinePlugin plug = getPluginByName(plugin);
                    plug.shutDown();
                } catch (Exception e) {
                    log.warn("Error shutting down plugin", e);
                }
            }
            pluginClasses.clear();
        } catch (Exception e) {
            log.warn("Error shutting down message delivery engine", e);
        }

    }

    private void prepareEventSubscriptionForSIP_REGISTER(EventSubscription es, String key, String messageId) {
        es.setEventType("IMS_USER_STATE");
        es.setEventSubType("REGISTERED");
        es.setDataMatch("");
        es.setEventKey(key);
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.HOUR, BaseUtils.getIntProperty("env.mm.sip.register.subscribe.hours", 24 * 4));
        es.setExpiryDateTime(Utils.getDateAsXMLGregorianCalendar(cal.getTime()));
        es.setRepeatable(false);
        es.setSubscriptionFieldList(new SubscriptionFieldList());
        es.getSubscriptionFieldList().getSubscriptionFields().add(new SubscriptionField());
        SubscriptionField field = es.getSubscriptionFieldList().getSubscriptionFields().get(0);
        field.setFieldName("_MESSAGE_ID_");
        field.setReplacementType("D");
        field.setReplacementData(messageId);
        es.setTemplateId(BaseUtils.getIntProperty("env.mm.messagepop.template.id"));
    }

    private void commitCharge(BaseMessage msg) {

        if (msg.isSkipCharging()) {
            log.debug("No reservation was done for this message so just returning [{}]", msg);
            return;
        }

        ChargingRequest cr = new ChargingRequest();
        com.smilecoms.commons.sca.direct.bm.ChargingData cd = new com.smilecoms.commons.sca.direct.bm.ChargingData();
        cd.setSessionId(msg.getMessageId());
        cr.getChargingData().add(cd);
        if (!msg.getDeliveryContext().mustCharge()) {
            log.debug("We must not charge for this message");
            cd.setUsedServiceUnit(new UsedServiceUnit());
            // Zero units indicates just clear out the reservation
            cd.getUsedServiceUnit().setUnitQuantity(BigDecimal.ZERO);
        }
        log.info("Calling BM for [{}]", msg);
        try {
            SCAWrapper.getAdminInstance().rateAndBill_Direct(cr);
        } catch (Exception e) {
            log.warn("Error committing charge for a message [{}] ", e.toString());
        }

        log.info("Finished calling BM to charge for message related to session id [{}]", msg.getMessageId());

    }

    private void initialisePlugins() {

        List<String> pluginMappings = null;
        while (pluginMappings == null) {
            try {
                pluginMappings = BaseUtils.getPropertyAsList("env.mm.delivery.plugin.mappings");
            } catch (Exception e) {
                log.warn("Cannot get plugin mappings yet [{}]", e.toString());
                Utils.sleep(1000);
            }
        }
        for (String pluginLine : pluginMappings) {
            String[] bits = pluginLine.split("=");
            String plugin = bits[1];
            Class pluginClass = pluginClasses.get(plugin);
            if (pluginClass == null) {
                try {
                    log.debug("Going to perform once off initialisation of plugin [{}]", plugin);
                    pluginClass = myClassLoader.loadClass(plugin);
                    pluginClasses.put(plugin, pluginClass);
                    DeliveryPipelinePlugin tmp = (DeliveryPipelinePlugin) pluginClass.newInstance();
                    tmp.initialise(emf);
                    Map<String, BaseMessage> pluginCallbackStates = new ConcurrentHashMap<>();
                    pluginCallbackLookupMap.put(plugin, pluginCallbackStates);
                } catch (Exception e) {
                    log.warn("Error creating or initialising plugin", e);
                    BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "MM", "a MM plugin failed to initialise: " + e.toString());
                }
            }
        }

    }

    private void processDeliveryReport(BaseMessage msg, DeliveryReportStatus reportStatus) {
        log.debug("In processDeliveryReport for message [{}]", msg);
                 
        if (msg.getDeliveryReportHandle() == null
                || msg.getDeliveryReportHandle().getDeliveryReportPlugin() == null
                || msg.getDeliveryReportHandle().getDeliveryReportData() == null) {
            
            log.debug("No delivery report processing necessary");
            return;
        }
        try {
            msg.getDeliveryContext().prepareForDeliveryReport();
            DeliveryPipelinePlugin plug = getPluginByName(msg.getDeliveryReportHandle().getDeliveryReportPlugin());
            log.debug("Calling [{}] to send delivery report", msg.getDeliveryReportHandle().getDeliveryReportPlugin());
            plug.sendDeliveryReport(reportStatus, msg.getDeliveryReportHandle().getDeliveryReportData());
            log.debug("Finished calling [{}] to send delivery report", msg.getDeliveryReportHandle().getDeliveryReportPlugin());
        } catch (RerouteException reroute) {
            try {
                String suffix = ServiceDiscoveryAgent.getInstance().getAvailableService("MM").getURL().split(":")[2];
                byte[] msgSerialised = EngineQueue.serialize(msg);
                String endPoint = "http://" + reroute.getRouteToHost() + ":" + suffix;
                EngineMessage engineMsg = new EngineMessage();
                engineMsg.setSerialisedMessageAsBase64(Utils.encodeBase64(msgSerialised));
                log.debug("Sending to engine at [{}]", endPoint);
                SCAWrapper.getAdminInstance().submitToEngine_Direct(engineMsg, endPoint);
                log.debug("Sent to engine at [{}]", endPoint);
            } catch (Exception e) {
                log.warn("Error rerouting delivery report", e);
                new ExceptionManager(log).reportError(e);
            }
        } catch (Exception e) {
            log.warn("Error in processDeliveryReport", e);
            new ExceptionManager(log).reportError(e);
        }
    }
}
