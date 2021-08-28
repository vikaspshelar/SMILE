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
import com.smilecoms.bm.unitcredits.UnitCreditManager;
import com.smilecoms.bm.unitcredits.wrappers.IUnitCredit;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class AllowIfNecessaryFilterClass extends AllowBasedOnParentUciCondition {

    private static final Logger log = LoggerFactory.getLogger(AllowIfNecessaryFilterClass.class);

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

        try {
            IUnitCredit wrappedUC = UnitCreditManager.getWrappedUnitCreditInstance(em, uci, null);
            boolean include  = false;
            // Only allow DustUnitCredits to charge for social media if they have current units left.
            if(BaseUtils.getBooleanProperty("env.bm.social.media.tax.logic.enabled", false)
                    && wrappedUC.getUnitCreditSpecification().getWrapperClass().equals("DustUnitCredit")
                        && (ratingKey.getServiceCode().equals("txtype.socialmedia.tax") || 
                                ratingKey.getServiceCode().equals("txtype.socialmedia.tax.unlimited"))) {
                if(DAO.isPositive(wrappedUC.getAvailableUnitsLeft())) {
                    include = true;
                } else {
                    include = false; // Do not charge for social media when Unlimited Bundle has balance < 0;
                }
            } else {
                 include = !(!wrappedUC.hasCurrentUnitsLeft() && wrappedUC.ignoreWhenNoUnitsLeft());
            }
            
            if (!include) {
                // Just for performance reasons
                log.debug("This UC need not be included as it has no units left and can be ignored");
            }
            return include;
        } catch (Exception e) {
            log.warn("Error wrapping UC", e);
            return false;
        }

    }

    @Override
    public boolean isUCApplicableInContext(EntityManager em,
            IAccount acc,
            String sessionId,
            ServiceInstance serviceInstance,
            RatingResult ratingResult,
            RatingKey ratingKey,
            String srcDevice,
            String description,
            Date eventTimestamp,
            UnitCreditSpecification ucs,
            IUnitCredit uci,
            List<IUnitCredit> unitCreditsInList,
            String location) {
        return true;
    }

}
