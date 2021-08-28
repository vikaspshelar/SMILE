/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pvs.db.model;

import com.smilecoms.pvs.db.op.DAO;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
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
@Table(name = "prepaid_strip")
@NamedQueries({
    @NamedQuery(name = "PrepaidStrip.findAll", query = "SELECT p FROM PrepaidStrip p")})
public class PrepaidStrip implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "PREPAID_STRIP_ID")
    private Integer prepaidStripId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ENCRYPTED_PIN_HEX")
    private String encryptedPINHex;
    @Basic(optional = false)
    @NotNull
    @Column(name = "STATUS")
    private String status;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Column(name = "VALUE_CENTS")
    private BigDecimal valueCents;
    @Column(name = "GENERATED_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date generatedDate;
    @Column(name = "EXPIRY_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date expiryDate;
    @Column(name = "PIN")
    private String PIN;
    @Column(name = "CHECKSUM")
    private String checksum;
    @Column(name = "INVOICE_DATA")
    private String invoiceData;
    @Column(name = "REDEMPTION_ACCOUNT_ID")
    private Long redemptionAccountId;
    @Column(name = "REDEMPTION_ACCOUNT_HISTORY_ID")
    private BigInteger redemptionAccountHistoryId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "UNIT_CREDIT_SPECIFICATION_ID")
    private Integer unitCreditSpecificationId;
    @Column(name = "ACCOUNT_ID")
    private Long accountId;

    public PrepaidStrip() {
    }

    public PrepaidStrip(Integer stripId) {
        this.prepaidStripId = stripId;
    }

    public PrepaidStrip(Integer stripId, String encryptedPINHex, String status, BigDecimal valueCents, Integer unitCreditSpecificationId) {
        this.prepaidStripId = stripId;
        this.encryptedPINHex = encryptedPINHex;
        this.status = status;
        this.valueCents = valueCents;
        this.unitCreditSpecificationId = unitCreditSpecificationId;
    }

    public String getPIN() {
        return PIN;
    }

    public void setPIN(String PIN) {
        this.PIN = PIN;
    }

    public String getInvoiceData() {
        return invoiceData;
    }

    public void setInvoiceData(String invoiceData) {
        this.invoiceData = invoiceData;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Integer getPrepaidStripId() {
        return prepaidStripId;
    }

    public void setPrepaidStripId(Integer stripId) {
        this.prepaidStripId = stripId;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getEncryptedPINHex() {
        return encryptedPINHex;
    }

    public void setEncryptedPINHex(String encryptedPINHex) {
        this.encryptedPINHex = encryptedPINHex;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) throws Exception {
        DAO.validateStatus(this.status, status);
        this.status = status;
    }

    public BigDecimal getValueCents() {
        return valueCents;
    }

    public void setValueCents(BigDecimal valueCents) {
        this.valueCents = valueCents;
    }

    public Date getGeneratedDate() {
        return generatedDate;
    }

    public void setGeneratedDate(Date generatedDate) {
        this.generatedDate = generatedDate;
    }

    public Long getRedemptionAccountId() {
        return redemptionAccountId;
    }

    public void setRedemptionAccountId(Long redemptionAccountId) {
        this.redemptionAccountId = redemptionAccountId;
    }

    public BigInteger getRedemptionAccountHistoryId() {
        return redemptionAccountHistoryId;
    }

    public void setRedemptionAccountHistoryId(BigInteger redemptionAccountHistoryId) {
        this.redemptionAccountHistoryId = redemptionAccountHistoryId;
    }

    public Integer getUnitCreditSpecificationId() {
        return unitCreditSpecificationId;
    }

    public void setUnitCreditSpecificationId(Integer unitCreditSpecificationId) {
        this.unitCreditSpecificationId = unitCreditSpecificationId;
    }
    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (prepaidStripId != null ? prepaidStripId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PrepaidStrip)) {
            return false;
        }
        PrepaidStrip other = (PrepaidStrip) object;
        if ((this.prepaidStripId == null && other.prepaidStripId != null) || (this.prepaidStripId != null && !this.prepaidStripId.equals(other.prepaidStripId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.pvs.db.model.PrepaidStrip[ stripId=" + prepaidStripId + " ]";
    }
    
}
