/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.scp.helpers.paymentgateway;

import com.smilecoms.commons.sca.Sale;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 *
 * @author sabza
 */
public class CellulantTransactionData extends AbstractTransactionData {

    private static final String ENCODING = "UTF-8";

    public CellulantTransactionData(Sale sale, String itemDescription) {
        super(sale, itemDescription);
        String url = this.paymentGatewaySale.getPaymentGatewayURL();

        int httpMethodLen = "GET:".length();
        StringBuilder sb = new StringBuilder();
        String urlData = paymentGatewaySale.getPaymentGatewayURLData();
        String[] avps = urlData.substring(httpMethodLen).split(",");

        //paymentGatewayPostURL
        boolean first = true;
        for (String avp : avps) {
            log.debug("Data to split is: {}", avp);
            String[] params = avp.split("=");
            String name = params[0];
            String value = params[1];
            if (first) {
                first = false;
            } else {
                sb.append("&");
            }
            sb.append(name.trim());
            sb.append("=");
            sb.append(percentEncode(value.trim()));
        }
        this.paymentGatewayPostURL = url + ((!url.contains("?")) ? "?" : "&") + sb.toString();
    }

    @Override
    public String getGatewayURLData() {

        if (gatewayURLData == null) {
            int httpMethodLen = "GET:".length();
            StringBuilder sb = new StringBuilder();
            String urlData = paymentGatewaySale.getPaymentGatewayURLData();
            String[] avps = urlData.substring(httpMethodLen).split(",");

            boolean first = true;
            for (String avp : avps) {
                log.debug("Data to split is: {}", avp);
                String[] params = avp.split("=");
                String name = params[0];
                String value = params[1];
                if (first) {
                    first = false;
                } else {
                    sb.append("&");
                }
                sb.append(name.trim());
                sb.append("=");
                sb.append(value.trim());
            }
            this.gatewayURLData = sb.toString();
        }

        return gatewayURLData;
    }

    @Override
    public boolean isAutoRedirect() {
        return true;
    }

    private String percentEncode(String s) {
        if (s == null) {
            return "";
        }
        log.debug("Validate encoding for: string: {}", s);
        try {
            return URLEncoder.encode(s, ENCODING).replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
        } catch (UnsupportedEncodingException wow) {
            throw new RuntimeException(wow.getMessage(), wow);
        }
    }
}
