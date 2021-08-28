package com.smilecoms.commons.sca;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.beans.NonUserSpecificCachedDataHelper;
import com.smilecoms.commons.scaauth.SCAAuthenticator;
import com.smilecoms.commons.scaauth.SCALoginException;
import com.smilecoms.commons.base.cache.CacheHelper;
import com.smilecoms.commons.util.Codec;
import com.smilecoms.commons.util.Utils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.*;

/**
 * Allows us to authenticate a user against SCA
 *
 * @author PCB
 */
public class SCAAuthenticatorImpl implements SCAAuthenticator {

    //Logging
    private static final Logger log = LoggerFactory.getLogger(SCAAuthenticatorImpl.class.getName());

    /**
     * Returns the groups the user is in if the authentication credentials are
     * correct, or throws an error otherwise
     *
     * @param user
     * @param password
     * @param application - Not used right now
     * @return String array of group names
     * @throws com.smilecoms.commons.scaauth.SCALoginException
     */
    @Override
    public String[] authenticate(String user, String password, String application) throws SCALoginException {
        if (log.isDebugEnabled()) {
            log.debug("In SCAAuthenticatorImpl and about to login into SCA with customer " + user);
        }

        if (user.equalsIgnoreCase("sop") && password.equals(BaseUtils.getPropertyFailFast("env.sop.auth.password", "SopRulesOkButNowYouCantAccessIt"))) {
            // Allow user SOP to access SOP with no password
            return new String[]{"SOP"};
        }

        String[] ret = null;
        log.debug("Creating auth query");
        AuthenticationQuery authQuery = new AuthenticationQuery();
        authQuery.setSSOIdentity(user);
        if (password.length() >= 30) {
            // Already encrypted
            log.debug("Password is already encrypted");
            authQuery.setSSOEncryptedPassword(password);
        } else {
            try {
                log.debug("Password is not encrypted");
                authQuery.setSSOEncryptedPassword(Codec.stringToEncryptedHexString(password));
                log.debug("Finished encrypting password");
            } catch (Throwable e) {
                log.warn("Error encrypting password", e);
                throw e;
            }
        }
        AuthenticationResult authResult;

        try {
            boolean done = false;
            log.debug("Calling authenticate on SCA");
            authResult = SCAWrapper.getAdminInstance().authenticate(authQuery);
            log.debug("Called authenticate on SCA");
            try {
                done = authResult.getDone().equals(StDone.TRUE);
                List groups = authResult.getSecurityGroups();
                Iterator<String> groupIt = groups.iterator();
                ret = new String[groups.size()];
                int cnt = 0;
                while (groupIt.hasNext()) {
                    ret[cnt] = Utils.getUnfriendlyRoleName(groupIt.next());
                    cnt++;
                }
            } catch (Exception e) {
            }

            if (!done) {
                // Failed authentication
                log.info("Invalid credentials while logging in user in SCA");
                throw new SCALoginException();
            }
        } catch (Exception e) {
            log.info("Error logging in user in SCA " + e.toString());
            throw new SCALoginException();
        } finally {
            if (log.isDebugEnabled()) {
                log.debug("Finished SCAAuthenticatorImpl login for customer " + user);
            }
        }
        return ret;
    }
    public static final String ALL_OK = "ok";
    public static final String OBVISCATE = "ob";
    public static final String ENTITY_ACCOUNT = "Account";
    public static final String ENTITY_SALE = "Sale";
    public static final String ENTITY_SPECIAL_ACCOUNT = "SpecialAccount";
    public static final String ENTITY_PRODUCT_INSTANCE = "ProductInstance";
    public static final String ENTITY_SERVICE_INSTANCE = "ServiceInstance";
    public static final String ENTITY_CUSTOMER = "Customer";
    public static final String ENTITY_ORGANISATION = "Organisation";
    public static final String ENTITY_UNIT_CREDIT_SPECIFICATION = "UnitCreditSpecification";
    private static final Set<String> possibleEntityTypes = new HashSet();

    static {
        possibleEntityTypes.add(ENTITY_ACCOUNT);
        possibleEntityTypes.add(ENTITY_SALE);
        possibleEntityTypes.add(ENTITY_PRODUCT_INSTANCE);
        possibleEntityTypes.add(ENTITY_SERVICE_INSTANCE);
        possibleEntityTypes.add(ENTITY_CUSTOMER);
        possibleEntityTypes.add(ENTITY_ORGANISATION);
        possibleEntityTypes.add(ENTITY_UNIT_CREDIT_SPECIFICATION);
    }

    private static final Set<String> customerRoles = new HashSet();

    static {
        customerRoles.add("Customer");
        customerRoles.add("SOP");
        customerRoles.add("Promo1");
        customerRoles.add("Promo2");
        customerRoles.add("Promo3");
        customerRoles.add("Promo4");
        customerRoles.add("Promo5");
        customerRoles.add("Promo6");
        customerRoles.add("Promo7");
        customerRoles.add("Promo8");
        customerRoles.add("Promo9");
        customerRoles.add("Promo10");
    }

    public static String obviscate(String data, String type) {
        if (type.equals(ALL_OK)) {
            return data;
        }
        return replaceEveryNthCharacters(data, 2, "#");
    }

