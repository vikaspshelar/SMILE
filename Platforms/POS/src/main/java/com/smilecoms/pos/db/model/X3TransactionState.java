/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos.db.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
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
@Table(name = "x3_transaction_state")
@NamedQueries({
    @NamedQuery(name = "X3TransactionState.findAll", query = "SELECT x FROM X3TransactionState x")})
public class X3TransactionState implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected X3TransactionStatePK x3TransactionStatePK;
    @Basic(optional = false)
    @NotNull
    @Column(name = "START_DATE_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startDateTime;
    @Column(name = "END_DATE_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endDateTime;
    @Basic(optional = false)
    @NotNull
    @Column(name = "STATUS")
    private String status;
    @Basic(optional = false)
    @NotNull
    @Column(name = "EXTRA_INFO")
    private String extraInfo;
    
    public X3TransactionState() {
    }

    public X3TransactionState(X3TransactionStatePK x3TransactionStatePK) {
        this.x3TransactionStatePK = x3TransactionStatePK;
    }

    public X3TransactionState(X3TransactionStatePK x3TransactionStatePK, Date startDateTime, String status, String extraInfo) {
        this.x3TransactionStatePK = x3TransactionStatePK;
        this.startDateTime = startDateTime;
        this.status = status;
        this.extraInfo = extraInfo;
    }

    public X3TransactionState(String transactionType, String tableName, int primaryKey) {
        this.x3TransactionStatePK = new X3TransactionStatePK(transactionType, tableName, primaryKey);
    }

    public X3TransactionStatePK getX3TransactionStatePK() {
        return x3TransactionStatePK;
    }

    public void setX3TransactionStatePK(X3TransactionStatePK x3TransactionStatePK) {
        this.x3TransactionStatePK = x3TransactionStatePK;
    }

    public Date getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(Date startDateTime) {
        this.startDateTime = startDateTime;
    }

    public Date getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(Date endDateTime) {
        this.endDateTime = endDateTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getExtraInfo() {
        return extraInfo;
    }

    public void setExtraInfo(String extraInfo) {
        this.extraInfo = extraInfo;
    }


    @Override
    public int hashCode() {
        int hash = 0;
        hash += (x3TransactionStatePK != null ? x3TransactionStatePK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof X3TransactionState)) {
            return false;
        }
        X3TransactionState other = (X3TransactionState) object;
        if ((this.x3TransactionStatePK == null && other.x3TransactionStatePK != null) || (this.x3TransactionStatePK != null && !this.x3TransactionStatePK.equals(other.x3TransactionStatePK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.pos.db.model.X3TransactionState[ x3TransactionStatePK=" + x3TransactionStatePK + " ]";
    }
    
}
