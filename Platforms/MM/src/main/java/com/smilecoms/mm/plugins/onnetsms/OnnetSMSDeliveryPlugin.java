/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.plugins.onnetsms;

import com.hazelcast.core.IMap;
import com.hazelcast.core.Member;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.props.PropertyFetchException;
import com.smilecoms.commons.sca.SCABusinessError;
import com.smilecoms.commons.util.Codec;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.mm.BinaryShortMessage;
import com.smilecoms.mm.InsufficientFundsException;
import com.smilecoms.mm.MessageManager;
import com.smilecoms.mm.engine.*;
import com.smilecoms.mm.plugins.offnetsms.smsc.SmileSMSC;
import com.smilecoms.mm.plugins.onnetsms.model.IpsmgwContact;
import com.smilecoms.mm.plugins.onnetsms.model.IpsmgwImpu;
import com.smilecoms.mm.plugins.onnetsms.model.IpsmgwSubscription;
import com.smilecoms.mm.plugins.onnetsms.op.IpsmDAO;
import com.smilecoms.mm.plugins.onnetsms.reginfo.ietf.params.xml.ns.reginfo.Contact;
import com.smilecoms.mm.plugins.onnetsms.reginfo.ietf.params.xml.ns.reginfo.Reginfo;
import com.smilecoms.mm.plugins.onnetsms.reginfo.ietf.params.xml.ns.reginfo.Registration;
import com.smilecoms.mm.sms.MultiPartSmsMessage;
import com.smilecoms.mm.sms.SmsHeader;
import com.smilecoms.mm.sms.SmsMessage;
import com.smilecoms.mm.sms.rp.messages.RpAckMessage;
import com.smilecoms.mm.sms.rp.messages.RpDataMessage;
import com.smilecoms.mm.sms.rp.messages.RpErrorMessage;
import com.smilecoms.mm.sms.rp.messages.RpMessage;
import com.smilecoms.mm.utils.SMSCodec;
import gov.nist.javax.sip.RequestEventExt;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.header.ims.PAssertedIdentity;
import gov.nist.javax.sip.header.ims.PCalledPartyIDHeader;
import gov.nist.javax.sip.header.ims.Path;
import gov.nist.javax.sip.header.ims.PathHeader;
import gov.nist.javax.sip.header.ims.PathList;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.parser.AddressParser;
import java.io.Serializable;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import org.slf4j.*;

/**
 *
 * @author JBP
 */
public class OnnetSMSDeliveryPlugin implements DeliveryPipelinePlugin, SipListener {

    private static final Logger log = LoggerFactory.getLogger(OnnetSMSDeliveryPlugin.class);
    private static final String className = OnnetSMSDeliveryPlugin.class.getName();
    private static final String transport = "udp";
    private static String myScUri = BaseUtils.getProperty("env.mm.ipsmgw.servicecenter.uri", "sip:+0@notset.nodomain");   //SC URI like "sip:+255662000000@tz.smilecoms.com
    private static String myScE164 = BaseUtils.getProperty("env.mm.ipsmgw.servicecenter.e164", "0");   //SC number in E.164 like "255662000000
    private static String ipAddressString = BaseUtils.getIPAddress();   //"10.24.0.65"
    private static String mySipDomain = BaseUtils.getProperty("env.sip.domain", "smilecoms.com");
    private static byte VOLTE_CODING_SCHEME = (byte) BaseUtils.getIntProperty("env.mm.volte.coding.scheme", SMSCodec.NOSMPP_UCS_2_CODING_SCHEME);
    private static byte APP_CODING_SCHEME = (byte) BaseUtils.getIntProperty("env.mm.app.coding.scheme", SMSCodec.NOSMPP_UTF_8_CODING_SCHEME);
    public static final ArrayBlockingQueue<Long> pendingSubscriptionDialogs = new ArrayBlockingQueue<>(BaseUtils.getIntProperty("env.mm.ipsmgw.pendingsubscriptionqueuesize", 1500), true);
    private static final Set<String> pendingPresentityURIs = new HashSet<>();
    private static final ConcurrentHashMap<String, Integer> activeRegistrations = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> activeMessageTransaction = new ConcurrentHashMap<>();
    private static final short RESP_CODE_NO_FUNDS = (short) BaseUtils.getIntProperty("env.mm.no.funds.response.code", 403);
    private static DeliveryEngine engine;
    private static SipStack sipStack;
    private static SipProvider sipProvider;
    private static AddressFactory addressFactory;
    private static MessageFactory messageFactory;
    private static HeaderFactory headerFactory;
    private static ListeningPoint udpListeningPoint;
    private static int port = 9060;
    private static EntityManagerFactory emf;
    private static boolean mustShutdown = false;
    private static int resubscriptionThreshold1 = BaseUtils.getIntProperty("env.mm.ipsmgw.resubscriptionthreshold1seconds", (20 * 60)); //20 minutes by default
    private static int resubscriptionThreshold2 = BaseUtils.getIntProperty("env.mm.ipsmgw.resubscriptionthreshold2seconds", (10 * 60)); //10 minutes by default
    private String myWatcherURI;
    private static Boolean sendDeliveryNotifications = Boolean.valueOf(BaseUtils.getProperty("env.mm.ipsmgw.senddeliverynotifications", "false"));
    public static String otaTest = BaseUtils.getProperty("env.mm.ipsmgw.otatest", "false");
    public static String otaString = BaseUtils.getProperty("env.mm.ipsmgw.otastring", "");
    public static String sipUserAgent = BaseUtils.getProperty("env.mm.ipsmgw.sipuseragent", "Smile IPSM Server");

    public static EntityManagerFactory getEMF() {
        return emf;
    }

    //Message received from Delivery engine - most likely for onnet delivery
    @Override
    public DeliveryPluginResult processMessage(BaseMessage msg, DeliveryEngine callbackEngine) {
        engine = callbackEngine;

        //We must deliver an onnet message
        OnnetSMSMessage message = (OnnetSMSMessage) msg;
        if (log.isDebugEnabled()) {
            log.debug("MMSIP: Asked to send message [{}]", message);
        }
        String uuid = Utils.getUUID();
        message.setUuid(uuid);
        if (message.contactParts == null) {
            message.contactParts = new HashMap<>();
        }

        try {
            log.debug("Checking if from: [{}] needs to be overwritten", message.getFrom());
            message.setFrom(BaseUtils.getSubProperty("env.mm.from.overwrite", message.getFrom()));
            log.debug("Number is to be rewritten to [{}]", message.getFrom());
        } catch (PropertyFetchException e) {
            log.debug("No property for env.mm.from.overwrite [{}]", message.getFrom());
        }

        boolean ret = false;
        message.getDeliveryContext().lock();
        /* locking here because we don't want responses processed until the message has been sent to all contacts */
        try {
            ret = sendMessageOverSIP(message);
        } finally {
            message.getDeliveryContext().unlock();
        }

        if (!ret) {
            FinalDeliveryPluginResult res = new FinalDeliveryPluginResult();
            if (message.getCodingSchemeHex().toLowerCase().charAt(0) == 'f') {
                log.debug("This is a flash message so dont retry [{}]", message.getCodingSchemeHex());
                res.setMustRetry(false);
                res.setPluginClassName(OnnetSMSDeliveryPlugin.className);
                return res;
            }
            log.debug("There was an error sending message to [{}].... will retry later", message.getTo());
            /* this means there was a tech failure and we couldnt even put the message on the wire */
            res.setMustRetry(true);
            res.setPluginClassName(OnnetSMSDeliveryPlugin.className);
            RetryTrigger rt = new RetryTrigger();
            log.debug("MMSIP: Setting trigger for user [{}]", message.getTo());
            rt.setTriggerKey(message.getTo());
            rt.setTriggerType(RetryTrigger.SIP_REGISTER);
            res.setRetryTrigger(rt);
            return res;
        } else {
            // Lets return asynchronously - ie. SIP MESSAGE request has been sent and we will respond after we get the response
            InitialDeliveryPluginResult res = new InitialDeliveryPluginResult();
            res.setCallBackId(uuid);
            return res;
        }
    }

    private static String removeRinstance(String uri) {
        int start = uri.indexOf(";rinstance");
        int end;
        if (start > 0) {
            end = uri.indexOf(";", start + 1);
            if (end > 0) {
                return uri.substring(0, start - 1) + uri.substring(end);
            } else {
                return uri.substring(0, start - 1);
            }
        } else {
            return uri;
        }
    }

    /* 
     * Send Message to ONNET subscriber. 
     * TODO do a check that all headers are correct as per spec and return messages - still need to support BINARY-SMS and delivery reports.
     * Will do this once we have a prdoper IMS Client with which to test
     */
    private boolean sendMessageOverSIP(OnnetSMSMessage msg) {
        log.debug("MMSIP: Received SIP Message to Send (On-Net) - [{}]", msg);
        log.debug("MMSIP: message as String is [{}]", msg.getMessage());
        HashMap<String, Integer> duplicateMessagePreventer = new HashMap<>();

        int ccLength = BaseUtils.getProperty("env.e164.country.code").length();
        int maxShortCodeLength = BaseUtils.getIntProperty("env.mm.shortcode.max.digits", 5) + ccLength;

        if (Utils.getFriendlyPhoneNumberKeepingCountryCode(msg.getFrom()).length() <= maxShortCodeLength) {
            log.debug("This source is a short code. Remove the leading + so its displayed nicely on phones");
            msg.setFrom(msg.getFrom().replace("+", ""));
            log.debug("Source is now [{}]", msg.getFrom());
        }

        /* we have to cater for TEL URI as well as SIP URI. All we do for TEL URI is effectively convert to the SIP equivalent by replacing tel: with sip: and tacking on the domain. This
         is feasible because according to spec every tel uri IMPU must have a SIP URI equivalent IMPU. 
         */
        String toURI = msg.getTo();
        if (toURI.startsWith("tel:")) {
            toURI = toURI.replace("tel:", "sip:");
            toURI = toURI.concat("@" + BaseUtils.getProperty("env.sip.domain"));
        }

        //do we have the target IMPU in our list and what format of SMS do it's contacts support binary/sip?
        String toURINoParams = toURI;
        if (toURI.indexOf(";") > 0) {
            toURINoParams = toURI.substring(0, toURI.indexOf(";"));
        }

        log.debug("MMSIP: looking for IMPU [{}]", toURINoParams);
        List<IpsmgwContact> contacts = IpsmDAO.getContactsForImpu(toURINoParams, emf);
        if (contacts != null && !contacts.isEmpty()) {
            log.debug("FOUND B-party IMPU of SMS in ImplicitImpu list and it has [{}] contacts", contacts.size());
        } else {
            log.debug("Could not find IMPU [{}] in implcitimpu list....", toURINoParams);
            return false;
        }

        boolean ret = false;
        String txID;
        String contactURINoRinstance;

        for (IpsmgwContact dbContact : contacts) {
            ImpuContact contact = buildContactfromDB(dbContact);
            if (log.isDebugEnabled()) {
                log.debug("Sending [{}] SMS to contact [{}] via path [{}]", new Object[]{contact.getSmsType(), contact.getUri(), contact.getPathList().getHeaderValue()});
            }
            contactURINoRinstance = dbContact.getSmsFormat() + ":" + removeRinstance(dbContact.getUri());
            if (duplicateMessagePreventer.isEmpty() || !duplicateMessagePreventer.containsKey(contactURINoRinstance)) {
                txID = sendMessageToContact(msg, contact);
                if (txID != null) {
                    msg.incrementPendingContacts();
                    duplicateMessagePreventer.put(contactURINoRinstance, 0);
                    ret = true; //successfully sent to at least one contact...
                } else {
                    log.warn("Failed to send MESSAGE to contact [{}]", contact.getUri());
                }
            } else {
                log.debug("ignoring a likely duplicate contact for URI [{}]", dbContact.getUri());
            }
        }

        duplicateMessagePreventer.clear();

        if (!ret) {
            log.error("Returning FALSE when sending On-Net MESSAGE...");
        }
        return ret;

    }

