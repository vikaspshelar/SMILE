/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.unitcredits.filters;

import com.smilecoms.bm.charging.IAccount;
import com.smilecoms.bm.db.model.ServiceInstance;
import com.smilecoms.bm.db.model.UnitCreditSpecification;
import com.smilecoms.bm.rating.RatingResult;
import com.smilecoms.bm.unitcredits.UnitCreditManager;
import com.smilecoms.bm.unitcredits.wrappers.IUnitCredit;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mukosi
 */
public class DisallowIfRateGroupAndNoInstanceOfAnotherUnitCredit extends AllowAllFilterClass {
    
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
            
            String blRatingGroupRegex = UnitCreditManager.getPropertyFromConfig(ucs, "AllowedRateGroupRegEx");
            
            if(StringUtils.isEmpty(blRatingGroupRegex)) {
                blRatingGroupRegex = "$^";
            }
            
            if (Utils.matchesWithPatternCache(ratingKey.getRatingGroup(), blRatingGroupRegex)) {
                // Check if the list of unit credits in the context includes one of the social media levy spec ids
                boolean hasAnyOfRequiredSpecIds = false;
                for (IUnitCredit uciExisting : unitCreditsInList) {

                    if (uciExisting.getUnitCreditSpecification().getWrapperClass().equals("SpecialLevyUnitCredit")) {
                        log.debug("Account [{}] already has a spec id of [{}]", uciExisting.getAccountId(), uciExisting.getUnitCreditSpecification().getUnitCreditSpecificationId());
                        hasAnyOfRequiredSpecIds = true;
                        break;
                    } else {
                            log.debug("UC is not a SpecialLevyUnitCredit");
                            continue;
                    }
                }
                    
                if(hasAnyOfRequiredSpecIds)  {
                    include = true;
                } else {
                    log.debug("This UC need not be included since its AllowedRateGroupRegEx [{}] does not match the incoming traffic's rating group [{}].", 
                    blRatingGroupRegex, ratingKey.getRatingGroup());
                    uci.doNoSpecialLevyBundleProcessing(); // Alert when user tries to browse social media and  they do not have the speial levy bundle.
                    include = false;
                }
            } else {
                include = true;
            }       
            
            return include;
            
        } catch (Exception e) {
            log.warn("Error wrapping UC", e);
            return false;
        }
    }
}
