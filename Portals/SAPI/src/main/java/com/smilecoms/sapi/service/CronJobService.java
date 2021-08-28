/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sapi.service;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.sca.Event;
import com.smilecoms.commons.sca.SCAContext;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.sapi.dao.Dao;
import com.smilecoms.sapi.model.CustomerDetails;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;

/**
 *
 * @author bhaskarhg
 */
@Service
public class CronJobService {

    private static final Logger log = LoggerFactory.getLogger(CronJobService.class);

    @Autowired
    private Dao dao;

    public void customerVisaExpitryAlert(int days, String subType) {
        log.info("visa expires#");
        log.info("CustemerLifecycleDaemon running for the customer whose visa expires");

        Collection<CustomerDetails> customerDetails = dao.getVisaExpiryCustomers(days);
        customerDetails.forEach((customer) -> {
            String kycStatus = customer.getKycStatus();
            log.info("Customer KYC status is :" + kycStatus);
            if (kycStatus.equalsIgnoreCase("V")) {
                Event event = new Event();
                event.setEventType("CL_UC");
                event.setEventSubType(subType);
                event.setEventKey(String.valueOf(customer.getCustomerId()));
                event.setEventData("CustId=" + customer.getCustomerId());
//                event.setUniqueKey("CL_UC_" + subType + "_" + customer.getCustomerId());
                try {
                    event.setSCAContext(new SCAContext());
                    event.getSCAContext().setAsync(Boolean.TRUE);
                    SCAWrapper.getAdminInstance().createEvent(event);
                    log.info("Event created#");
                } catch (Exception ex) {
                    log.error("Error while creating event" + ex);
                }
            }
            log.info("visa expires completed#");
        });
    }

}
