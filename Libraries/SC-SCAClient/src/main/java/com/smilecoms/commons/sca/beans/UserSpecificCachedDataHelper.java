/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.sca.beans;

import com.smilecoms.commons.sca.AVP;
import com.smilecoms.commons.sca.Account;
import com.smilecoms.commons.sca.AccountQuery;
import com.smilecoms.commons.sca.CallersRequestContext;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.CustomerQuery;
import com.smilecoms.commons.sca.Organisation;
import com.smilecoms.commons.sca.OrganisationQuery;
import com.smilecoms.commons.sca.ProductInstance;
import com.smilecoms.commons.sca.ProductInstanceQuery;
import com.smilecoms.commons.sca.ProductServiceInstanceMapping;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.ServiceInstanceQuery;
import com.smilecoms.commons.sca.StAccountLookupVerbosity;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.StOrganisationLookupVerbosity;
import com.smilecoms.commons.sca.StProductInstanceLookupVerbosity;
import com.smilecoms.commons.sca.StServiceInstanceLookupVerbosity;
import com.smilecoms.commons.base.cache.CacheHelper;
import com.smilecoms.commons.util.Utils;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class UserSpecificCachedDataHelper {

    /*
    
    
     ALWAYS USE USER SPECIFIC SCA WRAPPER IN HERE!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    
     ALSO MAKE SURE THE CACHE IS USERNAME KEYED!!!!!!!!!!!!!!!!!!!!!
    
    
     */
    private static final Logger log = LoggerFactory.getLogger(UserSpecificCachedDataHelper.class);
    private static final int SHORT_CACHE_SECS = 2;

    private static void checkContext() throws IllegalStateException {
        CallersRequestContext ctx = SCAWrapper.getUserSpecificInstance().getThreadsRequestContext();
        if (ctx == null) {
            throw new IllegalStateException("Cannot use UserSpecificCachedDataHelper without setting the callers context");
        }
    }

    public static Account getAccount(long accountId) {
        return getAccount(accountId, StAccountLookupVerbosity.ACCOUNT);
    }

    public static Account getAccount(long accountId, StAccountLookupVerbosity verbosity) {
        checkContext();
        String keyPrefix = SCAWrapper.getUserSpecificInstance().getThreadsRequestContext().getRemoteUser() + "acc" + accountId;
        Account acc = CacheHelper.getFromLocalCache(keyPrefix + verbosity, Account.class);
        if (acc == null) {
            log.debug("Account [{}] with Verbosity [{}] not found in local cache", accountId, verbosity);
            AccountQuery aq = new AccountQuery();
            aq.setAccountId((long) accountId);
            aq.setVerbosity(verbosity);
            acc = SCAWrapper.getUserSpecificInstance().getAccount(aq);
            switch (verbosity) {
                case ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES:
                    CacheHelper.putInLocalCache(keyPrefix + StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES, acc, SHORT_CACHE_SECS);
                case ACCOUNT_UNITCREDITS_RESERVATIONS:
                    CacheHelper.putInLocalCache(keyPrefix + StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS, acc, SHORT_CACHE_SECS);
                case ACCOUNT_UNITCREDITS:
                    CacheHelper.putInLocalCache(keyPrefix + StAccountLookupVerbosity.ACCOUNT_UNITCREDITS, acc, SHORT_CACHE_SECS);
                case ACCOUNT:
                    CacheHelper.putInLocalCache(keyPrefix + StAccountLookupVerbosity.ACCOUNT, acc, SHORT_CACHE_SECS);
            }
        } else {
            log.debug("Account [{}] with Verbosity [{}] was found in local cache", accountId, verbosity);
        }
        return acc;
    }

    public static Organisation getOrganisation(int orgId, StOrganisationLookupVerbosity verbosity) {
        checkContext();
        String keyPrefix = SCAWrapper.getUserSpecificInstance().getThreadsRequestContext().getRemoteUser() + "org" + orgId;
        Organisation org = CacheHelper.getFromLocalCache(keyPrefix + verbosity, Organisation.class);
        if (org == null) {
            log.debug("Organisation [{}] with Verbosity [{}] not found in local cache", orgId, verbosity);
            OrganisationQuery oq = new OrganisationQuery();
            oq.setOrganisationId(orgId);
            oq.setVerbosity(verbosity);
            org = SCAWrapper.getUserSpecificInstance().getOrganisation(oq);
            switch (verbosity) {
                case MAIN_PHOTO:
                    CacheHelper.putInLocalCache(keyPrefix + StOrganisationLookupVerbosity.MAIN_PHOTO, org, SHORT_CACHE_SECS);
                    CacheHelper.putInLocalCache(keyPrefix + StOrganisationLookupVerbosity.MAIN, org, SHORT_CACHE_SECS);
                    break;
                case MAIN_ROLES:
                    CacheHelper.putInLocalCache(keyPrefix + StOrganisationLookupVerbosity.MAIN_ROLES, org, SHORT_CACHE_SECS);
                case MAIN:
                    CacheHelper.putInLocalCache(keyPrefix + StOrganisationLookupVerbosity.MAIN, org, SHORT_CACHE_SECS);
            }
        } else {
            log.debug("Organisation [{}] with Verbosity [{}] was found in local cache", orgId, verbosity);
        }
        return org;
    }

    public static ProductInstance getProductInstance(int productInstanceId, StProductInstanceLookupVerbosity verbosity) {
        checkContext();
        String keyPrefix = SCAWrapper.getUserSpecificInstance().getThreadsRequestContext().getRemoteUser() + "pi" + productInstanceId;
        ProductInstance pi = CacheHelper.getFromLocalCache(keyPrefix + verbosity, ProductInstance.class);
        if (pi == null) {
            log.debug("PI [{}] with Verbosity [{}] not found in local cache", productInstanceId, verbosity);
            ProductInstanceQuery q = new ProductInstanceQuery();
            q.setProductInstanceId(productInstanceId);
            q.setVerbosity(verbosity);
            pi = SCAWrapper.getUserSpecificInstance().getProductInstance(q);
            switch (verbosity) {
                case MAIN_CAMPAIGNS_CAMPAIGNUC:
                    CacheHelper.putInLocalCache(keyPrefix + StProductInstanceLookupVerbosity.MAIN_CAMPAIGNS_CAMPAIGNUC, pi, SHORT_CACHE_SECS);
                case MAIN_SVC_SVCAVP:
                    CacheHelper.putInLocalCache(keyPrefix + StProductInstanceLookupVerbosity.MAIN_SVC_SVCAVP, pi, SHORT_CACHE_SECS);
                case MAIN_SVC:
                    CacheHelper.putInLocalCache(keyPrefix + StProductInstanceLookupVerbosity.MAIN_SVC, pi, SHORT_CACHE_SECS);
                case MAIN:
                    CacheHelper.putInLocalCache(keyPrefix + StProductInstanceLookupVerbosity.MAIN, pi, SHORT_CACHE_SECS);
            }
            for (ProductServiceInstanceMapping mapping : pi.getProductServiceInstanceMappings()) {
                ServiceInstance si = mapping.getServiceInstance();
                String siKeyPrefix = SCAWrapper.getUserSpecificInstance().getThreadsRequestContext().getRemoteUser() + "si" + si.getServiceInstanceId();
                switch (verbosity) {
                    case MAIN_SVC_SVCAVP:
                        CacheHelper.putInLocalCache(siKeyPrefix + StServiceInstanceLookupVerbosity.MAIN_SVCAVP, si, SHORT_CACHE_SECS);
                        CacheHelper.putInLocalCache(siKeyPrefix + StServiceInstanceLookupVerbosity.MAIN_SVCAVP_STATIC_ONLY, si, SHORT_CACHE_SECS);
                    case MAIN_SVC:
                        CacheHelper.putInLocalCache(siKeyPrefix + StServiceInstanceLookupVerbosity.MAIN, si, SHORT_CACHE_SECS);
                }
            }
        } else {
            log.debug("PI [{}] with Verbosity [{}] was found in local cache", productInstanceId, verbosity);
        }
        return pi;
    }

    public static ServiceInstance getServiceInstance(int serviceInstanceId, StServiceInstanceLookupVerbosity verbosity) {
        checkContext();
        String keyPrefix = SCAWrapper.getUserSpecificInstance().getThreadsRequestContext().getRemoteUser() + "si" + serviceInstanceId;
        ServiceInstance si = CacheHelper.getFromLocalCache(keyPrefix + verbosity, ServiceInstance.class);
        if (si == null) {
            log.debug("SI [{}] with Verbosity [{}] not found in local cache", serviceInstanceId, verbosity);
            ServiceInstanceQuery siq = new ServiceInstanceQuery();
            siq.setServiceInstanceId(serviceInstanceId);
            siq.setVerbosity(verbosity);
            si = SCAWrapper.getUserSpecificInstance().getServiceInstance(siq);
            switch (verbosity) {
                case MAIN_SVCAVP_MAPPINGS:
                    CacheHelper.putInLocalCache(keyPrefix + StServiceInstanceLookupVerbosity.MAIN_SVCAVP_MAPPINGS, si, SHORT_CACHE_SECS);
                case MAIN_SVCAVP:
                    CacheHelper.putInLocalCache(keyPrefix + StServiceInstanceLookupVerbosity.MAIN_SVCAVP, si, SHORT_CACHE_SECS);
                case MAIN_SVCAVP_STATIC_ONLY:
                    CacheHelper.putInLocalCache(keyPrefix + StServiceInstanceLookupVerbosity.MAIN_SVCAVP_STATIC_ONLY, si, SHORT_CACHE_SECS);
                case MAIN:
                    CacheHelper.putInLocalCache(keyPrefix + StServiceInstanceLookupVerbosity.MAIN, si, SHORT_CACHE_SECS);
            }
        } else {
            log.debug("SI [{}] with Verbosity [{}] was found in local cache", serviceInstanceId, verbosity);
        }
        return si;
    }

    public static Customer getCustomer(int customerId, StCustomerLookupVerbosity verbosity) {
        return getCustomer(customerId, verbosity, 0, 0); // 0 will return all
    }

    public static Customer getCustomer(int customerId, StCustomerLookupVerbosity verbosity, int productInstanceOffset, int resultLimit) {
        checkContext();
        String keyPrefix = SCAWrapper.getUserSpecificInstance().getThreadsRequestContext().getRemoteUser() + "cust" + customerId + "-" + productInstanceOffset + "-" + resultLimit;
        Customer cust = CacheHelper.getFromLocalCache(keyPrefix + verbosity, Customer.class);
        if (cust == null) {
            log.debug("Customer [{}] with Verbosity [{}] not found in local cache", customerId, verbosity);
            CustomerQuery cq = new CustomerQuery();
            cq.setCustomerId(customerId);
            cq.setVerbosity(verbosity);
            cq.setProductInstanceOffset(productInstanceOffset);
            cq.setProductInstanceResultLimit(resultLimit);
            cust = SCAWrapper.getUserSpecificInstance().getCustomer(cq);
            switch (verbosity) {
                case CUSTOMER_ADDRESS_PRODUCTS_SERVICES_SVCAVP:
                    CacheHelper.putInLocalCache(keyPrefix + StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES_SVCAVP, cust, SHORT_CACHE_SECS);
                case CUSTOMER_ADDRESS_PRODUCTS_SERVICES:
                    CacheHelper.putInLocalCache(keyPrefix + StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES, cust, SHORT_CACHE_SECS);
                case CUSTOMER_ADDRESS:
                    CacheHelper.putInLocalCache(keyPrefix + StCustomerLookupVerbosity.CUSTOMER_ADDRESS, cust, SHORT_CACHE_SECS);
                case CUSTOMER:
                    CacheHelper.putInLocalCache(keyPrefix + StCustomerLookupVerbosity.CUSTOMER, cust, SHORT_CACHE_SECS);
                    break;
                case CUSTOMER_ADDRESS_PRODUCTS:
                    CacheHelper.putInLocalCache(keyPrefix + StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS, cust, SHORT_CACHE_SECS);
                    CacheHelper.putInLocalCache(keyPrefix + StCustomerLookupVerbosity.CUSTOMER_ADDRESS, cust, SHORT_CACHE_SECS);
                    CacheHelper.putInLocalCache(keyPrefix + StCustomerLookupVerbosity.CUSTOMER, cust, SHORT_CACHE_SECS);
                case CUSTOMER_PHOTO_ADDRESS_MANDATEKYCFIELD:
                    CacheHelper.putInLocalCache(keyPrefix + StCustomerLookupVerbosity.CUSTOMER_PHOTO_ADDRESS_MANDATEKYCFIELD, cust, SHORT_CACHE_SECS);
            }

            for (ProductInstance pi : cust.getProductInstances()) {
                String piKeyPrefix = SCAWrapper.getUserSpecificInstance().getThreadsRequestContext().getRemoteUser() + "pi" + pi.getProductInstanceId();
                switch (verbosity) {
                    case CUSTOMER_ADDRESS_PRODUCTS_SERVICES_SVCAVP:
                        CacheHelper.putInLocalCache(piKeyPrefix + StProductInstanceLookupVerbosity.MAIN_SVC_SVCAVP, pi, SHORT_CACHE_SECS);
                    case CUSTOMER_ADDRESS_PRODUCTS_SERVICES:
                        CacheHelper.putInLocalCache(piKeyPrefix + StProductInstanceLookupVerbosity.MAIN_SVC, pi, SHORT_CACHE_SECS);
                    case CUSTOMER_ADDRESS_PRODUCTS:
                        CacheHelper.putInLocalCache(piKeyPrefix + StProductInstanceLookupVerbosity.MAIN, pi, SHORT_CACHE_SECS);
                }
                CacheHelper.putInLocalCache(SCAWrapper.getUserSpecificInstance().getThreadsRequestContext().getRemoteUser() + "pi" + pi.getProductInstanceId() + StProductInstanceLookupVerbosity.MAIN, pi, SHORT_CACHE_SECS);
                for (ProductServiceInstanceMapping mapping : pi.getProductServiceInstanceMappings()) {
                    ServiceInstance si = mapping.getServiceInstance();
                    String siKeyPrefix = SCAWrapper.getUserSpecificInstance().getThreadsRequestContext().getRemoteUser() + "si" + si.getServiceInstanceId();
                    switch (verbosity) {
                        case CUSTOMER_ADDRESS_PRODUCTS_SERVICES_SVCAVP:
                            CacheHelper.putInLocalCache(siKeyPrefix + StServiceInstanceLookupVerbosity.MAIN_SVCAVP, si, SHORT_CACHE_SECS);
                            CacheHelper.putInLocalCache(siKeyPrefix + StServiceInstanceLookupVerbosity.MAIN_SVCAVP_STATIC_ONLY, si, SHORT_CACHE_SECS);
                        case CUSTOMER_ADDRESS_PRODUCTS_SERVICES:
                            CacheHelper.putInLocalCache(siKeyPrefix + StServiceInstanceLookupVerbosity.MAIN, si, SHORT_CACHE_SECS);
                    }
                }
            }
        } else {
            log.debug("Customer [{}] with Verbosity [{}] was found in local cache", customerId, verbosity);
        }
        log.debug("customer kyc:[{}]",cust.getKYCStatus());
        return cust;
    }

    private static List<ServiceInstance> getServiceInstanceListWithAVPsForProductInstanceId(int productInstanceId) {
        checkContext();
        String key = SCAWrapper.getUserSpecificInstance().getThreadsRequestContext().getRemoteUser() + "pisilist" + productInstanceId;
        List<ServiceInstance> siList = CacheHelper.getFromLocalCache(key, List.class);
        if (siList == null) {
            siList = new ArrayList();
            ProductInstance pi = getProductInstance(productInstanceId, StProductInstanceLookupVerbosity.MAIN_SVC_SVCAVP);
            for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
                siList.add(m.getServiceInstance());
            }
            CacheHelper.putInLocalCache(key, siList, SHORT_CACHE_SECS);
        }
        return siList;
    }

    public static ServiceInstance getSIMServiceInstanceForProductInstanceId(int productInstanceId) {
        List<ServiceInstance> siList = getServiceInstanceListWithAVPsForProductInstanceId(productInstanceId);
        for (ServiceInstance si : siList) {
            for (AVP avp : si.getAVPs()) {
                if (avp.getAttribute().equals("ProvisioningFlow") && avp.getValue().equals("lte")) {
                    return si;
                }
            }
        }
        return null;
    }

    public static ServiceInstance getVoiceServiceInstanceForProductInstanceId(int productInstanceId) {
        List<ServiceInstance> siList = getServiceInstanceListWithAVPsForProductInstanceId(productInstanceId);
        for (ServiceInstance si : siList) {
            for (AVP avp : si.getAVPs()) {
                if (avp.getAttribute().equals("ProvisioningFlow") && avp.getValue().equals("voice")) {
                    return si;
                }
            }
        }
        return null;
    }

    public static int getPrimaryAccountHolderCustomerId(long accountId) {
        checkContext();
        String key = SCAWrapper.getUserSpecificInstance().getThreadsRequestContext().getRemoteUser() + "acccustid" + accountId;
        Integer custId = CacheHelper.getFromLocalCache(key, Integer.class);
        if (custId == null) {
            ServiceInstanceQuery siq = new ServiceInstanceQuery();
            siq.setAccountId(accountId);
            siq.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
            ServiceInstance si = SCAWrapper.getUserSpecificInstance().getServiceInstance(siq);
            String siKeyPrefix = SCAWrapper.getUserSpecificInstance().getThreadsRequestContext().getRemoteUser() + "si" + si.getServiceInstanceId();
            CacheHelper.putInLocalCache(siKeyPrefix + StServiceInstanceLookupVerbosity.MAIN, si, SHORT_CACHE_SECS);
            custId = getCustomer(si.getCustomerId(), StCustomerLookupVerbosity.CUSTOMER).getCustomerId();
            CacheHelper.putInLocalCache(key, custId, SHORT_CACHE_SECS);
        }
        return custId;
    }

    public static String getPrimaryAccountHolderName(long accountId) {
        checkContext();
        try {
            String key = SCAWrapper.getUserSpecificInstance().getThreadsRequestContext().getRemoteUser() + "accname" + accountId;
            String name = CacheHelper.getFromLocalCache(key, String.class);
            if (name == null) {
                ServiceInstanceQuery siq = new ServiceInstanceQuery();
                siq.setAccountId(accountId);
                siq.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
                ServiceInstance si = SCAWrapper.getUserSpecificInstance().getServiceInstance(siq);
                String siKeyPrefix = SCAWrapper.getUserSpecificInstance().getThreadsRequestContext().getRemoteUser() + "si" + si.getServiceInstanceId();
                CacheHelper.putInLocalCache(siKeyPrefix + StServiceInstanceLookupVerbosity.MAIN, si, SHORT_CACHE_SECS);
                Customer cust = getCustomer(si.getCustomerId(), StCustomerLookupVerbosity.CUSTOMER);
                name = String.format("%s %s", cust.getFirstName(), cust.getLastName());
                CacheHelper.putInLocalCache(key, name, SHORT_CACHE_SECS);
            }
            return name;
        } catch (Exception e) {
            log.warn("Error getting account holder name: ", e);
            return "No Customer Found";
        }

    }

    public static String getServiceInstanceUserName(int serviceInstanceId) {
        ServiceInstance si = getServiceInstance(serviceInstanceId, StServiceInstanceLookupVerbosity.MAIN);
        Customer cust = getCustomer(si.getCustomerId(), StCustomerLookupVerbosity.CUSTOMER);
        return String.format("%s %s", cust.getFirstName(), cust.getLastName());
    }

    public static String getCustomerName(int customerId) {
        if (customerId == 0) {
            return "Unknown";
        }
        try {
            Customer cust = getCustomer(customerId, StCustomerLookupVerbosity.CUSTOMER);
            return String.format("%s %s", cust.getFirstName(), cust.getLastName());
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public static String getOrganisationName(int orgId) {
        if (orgId == 0) {
            return "Unknown";
        }
        try {
            Organisation org = getOrganisation(orgId, StOrganisationLookupVerbosity.MAIN);
            return org.getOrganisationName();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public static String getProductInstanceICCID(int productInstanceId) {
        if (productInstanceId <= 0) {
            return "NA";
        }
        try {
            String iccid = "NA";
            ServiceInstance si = getSIMServiceInstanceForProductInstanceId(productInstanceId);
            if (si == null) {
                return "NA";
            }
            for (AVP avp : si.getAVPs()) {
                if (avp.getAttribute().equalsIgnoreCase("IntegratedCircuitCardIdentifier")) {
                    iccid = avp.getValue();
                    break;
                }
            }
            return iccid;
        } catch (Exception e) {
            log.warn("Error getting service instance ICCID: ", e);
            return "NA";
        }
    }

    public static String getProductInstancePhoneNumber(int productInstanceId) {
        if (productInstanceId <= 0) {
            return "NA";
        }

        try {
            String phone = "NA";
            ServiceInstance si = getVoiceServiceInstanceForProductInstanceId(productInstanceId);
            if (si == null) {
                return "NA";
            }
            for (AVP avp : si.getAVPs()) {
                if (avp.getAttribute().equalsIgnoreCase("PublicIdentity")) {
                    phone = Utils.getFriendlyPhoneNumber(avp.getValue());
                    break;
                }
            }
            return phone;
        } catch (Exception e) {
            log.warn("Error getting service instance Phone Number: ", e);
            return "NA";
        }
    }

    public static String getServiceInstanceICCID(int serviceInstanceId) {
        log.debug("Looking for ServiceInstanceICCID for [{}]", serviceInstanceId);
        if (serviceInstanceId <= 0) {
            return "NA";
        }
        try {
            String iccid = "NA";
            ServiceInstance si = getServiceInstance(serviceInstanceId, StServiceInstanceLookupVerbosity.MAIN_SVCAVP);
            if (si == null) {
                return "NA";
            }
            for (AVP avp : si.getAVPs()) {
                if (avp.getAttribute().equalsIgnoreCase("IntegratedCircuitCardIdentifier")) {
                    iccid = avp.getValue();
                    break;
                }
            }
            return iccid;
        } catch (Exception e) {
            log.warn("Error getting service instance ICCID: ", e);
            return "No ICCID Found";
        }
    }

    public static String getServiceInstancePhoneNumber(int serviceInstanceId) {
        if (serviceInstanceId <= 0) {
            return "NA";
        }

        try {
            String phone = "NA";
            ServiceInstance si = getServiceInstance(serviceInstanceId, StServiceInstanceLookupVerbosity.MAIN_SVCAVP);
            if (si == null) {
                return "NA";
            }
            for (AVP avp : si.getAVPs()) {
                if (avp.getAttribute().equalsIgnoreCase("PublicIdentity")) {
                    phone = Utils.getFriendlyPhoneNumber(avp.getValue());
                    break;
                }
            }
            return phone;
        } catch (Exception e) {
            log.warn("Error getting service instance Phone Number: ", e);
            return "No Number Found";
        }
    }

}
