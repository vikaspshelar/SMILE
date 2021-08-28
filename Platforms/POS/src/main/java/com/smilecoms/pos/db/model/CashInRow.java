/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos.db.model;

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
import javax.validation.constraints.Size;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "cash_in_row")
@NamedQueries({
    @NamedQuery(name = "CashInRow.findAll", query = "SELECT c FROM CashInRow c")})
public class CashInRow implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "CASH_IN_ROW_ID")
    private Integer cashInRowId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SALE_ID")
    private int saleId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CASH_IN_ID")
    private int cashInId;

    public CashInRow() {
    }

    public CashInRow(Integer cashInRowId) {
        this.cashInRowId = cashInRowId;
    }

    public CashInRow(Integer cashInRowId, int cashInId) {
        this.cashInRowId = cashInRowId;
        this.cashInId = cashInId;
    }

    public Integer getCashInRowId() {
        return cashInRowId;
    }

    public void setCashInRowId(Integer cashInRowId) {
        this.cashInRowId = cashInRowId;
    }

    public int getSaleId() {
        return saleId;
    }

    public void setSaleId(int saleId) {
        this.saleId = saleId;
    }


    public int getCashInId() {
        return cashInId;
    }

    public void setCashInId(int cashInId) {
        this.cashInId = cashInId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (cashInRowId != null ? cashInRowId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CashInRow)) {
            return false;
        }
        CashInRow other = (CashInRow) object;
        if ((this.cashInRowId == null && other.cashInRowId != null) || (this.cashInRowId != null && !this.cashInRowId.equals(other.cashInRowId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.pos.db.model.CashInRow[ cashInRowId=" + cashInRowId + " ]";
    }
    
}