    public static String checkEntityAccess(String originatingIdentity, Set<String> callersRoles, String methodName, String entityType, String entityKey) throws EntityAuthorisationException {

        try {
            if (originatingIdentity == null || methodName == null || entityKey == null || entityKey.isEmpty() || callersRoles == null) {
                if (BaseUtils.getBooleanPropertyFailFast("env.sca.strict.permissions.checks", true)) {
                    throw new EntityAuthorisationException(true);
                } else {
                    log.warn("A BPEL flow is using checkpermissions incorrectly!");
                    return ALL_OK;
                }
            }

            if (!possibleEntityTypes.contains(entityType)) {
                throw new RuntimeException("Invalid entity type to check access permissions on: " + entityType + " being used in process " + methodName);
            }

            if (callersRoles.isEmpty()) {
                throw new EntityAuthorisationException(false);
            }

            if (entityType.equals(ENTITY_UNIT_CREDIT_SPECIFICATION)) {
                return checkProvisionUnitCreditSpec(callersRoles, entityKey);
            }

            boolean isICP = false;
            boolean isSRASmilePartner = false;
            boolean isSRASmileWeb = false;
            boolean isOnlyACustomer = false;
            boolean isAdministrator = false;
            boolean isTPGW = false;
            boolean isDelivery = false;
            boolean isBatch = false;
            boolean isStaffToBeObviscated = false;

            Set<String> icpRoles = BaseUtils.getPropertyAsSet("env.icp.allowed.roles");
            if (callersRoles.contains("Administrator")) {
                isAdministrator = true;
            } else if (callersRoles.contains("TPGW")) {
                isTPGW = true;
            } else if (callersRoles.contains("Delivery")) {
                isDelivery = true;
            } else if (callersRoles.size() == 1 && callersRoles.contains("Batch")) {
                isBatch = true; // For ET etc
            } else {

                // http://jira.smilecoms.com/browse/HBT-8208
                // DirectSales, KioskSales, ShopSales
                isStaffToBeObviscated = false;
                for (String obvicatedStaffRole : BaseUtils.getPropertyAsList("env.staff.roles.to.be.obviscated")) {
                    if (callersRoles.contains(obvicatedStaffRole)) {
                        isStaffToBeObviscated = true;
                        break;
                    }
                }

                if (!isStaffToBeObviscated) {
                    boolean allRolesAreThatOfACustomer = true;
                    for (String role : callersRoles) {
                        if (icpRoles.contains(role)) {
                            isICP = true;
                        }
                        if (role.equals("SRASmileWeb")) {
                            isSRASmileWeb = true;
                        }
                        if (role.equals("SRASmilePartner")) {
                            isSRASmilePartner = true;
                        }
                        if (!customerRoles.contains(role)) {
                            allRolesAreThatOfACustomer = false;
                        }
                    }

                    if (allRolesAreThatOfACustomer) {
                        isOnlyACustomer = true;
                    }
                }
            }
            if (isOnlyACustomer) {
                return checkCustomerEntityAccess(originatingIdentity, methodName, entityType, Long.valueOf(entityKey));
            } else if (isICP) {
                return checkICPEntityAccess(originatingIdentity, methodName, entityType, Long.valueOf(entityKey));
            } else if (isDelivery) {
                return checkDeliveryEntityAccess(originatingIdentity, methodName, entityType, Long.valueOf(entityKey));
            } else if (isAdministrator) {
                return checkAdministratorEntityAccess(originatingIdentity, methodName, entityType, Long.valueOf(entityKey));
            } else if (isTPGW) {
                return checkTPGWEntityAccess(originatingIdentity, methodName, entityType, Long.valueOf(entityKey));
            } else if (isSRASmilePartner) {
                return checkSRASmilePartnerEntityAccess(originatingIdentity, methodName, entityType, Long.valueOf(entityKey));
            } else if (isSRASmileWeb) {
                return checkSRASmileWebEntityAccess(originatingIdentity, methodName, entityType, Long.valueOf(entityKey));
            } else if (isBatch) {
                return checkBatchEntityAccess(originatingIdentity, methodName, entityType, Long.valueOf(entityKey));
            } else if (isStaffToBeObviscated) {
                return checkSalesStaffEntityAccess(originatingIdentity, methodName, entityType, Long.valueOf(entityKey));
            } else {
                return checkStaffEntityAccess(originatingIdentity, methodName, entityType, Long.valueOf(entityKey));
            }
        } catch (EntityAuthorisationException eae) {
            eae.setRoles(callersRoles);
            try {
                if (entityType.equals(ENTITY_UNIT_CREDIT_SPECIFICATION) && !Utils.isNumeric(entityKey)) {
                    entityKey = String.valueOf(NonUserSpecificCachedDataHelper.getUnitCreditSpecificationByItemNumber(entityKey).getUnitCreditSpecificationId());
                }
            } catch (SCABusinessError sbe) {
            }
            eae.setEntityKey(Long.valueOf(entityKey));
            eae.setEntityType(entityType);
            eae.setIdentity(originatingIdentity);
            eae.setMethodName(methodName);
            eae.processException();
            throw eae;
        }
    }

