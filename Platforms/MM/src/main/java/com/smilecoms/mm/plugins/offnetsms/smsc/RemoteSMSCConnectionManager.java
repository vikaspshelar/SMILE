/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.plugins.offnetsms.smsc;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class RemoteSMSCConnectionManager implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(RemoteSMSCConnectionManager.class);
    private static final int connectionTimer = BaseUtils.getIntSubProperty("env.mm.smsc.config", "connection_timer_millis");
    private boolean running;

    protected boolean isRunning() {
        return running;
    }

    @Override
    public void run() {
        log.warn("SMSCConnectionsThread is starting");
        running = true;

        try {
            log.debug("Clearing all old binds for this host");
            DAO.clearAllHostsBinds(SmileSMSC.getEMF());
        } catch (Exception e) {
            log.warn("Error clearing out all binds for this host:", e);
        }

        while (!SmileSMSC.getInstance().isStarted() && SmileSMSC.getInstance().shouldSMSCBeRunning()) {
            log.warn("Going to try and start Smile SMSC");
            try {
                SmileSMSC.getInstance().startSmppServer();
            } catch (Exception e) {
                log.warn("Error starting Smile SMSC. Will try again in 10s", e);
                Utils.sleep(10000);
            }
        }
        Utils.sleep(10000);
        while (SmileSMSC.getInstance().shouldSMSCBeRunning()) {
            try {
                for (InterconnectSMSC config : DAO.getAllRemoteSMSCs(SmileSMSC.getEMF())) {
                    if (!SmileSMSC.getInstance().shouldSMSCBeRunning()) {
                        log.debug("Smile SMSC is stopped. Breaking out");
                        break;
                    }

                    if (config.getClientBind() != 1) {
                        log.debug("Interconnect SMSC [{}] is setup not to bind as a client, nothing to do here except check if the bind is up for SOP isup reporting", config);
                        if (config.getEnabled() == 1) {
                            long bindsOnAny = DAO.getBindCountByTrunkIdOnAnyHost(SmileSMSC.getEMF(), config.getInternalTrunkId());
                            if (bindsOnAny == 0) {
                                log.debug("Remote client is enabled but there is no bind to any MM.... going to mark as down");
                                BaseUtils.sendStatistic(BaseUtils.getHostNameFromKernel(), config.getSystemId(), "isup", 0, "SMPP Endpoint Test");
                            } else {
                                BaseUtils.sendStatistic(BaseUtils.getHostNameFromKernel(), config.getSystemId(), "isup", 1, "SMPP Endpoint Test");
                            }
                        }
                        continue;
                    }

                    log.debug("SMSCConnectionsThread is looking at Remote SMSC with Config [{}] ", config);

                    if (!config.getHostMapping().isEmpty() && !Utils.matchesWithPatternCache(BaseUtils.getHostNameFromKernel(), config.getHostMapping())) {
                        log.debug("Interconnect SMSC is not for this host. [{}] does not match [{}]", BaseUtils.getHostNameFromKernel(), config.getHostMapping());
                        continue;
                    }

                    if (config.getSystemId().contains("|")) {
                        log.debug("This config has multiple systemIds - we check to see if one is already connected");
                        String[] splitSystemIds = config.getSystemId().split("\\|");
                        for (String systemId : splitSystemIds) {
                            if (!systemId.isEmpty() && RemoteSMSC.getRemoteSMSCBySystemId(systemId) != null) {
                                log.debug("This is an SMSC with multiple system-ids and one is connected [{}] - so we just test that one and ignore the others", systemId);
                                config.setSystemId(systemId);
                                break;
                            }
                        }
                    }

                    String[] splitSystemIds = config.getSystemId().split("\\|");

                    int numSystemIds = 1;
                    if (config.getSystemId().contains("|")) {
                        numSystemIds = splitSystemIds.length - 2; // because we delimit at the start and the end of the list (e.g. |a|b|c|d|) we will end up with length of 2 more - so we minus 2
                    }
                    int failedBinds = 0;

                    for (String systemId : splitSystemIds) {
                        if (systemId.isEmpty()) {
                            log.debug("SystemId empty so not going to try to bind");
                            continue;
                        }
                        log.debug("Trying to connect with system id [{}]", systemId);
                        config.setSystemId(systemId);

                        if (config.getEnabled() != 1) {
                            log.info("This SMSC [{}] is not enabled - we check if remoteSMSC exists - if it does we shut it down and remove from hashmap", config);
                            RemoteSMSC remoteSMSC = RemoteSMSC.getRemoteSMSCWithConfig(config);
                            if (remoteSMSC != null) {
                                //make sure we remove this remoteSMSC from the hashmap
                                RemoteSMSC.shutdownAndRemoveSMSCWithConfig(config);
                            }
                            continue;
                        }

                        try {
                            RemoteSMSC.getOrCreateRemoteSMSCWithConfig(config).testAndReconnectOnFailure();
                            log.debug("Successfully connected to system Id [{}]", systemId);
                            break;
                        } catch (Exception ex) {
                            log.warn("Error testing remote SMSC: [{}]", ex.toString());
                            log.warn("Error: ", ex);

                            //make sure we remove this remoteSMSC from the hashmap
                            log.debug("Check if remoteSMSC exists for this failure - if so we shut it down and remove it from the hashmap");
                            RemoteSMSC remoteSMSC = RemoteSMSC.getRemoteSMSCWithConfig(config);
                            if (remoteSMSC != null) {
                                //make sure we remove this remoteSMSC from the hashmap
                                RemoteSMSC.shutdownAndRemoveSMSCWithConfig(config);
                            }

                            //we only throw an exception if all systemIds fail
                            failedBinds++;
                            if (failedBinds == numSystemIds && BaseUtils.getBooleanProperty("env.mm.report.smpp.isup", true)) {
                                //send isup here if MM must report on peers
                                String msg = String.format("SMPP connection: [%s] is failing its availability test. It must be down. Location: [%s]", config.getSystemId(), config.getSystemId());
                                log.warn(msg);
                                BaseUtils.sendStatistic(BaseUtils.getHostNameFromKernel(), config.getSystemId(), "isup", 0, "SMPP Endpoint Test");
                                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, config.getSystemId(), msg, config.getSystemId());
                            }
                        }
                    }
                }
                int timesToSleep1Sec = connectionTimer / 1000;
                while (SmileSMSC.getInstance().shouldSMSCBeRunning() && timesToSleep1Sec > 0) {
                    timesToSleep1Sec--;
                    Thread.sleep(1000);
                }
            } catch (Exception ex) {
                log.warn("Error in SMSCConnectionsThread: [{}]", ex.toString());
                log.warn("Error: ", ex);
                if (SmileSMSC.getInstance().shouldSMSCBeRunning()) {
                    Utils.sleep(10000);
                }
            }

        } // End while should be running
        log.warn("SMSCConnectionsThread is exiting. Going to shut down all remote connections");
        for (InterconnectSMSC config : DAO.getAllRemoteSMSCs(SmileSMSC.getEMF())) {
            log.debug("SMSCConnectionsThread is stopping connection to Remote SMSC with Config [{}] ", config);
            try {
                RemoteSMSC.shutdownAndRemoveSMSCWithConfig(config);
            } catch (Exception ex) {
                log.warn("Error shutting down remote SMSC: [{}]", ex.toString());
                log.warn("Error: ", ex);
            }
        }

        running = false;
        log.warn("SMSCConnectionsThread has now exited after shutting down all remote connections");
    }
}
