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
public interface ICellulantPaymentNotificationResponse {

    public int getStatusCode();

    public void setStatusCode(int statusCode);

    public long getCheckoutRequestID();

    public void setCheckoutRequestID(long checkoutReferenceID);

    public String getStatusDescription();

    public void setStatusDescription(String statusDescription);

    public String getReceiptNumber();

    public void setReceiptNumber(String receiptNumber);

}
