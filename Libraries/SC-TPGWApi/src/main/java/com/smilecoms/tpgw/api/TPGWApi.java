/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.tpgw.api;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.Account;
import com.smilecoms.commons.sca.AccountQuery;
import com.smilecoms.commons.sca.AuthenticationQuery;
import com.smilecoms.commons.sca.BalanceTransferData;
import com.smilecoms.commons.sca.CallersRequestContext;
import com.smilecoms.commons.sca.CashInData;
import com.smilecoms.commons.sca.CashInList;
import com.smilecoms.commons.sca.CashInQuery;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.CustomerQuery;
import com.smilecoms.commons.sca.Done;
import com.smilecoms.commons.sca.Event;
import com.smilecoms.commons.sca.EventList;
import com.smilecoms.commons.sca.EventQuery;
import com.smilecoms.commons.sca.InventoryItem;
import com.smilecoms.commons.sca.PaymentNotificationData;
import com.smilecoms.commons.sca.ProductInstance;
import com.smilecoms.commons.sca.ProductServiceInstanceMapping;
import com.smilecoms.commons.sca.PurchaseUnitCreditRequest;
import com.smilecoms.commons.sca.SCABusinessError;
import com.smilecoms.commons.sca.SCAInteger;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.Sale;
import com.smilecoms.commons.sca.SaleLine;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.ServiceInstanceList;
import com.smilecoms.commons.sca.ServiceInstanceQuery;
import com.smilecoms.commons.sca.StAccountLookupVerbosity;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.StDone;
import com.smilecoms.commons.sca.StProductInstanceLookupVerbosity;
import com.smilecoms.commons.sca.StSaleLookupVerbosity;
import com.smilecoms.commons.sca.StServiceInstanceLookupVerbosity;
import com.smilecoms.commons.sca.StUnitCreditSpecificationLookupVerbosity;
import com.smilecoms.commons.sca.ThirdPartyAuthorisationRule;
import com.smilecoms.commons.sca.ThirdPartyAuthorisationRuleSet;
import com.smilecoms.commons.sca.ThirdPartyAuthorisationRuleSetList;
import com.smilecoms.commons.sca.TransactionStatusResult;
import com.smilecoms.commons.sca.UnitCreditSpecification;
import com.smilecoms.commons.sca.UnitCreditSpecificationQuery;
import com.smilecoms.commons.sca.beans.NonUserSpecificCachedDataHelper;
import com.smilecoms.commons.base.cache.CacheHelper;
import com.smilecoms.commons.sca.NewTTIssue;
import com.smilecoms.commons.sca.direct.pvs.PrepaidStrip;
import com.smilecoms.commons.sca.direct.pvs.PrepaidStripQuery;
import com.smilecoms.commons.sca.ShortMessage;
import com.smilecoms.commons.sca.TTIssue;
import com.smilecoms.commons.sca.beans.ServiceBean;
import com.smilecoms.commons.sca.direct.pvs.PrepaidStripRedemptionData;
import com.smilecoms.commons.util.Codec;
import com.smilecoms.commons.util.ExceptionManager;
import com.smilecoms.commons.util.FriendlyException;
import com.smilecoms.commons.util.IMAPUtils;
import com.smilecoms.commons.util.Utils;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.slf4j.*;
import org.w3c.dom.Document;

/**
 *
 * @author mukosi
 */
public class TPGWApi {

    private static final TPGWApi myInstance = new TPGWApi();
    private static final Logger log = LoggerFactory.getLogger(TPGWApi.class.getName());
    private static final String STATUS_SUCCESSFUL = "SUCCESSFUL";
    private static final String STATUS_NOT_FOUND = "NOT_FOUND";
    private static final String STATUS_FAILED = "FAILED";
    private static final String SALE_TYPE_PREFIX_CASH_IN = "ci";
    private static final String PAYMENT_STATUS_PAID = "PD";
    private static final String PAYMENT_METHOD_BANK_TRANSFER = "Bank Transfer";
    private static final String PAYMENT_METHOD_CARD_INTEGRATION = "Card Integration";
    private static final String PAYMENT_METHOD_CHEQUE = "Cheque";
    private static String SALE_TYPE_PREFIX_CHEQUE_OR_BANK_TRANSFER = "";
    private static final Pattern errorInfoPattern = Pattern.compile("ERROR\\-INFO=" + "*.*"), statusPattern = Pattern.compile("STATUS=" + "\\s?[a-zA-Z0-9]*");

    public static TPGWApi getInstance() {
        return myInstance;
    }

    private void initialiseSCAForCaller(CallersRequestContext ctx) {
        SCAWrapper.getUserSpecificInstance().setThreadsRequestContext(ctx);
    }

    public String authenticateUser(String sessionId, String username, String password) throws TPGWApiError {

        if (password == null) {
            throw getTPGWApiError(new Exception("Invalid login credentials supplied"));
        }
        AuthenticationQuery authenticationQuery = new AuthenticationQuery();
        com.smilecoms.commons.sca.AuthenticationResult scaAuthResult;

        authenticationQuery.setSSOIdentity(username);
        authenticationQuery.setSSOEncryptedPassword(Codec.stringToEncryptedHexString(password));

        try {
            boolean done;
            scaAuthResult = SCAWrapper.getAdminInstance().authenticate(authenticationQuery);
            done = scaAuthResult.getDone().equals(com.smilecoms.commons.sca.StDone.TRUE);

            if (!done) {
                // Failed authentication
                log.info("Invalid credentials while logging in user in SCA");
                throw new Exception("Invalid login credentials supplied");
            }

            // Get customer Id for the username
            CustomerQuery customerQuery = new CustomerQuery();
            customerQuery.setSSOIdentity(username);
            customerQuery.setResultLimit(1);
            customerQuery.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
            Integer customerId = SCAWrapper.getAdminInstance().getCustomer(customerQuery).getCustomerId();
            CallersRequestContext ctx = new CallersRequestContext(username, "0.0.0.0", customerId, scaAuthResult.getSecurityGroups());
            log.debug("Customer with username [{}] has customer profile id [{}]. This is being put in remote cache under session id [{}]", new Object[]{username, customerId, sessionId});
            CacheHelper.putInRemoteCacheSync(sessionId, ctx, BaseUtils.getIntProperty("env.tpgw.session.expiry.seconds"));
            log.debug("Customer with username [{}] has customer profile id [{}]. successfully put in remote cache under session id [{}]", new Object[]{username, customerId, sessionId});

            initialiseSCAForCaller(ctx);
            return sessionId;

        } catch (Exception ex) {
            log.info("Error logging in user in TPGW: " + ex.toString());
            throw getTPGWApiError(ex); // getTPGWError(ex, ex.getMessage());
        }
    }

    public Customer validateAccount(long accountId) throws Exception {
        try {

            if (accountId <= 1200000000) {
                throw new Exception("No such customer -- [AccountId: " + accountId + "]");
            }

            Set<String> accountsNotallowed;
            try {
                accountsNotallowed = BaseUtils.getPropertyAsSet("env.tpgw.accounts.validate.fail");
            } catch (Exception e) {
                log.debug("env.tpgw.accounts.validate.fail does not exist");
                accountsNotallowed = new HashSet<>();
            }

            if (accountsNotallowed.contains(String.valueOf(accountId))) {
                throw new Exception("No such customer -- [AccountId: " + accountId + "]");
            }

            // PCB - ensure account can take a credit and buy a bundle or else say its invalid
            Account acc = SCAWrapper.getAdminInstance().getAccount(accountId, StAccountLookupVerbosity.ACCOUNT);
            if ((1 & acc.getStatus()) != 0 || (2 & acc.getStatus()) != 0) {
                throw new Exception("Invalid customer account detected -- Account Status is " + acc.getStatus() + " which does not allow debiting and crediting");
            }

            ServiceInstanceQuery serviceInstanceQuery = new ServiceInstanceQuery();

            serviceInstanceQuery.setAccountId(accountId);
            serviceInstanceQuery.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
            ServiceInstanceList serviceInstances = SCAWrapper.getUserSpecificInstance().getServiceInstances(serviceInstanceQuery);

            // - Get the first service instance, extract the customer and use that to pull the profile
            if (serviceInstances.getServiceInstances().size() <= 0) {
                throw new Exception("No such customer -- [AccountId: " + accountId + "]");
            }

            //Check that account has no service instance >= 1000 - to prevent Staff accounts from transacting
            for (ServiceInstance pi : serviceInstances.getServiceInstances()) {
                if (pi.getServiceSpecificationId() >= 1000) {
                    throw new Exception("Invalid customer account detected -- [ServiceInstanceId " + pi.getServiceInstanceId()
                            + ", ServiceInstanceSpecificationId: " + pi.getServiceSpecificationId()
                            + ", AccountId: " + accountId);
                }
            }

            ServiceInstance si = serviceInstances.getServiceInstances().get(0);
            CustomerQuery customerQuery = new CustomerQuery();
            customerQuery.setCustomerId(si.getCustomerId());

            log.debug("Looking up customer with id [{}]", customerQuery.getCustomerId());
            customerQuery.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
            Customer customer = SCAWrapper.getUserSpecificInstance().getCustomer(customerQuery);
            return customer;

        } catch (Exception ex) {
            // throw ex;
            log.warn("Error while trying to validate account with acccount id - [{}] - error message : [{}]", new Object[]{accountId, ex.getMessage()});
            throw getTPGWApiError(ex);
        }

    }

