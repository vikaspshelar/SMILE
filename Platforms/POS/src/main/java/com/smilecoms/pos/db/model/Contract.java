/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
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
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author mukosi
 */
@Entity
@Table(name = "contract")
@XmlRootElement
public class Contract implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "CONTRACT_ID")
    private Integer contractId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CONTRACT_START_DATE_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date contractStartDateTime;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CONTRACT_END_DATE_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date contractEndDateTime;
    @Basic(optional = false)
    @NotNull
    @Column(name = "STATUS")
    private String status;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CREATED_BY_CUSTOMER_PROFILE_ID")
    private int createdByCustomerProfileId;
    @Column(name = "CUSTOMER_PROFILE_ID")
    private Integer customerProfileId;
    @Column(name = "ORGANISATION_ID")
    private Integer organisationId;
    @Column(name = "INVOICE_CYCLE_DAY")
    private Integer invoiceCycleDay;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CONTRACT_NAME")
    private String contractName;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CREDIT_ACCOUNT_NUMBER")
    private String creditAccountNumber;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CREATED_DATE_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDateTime;
    @Basic(optional = false)
    @NotNull
    @Column(name = "LAST_MODIFIED_DATE_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastModifiedDateTime;
    @Column(name = "FULFILMENT_ITEMS_ALLOWED")
    private String fulfilmentItemsAllowed;
    @Column(name = "STAFF_MEMBERS_ALLOWED")
    private String staffMembersAllowed;
    @Column(name = "PAYMENT_METHOD")
    private String paymentMethod;
    @Column(name = "ACCOUNT_ID")
    private long accountId;

    public String getFulfilmentItemsAllowed() {
        return this.fulfilmentItemsAllowed;
    }

    public void setFulfilmentItemsAllowed(String fulfilmentItemsAllowed) {
        this.fulfilmentItemsAllowed = fulfilmentItemsAllowed;
    }

    public String getStaffMembersAllowed() {
        return this.staffMembersAllowed;
    }

    public void setStaffMembersAllowed(String staffMembersAllowed) {
        this.staffMembersAllowed = staffMembersAllowed;
    }
    
    public Contract() {
    }

    public Contract(Integer contractId) {
        this.contractId = contractId;
    }

    public Contract(Integer contractId, Date contractStartDateTime, Date contractEndDateTime, String status, String contractName, String creditAccountNumber, Date createdDateTime, Date lastModifiedDateTime) {
        this.contractId = contractId;
        this.contractStartDateTime = contractStartDateTime;
        this.contractEndDateTime = contractEndDateTime;
        this.status = status;
        this.contractName = contractName;
        this.creditAccountNumber = creditAccountNumber;
        this.createdDateTime = createdDateTime;
        this.lastModifiedDateTime = lastModifiedDateTime;
    }

    public Integer getContractId() {
        return contractId;
    }

    public void setContractId(Integer contractId) {
        this.contractId = contractId;
    }

    public Date getContractStartDateTime() {
        return contractStartDateTime;
    }

    public void setContractStartDateTime(Date contractStartDateTime) {
        this.contractStartDateTime = contractStartDateTime;
    }

    public Integer getCustomerProfileId() {
        return customerProfileId;
    }

    public void setCustomerProfileId(Integer customerProfileId) {
        this.customerProfileId = customerProfileId;
    }

    public Integer getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(Integer organisationId) {
        this.organisationId = organisationId;
    }

    public Date getContractEndDateTime() {
        return contractEndDateTime;
    }

    public void setContractEndDateTime(Date contractEndDateTime) {
        this.contractEndDateTime = contractEndDateTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCreatedByCustomerProfileId() {
        return createdByCustomerProfileId;
    }

    public void setCreatedByCustomerProfileId(int createdByCustomerProfileId) {
        this.createdByCustomerProfileId = createdByCustomerProfileId;
    }

    public String getCreditAccountNumber() {
        return creditAccountNumber;
    }

    public void setCreditAccountNumber(String creditAccountNumber) {
        this.creditAccountNumber = creditAccountNumber;
    }

    public Integer getInvoiceCycleDay() {
        return invoiceCycleDay;
    }

    public void setInvoiceCycleDay(Integer invoiceCycleDay) {
        this.invoiceCycleDay = invoiceCycleDay;
    }

    public String getContractName() {
        return contractName;
    }

    public void setContractName(String contractName) {
        this.contractName = contractName;
    }


    public Date getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(Date createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public Date getLastModifiedDateTime() {
        return lastModifiedDateTime;
    }

    public void setLastModifiedDateTime(Date lastModifiedDateTime) {
        this.lastModifiedDateTime = lastModifiedDateTime;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (contractId != null ? contractId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Contract)) {
            return false;
        }
        Contract other = (Contract) object;
        if ((this.contractId == null && other.contractId != null) || (this.contractId != null && !this.contractId.equals(other.contractId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.pos.db.model.Contract[ contractId=" + contractId + " ]";
    }
    
}
