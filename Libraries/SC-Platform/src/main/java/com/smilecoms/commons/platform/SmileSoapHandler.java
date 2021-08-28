package com.smilecoms.commons.platform;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;
import com.smilecoms.commons.base.sd.SOAPService;
import com.smilecoms.commons.base.sd.ServiceDiscoveryAgent;
import com.smilecoms.commons.util.ExceptionManager;
import com.smilecoms.commons.util.Utils;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import org.apache.commons.lang.mutable.MutableInt;
import org.slf4j.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class SmileSoapHandler implements SOAPHandler {

    private static final String KEY_MSSTART = "msstart";
    private static final Logger log = LoggerFactory.getLogger(SmileSoapHandler.class);
    private static final String ENCODING = "utf-8";
    private static final String STAT_PREFIX = "Plat_";
    private static final String PM_PLATFORM = "PM";
    private static final Map<String, MutableInt> concurrencyStore = new java.util.concurrent.ConcurrentHashMap<>();
    private static final int DEFAULT_MAX_CONCURRENCY = 300;
    private static final String KEY_UUID = "contextkey";
    private static final Map<String, MessageContext> contextMap = new ConcurrentHashMap<>(10);
    private static final String ELEMENT_ERROR_TYPE = "ErrorType";
    private static final String ERROR_TYPE_SYSTEM = "system";
    private static long ipListLastRefreshed = System.currentTimeMillis(); // dont bother checking for first 60s
    private static Set<String> allowedIps = null;
    private static final ReentrantLock lock = new ReentrantLock();
    private static final String RESMATCH = "<Done>true</Done>";
    private static final String PD_TEMP = "<soapenv:Envelope xsi:schemaLocation=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:x=\"http://xml.smilecoms.com/schema/XXX\"><soapenv:Body><x:IsUpRequest>_TIME_</x:IsUpRequest></soapenv:Body></soapenv:Envelope>";
    private static final int SHORT_TIMEOUT_MILLIS = 500;
    private static final int LONG_TIMEOUT_MILLIS = 2000;
    private static boolean publishingFast = true;
    private static ScheduledFuture runner1 = null;

    static {
        runner1 = Async.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                new SmileSoapHandler().trigger();
            }
        }, 0, 3000);
    }

    private void trigger() {
        try {
            registerAllPlatformsAsAvailable();
            if (publishingFast && !BaseUtils.IN_GRACE_PERIOD) {
                publishingFast = false;
                Async.cancel(runner1);
                runner1 = Async.scheduleAtFixedRate(new SmileBaseRunnable("SD.registerPlatsAsAvailable") {
                    @Override
                    public void run() {
                        new SmileSoapHandler().trigger();
                    }
                }, 1000, 60000);
            }
        } catch (Exception e) {
            log.warn("Error in trigger:", e);
        }
    }

    private void registerAllPlatformsAsAvailable() throws MalformedURLException {
        log.debug("Registering platforms as available");
        int port = getListeningPort();
        int SHORT_TEST_MILLIS = BaseUtils.getIntPropertyFailFast("env.sd.platforms.short.test.millis",3000);
        int LONG_TEST_MILLIS = BaseUtils.getIntPropertyFailFast("env.sd.platforms.long.test.millis",10000);
        registerPlatformAsAvailable("PM", "/PM/PropertyManager", PD_TEMP.replace("XXX", "PM"), RESMATCH, port, SHORT_TEST_MILLIS, SHORT_TIMEOUT_MILLIS);
        registerPlatformAsAvailable("BM", "/BM/BalanceManager", PD_TEMP.replace("XXX", "BM"), RESMATCH, port, SHORT_TEST_MILLIS, SHORT_TIMEOUT_MILLIS);
        registerPlatformAsAvailable("IM", "/IM/IdentityManager", PD_TEMP.replace("XXX", "IM"), RESMATCH, port, SHORT_TEST_MILLIS, SHORT_TIMEOUT_MILLIS);
        registerPlatformAsAvailable("CM", "/CM/CatalogManager", PD_TEMP.replace("XXX", "CM"), RESMATCH, port, SHORT_TEST_MILLIS, SHORT_TIMEOUT_MILLIS);
        registerPlatformAsAvailable("MM", "/MM/MessageManager", PD_TEMP.replace("XXX", "MM"), RESMATCH, port, SHORT_TEST_MILLIS, SHORT_TIMEOUT_MILLIS);
        registerPlatformAsAvailable("ET", "/ET/EventManager", PD_TEMP.replace("XXX", "ET"), RESMATCH, port, SHORT_TEST_MILLIS, SHORT_TIMEOUT_MILLIS);
        registerPlatformAsAvailable("AM", "/AM/AddressManager", PD_TEMP.replace("XXX", "AM"), RESMATCH, port, SHORT_TEST_MILLIS, SHORT_TIMEOUT_MILLIS);
        registerPlatformAsAvailable("POS", "/POS/POSManager", PD_TEMP.replace("XXX", "POS"), RESMATCH, port, SHORT_TEST_MILLIS, SHORT_TIMEOUT_MILLIS);
        registerPlatformAsAvailable("PVS", "/PVS/PrepaidVoucherSystem", PD_TEMP.replace("XXX", "PVS"), RESMATCH, port, SHORT_TEST_MILLIS, SHORT_TIMEOUT_MILLIS);
        registerPlatformAsAvailable("PC", "/PC/PolicyControl", PD_TEMP.replace("XXX", "PC"), RESMATCH, port, SHORT_TEST_MILLIS, SHORT_TIMEOUT_MILLIS);
        registerPlatformAsAvailable("IMSSC", "/IMSSC/IMSSessionControl", PD_TEMP.replace("XXX", "IMSSC"), RESMATCH, port, SHORT_TEST_MILLIS, SHORT_TIMEOUT_MILLIS);
        registerPlatformAsAvailable("HWF", "/HWF/HWFManager", PD_TEMP.replace("XXX", "HWF"), RESMATCH, port, SHORT_TEST_MILLIS, SHORT_TIMEOUT_MILLIS);
        registerPlatformAsAvailable("SN", "/SN/StickyNoteManager", PD_TEMP.replace("XXX", "SN"), RESMATCH, port, SHORT_TEST_MILLIS, SHORT_TIMEOUT_MILLIS);
        registerPlatformAsAvailable("CTI", "/CTI/CTIManager", PD_TEMP.replace("XXX", "CTI"), RESMATCH, port, LONG_TEST_MILLIS, SHORT_TIMEOUT_MILLIS);
        registerPlatformAsAvailable("TT", "/TT/TroubleTicketManager", PD_TEMP.replace("XXX", "TT"), RESMATCH, port, SHORT_TEST_MILLIS, SHORT_TIMEOUT_MILLIS);
        registerPlatformAsAvailable("TNF", "/TNF/TNFManager", PD_TEMP.replace("XXX", "TNF"), RESMATCH, port, LONG_TEST_MILLIS, LONG_TIMEOUT_MILLIS);
    }

    private void registerPlatformAsAvailable(String platform, String addPart, String postData, String resMatch, int port, int testGapMS, int timeout) throws MalformedURLException {
        SOAPService service = new SOAPService(platform, "http", port, addPart);
        service.setWeight(5);
        service.setClientHostnameRegexMatch(BaseUtils.getHostNameFromKernel());
        service.setTestData(postData);
        service.setTestResponseCriteria(resMatch);
        service.setGapBetweenTestsMillis(testGapMS);
        service.setTestTimeoutMillis(timeout);
        // Platform JVM's must be hazelcast servers
        ServiceDiscoveryAgent.getInstance().publishService(service);
    }

    private boolean handle(MessageContext context, boolean isFault) {

        log.debug("SmileSoapHandler is in handle message");
        MessageContext lastContext = null;
        String platform = ((javax.xml.namespace.QName) context.get(SOAPMessageContext.WSDL_SERVICE)).getLocalPart();
        boolean inbound = !(Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        log.debug("Platform for this message is [{}] inbound=[{}]", platform, inbound);
        String callersIP;
        if (inbound) {
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
                log.warn("IP address [{}] is denied from using SOAP services", callersIP);
                throw new RuntimeException("Service Denied " + callersIP);
            }
        }

        try {

            checkConcurrency(inbound, platform);

            String wsdlOperation = null;
            if (!inbound) {
                QName q = (QName) context.get(SOAPMessageContext.WSDL_OPERATION);
                if (q == null) {
                    log.debug("Operation not supported on the soap Endpoint. Probably a response to an isup ");
//                    for (Entry entry :context.entrySet()) {
//                        log.debug("Key [{}] value [{}]", entry.getKey(), entry.getValue());
//                    }
                    return true;
                }
                wsdlOperation = q.getLocalPart();
            }

            // Capture statistics on this call. If its inbound, then put the start time in the context.
            // If its outbound, get the start time, calculate the latency and add the sample.
            if (inbound) {
                context.put(KEY_MSSTART, System.currentTimeMillis());
                /* Generate a unique ID, store this context in a HashMap with this ID and inject the ID into the MessageContext */
                String uuid = Utils.getUUID();
                contextMap.put(uuid, context);
                context.put(KEY_UUID, uuid);
            } else {
                Long start = (Long) context.get(KEY_MSSTART);
                if (start != null && wsdlOperation != null && platform != null) {
                    long latency = System.currentTimeMillis() - start;
                    String statName = STAT_PREFIX + platform + "." + wsdlOperation;
                    if (log.isDebugEnabled()) {
                        log.debug("Latency: {} : {}ms", statName, latency);
                    }
                    BaseUtils.addStatisticSample(statName, BaseUtils.STATISTIC_TYPE.latency, latency);
                }
                /* Retrieve the original context for this "response" */
                String contextKey = (String) context.get(KEY_UUID);
                if (contextKey != null) {
                    lastContext = contextMap.remove(contextKey);
                }
            }

            if ((log.isDebugEnabled()) || (isFault)) {

                Level logLevel = Level.FINEST;
                if (isFault) {
                    logLevel = Level.WARNING;
                }

                SOAPMessage message = ((SOAPMessageContext) context).getMessage();
                Document doc = getDomDocument(message);
                boolean isSystemFault = false;

                /* If we pick up a FAULT, determine if it is a SYSTEM fault */
                if ((isFault) && (lastContext != null)) {
                    if (doc != null) {

                        String errorType;
                        NodeList nodeList = doc.getElementsByTagName(ELEMENT_ERROR_TYPE);
                        if (nodeList != null) {
                            int nodeListSize = nodeList.getLength();
                            if (nodeListSize > 0) {
                                Node errorTypeNode = nodeList.item(0);
                                if (errorTypeNode != null) {
                                    NodeList errorTypeNodeChildren = errorTypeNode.getChildNodes();
                                    if (errorTypeNodeChildren != null) {
                                        int errorTypeNodeChildrenSize = errorTypeNodeChildren.getLength();
                                        if (errorTypeNodeChildrenSize > 0) {
                                            Node errorTypeNodeChild = errorTypeNodeChildren.item(0);
                                            if (errorTypeNodeChild != null) {
                                                errorType = errorTypeNodeChild.getNodeValue();

                                                if (log.isDebugEnabled()) {
                                                    log.debug("errorType: [" + errorType + "]");
                                                }
                                                if (errorType.equalsIgnoreCase(ERROR_TYPE_SYSTEM)) {
                                                    isSystemFault = true;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (((log.isDebugEnabled()) || (isSystemFault)) && !BaseUtils.IN_GRACE_PERIOD) {
                    dumpSOAPMessage(doc, inbound, wsdlOperation, true, logLevel, null);

                    if ((isSystemFault) && (lastContext != null)) {
                        try {
                            SOAPMessage lastMessage = ((SOAPMessageContext) lastContext).getMessage();
                            doc = getDomDocument(lastMessage);
                            dumpSOAPMessage(doc, Boolean.TRUE, wsdlOperation, false, logLevel, null);
                        } catch (NullPointerException npe) {
                            log.debug("NPE when getting last message");
                        }
                    }
                }
            } // end if fault or in debug

        } catch (ExcessiveConcurrencyException ece) {
            throw ece;
        } catch (NullPointerException npe) {
            log.warn("Error in SmileSoapHandler. Continuing anyway: ", npe);
        } catch (Exception e) {
            log.warn("Error in SmileSoapHandler. Continuing anyway: ", e);
            // Under some weird errors, the concurrency did not seem to decrement
            log.debug("Decrementing concurrency just in case platform does not process the request");
            changeConcurrency(-1, platform);
        } catch (Throwable t) {
            log.warn("Error in SmileSoapHandler. Continuing anyway: ", t);
            // Under some weird errors, the concurrency did not seem to decrement
            log.debug("Decrementing concurrency just in case platform does not process the request");
            changeConcurrency(-1, platform);
        } finally {
            if (!inbound) {
                // was a memory leak...
                log.debug("Removing context from map in finally block");
                String contextKey = (String) context.get(KEY_UUID);
                if (contextKey != null) {
                    contextMap.remove(contextKey);
                }
            }
        }
        return true;
    }

    @Override
    public boolean handleMessage(MessageContext context) {
        return handle(context, false);
    }

    @Override
    public boolean handleFault(MessageContext context) {
        return handle(context, true);
    }

    @Override
    public Set getHeaders() {
        return null;
    }

    @Override
    public void close(MessageContext messageContext) {
    }

    private void checkConcurrency(boolean inbound, String platform) {

        try {
            if (inbound) {
                int maxConcurrency = getPlatformsMaxConcurrency(platform);
                int currentConcurrency = getCurrentConcurrency(platform);
                if (log.isDebugEnabled()) {
                    log.debug("Concurrency on platform " + platform + " is currently " + currentConcurrency + " and will be " + (currentConcurrency + 1) + " when this call is processed. Max allowed is " + maxConcurrency);
                }

                if (currentConcurrency >= maxConcurrency) {
                    ExcessiveConcurrencyException ece = new ExcessiveConcurrencyException("Excessive concurrency on platform " + platform + ". Concurrency is already at " + currentConcurrency + ". Max=" + maxConcurrency);
                    // Report problem to Ops management
                    ExceptionManager em = new ExceptionManager(this.getClass().getName());
                    em.reportError(ece);
                    throw ece;
                }

                changeConcurrency(1, platform);
            } else {
                changeConcurrency(-1, platform);
            }
        } finally {
            // write concurrency statistics with name e.g. Plat_BM and type concurrency
            BaseUtils.addStatisticSample(STAT_PREFIX + platform, BaseUtils.STATISTIC_TYPE.concurrency, getCurrentConcurrency(platform));
        }
    }

    private int getCurrentConcurrency(String platform) {
        MutableInt currentConcurrency = concurrencyStore.get(platform);
        if (currentConcurrency == null) {
            return 0;
        }
        return currentConcurrency.intValue();
    }

    private void changeConcurrency(int change, String platform) {
        MutableInt currentConcurrency = concurrencyStore.get(platform);
        if (currentConcurrency == null) {
            // Initialise the concurrency Integer in a thread safe manner
            try {
                lock.lock();
                currentConcurrency = concurrencyStore.get(platform);
                if (currentConcurrency == null) {
                    currentConcurrency = new MutableInt(0);
                    concurrencyStore.put(platform, currentConcurrency);
                }
            } finally {
                lock.unlock();
            }
        }

        // Be thread safe
        synchronized (currentConcurrency) {
            currentConcurrency.add(change);
            if (currentConcurrency.intValue() < 0) {
                currentConcurrency.setValue(0);
            }
        }
    }

    private String getMessageEncoding(SOAPMessage msg) throws SOAPException {
        String encoding = ENCODING;
        if (msg.getProperty(SOAPMessage.CHARACTER_SET_ENCODING) != null) {
            encoding = msg.getProperty(SOAPMessage.CHARACTER_SET_ENCODING).toString();
        }
        return encoding;
    }

    private Document getDomDocument(SOAPMessage msg) {
        if (msg == null) {
            return null;
        }
        Document doc = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            msg.writeTo(baos);
            String msgString = baos.toString(getMessageEncoding(msg));
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(msgString));
            doc = db.parse(is);
        } catch (Throwable e) {
            log.warn("Error in getDomDocument: ", e);
        }
        return doc;
    }

    private boolean dumpSOAPMessage(Document doc, boolean inbound, String wsdlOperation, boolean currentMessage, Level logLevel, String onlyLogIfContains) {
        if (doc == null) {
            return false;
        }
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(doc);
            transformer.transform(source, result);

            String msgString = result.getWriter().toString();
            if (onlyLogIfContains != null && !msgString.contains(onlyLogIfContains)) {
                return false;
            }
            if (msgString.length() > 10000) {
                msgString = msgString.substring(0, 10000) + "...TOO LARGE TO DISPLAY FULLY...";
            }

            String preMsg;
            if (inbound) {
                if (currentMessage) {
                    preMsg = "************** DUMP OF INBOUND SOAP MESSAGE **************";
                } else {
                    preMsg = "************** DUMP OF INBOUND SOAP MESSAGE (SOAP FAULT CAUSE) **************";
                }
            } else {
                preMsg = "************** DUMP OF OUTBOUND SOAP MESSAGE FROM " + wsdlOperation + " **************";
            }
            if (logLevel.equals(Level.FINEST)) {
                log.debug(preMsg);
                log.debug(msgString);
                log.debug("************** END OF SOAP MESSAGE **************");
            }

            if (logLevel.equals(Level.WARNING)) {
                log.warn(preMsg);
                log.warn(msgString);
                log.warn("************** END OF SOAP MESSAGE **************");
            }
            return true;

        } catch (Exception e) {
            log.warn("Error: ", e);
        }
        return false;
    }

    private int getPlatformsMaxConcurrency(String platform) {
        if (platform.equals(PM_PLATFORM) || !BaseUtils.isPropsAvailable()) {
            // Dont cause recursive loop for PM!!! I.e. cant look it up from a property
            return DEFAULT_MAX_CONCURRENCY;
        }
        int ret;
        try {
            ret = BaseUtils.getIntPropertyFailFast("env.platform.max.concurrency.per.jvm." + platform, -1);
            if (ret == -1) {
                ret = BaseUtils.getIntPropertyFailFast("env.platform.max.concurrency.per.jvm.default", DEFAULT_MAX_CONCURRENCY);
            }
        } catch (Exception e) {
            log.warn("Could not get property env.platform.max.concurrency.per.jvm." + platform + " nor env.platform.max.concurrency.per.jvm.default. Will use default max concurrency of " + DEFAULT_MAX_CONCURRENCY);
            ret = DEFAULT_MAX_CONCURRENCY;
        }
        return ret;
    }

    private static int listeningPort = 0;

    public static int getListeningPort() {
        if (listeningPort > 0) {
            return listeningPort;
        }
        listeningPort = 8003;
        if (System.getProperty("HTTP_BIND_PORT") != null) {
            listeningPort = Integer.parseInt(System.getProperty("HTTP_BIND_PORT"));
        }
        log.warn("Platforms are listening on port {}", listeningPort);
        return listeningPort;
    }

}
