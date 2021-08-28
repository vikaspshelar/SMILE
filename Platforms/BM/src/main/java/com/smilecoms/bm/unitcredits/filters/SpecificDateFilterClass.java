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
public class SpecificDateFilterClass extends AllowBasedOnServiceCodeAndRatingGroupFilterClass {

    private static final Logger log = LoggerFactory.getLogger(SpecificDateFilterClass.class);

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

        int currentDay = et.get(Calendar.DAY_OF_MONTH); // 1..30         
        int currentMonth = et.get(Calendar.MONTH); // 1..12
        int currentYear = et.get(Calendar.YEAR);
        if (log.isDebugEnabled()) {
            log.debug("Current day is [{}]", currentDay);
            log.debug("Current month is [{}]", currentMonth);
            log.debug("Current year is [{}]", currentYear);
        }

        int validDay = UnitCreditManager.getIntPropertyFromConfig(ucs, "validDay");
        int validMonth = UnitCreditManager.getIntPropertyFromConfig(ucs, "validMonth");
        int validYear = UnitCreditManager.getIntPropertyFromConfig(ucs, "validYear");

        if (log.isDebugEnabled()) {
            log.debug("Valid day is [{}]", validDay);
            log.debug("Valid month is [{}]", validMonth);
            log.debug("Valid year is [{}]", validYear);
        }

        if (validDay != -1 && validDay != currentDay) {
            return false;
        }
        if (validMonth != -1 && validMonth != currentMonth) {
            return false;
        }
        if (validYear != -1 && validYear != currentYear) {
            return false;
        }
        return true;
    }
}
