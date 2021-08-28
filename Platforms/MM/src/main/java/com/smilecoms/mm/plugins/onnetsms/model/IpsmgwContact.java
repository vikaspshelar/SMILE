/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.plugins.onnetsms.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author jaybeepee
 */
@Entity
@Table(name = "ipsmgw_contact")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "IpsmgwContact.findAll", query = "SELECT i FROM IpsmgwContact i"),
    @NamedQuery(name = "IpsmgwContact.findById", query = "SELECT i FROM IpsmgwContact i WHERE i.id = :id"),
    @NamedQuery(name = "IpsmgwContact.findByUri", query = "SELECT i FROM IpsmgwContact i WHERE i.uri = :uri"),
    @NamedQuery(name = "IpsmgwContact.findBySmsFormat", query = "SELECT i FROM IpsmgwContact i WHERE i.smsFormat = :smsFormat"),
    @NamedQuery(name = "IpsmgwContact.findByPath", query = "SELECT i FROM IpsmgwContact i WHERE i.path = :path"),
    @NamedQuery(name = "IpsmgwContact.findByExpires", query = "SELECT i FROM IpsmgwContact i WHERE i.expires = :expires")})
public class IpsmgwContact implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "ID")
    private Long id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "URI")
    private String uri;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SMS_FORMAT")
    private String smsFormat;
    @Column(name = "PATH")
    private String path;
    @Column(name = "EXPIRES")
    @Temporal(TemporalType.TIMESTAMP)
    private Date expires;
    @JoinTable(name = "ipsmgw_impu_contact", joinColumns = {
        @JoinColumn(name = "CONTACT_ID", referencedColumnName = "ID")}, inverseJoinColumns = {
        @JoinColumn(name = "IMPU_ID", referencedColumnName = "ID")})
    @ManyToMany
    private Collection<IpsmgwImpu> ipsmgwImpuCollection;

    public void addImpuIfDoesntExist(IpsmgwImpu impu) {
        for (IpsmgwImpu i : this.getIpsmgwImpuCollection()) {
            if (i.equals(impu)) {
                return;
            }
        }

        this.getIpsmgwImpuCollection().add(impu);
    }
    
    public enum SmsFormatType {binary3gpp, sip};

    public IpsmgwContact() {
        ipsmgwImpuCollection = new ArrayList<IpsmgwImpu>();
    }

    public IpsmgwContact(Long id) {
        this();
        this.id = id;
    }

    public IpsmgwContact(Long id, String uri, String smsFormat) {
        this();
        this.id = id;
        this.uri = uri;
        this.smsFormat = smsFormat;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getSmsFormat() {
        return smsFormat;
    }

    public void setSmsFormat(String smsFormat) {
        this.smsFormat = smsFormat;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Date getExpires() {
        return expires;
    }

    public void setExpires(Date expires) {
        this.expires = expires;
    }

    @XmlTransient
    public Collection<IpsmgwImpu> getIpsmgwImpuCollection() {
        return ipsmgwImpuCollection;
    }

    public void setIpsmgwImpuCollection(Collection<IpsmgwImpu> ipsmgwImpuCollection) {
        this.ipsmgwImpuCollection = ipsmgwImpuCollection;
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
        if (!(object instanceof IpsmgwContact)) {
            return false;
        }
        IpsmgwContact other = (IpsmgwContact) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.mm.plugins.onnetsms.model.IpsmgwContact[ id=" + id + " ]";
    }
    
}
