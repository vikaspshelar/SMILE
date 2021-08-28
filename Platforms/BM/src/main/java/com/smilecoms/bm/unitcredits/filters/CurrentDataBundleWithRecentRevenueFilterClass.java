/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.unitcredits.filters;

import com.smilecoms.bm.charging.IAccount;
import com.smilecoms.bm.db.model.ServiceInstance;
import com.smilecoms.bm.db.model.UnitCreditInstance;
import com.smilecoms.bm.db.model.UnitCreditSpecification;
import com.smilecoms.bm.db.op.DAO;
import com.smilecoms.bm.rating.RatingResult;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.util.Calendar;
import java.util.Date;
import javax.persistence.EntityManager;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class CurrentDataBundleWithRecentRevenueFilterClass extends AllowBasedOnServiceCodeAndRatingGroupFilterClass {

    private static final Logger log = LoggerFactory.getLogger(CurrentDataBundleWithRecentRevenueFilterClass.class);

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
        
        Date date = DAO.getAccountsLastDataOrVoiceBundleExpiry(em, acc);
        if (date == null || date.before(eventTimestamp)) {
            log.debug("Cannot use this UC as there is no current data bundle");
            return false;
        }
        // Have a current data bundle but does if have revenue generation in last 30 days?
        Date revdate = DAO.getAccountsLastDataOrVoiceBundleRevenueGeneration(em, acc);
        if (revdate == null) {
            log.debug("Cannot use this UC as there is no recent revenue");
            return false;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(eventTimestamp);
        cal.add(Calendar.DATE, -30);
        Date thirtyDaysAgo = cal.getTime();
        return thirtyDaysAgo.before(revdate);
        
    }
}
