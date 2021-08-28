/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.db.model;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "charging_info", catalog = "hss_db", schema = "")
@NamedQueries({
    @NamedQuery(name = "ChargingInfo.findAll", query = "SELECT c FROM ChargingInfo c")})
public class ChargingInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @NotNull
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "name")
    private String name;
    @Basic(optional = false)
    @NotNull
    @Column(name = "pri_ecf")
    private String priEcf;
    @Basic(optional = false)
    @NotNull
    @Column(name = "sec_ecf")
    private String secEcf;
    @Basic(optional = false)
    @NotNull
    @Column(name = "pri_ccf")
    private String priCcf;
    @Basic(optional = false)
    @NotNull
    @Column(name = "sec_ccf")
    private String secCcf;

    public ChargingInfo() {
    }

    public ChargingInfo(Integer id) {
        this.id = id;
    }

    public ChargingInfo(Integer id, String name, String priEcf, String secEcf, String priCcf, String secCcf) {
        this.id = id;
        this.name = name;
        this.priEcf = priEcf;
        this.secEcf = secEcf;
        this.priCcf = priCcf;
        this.secCcf = secCcf;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPriEcf() {
        return priEcf;
    }

    public void setPriEcf(String priEcf) {
        this.priEcf = priEcf;
    }

    public String getSecEcf() {
        return secEcf;
    }

    public void setSecEcf(String secEcf) {
        this.secEcf = secEcf;
    }

    public String getPriCcf() {
        return priCcf;
    }

    public void setPriCcf(String priCcf) {
        this.priCcf = priCcf;
    }

    public String getSecCcf() {
        return secCcf;
    }

    public void setSecCcf(String secCcf) {
        this.secCcf = secCcf;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ChargingInfo)) {
            return false;
        }
        ChargingInfo other = (ChargingInfo) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.im.model.ChargingInfo[ id=" + id + " ]";
    }
    
}
