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

/**
 *
 * @author paul
 */
@Entity
@Table(name = "ported_number")
@NamedQueries({
    @NamedQuery(name = "PortedNumber.findAll", query = "SELECT p FROM PortedNumber p")})
public class PortedNumber implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "NUMBER")
    private Long number;
    @Basic(optional = false)
    @NotNull
    @Column(name = "INTERCONNECT_PARTNER_ID")
    private int interconnectPartnerId;

    public PortedNumber() {
    }

    public PortedNumber(Long number) {
        this.number = number;
    }

    public PortedNumber(Long number, int interconnectPartnerId) {
        this.number = number;
        this.interconnectPartnerId = interconnectPartnerId;
    }

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }

    public int getInterconnectPartnerId() {
        return interconnectPartnerId;
    }

    public void setInterconnectPartnerId(int interconnectPartnerId) {
        this.interconnectPartnerId = interconnectPartnerId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (number != null ? number.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PortedNumber)) {
            return false;
        }
        PortedNumber other = (PortedNumber) object;
        if ((this.number == null && other.number != null) || (this.number != null && !this.number.equals(other.number))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.bm.db.model.PortedNumber[ number=" + number + " ]";
    }
    
}
