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
 
@XmlRootElement(name = "xmlipcansessions")
@XmlAccessorType (XmlAccessType.FIELD)
public class XMLIPCANSessions 
{
    @XmlElement(name = "xmlipcansession")
    private List<XMLIPCANSession> XMLIPCANSessions = null;
 
    public List<XMLIPCANSession> getXMLIPCANSessions() {
        return XMLIPCANSessions;
    }
 
    public void setXMLIPCANSessions(List<XMLIPCANSession> XMLIPCANSessions) {
        this.XMLIPCANSessions = XMLIPCANSessions;
    }
}