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
 * @author paul
 */
@Entity
@Table(name = "sp_ifc", catalog = "hss_db", schema = "")
public class SpIfc implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @NotNull
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "priority")
    private int priority;
    @JoinColumn(name = "id_ifc", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Ifc idIfc;
    @JoinColumn(name = "id_sp", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Sp idSp;

    public SpIfc() {
    }

    public SpIfc(Integer id) {
        this.id = id;
    }

    public SpIfc(Integer id, int priority) {
        this.id = id;
        this.priority = priority;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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
        if (!(object instanceof SpIfc)) {
            return false;
        }
        SpIfc other = (SpIfc) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.im.model.SpIfc[ id=" + id + " ]";
    }
    
}
