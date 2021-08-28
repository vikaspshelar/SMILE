/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.cm.db.model;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author mukosi
 */
@Entity
@Table(name = "service_specification")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ServiceSpecification.findAll", query = "SELECT s FROM ServiceSpecification s"),
    @NamedQuery(name = "ServiceSpecification.findByServiceSpecificationId", query = "SELECT s FROM ServiceSpecification s WHERE s.serviceSpecificationId = :serviceSpecificationId"),
    @NamedQuery(name = "ServiceSpecification.findByServiceName", query = "SELECT s FROM ServiceSpecification s WHERE s.serviceName = :serviceName"),
    @NamedQuery(name = "ServiceSpecification.findByServiceDescription", query = "SELECT s FROM ServiceSpecification s WHERE s.serviceDescription = :serviceDescription"),
    @NamedQuery(name = "ServiceSpecification.findByServiceCode", query = "SELECT s FROM ServiceSpecification s WHERE s.serviceCode = :serviceCode")})
public class ServiceSpecification implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @NotNull
    @Column(name = "SERVICE_SPECIFICATION_ID")
    private Integer serviceSpecificationId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SERVICE_NAME")
    private String serviceName;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SERVICE_DESCRIPTION")
    private String serviceDescription;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SERVICE_CODE")
    private String serviceCode;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "serviceSpecification")
    private Collection<ProductServiceMapping> productServiceMappingCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "serviceSpecification")
    private Collection<ServiceSpecificationAvp> serviceSpecificationAvpCollection;
    
    
    public ServiceSpecification() {
    }

    public ServiceSpecification(Integer serviceSpecificationId) {
        this.serviceSpecificationId = serviceSpecificationId;
    }

    public ServiceSpecification(Integer serviceSpecificationId, String serviceName, String serviceDescription, String serviceCode) {
        this.serviceSpecificationId = serviceSpecificationId;
        this.serviceName = serviceName;
        this.serviceDescription = serviceDescription;
        this.serviceCode = serviceCode;
    }

    public Integer getServiceSpecificationId() {
        return serviceSpecificationId;
    }

    public void setServiceSpecificationId(Integer serviceSpecificationId) {
        this.serviceSpecificationId = serviceSpecificationId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceDescription() {
        return serviceDescription;
    }

    public void setServiceDescription(String serviceDescription) {
        this.serviceDescription = serviceDescription;
    }

    
    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    @XmlTransient
    public Collection<ProductServiceMapping> getProductServiceMappingCollection() {
        return productServiceMappingCollection;
    }

    public void setProductServiceMappingCollection(Collection<ProductServiceMapping> productServiceMappingCollection) {
        this.productServiceMappingCollection = productServiceMappingCollection;
    }

    @XmlTransient
    public Collection<ServiceSpecificationAvp> getServiceSpecificationAvpCollection() {
        return serviceSpecificationAvpCollection;
    }

    public void setServiceSpecificationAvpCollection(Collection<ServiceSpecificationAvp> serviceSpecificationAvpCollection) {
        this.serviceSpecificationAvpCollection = serviceSpecificationAvpCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (serviceSpecificationId != null ? serviceSpecificationId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ServiceSpecification)) {
            return false;
        }
        ServiceSpecification other = (ServiceSpecification) object;
        if ((this.serviceSpecificationId == null && other.serviceSpecificationId != null) || (this.serviceSpecificationId != null && !this.serviceSpecificationId.equals(other.serviceSpecificationId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.cm.db.model.ServiceSpecification[ serviceSpecificationId=" + serviceSpecificationId + " ]";
    }
    
}
