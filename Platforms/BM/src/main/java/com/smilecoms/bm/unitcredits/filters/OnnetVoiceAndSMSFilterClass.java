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
public class OnnetVoiceAndSMSFilterClass extends AllowBasedOnServiceCodeAndRatingGroupFilterClass{

private static final Logger log = LoggerFactory.getLogger(OnnetVoiceAndSMSFilterClass.class);

/**
 * If any of the data bundles can take a charge then only is an onnet voice UC allowed to be used
 * @param em
 * @param acc
 * @param sessionId
 * @param serviceInstance
 * @param ratingResult
 * @param ratingKey
 * @param srcDevice
 * @param description
 * @param eventTimestamp
 * @param ucs
 * @param uci
 * @param unitCreditsInList
 * @return 
 */
    @Override
    public boolean isUCApplicableInContext(EntityManager em, IAccount acc, String sessionId, ServiceInstance serviceInstance, RatingResult ratingResult, RatingKey ratingKey, 
            String srcDevice, String description, Date eventTimestamp, UnitCreditSpecification ucs, IUnitCredit uci, List<IUnitCredit> unitCreditsInList, String location) {
        
        if (!super.isUCApplicableInContext(em, acc, sessionId, serviceInstance, ratingResult, ratingKey, srcDevice, description, eventTimestamp, ucs, uci, unitCreditsInList, location)) {
            log.debug("Parent says it must be filtered out");
            return false;
        }
        
        if (acc.canTakeACharge("0")) {
            log.debug("The account can take a data charge");
            return true;
        }
        
        boolean aDataUCCanTakeACharge = false;
        for (IUnitCredit uc : unitCreditsInList) {
            if (uc.getUnitType().equals("Byte") && uc.canTakeACharge()) {
                aDataUCCanTakeACharge = true;
                log.debug("UC [{}] can take a data charge", uc);
                break;
            }
        }
        return aDataUCCanTakeACharge;
    }
    
    
}
