/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos;

import com.smilecoms.commons.sca.AVP;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.ServiceInstanceQuery;
import com.smilecoms.commons.sca.StServiceInstanceLookupVerbosity;
import com.smilecoms.commons.util.Javassist;
import com.smilecoms.xml.schema.pos.Sale;
import javax.persistence.EntityManager;

/**
 *
 * @author paul
 */
public class FeeCalculator {

    private String deliveryModel;
    private String transactionModel;
    private double deliveryCents;
    private double transactionCents;

    public FeeCalculator(Sale xmlSale, EntityManager em) throws Exception {

        if (xmlSale.getPaymentMethod().equals(POSManager.PAYMENT_METHOD_CLEARING_BUREAU)
                || xmlSale.getPaymentMethod().equals(POSManager.PAYMENT_METHOD_PAYMENT_GATEWAY)) {
            ServiceInstanceQuery siq = new ServiceInstanceQuery();
            siq.setVerbosity(StServiceInstanceLookupVerbosity.MAIN_SVCAVP);
            siq.setAccountId(xmlSale.getSalesPersonAccountId());
            ServiceInstance si = SCAWrapper.getAdminInstance().getServiceInstance(siq);
            for (AVP avp : si.getAVPs()) {

                switch (avp.getAttribute()) {
                    case "TxFeeCalculation":
                        transactionCents = (double) Javassist.runCode(new Class[]{this.getClass(), xmlSale.getClass()}, avp.getValue(), xmlSale);
                        break;
                    case "GLType":
                        transactionModel = avp.getValue();
                        break;
                }
            }
        }
    }

    public String getDeliveryFeeModel() {
        return deliveryModel;
    }

    public double getDeliveryFeeCents() {
        return deliveryCents;
    }

    public String getTransactionFeeModel() {
        return transactionModel;
    }

    public double getTransactionFeeCents() {
        return transactionCents;
    }
}
