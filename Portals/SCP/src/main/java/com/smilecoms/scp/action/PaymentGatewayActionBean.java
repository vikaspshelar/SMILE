/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.scp.action;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.localisation.LocalisationHelper;
import com.smilecoms.commons.sca.Account;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.SCABusinessError;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.Sale;
import com.smilecoms.commons.sca.StSaleLookupVerbosity;
import com.smilecoms.commons.stripes.SmileActionBean;
import com.smilecoms.commons.tags.SmileTags;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.scp.helpers.paymentgateway.IPGWTransactionData;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Set;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;

/**
 *
 * @author sabelo
 */
public class PaymentGatewayActionBean extends SmileActionBean {

    private IPGWTransactionData PGWTransactionData;
    private String TPGWPartnerCode;

    public IPGWTransactionData getPGWTransactionData() {
        return PGWTransactionData;
    }

    public void setPGWTransactionData(IPGWTransactionData PGWTransactionData) {
        this.PGWTransactionData = PGWTransactionData;
    }

    public String getTPGWPartnerCode() {
        return TPGWPartnerCode;
    }

    @DefaultHandler
    public Resolution processBankTransaction() {
        log.debug("Entering processBankTransaction()");
        String AccessTransId = "";
        try {

            String orderId = "";
            
            for (Enumeration en = getRequest().getParameterNames(); en.hasMoreElements();) {
                String name = (String) en.nextElement();                
                if (name.equalsIgnoreCase("saleId")) {
                    String value = getRequest().getParameter(name);
                    orderId = value;
                    break;
                }
                if (name.equalsIgnoreCase("OrderID")) {
                    String value = getRequest().getParameter(name);
                    orderId = value;
                }
                if (name.equalsIgnoreCase("pesapal_merchant_reference")) {
                    String value = getRequest().getParameter(name);
                    orderId = value;
                }
                if (name.equalsIgnoreCase("TransactionID")) {
                    String value = getRequest().getParameter(name);
                    orderId = value;
                }
            }
            log.debug("Going to get account details by clonning AccountActionBean");
            AccountActionBean aab = cloneToActionBean(AccountActionBean.class);
            aab.retrieveAllUserServicesAccounts();

            setAccountList(aab.getAccountList());
            setCustomer(aab.getCustomer());
            setProductInstanceList(aab.getProductInstanceList());
            setServiceInstanceList(aab.getServiceInstanceList());

            if (!isCustomerAllowedToMakePayment(getCustomer())) {
                log.warn("Payment gateway functionality is restricted. Requesting customer is {}", getCustomer().getSSOIdentity());
                return getDDForwardResolution("/permissions/gateway_transaction_denied.jsp");
            }

            if (orderId == null || orderId.isEmpty()) {
                log.debug("Parameters to do a proper search are not included or empty, not going to process this transaction any further.");
                setPageMessage("scp.payment.gateway.transaction.doesnot.exist");
                return getDDForwardResolution(AccountActionBean.class, "retrieveAllUserServicesAccounts", "scp.payment.gateway.transaction.doesnot.exist");
            }

            Sale paymentGatewaySale = null;

            try {
                paymentGatewaySale = SCAWrapper.getUserSpecificInstance().getSale(Integer.parseInt(orderId), StSaleLookupVerbosity.SALE_LINES);
            } catch (SCABusinessError ex) {
                log.warn("Tried to search by SaleId but failed, Reason: [{}]", ex.toString());
                throw ex;
            }

            if (paymentGatewaySale == null) {
                log.warn("Seems like transaction does not exist as paymentGatewaySale is null, redirecting customer to retrieveAllUserServicesAccounts");
                return getDDForwardResolution(AccountActionBean.class, "retrieveAllUserServicesAccounts", "scp.payment.gateway.transaction.doesnot.exist");
            }

            int saleId = paymentGatewaySale.getSaleId();

            /*Check if request is not too old to proccess*/
            Date createdDate = Utils.getJavaDate(paymentGatewaySale.getSaleDate());
            Calendar cal = Calendar.getInstance();
            cal.setTime(createdDate);
            cal.add(Calendar.HOUR, 2);
            Date newExpiryDate = cal.getTime();

            if (newExpiryDate.before(new Date())) {
                return getDDForwardResolution(AccountActionBean.class, "retrieveAllUserServicesAccounts");
            }

            long recipeintAccount = paymentGatewaySale.getRecipientAccountId();


            /*Check if allowed to access the transaction data*/
            boolean isAllowedAccessToAccountEntity = false;
            for (Account acc : getAccountList().getAccounts()) {
                if (acc.getAccountId() == recipeintAccount) {
                    isAllowedAccessToAccountEntity = true;
                    break;
                }
            }
            if (!isAllowedAccessToAccountEntity) {
                log.warn("Customer [{}] requested transaction details for transaction ID [{}] but has no access to account [{}]. Not going to process request further.", new Object[]{getCustomer().getSSOIdentity(), saleId, recipeintAccount});
                return getDDForwardResolution(AccountActionBean.class, "retrieveAllUserServicesAccounts");
            }
            
            setSale(paymentGatewaySale);

        } catch (Exception e) {

            if (e instanceof SCABusinessError) {
                throw e;
            }
            log.warn("Encountered an irrecoverable error, what happened: {}", e.toString());
            AccountActionBean aab = cloneToActionBean(AccountActionBean.class);
            aab.retrieveAllUserServicesAccounts();
            setAccountList(aab.getAccountList());

            setCustomer(aab.getCustomer());
            setProductInstanceList(aab.getProductInstanceList());
            setServiceInstanceList(aab.getServiceInstanceList());
        }
        log.debug("Exiting processBankTransaction()");
        return getDDForwardResolution("/payment_gateway/gateway_transaction_summary.jsp");
    }

    public Resolution getPaymentGatewayTransactionStatusAsJSON() {
        log.debug("Entering getPaymentGatewayTransactionStatusAsJSON");
        Sale paymentGatewaySale = null;
        try {
            paymentGatewaySale = SCAWrapper.getUserSpecificInstance().getSale(getSale().getSaleId(), StSaleLookupVerbosity.SALE_LINES);
            String titleMessage;//Helps to update the title on the page dynamically
            if (paymentGatewaySale.getStatus().equals("PD")) {
                titleMessage = LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), "scp.payment.gateway.success.page");
            } else {
                titleMessage = LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), "scp.payment.gateway.failure.page");
            }
            paymentGatewaySale.setPaymentGatewayResponse(Utils.setValueInCRDelimitedAVPString(
                    paymentGatewaySale.getPaymentGatewayResponse() == null ? "" : paymentGatewaySale.getPaymentGatewayResponse(), "titleMessage", titleMessage));
        } catch (Exception ex) {
            log.warn("Payment gateway search failed. Reason: [{}]", ex.toString());
        }

        if (paymentGatewaySale == null) {
            paymentGatewaySale = new Sale();
            paymentGatewaySale.setStatus("RV");
            String titleMessage = LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), "scp.payment.gateway.failure.page");
            paymentGatewaySale.setPaymentGatewayResponse(Utils.setValueInCRDelimitedAVPString("", "titleMessage", titleMessage));
        }
        String json = SmileTags.getObjectAsJsonString(paymentGatewaySale);
        log.debug("Exiting getPaymentGatewayTransactionStatusAsJSON");
        return new StreamingResolution("application/json", new StringReader(json));
    }

    public Resolution processTPGWPartnerTransaction() {
        //Not implemented yet
        return getDDForwardResolution("/payment_gateway/tpgw_transaction_summary.jsp");
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
}
