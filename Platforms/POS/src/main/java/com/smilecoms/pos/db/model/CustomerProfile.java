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
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;

/**
 *
 * @author abhilash
 */
@Entity
@Table(name = "customer_profile", catalog = "SmileDB", schema = "")
public class CustomerProfile implements Serializable {

    
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "CUSTOMER_PROFILE_ID")
    private Integer customerProfileId;
    @Column(name = "TITLE")
    private String title;
    @Basic(optional = false)
    @NotNull
    @Column(name = "FIRST_NAME")
    private String firstName;
    @Basic(optional = false)
    @NotNull
    @Column(name = "MIDDLE_NAME")
    private String middleName;
    @Basic(optional = false)
    @NotNull
    @Column(name = "LAST_NAME")
    private String lastName;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ID_NUMBER")
    private String idNumber;
    @Column(name = "NATIONAL_ID_NUMBER")
    private String nationalIdNumber;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CREATED_DATETIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDatetime;
    @Basic(optional = false)
    @NotNull
    @Column(name = "DATE_OF_BIRTH")
    private String dateOfBirth;
    @Basic(optional = false)
    @NotNull
    @Column(name = "GENDER")
    private String gender;
    @Basic(optional = false)
    @NotNull
    @Column(name = "LANGUAGE")
    private String language;
    @Basic(optional = false)
    @NotNull
    @Column(name = "EMAIL_ADDRESS")
    private String emailAddress;
    @Column(name = "REFERRAL_CODE")
    private String referralCode;
    @Basic(optional = false)
    @Column(name = "ID_NUMBER_TYPE")
    private String idNumberType;
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
    @Column(name = "CLASSIFICATION")
    private String classification;
    @Basic(optional = false)
    @NotNull
    @Column(name = "VERSION")
    @Version
    private int version;
    @Basic(optional = false)
    @Column(name = "SSO_IDENTITY")
    private String ssoIdentity;
    @Basic(optional = false)
    @Column(name = "SSO_DIGEST")
    private String ssoDigest;
    @Basic(optional = false)
    @Column(name = "SSO_AUTH_FLAGS")
    private Integer ssoAuthFlags;
    @Basic(optional = false)
    @Column(name = "SSO_AUTH_ATTEMPTS")
    private short ssoAuthAttempts;
    @Basic(optional = false)
    @Column(name = "SSO_LOCK_EXPIRY")
    @Temporal(TemporalType.TIMESTAMP)
    private Date ssoLockExpiry;
    @Basic(optional = false)
    @NotNull
    @Column(name = "STATUS")
    private String status;
    @Basic(optional = false)
    @NotNull
    @Column(name = "OPT_IN_LEVEL")
    private int optInLevel;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CREATED_BY_CUSTOMER_PROFILE_ID")
    private int createdByCustomerProfileId;
    @Column(name = "ACCOUNT_MANAGER_CUSTOMER_PROFILE_ID")
    private Integer accountManagerCustomerProfileId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "MOTHERS_MAIDEN_NAME")
    private String mothersMaidenName;
    @Basic(optional = false)
    @NotNull
    @Column(name = "NATIONALITY")
    private String nationality;
    @Column(name = "PASSPORT_EXPIRY_DATE")
    private String passportExpiryDate;
    @Column(name = "VISA_EXPIRY_DATE")
    private String visaExpiryDate;

    @Column(name = "WAREHOUSE_ID")
    private String warehouseId;
    @Column(name = "UPDATED_DATETIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedDatetime;
    @Column(name = "KYC_STATUS")
    private String kYCStatus;
    
    @Column(name = "CARD_NUMBER")
    private String cardNumber; 

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }
        
    public CustomerProfile() {
    }

    public CustomerProfile(Integer customerProfileId) {
        this.customerProfileId = customerProfileId;
    }

    public CustomerProfile(Integer customerProfileId, String firstName, String lastName, String idNumber, String nationalIdNumber, Date createdDatetime, String dateOfBirth, String gender, String language, String emailAddress, String alternativeContact1, String alternativeContact2, String classification, int version, String ssoIdentity, String ssoDigest, short ssoAuthAttempts, Date ssoLockExiry, String status) {
        this.customerProfileId = customerProfileId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.idNumber = idNumber;
        this.nationalIdNumber = nationalIdNumber;
        this.createdDatetime = createdDatetime;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.language = language;
        this.emailAddress = emailAddress;
        this.alternativeContact1 = alternativeContact1;
        this.alternativeContact2 = alternativeContact2;
        this.classification = classification;
        this.version = version;
        this.ssoIdentity = ssoIdentity;
        this.ssoDigest = ssoDigest;
        this.ssoAuthAttempts = ssoAuthAttempts;
        this.ssoLockExpiry = ssoLockExiry;
        this.status = status;
    }

    public int getOptInLevel() {
        return optInLevel;
    }

    public void setOptInLevel(int optInLevel) {
        this.optInLevel = optInLevel;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public int getCreatedByCustomerProfileId() {
        return createdByCustomerProfileId;
    }

    public void setCreatedByCustomerProfileId(int createdByCustomerProfileId) {
        this.createdByCustomerProfileId = createdByCustomerProfileId;
    }

    public String getIdNumberType() {
        return idNumberType;
    }

    public void setIdNumberType(String idNumberType) {
        this.idNumberType = idNumberType;
    }

    public Integer getCustomerProfileId() {
        return customerProfileId;
    }

    public void setCustomerProfileId(Integer customerProfileId) {
        this.customerProfileId = customerProfileId;
    }

    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }
    
    public String getNationalIdNumber() {
        return nationalIdNumber;
    }

    public void setNationalIdNumber(String nationalIdNumber) {
        this.nationalIdNumber = nationalIdNumber;
    }

    public Date getCreatedDatetime() {
        return createdDatetime;
    }

    public void setCreatedDatetime(Date createdDatetime) {
        this.createdDatetime = createdDatetime;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
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

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getSsoIdentity() {
        return ssoIdentity;
    }

    public void setSsoIdentity(String ssoIdentity) {
        this.ssoIdentity = ssoIdentity;
    }

    public String getSsoDigest() {
        return ssoDigest;
    }

    public void setSsoDigest(String ssoDigest) {
        this.ssoDigest = ssoDigest;
    }

    public short getSsoAuthAttempts() {
        return ssoAuthAttempts;
    }

    public void setSsoAuthAttempts(short ssoAuthAttempts) {
        this.ssoAuthAttempts = ssoAuthAttempts;
    }

    public Integer getSsoAuthFlags() {
        return ssoAuthFlags;
    }

    
    // Bitmask field
    // Bit 0 on = Allow auto login I.e. allow auto login if ssoAuthFlags & 1 != 0
    public void setSsoAuthFlags(Integer ssoAuthFlags) {
        this.ssoAuthFlags = ssoAuthFlags;
    }
    
    public Date getSsoLockExpiry() {
        return ssoLockExpiry;
    }

    public void setSsoLockExpiry(Date ssoLockExpiry) {
        this.ssoLockExpiry = ssoLockExpiry;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getAccountManagerCustomerProfileId() {
        return accountManagerCustomerProfileId;
    }

    public void setAccountManagerCustomerProfileId(Integer accountManagerCustomerProfileId) {
        this.accountManagerCustomerProfileId = accountManagerCustomerProfileId;
    }

    public String getMothersMaidenName() {
        return mothersMaidenName;
    }

    public void setMothersMaidenName(String mothersMaidenName) {
        this.mothersMaidenName = mothersMaidenName;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getPassportExpiryDate() {
        return passportExpiryDate;
    }

    public void setPassportExpiryDate(String passportExpiryDate) {
        this.passportExpiryDate = passportExpiryDate;
    }

    public String getkYCStatus() {
        return kYCStatus;
    }

    public void setkYCStatus(String kYCStatus) {
        this.kYCStatus = kYCStatus;
    }

    public String getReferralCode() {
        return referralCode;
    }

    public void setReferralCode(String referralCode) {
        this.referralCode = referralCode;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (customerProfileId != null ? customerProfileId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CustomerProfile)) {
            return false;
        }
        CustomerProfile other = (CustomerProfile) object;
        if ((this.customerProfileId == null && other.customerProfileId != null) || (this.customerProfileId != null && !this.customerProfileId.equals(other.customerProfileId))) {
            return false;
        }
        return true;
    }

    public Date getUpdatedDatetime() {
        return updatedDatetime;
    }

    public void setUpdatedDatetime(Date updatedDatetime) {
        this.updatedDatetime = updatedDatetime;
    }
        
    public String getVisaExpiryDate() {
        return visaExpiryDate;
    }

    public void setVisaExpiryDate(String visaExpiryDate) {
        this.visaExpiryDate = visaExpiryDate;
    }

    @Override
    public String toString() {
        return "com.smilecoms.im.db.model.CustomerProfile[ customerProfileId=" + customerProfileId + " ]";
    }
}
