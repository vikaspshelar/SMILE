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
public class CustomerRolePK implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Column(name = "CUSTOMER_PROFILE_ID")
    private int customerProfileId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ORGANISATION_ID")
    private int organisationId;

    public CustomerRolePK() {
    }

    public CustomerRolePK(int customerProfileId, int organisationId) {
        this.customerProfileId = customerProfileId;
        this.organisationId = organisationId;
    }

    public int getCustomerProfileId() {
        return customerProfileId;
    }

    public void setCustomerProfileId(int customerProfileId) {
        this.customerProfileId = customerProfileId;
    }

    public int getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(int organisationId) {
        this.organisationId = organisationId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (int) customerProfileId;
        hash += (int) organisationId;
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CustomerRolePK)) {
            return false;
        }
        CustomerRolePK other = (CustomerRolePK) object;
        if (this.customerProfileId != other.customerProfileId) {
            return false;
        }
        if (this.organisationId != other.organisationId) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.im.db.model.CustomerRolePK[ customerProfileId=" + customerProfileId + ", organisationId=" + organisationId + " ]";
    }
    
}
