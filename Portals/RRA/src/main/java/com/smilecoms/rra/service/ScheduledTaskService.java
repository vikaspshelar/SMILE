/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.rra.service;

import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ShortMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.smilecoms.rra.dao.CommonDao;
import com.smilecoms.rra.model.MinorDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.smilecoms.commons.util.Utils;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

/**
 *
 * @author rajeshkumar
 */
@Service
public class ScheduledTaskService {
    
    private static final Logger LOG = LoggerFactory.getLogger(ScheduledTaskService.class);
    
    @Autowired
    private CommonDao dao;
    
    public void executeMinorScheduleTask(){
        
        Collection<MinorDetail> minorsDetails = dao.getMinorDetails();
        minorsDetails.forEach((minor) -> {
            int ageGroup = getMinorAgeGroup(minor.getDob());
            LOG.debug("minor has age group :"+ageGroup);
            if(ageGroup == 1){
                Set<String> mobileNumbers = minor.getMobileNumbers();
                mobileNumbers.forEach((mobileNo) -> {
                    LOG.debug("sending SMS to minor customer id:"+minor.getCustomerId());
                    sendNIDAVerificationSMS(mobileNo);
                });
            } else if (ageGroup == 2){
                LOG.debug("deactivating minor customer id:"+minor.getCustomerId());
                deactivateMinor(minor);
                //Deactivate customer services and change kyc_status='P' and classification='customer'
            }
        });  
    }

    private void deactivateMinor(MinorDetail minor) {
        LOG.debug("deactivating services of customer "+minor.getCustomerId());
        //TODO: deactivate customer services
    }

    private void sendNIDAVerificationSMS(String mobileNo) {
        LOG.debug("sending Nida verifiation alert SMS to "+mobileNo);
        ShortMessage sm = new ShortMessage();
        sm.setFrom("sip:+100@ng.smilecoms.com");
        sm.setTo(mobileNo);
        sm.setCampaignId("NIDA Verification");
        sm.setBody("Dear customer.Your account verification is pending. Please verify your account to avoid service disconnection.");
        try {
            SCAWrapper.getAdminInstance().sendShortMessage(sm);
        } catch(Exception ex){
            LOG.error("Error while sending SMS for NIDA verification");
        }
    }

    private int getMinorAgeGroup(String date) {
        Date dob = Utils.stringToDate(date);
        Date now = new Date();
        int months = differenceInMonths(dob, now);
        
        //minor age in less then 17 years and 9 months
        if(months < 213){
            return 0;
        // minor age is more hten 18 years and 3 months
        } else if (months > 219) {
            return 2;
        //minor age is between     
        } else {
            return 1;
        }
    }
    
    private static int differenceInMonths(Date fromDate, Date toDate ) {
	Calendar c1 = Calendar.getInstance();
	c1.setTime(fromDate);
	Calendar c2 = Calendar.getInstance();
	c2.setTime(toDate);
	int diff = 0;
	if (c2.after(c1)) {
	    while (c2.after(c1)) {
	        c1.add(Calendar.MONTH, 1);
	        if (c2.after(c1)) {
	            diff++;
	        }
	    }
	} else if (c2.before(c1)) {
	    while (c2.before(c1)) {
	        c1.add(Calendar.MONTH, -1);
	        if (c1.before(c2)) {
	            diff--;
	        }
	    }
	}
	return diff;
    }
}
