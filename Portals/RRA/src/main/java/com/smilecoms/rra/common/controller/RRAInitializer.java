/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.rra.common.controller;

import com.smilecoms.commons.base.BaseUtils;
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
 * @author rajeshkumar
 */
@RestController
@CrossOrigin
@RequestMapping("/RRAInitialiser")
public class RRAInitializer {
     private static final Logger log = LoggerFactory.getLogger(RRAInitializer.class);
     private static ScheduledFuture runner1 = null;
    
    @PostConstruct
    private void init() {
        log.info("called RRAInitServlet init...");
        runner1 = Async.scheduleAtFixedRate(new SmileBaseRunnable("RRA.Initialiser") {
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
            WebSite website = new WebSite("RRA", "http",8004, "/rra/");
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
