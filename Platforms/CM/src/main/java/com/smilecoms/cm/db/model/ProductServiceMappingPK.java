/*
 * To change this template, choose Tools | Templates
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
 * @author mukosi
 */
@Embeddable
public class ProductServiceMappingPK implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Column(name = "PRODUCT_SPECIFICATION_ID")
    private int productSpecificationId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SERVICE_SPECIFICATION_ID")
    private int serviceSpecificationId;

    public ProductServiceMappingPK() {
    }

    public ProductServiceMappingPK(int productSpecificationId, int serviceSpecificationId) {
        this.productSpecificationId = productSpecificationId;
        this.serviceSpecificationId = serviceSpecificationId;
    }

    public int getProductSpecificationId() {
        return productSpecificationId;
    }

    public void setProductSpecificationId(int productSpecificationId) {
        this.productSpecificationId = productSpecificationId;
    }

    public int getServiceSpecificationId() {
        return serviceSpecificationId;
    }

    public void setServiceSpecificationId(int serviceSpecificationId) {
        this.serviceSpecificationId = serviceSpecificationId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (int) productSpecificationId;
        hash += (int) serviceSpecificationId;
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ProductServiceMappingPK)) {
            return false;
        }
        ProductServiceMappingPK other = (ProductServiceMappingPK) object;
        if (this.productSpecificationId != other.productSpecificationId) {
            return false;
        }
        if (this.serviceSpecificationId != other.serviceSpecificationId) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.cm.db.model.ProductServiceMappingPK[ productSpecificationId=" + productSpecificationId + ", serviceSpecificationId=" + serviceSpecificationId + " ]";
    }
    
}
