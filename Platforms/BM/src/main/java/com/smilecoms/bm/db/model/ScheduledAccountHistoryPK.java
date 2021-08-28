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
public class ScheduledAccountHistoryPK implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Column(name = "SCHEDULE_ID")
    private long scheduleId;

    public ScheduledAccountHistoryPK() {
    }

    public ScheduledAccountHistoryPK(long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(long scheduleId) {
        this.scheduleId = scheduleId;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + (int) (this.scheduleId ^ (this.scheduleId >>> 32));
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
        final ScheduledAccountHistoryPK other = (ScheduledAccountHistoryPK) obj;
        if (this.scheduleId != other.scheduleId) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ScheduledAccountHistoryPK{" + "scheduleId=" + scheduleId + '}';
    }
    
    
    
}
