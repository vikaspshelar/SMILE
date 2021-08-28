/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.unitcredits.wrappers;

import com.smilecoms.bm.EventHelper;
import com.smilecoms.bm.charging.IAccount;
import com.smilecoms.bm.charging.ServiceRules;
import com.smilecoms.bm.charging.ServiceSpecHelper;
import com.smilecoms.bm.db.model.DailyFupInfo;
import com.smilecoms.bm.db.model.Reservation;
import com.smilecoms.bm.db.model.ServiceInstance;
import com.smilecoms.bm.db.model.UnitCreditInstance;
import com.smilecoms.bm.db.model.UnitCreditSpecification;
import com.smilecoms.bm.db.op.DAO;
import com.smilecoms.bm.rating.RatingResult;
import com.smilecoms.bm.unitcredits.UCChargeResult;
import com.smilecoms.bm.unitcredits.UCReserveResult;
import com.smilecoms.bm.unitcredits.UnitCreditManager;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.text.SimpleDateFormat;  
import org.slf4j.*;
import javax.persistence.NoResultException;

public class DustUnitCredit extends BaseUnitCredit {

    private static final Logger log = LoggerFactory.getLogger(DustUnitCredit.class);

    @Override
    /**
     * We want Unlimited UC to always act like its got units left
     */
    public boolean hasAvailableUnitsLeft() {
        return true;
    }

