/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.sca.beans;

import com.smilecoms.commons.base.BaseUtils;
import javax.xml.bind.annotation.XmlElement;

/**
 *
 * @author paul
 */
public class PropertyBean extends BaseBean {

    private String value;
    private String name;

    public PropertyBean() {
    }
    
    private PropertyBean(String name) {
        this.value = BaseUtils.getProperty(name);
        this.name = name;
    }

    public static PropertyBean getPropertyByName(String name) {
        return new PropertyBean(name);
    }

    @XmlElement
    public String getName() {
        return name;
    }
    
    @XmlElement
    public String getValue() {
        return value;
    }

}
