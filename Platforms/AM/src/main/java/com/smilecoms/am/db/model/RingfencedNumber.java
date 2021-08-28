/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.am.db.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author mukosi
 */
@Entity
@Table(name = "ringfenced_number")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "RingfencedNumber.findAll", query = "SELECT r FROM RingfencedNumber r"),
    @NamedQuery(name = "RingfencedNumber.findByNumber", query = "SELECT r FROM RingfencedNumber r WHERE r.number = :number"),
    @NamedQuery(name = "RingfencedNumber.findByRingfenceRecipientId", query = "SELECT r FROM RingfencedNumber r WHERE r.ringfenceRecipientId = :ringfenceRecipientId"),
    @NamedQuery(name = "RingfencedNumber.findByRingfenceExpiryDate", query = "SELECT r FROM RingfencedNumber r WHERE r.ringfenceExpiryDate = :ringfenceExpiryDate")})
public class RingfencedNumber implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "NUMBER")
    private Long number;
    @Basic(optional = false)
    @NotNull
    @Column(name = "RINGFENCE_RECIPIENT_ID")
    private String ringfenceRecipientId;
    @Column(name = "RINGFENCE_EXPIRY_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date ringfenceExpiryDate;

    public RingfencedNumber() {
    }

    public RingfencedNumber(Long number) {
        this.number = number;
    }

    public RingfencedNumber(Long number, String ringfenceRecipientId) {
        this.number = number;
        this.ringfenceRecipientId = ringfenceRecipientId;
    }

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }

    public String getRingfenceRecipientId() {
        return ringfenceRecipientId;
    }

    public void setRingfenceRecipientId(String ringfenceRecipientId) {
        this.ringfenceRecipientId = ringfenceRecipientId;
    }

    public Date getRingfenceExpiryDate() {
        return ringfenceExpiryDate;
    }

    public void setRingfenceExpiryDate(Date ringfenceExpiryDate) {
        this.ringfenceExpiryDate = ringfenceExpiryDate;
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
        if (!(object instanceof RingfencedNumber)) {
            return false;
        }
        RingfencedNumber other = (RingfencedNumber) object;
        if ((this.number == null && other.number != null) || (this.number != null && !this.number.equals(other.number))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.am.db.model.RingfencedNumber[ number=" + number + " ]";
    }
    
}
