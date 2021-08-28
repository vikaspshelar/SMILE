/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.unitcredits.wrappers;

import com.smilecoms.bm.EventHelper;
import com.smilecoms.bm.charging.IAccount;
import com.smilecoms.bm.db.model.UnitCreditInstance;
import com.smilecoms.bm.db.model.UnitCreditSpecification;
import com.smilecoms.bm.db.op.DAO;
import com.smilecoms.bm.rating.RatingResult;
import com.smilecoms.bm.unitcredits.UCChargeResult;
import com.smilecoms.bm.unitcredits.UCReserveResult;
import com.smilecoms.bm.unitcredits.UnitCreditManager;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.AVP;
import com.smilecoms.commons.sca.ProductOrder;
import com.smilecoms.commons.sca.SCAContext;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.ServiceInstanceOrder;
import com.smilecoms.commons.sca.ServiceInstanceQuery;
import com.smilecoms.commons.sca.StAction;
import com.smilecoms.commons.sca.StServiceInstanceLookupVerbosity;
import com.smilecoms.commons.util.ExceptionManager;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class StaticIPUnitCredit extends BaseUnitCredit {

    private static final Logger log = LoggerFactory.getLogger(StaticIPUnitCredit.class);

    @Override
    public void do30DaysPreExpiryProcessing() {
        log.debug("StaticIPUnitCredit [{}] called for 30 days pre expiry processing", uci.getUnitCreditInstanceId());
        // Log a JIRA ticket
        EventHelper.sendUnitCredit30DaysFromExpiredEvent(this);
        // Send notification to customer
        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("30DaysPreExpiryWarningEventSubType"));
    }
    

    @Override
    public void doPostExpiryProcessing() {
        // May be called more than once
        super.doPostExpiryProcessing();
        try {
            if (isThisTheLastStaticIPBundle()) {
                releaseStaticIP(uci.getProductInstanceId());
            }
        } catch (Exception e) {
            new ExceptionManager(this).reportError(e);
        }
    }

    private void releaseStaticIP(int productInstanceId) throws Exception {
        com.smilecoms.bm.db.model.ServiceInstance si = DAO.getSIMServiceInstanceForProductInstance(em, productInstanceId);
        log.debug("Need to release and remove the static IP on SI Id [{}] as this is the last StaticIP unit credit it has and it has expired", si.getServiceInstanceId());
        ProductOrder po = new ProductOrder();
        po.setAction(StAction.NONE);
        po.setSCAContext(new SCAContext());
        po.setProductInstanceId(si.getProductInstanceId());
        ServiceInstanceOrder sio = new ServiceInstanceOrder();
        sio.setAction(StAction.UPDATE);
        sio.setServiceInstance(getServiceInstance(si.getServiceInstanceId()));
        sio.getServiceInstance().getAVPs().clear();
        AVP avp = new AVP();
        avp.setAttribute("IPv4Address");
        avp.setValue(""); // Blank means it will be freed
        sio.getServiceInstance().getAVPs().add(avp);
        po.getServiceInstanceOrders().add(sio);
        SCAWrapper.getAdminInstance().processOrder(po);
        log.debug("Finished freeing the static IP address");
    }

    private boolean isThisTheLastStaticIPBundle() {
        log.debug("Checking if this is the last Static IP bundle");
        long cnt = DAO.getCountOfOtherAvailableUnitCreditsOnSI(em, ucs.getUnitCreditSpecificationId(), uci.getProductInstanceId(), uci.getUnitCreditInstanceId());
        log.debug("There are [{}] other unit credits of the same specification as this one that are currently applicable to this service instance", cnt);
        if (cnt > 0) {
            log.debug("Not going to release this IP Address as it still has other IPAddress bundles that are applicable");
            return false;
        }
        return true;
    }

    @Override
    public void provision(IAccount acc, int productInstanceId, Date startDate, boolean verifyOnly, 
            String extTxid, double posCentsPaidEach, double posCentsDiscountEach, int saleLineId, String info) throws Exception {
        
        checkProvisionRules(acc, startDate, verifyOnly, productInstanceId);
        
        Date maxDate = DAO.getMaxExpiryDateByUnitCreditWrapperClass(em, acc.getAccountId(), this.getClass().getSimpleName());
        if (startDate == null) {
            startDate = new Date();
        }
        if (maxDate.before(startDate)) {
            log.debug("The last static IP has expired already. This one will start now");
            maxDate = startDate;
        } else {
            log.debug("The last static IP has yet to expire. Next one will start when current one expires");
        }
        log.debug("This static IP will start at [{}]", maxDate);
        
        verifyBusinessRulesForStaticIP(acc);
        allocateStaticIP(productInstanceId, info);
        // Only provision after allocating IP or else we get deadlock on uci table
        super.provision(acc, productInstanceId, maxDate, verifyOnly, extTxid, posCentsPaidEach, posCentsDiscountEach, saleLineId, info);
        doPostProvisionProcessing(verifyOnly);
    }

    public String getPropertyFromInfo(String info, String propName) {
        if (info == null) {
            return null;
        }
        return Utils.getValueFromCRDelimitedAVPString(info, propName);
    }
    
    private void allocateStaticIP(int productInstanceId, String info) throws Exception {
        com.smilecoms.bm.db.model.ServiceInstance mysi = DAO.getSIMServiceInstanceForProductInstance(em, productInstanceId);
        log.debug("Going to allocate a static IP Address to service instance Id [{}]. If it already has a static IP, then we will do nothing", mysi.getServiceInstanceId());
        ServiceInstance si = getServiceInstance(mysi.getServiceInstanceId());
        for (AVP avp : si.getAVPs()) {
            if (avp.getAttribute().equals("IPv4Address") && !avp.getValue().isEmpty()) {
                log.debug("This service instance already has a static IP address. Nothing to do.");
                return;
            }
        }
        
        ProductOrder po = new ProductOrder();
        po.setAction(StAction.NONE);
        po.setSCAContext(new SCAContext());
        po.setProductInstanceId(si.getProductInstanceId());
        ServiceInstanceOrder sio = new ServiceInstanceOrder();
        sio.setAction(StAction.UPDATE);
        sio.setServiceInstance(si);
        sio.getServiceInstance().getAVPs().clear();
        
        String region = getPropertyFromInfo(info, "region");
        if (region == null || region.isEmpty()) {
            region = UnitCreditManager.getPropertyFromConfig(this.getUnitCreditSpecification(), "ApplicableRegion");
            log.debug("No overridingRegion so we use the UCS default [{}]", region);
        } else {
            log.debug("OverridingRegion [{}]", region);
        }
        
        if (log.isDebugEnabled()) {
            log.debug("ApplicableRegion AVP [{}]", region);
            log.debug("ApplicableApnRegex AVP [{}]", UnitCreditManager.getPropertyFromConfig(this.getUnitCreditSpecification(), "ApplicableApnRegex"));
            log.debug("PrivateOnly AVP [{}]", UnitCreditManager.getPropertyFromConfig(this.getUnitCreditSpecification(), "PrivateOnly"));
        }
        
        AVP avp = new AVP();
        avp.setAttribute("IPv4Address");
        log.debug("We use a special code for IPv4Address to pick a free IP address from the specified region and APN in the form: ASSIGN:REGION:APPLICABLE_APN_REGEX:PRIVATE_ONLY");        
        String avpString = "ASSIGN:" + region + ":" + UnitCreditManager.getPropertyFromConfig(this.getUnitCreditSpecification(), "ApplicableApnRegex") + ":" + UnitCreditManager.getPropertyFromConfig(this.getUnitCreditSpecification(), "PrivateOnly");
        log.debug("Avp String passed to SCA [{}]", avpString);
        avp.setValue(avpString); 
        sio.getServiceInstance().getAVPs().add(avp);
        po.getServiceInstanceOrders().add(sio);
        SCAWrapper.getAdminInstance().processOrder(po);
        log.debug("Finished allocating the static IP address");
    }

    private void verifyBusinessRulesForStaticIP(IAccount acc) throws Exception {
        log.debug("Verifying that the account [{}] has at least 120GB of data bundles available to it and/or it has an unlimited bundle for at least 6 months ahead", acc.getAccountId());
        
        List<UnitCreditInstance> accountsUCIs = DAO.getUnitCreditInstances(em, acc.getAccountId());
        double bytes = 0;
        int unlimitedCnt = 0;
        Calendar fiveMonthsTime = Calendar.getInstance();
        fiveMonthsTime.add(Calendar.DATE, BaseUtils.getIntProperty("env.staticip.committment.days"));
        // Lets only look at what the account has after say 150 days time. S&D want the customer to have a longer term committment
        for (UnitCreditInstance ucInstance : accountsUCIs) {
            if (ucInstance.getStartDate().before(fiveMonthsTime.getTime())) {
                log.debug("Unit credit instance id [{}] starts before [{}] so wont be looked at for the static IP rules", ucInstance.getUnitCreditInstanceId(), fiveMonthsTime.getTime());
                continue;
            }
            UnitCreditSpecification spec = DAO.getUnitCreditSpecification(em, ucInstance.getUnitCreditSpecificationId());
            if (spec.getUnitType().equals("Byte") && !spec.getWrapperClass().contains("Unlimited")) {
                bytes += ucInstance.getUnitsRemaining().doubleValue();
            } else if (spec.getWrapperClass().contains("Unlimited")) {
                unlimitedCnt++;
            }
        }
        if (bytes >= BaseUtils.getLongProperty("env.staticip.minimum.bytes.required.after.committment.days")) {
            return;
        }
        if (unlimitedCnt >= BaseUtils.getIntProperty("env.staticip.minimum.unlimited.instances.after.committment.days")) {
            return;
        }
        
        BigDecimal threshold = BaseUtils.getBigDecimalProperty("env.staticip.accountbalance.cents", new BigDecimal(Long.MAX_VALUE));
        if (acc.getAvailableBalanceCents().compareTo(threshold) >= 0) {
            log.debug("Account [{}] has a balance of [{}] which is greater than or equal to [{}] so a static ip can be granted", new Object[]{acc.getAccountId(), acc.getAvailableBalanceCents(), threshold});
            return;
        }

        throw new Exception("Static IP business rules not met");
    }

    private ServiceInstance getServiceInstance(int serviceInstanceId) throws Exception {
        ServiceInstanceQuery siq = new ServiceInstanceQuery();
        siq.setServiceInstanceId(serviceInstanceId);
        siq.setVerbosity(StServiceInstanceLookupVerbosity.MAIN_SVCAVP_MAPPINGS);
        log.debug("Calling SCA to get the ICCID of the SI");
        ServiceInstance si = SCAWrapper.getAdminInstance().getServiceInstance(siq);
        return si;
    }

    @Override
    public UCReserveResult reserve(RatingKey ratingKey, BigDecimal unitsToReserve, BigDecimal originalUnitsToReserve, IAccount acc, String sessionId, Date eventTimetamp, byte[] request, int reservationSecs, RatingResult ratingResult, com.smilecoms.bm.db.model.ServiceInstance serviceInstance, boolean isLastResort, boolean checkOnly, String unitType, String location) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public UCChargeResult charge(RatingKey ratingKey, BigDecimal unitsToCharge, BigDecimal originalUnitsToCharge, IAccount acc, RatingResult ratingResult, boolean isLastResort, String unitType, Date eventTimetamp) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