    private static String replaceEveryNthCharacters(String patternString, int nthVal, String patternCharacter) {
        if (patternCharacter.isEmpty()) {
            return patternString;
        }
        if (nthVal <= 0) {
            return patternString;
        }
        String result = "";
        int startIdx = nthVal;
        int currentLenOfAppendedString = 0;
        int inputLen = patternString.length() + (nthVal - 1);

        while (startIdx <= inputLen) {
            result = result + patternString.substring(currentLenOfAppendedString, startIdx - 1);
            result = result + patternCharacter;
            currentLenOfAppendedString = result.length();
            startIdx = startIdx + nthVal;
        }
        return result;
    }

    /**
     *************************
     *
     * LOGIC FOR ENTITY ACCESS
     *
     *************************
     */
    private static String checkCustomerEntityAccess(String originatingIdentity, String methodName, String entityType, long entityKey) throws EntityAuthorisationException {
        log.debug("In checkCustomerEntityAccess [{}] [{}] [{}] [{}]", new Object[]{originatingIdentity, methodName, entityType, entityKey});
        /**
         * Customers access to entities should be as follows.
         *
         * Accounts: Accounts used by services they own
         *
         * Customer Profiles: Their own profile
         *
         * Service Instances: Their own Service Instances
         *
         * Product Instances: Product instances that have services they own
         *
         * Organisations: None
         */

        Map<String, Set<Long>> entityData = getCustomersAllowedEntityMap(originatingIdentity);

        if (methodName.equals("transferBalance")) {
            Set<Long> specialAccounts = entityData.get(ENTITY_SPECIAL_ACCOUNT);
            // Plain customers cannot transfer out of special accounts
            if (specialAccounts != null && specialAccounts.contains(entityKey)) {
                throw new EntityAuthorisationException(true);
            }
        }

        // Just to be extra safe
        if (entityType.equals(ENTITY_ACCOUNT) && entityKey < 1100000000) {
            throw new EntityAuthorisationException(true);
        }

        Set<Long> allowedKeys = entityData.get(entityType);
        if (allowedKeys != null && allowedKeys.contains(entityKey)) {
            return ALL_OK;
        }
        log.debug("Seems like the customer is not allowed access but maybe its just that something changed between when the last cache was refreshed. Lets force cache refresh and try again");
        entityData = getCustomersAllowedEntityMap(originatingIdentity, true);
        allowedKeys = entityData.get(entityType);
        if (allowedKeys != null && allowedKeys.contains(entityKey)) {
            return ALL_OK;
        }

        throw new EntityAuthorisationException(true);
    }

    private static String checkSalesStaffEntityAccess(String originatingIdentity, String methodName, String entityType, long entityKey) throws EntityAuthorisationException {
        log.debug("In checkStaffEntityAccess [{}] [{}] [{}] [{}]", new Object[]{originatingIdentity, methodName, entityType, entityKey});
        // Staff can see anything. We limit their functionality based on SEP Permissions not access to specific entities
        // To be safe, dont let non admins use special accounts
        if (methodName.equals("transferBalance") && entityType.equals(ENTITY_ACCOUNT) && entityKey < 1100000000) {
            throw new EntityAuthorisationException(false);
        }

        if (entityType.equals(ENTITY_CUSTOMER)) {
            if (areTheyAccessingTheirOwnCustomerProfile(originatingIdentity, entityKey)) {
                return ALL_OK;
            } else {
                return OBVISCATE;
            }
        }

        return ALL_OK;
    }

    private static String checkStaffEntityAccess(String originatingIdentity, String methodName, String entityType, long entityKey) throws EntityAuthorisationException {
        log.debug("In checkStaffEntityAccess [{}] [{}] [{}] [{}]", new Object[]{originatingIdentity, methodName, entityType, entityKey});
        // Staff can see anything. We limit their functionality based on SEP Permissions not access to specific entities
        // To be safe, dont let non admins use special accounts
        if (methodName.equals("transferBalance") && entityType.equals(ENTITY_ACCOUNT) && entityKey < 1100000000) {
            throw new EntityAuthorisationException(false);
        }

        return ALL_OK;
    }

    private static String checkTPGWEntityAccess(String originatingIdentity, String methodName, String entityType, long entityKey) throws EntityAuthorisationException {
        log.debug("In checkTPGWEntityAccess [{}] [{}] [{}] [{}]", new Object[]{originatingIdentity, methodName, entityType, entityKey});

        String ussdUser = null;
        if (entityType.equals(ENTITY_ACCOUNT) && entityKey > 1100000000) {
            if (methodName.equals("transferBalance") || methodName.equals("getAccounts") || methodName.equals("processSale")) {
                if (doesCustomerOwnAccount(originatingIdentity, entityKey)) {
                    return ALL_OK;
                }
            }
            //.-- Allow USSD application to query account details(Balance query) of other users. 
            ussdUser = BaseUtils.getProperty("env.sep.ussd.user", null);
            if(ussdUser != null && ussdUser.equals(originatingIdentity) && (methodName.equals("getAccounts"))){
                return ALL_OK;
            }
            if (methodName.equals("purchaseUnitCredit")) {
                return ALL_OK;
            }

            if (methodName.equals("getAccountHistory")) {
                return ALL_OK;
            }

        } else if (entityType.equals(ENTITY_CUSTOMER)) {
            if (methodName.equals("getCustomers") || methodName.equals("processSale")) {
                return ALL_OK;
            }
        } else if (entityType.equals(ENTITY_PRODUCT_INSTANCE)) {
            if (methodName.equals("getProductInstances")) {
                return ALL_OK;
            }
        } else if (entityType.equals(ENTITY_SERVICE_INSTANCE)) {
            if (methodName.equals("getServiceInstances")) {
                return ALL_OK;
            }
        }

        log.error("unable to authenticate [{}] methodName [{}] entityType [{}] ussdUser [{}]", originatingIdentity, methodName, entityType, ussdUser);
        throw new EntityAuthorisationException(true);
    }

