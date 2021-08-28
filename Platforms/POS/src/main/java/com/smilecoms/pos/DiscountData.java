/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos;

/**
 *
 * @author paul
 */
public class DiscountData {
    private double tieredPricingPercentageOff;
    private double discountPercentageOff;

    public double getTieredPricingPercentageOff() {
        return tieredPricingPercentageOff;
    }

    public void setTieredPricingPercentageOff(double tieredPricingPercentageOff) {
        this.tieredPricingPercentageOff = tieredPricingPercentageOff;
    }

    public double getDiscountPercentageOff() {
        return discountPercentageOff;
    }

    public void setDiscountPercentageOff(double discountPercentageOff) {
        this.discountPercentageOff = discountPercentageOff;
    }

    boolean hasReductions() {
        return (tieredPricingPercentageOff > 0 || discountPercentageOff > 0);
    }
    
    
}
