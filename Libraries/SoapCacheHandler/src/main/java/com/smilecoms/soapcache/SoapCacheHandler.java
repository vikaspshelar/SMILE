package com.smilecoms.soapcache;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;
import com.smilecoms.commons.base.sd.SOAPService;
import com.smilecoms.commons.base.sd.ServiceDiscoveryAgent;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.commons.util.XSSHelper;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import org.slf4j.*;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

public class SoapCacheHandler implements SOAPHandler {

    private static final String CLASS = SoapCacheHandler.class.getName();
    private static final Logger log = LoggerFactory.getLogger(CLASS);
    private static final Logger scalog = LoggerFactory.getLogger("com.smilecoms.sca.soaplog");
    private static final String START_TIME_KEY = "scamsstart";
    private static long ipListLastRefreshed = System.currentTimeMillis(); // dont bother checking for first 60s
    private static Set<String> allowedIps = null;

    private static final String ISUP_POST_DATA = "<soapenv:Envelope xsi:schemaLocation=\"http://schemas.xmlsoap.org/soap/envelope/\""
            + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
            + " xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\""
            + " xmlns:sca=\"http://xml.smilecoms.com/schema/SCA\">"
            + "<soapenv:Body><sca:Test><sca:SCAContext><sca:TxId></sca:TxId><sca:OriginatingIdentity></sca:OriginatingIdentity><sca:OriginatingIP>0.0.0.0</sca:OriginatingIP>"
            + "</sca:SCAContext><sca:String>test</sca:String></sca:Test></soapenv:Body></soapenv:Envelope>";
    private static final String ISUP_IS_UP = "PlatformEndPointList";
    private static boolean publishingFast = true;
    private static ScheduledFuture runner1 = null;

