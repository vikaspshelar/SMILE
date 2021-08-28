/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.unitcredits.filters;

import com.smilecoms.bm.charging.IAccount;
import com.smilecoms.bm.db.model.ServiceInstance;
import com.smilecoms.bm.db.model.UnitCreditInstance;
import com.smilecoms.bm.db.model.UnitCreditSpecification;
import com.smilecoms.bm.rating.RatingResult;
import com.smilecoms.bm.unitcredits.UnitCreditManager;
import com.smilecoms.bm.unitcredits.wrappers.IUnitCredit;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class AllowBasedOnServiceCodeAndRatingGroupFilterClass extends AllowBasedOnUserPreferenceFilterClass {

    private static final Logger log = LoggerFactory.getLogger(AllowBasedOnServiceCodeAndRatingGroupFilterClass.class);

    @Override
    public boolean isUCApplicable(
            EntityManager em,
            IAccount acc,
            String sessionId,
            ServiceInstance serviceInstance,
            RatingResult ratingResult,
            RatingKey ratingKey,
            String srcDevice,
            String description,
            Date eventTimestamp,
            UnitCreditSpecification ucs,
            UnitCreditInstance uci,
            String location) {

        boolean parentAllows = super.isUCApplicable(em, acc, sessionId, serviceInstance, ratingResult, ratingKey, srcDevice, description, eventTimestamp, ucs, uci, location);
        if (!parentAllows) {
            log.debug("My parent says this UC cannot be used");
            return false;
        }

        String wlSvcCodeRegex = UnitCreditManager.getPropertyFromConfig(ucs, "WhiteListServiceCodeRegex");
        String wlRatingGroupRegex = UnitCreditManager.getPropertyFromConfig(ucs, "WhiteListRatingGroupRegex");
        String blSvcCodeRegex = UnitCreditManager.getPropertyFromConfig(ucs, "BlackListServiceCodeRegex");
        String blRatingGroupRegex = UnitCreditManager.getPropertyFromConfig(ucs, "BlackListRatingGroupRegex");
        if (wlSvcCodeRegex != null && wlRatingGroupRegex != null && blSvcCodeRegex != null && blRatingGroupRegex != null) {
            if ((wlSvcCodeRegex.isEmpty() || Utils.matchesWithPatternCache(ratingKey.getServiceCode(), wlSvcCodeRegex))
                    && (wlRatingGroupRegex.isEmpty() || Utils.matchesWithPatternCache(ratingKey.getRatingGroup(), wlRatingGroupRegex))
                    && (blSvcCodeRegex.isEmpty() || !Utils.matchesWithPatternCache(ratingKey.getServiceCode(), blSvcCodeRegex))
                    && (blRatingGroupRegex.isEmpty() || !Utils.matchesWithPatternCache(ratingKey.getRatingGroup(), blRatingGroupRegex))) {
                return true;
            } else {
                return false;
            }
        } else {
            log.debug("No regex are setup so returning true");
            // Assume if this filter is used and ServiceCodeRegex or  RatingGroupRegex is not specified, then it can work on any service code (e.g. like .*)
            return true;
        }
    }

    @Override
    public boolean isUCApplicableInContext(EntityManager em,
            IAccount acc, String sessionId,
            ServiceInstance serviceInstance,
            RatingResult ratingResult,
            RatingKey ratingKey, String srcDevice,
            String description,
            Date eventTimestamp,
            UnitCreditSpecification ucs,
            IUnitCredit uci,
            List<IUnitCredit> unitCreditsInList,
            String location) {

        if (!super.isUCApplicableInContext(em, acc, sessionId, serviceInstance, ratingResult, ratingKey, srcDevice, description, eventTimestamp, ucs, uci, unitCreditsInList, location)) {
            log.debug("Parent says it must be filtered out");
            return false;
        }

        String blDestinationPrefixRegex = UnitCreditManager.getPropertyFromConfig(ucs, "BlackListDestinationPrefixRegex");
        if (blDestinationPrefixRegex != null) {
            if ((ratingKey.getTo() != null && !ratingKey.getTo().isEmpty()) && (!blDestinationPrefixRegex.isEmpty() && Utils.matchesWithPatternCache(ratingKey.getTo(), blDestinationPrefixRegex))) {
                log.debug("BlackListDestinationPrefixRegex says it must be filtered out for [{}]", ratingKey.getTo());
                return false;
            }
        }
        
        long unitsUsedThresholdToIgnoreOnMultiple = uci.getLongPropertyFromConfig("MultipleUnitFilterThreshold");
        if (unitsUsedThresholdToIgnoreOnMultiple <= 0) {
            return true;
        }

        BigDecimal thisOnesUnitsUsed = uci.getUnitsAtStart().subtract(uci.getCurrentUnitsLeft());
        if (thisOnesUnitsUsed.longValue() < unitsUsedThresholdToIgnoreOnMultiple) {
            log.debug("UC has not used more than the threshold for multiple inclusions");
            return true;
        }

        log.debug("UC has used more than the threshold for multiple inclusions. Checking if another exists that has not");

        boolean foundAnotherOneNotExceedingThreshold = false;

        for (IUnitCredit otherUCInList : unitCreditsInList) {
            if (otherUCInList.getUnitCreditInstanceId() == uci.getUnitCreditInstanceId()) {
                continue;
            }

            if (!otherUCInList.getUnitCreditSpecification().getWrapperClass().equals(ucs.getWrapperClass())
                    && !uci.getBooleanPropertyFromConfig("ApplyMultipleUnitFilterThresholdAcrossAllUnitTypes")) {
                continue;
            }

            BigDecimal otherUCInListUnitsUsed = otherUCInList.getUnitsAtStart().subtract(otherUCInList.getCurrentUnitsLeft());
            unitsUsedThresholdToIgnoreOnMultiple = otherUCInList.getLongPropertyFromConfig("MultipleUnitFilterThreshold");
            if (otherUCInListUnitsUsed.longValue() < unitsUsedThresholdToIgnoreOnMultiple) {
                log.debug("Unit credit [{}] usage has not exceeded usage threshold for inclusion when multiple exist", otherUCInList);
                foundAnotherOneNotExceedingThreshold = true;
                break;
            } else if (otherUCInList.getUnitCreditSpecification().getUnitCreditSpecificationId().intValue() == uci.getUnitCreditSpecification().getUnitCreditSpecificationId().intValue()
                    && otherUCInListUnitsUsed.longValue() >= unitsUsedThresholdToIgnoreOnMultiple
                    && otherUCInListUnitsUsed.longValue() < thisOnesUnitsUsed.longValue()) {
                log.debug("Unit credit [{}] usage has exceeded usage threshold but it is of the same spec and has less units used than [{}]", otherUCInList, uci);
                foundAnotherOneNotExceedingThreshold = true;
                break;
            } else if (uci.getBooleanPropertyFromConfig("ApplyMultipleUnitFilterThresholdAcrossAllUnitTypes")
                    && otherUCInList.getUnitType().equals(uci.getUnitType())
                    && otherUCInList.getCurrentUnitsLeft().longValue() > 0) {
                foundAnotherOneNotExceedingThreshold = true;
                break;
            }
        }

        return !foundAnotherOneNotExceedingThreshold;
    }
}
