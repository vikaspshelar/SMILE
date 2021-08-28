/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.plugins.onnetsms;

import com.smilecoms.mm.engine.BaseMessage;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class OnnetSMSMessage extends BaseMessage  implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(OnnetSMSMessage.class);
    private static final long serialVersionUID=1L;
    private String to;
    private String from;
    private byte[] message;
    private String SCSCFName;
    private String uuid;
    private Boolean isSystemMessage;
    private byte codingScheme;
    private int pendingContacts;
    public HashMap<String, HashSet<String>> contactParts;
    
    
    public OnnetSMSMessage(String messageId, long creationMillis) {
        super(messageId, creationMillis);
        isSystemMessage = false;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public byte[] getMessage() {
        return message;
    }

    public void setMessage(byte[] message) {
        this.message = message;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSCSCFName() {
        return SCSCFName;
    }

    public void setSCSCFName(String SCSCFName) {
        this.SCSCFName = SCSCFName;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setIsSystemMessage(Boolean isSystemMessage) {
        this.isSystemMessage = isSystemMessage;
    }

    boolean isSystemMessage() {
        return isSystemMessage;
    }

    public byte getCodingScheme() {
        return codingScheme;
    }
    
    public String getCodingSchemeHex() {
        return String.format("%02X", codingScheme);
    }

    public void setCodingScheme(byte codingScheme) {
        this.codingScheme = codingScheme;
    }

    public int getPendingContacts() {
        return pendingContacts;
    }

    public void incrementPendingContacts() {
        getDeliveryContext().lock();
        try {
            log.debug("lock acquired, about to incrememnt contact counter which is currently [{}]", this.getPendingContacts());
            pendingContacts++;
            log.debug("contact counter is now [{}] for MID [{}]", new Object[]{this.getPendingContacts(), this.getMessageId()});
        } finally {
            getDeliveryContext().unlock();
        }
    }

    public void decrementPendingContacts() {
        getDeliveryContext().lock();
        try {
            log.debug("lock acquired, about to decrement contact counter which is currently [{}]", this.getPendingContacts());
            pendingContacts--;
            log.debug("contact counter is now [{}] for MID [{}]", new Object[]{this.getPendingContacts(), this.getMessageId()});
        } finally {
            getDeliveryContext().unlock();
        }
    }
    
    @Override
    public String toString() {
        return "Message ID[" + getMessageId() + "] Delivery Context Status[" + getDeliveryContext().toString() + "]" + " Age[" + getMessageAge() + "]ms From[" + getFrom() +"] To[" + getTo() +"]";
    }
    
}
