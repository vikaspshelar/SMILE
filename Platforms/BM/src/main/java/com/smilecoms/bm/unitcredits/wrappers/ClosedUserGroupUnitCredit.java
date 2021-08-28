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
 * @author paul
 */
public class ClosedUserGroupUnitCredit extends VoiceUnitCredit {

    private static final Logger log = LoggerFactory.getLogger(ClosedUserGroupUnitCredit.class);

    @Override
    public UCChargeResult charge(RatingKey ratingKey, BigDecimal unitsToCharge, BigDecimal originalUnitsToCharge, IAccount acc, RatingResult ratingResult, boolean isLastResort, String unitType, Date eventTimetamp) {
        return super.charge(ratingKey, unitsToCharge, originalUnitsToCharge, acc, ratingResult, isLastResort, unitType, eventTimetamp); 
    }

    @Override
    public UCReserveResult reserve(RatingKey ratingKey, BigDecimal unitsToReserve, BigDecimal originalUnitsToReserve, IAccount acc, String sessionId, Date eventTimetamp, byte[] request, int reservationSecs, RatingResult ratingResult, ServiceInstance serviceInstance, boolean isLastResort, boolean checkOnly, String unitType, String location) {
        return super.reserve(ratingKey, unitsToReserve, originalUnitsToReserve, acc, sessionId, eventTimetamp, request, reservationSecs, ratingResult, serviceInstance, isLastResort, checkOnly, unitType, location); 
    }

}
