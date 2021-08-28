/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos.paymentgateway;

import com.smilecoms.commons.sca.NewTTIssue;
import com.smilecoms.pos.POSManager;
import com.smilecoms.pos.db.model.Sale;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;

/**
 *
 * @author sabza
 */
public class InterswitchPaymentGateway extends AbstractPaymentGateway implements PaymentGatewayPlugin {

    EntityManager em = null;

    @Override
    public PaymentGatewayResult getPaymentGatewayResult(Sale dbSale) {
        PaymentGatewayResult res = new PaymentGatewayResult();
        res.setTryAgainLater(true);
        res.setSuccess(false);
        return res;
    }

    @Override
    public void init(EntityManager em) {
        this.em = em;
    }

    @Override
    public String getGatewayURL(Sale dbSale) {
        return "Not supported yet.";
    }

    @Override
    public String getLandingURL(Sale dbSale) {
        return "Not supported yet.";
    }

    @Override
    public String getGatewayURLData(Sale dbSale) {
        return "Not supported yet.";
    }

    @Override
    public long getAccountId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void processPaymentNotification(int saleId, double cashReciepted, String transactionId, String paymentGatewayExtraData) throws Exception {
        List<Integer> saleAsList = new ArrayList<>();
        saleAsList.add(saleId);
        new POSManager().processPayment(em, saleAsList, cashReciepted, transactionId, "PD", true, "Interswitch");
    }

    @Override
    public boolean isUp() {
        return true;
    }
    
    @Override
    public String getName() {
        return "Interswitch";
    }
    
    @Override
    public void addPaymentGatewaySpecificsOnTicket(NewTTIssue tt, String errorMessage) {
        
    }
}
