/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos.paymentgateway;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.paymentgateway.Pesapal;
import com.smilecoms.commons.paymentgateway.helpers.PaymentGatewayHelper;
import com.smilecoms.commons.platform.PlatformEventManager;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.EventList;
import com.smilecoms.commons.sca.EventQuery;
import com.smilecoms.commons.sca.JiraField;
import com.smilecoms.commons.sca.NewTTIssue;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.pos.POSManager;
import com.smilecoms.pos.db.model.Sale;
import com.smilecoms.pos.db.op.DAO;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;

/**
 *
 * @author sabza
 */
public class PesapalPaymentGateway extends AbstractPaymentGateway implements PaymentGatewayPlugin {

    private static final String PESAPAL_ISUP_CACHE_KEY = "pesapal_isup_error_key";
    private static int ISUP_FAIL_COUNTER = 0;
    private final DecimalFormat df = new DecimalFormat("#.00");

    @Override
    public PaymentGatewayResult getPaymentGatewayResult(Sale dbSale) {
        PaymentGatewayResult res = new PaymentGatewayResult();
        if (dbSale.getPaymentGatewayPollCount() < BaseUtils.getIntProperty("env.pos.paymentgateway.pesapal.firstquery.delay.count", 3)) {
            //Dont check transaction status early as we might get FAILED, we rather wait. See HBT-3571
            StringBuilder avpBuilder = new StringBuilder();
            avpBuilder.append("REQUEST_DELAYED");
            res.setTryAgainLater(true);
            res.setGatewayResponse(avpBuilder.toString());
            res.setSuccess(false);
            return res;

        }
        try {

            String transactionDetails;
            try {
                Pesapal pesapal = new Pesapal();
                //http://www.paysmile.co.tz/index.php?option=com_users&task=paybill.checkStatus&reference=19
                statusQueryURL = statusQueryURL.replaceAll("#", "=");
                String sQueryURL = statusQueryURL + String.valueOf(dbSale.getSaleId());
                transactionDetails = pesapal.getTransactionDetails(sQueryURL);
                log.debug("HTTP client request returned successfully for getTransactionDetails. Transaction details returned[{}]", transactionDetails);
            } catch (Exception ex) {
                log.warn("System error in Pesapal plugin: ", ex);
                res.setGatewayResponse(ex.toString());
                res.setSuccess(false);
                res.setTryAgainLater(true);
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "Error in PesaPal PaymentGateway plugin: " + ex.toString());
                PesapalPaymentGateway.ISUP_FAIL_COUNTER++;
                
                boolean isup = PesapalPaymentGateway.ISUP_FAIL_COUNTER <= BaseUtils.getIntProperty("env.pos.paymentgateway.isup.fail.counter", 5);
                publishGatewayInAvailabilityNotice(getName(), isup, PESAPAL_ISUP_CACHE_KEY, ex.toString());
                return res;
            }
            PesapalPaymentGateway.ISUP_FAIL_COUNTER = 0;
            updateGatewayStatus("env.scp.pesapal.partner.integration.config", true, getName());

            //reference=19&status=failed&reason=MPESA
            String returnedStatus = "unknown";
            String paymentGate = "unknown";
            String transactionRef = String.valueOf(dbSale.getSaleId());
            String orderId = String.valueOf(dbSale.getSaleId());
            String amount = "0";

            //reference=11024245&amount=100000&pesapal_transaction_tracking_id=&status=failed&reason=
            if (transactionDetails.contains("&")) {
                String[] splittedResponseBits = transactionDetails.split("&");
                for (String tkn : splittedResponseBits) {
                    String[] bts = tkn.split("=");
                    String val = "";
                    if (bts.length == 2) {
                        val = bts[1];
                    }
                    log.debug("Token field to process is {} and its value is {}", bts[0], val);
                    switch (bts[0]) {
                        case "reference":
                            orderId = val;
                            break;
                        case "status":
                            returnedStatus = val;
                            break;
                        case "reason":
                            paymentGate = val;
                            break;
                        case "pesapal_transaction_tracking_id":
                            transactionRef = val;
                            break;
                        case "amount":
                            amount = val;
                            break;
                        default:
                            log.warn("Unknown parameter from Pesapal response");
                            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "PesapalPaymentGateway has a new unsupported field [" + transactionDetails + "]");
                    }
                }
            }

