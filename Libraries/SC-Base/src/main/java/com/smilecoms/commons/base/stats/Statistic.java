/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.base.stats;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.BaseUtils.STATISTIC_TYPE;
import org.slf4j.*;

/**
 *
 * @author PCB
 */
public class Statistic {

    private final String name;
    private final STATISTIC_TYPE type;
    private boolean keepTPSStats;
    private boolean keepAverageStats;
    private boolean keepUnitsPSStats;
    private boolean keepPointInTimeStats;
    private boolean keepPercentageStats;
    private static final Logger log = LoggerFactory.getLogger(Statistic.class);
    private final TimeWindowSampleList samples;

    public Statistic(String statName, STATISTIC_TYPE statType, long windowMs) {

        this.name = statName;
        this.type = statType;
        switch (type) {
            case latency:
                keepTPSStats = true;
                keepUnitsPSStats = false;
                keepAverageStats = true;
                keepPointInTimeStats = false;
                keepPercentageStats = false;
                break;
            case concurrency:
                keepTPSStats = false;
                keepUnitsPSStats = false;
                keepAverageStats = false;
                keepPointInTimeStats = true;
                keepPercentageStats = false;
                break;
            case unitspersecond:
                keepTPSStats = false;
                keepUnitsPSStats = true;
                keepAverageStats = false;
                keepPointInTimeStats = false;
                keepPercentageStats = false;
                break;
            case percent:
                keepTPSStats = false;
                keepUnitsPSStats = false;
                keepAverageStats = false;
                keepPointInTimeStats = false;
                keepPercentageStats = true;
                break;
            default:
                break;
        }
        samples = new TimeWindowSampleList(windowMs);
    }

    String getType() {
        return type.toString();
    }

    public synchronized void addSample(long sample) {
        try {
            if (type.equals(STATISTIC_TYPE.percent) && sample != 0 && sample != 1) {
                throw new Exception("Percentage sample must have a value of 1 or 0");
            }
            samples.add(sample);
        } catch (Exception e) {
            log.error("Error", e);
        }
    }

    protected boolean writeStats() {
        try {
            StatsData stats = samples.getStats();
            if (stats == null) {
                // Remove stats that have not been published for a while                    
                return false;
            }

            if (keepAverageStats) {
                StatsManager.writeStat(BaseUtils.FQ_SERVER_NAME, name, type.toString(), stats.getAverage(), "");
            }
            if (keepPointInTimeStats) {
                StatsManager.writeStat(BaseUtils.FQ_SERVER_NAME, name, type.toString(), stats.getMostRecentSample(), "");
            }
            if (keepTPSStats && stats.getSampleCount() >= 20) {
                // Its statictically incorrect to report a tps for such a small sample as it can result in very high values if the sample is small
                StatsManager.writeStat(BaseUtils.FQ_SERVER_NAME, name, "tps", stats.getTps(), "");
            }
            if (keepUnitsPSStats && stats.getSampleCount() >= 20) {
                // Its statictically incorrect to report a sum per sec for such a small sample as it can result in very high values if the sample is small
                StatsManager.writeStat(BaseUtils.FQ_SERVER_NAME, name, "ups", stats.getSumPerSecond(), "");
            }
            if (keepPercentageStats) {
                StatsManager.writeStat(BaseUtils.FQ_SERVER_NAME, name, "percent", stats.getPercent(), "");
                StatsManager.writeStat(BaseUtils.FQ_SERVER_NAME, name, "count", stats.getSampleCount(), "");
            }
        } catch (Exception e) {
            log.warn("Error writing stat [{}] Error: ", getName(), e);
        }
        return true;
    }

    protected String getName() {
        return name;
    }
}
