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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class AllowBasedOnUserPreferenceFilterClass extends AllowBasedOnSectorTownFilterClass {

    private static final Logger log = LoggerFactory.getLogger(AllowBasedOnUserPreferenceFilterClass.class);
    // Whether its available in the hour of:
    private static final long AVAILABLE_00 = (long) Math.pow(2, 0);
    private static final long AVAILABLE_01 = (long) Math.pow(2, 1);
    private static final long AVAILABLE_02 = (long) Math.pow(2, 2);
    private static final long AVAILABLE_03 = (long) Math.pow(2, 3);
    private static final long AVAILABLE_04 = (long) Math.pow(2, 4);
    private static final long AVAILABLE_05 = (long) Math.pow(2, 5);
    private static final long AVAILABLE_06 = (long) Math.pow(2, 6);
    private static final long AVAILABLE_07 = (long) Math.pow(2, 7);
    private static final long AVAILABLE_08 = (long) Math.pow(2, 8);
    private static final long AVAILABLE_09 = (long) Math.pow(2, 9);
    private static final long AVAILABLE_10 = (long) Math.pow(2, 10);
    private static final long AVAILABLE_11 = (long) Math.pow(2, 11);
    private static final long AVAILABLE_12 = (long) Math.pow(2, 12);
    private static final long AVAILABLE_13 = (long) Math.pow(2, 13);
    private static final long AVAILABLE_14 = (long) Math.pow(2, 14);
    private static final long AVAILABLE_15 = (long) Math.pow(2, 15);
    private static final long AVAILABLE_16 = (long) Math.pow(2, 16);
    private static final long AVAILABLE_17 = (long) Math.pow(2, 17);
    private static final long AVAILABLE_18 = (long) Math.pow(2, 18);
    private static final long AVAILABLE_19 = (long) Math.pow(2, 19);
    private static final long AVAILABLE_20 = (long) Math.pow(2, 20);
    private static final long AVAILABLE_21 = (long) Math.pow(2, 21);
    private static final long AVAILABLE_22 = (long) Math.pow(2, 22);
    private static final long AVAILABLE_23 = (long) Math.pow(2, 23);
    // Whether its available on the day of
    private static final long AVAILABLE_SUN = (long) Math.pow(2, 24);
    private static final long AVAILABLE_MON = (long) Math.pow(2, 25);
    private static final long AVAILABLE_TUE = (long) Math.pow(2, 26);
    private static final long AVAILABLE_WED = (long) Math.pow(2, 27);
    private static final long AVAILABLE_THU = (long) Math.pow(2, 28);
    private static final long AVAILABLE_FRI = (long) Math.pow(2, 29);
    private static final long AVAILABLE_SAT = (long) Math.pow(2, 30);
    private static final long AVAILABLE_PUBLIC_HOLS = (long) Math.pow(2, 31);

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

        String availability = Utils.getValueFromCRDelimitedAVPString(uci.getInfo(), "Availability");
        if (availability == null) {
            log.debug("No user specified availability config so its assumed the UC is available now");
            return true;
        }

        long availabilityLong = Long.parseLong(availability);
        log.debug("Availability is [{}]", availabilityLong);

        if (availabilityLong == Long.MAX_VALUE) {
            log.debug("Availability is max long so its clearly available");
            return true;
        }
        
        Calendar et = Calendar.getInstance();
        et.setTime(eventTimestamp);

        int dayOfWeek = et.get(Calendar.DAY_OF_WEEK);
        int hour = et.get(Calendar.HOUR_OF_DAY); // 0..23
        log.debug("Day of week is [{}] and hour is [{}]", dayOfWeek, hour);

        // If the user has specifed availability then it must be available for both the DAY and the HOUR to be allowed
        boolean isDayAvailable = false;

        switch (dayOfWeek) {
            case Calendar.SUNDAY:
                isDayAvailable = (availabilityLong & AVAILABLE_SUN) != 0;
                break;
            case Calendar.MONDAY:
                isDayAvailable = (availabilityLong & AVAILABLE_MON) != 0;
                break;
            case Calendar.TUESDAY:
                isDayAvailable = (availabilityLong & AVAILABLE_TUE) != 0;
                break;
            case Calendar.WEDNESDAY:
                isDayAvailable = (availabilityLong & AVAILABLE_WED) != 0;
                break;
            case Calendar.THURSDAY:
                isDayAvailable = (availabilityLong & AVAILABLE_THU) != 0;
                break;
            case Calendar.FRIDAY:
                isDayAvailable = (availabilityLong & AVAILABLE_FRI) != 0;
                break;
            case Calendar.SATURDAY:
                isDayAvailable = (availabilityLong & AVAILABLE_SAT) != 0;
                break;
        }

        log.debug("Is day available : [{}]", isDayAvailable);
        if (!isDayAvailable) {
            return false;
        }

        if (Utils.isDateAPublicHoliday(eventTimestamp) && ((availabilityLong & AVAILABLE_PUBLIC_HOLS) == 0)) {
            log.debug("Today is a public holiday and UC cant be used on public holidays");
            return false;
        }

        // Thus far the day is allowed. Now we check the hour
        boolean isHourAvailable = false;

        switch (hour) {
            case 0:
                isHourAvailable = (availabilityLong & AVAILABLE_00) != 0;
                break;
            case 1:
                isHourAvailable = (availabilityLong & AVAILABLE_01) != 0;
                break;
            case 2:
                isHourAvailable = (availabilityLong & AVAILABLE_02) != 0;
                break;
            case 3:
                isHourAvailable = (availabilityLong & AVAILABLE_03) != 0;
                break;
            case 4:
                isHourAvailable = (availabilityLong & AVAILABLE_04) != 0;
                break;
            case 5:
                isHourAvailable = (availabilityLong & AVAILABLE_05) != 0;
                break;
            case 6:
                isHourAvailable = (availabilityLong & AVAILABLE_06) != 0;
                break;
            case 7:
                isHourAvailable = (availabilityLong & AVAILABLE_07) != 0;
                break;
            case 8:
                isHourAvailable = (availabilityLong & AVAILABLE_08) != 0;
                break;
            case 9:
                isHourAvailable = (availabilityLong & AVAILABLE_09) != 0;
                break;
            case 10:
                isHourAvailable = (availabilityLong & AVAILABLE_10) != 0;
                break;
            case 11:
                isHourAvailable = (availabilityLong & AVAILABLE_11) != 0;
                break;
            case 12:
                isHourAvailable = (availabilityLong & AVAILABLE_12) != 0;
                break;
            case 13:
                isHourAvailable = (availabilityLong & AVAILABLE_13) != 0;
                break;
            case 14:
                isHourAvailable = (availabilityLong & AVAILABLE_14) != 0;
                break;
            case 15:
                isHourAvailable = (availabilityLong & AVAILABLE_15) != 0;
                break;
            case 16:
                isHourAvailable = (availabilityLong & AVAILABLE_16) != 0;
                break;
            case 17:
                isHourAvailable = (availabilityLong & AVAILABLE_17) != 0;
                break;
            case 18:
                isHourAvailable = (availabilityLong & AVAILABLE_18) != 0;
                break;
            case 19:
                isHourAvailable = (availabilityLong & AVAILABLE_19) != 0;
                break;
            case 20:
                isHourAvailable = (availabilityLong & AVAILABLE_20) != 0;
                break;
            case 21:
                isHourAvailable = (availabilityLong & AVAILABLE_21) != 0;
                break;
            case 22:
                isHourAvailable = (availabilityLong & AVAILABLE_22) != 0;
                break;
            case 23:
                isHourAvailable = (availabilityLong & AVAILABLE_23) != 0;
                break;
        }

        log.debug("Is hour available : [{}]", isHourAvailable);
        return isHourAvailable;
    }
}
