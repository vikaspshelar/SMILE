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
@Table(name = "product_instance")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ProductInstance.findAll", query = "SELECT p FROM ProductInstance p where p.status!='DE'"),
    @NamedQuery(name = "ProductInstance.findByProductInstanceId", query = "SELECT p FROM ProductInstance p WHERE p.productInstanceId = :productInstanceId and p.status != 'DE'"),
    @NamedQuery(name = "ProductInstance.findByCustomerProfileId", query = "SELECT p FROM ProductInstance p WHERE p.customerProfileId = :customerProfileId and p.status != 'DE' order by p.productInstanceId"),
    @NamedQuery(name = "ProductInstance.findByOrganisationId", query = "SELECT p FROM ProductInstance p WHERE p.organisationId = :organisationId and p.status != 'DE' order by p.productInstanceId")})
public class ProductInstance implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "PRODUCT_INSTANCE_ID")
    private Integer productInstanceId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CUSTOMER_PROFILE_ID")
    private int customerProfileId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ORGANISATION_ID")
    private int organisationId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "STATUS")
    private String status;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SEGMENT")
    private String segment;
    @JoinColumn(name = "PRODUCT_SPECIFICATION_ID", referencedColumnName = "PRODUCT_SPECIFICATION_ID")
    @ManyToOne(optional = false)
    private ProductSpecification productSpecification;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CREATED_DATETIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDatetime;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CREATED_BY_CUSTOMER_PROFILE_ID")
    private int createdByCustomerProfileId;
    @Column(name = "CREATED_BY_ORGANISATION_ID")
    private int createdByOrganisationId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PROMOTION_CODE")
    private String promotionCode;
    @Basic(optional = false)
    @Column(name = "LAST_MODIFIED")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastModified;
    @Basic(optional = false)
    @NotNull
    @Column(name = "FRIENDLY_NAME")
    private String friendlyName;
    @Basic(optional = false)
    @NotNull
    @Column(name = "LOGICAL_ID")
    private int logicalId;
    @Basic(optional = false)
    @Column(name = "PHYSICAL_ID")
    private String physicalId;
    @Column(name = "LAST_IMEI")
    private String lastIMEI;
    @Column(name = "REFERRAL_CODE")
    private String referralCode;
    @Column(name = "FIRST_ACTIVITY_DATETIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date firstActivityDateTime;
    @Column(name = "LAST_ACTIVITY_DATETIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastActivityDateTime;
    
    public  ProductInstance() {
    }

    public ProductInstance(Integer productInstanceId) {
        this.productInstanceId = productInstanceId;
    }

    public ProductInstance(Integer productInstanceId, int customerProfileId) {
        this.productInstanceId = productInstanceId;
        this.customerProfileId = customerProfileId;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public Integer getProductInstanceId() {
        return productInstanceId;
    }

    public void setProductInstanceId(Integer productInstanceId) {
        this.productInstanceId = productInstanceId;
    }

    public int getCustomerProfileId() {
        return customerProfileId;
    }

    public void setCustomerProfileId(int customerProfileId) {
        this.customerProfileId = customerProfileId;
    }

    public int getLogicalId() {
        return logicalId;
    }

    public void setLogicalId(int logicalId) {
        this.logicalId = logicalId;
    }

    public String getPhysicalId() {
        return physicalId;
    }

    public void setPhysicalId(String physicalId) {
        this.physicalId = physicalId;
    }

    public int getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(int organisationId) {
        this.organisationId = organisationId;
    }
    
    public Date getCreatedDatetime() {
        return createdDatetime;
    }

    public void setCreatedDatetime(Date createdDatetime) {
        this.createdDatetime = createdDatetime;
    }

    public int getCreatedByCustomerProfileId() {
        return createdByCustomerProfileId;
    }

    public void setCreatedByCustomerProfileId(int createdByCustomerProfileId) {
        this.createdByCustomerProfileId = createdByCustomerProfileId;
    }

    public int getCreatedByOrganisationId() {
        return createdByOrganisationId;
    }

    public void setCreatedByOrganisationId(int createdByOrganisationId) {
        this.createdByOrganisationId = createdByOrganisationId;
    }
    
    public ProductSpecification getProductSpecification() {
        return productSpecification;
    }

    public void setProductSpecificationId(ProductSpecification productSpecification) {
        this.productSpecification = productSpecification;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSegment() {
        return segment;
    }

    public void setSegment(String segment) {
        this.segment = segment;
    }

    public String getPromotionCode() {
        return promotionCode;
    }

    public void setPromotionCode(String promotionCode) {
        this.promotionCode = promotionCode;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public String getLastIMEI() {
        return lastIMEI;
    }

    public void setLastIMEI(String lastIMEI) {
        this.lastIMEI = lastIMEI;
    }

    public String getReferralCode() {
        return referralCode;
    }

    public void setReferralCode(String referralCode) {
        this.referralCode = referralCode;
    }

    public Date getFirstActivityDateTime() {
        return firstActivityDateTime;
    }

    public void setFirstActivityDateTime(Date firstActivityDateTime) {
        this.firstActivityDateTime = firstActivityDateTime;
    }

    public Date getLastActivityDateTime() {
        return lastActivityDateTime;
    }

    public void setLastActivityDateTime(Date lastActivityDateTime) {
        this.lastActivityDateTime = lastActivityDateTime;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (productInstanceId != null ? productInstanceId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ProductInstance)) {
            return false;
        }
        ProductInstance other = (ProductInstance) object;
        if ((this.productInstanceId == null && other.productInstanceId != null) || (this.productInstanceId != null && !this.productInstanceId.equals(other.productInstanceId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.cm.db.model.ProductInstance[ productInstanceId=" + productInstanceId + " ]";
    }
    
}
