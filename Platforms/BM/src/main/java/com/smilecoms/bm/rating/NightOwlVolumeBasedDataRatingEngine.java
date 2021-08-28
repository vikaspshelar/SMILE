/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.rating;

import com.smilecoms.bm.db.model.RatePlan;
import com.smilecoms.bm.db.model.RatePlanAvp;
import com.smilecoms.bm.db.model.ServiceInstance;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManagerFactory;

/**
 *
 * @author paul
 */
public class NightOwlVolumeBasedDataRatingEngine implements IRatingEngine {

    @Override
    public RatingResult rate(ServiceInstance serviceInstance, RatingKey ratingKey, RatePlan plan, List<RatePlanAvp> avps, Date eventDate) {
        RatingResult rr = new RatingResult();

        int startFrom = -1;
        int upTo = -1;

        //get
        for (RatePlanAvp avp : avps) {
            if (avp.getRatePlanAvpPK().getAttribute().equals("HourApplicableFromInclusive")) {
                startFrom = Integer.parseInt(avp.getValue());
            }
            if (avp.getRatePlanAvpPK().getAttribute().equals("HourApplicableToExclusive")) {
                upTo = Integer.parseInt(avp.getValue());
            }
        }

        if (startFrom == -1 || upTo == -1) {
            throw new RuntimeException("NightOwlVolumeBasedDataRatingEngine expects an attribute HourApplicableFromInclusive and HourApplicableToExclusive");
        }


        boolean nightOwl = false;

        Calendar et = Calendar.getInstance();
        et.setTime(eventDate);
        int hour = et.get(Calendar.HOUR_OF_DAY); // 0..23

        if (startFrom < upTo) {
            // something like 11 -> 16
            if (hour >= startFrom && hour < upTo) {
                for (RatePlanAvp avp : avps) {
                    //apply the night owl rate
                    if (avp.getRatePlanAvpPK().getAttribute().equals("NightOwlCentsPerOctet")) {
                        rr.setRetailRateCentsPerUnit(BigDecimal.valueOf(Double.parseDouble(avp.getValue())));
                        nightOwl = true;
                        break;
                    }
                }
            }
        } else {
            // something like 22 -> 6
            if (hour >= startFrom || hour < upTo) {
                //apply the night owl rate
                for (RatePlanAvp avp : avps) {
                    if (avp.getRatePlanAvpPK().getAttribute().equals("NightOwlCentsPerOctet")) {
                        rr.setRetailRateCentsPerUnit(BigDecimal.valueOf(Double.parseDouble(avp.getValue())));
                        nightOwl = true;
                        break;
                    }
                }
            }
        }
        if (!nightOwl) {
            //aply normal rate
            for (RatePlanAvp avp : avps) {
                if (avp.getRatePlanAvpPK().getAttribute().equals("CentsPerOctet")) {
                    rr.setRetailRateCentsPerUnit(BigDecimal.valueOf(Double.parseDouble(avp.getValue())));
                    break;
                }
            }
        }
        if (rr.getRetailRateCentsPerUnit() == null) {
            throw new RuntimeException("NightOwlVolumeBasedDataRatingEngine expects an attribute CentsPerOctet");
        }
        return rr;
    }

    @Override
    public void reloadConfig(EntityManagerFactory emf) {
    }

    @Override
    public void onStart(EntityManagerFactory emf) {
    }

    @Override
    public void shutDown() {
    }
}
