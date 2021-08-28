/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mnp.tz;

import com.smilecoms.commons.sca.PortInEvent;
import com.smilecoms.commons.sca.SCAWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mukosi
 */
public class MNPWrapper extends SCAWrapper {
    
    private static final MNPWrapper myUserSpecificInstance = new MNPWrapper(false);
    private static final MNPWrapper myAdminInstance = new MNPWrapper(true);
    private static final Logger log = LoggerFactory.getLogger(MNPWrapper.class.getName());

    private MNPWrapper(boolean callAsAdministrator) {
        super(callAsAdministrator, null);
    }

    public static MNPWrapper getUserSpecificInstance() {
        return myUserSpecificInstance;
    }

    public static MNPWrapper getAdminInstance() {
        return myAdminInstance;
    }

    @Override
    public PortInEvent handlePortInEvent(PortInEvent requestObject) {
        return super.handlePortInEvent(requestObject); //To change body of generated methods, choose Tools | Templates.
    }
    
    
}
