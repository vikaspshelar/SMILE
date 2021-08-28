/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.base.lifecycle;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.ConnectionLifecycleInterceptor;
import com.smilecoms.commons.base.BaseUtils;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class SmileConnectionLifecycleInterceptor implements ConnectionLifecycleInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SmileConnectionLifecycleInterceptor.class);
    private String name;
    private boolean autoCommitIsOff = false;

    @Override
    public void close() throws SQLException {
    }

    @Override
    public boolean commit() throws SQLException {
        log.debug("Commit called on [{}]", name);
        BaseUtils.addStatisticSample("MySQLCommit", BaseUtils.STATISTIC_TYPE.unitspersecond, 1);
        return true;
    }

    @Override
    public boolean rollback() throws SQLException {
        BaseUtils.addStatisticSample("MySQLRollback", BaseUtils.STATISTIC_TYPE.unitspersecond, 1);
        return true;
    }

    @Override
    public boolean rollback(Savepoint svpnt) throws SQLException {
        return true;
    }

    @Override
    public boolean setAutoCommit(boolean on) throws SQLException {
        if (!on && !autoCommitIsOff) {
            autoCommitIsOff = true;
            log.debug("Allowing setAutoCommit to off");
            return true;
        }
        // Save the network round trip as its off already
        return false;
    }

    @Override
    public boolean setCatalog(String string) throws SQLException {
        return true;
    }

    @Override
    public void init(Connection cnctn, Properties prprts) throws SQLException {
        name = cnctn.toString();
        log.debug("Init for connection [{}]", name);
    }

    @Override
    public void destroy() {
        log.debug("Destroy called on [{}]", name);
    }

    @Override
    public boolean transactionBegun() throws SQLException {
        log.debug("TransactionBegun called on [{}]", name);
        return true;
    }

    @Override
    public boolean transactionCompleted() throws SQLException {
        log.debug("TransactionCompleted called on [{}]", name);
        return true;
    }
}
