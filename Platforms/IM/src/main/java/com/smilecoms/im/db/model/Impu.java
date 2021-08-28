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
@Table(name = "impu", catalog = "hss_db", schema = "")
@NamedQueries({
    @NamedQuery(name = "Impu.findAll", query = "SELECT i FROM Impu i")})
public class Impu implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "identity")
    private String identity;
    @Basic(optional = false)
    @NotNull
    @Column(name = "type")
    private short type;
    @Basic(optional = false)
    @NotNull
    @Column(name = "psi_activation")
    private short psiActivation;
    @Basic(optional = false)
    @NotNull
    @Column(name = "barring")
    private short barring;
    @Basic(optional = false)
    @NotNull
    @Column(name = "wildcard_psi")
    private String wildcardPSI;
    @Basic(optional = false)
    @NotNull
    @Column(name = "user_state")
    private short userState;
    @Basic(optional = false)
    @NotNull
    @Column(name = "id_implicit_set")
    private int idImplicitSet;
    @Basic(optional = false)
    @NotNull
    @Column(name = "display_name")
    private String displayName;
    @Basic(optional = false)
    @NotNull
    @Column(name = "can_register")
    private short canRegister;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "impu")
    private Collection<ImpiImpu> impiImpuCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "impu")
    private Collection<ImpuVisitedNetwork> impuVisitedNetworkCollection;
    @Column(name = "id_charging_info")
    private int idChargingInfo;
    @Column(name = "id_sp")
    private int idSp;

    public Impu() {
    }

    public Impu(Integer id) {
        this.id = id;
    }

    public Impu(Integer id, String identity, short type, short barring, short userState, int idImplicitSet, String displayName,  short canRegister) {
        this.id = id;
        this.identity = identity;
        this.type = type;
        this.barring = barring;
        this.userState = userState;
        this.idImplicitSet = idImplicitSet;
        this.displayName = displayName;
        this.canRegister = canRegister;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public short getType() {
        return type;
    }

    public void setType(short type) {
        this.type = type;
    }

    public short getBarring() {
        return barring;
    }

    public void setBarring(short barring) {
        this.barring = barring;
    }

    public short getUserState() {
        return userState;
    }

    public void setUserState(short userState) {
        this.userState = userState;
    }

    public int getIdImplicitSet() {
        return idImplicitSet;
    }

    public void setIdImplicitSet(int idImplicitSet) {
        this.idImplicitSet = idImplicitSet;
    }


    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public short getCanRegister() {
        return canRegister;
    }

    public void setCanRegister(short canRegister) {
        this.canRegister = canRegister;
    }

    public Collection<ImpiImpu> getImpiImpuCollection() {
        return impiImpuCollection;
    }

    public void setImpiImpuCollection(Collection<ImpiImpu> impiImpuCollection) {
        this.impiImpuCollection = impiImpuCollection;
    }

    public Collection<ImpuVisitedNetwork> getImpuVisitedNetworkCollection() {
        return impuVisitedNetworkCollection;
    }

    public void setImpuVisitedNetworkCollection(Collection<ImpuVisitedNetwork> impuVisitedNetworkCollection) {
        this.impuVisitedNetworkCollection = impuVisitedNetworkCollection;
    }


    public int getIdChargingInfo() {
        return idChargingInfo;
    }

    public void setIdChargingInfo(int idChargingInfo) {
        this.idChargingInfo = idChargingInfo;
    }

    public int getIdSp() {
        return idSp;
    }

    public void setIdSp(int idSp) {
        this.idSp = idSp;
    }

    public short getPSIActivation() {
        return psiActivation;
    }

    public void setPSIActivation(short psiActivation) {
        this.psiActivation = psiActivation;
    }

    public String getWildcardPSI() {
        return wildcardPSI;
    }

    public void setWildcardPSI(String wildcardPSI) {
        this.wildcardPSI = wildcardPSI;
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
        if (!(object instanceof Impu)) {
            return false;
        }
        Impu other = (Impu) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.im.model.Impu[ id=" + id + " ]";
    }
    
}
