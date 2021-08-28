package com.smilecoms.commons.platform;

import com.smilecoms.commons.sca.SCAConstants;
import com.smilecoms.commons.sca.SCAErr;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.util.ExceptionManager;
import com.smilecoms.commons.util.FriendlyException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.ejb.SessionContext;
import javax.naming.InitialContext;
import javax.transaction.Status;
import javax.transaction.UserTransaction;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import org.slf4j.*;

/**
 * Holds all utility stuff needed by platforms
 *
 * @author PCB
 */
public abstract class SmileWebService {
    //Logging 

    protected Logger log = null;
    //Strings
    public static final String SYSTEM_ERROR = "system";
    public static final String BUSINESS_ERROR = "business";
    private Object platformRequest = null;
    private String thisClassName = null;
    private String thisPlatform = null;
    private final Map<String, Long> startTimeMap = new HashMap<>();
    private String methodName;

    static {
        Platform.init();
    }

    public SmileWebService() {
        thisClassName = this.getClass().getName();
        thisPlatform = this.getClass().getPackage().getName().split("\\.")[2].toUpperCase();
        log = LoggerFactory.getLogger(thisClassName);
    }

    /**
     * Log a line at finer level showing entry into the class. A timer is
     * started to log the call duration on the logEnd call. e.g. ENTERING
     * com.smilecoms.am.AgentManager : modifyAgent Blah Blah
     *
     * @param methodName The name of the method
     * @param msg The message to log e.g. Blah Blah
     */
    public void logStart(String msg) {
        if (log.isDebugEnabled()) {
            if (msg == null) {
                log.debug("ENTERING {} : {}", thisClassName, methodName);
            } else {
                log.debug("ENTERING {} : {}:-{}", new Object[]{thisClassName, methodName, msg});
            }
            startTimer();
        }
    }

    public void logStart(int msg) {
        logStart(String.valueOf(msg));
    }

    private void startTimer() {
        startTimeMap.put(methodName, System.nanoTime());
    }

    private String getTimer() {
        String ret;
        try {
            float res = System.nanoTime() - startTimeMap.get(methodName);
            ret = String.format("%.3f", res / 1000000f);
        } catch (Exception e) {
            log.warn("Error getting a timer for logend : {0}", e.toString());
            return "9999999";
        }
        return ret;
    }

    /**
     * Log a line at finer level showing entry into the class. A timer is
     * started to log the call duration on the logEnd call. e.g. ENTERING
     * com.smilecoms.am.AgentManager : modifyAgent
     *
     * @param methodName The name of the method
     */
    public void logStart() {
        this.logStart(null);
    }

    /**
     * Log a line at finer level showing exit out of the class. The call
     * duration is logged based on the logStart time. e.g. EXITING
     * com.smilecoms.am.AgentManager : getAgent (2ms) Blah Blah
     *
     * @param msg Message to log with the line
     */
    public void logEnd(String msg) {
        if (log.isDebugEnabled()) {
            log.debug("EXITING {} : {} ({}ms) - {}", new Object[]{thisClassName, methodName, getTimer(), msg});
        }
    }

    public void logEnd() {
        if (log.isDebugEnabled()) {
            log.debug("EXITING {} : {} ({}ms)", new Object[]{thisClassName, methodName, getTimer()});
        }
    }

    public void createEvent(String identifier) {
        PlatformEventManager.createEvent(thisPlatform, methodName, identifier, platformRequest);
    }

    public void createEvent(long identifier) {
        PlatformEventManager.createEvent(thisPlatform, methodName, String.valueOf(identifier), platformRequest);
    }

    /**
     * Get the context for the current call out the incoming object or as that
     * object if its already a context (for backwards compatibility)
     *
     * @return Object
     */
    @SuppressWarnings("unchecked")
    protected Object getContext() {
        if (platformRequest == null) {
            return null;
        }
        Class objectClass = platformRequest.getClass();
        try {
            Method m = objectClass.getMethod("getPlatformContext");
            return m.invoke(platformRequest);
        } catch (Exception e) {
            // This object does not have that method, so it must be the context object itself
            return platformRequest;
        }
    }

