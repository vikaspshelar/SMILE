/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.sca.beans;

import com.smilecoms.commons.sca.ProductServiceSpecificationMapping;
import com.smilecoms.commons.sca.ProductSpecification;
import com.smilecoms.commons.util.Utils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;

/**
 *
 * @author paul
 */
public class ProductSpecificationBean extends BaseBean {

    private ProductSpecification scaProductSpecification;

    public ProductSpecificationBean() {
    }
    
    public static ProductSpecificationBean wrap(ProductSpecification scaProductSpecification) {
        return new ProductSpecificationBean(scaProductSpecification);
    }
    
    public static List<ProductSpecificationBean> wrap(List<ProductSpecification> scaProductSpecifications) {
        List<ProductSpecificationBean> beans = new ArrayList();
        for (ProductSpecification spec : scaProductSpecifications) {
            beans.add(wrap(spec));
        }
        return beans;
    }
    
    private ProductSpecificationBean(ProductSpecification scaProductSpecification) {
        this.scaProductSpecification = scaProductSpecification;
    }

    @XmlElement
    public int getProductSpecificationId() {
        return scaProductSpecification.getProductSpecificationId();
    }

    @XmlElement
    public String getName() {
        return scaProductSpecification.getName();
    }

    @XmlElement
    public String getDescription() {
        return scaProductSpecification.getDescription();
    }

    @XmlElement
    public long getAvailableFrom() {
        return Utils.getJavaDate(scaProductSpecification.getAvailableFrom()).getTime();
    }

    @XmlElement
    public long getAvailableTo() {
        return Utils.getJavaDate(scaProductSpecification.getAvailableTo()).getTime();
    }

    @XmlElement
    public String getProvisionRoles() {
        return scaProductSpecification.getProvisionRoles();
    }

    @XmlElement
    public String getSegments() {
        return scaProductSpecification.getSegments();
    }

    @XmlElement
    public List<ProductServiceSpecificationMapping> getProductServiceSpecificationMappings() {
        return scaProductSpecification.getProductServiceSpecificationMappings();
    }

}
