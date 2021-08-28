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
public class Paystack {

    public String getTransactionDetails(String requestURL, Map<String, String> headers) throws Exception {
        return PaymentGatewayHelper.sendHTTPRequest("QUERY_STRING", "GET", requestURL, headers, new HashMap<>(), "Paystack");
    }

    public String getTransactionDetails(String requestURL, Map<String, String> headers, String entity) throws Exception {
        return PaymentGatewayHelper.sendHTTPRequest("QUERY_STRING", "GET", requestURL, headers, entity, "Paystack");
    }

}
