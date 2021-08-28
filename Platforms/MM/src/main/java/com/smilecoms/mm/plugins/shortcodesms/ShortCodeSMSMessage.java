/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.plugins.shortcodesms;

import com.smilecoms.mm.engine.BaseMessage;

/**
 *
 * @author paul
 */
public class ShortCodeSMSMessage extends BaseMessage {
    private static final long serialVersionUID=1L;
    private String to;
    private String from;
    private byte[] message;
    private String SCSCFName;
    private String uuid;
    private Boolean isSystemMessage;
    private byte codingScheme;
    
    public ShortCodeSMSMessage(String messageId, long creationMillis) {
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

    public void setCodingScheme(byte codingScheme) {
        this.codingScheme = codingScheme;
    }

}
