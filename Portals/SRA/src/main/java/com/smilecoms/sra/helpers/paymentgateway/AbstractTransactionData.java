/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.helpers.paymentgateway;

import com.smilecoms.commons.sca.Sale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sabza
 */
public abstract class AbstractTransactionData implements IPGWTransactionData {
    protected Logger log = LoggerFactory.getLogger(this.getClass());
    protected String paymentGatewayPostURL;
    protected Sale paymentGatewaySale;
    protected String itemDescription;
    protected String gatewayURLData;

    public AbstractTransactionData(Sale sale, String itemDescription) {
        this.paymentGatewaySale = sale;
        if (itemDescription.equals("AIRTIME")) {
            this.itemDescription = "SmileAirtime";
        } else {
            this.itemDescription = itemDescription;
        }
    }

    @Override
    public String getAmountInMajorCurrencyUnit() {
        return PaymentGatewayManager.getStringRoundedForBank(paymentGatewaySale.getSaleTotalCentsIncl() / 100);
    }

    @Override
    public void setPaymentGatewaySale(Sale sale) {
        this.paymentGatewaySale = sale;
    }

    @Override
    public Sale getPaymentGatewaySale() {
        return paymentGatewaySale;
    }

    @Override
    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    @Override
    public String getItemDescription() {
        return itemDescription;
    }

    @Override
    public void setPaymentGatewayPostURL(String paymentGatewayPostURL) {
        this.paymentGatewayPostURL = paymentGatewayPostURL;
    }

    @Override
    public String getPaymentGatewayPostURL() {
        return paymentGatewayPostURL;
    }

    @Override
    public void setAmountInMajorCurrencyUnit(double amountInCents) {
        PaymentGatewayManager.getStringRoundedForBank(amountInCents / 100);
    }

    @Override
    public boolean isPaymentPageIFrame() {
        return false;
    }

    @Override
    public String getGatewayURLData() {
        return gatewayURLData;
    }

    @Override
    public void setGatewayURLData(String gatewayURLData) {
        this.gatewayURLData = gatewayURLData;
    }
    
    @Override
    public boolean isAutoRedirect() {
        return false;
    }
}
