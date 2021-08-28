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
public class CellulantPaymentNotificationResponse implements ICellulantPaymentNotificationResponse {

    public CellulantPaymentNotificationResponse() {
    }

    @JohnzonProperty("statusCode")
    private int statusCode;
    @JohnzonProperty("checkoutRequestID")
    private long checkoutRequestID;
    @JohnzonProperty("statusDescription")
    private String statusDescription;
    @JohnzonProperty("receiptNumber")
    private String receiptNumber;
    public static transient int SUCCESFULLY_PROCESSED_CODE_PAID = 183;
    public static transient int SUCCESFULLY_PROCESSED_CODE_FAILED = 180;
    public static transient String SUCCESFULLY_PROCESSED_CODE_SUCCESFUL = "Successfull";
    public static transient String SUCCESFULLY_PROCESSED_CODE_FAILURE = "Failed";

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
        return this.checkoutRequestID;
    }

    @Override
    public void setCheckoutRequestID(long checkoutRequestID) {
        this.checkoutRequestID = checkoutRequestID;
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
