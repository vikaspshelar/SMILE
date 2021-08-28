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
 * @author sabza
 */
public class DummyTransactionData extends AbstractTransactionData {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public DummyTransactionData(Sale sale, String itemDescription) {
        super(sale, itemDescription);
        log.warn("Initialising DummyTransactionData configs");
        this.paymentGatewayPostURL = BaseUtils.getSubProperty("env.pgw.dummy.config", "PaymentGatewayPostURL");
    }

    @Override
    public String getGatewayURLData() {
        StringBuilder sb = new StringBuilder();
        String urlData = paymentGatewaySale.getPaymentGatewayURLData();
        String[] urlBits = urlData.split(":");
        String[] avps = urlBits[1].split(",");

        boolean first = true;
        for (String avp : avps) {

            String[] params = avp.split("=");
            String name = params[0];
            String value = params[1];
            if (name.equals("prod")) {
                value = itemDescription;
            }
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(name);
            sb.append("=");
            sb.append(value);
        }
        this.gatewayURLData = sb.toString();
        return gatewayURLData;
    }

}
