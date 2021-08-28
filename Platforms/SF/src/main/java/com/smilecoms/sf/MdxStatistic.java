/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sf;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author jaybeepee
 */
@Entity
@Table(name = "mdx_statistic")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "MdxStatistic.findAll", query = "SELECT m FROM MdxStatistic m"),
    @NamedQuery(name = "MdxStatistic.findByLocation", query = "SELECT m FROM MdxStatistic m WHERE m.mdxStatisticPK.location = :location"),
    @NamedQuery(name = "MdxStatistic.findByStatName", query = "SELECT m FROM MdxStatistic m WHERE m.mdxStatisticPK.statName = :statName"),
    @NamedQuery(name = "MdxStatistic.findByStatType", query = "SELECT m FROM MdxStatistic m WHERE m.mdxStatisticPK.statType = :statType"),
    @NamedQuery(name = "MdxStatistic.findByOlapServerUrl", query = "SELECT m FROM MdxStatistic m WHERE m.olapServerUrl = :olapServerUrl"),
    @NamedQuery(name = "MdxStatistic.findByMdxQuery", query = "SELECT m FROM MdxStatistic m WHERE m.mdxQuery = :mdxQuery"),
    @NamedQuery(name = "MdxStatistic.findByOlapServerUname", query = "SELECT m FROM MdxStatistic m WHERE m.olapServerUname = :olapServerUname"),
    @NamedQuery(name = "MdxStatistic.findByOlapServerPasswd", query = "SELECT m FROM MdxStatistic m WHERE m.olapServerPasswd = :olapServerPasswd"),
    @NamedQuery(name = "MdxStatistic.findByOlapDatasourceName", query = "SELECT m FROM MdxStatistic m WHERE m.olapDatasourceName = :olapDatasourceName")})
public class MdxStatistic implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected MdxStatisticPK mdxStatisticPK;
    @Basic(optional = false)
    @NotNull
    @Column(name = "OLAP_SERVER_URL")
    private String olapServerUrl;
    @Basic(optional = false)
    @NotNull
    @Column(name = "MDX_QUERY")
    private String mdxQuery;
    @Basic(optional = false)
    @NotNull
    @Column(name = "OLAP_SERVER_UNAME")
    private String olapServerUname;
    @Basic(optional = false)
    @NotNull
    @Column(name = "OLAP_SERVER_PASSWD")
    private String olapServerPasswd;
    @Basic(optional = false)
    @NotNull
    @Column(name = "OLAP_DATASOURCE_NAME")
    private String olapDatasourceName;

    public MdxStatistic() {
    }

    public MdxStatistic(MdxStatisticPK mdxStatisticPK) {
        this.mdxStatisticPK = mdxStatisticPK;
    }

    public MdxStatistic(MdxStatisticPK mdxStatisticPK, String olapServerUrl, String mdxQuery, String olapServerUname, String olapServerPasswd, String olapDatasourceName) {
        this.mdxStatisticPK = mdxStatisticPK;
        this.olapServerUrl = olapServerUrl;
        this.mdxQuery = mdxQuery;
        this.olapServerUname = olapServerUname;
        this.olapServerPasswd = olapServerPasswd;
        this.olapDatasourceName = olapDatasourceName;
    }

    public MdxStatistic(String location, String statName, String statType) {
        this.mdxStatisticPK = new MdxStatisticPK(location, statName, statType);
    }

    public MdxStatisticPK getMdxStatisticPK() {
        return mdxStatisticPK;
    }

    public void setMdxStatisticPK(MdxStatisticPK mdxStatisticPK) {
        this.mdxStatisticPK = mdxStatisticPK;
    }

    public String getOlapServerUrl() {
        return olapServerUrl;
    }

    public void setOlapServerUrl(String olapServerUrl) {
        this.olapServerUrl = olapServerUrl;
    }

    public String getMdxQuery() {
        return mdxQuery;
    }

    public void setMdxQuery(String mdxQuery) {
        this.mdxQuery = mdxQuery;
    }

    public String getOlapServerUname() {
        return olapServerUname;
    }

    public void setOlapServerUname(String olapServerUname) {
        this.olapServerUname = olapServerUname;
    }

    public String getOlapServerPasswd() {
        return olapServerPasswd;
    }

    public void setOlapServerPasswd(String olapServerPasswd) {
        this.olapServerPasswd = olapServerPasswd;
    }

    public String getOlapDatasourceName() {
        return olapDatasourceName;
    }

    public void setOlapDatasourceName(String olapDatasourceName) {
        this.olapDatasourceName = olapDatasourceName;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (mdxStatisticPK != null ? mdxStatisticPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof MdxStatistic)) {
            return false;
        }
        MdxStatistic other = (MdxStatistic) object;
        if ((this.mdxStatisticPK == null && other.mdxStatisticPK != null) || (this.mdxStatisticPK != null && !this.mdxStatisticPK.equals(other.mdxStatisticPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.sf.MdxStatistic[ mdxStatisticPK=" + mdxStatisticPK + " ]";
    }
    
}
