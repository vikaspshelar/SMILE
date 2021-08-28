package com.smilecoms.commons.sca;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.sd.ServiceDiscoveryAgent;
import com.smilecoms.commons.util.ExceptionManager;
import com.smilecoms.commons.util.Stopwatch;
import com.smilecoms.commons.util.Utils;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import org.slf4j.*;

/**
 * Delegate class to make life easier when calling SCA. It ensures that we dont
 * need to make any changes to the ws-import generated classes as all
 * customisations to call SCA (logging etc) are held in this delegate class
 * instead. It also takes care of load balancing and has nice utility methods
 * for validating a message prior to sending it
 *
 * @author PCB
 */
public class SCADelegate {
    //Logging     

    private static final String CLASS = SCADelegate.class.getName();
    private static final Logger log = LoggerFactory.getLogger(CLASS);    //sca instance
    private static final String XERCES_CALENDAR = "com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl";
    public static final String NOT_LOGGED_IN_USER = "NOT_LOGGED_IN";
    private static final ReentrantLock lock = new ReentrantLock();
    private static final ThreadLocal<String> scaCallInfo = new ThreadLocal<>();
    private static final ThreadLocal<Float> scaCallTime = new ThreadLocal<>();
    private static final Map<String, SCASoap> scaProviders = new ConcurrentHashMap<>();
    private static final ThreadLocal<CallersRequestContext> requestStore = new ThreadLocal();
    private static final Set<String> adminRole = new HashSet();

    static {
        adminRole.add("Administrator");
    }

    private SCASoap getProviderForEndpoint(String endPoint) {
        if (endPoint == null) {
            // No alive endpoints
            com.smilecoms.commons.sca.SCASystemError se = new com.smilecoms.commons.sca.SCASystemError();
            se.setErrorCode(Errors.ERROR_CODE_SCA_NOTUP);
            se.setErrorDesc("Cannot connect to SCA");
            log.error("getProviderForEndpoint Error", se);
            throw se;
        }
        SCASoap scaProvider = scaProviders.get(endPoint);
        if (scaProvider == null) {
            try {
                lock.lock();
                scaProvider = scaProviders.get(endPoint);
                if (scaProvider == null) {
                    // only do once
                    scaProvider = getProvider(endPoint);
                    scaProviders.put(endPoint, scaProvider);
                }
            } finally {
                lock.unlock();
            }
        }
        if (scaProvider != null) {
            String fi = BaseUtils.getPropertyFailFast("env.soap.client.contentnegotiation", "none");
            log.debug("Using fast infoset value [{}] when calling SCA", fi);
            ((BindingProvider) scaProvider).getRequestContext().put("com.sun.xml.ws.client.ContentNegotiation", fi);
        }
        return scaProvider;
    }

