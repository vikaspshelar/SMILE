/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mukosi
 */
public class SpecialLevyUnitCredit extends BaseUnitCredit {

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
        UCReserveResult res = new UCReserveResult();
        
        if (lastUsed == null || (!Utils.areDatesOnSameDay(lastUsed, eventTimetamp) && lastUsed.before(eventTimetamp))) {
            log.debug("Last used [{}]", lastUsed);
            BigDecimal availableUnits = getAvailableUnitsLeft();
            
            /*if (availableUnits.compareTo(new BigDecimal(1)) >= 0) { //Has at least one unit left, then we are ok.
                // Unit credit has more units available than required. Reserve the required units
                res.setUnitsReserved(BigDecimal.ZERO);
                res.setStillHasUnitsLeftToReserve(false);
            } */
            
            if(DAO.isZero(availableUnits)) {
                // Unit credit not enough units to reserve
                throw new RuntimeException("Insufficient amount for reservation on SpecialLevyUnitCredit.");
            }
        }
        
        res.setUnitsReserved(BigDecimal.ZERO);
        res.setStillHasUnitsLeftToReserve(false);
        return res;
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
                
        if (lastUsed == null || (!Utils.areDatesOnSameDay(lastUsed, eventTimetamp) && lastUsed.before(eventTimetamp))) {
            log.debug("Last used [{}]", lastUsed);
            //Charge for 1 unit (1 day of social media usage)
            setUnitCreditUnitsRemaining(uci.getUnitsRemaining().subtract(new BigDecimal(1)));            
        }
        
        res = new UCChargeResult();
        res.setUnitCreditInstanceId(getUnitCreditInstanceId());
        res.setPaidForUsage(false);
        res.setFreeRevenueCents(BigDecimal.ZERO);
        res.setRevenueCents(BigDecimal.ZERO);
        res.setUnitsCharged(BigDecimal.ZERO);
        res.setOOBUnitRate(null);
        return res;

    }
}
