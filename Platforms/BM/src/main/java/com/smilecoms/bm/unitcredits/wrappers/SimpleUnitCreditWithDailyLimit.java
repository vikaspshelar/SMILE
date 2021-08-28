/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.unitcredits.wrappers;

import com.smilecoms.bm.charging.IAccount;
import com.smilecoms.bm.db.model.ServiceInstance;
import com.smilecoms.bm.db.op.DAO;
import com.smilecoms.bm.rating.RatingResult;
import com.smilecoms.bm.unitcredits.UCChargeResult;
import com.smilecoms.bm.unitcredits.UCReserveResult;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.math.BigDecimal;
import java.util.Date;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class SimpleUnitCreditWithDailyLimit extends SimpleUnitCredit {

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
        BigDecimal unitsUsedToday = uci.getAuxCounter1();

        if ((lastUsed != null && !Utils.areDatesOnSameDay(lastUsed, eventTimetamp) && lastUsed.before(eventTimetamp))
                || uci.getAuxCounter1() == null) {
            log.debug("Last used [{}]", lastUsed);
            unitsUsedToday = BigDecimal.ZERO;
        }
        
        BigDecimal dailyLimitUnits = getBigDecimalPropertyFromConfig("DailyLimitUnits");
        BigDecimal daysUnitsLeft = dailyLimitUnits.subtract(unitsUsedToday);
        if (DAO.isZeroOrNegative(daysUnitsLeft)) {
            log.debug("Daily limit has been reached");
            UCReserveResult res = new UCReserveResult();
            res.setUnitsReserved(BigDecimal.ZERO);
            res.setStillHasUnitsLeftToReserve(false);
            return res;
        }
        
        log.debug("We have [{}] units left today", daysUnitsLeft);
        if (daysUnitsLeft.compareTo(unitsToReserve) >= 0) {
            // Process as normal
            return super.reserve(ratingKey, unitsToReserve, originalUnitsToReserve, acc, sessionId, eventTimetamp, request, reservationSecs, ratingResult, serviceInstance, isLastResort, checkOnly, unitType, location);
        } else {
            log.debug("We do not have enough units left today to reserve everything being requested");
            return super.reserve(ratingKey, daysUnitsLeft, originalUnitsToReserve, acc, sessionId, eventTimetamp, request, reservationSecs, ratingResult, serviceInstance, isLastResort, checkOnly, unitType, location);
        }
        
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
        if ((lastUsed != null && !Utils.areDatesOnSameDay(lastUsed, eventTimetamp) && lastUsed.before(eventTimetamp))
                || uci.getAuxCounter1() == null) {
            log.debug("Last used [{}]", lastUsed);
            setAuxCounter1(BigDecimal.ZERO);
        }

        BigDecimal dailyLimitUnits = getBigDecimalPropertyFromConfig("DailyLimitUnits");
        BigDecimal daysUnitsLeft = dailyLimitUnits.subtract(uci.getAuxCounter1());
        log.debug("We have [{}] units left today", daysUnitsLeft);
        if (DAO.isZeroOrNegative(daysUnitsLeft)) {
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
        
        if (daysUnitsLeft.compareTo(unitsToCharge) >= 0) {
            log.debug("We have enough in daily limit to try and charge everything");
            res = super.charge(ratingKey, unitsToCharge, originalUnitsToCharge, acc, ratingResult, isLastResort, unitType, eventTimetamp);
        } else {
            log.debug("We dont have enough in daily limit so will will only try and charge the amount left [{}]", daysUnitsLeft);
            res = super.charge(ratingKey, daysUnitsLeft, originalUnitsToCharge, acc, ratingResult, isLastResort, unitType, eventTimetamp);
        }
        setAuxCounter1(uci.getAuxCounter1().add(res.getUnitsCharged()));
        return res;
    }

}
