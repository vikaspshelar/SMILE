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
public interface IPesapalPaymentNotificationData {

    //pesapal_transaction_tracking_id,payment_method,payment_status,pesapal_merchant_reference
    public String getPesapalTransactionId();

    public void setPesapalTransactionId(String pesapalTransactionId);

    public String getPaymentMethod();

    public void setPaymentMethod(String paymentMethod);

    public String getPaymentStatus();

    public void setPaymentStatus(String paymentStatus);

    public String getPesapalMerchantReference();

    public void setPesapalMerchantReference(String pesapalMerchantReference);

    public double getAmountPaid();

    public void setAmountPaid(double amountPaid);
}
