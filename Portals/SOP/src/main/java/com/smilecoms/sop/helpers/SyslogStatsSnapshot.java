package com.smilecoms.sop.helpers;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.util.Javassist;
import com.smilecoms.commons.util.SSH;
import com.smilecoms.commons.util.Utils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.*;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Pong;

/**
 * The Smile Operations Portal (SOP) is used to dashboard metrics within the
 * system for displaying on LCD screens and operations PC's. The dashboards all
 * run within a web browser and have configurable refresh times to keep the data
 * up to date. The dashboards show "as is" metrics and cannot be used to view
 * historical data. The data is however on disk and can be manually analysed if
 * required. <br/><br/> Data is "sent" to SOP using syslog messages from any
 * other platform in the architecture. Syslog is a standard protocol and within
 * the Java platforms, utility classes have been created to aid in sending
 * syslog messages. These helper classes are located in
 * com.smilecoms.commons.stats. <br/><br/> Here is a typical flow of data from a
 * Java based system till it gets onto a dashboard. We will use an example of a
 * platform wanting to have a dashboard of the latency and transactions per
 * second of it doing an operation called "DoSomething" <br/> 1) When the
 * platform is about to call DoSomething(), it gets the current timestamp<br/>
 * 2) After calling DoSomething(), it gets another timestamp and subtracts the
 * two in order to know the latency in milliseconds of the call to
 * DoSomething()<br/> 3) The platform calls the function
 * com.smilecoms.commons.stats.StatsManager.addSample passing in a name for the
 * statistic (e.g. DoSomething), the type of the statistic (in this case
 * "latency"), whether to also keep track of transactions per second ("true" in
 * this case), and finally the value of the statistic itself (the latency in ms
 * for this example)<br/> 4) The statsManager helper class has a background
 * thread and every 10 seconds, it calculates the average statistic value and
 * tps for each statistic name/type combination and sends a syslog message with
 * the statistic data. The syslog is sent as category local5 and level debug to
 * the ip address defined in SmileDB property env.syslog.hostname. The layout of
 * the syslog message is as follows:<br/> Datestamp
 * host_from_which_message_was_sent SMILE-STATS:
 * |platform|stat_name|stat_type|stat_value|<br/> e.g. for our example,
 * transactions per second are being tracked as well as latency, so 2 stats are
 * sent every 10 seconds:<br/> May 12 10:56:56 smile-dev-zone1.smilesiplab.net
 * SMILE-STATS: |smile-dev-zone1_DAS|Plat_EG.DoSomething|latency|8.0|<br/> May
 * 12 10:56:56 smile-dev-zone1.smilesiplab.net SMILE-STATS:
 * |smile-dev-zone1_DAS|Plat_EG.DoSomething|tps|0.01656653576342324|<br/> Note
 * that the statistic values are the average value of the last 100 samples, so
 * its like a running average calculated every 10s but covering the last 100
 * values. This helps to ensure the stats are a reasonable granuality but not
 * jumping up and down all the time. If a sample is not provided in the last 1
 * hour, then it wont be sent to syslog. This ensures that old stale data is not
 * sent to syslog.<br/> 5) The unix syslogd process on the host located at
 * env.syslog.hostname receives the syslog message and writes it to a file as
 * defined in its /etc/syslog.conf configuration. The syslog.conf should have a
 * line like this (doa kill -1 on the syslogd processid to have it reload the
 * config file. Type dmesg to see config errors):<br/> local5.debug
 * /opt/smile/var/log/stats.log<br/> This instructs syslog to write all messages
 * comming in to category local5 to the file /opt/smile/var/log/stats.log Here
 * is a sample of /opt/smile/var/log/stats.log:<br/><br/> May 12 10:56:56
 * smile-dev-zone1.smilesiplab.net SMILE-STATS:
 * |smile-dev-zone1_DAS|Plat_PM.isStale|latency|8.0|<br/> May 12 10:56:56
 * smile-dev-zone1.smilesiplab.net SMILE-STATS:
 * |smile-dev-zone1_DAS|Plat_PM.isStale|tps|0.01656653576342324|<br/> May 12
 * 10:56:56 smile-dev-zone1.smilesiplab.net SMILE-STATS:
 * |smile-dev-zone1_DAS|Plat_SM|concurrency|0.0|<br/> May 12 10:56:56
 * smile-dev-zone1.smilesiplab.net SMILE-STATS:
 * |smile-dev-zone1_DAS|Plat_RM|concurrency|0.0|<br/> May 12 10:56:56
 * smile-dev-zone1.smilesiplab.net SMILE-STATS:
 * |smile-dev-zone1_DAS|Plat_CTI|concurrency|0.0|<br/> May 12 10:56:56
 * smile-dev-zone1.smilesiplab.net SMILE-STATS:
 * |smile-dev-zone1_DAS|Plat_GM|concurrency|0.0|<br/> May 12 10:56:56
 * smile-dev-zone1.smilesiplab.net SMILE-STATS:
 * |smile-dev-zone1_DAS|Plat_BS|concurrency|0.0|<br/> May 12 10:56:56
 * smile-dev-zone1.smilesiplab.net SMILE-STATS:
 * |smile-dev-zone1_DAS|SCA.isStale|latency|40.0|<br/> May 12 10:56:56
 * smile-dev-zone1.smilesiplab.net SMILE-STATS:
 * |smile-dev-zone1_DAS|SCA.isStale|tps|0.01656656320847943|<br/> May 12
 * 10:56:56 smile-dev-zone1.smilesiplab.net SMILE-STATS:
 * |smile-dev-zone1_DAS|Plat_BM|concurrency|0.0|<br/><br/> 6) Now that all the
 * systems that want to send statistics can do so via syslog, the file
 * /opt/smile/var/log/stats.log will constantly grow with statistics Its now up
 * to SOP to read these statistics and dashboard them. SOP has a background
 * thread that reads all the lines in /opt/smile/var/log/stats.log (configured
 * with property env.stats.syslog.location) and puts them into memory, always
 * only keeping the most recent value for any statitics location/name/type
 * combination. SOP therefore always has a list of the most recent statistic
 * values in memory, being updated every 10 seconds. Note that if a statistic
 * has not been received within the last hour, then it will no longer be stored
 * in memory by SOP and wont be available for dashboarding. When SOP reads the
 * stats every 10 seconds, it moves /opt/smile/var/log/stats.log to a directory
 * defined by property env.stats.working.directory (e.g. /tmp/sop_stats) so that
 * /opt/smile/var/log/stats.log only keeps unprocessed stats. Every
 * env.stats.tar.minutes minutes, SOP tars up the stats files in
 * env.stats.working.directory and places them in env.stats.history.directory.
 * This is useful as it thus keeps a history of all statistics that can be
 * referred to when doing analysis of systems during a past point in time.<br/>
 * 8) With the latest value of each stat in SOP's memory, dashboarding can be
 * done. The dashboards are set up using SmileDB properties as follows:<br/>
 * env.sop.dash.pages -- a list of page id's that should be displayed on SOP
 * (each id refers to a web page that can have muliple graphs on it)<br/>
 * env.sop.dash.page.'id'.layout -- 'id' refers to a page id as in
 * env.sop.dash.pages and the property tells SOP what grid layout is required
 * for the page. e.g. 2X2, 1X1, 1X8 etc<br/>
 * env.sop.dash.page.'id'.url.'grid_index' -- 'id' refers to a page id as in
 * env.sop.dash.pages and 'grid_index' is the area that should display a certain
 * URL. The index's run from top left to bottom right. E.g. in a 3X2 layout,
 * index 4 would be in row 2, column 1. The property defines what URL should be
 * rendered in the frame at that place in the grid.<br/> env.sop.dash.changesecs
 * -- how long to wait in seconds between moving from one page id to the
 * next<br/> So, env.sop.dash.pages and env.sop.dash.page.'id'.layout determine
 * how many pages there are and how many frames there are within each page, and
 * the real meat sits in env.sop.dash.page.'id'.url.'grid_index' as those
 * properties determin the URL to render in each frame.<br/> The next thing to
 * understand is what URL to use if you want to create a dashboard to show the
 * latency and tps for DoSomething. SOP supports displaying of either bar graphs
 * or dials. These are configured as follows:<br/> Dial:<br/>
 * http://<env.sop.host>/sop/dial.jsp?loc=AA&name=BB&type=CC&title=DD&refreshsecs=EE&lower=FF&upper=GG&major=HH&minor=II&unit=JJ&agg=KK&warn=LL
 * AA -- A regular expression to match the location of the statistic you want to
 * dashboard. .* would be for all locations. This gives you the flexibility to
 * dashboard a specific nodes performance or show an average across certain
 * nodes.<br/> BB -- A regular expression to match the name of the statistic to
 * dashboard. For our example we would make this "DoSomething"<br/> CC -- a
 * regular expression matching the type of statistic. For our example we would
 * make this "latency" for one dial and "tps" for another<br/> DD -- Title for
 * the dial e.g. "DoSomething Latency" for one, and "DoSomething TPS" for the
 * other<br/> EE -- Number of seconds between refreshing the frame within the
 * page. This allows you to control not only how often entire pages change, but
 * also refreshing of frames with a page<br/> FF -- The start value to display
 * on the dial<br/> GG -- The the max value to display on the dial<br/> HH --
 * The increment between major values on the dial<br/> II -- The number of
 * "ticks" between major increments<br/> JJ -- The unit to display on the dial.
 * e.g. "ms" and "tps"<br/> KK -- Aggregate mechanism. This is used when the
 * regular expression match more than one statistic. "avg" averages the stats
 * and displays the value, while "sum" adds them up.<br/> LL -- The value above
 * which the dial should go red indicating a warning<br/> So, to display the
 * average latency of DoSomething across all hosts running it, the url would be:
 * <br/>
 * http://<env.sop.host>/sop/dial.jsp?loc=.*&name=DoSmoething&type=latency&title=DoSomething%20Latency&refreshsecs=60&lower=0&upper=200&major=50&minor=5&unit=ms&agg=avg&warn=150<br/>
 * <br/>Note that a -1 indicates that no stats matched the requested regular
 * expressions. BarGraph:<br/>
 * http://<env.sop.host>/sop/bargraph.jsp?loc=AA&name=BB&type=CC&title=DD&refreshsecs=EE
 * The parameters as the same as for dials, but there are fewer of them as the
 * ranges etc do not need to be defined<br/> <br/> One can also put in URL's for
 * platforms other than SOP, so you could display for example a page in ops
 * management platform in one of the frames, or any other page for that matter.
 * <br/><br/> Note that there is also a java platform called StatisticsFetcher
 * (SF) that is scheduled to run via the batch framework. SF reads rows in
 * SmileDB table sql_statistic and runs SQL queries to generate statistic values
 * and send them via syslog. This allows for simple configuration to do
 * dashboards or values that are determined by SQL queries. This is useful for
 * revenue dashboards etc.
 *
 * @author PCB
 */
