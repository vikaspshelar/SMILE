/*
 * To change this template, choose Tools | Templates
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

/**
 *
 * @author paul
 */
public class BonusBundleUnitCredit extends BaseUnitCredit {

    @Override
    public UCReserveResult reserve(RatingKey ratingKey, BigDecimal unitsToReserve, BigDecimal originalUnitsToReserve, IAccount acc, String sessionId, Date eventTimetamp, byte[] request, int reservationSecs, RatingResult ratingResult, ServiceInstance serviceInstance, boolean isLastResort, boolean checkOnly, String unitType, String location) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    @Override
    public UCChargeResult charge(RatingKey ratingKey, BigDecimal unitsToCharge, BigDecimal originalUnitsToCharge, IAccount acc, RatingResult ratingResult, boolean isLastResort, String unitType, Date eventTimetamp) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
