/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.unitcredits;

import com.smilecoms.bm.BMDataCache;
import com.smilecoms.bm.unitcredits.wrappers.IUnitCredit;
import com.smilecoms.bm.unitcredits.filters.IUCFilterClass;
import com.smilecoms.bm.charging.IAccount;
import com.smilecoms.bm.charging.InsufficientFundsException;
import com.smilecoms.bm.db.model.ServiceInstance;
import com.smilecoms.bm.db.model.UnitCreditInstance;
import com.smilecoms.bm.db.model.UnitCreditSpecification;
import com.smilecoms.bm.db.op.DAO;
import com.smilecoms.bm.rating.RatingResult;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.persistence.EntityManager;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class UnitCreditManager {

    private static final Logger log = LoggerFactory.getLogger(UnitCreditManager.class);

    public static List<IUnitCredit> getApplicableUnitCredits(
            EntityManager em,
            IAccount acc,
            String sessionId,
            ServiceInstance serviceInstance,
            RatingResult ratingResult,
            RatingKey ratingKey,
            String srcDevice,
            String description,
            Date eventTimestamp,
            String unitType,
            String location,
            BigDecimal usedUnits) throws Exception {

        //Check if the account can use unit credits
        if (acc.unitCreditChargingBarred()) {
            return new ArrayList<>();
        }

        List<IUnitCredit> unitCreditsToReturn = new ArrayList<>();

        putInUCIBasedUnitCredits(unitCreditsToReturn, em, acc, sessionId, serviceInstance, ratingResult, ratingKey, srcDevice, description, eventTimestamp, unitType, location, usedUnits);

        //putInSomeOtherUCIBasedUnitCredits
        sort(unitCreditsToReturn);

        return unitCreditsToReturn;
    }

    private static void sort(List<IUnitCredit> unsortedList) {
        if (unsortedList.size() <= 1) {
            log.debug("No need to sort the UCI list as it has 0 or 1 items");
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Unit Credit list prior to sorting:");
            for (IUnitCredit uc : unsortedList) {
                log.debug("UC: [{}]", uc);
            }
        }
        Collections.sort(unsortedList, new UCComparator());
        if (log.isDebugEnabled()) {
            log.debug("Unit Credit list after sorting:");
            for (IUnitCredit uc : unsortedList) {
                log.debug("UC: [{}]", uc);
            }
        }
    }

    private static List<IUnitCredit> putInUCIBasedUnitCredits(
            List<IUnitCredit> unitCreditsToReturn,
            EntityManager em,
            IAccount acc,
            String sessionId,
            ServiceInstance serviceInstance,
            RatingResult ratingResult,
            RatingKey ratingKey,
            String srcDevice,
            String description,
            Date eventTimestamp,
            String unitType,
            String location,
            BigDecimal usedUnits) {

        log.debug("Going to check what UCI based unit credits can apply to this service");
        // These will be in order of purchase date
        List<UnitCreditInstance> unitCreditInstances = DAO.getApplicableUnitCreditsBasedOnProductAndServiceOnly(em, acc, serviceInstance.getProductSpecificationId(), serviceInstance.getServiceInstanceId());

        for (UnitCreditInstance uci : unitCreditInstances) {

            UnitCreditSpecification ucs = DAO.getUnitCreditSpecification(em, uci.getUnitCreditSpecificationId());

            if (unitType == null) {
                throw new RuntimeException("Invalid unit type null in request. Cannot get applicable unit credits -- null");
            }

            // Check that the bundle is allowed based on the unit type
            boolean unitTypeIsOK = false;

            if (unitType.equals("OCTET")
                    && ucs.getUnitType().equals("Byte")) {
                log.debug("Charging for bytes and the UC is in bytes");
                unitTypeIsOK = true;

                if (BaseUtils.getBooleanProperty("env.bm.social.media.tax.logic.enabled", false)) {
                    /* As discussed with Cathy - For UGanda -  if we are charging for Unlimited Social media, only charge
                       the social media component on Unlimited bundles that are still positive and can cover the full charge.  
                     */
                    if (ratingKey.getServiceCode() != null && ratingKey.getServiceCode().equals("txtype.socialmedia.tax.unlimited")) {
                        // Only allow this bundle if it is an unlimited and has sufficient positive balance.
                        if (usedUnits != null && uci.getUnitsRemaining().compareTo(usedUnits) >= 0
                                && ucs.getWrapperClass().equals("DustUnitCredit")) {
                            //Unlimited bundle has enough remaining units to cover for social media tax.
                            unitTypeIsOK = true;
                        } else {
                            unitTypeIsOK = false;
                        }
                    }
                }
            } else if (unitType.equals("SECOND")
                    && ucs.getUnitType().equals("Byte")
                    && ucs.getWrapperClass().contains("BaselinedAsData")) {
                log.debug("Charging for seconds and the UC is in bytes and is baselined as data so voice as data is allowed");
                unitTypeIsOK = true;

            } else if (unitType.equals("SECOND")
                    && ucs.getUnitType().equals("Cent")
                    && DAO.isPositive(ratingResult.getRetailRateCentsPerUnit())) {
                log.debug("This is voice against a monetary unit credit and the price is non zero");
                unitTypeIsOK = true;

            } else if (unitType.equals("SMS")
                    && ucs.getUnitType().equals("Cent")
                    && DAO.isPositive(ratingResult.getRetailRateCentsPerUnit())) {
                log.debug("This is SMS against a monetary unit credit and the price is non zero");
                unitTypeIsOK = true;

            } else if (unitType.equals("SMS")
                    && ucs.getUnitType().equals("SMS")
                    && DAO.isPositive(ratingResult.getRetailRateCentsPerUnit())
                    && ratingKey.getLeg().equals("O")) {
                log.debug("This is SMS against a monetary unit credit and the price is non zero");
                unitTypeIsOK = true;

            } else if (unitType.equals("SMS")
                    && ucs.getUnitType().equals("Byte")
                    && ucs.getWrapperClass().contains("BaselinedAsData")) {
                log.debug("Charging for SMS and the UC is in bytes and is baselined as data so SMS as data is allowed");
                unitTypeIsOK = true;

            } else if (unitType.equals("SECOND")
                    && ucs.getUnitType().equals("Second")
                    && ratingKey.getLeg().equals("O")) {
                log.debug("Charging for Voice and the UC is in seconds so allowed");
                unitTypeIsOK = true;
            } else if (unitType.equals("SMTAX") // SMTAX = Social Media Tax and only allowed to be charged on baselined unit credits.
                    && ucs.getUnitType().equals("Byte")
                    && ucs.getWrapperClass().contains("BaselinedAsData")) {
                log.debug("Charging for Social Media Tax and the UC is baselined at data so is allowed");
                unitTypeIsOK = true;
            }

            if (!unitTypeIsOK) {
                log.debug("This UC cannot apply due to unit type or leg mismatch. UC Type [{}] Request Type [{}] Cents Per Unit [{}]",
                        new Object[]{ucs.getUnitType(), unitType, ratingResult.getRetailRateCentsPerUnit()});
                continue;
            }

            try {
                String wrapperName = getFullWrapperName(ucs.getWrapperClass());
                IUCFilterClass filterClass = (IUCFilterClass) UnitCreditManager.class.getClassLoader().loadClass(getFullFilterName(ucs.getFilterClass())).newInstance();
                if (filterClass.isUCApplicable(
                        em,
                        acc,
                        sessionId,
                        serviceInstance,
                        ratingResult,
                        ratingKey,
                        srcDevice,
                        description,
                        eventTimestamp,
                        ucs,
                        uci,
                        location)) {

                    log.debug("[{}] says unit credit instance [{}] does apply. Wrapper class is [{}]", new Object[]{wrapperName, uci.getUnitCreditInstanceId(), wrapperName});
                    unitCreditsToReturn.add(getWrappedUnitCreditInstance(em, uci, unitCreditsToReturn));
                } else {
                    log.debug("[{}] says that Unit Credit does not apply. Instance Id [{}]", wrapperName, uci.getUnitCreditInstanceId());
                }
            } catch (Exception ex) {
                log.error("Error trying to see if this unit credit applies:", ex);
            }
        }

        log.debug("Doing pass 2");
        List<IUnitCredit> unitCreditsToReturnPass2 = new ArrayList();

        for (IUnitCredit ucPass2 : unitCreditsToReturn) {
            try {
                UnitCreditSpecification ucs = ucPass2.getUnitCreditSpecification();
                String wrapperName = getFullWrapperName(ucs.getWrapperClass());
                IUCFilterClass filterClass = (IUCFilterClass) UnitCreditManager.class.getClassLoader().loadClass(getFullFilterName(ucs.getFilterClass())).newInstance();
                if (filterClass.isUCApplicableInContext(
                        em,
                        acc,
                        sessionId,
                        serviceInstance,
                        ratingResult,
                        ratingKey,
                        srcDevice,
                        description,
                        eventTimestamp,
                        ucs,
                        ucPass2,
                        unitCreditsToReturn,
                        location)) {

                    log.debug("[{}] says unit credit instance [{}] does apply after pass 2. Wrapper class is [{}]", new Object[]{wrapperName, ucPass2.getUnitCreditInstanceId(), wrapperName});
                    unitCreditsToReturnPass2.add(ucPass2);
                } else {
                    log.debug("[{}] says that Unit Credit does not apply after pass 2. Instance Id [{}]", wrapperName, ucPass2.getUnitCreditInstanceId());
                }
            } catch (Exception ex) {
                log.error("Error trying to see if this unit credit applies in pass 2:", ex);
            }
        }

        unitCreditsToReturn.clear();

        unitCreditsToReturn.addAll(unitCreditsToReturnPass2);

        if (log.isDebugEnabled()) {
            log.debug("[{}] UCI based unit credits match", unitCreditsToReturn.size());
        }

        return unitCreditsToReturn;
    }

    public static IUnitCredit getWrappedUnitCreditInstance(EntityManager em, UnitCreditInstance uci, List<IUnitCredit> listUCIsIn) throws Exception {
        UnitCreditSpecification ucs = DAO.getUnitCreditSpecification(em, uci.getUnitCreditSpecificationId());
        String wrapper = getFullWrapperName(ucs.getWrapperClass());
        IUnitCredit uc = (IUnitCredit) UnitCreditManager.class.getClassLoader().loadClass(wrapper).newInstance();
        uc.initialise(em, uci, ucs, listUCIsIn);
        log.debug("UCI with Id [{}] initialised with wrapper class [{}]", uci.getUnitCreditInstanceId(), wrapper);
        return uc;
    }

    public static IUnitCredit getLockedWrappedUnitCreditInstance(EntityManager em, int unitCreditInstanceId, List<IUnitCredit> listUCIsIn) throws Exception {
        UnitCreditInstance uci = DAO.getLockedUnitCreditInstance(em, unitCreditInstanceId);
        return getWrappedUnitCreditInstance(em, uci, listUCIsIn);
    }

    private static class UCComparator implements Comparator {

        @Override
        public int compare(Object o1, Object o2) {
            // Compare sorts from smalles to largest so negate to sort from highest priority to lowest
            Integer uci1 = -1 * ((IUnitCredit) o1).getPriority();
            Integer uci2 = -1 * ((IUnitCredit) o2).getPriority();
            return uci1.compareTo(uci2);
        }
    }

    public static String getPropertyFromConfig(UnitCreditSpecification ucs, String propName) {

        Map<String, String> config = BMDataCache.unitCreditConfigCacheBySpecId.get(ucs.getUnitCreditSpecificationId());
        if (config == null) {
            log.debug("No UC config cache for id [{}]. Going to populate", ucs.getUnitCreditSpecificationId());
            config = new HashMap<>();
            StringTokenizer stValues = new StringTokenizer(ucs.getConfiguration(), "\r\n");
            while (stValues.hasMoreTokens()) {
                String row = stValues.nextToken();
                if (!row.isEmpty()) {
                    String[] bits = row.split("=");
                    String name = bits[0].trim();
                    String value;
                    if (bits.length == 1) {
                        value = "";
                    } else {
                        value = bits[1].trim();
                    }
                    config.put(name, value);
                }
            }
            BMDataCache.unitCreditConfigCacheBySpecId.put(ucs.getUnitCreditSpecificationId(), config);
        }
        return config.get(propName);
    }

    public static int getIntPropertyFromConfig(UnitCreditSpecification ucs, String propName) {
        String val = getPropertyFromConfig(ucs, propName);
        if (val == null) {
            return -1;
        }
        return Integer.parseInt(val);
    }

    public static long getLongPropertyFromConfig(UnitCreditSpecification ucs, String propName) {
        String val = getPropertyFromConfig(ucs, propName);
        if (val == null) {
            return -1;
        }
        return Long.parseLong(val);
    }

    public static double getDoublePropertyFromConfig(UnitCreditSpecification ucs, String propName) {
        String val = getPropertyFromConfig(ucs, propName);
        if (val == null) {
            return -1;
        }
        return Double.parseDouble(val);
    }

    public static BigDecimal getBigDecimalPropertyFromConfigZeroIfMissing(UnitCreditSpecification ucs, String propName) {
        String val = getPropertyFromConfig(ucs, propName);
        if (val == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(Double.parseDouble(val));
    }

    public static BigDecimal getBigDecimalPropertyFromConfig(UnitCreditSpecification ucs, String propName) {
        return BigDecimal.valueOf(getDoublePropertyFromConfig(ucs, propName));
    }

    public static boolean getBooleanPropertyFromConfig(UnitCreditSpecification ucs, String propName) {
        String val = getPropertyFromConfig(ucs, propName);
        if (val == null) {
            return false;
        }
        return val.equalsIgnoreCase("true");
    }

    public static IUnitCredit provisionUnitCredit(EntityManager em, int specId, int productInstanceId, IAccount acc,
            int numberToProvision, int daysGapBetweenStart, boolean verifyOnly, String extTxid, double posCentsPaidEach, double posCentsDiscountEach,
            Date startDate, int saleLineId, String info) throws Exception {
        IUnitCredit uc = null;

        if (productInstanceId > 0) {
            log.debug("Verifying the product instance id is related to the account");
            List<ServiceInstance> accountsSIs = DAO.getServiceInstancesForAccount(em, acc.getAccountId());
            boolean found = false;
            for (ServiceInstance si : accountsSIs) {
                if (si.getProductInstanceId() == productInstanceId) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new Exception("Account does not have the product instance under it -- Account:" + acc.getAccountId() + " Product Instance:" + productInstanceId);
            }
        }

        for (int i = 0; i < numberToProvision; i++) {
            uc = UnitCreditManager.provisionUnitCredit(
                    em,
                    specId,
                    productInstanceId,
                    acc,
                    startDate,
                    verifyOnly,
                    extTxid,
                    posCentsPaidEach,
                    posCentsDiscountEach,
                    saleLineId,
                    info);
            // Next one starts when last one expires or X days after last one starts
            if (numberToProvision > 1) {
                if (daysGapBetweenStart < 0) {
                    startDate = uc.getExpiryDate();
                } else {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(uc.getStartDate());
                    cal.add(Calendar.DATE, daysGapBetweenStart);
                    startDate = cal.getTime();
                }
            }
        }
        return uc;
    }

    public static String getFullWrapperName(String wrapper) {
        return "com.smilecoms.bm.unitcredits.wrappers." + wrapper;
    }

    public static String getFullFilterName(String filter) {
        return "com.smilecoms.bm.unitcredits.filters." + filter;
    }

    public static void splitUnitCredit(EntityManager em, int unitCreditInstanceId, long targetAccountId, double units, int targetProductInstanceId) throws Exception {
        IUnitCredit uc = getLockedWrappedUnitCreditInstance(em, unitCreditInstanceId, null);
        try {
            uc.split(targetAccountId, new BigDecimal(units), targetProductInstanceId);
        } catch (InsufficientFundsException isf) {
            throw new InsufficientFundsException();
        }
    }

    public static IUnitCredit provisionUnitCredit(EntityManager em, int specId, int productInstanceId, IAccount acc,
            Date startDate, boolean verifyOnly, String extTxid, double posCentsPaidEach, double posCentsDiscountEach, int saleLineId, String info) throws Exception {

        UnitCreditSpecification ucs = DAO.getUnitCreditSpecification(em, specId);
        if (!Utils.isBetween(new Date(), ucs.getAvailableFrom(), ucs.getAvailableTo())) {
            throw new Exception("Unit credit is not available for provisioning");
        }

        IUnitCredit uc = (IUnitCredit) UnitCreditManager.class.getClassLoader().loadClass(getFullWrapperName(ucs.getWrapperClass())).newInstance();
        uc.initialise(em, null, ucs, null);
        boolean allowedSharing = uc.getBooleanPropertyFromConfig("AllowSharing");
        if (productInstanceId == 0 && !allowedSharing) {
            log.debug("Unit credits that dont allow sharing must have a non zero product instance Id. We will default to the first applicable product on this account");
            int piId = DAO.getLastActiveOrTempDeactiveProductInstanceIdForAccountAndUnitCreditSpec(em, acc.getAccountId(), specId);
            if (piId == -1) {
                throw new Exception("Unit credits that dont allow sharing must have a non zero product instance Id");
            }
            productInstanceId = piId;
        } else if (allowedSharing) {
            log.debug("Bundle sharing is allowed so service instance id is being set to 0");
            productInstanceId = 0;
        }
        if (uc.getBooleanPropertyFromConfig("StartOnBeginningOfDay")) {
            log.debug("UCS config says the start date must be the beginning of the start date");
            if (startDate == null) {
                startDate = new Date();
            }
            startDate = Utils.getBeginningOfDay(startDate);
        }

        if (BaseUtils.getBooleanProperty("env.bm.provisions.check.enabled", false)) {
            if (UCCanBeSoldWhenSpecIDExist(uc)) {
                String allowedSpecIds = uc.getPropertyFromConfig("CanBeSoldWhenSpecIDExist");

                if (!(allowedSpecIds == null || allowedSpecIds.length() <= 0)) { //If we have SpecIDs that must exist in user profile

                    //Need To check if this account has the main specid required to provision unitcredit                
                    List<IUnitCredit> clientExistingUnitCreditCredInstances = getAccountsActiveUnitCredits(em, acc);
                    List<String> specIDsForUpsize = Utils.getListFromCommaDelimitedString(allowedSpecIds);
                    boolean mustExistSpecIdExists = false;

                    for (String mustExistSpecId : specIDsForUpsize) {
                        for (IUnitCredit clientUnitCred : clientExistingUnitCreditCredInstances) {
                            if (clientUnitCred.getUnitCreditSpecification().getUnitCreditSpecificationId() == Integer.parseInt(mustExistSpecId)) {
                                mustExistSpecIdExists = true;
                                break;
                            }
                        }

                        if (mustExistSpecIdExists) {
                            break;
                        }
                    }

                    if (!mustExistSpecIdExists) {
                        log.warn("Error: Requested to provision add-on product, without main product on account. Requires one of the following SpecIDs on acc: ", specIDsForUpsize);
                        throw new Exception("Missing main product on account. Cannot provision add-on product without main product on account.");
                    }
                }
            }
        }

        try {
            uc.provision(acc, productInstanceId, startDate, verifyOnly, extTxid, posCentsPaidEach, posCentsDiscountEach, saleLineId, info);
        } catch (InsufficientFundsException e) {
            throw new InsufficientFundsException();
        }

        return uc;
    }

    public static List<IUnitCredit> getAccountsActiveUnitCredits(EntityManager em, IAccount acc) throws Exception {
        List<IUnitCredit> unitCreditsToReturn = new ArrayList<>();
        List<UnitCreditInstance> unitCreditInstances = DAO.getAccountsActiveUnitCredits(em, acc);
        for (UnitCreditInstance uci : unitCreditInstances) {
            unitCreditsToReturn.add(getWrappedUnitCreditInstance(em, uci, unitCreditsToReturn));
        }
        return unitCreditsToReturn;
    }

    private static boolean UCCanBeSoldWhenSpecIDExist(IUnitCredit ucspec) {
        String val = ucspec.getPropertyFromConfig("CanBeSoldWhenSpecIDExist");
        return !(val == null || val.length() <= 0);
    }
}
