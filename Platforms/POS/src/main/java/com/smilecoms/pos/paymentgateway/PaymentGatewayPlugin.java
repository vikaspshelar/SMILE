/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos.paymentgateway;

import com.smilecoms.pos.db.model.Sale;
import javax.persistence.EntityManager;

/**
 *
 * @author paul
 */
public interface PaymentGatewayPlugin {

    public PaymentGatewayResult getPaymentGatewayResult(Sale dbSale);

    public void init(EntityManager em);

    public String getGatewayURL(Sale dbSale);

    public String getLandingURL(Sale dbSale);

    public String getGatewayURLData(Sale dbSale);
    
    public String getName();

    public long getAccountId();
    
    public boolean isUp();

    public void processPaymentNotification(int saleId, double cashReciepted, String transactionId, String paymentGatewayExtraData) throws Exception;
}
