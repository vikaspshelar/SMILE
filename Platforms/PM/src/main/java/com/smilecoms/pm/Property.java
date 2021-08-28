/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.smilecoms.pm;

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
@Table(name = "property")
@NamedQueries({
    @NamedQuery(name = "Property.findAll", query = "SELECT p FROM Property p"),
    @NamedQuery(name = "Property.findByPropertyName", query = "SELECT p FROM Property p WHERE p.propertyPK.propertyName = :propertyName"),
    @NamedQuery(name = "Property.findByPropertyValue", query = "SELECT p FROM Property p WHERE p.propertyValue = :propertyValue"),
    @NamedQuery(name = "Property.findByPropertyVersion", query = "SELECT p FROM Property p WHERE p.propertyPK.propertyVersion = :propertyVersion"),
    @NamedQuery(name = "Property.findByLastModified", query = "SELECT p FROM Property p WHERE p.lastModified = :lastModified"),
    @NamedQuery(name = "Property.findByDescription", query = "SELECT p FROM Property p WHERE p.description = :description"),
    @NamedQuery(name = "Property.findByAsOf", query = "SELECT p FROM Property p WHERE p.asOf = :asOf")})
public class Property implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected PropertyPK propertyPK;
    @Basic(optional = false)
    @Column(name = "PROPERTY_VALUE")
    private String propertyValue;
    @Basic(optional = false)
    @Column(name = "LAST_MODIFIED")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastModified;
    @Column(name = "DESCRIPTION")
    private String description;
    @Basic(optional = false)
    @Column(name = "AS_OF")
    @Temporal(TemporalType.TIMESTAMP)
    private Date asOf;

    public Property() {
    }

    public Property(PropertyPK propertyPK) {
        this.propertyPK = propertyPK;
    }

    public Property(PropertyPK propertyPK, String propertyValue, Date lastModified, Date asOf) {
        this.propertyPK = propertyPK;
        this.propertyValue = propertyValue;
        this.lastModified = lastModified;
        this.asOf = asOf;
    }

    public Property(String propertyName, String propertyVersion) {
        this.propertyPK = new PropertyPK(propertyName, propertyVersion);
    }

    public PropertyPK getPropertyPK() {
        return propertyPK;
    }

    public void setPropertyPK(PropertyPK propertyPK) {
        this.propertyPK = propertyPK;
    }

    public String getPropertyValue() {
        return propertyValue;
    }

    public void setPropertyValue(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getAsOf() {
        return asOf;
    }

    public void setAsOf(Date asOf) {
        this.asOf = asOf;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (propertyPK != null ? propertyPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Property)) {
            return false;
        }
        Property other = (Property) object;
        if ((this.propertyPK == null && other.propertyPK != null) || (this.propertyPK != null && !this.propertyPK.equals(other.propertyPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.pm.Property[propertyPK=" + propertyPK + "]";
    }

}
