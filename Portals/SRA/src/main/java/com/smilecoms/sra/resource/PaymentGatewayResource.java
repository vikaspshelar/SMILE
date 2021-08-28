/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.resource;

import com.smilecoms.commons.sca.beans.SaleBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sabza
 */
public class PaymentGatewayResource extends Resource {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayResource.class);
    private SaleBean sale;

    public static PaymentGatewayResource getPaymentGatewayResourceBySaleId(int saleId) {
        return new PaymentGatewayResource(SaleBean.getSaleById(saleId));
    }

    public PaymentGatewayResource() {
    }

    public PaymentGatewayResource(SaleBean sale) {
        this.sale = sale;
    }

    public SaleBean getSale() {
        log.debug("In getSale");
        return sale;
    }

    public void setSale(SaleBean sale) {
        log.debug("In setSale");
        this.sale = sale;
    }
}
