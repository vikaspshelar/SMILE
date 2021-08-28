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
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class AllowBasedOnSectorTownFilterClass extends AllowIfNecessaryFilterClass {

    private static final Logger log = LoggerFactory.getLogger(AllowBasedOnSectorTownFilterClass.class);

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
        
        if (UnitCreditManager.getBooleanPropertyFromConfig(ucs, "IgnoreLocationMidniteAndWeekend") && isMidniteOrWeekend(eventTimestamp)) {
            log.debug("This UC can be used now as its midnite or weekends");
            return true;
        }

        String towns = UnitCreditManager.getPropertyFromConfig(ucs, "AllowedTowns");
        if (towns == null) {
            log.debug("This UC can be used in any town");
            return true;
        }

        if (location == null) {
            log.debug("Location is null");
            return false;
        }

        log.debug("This UC can only be used in [{}]", towns);

        List<String> townList = Utils.getListFromCommaDelimitedString(towns);
        String sectorTown = Utils.getSectorsTown(location);
        log.debug("Location is [{}] which is in town [{}]", location, sectorTown);
        if (townList.contains(sectorTown)) {
            log.debug("This UC can be used in this town");
            return true;
        }

        return false;
    }

    private boolean isMidniteOrWeekend(Date eventTimestamp) {
         Calendar et = Calendar.getInstance();
        et.setTime(eventTimestamp);

        int dayOfWeek = et.get(Calendar.DAY_OF_WEEK);
        int hour = et.get(Calendar.HOUR_OF_DAY); // 0..23
        log.debug("Day of week is [{}] and hour is [{}]", dayOfWeek, hour);

        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            log.debug("This is a weekend");
            return true;
        }

        if (hour >= 0 && hour < 6 ) {
            log.debug("This is between 0 and 6");
            return true;
        }
        
        return Utils.isDateAPublicHoliday(eventTimestamp);
    }
}
