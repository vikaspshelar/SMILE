/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sapi.cronjob;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.sapi.service.CronJobService;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


/**
 *
 * @author bhaskarhg
 */
@Component
public class Cronjob {

    private static final Logger LOG = LoggerFactory.getLogger(Cronjob.class);

    @Autowired
    CronJobService cronJobService;

    @Scheduled(cron = "0 5 0 * * *")
    public void sheduleNotification() throws Exception{
        LOG.info("Starting Notification Cron");
        if (BaseUtils.getBooleanProperty("env.customer.lifecycle.visa.expire.enabled", false)) {
            LOG.info("Sms notification is enabled");
            Set<String> periods = BaseUtils.getPropertyAsSet("env.customer.lifecycle.visa.expire.set");
            periods.forEach((ped) -> {
                String days = ped.split("\\|")[0];
                String subtype = ped.split("\\|")[1];
                LOG.info("SubType" + subtype);
                cronJobService.customerVisaExpitryAlert(Integer.parseInt(days), subtype);
            });
        }
    }
}
