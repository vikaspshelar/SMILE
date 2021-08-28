/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sapi.controller;

import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;
import com.smilecoms.commons.base.sd.ServiceDiscoveryAgent;
import com.smilecoms.commons.base.sd.WebSite;
import java.net.MalformedURLException;
import java.util.concurrent.ScheduledFuture;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author bhaskarhg
 */
@RestController
@CrossOrigin
@RequestMapping("/SAPIInitializer")
public class SAPIInitializer {
    private static final Logger log = LoggerFactory.getLogger(SAPIInitializer.class);
     private static ScheduledFuture runner1 = null;
    
    @PostConstruct
    private void init() {
        log.info("called SAPIInitializer init...");
        runner1 = Async.scheduleAtFixedRate(new SmileBaseRunnable("SAPI.Initialiser") {
            @Override
            public void run() {
                trigger();
            }
        }, 5000, 60000);
    }
    @PreDestroy
    private void destroy(){
        Async.cancel(runner1);
    }
    
    private void trigger() {
        try {
            WebSite website = new WebSite("SAPI", "http",8004, "/sapi/");
            website.setWeight(5);
            website.setTestData("isup");
            website.setTestResponseCriteria("<Done>true</Done>");
            website.setGapBetweenTestsMillis(3000);
            website.setClientHostnameRegexMatch(".*");
            website.setTestTimeoutMillis(1000);
            ServiceDiscoveryAgent.getInstance().publishService(website);
        } catch (MalformedURLException ex) {
            log.warn("Error publishing website service: ", ex);
        }
    }
}
