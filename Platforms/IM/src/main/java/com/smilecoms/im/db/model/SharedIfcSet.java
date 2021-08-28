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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
@Table(name = "shared_ifc_set", catalog = "hss_db", schema = "")
@NamedQueries({
    @NamedQuery(name = "SharedIfcSet.findAll", query = "SELECT s FROM SharedIfcSet s")})
public class SharedIfcSet implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @NotNull
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "id_set")
    private int idSet;
    @Basic(optional = false)
    @NotNull
    @Column(name = "name")
    private String name;
    @Basic(optional = false)
    @NotNull
    @Column(name = "priority")
    private int priority;
    @JoinColumn(name = "id_ifc", referencedColumnName = "id")
    @ManyToOne
    private Ifc idIfc;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "idSharedIfcSet")
    private Collection<SpSharedIfcSet> spSharedIfcSetCollection;

    public SharedIfcSet() {
    }

    public SharedIfcSet(Integer id) {
        this.id = id;
    }

    public SharedIfcSet(Integer id, int idSet, String name, int priority) {
        this.id = id;
        this.idSet = idSet;
        this.name = name;
        this.priority = priority;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getIdSet() {
        return idSet;
    }

    public void setIdSet(int idSet) {
        this.idSet = idSet;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Ifc getIdIfc() {
        return idIfc;
    }

    public void setIdIfc(Ifc idIfc) {
        this.idIfc = idIfc;
    }

    public Collection<SpSharedIfcSet> getSpSharedIfcSetCollection() {
        return spSharedIfcSetCollection;
    }

    public void setSpSharedIfcSetCollection(Collection<SpSharedIfcSet> spSharedIfcSetCollection) {
        this.spSharedIfcSetCollection = spSharedIfcSetCollection;
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
        if (!(object instanceof SharedIfcSet)) {
            return false;
        }
        SharedIfcSet other = (SharedIfcSet) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.im.model.SharedIfcSet[ id=" + id + " ]";
    }
    
}
