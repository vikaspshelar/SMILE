/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.tz.nida.restclient;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author mukosi
 */
public class ResponseObject {

    private String DateofBirth;
    private String Photo;
    private int biometricResult;
    private String FirstName;
    private String LastName;
    private String MiddleName;
    private String NationalIDNumber;
    private String Sex;
    
    @JsonProperty("biometricResult") 
    public int getBiometricResult() {
        return biometricResult;
    }

    @JsonProperty("BiometricResult") 
    public void setBiometricResult(int biometricResult) {
        this.biometricResult = biometricResult;
    }
    
    @JsonProperty("dateofBirth")    
    public String getDateofBirth() {
        return DateofBirth;
    }

    @JsonProperty("DateofBirth")
    public void setDateofBirth(String DateofBirth) {
        this.DateofBirth = DateofBirth;
    }
    
    @JsonProperty("photo")    
    public String getPhoto() {
        return Photo;
    }
    
    @JsonProperty("Photo")
    public void setPhoto(String Photo) {
        this.Photo = Photo;
    }
    @JsonProperty("firstName")
    public String getFirstName() {
        return FirstName;
    }

    @JsonProperty("FirstName")
    public void setFirstName(String FirstName) {
        this.FirstName = FirstName;
    }
    @JsonProperty("lastName")
    public String getLastName() {
        return LastName;
    }
    @JsonProperty("LastName")
    public void setLastName(String LastName) {
        this.LastName = LastName;
    }
    @JsonProperty("middleName")
    public String getMiddleName() {
        return MiddleName;
    }
    @JsonProperty("MiddleName")
    public void setMiddleName(String MiddleName) {
        this.MiddleName = MiddleName;
    }
    @JsonProperty("nationalIDNumber")
    public String getNationalIDNumber() {
        return NationalIDNumber;
    }
    @JsonProperty("NationalIDNumber")
    public void setNationalIDNumber(String NationalIDNumber) {
        this.NationalIDNumber = NationalIDNumber;
    }
    @JsonProperty("sex")
    public String getSex() {
        return Sex;
    }
    @JsonProperty("Sex")
    public void setSex(String Sex) {
        this.Sex = Sex;
    }
    
    
        
}