/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.charging;

import com.smilecoms.commons.util.Utils;
import java.util.Set;

/**
 *
 * @author paul
 */
public class ServiceRules {

    private boolean clearAllButStickyDPIRules = false;
    private boolean clearAllDPIRules = false;
    private Long maxBpsUp = null;
    private Long maxBpsDown = null;
    private Integer qci = null;
    private Set<String> systemDefinedDpiRulesToAdd = null;
    private Set<String> systemDefinedDpiRulesToRemove = null;

    @Override
    public String toString() {
        return "ServiceRules{" + "clearAllButStickyDPIRules=" + clearAllButStickyDPIRules + ", clearAllDPIRules=" + 
                clearAllDPIRules + ", maxBpsUp=" + maxBpsUp + ", maxBpsDown=" + maxBpsDown + 
                ", qci=" + qci + ", systemDefinedDpiRulesToAdd=" + Utils.makeCommaDelimitedString(systemDefinedDpiRulesToAdd) +
                ", systemDefinedDpiRulesToRemove=" + Utils.makeCommaDelimitedString(systemDefinedDpiRulesToRemove) + '}';
    }

    
    public boolean isClearAllButStickyDPIRules() {
        return clearAllButStickyDPIRules;
    }

    public void setClearAllButStickyDPIRules(boolean clearAllButStickyDPIRules) {
        this.clearAllButStickyDPIRules = clearAllButStickyDPIRules;
    }

    public boolean isClearAllDPIRules() {
        return clearAllDPIRules;
    }

    public void setClearAllDPIRules(boolean clearAllDPIRules) {
        this.clearAllDPIRules = clearAllDPIRules;
    }
    
    public Long getMaxBpsUp() {
        return maxBpsUp;
    }

    public void setMaxBpsUp(Long maxBpsUp) {
        this.maxBpsUp = maxBpsUp;
    }

    public Long getMaxBpsDown() {
        return maxBpsDown;
    }

    public void setMaxBpsDown(Long maxBpsDown) {
        this.maxBpsDown = maxBpsDown;
    }

    public Integer getQci() {
        return qci;
    }

    public void setQci(Integer qci) {
        this.qci = qci;
    }

    public Set<String> getSystemDefinedDpiRulesToAdd() {
        return systemDefinedDpiRulesToAdd;
    }

    public void setSystemDefinedDpiRulesToAdd(Set<String> systemDefinedDpiRulesToAdd) {
        this.systemDefinedDpiRulesToAdd = systemDefinedDpiRulesToAdd;
    }

    public Set<String> getSystemDefinedDpiRulesToRemove() {
        return systemDefinedDpiRulesToRemove;
    }

    public void setSystemDefinedDpiRulesToRemove(Set<String> systemDefinedDpiRulesToRemove) {
        this.systemDefinedDpiRulesToRemove = systemDefinedDpiRulesToRemove;
    }
    
}
