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

public class PaymentNotificationData implements IPaymentNotificationData,
        ICellulantPaymentNotificationData, IPesapalPaymentNotificationData {

    /**
     * GENERIC
     */
    @JohnzonProperty("paymentStatus")
    private String paymentStatus;
    @JohnzonProperty("transactionID")
    private String transactionID;
    @JohnzonProperty("payloadData")
    private String payloadData;

    @Override
    public String getPaymentStatus() {
        return this.paymentStatus;
    }

    @Override
    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    @Override
    public String getTransactionID() {
        return this.transactionID;
    }

    @Override
    public void setTransactionID(String transactionID) {
        this.transactionID = transactionID;
    }

    @Override
    public String getPayloadData() {
        return this.payloadData;
    }

    @Override
    public void setPayloadData(String payloadData) {
        this.payloadData = payloadData;
    }

    /**
     * CELLULANT
     */
    @JohnzonProperty("paymentStatusCode")
    private int paymentStatusCode;
    @JohnzonProperty("checkoutTransactionID")
    private long checkoutTransactionID;
    @JohnzonProperty("amountPaid")
    private double amountPaid;
    @JohnzonProperty("merchantReferenceID")
    private String merchantReferenceID;
    @JohnzonProperty("requestDate")
    private String requestDate;
    @JohnzonProperty("MSISDN")
    private String MSISDN;
    @JohnzonProperty("currencyCode")
    private String currencyCode;
    @JohnzonProperty("accountNumber")
    private String accountNumber;
    @JohnzonProperty("requestAmount")
    private String requestAmount;
    @JohnzonProperty("payments")
    private Payment[] payments;

    @Override
    public int getPaymentStatusCode() {
        return this.paymentStatusCode;
    }

    @Override
    public void setPaymentStatusCode(int paymentStatusCode) {
        this.paymentStatusCode = paymentStatusCode;
    }

    @Override
    public long getCheckoutTransactionID() {
        return this.checkoutTransactionID;
    }

    @Override
    public void setCheckoutTransactionID(long checkoutTransactionID) {
        this.checkoutTransactionID = checkoutTransactionID;
    }

    @Override
    public double getAmountPaid() {
        return this.amountPaid;
    }

    @Override
    public void setAmountPaid(double amountPaid) {
        this.amountPaid = amountPaid;
    }

    @Override
    public String getMerchantReferenceID() {
        return this.merchantReferenceID;
    }

    @Override
    public void setMerchantReferenceID(String merchantReferenceID) {
        this.merchantReferenceID = merchantReferenceID;
    }

    @Override
    public String getRequestDate() {
        return requestDate;
    }

    @Override
    public void setRequestDate(String requestDate) {
        this.requestDate = requestDate;
    }

    @Override
    public String getMSISDN() {
        return MSISDN;
    }

    @Override
    public void setMSISDN(String MSISDN) {
        this.MSISDN = MSISDN;
    }

    @Override
    public Payment[] getPayments() {
        return payments;
    }

    @Override
    public String getAccountNumber() {
        return accountNumber;
    }

    @Override
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    @Override
    public String getCurrencyCode() {
        return currencyCode;
    }

    @Override
    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    @Override
    public String getRequestAmount() {
        return requestAmount;
    }

    @Override
    public void setRequestAmount(String requestAmount) {
        this.requestAmount = requestAmount;
    }

    @Override
    public void setPayments(Payment[] payments) {
        this.payments = payments;
    }

    /**
     * PESAPAL (PoC)
     */
    @JohnzonProperty("pesapalTransactionId")
    private String pesapalTransactionId;
    @JohnzonProperty("paymentMethod")
    private String paymentMethod;
    @JohnzonProperty("pesapalMerchantReference")
    private String pesapalMerchantReference;

    @Override
    public String getPesapalTransactionId() {
        return pesapalTransactionId;
    }

    @Override
    public void setPesapalTransactionId(String pesapalTransactionId) {
        this.pesapalTransactionId = pesapalTransactionId;
    }

    @Override
    public String getPaymentMethod() {
        return paymentMethod;
    }

    @Override
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    @Override
    public String getPesapalMerchantReference() {
        return pesapalMerchantReference;
    }

    @Override
    public void setPesapalMerchantReference(String pesapalMerchantReference) {
        this.pesapalMerchantReference = pesapalMerchantReference;
    }
    
}
