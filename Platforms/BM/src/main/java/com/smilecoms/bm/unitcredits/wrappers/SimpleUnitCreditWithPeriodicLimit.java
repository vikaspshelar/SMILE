/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.unitcredits.wrappers;

import com.smilecoms.bm.EventHelper;
import com.smilecoms.bm.charging.IAccount;
import com.smilecoms.bm.db.model.ServiceInstance;
import com.smilecoms.bm.db.op.DAO;
import com.smilecoms.bm.rating.RatingResult;
import com.smilecoms.bm.unitcredits.UCChargeResult;
import com.smilecoms.bm.unitcredits.UCReserveResult;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class SimpleUnitCreditWithPeriodicLimit extends SimpleUnitCredit {

    private static final Logger log = LoggerFactory.getLogger(SimpleUnitCreditWithDailyLimit.class);

    @Override
    public UCReserveResult reserve(
            RatingKey ratingKey,
            BigDecimal unitsToReserve,
            BigDecimal originalUnitsToReserve,
            IAccount acc,
            String sessionId,
            Date eventTimetamp,
            byte[] request,
            int reservationSecs,
            RatingResult ratingResult,
            ServiceInstance serviceInstance,
            boolean isLastResort,
            boolean checkOnly,
            String unitType,
            String location) {

        
        Date lastUsed = uci.getLastUsedDate();
        BigDecimal unitsUsedCurrentPeriod = uci.getAuxCounter1();
        
        int periodLengthInDays = getIntPropertyFromConfig("PeriodLengthInDays");
        
        if(periodLengthInDays <= 0) { // Default to daily basis ....
            periodLengthInDays = 1;
        } 
        
        int daysSinceUciGotPurchased = Utils.getDaysBetweenDates(uci.getPurchaseDate(), eventTimetamp);
        
        Calendar currentPeriodStartDate = Calendar.getInstance();
        currentPeriodStartDate.setTime(uci.getPurchaseDate()); 
        
        currentPeriodStartDate.add(Calendar.DATE, (daysSinceUciGotPurchased/periodLengthInDays) * periodLengthInDays);
        
        //See if the current period should always start at  midnight?
        boolean currentPeriodsAlwaysStartAtMidnight = getBooleanPropertyFromConfig("PeriodsAlwaysStartAtMidnight");
        if(currentPeriodsAlwaysStartAtMidnight) {
            currentPeriodStartDate.set(Calendar.HOUR_OF_DAY, 0);
            currentPeriodStartDate.set(Calendar.MINUTE, 0);
            currentPeriodStartDate.set(Calendar.SECOND, 0);
            currentPeriodStartDate.set(Calendar.MILLISECOND, 0);
        }
        
        if ((lastUsed != null && lastUsed.before(currentPeriodStartDate.getTime()) && lastUsed.before(eventTimetamp)) || uci.getAuxCounter1() == null) {
            log.debug("Last used [{}]", lastUsed);
            unitsUsedCurrentPeriod = BigDecimal.ZERO;
            // This is first reservation for today, so send any notifications if any?
            doPeriodicLimitPeriodStartProcessing();
         }
        
        BigDecimal periodicLimitUnits = getBigDecimalPropertyFromConfig("PeriodicLimitUnits");
        BigDecimal periodUnitsLeft = periodicLimitUnits.subtract(unitsUsedCurrentPeriod);
        if (DAO.isZeroOrNegative(periodUnitsLeft)) {
            log.debug("Period limit has been reached");
            UCReserveResult res = new UCReserveResult();
            res.setUnitsReserved(BigDecimal.ZERO);
            res.setStillHasUnitsLeftToReserve(false);
            return res;
        }
        
        log.debug("We have [{}] units left for this period", periodUnitsLeft);
        if (periodUnitsLeft.compareTo(unitsToReserve) >= 0) {
            // Process as normal
            return super.reserve(ratingKey, unitsToReserve, originalUnitsToReserve, acc, sessionId, eventTimetamp, request, reservationSecs, ratingResult, serviceInstance, isLastResort, checkOnly, unitType, location);
        } else {
            log.debug("We do not have enough units left for this period to reserve everything being requested");
            return super.reserve(ratingKey, periodUnitsLeft, originalUnitsToReserve, acc, sessionId, eventTimetamp, request, reservationSecs, ratingResult, serviceInstance, isLastResort, checkOnly, unitType, location);
        }
        
    }
    
     @Override
     public boolean hasCurrentUnitsLeft() {
        //For SimpleUnitCreditWithPeriodicLimit, check if the bundle still has units left for the current period
        BigDecimal unitsUsedCurrentPeriod = uci.getAuxCounter1();
        
        Date lastUsed = uci.getLastUsedDate();
                
        int periodLengthInDays = getIntPropertyFromConfig("PeriodLengthInDays");
        
        if(periodLengthInDays <= 0) { // Default to daily basis ....
            periodLengthInDays = 1;
        } 
        
        int daysSinceUciGotPurchased = Utils.getDaysBetweenDates(uci.getPurchaseDate(), new Date());
        
        Calendar currentPeriodStartDate = Calendar.getInstance();
        
        currentPeriodStartDate.setTime(uci.getPurchaseDate()); 
        
        currentPeriodStartDate.add(Calendar.DATE, (daysSinceUciGotPurchased/periodLengthInDays) * periodLengthInDays);
        
        boolean currentPeriodsAlwaysStartAtMidnight = getBooleanPropertyFromConfig("PeriodsAlwaysStartAtMidnight");
        if(currentPeriodsAlwaysStartAtMidnight) {
            currentPeriodStartDate.set(Calendar.HOUR_OF_DAY, 0);
            currentPeriodStartDate.set(Calendar.MINUTE, 0);
            currentPeriodStartDate.set(Calendar.SECOND, 0);
            currentPeriodStartDate.set(Calendar.MILLISECOND, 0);
        }
        
        if ((lastUsed != null && lastUsed.before(currentPeriodStartDate.getTime())) || uci.getAuxCounter1() == null) {
            unitsUsedCurrentPeriod = BigDecimal.ZERO;
        }
        
        BigDecimal periodicLimitUnits = getBigDecimalPropertyFromConfig("PeriodicLimitUnits");
        BigDecimal periodUnitsLeft = periodicLimitUnits.subtract(unitsUsedCurrentPeriod);
        return DAO.isPositive(periodUnitsLeft);
            
        //return DAO.isPositive(getCurrentUnitsLeft());
    }

    @Override
    public UCChargeResult charge(
            RatingKey ratingKey,
            BigDecimal unitsToCharge,
            BigDecimal originalUnitsToCharge,
            IAccount acc,
            RatingResult ratingResult,
            boolean isLastResort, String unitType, Date eventTimetamp) {

        Date lastUsed = uci.getLastUsedDate();
        UCChargeResult res;
        
        
        int periodLengthInDays = getIntPropertyFromConfig("PeriodLengthInDays");
        
        if(periodLengthInDays <= 0) { // Default to daily basis ....
            periodLengthInDays = 1;
        } 
        
        int daysSinceUciGotPurchased = Utils.getDaysBetweenDates(uci.getPurchaseDate(), eventTimetamp);
        
        Calendar currentPeriodStartDate = Calendar.getInstance();
        
        currentPeriodStartDate.setTime(uci.getPurchaseDate()); 
        
        currentPeriodStartDate.add(Calendar.DATE, (daysSinceUciGotPurchased/periodLengthInDays) * periodLengthInDays);
        
        boolean currentPeriodsAlwaysStartAtMidnight = getBooleanPropertyFromConfig("PeriodsAlwaysStartAtMidnight");
        if(currentPeriodsAlwaysStartAtMidnight) {
            currentPeriodStartDate.set(Calendar.HOUR_OF_DAY, 0);
            currentPeriodStartDate.set(Calendar.MINUTE, 0);
            currentPeriodStartDate.set(Calendar.SECOND, 0);
            currentPeriodStartDate.set(Calendar.MILLISECOND, 0);
        }
        
        if ((lastUsed != null && lastUsed.before(currentPeriodStartDate.getTime()) && lastUsed.before(eventTimetamp)) || uci.getAuxCounter1() == null) {
            log.debug("Last used [{}]", lastUsed);
            setAuxCounter1(BigDecimal.ZERO);
        }
            
        BigDecimal periodicLimitUnits = getBigDecimalPropertyFromConfig("PeriodicLimitUnits");
        BigDecimal periodUnitsLeft = periodicLimitUnits.subtract(uci.getAuxCounter1());
        log.debug("We have [{}] units left today", periodUnitsLeft);
        if (DAO.isZeroOrNegative(periodUnitsLeft)) {
            log.debug("We have no units left today");
            res = new UCChargeResult();
            res.setUnitCreditInstanceId(getUnitCreditInstanceId());
            res.setPaidForUsage(DAO.isPositive(uci.getPOSCentsCharged()) && DAO.isPositive(uci.getUnitsRemaining()));
            res.setFreeRevenueCents(BigDecimal.ZERO);
            res.setRevenueCents(BigDecimal.ZERO);
            res.setUnitsCharged(BigDecimal.ZERO);
            res.setOOBUnitRate(null);
            return res;
        }
        
        if (periodUnitsLeft.compareTo(unitsToCharge) >= 0) {
            log.debug("We have enough in periodic limit to try and charge everything");
            res = super.charge(ratingKey, unitsToCharge, originalUnitsToCharge, acc, ratingResult, isLastResort, unitType, eventTimetamp);
        } else {
            log.debug("We dont have enough in periodic limit so will will only try and charge the amount left [{}]", periodUnitsLeft);
            res = super.charge(ratingKey, periodUnitsLeft, originalUnitsToCharge, acc, ratingResult, isLastResort, unitType, eventTimetamp);
        }
        setAuxCounter1(uci.getAuxCounter1().add(res.getUnitsCharged()));
        return res;
    }

    /*public String getPropertyFromConfig(String propName) {
        return UnitCreditManager.getPropertyFromConfig(ucs, propName);
    }*/
   
    
}
