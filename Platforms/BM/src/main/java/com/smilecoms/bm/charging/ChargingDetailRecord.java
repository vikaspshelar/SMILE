/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.charging;

import java.math.BigDecimal;
import java.util.Date;

/**
 *
 * @author paul
 */
public class ChargingDetailRecord {
    public long accountHistoryId;
    
    // Data from this event within the session
    public Date chargeDateTime;
    public BigDecimal eventBalanceRemaining;
    public BigDecimal eventUnits;
    public String eventLocation;
    public long accountId;
    public long eventDurationMillis;
    public BigDecimal eventRevenueCents;
    public BigDecimal eventFreeRevenueCents;
    public BigDecimal eventUnitCreditUnits;
    public BigDecimal eventUnitCreditBaselineUnits;
    public BigDecimal eventAccountCents;
    public int serviceInstanceId;
    
}
