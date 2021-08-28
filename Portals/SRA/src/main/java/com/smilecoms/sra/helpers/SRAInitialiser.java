/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.helpers;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;
import com.smilecoms.commons.base.sd.ServiceDiscoveryAgent;
import com.smilecoms.commons.base.sd.WebSite;
import java.net.MalformedURLException;
import java.util.concurrent.ScheduledFuture;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(name = "SRAInitialiser", urlPatterns = {"/SRAInitialiser"})
public class SRAInitialiser extends HttpServlet  {
    
    private static final Logger log = LoggerFactory.getLogger(SRAInitialiser.class);
    private static ScheduledFuture runner1 = null;
    
    @Override
    public void init() {
        runner1 = Async.scheduleAtFixedRate(new SmileBaseRunnable("SRA.Initialiser") {
            @Override
            public void run() {
                trigger();
            }
        }, 5000, 60000);
    }
    
    @Override
    public void destroy() {
        Async.cancel(runner1);
    }
    
    private void trigger() {
        try {
            WebSite website = new WebSite("SRA", "http",getListeningPort(), "/sra/");
            website.setWeight(5);
            website.setTestData("isup");
            website.setTestResponseCriteria("<Done>true</Done>");
            website.setGapBetweenTestsMillis(BaseUtils.getIntPropertyFailFast("env.sd.portals.test.millis",3000));
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
