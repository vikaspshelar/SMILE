/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.unitcredits.wrappers;

import com.smilecoms.bm.charging.IAccount;
import com.smilecoms.bm.db.op.DAO;
import com.smilecoms.bm.rating.RatingResult;
import com.smilecoms.bm.unitcredits.UCChargeResult;
import com.smilecoms.bm.unitcredits.UCReserveResult;
import com.smilecoms.commons.sca.AVP;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceInstanceList;
import com.smilecoms.commons.sca.ServiceInstanceQuery;
import com.smilecoms.commons.sca.StServiceInstanceLookupVerbosity;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class MicrowaveAccessUnitCredit extends BaseUnitCredit {

    private static final Logger log = LoggerFactory.getLogger(MicrowaveAccessUnitCredit.class);

    @Override
    public void provision(IAccount acc, int productInstanceId, Date startDate, boolean verifyOnly,
            String extTxid, double posCentsPaidEach, double posCentsDiscountEach, int saleLineId, String info) throws Exception {

        checkProvisionRules(acc, startDate, verifyOnly, productInstanceId);

        Date maxDate = DAO.getMaxExpiryDateByUnitCreditWrapperClass(em, acc.getAccountId(), this.getClass().getSimpleName());
        if (startDate == null) {
            startDate = new Date();
        }
        if (maxDate.before(startDate)) {
            log.debug("The last microwave access unit credit has expired already. This one will start now");
            maxDate = startDate;
        } else {
            log.debug("The last microwave access unit credit has yet to expire. Next one will start when current one expires");
        }
        log.debug("This microwave access unit credit will start at [{}]", maxDate);
        Calendar expiryDate = Calendar.getInstance();
        expiryDate.setTime(maxDate);
        if (getBooleanPropertyFromConfig("ValidityIsMonths")) {
            expiryDate.add(Calendar.MONTH, ucs.getValidityDays());
        } else if (getBooleanPropertyFromConfig("ValidityIsHours")) {
            expiryDate.add(Calendar.HOUR, ucs.getValidityDays());
        } else {
            expiryDate.add(Calendar.DATE, ucs.getValidityDays());
        }

        String p2PCalendarInvoicing = Utils.getValueFromCRDelimitedAVPString(info, "P2PCalendarInvoicing");
        String p2PExpiryDate = Utils.getValueFromCRDelimitedAVPString(info, "P2PExpiryDate");
        String p2PInvoicingPeriod = Utils.getValueFromCRDelimitedAVPString(info, "P2PInvoicingPeriod");

        if ((p2PCalendarInvoicing != null && !p2PCalendarInvoicing.isEmpty()) && (p2PExpiryDate != null && !p2PExpiryDate.isEmpty())) {
            log.debug("Going to apply P2P start date as [{}] and expiry date as [{}] on UC", p2PExpiryDate);
            Date expiryDt = Utils.getDateFromString(p2PExpiryDate, "yyyy/MM/dd");
            maxDate = Utils.getDateFromString(p2PInvoicingPeriod, "yyyy/MM/dd");
            log.warn("The start date should be same as invoicing date: {}", maxDate);
            expiryDate.setTime(expiryDt);
        }

        Date revenueFirstDate = null;
        Date revenueLastDate = null;
        Double revenuePerDay = null;

        if (getBooleanPropertyFromConfig("NoRevenueOnUsage") && getBooleanPropertyFromConfig("RecRevDaily")) {
            revenueFirstDate = maxDate;
            revenueLastDate = expiryDate.getTime();
            long seconds = Utils.getSecondsBetweenDates(revenueFirstDate, revenueLastDate);
            revenuePerDay = posCentsPaidEach / (seconds / 86400);
            if (log.isDebugEnabled()) {
                log.debug("Revenue must be recognised daily starting on [{}] and ending on [{}] which is [{}] seconds at [{}] per day totalling [{}]",
                        revenueFirstDate, revenueLastDate, seconds, revenuePerDay, revenuePerDay * seconds / 86400);
            }
        }

        uci = DAO.createUnitCreditInstance(em, ucs.getUnitCreditSpecificationId(), acc, expiryDate.getTime(),
                ucs.getUnits(), // Units
                0, // Revenue per unit
                0, // baseline rev per unit
                productInstanceId, // Product inst id
                posCentsPaidEach, // pos cents charges
                posCentsDiscountEach, maxDate, info, extTxid, saleLineId, expiryDate.getTime(), revenueFirstDate, revenueLastDate, revenuePerDay);

        doPostProvisionProcessing(verifyOnly);
    }

    @Override
    public void checkProvisionRules(IAccount acc, Date startDate, boolean verifyOnly, int productInstanceId) throws Exception {
        super.checkProvisionRules(acc, startDate, verifyOnly, productInstanceId); //To change body of generated methods, choose Tools | Templates.

        //Check that the product instance has a service instance AVP for this speed
        String avpValue = getPropertyFromConfig("MWSpeedMbps");
        if (avpValue == null) {
            throw new Exception("Missing attribute MWSpeedMbps");
        }
        ServiceInstanceQuery siq = new ServiceInstanceQuery();
        siq.setProductInstanceId(productInstanceId);
        siq.setVerbosity(StServiceInstanceLookupVerbosity.MAIN_SVCAVP);

        ServiceInstanceList siList = SCAWrapper.getAdminInstance().getServiceInstances(siq);
        boolean speedIsOk = false;
        for (com.smilecoms.commons.sca.ServiceInstance si : siList.getServiceInstances()) {
            for (AVP avp : si.getAVPs()) {
                if (avp.getAttribute().equals("MWSpeedMbps")) {
                    if (avp.getValue().equals(avpValue)) {
                        speedIsOk = true;
                    }
                    break;
                }
            }
            if (speedIsOk) {
                break;
            }
        }

        if (!speedIsOk) {
            throw new Exception("Unit credit must be used on a Microwave service with the correct speed -- Required speed is " + avpValue + " for AVP name MWSpeedMbps");
        }

    }

    @Override
    /**
     * We want Corporate UC to always act like its got units left
     */
    public boolean hasAvailableUnitsLeft() {
        return true;
    }

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
            com.smilecoms.bm.db.model.ServiceInstance serviceInstance,
            boolean isLastResort,
            boolean checkOnly,
            String unitType,
            String location) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public UCChargeResult charge(
            RatingKey ratingKey,
            BigDecimal unitsToCharge,
            BigDecimal originalUnitsToCharge,
            IAccount acc,
            RatingResult ratingResult,
            boolean isLastResort, String unitType, Date eventTimetamp) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
