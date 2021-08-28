/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.plugins.offnetsms;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.mm.engine.*;
import com.smilecoms.mm.plugins.offnetsms.smsc.InterconnectSMSC;
import com.smilecoms.mm.utils.SMSCodec;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.params.HttpParams;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class SMSPortalPlugin implements DeliveryPipelinePlugin {

    private static final Logger log = LoggerFactory.getLogger(SMSPortalPlugin.class);
    private static EntityManagerFactory emf;

    @Override
    public DeliveryPluginResult processMessage(BaseMessage msg, DeliveryEngine callbackEngine) {
        // Lets return synchronously - no need for a callback ID
        FinalDeliveryPluginResult res = new FinalDeliveryPluginResult();
        res.setMustRetry(false);
        try {
            OffnetSMSMessage msgOff = (OffnetSMSMessage) msg;
            send(Utils.getPhoneNumberFromSIPURI(msgOff.getTo()), SMSCodec.decode(msgOff.getMessage(), msgOff.getCodingScheme()));
        } catch (Exception e) {
            log.warn("Error sending offnet message to SMS portal: [{}]", e.toString());
        }
        return res;
    }

    @Override
    public void shutDown() {
        log.warn("In shutdown for SMSPortalPlugin");
    }

    @Override
    public void initialise(EntityManagerFactory emf) {
        log.debug("Initialising SMSPortalPlugin with EMF [{}]", emf.toString());
        SMSPortalPlugin.emf = emf;
        log.debug("Finished initialising SMSPortalPlugin");
    }

    @Override
    public void propertiesChanged() {

    }

    /**
     * Called by SmileSMSC when a message comes in from another SMSC
     *
     * @param fromAddress
     * @param toAddress
     * @param message
     * @param dataCodingScheme
     * @param configOfReceivingSMSC
     * @throws Exception
     */
    public static void onNewMessageFromOffnet(String fromAddress, String toAddress, byte[] message, byte dataCodingScheme, InterconnectSMSC configOfReceivingSMSC) throws Exception {
        log.debug("Finished OffnetSMSDeliveryPlugin.onNewMessageFromOffnet but did nothing");
    }

    private static HttpClient getHTTPClient() {
        HttpClient httpClient = new HttpClient();
        HttpParams httpParams = new HttpClientParams();
        httpParams.setIntParameter(HttpClientParams.SO_TIMEOUT, 60000);
        httpParams.setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, 10000);
        httpParams.setParameter(HttpClientParams.PREEMPTIVE_AUTHENTICATION, Boolean.FALSE);
        httpClient.setParams((HttpClientParams) httpParams);
        String proxyHost = BaseUtils.getProperty("env.http.proxy.host", "");
        int proxyPort = BaseUtils.getIntProperty("env.http.proxy.port", 0);
        if (!proxyHost.isEmpty() && proxyPort > 0) {
            httpClient.getHostConfiguration().setProxy(proxyHost, proxyPort);
        } else {
            httpClient.getHostConfiguration().setProxyHost(null);
        }
        return httpClient;
    }

    private void send(String to, String msg) {
        HttpMethod method = null;
        try {
            HttpClient client = getHTTPClient();
            // Get all remote statistics
            method = new GetMethod("http://www.mymobileapi.com/api5/http5.aspx?Type=sendparam&username=smilecoms&password=smilesms&numto=" + to + "&data1=" + msg);
            log.debug("Doing call to sms api [{}]", method.getURI());
            int status = client.executeMethod(method);
            log.debug("Done call to sms api");
            if (status == 200) {
                String ret = Utils.parseStreamToString(method.getResponseBodyAsStream(), "ISO-8859-1");
                log.debug("Result was [{}]", ret);
            } else {
                log.warn("Error sending SMS : ret code is " + status);
            }
        } catch (Exception e) {
            log.warn("Error sending sms", e);
        } finally {
            // release any connection resources used by the method
            try {
                if (method != null) {
                    method.releaseConnection();
                }
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void sendDeliveryReport(DeliveryEngine.DeliveryReportStatus reportStatus, HashMap<String, Serializable> deliveryReportData) {
        log.debug("In sendDeliveryReport");
    }
}
