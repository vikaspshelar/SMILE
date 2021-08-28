/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.db.model;

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
public class RatePlanAvpPK implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Column(name = "RATE_PLAN_ID")
    private int ratePlanId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ATTRIBUTE")
    private String attribute;

    public RatePlanAvpPK() {
    }

    public RatePlanAvpPK(int ratePlanId, String attribute) {
        this.ratePlanId = ratePlanId;
        this.attribute = attribute;
    }

    public int getRatePlanId() {
        return ratePlanId;
    }

    public void setRatePlanId(int ratePlanId) {
        this.ratePlanId = ratePlanId;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (int) ratePlanId;
        hash += (attribute != null ? attribute.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof RatePlanAvpPK)) {
            return false;
        }
        RatePlanAvpPK other = (RatePlanAvpPK) object;
        if (this.ratePlanId != other.ratePlanId) {
            return false;
        }
        if ((this.attribute == null && other.attribute != null) || (this.attribute != null && !this.attribute.equals(other.attribute))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.bm.db.model.RatePlanAvpPK[ ratePlanId=" + ratePlanId + ", attribute=" + attribute + " ]";
    }
    
}
