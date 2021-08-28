/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.resource;

import com.smilecoms.commons.sca.beans.SaleBean;
import org.slf4j.*;

/**
 * REST Web Service
 *
 * @author paul
 */
public class SaleResource extends Resource {

    private static final Logger log = LoggerFactory.getLogger(SaleResource.class);
    private SaleBean sale;

    public static SaleResource getSaleResourceBySaleId(int saleId) {
        return new SaleResource(SaleBean.getSaleById(saleId));
    }
    
    public static SaleResource getSaleResourceBySaleLineId(int saleLineId) {
        return new SaleResource(SaleBean.getSaleByLineId(saleLineId));
    }
    
    public static SaleResource getSaleResourceByOrderData(String orderData) {
        return new SaleResource(SaleBean.getSaleByPurchaseOrder(orderData));
    }
    
    public SaleResource() {
    }

    public SaleResource(SaleBean sale) {
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
