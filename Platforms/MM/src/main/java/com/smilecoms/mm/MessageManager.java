package com.smilecoms.mm;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.props.PropertyFetchException;
import com.smilecoms.commons.localisation.LocalisationHelper;
import com.smilecoms.commons.localisation.PDFUtils;
import com.smilecoms.commons.platform.SmileWebService;

import com.smilecoms.commons.sca.SCAWrapper;

import com.smilecoms.commons.sca.direct.bm.ChargingRequest;
import com.smilecoms.commons.sca.direct.bm.ChargingResult;
import com.smilecoms.commons.sca.direct.bm.RatingKey;
import com.smilecoms.commons.sca.direct.bm.RequestedServiceUnit;
import com.smilecoms.commons.sca.direct.bm.ServiceInstanceIdentifier;
import com.smilecoms.commons.sca.direct.bm.UsedServiceUnit;
import com.smilecoms.commons.util.ExceptionManager;
import com.smilecoms.commons.util.IMAPUtils;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.mm.db.interconnect.op.InMemoryRouter;
import com.smilecoms.mm.db.interconnect.op.RoutingInfo;
import com.smilecoms.mm.db.model.Sms;
import com.smilecoms.mm.engine.BaseMessage.Priority;
import com.smilecoms.mm.engine.DeliveryEngine;
import com.smilecoms.mm.engine.EngineQueue;
import com.smilecoms.mm.plugins.offnetsms.OffnetSMSMessage;
import com.smilecoms.mm.plugins.onnetsms.OnnetSMSMessage;
import com.smilecoms.mm.plugins.shortcodesms.ShortCodeSMSMessage;
import com.smilecoms.mm.silverpop.Silverpop;
import com.smilecoms.xml.mm.MMError;
import com.smilecoms.xml.mm.MMSoap;
import com.smilecoms.xml.schema.mm.CampaignEngineRequest;
import com.smilecoms.xml.schema.mm.Done;
import com.smilecoms.xml.schema.mm.Email;
import com.smilecoms.xml.schema.mm.EngineMessage;
import com.smilecoms.xml.schema.mm.MessageId;
import com.smilecoms.xml.schema.mm.ShortMessage;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.ejb.Stateless;
import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;

/**
 * PCB TODO
 */
@WebService(serviceName = "MM", portName = "MMSoap", endpointInterface = "com.smilecoms.xml.mm.MMSoap", targetNamespace = "http://xml.smilecoms.com/MM", wsdlLocation = "MMServiceDefinition.wsdl")
@Stateless
@HandlerChain(file = "/handler.xml")
public class MessageManager extends SmileWebService implements MMSoap {

    @javax.annotation.Resource
    javax.xml.ws.WebServiceContext wsctx;
    @PersistenceContext(unitName = "MMPU")
    private EntityManager em;
    private static final String DATA_CODING_ASCII = "ascii";
    private static final SimpleDateFormat sdfLong = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

    @Override
    public Done isUp(String str) throws MMError {
        if (!BaseUtils.isPropsAvailable()) {
            throw createError(MMError.class, "Properties are not available so this platform will be reported as down");
        }
        try {
            if (DeliveryEngine.getInstance().getMemoryQueueSize() > EngineQueue.getMaxAllowedMemoryMessages()) {
                throw createError(MMError.class, "Delivery Engine Queue is too full so reporting MM as down -- limit is " + EngineQueue.getMaxAllowedMemoryMessages());
            }
        } catch (MMError mm) {
            throw mm;
        } catch (PropertyFetchException e) {
            log.debug("Property framework is not available yet but I'll report as being up");
        } catch (Exception ex) {
            throw createError(MMError.class, ex.toString());
        }
        return makeDone();
    }

    /**
     * Utility method to make a complex boolean object with value TRUE.
     *
     * @return The resulting complex object
     */
    private com.smilecoms.xml.schema.mm.Done makeDone() {
        com.smilecoms.xml.schema.mm.Done done = new com.smilecoms.xml.schema.mm.Done();
        done.setDone(com.smilecoms.xml.schema.mm.StDone.TRUE);
        return done;
    }

    private String decodeMessagePart(String message, String dataCodingSchema) {
        if (dataCodingSchema.equals(DATA_CODING_ASCII)) {
            return message;
        }
        return "";
    }

    @Override
    public Done resendMessage(MessageId messageResendData) throws MMError {
        setContext(messageResendData, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        try {
            DeliveryEngine.getInstance().onRetryTriggerFiring(messageResendData.getMessageId());
        } catch (Exception e) {
            throw processError(MMError.class, e);
        }
        if (log.isDebugEnabled()) {
            logEnd();
        }

        return makeDone();
    }

    @Override
    public Done sendEmail(Email email) throws MMError {
        setContext(email, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        try {
            Locale loc;
            if (email.getLanguage() != null && email.getLanguage().length() > 0) {
                loc = LocalisationHelper.getLocaleForLanguage(email.getLanguage());
            } else {
                loc = LocalisationHelper.getDefaultLocale();
            }
            String subject;
            if (email.getSubjectParameters() != null && !email.getSubjectParameters().isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("Printing email subject parameters:");
                    int i = 0;
                    for (String p : email.getSubjectParameters()) {
                        log.debug("Parameter [{}] is [{}]", i, p);
                        i++;
                    }
                }
                subject = LocalisationHelper.getLocalisedStringAllowingDuplicatePlaceholders(loc, email.getSubjectResourceName(), email.getSubjectParameters().toArray());
            } else {
                subject = LocalisationHelper.getLocalisedString(loc, email.getSubjectResourceName());
            }
            String body;
            byte[] attachment = null;

            if (email.getBodyXML() != null && !email.getBodyXML().isEmpty()) {
                String xslt = LocalisationHelper.getLocalisedString(loc, email.getBodyResourceName());
                log.debug("EMail uses xslt to generate the body. XML: [{}], XSLT: [{}]", email.getBodyXML(), xslt);
                body = Utils.doXSLTransform(email.getBodyXML(), xslt, getClass().getClassLoader());
            } else if (email.getBodyParameters() != null && !email.getBodyParameters().isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("Printing email body parameters:");
                    int i = 0;
                    for (String p : email.getBodyParameters()) {
                        log.debug("Parameter [{}] is [{}]", i, p);
                        i++;
                    }
                }
                body = LocalisationHelper.getLocalisedStringAllowingDuplicatePlaceholders(loc, email.getBodyResourceName(), email.getBodyParameters().toArray());
            } else {
                body = LocalisationHelper.getLocalisedString(loc, email.getBodyResourceName());
            }
            log.debug("Email body is: [{}]", body);

            if (email.getAttachmentResourceName() != null
                    && email.getAttachmentFileName() != null
                    && !email.getAttachmentResourceName().isEmpty()
                    && !email.getAttachmentFileName().isEmpty()) {

                log.debug("The email must include a PDF generated from XHTML with file name [{}]", email.getAttachmentFileName());
                attachment = PDFUtils.generateLocalisedPDF(email.getAttachmentResourceName(), email.getAttachmentXML(), loc, getClass().getClassLoader());
                if (email.getAttachmentStorageLocation() != null && !email.getAttachmentStorageLocation().isEmpty()) {
                    log.debug("The attachment must be stored on the file system for possible later use or printing. Location: [{}]", email.getAttachmentStorageLocation());
                    Utils.writeStreamToDisk(email.getAttachmentFileName(), new ByteArrayInputStream(attachment), email.getAttachmentStorageLocation());
                    log.debug("Finished writing attachment to disk");
                }
            } else if (email.getAttachmentBase64() != null && !email.getAttachmentBase64().isEmpty()) {
                log.debug("Attachment is base64 encoded binary");
                attachment = Utils.decodeBase64(email.getAttachmentBase64());
            }

            try {
                IMAPUtils.sendEmail(email.getFromAddress(), email.getToAddress(), email.getCCAddress(), email.getBCCAddress(), subject, body, email.getAttachmentFileName(), attachment);
            } catch (javax.mail.MessagingException me) {
                log.warn("Error sending mail. Trying again.", me);
                IMAPUtils.sendEmail(email.getFromAddress(), email.getToAddress(), email.getCCAddress(), email.getBCCAddress(), subject, body, email.getAttachmentFileName(), attachment);
            }
        } catch (Exception e) {
            throw processError(MMError.class, e);
        }
        if (log.isDebugEnabled()) {
            logEnd();
        }
        return makeDone();
    }

