/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.et.db.model;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "event_subscription_field")
@NamedQueries({
    @NamedQuery(name = "EventSubscriptionField.findAll", query = "SELECT e FROM EventSubscriptionField e")})
public class EventSubscriptionField implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "EVENT_SUBSCRIPTION_FIELD_ID")
    private Integer eventSubscriptionFieldId;
    @JoinColumn(name = "EVENT_SUBSCRIPTION_ID", referencedColumnName = "EVENT_SUBSCRIPTION_ID")
    @ManyToOne(optional = false)
    private EventSubscription eventSubscription;
    @Basic(optional = false)
    @NotNull
    @Column(name = "FIELD_NAME")
    private String fieldName;
    @Column(name = "REPLACEMENT_TYPE")
    private String replacementType;
    @Column(name = "REPLACEMENT_DATA")
    private String replacementData;

    public EventSubscriptionField() {
    }

    public EventSubscriptionField(Integer eventSubscriptionFieldId) {
        this.eventSubscriptionFieldId = eventSubscriptionFieldId;
    }

    public EventSubscriptionField(Integer eventSubscriptionFieldId, EventSubscription eventSubscription, String fieldName) {
        this.eventSubscriptionFieldId = eventSubscriptionFieldId;
        this.eventSubscription = eventSubscription;
        this.fieldName = fieldName;
    }

    public Integer getEventSubscriptionFieldId() {
        return eventSubscriptionFieldId;
    }

    public void setEventSubscriptionFieldId(Integer eventSubscriptionFieldId) {
        this.eventSubscriptionFieldId = eventSubscriptionFieldId;
    }

    public EventSubscription getEventSubscription() {
        return eventSubscription;
    }

    public void setEventSubscription(EventSubscription eventSubscription) {
        this.eventSubscription = eventSubscription;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getReplacementType() {
        return replacementType;
    }

    public void setReplacementType(String replacementType) {
        this.replacementType = replacementType;
    }

    public String getReplacementData() {
        return replacementData;
    }

    public void setReplacementData(String replacementData) {
        this.replacementData = replacementData;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (eventSubscriptionFieldId != null ? eventSubscriptionFieldId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof EventSubscriptionField)) {
            return false;
        }
        EventSubscriptionField other = (EventSubscriptionField) object;
        if ((this.eventSubscriptionFieldId == null && other.eventSubscriptionFieldId != null) || (this.eventSubscriptionFieldId != null && !this.eventSubscriptionFieldId.equals(other.eventSubscriptionFieldId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.et.db.model.EventSubscriptionField[ eventSubscriptionFieldId=" + eventSubscriptionFieldId + " ]";
    }
    
}