public class SyslogStatsSnapshot implements BaseListener {

    private static final Map<String, SyslogStatistic> statsMap = new ConcurrentHashMap();
    private static final Map<String, SyslogStatistic> previousStatsMap = new HashMap();
    private static final Logger log = LoggerFactory.getLogger(SyslogStatsSnapshot.class);
    private static ScheduledFuture runner1 = null;
    private static ScheduledFuture runner2 = null;
    private static ScheduledFuture runner3 = null;
    private static ScheduledFuture runner4 = null;
    private static ScheduledFuture runner5 = null;
    private static Tailer tailer = null;
    private static Map<String, String> nameChange = new HashMap<>();
    private static Map<String, String> locationChange = new HashMap<>();
    private static boolean processStats = true;

    public SyslogStatsSnapshot() {
        BaseUtils.registerForPropsAvailability(this);
    }

    public void shutdown() {
        if (tailer != null) {
            tailer.stop();
        }
        Async.cancel(runner1);
        Async.cancel(runner2);
        Async.cancel(runner3);
        Async.cancel(runner4);
        Async.cancel(runner5);
    }

    public static void clearStats() {
        statsMap.clear();
    }

    private void kickOffReadStatsFromSyslogFile() {
        try {
            SSH.executeRemoteOSCommand("root", BaseUtils.getProperty("env.sop.os.root.passwd"), "localhost", 
                    "cp /opt/smile/var/log/stats.log /opt/smile/var/log/stats.log.old; echo \"\" > /opt/smile/var/log/stats.log; PID=`/var/run/rsyslogd.pid`; kill -1 $PID");
        } catch (Exception e) {
            log.error("Error truncating stats file: ", e);
        }
        boolean success = false;
        while (!success) {
            try {
                log.warn("Kicking off stats file tailer");
                String statsFileName = BaseUtils.getProperty("env.stats.syslog.location");
                TailerListener listener = new SyslogFileListener();
                tailer = new Tailer(new File(statsFileName), listener, 500, true);
                tailer.run();
                success = true;
            } catch (Exception e) {
                log.error("Error kicking off readStatsFromSyslogFile: ", e);
                Utils.sleep(1000);
            }
        }
    }