    @Override
    public Done submitToEngine(EngineMessage engineMessage) throws MMError {
        setContext(engineMessage, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }

        try {
            byte[] msg = Utils.decodeBase64(engineMessage.getSerialisedMessageAsBase64());
            DeliveryEngine.getInstance().enqueueMessage(msg);
        } catch (Exception e) {
            throw processError(MMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public Done sendShortMessage(ShortMessage newShortMessage) throws MMError {
        setContext(newShortMessage, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }

        try {
            if (newShortMessage.getDestination() == null || newShortMessage.getDestination().isEmpty()) {
                log.debug("Destination is null or empty");
                throw new Exception("Short message destination is null or empty -- From " + newShortMessage.getSource() + " To " + newShortMessage.getDestination() + " Msg " + newShortMessage.getDataAsString());
            }
            BinaryShortMessage message = new BinaryShortMessage(newShortMessage);

            if (message.getDataAsBinary().length == 0) {
                throw new Exception("Cannot send an SMS with zero length");
            }

            // Now the data in the message will only be in binary form
            // Anything coming into the webservice would be originating on Smile I.e. trunk Id 0
            sendShortMessageInternal(null, message, "O", "0", false);

        } catch (Exception e) {
            throw processError(MMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    private static final Map<String, Object[]> pendingMessageSubmissions = new ConcurrentHashMap<>();

    public String sendShortMessageInternal(EntityManagerFactory emf, BinaryShortMessage newShortMessage, String leg, String incomingTrunkId) throws InsufficientFundsException, Exception {
        return sendShortMessageInternal(emf, newShortMessage, leg, incomingTrunkId, false);
    }

    public String sendShortMessageInternal(EntityManagerFactory emf, BinaryShortMessage newShortMessage,
            String leg, String incomingTrunkId, boolean submitOnCallback) throws InsufficientFundsException, Exception {
        try {
            log.debug("In MessageManager.sendShortMessageInternal");
            // timestamp to see the message age at any point
            long millisAtStart = System.currentTimeMillis();
            if (em == null) {
                // If called not via web service then we need own em
                em = JPAUtils.getEM(emf);
                JPAUtils.beginTransaction(em);
            }

            String messageId = Utils.getUUID() + "_" + sdfLong.format(new Date()) + "_SMS";

            RoutingInfo routingInfo;
            long routingStart = System.currentTimeMillis();
            routingInfo = InMemoryRouter.getRoutingInfo(em, newShortMessage.getSource(), newShortMessage.getDestination(), leg, "SMS", incomingTrunkId);
            if (log.isDebugEnabled()) {
                log.debug("Message router took [{}]ms", System.currentTimeMillis() - routingStart);
            }

            if (routingInfo.getFromTrunkId().equals("0") && routingInfo.getToTrunkId().startsWith("NO_IC_MSG")) {
                try {
                    log.debug("No interconnect set up yet");
                    ShortMessage sm = new ShortMessage();
                    sm.setDataAsString("The SMS sent to " + Utils.getFriendlyPhoneNumber(newShortMessage.getDestination()) + "  could not be delivered. We'll let u know when their service becomes available. You were not charged for this SMS. #NowYouCan");
                    sm.setDestination(newShortMessage.getSource());
                    sm.setSource(BaseUtils.getProperty("env.mm.noic.msg.from", "343"));
                    sendShortMessage(sm);
                } catch (Exception e) {
                    log.warn("Error sending no interconnect sms", e);
                }
                return null;
            }

            Set<String> systemSources = BaseUtils.getPropertyAsSet("env.mm.skip.charging.sources");
            Set<String> systemDestinations = BaseUtils.getPropertyAsSet("env.mm.skip.charging.destinations");
            boolean skipCharging;

            if ((systemSources != null
                    && systemSources.contains(newShortMessage.getSource()))
                    || (systemDestinations != null
                    && systemDestinations.contains(newShortMessage.getDestination()))) {
                log.debug("In MessageManager.sendShortMessageInternal. Not Reserving funds for the message as its skipped as per env.mm.skip.charging.sources or env.mm.skip.charging.destinations");
                skipCharging = true;

            } else {

                log.debug("In MessageManager.sendShortMessageInternal. Reserving funds for the message");
                ChargingRequest cr = new ChargingRequest();
                cr.setRetrial(false);
                com.smilecoms.commons.sca.direct.bm.ChargingData cd = new com.smilecoms.commons.sca.direct.bm.ChargingData();
                cd.setServiceInstanceIdentifier(new ServiceInstanceIdentifier());
                // Terminating leg must have the service info of the destination
                cd.getServiceInstanceIdentifier().setIdentifierType("END_USER_SIP_URI");
                if (leg.equals("O")) {
                    cd.getServiceInstanceIdentifier().setIdentifier(newShortMessage.getSource());
                } else {
                    cd.getServiceInstanceIdentifier().setIdentifier(newShortMessage.getDestination());
                }

                if (BaseUtils.getBooleanProperty("env.mm.transit.enabled", true)) {
                    if (!(routingInfo.getFromTrunkId().equals("0") || routingInfo.getToTrunkId().equals("0"))) {
                        log.debug("Neither incoming or outgoing trunk equals 0 (SMILE) so this must be transit");
                        String transitServiceInstanceIdentifier = BaseUtils.getProperty("env.mm.transit.service.identifier");

                        if (transitServiceInstanceIdentifier != null && !transitServiceInstanceIdentifier.isEmpty()) {
                            cd.getServiceInstanceIdentifier().setIdentifier(transitServiceInstanceIdentifier);
                            String subProperty = routingInfo.getFromTrunkId() + "=";

                            if (transitServiceInstanceIdentifier.contains(subProperty)) {
                                String fromTrunkSIIdentifier = BaseUtils.getSubProperty("env.mm.transit.service.identifier", routingInfo.getFromTrunkId());
                                log.debug("Setting SI identifer for trunk [{}] to [{}]", routingInfo.getFromTrunkId(), fromTrunkSIIdentifier);
                                cd.getServiceInstanceIdentifier().setIdentifier(fromTrunkSIIdentifier);
                            }
                        }
                    }
                }

                cd.setDescription("SMS");
                cd.setUserEquipment("");
                cd.setRequestedServiceUnit(new RequestedServiceUnit());
                cd.getRequestedServiceUnit().setUnitQuantity(getQuantity(newShortMessage));
                cd.getRequestedServiceUnit().setUnitType("SMS");
                cd.getRequestedServiceUnit().setReservationSecs(BaseUtils.getIntProperty("env.mm.sms.reservation.secs", 864000));
                cd.getRequestedServiceUnit().setTriggerCharged(true);
                cd.setRatingKey(new RatingKey());
                cd.getRatingKey().setServiceCode("SMS");
                cd.getRatingKey().setRatingGroup("0");
                cd.getRatingKey().setIncomingTrunk(routingInfo.getFromTrunkId());
                cd.getRatingKey().setOutgoingTrunk(routingInfo.getToTrunkId());
                cd.getRatingKey().setLeg(leg);
                cd.getRatingKey().setTo(newShortMessage.getDestination());
                cd.getRatingKey().setFrom(newShortMessage.getSource());
                cd.setSessionId(messageId);
                cd.setEventTimestamp(Utils.getDateAsXMLGregorianCalendar(new Date()));
                cd.setChargingDataIndex(0);
                cr.getChargingData().add(cd);

                log.info("Calling BM for reservation with session Id [{}]", cd.getSessionId());
                ChargingResult cres = SCAWrapper.getAdminInstance().rateAndBill_Direct(cr);
                log.info("Called BM for reservation with session Id [{}]", cd.getSessionId());
                
                if (cres.getGrantedServiceUnits().get(0).getUnitQuantity() == null
                        || cres.getGrantedServiceUnits().get(0).getUnitQuantity().intValue() <= 0) {
                    throw new InsufficientFundsException(cd.getRatingKey().getFrom(), cd.getRatingKey().getTo(), cd.getRatingKey().getIncomingTrunk(), cd.getRatingKey().getOutgoingTrunk(), cd.getRatingKey().getLeg());
                }
                messageId = cd.getSessionId();
                skipCharging = false;
            }

            if (submitOnCallback) {
                pendingMessageSubmissions.put(messageId, new Object[]{newShortMessage, messageId, routingInfo.getToTrunkId(), millisAtStart, skipCharging, routingInfo.getMessageClass(), System.currentTimeMillis()});
                return messageId;
            }
            
            enqueueShortMessage(em, newShortMessage, messageId, routingInfo.getToTrunkId(), millisAtStart, skipCharging, routingInfo.getMessageClass());

        } finally {
            if (emf != null) {
                JPAUtils.commitTransactionAndClose(em);
            }
        }
        log.debug("Finished MessageManager.sendShortMessageInternal");
        return null;
    }

    public void discardPendingMessage(String messageId) throws Exception {
        log.debug("Discarding message id [{}] now that its no longer pending", messageId);
        pendingMessageSubmissions.remove(messageId);
    }

    public void enqueuePendingMessage(EntityManagerFactory emf, String messageId) throws Exception {
        log.debug("Enqueueing message id [{}] now that its no longer pending", messageId);
        Object[] msgData = pendingMessageSubmissions.get(messageId);
        if (msgData == null) {
            throw new Exception("Unknown pending message -- " + messageId);
        }

        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            enqueueShortMessage(em, (BinaryShortMessage) msgData[0], (String) msgData[1], (String) msgData[2], (Long) msgData[3], (Boolean) msgData[4], (String) msgData[5]);
        } finally {
            JPAUtils.commitTransactionAndClose(em);
        }

        if (pendingMessageSubmissions.size() > 100) {
            log.debug("Pruning map as its size is [{}]", pendingMessageSubmissions.size());
            try {
                for (Entry<String, Object[]> entry : pendingMessageSubmissions.entrySet()) {
                    String key = entry.getKey();
                    Object[] msg = entry.getValue();
                    if (msg != null) {
                        long startMillis = (long) msg[6];
                        if (startMillis < System.currentTimeMillis() - 30000) {
                            log.warn("Pruning msg [{}]", key);
                            pendingMessageSubmissions.remove(key);
                            log.debug("We must not charge for this message");
                            ChargingRequest cr = new ChargingRequest();
                            com.smilecoms.commons.sca.direct.bm.ChargingData cd = new com.smilecoms.commons.sca.direct.bm.ChargingData();
                            cd.setSessionId(messageId);
                            cd.setUsedServiceUnit(new UsedServiceUnit());
                            // Zero units indicates just clear out the reservation
                            cd.getUsedServiceUnit().setUnitQuantity(BigDecimal.ZERO);
                            cr.getChargingData().add(cd);
                            log.debug("Calling BM to release the reservation");
                            SCAWrapper.getAdminInstance().rateAndBill_Direct(cr);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Error garbage collecting pending message map", e);
            }
        }

        log.debug("Enqueued pending message id [{}] now that its no longer pending", messageId);
    }

    private void enqueueShortMessage(EntityManager em, BinaryShortMessage newShortMessage, String messageId, String outgoingTrunk, long millisAtStart, boolean skipCharging, String messageClass) throws Exception {
        log.debug("In enqueueShortMessage");
        
        storeShortMessage(em, newShortMessage, messageId);

        // Get message type
        log.debug("Need to work out the message type Destination: [{}] Outgoing trunk: [{}]", newShortMessage.getDestination(), outgoingTrunk);

        if (messageClass != null) {
            switch (messageClass) {
                case "ShortCodeSMSMessage":
                    
                    enqueueShortCodeMessage(newShortMessage, messageId, outgoingTrunk, millisAtStart, skipCharging, messageClass);
                    break;
                case "OnnetSMSMessage":
                    enqueueOnnetMessage(newShortMessage, messageId, outgoingTrunk, millisAtStart, skipCharging, messageClass);
                    break;
                case "OffnetSMSMessage":
                    enqueueOffnetMessage(newShortMessage, messageId, outgoingTrunk, millisAtStart, skipCharging, messageClass);
                    break;
                default:
                    throw new Exception("Unknown message class -- " + messageClass);
            }
        } else if (outgoingTrunk.equals("0")) {
            enqueueOnnetMessage(newShortMessage, messageId, outgoingTrunk, millisAtStart, skipCharging, messageClass);
        } else {
            enqueueOffnetMessage(newShortMessage, messageId, outgoingTrunk, millisAtStart, skipCharging, messageClass);
        }
        log.debug("Finished enqueueShortMessage");
    }

    private void enqueueShortCodeMessage(BinaryShortMessage newShortMessage, String messageId, String outgoingTrunk, long millisAtStart, boolean skipCharging, String messageClass) throws Exception {
        ShortCodeSMSMessage msg = new ShortCodeSMSMessage(messageId, millisAtStart);
        msg.setSkipCharging(skipCharging);
        msg.setFrom(newShortMessage.getSource());
        msg.setTo(newShortMessage.getDestination());
        msg.setMessage(newShortMessage.getDataAsBinary());
        msg.setCodingScheme(newShortMessage.getDataCodingScheme());
        msg.setExpiryDate(newShortMessage.getExpiryDate());
        msg.setDeliveryReportHandle(newShortMessage.getDeliveryReportHandle());
        if (newShortMessage.getCampaignId() != null && !newShortMessage.getCampaignId().isEmpty()) {
            msg.setCampaignId(newShortMessage.getCampaignId());
        }
        // Make shortcodes go quickly
        msg.setPriority(Priority.HIGH);
         
        log.debug("This is a shortcode SMS [{}] to [{}]", msg, newShortMessage.getDestination());
        DeliveryEngine.getInstance().enqueueMessage(msg);
    }

    private void enqueueOnnetMessage(BinaryShortMessage newShortMessage, String messageId, String outgoingTrunk, long millisAtStart, boolean skipCharging, String messageClass) throws Exception {
        // Message destination is onnet
        OnnetSMSMessage msg = new OnnetSMSMessage(messageId, millisAtStart);
        msg.setSkipCharging(skipCharging);
        msg.setFrom(newShortMessage.getSource());
        msg.setTo(newShortMessage.getDestination());
        msg.setMessage(newShortMessage.getDataAsBinary());
        msg.setCodingScheme(newShortMessage.getDataCodingScheme());
        msg.setPriority(newShortMessage.getPriority());
        msg.setExpiryDate(newShortMessage.getExpiryDate());
        msg.setDeliveryReportHandle(newShortMessage.getDeliveryReportHandle());
        if (newShortMessage.getCampaignId() != null && !newShortMessage.getCampaignId().isEmpty()) {
            msg.setCampaignId(newShortMessage.getCampaignId());
        }
        
        log.debug("This is a SMS going onnet [{}]", msg);
        DeliveryEngine.getInstance().enqueueMessage(msg);
    }

    private void enqueueOffnetMessage(BinaryShortMessage newShortMessage, String messageId, String outgoingTrunk, long millisAtStart, boolean skipCharging, String messageClass) throws Exception {
        OffnetSMSMessage msg = new OffnetSMSMessage(messageId, millisAtStart);
        msg.setSkipCharging(skipCharging);
        msg.setFrom(newShortMessage.getSource());
        msg.setTo(newShortMessage.getDestination());
        msg.setMessage(newShortMessage.getDataAsBinary());
        msg.setCodingScheme(newShortMessage.getDataCodingScheme());
        msg.setPriority(newShortMessage.getPriority());
        msg.setDestinationTrunkId(outgoingTrunk);
        msg.setExpiryDate(newShortMessage.getExpiryDate());
        msg.setDeliveryReportHandle(newShortMessage.getDeliveryReportHandle());
        if (newShortMessage.getCampaignId() != null && !newShortMessage.getCampaignId().isEmpty()) {
            msg.setCampaignId(newShortMessage.getCampaignId());
        }
        log.debug("This is a SMS going offnet [{}]", msg);
        DeliveryEngine.getInstance().enqueueMessage(msg);
    }

    private void storeShortMessage(EntityManager em, BinaryShortMessage newShortMessage, String messageId) throws Exception {
        
        try {
            int ccLength = BaseUtils.getProperty("env.e164.country.code").length();
            int maxShortCodeLength = BaseUtils.getIntProperty("env.mm.shortcode.max.digits", 5) + ccLength;
            String fromShort = Utils.getFriendlyPhoneNumberKeepingCountryCode(newShortMessage.getSource());

            if (!(fromShort.length() > maxShortCodeLength) && !BaseUtils.getBooleanProperty("env.mm.store.short.code.sms", false)) {
                log.debug("Message is from a shortcode so no need to store");
                return;
            }
            log.debug("Storing SMS for compliance reasons");
            Sms sms = new Sms();
            sms.setDataCodingScheme(newShortMessage.getDataCodingScheme());
            sms.setData(newShortMessage.getDataAsBinary());
            sms.setDateTime(new Date());
            sms.setSource(newShortMessage.getSource());
            sms.setDestination(newShortMessage.getDestination());
            sms.setMessageId(messageId);
            sms.setStatus("PROCESSING");
            if (newShortMessage.getCampaignId() != null && !newShortMessage.getCampaignId().isEmpty()) {
                sms.setCampaignId(newShortMessage.getCampaignId());
            }

            em.persist(sms);
            em.flush();
            log.debug("Finished storing SMS for compliance reasons");
        } catch (Exception e) {
            new ExceptionManager(log).reportError(e);
        }
    }

    private BigDecimal getQuantity(BinaryShortMessage newShortMessage) {
        if (!BaseUtils.getBooleanProperty("env.mm.charge.sms.based.on.size", false)) {
            return BigDecimal.ONE;
        }
        int multipartSplitSize = BaseUtils.getIntProperty("env.mm.sms.multipart.size", 160);
        /*we split for charging based on the full length of the message including the UDHs, if any */
        int splitSize = BaseUtils.getIntProperty("env.mm.max.sms.size.bytes", 160);
        // For 8 bit encoding the max length is 140 characters
        int eightBitChars = newShortMessage.getDataAsBinary().length;

        if (eightBitChars > splitSize) {
            splitSize = multipartSplitSize;
        }

        int remainder = eightBitChars % splitSize;
        int parts = eightBitChars / splitSize;
        if (remainder != 0 || parts == 0) {
            parts++;
        }
        log.debug("Message quantity is [{}]", parts);
        return new BigDecimal(parts);
    }

    @Override
    public Done submitToCampaignEngine(CampaignEngineRequest campaignEngineRequest) throws MMError {
        setContext(campaignEngineRequest, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }

        try {

            new Silverpop().sendTemplateEmail(
                    campaignEngineRequest.getEmailAddress(),
                    campaignEngineRequest.getCampaignId(),
                    campaignEngineRequest.getPersonalisations(),
                    Utils.decodeBase64(campaignEngineRequest.getAttachmentAsBase64()),
                    campaignEngineRequest.getAttachmentName(),
                    campaignEngineRequest.getDirectPersonalisationXML());

        } catch (Exception e) {
            throw processError(MMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    /*
     *  These are the short message bot functions used in Javassist code 
     *  
     */
    public boolean staffLookup(String from, String to, String msg, com.smilecoms.mm.plugins.shortcodesms.ShortCodeActionHelper helper, org.slf4j.Logger log) {

        msg = msg.toLowerCase().trim();
        java.util.List l = com.smilecoms.commons.base.BaseUtils.getPropertyFromSQLWithoutCache("env.staff.numbers");
        String res = "";
        if (msg.isEmpty()) {
            res = "Please send the name of the staff member you are searching for";
            helper.sendSMS(to, from, res);
            return true;
        }

        boolean isStaff = false;
        for (int i = 0; i < l.size(); i++) {
            String[] row = (String[]) l.get(i);
            String name = row[0];
            String number = row[1];
            if (name.toLowerCase().contains(msg)) {
                res = res + name + " " + number + "\r\n";
            }
            if (from.contains(number)) {
                isStaff = true;
            }
        }
        res = res.trim();
        if (res.isEmpty()) {
            res = "No results found";
        }
        if (!isStaff) {
            res = "Service is only available to staff";
        }
        if (msg.length() < 3) {
            res = "Search message must be at least 3 characters";
        }

        helper.sendSMS(to, from, res);
        return true;
    }

    public boolean accountBot(String from, String to, String msg, com.smilecoms.mm.plugins.shortcodesms.ShortCodeActionHelper helper, org.slf4j.Logger log) {        
        int maxAccounts = 15;
        String country = "UG";
        //String country = "TZ";
        //String country = "NG";

        com.smilecoms.commons.sca.Customer cust = helper.getCustomerByPhoneNumber(from);
        StringBuilder txt = new StringBuilder();

        if (cust == null) {
            txt.append("Error checking balance. Please send from a Smile registered number.  Smile #NowYouCan");
            //txt.append("Error checking balance. Use the mobile number we have in our records. #NowYouCan");            
            helper.sendSMS(to, from, txt.toString());
            return true;
        }

        java.util.List accountIds = new java.util.ArrayList();

        /*
        Requests for balance can be:
        All - requests all accounts of the customer associated with the from number
        $ACOUNT_NUMBER - specific account of the customer associated with the from number
        Any other test - request for the account number associated with this number
         */
        boolean requestForAccountOnThisNumber = false;

        msg = msg.toLowerCase().trim();
        if (msg.startsWith("on")) {            
            helper.sendSMS(to, from, "Post call notifications are now on. Reply with 'off' to turn off");
            helper.setSIpURIUsersOptInLevelBit(from, 2, 1);
            return true;
        } else if (msg.startsWith("off")) {            
            helper.sendSMS(to, from, "Post call notifications are now off, Reply with 'on' to turn on");
            helper.setSIpURIUsersOptInLevelBit(from, 2, 0);
            return true;
        }

        /*
        If request is for specific account of the customer associated with the from number
         */
        if (msg.length() == 10) {
            long reqAccId = -1;
            boolean accountFormat = true;
            try {
                reqAccId = java.lang.Long.parseLong(msg);
            } catch (java.lang.NumberFormatException e) {
                accountFormat = false;
            }
            if (accountFormat) {
                cust = helper.getCustomerByPhoneNumber(from);
                boolean allowedAccount = helper.isAccountOwnedByCustomer(cust, reqAccId);

                if (!allowedAccount) {
                    txt.append("Error checking balance for account ");
                    txt.append(reqAccId);
                    txt.append(". Please send an account number that is registered to you");                    
                    helper.sendSMS(to, from, txt.toString());
                    return true;
                }
                String tmp = "" + reqAccId;
                accountIds.add(tmp);
            }
        }

        //This means they came from offnet so we don't know what account they want so we send them all  FOR UG
        if (accountIds.isEmpty() && country.equals("UG") && to.contains("720000131")) {
            msg = "all";
        }

        if (msg.startsWith("all")) {
            /*
        if request is for all accounts of the customer associated with the from number
             */
            cust = helper.getCustomerByPhoneNumber(from);
            accountIds = helper.getAccountIdsByCustomer(cust);
        }

        /*
        If request is for the account number associated with this number
         */
        if (accountIds.isEmpty()) {
            requestForAccountOnThisNumber = true;
            String tmp = "" + helper.getAccountId(from);
            accountIds.add(tmp);
        }

        for (int c = 0; c < accountIds.size(); c++) {
            if (c >= maxAccounts) {
                txt.append("You have more accounts but balance enquiry is limited to ");
                txt.append(maxAccounts);
                txt.append(" accounts");
                break;
            }

            long accountId;
            try {
                accountId = java.lang.Long.parseLong((String) accountIds.get(c));
            } catch (java.lang.NumberFormatException e) {
                txt.append("Error checking balance for account ");
                txt.append(accountIds.get(c));
                txt.append(" - do you own this account?");                
                helper.sendSMS(to, from, txt.toString());
                return true;
            }

            com.smilecoms.commons.sca.Account acc = helper.getAccount(accountId);

            String friendlyName = null;
            if (!requestForAccountOnThisNumber) {
                friendlyName = helper.getFriendlyNameIfAccountHasOnlyOnePI(cust, accountId);
            }

            if (requestForAccountOnThisNumber) {
                txt.append("Your balance is ");
            } else {
                txt.append("The balance on account ");
                if (friendlyName == null) {
                    txt.append(accountId);
                } else {
                    txt.append("\"");
                    txt.append(friendlyName);
                    txt.append("\" (");
                    txt.append(accountId);
                    txt.append(")");
                }

                txt.append(" is ");
            }

            double cash = acc.getAvailableBalanceInCents();
            if (cash > 0) {
                if (country.equals("UG") || country.equals("TZ")) {
                    txt.append(com.smilecoms.commons.util.Utils.convertCentsToCurrencyLong(acc.getAvailableBalanceInCents()));
                } else if (country.equals("NG")) {
                    txt.append(com.smilecoms.commons.util.Utils.convertCentsToSpecifiedCurrencyLongRoundHalfEven("N", acc.getAvailableBalanceInCents()));
                }
            }

            double bundleBalance = 0.00d;
            double voiceBalance = 0.00d;
            int smsBalance = 0;
            boolean hasUnlimitedBundle = false;
            for (int i = 0; i < acc.getUnitCreditInstances().size(); i = i + 1) {
                com.smilecoms.commons.sca.UnitCreditInstance uci = (com.smilecoms.commons.sca.UnitCreditInstance) acc.getUnitCreditInstances().get(i);
                com.smilecoms.commons.sca.UnitCreditSpecification ucs = com.smilecoms.commons.sca.beans.NonUserSpecificCachedDataHelper.getUnitCreditSpecification(uci.getUnitCreditSpecificationId().intValue());
                String displayBal = com.smilecoms.commons.util.Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "DisplayBalance");
                if (ucs.getUnitType().equalsIgnoreCase("byte")
                        && (displayBal == null || displayBal.equals("true"))
                        && com.smilecoms.commons.util.Utils.getJavaDate(uci.getStartDate()).before(new java.util.Date())
                        && uci.getAvailableUnitsRemaining().doubleValue() > 0.0d) {
                    bundleBalance = bundleBalance + uci.getAvailableUnitsRemaining().doubleValue();
                } else if (ucs.getUnitType().equalsIgnoreCase("second")
                        && (displayBal == null || displayBal.equals("true"))
                        && com.smilecoms.commons.util.Utils.getJavaDate(uci.getStartDate()).before(new java.util.Date())
                        && uci.getAvailableUnitsRemaining().doubleValue() > 0.0d) {
                    voiceBalance = voiceBalance + uci.getAvailableUnitsRemaining().doubleValue();
                } else if (ucs.getUnitType().equalsIgnoreCase("SMS")
                        && (displayBal == null || displayBal.equals("true"))
                        && com.smilecoms.commons.util.Utils.getJavaDate(uci.getStartDate()).before(new java.util.Date())
                        && uci.getAvailableUnitsRemaining().doubleValue() > 0.0d) {
                    smsBalance = smsBalance + uci.getAvailableUnitsRemaining().intValue();
                }
                if (ucs.getConfiguration().contains("Unlimited=true")) {
                    hasUnlimitedBundle = true;
                }

            }
            if (cash > 0) {
                if (voiceBalance == 0 && smsBalance == 0) {
                    txt.append(" and ");
                } else {
                    txt.append(", ");
                }
            }

            txt.append(com.smilecoms.commons.util.Utils.displayVolumeAsString(bundleBalance, "byte"));

            if (country.equalsIgnoreCase("UG")) {
                String s = ". Your Smile account is" + acc.getAccountId() + " and Voice number " + from;
                txt.append(s);
            }

            if (hasUnlimitedBundle) {
                txt.append(" (+ SmileUnlimited)");
            }

            if (voiceBalance > 0) {
                if (smsBalance == 0) {
                    txt.append(" and ");
                } else {
                    txt.append(", ");
                }
                txt.append(com.smilecoms.commons.util.Utils.displayVolumeAsString(voiceBalance, "second"));
            }

            if (smsBalance > 0) {
                txt.append(" and ");
                txt.append(smsBalance);
                txt.append(" SMS.  ");
            }

            //if (countxttry.equals("UG") && to.contains("720000131")) {            
            helper.sendSMS(to, from, txt.toString());            
            txt.setLength(0);

            //} else if (accountIds.size() > 1 && c < (accountIds.size() - 1)) {
            //    txt.append("\n\n");
            //}
        }
        if (!txt.toString().isEmpty()) {            
            helper.sendSMS(to, from, txt.toString());
        }

        return true;
    }

    public boolean kycBot(String from, String to, String msg, com.smilecoms.mm.plugins.shortcodesms.ShortCodeActionHelper helper, org.slf4j.Logger log) {

        com.smilecoms.commons.sca.Customer cust = helper.getCustomerWithAvpsByPhoneNumber(from);
        boolean kycVerified = false;
        String successText = "You are fully registered. Thank you for choosing Smile.";
        String failureText = "You are not fully registered. Visit any Smile Outlet with your ID to register as soon as possible to avoid being blocked.";

        //String country = "TZ";
//        String country="NG";
        String country = "UG";

        //NG
        if (country.equalsIgnoreCase("NG")) {
            kycVerified = cust.getKYCStatus().equalsIgnoreCase("V");
            successText = "You are fully registered. Thank you for choosing Smile.";
            failureText = "You are not fully registered. Visit any Smile Outlet with your ID to register as soon as possible to avoid being blocked.";
        } //UG
        else if (country.equalsIgnoreCase("UG")) {
            successText = "You are fully registered. Thank you for choosing Smile.";
            failureText = "We do not recognise this number. Please use your Smile number or contact Customer Care on +256 (0) 720 100100.";
            java.util.List piList = cust.getProductInstances();
            int productId = -1;
            int simsInTD = 0;
            int simsInAC = 0;

            // find the right product on this customer for this from number
            for (int r = 0; r < piList.size(); r++) {
                java.util.List psimList = ((com.smilecoms.commons.sca.ProductInstance) piList.get(r)).getProductServiceInstanceMappings();
                for (int s = 0; s < psimList.size(); s++) {
                    java.util.List avpList = ((com.smilecoms.commons.sca.ProductServiceInstanceMapping) psimList.get(s)).getServiceInstance().getAVPs();
                    String simStatus = ((com.smilecoms.commons.sca.ProductServiceInstanceMapping) psimList.get(s)).getServiceInstance().getStatus();
                    for (int e = 0; e < avpList.size(); e++) {
                        if (((com.smilecoms.commons.sca.AVP) avpList.get(e)).getAttribute().equalsIgnoreCase("PublicIdentity") && ((com.smilecoms.commons.sca.AVP) avpList.get(e)).getValue().contains(from)) {
                            productId = r;
                            break;
                        }

                    }
                    if (simStatus.equalsIgnoreCase("TD")) {
                        ++simsInTD;
                    }

                    if (simStatus.equalsIgnoreCase("AC")) {
                        ++simsInAC;
                    }
                }
            }
            //find the SIM service with KYC status on it in this customer
            if (productId >= 0) {
                java.util.List psimList = ((com.smilecoms.commons.sca.ProductInstance) piList.get(productId)).getProductServiceInstanceMappings();
                for (int s = 0; s < psimList.size(); s++) {
                    java.util.List avpList = ((com.smilecoms.commons.sca.ProductServiceInstanceMapping) psimList.get(s)).getServiceInstance().getAVPs();
                    for (int e = 0; e < avpList.size(); e++) {
                        if (((com.smilecoms.commons.sca.AVP) avpList.get(e)).getAttribute().equalsIgnoreCase("KYCStatus") && ((com.smilecoms.commons.sca.AVP) avpList.get(e)).getValue().equalsIgnoreCase("Complete")) {
                            kycVerified = true;
                            break;
                        }
                    }
                }
            }

            // B. Where all SIMs are TD, respond with following SMS
            if (simsInTD > 0 && simsInAC == 0) {

            }

            // C. Where some SIMs have Provision Status AC, and others are TD, respond with following SMS and only list the SIMs with AC Provisional Status
            if (simsInTD > 0 && simsInAC > 0) {

            }
            // D. Where all SIMs are Provision Status AC, respond with following SMS and list all SIMs

            if (simsInTD == 0 && simsInAC > 0) {

            }

        } //TZ
        else if (country.equalsIgnoreCase("TZ")) {
            successText = "Dear Customer, your Smile SIM card is registered under " + cust.getFirstName() + " " + cust.getLastName() + ". If incorrect please visit a Smile outlet to ensure compliance with SIM registration";
            failureText = "Dear Customer, your Smile SIM registration is incomplete. Visit any Smile shop with a valid passport, driving licence, voting card or national ID to finalise.";

            java.util.List piList = cust.getProductInstances();
            int productId = -1;
            //find the right product on this customer for this from number
            for (int r = 0; r < piList.size(); r++) {
                java.util.List psimList = ((com.smilecoms.commons.sca.ProductInstance) piList.get(r)).getProductServiceInstanceMappings();
                for (int s = 0; s < psimList.size(); s++) {
                    java.util.List avpList = ((com.smilecoms.commons.sca.ProductServiceInstanceMapping) psimList.get(s)).getServiceInstance().getAVPs();
                    for (int e = 0; e < avpList.size(); e++) {
                        if (((com.smilecoms.commons.sca.AVP) avpList.get(e)).getAttribute().equalsIgnoreCase("PublicIdentity") && ((com.smilecoms.commons.sca.AVP) avpList.get(e)).getValue().contains(from)) {
                            productId = r;
                            break;
                        }
                    }
                }
            }
            //find the SIM service with KYC status on it in this customer
            if (productId >= 0) {
                java.util.List psimList = ((com.smilecoms.commons.sca.ProductInstance) piList.get(productId)).getProductServiceInstanceMappings();
                for (int s = 0; s < psimList.size(); s++) {
                    java.util.List avpList = ((com.smilecoms.commons.sca.ProductServiceInstanceMapping) psimList.get(s)).getServiceInstance().getAVPs();
                    for (int e = 0; e < avpList.size(); e++) {
                        if (((com.smilecoms.commons.sca.AVP) avpList.get(e)).getAttribute().equalsIgnoreCase("KYCStatus") && ((com.smilecoms.commons.sca.AVP) avpList.get(e)).getValue().equalsIgnoreCase("Complete")) {
                            kycVerified = true;
                            break;
                        }
                    }
                }
            }
        }

        if (kycVerified) {
            helper.sendSMS(to, from, successText);
        } else {
            helper.sendSMS(to, from, failureText);
        }
        return true;
    }

    public boolean UgKycBotTake2(String from, String to, String msg, com.smilecoms.mm.plugins.shortcodesms.ShortCodeActionHelper helper, org.slf4j.Logger log) {

        com.smilecoms.commons.sca.Customer cust = helper.getCustomerWithAvpsByPhoneNumber(from);

        String unrecognisedNumberText = "We do not recognise this number. Please use your Smile number or contact Customer Care on +256 (0) 720 100100.";
        if (cust == null) {
            helper.sendSMS(to, from, unrecognisedNumberText);
            return true;
        }

        String fn = cust.getFirstName();
        String incompleteText = "Hi " + fn + ", you are not fully registered. Please SMS your NIN to this number so we can update your profile";
        if (msg.length() != 14 && cust.getNationality().equals("UG") && (!cust.getIdentityNumberType().equals("nationalid") || cust.getIdentityNumber().length() != 14)) {
            helper.sendSMS(to, from, incompleteText);
            return true;
        }
        if (msg.length() == 14 && cust.getNationality().equals("UG") && cust.getIdentityNumber().length() != 14) {
            cust.setIdentityNumber(msg);
            cust.setIdentityNumberType("nationalid");
            com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().modifyCustomer(cust);
            helper.sendSMS(to, from, "Hi " + fn + ", thank you for providing your NIN. As per Uganda Communications Commission regulations, we need to verify and take a photo of your Identification card for our records. Please visit a Smile outlet with your ID card to avoid being disconnected");
            return true;
        }

        java.util.List piList = cust.getProductInstances();
        //run through all products and SIMs

        int unvalidatedCnt = 0;
        int incompleteCnt = 0;
        int completeCnt = 0;
        int incompleteNoNIN = 0;
        int completePendingNINVer = 0;
        int incompleteNINNotFound = 0;

        for (int r = 0; r < piList.size(); r++) {
            java.util.List psimList = ((com.smilecoms.commons.sca.ProductInstance) piList.get(r)).getProductServiceInstanceMappings();
            for (int s = 0; s < psimList.size(); s++) {
                java.util.List avpList = ((com.smilecoms.commons.sca.ProductServiceInstanceMapping) psimList.get(s)).getServiceInstance().getAVPs();
                for (int e = 0; e < avpList.size(); e++) {
                    if (((com.smilecoms.commons.sca.AVP) avpList.get(e)).getAttribute().equals("KYCStatus")) {
                        String val = ((com.smilecoms.commons.sca.AVP) avpList.get(e)).getValue();

                        if (val.equals("Incomplete - NIN not found")) {
                            incompleteNINNotFound++;
                        } else if (val.equals("Incomplete - NIN not done")) {
                            incompleteNoNIN++;
                        } else if (val.equals("Unvalidated")) {
                            unvalidatedCnt++;
                        } else if (val.equals("Complete")) {
                            completeCnt++;
                        } else if (val.equals("Incomplete")) {
                            incompleteCnt++;
                        } else if (val.equals("Complete, pending NIN verification")) {
                            completePendingNINVer++;
                        }
                        break;
                    }
                }
            }
        }

        if (completeCnt > 0 && unvalidatedCnt == 0 && incompleteCnt == 0 && incompleteNoNIN == 0 && completePendingNINVer == 0 && incompleteNINNotFound == 0) {
            helper.sendSMS(to, from, "Hi " + fn + ", you are fully registered. Thank you for choosing Smile.");
            return true;
        }
        if (!cust.getNationality().equals("UG")) {
            helper.sendSMS(to, from, "Hi " + fn + ", our records show you are not a Ugandan national and we need to verify your passport. Please visit a Smile outlet with your passport to avoid being disconnected");
        } else if (incompleteNINNotFound > 0) {
            helper.sendSMS(to, from, "Dear " + fn + ", Your NIN " + cust.getIdentityNumber() + " has been rejected by NIRA and your service is now disconnected. Please contact NIRA to resolve this issue and we restore your service. Apologies for the inconvenience. Smile");
        } else {
            helper.sendSMS(to, from, "Hi " + fn + ", according to our records your NIN is " + cust.getIdentityNumber() + ". As per Uganda Communications Commission regulations, we need to verify and take a photo of your Identification card for our records. Please visit a Smile outlet with your ID card to avoid being disconnected");
        }
        return true;
    }

    public boolean UgKycBotTake3(String from, String to, String msg, com.smilecoms.mm.plugins.shortcodesms.ShortCodeActionHelper helper, org.slf4j.Logger log) {

        if (from != null && to != null) {
            if (to.equalsIgnoreCase(from) || to.equals("sip:+256720000131@ug.smilecoms.com")) {
                return true;
            }
        }

        com.smilecoms.commons.sca.Customer cust = helper.getCustomerWithAvpsByPhoneNumber(from);

        String unrecognisedNumberText = "We do not recognise this number. Please use your Smile number or contact Customer Care on +256 (0) 720 100100.";
        if (cust == null) {
            helper.sendSMS(to, from, unrecognisedNumberText);
            return true;
        }

        String fn = cust.getFirstName();
        String incompleteText = "Hi " + fn + ", you are not fully registered. Please SMS your NIN to this number so we can update your profile";
        if (msg.length() != 14 && cust.getNationality().equals("UG") && (!cust.getIdentityNumberType().equals("nationalid") || cust.getIdentityNumber().length() != 14)) {
            helper.sendSMS(to, from, incompleteText);
            return true;
        }
        if (msg.length() == 14 && cust.getNationality().equals("UG") && cust.getIdentityNumber().length() != 14) {
            cust.setIdentityNumber(msg);
            cust.setIdentityNumberType("nationalid");
            com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().modifyCustomer(cust);
            helper.sendSMS(to, from, "Hi " + fn + ", thank you for providing your NIN. As per Uganda Communications Commission regulations, we need to verify and take a photo of your Identification card for our records. Please visit a Smile outlet with your ID card to avoid being disconnected");
            return true;
        }

        java.util.List piList = cust.getProductInstances();
        //run through all products and SIMs

        int unvalidatedCnt = 0;
        int incompleteCnt = 0;
        int completeCnt = 0;
        int incompleteNoNIN = 0;
        int completePendingNINVer = 0;
        int incompleteNINNotFound = 0;
        int simsInTD = 0;
        int simsInAC = 0;
        String acSIMs = "";

        for (int r = 0; r < piList.size(); r++) {
            java.util.List psimList = ((com.smilecoms.commons.sca.ProductInstance) piList.get(r)).getProductServiceInstanceMappings();
            String iccid = "";
            String strAccountId = "";
            String phoneNumber = "";

            //Look for telephone number on this product;
            for (int s = 0; s < psimList.size(); s++) {
                com.smilecoms.commons.sca.ServiceInstance currentServiceInstance = ((com.smilecoms.commons.sca.ProductServiceInstanceMapping) psimList.get(s)).getServiceInstance();
                java.util.List avpList = currentServiceInstance.getAVPs();

                for (int e = 0; e < avpList.size(); e++) {
                    if (((com.smilecoms.commons.sca.AVP) avpList.get(e)).getAttribute().equals("PublicIdentity")) {
                        phoneNumber = com.smilecoms.commons.util.Utils.getFriendlyPhoneNumber(((com.smilecoms.commons.sca.AVP) avpList.get(e)).getValue());
                    }
                }
            }

            for (int s = 0; s < psimList.size(); s++) {
                com.smilecoms.commons.sca.ServiceInstance currentServiceInstance = ((com.smilecoms.commons.sca.ProductServiceInstanceMapping) psimList.get(s)).getServiceInstance();
                java.util.List avpList = currentServiceInstance.getAVPs();

                if (currentServiceInstance.getServiceSpecificationId() == 1) { // For SIMs Only
                    strAccountId = "" + currentServiceInstance.getAccountId();
                    iccid = "";

                    for (int e = 0; e < avpList.size(); e++) {

                        if (((com.smilecoms.commons.sca.AVP) avpList.get(e)).getAttribute().equals("KYCStatus")) {
                            String val = ((com.smilecoms.commons.sca.AVP) avpList.get(e)).getValue();

                            if (val.equals("Incomplete - NIN not found")) {
                                incompleteNINNotFound++;
                            } else if (val.equals("Incomplete - NIN not done")) {
                                incompleteNoNIN++;
                            } else if (val.equals("Unvalidated")) {
                                unvalidatedCnt++;
                            } else if (val.equals("Complete")) {
                                completeCnt++;
                            } else if (val.equals("Incomplete")) {
                                incompleteCnt++;
                            } else if (val.equals("Complete, pending NIN verification")) {
                                completePendingNINVer++;
                            }
                            break;
                        }

                        if (((com.smilecoms.commons.sca.AVP) avpList.get(e)).getAttribute().equals("IntegratedCircuitCardIdentifier")) {
                            iccid = ((com.smilecoms.commons.sca.AVP) avpList.get(e)).getValue();
                        }
                    }

                    if (((com.smilecoms.commons.sca.ProductServiceInstanceMapping) psimList.get(s)).getServiceInstance().getStatus().equalsIgnoreCase("TD")) {
                        ++simsInTD;
                    }

                    if (((com.smilecoms.commons.sca.ProductServiceInstanceMapping) psimList.get(s)).getServiceInstance().getStatus().equalsIgnoreCase("AC")) {
                        ++simsInAC;
                        // 4907 (a/c 1809786443) 0720908999
                        acSIMs = acSIMs + "-" + iccid.substring(iccid.length() - 4)
                                + " (a/c " + strAccountId + ") " + phoneNumber + "\n";
                    }
                }
            }
        }

        // 1) Where all SIMs are TD, respond with following SMS: 
        if (simsInTD > 0 && simsInAC == 0) {
            helper.sendSMS(to, from, "Dear " + fn + ", you are not fully registered. Please contact Smile on 0720 100 100. Thank you");
            return true;
        }

        Locale locale = com.smilecoms.commons.localisation.LocalisationHelper.getLocaleForLanguage(cust.getLanguage());

        String identityType = com.smilecoms.commons.localisation.LocalisationHelper.getLocalisedString(locale, "document.type." + cust.getIdentityNumberType());
        // 2) Where some SIMs have Provision Status AC, and others are TD, respond with following SMS and only list the SIMs with AC Provisional Status
        if (simsInTD > 0 && simsInAC > 0) {
            helper.sendSMS(to, from, "Dear " + fn + ", your " + identityType + " is registered with SIM card(s) ending with:\n"
                    + acSIMs + "Thank you");
            return true;
        }

        if (completeCnt > 0 && unvalidatedCnt == 0 && incompleteCnt == 0 && incompleteNoNIN == 0 && completePendingNINVer == 0 && incompleteNINNotFound == 0
                && simsInAC > 0) {
            helper.sendSMS(to, from, "Dear " + fn + ", your " + identityType + " is registered with SIM card(s) ending with:\n"
                    + acSIMs + "Thank you");
            return true;
        }

        if (!cust.getNationality().equals("UG")) {
            helper.sendSMS(to, from, "Hi " + fn + ", our records show you are not a Ugandan national and we need to verify your passport. Please visit a Smile outlet with your passport to avoid being disconnected");
        } else if (incompleteNINNotFound > 0) {
            helper.sendSMS(to, from, "Dear " + fn + ", Your " + identityType + " has been rejected by NIRA and your service is now disconnected. Please contact NIRA to resolve this issue and we restore your service. Apologies for the inconvenience. Smile");
        } else {
            helper.sendSMS(to, from, "Hi " + fn + ", according to our records your NIN is " + cust.getIdentityNumber() + ". As per Uganda Communications Commission regulations, we need to verify and take a photo of your Identification card for our records. Please visit a Smile outlet with your ID card to avoid being disconnected");
        }
        return true;
    }

    public boolean redeemVoucherBot(String from, String to, String msg, com.smilecoms.mm.plugins.shortcodesms.ShortCodeActionHelper helper, org.slf4j.Logger log) {

        log.debug("Entering voucherRedemptionBot");
        String text = null;
        long accountId = 0;
        String voucherPin = null;

        String country = "NG";
        //String country = "TZ";
        //String country = "UG";

        // Retrieve service associated with the source number.
        com.smilecoms.commons.sca.ServiceInstance si = helper.getServiceInstance(from);

        text = "We do not recognise this number. Please use your Smile number or contact Customer Care on +256 (0) 720 100100.";
        if (si == null) {
            helper.sendSMS(to, from, text);
            return true;
        }

        accountId = si.getAccountId();

        voucherPin = msg;

        // Have the list of bundles ready since we do not know which path the user will choose to follow.
        int availableBundlesLimit = 5;
        StringBuilder strBufBundles = new StringBuilder();
        java.util.Map applicableUcsSpecs = new java.util.HashMap();
        java.util.Map applicableUcsNames = new java.util.HashMap();
        java.util.Map allUCSNames = new java.util.HashMap(); // Keep them for future reference.
        com.smilecoms.commons.sca.beans.CatalogBean unitCreditCatalog = com.smilecoms.commons.sca.beans.CatalogBean.getUnitCreditCatalog();
        java.util.List unitCreditSpecifications = unitCreditCatalog.getUnitCreditSpecifications();

        for (int a = 0; a < unitCreditSpecifications.size(); a++) {
            com.smilecoms.commons.sca.beans.UnitCreditSpecificationBean ucs = (com.smilecoms.commons.sca.beans.UnitCreditSpecificationBean) unitCreditSpecifications.get(a);
            allUCSNames.put("" + ucs.getUnitCreditSpecificationId(), ucs.getName());
        }

        for (int a = 0; (a < unitCreditSpecifications.size()) && (applicableUcsSpecs.size() < availableBundlesLimit); a++) {
            com.smilecoms.commons.sca.beans.UnitCreditSpecificationBean ucs = (com.smilecoms.commons.sca.beans.UnitCreditSpecificationBean) unitCreditSpecifications.get(a);
            if (ucs.getConfiguration().contains("TPGW=true") && ucs.getPurchaseRoles().contains("Customer")) {
                applicableUcsSpecs.put("" + a, "" + ucs.getUnitCreditSpecificationId());
                applicableUcsNames.put("" + a, ucs.getName());
                strBufBundles.append("Send " + a);
                strBufBundles.append(" for ");
                strBufBundles.append(ucs.getName());
                strBufBundles.append(" at N");
                int tmp = (int) (ucs.getPriceInCents() / 100);
                strBufBundles.append(tmp);
                strBufBundles.append("\n");
            }
        }

        // Check to see who is suppose to receive the voucher.
        // Retrieve the voucher strip here.
        // com.smilecoms.commons.sca.beans.AccountBean accountBean = new com.smilecoms.commons.sca.beans.AccountBean();
        StringBuilder txt = new StringBuilder();
        boolean isRedeemedByThirdParty = false;
        String replyTo = null;
        long thirdPartyAccountId = 0;
        com.smilecoms.commons.sca.PrepaidStrip voucher = null;
        // Try to redeem the strip;
        if (msg != null && !msg.isEmpty()) { // && com.smilecoms.commons.util.Utils.isNumeric(msg)) {

            if (msg.trim().equalsIgnoreCase("BAL")) { //This is a balance check 

                com.smilecoms.commons.sca.Account acc = helper.getAccount(accountId);

                String friendlyName = null;

                txt.append("The balance on account " + accountId);
                txt.append(" is ");

                double cash = acc.getAvailableBalanceInCents();
                if (country.equals("UG") || country.equals("TZ")) {
                    txt.append(com.smilecoms.commons.util.Utils.convertCentsToCurrencyLong(acc.getAvailableBalanceInCents()));
                } else if (country.equals("NG")) {
                    txt.append(com.smilecoms.commons.util.Utils.convertCentsToSpecifiedCurrencyLongRoundHalfEven("N", acc.getAvailableBalanceInCents()));
                }

                txt.append("\n" + strBufBundles);
                txt.append(". Smile");

                helper.sendSMS(to, from, txt.toString());
                txt.setLength(0);

                if (!txt.toString().isEmpty()) {
                    helper.sendSMS(to, from, txt.toString());
                }
                return true;
            }

            //Do voucher logic
            String[] parameters = msg.split("\\*");
            if (parameters.length >= 2) {
                voucherPin = parameters[0];
                // Check is parameters[1] is account number or telephone number?
                // com.smilecoms.commons.sca.Account accToRecharge = helper.getAccount(Long.parseLong(parameters[1])); //To detect is cusstomer gave wrong account id

                // Use the number of the recipient to send reply.
                String sql = "select product_instance_id from service_instance where account_id=" + parameters[1] + " limit 1;";
                java.util.List productInstances = helper.runSQL(sql);

                int productInstanceID = 0;

                accountId = java.lang.Long.parseLong(parameters[1]);

                try {
                    //If product not found by account id, try seach by phone number ...
                    if (productInstances == null || productInstances.size() <= 0) {
                        String recipientSipUri = Utils.getPublicIdentityForPhoneNumber(parameters[1]);
                        log.error("Searching of product instance by account id failed, will use the public identity: " + recipientSipUri);
                        // Search by phone number
                        com.smilecoms.commons.sca.ServiceInstance recipientSi = helper.getServiceInstance(recipientSipUri);
                        if (recipientSi != null) {
                            accountId = recipientSi.getAccountId();
                            replyTo = recipientSipUri;
                        } else {
                            log.error("Searching of product instance by phone number [{}] failed, source number is [{}] ", recipientSipUri, from);
                        }
                    } else {
                        productInstanceID = ((java.lang.Integer) productInstances.get(0)).intValue();
                        replyTo = com.smilecoms.commons.sca.beans.ProductBean.getProductInstanceById(productInstanceID).getProductPhoneNumber();
                    }

                } catch (java.lang.Exception ex) {
                    log.error("Error while trying to get recipient account id " + ex.getMessage());
                    productInstanceID = 0;
                    replyTo = from; // Send reply messages to the sender?
                }
                // replyTo =  com.smilecoms.commons.sca.beans.ProductBean.getProductInstanceById(((com.smilecoms.commons.sca.ServiceInstance) accToRecharge.getServiceInstances().get(0)).getProductInstanceId()).getProductPhoneNumber();
                /* if(productInstanceID == 0)  {
                    text = "You have provided an incorrect recipient account number " + parameters[1] + ". Please try again or call 0702 044 4444, FREE from your Smile number, for assistance 24/7.";
                    helper.sendSMS(to, from, text);
                    return true;
                } 
                
                replyTo = com.smilecoms.commons.sca.beans.ProductBean.getProductInstanceById(productInstanceID).getProductPhoneNumber();
                 */
                log.error("Recipient account id is " + accountId);
                thirdPartyAccountId = si.getAccountId();  // Account id for service that submitted the request. The voucher lock will operate onn this account if any error              
                isRedeemedByThirdParty = true;
            } else {
                voucherPin = msg;
                isRedeemedByThirdParty = false;
            }

            if (voucherPin.trim().length() == 15) { // Check the length later - && msg.trim().length() == 15
                // This is a PIN for recharge
                try {
                    com.smilecoms.commons.sca.VoucherLockForAccount vlA = null;
                    try {
                        if (isRedeemedByThirdParty) {
                            vlA = com.smilecoms.commons.sca.beans.AccountBean.getVoucherLockForAccount(thirdPartyAccountId);
                        } else {
                            vlA = com.smilecoms.commons.sca.beans.AccountBean.getVoucherLockForAccount(accountId);
                        }
                    } catch (Exception ex) {
                        //Not found
                        vlA = null;
                    }
                    // First check if the account is locked for recharding
                    if (vlA != null && (vlA.getAccountAttempts() > 1 || vlA.getAttempts() > 3)) {//1+ attempt
                        text = "This request cannot be processed as this account is temporarily locked. Please call 0702 044 4444, FREE from your Smile number for assistance.";
                        helper.sendSMS(to, from, text);
                        return true;
                    }

                    com.smilecoms.commons.sca.beans.AccountBean accBean = com.smilecoms.commons.sca.beans.AccountBean.redeemVoucher(thirdPartyAccountId, accountId, voucherPin);

                    // All is fine proceed to retrieve the voucher strip - to get more details for messaging.
                    voucher = com.smilecoms.commons.sca.beans.AccountBean.getVoucher(voucherPin, accountId);

                    // Check voucher type
                    StringBuilder strBuf = new StringBuilder();
                    if (voucher.getValueInCents() > 0) { // This is an airtime voucher
                        // TODO: Extract the bundles here...
                        //getting all applicable unit credits available to TPGW and with customer role

                        if (isRedeemedByThirdParty) {

                            strBuf.append("You have received N" + (voucher.getValueInCents() / 100)
                                    + " Airtime credit on your Smile Account " + accountId + " from "
                                    + com.smilecoms.commons.util.Utils.getFriendlyPhoneNumber(from) + ". Your Account balance is N" + (accBean.getAvailableBalanceInCents() / 100)
                                    + ". To convert airtime to data, reply with the exact name of the data plan. Choose from:\n");

                            strBuf.append(strBufBundles);
                            strBuf.append("Smile");
                            helper.sendSMS(to, replyTo, strBuf.toString());
                            text = "Account " + accountId + "has been credited with N" + (voucher.getValueInCents() / 100) + " Airtime. Thank you for choosing Smile.";
                            helper.sendSMS(to, from, text); // To the redeemer (thirdparty);
                            return true;
                        } else {
                            strBuf.append("Account " + accountId + " has been credited with N" + (voucher.getValueInCents() / 100)
                                    + ". Account balance is N" + (accBean.getAvailableBalanceInCents() / 100)
                                    + ". If you want to convert airtime to data, reply with the exact name of the data plan."
                                    + "Choose from:\n");

                            strBuf.append(strBufBundles);
                            strBuf.append("Smile");
                            helper.sendSMS(to, from, strBuf.toString());
                            return true;
                        }
                    } else if (voucher.getUnitCreditSpecificationId() > 0) { // Bundle type voucher. 

                        if (isRedeemedByThirdParty) {
                            String bunName = (String) allUCSNames.get("" + voucher.getUnitCreditSpecificationId());
                            strBuf.append("You have received " + bunName
                                    + " from " + com.smilecoms.commons.util.Utils.getFriendlyPhoneNumber(from) + ", " + bunName
                                    + " is now active on your account [" + accountId + "]. Enjoy your SuperFast internet from Smile.");
                            helper.sendSMS(to, replyTo, strBuf.toString());
                            //Confirm to the sender as well;
                            text = "Your " + bunName + " plan recharge was successful on account " + accountId + ". Thank you for choosing Smile!";
                            helper.sendSMS(to, from, text); // To the redeemer (thirdparty);
                            return true;
                        } else {
                            text = "Your " + allUCSNames.get("" + voucher.getUnitCreditSpecificationId()) + " data plan recharge was successful on account [" + accountId + "]. Thank you for choosing Smile!";
                            helper.sendSMS(to, from, text);
                            return true;
                        }
                    }
                } catch (java.lang.Exception ex0) { // Check error during redemption
                    log.error("Error while trying to redeem strip [" + voucherPin + "], on account [ " + accountId + "], error message [" + ex0.getMessage() + "].");
                    // try {

                    // Retrieve stip
                    // voucher = com.smilecoms.commons.sca.beans.AccountBean.getVoucher(voucherPin);
                    String errorCode = null;
                    String errorDesc = null;
                    text = "";
                    // Voucher exists so error is something else, just respond with it.
                    if (ex0 instanceof com.smilecoms.commons.sca.SCABusinessError) {
                        errorCode = ((com.smilecoms.commons.sca.SCAErr) ex0).getErrorCode();
                        errorDesc = ((com.smilecoms.commons.sca.SCAErr) ex0).getErrorDesc();
                    }

                    if (errorCode != null && errorCode.equalsIgnoreCase("PVS-0008")) { // Voucher expired.
                        text = "The voucher associated with PIN [" + voucherPin + "] has expired.";
                    } else if (errorCode != null && errorCode.equalsIgnoreCase("BM-0002")) {
                        log.error("Unexpected error while trying to redeem voucher strip [" + voucherPin + "], on account [ " + accountId + "], error message [" + ex0.getMessage() + "].");
                        text = "Unexpected error while trying to redeem voucher [" + voucherPin + "]. Please call 0702 044 4444, FREE from your Smile number, for assistance 24/7.";
                    } else {

                        // helper.sendSMS(to, from, text);  
                        // } catch (java.lang.Exception ex1) { // Voucher does not exist -  check locking count here ...
                        log.error("Error while trying to process voucher PIN [" + voucherPin + "], on account [" + accountId + "], error message [" + ex0.getMessage() + "].");
                        com.smilecoms.commons.sca.VoucherLockForAccount vlA = null;

                        try {
                            if (isRedeemedByThirdParty) {
                                vlA = com.smilecoms.commons.sca.beans.AccountBean.getVoucherLockForAccount(thirdPartyAccountId);
                            } else {
                                vlA = com.smilecoms.commons.sca.beans.AccountBean.getVoucherLockForAccount(accountId);
                            }
                        } catch (java.lang.Exception ex1) {
                            log.error("Error while tryig to retrieve voucher lock for account [" + accountId + "], error [" + ex1.getMessage() + "].");
                            vlA = null;
                        }

                        if (vlA == null || !vlA.isFound()) {
                            text = "Unexpected system fault encountered while trying to process voucher PIN [" + voucherPin + "] on account [" + accountId + "]. Please call 0702 044 4444 FREE from your Smile number for assistance. Smile";
                            helper.sendSMS(to, from, text);
                            return true;
                        }

                        if (vlA.getAccountAttempts() == 1) { //First attempt with wrong ACCOUNT
                            //text = "This request is unsuccessful as the account number provided is not recognised on the system. One more attempt left to send the PIN - if unsuccessful your account will be temporarily locked. Call 0702 044 4444 FREE from your Smile number for any assistance. Smile";
                            text = "Account " + accountId + " does not exist. Please try again. Thank you for choosing Smile.";
                        }

                        if (vlA.getAttempts() == 1) { //First attempt with wrong PIN
                            text = "This PIN number is not recognized on the system, and we are unable to process this request. Please try again! Smile";
                        }

                        if (vlA.getAttempts() == 2) { //Second attempt with wrong PIN
                            text = "This request is unsuccessful as the PIN provided is not recognized on the system. One try left to send the PIN - if unsuccessful this account will be temporarily locked. Call 0702 044 4444 FREE from your Smile number for any assistance. Smile";
                        }

                        if (vlA.getAttempts() == 3) { //Third attempt with wrong PIN
                            text = "This account is temporarily locked for Voucher Recharge transactions. Please call, 0702 044 4444, available 24/7, FREE from your Smile number, for assistance and to unlock this account. Smile";
                        }

                        if (vlA.getAttempts() > 3 || vlA.getAccountAttempts() > 1) {//3+ attempt
                            text = "This request cannot be processed as this account is temporarily locked for Voucher Recharge. Please call 0702 044 4444, FREE from your Smile number, for assistance 24/7.";
                        }

                    }
                    helper.sendSMS(to, from, text);
                    return true;
                }
            } else if (msg.trim().length() >= 1) { // Single digit for actual bundle to recharge with.
                String requestedUcsSpec = (String) applicableUcsSpecs.get(msg.trim());
                String requestedUcsName = (String) applicableUcsNames.get(msg.trim());
                String res;
                if (requestedUcsSpec != null) {
                    res = "Your " + requestedUcsName + " data plan recharge is now active on account " + accountId + ". Thank you for choosing Smile";
                    try {
                        com.smilecoms.commons.sca.PurchaseUnitCreditRequest pucr = new com.smilecoms.commons.sca.PurchaseUnitCreditRequest();
                        pucr.setProductInstanceId(new java.lang.Integer(si.getProductInstanceId()));
                        pucr.setAccountId(si.getAccountId());
                        log.debug("Specific Requested account so we use that  but the paying account is still the SI account");
                        pucr.setAccountId(si.getAccountId());
                        pucr.setPaidByAccountId(si.getAccountId());
                        pucr.setUnitCreditSpecificationId(java.lang.Integer.parseInt(requestedUcsSpec));
                        pucr.setNumberToPurchase(1);
                        com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().purchaseUnitCredit(pucr);
                    } catch (com.smilecoms.commons.sca.SCABusinessError e) {
                        res = "Dear Customer, you have insufficient funds for this bundle purchase. Please call 0702 044 4444, FREE from your Smile number, for assistance 24/7.";
                    }
                    helper.sendSMS(to, from, res);
                } else { //Invalid bundle here
                    res = "The data plan name sent is not recognized. Please reply with the exact name of the data plan you want to recharge with or call 0702 044 4444 FREE from your Smile number for assistance. Smile";
                    helper.sendSMS(to, from, res);
                }
            } else { //Keep quiet 
                // text =  "";
                // helper.sendSMS(to, from, text);
            }

        } // For vouchers ....
        //  cust.getProductInstances().get(0).getProductServiceInstanceMappings().get(0).getServiceInstance().getAccountId();
        return true;
    }

    public boolean bundleBot(String from, String to, String msg, com.smilecoms.mm.plugins.shortcodesms.ShortCodeActionHelper helper, org.slf4j.Logger log) {

        log.debug("Entering bundleBot");

        StringBuilder txt = new StringBuilder();
        txt.append("Dear Customer, Welcome to Smile top up.\n");

        //getting all applicable unit credits available to TPGW and with customer role
        java.util.Map applicableUcsSpecs = new java.util.HashMap();
        java.util.Map applicableUcsNames = new java.util.HashMap();
        com.smilecoms.commons.sca.beans.CatalogBean unitCreditCatalog = com.smilecoms.commons.sca.beans.CatalogBean.getUnitCreditCatalog();
        java.util.List unitCreditSpecifications = unitCreditCatalog.getUnitCreditSpecifications();
        for (int a = 0; a < unitCreditSpecifications.size(); a++) {
            com.smilecoms.commons.sca.beans.UnitCreditSpecificationBean ucs = (com.smilecoms.commons.sca.beans.UnitCreditSpecificationBean) unitCreditSpecifications.get(a);
            if (ucs.getConfiguration().contains("TPGW=true") && ucs.getPurchaseRoles().contains("Customer")) {
                applicableUcsSpecs.put("" + ucs.getPriceInCents(), "" + ucs.getUnitCreditSpecificationId());
                applicableUcsNames.put("" + ucs.getPriceInCents(), ucs.getName());
                txt.append("Send ");
                int tmp = (int) (ucs.getPriceInCents() / 100);
                txt.append(tmp);
                txt.append(" for ");
                txt.append(ucs.getName());
                txt.append("\n");
            }
        }

        //for testing only!
        //applicableUcsSpecs.put("" + 0.0, "" + 156);
        //applicableUcsNames.put("" + 0.0, "10GB NOC bundle");
        txt.append("For more information visit https://smile.co.tz/data-bundles/. Smile #NowYouCan");
        String res = txt.toString();

        //String res = "Dear Customer, Welcome to Smile top up, send 42500 for 5GB, 80000 for 10GB, etc. For details, visit https://smile.co.tz/data-bundles/. Smile #NowYouCan";
        com.smilecoms.commons.sca.Customer cust = helper.getCustomerWithAvpsByPhoneNumber(from);

        String[] parameters = msg.split("\\s+");
        String requestedUnitCreditPrice = "";
        long requestedAccountId = -1;
        switch (parameters.length) {
            case 2:
                log.debug("Message is 2 parameters - 1st is accountId or phone number 2nd is requestedUnitCreditPrice");
                boolean numberFormat = true;
                long firstParameter = -1;
                int firstTwoDigits = -1;
                try {
                    firstTwoDigits = java.lang.Integer.parseInt(parameters[0].substring(0, 2));
                    firstParameter = java.lang.Long.parseLong(parameters[0]);
                } catch (java.lang.NumberFormatException e) {
                    numberFormat = false;
                }
                if (numberFormat && firstTwoDigits > 11 && firstTwoDigits < 25 && parameters[0].length() == 10) {
                    log.debug("First parameter is number format and first 2 digits are between 12 and 24 and length is 10 - so this is account format");
                    if (!helper.isAccountOwnedByCustomer(cust, firstParameter)) {
                        res = "Dear Customer, the account number given does not belong to you, Kindly specify your own account or SmileVoice number, for support call 0662100100. Smile #NowYouCan";
                        helper.sendSMS(to, from, res);
                        return true;
                    }
                    requestedAccountId = firstParameter;
                    requestedUnitCreditPrice = parameters[1];
                } //if not an account and in number 
                else if (numberFormat && parameters[0].length() >= 10) {
                    log.debug("First parameter is number and not account format and length is more than or equal to 10 - so we assume it is a number");
                    com.smilecoms.commons.sca.ServiceInstance si = null;
                    try {
                        String impu = com.smilecoms.commons.util.Utils.getPublicIdentityForPhoneNumber(parameters[0]);
                        si = helper.getServiceInstance(impu);
                    } catch (com.smilecoms.commons.sca.SCABusinessError e) {
                        log.debug("Could find a service for this phone number");
                    }

                    if (si == null || !helper.isServiceInstanceOwnedByCustomer(cust, si.getServiceInstanceId())) {
                        res = "Dear Customer, the SmileVoice number given does not belong to you, Kindly specify your own SmileVoice or account number, for support call 0662100100. Smile #NowYouCan";
                        helper.sendSMS(to, from, res);
                        return true;
                    } else {
                        requestedUnitCreditPrice = parameters[1];
                        requestedAccountId = si.getAccountId();
                    }
                } else {
                    log.debug("Not supported, id 2 parameters first parameter must be in account or phone number format");
                    helper.sendSMS(to, from, res);
                    return true;
                }
                break;
            case 1:
                log.debug("Message is 1 parameter - this is requestedUnitCreditPrice");
                requestedUnitCreditPrice = parameters[0];
                break;
            default:
                log.debug("Not supported, msg must be either 1 or 2 parameters");
                helper.sendSMS(to, from, res);
                return true;
        }

        try {
            double input = Double.parseDouble(requestedUnitCreditPrice);
            String stringInput = "" + input * 100; //convert to cents;
            String requestedUcsSpec = (String) applicableUcsSpecs.get(stringInput);
            if (requestedUcsSpec != null) {
                res = "Dear Customer, your bundle purchase request has been processed successfully, Thank you for choosing Smile.";
                try {
                    com.smilecoms.commons.sca.PurchaseUnitCreditRequest pucr = new com.smilecoms.commons.sca.PurchaseUnitCreditRequest();
                    com.smilecoms.commons.sca.ServiceInstance si = helper.getServiceInstance(from);
                    if (requestedAccountId == -1) {
                        log.debug("No requested account so we check account of from number");

                        pucr.setProductInstanceId(new java.lang.Integer(si.getProductInstanceId()));
                        pucr.setAccountId(si.getAccountId());
                    } else {
                        log.debug("Specific Requested account so we use that  but the paying account is still the SI account");
                        pucr.setAccountId(requestedAccountId);
                        pucr.setPaidByAccountId(si.getAccountId());
                    }

                    pucr.setUnitCreditSpecificationId(java.lang.Integer.parseInt(requestedUcsSpec));
                    pucr.setNumberToPurchase(1);
                    com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().purchaseUnitCredit(pucr);
                } catch (com.smilecoms.commons.sca.SCABusinessError e) {
                    res = "Dear Customer, you have insufficient funds for this bundle purchase, Please visit a nearby shop to top up, or via mobile money, bus no 110110. Smile #NowYouCan";
                }

            }
        } catch (java.lang.NumberFormatException e) {
            log.debug("Input is not a number");
        }
        log.debug(res);
        helper.sendSMS(to, from, res);

        return true;
    }

    public boolean NgoptInBot(String from, String to, String msg, com.smilecoms.mm.plugins.shortcodesms.ShortCodeActionHelper helper, org.slf4j.Logger log) {
        log.debug("From [{}] to [{}] msg [{}]", new Object[]{from, to, msg});
        if (from.equals("sip:+2347020444444@ng.smilecoms.com")) {
            log.debug("skipping");
            return false;
        }
        msg = msg.toLowerCase().trim();
        try {
            if (msg.indexOf("start") >= 0) {
                helper.setSIpURIUsersOptInLevelBit(from, 3, 1);
                helper.sendSMS(to, from, "You have opted in to receive promotional information from Smile. To Opt Out at any time send STOP to 2442 (FREE)");
            } else if (msg.indexOf("stop") >= 0) {
                //helper.sendSMS(to, from, "You have opted out to receive promotional information from Smile. To Opt In at any time send START to 2442 (FREE)");
                helper.sendSMS(to, from, "FULL DND is now active on your line.  Thank you");
                helper.setSIpURIUsersOptInLevelBit(from, 3, 0);
            } else if (msg.indexOf("status") >= 0) {
                com.smilecoms.commons.sca.Customer cust = helper.getCustomerWithAvpsByPhoneNumber(from);
                if ((cust.getOptInLevel() & 4) == 0) {
                    helper.sendSMS(to, from, "The Full DND service is now active on your line.");
                } else {
                    helper.sendSMS(to, from, "You have opted in to receive promotional information from Smile. To Opt Out at any time send STOP to 2442 (FREE)");
                }
            }
        } catch (com.smilecoms.commons.sca.SCABusinessError e) {
            log.warn("Error:", e);
        }
        return true;
    }

}
