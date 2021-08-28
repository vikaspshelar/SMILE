/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.am.np;

import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.base.BaseUtils;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mukosi
 */
@Singleton
@Startup
public class PortInEventHandler implements BaseListener {
    private static final Logger log = LoggerFactory.getLogger(PortInEventHandler.class);
    
    private static IPortInStateMachine portInStateMachine = null;
    private static IPortInStateMachine portOutStateMachine = null;

    //public PortInEventHandler() {
    //}
    
        
    public  static IPortInStateMachine getPortInStateMachine() throws Exception {
        if(portInStateMachine == null) {
            Class portInStateMachineClass = PortInEventHandler.class.getClassLoader().loadClass(BaseUtils.getProperty("env.mnp.portin.statemachine.class"));
            portInStateMachine = (IPortInStateMachine) portInStateMachineClass.newInstance();
            portInStateMachine.init(); // Initialize state machine and load properties.
        }   
        return portInStateMachine;
    }
    
    public static IPortInStateMachine getPortOutStateMachine() throws Exception  {
        if(portOutStateMachine == null) {
            Class portOutStateMachineClass = PortInEventHandler.class.getClassLoader().loadClass(BaseUtils.getProperty("env.mnp.portout.statemachine.class"));
            portOutStateMachine = (IPortInStateMachine) portOutStateMachineClass.newInstance();
            portOutStateMachine.init(); // Initialize state machine and load properties.
        }     
        return portOutStateMachine;
    }

    @Override
    public void propsAreReadyTrigger() {
        MnpHelper.initialise();
        BaseUtils.registerForPropsChanges(this);
        BaseUtils.deregisterForPropsAvailability(this);
    }

    @Override
    public void propsHaveChangedTrigger() {
        MnpHelper.initialise();
    }
    
    @PostConstruct
    public void startUp() {
        log.warn("PortinEventHandler starting up.");
        BaseUtils.registerForPropsAvailability(this); 
        
    }

    @PreDestroy
    public void shutDown() {
        log.warn("PortinEventHandler shutting down.");
        BaseUtils.deregisterForPropsAvailability(this);
        BaseUtils.deregisterForPropsChanges(this);
    }

}
