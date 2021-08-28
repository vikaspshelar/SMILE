/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.plugins.offnetsms;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.ExceptionManager;
import com.smilecoms.mm.BinaryShortMessage;
import com.smilecoms.mm.MessageManager;
import com.smilecoms.mm.engine.*;
import com.smilecoms.mm.plugins.offnetsms.smsc.InterconnectSMSC;
import com.smilecoms.mm.plugins.offnetsms.smsc.NoRouteToSMSCException;
import com.smilecoms.mm.plugins.offnetsms.smsc.PermanentDeliveryFailure;
import com.smilecoms.mm.plugins.offnetsms.smsc.SmileSMSC;
import com.smilecoms.mm.plugins.offnetsms.smsc.TemporaryDeliveryFailure;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import javax.persistence.EntityManagerFactory;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class OffnetSMSDeliveryPlugin implements DeliveryPipelinePlugin {

    private static final Logger log = LoggerFactory.getLogger(OffnetSMSDeliveryPlugin.class);
    private static EntityManagerFactory emf;

    @Override
    public DeliveryPluginResult processMessage(BaseMessage msg, DeliveryEngine callbackEngine) {
        OffnetSMSMessage offnetMsg = (OffnetSMSMessage) msg;
        String messageId = null;
        try {
            messageId = SmileSMSC.getInstance().sendMessageOffnet(offnetMsg);
            if (log.isDebugEnabled()) {
                log.debug ("Message ID returned from send function [{}]", messageId);
            }
        } catch (NoRouteToSMSCException nr) {
            FinalDeliveryPluginResult res = new FinalDeliveryPluginResult();
            String hostConnectedToTrunk = SmileSMSC.getInstance().getHostConnectedToTrunk(offnetMsg.getDestinationTrunkId());
            if (hostConnectedToTrunk == null) {
                res.setMustRetry(true);
                return res;
            } else {
                throw new RerouteException(hostConnectedToTrunk);
            }
        } catch (PermanentDeliveryFailure pdf) {
            log.debug("Permanent failure - not retrying nor charging");
            FinalDeliveryPluginResult res = new FinalDeliveryPluginResult();
            res.setMustRetry(false);
            res.setMustCharge(false);
            res.setPermanentDeliveryFailure(true);
            return res;
        } catch (TemporaryDeliveryFailure tdf) {
            log.debug("Temporary failure - retrying");
            FinalDeliveryPluginResult res = new FinalDeliveryPluginResult();
            res.setMustRetry(true);
            return res;
        } catch (Exception e) {
            log.warn("Unknown error sending offnet message - will retry: ", e);
            new ExceptionManager(log).reportError(e);
            FinalDeliveryPluginResult res = new FinalDeliveryPluginResult();
            res.setMustRetry(true);
            return res;
        }
        
        if (messageId == null || messageId.isEmpty()) {
            log.warn("No messageId returned  -will retry: ");
            FinalDeliveryPluginResult res = new FinalDeliveryPluginResult();
            res.setMustRetry(true);
            return res;
        }

        InitialDeliveryPluginResult res = new InitialDeliveryPluginResult();
        res.setCallBackId(messageId);
        return res;

    }

    @Override
    public void shutDown() {
        log.warn("In shutdown for OffnetSMSDeliveryPlugin");
        try {
            SmileSMSC.getInstance().stop();
        } catch (Exception e) {
            log.warn("Error stopping SMSC", e);
        }
    }

    @Override
    public void initialise(EntityManagerFactory emf) {
        log.debug("Initialising OffnetSMSDeliveryPlugin with EMF [{}]", emf.toString());
        OffnetSMSDeliveryPlugin.emf = emf;
        try {
            SmileSMSC.boot(emf);
        } catch (Exception e) {
            log.warn("Error starting SMSC", e);
        }
        log.debug("Finished initialising OffnetSMSDeliveryPlugin");
    }

    @Override
    public void propertiesChanged() {
        try {
            SmileSMSC.getInstance().reloadConfiguration();
        } catch (Exception e) {
            log.warn("Error reloading SMSC config", e);
        }

    }
    
    /**
     * Called by SmileSMSC when a message comes in from another SMSC
     *
     * @param fromAddress
     * @param toAddress
     * @param deliveryReportData
     * @param message
     * @param dataCodingScheme
     * @param configOfReceivingSMSC
     * @param clientExpiry
     * @throws Exception
     */
    public static void onNewMessageFromOffnet(String fromAddress, String toAddress, HashMap<String, Serializable> deliveryReportData, byte[] message, byte dataCodingScheme, InterconnectSMSC configOfReceivingSMSC, Date clientExpiry) throws Exception {
        log.debug("In OffnetSMSDeliveryPlugin.onNewMessageFromOffnet");
        BinaryShortMessage m = new BinaryShortMessage();
        m.setDestination(toAddress);
        m.setSource(fromAddress);
        m.setDataAsBinary(message);
        m.setDataCodingScheme(dataCodingScheme);
        m.setPriority(BaseMessage.Priority.MEDIUM);
        m.setExpiryDate(clientExpiry);
        m.setDeliveryReportHandle(new DeliveryReportHandle(OffnetSMSDeliveryPlugin.class.getName(), deliveryReportData));

        // This is a message we got from an offnet UE so we are processing the terminating leg
        @SuppressWarnings("UseInjectionInsteadOfInstantion")
        MessageManager mm = new MessageManager();
        log.debug("Sending SMS message to MM via local call to sendShortMessageInternal");

        String legInfo = "T";
        if (BaseUtils.getBooleanProperty("env.mm.enable.smsc.leg.overide", true)) {
            String overridingLegInfo = configOfReceivingSMSC.getPropertyFromConfig("overridingLegInfo");
            if (overridingLegInfo != null && !overridingLegInfo.isEmpty()) {
                legInfo = overridingLegInfo;
            }
        }

        mm.sendShortMessageInternal(emf, m, legInfo, configOfReceivingSMSC.getInternalTrunkId());
        log.debug("Successfully sent SMS message to MM via call to sendShortMessageInternal");
        log.debug("Finished OffnetSMSDeliveryPlugin.onNewMessageFromOffnet");
    }

    @Override
    public void sendDeliveryReport(DeliveryEngine.DeliveryReportStatus reportStatus, HashMap<String, Serializable> deliveryReportData) {
        log.debug("In sendDeliveryReport");
        try {
            SmileSMSC.getInstance().sendDeliveryReport(reportStatus, deliveryReportData);
        } catch (NoRouteToSMSCException nr) {
            String hostConnectedToTrunk = SmileSMSC.getInstance().getHostConnectedToTrunk((String) deliveryReportData.get("TrunkId"));
            if (hostConnectedToTrunk == null) {
                log.warn("There is no SMSC connected on trunk [{}] so we cannot send the delivery report", deliveryReportData.get("TrunkId"));
            } else {
                throw new RerouteException(hostConnectedToTrunk);
            }
        } catch (Exception e) {
            log.warn("Unknown error sending delivery report: ", e);
            new ExceptionManager(log).reportError(e);
        }
    }

}
