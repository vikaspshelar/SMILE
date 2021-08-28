/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos.paymentgateway;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.paymentgateway.helpers.PaymentGatewayHelper;
import com.smilecoms.commons.paymentgateway.Paystack;
import com.smilecoms.commons.platform.PlatformEventManager;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.EventList;
import com.smilecoms.commons.sca.EventQuery;
import com.smilecoms.commons.sca.JiraField;
import com.smilecoms.commons.sca.MindMapFields;
import com.smilecoms.commons.sca.NewTTIssue;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.pos.POSManager;
import com.smilecoms.pos.db.model.Sale;
import com.smilecoms.pos.db.op.DAO;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
import org.apache.commons.lang.StringEscapeUtils;

/**
 *
 * @author sabza
 */
public class PaystackPaymentGateway extends AbstractPaymentGateway implements PaymentGatewayPlugin {

    private static final String PAYSTACK_CACHE_KEY = "paystack_error_key";
    private static int ISUP_FAIL_COUNTER = 0;
    private static final Map<String, String> HEADERS = new HashMap<>();
    private static final String AUTHORIZATION = "Authorization";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String USER_AGENT = "Java/8.0 HBT/2.3.1 POS-Paystack";
    private static final String BEARER = "Bearer";
    private static final String AGENT = "User-Agent";
    private final DecimalFormat df = new DecimalFormat("#");
    private static String callbackURL;
    private static String paymentGatewayPostURL;
    private static String clientSecret;
    private static String statusQueryURL;
    private static String accountId;
    private static String pendingPaymentStatuses;
    private static String gatewayCode;
    EntityManager em = null;

