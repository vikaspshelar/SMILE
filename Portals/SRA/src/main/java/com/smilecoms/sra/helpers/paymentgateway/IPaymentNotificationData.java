/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.helpers.paymentgateway;

/**
 *
 * @author sabza
 */
public interface IPaymentNotificationData {

    public String getPaymentStatus();

    public void setPaymentStatus(String paymentStatus);

    public String getTransactionID();

    public void setTransactionID(String transactionID);

    public double getAmountPaid();

    public void setAmountPaid(double amountPaid);

    public String getMerchantReferenceID();

    public void setMerchantReferenceID(String merchantReferenceID);

    public String getPayloadData();

    public void setPayloadData(String payloadData);

}