    @Override
    public void provision(IAccount acc, int productInstanceId, Date startDate, boolean verifyOnly,
            String extTxid, double posCentsPaidEach, double posCentsDiscountEach, int saleLineId, String info) throws Exception {

        super.provision(acc, productInstanceId, startDate, verifyOnly, extTxid, posCentsPaidEach, posCentsDiscountEach, saleLineId, info);

        if (getBooleanPropertyFromConfig("ApplySpeedUpdateOnProvision") && mustApplySpeedUpdateOnProvision(productInstanceId, acc.getAccountId())) {
            forceSpeedUpdateOnService(productInstanceId);
        }

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
        log.debug("DUST Unit Credit Instance [{}] has [{}] units available (i.e. current units - reserved units)", uci.getUnitCreditInstanceId(), availableUnits);

        // Check to ensure we do not reserve for social media tax if unlimited has below zero
        BigDecimal unitsReserved;
        if (BaseUtils.getBooleanProperty("env.bm.social.media.tax.logic.enabled", false)
                && (ratingKey.getServiceCode().equals("txtype.socialmedia.tax")
                || ratingKey.getServiceCode().equals("txtype.socialmedia.tax.unlimited"))) {
            if (availableUnits.compareTo(unitsToReserve) > 0) {
                // Unit credit has more units available than required. Reserve the required units
                unitsReserved = unitsToReserve;
                res.setStillHasUnitsLeftToReserve(true);
            } else {
                // Unit credit has just enough or not enough
                if (DAO.isZeroOrNegative(availableUnits)) {
                    unitsReserved = BigDecimal.ZERO;
                } else {
                    unitsReserved = availableUnits;
                }
                res.setStillHasUnitsLeftToReserve(false);
            }
            res.setUnitsReserved(unitsReserved);
        } else { // For everything else
            // This bundle always acts like it has units left and says that it reserved all the units requested
            res.setStillHasUnitsLeftToReserve(true);
            res.setUnitsReserved(unitsToReserve);
        }

        if (!checkOnly) {
            ensureDustCapsAreInPlace(serviceInstance, location);
        }
        res.setRecommendedReservationSecs(BaseUtils.getIntProperty("env.bm.dust.reserve.secs", reservationSecs));
        DAO.createOrUpdateReservation(
                em,
                acc.getAccountId(),
                sessionId,
                uci.getUnitCreditInstanceId(),
                BigDecimal.ZERO,
                eventTimetamp,
                request,
                res.getRecommendedReservationSecs(),
                unitsToReserve,
                checkOnly);
        res.setReservationWasCreated();
        availableUnitsLeft = null;
        log.debug("Successfully reserved [{}] units on DUST unit credit", unitsToReserve);
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

        Date lastUsed = uci.getLastUsedDate();

        UCChargeResult res = super.charge(ratingKey, unitsToCharge, originalUnitsToCharge, acc, ratingResult, isLastResort, unitType, eventTimetamp);

        // All unlimited usage must be considered revenue generating
        res.setPaidForUsage(DAO.isPositive(uci.getPOSCentsCharged()));
        res.setUnitsCharged(unitsToCharge);
        res.setOOBUnitRate(null);

        BigDecimal currentUnits = getCurrentUnitsLeft();
        BigDecimal availableUnits = getAvailableUnitsLeft();
        BigDecimal positiveUnitsCharged;
        if (currentUnits.compareTo(unitsToCharge) > 0) {
            // Unit credit has more units available than required. Subtract the required units
            if (log.isDebugEnabled()) {
                log.debug("DUST Unit credit has [{}] units available which is more than required. Deducting [{}] units from UC Instance[{}]", new Object[]{availableUnits, unitsToCharge, uci.getUnitCreditInstanceId()});
            }
            setUnitCreditUnitsRemaining(currentUnits.subtract(unitsToCharge));
            positiveUnitsCharged = unitsToCharge;
        } else {
            // Unit credit has just enough or not enough
            if (log.isDebugEnabled()) {
                log.debug("DUST Unit credit has [{}] units available which is not as much as required or exactly whats required. Deducting [{}] units from UC Instance[{}]. It may go negative which is fine", new Object[]{currentUnits, unitsToCharge, uci.getUnitCreditInstanceId()});
            }
            setUnitCreditUnitsRemaining(currentUnits.subtract(unitsToCharge));
            if (DAO.isPositive(currentUnits)) {
                positiveUnitsCharged = currentUnits;
            } else {
                positiveUnitsCharged = BigDecimal.ZERO;
            }

            if (BaseUtils.getBooleanProperty("env.bm.social.media.tax.logic.enabled", false)
                    && (ratingKey.getServiceCode().equals("txtype.socialmedia.tax")
                    || ratingKey.getServiceCode().equals("txtype.socialmedia.tax.unlimited"))) {

                if (DAO.isPositive(positiveUnitsCharged)) {
                    setUnitCreditUnitsRemaining(currentUnits.subtract(positiveUnitsCharged));
                    res.setUnitsCharged(positiveUnitsCharged);
                } else {
                    res.setUnitsCharged(BigDecimal.ZERO);
                    setUnitCreditUnitsRemaining(currentUnits);
                }
            }
        }

        Calendar et = Calendar.getInstance();
        et.setTime(eventTimetamp);
        int hour = et.get(Calendar.HOUR_OF_DAY); // 0..23
        if (hour >= 0 && hour < 6) {
            log.debug("This is midnite. Setting Aux counter 1");
            if (uci.getAuxCounter1() == null) {
                uci.setAuxCounter1(BigDecimal.ZERO);
            }
            setAuxCounter1(uci.getAuxCounter1().add(unitsToCharge));
        }

        if ((lastUsed != null && !Utils.areDatesOnSameDay(lastUsed, eventTimetamp) && lastUsed.before(eventTimetamp))
                || uci.getAuxCounter2() == null) {
            log.debug("Last used [{}]", lastUsed);
            setAuxCounter2(BigDecimal.ZERO);
        }

        setAuxCounter2(uci.getAuxCounter2().add(unitsToCharge));

        res.setRevenueCents(positiveUnitsCharged.multiply(uci.getRevenueCentsPerUnit())); // Revenue is the unit credit rate X unit credit units that could be covered by the UC
        res.setFreeRevenueCents(BigDecimal.ZERO); // Free Revenue is the free unit credit rate X unit credit units that went negative
        res.setUnitsRemaining(getCurrentUnitsLeft());
        if (log.isDebugEnabled()) {
            log.debug("Successfully charged [{}] units on DUST unit credit. This is [{}]cents revenue. Positive units charged was [{}]", new Object[]{res.getUnitsCharged(), res.getRevenueCents(), positiveUnitsCharged});
        }
        return res;
    }

