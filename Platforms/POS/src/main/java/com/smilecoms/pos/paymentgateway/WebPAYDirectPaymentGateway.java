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
import com.smilecoms.commons.paymentgateway.InterswitchWebPAY;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.JiraField;
import com.smilecoms.commons.sca.MindMapFields;
import com.smilecoms.commons.sca.NewTTIssue;
import com.smilecoms.commons.util.Codec;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.pos.db.model.Sale;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.persistence.EntityManager;
import org.apache.commons.lang.StringEscapeUtils;

/**
 *
 * @author sabza
 */
public class WebPAYDirectPaymentGateway extends AbstractPaymentGateway implements PaymentGatewayPlugin {

    private static final String WEBPAYDIRECT_CACHE_KEY = "interswitch_webpaydirect_error_key";
    private static int ISUP_FAIL_COUNTER = 0;
    private final DecimalFormat df = new DecimalFormat("#");
    private static String callbackURL;
    private static String paymentGatewayPostURL;
    private static String clientSecret;
    private static String statusQueryURL;
    private static String accountId;
    private static String currencyCode;
    private static String productId;
    private static String marchentCode;
    private static String paymentItemId;
    private static String pendingPaymentStatuses;

//    @Override
//    public PaymentGatewayResult getPaymentGatewayResult(Sale dbSale) {
//        PaymentGatewayResult res = new PaymentGatewayResult();
//        //Request HASH Computation:
//        //product_id+tnx_ref+<mackey>
//
//        /*
//        transactionreference = 7645536 :: Original transaction reference sent in the original request
//        productid = 174 :: Product Identifier for PAYDirect.
//        mackey = AC43543FA32234HB23423AFH843535 :: 
//        
//        174+7645536+AC43543FA32234HB23423AFH843535
//         */
//        //https://stageserv.interswitchng.com/test_paydirect/api/v1/gettransaction.xml?productid=21&transactionreference=8421941122&amount=300000
//        /*
//            <TransactionQueryResponse xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema">
//                <ResponseCode>00</ResponseCode>
//                <ResponseDescription>Approved Successful</ResponseDescription>
//                <Amount>300000</Amount>
//                <CardNumber>6055</CardNumber>
//                <MerchantReference>8421941122</MerchantReference>
//                <PaymentReference>ZIB|WEB|VNA|15-10-2012|015933</PaymentReference>
//                <RetrievalReferenceNumber>000000538268</RetrievalReferenceNumber>
//                <SplitAccounts />
//                <TransactionDate>2012-10-15T11:07:54.547</TransactionDate>
//            </TransactionQueryResponse>
//
//         */
//        if (dbSale.getPaymentGatewayPollCount() < 2) {
//            StringBuilder avpBuilder = new StringBuilder();
//            avpBuilder.append("statusHistory=NOT_SENT\r\n");
//            res.setTryAgainLater(true);
//            res.setGatewayResponse(avpBuilder.toString());
//            res.setSuccess(false);
//            return res;
//        }
//        try {
//            String transactionDetails;
//
//            try {
//                Map<String, String> requestParams = new HashMap<>();
//                int saleId = dbSale.getSaleId();
//                requestParams.put("productid", productId);
//                requestParams.put("transactionreference", String.valueOf(saleId));
//
//                String amount = df.format(dbSale.getSaleTotalCentsIncl().doubleValue());
//                requestParams.put("amount", amount);
//
//                String hashText = productId + String.valueOf(saleId) + clientSecret;
//                InterswitchWebPAY webPAY = new InterswitchWebPAY();
//                hashText = getSHA512(hashText);
//                transactionDetails = StringEscapeUtils.unescapeXml(webPAY.getTransactionDetails(statusQueryURL, requestParams, hashText));
//                log.debug("HTTP client request returned successfully for getTransactionDetails. Transaction details returned[{}]", transactionDetails);
//
//            } catch (Exception ex) {
//                log.warn("System error in WebPAYDirect plugin: ", ex);
//                res.setGatewayResponse(ex.toString());
//                res.setSuccess(false);
//                res.setTryAgainLater(true);
//                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "Error in WebPAYDirect PaymentGateway plugin: " + ex.toString());
//                WebPAYDirectPaymentGateway.ISUP_FAIL_COUNTER++;
//                publishGatewayInAvailabilityNotice(ex.toString());
//                return res;
//            }
//
//            WebPAYDirectPaymentGateway.ISUP_FAIL_COUNTER = 0;
//            StringBuilder avpBuilder = new StringBuilder();
//
//            String status = Utils.getBetween(transactionDetails, "<ResponseCode>", "</ResponseCode>");
//            if (status == null) {
//                log.debug("WebPAYDirect did not return a status in their response. Assuming its pending");
//                status = "NUL";
//            }
//            String uniqueTransactionId = StringEscapeUtils.escapeXml(Utils.getBetween(transactionDetails, "<PaymentReference>", "</PaymentReference>"));
//            String responseTime = StringEscapeUtils.escapeXml(Utils.getBetween(transactionDetails, "<TransactionDate>", "</TransactionDate>"));
//            String amount = StringEscapeUtils.escapeXml(Utils.getBetween(transactionDetails, "<Amount>", "</Amount>"));
//            String paymentGatewayName = StringEscapeUtils.escapeXml(Utils.getBetween(transactionDetails, "<CardNumber>", "</CardNumber>"));
//            String orderId = StringEscapeUtils.escapeXml(Utils.getBetween(transactionDetails, "<MerchantReference>", "</MerchantReference>"));
//            String responseDescription = StringEscapeUtils.escapeXml(Utils.getBetween(transactionDetails, "<ResponseDescription>", "</ResponseDescription>"));
//
//            avpBuilder.append("orderId=");
//            avpBuilder.append(orderId == null ? dbSale.getSaleId() : orderId);
//            avpBuilder.append("\r\n");
//
//            avpBuilder.append("amount=");
//            avpBuilder.append(amount);
//            avpBuilder.append("\r\n");
//
//            avpBuilder.append("dateTime=");
//            avpBuilder.append(responseTime);
//            avpBuilder.append("\r\n");
//
//            avpBuilder.append("status=");
//            avpBuilder.append(status);
//            avpBuilder.append("\r\n");
//
//            //Helps to keep track of the transition of statuses during the life cycle of this transaction
//            String gatewayResponse = dbSale.getPaymentGatewayResponse() == null ? "" : dbSale.getPaymentGatewayResponse();
//            if (Utils.getValueFromCRDelimitedAVPString(gatewayResponse, "statusHistory") != null) {
//                String statusHistory = Utils.getValueFromCRDelimitedAVPString(gatewayResponse, "statusHistory");
//                if (!statusHistory.contains(status)) {//Store distinct statuses
//                    avpBuilder.append("statusHistory=");
//                    avpBuilder.append(statusHistory).append(",").append(status);
//                    avpBuilder.append("\r\n");
//                }
//            } else {
//                avpBuilder.append("statusHistory=");
//                avpBuilder.append(status);
//                avpBuilder.append("\r\n");
//            }
//
//            avpBuilder.append("transactionRef=");
//            avpBuilder.append(uniqueTransactionId);
//            avpBuilder.append("\r\n");
//
//            avpBuilder.append("paymentGate=");
//            avpBuilder.append(paymentGatewayName);
//            avpBuilder.append("\r\n");
//
//            String cardType = uniqueTransactionId == null ? "" : uniqueTransactionId;
//            String cardLocale = cardType.split("\\|")[0]; //FBN|WEB|WDM|16-11-2016|240544
//            res.setInfo(cardLocale == null ? "" : cardLocale);//Needed to determine if international/local card was used. Needed for calculation of transaction fees, see HBT-5441.
//
//            avpBuilder.append("responseDescription=");
//            avpBuilder.append(responseDescription);
//            avpBuilder.append("\r\n");
//
//            avpBuilder.append("fullResponse=");
//            avpBuilder.append(transactionDetails);
//
//            res.setPaymentGatewayTransactionId(uniqueTransactionId);
//            res.setTransferredAmountCents(Double.parseDouble(amount));//Already in cents
//            res.setGatewayResponse(avpBuilder.toString());
//
//            if (status.equalsIgnoreCase("00")) {
//                log.debug("WebPAYDirect transaction completed successfully: {}", status);
//                res.setSuccess(true);
//                res.setTryAgainLater(false);
//            } else {
//                //Z6 	Customer cancelled the transaction before payment was made.
//                //51	Insufficient funds to make payment.
//                //Z1 	This is a general response code. It implies there was an Error with the transaction (e.g. No card record, Pin tries exceeded, insufficient funds, etc.) More details will be provided in the ResponseDescription
//                String[] pendingPaymentStatus = pendingPaymentStatuses.split(",");
//                res.setSuccess(false);
//                res.setTryAgainLater(false);
//                for (String pendingStatus : pendingPaymentStatus) {
//                    if (pendingStatus.equals(status)) {
//                        res.setTryAgainLater(true);
//                        log.debug("WebPAYDirect transaction is configured to be pending for sale id [{}], will be retried: [{}], reason: [{}]", new Object[]{dbSale.getSaleId(), status, responseDescription});
//                        break;
//                    }
//                }
//                log.warn("WebPAYDirect bank transaction failed completely for sale id [{}], cannot be retried ever again: [{}], reason: [{}]", new Object[]{dbSale.getSaleId(), status, responseDescription});
//            }
//
//        } catch (Exception ex) {
//            String transactionDetails = ex.toString();
//            log.warn("For some reason we got an error processing results: {}", transactionDetails);
//            res.setSuccess(false);
//            res.setTryAgainLater(true);
//            String avp = "fullResponse" + "=" + transactionDetails;
//            res.setGatewayResponse(avp);
//            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "WebPAYDirectPaymentGateway failed to process[" + transactionDetails + "] reason: " + ex.toString());
//            return res;
//        }
//        log.debug("Exiting getPaymentGatewayResult for WebPAYDirectPaymentGateway");
//        return res;
//    }
    @Override
    public PaymentGatewayResult getPaymentGatewayResult(Sale dbSale) {
        PaymentGatewayResult res = new PaymentGatewayResult();
        try {

            if (dbSale.getPaymentGatewayPollCount() < BaseUtils.getIntProperty("env.pos.paymentgateway.webpaydirect.firstquery.delay", 4)) {
                //Dont check transaction status early as we might get FAILED, we wait 10sec.
                res.setTryAgainLater(true);
                res.setGatewayResponse("statusHistory=NOT_SENT");
                res.setSuccess(false);
                return res;
            }

            String transactionDetails;

            try {
                Map<String, String> requestParams = new HashMap<>();
                int saleId = dbSale.getSaleId();
                requestParams.put("merchantcode", marchentCode);
                requestParams.put("productid", productId);
                requestParams.put("transactionreference", String.valueOf(saleId));

                String amount = df.format(dbSale.getSaleTotalCentsIncl().doubleValue());
                requestParams.put("amount", amount);

                String hashText = productId + String.valueOf(saleId) + clientSecret;
                InterswitchWebPAY webPAY = new InterswitchWebPAY();
                hashText = getSHA512(hashText);
                transactionDetails = StringEscapeUtils.unescapeHtml(webPAY.getTransactionDetails(statusQueryURL, requestParams, hashText));
                transactionDetails = transactionDetails.replaceAll("\\|", Pattern.quote("|"));
                log.debug("HTTP client request returned successfully for getTransactionDetails. Transaction details returned[{}]", transactionDetails);

            } catch (Exception ex) {
                log.warn("System error in WebPAYDirect plugin: ", ex);
                res.setGatewayResponse(ex.toString());
                res.setSuccess(false);
                res.setTryAgainLater(true);
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "Error in WebPAYDirect PaymentGateway plugin: " + ex.toString());
                WebPAYDirectPaymentGateway.ISUP_FAIL_COUNTER++;
                
                boolean isup = WebPAYDirectPaymentGateway.ISUP_FAIL_COUNTER <= BaseUtils.getIntProperty("env.pos.paymentgateway.isup.fail.counter", 5);
                publishGatewayInAvailabilityNotice(getName(), isup, WEBPAYDIRECT_CACHE_KEY, ex.toString());
                return res;
            }
            WebPAYDirectPaymentGateway.ISUP_FAIL_COUNTER = 0;

            String returnedResponseCode = "";
            String returnedResponseDescription = "";

            try {

                JsonParser jsonParser = new JsonParser();
                JsonObject transactionAsJsonObject = jsonParser.parse(transactionDetails).getAsJsonObject();

                String saleID = null;
                String returnedTransactionRef = null;
                String retrievalReferenceNumber = null;
                String leadBankCbnCode = null;
                String leadBankName = null;
                String cardNumber = null;
                double returnedAmount = 0;
                String returnedDatetime = null;
                String returnedPaymentRef = null;
                JsonElement je;

                for (Map.Entry element : transactionAsJsonObject.entrySet()) {

                    if (element.getKey().equals("MerchantReference")) {
                        je = transactionAsJsonObject.get("MerchantReference");
                        if (!je.isJsonNull()) {
                            saleID = je.getAsString();
                        }
                    }
                    if (element.getKey().equals("PaymentReference")) {
                        je = transactionAsJsonObject.get("PaymentReference");
                        if (!je.isJsonNull()) {
                            returnedTransactionRef = je.getAsString();
                        }
                    }
                    if (element.getKey().equals("ResponseCode")) {
                        je = transactionAsJsonObject.get("ResponseCode");
                        if (!je.isJsonNull()) {
                            returnedResponseCode = je.getAsString();
                        }
                    }
                    if (element.getKey().equals("RetrievalReferenceNumber")) {
                        je = transactionAsJsonObject.get("RetrievalReferenceNumber");
                        if (!je.isJsonNull()) {
                            retrievalReferenceNumber = je.getAsString();
                        }
                    }
                    if (element.getKey().equals("Amount")) {
                        je = transactionAsJsonObject.get("Amount");
                        if (!je.isJsonNull()) {
                            returnedAmount = je.getAsDouble();
                        }
                    }
                    if (element.getKey().equals("ResponseDescription")) {
                        je = transactionAsJsonObject.get("ResponseDescription");
                        if (!je.isJsonNull()) {
                            returnedResponseDescription = je.getAsString();
                        }
                    }
                    if (element.getKey().equals("TransactionDate")) {
                        je = transactionAsJsonObject.get("TransactionDate");
                        if (!je.isJsonNull()) {
                            returnedDatetime = je.getAsString();
                        }
                    }
                    if (element.getKey().equals("LeadBankCbnCode")) {
                        je = transactionAsJsonObject.get("LeadBankCbnCode");
                        if (!je.isJsonNull()) {
                            leadBankCbnCode = je.getAsString();
                        }
                    }
                    if (element.getKey().equals("LeadBankName")) {
                        je = transactionAsJsonObject.get("LeadBankName");
                        if (!je.isJsonNull()) {
                            leadBankName = je.getAsString();
                        }
                    }
                    if (element.getKey().equals("CardNumber")) {
                        je = transactionAsJsonObject.get("CardNumber");
                        if (!je.isJsonNull()) {
                            cardNumber = je.getAsString();
                        }
                    }
                }

                StringBuilder avpBuilder = new StringBuilder();

                avpBuilder.append("orderId=");
                avpBuilder.append(saleID);
                avpBuilder.append("\r\n");

                avpBuilder.append("paymentReference=");
                avpBuilder.append(returnedTransactionRef);
                avpBuilder.append("\r\n");

                avpBuilder.append("responseCode=");
                avpBuilder.append(returnedResponseCode);
                avpBuilder.append("\r\n");

                //Helps to keep track of the transition of statuses during the life cycle of this transaction
                String gatewayResponse = dbSale.getPaymentGatewayResponse() == null ? "" : dbSale.getPaymentGatewayResponse();
                if (Utils.getValueFromCRDelimitedAVPString(gatewayResponse, "statusHistory") != null) {
                    String statusHistory = Utils.getValueFromCRDelimitedAVPString(gatewayResponse, "statusHistory");
                    if (!statusHistory.contains(returnedResponseCode)) {//Store distinct statuses
                        avpBuilder.append("statusHistory=");
                        avpBuilder.append(statusHistory).append(",").append(returnedResponseCode);
                        avpBuilder.append("\r\n");
                    }
                } else {
                    avpBuilder.append("statusHistory=");
                    avpBuilder.append(returnedResponseCode);
                    avpBuilder.append("\r\n");
                }

                avpBuilder.append("leadBankName=");
                avpBuilder.append(leadBankName);
                avpBuilder.append("\r\n");

                avpBuilder.append("responseDescription=");
                avpBuilder.append(returnedResponseDescription);
                avpBuilder.append("\r\n");

                avpBuilder.append("transactionDate=");
                avpBuilder.append(returnedDatetime);
                avpBuilder.append("\r\n");

                avpBuilder.append("amount=");
                avpBuilder.append(returnedAmount);
                avpBuilder.append("\r\n");

                avpBuilder.append("leadBankCbnCode=");
                avpBuilder.append(leadBankCbnCode);
                avpBuilder.append("\r\n");

                avpBuilder.append("cardNumber=");
                avpBuilder.append(cardNumber);
                avpBuilder.append("\r\n");

                avpBuilder.append("fullResponse=");
                avpBuilder.append(transactionDetails);

                //4:28:58 PM] live:belloosa86: FOr international
                //[4:29:05 PM] live:belloosa86: [15:23:05] Sabelo Dlangamandla: <PaymentReference>FBN|WEB|WDM|16-11-2016|240544</PaymentReference>
                //[4:29:10 PM] live:belloosa86: This
                //[4:29:11 PM] live:belloosa86: [15:29:05] John Bello: FBN|
                //[4:29:19 PM] live:belloosa86: Will start with VIA - for VISA
                //[4:29:20 PM] live:belloosa86: or
                //[4:30:37 PM] live:belloosa86: INB - for MasterCard
                //[4:30:49 PM] live:belloosa86: THis means that the international option was used
                //[4:30:56 PM] live:belloosa86: and it went through or was successful
                String cardType = returnedTransactionRef == null ? "" : returnedTransactionRef;
                String cardLocale = cardType.split(Pattern.quote("|"))[0]; //FBN|WEB|WDM|16-11-2016|240544
                res.setInfo(cardLocale == null ? "" : cardLocale);//Needed to determine if international/local card was used. Needed for calculation of transaction fees, see HBT-5441.

                res.setGatewayResponse(avpBuilder.toString());
                res.setTransferredAmountCents(returnedAmount);
                res.setPaymentGatewayTransactionId(returnedTransactionRef);
            } catch (Exception ex) {
                log.warn("WebPAYDirect error [{}]", ex.toString());
                log.warn("Error: ", ex);
                res.setGatewayResponse(transactionDetails);
                res.setSuccess(false);
                res.setTryAgainLater(true);
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "WebPAYDirect failed to process[" + transactionDetails + "] reason: " + ex.toString());
                return res;
            }

