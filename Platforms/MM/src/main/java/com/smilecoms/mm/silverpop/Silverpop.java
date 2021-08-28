/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.silverpop;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.ExceptionManager;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.mm.Personalisation;
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
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class Silverpop {

    private static final Logger log = LoggerFactory.getLogger(Silverpop.class);

    private static final String PART1 = "<XTMAILING><CAMPAIGN_ID>";
    private static final String PART2 = "</CAMPAIGN_ID><SHOW_ALL_SEND_DETAIL>false</SHOW_ALL_SEND_DETAIL><SEND_AS_BATCH>false</SEND_AS_BATCH><NO_RETRY_ON_FAILURE>false</NO_RETRY_ON_FAILURE><RECIPIENT><EMAIL>";
    private static final String PART3 = "</EMAIL><BODY_TYPE>HTML</BODY_TYPE>";
    private static final String PART4 = "</RECIPIENT></XTMAILING>";

    public void sendTemplateEmail(String emailAddress, String campaignId, List<Personalisation> fields, byte[] attachment, String attachmentName, String personalisationXML) {

        StringBuilder xml = new StringBuilder(PART1);
        xml.append(campaignId);
        xml.append(PART2);
        xml.append(emailAddress);
        xml.append(PART3);
        for (Personalisation field : fields) {
            xml.append("<PERSONALIZATION><TAG_NAME>").append(field.getTagName()).append("</TAG_NAME><VALUE>").append(StringEscapeUtils.escapeXml(field.getValue())).append("</VALUE></PERSONALIZATION>");
        }
        if (personalisationXML != null) {
            xml.append(personalisationXML);
        }
        xml.append(PART4);

        String xmlString = xml.toString();
        log.debug("XML Going to Silverpop is [{}]", xmlString);
        doXMLPost(BaseUtils.getProperty("env.silverpop.transact.url", "http://transact1.silverpop.com/XTMail"), xmlString);
    }

    /**
     * This function is used to post given data to a specified URL.
     *
     * @param strURL the URL to which the data should be POSTed.
     * @param data the data to be POSTed.
     * @return the response received from the POST operation.
     */
    private void doXMLPost(String strURL, String data) {
        PostMethod post = null;
        HttpClient httpclient = null;
        try {
            // Get file to be posted
            post = new PostMethod(strURL);
            RequestEntity entity = new ByteArrayRequestEntity(data.getBytes(), "text/xml; charset=utf-8");

            post.setRequestEntity(entity);
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

            String proxyHost = BaseUtils.getProperty("env.http.proxy.host", "");
            int proxyPort = BaseUtils.getIntProperty("env.http.proxy.port", 0);
            if (!proxyHost.isEmpty() && proxyPort > 0) {
                log.debug("Post will use a proxy server [{}][{}]", proxyHost, proxyPort);
                httpclient.getHostConfiguration().setProxy(proxyHost, proxyPort);
            } else {
                httpclient.getHostConfiguration().setProxyHost(null);
            }
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
            if (result != 200) {
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "ET", "Non 200 result code returned when calling Silverpop. [URL: " + strURL + "] [Request: " + data + "] [Response: " + response + "]");
            }

            if (response != null) {
                String error = Utils.getBetween(response, "<ERROR_STRING>", "</ERROR_STRING>");
                if (error != null && !error.isEmpty()) {
                    BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "ET", "Error returned when calling Silverpop. [URL: " + strURL + "] [Request: " + data + "] [Response: " + response + "]");
                }
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
    }

}