    static {
        runner1 = Async.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                new SoapCacheHandler().trigger();
            }
        }, 0, 3000);
    }

    private void trigger() {
        try {
            registerThisSCAAsAvailable();
            if (publishingFast && !BaseUtils.IN_GRACE_PERIOD) {
                publishingFast = false;
                Async.cancel(runner1);
                runner1 = Async.scheduleAtFixedRate(new SmileBaseRunnable("SCA.SoapCacheTrigger") {
                    @Override
                    public void run() {
                        new SoapCacheHandler().trigger();
                    }
                }, 1000, 60000);
            }
        } catch (Exception e) {
            log.warn("Error in trigger:", e);
        }
    }

    private void registerThisSCAAsAvailable() throws MalformedURLException {
        int port = 18000;
        if (System.getProperty("sca.bind.port") != null) {
            port = Integer.parseInt(System.getProperty("sca.bind.port"));
        }
        log.info("I am SCA bound on port [{}]", port);
        SOAPService service = new SOAPService("SCA", "http", port, "/SCA/SCASoap");
        service.setClientHostnameRegexMatch(BaseUtils.getHostNameFromKernel());
        service.setWeight(5);
        service.setTestData(ISUP_POST_DATA);
        service.setGapBetweenTestsMillis(BaseUtils.getIntPropertyFailFast("env.sd.sca.test.millis",3000));
        service.setTestResponseCriteria(ISUP_IS_UP);
        service.setTestTimeoutMillis(1000);
        ServiceDiscoveryAgent.getInstance().publishService(service);
    }

    @Override
    public boolean handleMessage(MessageContext context) {

        // return value: true=can continue sending request down chain false=dont send down chain as response was got from cache
        boolean ret = true;
        long start = System.currentTimeMillis();

        try {

            SOAPMessageContext soapContext = (SOAPMessageContext) context;
            boolean inbound = !(Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
            String callersIP = null;
            if (inbound) {
                String msgText = soapContext.getMessage().getSOAPBody().getTextContent();
                if (XSSHelper.containsXSS(msgText)) {
                    log.warn("Suspicious XML [{}]", soapContext.getMessage().getSOAPBody().getTextContent());
                    throw new Exception("SOAP request contains suspicious XSS");
                }

                javax.servlet.http.HttpServletRequest servletRequest = (javax.servlet.http.HttpServletRequest) context.get(MessageContext.SERVLET_REQUEST);
                callersIP = Utils.getRemoteIPAddress(servletRequest);
                log.debug("Callers IP address is [{}]", callersIP);

                if (ipListLastRefreshed < (System.currentTimeMillis() - 300000)) {
                    ipListLastRefreshed = System.currentTimeMillis();
                    try {
                        allowedIps = BaseUtils.getPropertyAsSet("env.soap.allowed.ips");
                    } catch (Exception e) {
                        log.debug("Error getting allowed IPs", e);
                    }
                }

                if (allowedIps != null && !allowedIps.isEmpty() && !allowedIps.contains(callersIP)) {
                    log.warn("IP address [{}] is denied from using SOAP services. Turn on debug logging to see the request data", callersIP);
                    XMLUtils.logSOAPMessage(soapContext, scalog, inbound, false, callersIP);
                    throw new RuntimeException("Service Denied " + callersIP);
                }
            }

            if (scalog.isDebugEnabled()) {
                try {
                    XMLUtils.logSOAPMessage(soapContext, scalog, inbound, false, callersIP);
                } catch (Exception ex) {
                    scalog.warn("Error logging soap message: " + ex.toString());
                }
            }

            try {
                if (inbound) {
                    context.put(START_TIME_KEY, start);
                    if (log.isDebugEnabled()) {
                        log.debug("Got an inbound SOAP request in SoapCacheHandler handlemessage. Going to try and get a result using the Cache class");
                    }
                    ret = !Cache.getMessageFromCache(soapContext);
                    if (log.isDebugEnabled()) {
                        log.debug("Finished looking in cache for a response. Did cache find anything: " + !ret + ". Time taken: " + (System.currentTimeMillis() - start) + "ms");
                    }
                    if (ret == false && scalog.isDebugEnabled()) {
                        // From cache - log response
                        try {
                            XMLUtils.logSOAPMessage(soapContext, scalog, false, true, callersIP);
                        } catch (Exception ex) {
                            scalog.warn("Error logging soap message: " + ex.toString());
                        }
                    }
                    return ret;
                } else {
                    // Outbound SOAP response
                    if (log.isDebugEnabled()) {
                        log.debug("Got an outbound SOAP response in SoapCacheHandler handlemessage. Going to try and put it in the cache using the Cache class");
                    }
                    boolean wasPutInCache = Cache.putMessageInCache(soapContext);
                    if (log.isDebugEnabled()) {
                        log.debug("Finished trying to put response in cache. Was put in cache: " + wasPutInCache + ". Time taken: " + (System.currentTimeMillis() - start) + "ms");
                    }
                }
            } catch (Exception e) {
                log.warn("Error in SoapCacheHandler. Will ignore and continue without cache: " + e.toString());
            } finally {
                // Process statistics for outbound response
                if (!inbound || ret == false) {
                    Long startms = (Long) context.get(START_TIME_KEY);
                    String docType = (String) context.get(CacheManager.SOAP_DOC_KEY);
                    if (startms != null && docType != null) {
                        long callLatency = System.currentTimeMillis() - startms;
                        String name = (ret ? "SCA.NoCache." : "SCA.Cache.") + docType;
                        if (scalog.isDebugEnabled()) {
                            scalog.debug("{} took {}ms", name, callLatency);
                        }
                        BaseUtils.addStatisticSample(name, BaseUtils.STATISTIC_TYPE.latency, callLatency);
                    }
                }

            }

        } catch (Throwable e) {
            log.warn("Error in SoapCacheHandler while doing basic processing", e);
            throw new RuntimeException(e);
        }
        return ret;
    }

    @Override
    public boolean handleFault(MessageContext context) {
        return true;
    }

    @Override
    public Set getHeaders() {
        return Collections.emptySet();
    }

    @Override
    public void close(MessageContext messageContext) {
    }
}
