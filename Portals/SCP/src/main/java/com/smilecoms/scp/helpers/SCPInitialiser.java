/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.scp.helpers;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;

import com.smilecoms.commons.base.sd.ServiceDiscoveryAgent;
import com.smilecoms.commons.base.sd.WebSite;
import com.smilecoms.commons.localisation.LocalisationHelper;
import com.smilecoms.commons.sca.SCAAuthenticatorImpl;
import com.smilecoms.commons.scaauth.SCAAuthenticator;
import com.smilecoms.commons.scaauth.SCASPILoginModule;
import java.net.MalformedURLException;
import java.util.concurrent.ScheduledFuture;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author lesiba
 */
@WebServlet(name = "SCPInitialiser", urlPatterns = {"/SCPInitialiser"})
public class SCPInitialiser extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(SCPInitialiser.class);
    private static ScheduledFuture runner1 = null;

    @Override
    public void init() {
        /**
         * Provide for a callback mechanism for our custom auth module to be
         * able to call up the classloading hierarchy into smile commons SCA
         * client
         */
        
        BaseUtils b = new BaseUtils();
        SCAAuthenticator auth = new SCAAuthenticatorImpl();
        SCASPILoginModule.setAuthenticator(auth);
        runner1 = Async.scheduleAtFixedRate(new SmileBaseRunnable("SCP.Initialiser") {
            @Override
            public void run() {
                trigger();
            }
        }, 5000, 60000);
        
        // Initialise localisation ASAP to avoid long startup delays on first request
        Async.makeHappen(new Runnable() {
            @Override
            public void run() {
                LocalisationHelper.init();
            }
        }, 2000);
    }

    @Override
    public void destroy() {
        Async.cancel(runner1);
    }

    private void trigger() {
        try {
            WebSite website = new WebSite("SCP", "http", getListeningPort(), "/scp/");
            website.setWeight(5);
            website.setTestData("Login.action?isup=");
            website.setTestResponseCriteria("Yes SCP is UP");
            website.setGapBetweenTestsMillis(BaseUtils.getIntPropertyFailFast("env.sd.portals.test.millis", 3000));
            website.setClientHostnameRegexMatch(".*");
            website.setTestTimeoutMillis(500);
            ServiceDiscoveryAgent.getInstance().publishService(website);
        } catch (MalformedURLException ex) {
            log.warn("Error publishing website service: ", ex);
        }
    }
    private static int listeningPort = 0;

    public static int getListeningPort() {
        if (listeningPort > 0) {
            return listeningPort;
        }
        listeningPort = 8003;
        if (System.getProperty("HTTP_BIND_PORT") != null) {
            listeningPort = Integer.parseInt(System.getProperty("HTTP_BIND_PORT"));
        }
        log.warn("Websites are listening on port {}", listeningPort);
        return listeningPort;
    }
}
