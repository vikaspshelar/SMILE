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
@Table(name = "impi_impu", catalog = "hss_db", schema = "")
@NamedQueries({
    @NamedQuery(name = "ImpiImpu.findAll", query = "SELECT i FROM ImpiImpu i")})
public class ImpiImpu implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "user_state")
    private short userState;
    @JoinColumn(name = "id_impu", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Impu impu;
    @JoinColumn(name = "id_impi", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Impi impi;

    public ImpiImpu() {
    }

    public ImpiImpu(Integer id) {
        this.id = id;
    }

    public ImpiImpu(Integer id, short userState) {
        this.id = id;
        this.userState = userState;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public short getUserState() {
        return userState;
    }

    public void setUserState(short userState) {
        this.userState = userState;
    }

    public Impu getImpu() {
        return impu;
    }

    public void setImpu(Impu impu) {
        this.impu = impu;
    }

    public Impi getImpi() {
        return impi;
    }

    public void setImpi(Impi impi) {
        this.impi = impi;
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
        if (!(object instanceof ImpiImpu)) {
            return false;
        }
        ImpiImpu other = (ImpiImpu) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.im.model.ImpiImpu[ id=" + id + " ]";
    }
    
}
