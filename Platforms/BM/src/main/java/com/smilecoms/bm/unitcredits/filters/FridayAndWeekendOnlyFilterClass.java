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
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.util.Calendar;
import java.util.Date;
import javax.persistence.EntityManager;
import org.slf4j.*;

/**
 *
 * @author Kamogelo
 */
public class FridayAndWeekendOnlyFilterClass extends AllowBasedOnServiceCodeAndRatingGroupFilterClass {

    private static final Logger log = LoggerFactory.getLogger(FridayAndWeekendOnlyFilterClass.class);

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

        int dayOfWeek = et.get(Calendar.DAY_OF_WEEK);
        int hour = et.get(Calendar.HOUR_OF_DAY); // 0..23
        log.debug("Day of week is [{}] and hour is [{}]", dayOfWeek, hour);

        if ((dayOfWeek == Calendar.FRIDAY && hour>=21) || dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            log.debug("This is a friday night/weekend");
            return true;
        }

        return Utils.isDateAPublicHoliday(eventTimestamp);
        
    }
}
