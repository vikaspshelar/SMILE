/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.paymentgateway.helpers;

/**
 *
 * @author rajeshkumar
 */
public class PaymentGatewayResult {
        
    private String paymentGatewayTransactionId;
    private boolean success;
    private boolean tryAgainLater;
    private String gatewayResponse;
    double transferredAmountCents;
    private String info;

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public double getTransferredAmountCents() {
        return transferredAmountCents;
    }

    public void setTransferredAmountCents(double transferredAmountCents) {
        this.transferredAmountCents = transferredAmountCents;
    }

    public boolean mustTryAgainLater() {
        return tryAgainLater;
    }

    public void setTryAgainLater(boolean tryAgainLater) {
        this.tryAgainLater = tryAgainLater;
    }

    
    // Whatever the gateway will later pass us in their dump file that can be used for auto matching
    public String getPaymentGatewayTransactionId() {
        return paymentGatewayTransactionId == null ? "" : paymentGatewayTransactionId;
    }

    public void setPaymentGatewayTransactionId(String paymentGatewayTransactionId) {
        this.paymentGatewayTransactionId = paymentGatewayTransactionId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getGatewayResponse() {
        return gatewayResponse == null ? "" : gatewayResponse;
    }

    public void setGatewayResponse(String gatewayResponse) {
        this.gatewayResponse = gatewayResponse;
    }
}