    /**
     * Called by classes that extend Platform in order to set the context data
     * for the call onto the parent. The call to setContextData on the subclass
     * allows the subclass to determine how to populate the contextMap
     *
     * @param object
     */
    protected void setContext(Object object, WebServiceContext wsctx) {
        // Order is critical so that context is not null
        boolean foundTenant = false;
        try {
            Class objectClass = object.getClass();
            Method getContextMethod = null;
            try {
                getContextMethod = objectClass.getMethod("getPlatformContext");
            } catch (Exception e) {
                log.debug("This platform does not have platform context. Ignoring tenant");
                foundTenant = true;
            }
            if (getContextMethod != null) {
                Object platContext = getContextMethod.invoke(object);
                if (platContext != null) {
                    Method getTenantMethod = platContext.getClass().getMethod("getTenant");
                    String tenant = (String) getTenantMethod.invoke(platContext);
                    if (tenant == null || tenant.isEmpty()) {
                        Method setTenantMethod = platContext.getClass().getMethod("setTenant", String.class);
                        setTenantMethod.invoke(platContext, "sm");
                    } else {
                        foundTenant = true;
                        if (log.isDebugEnabled()) {
                            log.debug("Tenant is [{}]", (String) getTenantMethod.invoke(platContext));
                        }
                    }
                }
            }

        } catch (Throwable e) {
            log.warn("Error checking for tenant", e);
        }
        this.platformRequest = object;
        methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
        if (!foundTenant) {
            log.info("Platform request has a null or empty tenant:  [{}]", methodName);
        }
        if (wsctx != null) {
            if (wsctx.getMessageContext().get(SOAPMessageContext.WSDL_OPERATION) != null) {
                return;
            }
            wsctx.getMessageContext().put(SOAPMessageContext.WSDL_OPERATION, methodName);
        }

    }

    /*
     *
     * ###################################
     * EXCEPTION MANAGEMENT
     * ###################################
     *
     *
     */
    /**
     * Standard way for platforms to handle errors. This method looks up the
     * errors' config in error_info and deals with rolling back the JPA
     * transaction if configured to do so, sending errors to Operations and
     * logging etc
     *
     * @param <E>
     * @param errorClass
     * @param e
     * @return error or type requested
     */
    protected <E> E processError(Class<E> errorClass, Exception e) {
        return processError(errorClass, e, null);
    }

    /**
     * Creates an error of the request type and then does all the same stuff as
     * processError
     *
     * @param <E>
     * @param errorClass
     * @param exception
     * @return error of type requested
     */
    protected <E> E createError(Class<E> errorClass, String exception) {
        return processError(errorClass, new Exception(exception), null);
    }

    /**
     * Creates an error of the request type and then does all the same stuff as
     * processError
     *
     * @param <E>
     * @param errorClass
     * @param exception
     * @param utx
     * @return error of type requested
     */
    protected <E> E createError(Class<E> errorClass, String exception, UserTransaction utx) {
        return processError(errorClass, new Exception(exception), utx);
    }

    private Object populateErrorClass(Class errorClass, String code, String type, String desc, Throwable cause) throws Exception {
        Constructor[] ctors = errorClass.getDeclaredConstructors();
        /* Iterate through constructors */
        Constructor ctor = null;
        Class faultClass = null;
        for (Constructor ctor1 : ctors) {
            ctor = ctor1;
            Class[] parameterTypes = ctor.getParameterTypes();
            int parameterCount = parameterTypes.length;
            if (parameterCount == 3) {
                faultClass = parameterTypes[1];
                break;
            }
        }

        Object faultObject = faultClass.newInstance();

        @SuppressWarnings("unchecked")
        Method mSetErrorDesc = faultClass.getMethod("setErrorDesc", new Class[]{java.lang.String.class});
        @SuppressWarnings("unchecked")
        Method mSetErrorType = faultClass.getMethod("setErrorType", new Class[]{java.lang.String.class});
        @SuppressWarnings("unchecked")
        Method mSetErrorCode = faultClass.getMethod("setErrorCode", new Class[]{java.lang.String.class});

        mSetErrorCode.invoke(faultObject, new Object[]{code});
        mSetErrorType.invoke(faultObject, new Object[]{type});
        mSetErrorDesc.invoke(faultObject, new Object[]{desc});

        return ctor.newInstance(new Object[]{desc, faultObject, cause});
    }

