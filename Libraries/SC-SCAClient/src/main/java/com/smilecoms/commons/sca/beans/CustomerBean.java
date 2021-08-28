/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.sca.beans;

import com.smilecoms.commons.sca.Account;
import com.smilecoms.commons.sca.Address;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.CustomerQuery;
import com.smilecoms.commons.sca.CustomerRole;
import com.smilecoms.commons.sca.Errors;
import com.smilecoms.commons.sca.Photograph;
import com.smilecoms.commons.sca.SCABusinessError;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.SSOPasswordResetLinkData;
import com.smilecoms.commons.sca.StAccountLookupVerbosity;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.util.Utils;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CustomerBean extends BaseBean {

    private final com.smilecoms.commons.sca.Customer xmlCustomer;
    private static final Logger log = LoggerFactory.getLogger(CustomerBean.class);

    public CustomerBean() {
        xmlCustomer = new com.smilecoms.commons.sca.Customer();
    }

    public static CustomerBean getCustomerById(int customerId, StCustomerLookupVerbosity verbosity) {
        return new CustomerBean(UserSpecificCachedDataHelper.getCustomer(customerId, verbosity));
    }

    public static CustomerBean getCustomerByUserName(String username, StCustomerLookupVerbosity verbosity) {
        log.debug("Getting Customer by username [{}]", username);
        CustomerQuery q = new CustomerQuery();
        q.setSSOIdentity(username);
        q.setVerbosity(verbosity);
        q.setResultLimit(1);
        return new CustomerBean(SCAWrapper.getUserSpecificInstance().getCustomer(q));
    }

    public static CustomerBean getCustomerByEmail(String email, StCustomerLookupVerbosity verbosity) {
        log.debug("Getting Customer by email [{}]", email);
        CustomerQuery q = new CustomerQuery();
        q.setEmailAddress(email);
        q.setVerbosity(verbosity);
        q.setResultLimit(1);
        return new CustomerBean(SCAWrapper.getUserSpecificInstance().getCustomer(q));
    }

    public static CustomerBean getCustomerByPhoneNumber(String uri, StCustomerLookupVerbosity verbosity) {
        log.debug("Getting customer by phone number [{}]", uri);
        try {
            return getCustomerByIMPU(uri, verbosity);
        } catch (Exception e) {
            log.debug("No service instance for number [{}] [{}]", uri, e.toString());
        }
        // Must be alternative contact number:
        CustomerQuery q = new CustomerQuery();
        q.setAlternativeContact(uri);
        q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES);
        q.setResultLimit(1);
        return new CustomerBean(SCAWrapper.getUserSpecificInstance().getCustomer(q));
    }

    public static CustomerBean getCustomerByAccountNumber(long accountNumber, StCustomerLookupVerbosity verbosity) {
        log.debug("Getting customer by account number [{}]", accountNumber);
        Account acc = SCAWrapper.getUserSpecificInstance().getAccount(accountNumber, StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
        if (acc.getServiceInstances() == null || acc.getServiceInstances().isEmpty()) {
            SCABusinessError b = new SCABusinessError();
            b.setErrorCode(Errors.ERROR_CODE_SCA_NO_RESULT);
            b.setErrorDesc("No results found");
            throw b;
        }
        int custId = acc.getServiceInstances().get(0).getCustomerId();
        return getCustomerById(custId, verbosity);
    }

    public static CustomerBean getCustomerByIMPU(String impu, StCustomerLookupVerbosity verbosity) {
        return getCustomerById(ServiceBean.getServiceInstanceByIMPU(impu).getCustomerId(), verbosity);
    }

    public CustomerBean(com.smilecoms.commons.sca.Customer xmlCustomer) {
        this.xmlCustomer = xmlCustomer;
    }

    public static void changeCustomerPassword(int id, String newPassword) throws Exception {
        CustomerQuery cq = new CustomerQuery();
        cq.setCustomerId(id);
        cq.setResultLimit(1);
        cq.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        Customer tmp = SCAWrapper.getUserSpecificInstance().getCustomer(cq);
        tmp.setSSODigest(Utils.hashPasswordWithComplexityCheck(newPassword));
        SCAWrapper.getUserSpecificInstance().modifyCustomer(tmp);
    }

    public static void changeCustomerPassword(String username, String newPassword) throws Exception {
        CustomerQuery cq = new CustomerQuery();
        cq.setSSOIdentity(username);
        cq.setResultLimit(1);
        cq.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        Customer tmp = SCAWrapper.getUserSpecificInstance().getCustomer(cq);
        tmp.setSSODigest(Utils.hashPasswordWithComplexityCheck(newPassword));
        SCAWrapper.getUserSpecificInstance().modifyCustomer(tmp);
    }

    public static CustomerBean addCustomer(CustomerBean cust) {

        com.smilecoms.commons.sca.Customer scaCustomer = new com.smilecoms.commons.sca.Customer();
        scaCustomer.setAlternativeContact1(cust.getAlternativeContact1());
        scaCustomer.setAlternativeContact2(cust.getAlternativeContact2());
        scaCustomer.setDateOfBirth(cust.getDateOfBirth());
        scaCustomer.setEmailAddress(cust.getEmailAddress());
        scaCustomer.setFirstName(cust.getFirstName());
        scaCustomer.setGender(cust.getGender());
        scaCustomer.setIdentityNumber(cust.getIdentityNumber());
        scaCustomer.setIdentityNumberType(cust.getIdentityNumberType());
        scaCustomer.setLanguage(cust.getLanguage());
        scaCustomer.setLastName(cust.getLastName());
        scaCustomer.setMiddleName(cust.getMiddleName());
        scaCustomer.setMothersMaidenName(cust.getMothersMaidenName());
        scaCustomer.setNationality(cust.getNationality());
        scaCustomer.setPassportExpiryDate(cust.getPassportExpiryDate());
        scaCustomer.setSSOIdentity(cust.getSSOIdentity());
        scaCustomer.setClassification(cust.getClassification());
        scaCustomer.setOptInLevel(cust.getOptInLevel());
        scaCustomer.getSecurityGroups().add("Customer");
        scaCustomer.getAddresses().addAll(cust.getAddresses());
        scaCustomer.setNationalIdentityNumber(cust.getNationalIdentityNumber());
        int profileId = SCAWrapper.getUserSpecificInstance().addCustomer(scaCustomer).getInteger();
        return getCustomerById(profileId, StCustomerLookupVerbosity.CUSTOMER);
    }

    public static void sendPasswordResetLink(String idenitifier) throws Exception {
        String username = idenitifier;
        SSOPasswordResetLinkData pwdrd = new SSOPasswordResetLinkData();
        if (username.contains("@") && username.contains(".")) {
            CustomerQuery q = new CustomerQuery();
            q.setEmailAddress(idenitifier);
            q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
            q.setResultLimit(1);
            Customer tmp = SCAWrapper.getAdminInstance().getCustomer(q);
        }
        
        pwdrd.setIdentifier(idenitifier);
        SCAWrapper.getAdminInstance().sendSSOPasswordResetLink(pwdrd);
    }

    @XmlElement
    public List<ProductBean> getProducts() {
        return ProductBean.wrap(xmlCustomer.getProductInstances());
    }

    @XmlElement
    public int getCustomerId() {
        return xmlCustomer.getCustomerId();
    }

    @XmlElement
    public String getFirstName() {
        return xmlCustomer.getFirstName();
    }

    @XmlElement
    public String getMiddleName() {
        return xmlCustomer.getMiddleName();
    }

    @XmlElement
    public String getLastName() {
        return xmlCustomer.getLastName();
    }

    @XmlElement
    public String getCustomerStatus() {
        return xmlCustomer.getCustomerStatus();
    }

    @XmlElement
    public String getIdentityNumber() {
        return xmlCustomer.getIdentityNumber();
    }

    @XmlElement
    public String getIdentityNumberType() {
        return xmlCustomer.getIdentityNumberType();
    }

    @XmlElement
    public long getCreatedDateTime() {
        return Utils.getJavaDate(xmlCustomer.getCreatedDateTime()).getTime();
    }

    @XmlElement
    public List<Address> getAddresses() {
        return xmlCustomer.getAddresses();
    }

    @XmlElement
    public String getDateOfBirth() {
        return xmlCustomer.getDateOfBirth();
    }

    @XmlElement
    public int getVersion() {
        return xmlCustomer.getVersion();
    }

    @XmlElement
    public String getGender() {
        return xmlCustomer.getGender();
    }

    @XmlElement
    public String getClassification() {
        return xmlCustomer.getClassification();
    }

    @XmlElement
    public String getLanguage() {
        return xmlCustomer.getLanguage();
    }

    @XmlElement
    public String getEmailAddress() {
        return xmlCustomer.getEmailAddress();
    }

    @XmlElement
    public String getAlternativeContact1() {
        return xmlCustomer.getAlternativeContact1();
    }

    @XmlElement
    public String getAlternativeContact2() {
        return xmlCustomer.getAlternativeContact2();
    }

    @XmlElement
    public List<String> getSecurityGroups() {
        return xmlCustomer.getSecurityGroups();
    }

    @XmlElement
    public String getSSOIdentity() {
        return xmlCustomer.getSSOIdentity();
    }

    @XmlElement
    public int getOptInLevel() {
        return xmlCustomer.getOptInLevel();
    }

    @XmlElement
    public List<Photograph> getCustomerPhotographs() {
        return xmlCustomer.getCustomerPhotographs();
    }

    @XmlElement
    public int getAccountManagerCustomerProfileId() {
        return xmlCustomer.getAccountManagerCustomerProfileId();
    }

    @XmlElement
    public List<String> getOutstandingTermsAndConditions() {
        return xmlCustomer.getOutstandingTermsAndConditions();
    }

    @XmlElement
    public List<CustomerRole> getCustomerRoles() {
        return xmlCustomer.getCustomerRoles();
    }

    @XmlElement
    public String getMothersMaidenName() {
        return xmlCustomer.getMothersMaidenName();
    }

    @XmlElement
    public String getNationality() {
        return xmlCustomer.getNationality();
    }

    @XmlElement
    public String getPassportExpiryDate() {
        return xmlCustomer.getPassportExpiryDate();
    }

    @XmlElement
    public String getWarehouseId() {
        return xmlCustomer.getWarehouseId();
    }

    @XmlElement
    public int getCreatedByCustomerProfileId() {
        return xmlCustomer.getCreatedByCustomerProfileId();
    }

    @XmlElement
    public String getKycStatus(){
        return xmlCustomer.getKYCStatus();
    }
    
    @XmlElement
    public String getNationalIdentityNumber() {
        return xmlCustomer.getNationalIdentityNumber();
    }
    
    public void setNationalIdentityNumber(String value) {
        xmlCustomer.setNationalIdentityNumber(value);
    }
    
    @XmlElement
    public String getIsNinVerified() {
        return xmlCustomer.getIsNinVerified();
    }
    
    public void setIsNinVerified(String value) {
        xmlCustomer.setIsNinVerified(value);
    }
    
    public void setKycStatus(String kycStatus) {
        xmlCustomer.setKYCStatus(kycStatus);
    }
    
    public void setFirstName(String value) {
        xmlCustomer.setFirstName(value);
    }

    public void setMiddleName(String value) {
        xmlCustomer.setMiddleName(value);
    }

    public void setLastName(String value) {
        xmlCustomer.setLastName(value);
    }

    public void setIdentityNumber(String value) {
        xmlCustomer.setIdentityNumber(value);
    }

    public void setIdentityNumberType(String value) {
        xmlCustomer.setIdentityNumberType(value);
    }

    public void setDateOfBirth(String value) {
        xmlCustomer.setDateOfBirth(value);
    }

    public void setGender(String value) {
        xmlCustomer.setGender(value);
    }

    public void setLanguage(String value) {
        xmlCustomer.setLanguage(value);
    }

    public void setEmailAddress(String value) {
        xmlCustomer.setEmailAddress(value);
    }

    public void setAlternativeContact1(String value) {
        xmlCustomer.setAlternativeContact1(value);
    }

    public void setAlternativeContact2(String value) {
        xmlCustomer.setAlternativeContact2(value);
    }

    public void setClassification(String value) {
        xmlCustomer.setClassification(value);
    }

    public void setSSOIdentity(String value) {
        xmlCustomer.setSSOIdentity(value);
    }

    public void setOptInLevel(int value) {
        xmlCustomer.setOptInLevel(value);
    }

    public void setMothersMaidenName(String value) {
        xmlCustomer.setMothersMaidenName(value);
    }

    public void setNationality(String value) {
        xmlCustomer.setNationality(value);
    }

    public void setPassportExpiryDate(String value) {
        xmlCustomer.setPassportExpiryDate(value);
    }

    public void setWarehouseId(String value) {
        xmlCustomer.setWarehouseId(value);
    }

}
