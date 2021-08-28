/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.imssc.db.model;

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
 * @author jaybeepee
 */
@Entity
@Table(name = "preferred_scscf_set")
@NamedQueries({
    @NamedQuery(name = "PreferredScscfSet.findAll", query = "SELECT p FROM PreferredScscfSet p"),
    @NamedQuery(name = "PreferredScscfSet.findById", query = "SELECT p FROM PreferredScscfSet p WHERE p.id = :id"),
    @NamedQuery(name = "PreferredScscfSet.findByIdSet", query = "SELECT p FROM PreferredScscfSet p WHERE p.idSet = :idSet"),
    @NamedQuery(name = "PreferredScscfSet.findByName", query = "SELECT p FROM PreferredScscfSet p WHERE p.name = :name"),
    @NamedQuery(name = "PreferredScscfSet.findByScscfName", query = "SELECT p FROM PreferredScscfSet p WHERE p.scscfName = :scscfName"),
    @NamedQuery(name = "PreferredScscfSet.findByPriority", query = "SELECT p FROM PreferredScscfSet p WHERE p.priority = :priority")})
public class PreferredScscfSet implements Serializable {
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
    @Column(name = "scscf_name")
    private String scscfName;
    @Basic(optional = false)
    @NotNull
    @Column(name = "priority")
    private int priority;

    public PreferredScscfSet() {
    }

    public PreferredScscfSet(Integer id) {
        this.id = id;
    }

    public PreferredScscfSet(Integer id, int idSet, String name, String scscfName, int priority) {
        this.id = id;
        this.idSet = idSet;
        this.name = name;
        this.scscfName = scscfName;
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

    public String getScscfName() {
        return scscfName;
    }

    public void setScscfName(String scscfName) {
        this.scscfName = scscfName;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
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
        if (!(object instanceof PreferredScscfSet)) {
            return false;
        }
        PreferredScscfSet other = (PreferredScscfSet) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.imssc.PreferredScscfSet[ id=" + id + " ]";
    }
    
}
