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
public class FingerPrints {

    private String code;
    private String image;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
    
}
