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
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class VoiceUnitCredit extends SimpleUnitCredit {

    private static final Logger log = LoggerFactory.getLogger(VoiceUnitCredit.class);

    @Override
    public UCChargeResult charge(RatingKey ratingKey, BigDecimal unitsToCharge, BigDecimal originalUnitsToCharge, IAccount acc, RatingResult ratingResult, boolean isLastResort, String unitType, Date eventTimetamp) {
        UCChargeResult res;
        // This check should not be necessary but lets not assume the filter config was correct
        if (!ratingKey.getServiceCode().contains("32260") || !unitType.equals("SECOND")) {
            throw new RuntimeException("Incorrect filter setup for UnitCredit");
        }

        if (Utils.getBooleanValueFromCRDelimitedAVPString(this.getUnitCreditSpecification().getConfiguration(), "OnlyValidWithRecentDataBundle")) {
            log.debug("This bundle only allows charges if there is a recent data bundle");
            if (!hasRecentDataBundle(acc)) {
                //no recent data bundle so reservation fails
                res = new UCChargeResult();
                res.setUnitCreditInstanceId(getUnitCreditInstanceId());
                res.setPaidForUsage(DAO.isPositive(uci.getPOSCentsCharged()) && DAO.isPositive(uci.getUnitsRemaining()));
                res.setUnitsCharged(BigDecimal.ZERO);
                res.setFreeRevenueCents(BigDecimal.ZERO);
                res.setRevenueCents(BigDecimal.ZERO);
                return res;
            }
        }
        return super.charge(ratingKey, unitsToCharge, originalUnitsToCharge, acc, ratingResult, isLastResort, unitType, eventTimetamp);
    }

    @Override
    public UCReserveResult reserve(RatingKey ratingKey, BigDecimal unitsToReserve, BigDecimal originalUnitsToReserve, IAccount acc, String sessionId, Date eventTimetamp, byte[] request, int reservationSecs, RatingResult ratingResult, ServiceInstance serviceInstance, boolean isLastResort, boolean checkOnly, String unitType, String location) {
        UCReserveResult res;
        if (!ratingKey.getServiceCode().contains("32260") || !unitType.equals("SECOND")) {
            throw new RuntimeException("Incorrect filter setup for UnitCredit");
        }

        if (Utils.getBooleanValueFromCRDelimitedAVPString(this.getUnitCreditSpecification().getConfiguration(), "OnlyValidWithRecentDataBundle")) {
            log.debug("This bundle only allows reservations if there is a recent data bundle");
            if (!hasRecentDataBundle(acc)) {
                //no recent data bundle so reservation fails
                res = new UCReserveResult();
                res.setUnitsReserved(BigDecimal.ZERO);
                res.setFailureHint(BaseUtils.getProperty("env.bm.voice.res.failure.no.recent.data", "Available voice bundle but no recent data bundle"));
                return res;
            }
        }
        if (Utils.getBooleanValueFromCRDelimitedAVPString(this.getUnitCreditSpecification().getConfiguration(), "OnlyValidWithCurrentDataBundle")) {
            log.debug("This bundle only allows reservations if there is a current data bundle");
            if (!hasCurrentDataBundle(acc)) {
                //no current data bundle so reservation fails
                res = new UCReserveResult();
                res.setUnitsReserved(BigDecimal.ZERO);
                res.setFailureHint(BaseUtils.getProperty("env.bm.voice.res.failure.no.current.data", "Available voice bundle but no current data bundle"));
                return res;
            }
        }

        res = super.reserve(ratingKey, unitsToReserve, originalUnitsToReserve, acc, sessionId, eventTimetamp, request, reservationSecs, ratingResult, serviceInstance, isLastResort, checkOnly, unitType, location);
        return res;
    }

    private boolean hasRecentDataBundle(IAccount acc) {
        Date date = DAO.getAccountsLastDataBundleExpiry(em, acc);
        if (date == null) {
            log.debug("No recent data bundle");
            return false;
        }
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -30);
        Date thirtyDaysAgo = cal.getTime();
        return date.after(thirtyDaysAgo);
    }
    
    private boolean hasCurrentDataBundle(IAccount acc) {
        Date date = DAO.getAccountsLastDataBundleExpiry(em, acc);
        if (date == null) {
            log.debug("No current data bundle");
            return false;
        }
        return date.after(new Date());
    }
    
    @Override
    public boolean ignoreWhenNoUnitsLeft() {
        return true;
    }

}
