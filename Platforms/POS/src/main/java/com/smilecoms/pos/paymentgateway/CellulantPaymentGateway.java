/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos.paymentgateway;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.paymentgateway.Cellulant;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.JiraField;
import com.smilecoms.commons.sca.MindMapFields;
import com.smilecoms.commons.sca.NewTTIssue;
import com.smilecoms.commons.util.Codec;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.pos.POSManager;
import com.smilecoms.pos.db.model.Sale;
import com.smilecoms.pos.db.model.SaleRow;
import com.smilecoms.pos.db.op.DAO;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.persistence.EntityManager;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author sabza
 */
public class CellulantPaymentGateway extends AbstractPaymentGateway implements PaymentGatewayPlugin {

    private static final String CELLULANT_ISUP_CACHE_KEY = "cellulant_isup_error_key";
    private static final String ALGORITHM_AES256 = "AES/CBC/NoPadding";
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static int ISUP_FAIL_COUNTER = 0;
    private final DecimalFormat df = new DecimalFormat("#");
    EntityManager em = null;

    @Override
    public PaymentGatewayResult getPaymentGatewayResult(Sale dbSale) {
        PaymentGatewayResult res = new PaymentGatewayResult();
        res.setTryAgainLater(true);
        res.setSuccess(false);
        return res;
    }

    private static String currencyCode;
    private static String paymentGatewayPostURL;
    private static String callbackURL;
    private static String secretKey;
    private static String accountId;
    private static String pendingPaymentStatuses;
    private static String statusQueryURL;
    private static String ipnURL;
    private static String countryCode;
    private static String dueDateHour;
    private static String IV;
    private static String accessKey;
    private static String serviceCode;
    private static String successfulTransactionCode;
    private static String gatewayCode;

    @Override
    public void init(EntityManager em) {
        currencyCode = BaseUtils.getSubProperty("env.pgw.cellulant.config", "CurrencyCode");
        paymentGatewayPostURL = BaseUtils.getSubProperty("env.pgw.cellulant.config", "PaymentGatewayPostURL");
        callbackURL = BaseUtils.getSubProperty("env.pgw.cellulant.config", "CallbackURL");
        secretKey = BaseUtils.getSubProperty("env.pgw.cellulant.config", "SecreteKey");
        accountId = BaseUtils.getSubProperty("env.pgw.cellulant.config", "AccountId");
        pendingPaymentStatuses = BaseUtils.getSubProperty("env.pgw.cellulant.config", "PendingPaymentStatuses");
        statusQueryURL = BaseUtils.getSubProperty("env.pgw.cellulant.config", "StatusQueryURL");
        ipnURL = BaseUtils.getSubProperty("env.pgw.cellulant.config", "IPNURL");
        countryCode = BaseUtils.getSubProperty("env.pgw.cellulant.config", "CountryCode");
        dueDateHour = BaseUtils.getSubProperty("env.pgw.cellulant.config", "DueDateHour");
        IV = BaseUtils.getSubProperty("env.pgw.cellulant.config", "IV");
        accessKey = BaseUtils.getSubProperty("env.pgw.cellulant.config", "AccessKey");
        serviceCode = BaseUtils.getSubProperty("env.pgw.cellulant.config", "ServiceCode");
        successfulTransactionCode = BaseUtils.getSubProperty("env.pgw.cellulant.config", "SuccessCode");
        gatewayCode = BaseUtils.getSubProperty("env.pgw.cellulant.config", "GatewayCode");
        this.em = em;
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
        sb.append("GET: ");
        sb.append("accessKey=");
        sb.append(accessKey);
        sb.append(",countryCode=");
        sb.append(countryCode);
        sb.append(",params=");

        SecretKeySpec key = null;
        IvParameterSpec iv = null;
        try {
            key = createKey(secretKey);
        } catch (Exception ex) {
            log.warn("Failed to get secrete key");
        }
        try {
            iv = createIV(IV);
        } catch (Exception ex) {
            log.warn("Failed to create IV");
        }
        try {
            log.warn("Create encrypted message");
            sb.append(getEncryptedMessage(getTextToHash(dbSale), key, iv));
        } catch (Exception ex) {
            log.warn("Failed to get encrypted data");
        }
        return sb.toString();
    }

    @Override
    public String getName() {
        return "Cellulant";
    }

    @Override
    public long getAccountId() {
        return Long.valueOf(accountId);
    }

