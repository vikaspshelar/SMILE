/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.engine;

import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author paul
 */
public abstract class BaseMessage implements Serializable {

    private static final long serialVersionUID = 1L;
    private final String messageId;
    private final Date messageDate = new Date();
    private final long creationMillis;
    private boolean skipCharging;
    private Priority priority;
    private Date expiryDate;
    private DeliveryReportHandle deliveryReportHandle;
    private String campaignId;

    public enum Priority {
        HIGH, MEDIUM, LOW
    };
    private final DeliveryMessageContext deliveryContext = new DeliveryMessageContext();

    public BaseMessage(String messageId, long creationMillis, Priority priority) {
        this.messageId = messageId;
        this.creationMillis = creationMillis;
        this.priority = priority;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public DeliveryReportHandle getDeliveryReportHandle() {
        return deliveryReportHandle;
    }

    public void setDeliveryReportHandle(DeliveryReportHandle deliveryReportHandle) {
        this.deliveryReportHandle = deliveryReportHandle;
    }

    public BaseMessage(String messageId, long creationMillis) {
        this(messageId, creationMillis, Priority.MEDIUM);
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public DeliveryMessageContext getDeliveryContext() {
        return deliveryContext;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public Date getMessageDate() {
        return messageDate;
    }

    public long getMessageAge() {
        return System.currentTimeMillis() - creationMillis;
    }

    public boolean isSkipCharging() {
        return skipCharging;
    }

    public void setSkipCharging(boolean skipCharging) {
        this.skipCharging = skipCharging;
    }

    @Override
    public String toString() {
        return "Message ID[" + messageId + "] Delivery Context Status[" + getDeliveryContext().toString() + "]" + " Age[" + getMessageAge() + "]ms";
    }

}
