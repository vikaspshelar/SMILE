package com.smilecoms.pc.pcrf.api.model;

import com.smilecoms.pc.pcrf.AFSession;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author richard good
 * 
 * This is the model for passing IPCANSession inforrmation over HTTP API 
 * 
 */
@XmlRootElement(name = "xmlafsession")
@XmlAccessorType (XmlAccessType.FIELD)
public class XMLAFSession implements AFSession {

    
    private String bindingIdentifier;
    private String rxServerSessionId;
    private String state;
    private int type;

    @Override
    public String getBindingIdentifier() {
        return bindingIdentifier;
    }

    @Override
    public void setBindingIdentifier(String bindingIdentifier) {
        this.bindingIdentifier = bindingIdentifier;
    }

    @Override
    public String getRxServerSessionId() {
        return rxServerSessionId;
    }

    @Override
    public void setRxServerSessionId(String gxServerSessionId) {
        this.rxServerSessionId = gxServerSessionId;
    }

    @Override
    public String getState() {
        return state;
    }

    @Override
    public void setState(String state) {
        this.state = state;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public void setType(int type) {
        this.type = type;
    }
}
