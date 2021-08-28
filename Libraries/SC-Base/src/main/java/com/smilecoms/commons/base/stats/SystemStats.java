/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.base.stats;

import com.sun.management.OperatingSystemMXBean;
import com.sun.management.UnixOperatingSystemMXBean;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class SystemStats {
    
    private static final long mib = 1024 * 1024;
    private static final Logger log = LoggerFactory.getLogger(SystemStats.class);
    
    public static double getSystemCPUPercentUtilisation() {
        OperatingSystemMXBean osbean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        return osbean.getSystemCpuLoad() * 100;
    }
    
    public static double getJVMCPUPercentUtilisation() {
        OperatingSystemMXBean osbean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        return osbean.getProcessCpuLoad() * 100;
    }
    
    public static long getJVMHeapUsageMiB() {
        Runtime runtime = Runtime.getRuntime();
        long heapUsage = (runtime.totalMemory() - runtime.freeMemory()) / mib;
        return heapUsage;
    }
    
    public static long getJVMNumberOfOpenFileDescriptors() {
        OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        if (os instanceof UnixOperatingSystemMXBean) {
            return ((UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount();
        } else {
            return 0;
        }
    }
    
    public static long getSystemFreePhysicalMemoryMiB() {
        OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        return os.getFreePhysicalMemorySize() / mib;
    }
    
    public static Map<String, Long> getSystemInterfacesBitsPerSecond() {
        Map<String, Long> end = null;
        Map<String, Long> start;
        try {
            start = getCurrentNetworkCounters();
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
            }
            end = getCurrentNetworkCounters();
            for (Entry<String, Long> entry : end.entrySet()) {
                Long bytesAtStart = start.get(entry.getKey());
                if (bytesAtStart == null) {
                    continue;
                }
                entry.setValue((entry.getValue() - bytesAtStart) * 16); // convert to bits and per second 8/0.5=16
            }
        } catch (Exception e) {
            log.warn("Error getting nic stats", e);
        }
        return end;
    }
    
    private static Map<String, Long> getCurrentNetworkCounters() throws Exception {
        Map<String, Long> map = new HashMap<String, Long>();
        String stats = readNetworkStatsFile();
        StringTokenizer tok = new StringTokenizer(stats, "\n");
        int x = 0;
        while (tok.hasMoreTokens()) {
            String line = tok.nextToken();
            if (x++ < 2) {//skip headers
                continue;
            }
            line = line.trim();
            log.debug("Looking at network stats line [{}]", line);
            String[] bits = line.split("\\s+");
            String nic = bits[0].replace(":", "");
            if (nic.equals("lo")) {
                continue;
            }
            long receive = Long.parseLong(bits[1]);
            long transmit = Long.parseLong(bits[9]);
            long totalbytes = receive + transmit;
            map.put(nic, totalbytes);
        }
        return map;
    }
    
    private static String readNetworkStatsFile() throws Exception {
        InputStream in = null;
        try {
            File file = new File("/proc/net/dev");
            StringBuilder sb = new StringBuilder();
            byte[] bytes = new byte[1024];
            int read;
            in = new FileInputStream(file);
            while ((read = in.read(bytes)) != -1) {
                sb.append(new String(bytes, 0, read));
            }
            return sb.toString();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                }
            }
        }
    }
}
