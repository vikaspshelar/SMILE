/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.db.model;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "rate_plan_avp")
@NamedQueries({
    @NamedQuery(name = "RatePlanAvp.findAll", query = "SELECT r FROM RatePlanAvp r")})
public class RatePlanAvp implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected RatePlanAvpPK ratePlanAvpPK;
    @Basic(optional = false)
    @NotNull
    @Column(name = "VALUE")
    private String value;

    public RatePlanAvp() {
    }

    public RatePlanAvp(RatePlanAvpPK ratePlanAvpPK) {
        this.ratePlanAvpPK = ratePlanAvpPK;
    }

    public RatePlanAvp(RatePlanAvpPK ratePlanAvpPK, String value) {
        this.ratePlanAvpPK = ratePlanAvpPK;
        this.value = value;
    }

    public RatePlanAvp(int ratePlanId, String attribute) {
        this.ratePlanAvpPK = new RatePlanAvpPK(ratePlanId, attribute);
    }

    public RatePlanAvpPK getRatePlanAvpPK() {
        return ratePlanAvpPK;
    }

    public void setRatePlanAvpPK(RatePlanAvpPK ratePlanAvpPK) {
        this.ratePlanAvpPK = ratePlanAvpPK;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (ratePlanAvpPK != null ? ratePlanAvpPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof RatePlanAvp)) {
            return false;
        }
        RatePlanAvp other = (RatePlanAvp) object;
        if ((this.ratePlanAvpPK == null && other.ratePlanAvpPK != null) || (this.ratePlanAvpPK != null && !this.ratePlanAvpPK.equals(other.ratePlanAvpPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.bm.db.model.RatePlanAvp[ ratePlanAvpPK=" + ratePlanAvpPK + " ]";
    }
    
}
