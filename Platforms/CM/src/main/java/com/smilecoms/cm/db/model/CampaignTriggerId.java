/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.cm.db.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;

/**
 *
 * @author lesiba
 */
@Entity
@Table(name = "campaign_trigger_id")
@NamedQueries({
    @NamedQuery(name = "CampaignTriggerId.findAll", query = "SELECT c FROM CampaignTriggerId c"),
    @NamedQuery(name = "CampaignTriggerId.findByCampaignRunId", query = "SELECT c FROM CampaignTriggerId c WHERE c.campaignTriggerIdPK.campaignRunId = :campaignRunId"),
    @NamedQuery(name = "CampaignTriggerId.findByTriggerKey", query = "SELECT c FROM CampaignTriggerId c WHERE c.campaignTriggerIdPK.triggerKey = :triggerKey"),
    @NamedQuery(name = "CampaignTriggerId.findByTriggerDateTime", query = "SELECT c FROM CampaignTriggerId c WHERE c.triggerDateTime = :triggerDateTime")})
public class CampaignTriggerId implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected CampaignTriggerIdPK campaignTriggerIdPK;
    @Basic(optional = false)
    @NotNull
    @Column(name = "TRIGGER_DATE_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date triggerDateTime;
    @Column(name = "ENTICEMENT_KEY")
    private String enticementKey;

    public CampaignTriggerId() {
    }

    public CampaignTriggerId(CampaignTriggerIdPK campaignTriggerIdPK) {
        this.campaignTriggerIdPK = campaignTriggerIdPK;
    }

    public CampaignTriggerId(CampaignTriggerIdPK campaignTriggerIdPK, Date triggerDateTime) {
        this.campaignTriggerIdPK = campaignTriggerIdPK;
        this.triggerDateTime = triggerDateTime;
    }

    public CampaignTriggerId(int campaignRunId, String triggerKey) {
        this.campaignTriggerIdPK = new CampaignTriggerIdPK(campaignRunId, triggerKey);
    }

    public CampaignTriggerIdPK getCampaignTriggerIdPK() {
        return campaignTriggerIdPK;
    }

    public void setCampaignTriggerIdPK(CampaignTriggerIdPK campaignTriggerIdPK) {
        this.campaignTriggerIdPK = campaignTriggerIdPK;
    }

    public Date getTriggerDateTime() {
        return triggerDateTime;
    }

    public void setTriggerDateTime(Date triggerDateTime) {
        this.triggerDateTime = triggerDateTime;
    }

    public String getEnticementKey() {
        return enticementKey;
    }

    public void setEnticementKey(String enticementKey) {
        this.enticementKey = enticementKey;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (campaignTriggerIdPK != null ? campaignTriggerIdPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CampaignTriggerId)) {
            return false;
        }
        CampaignTriggerId other = (CampaignTriggerId) object;
        if ((this.campaignTriggerIdPK == null && other.campaignTriggerIdPK != null) || (this.campaignTriggerIdPK != null && !this.campaignTriggerIdPK.equals(other.campaignTriggerIdPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.cm.CampaignTriggerId[ campaignTriggerIdPK=" + campaignTriggerIdPK + " ]";
    }
    
}
