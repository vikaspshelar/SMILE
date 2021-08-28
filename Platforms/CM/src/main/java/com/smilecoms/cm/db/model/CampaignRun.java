/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.cm.db.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;

/**
 *
 * @author lesiba
 */
@Entity
@Table(name = "campaign_run")
public class CampaignRun implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "CAMPAIGN_RUN_ID")
    private Integer campaignRunId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CAMPAIGN_ID")
    private int campaignId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PRODUCT_INSTANCE_ID")
    private int productInstanceId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "STATUS")
    private String status;
    @Basic(optional = false)
    @NotNull
    @Column(name = "LAST_CHECK_DATE_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastCheckDateTime;
    @Column(name = "ACTION_DATA")
    private String actionData;
    @Column(name = "CREATED_DATE_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDateTime;
    @Column(name = "RUN_TYPE")
    private String runType;

    public CampaignRun() {
    }

    public CampaignRun(Integer campaignRunId) {
        this.campaignRunId = campaignRunId;
    }

    public CampaignRun(Integer campaignRunId, int campaignId, int productInstanceId, String status, Date lastCheckDateTime) {
        this.campaignRunId = campaignRunId;
        this.campaignId = campaignId;
        this.productInstanceId = productInstanceId;
        this.status = status;
        this.lastCheckDateTime = lastCheckDateTime;
    }

    public Integer getCampaignRunId() {
        return campaignRunId;
    }

    public void setCampaignRunId(Integer campaignRunId) {
        this.campaignRunId = campaignRunId;
    }

    public int getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(int campaignId) {
        this.campaignId = campaignId;
    }

    public int getProductInstanceId() {
        return productInstanceId;
    }

    public void setProductInstanceId(int productInstanceId) {
        this.productInstanceId = productInstanceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getLastCheckDateTime() {
        return lastCheckDateTime;
    }

    public void setLastCheckDateTime(Date lastCheckDateTime) {
        this.lastCheckDateTime = lastCheckDateTime;
    }

    public String getActionData() {
        return actionData;
    }

    public void setActionData(String actionData) {
        this.actionData = actionData;
    }

    public Date getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(Date createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public String getRunType() {
        return runType;
    }

    public void setRunType(String runType) {
        this.runType = runType;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (campaignRunId != null ? campaignRunId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CampaignRun)) {
            return false;
        }
        CampaignRun other = (CampaignRun) object;
        if ((this.campaignRunId == null && other.campaignRunId != null) || (this.campaignRunId != null && !this.campaignRunId.equals(other.campaignRunId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.cm.db.model.CampaignRun[ campaignRunId=" + campaignRunId + " ]";
    }
    
}
