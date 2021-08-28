/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.db.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "daily_fup_info")
@NamedQueries({
    @NamedQuery(name = "DailyFupInfo.findAll", query = "SELECT df FROM DailyFupInfo df")})
public class DailyFupInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected DailyFupInfoPK dailyFupInfoPK;    
    @Basic(optional = false)
    @NotNull
    @Column(name = "START_OF_DAY_UNITS")
    private long startOfDayUnits;
    @Basic(optional = false)
    @NotNull
    @Column(name = "LAST_MODIFIED")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastModified;

    public DailyFupInfo() {
    }

    public DailyFupInfo(DailyFupInfoPK dailyFupInfoPK) {
        this.dailyFupInfoPK = dailyFupInfoPK;
    }

    public DailyFupInfo(DailyFupInfoPK dailyFupInfoPK, long startOfDayUnits, Date lastModified) {
        this.dailyFupInfoPK = dailyFupInfoPK;
        this.startOfDayUnits = startOfDayUnits;
        this.lastModified = lastModified;
    }

    public DailyFupInfo(long accountId, int unitCreditSpecificationId) {
        this.dailyFupInfoPK = new DailyFupInfoPK(accountId, unitCreditSpecificationId);
    }

    public DailyFupInfoPK getDailyFupInfoPK() {
        return dailyFupInfoPK;
    }

    public void setDailyFupInfoPK(DailyFupInfoPK dailyFupInfoPK) {
        this.dailyFupInfoPK = dailyFupInfoPK;
    }

    public long getStartOfDayUnits() {
        return startOfDayUnits;
    }

    public void setStartOfDayUnits(long startOfDayUnits) {
        this.startOfDayUnits = startOfDayUnits;
    }
    
    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }


    @Override
    public int hashCode() {
        int hash = 0;
        hash += (dailyFupInfoPK != null ? dailyFupInfoPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DailyFupInfo)) {
            return false;
        }
        DailyFupInfo other = (DailyFupInfo) object;
        if ((this.dailyFupInfoPK == null && other.dailyFupInfoPK != null) || (this.dailyFupInfoPK != null && !this.dailyFupInfoPK.equals(other.dailyFupInfoPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.bm.db.model.Reservation[ reservationPK=" + dailyFupInfoPK + " ]";
    }
    
}
