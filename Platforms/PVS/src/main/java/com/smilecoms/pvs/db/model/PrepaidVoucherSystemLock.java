/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pvs.db.model;

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
@Table(name = "prepaid_voucher_system_lock")
@NamedQueries({@NamedQuery(name = "PrepaidVoucherSystemLock.findAll", query = "SELECT p FROM PrepaidVoucherSystemLock p")})
public class PrepaidVoucherSystemLock implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "ACCOUNT_ID")
    private Long accountId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ATTEMPTS")
    private int attempts;
    @Column(name = "LOCKED_UNTIL_TIMESTAMP")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lockedUntilTimestamp;
    @Column(name = "ACCOUNT_ATTEMPTS")
    private int accountAttempts;

    public int getAccountAttempts() {
        return accountAttempts;
    }

    public void setAccountAttempts(int accountAttempts) {
        this.accountAttempts = accountAttempts;
    }
    
    public PrepaidVoucherSystemLock() {
    }

    public PrepaidVoucherSystemLock(Long accountId) {
        this.accountId = accountId;
    }

    public PrepaidVoucherSystemLock(Long accountId, int attempts) {
        this.accountId = accountId;
        this.attempts = attempts;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public Date getLockedUntilTimestamp() {
        return lockedUntilTimestamp;
    }

    public void setLockedUntilTimestamp(Date lockedUntilTimestamp) {
        this.lockedUntilTimestamp = lockedUntilTimestamp;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (accountId != null ? accountId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PrepaidVoucherSystemLock)) {
            return false;
        }
        PrepaidVoucherSystemLock other = (PrepaidVoucherSystemLock) object;
        if ((this.accountId == null && other.accountId != null) || (this.accountId != null && !this.accountId.equals(other.accountId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.pvs.db.model.PrepaidVoucherSystemLock[ accountId=" + accountId + " ]";
    }
    
}
