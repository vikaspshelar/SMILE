/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.model;

/**
 *
 * @author rajeshkumar
 */
public class PaymentGateway {
    
    private String gatewayCode;
    private boolean isup;

    public String getGatewayCode() {
        return gatewayCode;
    }

    public void setGatewayCode(String code) {
        this.gatewayCode = code;
    }

    public boolean isIsup() {
        return isup;
    }

    public void setIsup(boolean isup) {
        this.isup = isup;
    }

    @Override
    public String toString() {
        return "PaymentGateway{" + "gatewayCode=" + gatewayCode + ", isup=" + isup + '}';
    }
    
    
}
