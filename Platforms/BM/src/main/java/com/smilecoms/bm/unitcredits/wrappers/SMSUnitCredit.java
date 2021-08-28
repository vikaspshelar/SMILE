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
public class SMSUnitCredit extends SimpleUnitCredit {

    private static final Logger log = LoggerFactory.getLogger(SMSUnitCredit.class);

    @Override
    public UCChargeResult charge(RatingKey ratingKey, BigDecimal unitsToCharge, BigDecimal originalUnitsToCharge, IAccount acc, RatingResult ratingResult, boolean isLastResort, String unitType, Date eventTimetamp) {

        // This check should not be necessary but lets not assume the filter config was correct
        if (!ratingKey.getServiceCode().contains("SMS") || !unitType.equals("SMS")) {
            throw new RuntimeException("Incorrect filter setup for UnitCredit");
        }
        return super.charge(ratingKey, unitsToCharge, originalUnitsToCharge, acc, ratingResult, isLastResort, unitType, eventTimetamp); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public UCReserveResult reserve(RatingKey ratingKey, BigDecimal unitsToReserve, BigDecimal originalUnitsToReserve, IAccount acc, String sessionId, Date eventTimetamp, byte[] request, int reservationSecs, RatingResult ratingResult, ServiceInstance serviceInstance, boolean isLastResort, boolean checkOnly, String unitType, String location) {
        UCReserveResult res;
        if (!ratingKey.getServiceCode().contains("SMS") || !unitType.equals("SMS")) {
            throw new RuntimeException("Incorrect filter setup for UnitCredit");
        }

        if (Utils.getBooleanValueFromCRDelimitedAVPString(this.getUnitCreditSpecification().getConfiguration(), "OnlyValidWithRecentDataBundle")) {
            log.debug("This bundle only allows reservations if there is a recent data bundle");
            if (!hasRecentDataBundle(acc)) {
                //no recent data bundle so reservation fails
                res = new UCReserveResult();
                res.setUnitsReserved(BigDecimal.ZERO);
                res.setFailureHint(BaseUtils.getProperty("env.bm.sms.res.failure.no.recent.data", "Available SMS bundle but no recent data bundle"));
                return res;
            }
        }
        if (Utils.getBooleanValueFromCRDelimitedAVPString(this.getUnitCreditSpecification().getConfiguration(), "OnlyValidWithCurrentDataBundle")) {
            log.debug("This bundle only allows reservations if there is a current data bundle");
            if (!hasCurrentDataBundle(acc)) {
                //no current data bundle so reservation fails
                res = new UCReserveResult();
                res.setUnitsReserved(BigDecimal.ZERO);
                res.setFailureHint(BaseUtils.getProperty("env.bm.sms.res.failure.no.current.data", "Available SMS bundle but no current data bundle"));
                return res;
            }
        }

        res = super.reserve(ratingKey, unitsToReserve, originalUnitsToReserve, acc, sessionId, eventTimetamp, request, reservationSecs, ratingResult, serviceInstance, isLastResort, checkOnly, unitType, location); //To change body of generated methods, choose Tools | Templates.
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

}
