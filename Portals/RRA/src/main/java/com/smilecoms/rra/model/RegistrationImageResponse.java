/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.rra.model;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author abhilash
 */
@XmlRootElement
public class RegistrationImageResponse {

    public RegistrationImageResponse() {
    }
    
    private String msisdn;
    private String iccid;
    private RegistrationImages registrationImages;

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public String getIccid() {
        return iccid;
    }

    public void setIccid(String iccid) {
        this.iccid = iccid;
    }

    public RegistrationImages getRegistrationImages() {
        return registrationImages;
    }

    public void setRegistrationImages(RegistrationImages registrationImages) {
        this.registrationImages = registrationImages;
    }

        
}
