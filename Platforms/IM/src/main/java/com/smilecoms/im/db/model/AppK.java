/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.db.model;

import java.io.Serializable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "app_k")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "AppK.findAll", query = "SELECT a FROM AppK a")})
public class AppK implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected AppKPK appKPK;

    public AppK() {
    }

    public AppK(AppKPK appKPK) {
        this.appKPK = appKPK;
    }

    public AppK(int idImpi, String appK) {
        this.appKPK = new AppKPK(idImpi, appK);
    }

    public AppKPK getAppKPK() {
        return appKPK;
    }

    public void setAppKPK(AppKPK appKPK) {
        this.appKPK = appKPK;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (appKPK != null ? appKPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof AppK)) {
            return false;
        }
        AppK other = (AppK) object;
        if ((this.appKPK == null && other.appKPK != null) || (this.appKPK != null && !this.appKPK.equals(other.appKPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.im.AppK[ appKPK=" + appKPK + " ]";
    }
    
}
