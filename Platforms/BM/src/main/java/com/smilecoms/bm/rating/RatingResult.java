/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.rating;

import com.smilecoms.commons.util.Utils;
import java.math.BigDecimal;

/**
 *
 * @author paul
 */
public class RatingResult {

    private BigDecimal retailRateCentsPerUnit = BigDecimal.ZERO;
    private boolean sessionBased;
    private boolean eventBased;
    private BigDecimal fromInterconnectRateCentsPerUnit = BigDecimal.ZERO;
    private BigDecimal toInterconnectRateCentsPerUnit = BigDecimal.ZERO;
    private int rateId;
    private String fromInterconnectCurrency;
    private String toInterconnectCurrency;
    private String ratingHint;
    private String description;

    public BigDecimal getRetailRateCentsPerUnit() {
        return retailRateCentsPerUnit;
    }

    public void setRetailRateCentsPerUnit(BigDecimal retailRateCentsPerUnit) {
        this.retailRateCentsPerUnit = retailRateCentsPerUnit;
    }

    public BigDecimal getFromInterconnectRateCentsPerUnit() {
        return fromInterconnectRateCentsPerUnit;
    }

    public void setFromInterconnectRateCentsPerUnit(BigDecimal fromInterconnectRateCentsPerUnit) {
        this.fromInterconnectRateCentsPerUnit = fromInterconnectRateCentsPerUnit;
    }

    public BigDecimal getToInterconnectRateCentsPerUnit() {
        return toInterconnectRateCentsPerUnit;
    }

    public void setToInterconnectRateCentsPerUnit(BigDecimal toInterconnectRateCentsPerUnit) {
        this.toInterconnectRateCentsPerUnit = toInterconnectRateCentsPerUnit;
    }

    public int getRateId() {
        return rateId;
    }

    public void setRateId(int rateId) {
        this.rateId = rateId;
    }

    public boolean isEventBased() {
        return eventBased;
    }

    public void setEventBased(boolean eventBased) {
        this.eventBased = eventBased;
    }

    public boolean isSessionBased() {
        return sessionBased;
    }

    public void setSessionBased(boolean sessionBased) {
        this.sessionBased = sessionBased;
    }

    public String getFromInterconnectCurrency() {
        return fromInterconnectCurrency;
    }

    public void setFromInterconnectCurrency(String fromInterconnectCurrency) {
        this.fromInterconnectCurrency = fromInterconnectCurrency;
    }

    public String getToInterconnectCurrency() {
        return toInterconnectCurrency;
    }

    public void setToInterconnectCurrency(String toInterconnectCurrency) {
        this.toInterconnectCurrency = toInterconnectCurrency;
    }

    public String getRatingHint() {
        return ratingHint;
    }

    public void setRatingHint(String ratingHint) {
        this.ratingHint = ratingHint;
    }

    public BigDecimal getBigDecimalPropertyFromHint(String propName) {
        return BigDecimal.valueOf(getLongPropertyFromHint(propName));
    }

    public boolean getBooleanPropertyFromHint(String propName) {
        String val = getPropertyFromHint(propName);
        if (val == null) {
            return false;
        }
        if (val.equalsIgnoreCase("true")) {
            return true;
        }
        return false;
    }

    public String getPropertyFromHint(String propName) {
        if (ratingHint == null) {
            return null;
        }
        return Utils.getValueFromCRDelimitedAVPString(ratingHint, propName);
    }

    public int getIntPropertyFromHint(String propName) {
        String val = getPropertyFromHint(propName);
        if (val == null) {
            return -1;
        }
        return Integer.parseInt(val);
    }

    public long getLongPropertyFromHint(String propName) {
        String val = getPropertyFromHint(propName);
        if (val == null) {
            return -1;
        }
        return Long.parseLong(val);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
}
