package com.smilecoms.sop.dp;

import com.smilecoms.sop.helpers.SyslogStatistic;
import com.smilecoms.sop.helpers.SyslogStatsSnapshot;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.jfree.data.category.DefaultCategoryDataset;
import de.laures.cewolf.DatasetProduceException;
import de.laures.cewolf.DatasetProducer;
import java.util.List;
import org.slf4j.*;

/** 
 * An example data producer.
 * @author  Guido Laures 
 */
public class SyslogStatsCategoryDataProducer implements DatasetProducer, Serializable {

    private static final Logger log = LoggerFactory.getLogger(SyslogStatsCategoryDataProducer.class.getName());

    @Override
    public Object produceDataset(Map params) throws DatasetProduceException {
        
       
        String locationRegex = params.get("location_filter").toString();
        String nameRegex = params.get("name_filter").toString();
        String typeRegex = params.get("type_filter").toString();
        
        List<SyslogStatistic> stats =  SyslogStatsSnapshot.getStats(locationRegex, nameRegex, typeRegex);
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        for (SyslogStatistic stat: stats) {
            dataset.addValue(stat.getValue(), stat.getLocation(),stat.getName());
        }
        
        return dataset;

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
        return "SyslogStatsCategoryDataProducer";
    }
}
