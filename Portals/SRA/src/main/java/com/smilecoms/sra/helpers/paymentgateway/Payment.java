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
public class Payment {

    @JohnzonProperty("paymentMode")
    private String paymentMode;
    @JohnzonProperty("paymentDate")
    private String paymentDate;
    @JohnzonProperty("payerClientCode")
    private String payerClientCode;
    @JohnzonProperty("amountPaid")
    private String amountPaid;
    @JohnzonProperty("MSISDN")
    private String MSISDN;
    @JohnzonProperty("payerTransactionID")
    private String payerTransactionID;
    @JohnzonProperty("payerClientID")
    private int payerClientID;
    @JohnzonProperty("beepTransactionID")
    private String beepTransactionID;

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String value) {
        this.paymentMode = value;
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(String paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getMSISDN() {
        return MSISDN;
    }

    public void setMSISDN(String MSISDN) {
        this.MSISDN = MSISDN;
    }

    public String getPayerClientCode() {
        return payerClientCode;
    }

    public void setPayerClientCode(String payerClientCode) {
        this.payerClientCode = payerClientCode;
    }

    public String getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(String amountPaid) {
        this.amountPaid = amountPaid;
    }

    public String getPayerTransactionID() {
        return payerTransactionID;
    }

    public void setPayerTransactionID(String payerTransactionID) {
        this.payerTransactionID = payerTransactionID;
    }

    public int getPayerClientID() {
        return payerClientID;
    }

    public void setPayerClientID(int payerClientID) {
        this.payerClientID = payerClientID;
    }

    public String getBeepTransactionID() {
        return beepTransactionID;
    }

    public void setBeepTransactionID(String beepTransactionID) {
        this.beepTransactionID = beepTransactionID;
    }
}
