package gov.nist.core;

import java.util.Properties;
import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

/**
 * This class abstracts away single-instanct and multi0instance loggers
 * legacyLogger is the old-school one logger per stack reference otherLogger is
 * multiinstance logger
 *
 * @author Vladimir Ralev
 *
 */
public class CommonLogger implements StackLogger {


    public static boolean useLegacyLogger = true;
    private final Logger log;

    public CommonLogger(String name) {
        log = LoggerFactory.getLogger(name);
    }

    public static StackLogger getLogger(String name) {
        return new CommonLogger(name);
    }

    public static StackLogger getLogger(Class clazz) {
        return getLogger(clazz.getName());
    }

    public static void init(Properties p) {

    }

    public void disableLogging() {
    }

    public void enableLogging() {

    }

    public int getLineCount() {

        return 0;
    }

    public String getLoggerName() {

        return log.getName();
    }

    public boolean isLoggingEnabled() {

        return log.isDebugEnabled();
    }

    public boolean isLoggingEnabled(int logLevel) {

        return log.isDebugEnabled();
    }

    public void logDebug(String message) {

        log.debug(message);
    }

    public void logDebug(String message, Exception ex) {

        log.debug(message, ex);
    }

    public void logError(String message) {

        log.error(message);
    }

    public void logError(String message, Exception ex) {

        log.error(message, ex);
    }

    public void logException(Throwable ex) {

        log.error("Error: ", ex);
    }

    public void logFatalError(String message) {

        log.error(message);
    }

    public void logInfo(String string) {

        log.info(string);
    }

    public void logStackTrace() {

       log.debug("Stack", new Exception());
    }

    public void logStackTrace(int traceLevel) {

    }

    public void logTrace(String message) {

        log.debug(message);
    }

    public void logWarning(String string) {

        log.warn(string);
    }

    public void setBuildTimeStamp(String buildTimeStamp) {

    }

    public void setStackProperties(Properties stackProperties) {

    }
}
