/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.db.model;

import java.io.Serializable;
import java.math.BigDecimal;
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
@Table(name = "bestprefix_sms_rate")
@NamedQueries({
    @NamedQuery(name = "BestprefixSMSRate.findAll", query = "SELECT b FROM BestprefixSMSRate b")})
public class BestprefixSMSRate implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "DESTINATION")
    private String destination;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CENTS_PER_PART")
    private BigDecimal centsPerPart;

    public BestprefixSMSRate() {
    }

    public BestprefixSMSRate(String destination) {
        this.destination = destination;
    }

    public BestprefixSMSRate(String destination, BigDecimal centsPerPart) {
        this.destination = destination;
        this.centsPerPart = centsPerPart;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public BigDecimal getCentsPerPart() {
        return centsPerPart;
    }

    public void setCentsPerPart(BigDecimal centsPerPart) {
        this.centsPerPart = centsPerPart;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (destination != null ? destination.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof BestprefixSMSRate)) {
            return false;
        }
        BestprefixSMSRate other = (BestprefixSMSRate) object;
        if ((this.destination == null && other.destination != null) || (this.destination != null && !this.destination.equals(other.destination))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.bm.db.model.BestprefixSMSRate[ destination=" + destination + " ]";
    }
    
}
