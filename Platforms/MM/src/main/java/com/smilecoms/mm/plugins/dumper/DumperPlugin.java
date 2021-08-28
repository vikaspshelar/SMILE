/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.plugins.dumper;

import com.smilecoms.mm.plugins.offnetsms.*;
import com.smilecoms.commons.util.Codec;
import com.smilecoms.commons.util.IMAPUtils;
import com.smilecoms.mm.engine.*;
import com.smilecoms.mm.plugins.onnetsms.OnnetSMSMessage;
import com.smilecoms.mm.plugins.shortcodesms.ShortCodeSMSMessage;
import com.smilecoms.mm.utils.SMSCodec;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class DumperPlugin implements DeliveryPipelinePlugin {

    private static final Logger log = LoggerFactory.getLogger(DumperPlugin.class);

    @Override
    public DeliveryPluginResult processMessage(BaseMessage msg, DeliveryEngine callbackEngine) {
        // Lets return synchronously - no need for a callback ID
        FinalDeliveryPluginResult res = new FinalDeliveryPluginResult();
        res.setMustRetry(false);
        try {
            if (msg instanceof OffnetSMSMessage) {
                OffnetSMSMessage m = (OffnetSMSMessage) msg;
                log.error("DUMPER got a OffnetSMSMessage message [{}][{}][{}][{}][{}][{}][{}]ms", new Object[]{m.getFrom(), m.getTo(), SMSCodec.decode(m.getMessage(), m.getCodingScheme()),
                    Codec.binToHexString(m.getMessage()), m.getCodingScheme(), m.getDestinationTrunkId(), m.getMessageAge()});

                String msgEmail = "From: " + m.getFrom() + " To: " + m.getTo() + " Characterset: " + m.getCodingSchemeHex() + " Message Body as Hex: "
                        + Codec.binToHexString(m.getMessage()) + " Message Decoded: " + SMSCodec.decode(m.getMessage(), m.getCodingScheme());
                IMAPUtils.sendEmail("admin@smilecoms.com", "rwemalla@gmail.com", "New SMS Received at " + new Date(), msgEmail);
                IMAPUtils.sendEmail("admin@smilecoms.com", "pcb@smilecoms.com", "New SMS Received at " + new Date(), msgEmail);

            } else if (msg instanceof OnnetSMSMessage) {
                OnnetSMSMessage m = (OnnetSMSMessage) msg;
                log.error("DUMPER got a OnnetSMSMessage message [{}][{}][{}][{}][{}][{}][{}]ms", new Object[]{m.getFrom(), m.getTo(), SMSCodec.decode(m.getMessage(), m.getCodingScheme()),
                    Codec.binToHexString(m.getMessage()), m.getCodingScheme(), m.getSCSCFName(), m.getMessageAge()});
                
                String msgEmail = "From: " + m.getFrom() + " To: " + m.getTo() + " Characterset: " + m.getCodingSchemeHex() + " Message Body as Hex: "
                        + Codec.binToHexString(m.getMessage()) + " Message Decoded: " + SMSCodec.decode(m.getMessage(), m.getCodingScheme());
                IMAPUtils.sendEmail("admin@smilecoms.com", "rwemalla@gmail.com", "New SMS Received at " + new Date(), msgEmail);
                IMAPUtils.sendEmail("admin@smilecoms.com", "pcb@smilecoms.com", "New SMS Received at " + new Date(), msgEmail);
                
            } else if (msg instanceof ShortCodeSMSMessage) {
                ShortCodeSMSMessage m = (ShortCodeSMSMessage) msg;
                log.error("DUMPER got a ShortCodeSMSMessage message [{}][{}][{}][{}][{}][{}][{}]ms", new Object[]{m.getFrom(), m.getTo(), SMSCodec.decode(m.getMessage(), m.getCodingScheme()),
                    Codec.binToHexString(m.getMessage()), m.getCodingScheme(), m.getSCSCFName(), m.getMessageAge()});
            }
        } catch (Exception e) {
            log.warn("Error sending offnet message: [{}]", e.toString());
            res.setRetryTrigger(new RetryTrigger());
            res.getRetryTrigger().setTriggerType(RetryTrigger.SIP_REGISTER);
            res.getRetryTrigger().setTriggerKey(((OffnetSMSMessage) msg).getTo());
            res.setMustRetry(true);
        }
        return res;
    }

    @Override
    public void shutDown() {
    }

    @Override
    public void initialise(EntityManagerFactory emf) {
    }

    @Override
    public void propertiesChanged() {
    }

    @Override
    public void sendDeliveryReport(DeliveryEngine.DeliveryReportStatus reportStatus, HashMap<String, Serializable> deliveryReportData) {
        log.debug("In sendDeliveryReport");
    }

}
