/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.base.stats;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.BaseUtils.STATISTIC_TYPE;
import com.smilecoms.commons.base.helpers.BaseHelper;
import com.smilecoms.commons.base.ops.Syslog;
import com.smilecoms.commons.base.ops.SyslogDefs;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.*;

/**
 *
 * @author PCB
 */
public class StatsManager {

    private static final Map<String, Statistic> statistics = new ConcurrentHashMap<>();
    private static final String DELIMITER = "|";
    private static final Logger log = LoggerFactory.getLogger(StatsManager.class);
    private static long warnMillis = 10000;
    private static String warnMustNotMatch = "^ASYNC|^X3|^Plat_TT|SendEmail";

    public static void addSample(String statName, STATISTIC_TYPE statType, long value, long windowMs) {
        Statistic stat = statistics.get(statName + statType);
        if (stat == null) {
            stat = new Statistic(statName, statType, windowMs);
            statistics.put(statName + statType, stat);
        }
        stat.addSample(value);
        if (statType.equals(STATISTIC_TYPE.latency) && value >= warnMillis && !BaseHelper.matches(statName,warnMustNotMatch)) {
            log.warn("Statistic sample [{}] exceeds warning latency. Latency was [{}]ms", statName, value);
        }
    }

    public static void dump() {
        long start = System.currentTimeMillis();
        try {
            Iterator<Statistic> stats = statistics.values().iterator();
            while (stats.hasNext()) {
                Statistic stat = stats.next();
                boolean stillValid = stat.writeStats();
                if (!stillValid) {
                    statistics.remove(stat.getName() + stat.getType());
                }
            }
            dumpSystemStats();
        } catch (Exception ex) {
            log.error("Error in StatsManager dump [{}]", ex.toString());
        }
        long end = System.currentTimeMillis();
        log.debug("Took [{}]ms to dump stats", end - start);
    }

    public static void reinit() {
        try {
            warnMillis = BaseUtils.getIntPropertyFailFast("env.stats.warn.latency.millis", 10000);
            warnMustNotMatch = BaseUtils.getPropertyFailFast("env.stats.warn.not.match.regex", "^ASYNC|^X3|^Plat_TT|SendEmail");
        } catch (Exception ex) {
            log.error("Error in StatsManager reinit [{}]", ex.toString());
        }
    }

    public static void writeStat(String location, String statName, String statType, double value, String other) {
        try {
            StringBuilder msg = new StringBuilder(DELIMITER);
            msg.append(location);
            msg.append(DELIMITER);
            msg.append(statName);
            msg.append(DELIMITER);
            msg.append(statType);
            msg.append(DELIMITER);
            msg.append(String.valueOf(value));
            msg.append(DELIMITER);
            msg.append(other);
            Syslog.sendSyslog(BaseUtils.getPropertyFailFast("env.syslog.hostname", "127.0.0.1"), "SMILE-STATS", SyslogDefs.LOG_LOCAL5, SyslogDefs.LOG_INFO, msg.toString());
        } catch (Exception ex) {
            log.error("Error in StatsManager writeStat [{}]", ex.toString());
        }
    }

    private static void dumpSystemStats() {
        writeStat(BaseUtils.getHostNameFromKernel(), "OS CPU Utilisation", "percent", SystemStats.getSystemCPUPercentUtilisation(), "");
        writeStat(BaseUtils.getHostNameFromKernel(), "OS Free Physical Memory", "mib", SystemStats.getSystemFreePhysicalMemoryMiB(), "");
        writeStat(BaseUtils.FQ_SERVER_NAME, "JVM Heap Usage", "mib", SystemStats.getJVMHeapUsageMiB(), "");
        writeStat(BaseUtils.FQ_SERVER_NAME, "JVM CPU Utilisation", "percent", SystemStats.getJVMCPUPercentUtilisation(), "");
        writeStat(BaseUtils.FQ_SERVER_NAME, "JVM Open File Descriptors", "count", SystemStats.getJVMNumberOfOpenFileDescriptors(), "");
        Map<String, Long> interfaceBitsPerSecond = SystemStats.getSystemInterfacesBitsPerSecond();
        if (interfaceBitsPerSecond != null) {
            for (Entry<String, Long> entry : interfaceBitsPerSecond.entrySet()) {
                writeStat(BaseUtils.getHostNameFromKernel(), entry.getKey(), "bps", entry.getValue(), "");
            }
        }
    }
}
