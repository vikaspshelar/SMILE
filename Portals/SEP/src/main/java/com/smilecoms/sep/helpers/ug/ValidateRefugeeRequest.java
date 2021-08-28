/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sep.helpers.ug;

/**
 *
 * @author rajeshkumar
 */
public class ValidateRefugeeRequest {
    String individualId;
    String sex;
    Integer yearOfBirth;
    String fingerprint;

    public String getIndividualId() {
        return individualId;
    }

    public void setIndividualId(String individualId) {
        this.individualId = individualId;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public Integer getYearOfBirth() {
        return yearOfBirth;
    }

    public void setYearOfBirth(Integer yearOfBirth) {
        this.yearOfBirth = yearOfBirth;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    @Override
    public String toString() {
        return "ValidateRefugeeRequest{" + "individualId=" + individualId + ", sex=" + sex + ", yearOfBirth=" + yearOfBirth + ", fingerprint=" + fingerprint + '}';
    }
}
