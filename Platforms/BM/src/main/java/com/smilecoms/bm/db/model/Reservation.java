/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.db.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "reservation")
@NamedQueries({
    @NamedQuery(name = "Reservation.findAll", query = "SELECT r FROM Reservation r")})
public class Reservation implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected ReservationPK reservationPK;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Column(name = "AMOUNT_CENTS")
    private BigDecimal amountCents;
    @Column(name = "AMOUNT_UNIT_CREDITS")
    private BigDecimal amountUnitCredits;
    @Basic(optional = false)
    @NotNull
    @Column(name = "RESERVATION_EVENT_TIMESTAMP")
    @Temporal(TemporalType.TIMESTAMP)
    private Date reservationEventTimestamp;
    @Column(name = "RESERVATION_EXPIRY_TIMESTAMP")
    @Temporal(TemporalType.TIMESTAMP)
    private Date reservationExpiryTimestamp;
    @Lob
    @Column(name = "REQUEST")
    private byte[] request;

    public Reservation() {
    }

    public Reservation(ReservationPK reservationPK) {
        this.reservationPK = reservationPK;
    }

    public Reservation(ReservationPK reservationPK, BigDecimal amountCents, Date reservationEventTimestamp) {
        this.reservationPK = reservationPK;
        this.amountCents = amountCents;
        this.reservationEventTimestamp = reservationEventTimestamp;
    }

    public Reservation(long accountId, String sessionId, int unitCreditInstanceId) {
        this.reservationPK = new ReservationPK(accountId, sessionId, unitCreditInstanceId);
    }

    public ReservationPK getReservationPK() {
        return reservationPK;
    }

    public void setReservationPK(ReservationPK reservationPK) {
        this.reservationPK = reservationPK;
    }

    public BigDecimal getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(BigDecimal amountCents) {
        this.amountCents = amountCents;
    }

    public BigDecimal getAmountUnitCredits() {
        return amountUnitCredits;
    }

    public void setAmountUnitCredits(BigDecimal amountUnitCredits) {
        this.amountUnitCredits = amountUnitCredits;
    }

    public Date getReservationEventTimestamp() {
        return reservationEventTimestamp;
    }

    public void setReservationEventTimestamp(Date reservationEventTimestamp) {
        this.reservationEventTimestamp = reservationEventTimestamp;
    }

    public Date getReservationExpiryTimestamp() {
        return reservationExpiryTimestamp;
    }

    public void setReservationExpiryTimestamp(Date reservationExpiryTimestamp) {
        this.reservationExpiryTimestamp = reservationExpiryTimestamp;
    }

    public byte[] getRequest() {
        return request;
    }

    public void setRequest(byte[] request) {
        this.request = request;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (reservationPK != null ? reservationPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Reservation)) {
            return false;
        }
        Reservation other = (Reservation) object;
        if ((this.reservationPK == null && other.reservationPK != null) || (this.reservationPK != null && !this.reservationPK.equals(other.reservationPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.bm.db.model.Reservation[ reservationPK=" + reservationPK + " ]";
    }
    
}