    private static String checkSRASmileWebEntityAccess(String originatingIdentity, String methodName, String entityType, long entityKey) throws EntityAuthorisationException {
        log.debug("In checkSRASmileWebEntityAccess [{}] [{}] [{}] [{}]", new Object[]{originatingIdentity, methodName, entityType, entityKey});

        // TODO: make less open based on method name
        // Basically anything a partner does on SRA must be on behalf of the logged in customer except whats needed for recharge express
        if (entityType.equals(ENTITY_ACCOUNT) || entityType.equals(ENTITY_CUSTOMER) || entityType.equals(ENTITY_PRODUCT_INSTANCE) || BaseUtils.getBooleanProperty("env.sra.allow.all.access", false)) {
            return ALL_OK;
        }

        throw new EntityAuthorisationException(true);
    }

    private static String checkSRASmilePartnerEntityAccess(String originatingIdentity, String methodName, String entityType, long entityKey) throws EntityAuthorisationException {
        log.debug("In checkSRASmilePartnerEntityAccess [{}] [{}] [{}] [{}]", new Object[]{originatingIdentity, methodName, entityType, entityKey});

        if (entityType.equals(ENTITY_ACCOUNT)) {

            // Just to be extra safe
            if (entityKey < 1100000000) {
                throw new EntityAuthorisationException(true);
            }

            if (doesCustomerOwnAccount(originatingIdentity, entityKey) || methodName.equals("processSale")) {
                return ALL_OK;
            }
        }

        throw new EntityAuthorisationException(true);
    }

    private static String checkAdministratorEntityAccess(String originatingIdentity, String methodName, String entityType, long entityKey) throws EntityAuthorisationException {
        log.debug("In checkAdministratorEntityAccess [{}] [{}] [{}] [{}]", new Object[]{originatingIdentity, methodName, entityType, entityKey});
        return ALL_OK;
    }

    private static String checkBatchEntityAccess(String originatingIdentity, String methodName, String entityType, long entityKey) throws EntityAuthorisationException {
        log.debug("In checkBatchEntityAccess [{}] [{}] [{}] [{}]", new Object[]{originatingIdentity, methodName, entityType, entityKey});
        return ALL_OK;
    }

    private static String checkICPEntityAccess(String originatingIdentity, String methodName, String entityType, long entityKey) throws EntityAuthorisationException {
        log.debug("In verifyICPAllowedAccess [{}] [{}] [{}] [{}]", new Object[]{originatingIdentity, methodName, entityType, entityKey});
        /**
         * ICPs access to entities should be as follows.
         *
         * Accounts: Accounts used by services they own or Owned by Customers
         * who's account manager belongs to their Organisation. All else denied
         * When doing a transfer, it must be their account. When doing a simple
         * view of an account or a bundle purchase all is allowed
         *
         * Customer Profiles: Their own profile or Customers who's account
         * manager belongs to their Organisation. All else Obviscated
         *
         * Service Instances: All is Ok as there is nothing personal in them
         *
         * Product Instances: All is Ok as there is nothing personal in them
         *
         * Organisations: None
         */
        if (entityType.equals(ENTITY_PRODUCT_INSTANCE) || entityType.equals(ENTITY_SERVICE_INSTANCE)) {
            return ALL_OK;
        }

        if (entityType.equals(ENTITY_ACCOUNT)) {

            // Just to be extra safe
            if (entityKey < 1100000000) {
                throw new EntityAuthorisationException(true);
            }

            if (methodName.equals("transferBalance")) {
                if (doesCustomerOwnAccount(originatingIdentity, entityKey)) {
                    return ALL_OK;
                }
            } else if (methodName.equals("purchaseUnitCredit")) {
                return ALL_OK;
            } else if (methodName.equals("getAccounts")) {
                return ALL_OK;
            } else if (isICPAllowedToViewAccount(originatingIdentity, entityKey)) {
                return ALL_OK;
            }
        }

        if (entityType.equals(ENTITY_CUSTOMER)) {
            if (canICPSeeCustomersDetails(originatingIdentity, (int) entityKey) || isCustomerPending((int) entityKey)) {
                return ALL_OK;
            } else if (methodName.equals("modifyCustomer")) {
                log.debug("Not allowing modification of customer profile");
            } else {
                return OBVISCATE;
            }
        }

        if (entityType.equals(ENTITY_ORGANISATION)) {
            if (canICPSeeOrganisationsDetails(originatingIdentity, (int) entityKey)) {
                return ALL_OK;
            } else {
                return OBVISCATE;
            }
        }
        throw new EntityAuthorisationException(false);
    }

