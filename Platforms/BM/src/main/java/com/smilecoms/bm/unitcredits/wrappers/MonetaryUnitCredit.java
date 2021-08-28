/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.unitcredits.wrappers;

import com.smilecoms.bm.charging.IAccount;
import com.smilecoms.bm.db.model.ServiceInstance;
import com.smilecoms.bm.db.op.DAO;
import com.smilecoms.bm.rating.RatingResult;
import com.smilecoms.bm.unitcredits.UCChargeResult;
import com.smilecoms.bm.unitcredits.UCReserveResult;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.math.BigDecimal;
import java.util.Date;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class MonetaryUnitCredit extends BaseUnitCredit {

    private static final Logger log = LoggerFactory.getLogger(MonetaryUnitCredit.class);

    @Override
    public UCReserveResult reserve(
            RatingKey ratingKey,
            BigDecimal unitsToReserve,
            BigDecimal originalUnitsToReserve,
            IAccount acc,
            String sessionId,
            Date eventTimetamp,
            byte[] request,
            int reservationSecs,
            RatingResult ratingResult,
            ServiceInstance serviceInstance,
            boolean isLastResort,
            boolean checkOnly, 
            String unitType,
            String location) {

        // We need to convert unitsToReserve into centsToReserve
        BigDecimal centsToReserve = unitsToReserve.multiply(ratingResult.getRetailRateCentsPerUnit());

        UCReserveResult res = new UCReserveResult();
        res.setOOBUnitRate(null);
        log.debug("Monetary unit credit needs to reserve [{}] cents", centsToReserve);
        BigDecimal availableCents = getAvailableUnitsLeft();
        log.debug("Monetary Unit Credit Instance [{}] has [{}] cents available (i.e. current cents - reserved cents)", uci.getUnitCreditInstanceId(), availableCents);

        if (DAO.isZeroOrNegative(availableCents)) {
            res.setUnitsReserved(BigDecimal.ZERO);
            res.setStillHasUnitsLeftToReserve(false);
            return res;
        }

        // If we get here, then this UC has units left

        BigDecimal centsReserved;

        if (availableCents.compareTo(centsToReserve) > 0) {
            // Unit credit has more cents available than required. Reserve the required cents
            centsReserved = centsToReserve;
            res.setStillHasUnitsLeftToReserve(true);
        } else {
            // Unit credit has just enough or not enough
            centsReserved = availableCents;
            res.setStillHasUnitsLeftToReserve(false);
        }

        DAO.createOrUpdateReservation(
                em,
                acc.getAccountId(),
                sessionId,
                uci.getUnitCreditInstanceId(),
                BigDecimal.ZERO,
                eventTimetamp,
                request,
                reservationSecs,
                centsReserved,
                checkOnly);

        res.setReservationWasCreated();
        availableUnitsLeft = null;
        res.setUnitsReserved(DAO.divide(centsReserved,ratingResult.getRetailRateCentsPerUnit())); // We reply with the actual units we could chargeForUsage e.g. 5c per byte charging 10c is 2 bytes
        log.debug("Successfully reserved [{}] cents on monetary unit credit which equates to [{}] of the requested units", centsReserved, res.getUnitsReserved());
        return res;
    }

    @Override
    public UCChargeResult charge(
            RatingKey ratingKey,
            BigDecimal unitsToCharge, 
            BigDecimal originalUnitsToCharge, 
            IAccount acc, 
            RatingResult ratingResult, 
            boolean isLastResort, String unitType, Date eventTimetamp) {

        // We need to convert unitsToCharge into centsToCharge
        BigDecimal centsToCharge = unitsToCharge.multiply(ratingResult.getRetailRateCentsPerUnit());
        UCChargeResult res = new UCChargeResult();
        res.setUnitCreditInstanceId(getUnitCreditInstanceId());
        res.setPaidForUsage(DAO.isPositive(uci.getPOSCentsCharged()) && DAO.isPositive(uci.getUnitsRemaining()));
        BigDecimal currentUnits = uci.getUnitsRemaining();
        BigDecimal availableUnits = getAvailableUnitsLeft();
        BigDecimal centsCharged;
        if (availableUnits.compareTo(centsToCharge) > 0) {
            // Unit credit has more cents available than required. Subtract the required cents
            if (log.isDebugEnabled()) {
                log.debug("Monetary Unit credit has [{}] cents available which is more than required. Deducting [{}] cents from UC Instance[{}]", new Object[]{availableUnits, centsToCharge, uci.getUnitCreditInstanceId()});
            }
            centsCharged = centsToCharge;
        } else {
            // Unit credit has just enough or not enough
            if (log.isDebugEnabled()) {
                log.debug("Monetary Unit credit has [{}] cents available which is not as much as required or exactly whats required. Deducting [{}] cents from UC Instance[{}]", new Object[]{availableUnits, availableUnits, uci.getUnitCreditInstanceId()});
            }
            centsCharged = availableUnits;
        }
        
        setUnitCreditUnitsRemaining(currentUnits.subtract(centsCharged));
        
        res.setUnitsCharged(DAO.divide(centsCharged,ratingResult.getRetailRateCentsPerUnit())); // We reply with the actual units we could chargeForUsage e.g. 5c per byte charging 10c is 2 bytes
        res.setRevenueCents(centsCharged.multiply(uci.getRevenueCentsPerUnit())); // Revenue is the cents  X unit credit revenue cents per cent
        res.setFreeRevenueCents(centsCharged.multiply(uci.getFreeCentsPerUnit())); // Free Revenue is the free unit credit rate X unit credit units
        res.setOOBUnitRate(null);
        res.setUnitsRemaining(getCurrentUnitsLeft());
        if (log.isDebugEnabled()) {
            log.debug("Successfully charged [{}] cents on monetary unit credit which equates to [{}] of the requested units. This is [{}]cents revenue", new Object[]{centsCharged, res.getUnitsCharged(), res.getRevenueCents()});
        }
        return res;
    }
    
    @Override
    public boolean ignoreWhenNoUnitsLeft() {
        return true;
    }
}
