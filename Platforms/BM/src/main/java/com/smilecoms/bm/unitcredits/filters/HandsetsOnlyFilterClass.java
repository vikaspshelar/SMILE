/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.unitcredits.filters;

import com.smilecoms.bm.charging.IAccount;
import com.smilecoms.bm.db.model.ServiceInstance;
import com.smilecoms.bm.db.model.UnitCreditInstance;
import com.smilecoms.bm.db.model.UnitCreditSpecification;
import com.smilecoms.bm.rating.RatingResult;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.platform.PlatformEventManager;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class HandsetsOnlyFilterClass extends AllowBasedOnServiceCodeAndRatingGroupFilterClass {

    private static final Logger log = LoggerFactory.getLogger(HandsetsOnlyFilterClass.class);

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
            String imeisv = srcDevice.split("=")[1];
            boolean allowed = !Utils.matchesWithPatternCache(imeisv, BaseUtils.getProperty("env.bm.nonhandset.imei.regex", "^$"));
            log.debug("IMEISV [{}] is allowed? [{}]", imeisv, allowed);

            if (BaseUtils.getBooleanProperty("env.bm.handsets.only.log.block.event", true) && !allowed) {
                log.debug("Unit credit is not allowed as it does not match the nonhandset IMEI regex, so we log an event unique for today");
                String subType = "DATA_DEVICE_BLOCK";
                String data = "AccId=" + acc.getAccountId() + "\r\nUCId=" + uci.getUnitCreditInstanceId();
                String date = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
                PlatformEventManager.createEvent(
                        "CL_UC",
                        subType,
                        String.valueOf(uci.getUnitCreditInstanceId()),
                        data,
                        "CL_UC_" + subType + "_" + uci.getUnitCreditInstanceId() + "_" + date);
            }

            return allowed;
        } catch (Exception e) {
            log.warn("Error getting device info", e);
            return false;
        }

    }

}
