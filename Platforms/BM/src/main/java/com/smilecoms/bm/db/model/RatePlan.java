/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.db.model;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "rate_plan")
@NamedQueries({
    @NamedQuery(name = "RatePlan.findAll", query = "SELECT r FROM RatePlan r")})
public class RatePlan implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @NotNull
    @Column(name = "RATE_PLAN_ID")
    private Integer ratePlanId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "RATE_PLAN_NAME")
    private String ratePlanName;
    @Basic(optional = false)
    @NotNull
    @Column(name = "RATING_ENGINE_CLASS")
    private String ratingEngineClass;
    @Basic(optional = false)
    @NotNull
    @Column(name = "EVENT_BASED")
    private String eventBased;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SESSION_BASED")
    private String sessionBased;
    @Basic(optional = false)
    @NotNull
    @Column(name = "RATE_PLAN_DESCRIPTION")
    private String ratePlanDescription;

    public String getRatePlanDescription() {
        return ratePlanDescription;
    }

    public void setRatePlanDescription(String ratePlanDescription) {
        this.ratePlanDescription = ratePlanDescription;
    }

    
    public String getEventBased() {
        return eventBased;
    }

    public void setEventBased(String eventBased) {
        this.eventBased = eventBased;
    }

    public String getRatingEngineClass() {
        return ratingEngineClass;
    }

    public void setRatingEngineClass(String ratingEngineClass) {
        this.ratingEngineClass = ratingEngineClass;
    }

    public String getSessionBased() {
        return sessionBased;
    }

    public void setSessionBased(String sessionBased) {
        this.sessionBased = sessionBased;
    }

    
    public RatePlan() {
    }

    public RatePlan(Integer ratePlanId) {
        this.ratePlanId = ratePlanId;
    }

    public RatePlan(Integer ratePlanId, String ratePlanName) {
        this.ratePlanId = ratePlanId;
        this.ratePlanName = ratePlanName;
    }

    public Integer getRatePlanId() {
        return ratePlanId;
    }

    public void setRatePlanId(Integer ratePlanId) {
        this.ratePlanId = ratePlanId;
    }

    public String getRatePlanName() {
        return ratePlanName;
    }

    public void setRatePlanName(String ratePlanName) {
        this.ratePlanName = ratePlanName;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (ratePlanId != null ? ratePlanId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof RatePlan)) {
            return false;
        }
        RatePlan other = (RatePlan) object;
        if ((this.ratePlanId == null && other.ratePlanId != null) || (this.ratePlanId != null && !this.ratePlanId.equals(other.ratePlanId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.bm.db.model.RatePlan[ ratePlanId=" + ratePlanId + " ]";
    }
    
}
