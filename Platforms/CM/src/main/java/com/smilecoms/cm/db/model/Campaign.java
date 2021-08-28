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
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author lesiba
 */
@Entity
@Table(name = "campaign")
@XmlRootElement
public class Campaign implements Serializable {

    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected CampaignPK campaignPK;
    @Basic(optional = false)
    @NotNull
    @Column(name = "DESCRIPTION")
    private String description;
    @Column(name = "START_DATE_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startDateTime;
    @Column(name = "END_DATE_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endDateTime;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PARTICIPANT_QUERY")
    private String participantQuery;
    @Basic(optional = false)
    @NotNull
    @Column(name = "REMOVAL_QUERY")
    private String removalQuery;
    @Basic(optional = false)
    @NotNull
    @Column(name = "TRIGGER_QUERY")
    private String triggerQuery;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ENTICEMENT_QUERY")
    private String enticementQuery;
    @Basic(optional = false)
    @NotNull
    @Column(name = "UNIT_CREDIT_SPEC_WHITELIST_QUERY")
    private String unitCreditSpecWhitelistQuery;
    @Column(name = "ACTION_CONFIG")
    private String actionConfig;
    @Column(name = "CREATED_DATE_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDateTime;
    @Column(name = "CAMPAIGN_TYPE")
    private String campaignType;
    @Column(name = "STATUS")
    private String status;
    @Column(name = "CHANNEL")
    private String channel;
    @Column(name = "CREATED_BY_CUSTOMER_PROFILE_ID")
    private Integer createdByCustomerProfileId;

    public Campaign() {
    }

    public Campaign(CampaignPK campaignPK) {
        this.campaignPK = campaignPK;
    }

    public Campaign(CampaignPK campaignPK, String description, String participantQuery, String triggerQuery) {
        this.campaignPK = campaignPK;
        this.description = description;
        this.participantQuery = participantQuery;
        this.triggerQuery = triggerQuery;
    }

    public Campaign(int campaignId, String name) {
        this.campaignPK = new CampaignPK(campaignId, name);
    }

    public CampaignPK getCampaignPK() {
        return campaignPK;
    }

    public void setCampaignPK(CampaignPK campaignPK) {
        this.campaignPK = campaignPK;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(Date startDateTime) {
        this.startDateTime = startDateTime;
    }

    public Date getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(Date endDateTime) {
        this.endDateTime = endDateTime;
    }

    public String getParticipantQuery() {
        return participantQuery;
    }

    public void setParticipantQuery(String participantQuery) {
        this.participantQuery = participantQuery;
    }

    public String getTriggerQuery() {
        return triggerQuery;
    }

    public void setTriggerQuery(String triggerQuery) {
        this.triggerQuery = triggerQuery;
    }

    public String getActionConfig() {
        return actionConfig;
    }

    public void setActionConfig(String actionConfig) {
        this.actionConfig = actionConfig;
    }

    public Date getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(Date createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public String getCampaignType() {
        return campaignType;
    }

    public void setCampaignType(String campaignType) {
        this.campaignType = campaignType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Integer getCreatedByCustomerProfileId() {
        return createdByCustomerProfileId;
    }

    public void setCreatedByCustomerProfileId(Integer createdByCustomerProfileId) {
        this.createdByCustomerProfileId = createdByCustomerProfileId;
    }

    public String getRemovalQuery() {
        return removalQuery;
    }

    public void setRemovalQuery(String removalQuery) {
        this.removalQuery = removalQuery;
    }

    public String getEnticementQuery() {
        return enticementQuery;
    }

    public void setEnticementQuery(String enticementQuery) {
        this.enticementQuery = enticementQuery;
    }

    public String getUnitCreditSpecWhitelistQuery() {
        return unitCreditSpecWhitelistQuery;
    }

    public void setUnitCreditSpecWhitelistQuery(String unitCreditSpecWhitelistQuery) {
        this.unitCreditSpecWhitelistQuery = unitCreditSpecWhitelistQuery;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (campaignPK != null ? campaignPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Campaign)) {
            return false;
        }
        Campaign other = (Campaign) object;
        if ((this.campaignPK == null && other.campaignPK != null) || (this.campaignPK != null && !this.campaignPK.equals(other.campaignPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.cm.db.model.Campaign[ campaignPK=" + campaignPK + " ]";
    }

}
