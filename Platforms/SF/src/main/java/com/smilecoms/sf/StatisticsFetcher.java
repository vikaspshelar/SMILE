package com.smilecoms.sf;

import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;
import com.smilecoms.commons.platform.Platform;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import org.slf4j.*;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;
import javax.persistence.Query;

/**
 * The Statistics Fetcher is used to send scheduled statistics to SOP (via
 * syslog) by fetching stats from various sources (as opposed to normal
 * platforms that send stats as they acquire them) <br/><br/> Current
 * implementation includes the following sources for statistics:<br/> SQL
 * Statistics:<br/> SQL statistics are used to generate stats values by running
 * a SQL query. This is useful for dashboarding things on SOP such as revenue,
 * number of customers etc<br/> The stats that should be fetched are cofigured
 * in the SmileDB sql_statistic table as follows:<br/> location -- The location
 * to be reported to SOP. Typically "DB" to indicate the stat was acquired from
 * a database<br/> stat_name -- Name of the statistic<br/> stat_type -- e.g.
 * count, currency, etc<br/> data_source_name -- The data source to use to run
 * the query against. It must exist in the glassfish instance where SF is
 * deployed. E.g. jdbc/SmileDB<br/> stat_query -- The SQL query that should be
 * run against the data source to create the statistic. The query should return
 * a single long/integer value.
 *
 * <br/><br/> The scheduling of StatisticsFetcher is configured via the standard
 * batch framework and would typically be called once a minute or so.
 * <br/><br/> For information on SOP and statistics in general, read up on SOP
 * on the intranet
 *
 * @author PCB
 */
@Singleton
@Startup
@Local({BaseListener.class})
public class StatisticsFetcher implements BaseListener {

    private EntityManagerFactory emf = null;
    private static final Logger log = LoggerFactory.getLogger(StatisticsFetcher.class.getName());
    private boolean running = false;
    private static ScheduledFuture runner1 = null;

    @PostConstruct
    public void startUp() {
        Platform.init();
        BaseUtils.registerForPropsAvailability(this);
        BaseUtils.registerForPropsChanges(this);
        emf = JPAUtils.getEMF("SFPU_RL");
    }

    @Override
    public void propsAreReadyTrigger() {
        runner1 = Async.scheduleAtFixedRate(new SmileBaseRunnable("SF.fetchStats") {
            @Override
            public void run() {
                trigger();
            }
        }, 60000, 1000 * BaseUtils.getIntProperty("env.statisticsfetcher.loop.secs") + Utils.getRandomNumber(0, 10000));
    }

    @PreDestroy
    public void shutDown() {
        BaseUtils.deregisterForPropsAvailability(this);
        BaseUtils.deregisterForPropsChanges(this);
        Async.cancel(runner1);
        Platform.close();
        JPAUtils.closeEMF(emf);
    }

    @Override
    public void propsHaveChangedTrigger() {
        Async.cancel(runner1);
        runner1 = Async.scheduleAtFixedRate(new SmileBaseRunnable("SF.fetchStats") {
            @Override
            public void run() {
                trigger();
            }
        }, 60000, 1000 * BaseUtils.getIntProperty("env.statisticsfetcher.loop.secs") + Utils.getRandomNumber(0, 10000));
    }

    private void trigger() {
        try {
            if (running) {
                log.debug("Im already running");
                return;
            }
            if (log.isDebugEnabled()) {
                log.debug("Statistics Fetcher tiggered by thread [{}] on class [{}]", new Object[]{Thread.currentThread().getId(), this});
            }
            running = true;
            EntityManager em = null;
            try {
                em = JPAUtils.getEM(emf);
                log.debug("running SQL query");
                fetchSQLBasedStatistics(em);
            } catch (Exception ex) {
                log.warn("Error retrieving SQL statistics: " + ex.toString());
            } finally {
                log.debug("Finished Statistics Fetcher run...");
                JPAUtils.closeEM(em);
                running = false;
            }
        } catch (Exception e) {
            log.warn("Error: ", e);
        }
    }

    private List<SqlStatistic> getQueries(EntityManager em) {
        Query q = em.createQuery("SELECT s FROM SqlStatistic s");
        List<SqlStatistic> queries = q.getResultList();
        return queries;
    }

    /**
     * Fetch and send all the SQL based statistics
     *
     * @throws java.lang.Exception
     */
    private void fetchSQLBasedStatistics(EntityManager em) throws Exception {
        List<SqlStatistic> queries = getQueries(em);
        Collections.shuffle(queries); // Lower chance of clashes of multiple threads
        for (SqlStatistic statQuery : queries) {
            try {
                JPAUtils.beginTransaction(em);
                log.debug("Getting locked row");
                statQuery = JPAUtils.findAndThrowENFE(em, SqlStatistic.class, statQuery.getSqlStatisticPK(), LockModeType.PESSIMISTIC_WRITE);
                log.debug("Got locked row to process");
                Calendar nextRun = Calendar.getInstance();
                nextRun.setTime(statQuery.getLastRan());
                nextRun.add(Calendar.SECOND, statQuery.getGapSeconds());
                if (nextRun.getTime().before(new Date())) {
                    String location = statQuery.getSqlStatisticPK().getLocation();
                    String statName = statQuery.getSqlStatisticPK().getStatName();
                    String statType = statQuery.getSqlStatisticPK().getStatType();
                    String query = statQuery.getStatQuery();
                    String dsName = statQuery.getDataSourceName();
                    long start = System.currentTimeMillis();
                    sendQueryBasedStatistic(dsName, query, location, statName, statType);
                    statQuery.setLastRuntimeMillis((int) (System.currentTimeMillis() - start));
                    statQuery.setLastRan(new Date());
                    em.persist(statQuery);
                    em.flush();
                } else {
                    log.debug("No need to run this query yet");
                }
                JPAUtils.commitTransactionAndClear(em);
            } catch (javax.persistence.PessimisticLockException ple) {
                JPAUtils.rollbackTransaction(em);
                log.debug("Timeout getting lock");
            } catch (Exception e) {
                JPAUtils.rollbackTransaction(em);
                log.warn("Error: ", e);
            }
        }
    }

    private Connection getConnection(String dsName) throws Exception {
        return JPAUtils.getNonJTAConnection(dsName);
    }

    private void sendQueryBasedStatistic(String dsName, String query, String location, String statName, String statType) throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        double result;
        try {
            conn = getConnection(dsName);
            ps = conn.prepareStatement(query);
            long start = 0;
            if (log.isDebugEnabled()) {
                log.debug("About to run statistics query: " + query + " on datasource name " + dsName);
                start = System.currentTimeMillis();
            }
            ResultSet rs = ps.executeQuery();
            if (log.isDebugEnabled()) {
                log.debug("Finished running statistics query. Query took " + (System.currentTimeMillis() - start) + "ms");
            }

            while (rs.next()) {
                try {
                    // Try and see if the first column is a description
                    result = rs.getDouble(2);
                    String resultDesc = rs.getString(1);
                    BaseUtils.sendStatistic(location, resultDesc, statType, result, "");
                } catch (Exception e) {
                    result = rs.getDouble(1);
                    BaseUtils.sendStatistic(location, statName, statType, result, "");
                }
            }

        } catch (Exception e) {
            log.warn("Error running statistics query: " + query + ". " + e.toString());
            throw e;
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ex) {
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception ex) {
            }
        }
    }
}
