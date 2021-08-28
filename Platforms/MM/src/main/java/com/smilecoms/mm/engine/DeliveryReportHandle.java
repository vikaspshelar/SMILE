/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.engine;

import java.io.Serializable;
import java.util.HashMap;

/**
 *
 * @author paul
 */
public class DeliveryReportHandle implements Serializable {
    
    private String deliveryReportPlugin;
    private HashMap<String,Serializable> deliveryReportData;

    public DeliveryReportHandle(String deliveryReportPlugin, HashMap<String, Serializable> deliveryReportData) {
        this.deliveryReportPlugin = deliveryReportPlugin;
        this.deliveryReportData = deliveryReportData;
    }

    public String getDeliveryReportPlugin() {
        return deliveryReportPlugin;
    }

    public void setDeliveryReportPlugin(String deliveryReportPlugin) {
        this.deliveryReportPlugin = deliveryReportPlugin;
    }

    public HashMap<String, Serializable> getDeliveryReportData() {
        return deliveryReportData;
    }

    public void setDeliveryReportData(HashMap<String, Serializable> deliveryReportData) {
        this.deliveryReportData = deliveryReportData;
    }
    
}


