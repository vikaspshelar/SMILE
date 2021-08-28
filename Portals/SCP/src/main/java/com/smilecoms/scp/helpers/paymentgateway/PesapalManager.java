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
public class PesapalManager extends PaymentGatewayManager {

    @Override
    public IPGWTransactionData startTransaction(Sale saleData, String itemDescription) throws Exception {
        IPGWTransactionData data = new PesapalTransactionData(saleData, itemDescription);
        return data;
    }
}
