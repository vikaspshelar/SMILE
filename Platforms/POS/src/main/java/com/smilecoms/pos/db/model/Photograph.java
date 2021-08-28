/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos.db.model;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "photograph", catalog = "SmileDB", schema = "")
@NamedQueries({
    @NamedQuery(name = "Photograph.findAll", query = "SELECT p FROM Photograph p")})
public class Photograph implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "PHOTO_GUID")
    private String photoGuid;
    @Column(name = "CUSTOMER_PROFILE_ID")
    private Integer customerProfileId;
    @Column(name = "CONTRACT_ID")
    private Integer contractId;
    @Column(name = "ORGANISATION_ID")
    private Integer organisationId;
    @Column(name = "SERVICE_INSTANCE_ID")
    private Integer serviceInstanceId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PHOTO_TYPE")
    private String photoType;
    @Basic(optional = true)
    @Lob
    @Column(name = "DATA")
    private byte[] data;
    @Column(name = "PHOTO_HASH")
    private String photoHash;

    public Photograph() {
    }

    public Photograph(String photoGuid) {
        this.photoGuid = photoGuid;
    }

    public Photograph(String photoGuid, int customerProfileId, String photoType) {
        this.photoGuid = photoGuid;
        this.customerProfileId = customerProfileId;
        this.photoType = photoType;
    }

    public String getPhotoGuid() {
        return photoGuid;
    }

    public void setPhotoGuid(String photoGuid) {
        this.photoGuid = photoGuid;
    }

    public Integer getCustomerProfileId() {
        return customerProfileId;
    }

    public void setCustomerProfileId(Integer customerProfileId) {
        this.customerProfileId = customerProfileId;
    }

    public Integer getServiceInstanceId() {
        return serviceInstanceId;
    }

    public void setServiceInstanceId(Integer serviceInstanceId) {
        this.serviceInstanceId = serviceInstanceId;
    }
    
    public Integer getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(Integer organisationId) {
        this.organisationId = organisationId;
    }

    public String getPhotoType() {
        return photoType;
    }

    public void setPhotoType(String photoType) {
        this.photoType = photoType;
    }

    public Integer getContractId() {
        return contractId;
    }

    public void setContractId(Integer contractId) {
        this.contractId = contractId;
    }
    
    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getPhotoHash() {
        return photoHash;
    }

    public void setPhotoHash(String photoHash) {
        this.photoHash = photoHash;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (photoGuid != null ? photoGuid.hashCode() : 0);
        return hash;
    }
    

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Photograph)) {
            return false;
        }
        Photograph other = (Photograph) object;
        if ((this.photoGuid == null && other.photoGuid != null) || (this.photoGuid != null && !this.photoGuid.equals(other.photoGuid))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.im.db.model.Photograph[ photoGuid=" + photoGuid + " ]";
    }
}
