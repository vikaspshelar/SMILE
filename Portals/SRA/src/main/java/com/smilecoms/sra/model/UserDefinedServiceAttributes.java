/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.model;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author xolaniM
 */
public class UserDefinedServiceAttributes {
    int currentBitsperSec;
    String userDefinedDPIRules;
    String availableDPIRules;
    List allowedSpeedList = new ArrayList();

    public String getAvailableDPIRules() {
        return availableDPIRules;
    }

    public void setAvailableDPIRules(String availableDPIRules) {
        this.availableDPIRules = availableDPIRules;
    }
    
    
    
    public UserDefinedServiceAttributes() {
    }

    public UserDefinedServiceAttributes(int currentBitsperSec, String userDefinedDPIRules, String availableDPIRules) {
        this.currentBitsperSec = currentBitsperSec;
        this.userDefinedDPIRules = userDefinedDPIRules;
        this.userDefinedDPIRules = availableDPIRules;
    }

    
    public int getCurrentBitsperSec() {
        return currentBitsperSec;
    }

    public void setCurrentBitsperSec(int currentBitsperSec) {
        this.currentBitsperSec = currentBitsperSec;
    }

    public String getUserDefinedDPIRules() {
        return userDefinedDPIRules;
    }

    public void setUserDefinedDPIRules(String userDefinedDPIRules) {
        this.userDefinedDPIRules = userDefinedDPIRules;
    }

    public List getAllowedSpeedList() {
        return allowedSpeedList;
    }

    public void setAllowedSpeedList(List allowedSpeedList) {
        this.allowedSpeedList = allowedSpeedList;
    }

    @Override
    public String toString() {
        return "UserDefinedServiceAttributes{" + "currentBitsperSec=" + currentBitsperSec + ", userDefinedDPIRules=" + userDefinedDPIRules + ", allowedSpeedList=" + allowedSpeedList + '}';
    }
    
    
}
