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
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "preferred_scscf_set", catalog = "hss_db", schema = "")
@NamedQueries({
    @NamedQuery(name = "PreferredScscfSet.findAll", query = "SELECT p FROM PreferredScscfSet p")})
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
    @Basic(optional = false)
    @NotNull
    @Column(name = "enabled")
    private int enabled;

    public PreferredScscfSet() {
    }

    public PreferredScscfSet(Integer id) {
        this.id = id;
    }

    public PreferredScscfSet(Integer id, int idSet, String name, String scscfName, int priority, int enabled) {
        this.id = id;
        this.idSet = idSet;
        this.name = name;
        this.scscfName = scscfName;
        this.priority = priority;
        this.enabled = enabled;
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
    
    public int getEnabled() {
        return enabled;
    }

    public void setEnabled(int enabled) {
        this.enabled = enabled;
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
        return "com.smilecoms.im.model.PreferredScscfSet[ id=" + id + " ]";
    }
    
}
