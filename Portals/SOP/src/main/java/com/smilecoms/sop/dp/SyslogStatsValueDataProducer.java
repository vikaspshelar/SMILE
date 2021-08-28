package com.smilecoms.sop.dp;

import com.smilecoms.sop.helpers.SyslogStatistic;
import com.smilecoms.sop.helpers.SyslogStatsSnapshot;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import de.laures.cewolf.DatasetProduceException;
import de.laures.cewolf.DatasetProducer;
import java.util.List;
import org.slf4j.*;
import org.jfree.data.general.DefaultValueDataset;

/** 
 * An example data producer.
 * @author  Guido Laures 
 */
public class SyslogStatsValueDataProducer implements DatasetProducer, Serializable {

    private static final Logger log = LoggerFactory.getLogger(SyslogStatsValueDataProducer.class.getName());

    @Override
    public Object produceDataset(Map params) throws DatasetProduceException {
        String locationRegex = params.get("location_filter").toString();
        String nameRegex = params.get("name_filter").toString();
        String typeRegex = params.get("type_filter").toString();
        
        
        List<SyslogStatistic> stats =  SyslogStatsSnapshot.getStats(locationRegex, nameRegex, typeRegex);
        double val = 0;
        
        if (stats.size() == 1) {
            val = stats.get(0).getValue();
        }  else if (stats.size() > 1) {
            String aggregationMechanism = params.get("sum_avg").toString();
            if (aggregationMechanism.equalsIgnoreCase("sum")) {
                // sum up all stats
                double total = 0;
                for (SyslogStatistic stat : stats) {
                    total += stat.getValue();
                }
                val = total;
            } else if (aggregationMechanism.equalsIgnoreCase("avg")) {
                // average all stats
                double total = 0;
                int cnt = 0;
                for (SyslogStatistic stat : stats) {
                    total += stat.getValue();
                    cnt ++;
                }
                val = total / (double)cnt;
            }
        } else {
            val = -1.0D;
        }
        
        DefaultValueDataset data = new DefaultValueDataset(val);
        
        return data;
    }

    /**
     * This producer's data is invalidated after 5 seconds. By this method the
     * producer can influence Cewolf's caching behaviour the way it wants to.
     */
    public boolean hasExpired(Map params, Date since) {
        return true;
    }

    /**
     * Returns a unique ID for this DatasetProducer
     */
    public String getProducerId() {
        return "SyslogStatsValueDataProducer";
    }
}
