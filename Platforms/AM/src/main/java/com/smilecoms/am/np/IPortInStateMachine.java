/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.am.np;

import com.smilecoms.xml.schema.am.PortInEvent;
import java.util.Properties;
import javax.persistence.EntityManager;

/**
 *
 * @author mukosi
 */
public interface IPortInStateMachine {
    
    
    public void init();
    public IPortState getState(String stateId);
    public PortInEvent handleState(EntityManager em, PortInEvent request) throws Exception  ;
    // public IPortState getCurrentState(EntityManager em, String orderId) throws Exception;
    public PortInEvent savePortState(EntityManager em, IPortState state) throws Exception;
   
}
