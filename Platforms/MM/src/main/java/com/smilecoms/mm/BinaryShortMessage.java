/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.mm.engine.BaseMessage.Priority;
import com.smilecoms.mm.engine.DeliveryReportHandle;
import com.smilecoms.mm.utils.SMSCodec;
import com.smilecoms.xml.schema.mm.ShortMessage;
import com.smilecoms.xml.schema.mm.StPriority;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author paul
 */
public class BinaryShortMessage {

    private byte[] dataAsBinary;
    private byte dataCodingScheme;
    private String destination;
    private String source;
    Priority priority;
    private String campaignId;
    private Date expiryDate;
    private DeliveryReportHandle deliveryReportHandle;

    public byte getDataCodingScheme() {
        return dataCodingScheme;
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

    public void setDataCodingScheme(byte dataCodingScheme) {
        this.dataCodingScheme = dataCodingScheme;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = Utils.getPublicIdentityForPhoneNumber(destination);
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = Utils.getPublicIdentityForPhoneNumber(source);
    }

    public byte[] getDataAsBinary() {
        return dataAsBinary;
    }

    public void setDataAsBinary(byte[] dataAsBinary) {
        this.dataAsBinary = dataAsBinary;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public BinaryShortMessage() {
    }

    public BinaryShortMessage(ShortMessage sm) throws UnsupportedEncodingException {

        if (sm.getDataAsBase64() != null
                && sm.getDataAsString() != null) {
            throw new RuntimeException("Cannot populate both a text and binary message");
        }

        if (sm.getDataAsBase64() != null) {
            this.dataAsBinary = Utils.decodeBase64(sm.getDataAsBase64());
        } else if (sm.getDataAsString() != null) {
            if (sm.getDataCodingScheme() == 0) {
                sm.setDataCodingScheme((byte) BaseUtils.getIntProperty("env.mm.string.default.coding.scheme", SMSCodec.ASCII_CODING_SCHEME));
            }
            this.dataAsBinary = SMSCodec.encode(sm.getDataAsString(), sm.getDataCodingScheme());
        } else {
            throw new RuntimeException("Invalid message data");
        }

        if (sm.getCampaignId() != null && !sm.getCampaignId().isEmpty()) {
            this.campaignId = sm.getCampaignId();
        }

        this.dataCodingScheme = sm.getDataCodingScheme();
        this.destination = Utils.getPublicIdentityForPhoneNumber(sm.getDestination());
        this.source = Utils.getPublicIdentityForPhoneNumber(sm.getSource());
        this.priority = getPriority(sm.getPriority());
        this.expiryDate = Utils.getFutureDate(Calendar.SECOND, sm.getValiditySeconds() == 0 ? BaseUtils.getIntProperty("env.mm.sms.default.bulk.expiry.seconds", 3600 * 2) : sm.getValiditySeconds());
    }

    private Priority getPriority(StPriority priority) {
        if (priority == null) {
            return Priority.LOW;
        }
        return Priority.valueOf(priority.toString());
    }

}
