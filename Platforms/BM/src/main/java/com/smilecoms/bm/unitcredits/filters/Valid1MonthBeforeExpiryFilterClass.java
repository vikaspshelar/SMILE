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
import com.smilecoms.xml.schema.bm.RatingKey;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class Valid1MonthBeforeExpiryFilterClass extends AllowBasedOnServiceCodeAndRatingGroupFilterClass{

    private static final Logger log = LoggerFactory.getLogger(Valid1MonthBeforeExpiryFilterClass.class);
    /**
     * Return true if we are within 1 month of the expiry
     * @param em
     * @param acc
     * @param sessionId
     * @param serviceInstance
     * @param ratingResult
     * @param ratingKey
     * @param usedUnits
     * @param srcDevice
     * @param description
     * @param eventTimestamp
     * @param ucs
     * @param uci
     * @return 
     */
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
        
       // Allow it if its within 1 month of the expiry. Even allow ones that are empty
        Calendar expiryDateLess1Month = Calendar.getInstance();
        expiryDateLess1Month.setTime(uci.getExpiryDate());
        expiryDateLess1Month.add(Calendar.MONTH, -1);
        Calendar now = Calendar.getInstance();
        now.setTime(eventTimestamp);
        
        if (expiryDateLess1Month.before(now))  {
            return true;
        }
        return false;
    }
    
}
