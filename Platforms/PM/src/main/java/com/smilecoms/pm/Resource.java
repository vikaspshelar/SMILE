/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.smilecoms.pm;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "resource")
@NamedQueries({
    @NamedQuery(name = "Resource.findAll", query = "SELECT r FROM Resource r"),
    @NamedQuery(name = "Resource.findByLocale", query = "SELECT r FROM Resource r WHERE r.resourcePK.locale = :locale"),
    @NamedQuery(name = "Resource.findByResourceKey", query = "SELECT r FROM Resource r WHERE r.resourcePK.resourceKey = :resourceKey"),
    @NamedQuery(name = "Resource.findByValue", query = "SELECT r FROM Resource r WHERE r.value = :value"),
    @NamedQuery(name = "Resource.findByStatus", query = "SELECT r FROM Resource r WHERE r.status = :status")})
public class Resource implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected ResourcePK resourcePK;
    @Basic(optional = false)
    @Column(name = "VALUE")
    private String value;
    @Basic(optional = false)
    @Column(name = "STATUS")
    private String status;

    public Resource() {
    }

    public Resource(ResourcePK resourcePK) {
        this.resourcePK = resourcePK;
    }

    public Resource(ResourcePK resourcePK, String value, String status) {
        this.resourcePK = resourcePK;
        this.value = value;
        this.status = status;
    }

    public Resource(String locale, String resourceKey) {
        this.resourcePK = new ResourcePK(locale, resourceKey);
    }

    public ResourcePK getResourcePK() {
        return resourcePK;
    }

    public void setResourcePK(ResourcePK resourcePK) {
        this.resourcePK = resourcePK;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (resourcePK != null ? resourcePK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Resource)) {
            return false;
        }
        Resource other = (Resource) object;
        if ((this.resourcePK == null && other.resourcePK != null) || (this.resourcePK != null && !this.resourcePK.equals(other.resourcePK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.pm.Resource[resourcePK=" + resourcePK + "]";
    }

}
