/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.helpers.paymentgateway;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.beans.SaleBean;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.sra.helpers.SRAException;
import com.smilecoms.sra.resource.PaymentGatewayResource;
import java.util.Map;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sabza
 */
public class PaymentNotifactionFactory {

    private static final Logger log = LoggerFactory.getLogger(PaymentNotifactionFactory.class);

    public static <T> T createPaymentNotificationResponse(com.smilecoms.commons.sca.PaymentNotificationData cr) {
        T pn;
        switch (cr.getPaymentGatewayCode()) {
            case "Cellulant":
                pn = (T) getCellulantPaymentNotificationResponse(cr);
                break;
            case "Pesapal":
                pn = (T) new PesapalPaymentNotificationResponse();
                break;
            case "Paystack":
                pn = (T) new PaystackPaymentNotificationResponse();
                break;
            default:
                throw new NullPointerException("Gateway does not exist [" + cr.getPaymentGatewayCode() + "]");
        }
        return pn;
    }

    public static com.smilecoms.commons.sca.PaymentNotificationData getPaymentNotificationData(String json, javax.servlet.http.HttpServletRequest request) {
        com.smilecoms.commons.sca.PaymentNotificationData cr;
        String paymentGatewayCode = "";
        String remoteIP = Utils.getRemoteIPAddress(request);

        Set<String> remoteIPTokenMapping = BaseUtils.getPropertyAsSet("env.sra.thirdparty.remoteip.token.mapping");

        for (String bits : remoteIPTokenMapping) {
            String[] bitsArray = bits.split("\\|");
            if (bitsArray[0].equals(remoteIP)) {
                log.debug("Found match for third party remote ip token mapping [{}]", bits);
                paymentGatewayCode = bitsArray[2];
                break;
            }
        }

        switch (paymentGatewayCode) {
            case "Paystack":
                cr = getPaystackPaymentNotificationData(json, request);
                break;
            case "Cellulant":
                cr = getCellulantPaymentNotificationData(json, request);
                break;
            case "Pesapal":
                cr = getPesapalPaymentNotificationData(json, request);
                break;
            default:
                log.warn("Gateway not configured for payment notification with IP [{}], not authorising request", remoteIPTokenMapping);
                throw new SRAException(Response.Status.UNAUTHORIZED);
        }
        return cr;
    }

    private static com.smilecoms.commons.sca.PaymentNotificationData getPaystackPaymentNotificationData(String json, javax.servlet.http.HttpServletRequest request) {

        //Check if its paystack
        String xpaystackSignature = getHeaderByName(request, "x-paystack-signature"); //put in the request's header value for x-paystack-signature
        if (xpaystackSignature == null) {
            throw new SRAException(Response.Status.UNAUTHORIZED);
        }

        if (!isPaystackVerified(json, request)) {
            log.debug("Paystack verification failed for this request");
            throw new SRAException(Response.Status.UNAUTHORIZED);
        }

        com.smilecoms.commons.sca.PaymentNotificationData cr = new com.smilecoms.commons.sca.PaymentNotificationData();
        JsonObject rqJson = new JsonParser().parse(json).getAsJsonObject();

        JsonObject jsonObj = null;
        JsonElement je;

        String successfulEvent = "unknown";

        for (Map.Entry element : rqJson.entrySet()) {
            if (element.getKey().equals("event")) {
                je = rqJson.get("event");
                successfulEvent = je.getAsString();
            }
            if (element.getKey().equals("data")) {
                if (rqJson.get("data").isJsonObject()) {
                    jsonObj = rqJson.get("data") != null ? rqJson.get("data").getAsJsonObject() : null;
                }
            }
        }

        /*We're only interested in successful payment notification events*/
        if (!successfulEvent.toLowerCase().contains("charge.success")) {
            log.debug("Recieved a notification for which were not interested in [{}]", successfulEvent);
            throw new SRAException(Response.Status.UNAUTHORIZED);
        }

        /*Look for data object, if not available then there is nothing to process*/
        if (jsonObj == null) {
            log.debug("The JSON String does not contain data required to process transaction: {}", json);
            throw new SRAException(Response.Status.UNAUTHORIZED);
        }
        String returnedTransactionRef = null;
        double returnedAmount = 0;

        for (Map.Entry element : jsonObj.entrySet()) {

            if (element.getKey().equals("reference")) {
                je = jsonObj.get("reference");
                if (!je.isJsonNull()) {
                    returnedTransactionRef = je.getAsString();
                }
            }
            if (element.getKey().equals("amount")) {
                je = jsonObj.get("amount");
                if (!je.isJsonNull()) {
                    returnedAmount = je.getAsDouble();
                }
            }
        }

        cr.setPaymentGatewayCode("Paystack");
        cr.setPaymentInCents(returnedAmount);
        cr.setSaleId(Integer.parseInt(returnedTransactionRef));
        cr.setPaymentGatewayTransactionId(returnedTransactionRef);
        cr.setPaymentGatewayExtraData(json);
        return cr;

    }

