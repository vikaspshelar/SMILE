/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos.db.model;

import java.io.Serializable;
import java.math.BigDecimal;
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

/**
 *
 * @author paul
 */
@Entity
@Table(name = "sale_row")
@NamedQueries({
    @NamedQuery(name = "SaleRow.findAll", query = "SELECT s FROM SaleRow s")})
public class SaleRow implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SALE_ROW_ID")
    private Integer saleRowId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SALE_ID")
    private int saleId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SERIAL_NUMBER")
    private String serialNumber;
    @Basic(optional = false)
    @NotNull
    @Column(name = "DESCRIPTION")
    private String description;
    @Column(name = "COMMENT")
    private String comment;
    @Basic(optional = false)
    @NotNull
    @Column(name = "UNIT_PRICE_CENTS_EXCL")
    private BigDecimal unitPriceCentsExcl;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ITEM_NUMBER")
    private String itemNumber;
    @Basic(optional = false)
    @NotNull
    @Column(name = "UNIT_PRICE_CENTS_INCL")
    private BigDecimal unitPriceCentsIncl;
    @Basic(optional = false)
    @NotNull
    @Column(name = "WAREHOUSE_ID")
    private String warehouseId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "LINE_NUMBER")
    private int lineNumber;
    @Basic(optional = false)
    @NotNull
    @Column(name = "QUANTITY")
    private long quantity;
    @Basic(optional = false)
    @NotNull
    @Column(name = "TOTAL_DISCOUNT_ON_INCL_CENTS")
    private BigDecimal totalDiscountOnInclCents;
    @Basic(optional = false)
    @NotNull
    @Column(name = "TOTAL_DISCOUNT_ON_EXCL_CENTS")
    private BigDecimal totalDiscountOnExclCents;
    @Basic(optional = false)
    @NotNull
    @Column(name = "TOTAL_CENTS_INCL")
    private BigDecimal totalCentsIncl;
    @Basic(optional = false)
    @NotNull
    @Column(name = "TOTAL_CENTS_EXCL")
    private BigDecimal totalCentsExcl;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PARENT_SALE_ROW_ID")
    private int parentSaleRowId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PROVISIONING_DATA")
    private String provisioningData;
    @Column(name = "HELD_BY_ORGANISATION_ID")
    private Integer heldByOrganisationId;

    public SaleRow() {
    }

    public SaleRow(Integer saleRowId) {
        this.saleRowId = saleRowId;
    }

    public SaleRow(Integer saleRowId, int saleId, String serialNumber, String description, BigDecimal unitPriceCentsExcl, String itemNumber, BigDecimal unitPriceCentsIncl, String warehouseId, int lineNumber, int quantity, BigDecimal totalDiscountOnInclCents, BigDecimal totalDiscountOnExclCents, BigDecimal totalCentsIncl, BigDecimal totalCentsExcl) {
        this.saleRowId = saleRowId;
        this.saleId = saleId;
        this.serialNumber = serialNumber;
        this.description = description;
        this.unitPriceCentsExcl = unitPriceCentsExcl;
        this.itemNumber = itemNumber;
        this.unitPriceCentsIncl = unitPriceCentsIncl;
        this.warehouseId = warehouseId;
        this.lineNumber = lineNumber;
        this.quantity = quantity;
        this.totalDiscountOnInclCents = totalDiscountOnInclCents;
        this.totalDiscountOnExclCents = totalDiscountOnExclCents;
        this.totalCentsIncl = totalCentsIncl;
        this.totalCentsExcl = totalCentsExcl;
    }

    public String getProvisioningData() {
        return provisioningData;
    }

    public void setProvisioningData(String provisioningData) {
        this.provisioningData = provisioningData;
    }
    
    public Integer getSaleRowId() {
        return saleRowId;
    }

    public void setSaleRowId(Integer saleRowId) {
        this.saleRowId = saleRowId;
    }

    public int getSaleId() {
        return saleId;
    }

    public void setSaleId(int saleId) {
        this.saleId = saleId;
    }

    public Integer getHeldByOrganisationId() {
        return heldByOrganisationId;
    }

    public void setHeldByOrganisationId(Integer heldByOrganisationId) {
        this.heldByOrganisationId = heldByOrganisationId;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
    
    public BigDecimal getUnitPriceCentsExcl() {
        return unitPriceCentsExcl;
    }

    public void setUnitPriceCentsExcl(BigDecimal unitPriceCentsExcl) {
        this.unitPriceCentsExcl = unitPriceCentsExcl;
    }

    public String getItemNumber() {
        return itemNumber;
    }

    public void setItemNumber(String itemNumber) {
        this.itemNumber = itemNumber;
    }

    public BigDecimal getUnitPriceCentsIncl() {
        return unitPriceCentsIncl;
    }

    public void setUnitPriceCentsIncl(BigDecimal unitPriceCentsIncl) {
        this.unitPriceCentsIncl = unitPriceCentsIncl;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getTotalDiscountOnInclCents() {
        return totalDiscountOnInclCents;
    }

    public void setTotalDiscountOnInclCents(BigDecimal totalDiscountOnInclCents) {
        this.totalDiscountOnInclCents = totalDiscountOnInclCents;
    }

    public BigDecimal getTotalDiscountOnExclCents() {
        return totalDiscountOnExclCents;
    }

    public void setTotalDiscountOnExclCents(BigDecimal totalDiscountOnExclCents) {
        this.totalDiscountOnExclCents = totalDiscountOnExclCents;
    }

    public BigDecimal getTotalCentsIncl() {
        return totalCentsIncl;
    }

    public void setTotalCentsIncl(BigDecimal totalCentsIncl) {
        this.totalCentsIncl = totalCentsIncl;
    }

    public BigDecimal getTotalCentsExcl() {
        return totalCentsExcl;
    }

    public void setTotalCentsExcl(BigDecimal totalCentsExcl) {
        this.totalCentsExcl = totalCentsExcl;
    }

    public int getParentSaleRowId() {
        return parentSaleRowId;
    }

    public void setParentSaleRowId(int parentSaleRowId) {
        this.parentSaleRowId = parentSaleRowId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (saleRowId != null ? saleRowId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof SaleRow)) {
            return false;
        }
        SaleRow other = (SaleRow) object;
        if ((this.saleRowId == null && other.saleRowId != null) || (this.saleRowId != null && !this.saleRowId.equals(other.saleRowId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.pos.db.model.SaleRow[ saleRowId=" + saleRowId + " ]";
    }
}
