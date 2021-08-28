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
public class SimpleUnitCreditWithTieredSlowDown extends SimpleUnitCredit {

    private static final Logger log = LoggerFactory.getLogger(SimpleUnitCreditWithTieredSlowDown.class);
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
            log.debug("SimpleUnitCreditWithTieredSlowDown being called and not in last resort");
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

        // Recommend a short reservation time as user will likely be in a defcon state
        res.setRecommendedReservationSecs(BaseUtils.getIntProperty("global.ocs.nocredit.validity.secs", 120));
        long speedToApply = -1;
        log.debug("Units to reserve is [{}] while what was originally required for reservation was [{}]", unitsToReserve, originalUnitsToReserve);
        boolean somethingElseHasBeenAbleToReserve = true;
        Date wentNegative = uci.getCrossoverDate() == null ? new Date() : uci.getCrossoverDate();
        int hoursSinceGoingNegative = (int) (Utils.getSecondsBetweenDates(wentNegative, new Date()) / 3600);
        log.debug("hoursSinceGoingNegative [{}]", hoursSinceGoingNegative);
        if (unitsToReserve.compareTo(originalUnitsToReserve) == 0) {
            log.debug("This is a last resort reservation and nothing has been able to reserve any units yet");
            speedToApply = getSpeedToApply(hoursSinceGoingNegative);
            somethingElseHasBeenAbleToReserve = false;
        } else {
            log.debug("A unit credit has reserved something already so lets not do anything to this tiering yet");
        }

        if (speedToApply > 0) {
            if (!checkOnly) {
                ServiceRules rules = new ServiceRules();
                rules.setMaxBpsDown(speedToApply);
                rules.setMaxBpsUp(speedToApply);
                ServiceSpecHelper.applyServiceRules(serviceInstance, rules);
            }
        } else {
            log.debug("Speed to apply is -1. Service need not have its speed changed");
        }

        int hoursLeftBeforeBlock = getBlockTierStart() - hoursSinceGoingNegative;
        log.debug("Hours left before we block are [{}]", hoursLeftBeforeBlock);

        if (hoursLeftBeforeBlock > 0 && somethingElseHasBeenAbleToReserve) {
            log.debug("Unit credit has hours left before blocking but as another UC has done some reserving, we wont provide a reservation but will say we have units left");
            res.setStillHasUnitsLeftToReserve(true);
            res.setUnitsReserved(BigDecimal.ZERO);
            return res;
        } else if (hoursLeftBeforeBlock == 0) {
            log.debug("Unit credit has zero hours left before blocking. We cannot reserve anything and do not have units left");
            res.setStillHasUnitsLeftToReserve(false);
            res.setUnitsReserved(BigDecimal.ZERO);
            return res;
        }

        res.setUnitsReserved(unitsToReserve);
        res.setStillHasUnitsLeftToReserve(true);
        log.debug("We are reserving [{}] on this UC", unitsToReserve);

        if (DAO.isPositive(unitsToReserve)) {
            DAO.createOrUpdateReservation(
                    em,
                    acc.getAccountId(),
                    sessionId,
                    uci.getUnitCreditInstanceId(),
                    BigDecimal.ZERO,
                    eventTimetamp,
                    request,
                    reservationSecs, // We dont know if the actual reservation given back will be as short as we recommend so we should reserve for the full time just to be safe. No harm is a longer reservation
                    unitsToReserve,
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

        Date wentNegative = (uci.getCrossoverDate() == null ? new Date() : uci.getCrossoverDate());
        int hoursSinceGoingNegative = (int) (Utils.getSecondsBetweenDates(wentNegative, new Date()) / 3600);
        log.debug("hoursSinceGoingNegative [{}]", hoursSinceGoingNegative);
        int speedToApply = getSpeedToApply(hoursSinceGoingNegative);
        if (speedToApply == 0) {
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
        Date wentNegative = uci.getCrossoverDate() == null ? new Date() : uci.getCrossoverDate();
        int hoursSinceGoingNegative = (int) (Utils.getSecondsBetweenDates(wentNegative, new Date()) / 3600);
        log.debug("hoursSinceGoingNegative [{}]", hoursSinceGoingNegative);
        int speedToApply = getSpeedToApply(hoursSinceGoingNegative);
        if (speedToApply == 0) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean supportsLastResortProcessing() {
        return true;
    }

    private int getSpeedToApply(int hoursSinceGoingNegative) {

        int speedToApply = -1;
        int tier = 1;
        int tierStarts = getIntPropertyFromConfig("Tier" + tier + "Start");
        while (tierStarts >= 0) { //Non existing config returns -1
            if (tierStarts > hoursSinceGoingNegative) {
                // This is the next tier above the correct tier
                break;
            }
            tier++;
            tierStarts = getIntPropertyFromConfig("Tier" + tier + "Start");
        }

        int tierToUse = tier - 1;

        if (tierToUse > 0) {
            speedToApply = getIntPropertyFromConfig("Tier" + tierToUse + "Speed");
            log.debug("Speed to apply is [{}]bps", speedToApply);
        } else if (tierToUse == 0) {
            log.debug("There is no speed that needs to be applied even though the bundle has gone negative");
        }
        return speedToApply;
    }

    private int getBlockTierStart() {
        int tier = 1;
        int tierStarts = getIntPropertyFromConfig("Tier" + tier + "Start");
        while (tierStarts >= 0) { //Non existing config returns -1
            tier++;
            tierStarts = getIntPropertyFromConfig("Tier" + tier + "Start");
        }

        int maxTier = tier - 1;
        int blockTierStart;
        String maxTierRule = getPropertyFromConfig("Tier" + maxTier + "Speed");
        if (maxTierRule.equals(BLOCK)) {
            blockTierStart = getIntPropertyFromConfig("Tier" + maxTier + "Start");
        } else {
            blockTierStart = 1; // Default 1 hour
        }
        log.debug("Blocking starts after [{}] hour of going negative", blockTierStart);
        return blockTierStart;
    }

}
