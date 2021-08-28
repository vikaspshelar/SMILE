/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sep.helpers;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;
import com.smilecoms.commons.base.sd.ServiceDiscoveryAgent;
import com.smilecoms.commons.base.sd.WebSite;
import com.smilecoms.commons.localisation.LocalisationHelper;
import com.smilecoms.commons.sca.SCAAuthenticatorImpl;
import com.smilecoms.commons.scaauth.SCAAuthenticator;
import com.smilecoms.commons.scaauth.SCASPILoginModule;
import com.smilecoms.commons.util.Utils;
import java.net.MalformedURLException;
import java.util.concurrent.ScheduledFuture;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
@WebServlet(name = "SEPInitialiser", urlPatterns = {"/SEPInitialiser"})
public class SEPInitialiser extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(SEPInitialiser.class);
    private static ScheduledFuture runner1 = null;

    @Override
    public void init() {
        log.warn("In SEPInitialiser Init");
        
        /**
         * Provide for a callback mechanism for our custom auth module to be
         * able to call up the classloading hierarchy into smile commons SCA
         * client
         */
        try {
            BaseUtils b = new BaseUtils();
            SCAAuthenticator auth = new SCAAuthenticatorImpl();
            // TomEE auth module
            SCASPILoginModule.setAuthenticator(auth);
        } catch (Throwable e) {
            log.warn("Error initialising SEP: ", e);
        }
        
        // Initialise localisation ASAP to avoid long startup delays on first request
        Async.makeHappen(new Runnable() {
            @Override
            public void run() {
                LocalisationHelper.init();
            }
        }, 2000);
        
        runner1 = Async.scheduleAtFixedRate(new SmileBaseRunnable("SEP.Initialiser") {
            @Override
            public void run() {
                trigger();
            }
        }, 5000, 60000);
    }

    @Override
    public void destroy() {
        //stop the CTI App Thread - we dont worry about websocket threads as they are per socket and 
        //die when each socket is closed
        Async.cancel(runner1);
    }

    private void trigger() {
        try {
            WebSite website = new WebSite("SEP", "http", getListeningPort(), "/sep/");
            website.setWeight(5);
            website.setTestData("Login.action?isup=");
            website.setTestResponseCriteria("Yes SEP is UP");
            website.setGapBetweenTestsMillis(BaseUtils.getIntPropertyFailFast("env.sd.portals.test.millis", 3000));
            website.setClientHostnameRegexMatch(".*");
            website.setTestTimeoutMillis(1000);
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
