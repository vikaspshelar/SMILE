package com.smilecoms.pc.pcrf.api.model;

import com.smilecoms.pc.pcrf.IPCANSession;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author richard good
 
 This is the model for passing XMLIPCANSession inforrmation over HTTP API 
 * 
 */
@XmlRootElement(name = "xmlipcansession")
@XmlAccessorType (XmlAccessType.FIELD)
public class XMLIPCANSession implements IPCANSession {

    private String bindingIdentifier;
    private String gxServerSessionId;
    private String state;
    private String calledStationId;
    private int highestPriorityServiceId;
    private String endUserPrivate;

    @Override
    public String getBindingIdentifier() {
        return bindingIdentifier;
    }

    @Override
    public void setBindingIdentifier(String bindingIdentifier) {
        this.bindingIdentifier = bindingIdentifier;
    }

    @Override
    public String getGxServerSessionId() {
        return gxServerSessionId;
    }

    @Override
    public void setGxServerSessionId(String gxServerSessionId) {
        this.gxServerSessionId = gxServerSessionId;
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
    public String getCalledStationId() {
        return calledStationId;
    }

    @Override
    public void setCalledStationId(String calledStationId) {
        this.calledStationId = calledStationId;
    }

    @Override
    public int getHighestPriorityServiceId() {
        return highestPriorityServiceId;
    }

    @Override
    public void setHighestPriorityServiceId(int highestPriorityServiceId) {
        this.highestPriorityServiceId = highestPriorityServiceId;
    }

    @Override
    public String getEndUserPrivate() {
        return endUserPrivate;
    }

    @Override
    public void setEndUserPrivate(String endUserPrivate) {
        this.endUserPrivate = endUserPrivate;
    }
}
