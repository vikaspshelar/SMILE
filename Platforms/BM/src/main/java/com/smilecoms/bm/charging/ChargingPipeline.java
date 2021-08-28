/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.charging;

import com.smilecoms.bm.EventHelper;
import com.smilecoms.bm.db.model.AccountHistory;
import com.smilecoms.bm.db.model.Reservation;
import com.smilecoms.bm.db.model.ServiceInstance;
import com.smilecoms.bm.db.model.UnitCreditInstance;
import com.smilecoms.bm.db.model.UnitCreditSpecification;
import com.smilecoms.bm.db.op.DAO;
import com.smilecoms.bm.rating.RatingManager;
import com.smilecoms.bm.rating.RatingResult;
import com.smilecoms.bm.unitcredits.wrappers.IUnitCredit;
import com.smilecoms.bm.unitcredits.UCChargeResult;
import com.smilecoms.bm.unitcredits.UCReserveResult;
import com.smilecoms.bm.unitcredits.UnitCreditManager;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.platform.PlatformEventManager;
import com.smilecoms.commons.util.Javassist;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.bm.ChargingData;
import com.smilecoms.xml.schema.bm.GrantedServiceUnit;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class ChargingPipeline {

    private static final Logger log = LoggerFactory.getLogger(DAO.class);
    private static final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols();
    private static final SimpleDateFormat sdfLong = new SimpleDateFormat("HH:mm:ss");

    static {
        formatSymbols.setDecimalSeparator('.');
        formatSymbols.setGroupingSeparator(' ');
    }

    public static void sessionCharge(EntityManager em, String sessionId, boolean isRetrial, boolean justClearReservation) throws Exception {

        log.debug("Doing a session charge for session id [{}]", sessionId);
        List<Reservation> reservations = DAO.getSessionsReservationListStartingWith(em, sessionId);
        if (reservations.isEmpty()) {
            throw new Exception("Cannot determine what session this charge relates to -- Session Id " + sessionId);
        }
        Reservation res = reservations.get(0);
        IAccount acc = AccountFactory.getAccount(em, res.getReservationPK().getAccountId(), false);
        ServiceInstance serviceInstance;

        byte[] obj = res.getRequest();
        Object object = new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(obj)).readObject();
        ChargingData initialRequest = (ChargingData) object;
        sessionId = res.getReservationPK().getSessionId();
        log.debug("Using the original BM session Id from the request which was [{}]", sessionId);
        try {
            serviceInstance = DAO.getServiceInstanceForIdentifierAndServiceCode(em, initialRequest.getServiceInstanceIdentifier(), initialRequest.getRatingKey().getServiceCode());
        } catch (NoResultException nre) {
            // PCB 202 - If a SI is deactivated or deleted mid-session, then we would get here. We want to remove any reservations for this session so that the 
            // reservation timeout daemon does not pick up an expired reservation and log it as an issue
            log.debug("Session cannot continue as the service instance is deactivated or removed");
            DAO.deleteReservationsBySessionId(em, sessionId);
            throw nre;
        }

        if (log.isDebugEnabled()) {
            log.debug("Initial reservation amount [{}]", res.getAmountCents());
            log.debug("Initial reservation service identifier [{}]", initialRequest.getServiceInstanceIdentifier());
            log.debug("Initial reservation unit quantity [{}]", initialRequest.getRequestedServiceUnit().getUnitQuantity());
            log.debug("Initial reservation from [{}]", initialRequest.getRatingKey().getFrom());
            log.debug("Initial reservation to [{}]", initialRequest.getRatingKey().getTo());
            log.debug("Initial reservation user equipment [{}]", initialRequest.getUserEquipment());
            log.debug("Initial reservation service code [{}]", initialRequest.getRatingKey().getServiceCode());
            log.debug("Initial reservation description [{}]", initialRequest.getDescription());
        }

        if (justClearReservation) {
            log.debug("Units is zero. Just clearing out the reservation");
            DAO.deleteReservationsBySessionId(em, sessionId);
            return;
        }

        RatingResult ratingResult = RatingManager.getRate(em, initialRequest.getRatingKey(), serviceInstance, Utils.getJavaDate(initialRequest.getEventTimestamp()));

        sessionCharge(
                em,
                acc,
                sessionId,
                serviceInstance,
                ratingResult,
                initialRequest.getRequestedServiceUnit().getUnitQuantity(),
                initialRequest.getRatingKey(),
                initialRequest.getUserEquipment(),
                initialRequest.getDescription(),
                "",
                new Date(),
                "FI",
                initialRequest.getLocation(),
                initialRequest.getIPAddress(),
                initialRequest.getServiceInstanceIdentifier().getIdentifierType() + "=" + initialRequest.getServiceInstanceIdentifier().getIdentifier(),
                isRetrial,
                initialRequest.getRequestedServiceUnit().getUnitType());
    }

    public static ChargingPipelineResult sessionCharge(
            EntityManager em,
            IAccount acc,
            String sessionId,
            ServiceInstance serviceInstance,
            RatingResult ratingResult,
            BigDecimal usedUnits,
            RatingKey ratingKey,
            String srcDevice,
            String description,
            String termCode,
            Date eventTimetamp,
            String status,
            String location,
            String IPAddress,
            String serviceInstanceIdentifier,
            boolean isRetrial,
            String unitType) throws Exception {

        if (log.isDebugEnabled()) {
            log.debug("Doing a session charge for [{}]units at rate [{}]c/unit on account [{}] and for session id [{}], Retrial is [{}]", new Object[]{usedUnits, ratingResult.getRetailRateCentsPerUnit(), acc.getAccountId(), sessionId, isRetrial});
        }
        ChargingPipelineResult res = new ChargingPipelineResult();
        res.setAccount(acc);
        // Remove sessions reservations
        log.debug("Deleting any existing reservations for this charging session with session id [{}]", sessionId);
        DAO.deleteReservationsBySessionId(em, sessionId);

        // Store all UCI changes
        List<UCChargeResult> unitCreditChanges = new ArrayList<>();

        BigDecimal amntChargedCents, unitsChargedToUnitCredits, revenueCents, freeRevenueCents, baselineUnitsChargedToUnitCredits;
        boolean hasSocialMediaUnitCredit = false;
        boolean hasFreeOnnetUnitCredit = false;

        if (DAO.isPositive(usedUnits)
                && ratingKey.getLeg() != null
                && ratingKey.getLeg().equals("T")
                && ratingKey.getIncomingTrunk() != null
                && !ratingKey.getIncomingTrunk().equals("0")) {
            log.debug("This is a terminating call from offnet for non zero units so flag as revenue generating");
            res.setConsiderAsRevenueGenerating(true);
        }

        if (DAO.isZero(ratingResult.getRetailRateCentsPerUnit())
                && ratingKey.getIncomingTrunk() != null
                && ratingKey.getIncomingTrunk().equals("0")
                && ratingKey.getLeg() != null
                && ratingKey.getLeg().equals("T")) {
            // If its from non Smile trunk it could be roaming so we must still do all the normal processing hence the check for incoming trunk being 0
            log.debug("This is the free receiving leg of an onnet session. We charge nothing and shortcut the pipeline");
            amntChargedCents = BigDecimal.ZERO;
            unitsChargedToUnitCredits = BigDecimal.ZERO;
            baselineUnitsChargedToUnitCredits = BigDecimal.ZERO;
            revenueCents = BigDecimal.ZERO;
            freeRevenueCents = BigDecimal.ZERO;

        } else {

            // Get applicable unit credits
            List<IUnitCredit> unitCredits = UnitCreditManager.getApplicableUnitCredits(em,
                    acc,
                    sessionId,
                    serviceInstance,
                    ratingResult,
                    ratingKey,
                    srcDevice,
                    description,
                    eventTimetamp,
                    unitType,
                    location,
                    usedUnits);

            // Calculate the revenue of this session and keep it in this variable
            revenueCents = BigDecimal.ZERO;
            // Calculate the effectively free revenue of this session and keep it in this variable
            freeRevenueCents = BigDecimal.ZERO;
            // Now, for each unit credit that can be applied, lets remove as many units as we can
            BigDecimal remainingUnitsToCharge = usedUnits;
            baselineUnitsChargedToUnitCredits = BigDecimal.ZERO;

            // The OOB Rate must be the rate of the most recently purchased bundle (newest) that would have been used had it not run out (as agreed with Thibaud)   
            // Higher priority bundles take precedence on OOB rate. E.g. Nighttime OOB rate use instead of standard bundle rate at nightime even if standard bundle is newer
            int highestPriority = 0;
            BigDecimal highestPriorityBundlesUnitRate = null;
            for (IUnitCredit uc : unitCredits) {
                if (log.isDebugEnabled()) {
                    log.debug("Looking at UC for possible session charging: [{}]", uc);
                }

                if (uc.getUnitCreditSpecification().getWrapperClass().equals("SpecialLevyUnitCredit")) {
                    hasSocialMediaUnitCredit = true;
                }

                if (UnitCreditManager.getBooleanPropertyFromConfig(uc.getUnitCreditSpecification(), "FreeOnnetVoice")) {
                    hasFreeOnnetUnitCredit = true;
                }

                UCChargeResult chargeResult = uc.charge(ratingKey, remainingUnitsToCharge, usedUnits, acc, ratingResult, false, unitType, eventTimetamp);
                unitCreditChanges.add(chargeResult);
                if (chargeResult.getOOBUnitRate() != null && uc.getPriority() >= highestPriority && DAO.isPositive(chargeResult.getOOBUnitRate())) {
                    // Only override OOB rate if its not null. This gives the UC the ability to decide if it should change the OOB rate
                    // Dont let a lower priority rate override a higher priority
                    highestPriorityBundlesUnitRate = chargeResult.getOOBUnitRate();
                    highestPriority = uc.getPriority();
                }
                if (chargeResult.isPaidForUsage() && DAO.isPositive(chargeResult.getUnitsCharged())) {
                    res.setConsiderAsRevenueGenerating(true);
                }

                revenueCents = revenueCents.add(chargeResult.getRevenueCents());
                baselineUnitsChargedToUnitCredits = baselineUnitsChargedToUnitCredits.add(chargeResult.getBaselineUnitsCharged());
                freeRevenueCents = freeRevenueCents.add(chargeResult.getFreeRevenueCents());
                remainingUnitsToCharge = remainingUnitsToCharge.subtract(chargeResult.getUnitsCharged());
                if (DAO.isZero(remainingUnitsToCharge)) {
                    // We have charged all we need to so we can stop looping
                    break;
                }
            }

            if (highestPriorityBundlesUnitRate != null && DAO.isPositive(highestPriorityBundlesUnitRate) && BaseUtils.getBooleanProperty("env.bm.oob.rate.last.uc.rate", false)
                    && ratingResult.getRetailRateCentsPerUnit().compareTo(highestPriorityBundlesUnitRate) > 0) {
                // Only lower unit rate, dont increase it
                ratingResult.setRetailRateCentsPerUnit(highestPriorityBundlesUnitRate);
                log.debug("env.bm.oob.rate.last.uc.rate is true. The last UC rate based on the amount charged to the wallet for the units gained was [{}] which will be used for OOB charging", highestPriorityBundlesUnitRate);
            }

            unitsChargedToUnitCredits = usedUnits.subtract(remainingUnitsToCharge);

            BigDecimal amountToChargeToAccount;

            // Work arround to cater for social media charge since the retail rate is based on the number of bytes charged for each bundle
            // We need to work the retail rate here based on how much bytes we are going to charge on the account.
            if (BaseUtils.getBooleanProperty("env.bm.social.media.tax.logic.enabled", false)) {
                if (ratingKey.getServiceCode() != null && ((ratingKey.getServiceCode().equals("txtype.socialmedia.tax")
                        || ratingKey.getServiceCode().equals("txtype.socialmedia.tax.unlimited"))
                        && unitType.equals("OCTET"))) {
                    // Calcutale social media retail rate as agreed with UG
                    ratingResult.setRetailRateCentsPerUnit(DAO.divide(new BigDecimal(20000), usedUnits));
                }
            }

            amountToChargeToAccount = remainingUnitsToCharge.multiply(ratingResult.getRetailRateCentsPerUnit());

            //Check if user has a social media unit credit.
            /* if(BaseUtils.getBooleanProperty("env.bm.social.media.tax.logic.enabled", false)) {
                if(acc.supportsOperationType(IAccount.ACCOUNT_OPERATION_TYPE.MONETARY_CHARGE) && 
                        !hasSocialMediaUnitCredit) {
                    throw new Exception("Account does not have the required social media tax bundle.");
                }
            }*/
            // If rate is negative then nothing will be charged
            amntChargedCents = acc.chargeForUsage(amountToChargeToAccount, ratingKey.getRatingGroup());
            revenueCents = revenueCents.add(amntChargedCents);

            BigDecimal unitsStillNotChargedFor;
            if (!DAO.isZero(ratingResult.getRetailRateCentsPerUnit())) {
                BigDecimal unitsChargedToAccount;
                unitsChargedToAccount = DAO.divide(amntChargedCents, ratingResult.getRetailRateCentsPerUnit());
                unitsStillNotChargedFor = remainingUnitsToCharge.subtract(unitsChargedToAccount);
                log.debug("Units charged on the account is [{}] and units still to charge is [{}]", unitsChargedToAccount, unitsStillNotChargedFor);
            } else {
                unitsStillNotChargedFor = BigDecimal.ZERO;
            }

            if (DAO.isPositive(unitsStillNotChargedFor)) {
                log.debug("There are still [{}] units that need to be charged. Going to give the unit credits another go to try and charge this", unitsStillNotChargedFor);
                for (IUnitCredit uc : unitCredits) {
                    if (!uc.supportsLastResortProcessing()) {
                        continue;
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Looking at UC for possible session charging as a last resort: [{}]", uc);
                    }
                    UCChargeResult chargeResult = uc.charge(ratingKey, unitsStillNotChargedFor, usedUnits, acc, ratingResult, true, unitType, eventTimetamp);
                    unitCreditChanges.add(chargeResult);
                    revenueCents = revenueCents.add(chargeResult.getRevenueCents());
                    baselineUnitsChargedToUnitCredits = baselineUnitsChargedToUnitCredits.add(chargeResult.getBaselineUnitsCharged());
                    freeRevenueCents = freeRevenueCents.add(chargeResult.getFreeRevenueCents());
                    unitsStillNotChargedFor = unitsStillNotChargedFor.subtract(chargeResult.getUnitsCharged());
                    unitsChargedToUnitCredits = unitsChargedToUnitCredits.add(chargeResult.getUnitsCharged());
                    if (chargeResult.isPaidForUsage() && DAO.isPositive(chargeResult.getUnitsCharged())) {
                        res.setConsiderAsRevenueGenerating(true);
                    }
                    if (DAO.isZero(unitsStillNotChargedFor)) {
                        // We have charged all we need to so we can stop looping
                        break;
                    }
                }
            }
        } // end else not free receiving leg

        Date startDate;
        String timeBasedTxTypesRegex = BaseUtils.getProperty("env.bm.timebased.transaction.types.regex", "");
        if (!timeBasedTxTypesRegex.isEmpty() && Utils.matchesWithPatternCache(ratingKey.getServiceCode(), timeBasedTxTypesRegex)) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(eventTimetamp);
            cal.add(Calendar.SECOND, usedUnits.intValue() * -1);
            startDate = cal.getTime(); // For voice (and other time based services), we should make this the start date of the call as opposed to the date of the first charge. 
        } else {
            startDate = eventTimetamp;
        }

        if (ratingKey.getServiceCode().equals("txtype.sale.purchase")
                || ratingKey.getServiceCode().equals("txtype.socialmedia.tax")
                || ratingKey.getServiceCode().equals("txtype.socialmedia.tax.unlimited")) {
            log.debug("This is a charge for something that must not recognise revenue");
            revenueCents = BigDecimal.ZERO;
            freeRevenueCents = BigDecimal.ZERO;
            res.setConsiderAsRevenueGenerating(true);
        }

        termCode = getTermCode(termCode, ratingKey.getServiceCode(), ratingKey.getLeg(), ratingKey.getIncomingTrunk(), ratingKey.getOutgoingTrunk());
        String info = getInfo(ratingKey.getServiceCode(), location, IPAddress, termCode, ratingKey.getLeg(), ratingKey.getIncomingTrunk(), ratingKey.getOutgoingTrunk());

        res.setRevenueCents(revenueCents);

        AccountHistory ah = DAO.createOrUpdateAccountHistoryForSession(
                em,
                sessionId,
                ratingKey.getFrom(),
                ratingKey.getTo(),
                amntChargedCents,
                acc,
                startDate, // Only used when row is first created
                eventTimetamp,
                makeDescription(description, ratingResult),
                usedUnits,
                srcDevice,
                termCode,
                ratingKey.getServiceCode(),
                status,
                serviceInstance.getServiceInstanceId(),
                unitsChargedToUnitCredits,
                revenueCents,
                revenueCents.negate(),
                freeRevenueCents,
                location,
                IPAddress,
                serviceInstanceIdentifier,
                isRetrial,
                ratingKey.getIncomingTrunk(),
                ratingKey.getOutgoingTrunk(),
                ratingKey.getLeg(),
                baselineUnitsChargedToUnitCredits,
                ratingKey.getRatingGroup(),
                info,
                unitCreditChanges);

        String eventOnFinishedTxTypesRegex = BaseUtils.getProperty("env.bm.event.on.finished.types.regex", "");

        boolean skip = false;
        try {
            skip = BaseUtils.getPropertyAsSet("env.bm.postcall.event.skip.accounts").contains(String.valueOf(acc.getAccountId()));
        } catch (Exception e) {
            log.debug("env.bm.postcall.event.skip.accounts does not exist");
        }

        log.debug("AlertValues:\nStatus = {}\neventOnFinishedTxTypesRegex.isEmpty()={}\nUtils.matchesWithPatternCache(ratingKey.getServiceCode(), eventOnFinishedTxTypesRegex)={}\nDAO.isPositive(ah.getTotalUnits()={}\nSkip={}", status, eventOnFinishedTxTypesRegex.isEmpty(), Utils.matchesWithPatternCache(ratingKey.getServiceCode(), eventOnFinishedTxTypesRegex), DAO.isPositive(ah.getTotalUnits()), skip);

        if (status.equalsIgnoreCase("FI")
                && !eventOnFinishedTxTypesRegex.isEmpty()
                && Utils.matchesWithPatternCache(ratingKey.getServiceCode(), eventOnFinishedTxTypesRegex)
                && DAO.isPositive(ah.getTotalUnits())
                && !skip) {

            log.debug("Inside the FI, we should alert");

            double bytesCharged = ah.getUnitCreditBaselineUnits().abs().doubleValue();
            String baselineUnitsChargedToUnitCreditsStr = getBytesAsString(bytesCharged);
            int durationMin = ah.getTotalUnits().intValue() / 60;
            int durationSec = ah.getTotalUnits().intValue() % 60;
            int ucDurationMin = ah.getUnitCreditUnits().intValue() * -1 / 60;
            int ucDurationSec = ah.getUnitCreditUnits().intValue() * -1 % 60;
            DecimalFormat CurrencyShort = new DecimalFormat(BaseUtils.getProperty("env.locale.currency.shortformat"), formatSymbols);
            DecimalFormat curShort = new DecimalFormat(BaseUtils.getProperty("env.locale.currency.majorunit") + BaseUtils.getProperty("env.bm.charging.pipeline.free.onnet.received.value.format", " #,##0"), formatSymbols);
            String accountCost = CurrencyShort.format(ah.getAccountCents().longValue() * -1 / 100.0d);
            String accountBal = CurrencyShort.format(acc.getAvailableBalanceCents().longValue() / 100.0d);
            String revenue = CurrencyShort.format(ah.getRevenueCents().longValue() / 100.0d);
            String onnetReceivedValue = curShort.format((ah.getTotalUnits().intValue() * BaseUtils.getIntProperty("env.bm.charging.pipeline.free.onnet.cents.persec", 9)) / 100.0d);
            double centsPerSec = DAO.divide(ah.getRevenueCents(), ah.getTotalUnits()).setScale(1, RoundingMode.HALF_EVEN).doubleValue();
            double bytesPerMin = DAO.divide(ah.getUnitCreditBaselineUnits().abs(), ah.getTotalUnits()).multiply(new BigDecimal(60)).doubleValue();
            String mbPerMinute = Utils.round(bytesPerMin / 1048576, 1) + "MB/min"; // Is actually MiB
            double dataBytesOnAccount = 0;
            try {
                dataBytesOnAccount = DAO.getAccountsBytesRemaining(em, acc.getAccountId());
            } catch (Exception e) {
                log.warn("Error getting accounts data balance:", e);
            }
            String dataBalance;
            if (dataBytesOnAccount == Double.MAX_VALUE) {
                dataBalance = "Unlimited";
            } else {
                dataBalance = getBytesAsString(dataBytesOnAccount);
            }

            if (!BaseUtils.getBooleanProperty("env.bm.charging.pipeline.onnet.reporting.enabled", false)) {
                log.debug("Inside:: BaseUtils.getBooleanProperty(\"env.bm.charging.pipeline.onnet.reporting.enabled\", false) with {}", BaseUtils.getBooleanProperty("env.bm.charging.pipeline.onnet.reporting.enabled", false));

                PlatformEventManager.createEventAsync("BM", "FI-" + ratingKey.getServiceCode() + "_" + ratingKey.getLeg(), String.valueOf(acc.getAccountId()),
                        String.valueOf(acc.getAccountId()) + "|" + ratingKey.getServiceCode() + "|" + durationMin + "|" + durationSec + "|" + serviceInstanceIdentifier.split("=")[1] + "|" + accountBal + "|"
                        + accountCost + "|" + usedUnits + "|" + Utils.getFriendlyPhoneNumber(ratingKey.getFrom()) + "|" + Utils.getFriendlyPhoneNumber(ratingKey.getTo())
                        + "|" + serviceInstance.getServiceInstanceId() + "|" + ucDurationMin + "|" + ucDurationSec + "|" + sdfLong.format(startDate) + "|" + description + "|" + baselineUnitsChargedToUnitCreditsStr
                        + "|" + location + "|" + revenue + "|" + centsPerSec + "|" + mbPerMinute + "|" + dataBalance + "|" + info + "|" + serviceInstance.getProductSpecificationId() + "|" + ah.getId(),
                        null, eventTimetamp);
            } else {
                log.debug("Inside ELSE :: ");
                String callDirection = "DATA_SESSION";
                if (ratingKey.getOutgoingTrunk() != null && ratingKey.getIncomingTrunk() != null) {
                    callDirection = (ratingKey.getIncomingTrunk().equals("0") && (ratingKey.getOutgoingTrunk().equals("0") || ratingKey.getOutgoingTrunk().equals("1000"))) ? "ONNET_CALL" : "OFFNET_CALL";
                }

                switch (callDirection) {
                    case "ONNET_CALL":
                        callDirection = bytesCharged == 0 ? "ONNET_CALL_VOICE_BUNDLE" : "ONNET_CALL_DATA";
                        if (hasFreeOnnetUnitCredit) {
                            callDirection = "ONNET_CALL_FREE_VOICE_BUNDLE";
                        }
                        callDirection = ratingKey.getOutgoingTrunk().equals("1000") ? "ONNET_CALL_CALL_CENTER" : callDirection;
                        break;
                    case "OFFNET_CALL":
                        callDirection = bytesCharged == 0 ? "OFFNET_CALL_VOICE_BUNDLE" : "OFFNET_CALL_DATA";
                        break;
                    default:
                        callDirection = "DATA_SESSION";
                }

                int dataSecondsOnAccount = 0;
                if (callDirection.equals("ONNET_CALL_VOICE_BUNDLE") || callDirection.equals("OFFNET_CALL_VOICE_BUNDLE")) {
                    try {
                        if (BaseUtils.getBooleanProperty("env.bm.charging.pipeline.voice.bundle.seperation.enabled", false)) {
                            List<UnitCreditInstance> unitCreditInstanceList = DAO.getUnitCreditInstanceList(em, ah.getId());
                            if (unitCreditInstanceList != null) {
                                UnitCreditSpecification unitCreditSpecification = DAO.getUnitCreditSpecification(em, unitCreditInstanceList.get(0).getUnitCreditSpecificationId());
                                if (unitCreditSpecification != null) {
                                    if (unitCreditSpecification.getUnitCreditName().startsWith("International")) {
                                        callDirection = "CALL_INTERNATIONAL_VOICE_BUNDLE";
                                        dataSecondsOnAccount = DAO.getAccountsInternationalSecondsRemaining(em, acc.getAccountId());
                                    } else {
                                        dataSecondsOnAccount = DAO.getAccountsLocalSecondsRemaining(em, acc.getAccountId());
                                    }
                                }
                            } else {
                                dataSecondsOnAccount = DAO.getAccountsSecondsRemaining(em, acc.getAccountId());
                            }
                        } else {
                            dataSecondsOnAccount = DAO.getAccountsSecondsRemaining(em, acc.getAccountId());
                        }
                    } catch (Exception e) {
                        log.warn("Error getting accounts voice seconds balance:", e);
                    }
                }

                log.debug("Inside ELSE :: ");
                PlatformEventManager.createEventAsync("BM",
                        "FI-" + ratingKey.getServiceCode() + "_" + ratingKey.getLeg(),
                        String.valueOf(acc.getAccountId()),
                        String.valueOf(acc.getAccountId()) + "|" + ratingKey.getServiceCode() + "|" + durationMin + "|" + durationSec + "|" + serviceInstanceIdentifier.split("=")[1] + "|" + accountBal + "|"
                        + accountCost + "|" + usedUnits + "|" + Utils.getFriendlyPhoneNumber(ratingKey.getFrom()) + "|" + Utils.getFriendlyPhoneNumber(ratingKey.getTo())
                        + "|" + serviceInstance.getServiceInstanceId() + "|" + ucDurationMin + "|" + ucDurationSec + "|" + sdfLong.format(startDate) + "|" + description + "|" + baselineUnitsChargedToUnitCreditsStr
                        + "|" + location + "|" + revenue + "|" + centsPerSec + "|" + mbPerMinute + "|" + dataBalance + "|" + info + "|" + serviceInstance.getProductSpecificationId()
                        + "|" + ah.getId() + "|" + onnetReceivedValue + "|" + dataSecondsOnAccount + "|" + callDirection,
                        null,
                        eventTimetamp);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Finished a session charge for [{}]c on account [{}] and [{}] units of unit credits and [{}] unit credit baseline units for session id [{}]", new Object[]{amntChargedCents, acc.getAccountId(), unitsChargedToUnitCredits, baselineUnitsChargedToUnitCredits, sessionId});
        }
        return res;
    }

    public static ChargingPipelineResult eventCharge(
            EntityManager em,
            IAccount acc,
            String sessionId,
            ServiceInstance serviceInstance,
            RatingResult ratingResult,
            BigDecimal usedUnits,
            RatingKey ratingKey,
            String srcDevice,
            String description,
            Date eventTimetamp,
            String status,
            String location,
            String IPAddress,
            String serviceInstanceIdentifier,
            boolean isRetrial,
            String unitType) throws Exception {

        if (log.isDebugEnabled()) {
            log.debug("Doing an event charge for [{}]units at rate [{}] on account [{}] and for session id [{}]. IsRetrial is [{}]", new Object[]{usedUnits, ratingResult.getRetailRateCentsPerUnit(), acc.getAccountId(), sessionId, isRetrial});
        }

        ChargingPipelineResult res = new ChargingPipelineResult();
        res.setAccount(acc);
        BigDecimal amntChargedCents, unitsChargedToUnitCredits, revenueCents, freeRevenueCents, baselineUnitsChargedToUnitCredits;
        // Store all UCI changes
        List<UCChargeResult> unitCreditChanges = new ArrayList<>();

        if (DAO.isPositive(usedUnits)
                && ratingKey.getLeg() != null
                && ratingKey.getLeg().equals("T")
                && ratingKey.getIncomingTrunk() != null
                && !ratingKey.getIncomingTrunk().equals("0")) {
            log.debug("This is a terminating event from offnet for non zero units so flag as revenue generating");
            res.setConsiderAsRevenueGenerating(true);
        }

        boolean isOnnetReceivingLeg = false;

        if (DAO.isZero(ratingResult.getRetailRateCentsPerUnit())
                && ratingKey.getIncomingTrunk() != null
                && ratingKey.getIncomingTrunk().equals("0")
                && ratingKey.getLeg() != null
                && ratingKey.getLeg().equals("T")) {

            // If its from non Smile trunk it could be roaming so we must still do all the normal processing hence the check for incoming trunk being 0
            log.debug("This is the free receiving leg of an onnet session.");
            amntChargedCents = BigDecimal.ZERO;
            unitsChargedToUnitCredits = BigDecimal.ZERO;
            revenueCents = BigDecimal.ZERO;
            freeRevenueCents = BigDecimal.ZERO;
            baselineUnitsChargedToUnitCredits = BigDecimal.ZERO;
            isOnnetReceivingLeg = true;
        } else {

            // Get applicable unit credits
            List<IUnitCredit> unitCredits = UnitCreditManager.getApplicableUnitCredits(
                    em,
                    acc,
                    sessionId,
                    serviceInstance,
                    ratingResult,
                    ratingKey,
                    srcDevice,
                    description,
                    eventTimetamp,
                    unitType,
                    location,
                    usedUnits);

            // Calculate the revenue of this session chargeForUsage and keep it in this variable
            revenueCents = BigDecimal.ZERO;
            // Calculate the effectively free revenue of this session and keep it in this variable
            freeRevenueCents = BigDecimal.ZERO;
            baselineUnitsChargedToUnitCredits = BigDecimal.ZERO;
            // Now, for each unit credit that can be applied, lets remove as many units as we can
            BigDecimal remainingUnitsToCharge = usedUnits;

            for (IUnitCredit uc : unitCredits) {
                if (log.isDebugEnabled()) {
                    log.debug("Looking at UC for possible event charging: [{}]", uc);
                }
                UCChargeResult chargeResult = uc.charge(ratingKey, remainingUnitsToCharge, usedUnits, acc, ratingResult, false, unitType, eventTimetamp);
                unitCreditChanges.add(chargeResult);
                baselineUnitsChargedToUnitCredits = baselineUnitsChargedToUnitCredits.add(chargeResult.getBaselineUnitsCharged());
                revenueCents = revenueCents.add(chargeResult.getRevenueCents());
                freeRevenueCents = freeRevenueCents.add(chargeResult.getFreeRevenueCents());
                remainingUnitsToCharge = remainingUnitsToCharge.subtract(chargeResult.getUnitsCharged());
                if (DAO.isZero(remainingUnitsToCharge)) {
                    // We have charged all we need to so we can stop looping
                    break;
                }
            }

            unitsChargedToUnitCredits = usedUnits.subtract(remainingUnitsToCharge);

            BigDecimal amountToChargeToAccount;

            amountToChargeToAccount = remainingUnitsToCharge.multiply(ratingResult.getRetailRateCentsPerUnit());

            amntChargedCents = acc.chargeForUsage(amountToChargeToAccount, ratingKey.getRatingGroup());
            revenueCents = revenueCents.add(amntChargedCents);

            revenueCents = revenueCents.add(amntChargedCents);

            if (sessionId != null && !sessionId.isEmpty()) {
                DAO.deleteReservationsBySessionId(em, sessionId);
            }

        }

        if (ratingKey.getServiceCode().equals("txtype.sale.purchase")) {
            log.debug("This is a charge for something that must not recognise revenue");
            revenueCents = BigDecimal.ZERO;
            freeRevenueCents = BigDecimal.ZERO;
        }

        res.setRevenueCents(revenueCents);
        AccountHistory ah = DAO.createAccountHistoryForEvent(
                em,
                sessionId,
                ratingKey.getFrom(),
                ratingKey.getTo(),
                amntChargedCents,
                acc,
                eventTimetamp,
                makeDescription(description, ratingResult),
                usedUnits,
                srcDevice,
                ratingKey.getServiceCode(),
                status,
                serviceInstance.getServiceInstanceId(),
                unitsChargedToUnitCredits,
                revenueCents,
                revenueCents.negate(),
                freeRevenueCents,
                location,
                IPAddress,
                serviceInstanceIdentifier,
                ratingKey.getIncomingTrunk(),
                ratingKey.getOutgoingTrunk(),
                ratingKey.getLeg(),
                baselineUnitsChargedToUnitCredits,
                ratingKey.getRatingGroup(),
                null,
                unitCreditChanges);

        if (!isOnnetReceivingLeg && DAO.isPositive(usedUnits)) {
            String baselineUnitsChargedToUnitCreditsStr = getBytesAsString(baselineUnitsChargedToUnitCredits.abs().doubleValue());
            PlatformEventManager.createEventAsync("BM", "FI-" + ratingKey.getServiceCode(), String.valueOf(acc.getAccountId()),
                    String.valueOf(acc.getAccountId()) + "|" + ratingKey.getServiceCode() + "|0|" + serviceInstanceIdentifier.split("=")[1] + "|" + acc.getCurrentBalanceCents() + "|"
                    + amntChargedCents + "|" + usedUnits + "|" + Utils.getFriendlyPhoneNumber(ratingKey.getFrom()) + "|" + Utils.getFriendlyPhoneNumber(ratingKey.getTo())
                    + "|" + serviceInstance.getServiceInstanceId() + "|" + unitsChargedToUnitCredits + "|" + sdfLong.format(eventTimetamp) + "|" + description + "|" + baselineUnitsChargedToUnitCreditsStr
                    + "|" + location,
                    null, eventTimetamp);
        }

        if (log.isDebugEnabled()) {
            log.debug("Finished an event charge for [{}]c on account [{}] and for session id [{}]", new Object[]{amntChargedCents, acc.getAccountId(), sessionId});
        }
        return res;
    }

    public static GrantedServiceUnit reserve(
            EntityManager em,
            IAccount acc,
            String sessionId,
            ServiceInstance serviceInstance,
            RatingResult ratingResult,
            BigDecimal units,
            RatingKey ratingKey,
            String srcDevice,
            String description,
            Date eventTimestamp,
            int requestedReservationSecs,
            byte[] request,
            boolean isRetrial,
            boolean checkOnly,
            String unitType,
            String location) throws Exception {

        if (log.isDebugEnabled()) {
            log.debug("Reserve funds request: Account [{}] Units [{}] Rate [{}]c/unit SessionId [{}]. IsRetrial is [{}] Location [{}]", new Object[]{acc.getAccountId(), units, ratingResult.getRetailRateCentsPerUnit(), sessionId, isRetrial, location});
        }

        Set<String> noReservationSvcInstanceIds = null;
        try {
            noReservationSvcInstanceIds = BaseUtils.getPropertyAsSet("env.bm.noreservation.svc.instance.ids");
        } catch (Exception e) {
        }

        if (noReservationSvcInstanceIds != null && noReservationSvcInstanceIds.contains(String.valueOf(serviceInstance.getServiceInstanceId()))) {
            throw new Exception("Insufficient available balance nor unit credits");
        }

        if (DAO.isZero(ratingResult.getRetailRateCentsPerUnit())
                && ratingKey.getIncomingTrunk() != null
                && ratingKey.getIncomingTrunk().equals("0")
                && ratingKey.getLeg() != null
                && ratingKey.getLeg().equals("T")) {

            log.debug("This is the free receiving leg of an onnet session. Grant everything and more");
            GrantedServiceUnit gsu = new GrantedServiceUnit();
            gsu.setValidityTime((int) ((BaseUtils.getDoubleProperty("env.bm.zerorate.reservation.secs.multiplier", 10.0d) * requestedReservationSecs)));
            if (gsu.getValidityTime() == 0) {
                gsu.setValidityTime(1);
            }
            gsu.setRetailCentsPerUnit(ratingResult.getRetailRateCentsPerUnit());
            gsu.setFromInterconnectCentsPerUnit(ratingResult.getFromInterconnectRateCentsPerUnit());
            gsu.setToInterconnectCentsPerUnit(ratingResult.getToInterconnectRateCentsPerUnit());
            gsu.setUnitQuantity(units.multiply(BaseUtils.getBigDecimalProperty("env.bm.zerorate.reservation.units.multiplier", new BigDecimal(10))));
            return gsu;
        }

        /*
         * Basic process is as follows:
         * 1) Try and reserve against unit credits for as much as we can
         * 2) Whatever was not reserved against unit credits should be reserved against the monetary balance
         * 3) If we couldnt reserve anything on UC's or balance, then throw an insufficient funds error
         * 4) If after doing the reservations, there are no unit credit nor money left to reserve, then set final unit flag
         * 5) Only return what we could actually reserve
         */
        boolean stillUnitCreditsLeft = false;
        boolean stillMoneyLeft = false;
        boolean wasAnyReservationDone = false;
        boolean hasSocialMediaUnitCredit = false;

        int recommendedReservationSecs = requestedReservationSecs;

        /*
         * Step 1 - Try and reserve against unit credits for as much as we can
         */
        GrantedServiceUnit gsu = new GrantedServiceUnit();

        List<IUnitCredit> unitCredits = UnitCreditManager.getApplicableUnitCredits(em,
                acc,
                sessionId,
                serviceInstance,
                ratingResult,
                ratingKey,
                srcDevice,
                description,
                eventTimestamp,
                unitType,
                location,
                units);

        // Dont give them everything they want if the user is running low
        units = getUnitsToReserveBasedOnContext(acc, unitCredits, ratingKey, ratingResult, units);

        // Get applicable unit credits
        BigDecimal remainingUnitsToReserve = units;

        BigDecimal highestPriorityBundlesUnitRate = null;
        int highestPriority = 0;

        String firstFailureHint = "";

        for (IUnitCredit uc : unitCredits) {
            if (log.isDebugEnabled()) {
                log.debug("Looking at UC for possible reservation: [{}]", uc);
            }

            if (uc.getUnitCreditSpecification().getWrapperClass().equals("SpecialLevyUnitCredit")) {
                hasSocialMediaUnitCredit = true;
            }

            if (uc.hasAvailableUnitsLeft() && DAO.isZero(remainingUnitsToReserve)) {
                log.debug("We have reserved all we need to, and there are still UCs left. Breaking from UC loop point 1");
                stillUnitCreditsLeft = true;
                break;
            }
            // If we get here, then we still have reservations to do or the last UC did not have units left
            UCReserveResult reserveResult = uc.reserve(ratingKey, remainingUnitsToReserve, units, acc, sessionId, eventTimestamp,
                    request, requestedReservationSecs, ratingResult, serviceInstance, false, checkOnly, unitType, location);

            if (reserveResult.getFailureHint() != null && !reserveResult.getFailureHint().isEmpty() && firstFailureHint.isEmpty()) {
                firstFailureHint = reserveResult.getFailureHint();
                log.debug("First failure hint returned: [{}]", firstFailureHint);
            }

            if (!wasAnyReservationDone) {
                wasAnyReservationDone = reserveResult.wasReservationCreated();
            }
            if (reserveResult.getOOBUnitRate() != null && uc.getPriority() >= highestPriority && DAO.isPositive(reserveResult.getOOBUnitRate())) {
                // Only override OOB rate if its not null. This gives the UC the ability to decide if it should change the OOB rate
                // Dont let a lower priority rate override a higher priority
                highestPriorityBundlesUnitRate = reserveResult.getOOBUnitRate();
                highestPriority = uc.getPriority();
            }
            remainingUnitsToReserve = remainingUnitsToReserve.subtract(reserveResult.getUnitsReserved());
            stillUnitCreditsLeft = reserveResult.stillHasUnitsLeftToReserve();
            if (reserveResult.getRecommendedReservationSecs() > 0 && reserveResult.getRecommendedReservationSecs() < recommendedReservationSecs) {
                recommendedReservationSecs = reserveResult.getRecommendedReservationSecs();
                log.debug("recommendedReservationSecs has been lowered to [{}]", recommendedReservationSecs);
            }
            if (DAO.isZero(remainingUnitsToReserve) && stillUnitCreditsLeft) {
                log.debug("We have reserved all we need to, and there are still UCs left. Breaking from UC loop point 2");
                // We have reserved all we need to so we can stop looping (but only if we have UCs left as we still need to see if there are any left for the FUI)
                break;
            }
        }

        /*
         * Step 2 - Whatever was not reserved against unit credits should be reserved against the monetary balance
         */
        if (highestPriorityBundlesUnitRate != null && DAO.isPositive(highestPriorityBundlesUnitRate) && BaseUtils.getBooleanProperty("env.bm.oob.rate.last.uc.rate", false)
                && ratingResult.getRetailRateCentsPerUnit().compareTo(highestPriorityBundlesUnitRate) > 0) {
            // Only lower unit rate, dont increase it
            ratingResult.setRetailRateCentsPerUnit(highestPriorityBundlesUnitRate);
            log.debug("env.bm.oob.rate.last.uc.rate is true. The last UC rate based on the amount charged to the wallet for the units gained was [{}] which will be used for OOB charging",
                    highestPriorityBundlesUnitRate);
        }

        BigDecimal unitsReservedByUnitCredits = units.subtract(remainingUnitsToReserve);

        BigDecimal amntCentsStillToReserve;

        amntCentsStillToReserve = remainingUnitsToReserve.multiply(ratingResult.getRetailRateCentsPerUnit());
        log.debug("Amount in cents still to reserve is [{}]", amntCentsStillToReserve);

        // Check if user has a social media unit credit.
        /*if(BaseUtils.getBooleanProperty("env.bm.social.media.tax.logic.enabled", false)) {
            if(acc.supportsOperationType(IAccount.ACCOUNT_OPERATION_TYPE.MONETARY_CHARGE) && 
                    !hasSocialMediaUnitCredit) {
                
                List<String> testAccounts = BaseUtils.getPropertyAsList("env.bm.social.media.tax.test.accounts");
                
                if(testAccounts == null || testAccounts.isEmpty() || testAccounts.contains(String.valueOf(acc.getAccountId()))) {
                    SimpleDateFormat howFrequent = new SimpleDateFormat("yyyyMMdd"); 
                    EventHelper.sendAccountEvent(acc, EventHelper.AccSubTypes.DEBIT, 0, howFrequent.format(new Date()));
                    throw new Exception("Account does not have the required social media tax bundle.");
                }
            }
        } */
        BigDecimal amntCentsReserved = BigDecimal.ZERO;

        if (DAO.isPositive(amntCentsStillToReserve) || !stillUnitCreditsLeft) {
            // Even if we dont have anything to reserve, we may still need to check if there is balance left
            // A negative unit rate should return the same result as if the account had no balance
            ReserveResult reserveResult = acc.reserve(amntCentsStillToReserve, sessionId, eventTimestamp, request, requestedReservationSecs, serviceInstance, ratingKey.getRatingGroup(), checkOnly);
            if (!wasAnyReservationDone) {
                wasAnyReservationDone = reserveResult.wasReservationCreated();
            }
            stillMoneyLeft = reserveResult.stillHasBalanceLeftToReserve();
            amntCentsReserved = reserveResult.getCentsReserved();
        }

        BigDecimal unitsReservedOnAccount = BigDecimal.ZERO;
        BigDecimal unitsStillToReserve;
        if (!DAO.isZero(ratingResult.getRetailRateCentsPerUnit())) {
            unitsReservedOnAccount = DAO.divide(amntCentsReserved, ratingResult.getRetailRateCentsPerUnit());
            unitsStillToReserve = remainingUnitsToReserve.subtract(unitsReservedOnAccount);
            log.debug("Units reserved on the account is [{}] and units still to reserve is [{}]", unitsReservedOnAccount, unitsStillToReserve.doubleValue());
        } else {
            unitsStillToReserve = new BigDecimal(0);
        }

        // Last resort processing
        boolean foundAUCWillingToDoLastResortReservation = false;
        if (DAO.isPositive(unitsStillToReserve)) {
            log.debug("There are still [{}] units that need to be reserved. Going to give the unit credits another go to try and reserve this", unitsStillToReserve);
            for (IUnitCredit uc : unitCredits) {
                if (!uc.supportsLastResortProcessing()) {
                    continue;
                }
                if (log.isDebugEnabled()) {
                    log.debug("Looking at UC for possible session reservation as a last resort: [{}]", uc);
                }
                UCReserveResult reserveResult = uc.reserve(ratingKey, unitsStillToReserve, units, acc, sessionId, eventTimestamp, request, requestedReservationSecs, ratingResult, serviceInstance, true, checkOnly, unitType, location);
                if (!wasAnyReservationDone) {
                    wasAnyReservationDone = reserveResult.wasReservationCreated();
                }
                unitsStillToReserve = unitsStillToReserve.subtract(reserveResult.getUnitsReserved());
                unitsReservedByUnitCredits = unitsReservedByUnitCredits.add(reserveResult.getUnitsReserved());
                if (reserveResult.getRecommendedReservationSecs() > 0 && reserveResult.getRecommendedReservationSecs() < recommendedReservationSecs) {
                    recommendedReservationSecs = reserveResult.getRecommendedReservationSecs();
                    log.debug("recommendedReservationSecs has been lowered to [{}]", recommendedReservationSecs);
                }
                if (reserveResult.stillHasUnitsLeftToReserve()) {
                    log.debug("The last UC still has the ability to accept a reservation");
                    foundAUCWillingToDoLastResortReservation = true;
                }
                if (DAO.isZero(unitsStillToReserve) && foundAUCWillingToDoLastResortReservation) {
                    // We have reserved all we need to and know that a UC has the ability to accept a reservation again (so we know whether to set FUI) 
                    // So we can stop looping
                    break;
                }
            }
        }

        /*
         * Step 3 - If after doing the reservations, there are no unit credit nor money left to reserve, nor unit credits that do last resort reserving, and the unit rate is non zero then set final unit flag
         */
        if (!stillUnitCreditsLeft && !stillMoneyLeft && !foundAUCWillingToDoLastResortReservation && !DAO.isZero(ratingResult.getRetailRateCentsPerUnit())) {
            log.debug("The final units have been reserved as there are no UCs nor balance left");
            gsu.setFinalUnits(true);
        } else {
            log.debug("The final units have not been reserved as there are still UCs or balance left");
            gsu.setFinalUnits(false);
        }


        /*
         * Step 5 - Only return what we could actually reserve
         */
        // We grant the units, not the monetary amount
        // Check for zero rate. If zero, we can let them know they have a lot reserved (e.g. incoming call)
        BigDecimal unitsReserved;
        int allowReservationSecs;
        if (DAO.isZero(ratingResult.getRetailRateCentsPerUnit())) {
            unitsReserved = units.multiply(BaseUtils.getBigDecimalProperty("env.bm.zerorate.reservation.units.multiplier", new BigDecimal(10)));
            log.debug("Retail Rate is zero so we will give more units and for longer than requested. Requested was [{}]units, allowing [{}]units", units, unitsReserved);
            allowReservationSecs = (int) ((BaseUtils.getDoubleProperty("env.bm.zerorate.reservation.secs.multiplier", 10.0d) * requestedReservationSecs));
            if (allowReservationSecs == 0) {
                allowReservationSecs = 1;
            }
        } else {
            unitsReserved = unitsReservedByUnitCredits.add(unitsReservedOnAccount);
            // Reserve secs must be the shortest of the UC reservations. They know whether the user should come back often to check for balance
            allowReservationSecs = recommendedReservationSecs;
            log.debug("Rate is non-zero. Requested was [{}]units, allowing [{}]units", units, unitsReserved);
        }
        log.debug("This reservation is being allowed for [{}]s", allowReservationSecs);
        gsu.setValidityTime(allowReservationSecs);
        gsu.setRetailCentsPerUnit(ratingResult.getRetailRateCentsPerUnit());
        gsu.setFromInterconnectCentsPerUnit(ratingResult.getFromInterconnectRateCentsPerUnit());
        gsu.setToInterconnectCentsPerUnit(ratingResult.getToInterconnectRateCentsPerUnit());

        if (ratingKey.getServiceCode().equals("SMS") && unitsReserved.compareTo(units) < 0) {
            log.debug("Special case for when one unit is requested (e.g. SMS) and we could not reserve the full unit");
            DAO.deleteReservationsBySessionId(em, sessionId); // This exception does not result in a rollback as rolling back could remove a successful charge that happened in this tx
            BaseUtils.addStatisticSample("ReservationAvailable", BaseUtils.STATISTIC_TYPE.percent, 0, 10000);
            throw new Exception("Insufficient available balance nor unit credits");
        }
        if (unitsReserved.compareTo(units) < 0 && ratingKey.getServiceCode().equals("txtype.sale.purchase")) {
            log.debug("Special case for when monetary charging for a non negotiable amount");
            DAO.deleteReservationsBySessionId(em, sessionId); // This exception does not result in a rollback as rolling back could remove a successful charge that happened in this tx
            BaseUtils.addStatisticSample("ReservationAvailable", BaseUtils.STATISTIC_TYPE.percent, 0, 10000);
            throw new Exception("Insufficient available balance nor unit credits");
        }

        /*if (unitsReserved.compareTo(units) < 0 && ratingKey.getServiceCode().equals("txtype.socialmedia.tax")) {
            log.debug("Special case for when monetary charging for a non negotiable amount - social media tax");
            DAO.deleteReservationsBySessionId(em, sessionId); // This exception does not result in a rollback as rolling back could remove a successful charge that happened in this tx
            BaseUtils.addStatisticSample("ReservationAvailable", BaseUtils.STATISTIC_TYPE.percent, 0, 10000);
            throw new Exception("Insufficient available balance nor unit credits");
        }*/
        if (DAO.isPositive(unitsReserved)) {
            gsu.setUnitQuantity(unitsReserved);
        } else if (DAO.isPositive(units)) {
            BaseUtils.addStatisticSample("ReservationAvailable", BaseUtils.STATISTIC_TYPE.percent, 0, 10000);
            if (firstFailureHint.isEmpty()) {
                throw new Exception("Insufficient available balance nor unit credits");
            } else {
                log.debug("There is a failureHint [{}] - so we use that as the Exception name", firstFailureHint);
                throw new Exception(firstFailureHint);
            }
        }

        BaseUtils.addStatisticSample("ReservationAvailable", BaseUtils.STATISTIC_TYPE.percent, 1, 10000);

        if (request != null && !wasAnyReservationDone && !checkOnly) {
            log.debug("This reservation could be trigger charged and yet no reservation has been done. Creating a reservation for zero so the request can be picked up");
            // For incoming offnet SMS scenario or free onnet SMS scenario where retail rate is zero but we need the record for interconnect billing
            DAO.createOrUpdateReservation(
                    em,
                    acc.getAccountId(),
                    sessionId,
                    0,
                    BigDecimal.ZERO,
                    eventTimestamp,
                    request,
                    requestedReservationSecs,
                    BigDecimal.ZERO,
                    checkOnly);
        }

        return gsu;
    }

    private static final String mbmib = BaseUtils.getProperty("env.portals.data.volume.display.suffix", "MB");
    private static final DecimalFormat dataWithoutDecimalPlace = new DecimalFormat("#,##0 " + mbmib, formatSymbols);
    private static final DecimalFormat dataWithDecimalPlace = new DecimalFormat("#,##0.0 " + mbmib, formatSymbols);

    private static String getBytesAsString(double val) {
        // convert to Mb or MiB
        val = val / BaseUtils.getDoubleProperty("env.portals.data.volume.display.denominator", 1000000d); // to show MB as MiB, change this property to 1048576
        if (val < 10) {
            //value is less than 10MB so return 1 decimal place
            return dataWithDecimalPlace.format(val);
        } else {
            return dataWithoutDecimalPlace.format(val);
        }
    }

    /**
     * This function is a bit of a guess on whether units are left as the actual
     * UC implementation may have rules saying units are not available Its a
     * good try at least and could be slightly wrong but its better than nothing
     *
     * @param acc
     * @param unitCredits
     * @param ratingKey
     * @param ratingResult
     * @param units
     * @return
     */
    private static BigDecimal getUnitsToReserveBasedOnContext(IAccount acc, List<IUnitCredit> unitCredits, RatingKey ratingKey, RatingResult ratingResult, BigDecimal units) {
        log.debug("In getUnitsToReserveBasedOnContext for units [{}]", units);
        BigDecimal unitsToReturn = units;
        if ((!ratingKey.getServiceCode().contains("32251") && !ratingKey.getServiceCode().contains("32252")) || (acc.canTakeACharge(ratingKey.getRatingGroup()) && !BaseUtils.getBooleanProperty("env.bm.reservation.low.bytes.when.oob", true))) {
            log.debug("This is not data usage or the account can take a charge so return the requested units");
        } else {
            log.debug("This is a data service and the account cant take a charge so lets only grant a small reservation if the data unit credits are running low");
            BigDecimal totalBytesLeft = BigDecimal.ZERO;
            for (IUnitCredit uc : unitCredits) {
                if (uc.getUnitType().equals("Byte") && DAO.isPositive(uc.getCurrentUnitsLeft())) {
                    totalBytesLeft = totalBytesLeft.add(uc.getCurrentUnitsLeft());
                }
            }
            // If balance is zero (or could be negative with SmileON) then dont bother lowering the reservation size
            if (DAO.isPositive(totalBytesLeft)
                    && totalBytesLeft.compareTo(BaseUtils.getBigDecimalProperty("env.bm.reservation.low.bytes.dividing.threshold", new BigDecimal(150000000))) < 0) {
                log.debug("Account data is running low so lets reserve half of whats left");
                unitsToReturn = DAO.divide(totalBytesLeft, BaseUtils.getBigDecimalProperty("env.bm.reservation.low.bytes.dividing.factor", THREE)).setScale(0, RoundingMode.HALF_EVEN);
                if (unitsToReturn.compareTo(THREE_MB) < 0) {
                    unitsToReturn = THREE_MB;
                }
            }
        }
        if (unitsToReturn.compareTo(units) > 0) {
            // Just a check that we wont pass a bigger result than requested
            unitsToReturn = units;
        }
        log.debug("getUnitsToReserveBasedOnContext returning [{}]", unitsToReturn);
        return unitsToReturn;
    }

    private static final BigDecimal THREE = new BigDecimal(3);
    private static final BigDecimal THREE_MB = new BigDecimal(3000000);

    /**
     * As per request from Zondi - make the description include whatever the
     * description was on the rate if there is one
     *
     * @param description
     * @param ratingResult
     * @return
     */
    private static String makeDescription(String description, RatingResult ratingResult) {
        if (ratingResult.getDescription() == null || ratingResult.getDescription().isEmpty()) {
            return description;
        }
        return description + ": " + ratingResult.getDescription();
    }

    private static String getTermCode(String termCode, String txType, String leg, String incomingTrunk, String outgoingTrunk) {
        if (termCode == null || termCode.isEmpty()) {
            return "";
        }
        if (!txType.startsWith("ext.")) {
            return termCode;
        }
        String ret = termCode;
        try {
            String termCodeCode = BaseUtils.getProperty("env.bm.termcode.code");
            log.debug("Calling runtime compiled code to get the termination code to put in account history. Incoming term code [{}] TxType [{}] Leg [{}] IncomingTrunk [{}] OutgoingTrunk [{}]",
                    new Object[]{termCode, txType, leg, incomingTrunk, outgoingTrunk});
            ret
                    = (String) Javassist.runCode(new Class[]{DAO.class
            }, termCodeCode, txType, termCode, leg, incomingTrunk, outgoingTrunk);
        } catch (Throwable e) {
            log.warn("Error transforming term code", e);
        }
        log.debug("Term code transformed from [{}] to [{}]", termCode, ret);
        return ret;
    }

    private static String getInfo(String txType, String location, String IPAddress, String termCode, String leg, String incomingTrunk, String outgoingTrunk) {
        String ret = null;
        if (!txType.startsWith("ext.")) {
            return ret;
        }
        try {
            String infoCode = BaseUtils.getProperty("env.bm.info.code");
            log.debug("Calling runtime compiled code to get the info to put in account history");
            ret
                    = (String) Javassist.runCode(new Class[]{DAO.class
            }, infoCode, txType, location, IPAddress, termCode, leg, incomingTrunk, outgoingTrunk);
        } catch (Throwable e) {
            log.warn("Error getting info", e);
        }
        log.debug("Info is [{}]", ret);
        return ret;
    }

    public String getInfoJavassist(String txType, String location, String IPAddress, String termCode, String leg, String incomingTrunk, String outgoingTrunk) throws Exception {
        if (!leg.equals("O")) {
            return null;
        }
        if (location.contains("address=169.159.") || location.contains("address=154.66.") || location.contains("address=160.152.")) {
            return "NW=NG_SmileLagos";
        }
        if (location.contains("address=197.211.")) {
            return "NW=NG_Glo";
        }
        if (location.contains("address=197.210.")) {
            return "NW=NG_MTN";
        }
        if (location.contains("address=10.33.")) {
            return "NW=NG_SmileLAN";
        }

        return null;
    }

}