    private static String checkDeliveryEntityAccess(String originatingIdentity, String methodName, String entityType, long entityKey) throws EntityAuthorisationException {
        log.debug("In verifyDeliveryAllowedAccess [{}] [{}] [{}] [{}]", new Object[]{originatingIdentity, methodName, entityType, entityKey});
        /**
         * Delivery persons access to entities should be as follows.
         *
         * Accounts: None
         *
         * Customer Profiles: Only edit pending profiles
         *
         * Service Instances: All is Ok as there is nothing personal in them
         *
         * Product Instances: All is Ok as there is nothing personal in them
         *
         * Organisations: Obviscate
         */
        if (entityType.equals(ENTITY_PRODUCT_INSTANCE) || entityType.equals(ENTITY_SERVICE_INSTANCE)) {
            return ALL_OK;
        }

        if (entityType.equals(ENTITY_CUSTOMER)) {
            if (isCustomerPending((int) entityKey)) {
                return ALL_OK;
            } else {
                return OBVISCATE;
            }
        }

        if (entityType.equals(ENTITY_ORGANISATION)) {
            return OBVISCATE;
        }

        throw new EntityAuthorisationException(false);
    }

    private static Map<String, Set<Long>> getCustomersAllowedEntityMap(String originatingIdentity) {
        return getCustomersAllowedEntityMap(originatingIdentity, false);
    }

    private static Map<String, Set<Long>> getCustomersAllowedEntityMap(String originatingIdentity, boolean refreshCache) {

        String cacheKey = "EM:" + originatingIdentity;
        Map<String, Set<Long>> customersEntityData = (Map<String, Set<Long>>) CacheHelper.getFromRemoteCache(cacheKey);

        if (customersEntityData == null || refreshCache) {
            // - Initialize it ... 
            Set<Long> accountIdSet, specialAccountIdSet, customerIdSet, productInstanceIdSet, serviceInstanceIdSet, organisationIdSet;

            customersEntityData = new HashMap<>();

            CustomerQuery cq = new CustomerQuery();
            cq.setSSOIdentity(originatingIdentity);
            cq.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES);
            cq.setResultLimit(1);
            Customer cust = SCAWrapper.getAdminInstance().getCustomer(cq);

            accountIdSet = new HashSet<>();
            specialAccountIdSet = new HashSet<>();
            customerIdSet = new HashSet<>();
            productInstanceIdSet = new HashSet<>();
            serviceInstanceIdSet = new HashSet<>();
            organisationIdSet = new HashSet<>();

            // Allow seeing oneself
            customerIdSet.add(new Long(cust.getCustomerId()));

            for (CustomerRole role : cust.getCustomerRoles()) {
                organisationIdSet.add(new Long(role.organisationId));
            }

            for (ProductInstance pi : cust.getProductInstances()) {
                productInstanceIdSet.add(new Long(pi.getProductInstanceId()));
                // We must allow a customer to see product instances that share accounts with their own service instances
                for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
                    ServiceInstance si = m.getServiceInstance();
                    accountIdSet.add(si.getAccountId());
                    if (si.getServiceSpecificationId() >= 1000 || si.getServiceSpecificationId() == 15) {
                        // 15 is corporate airtime account
                        specialAccountIdSet.add(si.getAccountId());
                    }
                    customerIdSet.add(new Long(si.getCustomerId()));
                    serviceInstanceIdSet.add(new Long(si.getServiceInstanceId()));
                }
            }

            // Add all the products and services and customers under the accounts the customer can see
            for (long accountId : accountIdSet) {
                Account acc = SCAWrapper.getAdminInstance().getAccount(accountId, StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
                for (ServiceInstance si : acc.getServiceInstances()) {
                    productInstanceIdSet.add(new Long(si.getProductInstanceId()));
                    serviceInstanceIdSet.add(new Long(si.getServiceInstanceId()));
                    customerIdSet.add(new Long(si.getCustomerId()));
                }
            }

            customersEntityData.put(ENTITY_ACCOUNT, accountIdSet);
            customersEntityData.put(ENTITY_SPECIAL_ACCOUNT, specialAccountIdSet);
            customersEntityData.put(ENTITY_PRODUCT_INSTANCE, productInstanceIdSet);
            customersEntityData.put(ENTITY_SERVICE_INSTANCE, serviceInstanceIdSet);
            customersEntityData.put(ENTITY_CUSTOMER, customerIdSet);
            customersEntityData.put(ENTITY_ORGANISATION, organisationIdSet);

            CacheHelper.putInRemoteCache(cacheKey, customersEntityData, 300);
            log.debug("Put customer [{}] AllowedEntityMap in the cache", originatingIdentity);
        } else {
            log.debug("Got customer [{}] AllowedEntityMap from the cache", originatingIdentity);
        }

