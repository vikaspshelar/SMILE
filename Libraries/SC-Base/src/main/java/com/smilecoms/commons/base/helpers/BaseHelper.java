/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.base.helpers;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class BaseHelper {
    
    private static final Logger log = LoggerFactory.getLogger(BaseHelper.class);
    private static final Map<String, Pattern> patternMap = new HashMap<>(10);
    
    public static String parseStreamToString(java.io.InputStream is, String encoding) throws Exception {
        try {
            return IOUtils.toString(is, encoding);
        } finally {
            is.close();
        }
    }
    
    
    public static boolean matches(String field, String regex) {
        Pattern pattern = patternMap.get(regex);
        if (pattern == null) {
            pattern = Pattern.compile(regex);
            synchronized (patternMap) {
                patternMap.put(regex, pattern);
            }
        }
        return pattern.matcher(field).find();
    }
    
    public static String getFullThreadDump() {
        try {
            final StringBuilder dump = new StringBuilder();
            final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            final ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), 100);

            Thread[] nonDaemons = getAllNonDaemonThreads();

            for (ThreadInfo threadInfo : threadInfos) {

                printThreadInfo(threadInfo, dump, nonDaemons);
                LockInfo[] syncs = threadInfo.getLockedSynchronizers();
                printLockInfo(syncs, dump);
                dump.append("\n");
            }
            return dump.toString();
        } catch (Exception e) {
            log.warn("Error: ", e);
            return "Error in getFullThreadDump: " + e.toString();
        }
    }

    static Thread[] getAllNonDaemonThreads() {
        final Thread[] allThreads = getAllThreads();
        final Thread[] nondaemons = new Thread[allThreads.length];
        int nDaemon = 0;
        for (Thread thread : allThreads) {
            if (!thread.isDaemon()) {
                nondaemons[nDaemon++] = thread;
            }
        }
        return java.util.Arrays.copyOf(nondaemons, nDaemon);
    }

    static Thread[] getAllThreads() {
        final ThreadGroup root = getRootThreadGroup();
        final ThreadMXBean thbean = ManagementFactory.getThreadMXBean();
        int nAlloc = thbean.getThreadCount();
        int n = 0;
        Thread[] threads;
        do {
            nAlloc *= 2;
            threads = new Thread[nAlloc];
            n = root.enumerate(threads, true);
        } while (n == nAlloc);
        return java.util.Arrays.copyOf(threads, n);
    }

    static ThreadGroup getThreadGroup(final String name) {
        if (name == null) {
            throw new NullPointerException("Null name");
        }
        final ThreadGroup[] groups = getAllThreadGroups();
        for (ThreadGroup group : groups) {
            if (group.getName().equals(name)) {
                return group;
            }
        }
        return null;
    }

    static ThreadGroup[] getAllThreadGroups() {
        final ThreadGroup root = getRootThreadGroup();
        int nAlloc = root.activeGroupCount();
        int n = 0;
        ThreadGroup[] groups;
        do {
            nAlloc *= 2;
            groups = new ThreadGroup[nAlloc];
            n = root.enumerate(groups, true);
        } while (n == nAlloc);

        ThreadGroup[] allGroups = new ThreadGroup[n + 1];
        allGroups[0] = root;
        System.arraycopy(groups, 0, allGroups, 1, n);
        return allGroups;
    }

    static ThreadGroup rootThreadGroup = null;

    static ThreadGroup getRootThreadGroup() {
        if (rootThreadGroup != null) {
            return rootThreadGroup;
        }
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        ThreadGroup ptg;
        while ((ptg = tg.getParent()) != null) {
            tg = ptg;
        }
        return tg;
    }

    private static final String INDENT = "    ";

    private static void printThreadInfo(ThreadInfo ti, StringBuilder dump, Thread[] nonDaemons) {
        // print thread information
        printThread(ti, dump, nonDaemons);
        dump.append("\n");
        // print stack trace with locks
        StackTraceElement[] stacktrace = ti.getStackTrace();
        MonitorInfo[] monitors = ti.getLockedMonitors();
        for (int i = 0; i < stacktrace.length; i++) {
            StackTraceElement ste = stacktrace[i];
            dump.append(INDENT + "at ").append(ste.toString()).append("\n");
            for (MonitorInfo mi : monitors) {
                if (mi.getLockedStackDepth() == i) {
                    dump.append(INDENT + "  - locked ").append(mi).append("\n");
                }
            }
        }
    }

    private static void printThread(ThreadInfo ti, StringBuilder dump, Thread[] nonDaemons) {
        StringBuilder sb = new StringBuilder("\"" + ti.getThreadName() + "\"" + " Id=" + ti.getThreadId() + " in " + ti.getThreadState());
        if (ti.getLockName() != null) {
            sb.append(" on lock=").append(ti.getLockName());
        }
        if (ti.isSuspended()) {
            sb.append(" (suspended)");
        }
        if (ti.isInNative()) {
            sb.append(" (running in native)");
        }
        for (Thread t : nonDaemons) {
            if (t.getId() == ti.getThreadId()) {
                sb.append(" (NON DAEMON THREAD)");
                break;
            }
        }

        dump.append(sb.toString());
        if (ti.getLockOwnerName() != null) {
            dump.append(INDENT + " owned by ").append(ti.getLockOwnerName()).append(" Id=").append(ti.getLockOwnerId());
        }
    }

    private static void printLockInfo(LockInfo[] locks, StringBuilder dump) {
        dump.append(INDENT + "Locked synchronizers: count = ").append(locks.length);
        for (LockInfo li : locks) {
            dump.append(INDENT + "  - ").append(li);
        }
        dump.append("\n");
    }
    
    
    private static final String IPV4_REGEX = "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$";
    private static final RegexValidator ipv4Validator = new RegexValidator(IPV4_REGEX);

    /**
     * Validates an IPv4 address. Returns true if valid.
     *
     * @param inet4Address the IPv4 address to validate
     * @return true if the argument contains a valid IPv4 address
     */
    public static boolean isValidInet4Address(String inet4Address) {
        // verify that address conforms to generic IPv4 format
        String[] groups = ipv4Validator.match(inet4Address);

        if (groups == null) {
            return false;
        }

        // verify that address subgroups are legal
        for (int i = 0; i <= 3; i++) {
            String ipSegment = groups[i];
            if (ipSegment == null || ipSegment.length() <= 0) {
                return false;
            }

            int iIpSegment = 0;

            try {
                iIpSegment = Integer.parseInt(ipSegment);
            } catch (NumberFormatException e) {
                return false;
            }

            if (iIpSegment > 255) {
                return false;
            }

        }

        return true;
    }
}