    @Override
    public boolean isUp() {
        boolean isup = CellulantPaymentGateway.ISUP_FAIL_COUNTER <= BaseUtils.getIntProperty("env.pos.paymentgateway.isup.fail.counter", 5);
        BaseUtils.sendStatistic("Cellulant", "PaymentGateway", "isup", isup ? 1 : 0, "POS");
        updateGatewayStatus("env.scp.cellulant.partner.integration.config", isup, getName());
        return isup;
    }

    @Override
    public void processPaymentNotification(int saleId, double cashReciepted, String transactionId, String paymentGatewayExtraData) throws Exception {
        Sale sale = DAO.getSale(em, saleId);
        if (!sale.getPaymentGatewayCode().equals(gatewayCode)) {
            /*Just make sure we're processing transaction initiated by Cellulant*/
            throw new Exception("Sale not initiated by payment gateway");
        }

        if (sale.getStatus().equals(POSManager.PAYMENT_STATUS_PAID)) {
            log.debug("Sale has already been processed");
            return;
        }
        processExtraData(saleId, paymentGatewayExtraData);

    }

    private static Cipher getCipher(int encryptMode, SecretKeySpec secretKeySpec, IvParameterSpec iv) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM_AES256);
        cipher.init(encryptMode, secretKeySpec, iv);
        return cipher;
    }

    private SecretKeySpec createKey(String password) throws UnsupportedEncodingException {
        SecretKeySpec key = new SecretKeySpec(password.getBytes("UTF-8"), "AES");
        log.info("Finished creating a key");
        return key;
    }

    public IvParameterSpec createIV(String iv) throws UnsupportedEncodingException {
        //The IV doesn't have to be secret, but it has to be unpredictable for CBC mode (and unique for CTR). It can be sent along with the ciphertext.
        //A common way to do this is by prefixing the IV to the ciphertext and slicing it off before decryption. It should be generated through SecureRandom
        log.info("Creating a createIV");
        //byte[] nonce = new byte[16];
        //new SecureRandom().nextBytes(nonce);
        //return new IvParameterSpec(nonce);
        return new IvParameterSpec(iv.getBytes("UTF-8"));//IV is supplied before-hand
    }

    /**
     * Takes message and encrypts with Key
     *
     * @param message String
     * @param secretKeySpec
     * @param iv
     * @return String Base64 encoded
     * @throws java.lang.Exception
     */
    private String getEncryptedMessage(String message, SecretKeySpec secretKeySpec, IvParameterSpec iv) throws Exception {
        Cipher cipher = getCipher(Cipher.ENCRYPT_MODE, secretKeySpec, iv);
        //byte[] encryptedTextBytes = cipher.doFinal(message.getBytes("UTF-8"));
        // Manually change the padding of the text to multiple of 16 bits.
        byte[] decryptedTextBytes = cipher.doFinal(padString(message).getBytes());
        String ecryptedData = bytesToHex(decryptedTextBytes);
        log.warn("Successfully encrypted data {}", ecryptedData);
        //return Codec.binToHexString(decryptedTextBytes);
        //return base64Encode(encryptedTextBytes);
        return ecryptedData;
    }

    /**
     * Takes Base64 encoded String and decodes with provided key
     *
     * @param message String encoded with Base64
     * @param secretKeySpec
     * @param iv
     * @return String
     * @throws java.lang.Exception
     */
    private String getDecryptedMessage(String message, SecretKeySpec secretKeySpec, IvParameterSpec iv) throws Exception {
        Cipher cipher = getCipher(Cipher.DECRYPT_MODE, secretKeySpec, iv);
        //byte[] encryptedTextBytes = decodeBase64(message);
        //byte[] decryptedTextBytes = cipher.doFinal(encryptedTextBytes);

        // Manually change the padding of the text to multiple of 16 bits.
        byte[] decryptedTextBytes = cipher.doFinal(padString(message).getBytes());

        return new String(decryptedTextBytes);

    }

    private String padString(String source) {
        char paddingChar = ' ';
        int size = 16;
        int x = source.length() % size;
        int padLength = size - x;
        for (int i = 0; i < padLength; i++) {
            source += paddingChar;
        }
        log.info("Padded string is [{}]", source);
        return source;
    }
    private static final Base64 BASE64 = new Base64();

    private static byte[] decodeBase64(String s) {
        return BASE64.decode(s.getBytes());
    }

    private static String base64Encode(byte[] b) {
        return new String(BASE64.encode(b));
    }

    private void doCallback(Sale dbSale) {
        try {
            Cellulant cellulant = new Cellulant();
            //String sQueryURL = statusQueryURL;
            Map<String, String> requestParams = new HashMap<>();
            JsonObject rqJson = new JsonObject();

            //rqJson.addProperty("status", dbSale.getStatus().equals("PD") ? 156 : 174);
            rqJson.addProperty("statusCode", dbSale.getStatus().equals("PD") ? 180 : 183);
            rqJson.addProperty("checkoutRequestID", dbSale.getExtTxid());
            rqJson.addProperty("checkoutRequestID", dbSale.getExtTxid());
            rqJson.addProperty("receiptNumber", dbSale.getStatus().equals("PD") ? String.valueOf(dbSale.getSaleId()) : "");
            String json = getObjectAsJsonString(rqJson);

            requestParams.put("countryCode", countryCode);
            requestParams.put("accessKey", accessKey);
            requestParams.put("params", getEncryptedMessage(json, createKey(secretKey), createIV(IV)));
            log.debug("Created encrypted doCallback data");

            cellulant.acknowledgePayment(statusQueryURL, requestParams);
            log.debug("HTTP client callback request returned successfully");
        } catch (Exception ex) {
            log.warn("System error in Cellulant plugin when doing a callback: ", ex);
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "Error in Cellulant PaymentGateway plugin when doing payment notification callback: " + ex.toString());
        }
    }

    private String getSHA256(String text) {
        String hash = "";
        try {
            MessageDigest mda = MessageDigest.getInstance("SHA-256");
            byte[] digest = mda.digest(text.getBytes());
            hash = Codec.binToHexString(digest);
        } catch (NoSuchAlgorithmException ex) {
            log.warn("Error occured trying to hash string message digest: {}", ex);
        }
        return hash.toUpperCase();
    }

    private String getTextToHash(Sale dbSale) {

        /*
            transactionID 	STRING 	The merchant's unique transaction identifier. 	TRUE 	N/A
            customerFirstName 	STRING 	The customer's first name. 	TRUE 	N/A
            customerLastName 	STRING 	The customer's last name. 	TRUE 	N/A
            MSISDN 	STRING 	The customer's mobile number. 	TRUE 	N/A
            customerEmail 	STRING 	The customer's email. 	TRUE 	N/A
            amount 	STRING 	The total amount that the customer is going to pay. 	TRUE 	N/A
            currency 	STRING 	The currency the amount passed is in. 	TRUE 	N/A
            reference 	STRING 	The account number/reference number for the transaction. 	TRUE 	N/A
            serviceCode 	STRING 	The merchant's service code. 	TRUE 	N/A
            productCode 	STRING 	The product code for the transaction. 	TRUE 	N/A
            dueDate 	STRING 	The transaction's due date. 	TRUE 	N/A
            serviceDescription 	STRING 	The transaction's narrative. 	TRUE 	N/A
            countryCode 	STRING 	The merchant's default country country code. 	TRUE 	N/A
            language 	STRING 	The merchant's language language code. 	TRUE 	N/A
            callBackUrl 	STRING 	The URL the express checkout will redirect to once the payment is finished whether successfully or not. 	TRUE 	N/A
            webhookPaymentUrl 	STRING 	This is the URL checkout will use to call the merchant with the relevant payment details for the merchant to acknowledge the payments 	TRUE 	N/A
            failedCallBackUrl 	STRING 	This is the URL checkout will call on a failure of the checkout. 	FALSE 	N/A
         */
        Customer cust = getCustomer(dbSale.getRecipientCustomerId());
        String amount = df.format(dbSale.getSaleTotalCentsIncl().divide(new BigDecimal(100)));
        String saleId = String.valueOf(dbSale.getSaleId());
        Collection<SaleRow> salesRowsAndSubRows = DAO.getSalesRowsAndSubRows(em, dbSale.getSaleId());
        String itemDescription = "";
        String productCode = "";
        for (SaleRow sr : salesRowsAndSubRows) {
            itemDescription = sr.getDescription();
            productCode = String.valueOf(sr.getSaleRowId());
        }

        JsonObject rqJson = new JsonObject();
        String callBack = callbackURL.replaceAll("OrderId#", "OrderId=" + saleId);
        callBack = callBack.replaceAll("#", "=");
        String webhookURL = ipnURL.replaceAll("orderId#", "orderId=" + saleId);
        webhookURL = webhookURL.replaceAll("#", "=");

        Calendar expiryDate = Calendar.getInstance();
        expiryDate.add(Calendar.HOUR, Integer.parseInt(dueDateHour));

        rqJson.addProperty("transactionID", saleId);
        rqJson.addProperty("customerFirstName", cust.getFirstName());
        rqJson.addProperty("customerLastName", cust.getLastName());
        rqJson.addProperty("MSISDN", getFriendlyPhoneNumber(Utils.getCleanDestination(cust.getAlternativeContact1())));
        rqJson.addProperty("customerEmail", cust.getEmailAddress());
        rqJson.addProperty("amount", amount);
        rqJson.addProperty("currency", currencyCode);
        rqJson.addProperty("reference", dbSale.getRecipientAccountId());
        rqJson.addProperty("serviceCode", serviceCode);
        rqJson.addProperty("productCode", productCode);
        rqJson.addProperty("dueDate", sdf.format(expiryDate.getTime()));

        rqJson.addProperty("serviceDescription", itemDescription);
        rqJson.addProperty("countryCode", countryCode);
        rqJson.addProperty("language", "en");
        rqJson.addProperty("callBackUrl", callBack);
        rqJson.addProperty("webhookPaymentUrl", webhookURL);
        rqJson.addProperty("failedCallBackUrl", callBack);

        String json = getObjectAsJsonString(rqJson);

        return json;
    }

    private String bytesToHex(byte[] data) {
        if (data == null) {
            return null;
        }
        int len = data.length;
        String string = "";
        for (int i = 0; i < len; i++) {
            if ((data[i] & 0xFF) < 16) {
                string = string + "0" + Integer.toHexString(data[i] & 0xFF);
            } else {
                string = string + Integer.toHexString(data[i] & 0xFF);
            }
        }
        return string;
    }

    private static String getFriendlyPhoneNumber(String destination) {
        //Get rid of domain if there is one in the destination
        int atLoc = destination.indexOf("@");
        if (atLoc != -1) {
            destination = destination.substring(0, atLoc);
        }
        return destination;
    }

    private void processExtraData(int saleId, String paymentGatewayExtraData) throws Exception {

        JsonObject jsonObj = getJsonObjectFromJsonString(paymentGatewayExtraData);
        JsonElement je;

        String returnedTransactionRef = null;
        double returnedAmount = 0;
        String returnedTransactionStatus = "";

        for (Map.Entry element : jsonObj.entrySet()) {

            if (element.getKey().equals("checkoutRequestID")) {
                je = jsonObj.get("checkoutRequestID");
                if (!je.isJsonNull()) {
                    returnedTransactionRef = je.getAsString();
                }
            }
            /*V1 :: if (element.getKey().equals("paymentStatusCode")) {
                je = jsonObj.get("paymentStatusCode");
                if (!je.isJsonNull()) {
                    returnedTransactionStatus = je.getAsString();
                }
            }*/

            //V2
            if (element.getKey().equals("requestStatusCode")) {
                je = jsonObj.get("requestStatusCode");
                if (!je.isJsonNull()) {
                    returnedTransactionStatus = je.getAsString();
                }
            }
            if (element.getKey().equals("requestAmount")) {
                je = jsonObj.get("requestAmount");
                if (!je.isJsonNull()) {
                    returnedAmount = je.getAsDouble();
                }
            }
        }

        doPaymentGatewayPostProcessing(saleId, returnedTransactionStatus, paymentGatewayExtraData);
        Sale sale = DAO.getSale(em, saleId);

        /*Send only successful transaction for post-processing*/
        if (!sale.getStatus().equals(POSManager.PAYMENT_STATUS_GW_FAIL_SMILE) && returnedTransactionStatus.equalsIgnoreCase(successfulTransactionCode)) {
            //if (returnedTransactionStatus.equalsIgnoreCase(successfulTransactionCode)) {
            List<Integer> saleAsList = new ArrayList<>();
            saleAsList.add(saleId);
            new POSManager().processPayment(em, saleAsList, (returnedAmount * 100), returnedTransactionRef, "PD", true, "Cellulant");
        }

    }

    private void doPaymentGatewayPostProcessing(int saleId, String returnedTransactionStatus, String paymentGatewayExtraData) {
        JsonArray authorisationJsonArray = null;
        String returnedTransactionRef = null;
        String paymentGatewayInfo = "";
        JsonObject jsonObj = getJsonObjectFromJsonString(paymentGatewayExtraData);
        JsonElement je;

        for (Map.Entry element : jsonObj.entrySet()) {

            if (element.getKey().equals("checkoutRequestID")) {
                je = jsonObj.get("checkoutRequestID");
                if (!je.isJsonNull()) {
                    returnedTransactionRef = je.getAsString();
                }
            }
            if (element.getKey().equals("paymentStatusCode")) {
                je = jsonObj.get("paymentStatusCode");
                if (!je.isJsonNull()) {
                    returnedTransactionStatus = je.getAsString();
                }
            }
            if (element.getKey().equals("payments")) {
                if (jsonObj.get("payments").isJsonArray()) {
                    authorisationJsonArray = jsonObj.get("payments") != null ? jsonObj.get("payments").getAsJsonArray() : null;
                }
            }
        }

        if (authorisationJsonArray != null) {
            Iterator<JsonElement> iterator = authorisationJsonArray.iterator();
            while (iterator.hasNext()) {
                JsonObject jsonO = (JsonObject) iterator.next();
                for (Map.Entry element : jsonO.entrySet()) {

                    if (element.getKey().equals("paymentMode")) {
                        je = jsonO.get("paymentMode");
                        if (!je.isJsonNull()) {
                            paymentGatewayInfo = je.getAsString();
                        }
                    }
                    if (element.getKey().equals("paymentStatusCode")) {
                        je = jsonO.get("paymentStatusCode");
                        if (!je.isJsonNull()) {
                            returnedTransactionStatus = je.getAsString();
                        }
                    }
                }
            }
        }

        log.debug("Getting locked sale to doPaymentGatewayPostProcessing [{}]", saleId);
        Sale lockedSale = DAO.getLockedSale(em, saleId);
        log.debug("Got locked sale to doPaymentGatewayPostProcessing");

        lockedSale.setPaymentGatewayLastPollDate(new Date());
        lockedSale.setPaymentGatewayNextPollDate(null);
        lockedSale.setPaymentGatewayPollCount(lockedSale.getPaymentGatewayPollCount() + 1);
        lockedSale.setPaymentGatewayResponse(paymentGatewayExtraData);
        lockedSale.setExtraInfo(Utils.setValueInCRDelimitedAVPString(lockedSale.getExtraInfo(), "PaymentGatewayInfo", paymentGatewayInfo));
        lockedSale.setExtTxid(returnedTransactionRef);

        if (!returnedTransactionStatus.equalsIgnoreCase(successfulTransactionCode)) {
            lockedSale.setStatus(POSManager.PAYMENT_STATUS_GW_FAIL_GW);
        }

        /*
            For some reason the transaction status has been updated by Cellulant... they're now nofifying us of such
            So we need to update the status so POS can reprocess the transaction
         */
        if (returnedTransactionStatus.equalsIgnoreCase(successfulTransactionCode) && lockedSale.getStatus().equals(POSManager.PAYMENT_STATUS_GW_FAIL_GW)) {
            log.debug("This transaction had previously failed on GW, its now being reprocessed after status change");
            lockedSale.setStatus(POSManager.PAYMENT_STATUS_PENDING_PAYMENT);
        }

        em.persist(lockedSale);
        em.flush();

    }

    @Override
    public void addPaymentGatewaySpecificsOnTicket(NewTTIssue tt, String errorMessage) {
        JiraField jiraF = new JiraField();

        jiraF.setFieldName("TT_FIXED_FIELD_Description");
        jiraF.setFieldType("TT_FIXED_FIELD");
        StringBuilder description = new StringBuilder("{panel:title=(!) ");
        description.append("Warning: Cellulant is currently not available to query transaction statuses");
        description.append("|borderStyle=dashed|titleBGColor=red}");
        description.append("Cellulant interface to query transaction statuses is currently not available\n")
                .append("Customers using MySmile to recharge would be affected\n").append("{panel}");
        jiraF.setFieldValue(description.toString());
        tt.getMindMapFields().getJiraField().add(jiraF);

        jiraF = new JiraField();
        jiraF.setFieldName("TT_FIXED_FIELD_Summary");
        jiraF.setFieldType("TT_FIXED_FIELD");
        String summary = "Cellulant API interface instability";
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
