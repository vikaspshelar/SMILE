/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.charging;

import java.math.BigDecimal;

/**
 *
 * @author paul
 */
public class ReserveResult {

    private BigDecimal centsReserved;
    private boolean stillHasBalanceLeftToReserve;
    private boolean reservationCreated;

    public BigDecimal getCentsReserved() {
        return centsReserved;
    }

    public void setCentsReserved(BigDecimal centsReserved) {
        this.centsReserved = centsReserved;
    }

    public boolean stillHasBalanceLeftToReserve() {
        return stillHasBalanceLeftToReserve;
    }

    public void setStillHasBalanceLeftToReserve(boolean stillHasBalanceLeftToReserve) {
        this.stillHasBalanceLeftToReserve = stillHasBalanceLeftToReserve;
    }

    public boolean wasReservationCreated() {
        return reservationCreated;
    }

    public void setReservationWasCreated() {
        this.reservationCreated = true;
    }
}
