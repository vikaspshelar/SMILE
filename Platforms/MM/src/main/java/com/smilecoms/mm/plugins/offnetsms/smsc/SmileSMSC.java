/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.plugins.offnetsms.smsc;

import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppServerConfiguration;
import com.cloudhopper.smpp.SmppServerHandler;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppServer;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.pdu.BaseSm;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppProcessingException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.Member;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.hazelcast.HazelcastHelper;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.mm.engine.DeliveryEngine;
import com.smilecoms.mm.engine.FinalDeliveryPluginCallBack;
import com.smilecoms.mm.plugins.offnetsms.OffnetSMSDeliveryPlugin;
import com.smilecoms.mm.plugins.offnetsms.OffnetSMSMessage;
import com.smilecoms.mm.plugins.offnetsms.smsc.pdu.OffnetBaseSm;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class SmileSMSC {

    private static EntityManagerFactory emf;
    private SmppServerConfiguration serverConfiguration;
    private DefaultSmppServer smppServer;
    private static final Logger log = LoggerFactory.getLogger(SmileSMSC.class);
    private boolean shouldBeRunning;
    private boolean started;
    private int configVersion;
    private RemoteSMSCConnectionManager remoteSMSCConnectionManager;
    private static SmileSMSC singleton;
    private static int messageId = 1;
    private static final Lock lock = new ReentrantLock();
    private static HazelcastInstance hazelcastInstance = null;
    public static final String HAZELCAST_SMPP_MAP_NAME = "distributed-multipartsmpp-map";
    public static final String HAZELCAST_SMPP_RESP_MAP_NAME = "distributed-multipartsmpp-response-map";

    public static SmileSMSC boot(EntityManagerFactory emf) throws Exception {
        singleton = new SmileSMSC(emf);
        singleton.start();
        return singleton;
    }

    public static SmileSMSC getInstance() {
        if (singleton == null) {
            log.debug("Singleton is null. Going to boot");
            try {
                return boot(SmileSMSC.emf);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        return singleton;
    }

    private SmileSMSC(EntityManagerFactory emf) {
        log.debug("In Smile SMSC constructor");
        if (emf != null) {
            SmileSMSC.emf = emf;
        } else {
            log.warn("We were given a null emf");
        }
    }

    private static String getAndIncrementMessageId() {
        lock.lock();
        try {
            messageId++;
            if (messageId > BaseUtils.getIntProperty("env.mm.smpp.max.messageid", 999999)) {
                messageId = 1;
            }
            return "" + messageId;
        } finally {
            lock.unlock();
        }
    }

    public void sendDeliveryReport(DeliveryEngine.DeliveryReportStatus reportStatus, HashMap<String, Serializable> deliveryReportData) throws Exception {
        if (!shouldBeRunning || !started) {
            log.warn("Smile SMSC is not running. Wont send delivery report");
            return;
        }
        try {
            RemoteSMSC.sendDeliveryReport(reportStatus, deliveryReportData);
        } catch (NoRouteToSMSCException e) {
            log.debug("We have no route to SMSC with trunk Id [{}]", deliveryReportData.get("TrunkId"));
            throw e;
        }

    }

    public String sendMessageOffnet(OffnetSMSMessage offnetSMS) throws Exception {
        if (!shouldBeRunning || !started) {
            log.warn("Smile SMSC is not running. Wont send message");
            return null;
        }
        try {
            return RemoteSMSC.sendMessage(offnetSMS, null);
        } catch (NoRouteToSMSCException e) {
            log.debug("We have no route to SMSC with trunk Id [{}]", offnetSMS.getDestinationTrunkId());
            throw e;
        }
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    private void start() throws Exception {
        log.debug("In Smile SMSC start");
        shouldBeRunning = true;

        initialiseHazelcast();

        try {
            this.configVersion = BaseUtils.getIntSubProperty("env.mm.smsc.config", "config_version");
        } catch (Exception e) {
            log.warn("Smile SMSC cannot run without property env.mm.smsc.config");
            return;
        }
        if (!BaseUtils.getBooleanSubProperty("env.mm.smsc.config", "enabled")) {
            log.warn("Smile SMSC is disabled");
            return;
        }
        try {
            startSmppServer();
        } catch (Exception e) {
            log.warn("Error starting Smile SMSC. RemoteSMSCConnectionManager will try again later", e);
        }
        log.debug("Starting thread to manage remote SMSC Connections");
        remoteSMSCConnectionManager = new RemoteSMSCConnectionManager();
        Thread managerThread = new Thread(remoteSMSCConnectionManager);
        managerThread.setContextClassLoader(this.getClass().getClassLoader());
        managerThread.setName("Smile-MM-RemoteSMSCConnectionManager");
        // This thread will go through all the remote SMSC's in interconnect_smsc and connect to them if enabled and keep the connections up
        managerThread.start();
        log.debug("Finished starting thread to manage remote SMSC Connections");
    }

    private void restart() throws Exception {
        stop();
        start();
    }

    public void stop() throws Exception {
        log.debug("In SmileSMSC stop");
        stopSmppServer();
        shouldBeRunning = false;
        int tries = 0;
        while (remoteSMSCConnectionManager != null && remoteSMSCConnectionManager.isRunning() && tries < 30) {
            log.warn("RemoteSMSCConnectionManager is still exiting... will wait for a max of 30 seconds");
            Utils.sleep(1000);
            tries++;
        }
        HazelcastHelper.shutdown(hazelcastInstance);
        log.debug("Finished SmileSMSC stop");
    }

    public boolean shouldSMSCBeRunning() {
        return shouldBeRunning;
    }

    public void reloadConfiguration() throws Exception {
        log.debug("In reloadConfiguration");
        if (this.configVersion != BaseUtils.getIntSubProperty("env.mm.smsc.config", "config_version")) {
            restart();
        }
    }

    public void onNewMessageFromOffnet(String fromAddress, String toAddress, Date expiryDate, HashMap<String, Serializable> deliveryReportData, byte[] message, byte dataCodingScheme, InterconnectSMSC configOfReceivingSMSC) throws Exception {
        OffnetSMSDeliveryPlugin.onNewMessageFromOffnet(fromAddress, toAddress, deliveryReportData, message, dataCodingScheme, configOfReceivingSMSC, expiryDate);
    }

    public void onNewAsyncResponseFromOffnet(FinalDeliveryPluginCallBack finalDeliveryPluginResult) throws Exception {
        DeliveryEngine.getInstance().processPluginResult(finalDeliveryPluginResult);
    }

    private void stopSmppServer() {
        log.debug("Stopping SMPP Server on Smile SMSC");
        try {
            if (smppServer != null) {
                smppServer.stop();
            }
        } catch (Exception e) {
            log.warn("Error stopping Smile SMPP Server", e);
        }
        log.debug("Finished stopping SMPP Server on Smile SMSC");
    }

    protected static EntityManagerFactory getEMF() {
        return emf;
    }

    private void initialiseHazelcast() {
        log.debug("Initialising hazelcast");
        HazelcastHelper.shutdown(hazelcastInstance);
        hazelcastInstance = HazelcastHelper.newHazelcastClientInstance("SMSC", getClass().getClassLoader());
        log.debug("Finished initialising hazelcast");
    }

    public static HazelcastInstance getHazelcastInstance() {
        log.debug("Getting hazelcast instance");
        return hazelcastInstance;
    }

    void startSmppServer() throws Exception {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        ScheduledThreadPoolExecutor monitorExecutor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(BaseUtils.getIntSubProperty("env.mm.smsc.config", "monitor_executor_threads"), new ThreadFactory() {
            private final AtomicInteger sequence = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("Smile-MM-SmppServerWindowMonitor-" + sequence.getAndIncrement());
                return t;
            }
        });

        /*
         config_version=1
         enabled=true
         monitor_executor_threads=1
         connection_timer_millis=30000
         port=2775
         max_connections=100
         non_blocking=true
         request_expiry_timeout_millis=5000
         window_monitor_interval_millis=15000
         window_size=100
         window_wait_timeout_millis=30000
         counters_enabled=false
         jmx_enabled=false
         */
        serverConfiguration = new SmppServerConfiguration();
        serverConfiguration.setPort(BaseUtils.getIntSubProperty("env.mm.smsc.config", "port"));
        serverConfiguration.setMaxConnectionSize(BaseUtils.getIntSubProperty("env.mm.smsc.config", "max_connections"));
        serverConfiguration.setNonBlockingSocketsEnabled(BaseUtils.getBooleanSubProperty("env.mm.smsc.config", "non_blocking"));
        serverConfiguration.setDefaultRequestExpiryTimeout(BaseUtils.getIntSubProperty("env.mm.smsc.config", "request_expiry_timeout_millis"));
        serverConfiguration.setDefaultWindowMonitorInterval(BaseUtils.getIntSubProperty("env.mm.smsc.config", "window_monitor_interval_millis"));
        serverConfiguration.setDefaultWindowSize(BaseUtils.getIntSubProperty("env.mm.smsc.config", "window_size"));
        serverConfiguration.setDefaultWindowWaitTimeout(BaseUtils.getIntSubProperty("env.mm.smsc.config", "window_wait_timeout_millis"));
        serverConfiguration.setDefaultSessionCountersEnabled(BaseUtils.getBooleanSubProperty("env.mm.smsc.config", "counters_enabled"));
        serverConfiguration.setJmxEnabled(BaseUtils.getBooleanSubProperty("env.mm.smsc.config", "jmx_enabled"));
        serverConfiguration.setReuseAddress(true);

        class DefaultSmppServerHandler implements SmppServerHandler {

            @Override
            public void sessionBindRequested(Long sessionId, SmppSessionConfiguration sessionConfiguration, final BaseBind bindRequest) throws SmppProcessingException {
                // Put the system Id in the name
                log.debug("In Smile SMSC sessionBindRequested for system Id [{}]", bindRequest.getSystemId());
                try {
                    RemoteSMSC remoteSMSC = RemoteSMSC.getOrCreateRemoteSMSCBySystemId(bindRequest.getSystemId());

                    if (remoteSMSC.getConfig().getEnabled() != 1) {
                        log.warn("This SMSC [{}] is not enabled yet someone is trying to bind to it.", remoteSMSC.getConfig());
                        throw new Exception("SMSC received bind attempt but is disabled");
                    }

                    String suppliedPass = bindRequest.getPassword();
                    String myPassForOtherSMSC = remoteSMSC.getConfig().getPassword();
                    if (!suppliedPass.equals(myPassForOtherSMSC)) {
                        String msg = String.format("Invalid password [%s] expecting [%s]", suppliedPass, myPassForOtherSMSC);
                        log.warn(msg);
                        BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "SMPP", msg);
                        throw new Exception(msg);
                    } else {
                        log.debug("Correct password [{}]", suppliedPass);
                    }
                    remoteSMSC.storeBind();
//                    BaseUtils.sendStatistic(BaseUtils.getHostNameFromKernel(), remoteSMSC.getConfig().getSystemId(), "isup", 1, "SMPP Endpoint Test");
                } catch (Exception e) {
                    log.warn("Failed to authenticate Remote SMSC", e);
                    SmppProcessingException ex = new SmppProcessingException(SmppConstants.STATUS_INVPASWD);
                    throw ex;
                }
                sessionConfiguration.setName(bindRequest.getSystemId());
            }

            @Override
            public void sessionCreated(Long sessionId, final SmppServerSession session, BaseBindResp preparedBindResponse) throws SmppProcessingException {
                log.debug("SmileSMSC Session created: [{}] [{}]", session.getConfiguration().getSystemId(), session);
                try {
                    RemoteSMSC remoteSMSC = RemoteSMSC.getOrCreateRemoteSMSCBySystemId(session.getConfiguration().getSystemId());
                    remoteSMSC.setServerSession(session);
                } catch (Exception e) {
                    log.warn("Error getting SMSC on sessionCreation");
                }

                class SmppSessionHandler extends DefaultSmppSessionHandler {

                    @Override
                    public PduResponse firePduRequestReceived(PduRequest pduRequest) {
                        log.debug("SmileSMSC New SMS received by server [{}] with command id [{}]", pduRequest, pduRequest.getCommandId());

                        PduResponse resp = pduRequest.createResponse();
                        if (pduRequest.getCommandId() == SmppConstants.CMD_ID_SUBMIT_SM) {
                            SubmitSmResp submitResp = (SubmitSmResp) resp;
                            String mId = getAndIncrementMessageId();
                            try {
                                RemoteSMSC.onNewMessageFromOffnet(session.getConfiguration().getName(), (BaseSm) pduRequest, mId, session.toString());
                            } catch (Exception e) {
                                log.warn("Exception processing incoming PDU: ", e);
                                submitResp.setCommandStatus(SmppConstants.STATUS_DELIVERYFAILURE);
                                return submitResp;
                            }
                            submitResp.setMessageId(mId);
                            return submitResp;
                        } else if (pduRequest.getCommandId() == SmppConstants.CMD_ID_ENQUIRE_LINK) {
                            try {
                                RemoteSMSC remoteSMSC = RemoteSMSC.getRemoteSMSCBySystemId(session.getConfiguration().getName());
                                if (remoteSMSC != null) {
                                    /* all we want do here is update the remote sms bind table */
                                    int interconnectSMSCBindId = DAO.updateBind(SmileSMSC.getEMF(), remoteSMSC.getInterconnectSMSCBindId(), remoteSMSC.getConfig().getInterconnectSmscId());
                                    remoteSMSC.setInterconnectSMSCBindId(interconnectSMSCBindId);
                                    if (remoteSMSC.getConfig().getClientBind() == 0) {
                                        BaseUtils.sendStatistic(BaseUtils.getHostNameFromKernel(), remoteSMSC.getConfig().getSystemId(), "isup", 1, "SMPP Endpoint Test");
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("Exception processing incoming EnquireLink PDU: ", e);
                            }
                        } else {
                            log.debug("Got a request with command id [{}]", pduRequest.getCommandId());
                        }
                        return resp;
                    }

                    @Override
                    public void fireExpectedPduResponseReceived(PduAsyncResponse pduAsyncResponse) {
                        log.debug("ExpectedPduResponseReceived RemoteSMSC [{}] Response message: [{}]", session.getConfiguration(), pduAsyncResponse.getResponse().getResultMessage());

                        String callbackId = session.getConfiguration().getSystemId() + "_" + pduAsyncResponse.getResponse().getSequenceNumber();
                        OffnetBaseSm offnetBaseSm = new OffnetBaseSm((BaseSm) (pduAsyncResponse.getRequest()));
                        boolean multiPart = false;
                        try {
                            multiPart = offnetBaseSm.isMultipart();

                        } catch (Exception e) {
                            String msg = String.format("Exception caught while checking if response is for multipart - going to ignore this response: " + e.toString());
                            log.warn(msg);
                            BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "SMPP", msg);
                        }

                        if (multiPart) {
                            callbackId = session.getConfiguration().getSystemId() + "_MP_" + offnetBaseSm.getMessageId();
                        }

                        FinalDeliveryPluginCallBack callback = new FinalDeliveryPluginCallBack();
                        callback.setPluginClassName(OffnetSMSDeliveryPlugin.class.getName());
                        callback.setCallbackId(callbackId);

                        if (pduAsyncResponse.getResponse().getCommandStatus() != SmppConstants.STATUS_OK) {

                            try {
                                if (BaseUtils.getPropertyAsSet("env.mm.smpp.permanent.failure.codes.business.error").contains(String.valueOf(pduAsyncResponse.getResponse().getCommandStatus()))) {
                                    log.debug("This is a permanent failure, business error so don't log an error");
                                    callback.setMustCharge(false);
                                    callback.setMustRetry(false);
                                    callback.setPermanentDeliveryFailure(true);
                                } else if (BaseUtils.getPropertyAsSet("env.mm.smpp.permanent.failure.codes.system.error").contains(String.valueOf(pduAsyncResponse.getResponse().getCommandStatus()))) {
                                    log.error("This is a permanent failure, system error so we log an error");
                                    String result = "StatusCode:" + String.valueOf(pduAsyncResponse.getResponse().getCommandStatus()) + " ResultMessage:" + pduAsyncResponse.getResponse().getResultMessage()
                                            + " CodeDescription:" + SmppConstants.STATUS_MESSAGE_MAP.get(pduAsyncResponse.getResponse().getCommandStatus());
                                    String msg = String.format("Server SMS submission to remote SMSC over SMPP Failed. Partner [%s] result: [%s]", session.getConfiguration().getSystemId(), result);
                                    log.warn(msg);
                                    BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "SMPP", msg);
                                    callback.setMustCharge(false);
                                    callback.setMustRetry(false);
                                    callback.setPermanentDeliveryFailure(true);

                                } else if (BaseUtils.getPropertyAsSet("env.mm.smpp.temporary.failure.codes").contains(String.valueOf(pduAsyncResponse.getResponse().getCommandStatus()))) {
                                    log.debug("This is a temporary failure");
                                    callback.setMustCharge(false);
                                    callback.setMustRetry(true);
                                } else {
                                    String result = "StatusCode:" + String.valueOf(pduAsyncResponse.getResponse().getCommandStatus()) + " ResultMessage:" + pduAsyncResponse.getResponse().getResultMessage()
                                            + " CodeDescription:" + SmppConstants.STATUS_MESSAGE_MAP.get(pduAsyncResponse.getResponse().getCommandStatus());
                                    String msg = String.format("Server SMS submission to remote SMSC over SMPP Failed. Partner [%s] result: [%s]", session.getConfiguration().getSystemId(), result);
                                    log.warn(msg);
                                    BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "SMPP", msg);
                                    callback.setMustCharge(false);
                                    callback.setMustRetry(true);
                                }
                            } catch (com.smilecoms.commons.base.props.PropertyFetchException e) {
                                String result = "StatusCode:" + String.valueOf(pduAsyncResponse.getResponse().getCommandStatus()) + " ResultMessage:" + pduAsyncResponse.getResponse().getResultMessage()
                                        + " CodeDescription:" + SmppConstants.STATUS_MESSAGE_MAP.get(pduAsyncResponse.getResponse().getCommandStatus());
                                String msg = String.format("Server SMS submission to remote SMSC over SMPP Failed. Partner [%s] result: [%s]", session.getConfiguration().getSystemId(), result);
                                log.warn(msg);
                                BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "SMPP", msg);
                                callback.setMustCharge(false);
                                callback.setMustRetry(true);
                            }

                            try {
                                SmileSMSC.getInstance().onNewAsyncResponseFromOffnet(callback);
                            } catch (Exception e) {
                                log.warn("Exception running callback on SMPP response", e);
                                BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "SMPP", "Exception running callback on SMPP response");
                                return;
                            }
                            return;
                        }

                        try {

                            if (multiPart) {
                                boolean completeMessage = false;
                                String key = MultiPartSmppMessage.getKey(offnetBaseSm.getFrom(), offnetBaseSm.getTo(), offnetBaseSm.getMessageId());
                                log.debug("key is: [{}]", key);

                                IMap<String, MultiPartSmppMessage> multipartSMPPResponseMap = SmileSMSC.getHazelcastInstance().getMap(SmileSMSC.HAZELCAST_SMPP_RESP_MAP_NAME);
                                log.debug("Getting lock on hazelcast map for key [{}]", key);
                                multipartSMPPResponseMap.lock(key);
                                log.debug("Got lock on hazelcast map for key [{}]", key);


                                try {
                                    if (log.isDebugEnabled()) {
                                        for (Member m : SmileSMSC.getHazelcastInstance().getCluster().getMembers()) {
                                            log.debug("Member exists at [{}]", m.getSocketAddress().getAddress().getHostAddress());
                                        }
                                    }
                                    MultiPartSmppMessage multiPartResponse = multipartSMPPResponseMap.get(key); //does message already exist?
                                    if (multiPartResponse == null) {
                                        //new MP message
                                        MultiPartSmppMessage newMPResponse = new MultiPartSmppMessage(offnetBaseSm.getFrom(), offnetBaseSm.getTo(), offnetBaseSm.getTotalMessages(), offnetBaseSm.getMessageId());
                                        newMPResponse.getParts().put(offnetBaseSm.getCurrentMessageNum(), "" + offnetBaseSm.getSequenceNumber());
                                        multipartSMPPResponseMap.put(key, newMPResponse, BaseUtils.getIntProperty("env.hazelcast.config.smpp.ttl", 1200), TimeUnit.SECONDS);

                                        if (log.isDebugEnabled()) {
                                            log.debug("Added multipart message to Map. Size is now: [{}]", multipartSMPPResponseMap.size());
                                        }
                                        completeMessage = false; //only go final once you have the whole message
                                    } else {
                                        multiPartResponse.getParts().put(offnetBaseSm.getCurrentMessageNum(), "" + offnetBaseSm.getSequenceNumber());
                                        //now check if we have all the parts..... if we do, we can queue, otherwise we carry on waiting..
                                        if (multiPartResponse.hasAllParts()) {
                                            //ready to acknowledge
                                            if (log.isDebugEnabled()) {
                                                log.debug("Received response for all parts of multipart message. Going to give final plugin result");
                                            }
                                            multipartSMPPResponseMap.remove(key);
                                            if (log.isDebugEnabled()) {
                                                log.debug("Removed multipart message and size is now: [{}]", multipartSMPPResponseMap.size());
                                            }
                                            completeMessage = true;
                                        } else {
                                            log.debug("We dont have all of the parts of this message yet");
                                            multipartSMPPResponseMap.put(key, multiPartResponse, BaseUtils.getIntProperty("env.hazelcast.config.smpp.ttl", 1200), TimeUnit.SECONDS);
                                            //we don't have all the parts yet.
                                            completeMessage = false;
                                        }
                                    }
                                } finally {
                                    log.debug("Unlocking hazelcast map for key [{}]", key);
                                    multipartSMPPResponseMap.unlock(key);
                                    log.debug("Unlocked hazelcast map for key [{}]", key);
                                }

                                if (completeMessage) {
                                    callback.setMustCharge(true);
                                    callback.setMustRetry(false);
                                    SmileSMSC.getInstance().onNewAsyncResponseFromOffnet(callback);
                                }
                            } else {
                                callback.setMustCharge(true);
                                callback.setMustRetry(false);
                                SmileSMSC.getInstance().onNewAsyncResponseFromOffnet(callback);
                            }

                        } catch (Exception e) {
                            String msg = String.format("Exception caught while processing multipart - going to ignore this response: " + e.toString());
                            log.warn(msg, e);
                            BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "SMPP", msg);
                        }
                    }

                    @Override
                    public void fireUnexpectedPduResponseReceived(PduResponse pduResponse) {
                        log.debug("UnexpectedPduResponseReceived RemoteSMSC [{}] Response message: [{}]", session.getConfiguration(), pduResponse.getResultMessage());
                        String msg = String.format("Client SMS submission to remote SMSC received UnexpectedPduResponse. Partner [%s] result: [%s]", session.getConfiguration().getSystemId(), pduResponse.getResultMessage());
                        log.warn(msg);
                        BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "SMPP", msg);
                    }

                    @Override
                    public void fireUnrecoverablePduException(UnrecoverablePduException e) {
                        log.debug("UnrecoverablePduException RemoteSMSC [{}] Response message: [{}]", session.getConfiguration(), e.getMessage());

                        String msg = String.format("Client SMS submission to remote SMSC received UnrecoverablePduException. Partner [%s] result: [%s]", session.getConfiguration().getSystemId(), e.getMessage());
                        log.warn(msg);
                        BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "SMPP", msg);
                    }

                    @Override
                    public void fireRecoverablePduException(RecoverablePduException e) {
                        log.debug("RecoverablePduException RemoteSMSC [{}] Response message: [{}]", session.getConfiguration(), e.getMessage());

                        String msg = String.format("Client SMS submission to remote SMSC received RecoverablePduException. Partner [%s] result: [%s]", session.getConfiguration().getSystemId(), e.getMessage());
                        log.warn(msg);
                        BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "SMPP", msg);
                    }

                    @Override
                    public void fireUnknownThrowable(Throwable t) {
                        log.debug("UnknownThrowable RemoteSMSC [{}] Response message: [{}]", session.getConfiguration(), t.getMessage());

                        String msg = String.format("Client SMS submission to remote SMSC received UnknownThrowable. Partner [%s] result: [%s]", session.getConfiguration().getSystemId(), t.getMessage());
                        log.debug(msg);
                        //BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "SMPP", msg);
                    }

                    @Override
                    public void firePduRequestExpired(PduRequest pduRequest) {
                        log.debug("PduRequestExpired RemoteSMSC [{}] PDU request expired: [{}]", session.getConfiguration(), pduRequest);

                        String callbackId = session.getConfiguration().getSystemId() + "_" + pduRequest.getSequenceNumber();
                        OffnetBaseSm offnetBaseSm = new OffnetBaseSm((BaseSm) (pduRequest));
                        boolean multiPart = false;
                        try {
                            multiPart = offnetBaseSm.isMultipart();

                        } catch (Exception e) {
                            String msg = String.format("Exception caught while checking if response is for multipart - going to ignore this response: " + e.toString());
                            log.warn(msg);
                            BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "SMPP", msg);
                        }

                        if (multiPart) {
                            callbackId = session.getConfiguration().getSystemId() + "_MP_" + offnetBaseSm.getMessageId();
                        }

                        String msg = String.format("Client SMS submission to remote SMSC received PduRequestExpired. Partner [%s] request: [%s]", session.getConfiguration().getSystemId(), pduRequest);
                        log.debug(msg);
                        BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "SMPP", msg);

                        try {
                            FinalDeliveryPluginCallBack callback = new FinalDeliveryPluginCallBack();
                            callback.setCallbackId(callbackId);
                            callback.setMustRetry(true);
                            callback.setMustCharge(false);
                            callback.setPluginClassName(OffnetSMSDeliveryPlugin.class.getName());
                            SmileSMSC.getInstance().onNewAsyncResponseFromOffnet(callback);
                        } catch (Exception ex) {
                            log.warn("Exception processing UnexpectedPduResponseReceived: ", ex);
                        }
                    }
                }

                session.serverReady(
                        new SmppSessionHandler());
            }

            @Override
            public void sessionDestroyed(Long sessionId, SmppServerSession session) {
                log.debug("Session destroyed: [{}][{}]", session, session.getConfiguration().getSystemId());
                // print out final stats
                if (session.hasCounters()) {
                    log.debug(" final session rx-submitSM: [{}]", session.getCounters().getRxSubmitSM());
                }
                try {
                    RemoteSMSC remoteSMSC = RemoteSMSC.getRemoteSMSCBySystemId(session.getConfiguration().getSystemId());
                    if (remoteSMSC != null) {
                        if (remoteSMSC.getConfig().getClientBind() == 0) {
                            log.debug("remote SMSC bind has disconnected [{}]", remoteSMSC.getConfig().getName());
//                            BaseUtils.sendStatistic(BaseUtils.getHostNameFromKernel(), remoteSMSC.getConfig().getSystemId(), "isup", 0, "SMPP Endpoint Test");
                        }
                        remoteSMSC.closeAndDestroyServerSession(session, true);
                    }
                } catch (Exception e) {
                    log.warn("Exception while trying to remove remote SMSC on server tear down");
                }

                // make sure it's really shutdown
                session.destroy();
            }
        }

        smppServer = new DefaultSmppServer(serverConfiguration, new DefaultSmppServerHandler(), executor, monitorExecutor);

        log.warn("Starting Smile SMPP server...");
        smppServer.start();
        started = true;

        log.warn("Smile SMPP server started successfully!");
    }

    public String getHostConnectedToTrunk(String destinationTrunkId) {
        return DAO.getBindByTrunkId(SmileSMSC.getEMF(), destinationTrunkId);
    }

}
