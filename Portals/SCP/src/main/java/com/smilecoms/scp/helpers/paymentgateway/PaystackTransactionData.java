/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.scp.helpers.paymentgateway;

import com.smilecoms.commons.sca.Sale;

/**
 *
 * @author sabza
 */
public class PaystackTransactionData extends AbstractTransactionData {

    public PaystackTransactionData(Sale sale, String itemDescription) {
        super(sale, itemDescription);
        this.paymentGatewayPostURL = sale.getPaymentGatewayURL();
    }

    @Override
    public boolean isAutoRedirect() {
        return true;
    }
}
