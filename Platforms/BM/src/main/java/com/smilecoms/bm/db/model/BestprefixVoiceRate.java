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
@Table(name = "bestprefix_voice_rate")
@NamedQueries({
    @NamedQuery(name = "BestprefixVoiceRate.findAll", query = "SELECT b FROM BestprefixVoiceRate b")})
public class BestprefixVoiceRate implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "DESTINATION")
    private String destination;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Column(name = "CENTS_PER_SECOND")
    private BigDecimal centsPerSecond;

    public BestprefixVoiceRate() {
    }

    public BestprefixVoiceRate(String destination) {
        this.destination = destination;
    }

    public BestprefixVoiceRate(String destination, BigDecimal centsPerSecond) {
        this.destination = destination;
        this.centsPerSecond = centsPerSecond;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public BigDecimal getCentsPerSecond() {
        return centsPerSecond;
    }

    public void setCentsPerSecond(BigDecimal centsPerSecond) {
        this.centsPerSecond = centsPerSecond;
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
        if (!(object instanceof BestprefixVoiceRate)) {
            return false;
        }
        BestprefixVoiceRate other = (BestprefixVoiceRate) object;
        if ((this.destination == null && other.destination != null) || (this.destination != null && !this.destination.equals(other.destination))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.bm.db.model.BestprefixVoiceRate[ destination=" + destination + " ]";
    }
    
}
