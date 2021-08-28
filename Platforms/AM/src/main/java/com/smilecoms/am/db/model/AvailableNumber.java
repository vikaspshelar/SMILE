/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.smilecoms.am.db.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "available_number")
public class AvailableNumber implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "IMPU", nullable = false)
    private String impu;
    @Column(name = "ISSUED", nullable = false)
    private int issued;
    @Column(name = "ISSUED_DATETIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date issuedDateTime;
    @Column(name = "PRICE_CENTS", nullable = false)
    private int priceCents;
    @Basic(optional = false)
    @Column(name = "OWNED_BY_CUSTOMER_PROFILE_ID")
    private int ownedByCustomerProfileId;
    @Basic(optional = false)
    @Column(name = "OWNED_BY_ORGANISATION_ID")
    private int ownedByOrganisationId;
    @Column(name = "RELEASED_DATETIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date releasedDateTime;
    @Column(name = "ICCID")
    private String iccid;
    
    public AvailableNumber() {
    }

    public AvailableNumber(String impu) {
        this.impu = impu;
    }

    public AvailableNumber(String impu, int issued) {
        this.impu = impu;
        this.issued = issued;
    }

    public String getIMPU() {
        return impu;
    }

    public void setIMPU(String impu) {
        this.impu = impu;
    }


    public int getIssued() {
        return issued;
    }

    public void setIssued(int issued) {
        this.issued = issued;
    }

    public Date getIssuedDateTime() {
        return issuedDateTime;
    }

    public void setIssuedDateTime(Date issuedDateTime) {
        this.issuedDateTime = issuedDateTime;
    }

    public int getPriceCents() {
        return priceCents;
    }

    public void setPriceCents(int priceCents) {
        this.priceCents = priceCents;
    }

    public int getOwnedByCustomerProfileId() {
        return ownedByCustomerProfileId;
    }

    public void setOwnedByCustomerProfileId(int ownedByCustomerProfileId) {
        this.ownedByCustomerProfileId = ownedByCustomerProfileId;
    }

    public int getOwnedByOrganisationId() {
        return ownedByOrganisationId;
    }

    public void setOwnedByOrganisationId(int ownedByOrganisationId) {
        this.ownedByOrganisationId = ownedByOrganisationId;
    }

    public Date getReleasedDateTime() {
        return releasedDateTime;
    }

    public void setReleasedDateTime(Date releasedDateTime) {
        this.releasedDateTime = releasedDateTime;
    }
     
    public String getICCID() {
        return iccid;
    }

    public void setICCID(String iccid) {
        this.iccid = iccid;
    }

    

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (impu != null ? impu.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof AvailableNumber)) {
            return false;
        }
        AvailableNumber other = (AvailableNumber) object;
        if ((this.impu == null && other.impu != null) || (this.impu != null && !this.impu.equals(other.impu))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.nm.AvailableNumber[impu=" + impu + "]";
    }

}
