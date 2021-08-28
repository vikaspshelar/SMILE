/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.helpers.radius;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author sabza
 */
@XmlRootElement(name = "AccessResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class AccessResponse {

    @XmlElement(name = "SubscriptionData")
    private SubscriptionData subscriptionData;
    @XmlElement(name = "DiameterAgentHosts")
    private List<DiameterAgentHost> diameterAgentHosts;

    public List<DiameterAgentHost> getDiameterAgentHosts() {
        if (diameterAgentHosts == null) {
            diameterAgentHosts = new ArrayList<>();
        }
        return diameterAgentHosts;
    }

    public SubscriptionData getSubscriptionData() {
        return subscriptionData;
    }

    public void setSubscriptionData(SubscriptionData subscriptionData) {
        this.subscriptionData = subscriptionData;
    }

}