    @Override
    public void propsAreReadyTrigger() {
        propsHaveChangedTrigger();
        Async.execute(new SmileBaseRunnable("SOP.kickOffReadStatsFromSyslogFile") {
            @Override
            public void run() {
                kickOffReadStatsFromSyslogFile();
            }
        });
        runner1 = Async.scheduleAtFixedRate(new SmileBaseRunnable("SOP.removeOldStats") {
            @Override
            public void run() {
                removeOldStats();
            }
        }, 20000, 60000);
        runner2 = Async.scheduleAtFixedRate(new SmileBaseRunnable("SOP.writeStatsToInfluxDB") {
            @Override
            public void run() {
                writeStatsToInfluxDB();
            }
        }, 40000, 60000);
        runner3 = Async.scheduleAtFixedRate(new SmileBaseRunnable("SOP.processRRDs") {
            @Override
            public void run() {
                processRRDs();
            }
        }, 60000, 60000);
        runner4 = Async.scheduleAtFixedRate(new SmileBaseRunnable("SOP.processErrors") {
            @Override
            public void run() {
                processErrors();
            }
        }, 55000, 60000);
        runner5 = Async.scheduleAtFixedRate(new SmileBaseRunnable("SOP.doSOPPostProcessing") {
            @Override
            public void run() {
                doSOPPostProcessing();
            }
        }, 10000, 10000);
        BaseUtils.registerForPropsChanges(this);
    }

