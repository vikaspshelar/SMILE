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
@Table(name = "unit_credit_instance")
@NamedQueries({
    @NamedQuery(name = "UnitCreditInstance.findAll", query = "SELECT u FROM UnitCreditInstance u")})
public class UnitCreditInstance implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "UNIT_CREDIT_INSTANCE_ID")
    private Integer unitCreditInstanceId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "UNIT_CREDIT_SPECIFICATION_ID")
    private int unitCreditSpecificationId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ACCOUNT_ID")
    private long accountId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PURCHASE_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date purchaseDate;
    @Basic(optional = false)
    @NotNull
    @Column(name = "EXPIRY_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date expiryDate;
    @Basic(optional = false)
    @NotNull
    @Column(name = "START_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startDate;
    @Column(name = "END_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endDate;
    @Basic(optional = false)
    @NotNull
    @Column(name = "UNITS_REMAINING")
    private BigDecimal unitsRemaining;
    @Column(name = "REVENUE_CENTS_PER_UNIT")
    private BigDecimal revenueCentsPerUnit;
    @Column(name = "BASELINE_CENTS_PER_UNIT")
    private BigDecimal baselineCentsPerUnit;
    @Column(name = "FREE_CENTS_PER_UNIT")
    private BigDecimal freeCentsPerUnit;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PRODUCT_INSTANCE_ID")
    private int productInstanceId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "UNITS_AT_START")
    private BigDecimal unitsAtStart;
    // The actual amount paid by the customer after discounting
    @Column(name = "POS_CENTS_CHARGED")
    private BigDecimal POSCentsCharged;
    @Column(name = "POS_CENTS_DISCOUNT")
    private BigDecimal POSCentsDiscount;
    @Basic(optional = false)
    @Column(name = "INFO")
    private String info;
    @Column(name = "EXT_TXID")
    private String extTxid;
    @Column(name = "FIRST_USED_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date firstUsedDate;
    @Column(name = "LAST_USED_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUsedDate;
    @Column(name = "CROSSOVER_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date crossoverDate;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PROVISIONED_BY_CUSTOMER_PROFILE_ID")
    private int provisionedByCustomerProfileId;
    @Column(name = "SALE_ROW_ID")
    private int saleRowId;
    @Column(name = "AUX_COUNTER_1")
    private BigDecimal auxCounter1;
    @Column(name = "AUX_COUNTER_2")
    private BigDecimal auxCounter2;
    
    // These date are inclusive
    @Column(name = "REVENUE_FIRST_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date revenueFirstDate;
    @Column(name = "REVENUE_LAST_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date revenueLastDate;
    @Column(name = "REVENUE_CENTS_PER_DAY")
    private BigDecimal revenueCentsPerDay;

    public UnitCreditInstance() {
    }

    public UnitCreditInstance(Integer unitCreditInstanceId) {
        this.unitCreditInstanceId = unitCreditInstanceId;
    }

    public UnitCreditInstance(Integer unitCreditInstanceId, int unitCreditSpecificationId, long accountId, Date purchaseDate, Date expiryDate, BigDecimal unitsRemaining) {
        this.unitCreditInstanceId = unitCreditInstanceId;
        this.unitCreditSpecificationId = unitCreditSpecificationId;
        this.accountId = accountId;
        this.purchaseDate = purchaseDate;
        this.expiryDate = expiryDate;
        this.unitsRemaining = unitsRemaining;
    }

    public int getProvisionedByCustomerProfileId() {
        return provisionedByCustomerProfileId;
    }

    public void setProvisionedByCustomerProfileId(int provisionedByCustomerProfileId) {
        this.provisionedByCustomerProfileId = provisionedByCustomerProfileId;
    }

    public Integer getUnitCreditInstanceId() {
        return unitCreditInstanceId;
    }

    public void setUnitCreditInstanceId(Integer unitCreditInstanceId) {
        this.unitCreditInstanceId = unitCreditInstanceId;
    }

    public int getUnitCreditSpecificationId() {
        return unitCreditSpecificationId;
    }

    public void setUnitCreditSpecificationId(int unitCreditSpecificationId) {
        this.unitCreditSpecificationId = unitCreditSpecificationId;
    }

    public int getSaleRowId() {
        return saleRowId;
    }

    public void setSaleRowId(int saleRowId) {
        this.saleRowId = saleRowId;
    }

    public String getExtTxid() {
        return extTxid;
    }

    public void setExtTxid(String extTxid) {
        this.extTxid = extTxid;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getRevenueCentsPerUnit() {
        return revenueCentsPerUnit;
    }

    public void setRevenueCentsPerUnit(BigDecimal revenueCentsPerUnit) {
        this.revenueCentsPerUnit = revenueCentsPerUnit;
    }

    public BigDecimal getBaselineCentsPerUnit() {
        return baselineCentsPerUnit;
    }

    public void setBaselineCentsPerUnit(BigDecimal baselineCentsPerUnit) {
        this.baselineCentsPerUnit = baselineCentsPerUnit;
    }

    public Date getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(Date purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }
    
    public BigDecimal getUnitsRemaining() {
        return unitsRemaining;
    }

    public void setUnitsRemaining(BigDecimal unitsRemaining) {
        this.unitsRemaining = unitsRemaining;
    }

    public int getProductInstanceId() {
        return productInstanceId;
    }

    public void setProductInstanceId(int productInstanceId) {
        this.productInstanceId = productInstanceId;
    }
    
    public BigDecimal getFreeCentsPerUnit() {
        return freeCentsPerUnit;
    }

    public void setFreeCentsPerUnit(BigDecimal freeCentsPerUnit) {
        this.freeCentsPerUnit = freeCentsPerUnit;
    }

    public BigDecimal getUnitsAtStart() {
        return unitsAtStart;
    }

    public void setUnitsAtStart(BigDecimal unitsAtStart) {
        this.unitsAtStart = unitsAtStart;
    }

    public BigDecimal getPOSCentsCharged() {
        return POSCentsCharged;
    }

    public void setPOSCentsCharged(BigDecimal POSCentsCharged) {
        this.POSCentsCharged = POSCentsCharged;
    }

    public BigDecimal getPOSCentsDiscount() {
        return POSCentsDiscount;
    }

    public void setPOSCentsDiscount(BigDecimal POSCentsDiscount) {
        this.POSCentsDiscount = POSCentsDiscount;
    }
    
    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public Date getFirstUsedDate() {
        return firstUsedDate;
    }

    public void setFirstUsedDate(Date firstUsedDate) {
        this.firstUsedDate = firstUsedDate;
    }

    public Date getLastUsedDate() {
        return lastUsedDate;
    }

    public void setLastUsedDate(Date lastUsedDate) {
        this.lastUsedDate = lastUsedDate;
    }

    public Date getCrossoverDate() {
        return crossoverDate;
    }

    public void setCrossoverDate(Date crossoverDate) {
        this.crossoverDate = crossoverDate;
    }

    public BigDecimal getAuxCounter1() {
        return auxCounter1;
    }

    public void setAuxCounter1(BigDecimal auxCounter1) {
        this.auxCounter1 = auxCounter1;
    }

    public BigDecimal getAuxCounter2() {
        return auxCounter2;
    }

    public void setAuxCounter2(BigDecimal auxCounter2) {
        this.auxCounter2 = auxCounter2;
    }

    public Date getRevenueFirstDate() {
        return revenueFirstDate;
    }

    public void setRevenueFirstDate(Date revenueFirstDate) {
        this.revenueFirstDate = revenueFirstDate;
    }

    public Date getRevenueLastDate() {
        return revenueLastDate;
    }

    public void setRevenueLastDate(Date revenueLastDate) {
        this.revenueLastDate = revenueLastDate;
    }

    public BigDecimal getRevenueCentsPerDay() {
        return revenueCentsPerDay;
    }

    public void setRevenueCentsPerDay(BigDecimal revenueCentsPerDay) {
        this.revenueCentsPerDay = revenueCentsPerDay;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (unitCreditInstanceId != null ? unitCreditInstanceId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof UnitCreditInstance)) {
            return false;
        }
        UnitCreditInstance other = (UnitCreditInstance) object;
        if ((this.unitCreditInstanceId == null && other.unitCreditInstanceId != null) || (this.unitCreditInstanceId != null && !this.unitCreditInstanceId.equals(other.unitCreditInstanceId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.bm.db.model.UnitCreditInstance[ unitCreditInstanceId=" + unitCreditInstanceId + " ]";
    }
}
