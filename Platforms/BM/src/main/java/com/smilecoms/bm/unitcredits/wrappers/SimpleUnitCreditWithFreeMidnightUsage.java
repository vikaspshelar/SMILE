/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.unitcredits.wrappers;

import com.smilecoms.bm.EventHelper;
import com.smilecoms.bm.charging.AccountFactory;
import com.smilecoms.bm.charging.IAccount;
import com.smilecoms.bm.charging.InsufficientFundsException;
import com.smilecoms.bm.charging.ServiceRules;
import com.smilecoms.bm.charging.ServiceSpecHelper;
import com.smilecoms.bm.db.model.ServiceInstance;
import com.smilecoms.bm.db.model.UnitCreditInstance;
import com.smilecoms.bm.db.op.DAO;
import com.smilecoms.bm.rating.RatingResult;
import com.smilecoms.bm.unitcredits.UCChargeResult;
import com.smilecoms.bm.unitcredits.UCReserveResult;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.PurchaseUnitCreditRequest;
import com.smilecoms.commons.sca.SCAContext;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class SimpleUnitCreditWithFreeMidnightUsage extends BaseUnitCredit {

    private static final Logger log = LoggerFactory.getLogger(SimpleUnitCredit.class);

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

        if (getBooleanPropertyFromConfig("LowersOOBRate")) {
            BigDecimal cur = getBigDecimalPropertyFromConfig("OOBRateCentsPerByte");
            if (!DAO.isPositive(cur)) {
                // If not specified it would be -1 and we use the bundles effective rate
                cur = getOOBUnitRate();
            }
            log.debug("This UC impacts the OOB rate when its depleted but is still valid. Rate is [{}]c/unit", cur);
            res.setOOBUnitRate(cur);
        } else {
            res.setOOBUnitRate(null);
        }

        BigDecimal availableUnits = getAvailableUnitsLeft();
        log.debug("Unit Credit Instance [{}] has [{}] units available (i.e. current units - reserved units)", uci.getUnitCreditInstanceId(), availableUnits);

        if(getBooleanPropertyFromConfig("MidniteUsageIsFree")) {
            Calendar et = Calendar.getInstance();
            et.setTime(eventTimetamp);
            int hour = et.get(Calendar.HOUR_OF_DAY); // 0..23
            if (hour >= 0 && hour < 6) {
                log.debug("Unit Credit Instance [{}] has MidniteUsageIsFree=true all traffic is free at this time, so just " +
                        "reserve what has been requested [{}] units and StillHasUnitsLeftToReserve=true", unitsToReserve);
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
                res.setStillHasUnitsLeftToReserve(true);
                res.setUnitsReserved(unitsToReserve);
                log.debug("Successfully reserved [{}] units on simple unit credit", unitsToReserve);
                return res;
            }            
        }
            
        if (DAO.isZeroOrNegative(availableUnits)) {
            log.debug("Unit Credit Instance [{}] has no units available for reserving. Returning with zero units reserved and StillHasUnitsLeftToReserve=false", uci.getUnitCreditInstanceId());
            res.setUnitsReserved(BigDecimal.ZERO);
            res.setStillHasUnitsLeftToReserve(false);
            return res;
        }

        if (DAO.isZero(unitsToReserve)) {
            log.debug("Units to reserve is zero so just returning now");
            res.setUnitsReserved(BigDecimal.ZERO);
            res.setStillHasUnitsLeftToReserve(true);
            return res;
        }

        // If we get here, then this UC has units left
        if (!checkOnly) {
            log.debug("We must ensure that the service has no system defined DPI Rules");
            long maxSpeed = getLongPropertyFromConfig("MaxSpeedbps");
            long maxSpeedDown = getLongPropertyFromConfig("MaxSpeedbpsDown");
            long maxSpeedUp = getLongPropertyFromConfig("MaxSpeedbpsUp");

            ServiceRules rules = new ServiceRules();
            rules.setClearAllButStickyDPIRules(true);
            rules.setQci(-1);
            String stickyDPIRules = getPropertyFromConfig("StickyDPIRules");
            if (stickyDPIRules != null) {
                log.debug("This UC has sticky DPI rules that must be added [{}]", stickyDPIRules);
                rules.setSystemDefinedDpiRulesToAdd(Utils.getSetFromCommaDelimitedString(stickyDPIRules));
            }

            if (maxSpeed > 0 || maxSpeedDown > 0 || maxSpeedUp > 0) {
                if (maxSpeed > 0) {
                    maxSpeedDown = maxSpeed;
                    maxSpeedUp = maxSpeed;
                }
                // Override speed. Defaults of service but leave sticky rules
                rules.setMaxBpsDown(maxSpeedDown);
                rules.setMaxBpsUp(maxSpeedUp);
            } else {
                // Defaults of service but leave sticky rules
                rules.setMaxBpsDown(-1L);
                rules.setMaxBpsUp(-1L);
            }
            ServiceSpecHelper.applyServiceRules(serviceInstance, rules);
        }

        BigDecimal unitsReserved;

        if (availableUnits.compareTo(unitsToReserve) > 0) {
            // Unit credit has more units available than required. Reserve the required units
            unitsReserved = unitsToReserve;
            res.setStillHasUnitsLeftToReserve(true);
        } else {
            // Unit credit has just enough or not enough
            unitsReserved = availableUnits;
            res.setStillHasUnitsLeftToReserve(false);
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
                unitsReserved,
                checkOnly);
        res.setReservationWasCreated();
        availableUnitsLeft = null;
        res.setUnitsReserved(unitsReserved);
        log.debug("Successfully reserved [{}] units on simple unit credit", unitsReserved);
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

        Date lastUsed = uci.getLastUsedDate();
        
        UCChargeResult res = super.charge(ratingKey, unitsToCharge, originalUnitsToCharge, acc, ratingResult, isLastResort, unitType, eventTimetamp);
        
        if (getBooleanPropertyFromConfig("LowersOOBRate")) {
            BigDecimal cur = getBigDecimalPropertyFromConfig("OOBRateCentsPerByte");
            if (!DAO.isPositive(cur)) {
                // If not specified it would be -1 and we use the bundles effective rate
                cur = getOOBUnitRate();
            }
            log.debug("This UC impacts the OOB rate when its depleted but is still valid. Rate is [{}]c/unit", cur);
            res.setOOBUnitRate(cur);
        } else {
            res.setOOBUnitRate(null);
        }
        BigDecimal currentUnits = uci.getUnitsRemaining();
        BigDecimal availableUnits = getAvailableUnitsLeft();

        double currentUnitsBeforeCharge = currentUnits.doubleValue();

        if (DAO.isZeroOrNegative(availableUnits)) {
            log.debug("UCI [{}] has no available units remaining so just returning", uci.getUnitCreditInstanceId());
            res.setFreeRevenueCents(BigDecimal.ZERO);
            res.setRevenueCents(BigDecimal.ZERO);
            res.setUnitsCharged(BigDecimal.ZERO);
            return res;
        }

        if (DAO.isZero(unitsToCharge)) {
            log.debug("Units to charge is zero so just returning");
            res.setFreeRevenueCents(BigDecimal.ZERO);
            res.setRevenueCents(BigDecimal.ZERO);
            res.setUnitsCharged(BigDecimal.ZERO);
            return res;
        }

        if (availableUnits.compareTo(unitsToCharge) > 0) {
            // Unit credit has more units available than required. Subtract the required units
            if (log.isDebugEnabled()) {
                log.debug("Unit credit has [{}] units available which is more than required. Deducting [{}] units from UC Instance[{}]", new Object[]{availableUnits, unitsToCharge, uci.getUnitCreditInstanceId()});
            }
            res.setUnitsCharged(unitsToCharge);

        } else {
            // Unit credit has just enough or not enough
            if (log.isDebugEnabled()) {
                log.debug("Unit credit has [{}] units available which is not as much as required or exactly whats required. Deducting [{}] units from UC Instance[{}]", new Object[]{availableUnits, availableUnits, uci.getUnitCreditInstanceId()});
            }
            res.setUnitsCharged(availableUnits);
        }

        if(getBooleanPropertyFromConfig("MidniteUsageIsFree")) {
          
            Calendar et = Calendar.getInstance();
            et.setTime(eventTimetamp);
            int hour = et.get(Calendar.HOUR_OF_DAY); // 0..23
            if (hour >= 0 && hour < 6) {
                log.debug("This is midnite. Setting Aux counter 1");
                if (uci.getAuxCounter1() == null) { //Keeps track of all night usage from start to expiry of the bundle.
                    uci.setAuxCounter1(BigDecimal.ZERO);
                }
                setAuxCounter1(uci.getAuxCounter1().add(unitsToCharge));
             
                if ((lastUsed != null && !Utils.areDatesOnSameDay(lastUsed, eventTimetamp) && lastUsed.before(eventTimetamp))
                    || uci.getAuxCounter2() == null) {
                log.debug("Last used [{}]", lastUsed);
                setAuxCounter2(BigDecimal.ZERO); // Only keeps night usage of the previous/current night.
                }
                setAuxCounter2(uci.getAuxCounter2().add(unitsToCharge));
                
                res.setUnitsCharged(BigDecimal.ZERO); // Nothing charged 
            }            
        }
        
        setUnitCreditUnitsRemaining(currentUnits.subtract(res.getUnitsCharged()));

        res.setRevenueCents(res.getUnitsCharged().multiply(uci.getRevenueCentsPerUnit())); // Revenue is the unit credit rate X unit credit units
        res.setFreeRevenueCents(res.getUnitsCharged().multiply(uci.getFreeCentsPerUnit())); // Free Revenue is the free unit credit rate X unit credit units
        res.setUnitsRemaining(getCurrentUnitsLeft());
        if (log.isDebugEnabled()) {
            log.debug("Successfully charged [{}] units on simple unit credit. This is [{}]cents revenue", res.getUnitsCharged(), res.getRevenueCents());
        }

        double crossOverWindow = BaseUtils.getDoubleProperty("env.bm.unitcredit.rewards.crossover.window.bytes", 100000000d);
        if (uci.getUnitsRemaining().doubleValue() < crossOverWindow && currentUnitsBeforeCharge >= crossOverWindow) {
            // UCI crossed over from 100MB or more available, to less than 100MB available. Can give the next UC away so that its available by the time the next charge comes through
            int idToProvision = getIntPropertyFromConfig("UCSpecToProvisionWhenFinished");
            if (idToProvision > 0) {
                try {
                    log.debug("Purchase date is [{}]", getPurchaseDate());
                    Calendar thirtyDaysAfterPurchaseDate = Calendar.getInstance();
                    thirtyDaysAfterPurchaseDate.setTime(getPurchaseDate());
                    thirtyDaysAfterPurchaseDate.add(Calendar.DATE, BaseUtils.getIntProperty("env.bm.unitcredit.rewards.limit.days", 30));
                    // A null OriginalExpiry means the date has never been extended beyond its initial allocation
                    if (Utils.isInTheFuture(thirtyDaysAfterPurchaseDate.getTime())) {
                        log.debug("UC Instance [{}] is almost depleted as it has [{}] units left so UC Spec [{}] will now be provisioned", new Object[]{uci.getUnitCreditInstanceId(), uci.getUnitsRemaining(), idToProvision});

                        PurchaseUnitCreditRequest provReq = new PurchaseUnitCreditRequest();
                        provReq.setSCAContext(new SCAContext());
                        provReq.getSCAContext().setAsync(true);
                        provReq.getSCAContext().setTxId("UNIQUE-Post-UCI-" + uci.getUnitCreditInstanceId());
                        provReq.setAccountId(uci.getAccountId());
                        provReq.setUniqueId("Post-UCI-" + uci.getUnitCreditInstanceId());
                        provReq.setNumberToPurchase(1);
                        provReq.setUnitCreditSpecificationId(idToProvision);
                        provReq.setProductInstanceId(uci.getProductInstanceId());
                        log.debug("Calling SCA to provision unit credit [{}] on account [{}]", idToProvision, uci.getAccountId());
                        SCAWrapper.getAdminInstance().purchaseUnitCredit(provReq);
                        log.debug("Finished provisioning new UC due to near completion of this one");
                    } else {
                        log.debug("The [{}] days post purchase date has passed so free UC wont be awarded", BaseUtils.getIntProperty("env.bm.unitcredit.rewards.limit.days", 30));
                    }
                } catch (Exception e) {
                    log.warn("Error provisioning new UC on post depletion", e);
                    BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "BM", "Error provisioning new UC upon near completion of existing one. Error: " + e.toString());
                }
            }
        }

        return res;
    }

    @Override
    public void split(long targetAccountId, BigDecimal units, int targetProductInstanceId) throws Exception {

        if (DAO.isZeroOrNegative(units)) {
            throw new Exception("Invalid number of units to split off -- " + units);
        }
        if (getAvailableUnitsLeft().compareTo(units) < 0) {
            throw new InsufficientFundsException();
        }

        boolean allowSplitting = getBooleanPropertyFromConfig("AllowSplitting");
        if (!allowSplitting) {
            throw new Exception("Unit credit does not allow splitting -- " + ucs.getUnitCreditSpecificationId());
        }

        boolean denySubSplitting = getBooleanPropertyFromConfig("DenySubSplitting");
        if (denySubSplitting && Utils.getBooleanValueFromCRDelimitedAVPString(uci.getInfo(), "Split")) {
            throw new Exception("Unit credit does not allow sub splitting -- " + ucs.getUnitCreditSpecificationId());
        }
        
        boolean allowsSharing = getBooleanPropertyFromConfig("AllowSharing");
        if (!allowsSharing && targetProductInstanceId == 0) {
            log.debug("Unit credits that dont allow sharing must have a non zero product instance Id. We will default to the first applicable product on this account");
            targetProductInstanceId = DAO.getLastActiveOrTempDeactiveProductInstanceIdForAccountAndUnitCreditSpec(em, targetAccountId, ucs.getUnitCreditSpecificationId());
            if (targetProductInstanceId == -1) {
                throw new Exception("Unit credits that dont allow sharing must have a non zero product instance Id");
            }
        }

        int minSplitValidityDays = getIntPropertyFromConfig("MinDaysLeftToSplit");
        if (minSplitValidityDays > 0 && Utils.getFutureDate(Calendar.DATE, minSplitValidityDays).after(uci.getExpiryDate())) {
            throw new Exception("Cannot split the unit credit as its too close to expiry");
        }

        UnitCreditInstance uciNew = new UnitCreditInstance();
        uciNew.setAccountId(targetAccountId == 0 ? uci.getAccountId() : targetAccountId);
        uciNew.setPOSCentsCharged(BigDecimal.ZERO);
        uciNew.setPOSCentsDiscount(BigDecimal.ZERO);
        // Baseline usage rate is same as parent
        uciNew.setBaselineCentsPerUnit(uci.getBaselineCentsPerUnit());
        uciNew.setRevenueCentsPerUnit(uci.getRevenueCentsPerUnit());
        int maxSplitExpiryDays = getIntPropertyFromConfig("SplitMaxExpiryDays");
        if (maxSplitExpiryDays > 0) {
            Date d = Utils.getFutureDate(Calendar.DATE, maxSplitExpiryDays);
            if (d.before(uci.getExpiryDate())) {
                uciNew.setExpiryDate(d);
            }
        }
        if (uciNew.getExpiryDate() == null) {
            uciNew.setExpiryDate(uci.getExpiryDate());
        }
        uciNew.setExtTxid("Split_" + uci.getUnitCreditInstanceId());
        uciNew.setFreeCentsPerUnit(uci.getFreeCentsPerUnit());
        uciNew.setInfo(Utils.setValueInCRDelimitedAVPString(uci.getInfo(), "Split", "true"));
        uciNew.setProductInstanceId(targetProductInstanceId);
        uciNew.setProvisionedByCustomerProfileId(uci.getProvisionedByCustomerProfileId());
        uciNew.setPurchaseDate(uci.getPurchaseDate());
        uciNew.setStartDate(uci.getStartDate());
        uciNew.setEndDate(uciNew.getExpiryDate());
        uciNew.setUnitCreditSpecificationId(uci.getUnitCreditSpecificationId());
        uciNew.setUnitsAtStart(units);
        uciNew.setUnitsRemaining(units);
        uciNew.setSaleRowId(uci.getSaleRowId());
        em.persist(uciNew);
        em.flush();
        em.refresh(uciNew);
        com.smilecoms.bm.db.model.AccountHistory ah = DAO.createAccountHistoryForEvent(
                em,
                "",
                String.valueOf(uci.getAccountId()),
                String.valueOf(uciNew.getAccountId()),
                BigDecimal.ZERO,
                AccountFactory.getAccount(em, uci.getAccountId(), true),
                new Date(),
                "Split " + ucs.getUnitCreditName() + " " + uci.getUnitCreditInstanceId() + " to " + uciNew.getUnitCreditInstanceId(),
                units,
                "",
                "txtype.uc.split", // Txtype
                "",
                0,
                units.negate(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "",
                "",
                "",
                "",
                "",
                "",
                BigDecimal.ZERO, null, null, null);
        setUnitCreditUnitsRemaining(getCurrentUnitsLeft().subtract(units));

        EventHelper.sendUCSplitEvent(uci.getAccountId(), this, ah.getId());
    }

    /**
     * Returns the number of OCTETS per SECOND or SMS etc based on the bundles
     * Cents per OCTET
     *
     * We are passed in the Cents / SECOND or SMS and the UC knows its Baseline
     * Cents/OCTET
     *
     * Then
     *
     * CENTS/SECOND Div CENTS/OCTET = OCTETS/SECOND
     *
     *
     *
     *
     * @return OCTETS/SECOND (or OCTETS/SMS etc)
     */
    private BigDecimal baselineConversionRate;

    protected BigDecimal getBaselineConversionRate(RatingResult ratingResult, RatingKey ratingKey) {
        if (baselineConversionRate != null) {
            return baselineConversionRate;
        }

        String priceDriver = getPropertyFromConfig("LocalPriceDriver");
        if (priceDriver == null) {
            priceDriver = "MiB";
        }

        BigDecimal percentDiscount = getBigDecimalPropertyFromConfigZeroIfMissing("DISC-" + ratingKey.getServiceCode() + "-" + ratingKey.getRatingGroup());
        BigDecimal discount = DAO.divide(percentDiscount, new BigDecimal(100)).multiply(ratingResult.getRetailRateCentsPerUnit());
        BigDecimal rate = ratingResult.getRetailRateCentsPerUnit().subtract(discount);
        log.debug("Retail rate per unit after UC discount is [{}]", rate);
        BigDecimal convRate = uci.getBaselineCentsPerUnit();
        if (priceDriver.equals("MiB")) {
            // Ignore what was paid as we want to take out MiB irrespective of the price paid
            int baselineCents = getIntPropertyFromConfig("BaselineCents");
            if (baselineCents <= 0) {
                throw new java.lang.IllegalArgumentException("Cannot baseline driven by MiB without config parameter BaselineCents on the unit credit");
            }
            convRate = DAO.divide(new BigDecimal(baselineCents), new BigDecimal(ucs.getUnits()));
        }

        if (DAO.isZero(convRate)) {
            log.debug("Unit credit has a zero cents per unit rate so cannot be used for baselining");
            throw new java.lang.IllegalArgumentException("Cannot baseline with a free unit credit");
        }
        baselineConversionRate = DAO.divide(rate, convRate);
        log.debug("Baseline conversion rate is [{}] Bytes per Second (or per SMS etc) using convRate [{}]", baselineConversionRate, convRate);
        return baselineConversionRate;
    }

    // Normal bundles only send if some data left
    @Override
    public void do1DayPreExpiryProcessing() {
        log.debug("UCI [{}] called for 1 day pre expiry processing", uci.getUnitCreditInstanceId());
        if (DAO.isPositive(getAvailableUnitsLeft())) {
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("1DayPreExpiryEventSubType"));
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("1DayPreExpiryEventSubType1"));
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("1DayPreExpiryEventSubType2"));
        }
    }

    // Normal bundles only send if some data left
    @Override
    public void do2DaysPreExpiryProcessing() {
        log.debug("UCI [{}] called for 2 day pre expiry processing", uci.getUnitCreditInstanceId());
        if (DAO.isPositive(getAvailableUnitsLeft())) {
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("2DaysPreExpiryEventSubType"));
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("2DaysPreExpiryEventSubType1"));
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("2DaysPreExpiryEventSubType2"));
        }
    }

    // Normal bundles only send if some data left
    @Override
    public void do3DaysPreExpiryProcessing() {
        log.debug("UCI [{}] called for 3 day pre expiry processing", uci.getUnitCreditInstanceId());
        if (DAO.isPositive(getAvailableUnitsLeft())) {
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("3DaysPreExpiryEventSubType"));
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("3DaysPreExpiryEventSubType1"));
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("3DaysPreExpiryEventSubType2"));
        }
    }

    @Override
    public void do1WeekPreExpiryProcessing() {

        if (DAO.isPositive(getAvailableUnitsLeft())) {
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("1WeekPreExpiryEventSubType"));
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("1WeekPreExpiryEventSubType1"));
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("1WeekPreExpiryEventSubType2"));
        }
        int daysToAutoExtend = getIntPropertyFromConfig("AutoExtendDays");
        String extensionCount = getInfoValue("ExtensionCount");
        if (daysToAutoExtend > 0 && (extensionCount == null || extensionCount.equals("0"))) {
            log.debug("UCI has never been extended");
            Date currentExpiryDate = uci.getExpiryDate();
            Calendar cal = Calendar.getInstance();
            cal.setTime(currentExpiryDate);
            cal.add(Calendar.DATE, daysToAutoExtend);
            Date newExpiryDate = cal.getTime();
            log.debug("Autoextending date [{}] by [{}] days to [{}]", new Object[]{currentExpiryDate, daysToAutoExtend, newExpiryDate});
            DAO.extendUnitCreditExpiryAndEndDate(em, uci.getUnitCreditInstanceId(), currentExpiryDate, newExpiryDate);
            setInfoValue("ExtensionCount", "1");
            log.debug("Committing transaction to prevent locks");
            JPAUtils.commitTransaction(em);
            JPAUtils.beginTransaction(em);
        }
    }
    
    
}
