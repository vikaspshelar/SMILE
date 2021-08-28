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
import javax.validation.constraints.Size;

/**
 *
 * @author lesiba
 */
@Entity
@Table(name = "campaign_enticement_id")
public class CampaignEnticementId implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected CampaignEnticementIdPK campaignEnticementIdPK;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ENTICEMENT_DATE_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date enticementDateTime;
    @Column(name = "TRIGGER_KEY")
    private String triggerKey;

    public CampaignEnticementId() {
    }

    public CampaignEnticementId(CampaignEnticementIdPK campaignEnticementIdPK) {
        this.campaignEnticementIdPK = campaignEnticementIdPK;
    }

    public CampaignEnticementId(CampaignEnticementIdPK campaignEnticementIdPK, Date enticementDateTime) {
        this.campaignEnticementIdPK = campaignEnticementIdPK;
        this.enticementDateTime = enticementDateTime;
    }

    public CampaignEnticementId(int campaignRunId, String enticementKey) {
        this.campaignEnticementIdPK = new CampaignEnticementIdPK(campaignRunId, enticementKey);
    }

    public CampaignEnticementIdPK getCampaignEnticementIdPK() {
        return campaignEnticementIdPK;
    }

    public void setCampaignEnticementIdPK(CampaignEnticementIdPK campaignEnticementIdPK) {
        this.campaignEnticementIdPK = campaignEnticementIdPK;
    }

    public Date getEnticementDateTime() {
        return enticementDateTime;
    }

    public void setEnticementDateTime(Date enticementDateTime) {
        this.enticementDateTime = enticementDateTime;
    }

    public String getTriggerKey() {
        return triggerKey;
    }

    public void setTriggerKey(String triggerKey) {
        this.triggerKey = triggerKey;
    }

    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (campaignEnticementIdPK != null ? campaignEnticementIdPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CampaignEnticementId)) {
            return false;
        }
        CampaignEnticementId other = (CampaignEnticementId) object;
        if ((this.campaignEnticementIdPK == null && other.campaignEnticementIdPK != null) || (this.campaignEnticementIdPK != null && !this.campaignEnticementIdPK.equals(other.campaignEnticementIdPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.cm.CampaignEnticementId[ campaignEnticementIdPK=" + campaignEnticementIdPK + " ]";
    }
    
}
