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
public class CellulantManager extends PaymentGatewayManager {

    @Override
    public IPGWTransactionData startTransaction(Sale sale, String itemDescription) throws Exception {
        IPGWTransactionData data = new CellulantTransactionData(sale, itemDescription);
        return data;
    }
    
}
