/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.db.model;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 *
 * @author user
 */
@Entity
@Table(name = "products_kyc_verification", catalog = "SmileDB", schema = "")
public class ProductsKycVerification implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Basic(optional = false)
    @Column(name = "kyc_id")
    private Integer kycId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "customer_id")
    private Integer customerProfileId;
    @Column(name = "nin")
    private String nin;
    @Column(name = "created_date")
    private String createdDate;
    @Column(name = "status")
    private String status;
    @Column(name = "verified_by")
    private String verifiedBy;    
    @Column(name = "verification_date")
    private String verificationDate;
    @Column(name = "sales_person")
    private String salesPerson;

    public ProductsKycVerification() {
    }
    
    
    public ProductsKycVerification(Integer kycId, Integer customerProfileId, String nin, String createdDate, String status, String verifiedBy, String verificationDate, String salesPerson) {
        this.kycId = kycId;
        this.customerProfileId = customerProfileId;
        this.nin = nin;
        this.createdDate = createdDate;
        this.status = status;
        this.verifiedBy = verifiedBy;
        this.verificationDate = verificationDate;
        this.salesPerson = salesPerson;
    }

    public Integer getKycId() {
        return kycId;
    }

    public void setKycId(Integer id) {
        this.kycId = id;
    }

    public Integer getCustomerProfileId() {
        return customerProfileId;
    }

    public void setCustomerProfileId(Integer customerProfileId) {
        this.customerProfileId = customerProfileId;
    }

    public String getNin() {
        return nin;
    }

    public void setNin(String nin) {
        this.nin = nin;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public String getVerificationDate() {
        return verificationDate;
    }

    public void setVerificationDate(String verificationDate) {
        this.verificationDate = verificationDate;
    }

    public String getSalesPerson() {
        return salesPerson;
    }

    public void setSalesPerson(String salesPerson) {
        this.salesPerson = salesPerson;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 19 * hash + Objects.hashCode(this.kycId);
        hash = 19 * hash + Objects.hashCode(this.customerProfileId);
        hash = 19 * hash + Objects.hashCode(this.nin);
        hash = 19 * hash + Objects.hashCode(this.createdDate);
        hash = 19 * hash + Objects.hashCode(this.status);
        hash = 19 * hash + Objects.hashCode(this.verifiedBy);
        hash = 19 * hash + Objects.hashCode(this.verificationDate);
        hash = 19 * hash + Objects.hashCode(this.salesPerson);
        return hash;
    }

    @Override
    public String toString() {
        return "CustomerNinData{" + "id=" + kycId + ", customerProfileId=" + customerProfileId + ", nin=" + nin + ", createdDate=" + createdDate + ", status=" + status + ", verifiedBy=" + verifiedBy + ", verificationDate=" + verificationDate + ", salesPerson=" + salesPerson + '}';
    }
    
}
