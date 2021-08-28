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
public class RegistrationImage {

    public RegistrationImage() {
    }
    
    private String imageType;
    private String imageContent;
    private String imageFormat;

    public String getImageType() {
        return imageType;
    }

    public void setImageType(String imageType) {
        this.imageType = imageType;
    }

    public String getImageContent() {
        return imageContent;
    }

    public void setImageContent(String imageContent) {
        this.imageContent = imageContent;
    }

    public String getImageFormat() {
        return imageFormat;
    }

    public void setImageFormat(String imageFormat) {
        this.imageFormat = imageFormat;
    }
        
}