            StringBuilder avpBuilder = new StringBuilder();

            avpBuilder.append("orderId=");
            avpBuilder.append(orderId);
            avpBuilder.append("\r\n");

            avpBuilder.append("amount=");
            avpBuilder.append(dbSale.getSaleTotalCentsIncl().divide(new BigDecimal(100)));// The request has been signed, we can safely use our origanal amount
            avpBuilder.append("\r\n");

            avpBuilder.append("dateTime=");
            avpBuilder.append(dbSale.getSaleDateTime());
            avpBuilder.append("\r\n");

            avpBuilder.append("status=");
            avpBuilder.append(returnedStatus);
            avpBuilder.append("\r\n");

            //Helps to keep track of the transition of statuses during the life cycle of this transaction
            String gatewayResponse = dbSale.getPaymentGatewayResponse() == null ? "" : dbSale.getPaymentGatewayResponse();
            if (Utils.getValueFromCRDelimitedAVPString(gatewayResponse, "statusHistory") != null) {
                String statusHistory = Utils.getValueFromCRDelimitedAVPString(gatewayResponse, "statusHistory");
                if (!statusHistory.contains(returnedStatus)) {//Store distinct statuses
                    avpBuilder.append("statusHistory=");
                    avpBuilder.append(statusHistory).append(",").append(returnedStatus);
                    avpBuilder.append("\r\n");
                }
            } else {
                avpBuilder.append("statusHistory=");
                avpBuilder.append(returnedStatus);
                avpBuilder.append("\r\n");
            }

            avpBuilder.append("transactionRef=");
            avpBuilder.append(transactionRef);
            avpBuilder.append("\r\n");

            avpBuilder.append("paymentGate=");
            avpBuilder.append(paymentGate);
            res.setInfo(paymentGate);
            avpBuilder.append("\r\n");

            avpBuilder.append("responseDescription=");
            avpBuilder.append(returnedStatus);
            avpBuilder.append("\r\n");

            avpBuilder.append("fullResponse=");
            avpBuilder.append(transactionDetails.replaceAll("=", ":"));

            res.setGatewayResponse(avpBuilder.toString());
            res.setInfo(paymentGate == null ? "" : paymentGate);
            res.setTransferredAmountCents(Double.parseDouble(amount) * 100);
            res.setPaymentGatewayTransactionId(transactionRef == null ? "" : transactionRef);

