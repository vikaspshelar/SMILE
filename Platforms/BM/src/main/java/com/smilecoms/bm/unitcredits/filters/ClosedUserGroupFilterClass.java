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
import com.smilecoms.xml.schema.bm.ServiceInstanceIdentifier;
import java.util.Date;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class ClosedUserGroupFilterClass extends AllowBasedOnServiceCodeAndRatingGroupFilterClass {

    private static final Logger log = LoggerFactory.getLogger(ClosedUserGroupFilterClass.class);

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

        // Look up the destination IMPU to get its unit credits
        ServiceInstanceIdentifier serviceInstanceIdentifier = new ServiceInstanceIdentifier();
        serviceInstanceIdentifier.setIdentifier(Utils.getPublicIdentityForPhoneNumber(ratingKey.getTo()));
        serviceInstanceIdentifier.setIdentifierType("END_USER_SIP_URI");
        ServiceInstance si;
        try {
            si = DAO.getServiceInstanceForIdentifierAndServiceCode(em, serviceInstanceIdentifier, ratingKey.getServiceCode());
        } catch (NoResultException nre) {
            log.debug("Destination is not a number on our network [{}]", ratingKey.getTo());
            return false;
        }

        log.debug("B number has account [{}] and PI [{}]", si.getAccountId(), si.getProductInstanceId());
        
        UnitCreditInstance cugUCI = null;
        for (UnitCreditInstance toUCI : DAO.getUnitCreditInstances(em, si.getAccountId())) {
            if (toUCI.getProductInstanceId() == si.getProductInstanceId()
                    && toUCI.getProductInstanceId() != 0
                    && uci.getUnitCreditSpecificationId() == toUCI.getUnitCreditSpecificationId()
                    && new Date().after(toUCI.getStartDate())) {
                log.debug("Found CUG unit credit instance id [{}] on the B number", toUCI.getUnitCreditInstanceId());
                cugUCI = toUCI;
                break;
            }
        }

        if (cugUCI == null) {
            log.debug("B number is not on a CUG");
            return false;
        }

        // If it has a valid CUG unit credit then
        // Look up the Organisation of the product instance the UC is assigned to
        int toProductInstanceId = cugUCI.getProductInstanceId();
        int fromProductInstanceId = si.getProductInstanceId();

        log.debug("From PI [{}] to PI [{}]. Checking if organisation is the same", fromProductInstanceId, toProductInstanceId);
        // If its the same as this UCI, then return true

        int fromOrgId = DAO.getProductInstanceOrganisationId(em, fromProductInstanceId);
        if (fromOrgId == 0) {
            return false;
        }
        int toOrgId = DAO.getProductInstanceOrganisationId(em, toProductInstanceId);
        if (toOrgId == 0) {
            return false;
        }
        if (fromOrgId == toOrgId) {
            log.debug("Both CUG unit credits are on a PI for org Id [{}]. This CUG can be used for this call", fromOrgId);
            return true;
        } else {
            log.debug("CUG's are for different organisations [{}] vs [{}]", fromOrgId, toOrgId);
        }
        
        return false;

    }
}
