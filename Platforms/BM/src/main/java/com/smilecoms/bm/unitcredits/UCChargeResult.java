/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.unitcredits;

import java.math.BigDecimal;

/**
 *
 * @author paul
 */
public class UCChargeResult {
    
    private BigDecimal revenueCents;
    private BigDecimal freeRevenueCents;
    private BigDecimal unitsCharged;
    private BigDecimal OOBUnitRate;
    private BigDecimal baselineUnitsCharged = BigDecimal.ZERO;
    private BigDecimal unitsRemaining;
    private int unitCreditInstanceId;
    private boolean paidForUsage = false;
    
    public BigDecimal getRevenueCents() {
        return revenueCents;
    }

    public void setRevenueCents(BigDecimal revenueCents) {
        this.revenueCents = revenueCents;
    }

    public BigDecimal getUnitsCharged() {
        return unitsCharged;
    }

    public void setUnitsCharged(BigDecimal unitsCharged) {
        this.unitsCharged = unitsCharged;
    }

    public BigDecimal getFreeRevenueCents() {
        return freeRevenueCents;
    }

    public void setFreeRevenueCents(BigDecimal freeRevenueCents) {
        this.freeRevenueCents = freeRevenueCents;
    }

    public BigDecimal getOOBUnitRate() {
        return OOBUnitRate;
    }

    public void setOOBUnitRate(BigDecimal OOBUnitRate) {
        this.OOBUnitRate = OOBUnitRate;
    }

    public BigDecimal getBaselineUnitsCharged() {
        return baselineUnitsCharged;
    }

    public void setBaselineUnitsCharged(BigDecimal baselineUnitsCharged) {
        this.baselineUnitsCharged = baselineUnitsCharged;
    }

    public boolean isPaidForUsage() {
        return paidForUsage;
    }

    public void setPaidForUsage(boolean paidForUsage) {
        this.paidForUsage = paidForUsage;
    }

    public BigDecimal getUnitsRemaining() {
        return unitsRemaining;
    }

    public void setUnitsRemaining(BigDecimal unitsRemaining) {
        this.unitsRemaining = unitsRemaining;
    }

    public int getUnitCreditInstanceId() {
        return unitCreditInstanceId;
    }

    public void setUnitCreditInstanceId(int unitCreditInstanceId) {
        this.unitCreditInstanceId = unitCreditInstanceId;
    }
    
}
