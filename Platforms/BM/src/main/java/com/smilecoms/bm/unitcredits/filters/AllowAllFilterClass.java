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
import com.smilecoms.bm.unitcredits.wrappers.IUnitCredit;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;

/**
 *
 * @author paul
 */
public class AllowAllFilterClass implements IUCFilterClass{

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
        
        return true;
       
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