    @Override
    public void propsHaveChangedTrigger() {
        try {
            log.debug("Getting props");
            processStats = BaseUtils.getBooleanProperty("env.sop.process.stats", true);
            if (!processStats) {
                log.warn("SOP stats processing is paused");
            }
            try {
                nameChange = BaseUtils.getPropertyAsMap("env.sop.stat.name.replacements");
            } catch (Exception e) {
            }
            try {
                locationChange = BaseUtils.getPropertyAsMap("env.sop.stat.location.replacements");
            } catch (Exception e) {
            }
        } catch (Exception e) {
            log.warn("Props errors [{}]", e.toString());
        }
    }

    private class SyslogFileListener implements TailerListener {

        public SyslogFileListener() {
        }

        @Override
        public void init(Tailer tailer) {
        }

        @Override
        public void fileNotFound() {
            log.warn("Stats file not found: " + BaseUtils.getProperty("env.stats.syslog.location"));
        }

        @Override
        public void fileRotated() {
            log.warn("File rotated: " + BaseUtils.getProperty("env.stats.syslog.location"));
        }

        @Override
        public void handle(String line) {
            processStatLine(line);
        }

        @Override
        public void handle(Exception e) {
            log.warn("Exception: ", e);
        }
    }

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy MMM d HH:mm:ss");

