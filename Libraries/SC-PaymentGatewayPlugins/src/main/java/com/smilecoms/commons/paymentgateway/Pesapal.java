/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.paymentgateway;

import com.smilecoms.commons.paymentgateway.helpers.PaymentGatewayHelper;
import com.smilecoms.commons.util.Oauth1Utils;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author sabza
 */
public class Pesapal {

    private final String reqURL;
    private final String aouthKeyProp;

    public Pesapal(String reqURL, String aouthKeyProp) {
        this.reqURL = reqURL;
        this.aouthKeyProp = aouthKeyProp;
    }

    public Pesapal() {
        this.reqURL = null;
        this.aouthKeyProp = null;
    }

    public String getTransactionDetailsOld(String orderId) throws Exception {
        return queryPaymentStatusByMerchantRef(orderId);
    }

    public String getTransactionDetails(String transactionReference, String orderId, boolean statusOnly) throws Exception {
        if (statusOnly) {
            return queryPaymentStatus(transactionReference, orderId);
        }
        return queryPaymentDetails(transactionReference, orderId);
    }

    /*
     QueryPaymentStatusByMerchantRef
     Once the transaction has been posted to PesaPal, you may use QueryPaymentStatus to query the status
     of the payment.
     URL:
     https://www.pesapal.com/API/QueryPaymentStatusByMerchantRef
     Parameters:
     pesapal_merchant_reference = <the Reference you sent to PesaPal when posting the transaction>
     Return Value:
     pesapal_response_data =<PENDING|COMPLETED|FAILED|INVALID>
     */
    private String queryPaymentStatusByMerchantRef(String orderId) throws Exception {

        Map<String, String> oauthParams = new HashMap<>();
        oauthParams.put("pesapal_merchant_reference", orderId);
        //http://www.paysmile.co.tz/index.php?option=com_users&amp;task=paybill.checkStatus&amp;reference=19
        String ret = Oauth1Utils.getHttpRequestResponse(reqURL, "GET", oauthParams, null, aouthKeyProp);
        return ret;
    }

    private String queryPaymentStatusByMerchantRef1(String orderId) throws Exception {

        Map<String, String> oauthParams = new HashMap<>();
        oauthParams.put("pesapal_merchant_reference", orderId);

        String ret = Oauth1Utils.getHttpRequestResponse(reqURL, "GET", oauthParams, null, aouthKeyProp);
        return ret;
    }


    /*
     QueryPaymentStatus
     Once the transaction has been posted to PesaPal, you may use QueryPaymentStatus to query the status
     of the payment.
     URL:
     https://www.pesapal.com/API/QueryPaymentStatus
     Parameters:
     pesapal_merchant_reference = <the Reference you sent to PesaPal when posting the transaction>
     pesapal_transaction_tracking_id = <the id returned to you by PesaPal (as a query parameter) when redirecting to your website>
     Return Value:
     pesapal_response_data =<PENDING|COMPLETED|FAILED|INVALID>
     */
    private String queryPaymentStatus(String transactionReference, String orderId) throws Exception {

        Map<String, String> oauthParams = new HashMap<>();
        oauthParams.put("pesapal_merchant_reference", orderId);
        oauthParams.put("pesapal_transaction_tracking_id", transactionReference);
        String ret = Oauth1Utils.getHttpRequestResponse(reqURL, "GET", oauthParams, null, aouthKeyProp);
        return ret;
    }

    /*
     QueryPaymentDetails
     Once the transaction has been posted to PesaPal, you may use QueryPaymentStatus to query the status
     of the payment.
     URL:
     https://www.pesapal.com/API/QueryPaymentDetails
     Parameters:
     pesapal_merchant_reference = <the Reference you sent to PesaPal when posting the transaction>
     pesapal_transaction_tracking_id = <the id returned to you by PesaPal (as a query parameter) when redirecting to your website>
     Return Value:
     pesapal_response_data =<pesapal_transaction_tracking_id,payment_method,payment_status,pesapal_merchant_reference>
     */
    private String queryPaymentDetails(String transactionReference, String orderId) throws Exception {

        Map<String, String> oauthParams = new HashMap<>();
        oauthParams.put("pesapal_merchant_reference", orderId);
        oauthParams.put("pesapal_transaction_tracking_id", transactionReference);
        String ret = Oauth1Utils.getHttpRequestResponse(reqURL, "GET", oauthParams, null, aouthKeyProp);
        return ret;
    }

    public String getTransactionDetails(String requestURL) throws Exception {
        return PaymentGatewayHelper.sendHTTPRequest("QUERY_STRING", "GET", requestURL, new HashMap<>(), new HashMap<>(), "Pesapal");
    }

}
