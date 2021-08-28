/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.base.sd;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.helpers.BaseHelper;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodRetryHandler;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class SOAPService extends BaseService {
    
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final transient Logger log = LoggerFactory.getLogger(SOAPService.class);
    private static final long serialVersionUID = 1;
    
    public SOAPService(String serviceName, String protocol, int port, String addressPart) throws MalformedURLException {
        super(serviceName, protocol, port, addressPart);
    }
    
    @Override
    boolean doTest() {
        boolean isup = true;
        if (this.testData == null || this.testResponseCriteria == null || this.testData.isEmpty() || this.testResponseCriteria.isEmpty()) {
            return isup;
        }
        
        String result = doPost();
        
        isup = result != null && !result.startsWith("Non 200") && BaseHelper.matches(result, this.testResponseCriteria);
        
        if (isup) {
            BaseUtils.sendStatistic(getHostName(), getServiceName(), "isup", 1, getClass().getSimpleName());
        } else if (System.currentTimeMillis() - dateOfBirth > 60000) {
            // Only moan about adults
            log.warn("A SOAP service is failing its availability test. Result: [{}][{}]", getURL(), result);
            if (result != null && result.contains("UnknownHostException")) {
                BaseUtils.sendTrapToOpsManagement(
                        BaseUtils.MAJOR,
                        "DNS",
                        "A SOAP service is failing its availability test due to an error resolving the hostname. Either hostname is wrong or DNS is not available. Result: [" + result + "]",
                        getHostName());
            } else {
                if (status.equals(STATUS.DOWN)) {
                    // Send trap if its down more than once in a row - i.e. its currently down and is down again
                    BaseUtils.sendTrapToOpsManagement(
                            BaseUtils.MAJOR,
                            getServiceName(),
                            "A SOAP service is failing its availability test. It must be down. URL: " + url + " Result: [" + result + "]",
                            getHostName());
                } else {
                    log.warn("An UP SOAP service has failed its availability test. URL: " + url + " Result: [" + result + "]");
                }
            }
            // Send statistics that its down
            BaseUtils.sendStatistic(getHostName(), getServiceName(), "isup", 0, getClass().getSimpleName());
        }
        return isup;
    }
    
    private String doPost() {
        String ret = null;
        PostMethod post = null;
        try {
            // Get HTTP client
            HttpClient httpclient = new HttpClient();
            HttpParams httpParams = new HttpClientParams();
            httpParams.setIntParameter(HttpClientParams.SO_TIMEOUT, BaseUtils.getIntPropertyFailFast("global.lt.test.socket.timeout.millis", testTimeoutMillis));
            httpParams.setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, BaseUtils.getIntPropertyFailFast("global.lt.test.connect.timeout.millis", testTimeoutMillis));
            httpParams.setParameter(HttpClientParams.PREEMPTIVE_AUTHENTICATION, Boolean.FALSE);
            HttpMethodRetryHandler myretryhandler = new HttpMethodRetryHandler() {
                @Override
                public boolean retryMethod(final HttpMethod method, final IOException exception, int executionCount) {
                    // do not retry
                    return false;
                }
            };
            httpParams.setParameter(HttpClientParams.RETRY_HANDLER, myretryhandler);
            httpclient.setParams((HttpClientParams) httpParams);
            // Get file to be posted
            post = new PostMethod(this.url.toExternalForm());
            String time = sdf.format(new Date());
            String data = this.testData.replace("_TIME_", time);
            post.setRequestEntity(new ByteArrayRequestEntity(data.getBytes(), "text/xml; charset=utf-8"));
            int result = httpclient.executeMethod(post);
            InputStream resStream = post.getResponseBodyAsStream();
            if (result == 200 && resStream != null) {
                ret = BaseHelper.parseStreamToString(resStream, "UTF-8"); // SOAP should be UTF-8
            } else if (resStream != null) {
                ret = "Non 200 Response with a body: " + result + " [" + BaseHelper.parseStreamToString(resStream, "UTF-8") + "]";
            } else {
                ret = "Non 200 Response without a body: " + result;
            }
        } catch (Exception e) {
            ret = "Error: " + e.toString();
        } finally {
            // Release current connection to the connection pool once you are done
            try {
                if (post != null) {
                    post.releaseConnection();
                }
            } catch (Exception ex) {
            }
        }
        return ret;
    }
    
}
