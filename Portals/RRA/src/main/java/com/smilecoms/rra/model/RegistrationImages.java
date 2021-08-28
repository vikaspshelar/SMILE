/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.rra.model;

import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author abhilash
 */
@XmlRootElement
public class RegistrationImages {

    public RegistrationImages() {
    }
        
    private List<RegistrationImage> registrationImage;

    public List<RegistrationImage> getRegistrationImage() {
        return registrationImage;
    }

    public void setRegistrationImage(List<RegistrationImage> registrationImage) {
        this.registrationImage = registrationImage;
    }

    
}
