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
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import org.slf4j.*;

/**
 *
 * @author richard
 *
 * This filter class extends MidNightFilterClass and when applied to a UC means that UC is only applicable if at least one other UC with the
 * same sale_row_id is also active (expiry > 0, units remaining > 0)
 * This filter class is only applicable to UC that have at least one other UC with same saleRowId
 */
public class MidnightActiveWithOtherBundleFilterClass extends MidnightFilterClass {

    private static final Logger log = LoggerFactory.getLogger(MidnightActiveWithOtherBundleFilterClass.class);

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

        List<UnitCreditInstance> ucis = DAO.getActiveUnitCreditsBasedOnSaleIdRow(em, uci.getSaleRowId());
        
        if (ucis.size() > 1) {
            log.debug("We have another valid, active bundle in same sale row");
            return true;
        } else {
            log.debug("Cannot use this UC as no valud");
            return false;
        }
    }
}
