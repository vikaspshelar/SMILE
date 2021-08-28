/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.plugins.shortcodesms;

import com.smilecoms.commons.sca.Account;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.CustomerQuery;
import com.smilecoms.commons.sca.ProductInstance;
import com.smilecoms.commons.sca.ProductServiceInstanceMapping;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.ServiceInstanceQuery;
import com.smilecoms.commons.sca.StAccountLookupVerbosity;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.StMessagePriority;
import com.smilecoms.commons.sca.StServiceInstanceLookupVerbosity;
import com.smilecoms.commons.base.cache.CacheHelper;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class ShortCodeActionHelper {

    private static final Logger log = LoggerFactory.getLogger(ShortCodeActionHelper.class);

    /**
     * Bit starts at 1 for the first bit and so on
     *
     * @param uri
     * @param bit
     * @param newValue
     */
    public void setSIpURIUsersOptInLevelBit(String uri, int bit, int newValue) {

        log.debug("Setting bit [{}] to [{}] for END_USER_SIP_URI [{}]", new Object[]{bit, newValue, uri});
        ServiceInstance si = getServiceInstance(uri);
        log.debug("Got service instance id [{}] used by customer id [{}]. Going to get the customers current opt in level", si.getServiceInstanceId(), si.getCustomerId());
        Customer c = SCAWrapper.getAdminInstance().getCustomer(si.getCustomerId(), StCustomerLookupVerbosity.CUSTOMER);

        int currentOptInLevel = c.getOptInLevel();
        log.debug("Current Opt in level is [{}]", currentOptInLevel);

        int newOptInLevel;
        int val = (int) Math.pow(2, bit - 1);
        if (newValue == 0) {
            val = ~val;
            newOptInLevel = val & currentOptInLevel;
        } else {
            newOptInLevel = val | currentOptInLevel;
        }

        c.setOptInLevel(newOptInLevel);
        log.debug("Setting customers opt in level to [{}]", newOptInLevel);
        SCAWrapper.getAdminInstance().modifyCustomer(c);
        log.debug("Finished");
    }

    public void sendSMS(String from, String to, String msg) {
        log.debug("Sending message from [{}] to [{}]: [{}]", new Object[]{from, to, msg});
        com.smilecoms.commons.sca.ShortMessage sm = new com.smilecoms.commons.sca.ShortMessage();
        sm.setFrom(from);
        sm.setTo(to);
        sm.setBody(msg);
        sm.setPriority(StMessagePriority.HIGH);
        com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().sendShortMessage(sm);
        log.debug("Message submitted");
    }

    public void sendSMS(String from, String to, String msg, byte dcs) {
        log.debug("Sending message from [{}] to [{}]: [{}]", new Object[]{from, to, msg});
        com.smilecoms.commons.sca.ShortMessage sm = new com.smilecoms.commons.sca.ShortMessage();
        sm.setFrom(from);
        sm.setTo(to);
        sm.setBody(msg);
        sm.setDataCodingScheme(dcs);
        sm.setPriority(StMessagePriority.HIGH);
        com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().sendShortMessage(sm);
        log.debug("Message submitted");
    }

    public Object getStatefulObject(String key) {
        return CacheHelper.getFromRemoteCache("scah_" + key);
    }

    public void setStatefulObject(String key, Object val) {
        CacheHelper.putInRemoteCacheSync("scah_" + key, val, 3600 * 2);
    }

    public ServiceInstance getServiceInstance(String uri) {
        ServiceInstanceQuery siq = new ServiceInstanceQuery();
        siq.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
        siq.setIdentifier(uri);
        siq.setIdentifierType("END_USER_SIP_URI");
        log.debug("Getting the service instance for SIP URI [{}]", uri);
        return SCAWrapper.getAdminInstance().getServiceInstance(siq);
    }

    public Customer getCustomerWithAvpsByPhoneNumber(String uri) {
        log.debug("Getting customer by URI [{}]", uri);
        Customer cust = null;
        try {
            ServiceInstance si = getServiceInstance(uri);
            if (si != null) {
                cust = SCAWrapper.getAdminInstance().getCustomer(si.getCustomerId(), StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES_SVCAVP);
                log.debug("Got customer with id [{}] based on Smile number", cust.getCustomerId());
                return cust;
            } else {
                log.debug("No service instance found");
            }
        } catch (Exception e) {
            log.debug("No service instance for number [{}] [{}]", uri, e.toString());
        }
        uri = Utils.getFriendlyPhoneNumberKeepingCountryCode(uri);
        try {
            // Must be alternative contact number:
            CustomerQuery q = new CustomerQuery();
            q.setAlternativeContact(uri);
            q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES_SVCAVP);
            q.setResultLimit(1);
            cust = SCAWrapper.getAdminInstance().getCustomer(q);
            log.debug("Got customer with id [{}] based on alternative contact number", cust.getCustomerId());
        } catch (Exception e) {
            log.debug("No customer for alternate contact [{}] [{}]", uri, e.toString());
        }
        return cust;
    }

    public Customer getCustomerByPhoneNumber(String uri) {
        log.debug("Getting customer by URI [{}]", uri);
        Customer cust = null;
        try {
            ServiceInstance si = getServiceInstance(uri);
            if (si != null) {
                cust = SCAWrapper.getAdminInstance().getCustomer(si.getCustomerId(), StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES);
                log.debug("Got customer with id [{}] based on Smile number", cust.getCustomerId());
                return cust;
            } else {
                log.debug("No service instance found");
            }
        } catch (Exception e) {
            log.debug("No service instance for number [{}] [{}]", uri, e.toString());
        }
        uri = Utils.getFriendlyPhoneNumberKeepingCountryCode(uri);
        try {
            // Must be alternative contact number:
            CustomerQuery q = new CustomerQuery();
            q.setAlternativeContact(uri);
            q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES);
            q.setResultLimit(1);
            cust = SCAWrapper.getAdminInstance().getCustomer(q);
            log.debug("Got customer with id [{}] based on alternative contact number", cust.getCustomerId());
        } catch (Exception e) {
            log.debug("No customer for alternate contact [{}] [{}]", uri, e.toString());
        }
        return cust;
    }

    public void setAdminContextOnThread() {
        SCAWrapper.setThreadsRequestContextAsAdmin();
    }

    public void removeAdminContextOffThread() {
        SCAWrapper.removeThreadsRequestContext();
    }

    public Account getAccount(long accountId) {
        return SCAWrapper.getAdminInstance().getAccount(accountId, StAccountLookupVerbosity.ACCOUNT_UNITCREDITS);
    }

    public Account getAccountFromAccountId(String accountId) {
        long longAccountId = -1;
        try {
            longAccountId = Long.parseLong(accountId);
        } catch (java.lang.NumberFormatException e) {
            log.warn("Error getting account [{}]", accountId);
        }
        if (longAccountId == -1) {
            return null;
        }
        return SCAWrapper.getAdminInstance().getAccount(longAccountId, StAccountLookupVerbosity.ACCOUNT_UNITCREDITS);
    }

    public Account getAccount(String sipUri) {
        return getAccount(getServiceInstance(sipUri).getAccountId());
    }

    public long getAccountId(String sipUri) {
        return getServiceInstance(sipUri).getAccountId();
    }

    /*
    Request from UG is if the account has only 1 PI with a friendly name then this name should be used when returning results for "All accounts" request
    
     */
    public String getFriendlyNameIfAccountHasOnlyOnePI(Customer cust, long accountId) {
        String friendlyName = null;
        int numPIOnAccount = 0;
        java.util.List<ProductInstance> piList = cust.getProductInstances();
        for (com.smilecoms.commons.sca.ProductInstance pi : piList) {
            boolean isPiOnAccount = false;
            java.util.List<ProductServiceInstanceMapping> psimList = pi.getProductServiceInstanceMappings();
            for (ProductServiceInstanceMapping psim : psimList) {
                if (accountId == psim.getServiceInstance().getAccountId()) {
                    isPiOnAccount = true;
                }
            }
            if (isPiOnAccount && pi.getFriendlyName() != null && !pi.getFriendlyName().isEmpty()) {
                friendlyName = pi.getFriendlyName();
                numPIOnAccount++;
            }
        }
        if (numPIOnAccount == 1) {
            return friendlyName;
        }
        return null;
    }

    public boolean isAccountOwnedByCustomer(Customer cust, long accountId) {
        boolean accountOwned = false;
        java.util.List<ProductInstance> piList = cust.getProductInstances();
        for (com.smilecoms.commons.sca.ProductInstance pi : piList) {
            java.util.List<ProductServiceInstanceMapping> psimList = pi.getProductServiceInstanceMappings();
            for (ProductServiceInstanceMapping psim : psimList) {
                if (accountId == psim.getServiceInstance().getAccountId()) {
                    accountOwned = true;
                    break;
                }
            }
        }
        return accountOwned;
    }

    public boolean isServiceInstanceOwnedByCustomer(Customer cust, int serviceInstanceId) {
        boolean serviceInstanceOwned = false;
        java.util.List<ProductInstance> piList = cust.getProductInstances();
        for (com.smilecoms.commons.sca.ProductInstance pi : piList) {
            java.util.List<ProductServiceInstanceMapping> psimList = pi.getProductServiceInstanceMappings();
            for (ProductServiceInstanceMapping psim : psimList) {
                if (serviceInstanceId == psim.getServiceInstance().getServiceInstanceId()) {
                    serviceInstanceOwned = true;
                    break;
                }
            }
        }
        return serviceInstanceOwned;
    }

    public List getAccountIdsByCustomer(Customer cust) {
        java.util.List accountIds = new java.util.ArrayList();
        java.util.List<ProductInstance> piList = cust.getProductInstances();
        for (com.smilecoms.commons.sca.ProductInstance pi : piList) {
            java.util.List<ProductServiceInstanceMapping> psimList = pi.getProductServiceInstanceMappings();
            for (ProductServiceInstanceMapping psim : psimList) {
                String tmp = "" + psim.getServiceInstance().getAccountId();
                if (!accountIds.contains(tmp)) {
                    accountIds.add(tmp);
                }
            }
        }
        return accountIds;
    }

    public List runSQL(String sql) {

        EntityManagerFactory emf = ShortCodeSMSDeliveryPlugin.getEMF();
        EntityManager em = JPAUtils.getEM(emf);
        try {
            Query q = em.createNativeQuery(sql);
            return q.getResultList();
        } catch (Exception e) {
            log.warn("Error running SQL: ", e);
            return null;
        } finally {
            JPAUtils.closeEM(em);
        }
    }

    public String getNINfromNIDA(String from, String input) {
        log.debug("Inside getNINfromNIDA with input parameters: " + from + " and " + input);

        //return NidaRestService.getNIN(from,input);
        return "This functionality is still under development phase";
    }

    public Customer updateCustomerNIN(String uri, String idnumber) throws Exception {
        log.debug("Update customer NIN by URI [{}]", uri);
        Customer cust = getCustomerByPhoneNumber(uri);
        if (cust != null) {
            if(!StringUtils.isNumeric(idnumber)) {
                return null;
            }
            cust.setNationalIdentityNumber(idnumber);
            SCAWrapper.getUserSpecificInstance().modifyCustomer(cust);
        }
        return cust;
    }
    
    
    public boolean kycBot(String from, String to, String msg, com.smilecoms.mm.plugins.shortcodesms.ShortCodeActionHelper helper, org.slf4j.Logger log) {
        log.error("Message received is:"+msg+" and from:"+from);
        com.smilecoms.commons.sca.Customer cust = helper.getCustomerWithAvpsByPhoneNumber(from);
        boolean kycVerified = false;
        String successText = "You are fully registered. Thank you for choosing Smile.";
        String failureText = "You are not fully registered. Visit any Smile Outlet with your ID to register as soon as possible to avoid being blocked.";

        String country = "TZ";
//        String country="NG";
//        String country="UG";

        //NG
        if (country.equalsIgnoreCase("NG")) {
            kycVerified = cust.getKYCStatus().equalsIgnoreCase("V");
            successText = "You are fully registered. Thank you for choosing Smile.";
            failureText = "You are not fully registered. Visit any Smile Outlet with your ID to register as soon as possible to avoid being blocked.";
        } //UG
        else if (country.equalsIgnoreCase("UG")) {
            successText = "You are fully registered. Thank you for choosing Smile.";
            failureText = "You are not fully registered. Visit any Smile Outlet with your ID to register as soon as possible to avoid being blocked.";
            java.util.List piList = cust.getProductInstances();
            int productId = -1;
            //find the right product on this customer for this from number
            for (int r = 0; r < piList.size(); r++) {
                java.util.List psimList = ((com.smilecoms.commons.sca.ProductInstance) piList.get(r)).getProductServiceInstanceMappings();
                for (int s = 0; s < psimList.size(); s++) {
                    java.util.List avpList = ((com.smilecoms.commons.sca.ProductServiceInstanceMapping) psimList.get(s)).getServiceInstance().getAVPs();
                    for (int e = 0; e < avpList.size(); e++) {
                        if (((com.smilecoms.commons.sca.AVP) avpList.get(e)).getAttribute().equalsIgnoreCase("PublicIdentity") && ((com.smilecoms.commons.sca.AVP) avpList.get(e)).getValue().contains(from)) {
                            productId = r;
                            break;
                        }
                    }
                }
            }
            //find the SIM service with KYC status on it in this customer
            if (productId >= 0) {
                java.util.List psimList = ((com.smilecoms.commons.sca.ProductInstance) piList.get(productId)).getProductServiceInstanceMappings();
                for (int s = 0; s < psimList.size(); s++) {
                    java.util.List avpList = ((com.smilecoms.commons.sca.ProductServiceInstanceMapping) psimList.get(s)).getServiceInstance().getAVPs();
                    for (int e = 0; e < avpList.size(); e++) {
                        if (((com.smilecoms.commons.sca.AVP) avpList.get(e)).getAttribute().equalsIgnoreCase("KYCStatus") && ((com.smilecoms.commons.sca.AVP) avpList.get(e)).getValue().equalsIgnoreCase("Complete")) {
                            kycVerified = true;
                            break;
                        }
                    }
                }
            }

        } //TZ
        else if (country.equalsIgnoreCase("TZ")) {
        if(msg.equalsIgnoreCase("CHKREG"))
		{
			successText = "Dear Customer, you are fully registered with NIDA under NIN "+cust.getIdentityNumber()+". Thank you for choosing Smile.";
			failureText = "Dear Customer, your Smile SIM is not verified with NIDA. Visit any Smile shop with a valid passport, driving licence, voting card or national ID to verification.";
			kycVerified = cust.getKYCStatus().equalsIgnoreCase("V");
		} else if(msg.equalsIgnoreCase("LISTMSISDN"))
		{
			String nin = cust.getIdentityNumber();
			String sql = "select IF(SIM.IDENTIFIER is NULL, 'N/A', SUBSTRING(SUBSTRING_INDEX(SIM.IDENTIFIER, '@', 1),LENGTH('tel:')+1)) as MSISDN from customer_profile CP join product_instance PDI on (PDI.CUSTOMER_PROFILE_ID =CP.CUSTOMER_PROFILE_ID) join service_instance SI on (SI.product_instance_id = PDI.PRODUCT_INSTANCE_ID) join service_instance_mapping SIM on (SIM.SERVICE_INSTANCE_ID = SI.SERVICE_INSTANCE_ID) where SIM.IDENTIFIER_TYPE='END_USER_E164' and CP.ID_NUMBER="+nin;
			java.util.List msisdnList = helper.runSQL(sql);
			
                        
                        
                        successText="The Smile MSISDN's associated with NIN "+nin+" are "+String.join(",", msisdnList);
			
                        
			kycVerified=true;
		} else if(msg.equalsIgnoreCase("DEREG"))
		{
			successText="Hello, it is your responsibility as a customer to visit the concerned mobile network to de-register the number that is not in use. Thank you.";
			kycVerified=true;
		} else {
			successText = "wrong attribute specified to 106 shortcode";
			kycVerified=true;
		}
		
        }

        if (kycVerified) {
            helper.sendSMS(to, from, successText);
        } else {
            helper.sendSMS(to, from, failureText);
        }
        return true;
    }
}
