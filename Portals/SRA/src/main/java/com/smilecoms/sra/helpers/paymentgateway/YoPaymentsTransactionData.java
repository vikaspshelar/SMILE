/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.helpers.paymentgateway;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.Sale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * @author rajeshkumar
 */
public class YoPaymentsTransactionData extends AbstractTransactionData {
    
    private static final Logger log = LoggerFactory.getLogger(YoPaymentsTransactionData.class);

    public YoPaymentsTransactionData(Sale sale, String itemDescription) {
        super(sale, itemDescription);
        this.paymentGatewayPostURL = BaseUtils.getSubProperty("env.pgw.yopayments.config", "PaymentGatewayPostURL");
    }

    @Override
    public String getGatewayURLData() {

        if (gatewayURLData == null) {
            int httpMethodLen = "POST:".length();
            StringBuilder sb = new StringBuilder();
            String urlData = paymentGatewaySale.getPaymentGatewayURLData();
            log.debug("urlData :"+urlData);
            String[] avps = urlData.substring(httpMethodLen).split(",");

            boolean first = true;
            for (String avp : avps) {
                String[] params = avp.split("=");
                if(params.length < 2){
                    log.debug("Parameter not found for [{}]",params);
                    continue;
                }
                String name = params[0];
                String value = params[1];
                if (name.equals("narrative") || name.equals("provider_reference_text")) {
                    value = itemDescription;
                }
                if (first) {
                    first = false;
                } else {
                    sb.append(",");
                }
                sb.append(name.trim());
                sb.append("=");
                sb.append(value.trim());
            }
            this.gatewayURLData = sb.toString();
        }

        return gatewayURLData;
    }

}
