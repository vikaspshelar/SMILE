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
public interface ICellulantPaymentNotificationData {

    /**
     * {
        "paymentStatusCode":178,
        "accountNumber":"<reference>,
        "currencyCode":"KES",
        "checkoutTransactionID":46910000,
        "requestAmount":"500.00",
        "amountPaid":500,
        "payments":[
                {"paymentMode":"MOBILE",
                "paymentDate":"2018-02-22 17:39:12.0",
                "payerClientCode":"SAFKE",
                "amountPaid":"500.00",
                "MSISDN":"<payermobilenumber>",
                "payerTransactionID":"<referencefromMNO>",
                "payerClientID":<ourid>,
                "beepTransactionID":"<ourreferenceID>"
                }],
        "merchantReferenceID":"<idyourhadprovided>",
        "requestDate":"2018-02-22 17:38:41.0",
        "MSISDN":"<customermobilenumber>"
      }
     */
    
    public int getPaymentStatusCode();

    public void setPaymentStatusCode(int paymentStatusCode);

    public long getCheckoutTransactionID();

    public void setCheckoutTransactionID(long checkoutTransactionID);

    public double getAmountPaid();

    public void setAmountPaid(double amountPaid);

    public String getMerchantReferenceID();

    public void setMerchantReferenceID(String merchantReferenceID);

    public String getRequestDate();

    public void setRequestDate(String requestDate);

    public String getMSISDN();

    public void setMSISDN(String MSISDN);

    public Payment[] getPayments();

    public void setPayments(Payment[] payments);
    
    public String getAccountNumber();

    public void setAccountNumber(String accountNumber);
    
    public String getCurrencyCode();

    public void setCurrencyCode(String currencyCode);
    
    public String getRequestAmount();

    public void setRequestAmount(String requestAmount);

}
