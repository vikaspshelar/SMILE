/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.JPAUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class LazyX3Connection {

    Connection conn = null;
    boolean doneInit = false;
    private static final Logger log = LoggerFactory.getLogger(LazyX3Connection.class);

    public LazyX3Connection(boolean alwaysTry) throws Exception {
        init(true);
    }

    public LazyX3Connection() {
    }

    public boolean isConnectionDown() throws Exception {
        init(false);
        return (conn == null);
    }

    private void init(boolean alwaysTry) throws Exception {
        if (doneInit) {
            return;
        }
        doneInit = true;
        conn = getConnection(alwaysTry);
    }

    private static long lastConnectionError = 0;

    public Connection getConnection(boolean alwaysTry) throws Exception {

        if (conn != null) {
            return conn;
        }

        if (BaseUtils.getBooleanProperty("env.pos.x3.offline", false) && !alwaysTry) {
            log.warn("env.pos.x3.offline is true. Wont try to get a conection to X3");
            return null;
        }

        long secsSinceLastFailure = (System.currentTimeMillis() - lastConnectionError) / 1000;
        long secsToGoOfflineAfterFailure = BaseUtils.getLongProperty("env.pos.x3.failure.gooffline.seconds", -1);
        if (secsSinceLastFailure < secsToGoOfflineAfterFailure && !alwaysTry) {
            log.warn("[{}] seconds since last X3 connection failure. Not going to even try to get an online connection", secsSinceLastFailure);
            return null;
        }

        String dsName = "jdbc/X3DB";
        long startTime = 0;
        if (log.isDebugEnabled()) {
            log.debug("Getting a connection from pool with JNDI name [{}]", dsName);
            startTime = System.currentTimeMillis();
        }
        try {
            conn = JPAUtils.getNonJTAConnection(dsName);
        } catch (Exception e) {
            log.warn("Setting lastConnectionError to now due to error getting connection: [{}]. Returning null for X3DB Connection", e.toString());
            lastConnectionError = System.currentTimeMillis();
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "X3 Inventory is going to be retrieved offline for the next "
                    + secsToGoOfflineAfterFailure + " seconds due to error getting connection : " + e.toString());
            return null;
        }
        if (log.isDebugEnabled()) {
            long time = System.currentTimeMillis() - startTime;
            log.debug("Successfully got connection using datasource [{}]. Took [{}]ms", dsName, time);
        }
        return conn;
    }

    public PreparedStatement prepareStatement(String sql) throws Exception {
        init(false);
        return conn.prepareStatement(sql);
    }

    void close() {
        if (conn != null) {
            try {
                log.debug("Closing X3 connection");
                conn.close();
            } catch (Exception e) {
                log.warn("Error closing connection: ", e);
            }
            conn = null;
        }
        doneInit = false;
    }
}
