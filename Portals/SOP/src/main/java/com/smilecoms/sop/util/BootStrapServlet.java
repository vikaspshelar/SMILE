/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sop.util;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;
import com.smilecoms.commons.base.sd.ServiceDiscoveryAgent;
import com.smilecoms.commons.base.sd.WebSite;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.ScheduledFuture;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class BootStrapServlet extends HttpServlet  {
    private static final Logger log = LoggerFactory.getLogger(BootStrapServlet.class.getName());
    private static ScheduledFuture runner1 = null;
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
    }

    @Override
    public void init() {
        log.warn("Initialising bootstrap servlet");
        BootStrapManager.initialiseSOP();
        runner1 = Async.scheduleAtFixedRate(new SmileBaseRunnable("SOP.Bootstrap") {
            @Override
            public void run() {
                new BootStrapServlet().trigger();
            }
        }, 10000, 60000);
    }

    @Override
    public void destroy() {
        log.warn("Destroying bootstrap servlet");
        BootStrapManager.shutdownSOP();
        Async.cancel(runner1);
    }
    
    private void trigger() {
        try {
            WebSite website = new WebSite("SOP", "http",getListeningPort(), "/sop/");
            website.setWeight(5);
            website.setTestData("isup");
            website.setTestResponseCriteria("Yes SOP is UP");
            website.setClientHostnameRegexMatch("java");
            // SOP does lots of IO so can be a bit slow
            website.setGapBetweenTestsMillis(30000);
            website.setTestTimeoutMillis(10000);
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
