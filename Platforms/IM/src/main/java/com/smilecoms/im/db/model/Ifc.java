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
@Table(name = "ifc", catalog = "hss_db", schema = "")
@NamedQueries({
    @NamedQuery(name = "Ifc.findAll", query = "SELECT i FROM Ifc i")})
public class Ifc implements Serializable {
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
    @Column(name = "profile_part_ind")
    private Integer profilePartInd;
    @OneToMany(mappedBy = "idIfc")
    private Collection<SharedIfcSet> sharedIfcSetCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "idIfc")
    private Collection<SpIfc> spIfcCollection;
    @JoinColumn(name = "id_tp", referencedColumnName = "id")
    @ManyToOne
    private Tp tp;
    @JoinColumn(name = "id_application_server", referencedColumnName = "id")
    @ManyToOne
    private ApplicationServer applicationServer;

    public Ifc() {
    }

    public Ifc(Integer id) {
        this.id = id;
    }

    public Ifc(Integer id, String name) {
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

    public Integer getProfilePartInd() {
        return profilePartInd;
    }

    public void setProfilePartInd(Integer profilePartInd) {
        this.profilePartInd = profilePartInd;
    }

    public Collection<SharedIfcSet> getSharedIfcSetCollection() {
        return sharedIfcSetCollection;
    }

    public void setSharedIfcSetCollection(Collection<SharedIfcSet> sharedIfcSetCollection) {
        this.sharedIfcSetCollection = sharedIfcSetCollection;
    }

    public Collection<SpIfc> getSpIfcCollection() {
        return spIfcCollection;
    }

    public void setSpIfcCollection(Collection<SpIfc> spIfcCollection) {
        this.spIfcCollection = spIfcCollection;
    }

    public Tp getTp() {
        return tp;
    }

    public void setTp(Tp tp) {
        this.tp = tp;
    }

    public ApplicationServer getApplicationServer() {
        return applicationServer;
    }

    public void setApplicationServer(ApplicationServer applicationServer) {
        this.applicationServer = applicationServer;
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
        if (!(object instanceof Ifc)) {
            return false;
        }
        Ifc other = (Ifc) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.im.model.Ifc[ id=" + id + " ]";
    }
    
}
