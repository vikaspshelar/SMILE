/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pc.pcrf.api.model;

import java.util.List;
 
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
 
@XmlRootElement(name = "xmlpccrules")
@XmlAccessorType (XmlAccessType.FIELD)
public class XMLPCCRules
{
    @XmlElement(name = "xmlpccrule")
    private List<XMLPCCRule> XMLPCCRules = null;
 
    public List<XMLPCCRule> getXMLPCCRules() {
        return XMLPCCRules;
    }
 
    public void setXMLPCCRules(List<XMLPCCRule> XMLPCCRules) {
        this.XMLPCCRules = XMLPCCRules;
    }
}