        return customersEntityData;
    }

    // Return true if there is an overlap between the Accounts' owners account managers organisations and the calling identities organisations
    // Or if the ICP owns the account
    // Or if the accounts product instances and ICP share an organisation
    private static boolean isICPAllowedToViewAccount(String originatingIdentity, long accountId) {
        log.debug("Checking if ICP [{}] can view Account [{}]", originatingIdentity, accountId);
        Set<Long> icpsAccounts = getCustomersAllowedEntityMap(originatingIdentity).get(ENTITY_ACCOUNT);
        if (icpsAccounts.contains(accountId)) {
            log.debug("ICP owns this account");
            return true;
        }
        Set<Long> icpsOrganisationIDs = getCustomersAllowedEntityMap(originatingIdentity).get(ENTITY_ORGANISATION);
        Set<Integer> accountsAccountManagersOrganisationIds = getAccountsCustomersAccountManagersOrganisationIds(accountId);
        for (long icpOrgId : icpsOrganisationIDs) {
            for (long amOrgId : accountsAccountManagersOrganisationIds) {
                if (icpOrgId == amOrgId) {
                    log.debug("Yes they can - Accounts customers account managers organisation overlap");
                    return true;
                }
            }
        }
        Set<Integer> accountOwnersOrganisationIDs = getAccountsOrganisationIds(accountId);
        for (long icpOrgId : icpsOrganisationIDs) {
            for (int accOrgId : accountOwnersOrganisationIDs) {
                if (icpOrgId == accOrgId) {
                    log.debug("Yes they can - Accounts products organisation overlap ICP organisations");
                    return true;
                }
            }
        }
        log.debug("No they cant");
        return false;
    }

    // Return true if there is an overlap between the Customers account managers organisations and the calling identities organisations
    // Or if the customer is in the same Organisation as the ICP
    private static boolean canICPSeeCustomersDetails(String originatingIdentity, int customerIdToView) {
        log.debug("Checking if ICP [{}] can view Customer Details [{}]", originatingIdentity, customerIdToView);
        Set<Long> icpsOrganisationIDs = getCustomersAllowedEntityMap(originatingIdentity).get(ENTITY_ORGANISATION);
        Set<Integer> customersOrganisationIDs = getCustomersOrganisationIds(customerIdToView);
        for (long icpOrgId : icpsOrganisationIDs) {
            for (long custOrgId : customersOrganisationIDs) {
                if (icpOrgId == custOrgId) {
                    log.debug("Yes they can - They both belong to the same Organisation");
                    return true;
                }
            }
        }
        Set<Integer> customersAccountManagersOrganisationIds = getCustomersAccountManagersOrganisationIds(customerIdToView);
        for (long icpOrgId : icpsOrganisationIDs) {
            for (long amOrgId : customersAccountManagersOrganisationIds) {
                if (icpOrgId == amOrgId) {
                    log.debug("Yes they can - Organisation overlap");
                    return true;
                }
            }
        }
        log.debug("No they cant");
        return false;
    }

    // Return true if there is an overlap between the Organisations account managers organisations and the calling identities organisations
    // Or if the ICP is a member of the Organisation
    private static boolean canICPSeeOrganisationsDetails(String originatingIdentity, int organisationId) {
        log.debug("Checking if ICP [{}] can view Organisations Details [{}]", originatingIdentity, organisationId);
        Set<Long> icpsOrganisationIDs = getCustomersAllowedEntityMap(originatingIdentity).get(ENTITY_ORGANISATION);
        if (icpsOrganisationIDs.contains((long) organisationId)) {
            log.debug("ICP is in this Organisation");
            return true;
        }
        Set<Integer> organisationsAccountManagersOrganisationIds = getOrganisationsAccountManagersOrganisationIds(organisationId);
        for (long icpOrgId : icpsOrganisationIDs) {
            for (long amOrgId : organisationsAccountManagersOrganisationIds) {
                if (icpOrgId == amOrgId) {
                    log.debug("Yes they can - Organisation overlap");
                    return true;
                }
            }
        }
        log.debug("No they cant");
        return false;
    }

    private static boolean doesCustomerOwnAccount(String originatingIdentity, long accountId) {
        log.debug("Checking if ICP [{}] can transfer out of account [{}]", originatingIdentity, accountId);
        boolean res = getCustomersAllowedEntityMap(originatingIdentity).get(ENTITY_ACCOUNT).contains(accountId);
        log.debug("Result [{}]", res);
        return res;
    }

    private static boolean areTheyAccessingTheirOwnCustomerProfile(String originatingIdentity, long entityId) {
        CustomerQuery cq = new CustomerQuery();
        cq.setCustomerId((int) entityId);
        cq.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        cq.setResultLimit(1);
        Customer cust = SCAWrapper.getAdminInstance().getCustomer(cq);

        if (cust != null) {
            return cust.getSSOIdentity().equals(originatingIdentity);
        }

        return false;
    }

    private static Set<Integer> getCustomersAccountManagersOrganisationIds(int customerId) {
        log.debug("In getCustomersAccountManagersOrganisationIds [{}]", customerId);
        String cacheKey = "CAMORGIDS:" + customerId;
        Set<Integer> res = (Set<Integer>) CacheHelper.getFromRemoteCache(cacheKey);
        if (res == null) {
            res = new HashSet<>();
            try {
                CustomerQuery cq = new CustomerQuery();
                cq.setCustomerId(customerId);
                cq.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
                cq.setResultLimit(1);
                Customer cust = SCAWrapper.getAdminInstance().getCustomer(cq);
                int accManCustomerId = cust.getAccountManagerCustomerProfileId();
                CustomerQuery cqAccMan = new CustomerQuery();
                cqAccMan.setCustomerId(accManCustomerId);
                cqAccMan.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
                cqAccMan.setResultLimit(1);
                Customer custAccMan = SCAWrapper.getAdminInstance().getCustomer(cqAccMan);
                for (CustomerRole role : custAccMan.getCustomerRoles()) {
                    log.debug("Customer [{}] has an Account Manager in Organisation [{}]", customerId, role.organisationId);
                    res.add(role.organisationId);
                }
                CacheHelper.putInRemoteCache(cacheKey, res, 300);
            } catch (Exception e) {
                log.warn("Error getting Customers Account Managers Organisation Ids. Probably the account manager does not exist : [{}]", e.toString());
            }
        } else {
            log.debug("getCustomersAccountManagersOrganisationIds got a cache hit");
        }
        log.debug("Finished getCustomersAccountManagersOrganisationIds [{}]", customerId);
        return res;
    }

    private static Set<Integer> getCustomersOrganisationIds(int customerId) {
        log.debug("In getCustomersOrganisationIds [{}]", customerId);
        String cacheKey = "CORGIDS:" + customerId;
        Set<Integer> res = (Set<Integer>) CacheHelper.getFromRemoteCache(cacheKey);
        if (res == null) {
            res = new HashSet<>();
            try {
                CustomerQuery cq = new CustomerQuery();
                cq.setCustomerId(customerId);
                cq.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
                cq.setResultLimit(1);
                Customer cust = SCAWrapper.getAdminInstance().getCustomer(cq);
                for (CustomerRole role : cust.getCustomerRoles()) {
                    log.debug("Customer [{}] is in Organisation [{}]", customerId, role.organisationId);
                    res.add(role.organisationId);
                }
                CacheHelper.putInRemoteCache(cacheKey, res, 300);
            } catch (Exception e) {
                log.warn("Error getting Customers Organisation Ids: [{}]", e.toString());
            }
        } else {
            log.debug("getCustomersOrganisationIds got a cache hit");
        }
        log.debug("Finished getCustomersOrganisationIds [{}]", customerId);
        return res;
    }

    private static boolean isCustomerPending(int customerId) {
        log.debug("In isCustomerPending [{}]", customerId);
        String cacheKey = "ISPEND:" + customerId;
        Boolean res = (Boolean) CacheHelper.getFromRemoteCache(cacheKey);
        if (res == null) {
            try {
                CustomerQuery cq = new CustomerQuery();
                cq.setCustomerId(customerId);
                cq.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
                cq.setResultLimit(1);
                Customer cust = SCAWrapper.getAdminInstance().getCustomer(cq);
                res = cust.getClassification().equals("pending");
                CacheHelper.putInRemoteCache(cacheKey, res, 300);
            } catch (Exception e) {
                log.warn("Error getting Customers Classification [{}]", e.toString());
            }
        } else {
            log.debug("isCustomerPending got a cache hit");
        }
        log.debug("Finished isCustomerPending [{}][{}]", customerId, res);
        return res;
    }

    private static Set<Integer> getOrganisationsAccountManagersOrganisationIds(int organisationId) {
        log.debug("In getOrganisationsAccountManagersOrganisationIds [{}]", organisationId);
        String cacheKey = "OAMORGIDS:" + organisationId;
        Set<Integer> res = (Set<Integer>) CacheHelper.getFromRemoteCache(cacheKey);
        if (res == null) {
            res = new HashSet<>();
            try {
                OrganisationQuery oq = new OrganisationQuery();
                oq.setOrganisationId(organisationId);
                oq.setVerbosity(StOrganisationLookupVerbosity.MAIN);
                oq.setResultLimit(1);
                Organisation org = SCAWrapper.getAdminInstance().getOrganisation(oq);
                int accManCustomerId = org.getAccountManagerCustomerProfileId();
                CustomerQuery cqAccMan = new CustomerQuery();
                cqAccMan.setCustomerId(accManCustomerId);
                cqAccMan.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
                cqAccMan.setResultLimit(1);
                Customer custAccMan = SCAWrapper.getAdminInstance().getCustomer(cqAccMan);

                for (CustomerRole role : custAccMan.getCustomerRoles()) {
                    log.debug("Organisation [{}] has an Account Manager in Organisation [{}]", organisationId, role.organisationId);
                    res.add(role.organisationId);
                }
                CacheHelper.putInRemoteCache(cacheKey, res, 300);
            } catch (Exception e) {
                log.warn("Error getting Organisations Account Managers Organisation Ids. Probably the account manager does not exist : [{}]", e.toString());
            }
        } else {
            log.debug("getOrganisationsAccountManagersOrganisationIds got a cache hit");
        }
        log.debug("Finished getOrganisationsAccountManagersOrganisationIds [{}]", organisationId);
        return res;
    }

    private static Set<Integer> getAccountsCustomersAccountManagersOrganisationIds(long accountId) {
        log.debug("In getAccountsCustomersAccountManagersOrganisationIds [{}]", accountId);
        String cacheKey = "AAMORGIDS:" + accountId;
        Set<Integer> res = (Set<Integer>) CacheHelper.getFromRemoteCache(cacheKey);
        if (res == null) {
            res = new HashSet<>();
            try {
                ServiceInstanceQuery siq = new ServiceInstanceQuery();
                siq.setAccountId(accountId);
                siq.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
                ServiceInstanceList sis = SCAWrapper.getAdminInstance().getServiceInstances(siq);
                for (ServiceInstance si : sis.getServiceInstances()) {
                    log.debug("Account [{}] has an owner [{}]", accountId, si.getCustomerId());
                    res.addAll(getCustomersAccountManagersOrganisationIds(si.getCustomerId()));
                }
                CacheHelper.putInRemoteCache(cacheKey, res, 300);
            } catch (Exception e) {
                log.warn("Error getting Accounts Owners Account Managers Organisation Ids. Probably the account manager does not exist : [{}]", e.toString());
            }
        } else {
            log.debug("getAccountsCustomersAccountManagersOrganisationIds got a cache hit");
        }
        log.debug("Finished getAccountsCustomersAccountManagersOrganisationIds [{}]", accountId);
        return res;
    }

    // The ord ids of product instances related to the account
    private static Set<Integer> getAccountsOrganisationIds(long accountId) {
        log.debug("In getAccountsOrganisationIds [{}]", accountId);
        String cacheKey = "ACCORGIDS:" + accountId;
        Set<Integer> res = (Set<Integer>) CacheHelper.getFromRemoteCache(cacheKey);
        if (res == null) {
            res = new HashSet<>();
            try {
                ServiceInstanceQuery siq = new ServiceInstanceQuery();
                siq.setAccountId(accountId);
                siq.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
                ServiceInstanceList sis = SCAWrapper.getAdminInstance().getServiceInstances(siq);
                for (ServiceInstance si : sis.getServiceInstances()) {
                    log.debug("Account [{}] has a SI [{}]", accountId, si.getServiceInstanceId());
                    int piOrgId = getProductInstancesOrganisationId(si.productInstanceId);
                    if (piOrgId > 0) {
                        res.add(piOrgId);
                    }
                }
                CacheHelper.putInRemoteCache(cacheKey, res, 300);
            } catch (Exception e) {
                log.warn("Error getting Accounts Product Instances Organisation Ids : [{}]", e.toString());
            }
        } else {
            log.debug("getAccountsOrganisationIds got a cache hit");
        }
        log.debug("Finished getAccountsOrganisationIds [{}]", accountId);
        return res;
    }

    private static Integer getProductInstancesOrganisationId(int productInstanceId) {
        log.debug("In getProductInstancesOrganisationId [{}]", productInstanceId);
        String cacheKey = "PIORGID:" + productInstanceId;
        Integer res = (Integer) CacheHelper.getFromRemoteCache(cacheKey);
        if (res == null) {
            try {
                ProductInstanceQuery piq = new ProductInstanceQuery();
                piq.setProductInstanceId(productInstanceId);
                piq.setVerbosity(StProductInstanceLookupVerbosity.MAIN);
                piq.setResultLimit(1);
                ProductInstanceList pis = SCAWrapper.getAdminInstance().getProductInstances(piq);
                res = pis.getProductInstances().get(0).getOrganisationId();
                CacheHelper.putInRemoteCache(cacheKey, res, 300);
                log.debug("PI [{}] has org Id [{}]", productInstanceId, res);
            } catch (Exception e) {
                log.warn("Error getting Product Instances Organisation Id : [{}[", e.toString());
            }
        } else {
            log.debug("getProductInstancesOrganisationId got a cache hit");
        }
        log.debug("Finished getProductInstancesOrganisationId [{}] [{}]", productInstanceId, res);
        return (res == null ? 0 : res);
    }

    private static String checkProvisionUnitCreditSpec(Set<String> callersRoles, String identifier) throws EntityAuthorisationException {
        log.debug("In checkProvisionUnitCreditSpec [{}]", callersRoles);
        if (callersRoles.size() == 1 && (callersRoles.contains("Batch") || callersRoles.contains("Administrator"))) {
            log.debug("This is a batch program or administrator so access will be allowed");
            return ALL_OK;
        }
        String ucAllowedroles;
        String cacheKey = "UCSPECROLES:" + identifier;
        ucAllowedroles = (String) CacheHelper.getFromRemoteCache(cacheKey);
        if (ucAllowedroles == null) {
            try {
                if (Utils.isNumeric(identifier)) {
                    ucAllowedroles = NonUserSpecificCachedDataHelper.getUnitCreditSpecification(Integer.parseInt(identifier)).getPurchaseRoles();
                } else {
                    ucAllowedroles = NonUserSpecificCachedDataHelper.getUnitCreditSpecificationByItemNumber(identifier).getPurchaseRoles();
                }
            } catch (SCABusinessError sbe) {
                log.debug("Uc [{}] does not exist", identifier);
                ucAllowedroles = "UC DOES NOT EXIST";
            }
            CacheHelper.putInRemoteCache(cacheKey, ucAllowedroles, 300);
        }
        if (!Utils.listsIntersect(callersRoles, Utils.getListFromCRDelimitedString(ucAllowedroles))) {
            throw new EntityAuthorisationException(false);
        }
        log.debug("Finished checkProvisionUnitCreditSpec [{}]", callersRoles);
        return ALL_OK;
    }

}
