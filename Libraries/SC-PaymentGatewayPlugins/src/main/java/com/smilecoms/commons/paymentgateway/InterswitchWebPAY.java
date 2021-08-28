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
public class InterswitchWebPAY {

    public String getTransactionDetails(String requestURL, Map<String, String> requestParams, String headerParam) throws Exception {
        /*
        GET
        https://stageserv.interswitchng.com/test_paydirect/api/v1/gettransaction.xml?productid=21&transactionreference=8421941122&amount=300000 HTTP/1.1
        UserAgent: Mozilla/4.0 (compatible; MSIE 6.0; MS Web Services Client Protocol 4.0.30319.239)
        Hash: F6FF2E22F99D93DDDA52D71811FD92B3A71FA1968A66216E0D310DAD
         */
        Map<String, String> headers = new HashMap<>();
        headers.put("Hash", headerParam);
        return PaymentGatewayHelper.sendHTTPRequest("QUERY_STRING", "GET", requestURL, headers, requestParams, "InterswitchWebPAY");
    }

}
