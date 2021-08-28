/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.model;

import com.smilecoms.commons.sca.beans.AccountBean;
import java.util.List;
import java.util.Map;

/**
 *
 * @author rajeshkumar
 */
public class AppAccount {

    private String smileVoiceNo;
    private String friendlyName = "";
    private AccountBean account;
    private List<Map<String,String>> unitCreditConfig;

    public String getSmileVoiceNo() {
        return smileVoiceNo;
    }

    public void setSmileVoiceNo(String smileVoiceNo) {
        this.smileVoiceNo = smileVoiceNo;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public AccountBean getAccount() {
        return account;
    }

    public void setAccount(AccountBean account) {
        this.account = account;
    }

    public List<Map<String, String>> getUnitCreditConfig() {
        return unitCreditConfig;
    }

    public void setUnitCreditConfig(List<Map<String, String>> unitCreditConfig) {
        this.unitCreditConfig = unitCreditConfig;
    }
}
