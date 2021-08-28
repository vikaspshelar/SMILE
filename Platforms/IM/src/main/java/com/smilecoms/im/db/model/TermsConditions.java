/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.db.model;

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
@Table(name = "terms_conditions",  catalog = "SmileDB", schema = "")
@NamedQueries({
    @NamedQuery(name = "TermsConditions.findAll", query = "SELECT t FROM TermsConditions t")})
public class TermsConditions implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected TermsConditionsPK termsConditionsPK;
    @Basic(optional = false)
    @NotNull
    @Column(name = "STATUS")
    private String status;

    public TermsConditions() {
    }

    public TermsConditions(TermsConditionsPK termsConditionsPK) {
        this.termsConditionsPK = termsConditionsPK;
    }

    public TermsConditions(TermsConditionsPK termsConditionsPK, String status) {
        this.termsConditionsPK = termsConditionsPK;
        this.status = status;
    }

    public TermsConditions(int customerProfileId, String tCResourceKey) {
        this.termsConditionsPK = new TermsConditionsPK(customerProfileId, tCResourceKey);
    }

    public TermsConditionsPK getTermsConditionsPK() {
        return termsConditionsPK;
    }

    public void setTermsConditionsPK(TermsConditionsPK termsConditionsPK) {
        this.termsConditionsPK = termsConditionsPK;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (termsConditionsPK != null ? termsConditionsPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof TermsConditions)) {
            return false;
        }
        TermsConditions other = (TermsConditions) object;
        if ((this.termsConditionsPK == null && other.termsConditionsPK != null) || (this.termsConditionsPK != null && !this.termsConditionsPK.equals(other.termsConditionsPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.im.db.model.TermsConditions[ termsConditionsPK=" + termsConditionsPK + " ]";
    }
    
}
