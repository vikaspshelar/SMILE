/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.cm.db.model;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Embeddable
public class UnitCreditServiceMappingPK implements Serializable {

    @Basic(optional = false)
    @NotNull
    @Column(name = "UNIT_CREDIT_SPECIFICATION_ID")
    private int unitCreditSpecificationId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SERVICE_SPECIFICATION_ID")
    private int serviceSpecificationId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PRODUCT_SPECIFICATION_ID")
    private int productSpecificationId;

    public UnitCreditServiceMappingPK() {
    }

    public UnitCreditServiceMappingPK(int unitCreditSpecificationId, int serviceSpecificationId, int productSpecificationId) {
        this.unitCreditSpecificationId = unitCreditSpecificationId;
        this.serviceSpecificationId = serviceSpecificationId;
        this.productSpecificationId = productSpecificationId;
    }

    public int getUnitCreditSpecificationId() {
        return unitCreditSpecificationId;
    }

    public void setUnitCreditSpecificationId(int unitCreditSpecificationId) {
        this.unitCreditSpecificationId = unitCreditSpecificationId;
    }

    public int getServiceSpecificationId() {
        return serviceSpecificationId;
    }

    public void setServiceSpecificationId(int serviceSpecificationId) {
        this.serviceSpecificationId = serviceSpecificationId;
    }

    public int getProductSpecificationId() {
        return productSpecificationId;
    }

    public void setProductSpecificationId(int productSpecificationId) {
        this.productSpecificationId = productSpecificationId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (int) unitCreditSpecificationId;
        hash += (int) serviceSpecificationId;
        hash += (int) productSpecificationId;
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof UnitCreditServiceMappingPK)) {
            return false;
        }
        UnitCreditServiceMappingPK other = (UnitCreditServiceMappingPK) object;
        if (this.unitCreditSpecificationId != other.unitCreditSpecificationId) {
            return false;
        }
        if (this.serviceSpecificationId != other.serviceSpecificationId) {
            return false;
        }
        if (this.productSpecificationId != other.productSpecificationId) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.cm.db.model.UnitCreditServiceMappingPK[ unitCreditSpecificationId=" + unitCreditSpecificationId + ", serviceSpecificationId=" + serviceSpecificationId + ", productSpecificationId=" + productSpecificationId + " ]";
    }
    
}