    public int getCustomerIdByAccountId(long accountId) throws Exception {
        try {

            if (accountId <= 1200000000) {
                throw new Exception("No such customer -- [AccountId: " + accountId + "]");
            }

            Set<String> accountsNotallowed;
            try {
                accountsNotallowed = BaseUtils.getPropertyAsSet("env.tpgw.accounts.validate.fail");
            } catch (Exception e) {
                log.debug("env.tpgw.accounts.validate.fail does not exist");
                accountsNotallowed = new HashSet<>();
            }

            if (accountsNotallowed.contains(String.valueOf(accountId))) {
                throw new Exception("No such customer -- [AccountId: " + accountId + "]");
            }

            // PCB - ensure account can take a credit and buy a bundle or else say its invalid
            Account acc = SCAWrapper.getAdminInstance().getAccount(accountId, StAccountLookupVerbosity.ACCOUNT);
            if ((1 & acc.getStatus()) != 0 || (2 & acc.getStatus()) != 0) {
                throw new Exception("Invalid customer account detected -- Account Status is " + acc.getStatus() + " which does not allow debiting and crediting");
            }

            ServiceInstanceQuery serviceInstanceQuery = new ServiceInstanceQuery();

            serviceInstanceQuery.setAccountId(accountId);
            serviceInstanceQuery.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
            ServiceInstanceList serviceInstances = SCAWrapper.getUserSpecificInstance().getServiceInstances(serviceInstanceQuery);

            // - Get the first service instance, extract the customer and use that to pull the profile
            if (serviceInstances.getServiceInstances().size() <= 0) {
                throw new Exception("No such customer -- [AccountId: " + accountId + "]");
            }

            //Check that account has no service instance >= 1000 - to prevent Staff accounts from transacting
            for (ServiceInstance pi : serviceInstances.getServiceInstances()) {
                if (pi.getServiceSpecificationId() >= 1000) {
                    throw new Exception("Invalid customer account detected -- [ServiceInstanceId " + pi.getServiceInstanceId()
                            + ", ServiceInstanceSpecificationId: " + pi.getServiceSpecificationId()
                            + ", AccountId: " + accountId);
                }
            }

            ServiceInstance si = serviceInstances.getServiceInstances().get(0);
            CustomerQuery customerQuery = new CustomerQuery();
            customerQuery.setCustomerId(si.getCustomerId());

            log.debug("Looking up customer with id [{}]", customerQuery.getCustomerId());
            customerQuery.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
            Customer customer = SCAWrapper.getUserSpecificInstance().getCustomer(customerQuery);
            return customer.getCustomerId();

        } catch (Exception ex) {
            // throw ex;
            log.warn("Error while trying to validate account with acccount id - [{}] - error message : [{}]", new Object[]{accountId, ex.getMessage()});
            throw getTPGWApiError(ex);
        }

    }

    public Customer getCustomerByAccountIdOrSmileNumber(String accountIdOrPhoneNumber) throws Exception {
        try {
            long accountId = 0;            
            if (accountIdOrPhoneNumber.startsWith("0")) {
                log.warn("**********Yes************");
                ServiceBean service = ServiceBean.getServiceInstanceByIMPU(accountIdOrPhoneNumber);
                accountId = service.getAccountId();                
            }else{
                accountId = Long.parseLong(accountIdOrPhoneNumber);
            }

            if (accountId <= 1200000000) {
                throw new Exception("No such customer -- [AccountId: " + accountId + "]");
            }

            Set<String> accountsNotallowed;
            try {
                accountsNotallowed = BaseUtils.getPropertyAsSet("env.tpgw.accounts.validate.fail");
            } catch (Exception e) {
                log.debug("env.tpgw.accounts.validate.fail does not exist");
                accountsNotallowed = new HashSet<>();
            }

            if (accountsNotallowed.contains(String.valueOf(accountId))) {
                throw new Exception("No such customer -- [AccountId: " + accountId + "]");
            }

            // PCB - ensure account can take a credit and buy a bundle or else say its invalid
            Account acc = SCAWrapper.getAdminInstance().getAccount(accountId, StAccountLookupVerbosity.ACCOUNT);
            if ((1 & acc.getStatus()) != 0 || (2 & acc.getStatus()) != 0) {
                throw new Exception("Invalid customer account detected -- Account Status is " + acc.getStatus() + " which does not allow debiting and crediting");
            }

            ServiceInstanceQuery serviceInstanceQuery = new ServiceInstanceQuery();

            serviceInstanceQuery.setAccountId(accountId);
            serviceInstanceQuery.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
            ServiceInstanceList serviceInstances = SCAWrapper.getUserSpecificInstance().getServiceInstances(serviceInstanceQuery);

            // - Get the first service instance, extract the customer and use that to pull the profile
            if (serviceInstances.getServiceInstances().size() <= 0) {
                throw new Exception("No such customer -- [AccountId: " + accountId + "]");
            }

            //Check that account has no service instance >= 1000 - to prevent Staff accounts from transacting
            for (ServiceInstance pi : serviceInstances.getServiceInstances()) {
                if (pi.getServiceSpecificationId() >= 1000) {
                    throw new Exception("Invalid customer account detected -- [ServiceInstanceId " + pi.getServiceInstanceId()
                            + ", ServiceInstanceSpecificationId: " + pi.getServiceSpecificationId()
                            + ", AccountId: " + accountId);
                }
            }

            ServiceInstance si = serviceInstances.getServiceInstances().get(0);
            CustomerQuery q = new CustomerQuery();
            q.setCustomerId(si.getCustomerId());
            q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
            q.setResultLimit(1);
            Customer customer = SCAWrapper.getAdminInstance().getCustomer(q);
            return customer;

        } catch (Exception ex) {
            // throw ex;
            log.warn("Error while trying to validate account with acccount id - [{}] - error message : [{}]", new Object[]{accountIdOrPhoneNumber, ex.getMessage()});
            throw getTPGWApiError(ex);
        }

    }

    public ValidateEmailAddressResult validateEmailAddress(String emailAddress, String sessionId) throws Exception {
        try {

            ValidateEmailAddressResult result = new ValidateEmailAddressResult();
            List<Account> accountList = new ArrayList<>();
            Set<Long> accountIds = new HashSet<>();
            Map<Long, String> friendlyNameKeyedByAccountId = new HashMap<>();

            CustomerQuery customerQuery = new CustomerQuery();
            customerQuery.setEmailAddress(emailAddress);
            customerQuery.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES);

            Customer customer;
            try {
                log.debug("Looking up customer with email [{}]", customerQuery.getEmailAddress());
                customer = SCAWrapper.getAdminInstance().getCustomer(customerQuery);
            } catch (Exception ex) {
                log.warn("Failed to retrieve customer: ", ex);
                throw new Exception("No such customer -- [Email address: " + emailAddress + "]");
            }

            String invalidAccountMsg = "Invalid account error";
            boolean invalidAccountExist = false;

            for (ProductInstance pi : customer.getProductInstances()) {
                for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
                    ServiceInstance si = m.getServiceInstance();
                    if (si.getCustomerId() == customer.getCustomerId()) {
                        if (si.getServiceSpecificationId() >= 1000) {
                            invalidAccountMsg = "Invalid customer account detected -- [ServiceInstanceId " + si.getServiceInstanceId()
                                    + ", ServiceInstanceSpecificationId: " + si.getServiceSpecificationId()
                                    + ", AccountId: " + si.getAccountId();
                            log.warn(invalidAccountMsg);
                            invalidAccountExist = true;
                            continue;
                        }
                        if (si.getAccountId() <= 1200000000) {
                            log.debug("Invalid customer account -- [AccountId: " + si.getAccountId() + "]");
                            continue;
                        }
                        accountIds.add(si.getAccountId());
                        String friendlyName = "";
                        if (pi.getFriendlyName() == null || pi.getFriendlyName().isEmpty()) {
                            //Recommendation from Cathy is that if not specified leave as is
                            //friendlyName = NonUserSpecificCachedDataHelper.getServiceSpecification(si.getServiceSpecificationId()).getName();
                        } else {
                            friendlyName = pi.getFriendlyName();
                        }
                        friendlyNameKeyedByAccountId.put(si.getAccountId(), friendlyName);
                    }
                }
            }

