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
@XmlRootElement(name = "SubscriptionData")
@XmlAccessorType(XmlAccessType.FIELD)
public class SubscriptionData {

    @XmlElement(name = "SubscriberStatus")
    private String subscriberStatus;
    @XmlElement(name = "SubscriptionIdType")
    private String subscriptionIdType;
    @XmlElement(name = "SubscriptionIdData")
    private String subscriptionIdData;

    public String getSubscriberStatus() {
        return subscriberStatus;
    }

    public void setSubscriberStatus(String subscriberStatus) {
        this.subscriberStatus = subscriberStatus;
    }

    public String getSubscriptionIdType() {
        return subscriptionIdType;
    }

    public void setSubscriptionIdType(String subscriptionIdType) {
        this.subscriptionIdType = subscriptionIdType;
    }

    public String getSubscriptionIdData() {
        return subscriptionIdData;
    }

    public void setSubscriptionIdData(String subscriptionIdData) {
        this.subscriptionIdData = subscriptionIdData;
    }
}
