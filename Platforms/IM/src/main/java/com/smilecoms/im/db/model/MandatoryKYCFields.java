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
 * @author bhaskarhg
 */
@Entity
@Table(name = "mandatorykycfields", catalog = "SmileDB", schema = "")
public class MandatoryKYCFields implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Basic(optional = false)
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "customerId")
    private Integer customerId;
    @Column(name = "titleVerified")
    private String titleVerified;
    @Column(name = "nameVerified")
    private String nameVerified;
    @Column(name = "mobileVerified")
    private String mobileVerified;
    @Column(name = "emailVerified")
    private String emailVerified;
    @Column(name = "genderVerified")
    private String genderVerified;
    @Column(name = "dobVerified")
    private String dobVerified;
    @Column(name = "nationalityVerified")
    private String nationalityVerified;
    @Column(name = "physicalAddressVerified")
    private String physicalAddressVerified;
    @Column(name = "facialPitureVerified")
    private String facialPitureVerified;
    @Column(name = "validIdCardVerified")
    private String validIdCardVerified;
    @Column(name = "fingerPrintVerified")
    private String fingerPrintVerified;

    public MandatoryKYCFields() {
    }

    public MandatoryKYCFields(Integer id) {
        this.id = id;
    }

    public MandatoryKYCFields(Integer id, Integer customerId, String titleVerified, String nameVerified, String mobileVerified, String emailVerified, String genderVerified, String dobVerified, String nationalityVerified, String physicalAddressVerified, String facialPitureVerified, String validIdCardVerified, String fingerPrintVerified) {
        this.id = id;
        this.customerId = customerId;
        this.titleVerified = titleVerified;
        this.nameVerified = nameVerified;
        this.mobileVerified = mobileVerified;
        this.emailVerified = emailVerified;
        this.genderVerified = genderVerified;
        this.dobVerified = dobVerified;
        this.nationalityVerified = nationalityVerified;
        this.physicalAddressVerified = physicalAddressVerified;
        this.facialPitureVerified = facialPitureVerified;
        this.validIdCardVerified = validIdCardVerified;
        this.fingerPrintVerified = fingerPrintVerified;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public String getTitleVerified() {
        return titleVerified;
    }

    public void setTitleVerified(String titleVerified) {
        this.titleVerified = titleVerified;
    }

    public String getNameVerified() {
        return nameVerified;
    }

    public void setNameVerified(String nameVerified) {
        this.nameVerified = nameVerified;
    }

    public String getMobileVerified() {
        return mobileVerified;
    }

    public void setMobileVerified(String mobileVerified) {
        this.mobileVerified = mobileVerified;
    }

    public String getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(String emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getGenderVerified() {
        return genderVerified;
    }

    public void setGenderVerified(String genderVerified) {
        this.genderVerified = genderVerified;
    }

    public String getDobVerified() {
        return dobVerified;
    }

    public void setDobVerified(String dobVerified) {
        this.dobVerified = dobVerified;
    }

    public String getNationalityVerified() {
        return nationalityVerified;
    }

    public void setNationalityVerified(String nationalityVerified) {
        this.nationalityVerified = nationalityVerified;
    }

    public String getPhysicalAddressVerified() {
        return physicalAddressVerified;
    }

    public void setPhysicalAddressVerified(String physicalAddressVerified) {
        this.physicalAddressVerified = physicalAddressVerified;
    }

    public String getFacialPitureVerified() {
        return facialPitureVerified;
    }

    public void setFacialPitureVerified(String facialPitureVerified) {
        this.facialPitureVerified = facialPitureVerified;
    }

    public String getValidIdCardVerified() {
        return validIdCardVerified;
    }

    public void setValidIdCardVerified(String validIdCardVerified) {
        this.validIdCardVerified = validIdCardVerified;
    }

    public String getFingerPrintVerified() {
        return fingerPrintVerified;
    }

    public void setFingerPrintVerified(String fingerPrintVerified) {
        this.fingerPrintVerified = fingerPrintVerified;
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
        if (!(object instanceof MandatoryKYCFields)) {
            return false;
        }
        MandatoryKYCFields other = (MandatoryKYCFields) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.im.db.model.MandatoryKYCFields[ id=" + id + " ]";
    }
}
