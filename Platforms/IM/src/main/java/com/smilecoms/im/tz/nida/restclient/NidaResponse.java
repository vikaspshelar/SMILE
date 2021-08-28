/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.tz.nida.restclient;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 *
 * @author mukosi
 */
@JsonDeserialize(using = NidaResponseDeserializer.class)
public class NidaResponse {
    
    private String id;
    private String code;
    private String firstName;
    private String middleName;
    private String otherNames;
    private String lastName;
    private String gender;
    private String dateOfBirth;
    private String placeOfBirth;
    private String residentRegion;
    private String residentDistrict;
    private String residentWard;
    private String residentVillage;
    private String residentStreet;
    private String residentHouseNo;
    private String residentPostalAddress;
    private String residentPostCode;
    private String birthCountry;
    private String birthRegion;
    private String birthDistrict;
    private String birthWard;
    private String birthCertificateNo;
    private String nationality;
    private String phoneNumber;
    private String photo;
    private String signature;
    private String status;   
    
    
    public String getTransactionId() {
        return id;
    }

    public void setTransactionId(String id) {
        this.id = id;
    }
    
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    
    private ResponseObject responseObject;

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public String getOtherNames() {
        return otherNames;
    }

    public void setOtherNames(String otherNames) {
        this.otherNames = otherNames;
    }

    public String getGender() {
        if(gender == null) {
            return "";
        } else 
            if(gender.equalsIgnoreCase("MALE")) {
                return "M";
            } else 
                if(gender.equalsIgnoreCase("FEMALE")) {
                    return "F";
                } else {
                    return  "";
                }        
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDateOfBirth() {
        //  getCustomer().setDateOfBirth(getCustomer().getDateOfBirth().replace("/", ""));
        // "dateOfBirth":"1901-01-10",
        return dateOfBirth.replace("-",  "/");
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getPlaceOfBirth() {
        return placeOfBirth;
    }

    public void setPlaceOfBirth(String placeOfBirth) {
        this.placeOfBirth = placeOfBirth;
    }

    public String getResidentRegion() {
        return residentRegion;
    }

    public void setResidentRegion(String residentRegion) {
        this.residentRegion = residentRegion;
    }

    public String getResidentDistrict() {
        return residentDistrict;
    }

    public void setResidentDistrict(String residentDistrict) {
        this.residentDistrict = residentDistrict;
    }

    public String getResidentWard() {
        return residentWard;
    }

    public void setResidentWard(String residentWard) {
        this.residentWard = residentWard;
    }

    public String getResidentVillage() {
        return residentVillage;
    }

    public void setResidentVillage(String residentVillage) {
        this.residentVillage = residentVillage;
    }

    public String getResidentStreet() {
        return residentStreet;
    }

    public void setResidentStreet(String residentStreet) {
        this.residentStreet = residentStreet;
    }

    public String getResidentHouseNo() {
        return residentHouseNo;
    }

    public void setResidentHouseNo(String residentHouseNo) {
        this.residentHouseNo = residentHouseNo;
    }

    public String getResidentPostalAddress() {
        return residentPostalAddress;
    }

    public void setResidentPostalAddress(String residentPostalAddress) {
        this.residentPostalAddress = residentPostalAddress;
    }

    public String getResidentPostCode() {
        return residentPostCode;
    }

    public void setResidentPostCode(String residentPostCode) {
        this.residentPostCode = residentPostCode;
    }

    public String getBirthCountry() {
        return birthCountry;
    }

    public void setBirthCountry(String birthCountry) {
        this.birthCountry = birthCountry;
    }

    public String getBirthRegion() {
        return birthRegion;
    }

    public void setBirthRegion(String birthRegion) {
        this.birthRegion = birthRegion;
    }

    public String getBirthDistrict() {
        return birthDistrict;
    }

    public void setBirthDistrict(String birthDistrict) {
        this.birthDistrict = birthDistrict;
    }

    public String getBirthWard() {
        return birthWard;
    }

    public void setBirthWard(String birthWard) {
        this.birthWard = birthWard;
    }

    public String getBirthCertificateNo() {
        return birthCertificateNo;
    }

    public void setBirthCertificateNo(String birthCertificateNo) {
        this.birthCertificateNo = birthCertificateNo;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
    
    

    public ResponseObject getResponseObject() {
        return responseObject;
    }

    public void setResponseObject(ResponseObject responseObject) {
        this.responseObject = responseObject;
    }
}

 