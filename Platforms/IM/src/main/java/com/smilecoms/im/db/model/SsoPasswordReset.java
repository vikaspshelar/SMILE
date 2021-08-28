/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.db.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "sso_password_reset", catalog = "SmileDB", schema = "")
@NamedQueries({
    @NamedQuery(name = "SsoPasswordReset.findAll", query = "SELECT s FROM SsoPasswordReset s")})
public class SsoPasswordReset implements Serializable {
    private static final long serialVersionUID = 1L;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CUSTOMER_PROFILE_ID")
    private int customerProfileId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "EXPIRY_DATETIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date expiryDatetime;
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "GUID")
    private String guid;

    public SsoPasswordReset() {
    }

    public SsoPasswordReset(String guid) {
        this.guid = guid;
    }

    public SsoPasswordReset(String guid, int customerProfileId, Date expiryDatetime) {
        this.guid = guid;
        this.customerProfileId = customerProfileId;
        this.expiryDatetime = expiryDatetime;
    }

    public int getCustomerProfileId() {
        return customerProfileId;
    }

    public void setCustomerProfileId(int customerProfileId) {
        this.customerProfileId = customerProfileId;
    }

    public Date getExpiryDatetime() {
        return expiryDatetime;
    }

    public void setExpiryDatetime(Date expiryDatetime) {
        this.expiryDatetime = expiryDatetime;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (guid != null ? guid.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof SsoPasswordReset)) {
            return false;
        }
        SsoPasswordReset other = (SsoPasswordReset) object;
        if ((this.guid == null && other.guid != null) || (this.guid != null && !this.guid.equals(other.guid))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.im.SsoPasswordReset[ guid=" + guid + " ]";
    }
    
}
