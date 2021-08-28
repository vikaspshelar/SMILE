/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.selfcare;

/**
 *
 * @author rajeshkumar
 */
public class OnesignalResp {
    
    private String id;
    private String recipients;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRecipients() {
        return recipients;
    }

    public void setRecipients(String recipients) {
        this.recipients = recipients;
    }

    @Override
    public String toString() {
        return "OnesignalResp{" + "id=" + id + ", recipients=" + recipients + '}';
    }
}
