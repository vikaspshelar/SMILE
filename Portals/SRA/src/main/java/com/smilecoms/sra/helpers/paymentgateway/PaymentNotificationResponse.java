/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.helpers.paymentgateway;

import org.apache.johnzon.mapper.JohnzonProperty;

/**
 *
 * @author sabza
 */
public class PaymentNotificationResponse implements ICellulantPaymentNotificationResponse {

    /**
     * CELLULANT
     */
    @JohnzonProperty("statusCode")
    private int statusCode;
    @JohnzonProperty("checkoutReferenceID")
    private long checkoutReferenceID;
    @JohnzonProperty("statusDescription")
    private String statusDescription;
    @JohnzonProperty("receiptNumber")
    private String receiptNumber;

    @Override
    public int getStatusCode() {
        return this.statusCode;
    }

    @Override
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public long getCheckoutRequestID() {
        return this.checkoutReferenceID;
    }

    @Override
    public void setCheckoutRequestID(long checkoutReferenceID) {
        this.checkoutReferenceID = checkoutReferenceID;
    }

    @Override
    public String getStatusDescription() {
        return this.statusDescription;
    }

    @Override
    public void setStatusDescription(String statusDescription) {
        this.statusDescription = statusDescription;
    }

    @Override
    public String getReceiptNumber() {
        return this.receiptNumber;
    }

    @Override
    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

}
