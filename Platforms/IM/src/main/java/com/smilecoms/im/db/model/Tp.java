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
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "tp", catalog = "hss_db", schema = "")
public class Tp implements Serializable {
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
    @Column(name = "condition_type_cnf")
    private int conditionTypeCnf;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "idTp")
    private Collection<Spt> sptCollection;
    @OneToMany(mappedBy = "tp")
    private Collection<Ifc> ifcCollection;

    public Tp() {
    }

    public Tp(Integer id) {
        this.id = id;
    }

    public Tp(Integer id, String name, int conditionTypeCnf) {
        this.id = id;
        this.name = name;
        this.conditionTypeCnf = conditionTypeCnf;
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

    public int getConditionTypeCnf() {
        return conditionTypeCnf;
    }

    public void setConditionTypeCnf(int conditionTypeCnf) {
        this.conditionTypeCnf = conditionTypeCnf;
    }

    public Collection<Spt> getSptCollection() {
        return sptCollection;
    }

    public void setSptCollection(Collection<Spt> sptCollection) {
        this.sptCollection = sptCollection;
    }

    public Collection<Ifc> getIfcCollection() {
        return ifcCollection;
    }

    public void setIfcCollection(Collection<Ifc> ifcCollection) {
        this.ifcCollection = ifcCollection;
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
        if (!(object instanceof Tp)) {
            return false;
        }
        Tp other = (Tp) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.im.model.Tp[ id=" + id + " ]";
    }
    
}
