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
public class OrganisationSellersPK implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Column(name = "SELLER_ORGANISATION_ID")
    private int sellerOrganisationId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ORGANISATION_ID")
    private int organisationId;

    public OrganisationSellersPK() {
    }

    public OrganisationSellersPK(int sellerOrganisationId, int organisationId) {
        this.sellerOrganisationId = sellerOrganisationId;
        this.organisationId = organisationId;
    }

    public int getSellerOrganisationId() {
        return sellerOrganisationId;
    }

    public void setSellerOrganisationId(int sellerOrganisationId) {
        this.sellerOrganisationId = sellerOrganisationId;
    }

    public int getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(int organisationId) {
        this.organisationId = organisationId;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + this.sellerOrganisationId;
        hash = 37 * hash + this.organisationId;
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
        final OrganisationSellersPK other = (OrganisationSellersPK) obj;
        if (this.sellerOrganisationId != other.sellerOrganisationId) {
            return false;
        }
        if (this.organisationId != other.organisationId) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "OrganisationSellersPK{" + "sellerOrganisationId=" + sellerOrganisationId + ", organisationId=" + organisationId + '}';
    }
    
}
