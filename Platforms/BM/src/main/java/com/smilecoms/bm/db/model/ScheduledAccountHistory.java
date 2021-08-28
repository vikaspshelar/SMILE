/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.db.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;


@Entity
@Table(name = "scheduled_account_history")
@NamedQueries({
    @NamedQuery(name = "ScheduledAccountHistory.findAll", query = "SELECT r FROM ScheduledAccountHistory r")})
public class ScheduledAccountHistory implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected ScheduledAccountHistoryPK scheduledAccountHistoryPK;
   
    @Basic(optional = false)
    @NotNull
    @Column(name = "ACCOUNT_ID")
    private long accountId;
    @Column(name = "FREQUENCY")
    private String frequency;
    @Column(name = "EMAIL_TO")
    private String emailTo;
    @Column(name = "LAST_RUN_DATE")
    private Date lastRunDate;
    @Column(name = "STATUS")
    private String status;
    @Column(name = "CREATED_BY_PROFILE_ID")
    private int createdByProfileId;
    @Column(name = "CREATED_DATETIME")
    private Date createdDateTime;    

    public ScheduledAccountHistory() {
    }

    public ScheduledAccountHistory(ScheduledAccountHistoryPK scheduledAccountHistoryPK, long accountId, String frequency, String emailTo, Date lastRunDate, String status, int createdByProfileId, Date createdDateTime) {
        this.scheduledAccountHistoryPK = scheduledAccountHistoryPK;
        this.accountId = accountId;
        this.frequency = frequency;
        this.emailTo = emailTo;
        this.lastRunDate = lastRunDate;
        this.status = status;
        this.createdByProfileId = createdByProfileId;
        this.createdDateTime = createdDateTime;
    }

    public ScheduledAccountHistoryPK getScheduledAccountHistoryPK() {
        return scheduledAccountHistoryPK;
    }

    public void setScheduledAccountHistoryPK(ScheduledAccountHistoryPK scheduledAccountHistoryPK) {
        this.scheduledAccountHistoryPK = scheduledAccountHistoryPK;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getEmailTo() {
        return emailTo;
    }

    public void setEmailTo(String emailTo) {
        this.emailTo = emailTo;
    }

    public Date getLastRunDate() {
        return lastRunDate;
    }

    public void setLastRunDate(Date lastRunDate) {
        this.lastRunDate = lastRunDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCreatedByProfileId() {
        return createdByProfileId;
    }

    public void setCreatedByProfileId(int createdByProfileId) {
        this.createdByProfileId = createdByProfileId;
    }

    public Date getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(Date createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.scheduledAccountHistoryPK);
        hash = 29 * hash + (int) (this.accountId ^ (this.accountId >>> 32));
        hash = 29 * hash + Objects.hashCode(this.frequency);
        hash = 29 * hash + Objects.hashCode(this.emailTo);
        hash = 29 * hash + Objects.hashCode(this.lastRunDate);
        hash = 29 * hash + Objects.hashCode(this.status);
        hash = 29 * hash + this.createdByProfileId;
        hash = 29 * hash + Objects.hashCode(this.createdDateTime);
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
        final ScheduledAccountHistory other = (ScheduledAccountHistory) obj;
        if (this.accountId != other.accountId) {
            return false;
        }
        if (this.createdByProfileId != other.createdByProfileId) {
            return false;
        }
        if (!Objects.equals(this.frequency, other.frequency)) {
            return false;
        }
        if (!Objects.equals(this.emailTo, other.emailTo)) {
            return false;
        }
        if (!Objects.equals(this.status, other.status)) {
            return false;
        }
        if (!Objects.equals(this.scheduledAccountHistoryPK, other.scheduledAccountHistoryPK)) {
            return false;
        }
        if (!Objects.equals(this.lastRunDate, other.lastRunDate)) {
            return false;
        }
        if (!Objects.equals(this.createdDateTime, other.createdDateTime)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ScheduledAccountHistory{" + "scheduledAccountHistoryPK=" + scheduledAccountHistoryPK + ", accountId=" + accountId + ", frequency=" + frequency + ", emailTo=" + emailTo + ", lastRunDate=" + lastRunDate + ", status=" + status + ", createdByProfileId=" + createdByProfileId + ", createdDateTime=" + createdDateTime + '}';
    }

    
    
}
