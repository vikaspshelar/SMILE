/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.helpers.paymentgateway;

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
    PAYSTACK("Paystack"),
    CELLULANT("Cellulant"),
    SELCOM("Selcom");
    
    private final String gatewayCode;

    GatewayCodes(String gatewayCode) {
        this.gatewayCode = gatewayCode;
    }
    
    public String getGatewayCode() { return gatewayCode; }
}
