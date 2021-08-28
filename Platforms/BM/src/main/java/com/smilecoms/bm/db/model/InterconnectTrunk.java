/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.db.model;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "interconnect_trunk")
@NamedQueries({
    @NamedQuery(name = "InterconnectTrunk.findAll", query = "SELECT i FROM InterconnectTrunk i")})
public class InterconnectTrunk implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "EXTERNAL_ID")
    private String externalId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "INTERNAL_ID")
    private String internalId;

    public InterconnectTrunk() {
    }

    public InterconnectTrunk(String externalId) {
        this.externalId = externalId;
    }

    public InterconnectTrunk(String externalId, String internalId) {
        this.externalId = externalId;
        this.internalId = internalId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getInternalId() {
        return internalId;
    }

    public void setInternalId(String internalId) {
        this.internalId = internalId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (externalId != null ? externalId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof InterconnectTrunk)) {
            return false;
        }
        InterconnectTrunk other = (InterconnectTrunk) object;
        if ((this.externalId == null && other.externalId != null) || (this.externalId != null && !this.externalId.equals(other.externalId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.bm.db.model.InterconnectTrunk[ externalId=" + externalId + " ]";
    }
    
}
