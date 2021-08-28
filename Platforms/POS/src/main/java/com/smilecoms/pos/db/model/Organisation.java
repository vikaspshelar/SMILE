/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos.db.model;

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
import javax.persistence.Version;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author abhilash
 */
@Entity
@Table(name = "organisation",  catalog = "SmileDB", schema = "")
@NamedQueries({
    @NamedQuery(name = "Organisation.findAll", query = "SELECT o FROM Organisation o")})
public class Organisation implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Size(min = 0, max = 20)
    @Column(name = "CREDIT_ACCOUNT_NUMBER")
    private String creditAccountNumber;
    @Basic(optional = false)
    @NotNull
    @Size(min = 0, max = 200)
    @Column(name = "MODIFICATION_ROLES")
    private String modificationRoles;
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "ORGANISATION_ID")
    private Integer organisationId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ORGANISATION_NAME")
    private String organisationName;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ALTERNATIVE_CONTACT_1")
    private String alternativeContact1;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ALTERNATIVE_CONTACT_2")
    private String alternativeContact2;
    @Basic(optional = false)
    @NotNull
    @Column(name = "EMAIL_ADDRESS")
    private String emailAddress;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ORGANISATION_TYPE")
    private String organisationType;
    @Basic(optional = false)
    @NotNull
    @Column(name = "TAX_NUMBER")
    private String taxNumber;
    @Basic(optional = false)
    @NotNull
    @Column(name = "COMPANY_NUMBER")
    private String companyNumber;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SIZE")
    private String size;
    @Basic(optional = false)
    @NotNull
    @Column(name = "INDUSTRY")
    private String industry;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ACCOUNT_MANAGER_CUSTOMER_PROFILE_ID")
    private int accountManagerCustomerProfileId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CREATED_BY_CUSTOMER_PROFILE_ID")
    private int createdByCustomerProfileId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "STATUS")
    private String status;
    @Basic(optional = false)
    @NotNull
    @Version
    @Column(name = "VERSION")
    private int version;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CREATED_DATETIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDatetime;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CHANNEL_CODE")
    private String channelCode;
    
    @Column(name = "KYC_STATUS")
    private String kycStatus;
    
    @Column(name = "KYC_COMMENT")
    private String kycComment;

    public String getKycStatus() {
        return kycStatus;
    }

    public void setKycStatus(String kycStatus) {
        this.kycStatus = kycStatus;
    }

    public String getKycComment() {
        return kycComment;
    }

    public void setKycComment(String kycComment) {
        this.kycComment = kycComment;
    }
    
    
    public Organisation() {
    }

    public Organisation(Integer organisationId) {
        this.organisationId = organisationId;
    }

    public Organisation(Integer organisationId, String organistionName, String alternativeContact1, String alternativeContact2, String emailAddress, String organisationType, String taxNumber, String companyNumber, String size, String industry, int accountManagerCustomerProfileId, int createdByCustomerProfileId, String status, int version, String creditAccountNumber, String modificationRoles) {
        this.organisationId = organisationId;
        this.organisationName = organistionName;
        this.alternativeContact1 = alternativeContact1;
        this.alternativeContact2 = alternativeContact2;
        this.emailAddress = emailAddress;
        this.organisationType = organisationType;
        this.taxNumber = taxNumber;
        this.companyNumber = companyNumber;
        this.size = size;
        this.industry = industry;
        this.accountManagerCustomerProfileId = accountManagerCustomerProfileId;
        this.createdByCustomerProfileId = createdByCustomerProfileId;
        this.status = status;
        this.version = version;
        this.creditAccountNumber = creditAccountNumber;
        this.modificationRoles = modificationRoles;
    }

    public Integer getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(Integer organisationId) {
        this.organisationId = organisationId;
    }

    public String getOrganisationName() {
        return organisationName;
    }

    public void setOrganisationName(String organisationName) {
        this.organisationName = organisationName;
    }

    public String getAlternativeContact1() {
        return alternativeContact1;
    }

    public void setAlternativeContact1(String alternativeContact1) {
        this.alternativeContact1 = alternativeContact1;
    }

    public String getAlternativeContact2() {
        return alternativeContact2;
    }

    public void setAlternativeContact2(String alternativeContact2) {
        this.alternativeContact2 = alternativeContact2;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getOrganisationType() {
        return organisationType;
    }

    public void setOrganisationType(String organisationType) {
        this.organisationType = organisationType;
    }

    public String getTaxNumber() {
        return taxNumber;
    }

    public void setTaxNumber(String taxNumber) {
        this.taxNumber = taxNumber;
    }

    public String getCompanyNumber() {
        return companyNumber;
    }

    public void setCompanyNumber(String companyNumber) {
        this.companyNumber = companyNumber;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public int getAccountManagerCustomerProfileId() {
        return accountManagerCustomerProfileId;
    }

    public void setAccountManagerCustomerProfileId(int accountManagerCustomerProfileId) {
        this.accountManagerCustomerProfileId = accountManagerCustomerProfileId;
    }

    public int getCreatedByCustomerProfileId() {
        return createdByCustomerProfileId;
    }

    public void setCreatedByCustomerProfileId(int createdByCustomerProfileId) {
        this.createdByCustomerProfileId = createdByCustomerProfileId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Date getCreatedDatetime() {
        return createdDatetime;
    }

    public void setCreatedDatetime(Date createdDatetime) {
        this.createdDatetime = createdDatetime;
    }
    
    

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (organisationId != null ? organisationId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Organisation)) {
            return false;
        }
        Organisation other = (Organisation) object;
        if ((this.organisationId == null && other.organisationId != null) || (this.organisationId != null && !this.organisationId.equals(other.organisationId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.im.db.model.Organisation[ organisationId=" + organisationId + " ]";
    }

    public String getCreditAccountNumber() {
        return creditAccountNumber;
    }

    public void setCreditAccountNumber(String creditAccountNumber) {
        this.creditAccountNumber = creditAccountNumber;
    }

    public String getModificationRoles() {
        return modificationRoles;
    }

    public void setModificationRoles(String modificationRoles) {
        this.modificationRoles = modificationRoles;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public void setChannelCode(String channelCode) {
        this.channelCode = channelCode;
    }
    
}
