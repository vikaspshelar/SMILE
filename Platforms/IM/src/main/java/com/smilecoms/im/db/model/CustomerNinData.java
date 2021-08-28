/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.db.model;

import java.io.Serializable;
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
@Table(name = "customer_nin_data", catalog = "SmileDB", schema = "")
public class CustomerNinData implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Basic(optional = false)
    @Column(name = "nin_data_id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "customer_profile_id")
    private Integer customerProfileId;
    @Column(name = "nin_verification_tracking_id")
    private String ninVerificationTrackingId;
    @Column(name = "nin_verification_type")
    private String ninVerificationType;
    @Column(name = "nin_verified_date")
    private String ninVerifiedDate;
    @Column(name = "nin_response_status")
    private String ninResponseStatus;
    @Column(name = "nin_collection_date")
    private String ninCollectionDate;

    public CustomerNinData() {
    }

    public CustomerNinData(Integer id) {
        this.id = id;
    }

    public CustomerNinData(Integer id, Integer customerProfileId, String ninVerificationTrackingId, String ninVerificationType, String ninVerifiedDate, String ninResponseStatus, String ninCollectionDate) {
        this.id = id;
        this.customerProfileId = customerProfileId;
        this.ninVerificationTrackingId = ninVerificationTrackingId;
        this.ninVerificationType = ninVerificationType;
        this.ninVerifiedDate = ninVerifiedDate;
        this.ninResponseStatus = ninResponseStatus;
        this.ninCollectionDate = ninCollectionDate;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCustomerProfileId() {
        return customerProfileId;
    }

    public void setCustomerProfileId(Integer customerProfileId) {
        this.customerProfileId = customerProfileId;
    }

    public String getNinVerificationTrackingId() {
        return ninVerificationTrackingId;
    }

    public void setNinVerificationTrackingId(String ninVerificationTrackingId) {
        this.ninVerificationTrackingId = ninVerificationTrackingId;
    }

    public String getNinVerificationType() {
        return ninVerificationType;
    }

    public void setNinVerificationType(String ninVerificationType) {
        this.ninVerificationType = ninVerificationType;
    }

    public String getNinVerifiedDate() {
        return ninVerifiedDate;
    }

    public void setNinVerifiedDate(String ninVerifiedDate) {
        this.ninVerifiedDate = ninVerifiedDate;
    }

    public String getNinResponseStatus() {
        return ninResponseStatus;
    }

    public void setNinResponseStatus(String ninResponseStatus) {
        this.ninResponseStatus = ninResponseStatus;
    }

    public String getNinCollectionDate() {
        return ninCollectionDate;
    }

    public void setNinCollectionDate(String ninCollectionDate) {
        this.ninCollectionDate = ninCollectionDate;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CustomerNinData)) {
            return false;
        }
        CustomerNinData other = (CustomerNinData) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "CustomerNinData{" + "id=" + id + ", customerProfileId=" + customerProfileId + ", ninVerificationTrackingId=" + ninVerificationTrackingId + ", ninVerificationType=" + ninVerificationType + ", ninVerifiedDate=" + ninVerifiedDate + ", ninResponseStatus=" + ninResponseStatus + ", ninCollectionDate=" + ninCollectionDate + '}';
    }
    
    
}
