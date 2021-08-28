/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.paymentgateway;

import com.smilecoms.commons.paymentgateway.helpers.PaymentGatewayHelper;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author sabza
 */
public class Cellulant {
    public String getTransactionDetails(String requestURL, Map<String, String> headers) throws Exception {
        return PaymentGatewayHelper.sendHTTPRequest("X_WWW_FORM_URLENCODED", "POST", requestURL, headers, new HashMap<>(), "Cellulant");
    }

    public String getTransactionDetails(String requestURL, Map<String, String> headers, String entity) throws Exception {
        return PaymentGatewayHelper.sendHTTPRequest("X_WWW_FORM_URLENCODED", "POST", requestURL, headers, entity, "Cellulant");
    }
    
    public void acknowledgePayment(String requestURL, Map<String, String> requestParams) throws Exception {
        PaymentGatewayHelper.sendHTTPRequest("X_WWW_FORM_URLENCODED", "POST", requestURL, null, requestParams, "Cellulant");
    }

}
