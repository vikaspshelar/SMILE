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
public class AppNotificationReq {
    
    private int customerId;
    private String appId;
    private String appVersion;
    private String os;

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    @Override
    public String toString() {
        return "AppNotificationReq{" + "customerId=" + customerId + ", appId=" + appId + ", appVersion=" + appVersion + ", os=" + os + '}';
    }
}
