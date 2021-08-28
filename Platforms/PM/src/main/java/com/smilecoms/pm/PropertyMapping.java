/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.smilecoms.pm;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
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
@Table(name = "property_mapping")
@NamedQueries({
    @NamedQuery(name = "PropertyMapping.findAll", query = "SELECT p FROM PropertyMapping p"),
    @NamedQuery(name = "PropertyMapping.findByClient", query = "SELECT p FROM PropertyMapping p WHERE p.client = :client"),
    @NamedQuery(name = "PropertyMapping.findByPropertyVersion", query = "SELECT p FROM PropertyMapping p WHERE p.propertyVersion = :propertyVersion"),
    @NamedQuery(name = "PropertyMapping.findByLastModified", query = "SELECT p FROM PropertyMapping p WHERE p.lastModified = :lastModified")})
public class PropertyMapping implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "CLIENT")
    private String client;
    @Basic(optional = false)
    @Column(name = "PROPERTY_VERSION")
    private String propertyVersion;
    @Basic(optional = false)
    @Column(name = "LAST_MODIFIED")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastModified;

    public PropertyMapping() {
    }

    public PropertyMapping(String client) {
        this.client = client;
    }

    public PropertyMapping(String client, String propertyVersion, Date lastModified) {
        this.client = client;
        this.propertyVersion = propertyVersion;
        this.lastModified = lastModified;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getPropertyVersion() {
        return propertyVersion;
    }

    public void setPropertyVersion(String propertyVersion) {
        this.propertyVersion = propertyVersion;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (client != null ? client.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PropertyMapping)) {
            return false;
        }
        PropertyMapping other = (PropertyMapping) object;
        if ((this.client == null && other.client != null) || (this.client != null && !this.client.equals(other.client))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.pm.PropertyMapping[client=" + client + "]";
    }

}