    @Override
    public PaymentGatewayResult getPaymentGatewayResult(Sale dbSale) {
        PaymentGatewayResult res = new PaymentGatewayResult();
        try {

            if (dbSale.getPaymentGatewayPollCount() < BaseUtils.getIntProperty("env.pos.paymentgateway.paystack.firstquery.delay.count", 1)) {
                //Dont check transaction status early as we might get FAILED
                res.setTryAgainLater(true);
                res.setGatewayResponse("REQUEST_DELAYED");
                res.setSuccess(false);
                return res;
            }

            String transactionDetails;

            try {
                int saleId = dbSale.getSaleId();
                Paystack paystack = new Paystack();
                String sQueryURL = statusQueryURL + saleId;
                transactionDetails = StringEscapeUtils.unescapeHtml(paystack.getTransactionDetails(sQueryURL, HEADERS)).replaceAll("[^\\p{ASCII}]", "");
                log.debug("HTTP client request returned successfully for getTransactionDetails. Transaction details returned[{}]", transactionDetails);

            } catch (Exception ex) {
                log.warn("System error in Paystack plugin: ", ex);
                res.setGatewayResponse(ex.toString());
                res.setSuccess(false);
                res.setTryAgainLater(true);
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "Error in Paystack PaymentGateway plugin: " + ex.toString());
                PaystackPaymentGateway.ISUP_FAIL_COUNTER++;
                
                boolean isup = PaystackPaymentGateway.ISUP_FAIL_COUNTER <= BaseUtils.getIntProperty("env.pos.paymentgateway.isup.fail.counter", 5);
                publishGatewayInAvailabilityNotice(getName(), isup, PAYSTACK_CACHE_KEY, ex.toString());
                return res;
            }
            PaystackPaymentGateway.ISUP_FAIL_COUNTER = 0;
            updateGatewayStatus("env.scp.paystack.partner.integration.config", true, getName());

            /*
                {
                        "status": true,
                        "message": "Verification successful",
                        "data": {
                          "id": 6010021,
                          "amount": 342200,
                          "currency": "NGN",
                          "transaction_date": "2017-11-09T08:16:00.000Z",
                          "status": "success",
                          "reference": "7787565",
                          "domain": "test",
                          "metadata": {
                            "referrer": "http://localhost:8004/scp/Account.action"
                          },
                          "gateway_response": "Successful",
                          "message": null,
                          "channel": "card",
                          "ip_address": "41.169.133.178, 197.234.242.46, 172.31.10.202",
                          "log": {
                            "time_spent": 54,
                            "attempts": 1,
                            "authentication": null,
                            "errors": 0,
                            "success": false,
                            "mobile": false,
                            "input": [],
                            "channel": null,
                            "history": [
                              {
                                "type": "input",
                                "message": "Filled these fields: card number, card expiry, card cvv",
                                "time": 54
                              },
                              {
                                "type": "action",
                                "message": "Attempted to pay",
                                "time": 54
                              }
                            ]
                          },
                          "fees": 15133,
                          "authorization": {
                            "authorization_code": "AUTH_pr6wa1onto",
                            "bin": "408408",
                            "last4": "4081",
                            "exp_month": "01",
                            "exp_year": "2020",
                            "channel": "card",
                            "card_type": "visa DEBIT",
                            "bank": "Test Bank",
                            "country_code": "NG",
                            "brand": "visa",
                            "reusable": true,
                            "signature": "SIG_4QjFjHEqqZfjogEcWMNJ"
                          },
                          "customer": {
                            "id": 977747,
                            "first_name": "Sabelo",
                            "last_name": "Dlangamandla",
                            "email": "sabelo.dlangamandla@smilecoms.com",
                            "customer_code": "CUS_0ny6liskdt1s604",
                            "phone": null,
                            "metadata": {},
                            "risk_action": "default"
                          },
                          "plan": null
                        }

             */
            String returnedTransactionStatus = "";
            String returnedResponseDescription = "";

            try {

                JsonParser jsonParser = new JsonParser();
                JsonObject transactionAsJsonObject = jsonParser.parse(transactionDetails).getAsJsonObject();

                String saleID = null;
                String returnedTransactionRef = null;
                double returnedAmount = 0;
                String returnedDatetime = null;
                String cardLocale = "";
                JsonElement je;

                JsonObject jsonObj = null;
                JsonObject authorisationJsonObj = null;
                String status200OK = "unknown";//This just indicate that the MySmile customer successfully called Paystack's front-end portal.

                for (Map.Entry element : transactionAsJsonObject.entrySet()) {
                    String key = element.getKey().toString();
                    if (element.getKey().equals("status")) {
                        je = transactionAsJsonObject.get("status");
                        status200OK = je.getAsString();
                    }
                    if (element.getKey().equals("data")) {
                        if (transactionAsJsonObject.get("data").isJsonObject()) {
                            jsonObj = transactionAsJsonObject.get("data") != null ? transactionAsJsonObject.get("data").getAsJsonObject() : null;
                        }
                    }
                }

                if (jsonObj == null) {
                    log.debug("Expected data property to be of type JSON Object. Will retry this transaction: {}", transactionAsJsonObject.toString());
                    res.setGatewayResponse("fullResponse=" + transactionDetails);
                    res.setSuccess(false);
                    res.setTryAgainLater(true);
                    if (dbSale.getPaymentGatewayPollCount() > 45) {//15min=30x10sec + 15x60sec
                        log.debug("Paystack probably doesn't know about this record. Permanently marking it as failed.");
                        res.setTryAgainLater(false);
                    }
                    return res;
                }

                for (Map.Entry element : jsonObj.entrySet()) {

                    if (element.getKey().equals("reference")) {
                        je = jsonObj.get("reference");
                        if (!je.isJsonNull()) {
                            saleID = je.getAsString();
                        }
                    }
                    if (element.getKey().equals("reference")) {
                        je = jsonObj.get("reference");
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
                    if (element.getKey().equals("amount")) {
                        je = jsonObj.get("amount");
                        if (!je.isJsonNull()) {
                            returnedAmount = je.getAsDouble();
                        }
                    }
                    if (element.getKey().equals("transaction_date")) {
                        je = jsonObj.get("transaction_date");
                        if (!je.isJsonNull()) {
                            returnedDatetime = je.getAsString();
                        }
                    }
                    if (element.getKey().equals("authorization")) {
                        if (jsonObj.get("authorization").isJsonObject()) {
                            authorisationJsonObj = jsonObj.get("authorization") != null ? jsonObj.get("authorization").getAsJsonObject() : null;
                        }
                    }
                    if (element.getKey().equals("gateway_response")) {
                        je = jsonObj.get("gateway_response");
                        if (!je.isJsonNull()) {
                            returnedResponseDescription = je.getAsString();
                        }
                    }
                }

                if (authorisationJsonObj != null) {
                    log.debug("Going to try determine card locale, Local/International: {}", transactionAsJsonObject.toString());
                    for (Map.Entry authElem : authorisationJsonObj.entrySet()) {
                        if (authElem.getKey().equals("country_code")) {
                            je = authorisationJsonObj.get("country_code");
                            if (!je.isJsonNull()) {
                                cardLocale = je.getAsString();
                                log.debug("Card locale is: {}", cardLocale);
                                break;
                            }
                        }
                    }
                }

                StringBuilder avpBuilder = new StringBuilder();

                avpBuilder.append("orderId=");
                avpBuilder.append(dbSale.getSaleId());
                avpBuilder.append("\r\n");

                avpBuilder.append("paymentReference=");
                avpBuilder.append(returnedTransactionRef);
                avpBuilder.append("\r\n");

                avpBuilder.append("responseCode=");
                avpBuilder.append(returnedTransactionStatus);
                avpBuilder.append("\r\n");

                //Helps to keep track of the transition of statuses during the life cycle of this transaction
                String gatewayResponse = dbSale.getPaymentGatewayResponse() == null ? "" : dbSale.getPaymentGatewayResponse();
                if (Utils.getValueFromCRDelimitedAVPString(gatewayResponse, "statusHistory") != null) {
                    String statusHistory = Utils.getValueFromCRDelimitedAVPString(gatewayResponse, "statusHistory");
                    if (!statusHistory.contains(returnedTransactionStatus)) {//Store distinct statuses
                        avpBuilder.append("statusHistory=");
                        avpBuilder.append(statusHistory).append(",").append(returnedTransactionStatus);
                        avpBuilder.append("\r\n");
                    }
                } else {
                    avpBuilder.append("statusHistory=");
                    avpBuilder.append(returnedTransactionStatus);
                    avpBuilder.append("\r\n");
                }

                avpBuilder.append("responseDescription=");
                avpBuilder.append(returnedResponseDescription);
                avpBuilder.append("\r\n");

                avpBuilder.append("transactionDate=");
                avpBuilder.append(returnedDatetime);
                avpBuilder.append("\r\n");

                avpBuilder.append("amount=");
                avpBuilder.append(returnedAmount);
                avpBuilder.append("\r\n");

                avpBuilder.append("fullResponse=");
                avpBuilder.append(transactionDetails);

                res.setInfo(cardLocale == null ? "" : cardLocale);//Needed to determine if international/local card was used.
                res.setGatewayResponse(avpBuilder.toString());
                res.setTransferredAmountCents(returnedAmount);
                res.setPaymentGatewayTransactionId(returnedTransactionRef);
            } catch (Exception ex) {
                log.warn("SaleId {}, Paystack response {}, error [{}]", new Object[]{dbSale.getSaleId(), transactionDetails, ex.toString()});
                res.setGatewayResponse(transactionDetails);
                res.setSuccess(false);
                res.setTryAgainLater(true);
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "Paystack failed to process[" + transactionDetails + "] reason: " + ex.toString());
                return res;
            }

            if (returnedTransactionStatus.equalsIgnoreCase("success")) {
                log.debug("Paystack transaction completed successfully: {}", returnedTransactionStatus);
                res.setSuccess(true);
                res.setTryAgainLater(false);
            } else {
                String[] pendingPaymentStatus = pendingPaymentStatuses.split(",");
                res.setSuccess(false);
                res.setTryAgainLater(false);
                log.debug("Paystack transaction failed for sale id [{}], going to check if configured to be retried: [{}], reason: [{}]", new Object[]{dbSale.getSaleId(), returnedTransactionStatus, returnedResponseDescription});
                for (String pendingStatus : pendingPaymentStatus) {

                    //We are treating all responses from Paystack as PENDING because the ONLY FINAL state is 'success'. Other states can transition any time, even a FAIL status
                    //We control the amount of time to query by setting a session timeout on the Paystack payment portal. Currently we set it to 30 minutes
                    //curl --request PUT --url https://api.paystack.co/integration/payment_session_timeout --header 'authorization: Bearer SECRET_KEY' --header 'content-type: application/x-www-form-urlencoded' --data timeout=1800
                    //curl --request GET --url https://api.paystack.co/integration/payment_session_timeout --header 'authorization: Bearer SECRET_KEY'
                    if (pendingStatus.equals(returnedTransactionStatus)) {
                        if (dbSale.getPaymentGatewayPollCount() < BaseUtils.getIntProperty("env.pos.paymentgateway.pending.query.count", 50)) {//Can be set depending on the load
                            //First 5 minutes . 30X10=300s=5min.
                            //50-30=20 reps 20X60s= 20min
                            res.setTryAgainLater(true);
                        }
                        log.debug("Paystack transaction is configured to be pending for sale id [{}], will be retried: [{}], reason: [{}]", new Object[]{dbSale.getSaleId(), returnedTransactionStatus, returnedResponseDescription});
                        break;
                    }
                }
            }

        } catch (Exception ex) {
            log.warn("System error in Paystack plugin: ", ex);
            res.setGatewayResponse(ex.toString());
            res.setSuccess(false);
            res.setTryAgainLater(true);
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "Error in Paystack plugin: " + ex.toString());
        }

        log.debug("Exiting getPaymentGatewayResult for Paystack");

        return res;
    }

    @Override
    public void init(EntityManager em) {
        paymentGatewayPostURL = BaseUtils.getSubProperty("env.pgw.paystack.config", "PaymentGatewayPostURL");
        clientSecret = BaseUtils.getSubProperty("env.pgw.paystack.config", "ClientSecret");
        statusQueryURL = BaseUtils.getSubProperty("env.pgw.paystack.config", "StatusQueryURL");
        callbackURL = BaseUtils.getSubProperty("env.pgw.paystack.config", "CallbackURL");
        accountId = BaseUtils.getSubProperty("env.pgw.paystack.config", "AccountId");
        pendingPaymentStatuses = BaseUtils.getSubProperty("env.pgw.paystack.config", "PendingPaymentStatuses");
        gatewayCode = BaseUtils.getSubProperty("env.pgw.paystack.config", "GatewayCode");
        HEADERS.put(CONTENT_TYPE, "application/json");
        HEADERS.put(AUTHORIZATION, getAuthorizationHeaderForAccessToken());
        HEADERS.put(AGENT, USER_AGENT);
        this.em = em;
    }

    private static String getAuthorizationHeaderForAccessToken() {
        return BEARER + " " + clientSecret;
    }

    @Override
    public String getGatewayURL(Sale dbSale) {
        //The paymentGatewayPostURL is unique per transaction ::: call paystack to generate one for this transaction
        String ret = getCustomisedPostURL(dbSale);
        if (ret != null) {
            log.debug("Payment gateway post URL is: {}", ret);
            return ret;
        }
        return paymentGatewayPostURL;
    }

    private String getCustomisedPostURL(Sale dbSale) {

        EventQuery eq = new EventQuery();
        String eventKey = String.valueOf(dbSale.getSaleId()) + "_" + dbSale.getPaymentGatewayCode();
        eq.setEventKey(eventKey);
        eq.setResultLimit(1);

        log.debug("Going to look for authorisation url in events data using event key {}", eventKey);
        EventList events = SCAWrapper.getAdminInstance().getEvents(eq);

        if (events.getNumberOfEvents() == 1) {
            return events.getEvents().get(0).getEventData();
        }


        /*
                RESPONSE EXAMPLE
                {
                    "status": true,
                    "message": "Authorization URL created",
                    "data": {
                      "authorization_url": "https://standard.paystack.co/pay/0peioxfhpn",
                      "access_code": "0peioxfhpn",
                      "reference": "920iieuwq"
                    }
                }
         */
        log.debug("Going to request authorisation url from payment gateway then save it");
        Customer cust = getCustomer(dbSale.getRecipientCustomerId());
        JsonObject ss = new JsonObject();
        ss.addProperty("reference", String.valueOf(dbSale.getSaleId()));
        ss.addProperty("amount", df.format(dbSale.getSaleTotalCentsIncl().doubleValue()));
        ss.addProperty("email", cust.getEmailAddress());

        String callBack = callbackURL;
        //https://154.73.71.2/scp/PaymentGateway.action?processBankTransaction=&OrderId=1234
        callBack = callBack.replaceAll("#", "=");
        callBack = callBack.replaceAll("OrderId=", "OrderId=" + String.valueOf(dbSale.getSaleId()));

        ss.addProperty("callback_url", callBack);
        JsonObject metaData = new JsonObject();
        metaData.addProperty("cancel_action", callBack);
        ss.add("metadata", metaData);

        String json = getObjectAsJsonString(ss);
        String authorisedURL = null;
        String authorisedURLData = null;
        try {
            authorisedURLData = PaymentGatewayHelper.sendHTTPRequest("BODY", "POST", paymentGatewayPostURL, HEADERS, json, "Paystack");
        } catch (Exception ex) {
            log.warn("Failed to get authorisation url for transaction, error: {}", ex);
            if (dbSale.getStatus().equals("PP") && dbSale.getPaymentGatewayPollCount() < 4) {
                throw new RuntimeException("Failed to get authorisation url for transaction, redirecting to payment gateway is not possible");
            }
        }

        JsonParser jsonParser = new JsonParser();
        JsonObject authorisedURLDataAsJsonObject = jsonParser.parse(authorisedURLData).getAsJsonObject();

        JsonElement je;
        JsonObject jsonObj = null;

        for (Map.Entry element : authorisedURLDataAsJsonObject.entrySet()) {
            if (element.getKey().equals("data")) {
                if (authorisedURLDataAsJsonObject.get("data").isJsonObject()) {
                    jsonObj = authorisedURLDataAsJsonObject.get("data") != null ? authorisedURLDataAsJsonObject.get("data").getAsJsonObject() : null;
                    break;
                }
            }
        }

        if (jsonObj != null) {
            for (Map.Entry element : jsonObj.entrySet()) {

                if (element.getKey().equals("authorization_url")) {
                    je = jsonObj.get("authorization_url");
                    if (!je.isJsonNull()) {
                        authorisedURL = je.getAsString();
                        break;
                    }
                }
            }
        }

        if (authorisedURL == null || authorisedURL.isEmpty()) {
            log.warn("Failed to get authorisation url for transaction from parsed response: {}", authorisedURLData);
            if (dbSale.getStatus().equals("PP") && dbSale.getPaymentGatewayPollCount() < 4) {
                throw new RuntimeException("Failed to get authorisation url for transaction, redirecting to payment gateway is not possible");
            }
        }

        PlatformEventManager.createEvent("POS", "PaymentGatewayPlugin", eventKey, authorisedURL);

        return authorisedURL;
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
        String amount = df.format(dbSale.getSaleTotalCentsIncl().doubleValue());
        String saleId = String.valueOf(dbSale.getSaleId());

        StringBuilder sb = new StringBuilder();
        sb.append("POST: ");
        sb.append("amount=");//M
        sb.append(amount);
        sb.append(",callback_url=");//M
        sb.append(callbackURL.replaceAll("OrderId#", "OrderId#" + saleId));//e.g. https://154.73.71.2/scp/PaymentGateway.action?processBankTransaction#&saleId#1234
        sb.append(",reference=");//M
        sb.append(saleId);
        sb.append(",email=");//M
        sb.append(cust.getEmailAddress());

        return sb.toString();
    }

    @Override
    public String getName() {
        return "Paystack";
    }

    @Override
    public long getAccountId() {
        return Long.valueOf(accountId);
    }

    @Override
    public boolean isUp() {
        boolean isup = PaystackPaymentGateway.ISUP_FAIL_COUNTER <= BaseUtils.getIntProperty("env.pos.paymentgateway.isup.fail.counter", 5);
        BaseUtils.sendStatistic("Paystack", "PaymentGateway", "isup", isup ? 1 : 0, "POS");
        updateGatewayStatus("env.scp.paystack.partner.integration.config", isup, getName());
        return isup;
    }

    @Override
    public void processPaymentNotification(int saleId, double cashReciepted, String transactionId, String paymentGatewayExtraData) throws Exception {
        Sale sale = DAO.getSale(em, saleId);
        if (!sale.getPaymentGatewayCode().equals(gatewayCode)) {
            /*Just make sure we're processing transaction initiated by Paystack*/
            throw new Exception("Sale not initiated by payment gateway");
        }

        if (sale.getStatus().equals(POSManager.PAYMENT_STATUS_PAID)) {
            log.debug("Sale has already been processed");
            return;
        }
        processExtraData(saleId, paymentGatewayExtraData);
    }

    @Override
    public void addPaymentGatewaySpecificsOnTicket(NewTTIssue tt, String errorMessage) {
        JiraField jiraF = new JiraField();

        jiraF.setFieldName("TT_FIXED_FIELD_Description");
        jiraF.setFieldType("TT_FIXED_FIELD");
        StringBuilder description = new StringBuilder("{panel:title=(!) ");
        description.append("Warning: Paystack is reporting instability");
        description.append("|borderStyle=dashed|titleBGColor=red}");
        description.append("Paystack interface to query transaction statuses is reporting instability\n")
                .append("Customers using MySmile or XpressRecharge to recharge would be affected\n").append("{panel}");
        jiraF.setFieldValue(description.toString());
        tt.getMindMapFields().getJiraField().add(jiraF);

        jiraF = new JiraField();
        jiraF.setFieldName("TT_FIXED_FIELD_Summary");
        jiraF.setFieldType("TT_FIXED_FIELD");
        String summary = "Paystack API interface instability";
        jiraF.setFieldValue(summary);
        tt.getMindMapFields().getJiraField().add(jiraF);

        jiraF = new JiraField();
        jiraF.setFieldName("TT_FIXED_FIELD_Issue Type");
        jiraF.setFieldType("TT_FIXED_FIELD");
        jiraF.setFieldValue("Fault");
        tt.getMindMapFields().getJiraField().add(jiraF);

        jiraF = new JiraField();
        jiraF.setFieldName("TT_FIXED_FIELD_Project");
        jiraF.setFieldType("TT_FIXED_FIELD");
        jiraF.setFieldValue("NGIT");
        tt.getMindMapFields().getJiraField().add(jiraF);

    }

    @Override
    public void sendSMSForGatewayInavailability() {
        try {
            Set<String> configWatchers = BaseUtils.getPropertyAsSet("env.pos.paymentgateway.sms.isup.watchers");
            String sms = "Paystack interface alert: Environment instability. Please monitor environment to determine issue severity.";
            for (String watcher : configWatchers) {
                sendSMS(watcher, sms);
            }
        } catch (Exception ex) {
        }
    }

    @Override
    public void sendEmailForGatewayInavailability(String errorMessage) {
        try {
            Set<String> configWatchers = BaseUtils.getPropertyAsSet("env.pos.paymentgateway.email.isup.watchers");
            String from = BaseUtils.getProperty("env.pos.paymentgateway.email.notification.from", "admin@smilecoms.com");
            String bodyPart = ",<br/><br/><strong>Paystack interface alert:</strong> Interface is reporting instability, this may result to failure in giving credint customers accounts for their airtime or bundle purchase."
                    + "<br/><strong>Caused by:</strong><br/>" + errorMessage
                    + "<br/><br/>Please monitor environment to determine issue severity.<br/><br/>";
            String subject = "Paystack interface instability alert";
            for (String to : configWatchers) {
                String fullBody = "Hi " + to + bodyPart;
                sendEmail(from, to, subject, fullBody);
            }
        } catch (Exception ex) {
        }
    }

    private void processExtraData(int saleId, String paymentGatewayExtraData) throws Exception {

        JsonObject transactionAsJsonObject = getJsonObjectFromJsonString(paymentGatewayExtraData);
        JsonObject jsonObj = null;
        JsonElement je;

        for (Map.Entry element : transactionAsJsonObject.entrySet()) {
            if (element.getKey().equals("data")) {
                if (transactionAsJsonObject.get("data").isJsonObject()) {
                    jsonObj = transactionAsJsonObject.get("data") != null ? transactionAsJsonObject.get("data").getAsJsonObject() : null;
                }
            }
        }

        String returnedTransactionRef = null;
        double returnedAmount = 0;
        String returnedTransactionStatus = "";

        for (Map.Entry element : jsonObj.entrySet()) {
            if (element.getKey().equals("reference")) {
                je = jsonObj.get("reference");
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
            if (element.getKey().equals("amount")) {
                je = jsonObj.get("amount");
                if (!je.isJsonNull()) {
                    returnedAmount = je.getAsDouble();
                }
            }
        }

        doPaymentGatewayPreProcessing(saleId, returnedTransactionStatus, paymentGatewayExtraData);
        Sale sale = DAO.getSale(em, saleId);
        /*Send only successful transaction for post-processing*/
        if (!sale.getStatus().equals(POSManager.PAYMENT_STATUS_GW_FAIL_SMILE) && returnedTransactionStatus.equalsIgnoreCase("success")) {
            List<Integer> saleAsList = new ArrayList<>();
            saleAsList.add(saleId);
            new POSManager().processPayment(em, saleAsList, (returnedAmount), returnedTransactionRef, "PD", true, "Paystack");
        }

    }

    private void doPaymentGatewayPreProcessing(int saleId, String returnedTransactionStatus, String paymentGatewayExtraData) {
        log.debug("In doPaymentGatewayPreProcessing");
        String returnedTransactionRef = null;
        JsonObject transactionAsJsonObject = getJsonObjectFromJsonString(paymentGatewayExtraData);
        JsonElement je;
        JsonObject jsonObj = null;
        JsonObject authorisationJsonObj = null;
        String cardLocale = "";

        for (Map.Entry element : transactionAsJsonObject.entrySet()) {
            if (element.getKey().equals("data")) {
                if (transactionAsJsonObject.get("data").isJsonObject()) {
                    jsonObj = transactionAsJsonObject.get("data") != null ? transactionAsJsonObject.get("data").getAsJsonObject() : null;
                }
            }
        }

        for (Map.Entry element : jsonObj.entrySet()) {/*Won't be null, this has been tested already before request was submitted to POS*/

            if (element.getKey().equals("reference")) {
                je = jsonObj.get("reference");
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

            if (element.getKey().equals("authorization")) {
                if (jsonObj.get("authorization").isJsonObject()) {
                    authorisationJsonObj = jsonObj.get("authorization") != null ? jsonObj.get("authorization").getAsJsonObject() : null;
                }
            }
        }

        if (authorisationJsonObj != null) {
            for (Map.Entry authElem : authorisationJsonObj.entrySet()) {
                if (authElem.getKey().equals("country_code")) {
                    je = authorisationJsonObj.get("country_code");
                    if (!je.isJsonNull()) {
                        cardLocale = je.getAsString();
                        log.debug("Card locale is: {}", cardLocale);
                        break;
                    }
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
        lockedSale.setExtraInfo(Utils.setValueInCRDelimitedAVPString(lockedSale.getExtraInfo(), "PaymentGatewayInfo", cardLocale));
        lockedSale.setExtTxid(returnedTransactionRef);
        if (!returnedTransactionStatus.equalsIgnoreCase("success")) {
            lockedSale.setStatus(POSManager.PAYMENT_STATUS_GW_FAIL_GW);
        }

        /*
            For some reason the transaction status has been updated by Paystack... they're now nofifying us of such
            So we need to update the status so POS can reprocess the transaction
         */
        if (lockedSale.getStatus().equals(POSManager.PAYMENT_STATUS_GW_FAIL_GW)) {
            log.debug("This transaction had previously failed on GW, its now being reprocessed after status change");
            lockedSale.setStatus(POSManager.PAYMENT_STATUS_PENDING_PAYMENT);
        }

        em.persist(lockedSale);
        em.flush();
    }

}
