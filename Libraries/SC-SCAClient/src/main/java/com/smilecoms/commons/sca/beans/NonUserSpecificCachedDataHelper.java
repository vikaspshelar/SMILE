/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.sca.beans;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.AVP;
import com.smilecoms.commons.sca.ProductSpecification;
import com.smilecoms.commons.sca.ProductSpecificationQuery;
import com.smilecoms.commons.sca.RatePlan;
import com.smilecoms.commons.sca.SCAInteger;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceSpecification;
import com.smilecoms.commons.sca.ServiceSpecificationQuery;
import com.smilecoms.commons.sca.StProductSpecificationLookupVerbosity;
import com.smilecoms.commons.sca.StServiceSpecificationLookupVerbosity;
import com.smilecoms.commons.sca.StUnitCreditSpecificationLookupVerbosity;
import com.smilecoms.commons.sca.UnitCreditSpecification;
import com.smilecoms.commons.sca.UnitCreditSpecificationQuery;
import com.smilecoms.commons.base.cache.CacheHelper;
import com.smilecoms.commons.util.Utils;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sabza
 */
public class NonUserSpecificCachedDataHelper {

    /*
    
     THIS MUST USE ADMIN SCA WRAPPER INSTANCE
    
     */
    private static final Logger log = LoggerFactory.getLogger(NonUserSpecificCachedDataHelper.class);

    private static int getLongCacheSecs() {
        try {
            return BaseUtils.getIntProperty("env.static.data.cache.secs");
        } catch (Exception e) {
            log.warn("Property env.static.data.cache.secs does not exist. Defaulting to 60s");
            return 60;
        }
    }

    public static RatePlan getRatePlan(int ratePlanId) {
        RatePlan rate = CacheHelper.getFromLocalCache(ratePlanId, RatePlan.class);
        if (rate == null) {
            SCAInteger id = new SCAInteger();
            id.setInteger(ratePlanId);
            rate = SCAWrapper.getAdminInstance().getRatePlan(id);
            CacheHelper.putInLocalCache(ratePlanId, rate, getLongCacheSecs());
        }
        return rate;
    }

    public static ProductSpecification getProductSpecification(int productSpecificationId) {
        ProductSpecification ps = CacheHelper.getFromLocalCache(productSpecificationId, ProductSpecification.class);
        if (ps == null) {
            ProductSpecificationQuery psq = new ProductSpecificationQuery();
            psq.setProductSpecificationId(productSpecificationId);
            psq.setVerbosity(StProductSpecificationLookupVerbosity.MAIN_PRODAVP_SVC_SVCAVP);
            ps = SCAWrapper.getAdminInstance().getProductSpecifications(psq).getProductSpecifications().get(0);
            CacheHelper.putInLocalCache(productSpecificationId, ps, getLongCacheSecs());
        }
        return ps;
    }

    public static List<ProductSpecification> getAllProductSpecifications() {
        List<ProductSpecification> psList = CacheHelper.getFromLocalCache("allprodspecs", ArrayList.class);
        if (psList == null) {
            log.debug("No Product Specs in Cache");
            ProductSpecificationQuery psq = new ProductSpecificationQuery();
            psq.setProductSpecificationId(-1);
            psq.setVerbosity(StProductSpecificationLookupVerbosity.MAIN_PRODAVP_SVC_SVCAVP);
            psList = SCAWrapper.getAdminInstance().getProductSpecifications(psq).getProductSpecifications();
            CacheHelper.putInLocalCache("allprodspecs", psList, getLongCacheSecs());
        }
        return psList;
    }

    public static List<UnitCreditSpecification> getAllUnitCreditSpecifications() {
        List<UnitCreditSpecification> ucList = CacheHelper.getFromLocalCache("allucspecs", ArrayList.class);
        if (ucList == null) {
            log.debug("No UC Specs in Cache");
            UnitCreditSpecificationQuery ucsq = new UnitCreditSpecificationQuery();
            ucsq.setUnitCreditSpecificationId(-1);
            ucsq.setVerbosity(StUnitCreditSpecificationLookupVerbosity.MAIN_SVCSPECIDS);
            ucList = SCAWrapper.getAdminInstance().getUnitCreditSpecifications(ucsq).getUnitCreditSpecifications();
            CacheHelper.putInLocalCache("allucspecs", ucList, getLongCacheSecs());
        }
        return ucList;
    }

    public static ServiceSpecification getServiceSpecification(int serviceSpecificationId) {
        ServiceSpecification ss = CacheHelper.getFromLocalCache(serviceSpecificationId, ServiceSpecification.class);
        if (ss == null) {
            ServiceSpecificationQuery ssq = new ServiceSpecificationQuery();
            ssq.setServiceSpecificationId(serviceSpecificationId);
            ssq.setVerbosity(StServiceSpecificationLookupVerbosity.MAIN_SVCAVP);
            ss = SCAWrapper.getAdminInstance().getServiceSpecification(ssq);
            CacheHelper.putInLocalCache(serviceSpecificationId, ss, getLongCacheSecs());
        }
        return ss;
    }

    public static UnitCreditSpecification getUnitCreditSpecification(int unitCreditSpecificationId) {
        UnitCreditSpecification ucs = CacheHelper.getFromLocalCache(unitCreditSpecificationId, UnitCreditSpecification.class);
        if (ucs == null) {
            ucs = SCAWrapper.getAdminInstance().getUnitCreditSpecification(unitCreditSpecificationId);
            CacheHelper.putInLocalCache(unitCreditSpecificationId, ucs, getLongCacheSecs());
        }
        return ucs;
    }
    
    public static UnitCreditSpecification getUnitCreditSpecificationByItemNumber(String ItemNumber) {
        UnitCreditSpecification ucs = CacheHelper.getFromLocalCache(ItemNumber, UnitCreditSpecification.class);
        if (ucs == null) {
            ucs = SCAWrapper.getAdminInstance().getUnitCreditSpecification(ItemNumber);
            CacheHelper.putInLocalCache(ItemNumber, ucs, getLongCacheSecs());
        }
        return ucs;
    }

    public static List<String> getServiceSpecificationAvailableUserDefinedDPIRules(int serviceSpecificationId) {
        List<String> dpiRules = new ArrayList();
        if (serviceSpecificationId <= 0) {
            return dpiRules;
        }

        try {
            String key = serviceSpecificationId + "availableUserDefinedDPIRules";
            String rules = CacheHelper.getFromLocalCache(key, String.class);
            if (rules == null) {
                rules = "";
                ServiceSpecification ss = getServiceSpecification(serviceSpecificationId);

                for (AVP ssAvp : ss.getAVPs()) {
                    if (ssAvp.getAttribute().equalsIgnoreCase("AvailableUserDefinedDPIRules")) {
                        rules = ssAvp.getValue();
                        break;
                    }
                }
                CacheHelper.putInLocalCache(key, rules, getLongCacheSecs());
            }
            if (rules.isEmpty()) {
                return dpiRules;
            }
            dpiRules = Utils.getListFromCRDelimitedString(rules);
            return dpiRules;
        } catch (Exception e) {
            log.warn("Error getting service specification AvailableUserDefinedDPIRules: ", e);
            return dpiRules;
        }
    }

}