    private void processStatLine(String strLine) {
        if (!processStats) {
            return;
        }
        try {
            log.debug("Processing line [{}]", strLine);
            // Apr 14 11:27:23
            String date = Calendar.getInstance().get(Calendar.YEAR) + " " + strLine.substring(0, 15).replace("  ", " ");
            Date d = sdf.parse(date);

            if (!Utils.isDateInTimeframeInclusive(d, 30, Calendar.SECOND) && !Utils.isInTheFuture(d)) {
                log.debug("Line is too old to parse. Ignoring [{}]", d);
                return;
            }

            String[] bits = strLine.split("\\|");
            SyslogStatistic stat = new SyslogStatistic();
            String newLocation = locationChange.get(bits[1]);
            if (newLocation == null) {
                newLocation = bits[1];
            }
            stat.setLocation(newLocation);
            String newName = nameChange.get(bits[2]);
            if (newName == null) {
                newName = bits[2];
            }
            stat.setName(newName);
            stat.setType(bits[3]);
            stat.setValue(Double.parseDouble(bits[4]));
            if (bits.length > 5) {
                // Has other data
                stat.setOther(bits[5]);
            }
            statsMap.put(stat.getLocation() + stat.getName() + stat.getType(), stat);
        } catch (Exception e) {
            log.warn("Error processing line in stats file. Error: " + e.toString() + ". Line was: " + strLine);
        }
    }

    private void doSOPPostProcessing() {
        if (!processStats) {
            return;
        }
        try {
            log.debug("In doSOPPostProcessing");
            if (!previousStatsMap.isEmpty()) {
                String code = BaseUtils.getPropertyFailFast("env.sop.statsprocessor.code", "");
                try {
                    SCAWrapper.setThreadsRequestContextAsAdmin();
                    if (!code.isEmpty()) {
                        Javassist.runCode(new Class[]{this.getClass(), java.util.Calendar.class, org.slf4j.Logger.class}, code, statsMap, previousStatsMap, log);
                    }
                } catch (Throwable e) {
                    log.warn("Error in postProcess", e);
                } finally {
                    SCAWrapper.removeThreadsRequestContext();
                }
            }
            previousStatsMap.clear();
            previousStatsMap.putAll(statsMap);
            SSH.executeRemoteOSCommand("root", BaseUtils.getProperty("env.sop.os.root.passwd"), "localhost", BaseUtils.getProperty("env.sop.postprocessing.command"));
        } catch (Exception e) {
            log.warn("Error running post processing: " + e.toString());
        }
    }

    private void removeOldStats() {
        if (!processStats) {
            return;
        }
        for (SyslogStatistic stat : statsMap.values()) {
            if (stat.getAgeSecs() > 3600) {
                statsMap.remove(stat.getLocation() + stat.getName() + stat.getType());
            }
        }
    }

    public static List<SyslogStatistic> getStats(String locationRegex, String nameRegex, String typeRegex) {
        long start = 0;
        if (log.isInfoEnabled()) {
            log.debug("Start getStats. Returning syslog based stats for filters as follows: Location=" + locationRegex + " Name=" + nameRegex + " Type=" + typeRegex);
            start = System.currentTimeMillis();
        }
        List<SyslogStatistic> ret = new ArrayList();
        for (SyslogStatistic stat : statsMap.values()) {

            if (log.isDebugEnabled()) {
                log.debug("Checking if stat should be included: Location=" + stat.getLocation() + " Name=" + stat.getName() + " Value=" + stat.getValue());
            }
            if (Utils.matchesWithPatternCache(stat.getLocation(), locationRegex) && Utils.matchesWithPatternCache(stat.getName(), nameRegex) && Utils.matchesWithPatternCache(stat.getType(), typeRegex)) {

                if (log.isDebugEnabled()) {
                    log.debug("Including stat: Location=" + stat.getLocation() + " Name=" + stat.getName() + " Value=" + stat.getValue());
                }
                ret.add(stat);
            }
        }
        if (log.isInfoEnabled()) {
            log.debug("Finished getstats... Took " + (System.currentTimeMillis() - start) + "ms");
        }
        return ret;
    }

