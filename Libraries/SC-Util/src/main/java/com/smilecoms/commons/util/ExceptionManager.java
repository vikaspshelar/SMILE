package com.smilecoms.commons.util;

import com.smilecoms.commons.base.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import org.slf4j.*;

/**
 * This class is used to deal with Exceptions in a standardised way across all
 * the java code.
 *
 * @author PCB
 */
public class ExceptionManager {
    // It may look weird exposing public stuff from another classes public stuff, but doing so allows us to hide
    // away the backend ops platform from users of ExceptionManager so that we can change ops platforms if need be without
    // impacting any other code other than this class.

    public static final String SYSTEM_ERROR = "system";
    public static final String BUSINESS_ERROR = "business";
    public static final String ERROR_CODE_UNKNOWN = "UNKN-0000";
    public static final String DEBUG = BaseUtils.DEBUG;
    public static final String INFO = BaseUtils.INFO;
    public static final String MINOR = BaseUtils.MINOR;
    public static final String MAJOR = BaseUtils.MAJOR;
    private static final String SMILE_PACKAGE = "com.smilecoms.";
    private static final String SMILE_PACKAGE_COMMONS = "com.smilecoms.commons";
    private static final String DEFAULT_ERROR_TYPE = "system";
    private static final String DEFAULT_ERROR_SEVERITY = MAJOR;
    private static final boolean ROLLBACK_DEFAULT = true;
    private static final String DASH = "--";
    private static final String DEFAULT_ERROR_DESC = "Unknown Err";
    private static final String EXCEPTION = "java.lang.Exception";
    private static final String RUNTIME_EXCEPTION = "java.lang.RuntimeException";
    private static Logger log;
    private static final String DEFAULT_RESOLUTION = "TODO : Insert resolution in ERROR_INFO table";
    private static final Lock propsLock = new ReentrantLock();

    /**
     * Set up the instance data so that it logs with the same log as the caller
     *
     * @param callersClassName
     */
    public ExceptionManager(String callersClassName) {
        log = LoggerFactory.getLogger(callersClassName);
    }

    /**
     * Set up the instance data so that it logs with the same log as the caller
     *
     * @param callersClass
     */
    public ExceptionManager(Object callersClass) {
        log = LoggerFactory.getLogger(callersClass.getClass());
    }

    /**
     * Set up the instance data so that it logs with the same log as the caller
     *
     * @param callersClass
     */
    public ExceptionManager(Class callersClass) {
        log = LoggerFactory.getLogger(callersClass);
    }

    /**
     * Set up the instance data so that it logs with the same log as the caller
     *
     * @param callersClass
     */
    public ExceptionManager(Logger logg) {
        log = logg;
    }

    /**
     * Return a nice standardised exception with info as configured in the
     * error_info table. The message etc is nice and detailed and includes the
     * underlying root cause along with the line number that caused it.
     *
     * @param e The initial error
     * @return FriendlyException
     */
    public FriendlyException getFriendlyException(Throwable e) {
        return getFriendlyException(e, null);
    }

    public FriendlyException getFriendlyException(Throwable e, Object inputMsg) {
        FriendlyException fe = new FriendlyException();
        fe.topLevelException = e;
        populateOriginalCause(fe);
        populateTopLevelData(fe);
        getAndPopulateErrorConfiguration(fe);

        if (fe.getErrorType().equals(DEFAULT_ERROR_TYPE)) {
            // Marshaling an object is expensive so only do so for system errors
            if (inputMsg instanceof String) {
                fe.inputMsg = (String) inputMsg;
            } else {
                fe.inputMsg = Utils.marshallSoapObjectToString(inputMsg);
                if (fe.inputMsg != null && fe.inputMsg.length() > 10000) {
                    fe.inputMsg = fe.inputMsg.substring(0, 10000) + "...TOO LARGE TO DISPLAY FULLY...";
                }
            }
        }

        //if there is an action then execute it here
        if (fe.topLevelConfiguredAction != null && !fe.topLevelConfiguredAction.isEmpty()) {
            log.debug("We have the following action to perform: [{}]", fe.topLevelConfiguredAction);

            try {
                Boolean res = (Boolean) Javassist.runCode(new Class[]{this.getClass(), java.util.Calendar.class}, fe.topLevelConfiguredAction, inputMsg, log);
                log.debug("Called javassist code with result [{}]", res);
            } catch (Throwable ex) {
                log.warn("Error in getFriendlyException while running action code:", ex);
            }
        }

        return fe;
    }

