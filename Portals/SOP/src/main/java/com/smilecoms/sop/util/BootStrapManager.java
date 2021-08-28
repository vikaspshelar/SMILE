/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sop.util;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.SCAAuthenticatorImpl;
import com.smilecoms.commons.scaauth.SCAAuthenticator;
import com.smilecoms.commons.scaauth.SCASPILoginModule;
import com.smilecoms.sop.helpers.SyslogStatsSnapshot;
import com.smilecoms.sop.vmware.InfrastructureMonitor;

/**
 *
 * @author paul
 */
public class BootStrapManager {

    private static SyslogStatsSnapshot sss = null;
    private static InfrastructureMonitor im = null;

    public static void initialiseSOP() {
        sss = new SyslogStatsSnapshot();
        im = new InfrastructureMonitor();
        SCAAuthenticator auth = new SCAAuthenticatorImpl();
        // TomEE auth module
        SCASPILoginModule.setAuthenticator(auth);
    }

    public static void shutdownSOP() {
        if (sss != null) {
            sss.shutdown();
        }
        sss = null;
        if (im != null) {
            im.shutdown();
        }
        im = null;

    }
}
