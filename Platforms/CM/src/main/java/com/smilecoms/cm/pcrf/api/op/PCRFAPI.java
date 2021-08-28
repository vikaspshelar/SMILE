/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.cm.pcrf.api.op;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.ExceptionManager;
import java.io.IOException;
import java.util.List;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodRetryHandler;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author richard
 */
public class PCRFAPI {

    private static final Logger log = LoggerFactory.getLogger(PCRFAPI.class);

    private static final String PATH_GET_SVC_INSTANCE_ID = "/mobicents/getSvcInstanceId";
    private static final String PATH_GET_IMEI = "/mobicents/getImei";

    /**
     * This function is used to post given data to a specified URL.
     *
     * @param strURL the URL to which the data should be POSTed.
     * @param imsPrivateIdentity
     * @return the response received from the POST operation.
     */
    /**
     * This function is used to post given data to a specified URL.
     *
     * @param strURL the URL to which the data should be POSTed.
     * @return the response received from the POST operation.
     */
    public static int doPCRFGetServiceInstanceIdByIPAddress(String strURL, String bindingIdentifier) {
        PostMethod post = null;
        HttpClient httpclient = null;

        int svcInstanceId = -1;

        try {
            // Get file to be posted
            post = new PostMethod(strURL);
            RequestEntity entity = new ByteArrayRequestEntity(bindingIdentifier.getBytes(), "text/xml; charset=utf-8");

            post.setRequestEntity(entity);
            post.setPath(PATH_GET_SVC_INSTANCE_ID);
            // Get HTTP client
            httpclient = new HttpClient();
            // use  SimpleHttpConnectionManager cause one can close its ports and prevent close-wait states
            httpclient.setHttpConnectionManager(new SimpleHttpConnectionManager());
            HttpMethodRetryHandler myretryhandler = new HttpMethodRetryHandler() {
                @Override
                public boolean retryMethod(final HttpMethod method, final IOException exception, int executionCount) {
                    // do not retry
                    return false;
                }
            };

            HttpParams httpParams = new HttpClientParams();
            httpParams.setParameter(HttpClientParams.RETRY_HANDLER, myretryhandler);
            httpParams.setIntParameter(HttpClientParams.SO_TIMEOUT, 30000);
            httpParams.setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, 5000);
            httpParams.setParameter(HttpClientParams.PREEMPTIVE_AUTHENTICATION, Boolean.FALSE);
            httpclient.setParams((HttpClientParams) httpParams);

            httpclient.getHostConfiguration().setProxyHost(null);

            if (log.isDebugEnabled()) {
                log.debug("About to post to method...");
            }
            int result = httpclient.executeMethod(post);
            // Display status code

            String response = post.getResponseBodyAsString();
            if (log.isDebugEnabled()) {
                log.debug("Finished post to method");
                log.debug("HTTP Post Response status code: " + result);
                log.debug("Response body: ");
                log.debug(response);
            }

            if (!response.isEmpty()) {
                try {
                    svcInstanceId = Integer.parseInt(response);
                } catch (NumberFormatException e) {
                    log.debug("svcInstanceId number not returned in body, means there is no user attached with this IP");
                    svcInstanceId = -1;
                }
            }

            if (result != 200) {
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "PC", "Non 200 result code returned when calling PCRFAPI doPCRFGetServiceInstanceIdByIPAddress. [URL: " + strURL + "] [Request: " + bindingIdentifier + "] [Response: " + response + "]");
            }
        } catch (Exception e) {
            log.warn("Error posting to address " + strURL + " : " + e.toString());
            new ExceptionManager(log).reportError(e);
        } finally {
            // Release current connection to the connection pool once you are done
            try {
                post.releaseConnection();
            } catch (Exception ex) {
                log.warn("Error releasing http post connection:" + ex.toString());
            }
            try {
                ((SimpleHttpConnectionManager) httpclient.getHttpConnectionManager()).shutdown();
            } catch (Exception ex) {
                log.warn("Error releasing http client connection:" + ex.toString());
            }
        }

        return svcInstanceId;
    }

    public static String doPCRFGetImeiByIPAddress(String strURL, String bindingIdentifier) {
        PostMethod post = null;
        HttpClient httpclient = null;

        String imei = "";

        try {
            // Get file to be posted
            post = new PostMethod(strURL);
            RequestEntity entity = new ByteArrayRequestEntity(bindingIdentifier.getBytes(), "text/xml; charset=utf-8");

            post.setRequestEntity(entity);
            post.setPath(PATH_GET_IMEI);
            // Get HTTP client
            httpclient = new HttpClient();
            // use  SimpleHttpConnectionManager cause one can close its ports and prevent close-wait states
            httpclient.setHttpConnectionManager(new SimpleHttpConnectionManager());
            HttpMethodRetryHandler myretryhandler = new HttpMethodRetryHandler() {
                @Override
                public boolean retryMethod(final HttpMethod method, final IOException exception, int executionCount) {
                    // do not retry
                    return false;
                }
            };

            HttpParams httpParams = new HttpClientParams();
            httpParams.setParameter(HttpClientParams.RETRY_HANDLER, myretryhandler);
            httpParams.setIntParameter(HttpClientParams.SO_TIMEOUT, 30000);
            httpParams.setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, 5000);
            httpParams.setParameter(HttpClientParams.PREEMPTIVE_AUTHENTICATION, Boolean.FALSE);
            httpclient.setParams((HttpClientParams) httpParams);

            httpclient.getHostConfiguration().setProxyHost(null);

            if (log.isDebugEnabled()) {
                log.debug("About to post to method...");
            }
            int result = httpclient.executeMethod(post);
            // Display status code

            String response = post.getResponseBodyAsString();
            if (log.isDebugEnabled()) {
                log.debug("Finished post to method");
                log.debug("HTTP Post Response status code: " + result);
                log.debug("Response body: ");
                log.debug(response);
            }

            if (!response.isEmpty()) {
                imei = response;
            }

            if (result != 200) {
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "PC", "Non 200 result code returned when calling PCRFAPI doPCRFGetServiceInstanceIdByIPAddress. [URL: " + strURL + "] [Request: " + bindingIdentifier + "] [Response: " + response + "]");
            }
        } catch (Exception e) {
            log.warn("Error posting to address " + strURL + " : " + e.toString());
            new ExceptionManager(log).reportError(e);
        } finally {
            // Release current connection to the connection pool once you are done
            try {
                post.releaseConnection();
            } catch (Exception ex) {
                log.warn("Error releasing http post connection:" + ex.toString());
            }
            try {
                ((SimpleHttpConnectionManager) httpclient.getHttpConnectionManager()).shutdown();
            } catch (Exception ex) {
                log.warn("Error releasing http client connection:" + ex.toString());
            }
        }

        return imei;
    }

}
