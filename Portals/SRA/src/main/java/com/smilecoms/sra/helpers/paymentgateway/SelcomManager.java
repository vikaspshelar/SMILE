/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.helpers.paymentgateway;

import com.smilecoms.commons.paymentgateway.helpers.PaymentGatewayResult;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.Sale;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHost;

import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.apache.http.util.EntityUtils;

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
    private static String APIKey;
    private static String APISecret;
    private static String proxyHost;
    private static String proxyPort;
    private static String statusQueryURL;
    //private static String pendingPaymentStatuses;

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
        APIKey = BaseUtils.getSubProperty("env.pgw.selcom.config", "APIKey");
        APISecret = BaseUtils.getSubProperty("env.pgw.selcom.config", "APISecret");
        //pendingPaymentStatuses = BaseUtils.getSubProperty("env.pgw.selcom.config", "PendingPaymentStatuses");//Currently not sure which are valid statuses to monitor for Pending, going to build some artificial intelligence to handle pending transactions
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
        
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSX");
        Date now = new Date();
	String timestamp = sdfDate.format(now);
        
        JSONObject json = new JSONObject();   
        json.put("transid", dbSale.getSaleId());
        json.put("utilityref",dbSale.getSaleId());
        json.put("msisdn", dbSale.getRecipientPhoneNumber().trim());
        json.put("vendor", "SMILE");
        json.put("amount", (int) Math.round(dbSale.getSaleTotalCentsIncl() / 100));
        String respResult = null;
        try {
            log.debug("sending post request url:[{}], json:[{}], APISecret:[{}], APIKey:[{}]",paymentGatewayPostURL,json,APISecret, APIKey);
           JSONObject postReponse = post(paymentGatewayPostURL, json, computeHeader( json,  APISecret,  timestamp,  APIKey)); 
           if(null != postReponse){
	       log.debug("POST RESPONSE: "+postReponse.toJSONString());
               respResult = (String)postReponse.get("result");
               log.debug("respResult = "+respResult);
	    } else {
	        log.debug("selcom response is null");
	    }
        }catch (Exception ex){
            log.error("exception in sending request to selcom :",ex);
        }
        boolean isSuccess = "SUCCESS".equals(respResult);
        res.setSuccess(isSuccess);
        return res;
    }
    
    public JSONObject post(String url, JSONObject json, Map header) {  
        log.debug("selocm post request url [{}], json [{}] and header [{}]",url, json, header);
        log.debug("proxyHost [{}] and proxyPort [{}]",proxyHost, proxyPort);
        HttpHost proxy = new HttpHost(proxyHost, Integer.valueOf(proxyPort));
        HttpRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
        
        HttpClient httpClient = HttpClientBuilder.create().setRoutePlanner(routePlanner).build();
        
        final RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(30000).setSocketTimeout(30000).build();
        JSONObject result = new JSONObject();
        try {
            HttpPost request = new HttpPost(url);
            request.setConfig(requestConfig);
            StringEntity params = new StringEntity(json.toString());
            
            for (Object key : header.keySet()) {
                request.addHeader(key.toString(), header.get(key).toString());
            }
            
            request.setEntity(params);
            
            HttpResponse hresp = httpClient.execute(request);
            
            HttpEntity httpEntity = hresp.getEntity();
            String apiOutput = EntityUtils.toString(httpEntity);
            JSONParser parser = new JSONParser();
            
            result = (JSONObject) parser.parse(apiOutput);
            log.debug("result of post request is :"+result.toJSONString());
        } catch (Exception ex) {
            log.error("error in post request of selcom request", ex);
        } finally {
            
        }
        return result;
    }
    
    public Map computeHeader(Map params, String secret, String timestamp, String api_key) throws NoSuchAlgorithmException, InvalidKeyException{
	    Map header = new HashMap();
	    String message = "", signed_fields = "";

	    List<String> keys = new ArrayList<>();
	    List<String> serializedJson = new ArrayList<>();
	    serializedJson.add("timestamp="+timestamp);

	    for (Object key : params.keySet()) {
	        String keyStr = (String)key;
	        String keyvalue = params.get(keyStr).toString();
	        serializedJson.add(keyStr+"="+keyvalue);
	        keys.add(keyStr);

	    }

	    message =  String.join("&", serializedJson);
	    signed_fields =  String.join(",", keys);


	    Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
	    SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
	    sha256_HMAC.init(secret_key);

            log.debug("digest text [{}]",message);
	    String digest = new String(Base64.encodeBase64(sha256_HMAC.doFinal(message.getBytes())));
	    String encodedApiKey = new String(Base64.encodeBase64(api_key.getBytes()));

	    header.put("Content-type", "application/json");
	    header.put("Authorization", "SELCOM "+encodedApiKey);
	    header.put("Digest-Method", "HS256");
	    header.put("Digest", digest);
	    header.put("Timestamp", timestamp);
	    header.put("Signed-Fields", signed_fields);
            log.debug("header of selcom payment request is :"+header.toString());
	    return header;

	}

}
