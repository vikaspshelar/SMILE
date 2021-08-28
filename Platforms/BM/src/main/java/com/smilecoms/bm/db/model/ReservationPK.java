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
public class ReservationPK implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Column(name = "ACCOUNT_ID")
    private long accountId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SESSION_ID")
    private String sessionId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "UNIT_CREDIT_INSTANCE_ID")
    private int unitCreditInstanceId;

    public ReservationPK() {
    }

    public ReservationPK(long accountId, String sessionId, int unitCreditInstanceId) {
        this.accountId = accountId;
        this.sessionId = sessionId;
        this.unitCreditInstanceId = unitCreditInstanceId;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public int getUnitCreditInstanceId() {
        return unitCreditInstanceId;
    }

    public void setUnitCreditInstanceId(int unitCreditInstanceId) {
        this.unitCreditInstanceId = unitCreditInstanceId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (int) accountId;
        hash += (sessionId != null ? sessionId.hashCode() : 0);
        hash += (int) unitCreditInstanceId;
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ReservationPK)) {
            return false;
        }
        ReservationPK other = (ReservationPK) object;
        if (this.accountId != other.accountId) {
            return false;
        }
        if ((this.sessionId == null && other.sessionId != null) || (this.sessionId != null && !this.sessionId.equals(other.sessionId))) {
            return false;
        }
        if (this.unitCreditInstanceId != other.unitCreditInstanceId) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.bm.db.model.ReservationPK[ accountId=" + accountId + ", sessionId=" + sessionId + ", unitCreditInstanceId=" + unitCreditInstanceId + " ]";
    }
    
}