            Set<String> accountsNotallowed;
            try {
                accountsNotallowed = BaseUtils.getPropertyAsSet("env.tpgw.accounts.validate.fail");
            } catch (Exception e) {
                log.debug("env.tpgw.accounts.validate.fail does not exist");
                accountsNotallowed = new HashSet<>();
            }
            for (long accId : accountIds) {

                if (accountsNotallowed.contains(String.valueOf(accId))) {
                    friendlyNameKeyedByAccountId.remove(accId);
                    continue;
                }
                // PCB - ensure account can take a credit and buty a bundle or else say its invalid
                Account acc = SCAWrapper.getAdminInstance().getAccount(accId, StAccountLookupVerbosity.ACCOUNT);
                if ((1 & acc.getStatus()) != 0 || (2 & acc.getStatus()) != 0) {
                    invalidAccountMsg = "Invalid customer account detected -- Account " + acc.getAccountId() + ", Status is " + acc.getStatus() + " which does not allow debiting and crediting";
                    log.debug(invalidAccountMsg);
                    invalidAccountExist = true;
                    friendlyNameKeyedByAccountId.remove(accId);//remove invalid account on the map
                    continue;
                }
                accountList.add(acc);
            }

            if (accountList.size() <= 0) {
                if (invalidAccountExist) {
                    throw new Exception(invalidAccountMsg);
                }
                throw new Exception("No such customer -- [Email address: " + emailAddress + "]");
            }

            if (accountList.size() > 1) {
                if (checkIfEmailRechargeMultiAccountSupported(getUsername(sessionId))) {
                    throw new Exception("Partner interface does not support email recharge when there is more than one account.");
                }
            }
            //Deflate customer
            customer.getProductInstances().clear();
            customer.getAddresses().clear();

            result.setAccountIdFriendlyNameMapping(friendlyNameKeyedByAccountId);
            result.setCustomer(customer);

