/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.plugins.offnetsms.smsc.pdu;

import com.cloudhopper.commons.gsm.GsmUtil;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.pdu.BaseSm;
import com.cloudhopper.smpp.tlv.Tlv;
import com.cloudhopper.smpp.util.SmppUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author richard
 */
public class OffnetBaseSm {
    
    private static final Logger log = LoggerFactory.getLogger(OffnetBaseSm.class);
    private final BaseSm baseSm;
    private int messageId = -1;
    private int totalMessages = -1;
    private int currentMessageNum = -1;
    private final String from;
    private final String to;
    private final int sequenceNumber;
    
    public OffnetBaseSm(BaseSm baseSm) {
        this.baseSm = baseSm;
        this.from = baseSm.getSourceAddress().getAddress();
        this.to = baseSm.getDestAddress().getAddress();
        this.sequenceNumber = baseSm.getSequenceNumber();
    }

    //returns true if this message is multipart and also populates multipart params: thisMessageId, totalMessages, messageId
    public boolean isMultipart() throws Exception {
        try {
            boolean multiPart = false;
            Tlv messagePartTlv = baseSm.getOptionalParameter(SmppConstants.TAG_SAR_SEGMENT_SEQNUM);
            Tlv totalPartstlv = baseSm.getOptionalParameter(SmppConstants.TAG_SAR_TOTAL_SEGMENTS);
            Tlv messageIdTlv = baseSm.getOptionalParameter(SmppConstants.TAG_SAR_MSG_REF_NUM);
            
            if (SmppUtil.isUserDataHeaderIndicatorEnabled(baseSm.getEsmClass())) {
                log.debug("This message has UDHI that indicates multipart message");
                byte[] userDataHeader = GsmUtil.getShortMessageUserDataHeader(baseSm.getShortMessage());
                int iei = userDataHeader[1] & 0xff; // Range 0 to 255, not -128 to 127;
                //if (userDataHeader.length == 6 && iei == 0 /*IEI for concatenation*/) {
                if (iei == 0 /*IEI for concatenation*/) {
                    log.debug("This UDH has an IEI of 0 meaning its for concatenation");
                    messageId = userDataHeader[3] & 0xff; // Range 0 to 255, not -128 to 127;
                    totalMessages = userDataHeader[4] & 0xff; // Range 0 to 255, not -128 to 127;
                    currentMessageNum = userDataHeader[5] & 0xff; // Range 0 to 255, not -128 to 127;
                } else {
                    String msg = String.format("SMPP message with UDH IEI which we do not support. UDH length [%d] IEI [%s].  From [%s] To [%s]",
                            userDataHeader.length, iei, this.from, this.to);
                    log.warn(msg);
                    throw new Exception(msg);
                }
                
                multiPart = true;
            } else if ((messagePartTlv != null && totalPartstlv != null && messageIdTlv != null)) {
                log.debug("This message has TLV optional params so could be a multipart message - need to check if total segments more than 1");
                //some operators send TLV parameters for multipart but they say total_segments are 0 and its just short message!  So we check here and treat those just as normal SMS
                try {
                    switch (totalPartstlv.getLength()) {
                        case 1:
                            if (log.isDebugEnabled()) {
                                log.debug("totalPartstlv is 1 byte so we cast as byte [{}]", totalPartstlv.getValueAsUnsignedByte());
                            }
                            totalMessages = (int) totalPartstlv.getValueAsUnsignedByte();
                            break;
                        
                        case 2:
                            log.debug("totalPartstlv is 2 bytes so we cast as short");
                            totalMessages = (int) totalPartstlv.getValueAsUnsignedShort();
                            break;
                        case 4:
                            log.debug("totalPartstlv is 4 bytes so we cast as int");
                            totalMessages = totalPartstlv.getValueAsInt();
                            break;
                        default:
                            log.warn("totalPartstlv is not 1, 2 or 4 bytes");
                    }
                    switch (messageIdTlv.getLength()) {
                        case 2:
                            if (log.isDebugEnabled()) {
                                log.debug("Message id is 2 bytes so we cast as short [{}]", messageIdTlv.getValueAsUnsignedShort());
                            }
                            messageId = (int) messageIdTlv.getValueAsUnsignedShort();
                            break;
                        case 4:
                            log.debug("Message id is 4 bytes so we cast as int");
                            messageId = messageIdTlv.getValueAsInt();
                            break;
                        default:
                            
                            String msg = String.format("SMPP message with TLV message ID not 2 or 4 bytes:  bad message ID");
                            log.warn(msg);
                            throw new Exception(msg);
                    }

                    //Note totalParts has already been retrieved from the message further up when checking to see if we should treat as multipart
                    switch (messagePartTlv.getLength()) {
                        case 1:
                            if (log.isDebugEnabled()) {
                                log.debug("messagePartTlv is 1 byte so we cast as byte [{}]", messagePartTlv.getValueAsUnsignedByte());
                            }
                            currentMessageNum = (int) messagePartTlv.getValueAsUnsignedByte();
                            break;
                        
                        case 2:
                            log.debug("messagePartTlv is 2 bytes so we cast as short");
                            currentMessageNum = (int) messagePartTlv.getValueAsUnsignedShort();
                            break;
                        case 4:
                            log.debug("messagePartTlv is 4 bytes so we cast as int");
                            currentMessageNum = messagePartTlv.getValueAsInt();
                            break;
                        default:
                            log.warn("messagePartTlv is not 1, 2 or 4 bytes");
                            String msg = String.format("Concatenated SMPP message with bad messagePartTlv");
                            log.warn(msg);
                            throw new Exception(msg);
                    }
                } catch (Exception e) {
                    String msg = String.format("Concatenated SMPP message with bad totalPartstlv");
                    log.warn(msg);
                    throw new Exception(msg);
                }
                
                if (totalMessages <= 1) {
                    log.debug("Received concatenated SMPP message with total messages less than equal to 1 - we just treat this as non-multipart message");
                } else {
                    log.debug("Received concatenated SMPP message with total messages more than 1 - this is a multipart message");
                    multiPart = true;
                }
            }
            return multiPart;
        } catch (Exception e) {
            log.warn("Error  in isMultipart", e);
            throw e;
        }
    }
    
    public int getMessageId() {
        return messageId;
    }
    
    public int getTotalMessages() {
        return totalMessages;
    }
    
    public int getCurrentMessageNum() {
        return currentMessageNum;
    }
    
    public String getFrom() {
        return from;
    }
    
    public String getTo() {
        return to;
    }
    
    public int getSequenceNumber() {
        return sequenceNumber;
    }
    
    public boolean hasUserDataHeader() {
        return SmppUtil.isUserDataHeaderIndicatorEnabled(baseSm.getEsmClass());
    }
    
}
