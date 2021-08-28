/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.cm.db.model;

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
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author mukosi
 */
@Entity
@Table(name = "service_instance")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ServiceInstance.findAll", query = "SELECT s FROM ServiceInstance s where s.status != 'DE'"),
    @NamedQuery(name = "ServiceInstance.findByServiceInstanceId", query = "SELECT s FROM ServiceInstance s WHERE s.serviceInstanceId = :serviceInstanceId and s.status != 'DE'"),
    @NamedQuery(name = "ServiceInstance.findByProductInstanceId", query = "SELECT s FROM ServiceInstance s WHERE s.productInstanceId = :productInstanceId and s.status != 'DE' order by s.serviceInstanceId"),
    @NamedQuery(name = "ServiceInstance.findByRemoteResourceId", query = "SELECT s FROM ServiceInstance s WHERE s.remoteResourceId = :remoteResourceId  and s.status != 'DE' order by s.serviceInstanceId"),
    @NamedQuery(name = "ServiceInstance.findByCustomerProfileId", query = "SELECT s FROM ServiceInstance s WHERE s.customerProfileId = :customerProfileId  and s.status != 'DE' order by s.serviceInstanceId"),
    @NamedQuery(name = "ServiceInstance.findByAccountId", query = "SELECT s FROM ServiceInstance s WHERE s.accountId = :accountId  and s.status != 'DE' order by s.serviceInstanceId")})
public class ServiceInstance implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "SERVICE_INSTANCE_ID")
    private Integer serviceInstanceId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PRODUCT_INSTANCE_ID")
    private int productInstanceId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "REMOTE_RESOURCE_ID")
    private String remoteResourceId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CUSTOMER_PROFILE_ID")
    private int customerProfileId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ACCOUNT_ID")
    private long accountId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "STATUS")
    private String status;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CREATED_DATETIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDatetime;
    @Basic(optional = false)
    @Column(name = "SERVICE_SPECIFICATION_ID")
    @NotNull
    private int serviceSpecificationId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CREATED_BY_CUSTOMER_PROFILE_ID")
    private int createdByCustomerProfileId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "INFO")
    private String info;
    @Basic(optional = false)
    @Column(name = "LAST_MODIFIED")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastModified;
    
    public ServiceInstance() {
    }

    public ServiceInstance(Integer serviceInstanceId) {
        this.serviceInstanceId = serviceInstanceId;
    }

    public ServiceInstance(Integer serviceInstanceId, int productInstanceId, String remoteResourceId, int customerProfileId, long accountId) {
        this.serviceInstanceId = serviceInstanceId;
        this.productInstanceId = productInstanceId;
        this.remoteResourceId = remoteResourceId;
        this.customerProfileId = customerProfileId;
        this.accountId = accountId;
    }

    public int getCreatedByCustomerProfileId() {
        return createdByCustomerProfileId;
    }

    public void setCreatedByCustomerProfileId(int createdByCustomerProfileId) {
        this.createdByCustomerProfileId = createdByCustomerProfileId;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public Integer getServiceInstanceId() {
        return serviceInstanceId;
    }

    public void setServiceInstanceId(Integer serviceInstanceId) {
        this.serviceInstanceId = serviceInstanceId;
    }

    public int getProductInstanceId() {
        return productInstanceId;
    }

    public void setProductInstanceId(int productInstanceId) {
        this.productInstanceId = productInstanceId;
    }

    public Date getCreatedDatetime() {
        return createdDatetime;
    }

    public void setCreatedDatetime(Date createdDatetime) {
        this.createdDatetime = createdDatetime;
    }
    
    public String getRemoteResourceId() {
        return remoteResourceId;
    }

    public void setRemoteResourceId(String remoteResourceId) {
        this.remoteResourceId = remoteResourceId;
    }

    public int getCustomerProfileId() {
        return customerProfileId;
    }

    public void setCustomerProfileId(int customerProfileId) {
        this.customerProfileId = customerProfileId;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (serviceInstanceId != null ? serviceInstanceId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ServiceInstance)) {
            return false;
        }
        ServiceInstance other = (ServiceInstance) object;
        if ((this.serviceInstanceId == null && other.serviceInstanceId != null) || (this.serviceInstanceId != null && !this.serviceInstanceId.equals(other.serviceInstanceId))) {
            return false;
        }
        return true;
    }
    
    public int getServiceSpecificationId() {
        return serviceSpecificationId;
    }

    public void setServiceSpecificationId(int serviceSpecificationId) {
        this.serviceSpecificationId = serviceSpecificationId;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
    
    
    @Override
    public String toString() {
        return "com.smilecoms.cm.db.model.ServiceInstance[ serviceInstanceId=" + serviceInstanceId + " ]";
    }

    

    
    
}