    private static com.smilecoms.commons.sca.PaymentNotificationData getCellulantPaymentNotificationData(String json, javax.servlet.http.HttpServletRequest request) {
        com.smilecoms.commons.sca.PaymentNotificationData cr = new com.smilecoms.commons.sca.PaymentNotificationData();
        JsonObject jsonObj = new JsonParser().parse(json).getAsJsonObject();
        String returnedTransactionRef = null;
        int saleId = 0;
        JsonElement je;
        double amountPaid = 0;

        for (Map.Entry element : jsonObj.entrySet()) {

            if (element.getKey().equals("merchantTransactionID")) {
                je = jsonObj.get("merchantTransactionID");
                if (!je.isJsonNull()) {
                    saleId = je.getAsInt();
                }
            }

            if (element.getKey().equals("checkoutRequestID")) {
                je = jsonObj.get("checkoutRequestID");
                if (!je.isJsonNull()) {
                    returnedTransactionRef = je.getAsString();
                }
            }

            if (element.getKey().equals("requestAmount")) {
                je = jsonObj.get("requestAmount");
                if (!je.isJsonNull()) {
                    amountPaid = je.getAsDouble();
                }
            }
        }

        cr.setPaymentGatewayCode("Cellulant");
        cr.setPaymentInCents(amountPaid);
        cr.setSaleId(saleId);
        cr.setPaymentGatewayTransactionId(returnedTransactionRef);
        cr.setPaymentGatewayExtraData(json);
        return cr;

    }

    private static CellulantPaymentNotificationResponse getCellulantPaymentNotificationResponse(com.smilecoms.commons.sca.PaymentNotificationData cr) {
        PaymentGatewayResource pgRes = PaymentGatewayResource.getPaymentGatewayResourceBySaleId(cr.getSaleId());
        SaleBean processedSale = pgRes.getSale();

        CellulantPaymentNotificationResponse cpnr = new CellulantPaymentNotificationResponse();
        cpnr.setCheckoutRequestID(Long.valueOf(processedSale.getPaymentTransactionData()));
        cpnr.setReceiptNumber(processedSale.getStatus().equals("PD") ? String.valueOf(processedSale.getSaleId()) : "");
        cpnr.setStatusDescription(processedSale.getStatus().equals("PD") ? CellulantPaymentNotificationResponse.SUCCESFULLY_PROCESSED_CODE_SUCCESFUL : CellulantPaymentNotificationResponse.SUCCESFULLY_PROCESSED_CODE_FAILURE);
        cpnr.setStatusCode(processedSale.getStatus().equals("PD") ? CellulantPaymentNotificationResponse.SUCCESFULLY_PROCESSED_CODE_PAID : CellulantPaymentNotificationResponse.SUCCESFULLY_PROCESSED_CODE_FAILED);
        return cpnr;
    }
    
    private static com.smilecoms.commons.sca.PaymentNotificationData getPesapalPaymentNotificationData(String json, javax.servlet.http.HttpServletRequest request) {
        com.smilecoms.commons.sca.PaymentNotificationData cr = new com.smilecoms.commons.sca.PaymentNotificationData();
        JsonObject jsonObj = new JsonParser().parse(json).getAsJsonObject();
        String returnedTransactionRef = null;
        int saleId = 0;
        JsonElement je;
        double amountPaid = 0;

        for (Map.Entry element : jsonObj.entrySet()) {

            if (element.getKey().equals("reference")) {
                je = jsonObj.get("reference");
                if (!je.isJsonNull()) {
                    saleId = je.getAsInt();
                }
            }

            if (element.getKey().equals("pesapal_transaction_tracking_id")) {
                je = jsonObj.get("pesapal_transaction_tracking_id");
                if (!je.isJsonNull()) {
                    returnedTransactionRef = je.getAsString();
                }
            }

            if (element.getKey().equals("amount")) {
                je = jsonObj.get("amount");
                if (!je.isJsonNull()) {
                    amountPaid = je.getAsDouble();
                }
            }
        }

        cr.setPaymentGatewayCode("Pesapal");
        cr.setPaymentInCents(amountPaid);
        cr.setSaleId(saleId);
        cr.setPaymentGatewayTransactionId(returnedTransactionRef);
        cr.setPaymentGatewayExtraData(json);
        return cr;

    }

    private static boolean isPaystackVerified(String json, javax.servlet.http.HttpServletRequest request) {
        /*Verify that this request came from paystack*/
        String clientSecret = BaseUtils.getSubProperty("env.pgw.paystack.config", "ClientSecret"); //paystack secret_key
        String result = "";
        String HmacSHA512 = "HmacSHA512";
        String xpaystackSignature = getHeaderByName(request, "x-paystack-signature"); //put in the request's header value for x-paystack-signature
        log.debug("Paystack signature is: {}", xpaystackSignature);

        try {
            byte[] byteKey = clientSecret.getBytes("UTF-8");
            SecretKeySpec keySpec = new SecretKeySpec(byteKey, HmacSHA512);
            Mac sha512HMAC = Mac.getInstance(HmacSHA512);
            sha512HMAC.init(keySpec);

            byte[] macData = sha512HMAC.doFinal(json.getBytes("UTF-8"));
            result = DatatypeConverter.printHexBinary(macData);

            log.debug("Going to test header value [{}] vs computed value [{}]", xpaystackSignature, result);
            if (result.toLowerCase().equals(xpaystackSignature.toLowerCase())) {
                return true;
            }
        } catch (Exception ex) {
            log.warn("Error occured: ", ex);
        }
        log.debug("Verification failed");
        return false;
    }

    private static String getHeaderByName(HttpServletRequest request, String header) {
        return request.getHeader(header);
    }
}
