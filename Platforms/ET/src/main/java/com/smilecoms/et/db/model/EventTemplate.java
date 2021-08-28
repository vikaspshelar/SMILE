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
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "event_template")
@NamedQueries({
    @NamedQuery(name = "EventTemplate.findAll", query = "SELECT e FROM EventTemplate e")})
public class EventTemplate implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @NotNull
    @Column(name = "EVENT_TEMPLATE_ID")
    private Integer eventTemplateId;
    @Column(name = "DESTINATION")
    private String destination;
    @Lob
    @Column(name = "DATA")
    private String data;
    @Column(name = "PROTOCOL")
    private String protocol;

    public EventTemplate() {
    }

    public EventTemplate(Integer eventTemplateId) {
        this.eventTemplateId = eventTemplateId;
    }

    public Integer getEventTemplateId() {
        return eventTemplateId;
    }

    public void setEventTemplateId(Integer eventTemplateId) {
        this.eventTemplateId = eventTemplateId;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (eventTemplateId != null ? eventTemplateId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof EventTemplate)) {
            return false;
        }
        EventTemplate other = (EventTemplate) object;
        if ((this.eventTemplateId == null && other.eventTemplateId != null) || (this.eventTemplateId != null && !this.eventTemplateId.equals(other.eventTemplateId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.et.db.model.EventTemplate[ eventTemplateId=" + eventTemplateId + " ]";
    }
    
}
