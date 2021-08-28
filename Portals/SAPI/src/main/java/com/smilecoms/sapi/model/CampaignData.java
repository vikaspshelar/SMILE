/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sapi.model;

import java.util.GregorianCalendar;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 *
 * @author bhaskarhg
 */
public class CampaignData {
    
    
    protected int campaignId;
    
    protected String name;
    
    protected GregorianCalendar startDateTime;
    
    protected GregorianCalendar endDateTime;
    
    protected String status;
    
    protected String productInstanceIds;
    
    protected GregorianCalendar lastCheckDateTime;
    
    protected List<Integer> campaignUnitCredits;

    public int getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(int campaignId) {
        this.campaignId = campaignId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GregorianCalendar getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(GregorianCalendar startDateTime) {
        this.startDateTime = startDateTime;
    }

    public GregorianCalendar getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(GregorianCalendar endDateTime) {
        this.endDateTime = endDateTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProductInstanceIds() {
        return productInstanceIds;
    }

    public void setProductInstanceIds(String productInstanceIds) {
        this.productInstanceIds = productInstanceIds;
    }

    public GregorianCalendar getLastCheckDateTime() {
        return lastCheckDateTime;
    }

    public void setLastCheckDateTime(GregorianCalendar lastCheckDateTime) {
        this.lastCheckDateTime = lastCheckDateTime;
    }

    public List<Integer> getCampaignUnitCredits() {
        return campaignUnitCredits;
    }

    public void setCampaignUnitCredits(List<Integer> campaignUnitCredits) {
        this.campaignUnitCredits = campaignUnitCredits;
    }
    
}