            if (returnedStatus.equalsIgnoreCase("COMPLETED")) {
                log.debug("Pesapal transaction completed successfully: {}", returnedStatus);
                res.setSuccess(true);
                res.setTryAgainLater(false);
            } else if (returnedStatus.equalsIgnoreCase("PENDING")) {
                log.debug("PesaPal transaction is ongoing: {}", returnedStatus);
                res.setGatewayResponse(avpBuilder.toString());
                res.setSuccess(false);
                res.setTryAgainLater(true);
                return res;
            } else {
                String[] pendingPaymentStatus = pendingPaymentStatuses.split(","); //See HBT-3571
                res.setSuccess(false);
                res.setTryAgainLater(false);
                log.debug("PesaPal transaction failed for sale id [{}], going to check if configured to be retried: [{}]", new Object[]{dbSale.getSaleId(), returnedStatus});
                for (String pendingStatus : pendingPaymentStatus) {

                    if (pendingStatus.equals(returnedStatus)) {
                        if (dbSale.getPaymentGatewayPollCount() < BaseUtils.getIntProperty("env.pos.paymentgateway.pending.query.count", 25)) {//Can be set depending on the load
                            //First 5 minutes . 30X10=300s=5min.
                            //50-30=20 reps 20X60s= 20min
                            res.setTryAgainLater(true);
                        }
                        log.debug("PesaPal transaction is configured to be pending for sale id [{}], will be retried: [{}]", new Object[]{dbSale.getSaleId(), returnedStatus});
                        break;
                    }
                }
            }

        } catch (Exception ex) {
            String transactionDetails = ex.toString();
            log.warn("For some reason we got an error processing results: {}", transactionDetails);
            res.setSuccess(false);
            res.setTryAgainLater(true);
            String avp = "fullResponse" + "=" + transactionDetails;
            res.setGatewayResponse(avp);
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "PesapalPaymentGateway failed to process[" + transactionDetails + "] reason: " + ex.toString());

        }
        return res;
    }

    private static String currencyCode;
    private static String paymentGatewayPostURL;
    private static String callbackURL;
    private static String oauthKeyProp;
    private static String accountId;
    private static String pendingPaymentStatuses;
    private static String statusQueryURL;
    private static String transactionTokenURL;
    private static String ipnURL;
    private static String gatewayCode;
    EntityManager em = null;

    @Override
    public void init(EntityManager em) {
        currencyCode = BaseUtils.getSubProperty("env.pgw.pesapal.config", "CurrencyCode");
        paymentGatewayPostURL = BaseUtils.getSubProperty("env.pgw.pesapal.config", "PaymentGatewayPostURL");
        callbackURL = BaseUtils.getSubProperty("env.pgw.pesapal.config", "CallbackURL");
        oauthKeyProp = BaseUtils.getSubProperty("env.pgw.pesapal.config", "Oauth10KeysProp");
        accountId = BaseUtils.getSubProperty("env.pgw.pesapal.config", "AccountId");
        pendingPaymentStatuses = BaseUtils.getSubProperty("env.pgw.pesapal.config", "PendingPaymentStatuses");
        statusQueryURL = BaseUtils.getSubProperty("env.pgw.pesapal.config", "StatusQueryURL");
        transactionTokenURL = BaseUtils.getSubProperty("env.pgw.pesapal.config", "TransactionTokenURL");
        ipnURL = BaseUtils.getSubProperty("env.pgw.pesapal.config", "IPNURL");
        gatewayCode = BaseUtils.getSubProperty("env.pgw.pesapal.config", "GatewayCode");
        this.em = em;
    }

    @Override
    public String getGatewayURL(Sale dbSale) {
        //http://www.paysmile.co.tz/index.php?option=com_users&task=paybill.ApiPay
        paymentGatewayPostURL = paymentGatewayPostURL.replaceAll("#", "=");
        return paymentGatewayPostURL;
    }

    @Override
    public String getLandingURL(Sale dbSale) {
        if (dbSale.getLandingURL().contains("?")) {
            return dbSale.getLandingURL() + "&" + "saleId=" + dbSale.getSaleId();
        } else {
            return dbSale.getLandingURL() + "?" + "saleId=" + dbSale.getSaleId();
        }
    }

    @Override
    public String getGatewayURLData(Sale dbSale) {
        Customer cust = getCustomer(dbSale.getRecipientCustomerId());
        String amount = df.format(dbSale.getSaleTotalCentsIncl().divide(new BigDecimal(100)));
        String saleId = String.valueOf(dbSale.getSaleId());

        /*
        The convention 'jdata[param-name]' is specific to Pesapal
        
        <input type="text" placeholder="First Name" name="jdata[first_name]" value="" />
	<input type="text" placeholder="Last Name" name="jdata[last_name]" value="" />
	<input type="text" placeholder="Email" name="jdata[email]" value="" />
	<input type="text" placeholder="Phone Number" name="jdata[phone]" value="" />
	<input type="text" placeholder="Amount" name="jdata[amount]" value="" />
	<input type="hidden" name="jdata[currency]" value="TZS" />
	<input type="hidden" name="jdata[description]" value="30 day UnlimitedPremium plan" />
	<input type="text" name="jdata[reference]" value="1" />
	<input type="hidden" name="callback" value="Callback URL" />
	<input type="hidden" name="ipn" value="Instant Pin Notification Url" />
	<input type="hidden" name="pesapal-token" value="eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwOlwvXC9wYXlzbWlsZS5jby50eiIsImF1ZCI6Imh0dHA6XC9cL3BheXNtaWxlLmNvLnR6IiwiZXhwIjoxNTI1MzQ3ODY0LCJqdGkiOjE1MjUzNDYwNjR9.iHS0X2LNK8IsBvXXeddd2a64D8_MQbnVqOt3FI7Ahr4" />
         */
        StringBuilder sb = new StringBuilder();
        sb.append("POST: ");
        sb.append("jdata[amount]=");//M
        sb.append(amount);
        sb.append(",callback=");//M
        sb.append(callbackURL.replaceAll("OrderId#", "OrderId#" + saleId));//e.g. https://154.73.71.2/scp/PaymentGateway.action?processBankTransaction#&saleId#1234
        sb.append(",jdata[reference]=");//M
        sb.append(saleId);
        sb.append(",jdata[email]=");//M
        sb.append(cust.getEmailAddress());
        sb.append(",jdata[first_name]=");//M
        sb.append(cust.getFirstName());
        sb.append(",jdata[last_name]=");//M
        sb.append(cust.getLastName());
        sb.append(",jdata[phone]=");//M
        sb.append(cust.getAlternativeContact1());
        sb.append(",jdata[currency]=");//M
        sb.append(currencyCode);
        sb.append(",jdata[description]=");//M
        sb.append("Smile data and voice services");
        sb.append(",ipn=");//M
        //ipnURL = ipnURL.replaceAll("#", "=");
        sb.append(ipnURL);
        sb.append(",pesapal-token=");//M
        sb.append(getTransactionToken(dbSale));

        return sb.toString();
    }

    @Override
    public String getName() {
        return "PesaPal";
    }

    @Override
    public long getAccountId() {
        return Long.valueOf(accountId);
    }

    @Override
    public boolean isUp() {
        boolean isup = PesapalPaymentGateway.ISUP_FAIL_COUNTER <= BaseUtils.getIntProperty("env.pos.paymentgateway.isup.fail.counter", 5);
        BaseUtils.sendStatistic("Pesapal", "PaymentGateway", "isup", isup ? 1 : 0, "POS");
        updateGatewayStatus("env.scp.pesapal.partner.integration.config", isup, getName());
        return isup;
    }

    @Override
    public void processPaymentNotification(int saleId, double cashReciepted, String transactionId, String paymentGatewayExtraData) throws Exception {
        Sale sale = DAO.getSale(em, saleId);
        if (!sale.getPaymentGatewayCode().equals(gatewayCode)) {
            /*Just make sure we're processing transaction initiated by Pesapal*/
            throw new Exception("Sale not initiated by this payment gateway");
        }

        if (sale.getStatus().equals(POSManager.PAYMENT_STATUS_PAID)) {
            log.debug("Sale has already been processed");
            return;
        }
        processExtraData(saleId, paymentGatewayExtraData);
    }
    
    private void processExtraData(int saleId, String paymentGatewayExtraData) throws Exception {

        JsonObject jsonObj = getJsonObjectFromJsonString(paymentGatewayExtraData);
        JsonElement je;

        String returnedTransactionRef = null;
        double returnedAmount = 0;
        String returnedTransactionStatus = "";

        for (Map.Entry element : jsonObj.entrySet()) {
            
            if (element.getKey().equals("status")) {
                je = jsonObj.get("status");
                if (!je.isJsonNull()) {
                    returnedTransactionStatus = je.getAsString();
                }
            }
            if (element.getKey().equals("amount")) {
                je = jsonObj.get("amount");
                if (!je.isJsonNull()) {
                    returnedAmount = je.getAsDouble();
                }
            }
            if (element.getKey().equals("pesapal_transaction_tracking_id")) {
                je = jsonObj.get("pesapal_transaction_tracking_id");
                if (!je.isJsonNull()) {
                    returnedTransactionRef = je.getAsString();
                }
            }
        }

        doPaymentGatewayPreProcessing(saleId, returnedTransactionStatus, paymentGatewayExtraData);
        Sale sale = DAO.getSale(em, saleId);
        /*Send only successful transaction for post-processing*/
        if (!sale.getStatus().equals(POSManager.PAYMENT_STATUS_GW_FAIL_SMILE) && returnedTransactionStatus.equalsIgnoreCase("COMPLETE")) {
            List<Integer> saleAsList = new ArrayList<>();
            saleAsList.add(saleId);
            new POSManager().processPayment(em, saleAsList, returnedAmount * 100, returnedTransactionRef, "PD", true, "Pesapal");
        }

    }
    
    private void doPaymentGatewayPreProcessing(int saleId, String returnedTransactionStatus, String paymentGatewayExtraData) {
        log.debug("In doPaymentGatewayPreProcessing");
        String returnedTransactionRef = null;
        JsonObject jsonObj = getJsonObjectFromJsonString(paymentGatewayExtraData);
        JsonElement je;
        String paymentMethod = "";


        for (Map.Entry element : jsonObj.entrySet()) {/*Won't be null, this has been tested already before request was submitted to POS*/

            if (element.getKey().equals("pesapal_transaction_tracking_id")) {
                je = jsonObj.get("pesapal_transaction_tracking_id");
                if (!je.isJsonNull()) {
                    returnedTransactionRef = je.getAsString();
                }
            }
            if (element.getKey().equals("status")) {
                je = jsonObj.get("status");
                if (!je.isJsonNull()) {
                    returnedTransactionStatus = je.getAsString();
                }
            }
            if (element.getKey().equals("method")) {
                je = jsonObj.get("method");
                if (!je.isJsonNull()) {
                    paymentMethod = je.getAsString();
                }
            }

        }

        log.debug("Getting locked sale to doPaymentGatewayPreProcessing [{}]", saleId);
        Sale lockedSale = DAO.getLockedSale(em, saleId);
        log.debug("Got locked sale to doPaymentGatewayPreProcessing");
        if (lockedSale.getStatus().equals(POSManager.PAYMENT_STATUS_PAID)) {
            //Some other thread has already processed request
            log.debug("No further processing required");
            em.flush();
            return;
        }
        lockedSale.setPaymentGatewayLastPollDate(new Date());
        lockedSale.setPaymentGatewayNextPollDate(null);
        lockedSale.setPaymentGatewayPollCount(lockedSale.getPaymentGatewayPollCount() + 1);
        lockedSale.setPaymentGatewayResponse(paymentGatewayExtraData);
        lockedSale.setExtraInfo(Utils.setValueInCRDelimitedAVPString(lockedSale.getExtraInfo(), "PaymentGatewayInfo", paymentMethod));//PaymentGatewayInfo=CREDITCARD_ACP_TZ
        lockedSale.setExtTxid(returnedTransactionRef);
        if (!returnedTransactionStatus.equalsIgnoreCase("COMPLETED")) {
            lockedSale.setStatus(POSManager.PAYMENT_STATUS_GW_FAIL_GW);
        }

        /*
            For some reason the transaction status has been updated by Pesapal... they're now nofifying us of such
            So we need to update the status so POS can reprocess the transaction
         */
        if (lockedSale.getStatus().equals(POSManager.PAYMENT_STATUS_GW_FAIL_GW)) {
            log.debug("This transaction had previously failed on GW, its now being reprocessed after status change");
            lockedSale.setStatus(POSManager.PAYMENT_STATUS_PENDING_PAYMENT);
        }

        em.persist(lockedSale);
        em.flush();
    }

    private String getTransactionToken(Sale dbSale) {

        EventQuery eq = new EventQuery();
        String eventKey = String.valueOf(dbSale.getSaleId()) + "_" + dbSale.getPaymentGatewayCode();
        eq.setEventKey(eventKey);
        eq.setResultLimit(1);

        log.debug("Going to look for transaction token in events data using event key {}", eventKey);
        EventList events = SCAWrapper.getAdminInstance().getEvents(eq);

        if (events.getNumberOfEvents() == 1) {
            return events.getEvents().get(0).getEventData();
        }

        log.debug("Going to request unique token from payment gateway then save it");
        String authorisedURL = null;
        try {
            transactionTokenURL = transactionTokenURL.replaceAll("#", "=");
            authorisedURL = PaymentGatewayHelper.sendHTTPRequest("QUERY_STRING", "GET", transactionTokenURL, new HashMap<>(), new HashMap<>(), "Pesapal");
        } catch (Exception ex) {
            log.warn("Failed to get unique token for transaction, error: {}", ex);
            if (dbSale.getStatus().equals("PP") && dbSale.getPaymentGatewayPollCount() < 4) {
                throw new RuntimeException("Failed to get unique token for transaction, redirecting to payment gateway is not possible");
            }
        }

        if (authorisedURL == null || authorisedURL.isEmpty()) {
            if (dbSale.getStatus().equals("PP") && dbSale.getPaymentGatewayPollCount() < 4) {
                throw new RuntimeException("Failed to get unique token for transaction, redirecting to payment gateway is not possible");
            }
        }

        PlatformEventManager.createEvent("POS", "PaymentGatewayPlugin", eventKey, authorisedURL);

        return authorisedURL;
    }

    @Override
    public void sendSMSForGatewayInavailability() {
        try {
            Set<String> configWatchers = BaseUtils.getPropertyAsSet("env.pos.paymentgateway.sms.isup.watchers");
            String sms = "PesaPal interface alert: Failing to get transaction status response when communicating with payment gateway. Please monitor environment to determine issue severity.";
            for (String watcher : configWatchers) {
                sendSMS(watcher, sms);
            }
        } catch (Exception ex) {
        }
    }

    @Override
    public void addPaymentGatewaySpecificsOnTicket(NewTTIssue tt, String errorMessage) {
        
        JiraField jiraF = new JiraField();

        jiraF.setFieldName("TT_FIXED_FIELD_Description");
        jiraF.setFieldType("TT_FIXED_FIELD");
        StringBuilder description = new StringBuilder("{panel:title=(!) ");
        description.append("Warning: PesaPal is currently not available to query transaction statuses");
        description.append("|borderStyle=dashed|titleBGColor=red}");
        description.append("PesaPal interface to query transaction statuses is currently not available\n")
                .append("Customers using MySmile to recharge would be affected\n").append("{panel}");
        jiraF.setFieldValue(description.toString());
        tt.getMindMapFields().getJiraField().add(jiraF);

        jiraF = new JiraField();
        jiraF.setFieldName("TT_FIXED_FIELD_Summary");
        jiraF.setFieldType("TT_FIXED_FIELD");
        String summary = "PesaPal API interface instability";
        jiraF.setFieldValue(summary);
        tt.getMindMapFields().getJiraField().add(jiraF);

        jiraF = new JiraField();
        jiraF.setFieldName("TT_FIXED_FIELD_Issue Type");
        jiraF.setFieldType("TT_FIXED_FIELD");
        jiraF.setFieldValue("Customer Incident");
        tt.getMindMapFields().getJiraField().add(jiraF);

        jiraF = new JiraField();
        jiraF.setFieldName("TT_FIXED_FIELD_Project");
        jiraF.setFieldType("TT_FIXED_FIELD");
        jiraF.setFieldValue(BaseUtils.getProperty("env.jira.customer.care.project.key"));
        tt.getMindMapFields().getJiraField().add(jiraF);

        jiraF = new JiraField();
        jiraF.setFieldName("SEP Reporter");
        jiraF.setFieldType("TT_FIXED_FIELD");
        jiraF.setFieldValue("admin admin");
        tt.getMindMapFields().getJiraField().add(jiraF);

        jiraF = new JiraField();
        jiraF.setFieldName("Incident Channel");
        jiraF.setFieldType("TT_FIXED_FIELD");
        jiraF.setFieldValue("System");
        tt.getMindMapFields().getJiraField().add(jiraF);

    }

}
