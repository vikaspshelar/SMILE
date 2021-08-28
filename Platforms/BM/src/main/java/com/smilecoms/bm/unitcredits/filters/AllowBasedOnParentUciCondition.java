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
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class AllowBasedOnParentUciCondition extends AllowBasedOnExistanceOfAnotherUnitCredit {

    private static final Logger log = LoggerFactory.getLogger(AllowBasedOnParentUciCondition.class);

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
            boolean include = true;
            
            if(UnitCreditManager.getBooleanPropertyFromConfig(ucs, "IgnoreWhenParentUciIsNotInSameAccount")) {
                String strParentUciId = Utils.getValueFromCRDelimitedAVPString(uci.getInfo(), "ParentUCI");
                if (strParentUciId != null) {
                    UnitCreditInstance parent = DAO.getUnitCreditInstance(em, Integer.valueOf(strParentUciId));
                    include = (parent.getAccountId() == uci.getAccountId());
                    if (!include) {
                       log.debug("This UC need not be included as its parent [{}] is no longer in the same account as me.", strParentUciId);
                    }
                } else {
                    log.error("IgnoreWhenParentUciIsDepleted is true, but no ParentUCI set on the Info field of unit credit instance [{}]", uci.getUnitCreditInstanceId());
                }
                return include;
            }
            
            if(UnitCreditManager.getBooleanPropertyFromConfig(ucs, "IgnoreWhenParentUciIsDepleted")) {
                String strParentUciId = Utils.getValueFromCRDelimitedAVPString(uci.getInfo(), "ParentUCI");
                
                if (strParentUciId != null) {
                    UnitCreditInstance parent = DAO.getUnitCreditInstance(em, Integer.valueOf(strParentUciId));
                    include = DAO.isPositive(parent.getUnitsRemaining());
                    if (!include) {
                       log.debug("This UC need not be included as its parent [{}] has no units left.", strParentUciId);
                    }
                    return include;
                } else {
                    log.error("IgnoreWhenParentUciIsDepleted is true, but no ParentUCI set on the Info field of unit credit instance [{}]", uci.getUnitCreditInstanceId());
                }
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
