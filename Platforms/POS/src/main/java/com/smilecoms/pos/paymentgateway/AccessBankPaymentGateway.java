/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos.paymentgateway;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.paymentgateway.AccessBank;
import com.smilecoms.commons.sca.JiraField;
import com.smilecoms.commons.sca.MindMapFields;
import com.smilecoms.commons.sca.NewTTIssue;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.pos.db.model.Sale;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.persistence.EntityManager;
import org.apache.commons.lang.StringEscapeUtils;

/**
 *
 * @author sabza
 */
public class AccessBankPaymentGateway extends AbstractPaymentGateway implements PaymentGatewayPlugin {

    private static final String ACCESS_BANK_CACHE_KEY = "access_bank_error_key";
    private static int ISUP_FAIL_COUNTER = 0;
    private static String merchantId;
    private static String paymentGatewayPostURL;
    private static String username;
    private static String password;
    private static String clientId;
    private static String clientSecret;
    private static String statusQueryURL;
    private static String callbackURL;
    private static String oauth20TokenURL;
    private static String accountId;
    private static String publicKey;

    @Override
    public PaymentGatewayResult getPaymentGatewayResult(Sale dbSale) {
        PaymentGatewayResult res = new PaymentGatewayResult();
        log.debug("In AccessgetPaymentGatewayResult");
        try {

            if (dbSale.getPaymentGatewayPollCount() < BaseUtils.getIntProperty("env.pos.paymentgateway.access.firstquery.delay.count",1)) {                
                //Dont check transaction status early as we might get FAILED, we wait 10sec. //Body={"status":"fail","data":{}}, StatusLine=HTTP/1.1 422 Unprocessable Entity                
                res.setTryAgainLater(true);
                res.setGatewayResponse("REQUEST_DELAYED");
                res.setSuccess(false);
                return res;
            }

            String transactionDetails;

            try {
                AccessBank access = new AccessBank(statusQueryURL, oauth20TokenURL, username, password, clientId, clientSecret);                 
                transactionDetails = access.getTransactionDetails(String.valueOf(dbSale.getSaleId()));
                transactionDetails = StringEscapeUtils.unescapeHtml(transactionDetails);
                transactionDetails = transactionDetails.replaceAll("\\|", Pattern.quote("|"));
                
            } catch (Exception ex) {
                transactionDetails = ex.toString();
                log.warn("We got an error communicating with payment_gateway: {}", transactionDetails);
                res.setSuccess(false);
                res.setTryAgainLater(true);
                String avp = "fullResponse" + "=" + transactionDetails;
                res.setGatewayResponse(avp);
                AccessBankPaymentGateway.ISUP_FAIL_COUNTER++;
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "AccessBankPaymentGateway got an error communicating with payment gateway [" + transactionDetails + "]");
                
                boolean isup = AccessBankPaymentGateway.ISUP_FAIL_COUNTER <= BaseUtils.getIntProperty("env.pos.paymentgateway.isup.fail.counter", 5);
                publishGatewayInAvailabilityNotice(getName(), isup, ACCESS_BANK_CACHE_KEY, ex.toString());
                
                return res;
            }
             
            AccessBankPaymentGateway.ISUP_FAIL_COUNTER = 0;
            updateGatewayStatus("env.scp.accessbank.partner.integration.config", true, getName());

            if (transactionDetails.contains("\"status\":\"processing\"") || transactionDetails.contains("Application Error") || transactionDetails.contains("Internal Server Error")) {//The response here contains HTML tags, the transaction will transition to parsable JSON responses.
                res.setSuccess(false);
                res.setTryAgainLater(true);
                String avp = "fullResponse" + "=" + transactionDetails;
                res.setGatewayResponse(StringEscapeUtils.escapeHtml(avp));
                log.debug("Transaction status is processing, and contains string that is not JSON parsable: {}", transactionDetails);
                return res;
            }

            String status200OK = "unknown";//This just indicate that the MySmile customer successfully called AccessBank's front-end portal.
            String returnedDatetime = null;
            String actualTransactionStatus = "";
            String cardLocale = null;

            try {

                JsonParser jsonParser = new JsonParser();
                JsonObject transactionAsJsonObject = jsonParser.parse(transactionDetails).getAsJsonObject();
                boolean containsTransactionData = false;

                for (Map.Entry element : transactionAsJsonObject.entrySet()) {
                    if (element.getKey().equals("status")) {
                        containsTransactionData = true;
                        break;
                    }
                }

                if (!containsTransactionData) {
                    log.debug("Access Bank returned a response that cannot be parsed into a transaction: {}", transactionDetails);
                    res.setGatewayResponse("fullResponse=" + StringEscapeUtils.escapeHtml(transactionDetails));
                    res.setSuccess(false);
                    res.setTryAgainLater(true);
                    if (dbSale.getPaymentGatewayPollCount() > 45) {//15min=30x10sec + 15x60sec
                        log.debug("Access Bank probably doesn't know about this record. Permanently marking it as failed.");
                        res.setTryAgainLater(false);
                    }
                    return res;
                }

                JsonObject jsonObj = null;
                for (Map.Entry element : transactionAsJsonObject.entrySet()) {
                    if (element.getKey().equals("status")) {
                        JsonPrimitive asJsonPrimitive = transactionAsJsonObject.getAsJsonPrimitive("status");
                        status200OK = asJsonPrimitive.getAsString();
                    }
                    if (element.getKey().equals("data")) {
                        
                        if (transactionAsJsonObject.get("data") != null && transactionAsJsonObject.get("data").isJsonArray()) {                            
                            for(int i=0; i< transactionAsJsonObject.get("data").getAsJsonArray().size(); i++) {
                                jsonObj = transactionAsJsonObject.get("data").getAsJsonArray().get(i).getAsJsonObject();                                
                                
                                
                                if(jsonObj.get("status").toString().toLowerCase().contains("successful")) { //Soon as we find the successful record, no need to consider others
                                    break;
                                }
                            }
                            
                        //Sometimes client may enter wrong details, but fix them later. Flutterwave keeps both records
                        //If it's not an actual payment processing failure, keep checking incase client fixes later 
                        if(jsonObj.get("processor_response").toString().toLowerCase().contains("Invalid Pin".toLowerCase()) || jsonObj.get("processor_response").toString().toLowerCase().contains("Invalid cvv".toLowerCase())) {
                            log.debug("Client may have entered invalid info at our time of checking. We will check again later");
                            res.setGatewayResponse("fullResponse=" + StringEscapeUtils.escapeHtml(transactionDetails));
                            res.setSuccess(false);
                            res.setTryAgainLater(true);
                            if (dbSale.getPaymentGatewayPollCount() > 45) {//15min=30x10sec + 15x60sec
                                log.debug("Access Bank probably doesn't know about this record. Permanently marking it as failed.");
                                res.setTryAgainLater(false);
                            }
                            return res;
                        }

                            
                            //Body={"status":"fail","data":{"name":"Warn","message":"Order not found"}}, responseBody: HTTP/1.1 422 Unprocessable Entity
                        } else {

                            log.debug("Expected data property to be of type JSONArrayObject. Will retry this transaction: {}", transactionAsJsonObject.toString());
                            res.setGatewayResponse("fullResponse=" + StringEscapeUtils.escapeHtml(transactionDetails));
                            res.setSuccess(false);
                            res.setTryAgainLater(true);
                            if (dbSale.getPaymentGatewayPollCount() > 45) {//15min=30x10sec + 15x60sec
                                log.debug("Access Bank probably doesn't know about this record. Permanently marking it as failed.");
                                res.setTryAgainLater(false);
                            }
                            return res;
                        }
                    }

                }
                
                String saleID = null;
                String returnedTransactionRef = null;
                String returnedResponseCode = null;
                String returnedResponseDescription = null;

                double returnedAmount = 0;
                String returnedPaymentRef = null;
                JsonElement je;

                if (jsonObj == null || status200OK.equalsIgnoreCase("fail")) {
                    log.debug("Access Bank has not processed transaction yet: {}", transactionDetails);
                    StringBuilder avpBuilder = new StringBuilder();
                    avpBuilder.append("fullResponse=").append(StringEscapeUtils.escapeHtml(transactionDetails));
                    res.setGatewayResponse(avpBuilder.toString());
                    res.setSuccess(false);
                    res.setTryAgainLater(true);
                    if (dbSale.getPaymentGatewayPollCount() > 45) {//15min=30x10sec + 15x60sec
                        //The transaction should have transitioned to proper format by now, otherwise Access Bank really doesn't know about this record
                        res.setTryAgainLater(false);
                    }
                    return res;
                }

                for (Map.Entry element : jsonObj.entrySet()) {

                    if (element.getKey().equals("tx_ref")) {
                        je = jsonObj.get("tx_ref");
                        if (!je.isJsonNull()) {
                            saleID = je.getAsString();
                        }
                    }
                    
                    if (element.getKey().equals("amount")) {
                        je = jsonObj.get("amount");
                        if (!je.isJsonNull()) {
                            returnedAmount = je.getAsDouble();
                        }
                    }
                    
                    if (element.getKey().equals("status")) {
                        je = jsonObj.get("status");
                        if (!je.isJsonNull()) {
                            actualTransactionStatus = je.getAsString();
                        }
                    }
                    
                    if (element.getKey().equals("created_at")) {
                        je = jsonObj.get("created_at");
                        if (!je.isJsonNull()) {
                            returnedDatetime = je.getAsString();
                        }
                    }
                    
                    if (element.getKey().equals("narration")) {
                        je = jsonObj.get("narration");
                        if (!je.isJsonNull()) {
                            returnedResponseDescription = je.getAsString();
                        }
                    }
                                        
                      if (element.getKey().equals("id")) {
                        je = jsonObj.get("id");
                        if (!je.isJsonNull()) {
                            returnedTransactionRef = je.getAsString();
                        }
                    }
                    
                    
                 /* 
                    

                    if (element.getKey().equals("gateway_response")) {
                        je = jsonObj.get("gateway_response");
                        if (je.isJsonNull()) {
                            continue;
                        }
                        JsonObject gateJO = je.getAsJsonObject();
                        if (gateJO == null) {
                            continue;
                        }
                        JsonElement gateJe;
                        if (!gateJO.isJsonNull()) {
                            for (Map.Entry gateElement : gateJO.entrySet()) {

                                if (gateElement.getKey().equals("ResponseCode")) {
                                    gateJe = gateJO.get("ResponseCode");
                                    if (!gateJe.isJsonNull()) {
                                        returnedResponseCode = gateJe.getAsString();
                                    }
                                }

                                if (gateElement.getKey().equals("ResponseDescription")) {
                                    gateJe = gateJO.get("ResponseDescription");
                                    if (!gateJe.isJsonNull()) {
                                        returnedResponseDescription = gateJe.getAsString();
                                    }
                                }
                            }
                        }
                    }*/
                }

                StringBuilder avpBuilder = new StringBuilder();

                avpBuilder.append("orderId=");
                avpBuilder.append(saleID);
                avpBuilder.append("\r\n");
                
                avpBuilder.append("status=");
                avpBuilder.append(actualTransactionStatus);
                avpBuilder.append("\r\n");

                avpBuilder.append("transactionRef=");
                avpBuilder.append(returnedTransactionRef);
                avpBuilder.append("\r\n");

         /*       avpBuilder.append("cardLocale=");
                avpBuilder.append(cardLocale);
                res.setInfo("cardLocale:" + cardLocale);
                avpBuilder.append("\r\n");
        */

                

                //Helps to keep track of the transition of statuses during the life cycle of this transaction
                String gatewayResponse = dbSale.getPaymentGatewayResponse() == null ? "" : dbSale.getPaymentGatewayResponse();
                if (Utils.getValueFromCRDelimitedAVPString(gatewayResponse, "statusHistory") != null) {
                    String statusHistory = Utils.getValueFromCRDelimitedAVPString(gatewayResponse, "statusHistory");
                    if (!statusHistory.contains(actualTransactionStatus)) {//Store distinct statuses
                        avpBuilder.append("statusHistory=");
                        avpBuilder.append(statusHistory).append(",").append(actualTransactionStatus);
                        avpBuilder.append("\r\n");
                    }
                } else {
                    avpBuilder.append("statusHistory=");
                    avpBuilder.append(actualTransactionStatus);
                    avpBuilder.append("\r\n");
                }

                avpBuilder.append("responseCode=");
                avpBuilder.append(returnedResponseCode);
                avpBuilder.append("\r\n");

                avpBuilder.append("responseDescription=");
                avpBuilder.append(returnedResponseDescription);
                avpBuilder.append("\r\n");

                avpBuilder.append("dateTime=");
                avpBuilder.append(returnedDatetime);
                avpBuilder.append("\r\n");

                avpBuilder.append("amount=");
                avpBuilder.append(returnedAmount);
                avpBuilder.append("\r\n");

                avpBuilder.append("paymentRef=");
                avpBuilder.append(returnedPaymentRef);
                avpBuilder.append("\r\n");

        /*      avpBuilder.append("cardLocale=");//Determines criteria for calculating transaction fees: 3% Intl Cards & 0.75% Local Cards
                avpBuilder.append(cardLocale);
                avpBuilder.append("\r\n");
        */
                avpBuilder.append("fullResponse=");
                avpBuilder.append(StringEscapeUtils.escapeHtml(transactionDetails));

                res.setGatewayResponse(avpBuilder.toString());
                res.setTransferredAmountCents(returnedAmount * 100);
                res.setPaymentGatewayTransactionId(returnedTransactionRef);
            } catch (Exception ex) {
                log.warn("AccessBankPaymentGateway error [{}]", ex.toString());
                log.warn("Error: ", ex);
                res.setGatewayResponse(StringEscapeUtils.escapeHtml(transactionDetails));
                res.setSuccess(false);
                res.setTryAgainLater(true);
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "POS", "AccessBankPaymentGateway failed to process[" + transactionDetails + "] reason: " + ex.toString());
                return res;
            }

