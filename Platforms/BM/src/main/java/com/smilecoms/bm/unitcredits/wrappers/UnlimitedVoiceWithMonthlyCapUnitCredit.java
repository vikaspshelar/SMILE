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
 * @author richard
 */
public class UnlimitedVoiceWithMonthlyCapUnitCredit extends VoiceUnitCredit {

    private static final Logger log = LoggerFactory.getLogger(VoiceUnitCredit.class);

    @Override
    public UCChargeResult charge(RatingKey ratingKey, BigDecimal unitsToCharge, BigDecimal originalUnitsToCharge, IAccount acc, RatingResult ratingResult, boolean isLastResort, String unitType, Date eventTimetamp) {
        Date lastUsed = uci.getLastUsedDate();
        UCChargeResult res;
        if ((lastUsed != null && !Utils.areDatesInSameMonth(lastUsed, eventTimetamp) && lastUsed.before(eventTimetamp))
                || uci.getAuxCounter1() == null) {
            log.debug("Last used [{}]", lastUsed);
            setAuxCounter1(BigDecimal.ZERO);
        }
        
        BigDecimal allowedToCharge = getBigDecimalPropertyFromConfig("MonthlySecondsCap").subtract(uci.getAuxCounter1());
        if (allowedToCharge.compareTo(BigDecimal.ZERO) < 1 /*<=*/) {
            log.debug("Monthly usage [{}], more than monthly cap [{}] so reservation not allowed");
            //no recent data bundle so reservation fails
            res = new UCChargeResult();
            res.setUnitCreditInstanceId(getUnitCreditInstanceId());
            res.setPaidForUsage(DAO.isPositive(uci.getPOSCentsCharged()) && DAO.isPositive(uci.getUnitsRemaining()));
            res.setUnitsCharged(BigDecimal.ZERO);
            res.setFreeRevenueCents(BigDecimal.ZERO);
            res.setRevenueCents(BigDecimal.ZERO);
            return res;
        }
        
        if (allowedToCharge.compareTo(unitsToCharge) >= 0 /*>=*/) {
            log.debug("Requested units to reserve is less or equal to allowed to reserve, so we can reserve the full amount");
            res = super.charge(ratingKey, unitsToCharge, originalUnitsToCharge, acc, ratingResult, isLastResort, unitType, eventTimetamp);
        } else {
            log.debug("Requested units to reserve is more than allowed to reserve, so we can only reserve allowed units");
            res = super.charge(ratingKey, allowedToCharge, originalUnitsToCharge, acc, ratingResult, isLastResort, unitType, eventTimetamp);
        }
        
        setAuxCounter1(uci.getAuxCounter1().add(unitsToCharge));
        return res;
    }

    @Override
    public UCReserveResult reserve(RatingKey ratingKey, BigDecimal unitsToReserve, BigDecimal originalUnitsToReserve, IAccount acc, String sessionId, Date eventTimetamp, byte[] request, int reservationSecs, RatingResult ratingResult, ServiceInstance serviceInstance, boolean isLastResort, boolean checkOnly, String unitType, String location) {
        UCReserveResult res;
        Date lastUsed = uci.getLastUsedDate();
        if ((lastUsed != null && !Utils.areDatesInSameMonth(lastUsed, eventTimetamp) && lastUsed.before(eventTimetamp))
                || uci.getAuxCounter1() == null) {
            log.debug("Last used [{}]", lastUsed);
            setAuxCounter1(BigDecimal.ZERO);
        }
        
        BigDecimal allowedToReserve = getBigDecimalPropertyFromConfig("MonthlySecondsCap").subtract(uci.getAuxCounter1());
        if (allowedToReserve.compareTo(BigDecimal.ZERO) < 1 /*<=*/) {
            log.debug("Monthly usage [{}], more than monthly cap [{}] so reservation not allowed");
            //no recent data bundle so reservation fails
            res = new UCReserveResult();
            res.setUnitsReserved(BigDecimal.ZERO);
            return res;
        }
        
        if (allowedToReserve.compareTo(unitsToReserve) >= 0 /*>=*/) {
            log.debug("Requested units to reserve is less or equal to allowed to reserve, so we can reserve the full amount");
            res = super.reserve(ratingKey, unitsToReserve, originalUnitsToReserve, acc, sessionId, eventTimetamp, request, reservationSecs, ratingResult, serviceInstance, isLastResort, checkOnly, unitType, location);
        } else {
            log.debug("Requested units to reserve is more than allowed to reserve, so we can only reserve allowed units");
            res = super.reserve(ratingKey, allowedToReserve, originalUnitsToReserve, acc, sessionId, eventTimetamp, request, reservationSecs, ratingResult, serviceInstance, isLastResort, checkOnly, unitType, location);
        }
        
        
        return res;

    }

}
