/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sep.action;

import com.google.gson.JsonObject;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.localisation.LocalisationHelper;
import com.smilecoms.commons.sca.*;
import com.smilecoms.commons.sca.beans.NonUserSpecificCachedDataHelper;
import com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper;
import com.smilecoms.commons.sca.direct.pvs.ResetAccountVoucherLock;
import com.smilecoms.commons.sca.direct.pvs.SendPrepaidStripsData;
import com.smilecoms.commons.stripes.InsufficientPrivilegesException;
import com.smilecoms.commons.stripes.SmileActionBean;
import com.smilecoms.commons.tags.SmileTags;
import com.smilecoms.commons.util.Codec;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.sep.helpers.ChargingDetail;
import com.smilecoms.sep.helpers.GraphViz;
import com.smilecoms.commons.sca.helpers.Permissions;
import com.smilecoms.sep.helpers.SEPJAXBHelper;
import com.smilecoms.sep.helpers.ext.tnf.BreakdownDataset;
import com.smilecoms.sep.helpers.ext.tnf.ComplexResponse;
import com.smilecoms.sep.helpers.ext.tnf.Report;
import com.smilecoms.sep.helpers.ext.tnf.TrendOverTimeDataset;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.xml.datatype.XMLGregorianCalendar;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.DontValidate;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import org.apache.commons.lang.StringUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import javax.servlet.http.HttpSession;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;



/**
 *
 * @author lesiba
 *
 */
public class AccountActionBean extends SmileActionBean {

    private boolean transferFromAnyAccount = false;
    private Set<Long> accountIdSet;
    private XMLGregorianCalendar searchMonth;
    private String sessionDetailBarGraph;
    private String sessionDetailLineGraph;

