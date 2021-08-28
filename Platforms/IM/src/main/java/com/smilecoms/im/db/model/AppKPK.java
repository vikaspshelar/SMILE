/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.db.model;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Embeddable
public class AppKPK implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Column(name = "id_impi")
    private int idImpi;
    @Basic(optional = false)
    @NotNull
    @Column(name = "app_k")
    private String appK;

    public AppKPK() {
    }

    public AppKPK(int idImpi, String appK) {
        this.idImpi = idImpi;
        this.appK = appK;
    }

    public int getIdImpi() {
        return idImpi;
    }

    public void setIdImpi(int idImpi) {
        this.idImpi = idImpi;
    }

    public String getAppK() {
        return appK;
    }

    public void setAppK(String appK) {
        this.appK = appK;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (int) idImpi;
        hash += (appK != null ? appK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof AppKPK)) {
            return false;
        }
        AppKPK other = (AppKPK) object;
        if (this.idImpi != other.idImpi) {
            return false;
        }
        if ((this.appK == null && other.appK != null) || (this.appK != null && !this.appK.equals(other.appK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.im.AppKPK[ idImpi=" + idImpi + ", appK=" + appK + " ]";
    }
    
}
