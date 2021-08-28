/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.unitcredits.wrappers;

import com.smilecoms.bm.charging.IAccount;
import com.smilecoms.bm.charging.ServiceRules;
import com.smilecoms.bm.charging.ServiceSpecHelper;
import com.smilecoms.bm.db.model.ServiceInstance;
import com.smilecoms.bm.db.op.DAO;
import com.smilecoms.bm.rating.RatingResult;
import com.smilecoms.bm.unitcredits.UCChargeResult;
import com.smilecoms.bm.unitcredits.UCReserveResult;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.ExceptionManager;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import org.slf4j.*;

public class UnlimitedPlusUnitCredit extends BaseUnitCredit {

    private static final Logger log = LoggerFactory.getLogger(UnlimitedPlusUnitCredit.class);

    @Override
    /**
     * We want Unlimited UC to always act like its got units left
     */
    public boolean hasAvailableUnitsLeft() {
        return true;
    }

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

        UCReserveResult res = new UCReserveResult();
        res.setOOBUnitRate(null);
        BigDecimal availableUnits = getAvailableUnitsLeft();
        log.debug("Unlimited Plus Unit Credit Instance [{}] has [{}] units available (i.e. current units - reserved units)", uci.getUnitCreditInstanceId(), availableUnits);

        // This bundle always acts like it has units left and says that it reserved all the units requested
        res.setStillHasUnitsLeftToReserve(true);
        res.setUnitsReserved(unitsToReserve);
        if (!checkOnly) {
            ensureDayNightCapsAreInPlace(serviceInstance);
            log.debug("We must ensure that the service has no system defined DPI Rules");
            ServiceRules rules = new ServiceRules();
            rules.setClearAllButStickyDPIRules(true);
            ServiceSpecHelper.applyServiceRules(serviceInstance, rules);
        }

