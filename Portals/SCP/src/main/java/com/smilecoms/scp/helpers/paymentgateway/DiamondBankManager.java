/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.scp.helpers.paymentgateway;

import com.smilecoms.commons.sca.Sale;

/**
 *
 * @author sabza
 */
public class DiamondBankManager extends PaymentGatewayManager {

    @Override
    public IPGWTransactionData startTransaction(Sale data, String itemDescription) throws Exception {
        IPGWTransactionData transaction = new DiamondBankTransactionData(data, itemDescription);
        replaceParamValuePlaceholder(transaction);
        return transaction;
    }
}
