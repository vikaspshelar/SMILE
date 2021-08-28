/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.unitcredits.wrappers;

import com.smilecoms.bm.charging.IAccount;
import com.smilecoms.bm.db.op.DAO;
import com.smilecoms.bm.rating.RatingResult;
import com.smilecoms.bm.unitcredits.UCChargeResult;
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
public class SimpleUnitCreditWithValidityBasedOnStart extends SimpleUnitCredit {

    private static final Logger log = LoggerFactory.getLogger(SimpleUnitCreditWithValidityBasedOnStart.class);

    @Override
    public void provision(IAccount acc, int productInstanceId, Date startDate, boolean verifyOnly,
            String extTxid, double posCentsPaidEach, double posCentsDiscountEach, int saleLineId, String info) throws Exception {

        checkProvisionRules(acc, startDate, verifyOnly, productInstanceId);
        /*
         If the bundle size is B bytes and the normal cost is N UGX and the discount amount is D UGX and the extra units is E bytes and the Free cents already included is F 
         Normal revenue rate: 
         (N-D)/(B+E) UGX per Byte
         Free revenue rate:
         (F + D + (E*N/B))/(B+E)
         */
        double units = ucs.getUnits();
        double normalRevenueRate;
        double baselineRate;
        if (getBooleanPropertyFromConfig("BaselineOnMarketPrice")) {
            baselineRate = (posCentsPaidEach + posCentsDiscountEach) / units;
        } else if (getBooleanPropertyFromConfig("BaselineOnPaidPrice")) {
            baselineRate = (posCentsPaidEach) / units;
        } else {
            int baselineCents = getIntPropertyFromConfig("BaselineCents");
            if (baselineCents > 0) {
                baselineRate = baselineCents / units;
            } else {
                // Default treatment is BaselineOnPaidPrice
                baselineRate = (posCentsPaidEach) / units;
            }
        }

        if (BaseUtils.getBooleanProperty("env.bm.uc.revenue.recognition.nett", true)) {
            normalRevenueRate = (posCentsPaidEach) / units;
        } else {
            normalRevenueRate = (posCentsPaidEach + posCentsDiscountEach) / units;
        }

        if (startDate == null) {
            startDate = new Date();
        }
        Calendar expiryDateToUse = Calendar.getInstance();
        int startByDays = getIntPropertyFromConfig("StartByDaysAfterPurchase");
        if (startByDays == -1) {
            startByDays = 90; // default in case its not set
        }
        expiryDateToUse.add(Calendar.DATE, startByDays);

        Date revenueFirstDate = null;
        Date revenueLastDate = null;
        Double revenuePerDay = null;

        if (getBooleanPropertyFromConfig("NoRevenueOnUsage") && getBooleanPropertyFromConfig("RecRevDaily")) {
            revenueFirstDate = startDate;
            revenueLastDate = expiryDateToUse.getTime();
            long seconds = Utils.getSecondsBetweenDates(revenueFirstDate, revenueLastDate);
            revenuePerDay = normalRevenueRate * units / (seconds / 86400);
            if (log.isDebugEnabled()) {
                log.debug("Revenue must be recognised daily starting on [{}] and ending on [{}] which is [{}] seconds at [{}] per day totalling [{}]",
                        revenueFirstDate, revenueLastDate, seconds, revenuePerDay, revenuePerDay * seconds / 86400);
            }
            normalRevenueRate = 0;
            baselineRate = 0;
        }

        uci = DAO.createUnitCreditInstance(em, ucs.getUnitCreditSpecificationId(), acc, expiryDateToUse.getTime(), units, normalRevenueRate, baselineRate,
                productInstanceId, posCentsPaidEach, posCentsDiscountEach, startDate, info, extTxid, saleLineId, expiryDateToUse.getTime(),
                revenueFirstDate, revenueLastDate, revenuePerDay);

        doPostProvisionProcessing(verifyOnly);
    }

    @Override
    public UCChargeResult charge(
            RatingKey ratingKey,
            BigDecimal unitsToCharge,
            BigDecimal originalUnitsToCharge,
            IAccount acc,
            RatingResult ratingResult,
            boolean isLastResort, String unitType, Date eventTimetamp) {
        if (DAO.isPositive(unitsToCharge) && neverBeenUsed()) {
            extendExpiryAndEndDate(ucs.getUsableDays(), true, false, false, null);
        }
        return super.charge(ratingKey, unitsToCharge, originalUnitsToCharge, acc, ratingResult, isLastResort, unitType, eventTimetamp);
    }

}
