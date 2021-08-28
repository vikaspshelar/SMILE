/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.model;

import java.util.Objects;

/**
 *
 * @author xolaniM
 */
public class RechargeOffer {
    String productInstanceId;
    int campaignId;

    public RechargeOffer() {
    }

    public RechargeOffer(String productInstanceId, int campaignId) {
        this.productInstanceId = productInstanceId;      
        this.campaignId = campaignId;
      
    }
    
    public String getProductInstanceId() {
        return productInstanceId;
    }

    public void setProductInstanceId(String productInstanceId) {
        this.productInstanceId = productInstanceId;
    }

    public int getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(int campaignId) {
        this.campaignId = campaignId;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RechargeOffer other = (RechargeOffer) obj;
        if (this.campaignId != other.campaignId) {
            return false;
        }
        if (!Objects.equals(this.productInstanceId, other.productInstanceId)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "RechargeOffer{" + "productInstanceId=" + productInstanceId + ", campaignId=" + campaignId + '}';
    }
    
    
    
}
