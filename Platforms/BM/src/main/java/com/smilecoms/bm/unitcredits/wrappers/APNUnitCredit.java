/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.unitcredits.wrappers;

import com.smilecoms.bm.EventHelper;
import com.smilecoms.bm.charging.IAccount;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author richard
 */
public class APNUnitCredit extends BaseUnitCredit {

    private static final Logger log = LoggerFactory.getLogger(StaticIPUnitCredit.class);

    @Override
    public void doCheckUnitCreditConstraints() {
        super.doCheckUnitCreditConstraints();
        log.debug("Checking unit credit constraints for APNUnitCredit");
        
        int requiredUnitCreditSpecId = UnitCreditManager.getIntPropertyFromConfig(this.getUnitCreditSpecification(), "UnitCreditSpecIdMustExistInOrg");
        if (requiredUnitCreditSpecId > 0) {
            if (!DAO.doesOrganisationHaveRequiredUnitCredit(em, uci.getUnitCreditInstanceId(), requiredUnitCreditSpecId)) {
                log.debug("Unit credit does not meet bundle constraints as there is no active unit credit spec id [{}] owned by the associated organisation - terminating bundle", requiredUnitCreditSpecId);
                uci.setUnitsRemaining(BigDecimal.ZERO);
                uci.setExpiryDate(new Date());
                uci.setEndDate(new Date());
                em.persist(uci);
                EventHelper.sendUnitCreditRemovedDueToConstraints(this);
            }
        }
    }

    @Override
    public void doPostExpiryProcessing() {
        // May be called more than once
        super.doPostExpiryProcessing();
        try {
            if (isThisTheLastAPNBundle()) {
                releaseAPN(uci.getProductInstanceId());
            }
        } catch (Exception e) {
            new ExceptionManager(this).reportError(e);
        }
    }

    private void releaseAPN(int productInstanceId) throws Exception {
        com.smilecoms.bm.db.model.ServiceInstance si = DAO.getSIMServiceInstanceForProductInstance(em, productInstanceId);
        log.debug("Need to release and remove the APN on SI Id [{}] as this is the last APN unit credit it has and it has expired", si.getServiceInstanceId());
        ProductOrder po = new ProductOrder();
        po.setAction(StAction.NONE);
        po.setSCAContext(new SCAContext());
        po.setProductInstanceId(si.getProductInstanceId());
        ServiceInstanceOrder sio = new ServiceInstanceOrder();
        sio.setAction(StAction.UPDATE);
        sio.setServiceInstance(getServiceInstance(si.getServiceInstanceId()));
        sio.getServiceInstance().getAVPs().clear();
        AVP avp = new AVP();
        avp.setAttribute("APNList");
        avp.setValue(""); // Blank means it will be freed
        sio.getServiceInstance().getAVPs().add(avp);
        po.getServiceInstanceOrders().add(sio);
        SCAWrapper.getAdminInstance().processOrder(po);
        log.debug("Finished freeing the static APN");
    }

    private boolean isThisTheLastAPNBundle() {
        log.debug("Checking if this is the last APN bundle");
        long cnt = DAO.getCountOfOtherAvailableUnitCreditsOnSI(em, ucs.getUnitCreditSpecificationId(), uci.getProductInstanceId(), uci.getUnitCreditInstanceId());
        log.debug("There are [{}] other unit credits of the same specification as this one that are currently applicable to this service instance", cnt);
        if (cnt > 0) {
            log.debug("Not going to release this APN as it still has other APN bundles that are applicable");
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
            log.debug("The last APN has expired already. This one will start now");
            maxDate = startDate;
        } else {
            log.debug("The last APN has yet to expire. Next one will start when current one expires");
        }
        log.debug("This static APN will start at [{}]", maxDate);

        allocateAPN(productInstanceId, info);
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

    private void allocateAPN(int productInstanceId, String info) throws Exception {
        com.smilecoms.bm.db.model.ServiceInstance mysi = DAO.getSIMServiceInstanceForProductInstance(em, productInstanceId);
        log.debug("Going to allocate a APN to service instance Id [{}].", mysi.getServiceInstanceId());
        ServiceInstance si = getServiceInstance(mysi.getServiceInstanceId());
//        for (AVP avp : si.getAVPs()) {
//            if (avp.getAttribute().equals("APNList") && !avp.getValue().isEmpty()) {
//                log.debug("This service instance already has a APN. Nothing to do.");
//                return;
//            }
//        }

        ProductOrder po = new ProductOrder();
        po.setAction(StAction.NONE);
        po.setSCAContext(new SCAContext());
        po.setProductInstanceId(si.getProductInstanceId());
        ServiceInstanceOrder sio = new ServiceInstanceOrder();
        sio.setAction(StAction.UPDATE);
        sio.setServiceInstance(si);
        sio.getServiceInstance().getAVPs().clear();

        String apnName = getPropertyFromInfo(info, "apnname");
        if (apnName == null || apnName.isEmpty()) {
            apnName = UnitCreditManager.getPropertyFromConfig(this.getUnitCreditSpecification(), "DefaultApnName");
            log.debug("No overridingApnName so we use the UCS default [{}]", apnName);
        } else {
            log.debug("OverridingRegion [{}]", apnName);
        }

        AVP avp = new AVP();
        avp.setAttribute("APNList");
        log.debug("We use a special code for APNList to assign an APN: ASSIGN:APN_NAME");
        String avpString = "ASSIGN:" + apnName;
        log.debug("Avp String passed to SCA [{}]", avpString);
        avp.setValue(avpString);
        sio.getServiceInstance().getAVPs().add(avp);
        po.getServiceInstanceOrders().add(sio);
        SCAWrapper.getAdminInstance().processOrder(po);
        log.debug("Finished allocating the static APN Name");
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
