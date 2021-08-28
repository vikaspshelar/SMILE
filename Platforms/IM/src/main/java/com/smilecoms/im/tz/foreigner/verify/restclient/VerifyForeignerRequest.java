/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.tz.foreigner.verify.restclient;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author bhaskarhg
 */
@XmlRootElement
public class VerifyForeignerRequest {

    private String documentNo;
    private String countryCode;
    private FingerPrints[] fingerPrints;

    public String getDocumentNo() {
        return documentNo;
    }

    public void setDocumentNo(String documentNo) {
        this.documentNo = documentNo;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public FingerPrints[] getFingerPrints() {
        return fingerPrints;
    }

    public void setFingerPrints(FingerPrints[] fingerPrints) {
        this.fingerPrints = fingerPrints;
    }
    
//    private String DocumentNo;
//    private String IssuingCountry;
//    private String RThumb;
//    private String RIndex;
//    private String RMiddle;
//    private String RRing;
//    private String RLittle;
//    private String LThumb;
//    private String LIndex;
//    private String LMiddle;
//    private String LRing;
//    private String LLittle;
//
//    public String getDocumentNo() {
//        return DocumentNo;
//    }
//
//    public void setDocumentNo(String DocumentNo) {
//        this.DocumentNo = DocumentNo;
//    }
//
//    public String getIssuingCountry() {
//        return IssuingCountry;
//    }
//
//    public void setIssuingCountry(String IssuingCountry) {
//        this.IssuingCountry = IssuingCountry;
//    }
//
//    public String getRThumb() {
//        return RThumb;
//    }
//
//    public void setRThumb(String RThumb) {
//        this.RThumb = RThumb;
//    }
//
//    public String getRIndex() {
//        return RIndex;
//    }
//
//    public void setRIndex(String RIndex) {
//        this.RIndex = RIndex;
//    }
//
//    public String getRMiddle() {
//        return RMiddle;
//    }
//
//    public void setRMiddle(String RMiddle) {
//        this.RMiddle = RMiddle;
//    }
//
//    public String getRRing() {
//        return RRing;
//    }
//
//    public void setRRing(String RRing) {
//        this.RRing = RRing;
//    }
//
//    public String getRLittle() {
//        return RLittle;
//    }
//
//    public void setRLittle(String RLittle) {
//        this.RLittle = RLittle;
//    }
//
//    public String getLThumb() {
//        return LThumb;
//    }
//
//    public void setLThumb(String LThumb) {
//        this.LThumb = LThumb;
//    }
//
//    public String getLIndex() {
//        return LIndex;
//    }
//
//    public void setLIndex(String LIndex) {
//        this.LIndex = LIndex;
//    }
//
//    public String getLMiddle() {
//        return LMiddle;
//    }
//
//    public void setLMiddle(String LMiddle) {
//        this.LMiddle = LMiddle;
//    }
//
//    public String getLRing() {
//        return LRing;
//    }
//
//    public void setLRing(String LRing) {
//        this.LRing = LRing;
//    }
//
//    public String getLLittle() {
//        return LLittle;
//    }
//
//    public void setLLittle(String LLittle) {
//        this.LLittle = LLittle;
//    }
}
