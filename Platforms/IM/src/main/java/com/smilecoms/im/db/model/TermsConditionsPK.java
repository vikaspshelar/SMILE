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

/**
 *
 * @author paul
 */
@Embeddable
public class TermsConditionsPK implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Column(name = "CUSTOMER_PROFILE_ID")
    private int customerProfileId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "T_C_RESOURCE_KEY")
    private String tCResourceKey;

    public TermsConditionsPK() {
    }

    public TermsConditionsPK(int customerProfileId, String tCResourceKey) {
        this.customerProfileId = customerProfileId;
        this.tCResourceKey = tCResourceKey;
    }

    public int getCustomerProfileId() {
        return customerProfileId;
    }

    public void setCustomerProfileId(int customerProfileId) {
        this.customerProfileId = customerProfileId;
    }

    public String getTCResourceKey() {
        return tCResourceKey;
    }

    public void setTCResourceKey(String tCResourceKey) {
        this.tCResourceKey = tCResourceKey;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (int) customerProfileId;
        hash += (tCResourceKey != null ? tCResourceKey.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof TermsConditionsPK)) {
            return false;
        }
        TermsConditionsPK other = (TermsConditionsPK) object;
        if (this.customerProfileId != other.customerProfileId) {
            return false;
        }
        if ((this.tCResourceKey == null && other.tCResourceKey != null) || (this.tCResourceKey != null && !this.tCResourceKey.equals(other.tCResourceKey))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.im.db.model.TermsConditionsPK[ customerProfileId=" + customerProfileId + ", tCResourceKey=" + tCResourceKey + " ]";
    }
    
}
