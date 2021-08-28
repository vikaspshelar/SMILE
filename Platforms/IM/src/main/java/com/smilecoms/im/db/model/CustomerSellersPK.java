/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.db.model;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

@Embeddable
public class CustomerSellersPK implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Column(name = "SELLER_PROFILE_ID")
    private int sellerProfileId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CUSTOMER_PROFILE_ID")
    private int customerProfileId;

    public CustomerSellersPK() {
    }

    public CustomerSellersPK(int sellerProfileId, int customerProfileId) {
        this.sellerProfileId = sellerProfileId;
        this.customerProfileId = customerProfileId;
    }

    public int getSellerProfileId() {
        return sellerProfileId;
    }

    public void setSellerProfileId(int sellerProfileId) {
        this.sellerProfileId = sellerProfileId;
    }

    public int getCustomerProfileId() {
        return customerProfileId;
    }

    public void setCustomerProfileId(int customerProfileId) {
        this.customerProfileId = customerProfileId;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + this.sellerProfileId;
        hash = 37 * hash + this.customerProfileId;
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
        final CustomerSellersPK other = (CustomerSellersPK) obj;
        if (this.sellerProfileId != other.sellerProfileId) {
            return false;
        }
        if (this.customerProfileId != other.customerProfileId) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "CustomerSellersPK{" + "sellerProfileId=" + sellerProfileId + ", customerProfileId=" + customerProfileId + '}';
    }
    
}
