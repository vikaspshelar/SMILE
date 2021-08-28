/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.helpers.paymentgateway;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.Sale;

/**
 *
 * @author sabza
 */
public class DiamondBankTransactionData extends AbstractTransactionData {

    public DiamondBankTransactionData(Sale sale, String itemDescription) {
        super(sale, itemDescription);
        this.paymentGatewayPostURL = BaseUtils.getSubProperty("env.pgw.diamond.config", "PaymentGatewayPostURL");
    }

    @Override
    public String getGatewayURLData() {

        if (gatewayURLData == null) {
            int httpMethodLen = "POST:".length();
            StringBuilder sb = new StringBuilder();
            String urlData = paymentGatewaySale.getPaymentGatewayURLData();
            String[] avps = urlData.substring(httpMethodLen).split(",");

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
                sb.append(name.trim());
                sb.append("=");
                sb.append(value.trim());
            }
            this.gatewayURLData = sb.toString();
        }

        return gatewayURLData;
    }

}
