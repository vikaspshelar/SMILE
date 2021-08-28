/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.plugins.offnetsms.smsc;

import com.cloudhopper.commons.gsm.GsmUtil;
import com.cloudhopper.commons.util.DateTimeUtil;
import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.BaseSm;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.pdu.EnquireLinkResp;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.tlv.Tlv;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.cloudhopper.smpp.util.DeliveryReceipt;
import com.cloudhopper.smpp.util.SmppUtil;
import com.hazelcast.core.IMap;
import com.hazelcast.core.Member;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.Codec;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.mm.engine.DeliveryEngine;
import com.smilecoms.mm.engine.FinalDeliveryPluginCallBack;
import com.smilecoms.mm.plugins.offnetsms.OffnetSMSDeliveryPlugin;
import com.smilecoms.mm.plugins.offnetsms.OffnetSMSMessage;
import com.smilecoms.mm.plugins.offnetsms.smsc.pdu.OffnetBaseSm;
import com.smilecoms.mm.sms.PduBitPacker;
import com.smilecoms.mm.utils.SMSCodec;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class RemoteSMSC {

    private InterconnectSMSC config;
    private SMSCConnection connection;
    private final Map<String, SmppServerSession> serverSessions = new HashMap<>();
    private static final Logger log = LoggerFactory.getLogger(RemoteSMSC.class);
    private static final Map<Integer, RemoteSMSC> remoteSMSCsById = new HashMap();
    private static final Map<String, RemoteSMSC> remoteSMSCsBySystemId = new HashMap();
    private static final Map<String, RemoteSMSC> remoteSMSCsByTrunkId = new HashMap();
    private static final Map<String, RemoteSMSC> remoteSMSCsByUUID = new HashMap();
    // Ensure we dont create and remove connections untidily
    private static final Lock lock = new ReentrantLock();
    private int interconnectSMSCBindId;
    private static int messageId = 1;
    private long lastMessageSent = 0;
    private final String uuid = Utils.getUUID();

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

    public int getInterconnectSMSCBindId() {
        return interconnectSMSCBindId;
    }

    public void setInterconnectSMSCBindId(int interconnectSMSCBindId) {
        this.interconnectSMSCBindId = interconnectSMSCBindId;
    }

    static RemoteSMSC getOrCreateRemoteSMSCWithConfig(InterconnectSMSC config) throws Exception {
        lock.lock();
        try {
            RemoteSMSC remoteSmsc = remoteSMSCsById.get(config.getInterconnectSmscId());
            if (remoteSmsc == null) {
                remoteSmsc = new RemoteSMSC(config);
                remoteSMSCsById.put(config.getInterconnectSmscId(), remoteSmsc);
                remoteSMSCsBySystemId.put(config.getSystemId(), remoteSmsc);
                remoteSMSCsByTrunkId.put(config.getInternalTrunkId(), remoteSmsc);
                remoteSMSCsByUUID.put(remoteSmsc.uuid, remoteSmsc);
            } else {
                log.debug("Found a remote SMSC already created. Checking if the config has changed");
                if (!config.equals(remoteSmsc.config)) {
                    log.debug("Remote SMSC configuration has changed, Going to rebind");
                    remoteSmsc.config = config;
                    if (config.getClientBind() == 1) {
                        remoteSmsc.bind();
                    }
                    remoteSMSCsById.put(config.getInterconnectSmscId(), remoteSmsc);
                    remoteSMSCsBySystemId.put(config.getSystemId(), remoteSmsc);
                    remoteSMSCsByTrunkId.put(config.getInternalTrunkId(), remoteSmsc);
                    remoteSMSCsByUUID.put(remoteSmsc.uuid, remoteSmsc);
                } else {
                    log.debug("Config has not changed");
                }
            }
            log.debug("Returning remote SMSC instance with system Id[{}] host[{}] and port [{}] and UUID [{}]", new Object[]{remoteSmsc.config.getSystemId(), remoteSmsc.config.getHost(), remoteSmsc.config.getPort(), remoteSmsc.uuid});
            return remoteSmsc;
        } finally {
            lock.unlock();
        }

    }

    static RemoteSMSC getRemoteSMSCWithConfig(InterconnectSMSC config) throws Exception {
        log.debug("In getRemoteSMSCWithConfig [{}]", config);
        lock.lock();
        try {
            return remoteSMSCsById.get(config.getInterconnectSmscId());
        } finally {
            lock.unlock();
        }
    }

    static RemoteSMSC getRemoteSMSCBySystemId(String systemId) throws Exception {
        log.debug("In getRemoteSMSCBySystemId [{}]", systemId);
        lock.lock();
        try {
            return remoteSMSCsBySystemId.get(systemId);
        } finally {
            lock.unlock();
        }
    }

    static RemoteSMSC getRemoteSMSCByUUID(String uuid) throws Exception {
        log.debug("In getRemoteSMSCByUUID [{}]", uuid);
        lock.lock();
        try {
            return remoteSMSCsByUUID.get(uuid);
        } finally {
            lock.unlock();
        }
    }

    static void shutdownAndRemoveSMSCWithConfig(InterconnectSMSC config) throws Exception {
        log.debug("In removeSMSCWithConfig [{}]", config);
        String uuidOfSMSC = null;
        try {
            RemoteSMSC remoteSmsc = remoteSMSCsById.get(config.getInterconnectSmscId());
            if (remoteSmsc != null) {
                uuidOfSMSC = remoteSmsc.uuid;
                remoteSmsc.shutdown();
                for (Object serverSession : remoteSmsc.serverSessions.values()) {
                    remoteSmsc.closeAndDestroyServerSession((SmppServerSession) serverSession, false);
                }
                if (remoteSmsc.interconnectSMSCBindId > 0) {
                    DAO.removeBind(SmileSMSC.getEMF(), remoteSmsc.interconnectSMSCBindId);
                }
            }
        } catch (Exception e) {
            log.warn("Error removing bind from db", e);
        }
        lock.lock();
        try {
            remoteSMSCsById.remove(config.getInterconnectSmscId());
            remoteSMSCsBySystemId.remove(config.getSystemId());
            remoteSMSCsByTrunkId.remove(config.getInternalTrunkId());
            if (uuidOfSMSC != null) {
                remoteSMSCsByUUID.remove(uuidOfSMSC);
            }
        } finally {
            lock.unlock();
        }
    }

    protected static RemoteSMSC getOrCreateRemoteSMSCBySystemId(String systemId) throws Exception {
        log.debug("In getOrCreateRemoteSMSCBySystemId [{}]", systemId);
        lock.lock();
        try {
            return getOrCreateRemoteSMSCWithConfig(DAO.getSMSCConfigBySystemId(SmileSMSC.getEMF(), systemId));
        } finally {
            lock.unlock();
        }
    }

    private static RemoteSMSC getRemoteSMSCByTrunkId(String trunkId) throws Exception {
        log.debug("In getOrCreateRemoteSMSCByTrunkId [{}]", trunkId);
        lock.lock();
        try {
            RemoteSMSC remoteSmsc = remoteSMSCsByTrunkId.get(trunkId);
            return remoteSmsc;
        } finally {
            lock.unlock();
        }
    }

    private static boolean isAsciiPrintable(String str) {
        if (str == null) {
            return false;
        }
        if (BaseUtils.getBooleanProperty("env.dcs.new.ascii.check", false)) {
            return str.matches("\\A\\p{ASCII}*\\z");
        }

        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if (isAsciiPrintable(str.charAt(i)) == false) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAsciiPrintable(char ch) {
        return ch >= 32 && ch < 127;
    }

    static public byte[][] createConcatenatedBinaryShortMessagesWithLimit(byte[] binaryShortMessage, byte referenceNum, int split) {
        int splitPlusUDH = split + 6;
        if (binaryShortMessage == null) {
            return null;
        }
        // if the short message does not need to be concatenated
        if (binaryShortMessage.length <= splitPlusUDH) {
            return null;
        }

        // since the UDH will be 6 bytes, we'll split the data into chunks of 134
        int numParts = (int) (binaryShortMessage.length / split) + (binaryShortMessage.length % split != 0 ? 1 : 0);
        //logger.debug("numParts=" + numParts);

        byte[][] shortMessageParts = new byte[numParts][];

        for (int i = 0; i < numParts; i++) {
            // default this part length to max of 134
            int shortMessagePartLength = split;
            if ((i + 1) == numParts) {
                // last part (only need to add remainder)
                shortMessagePartLength = binaryShortMessage.length - (i * split);
            }

            //logger.debug("part " + i + " len: " + shortMessagePartLength);
            // part will be UDH (6 bytes) + length of part
            byte[] shortMessagePart = new byte[6 + shortMessagePartLength];
            // Field 1 (1 octet): Length of User Data Header, in this case 05.
            shortMessagePart[0] = (byte) 0x05;
            // Field 2 (1 octet): Information Element Identifier, equal to 00 (Concatenated short messages, 8-bit reference number)
            shortMessagePart[1] = (byte) 0x00;
            // Field 3 (1 octet): Length of the header, excluding the first two fields; equal to 03
            shortMessagePart[2] = (byte) 0x03;
            // Field 4 (1 octet): 00-FF, CSMS reference number, must be same for all the SMS parts in the CSMS
            shortMessagePart[3] = referenceNum;
            // Field 5 (1 octet): 00-FF, total number of parts. The value shall remain constant for every short message which makes up the concatenated short message. If the value is zero then the receiving entity shall ignore the whole information element
            shortMessagePart[4] = (byte) numParts;
            // Field 6 (1 octet): 00-FF, this part's number in the sequence. The value shall start at 1 and increment for every short message which makes up the concatenated short message. If the value is zero or greater than the value in Field 5 then the receiving entity shall ignore the whole information element. [ETSI Specification: GSM 03.40 Version 5.3.0: July 1996]
            shortMessagePart[5] = (byte) (i + 1);

            // copy this part's user data onto the end
            System.arraycopy(binaryShortMessage, (i * split), shortMessagePart, 6, shortMessagePartLength);
            shortMessageParts[i] = shortMessagePart;
        }

        return shortMessageParts;
    }

    private byte[][] splitMultipartMessage(byte[] aMessage, int concatenationMode, int dataCoding, boolean use7bitPacking, byte[] referenceNumber) throws Exception {

        int maximumMultipartMessageSegmentSize;

        /* we added this to be able to have a max message segment size different per partner - thanks MTN!!!! not */
        maximumMultipartMessageSegmentSize = config.getPropertyAsIntFromConfig("maximumMultipartMessageSegmentSize");
        if (maximumMultipartMessageSegmentSize == -1) {
            maximumMultipartMessageSegmentSize = BaseUtils.getIntProperty("env.mm.smpp.default.multipart.segment.size", 134);
            /* if no specific value we default to what we had originally (standard) */
        }

        if (dataCoding == SMSCodec.UTF_16_CODING_SCHEME) {
            maximumMultipartMessageSegmentSize = BaseUtils.getIntProperty("env.mm.smpp.ucs2.multipart.segment.size", 132);
            log.debug("DCS is UTF16/UCS2 so we use maximumMultipartMessageSegmentSize [{}]", maximumMultipartMessageSegmentSize);
        }

        final byte UDHIE_HEADER_LENGTH = 0x05;
        final byte UDHIE_IDENTIFIER_SAR = 0x00;
        final byte UDHIE_SAR_LENGTH = 0x03;

        // determine how many messages have to be sent
        log.debug("Splitting message up: [{}]", aMessage);
        int numberOfSegments = aMessage.length / maximumMultipartMessageSegmentSize;
        int messageLength = aMessage.length;
        if (numberOfSegments > 255) {
            numberOfSegments = 255;
            messageLength = numberOfSegments * maximumMultipartMessageSegmentSize;
        }
        if ((messageLength % maximumMultipartMessageSegmentSize) > 0) {
            numberOfSegments++;
        }

        // prepare array for all of the msg segments
        byte[][] segments = new byte[numberOfSegments][];

        int lengthOfData;

        if (concatenationMode == InterconnectSMSC.UDH_CONCATENATION_MODE && (dataCoding == SMSCodec.UTF_16_CODING_SCHEME || !use7bitPacking)) {
            log.debug("Concat mode is UDH and DCS  is UTF_16 or is not 7 bit so we just use this direct method");
            return createConcatenatedBinaryShortMessagesWithLimit(aMessage, referenceNumber[0], maximumMultipartMessageSegmentSize);
        }

        // split the message adding required headers
        for (int i = 0; i < numberOfSegments; i++) {
            if (numberOfSegments - i == 1) {
                lengthOfData = messageLength - i * maximumMultipartMessageSegmentSize;
            } else {
                lengthOfData = maximumMultipartMessageSegmentSize;
            }

            switch (concatenationMode) {
                case InterconnectSMSC.UDH_CONCATENATION_MODE:
                    log.debug("Concat mode is UDHI");

                    //prepend 0x00 to aMessage
                    //bit pack aMessage
                    byte[] bMessage = new byte[lengthOfData + 1];
                    bMessage[0] = 0x00;
                    System.arraycopy(aMessage, (i * maximumMultipartMessageSegmentSize), bMessage, 1, lengthOfData);
                    //now pack bmessage using gsm 7 bit
                    byte[] messageBytes = PduBitPacker.PackBytes(bMessage, 1);

                    // new array to store the header
                    segments[i] = new byte[6 + messageBytes.length - 1 /*remove 1 byte of bit padding*/];
                    // UDH header
                    // doesn't include itself, its header length
                    segments[i][0] = UDHIE_HEADER_LENGTH;
                    // SAR identifier
                    segments[i][1] = UDHIE_IDENTIFIER_SAR;
                    // SAR length
                    segments[i][2] = UDHIE_SAR_LENGTH;
                    // reference number (same for all messages)
                    segments[i][3] = referenceNumber[0];
                    // total number of segments
                    segments[i][4] = (byte) numberOfSegments;
                    // segment number
                    segments[i][5] = (byte) (i + 1);
                    // copy the data into the array

                    System.arraycopy(messageBytes, 0, segments[i], 6, messageBytes.length - 1 /*remove 1 byte of bit padding*/);
                    break;
                case InterconnectSMSC.TLV_CONCATENATION_MODE:
                    log.debug("Concat mode is TLV - no UDH header");
                    // new array to store the header
                    segments[i] = new byte[lengthOfData];
                    // copy the data into the array
                    System.arraycopy(aMessage, (i * maximumMultipartMessageSegmentSize), segments[i], 0, lengthOfData);
                    break;
                default:
                    String msg = String.format("Tried to send SMPP message with unknown concat mode: [%d] - check smsc config", concatenationMode);
                    log.warn(msg);
                    BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "SMPP", msg);
                    throw new Exception(msg);

            }

        }
        return segments;
    }

    private RemoteSMSC(InterconnectSMSC config) {
        log.debug("In RemoteSMSC constructor for Config [{}]", config);
        this.config = config;
    }

    public InterconnectSMSC getConfig() {
        return config;
    }

    public void bind() throws Exception {
        log.debug("In bind");
        if (config.getEnabled() != 1) {
            log.info("This SMSC [{}] is not enabled so wont bind", config);
            shutdown();
            return;
        }

        if (connection != null) {
            shutdown();
        }
        connection = new SMSCConnection();
        connection.bind();
        if (interconnectSMSCBindId == 0) {
            interconnectSMSCBindId = DAO.storeBind(SmileSMSC.getEMF(), config.getInterconnectSmscId());
        }
    }

    public void storeBind() {
        if (interconnectSMSCBindId == 0) {
            interconnectSMSCBindId = DAO.storeBind(SmileSMSC.getEMF(), config.getInterconnectSmscId());
        }
    }

    private void shutdown() {
        log.debug("In shutdown");
        if (connection != null) {
            log.debug("Connection is not null");
            connection.shutdown();
        } else {
            log.debug("Connection is null");
        }
        connection = null;
    }

    public void testAndReconnectOnFailure() throws Exception {
        if (config.getEnabled() != 1) {
            log.info("This SMSC [{}] is not enabled so wont test connection", config);
            if (connection != null) {
                log.warn("A previously enabled remote connection has been disabled. Shutting down");
                shutdown();
            }
            return;
        }
        if (connection == null) {
            log.debug("Connection is null. We must connect");
            bind();
            return;
        }
        if (!connection.synchronousEnquire()) {
            log.debug("Synchronous enquire returned false. Reconnecting");
            bind();
            log.debug("Reconnected");
            if (!connection.synchronousEnquire()) {
                throw new Exception("SMPP connection is failing its availability test");
            } else {
                //send isup here if MM must report on peers
                BaseUtils.sendStatistic(BaseUtils.getHostNameFromKernel(), config.getSystemId(), "isup", 1, "SMPP Endpoint Test");
            }
        } else {
            log.debug("Synchronous enquire returned true");
            if (BaseUtils.getBooleanProperty("env.mm.report.smpp.isup", true)) {
                //send isup here if MM must report on peers
                BaseUtils.sendStatistic(BaseUtils.getHostNameFromKernel(), config.getSystemId(), "isup", 1, "SMPP Endpoint Test");
            }
        }
    }

    public void closeAndDestroyServerSession(SmppServerSession serverSession, boolean shutdownIfAllDown) throws Exception {
        if (serverSession != null) {
            log.debug("In closeAndDestroyServerSession [{}]", serverSession);
            serverSessions.remove(serverSession.toString());
            try {
                serverSession.close();
            } catch (Exception e) {
                log.warn("Error closing server session", e);
            }
            try {
                serverSession.destroy();
            } catch (Exception e) {
                log.warn("Error destroying server session", e);
            }
            if (shutdownIfAllDown && serverSessions.isEmpty() && connection == null) {
                log.debug("No more client nor server sessions exist. Clearing out remote smsc");
                shutdownAndRemoveSMSCWithConfig(config);
            }
        }
    }

    private byte fixDataCodingIfDefault(byte dataCoding) {
        byte ret;
        if (dataCoding == 0) {
            ret = (byte) config.getPropertyAsIntFromConfig("defaultCodingScheme");
        } else {
            ret = dataCoding;
        }
        log.debug("Data coding scheme was [{}] and is now [{}]", dataCoding, ret);
        return ret;
    }

    public void setServerSession(SmppServerSession serverSession) {
        log.debug("In setServerSession [{}]", serverSession);
        serverSessions.put(serverSession.toString(), serverSession);
        log.debug("We now have [{}] server sessions for this RemoteSMSC", serverSessions.size());
    }

    public static void onNewMessageFromOffnet(String smscSystemId, BaseSm baseSm, String messageId, String sessionIdentifier) throws Exception {
        RemoteSMSC remoteSMSC = RemoteSMSC.getOrCreateRemoteSMSCBySystemId(smscSystemId);
        remoteSMSC.onNewMessageFromOffnet(baseSm, messageId, sessionIdentifier);
    }

    private Date getExpiryDateFromValidity(String validityPeriod) throws Exception {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.YEAR, 1);
        Date expiryDate = cal.getTime();

        if (validityPeriod.length() >= 16) {
            log.debug("received validity period of [{}]", validityPeriod);
            if (validityPeriod.charAt(15) == 'R') {
                log.debug("received relative validity");
                cal.setTime(new Date());
                int years = Integer.parseInt(validityPeriod.substring(0, 2));
                int months = Integer.parseInt(validityPeriod.substring(2, 4));
                int days = Integer.parseInt(validityPeriod.substring(4, 6));
                int hours = Integer.parseInt(validityPeriod.substring(6, 8));
                int minutes = Integer.parseInt(validityPeriod.substring(8, 10));
                int seconds = Integer.parseInt(validityPeriod.substring(10, 12));
                log.debug("adding the following for validity date [{}] = Y{} M{} D{} H{} m{} s{}", new Object[]{
                    validityPeriod,
                    years,
                    months,
                    days,
                    hours,
                    minutes,
                    seconds
                });
                cal.add(Calendar.YEAR, years);
                cal.add(Calendar.MONTH, months);
                cal.add(Calendar.DATE, days);
                cal.add(Calendar.HOUR, hours);
                cal.add(Calendar.MINUTE, minutes);
                cal.add(Calendar.SECOND, seconds);
                expiryDate = cal.getTime();
            } else {
                log.debug("received absolute validity");
                String datePart = validityPeriod.substring(0, 12);
                SimpleDateFormat df = new SimpleDateFormat("yyMMddhhmmss");
                expiryDate = df.parse(datePart);
            }
        } else {
            log.debug("no received validity period [{}], using default", validityPeriod);
        }

        log.debug("returning validity date [{}]", expiryDate);
        return expiryDate;
    }

    private void onNewMessageFromOffnet(BaseSm baseSm, String messageId, String sessionIdentifier) throws Exception {

        if (config.getEnabled() != 1) {
            log.warn("This SMSC [{}] is not enabled yet it got a new message!", config);
            return;
        }
        try {
            interconnectSMSCBindId = DAO.updateBind(SmileSMSC.getEMF(), interconnectSMSCBindId, config.getInterconnectSmscId());
        } catch (Exception e) {
            log.warn("Error updating bind", e);
        }

        OffnetBaseSm offnetBaseSm = new OffnetBaseSm(baseSm);

        String from = baseSm.getSourceAddress().getAddress();
        String to = baseSm.getDestAddress().getAddress();
        Date expiryDate = getExpiryDateFromValidity(baseSm.getValidityPeriod());
        boolean doDeliveryReport = baseSm.getRegisteredDelivery() == 0x01;

        byte[] messageTextByte = null;
        String messageText = "";
        byte dataCoding = fixDataCodingIfDefault(baseSm.getDataCoding());
        boolean mustQueueMessage = false;

        LinkedHashSet<String> messageIds = null;

        boolean multiPart = false;

        int thisMessageId;
        int totalMessages;
        int currentMessageNum;

        if (BaseUtils.getBooleanProperty("env.mm.smpp.process.delivery.receipts", true)) {
            if (SmppUtil.isMessageTypeAnyDeliveryReceipt(baseSm.getEsmClass())) {
                log.debug("This is a delivery receipt.  Encoding for delivery receipt should be ascii and it should never be a multipart message");
                String deliveryReceiptText = SMSCodec.decodeSmpp(baseSm.getShortMessage(), SMSCodec.ASCII_CODING_SCHEME);
                log.debug("Delivery receipt text: [{}]", deliveryReceiptText);

                List<Tlv> optionalParameters = baseSm.getOptionalParameters();
                if (optionalParameters.isEmpty()) {
                    log.debug("Delivery receipt has no optional parameters");
                } else {
                    log.debug("Delivery receipt optional parameters: ");
                    for (Tlv optionalParameter : optionalParameters) {
                        log.debug("Tag value: [{}] name: [{}]", optionalParameter.getTag(), optionalParameter.getTagName());
                        log.debug("Value: [{}]", optionalParameter.getValueAsString());
                    }
                }
                return;
            }
        }

        String userDataString;
        if (offnetBaseSm.isMultipart()) {
            multiPart = true;
            thisMessageId = offnetBaseSm.getMessageId();
            totalMessages = offnetBaseSm.getTotalMessages();
            currentMessageNum = offnetBaseSm.getCurrentMessageNum();

            if (offnetBaseSm.hasUserDataHeader()) {
                log.debug("This message has user Data Header Indicator Enabled so is likely a multi-part message");

                if (dataCoding != SMSCodec.UTF_16_CODING_SCHEME && config.getPropertyAsBooleanFromConfig("useSevenBitForUDHMultipartDefault")) {
                    log.debug("This is not UTF_16/UCS2 and partner is configured to use 7 bit for default DCS");
                    userDataString = SMSCodec.decodeMultiPartUserData(baseSm.getShortMessage());
                } else {
                    log.debug("DCS is UFT_16/UCS2 or partner is set to not use 7bit for default so we decode normally");
                    byte[] userData = GsmUtil.getShortMessageUserData(baseSm.getShortMessage());

                    if (dataCoding == config.getPropertyAsIntFromConfig("defaultCodingScheme")) {
                        String defaultCharSet = config.getPropertyFromConfig("defaultCharset");
                        log.debug("DCS for this incoming message [{}], is the default DCS for this partner - this partner uses charset [{}] for this DCS", dataCoding, defaultCharSet);
                        log.debug("We use this charSet to decode direct");
                        userDataString = SMSCodec.decodeSmppDirect(userData, defaultCharSet);
                    } else {
                        userDataString = SMSCodec.decodeSmpp(userData, dataCoding);
                    }
                }

                log.debug("userDataString: [{}]", userDataString);
            } else {
                log.debug("This message has TLV optional params that indicate multipart message");

                if (dataCoding == config.getPropertyAsIntFromConfig("defaultCodingScheme")) {
                    String defaultCharSet = config.getPropertyFromConfig("defaultCharset");
                    log.debug("DCS for this incoming message [{}], is the default DCS for this partner - this partner uses charset [{}] for this DCS", dataCoding, defaultCharSet);
                    log.debug("We use this charSet to decode direct");
                    userDataString = SMSCodec.decodeSmppDirect(baseSm.getShortMessage(), defaultCharSet);
                } else {
                    userDataString = SMSCodec.decodeSmpp(baseSm.getShortMessage(), dataCoding);
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("This is message [{}] of [{}]", currentMessageNum, totalMessages);
                log.debug("Message id: [{}] User Data: [{}]", thisMessageId, userDataString);
            }

            String key = MultiPartSmppMessage.getKey(from, to, thisMessageId);
            log.debug("key is: [{}]", key);

            IMap<String, MultiPartSmppMessage> multipartSMPPMap = SmileSMSC.getHazelcastInstance().getMap(SmileSMSC.HAZELCAST_SMPP_MAP_NAME);
            log.debug("Getting lock on hazelcast map for key [{}]", key);
            multipartSMPPMap.lock(key);
            log.debug("Got lock on hazelcast map for key [{}]", key);

            if (log.isDebugEnabled()) {
                for (Member m : SmileSMSC.getHazelcastInstance().getCluster().getMembers()) {
                    log.debug("Member exists at [{}]", m.getSocketAddress().getAddress().getHostAddress());
                }
            }

            try {
                MultiPartSmppMessage multiPartMessage = multipartSMPPMap.get(key); //does message already exist?
                if (multiPartMessage == null) {
                    //new MP message
                    MultiPartSmppMessage newMPMessage = new MultiPartSmppMessage(from, to, totalMessages, thisMessageId);
                    newMPMessage.addSMPPMessageIdForDeliveryReports(messageId);
                    newMPMessage.getParts().put(currentMessageNum, userDataString);
                    multipartSMPPMap.put(key, newMPMessage, BaseUtils.getIntProperty("env.hazelcast.config.smpp.ttl", 1200), TimeUnit.SECONDS);

                    if (log.isDebugEnabled()) {
                        log.debug("Added multipart message to Map. Size is now: [{}]", multipartSMPPMap.size());
                    }
                    mustQueueMessage = false; //can't send half a message
                } else {
                    multiPartMessage.addSMPPMessageIdForDeliveryReports(messageId);
                    multiPartMessage.getParts().put(currentMessageNum, userDataString);
                    //now check if we have all the parts..... if we do, we can queue, otherwise we carry on waiting..
                    if (multiPartMessage.hasAllParts()) {
                        //send/enqueue message
                        if (log.isDebugEnabled()) {
                            log.debug("Received all parts of multipart message. Going to enqueue message: [{}]", multiPartMessage.getCombinedMessageParts());
                        }
                        messageText = multiPartMessage.getCombinedMessageParts();
                        multipartSMPPMap.remove(key);
                        if (log.isDebugEnabled()) {
                            log.debug("Removed multipart message and size is now: [{}]", multipartSMPPMap.size());
                        }
                        mustQueueMessage = true;
                        messageIds = multiPartMessage.getSMPPMessageIdSetForDeliveryReports();
                    } else {
                        log.debug("We dont have all of the parts of this message yet");
                        multipartSMPPMap.put(key, multiPartMessage, BaseUtils.getIntProperty("env.hazelcast.config.smpp.ttl", 1200), TimeUnit.SECONDS);
                        //we don't have all the parts yet.
                        mustQueueMessage = false;
                    }
                }
            } finally {
                log.debug("Unlocking hazelcast map for key [{}]", key);
                multipartSMPPMap.unlock(key);
                log.debug("Unlocked hazelcast map for key [{}]", key);
            }
            if (mustQueueMessage) {
                if (dataCoding == SMSCodec.DEFAULT_CODING_SCHEME) {
                    log.debug("Received multi-part SMS in Default code scheme");
                    messageTextByte = SMSCodec.encode(messageText, SMSCodec.ASCII_CODING_SCHEME);
                } else if (dataCoding == SMSCodec.ASCII_CODING_SCHEME) {
                    log.debug("Received multi-part SMS in ASCII code scheme");
                    messageTextByte = SMSCodec.encode(messageText, SMSCodec.ASCII_CODING_SCHEME);
                } else if (dataCoding == SMSCodec.ISO_8859_1_CODING_SCHEME) {
                    log.debug("Received multi-part SMS in ISO_8859_1_CODING_SCHEME code scheme");
                    messageTextByte = SMSCodec.encode(messageText, SMSCodec.ISO_8859_1_CODING_SCHEME);
                } else if (dataCoding == SMSCodec.UTF_16_CODING_SCHEME) {
                    log.debug("Received multi-part SMS in UTF16 code scheme");
                    messageTextByte = SMSCodec.encode(messageText, SMSCodec.UTF_16_CODING_SCHEME);
                } else if (dataCoding == SMSCodec.CLASS_2_8_BIT_DATA_CODING_SCHEME) {
                    log.debug("Received multi-part SMS in CLASS_2_8_BIT_DATA_CODING_SCHEME (DCS 16) code scheme");
                    messageTextByte = SMSCodec.encode(messageText, SMSCodec.CLASS_2_8_BIT_DATA_CODING_SCHEME);
                } else if (dataCoding == SMSCodec.ASCII_CODING_SCHEME_FLASH) {
                    log.debug("Received multi-part SMS in ASCII_CODING_SCHEME_FLASH (DCS 10) code scheme");
                    messageTextByte = SMSCodec.encode(messageText, SMSCodec.ASCII_CODING_SCHEME_FLASH);
                } else if (dataCoding == SMSCodec.CLASS_1_GSM_7_BIT_DATA_CODING_SCHEME) {
                    log.debug("Received multi-part SMS in CLASS_1_GSM_7_BIT_DATA_CODING_SCHEME (DCS F1) code scheme");
                    messageTextByte = SMSCodec.encode(messageText, SMSCodec.CLASS_1_GSM_7_BIT_DATA_CODING_SCHEME);
                } else if (dataCoding == SMSCodec.CLASS_1A_GSM_7_BIT_DATA_CODING_SCHEME) {
                    log.debug("Received multi-part SMS in CLASS_1A_GSM_7_BIT_DATA_CODING_SCHEME (DCS 11) code scheme");
                    messageTextByte = SMSCodec.encode(messageText, SMSCodec.CLASS_1A_GSM_7_BIT_DATA_CODING_SCHEME);
                } else if (dataCoding == SMSCodec.CLASS_1B_GSM_7_BIT_DATA_CODING_SCHEME) {
                    log.debug("Received multi-part SMS in CLASS_1B_GSM_7_BIT_DATA_CODING_SCHEME (DCS F2) code scheme");
                    messageTextByte = SMSCodec.encode(messageText, SMSCodec.CLASS_1B_GSM_7_BIT_DATA_CODING_SCHEME);
                } else if (dataCoding == SMSCodec.NOT_DEFINED_2_DATA_CODING_SCHEME) {
                    log.debug("Received multi-part SMS in NOT_DEFINED_2_DATA_CODING_SCHEME Data coding scheme");
                    messageTextByte = SMSCodec.encode(messageText, SMSCodec.NOT_DEFINED_2_DATA_CODING_SCHEME);
                } else if (dataCoding == SMSCodec.CLASS_3_GSM_7_BIT_TE_SPECIFIC_DATA_CODING_SCHEME) {
                    log.debug("Received multi-part SMS in CLASS_3_GSM_7_BIT_TE_SPECIFIC_DATA_CODING_SCHEME Data coding scheme");
                    messageTextByte = SMSCodec.encode(messageText, SMSCodec.CLASS_3_GSM_7_BIT_TE_SPECIFIC_DATA_CODING_SCHEME);
                } else {
                    String msg = String.format("Received multi-part SMS with unsupported code scheme [%x] and messageText [%s]", dataCoding, messageText);
                    log.warn(msg);
                    BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "SMPP", msg);
                    throw new Exception(msg);
                }
            }
        } else {
            totalMessages = 1;
            log.debug("This message has no user Data Header Indicator Enabled so is not a multi-part message - we just enqueue as normal");
            mustQueueMessage = true;
            if (dataCoding == config.getPropertyAsIntFromConfig("defaultCodingScheme")) {
                String defaultCharSet = config.getPropertyFromConfig("defaultCharset");
                log.debug("DCS for this incoming message [{}], is the default DCS for this partner - this partner uses charset [{}] for this DCS", dataCoding, defaultCharSet);
                log.debug("We use this charSet to decode direct");
                messageText = SMSCodec.decodeSmppDirect(baseSm.getShortMessage(), defaultCharSet);
            } else {
                messageText = SMSCodec.decodeSmpp(baseSm.getShortMessage(), dataCoding);
            }

            if (dataCoding == SMSCodec.DEFAULT_CODING_SCHEME) {
                log.debug("Received single SMS in Default code scheme");
                messageTextByte = SMSCodec.encode(messageText, SMSCodec.ASCII_CODING_SCHEME);
            } else if (dataCoding == SMSCodec.ASCII_CODING_SCHEME) {
                log.debug("Received single SMS in ASCII code scheme");
                messageTextByte = SMSCodec.encode(messageText, SMSCodec.ASCII_CODING_SCHEME);
            } else if (dataCoding == SMSCodec.ISO_8859_1_CODING_SCHEME) {
                log.debug("Received single SMS in ISO_8859_1_CODING_SCHEME code scheme");
                messageTextByte = SMSCodec.encode(messageText, SMSCodec.ISO_8859_1_CODING_SCHEME);
            } else if (dataCoding == SMSCodec.UTF_16_CODING_SCHEME) {
                log.debug("Received single SMS in UTF16 code scheme");
                messageTextByte = SMSCodec.encode(messageText, SMSCodec.UTF_16_CODING_SCHEME);
            } else if (dataCoding == SMSCodec.CLASS_2_8_BIT_DATA_CODING_SCHEME) {
                log.debug("Received single SMS in CLASS_2_8_BIT_DATA_CODING_SCHEME (DCS 16) code scheme");
                messageTextByte = SMSCodec.encode(messageText, SMSCodec.CLASS_2_8_BIT_DATA_CODING_SCHEME);
            } else if (dataCoding == SMSCodec.ASCII_CODING_SCHEME_FLASH) {
                log.debug("Received single SMS in ASCII_CODING_SCHEME_FLASH (DCS 10) code scheme");
                messageTextByte = SMSCodec.encode(messageText, SMSCodec.ASCII_CODING_SCHEME_FLASH);
            } else if (dataCoding == SMSCodec.CLASS_0_GSM_7_BIT_DATA_CODING_SCHEME) {
                log.debug("Received single SMS in CLASS_0_GSM_7_BIT_DATA_CODING_SCHEME (DCS F0) code scheme");
                messageTextByte = SMSCodec.encode(messageText, SMSCodec.CLASS_0_GSM_7_BIT_DATA_CODING_SCHEME);
            } else if (dataCoding == SMSCodec.CLASS_1_GSM_7_BIT_DATA_CODING_SCHEME) {
                log.debug("Received single SMS in CLASS_1_GSM_7_BIT_DATA_CODING_SCHEME (DCS F1) code scheme");
                messageTextByte = SMSCodec.encode(messageText, SMSCodec.CLASS_1_GSM_7_BIT_DATA_CODING_SCHEME);
            } else if (dataCoding == SMSCodec.CLASS_1_GSM_7_BIT_A_DATA_CODING_SCHEME) {
                log.debug("Received single SMS in CLASS_1_GSM_7_BIT_A_DATA_CODING_SCHEME (DCS 11) code scheme");
                messageTextByte = SMSCodec.encode(messageText, SMSCodec.CLASS_1_GSM_7_BIT_A_DATA_CODING_SCHEME);
            } else if (dataCoding == SMSCodec.CLASS_3_GSM_7_BIT_A_DATA_CODING_SCHEME) {
                log.debug("Received single SMS in CLASS_3_GSM_7_BIT_A_DATA_CODING_SCHEME (DCS F3) code scheme");
                messageTextByte = SMSCodec.encode(messageText, SMSCodec.CLASS_3_GSM_7_BIT_A_DATA_CODING_SCHEME);
            } else if (dataCoding == SMSCodec.CLASS_1B_GSM_7_BIT_DATA_CODING_SCHEME) {
                log.debug("Received single SMS in CLASS_1B_GSM_7_BIT_DATA_CODING_SCHEME (DCS F2) code scheme");
                messageTextByte = SMSCodec.encode(messageText, SMSCodec.CLASS_1B_GSM_7_BIT_DATA_CODING_SCHEME);
            } else if (dataCoding == SMSCodec.DATA_8_BIT_CODING_SCHEME) {
                log.debug("Received single SMS in 8 Bit Data coding scheme");
                messageTextByte = SMSCodec.encode(messageText, SMSCodec.DATA_8_BIT_CODING_SCHEME);
            } else if (dataCoding == SMSCodec.CLASS_0_UCS2_DATA_CODING_SCHEME) {
                log.debug("Received single SMS in CLASS_0_UCS2_DATA_CODING_SCHEME Data coding scheme");
                messageTextByte = SMSCodec.encode(messageText, SMSCodec.CLASS_0_UCS2_DATA_CODING_SCHEME);
            } else if (dataCoding == SMSCodec.NOT_DEFINED_2_DATA_CODING_SCHEME) {
                log.debug("Received single SMS in NOT_DEFINED_2_DATA_CODING_SCHEME Data coding scheme");
                messageTextByte = SMSCodec.encode(messageText, SMSCodec.NOT_DEFINED_2_DATA_CODING_SCHEME);
            } else if (dataCoding == SMSCodec.CLASS_3_GSM_7_BIT_TE_SPECIFIC_DATA_CODING_SCHEME) {
                log.debug("Received single SMS in CLASS_3_GSM_7_BIT_TE_SPECIFIC_DATA_CODING_SCHEME Data coding scheme");
                messageTextByte = SMSCodec.encode(messageText, SMSCodec.CLASS_3_GSM_7_BIT_TE_SPECIFIC_DATA_CODING_SCHEME);
            } else {
                String msg = String.format("Received single SMS with unsupported code scheme [%x] and messageText [%s]", dataCoding, messageText);
                log.warn(msg);
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "SMPP", msg);
                throw new Exception(msg);
            }
        }

        if (mustQueueMessage) {
            String mnpPrefix = BaseUtils.getProperty("env.mm.smile.mnp.prefix", "");
            if (!mnpPrefix.isEmpty()) {
                log.debug("There is a Smile MNP prefix [{}], we must strip this off incoming destination numbers.", mnpPrefix);
                if (to.startsWith(mnpPrefix)) {
                    log.debug("To number [{}] starts with MNP prefix [{}] , so we strip it off.", to, mnpPrefix);
                    to = to.replaceFirst(mnpPrefix, "");
                }
            }

            if (!multiPart || (messageIds == null)) {
                log.debug("this is not multipart or we don't have any messageIds");
                messageIds = new LinkedHashSet<>();
                messageIds.add(messageId);
            }

            HashMap<String, Serializable> deliveryReportData = null;
            if (doDeliveryReport) {
                deliveryReportData = new HashMap<>();
                deliveryReportData.put("TrunkId", getConfig().getInternalTrunkId()); // Needed
                deliveryReportData.put("RemoteSMSCUUID", uuid); // Needed
                deliveryReportData.put("SessionIdentifier", sessionIdentifier); // Needed
                log.debug("RemoteSMSCUUID inserted into deliveryReportData [{}] TrunkId [{}] SessionIdentifier [{}]", new Object[]{uuid, getConfig().getInternalTrunkId(), sessionIdentifier});
                log.debug("Adding MessageIds into deliveryReportData of size [{}]", messageIds.size());
                deliveryReportData.put("MessageIds", messageIds);
                deliveryReportData.put("From", to);
                deliveryReportData.put("To", from);
                deliveryReportData.put("SubmitParts", totalMessages);
                deliveryReportData.put("SubmitDate", DateTimeUtil.now());
            }
            SmileSMSC.getInstance().onNewMessageFromOffnet(from, to, expiryDate, deliveryReportData, messageTextByte, dataCoding, config);
        }

    }

    private void sendDeliveryReportInternal(DeliveryEngine.DeliveryReportStatus reportStatus, HashMap<String, Serializable> deliveryReportData) throws Exception {
        DeliveryReceipt dlr = new DeliveryReceipt();

        switch (reportStatus) {
            case EXPIRED:
                dlr.setState(SmppConstants.STATE_EXPIRED);
                break;
            case SUCCESS:
                dlr.setState(SmppConstants.STATE_DELIVERED);
                break;
            case FAILED:
                dlr.setState(SmppConstants.STATE_UNDELIVERABLE);
                break;

        }

        Boolean deliveryReportPerPart = config.getPropertyAsBooleanFromConfig("deliveryReportPerPart");
        LinkedHashSet<String> messageIds = (LinkedHashSet<String>) deliveryReportData.get("MessageIds");
        if (messageIds == null) {
            log.warn("Asked to send a delivery report but I have no messageIds... ignoring");
            return;
        }

        log.debug("going to send delivery report which has [{}] messageIds", messageIds.size());

        if (!deliveryReportPerPart || messageIds.size() == 1) {
            /* send messageId of last part */
            log.debug("sending normal delivery report");
            // Get last part
            String smppMessageId = (String) messageIds.toArray()[(messageIds.size() - 1)];
            dlr.setMessageId(smppMessageId);
            /*set messageid to last in the set*/
            dlr.setDeliveredCount((int) deliveryReportData.get("SubmitParts"));
            dlr.setSubmitCount((int) deliveryReportData.get("SubmitParts"));
            dlr.setDoneDate(DateTimeUtil.now());
            dlr.setSubmitDate((DateTime) deliveryReportData.get("SubmitDate"));
            String receiptText = dlr.toShortMessage();

            OffnetSMSMessage deliveryReportMsg = new OffnetSMSMessage(smppMessageId, System.currentTimeMillis());
            deliveryReportMsg.setFrom((String) deliveryReportData.get("From"));
            deliveryReportMsg.setTo((String) deliveryReportData.get("To"));
            deliveryReportMsg.setDeliveryReport(true);
            deliveryReportMsg.setMessage(receiptText.getBytes());
            deliveryReportMsg.setDestinationTrunkId((String) deliveryReportData.get("TrunkId"));

            String sessionIdentifier = (String) deliveryReportData.get("SessionIdentifier");
            log.debug("This delivery report must be sent on server session [{}]", sessionIdentifier);
            sendMessageInternal(deliveryReportMsg, sessionIdentifier);
        } else {
            log.debug("sending delivery reports for each part");
            /* send messageId per part */
            dlr.setDeliveredCount(1);
            dlr.setSubmitCount(1);
            dlr.setDoneDate(DateTimeUtil.now());
            dlr.setSubmitDate((DateTime) deliveryReportData.get("SubmitDate"));

            for (String smppMessageId : messageIds) {
                dlr.setMessageId(smppMessageId);
                String receiptText = dlr.toShortMessage();
                OffnetSMSMessage deliveryReportMsg = new OffnetSMSMessage(smppMessageId, System.currentTimeMillis());
                deliveryReportMsg.setFrom((String) deliveryReportData.get("From"));
                deliveryReportMsg.setTo((String) deliveryReportData.get("To"));
                deliveryReportMsg.setDeliveryReport(true);
                deliveryReportMsg.setMessage(receiptText.getBytes());
                deliveryReportMsg.setDestinationTrunkId((String) deliveryReportData.get("TrunkId"));
                String sessionIdentifier = (String) deliveryReportData.get("SessionIdentifier");
                log.debug("This delivery report must be sent on server session [{}]", sessionIdentifier);
                sendMessageInternal(deliveryReportMsg, sessionIdentifier);
            }
        }
    }

    static void sendDeliveryReport(DeliveryEngine.DeliveryReportStatus reportStatus, HashMap<String, Serializable> deliveryReportData) throws Exception {
        boolean deliveryReportsEnabled = BaseUtils.getBooleanProperty("env.mm.deliveryreports.enabled", false);
        if (deliveryReportsEnabled) {
            String uuidOfRemoteSMSCToSendOn = (String) deliveryReportData.get("RemoteSMSCUUID");
            String trunkId = (String) deliveryReportData.get("TrunkId");
            log.debug("Delivery report must go out on TrunkId [{}] and preferably UUID [{}]", trunkId, uuidOfRemoteSMSCToSendOn);
            RemoteSMSC remoteSMSC = RemoteSMSC.getRemoteSMSCByUUID(uuidOfRemoteSMSCToSendOn);
            if (remoteSMSC == null) {
                log.debug("No remote SMSC available with UUID [{}]", uuidOfRemoteSMSCToSendOn);
                remoteSMSC = RemoteSMSC.getRemoteSMSCByTrunkId(trunkId);
            }
            if (remoteSMSC == null) {
                throw new NoRouteToSMSCException();
            }
            remoteSMSC.sendDeliveryReportInternal(reportStatus, deliveryReportData);
        }
    }

    public static String sendMessage(OffnetSMSMessage offnetSMS, String sessionIdentifier) throws Exception {
        RemoteSMSC remoteSMSC = RemoteSMSC.getRemoteSMSCByTrunkId(offnetSMS.getDestinationTrunkId());
        if (remoteSMSC == null) {
            throw new NoRouteToSMSCException();
        }
        log.info("Sending offnet sms [{}]", offnetSMS);
        return remoteSMSC.sendMessageInternal(offnetSMS, sessionIdentifier);
    }

    private String sendMessageInternal(OffnetSMSMessage offnetSMS, String sessionIdentifier) throws Exception {
        String callbackId = null;
        log.debug("In sendMessageInternal on RemoteSMSC with UUID [{}] and Trunk [{}] and interconnectSMSCBindId [{}]", new Object[]{uuid, config.getInternalTrunkId(), interconnectSMSCBindId});
        long minGapMs = config.getMinSubmitGapMs();
        if (minGapMs > 0) {
            long now = System.currentTimeMillis();
            long sleepTime = minGapMs - (now - lastMessageSent);
            if (log.isDebugEnabled()) {
                log.debug("This SMSC [{}][{}] needs a gap of [{}]ms between submitting messages. Last msg was sent at [{}] and its now [{}] so last was sent [{}]ms ago so sleep time is [{}]", new Object[]{config.getName(), config.getSystemId(), minGapMs, lastMessageSent, now, now - lastMessageSent, sleepTime});
            }
            if (sleepTime > 0) {
                log.debug("Sleeping for [{}]ms", sleepTime);
                Utils.sleep(sleepTime);
                log.debug("Ok, can submit now");
            }
        }
        lastMessageSent = System.currentTimeMillis();

        String stringSingleMessage = SMSCodec.decode(offnetSMS.getMessage(), offnetSMS.getCodingScheme());
        byte[] byteSingleMessage;
        byte dataCoding;

        if (isAsciiPrintable(stringSingleMessage)) {
            byte interconnectSMSCDefaultDataCoding;
            String interconnectSMSCDefaultCharSet;
            interconnectSMSCDefaultDataCoding = (byte) config.getPropertyAsIntFromConfig("defaultCodingScheme");
            interconnectSMSCDefaultCharSet = config.getPropertyFromConfig("defaultCharset");
            log.debug("This message is only ASCII so we send using the default coding scheme [{}] and charset [{}] for this remote smsc", interconnectSMSCDefaultDataCoding, interconnectSMSCDefaultCharSet);
            dataCoding = interconnectSMSCDefaultDataCoding;
            byteSingleMessage = SMSCodec.encodeSmppDirect(stringSingleMessage, interconnectSMSCDefaultCharSet);
        } else {
            log.debug("This message [{}] has non ASCII in it so we send as default non-ascii coding scheme", stringSingleMessage);
            dataCoding = (byte) BaseUtils.getIntProperty("env.mm.offnet.default.non.ascii.coding.scheme", SMSCodec.UTF_16_CODING_SCHEME);
            byteSingleMessage = SMSCodec.encodeSmpp(stringSingleMessage, dataCoding);
        }

        String from = Utils.getPhoneNumberFromSIPURIWithoutPlus(offnetSMS.getFrom());
        String to = Utils.getPhoneNumberFromSIPURIWithoutPlus(offnetSMS.getTo());

        String replaceFromSettings = config.getPropertyFromConfig("replaceFrom");
        if (replaceFromSettings != null && !replaceFromSettings.isEmpty()) {
            log.debug("Have replacement settings for this remotesmsc: {}", replaceFromSettings);
            String[] replacements = replaceFromSettings.split("\\|");

            for (String replacement : replacements) {
                String[] matchReplace = replacement.split(":");
                log.debug("Checking [{}] against from number [{}]", matchReplace[0], from);
                if (from.equalsIgnoreCase(matchReplace[0])) {
                    log.debug("Replacing [{}] with [{}]", from, matchReplace[1]);
                    from = matchReplace[1];
                    break;
                }
            }
        }

        String mnpPrefix = config.getMnpPrefix();
        if (mnpPrefix != null && !mnpPrefix.isEmpty()) {
            log.debug("This remoteSMSC has an MNP prefix [{}] so we must append it to the to number", mnpPrefix);
            to = mnpPrefix + to;
        }

        int maximumSingleMessageSegmentSize = BaseUtils.getIntProperty("env.mm.smpp.default.single.segment.size", 160);
        if (dataCoding == SMSCodec.UTF_16_CODING_SCHEME) {
            maximumSingleMessageSegmentSize = BaseUtils.getIntProperty("env.mm.smpp.ucs2.single.segment.size", 138);
            log.debug("DCS is UTF16/UCS2 so we use maximumSingleMessageSegmentSize [{}]", maximumSingleMessageSegmentSize);
        }

        if (byteSingleMessage.length <= maximumSingleMessageSegmentSize) {
            log.debug("This is a single length SMS so we just send as is");
            if (config.getClientBind() == 1) {
                log.debug("This partner is a client bind so we send using the client connection");
                if (connection == null) {
                    throw new Exception("Cannot send a message to a disconnected or disabled peer");
                }
                callbackId = connection.sendMessageClientSession(dataCoding, byteSingleMessage, from, to, false, null, null, null, offnetSMS.isDeliveryReport());

            } else {
                log.debug("This partner is not a client bind so we send using the server connection");
                callbackId = sendMessageServerSession(dataCoding, byteSingleMessage, from, to, false, null, null, null, offnetSMS.isDeliveryReport(), sessionIdentifier);
            }
        } else {
            log.debug("This is a multi part SMS so we break it up and send one by one");

            byte[][] byteMessagesArray;

            int concatenationMode;
            boolean useSevenBitForUDHMultipartDefault;
            concatenationMode = config.getPropertyAsIntFromConfig("concatenationMode");
            useSevenBitForUDHMultipartDefault = config.getPropertyAsBooleanFromConfig("useSevenBitForUDHMultipartDefault");

            String defaultMultipartCharset = config.getPropertyFromConfig("defaultMultipartCharset");

            byte[] referenceNumber = null;
            switch (concatenationMode) {
                case InterconnectSMSC.UDH_CONCATENATION_MODE:
                    log.debug("Concat mode is UDHI");
                    // generate new reference number - 1 byte for UDH
                    referenceNumber = new byte[1];
                    new Random().nextBytes(referenceNumber);

                    break;
                case InterconnectSMSC.TLV_CONCATENATION_MODE:
                    log.debug("Concat mode is TLV");
                    // generate new reference number - 2 byte for TLV
                    referenceNumber = new byte[2];
                    new Random().nextBytes(referenceNumber);

                    break;
                default:

            }

            if (dataCoding != SMSCodec.UTF_16_CODING_SCHEME && defaultMultipartCharset != null && !defaultMultipartCharset.isEmpty()) {
                log.debug("This is a multipart SMS with DCS not UTF_16/UCS2 and has a defaultMultipartCharset {{}] so we pass in encoded byte using this charset", defaultMultipartCharset);
                byteMessagesArray = splitMultipartMessage(SMSCodec.encodeSmppDirect(stringSingleMessage, defaultMultipartCharset), concatenationMode, dataCoding, useSevenBitForUDHMultipartDefault, referenceNumber);
            } else {
                log.debug("We pass already encode byte array into split message");
                byteMessagesArray = splitMultipartMessage(byteSingleMessage, concatenationMode, dataCoding, useSevenBitForUDHMultipartDefault, referenceNumber);
            }

            // submit all messages
            for (int i = 0; i < byteMessagesArray.length; i++) {
                if (log.isDebugEnabled()) {
                    log.debug("Sending multi part SMS number [{}] of [{}]", i + 1, byteMessagesArray.length);
                    log.debug("Sending message over SMPP [{}]", offnetSMS);
                }

                byte[] messagePart = new byte[1];
                messagePart[0] = (byte) (i + 1);
                byte[] totalParts = new byte[1];
                totalParts[0] = (byte) byteMessagesArray.length;

                if (config.getClientBind() == 1) {
                    log.debug("This partner is a client bind so we send using the client connection");
                    if (connection == null) {
                        throw new Exception("Cannot send a message to a disconnected or disabled peer");
                    }
                    callbackId = connection.sendMessageClientSession(dataCoding, byteMessagesArray[i], from, to, true, messagePart, totalParts, referenceNumber, offnetSMS.isDeliveryReport());

                } else {
                    log.debug("This partner is not a client bind so we send using the server connection");
                    callbackId = sendMessageServerSession(dataCoding, byteMessagesArray[i], from, to, true, messagePart, totalParts, referenceNumber, offnetSMS.isDeliveryReport(), sessionIdentifier);
                }
            }
        }
        log.debug("Returning callback id [{}]", callbackId);
        return callbackId;
    }

    private String sendMessageServerSession(byte dataCoding, byte[] shortMessage, String from, String to, boolean multiPart, byte[] messagePart, byte[] totalParts, byte[] messageId, boolean isDeliveryReport, String sessionIdentifier) throws Exception {
        if (serverSessions.isEmpty()) {
            throw new Exception("Cannot send a message to a disconnected or disabled server peer");
        }

        if (log.isDebugEnabled()) {
            log.debug("In sendMessageInternalServerSession dataCoding[{}] shortMessage[{}] from[{}] to[{}] multipart[{}] messagePart[{}] totalParts[{}] messageId[{}]",
                    new Object[]{dataCoding, Codec.binToHexString(shortMessage), from, to, multiPart, Codec.binToHexString(messagePart), Codec.binToHexString(totalParts), Codec.binToHexString(messageId)});
        }

        SmppServerSession serverSessionToUse = null;

        if (sessionIdentifier != null) {
            serverSessionToUse = serverSessions.get(sessionIdentifier);
            if (serverSessionToUse == null) {
                log.debug("We have no connected server session called [{}]. We will randomly pick one we do have", sessionIdentifier);
            } else {
                log.debug("The message must go out on session [{}] and we found it", sessionIdentifier);
            }
        } else {
            log.debug("No specific server session was requested");
        }

        if (serverSessionToUse == null) {
            int random = Utils.getRandomNumber(0, serverSessions.size());
            serverSessionToUse = (SmppServerSession) serverSessions.values().toArray()[random];
            log.debug("We have randomly picked server session [{}]", serverSessionToUse);
        }

        DeliverSm sm = new DeliverSm();
        sm.setDataCoding(dataCoding);
        sm.setShortMessage(shortMessage);
        sm.setSourceAddress(new Address(SmppConstants.NPI_E164, SmppConstants.TON_INTERNATIONAL, from));
        sm.setDestAddress(new Address(SmppConstants.NPI_E164, SmppConstants.TON_INTERNATIONAL, to));
        if (isDeliveryReport) {
            sm.setEsmClass(SmppConstants.ESM_CLASS_MT_SMSC_DELIVERY_RECEIPT);
        }

        if (BaseUtils.getBooleanProperty("env.mm.smpp.request.delivery.receipts", true)) {
            if (config.getPropertyAsBooleanFromConfig("deliveryReceipts")) {
                sm.setRegisteredDelivery(SmppConstants.REGISTERED_DELIVERY_SMSC_RECEIPT_REQUESTED);
            }
        }

        if (multiPart) {

            //We either need to add UDHI header or add TLV options depending on the SMS configuration
            int concatenationMode;
            concatenationMode = config.getPropertyAsIntFromConfig("concatenationMode");
            if (log.isDebugEnabled()) {
                log.debug("Server session is sending one message of a multipart message - checking concatenation mode: [{}]", concatenationMode);
            }

            switch (concatenationMode) {
                case InterconnectSMSC.UDH_CONCATENATION_MODE:
                    log.debug("Concat mode is UDHI");
                    sm.setEsmClass(SmppConstants.ESM_CLASS_UDHI_MASK);
                    sm.calculateAndSetCommandLength();
                    break;
                case InterconnectSMSC.TLV_CONCATENATION_MODE:
                    log.debug("Concat mode is TLV - adding TLV params");

                    sm.addOptionalParameter(new Tlv(SmppConstants.TAG_SAR_MSG_REF_NUM, messageId));
                    sm.addOptionalParameter(new Tlv(SmppConstants.TAG_SAR_SEGMENT_SEQNUM, messagePart));
                    sm.addOptionalParameter(new Tlv(SmppConstants.TAG_SAR_TOTAL_SEGMENTS, totalParts));

                    break;
                default:
                    String msg = String.format("Tried to send SMPP message with unknown concat mode: [%d] - check smsc config [%s]", concatenationMode, config.getSystemId());
                    log.warn(msg);
                    BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "SMPP", msg);
                    throw new Exception(msg);
            }
        }

        String callbackId = "";
        try {
            serverSessionToUse.sendRequestPdu(sm, config.getResponseTimeout(), false);

            OffnetBaseSm offnetBaseSm = new OffnetBaseSm((BaseSm) (sm));
            try {
                if (offnetBaseSm.isMultipart()) {
                    log.debug("Message is multiPart so we use callBackId in format SYSTEM_ID_MULTIPART_MESSAGE_ID");
                    callbackId = config.getSystemId() + "_MP_" + offnetBaseSm.getMessageId();
                } else {
                    log.debug("Message is not multiPart so we use callBackId in format SYSTEM_ID_SEQ_NUM");
                    callbackId = config.getSystemId() + "_" + sm.getSequenceNumber();
                }
            } catch (Exception e) {
                String msg = String.format("Exception caught while checking if request is for multipart - going to ignore this response: " + e.toString());
                log.warn(msg);
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "SMPP", msg);
                throw new Exception(msg);
            }

            if (log.isDebugEnabled()) {
                log.debug("callbackId to be returned: [{}]", callbackId);
            }
        } catch (RecoverablePduException | UnrecoverablePduException | SmppTimeoutException | SmppChannelException | InterruptedException e) {
            String msg = String.format("Failed to send deliver_sm: A number: [%s] B number [%s] Partner [%s] Exception: [%s]", from, to, config.getSystemId(), e.getMessage());
            log.warn(msg);
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "SMPP", msg);
            throw new Exception(msg);
        }

        return callbackId;
    }

    private class SMSCConnection {

        private DefaultSmppClient clientBootstrap;
        private final DefaultSmppSessionHandler sessionHandler;
        private SmppSession session;
        ScheduledThreadPoolExecutor monitorExecutor;
        private final SmppSessionConfiguration sessionConfiguration;

        public SMSCConnection() {

            log.debug("In constructor of SMSCConnection for [{}]", config.getName());
            sessionConfiguration = new SmppSessionConfiguration();
            sessionConfiguration.setWindowSize(config.getClientWindowSize());
            sessionConfiguration.setName(config.getName());
            sessionConfiguration.setType(config.getBindType());
            log.debug("Bind type is [{}]", sessionConfiguration.getType());
            sessionConfiguration.setHost(config.getHost());
            sessionConfiguration.setPort(config.getPort());
            sessionConfiguration.setConnectTimeout(config.getConnectionTimeout());
            sessionConfiguration.setSystemId(config.getSystemId());
            sessionConfiguration.setPassword(config.getPassword());
            sessionConfiguration.getLoggingOptions().setLogBytes(config.getLogBytesEnabled() == 1);
            sessionConfiguration.setRequestExpiryTimeout(config.getConnectionDreTimeout());
            sessionConfiguration.setWindowMonitorInterval(config.getConnectionDwmInterval());
            sessionConfiguration.setCountersEnabled(config.getCountersEnabled() == 1);

            monitorExecutor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(config.getClientMonitorExecutorThreads(), new ThreadFactory() {
                private final AtomicInteger sequence = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setName("Smile-MM-SmppClientWindowMonitor-" + config.getName() + "-" + sequence.getAndIncrement());
                    return t;
                }
            });

            clientBootstrap = new DefaultSmppClient(Executors.newCachedThreadPool(), 1, monitorExecutor);

            class ClientSmppSessionHandler extends DefaultSmppSessionHandler {

                @Override
                public PduResponse firePduRequestReceived(PduRequest pduRequest) {
                    log.debug("RemoteSMSC [{}] New SMS received by client [{}] with command id [{}]", new Object[]{config, pduRequest, pduRequest.getCommandId()});
                    PduResponse resp = pduRequest.createResponse();
                    String messageId = getAndIncrementMessageId();
                    if (pduRequest.getCommandId() == SmppConstants.CMD_ID_DELIVER_SM) {
                        try {
                            RemoteSMSC.onNewMessageFromOffnet(session.getConfiguration().getSystemId(), (BaseSm) pduRequest, messageId, session.toString());
                        } catch (com.smilecoms.mm.InsufficientFundsException isf) {
                            log.debug("Insufficient funds From [{}] to [{}] with SystemId [{}]", new Object[]{((BaseSm) pduRequest).getSourceAddress(), ((BaseSm) pduRequest).getDestAddress(), config.getSystemId()});
                            resp.setCommandStatus(SmppConstants.STATUS_DELIVERYFAILURE);
                        } catch (Exception e) {
                            log.warn("Exception processing incoming Deliver PDU: ", e);
                            resp.setCommandStatus(SmppConstants.STATUS_DELIVERYFAILURE);
                        }
                    } else if (pduRequest.getCommandId() == SmppConstants.CMD_ID_SUBMIT_SM) {
                        SubmitSmResp submitResp = (SubmitSmResp) resp;
                        try {
                            RemoteSMSC.onNewMessageFromOffnet(session.getConfiguration().getSystemId(), (BaseSm) pduRequest, messageId, session.toString());
                        } catch (com.smilecoms.mm.InsufficientFundsException isf) {
                            log.debug("Insufficient funds From [{}] to [{}] with SystemId [{}]", new Object[]{((BaseSm) pduRequest).getSourceAddress(), ((BaseSm) pduRequest).getDestAddress(), config.getSystemId()});
                            submitResp.setCommandStatus(SmppConstants.STATUS_DELIVERYFAILURE);
                        } catch (Exception e) {
                            log.warn("Exception processing incoming Submit PDU: ", e);
                            submitResp.setCommandStatus(SmppConstants.STATUS_DELIVERYFAILURE);
                            return submitResp;
                        }
                        submitResp.setMessageId(messageId);
                        return submitResp;
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
                        log.warn(msg, e);
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
                            log.error("Exception running callback on SMPP response");
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

                            if (log.isDebugEnabled()) {
                                for (Member m : SmileSMSC.getHazelcastInstance().getCluster().getMembers()) {
                                    log.debug("Member exists at [{}]", m.getSocketAddress().getAddress().getHostAddress());
                                }
                            }

                            try {
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
                        String msg = String.format("Exception caught while checking if response is for multipart - going to ignore this response: " + e.toString());
                        log.warn(msg, e);
                        BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "SMPP", msg);
                    }
                }

                @Override
                public void fireUnexpectedPduResponseReceived(PduResponse pduResponse) {
                    log.debug("UnexpectedPduResponseReceived RemoteSMSC [{}] Response message: [{}]", session == null ? "null" : session.getConfiguration(), pduResponse.getResultMessage());
                    String msg = String.format("Client SMS submission to remote SMSC received UnexpectedPduResponse. Partner [%s] result: [%s]", session == null ? "null" : session.getConfiguration().getSystemId(), pduResponse.getResultMessage());
                    log.warn(msg);
                    BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "SMPP", msg);
                }

                @Override
                public void fireUnrecoverablePduException(UnrecoverablePduException e) {
                    log.debug("UnrecoverablePduException RemoteSMSC [{}] Response message: [{}]", session == null ? "null" : session.getConfiguration(), e.getMessage());
                    String msg = String.format("Client SMS submission to remote SMSC received UnrecoverablePduException. Partner [%s] result: [%s]", session == null ? "null" : session.getConfiguration().getSystemId(), e.getMessage());
                    log.warn(msg);
                    BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "SMPP", msg);
                }

                @Override
                public void fireRecoverablePduException(RecoverablePduException e) {
                    log.debug("RecoverablePduException RemoteSMSC [{}] Response message: [{}]", session == null ? "null" : session.getConfiguration(), e.getMessage());
                    String msg = String.format("Client SMS submission to remote SMSC received RecoverablePduException. Partner [%s] result: [%s]", session == null ? "null" : session.getConfiguration().getSystemId(), e.getMessage());
                    log.warn(msg);
                    BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "SMPP", msg);
                }

                @Override
                public void fireUnknownThrowable(Throwable t) {
                    log.debug("UnknownThrowable RemoteSMSC [{}] Response message: [{}]", session == null ? "null" : session.getConfiguration(), t.getMessage());
                    String msg = String.format("Client SMS submission to remote SMSC received UnknownThrowable. Partner [%s] result: [%s]", session == null ? "null" : session.getConfiguration().getSystemId(), t.getMessage());
                    log.debug(msg);
                    log.warn("fireUnknownThrowable: ", t);
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
                        callbackId = config.getSystemId() + "_MP_" + offnetBaseSm.getMessageId();
                    }

                    String msg = String.format("Client SMS submission to remote SMSC received PduRequestExpired. Partner [%s] request: [%s]", config.getSystemId(), pduRequest);
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
            sessionHandler = new ClientSmppSessionHandler();

        }

        private void bind() throws Exception {
            log.debug("In SMSCConnection bind");

            session = clientBootstrap.bind(sessionConfiguration, sessionHandler);
            if (!session.isBound()) {
                throw new Exception("Session is not bound after binding -- System id " + sessionConfiguration.getSystemId() + " Password " + sessionConfiguration.getPassword());
            }
            log.debug("Finished SMSCConnection bind");
        }

        private String sendMessageClientSession(byte dataCoding, byte[] shortMessage, String from, String to, boolean multiPart, byte[] messagePart, byte[] totalParts, byte[] messageId, boolean isDeliveryReport) throws Exception {
            if (session == null) {
                throw new TemporaryDeliveryFailure("Session is null - not connected");
            }
            if (log.isDebugEnabled()) {
                log.debug("In sendMessageClientSession dataCoding[{}] shortMessage[{}] from[{}] to[{}] multipart[{}] messagePart[{}] totalParts[{}] messageId[{}]",
                        new Object[]{dataCoding, Codec.binToHexString(shortMessage), from, to, multiPart, Codec.binToHexString(messagePart), Codec.binToHexString(totalParts), Codec.binToHexString(messageId)});
            }
            SubmitSm sm = new SubmitSm();
            sm.setDataCoding(dataCoding);
            sm.setShortMessage(shortMessage);
	    if (from.matches(".*\\d+.*")) {
		log.debug("From address [{}] has numbers in it, assuming normal e.164", from);
		sm.setSourceAddress(new Address(SmppConstants.NPI_E164, SmppConstants.TON_INTERNATIONAL, from));
	    } else {
		log.debug("From address [{}] has only alphas", from);
		sm.setSourceAddress(new Address(SmppConstants.NPI_UNKNOWN, SmppConstants.TON_UNKNOWN, from));
		sm.setDestAddress(new Address(SmppConstants.NPI_E164, SmppConstants.TON_INTERNATIONAL, to));
	    }
	    sm.setDestAddress(new Address(SmppConstants.NPI_E164, SmppConstants.TON_INTERNATIONAL, to));


            if (BaseUtils.getBooleanProperty("env.mm.smpp.request.delivery.receipts", true)) {
                if (config.getPropertyAsBooleanFromConfig("deliveryReceipts")) {
                    sm.setRegisteredDelivery(SmppConstants.REGISTERED_DELIVERY_SMSC_RECEIPT_REQUESTED);
                }
            }

            if (multiPart) {
                int concatenationMode;
                concatenationMode = config.getPropertyAsIntFromConfig("concatenationMode");

                if (log.isDebugEnabled()) {
                    log.debug("Client session is sending one message of a multipart message - checking concatenation mode: [{}]", concatenationMode);
                }

                //We either need to add UDHI header or add TLV options depending on the SMS configuration
                switch (concatenationMode) {
                    case InterconnectSMSC.UDH_CONCATENATION_MODE:
                        log.debug("Concat mode is UDHI");
                        sm.setEsmClass(SmppConstants.ESM_CLASS_UDHI_MASK);
                        sm.calculateAndSetCommandLength();
                        break;
                    case InterconnectSMSC.TLV_CONCATENATION_MODE:
                        log.debug("Concat mode is TLV - adding TLV params");
                        sm.addOptionalParameter(new Tlv(SmppConstants.TAG_SAR_MSG_REF_NUM, messageId));
                        sm.addOptionalParameter(new Tlv(SmppConstants.TAG_SAR_SEGMENT_SEQNUM, messagePart));
                        sm.addOptionalParameter(new Tlv(SmppConstants.TAG_SAR_TOTAL_SEGMENTS, totalParts));

                        break;
                    default:
                        String msg = String.format("Tried to send SMPP message with unknown concat mode: [%d] - check smsc config [%s]", concatenationMode, config.getSystemId());
                        log.warn(msg);
                        BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "SMPP", msg);
                        throw new Exception(msg);
                }
            }

            String callbackId;
            try {

                session.sendRequestPdu(sm, config.getResponseTimeout(), false);

                OffnetBaseSm offnetBaseSm = new OffnetBaseSm((BaseSm) (sm));
                try {
                    if (offnetBaseSm.isMultipart()) {
                        log.debug("Message is multiPart so we use callBackId in format SYSTEM_ID_MULTIPART_MESSAGE_ID");
                        callbackId = config.getSystemId() + "_MP_" + offnetBaseSm.getMessageId();
                    } else {
                        log.debug("Message is not multiPart so we use callBackId in format SYSTEM_ID_SEQ_NUM");
                        callbackId = config.getSystemId() + "_" + sm.getSequenceNumber();
                    }
                } catch (Exception e) {
                    String msg = String.format("Exception caught while checking if request is for multipart - going to ignore this response");
                    log.warn(msg);
                    BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "SMPP", msg);
                    throw new Exception(msg);
                }

                if (log.isDebugEnabled()) {
                    log.debug("callbackId to be returned: [{}]", callbackId);
                }
            } catch (RecoverablePduException | UnrecoverablePduException | SmppTimeoutException | SmppChannelException | InterruptedException e) {
                String msg = String.format("Failed to send submit_sm: A number: [%s] B number [%s] Partner [%s] Exception: [%s]", from, to, config.getSystemId(), e.getMessage());
                log.warn(msg);
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "SMPP", msg);
                throw new Exception(msg);
            }

            return callbackId;
        }

        private boolean synchronousEnquire() throws Exception {
            if (session == null) {
                log.debug("Session is null so we are definitely not connected");
                return false;
            }
            boolean ok = false;
            try {
                log.debug("In SMSCConnection synchronousEnquire");
                EnquireLinkResp enquireLinkResp1 = session.enquireLink(new EnquireLink(), 10000);
                log.debug("synchronous enquire_link_resp: commandStatus [" + enquireLinkResp1.getCommandStatus() + "=" + enquireLinkResp1.getResultMessage() + "]");
                ok = enquireLinkResp1.getResultMessage().equalsIgnoreCase("ok");
                if (ok) {
                    try {
                        if (interconnectSMSCBindId != 0) {
                            interconnectSMSCBindId = DAO.updateBind(SmileSMSC.getEMF(), interconnectSMSCBindId, config.getInterconnectSmscId());
                        }
                    } catch (Exception e) {
                        log.warn("Error updating bind", e);
                    }
                }
            } catch (Exception e) {
                log.warn("synchronousEnquire failed", e);
            }
            return ok;
        }

        private void shutdown() {
            log.debug("In SMSCConnection shutdown for [{}]", config.getName());

            if (session != null) {
                try {
                    session.unbind(5000);
                } catch (Exception e) {
                    log.warn("Ignoring exception while trying to unbind", e);
                }
                try {
                    session.destroy();
                } catch (Exception e) {
                    log.warn("Ignoring exception while trying to destroy session", e);
                }
            }
            log.debug("Shutting down client bootstrap and executors...");
            try {
                clientBootstrap.destroy();
            } catch (Exception e) {
                log.warn("Ignoring exception while trying to destroy clientBootstrap", e);
            }
            try {
                monitorExecutor.shutdownNow();
            } catch (Exception e) {
                log.warn("Ignoring exception while trying to shutdown monitorExecutor", e);
            }
            log.debug("Finished shutting down");

        }

    }

}
