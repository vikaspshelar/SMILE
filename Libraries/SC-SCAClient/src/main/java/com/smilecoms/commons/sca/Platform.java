/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.sca;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.sd.ServiceDiscoveryAgent;
import com.smilecoms.commons.sca.direct.am.AM;
import com.smilecoms.commons.sca.direct.bm.BM;
import com.smilecoms.commons.sca.direct.et.ET;
import com.smilecoms.commons.sca.direct.hwf.HWF;
import com.smilecoms.commons.sca.direct.im.IM;
import com.smilecoms.commons.sca.direct.mm.MM;
import com.smilecoms.commons.sca.direct.pvs.PVS;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class Platform {

    private String namespace;
    private final String platformName;
    private static final Logger log = LoggerFactory.getLogger(Platform.class);
    private static final ReentrantLock lock = new ReentrantLock();
    private static final String BM_NAMESPACE = "http://xml.smilecoms.com/BM";
    private static final String AM_NAMESPACE = "http://xml.smilecoms.com/AM";
    private static final String ET_NAMESPACE = "http://xml.smilecoms.com/ET";
    private static final String IM_NAMESPACE = "http://xml.smilecoms.com/IM";
    private static final String MM_NAMESPACE = "http://xml.smilecoms.com/MM";
    private static final String PVS_NAMESPACE = "http://xml.smilecoms.com/PVS";

    private static final String HWF_NAMESPACE = "http://xml.smilecoms.com/HWF";
    private static final Map<String, Object> platformProviders = new ConcurrentHashMap<>();
    private Object oneProvider = null;

    public Platform(String name) {
        switch (name) {
            case "BM":
                this.namespace = BM_NAMESPACE;
                break;
            case "AM":
                this.namespace = AM_NAMESPACE;
                break;
            case "ET":
                this.namespace = ET_NAMESPACE;
                break;
            case "IM":
                this.namespace = IM_NAMESPACE;
                break;
            case "HWF":
                this.namespace = HWF_NAMESPACE;
                break;
            case "MM":
                this.namespace = MM_NAMESPACE;
                break;
            case "PVS":
                this.namespace = PVS_NAMESPACE;
                break;
            default:
                break;
        }
        this.platformName = name;
    }

    public boolean supports(String platformMethodName, Class classIn) {
        try {
            if (oneProvider == null) {
                oneProvider = getPlatformProvider();
            }
            if (oneProvider == null) {
                return false;
            }
            oneProvider.getClass().getMethod(platformMethodName, classIn);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public String getNamespace() {
        return namespace;
    }

    private Object getPlatformProviderForEndpoint(String endPoint) {
        if (endPoint == null) {
            // No alive endpoints
            com.smilecoms.commons.sca.SCASystemError se = new com.smilecoms.commons.sca.SCASystemError();
            se.setErrorCode(Errors.ERROR_CODE_SCA_NOTUP);
            se.setErrorDesc("Cannot connect to Platform as no endpoints are available");
            log.error("getProviderForEndpoint Error", se);
            throw se;
        }
        Object platformProvider = platformProviders.get(endPoint);
        if (platformProvider == null) {
            try {
                lock.lock();
                if ((platformProvider = platformProviders.get(endPoint)) == null) {
                    // only do once
                    platformProvider = getPlatformProvider(endPoint);
                    platformProviders.put(endPoint, platformProvider);
                }
            } finally {
                lock.unlock();
            }
        }
        if (platformProvider != null) {
            String fi = BaseUtils.getPropertyFailFast("env.soap.client.contentnegotiation", "none");
            log.debug("Using fast infoset value [{}] when calling Platforms", fi);
            ((BindingProvider) platformProvider).getRequestContext().put("com.sun.xml.ws.client.ContentNegotiation", fi);
        }
        return platformProvider;
    }

    private Object getPlatformProvider() throws Exception {
        String endPoint = ServiceDiscoveryAgent.getInstance().getAvailableService(platformName).getURL();
        return getPlatformProvider(endPoint);
    }

    private Object getPlatformProvider(String endPoint) {
        Object provider = null;
        try {
            switch (platformName) {
                case "BM": {
                    URL url = getClass().getResource("/BMServiceDefinition.wsdl");
                    provider = new BM(url, new QName(Platform.BM_NAMESPACE, "BM")).getBMSoap();
                    break;
                }
                case "AM": {
                    URL url = getClass().getResource("/AMServiceDefinition.wsdl");
                    provider = new AM(url, new QName(Platform.AM_NAMESPACE, "AM")).getAMSoap();
                    break;
                }
                case "ET": {
                    URL url = getClass().getResource("/ETServiceDefinition.wsdl");
                    provider = new ET(url, new QName(Platform.ET_NAMESPACE, "ET")).getETSoap();
                    break;
                }
                case "IM": {
                    URL url = getClass().getResource("/IMServiceDefinition.wsdl");
                    provider = new IM(url, new QName(Platform.IM_NAMESPACE, "IM")).getIMSoap();
                    break;
                }
                case "HWF": {
                    URL url = getClass().getResource("/HWFServiceDefinition.wsdl");
                    provider = new HWF(url, new QName(Platform.HWF_NAMESPACE, "HWF")).getHWFSoap();
                    break;
                }
                case "MM": {
                    URL url = getClass().getResource("/MMServiceDefinition.wsdl");
                    provider = new MM(url, new QName(Platform.MM_NAMESPACE, "MM")).getMMSoap();
                    break;
                }
                case "PVS": {
                    URL url = getClass().getResource("/PVSServiceDefinition.wsdl");
                    provider = new PVS(url, new QName(Platform.PVS_NAMESPACE, "PVS")).getPVSSoap();
                    break;
                }

                default:
                    throw new RuntimeException("Unsupported platform " + platformName);
            }
            ((BindingProvider) provider).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endPoint);
            ((BindingProvider) provider).getRequestContext().put("set-jaxb-validation-event-handler", "false");
            log.debug("SOAP provider class is [{}]", provider.getClass().getName());
        } catch (Exception ex) {
            log.error("Could not initialise a platform client : " + ex.toString());
            log.error("SCADelegate", ex);
            throw new RuntimeException(ex);
        }
        return provider;
    }

    /**
     * Sets the timeouts prior to the call
     *
     * @param connectTimeoutMillis Number of milliseconds to timeout after
     * trying to do a socket connect
     * @param responseTimeoutMillis Number of milliseconds to timeout after
     * sending the request on the socket
     */
    private void setTimeouts(Object provider, int connectTimeoutMillis, int responseTimeoutMillis) {
        ((BindingProvider) provider).getRequestContext().put("com.sun.xml.ws.connect.timeout", connectTimeoutMillis);
        ((BindingProvider) provider).getRequestContext().put("com.sun.xml.ws.request.timeout", responseTimeoutMillis);
        ((BindingProvider) provider).getRequestContext().put("javax.xml.ws.client.connectionTimeout", connectTimeoutMillis);
        ((BindingProvider) provider).getRequestContext().put("javax.xml.ws.client.receiveTimeout", responseTimeoutMillis);
    }

    private static final ThreadLocal<String> lastEndpoint = new ThreadLocal<>();

    public String getLastEndpoint() {
        return lastEndpoint.get();
    }

    Object invoke(String platformMethodName, Class classIn, Object objIn, String endpoint, SCAProperties props) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, Exception {
        if (endpoint == null) {
            endpoint = ServiceDiscoveryAgent.getInstance().getAvailableService(platformName).getURL();
        }
        Object provider = getPlatformProviderForEndpoint(endpoint);
        Method soapMethod = provider.getClass().getMethod(platformMethodName, classIn);
        if (log.isDebugEnabled()) {
            log.debug("Calling Platform at " + endpoint);
        }
        try {
            int requestTimeoutMillis;
            int connectTimeoutMillis;

            if (props != null) {
                if (props.getRequestTimeoutMillis() > 0) {
                    requestTimeoutMillis = props.getRequestTimeoutMillis();
                } else {
                    requestTimeoutMillis = BaseUtils.getIntPropertyFailFast(SCAConstants.REQUEST_TIMEOUT_KEY, 40000);
                }
                if (props.getConnectTimeoutMillis() > 0) {
                    connectTimeoutMillis = props.getConnectTimeoutMillis();
                } else {
                    connectTimeoutMillis = BaseUtils.getIntPropertyFailFast(SCAConstants.CONNECT_TIMEOUT_KEY, 2000);
                }
            } else {
                requestTimeoutMillis = BaseUtils.getIntPropertyFailFast(SCAConstants.REQUEST_TIMEOUT_KEY, 40000);
                connectTimeoutMillis = BaseUtils.getIntPropertyFailFast(SCAConstants.CONNECT_TIMEOUT_KEY, 2000);
            }
            setTimeouts(provider, connectTimeoutMillis, requestTimeoutMillis);
        } catch (Exception ex) {
            // In case there is an error on the getPropertyCalls, default the values
            log.warn("Error setting timeouts for Platform call. Will use default timeouts : " + ex.toString());
            setTimeouts(provider, Integer.parseInt(SCAConstants.DEFAULT_CONNECT_TIMEOUT), Integer.parseInt(SCAConstants.DEFAULT_REQUEST_TIMEOUT));
        }
        lastEndpoint.set(endpoint);
        return soapMethod.invoke(provider, objIn);
    }
}
