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
public class CorporateAccessUnitCredit extends BaseUnitCredit {

    private static final Logger log = LoggerFactory.getLogger(CorporateAccessUnitCredit.class);

    @Override
    public void provision(IAccount acc, int productInstanceId, Date startDate, boolean verifyOnly,
            String extTxid, double posCentsPaidEach, double posCentsDiscountEach, int saleLineId, String info) throws Exception {

        checkProvisionRules(acc, startDate, verifyOnly, productInstanceId);

        Date maxDate = DAO.getMaxExpiryDateByUnitCreditWrapperClass(em, acc.getAccountId(), this.getClass().getSimpleName());
        if (startDate == null) {
            startDate = new Date();
        }
        if (maxDate.before(startDate)) {
            log.debug("The last corporate access unit credit has expired already. This one will start now");
            maxDate = startDate;
        } else {
            log.debug("The last corporate access unit credit has yet to expire. Next one will start when current one expires");
        }
        log.debug("This corporate access unit credit will start at [{}]", maxDate);
        Calendar expiryDate = Calendar.getInstance();
        expiryDate.setTime(maxDate);
        if (getBooleanPropertyFromConfig("ValidityIsMonths")) {
            expiryDate.add(Calendar.MONTH, ucs.getValidityDays());
        } else if (getBooleanPropertyFromConfig("ValidityIsHours")) {
            expiryDate.add(Calendar.HOUR, ucs.getValidityDays());
        } else {
            expiryDate.add(Calendar.DATE, ucs.getValidityDays());
        }

        double normalRevenueRate;

        if (BaseUtils.getBooleanProperty("env.bm.uc.revenue.recognition.nett", true)) {
            normalRevenueRate = (posCentsPaidEach) / ucs.getUnits();
        } else {
            normalRevenueRate = (posCentsPaidEach + posCentsDiscountEach) / ucs.getUnits();
        }

        Date revenueFirstDate = null;
        Date revenueLastDate = null;
        Double revenuePerDay = null;

        if (getBooleanPropertyFromConfig("NoRevenueOnUsage")) {
            log.debug("This unit credit does not recognise revenue as its used");
            if (getBooleanPropertyFromConfig("RecRevDaily")) {
                revenueFirstDate = maxDate;
                revenueLastDate = expiryDate.getTime();
                long seconds = Utils.getSecondsBetweenDates(revenueFirstDate, revenueLastDate);
                revenuePerDay = normalRevenueRate * ucs.getUnits() / (seconds / 86400);
                if (log.isDebugEnabled()) {
                    log.debug("Revenue must be recognised daily starting on [{}] and ending on [{}] which is [{}] seconds at [{}] per day totalling [{}]",
                            revenueFirstDate, revenueLastDate, seconds, revenuePerDay, revenuePerDay * seconds / 86400);
                }
            }
            normalRevenueRate = 0;
        }

        uci = DAO.createUnitCreditInstance(em, ucs.getUnitCreditSpecificationId(), acc, expiryDate.getTime(),
                ucs.getUnits(), // Units
                normalRevenueRate, // Revenue per unit
                0, // baseline rev per unit
                productInstanceId, // Product inst id
                posCentsPaidEach, // pos cents charges
                posCentsDiscountEach, maxDate, info, extTxid, saleLineId, expiryDate.getTime(), revenueFirstDate, revenueLastDate, revenuePerDay);

        doPostProvisionProcessing(verifyOnly);
    }

    @Override
    /**
     * We want Corporate UC to always act like its got units left
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
            com.smilecoms.bm.db.model.ServiceInstance serviceInstance,
            boolean isLastResort,
            boolean checkOnly,
            String unitType,
            String location) {

        UCReserveResult res = new UCReserveResult();
        res.setOOBUnitRate(null);

        BigDecimal availableUnits = getAvailableUnitsLeft();
        log.debug("Corporate Access Unit Credit Instance [{}] has [{}] units available (i.e. current units - reserved units)", uci.getUnitCreditInstanceId(), availableUnits);

        implementFUPIfNecessary(serviceInstance);

        // This bundle always acts like it has units left and says that it reserved all the units requested
        res.setStillHasUnitsLeftToReserve(true);
        res.setUnitsReserved(unitsToReserve);
        res.setRecommendedReservationSecs(reservationSecs);
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
        log.debug("Successfully reserved [{}] units on corporate access unit credit", unitsToReserve);
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
        // We always indicate that we have charged the amount requested to be charged. The revenue is zero when we dont actually have units left
        UCChargeResult res = new UCChargeResult();
        res.setUnitCreditInstanceId(getUnitCreditInstanceId());
        res.setPaidForUsage(DAO.isPositive(uci.getPOSCentsCharged()) && DAO.isPositive(uci.getUnitsRemaining()));
        res.setUnitsCharged(unitsToCharge);
        res.setOOBUnitRate(null);
        BigDecimal currentUnits = getCurrentUnitsLeft();
        setUnitCreditUnitsRemaining(currentUnits.subtract(unitsToCharge));

        BigDecimal positiveUnitsCharged;
        if (currentUnits.compareTo(unitsToCharge) > 0) {
            // Unit credit has more units available than required. Subtract the required units
            positiveUnitsCharged = unitsToCharge;
        } else // Unit credit has just enough or not enough
         if (DAO.isPositive(currentUnits)) {
                positiveUnitsCharged = currentUnits;
            } else {
                positiveUnitsCharged = BigDecimal.ZERO;
            }
        res.setRevenueCents(positiveUnitsCharged.multiply(uci.getRevenueCentsPerUnit())); // Revenue is the unit credit rate X unit credit units that could be covered by the UC
        res.setFreeRevenueCents(BigDecimal.ZERO);
        res.setUnitsRemaining(getCurrentUnitsLeft());
        if (log.isDebugEnabled()) {
            log.debug("Successfully charged [{}] units on corporate access unit credit", res.getUnitsCharged());
        }
        return res;
    }

    private void implementFUPIfNecessary(ServiceInstance serviceInstance) {
        ServiceRules rules = new ServiceRules();
        // For corporate access remove any non sticky DPI Rules
        rules.setClearAllButStickyDPIRules(true);
        rules.setMaxBpsDown(-1L);
        rules.setMaxBpsUp(-1L);
        rules.setQci(-1);
        BigDecimal fupBytes = getBigDecimalPropertyFromConfigZeroIfMissing("FUPBytes");
        if (DAO.isPositive(fupBytes)) {
            BigDecimal usedBytes = getCurrentUnitsLeft().negate();
            log.debug("This UC has a FUP of [{}] bytes and [{}] is used", fupBytes, usedBytes);
            if (usedBytes.compareTo(fupBytes) > 0) {
                long speedBPS = getLongPropertyFromConfig("FUPBitsPerSec");
                if (speedBPS > 0) {
                    log.debug("FUP Exceeded. Ensuring speed cannot exceed [{}]bps", speedBPS);
                    // Enforce speed and clear any DPI Rules
                    rules.setMaxBpsDown(speedBPS);
                    rules.setMaxBpsUp(speedBPS);
                }
            }
        }
        ServiceSpecHelper.applyServiceRules(serviceInstance, rules);
    }

    

    
}
