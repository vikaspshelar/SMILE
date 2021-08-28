/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.paymentgateway;

import com.smilecoms.commons.paymentgateway.helpers.PaymentGatewayHelper;
import java.util.Map;

/**
 *
 * @author rajeshkumar
 */
public class SelcomPayments {
    
    public String getTransactionDetails(String requestURL, Map<String, String> headers, String postData) throws Exception {
        return PaymentGatewayHelper.sendHTTPRequest("BODY", "GET", requestURL, headers, postData, "SelcomPayments");
    }
}
