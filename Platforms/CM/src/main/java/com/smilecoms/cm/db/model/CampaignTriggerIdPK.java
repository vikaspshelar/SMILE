/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.cm.db.model;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author lesiba
 */
@Embeddable
public class CampaignTriggerIdPK implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Column(name = "CAMPAIGN_RUN_ID")
    private int campaignRunId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 200)
    @Column(name = "TRIGGER_KEY")
    private String triggerKey;

    public CampaignTriggerIdPK() {
    }

    public CampaignTriggerIdPK(int campaignRunId, String triggerKey) {
        this.campaignRunId = campaignRunId;
        this.triggerKey = triggerKey;
    }

    public int getCampaignRunId() {
        return campaignRunId;
    }

    public void setCampaignRunId(int campaignRunId) {
        this.campaignRunId = campaignRunId;
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
        hash += (int) campaignRunId;
        hash += (triggerKey != null ? triggerKey.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CampaignTriggerIdPK)) {
            return false;
        }
        CampaignTriggerIdPK other = (CampaignTriggerIdPK) object;
        if (this.campaignRunId != other.campaignRunId) {
            return false;
        }
        if ((this.triggerKey == null && other.triggerKey != null) || (this.triggerKey != null && !this.triggerKey.equals(other.triggerKey))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.cm.CampaignTriggerIdPK[ campaignRunId=" + campaignRunId + ", triggerKey=" + triggerKey + " ]";
    }
    
}
