/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sop.vmware;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.cache.CacheHelper;
import com.smilecoms.commons.base.props.PropertyFetchException;
import com.smilecoms.commons.util.JPAUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class HostLookup {

    private static final Logger log = LoggerFactory.getLogger(HostLookup.class);
    private static String SQL;

    public static String getPhysicalHost(String vmHostName) {
        String ret = CacheHelper.getFromLocalCache("HOST_" + vmHostName, String.class);
        if (ret == null) {
            Connection conn = null;
            PreparedStatement ps = null;
            try {
                String user = BaseUtils.getSubProperty("env.vmware.db.config", "user");
                String pass = BaseUtils.getSubProperty("env.vmware.db.config", "pass");
                String host = BaseUtils.getSubProperty("env.vmware.db.config", "host");
                String db = BaseUtils.getSubProperty("env.vmware.db.config", "db");
                String dbType = null;
                try {
                    SQL = BaseUtils.getProperty("env.vmware.db.host.lookup.sql");
                } catch (PropertyFetchException e) {
                }
                try {
                    dbType = BaseUtils.getSubProperty("env.vmware.db.config", "dbtype");
                } catch (PropertyFetchException e) {
                }
                if (dbType == null || dbType.equals("mssql")) {
                    if (SQL == null) {
                        SQL = "SELECT TOP 1 vpxv_hosts.NAME FROM vpxv_vms "
                                + "JOIN vpxv_hosts on vpxv_vms.HOSTID = vpxv_hosts.HOSTID "
                                + "WHERE vpxv_vms.NAME like ?";
                    }
                    conn = JPAUtils.getMSSQLConnection(user, pass, host, db);
                } else {
                    if (SQL == null) {
                        SQL = "SELECT vpxv_hosts.NAME FROM vpxv_vms "
                                + "JOIN vpxv_hosts on vpxv_vms.HOSTID = vpxv_hosts.HOSTID "
                                + "WHERE upper(vpxv_vms.NAME) like ? LIMIT 1";
                    }
                    conn = JPAUtils.getPostgresConnection(user, pass, host, db);
                }
                ps = conn.prepareStatement(SQL);
                ps.setString(1, "%" + vmHostName.toUpperCase() + "%");
                ResultSet rs = ps.executeQuery();
                rs.next();
                ret = rs.getString(1);
                if (ret.contains(".")) {
                    ret = ret.substring(0, ret.indexOf("."));
                }
            } catch (Exception e) {
                log.warn("Error: ", e);
                ret = "Unknown";
            } finally {
                if (ps != null) {
                    try {
                        ps.close();
                    } catch (SQLException ex) {
                    }
                }
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (Exception ex) {
                    }
                }
            }
            CacheHelper.putInLocalCache("HOST_" + vmHostName, ret, 60);
        }
        return ret;
    }

}
