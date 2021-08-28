/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.smilecoms.mm.sms;

import java.io.Serializable;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jaybeepee
 */
public class MultiPartSmsMessage implements Serializable {
    private String from;
    private String to;
    private int lastReceivedSeq;
    private int partCount;
    private HashMap<Integer,String> parts;
    private static final Logger log = LoggerFactory.getLogger(MultiPartSmsMessage.class);
    
    public static String getKey(String from, String to, int reference) {
        return from + ":" + to + ":" + reference;
    }

    public MultiPartSmsMessage(int lastReceivedSeq, int partCount) {
        this.lastReceivedSeq = lastReceivedSeq;
        this.partCount = partCount;
        parts = new HashMap<>();
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public int getLastReceivedSeq() {
        return lastReceivedSeq;
    }

    public void setLastReceivedSeq(int lastReceivedSeq) {
        this.lastReceivedSeq = lastReceivedSeq;
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
