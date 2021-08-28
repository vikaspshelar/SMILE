/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.db.model;

import java.io.Serializable;
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
@Table(name = "unit_credit_specification")
@NamedQueries({
    @NamedQuery(name = "UnitCreditSpecification.findAll", query = "SELECT u FROM UnitCreditSpecification u")})
public class UnitCreditSpecification implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @NotNull
    @Column(name = "UNIT_CREDIT_SPECIFICATION_ID")
    private Integer unitCreditSpecificationId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "UNIT_CREDIT_NAME")
    private String unitCreditName;
    @Basic(optional = false)
    @NotNull
    @Column(name = "UNIT_CREDIT_DESCRIPTION")
    private String unitCreditDescription;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PRICE_CENTS")
    private long priceCents;
    @Basic(optional = false)
    @NotNull
    @Column(name = "AVAILABLE_FROM")
    @Temporal(TemporalType.TIMESTAMP)
    private Date availableFrom;
    @Basic(optional = false)
    @NotNull
    @Column(name = "AVAILABLE_TO")
    @Temporal(TemporalType.TIMESTAMP)
    private Date availableTo;
    @Basic(optional = false)
    @NotNull
    @Column(name = "VALIDITY_DAYS")
    private int validityDays;
    @Basic(optional = false)
    @NotNull
    @Column(name = "USABLE_DAYS")
    private int usableDays;
    @Basic(optional = false)
    @NotNull
    @Column(name = "UNITS")
    private long units;
    @Basic(optional = false)
    @NotNull
    @Column(name = "UNIT_TYPE")
    private String unitType;
    @Basic(optional = false)
    @NotNull
    @Column(name = "FILTER_CLASS")
    private String filterClass;
    @Basic(optional = false)
    @NotNull
    @Column(name = "WRAPPER_CLASS")
    private String wrapperClass;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PRIORITY")
    private int priority;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CONFIGURATION")
    private String configuration;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ITEM_NUMBER")
    private String itemNumber;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PURCHASE_ROLES")
    private String purchaseRoles;
    
    
    public UnitCreditSpecification() {
    }

    public UnitCreditSpecification(Integer unitCreditSpecificationId) {
        this.unitCreditSpecificationId = unitCreditSpecificationId;
    }

    public UnitCreditSpecification(Integer unitCreditSpecificationId, String unitCreditName, String unitCreditDescription, long priceCents, Date availableFrom, Date availableTo, int validityDays, int units) {
        this.unitCreditSpecificationId = unitCreditSpecificationId;
        this.unitCreditName = unitCreditName;
        this.unitCreditDescription = unitCreditDescription;
        this.priceCents = priceCents;
        this.availableFrom = availableFrom;
        this.availableTo = availableTo;
        this.validityDays = validityDays;
        this.units = units;
    }

    public Integer getUnitCreditSpecificationId() {
        return unitCreditSpecificationId;
    }

    public void setUnitCreditSpecificationId(Integer unitCreditSpecificationId) {
        this.unitCreditSpecificationId = unitCreditSpecificationId;
    }

    public String getUnitCreditName() {
        return unitCreditName;
    }

    public void setUnitCreditName(String unitCreditName) {
        this.unitCreditName = unitCreditName;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }
    
    public String getUnitType() {
        return unitType;
    }

    public void setUnitType(String unitType) {
        this.unitType = unitType;
    }
    
    public String getUnitCreditDescription() {
        return unitCreditDescription;
    }

    public void setUnitCreditDescription(String unitCreditDescription) {
        this.unitCreditDescription = unitCreditDescription;
    }

    public long getPriceCents() {
        return priceCents;
    }

    public void setPriceCents(long priceCents) {
        this.priceCents = priceCents;
    }

    public Date getAvailableFrom() {
        return availableFrom;
    }

    public void setAvailableFrom(Date availableFrom) {
        this.availableFrom = availableFrom;
    }

    public Date getAvailableTo() {
        return availableTo;
    }

    public void setAvailableTo(Date availableTo) {
        this.availableTo = availableTo;
    }

    public int getValidityDays() {
        return validityDays;
    }

    public void setValidityDays(int validityDays) {
        this.validityDays = validityDays;
    }

    public int getUsableDays() {
        return usableDays;
    }

    public void setUsableDays(int usableDays) {
        this.usableDays = usableDays;
    }
    
    public String getWrapperClass() {
        return wrapperClass;
    }

    public void setWrapperClass(String wrapperClass) {
        this.wrapperClass = wrapperClass;
    }

    
    public long getUnits() {
        return units;
    }

    public void setUnits(long units) {
        this.units = units;
    }

    public String getItemNumber() {
        return itemNumber;
    }

    public void setItemNumber(String itemNumber) {
        this.itemNumber = itemNumber;
    }

    public String getPurchaseRoles() {
        return purchaseRoles;
    }

    public void setPurchaseRoles(String purchaseRoles) {
        this.purchaseRoles = purchaseRoles;
    }
    
    

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (unitCreditSpecificationId != null ? unitCreditSpecificationId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof UnitCreditSpecification)) {
            return false;
        }
        UnitCreditSpecification other = (UnitCreditSpecification) object;
        if ((this.unitCreditSpecificationId == null && other.unitCreditSpecificationId != null) || (this.unitCreditSpecificationId != null && !this.unitCreditSpecificationId.equals(other.unitCreditSpecificationId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.bm.db.model.UnitCreditSpecification[ unitCreditSpecificationId=" + unitCreditSpecificationId + " ]";
    }

    public String getFilterClass() {
        return filterClass;
    }

    public void setFilterClass(String filterClass) {
        this.filterClass = filterClass;
    }
    
    
    
}
