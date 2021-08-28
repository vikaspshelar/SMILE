/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sop.helpers;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static org.rrd4j.ConsolFun.*;
import static org.rrd4j.DsType.GAUGE;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphConstants;
import org.rrd4j.graph.RrdGraphDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class RRDHelper {

    private static final Logger log = LoggerFactory.getLogger(RRDHelper.class);

    public static void processStat(SyslogStatistic stat) {
        log.debug("In procesStat");
        try {
            String rrdPath = getRRDPath(stat);
            log.debug("Updating [{}] in RRD [{}]", stat.getName(), rrdPath);
            RrdDb rrdDb = getOrCreateRRDB(rrdPath);
            try {
                Sample sample = rrdDb.createSample();
                sample.setValue("DATA", stat.getValue());
                sample.update();
            } finally {
                // rrdDb.close();
            }
        } catch (Exception e) {
            log.warn("Error updating stat in RRD", e);
        }
        log.debug("Finished processRRD");
    }

    private static final Map<String, RrdDb> rrdbs = new ConcurrentHashMap<>();

    private static RrdDb getOrCreateRRDB(String rrdPath) throws IOException {
        RrdDb rrdDb = rrdbs.get(rrdPath);
        if (rrdDb == null) {
            if (!org.rrd4j.core.Util.fileExists(rrdPath)) {
                log.debug("Creating new RRD at [{}]", rrdPath);
                RrdDef rrdDef = new RrdDef(rrdPath, 60);
                rrdDef.addDatasource("DATA", GAUGE, 600, 0, Double.NaN); // Unknown if no stat for 10 minutes
                rrdDef.addArchive(AVERAGE, 0.5, 1, 3000); // 50 hours worth of samples every minute
                rrdDef.addArchive(AVERAGE, 0.5, 30, 384); // 8 days worth of 30 minute average
                rrdDef.addArchive(AVERAGE, 0.5, 60, 840); // 35 days worth of hourly average
                rrdDef.addArchive(AVERAGE, 0.5, 1440, 1095); // 3 years worth of daily average
                rrdDef.addArchive(MAX, 0.5, 1, 3000); // 50 hours worth of samples every minute
                rrdDef.addArchive(MAX, 0.5, 30, 384); // 8 days worth of 30 minute max
                rrdDef.addArchive(MAX, 0.5, 60, 840); // 35 days worth of hourly max
                rrdDef.addArchive(MAX, 0.5, 1440, 1095); // 3 years worth of daily max
                rrdDb = new RrdDb(rrdDef);
            } else {
                rrdDb = new RrdDb(rrdPath);
            }
            rrdbs.put(rrdPath, rrdDb);
        }
        return rrdDb;
    }

    public static byte[] getGraph(String location, String name, String type, int width, int height, int startHoursBack, int endHoursBack) throws IOException {

        String title = location + ": " + name + " (" + type + ")";
        RrdGraphDef gDef = new RrdGraphDef();
        gDef.setSmallFont(new Font("monospaced", Font.PLAIN, 12));
        gDef.setLargeFont(new Font("monospaced", Font.BOLD, 16));
        gDef.setWidth(width);
        gDef.setColor(RrdGraphConstants.COLOR_BACK, new Color(0x00, 0x00, 0x00));
        gDef.setColor(RrdGraphConstants.COLOR_CANVAS, new Color(0x00, 0x00, 0x00));
        gDef.setColor(RrdGraphConstants.COLOR_FONT, new Color(0xEB, 0xF0, 0xEB));
        gDef.setColor(RrdGraphConstants.COLOR_ARROW, new Color(0x00, 0x00, 0x00));
        gDef.setHeight(height);
        gDef.setImageFormat("png");
        gDef.setStartTime(startHoursBack * 3600 * -1);
        gDef.setEndTime((endHoursBack * 3600 * -1) - 60); // Dont look at now as there may not be a stat. Look from 1min back
        gDef.setTitle(title);
        gDef.setBase(1000);
        gDef.setAltAutoscaleMax(true);
        gDef.setMinValue(0);
        gDef.setVerticalLabel(type);
        // Main data from RRD File
        gDef.datasource("Main", getRRDPath(location, name, type), "DATA", AVERAGE);
        gDef.area("Main", new Color(0x69, 0xE8, 0x5A));
        if (type.equalsIgnoreCase("latency")
                || type.equalsIgnoreCase("tps")
                || type.equalsIgnoreCase("kstat")) {
            // 95'th percentile line
            gDef.percentile("Percentile", "Main", 95);
            gDef.line("Percentile", Color.YELLOW, "95'th Percentile:", 1);
            gDef.gprint("Percentile", LAST, "%8.2lf %s");
        }
        if (type.equalsIgnoreCase("latency")
                || type.equalsIgnoreCase("tps")
                || type.equalsIgnoreCase("mib")
                || type.equalsIgnoreCase("percent")
                || type.equalsIgnoreCase("kstat")) {
            // Average line
            gDef.datasource("Avg", "Main", AVERAGE);
            gDef.line("Avg", Color.RED, "Average:", 1);
            gDef.gprint("Main", AVERAGE, "%8.2lf %s");
        }
        // Printed data at bottom
        gDef.gprint("Main", LAST, "Current:%8.2lf %s");
        gDef.gprint("Main", MIN, "Min:%8.2lf %s");
        gDef.gprint("Main", MAX, "Max:%8.2lf %s\\c");
        gDef.setFilename("-");
        RrdGraph graph = new RrdGraph(gDef);
        return graph.getRrdGraphInfo().getBytes();
    }

    private static String getRRDPath(SyslogStatistic stat) {
        return getRRDPath(stat.getLocation(), stat.getName(), stat.getType());
    }

    private static String getRRDPath(String location, String name, String type) {
        return "/var/smile/rrd/" + location + "~" + name + "~" + type + ".rrd";
    }
}
