/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.resource;

import com.smilecoms.commons.sca.StProductInstanceLookupVerbosity;
import com.smilecoms.commons.sca.beans.ProductBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author lesiba
 */
public class ProductResource extends Resource {

    private static final Logger log = LoggerFactory.getLogger(ProductResource.class);
    private ProductBean product;

    public ProductResource() {
    }

    public ProductResource(int productInstanceId) {
        product = ProductBean.getProductInstanceById(productInstanceId);
    }

    public ProductResource(int productInstanceId, StProductInstanceLookupVerbosity verbosity) {
        product = ProductBean.getProductInstanceById(productInstanceId, verbosity);
    }

    public ProductResource(ProductBean product) {
        this.product = product;
    }

    public ProductBean getProduct() {
        return product;
    }

    public void setProduct(ProductBean product) {
        this.product = product;
    }
}
