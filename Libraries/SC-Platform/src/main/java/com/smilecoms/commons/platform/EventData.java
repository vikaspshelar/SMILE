/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.platform;

import java.util.Date;

/**
 *
 * @author paul
 */
public class EventData {
    
    
    private String type;
    private String subType;
    private String eventKey;
    private String data;
    private String uniqueKey;
    private Date date;

    public EventData(String type, String subType, String eventKey, String data, String uniqueKey, Date date) {
        this.type = type;
        this.subType = subType;
        this.eventKey = eventKey;
        this.data = data;
        this.uniqueKey = uniqueKey;
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    public String getEventKey() {
        return eventKey;
    }

    public void setEventKey(String eventKey) {
        this.eventKey = eventKey;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getUniqueKey() {
        return uniqueKey;
    }

    public void setUniqueKey(String uniqueKey) {
        this.uniqueKey = uniqueKey;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
    
    
}
