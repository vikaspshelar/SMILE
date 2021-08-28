/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.plugins.onnetsms.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author jaybeepee
 */
@Entity
@Table(name = "ipsmgw_impu")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "IpsmgwImpu.findAll", query = "SELECT i FROM IpsmgwImpu i"),
    @NamedQuery(name = "IpsmgwImpu.findById", query = "SELECT i FROM IpsmgwImpu i WHERE i.id = :id"),
    @NamedQuery(name = "IpsmgwImpu.findByUri", query = "SELECT i FROM IpsmgwImpu i WHERE i.uri = :uri")})
public class IpsmgwImpu implements Serializable {
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
    @ManyToMany(mappedBy = "ipsmgwImpuCollection")
    private Collection<IpsmgwSubscription> ipsmgwSubscriptionCollection;
    @ManyToMany(mappedBy = "ipsmgwImpuCollection")
    private Collection<IpsmgwContact> ipsmgwContactCollection;

    public IpsmgwImpu() {
        ipsmgwContactCollection = new ArrayList<IpsmgwContact>();
        ipsmgwSubscriptionCollection = new ArrayList<IpsmgwSubscription>();
    }

    public IpsmgwImpu(Long id) {
        this();
        this.id = id;
    }

    public IpsmgwImpu(Long id, String uri) {
        this();
        this.id = id;
        this.uri = uri;
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

    @XmlTransient
    public Collection<IpsmgwSubscription> getIpsmgwSubscriptionCollection() {
        return ipsmgwSubscriptionCollection;
    }

    public void setIpsmgwSubscriptionCollection(Collection<IpsmgwSubscription> ipsmgwSubscriptionCollection) {
        this.ipsmgwSubscriptionCollection = ipsmgwSubscriptionCollection;
    }

    @XmlTransient
    public Collection<IpsmgwContact> getIpsmgwContactCollection() {
        return ipsmgwContactCollection;
    }

    public void setIpsmgwContactCollection(Collection<IpsmgwContact> ipsmgwContactCollection) {
        this.ipsmgwContactCollection = ipsmgwContactCollection;
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
        if (!(object instanceof IpsmgwImpu)) {
            return false;
        }
        IpsmgwImpu other = (IpsmgwImpu) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.mm.plugins.onnetsms.model.IpsmgwImpu[ id=" + id + " ]";
    }
    
    public void addContactIfDoesntExist(IpsmgwContact newContact) {
        for (IpsmgwContact c : this.getIpsmgwContactCollection()) {
            if (c.equals(newContact)) {
                return;
            }
        }

        this.getIpsmgwContactCollection().add(newContact);
    }

    
}
