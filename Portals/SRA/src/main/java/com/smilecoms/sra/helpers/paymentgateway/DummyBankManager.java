/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.helpers.paymentgateway;

import com.smilecoms.commons.sca.Sale;

/**
 *
 * @author sabza
 */
public class DummyBankManager  extends PaymentGatewayManager {
    @Override
    public IPGWTransactionData startTransaction(Sale data, String itemDescription) throws Exception {
        return new DummyTransactionData(data, itemDescription);
    }
}