    private SCASoap getProvider(String endPoint) {
        SCASoap sca;
        try {
            // local copy of sca is static and must be initialised once
            log.debug("SCA Delegate is initialising SCADelegate static data by creating the SCASoap instance for endpoint [{}]", endPoint);
            URL url = getClass().getResource("/SCAServiceDefinition.wsdl");
            SCA service = new SCA(url, new QName(SCAConstants.SCA_NAMESPACE, "SCA"));
            sca = service.getSCASoap();
            log.debug("SOAP provider class is [{}]", sca.getClass().getName());
            // Set the endpoint
            ((BindingProvider) sca).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endPoint);
            ((BindingProvider) sca).getRequestContext().put("set-jaxb-validation-event-handler", "false");
            log.debug("Finished initialisation of SCADelegate static data by creating the SCASoap instance for endpoint [{}]", endPoint);
        } catch (Exception ex) {
            sca = null;
            log.error("Could not initialise SCA client : " + ex.toString());
            throw new RuntimeException(ex);
        }
        return sca;
    }

    /**
     * Creates the SCA context data to be sent with the request
     *
     * @param SCAMethodToCall
     * @param objIn
     * @param originatingIP
     * @param originatingIdentity
     * @param SCAEndpoint
     * @param txID
     */
    private void addSCAContextData(SCAObject objIn, String originatingIP, String originatingIdentity, Set callersRoles, String tenant) {
        if (objIn.getSCAContext() == null) {
            objIn.setSCAContext(new SCAContext());
        }
        //Add TXID only if its blank or null
        if (objIn.getSCAContext().getTxId() == null || objIn.getSCAContext().getTxId().isEmpty()) {
            objIn.getSCAContext().setTxId(Utils.getUUID());
        }
        //Add calling smile ID
        objIn.getSCAContext().setOriginatingIdentity(originatingIdentity);
        //Add Callers IP
        objIn.getSCAContext().setOriginatingIP(originatingIP);
        //Add Callers Tenant
        objIn.getSCAContext().setTenant(tenant);
        if (objIn.getSCAContext().getTenant() == null || objIn.getSCAContext().getTenant().isEmpty()) {
            log.warn("SCA request has a null or empty tenant. Will default to sm while tenant functionality is being built. Stack trace [{}]", Utils.getStackTrace(new Exception()));
            objIn.getSCAContext().setTenant("sm");
        }
        //Add Callers Roles
        objIn.getSCAContext().getRoles().clear();
        if (BaseUtils.getBooleanPropertyFailFast("global.sca.check.permissions", true)) {
            objIn.getSCAContext().getRoles().addAll(callersRoles);
        }
    }

    protected static void setThreadsRequestContext(CallersRequestContext ctx) {
        requestStore.set(ctx);
    }

    protected static void removeThreadsRequestContext() {
        requestStore.remove();
    }

    public static CallersRequestContext getThreadsRequestContext() {
        return requestStore.get();
    }

    /**
     * Logging Utility. Logs something surrounded with # for easy viewing
     *
     * @param msg The string to log
     */
    private void logHashes(String msg) {
        log.debug("#########################################################################################################################");
        log.debug("# " + msg);
        log.debug("#########################################################################################################################");
    }

    /**
     * Helper method to make it easy to call into the ESB via SCA
     *
     * This rather cleaver method calls SCA and puts nice error handling around
     * it without having to change the generated ws client itself. This makes it
     * easy to make changes on the SCA web service without making it a mission
     * to change the portal
     *
     * As per the SCA schema, SCA always returns exceptions of type
     * SCAError_Exception. This method checks for this and extracts the embedded
     * error info and puts it into a local SCAErr class that does not require
     * calls to getFaultInfo to get the embedded XML error info
     *
     * @param asAdministrator
     * @param props
     * @return Object The object returned by SCA
     * @param SCAMethodToCall Name of the method to call in SCA e.g. getCustomer
     * @param objIn The object to pass to SCA
     */
    protected Object callSCA(String SCAMethodToCall, Object objIn, boolean asAdministrator, SCAProperties props) throws com.smilecoms.commons.sca.SCAErr {
        return callSCA(SCAMethodToCall, objIn, null, asAdministrator, props);
    }

    protected Object callSCA(String SCAMethodToCall, Object objIn, String endpoint, boolean asAdministrator, SCAProperties props) throws com.smilecoms.commons.sca.SCAErr {
        if (asAdministrator) {
            CallersRequestContext ctx = getThreadsRequestContext();
            String ip;
            if (ctx != null) {
                ip = ctx.getRemoteAddr();
            } else {
                ip = BaseUtils.getIPAddress();
            }
            addSCAContextData((SCAObject) objIn, ip, "admin", adminRole, ADMIN_TENANT);
        } else {
            CallersRequestContext ctx = getThreadsRequestContext();
            if (ctx == null || ctx.getRemoteUser() == null) {
                com.smilecoms.commons.sca.SCASystemError se = new com.smilecoms.commons.sca.SCASystemError();
                se.setErrorCode(Errors.ERROR_CODE_SCA_INVALID_METHOD);
                se.setErrorDesc("Programming error. You have attempted to use a Web instance of SCA outside of a web container");
                se.setRequest(SCAMarshaller.marshalToString(objIn));
                log.error("callSCA", se);
                new ExceptionManager(log).reportError(se);
                throw se;
            }
            addSCAContextData((SCAObject) objIn, ctx.getRemoteAddr(), ctx.getRemoteUser() == null ? NOT_LOGGED_IN_USER : ctx.getRemoteUser(), ctx.getUsersRoles(), ctx.getTenant());
        }

        return callSCA(SCAMethodToCall, (SCAObject) objIn, endpoint, props);
    }

    /**
     * Helper method to make it easy to call into the ESB via SCA
     *
     * This rather cleaver method calls SCA and puts nice error handling around
     * it without having to change the generated ws client itself. This makes it
     * easy to make changes on the SCA web service without making it a mission
     * to change the portal
     *
     * As per the SCA schema, SCA always returns exceptions of type
     * SCAError_Exception. This method checks for this and extracts the embedded
     * error info and puts it into a local SCAErr class that does not require
     * calls to getFaultInfo to get the embedded XML error info
     *
     * @param SCAMethodToCall
     * @param objIn
     * @param originatingIP
     * @param originatingIdentity
     * @param endPoint
     * @return SCAResult
     * @throws com.smilecoms.commons.sca.SCAErr
     */
    private Object callSCA(String SCAMethodToCall, SCAObject objIn, String endPoint, SCAProperties props) throws com.smilecoms.commons.sca.SCAErr {
        if (log.isDebugEnabled()) {
            log.debug("ENTERING SCADelegate : callSCA. Method being called on SCA : " + SCAMethodToCall);
        }

        // Variables
        Object objRet = null;

        try {

            // If object passed in is null then throw error
            if (objIn == null) {
                com.smilecoms.commons.sca.SCASystemError se = new com.smilecoms.commons.sca.SCASystemError();
                // Put in request data                            
                se.setRequest(SCAConstants.NULL);
                se.setErrorCode(Errors.ERROR_CODE_SCA_NULLIN);
                se.setErrorDesc("Request object going to SCA was null. Make sure the action bean calling SCA has been coded/populated correctly." + " Method being called on SCA is : " + SCAMethodToCall);
                log.error("callSCA", se);
                throw se;
            }

            if (BaseUtils.getBooleanPropertyFailFast("global.sca.check.permissions", true)) {
                objIn.getSCAContext().setMethod(SCAMethodToCall);
            }

            // We must never allow someone to submit data to SCA is its been Obviscated
            // Not only do we not want the info overwritten with hashes, but if you cant see something how can one ever be allowed to update it
            // Only way to override this is to call SCA as Admin to be sure no data is obviscated - but this override must be used with caution
            // As it could elevate the priviledges of the caller
            if (objIn.getSCAContext().getObviscated() != null && objIn.getSCAContext().getObviscated().equals("ob")) {
                com.smilecoms.commons.sca.SCABusinessError be = new com.smilecoms.commons.sca.SCABusinessError();
                // Put in request data                            
                be.setRequest(SCAConstants.NULL);
                be.setErrorCode(Errors.ERROR_CODE_CANNOT_POST_OBVISCATED_DATA);
                be.setErrorDesc("Request object going to SCA has obviscated data");
                log.error("callSCA", be);
                throw be;
            }

            Class classIn = objIn.getClass();
            Method m;
            //Set endpoint with loadbalancing and failover aware endpoint Manager (if one wasnt passed in)       
            if (endPoint == null) {
                try {
                    endPoint = ServiceDiscoveryAgent.getInstance().getAvailableService("SCA").getURL();
                } catch (Exception e) {
                    log.warn("Could not get SCA endpoint [{}]", e.toString());
                }
            }

            SCASoap sca = getProviderForEndpoint(endPoint);

            //Validate request against SCA XSD if configured to do so
            ValidationErrorCollector errs = SCAValidator.validateSCAMessage(objIn);
            if (errs.getErrorCount() > 0) {
                com.smilecoms.commons.sca.SCABusinessError be = new com.smilecoms.commons.sca.SCABusinessError();
                be.setErrorCode(Errors.ERROR_CODE_SCA_INVALID_MESSAGE);
                be.setErrorDesc(errs.getAllMessages());
                if (log.isDebugEnabled()) {
                    log.debug("Validation Errors : " + be.getErrorDesc());
                }
                throw be;
            }
            //Get method to call via reflection
            try {
                //Hack to change Integer into int
                if (classIn.getName().equalsIgnoreCase(SCAConstants.INTEGER)) {
                    classIn = int.class;
                }
                //Hack to change com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl
                //into javax.xml.datatype.XMLGregorianCalendar
                if (classIn.getName().equalsIgnoreCase(XERCES_CALENDAR)) {
                    classIn = javax.xml.datatype.XMLGregorianCalendar.class;
                }
                m = sca.getClass().getMethod(SCAMethodToCall, classIn);
            } catch (Exception e) {
                // Cannot find method
                com.smilecoms.commons.sca.SCASystemError se = new com.smilecoms.commons.sca.SCASystemError(e);
                se.setErrorCode(Errors.ERROR_CODE_SCA_INVALID_METHOD);
                se.setErrorDesc("Requested method does not exist on SCA : " + SCAMethodToCall + " passing " + objIn.getClass().getName() + " : " + e.toString());
                log.error("callSCA", se);
                throw se;
            }

            //Call SCA
            try {

                if (log.isDebugEnabled()) {
                    logHashes("Calling SCA SOAP Method : " + SCAMethodToCall + " at " + endPoint + " passing " + classIn.toString());
                }
                // Log request
                if (log.isDebugEnabled()) {
                    SCAMarshaller.logObject(objIn, "XML being sent to SCA:");
                }

                //Very important to set timeouts so we dont get stuck threads
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
                    setTimeouts(sca, connectTimeoutMillis, requestTimeoutMillis);
                } catch (Exception ex) {
                    // In case there is an error on the getPropertyCalls, default the values
                    log.warn("Error setting timeouts for Platform call. Will use default timeouts : " + ex.toString());
                    setTimeouts(sca, Integer.parseInt(SCAConstants.DEFAULT_CONNECT_TIMEOUT), Integer.parseInt(SCAConstants.DEFAULT_REQUEST_TIMEOUT));
                }

                Stopwatch.start();

                try {
                    objRet = m.invoke(sca, objIn);
                } finally {
                    Stopwatch.stop();
                    addSCACallInfo(SCAMethodToCall, Stopwatch.millis());
                }

                // Log result
                if (log.isDebugEnabled()) {
                    SCAMarshaller.logObject(objRet, "XML received back from SCA:");
                }

            } catch (Exception e) {
                // Dig down into the true underlying error
                Throwable underlying = e.getCause();
                while (underlying != null && underlying.getCause() != null) {
                    underlying = underlying.getCause();
                }
                if (underlying == null) {
                    //There is no deeper level error, use e
                    underlying = e;
                }

                if (log.isDebugEnabled()) {
                    logHashes("Error calling SCA Soap Method [" + SCAMethodToCall + "] on SCA passing Class [" + classIn.getName() + "] Operation took " + Stopwatch.millisString() + ". Error info follows...");
                }

                // Put in other data
                if (underlying instanceof SCAError_Exception) {

                    /* Ok so we know its a SCA Exception and can look for the embedded field
                     * data to tell us more about the error and then throw it
                     */
                    SCAError_Exception s = (SCAError_Exception) underlying;
                    String errType = s.getFaultInfo().getErrorType();
                    if (errType != null && errType.equalsIgnoreCase(SCAConstants.BUSINESS_ERROR)) {
                        com.smilecoms.commons.sca.SCABusinessError be = new com.smilecoms.commons.sca.SCABusinessError();
                        be.setErrorCode(s.getFaultInfo().getErrorCode());
                        be.setErrorDesc(s.getFaultInfo().getErrorDesc());
                        be.setRequest(SCAMarshaller.marshalToString(objIn));
                        log.debug("SCA Delegate is throwing a SCAErr that has been created from a SCA business error");
                        throw be;
                    } else {
                        if (errType == null) {
                            log.debug("Note that errType is null. Ideally a proper error type should be populated. Assuming a system error");
                        }
                        com.smilecoms.commons.sca.SCASystemError se = new com.smilecoms.commons.sca.SCASystemError();
                        se.setErrorCode(s.getFaultInfo().getErrorCode());
                        se.setErrorDesc(s.getFaultInfo().getErrorDesc() + " Endpoint [" + endPoint + "]");
                        se.setRequest(SCAMarshaller.marshalToString(objIn));
                        log.warn("SCA Delegate is throwing a SCAErr that has been created from a SCA system error: " + s.getFaultInfo().getErrorDesc());
                        log.error("callSCA", se);
                        throw se;
                    }

                } else if (underlying instanceof javax.xml.ws.soap.SOAPFaultException || underlying instanceof org.apache.cxf.binding.soap.SoapFault) {
                    if (underlying.toString().contains("EntityAuthorisationException")) {
                        com.smilecoms.commons.sca.SCABusinessError be = new com.smilecoms.commons.sca.SCABusinessError();
                        be.setErrorCode(Errors.ERROR_CODE_SCA_ENTITY_NOT_AUTHORISED);
                        be.setErrorDesc("Access to entity is not authorised");
                        be.setRequest(SCAMarshaller.marshalToString(objIn));
                        log.debug("SCA Delegate is throwing a SCAErr that has been created from a SCA business error due to SCAEntityAuthorisationException. Stack trace follows to see the calling code");
                        log.warn("Error: ", e);
                        throw be;
                    } else {
                        com.smilecoms.commons.sca.SCASystemError se = new com.smilecoms.commons.sca.SCASystemError();
                        // Put in request data                    
                        se.setRequest(SCAMarshaller.marshalToString(objIn));
                        se.setErrorCode(Errors.ERROR_CODE_SCA_UNKNOWN);
                        String errMsg = underlying.toString();
                        try {
                            String detail = "";
                            try {
                                if (underlying instanceof javax.xml.ws.soap.SOAPFaultException) {
                                    javax.xml.ws.soap.SOAPFaultException soapFault = (javax.xml.ws.soap.SOAPFaultException) underlying;
                                    detail = soapFault.getFault().getDetail().getTextContent();
                                } else {
                                    org.apache.cxf.binding.soap.SoapFault soapFault = (org.apache.cxf.binding.soap.SoapFault) underlying;
                                    detail = soapFault.getReason();
                                }
                            } catch (Exception ex) {
                                log.warn("Error getting fault detail [{}]", ex.toString());
                            }
                            errMsg = "Underlying Error String [" + errMsg + "] DETAIL: [[" + detail + "]  Endpoint [" + endPoint
                                    + "] Method [" + SCAMethodToCall + "] Passing [" + objIn.getClass().getName() + "]]";
                        } catch (Exception ex) {
                            log.warn("Error compiling error string", ex);
                        }
                        se.setErrorDesc(errMsg);
                        // Send error to ops. If it was a SCAError_Exception then the platform would have reported it already
                        BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, SCADelegate.class.getName(), se.toString());
                        log.warn("SCA Delegate is throwing a SCAErr that has been created from a SOAP exception thrown by SCA");
                        log.error("callSCA", se);
                        log.error("callSCA Actual Error", underlying);
                        throw se;
                    }
                } else if (underlying.toString().contains("SOAP request contains suspicious XSS")) {
                    com.smilecoms.commons.sca.SCABusinessError be = new com.smilecoms.commons.sca.SCABusinessError();
                    be.setErrorCode(Errors.ERROR_CODE_SUSPICIOUS_XSS);
                    be.setErrorDesc("SOAP request contains suspicious XSS");
                    be.setRequest(SCAMarshaller.marshalToString(objIn));
                    log.debug("SCA Delegate is throwing a SCAErr that has been created from a SCA business error due to the SOAP request contains suspicious XSS");
                    throw be;
                } else {
                    log.warn("Stacktrace: ", underlying);
                    String errMsg = underlying.toString();
                    com.smilecoms.commons.sca.SCASystemError se = new com.smilecoms.commons.sca.SCASystemError();
                    // Put in request data       

                    se.setRequest(SCAMarshaller.marshalToString(objIn));
                    se.setErrorCode(Errors.ERROR_CODE_SCA_UNKNOWN);

                    try {
                        errMsg = "Underlying Error String [" + errMsg + "] Endpoint [" + endPoint + "] Method [" + SCAMethodToCall + "] Passing [" + objIn.getClass().getName() + "]";
                    } catch (Exception ex) {
                        log.warn("Error compiling error string", ex);
                    }
                    se.setErrorDesc(errMsg);
                    // Send error to ops. If it was a SCAError_Exception then the platform would have reported it already
                    BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, SCADelegate.class.getName(), se.toString());
                    log.warn("SCA Delegate is throwing a SCAErr that has been created from a SOAP exception thrown by SCA");
                    log.error("callSCA", se);
                    throw se;
                }
            }

            // If result is null then throw error
            if (objRet == null) {
                com.smilecoms.commons.sca.SCASystemError se = new com.smilecoms.commons.sca.SCASystemError();
                // Put in request data                            
                se.setRequest(SCAMarshaller.marshalToString(objIn));
                se.setErrorCode(Errors.ERROR_CODE_SCA_NULLOUT);
                se.setErrorDesc("Response object from SCA was null. Make sure the JAX-WS client is built against the latest SCA wsdl.");
                log.error("callSCA", se);
                throw se;
            }

            // If we got there then all is well, a good object was returned and no errors occured
            if (log.isDebugEnabled()) {
                logHashes("Called SCA SOAP Method : " + SCAMethodToCall + ". Operation took " + Stopwatch.millisString());
            }

            if (log.isDebugEnabled()) {
                log.debug("EXITING SCADelegate : callSCA");
            }
        } catch (com.smilecoms.commons.sca.SCAErr e) {
            throw e;
        }
        return objRet;
    }

    /**
     * Sets the timeouts prior to the call
     *
     * @param connectTimeoutMillis Number of milliseconds to timeout after
     * trying to do a socket connect
     * @param responseTimeoutMillis Number of milliseconds to timeout after
     * sending the request on the socket
     */
    private void setTimeouts(SCASoap sca, int connectTimeoutMillis, int responseTimeoutMillis) {
        ((BindingProvider) sca).getRequestContext().put("com.sun.xml.ws.connect.timeout", connectTimeoutMillis);
        ((BindingProvider) sca).getRequestContext().put("com.sun.xml.ws.request.timeout", responseTimeoutMillis);
        ((BindingProvider) sca).getRequestContext().put("javax.xml.ws.client.connectionTimeout", connectTimeoutMillis);
        ((BindingProvider) sca).getRequestContext().put("javax.xml.ws.client.receiveTimeout", responseTimeoutMillis);
    }

    private void addSCACallInfo(String SCAMethodToCall, float millis) {
        //Prevent memory leak
        String currentCallInfo = scaCallInfo.get();
        Float time = scaCallTime.get();
        if (currentCallInfo != null && currentCallInfo.length() > 5000) {
            currentCallInfo = null;
            time = null;
        }

        if (currentCallInfo != null && time != null) {
            currentCallInfo += " " + SCAMethodToCall + " " + String.format("%.3f", millis) + "ms";
            time += millis;
        } else {
            currentCallInfo = SCAMethodToCall + " " + String.format("%.3f", millis) + "ms";
            time = millis;
        }
        scaCallInfo.set(currentCallInfo);
        scaCallTime.set(time);
    }

    public static String getSCACallInfo() {
        String val = scaCallInfo.get();
        Float time = scaCallTime.get();
        if (val == null || time == null) {
            return "";
        }
        return val + " TOTAL: " + String.format("%.3f", time) + "ms";
    }

    public static void clearSCACallInfo() {
        scaCallInfo.remove();
        scaCallTime.remove();
    }

    public static final String ADMIN_TENANT = "ad";
}