    private void ensureDustCapsAreInPlace(ServiceInstance serviceInstance, String location) {
        try {
            log.debug("Location for device making use of Dust is [{}]", location);
            long unitsUsed = -1;
            String dustVer = null;
            String hint = getInfoValue("BehaviourHint");
            if (hint != null && !hint.isEmpty()) {
                log.debug("Hint is [{}]", hint);
                dustVer = Utils.getValueFromCommaDelimitedAVPString(hint, "DUSTVersion");
            }
            if (dustVer == null) {
                dustVer = "";
            }
            
            
            if(getBooleanPropertyFromConfig("ContainsPurchaseDateControlledVersion")) {
                
                try {
                    
                    log.debug("Uses PurchaseDateControlledVersion : [{}]", getPropertyFromConfig("PurchaseDateControlledVersion"));
                    
                    if(getPropertyFromConfigBlankIfNull("PurchaseDateControlledVersionStart").trim().length()>0) {
                        Date purchaseDate = getUnitCreditInstance().getPurchaseDate();                    
                        Date configStartDate = new SimpleDateFormat("yyyy-MM-dd").parse(getPropertyFromConfigBlankIfNull("PurchaseDateControlledVersionStart"));
                    
                        if(purchaseDate.after(configStartDate)) {
                            log.debug("Purchased after controlDate. PurchaseDate [{}] is after ControlDate [{}]",purchaseDate,configStartDate);
                            dustVer = getPropertyFromConfigBlankIfNull("PurchaseDateControlledVersion");
                        }
                    } 
                    
                } catch (Exception e) {
                    
                }
            }

            dustVer = overrideDUSTVersionIfNecessary(dustVer, serviceInstance, location);

            log.debug("Dust version is [{}]", dustVer);
            if (getBooleanPropertyFromConfig("ForceBand1AtMidnite" + dustVer)) {
                Calendar et = Calendar.getInstance();
                int hour = et.get(Calendar.HOUR_OF_DAY); // 0..23
                if (hour >= 0 && hour < 6) {
                    log.debug("This is midnite and ForceBand1AtMidnite=true so saying units used is 0");
                    unitsUsed = 0;
                }
            }
            if (unitsUsed == -1) {
                if (getBooleanPropertyFromConfig("IgnoreMidniteUsage" + dustVer)) {
                    long aux1 = uci.getAuxCounter1() == null ? 0 : uci.getAuxCounter1().longValue();
                    unitsUsed = getUnitCreditInstance().getUnitsAtStart().longValue() - getCurrentUnitsLeft().longValue() - aux1;
                    log.debug("Ignoring midnite usage. Daytime usage is [{}]", unitsUsed);
                } else {
                    unitsUsed = getUnitCreditInstance().getUnitsAtStart().longValue() - getCurrentUnitsLeft().longValue();
                }
            }

            DustParams params = getDustParams(location, unitsUsed, dustVer);
            
            if(getBooleanPropertyFromConfig("UseTorrentBlock")) {
                if(params.dpiRules.trim().isEmpty())
                    params.dpiRules = getPropertyFromConfigBlankIfNull("Band" + params.band + "TorrentDPIRules" + dustVer);  
                else
                    params.dpiRules = params.dpiRules + "," + getPropertyFromConfigBlankIfNull("Band" + params.band + "TorrentDPIRules" + dustVer);              
            }
            
            log.debug("DUST speed is [{}] Down [{}] Up and QCI [{}] and DPI Rules [{}]", new Object[]{params.maxSpeedDown, params.maxSpeedUp, params.qci, params.dpiRules});
            ServiceRules rules = new ServiceRules();
            rules.setClearAllButStickyDPIRules(true);
            rules.setSystemDefinedDpiRulesToAdd(Utils.getSetFromCommaDelimitedString(params.dpiRules));            
            rules.setMaxBpsDown(params.maxSpeedDown);
            rules.setMaxBpsUp(params.maxSpeedUp);
            rules.setQci(params.qci);                  
            ServiceSpecHelper.applyServiceRules(serviceInstance, rules);

        } catch (Exception e) {
            log.warn("Error putting dust caps in place for SI {}, {}", serviceInstance.getServiceInstanceId(), e);
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "BM", "Error putting dust caps in place: " + e.toString());
        }
    }

    private DustParams getDustParams(String location, long unitsUsed, String dustVer) throws Exception {        
                
        DustParams params = getParamsForUsage(unitsUsed, dustVer);

        if (!params.useCongestion) {
            log.debug("No need to look at congestion to modify the speed");
            return params;
        }

        int intReductionPercent = getIntPropertyFromConfig("Band" + params.band + "IntCongReductionPercent" + dustVer);
        boolean isIntCongestion;

        String congested = getNetworkConfig("IntLinkCongested", false);
        log.debug("IntLinkCongested network value is [{}]", congested);
        if (congested != null && (congested.equalsIgnoreCase("true") || congested.equalsIgnoreCase("false"))) {
            isIntCongestion = congested.equalsIgnoreCase("true");
        } else {
            int intStartMBPS = getIntPropertyFromConfig("DefaultTotalCapacityMbpsAtPGW" + dustVer);
            String startMBPSString = getNetworkConfig("TotalCapacityMbpsAtPGW", false);
            if (startMBPSString != null && !startMBPSString.isEmpty()) {
                try {
                    intStartMBPS = Integer.parseInt(startMBPSString);
                } catch (Exception e) {
                    log.warn("Error getting network max mbps", e);
                    BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "Network MaxMbps", "Error getting network max mbps " + e.toString());
                }
            }
            log.debug("TotalCapacityMbpsAtPGW is [{}]", intStartMBPS);
            int intLinkMBPS = getSectorCongestion("TOTAL_MBPS");
            isIntCongestion = (intLinkMBPS >= intStartMBPS);
        }

        if (isIntCongestion) {
            log.debug("There is international congestion");
            double throttleBy = params.maxSpeedDown * intReductionPercent / 100d;
            params.maxSpeedDown = (long) (params.maxSpeedDown - throttleBy);
            params.maxSpeedUp = params.maxSpeedDown;
            params.dpiRules = getPropertyFromConfigBlankIfNull("Band" + params.band + "IntCongDPIRules" + dustVer);
        } else {
            log.debug("There is no international congestion");
            params.dpiRules = getPropertyFromConfigBlankIfNull("Band" + params.band + "NoIntCongDPIRules" + dustVer);
        }

        int sectorCongestion = getSectorCongestion(location);
        int congestionOutOf10 = sectorCongestion / 10;
        log.debug("Sectors congestion is [{}]% which is [{}] out of 10", sectorCongestion, congestionOutOf10);
        String congestionConfig = getPropertyFromConfig("Band" + params.band + "Cong" + String.valueOf(congestionOutOf10) + dustVer);
        if (congestionConfig != null) {
            String[] bits = congestionConfig.split(",");
            if (bits.length == 2) {
                params.qci = Integer.parseInt(bits[1]);
            }
            double throttlePercent = Double.parseDouble(bits[0]);
            double throttleBy = params.maxSpeedDown * throttlePercent / 100d;
            log.debug("Throttle percent is [{}] and throttle amount is [{}]", throttlePercent, throttleBy);
            params.maxSpeedDown = (long) (params.maxSpeedDown - throttleBy);
            params.maxSpeedUp = params.maxSpeedDown;
        } else {
            log.debug("No dust congestion config for band [{}] and congestion [{}]", params.band, congestionOutOf10);
        }
        return params;
    }

    private DustParams getParamsForUsage(long unitsUsed, String dustVer) throws Exception {
        // return the band we are in or the highest one if we are beyond the last band
        DustParams params = new DustParams();
        int startBand = 1;

        long auxCounter1KickInBytes = getLongPropertyFromConfig("AuxCnt1KickIn" + dustVer);
        int auxCounter1KickInMinBand = getIntPropertyFromConfig("AuxCnt1KickInMinBand" + dustVer);
        if (auxCounter1KickInBytes != -1
                && auxCounter1KickInMinBand != -1
                && uci.getAuxCounter1() != null
                && uci.getAuxCounter1().longValue() >= auxCounter1KickInBytes) {
            startBand = auxCounter1KickInMinBand;
        }

        long auxCounter2KickInBytes = getLongPropertyFromConfig("AuxCnt2KickIn" + dustVer);
        int auxCounter2KickInMinBand = getIntPropertyFromConfig("AuxCnt2KickInMinBand" + dustVer);
        if (auxCounter2KickInBytes != -1
                && auxCounter2KickInMinBand != -1
                && uci.getAuxCounter2() != null
                && uci.getAuxCounter2().longValue() >= auxCounter2KickInBytes
                && startBand < auxCounter2KickInMinBand) {
            startBand = auxCounter2KickInMinBand;
        }

        int cnt = startBand;

        String FUPEvent = null;

        do {
            long bytes = getLongPropertyFromConfig("Band" + cnt + "EndBytes" + dustVer);
            log.debug("Looking at band [{}] which ends at [{}] bytes", cnt, bytes);
            if (bytes == -1) {
                break;
            }
            params.qci = getIntPropertyFromConfig("Band" + cnt + "QCI" + dustVer);
            params.maxSpeedDown = getLongPropertyFromConfig("Band" + cnt + "Maxbps" + dustVer);
            params.maxSpeedUp = params.maxSpeedDown;
            params.useCongestion = getBooleanPropertyFromConfig("Band" + cnt + "UseCongestion" + dustVer);
            params.band = cnt;
            FUPEvent = getPropertyFromConfig("Band" + cnt + "FUPEvent" + dustVer);
            if (unitsUsed <= bytes) {
                break;
            }
            cnt++;
        } while (cnt < 20); // failsafe       
        
        
        if(getBooleanPropertyFromConfig("UseDailyFup")) {       
            DailyFupInfo dailyFupInfo=null;  
            try {
                 dailyFupInfo= DAO.getDailyFupStartOfDayUnits(em, getUnitCreditInstance().getAccountId() , getUnitCreditInstance().getUnitCreditSpecificationId());
            } catch (NoResultException nre){
            
            }    

            long remainingUnitsInBundle = getCurrentUnitsLeft().longValue();

            if(dailyFupInfo != null) {  //Entry exists in DB, check if it's time to reset
                Calendar todayMidnight = Calendar.getInstance();
                todayMidnight.set(Calendar.HOUR_OF_DAY, 0);
                todayMidnight.set(Calendar.MINUTE, 0);                
                
                Calendar lastTimeDailyFupWasSet=Calendar.getInstance();
                lastTimeDailyFupWasSet.setTime(dailyFupInfo.getLastModified());
                lastTimeDailyFupWasSet.set(Calendar.HOUR_OF_DAY, 0);
                lastTimeDailyFupWasSet.set(Calendar.MINUTE, 0);
                
                int daysSinceFupInfoWasModified = todayMidnight.get(Calendar.DAY_OF_MONTH) - lastTimeDailyFupWasSet.get(Calendar.DAY_OF_MONTH);                
                
                
                if(daysSinceFupInfoWasModified<0) {  //This will happen when today is 1st of new month
                    daysSinceFupInfoWasModified *= -1;
                }
                
                if(daysSinceFupInfoWasModified>0) { //Was not today so go set start units for today                                          
                    DAO.setDailyFupStartOfDayUnits(em, getUnitCreditInstance().getAccountId() , getUnitCreditInstance().getUnitCreditSpecificationId(), remainingUnitsInBundle);
                } else {
                    long unitsAtStartOfToday = dailyFupInfo.getStartOfDayUnits();
                    long currentDayUsage = unitsAtStartOfToday - remainingUnitsInBundle;
                    long usageUnitsToStartDailyFup = getLongPropertyFromConfig("Band" + params.band + "DailyFupStartBytes" + dustVer);
                    
                    /**
                     * For Unlimited Remaining Units can go up to less than 0
                     * Make it positive to compare with configured units for DailyFup
                     *
                     * */
                     
                    if(currentDayUsage<0) { 
                        currentDayUsage *= -1;
                    }                    
                    
                    if(currentDayUsage >= usageUnitsToStartDailyFup) {
                        //Change Speed to DailyFUPSpeed
                        if(!getPropertyFromConfigBlankIfNull("Band" + params.band + "DailyFupMbps" + dustVer).isEmpty()) {
                           // log.debug("Setting DailyFup Speed now to: [{}] Mbps, accountId [{}], unitCreditId [{}]", getPropertyFromConfigBlankIfNull("Band" + params.band + "DailyFupMbps" + dustVer), getUnitCreditInstance().getAccountId(), getUnitCreditInstance().getUnitCreditSpecificationId());
                            params.maxSpeedDown = getLongPropertyFromConfig("Band" + params.band + "DailyFupMbps" + dustVer);
                            params.maxSpeedUp = params.maxSpeedDown;
                        } 
                    }                          
                }
            } else {
                    DAO.setDailyFupStartOfDayUnits(em, getAccountId() , getUnitCreditSpecification().getUnitCreditSpecificationId().longValue(), remainingUnitsInBundle);
            }

        }

        if (params.maxSpeedDown == -1) {
            throw new Exception("Invalid configuration - no band exists for usage -- " + unitsUsed);
        }
        if (FUPEvent != null) {
            EventHelper.sendUnitCreditEvent(this, FUPEvent);
        }
        log.debug("QCI is [{}] and max bps is [{}] and use congestion is [{}] for [{}] bytes used", new Object[]{params.qci, params.maxSpeedDown, params.useCongestion, unitsUsed});
        return params;
    }

    private int getSectorCongestion(String location) {
        log.debug("Getting sector congestion for [{}]", location);
        int congestion = 0;
        try {
            congestion = DAO.getSectorCongestion(em, location);
        } catch (Exception e) {
            log.warn("Error getting congestion for sectorXXX [{}] ", location);
            //BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "Network Congestion", "No congestion data for sector " + location);
        }
        return congestion;
    }

    private String getNetworkConfig(String param, boolean alertOnMissing) {
        log.debug("Getting network config for [{}]", param);
        String ret = null;
        try {
            ret = DAO.getNetworkConfig(em, param);
        } catch (Exception e) {
            if (alertOnMissing) {
                log.warn("Error getting network parameter [{}] : [{}]", param, e.toString());
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "Network Parameter", "No value for network param " + param + " : " + e.toString());
            } else {
                log.debug("Could not get network parameter [{}] : [{}]", param, e.toString());
            }
        }
        return ret;
    }

    private String getPropertyFromConfigBlankIfNull(String prop) {
        String val = getPropertyFromConfig(prop);
        if (val == null) {
            val = "";
        }
        return val;
    }

    private String overrideDUSTVersionIfNecessary(String dustVer, ServiceInstance serviceInstance, String location) {

        String v1towns = getPropertyFromConfig("Version1Towns");
        String v2towns = getPropertyFromConfig("Version2Towns");
        String v3towns = getPropertyFromConfig("Version3Towns");

        if (v1towns == null && v2towns == null && v3towns == null) {
            return dustVer;
        }

        String sectorTown = Utils.getSectorsTown(location);
        log.debug("Device is in [{}]", sectorTown);

        if (v1towns != null && Utils.getSetFromCommaDelimitedString(v1towns).contains(sectorTown)) {
            dustVer = "V1";
        } else if (v2towns != null && Utils.getSetFromCommaDelimitedString(v2towns).contains(sectorTown)) {
            dustVer = "V2";
        } else if (v3towns != null && Utils.getSetFromCommaDelimitedString(v3towns).contains(sectorTown)) {
            dustVer = "V3";
        }

        return dustVer;

    }

    private boolean mustApplySpeedUpdateOnProvision(int productInstanceId, long accountId) {
        log.debug("in mustApplySpeedUpdateOnProvision to process for product instance [{}] and account [{}]", productInstanceId, accountId);
        //1. First check if there is a reservation on this account
        List<Reservation> resList = DAO.getAccountsReservationList(em, accountId);
        if (resList.isEmpty()) {
            //no need to adjust speeds
            log.debug("No need to adjust speeds as reservation is empty");
            return false;
        }

        log.debug("Reservation list size is [{}]", resList.size());
        UnitCreditInstance dustUCIinReserv = null;
        UnitCreditSpecification dustUCSinReserv = null;
        boolean ignoreBasedOnBeingOfLowerPriority = false;
        boolean productInstanceMatched = false;
        int specId = 0;

        //2. Loop through the account's reservations and make sure none of UCs have a higher priority than this UC's
        for (Reservation res : resList) {
            UnitCreditInstance reservUCI;
            UnitCreditSpecification reservUCS;
            try {
                
                log.debug("Checking reservation with UCI [{}]", res.getReservationPK().getUnitCreditInstanceId());
                if(res.getReservationPK().getUnitCreditInstanceId() <= 0){
                    continue;
                }
                reservUCI = DAO.getUnitCreditInstance(em, res.getReservationPK().getUnitCreditInstanceId());
                reservUCS = DAO.getUnitCreditSpecification(em, reservUCI.getUnitCreditSpecificationId());
            } catch (Exception ex) {
                log.warn("Got an error whilst checking for reservation [{}], for account [{}]", res.getReservationPK().getSessionId(), res.getReservationPK().getAccountId());
                log.warn("Error: ", ex);
                continue;
            }
            specId = reservUCS.getUnitCreditSpecificationId();
            if (reservUCS.getPriority() > ucs.getPriority()
                    && reservUCS.getUnitType().equals(ucs.getUnitType())
                    && reservUCI.getProductInstanceId() == productInstanceId
                    && !reservUCS.getWrapperClass().equalsIgnoreCase("DustUnitCredit")) {
                ignoreBasedOnBeingOfLowerPriority = true;
                break;
            }
            if (reservUCS.getWrapperClass().equals(ucs.getWrapperClass()) && reservUCS.getWrapperClass().equalsIgnoreCase("DustUnitCredit")) {
                dustUCIinReserv = reservUCI;
                dustUCSinReserv = reservUCS;
            }
            if (productInstanceId > 0 && reservUCI.getProductInstanceId() == productInstanceId) {
                productInstanceMatched = true;
            }
        }

        if (ignoreBasedOnBeingOfLowerPriority) {
            log.debug("No need to adjust speeds as my priority is lower than that of ucs [{}]", specId);
            return false;
        }

        //3. If there is no DUST in the reservation dont do speed changes
        if (dustUCIinReserv == null) {
            log.debug("There is no need to adjust speeds as there is no DustUnitCredit being used");
            return false;
        }

        //4. Check if the product instance for this DUST has an active ipcan session
        if (productInstanceId > 0 && !productInstanceMatched) {
            log.debug("The product instance applicable to this Dust has no active ipcan session");
            return false;
        }

        //5. Get size of units used on the Dust in reservation and its MultipleUnitFilterThreshold size
        long unitsUsed = dustUCIinReserv.getUnitsAtStart().longValue() - dustUCIinReserv.getUnitsRemaining().longValue();
        long bytes = UnitCreditManager.getLongPropertyFromConfig(dustUCSinReserv, "Band1EndBytes");

        //6. If units used is less than there is on Band1EndBytes then no speed adjustments required
        if (unitsUsed <= bytes) {
            log.debug("No need to adjust speed as units used still less than Band1EndBytes property");
            return false;
        }

        log.debug("Units used is more than there is on Band1EndBytes we need to adjust speeds");

        return true;
    }

    private void forceSpeedUpdateOnService(int productInstanceId) {
        log.debug("Applying speed adjustments on service");
        //No need to check info value for dustVer
        int qci = getIntPropertyFromConfig("Band1QCI");
        long maxSpeedDown = getLongPropertyFromConfig("Band1Maxbps");
        long maxSpeedUp = maxSpeedDown;
        ServiceInstance serviceInstance = DAO.getDataServiceInstanceForProductInstance(em, productInstanceId);
        log.debug("Applying speed adjustments on service [{}], speed [{}]", serviceInstance.getServiceInstanceId(), maxSpeedUp);
        ServiceRules rules = new ServiceRules();
        rules.setMaxBpsDown(maxSpeedDown);
        rules.setMaxBpsUp(maxSpeedUp);
        rules.setQci(qci);

        ServiceSpecHelper.applyServiceRules(serviceInstance, rules);
    }

}

class DustParams {

    int qci = -1;
    long maxSpeedUp = -1;
    long maxSpeedDown = -1;
    boolean useCongestion = false;
    int band = 1;
    String dpiRules = "";
}
