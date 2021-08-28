/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.et.db.model;

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
@Table(name = "event_data")
@NamedQueries({
    @NamedQuery(name = "EventData.findAll", query = "SELECT e FROM EventData e")})
public class EventData implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "EVENT_DATA_ID")
    private Long eventDataId;
    @Column(name = "TYPE")
    private String type;
    @Column(name = "SUB_TYPE")
    private String subType;
    @Column(name = "EVENT_KEY")
    private String eventKey;
    @Column(name = "UNIQUE_KEY")
    private String uniqueKey;
    @Column(name = "EVENT_TIMESTAMP")
    @Temporal(TemporalType.TIMESTAMP)
    private Date eventTimestamp;
    @Column(name = "DATA")
    private String data;
    @Column(name = "STATUS")
    private String status;
    @Column(name = "PROCESSED_TIMESTAMP")
    @Temporal(TemporalType.TIMESTAMP)
    private Date processedTimestamp;

    public EventData() {
    }

    public EventData(Long eventDataId) {
        this.eventDataId = eventDataId;
    }

    public Long getEventDataId() {
        return eventDataId;
    }

    public void setEventDataId(Long eventDataId) {
        this.eventDataId = eventDataId;
    }

    public String getUniqueKey() {
        return uniqueKey;
    }

    public void setUniqueKey(String uniqueKey) {
        this.uniqueKey = uniqueKey;
    }

    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    public String getEventKey() {
        return eventKey;
    }

    public void setEventKey(String eventKey) {
        this.eventKey = eventKey;
    }

    public Date getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(Date eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getProcessedTimestamp() {
        return processedTimestamp;
    }

    public void setProcessedTimestamp(Date processedTimestamp) {
        this.processedTimestamp = processedTimestamp;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (eventDataId != null ? eventDataId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof EventData)) {
            return false;
        }
        EventData other = (EventData) object;
        if ((this.eventDataId == null && other.eventDataId != null) || (this.eventDataId != null && !this.eventDataId.equals(other.eventDataId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.et.db.model.EventData[ eventDataId=" + eventDataId + " ]";
    }
    
}