            if (status200OK.equalsIgnoreCase("success") && actualTransactionStatus.equalsIgnoreCase("successful")) {//STATUS attribute of data array must be 'successful' to give benefits
                log.debug("Access bank transaction completed, front-end portal call: [{}], paid: [{}]", status200OK, actualTransactionStatus);
                res.setSuccess(true);
                res.setTryAgainLater(false);
            } else if (status200OK.equalsIgnoreCase("success") && actualTransactionStatus.equalsIgnoreCase("failed")) {
                log.debug("Access bank transaction completed, front-end portal call:[{}], paid[{}]", status200OK, actualTransactionStatus);
                res.setSuccess(false);
                res.setTryAgainLater(false);
            } else if (status200OK.equalsIgnoreCase("success") && (actualTransactionStatus.equalsIgnoreCase("pending") || actualTransactionStatus.equalsIgnoreCase("processing"))) {
                log.debug("Access bank transaction completed, front-end portal call:[{}], paid[{}]. Going to retry this transaction.", status200OK, actualTransactionStatus);
                res.setTryAgainLater(true);
                res.setSuccess(false);
            } else {
                log.debug("We should not enter this block if all was well, front-end portal call:[{}], paid[{}]. Going to retry this transaction.", status200OK, actualTransactionStatus);
                res.setTryAgainLater(true);
                res.setSuccess(false);
                if (dbSale.getPaymentGatewayPollCount() > 45) {//15min=30x10sec + 15x60sec
                    //The transaction should have transitioned to paid by now else the transaction was not really paid/completed
                    res.setTryAgainLater(false);
                }
            }
        } catch (Exception ex) {
            log.warn("System error in Access Bank plugin: ", ex);
            res.setGatewayResponse(ex.toString());
            res.setSuccess(false);
            res.setTryAgainLater(true);
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "Error in Access Bank PaymentGateway plugin: " + ex.toString());
        }

        log.debug("Exiting getPaymentGatewayResult for AccessBankPaymentGateway");

        return res;
    }

    @Override
    public void init(EntityManager em) {
        merchantId = BaseUtils.getSubProperty("env.pgw.access.config", "ProfileID");
        paymentGatewayPostURL = BaseUtils.getSubProperty("env.pgw.access.config", "PaymentGatewayPostURL");
        username = BaseUtils.getSubProperty("env.pgw.access.config", "Username");
        password = BaseUtils.getSubProperty("env.pgw.access.config", "Password");
        clientId = BaseUtils.getSubProperty("env.pgw.access.config", "ClientId");
        publicKey = BaseUtils.getSubProperty("env.pgw.access.config", "PublicKey");
        clientSecret = BaseUtils.getSubProperty("env.pgw.access.config", "ClientSecret");
        statusQueryURL = BaseUtils.getSubProperty("env.pgw.access.config", "StatusQueryURL");
        callbackURL = BaseUtils.getSubProperty("env.pgw.access.config", "CallbackURL");
        oauth20TokenURL = BaseUtils.getSubProperty("env.pgw.access.config", "Oauth20TokenURL");
        accountId = BaseUtils.getSubProperty("env.pgw.access.config", "AccountId");        
    }

    @Override
    public String getGatewayURL(Sale dbSale) {
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
        StringBuilder sb = new StringBuilder();
        sb.append("POST: "); 
        sb.append("public_key=");
        sb.append(publicKey);
        sb.append(",payment_options=");
        sb.append("card");
        sb.append(",customer[email]=");
        sb.append(dbSale.getExtraInfo());
        sb.append(",customer[phone_number]=");
        sb.append(dbSale.getRecipientPhoneNumber());
        sb.append(",customer[name]=");
        sb.append(dbSale.getRecipientName());
        sb.append(",tx_ref=");
        sb.append(dbSale.getSaleId());
        sb.append(",amount=");
        sb.append(dbSale.getSaleTotalCentsIncl().divide(new BigDecimal(100)));        
        sb.append(",currency=");
        sb.append("NGN");               
        sb.append(",account_id=");        
        sb.append(dbSale.getRecipientAccountId());
        sb.append(",customizations[title]=");
        sb.append("Smile Communications Payment");
        sb.append(",customizations[description]=");
        sb.append("Smile data and voice services");
        sb.append(",customizations[logo]=");
        sb.append("https://www.smile.com.ng/scp/images/smile-logo.svg");
        sb.append(",redirect_url=");
        sb.append(callbackURL.replaceAll("OrderId#", "OrderId#" + dbSale.getSaleId()));//e.g. https://154.73.71.2/scp/PaymentGateway.action?processBankTransaction#&saleId#1234       
        return sb.toString();
    }
    

    @Override
    public long getAccountId() {
        return Long.valueOf(accountId);
    }

    @Override
    public void processPaymentNotification(int saleId, double cashReciepted, String transactionId, String paymentGatewayExtraData) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addPaymentGatewaySpecificsOnTicket(NewTTIssue tt, String errorMessage) {
        JiraField jiraF = new JiraField();

        jiraF.setFieldName("TT_FIXED_FIELD_Description");
        jiraF.setFieldType("TT_FIXED_FIELD");
        StringBuilder description = new StringBuilder("{panel:title=(!) ");
        description.append("Warning: Access Bank is currently not available to query transaction statuses");
        description.append("|borderStyle=dashed|titleBGColor=red}");
        description.append("Access Bank interface to query transaction statuses is currently not available\n")
                .append("Customers using MySmile or Xpress recharge to recharge would be affected\n").append("{panel}");
        jiraF.setFieldValue(description.toString());
        tt.getMindMapFields().getJiraField().add(jiraF);

        jiraF = new JiraField();
        jiraF.setFieldName("TT_FIXED_FIELD_Summary");
        jiraF.setFieldType("TT_FIXED_FIELD");
        String summary = "Access Bank API interface is down";
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
            String sms = "Access bank interface alert: Failing to get transaction status response when communicating with payment gateway. Please monitor environment to determine issue severity.";
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
            String bodyPart = ",<br/><br/><strong>Access bank interface alert:</strong> Interface is reporting instability, resulting in failure to get proper response when communicating with payment gateway."
                    + "<br/><strong>Caused by:</strong><br/>" + errorMessage
                    + "<br/><br/>Please monitor environment to determine issue severity.<br/><br/>";
            String subject = "Access bank interface instability alert";
            for (String to : configWatchers) {
                String fullBody = "Hi " + to + bodyPart;
                sendEmail(from, to, subject, fullBody);
            }
        } catch (Exception ex) {
        }
    }

    @Override
    public boolean isUp() {
        boolean isup = AccessBankPaymentGateway.ISUP_FAIL_COUNTER <= BaseUtils.getIntProperty("env.pos.paymentgateway.isup.fail.counter", 5);
        BaseUtils.sendStatistic("AccessBank", "PaymentGateway", "isup", isup ? 1 : 0, "POS");
        updateGatewayStatus("env.scp.accessbank.partner.integration.config", isup, getName());
        return isup;
    }

    @Override
    public String getName() {
        return "AccessBank";
    }
    
    public static String getPublicKey() {
        return publicKey;
    }
       

}
