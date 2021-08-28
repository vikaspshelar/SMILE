/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.scp.helpers.paymentgateway;

import com.smilecoms.commons.paymentgateway.helpers.PaymentGatewayResult;


import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.Sale;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sabza
 */
public class SelcomManager extends PaymentGatewayManager {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private static final Map<String, String> headers = new HashMap<>();

    private static String currencyCode;
    private static String paymentGatewayPostURL;
    private static String callbackURL;
    private static String accountId;
    private static String APIUsername;
    private static String APIPassword;
    private static String proxyHost;
    private static String proxyPort;
    private static String statusQueryURL;
    private static String pendingPaymentStatuses;

    static {
        init();
    }

    private static void init() {
        currencyCode = BaseUtils.getSubProperty("env.pgw.selcom.config", "CurrencyCode");
        proxyHost = BaseUtils.getSubProperty("env.pgw.selcom.config", "ProxyHost");
        proxyPort = BaseUtils.getSubProperty("env.pgw.selcom.config", "ProxyPort");
        paymentGatewayPostURL = BaseUtils.getSubProperty("env.pgw.selcom.config", "PaymentGatewayPostURL");
        callbackURL = BaseUtils.getSubProperty("env.pgw.selcom.config", "CallbackURL");
        accountId = BaseUtils.getSubProperty("env.pgw.selcom.config", "AccountId");
        statusQueryURL = BaseUtils.getSubProperty("env.pgw.selcom.config", "StatusQueryURL");
        APIUsername = BaseUtils.getSubProperty("env.pgw.selcom.config", "APIUsername");
        APIPassword = BaseUtils.getSubProperty("env.pgw.selcom.config", "APIPassword");
        pendingPaymentStatuses = BaseUtils.getSubProperty("env.pgw.selcom.config", "PendingPaymentStatuses");//Currently not sure which are valid statuses to monitor for Pending, going to build some artificial intelligence to handle pending transactions
        headers.put("Content-Type", "text/xml");
        headers.put("Content-Transfer-Encoding", "text");
    }

    @Override
    public IPGWTransactionData startTransaction(Sale data, String itemDescription) throws Exception {
        IPGWTransactionData transaction = new SelcomTransactionData(data, itemDescription);
        PaymentGatewayResult gwResponse = sendPaymentRequest(data);
        LOG.debug("Selcom gwResponse success [{}]", gwResponse.isSuccess());
        if (!gwResponse.isSuccess()) {
            throw new Exception("Payment failed. please try again");
        }
        return transaction;
    }

    public PaymentGatewayResult sendPaymentRequest(Sale dbSale) {
        PaymentGatewayResult res = new PaymentGatewayResult();
        String transactionDetails = "";
        /*String req = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<AutoCreate>\n"
                + "<Request>\n"
                + "<APIUsername>" + APIUsername + "</APIUsername>\n"
                + "<APIPassword>" + APIPassword + "</APIPassword>\n"
                + "<Method>acdepositfunds</Method>\n"
                + "<NonBlocking>TRUE</NonBlocking>\n"
                + "<Amount>" + dbSale.getSaleTotalCentsIncl() / 100 + "</Amount>\n"
                + "<Account>" + dbSale.getRecipientPhoneNumber() + "</Account>\n" // Mobile Number of customer mapped with Yo 
                + "<ExternalReference>" + dbSale.getSaleId() + "</ExternalReference>\n"
                + "<Narrative>Smile Communications payments for account " + dbSale.getRecipientAccountId() + "</Narrative>\n"
                + "</Request>\n"
                + "</AutoCreate>";*/
        Map<String,Object> reqObj = new HashMap<String, Object>();
        reqObj.put("vendor", "Smile communications");
        reqObj.put("order_id", dbSale.getSaleId());
        reqObj.put("buyer_email", "");
        reqObj.put("buyer_name", dbSale.getRecipientName());
        reqObj.put("buyer_phone", dbSale.getRecipientPhoneNumber());
        reqObj.put("amount", dbSale.getSaleTotalCentsIncl() / 100);
        reqObj.put("currency", "TZS");
        reqObj.put("webhook", dbSale);
        reqObj.put("buyer_remarks", "None");
        reqObj.put("merchant_remarks", "None");
        reqObj.put("no_of_items", 1);
        JSONObject json = new JSONObject();
        json.putAll( reqObj );
        String req = json.toString();
        String respResult = null;
        try {
            //YoPayments yo = new YoPayments();
            log.debug("sending request to Yo Payments. datat [{}]", req);
            String paymentResp = sendPaymentRequest(req);
            if(paymentResp == null || paymentResp.isEmpty()){
                log.debug("response from payment gateway [{}]",paymentResp);
                return res;
            }
            JSONParser parser = new JSONParser();
            JSONObject responseObj = (JSONObject) parser.parse(paymentResp);
            respResult = (String)responseObj.get("result");
            //transactionDetails = StringEscapeUtils.unescapeXml(paymentResp);
            log.debug("HTTP client request returned successfully for sendPaymentRequest. Transaction details returned[{}]", transactionDetails);
        } catch (Exception ex) {
            log.warn("System error in Yo! Payments plugin: ", ex);
            res.setGatewayResponse(ex.toString());
            res.setSuccess(false);
            res.setTryAgainLater(true);
            return res;
        }

        /*String statusCode = Utils.getBetween(transactionDetails, "<StatusCode>", "</StatusCode>");
        String status = Utils.getBetween(transactionDetails, "<Status>", "</Status>");
        if (statusCode == null) {
            log.debug("Yo! Payments did not return a status in their response. Assuming its pending");
            statusCode = "NUL";
        }*/
        boolean isSuccess = "SUCCESS".equals(respResult);
        res.setSuccess(isSuccess);
        return res;
    }
    
    private String sendPaymentRequest(String reqData){
        WebClient wc = getClient(paymentGatewayPostURL);
        Response response = null;
        String result = null;
        try {
            response = wc.post(reqData);
        } catch (Exception ex){
            log.error("selcom post exception",ex);
            return null;
        }
        int httpStatus = response.getStatus();
        if (httpStatus > 202) {
            log.debug("selcom httpStatus = " + httpStatus);
            return null;
        }
        try {
            InputStream is = (InputStream) response.getEntity();
            result = IOUtils.toString(is, "UTF-8");
            log.debug("SelcomResponse = "+result);
            return result;
        } catch (Exception ex) {
            log.error("exception in getToken", ex);
        } finally {
             wc.close();
        }
        return result;
    }
    
    private static WebClient getClient(String url) {
        WebClient wc = WebClient.create(url);
        HTTPConduit conduit = WebClient.getConfig(wc).getHttpConduit();

        HTTPClientPolicy policy = new HTTPClientPolicy();
        policy.setProxyServer(proxyHost);
        policy.setProxyServerPort(Integer.parseInt(proxyPort));
        policy.setConnectionTimeout(10 * 1000);
        policy.setReceiveTimeout(180 * 1000);
        policy.setAllowChunking(false);
        policy.setConnection(ConnectionType.KEEP_ALIVE);

        conduit.setClient(policy);

        TLSClientParameters tlsCP = new TLSClientParameters();
        tlsCP.setDisableCNCheck(true);
        conduit.setTlsClientParameters(tlsCP);
        wc.type(MediaType.TEXT_XML);

        return wc;
    }

}
