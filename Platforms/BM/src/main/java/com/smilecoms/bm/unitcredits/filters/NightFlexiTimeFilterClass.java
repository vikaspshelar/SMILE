/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.unitcredits.filters;

import com.smilecoms.bm.charging.IAccount;
import com.smilecoms.bm.db.model.ServiceInstance;
import com.smilecoms.bm.db.model.UnitCreditInstance;
import com.smilecoms.bm.db.model.UnitCreditSpecification;
import com.smilecoms.bm.rating.RatingResult;
import com.smilecoms.bm.unitcredits.UnitCreditManager;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.util.Calendar;
import java.util.Date;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author rajeshkumar
 */
public class NightFlexiTimeFilterClass extends AllowBasedOnServiceCodeAndRatingGroupFilterClass {
         private static final Logger log = LoggerFactory.getLogger(NightFlexiTimeFilterClass.class);

    @Override
    public boolean isUCApplicable(
            EntityManager em,
            IAccount acc,
            String sessionId,
            ServiceInstance serviceInstance, 
            RatingResult ratingResult, 
            RatingKey ratingKey,
            String srcDevice,
            String description,
            Date eventTimestamp,
            UnitCreditSpecification ucs,
            UnitCreditInstance uci,
            String location) {

        boolean parentAllows = super.isUCApplicable(em, acc, sessionId, serviceInstance, ratingResult, ratingKey, srcDevice, description, eventTimestamp, ucs, uci, location);
        if (!parentAllows) {
            log.debug("My parent says this UC cannot be used");
            return false;
        }
        
        int startHour = UnitCreditManager.getIntPropertyFromConfig(ucs, "NightTimeStartHour");
        int endHour = UnitCreditManager.getIntPropertyFromConfig(ucs, "NightTimeEndHour");
        
        if(startHour < 0 || endHour < 0){
            log.debug("NightFlexiTimeFilterClass missing NightTimeStartHour or NightTimeEndHour");
            return false;
        }
        
        Calendar et = Calendar.getInstance();
        et.setTime(eventTimestamp);

        int hour = et.get(Calendar.HOUR_OF_DAY); // 0..23
        log.debug("Hour is [{}]", hour);

        if (hour >= startHour || hour < endHour ) {
            log.debug("Filter is between [{}] and [{}]",startHour,endHour);
            return true;
        }
        return false;       
    }
}