            return result;

        } catch (Exception ex) {
            log.warn("Error while trying to validate customer with email address - [{}] - error message : [{}]", new Object[]{emailAddress, ex.getMessage()});
            throw getTPGWApiError(ex);
        }
    }

    public void removeThreadsRequestContext() {
        SCAWrapper.removeThreadsRequestContext();
    }

    public Integer validateSession(String sessionId, String docType) throws Exception {
        // - Validate user session and update the expiry timestamp
        // - if session validate is successfull, return cutomer profile id

        log.debug("validateSession received request with SessionId: [{}]", sessionId);

        if (sessionId.isEmpty()) {
            throw new Exception("SessionId not specified in the request -- DocType [" + docType + "]");
        }

        // Resolve the SessionId to a customer profile id
        CallersRequestContext ctx = (CallersRequestContext) CacheHelper.getFromRemoteCache(sessionId);
        if (ctx == null) {
            log.debug("Invalid or expired session - SessionId [{}]", sessionId);
            throw new Exception("Invalid or expired session -- SessionId [" + sessionId + "]");
        }
        // Update the existing session and set a new expiry time ...
        CacheHelper.putInRemoteCacheSync(sessionId, ctx, BaseUtils.getIntProperty("env.tpgw.session.expiry.seconds"));
        initialiseSCAForCaller(ctx);
        return ctx.getCustomerProfileId();
    }

    public void authoriseUser(Integer customerId, Document requestDocument, NamespaceContext namespaceContext) throws Exception {

        if (log.isDebugEnabled()) {
            log.debug("In TPGWApi.authoriseUser(...)");
            log.debug(xmlDocumentToString(requestDocument));
        }
        // Checks to see if the user is authorised to execute/submit the request contained in the requestDocument
        // XML. Authorisation is checked against the list of configured authorisation rules.
        XPathFactory factory = XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
        if (namespaceContext != null) {
            xPath.setNamespaceContext(namespaceContext);
        }

        // 3. Use the customer id obtained above to retrieve the Authorisation rules for this user
        SCAInteger customerProfileId = new SCAInteger();
        customerProfileId.setInteger(customerId);
        // Retrieve the rules configured for this user here ...
        ThirdPartyAuthorisationRuleSetList tpAuthRuleSetList = SCAWrapper.getUserSpecificInstance().getThirdPartyAuthorisationRules(customerProfileId);
        // - Evaluate the rules
        boolean ruleSetMatches = false;
        for (ThirdPartyAuthorisationRuleSet ruleSet : tpAuthRuleSetList.getThirdPartyAuthorisationRuleSets()) {
            log.debug("Processing rule set [{}]", ruleSet.getRuleSetId());
            for (ThirdPartyAuthorisationRule rule : ruleSet.getThirdPartyAuthorisationRules()) {
                log.debug("Evaluating rule - [RuleId:{}, xPath:{}, RegEx:{}]", new Object[]{rule.getRuleId(), rule.getXQuery(), rule.getRegexMatch()});
                String value = evaluateXPath(requestDocument, xPath, rule.getXQuery());
                log.debug("Value extracted by xPath: [{}]", value);
                // - Evaluate the associated regular regular expression against the value obtained above.
                ruleSetMatches = value.matches(rule.getRegexMatch());
                if (!ruleSetMatches) {
                    log.debug("Rule FAILED - [RuleId:{}]", rule.getRuleId());
                    break;
                } else {
                    log.debug("Rule PASSED - [RuleId:{}]", rule.getRuleId());
                }
            }
            if (ruleSetMatches) {// - Exit as soon as a matching rules is found
                break;
            }
        }

        if (!ruleSetMatches) { // No matching rule set found ...
            log.error("Access is denied, no authorisation rules match for user [{}]", customerProfileId.getInteger());
            throw new Exception("Access denied, authorisation failure");
        } else {
            log.debug("Authorisation for user [{}] was successful.", customerProfileId.getInteger());
        }

    }

    /*private TPGWError getTPGWError (Throwable e, Object inputMsg) {
     ExceptionManager exm = new ExceptionManager(this.getClass().getName());
     FriendlyException fe = exm.getFriendlyException(e, inputMsg);
     exm.reportError(fe);
     TPGWError tpgwError = null;
     try {
     tpgwError = new TPGWError(e);
     tpgwError.setErrorCode(fe.getErrorCode());
     tpgwError.setErrorDesc(fe.getErrorDesc());
     tpgwError.setErrorType(fe.getErrorType());
     } catch (Exception ex) {
     log.warn("Error: ", ex);
     }
     return tpgwError;
     }*/
    private String evaluateXPath(Document doc, XPath xPath, String xPathString) throws Exception {
        XPathExpression xpathExp = xPath.compile(xPathString);
        String value = (String) xpathExp.evaluate(doc, XPathConstants.STRING);

        if (value.isEmpty()) {
            log.debug("A XML element was not found: [{}]", xPathString);
        }
        return value;
    }

    private String xmlDocumentToString(Document doc) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        //initialize StreamResult with File object to save to file
        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source = new DOMSource(doc);
        transformer.transform(source, result);

        return result.getWriter().toString();

    }

    public Account getBalance(AccountQuery accountQuery) throws TPGWApiError {
        Account account = null;
        try {
            account = SCAWrapper.getUserSpecificInstance().getAccount(accountQuery);
        } catch (Exception ex) {
            throw getTPGWApiError(ex);
        }
        return account;
    }

    public Done doBalanceTransfer(BalanceTransferData balanceTransferData, String channel, String extId) throws TPGWApiError {
        Done isDone = null;
        String statusMessage = "";

        try {
            // - Mukosi Make a floor of the major currency amount paid by customer as decimals are not allowed on the sale quantity.
            double flooredAmount = Math.floor(balanceTransferData.getAmountInCents() / 100d) * 100d;
            balanceTransferData.setAmountInCents(flooredAmount);

            // If the source account is of service spec 1005 then process this as a clearing bureau sale and not a balance transfer
            ServiceInstanceQuery serviceInstanceQuery = new ServiceInstanceQuery();
            serviceInstanceQuery.setAccountId(balanceTransferData.getSourceAccountId());
            serviceInstanceQuery.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
            ServiceInstanceList serviceInstances = SCAWrapper.getUserSpecificInstance().getServiceInstances(serviceInstanceQuery);
            boolean foundCB = false;
            int salesPersonCustomerId = 0;

            for (ServiceInstance si : serviceInstances.getServiceInstances()) {
                if (si.getServiceSpecificationId() == 1005) {
                    foundCB = true;
                    salesPersonCustomerId = si.getCustomerId();
                    break;
                }
            }
            if (foundCB) {
                log.debug("Channel to use is: {}, and extTxId is: {}", channel, extId);
                Sale sale = new Sale();

                Account recipientAccount = SCAWrapper.getAdminInstance().getAccount(balanceTransferData.getTargetAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
                sale.setRecipientCustomerId(recipientAccount.getServiceInstances().get(0).getCustomerId());
                sale.setRecipientAccountId(balanceTransferData.getTargetAccountId());
                if (!recipientAccount.getServiceInstances().isEmpty()) {
                    ProductInstance pi = SCAWrapper.getAdminInstance().getProductInstance(recipientAccount.getServiceInstances().get(0).getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN);
                    log.debug("This sale is to organisation id [{}]", pi.getOrganisationId());
                    sale.setRecipientOrganisationId(pi.getOrganisationId());
                }
                sale.setPaymentMethod("Clearing Bureau");
                sale.setSaleLocation("");
                sale.setSaleTotalCentsIncl(0d);
                sale.setWarehouseId("");
                sale.setPromotionCode("");
                sale.setExtraInfo("");
                sale.setPurchaseOrderData("");
                sale.setTaxExempt(false);
                sale.setUniqueId(balanceTransferData.getSCAContext().getTxId().replace("UNIQUE-", ""));
                if (extId == null || extId.isEmpty()) {
                    log.debug("ExtTxId is null or empty, going to default to one used by SCAContext");
                    extId = balanceTransferData.getSCAContext().getTxId();
                }
                sale.setExtTxId(extId);

                sale.setSalesPersonAccountId(balanceTransferData.getSourceAccountId());
                sale.setSalesPersonCustomerId(salesPersonCustomerId);

                if (channel == null || channel.isEmpty()) {
                    channel = String.valueOf(balanceTransferData.getSourceAccountId());
                } else {
                    channel = String.valueOf(balanceTransferData.getSourceAccountId()) + "." + channel;
                }
                String x3Channel;
                try {
                    x3Channel = BaseUtils.getSubProperty("env.tpgw.x3channel.mapping", channel.toLowerCase());
                } catch (Exception ex) {
                    log.debug("Failed to find x3Channel linked to channel {}, its probably a new channel, going to default to channel associated with account", channel.toLowerCase());
                    x3Channel = BaseUtils.getSubProperty("env.tpgw.x3channel.mapping", String.valueOf(balanceTransferData.getSourceAccountId()));
                }
                sale.setChannel(x3Channel);

                SaleLine line = new SaleLine();//We dont have TransferLines on the request, going to create just one SaleLine
                line.setLineNumber(0);
                Double d = balanceTransferData.getAmountInCents();
                line.setQuantity(d.longValue() / 100);
                line.setLineTotalCentsIncl(balanceTransferData.getAmountInCents());

                if (line.getQuantity() <= 0) {
                    throw new Exception("Tendered amount for airtime purchase is zero or below zero -- [Amount tendered: " + balanceTransferData.getAmountInCents() + "]");
                }

                InventoryItem ii = new InventoryItem();
                ii.setItemNumber("AIR1004");
                ii.setSerialNumber("AIRTIME");
                line.setInventoryItem(ii);
                sale.getSaleLines().add(line);
                Sale processedSale = SCAWrapper.getUserSpecificInstance().processSale(sale);
                isDone = new Done();
                if ("PD".equals(processedSale.getStatus())) {
                    isDone.setDone(StDone.TRUE);
                    statusMessage = STATUS_SUCCESSFUL;
                }
            } else {
                isDone = SCAWrapper.getUserSpecificInstance().transferBalance(balanceTransferData);
                statusMessage = STATUS_SUCCESSFUL;
            }

        } catch (SCABusinessError er) {
            if (er.getErrorCode().equalsIgnoreCase("bm-0011")
                    || er.getErrorCode().equalsIgnoreCase("bm-0013")
                    || er.getErrorCode().equalsIgnoreCase("pos-0056") //Duplicate
                    || er.getErrorCode().equalsIgnoreCase("sca-0028")) {
                // Respond that its been done successfully
                isDone = new Done();
                isDone.setDone(StDone.TRUE);
                statusMessage = STATUS_SUCCESSFUL + "\nERROR-INFO=" + er.getErrorDesc();
            } else {
                statusMessage = "ERROR-INFO=" + er.getErrorDesc();
                throw er;
            }
        } catch (Exception ex) {
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "TPGW", "Error processing TPGW Transaction: " + ex.toString());
            statusMessage = "ERROR-INFO=" + ex.getMessage();
            throw getTPGWApiError(ex);
        } finally {

            String uniqueKey = balanceTransferData.getSCAContext() == null ? "" : balanceTransferData.getSCAContext().getTxId().replace("UNIQUE-", "");
            if (uniqueKey.isEmpty()) {
                uniqueKey = balanceTransferData.getTargetAccountId() + "-" + balanceTransferData.getSourceAccountId();
            }
            createEvent("doBalanceTransfer", uniqueKey, balanceTransferData, statusMessage);
        }

        return isDone;
    }

    public Done buyBundleUsingPriceInCents(BalanceTransferData balanceTransferData, String channel, String extId) throws TPGWApiError {
        Done isDone = null;
        String statusMessage = "";

        try {
            // - Mukosi Make a floor of the major currency amount paid by customer as decimals are not allowed on the sale quantity.
            double flooredAmount = Math.floor(balanceTransferData.getAmountInCents() / 100d) * 100d;
            balanceTransferData.setAmountInCents(flooredAmount);

            // If the source account is of service spec 1005 then process this as a clearing bureau sale otherwise fail transaction
            ServiceInstanceQuery serviceInstanceQuery = new ServiceInstanceQuery();
            serviceInstanceQuery.setAccountId(balanceTransferData.getSourceAccountId());
            serviceInstanceQuery.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
            ServiceInstanceList serviceInstances = SCAWrapper.getUserSpecificInstance().getServiceInstances(serviceInstanceQuery);
            boolean foundCB = false;
            int salesPersonCustomerId = 0;

            for (ServiceInstance si : serviceInstances.getServiceInstances()) {
                if (si.getServiceSpecificationId() == 1005) {
                    foundCB = true;
                    salesPersonCustomerId = si.getCustomerId();
                    break;
                }
            }

            if (!foundCB) {
                throw new Exception("Not supported yet, only Clearing Bureau service is supported");
            }

            log.debug("Channel to use is: {}, and extTxId is: {}", channel, extId);
            Sale sale = new Sale();

            Account recipientAccount = SCAWrapper.getAdminInstance().getAccount(balanceTransferData.getTargetAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
            sale.setRecipientCustomerId(recipientAccount.getServiceInstances().get(0).getCustomerId());
            sale.setRecipientAccountId(balanceTransferData.getTargetAccountId());
            if (!recipientAccount.getServiceInstances().isEmpty()) {
                ProductInstance pi = SCAWrapper.getAdminInstance().getProductInstance(recipientAccount.getServiceInstances().get(0).getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN);
                log.debug("This sale is to organisation id [{}]", pi.getOrganisationId());
                sale.setRecipientOrganisationId(pi.getOrganisationId());
            }
            sale.setPaymentMethod("Clearing Bureau");
            sale.setSaleLocation("");
            sale.setSaleTotalCentsIncl(0d);
            sale.setWarehouseId("");
            sale.setPromotionCode("");
            sale.setExtraInfo("");
            sale.setPurchaseOrderData("");
            sale.setTaxExempt(false);
            sale.setUniqueId(balanceTransferData.getSCAContext().getTxId().replace("UNIQUE-", ""));
            if (extId == null || extId.isEmpty()) {
                log.debug("ExtTxId is null or empty, going to default to one used by SCAContext");
                extId = balanceTransferData.getSCAContext().getTxId();
            }
            sale.setExtTxId(extId);

            sale.setSalesPersonAccountId(balanceTransferData.getSourceAccountId());
            sale.setSalesPersonCustomerId(salesPersonCustomerId);

            if (channel == null || channel.isEmpty()) {
                channel = String.valueOf(balanceTransferData.getSourceAccountId());
            } else {
                channel = String.valueOf(balanceTransferData.getSourceAccountId()) + "." + channel;
            }
            String x3Channel;
            try {
                x3Channel = BaseUtils.getSubProperty("env.tpgw.x3channel.mapping", channel.toLowerCase());
            } catch (Exception ex) {
                log.debug("Failed to find x3Channel linked to channel {}, its probably a new channel, going to default to channel associated with account", channel.toLowerCase());
                x3Channel = BaseUtils.getSubProperty("env.tpgw.x3channel.mapping", String.valueOf(balanceTransferData.getSourceAccountId()));
            }
            sale.setChannel(x3Channel);

            BigDecimal remainingAmountForAirtimePurchase = new BigDecimal(balanceTransferData.getAmountInCents());
            UnitCreditSpecification ucs;
            List<UnitCreditSpecification> unitCreditsToBuy = new ArrayList<>();
            List<UnitCreditSpecification> tpgwUnitCredits = orderList(getTPGWUnitCreditCatalogue(), "getPriceInCents", "asc");

            if (tpgwUnitCredits == null || tpgwUnitCredits.isEmpty()) {
                throw new Exception("No TPGW unit credit available for purchase");
            }

            ucs = binarySearchUCSByPriceFromList(tpgwUnitCredits, remainingAmountForAirtimePurchase.doubleValue());//Check if we can find exact match on first attempt

            if (ucs == null) {
                log.debug("Binary search was not successful, going to do a linear search");
                UnitCreditSpecification[] ucsArray = tpgwUnitCredits.toArray(new UnitCreditSpecification[tpgwUnitCredits.size()]);
                int indexToPurchase;
                double bundlePrice;
                while (remainingAmountForAirtimePurchase.doubleValue() > 0) {
                    log.debug("Testing amount: {}", remainingAmountForAirtimePurchase.doubleValue());
                    indexToPurchase = getUnitCreditIndexToPurchase(ucsArray, remainingAmountForAirtimePurchase.doubleValue());
                    if (indexToPurchase >= 0) {
                        ucs = ucsArray[indexToPurchase];
                        bundlePrice = ucs.getPriceInCents();
                        unitCreditsToBuy.add(ucs);
                    } else {
                        break;
                    }
                    remainingAmountForAirtimePurchase = remainingAmountForAirtimePurchase.subtract(new BigDecimal(bundlePrice));
                }
            } else {
                remainingAmountForAirtimePurchase = remainingAmountForAirtimePurchase.subtract(new BigDecimal(ucs.getPriceInCents()));
                unitCreditsToBuy.add(ucs);
            }

            orderList(unitCreditsToBuy, "getValidityDays", "asc");//Sort according to Validity so that the purchase date-time for those with lower validity is first. Hoping pos does not modify SALE_LINE order
            int count = 0;
            SaleLine line;
            InventoryItem ii;

            for (UnitCreditSpecification ucsToBuy : unitCreditsToBuy) {
                if (log.isDebugEnabled()) {
                    log.debug("Adding BUNDLE SALE_LINE_{} with UCS [{}@{}]", new Object[]{count, ucsToBuy.getUnitCreditSpecificationId(), ucsToBuy.getPriceInCents()});
                }
                line = new SaleLine();
                line.setLineNumber(count);
                line.setQuantity(1);
                line.setLineTotalCentsIncl(ucsToBuy.getPriceInCents());

                ii = new InventoryItem();
                ii.setItemNumber(ucsToBuy.getItemNumber());
                ii.setSerialNumber("");
                line.setInventoryItem(ii);
                sale.getSaleLines().add(line);
                count++;
            }

            if (remainingAmountForAirtimePurchase.compareTo(BigDecimal.ZERO) > 0) {
                if (log.isDebugEnabled()) {
                    log.debug("Adding Airtime SALE_LINE_{} @{}", count, remainingAmountForAirtimePurchase.doubleValue());
                }
                line = new SaleLine();
                line.setLineNumber(count);
                line.setQuantity(remainingAmountForAirtimePurchase.longValue() / 100);
                line.setLineTotalCentsIncl(remainingAmountForAirtimePurchase.doubleValue());

                ii = new InventoryItem();
                ii.setItemNumber("AIR1004");
                ii.setSerialNumber("AIRTIME");
                line.setInventoryItem(ii);
                sale.getSaleLines().add(line);
            }
            Sale processedSale = SCAWrapper.getUserSpecificInstance().processSale(sale);
            isDone = new Done();
            if ("PD".equals(processedSale.getStatus())) {
                isDone.setDone(StDone.TRUE);
                statusMessage = STATUS_SUCCESSFUL;
            }

        } catch (SCABusinessError er) {
            if (er.getErrorCode().equalsIgnoreCase("bm-0011")
                    || er.getErrorCode().equalsIgnoreCase("bm-0013")
                    || er.getErrorCode().equalsIgnoreCase("pos-0056") //Duplicate
                    || er.getErrorCode().equalsIgnoreCase("sca-0028")) {
                // Respond that its been done successfully
                isDone = new Done();
                isDone.setDone(StDone.TRUE);
                statusMessage = STATUS_SUCCESSFUL + "\nERROR-INFO=" + er.getErrorDesc();
            } else {
                statusMessage = "ERROR-INFO=" + er.getErrorDesc();
                throw er;
            }
        } catch (Exception ex) {
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "TPGW", "Error processing TPGW Transaction: " + ex.toString());
            statusMessage = "ERROR-INFO=" + ex.getMessage();
            throw getTPGWApiError(ex);
        } finally {

            String uniqueKey = balanceTransferData.getSCAContext() == null ? "" : balanceTransferData.getSCAContext().getTxId().replace("UNIQUE-", "");
            if (uniqueKey.isEmpty()) {
                uniqueKey = balanceTransferData.getTargetAccountId() + "-" + balanceTransferData.getSourceAccountId();
            }
            createEvent("buyBundleUsingPriceInCents", uniqueKey, balanceTransferData, statusMessage);
        }

        return isDone;
    }

    private int getUnitCreditIndexToPurchase(UnitCreditSpecification[] ucsArray, double amount) throws Exception {
        int indexToPurchace = 0;
        while (indexToPurchace < ucsArray.length && amount >= ucsArray[indexToPurchace].getPriceInCents()) {
            indexToPurchace++;
        }
        return indexToPurchace - 1;
    }

    private UnitCreditSpecification binarySearchUCSByPriceFromList(List<UnitCreditSpecification> tpgwUnitCredits, double priceToCheck) throws Exception {

        if (priceToCheck <= 0 || tpgwUnitCredits == null || tpgwUnitCredits.isEmpty()) {
            return null;
        }
        int max = tpgwUnitCredits.size();
        UnitCreditSpecification[] ucsArray = tpgwUnitCredits.toArray(new UnitCreditSpecification[max]);
        int min = 0;
        UnitCreditSpecification ucs = ucsArray[max - 1];
        double maxPrice = ucs.getPriceInCents();//Order of bundle price is from low to high, checking the highest.

        if (maxPrice == priceToCheck) {
            if (log.isDebugEnabled()) {
                log.debug("Amount [{}] is enough to purchase a UCS with highest price, going to return UCS {}@{}", new Object[]{priceToCheck, ucs.getUnitCreditSpecificationId(), ucs.getPriceInCents()});
            }
            return ucs;
        }
        ucs = null;
        int guess = (min + max) / 2;

        while (max > min) {
            UnitCreditSpecification tmp = ucsArray[guess - 1];
            if (log.isDebugEnabled()) {
                log.debug("Max[{}], min[{}], guess[{}]. UCS in check is {}@{}", new Object[]{max, min, guess, tmp.getUnitCreditSpecificationId(), tmp.getPriceInCents()});
            }
            if (tmp.getPriceInCents() == priceToCheck) {
                int code = tmp.getUnitCreditSpecificationId();
                ucs = tmp;
                log.debug("Found UCS with ID {}", code);
                break;
            }
            if (tmp.getPriceInCents() < priceToCheck) {
                min = guess + 1;
            } else {
                max = guess - 1;
            }
            guess = (min + max) / 2;
        }
        return ucs;
    }

    public static List orderList(List list, String getterToOrderBy, String order) throws Exception {
        if (list == null) {
            return null;
        }
        if (list.size() <= 1) {
            log.debug("No need to sort the list as it has 0 or 1 items");
            return list;
        }
        Comparator comparator = new DynamicComparator(getterToOrderBy, order);
        Collections.sort(list, comparator);
        return list;
    }

    private static class DynamicComparator implements Comparator {

        private String getterToOrderBy;
        private String order;
        private static final String DESC = "desc";
        private Method m = null;

        public DynamicComparator(String getterToOrderBy, String order) {
            this.getterToOrderBy = getterToOrderBy;
            this.order = order;
        }

        @Override
        public int compare(Object o1, Object o2) {
            // Ok so, o1 and o2 are both objects in a list. We need to get the result of calling the method "getterToOrderBy" on each object and compare them
            int ret = 0;
            if (m == null) {
                try {
                    m = o1.getClass().getMethod(getterToOrderBy);
                } catch (Exception ex) {
                    log.error("Error", ex);
                }
            }
            try {
                Object o1Data = m.invoke(o1);
                Object o2Data = m.invoke(o2);
                if (o1Data instanceof String) {
                    ret = ((String) o1Data).compareTo((String) o2Data);
                } else if (o1Data instanceof XMLGregorianCalendar) {
                    ret = ((XMLGregorianCalendar) o1Data).compare((XMLGregorianCalendar) o2Data);
                } else if (o1Data instanceof Integer) {
                    ret = ((Integer) o1Data).compareTo((Integer) o2Data);
                } else if (o1Data instanceof Double) {
                    ret = ((Double) o1Data).compareTo((Double) o2Data);
                }
            } catch (Exception ex) {
                log.error("Error", ex);
            }

            if (order.equalsIgnoreCase(DESC)) {
                ret *= -1;
            }
            return ret;
        }
    }

    public long getThirdPartyAccountMappingBySessionId(String sessionId) throws Exception {
        return getThirdPartyAccountMapping(getUsername(sessionId));
    }

    public long getThirdPartyAccountMapping(String organisationUsername) throws Exception {

        String propKey = "env.tpgw.account.mappings";
        List<String> lstProperty = BaseUtils.getPropertyAsList(propKey);
        StringTokenizer st;

        for (String prop : lstProperty) {
            st = new StringTokenizer(prop, "|");
            if (st.nextToken().equals(organisationUsername)) {
                return Long.parseLong(st.nextToken());
            }
        }

        throw new Exception("No account mapping found for TPGW username -- [" + organisationUsername + "], "
                + "check the value set for property [" + propKey + "].");
    }

    public boolean checkIfEmailRechargeMultiAccountSupported(String organisationUsername) throws Exception {

        String propKey = "env.tpgw.account.mappings";
        List<String> lstProperty = BaseUtils.getPropertyAsList(propKey);
        StringTokenizer st;
        boolean emailRechargeMultiAccountNotSupported = false;
        String emailInteractionSupport = null;

        for (String prop : lstProperty) {
            st = new StringTokenizer(prop, "|");
            if (st.nextToken().equals(organisationUsername)) {
                st.nextToken();//Skip second token
                try {
                    emailInteractionSupport = st.nextToken();//Take third token
                    log.debug("Did find third token: {}", emailInteractionSupport);
                } catch (NoSuchElementException ex) {
                    //Do nothing
                }
                if (emailInteractionSupport != null && !emailInteractionSupport.isEmpty() && emailInteractionSupport.equalsIgnoreCase("EmailRechargeMultiAccountNotSupported")) {
                    emailRechargeMultiAccountNotSupported = true;
                    break;
                }
                log.debug("Supports multi account recharge interaction");
                break;
            }
        }
        return emailRechargeMultiAccountNotSupported;
    }

    private TPGWApiError getTPGWApiError(Throwable e) {
        TPGWApiError tpgwError = new TPGWApiError();
        ExceptionManager exm = new ExceptionManager(log);
        FriendlyException fe = exm.getFriendlyException(e);
        if (!(e instanceof SCABusinessError)) {
            // No need to report business errors
            exm.reportError(fe);
        }
        try {
            tpgwError.setErrorCode(fe.getErrorCode());
            tpgwError.setErrorDesc(fe.getErrorDesc());
            tpgwError.setErrorType(fe.getErrorType());
        } catch (Exception ex) {
            log.warn("Error: ", ex);
        }
        return tpgwError;
    }

    public int addCustomer(Customer newCustomer) {
        return SCAWrapper.getAdminInstance().addCustomer(newCustomer).getInteger();
    }

    public Done doPurchaseBundle(PurchaseUnitCreditRequest request, String sessionId, String username, double tenderedAmount, String channel) throws TPGWApiError {
        Done isDone = null;
        String statusMessage = "";
        String amountTenderedAndAmountDue = "";

        try {

            if (request.getAccountId() <= 1100000000l) {
                throw getTPGWApiError(new Exception("Invalid customer account detected"));
            }
            String tpgwUser = username == null ? getUsername(sessionId) : username;
            request.setPaidByAccountId(getThirdPartyAccountMapping(tpgwUser));

            // Double check that the customer did pay for the bundle.
            UnitCreditSpecification ucs = NonUserSpecificCachedDataHelper.getUnitCreditSpecification(request.getUnitCreditSpecificationId());

            if (!ucs.getConfiguration().contains("TPGW=true")
                    || !Utils.isBetween(new Date(), ucs.getAvailableFrom(), ucs.getAvailableTo())) {
                throw new Exception("Invalid unit credit specification. UC is not currently available on the TPGW -- " + ucs.getUnitCreditSpecificationId());
            }

            double amountDue = ucs.getPriceInCents() * request.getNumberToPurchase();
            log.debug("Amount due is {}, tenderedAmount is {} and quantity bought is {}", new Object[]{amountDue, tenderedAmount, request.getNumberToPurchase()});

            if (tenderedAmount != amountDue) {
                amountTenderedAndAmountDue = ": tenderedAmount[" + tenderedAmount + "], amountDue[" + amountDue + "]";
                throw new Exception("Insufficient tendered amount while trying to purchase a bundle");
            }

            // If the source account is of service spec 1005 then process this as a clearing bureau sale and not airtime payment
            ServiceInstanceQuery serviceInstanceQuery = new ServiceInstanceQuery();
            serviceInstanceQuery.setAccountId(request.getPaidByAccountId());
            serviceInstanceQuery.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
            ServiceInstanceList serviceInstances = SCAWrapper.getUserSpecificInstance().getServiceInstances(serviceInstanceQuery);
            for (ServiceInstance si : serviceInstances.getServiceInstances()) {
                if (si.getServiceSpecificationId() == 1005) {
                    request.setPaymentMethod("Clearing Bureau");
                    break;
                }
            }

            if (channel == null || channel.isEmpty()) {
                channel = String.valueOf(request.getPaidByAccountId());
            } else {
                channel = String.valueOf(request.getPaidByAccountId()) + "." + channel;
            }
            try {
                request.setChannel(BaseUtils.getSubProperty("env.tpgw.x3channel.mapping", channel.toLowerCase()));
            } catch (Exception ex) {
                log.debug("Failed to find x3Channel linked to channel {}, its probably a new channel, going to default to channel associated with account", channel.toLowerCase());
                request.setChannel(BaseUtils.getSubProperty("env.tpgw.x3channel.mapping", String.valueOf(request.getPaidByAccountId())));
            }
            //paidByAccountId and accountId for USSD interface should be same
            String ussdUser = BaseUtils.getProperty("env.tpgw.ussduser", null);
            if (tpgwUser != null && tpgwUser.equals(ussdUser)) {
                request.setPaidByAccountId(request.getAccountId());
            }
            isDone = SCAWrapper.getUserSpecificInstance().purchaseUnitCredit(request);
            statusMessage = STATUS_SUCCESSFUL;

        } catch (SCABusinessError er) {

            if (er.getErrorCode().equalsIgnoreCase("bm-0011") // Duplicate
                    || er.getErrorCode().equalsIgnoreCase("bm-0013") // Duplicate
                    || er.getErrorCode().equalsIgnoreCase("pos-0056") //Duplicate
                    || er.getErrorCode().equalsIgnoreCase("sca-0028")) { // Duplicate
                // Respond that its been done successfully
                isDone = new Done();
                isDone.setDone(StDone.TRUE);
                statusMessage = STATUS_SUCCESSFUL + "\nERROR-INFO=" + er.getErrorDesc();
                log.debug("This is a duplicate. Replying wth successful");
            } else if (er.getErrorCode().equalsIgnoreCase("bm-0002")) { // Account does not exist
                statusMessage = "ERROR-INFO=Invalid customer account detected";
                throw getTPGWApiError(new Exception("Invalid customer account detected"));
            } else {
                statusMessage = "ERROR-INFO=" + er.getErrorDesc();
                throw getTPGWApiError(er);
            }

        } catch (TPGWApiError tpe) {
            throw tpe;
        } catch (Exception ex) {

            if (!ex.getMessage().equals("Insufficient tendered amount while trying to purchase a bundle")) {
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "TPGW", "Error processing TPGW Transaction: " + ex.toString());
            }
            statusMessage = "ERROR-INFO=" + ex.getMessage() + (amountTenderedAndAmountDue.isEmpty() ? "" : amountTenderedAndAmountDue);
            throw getTPGWApiError(ex);

        } finally {

            String uniqueKey = request.getUniqueId() == null ? "" : request.getUniqueId();
            if (uniqueKey.isEmpty()) {
                uniqueKey = request.getAccountId() + "-" + request.getPaidByAccountId();
            }
            createEvent("doPurchaseBundle", uniqueKey, request, statusMessage);
        }

        return isDone;
    }

    public String getUsername(String sessionId) {
        CallersRequestContext crc = (CallersRequestContext) CacheHelper.getFromRemoteCache(sessionId);
        return crc.getRemoteUser();
    }

    public List<UnitCreditSpecification> getTPGWUnitCreditCatalogue() {
        // Returns a list of bundles that are allowed to be sold through TPGW
        List<UnitCreditSpecification> ucsList = new ArrayList<>();
        UnitCreditSpecificationQuery q = new UnitCreditSpecificationQuery();
        q.setUnitCreditSpecificationId(-1);
        q.setVerbosity(StUnitCreditSpecificationLookupVerbosity.MAIN_SVCSPECIDS);

        for (UnitCreditSpecification ucs : SCAWrapper.getUserSpecificInstance().getUnitCreditSpecifications(q).getUnitCreditSpecifications()) {
            if (ucs.getConfiguration() != null && ucs.getConfiguration().contains("TPGW=true") && Utils.isBetween(new Date(), ucs.getAvailableFrom(), ucs.getAvailableTo())) {
                if (!isAllowed(ucs.getPurchaseRoles())) {
                    continue;
                }
                ucsList.add(ucs);
            }
        }
        return ucsList;
    }

    public boolean isAllowed(String roles) {
        //Set usersRoles = BaseUtils.getPropertyAsSet("env.tpgw.uc.purchase.roles");
        Set<String> ucsRoles = Utils.getSetFromCRDelimitedString(roles);
        for (String role : ucsRoles) {
            if (role.equals("Customer")) {
                return true;
            }
        }
        return false;
    }

    public TransactionStatusResult getTransactionStatus(String txId, String sessionId, Date dateFrom, Date dateTo) throws TPGWApiError {
        log.debug("Entering getTransactionStatus");
        EventQuery eq = new EventQuery();
        TransactionStatusResult tsr = new TransactionStatusResult();
        try {

            if (txId == null || txId.isEmpty()) {
                tsr.setTransactionStatus(STATUS_NOT_FOUND);
                return tsr;
            }
            String eventKey = txId + "-" + getThirdPartyAccountMappingBySessionId(sessionId);
            eq.setEventKey(eventKey);
            eq.setEventType("TPGW");
            if (dateFrom != null) {
                eq.setDateFrom(Utils.getDateAsXMLGregorianCalendar(dateFrom));
            }
            if (dateTo != null) {
                eq.setDateTo(Utils.getDateAsXMLGregorianCalendar(dateTo));
            }
            eq.setResultLimit(100);//Should this be a property?

            EventList events = SCAWrapper.getAdminInstance().getEvents(eq);

            if (events.getNumberOfEvents() < 1) {
                tsr.setTransactionStatus(STATUS_NOT_FOUND);
                return tsr;
            }

            String eventData = "";
            String status = "";
            boolean canBreak = false;

            for (Event e : events.getEvents()) {
                String eData = e.getEventData();
                eventData = eData;
                Matcher m = statusPattern.matcher(eData);
                while (m.find()) {
                    status = m.group();
                    log.debug("Property with matching pattern found: {}", status);
                    if (status.contains(STATUS_SUCCESSFUL)) {
                        log.debug("This transaction was successful, going to break out of loop: {}", status);
                        canBreak = true;
                        break;
                    }
                }
                if (canBreak) {
                    break;
                }
            }

            if (status.isEmpty()) {
                status = "STATUS=" + STATUS_NOT_FOUND;//Just in case another event sub-type besides doPurchaseBundle|doBalanceTransfer is logging events using this event key without supplying status info
            }
            status = status.split("=")[1];
            tsr.setTransactionStatus(status);
            if (!status.equalsIgnoreCase(STATUS_SUCCESSFUL)) {
                tsr.setTransactionInfo(eventData);
            }

        } catch (Exception ex) {
            log.warn("Error occured trying to get transaction status from events table: {}", ex.toString());
            throw getTPGWApiError(ex);
        }
        log.debug("Exiting getTransactionStatus");
        return tsr;
    }

    public ValidateReferenceIdResult validateReferenceID(String referenceId) throws TPGWApiError {
        log.debug("ReferenceId to validate is: {}", referenceId);
        ValidateReferenceIdResult ret = new ValidateReferenceIdResult();
        ret.setValidationResult(STATUS_NOT_FOUND);
        int custId = 0;

        if (referenceId == null || referenceId.isEmpty() || referenceId.length() <= 2) {
            return ret;
        }

        int reference = removePrefixFromReferenceId(referenceId);
        if (reference <= 0) {
            return ret;
        }

        if (referenceId.toLowerCase().startsWith(SALE_TYPE_PREFIX_CASH_IN)) {
            CashInQuery cq = new CashInQuery();
            cq.setCashInId(reference);
            CashInList cashIns = null;
            try {
                cashIns = SCAWrapper.getUserSpecificInstance().getCashIns(cq);
            } catch (Exception ex) {
                log.warn("Failed to retrieve cashIn with id {}, reason: {}", reference, ex.toString());
            }
            if (cashIns != null && !cashIns.getCashInDataList().isEmpty()) {
                CashInData cashIn = cashIns.getCashInDataList().get(0);
                if (!cashIn.getStatus().equals("BDP")) {
                    return ret;
                }
                ret.setValidationResult("SUCCESS");
                ret.setAmountInCents(cashIn.getCashRequiredInCents());
                custId = cashIn.getSalesPersonCustomerId();
            }
        } else {

            Sale sale = null;
            try {
                sale = SCAWrapper.getAdminInstance().getSale(reference, StSaleLookupVerbosity.SALE);
            } catch (Exception ex) {
                log.warn("Failed to retrieve sale with id {}, reason: {}", reference, ex.toString());
            }

            if (sale != null && !sale.getStatus().equals(PAYMENT_STATUS_PAID)
                    && (sale.getPaymentMethod().equals(PAYMENT_METHOD_BANK_TRANSFER)
                    || sale.getPaymentMethod().equals(PAYMENT_METHOD_CHEQUE)
                    || sale.getPaymentMethod().equals(PAYMENT_METHOD_CARD_INTEGRATION))) {
                ret.setValidationResult("SUCCESS");
                ret.setAmountInCents(sale.getSaleTotalCentsIncl());
                custId = sale.getRecipientCustomerId();
            }
        }

        if (custId > 0) {
            Customer cust = SCAWrapper.getAdminInstance().getCustomer(custId, StCustomerLookupVerbosity.CUSTOMER);
            ret.setCustomer(cust);
        }

        return ret;
    }

    public Done processTransaction(String referenceId, double cashReceiptedInCents, String extId, String bankName, String gatewayCode) throws Exception {
        log.debug("ReferenceId to validate is: {}", referenceId);
        Done isDone = new Done();
        if (referenceId == null || referenceId.isEmpty() || referenceId.length() <= 2) {
            return isDone;
        }
        int reference = removePrefixFromReferenceId(referenceId);
        if (reference <= 0) {
            return isDone;
        }
        if (referenceId.toLowerCase().startsWith(SALE_TYPE_PREFIX_CASH_IN)) {
            boolean isSuccess = processCashIn(reference, cashReceiptedInCents, bankName, extId, gatewayCode);
            if (isSuccess) {
                isDone.setDone(StDone.TRUE);
            }
        } else {
            boolean isSuccess = processSale(reference, cashReceiptedInCents, extId, gatewayCode);
            if (isSuccess) {
                isDone.setDone(StDone.TRUE);
            }
        }
        return isDone;
    }

    private boolean processCashIn(int cashId, double cashReceiptedInCents, String bankName, String extId, String gatewayCode) throws Exception {

        CashInQuery cq = new CashInQuery();
        cq.setCashInId(cashId);
        CashInList cashIns = null;
        CashInData cashInData;
        try {
            cashIns = SCAWrapper.getAdminInstance().getCashIns(cq);
        } catch (Exception ex) {
            log.warn("Failed to retrieve cashIn with id {}, reason: {}", cashId, ex.toString());
        }
        if (cashIns == null) {
            return false;
        }
        if (cashIns.getCashInDataList().isEmpty()) {
            return false;
        }

        cashInData = cashIns.getCashInDataList().get(0);

        log.debug("Cash required in cents is [{}]c and got [{}]", cashInData.getCashRequiredInCents(), cashReceiptedInCents);
        double cashRequiredInCents = cashInData.getCashRequiredInCents();
        DecimalFormat df = new DecimalFormat("#.00");
        String cashRequiredInCentsFormatted = df.format(Utils.round((cashRequiredInCents / 100), 0));
        log.debug("Formatted required amount is [{}]", cashRequiredInCentsFormatted);

        double requiredInCents = Utils.round((Double.parseDouble(cashRequiredInCentsFormatted) * 100), 0);
        double paymentDifferenceInCents = requiredInCents - cashReceiptedInCents;
        log.debug("Payment difference is [{}]cents", paymentDifferenceInCents);

        if (requiredInCents != cashReceiptedInCents) {
            log.warn("Amount tendered is not equal to required amount for cash-in with reference: {}", cashId);
            return false;
        }

        if (cashInData.getStatus().equals("BDC")) {
            return true;
        }
        if (!cashInData.getStatus().equals("BDP")) {
            return false;
        }

        log.debug("Cash id exist and has not been processed yet, going to attempt cashin");
        String bank = bankName == null ? "" : bankName;
        cashInData.setBankName(bank);
        cashInData.setExtTxId(extId);
        cashInData.setCashReceiptedInCents(cashReceiptedInCents);
        int custId = Integer.parseInt(BaseUtils.getSubProperty("env.pgw." + gatewayCode.toLowerCase() + ".config", "CustomerId"));
        cashInData.setSalesAdministratorCustomerId(custId);
        CashInData processedCashIn = SCAWrapper.getAdminInstance().processCashIn(cashInData);

        return processedCashIn.getStatus().endsWith("C");

    }

    private boolean processSale(int saleId, double cashReceiptedInCents, String extId, String gatewayCode) throws Exception {
        Sale sale;
        Done done = new Done();
        try {
            sale = SCAWrapper.getAdminInstance().getSale(saleId, StSaleLookupVerbosity.SALE);
        } catch (Exception ex) {
            log.warn("Failed to retrieve sale with id {}, reason: {}", saleId, ex.toString());
            return done.getDone().equals(StDone.TRUE);
        }
        if (sale == null) {
            return false;
        }
        log.debug("Cash required in cents is [{}]c and got [{}]", sale.getSaleTotalCentsIncl(), cashReceiptedInCents);
        double cashRequiredInCents = sale.getSaleTotalCentsIncl();
        DecimalFormat df = new DecimalFormat("#.00");
        String cashRequiredInCentsFormatted = df.format(Utils.round((cashRequiredInCents / 100), 0));
        log.debug("Formatted required amount is [{}]", cashRequiredInCentsFormatted);

        double requiredInCents = Utils.round((Double.parseDouble(cashRequiredInCentsFormatted) * 100), 0);
        double paymentDifferenceInCents = requiredInCents - cashReceiptedInCents;
        log.debug("Payment difference is [{}]cents", paymentDifferenceInCents);

        if (requiredInCents != cashReceiptedInCents) {
            log.warn("Amount tendered is not equal to required amount for sale with reference: {}", saleId);
            return false;
        }
        if (sale.getStatus().equals(PAYMENT_STATUS_PAID) && sale.getPaymentTransactionData().equals(extId)
                && (sale.getPaymentMethod().equals(PAYMENT_METHOD_BANK_TRANSFER)
                || sale.getPaymentMethod().equals(PAYMENT_METHOD_CHEQUE)
                || sale.getPaymentMethod().equals(PAYMENT_METHOD_CARD_INTEGRATION))) {
            done.setDone(StDone.TRUE);
            return done.getDone().equals(StDone.TRUE);
        }

        PaymentNotificationData ppn = new PaymentNotificationData();
        ppn.setPaymentGatewayTransactionId(extId);
        ppn.setPaymentInCents(cashReceiptedInCents);
        ppn.setSaleId(saleId);
        ppn.setPaymentGatewayCode(gatewayCode);
        done = SCAWrapper.getAdminInstance().processPaymentNotification(ppn);
        return done.getDone().equals(StDone.TRUE);
    }

    private int removePrefixFromReferenceId(String referenceId) {
        if (SALE_TYPE_PREFIX_CHEQUE_OR_BANK_TRANSFER.isEmpty()) {
            SALE_TYPE_PREFIX_CHEQUE_OR_BANK_TRANSFER = BaseUtils.getProperty("env.locale.country.for.language.en");
        }
        String retId = "0";
        if (referenceId.toLowerCase().startsWith(SALE_TYPE_PREFIX_CASH_IN)) {
            retId = referenceId.substring(SALE_TYPE_PREFIX_CASH_IN.length()).trim();
        } else if (referenceId.toLowerCase().startsWith(SALE_TYPE_PREFIX_CHEQUE_OR_BANK_TRANSFER.toLowerCase())) {
            retId = referenceId.substring(SALE_TYPE_PREFIX_CHEQUE_OR_BANK_TRANSFER.length()).trim();
        } else if (Character.isDigit(referenceId.charAt(0))) {
            retId = referenceId;
        } else {
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "TPGW", "Could not process transaction, unknown reference: " + referenceId);
            log.warn("The supplied Reference cannot be mapped to currently configured references: {}", referenceId);
        }
        return Integer.parseInt(retId);
    }

    private void createEvent(String eventSubType, String eventKey, Object soapObject, String statusMessage) {
        log.debug("Writing event for event key {}", eventKey);

        StringBuilder sb = new StringBuilder();
        String status = statusMessage.contains(STATUS_SUCCESSFUL) ? STATUS_SUCCESSFUL : STATUS_FAILED;

        Matcher m = errorInfoPattern.matcher(statusMessage);
        String errorInfoData = "";
        while (m.find()) {
            errorInfoData = m.group();
            log.debug("Property with matching pattern found: {}", errorInfoData);
        }

        String eventData = Utils.marshallSoapObjectToString(soapObject);

        if (eventData == null) {
            sb.append("STATUS=").append(status).append("\n");
            sb.append("INPUT=Not called via SOAP").append("\n").append(errorInfoData);
        } else {
            if (eventData.length() > 10000) {
                eventData = eventData.substring(0, 10000) + "...TOO LARGE TO MARSHAL FULLY...";
            }
            sb.append("STATUS=").append(status).append("\n");
            sb.append("INPUT=").append(eventData).append("\n").append(errorInfoData);
        }
        createEvent(eventSubType, eventKey, sb.toString());
    }

    public Done createEvent(String eventSubType, String eventKey, String eventData) {
        Event event = new Event();
        event.setEventSubType(eventSubType);
        event.setEventType("TPGW");
        event.setEventKey(eventKey);
        event.setEventData(eventData);
        Done done;
        try {
            done = SCAWrapper.getAdminInstance().createEvent(event);
        } catch (Exception ex) {
            log.warn("Failed to create event via SCA call, cause: {}", ex.toString());
            done = new Done();
        }
        return done;
    }

    public void sendSMS(String to, String body) throws Exception {
        String alwaysTo = BaseUtils.getProperty("env.tpgw.paymentgateway.sms.notification.always.to", "");
        String from = BaseUtils.getProperty("env.tpgw.paymentgateway.sms.notification.from", "");
        ShortMessage sm = new ShortMessage();
        sm.setBody(body);
        sm.setFrom(from);
        sm.setTo(alwaysTo.isEmpty() ? to : alwaysTo);
        if (!sm.getTo().isEmpty()) {
            log.debug("Sending third party gateway notification sms from [{}] to [{}] text [{}]", new Object[]{from, to, body});
            try {
                SCAWrapper.getAdminInstance().sendShortMessage(sm);
            } catch (Exception ex) {
                log.warn("Failed to send third party gateway notification sms from [{}] to [{}] text [{}]", new Object[]{from, to, body});
            }
        }
    }

    public String createTicket(NewTTIssue tt) {
        TTIssue issue = SCAWrapper.getAdminInstance().createTroubleTicketIssue(tt);
        return issue.getID();
    }

    public void sendEmail(String from, String to, String subject, String body) {
        try {
            IMAPUtils.sendEmail(from, to, subject, body);
        } catch (Exception ex) {//Change if the is a need to propagate error to caller
            log.warn("Failed to send email to {}, reason: {}", to, ex.toString());
        }
    }

    public PrepaidStrip getPrepaidStrip(PrepaidStripQuery prepaidStripQuery) throws TPGWApiError {
        PrepaidStrip strip = null;
        try {
            strip = SCAWrapper.getUserSpecificInstance().getPrepaidStrip_Direct(prepaidStripQuery);
        } catch (Exception ex) {
            throw getTPGWApiError(ex);
        }
        return strip;
    }

    public PrepaidStrip redeamPrepaidStrip(PrepaidStripRedemptionData requestObject) throws TPGWApiError {
        PrepaidStrip strip = null;
        try {
            strip = SCAWrapper.getUserSpecificInstance().redeemPrepaidStrip_Direct(requestObject);
        } catch (Exception ex) {
            throw getTPGWApiError(ex);
        }
        return strip;
    }

    public void updateCustomer(Customer newCustomer) {
        SCAWrapper.getAdminInstance().modifyCustomer(newCustomer);
    }

}