        DAO.createOrUpdateReservation(
                em,
                acc.getAccountId(),
                sessionId,
                uci.getUnitCreditInstanceId(),
                BigDecimal.ZERO,
                eventTimetamp,
                request,
                reservationSecs,
                unitsToReserve,
                checkOnly);
        res.setReservationWasCreated();
        availableUnitsLeft = null;
        log.debug("Successfully reserved [{}] units on unlimited plus unit credit", unitsToReserve);
        return res;
    }

    @Override
    public UCChargeResult charge(
            RatingKey ratingKey,
            BigDecimal unitsToCharge,
            BigDecimal originalUnitsToCharge,
            IAccount acc,
            RatingResult ratingResult,
            boolean isLastResort, 
            String unitType, 
            Date eventTimetamp) {
        // We always indicate that we have charged the amount requested to be charged. The revenue is zero when we dont actually have units left
        UCChargeResult res = new UCChargeResult();
        res.setUnitCreditInstanceId(getUnitCreditInstanceId());
        res.setPaidForUsage(DAO.isPositive(uci.getPOSCentsCharged()));
        res.setUnitsCharged(unitsToCharge);
        res.setOOBUnitRate(null);

        BigDecimal currentUnits = getCurrentUnitsLeft();
        BigDecimal availableUnits = getAvailableUnitsLeft();
        BigDecimal positiveUnitsCharged;
        if (currentUnits.compareTo(unitsToCharge) > 0) {
            // Unit credit has more units available than required. Subtract the required units
            if (log.isDebugEnabled()) {
                log.debug("Unlimited Plus Unit credit has [{}] units available which is more than required. Deducting [{}] units from UC Instance[{}]", new Object[]{availableUnits, unitsToCharge, uci.getUnitCreditInstanceId()});
            }
            setUnitCreditUnitsRemaining(currentUnits.subtract(unitsToCharge));
            positiveUnitsCharged = unitsToCharge;
        } else {
            // Unit credit has just enough or not enough
            if (log.isDebugEnabled()) {
                log.debug("Unlimited Plus Unit credit has [{}] units available which is not as much as required or exactly whats required. Deducting [{}] units from UC Instance[{}]. It may go negative which is fine", new Object[]{currentUnits, unitsToCharge, uci.getUnitCreditInstanceId()});
            }
            setUnitCreditUnitsRemaining(currentUnits.subtract(unitsToCharge));
            if (DAO.isPositive(currentUnits)) {
                positiveUnitsCharged = currentUnits;
            } else {
                positiveUnitsCharged = BigDecimal.ZERO;
            }
        }
        res.setRevenueCents(positiveUnitsCharged.multiply(uci.getRevenueCentsPerUnit())); // Revenue is the unit credit rate X unit credit units that could be covered by the UC
        res.setFreeRevenueCents((unitsToCharge.subtract(positiveUnitsCharged)).multiply(uci.getFreeCentsPerUnit())); // Free Revenue is the free unit credit rate X unit credit units that went negative
        res.setUnitsRemaining(getCurrentUnitsLeft());
        if (log.isDebugEnabled()) {
            log.debug("Successfully charged [{}] units on Unlimited Plus unit credit. This is [{}]cents revenue. Positive units charged was [{}]", new Object[]{res.getUnitsCharged(), res.getRevenueCents(), positiveUnitsCharged});
        }
        return res;
    }

    @Override
    public void provision(IAccount acc, int productInstanceId, Date startDate, boolean verifyOnly,
            String extTxid, double posCentsPaidEach, double posCentsDiscountEach, int saleLineId, String info) throws Exception {

        checkProvisionRules(acc, startDate, verifyOnly, productInstanceId);

        double units = ucs.getUnits();

        Date maxDate = DAO.getMaxExpiryDateByUnitCreditWrapperClass(em, acc.getAccountId(), this.getClass().getSimpleName());

        Calendar oneCalendarMonthAfterMaxExpiry = Calendar.getInstance();
        oneCalendarMonthAfterMaxExpiry.setTime(maxDate);
        oneCalendarMonthAfterMaxExpiry.add(Calendar.MONTH, 1);

        Calendar oneCalendarMonthsTime = Calendar.getInstance();
        oneCalendarMonthsTime.add(Calendar.MONTH, 1);

        Date toUse; // The date we must expire this bundle on is the greater of now + 1 month and the max expiry + 1 month
        if (oneCalendarMonthsTime.after(oneCalendarMonthAfterMaxExpiry)) {
            toUse = oneCalendarMonthsTime.getTime();
        } else {
            toUse = oneCalendarMonthAfterMaxExpiry.getTime();
        }

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

        Date revenueFirstDate = null;
        Date revenueLastDate = null;
        Double revenuePerDay = null;
        
        if (getBooleanPropertyFromConfig("NoRevenueOnUsage") && getBooleanPropertyFromConfig("RecRevDaily")) {
            revenueFirstDate = new Date();
            revenueLastDate = toUse;
            long seconds = Utils.getSecondsBetweenDates(revenueFirstDate, revenueLastDate);
            revenuePerDay = normalRevenueRate * units / (seconds / 86400);
            if (log.isDebugEnabled()) {
                log.debug("Revenue must be recognised daily starting on [{}] and ending on [{}] which is [{}] seconds at [{}] per day totalling [{}]",
                        revenueFirstDate, revenueLastDate, seconds, revenuePerDay, revenuePerDay * seconds / 86400);
            }
            normalRevenueRate = 0;
            baselineRate = 0;
        }
        
        
        // Free rate is for revenue recognition of free bundles. Finance need to know this cause VAT is charged even for free stuff
        //double freeCents = ucs.getFreeCents();
        //double freeRate = freeCents / units; - PCB (as agreed with Heleen 0n 26/6/2013): dont send extra as free revenue as X3 would not balance unless the free part is invoiced. This is tiered pricing to need not be invoiced
        uci = DAO.createUnitCreditInstance(em, ucs.getUnitCreditSpecificationId(), acc, toUse, units, normalRevenueRate, baselineRate,
                productInstanceId, posCentsPaidEach, posCentsDiscountEach, null, info, extTxid, saleLineId, toUse, revenueFirstDate, revenueLastDate, revenuePerDay);

        doPostProvisionProcessing(verifyOnly);
    }

    /**
     * Ensure the service instance has the correct service spec for the time
     *
     * @param accId
     */
    private void ensureDayNightCapsAreInPlace(ServiceInstance serviceInstance) {

        int hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        long speedBPS;
        int nightStart = getIntPropertyFromConfig("NighttimeStartHour");
        int nightEnd = getIntPropertyFromConfig("NighttimeEndHour");
        if (hourOfDay >= nightStart || hourOfDay < nightEnd) {
            speedBPS = getLongPropertyFromConfig("BitsPerSecAtNight");
            log.debug("Speed should be [{}] as the time is between 7PM and 7AM", speedBPS);
        } else {
            speedBPS = getLongPropertyFromConfig("BitsPerSecAtDay");
            log.debug("Speed should be [{}] as the time is between 7AM and 7PM", speedBPS);
        }
        ServiceRules rules = new ServiceRules();
        rules.setMaxBpsDown(speedBPS);
        rules.setMaxBpsUp(speedBPS);
        ServiceSpecHelper.applyServiceRules(serviceInstance, rules);
    }

    @Override
    public void doPostExpiryProcessing() {
        // May be called more than once
        super.doPostExpiryProcessing();
        try {
            ServiceInstance serviceInstance = DAO.getDataServiceInstanceForProductInstance(em, uci.getProductInstanceId());
            ServiceRules rules = new ServiceRules();
            rules.setMaxBpsDown(-1L);
            rules.setMaxBpsUp(-1L);
            ServiceSpecHelper.applyServiceRules(serviceInstance, rules);

        } catch (Exception e) {
            new ExceptionManager(this).reportError(e);
        }
    }
}