    /**
     * Report an error to the ops management platform. Calls
     * getFriendlyException and then reportError(FriendlyException fe) under the
     * covers
     *
     * @param e The exception
     */
    public void reportError(Throwable e) {
        reportError(this.getFriendlyException(e));
    }

    public void reportError(Throwable e, String requestInfo) {
        reportError(this.getFriendlyException(e, requestInfo));
    }

    /**
     * Report a friendly exception to ops
     *
     * @param fe
     */
    public void reportError(FriendlyException fe) {
        // Central point for logging errors wherever they should be reported
        logError(fe);
        sendTrapToOpsManagement(fe);

    }

    /**
     * Logs an error at its configured log level in the server.log as per
     * error_info config for the error. Also prints the stack trace if
     * configured to do so as per env.exceptions.printstacktrace
     *
     * @param fe
     */
    private void logError(FriendlyException fe) {
        //Log info about the error        
        boolean logged = false;

        if (fe.topLevelConfiguredSeverity != null) {
            if (!logged && fe.topLevelConfiguredSeverity.equalsIgnoreCase(DEBUG)) {
                log.debug(fe.getLogLine());
                logged = true;
            }
            if (!logged && fe.topLevelConfiguredSeverity.equalsIgnoreCase(INFO)) {
                log.info(fe.getLogLine());
                logged = true;
            }
            if (!logged && fe.topLevelConfiguredSeverity.equalsIgnoreCase(MINOR)) {
                log.warn(fe.getLogLine());
                logged = true;
            }
            if (!logged && fe.topLevelConfiguredSeverity.equalsIgnoreCase(MAJOR)) {
                log.error(fe.getLogLine());
            }
        }

        try {
            if (fe.topLevelConfiguredSeverity != null && !fe.topLevelConfiguredSeverity.equalsIgnoreCase(DEBUG)) {
                if (propsLock.tryLock()) {
                    try {
                        if (BaseUtils.getBooleanPropertyFailFast("env.exceptions.printstacktrace", false)) {
                            log.warn("Error: ", fe.topLevelException);
                        }
                    } finally {
                        propsLock.unlock();
                    }
                } else {
                    log.warn("Another thread is doing a stack trace. Wont do one for this error");
                }
            }
        } catch (Exception e) {
        }

        if (fe.getErrorDesc().contains("In-use connections equal max-pool-size")) {
            if (lock.tryLock()) {
                try {
                    long now = System.currentTimeMillis();
                    if (now - lastThreadDump > 120000) {
                        lastThreadDump = now;
                        log.warn("We have a DB pool out of connections and last thread dump was done more than 2 minutes ago. Going to do one now");
                        log.warn(Utils.getFullThreadDump());
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    private static final Lock lock = new ReentrantLock();
    private static long lastThreadDump = 0;

    /**
     * Extract and populate the root cause into the friendly exception
     *
     * @param fe
     */
    private void populateOriginalCause(FriendlyException fe) {

        // Find the deepest level cause of the problem - or null if its only single level
        fe.rootCauseException = fe.topLevelException.getCause();
        if (fe.rootCauseException == null) {
            return;
        }
        while (fe.rootCauseException.getCause() != null) {
            fe.rootCauseException = fe.rootCauseException.getCause();
        }

        if (fe.rootCauseException == null) {
            return;
        }

        fe.rootCauseTechnicalDescription = fe.rootCauseException.getMessage();

        // Use the detail embedded message if its a javax.xml.ws.soap.SOAPFaultException        
        if (fe.rootCauseException instanceof javax.xml.ws.soap.SOAPFaultException) {
            try {
                javax.xml.ws.soap.SOAPFaultException soapFault = (javax.xml.ws.soap.SOAPFaultException) fe.rootCauseException;
                fe.rootCauseTechnicalDescription = soapFault.getFault().getDetail().getTextContent();
            } catch (Exception e) {
            }
        }
        fe.rootCauseExceptionClassName = fe.rootCauseException.getClass().getName();

        StackTraceElement[] stes = fe.rootCauseException.getStackTrace();

        //Populate original cause local variables
        if (stes.length > 0) {
            StackTraceElement ste = stes[0];
            fe.rootCauseLineNumber = ste.getLineNumber();
            fe.rootCauseMethodName = ste.getMethodName();
            fe.rootCauseClassName = ste.getClassName();
        } else {
            fe.rootCauseLineNumber = 0;
            fe.rootCauseMethodName = "Unknown: No stack trace available";
            fe.rootCauseClassName = "Unknown: No stack trace available";
        }
    }

    /**
     * Populate the top level data into the friendly exception
     *
     * @param fe
     */
    private void populateTopLevelData(FriendlyException fe) {
        // Traverse stack to find last call from com.smilecoms class thats not in commons.
        StackTraceElement[] stElements = fe.topLevelException.getStackTrace();
        StackTraceElement ste = null;
        boolean usedRootStack = true;
        for (int i = 0; i < stElements.length; i++) {
            ste = stElements[i];
            if (log.isDebugEnabled()) {
                log.debug("In populateTopLevelData checking stack trace element [{}][{}][{}]", new Object[]{ste.getClassName(), ste.getMethodName(), ste.getLineNumber()});
            }
            String cls = ste.getClassName();
            if (!cls.startsWith(SMILE_PACKAGE_COMMONS) && cls.startsWith(SMILE_PACKAGE)) {
                usedRootStack = false;
                break;
            }
        }

        //Populate top level local variables
        if (ste != null) {
            if (usedRootStack) {
                log.debug("The root element in the stack was reached [{}][{}][{}] reporting this as the top level cause wont add value", new Object[]{ste.getClassName(), ste.getMethodName(), ste.getLineNumber()});
                stElements = Thread.currentThread().getStackTrace();
                for (int i = 0; i < stElements.length; i++) {
                    ste = stElements[i];
                    if (log.isDebugEnabled()) {
                        log.debug("In populateTopLevelData checking this threads stack trace element [{}][{}][{}]", new Object[]{ste.getClassName(), ste.getMethodName(), ste.getLineNumber()});
                    }
                    String cls = ste.getClassName();
                    if (!cls.startsWith(SMILE_PACKAGE_COMMONS) && cls.startsWith(SMILE_PACKAGE)) {
                        break;
                    }
                }
            }
            fe.topLevelClassName = ste.getClassName();
            fe.topLevelMethodName = ste.getMethodName();
            fe.topLevelLineNumber = ste.getLineNumber();

        } else {
            fe.topLevelClassName = "<No Stack Trace>";
            fe.topLevelMethodName = "<No Stack Trace>";
            fe.topLevelLineNumber = 0;
        }
        fe.topLevelErrorMessage = fe.topLevelException.getMessage();
        fe.topLevelExceptionClassName = fe.topLevelException.getClass().getName();

        try {
            if (fe.topLevelException instanceof ConstraintViolationException) {
                try {
                    ConstraintViolationException cve = (ConstraintViolationException) fe.topLevelException;
                    Set<ConstraintViolation<?>> violations = cve.getConstraintViolations();
                    Iterator<ConstraintViolation<?>> it = violations.iterator();
                    while (it.hasNext()) {
                        ConstraintViolation violation = it.next();
                        fe.topLevelErrorMessage += "[" + violation.getPropertyPath().toString() + " " + violation.getMessage() + "] ";
                    }
                } catch (Exception e) {
                    log.warn("Error getting constraint violation details: ", e);
                }
            }
        } catch (Throwable e) {
            log.debug(e.toString());
        }

    }

    private static final Map<String, String[]> errorConfig = new ConcurrentHashMap<>();
    private static long errorsLastRequested = 0;

    private String[] getErrorConfig(String className, String methodName, String errClass) {
        long now = System.currentTimeMillis();
        if (errorConfig.isEmpty() || errorsLastRequested < now - 60000) {
            errorsLastRequested = now;
            List<String[]> errorConfigList = BaseUtils.getPropertyFromSQL("global.error.info");
            for (String[] config : errorConfigList) {
                String configsErrorKey = config[0] + config[1] + config[2]; // we expect the columns to be in the correct order
                errorConfig.put(configsErrorKey, config);
            }
        }
        String key = className + methodName + errClass;
        String[] ret = errorConfig.get(key);
        if (1 == 2 && ret == null && log.isDebugEnabled()) {
            log.debug("Looked for error info with key [{}] and did not find anything. Keys are...", key);
            for (String keyInSet : errorConfig.keySet()) {
                log.debug("[{}]", keyInSet);
            }
        }
        return ret;
    }

    /**
     * Get an errors configuration from error_info and try to add it in the
     * table with default values if its not there already
     *
     * @param fe
     */
    private void getAndPopulateErrorConfiguration(FriendlyException fe) {
        String errorClassName;
        boolean foundConfig = false;
        try {
            if (fe.topLevelExceptionClassName != null) {
                if (fe.topLevelExceptionClassName.equals(EXCEPTION) || fe.topLevelExceptionClassName.equals(RUNTIME_EXCEPTION)) {
                    // Use the exception message in the lookup instead of the class
                    // Allow arb data to be after the "--" without making it a different error
                    errorClassName = fe.topLevelErrorMessage.split(DASH)[0].trim();
                } else {
                    errorClassName = fe.topLevelExceptionClassName;
                }

                // Now get the error configuration
                String[] config = getErrorConfig(fe.topLevelClassName, fe.topLevelMethodName, errorClassName);

                if (config != null) {
                    populateFriendlyExceptionWithConfig(fe, config);
                    foundConfig = true;
                } else if (fe.rootCauseTechnicalDescription != null && (fe.rootCauseTechnicalDescription.contains("Incorrect string value:")
                        || fe.rootCauseTechnicalDescription.contains("Illegal mix of collations"))) {
                    // Trying to write non latin-1 characters to MySQL
                    fe.topLevelConfiguredDescription = "Invalid characters in text";
                    fe.topLevelConfiguredErrorType = BUSINESS_ERROR;
                    fe.topLevelConfiguredSeverity = DEBUG;
                    fe.topLevelConfiguredMustRollback = ROLLBACK_DEFAULT;
                    fe.topLevelConfiguredErrorCode = "GEN-0001";
                    fe.topLevelConfiguredResolution = "Please remove any weird characters from the text and try again";
                    foundConfig = true;
                }
            } else {
                log.warn("Exception top level class name is null");
            }
        } catch (Exception e) {
            log.warn("Unknown exception getting error info. Probably SCA/PM is down or else verify error_info table has valid entries: [{}]", e.toString());
            log.debug("Error: ", e);
        }

        if (!foundConfig) {
            fe.topLevelConfiguredDescription = DEFAULT_ERROR_DESC;
            fe.topLevelConfiguredErrorType = DEFAULT_ERROR_TYPE;
            fe.topLevelConfiguredSeverity = DEFAULT_ERROR_SEVERITY;
            fe.topLevelConfiguredMustRollback = ROLLBACK_DEFAULT;
            fe.topLevelConfiguredErrorCode = ERROR_CODE_UNKNOWN;
            fe.topLevelConfiguredResolution = DEFAULT_RESOLUTION;
        }
    }

    /**
     * Send trap to Ops
     *
     * @param fe The friendlyexception
     */
    private void sendTrapToOpsManagement(FriendlyException fe) {
        BaseUtils.sendTrapToOpsManagement(fe.topLevelConfiguredSeverity, fe.topLevelClassName, fe.getTrapMessage());
    }

    private void populateFriendlyExceptionWithConfig(FriendlyException fe, String[] configBits) {

        log.debug("Populating FE with config");
        String errorCode = configBits[3];
        String errorType = configBits[4];
        String errorDescription = configBits[5];
        String errorSeverity = configBits[6];
        boolean mustRollback = false;
        if (configBits[7].equalsIgnoreCase("true") || configBits[7].equalsIgnoreCase("yes")) {
            mustRollback = true;
        }
        String errorResolution = configBits[8];

        String action = configBits[9];
        if (action.equals("null")) {
            action = null;
        }

        fe.topLevelConfiguredDescription = errorDescription;
        fe.topLevelConfiguredErrorType = errorType;
        fe.topLevelConfiguredSeverity = errorSeverity;
        fe.topLevelConfiguredMustRollback = mustRollback;
        fe.topLevelConfiguredErrorCode = errorCode;
        fe.topLevelConfiguredResolution = errorResolution;
        fe.topLevelConfiguredAction = action;
    }
}
