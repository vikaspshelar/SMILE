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
import java.util.Date;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class SimpleUnitCreditWithTieredCutoff extends SimpleUnitCredit {

    private static final Logger log = LoggerFactory.getLogger(SimpleUnitCreditWithTieredCutoff.class);
    private static final String BLOCK = "block";

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

        UCReserveResult res;

        if (!isLastResort) {
            // For non last resort reservations, behave exactly like SimpleUnitCredit
            log.debug("SimpleUnitCreditWithTieredCutoff being called and not in last resort");
            res = super.reserve(ratingKey, unitsToReserve, originalUnitsToReserve, acc, sessionId, eventTimetamp, request, reservationSecs, ratingResult, serviceInstance, isLastResort, checkOnly, unitType, location);
            return res;
        }

        res = new UCReserveResult();
        res.setOOBUnitRate(null);

        // Business rule as agreed with Martin for voice. No access to SmileON MB for non data service
        if (!ratingKey.getServiceCode().contains("32251")) {
            log.debug("This is not data service usage so we cannot allow SmileON");
            res.setStillHasUnitsLeftToReserve(false);
            res.setUnitsReserved(BigDecimal.ZERO);
            return res;
        }

        String smileOnBlocked = Utils.getValueFromCRDelimitedAVPString(uci.getInfo(), "NoSmileOn");
        if (smileOnBlocked != null && smileOnBlocked.equals("true")) {
            log.debug("SmileON is turned off for this UC");
            res.setStillHasUnitsLeftToReserve(false);
            res.setUnitsReserved(BigDecimal.ZERO);
            return res;
        }

        if (!acc.supportsOperationType(IAccount.ACCOUNT_OPERATION_TYPE.SMILE_ON)) {
            log.debug("SmileON is turned off for this account");
            res.setStillHasUnitsLeftToReserve(false);
            res.setUnitsReserved(BigDecimal.ZERO);
            return res;
        }

        String split = Utils.getValueFromCRDelimitedAVPString(uci.getInfo(), "Split");
        if (split != null && split.equals("true")) {
            log.debug("This is a split bundle so cannot get SmileON");
            res.setStillHasUnitsLeftToReserve(false);
            res.setUnitsReserved(BigDecimal.ZERO);
            return res;
        }

        BigDecimal currentUnits = uci.getUnitsRemaining();
        if (DAO.isPositive(currentUnits)) {
            log.debug("We are in last resort processing yet have units. Just return saying we cant reserve as whatever we have is probably already reserved");
            res.setStillHasUnitsLeftToReserve(true);
            res.setUnitsReserved(BigDecimal.ZERO);
            return res;
        }

        // By now we must have zero or negative units
        // Recommend a short reservation time as user will likely be in a defcon state
        res.setRecommendedReservationSecs(BaseUtils.getIntProperty("global.ocs.nocredit.validity.secs", 120));
        String dpiRuleToApply = null;
        BigDecimal bytesInTheNegative = currentUnits.abs();
        log.debug("UC is [{}] bytes in the negative", bytesInTheNegative);
        log.debug("Units to reserve is [{}] while what was originally required for reservation was [{}]", unitsToReserve, originalUnitsToReserve);
        boolean somethingElseHasBeenAbleToReserve = true;
        if (unitsToReserve.compareTo(originalUnitsToReserve) == 0) {
            log.debug("This is a last resort reservation and nothing has been able to reserve any units yet. Lets check the amount of data used");
            dpiRuleToApply = getDPIRule(bytesInTheNegative);
            somethingElseHasBeenAbleToReserve = false;
        } else {
            log.debug("A unit credit has reserved something already so lets not do anything to this tiering yet");
        }

        if (dpiRuleToApply != null && dpiRuleToApply.equals(BLOCK)) {
            log.debug("Unit Credit is too far into the negative. Not allowing a reservation");
            res.setStillHasUnitsLeftToReserve(false);
            res.setUnitsReserved(BigDecimal.ZERO);
            return res;
        }

        if (dpiRuleToApply != null) {
            if (!checkOnly) {
                ServiceRules rules = new ServiceRules();
                rules.setClearAllButStickyDPIRules(true);
                rules.setSystemDefinedDpiRulesToAdd(new HashSet<String>());
                rules.getSystemDefinedDpiRulesToAdd().add(dpiRuleToApply);
                ServiceSpecHelper.applyServiceRules(serviceInstance, rules);
            }
        } else {
            log.debug("DPI Rule to apply is null. Service need not have its rule changed");
        }

        BigDecimal unitsReserved;
        BigDecimal unitsLeftBeforeBlock = getBlockTierStart().subtract(bytesInTheNegative);
        log.debug("Units left before we block are [{}]", unitsLeftBeforeBlock);

        if (!DAO.isZero(unitsLeftBeforeBlock) && somethingElseHasBeenAbleToReserve) {
            log.debug("Unit credit has units left to go negative but as another UC has done some reserving, we wont provide a reservation but will say we have units left");
            res.setStillHasUnitsLeftToReserve(true);
            res.setUnitsReserved(BigDecimal.ZERO);
            return res;
        } else if (DAO.isZero(unitsLeftBeforeBlock)) {
            log.debug("Unit credit has zero units left to go negative. We cannot reserve anything and do not have units left");
            res.setStillHasUnitsLeftToReserve(false);
            res.setUnitsReserved(BigDecimal.ZERO);
            return res;
        }

        BigDecimal unitsBeforeNextTier = getNextTierStart(bytesInTheNegative).subtract(bytesInTheNegative);

        if (unitsToReserve.compareTo(unitsBeforeNextTier) >= 0) {
            log.debug("We have equal or more requested units to reserve than available to get to the next tier");
            unitsReserved = unitsBeforeNextTier;
            if (unitsLeftBeforeBlock.compareTo(unitsBeforeNextTier) == 0) {
                log.debug("We are reserving up to the block tier so will say we have nothing left to reserve");
                res.setStillHasUnitsLeftToReserve(false);
            } else {
                log.debug("We are reserving less than the block tier so will say we have something left to reserve");
                res.setStillHasUnitsLeftToReserve(true);
            }
        } else {
            log.debug("We have less units to reserve than available to get to the next tier. Can reserve all that was asked for");
            unitsReserved = unitsToReserve;
            res.setStillHasUnitsLeftToReserve(true);
        }
        log.debug("We are reserving [{}] on this UC", unitsReserved);

        res.setUnitsReserved(unitsReserved);

        if (DAO.isPositive(unitsReserved)) {
            DAO.createOrUpdateReservation(
                    em,
                    acc.getAccountId(),
                    sessionId,
                    uci.getUnitCreditInstanceId(),
                    BigDecimal.ZERO,
                    eventTimetamp,
                    request,
                    reservationSecs, // We dont know if the actual reservation given back will be as short as we recommend so we should reserve for the full time just to be safe. No harm is a longer reservation
                    unitsReserved,
                    checkOnly);
            res.setReservationWasCreated();
        }

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
        if (!isLastResort) {
            // For non last resort charges, behave exactly like SimpleUnitCredit
            return super.charge(ratingKey, unitsToCharge, originalUnitsToCharge, acc, ratingResult, isLastResort, unitType, eventTimetamp);
        }

        UCChargeResult res = new UCChargeResult();
        res.setUnitCreditInstanceId(getUnitCreditInstanceId());
        res.setPaidForUsage(DAO.isPositive(uci.getPOSCentsCharged()) && DAO.isPositive(uci.getUnitsRemaining()));
        res.setFreeRevenueCents(BigDecimal.ZERO);
        res.setRevenueCents(BigDecimal.ZERO);
        res.setUnitsCharged(BigDecimal.ZERO);
        res.setOOBUnitRate(null);
        // Business rule as agreed with Martin for voice. No access to SmileON MB for non data service
        if (!ratingKey.getServiceCode().contains("32251")) {
            log.debug("This is not data service usage so we cannot allow SmileON");
            return res;
        }

        String smileOnBlocked = Utils.getValueFromCRDelimitedAVPString(uci.getInfo(), "NoSmileOn");
        if (smileOnBlocked != null && smileOnBlocked.equals("true")) {
            log.debug("SmileON is turned off for this UC");
            return res;
        }

        if (!acc.supportsOperationType(IAccount.ACCOUNT_OPERATION_TYPE.SMILE_ON)) {
            log.debug("SmileON is turned off for this account");
            return res;
        }

        String split = Utils.getValueFromCRDelimitedAVPString(uci.getInfo(), "Split");
        if (split != null && split.equals("true")) {
            log.debug("This is a split bundle so cannot get SmileON");
            return res;
        }

        BigDecimal currentUnits = uci.getUnitsRemaining();
        if (DAO.isPositive(currentUnits)) {
            log.debug("Should not be possible...we are in last resort processing yet have units. Just return saying we cant charge");
            return res;
        }

        BigDecimal bytesInTheNegative = currentUnits.abs();
        String dpiRule = getDPIRule(bytesInTheNegative);
        if (dpiRule != null && dpiRule.equals(BLOCK)) {
            // This UC can no longer accept charging
            log.debug("This UC can no longer accept charging as its far enough in the negative to be in block");
            return res;
        }

        log.debug("This is a last resort charge. We will say we charged everything being requested");
        res.setUnitsCharged(unitsToCharge);
        // Make UCI go negative to track over usage
        setUnitCreditUnitsRemaining(uci.getUnitsRemaining().subtract(unitsToCharge));
        res.setUnitsRemaining(getCurrentUnitsLeft());
        log.debug("Unit credit units remaining after tiered cut-off processing is now [{}]", uci.getUnitsRemaining());
        return res;
    }

    @Override
    public boolean canTakeACharge() {
        // Check if in SmileON
        BigDecimal balance = uci.getUnitsRemaining();
        if (DAO.isPositive(balance)) {
            return true;
        }
        String dpiRule = getDPIRule(balance.abs());
        if (dpiRule != null && dpiRule.equals(BLOCK)) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean supportsLastResortProcessing() {
        return true;
    }

    private String getDPIRule(BigDecimal bytesInTheNegative) {
        String dpiRuleToApply = null;
        int tier = 1;
        BigDecimal tierStarts = getBigDecimalPropertyFromConfig("Tier" + tier + "Start");
        while (tierStarts.compareTo(BigDecimal.ZERO) >= 0) { //Non existing config returns -1
            if (tierStarts.compareTo(bytesInTheNegative) > 0) {
                // This is the next tier above the correct tier
                break;
            }
            tier++;
            tierStarts = getBigDecimalPropertyFromConfig("Tier" + tier + "Start");
        }

        int tierToUse = tier - 1;

        if (tierToUse > 0) {
            dpiRuleToApply = getPropertyFromConfig("Tier" + tierToUse + "DPIRule");
            log.debug("DPI Rule to apply is [{}]", dpiRuleToApply);
        } else if (tierToUse == 0) {
            log.debug("There is no DPI rule that needs to be applied even though the bundle has gone negative");
        }
        return dpiRuleToApply;
    }

    private BigDecimal getBlockTierStart() {
        int tier = 1;
        BigDecimal tierStarts = getBigDecimalPropertyFromConfig("Tier" + tier + "Start");
        while (tierStarts.compareTo(BigDecimal.ZERO) >= 0) { //Non existing config returns -1
            tier++;
            tierStarts = getBigDecimalPropertyFromConfig("Tier" + tier + "Start");
        }

        int maxTier = tier - 1;
        BigDecimal blockTierStart;
        String maxTierRule = getPropertyFromConfig("Tier" + maxTier + "DPIRule");
        if (maxTierRule.equals(BLOCK)) {
            blockTierStart = getBigDecimalPropertyFromConfig("Tier" + maxTier + "Start");
        } else {
            blockTierStart = BigDecimal.valueOf(100000000l); // Default to 100MB in case its not set. Rather be safe than sorry
        }
        log.debug("Blocking starts once [{}] units are used", blockTierStart);
        return blockTierStart;
    }

    private BigDecimal getNextTierStart(BigDecimal bytesInTheNegative) {
        int tier = 1;
        BigDecimal tierStartsBytes = getBigDecimalPropertyFromConfig("Tier" + tier + "Start");
        while (tierStartsBytes.compareTo(BigDecimal.ZERO) >= 0 && tierStartsBytes.compareTo(bytesInTheNegative) <= 0) { //Non existing config returns -1
            // tierStatsBytes exists and is less than or equal what the UC is in the negative
            tier++;
            tierStartsBytes = getBigDecimalPropertyFromConfig("Tier" + tier + "Start");
        }
        if (tierStartsBytes.compareTo(BigDecimal.ZERO) < 0) {
            tierStartsBytes = BigDecimal.ZERO;
        }
        log.debug("Next tier starts once [{}] bytes are used as we are currently [{}] bytes in the negative", tierStartsBytes, bytesInTheNegative);
        return tierStartsBytes;
    }
}
