/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.scp.helpers.paymentgateway;

/**
 *
 * @author sabza
 */
public enum GatewayCodes {

    DIAMOND_BANK("Diamond"),
    DUMMY_BANK("Dummy"),
    GEMEX("Gemex"),
    PESAPAL("Pesapal"),
    ACCESS_BANK("Access"),
    WEB_PAY_DIRECT("WebPAYDirect"),
    YO_PAYMENTS("YoPayments"),
    SELCOM("Selcom"),
    PAYSTACK("Paystack"),
    CELLULANT("Cellulant");
    
    private final String gatewayCode;

    GatewayCodes(String gatewayCode) {
        this.gatewayCode = gatewayCode;
    }
    
    public String getGatewayCode() { return gatewayCode; }
}