    /**
     * This actually sends a message to the individual contact via the owning
     * IMPU's currently assigned S-CSCF
     *
     * @param msg - The original msg as delivered from SCA - it has from, to and
     * message
     * @param contact - The contact to send to - has URI as well as format of
     * SMS to be used for this contact
     * @param scscfAddress - SCSCF through which we must route the request.
     * @param uuid - unique ID identifying the global originating message this
     * belongs to - many contacts can receive a single originated message. The
     * glue between these outgoing messages and the parent is this ID
     * @return - returns transaction ID of SIP transaction or null if there was
     * a problem with the delivery of the message.
     */
    private String sendMessageToContact(OnnetSMSMessage msg, ImpuContact contact) {
        ClientTransaction tx = null;
        ContactHeader contactHeader;
        ContentTypeHeader contentTypeHeader;
        PathList pathList = contact.getPathList();
        int numMessages;
        int messageID = Utils.getRandomNumber(1, 254);
        String msgText;
        int maxChars;
        int totalBytes;
        int remainder;


        /* we have to cater for TEL URI as well as SIP URI. All we do for TEL URI is effectively convert to the SIP equivalent by replacing tel: with sip: and tacking on the domain. This
         is feasible because according to spec every tel uri IMPU must have a SIP URI equivalent IMPU. 
         */
        String fromURI = msg.getFrom();
        if (fromURI.startsWith("tel:")) {
            fromURI = fromURI.replace("tel:", "sip:");
            fromURI = fromURI.concat("@" + BaseUtils.getProperty("env.sip.domain"));
        }

        String toURI = msg.getTo();
        if (toURI.startsWith("tel:")) {
            toURI = toURI.replace("tel:", "sip:");
            toURI = toURI.concat("@" + BaseUtils.getProperty("env.sip.domain"));
        }
        log.debug("MMSIP: Cleaned From:To for Onnet SMS: - from: [{}], to: [{}] and path is [{}]", new Object[]{fromURI, toURI, pathList.getHeaderValue()});

        String[] fromURIBits = fromURI.split("@");
        String[] toURIBits = toURI.split("@");

        try {
            msgText = SMSCodec.decode(msg.getMessage(), msg.getCodingScheme());
            if (log.isDebugEnabled()) {
                log.debug("Message [{}] with DCS [{}] is decoded as [{}]", new Object[]{Codec.binToHexString(msg.getMessage()), msg.getCodingScheme(), msgText});
            }
            if ((contact.getSmsType().equals(ImpuContact.SmsFormat.binary3gpp))) {
                log.debug("Message is destined for a 3gpp SMS device and the length of full message is [{}] bytes encoded with [{}]",
                        new Object[]{msg.getMessage().length, msg.getCodingScheme()});
                if (msg.getCodingScheme() == BaseUtils.getIntProperty("env.mm.offnet.default.ascii.coding.scheme", SMSCodec.ASCII_CODING_SCHEME) || msg.getCodingScheme() == SMSCodec.ASCII_CODING_SCHEME_FLASH) {
                    if (msgText.length() <= 160) {
                        maxChars = 160; //if there are just enough chars for a single message (ie we don't need a header, then we can send as a single message).
                    } else {
                        maxChars = 153; //otherwise we have to make the message payload smaller to have space for the UDHI
                    }
                } else {
                    //make sure we are in UCS2
//                    msg.setMessage(SMSCodec.encode(msgText, SMSCodec.NOSMPP_UCS_2_CODING_SCHEME));
                    maxChars = 134 / 2;
                }
            } else {
                maxChars = 20000;
            }

        } catch (UnsupportedEncodingException ex) {
            log.error("unsupported encoding exception trying to send message " + ex.getMessage());
            log.warn("Error: ", ex);
            return null;
        } catch (Exception ex) {
            log.error("exception trying to send message " + ex.getMessage());
            log.warn("Error: ", ex);
            return null;
        }
        log.debug("Decode version 1");
        totalBytes = msgText.length();//.getMessage().length;
        numMessages = (totalBytes + (maxChars - 1)) / maxChars;
        remainder = totalBytes % maxChars;

        if (numMessages > 1) {
            log.debug("MMSIP: This is a long message to be delivered to a VoLTE device with length [{}] - which will require [{}] messages", new Object[]{msgText.length(), numMessages});
        }

        HashSet<String> partsMap = new HashSet();

        try {
            for (int i = 0; i < numMessages; i++) {
                /*now we will loop through all of the messages.....*/
                SipURI fromAddress = addressFactory.createSipURI(fromURIBits[0].substring(4), fromURIBits[1]);
                Address fromNameAddress = addressFactory.createAddress(fromAddress);
                fromNameAddress.setDisplayName(fromURIBits[0].substring(4));
                FromHeader fromHeader = headerFactory.createFromHeader(fromNameAddress, "12345");

                SipURI toAddress = addressFactory.createSipURI(toURIBits[0].substring(4), toURIBits[1]);
                Address toNameAddress = addressFactory.createAddress(toAddress);
                toNameAddress.setDisplayName(toURIBits[0].substring(4));
                ToHeader toHeader = headerFactory.createToHeader(toNameAddress, null);

                // create Request URI
                URI requestURI = addressFactory.createURI(contact.getUri());
                // Create ViaHeaders
                ArrayList viaHeaders = new ArrayList();
                String ipAddress = udpListeningPoint.getIPAddress();
                ViaHeader viaHeader = headerFactory.createViaHeader(ipAddress, sipProvider.getListeningPoint(transport).getPort(), transport, null);
                viaHeaders.add(viaHeader);
                // Create a new Cseq header
                CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1L, Request.MESSAGE);
                // Create a new MaxForwardsHeader
                MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);
                // Create a new CallId header
                CallIdHeader callIdHeader = sipProvider.getNewCallId();
                partsMap.add(callIdHeader.getCallId());
                msg.contactParts.put(callIdHeader.getCallId(), partsMap);

                // Create the request.
                Request request = messageFactory.createRequest(requestURI, Request.MESSAGE, callIdHeader, cSeqHeader, fromHeader, toHeader, viaHeaders, maxForwards);
                // Create contact headers
                String host = BaseUtils.getIPAddress();

                for (Path p : pathList.getHeaderList()) {
                    RouteHeader newRouteHeader = headerFactory.createRouteHeader(p.getAddress());
                    request.addHeader(newRouteHeader);
                }

                SipURI contactUrl = addressFactory.createSipURI(fromURIBits[0].substring(4), host);
                contactUrl.setPort(udpListeningPoint.getPort());
                contactUrl.setLrParam();

                // Create the contact name address.
                SipURI contactURI = addressFactory.createSipURI(fromURIBits[0].substring(4), host);
                contactURI.setPort(sipProvider.getListeningPoint(transport).getPort());

                Address contactAddress = addressFactory.createAddress(contactURI);

                // Add the contact address
                contactAddress.setDisplayName(fromURIBits[0].substring(4));

                contactHeader = headerFactory.createContactHeader(contactAddress);
                request.addHeader(contactHeader);

                Header acceptContactHeader = headerFactory.createHeader("Accept-Contact", "*;+g.3gpp.smsip");
                request.addHeader(acceptContactHeader);
                Header paiHeader = headerFactory.createHeader("P-Asserted-Idenitity", myScUri);
                request.addHeader(paiHeader);
                Header contentTransferEncodingHeader = headerFactory.createHeader("Content-Transfer-Encoding", "binary");
                request.addHeader(contentTransferEncodingHeader);
                List x = new ArrayList();
                x.add(sipUserAgent);
                UserAgentHeader userAgentHeader = headerFactory.createUserAgentHeader(x);
                request.addHeader(userAgentHeader);

                if (log.isDebugEnabled()) {
                    log.debug("Message length is [{}] and coding scheme is [{}]", msg.getMessage().length, msg.getCodingSchemeHex());
                }

                ContentLengthHeader contentLengthHeader = headerFactory.createContentLengthHeader(msgText.length());
                request.setContentLength(contentLengthHeader);

                log.debug("MMSIP: Cleaned From:To for Onnet SMS: - from: [{}], to: [{}] and path is [{}]", new Object[]{fromURI, toURI, pathList.getHeaderValue()});

                if (contact.getSmsType() == ImpuContact.SmsFormat.sip) {
                    log.debug("Sending SMS using plain/text");
                    contentTypeHeader = headerFactory.createContentTypeHeader("text", "plain");
                    request.setContent(msgText, contentTypeHeader);
                } else {
                    log.debug("Sending SMS using application/vnd.3gpp.sms");
                    /* assume for now that this will be a binary SMS */
                    RpDataMessage rpDataMessage = new RpDataMessage(RpMessage.Direction.outgoing, Utils.getPhoneNumberFromSIPURI(fromURI), Utils.getPhoneNumberFromSIPURI(toURI));
                    SmsMessage smsMessage = new SmsMessage(SmsMessage.MessageDirection.OUTGOING, SmsMessage.MessageType.SMS_DELIVER,
                            Utils.getPhoneNumberFromSIPURI(fromURI), msg.getMessageDate());
                    smsMessage.setmMessageRef(rpDataMessage.getMessageReference());
                    smsMessage.setmValidityPeriodLength(4/*number of days*/);
                    if (msg.getCodingScheme() == BaseUtils.getIntProperty("env.mm.offnet.default.ascii.coding.scheme", SMSCodec.ASCII_CODING_SCHEME)) {
                        smsMessage.setmDataCodingScheme(0);
                    } else if (msg.getCodingScheme() == 16) {
                        smsMessage.setmDataCodingScheme(16);    //this is for flash sms messages 7bit alphabet class 0
                    } else {
                        //assume UCS2 16 bit
                        smsMessage.setmDataCodingScheme(8);
                    }

                    if (i == (numMessages - 1) && remainder != 0) {
//                        smsMessage.setmUserData(Arrays.copyOfRange(msg.getMessage(), maxMessageBytes * i, maxMessageBytes * i + remainder));
                        smsMessage.setmUserData(msgText.substring(maxChars * i, maxChars * i + remainder).getBytes());
                    } else {
                        smsMessage.setmUserData(msgText.substring(maxChars * i, maxChars * i + (maxChars)).getBytes());
//                        smsMessage.setmUserData(Arrays.copyOfRange(msg.getMessage(), maxMessageBytes * i, maxMessageBytes * i + (maxMessageBytes-1)));
                    }

                    smsMessage.setMessageInfo(i + 1, numMessages, messageID);
                    rpDataMessage.setSmsMessage(smsMessage);

                    contentTypeHeader = headerFactory.createContentTypeHeader("application", "vnd.3gpp.sms");
                    byte[] payloadBytes = rpDataMessage.serialise();
                    //        if (payloadBytes != null) {
                    //            for (int i = 0; i < payloadBytes.length; i++) {
                    //                log.debug(Integer.toHexString(payloadBytes[i]));
                    //            }
                    //        }
                    request.setContent(payloadBytes, contentTypeHeader);
                }

                // send the request out.
                tx = sipProvider.getNewClientTransaction(request);
                tx.setApplicationData(msg);
                log.debug("MMSIP: Sending SIP MESSAGE using sipProvider. Call Id [{}] Tx [{}]", callIdHeader.getCallId(), tx);
                tx.sendRequest();
                log.debug("MMSIP: Sent SIP MESSAGE using sipProvider. Call Id [{}] Branch Id [{}]", callIdHeader.getCallId(), tx.getBranchId());
                //            addtoActiveTransactions(tx);
            }
        } catch (ParseException ex) {
            log.warn("MMSIP: Unable to parse From address", ex);
            log.warn("Error: ", ex);
            return null;
        } catch (InvalidArgumentException ex) {
            log.warn("MMSIP: Invalid Argument", ex);
            log.warn("Error: ", ex);
            return null;
        } catch (TransactionUnavailableException ex) {
            log.warn("MMSIP: unable to create Transaction", ex);
            log.warn("Error: ", ex);
            return null;
        } catch (SipException ex) {
            log.warn("MMSIP: SIPException", ex);
            log.warn("Error: ", ex);
            return null;
        } catch (SCABusinessError bus) {
            log.warn("MMSIP: SCABusinessError", bus);
            log.warn("Error: ", bus);
            return null;
        } catch (Exception ex) {
            log.warn("Failed to send message");
            log.warn("Error: ", ex);
            return null;
        }

        return (tx == null ? null : tx.getBranchId());
    }

    @Override
    public void initialise(EntityManagerFactory emf) {

        if (emf != null) {
            OnnetSMSDeliveryPlugin.emf = emf;
        }

        SipFactory sipFactory;
        sipStack = null;
        myWatcherURI = "sip:" + BaseUtils.getIPAddress();

        log.debug("MMSIP: Creating MM SIP Stack");
        try {
            sipFactory = SipFactory.getInstance();
            sipFactory.setPathName("gov.nist");
            log.debug("MMSIP: Here are the MM SIP Stack Properties as contained in env.mm.sipstack.config \n");
            log.debug(BaseUtils.getProperty("env.mm.sipstack.config"));
            Properties properties = new Properties();
            properties.load(BaseUtils.getPropertyAsStream("env.mm.sipstack.config"));
            sipStack = sipFactory.createSipStack(properties);
        } catch (Exception e) {
            log.error("MMSIP: Could not create MM SIP Stack... aborting", e);
            return;
        }

        try {
            port = Integer.parseInt(BaseUtils.getProperty("env.mm.sipstack.port"));
        } catch (Exception e) {
            log.debug("MMSIP: no port set for SIP stack in Properties, using default [{}]", port);
        }

        log.debug("MMSIP: Using port [{}] for MM SIP stack", port);

        try {
            headerFactory = sipFactory.createHeaderFactory();
            addressFactory = sipFactory.createAddressFactory();
            messageFactory = sipFactory.createMessageFactory();

            log.debug("MMSIP: Trying to create SIP stack listening on {}:{}", ipAddressString, port);
            udpListeningPoint = sipStack.createListeningPoint(ipAddressString, port, transport);
            sipProvider = sipStack.createSipProvider(udpListeningPoint);
            sipProvider.addSipListener(this);
            log.info("MMSIP: Successfully added SIP Listener");
        } catch (Exception e) {
            log.error("MMSIP: Unable to initialise MM SIP Stack", e);
        }
        try {
            sipStack.start();
        } catch (SipException ex) {
            log.error("MMSIP: Failed to start SIP Stack", ex);
        }

        mustShutdown = false;

        /* start the thread to manage the pending subscriptions (basically an async list to send inital subscriptions) */
        int numPendingWorkers = BaseUtils.getIntProperty("env.mm.ipsmgw.pendingworkerthreads", 2);
        for (int i = 0; i < numPendingWorkers; i++) {
            PendingSubscriptionThread t = new PendingSubscriptionThread(i, pendingSubscriptionDialogs);
            t.start();
        }

        Thread t = new Thread(
                new Runnable() {
            @Override
            public void run() {
                EntityManager em = null;

                while (!mustShutdown) {
                    try {
                        EntityManagerFactory emf = OnnetSMSDeliveryPlugin.getEMF();
                        if (emf == null) {
                            log.warn("For some reason EMF is null!!! Exiting Active Subscription thread");
                            break;
                        }
                        int fetchSize = BaseUtils.getIntProperty("env.mm.ipsmgw.expiry.fetch.size", 1000);
                        log.debug("Active Subscription thread running");

                        em = JPAUtils.getEM(emf);
                        JPAUtils.beginTransaction(em);
                        List<Long> subscriptions = IpsmDAO.getActiveSubscriptionsExpiringWithin(em, myWatcherURI, resubscriptionThreshold1, resubscriptionThreshold2, fetchSize);
                        log.debug("[{}] Subscriptions to process that have failed or need resubscribing", subscriptions.size());
                        JPAUtils.commitTransaction(em);
                        IpsmgwSubscription s = null;
                        for (long sId : subscriptions) {
                            boolean addToPending = false;
                            try {
                                JPAUtils.beginTransaction(em);
                                s = IpsmDAO.getLockedSubscription(em, sId);
                                if (s == null) {
                                    log.debug("Subscription [{}] is no longer in DB", sId);
                                    continue;
                                }

                                if (log.isDebugEnabled()) {
                                    long secs = -1;
                                    Date now = new Date();
                                    if (s.getExpires() != null) {
                                        secs = (s.getExpires().getTime() - now.getTime()) / 1000;
                                    }
                                    log.debug("Processing Subscription [{}] in state [{}] and expires in [{}] seconds, and has [{}] linked IMPUs",
                                            new Object[]{s.getPresentityUri(), s.getStateEnum().name(), secs, s.getIpsmgwImpuCollection().size()});
                                }

                                switch (s.getStateEnum()) {
                                    case active:
                                        if (!isSubscriptionValid(s)) {
                                            log.debug("Subscription for [{}] is invalid state [{}], expires [{}] - resetting and then resubscribing", new Object[]{s.getPresentityUri(), s.getStateEnum().name(), s.getExpires()});
                                            log.warn("I would normally resubscribe here because the subscription is no longer valid in state [{}]", s.getState());
                                            s.setStateEnum(IpsmgwSubscription.State.failed);
                                        } else {
                                            log.debug("Resubscribing to presentity [{}]", s.getPresentityUri());
                                            s.setStateEnum(IpsmgwSubscription.State.resubscribing);
                                            addToPending = true;
                                        }
                                        em.persist(s);
                                        break;
                                    case failed:
                                        log.debug("Subscription for [{}] in failed state and has retries of [{}]", new Object[]{s.getPresentityUri(), s.getRetries()});
                                        if (s.getRetries() < 4) {
                                            log.debug("retries < 10 - retrying");
                                            log.debug("Resubscribing to presentity [{}]", s.getPresentityUri());
                                            s.incrementRetries();
                                            s.setStateEnum(IpsmgwSubscription.State.resubscribing);
                                            addToPending = true;
                                        } else {
                                            log.debug("Giving up - deleting");
                                            em.remove(s);
                                        }
                                        break;
                                    default:
                                        log.debug("subscription in unhandled state [{}]... ignoring", s.getStateEnum().name());
                                }

                            } catch (Exception e) {
                                log.warn("Exception trying to update subscription");
                                log.warn("Error: ", e);
                                JPAUtils.rollbackTransaction(em);
                            } finally {
                                JPAUtils.commitTransactionAndClear(em);
                                if (s != null) {
                                    s.unlock();
                                }
                                if (addToPending) {
                                    addToPendingSubscriptionDialogs(s);
                                }
                            }
                        }
                        if (fetchSize != subscriptions.size()) {
                            log.debug("Sleeping for 5s as we dont have much work to do");
                            for (int i = 0; i < 5 && !mustShutdown; i++) {
                                Thread.sleep(1000);
                            }
                        } else {
                            log.debug("Not sleeping as we have lots of work to do");
                        }
                    } catch (Exception ex) {
                        log.error("Error in main OnnetSMS Housekeeping thread", ex);
                        log.warn("Error: ", ex);
                    } finally {
                        JPAUtils.closeEM(em);
                    }
                }
                log.warn("Active subscription thread shutting down");
            }
        }, "IpsmgwActiveSubscriptionSweeper");
        t.setContextClassLoader(this.getClass().getClassLoader());
        t.start();
    }

    /**
     * Check if the subscription is valid must be in state (active, subscribing,
     * or resubscribing), AND not have expired
     *
     * @param s subscription to test
     * @return true if valid, false if not
     */
    private boolean isSubscriptionValid(IpsmgwSubscription s) {
        Date now = new Date();
        if (s.getStateEnum() != IpsmgwSubscription.State.active
                && s.getStateEnum() != IpsmgwSubscription.State.subscribing
                && s.getStateEnum() != IpsmgwSubscription.State.resubscribing) {
            return false;
        }

        return !now.after(s.getExpires());
    }

    /**
     * send a new SUBSCRIBE to refresh and existing ACTIVE subscription NB: s
     * must be managed by JPA to be updated...
     *
     * @param s
     */
    private boolean reSubscribe(IpsmgwSubscription s) {
        Date now = new Date();
        log.debug("Refreshing subscription: [{}] in state [{}] which expires in [{}]", new Object[]{s.getPresentityUri(), s.getState(), (s.getExpires().getTime() - now.getTime()) / 1000});
        boolean success = subscribeToIMPU(s, false);
        if (success) {
            s.setStateEnum(IpsmgwSubscription.State.subscribing);
        }

        return success;
    }

    public void reInitialise() {
        log.debug("Re-initialising SIP Stack.... probably becuase of a fundamental property change");
        this.shutDown();
        this.initialise(emf);
    }

    @Override
    public void shutDown() {
        mustShutdown = true;
        //shut down sip stack
        try {
            PendingSubscriptionThread.shutDown();
            if (sipProvider != null) {
                sipProvider.removeSipListener(this);
                if (sipStack != null) {
                    sipStack.deleteSipProvider(sipProvider);
                }
            }
        } catch (ObjectInUseException ex) {/*stack always thorws this no matter what....*/

        } finally {
            if (sipStack != null) {
                sipStack.stop();
            }
        }
    }

    private String enqueueReceivedMessage(String fromAddress, String toAddress, byte[] message, byte dataCodingScheme, boolean submitOnCallback) throws InsufficientFundsException, Exception {
        String ret;
        log.debug("Enqueueing message, from: [{}], to [{}] with text [{}]", new Object[]{fromAddress, toAddress, message});
        BinaryShortMessage m = new BinaryShortMessage();
        m.setDestination(toAddress);
        m.setSource(fromAddress);
        m.setDataAsBinary(message);
        m.setDataCodingScheme(dataCodingScheme);
        m.setPriority(BaseMessage.Priority.MEDIUM);
        @SuppressWarnings("UseInjectionInsteadOfInstantion")
        MessageManager mm = new MessageManager();
        log.debug("Sending SIP SMS message to MM");
        // Anything coming in on the onnetplugin will be from a Smile UE
        ret = mm.sendShortMessageInternal(emf, m, "O", "0", submitOnCallback);
        log.debug("Successfully sent SIP SMS message to MM");
        return ret;
    }

    /*
     * Incoming SIP message from On-Net subscriber using SIP protocol. We should confirm that the request is a SIP MESSAGE
     * and that the P-Asserted-Identity header is populated. We should ideally also only accept SIP requests from our "known" S-CSCF IP addresses (TODO)
     * If the message is successfully queued, we send RP-ACK message back to the sender using our S-CSCF (most likely a loadbalancer, that will choose a particular S-CSCF)
     * 
     */
    @Override
    public void processRequest(RequestEvent re) {
        RequestEventExt reExt = (RequestEventExt) re;
        Request request = re.getRequest();
        SIPMessage message = (SIPMessage) request;
        PAssertedIdentity pai;
        String sipReason = null;
        short sipResponseCode = 200;
        boolean mustQueueMessage = true;
        boolean messageQueued = true;

        long start = System.currentTimeMillis();
        String messageRef = message.getCallId().getCallId() + message.getCSeq().getSeqNumber();

        try {
            log.debug("PROCESSING SIP REQUEST: [{}], From Tag: [{}], CSeq: [{}], CallID: [{}]", new Object[]{request.getMethod(), message.getCallId(), message.getFromTag(), message.getCSeq().getSeqNumber()});
            ServerTransaction serverTransaction = re.getServerTransaction();

            //        String fromSCSCFAddress = SipUtils.getSCSCFFromRoute(request);
            String paiAddress;

            /* We expect 3rd party registrations to be accpeted so MM can subscribe to the REG EVENT package of the IMPU in the To header. This serves to allow MM to know
             a) which S-CSCF the target IMPU is registered at
             b) the full implicit set of IMPUs (remember we could be asked to send a message to ANY one of the possible IMPUs).
             c) possibly know the type of SMS capable for reception on UE (contact header)
             */
            if (request.getMethod().equalsIgnoreCase(Request.REGISTER)) {
                try {
                    if (processSIPRegister(request)) {
                        sendSipResponse(serverTransaction, request, 200, "OK");
                    } else {
                        sendSipResponse(serverTransaction, request, 500, "Unable to process SIP 3rd party REG");
                    }
                    long millis = System.currentTimeMillis() - start;
                    log.debug("[{}] Message processing took [{}] milliseconds", request.getMethod(), millis);
                    return;
                } catch (Exception ex) {
                    log.warn("MMSIP: unable to send response for SIP REGISTER " + ex.getMessage());
                    log.warn("Error: ", ex);
                    return;
                }
            } else if (request.getMethod().equalsIgnoreCase(Request.OPTIONS)) {
                sendSipResponse(serverTransaction, request, 200, "OK");
                return;
            } else if (request.getMethod().equalsIgnoreCase(Request.NOTIFY)) {
                try {
                    if (processSIPNotify(request)) {
                        sendSipResponse(serverTransaction, request, 200, "OK");
                    } else {
                        sendSipResponse(serverTransaction, request, 500, "Unable to process SIP NOTIFY");
                    }
                    long millis = System.currentTimeMillis() - start;
                    log.debug("[{}] Message processing took [{}] milliseconds", request.getMethod(), millis);
                    return;
                } catch (Exception ex) {
                    log.warn("MMSIP: unable to send response for SIP REGISTER");
                    log.warn("Error: ", ex);
                    return;
                }
            }

            if (!request.getMethod().equalsIgnoreCase(Request.MESSAGE)) {
                try {
                    //we don't accept anything but SIP message requests
                    sendSipResponse(serverTransaction, request, 488, "Not acceptable here");
                    return;
                } catch (Exception ex) {
                    log.warn("MMSIP: unable to send response for invalid SIP request of type [{}]", request.getMethod());
                    log.warn("Error: ", ex);
                    return;
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("activeMessageTransaction size is [{}]", activeMessageTransaction.size());
            }

            if (activeMessageTransaction.containsKey(messageRef)) {
                log.warn("Already have an active transaction for MESSAGE [{}]..... ignoring", messageRef);
                return;
            } else {
                log.debug("About to put new entry in activemessage with key [{}] transaction and size is already [{}]", new Object[]{messageRef, activeMessageTransaction.size()});
                activeMessageTransaction.put(messageRef, "nothing");
            }
            sendSipResponse(serverTransaction, request, 100, "Trying");

            log.debug("MMSIP: Received SIP MESSAGE from IP: [{}] with R-URI [{}]", new Object[]{reExt.getRemoteIpAddress(), re.getRequest().getRequestURI()});

            /* get the P-Preferred-Identity as the sender of the message, if we can't get it we must not continue */
            try {
                pai = (PAssertedIdentity) request.getHeader("P-Asserted-Identity");
                paiAddress = pai.getAddress().getURI().toString();
                if (paiAddress == null || paiAddress.equalsIgnoreCase("")) {
                    sendSipResponse(serverTransaction, request, 488, "Not acceptable here");
                    activeMessageTransaction.remove(messageRef);
                    return;
                }
            } catch (Exception e) {
                log.error("MMSIP: P-Asserted-Identity corrupt or not available");
                try {
                    sendSipResponse(serverTransaction, request, 488, "Not acceptable here");
                } catch (Exception ex) {
                }
                activeMessageTransaction.remove(messageRef);
                return;
            }

            Header contentEncHeader = request.getHeader("Content-Type");
            log.debug("Content-Type header is [{}]", contentEncHeader);
            if (log.isDebugEnabled()) {
                log.debug("Message raw content is [{}]", Codec.binToHexString(message.getRawContent()));
                log.debug("Request raw content is [{}]", Codec.binToHexString(request.getRawContent()));
            }

            if (contentEncHeader.toString().contains("application/vnd.3gpp.sms")) {
                log.debug("This is a VoLTE message");
                try {
                    byte[] bytes = request.getRawContent();
                    log.debug("MMSIP: about to decode outer RP-Message structure");
                    RpMessage rpMessage = RpMessage.decode(bytes, RpMessage.Direction.incoming);
                    if (rpMessage instanceof RpDataMessage) {
                        log.debug("MMSIP: SIP payload is RP-DATA Message");
                        log.debug("MMSIP: Received SIP MESSAGE with embedded RP-DATA message: [{}]", rpMessage);
                        RpDataMessage rpDataMessage = (RpDataMessage) rpMessage;
                        log.debug("MMSIP: Embedded SMS Message: [{}]", rpDataMessage.getSmsMessage());
                        SmsMessage smsMessage = rpDataMessage.getSmsMessage();
                        log.debug("MMSIP: Embedded SMS Message has data coding scheme of [{}] and length of [{}] bytes",
                                new Object[]{smsMessage.getmDataCodingScheme(), smsMessage.getmUserData().length});
                        log.debug("MMSIP: MessageUDHI in payload?: " + smsMessage.getmUserDataHeaderIndicator());

                        String messageText = smsMessage.getMessageBody();
                        SmsHeader udh = smsMessage.getmUserDataHeader();
                        if (udh != null) {
                            mustQueueMessage = false;   //if this is multipart, we only send if we have all the parts

                            log.debug("We have a UDH which prob means we have a multipart SMS (>160... actually about 154 chars)");
                            log.debug("Message - Ref: " + udh.concatRef.refNumber + ", part: " + udh.concatRef.seqNumber + "/" + udh.concatRef.msgCount);

                            if (udh.concatRef.msgCount > 1) { //multipart double check ;)
                                String key = MultiPartSmsMessage.getKey(paiAddress, smsMessage.getmRecipientAddress().getAddressString(), udh.concatRef.refNumber);
                                log.debug("key is: [{}]", key);

                                if (log.isDebugEnabled()) {
                                    for (Member m : SmileSMSC.getHazelcastInstance().getCluster().getMembers()) {
                                        log.debug("Member exists at [{}]", m.getSocketAddress().getAddress().getHostAddress());
                                    }
                                }
                                IMap<String, MultiPartSmsMessage> multipartSMSMap = SmileSMSC.getHazelcastInstance().getMap("distributed-multipartsms-map");
                                log.debug("Getting lock on hazelcast map for key [{}]", key);
                                multipartSMSMap.lock(key);
                                log.debug("Got lock on hazelcast map for key [{}]", key);
                                try {
                                    MultiPartSmsMessage multiPartMessage = multipartSMSMap.get(key); //does message already exist?
                                    if (multiPartMessage == null) {
                                        //new MP message
                                        MultiPartSmsMessage newMPMessage = new MultiPartSmsMessage(udh.concatRef.seqNumber, udh.concatRef.msgCount);
                                        newMPMessage.setFrom(paiAddress);
                                        newMPMessage.setTo(smsMessage.getmRecipientAddress().getAddressString());
                                        newMPMessage.getParts().put(udh.concatRef.seqNumber, smsMessage.getMessageBody());
                                        multipartSMSMap.put(key, newMPMessage, BaseUtils.getIntProperty("env.hazelcast.config.onnet.ttl", 1200), TimeUnit.SECONDS);
                                        if (log.isDebugEnabled()) {
                                            log.debug("Added multipart message to Map. Size is now: [{}]", multipartSMSMap.size());
                                        }
                                        mustQueueMessage = false; //can't send half a message
                                    } else {
                                        multiPartMessage.getParts().put(udh.concatRef.seqNumber, smsMessage.getMessageBody());
                                        //now check if we have all the parts..... if we do, we can queue, otherwise we carry on waiting..
                                        if (multiPartMessage.hasAllParts()) {
                                            //send/enqueue message
                                            log.debug("Received all parts of multipart message. Going to enqueue message: [{}]", multiPartMessage.getCombinedMessageParts());
                                            messageText = multiPartMessage.getCombinedMessageParts();
                                            multipartSMSMap.remove(key);
                                            if (log.isDebugEnabled()) {
                                                log.debug("Removed multipart message and size is now: [{}]", multipartSMSMap.size());
                                            }
                                            mustQueueMessage = true;
                                        } else {
                                            log.debug("We dont have all of the parts of this message yet");
                                            multipartSMSMap.put(key, multiPartMessage, BaseUtils.getIntProperty("env.hazelcast.config.onnet.ttl", 1200), TimeUnit.SECONDS);
                                            //we don't have all the parts yet.
                                            mustQueueMessage = false;
                                        }
                                    }
                                } finally {
                                    log.debug("Unlocking hazelcast map for key [{}]", key);
                                    multipartSMSMap.unlock(key);
                                    log.debug("Unlocked hazelcast map for key [{}]", key);
                                }
                            }
                        }

                        String delayedSubmissionID = null;
                        if (mustQueueMessage) {
                            log.debug("Enqueueing message to engine");
                            try {
//                                enqueueReceivedMessage(paiAddress, smsMessage.getmRecipientAddress().getAddressString(), SMSCodec.encode(messageText, VOLTE_CODING_SCHEME), VOLTE_CODING_SCHEME);
                                byte encoding;
                                if (smsMessage.getmDataCodingScheme() == 8) {
                                    encoding = SMSCodec.NOSMPP_UCS_2_CODING_SCHEME;
                                    delayedSubmissionID = enqueueReceivedMessage(paiAddress, smsMessage.getmRecipientAddress().getAddressString(),
                                            SMSCodec.encode(messageText, SMSCodec.NOSMPP_UCS_2_CODING_SCHEME), encoding, true);
                                } else {
                                    encoding = (byte) BaseUtils.getIntProperty("env.mm.offnet.default.ascii.coding.scheme", SMSCodec.ASCII_CODING_SCHEME);
                                    delayedSubmissionID = enqueueReceivedMessage(paiAddress, smsMessage.getmRecipientAddress().getAddressString(),
                                            SMSCodec.encode(messageText, encoding), encoding, true);
                                }
                            } catch (InsufficientFundsException esf) {
                                //No funds available
                                log.debug("MMSIP: Can't send SMS as sender does not have enough funds");
                                sipResponseCode = RESP_CODE_NO_FUNDS;
                                sipReason = "Not enough credit";
                                messageQueued = false;
                            } catch (Exception e) {
                                log.error("MMSIP: Unknown error whilst trying to call engine to send message", e);
                                sipResponseCode = 500;
                                sipReason = "Engine error " + e.getLocalizedMessage();
                                messageQueued = false;
                            }
                        }

                        //acknowledge receipt of SIP message - this message is rather meaningless in the overall protocol
                        sendSipResponse(serverTransaction, request, sipResponseCode, sipReason);

                        /* all ok - check if we must send positive/negative response RP-ACK/RP-ERROR */
                        sendRpDataResponse(pai, message.getTopmostVia(), rpDataMessage, messageQueued, delayedSubmissionID);

                    } else if (rpMessage instanceof RpAckMessage) {
                        log.debug("MMSIP: Received Rp-ACK message and responding with 200OK to SIP MESSAGE");
                        sendSipResponse(serverTransaction, request, 200, null);
                        //TODO: find the message reference and update it's status as "delivered successfully"
                    } else {
                        log.warn("MMSIP: Received unknown RP-Message");
                    }
                } catch (Exception e) {
                    log.error("MMSIP: Error processing received SIP Message: " + e.getLocalizedMessage(), e);
                    //acknowledge receipt of SIP message - this message is rather meaningless in the overall protocol
                    try {
                        sendSipResponse(serverTransaction, request, sipResponseCode, sipReason);
                    } catch (Exception ex) {
                        log.error("unable to send SIP response", ex);
                    }
                }

                // END VOLTE MESSAGE
            } else {

                log.debug("This is an App message");
                //SIP SMS Message (not binary).
                PCalledPartyIDHeader pCalledPartyIDHeader = (PCalledPartyIDHeader) message.getHeader("P-Called-Party-ID");

                if (pCalledPartyIDHeader == null) {
                    try {
                        sendSipResponse(serverTransaction, request, 500, "no P-Called-Party");
                    } catch (Exception ex) {
                    }
                    return;
                }

                String recipientURIString = pCalledPartyIDHeader.getAddress().getURI().toString();

                if ((request.getRawContent() == null) || (request.getRawContent().length == 0)) {
                    log.error("Received SIP [{}] request with no content ([{}])- call-ID is [{}]... aborting", new Object[]{request.getMethod(), message.getContentTypeHeader().getContentType(), message.getCallId()});
                    try {
                        sendSipResponse(serverTransaction, request, 500, "bad message - empty");
                    } catch (Exception ex) {
                    }
                    return;
                }

                log.debug("Recipient String is [{}]", recipientURIString);
                try {
                    try {
                        enqueueReceivedMessage(paiAddress, recipientURIString, request.getRawContent(), APP_CODING_SCHEME, false);
                    } catch (InsufficientFundsException esf) {
                        //No funds available
                        log.debug("MMSIP: Can't send SMS as sender does not have enough funds");
                        sipResponseCode = RESP_CODE_NO_FUNDS;
                        sipReason = "Not enough credit";
                    } catch (Exception e) {
                        log.error("MMSIP: Unknown error whilst trying to call engine to send message", e);
                        sipResponseCode = 500;
                        sipReason = "Engine error " + e.getLocalizedMessage();
                    }
                    //acknowledge receipt of SIP message - this message is rather meaningless in the overall protocol
                    sendSipResponse(serverTransaction, request, sipResponseCode, sipReason);
                } catch (Exception ex) {
                    log.error("Failure receiving and queue SIP message (not binary) " + ex.getMessage());
                    log.warn("Error: ", ex);
                }
            }

            long millis = System.currentTimeMillis() - start;
            log.debug("[{}] Message processing took [{}] milliseconds", request.getMethod(), millis);
            activeMessageTransaction.remove(messageRef);
        } catch (Exception e) {
            log.warn("Error: ", e);
        } finally {
            BaseUtils.addStatisticSample("MM.processSIPMessage." + request.getMethod().toUpperCase(), BaseUtils.STATISTIC_TYPE.latency, System.currentTimeMillis() - start);
        }
    }

    /**
     *
     * @param ppi - PPI of original message - we use this in the response
     * message (viz. response is actually another MESSAGE request)
     * @param topmostVia - This is the Via of the server that we received the
     * original SIP MESSAGE request from, most likely the S-CSCF. We use this as
     * the next hop for the response MESSAGE request.
     * @param rpDataMessage - the message to which we are sending a response for
     * @param success - must we send an RP-ACK or an RP-ERROR
     */
    private void sendRpDataResponse(PAssertedIdentity paiHeader, Via topmostVia, RpDataMessage rpDataMessage, boolean success, String delayedSubmissionID) {
        SipURI to = null;
        RpMessage rpMessage;
        byte[] rpMessageBytes;

        if (success) {
            rpMessage = rpDataMessage.createAck();
            log.debug("MMSIP: RP-ACK: [{}]", rpMessage);

            //now create the TPDU - SMS-SUBMIT-REPORT
            SmsMessage smsSubmitReportMessage = new SmsMessage(SmsMessage.MessageDirection.OUTGOING, SmsMessage.MessageType.SMS_SUBMITREPORT, null, null);

            ((RpAckMessage) rpMessage).setSmsMessage(smsSubmitReportMessage);
            rpMessageBytes = ((RpAckMessage) rpMessage).serialise();
        } else {
            rpMessage = new RpErrorMessage();
            rpMessage.setMessageReference(rpDataMessage.getMessageReference());
            rpMessageBytes = ((RpErrorMessage) rpMessage).serialise();
        }

        try {
            try {
                if (paiHeader != null && paiHeader.getAddress().getURI().isSipURI()) {
                    to = (SipURI) paiHeader.getAddress().getURI();
                }
            } catch (Exception e) {
                log.error("MMSIP: PAI failed... aborting");
                return;
            }

            if (to == null) {
                log.error("MMSIP: Can't send RP SMS response without a valid To... aborting");
                return;
            }

            SipURI ruri = to;

            log.debug("MMSIP: Setting to URI to [{}]", ruri);

            /* assume we got the initial request from an S-CSCF, we should reply via the same one */
            String replyHost = topmostVia.getHost() + ":" + topmostVia.getPort();
            log.debug("MMSIP: replying with RP-ACK message via S-CSCF [{}]", replyHost);
            SipURI routeURI = addressFactory.createSipURI("orig", replyHost);
            routeURI.setLrParam();
            Address routeAddress = addressFactory.createAddress(routeURI);
            RouteHeader routeHeader = headerFactory.createRouteHeader(routeAddress);

            Address fromNameAddress = addressFactory.createAddress(myScUri);
            FromHeader fromHEader = headerFactory.createFromHeader(fromNameAddress, "ipsmgwtest");
            Address toNameAddress = addressFactory.createAddress(to);
            ToHeader toHeader = headerFactory.createToHeader(toNameAddress, null);
            ruri.setTransportParam("udp");
            ArrayList viaHeaders = new ArrayList();
            ViaHeader header = headerFactory.createViaHeader(ipAddressString, port, "udp", null);
            viaHeaders.add(header);
            CallIdHeader callIdHeader = sipProvider.getNewCallId();
            CSeqHeader cseqHeader = headerFactory.createCSeqHeader(1, Request.MESSAGE);
            MaxForwardsHeader mfHeader = headerFactory.createMaxForwardsHeader(70);

            Request newRequest = messageFactory.createRequest(ruri, Request.MESSAGE, callIdHeader, cseqHeader, fromHEader, toHeader, viaHeaders, mfHeader);
            newRequest.addHeader(routeHeader);

            ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("application", "vnd.3gpp.sms");
            newRequest.setContent((Object) rpMessageBytes, contentTypeHeader);

            List x = new ArrayList();
            x.add(sipUserAgent);
            UserAgentHeader userAgentHeader = headerFactory.createUserAgentHeader(x);
            newRequest.addHeader(userAgentHeader);

            ClientTransaction tx = sipProvider.getNewClientTransaction(newRequest);
            tx.setApplicationData(delayedSubmissionID);
            log.debug("MMSIP: sending SIP MESSAGE Request with tx branch: [{}] on Tx [{}]", tx.getBranchId(), tx);
            tx.sendRequest();
        } catch (Exception e) {
            log.error("MMSIP: failed to send RP message response [{}]", e.getLocalizedMessage());
        }
    }

    /**
     * Process a SIP response. This could be a response to a SUBSCRIBE, a
     * MESSAGE, etc. see code for details
     *
     * @param re
     */
    @Override
    public void processResponse(ResponseEvent re) {
        String branchId = "";
        OnnetSMSMessage smsMessage;
        SIPResponse response = (SIPResponse) re.getResponse();
        String callID = response.getCallId().getCallId();
        ClientTransaction tx = re.getClientTransaction();
        long subscriptionId;
        IpsmgwSubscription s = null;

        log.debug("MMSIP: Received a SIP response: [{}] [{}] for Call-ID [{}] with branch ID [{}]", new Object[]{response.getStatusCode(), response.getReasonPhrase(), callID, branchId});

        if (tx == null) {
            log.warn("MMSIP: ignoring response that is not part of an active transaction: CallID: [{}]", response.getCallId().getCallId());
            return;
        }
        branchId = tx.getBranchId();

        CSeqHeader cseq = (CSeqHeader) response.getHeader("CSeq");
        String responseMethod = cseq.getMethod();

        if (response.getStatusCode() == 100) {
            log.debug("Ignoring 100 response codes.... meaningless to us");
            return;
        }

        /*PROCESS SUBSCRIBE RESPONSE*/
        if (responseMethod.equalsIgnoreCase("SUBSCRIBE")) {
            EntityManager em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            try {
                log.debug("Received response to a SUBSCRIPTION...");
                subscriptionId = (Long) tx.getApplicationData();
                s = IpsmDAO.getLockedSubscription(em, subscriptionId);
                if (s == null) {
                    log.warn("We dont have a subscription for this [{}]. Ignoring", subscriptionId);
                    return;
                }
                if ((response.getStatusCode() > 199) && (response.getStatusCode() < 300)) {
                    //is this a response to re-subscribe or a response to an initial subscribe?
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.SECOND, response.getExpires().getExpires());
                    s.setExpires(cal.getTime());
                    log.debug("We are currently in state [{}]", s.getStateEnum());
                    if (s.getStateEnum() == IpsmgwSubscription.State.pending || s.getStateEnum() == IpsmgwSubscription.State.subscribing) { //if we are currently init/subscribing
                        s.setStateEnum(IpsmgwSubscription.State.active);
                        s.setDialogId(tx.getDialog().getDialogId());
                        s.setToTag(tx.getDialog().getRemoteTag());
                        s.setFromTag(tx.getDialog().getLocalTag());
                        s.resetRetries();
                        em.persist(s);
                        //the active list is ordered in asc order of expires epoch, so the subscriptions closest to expiry will be at the front.
                    } else if (s.getStateEnum() == IpsmgwSubscription.State.unsubscribing) {
                        em.remove(s);
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Got " + response.getStatusCode() + " response for SUBSCRIBE and expires is in " + (s.getExpires().getTime() - new Date().getTime()) / 1000 + " seconds");
                    }
                    log.debug("dialog id for confirmed subscription is " + tx.getDialog().getDialogId());
                } else {
                    //TODO: maybe we should re-init - do some retries?
                    log.error("Received bad response for SUBSCRIBE or UN-SUBSCRIBE [{}-{}] with Call-ID: [{}] in state [{}]", new Object[]{response.getStatusCode(), response.getStatusLine().getReasonPhrase(), response.getCallId().getCallId(), s.getState()});
                    em.remove(s);
                }
                em.flush();
            } catch (Exception ex) {
                log.error("Exception occurred trying to process response to SUBSCRIBE [{}]", ex.getMessage());
                log.warn("Error: ", ex);
            } finally {
                try {
                    Dialog dlg = tx.getDialog();
                    if (dlg != null) {
                        log.debug("dlg is not null. Deleting");
                        dlg.delete();
                    }
                } catch (Exception e) {
                    log.warn("Error cleaning up dialog", e);
                }
                JPAUtils.commitTransactionAndClose(em);
                if (s != null) {
                    removeFromPendingSubscriptionDialogs(s);
                    s.unlock();
                }
            }
            return;
        }

        //PROCESS RESPONSE TO MESSAGE
        if (responseMethod.equalsIgnoreCase("MESSAGE")) {
            String delayedSubmissionId;
            Object obj = re.getClientTransaction().getApplicationData();

            log.debug("Received a response on a MESSAGE transaction for Call-ID: [{}]", response.getCallId().getCallId());
            if (obj == null) {
                log.debug("MMSIP: no SMS message or delayedSubmissionID associated with this transaction... aborting");
            } else if (obj instanceof OnnetSMSMessage) {
                smsMessage = (OnnetSMSMessage) obj;
                processSMSDeliveryResponse(smsMessage, response, callID, branchId, 0);
            } else if (obj instanceof String) {
                delayedSubmissionId = (String) obj;
                processDelayedSubmissionResponse(delayedSubmissionId, response);
            }
        }
    }

    public void processDelayedSubmissionResponse(String delayedSubmissionId, SIPResponse response) {
        if (delayedSubmissionId == null || delayedSubmissionId.isEmpty()) {
            log.debug("no delayedSubmissionID received");
            return;
        }

        MessageManager mm = new MessageManager();
        try {
            if ((response != null) && (response.getStatusCode() > 199 && response.getStatusCode() < 300)) {
                log.debug("received positive response for delayed delivery submission on ID [{}]", delayedSubmissionId);
                mm.enqueuePendingMessage(emf, delayedSubmissionId);
            } else {
                log.debug("received negative response for delayed delivery submission on ID [{}]", delayedSubmissionId);
                mm.discardPendingMessage(delayedSubmissionId);
            }
        } catch (Exception ex) {
            log.error("Exception on pending message callback invocation [{}]", ex.getMessage());
            log.warn("Error: ", ex);
        }
    }

    public void processSMSDeliveryResponse(OnnetSMSMessage smsMessage, SIPResponse response, String callId, String branchId, long cseqNumber) {
        if (smsMessage == null) {
            log.debug("MMSIP: no SMS message or delayedSubmissionID associated with this transaction... aborting");
            return;
        }

        /* TODO: we need to change this to if we get an RP-ACK - not just a SIP MESSAGE success response */
        if ((response != null) && response.getStatusCode() > 199 && response.getStatusCode() < 300) {
            //We have a successfull send
            log.debug("MMSIP: Message Delivered Successfully to one of the registered contacts. "
                    + "Sending a positive async response to the delivery engine for Call-Id: [{}], Branch-Id [{}]", new Object[]{callId, branchId});
            HashSet<String> parts = smsMessage.contactParts.get(response.getCallId().getCallId());
            if (parts == null) {
                log.warn("We have received a response for MESSAGE we don't recall sending");
            } else {
                parts.remove(response.getCallId().getCallId());
                if (parts.size() > 0) {
                    log.debug("MMSIP: received successful response to part of a concatenated SMS and there are [{}] parts left", parts.size());
                    return;
                } else {
                    log.debug("MMSIP: all parts delivered successfully for at least one contact - processing callback");
                }
            }

            FinalDeliveryPluginCallBack callback = new FinalDeliveryPluginCallBack();
            callback.setCallbackId(smsMessage.getUuid());
            callback.setMustRetry(false);
            callback.setPluginClassName(OnnetSMSDeliveryPlugin.className);
            callback.setRetryTrigger(null);

            engine.processPluginResult(callback);

            if (!smsMessage.isSystemMessage() && sendDeliveryNotifications) {
                log.error("Sending delivery confirmation message");
                sendDeliveryConfirmation(smsMessage);
            }
        } else {
            //if we get here - we have a bad response - either an error or possibly user was offline
            if (response != null) {
                log.debug("MMSIP: Received a bad response to a SIP MESSAGE [{}-{}], Call-ID: [{}] - calling engine callback", new Object[]{response.getStatusCode(), response.getStatusLine().getReasonPhrase(), response.getCallId().getCallId()});
            } else {
                log.debug("MMSIP: Response is null, assuming timeout on SIP MESSAGE with Call-ID [{}] and CSeq [{}] - calling engine callback", new Object[]{callId, cseqNumber});
            }

            smsMessage.decrementPendingContacts();
            if (smsMessage.getPendingContacts() <= 0) {
                log.debug("no more pending contacts so we can process the result");
                FinalDeliveryPluginCallBack callback = new FinalDeliveryPluginCallBack();
                callback.setCallbackId(smsMessage.getUuid());
                callback.setMustRetry(true);
                callback.setPluginClassName(OnnetSMSDeliveryPlugin.className);
                RetryTrigger rt = new RetryTrigger();
                log.debug("MMSIP: Setting trigger for user [{}]", smsMessage.getTo());
                rt.setTriggerKey(smsMessage.getTo());
                rt.setTriggerType(RetryTrigger.SIP_REGISTER);
                callback.setRetryTrigger(rt);
                engine.processPluginResult(callback);
            } else {
                log.debug("There are still [{}] pending contacts - waiting for them to finish before processing result", smsMessage.getPendingContacts());
            }
        }
        if (response != null) {
            activeMessageTransaction.remove(response.getCallId().getCallId() + Long.toString(response.getCSeq().getSeqNumber()));
        } else {
            activeMessageTransaction.remove(callId + Long.toString(cseqNumber));
        }
    }

    @Override
    public void processTimeout(TimeoutEvent te) {
        long subscriptionId;
        IpsmgwSubscription s = null;
        try {
            if (te.isServerTransaction()) {
                return;
            }

            ClientTransaction tx = te.getClientTransaction();

            if (tx.getRequest().getMethod().equalsIgnoreCase("SUBSCRIBE")) {
                subscriptionId = (Long) tx.getApplicationData();
                if (subscriptionId < 0) {
                    log.error("Received timeout for SUBSCRIBE but can't get Subscription from Application Data...");
                } else {
                    EntityManager em = JPAUtils.getEM(emf);
                    try {
                        JPAUtils.beginTransaction(em);
                        s = IpsmDAO.getLockedSubscription(em, subscriptionId);
                        log.error("Timeout on SUBSCRIBE for IMPU [{}], transaction [{}], Call-ID: [{}.... setting state of subscription to failed]", new Object[]{s.getPresentityUri(), te.getClientTransaction().getBranchId(), s.getCallId()});
                        s.setStateEnum(IpsmgwSubscription.State.failed);
                        em.flush();
                    } finally {
                        JPAUtils.commitTransactionAndClose(em);
                        if (s != null) {
                            removeFromPendingPresentityURIs(s);
                            s.unlock();
                        }
                    }
                }
            } else if (tx.getRequest().getMethod().equalsIgnoreCase("MESSAGE")) {
                String delayedSubmissionId;
                Object obj = tx.getApplicationData();
                OnnetSMSMessage smsMessage;

                log.debug("Received a response on a MESSAGE transaction for Call-ID: [{}]", ((SIPRequest) tx.getRequest()).getCallId().getCallId());
                if (obj == null) {
                    log.debug("MMSIP: no SMS message or delayedSubmissionID associated with this transaction... aborting");
                } else if (obj instanceof OnnetSMSMessage) {
                    smsMessage = (OnnetSMSMessage) obj;
                    processSMSDeliveryResponse(smsMessage, null, ((SIPRequest) tx.getRequest()).getCallId().getCallId(), tx.getBranchId(), ((SIPRequest) tx.getRequest()).getCSeq().getSeqNumber());
                } else if (obj instanceof String) {
                    delayedSubmissionId = (String) obj;
                    processDelayedSubmissionResponse(delayedSubmissionId, null);
                }
            }
            log.info("Timeout on transaction [{}]", te.getClientTransaction().getBranchId());
        } catch (Exception ex) {
            log.warn("Error: ", ex);
        } finally {
            Dialog dlg = te.getClientTransaction().getDialog();
            if (dlg != null) {
                log.debug("dlg is not null. Deleting");
                dlg.delete();
            }
        }
    }

    @Override
    public void processIOException(IOExceptionEvent ioee) {
        log.error("MMSIP: IOException not implemented");
    }

    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent tte) {
        if (tte.isServerTransaction()) {
            SIPRequest request = (SIPRequest) tte.getServerTransaction().getRequest();
            log.debug("Server transaction terminated [{}]", tte.getServerTransaction().getBranchId());
            activeMessageTransaction.remove(request.getCallId().getCallId() + request.getCSeq().getSeqNumber());
            return;
        }

        log.debug("Client transaction terminated");

        ClientTransaction tx = tte.getClientTransaction();

        if (tx != null) {
            SIPRequest request = (SIPRequest) tx.getRequest();
            activeMessageTransaction.remove(request.getCallId().getCallId() + request.getCSeq().getSeqNumber());
        }
    }

    @Override
    public void processDialogTerminated(DialogTerminatedEvent dte) {
        log.debug("Dialog with dialog id: " + dte.getDialog().getDialogId() + " terminated");
    }

    /**
     * Send a SIP response
     *
     * @param serverTransactionId Transaction created during request
     * @param request initial request being replied to
     * @param responseCode SIP response code eg 200
     * @param reason SIP response reason eg OK - leave null for defaults
     * @throws Exception
     */
    private void sendSipResponse(ServerTransaction serverTransactionId, Request request, int responseCode, String reason) throws ParseException, SipException, InvalidArgumentException {
        /* we always send a 200OK if we get the message - we actually did get the message and parsed it correctly */
        log.debug("MMSIP: Sending SIP Response with code [{}] and reason [{}]", new Object[]{responseCode, reason});
        Response response = messageFactory.createResponse(responseCode, request);
        if (reason != null && !reason.isEmpty()) {
            response.setReasonPhrase(reason);
        }
        if (serverTransactionId != null) {
            serverTransactionId.sendResponse(messageFactory.createResponse(responseCode, request));
        } else {
            sipProvider.sendResponse(response);
        }
    }

    public static String getMyScUri() {
        return myScUri;
    }

    public static String getMyScE164() {
        return myScE164;
    }

    @Override
    public void propertiesChanged() {
        String _myScUri = BaseUtils.getProperty("env.mm.ipsmgw.servicecenter.uri", "sip:+0@notset.nodomain");   //SC URI like "sip:+255662000000@tz.smilecoms.com
        String _myScE164 = BaseUtils.getProperty("env.mm.ipsmgw.servicecenter.e164", "0");                      //SC number in E.164 like "255662000000
        String _mySipDomain = BaseUtils.getProperty("env.sip.domain", "smilecoms.com");
        VOLTE_CODING_SCHEME = (byte) BaseUtils.getIntProperty("env.mm.volte.coding.scheme", VOLTE_CODING_SCHEME);
        APP_CODING_SCHEME = (byte) BaseUtils.getIntProperty("env.mm.app.coding.scheme", APP_CODING_SCHEME);
        resubscriptionThreshold1 = BaseUtils.getIntProperty("env.mm.ipsmgw.resubscriptionthreshold1seconds", (20 * 60)); //20 minutes by default
        resubscriptionThreshold2 = BaseUtils.getIntProperty("env.mm.ipsmgw.resubscriptionthreshold2seconds", (10 * 60)); //10 minutes by default
        sendDeliveryNotifications = Boolean.valueOf(BaseUtils.getProperty("env.mm.ipsmgw.senddeliverynotifications", "false"));
        otaTest = BaseUtils.getProperty("env.mm.ipsmgw.otatest", "false");
        otaString = BaseUtils.getProperty("env.mm.ipsmgw.otastring", "");

        //Only restart SIP stack if vital props change....
        if (_myScUri.equals(myScUri)
                && _myScE164.equals(myScE164)
                && _mySipDomain.equals(mySipDomain)) {
        } else {
            myScUri = BaseUtils.getProperty("env.mm.ipsmgw.servicecenter.uri", "sip:+0@notset.nodomain");   //SC URI like "sip:+255662000000@tz.smilecoms.com
            myScE164 = BaseUtils.getProperty("env.mm.ipsmgw.servicecenter.e164", "0");
            ipAddressString = BaseUtils.getIPAddress();   //"10.24.0.65"
            mySipDomain = BaseUtils.getProperty("env.sip.domain", "smilecoms.com");
            reInitialise();
        }
    }

    /**
     * Add a client transaction created to send an RP-DATA message over SIP to a
     * remote VoLTE UE all transactions in this list will be active transactions
     * that have not yet received a final response
     *
     * @param tx - the new transaction to add to the list
     */
