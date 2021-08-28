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
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "sp_shared_ifc_set", catalog = "hss_db", schema = "")
public class SpSharedIfcSet implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @NotNull
    @Column(name = "id")
    private Integer id;
    @JoinColumn(name = "id_shared_ifc_set", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private SharedIfcSet idSharedIfcSet;
    @JoinColumn(name = "id_sp", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Sp idSp;

    public SpSharedIfcSet() {
    }

    public SpSharedIfcSet(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public SharedIfcSet getIdSharedIfcSet() {
        return idSharedIfcSet;
    }

    public void setIdSharedIfcSet(SharedIfcSet idSharedIfcSet) {
        this.idSharedIfcSet = idSharedIfcSet;
    }

    public Sp getIdSp() {
        return idSp;
    }

    public void setIdSp(Sp idSp) {
        this.idSp = idSp;
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
        if (!(object instanceof SpSharedIfcSet)) {
            return false;
        }
        SpSharedIfcSet other = (SpSharedIfcSet) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.im.model.SpSharedIfcSet[ id=" + id + " ]";
    }
    
}