            if (returnedResponseCode.equalsIgnoreCase("00")) {
                log.debug("WebPAYDirect transaction completed successfully: {}", returnedResponseCode);
                res.setSuccess(true);
                res.setTryAgainLater(false);
            } else {
                //Z6 	Customer cancelled the transaction before payment was made.
                //51	Insufficient funds to make payment.
                //Z1 	This is a general response code. It implies there was an Error with the transaction (e.g. No card record, Pin tries exceeded, insufficient funds, etc.) More details will be provided in the ResponseDescription
                String[] pendingPaymentStatus = pendingPaymentStatuses.split(",");
                res.setSuccess(false);
                res.setTryAgainLater(false);
                for (String pendingStatus : pendingPaymentStatus) {
                    if (pendingStatus.equals(returnedResponseCode)) {
                        res.setTryAgainLater(true);
                        log.debug("WebPAYDirect transaction is configured to be pending for sale id [{}], will be retried: [{}], reason: [{}]", new Object[]{dbSale.getSaleId(), returnedResponseCode, returnedResponseDescription});
                        break;
                    }
                }
                log.warn("WebPAYDirect transaction failed completely for sale id [{}], cannot be retried ever again: [{}], reason: [{}]", new Object[]{dbSale.getSaleId(), returnedResponseCode, returnedResponseDescription});
            }

        } catch (Exception ex) {
            log.warn("System error in WebPAYDirect plugin: ", ex);
            res.setGatewayResponse(ex.toString());
            res.setSuccess(false);
            res.setTryAgainLater(true);
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "Error in WebPAYDirect plugin: " + ex.toString());
        }

        log.debug("Exiting getPaymentGatewayResult for WebPAYDirect");

        return res;
    }

    @Override
    public void init(EntityManager em) {
        paymentGatewayPostURL = BaseUtils.getSubProperty("env.pgw.webpaydirect.config", "PaymentGatewayPostURL");
        clientSecret = BaseUtils.getSubProperty("env.pgw.webpaydirect.config", "ClientSecret");
        statusQueryURL = BaseUtils.getSubProperty("env.pgw.webpaydirect.config", "StatusQueryURL");
        callbackURL = BaseUtils.getSubProperty("env.pgw.webpaydirect.config", "CallbackURL");
        accountId = BaseUtils.getSubProperty("env.pgw.webpaydirect.config", "AccountId");
        currencyCode = BaseUtils.getSubProperty("env.pgw.webpaydirect.config", "CurrencyCode");
        productId = BaseUtils.getSubProperty("env.pgw.webpaydirect.config", "ProductId");
        marchentCode = BaseUtils.getSubProperty("env.pgw.webpaydirect.config", "MerchantCode");
        paymentItemId = BaseUtils.getSubProperty("env.pgw.webpaydirect.config", "PaymentItemId");
        pendingPaymentStatuses = BaseUtils.getSubProperty("env.pgw.webpaydirect.config", "PendingPaymentStatuses");//Currently not sure which are valid statuses to monitor for Pending.
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

        //String to hash: tnx_ref+product_id+pay_item_id+amount+site_redirect_url+<mackey>

        /*
        txn_ref = 7645536 :: This Reference Number must be generated by your web site/portal and a unique value must be sent for each transaction
        product_id = 174 :: Product Identifier for PAYDirect.
        pay_item_id = 23 :: PAYDirect Payment Item ID
        amount = 1000000 :: Transaction Amount in small (kobo) denomination
        site_redirect_url = http://www.yoursite.com/return/ :: URL of the page on your web site/portal user is to be redirected to after payment
        mackey = AC43543FA32234HB23423AFH843535 :: 
        
        String to hash: 7645536174231000000http://www.yoursite.com/return/AC43543FA32234HB23423AFH843535
         */
        Customer cust = getCustomer(dbSale.getRecipientCustomerId());
        String amount = df.format(dbSale.getSaleTotalCentsIncl().doubleValue());
        String saleId = String.valueOf(dbSale.getSaleId());

        String callBack = callbackURL;
        //https://154.73.71.2/scp/PaymentGateway.action?processBankTransaction=&OrderId=1234
        callBack = callBack.replaceAll("#", "=");
        callBack = callBack.replaceAll("OrderId=", "OrderId=" + saleId);//We need to sign the request with SHA
        String textToHash = saleId + productId + paymentItemId + amount + callBack + clientSecret;

        StringBuilder sb = new StringBuilder();
        sb.append("POST: ");
        sb.append("product_id=");//M --> Provided by Interswitch
        sb.append(productId);
        sb.append(",pay_item_id=");//M --> Provided by Interswitch
        sb.append(paymentItemId);
        sb.append(",merchant_code=");//M --> Provided by Interswitch
        sb.append(marchentCode);
        sb.append(",amount=");//M
        sb.append(amount);
        sb.append(",currency=");//M
        sb.append(currencyCode);
        sb.append(",site_redirect_url=");//M
        sb.append(callbackURL.replaceAll("OrderId#", "OrderId#" + saleId));//e.g. https://154.73.71.2/scp/PaymentGateway.action?processBankTransaction#&saleId#1234

        sb.append(",hash=");//M
        sb.append(getSHA512(textToHash));
        sb.append(",txn_ref=");//M
        sb.append(saleId);
        sb.append(",cust_id=");//O
        sb.append(accountId);
        sb.append(",cust_id_desc=");//O
        sb.append("Customer's Smile Account number");
        sb.append(",cust_name=");//O
        sb.append(cust.getFirstName()).append(" ").append(cust.getLastName());
        //sb.append(",prod=");
        //sb.append("Smile Shop");

        return sb.toString();
    }

    @Override
    public long getAccountId() {
        return Long.valueOf(accountId);
    }

    @Override
    public void processPaymentNotification(int saleId, double cashReciepted, String transactionId, String paymentGatewayExtraData) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private String getSHA512(String text) {
        String hash = "";
        try {
            MessageDigest mda = MessageDigest.getInstance("SHA-512");
            byte[] digest = mda.digest(text.getBytes());
            hash = Codec.binToHexString(digest);
        } catch (NoSuchAlgorithmException ex) {
            log.warn("Error occured trying to hash string message digest: {}", ex);
        }
        return hash.toUpperCase();
    }

    @Override
    public void addPaymentGatewaySpecificsOnTicket(NewTTIssue tt, String errorMessage) {
        JiraField jiraF = new JiraField();

        jiraF.setFieldName("TT_FIXED_FIELD_Description");
        jiraF.setFieldType("TT_FIXED_FIELD");
        StringBuilder description = new StringBuilder("{panel:title=(!) ");
        description.append("Warning: Interswitch WebPAYDirect is reporting instability");
        description.append("|borderStyle=dashed|titleBGColor=red}");
        description.append("Interswitch WebPAYDirect interface to query transaction statuses is reporting instability\n")
                .append("Customers using MySmile or Xpress recharge to recharge would be affected\n").append("{panel}");
        jiraF.setFieldValue(description.toString());
        tt.getMindMapFields().getJiraField().add(jiraF);

        jiraF = new JiraField();
        jiraF.setFieldName("TT_FIXED_FIELD_Summary");
        jiraF.setFieldType("TT_FIXED_FIELD");
        String summary = "Interswitch WebPAYDirect API interface instability";
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
            String sms = "Interswitch WebPAYDirect interface alert: Environment instability. Please monitor environment to determine issue severity.";
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
            String bodyPart = ",<br/><br/><strong>Interswitch WebPAYDirect interface alert:</strong> Interface is reporting instability, resulting in failure to get proper response when communicating with payment gateway."
                    + "<br/><strong>Caused by:</strong><br/>" + errorMessage
                    + "<br/><br/>Please monitor environment to determine issue severity.<br/><br/>";
            String subject = "WebPAYDirect interface instability alert";
            for (String to : configWatchers) {
                String fullBody = "Hi " + to + bodyPart;
                sendEmail(from, to, subject, fullBody);
            }
        } catch (Exception ex) {
        }
    }

    @Override
    public boolean isUp() {
        boolean isup = WebPAYDirectPaymentGateway.ISUP_FAIL_COUNTER <= BaseUtils.getIntProperty("env.pos.paymentgateway.isup.fail.counter", 5);
        BaseUtils.sendStatistic("WebPAYDirect", "PaymentGateway", "isup", isup ? 1 : 0, "POS");
        updateGatewayStatus("env.scp.webpaydirect.partner.integration.config", isup, getName());
        return isup;
    }

    @Override
    public String getName() {
        return "WebPAY";
    }

}
