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
 
@XmlRootElement(name = "xmlafsessions")
@XmlAccessorType (XmlAccessType.FIELD)
public class XMLAFSessions
{
    @XmlElement(name = "xmlafsession")
    private List<XMLAFSession> XMLAFSessions = null;
 
    public List<XMLAFSession> getXMLAFSessions() {
        return XMLAFSessions;
    }
 
    public void setXMLAFSessions(List<XMLAFSession> XMLAFSessions) {
        this.XMLAFSessions = XMLAFSessions;
    }
}