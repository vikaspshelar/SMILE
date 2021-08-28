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
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "sale_return_row")
public class SaleReturnRow implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SALE_RETURN_ROW_ID")
    private Integer saleReturnRowId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SALE_RETURN_ID")
    private int saleReturnId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SALE_ROW_ID")
    private int saleRowId;
    @Column(name = "RETURNED_QUANTITY")
    private long returnedQuantity;

    public SaleReturnRow() {
    }

    public SaleReturnRow(Integer saleReturnRowId) {
        this.saleReturnRowId = saleReturnRowId;
    }

    public int getSaleRowId() {
        return saleRowId;
    }

    public void setSaleRowId(int saleRowId) {
        this.saleRowId = saleRowId;
    }

    public Integer getSaleReturnRowId() {
        return saleReturnRowId;
    }

    public void setSaleReturnRowId(Integer saleReturnRowId) {
        this.saleReturnRowId = saleReturnRowId;
    }

    public int getSaleReturnId() {
        return saleReturnId;
    }

    public void setSaleReturnId(int saleReturnId) {
        this.saleReturnId = saleReturnId;
    }

    public long getReturnedQuantity() {
        return returnedQuantity;
    }

    public void setReturnedQuantity(long returnedQuantity) {
        this.returnedQuantity = returnedQuantity;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (saleReturnRowId != null ? saleReturnRowId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof SaleReturnRow)) {
            return false;
        }
        SaleReturnRow other = (SaleReturnRow) object;
        if ((this.saleReturnRowId == null && other.saleReturnRowId != null) || (this.saleReturnRowId != null && !this.saleReturnRowId.equals(other.saleReturnRowId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.pos.db.model.SaleReturnRow[ saleReturnRowId=" + saleReturnRowId + " ]";
    }
}
