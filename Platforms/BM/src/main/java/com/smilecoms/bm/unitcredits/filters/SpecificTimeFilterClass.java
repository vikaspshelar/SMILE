/*
 * To change this template, choose Tools | Templates
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
import org.slf4j.*;

/**
 *
 * @author richard
 *
 * This bundle will only be valid for a specific date as specified in unit
 * credit config variables validDay validMonth validYear. If any not set then any applies
 * e.g. if validDay=25 and nothing else, then it is applicable on the 25th of
 * every month e.g. if validDay=25, validMonth=6 and nothing else, then it is
 * applicable on the 25th of June every year e.g. if validDay=25, validMonth=6,
 * validYear=2016 , then it is applicable on the 25th of June 2016
 */
public class SpecificTimeFilterClass extends AllowBasedOnServiceCodeAndRatingGroupFilterClass {

    private static final Logger log = LoggerFactory.getLogger(SpecificTimeFilterClass.class);

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

        Calendar et = Calendar.getInstance();
        et.setTime(eventTimestamp);

        String currentHour = "" + et.get(Calendar.HOUR_OF_DAY); // 0..23         
        if (log.isDebugEnabled()) {
            log.debug("Current hour is [{}]", currentHour);
        }

        String validHourRegex = UnitCreditManager.getPropertyFromConfig(ucs, "ValidHourRegex");
        
        if (log.isDebugEnabled()) {
            log.debug("Valid hour refex is [{}]", validHourRegex);
        }
        
        if (currentHour.matches(validHourRegex)) {
            log.debug("This hour matches validHourRegex so bundle is allowed");
            return true;
        } else {
            log.debug("This hour does not match validHourRegex so bundle is not allowed");
            return false;
        }
    }
}
