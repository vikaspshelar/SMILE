/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.cm.db.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author mukosi
 */
@Entity
@Table(name = "product_service_mapping")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ProductServiceMapping.findAll", query = "SELECT p FROM ProductServiceMapping p"),
    @NamedQuery(name = "ProductServiceMapping.findByProductSpecificationId", query = "SELECT p FROM ProductServiceMapping p WHERE p.productServiceMappingPK.productSpecificationId = :productSpecificationId"),
    @NamedQuery(name = "ProductServiceMapping.findByServiceSpecificationId", query = "SELECT p FROM ProductServiceMapping p WHERE p.productServiceMappingPK.serviceSpecificationId = :serviceSpecificationId"),
    @NamedQuery(name = "ProductServiceMapping.findByMinServiceOccurences", query = "SELECT p FROM ProductServiceMapping p WHERE p.minServiceOccurences = :minServiceOccurences"),
    @NamedQuery(name = "ProductServiceMapping.findByMaxServiceOccurences", query = "SELECT p FROM ProductServiceMapping p WHERE p.maxServiceOccurences = :maxServiceOccurences")})
public class ProductServiceMapping implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected ProductServiceMappingPK productServiceMappingPK;
    @Basic(optional = false)
    @NotNull
    @Column(name = "MIN_SERVICE_OCCURENCES")
    private int minServiceOccurences;
    @Basic(optional = false)
    @NotNull
    @Column(name = "MAX_SERVICE_OCCURENCES")
    private int maxServiceOccurences;
    @JoinColumn(name = "SERVICE_SPECIFICATION_ID", referencedColumnName = "SERVICE_SPECIFICATION_ID", insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private ServiceSpecification serviceSpecification;
    @JoinColumn(name = "PRODUCT_SPECIFICATION_ID", referencedColumnName = "PRODUCT_SPECIFICATION_ID", insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private ProductSpecification productSpecification;
    @Basic(optional = false)
    @NotNull
    @Column(name = "RATE_PLAN_ID")
    private int ratePlanId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "GROUP_ID")
    private int groupId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PROVISION_ROLES")
    private String provisionRoles;
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
    

    public ProductServiceMapping() {
    }

    public ProductServiceMapping(ProductServiceMappingPK productServiceMappingPK) {
        this.productServiceMappingPK = productServiceMappingPK;
    }

    public ProductServiceMapping(ProductServiceMappingPK productServiceMappingPK, int minServiceOccurences, int maxServiceOccurences) {
        this.productServiceMappingPK = productServiceMappingPK;
        this.minServiceOccurences = minServiceOccurences;
        this.maxServiceOccurences = maxServiceOccurences;
    }

    public ProductServiceMapping(int productSpecificationId, int serviceSpecificationId) {
        this.productServiceMappingPK = new ProductServiceMappingPK(productSpecificationId, serviceSpecificationId);
    }

    public ProductServiceMappingPK getProductServiceMappingPK() {
        return productServiceMappingPK;
    }

    public void setProductServiceMappingPK(ProductServiceMappingPK productServiceMappingPK) {
        this.productServiceMappingPK = productServiceMappingPK;
    }

    public int getMinServiceOccurences() {
        return minServiceOccurences;
    }

    public void setMinServiceOccurences(int minServiceOccurences) {
        this.minServiceOccurences = minServiceOccurences;
    }

    public int getMaxServiceOccurences() {
        return maxServiceOccurences;
    }

    public void setMaxServiceOccurences(int maxServiceOccurences) {
        this.maxServiceOccurences = maxServiceOccurences;
    }

    public ServiceSpecification getServiceSpecification() {
        return serviceSpecification;
    }

    public void setServiceSpecification(ServiceSpecification serviceSpecification) {
        this.serviceSpecification = serviceSpecification;
    }

    public ProductSpecification getProductSpecification() {
        return productSpecification;
    }

    public void setProductSpecification(ProductSpecification productSpecification) {
        this.productSpecification = productSpecification;
    }

    public int getRatePlanId() {
        return ratePlanId;
    }

    public void setRatePlanId(int ratePlanId) {
        this.ratePlanId = ratePlanId;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public String getProvisionRoles() {
        return provisionRoles;
    }

    public void setProvisionRoles(String provisionRoles) {
        this.provisionRoles = provisionRoles;
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
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (productServiceMappingPK != null ? productServiceMappingPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ProductServiceMapping)) {
            return false;
        }
        ProductServiceMapping other = (ProductServiceMapping) object;
        if ((this.productServiceMappingPK == null && other.productServiceMappingPK != null) || (this.productServiceMappingPK != null && !this.productServiceMappingPK.equals(other.productServiceMappingPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.cm.db.model.ProductServiceMapping[ productServiceMappingPK=" + productServiceMappingPK + " ]";
    }
    
}
