/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.db.model;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;


@Entity
@Table(name = "organisation_sellers", catalog = "SmileDB", schema = "")
@NamedQueries({
    @NamedQuery(name = "OrganisationSellers.findAll", query = "SELECT s FROM OrganisationSellers s")})
public class OrganisationSellers implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected OrganisationSellersPK organisationSellersPK;

    public OrganisationSellers() {
    }

    public OrganisationSellers(OrganisationSellersPK organisationSellersPK) {
        this.organisationSellersPK = organisationSellersPK;
    }

    public OrganisationSellersPK getOrganisationSellersPK() {
        return organisationSellersPK;
    }

    public void setOrganisationSellersPK(OrganisationSellersPK organisationSellersPK) {
        this.organisationSellersPK = organisationSellersPK;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 41 * hash + Objects.hashCode(this.organisationSellersPK);
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
        final OrganisationSellers other = (OrganisationSellers) obj;
        if (!Objects.equals(this.organisationSellersPK, other.organisationSellersPK)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "OrganisationSellers{" + "organisationSellersPK=" + organisationSellersPK + '}';
    }
    
    
    
}
