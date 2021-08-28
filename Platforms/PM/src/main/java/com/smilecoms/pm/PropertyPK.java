/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.smilecoms.pm;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 *
 * @author paul
 */
@Embeddable
public class PropertyPK implements Serializable {
    @Basic(optional = false)
    @Column(name = "PROPERTY_NAME")
    private String propertyName;
    @Basic(optional = false)
    @Column(name = "PROPERTY_VERSION")
    private String propertyVersion;

    public PropertyPK() {
    }

    public PropertyPK(String propertyName, String propertyVersion) {
        this.propertyName = propertyName;
        this.propertyVersion = propertyVersion;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getPropertyVersion() {
        return propertyVersion;
    }

    public void setPropertyVersion(String propertyVersion) {
        this.propertyVersion = propertyVersion;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (propertyName != null ? propertyName.hashCode() : 0);
        hash += (propertyVersion != null ? propertyVersion.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PropertyPK)) {
            return false;
        }
        PropertyPK other = (PropertyPK) object;
        if ((this.propertyName == null && other.propertyName != null) || (this.propertyName != null && !this.propertyName.equals(other.propertyName))) {
            return false;
        }
        if ((this.propertyVersion == null && other.propertyVersion != null) || (this.propertyVersion != null && !this.propertyVersion.equals(other.propertyVersion))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.pm.PropertyPK[propertyName=" + propertyName + ", propertyVersion=" + propertyVersion + "]";
    }

}