//    private void addtoActiveTransactions(ClientTransaction tx) {
//        if (activeOnnetDeliveryTransactions.containsKey(tx.getBranchId())) {
//            log.error("MMSIP: Trying to add new active SMS transaction which already exists: [{}]", tx.getBranchId());
//            return;
//        }
//        activeOnnetDeliveryTransactions.put(tx.getBranchId(), tx);
//    }
//    
//    private void removeFromActiveTransactions(ClientTransaction tx) {
//        activeOnnetDeliveryTransactions.remove(tx.getBranchId());
//    }
    /**
     * REGISTER comes in for both REG, RE-REG, and DE-REG - and we need to
     * SUBSCRIBE to the AOR of we haven't already... Basically what we do here
     * is check for an existin subscription based on the IMPU (to header of
     * REGISTER). If we do, we check it's state - if it is anything other than
     * ACTIVE or SUBSCRIBING (TODO check this....) then we reset the
     * subscription to re-subscribe (by putting it on the pending list and
     * setting it's state to init). If there is no Subscription, we create a new
     * one, add it to the queue of pending subscriptions to be subscribed to.
     *
     * @param request
     * @return
     */
    private boolean processSIPRegister(Request request) {
        if (log.isDebugEnabled()) {
            log.debug("In processSIPRegister. activeRegistrations size is [{}]", activeRegistrations.size());
        }
        boolean ret = true;
        ToHeader toHeader = (ToHeader) request.getHeader("To");
        ContactHeader contactHeader = (ContactHeader) request.getHeader("Contact");
        String contactAddress = contactHeader.getAddress().getURI().toString();
        String presentityUri = toHeader.getAddress().getURI().toString();
        int contactExpires = contactHeader.getExpires();
        ExpiresHeader expiresHeader = (ExpiresHeader) request.getExpires();
        int expires = 0;
        CSeqHeader cseqHeader = (CSeqHeader) request.getHeader("CSeq");
        SIPMessage sipMessage = (SIPMessage) request;
        String callId = sipMessage.getCallId().getCallId();

        //reg or de-reg?
        if (expiresHeader != null) {
            expires = expiresHeader.getExpires();
        }
        log.debug("expires is [{}] and contactexpires is [{}]", new Object[]{expires, contactExpires});
        if (expires <= 0 && (contactExpires <= 0)) {
            log.debug("Received De-REGISTER.... ignoring as we will wait for notify");
            return true;
        }

        //make sure we don't double process multiple registrations for the same IMPU
        if (activeRegistrations.containsKey(presentityUri)) {
            log.warn("Still processing REGISTER for this IMPU - will ignore...", presentityUri);
            return true;
        } else {
            log.debug("About to put in active registration entry and size is already [{}]", activeRegistrations.size());
            activeRegistrations.put(presentityUri, 1);
        }

        //get the path header
        PathHeader pHdr;
        ListIterator x = request.getHeaders("Path");
        PathList pathList = new PathList();
        while (x.hasNext()) {
            pHdr = (PathHeader) x.next();
            Path newPath = new Path();
            newPath.setAddress(pHdr.getAddress());
            pathList.add(newPath);
        }
        log.debug("Received REGISTER request from SCSCF: [{}] for IMPU: [{}] and PATH: [{}] and CSEQ: [{}]", new Object[]{contactAddress, presentityUri, pathList.getValue(), cseqHeader.getSeqNumber()});

        EntityManager em = JPAUtils.getEM(emf);

        JPAUtils.beginTransaction(em);

        IpsmgwSubscription s = null;

        try {

            try {
                s = IpsmDAO.getLockedSubscription(em, presentityUri, myWatcherURI, contactHeader.getAddress().getURI().toString(), callId);
            } catch (Exception ex) {
                log.error("Failed to get/create subscription");
                JPAUtils.rollbackTransaction(em);
                JPAUtils.closeEM(em);
                activeRegistrations.remove(presentityUri);
                log.warn("Error: ", ex);
                return false;
            }

            boolean addToPending = false;

            if (s.getStateEnum() == IpsmgwSubscription.State.init) {
                //new subscription
                log.debug("Have a new subscription request for IMPU [{}]", presentityUri);
                addToPending = true;
                s.setStateEnum(IpsmgwSubscription.State.pending);
            } else {
                log.debug("We have a subscription already for the presentity [{}] in this REGISTER..... currently in state [{}]", presentityUri, s.getStateEnum().name());
                if ((s.getStateEnum() != IpsmgwSubscription.State.active)) {// && (s.getStateEnum() != IpsmgwSubscription.State.subscribing)) {
                    log.debug("Existing subscription is not active...");
                    log.debug("existing subscription has failed and we have just received a register, so we will try and resubscribe");
                    s.reset();
                    s.setStateEnum(IpsmgwSubscription.State.pending);
                    log.debug("Clear out this subscription from pending queues - in case it got in a bad state this will reset everything");
                    removeFromPendingSubscriptionDialogs(s);
                    addToPending = true;
                } else {
                    log.debug("resubscribing after register... staying in sync with SUBSCRIPTION for Call-ID [{}] and IMPU [{}]", new Object[]{s.getCallId(), s.getPresentityUri()});
//               updateSubscription(s);
                    if (reSubscribe(s)) {
                        addToPending = true;
                        log.debug("resubscription successfull");
                    } else {
                        log.debug("Failed to send resubscribe");
                    }
                }
            }
            JPAUtils.persistAndFlush(em, s);
            JPAUtils.commitTransactionAndClose(em);

            //we delay the sending of subscription until we have put the subscription in the DB. This is to make sure we know about the subscription in case the NOTIFY beats the ack (200OK) of the subscribe
            if (addToPending) {
                addToPendingSubscriptionDialogs(s);
            }

            activeRegistrations.remove(presentityUri);
            if (log.isDebugEnabled()) {
                log.debug("Finished processSIPRegister. activeRegistrations size is [{}]", activeRegistrations.size());
            }
        } finally {
            if (s != null) {
                s.unlock();
            }
        }
        return ret;
    }

    public static boolean subscribeToIMPU(IpsmgwSubscription subscription, boolean isUnsubscribe) {
        boolean ret = true;
        log.debug("SUBSCRIBING to IMPU [{}] currently in state [{}] - unsubscribe: [{}]", new Object[]{subscription.getPresentityUri(), subscription.getState(), isUnsubscribe});

        try {
            Address from = addressFactory.createAddress(subscription.getScscfUri());
            Address to = addressFactory.createAddress(subscription.getPresentityUri());
            String host = BaseUtils.getIPAddress();
            FromHeader fromHeader = headerFactory.createFromHeader(addressFactory.createAddress("sip:" + host), "12345");
            ToHeader toHeader = headerFactory.createToHeader(to, (subscription.getToTag() != null) ? subscription.getToTag() : null);

            // create Request URI
            URI requestURI = to.getURI();

            Address nextHopAddress = addressFactory.createAddress(from.getURI().toString() + ";lr");
            log.debug("Sending subscribe to next hop [{}]", nextHopAddress.getURI());
            RouteHeader scscfRouteHeader = headerFactory.createRouteHeader(nextHopAddress);
            // Create ViaHeaders
            ArrayList viaHeaders = new ArrayList();
            String ipAddress = udpListeningPoint.getIPAddress();
            ViaHeader viaHeader = headerFactory.createViaHeader(ipAddress, sipProvider.getListeningPoint(transport).getPort(), transport, null);
            viaHeaders.add(viaHeader);
            // Create a new Cseq header
            CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(subscription.getNextCSeq(), Request.SUBSCRIBE);
            // Create a new MaxForwardsHeader
            MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);
            // Create a new CallId header
            CallIdHeader callIdHeader = sipProvider.getNewCallId();
            if (subscription.getCallId() != null) {
                callIdHeader.setCallId(subscription.getCallId());
            } else {
                subscription.setCallId(callIdHeader.getCallId());
            }

            Request request = messageFactory.createRequest(requestURI, Request.SUBSCRIBE, callIdHeader, cSeqHeader, fromHeader, toHeader, viaHeaders, maxForwards);

            request.addHeader(scscfRouteHeader);
            SipURI contactURI = addressFactory.createSipURI(null, host);
            contactURI.setPort(sipProvider.getListeningPoint(transport).getPort());

            Address contactAddress = addressFactory.createAddress(contactURI);

            // Add the contact address
            contactAddress.setDisplayName("IPSMGW");

            ContactHeader contactHeader = headerFactory.createContactHeader(contactAddress);
            request.addHeader(contactHeader);

            EventHeader regHeader = headerFactory.createEventHeader("reg");
            request.addHeader(regHeader);

            if (isUnsubscribe) {
                ExpiresHeader expiresHeader = headerFactory.createExpiresHeader(0);
                request.addHeader(expiresHeader);
            }

            List x = new ArrayList();
            x.add(sipUserAgent);
            UserAgentHeader userAgentHeader = headerFactory.createUserAgentHeader(x);
            request.addHeader(userAgentHeader);

            ContentLengthHeader contentLengthHeader = headerFactory.createContentLengthHeader(0);
            request.setContentLength(contentLengthHeader);

            log.debug("MMSIP: Sending SIP SUBSCRIBE with IMPU: [{}], callid: [{}}, cseq: [{}]", new Object[]{subscription.getPresentityUri(), callIdHeader.getCallId(), cSeqHeader.getSeqNumber()});

            ClientTransaction tx = sipProvider.getNewClientTransaction(request);
            subscription.setFromTag(tx.getDialog().getLocalTag());     //TODO: use UUID for from tag (local)
            tx.setApplicationData(subscription.getId());
            tx.sendRequest();
            log.debug("Sent on Tx [{}]", tx);
        } catch (Exception ex) {
            log.error("Failed to send SUBSCRIBE for IMPU [{}]", subscription.getPresentityUri());
            log.warn("Error: ", ex);
            ret = false;
        }

        return ret;
    }

    private boolean processSIPNotify(Request request) {
        boolean ret = false;
        
        CallIdHeader callidHeader = (CallIdHeader) request.getHeader("Call-Id");
        FromHeader fromHeader = (FromHeader) request.getHeader("From");
        ToHeader toHeader = (ToHeader) request.getHeader("To");
        String callID = callidHeader.getCallId();
        String fromTag = fromHeader.getTag();
        String toTag = toHeader.getTag();
        
        if (!BaseUtils.getBooleanProperty("env.im.write.imsuser.state.registered.events", true)) {
            //Pretend everything is well whilst trouble-shooting the flood of NOTIFYs
            return true;
        }
        
        IpsmgwSubscription s = null;
        boolean addToPending = false;

        log.debug("Received NOTIFY with presentity [{}], call-id: [{}], from-tag: [{}], to-tag: [{}]", new Object[]{fromHeader.getAddress().getURI(), callID, fromTag, toTag});
        String xml = new String(request.getRawContent());

        EntityManager em = JPAUtils.getEM(emf);
        StringReader reader = null;
        try {
            JPAUtils.beginTransaction(em);
            try {
                s = IpsmDAO.getLockedSubscription(em, fromHeader.getAddress().getURI().toString(), callID); //we do mind if others read the subscription
            } catch (Exception e) {
                if (e.toString().contains("Deadlock")) {
                    // Can get deadlock if the combination of presentity and call id is not unique and another thread in another JVM tries to do work on the subscription
                    // Does not hapen often enough to try and globally lock. Rather just retry
                    log.debug("Got deadlock. Going to try again");
                } else {
                    throw e;
                }
            }
            if (s == null) {
                log.debug("No subscription yet. Waiting 20ms");
                Utils.sleep(20);
                JPAUtils.restartTransaction(em);
                s = IpsmDAO.getLockedSubscription(em, fromHeader.getAddress().getURI().toString(), callID); //we do mind if others read the subscription    
            }
            if (s != null) {
                log.debug("Found subscription, updating RegInfo");
                //TODO - maybe just put the subsciprtion in early - even before 200OK so we can process the notify before the 200.

                Reginfo reginfo;

                JAXBContext jaxbContext = JAXBContext.newInstance(Reginfo.class);
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                reader = new StringReader(xml);
                reginfo = (Reginfo) unmarshaller.unmarshal(reader);
                log.debug("REGINFO object parsed:[{}] ", reginfo.getState());

                processReginfo(em, s, reginfo);
                log.debug("Persisting subscription which now has [{}] IMPUs", s.getIpsmgwImpuCollection().size());
                //if there are no more IMPUs associated with this subscription we may as well unsubscribe
                if (s.getIpsmgwImpuCollection().isEmpty()) {
                    log.debug("No more IMPUs associated to this subscription [{}] - will unscubscribe", s.getPresentityUri());
                    s.setStateEnum(IpsmgwSubscription.State.unsubscribe);
                    addToPending = true;
                    em.persist(s);
                }
                ret = true;
            } else {
                log.warn("Received NOTIFY but we don't have an active subscription..... did we beat the 200OK? (call-id: [{}], from-tag: [{}], to-tag: [{}]", new Object[]{callID, fromTag, toTag});
            }
            JPAUtils.commitTransaction(em);
            if (addToPending) {
                addToPendingSubscriptionDialogs(s);
            }
        } catch (Exception ex) {
            JPAUtils.rollbackTransaction(em);
            log.error("Failed to process SIP NOTIFY [{}] [{}]", xml, ex.toString(), ex);
        } finally {
            if (s != null) {
                s.unlock();
            }
            if (reader != null) {
                reader.close();
            }
            JPAUtils.closeEM(em);
        }

        return ret;
    }

    public static void removeFromPendingPresentityURIs(IpsmgwSubscription newSubscription) {
        log.debug("In removeFromPendingSubscriptionDialogs for [{}][{}]", newSubscription.getId(), newSubscription.getPresentityUri());
        synchronized (pendingSubscriptionDialogs) {
            pendingPresentityURIs.remove(newSubscription.getPresentityUri());
        }
    }

    public static void removeFromPendingSubscriptionDialogs(IpsmgwSubscription newSubscription) {
        log.debug("In removeFromPendingSubscriptionDialogs for [{}][{}]", newSubscription.getId(), newSubscription.getPresentityUri());
        synchronized (pendingSubscriptionDialogs) {
            if (pendingPresentityURIs.contains(newSubscription.getPresentityUri())) {
                pendingSubscriptionDialogs.remove(newSubscription.getId());
                removeFromPendingPresentityURIs(newSubscription);
            }
        }
    }

    public static void addToPendingSubscriptionDialogs(IpsmgwSubscription newSubscription) {
        //only add if a pending subscription does not already exist
        synchronized (pendingSubscriptionDialogs) {
            if (pendingSubscriptionDialogs.contains(newSubscription.getId())) {
                log.warn("Subscription already in pending subscription list IMPU: [{}], Call-ID: [{}] - not adding again", newSubscription.getPresentityUri(), newSubscription.getCallId());
                return;
            }

            if (pendingPresentityURIs.contains(newSubscription.getPresentityUri())) {
                log.warn("Not going to add a subscription to the pending list that already exists IMPU: [{}], Call-ID [{}]", newSubscription.getPresentityUri(), newSubscription.getCallId());
                return;
            }
            try {
                pendingPresentityURIs.add(newSubscription.getPresentityUri());
                Boolean added = pendingSubscriptionDialogs.offer(newSubscription.getId(), 1000, TimeUnit.MILLISECONDS);
                if (!added) {
                    log.error("Failed to add subscription to pending queue, IMPU: [{}], Call-ID: [{}], state [{}]", new Object[]{newSubscription.getPresentityUri(), newSubscription.getCallId(), newSubscription.getState()});
                    BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "MM OnnetSMS", "pendingSubscriptionDialogs queue is full. Size is " + pendingSubscriptionDialogs.size());
                    pendingPresentityURIs.remove(newSubscription.getPresentityUri());
                } else {
                    log.debug("Added subscription to pending subscription queue [{}] which is in state [{}] with ID [{}]", new Object[]{newSubscription.getPresentityUri(), newSubscription.getState(), newSubscription.getId()});
                }
                log.debug("Subscription queue has [{}] entries and pendingPresentityURIs has [{}]", pendingSubscriptionDialogs.size(), pendingPresentityURIs.size());
            } catch (InterruptedException ex) {
                log.error("Failed to add subscription to pending queue, IMPU: [{}], Call-ID: [{}], state [{}], Exception [{}]", new Object[]{newSubscription.getPresentityUri(), newSubscription.getCallId(), newSubscription.getState(), ex.getMessage()});
                log.warn("Error: ", ex);
            }
        }
    }

    private ImpuContact buildContactfromDB(IpsmgwContact dbContact) {
        ImpuContact contact = null;
        PathList pathList = new PathList();
        try {
            contact = new ImpuContact(dbContact.getUri(), ImpuContact.SmsFormat.valueOf(dbContact.getSmsFormat()));

            AddressParser addressParser = new AddressParser(dbContact.getPath());
            AddressImpl address;
            while ((address = addressParser.address(true)) != null) {
                pathList.add(new Path(address));
            }
        } catch (ParseException ex) {
            //this is not really an error (parse eventually fails after processing all the comma-separated addresses
//            log.error("failed to parse SIP Path for contact [{}], path: [{}]", dbContact.getUri(), dbContact.getPath());
//            log.warn("Error: ", ex);
        }
        contact.setPathList(pathList);

        return contact;

    }

    /**
     * process REginfo from a NOTFY.
     *
     * @param em - current EntityMManager associated with S
     * @param s - Subscription on which this NOTIFY was received (already
     * locked)
     * @param regInfo - parsed reginfo object
     */
    private void processReginfo(EntityManager em, IpsmgwSubscription s, Reginfo regInfo) {
        /* reginfo XML looks like this....
         * <reginfo xmlns="urn:ietf:params:xml:ns:reginfo" version="2" state="full">
         <registration aor="sip:+27830011177@tz.smilecoms.com" id="0x7f2c344e2a48" state="terminated">
         </registration>
         <registration aor="tel:+27830011177" id="0x7f2c344e4128" state="terminated">
         </registration>
         </reginfo>
         */
        boolean isFull = false;
        Registration reg;

        if (regInfo.getState().equalsIgnoreCase("full")) {
            log.debug("Doing full update on subscription NOTIFY for presentity [{}]", s.getPresentityUri());
            isFull = true;
            //make sure we have subscribed to all implicits
        } else {
            log.debug("Doing partial update on subscription NOTIFY for presentity [{}]", s.getPresentityUri());
        }

        Iterator<Registration> it = regInfo.getRegistration().iterator();
        while (it.hasNext()) {
            reg = it.next();
            log.debug("Registration: [{}] in state [{}]", reg.getAor(), reg.getState());
            updateImpuRegstate(em, reg, s, isFull);
        }

    }

    /**
     * update the registration of an IMPU based on reginfo data from NOTIFY
     *
     * @param reg - reginfo data for the IMPU
     * @param s - subscription to which the IMPU is associated
     */
    private void updateImpuRegstate(EntityManager em, Registration reg, IpsmgwSubscription s, boolean full) {
        ArrayList<IpsmgwContact> contactsAdded = new ArrayList<>();
        log.debug("updating state for IMPU [{}] in state [{}]", new Object[]{reg.getAor(), reg.getState()});

        IpsmgwImpu impu = s.getAssociatedImpu(em, reg.getAor());
        if (impu == null) {
            log.debug("can't find IMPU [{}] for subscription presentity [{}]", reg.getAor(), s.getPresentityUri());
            if (reg.getState().equalsIgnoreCase("active")) {
                log.debug("state for non-existant IMPU is active so addding...");
                impu = IpsmDAO.getImpuFromDBCreateIfMissing(em, reg.getAor());
                IpsmDAO.addImpuToSubscription(em, s.getId(), impu.getId());
            }
        }

        if (!reg.getState().equalsIgnoreCase("active")) {
            log.debug("Reg state for impu [{}] is not active: [{}] - removing from subscription presentity [{}]]", new Object[]{reg.getAor(), reg.getState(), s.getPresentityUri()});
            IpsmDAO.removeImpusFromSubscription(em, s); //if one IMPU has been terminated we assume the entire implicit set is too
            s.getIpsmgwImpuCollection().clear();
            em.flush();
        } else {
            //IMPU is active - update it and it's respective contacts
            for (Contact c : reg.getContact()) {
                log.debug("Reg state for contact [{}] is [{}]", new Object[]{c.getUri(), c.getState()});
                if (c.getState().equalsIgnoreCase("terminated")) {
                    log.debug("We have a terminated contact....");
                    IpsmgwContact newContact = IpsmDAO.getContact(em, c.getUri(), false);
                    if (newContact != null) {
                        log.debug("contact [{}] needs to be removed from the DB", newContact.getUri());
                        Boolean test = impu.getIpsmgwContactCollection().remove(newContact);
                        log.debug("removed contact [{}]", test);
                        newContact.getIpsmgwImpuCollection().clear();        //because of implicits, if this contact is termianted, lets remove it from all IMPUs - test = newContact.getIpsmgwImpuCollection().remove(impu);
                        log.debug("removed impu from contact [{}]", test);
                        em.flush();
                        if (newContact.getIpsmgwImpuCollection().size() <= 0) {
                            log.debug("contact [{}] has no more IMPUs associated.... removing", newContact.getUri());
                            em.remove(newContact);
                            em.flush();
                        } else {
                            log.debug("contact [{}] still has [{}] IMPUs associated... keeping", new Object[]{newContact.getUri(), newContact.getIpsmgwImpuCollection().size()});
                        }
                    }
                } else {
                    IpsmgwContact newContact = IpsmDAO.getContact(em, c.getUri(), true);
                    newContact.setSmsFormat(OnnetSMSPluginUtils.getSmsFormat(c).name());
                    newContact.setPath(s.getScscfUri() + ";lr");
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.SECOND, c.getExpires().intValue());
                    newContact.setExpires(cal.getTime());
                    newContact.addImpuIfDoesntExist(impu);
                    try {
                        em.flush();
                    } catch (Exception ex) {
                        if (!ex.toString().contains("Duplicate entry")) {
                            log.error("Failed to flush to DB... [{}] - [{}]", new Object[]{ex.getClass(), ex.getMessage()});
                            try {
                                throw (ex);
                            } catch (Exception e) {
                            }
                        }
                    }
                    contactsAdded.add(newContact);
                }
            }
            if (full) {
                //remove any old contacts
                for (IpsmgwContact c : impu.getIpsmgwContactCollection()) {
                    boolean found = false;
                    for (IpsmgwContact added : contactsAdded) {
                        if (c.equals(added)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        c.getIpsmgwImpuCollection().remove(impu);
                        em.remove(c);
                        em.flush();
                    }
                }
            }
        }
    }

    private void sendDeliveryConfirmation(OnnetSMSMessage smsMessage) {
        log.error("Sending delivery confirmation to [{}]", smsMessage.getFrom());
        String tmp = OnnetSMSPluginUtils.getUriStringNoParams(smsMessage.getTo());
        smsMessage.setTo(smsMessage.getFrom());
        smsMessage.setFrom(getMyScUri());
        smsMessage.setIsSystemMessage(true);
        smsMessage.setCodingScheme((byte) 0xF0);
        try {
            smsMessage.setMessage(SMSCodec.encode("Your message with ID:" + smsMessage.getMessageId() + " to " + tmp + " has been successfully received by the recipient", smsMessage.getCodingScheme()));
            smsMessage.getDeliveryContext().lock();
            try {
                sendMessageOverSIP(smsMessage);
            } finally {
                smsMessage.getDeliveryContext().unlock();
            }
        } catch (UnsupportedEncodingException ex) {
            log.warn("Error", ex);
        }
    }

    @Override
    public void sendDeliveryReport(DeliveryEngine.DeliveryReportStatus reportStatus, HashMap<String, Serializable> deliveryReportData) {
        log.debug("In sendDeliveryReport");
    }
}