    /**
     * Deal with the error by logging it, sending a trap to ops management,
     * rolling back transactions etc as required.
     *
     * @param <E>
     * @param errorClass
     * @param e
     * @param utx
     * @return A nice friendly error of the class requested with detailed
     * descriptions of the error.
     */
    @SuppressWarnings("unchecked")
    private <E> E processError(Class<E> errorClass, Exception e, UserTransaction utx) {

        if (e.getClass().getName().equals(errorClass.getName())) {
            // The exception is already of type errorClass so just return the existing exception as is - it has been processed already
            return (E) e;
        }

        Object errorToReturnWith = null;

        if (e instanceof com.smilecoms.commons.sca.SCABusinessError) {
            try {
                SCAErr se = (SCAErr) e;
                errorToReturnWith = populateErrorClass(errorClass, se.getErrorCode(), SCAConstants.BUSINESS_ERROR, se.getErrorDesc(), se.getCause());
                // We must roll back for SCA Errors when SCA is called from a PLatform
                dealWithActiveTransaction(null, utx);
            } catch (Exception ex) {
                log.error("Error processing exception: " + ex.toString());
                log.warn("Error: ", ex);
            }
        } else if (e instanceof com.smilecoms.commons.sca.SCASystemError) {
            try {
                SCAErr se = (SCAErr) e;
                errorToReturnWith = populateErrorClass(errorClass, se.getErrorCode(), SCAConstants.SYSTEM_ERROR, se.getErrorDesc(), se.getCause());
                // We must roll back for SCA Errors when SCA is called from a PLatform
                dealWithActiveTransaction(null, utx);
            } catch (Exception ex) {
                log.error("Error processing exception: " + ex.toString());
                log.warn("Error: ", ex);
            }
        } else {
            FriendlyException fe = null;
            try {
                ExceptionManager exm = new ExceptionManager(thisClassName);
                fe = exm.getFriendlyException(e, platformRequest);
                exm.reportError(fe);
                errorToReturnWith = populateErrorClass(errorClass, fe.getErrorCode(), fe.getErrorType(), fe.getErrorDesc(), fe.getCause());
            } catch (Exception ex) {
                log.error("Error processing exception: " + ex.toString());
                log.warn("Error: ", ex);
            } finally {
                dealWithActiveTransaction(fe, utx);
            }
        }

        return (E) errorToReturnWith;
    }

    /**
     * Use the errors configuration and roll back transactions if configured to
     * do so
     *
     * @param fe
     * @param utx A user transaction for when the platform uses its own
     * transaction management instead of the containers. Pass in null when using
     * container transactions
     */
    private void dealWithActiveTransaction(FriendlyException fe, UserTransaction utx) {
        try {
            //Rollback transaction if configured to do so
            if (fe == null || fe.getMustRollback()) {
                try {
                    log.info("Error has been configured to roll back the active JTA transaction. About to roll back / setRollbackOnly depending on Tx type...");
                    // If a user tx was passed in then roll that back, otherwise "set roll back only" on the container tx
                    if (utx != null) {
                        log.debug("Transaction was a user transaction so rolling it back");
                        if (utx.getStatus() == Status.STATUS_MARKED_ROLLBACK || utx.getStatus() == Status.STATUS_ACTIVE) {
                            utx.rollback();
                            log.info("User transaction has been rolled back");
                        }
                    } else {
                        log.debug("Looking in context to see if there is a container transaction");
                        InitialContext ic = new InitialContext();
                        try {
                            SessionContext sessionContext = (SessionContext) ic.lookup("java:comp/EJBContext");
                            sessionContext.setRollbackOnly();
                            log.info("Container transaction was found and has been rolled back");
                        } catch (javax.naming.NameNotFoundException e) {
                            log.debug("Could not find a container transaction so no need to roll it back");
                        }
                    }
                } catch (Exception ex1) {
                    log.warn("Error rolling back JTA transaction : " + ex1.toString());
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Error has been configured to NOT roll back the active JTA transaction.");
                }
                // Ensure a usertransaction is committed
                if (utx != null) {
                    log.debug("Transaction is a user transaction. Committing user transaction as it should not be rolled back nor left uncommitted");
                    try {
                        if (utx.getStatus() == Status.STATUS_ACTIVE) {
                            utx.commit();
                        }
                    } catch (Exception ex2) {
                        log.warn("Error committing user transaction : " + ex2.toString());
                    }
                }
            }
        } catch (Exception ex3) {
            log.warn("Error rolling back transaction: " + ex3.toString());
        }
    }    
}
