package com.smilecoms.scp.action;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.localisation.LocalisationHelper;
import com.smilecoms.commons.sca.Account;
import com.smilecoms.commons.sca.AccountHistoryQuery;
import com.smilecoms.commons.sca.AccountList;
import com.smilecoms.commons.sca.AccountQuery;
import com.smilecoms.commons.sca.AccountSummaryQuery;
import com.smilecoms.commons.sca.AttributeNames;
import com.smilecoms.commons.sca.BalanceTransferData;
import com.smilecoms.commons.sca.BalanceTransferLine;
import com.smilecoms.commons.sca.CampaignData;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.CustomerQuery;
import com.smilecoms.commons.sca.CustomerRole;
import com.smilecoms.commons.sca.Done;
import com.smilecoms.commons.sca.EntityAuthorisationException;
import com.smilecoms.commons.sca.Errors;
import com.smilecoms.commons.sca.InventoryItem;
import com.smilecoms.commons.sca.NumberList;
import com.smilecoms.commons.sca.NumbersQuery;
import com.smilecoms.commons.sca.ProductInstance;
import com.smilecoms.commons.sca.ProductInstanceList;
import com.smilecoms.commons.sca.ProductServiceInstanceMapping;
import com.smilecoms.commons.sca.ProductServiceMapping;
import com.smilecoms.commons.sca.ProductSpecification;
import com.smilecoms.commons.sca.ReportIds;
import com.smilecoms.commons.sca.Reservation;
import com.smilecoms.commons.sca.SCABusinessError;
import com.smilecoms.commons.sca.SCAErr;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.Sale;
import com.smilecoms.commons.sca.SaleLine;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.ServiceInstanceList;
import com.smilecoms.commons.sca.ServiceInstanceQuery;
import com.smilecoms.commons.sca.ServiceSpecification;
import com.smilecoms.commons.sca.StAccountHistoryVerbosity;
import com.smilecoms.commons.sca.StAccountLookupVerbosity;
import com.smilecoms.commons.sca.StAccountSummaryVerbosity;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.StDone;
import com.smilecoms.commons.sca.StProductInstanceLookupVerbosity;
import com.smilecoms.commons.sca.StServiceInstanceLookupVerbosity;
import com.smilecoms.commons.sca.StUnitCreditSpecificationLookupVerbosity;
import com.smilecoms.commons.sca.TNFData;
import com.smilecoms.commons.sca.TNFQuery;
import com.smilecoms.commons.sca.TimeRange;
import com.smilecoms.commons.sca.UnitCreditInstance;
import com.smilecoms.commons.sca.UnitCreditInstanceQuery;
import com.smilecoms.commons.sca.UnitCreditSpecification;
import com.smilecoms.commons.sca.UnitCreditSpecificationList;
import com.smilecoms.commons.sca.UnitCreditSpecificationQuery;
import com.smilecoms.commons.sca.beans.NonUserSpecificCachedDataHelper;
import com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper;
import com.smilecoms.commons.stripes.InsufficientPrivilegesException;
import com.smilecoms.commons.stripes.SmileActionBean;
import com.smilecoms.commons.tags.SmileTags;
import com.smilecoms.commons.util.Codec;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.scp.helpers.ChargingDetail;
import com.smilecoms.scp.helpers.PurchaseUnitCreditLine;
import com.smilecoms.scp.helpers.paymentgateway.GatewayCodes;
import com.smilecoms.scp.helpers.paymentgateway.IPGWTransactionData;
import com.smilecoms.scp.helpers.paymentgateway.PaymentGatewayManager;
import com.smilecoms.scp.helpers.paymentgateway.PaymentGatewayManagerFactory;
import com.smilecoms.scp.helpers.SCPJAXBHelper;
import com.smilecoms.scp.helpers.ext.tnf.BreakdownDataset;
import com.smilecoms.scp.helpers.ext.tnf.ComplexResponse;
import com.smilecoms.scp.helpers.ext.tnf.Report;
import com.smilecoms.scp.helpers.ext.tnf.TrendOverTimeDataset;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import javax.servlet.http.HttpSession;
import javax.xml.datatype.XMLGregorianCalendar;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.DontValidate;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.validation.ValidationErrors;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author sabelo
 */
public class AccountActionBean extends SmileActionBean {

    private java.lang.String stripesEventName;
    private java.lang.String eventSourceLink;
    private String stripesActionBeanName;
    public double bundleBalance;
    public double minutesBalance;
    private final int STAFF_SERVICES_ID_FLAG = 1000;
    private IPGWTransactionData PGWTransactionData;
    private int productInstanceIdForSIM;
    private final String PAYMENT_METHOD_PAYMENT_GATEWAY = "Payment Gateway";
    private final String PAYMENT_METHOD_AIRTIME = "Airtime";

    public double getBundleBalance() {
        return bundleBalance;
    }

    public void setBundleBalance(double val) {
        this.bundleBalance = val;
    }

    public double getMinutesBalance() {
        return minutesBalance;
    }

    public void setMinutesBalance(double val) {
        this.minutesBalance = val;
    }

    public int getProductInstanceIdForSIM() {
        return productInstanceIdForSIM;
    }

    public void setProductInstanceIdForSIM(int productInstanceIdForSIM) {
        this.productInstanceIdForSIM = productInstanceIdForSIM;
    }

    public String getSessionDetailBarGraphJson() {
        return sessionDetailBarGraphJson;
    }
    String sessionDetailBarGraphJson = null;
    public XMLGregorianCalendar searchMonth;

    public XMLGregorianCalendar getSearchMonth() {
        return searchMonth;
    }

    public boolean getKYCUnverified() {
        // Only return true once if the status is U
        String status = (String) getRequest().getSession().getAttribute("kyc");
        if (status == null) {
            int custId = this.getUserCustomerIdFromSession();
            Customer cust = UserSpecificCachedDataHelper.getCustomer(custId, StCustomerLookupVerbosity.CUSTOMER);
            status = cust.getKYCStatus();
            getRequest().getSession().setAttribute("kyc", status);
            return status.equals("U");
        }
        return false;
    }

    public void setSearchMonth(XMLGregorianCalendar searchMonth) {
        this.searchMonth = searchMonth;
    }

    public void setPGWTransactionData(IPGWTransactionData PGWTransactionData) {
        this.PGWTransactionData = PGWTransactionData;
    }

    public IPGWTransactionData getPGWTransactionData() {
        return PGWTransactionData;
    }

    public Resolution retrieveTransactionHistory() {
        if (getAccountHistoryQuery() == null || searchMonth == null) {
            return showTransactionHistory();
        }
        try {
            Date searchMonthFromGCtoJava = Utils.getJavaDate(searchMonth);
            XMLGregorianCalendar xmlTo = Utils.getDateAsXMLGregorianCalendar(searchMonthFromGCtoJava);
            XMLGregorianCalendar xmlFrom = Utils.getDateAsXMLGregorianCalendar(searchMonthFromGCtoJava);

            getAccountHistoryQuery().setDateFrom(xmlFrom);
            getAccountHistoryQuery().setDateTo(xmlTo);

            Calendar now = getAccountHistoryQuery().getDateTo().toGregorianCalendar();
            getAccountHistoryQuery().getDateTo().setDay(now.getActualMaximum(GregorianCalendar.DAY_OF_MONTH));
            getAccountHistoryQuery().setDateTo(Utils.getBeginningOfNextDay(getAccountHistoryQuery().getDateTo()));

            getAccountHistoryQuery().setVerbosity(StAccountHistoryVerbosity.RECORDS);

            getAccountHistoryQuery().setResultLimit(BaseUtils.getIntProperty("global.account.history.result.limit.scp", 50));
            List<Integer> serviceInstanceIds = new ArrayList<>();

            if (getProductInstance() != null && getProductInstance().getProductInstanceId() > 0) {
                log.debug("History query is for a specific product instance [{}]", getProductInstance().getProductInstanceId());
                ProductInstance pi = UserSpecificCachedDataHelper.getProductInstance(getProductInstance().getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN_SVC);
                for (ProductServiceInstanceMapping mapping : pi.getProductServiceInstanceMappings()) {
                    log.debug("Including SI ID [{}]", mapping.getServiceInstance().getServiceInstanceId());
                    serviceInstanceIds.add(mapping.getServiceInstance().getServiceInstanceId());
                }
                getAccountHistoryQuery().getServiceInstanceIds().addAll(serviceInstanceIds);
            }

            setAccountHistory(SCAWrapper.getUserSpecificInstance().getAccountHistory(getAccountHistoryQuery()));

            if (getAccountHistory().getResultsReturned() <= 0) {
                setPageMessage("no.records.found");
            }
            setAccount(UserSpecificCachedDataHelper.getAccount(getAccountHistoryQuery().getAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES));

            try {
                setAccountSummaryQuery(new AccountSummaryQuery());
                getAccountSummaryQuery().setAccountId(getAccountHistoryQuery().getAccountId());
                getAccountSummaryQuery().setDateFrom(xmlFrom);
                getAccountSummaryQuery().setDateTo(xmlTo);
                getAccountSummaryQuery().getDateTo().setDay(now.getActualMaximum(GregorianCalendar.DAY_OF_MONTH));
                getAccountSummaryQuery().setDateTo(Utils.getBeginningOfNextDay(getAccountSummaryQuery().getDateTo()));
                getAccountSummaryQuery().setVerbosity(StAccountSummaryVerbosity.DAILY);

                if (getProductInstance() != null && getProductInstance().getProductInstanceId() > 0) {
                    log.debug("Account Summary query is for a specific product instance [{}]", getProductInstance().getProductInstanceId());
                    getAccountSummaryQuery().getServiceInstanceIds().addAll(serviceInstanceIds);
                }

                setAccountSummary(SCAWrapper.getUserSpecificInstance().getAccountSummary(getAccountSummaryQuery()));

                setAccount(UserSpecificCachedDataHelper.getAccount(getAccountSummaryQuery().getAccountId(), StAccountLookupVerbosity.ACCOUNT));
                sessionDetailBarGraphJson = SmileTags.getObjectAsJsonString(getAccountSummary());
            } catch (SCABusinessError e) {
                log.warn("Error occured trying to call getAccountSummary");
            }

            Map<Integer, ProductInstance> accountsProductInstances = new HashMap<>();
            for (ServiceInstance si : getAccount().getServiceInstances()) {
                accountsProductInstances.put(si.getProductInstanceId(), UserSpecificCachedDataHelper.getProductInstance(si.getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN));
            }
            setProductInstanceList(new ProductInstanceList());
            getProductInstanceList().getProductInstances().addAll(accountsProductInstances.values());

        } catch (SCABusinessError e) {
            showTransactionHistory();
            throw e;
        }
        return getDDForwardResolution("/account/view_transactions.jsp");
    }

    public Resolution showTransactionHistory() {

        Map<Integer, ProductInstance> accountsProductInstances = new HashMap<>();

        if (getAccountHistoryQuery() != null) {
            if (getAccountQuery() == null) {
                setAccountQuery(new AccountQuery());
            }
            getAccountQuery().setVerbosity(StAccountLookupVerbosity.ACCOUNT);
            setAccount(UserSpecificCachedDataHelper.getAccount(getAccountHistoryQuery().getAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES));
            for (ServiceInstance si : getAccount().getServiceInstances()) {
                accountsProductInstances.put(si.getProductInstanceId(), UserSpecificCachedDataHelper.getProductInstance(si.getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN));
            }

        } else {

            setCustomerWithProductsAndServices();
            setAccountListForCustomer(getCustomer(), StAccountLookupVerbosity.ACCOUNT);
            if (getAccountList().getAccounts() == null || getAccountList().getAccounts().isEmpty()) {
                setPageMessage("no.accounts.found");
                return retrieveAllUserServicesAccounts();
            }

            if (getAccountList().getNumberOfAccounts() > 1) {
                //The verbosity of this customer object is: StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES
                populateProductInstanceListAndServiceInstanceList(getCustomer(), StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES);
                stripesEventName = getContext().getEventName();
                stripesActionBeanName = "Account";
                eventSourceLink = "Transaction History";
                return getDDForwardResolution("/account/select_account_to_manage.jsp");
            } else {
                setAccount(getAccountList().getAccounts().get(0));
                setAccountHistoryQuery(new AccountHistoryQuery());
                getAccountHistoryQuery().setAccountId(getAccount().getAccountId());
            }

            setAccount(UserSpecificCachedDataHelper.getAccount(getAccountList().getAccounts().get(0).getAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES));
            for (ServiceInstance si : getAccount().getServiceInstances()) {
                accountsProductInstances.put(si.getProductInstanceId(), UserSpecificCachedDataHelper.getProductInstance(si.getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN));
            }

        }

        setProductInstanceList(new ProductInstanceList());
        getProductInstanceList().getProductInstances().addAll(accountsProductInstances.values());

        Calendar now = Calendar.getInstance();
        now.add(Calendar.DATE, 0);
        Date dateTo = now.getTime();
        now.add(Calendar.DATE, -1);
        Date dateFrom = now.getTime();

        getAccountHistoryQuery().setDateFrom(Utils.getDateAsXMLGregorianCalendar(dateFrom));
        getAccountHistoryQuery().setDateTo(Utils.getDateAsXMLGregorianCalendar(dateTo));
        setSearchMonth(Utils.getDateAsXMLGregorianCalendar(dateTo));
        return getDDForwardResolution("/account/view_transactions.jsp");
    }

    public Resolution previousAccountsHistoryPage() {
        return showTransactionHistory();
    }

    public Resolution nextAccountsHistoryPage() {
        return showTransactionHistory();
    }

    public Resolution retrieveAccount() {
        log.debug("In retrieveAccount");

        if (getAccountQuery() != null) {

            getAccountQuery().setVerbosity(StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
            setAccount(SCAWrapper.getUserSpecificInstance().getAccount(getAccountQuery()));
            setServiceInstanceList(new ServiceInstanceList());
            setProductInstanceList(new ProductInstanceList());

            //Get unique product instances for the service instances under this account
            Set<Integer> uniqueProductInstanceIdsSet = new HashSet<>();

            if (getAccount().getServiceInstances() != null) {
                for (ServiceInstance si : getAccount().getServiceInstances()) {
                    if (!isSIDeactivatedOrSIMCard(si)) {//dont add a service with status 'TD' to the list OR SIM Card service
                        getServiceInstanceList().getServiceInstances().add(si);
                        uniqueProductInstanceIdsSet.add(si.getProductInstanceId());
                    }
                }
            }

            if (uniqueProductInstanceIdsSet.size() > 0) {
                for (Integer pId : uniqueProductInstanceIdsSet) {
                    ProductInstance pi = UserSpecificCachedDataHelper.getProductInstance(pId, StProductInstanceLookupVerbosity.MAIN);
                    getProductInstanceList().getProductInstances().add(pi);
                }
                getProductInstanceList().setNumberOfProductInstances(getProductInstanceList().getProductInstances().size());
            }

            setCustomer(UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER));
            bundleBalance = 0.00d;
            minutesBalance = 0;
            Date now = new Date();
            List<UnitCreditInstance> unitCreditInstancesToDisplay = new ArrayList<>();

            for (UnitCreditInstance uci : getAccount().getUnitCreditInstances()) {
                UnitCreditSpecification ucs = NonUserSpecificCachedDataHelper.getUnitCreditSpecification(uci.getUnitCreditSpecificationId());
                if (ucs.getConfiguration().contains("DonotDisplayUCInstanceOnMySmile=true")) {
                    continue;
                }
                unitCreditInstancesToDisplay.add(uci);
                // Only include data that is not from some umlimited bundles and is currently available for use
                if (ucs.getUnitType().equalsIgnoreCase("byte")
                        && (!ucs.getConfiguration().contains("DisplayBalance=false")
                        && (!ucs.getConfiguration().contains("DisplayUnitsType=sec") && !ucs.getConfiguration().contains("DisplayUnitsType=minute")))
                        && Utils.getJavaDate(uci.getStartDate()).before(now)
                        && uci.getAvailableUnitsRemaining() > 0) {
                    bundleBalance += uci.getAvailableUnitsRemaining();
                }
            }
            //reset
            getAccount().getUnitCreditInstances().clear();
            getAccount().getUnitCreditInstances().addAll(unitCreditInstancesToDisplay);

            for (UnitCreditInstance uci : getAccount().getUnitCreditInstances()) {
                UnitCreditSpecification ucs = NonUserSpecificCachedDataHelper.getUnitCreditSpecification(uci.getUnitCreditSpecificationId());
                // Only include data that is not from some umlimited bundles and is currently available for use
                if ((ucs.getUnitType().equalsIgnoreCase("sec") || ucs.getUnitType().equalsIgnoreCase("second")
                        && !ucs.getConfiguration().contains("DisplayBalance=false"))
                        || (ucs.getUnitType().equalsIgnoreCase("byte") && !ucs.getConfiguration().contains("DisplayBalance=false") && !ucs.getConfiguration().contains("DisplayUnitsType=byte"))
                        && Utils.getJavaDate(uci.getStartDate()).before(now)
                        && uci.getAvailableUnitsRemaining() > 0) {

                    int timeLeft;
                    log.debug("AUX COUNTER RETURNED IS: {}", uci.getAuxCounter1());
                    if (ucs.getConfiguration().contains("PeriodicLimitUnits=")) {
                        String displayUnitsType = Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "DisplayUnitsType");
                        if (displayUnitsType == null || displayUnitsType.equals("byte")) {
                            continue;
                        }
                        timeLeft = SmileTags.getTimeLeftBasedOnUnitsUsedFromBaseline(Long.parseLong(Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "MaxSpeedbps")),
                                uci.getAuxCounter1(), uci.getUnitType(),
                                Double.parseDouble(Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "PeriodicLimitUnits")),
                                Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "PeriodicLimitUnitType"), Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "DisplayUnitsType"));

