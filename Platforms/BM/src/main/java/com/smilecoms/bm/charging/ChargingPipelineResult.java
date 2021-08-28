/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.charging;

import java.math.BigDecimal;

/**
 *
 * @author paul
 */
public class ChargingPipelineResult {

    private IAccount account;
    private BigDecimal revenueCents;
    private boolean considerAsRevenueGenerating = false;

    public IAccount getAccount() {
        return account;
    }

    public void setAccount(IAccount account) {
        this.account = account;
    }

    public BigDecimal getRevenueCents() {
        return revenueCents;
    }

    public void setRevenueCents(BigDecimal revenueCents) {
        this.revenueCents = revenueCents;
    }

    public boolean getConsiderAsRevenueGenerating() {
        return considerAsRevenueGenerating;
    }

    public void setConsiderAsRevenueGenerating(boolean considerAsRevenueGenerating) {
        this.considerAsRevenueGenerating = considerAsRevenueGenerating;
    }

}
