/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.db.model;

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
@Table(name = "interconnect_partner")
@NamedQueries({
    @NamedQuery(name = "InterconnectPartner.findAll", query = "SELECT i FROM InterconnectPartner i")})
public class InterconnectPartner implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "INTERCONNECT_PARTNER_ID")
    private Integer interconnectPartnerId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PARTNER_NAME")
    private String partnerName;
    @Basic(optional = false)
    @NotNull
    @Column(name = "INFO")
    private String info;
    @Column(name = "INTERCONNECT_PARTNER_CODE")
    private String interconnectPartnerCode;

    public InterconnectPartner() {
    }

    public InterconnectPartner(Integer interconnectPartnerId) {
        this.interconnectPartnerId = interconnectPartnerId;
    }

    public InterconnectPartner(Integer interconnectPartnerId, String partnerName, String info) {
        this.interconnectPartnerId = interconnectPartnerId;
        this.partnerName = partnerName;
        this.info = info;
    }

    public Integer getInterconnectPartnerId() {
        return interconnectPartnerId;
    }

    public void setInterconnectPartnerId(Integer interconnectPartnerId) {
        this.interconnectPartnerId = interconnectPartnerId;
    }

    public String getInterconnectPartnerCode() {
        return interconnectPartnerCode;
    }

    public void setInterconnectPartnerCode(String interconnectPartnerCode) {
        this.interconnectPartnerCode = interconnectPartnerCode;
    }
    public String getPartnerName() {
        return partnerName;
    }

    public void setPartnerName(String partnerName) {
        this.partnerName = partnerName;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (interconnectPartnerId != null ? interconnectPartnerId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof InterconnectPartner)) {
            return false;
        }
        InterconnectPartner other = (InterconnectPartner) object;
        if ((this.interconnectPartnerId == null && other.interconnectPartnerId != null) || (this.interconnectPartnerId != null && !this.interconnectPartnerId.equals(other.interconnectPartnerId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.bm.db.model.InterconnectPartner[ interconnectPartnerId=" + interconnectPartnerId + " ]";
    }
    
}
