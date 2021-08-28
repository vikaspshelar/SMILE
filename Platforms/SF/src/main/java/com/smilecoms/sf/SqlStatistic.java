/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.smilecoms.sf;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "sql_statistic")
@NamedQueries({
    @NamedQuery(name = "SqlStatistic.findAll", query = "SELECT s FROM SqlStatistic s"),
    @NamedQuery(name = "SqlStatistic.findByLocation", query = "SELECT s FROM SqlStatistic s WHERE s.sqlStatisticPK.location = :location"),
    @NamedQuery(name = "SqlStatistic.findByStatName", query = "SELECT s FROM SqlStatistic s WHERE s.sqlStatisticPK.statName = :statName"),
    @NamedQuery(name = "SqlStatistic.findByStatType", query = "SELECT s FROM SqlStatistic s WHERE s.sqlStatisticPK.statType = :statType"),
    @NamedQuery(name = "SqlStatistic.findByDataSourceName", query = "SELECT s FROM SqlStatistic s WHERE s.dataSourceName = :dataSourceName"),
    @NamedQuery(name = "SqlStatistic.findByStatQuery", query = "SELECT s FROM SqlStatistic s WHERE s.statQuery = :statQuery")})
public class SqlStatistic implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected SqlStatisticPK sqlStatisticPK;
    @Basic(optional = false)
    @Column(name = "DATA_SOURCE_NAME")
    private String dataSourceName;
    @Basic(optional = false)
    @Column(name = "STAT_QUERY")
    private String statQuery;
    @Column(name = "GAP_SECONDS")
    private int gapSeconds;
    @Column(name = "LAST_RAN")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastRan;
    @Column(name = "LAST_RUNTIME_MILLIS")
    private int lastRuntimeMillis;

    public SqlStatistic() {
    }

    public SqlStatistic(SqlStatisticPK sqlStatisticPK) {
        this.sqlStatisticPK = sqlStatisticPK;
    }

    public SqlStatistic(SqlStatisticPK sqlStatisticPK, String dataSourceName, String statQuery) {
        this.sqlStatisticPK = sqlStatisticPK;
        this.dataSourceName = dataSourceName;
        this.statQuery = statQuery;
    }

    public SqlStatistic(String location, String statName, String statType) {
        this.sqlStatisticPK = new SqlStatisticPK(location, statName, statType);
    }

    public SqlStatisticPK getSqlStatisticPK() {
        return sqlStatisticPK;
    }

    public void setSqlStatisticPK(SqlStatisticPK sqlStatisticPK) {
        this.sqlStatisticPK = sqlStatisticPK;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    public String getStatQuery() {
        return statQuery;
    }

    public void setStatQuery(String statQuery) {
        this.statQuery = statQuery;
    }

    public int getGapSeconds() {
        return gapSeconds;
    }

    public void setGapSeconds(int gapSeconds) {
        this.gapSeconds = gapSeconds;
    }

    public Date getLastRan() {
        return lastRan;
    }

    public void setLastRan(Date lastRan) {
        this.lastRan = lastRan;
    }

    public int getLastRuntimeMillis() {
        return lastRuntimeMillis;
    }

    public void setLastRuntimeMillis(int lastRuntimeMillis) {
        this.lastRuntimeMillis = lastRuntimeMillis;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (sqlStatisticPK != null ? sqlStatisticPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof SqlStatistic)) {
            return false;
        }
        SqlStatistic other = (SqlStatistic) object;
        if ((this.sqlStatisticPK == null && other.sqlStatisticPK != null) || (this.sqlStatisticPK != null && !this.sqlStatisticPK.equals(other.sqlStatisticPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.sf.SqlStatistic[sqlStatisticPK=" + sqlStatisticPK + "]";
    }

}
