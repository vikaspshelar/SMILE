/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.model;

/**
 *
 * @author rajeshkumar
 */
public class AccountProduct {
    private String deviceName;
    private String description;
    private String serialNumber;
    private String smileNumber;
    private String purchageDate;
    private String activationDate;
    private String lastUsedDate;

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getPurchageDate() {
        return purchageDate;
    }

    public void setPurchageDate(String purchageDate) {
        this.purchageDate = purchageDate;
    }

    public String getActivationDate() {
        return activationDate;
    }

    public void setActivationDate(String activationDate) {
        this.activationDate = activationDate;
    }

    public String getLastUsedDate() {
        return lastUsedDate;
    }

    public void setLastUsedDate(String lastUsedDate) {
        this.lastUsedDate = lastUsedDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSmileNumber() {
        return smileNumber;
    }

    public void setSmileNumber(String smileNumber) {
        this.smileNumber = smileNumber;
    }
    
}
