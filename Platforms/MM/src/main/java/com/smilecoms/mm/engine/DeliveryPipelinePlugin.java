/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.engine;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManagerFactory;


/**
 *
 * @author paul
 */
public interface DeliveryPipelinePlugin {
    
    public DeliveryPluginResult processMessage(BaseMessage msg,  DeliveryEngine callbackEngine);
    
    public void shutDown();
    
    public void initialise(EntityManagerFactory emf);
    
    public void propertiesChanged();
    
    public void sendDeliveryReport(DeliveryEngine.DeliveryReportStatus status, HashMap<String, Serializable> deliveryReportData);
    
}
