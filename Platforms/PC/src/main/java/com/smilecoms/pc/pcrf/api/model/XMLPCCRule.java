package com.smilecoms.pc.pcrf.api.model;

import com.smilecoms.pc.pcrf.PCCRule;
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
@XmlRootElement(name = "xmlpccrule")
@XmlAccessorType (XmlAccessType.FIELD)
public class XMLPCCRule implements PCCRule {
    private String pccRuleName;
    private String bindingIdentifier;
    private int type;

    @Override
    public String getPccRuleName() {
        return pccRuleName;
    }

    @Override
    public void setPccRuleName(String pccRuleName) {
        this.pccRuleName = pccRuleName;
    }

    @Override
    public String getBindingIdentifier() {
        return bindingIdentifier;
    }

    @Override
    public void setBindingIdentifier(String bindingIdentifier) {
        this.bindingIdentifier = bindingIdentifier;
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
