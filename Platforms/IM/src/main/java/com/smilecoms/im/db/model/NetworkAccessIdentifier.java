/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
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
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 *
 * @author sabza
 */
@Entity
@Table(name = "network_access_identifier", catalog = "SmileDB", schema = "")
@NamedQueries({
    @NamedQuery(name = "NetworkAccessIdentifier.findAll", query = "SELECT n FROM NetworkAccessIdentifier n")})
public class NetworkAccessIdentifier implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "ID")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "NAI_USERNAME")
    private String NAIUsername;
    @Basic(optional = false)
    @NotNull
    @Column(name = "NAI_PASSWORD")
    private String NAIPassword;
    @Basic(optional = false)
    @Column(name = "OSSBSS_REFERENCE_ID")
    private String OSSBSSReferenceId;
    @Column(name = "STATUS")
    private String status;
    @Column(name = "WLAN_ATTACH_STATUS")
    private String WLANAttachStatus;
    @Column(name = "INFO")
    private String info;

    public NetworkAccessIdentifier() {
    }

    public NetworkAccessIdentifier(Integer id) {
        this.id = id;
    }

    public NetworkAccessIdentifier(Integer id, String identity, String password) {
        this.id = id;
        this.NAIUsername = identity;
        this.NAIPassword = password;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNAIUsername() {
        return NAIUsername;
    }

    public void setNAIUsername(String NAIUsername) {
        this.NAIUsername = NAIUsername;
    }

    public String getNAIPassword() {
        return NAIPassword;
    }

    public void setNAIPassword(String NAIPassword) {
        this.NAIPassword = NAIPassword;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getInfo() {
        return info;
    }

    public String getOSSBSSReferenceId() {
        return OSSBSSReferenceId;
    }

    public void setOSSBSSReferenceId(String OSSBSSReferenceId) {
        this.OSSBSSReferenceId = OSSBSSReferenceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getWLANAttachStatus() {
        return WLANAttachStatus;
    }

    public void setWLANAttachStatus(String WLANAttachStatus) {
        this.WLANAttachStatus = WLANAttachStatus;
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
        if (!(object instanceof Impi)) {
            return false;
        }
        NetworkAccessIdentifier other = (NetworkAccessIdentifier) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.im.model.NetworkAccessIdentifier[ id=" + id + " ]";
    }

}
