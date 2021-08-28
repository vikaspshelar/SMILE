/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.model;

/**
 *
 * @author xolaniM
 */
public class ModifyUserDefinedAttributesRequest {
    String userDefinedInternetUpDownlinkSpeed;    
    String userDefinedDPIRules;

    public String getUserDefinedInternetUpDownlinkSpeed() {
        return userDefinedInternetUpDownlinkSpeed;
    }

    public void setUserDefinedInternetUpDownlinkSpeed(String userDefinedInternetUpDownlinkSpeed) {
        this.userDefinedInternetUpDownlinkSpeed = userDefinedInternetUpDownlinkSpeed;
    }

    public String getUserDefinedDPIRules() {
        return userDefinedDPIRules;
    }

    public void setUserDefinedDPIRules(String userDefinedDPIRules) {
        this.userDefinedDPIRules = userDefinedDPIRules;
    }

    @Override
    public String toString() {
        return "ModifyUserDefinedAttributesRequest{" + "userDefinedInternetUpDownlinkSpeed=" + userDefinedInternetUpDownlinkSpeed + ", userDefinedDPIRules=" + userDefinedDPIRules + '}';
    }

    
}
