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

/**
 *
 * @author paul
 */
@Entity
@Table(name = "impu_visited_network", catalog = "hss_db", schema = "")
@NamedQueries({
    @NamedQuery(name = "ImpuVisitedNetwork.findAll", query = "SELECT i FROM ImpuVisitedNetwork i")})
public class ImpuVisitedNetwork implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Column(name = "id_visited_network")
    private int idVisitedNetwork;
    @JoinColumn(name = "id_impu", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Impu impu;

    public ImpuVisitedNetwork() {
    }

    public ImpuVisitedNetwork(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getIdVisitedNetwork() {
        return idVisitedNetwork;
    }

    public void setIdVisitedNetwork(int idVisitedNetwork) {
        this.idVisitedNetwork = idVisitedNetwork;
    }

   

    public Impu getImpu() {
        return impu;
    }

    public void setImpu(Impu impu) {
        this.impu = impu;
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
        if (!(object instanceof ImpuVisitedNetwork)) {
            return false;
        }
        ImpuVisitedNetwork other = (ImpuVisitedNetwork) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.im.model.ImpuVisitedNetwork[ id=" + id + " ]";
    }
    
}
