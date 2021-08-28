/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.base;

import com.smilecoms.commons.base.hazelcast.HazelcastHelper;
import com.smilecoms.commons.base.helpers.BaseHelper;
import com.smilecoms.commons.base.helpers.DNSUtils;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.props.Props;
import com.smilecoms.commons.base.props.PropertyFetchException;
import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;
import com.smilecoms.commons.base.ops.SyslogDefs;
import com.smilecoms.commons.base.ops.Syslog;
import com.smilecoms.commons.base.stats.StatsManager;
import com.smilecoms.commons.base.sd.ServiceDiscoveryAgent;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.io.IOUtils;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class BaseUtils {

    private static final Logger log = LoggerFactory.getLogger(BaseUtils.class);
    private static final long bootedAt = System.currentTimeMillis();
    public static final String SERVER_NAME;
    public static final String FQ_SERVER_NAME;
    public static final String SCA_CLIENT_ID;

    static {
        if (System.getProperty("SVRNAME") != null && !System.getProperty("SVRNAME").isEmpty()) {
            SERVER_NAME = System.getProperty("SVRNAME");
            FQ_SERVER_NAME = BaseUtils.getHostNameFromKernel() + "_" + SERVER_NAME;
            SCA_CLIENT_ID = System.getProperty("SCA_CLIENT_ID") == null ? BaseUtils.getHostNameFromKernel() + "_" + SERVER_NAME : System.getProperty("SCA_CLIENT_ID");
        } else {
            SERVER_NAME = "";
            FQ_SERVER_NAME = BaseUtils.getHostNameFromKernel();
            SCA_CLIENT_ID = System.getProperty("SCA_CLIENT_ID") == null ? BaseUtils.getHostNameFromKernel() : System.getProperty("SCA_CLIENT_ID");
        }
    }
    public static boolean IN_SHUTDOWN = false;
    public static final String JVM_ID = UUID.randomUUID().toString().substring(0, 8);
    public static final String SW_VERSION = getSoftwareVersion();
    public static boolean IN_GRACE_PERIOD = true;
    private static final List<BaseListener> propsAvailabilityListeners = new CopyOnWriteArrayList<>();
    private static final List<BaseListener> propsChangeListeners = new CopyOnWriteArrayList<>();
    private static final List<Thread> JVMShutdownListeners = new CopyOnWriteArrayList<>();
    public static final String DEBUG = "DEBUG";
    public static final String INFO = "INFO";
    public static final String MINOR = "MINOR";
    public static final String MAJOR = "MAJOR";
    public static final String SCA_BOOTSTRAP_HOST = System.getProperty("SCA_BOOTSTRAP_HOST") == null ? "http://" + BaseUtils.getHostNameFromKernel() + ":18000" : System.getProperty("SCA_BOOTSTRAP_HOST");

    static {
        log.error("####################### HOBIT Software Version : {} in {} with SCA Client Id {} #######################", new Object[]{SW_VERSION, FQ_SERVER_NAME, SCA_CLIENT_ID});
        File dir = new File("/etc/smile/");
        if (!dir.exists()) {
            try {
                dir.mkdirs();
            } catch (Exception ex) {
                log.warn("Error: ", ex);
            }
        }
        HazelcastHelper.warmUp();
        // Warm up
        ServiceDiscoveryAgent.getInstance();
    }

    public static void shutdownGracefully() {
        if (IN_SHUTDOWN) {
            return;
        }
        log.error("############################################################################################################");
        log.error("I HAVE BEEN INSTRUCTED TO SHUTDOWN GRACEFULLY");
        log.error("############################################################################################################");
        IN_SHUTDOWN = true;
        boolean wasSomethingInHazelCast = ServiceDiscoveryAgent.getInstance().setAllMyServicesAsGoingDown();
        if (wasSomethingInHazelCast) {
            log.error("This JVM had services published. Sleeping for 10s for inflight requests to end");
            for (int i = 10; i > 0; i--) {
                try {
                    log.error("Waiting for traffic to end for another {}s", i);
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                }
            }
        }

        try {
            killMyPID();
            log.warn("Shutting down Hazelcast");
            HazelcastHelper.shutdownBaseHazelcastInstance();
            ThreadMXBean b = ManagementFactory.getThreadMXBean();
            for (int i = 60; i > 0; i--) {
                try {
                    int threads = b.getThreadCount() - b.getDaemonThreadCount();
                    log.error("DOING SYSTEM.EXIT IN {}s IF NON-DAEMON THREADS DONT EXIT GRACEFULLY. CURRENT COUNT IS {}", i, threads);
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                }
            }
        } catch (Exception e) {
            log.warn("This is not TomEE [{}], Doing System.exit()", e.toString());
        }
        log.error("###################### HERE IS A THREAD DUMP ######################");
        log.error("{}", BaseHelper.getFullThreadDump());
        log.error("###################### DOING SYSTEM.EXIT ######################");
        System.exit(0);
    }

    private static void killMyPID() {
        try {
            String name = ManagementFactory.getRuntimeMXBean().getName();
            log.error("########################################");
            log.error("Issuing kill to my own process Id based on name [{}]", name);
            log.error("########################################");
            int pid = getFirstNumericPartOfString(name);
            log.error("My PID is {}", pid);
            Runtime.getRuntime().exec("kill " + pid);
            log.error("Finished calling kill on PID {}", pid);
        } catch (Exception e) {
            log.warn("Error telling myself to shutdown");
        }
    }

    public static enum STATISTIC_TYPE {
        latency, concurrency, unitspersecond, percent;
    };

    static {

        // Logger on cache does not work if its initialised from a portal for some reason
        BaseUtils.registerForJVMShutdown(new Thread() {
            @Override
            public void run() {
                log.error("################# JVM SHUTDOWN HOOK CALLED #################");
                shutdownGracefully();
            }
        });

        // Register timer to check if i must shutdown
        Runnable timerShutdown = new SmileBaseRunnable("Base.ShutdownFileCheck") {
            @Override
            public void run() {
                try {
                    log.debug("Checking for shutdown file");
                    File f = new File("/shutdown");
                    if (f.exists()) {
                        f.delete();
                        log.error("################# SHUTDOWN FILE DETECTED #################");
                        shutdownGracefully();
                    }
                } catch (Exception e) {
                    log.warn("Error in a runner: ", e);
                }
            }
        };
        Async.scheduleWithFixedDelay(timerShutdown, 0, 2000);

        // Register timer to check if properties are stale
        if (System.getProperty("NO_SCA") == null) {
            Runnable propCacheChecker = new SmileBaseRunnable("Base.PropCacheChecker") {
                @Override
                public void run() {
                    try {
                        if (log.isDebugEnabled()) {
                            log.debug("propCacheChecker triggered by timer");
                        }
                        if (!Props.isAvailable()) {
                            return;
                        }

                        try {
                            if (Props.checkIfMustClearCache()) {
                                // props did change
                                for (final BaseListener bl : propsChangeListeners) {
                                    try {
                                        log.warn("Props have changed. Notifying " + bl.getClass().getName());
                                        Async.execute(new SmileBaseRunnable("Base.PropsChangedTrigger") {
                                            @Override
                                            public void run() {
                                                bl.propsHaveChangedTrigger();
                                            }
                                        }
                                        );
                                    } catch (Throwable e) {
                                        log.warn("Error notifying a base listener of properties changing:" + e.toString());
                                    }
                                }
                                StatsManager.reinit();
                            }
                        } catch (Throwable e) {
                            log.warn("Error: ", e);
                        }
                    } catch (Exception e) {
                        log.warn("Error in a runner: ", e);
                    }
                }
            };
            Async.scheduleWithFixedDelay(propCacheChecker, 30000, 10000);
        }

        if (System.getProperty("NO_SCA") == null) {
            // Register timer to trigger listeners wanting to know when props become available
            Runnable propsTrigger = new SmileBaseRunnable("Base.PropsTrigger") {
                @Override
                public void run() {
                    try {
                        if (log.isDebugEnabled()) {
                            log.debug("propsTrigger triggered by timer");
                        }
                        if (IN_GRACE_PERIOD) {
                            if (System.currentTimeMillis() - bootedAt > 120000) {
                                log.warn("JVM has been running for more than 120s. No longer in grace period. Refreshing properties");
                                Props.clearCache();
                                IN_GRACE_PERIOD = false;
                            }
                        }
                        if (Props.testForPropsAvailability() && propsAvailabilityListeners.size() > 0) {
                            List<BaseListener> tmpList = new ArrayList<>();
                            synchronized (propsAvailabilityListeners) {
                                tmpList.addAll(propsAvailabilityListeners);
                                propsAvailabilityListeners.clear();
                            }
                            for (final BaseListener bl : tmpList) {
                                int wait = 5000;
                                if (!IN_GRACE_PERIOD) {
                                    log.warn("Props are available. Notifying [{}]", bl.getClass().getName());
                                    Async.execute(new SmileBaseRunnable("Base.PropsReadyTrigger") {
                                        @Override
                                        public void run() {
                                            bl.propsAreReadyTrigger();
                                        }
                                    });
                                } else {
                                    // Stagger a bit
                                    log.warn("Props are available. Notifying [{}] in about 5s time", bl.getClass().getName());
                                    Async.submitInTheFuture(new SmileBaseRunnable("Base.PropsReadyTrigger") {
                                        @Override
                                        public void run() {
                                            bl.propsAreReadyTrigger();
                                        }
                                    }, wait);
                                    wait += 500;
                                }
                            }
                        }
                    } catch (Throwable e) {
                        log.warn("Error in props ready timer thread: " + e.toString());
                        log.warn("Error: ", e);
                    }
                }
            };
            Async.scheduleWithFixedDelay(propsTrigger, 0, 1000);
        }

        if (System.getProperty("NO_SCA") == null) {
            // Register timer to dump statistics
            Runnable statsDumper = new SmileBaseRunnable("Base.StatsDumper") {
                @Override
                public void run() {
                    try {
                        if (log.isDebugEnabled()) {
                            log.debug("statsDumper triggered by timer");
                        }
                        if (!Props.isAvailable()) {
                            return;
                        }
                        try {
                            StatsManager.dump();
                        } catch (Throwable e) {
                            log.warn("Error: ", e);
                        }
                    } catch (Exception e) {
                        log.warn("Error in a runner: ", e);
                    }
                }
            };

            Async.scheduleAtFixedRate(statsDumper, 40000, 10000);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                log.error("###############");
                log.error("In JVM Shutdown");
                log.error("###############");
                for (Thread t : JVMShutdownListeners) {
                    log.error("Calling thread [{}] on JVM shutdown", t);
                    t.start();
                }
            }
        });

    }

    public static void deregisterForPropsAvailability(BaseListener bl) {
        log.warn(bl.getClass().getName() + " is deregistering from a props availability trigger");
        synchronized (propsAvailabilityListeners) {
            propsAvailabilityListeners.remove(bl);
        }
    }

    public static void deregisterForPropsChanges(BaseListener bl) {
        log.warn(bl.getClass().getName() + " is deregistering from a props changes trigger");
        synchronized (propsAvailabilityListeners) {
            propsChangeListeners.remove(bl);
        }
    }

    public static void registerForJVMShutdown(Thread t) {
        log.warn("Thread is calling registerForJVMShutdown");
        synchronized (JVMShutdownListeners) {
            JVMShutdownListeners.add(t);
        }
    }

    public static void registerForPropsAvailability(BaseListener bl) {
        log.warn(bl.getClass().getName() + " is registering for a props availability trigger");
        synchronized (propsAvailabilityListeners) {
            propsAvailabilityListeners.add(bl);
        }
        log.warn(bl.getClass().getName() + " has finished registering for a props availability trigger. Listener count is " + propsAvailabilityListeners.size());
    }

    public static void registerForPropsChanges(BaseListener bl) {
        log.warn(bl.getClass().getName() + " is registering for a props change trigger");
        synchronized (propsChangeListeners) {
            propsChangeListeners.add(bl);
        }
        log.warn(bl.getClass().getName() + " has finished registering for a props change trigger. Listener count is " + propsChangeListeners.size());
    }

    /**
     *
     * Ops and stats helpers
     *
     *
     */
    public static void sendTrapToOpsManagement(String severity, String component, String message) {
        sendTrapToOpsManagement(severity, component, message, BaseUtils.getHostNameFromKernel());
    }

    public static void sendTrapToOpsManagement(String severity, String component, String message, String device) {
        // Replace non-printable characters as syslog and ops platform only want normal characters
        message = message.replaceAll("[^\\p{ASCII}]", "");

        if (severity.equalsIgnoreCase(MINOR) || severity.equalsIgnoreCase(MAJOR)) {
            StringBuilder msg = new StringBuilder();
            msg.append("Severity: [").append(severity);
            msg.append("] Component: [").append(component);
            msg.append("] Message: [").append(message);
            msg.append("] Device: [").append(device).append("]");
            Syslog.sendSyslogWithSplits(BaseUtils.getPropertyFailFast("env.syslog.hostname", "127.0.0.1"), "JAVA_ERROR", SyslogDefs.LOG_LOCAL6, SyslogDefs.LOG_INFO, msg.toString());
        }
        // PCB - Zenoss not used any more
        //ZenossHelper.sendTrapToZenoss(severity, component, message, eventClass, device);
    }

    public static void addStatisticSample(String statName, STATISTIC_TYPE statType, long value) {
        StatsManager.addSample(statName, statType, value, 60000);
    }

    public static void addStatisticSample(String statName, STATISTIC_TYPE statType, long value, long windowMs) {
        StatsManager.addSample(statName, statType, value, windowMs);
    }

    public static void sendStatistic(String location, String statName, String statType, double value, String other) {
        StatsManager.writeStat(location, statName, statType, value, other);
    }

    /**
     *
     * Property Management
     *
     */
    public static boolean isSCAAvailable() {
        try {
            return ServiceDiscoveryAgent.getInstance().getAvailableService("SCA") != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isPropsAvailable() {
        return Props.isAvailable();
    }

    public static void clearPropertyPache() {
        Props.clearCache();
    }

    public static boolean getBooleanProperty(String prop) {
        return Props.getBooleanProperty(prop);
    }

    public static int getIntProperty(String prop) {
        return Props.getIntProperty(prop);
    }

    public static double getDoubleProperty(String prop) {
        return Props.getDoubleProperty(prop);
    }

    public static BigDecimal getBigDecimalProperty(String prop) {
        return Props.getDecimalProperty(prop);
    }

    public static long getLongProperty(String prop) {
        return Props.getDecimalProperty(prop).longValue();
    }

    public static String getProperty(String prop) {
        return Props.getProperty(prop);
    }

    public static String getPropertyWithoutCache(String prop) {
        return Props.getPropertyWithoutCache(prop);
    }

    public static boolean getBooleanProperty(String prop, boolean defaultValue) {
        return Props.getBooleanProperty(prop, defaultValue);
    }

    public static int getIntProperty(String prop, int defaultValue) {
        return Props.getIntProperty(prop, defaultValue);
    }

    public static double getDoubleProperty(String prop, double defaultValue) {
        return Props.getDoubleProperty(prop, defaultValue);
    }

    public static BigDecimal getBigDecimalProperty(String prop, BigDecimal defaultValue) {
        return Props.getDecimalProperty(prop, defaultValue);
    }

    public static long getLongProperty(String prop, long defaultValue) {
        return Props.getDecimalProperty(prop, new BigDecimal(defaultValue)).longValue();
    }

    public static String getProperty(String prop, String defaultValue) {
        return Props.getProperty(prop, defaultValue);
    }

    public static boolean getBooleanPropertyFailFast(String prop, boolean defaultValue) {
        if (IN_GRACE_PERIOD || !BaseUtils.isPropsAvailable() || !BaseUtils.isSCAAvailable()) {
            return defaultValue;
        }
        return Props.getBooleanProperty(prop, defaultValue);
    }

    public static int getIntPropertyFailFast(String prop, int defaultValue) {
        if (IN_GRACE_PERIOD || !BaseUtils.isPropsAvailable() || !BaseUtils.isSCAAvailable()) {
            return defaultValue;
        }
        return Props.getIntProperty(prop, defaultValue);
    }

    public static double getDoublePropertyFailFast(String prop, double defaultValue) {
        if (IN_GRACE_PERIOD || !BaseUtils.isPropsAvailable() || !BaseUtils.isSCAAvailable()) {
            return defaultValue;
        }
        return Props.getDoubleProperty(prop, defaultValue);
    }

    public static BigDecimal getBigDecimalPropertyFailFast(String prop, BigDecimal defaultValue) {
        if (IN_GRACE_PERIOD || !BaseUtils.isPropsAvailable() || !BaseUtils.isSCAAvailable()) {
            return defaultValue;
        }
        return Props.getDecimalProperty(prop, defaultValue);
    }

    public static long getLongPropertyFailFast(String prop, long defaultValue) {
        if (IN_GRACE_PERIOD || !BaseUtils.isPropsAvailable() || !BaseUtils.isSCAAvailable()) {
            return defaultValue;
        }
        return Props.getDecimalProperty(prop, new BigDecimal(defaultValue)).longValue();
    }

    public static String getPropertyFailFast(String prop, String defaultValue) {
        if (IN_GRACE_PERIOD || !BaseUtils.isPropsAvailable() || !BaseUtils.isSCAAvailable()) {
            return defaultValue;
        }
        return Props.getProperty(prop, defaultValue);
    }

    public static InputStream getPropertyAsStream(String prop) {
        return Props.getPropertyAsStream(prop);
    }

    public static List<String> getPropertyAsList(String prop) {
        return Props.getPropertyAsList(prop);
    }

    public static Set<String> getPropertyAsSet(String prop) {
        return Props.getPropertyAsSet(prop);
    }

    public static Map<String, String> getPropertyAsMap(String prop) {
        return Props.getPropertyAsMap(prop);
    }

    public static List<String[]> getPropertyFromSQL(String prop) {
        return Props.getPropertyFromSQL(prop);
    }

    public static Set<String> getPropertyFromSQLAsSet(String prop) {
        return Props.getPropertyFromSQLAsSet(prop);
    }

    public static List<String[]> getPropertyFromSQLWithoutCache(String prop) {
        return Props.getPropertyFromSQLWithoutCache(prop);
    }

    public static Set<String> getPropertyFromSQLAsSetWithoutCache(String prop) {
        return Props.getPropertyFromSQLAsSetWithoutCache(prop);
    }

    public static String getSubProperty(String property, String subProperty) {
        Set<String> all = BaseUtils.getPropertyAsSet(property);
        for (String row : all) {
            String[] bits = row.split("=", 2);
            if (bits.length == 2) {
                String name = bits[0].trim();
                if (name.equalsIgnoreCase(subProperty)) {
                    return bits[1].trim();
                }
            }
        }
        throw new PropertyFetchException("No sub property " + subProperty + " in " + property);
    }

    public static int getIntSubProperty(String property, String subProperty) {
        return Integer.parseInt(getSubProperty(property, subProperty));
    }

    public static boolean getBooleanSubProperty(String property, String subProperty) {
        return getSubProperty(property, subProperty).equalsIgnoreCase("true");
    }
    /**
     *
     * Host name and IP address utilities
     *
     *
     */
    private static String ipAddress = null;

    /**
     * Returns the IP address of the host where the JVM is running
     *
     * @return IP Address
     */
    public static String getIPAddress() {
        if (ipAddress == null) {
            InetAddress addr;
            try {
                addr = InetAddress.getLocalHost();
            } catch (Exception ex) {
                log.info("Error", ex);
                return "";
            }
            ipAddress = addr.getHostAddress();
            log.info("This hosts IP address is [{}]", ipAddress);
        }
        return ipAddress;
    }
    private static String hostNameFromKernel = null;

    public static String getHostNameFromKernel() {

        if (hostNameFromKernel != null) {
            return hostNameFromKernel;
        }
        Process p = null;
        try {
            p = Runtime.getRuntime().exec("hostname");
            p.waitFor();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            p.getInputStream()));

            StringBuilder result = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            hostNameFromKernel = result.toString().toLowerCase();
            p.destroy();
        } catch (Exception e) {
            log.warn("Error: ", e);
        } finally {
            if (p != null) {
                try {
                    p.getErrorStream().close();
                } catch (Exception ex) {
                }
                try {
                    p.getInputStream().close();
                } catch (Exception ex) {
                }
                try {
                    p.getOutputStream().close();
                } catch (Exception ex) {
                }

                try {
                    p.destroy();
                } catch (Exception e) {
                    log.warn("Error: ", e);
                }
            }
        }
        return hostNameFromKernel;
    }

    private static String getSoftwareVersion() {
        InputStream is = null;
        String softwareVersion;
        try {
            is = BaseUtils.class.getResourceAsStream("/com/smilecoms/commons/base/ver.txt");
            softwareVersion = IOUtils.toString(is, "utf-8");
        } catch (Exception e) {
            log.warn("Error getting software version [{}]", e.toString());
            softwareVersion = "Unknown";
        } finally {
            IOUtils.closeQuietly(is);
        }
        return softwareVersion;
    }

    public static int getFirstNumericPartOfString(String s) {
        return Integer.parseInt(getFirstNumericPartOfStringAsString(s));
    }

    public static String getFirstNumericPartOfStringAsString(String s) {
        final StringBuilder sb = new StringBuilder(s.length());
        boolean canBreak = false;
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c > 47 && c < 58) {
                sb.append(c);
                canBreak = true;
            } else if (canBreak) {
                break;
            }
        }
        return sb.toString();
    }

    public static String doForwardLookup(String hostName) {
        DNSUtils dns = new DNSUtils();
        return dns.doForwardLookup(hostName);
    }

    private static String getShortHostName(String host) {
        if (BaseHelper.isValidInet4Address(host)) {
            return host;
        } else {
            return host.split("\\.")[0];
        }
    }

    /**
     * Return the path portion of a URL
     *
     * @param url
     * @return Path
     */
    public static String getPathFromURL(String url) {
        String path = url;
        try {
            URL u = new URL(url);
            path = u.getPath();
        } catch (MalformedURLException ex) {
            log.error("Error", ex);
        }
        return path;
    }

}
