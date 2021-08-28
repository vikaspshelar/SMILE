/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.sca.beans;

import com.smilecoms.commons.sca.CampaignData;
import com.smilecoms.commons.sca.ProductInstance;
import com.smilecoms.commons.sca.ProductOrder;
import com.smilecoms.commons.sca.ProductServiceInstanceMapping;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.StAction;
import com.smilecoms.commons.sca.StProductInstanceLookupVerbosity;
import com.smilecoms.commons.util.Utils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class ProductBean {

    private final ProductInstance productInstance;
    private static final Logger log = LoggerFactory.getLogger(ProductBean.class);

    public ProductBean() {
        productInstance = new ProductInstance();
    }

    public static ProductBean getProductInstanceById(int productInstanceId) {
        return new ProductBean(UserSpecificCachedDataHelper.getProductInstance(productInstanceId, StProductInstanceLookupVerbosity.MAIN_SVC_SVCAVP));
    }
    
    public static ProductBean getProductInstanceById(int productInstanceId, StProductInstanceLookupVerbosity verbosity) {
        return new ProductBean(UserSpecificCachedDataHelper.getProductInstance(productInstanceId, verbosity));
    }

    public static ProductBean modifyProductInstance(ProductBean productInstance) {
        ProductInstance existingPI = UserSpecificCachedDataHelper.getProductInstance(productInstance.getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN);
        ProductOrder po = new ProductOrder();
        po.setAction(StAction.UPDATE);
        po.setOrganisationId(existingPI.getOrganisationId());
        po.setCustomerId(existingPI.getCustomerId());
        po.setSegment(existingPI.getSegment());
        po.setFriendlyName(productInstance.getFriendlyName());
        po.setProductInstanceId(productInstance.getProductInstanceId());
        SCAWrapper.getUserSpecificInstance().processOrder(po);
        return getProductInstanceById(productInstance.getProductInstanceId());
    }

    public static List<ProductBean> wrap(List<ProductInstance> productInstances) {
        List<ProductBean> products = new ArrayList<>();
        for (ProductInstance productInstance : productInstances) {
            products.add(new ProductBean(productInstance));
        }
        return products;
    }

    public ProductBean(ProductInstance productInstance) {
        this.productInstance = productInstance;
    }

    @XmlElement
    public String getProductName() {
        return NonUserSpecificCachedDataHelper.getProductSpecification(getProductSpecificationId()).getName();
    }

    @XmlElement
    public String getProductDescription() {
        return NonUserSpecificCachedDataHelper.getProductSpecification(getProductSpecificationId()).getDescription();
    }

    @XmlElement
    public String getProductPhoneNumber() {
        return UserSpecificCachedDataHelper.getProductInstancePhoneNumber(productInstance.getProductInstanceId());
    }

    @XmlElement
    public String getProductICCID() {
        return UserSpecificCachedDataHelper.getProductInstanceICCID(productInstance.getProductInstanceId());
    }

    @XmlElement
    public int getProductInstanceId() {
        return productInstance.getProductInstanceId();
    }

    @XmlElement
    public int getProductSpecificationId() {
        return productInstance.getProductSpecificationId();
    }

    @XmlElement
    public int getCustomerId() {
        return productInstance.getCustomerId();
    }

    @XmlElement
    public int getOrganisationId() {
        return productInstance.getOrganisationId();
    }

    @XmlElement
    public String getSegment() {
        return productInstance.getSegment();
    }

    @XmlElement
    public long getCreatedDateTime() {
        return Utils.getJavaDate(productInstance.getCreatedDateTime()).getTime();
    }
    
    @XmlElement
    public long getFirstActivityDate() {
        if(productInstance.getFirstActivityDateTime() == null){
            return 0;
        }
        return Utils.getJavaDate(productInstance.getFirstActivityDateTime()).getTime();
    }
    
    @XmlElement
    public long getLastActivityDate() {
        if(productInstance.getLastActivityDateTime() == null){
            return 0;
        }
        return Utils.getJavaDate(productInstance.getLastActivityDateTime()).getTime();
    }

    @XmlElement
    public String getPromotionCode() {
        return productInstance.getPromotionCode();
    }

    @XmlElement
    public String getLastDevice() {
        return productInstance.getLastDevice();
    }

    @XmlElement
    public String getFriendlyName() {
        return productInstance.getFriendlyName();
    }

    @XmlElement
    public List<ServiceBean> getServices() {
        List<ServiceBean> services = new ArrayList<>();
        for (ProductServiceInstanceMapping mapping : productInstance.getProductServiceInstanceMappings()) {
            services.add(new ServiceBean(mapping.getServiceInstance()));
        }
        return services;
    }
    
    @XmlElement
    public List<CampaignData> getCampaigns() {
        return productInstance.getCampaigns();
    }

    public void setProductInstanceId(int productInstanceId) {
        productInstance.setProductInstanceId(productInstanceId);
    }
    
    public void setFriendlyName(String friendlyName) {
        productInstance.setFriendlyName(friendlyName);
    }
}
