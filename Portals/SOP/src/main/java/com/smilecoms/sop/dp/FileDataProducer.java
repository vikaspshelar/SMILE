package com.smilecoms.sop.dp;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import de.laures.cewolf.DatasetProduceException;
import de.laures.cewolf.DatasetProducer;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import org.slf4j.*;
import org.jfree.data.time.Minute;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

/**
 * An example data producer.
 *
 * @author Guido Laures
 */
public class FileDataProducer implements DatasetProducer, Serializable {

    private static final Logger log = LoggerFactory.getLogger(FileDataProducer.class.getName());

    public Object produceDataset(Map params) throws DatasetProduceException {


        String filelocation = params.get("filelocation").toString();


        TimeSeriesCollection dataset = new TimeSeriesCollection();

        if (log.isDebugEnabled()) {
            log.debug("Processing file:" + filelocation);
        }
        File data = new File(filelocation);
        if (!data.exists()) {
            // No data
            log.warn("File " + filelocation + " does not exist");
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

        DataInputStream in = null;
        try {
            in = new DataInputStream(new FileInputStream(data));
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            //Read File Line By Line
            while ((strLine = br.readLine()) != null) {
                String[] bits = strLine.split("\\|");
                if (log.isDebugEnabled()) {
                    log.debug("Processing line:" + strLine);
                    for (int i = 0; i < bits.length; i++) {
                        log.debug("bit " + i + " is [" + bits[i] + "]");
                    }
                }
                Float val = Float.valueOf(bits[1]);
                Date d = sdf.parse(bits[0].substring(0, 19));
                Second sec = new Second(d);
                TimeSeries series = dataset.getSeries(bits[2]);
                if (series == null) {
                    series = new TimeSeries(bits[2], Second.class);
                    dataset.addSeries(series);
                }
                series.addOrUpdate(sec, val);
            }
        } catch (Exception ex) {
            log.error(ex.toString());
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                log.error(ex.toString());
            }
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
        return "FileDataProducer";
    }
}