    private void processRRDs() {
        try {
            long start = System.currentTimeMillis();
            int cnt = 0;
            String regex = BaseUtils.getProperty("env.sop.rrd.skip.regex", "XXX");

            try {
                File rrdDir = new File("/var/smile/rrd");
                if (!rrdDir.isDirectory()) {
                    log.debug("Creating rrd working directory: /var/smile/rrd");
                    boolean res = rrdDir.mkdirs();
                    if (!res) {
                        throw new Exception("Error creating directory /var/smile/rrd");
                    }
                }
            } catch (Exception e) {
                log.warn("Error Creating rrd working directory: " + e.toString());
            }

            for (SyslogStatistic stat : statsMap.values()) {
                if (Utils.matchesWithPatternCache(stat.getLocation() + "#" + stat.getName() + "#" + stat.getType(), regex)) {
                    continue;
                }
                cnt++;
                RRDHelper.processStat(stat);
            }
            log.info("Updated [{}] stats in RRDBs in [{}]ms", cnt, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.warn("Error: ", e);
        }
    }

    public static Set<String> getStatTypes() {
        Set<String> types = new java.util.TreeSet();
        for (SyslogStatistic stat : statsMap.values()) {
            types.add(stat.getType());
        }
        return types;
    }

    public static Set<String> getStatLocations() {
        Set<String> locations = new java.util.TreeSet();
        for (SyslogStatistic stat : statsMap.values()) {
            locations.add(stat.getLocation());
        }
        return locations;
    }

    public static Set<String> getStatNames() {
        Set<String> names = new java.util.TreeSet();
        for (SyslogStatistic stat : statsMap.values()) {
            names.add(stat.getName());
        }
        return names;
    }

    private static String lastErrorProcessed = null;
    private static boolean firstRun = true;

    private void processErrors() {
        try {
            if (!processStats) {
                return;
            }
            if (!BaseUtils.getBooleanProperty("env.sop.process.errors", true)) {
                log.debug("env.sop.process.errors is false so skipping parsing errors");
                return;
            }
            BufferedReader errorsReader = null;
            log.debug("In processErrors");
            try {
                if (firstRun) {
                    log.warn("This is the first run so clearing out the errors file");
                    try (PrintWriter writer = new PrintWriter(new File(BaseUtils.getProperty("env.sop.err.file.location", "/opt/smile/var/log/smile_errors.log")))) {
                        writer.print("");
                    }
                    firstRun = false;
                }

                errorsReader = new BufferedReader(new FileReader(BaseUtils.getProperty("env.sop.err.file.location", "/opt/smile/var/log/smile_errors.log")));
                String line;
                StringBuilder fullError = new StringBuilder();
                boolean canStartProcessing = false;
                if (lastErrorProcessed == null) {
                    canStartProcessing = true;
                }

                while ((line = errorsReader.readLine()) != null) {
                    if (line.contains("{{{")) {
                        fullError = new StringBuilder();
                    }
                    int contEnd = line.indexOf("...cont...");
                    if (contEnd != -1) {
                        line = line.substring(contEnd + 10);
                    }
                    fullError.append(line);

                    if (line.contains("}}}")) {
                        String err = fullError.toString().replaceAll("\\{\\{\\{", "").replaceAll("\\}\\}\\}", "");
                        log.debug("Looking at error: [{}]", err);
                        if (canStartProcessing) {
                            log.debug("Going to call processFullError");
                            processFullError(err);
                            lastErrorProcessed = err;
                        } else if (lastErrorProcessed.equals(err)) {
                            log.debug("This was the last error processed in the previous run [{}]", err);
                            canStartProcessing = true;
                        } else {
                            log.debug("Ignoring error as its been processed before [{}]", err);
                        }
                    }
                }

                if (!canStartProcessing) {
                    log.debug("File must have been cleared out as we cant find any previously processed error. Going to reset");
                    lastErrorProcessed = null;
                }
            } catch (Exception e) {
                log.warn("Error parsing env.sop.err.file.location: " + e.toString());
            } finally {
                if (errorsReader != null) {
                    try {
                        errorsReader.close();
                    } catch (Exception ex) {
                        log.warn("Error closing buffered reader for errors file: ", ex);
                    }
                }
                log.debug("Finished processErrors");
            }
        } catch (Exception e) {
            log.warn("Error: ", e);
        }
    }

    private static long timestampOfLastErrorProcessed = 0;

    private void processFullError(String err) {

        long now = System.currentTimeMillis();
        log.debug("timestampOfLastErrorProcessed is [{}] and current is [{}]", timestampOfLastErrorProcessed, now);
        if (now - timestampOfLastErrorProcessed < BaseUtils.getLongProperty("env.sop.min.gap.between.errors.ms", 30000)) {
            log.debug("Not processing error [{}] as its too soon after the last one", err);
            return;
        }

        String code = BaseUtils.getPropertyFailFast("env.sop.errorprocessor.code", "");
        try {
            if (!code.isEmpty()) {
                log.debug("Calling javassist code");
                Boolean res = (Boolean) Javassist.runCode(new Class[]{this.getClass(), java.util.Calendar.class, org.slf4j.Logger.class}, code, err, log);
                if (res != null && res) {
                    timestampOfLastErrorProcessed = now;
                }
                log.debug("Called javassist code with result [{}]", res);
            }
        } catch (Throwable e) {
            log.warn("Error in processFullError", e);
        }
    }

    private void writeStatsToInfluxDB() {
        try {
            boolean mustPopulateInflux = BaseUtils.getBooleanProperty("env.sop.influxdb.enable", false);
            if (!mustPopulateInflux) {
                log.debug("writing to influx is disabled - please check boolean property - env.sop.influxdb.enable");
                return;
            }
            log.info("Doing influxDB writing");
            long start = System.currentTimeMillis();
            int cnt = 0;
            org.influxdb.InfluxDB influxDB = null;
            try {
                String influxUrl = BaseUtils.getProperty("env.sop.influxdb.url");  //should be something like "http://10.33.64.19:8086"
                String influxUser = BaseUtils.getProperty("env.sop.influxdb.user");
                String influxPassword = BaseUtils.getProperty("env.sop.influxdb.password");
                String influxDBName = BaseUtils.getProperty("env.sop.influxdb.dbname", "Smile");
                String regex = BaseUtils.getProperty("env.sop.influx.skip.regex", "XXX");

                if (influxUrl == null || influxUser == null || influxPassword == null || influxDBName == null) {
                    log.warn("writing stats to influx db has been enabled but don't have all the details to connect - aborting");
                    return;
                }

                influxDB = InfluxDBFactory.connect(influxUrl, influxUser, influxPassword);
//            influxDB.enableBatch(500, 1000, TimeUnit.MILLISECONDS); /* we will batch 500 entries or 1000 milliseconds */

                Pong response;
                response = influxDB.ping();
                if (response.getVersion().equalsIgnoreCase("unknown")) {
                    log.error("unable to connect to influx db: [{}]", influxUrl);
                    return;
                }
                log.debug("Connected to InfluxDB Version: " + influxDB.version());

                for (SyslogStatistic stat : statsMap.values()) {
                    if (stat.getName().isEmpty() || Utils.matchesWithPatternCache(stat.getLocation() + "#" + stat.getName() + "#" + stat.getType(), regex)) {
                        continue;
                    }
                    cnt++;
                    try {
                        Point point1 = Point.measurement(stat.getName())
                                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                                .addField("value", (float) stat.getValue())
                                .tag("location", stat.getLocation())
                                .tag("type", stat.getType())
                                .build();
                        log.debug("About to write stat [{}]", stat.getName());
                        influxDB.write(influxDBName, "default", point1);
                    } catch (Exception ex) {
                        log.error("Failed to write stat with name [{}]", stat.getName());
                        log.warn("Error: ", ex);
                    }
                }
            } catch (Exception ex) {
                log.error("Error trying to write stats to influx db: [{}]", ex.getMessage());
                log.warn("Error: ", ex);
            } finally {
if (influxDB != null) {
		influxDB.close();
}
	    }
            log.info("Done influxDB writing took [{}]ms for [{}] stats", System.currentTimeMillis() - start, cnt);
        } catch (Exception e) {
            log.warn("Error: ", e);
        }
    }

}
