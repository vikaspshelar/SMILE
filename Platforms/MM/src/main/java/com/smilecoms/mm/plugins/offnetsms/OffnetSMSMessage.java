/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.plugins.offnetsms;

import com.smilecoms.mm.engine.BaseMessage;
import java.io.Serializable;

/**
 *
 * @author paul
 */
public class OffnetSMSMessage extends BaseMessage implements Serializable {
    private static final long serialVersionUID=1L;
    private String to;
    private String from;
    private byte[] message;
    private String destinationTrunkId;
    private byte codingScheme;
    private boolean deliveryReport;

    public OffnetSMSMessage(String messageId, long creationMillis) {
        super(messageId, creationMillis);
    }

    public String getFrom() {
        return from;
    }

    public boolean isDeliveryReport() {
        return deliveryReport;
    }

    public void setDeliveryReport(boolean deliveryReport) {
        this.deliveryReport = deliveryReport;
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

    public String getDestinationTrunkId() {
        return destinationTrunkId;
    }

    public void setDestinationTrunkId(String destinationTrunkId) {
        this.destinationTrunkId = destinationTrunkId;
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

    @Override
    public String toString() {
        return "Message ID[" + getMessageId() + "] Delivery Context Status[" + getDeliveryContext().toString() + "]" + " Age[" + getMessageAge() + "]ms From[" + getFrom() +"] To[" + getTo() +"] Dest TrunkId[" + getDestinationTrunkId();
    }
}
