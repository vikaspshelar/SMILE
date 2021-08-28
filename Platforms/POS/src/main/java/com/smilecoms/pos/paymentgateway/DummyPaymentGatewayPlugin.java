/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos.paymentgateway;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.NewTTIssue;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.pos.db.model.Sale;
import java.util.Random;
import javax.persistence.EntityManager;

/**
 *
 * @author paul
 */
public class DummyPaymentGatewayPlugin extends AbstractPaymentGateway implements PaymentGatewayPlugin {

    EntityManager em = null;

    @Override
    public PaymentGatewayResult getPaymentGatewayResult(Sale dbSale) {

        Random r = new Random(System.currentTimeMillis());
        PaymentGatewayResult res = new PaymentGatewayResult();
        res.setPaymentGatewayTransactionId(Utils.getUUID());
        int random = r.nextInt(100);
        if (random >= 60) {
            // Succeed 40% of the time
            res.setSuccess(true);
            res.setGatewayResponse("Succeeded");
            res.setInfo("Mastercard");
            res.setTransferredAmountCents(dbSale.getSaleTotalCentsIncl().doubleValue());
        } else {
            res.setSuccess(false);
            int anotherrandom = r.nextInt(100);
            if (anotherrandom >= 2) {
                // Try again later 98% of the time
                res.setTryAgainLater(true);
                res.setGatewayResponse("Try again later - " + random);
            } else {
                res.setGatewayResponse("Failure");
                res.setTryAgainLater(false);
            }
        }
        return res;
    }

    @Override
    public void init(EntityManager em) {
        this.em = em;
    }

    @Override
    public String getGatewayURL(Sale dbSale) {
        return "http://www.hello.world";
    }

    @Override
    public String getGatewayURLData(Sale dbSale) {
        return "POST: a=111,b=222,c=333";
    }

    @Override
    public String getLandingURL(Sale dbsale) {
        if (dbsale.getLandingURL() == null) {
            dbsale.setLandingURL("http://nowhere.abc");
        }
        if (dbsale.getLandingURL().contains("?")) {
            return dbsale.getLandingURL() + "&" + "saleId=" + dbsale.getSaleId();
        } else {
            return dbsale.getLandingURL() + "?" + "saleId=" + dbsale.getSaleId();
        }
    }

    @Override
    public long getAccountId() {
        return Long.valueOf(BaseUtils.getSubProperty("env.pgw.dummy.config", "AccountId"));
    }

    @Override
    public void processPaymentNotification(int saleId, double cashReciepted, String transactionId, String paymentGatewayExtraData) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isUp() {
        return true;
    }

    @Override
    public String getName() {
        return "Dummy";
    }

    @Override
    public void addPaymentGatewaySpecificsOnTicket(NewTTIssue tt, String errorMessage) {

    }

}
