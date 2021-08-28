/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.plugins.offnetsms.smsc;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author richard.good
 */
public class MultiPartSmppMessage implements Serializable {
    private final String from;
    private final String to;
    private int partCount;
    private final int messageId;
    private HashMap<Integer,String> parts;
    private final LinkedHashSet<String> smppMessageIds;
    private static final Logger log = LoggerFactory.getLogger(MultiPartSmppMessage.class);
    
    public static String getKey(String from, String to, int messageId) {
        return from + ":" + to + ":" + messageId;
    }

    public MultiPartSmppMessage(String from, String to, int partCount, int messageId) {
        this.from = from;
        this.to = to;
        this.messageId = messageId;
        this.partCount = partCount;
        parts = new HashMap<>();
        smppMessageIds = new LinkedHashSet<>();
    }
    
    public void addSMPPMessageIdForDeliveryReports(String messageID) {
        smppMessageIds.add(messageID);
    }
    
    public LinkedHashSet<String> getSMPPMessageIdSetForDeliveryReports() {
        return smppMessageIds;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public int getMessageId() {
        return messageId;
    }

    public int getPartCount() {
        return partCount;
    }

    public void setPartCount(int partCount) {
        this.partCount = partCount;
    }

    public HashMap<Integer, String> getParts() {
        return parts;
    }

    public void setParts(HashMap<Integer, String> parts) {
        this.parts = parts;
    }

    public boolean hasAllParts() {
        log.debug("checking if multipart message has all parts");
        for (int i=1; i<=this.partCount;i++) {
            if (this.parts.get(i) == null){ 
                log.debug("multipart message does not have all parts");
                return false;
            }
        }
        log.debug("multipart message does have all parts");
        return true;
    }    

    public String getCombinedMessageParts() throws Exception {
        String ret = "";
        String tmp;
        for (int i=1; i<=this.partCount;i++) {
            tmp = this.parts.get(i);
            if (tmp == null){ 
                throw new Exception("can't combine parts until all have been received");
            }
            ret = ret + tmp; 
        }
        return ret;        
    }
}

