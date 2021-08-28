/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.cm.db.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author mukosi
 */
@Entity
@Table(name = "product_specification")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ProductSpecification.findAll", query = "SELECT p FROM ProductSpecification p"),
    @NamedQuery(name = "ProductSpecification.findByProductSpecificationId", query = "SELECT p FROM ProductSpecification p WHERE p.productSpecificationId = :productSpecificationId"),
    @NamedQuery(name = "ProductSpecification.findByProductName", query = "SELECT p FROM ProductSpecification p WHERE p.productName = :productName"),
    @NamedQuery(name = "ProductSpecification.findByProductDescription", query = "SELECT p FROM ProductSpecification p WHERE p.productDescription = :productDescription"),
    @NamedQuery(name = "ProductSpecification.findByAvailableFrom", query = "SELECT p FROM ProductSpecification p WHERE p.availableFrom = :availableFrom"),
    @NamedQuery(name = "ProductSpecification.findByAvailableTo", query = "SELECT p FROM ProductSpecification p WHERE p.availableTo = :availableTo")})
public class ProductSpecification implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @NotNull
    @Column(name = "PRODUCT_SPECIFICATION_ID")
    private Integer productSpecificationId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PRODUCT_NAME")
    private String productName;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PRODUCT_DESCRIPTION")
    private String productDescription;
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
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "productSpecification")
    private Collection<ProductInstance> productInstanceCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "productSpecification")
    private Collection<ProductServiceMapping> productServiceMappingCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "productSpecification")
    private Collection<ProductSpecificationAvp> productSpecificationAvpCollection;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PROVISION_ROLES")
    private String provisionRoles;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SEGMENTS")
    private String segments;
    @Basic(optional = false)
    @NotNull
    @Column(name = "REPORTING_TYPE")
    private String reportingType;
    
    public ProductSpecification() {
    }

    public ProductSpecification(Integer productSpecificationId) {
        this.productSpecificationId = productSpecificationId;
    }

    public ProductSpecification(Integer productSpecificationId, String productName, String productDescription, Date availableFrom, Date availableTo) {
        this.productSpecificationId = productSpecificationId;
        this.productName = productName;
        this.productDescription = productDescription;
        this.availableFrom = availableFrom;
        this.availableTo = availableTo;
    }

    public Integer getProductSpecificationId() {
        return productSpecificationId;
    }

    public void setProductSpecificationId(Integer productSpecificationId) {
        this.productSpecificationId = productSpecificationId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductDescription() {
        return productDescription;
    }

    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
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

    public String getReportingType() {
        return reportingType;
    }

    public void setReportingType(String reportingType) {
        this.reportingType = reportingType;
    }

    @XmlTransient
    public Collection<ProductInstance> getProductInstanceCollection() {
        return productInstanceCollection;
    }

    public void setProductInstanceCollection(Collection<ProductInstance> productInstanceCollection) {
        this.productInstanceCollection = productInstanceCollection;
    }

    @XmlTransient
    public Collection<ProductServiceMapping> getProductServiceMappingCollection() {
        return productServiceMappingCollection;
    }

    public void setProductServiceMappingCollection(Collection<ProductServiceMapping> productServiceMappingCollection) {
        this.productServiceMappingCollection = productServiceMappingCollection;
    }

    @XmlTransient
    public Collection<ProductSpecificationAvp> getProductSpecificationAvpCollection() {
        return productSpecificationAvpCollection;
    }

    public void setProductSpecificationAvpCollection(Collection<ProductSpecificationAvp> productSpecificationAvpCollection) {
        this.productSpecificationAvpCollection = productSpecificationAvpCollection;
    }

    public String getProvisionRoles() {
        return provisionRoles;
    }

    public void setProvisionRoles(String provisionRoles) {
        this.provisionRoles = provisionRoles;
    }

    public String getSegments() {
        return segments;
    }

    public void setSegments(String segments) {
        this.segments = segments;
    }
    
    
    

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (productSpecificationId != null ? productSpecificationId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ProductSpecification)) {
            return false;
        }
        ProductSpecification other = (ProductSpecification) object;
        if ((this.productSpecificationId == null && other.productSpecificationId != null) || (this.productSpecificationId != null && !this.productSpecificationId.equals(other.productSpecificationId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.cm.db.model.ProductSpecification[ productSpecificationId=" + productSpecificationId + " ]";
    }
    
}