                        if (displayUnitsType.equals("minute")) {
                            timeLeft = timeLeft * 60;
                        }
                        minutesBalance += timeLeft;

                    } else {
                        minutesBalance += uci.getAvailableUnitsRemaining();
                    }
                }
            }
        }
        log.debug("Finished retrieveAccount");
        return getDDForwardResolution("/account/view_account.jsp");
    }

    @DefaultHandler
    public Resolution retrieveAllUserServicesAccounts() {
        if (termsAndConditionsPending(getUserCustomerIdFromSession())) {
            return getDDForwardResolution("/termsAndConditions/tcs.jsp");
        }

        setCustomerWithProductsAndServices();
        setAccountListForCustomer(getCustomer(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);

        if (getAccountList().getAccounts() != null || !getAccountList().getAccounts().isEmpty()) {
            if (getAccountList().getNumberOfAccounts() == 1) {
                setAccountQuery(new AccountQuery());
                getAccountQuery().setAccountId(getAccountList().getAccounts().get(0).getAccountId());
                return retrieveAccount();
            } else {
                //The verbosity of this customer object is: StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES         
                populateProductInstanceListAndServiceInstanceList(getCustomer(), StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES);
            }
        }
        return getDDForwardResolution("/account/customer_accounts.jsp");
    }

    public Resolution showRedeemPrepaidVoucher() {

        if (getAccountQuery() != null) {
            setAccount(UserSpecificCachedDataHelper.getAccount(getAccountQuery().getAccountId(), StAccountLookupVerbosity.ACCOUNT));
            return getDDForwardResolution("/account/redeem_voucher.jsp");
        }
        setCustomerWithProductsAndServices();
        setAccountListForCustomer(getCustomer(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
        if (getAccountList() == null || getAccountList().getNumberOfAccounts() <= 0) {
            setPageMessage("no.accounts.found");
            return retrieveAllUserServicesAccounts();
        }
        if (getAccountList().getNumberOfAccounts() > 1) {
            stripesEventName = getContext().getEventName();
            stripesActionBeanName = "Account";
            eventSourceLink = "Recharge";
            return getDDForwardResolution("/account/select_account_to_manage.jsp");
        } else {
            setAccount(getAccountList().getAccounts().get(0));
            return getDDForwardResolution("/account/redeem_voucher.jsp");
        }

    }

    public Resolution redeemPrepaidStrip() {
        //Cross Site Reference Forgery
        checkCSRF();
        if (notConfirmed()) {
            return confirm();
        }
        String rawPIN = getPrepaidStripRedemptionData().getEncryptedPIN();
        getPrepaidStripRedemptionData().setEncryptedPIN(Codec.stringToEncryptedHexString(rawPIN));
        Done isDone = SCAWrapper.getUserSpecificInstance().redeemPrepaidStrip(getPrepaidStripRedemptionData());
        if (isDone.getDone().equals(StDone.TRUE)) {
            setPageMessage("voucher.redeemed.succesfully");
        }
        return retrieveAccount();
    }

    public Resolution changeAccountStatus() {

        try {
            if (!BaseUtils.getPropertyAsSet("global.account.allowed.statuses.scp").contains(String.valueOf(getAccount().getStatus()))) {
                localiseErrorAndAddToGlobalErrors("scp.account.status.change.unknown.option.selected");
                return retrieveAccount();
            }
            Account acc = SCAWrapper.getUserSpecificInstance().getAccount(getAccount().getAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS);

            /*
                If account status from DB is equal to one of those that cannot be overriden
                then disallow account status update... these statuses are held by the property
                env.immutable.account.statuses.scp
             */
            if (!BaseUtils.getProperty("env.immutable.account.statuses.scp", "").isEmpty()) {
                if (BaseUtils.getPropertyAsSet("env.immutable.account.statuses.scp").contains(String.valueOf(acc.getStatus()))) {
                    localiseErrorAndAddToGlobalErrors("scp.account.status.override.notallowed");
                    return retrieveAccount();
                }
            }

            for (Reservation res : acc.getReservations()) {
                if (res.getDescription().equals("SMS") && getAccount().getStatus() == 14) {
                    localiseErrorAndAddToGlobalErrors("scp.account.status.allusage.selected.withactive.reservation");
                    return retrieveAccount();
                } else if (res.getDescription().equals("Voice") && getAccount().getStatus() == 14) {
                    localiseErrorAndAddToGlobalErrors("scp.account.status.allusage.selected.withactive.reservation");
                    return retrieveAccount();
                }/* else if (res.getDescription().equals("Data") && getAccount().getStatus() == 14) {//Need to be tested; one might only be able to change this if not browsing at all
                    localiseErrorAndAddToGlobalErrors("scp.account.status.allusage.selected.withactive.reservation");
                    return retrieveAccount();
                }*/
            }

            SCAWrapper.getUserSpecificInstance().modifyAccount(getAccount());
            setPageMessage("account.status.change.success");
        } catch (SCABusinessError e) {
            setPageMessage("account.status.change.error");
            retrieveAccount();
            throw e;
        }
        return retrieveAccount();
    }

    private Set<String> unitCreditSections;

    public Set<String> getUnitCreditSections() {
        return unitCreditSections;
    }

    @DontValidate()
    public Resolution showAddUnitCredits() {

        log.debug("Entering showAddUnitCredits");

        if (getContext().getValidationErrors().hasFieldErrors()) {
            ValidationErrors er = getContext().getValidationErrors();
            Set<String> keySet = er.keySet();
            for (String field : keySet) {
                if (field.equals("productInstanceIdForSIM")) {
                    log.warn("Field 'productInstanceIdForSIM' failed validation. Going to clear errors");
                    clearValidationErrors();//Clear productInstanceIdForSIM errors
                }
            }
        }

        if (getAccountQuery() != null || getAccount() != null) {

            setCustomer(UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER));
            if (getAccountQuery() != null) {
                log.debug("getAccountQuery is not null, going to search using that");
                getAccountQuery().setVerbosity(StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
                setAccount(SCAWrapper.getUserSpecificInstance().getAccount(getAccountQuery()));
            } else {
                log.debug("getAccount is not null going to search using that object");
                long accId = getAccount().getAccountId();
                setAccount(SCAWrapper.getUserSpecificInstance().getAccount(accId, StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES));
            }
        } else {
            log.debug("getAccountQuery and getAccount are null going to do populate account based on customer session");
            setCustomerWithProductsAndServices();
            setAccountListForCustomer(getCustomer(), StAccountLookupVerbosity.ACCOUNT);
            if (getAccountList().getAccounts() == null || getAccountList().getAccounts().isEmpty()) {
                setPageMessage("no.accounts.found");
                return retrieveAllUserServicesAccounts();
            }
            log.debug("Done setting customer and accountList objects, accountList is not empty");
            //If the is more than one account, go the customized page for bundle purchase
            if (getAccountList().getAccounts().size() > 1) {
                return retrieveAllUserServicesAccounts();
            }

            log.debug("AccountList is not empty, going set account object from the first account in the list");
            long accId = getAccountList().getAccounts().get(0).getAccountId();
            setAccount(SCAWrapper.getUserSpecificInstance().getAccount(accId, StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES));
        }

        setUnitCreditSpecificationList(new UnitCreditSpecificationList());
        UnitCreditSpecificationQuery ucsq = new UnitCreditSpecificationQuery();
        ucsq.setUnitCreditSpecificationId(-1);
        ucsq.setVerbosity(StUnitCreditSpecificationLookupVerbosity.MAIN_SVCSPECIDS);
        UnitCreditSpecificationList allUCs = (SCAWrapper.getUserSpecificInstance().getUnitCreditSpecifications(ucsq));

        Set<Integer> productInstanceIdSet = new HashSet<>();
        Set<Integer> campaignUC = new HashSet<>();
        Set<Integer> campaignID = new HashSet<>();

        for (ServiceInstance svc : getAccount().getServiceInstances()) {
            productInstanceIdSet.add(svc.getProductInstanceId());
        }

        for (Integer pi : productInstanceIdSet) {
            campaignUC.addAll(getProductInstanceCampaignUCSet(pi));
            campaignID.addAll(getProductInstanceCampaignIDSet(pi));
        }

        unitCreditSections = new TreeSet();
        unitCreditSections.add("default");
        Random r = new Random();
        for (UnitCreditSpecification ucs : allUCs.getUnitCreditSpecifications()) {

            String mySmileUCDiscount = Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "MySmileDiscountPercent");

            if (mySmileUCDiscount != null && mySmileUCDiscount.length() > 0) {
                double discountPercOff = Double.parseDouble(mySmileUCDiscount);
                // Apply the discount
                ucs.setPriceInCents(ucs.getPriceInCents() * (1 - discountPercOff / 100d));

            }

            if (!campaignUC.contains(ucs.getUnitCreditSpecificationId()) && Utils.getBooleanValueFromCRDelimitedAVPString(ucs.getConfiguration(), "CampaignsOnly")) {
                log.debug("This UC is for campaigns only");
                continue;
            }

            if (ucs.getConfiguration().contains("BundleCannotBeSoldAsStandAlone=true")) {
                log.debug("This UC shouldn't be displayed on product catalogue");
                continue;
            }

            if (ucs.getConfiguration().contains("BundleNotVisibleOnSCPWhenOnCampaignId=")) {
                Set<String> restrictingSet = Utils.getSetFromCommaDelimitedString(Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "BundleNotVisibleOnSCPWhenOnCampaignId"));
                boolean skipFromDisplay = false;

                for (String campaignId : restrictingSet) {
                    log.debug("CampaignId to test for restriction: {}", campaignId);
                    if (campaignID.contains(Integer.parseInt(campaignId))) {
                        skipFromDisplay = true;
                        break;
                    }
                }
                if (skipFromDisplay) {
                    log.debug("This UC shouldn't be displayed on product catalogue alternative exist for customer");
                    continue;
                }
            }
            
            
            if (UCCanBeSoldWhenSpecIDExist(ucs)) {
                    String allowedSpecIds = Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "CanBeSoldWhenSpecIDExist");

                    if (!(allowedSpecIds == null || allowedSpecIds.length() <= 0)) { //If we have SpecIDs that must exist in user profile
                        
                        //Get account we are purchasing for. To check if it has the main required unit credit
                        Account ucAccount = getAccount();
                        
                        List<UnitCreditInstance> clientExistingUnitCreditCredInstances = ucAccount.getUnitCreditInstances();                        
                        List<String> specIDsForUpsize = Utils.getListFromCommaDelimitedString(allowedSpecIds);
                        boolean allowedSpecIdExists = false;

                        for (String allowedSpecId : specIDsForUpsize) {
                            for(UnitCreditInstance clientUnitCred :  clientExistingUnitCreditCredInstances) {                                
                                if(clientUnitCred.getUnitCreditSpecificationId() == Integer.parseInt(allowedSpecId)) {
                                    allowedSpecIdExists = true;
                                    break;
                                }
                            }
                            
                            if(allowedSpecIdExists)
                                break;
                        }

                        if(!allowedSpecIdExists) {                            
                            log.debug("Main product not found.");
                             continue;
                        }
                    }
            }

            if (!ucs.getConfiguration().contains("MySmileSection=")) {
                // default to default section if the config has not been set
                ucs.setConfiguration(ucs.getConfiguration() + "\r\nMySmileSection=default");
            }

            if (!ucs.getConfiguration().contains("MySmileBubbleColor=")) {
                // default to a random color if the config has not been set
                ucs.setConfiguration(ucs.getConfiguration() + "\r\nMySmileBubbleColor=" + getRandomBubbleColor(r));
            }
            if (Utils.isBetween(new Date(), ucs.getAvailableFrom(), ucs.getAvailableTo())) {
                for (ProductServiceMapping psm : ucs.getProductServiceMappings()) {
                    for (ServiceInstance si : getAccount().getServiceInstances()) {

                        if (si.getServiceSpecificationId() == psm.getServiceSpecificationId()
                                && (psm.getProductSpecificationId() == 0
                                || psm.getProductSpecificationId() == UserSpecificCachedDataHelper.getProductInstance(si.getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN).getProductSpecificationId())) {
                            //The following code checks if UC already exists to avoid duplicates
                            boolean isFound = false;
                            for (UnitCreditSpecification ucss : getUnitCreditSpecificationList().getUnitCreditSpecifications()) {
                                if (ucss.getUnitCreditSpecificationId() == ucs.getUnitCreditSpecificationId()) {
                                    isFound = true;
                                    break;
                                }
                            }
                            if (!isFound) {
                                // Check UC purchase roles if allowed to purchase or just allow if its a campaign UC
                                if (isAllowed(ucs.getPurchaseRoles())) {
                                    if (isAllowedInThisLocation(ucs)) {
                                        getUnitCreditSpecificationList().getUnitCreditSpecifications().add(ucs);
                                        String section = Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "MySmileSection");
                                        unitCreditSections.add(section == null ? "default" : section);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (unitCreditSections.contains("unlimited")) {
            Set<String> tmp = new LinkedHashSet();
            tmp.add("unlimited");
            if (unitCreditSections.contains("voicebundle")) {//voice bundles to be placed under unlimited: HBT-5149
                tmp.add("voicebundle");
            }
            for (String section : unitCreditSections) {
                if (section.equals("unlimited") || section.equals("voicebundle")) {
                    continue;
                }
                tmp.add(section);
            }
            unitCreditSections = tmp;
        }

        if (unitCreditSections.contains("promos")) {
            Set<String> tmp = new LinkedHashSet();
            tmp.add("promos");
            for (String section : unitCreditSections) {
                if (section.equals("promos")) {
                    continue;
                }
                tmp.add(section);
            }
            unitCreditSections = tmp;
        }

        if (BaseUtils.getBooleanProperty("env.scp.unitcredit.sections.override", false)) {
            Set<String> tmp = new TreeSet();
            tmp.addAll(unitCreditSections);
            try {
                List<String> sectionsList = new ArrayList<>();
                sectionsList.addAll(unitCreditSections);
                unitCreditSections.clear();//reset
                unitCreditSections.addAll(orderList(sectionsList));
            } catch (Exception ex) {
                unitCreditSections = tmp;
            }

            //Can happen if a section has not been added in the property list
            if (tmp.size() != unitCreditSections.size()) {
                for (String section : tmp) {
                    if (!unitCreditSections.contains(section)) {
                        unitCreditSections.add(section);
                    }
                }
            }
        }

        if (getUnitCreditSpecificationList().getUnitCreditSpecifications().size() <= 0) {
            setPageMessage("scp.acc.si.hasno.ucimapping", getAccount().getAccountId());
        }

        return getDDForwardResolution("/products/add_unit_credit.jsp");
    }

    private Set<Integer> getProductInstanceCampaignUCSet(int productInstanceId) {
        Set<Integer> campaignUC = new HashSet<>();
        try {
            ProductInstance pi = UserSpecificCachedDataHelper.getProductInstance(productInstanceId, StProductInstanceLookupVerbosity.MAIN_CAMPAIGNS_CAMPAIGNUC);
            for (CampaignData cd : pi.getCampaigns()) {
                campaignUC.addAll(cd.getCampaignUnitCredits());
            }
        } catch (Exception e) {
            log.warn("Error: ", e);
        }
        return campaignUC;
    }

    private Set<Integer> getProductInstanceCampaignIDSet(int productInstanceId) {
        Set<Integer> campaignID = new HashSet<>();
        try {
            ProductInstance pi = UserSpecificCachedDataHelper.getProductInstance(productInstanceId, StProductInstanceLookupVerbosity.MAIN_CAMPAIGNS_CAMPAIGNUC);
            for (CampaignData cd : pi.getCampaigns()) {
                campaignID.add(cd.getCampaignId());
            }
        } catch (Exception e) {
            log.warn("Error: ", e);
        }
        return campaignID;
    }

    private boolean isCampaignsOnlyUCInProductInstancesUCCampaignList(int productInstanceId, int ucId) {
        boolean campaignUC = false;
        try {
            ProductInstance pi = UserSpecificCachedDataHelper.getProductInstance(productInstanceId, StProductInstanceLookupVerbosity.MAIN_CAMPAIGNS_CAMPAIGNUC);
            for (CampaignData cd : pi.getCampaigns()) {
                for (Integer i : cd.getCampaignUnitCredits()) {
                    if (ucId == i) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error: ", e);
        }
        return campaignUC;
    }

    private List<PurchaseUnitCreditLine> purchaseUnitCreditLines;
    private PurchaseUnitCreditLine purchaseUnitCreditLine;

    public PurchaseUnitCreditLine getPurchaseUnitCreditLine() {
        return purchaseUnitCreditLine;
    }

    public void setPurchaseUnitCreditLine(PurchaseUnitCreditLine purchaseUnitCreditLine) {
        this.purchaseUnitCreditLine = purchaseUnitCreditLine;
    }

    public List<PurchaseUnitCreditLine> getPurchaseUnitCreditLines() {
        if (purchaseUnitCreditLines == null) {
            purchaseUnitCreditLines = new ArrayList<>();
        }
        return this.purchaseUnitCreditLines;
    }

    @DontValidate()
    public Resolution provisionUnitCreditByPaymentType() {

        if (getContext().getValidationErrors().hasFieldErrors()) {
            clearValidationErrors();
        }

        String gatewayCode = (getParameter("gatewayCode") == null ? "" : getParameter("gatewayCode"));
        if (gatewayCode.isEmpty()) {
            localiseErrorAndAddToGlobalErrors("unit.credit.select.payment.option");
            return retrieveAllUserServicesAccounts();
        }
        if (gatewayCode.equalsIgnoreCase("Wallet")) {
            log.debug("Provision bundle using wallet");
            return provisionUnitCredit();
        } else {
            log.debug("Provision bundle using payment gateway");
            return provisionUnitCreditViaPaymentGateway();
        }

    }

//    public Resolution showRechargePaymentOptions() {
//        setCustomer(UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER));
//        return getDDForwardResolution("/products/recharge_payment_options.jsp");
//    }
    @DontValidate()
    public Resolution provisionUnitCredit() {
        checkCSRF();
        boolean validationFailed = false;
        if (getContext().getValidationErrors().hasFieldErrors()) {
            ValidationErrors er = getContext().getValidationErrors();
            Set<String> keySet = er.keySet();
            for (String field : keySet) {
                if (field.equals("purchaseUnitCreditRequest.paidByAccountId")) {
                    log.debug("Field 'purchaseUnitCreditRequest.paidByAccountId' failed validation. Going to clear errors");
                    clearValidationErrors();//Clear purchaseUnitCreditRequest.paidByAccountId errors
                    validationFailed = true;
                }
            }
        }

        setCustomer(UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES));
        if (validationFailed) {
            setAccountListAndRemoveSpecialAndInactiveAccounts(getCustomer());
            if (getAccountList().getAccounts().size() > 1) {
                TPGWPartnerCode = getParameter("gatewayCode");
                return getDDForwardResolution("/products/add_unit_credit_select_paying_account.jsp");
            } else {
                getPurchaseUnitCreditRequest().setPaidByAccountId(getAccountList().getAccounts().get(0).getAccountId());
            }
        }

        if (getPurchaseUnitCreditRequest().getPaidByAccountId() == 0) {
            setAccountListAndRemoveSpecialAndInactiveAccounts(getCustomer());
            if (getAccountList().getAccounts().size() == 1) {
                getPurchaseUnitCreditRequest().setPaidByAccountId(getAccountList().getAccounts().get(0).getAccountId());
            }
        }

        try {
            if (getPurchaseUnitCreditRequest().getPaidByAccountId() > 0) {

                setAccountListAndRemoveSpecialAndInactiveAccounts(getCustomer());
                long reciepientAcc = getPurchaseUnitCreditRequest().getAccountId();
                long salesAcc = getPurchaseUnitCreditRequest().getPaidByAccountId();
                /*Check if allowed to access the sales account data*/
                boolean isAllowedAccessToAccountEntity = false;
                for (Account acc : getAccountList().getAccounts()) {
                    if (acc.getAccountId() == salesAcc) {
                        isAllowedAccessToAccountEntity = true;
                        break;
                    }
                }
                if (!isAllowedAccessToAccountEntity) {
                    log.warn("Customer [{}] requested to buy bundle to account ID [{}] but has no access to account [{}]. Not going to process request further.", new Object[]{getCustomer().getSSOIdentity(), reciepientAcc, salesAcc});
                    return retrieveAllUserServicesAccounts();
                }
            }
            UnitCreditSpecificationQuery q = new UnitCreditSpecificationQuery();
            q.setUnitCreditSpecificationId(getPurchaseUnitCreditRequest().getUnitCreditSpecificationId());
            q.setVerbosity(StUnitCreditSpecificationLookupVerbosity.MAIN_SVCSPECIDS);
            UnitCreditSpecification ucs = SCAWrapper.getUserSpecificInstance().getUnitCreditSpecifications(q).getUnitCreditSpecifications().get(0);
            setUnitCreditSpecification(ucs);
            String mySmileUCDiscount = Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "MySmileDiscountPercent");

            if (mySmileUCDiscount != null && mySmileUCDiscount.length() > 0) {
                double discountPercOff = Double.parseDouble(mySmileUCDiscount);
                // Apply the discount
                ucs.setPriceInCents(ucs.getPriceInCents() * (1 - discountPercOff / 100d));

            }

            if (!isAllowed(ucs.getPurchaseRoles())) {
                localiseErrorAndAddToGlobalErrors("unit.credit.not.allowed");
                return retrieveAllUserServicesAccounts();
            }

            boolean provisionUpsize = false;
            UnitCreditSpecification upsizeUCSpec = null;            
            if (UCHasExtraUpsizeConfig() && (getPurchaseUnitCreditLine() != null && getPurchaseUnitCreditLine().getUnitCreditSpecificationId() > 0)) {
                log.debug("UCS Id [{}] is to be provisioned with upsize bundle with spec id [{}]", ucs.getUnitCreditSpecificationId(), getPurchaseUnitCreditLine().getUnitCreditSpecificationId());
                q = new UnitCreditSpecificationQuery();
                q.setUnitCreditSpecificationId(getPurchaseUnitCreditLine().getUnitCreditSpecificationId());
                q.setVerbosity(StUnitCreditSpecificationLookupVerbosity.MAIN_SVCSPECIDS);
                upsizeUCSpec = SCAWrapper.getUserSpecificInstance().getUnitCreditSpecifications(q).getUnitCreditSpecifications().get(0);                
                
                if (upsizeUCSpec != null && isAllowed(upsizeUCSpec.getPurchaseRoles())) {
                    String upsizeMySmileUCDiscount = Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "MySmileDiscountPercent");

                    if (upsizeMySmileUCDiscount != null && upsizeMySmileUCDiscount.length() > 0) {
                        double discountPercOff = Double.parseDouble(upsizeMySmileUCDiscount);
                        upsizeUCSpec.setPriceInCents(upsizeUCSpec.getPriceInCents() * (1 - discountPercOff / 100d));
                    }

                    String val = Utils.getValueFromCRDelimitedAVPString(upsizeUCSpec.getConfiguration(), "CanOnlyBeSoldWithTheseLineItems");
                    
                    log.warn("CanOnlyBeSoldWithTheseLineItems {}", val);
                    if (!(val == null || val.length() <= 0)) {
                        List<String> itemsForUpsize = Utils.getListFromCommaDelimitedString(val);
                        for (String itemNum : itemsForUpsize) {
                            if (itemNum.equals(ucs.getItemNumber())) {
                                provisionUpsize = true;
                                break;
                            }
                        }
                    }                   
                    
                }
            }

            if (UCCanBeSoldWhenSpecIDExist(ucs)) {
                    String allowedSpecIds = Utils.getValueFromCRDelimitedAVPString(getUnitCreditSpecification().getConfiguration(), "CanBeSoldWhenSpecIDExist");

                    if (!(allowedSpecIds == null || allowedSpecIds.length() <= 0)) { //If we have SpecIDs that must exist in user profile
                        
                        //Get account we are purchasing for. To check if it has the main required unit credit
                        Account ucAccount = new Account();                        
                        for (Account acc : getAccountList().getAccounts()) {
                            if (acc.getAccountId() == getPurchaseUnitCreditRequest().getAccountId()) {
                                ucAccount = acc;
                                break;
                            }
                        }
                        
                        List<UnitCreditInstance> clientExistingUnitCreditCredInstances = ucAccount.getUnitCreditInstances();                        
                        List<String> specIDsForUpsize = Utils.getListFromCommaDelimitedString(allowedSpecIds);
                        boolean allowedSpecIdExists = false;

                        for (String allowedSpecId : specIDsForUpsize) {
                            for(UnitCreditInstance clientUnitCred :  clientExistingUnitCreditCredInstances) {                                
                                if(clientUnitCred.getUnitCreditSpecificationId() == Integer.parseInt(allowedSpecId)) {
                                    allowedSpecIdExists = true;
                                    break;
                                }
                            }
                            
                            if(allowedSpecIdExists)
                                break;
                        }

                        if(!allowedSpecIdExists) {
                            provisionUpsize = false;
                            log.warn("Customer requested to buy bundle but does not have required main product.");
                            localiseErrorAndAddToGlobalErrors("unit.credit.not.allowed.without.spec.ids");
                            return retrieveAllUserServicesAccounts();
                        }
                    }
            }

            if (notConfirmed()) {
                Calendar end = Calendar.getInstance();
                if (getPurchaseUnitCreditRequest().getDaysGapBetweenStart() < 0) {
                    // Each one will start when the previous expires
                    if (ucs.getConfiguration().contains("ValidityIsMonths=true")) {
                        end.add(Calendar.MONTH, ucs.getUsableDays() * getPurchaseUnitCreditRequest().getNumberToPurchase());
                    } else if (ucs.getConfiguration().contains("ValidityIsHours=true")) {
                        end.add(Calendar.HOUR, ucs.getUsableDays() * getPurchaseUnitCreditRequest().getNumberToPurchase());
                    } else {
                        end.add(Calendar.DATE, ucs.getUsableDays() * getPurchaseUnitCreditRequest().getNumberToPurchase());
                    }
                } else if (ucs.getConfiguration().contains("ValidityIsMonths=true")) {
                    end.add(Calendar.MONTH, ucs.getUsableDays() + (getPurchaseUnitCreditRequest().getDaysGapBetweenStart() * getPurchaseUnitCreditRequest().getNumberToPurchase()));
                } else if (ucs.getConfiguration().contains("ValidityIsHours=true")) {
                    end.add(Calendar.HOUR, ucs.getUsableDays() + (getPurchaseUnitCreditRequest().getDaysGapBetweenStart() * getPurchaseUnitCreditRequest().getNumberToPurchase()));
                } else {
                    end.add(Calendar.DATE, ucs.getUsableDays() + (getPurchaseUnitCreditRequest().getDaysGapBetweenStart() * getPurchaseUnitCreditRequest().getNumberToPurchase()));
                }
                XMLGregorianCalendar unitCreditEndDate = Utils.getDateAsXMLGregorianCalendar(end.getTime());
                if (provisionUpsize && upsizeUCSpec != null) {
                    return confirm(ucs.getName() + " + " + upsizeUCSpec.getName(), SmileTags.convertCentsToCurrencyShort(ucs.getPriceInCents() + upsizeUCSpec.getPriceInCents()), String.valueOf(ucs.getUsableDays()), SmileTags.formatDateShort(unitCreditEndDate));
                } else {
                    return confirm(ucs.getName(), SmileTags.convertCentsToCurrencyShort(ucs.getPriceInCents()), String.valueOf(ucs.getUsableDays()), SmileTags.formatDateShort(unitCreditEndDate));
                }

            }

            getPurchaseUnitCreditRequest().setNumberToPurchase(1);
            getPurchaseUnitCreditRequest().setItemNumber(ucs.getItemNumber());
            getPurchaseUnitCreditRequest().setPaymentMethod(PAYMENT_METHOD_AIRTIME);
            getPurchaseUnitCreditRequest().setCreditAccountNumber("");
            getPurchaseUnitCreditRequest().setChannel(BaseUtils.getSubProperty("env.mysmile.airtime.config", "MySmileX3Channel"));

            if (!provisionUpsize) {
                try {
                    SCAWrapper.getUserSpecificInstance().purchaseUnitCredit(getPurchaseUnitCreditRequest());
                } catch (SCAErr ex) {
                    if (ex.getErrorCode().equalsIgnoreCase("BM-0001") || ex.getErrorCode().equalsIgnoreCase("BM-0007") || ex.getErrorCode().equalsIgnoreCase("POS-0057")) {

                        Account account = UserSpecificCachedDataHelper.getAccount(getPurchaseUnitCreditRequest().getPaidByAccountId(), StAccountLookupVerbosity.ACCOUNT);
                        //CREDIT_BARRED = 1; // e.g. receiving me2u transfers, topping up etc
                        //DEBIT_BARRED = 2; // e.g. sending me2u transfers, buying bundles etc
                        //UNIT_CREDIT_CHARGE_BARRED = 4; // e.g. in bundle usage
                        //MONETARY_CHARGE_BARRED = 8; // e.g. out of bundle usage (does not stop rate group 9000 charges
                        if (account.getStatus() == 14 || account.getStatus() == 223) {
                            localiseErrorAndAddToGlobalErrors("scp.recharge.error.account.status.blocking", String.valueOf(account.getAccountId()));
                            return showUnitCreditPaymentMethodPage();
                        }
                        localiseErrorAndAddToGlobalErrors("scp.recharge.error.insufficient.funds", String.valueOf(account.getAccountId()));
                        return showUnitCreditPaymentMethodPage();
                    }
                    throw ex;
                }

            } else {

                setSale(new Sale());

                getSale().setSaleTotalCentsIncl(ucs.getPriceInCents());
                getSale().setRecipientCustomerId(getUserCustomerIdFromSession());
                getSale().setPaymentMethod(PAYMENT_METHOD_AIRTIME);
                getSale().setRecipientPhoneNumber(getCustomer().getAlternativeContact1());

                if (getPurchaseUnitCreditRequest().getProductInstanceId() > 0) {
                    int organisationId = UserSpecificCachedDataHelper.getProductInstance(getPurchaseUnitCreditRequest().getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN).getOrganisationId();
                    getSale().setRecipientOrganisationId(organisationId);

                }

                getSale().setSaleLocation("");
                getSale().setRecipientAccountId(getPurchaseUnitCreditRequest().getAccountId());
                getSale().setRecipientName(getCustomer().getFirstName());
                getSale().setChannel(BaseUtils.getSubProperty("env.mysmile.airtime.config", "MySmileX3Channel"));
                getSale().setWarehouseId("");
                getSale().setPromotionCode("");
                getSale().setTaxExempt(false);
                getSale().setSalesPersonAccountId(getPurchaseUnitCreditRequest().getPaidByAccountId());
                getSale().setSalesPersonCustomerId(1); // Make sale as admin
                getSale().setPurchaseOrderData("");
                getSale().setExtraInfo("");
                getSale().setChangeCents(0);
                getSale().setAmountTenderedCents(0);
                getSale().setPaymentTransactionData("");
                getSale().setWithholdingTaxRate(0);
                getSale().setCreditAccountNumber("");

                SaleLine line = new SaleLine();
                line.setLineNumber(1);
                line.setQuantity(1);
                line.setLineTotalCentsIncl(ucs.getPriceInCents());
                line.setProvisioningData("ProductInstanceId=" + getPurchaseUnitCreditRequest().getProductInstanceId() + "\r\nDaysGapBetweenStart=" + getPurchaseUnitCreditRequest().getDaysGapBetweenStart());

                InventoryItem ii = new InventoryItem();
                ii.setItemNumber(ucs.getItemNumber());
                ii.setSerialNumber("");
                line.setInventoryItem(ii);
                getSale().getSaleLines().add(line);

                line = new SaleLine();
                line.setLineNumber(2);
                line.setQuantity(1);
                line.setLineTotalCentsIncl(upsizeUCSpec.getPriceInCents());
                line.setProvisioningData("ProductInstanceId=" + getPurchaseUnitCreditRequest().getProductInstanceId() + "\r\nDaysGapBetweenStart=" + getPurchaseUnitCreditRequest().getDaysGapBetweenStart());

                ii = new InventoryItem();
                ii.setItemNumber(upsizeUCSpec.getItemNumber());
                ii.setSerialNumber("");
                line.setInventoryItem(ii);
                getSale().getSaleLines().add(line);

                getSale().setSaleTotalCentsIncl(getSale().getSaleTotalCentsIncl() + upsizeUCSpec.getPriceInCents());

                try {
                    SCAWrapper.getUserSpecificInstance().processSale(getSale());
                } catch (SCAErr ex) {
                    if (ex.getErrorCode().equalsIgnoreCase("BM-0001") || ex.getErrorCode().equalsIgnoreCase("BM-0007") || ex.getErrorCode().equalsIgnoreCase("POS-0057")) {

                        Account account = UserSpecificCachedDataHelper.getAccount(getPurchaseUnitCreditRequest().getPaidByAccountId(), StAccountLookupVerbosity.ACCOUNT);
                        if (account.getStatus() == 14 || account.getStatus() == 223) {
                            localiseErrorAndAddToGlobalErrors("scp.recharge.error.account.status.blocking", String.valueOf(account.getAccountId()));
                            return showUnitCreditPaymentMethodPage();
                        }
                        localiseErrorAndAddToGlobalErrors("scp.recharge.error.insufficient.funds", String.valueOf(account.getAccountId()));
                        return showUnitCreditPaymentMethodPage();
                    }
                    throw ex;
                }

            }

            setPageMessage("unit.credit.added");

        } catch (SCABusinessError e) {
            retrieveAllUserServicesAccounts();
            throw e;
        }
        return retrieveAccount();
    }

    private boolean UCHasExtraUpsizeConfig() {
        String val = Utils.getValueFromCRDelimitedAVPString(getUnitCreditSpecification().getConfiguration(), "ExtraUpsizeSpecIds");        
        return !(val == null || val.length() <= 0);
    }
    
    private boolean UCCanBeSoldWhenSpecIDExist(UnitCreditSpecification ucs) {      
        String val = Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "CanBeSoldWhenSpecIDExist");
        return !(val == null || val.length() <= 0);
    }    

    private boolean UCHasDisplayRestrictionConfig() {
        String val = Utils.getValueFromCRDelimitedAVPString(getUnitCreditSpecification().getConfiguration(), "DisplayRestriction");
        return !(val == null || val.length() <= 0);
    }

    public Resolution previousAccountsPage() {
        return retrieveAllUserServicesAccounts();
    }

    public Resolution nextAccountsPage() {
        return retrieveAllUserServicesAccounts();
    }

    private void setCustomerWithProductsAndServices() {
        if (getCustomerQuery() == null) {
            setCustomerQuery(new CustomerQuery());
        }
        getCustomerQuery().setCustomerId(getUserCustomerIdFromSession());
        getCustomerQuery().setResultLimit(1);
        getCustomerQuery().setProductInstanceResultLimit(BaseUtils.getIntProperty("env.scp.pagesize", 30));
        if (getCustomerQuery().getProductInstanceOffset() == null) {
            getCustomerQuery().setProductInstanceOffset(0);
        }

        setCustomer(UserSpecificCachedDataHelper.getCustomer(getCustomerQuery().getCustomerId(),
                StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES,
                getCustomerQuery().getProductInstanceOffset(),
                getCustomerQuery().getProductInstanceResultLimit()));
    }

    public java.lang.String getStripesEventName() {
        return stripesEventName;
    }

    public String getStripesActionBeanName() {
        return stripesActionBeanName;
    }

    public java.lang.String getEventSourceLink() {
        return eventSourceLink;
    }

    public List<String> getAllowedAccountStatuses() {
        return BaseUtils.getPropertyAsList("global.account.allowed.statuses.scp");
    }
    private long transactionRecordId;

    public void setTransactionRecordId(long transactionRecordId) {
        this.transactionRecordId = transactionRecordId;
    }

    public long getTransactionRecordId() {
        return transactionRecordId;
    }

    public String getSessionDetailLineGraph() {
        return sessionDetailLineGraph;
    }
    //SessionDetailLineGraph is used to draw the sessionDetailLineGraph
    String sessionDetailLineGraph = null;

    public Resolution retrieveSessionDetail() {

        AccountHistoryQuery q = new AccountHistoryQuery();
        q.setTransactionRecordId(transactionRecordId);
        q.setVerbosity(StAccountHistoryVerbosity.RECORDS_DETAIL);
        q.setResultLimit(1);
        setAccountHistory(SCAWrapper.getUserSpecificInstance().getAccountHistory(q));
        String sessionDetail = Utils.unzip(getAccountHistory().getTransactionRecords().get(0).getChargingDetail());
        sessionDetail = sessionDetail.replaceAll("\n", "\\\\n");
        //SessionDetailLineGraph is used to draw the sessionDetailLineGraph
        String[] cdrs = sessionDetail.split("\\\\n");
        StringBuilder graphSessionDetail = new StringBuilder();
        double accumulatedUnits = 0;
        for (String cdr : cdrs) {
            try {
                ChargingDetail chargingDetail = new ChargingDetail(cdr);
                accumulatedUnits += chargingDetail.getEventUnits();
                graphSessionDetail.append(chargingDetail.getChargingTimeStamp());
                graphSessionDetail.append(",");
                graphSessionDetail.append(Utils.round(accumulatedUnits / 1000000, 1)); // MB
                graphSessionDetail.append("\\n");
            } catch (Exception ex) {
                log.debug("Error adding charging detail record", ex);
            }
        }
        sessionDetailLineGraph = graphSessionDetail.toString();

        return getDDForwardResolution(
                "/account/view_charging_detail.jsp");
    }

    @DontValidate()
    public Resolution performTransfer() {
        try {
            checkCSRF();
            Account targetAcc;
            Account sourceAcc;

            if (!BaseUtils.getBooleanProperty("env.scp.me2u.enabled", true)) {
                if (!isAllowedToDoMe2u()) {
                    throw new InsufficientPrivilegesException("Me2U transfers is currently disabled");
                }
            }
            String transferToAllAccounts = (getParameter("me2uAllAccounts") == null ? "" : getParameter("me2uAllAccounts"));
            if (!transferToAllAccounts.isEmpty() && transferToAllAccounts.equals("Me2UAll")) {
                log.debug("This is a transfer to all accounts");
                return performMultipleTransfers();
            }
            if (getContext().getValidationErrors().hasFieldErrors()) {
                clearValidationErrors();
            }

            if (getBalanceTransferData().getTargetAccountId() < 1100000000 || getBalanceTransferData().getSourceAccountId() < 1100000000) {
                log.warn("Customere is attempting to use system account for me2u: source {}, target {}", getBalanceTransferData().getTargetAccountId(), getBalanceTransferData().getSourceAccountId());
                String sourceTarget = "source: " + getBalanceTransferData().getTargetAccountId() + " target: " + getBalanceTransferData().getTargetAccountId();
                throw new InsufficientPrivilegesException("System accounts cannot do Me2U transfers -- " + sourceTarget);
            }

            log.warn("This is a transfer to one account, going to remove additional lines");//Clear additional lines data
            getBalanceTransferData().getAdditionalTransferLines().clear();
            try {
                //Requesting access to the account that would be credited; We would use it to request the customer's name & surname
                targetAcc = SCAWrapper.getAdminInstance().getAccount(getBalanceTransferData().getTargetAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);

                sourceAcc = SCAWrapper.getUserSpecificInstance().getAccount(getBalanceTransferData().getSourceAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
                for (ServiceInstance si : sourceAcc.getServiceInstances()) {
                    if (si.getServiceSpecificationId() == 15 || si.getServiceSpecificationId() >= 1000) {
                        throw new InsufficientPrivilegesException("Special accounts cannot do Me2U transfers");
                    }
                }
            } catch (SCABusinessError sbe) {
                localiseErrorAndAddToGlobalErrors("scp.transfer.error.noaccount");
                return displayTransferPage();
            }
            int targetCustId = targetAcc.getServiceInstances().get(0).getCustomerId();
            int sourceCustId = sourceAcc.getServiceInstances().get(0).getCustomerId();
            
            //Requesting access to the customer whose account is being credited; we need their name & surname
            Customer targetCust = SCAWrapper.getAdminInstance().getCustomer(targetCustId, StCustomerLookupVerbosity.CUSTOMER);
            Customer sourceCust = UserSpecificCachedDataHelper.getCustomer(sourceCustId, StCustomerLookupVerbosity.CUSTOMER);
            String fullName = targetCust.getFirstName() + " " + targetCust.getLastName();
            String targetAccount = String.valueOf(targetAcc.getAccountId());
            String amount = SmileTags.convertCentsToCurrencyShort(getBalanceTransferData().getAmountInCents() * 100).trim();
            amount = amount.replaceAll("\\s+","");
            setCustomerWithProductsAndServices();
            setAccountListForCustomer(getCustomer(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
            
            boolean targetAccountBelongsToSameUser = false;
            for(int i=0; i<getAccountList().getAccounts().size(); i++) {                    
                 if(targetAcc.getAccountId()== getAccountList().getAccounts().get(i).getAccountId()) {                        
                     targetAccountBelongsToSameUser=true;
                     break;                        
                 }
            }
               
            if(BaseUtils.getBooleanProperty("env.m2u.transfer.restriction.own.accounts",false)) {                               
                if (getAccountList().getAccounts() == null || getAccountList().getAccounts().isEmpty()) {
                    setPageMessage("no.accounts.found");
                    return displayTransferPage();
                }
               
               if(!targetAccountBelongsToSameUser) {
                   
                    if(sourceCust.getCustomerRoles().size()==0 && targetCust.getCustomerRoles().size()==0) {   //Individual to Individual
                        
                        if(sourceCustId != targetCustId) {  //Can onlytransfer to self, do not allow if not same customerID
                           log.warn("Customer is attempting to transfer to another customer account for me2u: source customer {}, target customer {}", sourceCustId, targetCustId);
                           localiseErrorAndAddToGlobalErrors("scp.transfer.error.invalid.account");
                           return displayTransferPage();
                       }

                    } else if(sourceCust.getCustomerRoles().size()>0 && targetCust.getCustomerRoles().size()>0) { //organisation to Organisation                        
                        int srcOrganisationId=0;
                        int targetOrganisationId=0;
                        
                        for (ServiceInstance si : sourceAcc.getServiceInstances()) {
                            srcOrganisationId = UserSpecificCachedDataHelper.getProductInstance(si.getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN).getOrganisationId();                            
                            
                            if(srcOrganisationId>0)
                                break;
                        }
                        
                        for (ServiceInstance si : targetAcc.getServiceInstances()) {
                            targetOrganisationId = SCAWrapper.getAdminInstance().getProductInstance(si.getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN).getOrganisationId();                            
                            if(targetOrganisationId>0)
                                break;
                        } 
                        
                        if(srcOrganisationId != targetOrganisationId) {  //Only allow transfer to same organisation
                           log.warn("Customer is attempting to transfer to another organization account for me2u: source Org {}, target Org {}", srcOrganisationId, targetOrganisationId);
                           localiseErrorAndAddToGlobalErrors("scp.transfer.error.invalid.account");
                           return displayTransferPage();
                        }
                        
                    } else if ((sourceCust.getCustomerRoles().size()==0 && targetCust.getCustomerRoles().size()>0) ||
                            (sourceCust.getCustomerRoles().size()>0 && targetCust.getCustomerRoles().size()==0)) { //Individual to cust belonging to Organisation || ViceVersa
                        //Individual must belong to the organisation                        
                           log.warn("Individual Customer to organization customer Not Allowed");
                           localiseErrorAndAddToGlobalErrors("scp.transfer.error.invalid.account");
                           return displayTransferPage();                        
                    } else {
                            log.warn("Uncatered for transfer : source cust {}, target Org {}", sourceCust.getCustomerId(), targetCust.getCustomerId());
                            localiseErrorAndAddToGlobalErrors("scp.transfer.error.invalid.account");
                            return displayTransferPage();                           
                    }
                }

            } 
            HttpSession session = getContext().getRequest().getSession(); 
            if (notConfirmed()) {
                
                if(!targetAccountBelongsToSameUser && BaseUtils.getBooleanProperty("env.scp.otp.enabled", false)) {
                    
                    if(session.getAttribute("otpConfirmed")==null || session.getAttribute("otpConfirmed")!="true") {
                        session.removeAttribute("sessionOtp");
                        setOtp("");
                        return otpConfirm(getCustomer().getAlternativeContact1(), targetAccount, fullName, amount);
                    } else {
                        session.removeAttribute("otpConfirmed");
                        session.removeAttribute("sessionOtp");
                        setOtp("");
                    }
                    
                } else {
                        return confirm(targetAccount, fullName, amount);
                }
            }
            getBalanceTransferData().setSCAContext(null); // Safety precaution to prevent impersonation
            getBalanceTransferData().setAmountInCents(getBalanceTransferData().getAmountInCents() * 100);

            try {
                SCAWrapper.getUserSpecificInstance().transferBalance(getBalanceTransferData());
            } catch (SCAErr e) {
                if (e.getErrorCode().equalsIgnoreCase("BM-0001") || e.getErrorCode().equalsIgnoreCase("BM-0007")) {
                    localiseErrorAndAddToGlobalErrors("scp.transfer.error.insufficient.funds");
                } else if (e.getErrorCode().equalsIgnoreCase("BM-0002")) {
                    localiseErrorAndAddToGlobalErrors("scp.transfer.error.noaccount");
                } else if (e.getErrorCode().equalsIgnoreCase("BM-0003")) {
                    localiseErrorAndAddToGlobalErrors("scp.transfer.error.fields.msg");
                } else if (e.getErrorCode().equalsIgnoreCase("BM-0004")) {
                    localiseErrorAndAddToGlobalErrors("scp.transfer.error.negative.amount");
                } else {
                    localiseErrorAndAddToGlobalErrors("scp.transfer.error.msg");
                }
                return displayTransferPage();
            }
            AccountHistoryQuery ahq = new AccountHistoryQuery();

            ahq.setExtTxId(getBalanceTransferData().getSCAContext().getTxId());
            ahq.setResultLimit(10);
            // Use admin instance to prevent getting denied to see the transaction due to the other parties account not normally being allowed to be accessed
            setAccountHistory(SCAWrapper.getAdminInstance().getAccountHistory(ahq));

            //Construct page message parameters
            String targetEmail = targetCust.getEmailAddress();
            String sourceEmail = sourceCust.getEmailAddress();

            setPageMessage("scp.transfer.completed.succesfully", targetAccount, fullName, amount, targetEmail, sourceEmail);

            return displayTransferPage();
        } catch (SCABusinessError se) {
            log.error("Error while trying to perform a transfer - {}", se.getMessage());
            log.error(Utils.getStackTrace(se));

            addErrorToValidationErrors(se);
            return displayTransferPage();

        } catch (Exception ex) {
            log.error("Error while trying to perform a transfer - {}", ex.getMessage());
            log.error(Utils.getStackTrace(ex));

            if (ex instanceof SCAErr) {
                SCAErr err = (SCAErr) ex;
                embedErrorIntoStipesActionBeanErrorMechanism(err);
            } else if (ex instanceof InsufficientPrivilegesException) {
                InsufficientPrivilegesException err = (InsufficientPrivilegesException) ex;
                throw err;
            } else if (ex instanceof EntityAuthorisationException) {
                InsufficientPrivilegesException err = new InsufficientPrivilegesException(((EntityAuthorisationException) ex).getMessage());
                throw err;
            } else {
                localiseErrorAndAddToGlobalErrors("scp.transfer.error.msg.last");
            }
            return displayTransferPage();
        }
    }

    @DontValidate()
    public Resolution performMultipleTransfers() {
        log.debug("Entering performMultipleTransfers");

        if (getContext().getValidationErrors().hasFieldErrors()) {
            clearValidationErrors();
        }

        if (!BaseUtils.getBooleanProperty("env.scp.me2u.enabled", true)) {
            if (!isAllowedToDoMe2u()) {
                throw new InsufficientPrivilegesException("Me2U transfers is currently disabled");
            }
        }
        long sourceAcc = getBalanceTransferData().getSourceAccountId();
        BalanceTransferData btd = null;
        Set<Long> targetAccCollections = new HashSet<>();
        StringBuilder targetAccsToConfirm = new StringBuilder();
        double transferAmount = 0;

        if (sourceAcc < 1100000000) {
            log.warn("Customere is attempting to use system account for me2u: source {}", getBalanceTransferData().getSourceAccountId());
            String source = "source: " + sourceAcc;
            throw new InsufficientPrivilegesException("System accounts cannot do Me2U transfers -- " + source);
        }

        Account srcAcc = SCAWrapper.getUserSpecificInstance().getAccount(getBalanceTransferData().getSourceAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
        for (ServiceInstance si : srcAcc.getServiceInstances()) {
            if ((si.getServiceSpecificationId() == 15 && !BaseUtils.getBooleanProperty("env.scp.me2u.oncorporate.airtime.account.enabled", true)) || si.getServiceSpecificationId() >= 1000) {
                throw new InsufficientPrivilegesException("Special accounts cannot do Me2U transfers");
            }
        }

        if (getBalanceTransferData() != null && !getBalanceTransferData().getAdditionalTransferLines().isEmpty()) {
            for (BalanceTransferLine line : getBalanceTransferData().getAdditionalTransferLines()) {
                if (line.getTargetAccountId() == sourceAcc) {
                    continue;
                }
                if (line.getTargetAccountId() > 0 && line.getAmountInCents() > 0) {
                    if (btd == null) {
                        log.debug("performMultipleTransfers: creating new BalanceTransferData with no base transfer, only AdditionalTransferLines.");
                        btd = new BalanceTransferData();
                    }

                    if (line.getTargetAccountId() < 1100000000) {
                        log.warn("Customere is attempting to use system account for me2u: source {}", line.getTargetAccountId());
                        String targ = "target: " + line.getTargetAccountId();
                        throw new InsufficientPrivilegesException("System accounts cannot do Me2U transfers -- " + targ);
                    }

                    line.setAmountInCents(line.getAmountInCents() * 100);
                    line.setSourceAccountId(sourceAcc);
                    btd.getAdditionalTransferLines().add(line);

                    targetAccCollections.add(line.getTargetAccountId());
                    targetAccsToConfirm.append(line.getTargetAccountId()).append("#");
                    transferAmount += line.getAmountInCents();
                }
            }

            if (targetAccCollections.size() < 1) {
                localiseErrorAndAddToGlobalErrors("scp.transfer.error.noaccount");
                return displayTransferPage();
            }

            String amount = SmileTags.convertCentsToCurrencyShort(transferAmount);
            Account acc = UserSpecificCachedDataHelper.getAccount(sourceAcc);

            if (notConfirmed()) {
                if (acc.getAvailableBalanceInCents() <= (transferAmount * targetAccCollections.size())) {
                    String msg = LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), "scp.multiple.me2u.transfer.notenough.balance.msg");
                    return confirm(amount, String.valueOf(sourceAcc), targetAccsToConfirm.toString() + msg);
                } else {
                    return confirm(amount, String.valueOf(sourceAcc), targetAccsToConfirm.toString());
                }
            }

            StringBuilder errorCollections = new StringBuilder();
            boolean didATransfer = false;

            if (btd != null) {
                try {
                    SCAWrapper.getUserSpecificInstance().transferBalance(btd);
                    didATransfer = true;
                } catch (SCAErr e) {
                    if (e.getErrorCode().equalsIgnoreCase("BM-0001") || e.getErrorCode().equalsIgnoreCase("BM-0007")) {
                        String localErrorMessage = LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), "scp.transfer.error.insufficient.funds");
                        errorCollections.append(localErrorMessage);
                    } else if (e.getErrorCode().equalsIgnoreCase("BM-0003")) {
                        String localErrorMessage = LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), "scp.transfer.error.fields.msg");
                        errorCollections.append(localErrorMessage);
                    } else if (e.getErrorCode().equalsIgnoreCase("BM-0004")) {
                        String localErrorMessage = LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), "scp.transfer.error.negative.amount");
                        errorCollections.append(localErrorMessage);
                    } else {
                        //Type [business] Code [SCA-0008] Desc [Invalid transfer type. Transferring between those accounts is not permitted] Request [<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                        String localErrorMessage = LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), "scp.transfer.error.msg");
                        if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                            localErrorMessage = e.getMessage().substring(e.getMessage().indexOf("Desc"), e.getMessage().indexOf("Request [<?xml"));
                        }
                        errorCollections.append(localErrorMessage);
                    }
                } catch (InsufficientPrivilegesException ex) {
                    InsufficientPrivilegesException err = (InsufficientPrivilegesException) ex;
                    throw err;
                }
            }
            //No need to call account history
            if (!didATransfer) {
                log.debug("performMultipleTransfers: no transfer was done at all");
                setPageMessage("scp.multi.transfer.failed", sourceAcc, errorCollections);
                return displayTransferPage();
            }

            AccountHistoryQuery ahq = new AccountHistoryQuery();
            ahq.setAccountId(sourceAcc);
            Date futureDate = Utils.getFutureDate(Calendar.MINUTE, -2);//two minutes should be fine
            ahq.setDateFrom(Utils.getDateAsXMLGregorianCalendar(futureDate));
            ahq.setDateTo(Utils.getDateAsXMLGregorianCalendar(new Date()));
            ahq.setResultLimit(targetAccCollections.size() * 2);

            setAccountHistory(SCAWrapper.getUserSpecificInstance().getAccountHistory(ahq));

            //Construct page message parameters
            setPageMessage("scp.multi.transfer.completed.succesfully", amount, sourceAcc, targetAccsToConfirm.toString());

        }
        return displayTransferPage();
    }

    public Resolution displayTransferPage() {

        String custSSOID = getUser();
        if (custSSOID == null || custSSOID.isEmpty()) {
            localiseErrorAndAddToGlobalErrors("customer.ssoidentity.is.null");
            return getDDForwardResolution("/account/transfer.jsp");
        }

        setCustomerWithProductsAndServices();
        setAccountListForCustomer(getCustomer(), StAccountLookupVerbosity.ACCOUNT);
        if (getAccountList().getAccounts() == null || getAccountList().getAccounts().isEmpty()) {
            setPageMessage("no.accounts.found");
            return retrieveAllUserServicesAccounts();
        }
        // Default target account with the account id
        setAccount(getAccountList().getAccounts().get(0));
        setBalanceTransferData(new BalanceTransferData());
        getBalanceTransferData().setSourceAccountId(getAccount().getAccountId());

        if (!BaseUtils.getBooleanProperty("env.scp.me2u.enabled", true)) {
            if (!isAllowedToDoMe2u()) {
                throw new InsufficientPrivilegesException("Me2U transfers is currently disabled");
            }
        }
        return getDDForwardResolution("/account/transfer.jsp");
    }

    public Resolution getCustomerNameViaXMLHTTP() {

        String errorMsg = LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), "scp.account.xmlhttp.error.msg");
        try {
            //Requesting ACCOUNT details for the account to be credited during a balanceTransfer; we want access to their products
            setAccount(SCAWrapper.getAdminInstance().getAccount(getAccount().getAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES));

            //this account does not have any customer associated with it.
            if (getAccount().getServiceInstances().isEmpty()) {
                throw new Exception();
            }

            String prodSpecName = "";
            String jsonSpecObj;
            StringBuilder jsonSpecName = new StringBuilder();

            Set<Integer> prodIds = new HashSet();
            for (ServiceInstance si : getAccount().getServiceInstances()) {
                if (!isSIDeactivatedOrSIMCard(si)) {//dont add a service with status 'TD' to the list OR SIM Card service
                    prodIds.add(si.getProductInstanceId());
                }
            }

            //the services on this account seems to be all deactivated.
            if (prodIds.isEmpty()) {
                errorMsg = LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), "scp.account.xmlhttp.error.msg.deactivated.service");
                throw new Exception();
            }

            for (Integer pid : prodIds) {
                //Requesting access to the PRODUCT of the customer who's account is being credited with airtime
                ProductInstance pi = SCAWrapper.getAdminInstance().getProductInstance(pid, StProductInstanceLookupVerbosity.MAIN);
                //building: Smile High Speed Internet; Smile Voice & Video & Internet Access;
                if (pi.getFriendlyName().isEmpty()) {
                    ProductSpecification ps = NonUserSpecificCachedDataHelper.getProductSpecification(pi.getProductSpecificationId());
                    if (!prodSpecName.equalsIgnoreCase(ps.getName())) {
                        jsonSpecName.append(ps.getName());
                        jsonSpecName.append("; ");
                        prodSpecName = ps.getName();
                    }
                } else if (!prodSpecName.equalsIgnoreCase(pi.getFriendlyName())) {
                    jsonSpecName.append(pi.getFriendlyName());
                    jsonSpecName.append("; ");
                    prodSpecName = pi.getFriendlyName();
                }
            }

            //removing last two characters and adding double quotes"
            if (jsonSpecName.toString().endsWith("; ")) {
                int lastColon = jsonSpecName.toString().lastIndexOf(";");
                jsonSpecObj = jsonSpecName.toString().substring(0, lastColon);
                StringBuilder sb = new StringBuilder("(");
                sb.append(jsonSpecObj);
                sb.append(")");
                jsonSpecName = sb;
            }

            //getServiceInstanceUserName() is making use of Admin instance: Requesting access to the CUSTOMER; we want access to thier basic data e.g firstname
            String customerName = getServiceInstanceUserName(getAccount().getServiceInstances().get(0).getServiceInstanceId());
            //building: {"customerWithProduct":{"productInstanceUserName":"admin admin","productNames":"Corporate Sales"}}
            JsonObject ss = new JsonObject();
            ss.addProperty("productInstanceUserName", customerName);
            ss.addProperty("productNames", jsonSpecName.toString());
            JsonObject custJson = new JsonObject();
            custJson.add("customerWithProduct", ss);
            String json = SmileTags.getObjectAsJsonString(custJson);

            return new StreamingResolution("application/json", new StringReader(json));

        } catch (Exception ex) {
            JsonObject ss = new JsonObject();
            ss.addProperty("productInstanceUserName", errorMsg);
            JsonObject custJson = new JsonObject();
            custJson.add("customerWithProduct", ss);
            String json = SmileTags.getObjectAsJsonString(custJson);
            return new StreamingResolution("application/json", new StringReader(json));
        }
    }

    public static String getServiceInstanceUserName(int serviceInstanceId) {
        ServiceInstanceQuery siq = new ServiceInstanceQuery();
        siq.setServiceInstanceId(serviceInstanceId);
        siq.setVerbosity(StServiceInstanceLookupVerbosity.MAIN_SVCAVP);
        // Use admin instance
        ServiceInstance si = SCAWrapper.getAdminInstance().getServiceInstance(siq);
        CustomerQuery cq = new CustomerQuery();
        cq.setCustomerId(si.getCustomerId());
        cq.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        Customer cust = SCAWrapper.getAdminInstance().getCustomer(cq);
        return cust.getFirstName() + " " + cust.getLastName().substring(0, 1) + "...";
    }

    private boolean termsAndConditionsPending(int customerId) {
        boolean pendingTCs = false;
        setCustomer(UserSpecificCachedDataHelper.getCustomer(customerId, StCustomerLookupVerbosity.CUSTOMER));
        if (getCustomer().getOutstandingTermsAndConditions().size() > 0) {
            pendingTCs = true;
        }
        return pendingTCs;
    }
    private List<String> acceptedTCs;

    public List<String> getAcceptedTCs() {
        return acceptedTCs;
    }

    public void setAcceptedTCs(List<String> acceptedTCs) {
        this.acceptedTCs = acceptedTCs;
    }

    public Resolution acceptTermsAndConditions() {
        Customer clwCustomer = UserSpecificCachedDataHelper.getCustomer(getCustomer().getCustomerId(), StCustomerLookupVerbosity.CUSTOMER);
        List<String> originalTCs = clwCustomer.getOutstandingTermsAndConditions();
        if (getAcceptedTCs() == null) {
            return retrieveAllUserServicesAccounts();
        }
        try {
            for (String acceptedTC : getAcceptedTCs()) {
                if (clwCustomer.getOutstandingTermsAndConditions().contains(acceptedTC)) {
                    //TCs have been accepted 
                    clwCustomer.getOutstandingTermsAndConditions().remove(acceptedTC);
                }
            }
        } catch (Exception ex) {
            clwCustomer.getOutstandingTermsAndConditions().clear();
            clwCustomer.getOutstandingTermsAndConditions().addAll(originalTCs);
        }

        SCAWrapper.getUserSpecificInstance().modifyCustomer(clwCustomer);
        return retrieveAllUserServicesAccounts();
    }

    public Resolution backToUnitCredits() {
        return showAddUnitCredits();
    }

    private void doUCAndServicesValidations() {

        UnitCreditSpecificationQuery q = new UnitCreditSpecificationQuery();
        if (getPurchaseUnitCreditRequest().getUnitCreditSpecificationId() > 0) {
            q.setUnitCreditSpecificationId(getPurchaseUnitCreditRequest().getUnitCreditSpecificationId());
        } else {
            q.setItemNumber(getPurchaseUnitCreditRequest().getItemNumber());
        }
        q.setVerbosity(StUnitCreditSpecificationLookupVerbosity.MAIN_SVCSPECIDS);
        UnitCreditSpecification ucs = SCAWrapper.getUserSpecificInstance().getUnitCreditSpecifications(q).getUnitCreditSpecifications().get(0);

        String mySmileUCDiscount = Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "MySmileDiscountPercent");

        if (mySmileUCDiscount != null && mySmileUCDiscount.length() > 0) {

            double discountPercOff = Double.parseDouble(mySmileUCDiscount);
            // Apply the discount
            ucs.setPriceInCents(ucs.getPriceInCents() * (1 - discountPercOff / 100d));

        }

        setUnitCreditSpecification(ucs);
        if (!isAllowed(ucs.getPurchaseRoles())) {
            localiseErrorAndAddToGlobalErrors("unit.credit.not.allowed");
            SCABusinessError be = new SCABusinessError();
            be.setErrorDesc("Unit Credit not allowed for this role");
            be.setRequest("NA");
            be.setErrorCode("");
            throw be;
        }
        setServiceInstanceList(new ServiceInstanceList());
        setProductInstanceList(new ProductInstanceList());
        Set<Integer> activeProductInstances = new HashSet<>();

        setAccount(SCAWrapper.getUserSpecificInstance().getAccount(getPurchaseUnitCreditRequest().getAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES));

        for (ServiceInstance si : getAccount().getServiceInstances()) {
            ProductInstance pi = UserSpecificCachedDataHelper.getProductInstance(si.getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN);
            boolean canBeUsed = false;
            for (ProductServiceMapping psm : ucs.getProductServiceMappings()) {
                // Loop though UC's allowed service spec Ids
                log.debug("The UC can be used by service spec id [{}] and prod spec id [{}]", psm.getServiceSpecificationId(), psm.getProductSpecificationId());
                if (si.getServiceSpecificationId() == psm.getServiceSpecificationId()
                        && (psm.getProductSpecificationId() == 0 || psm.getProductSpecificationId() == pi.getProductSpecificationId())) {
                    // Found a SI that can use this UC
                    log.debug("UC can be used");
                    canBeUsed = true;
                    break;
                }
            }
            if (!canBeUsed) {
                continue;
            }
            si = UserSpecificCachedDataHelper.getServiceInstance(si.getServiceInstanceId(), StServiceInstanceLookupVerbosity.MAIN);

            if (!isSIDeactivatedOrSIMCard(si)) {
                getServiceInstanceList().getServiceInstances().add(si);
                activeProductInstances.add(si.getProductInstanceId());
                getPurchaseUnitCreditRequest().setProductInstanceId(si.getProductInstanceId());// For when there's one Browsing Service Instance
            }
        }

        setCustomerWithProductsAndServices();
        setAccountListAndRemoveSpecialAndInactiveAccounts(getCustomer());
        if (getServiceInstanceList().getServiceInstances().isEmpty()) {
            localiseErrorAndAddToGlobalErrors("no.valid.services.for.unit.credit");
            SCABusinessError be = new SCABusinessError();
            be.setErrorDesc("No valid services for unit credit");
            be.setRequest("NA");
            be.setErrorCode("");
            throw be;
        }
        for (Integer id : activeProductInstances) {
            getProductInstanceList().getProductInstances().add(UserSpecificCachedDataHelper.getProductInstance(id, StProductInstanceLookupVerbosity.MAIN));
        }

    }

    public Resolution showUnitCreditPaymentMethodPage() {
        doUCAndServicesValidations();
        if (getPurchaseUnitCreditLine() != null) {//if customer wants upsize
            log.debug("Customer wants upsize bundle: {}", getPurchaseUnitCreditLine().getUnitCreditSpecificationId());
        }
        return getDDForwardResolution("/products/unit_credit_payment_method.jsp");
    }

    public Resolution provisionUnitCreditPaymentMethodPage() {

        doUCAndServicesValidations();

        if (Utils.getBooleanValueFromCRDelimitedAVPString(getUnitCreditSpecification().getConfiguration(), "CampaignsOnly")) {
            log.debug("Checking if PI matches chosen CampaignsOnly UC");
            if (!isCampaignsOnlyUCInProductInstancesUCCampaignList(getPurchaseUnitCreditRequest().getProductInstanceId(), getPurchaseUnitCreditRequest().getUnitCreditSpecificationId())) {
                localiseErrorAndAddToGlobalErrors("scp.campainsonly.unit.credit.notpart.ofsim");
                return showAddUnitCredits();
            }
        }

        if (UCHasExtraUpsizeConfig()) {
            log.debug("Going to do validations for to all Upsize unit credit specs under UCS {}", getUnitCreditSpecification().getUnitCreditSpecificationId());
            String upSizeUCSpecIds = Utils.getValueFromCRDelimitedAVPString(getUnitCreditSpecification().getConfiguration(), "ExtraUpsizeSpecIds");
            String[] upSizeIDs = upSizeUCSpecIds.split(",");            

            for (String upsizeUCSpecID : upSizeIDs) {

                UnitCreditSpecificationQuery ucsq = new UnitCreditSpecificationQuery();
                ucsq.setUnitCreditSpecificationId(Integer.valueOf(upsizeUCSpecID));
                ucsq.setVerbosity(StUnitCreditSpecificationLookupVerbosity.MAIN_SVCSPECIDS);
                UnitCreditSpecification upsizeUCSpec = SCAWrapper.getUserSpecificInstance().getUnitCreditSpecifications(ucsq).getUnitCreditSpecifications().get(0);
                
                if(upsizeUCSpec.getName().toLowerCase().contains("booster".toLowerCase()) && !BaseUtils.getBooleanProperty("env.allow.booster.combine.at.buy", false)) {                    
                    continue;
                }
                
                String mySmileUpsizeUCDiscount = Utils.getValueFromCRDelimitedAVPString(upsizeUCSpec.getConfiguration(), "MySmileDiscountPercent");
                
                if (mySmileUpsizeUCDiscount != null && mySmileUpsizeUCDiscount.length() > 0) {
                    double upsizeDiscountPercOff = Double.parseDouble(mySmileUpsizeUCDiscount);
                    upsizeUCSpec.setPriceInCents(upsizeUCSpec.getPriceInCents() * (1 - upsizeDiscountPercOff / 100d));
                }

                if (!isAllowed(upsizeUCSpec.getPurchaseRoles())) {
                    localiseErrorAndAddToGlobalErrors("unit.credit.not.allowed");
                    return showAddUnitCredits();
                }

                PurchaseUnitCreditLine pUnitCreditLine = new PurchaseUnitCreditLine();
                pUnitCreditLine.setUnitCreditSpecification(upsizeUCSpec);
                pUnitCreditLine.setNumberToPurchase(1);

                getPurchaseUnitCreditLines().add(pUnitCreditLine);
            }

            if (getPurchaseUnitCreditLines().isEmpty()) {
                return showUnitCreditPaymentMethodPage();
            }
            return getDDForwardResolution("/products/upsize_selection.jsp");
        }

        return getDDForwardResolution("/products/unit_credit_payment_method.jsp");
    }

    public Resolution previousPaymentMethodPage() {
        return provisionUnitCreditPaymentMethodPage();
    }

    public Resolution nextPaymentMethodPage() {
        return provisionUnitCreditPaymentMethodPage();
    }

    public Resolution provisionUnitCreditViaPaymentGateway() {
        log.debug("Entering provisionUnitCreditViaPaymentGateway");
        try {

            setCustomer(UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER));
            if (!isCustomerAllowedToMakePayment(getCustomer())) {
                log.warn("Payment gateway functionality is restricted. Requesting customer is {}", getCustomer().getSSOIdentity());
                return getDDForwardResolution("/permissions/gateway_transaction_denied.jsp");
            }

            UnitCreditSpecificationQuery q = new UnitCreditSpecificationQuery();
            if (getPurchaseUnitCreditRequest().getUnitCreditSpecificationId() > 0) {
                q.setUnitCreditSpecificationId(getPurchaseUnitCreditRequest().getUnitCreditSpecificationId());
            }
            if (getPurchaseUnitCreditRequest().getItemNumber() != null && !getPurchaseUnitCreditRequest().getItemNumber().isEmpty()) {
                q.setItemNumber(getPurchaseUnitCreditRequest().getItemNumber());
            }

            q.setVerbosity(StUnitCreditSpecificationLookupVerbosity.MAIN_SVCSPECIDS);
            UnitCreditSpecification ucs = SCAWrapper.getUserSpecificInstance().getUnitCreditSpecifications(q).getUnitCreditSpecifications().get(0);

            String mySmileUCDiscount = Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "MySmileDiscountPercent");

            if (mySmileUCDiscount != null && mySmileUCDiscount.length() > 0) {
                double discountPercOff = Double.parseDouble(mySmileUCDiscount);
                // Apply the discount
                ucs.setPriceInCents(ucs.getPriceInCents() * (1 - discountPercOff / 100d));

            }

            String gatewayCode = getParameter("gatewayCode");

            if (!gatewayExist(gatewayCode)) {
                //Its probably a Third Partner
                return provisionUnitCreditViaTPGWPartner();
            }
            log.debug("Going to use SCA integration gateway for unit credit provisioning");

            setUnitCreditSpecification(ucs);
            String recPhoneNumber = getSale().getRecipientPhoneNumber() == null ? 
                    getCustomer().getAlternativeContact1() : getSale().getRecipientPhoneNumber();
            getSale().setSaleTotalCentsIncl(ucs.getPriceInCents());
            getSale().setRecipientCustomerId(getUserCustomerIdFromSession());
            getSale().setPaymentMethod(PAYMENT_METHOD_PAYMENT_GATEWAY);
            getSale().setRecipientPhoneNumber(getCustomer().getAlternativeContact1());
            getSale().setExtraInfo(getCustomer().getEmailAddress());
            if (getPurchaseUnitCreditRequest().getProductInstanceId() > 0) {
                int organisationId = UserSpecificCachedDataHelper.getProductInstance(getPurchaseUnitCreditRequest().getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN).getOrganisationId();
                getSale().setRecipientOrganisationId(organisationId);
            }
            getSale().setSaleLocation("");
            getSale().setRecipientName(getCustomer().getFirstName());
            
            if(gatewayCode.toLowerCase().equalsIgnoreCase("yopayments")) {
                    String channel = BaseUtils.getSubProperty("env.pgw." + gatewayCode.toLowerCase() + ".config", "MySmileX3MTNChannel");
                    String mobileMoneyProcessor = (getParameter("mobileMoneyProcessor") == null ? "" : getParameter("mobileMoneyProcessor"));
                    
                    if(mobileMoneyProcessor.equalsIgnoreCase("MTN")) {
                        channel = BaseUtils.getSubProperty("env.pgw." + gatewayCode.toLowerCase() + ".config", "MySmileX3MTNChannel");
                    } else if(mobileMoneyProcessor.equalsIgnoreCase("AIRTEL")) {
                        channel = BaseUtils.getSubProperty("env.pgw." + gatewayCode.toLowerCase() + ".config", "MySmileX3AirtelChannel");
                    } else {
                        log.warn("Payment gateway MobileProcessor Invalid {}", mobileMoneyProcessor);
                        return getDDForwardResolution("/permissions/gateway_transaction_denied.jsp");
                    }
                    
                    getSale().setChannel(channel);
                } else {
                    getSale().setChannel(BaseUtils.getSubProperty("env.pgw." + gatewayCode.toLowerCase() + ".config", "MySmileX3Channel"));
                } 
                
                
            getSale().setWarehouseId("");
            getSale().setPromotionCode("");
            getSale().setTaxExempt(false);
            getSale().setPaymentGatewayCode(gatewayCode);
            getSale().setLandingURL("/PaymentGateway.action?processBankTransaction");

            if (getSale().getSaleLines() != null && !getSale().getSaleLines().isEmpty()) {
                getSale().getSaleLines().clear();//cleanup   
            }

            SaleLine line = new SaleLine();
            line.setLineNumber(1);
            line.setQuantity(1);
            line.setLineTotalCentsIncl(ucs.getPriceInCents());

            InventoryItem ii = new InventoryItem();
            ii.setItemNumber(ucs.getItemNumber());
            ii.setSerialNumber("");
            line.setInventoryItem(ii);
            getSale().getSaleLines().add(line);

            if (UCHasExtraUpsizeConfig() && (getPurchaseUnitCreditLine() != null && getPurchaseUnitCreditLine().getUnitCreditSpecificationId() > 0)) {

                q = new UnitCreditSpecificationQuery();
                q.setUnitCreditSpecificationId(getPurchaseUnitCreditLine().getUnitCreditSpecificationId());
                q.setVerbosity(StUnitCreditSpecificationLookupVerbosity.MAIN_SVCSPECIDS);

                UnitCreditSpecification upsizeUCSpec = SCAWrapper.getUserSpecificInstance().getUnitCreditSpecifications(q).getUnitCreditSpecifications().get(0);

                String upsizeMySmileUCDiscount = Utils.getValueFromCRDelimitedAVPString(upsizeUCSpec.getConfiguration(), "MySmileDiscountPercent");

                if (upsizeMySmileUCDiscount != null && upsizeMySmileUCDiscount.length() > 0) {
                    double discountPercOff = Double.parseDouble(upsizeMySmileUCDiscount);
                    upsizeUCSpec.setPriceInCents(upsizeUCSpec.getPriceInCents() * (1 - discountPercOff / 100d));
                }

                line = new SaleLine();
                line.setLineNumber(2);
                line.setQuantity(1);
                line.setLineTotalCentsIncl(upsizeUCSpec.getPriceInCents());

                ii = new InventoryItem();
                ii.setItemNumber(upsizeUCSpec.getItemNumber());
                ii.setSerialNumber("");
                line.setInventoryItem(ii);
                getSale().getSaleLines().add(line);

                getSale().setSaleTotalCentsIncl(getSale().getSaleTotalCentsIncl() + upsizeUCSpec.getPriceInCents());
            }
            
            Sale saleData = SCAWrapper.getUserSpecificInstance().generateQuote(getSale());

            saleData.setPaymentGatewayCode(gatewayCode);
            long accId = Long.parseLong(BaseUtils.getSubProperty("env.pgw." + gatewayCode.toLowerCase() + ".config", "AccountId"));
            saleData.setSalesPersonAccountId(accId);
            saleData.setSalesPersonCustomerId(1); // Make sale as admin
            saleData.setPurchaseOrderData("");
            if(gatewayCode.equalsIgnoreCase("Access")) {
                saleData.setExtraInfo(getCustomer().getEmailAddress());
            } else {
                saleData.setExtraInfo("");
            }
            saleData.setPaymentMethod(PAYMENT_METHOD_PAYMENT_GATEWAY);

            Sale processedSale = SCAWrapper.getUserSpecificInstance().processSale(saleData);

            PaymentGatewayManager pgm = null;
            for (GatewayCodes gc : GatewayCodes.values()) {
                if (gc.getGatewayCode().equals(gatewayCode)) {
                    pgm = PaymentGatewayManagerFactory.createPaymentGatewayManager(gc);
                    log.debug("Gateway code [{}], uses class [{}]", gatewayCode, pgm.getClass().getCanonicalName());
                    break;
                }
            }

            IPGWTransactionData initialisedTransaction;
            if(recPhoneNumber != null && recPhoneNumber.startsWith("+")){
                recPhoneNumber = recPhoneNumber.substring(1, recPhoneNumber.length());
                log.debug("received recipient phone number :"+recPhoneNumber);
            }
            try {
                processedSale.setRecipientPhoneNumber(recPhoneNumber);
                initialisedTransaction = pgm.startTransaction(processedSale, ucs.getName());
                if(initialisedTransaction.getPaymentGatewayPostURL() == null || initialisedTransaction.getPaymentGatewayPostURL().isEmpty()){
                    setPageMessage("scp.payment.gateway.paid.success");
                    return retrieveAllUserServicesAccounts();
                }
            } catch (Exception ex) {
                localiseErrorAndAddToGlobalErrors("scp.payment.gateway.manager.initialisation.failure");
                return retrieveAllUserServicesAccounts();
            }
            setPGWTransactionData(initialisedTransaction);

        } catch (SCABusinessError ex) {
            embedErrorIntoStipesActionBeanErrorMechanism(ex);
            return retrieveAllUserServicesAccounts();
        } catch (Exception e) {
            log.warn("Error occured trying to instantiate buy bundle via payment gateway");
            log.warn("Error: ", e);
            localiseErrorAndAddToGlobalErrors("unit.credit.via.gateway.payment.error");
            return retrieveAllUserServicesAccounts();
        }

        return getDDForwardResolution("/payment_gateway/confirm_payment_gateway_transaction.jsp");
    }

    private String TPGWPartnerCode;

    public String getTPGWPartnerCode() {
        return TPGWPartnerCode;
    }

    public Resolution provisionUnitCreditViaTPGWPartner() {
        log.debug("Entering provisionUnitCreditViaTPGWPartner");
        //Not implemented yet
        return getDDForwardResolution("/payment_gateway/confirm_tpgw_partner_transaction.jsp");
    }

    public Resolution confirmAirtimeTransaction() {

        try {

            setCustomerWithProductsAndServices();
            setAccountListAndRemoveSpecialAndInactiveAccounts(getCustomer());
            if (!isCustomerAllowedToMakePayment(getCustomer())) {
                log.warn("Payment gateway functionality is restricted. Requesting customer is {}", getCustomer().getSSOIdentity());
                return getDDForwardResolution("/permissions/gateway_transaction_denied.jsp");
            }

            if (getSale() != null) {

                double totalAmount = 0.0d;
                double transactionAmount = getSale().getSaleTotalCentsIncl() * 100d;
                if (getSale().getSaleLines() != null && !getSale().getSaleLines().isEmpty()) {
                    List<SaleLine> tmpSaleLines = new ArrayList<>();
                    tmpSaleLines.addAll(getSale().getSaleLines());
                    getSale().getSaleLines().clear();//reset

                    int ln = 0;
                    for (SaleLine line : tmpSaleLines) {
                        if (line.getLineTotalCentsIncl() >= 25d) {//Minimum allowed amount (major unit) per transaction set by Diamond Bank
                            line.setLineNumber(ln);
                            line.setLineTotalCentsIncl(line.getLineTotalCentsIncl() * 100);
                            line.getInventoryItem().setItemNumber("AIR1004");
                            line.getInventoryItem().setSerialNumber("AIRTIME");
                            Double quantity = line.getLineTotalCentsIncl();
                            line.setQuantity(quantity.longValue() / 100);
                            getSale().getSaleLines().add(line);
                            totalAmount += line.getLineTotalCentsIncl();
                            ln++;
                        }
                    }
                    if (getSale().getSaleLines().isEmpty()) {
                        return getDDForwardResolution(AccountActionBean.class, "showRechargePaymentOptions", "amount.to.topup.not.specified");
                    }
                }
                if (totalAmount != transactionAmount) {
                    transactionAmount = totalAmount;
                }

                setAccount(UserSpecificCachedDataHelper.getAccount(getSale().getRecipientAccountId(), StAccountLookupVerbosity.ACCOUNT));
                getSale().setSaleTotalCentsIncl(transactionAmount);

            } else {
                return getDDForwardResolution(AccountActionBean.class, "showRechargePaymentOptions");
            }

        } catch (Exception e) {
            return getDDForwardResolution(AccountActionBean.class, "showRechargePaymentOptions", "scp.recharge.accounts.confirmation.error");
        }
        return getDDForwardResolution("/products/unit_credit_payment_method.jsp");
    }

    public Resolution buyAirtimeViaPaymentGateway() {
        log.debug("Entering: buyAirtimeViaPaymentGateway");
        try {
            setCustomer(UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER));

            if (!isCustomerAllowedToMakePayment(getCustomer())) {
                log.warn("Payment gateway functionality is restricted. Requesting customer is {}", getCustomer().getSSOIdentity());
                return getDDForwardResolution("/permissions/gateway_transaction_denied.jsp");
            }
            String recPhoneNumber = getSale().getRecipientPhoneNumber() == null ? 
                    getCustomer().getAlternativeContact1() : getSale().getRecipientPhoneNumber();
            if (getSale() != null) {
                double totalAmount = 0.0d;
                double transactionAmount = getSale().getSaleTotalCentsIncl() * 100d;
                String gatewayCode = getSale().getPaymentGatewayCode();

                if (!gatewayExist(gatewayCode)) {
                    //Its probably a payment via Third Partner
                    return provisionUnitCreditViaTPGWPartner();
                }

                if (getSale().getSaleLines() != null && !getSale().getSaleLines().isEmpty()) {

                    List<SaleLine> tmpSaleLines = new ArrayList<>();
                    tmpSaleLines.addAll(getSale().getSaleLines());
                    getSale().getSaleLines().clear();//reset
                    int cnt = 0;
                    for (SaleLine line : tmpSaleLines) {
                        if (line.getLineTotalCentsIncl() >= 25d) {//Minimum allowed amount (major unit) per transaction set by Diamond Bank
                            line.setLineNumber(cnt);
                            line.setLineTotalCentsIncl(line.getLineTotalCentsIncl() * 100);
                            line.getInventoryItem().setItemNumber("AIR1004");
                            line.getInventoryItem().setSerialNumber("AIRTIME");
                            line.getInventoryItem().setDescription("Smile Airtime purchased through Clearing Bureau");
                            Double quantity = line.getLineTotalCentsIncl();
                            line.setQuantity(quantity.longValue() / 100);
                            totalAmount += line.getLineTotalCentsIncl();
                            getSale().getSaleLines().add(line);
                            cnt++;
                        }
                    }

                    if (getSale().getSaleLines().isEmpty()) {
                        return getDDForwardResolution(AccountActionBean.class, "showRechargePaymentOptions", "account.to.topup.not.specified");
                    }
                    if (getSale().getSaleLines().size() > 1) {
                        return getDDForwardResolution(AccountActionBean.class, "showRechargePaymentOptions", "scp.multiple.recharge.not.supported");
                    }
                }

                setAccount(UserSpecificCachedDataHelper.getAccount(getSale().getRecipientAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES));
                if (totalAmount != transactionAmount) {
                    transactionAmount = totalAmount;
                }
                for (ServiceInstance si : getAccount().getServiceInstances()) {
                    int organisationId = UserSpecificCachedDataHelper.getProductInstance(si.getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN).getOrganisationId();
                    if (organisationId > 0) {
                        getSale().setRecipientOrganisationId(organisationId);
                        break;
                    }
                }
                
                getSale().setSaleTotalCentsIncl(transactionAmount);
                getSale().setRecipientCustomerId(getUserCustomerIdFromSession());
                getSale().setPaymentMethod(PAYMENT_METHOD_PAYMENT_GATEWAY);
                getSale().setRecipientPhoneNumber(getCustomer().getAlternativeContact1());
                getSale().setSaleLocation("");
                getSale().setRecipientName(getCustomer().getFirstName());                
                
                if(gatewayCode.toLowerCase().equalsIgnoreCase("yopayments")) {
                    String channel = BaseUtils.getSubProperty("env.pgw." + gatewayCode.toLowerCase() + ".config", "MySmileX3MTNChannel");
                    String mobileMoneyProcessor = (getParameter("mobileMoneyProcessor") == null ? "" : getParameter("mobileMoneyProcessor"));
                    
                    if(mobileMoneyProcessor.equalsIgnoreCase("MTN")) {
                        channel = BaseUtils.getSubProperty("env.pgw." + gatewayCode.toLowerCase() + ".config", "MySmileX3MTNChannel");
                    } else if(mobileMoneyProcessor.equalsIgnoreCase("AIRTEL")) {
                        channel = BaseUtils.getSubProperty("env.pgw." + gatewayCode.toLowerCase() + ".config", "MySmileX3AirtelChannel");
                    } else {
                        log.warn("Payment gateway MobileProcessor Invalid {}", mobileMoneyProcessor);
                        return getDDForwardResolution("/permissions/gateway_transaction_denied.jsp");
                    }
                    
                    getSale().setChannel(channel);
                } else {
                    getSale().setChannel(BaseUtils.getSubProperty("env.pgw." + gatewayCode.toLowerCase() + ".config", "MySmileX3Channel"));
                } 
                
                getSale().setWarehouseId("");
                getSale().setPromotionCode("");
                getSale().setTaxExempt(false);
                getSale().setPaymentGatewayCode(gatewayCode);
                getSale().setLandingURL("/PaymentGateway.action?processBankTransaction");
                            
                Sale saleData = SCAWrapper.getUserSpecificInstance().generateQuote(getSale());

                saleData.setPaymentGatewayCode(gatewayCode);
                long accId = Long.parseLong(BaseUtils.getSubProperty("env.pgw." + gatewayCode.toLowerCase() + ".config", "AccountId"));
                saleData.setSalesPersonAccountId(accId);
                saleData.setSalesPersonCustomerId(1);
                saleData.setPurchaseOrderData("");
                if(gatewayCode.equalsIgnoreCase("Access")) {
                    saleData.setExtraInfo(getCustomer().getEmailAddress());
                } else {
                    saleData.setExtraInfo("");
                }
                saleData.setPaymentMethod(PAYMENT_METHOD_PAYMENT_GATEWAY);                

                Sale processedSale = SCAWrapper.getUserSpecificInstance().processSale(saleData);

                PaymentGatewayManager pgm = null;
                for (GatewayCodes gc : GatewayCodes.values()) {
                    if (gc.getGatewayCode().equals(gatewayCode)) {
                        pgm = PaymentGatewayManagerFactory.createPaymentGatewayManager(gc);
                        log.debug("Gateway code [{}], uses class [{}]", gatewayCode, pgm.getClass().getCanonicalName());
                        break;
                    }
                }

                IPGWTransactionData initialisedTransaction;
                if(recPhoneNumber != null && recPhoneNumber.startsWith("+")){
                    recPhoneNumber = recPhoneNumber.substring(1, recPhoneNumber.length());
                    log.debug("received recipient phone number :"+recPhoneNumber);
                }
                try {
                    processedSale.setRecipientPhoneNumber(recPhoneNumber);
                    initialisedTransaction = pgm.startTransaction(processedSale, "AIRTIME");
                    if(initialisedTransaction.getPaymentGatewayPostURL() == null || initialisedTransaction.getPaymentGatewayPostURL().isEmpty()){
                        setPageMessage("scp.payment.gateway.paid.success");
                        return retrieveAllUserServicesAccounts();
                    }
                } catch (Exception ex) {
                    log.warn("Error: ", ex);
                    localiseErrorAndAddToGlobalErrors("scp.payment.gateway.manager.initialisation.failure");
                    return retrieveAllUserServicesAccounts();
                }

                setPGWTransactionData(initialisedTransaction);

            } else {
                localiseErrorAndAddToGlobalErrors("scp.recharge.accounts.confirmation.error");
                return retrieveAllUserServicesAccounts();
            }

        } catch (SCABusinessError ex) {
            embedErrorIntoStipesActionBeanErrorMechanism(ex);
            return retrieveAllUserServicesAccounts();
        } catch (Exception e) {
            log.warn("Error occured trying to instantiate buy airtime via payment gateway: ", e);
            localiseErrorAndAddToGlobalErrors("scp.recharge.accounts.confirmation.error");
            return retrieveAllUserServicesAccounts();
        }
        return getDDForwardResolution("/payment_gateway/confirm_payment_gateway_transaction.jsp");
    }

    public Resolution previousAddUnitCreditMultipleAccountsPage() {
        return showAddUnitCreditMultipleAccounts();
    }

    public Resolution nextAddUnitCreditMultipleAccountsPage() {
        return showAddUnitCreditMultipleAccounts();
    }

    public Resolution showAddUnitCreditMultipleAccounts() {
        log.debug("Entering showAddUnitCreditMultipleAccounts");
        setCustomerWithProductsAndServices();
        setAccountListForCustomer(getCustomer(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
        Set<Long> specialAccountIdsExcludedSet = new HashSet<>();
        log.debug("Done setting customer and their accounts");
        if (getAccountList().getAccounts() != null && !getAccountList().getAccounts().isEmpty()) {

            populateProductInstanceListAndServiceInstanceList(getCustomer(), StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES);

            ServiceInstanceList tmpSIList = new ServiceInstanceList();
            ProductInstanceList tmpPIList = new ProductInstanceList();
            AccountList specialAccountsExcludedAccountList = new AccountList();

            Set<Integer> productInstanceIdSet = new HashSet<>();

            for (ServiceInstance si : getServiceInstanceList().getServiceInstances()) {
                //skip accounts with special services
                if (checkIfStaffOrPartnerService(si.getServiceSpecificationId())) {
                    continue;
                }
                tmpSIList.getServiceInstances().add(si);
                productInstanceIdSet.add(si.getProductInstanceId());
                specialAccountIdsExcludedSet.add(si.getAccountId());
            }
            for (Integer si : productInstanceIdSet) {
                for (ProductInstance pi : getProductInstanceList().getProductInstances()) {
                    if (pi.getProductInstanceId() == si) {
                        tmpPIList.getProductInstances().add(pi);
                    }
                }
            }
            for (Long accId : specialAccountIdsExcludedSet) {
                for (Account ac : getAccountList().getAccounts()) {
                    if (ac.getAccountId() == accId) {
                        specialAccountsExcludedAccountList.getAccounts().add(ac);
                    }
                }
            }

            if (getAccountQuery() != null && getAccountQuery().getAccountId() > 0) {

                ServiceInstanceList accSI = new ServiceInstanceList();
                ProductInstanceList accPI = new ProductInstanceList();
                AccountList accList = new AccountList();
                Set<Integer> accPIIdSet = new HashSet<>();

                for (ServiceInstance si : tmpSIList.getServiceInstances()) {
                    if (si.getAccountId() == getAccountQuery().getAccountId()) {
                        accSI.getServiceInstances().add(si);
                        accPIIdSet.add(si.getProductInstanceId());
                    }
                }
                for (Integer prodInstanceId : accPIIdSet) {
                    for (ProductInstance pi : tmpPIList.getProductInstances()) {
                        if (pi.getProductInstanceId() == prodInstanceId) {
                            accPI.getProductInstances().add(pi);
                        }
                    }
                }
                for (Account ac : specialAccountsExcludedAccountList.getAccounts()) {
                    if (ac.getAccountId() == getAccountQuery().getAccountId()) {
                        accList.getAccounts().add(ac);
                    }
                }
                tmpSIList = accSI;
                tmpPIList = accPI;
                specialAccountsExcludedAccountList = accList;
            }

            //Reset
            getServiceInstanceList().getServiceInstances().clear();
            getProductInstanceList().getProductInstances().clear();
            getAccountList().getAccounts().clear();

            //Re-initialise
            getProductInstanceList().getProductInstances().addAll(tmpPIList.getProductInstances());
            getProductInstanceList().setNumberOfProductInstances(tmpPIList.getProductInstances().size());

            getAccountList().getAccounts().addAll(specialAccountsExcludedAccountList.getAccounts());
            getAccountList().setNumberOfAccounts(specialAccountsExcludedAccountList.getAccounts().size());

            getServiceInstanceList().getServiceInstances().addAll(tmpSIList.getServiceInstances());
            getServiceInstanceList().setNumberOfServiceInstances(tmpSIList.getServiceInstances().size());
        }

        if (getAccountList().getAccounts() == null || getAccountList().getAccounts().isEmpty()) {
            setPageMessage("scp.recharge.acc.hasno.ucimapping");
            return retrieveAllUserServicesAccounts();
        }
        log.debug("AccountList is not empty");
        if (getAccountList().getAccounts().size() == 1) {
            //Can happen if customer has a Smile sales service(direct, tpgw, partner, cb) and a Smile internet/voice/sms service.
            //Since a Smile sales service cannot be used for customer recharge we dont provide is as an option to choose for recharge
            log.debug("The is only one account");
            setAccount(getAccountList().getAccounts().get(0));
            return showAddUnitCredits();
        }

        for (Account acc : getAccountList().getAccounts()) {
            setUniqueSIMsForAccount(acc);
        }

        return getDDForwardResolution("/products/add_unit_credit_select_account_sim.jsp");
    }

    public Resolution invokeIframe() {
        getContext().getRequest().getSession().setAttribute("gatewayPostURL", getParameter("gatewayPostURL"));
        return getDDForwardResolution("/payment_gateway/payment_gateway_iframe.jsp");
    }

    public Resolution invokeAutoRedirect() {
        return new RedirectResolution(getParameter("gatewayPostURL"), false);
    }

    private TimeRange timeRange;
    private BreakdownDataset breakdownDataset;
    private TrendOverTimeDataset trendOverTimeDataset;

    public void setTimeRange(TimeRange timeRange) {
        this.timeRange = timeRange;
    }

    public TimeRange getTimeRange() {
        return timeRange;
    }

    public void setBreakdownDataset(BreakdownDataset breakdownDataset) {
        this.breakdownDataset = breakdownDataset;
    }

    public BreakdownDataset getBreakdownDataset() {
        return breakdownDataset;
    }

    public void setTrendOverTimeDataset(TrendOverTimeDataset trendOverTimeDataset) {
        this.trendOverTimeDataset = trendOverTimeDataset;
    }

    public TrendOverTimeDataset getTrendOverTimeDataset() {
        return trendOverTimeDataset;
    }

    public Resolution retrieveTNFData() {

        Set<Integer> prodIds = new HashSet();

        if (getProductInstance() != null) {
            prodIds.add(getProductInstance().getProductInstanceId());
        } else {
            setAccount(UserSpecificCachedDataHelper.getAccount(getAccountQuery().getAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES));
            for (ServiceInstance si : getAccount().getServiceInstances()) {
                if (!isSIDeactivatedOrSIMCard(si)) {
                    prodIds.add(si.getProductInstanceId());
                }
            }
        }

        //the services on this account seems to be all deactivated.
        if (prodIds.isEmpty()) {
            log.debug("The services in this account/product seems to be all deactivated");
            return showTransactionHistory();
        }

        if (prodIds.size() > 1) {
            log.debug("The account has more than one product intances");
            setProductInstanceList(new ProductInstanceList());
            for (int piId : prodIds) {
                getProductInstanceList().getProductInstances().add(UserSpecificCachedDataHelper.getProductInstance(piId, StProductInstanceLookupVerbosity.MAIN));
            }
            return getDDForwardResolution("/products/select_product_instance_for_tnf.jsp");
        }

        int productInstanceId = 0;
        for (int piId : prodIds) {
            productInstanceId = piId;
        }

        ProductInstance pi = UserSpecificCachedDataHelper.getProductInstance(productInstanceId, StProductInstanceLookupVerbosity.MAIN);

        Date searchMonthFromGCtoJava = Utils.getJavaDate(getSearchMonth());
        XMLGregorianCalendar xmlTo = Utils.getDateAsXMLGregorianCalendar(searchMonthFromGCtoJava);
        XMLGregorianCalendar xmlFrom = Utils.getDateAsXMLGregorianCalendar(searchMonthFromGCtoJava);

        TNFQuery tnfQuery = new TNFQuery();
        tnfQuery.setTNFMethod("Complex");
        tnfQuery.setTimeRange(new TimeRange());
        tnfQuery.getTimeRange().setStartTime(xmlFrom);
        tnfQuery.getTimeRange().setEndTime(xmlTo);

        Calendar now = tnfQuery.getTimeRange().getEndTime().toGregorianCalendar();
        tnfQuery.getTimeRange().getEndTime().setDay(now.getActualMaximum(GregorianCalendar.DAY_OF_MONTH));
        tnfQuery.getTimeRange().setEndTime(Utils.getBeginningOfNextDay(tnfQuery.getTimeRange().getEndTime()));

        setTimeRange(tnfQuery.getTimeRange());

        Date startDateMustBeforeEndDate = Utils.getJavaDate(tnfQuery.getTimeRange().getStartTime());
        Date endDateMustAfterStartDate = Utils.getJavaDate(tnfQuery.getTimeRange().getEndTime());

        int daysBetween = Utils.getDaysBetweenDates(startDateMustBeforeEndDate, endDateMustAfterStartDate);

        if (daysBetween <= 0 || daysBetween == 1) {
            tnfQuery.getTimeRange().setGranularity("GRANULARITY_HOUR");
            if (isDateOutsideTNFRententionPeriodForGranularity(startDateMustBeforeEndDate, "GRANULARITY_HOUR")) {
                tnfQuery.getTimeRange().setGranularity("GRANULARITY_MONTH");
            }
        } else {
            tnfQuery.getTimeRange().setGranularity("GRANULARITY_DAY");
            if (isDateOutsideTNFRententionPeriodForGranularity(startDateMustBeforeEndDate, "GRANULARITY_DAY")) {
                tnfQuery.getTimeRange().setGranularity("GRANULARITY_MONTH");
            }
        }

        AttributeNames at = new AttributeNames();
        at.getAttributeName().add("COUNTRY_CODE");
        at.getAttributeName().add("PRODUCT_NAME");
        at.getAttributeName().add("SERVICE_NAME");
        tnfQuery.setAttributeNames(at);

        //uncomment the line below, its for Dev testing ONLY
        //tnfQuery.setLogicalSimId(getTNFLogicalID());
        log.warn("LogicalId for this productInstance is: {}", pi.getLogicalId());
        tnfQuery.setLogicalSimId(getTNFLogicalID(String.valueOf(pi.getLogicalId())));

        ReportIds ids = new ReportIds();
        //ids.getReportId().add("REPORT_TV_PER_APPLICATION");
        ids.getReportId().add("REPORT_ALL_APPS");
        ids.getReportId().add("REPORT_TV_GRANULARITY");
        tnfQuery.setReportIds(ids);

        TNFData data = SCAWrapper.getUserSpecificInstance().getTNFData(tnfQuery);
        timeRange = tnfQuery.getTimeRange();
        boolean noResultFoundForBreakdownDataset = true;
        boolean noResultFoundForTrendOverTimeDataset = true;

        try {
            ComplexResponse cr = SCPJAXBHelper.getPIsReportsAndAttributes(data.getTNFXmlData());
            if (cr.getMetricResponses() != null) {
                for (Report rp : cr.getMetricResponses().getReport()) {
                    if (rp.getTrendOverTimeDataset() != null) {
                        setTrendOverTimeDataset(rp.getTrendOverTimeDataset());
                        noResultFoundForTrendOverTimeDataset = false;
                    }
                    if (rp.getBreakdownDataset() != null) {
                        setBreakdownDataset(rp.getBreakdownDataset());
                        noResultFoundForBreakdownDataset = false;
                    }
                }
            }

        } catch (Exception ex) {
            log.warn("Error in getting tnf data for productinstance [{}], reason: {}", pi.getProductInstanceId(), ex.toString());
        }

        if (noResultFoundForBreakdownDataset && noResultFoundForTrendOverTimeDataset) {
            setPageMessage("no.tnf.results.found.forperiod");
        }
        if (noResultFoundForBreakdownDataset && !noResultFoundForTrendOverTimeDataset) {
            setPageMessage("no.tnf.results.found.forspline.graph");
        }
        if (!noResultFoundForBreakdownDataset && noResultFoundForTrendOverTimeDataset) {
            setPageMessage("no.tnf.results.found.forpie.graph");
        }

        return getDDForwardResolution("/account/view_transactions.jsp");
    }

    public Resolution getMe2UCompliantAccountListAsJSON() {
        long sourceAcc = getAccount().getAccountId();
        setAccountListAndRemoveSpecialAndInactiveAccounts(UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES));
        JsonArray ssAr = new JsonArray();
        for (Account targetAcc : getAccountList().getAccounts()) {
            if (targetAcc.getAccountId() == sourceAcc) {
                log.debug("Excluding account {} from participating in Me2U as it plays the role of source account", sourceAcc);
                continue;
            }
            JsonObject ss = new JsonObject();
            ss.addProperty("accountId", targetAcc.getAccountId());
            ss.addProperty("availableBalance", SmileTags.convertCentsToCurrencyLongWithCommaGroupingSeparator(targetAcc.getAvailableBalanceInCents()));
            ss.addProperty("dataBundleBalance", SmileTags.displayVolumeAsStringWithCommaGroupingSeparator(SmileTags.getBundleBalance(targetAcc.getAccountId()), "Byte"));
            ssAr.add(ss);
        }
        JsonObject accJson = new JsonObject();
        JsonObject accList = new JsonObject();
        accList.add("accounts", ssAr);
        accList.addProperty("numberOfAccounts", getAccountList().getAccounts().size());
        accJson.add("accountList", accList);
        String json = SmileTags.getObjectAsJsonString(accJson);

        return new StreamingResolution("application/json", new StringReader(json));
    }

    private void setAccountListForCustomer(Customer customer, StAccountLookupVerbosity verbosity) {
        Set<Long> uniqueAccounts = new HashSet<>();
        
        log.warn("setAccountListForCustomer looking for accounts for {}", customer.getFirstName());
        if (getAccountList() != null) {
            getAccountList().getAccounts().clear();
            getAccountList().setNumberOfAccounts(0);
            setAccountList(null);
        }
        setAccountList(new AccountList());

        for (ProductInstance pi : customer.getProductInstances()) {
            for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
                ServiceInstance si = m.getServiceInstance();
                if (si.getCustomerId() == customer.getCustomerId()) {
                    if (!isSIDeactivatedOrSIMCard(si)) {//dont add a service with status 'TD' to the list OR SIM Card service
                        uniqueAccounts.add(si.getAccountId());
                    }
                }
            }
        }
        
        log.warn("Unique accounts found: {}", uniqueAccounts.size());
        for (Long accNum : uniqueAccounts) {
            if (verbosity == StAccountLookupVerbosity.ACCOUNT) {
                getAccountList().getAccounts().add(UserSpecificCachedDataHelper.getAccount(accNum, StAccountLookupVerbosity.ACCOUNT));
            }
            if (verbosity == StAccountLookupVerbosity.ACCOUNT_UNITCREDITS) {
                Account ac = SCAWrapper.getUserSpecificInstance().getAccount(accNum, StAccountLookupVerbosity.ACCOUNT_UNITCREDITS);
                getAccountList().getAccounts().add(ac);
            }
            if (verbosity == StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS) {
                Account ac = SCAWrapper.getUserSpecificInstance().getAccount(accNum, StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS);
                getAccountList().getAccounts().add(ac);
            }
            if (verbosity == StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES) {
                Account ac = SCAWrapper.getUserSpecificInstance().getAccount(accNum, StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
                getAccountList().getAccounts().add(ac);
            }
        }
        
        log.warn("getAccountList() ADDED accounts: {}", getAccountList().getAccounts().size());
        getAccountList().setNumberOfAccounts(getAccountList().getAccounts().size());
    }

    private void populateProductInstanceListAndServiceInstanceList(Customer cust, StCustomerLookupVerbosity verbosity) {

        setServiceInstanceList(new ServiceInstanceList());
        setProductInstanceList(new ProductInstanceList());
        Set<Integer> activeProductInstances = new HashSet<>();

        if (verbosity == StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES) {
            for (ProductInstance pi : cust.getProductInstances()) {
                for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
                    ServiceInstance si = m.getServiceInstance();
                    if (si.getCustomerId() == cust.getCustomerId()) {
                        if (!isSIDeactivatedOrSIMCard(si)) {//dont add a service with status 'TD' to the list OR SIM Card service
                            getServiceInstanceList().getServiceInstances().add(si);
                            activeProductInstances.add(si.getProductInstanceId());
                        }
                    }
                }
            }
            getServiceInstanceList().setNumberOfServiceInstances(getServiceInstanceList().getServiceInstances().size());
        }

        for (int productInstanceId : activeProductInstances) {
            for (ProductInstance pi : cust.getProductInstances()) {
                if (productInstanceId == pi.getProductInstanceId()) {
                    getProductInstanceList().getProductInstances().add(pi);
                }
            }
        }
        getProductInstanceList().setNumberOfProductInstances(getProductInstanceList().getProductInstances().size());
    }

    private boolean isSIDeactivatedOrSIMCard(ServiceInstance si) {
        boolean ret = false;
        ServiceSpecification ss = NonUserSpecificCachedDataHelper.getServiceSpecification(si.getServiceSpecificationId());
        if (si.getStatus().equals("TD") || ss.getName().equals("SIM Card")) {
            log.debug("Service instance [{}] on product instance [{}] under account [{}] is deactivated or a SIM", new Object[]{si.getServiceInstanceId(), si.getProductInstanceId(), si.getAccountId()});
            ret = true;
        }
        return ret;
    }

    private boolean gatewayExist(String gatewayCode) {
        try {
            String subProperty = BaseUtils.getSubProperty("env.pgw." + gatewayCode.toLowerCase() + ".config", "GatewayCode");
            return true;
        } catch (Exception x) {
            log.warn("No payment gateway configuration corresponding to GatewayCode: {}", gatewayCode);
        }
        return false;
    }

    private boolean checkIfStaffOrPartnerService(int serviceSpecificationId) {
        return (serviceSpecificationId >= STAFF_SERVICES_ID_FLAG);
    }

    private boolean isCustomerAllowedToMakePayment(Customer customer) {
        boolean ret = false;
        Set<String> allowedCustomerIds = BaseUtils.getPropertyAsSet("env.scp.paymentgateway.makepayment.allowed.customers");
        if (allowedCustomerIds.isEmpty()) {
            return true;
        }
        try {
            for (String id : allowedCustomerIds) {
                int custId = Integer.parseInt(id);
                if (customer.getCustomerId() == custId) {
                    ret = true;
                    break;
                }
            }
        } catch (Exception ex) {
        }
        return ret;
    }

    private void setAccountListAndRemoveSpecialAndInactiveAccounts(Customer cust) {

        setAccountListForCustomer(cust, StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);

        if (getAccountList().getAccounts() != null && !getAccountList().getAccounts().isEmpty()) {

            AccountList specialAccountsExcludedAccountList = new AccountList();
            Set<Long> specialAccountIdsExcludedSet = new HashSet<>();
            for (Account acc : getAccountList().getAccounts()) {
                for (ServiceInstance si : acc.getServiceInstances()) {
                    if (checkIfStaffOrPartnerService(si.getServiceSpecificationId())) {
                        continue;
                    }
                    if (isSIDeactivatedOrSIMCard(si)) {
                        continue;
                    }
                    specialAccountIdsExcludedSet.add(si.getAccountId());
                }
            }

            for (Long accId : specialAccountIdsExcludedSet) {
                for (Account ac : getAccountList().getAccounts()) {
                    if (ac.getAccountId() == accId) {
                        specialAccountsExcludedAccountList.getAccounts().add(ac);
                    }
                }
            }

            //Reset
            getAccountList().getAccounts().clear();

            //Re-initialise
            setAccountList(null);
            setAccountList(new AccountList());

            getAccountList().getAccounts().addAll(specialAccountsExcludedAccountList.getAccounts());

            getAccountList().setNumberOfAccounts(specialAccountsExcludedAccountList.getAccounts().size());
        }
    }
    private final Map<Long, Set<String>> uniqueSIMsForAccount = new HashMap<>();

    public Map<Long, Set<String>> getUniqueSIMsForAccount() {
        return uniqueSIMsForAccount;
    }

    public void setUniqueSIMsForAccount(Account acc) {
        Set<String> uniqueSI = getUniqueSIMsForActiveServicesOnAccount(acc);
        uniqueSIMsForAccount.put(acc.getAccountId(), uniqueSI);
    }

    private Set<String> getUniqueSIMsForActiveServicesOnAccount(Account acc) {
        Set<String> uniqueSI = new HashSet<>();
        for (ServiceInstance si : acc.getServiceInstances()) {
            if (checkIfStaffOrPartnerService(si.getServiceSpecificationId())) {
                continue;
            }
            if (isSIDeactivatedOrSIMCard(si)) {
                continue;
            }
            String iccId = UserSpecificCachedDataHelper.getServiceInstanceICCID(si.getServiceInstanceId());
            if (!iccId.isEmpty() && !iccId.equalsIgnoreCase("NA") && !iccId.equalsIgnoreCase("No ICCID Found")) {
                uniqueSI.add(iccId);
            }
        }
        return uniqueSI;
    }

    private String getTNFLogicalID(String logicaId) {
        String leftPaddedLogicalId = StringUtils.leftPad(logicaId, 7, "0");
        String logSimId = BaseUtils.getProperty("env.logical.sim.mcc.mnc") + leftPaddedLogicalId;
        return logSimId;
    }

    private boolean isDateOutsideTNFRententionPeriodForGranularity(Date fromDate, String granularity) {
        //To see the predefined retention period for granularities, please refer to http://jira.smilecoms.com/browse/HBT-3687
        String granularities = BaseUtils.getProperty("env.tnf.granularities.with.retention");
        String granularityValue = Utils.getValueFromCRDelimitedAVPString(granularities, granularity);//GRANULARITY_HOUR=_H_1,7_DAY
        String[] bits = granularityValue.split(",");
        String granularityRetentionPeriod = bits[1];
        String[] granularityRetentionPeriodData = granularityRetentionPeriod.split("_");

        int retentionValue = Integer.parseInt(granularityRetentionPeriodData[0]);
        String retentionPeriod = granularityRetentionPeriodData[1];
        int daysBetween = Utils.getDaysBetweenDates(fromDate, new Date());
        if (granularity.equals("GRANULARITY_HOUR")) {
            daysBetween = 1;
        }

        switch (retentionPeriod.toUpperCase()) {
            case "HOUR":
                int val = daysBetween * 24;
                return val > retentionValue;
            case "DAY":
                return daysBetween > retentionValue;
            case "MONTH":
                int val2 = retentionValue * 31;//days
                return daysBetween > val2;
            case "YEAR":
                int val3 = retentionValue * 365;//days
                return daysBetween > val3;
            default:
                return false;
        }

    }

    @DontValidate()
    public Resolution splitUnitCreditInstance() {
        checkCSRF();
        Set<Long> accountIds = new HashSet<>();
        boolean defaultTargetIncluded = true;
        if (getContext().getValidationErrors().hasFieldErrors()) {
            defaultTargetIncluded = false;
        }

        String[] parameterValues = null;
        if (getParameter("splitDataTargetAccount") != null) {
            clearValidationErrors();
            parameterValues = getRequest().getParameterValues("splitDataTargetAccount");
        }
        if (parameterValues != null && parameterValues.length > 0) {
            for (String accId : parameterValues) {
                accountIds.add(Long.parseLong(accId));
            }
        }
        /* To add list option here */
        Set<String> listAcc = new LinkedHashSet<>();
        String accList = getParameter("AccountIdList");

        if (accList != null && !accList.isEmpty()) {
            //accList = "accountId" + "\r\n" + accList;
            clearValidationErrors();
            log.debug("Account ID List is [{}]", accList);
            listAcc.addAll(Arrays.asList(accList.split("\r\n")));
            for (String accou : listAcc) {
                accountIds.add(Long.parseLong(accou));
            }
        }
        /* End of list generation*/
        if (defaultTargetIncluded) {
            accountIds.add(getSplitUnitCreditData().getTargetAccountId());
        }
        if (accountIds.isEmpty()) {
            clearValidationErrors();
            localiseErrorAndAddToGlobalErrors("scp.split.unitcredit.notarget.account");
            return showSplitUnitCreditInstance();
        }
        if (accountIds.size() > BaseUtils.getIntProperty("env.split.unitcredit.max.target.accounts", 20)) {
            localiseErrorAndAddToGlobalErrors("scp.split.unitcredit.max.target.accounts.exceeded");
            return showSplitUnitCreditInstance();
        }
        setUnitCreditInstance(this.getUnitCreditInstance(getSplitUnitCreditData().getUnitCreditInstanceId()));
        StringBuilder errorCollections = new StringBuilder("[");
        boolean splitFailure = false;

        try {
            if (accountIds.size() == 1 && defaultTargetIncluded) {
                log.debug("Target account is [{}]", getSplitUnitCreditData().getTargetAccountId());
                SCAWrapper.getUserSpecificInstance().splitUnitCredit(getSplitUnitCreditData());
            } else {
                log.debug("Going to do a split to [{}] accounts", accountIds.size());
                for (Long accId : accountIds) {
                    log.debug("Target account is [{}]", accId);

                    if (accId < 1100000000) {
                        log.warn("Customer is attempting to use system account: target {}", accId);
                        String targ = "target: " + accId;
                        throw new InsufficientPrivilegesException("System accounts cannot be credited -- " + targ);
                    }

                    if (String.valueOf(accId).length() != 10) {
                        continue;
                    }

                    /* end  for i invalid account */
                    getSplitUnitCreditData().setTargetAccountId(accId);

                    try {
                        SCAWrapper.getUserSpecificInstance().splitUnitCredit(getSplitUnitCreditData());
                    } catch (Exception ex) {
                        log.warn("Failed to do split on account: ", ex);
                        splitFailure = true;
                        errorCollections.append(accId).append(",");
                    }
                }
            }

        } catch (SCABusinessError b) {
            setCustomerWithProductsAndServices();
            throw b;
        }
        setPageMessage("unit.credit.instance.split.successfully");
        if (splitFailure) {
            String errorColl = errorCollections.toString();
            errorColl = errorColl.substring(0, errorColl.lastIndexOf(","));
            errorColl = errorColl + "]";
            setPageMessage("scp.splituc.completed.partially", errorColl);
        }
        return retrieveAccount();
    }

    private UnitCreditInstance getUnitCreditInstance(int unitCreditInstanceId) {
        AccountQuery aq = new AccountQuery();
        aq.setUnitCreditInstanceId(unitCreditInstanceId);
        aq.setVerbosity(StAccountLookupVerbosity.ACCOUNT_UNITCREDITS);
        Account acc = SCAWrapper.getUserSpecificInstance().getAccount(aq);
        for (UnitCreditInstance uci : acc.getUnitCreditInstances()) {
            if (uci.getUnitCreditInstanceId() == unitCreditInstanceId) {
                return uci;
            }
        }
        return null;
    }

    public Resolution showSplitUnitCreditInstance() {
        setCustomerWithProductsAndServices();
        return getDDForwardResolution("/account/split_uc.jsp");
    }

    public Resolution getAccountsSIMsAsJSON() {
        Set<Integer> uniqueProductInstances = new HashSet<>();
        long sourceAcc = getAccount().getAccountId();
        Account ac = SCAWrapper.getUserSpecificInstance().getAccount(sourceAcc, StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);

        for (ServiceInstance si : ac.getServiceInstances()) {
            // http://jira.smilecoms.com/browse/HBT-6695
            if (si.getStatus().equals("TD")) {
                continue;
            }
            uniqueProductInstances.add(si.getProductInstanceId());
        }

        JsonArray ssAr = new JsonArray();
        for (int piId : uniqueProductInstances) {
            ProductInstance prodInstance = UserSpecificCachedDataHelper.getProductInstance(piId, StProductInstanceLookupVerbosity.MAIN);
            ProductSpecification productSpecification = NonUserSpecificCachedDataHelper.getProductSpecification(prodInstance.getProductSpecificationId());

            JsonObject ss = new JsonObject();
            ss.addProperty("productInstanceId", piId);
            ss.addProperty("ICCID", UserSpecificCachedDataHelper.getProductInstanceICCID(piId));
            ss.addProperty("friendlyName", prodInstance.getFriendlyName().isEmpty() ? productSpecification.getName() : prodInstance.getFriendlyName());
            ss.addProperty("publicIdentity", UserSpecificCachedDataHelper.getProductInstancePhoneNumber(piId));
            ssAr.add(ss);
        }
        JsonObject accJson = new JsonObject();
        JsonObject accList = new JsonObject();
        accList.add("products", ssAr);
        accList.addProperty("numberOfProducts", uniqueProductInstances.size());
        accList.addProperty("accountId", sourceAcc);
        accJson.add("accountSIMs", accList);
        String json = SmileTags.getObjectAsJsonString(accJson);

        return new StreamingResolution("application/json", new StringReader(json));
    }

    public Resolution getCustomersAccountsAsJson() {
        if (getCustomerQuery() == null) {
            setCustomerQuery(new CustomerQuery());
        }

        int pageSize = BaseUtils.getIntProperty("env.scp.pagesize", 30);
        getCustomerQuery().setCustomerId(getUserCustomerIdFromSession());
        getCustomerQuery().setResultLimit(1);
        getCustomerQuery().setProductInstanceResultLimit(pageSize);
        if (getCustomerQuery().getProductInstanceOffset() == null) {
            getCustomerQuery().setProductInstanceOffset(0);
        }

        int currentPage = Integer.parseInt(getParameter("pageNumber"));
        getCustomerQuery().setProductInstanceOffset(getCustomerQuery().getProductInstanceResultLimit() * (currentPage - 1));
        getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES);

        setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomerQuery()));
        getCustomer().setProductInstancesTotalCount(getCustomer().getProductInstancesTotalCount() != null ? getCustomer().getProductInstancesTotalCount() : 0);

        int totalPages = getCustomer().getProductInstancesTotalCount() / getCustomerQuery().getProductInstanceResultLimit();
        int pendingPagesCount = (getCustomer().getProductInstancesTotalCount() - (getCustomerQuery().getProductInstanceOffset())) / getCustomerQuery().getProductInstanceResultLimit();
        int pendingRecords = getCustomer().getProductInstancesTotalCount() - (getCustomerQuery().getProductInstanceOffset());

        Set<Long> uniqueAccounts = new HashSet<>();

        for (ProductInstance pi : getCustomer().getProductInstances()) {
            for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
                ServiceInstance si = m.getServiceInstance();
                if (si.getCustomerId() == getCustomer().getCustomerId()) {
                    if (!si.getStatus().equals("TD")) {
                        uniqueAccounts.add(si.getAccountId());
                    }
                }
            }
        }
        log.debug("Unique accounts found: {}, TOTAL PI [{}]", uniqueAccounts.size(), getCustomer().getProductInstancesTotalCount());

        setAccountList(new AccountList());

        for (long accNum : uniqueAccounts) {
            getAccountList().getAccounts().add(UserSpecificCachedDataHelper.getAccount(accNum, StAccountLookupVerbosity.ACCOUNT));
        }
        getAccountList().setNumberOfAccounts(getAccountList().getAccounts().size());

        JsonArray ssAr = new JsonArray();
        for (Account acc : getAccountList().getAccounts()) {
            JsonObject ac = new JsonObject();
            ac.addProperty("accountId", acc.getAccountId());
            ac.addProperty("availableBalanceInCents", acc.getAvailableBalanceInCents());
            ac.addProperty("currentBalanceInCents", acc.getCurrentBalanceInCents());
            ssAr.add(ac);
        }
        JsonObject accJson = new JsonObject();
        JsonObject accDetails = new JsonObject();
        accDetails.add("accounts", ssAr);
        //Product Instance Total Count is 100, returned accList 13, and LIMIT is 20

        accDetails.addProperty("pendingPagesCount", getCustomer().getProductInstancesTotalCount() < pageSize ? getAccountList().getNumberOfAccounts() : getCustomer().getProductInstancesTotalCount());
        //accDetails.addProperty("pendingPagesCount", pendingPagesCount > 1 ? pendingRecords : getAccountList().getNumberOfAccounts());
        //accDetails.addProperty("productInstancesTotalCount", getCustomer().getProductInstancesTotalCount());
        accDetails.addProperty("currentOffset", getAccountList().getNumberOfAccounts() >= getCustomerQuery().getProductInstanceResultLimit() ? getCustomerQuery().getProductInstanceOffset() : 0);
        accDetails.addProperty("numberOfAccounts", getAccountList().getNumberOfAccounts());
        accJson.add("userAccounts", accDetails);
        String json = SmileTags.getObjectAsJsonString(accJson);

        return new StreamingResolution("application/json", new StringReader(json));
    }

    private boolean isAllowedInThisLocation(UnitCreditSpecification ucs) {
        String allowedTowns = Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "AllowedTowns");
        if (allowedTowns == null) {
            log.debug("UCS [{}] is allowed in any town", ucs.getUnitCreditSpecificationId());
            return true;
        }
        log.debug("UCS [{}] is allowed in [{}]", ucs.getUnitCreditSpecificationId(), allowedTowns);
        Set<String> allowedTownSet = Utils.getSetFromCommaDelimitedString(allowedTowns.toLowerCase());

        String town = getUserTownBasedOnIPAddress();
        log.debug("Logged in customers town is [{}]", town);
        return allowedTownSet.contains(town);
    }

    private boolean isAllowedToDoMe2u() {
        Set<String> orgIDs = BaseUtils.getPropertyAsSet("env.uc.diaspora.icps");
        Customer loggedInCustomer = UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER);
        for (CustomerRole role : loggedInCustomer.getCustomerRoles()) {
            if (orgIDs.contains(String.valueOf(role.getOrganisationId()))) {
                return true;
            }
        }
        return false;
    }
    
    
    
     public Resolution showVerifySimsPage() { 
        setCustomerWithProductsAndServices();
        setAccountListForCustomer(getCustomer(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);

        if (getAccountList().getAccounts() != null || !getAccountList().getAccounts().isEmpty()) {
            if (getAccountList().getNumberOfAccounts() == 1) {
                setAccountQuery(new AccountQuery());
                getAccountQuery().setAccountId(getAccountList().getAccounts().get(0).getAccountId());
                return retrieveAccount();
            } else {
                //The verbosity of this customer object is: StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES         
                populateProductInstanceListAndServiceInstanceList(getCustomer(), StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES);
            }
        }
        return getDDForwardResolution("/account/verify_sims_page.jsp");
    }
    
    public Resolution checkRegStatus() {
       log.warn("In checkRegStatus");
       
       if(getParameter("simStatus").trim().equalsIgnoreCase("AC")) {
           setPageMessage("sim.active");
       } else if(getParameter("simStatus").trim().equalsIgnoreCase("DE")) {
           setPageMessage("sim.deleted");
       }else if(getParameter("simStatus").trim().equalsIgnoreCase("TD")) {
           setPageMessage("sim.deactivated");
       }
       
      
       return showVerifySimsPage();
    }

    public void verifySimcard() {
        
        showVerifySimsPage();
    } 
    
    public void removeSimcard() {
        
       showVerifySimsPage();
    }
    
    
    private List orderList(List<String> list) {
        if (list == null) {
            return null;
        }
        if (list.size() <= 1) {
            return list;
        }
        Comparator comparator = new UnitCreditSectionsComparator();
        Collections.sort(list, comparator);
        return list;
    }

    private class UnitCreditSectionsComparator implements Comparator {

        private final Set<String> sections = BaseUtils.getPropertyAsSet("env.scp.unitcredit.sections.order");

        public UnitCreditSectionsComparator() {
        }

        @Override
        public int compare(Object o1, Object o2) {

            int ret = 1;

            try {
                String o1Data = (String) o1;
                String o2Data = (String) o2;
                int o1Position = 1000;
                int o2Position = 1000;

                for (String section : sections) {
                    String[] bit = section.split("=");
                    if (o1Data.equals(bit[1])) {
                        o1Position = Integer.parseInt(bit[0]);
                        break;
                    }
                }

                for (String section : sections) {
                    String[] bit = section.split("=");
                    if (o2Data.equals(bit[1])) {
                        o2Position = Integer.parseInt(bit[0]);
                        break;
                    }
                }

                ret = ((Integer) o1Position).compareTo((Integer) o2Position);
            } catch (Exception ex) {
                log.error("Some error occured:: ", ex);
            }
            return ret;
        }
    }

    private String getRandomBubbleColor(Random r) {
        String[] bubbleColors = {"orange", "grey", "light-green", "dark-green"};
        int nextInt = r.nextInt(bubbleColors.length);
        return bubbleColors[nextInt];
    }
    
     public Resolution showVerifyRegulatorSim() {
      
        return getDDForwardResolution("/account/verify_sim_approval.jsp");
    }      
    
    public Resolution verifySimRegistration () {
        
       /* if(!BaseUtils.getProperty("env.country.name").equals("Tanzania")) {
            localiseErrorAndAddToGlobalErrors("error.system");
            return getDDForwardResolution("/sim/verify_sim_approval.jsp");
            
        }*/        
            String response="";            
                    
            
            ArrayList otherNumbers = new ArrayList();
            Customer cust = UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES);
            
            int agentCode = cust.getCustomerId();
            String agentNIN = getParameter("customerNIN").trim();
            
            String agentMSISDN = cust.getAlternativeContact1();
            
            String conversationId = String.valueOf(cust.getCustomerId()) + "-" + UUID.randomUUID().toString().substring(0, 8);
            
            
            List <ProductInstance> prodInstances = cust.getProductInstances();                        
            for (ProductInstance prodInstance : prodInstances) {
                
                List<ProductServiceInstanceMapping> servInstMaps = prodInstance.getProductServiceInstanceMappings();
                
                for(ProductServiceInstanceMapping prodServMap: servInstMaps) {
                    String pnumber = UserSpecificCachedDataHelper.getServiceInstancePhoneNumber(prodServMap.getServiceInstance().getServiceInstanceId());
                    
                    if(pnumber.trim().length()>0)
                        otherNumbers.add(UserSpecificCachedDataHelper.getServiceInstancePhoneNumber(prodServMap.getServiceInstance().getServiceInstanceId()));
                }
            }
            
             try {
                    URL url = new URL("http://10.24.64.20:8090/additionalSIMCard");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");

                    String nums="";
                    for(Object otherNumber: otherNumbers) {

                        if(nums.isEmpty())
                            nums = "\""+ otherNumber + "\"";
                        else
                            nums += ",\""+ otherNumber + "\"";
                    }
                                        
                    String input = "{"
                            + "\"agentCode\":\"" + agentCode + "\","
                            + "\"agentNIN\": \"" + agentNIN + "\","
                            + "\"agentMSISDN\":\"" + agentMSISDN + "\","
                            + "\"conversationId\":\"" + conversationId + "\","
                            + "\"customerMSISDN\":\"" + getParameter("customerMSISDN").trim() + "\","
                            + "\"customerNIN\":\"" + getParameter("customerNIN").trim() + "\","
                            + "\"reasonCode\":\"" + getParameter("addSimReasonCode").trim() + "\","
                            + "\"registrationCategoryCode\": \"" + getParameter("simRegistrationCategory").trim() + "\","                        
                            + "\"iccid\": \"" + getParameter("iccid").trim() + "\","                        
                            + "\"registrationType\": \"Existing\"," ;
                    
                        if(!getParameter("simRegistrationCategory").equals("2000")) {    
                           input += "\"otherNumbers\": [" + nums + "]";
                        } else {
                            input += "\"otherNumbers\": []";
                        } 
                        
                        input += "}";
                    
                    OutputStream os = conn.getOutputStream();
                    os.write(input.getBytes());
                    os.flush();

                    if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                            throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode() + " - "
                                    + conn.getResponseMessage());
                    }
                    
                    BufferedReader br = new BufferedReader(new InputStreamReader(
                                    (conn.getInputStream())));

                    
                    JSONParser parser = new JSONParser(); 
                    JSONObject json = new JSONObject();
                    
                    while ((response = br.readLine()) != null) {                        
                        try {
                            json = (JSONObject) parser.parse(response);
                        } catch (Exception e) { log.warn("Problem parsing response [{}]", response);}
                    } 
                    
                log.debug("TCRA AddSimVerify INPUT: [{}]", input);    
                log.debug("TCRA AddSimVerify RESPONSE: [{}]", json.toJSONString());
                
                String customerNIN="", customerMSISDN="", customerICCID="", tcraResponseDesc="";
                try {
                    JSONObject jsonInput = (JSONObject) parser.parse(input);
                    customerNIN= jsonInput.get("customerNIN").toString();
                    customerMSISDN= jsonInput.get("customerMSISDN").toString();
                    customerICCID= jsonInput.get("iccid").toString();
                    tcraResponseDesc= json.get("responseDescription").toString();
                    
                } catch (Exception e) {}
                
                storeAdditionalSimData(input, json.toJSONString(), customerNIN, customerMSISDN, customerICCID, tcraResponseDesc);
                                        
                if(!json.get("responseCode").equals("150")) {
                    if(json.get("responseCode").equals("999")) {
                        setRegulatorResponse("Regulator Response: " + json.get("responseDescription").toString());
                    
                    } else if(json.get("responseCode").equals("151")) {
                        setRegulatorResponse("Regulator Response: " + json.get("responseDescription").toString());
                    
                    } else if(json.get("responseCode").equals("152")) {
                        setRegulatorResponse("Regulator Response: " + json.get("responseDescription").toString());
                    
                    } else if(json.get("responseCode").equals("153")) {
                        setRegulatorResponse("Regulator Response: " + json.get("responseDescription").toString());
                    
                    } else if(json.get("responseCode").equals("154")) {
                        setRegulatorResponse("Regulator Response: " + json.get("responseDescription").toString());
                    
                    } else if(json.get("responseCode").equals("155")) {
                        setRegulatorResponse("Regulator Response: " + json.get("responseDescription").toString());
                    
                    } else if(json.get("responseCode").equals("156")) {
                        setRegulatorResponse("Regulator Response: " + json.get("responseDescription").toString());
                    
                    } else if(json.get("responseCode").equals("000")) {                        
                        setRegulatorResponse("Regulator Response: " + json.get("responseDescription").toString());
                       
                    } else {
                        localiseErrorAndAddToGlobalErrors("system.error");
                        
                    }
                     
                    return getDDForwardResolution("/account/verify_sim_approval.jsp");                   
                } else {
                    //log.warn("Returned: [{}]", json.toJSONString());
                    
                    setRegulatorResponse("Regulator Response: " + json.get("responseDescription").toString());
                }                    
                    
                conn.disconnect();
              } catch (MalformedURLException e) {

                    e.printStackTrace();
                    localiseErrorAndAddToGlobalErrors("System error");
                     
                    return getDDForwardResolution("/account/verify_sim_approval.jsp");  

              } catch (IOException e) {

                    e.printStackTrace();
                    localiseErrorAndAddToGlobalErrors("System error");
                     
                    return getDDForwardResolution("/account/verify_sim_approval.jsp");  
             }                         
            
            //return getDDForwardResolution("/account/verify_sim_approval.jsp");
            return showVerifySimsPage();            
    }
    
    
    public void storeAdditionalSimData(String tcraInput,String tcraResponse, String customerNIN, String customerMSISDN, String customerICCID, String tcraResponseDesc)
    {
        PreparedStatement ps = null;
        Connection conn = null;
        try {
                conn = JPAUtils.getNonJTAConnection("jdbc/SmileDB");
                conn.setAutoCommit(false);
                String query="insert into additional_sim_webservice_info (REQUEST_INPUT, RETURNED_RESPONSE, CUSTOMER_NIN, CUSTOMER_MSISDN,CUSTOMER_ICCID,TCRA_RESPONSE) values (?,?,?,?,?,?)";
                ps = conn.prepareStatement(query);              
                ps.setString(1, tcraInput);
                ps.setString(2, tcraResponse);
                ps.setString(3, customerNIN);
                ps.setString(4, customerMSISDN);
                ps.setString(5, customerICCID);
                ps.setString(6, tcraResponseDesc);
                
                log.warn("SQL: [{}]", ps.toString());
                int del = ps.executeUpdate();
                
                log.warn("Returned: [{}] ", del);
                ps.close();
                conn.commit();
                conn.close();
            } catch (Exception ex) {
                log.error("Error occured creating tcra data: "+ex);
            }
            finally
                {
                    if (ps != null) 
                    {
                        try {
                                ps.close();
                        } catch (SQLException ex) {
                                log.error("Error closing the prepared statement " + ex);
                        }
                    }
                    if (conn != null) 
                    {
                        try {
                                conn.close();
                        } catch (SQLException ex) {
                                log.error("Error closing the connection " + ex);
                        }
                    }
                }
        
    }

    
    private String iccid;
    private String phoneNumber;

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public java.lang.String getIccid() {
        return iccid;
    }

    public void setIccid(java.lang.String iccid) {
        this.iccid = iccid;
    }
    
   
    
    private String regulatorResponse;

    public String getRegulatorResponse() {
        return regulatorResponse;
    }

    public void setRegulatorResponse(String regulatorResponse) {
        this.regulatorResponse = regulatorResponse;
    }
    
}
