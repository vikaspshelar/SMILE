/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sapi.model;

import java.util.GregorianCalendar;
import java.util.List;

/**
 *
 * @author bhaskarhg
 */
public class Customer {
    
    
    protected int customerId;
    
    protected String title;
    
    protected String firstName;
    
    protected String middleName;
    
    protected String lastName;
    
    protected String customerStatus;
    
    protected String identityNumber;
    
    protected String identityNumberType;
    
    protected String cardNumber;
    
    protected GregorianCalendar createdDateTime;
    
    protected List<Address> addresses;
    
    protected String dateOfBirth;
    
    protected int version;
    
    protected String gender;
    
    protected String language;
    
    protected String emailAddress;
    
    protected String alternativeContact1;
    
    protected String alternativeContact2;
    
    protected String classification;
    
    protected List<String> securityGroups;
    
    protected String ssoIdentity;
    
//    protected String ssoDigest;
    
    protected int ssoAuthFlags;
    
    protected int optInLevel;
    
    protected List<ProductInstance> productInstances;
    
    protected Integer productInstancesTotalCount;
    
    protected List<Photograph> customerPhotographs;
    
    protected int accountManagerCustomerProfileId;
    
    protected List<String> outstandingTermsAndConditions;
    
    protected List<CustomerRole> customerRoles;
    
    protected String mothersMaidenName;
    
    protected String nationality;
    
    protected String passportExpiryDate;
    
    protected String visaExpiryDate;
    
    protected String warehouseId;
    
    protected int createdByCustomerProfileId;
    
    protected String kycStatus;
    
    protected String referralCode;

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
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

    public String getCustomerStatus() {
        return customerStatus;
    }

    public void setCustomerStatus(String customerStatus) {
        this.customerStatus = customerStatus;
    }

    public String getIdentityNumber() {
        return identityNumber;
    }

    public void setIdentityNumber(String identityNumber) {
        this.identityNumber = identityNumber;
    }

    public String getIdentityNumberType() {
        return identityNumberType;
    }

    public void setIdentityNumberType(String identityNumberType) {
        this.identityNumberType = identityNumberType;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public GregorianCalendar getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(GregorianCalendar createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
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

    public List<String> getSecurityGroups() {
        return securityGroups;
    }

    public void setSecurityGroups(List<String> securityGroups) {
        this.securityGroups = securityGroups;
    }

    public String getSsoIdentity() {
        return ssoIdentity;
    }

    public void setSsoIdentity(String ssoIdentity) {
        this.ssoIdentity = ssoIdentity;
    }

//    public String getSsoDigest() {
//        return ssoDigest;
//    }
//
//    public void setSsoDigest(String ssoDigest) {
//        this.ssoDigest = ssoDigest;
//    }

    public int getSsoAuthFlags() {
        return ssoAuthFlags;
    }

    public void setSsoAuthFlags(int ssoAuthFlags) {
        this.ssoAuthFlags = ssoAuthFlags;
    }

    public int getOptInLevel() {
        return optInLevel;
    }

    public void setOptInLevel(int optInLevel) {
        this.optInLevel = optInLevel;
    }

    public List<ProductInstance> getProductInstances() {
        return productInstances;
    }

    public void setProductInstances(List<ProductInstance> productInstances) {
        this.productInstances = productInstances;
    }

    public Integer getProductInstancesTotalCount() {
        return productInstancesTotalCount;
    }

    public void setProductInstancesTotalCount(Integer productInstancesTotalCount) {
        this.productInstancesTotalCount = productInstancesTotalCount;
    }

    public List<Photograph> getCustomerPhotographs() {
        return customerPhotographs;
    }

    public void setCustomerPhotographs(List<Photograph> customerPhotographs) {
        this.customerPhotographs = customerPhotographs;
    }

    public int getAccountManagerCustomerProfileId() {
        return accountManagerCustomerProfileId;
    }

    public void setAccountManagerCustomerProfileId(int accountManagerCustomerProfileId) {
        this.accountManagerCustomerProfileId = accountManagerCustomerProfileId;
    }

    public List<String> getOutstandingTermsAndConditions() {
        return outstandingTermsAndConditions;
    }

    public void setOutstandingTermsAndConditions(List<String> outstandingTermsAndConditions) {
        this.outstandingTermsAndConditions = outstandingTermsAndConditions;
    }

    public List<CustomerRole> getCustomerRoles() {
        return customerRoles;
    }

    public void setCustomerRoles(List<CustomerRole> customerRoles) {
        this.customerRoles = customerRoles;
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

    public String getVisaExpiryDate() {
        return visaExpiryDate;
    }

    public void setVisaExpiryDate(String visaExpiryDate) {
        this.visaExpiryDate = visaExpiryDate;
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

    public String getKycStatus() {
        return kycStatus;
    }

    public void setKycStatus(String kycStatus) {
        this.kycStatus = kycStatus;
    }

    public String getReferralCode() {
        return referralCode;
    }

    public void setReferralCode(String referralCode) {
        this.referralCode = referralCode;
    }
    
}
