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
public class ResourcePK implements Serializable {
    @Basic(optional = false)
    @Column(name = "LOCALE")
    private String locale;
    @Basic(optional = false)
    @Column(name = "RESOURCE_KEY")
    private String resourceKey;

    public ResourcePK() {
    }

    public ResourcePK(String locale, String resourceKey) {
        this.locale = locale;
        this.resourceKey = resourceKey;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getResourceKey() {
        return resourceKey;
    }

    public void setResourceKey(String resourceKey) {
        this.resourceKey = resourceKey;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (locale != null ? locale.hashCode() : 0);
        hash += (resourceKey != null ? resourceKey.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ResourcePK)) {
            return false;
        }
        ResourcePK other = (ResourcePK) object;
        if ((this.locale == null && other.locale != null) || (this.locale != null && !this.locale.equals(other.locale))) {
            return false;
        }
        if ((this.resourceKey == null && other.resourceKey != null) || (this.resourceKey != null && !this.resourceKey.equals(other.resourceKey))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.pm.ResourcePK[locale=" + locale + ", resourceKey=" + resourceKey + "]";
    }

}
