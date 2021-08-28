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

@Embeddable
public class DailyFupInfoPK implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Column(name = "ACCOUNT_ID")
    private long accountId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "UNIT_CREDIT_SPECIFICATION_ID")
    private int unitCreditSpecificationId;
    

    public DailyFupInfoPK() {
    }

    public DailyFupInfoPK(long accountId, int unitCreditSpecificationId) {
        this.accountId = accountId;        
        this.unitCreditSpecificationId = unitCreditSpecificationId;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public int getUnitCreditSpecificationId() {
        return unitCreditSpecificationId;
    }

    public void setUnitCreditSpecificationId(int unitCreditSpecificationId) {
        this.unitCreditSpecificationId = unitCreditSpecificationId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (int) accountId;        
        hash += (int) unitCreditSpecificationId;
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DailyFupInfoPK)) {
            return false;
        }
        DailyFupInfoPK other = (DailyFupInfoPK) object;
        if (this.accountId != other.accountId) {
            return false;
        }
        if (this.unitCreditSpecificationId != other.unitCreditSpecificationId) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.bm.db.model.DailyFupInfoPK[ accountId=" + accountId + ",  unitCreditInstanceId=" + unitCreditSpecificationId + " ]";
    }
    
}
