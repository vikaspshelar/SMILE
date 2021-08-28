/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jaybeepee
 */
public class SCSCF {
    private static final Logger log = LoggerFactory.getLogger(SCSCF.class);
    private String scscfName;
    private int maxLoadPercentage;
    private int currentLoad;
    private int currentLoadPercentage;
    private int deltarequired;
    private boolean available;
    private double weight;

    public SCSCF(String scscfName, int maxLoadPercentage, int currentLoad, int weightPercentage) {
        log.debug("Creating new SCSCF [{}] with maxLoadPercentage [{}], currentLoad [{}] and weight [{}]", new Object[]{scscfName, maxLoadPercentage, currentLoad, weightPercentage});
        this.scscfName = scscfName;
        this.currentLoad = currentLoad;
        this.maxLoadPercentage = maxLoadPercentage;
        this.available = true;
        this.weight = (double)weightPercentage/100;
    }

    public int getCurrentLoad() {
        return currentLoad;
    }

    public void setCurrentLoad(int currentLoad) {
        this.currentLoad = currentLoad;
    }

    
    public String getScscfName() {
        return scscfName;
    }

    public void setScscfName(String scscfName) {
        this.scscfName = scscfName;
    }

    public int getMaxLoadPercentage() {
        return maxLoadPercentage;
    }

    public void setMaxLoadPercentage(int MaxLoadPercentage) {
        this.maxLoadPercentage = MaxLoadPercentage;
    }

    public int getCurrentLoadPercentage() {
        return currentLoadPercentage;
    }

    public void setCurrentLoadPercentage(int currentLoadPercentage) {
        this.currentLoadPercentage = currentLoadPercentage;
    }

    public int getDeltarequired() {
        return deltarequired;
    }

    public void setDeltarequired(int deltarequired) {
        this.deltarequired = deltarequired;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    double getProbability() {
        return this.weight;
    }
    
    void setProbability(double weight) {
        this.weight = weight;
    }
    
}
