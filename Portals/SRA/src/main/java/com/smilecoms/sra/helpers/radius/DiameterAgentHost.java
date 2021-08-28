/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.helpers.radius;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author sabza
 */
@XmlRootElement(name = "DiameterAgentHost")
@XmlAccessorType(XmlAccessType.FIELD)
public class DiameterAgentHost {

    @XmlElement(name = "Name")
    private String name;
    @XmlElement(name = "Realm")
    private String realm;
    @XmlElement(name = "AuthAppId")
    private String[] AuthAppId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getRealm() {
        return realm;
    }

    public String[] getAuthAppId() {
        return AuthAppId;
    }

    public void setAuthAppId(String[] AuthAppId) {
        this.AuthAppId = AuthAppId;
    }
}
