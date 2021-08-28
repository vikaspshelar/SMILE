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
public class GemexManager extends PaymentGatewayManager {

    @Override
    public IPGWTransactionData startTransaction(Sale data, String itemDescription) throws Exception {
        IPGWTransactionData transaction = new GemexTransactionData(data, itemDescription);
        replaceParamValuePlaceholder(transaction);
        return transaction;
    }

}
