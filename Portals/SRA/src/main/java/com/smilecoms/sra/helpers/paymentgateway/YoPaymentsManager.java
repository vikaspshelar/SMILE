/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.helpers.paymentgateway;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.paymentgateway.YoPayments;
import com.smilecoms.commons.paymentgateway.helpers.PaymentGatewayHelper;
import com.smilecoms.commons.sca.Sale;
import com.smilecoms.commons.util.Utils;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringEscapeUtils;
import com.smilecoms.commons.paymentgateway.helpers.PaymentGatewayResult;
import com.smilecoms.sra.helpers.SRAException;
import java.io.InputStream;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.transports.http.configuration.ProxyServerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author rajeshkumar
 */
public class YoPaymentsManager extends PaymentGatewayManager {

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
        currencyCode = BaseUtils.getSubProperty("env.pgw.yopayments.config", "CurrencyCode");
        proxyHost = BaseUtils.getSubProperty("env.pgw.yopayments.config", "ProxyHost");
        proxyPort = BaseUtils.getSubProperty("env.pgw.yopayments.config", "ProxyPort");
        paymentGatewayPostURL = BaseUtils.getSubProperty("env.pgw.yopayments.config", "PaymentGatewayPostURL");
        callbackURL = BaseUtils.getSubProperty("env.pgw.yopayments.config", "CallbackURL");
        accountId = BaseUtils.getSubProperty("env.pgw.yopayments.config", "AccountId");
        statusQueryURL = BaseUtils.getSubProperty("env.pgw.yopayments.config", "StatusQueryURL");
        APIUsername = BaseUtils.getSubProperty("env.pgw.yopayments.config", "APIUsername");
        APIPassword = BaseUtils.getSubProperty("env.pgw.yopayments.config", "APIPassword");
        pendingPaymentStatuses = BaseUtils.getSubProperty("env.pgw.yopayments.config", "PendingPaymentStatuses");//Currently not sure which are valid statuses to monitor for Pending, going to build some artificial intelligence to handle pending transactions
        headers.put("Content-Type", "text/xml");
        headers.put("Content-Transfer-Encoding", "text");
    }

    @Override
    public IPGWTransactionData startTransaction(Sale data, String itemDescription) throws Exception {
        IPGWTransactionData transaction = new YoPaymentsTransactionData(data, itemDescription);
        PaymentGatewayResult gwResponse = sendPaymentRequest(data);
        LOG.debug("YoPayments gwResponse success [{}]", gwResponse.isSuccess());
        if( ! gwResponse.isSuccess()){
            throw new SRAException("Payment failed. please try again","BUSINESS","SRA-00098",Response.Status.INTERNAL_SERVER_ERROR);
        }
        transaction.setPaymentGatewayPostURL(null);//No need to redirect to payment gateway.
        return transaction;
    }

    public PaymentGatewayResult sendPaymentRequest(Sale dbSale) {
        PaymentGatewayResult res = new PaymentGatewayResult();
        String transactionDetails = "";
        String req = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<AutoCreate>\n"
                + "<Request>\n"
                + "<APIUsername>" + APIUsername + "</APIUsername>\n"
                + "<APIPassword>" + APIPassword + "</APIPassword>\n"
                + "<Method>acdepositfunds</Method>\n"
                + "<NonBlocking>TRUE</NonBlocking>\n"
                + "<Amount>" + dbSale.getSaleTotalCentsIncl() / 100 + "</Amount>\n"
                + "<Account>" + dbSale.getRecipientPhoneNumber().trim() + "</Account>\n" // Mobile Number of customer mapped with Yo 
                + "<ExternalReference>" + dbSale.getSaleId() + "</ExternalReference>\n"
                + "<Narrative>Smile Communications payments for account " + dbSale.getRecipientAccountId()+ "</Narrative>\n"
                + "</Request>\n"
                + "</AutoCreate>";
        try {
            //YoPayments yo = new YoPayments();
            log.debug("sending request to Yo Payments. datat [{}]", req);
            String paymentResp = sendPaymentRequest(req);
            if(paymentResp == null || paymentResp.isEmpty()){
                log.debug("response from payment gateway [{}]",paymentResp);
                return res;
            }
            transactionDetails = StringEscapeUtils.unescapeXml(paymentResp);
            log.debug("HTTP client request returned successfully for sendPaymentRequest. Transaction details returned[{}]", transactionDetails);
        } catch (Exception ex) {
            log.warn("System error in Yo! Payments plugin: ", ex);
            res.setGatewayResponse(ex.toString());
            res.setSuccess(false);
            res.setTryAgainLater(true);
            return res;
        }

        String statusCode = Utils.getBetween(transactionDetails, "<StatusCode>", "</StatusCode>");
        String status = Utils.getBetween(transactionDetails, "<Status>", "</Status>");
        if (statusCode == null) {
            log.debug("Yo! Payments did not return a status in their response. Assuming its pending");
            statusCode = "NUL";
        }
        boolean isSuccess = ((statusCode.equals("0") || (statusCode.equals("1"))) && "OK".equalsIgnoreCase(status)) ? true : false;
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
            log.error("getYopaymentResponse exception",ex);
            return null;
        }
        int httpStatus = response.getStatus();
        if (httpStatus > 202) {
            log.debug("getYopaymentResponse httpStatus = " + httpStatus);
            return null;
        }
        try {
            InputStream is = (InputStream) response.getEntity();
            result = IOUtils.toString(is, "UTF-8");
            log.debug("getYopaymentResponse = "+result);
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
