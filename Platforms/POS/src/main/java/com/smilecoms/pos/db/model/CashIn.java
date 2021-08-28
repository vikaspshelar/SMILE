/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos.db.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
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
@Table(name = "cash_in")
@NamedQueries({
    @NamedQuery(name = "CashIn.findAll", query = "SELECT c FROM CashIn c")})
public class CashIn implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "CASH_IN_ID")
    private Integer cashInId;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Column(name = "CASH_RECEIPTED_IN_CENTS")
    private BigDecimal cashReceiptedInCents;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CASH_REQUIRED_IN_CENTS")
    private BigDecimal cashRequiredInCents;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SALES_PERSON_CUSTOMER_ID")
    private int salesPersonCustomerId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SALES_ADMINISTRATOR_CUSTOMER_ID")
    private int salesAdministratorCustomerId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CASH_IN_DATE_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date cashInDateTime;
    @Basic(optional = false)
    @NotNull
    @Column(name = "EXT_TXID")
    private String extTxId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CASH_IN_TYPE")
    private String cashInType;
    @Basic(optional = false)
    @NotNull
    @Column(name = "STATUS")
    private String status;
    @Column(name = "BANK_NAME")
    private String bankName;

    public CashIn() {
    }

    public CashIn(Integer cashInId) {
        this.cashInId = cashInId;
    }

    public String getExtTxId() {
        return extTxId;
    }

    public void setExtTxId(String extTxId) {
        this.extTxId = extTxId;
    }
    
    

    public CashIn(Integer cashInId, BigDecimal cashReceiptedInCents, BigDecimal cashRequiredInCents, int salesPersonCustomerId, int salesAdministratorCustomerId, Date cashInDateTime) {
        this.cashInId = cashInId;
        this.cashReceiptedInCents = cashReceiptedInCents;
        this.cashRequiredInCents = cashRequiredInCents;
        this.salesPersonCustomerId = salesPersonCustomerId;
        this.salesAdministratorCustomerId = salesAdministratorCustomerId;
        this.cashInDateTime = cashInDateTime;
    }

    public Integer getCashInId() {
        return cashInId;
    }

    public void setCashInId(Integer cashInId) {
        this.cashInId = cashInId;
    }

    public BigDecimal getCashReceiptedInCents() {
        return cashReceiptedInCents;
    }

    public void setCashReceiptedInCents(BigDecimal cashReceiptedInCents) {
        this.cashReceiptedInCents = cashReceiptedInCents;
    }

    public BigDecimal getCashRequiredInCents() {
        return cashRequiredInCents;
    }

    public void setCashRequiredInCents(BigDecimal cashRequiredInCents) {
        this.cashRequiredInCents = cashRequiredInCents;
    }

    public int getSalesPersonCustomerId() {
        return salesPersonCustomerId;
    }

    public void setSalesPersonCustomerId(int salesPersonCustomerId) {
        this.salesPersonCustomerId = salesPersonCustomerId;
    }

    public int getSalesAdministratorCustomerId() {
        return salesAdministratorCustomerId;
    }

    public void setSalesAdministratorCustomerId(int salesAdministratorCustomerId) {
        this.salesAdministratorCustomerId = salesAdministratorCustomerId;
    }

    public Date getCashInDateTime() {
        return cashInDateTime;
    }

    public void setCashInDateTime(Date cashInDateTime) {
        this.cashInDateTime = cashInDateTime;
    }

    public String getCashInType() {
        return cashInType;
    }

    public void setCashInType(String cashInType) {
        this.cashInType = cashInType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bName) {
        this.bankName = bName;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (cashInId != null ? cashInId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CashIn)) {
            return false;
        }
        CashIn other = (CashIn) object;
        if ((this.cashInId == null && other.cashInId != null) || (this.cashInId != null && !this.cashInId.equals(other.cashInId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.pos.db.model.CashIn[ cashInId=" + cashInId + " ]";
    }
    
}
