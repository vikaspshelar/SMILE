/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.am.db.model;

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
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author mukosi
 */
@Entity
@Table(name = "photograph")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Photograph.findAll", query = "SELECT p FROM Photograph p"),
    @NamedQuery(name = "Photograph.findByPhotoGuid", query = "SELECT p FROM Photograph p WHERE p.photoGuid = :photoGuid"),
    @NamedQuery(name = "Photograph.findByPhotoType", query = "SELECT p FROM Photograph p WHERE p.photoType = :photoType"),
    @NamedQuery(name = "Photograph.findByPhotoHash", query = "SELECT p FROM Photograph p WHERE p.photoHash = :photoHash"),
    @NamedQuery(name = "Photograph.findByContractId", query = "SELECT p FROM Photograph p WHERE p.contractId = :contractId"),
    @NamedQuery(name = "Photograph.findByServiceInstanceId", query = "SELECT p FROM Photograph p WHERE p.serviceInstanceId = :serviceInstanceId"),
    @NamedQuery(name = "Photograph.findByPortingOrderId", query = "SELECT p FROM Photograph p WHERE p.portingOrderId = :portingOrderId")})
public class Photograph implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "PHOTO_GUID")
    private String photoGuid;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "PHOTO_TYPE")
    private String photoType;
    @Lob
    @Column(name = "DATA")
    private byte[] data;
    @Size(max = 200)
    @Column(name = "PHOTO_HASH")
    private String photoHash;
    @Column(name = "CONTRACT_ID")
    private Integer contractId;
    @Column(name = "SERVICE_INSTANCE_ID")
    private Integer serviceInstanceId;
    @Column(name = "PORTING_ORDER_ID")
    private String portingOrderId;

    public Photograph() {
    }

    public Photograph(String photoGuid) {
        this.photoGuid = photoGuid;
    }

    public Photograph(String photoGuid, String photoType) {
        this.photoGuid = photoGuid;
        this.photoType = photoType;
    }

    public String getPhotoGuid() {
        return photoGuid;
    }

    public void setPhotoGuid(String photoGuid) {
        this.photoGuid = photoGuid;
    }

    public String getPhotoType() {
        return photoType;
    }

    public void setPhotoType(String photoType) {
        this.photoType = photoType;
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

    public Integer getContractId() {
        return contractId;
    }

    public void setContractId(Integer contractId) {
        this.contractId = contractId;
    }

    public Integer getServiceInstanceId() {
        return serviceInstanceId;
    }

    public void setServiceInstanceId(Integer serviceInstanceId) {
        this.serviceInstanceId = serviceInstanceId;
    }

    public String getPortingOrderId() {
        return portingOrderId;
    }

    public void setPortingOrderId(String portingOrderId) {
        this.portingOrderId = portingOrderId;
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
        return "com.smilecoms.am.db.model.Photograph[ photoGuid=" + photoGuid + " ]";
    }
    
}
