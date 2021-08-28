/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sapi.model;


import java.util.GregorianCalendar;
import java.util.List;


/**
 *
 * @author bhaskarhg
 */
public class ProductInstance {
    
    
    protected int productInstanceId;
    
    protected int productSpecificationId;
    
    protected int customerId;
    
    protected int organisationId;
    
    protected String segment;
    
    protected GregorianCalendar createdDateTime;
    
    protected List<ProductServiceInstanceMapping> productServiceInstanceMappings;
    
    protected String promotionCode;
    
    protected String friendlyName;
    
    protected int logicalId;
    
    protected String physicalId;
    
    protected int createdByOrganisationId;
    
    protected int createdByCustomerProfileId;
    
    protected List<CampaignData> campaigns;
    
    protected String lastDevice;
    
    protected String referralCode;
    
    protected GregorianCalendar firstActivityDateTime;
    
    protected GregorianCalendar lastActivityDateTime;
    
    protected String status;

    public int getProductInstanceId() {
        return productInstanceId;
    }

    public void setProductInstanceId(int productInstanceId) {
        this.productInstanceId = productInstanceId;
    }

    public int getProductSpecificationId() {
        return productSpecificationId;
    }

    public void setProductSpecificationId(int productSpecificationId) {
        this.productSpecificationId = productSpecificationId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public int getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(int organisationId) {
        this.organisationId = organisationId;
    }

    public String getSegment() {
        return segment;
    }

    public void setSegment(String segment) {
        this.segment = segment;
    }

    public GregorianCalendar getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(GregorianCalendar createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public List<ProductServiceInstanceMapping> getProductServiceInstanceMappings() {
        return productServiceInstanceMappings;
    }

    public void setProductServiceInstanceMappings(List<ProductServiceInstanceMapping> productServiceInstanceMappings) {
        this.productServiceInstanceMappings = productServiceInstanceMappings;
    }

    public String getPromotionCode() {
        return promotionCode;
    }

    public void setPromotionCode(String promotionCode) {
        this.promotionCode = promotionCode;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public int getLogicalId() {
        return logicalId;
    }

    public void setLogicalId(int logicalId) {
        this.logicalId = logicalId;
    }

    public String getPhysicalId() {
        return physicalId;
    }

    public void setPhysicalId(String physicalId) {
        this.physicalId = physicalId;
    }

    public int getCreatedByOrganisationId() {
        return createdByOrganisationId;
    }

    public void setCreatedByOrganisationId(int createdByOrganisationId) {
        this.createdByOrganisationId = createdByOrganisationId;
    }

    public int getCreatedByCustomerProfileId() {
        return createdByCustomerProfileId;
    }

    public void setCreatedByCustomerProfileId(int createdByCustomerProfileId) {
        this.createdByCustomerProfileId = createdByCustomerProfileId;
    }

    public List<CampaignData> getCampaigns() {
        return campaigns;
    }

    public void setCampaigns(List<CampaignData> campaigns) {
        this.campaigns = campaigns;
    }

    public String getLastDevice() {
        return lastDevice;
    }

    public void setLastDevice(String lastDevice) {
        this.lastDevice = lastDevice;
    }

    public String getReferralCode() {
        return referralCode;
    }

    public void setReferralCode(String referralCode) {
        this.referralCode = referralCode;
    }

    public GregorianCalendar getFirstActivityDateTime() {
        return firstActivityDateTime;
    }

    public void setFirstActivityDateTime(GregorianCalendar firstActivityDateTime) {
        this.firstActivityDateTime = firstActivityDateTime;
    }

    public GregorianCalendar getLastActivityDateTime() {
        return lastActivityDateTime;
    }

    public void setLastActivityDateTime(GregorianCalendar lastActivityDateTime) {
        this.lastActivityDateTime = lastActivityDateTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
       
}
