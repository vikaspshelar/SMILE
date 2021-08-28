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
public class CampaignEnticementIdPK implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Column(name = "CAMPAIGN_RUN_ID")
    private int campaignRunId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 200)
    @Column(name = "ENTICEMENT_KEY")
    private String enticementKey;

    public CampaignEnticementIdPK() {
    }

    public CampaignEnticementIdPK(int campaignRunId, String enticementKey) {
        this.campaignRunId = campaignRunId;
        this.enticementKey = enticementKey;
    }

    public int getCampaignRunId() {
        return campaignRunId;
    }

    public void setCampaignRunId(int campaignRunId) {
        this.campaignRunId = campaignRunId;
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
        hash += (int) campaignRunId;
        hash += (enticementKey != null ? enticementKey.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CampaignEnticementIdPK)) {
            return false;
        }
        CampaignEnticementIdPK other = (CampaignEnticementIdPK) object;
        if (this.campaignRunId != other.campaignRunId) {
            return false;
        }
        if ((this.enticementKey == null && other.enticementKey != null) || (this.enticementKey != null && !this.enticementKey.equals(other.enticementKey))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.cm.CampaignEnticementIdPK[ campaignRunId=" + campaignRunId + ", enticementKey=" + enticementKey + " ]";
    }
    
}
