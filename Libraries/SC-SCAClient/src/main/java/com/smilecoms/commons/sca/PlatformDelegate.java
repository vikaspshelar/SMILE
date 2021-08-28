package com.smilecoms.commons.sca;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.Stopwatch;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
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
public class PlatformDelegate {
    //Logging     

    private static final Logger log = LoggerFactory.getLogger(PlatformDelegate.class);
    private static final List<Platform> platformList = new ArrayList<>();
    private static final String DASH = "-";
    private String loggingTXID = DASH;
    private static final String XERCES_CALENDAR = "com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl";

    private Platform getPlatform(String platformMethodName, Class classIn) throws NoSuchMethodException {
        for (Platform p : platformList) {
            if (p.supports(platformMethodName, classIn)) {
                return p;
            }
        }
        throw new NoSuchMethodException("No platform exists for that method");
    }

    /**
     * Constructor. Initialises the static reference to SCASoap if it hasn't
     * already done so. Set the local reference to the SmileActionBean that
     * created the delegate for callback purposes
     *
     */
    public PlatformDelegate() {
        if (platformList.isEmpty()) {
            try {
                synchronized (platformList) {
                    if (platformList.isEmpty()) {
                        platformList.add(new Platform("ET"));
                        platformList.add(new Platform("AM"));
                        platformList.add(new Platform("MM"));
                        platformList.add(new Platform("BM"));
                        platformList.add(new Platform("IM"));
                        platformList.add(new Platform("HWF"));
                        platformList.add(new Platform("PVS"));
                    }
                }
            } catch (Exception ex) {
                platformList.clear();
                log.error("Could not initialise a platform client : " + ex.toString());
                log.error("SCADelegate", ex);
                throw new RuntimeException(ex);
            }
        }
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

    public Object callPlatform(String platformMethodToCall, Object objIn, SCAProperties props) throws com.smilecoms.commons.sca.SCAErr {
        return callPlatform(platformMethodToCall, objIn, null, props);
    }

    public Object callPlatform(String platformMethodToCall, Object objIn, String endPoint, SCAProperties props) throws com.smilecoms.commons.sca.SCAErr {
        if (log.isDebugEnabled()) {
            log.debug("ENTERING PlatformDelegate : callPlatform. Method being called on Platform : " + platformMethodToCall);
        }

        // Variables
        Object objRet = null;
        Platform platform = null;

        try {

            // If object passed in is null then throw error
            if (objIn == null) {
                com.smilecoms.commons.sca.SCASystemError se = new com.smilecoms.commons.sca.SCASystemError();
                // Put in request data                            
                se.setRequest(SCAConstants.NULL);
                se.setErrorCode(Errors.ERROR_CODE_SCA_NULLIN);
                se.setErrorDesc("Request object going to Platform was null. Make sure the code calling Platform has been coded/populated correctly." + " Method being called on Platform is : " + platformMethodToCall);
                log.error("callPlatform", se);
                throw se;
            }

            //Get method to call via reflection
            Class classIn = objIn.getClass();

            try {
                //Hack to change Integer into int
                if (classIn.getName().equals(SCAConstants.INTEGER)) {
                    classIn = int.class;
                }
                //Hack to change com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl
                //into javax.xml.datatype.XMLGregorianCalendar
                if (classIn.getName().equals(XERCES_CALENDAR)) {
                    classIn = javax.xml.datatype.XMLGregorianCalendar.class;
                }

                platform = getPlatform(platformMethodToCall, classIn);

            } catch (Exception e) {
                // Cannot find method
                com.smilecoms.commons.sca.SCASystemError se = new com.smilecoms.commons.sca.SCASystemError(e);
                se.setErrorCode(Errors.ERROR_CODE_SCA_INVALID_METHOD);
                se.setErrorDesc("Requested method does not exist on any platform : " + platformMethodToCall + " passing " + objIn.getClass().getName() + " : " + e.toString());
                log.error("callPlatform", se);
                throw se;
            }

            //Call Platform
            String millisString = null;
            try {

                // Log request
                if (log.isDebugEnabled()) {
                    SCAMarshaller.logObject(objIn, "XML being sent to Platform:", platform.getNamespace());
                }
                Stopwatch.start();
                try {
                    objRet = platform.invoke(platformMethodToCall, classIn, objIn, endPoint, props);
                } finally {
                    Stopwatch.stop();
                    millisString = Stopwatch.millisString();
                }

                // Log result
                if (log.isDebugEnabled()) {
                    SCAMarshaller.logObject(objRet, "XML received back from Platform:", platform.getNamespace());
                }

            } catch (com.smilecoms.commons.sca.SCAErr scaError) {
                throw scaError;
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
                    logHashes("Error calling Platform Soap Method [" + platformMethodToCall + "] on Platform passing Class [" + classIn.getName() + "] Operation took " + millisString + ". Error info follows...");
                }

                // Put in other data
                try {
                    if (underlying instanceof javax.xml.ws.soap.SOAPFaultException) {
                        javax.xml.ws.soap.SOAPFaultException soapFault = (javax.xml.ws.soap.SOAPFaultException) underlying;
                        com.smilecoms.commons.sca.SCASystemError se = new com.smilecoms.commons.sca.SCASystemError();
                        // Put in request data
                        se.setRequest(SCAMarshaller.marshalToString(objIn));
                        se.setErrorCode(Errors.ERROR_CODE_SCA_UNKNOWN);
                        String errMsg = underlying.toString();
                        try {
                            errMsg = errMsg + " DETAIL: [[" + soapFault.getFault().getDetail().getTextContent() + "]] Endpoint [" + platform.getLastEndpoint() +"]";
                        } catch (Exception ex) {
                        }
                        se.setErrorDesc(errMsg);
                        // Send error to ops. If it was a SCAError_Exception then the platform would have reported it already
                        BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, PlatformDelegate.class.getName(), se.toString());
                        log.warn("Platform Delegate is throwing a SCAErr that has been created from a SOAP exception thrown by a Platform");
                        log.error("callPlatform", se);
                        throw se;
                    } else if (underlying instanceof org.apache.cxf.binding.soap.SoapFault) {
                        org.apache.cxf.binding.soap.SoapFault soapFault = (org.apache.cxf.binding.soap.SoapFault) underlying;
                        com.smilecoms.commons.sca.SCASystemError se = new com.smilecoms.commons.sca.SCASystemError();
                        // Put in request data
                        se.setRequest(SCAMarshaller.marshalToString(objIn));
                        se.setErrorCode(Errors.ERROR_CODE_SCA_UNKNOWN);
                        String errMsg = underlying.toString();
                        try {
                            errMsg = errMsg + " DETAIL: [[" + soapFault.getReason() + "]] Endpoint [" + platform.getLastEndpoint() +"]";
                        } catch (Exception ex) {
                        }
                        se.setErrorDesc(errMsg);
                        // Send error to ops. If it was a SCAError_Exception then the platform would have reported it already
                        BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, PlatformDelegate.class.getName(), se.toString());
                        log.warn("Platform Delegate is throwing a SCAErr that has been created from a SOAP exception thrown by a Platform");
                        log.error("callPlatform", se);
                        throw se;
                    } else {
                        Method faultinfoMethod = null;
                        try {
                            faultinfoMethod = underlying.getClass().getMethod("getFaultInfo");
                        } catch (java.lang.NoSuchMethodException nsme) {
                            // must be some low level error and not a platform error
                            com.smilecoms.commons.sca.SCASystemError se = new com.smilecoms.commons.sca.SCASystemError();
                            // Put in request data
                            se.setRequest(SCAMarshaller.marshalToString(objIn));
                            se.setErrorCode(Errors.ERROR_CODE_SCA_UNKNOWN);
                            String errMsg = underlying.toString() + " Endpoint [" + platform.getLastEndpoint() +"]";                      
                            se.setErrorDesc(errMsg);
                            // Send error to ops. If it was a SCAError_Exception then the platform would have reported it already
                            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, PlatformDelegate.class.getName(), se.toString());
                            log.warn("Platform Delegate is throwing a SCAErr that has been created from a SOAP exception thrown by a Platform. Stacktrace of the error:");
                            log.warn("Stacktrace: ", underlying);
                            log.error("callPlatform", se);
                            throw se;
                        }
                        Object faultInfo = faultinfoMethod.invoke(underlying);
                        Method descMethod = faultInfo.getClass().getMethod("getErrorDesc");
                        Method codeMethod = faultInfo.getClass().getMethod("getErrorCode");
                        Method typeMethod = faultInfo.getClass().getMethod("getErrorType");
                        String errType = (String) typeMethod.invoke(faultInfo);
                        String errCode = (String) codeMethod.invoke(faultInfo);
                        String errDesc = (String) descMethod.invoke(faultInfo) + " Endpoint [" + platform.getLastEndpoint() +"]";
                        if (errType.equalsIgnoreCase(SCAConstants.BUSINESS_ERROR)) {
                            com.smilecoms.commons.sca.SCABusinessError be = new com.smilecoms.commons.sca.SCABusinessError();
                            be.setErrorCode(errCode);
                            be.setErrorDesc(errDesc);
                            be.setRequest(SCAMarshaller.marshalToString(objIn, platform.getNamespace()));
                            log.debug("SCA Delegate is throwing a SCAErr that has been created from a Platform business error");
                            throw be;
                        } else {
                            com.smilecoms.commons.sca.SCASystemError se = new com.smilecoms.commons.sca.SCASystemError();
                            se.setErrorCode(errCode);
                            se.setErrorDesc(errDesc);
                            se.setRequest(SCAMarshaller.marshalToString(objIn, platform.getNamespace()));
                            log.warn("SCA Delegate is throwing a SCAErr that has been created from a Platform system error: " + errDesc);
                            log.error("callPlatform", se);
                            throw se;
                        }
                    }
                } catch (SCAErr se) {
                    throw se;
                } catch (java.lang.Exception ex) {
                    log.error("Got an error trying to deal with an error!");
                    log.warn("Error: ", ex);
                    com.smilecoms.commons.sca.SCASystemError se = new com.smilecoms.commons.sca.SCASystemError();
                    // Put in request data
                    se.setRequest(SCAMarshaller.marshalToString(objIn, platform.getNamespace()));
                    se.setErrorCode(Errors.ERROR_CODE_SCA_UNKNOWN);
                    se.setErrorDesc(ex.toString());
                    // Send error to ops. If it was a SCAError_Exception then the platform would have reported it already
                    BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, PlatformDelegate.class.getName(), se.toString());
                    log.warn("Platform Delegate is throwing a SCAErr that has been created from a SOAP exception thrown by a Platform");
                    log.error("callPlatform", se);
                    throw se;
                }
            }

            // If result is null then throw error
            if (objRet == null) {
                com.smilecoms.commons.sca.SCASystemError se = new com.smilecoms.commons.sca.SCASystemError();
                // Put in request data                            
                se.setRequest(SCAMarshaller.marshalToString(objIn));
                se.setErrorCode(Errors.ERROR_CODE_SCA_NULLOUT);
                se.setErrorDesc("Response object from Platform was null. Make sure the JAX-WS client is built against the latest Platform wsdl.");
                log.error("callPlatform", se);
                throw se;
            }

            // If we got there then all is well, a good object was returned and no errors occured
            if (log.isDebugEnabled()) {
                logHashes("Called Platform SOAP Method : " + platformMethodToCall + ". Operation took " + millisString);
            }

            if (log.isDebugEnabled()) {
                log.debug("EXITING PlatformDelegate : callPlatform");
            }
        } catch (com.smilecoms.commons.sca.SCAErr e) {
            throw e;
        }
        return objRet;
    }

    /**
     * Returns the logging TXID
     *
     * @return Logging TXID
     */
    public String getLoggingTXID() {
        return loggingTXID;
    }

    /**
     * Sets the logging TXID
     *
     * @param loggingTXID
     */
    public void setLoggingTXID(String loggingTXID) {
        this.loggingTXID = loggingTXID;
    }
}
