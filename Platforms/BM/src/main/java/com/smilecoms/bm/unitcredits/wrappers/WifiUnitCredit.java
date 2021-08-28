/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.unitcredits.wrappers;

import com.smilecoms.bm.charging.IAccount;
import com.smilecoms.bm.db.model.ServiceInstance;
import com.smilecoms.bm.rating.RatingResult;
import com.smilecoms.bm.unitcredits.UCChargeResult;
import com.smilecoms.bm.unitcredits.UCReserveResult;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.math.BigDecimal;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sabza
 */
public class WifiUnitCredit extends SimpleUnitCredit {

    private static final Logger log = LoggerFactory.getLogger(WifiUnitCredit.class);

    @Override
    public UCChargeResult charge(RatingKey ratingKey, BigDecimal unitsToCharge, BigDecimal originalUnitsToCharge, IAccount acc, RatingResult ratingResult, boolean isLastResort, String unitType, Date eventTimetamp) {
        // This check should not be necessary but lets not assume the filter config was correct
        if (!ratingKey.getServiceCode().contains("32252") || !unitType.equals("OCTET")) {
            throw new RuntimeException("Incorrect filter setup for UnitCredit");
        }
        return super.charge(ratingKey, unitsToCharge, originalUnitsToCharge, acc, ratingResult, isLastResort, unitType, eventTimetamp);
    }

    @Override
    public UCReserveResult reserve(RatingKey ratingKey, BigDecimal unitsToReserve, BigDecimal originalUnitsToReserve, IAccount acc, String sessionId, Date eventTimetamp, byte[] request, int reservationSecs, RatingResult ratingResult, ServiceInstance serviceInstance, boolean isLastResort, boolean checkOnly, String unitType, String location) {
        UCReserveResult res;
        log.debug("Service code is {}, does service code contain 32252 [{}], unit type is [{}] is it really Byte [{}]", new Object[] {ratingKey.getServiceCode(), ratingKey.getServiceCode().contains("32252"), unitType, unitType.equals("Byte")});
        if (!ratingKey.getServiceCode().contains("32252") || !unitType.equals("OCTET")) {
            throw new RuntimeException("Incorrect filter setup for UnitCredit");
        }
        res = super.reserve(ratingKey, unitsToReserve, originalUnitsToReserve, acc, sessionId, eventTimetamp, request, reservationSecs, ratingResult, serviceInstance, isLastResort, checkOnly, unitType, location);
        return res;
    }

    @Override
    public boolean ignoreWhenNoUnitsLeft() {
        return true;
    }

}
