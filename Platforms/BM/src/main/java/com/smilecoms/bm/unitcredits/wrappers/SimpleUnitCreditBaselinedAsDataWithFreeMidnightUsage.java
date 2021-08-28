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
import com.smilecoms.xml.schema.bm.RatingKey;
import java.math.BigDecimal;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class SimpleUnitCreditBaselinedAsDataWithFreeMidnightUsage extends SimpleUnitCreditWithFreeMidnightUsage {

    private static final Logger log = LoggerFactory.getLogger(SimpleUnitCreditBaselinedAsData.class);

    @Override
    public UCChargeResult charge(RatingKey ratingKey, BigDecimal unitsToCharge, BigDecimal originalUnitsToCharge, IAccount acc, RatingResult ratingResult, boolean isLastResort, String unitType, Date eventTimetamp) {
        UCChargeResult res;
        try {
            BigDecimal equivalentUnitsToCharge, equivalentOriginalUnitsToCharge;
            if (!unitType.equals("OCTET")) {
                equivalentUnitsToCharge = unitsToCharge.multiply(getBaselineConversionRate(ratingResult, ratingKey));
                equivalentOriginalUnitsToCharge = originalUnitsToCharge.multiply(getBaselineConversionRate(ratingResult, ratingKey));
                log.debug("This service is not natively counted in the used unit but is charged in a different unit. Converting units used to equivalent of other unit. [{}] becomes [{}]", unitsToCharge, equivalentUnitsToCharge);
            } else {
                equivalentUnitsToCharge = unitsToCharge;
                equivalentOriginalUnitsToCharge = originalUnitsToCharge;
            }

            res = super.charge(ratingKey, equivalentUnitsToCharge, equivalentOriginalUnitsToCharge, acc, ratingResult, isLastResort, unitType, eventTimetamp);

            if (!unitType.equals("OCTET")) {
                res.setBaselineUnitsCharged(res.getUnitsCharged());
                res.setUnitsCharged(DAO.divide(res.getUnitsCharged(), getBaselineConversionRate(ratingResult, ratingKey)));
                log.debug("This service was not natively counted in the used unit but was charged in a different unit. Converting units used to equivalent of other unit. [{}] becomes [{}]", res.getBaselineUnitsCharged(), res.getUnitsCharged());
                // We cannot override the rate as its for data only
                res.setOOBUnitRate(null);
            }
        } catch (IllegalArgumentException iae) {
            res = new UCChargeResult();
            res.setUnitCreditInstanceId(getUnitCreditInstanceId());
            res.setPaidForUsage(DAO.isPositive(uci.getPOSCentsCharged()) && DAO.isPositive(uci.getUnitsRemaining()));
            res.setFreeRevenueCents(BigDecimal.ZERO);
            res.setRevenueCents(BigDecimal.ZERO);
            res.setUnitsCharged(BigDecimal.ZERO);
        }
        return res;
    }

    @Override
    public UCReserveResult reserve(RatingKey ratingKey, BigDecimal unitsToReserve, BigDecimal originalUnitsToReserve, IAccount acc, String sessionId, Date eventTimetamp, byte[] request, int reservationSecs, RatingResult ratingResult, ServiceInstance serviceInstance, boolean isLastResort, boolean checkOnly, String unitType, String location) {
        UCReserveResult res;
        try {
            BigDecimal equivalentUnitsToReserve, equivalentOriginalUnitsToReserve;
            if (!unitType.equals("OCTET")) {
                equivalentUnitsToReserve = unitsToReserve.multiply(getBaselineConversionRate(ratingResult, ratingKey));
                equivalentOriginalUnitsToReserve = originalUnitsToReserve.multiply(getBaselineConversionRate(ratingResult, ratingKey));
                log.debug("This service is not natively counted in the used unit but is charged in a different unit. Converting units used to equivalent of other unit. [{}] becomes [{}]", unitsToReserve, equivalentUnitsToReserve);
            } else {
                equivalentUnitsToReserve = unitsToReserve;
                equivalentOriginalUnitsToReserve = originalUnitsToReserve;
            }

            res = super.reserve(ratingKey, equivalentUnitsToReserve, equivalentOriginalUnitsToReserve, acc, sessionId, eventTimetamp, request, reservationSecs, ratingResult, serviceInstance, isLastResort, checkOnly, unitType, location);

            if (!unitType.equals("OCTET")) {
                BigDecimal orig = res.getUnitsReserved();
                res.setUnitsReserved(DAO.divide(res.getUnitsReserved(), getBaselineConversionRate(ratingResult, ratingKey)));
                log.debug("This service was not natively counted in the used unit but was charged in a different unit. Converting units used to equivalent of other unit. [{}] becomes [{}]", orig, res.getUnitsReserved());
                // We cannot override the rate as its for data only
                res.setOOBUnitRate(null);
            }

        } catch (IllegalArgumentException iae) {
            log.debug("Got IllegalArgumentException: [{}]", iae.getMessage());
            res = new UCReserveResult();
            res.setUnitsReserved(BigDecimal.ZERO);
            res.setStillHasUnitsLeftToReserve(false);
        }
        return res;
    }


}
