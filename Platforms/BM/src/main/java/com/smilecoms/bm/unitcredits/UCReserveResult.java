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
public class UCReserveResult {
    
    private BigDecimal unitsReserved;
    private boolean stillHasUnitsLeftToReserve;
    private BigDecimal OOBUnitRate;
    private int recommendedReservationSecs;
    private boolean reservationCreated;
    String failureHint;
    
    public BigDecimal getUnitsReserved() {
        return unitsReserved;
    }

    public void setUnitsReserved(BigDecimal unitsReserved) {
        this.unitsReserved = unitsReserved;
    }

    public boolean stillHasUnitsLeftToReserve() {
        return stillHasUnitsLeftToReserve;
    }

    public void setStillHasUnitsLeftToReserve(boolean stillHasUnitsLeftToReserve) {
        this.stillHasUnitsLeftToReserve = stillHasUnitsLeftToReserve;
    }

    public BigDecimal getOOBUnitRate() {
        return OOBUnitRate;
    }

    public void setOOBUnitRate(BigDecimal OOBUnitRate) {
        this.OOBUnitRate = OOBUnitRate;
    }

    public int getRecommendedReservationSecs() {
        return recommendedReservationSecs;
    }

    public void setRecommendedReservationSecs(int recommendedReservationSecs) {
        this.recommendedReservationSecs = recommendedReservationSecs;
    }

    public boolean wasReservationCreated() {
        return reservationCreated;
    }

    public void setReservationWasCreated() {
        this.reservationCreated = true;
    }

    public String getFailureHint() {
        return failureHint;
    }

    public void setFailureHint(String failureHint) {
        this.failureHint = failureHint;
    }
}
