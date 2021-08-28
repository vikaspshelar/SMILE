/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.db.model;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 *
 * @author richard
 */
@Entity
@Table(name = "impi_apn", catalog = "hss_db", schema = "")
@NamedQueries({
    @NamedQuery(name = "ImpiApn.findAll", query = "SELECT i FROM ImpiApn i")})
public class ImpiApn implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "apn_name")
    private String apnName;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ipv4")
    private String ipv4;
    @Basic(optional = false)
    @Column(name = "ipv6")
    private String ipv6;
    @Column(name = "type")
    private Integer type;
    @JoinColumn(name = "id_impi", referencedColumnName = "id")
    @ManyToOne
    private Impi impi;

    public ImpiApn() {
    }

    public ImpiApn(Integer id) {
        this.id = id;
    }

    public ImpiApn(Integer id, String apnName, String ipv4, String ipv6, int type) {
        this.id = id;
        this.apnName = apnName;
	this.ipv4 = ipv4;
	this.ipv6 = ipv6;
	this.type = type;
    }

    public Integer getId() {
	return id;
    }

    public void setId(Integer id) {
	this.id = id;
    }

    public String getApnName() {
	return apnName;
    }

    public void setApnName(String apnName) {
	this.apnName = apnName;
    }

    public String getIpv4() {
	return ipv4;
    }

    public void setIpv4(String ipv4) {
	this.ipv4 = ipv4;
    }

    public String getIpv6() {
	return ipv6;
    }

    public void setIpv6(String ipv6) {
	this.ipv6 = ipv6;
    }

    public Impi getImpi() {
	return impi;
    }

    public void setImpi(Impi impi) {
	this.impi = impi;
    }

    public Integer getType() {
	return type;
    }

    public void setType(Integer type) {
	this.type = type;
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
        if (!(object instanceof ImpiApn)) {
            return false;
        }
        ImpiApn other = (ImpiApn) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.im.model.ImpiApn[ id=" + id + " ]";
    }
    
}
