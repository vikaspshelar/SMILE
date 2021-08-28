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
public class AllowBasedOnExistanceOfAnotherUnitCredit extends DisallowIfRateGroupAndNoInstanceOfAnotherUnitCredit {

    private static final Logger log = LoggerFactory.getLogger(AllowBasedOnServiceCodeAndRatingGroupFilterClass.class);

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

        if (!super.isUCApplicableInContext(em, acc, sessionId, serviceInstance, ratingResult, ratingKey, srcDevice, description, eventTimestamp, ucs, uci, unitCreditsInList, location)) {
            log.debug("Parent says it must be filtered out");
            return false;
        }
        
            boolean include = true;
            try {
                
            String requiredSpecIds = UnitCreditManager.getPropertyFromConfig(ucs, "AllowUsageOnlyIfAccountHasAnyOfSpecIds");
            if (requiredSpecIds != null) {
                log.debug("Allowed Spec IDs are [{}]", requiredSpecIds);
                String[] requiredSpecIdsArray = requiredSpecIds.split(",");
                boolean hasAnyOfRequiredSpecIds = false;
                for (String reqSpecId : requiredSpecIdsArray) {
                    reqSpecId = reqSpecId.trim();
                    if (reqSpecId.isEmpty()) {
                        continue;
                    }
                    
                    int reqUCSpecId = Integer.valueOf(reqSpecId);
                    
                    for (IUnitCredit uciExisting : unitCreditsInList) {
                        if (uciExisting.getUnitCreditSpecification().getUnitCreditSpecificationId() == reqUCSpecId
                                && uciExisting.getProductInstanceId() == uci.getProductInstanceId()) {
                            log.debug("Account and product instance already a spec id of [{}]", reqUCSpecId);
                            hasAnyOfRequiredSpecIds = true;
                            break;
                        }
                    }
                    if (hasAnyOfRequiredSpecIds) {
                        break;
                    }
                }
                if(hasAnyOfRequiredSpecIds)  {
                    include = true;
                } else {
                    include = false;
                }
            }
            
            if (!include) {
                log.debug("This UC need not be included as it does not have any of the required spec Ids [{}].", requiredSpecIds);
            }
            
            return include;
            
        } catch (Exception e) {
            log.warn("Error wrapping UC", e);
            return false;
        }
    }

}
