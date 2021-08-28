/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.rra.cronjob;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.rra.service.ScheduledTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 *
 * @author rajeshkumar
 */
@Component
public class MinorSIMManager {
    private static final Logger LOG = LoggerFactory.getLogger(MinorSIMManager.class);
    
    @Autowired
    ScheduledTaskService scheduledService;
    
    /**
     * Manage minors SIM service. this task will get executed on every day at 11:00 PM.
     */
    //TODO: change cron time.
    @Scheduled(cron = "0 0 23 * * ?")
    public void manageMinorSIM(){
        LOG.debug("Starting manageMinorSIM");
        if(BaseUtils.getBooleanProperty("env.scheduler.minor.sms.notification", false)){
            LOG.debug("Minor SIM manager is enabled");
            scheduledService.executeMinorScheduleTask();  
        } else {
            LOG.info("env.scheduler.account.minor is false");
        }   
    }
}
