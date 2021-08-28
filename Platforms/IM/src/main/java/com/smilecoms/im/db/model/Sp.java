/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.db.model;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "sp", catalog = "hss_db", schema = "")
@NamedQueries({
    @NamedQuery(name = "Sp.findAll", query = "SELECT s FROM Sp s")})
public class Sp implements Serializable {
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
    @Column(name = "cn_service_auth")
    private Integer cnServiceAuth;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "idSp")
    private Collection<SpSharedIfcSet> spSharedIfcSetCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "idSp")
    private Collection<SpIfc> spIfcCollection;

    public Sp() {
    }

    public Sp(Integer id) {
        this.id = id;
    }

    public Sp(Integer id, String name) {
        this.id = id;
        this.name = name;
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

    public Integer getCnServiceAuth() {
        return cnServiceAuth;
    }

    public void setCnServiceAuth(Integer cnServiceAuth) {
        this.cnServiceAuth = cnServiceAuth;
    }

    public Collection<SpSharedIfcSet> getSpSharedIfcSetCollection() {
        return spSharedIfcSetCollection;
    }

    public void setSpSharedIfcSetCollection(Collection<SpSharedIfcSet> spSharedIfcSetCollection) {
        this.spSharedIfcSetCollection = spSharedIfcSetCollection;
    }

    public Collection<SpIfc> getSpIfcCollection() {
        return spIfcCollection;
    }

    public void setSpIfcCollection(Collection<SpIfc> spIfcCollection) {
        this.spIfcCollection = spIfcCollection;
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
        if (!(object instanceof Sp)) {
            return false;
        }
        Sp other = (Sp) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.im.model.Sp[ id=" + id + " ]";
    }
    
}
