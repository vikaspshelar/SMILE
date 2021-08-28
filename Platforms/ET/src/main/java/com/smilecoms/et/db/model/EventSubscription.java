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
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "event_subscription")
@NamedQueries({
    @NamedQuery(name = "EventSubscription.findAll", query = "SELECT e FROM EventSubscription e")})
public class EventSubscription implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "EVENT_SUBSCRIPTION_ID")
    private Integer eventSubscriptionId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "TYPE")
    private String type;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SUB_TYPE")
    private String subType;
    @Basic(optional = false)
    @NotNull
    @Column(name = "EVENT_KEY")
    private String eventKey;
    @Basic(optional = false)
    @NotNull
    @Column(name = "DATA_MATCH")
    private String dataMatch;
    @Basic(optional = false)
    @NotNull
    @Column(name = "EXPIRY_TIMESTAMP")
    @Temporal(TemporalType.TIMESTAMP)
    private Date expiryTimestamp;
    @Basic(optional = false)
    @NotNull
    @Column(name = "EVENT_TEMPLATE_ID")
    private int eventTemplateId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "REPEATABLE")
    private String repeatable;

    public EventSubscription() {
    }

    public EventSubscription(Integer eventSubscriptionId) {
        this.eventSubscriptionId = eventSubscriptionId;
    }

    public EventSubscription(Integer eventSubscriptionId, String type, String subType, String eventKey, String dataMatch, Date expiryTimestamp, int eventTemplateId, String repeatable) {
        this.eventSubscriptionId = eventSubscriptionId;
        this.type = type;
        this.subType = subType;
        this.eventKey = eventKey;
        this.dataMatch = dataMatch;
        this.expiryTimestamp = expiryTimestamp;
        this.eventTemplateId = eventTemplateId;
        this.repeatable = repeatable;
    }

    public Integer getEventSubscriptionId() {
        return eventSubscriptionId;
    }

    public void setEventSubscriptionId(Integer eventSubscriptionId) {
        this.eventSubscriptionId = eventSubscriptionId;
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

    public String getDataMatch() {
        return dataMatch;
    }

    public void setDataMatch(String dataMatch) {
        this.dataMatch = dataMatch;
    }

    public Date getExpiryTimestamp() {
        return expiryTimestamp;
    }

    public void setExpiryTimestamp(Date expiryTimestamp) {
        this.expiryTimestamp = expiryTimestamp;
    }

    public int getEventTemplateId() {
        return eventTemplateId;
    }

    public void setEventTemplateId(int eventTemplateId) {
        this.eventTemplateId = eventTemplateId;
    }

    public String getRepeatable() {
        return repeatable;
    }

    public void setRepeatable(String repeatable) {
        this.repeatable = repeatable;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (eventSubscriptionId != null ? eventSubscriptionId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof EventSubscription)) {
            return false;
        }
        EventSubscription other = (EventSubscription) object;
        if ((this.eventSubscriptionId == null && other.eventSubscriptionId != null) || (this.eventSubscriptionId != null && !this.eventSubscriptionId.equals(other.eventSubscriptionId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.et.db.model.EventSubscription[ eventSubscriptionId=" + eventSubscriptionId + " ]";
    }
    
}
