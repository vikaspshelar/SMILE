/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.db.model;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "imsu", catalog = "hss_db", schema = "")
@NamedQueries({
    @NamedQuery(name = "Imsu.findAll", query = "SELECT i FROM Imsu i")})
public class Imsu implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Column(name = "version")
    @Version
    private int version;
    @Basic(optional = false)
    @NotNull
    @Column(name = "name")
    private String name;
    @Column(name = "scscf_name")
    private String scscfName;
    @Column(name = "diameter_name")
    private String diameterName;
    @Column(name = "id_preferred_scscf_set")
    private int idPreferredScscfSet;
    @Column(name = "id_capabilities_set")
    private int idCapabilitiesSet;
    @OneToMany(mappedBy = "imsu")
    private Collection<Impi> impiCollection;

    public Imsu() {
    }

    public Imsu(Integer id) {
        this.id = id;
    }

    public Imsu(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getVersion() {
        return version;
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

    public String getDiameterName() {
        return diameterName;
    }

    public void setDiameterName(String diameterName) {
        this.diameterName = diameterName;
    }

    public int getIdPreferredScscfSet() {
        return idPreferredScscfSet;
    }

    public void setIdPreferredScscfSet(int idPreferredScscfSet) {
        this.idPreferredScscfSet = idPreferredScscfSet;
    }

    public int getIdCapabilitiesSet() {
        return idCapabilitiesSet;
    }

    public void setIdCapabilitiesSet(int idCapabilitiesSet) {
        this.idCapabilitiesSet = idCapabilitiesSet;
    }

    public Collection<Impi> getImpiCollection() {
        return impiCollection;
    }

    public void setImpiCollection(Collection<Impi> impiCollection) {
        this.impiCollection = impiCollection;
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
        if (!(object instanceof Imsu)) {
            return false;
        }
        Imsu other = (Imsu) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.im.model.Imsu[ id=" + id + " ]";
    }
    
}
