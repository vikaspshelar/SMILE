/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.rating;

import com.smilecoms.bm.db.model.RatePlan;
import com.smilecoms.bm.db.model.RatePlanAvp;
import com.smilecoms.bm.db.model.ServiceInstance;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManagerFactory;

/**
 *
 * @author paul
 */
public class VolumeBasedDataRatingEngine implements IRatingEngine{

    @Override
    public RatingResult rate(ServiceInstance serviceInstance, RatingKey ratingKey, RatePlan plan, List<RatePlanAvp> avps, Date eventDate) {
        RatingResult rr = new RatingResult();
        BigDecimal centsPerOctet = null;
        String freeRatingGroupsRegex = null;
        String blockedRatingGroupsRegex = null;
        for (RatePlanAvp avp :avps) {
            if (avp.getRatePlanAvpPK().getAttribute().equals("CentsPerOctet")) {
                centsPerOctet = BigDecimal.valueOf(Double.parseDouble(avp.getValue()));
            } else if (avp.getRatePlanAvpPK().getAttribute().equals("FreeRatingGroupsRegex")) {
                freeRatingGroupsRegex = avp.getValue();
            } else if (avp.getRatePlanAvpPK().getAttribute().equals("BlockedRatingGroupsRegex")) {
                blockedRatingGroupsRegex = avp.getValue();
            }
        }
        if (freeRatingGroupsRegex != null && Utils.matchesWithPatternCache(ratingKey.getRatingGroup(), freeRatingGroupsRegex)) {
            rr.setRetailRateCentsPerUnit(BigDecimal.ZERO);
        } else if (blockedRatingGroupsRegex != null && Utils.matchesWithPatternCache(ratingKey.getRatingGroup(), blockedRatingGroupsRegex)) {
            rr.setRetailRateCentsPerUnit(new BigDecimal(-1));
        } else {
            rr.setRetailRateCentsPerUnit(centsPerOctet);
        }
        
        if (rr.getRetailRateCentsPerUnit() == null) {
            throw new RuntimeException("VolumeBasedDataRatingEngine expects an attribute CentsPerOctet");
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
