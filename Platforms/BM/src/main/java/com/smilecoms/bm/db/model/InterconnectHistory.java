/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.db.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "interconnect_history")
@NamedQueries({
    @NamedQuery(name = "InterconnectHistory.findAll", query = "SELECT i FROM InterconnectHistory i")})
public class InterconnectHistory implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "INTERCONNECT_HISTORY_ID")
    private Integer interconnectHistoryId;
    @Column(name = "IS_RUNNING")
    private Character isRunning;
    @Column(name = "RUN_START_DATETIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date runStartDatetime;
    @Column(name = "RUN_END_DATETIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date runEndDatetime;
    @Column(name = "EVENT_START_DATETIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date eventStartDatetime;
    @Column(name = "EVENT_END_DATETIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date eventEndDatetime;
    @Column(name = "EVENTS_PROCESSED")
    private Integer eventsProcessed;
    @Column(name = "ERROR_DETAIL")
    private String errorDetail;

    public InterconnectHistory() {
    }

    public InterconnectHistory(Integer interconnectHistoryId) {
        this.interconnectHistoryId = interconnectHistoryId;
    }

    public Integer getInterconnectHistoryId() {
        return interconnectHistoryId;
    }

    public void setInterconnectHistoryId(Integer interconnectHistoryId) {
        this.interconnectHistoryId = interconnectHistoryId;
    }

    public Character getIsRunning() {
        return isRunning;
    }

    public void setIsRunning(Character isRunning) {
        this.isRunning = isRunning;
    }

    public Date getRunStartDatetime() {
        return runStartDatetime;
    }

    public void setRunStartDatetime(Date runStartDatetime) {
        this.runStartDatetime = runStartDatetime;
    }

    public Date getRunEndDatetime() {
        return runEndDatetime;
    }

    public void setRunEndDatetime(Date runEndDatetime) {
        this.runEndDatetime = runEndDatetime;
    }

    public Date getEventStartDatetime() {
        return eventStartDatetime;
    }

    public void setEventStartDatetime(Date eventStartDatetime) {
        this.eventStartDatetime = eventStartDatetime;
    }

    public Date getEventEndDatetime() {
        return eventEndDatetime;
    }

    public void setEventEndDatetime(Date eventEndDatetime) {
        this.eventEndDatetime = eventEndDatetime;
    }

    public Integer getEventsProcessed() {
        return eventsProcessed;
    }

    public void setEventsProcessed(Integer eventsProcessed) {
        this.eventsProcessed = eventsProcessed;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public void setErrorDetail(String errorDetail) {
        this.errorDetail = errorDetail;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (interconnectHistoryId != null ? interconnectHistoryId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof InterconnectHistory)) {
            return false;
        }
        InterconnectHistory other = (InterconnectHistory) object;
        if ((this.interconnectHistoryId == null && other.interconnectHistoryId != null) || (this.interconnectHistoryId != null && !this.interconnectHistoryId.equals(other.interconnectHistoryId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.bm.db.model.InterconnectHistory[ interconnectHistoryId=" + interconnectHistoryId + " ]";
    }
}
