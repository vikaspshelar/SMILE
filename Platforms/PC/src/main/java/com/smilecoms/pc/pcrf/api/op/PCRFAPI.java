/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pc.pcrf.api.op;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.ExceptionManager;
import com.smilecoms.pc.pcrf.api.model.XMLAFSession;
import com.smilecoms.pc.pcrf.api.model.XMLIPCANSession;
import com.smilecoms.pc.pcrf.api.model.XMLPCCRule;
import com.smilecoms.pc.pcrf.api.model.XMLAFSessions;
import com.smilecoms.pc.pcrf.api.model.XMLIPCANSessions;
import com.smilecoms.pc.pcrf.api.model.XMLPCCRules;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
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

    private static final String PATH_GET_IPCAN_SESSIONS = "/mobicents/getIPCANSessions";
    private static final String PATH_GET_AFSESSIONS = "/mobicents/getAFSessions";
    private static final String PATH_GET_PCCRULES = "/mobicents/getPCCRules";
    private static JAXBContext jaxbContextIPCANSessions = null;
    private static JAXBContext jaxbContextAFSessions = null;
    private static JAXBContext jaxbContextPCCRules = null;

    /**
     * This function is used to post given data to a specified URL.
     *
     * @param strURL the URL to which the data should be POSTed.
     * @param imsPrivateIdentity
     * @return the response received from the POST operation.
     */
    public static List<XMLIPCANSession> doPCRFGetIPCANSessionsByPrivateIdentity(String strURL, String imsPrivateIdentity) {
        PostMethod post = null;
        HttpClient httpclient = null;
        XMLIPCANSessions XMLIPCANSessions = null;

        try {
            // Get file to be posted
            post = new PostMethod(strURL);
            RequestEntity entity = new ByteArrayRequestEntity(imsPrivateIdentity.getBytes(), "text/xml; charset=utf-8");

            post.setRequestEntity(entity);
            post.setPath(PATH_GET_IPCAN_SESSIONS);
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

            try {
                if (jaxbContextIPCANSessions == null) {
                    jaxbContextIPCANSessions = JAXBContext.newInstance(XMLIPCANSessions.class);
                }
                Unmarshaller jaxbUnmarshallerXMLIPCANSessions = jaxbContextIPCANSessions.createUnmarshaller();
                XMLIPCANSessions = (XMLIPCANSessions) jaxbUnmarshallerXMLIPCANSessions.unmarshal(new ByteArrayInputStream(response.getBytes()));
            } catch (JAXBException e) {
                log.warn("Error Unmarshalling", e);
            }

            if (result != 200) {
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "PC", "Non 200 result code returned when calling PCRFAPI doPCRFGetIPCANSessionsByPrivateIdentity. [URL: " + strURL + "] [Request: " + imsPrivateIdentity + "] [Response: " + response + "]");
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

        if (XMLIPCANSessions == null) {
            return null;
        }
        return XMLIPCANSessions.getXMLIPCANSessions();
    }

    public static List<XMLAFSession> doPCRFGetAFSessionsByBindingIdentifier(String strURL, String bindingIdentifier) {
        PostMethod post = null;
        HttpClient httpclient = null;

        XMLAFSessions XMLAFSessions = null;

        try {
            // Get file to be posted
            post = new PostMethod(strURL);
            RequestEntity entity = new ByteArrayRequestEntity(bindingIdentifier.getBytes(), "text/xml; charset=utf-8");

            post.setRequestEntity(entity);
            post.setPath(PATH_GET_AFSESSIONS);
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

            try {
                if (jaxbContextAFSessions == null) {
                    jaxbContextAFSessions = JAXBContext.newInstance(XMLAFSessions.class);
                }
                Unmarshaller jaxbUnmarshallerXMLAFSessions = jaxbContextAFSessions.createUnmarshaller();
                XMLAFSessions = (XMLAFSessions) jaxbUnmarshallerXMLAFSessions.unmarshal(new ByteArrayInputStream(response.getBytes()));
            } catch (JAXBException e) {
                log.warn("Error Unmarshalling", e);
            }

            if (result != 200) {
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "PC", "Non 200 result code returned when calling PCRFAPI doPCRFGetAFSessionsByBindingIdentifier. [URL: " + strURL + "] [Request: " + bindingIdentifier + "] [Response: " + response + "]");
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

        if (XMLAFSessions == null) {
            return null;
        }
        return XMLAFSessions.getXMLAFSessions();
    }

    public static List<XMLPCCRule> doPCRFGetPCCRulesByBindingIdentifier(String strURL, String bindingIdentifier) {
        PostMethod post = null;
        HttpClient httpclient = null;
        XMLPCCRules XMLPCCRules = null;

        try {
            // Get file to be posted
            post = new PostMethod(strURL);
            RequestEntity entity = new ByteArrayRequestEntity(bindingIdentifier.getBytes(), "text/xml; charset=utf-8");

            post.setRequestEntity(entity);
            post.setPath(PATH_GET_PCCRULES);
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

            try {
                if (jaxbContextPCCRules == null) {
                    jaxbContextPCCRules = JAXBContext.newInstance(XMLPCCRules.class);
                }
                Unmarshaller jaxbUnmarshallerXMLPCCRules = jaxbContextPCCRules.createUnmarshaller();
                XMLPCCRules = (XMLPCCRules) jaxbUnmarshallerXMLPCCRules.unmarshal(new ByteArrayInputStream(response.getBytes()));
            } catch (JAXBException e) {
                log.warn("Error Unmarshalling", e);
            }

            if (result != 200) {
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "PC", "Non 200 result code returned when calling PCRFAPI doPCRFGetPCCRulesByBindingIdentifier. [URL: " + strURL + "] [Request: " + bindingIdentifier + "] [Response: " + response + "]");
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

        if (XMLPCCRules == null) {
            return null;
        }
        return XMLPCCRules.getXMLPCCRules();
    }

}
