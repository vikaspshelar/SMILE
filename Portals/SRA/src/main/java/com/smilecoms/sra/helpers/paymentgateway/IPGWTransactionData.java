/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.helpers.paymentgateway;

import com.smilecoms.commons.sca.Sale;

/**
 *
 * @author sabza
 */
public interface IPGWTransactionData {

    public void setPaymentGatewaySale(Sale sale);

    public Sale getPaymentGatewaySale();

    public void setItemDescription(String itemDescription);

    public String getItemDescription();

    public void setPaymentGatewayPostURL(String paymentGatewayPostURL);

    public String getPaymentGatewayPostURL();

    public String getAmountInMajorCurrencyUnit();

    public void setAmountInMajorCurrencyUnit(double amountInCents);

    public boolean isPaymentPageIFrame();

    public String getGatewayURLData();

    public void setGatewayURLData(String gatewayURLData);

    public boolean isAutoRedirect();
}
