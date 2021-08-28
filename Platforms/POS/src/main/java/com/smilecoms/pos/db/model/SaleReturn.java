/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos.db.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "sale_return")
public class SaleReturn implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SALE_RETURN_ID")
    private Integer saleReturnId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SALES_PERSON_CUSTOMER_ID")
    private int salesPersonCustomerId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SALE_ID")
    private int saleId;
    @Column(name = "SALE_RETURN_DATE_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date saleReturnDateTime;
    @Basic(optional = false)
    @NotNull
    @Column(name = "REASON_CODE")
    private String reasonCode;
    @Basic(optional = false)
    @NotNull
    @Column(name = "DESCRIPTION")
    private String description;
    @Basic(optional = false)
    @Lob
    @Column(name = "SALE_RETURN_PDF")
    private byte[] saleReturnPDF;
    @Basic(optional = false)
    @NotNull
    @Column(name = "RETURN_LOCATION")
    private String returnLocation;
    
    public SaleReturn() {
    }

    public SaleReturn(Integer returnId) {
        this.saleReturnId = returnId;
    }

    public Integer getSaleReturnId() {
        return saleReturnId;
    }

    public void setSaleReturnId(Integer saleReturnId) {
        this.saleReturnId = saleReturnId;
    }

    public int getSalesPersonCustomerId() {
        return salesPersonCustomerId;
    }

    public void setSalesPersonCustomerId(int salesPersonCustomerId) {
        this.salesPersonCustomerId = salesPersonCustomerId;
    }

    public int getSaleId() {
        return saleId;
    }

    public void setSaleId(int saleId) {
        this.saleId = saleId;
    }

    public Date getSaleReturnDateTime() {
        return saleReturnDateTime;
    }

    public void setSaleReturnDateTime(Date saleReturnDateTime) {
        this.saleReturnDateTime = saleReturnDateTime;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public byte[] getSaleReturnPDF() {
        return saleReturnPDF;
    }

    public void setSaleReturnPDF(byte[] saleReturnPDF) {
        this.saleReturnPDF = saleReturnPDF;
    }

    public String getReturnLocation() {
        return returnLocation;
    }

    public void setReturnLocation(String returnLocation) {
        this.returnLocation = returnLocation;
    }

    
    
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (saleReturnId != null ? saleReturnId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof SaleReturn)) {
            return false;
        }
        SaleReturn other = (SaleReturn) object;
        if ((this.saleReturnId == null && other.saleReturnId != null) || (this.saleReturnId != null && !this.saleReturnId.equals(other.saleReturnId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.pos.db.model.SaleReturn[ saleReturnId=" + saleReturnId + " ]";
    }
    
}