    public List<Long> getAccountIdList() {
        List ret = new ArrayList();
        if (accountIdSet != null) {
            ret.addAll(accountIdSet);
        }
        return ret;
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

    public String getSessionDetailBarGraph() {
        return sessionDetailBarGraph;
    }

    public Resolution showChangeStatus() {
        return getDDForwardResolution("/account/edit_account_status.jsp");
    }
    
    public Resolution showManageAccountServices() {
        return getDDForwardResolution("/account/manage_account_services.jsp");
    }
    
    public XMLGregorianCalendar getSearchMonth() {
        return searchMonth;
    }

    public void setSearchMonth(XMLGregorianCalendar searchMonth) {
        this.searchMonth = searchMonth;
    }

    private Set<Long> getLoggedInUsersAccounts() {
        return getCustomersAccounts(getUserCustomerIdFromSession());
    }

    private Set<Long> getCustomersAccounts(int custId) {
        Customer cust = UserSpecificCachedDataHelper.getCustomer(custId, StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES);
        Set<Long> accounts = new HashSet<>();
        for (ProductInstance pi : cust.getProductInstances()) {
            for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
                ServiceInstance si = m.getServiceInstance();
                if (si.getCustomerId() == custId) {
                    accounts.add(si.getAccountId());
                }
            }
        }
        return accounts;
    }

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
            } catch (Exception e) {
                log.debug("Error adding charging detail record", e);
            }
        }
        sessionDetailLineGraph = graphSessionDetail.toString();

        return getDDForwardResolution("/account/view_charging_detail.jsp");
    }

    public Resolution showTransfer() {
        checkPermissions(Permissions.TRANSFER_FUNDS);
        String custSSOID = getUser();
        if (custSSOID == null || custSSOID.isEmpty()) {
            localiseErrorAndAddToGlobalErrors("customer.ssoidentity.is.null");

            return getDDForwardResolution("/account/transfer.jsp");
        }

        // To set the transferFromAnyAccount variable
        try {
            checkPermissions(Permissions.TRANSFER_FUNDS_FROM_ANY_ACCOUNT);
            transferFromAnyAccount = true;

        } catch (InsufficientPrivilegesException ipe) {
            transferFromAnyAccount = false;
        }

        setCustomerQuery(new CustomerQuery());
        getCustomerQuery().setSSOIdentity(custSSOID);
        getCustomerQuery().setResultLimit(1);
        getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES);
        setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomerQuery()));
        accountIdSet = new HashSet<>();
        for (ProductInstance pi : getCustomer().getProductInstances()) {
            for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
                ServiceInstance si = m.getServiceInstance();
                if (si.getCustomerId() == getCustomer().getCustomerId()) {
                    accountIdSet.add(si.getAccountId());
                }
            }
        }
        if (getAccount() != null) {
            // Default target account with the account id
            setBalanceTransferData(new BalanceTransferData());
            getBalanceTransferData().setTargetAccountId(getAccount().getAccountId());
        }

        return getDDForwardResolution("/account/transfer.jsp");
    }

    public Resolution reverseBalanceTransfer() {
        checkPermissions(Permissions.REVERSE_BALANCE_TRANSFER);
        checkCSRF();

        SCAWrapper.getUserSpecificInstance().reverseTransactions(getTransactionReversalData());

        setAccountHistoryQuery(new AccountHistoryQuery());
        getAccountHistoryQuery().setAccountId(getAccount().getAccountId());
        return showTransactionHistory();
    }

    String transferRecipientKYCStatus;

    public java.lang.String getTransferRecipientKYCStatus() {
        return transferRecipientKYCStatus;
    }

    public void setTransferRecipientKYCStatus(java.lang.String transferRecipientKYCStatus) {
        this.transferRecipientKYCStatus = transferRecipientKYCStatus;
    }
    
    public Resolution doBalanceTransfer() {
        checkPermissions(Permissions.TRANSFER_FUNDS);

        if (!getLoggedInUsersAccounts().contains(getBalanceTransferData().getSourceAccountId())) {
            checkPermissions(Permissions.TRANSFER_FUNDS_FROM_ANY_ACCOUNT);
        }

        checkCSRF();
        
        if(BaseUtils.getBooleanProperty("env.buyer.seller.op.enabled", false)) {  //BuyerSeller Operation Enabled
            boolean sourceToTargetAllowed=false;
            int srcCustomerId = SCAWrapper.getAdminInstance().getAccount(getBalanceTransferData().getSourceAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES).getServiceInstances().get(0).getCustomerId();
            Customer srcCustomer = SCAWrapper.getUserSpecificInstance().getCustomer(srcCustomerId, StCustomerLookupVerbosity.CUSTOMER);
            
            int targetCustomerId = SCAWrapper.getAdminInstance().getAccount(getBalanceTransferData().getTargetAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES).getServiceInstances().get(0).getCustomerId();
            Customer targetCustomer = SCAWrapper.getUserSpecificInstance().getCustomer(targetCustomerId, StCustomerLookupVerbosity.CUSTOMER);
            
            
            if((srcCustomer.getCustomerRoles()!=null && targetCustomer.getCustomerRoles()!=null && !isICP(srcCustomer.getCustomerRoles().get(0).getOrganisationId())) &&  
                ((isSuperDealer(srcCustomer.getCustomerRoles().get(0).getOrganisationId()) ||
               isMegaDealer(srcCustomer.getCustomerRoles().get(0).getOrganisationId())) &&     
               (isICP(targetCustomer.getCustomerRoles().get(0).getOrganisationId()) ||
               isSuperDealer(targetCustomer.getCustomerRoles().get(0).getOrganisationId())))) {
                
                List<Integer> allowedSources = SCAWrapper.getUserSpecificInstance().getOrganisation(targetCustomer.getCustomerRoles().get(0).getOrganisationId(), StOrganisationLookupVerbosity.MAIN).getOrganisationSellers();
            
                for(int allowedSource: allowedSources) {
                    if(allowedSource == srcCustomer.getCustomerRoles().get(0).getOrganisationId()) {
                        sourceToTargetAllowed=true;
                        break;
                    }
                }

                if(!sourceToTargetAllowed) {
                    localiseErrorAndAddToGlobalErrors("cannot.sell.to.buyer");
                    return showTransfer();
                }
            }
            
        }
        
        HttpSession session = getContext().getRequest().getSession();           
        Account srcAcc = SCAWrapper.getAdminInstance().getAccount(getBalanceTransferData().getSourceAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
        Account destAcc = SCAWrapper.getAdminInstance().getAccount(getBalanceTransferData().getTargetAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
        
        if (notConfirmed()) {
            
            //if the 2 accounts do not belong to the same customer & OTP is enabled
            if((srcAcc.getServiceInstances().get(0).getCustomerId() != destAcc.getServiceInstances().get(0).getCustomerId()) 
                    && BaseUtils.getBooleanProperty("env.sep.otp.enabled", false)) {
                    
                    if(session.getAttribute("otpConfirmed")==null || session.getAttribute("otpConfirmed")!="true") {
                            session.removeAttribute("sessionOtp");
                            setOtp("");

                            int customerId=-1;
                            if (srcAcc.getServiceInstances().size() > 0) {
                                customerId = srcAcc.getServiceInstances().get(0).getCustomerId();

                                Customer customer = null;
                                CustomerQuery customerQuery = new CustomerQuery();
                                customerQuery.setResultLimit(1);
                                customerQuery.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
                                customerQuery.setCustomerId(customerId);
                                try {
                                    customer = SCAWrapper.getAdminInstance().getCustomer(customerQuery);
                                } catch (Exception ex) {
                                    log.debug("Error getting user: ", ex);
                                } 
                        //Call otpConfirm with the client's registered no.
                        return otpConfirm(customer.getAlternativeContact1());
                    }
                } else {
                        session.removeAttribute("sessionOtp");   
                        setOtp("");
                        localiseErrorAndAddToGlobalErrors("Error Occured"); 
                        setConfirmed(false);
                        return getDDForwardResolution("/account/transfer.jsp");            
                }                
                
            } else {
                return confirm();
            }
    }   
        
        getBalanceTransferData().setSCAContext(null); // Safety precaution to prevent impersonation
        getBalanceTransferData().setAmountInCents(getBalanceTransferData().getAmountInCents() * 100);        
        try {
            SCAWrapper.getUserSpecificInstance().transferBalance(getBalanceTransferData());
        } catch (SCABusinessError e) {            
            e.printStackTrace();
            session.removeAttribute("sessionOtp");   
            setOtp("");
            setConfirmed(false);
            showTransfer();
            throw e;
        }
        AccountHistoryQuery ahq = new AccountHistoryQuery();
        ahq.setExtTxId(getBalanceTransferData().getSCAContext().getTxId());
        ahq.setResultLimit(10);
        // Allow seeing the transfer data
        setAccountHistory(SCAWrapper.getAdminInstance().getAccountHistory(ahq));
        setPageMessage("transfer.completed.succesfully");

        for (ServiceInstance si : destAcc.getServiceInstances()) {
            setCustomer(SCAWrapper.getAdminInstance().getCustomer(si.getCustomerId(), StCustomerLookupVerbosity.CUSTOMER));
            transferRecipientKYCStatus = getCustomer().getKYCStatus();
            break;
        }
        return showTransfer();
    }

    public Resolution removePrepaidVoucherLockForAccount() {
        checkPermissions(Permissions.REMOVE_ACCOUNT_VOUCHER_LOCK);

        ResetAccountVoucherLock resetAccountVoucherLock = new ResetAccountVoucherLock();
        resetAccountVoucherLock.setAccountIdToReset(getAccount().getAccountId());

        com.smilecoms.commons.sca.direct.pvs.Done isDone = SCAWrapper.getUserSpecificInstance().resetVoucherLockForAccount_Direct(resetAccountVoucherLock);

        if (isDone.getDone() == com.smilecoms.commons.sca.direct.pvs.StDone.TRUE) {
            setPageMessage("voucher.lock.removed.successfully");
        } else {
            localiseErrorAndAddToGlobalErrors("voucher.lock.removal.failed");
            
        }

        if (getAccountQuery() == null && getAccount() != null) {
            setAccountQuery(new AccountQuery());
            getAccountQuery().setAccountId(getAccount().getAccountId());
        }

        // return getDDForwardResolution("/account/view_account.jsp");
        return retrieveAccount();
    }

    public Resolution retrieveAccount() {
        checkPermissions(Permissions.VIEW_ACCOUNT);
        if (getAccountQuery() == null && getAccount() != null) {
            setAccountQuery(new AccountQuery());
            getAccountQuery().setAccountId(getAccount().getAccountId());
        } else if (getAccountQuery() == null && getUnitCreditInstance() != null && getUnitCreditInstance().getAccountId() > 0) {
            setAccountQuery(new AccountQuery());
            getAccountQuery().setAccountId(getUnitCreditInstance().getAccountId());
        }

        getAccountQuery().setVerbosity(StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
        setAccount(SCAWrapper.getUserSpecificInstance().getAccount(getAccountQuery()));

        // Flag which UC's cannot be used by any services under the account
        ucCannotBeUsedSet = new HashSet<>();
        for (UnitCreditInstance uci : getAccount().getUnitCreditInstances()) {
            log.debug("Checking if UC [{}] for PI [{}] can be used by any services under the account", uci.getUnitCreditInstanceId(), uci.getProductInstanceId());
            boolean canBeUsed = false;
            for (ServiceInstance si : getAccount().getServiceInstances()) {
                log.debug("Checking if SI [{}] can be used by UC [{}]", si.getServiceInstanceId(), uci.getUnitCreditInstanceId());
                for (ProductServiceMapping psm : NonUserSpecificCachedDataHelper.getUnitCreditSpecification(uci.getUnitCreditSpecificationId()).getProductServiceMappings()) {
                    // Loop though UC's allowed service spec Ids
                    log.debug("The UC can be used by service spec id [{}] and prod spec id [{}]", psm.getServiceSpecificationId(), psm.getProductSpecificationId());
                    if (si.getServiceSpecificationId() == psm.getServiceSpecificationId()
                            && (uci.getProductInstanceId() == si.getProductInstanceId() || uci.getProductInstanceId() == 0)
                            && (psm.getProductSpecificationId() == 0
                            || psm.getProductSpecificationId() == UserSpecificCachedDataHelper.getProductInstance(si.getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN).getProductSpecificationId())) {
                        // Found a SI that can use this UC
                        log.debug("UC can be used");
                        canBeUsed = true;
                        break;
                    }
                }
                if (canBeUsed) {
                    break;
                }
            }
            if (!canBeUsed) {
                log.debug("UCI [{}] cannot be used", uci.getUnitCreditInstanceId());
                ucCannotBeUsedSet.add(String.valueOf(uci.getUnitCreditInstanceId()));
            }
        }

        return getDDForwardResolution("/account/view_account.jsp");
    }

    Set<String> ucCannotBeUsedSet;

    public Set<String> getUcCannotBeUsedSet() {
        return ucCannotBeUsedSet;
    }

    public Resolution checkRate() {
        checkPermissions(Permissions.VIEW_ACCOUNT);
        if (getReservationRequestData() != null) {
            setServiceInstanceQuery(new ServiceInstanceQuery());
            getServiceInstanceQuery().setVerbosity(StServiceInstanceLookupVerbosity.MAIN_SVCAVP_MAPPINGS);
            getServiceInstanceQuery().setServiceInstanceId(getServiceInstance().getServiceInstanceId());
            ServiceInstance si = SCAWrapper.getUserSpecificInstance().getServiceInstances(getServiceInstanceQuery()).getServiceInstances().get(0);
            ReservationRequestData rrd = getReservationRequestData();
            rrd.setEventTimestamp(Utils.getDateAsXMLGregorianCalendar(new Date()));
            rrd.setIdentifier(si.getServiceInstanceMappings().get(0).getIdentifier());
            rrd.setIdentifierType(si.getServiceInstanceMappings().get(0).getIdentifierType());
            setReservationResultData(SCAWrapper.getUserSpecificInstance().checkReservation(rrd));
        }
        return getDDForwardResolution("/account/check_rate.jsp");
    }

    public List<String> getAllowedAccountStatuses() {
        return BaseUtils.getPropertyAsList("global.account.allowed.statuses.sep");
    }

    public Resolution modifyAccountStatus() {
        checkPermissions(Permissions.CHANGE_ACCOUNT_STATUS);
        if (notConfirmed()) {
            return confirm();
        }
        if (!BaseUtils.getPropertyAsSet("global.account.allowed.statuses.sep").contains(String.valueOf(getAccount().getStatus()))) {
            localiseErrorAndAddToGlobalErrors("sep.account.status.change.unknown.option.selected");
            return retrieveAccount();
        }
        Account acc = SCAWrapper.getUserSpecificInstance().getAccount(getAccount().getAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS);

        /*
                If account status from DB is equal to one of those that cannot be overriden
                then disallow account status update... these statuses are held by the property
                env.immutable.account.statuses.sep
         */
        if (!BaseUtils.getProperty("env.immutable.account.statuses.sep", "").isEmpty()) {
            if (BaseUtils.getPropertyAsSet("env.immutable.account.statuses.sep").contains(String.valueOf(acc.getStatus())) && !hasPermissions(Permissions.OVERRIDE_RESTRICTIVE_ACCOUNT_STATUS)) {
                localiseErrorAndAddToGlobalErrors("sep.account.status.override.notallowed");
                return retrieveAccount();
            }
        }

        for (Reservation res : acc.getReservations()) {
            if (res.getDescription().equals("SMS") && getAccount().getStatus() == 14) {
                localiseErrorAndAddToGlobalErrors("sep.account.status.allusage.selected.withactive.reservation");
                return retrieveAccount();
            } else if (res.getDescription().equals("Voice") && getAccount().getStatus() == 14) {
                localiseErrorAndAddToGlobalErrors("sep.account.status.allusage.selected.withactive.reservation");
                return retrieveAccount();
            } else if (res.getDescription().equals("Data") && getAccount().getStatus() == 14) {//Need to be tested; one might only be able to change this if not browsing at all
                localiseErrorAndAddToGlobalErrors("sep.account.status.allusage.selected.withactive.reservation");
                return retrieveAccount();
            }
        }

        SCAWrapper.getUserSpecificInstance().modifyAccount(getAccount());
        return retrieveAccount();
    }
    
    public List<String> getAllowedAccountLevelServiceStatuses() {
        return BaseUtils.getPropertyAsList("global.account.allowed.service.statuses.sep");
    }
    
    public List<String> getAccountLevelServiceStatusReason() {
        return BaseUtils.getPropertyAsList("global.account.allowed.service.statuses.reason.sep");
    }
   

    public Resolution modifyAccountServiceStatus() {
        checkPermissions(Permissions.CHANGE_ACCOUNT_SERVICES_STATUS);
        if (notConfirmed()) {
            return confirm();
        }
        
        if (!BaseUtils.getPropertyAsSet("global.account.allowed.service.statuses.sep").contains(String.valueOf(getParameter("serviceStatus")))) {
            localiseErrorAndAddToGlobalErrors("Error", "Invalid account service status provided");
            return retrieveAccount();
        } 
        
        Account acc = SCAWrapper.getUserSpecificInstance().getAccount(getAccount().getAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
        if(getParameter("serviceStatus").equalsIgnoreCase("TD")) {
            checkPermissions(Permissions.TEMPORARILY_DEACTIVATE_SERVICE_INSTANCE);
            checkCSRF();
            for(ServiceInstance si: acc.getServiceInstances()) {
               
               if(si.getStatus().equalsIgnoreCase("AC")) {
                    ProductOrder order = new ProductOrder();
                    order.setProductInstanceId(si.getProductInstanceId()); 
                    order.setAction(StAction.NONE);
                    order.setCustomerId(si.getCustomerId());
                    order.getServiceInstanceOrders().add(new ServiceInstanceOrder());
                    order.getServiceInstanceOrders().get(0).setAction(StAction.UPDATE);
                    order.getServiceInstanceOrders().get(0).setServiceInstance(si);                
                    order.getServiceInstanceOrders().get(0).getServiceInstance().setStatus(getParameter("serviceStatus"));
                    order.getServiceInstanceOrders().get(0).getServiceInstance().setAccountId(si.getAccountId());
                    try {
                        
                        SCAWrapper.getUserSpecificInstance().processOrder(order);
                    } catch (SCABusinessError e) {
                        localiseErrorAndAddToGlobalErrors("Error", "Something went wrong trying to update service status");
                        retrieveAccount();
                        throw e;
                    }
               }
            } 
        } else if(getParameter("serviceStatus").equalsIgnoreCase("AC")) {
            checkPermissions(Permissions.REACTIVATE_SERVICE_INSTANCE);
            checkCSRF();
            for(ServiceInstance si: acc.getServiceInstances()) {               

               if(si.getStatus().equalsIgnoreCase("TD")) {
                    ProductOrder order = new ProductOrder();
                    order.setProductInstanceId(si.getProductInstanceId()); 
                    order.setAction(StAction.NONE);
                    order.setCustomerId(si.getCustomerId());
                    order.getServiceInstanceOrders().add(new ServiceInstanceOrder());
                    order.getServiceInstanceOrders().get(0).setAction(StAction.UPDATE);
                    order.getServiceInstanceOrders().get(0).setServiceInstance(si);                
                    order.getServiceInstanceOrders().get(0).getServiceInstance().setStatus(getParameter("serviceStatus"));
                    order.getServiceInstanceOrders().get(0).getServiceInstance().setAccountId(si.getAccountId());
                    try {
                        SCAWrapper.getUserSpecificInstance().processOrder(order);
                    } catch (SCABusinessError e) {
                        localiseErrorAndAddToGlobalErrors("Error", "Something went wrong trying to update service status");
                        retrieveAccount();
                        throw e;
                    }
               }
            }    
        }
        
        Event event = new Event();
        event.setEventType("PM");
        event.setEventSubType("ChangeAccountServicesStatus");
        event.setEventKey(getUser());
        event.setEventData(new Date() + " | " + getUser() + " changed active account services status to " + getAccount().getStatus() + ". | Reason: " + getParameter("reason"));
          
        SCAWrapper.getAdminInstance().createEvent(event);
        setPageMessage("account.status.change.success");  
        return retrieveAccount();
    }
    
    public Resolution showAddUnitCredits() {
        checkPermissions(Permissions.PURCHASE_UNIT_CREDIT);
        setUnitCreditSpecificationList(new UnitCreditSpecificationList());
        UnitCreditSpecificationQuery q = new UnitCreditSpecificationQuery();
        q.setUnitCreditSpecificationId(-1);
        q.setVerbosity(StUnitCreditSpecificationLookupVerbosity.MAIN);
        UnitCreditSpecificationList allUCs = (SCAWrapper.getUserSpecificInstance().getUnitCreditSpecifications(q));
        for (UnitCreditSpecification ucs : allUCs.getUnitCreditSpecifications()) {
            if (Utils.isBetween(new Date(), ucs.getAvailableFrom(), ucs.getAvailableTo())) {
                if (isAllowed(ucs.getPurchaseRoles())) {
                    if (isAllowedInThisLocation(ucs)) {
                        getUnitCreditSpecificationList().getUnitCreditSpecifications().add(ucs);
                    }
                }
            }
        }
        return getDDForwardResolution("/product_catalog/add_unit_credit.jsp");
    }

    private Map<Integer, String> productInstanceSIMList;

    public Map<Integer, String> getProductInstanceSIMList() {
        return productInstanceSIMList;
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

    public Resolution provisionUnitCreditForSim() {
        if (getPurchaseUnitCreditRequest().getAccountId() == getPurchaseUnitCreditRequest().getPaidByAccountId()) {
            checkPermissions(Permissions.PURCHASE_UNIT_CREDIT);
        } else if (getLoggedInUsersAccounts().contains(getPurchaseUnitCreditRequest().getPaidByAccountId())) {
            checkPermissions(Permissions.PURCHASE_UNIT_CREDIT_PAID_BY_LOGGED_IN_USERS_ACCOUNT);
        } else if (getCustomersAccounts(UserSpecificCachedDataHelper.getPrimaryAccountHolderCustomerId(getPurchaseUnitCreditRequest().getAccountId())).contains(getPurchaseUnitCreditRequest().getPaidByAccountId())) {
            checkPermissions(Permissions.PURCHASE_UNIT_CREDIT_PAID_BY_CUSTOMERS_ACCOUNT);
        } else {
            checkPermissions(Permissions.PURCHASE_UNIT_CREDIT_PAID_BY_ANY_ACCOUNT);
        }

        if (notConfirmed()) {
            UnitCreditSpecificationQuery q = new UnitCreditSpecificationQuery();
            q.setUnitCreditSpecificationId(getPurchaseUnitCreditRequest().getUnitCreditSpecificationId());
            q.setVerbosity(StUnitCreditSpecificationLookupVerbosity.MAIN_SVCSPECIDS);
            UnitCreditSpecification ucs = SCAWrapper.getUserSpecificInstance().getUnitCreditSpecifications(q).getUnitCreditSpecifications().get(0);
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
            return confirm(SmileTags.displayVolumeAsString(ucs.getUnits(), ucs.getUnitType()), SmileTags.convertCentsToCurrencyShort(ucs.getPriceInCents()), SmileTags.formatDateShort(unitCreditEndDate), String.valueOf(getPurchaseUnitCreditRequest().getNumberToPurchase()));
        }
        SCAWrapper.getUserSpecificInstance().purchaseUnitCredit(getPurchaseUnitCreditRequest());
        setPageMessage("bundle.added");
        return retrieveAccount();
    }

    public Resolution provisionUnitCredit() {
        checkCSRF();
        if (getPurchaseUnitCreditRequest().getAccountId() == getPurchaseUnitCreditRequest().getPaidByAccountId()) {
            checkPermissions(Permissions.PURCHASE_UNIT_CREDIT);
        } else if (getLoggedInUsersAccounts().contains(getPurchaseUnitCreditRequest().getPaidByAccountId())) {
            checkPermissions(Permissions.PURCHASE_UNIT_CREDIT_PAID_BY_LOGGED_IN_USERS_ACCOUNT);
        } else if (getCustomersAccounts(UserSpecificCachedDataHelper.getPrimaryAccountHolderCustomerId(getPurchaseUnitCreditRequest().getAccountId())).contains(getPurchaseUnitCreditRequest().getPaidByAccountId())) {
            checkPermissions(Permissions.PURCHASE_UNIT_CREDIT_PAID_BY_CUSTOMERS_ACCOUNT);
        } else {
            checkPermissions(Permissions.PURCHASE_UNIT_CREDIT_PAID_BY_ANY_ACCOUNT);
        }
        try {
            productInstanceSIMList = new HashMap<>();
            UnitCreditSpecificationQuery q = new UnitCreditSpecificationQuery();
            q.setUnitCreditSpecificationId(getPurchaseUnitCreditRequest().getUnitCreditSpecificationId());
            q.setVerbosity(StUnitCreditSpecificationLookupVerbosity.MAIN_SVCSPECIDS);
            UnitCreditSpecification ucs = SCAWrapper.getUserSpecificInstance().getUnitCreditSpecifications(q).getUnitCreditSpecifications().get(0);
            String sharingProp = Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "AllowSharing");
            boolean allowsSharing = (sharingProp != null && sharingProp.equalsIgnoreCase("true"));

            if (!isAllowed(ucs.getPurchaseRoles())) {
                localiseErrorAndAddToGlobalErrors("unit.credit.not.allowed");
                return showAddUnitCredits();
            }
            setAccount(SCAWrapper.getUserSpecificInstance().getAccount(getPurchaseUnitCreditRequest().getAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES));
            
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
                            log.warn("Customer requested to buy bundle but does not have required main product.");
                            localiseErrorAndAddToGlobalErrors("unit.credit.not.allowed.without.spec.ids");                            
                            return showAddUnitCredits();
                        }
                    }
            }
            
            
            //Todo: If the UCS allows sharing, then it should be an option to send PI as 0 so that it can be shared in BM
            for (ServiceInstance si : getAccount().getServiceInstances()) {
                boolean canBeUsed = false;
                for (ProductServiceMapping psm : ucs.getProductServiceMappings()) {
                    // Loop though UC's allowed service spec Ids
                    log.debug("The UC can be used by service spec id [{}] and prod spec id [{}]", psm.getServiceSpecificationId(), psm.getProductSpecificationId());
                    if (si.getServiceSpecificationId() == psm.getServiceSpecificationId()
                            && (psm.getProductSpecificationId() == 0
                            || psm.getProductSpecificationId() == UserSpecificCachedDataHelper.getProductInstance(si.getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN).getProductSpecificationId())) {
                        // Found a SI that can use this UC
                        log.debug("UC can be used");
                        canBeUsed = true;
                        break;
                    }
                }
                if (!canBeUsed) {
                    continue;
                }
                si = SCAWrapper.getUserSpecificInstance().getServiceInstance(si.getServiceInstanceId(), StServiceInstanceLookupVerbosity.MAIN_SVCAVP);
                getPurchaseUnitCreditRequest().setProductInstanceId(si.getProductInstanceId());// For when there's one Browsing Service Instance
                for (AVP avp : si.getAVPs()) {
                    if (avp.getAttribute().equals("IntegratedCircuitCardIdentifier")) {
                        productInstanceSIMList.put(si.getProductInstanceId(), avp.getValue());
                        break;
                    }
                }
            }

            if (allowsSharing) {
                getPurchaseUnitCreditRequest().setProductInstanceId(0);
            }

            if (productInstanceSIMList.size() == 1 || allowsSharing) {
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
                    return confirm(SmileTags.displayVolumeAsString(ucs.getUnits(), ucs.getUnitType()), SmileTags.convertCentsToCurrencyShort(ucs.getPriceInCents()), SmileTags.formatDateShort(unitCreditEndDate), String.valueOf(getPurchaseUnitCreditRequest().getNumberToPurchase()));
                }
                SCAWrapper.getUserSpecificInstance().purchaseUnitCredit(getPurchaseUnitCreditRequest());
                setPageMessage("unit.credit.added");
            } else if (productInstanceSIMList.size() > 1) {
                //Navigate to page to prompt customer to choose which service instance to load the bundle on
                return getDDForwardResolution("/account/select_sim_to_load_bundle.jsp");
            } else if (productInstanceSIMList.isEmpty()) {
                this.localiseErrorAndAddToGlobalErrors("no.valid.services.for.unit.credit");
                return showAddUnitCredits();
            }
        } catch (SCABusinessError e) {
            showAddUnitCredits();
            throw e;
        }

        return retrieveAccount();
    }

    @DefaultHandler
    public Resolution showSearchAccount() {
        checkPermissions(Permissions.VIEW_ACCOUNT);
        return getDDForwardResolution("/account/search_account.jsp");
    }

    public Resolution showRedeemPrepaidStrip() {
        checkPermissions(Permissions.REDEEM_STRIP);
        return getDDForwardResolution("/account/redeem_strip.jsp");
    }

    public Resolution showCreateStrips() {
        checkPermissions(Permissions.CREATE_STRIPS);
        Calendar now = Calendar.getInstance();
        now.add(Calendar.YEAR, 3);
        setNewPrepaidStripsData(new NewPrepaidStripsData());
        getNewPrepaidStripsData().setExpiryDate(Utils.getDateAsXMLGregorianCalendar(now.getTime()));
        return getDDForwardResolution("/account/create_strips.jsp");
    }

    public Resolution showViewStrip() {
        checkPermissions(Permissions.VIEW_STRIP);
        return getDDForwardResolution("/account/view_strip.jsp");
    }
    
    public Resolution showResendStrips() {
        checkPermissions(Permissions.SEND_STRIPS_TO_DISTRIBUTOR);
        return getDDForwardResolution("/account/send_strips.jsp");
    }
    
    public Resolution resendStrips() {
        checkPermissions(Permissions.SEND_STRIPS_TO_DISTRIBUTOR);
                
        SendPrepaidStripsData spsd = new SendPrepaidStripsData();
        spsd.setSaleId(Integer.parseInt(getPrepaidStripBatchData().getInvoiceData()));
        SCAWrapper.getAdminInstance().sendPrepaidStripsForInvoice_Direct(spsd);
        
        // getSendPrepaidStripsData()
        return getDDForwardResolution("/account/send_strips.jsp");
    }

    public Resolution showBatchUpdateStrips() {
        return getDDForwardResolution("/account/batch_update_strips.jsp");
    }

    public Resolution retrieveStrip() {
        checkPermissions(Permissions.VIEW_STRIP);
        setPrepaidStrip(SCAWrapper.getUserSpecificInstance().getPrepaidStrip(getPrepaidStripQuery()));
        if (getPrepaidStrip().getRedemptionAccountHistoryId() > 0) {
            setAccountHistoryQuery(new AccountHistoryQuery());
            getAccountHistoryQuery().setTransactionRecordId(getPrepaidStrip().getRedemptionAccountHistoryId());
            getAccountHistoryQuery().setResultLimit(1);
            setAccountHistory(SCAWrapper.getUserSpecificInstance().getAccountHistory(getAccountHistoryQuery()));
        }
        return getDDForwardResolution("/account/view_strip.jsp");
    }

    public Resolution createStrips() {
        checkPermissions(Permissions.CREATE_STRIPS);
        checkCSRF();
        if (notConfirmed()) {
            return confirm();
        }
        // Convert to cents
        getNewPrepaidStripsData().setValueInCents(getNewPrepaidStripsData().getValueInCents() * 100);
        SCAWrapper.getUserSpecificInstance().createPrepaidStrips(getNewPrepaidStripsData());
        setPageMessage("strips.created.successfully");
        return getDDForwardResolution("/account/create_strips.jsp");
    }

    public Resolution batchUpdateStrips() {
        switch (getPrepaidStripBatchData().getStatus()) {
            case "DC":
                checkPermissions(Permissions.BULK_CHANGE_STRIP_STATUS_TO_DC);
                break;
            case "EX":
                checkPermissions(Permissions.BULK_CHANGE_STRIP_STATUS_TO_EX);
                break;
            case "WH":
                checkPermissions(Permissions.BULK_CHANGE_STRIP_STATUS_TO_WH);
                break;
            default:
                showBatchUpdateStrips();
                break;
        }

        if (getPrepaidStripBatchData().getStatus().equalsIgnoreCase("DC")) {
            // Only the invoice must be supplied to move into DC
            if (StringUtils.isEmpty(getPrepaidStripBatchData().getInvoiceData())) {
                localiseErrorAndAddToGlobalErrors("batch.update.strips.missing.saleid");
                return getDDForwardResolution("/account/batch_update_strips.jsp");
            }

            if (!StringUtils.isNumeric(getPrepaidStripBatchData().getInvoiceData())) {
                localiseErrorAndAddToGlobalErrors("batch.update.strips.invalid.saleid.format");
                return getDDForwardResolution("/account/batch_update_strips.jsp");
            }

            // Ignore these as we will use strip ids calculated from the sale. 
            getPrepaidStripBatchData().setEndingPrepaidStripId(0);
            getPrepaidStripBatchData().setStartingPrepaidStripId(0);
        } else {

            // For any status update, the start and end strip ids must be specified.
            if (!(getPrepaidStripBatchData().getEndingPrepaidStripId() > 0)
                    || !(getPrepaidStripBatchData().getStartingPrepaidStripId() > 0)) {
                localiseErrorAndAddToGlobalErrors("batch.update.strips.start.or.end.stripid.missing");
                return getDDForwardResolution("/account/batch_update_strips.jsp");
            }
        }

        checkCSRF();
        if (notConfirmed()) {
            return confirm();
        }
        SCAWrapper.getUserSpecificInstance().batchUpdatePrepaidStrips(getPrepaidStripBatchData());
        
        if(BaseUtils.getBooleanProperty("env.pvs.dc.send.pgp.encrypted.file", false) && getPrepaidStripBatchData().getStatus().equals("DC")) {
            setPageMessage("strips.updated.successfully.pgp.encrypted.file.sent");
        } else {
            setPageMessage("strips.updated.successfully");
        }
        return getDDForwardResolution("/account/batch_update_strips.jsp");
    }

    public Resolution redeemPrepaidStrip() {
        checkPermissions(Permissions.REDEEM_STRIP);
        checkCSRF();
        if (notConfirmed()) {
            return confirm();
        }

        getPrepaidStripRedemptionData().setEncryptedPIN(Codec.stringToEncryptedHexString(getPrepaidStripRedemptionData().getEncryptedPIN()));
        Done isDone = SCAWrapper.getUserSpecificInstance().redeemPrepaidStrip(getPrepaidStripRedemptionData());
        if (isDone.getDone() == StDone.TRUE) {
            AccountHistoryQuery ahq = new AccountHistoryQuery();
            ahq.setExtTxId(getPrepaidStripRedemptionData().getSCAContext().getTxId());
            ahq.setResultLimit(2);
            setAccountHistory(SCAWrapper.getUserSpecificInstance().getAccountHistory(ahq));
        }
        setPageMessage("strip.redeemed.succesfully");
        return showRedeemPrepaidStrip();
    }

    public Resolution searchAccount() {
        return retrieveAccount();
    }

    public Resolution cancelEditUnitCredit() {
        return searchAccount();
    }

    public Resolution showTransactionHistory() {
        checkPermissions(Permissions.VIEW_ACCOUNT_HISTORY);
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DATE, 0);
        Date dateTo = now.getTime();
        now.add(Calendar.DATE, -1);
        Date dateFrom = now.getTime();
        if (getAccountHistoryQuery() == null) {
            log.warn("getAccountHistoryQuery() is null. How did we get here?. Sending user to showSearchAccount()");
            return showSearchAccount();
        }
        getAccountHistoryQuery().setDateFrom(Utils.getDateAsXMLGregorianCalendar(dateFrom));
        getAccountHistoryQuery().setDateTo(Utils.getDateAsXMLGregorianCalendar(dateTo));
        Account acc = UserSpecificCachedDataHelper.getAccount(getAccount().getAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
        Map<Integer, ProductInstance> accountsProductInstances = new HashMap<>();
        for (ServiceInstance si : acc.getServiceInstances()) {
            accountsProductInstances.put(si.getProductInstanceId(), UserSpecificCachedDataHelper.getProductInstance(si.getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN));
        }
        setProductInstanceList(new ProductInstanceList());
        getProductInstanceList().getProductInstances().addAll(accountsProductInstances.values());

        setSearchMonth(Utils.getDateAsXMLGregorianCalendar(dateTo));
        return getDDForwardResolution("/account/view_transactions.jsp");
    }
    
    public Resolution showTPGWDownloadTransactionHistory() {
        checkPermissions(Permissions.VIEW_ACCOUNT_HISTORY);
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DATE, 0);
        Date dateTo = now.getTime();
        now.add(Calendar.DATE, -1);
        Date dateFrom = now.getTime();
        if (getAccountHistoryQuery() == null) {
            log.warn("getAccountHistoryQuery() is null. How did we get here?. Sending user to showSearchAccount()");
            return showSearchAccount();
        }
        getAccountHistoryQuery().setDateFrom(Utils.getDateAsXMLGregorianCalendar(dateFrom));
        getAccountHistoryQuery().setDateTo(Utils.getDateAsXMLGregorianCalendar(dateTo));
        Account acc = UserSpecificCachedDataHelper.getAccount(getAccount().getAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
        Map<Integer, ProductInstance> accountsProductInstances = new HashMap<>();
        for (ServiceInstance si : acc.getServiceInstances()) {
            accountsProductInstances.put(si.getProductInstanceId(), UserSpecificCachedDataHelper.getProductInstance(si.getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN));
        }
        setProductInstanceList(new ProductInstanceList());
        getProductInstanceList().getProductInstances().addAll(accountsProductInstances.values());

        setSearchMonth(Utils.getDateAsXMLGregorianCalendar(dateTo));
        
        return getDDForwardResolution("/account/view_transactions_download.jsp");
    }

    public Resolution showCreateAccount() {
        checkPermissions(Permissions.CREATE_ACCOUNT);
        return getDDForwardResolution("/account/add_account.jsp");
    }

    public Resolution createAccount() {
        checkPermissions(Permissions.CREATE_ACCOUNT);
        checkCSRF();
        Account newAccount = new Account();
        newAccount.setAccountId(-1);
        newAccount.setAvailableBalanceInCents(0);
        newAccount.setCurrentBalanceInCents(0);
        Account acc = SCAWrapper.getUserSpecificInstance().createAccount(newAccount);
        setAccountQuery(new AccountQuery());
        getAccountQuery().setAccountId(acc.getAccountId());
        setPageMessage("account.created.successfully");
        return retrieveAccount();
    }
    
       public Resolution getSaleReversal() {
        getSalesQuery().setVerbosity(StSaleLookupVerbosity.SALE_LINES_DOCUMENTS);
        SalesList sl = SCAWrapper.getUserSpecificInstance().getSales(getSalesQuery());
        byte[] pdf = null;
        Sale sale;
        try {
            sale = sl.getSales().get(0);
            pdf = Utils.decodeBase64(sale.getReversalPDFBase64());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        StreamingResolution res = new StreamingResolution("application/pdf", new ByteArrayInputStream(pdf));
        res.setFilename("Smile Reversal - " + sale.getSaleId() + ".pdf");
        return res;
    }
     
    public Resolution retrieveTransactionHistory() {
        checkPermissions(Permissions.VIEW_ACCOUNT_HISTORY);

        if (getAccountHistoryQuery() == null) {
            return showTransactionHistory();
        }

        if (searchMonth != null) {
            Date searchMonthFromGCtoJava = Utils.getJavaDate(searchMonth);
            XMLGregorianCalendar xmlTo = Utils.getDateAsXMLGregorianCalendar(searchMonthFromGCtoJava);
            XMLGregorianCalendar xmlFrom = Utils.getDateAsXMLGregorianCalendar(searchMonthFromGCtoJava);
            getAccountHistoryQuery().setDateFrom(xmlFrom);
            getAccountHistoryQuery().setDateTo(xmlTo);

            Calendar now = getAccountHistoryQuery().getDateTo().toGregorianCalendar();
            getAccountHistoryQuery().getDateTo().setDay(now.getActualMaximum(GregorianCalendar.DAY_OF_MONTH));
        }

        getAccountHistoryQuery().getDateFrom().setTime(0, 0, 0);
        // Ensure to date is inclusive of the days transactions
        getAccountHistoryQuery().setDateTo(Utils.getEndOfDay(getAccountHistoryQuery().getDateTo()));
        getAccountHistoryQuery().setVerbosity(StAccountHistoryVerbosity.RECORDS);
        getAccountHistoryQuery().setResultLimit(BaseUtils.getIntProperty("global.account.history.result.limit.sep", 200));

        if (getProductInstance() != null && getProductInstance().getProductInstanceId() > 0) {
            log.debug("History query is for a specific product instance [{}]", getProductInstance().getProductInstanceId());
            ProductInstance pi = UserSpecificCachedDataHelper.getProductInstance(getProductInstance().getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN_SVC);
            for (ProductServiceInstanceMapping mapping : pi.getProductServiceInstanceMappings()) {
                log.debug("Including SI ID [{}]", mapping.getServiceInstance().getServiceInstanceId());
                getAccountHistoryQuery().getServiceInstanceIds().add(mapping.getServiceInstance().getServiceInstanceId());
            }
        }

        setAccountHistory(SCAWrapper.getUserSpecificInstance().getAccountHistory(getAccountHistoryQuery()));

        if (getAccountHistory().getResultsReturned() <= 0) {
            setPageMessage("no.records.found");
        }

        try {
            setAccountSummaryQuery(new AccountSummaryQuery());
            getAccountSummaryQuery().setAccountId(getAccountHistoryQuery().getAccountId());
            getAccountSummaryQuery().setDateFrom(getAccountHistoryQuery().getDateFrom());
            getAccountSummaryQuery().setDateTo(getAccountHistoryQuery().getDateTo());
            getAccountSummaryQuery().getServiceInstanceIds().addAll(getAccountHistoryQuery().getServiceInstanceIds());
            Date endDate = Utils.getJavaDate(getAccountSummaryQuery().getDateTo());
            Date startDate = Utils.getJavaDate(getAccountSummaryQuery().getDateFrom());
            int daysBetween = Utils.getDaysBetweenDates(startDate, endDate);

            if (daysBetween <= 1) {
                getAccountSummaryQuery().setVerbosity(StAccountSummaryVerbosity.HOURLY);
            } else if (daysBetween <= 31) {
                getAccountSummaryQuery().setVerbosity(StAccountSummaryVerbosity.DAILY);
            } else if (daysBetween > 31) {
                getAccountSummaryQuery().setVerbosity(StAccountSummaryVerbosity.MONTHLY);
            }

            setAccountSummary(SCAWrapper.getUserSpecificInstance().getAccountSummary(getAccountSummaryQuery()));
            sessionDetailBarGraph = SmileTags.getObjectAsJsonString(getAccountSummary());
        } catch (SCABusinessError e) {
            log.warn("Error occured trying to call getAccountSummary");
        }
        
        Account acc = UserSpecificCachedDataHelper.getAccount(getAccountHistoryQuery().getAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
        Map<Integer, ProductInstance> accountsProductInstances = new HashMap<>();
        for (ServiceInstance si : acc.getServiceInstances()) {
            accountsProductInstances.put(si.getProductInstanceId(), UserSpecificCachedDataHelper.getProductInstance(si.getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN));
        }
        setProductInstanceList(new ProductInstanceList());
        getProductInstanceList().getProductInstances().addAll(accountsProductInstances.values());

        return getDDForwardResolution("/account/view_transactions.jsp");
    }
    
     public Resolution retrieveTPGWTransactionHistoryDownload() {
       checkPermissions(Permissions.VIEW_ACCOUNT_HISTORY);

        if (getAccountHistoryQuery() == null) {
            return showTransactionHistory();
        }

        if (searchMonth != null) {
            Date searchMonthFromGCtoJava = Utils.getJavaDate(searchMonth);
            XMLGregorianCalendar xmlTo = Utils.getDateAsXMLGregorianCalendar(searchMonthFromGCtoJava);
            XMLGregorianCalendar xmlFrom = Utils.getDateAsXMLGregorianCalendar(searchMonthFromGCtoJava);
            getAccountHistoryQuery().setDateFrom(xmlFrom);
            getAccountHistoryQuery().setDateTo(xmlTo);

            Calendar now = getAccountHistoryQuery().getDateTo().toGregorianCalendar();
            getAccountHistoryQuery().getDateTo().setDay(now.getActualMaximum(GregorianCalendar.DAY_OF_MONTH));
        }

        getAccountHistoryQuery().getDateFrom().setTime(0, 0, 0);
        // Ensure to date is inclusive of the days transactions
        getAccountHistoryQuery().setDateTo(Utils.getEndOfDay(getAccountHistoryQuery().getDateTo()));
        getAccountHistoryQuery().setVerbosity(StAccountHistoryVerbosity.RECORDS);
        getAccountHistoryQuery().setResultLimit(BaseUtils.getIntProperty("global.account.history.result.limit.sep", 200));

        if (getProductInstance() != null && getProductInstance().getProductInstanceId() > 0) {
            log.debug("History query is for a specific product instance [{}]", getProductInstance().getProductInstanceId());
            ProductInstance pi = UserSpecificCachedDataHelper.getProductInstance(getProductInstance().getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN_SVC);
            for (ProductServiceInstanceMapping mapping : pi.getProductServiceInstanceMappings()) {
                log.debug("Including SI ID [{}]", mapping.getServiceInstance().getServiceInstanceId());
                getAccountHistoryQuery().getServiceInstanceIds().add(mapping.getServiceInstance().getServiceInstanceId());
            }
        }

        setAccountHistory(SCAWrapper.getUserSpecificInstance().getAccountHistory(getAccountHistoryQuery()));

        if (getAccountHistory().getResultsReturned() <= 0) {
            setPageMessage("no.records.found");
        }

        try {
            setAccountSummaryQuery(new AccountSummaryQuery());
            getAccountSummaryQuery().setAccountId(getAccountHistoryQuery().getAccountId());
            getAccountSummaryQuery().setDateFrom(getAccountHistoryQuery().getDateFrom());
            getAccountSummaryQuery().setDateTo(getAccountHistoryQuery().getDateTo());
            getAccountSummaryQuery().getServiceInstanceIds().addAll(getAccountHistoryQuery().getServiceInstanceIds());
            Date endDate = Utils.getJavaDate(getAccountSummaryQuery().getDateTo());
            Date startDate = Utils.getJavaDate(getAccountSummaryQuery().getDateFrom());
            int daysBetween = Utils.getDaysBetweenDates(startDate, endDate);

            if (daysBetween <= 1) {
                getAccountSummaryQuery().setVerbosity(StAccountSummaryVerbosity.HOURLY);
            } else if (daysBetween <= 31) {
                getAccountSummaryQuery().setVerbosity(StAccountSummaryVerbosity.DAILY);
            } else if (daysBetween > 31) {
                getAccountSummaryQuery().setVerbosity(StAccountSummaryVerbosity.MONTHLY);
            }

            setAccountSummary(SCAWrapper.getUserSpecificInstance().getAccountSummary(getAccountSummaryQuery()));
            sessionDetailBarGraph = SmileTags.getObjectAsJsonString(getAccountSummary());
        } catch (SCABusinessError e) {
            log.warn("Error occured trying to call getAccountSummary");
        }
        
        Account acc = UserSpecificCachedDataHelper.getAccount(getAccountHistoryQuery().getAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
        Map<Integer, ProductInstance> accountsProductInstances = new HashMap<>();
        for (ServiceInstance si : acc.getServiceInstances()) {
            accountsProductInstances.put(si.getProductInstanceId(), UserSpecificCachedDataHelper.getProductInstance(si.getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN));
        }
        setProductInstanceList(new ProductInstanceList());
        getProductInstanceList().getProductInstances().addAll(accountsProductInstances.values());

        return getDDForwardResolution("/account/view_transactions_download.jsp");
    }

    public Resolution retrieveTransactionHistoryPrintable() {
        checkPermissions(Permissions.VIEW_ACCOUNT_HISTORY);
        getAccountHistoryQuery().getDateFrom().setTime(0, 0, 0);
        getAccountHistoryQuery().setDateTo(Utils.getEndOfDay(getAccountHistoryQuery().getDateTo()));
        setAccountHistory(SCAWrapper.getUserSpecificInstance().getAccountHistory(getAccountHistoryQuery()));
        return getDDForwardResolution("/account/view_transactions_printable.jsp");
    }
    
    public Resolution retrieveTransactionHistoryPrintableTPGW() {
        checkPermissions(Permissions.VIEW_ACCOUNT_HISTORY);
        getAccountHistoryQuery().getDateFrom().setTime(0, 0, 0);
        getAccountHistoryQuery().setDateTo(Utils.getEndOfDay(getAccountHistoryQuery().getDateTo()));
        setAccountHistory(SCAWrapper.getUserSpecificInstance().getAccountHistory(getAccountHistoryQuery()));
        return getDDForwardResolution("/account/view_transactions_downloadable.jsp");
    }
    
    public Resolution doTransactionHistoryDownloadTPGW() {
        checkPermissions(Permissions.VIEW_ACCOUNT_HISTORY);
        getAccountHistoryQuery().getDateFrom().setTime(0, 0, 0);
        getAccountHistoryQuery().setDateTo(Utils.getEndOfDay(getAccountHistoryQuery().getDateTo()));
        setAccountHistory(SCAWrapper.getUserSpecificInstance().getAccountHistory(getAccountHistoryQuery()));
        
        
        List<TransactionRecord> records = new ArrayList();
        records = getAccountHistory().getTransactionRecords();
        
        if(!records.isEmpty()) {
            Iterator<TransactionRecord> recordsIterator = records.iterator();
            TransactionRecord record = new TransactionRecord();       
            
            try {
                    String fw = "ExternalId Id, Source, Destination,Value,Total Units,UC Units,Balance,Type,Description,Start Date,Device,Status,Term Code\n";
                    
                    while(recordsIterator.hasNext()) {
                        record = recordsIterator.next(); 
                        
                        fw+= String.valueOf(record.getExtTxId()) + ",";
                        
                        fw+=record.getSource();
                        fw+=",";
                        fw+=record.getDestination();
                        fw+=",";
                        fw+=String.valueOf(record.getAmountInCents());
                        fw+=",";                        
                        fw+=String.valueOf(record.getTotalUnits());
                        fw+=",";
                        fw+=String.valueOf(record.getUnitCreditUnits());
                        fw+=",";
                        fw+=record.getTransactionType();
                        fw+=",";
                        fw+=record.getDescription();
                        fw+=",";
                        fw+=record.getStartDate().toString();
                        fw+=",";
                        fw+=record.getSourceDevice();
                        fw+=",";
                        fw+=record.getStatus();
                        fw+=",";
                        fw+=record.getTermCode();
                        fw+="\n";

                    }
                               
                    StreamingResolution res = new StreamingResolution("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", fw);
                    res.setFilename(String.valueOf(getAccountHistoryQuery().getAccountId()) + "_transactions.csv");                    
                    return res;
                    
            } catch (Exception e) {
                e.printStackTrace();
            }
        }        
        
        return getDDForwardResolution("/account/view_transactions_downloadable.jsp");
    }

    public Resolution getCustomerNameAsStream() {

        try {
            // PCB: Allow anyone to see the account holders name
            setAccount(SCAWrapper.getAdminInstance().getAccount(getAccount().getAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES));
            String prodSpecName = "";
            String jsonSpecObj;
            StringBuilder jsonSpecName = new StringBuilder();

            //this account does not have any customer associated with it.
            if (getAccount().getServiceInstances().size() <= 0) {
                throw new SCABusinessError();
            }

            Set<Integer> prodIds = new HashSet();
            int custId = 0;
            for (ServiceInstance si : getAccount().getServiceInstances()) {
                ServiceSpecification ss = NonUserSpecificCachedDataHelper.getServiceSpecification(si.getServiceSpecificationId());
                custId = si.getCustomerId();
                if (si.getStatus().equals("TD") || ss.getName().equals("SIM Card")) {
                    continue;
                }
                prodIds.add(si.getProductInstanceId());
            }
            setCustomer(UserSpecificCachedDataHelper.getCustomer(custId, StCustomerLookupVerbosity.CUSTOMER));

            //the services on this account seems to be all deactivated.
            if (prodIds.isEmpty()) {
                String msg = LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), "sep.account.xmlhttp.error.msg.deactivated.service");
                JsonObject ss = new JsonObject();
                ss.addProperty("productInstanceUserName", msg);
                JsonObject custJson = new JsonObject();
                custJson.add("customerWithProduct", ss);
                String json = SmileTags.getObjectAsJsonString(custJson);
                return new StreamingResolution("application/json", new StringReader(json));
            }

            for (Integer pid : prodIds) {
                ProductInstance pi = UserSpecificCachedDataHelper.getProductInstance(pid, StProductInstanceLookupVerbosity.MAIN);
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

            String customerName = UserSpecificCachedDataHelper.getServiceInstanceUserName(getAccount().getServiceInstances().get(0).getServiceInstanceId());
            //building: {"customerWithProduct":{"productInstanceUserName":"admin admin","productNames":"Corporate Sales"}}
            JsonObject ss = new JsonObject();
            ss.addProperty("productInstanceUserName", customerName);
            ss.addProperty("productNames", jsonSpecName.toString());
            JsonObject custJson = new JsonObject();
            custJson.add("customerWithProduct", ss);
            String json = SmileTags.getObjectAsJsonString(custJson);

            return new StreamingResolution("application/json", new StringReader(json));

        } catch (SCABusinessError se) {
            String msg = "Unknown";
            if (se.getErrorCode().equalsIgnoreCase("BM-0002")) {
                msg = LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), "sep.error.noaccount");
            }
            JsonObject ss = new JsonObject();
            ss.addProperty("productInstanceUserName", msg);
            JsonObject custJson = new JsonObject();
            custJson.add("customerWithProduct", ss);
            String json = SmileTags.getObjectAsJsonString(custJson);
            return new StreamingResolution("application/json", new StringReader(json));
        }
    }

    /**
     * Retrieve note types applicable to this entity
     *
     * @return Resolution The Stripes resolution object to tell Stripes where to
     * go next
     */
    public Resolution retrieveNoteTypes() {
        checkPermissions(Permissions.STICKY_NOTES);
        // Need to pass the notes page the info about this entity so it knows where to attach the note        
        setStickyNoteEntityIdentifier(new StickyNoteEntityIdentifier());
        getStickyNoteEntityIdentifier().setEntityId(getAccount().getAccountId());
        getStickyNoteEntityIdentifier().setEntityType("Account");
        setStickyNoteTypeList(SCAWrapper.getUserSpecificInstance().getStickyNoteTypeList(makeSCAString("Account")));
        return getDDForwardResolution("/note/view_note_types.jsp");
    }

    /**
     * Retrieve notes attached to this entity
     *
     * @return Resolution The Stripes resolution object to tell Stripes where to
     * go next
     */
    public Resolution retrieveNotes() {
        checkPermissions(Permissions.STICKY_NOTES);
        // Need to pass the notes page the info about this entity so it knows where to attach the note
        setStickyNoteEntityIdentifier(new StickyNoteEntityIdentifier());
        getStickyNoteEntityIdentifier().setEntityId(getAccount().getAccountId());
        getStickyNoteEntityIdentifier().setEntityType("Account");
        setStickyNoteList((StickyNoteList) SCAWrapper.getUserSpecificInstance().getEntitiesStickyNotes(getStickyNoteEntityIdentifier()));
        return getDDForwardResolution("/note/view_notes.jsp");
    }

    /**
     * @return the transferFromAnyAccount
     */
    public boolean isTransferFromAnyAccount() {
        return transferFromAnyAccount;
    }

    /**
     * @param transferFromAnyAccount the transferFromAnyAccount to set
     */
    public void setTransferFromAnyAccount(boolean transferFromAnyAccount) {
        this.transferFromAnyAccount = transferFromAnyAccount;
    }

    /*
     * 
     * Future Transfers and Bundle Purchases
     * 
     */
    public Resolution scheduleFutureAccountTransfer() {
        checkPermissions(Permissions.MANAGE_FUTURE_TRANSFERS);
        if (getAccount().getAccountId() < 1100000000) {
            // Not allowed to schedule management accounts unless you have high permissions
            checkPermissions(Permissions.TRANSFER_FUNDS_FROM_ANY_ACCOUNT);
        }
        if (getEvent() == null) {
            return showAccountsFutureTransfers();
        }
        getEvent().setEventType("BM");
        getEvent().setEventSubType("SCHEDULED_TRANSFER");
        getEvent().setEventData(getAccount().getAccountId() + "|" + futureTransferDestinationAccountId + "|" + (majorCurrencyUnitsToTransferInFuture * 100) + "|" + getUser());
        getEvent().setEventKey(String.valueOf(getAccount().getAccountId()));
        getEvent().getDate().setTime(5, 0, 0);
        Date eventDate = Utils.getJavaDate(getEvent().getDate());
        if (eventDate.before(new Date())) {
            localiseErrorAndAddToGlobalErrors("past.date.not.allowed");
            return showAccountsFutureTransfers();
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(eventDate);
        for (int i = 0; i <= repeats; i++) {
            getEvent().setDate(Utils.getDateAsXMLGregorianCalendar(cal.getTime()));
            SCAWrapper.getUserSpecificInstance().createEvent(getEvent());
            switch (repeatCycle) {
                case "d":
                    cal.add(Calendar.DATE, 1);
                    break;
                case "w":
                    cal.add(Calendar.DATE, 7);
                    break;
                case "m":
                    cal.add(Calendar.MONTH, 1);
                    break;
                case "y":
                    cal.add(Calendar.YEAR, 1);
                    break;
                case "t":
                    cal.add(Calendar.DATE, 30);
                    break;
            }
        }

        setPageMessage("future.transfer.scheduled.successfully");
        return showAccountsFutureTransfers();
    }

    public Resolution deleteFutureAccountTransfer() {
        checkPermissions(Permissions.MANAGE_FUTURE_TRANSFERS);

        SCAWrapper.getUserSpecificInstance().deleteFutureEvent(makeSCALong(eventToDelete));
        setPageMessage("future.transfer.deleted.successfully");
        return showAccountsFutureTransfers();
    }

    public Resolution showAccountsFutureTransfers() {
        checkPermissions(Permissions.VIEW_ACCOUNT);

        ServiceInstanceQuery siq = new ServiceInstanceQuery();
        siq.setAccountId(getAccount().getAccountId());
        siq.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);

        ServiceInstanceList siList = SCAWrapper.getUserSpecificInstance().getServiceInstances(siq);
        for (ServiceInstance si : siList.getServiceInstances()) {
            if (si.getServiceSpecificationId() >= 1000) {
                localiseErrorAndAddToGlobalErrors("special.accounts.not.allowed.future.transfers");
                return retrieveAccount();
            }
        }

        EventQuery q = new EventQuery();
        q.setEventType("BM");
        q.setEventSubType("SCHEDULED_TRANSFER");
        q.setDateFrom(Utils.getDateAsXMLGregorianCalendar(new Date()));
        q.setEventKey(String.valueOf(getAccount().getAccountId()));
        q.setResultLimit(200);
        setEventList(SCAWrapper.getUserSpecificInstance().getEvents(q));
        return getDDForwardResolution("/account/view_future_transfers.jsp");
    }
    private long paidByAccountId;

    public long getPaidByAccountId() {
        return paidByAccountId;
    }

    public void setPaidByAccountId(long paidByAccountId) {
        this.paidByAccountId = paidByAccountId;
    }

    public Resolution scheduleFutureUCPurchase() {
        checkPermissions(Permissions.MANAGE_FUTURE_UC_PURCHASES);
        checkPermissions(Permissions.PURCHASE_UNIT_CREDIT);
        if (getAccount().getAccountId() < 1100000000) {
            // Not allowed to schedule management accounts unless you have high permissions
            checkPermissions(Permissions.TRANSFER_FUNDS_FROM_ANY_ACCOUNT);
        }
        if (getEvent() == null || unitCreditSpecIdToSchedule <= 0) {
            return showAccountsFutureUCPurchases();
        }
        getEvent().setEventType("BM");
        getEvent().setEventSubType("SCHEDULED_UNIT_CREDIT");
        getEvent().setEventData(getAccount().getAccountId() + "|" + unitCreditSpecIdToSchedule + "|" + getUser() + "|" + getPaidByAccountId());
        getEvent().setEventKey(String.valueOf(getAccount().getAccountId()));

        getEvent().getDate().setTime(5, 5, 0);
        Date eventDate = Utils.getJavaDate(getEvent().getDate());
        if (eventDate.before(new Date())) {
            localiseErrorAndAddToGlobalErrors("past.date.not.allowed");
            return showAccountsFutureUCPurchases();
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(eventDate);
        for (int i = 0; i <= repeats; i++) {
            getEvent().setDate(Utils.getDateAsXMLGregorianCalendar(cal.getTime()));
            SCAWrapper.getUserSpecificInstance().createEvent(getEvent());
            switch (repeatCycle) {
                case "d":
                    cal.add(Calendar.DATE, 1);
                    break;
                case "w":
                    cal.add(Calendar.DATE, 7);
                    break;
                case "m":
                    cal.add(Calendar.MONTH, 1);
                    break;
                case "y":
                    cal.add(Calendar.YEAR, 1);
                    break;
                case "t":
                    cal.add(Calendar.DATE, 30);
                    break;
            }
        }
        setPageMessage("future.uc.purchase.scheduled.successfully");
        return showAccountsFutureUCPurchases();
    }

    public Resolution deleteFutureUCPurchase() {
        checkPermissions(Permissions.MANAGE_FUTURE_UC_PURCHASES);

        SCAWrapper.getUserSpecificInstance().deleteFutureEvent(makeSCALong(eventToDelete));
        setPageMessage("future.uc.purchase.deleted.successfully");
        return showAccountsFutureUCPurchases();
    }

    public Resolution showAccountsFutureUCPurchases() {
        checkPermissions(Permissions.VIEW_ACCOUNT);
        EventQuery q = new EventQuery();
        q.setEventType("BM");
        q.setEventSubType("SCHEDULED_UNIT_CREDIT");
        q.setDateFrom(Utils.getDateAsXMLGregorianCalendar(new Date()));
        q.setEventKey(String.valueOf(getAccount().getAccountId()));
        q.setResultLimit(200);
        setEventList(SCAWrapper.getUserSpecificInstance().getEvents(q));
        for (Event e : getEventList().getEvents()) {
            // Add last pipe for paying account if it is not there (for backwards compatability)
            String[] split = e.getEventData().split("\\|");
            if (split.length == 3) {
                e.setEventData(e.getEventData() + "|" + split[0]);
            }
        }
        setUnitCreditSpecificationList(new UnitCreditSpecificationList());
        UnitCreditSpecificationQuery ucsq = new UnitCreditSpecificationQuery();
        ucsq.setUnitCreditSpecificationId(-1);
        ucsq.setVerbosity(StUnitCreditSpecificationLookupVerbosity.MAIN);
        UnitCreditSpecificationList allUCs = (SCAWrapper.getUserSpecificInstance().getUnitCreditSpecifications(ucsq));
        for (UnitCreditSpecification ucs : allUCs.getUnitCreditSpecifications()) {
            if (Utils.isBetween(new Date(), ucs.getAvailableFrom(), ucs.getAvailableTo())) {
                if (isAllowed(ucs.getPurchaseRoles())) {
                    getUnitCreditSpecificationList().getUnitCreditSpecifications().add(ucs);
                }
            }
        }

        setPaidByAccountId(getAccount().getAccountId());
        return getDDForwardResolution("/account/view_future_uc_purchases.jsp");
    }
    private long eventToDelete;
    private int unitCreditSpecIdToSchedule;
    private long futureTransferDestinationAccountId;
    private double majorCurrencyUnitsToTransferInFuture;
    private String repeatCycle;
    private int repeats;

    public int getRepeats() {
        return repeats;
    }

    public void setRepeats(int repeats) {
        this.repeats = repeats;
    }

    public java.lang.String getRepeatCycle() {
        return repeatCycle;
    }

    public void setRepeatCycle(java.lang.String repeatCycle) {
        this.repeatCycle = repeatCycle;
    }

    public int getUnitCreditSpecIdToSchedule() {
        return unitCreditSpecIdToSchedule;
    }

    public void setUnitCreditSpecIdToSchedule(int unitCreditSpecIdToSchedule) {
        this.unitCreditSpecIdToSchedule = unitCreditSpecIdToSchedule;
    }

    public long getFutureTransferDestinationAccountId() {
        return futureTransferDestinationAccountId;
    }

    public void setFutureTransferDestinationAccountId(long futureTransferDestinationAccountId) {
        this.futureTransferDestinationAccountId = futureTransferDestinationAccountId;
    }

    public double getMajorCurrencyUnitsToTransferInFuture() {
        return majorCurrencyUnitsToTransferInFuture;
    }

    public void setMajorCurrencyUnitsToTransferInFuture(double majorCurrencyUnitsToTransferInFuture) {
        this.majorCurrencyUnitsToTransferInFuture = majorCurrencyUnitsToTransferInFuture;
    }

    public long getEventToDelete() {
        return eventToDelete;
    }

    public void setEventToDelete(long eventToDelete) {
        this.eventToDelete = eventToDelete;
    }

    @DontValidate
    public Resolution getAccountJSON() {
        getAccountQuery().setVerbosity(StAccountLookupVerbosity.ACCOUNT);
        Account a = SCAWrapper.getUserSpecificInstance().getAccount(getAccountQuery());
        return getJSONResolution(a);
    }

    private List<Integer> availabilityBitIndexOn;

    public List<Integer> getAvailabilityBitIndexOn() {
        return availabilityBitIndexOn;
    }

    public void setAvailabilityBitIndexOn(List<Integer> availabilityBitIndexOn) {
        this.availabilityBitIndexOn = availabilityBitIndexOn;
    }

    private String behaviourHint;

    public java.lang.String getBehaviourHint() {
        return behaviourHint;
    }

    public void setBehaviourHint(java.lang.String behaviourHint) {
        this.behaviourHint = behaviourHint;
    }

    public Resolution showEditUnitCreditInstance() {
        checkPermissions(Permissions.EDIT_UNIT_CREDIT_INSTANCE);
        setUnitCreditInstance(this.getUnitCreditInstance(getUnitCreditInstance().getUnitCreditInstanceId()));
        if (getUnitCreditInstance() == null) {
            // UC has expired in the mean time
            return retrieveAccount();
        }

        behaviourHint = Utils.getValueFromCRDelimitedAVPString(getUnitCreditInstance().getInfo(), "BehaviourHint");

        String avail = Utils.getValueFromCRDelimitedAVPString(getUnitCreditInstance().getInfo(), "Availability");
        long availability;
        if (avail == null) {
            availability = Long.MAX_VALUE;
        } else {
            availability = Long.parseLong(avail);
        }
        availabilityBitIndexOn = new ArrayList();
        for (int i = 0; i < 40; i++) {
            if ((availability & (long) Math.pow(2, i)) != 0) {
                availabilityBitIndexOn.add(i);
            }
        }
        return getDDForwardResolution("/account/edit_unit_credit_instance.jsp");
    }

    public Resolution updateUnitCreditInstance() {
        checkCSRF();
        checkPermissions(Permissions.EDIT_UNIT_CREDIT_INSTANCE);
        long availability = 0;
        for (int bitIndexIsOn : availabilityBitIndexOn) {
            availability += Math.pow(2, bitIndexIsOn);
        }
        UnitCreditInstance uci = getUnitCreditInstance(getUnitCreditInstance().getUnitCreditInstanceId());

        UnitCreditSpecification ucs = NonUserSpecificCachedDataHelper.getUnitCreditSpecification(uci.getUnitCreditSpecificationId());

        if (ucs.getWrapperClass().equals("DustUnitCredit") && !uci.getExpiryDate().equals(getUnitCreditInstance().getExpiryDate())) {
            checkPermissions(Permissions.EDIT_UNLIMITED_UNIT_CREDIT_EXPIRY);
        }

        uci.setAccountId(getUnitCreditInstance().getAccountId());
        uci.setStartDate(getUnitCreditInstance().getStartDate());
        uci.setExpiryDate(getUnitCreditInstance().getExpiryDate());
        uci.setEndDate(getUnitCreditInstance().getEndDate());
        uci.setComment(getUnitCreditInstance().getComment());
        uci.setProductInstanceId(getUnitCreditInstance().getProductInstanceId());

        String currentBehaviourHint = Utils.getValueFromCRDelimitedAVPString(uci.getInfo(), "BehaviourHint");

        if ((currentBehaviourHint != null && behaviourHint != null && !currentBehaviourHint.equals(behaviourHint))
                || (currentBehaviourHint == null && behaviourHint != null && !behaviourHint.isEmpty())) {
            checkPermissions(Permissions.EDIT_UNIT_CREDIT_HINT);
        }

        uci.setInfo(Utils.setValueInCRDelimitedAVPString(uci.getInfo(), "BehaviourHint", behaviourHint));
        uci.setInfo(Utils.setValueInCRDelimitedAVPString(uci.getInfo(), "Availability", String.valueOf(availability)));
        log.debug("Setting availability to [{}]", availability);
        SCAWrapper.getUserSpecificInstance().modifyUnitCredit(uci);
        setPageMessage("unit.credit.instance.modified.successfully");
        return retrieveAccount();
    }

    public Resolution showSplitUnitCreditInstance() {
        checkPermissions(Permissions.SPLIT_UNIT_CREDIT_INSTANCE_WITHIN_ORG);
        setUnitCreditInstance(getUnitCreditInstance(getUnitCreditInstance().getUnitCreditInstanceId()));
        return getDDForwardResolution("/account/split_unit_credit_instance.jsp");
    }

    public Resolution splitUnitCreditInstance() {
        checkCSRF();
        setUnitCreditInstance(this.getUnitCreditInstance(getSplitUnitCreditData().getUnitCreditInstanceId()));
        checkPermissions(Permissions.SPLIT_UNIT_CREDIT_INSTANCE_WITHIN_ORG);
        SCAWrapper.getUserSpecificInstance().splitUnitCredit(getSplitUnitCreditData());
        setPageMessage("unit.credit.instance.split.successfully");
        setAccount(new Account());
        getAccount().setAccountId(getSplitUnitCreditData().getTargetAccountId());
        return retrieveAccount();
    }

    public Resolution getUnitCreditSale() {
        if (getUnitCreditInstance().getSaleLineId() == 0) {
            setPageMessage("unit.credit.has.no.sale");
            return retrieveAccount();
        }
        ForwardResolution forward = (ForwardResolution) getDDForwardResolution(SalesActionBean.class, "showSale");
        forward.addParameter("salesQuery.saleLineId", getUnitCreditInstance().getSaleLineId());
        return forward;
    }

    public Resolution deleteUnitCreditInstance() {
        checkPermissions(Permissions.DELETE_UNIT_CREDIT_INSTANCE);
        checkCSRF();
        if (notConfirmed()) {
            return confirm();
        }
        // Set units to -1 to have BM set it to zero and put the value back into airtime
        getUnitCreditInstance().setCurrentUnitsRemaining(-1.0d);
        try {
            SCAWrapper.getUserSpecificInstance().modifyUnitCredit(getUnitCreditInstance());
            setPageMessage("unit.credit.deleted");
        } catch (Exception e) {
            retrieveAccount();
            throw e;
        }
        return retrieveAccount();
    }

    public Resolution showTransferGraph() {
        checkPermissions(Permissions.TRANSFER_GRAPH);
        setTransferGraphQuery(new TransferGraphQuery());
        getTransferGraphQuery().setRecursions(3);
        getTransferGraphQuery().setStartDate(Utils.getDateAsXMLGregorianCalendar(new Date()));
        getTransferGraphQuery().setEndDate(Utils.getDateAsXMLGregorianCalendar(new Date()));
        return getDDForwardResolution("/account/transfer_graph.jsp");
    }

    public Resolution doTransferGraph() {
        checkPermissions(Permissions.TRANSFER_GRAPH);
        log.debug("Graph root account id is [{}]", getTransferGraphQuery().getRootAccountId());
        getTransferGraphQuery().setEndDate(Utils.getEndOfDay(getTransferGraphQuery().getEndDate()));
        log.debug("Graph end is [{}]", getTransferGraphQuery().getEndDate());
        setTransferGraph(SCAWrapper.getUserSpecificInstance().getTransferGraph(getTransferGraphQuery()));
        log.debug("Transfer graph: [{}]", getTransferGraph().getGraph());

        GraphViz gv = new GraphViz();
        byte[] pdf = gv.getGraph(getTransferGraph().getGraph(), "pdf");
        StreamingResolution res = new StreamingResolution("application/pdf", new ByteArrayInputStream(pdf));
        res.setFilename(getTransferGraphQuery().getRootAccountId() + ".pdf");
        return res;
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

    private void provisionUnitCreditViaSale(PurchaseUnitCreditRequest purchaseUnitCreditRequest) {
//        Sale sale = new Sale();
//        sale.setRecipientAccountId(purchaseUnitCreditRequest.getAccountId());
//        sale.setRecipientCustomerId(repeats);
//        sale.setRecipientOrganisationId(repeats);
//        sale.setSalesPersonCustomerId(getUserCustomerIdFromSession());
//        sale.set

        throw new UnsupportedOperationException("Use make sale to provision a bundle");
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

        if (getProductInstance() != null && getProductInstance().getProductInstanceId() > 0) {
            prodIds.add(getProductInstance().getProductInstanceId());
        } else {
            setAccount(UserSpecificCachedDataHelper.getAccount(getAccountQuery().getAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES));
            for (ServiceInstance si : getAccount().getServiceInstances()) {
                prodIds.add(si.getProductInstanceId());
            }
        }

        //the services on this account seems to be all deactivated.
        if (prodIds.isEmpty()) {
            log.debug("The services in this account/product seems to be all deactivated");
            return showTransactionHistory();
        }

        if (prodIds.size() > 1) {
            log.debug("The account has more than one product instances and a specific one needs to be selected");
            localiseErrorAndAddToGlobalErrors("select.product.instance.for.tnf");
            return showTransactionHistory();
        }

        ProductInstance pi = UserSpecificCachedDataHelper.getProductInstance((int) prodIds.toArray()[0], StProductInstanceLookupVerbosity.MAIN);

        getAccountHistoryQuery().getDateFrom().setTime(0, 0, 0);
        getAccountHistoryQuery().setDateTo(Utils.getEndOfDay(getAccountHistoryQuery().getDateTo()));

        TNFQuery tnfQuery = new TNFQuery();
        tnfQuery.setTNFMethod("Complex");
        tnfQuery.setTimeRange(new TimeRange());
        tnfQuery.getTimeRange().setStartTime(getAccountHistoryQuery().getDateFrom());
        tnfQuery.getTimeRange().setEndTime(Utils.getBeginningOfNextDay(getAccountHistoryQuery().getDateTo()));
        AttributeNames at = new AttributeNames();
        at.getAttributeName().add("COUNTRY_CODE");
        at.getAttributeName().add("PRODUCT_NAME");
        at.getAttributeName().add("SERVICE_NAME");
        tnfQuery.setAttributeNames(at);

        setTimeRange(tnfQuery.getTimeRange());

        Date startDateMustBeforeEndDate = Utils.getJavaDate(tnfQuery.getTimeRange().getStartTime());
        Date endDateMustAfterStartDate = Utils.getJavaDate(tnfQuery.getTimeRange().getEndTime());

        int daysBetween = Utils.getDaysBetweenDates(startDateMustBeforeEndDate, endDateMustAfterStartDate);

        if (daysBetween <= 0 || daysBetween == 1) {
            tnfQuery.getTimeRange().setGranularity("GRANULARITY_HOUR");
            if (isDateOutsideTNFRententionPeriodForGranularity(startDateMustBeforeEndDate, "GRANULARITY_HOUR")) {
                tnfQuery.getTimeRange().setGranularity("GRANULARITY_MONTH");
            }
        } else if (daysBetween <= 31) {
            tnfQuery.getTimeRange().setGranularity("GRANULARITY_DAY");
            if (isDateOutsideTNFRententionPeriodForGranularity(startDateMustBeforeEndDate, "GRANULARITY_DAY")) {
                tnfQuery.getTimeRange().setGranularity("GRANULARITY_MONTH");
            }
        } else {
            tnfQuery.getTimeRange().setGranularity("GRANULARITY_MONTH");
            if (isDateOutsideTNFRententionPeriodForGranularity(startDateMustBeforeEndDate, "GRANULARITY_MONTH")) {
                Account acc = UserSpecificCachedDataHelper.getAccount(getAccountHistoryQuery().getAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
                Map<Integer, ProductInstance> accountsProductInstances = new HashMap<>();
                for (ServiceInstance si : acc.getServiceInstances()) {
                    accountsProductInstances.put(si.getProductInstanceId(), UserSpecificCachedDataHelper.getProductInstance(si.getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN));
                }
                setProductInstanceList(new ProductInstanceList());
                getProductInstanceList().getProductInstances().addAll(accountsProductInstances.values());

                setPageMessage("no.tnf.search.rangeoutside.rentention.period");
                return getDDForwardResolution("/account/view_transactions.jsp");
            }
        }
        
        //Uncomment the line below, its for Dev testing ONLY
        //tnfQuery.setLogicalSimId(getTNFLogicalID());
        log.debug("LogicalId for this productInstance is: {}", pi.getLogicalId());
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
            ComplexResponse cr = SEPJAXBHelper.getPIsReportsAndAttributes(data.getTNFXmlData());
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

        Account acc = UserSpecificCachedDataHelper.getAccount(getAccountHistoryQuery().getAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
        Map<Integer, ProductInstance> accountsProductInstances = new HashMap<>();
        for (ServiceInstance si : acc.getServiceInstances()) {
            accountsProductInstances.put(si.getProductInstanceId(), UserSpecificCachedDataHelper.getProductInstance(si.getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN));
        }
        setProductInstanceList(new ProductInstanceList());
        getProductInstanceList().getProductInstances().addAll(accountsProductInstances.values());

        return getDDForwardResolution("/account/view_transactions.jsp");
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
    
    private boolean UCCanBeSoldWhenSpecIDExist(UnitCreditSpecification ucs) {      
        String val = Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "CanBeSoldWhenSpecIDExist");
        return !(val == null || val.length() <= 0);
    } 
    
     public Resolution showScheduleTransactionHistory() {
        checkPermissions(Permissions.VIEW_ACCOUNT_HISTORY);
        return getDDForwardResolution("/account/add_account_history_schedule.jsp");
    }

    public Resolution scheduleTransactionHistorySend() {
        checkPermissions(Permissions.VIEW_ACCOUNT_HISTORY);
        checkCSRF();
        Account acc = SCAWrapper.getAdminInstance().getAccount(getAccount().getAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
        
        if(getParameter("emailAddress").trim().isEmpty() || getParameter("frequency").equals("-1")){
            localiseErrorAndAddToGlobalErrors("Error", "Invalid setup data. Please enter all required info.");
            return showScheduleTransactionHistory();
        }
        
        boolean hasActiveService=false;
        for(ServiceInstance si: acc.getServiceInstances()) {
            if(si.getStatus().equalsIgnoreCase("AC")) {
                hasActiveService=true;                        
            }
        }
        
        if(!hasActiveService) {
            localiseErrorAndAddToGlobalErrors("Error", "No active service(s) found on this account.");
            return showScheduleTransactionHistory();
        }
        
        com.smilecoms.commons.sca.direct.bm.ScheduledAccountHistory createSchedule = new com.smilecoms.commons.sca.direct.bm.ScheduledAccountHistory();
        createSchedule.setAccountId(acc.getAccountId());
        createSchedule.setCreatedByProfileId(getUserCustomerIdFromSession());
        createSchedule.setEmailTo(getParameter("emailAddress").trim());
        createSchedule.setFrequency(getParameter("frequency").trim());
        
        SCAWrapper.getUserSpecificInstance().createScheduledAccountHistory_Direct(createSchedule);
        setPageMessage("successfully.scheduled.account.history");
        return showScheduleTransactionHistory();        
    } 
}
