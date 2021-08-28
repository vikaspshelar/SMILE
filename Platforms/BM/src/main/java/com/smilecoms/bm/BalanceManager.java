/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm;

import com.smilecoms.bm.charging.AccountFactory;
import com.smilecoms.bm.charging.AccountFactory.ACCOUNT_TYPE;
import com.smilecoms.bm.charging.IAccount;
import com.smilecoms.bm.charging.ChargingPipeline;
import com.smilecoms.bm.charging.ChargingPipelineResult;
import com.smilecoms.bm.db.model.AccountHistory;
import com.smilecoms.bm.db.model.InterconnectPartner;
import com.smilecoms.bm.db.model.RatePlanAvp;
import com.smilecoms.bm.db.model.Reservation;
import com.smilecoms.bm.db.model.ServiceInstance;
import com.smilecoms.bm.db.model.UnitCreditInstance;
import com.smilecoms.bm.db.model.UnitCreditSpecification;
import com.smilecoms.bm.db.op.DAO;
import com.smilecoms.bm.rating.RatingManager;
import com.smilecoms.bm.rating.RatingResult;
import com.smilecoms.bm.unitcredits.wrappers.IUnitCredit;
import com.smilecoms.bm.unitcredits.UnitCreditManager;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.platform.PlatformEventManager;
import com.smilecoms.commons.platform.SmileWebService;
import com.smilecoms.commons.sca.Account;
import com.smilecoms.commons.sca.CreateStandardGLData;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.CustomerQuery;
import com.smilecoms.commons.sca.ReverseGLData;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceInstanceList;
import com.smilecoms.commons.sca.ServiceInstanceQuery;
import com.smilecoms.commons.sca.StAccountLookupVerbosity;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.StServiceInstanceLookupVerbosity;
import com.smilecoms.commons.util.ExceptionManager;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Javassist;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.bm.BMError;
import com.smilecoms.xml.bm.BMSoap;
import com.smilecoms.xml.schema.bm.AVP;
import com.smilecoms.xml.schema.bm.AccountHistoryQuery;
import com.smilecoms.xml.schema.bm.AccountList;
import com.smilecoms.xml.schema.bm.AccountQuery;
import com.smilecoms.xml.schema.bm.AccountSummary;
import com.smilecoms.xml.schema.bm.AccountSummaryQuery;
import com.smilecoms.xml.schema.bm.BalanceTransferData;
import com.smilecoms.xml.schema.bm.BalanceTransferLine;
import com.smilecoms.xml.schema.bm.ChargingData;
import com.smilecoms.xml.schema.bm.ChargingRequest;
import com.smilecoms.xml.schema.bm.ChargingResult;
import com.smilecoms.xml.schema.bm.Done;
import com.smilecoms.xml.schema.bm.GrantedServiceUnit;
import com.smilecoms.xml.schema.bm.MaximumExpiryDateOfUnitCreditOnAccountQuery;
import com.smilecoms.xml.schema.bm.MaximumExpiryDateOfUnitCreditOnAccountReply;
import com.smilecoms.xml.schema.bm.PeriodSummary;
import com.smilecoms.xml.schema.bm.PlatformInteger;
import com.smilecoms.xml.schema.bm.PlatformLong;
import com.smilecoms.xml.schema.bm.PortingData;
import com.smilecoms.xml.schema.bm.ProvisionUnitCreditLine;
import com.smilecoms.xml.schema.bm.ProvisionUnitCreditRequest;
import com.smilecoms.xml.schema.bm.RatePlan;
import com.smilecoms.xml.schema.bm.RatingKey;
import com.smilecoms.xml.schema.bm.RequestedServiceUnit;
import com.smilecoms.xml.schema.bm.TransactionReversalData;
import com.smilecoms.xml.schema.bm.ServiceInstanceMapping;
import com.smilecoms.xml.schema.bm.ServiceInstanceMappingList;
import com.smilecoms.xml.schema.bm.ServiceInstanceMappingsReplacementData;
import com.smilecoms.xml.schema.bm.ServiceInstanceUpdateData;
import com.smilecoms.xml.schema.bm.SplitUnitCreditData;
import com.smilecoms.xml.schema.bm.TransactionRecord;
import com.smilecoms.xml.schema.bm.TransferGraph;
import com.smilecoms.xml.schema.bm.TransferGraphQuery;
import com.smilecoms.xml.schema.bm.UnitCreditInstanceList;
import com.smilecoms.xml.schema.bm.UnitCreditInstanceQuery;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ejb.Stateless;
import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import org.slf4j.Logger;

/**
 *
 * @author paul
 */
@WebService(serviceName = "BM", portName = "BMSoap", endpointInterface = "com.smilecoms.xml.bm.BMSoap", targetNamespace = "http://xml.smilecoms.com/BM", wsdlLocation = "BMServiceDefinition.wsdl")
@Stateless
@HandlerChain(file = "/handler.xml")
public class BalanceManager extends SmileWebService implements BMSoap {

    @javax.annotation.Resource
    javax.xml.ws.WebServiceContext wsctx;
    @PersistenceContext(unitName = "BMPU")
    private EntityManager em;

    @Override
    public ChargingResult rateAndBill(ChargingRequest chargingRequest) throws BMError {
        setContext(chargingRequest, wsctx);
        ChargingResult chargingResult = new ChargingResult();
        if (log.isDebugEnabled()) {
            logStart();
            log.debug("This charging request has [{}] individual charges within it", chargingRequest.getChargingData().size());
        }
        int sleepTime = BaseUtils.getIntProperty("env.bm.rateandbill.sleep.millis", 0);
        if (sleepTime > 0) {
            log.warn("env.bm.rateandbill.sleep.millis is [{}]ms. Going to sleep for that time to simulate slow processing", sleepTime);
            Utils.sleep(sleepTime);
            log.warn("Finished sleeping");
        }
        try {

            if (chargingRequest.getPlatformContext() != null) {
                checkForUniqueness(chargingRequest.getPlatformContext().getTxId(), null);
            }
            // Check if the charging list came through with one entry and confirm is for social media?
            boolean applySocialMediaTaxLogic = BaseUtils.getBooleanProperty("env.bm.social.media.tax.logic.enabled", false);

            if (applySocialMediaTaxLogic) {
                doSocialMediaChargeProcessing(chargingRequest);
            }

            for (ChargingData chargingData : chargingRequest.getChargingData()) {
                GrantedServiceUnit gsu;
                try {
                    gsu = rateAndBill(chargingData, chargingRequest.isRetrial());
                } catch (Exception e) {
                    BMError error = processError(BMError.class, e);
                    gsu = new GrantedServiceUnit();
                    gsu.setErrorCode(error.getFaultInfo().getErrorCode());

                    //Check if social media tax was paid in full?
                    if (chargingData.getRatingKey().getServiceCode().equals("txtype.socialmedia.tax")
                            || chargingData.getRatingKey().getServiceCode().equals("txtype.socialmedia.tax.unlimited")) {
                        //Check if it was paid in full?
                        if (error.getFaultInfo().getErrorCode().equals("BM-0001")) {
                            chargingResult.getGrantedServiceUnits().add(gsu);
                            log.error("Social media tax charge failed for service identifier [{}], reserved units [{}], requested units [{}].", new Object[]{
                                chargingData.getServiceInstanceIdentifier().getIdentifier(),
                                gsu.getUnitQuantity(),
                                chargingData.getRequestedServiceUnit().getUnitQuantity()});
                            return chargingResult;
                        }
                    }
                }
                if (gsu != null) {
                    if (!chargingData.getRatingKey().getServiceCode().equals("txtype.socialmedia.tax")) { //Do not add social media stuff on the chargingResult
                        chargingResult.getGrantedServiceUnits().add(gsu);
                    }
                }
            }
        } catch (Exception e) {
            throw processError(BMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return chargingResult;
    }

    public ChargingResult rateAndBill(EntityManager emLocal, ChargingRequest chargingRequest) throws BMError {
        em = emLocal;
        return rateAndBill(chargingRequest);
    }

    private GrantedServiceUnit rateAndBill(ChargingData chargingData, boolean isRetrial) throws BMError {
        GrantedServiceUnit grantedServiceUnit = null;

        try {

            if (BaseUtils.getBooleanProperty("env.bm.only.allow.valid.tac", false) && chargingData.getUserEquipment() != null && chargingData.getUserEquipment().startsWith("IMEISV=")) {
                String device = "ERROR Getting Device Info";
                String imeisv = "";
                try {
                    imeisv = chargingData.getUserEquipment().split("=")[1];
                    device = Utils.getDeviceMakeAndModel(imeisv);
                } catch (Exception e) {
                    log.warn("Error getting device info. Will ignore [{}]", e.toString());
                }
                if (device == null) {
                    //create new event for unknown IMEI
                    if (BaseUtils.getBooleanProperty("env.bm.log.event.imei.unknown", true)) {
                        EventHelper.sendImeiUnknownEvent(imeisv);
                    }

                    log.debug("Unknown IMEI [{}]", imeisv);
                    throw new Exception("Unknown IMEI");

                } else {
                    log.debug("Device is [{}]", device);
                }

            }

            if (chargingData.getRatingKey() == null
                    && chargingData.getRequestedServiceUnit() == null
                    && (chargingData.getUsedServiceUnit() == null || (DAO.isZero(chargingData.getUsedServiceUnit().getUnitQuantity()) && chargingData.getUsedServiceUnit().getUnitType() == null))
                    && chargingData.getSessionId() != null) {
                log.debug("This is a simple charge for a previous reservation. All we will use is the account information and session id. Rest will be retrieved from the reservation");
                boolean justClearReservation = chargingData.getUsedServiceUnit() != null && (DAO.isZero(chargingData.getUsedServiceUnit().getUnitQuantity()) && chargingData.getUsedServiceUnit().getUnitType() == null);
                ChargingPipeline.sessionCharge(em, chargingData.getSessionId(), isRetrial, justClearReservation);
                return grantedServiceUnit;
            }

            // Our BM Session Id must be unique within multiple MSCC's that have different rating groups
            ServiceInstance serviceInstance;
            try {
                serviceInstance = DAO.getServiceInstanceForIdentifierAndServiceCode(em, chargingData.getServiceInstanceIdentifier(), chargingData.getRatingKey().getServiceCode());
            } catch (NoResultException nre) {
                // PCB 202 - If a SI is deactivated or deleted mid-session, then we would get here. We want to remove any reservations for this session so that the 
                // reservation timeout daemon does not pick up an expired reservation and log it as an issue
                if (chargingData.getSessionId() != null && !chargingData.getSessionId().isEmpty()) {
                    log.debug("Session cannot continue as the service instance is deactivated or removed");
                    DAO.deleteReservationsBySessionId(em, chargingData.getSessionId() + "_" + chargingData.getRatingKey().getRatingGroup());
                }
                throw nre;
            }

            long accountIdToCharge = DAO.getAccountToBeCharged(em, chargingData.getRatingKey().getFrom(), chargingData.getRatingKey().getTo(), serviceInstance.getAccountId());

            IAccount acc = AccountFactory.getAccount(em, accountIdToCharge, false);

            RatingResult ratingResult = RatingManager.getRate(em, chargingData.getRatingKey(), serviceInstance, Utils.getJavaDate(chargingData.getEventTimestamp()));
            log.debug("Rate for this request is [{}]c per unit. Rating group is [{}]", new Object[]{ratingResult.getRetailRateCentsPerUnit(), chargingData.getRatingKey().getRatingGroup()});

            String BMChargingDataSessionId = null;
            if (chargingData.getSessionId() != null && !chargingData.getSessionId().isEmpty()) {
                BMChargingDataSessionId = chargingData.getSessionId() + "_" + chargingData.getRatingKey().getRatingGroup();
            }

            if (chargingData.getUsedServiceUnit() != null
                    && chargingData.getUsedServiceUnit().isTotalSessionUnits()
                    && chargingData.getUsedServiceUnit().getUnitQuantity() != null
                    && !DAO.isZero(chargingData.getUsedServiceUnit().getUnitQuantity())) {
                log.debug("The used units are the total for the session [{}] thus far. Going to convert it to the units used since the last charge request", BMChargingDataSessionId);
                AccountHistory ah = DAO.getAccountHistoryByServiceInstanceIdAndExtTxIdId(em, BMChargingDataSessionId, serviceInstance.getServiceInstanceId());
                if (ah != null) {
                    log.debug("This session is on account history id [{}]", ah.getId());
                    if (!ah.getIPAddress().equals(chargingData.getIPAddress())) {
                        throw new Exception("Trying to change IP address mid session -- Session has IP " + ah.getIPAddress() + " while charge has IP " + chargingData.getIPAddress());
                    }
                    if (!ah.getStatus().equals("IP")) {
                        throw new Exception("Trying to charge on a finished session -- Status is " + ah.getStatus());
                    }
                    if (!ah.getSourceDevice().equals(chargingData.getUserEquipment())) {
                        throw new Exception("Trying to change user equipment mid session -- Session has " + ah.getSourceDevice() + " while charge has " + chargingData.getUserEquipment());
                    }
                    if (!Utils.isDateInTimeframe(ah.getEndDate(), BaseUtils.getIntProperty("env.bm.wifi.interim.update.secs", 60 * 10) + 10, Calendar.SECOND)) {
                        throw new Exception("Trying to charge to an expired session -- Session was last updated at " + ah.getEndDate());
                    }
                    BigDecimal interimUsedUnits = chargingData.getUsedServiceUnit().getUnitQuantity().subtract(ah.getTotalUnits());
                    if (DAO.isNegative(interimUsedUnits)) {
                        throw new Exception("Charging session with total session units cannot go down over time");
                    }
                    chargingData.getUsedServiceUnit().setUnitQuantity(interimUsedUnits);
                    log.debug("Interim used units is [{}] as current in session is [{}]", interimUsedUnits, ah.getTotalUnits());
                } else {
                    log.debug("This is a new session");
                }
            }

            boolean hasReservation = (chargingData.getRequestedServiceUnit() != null);
            boolean hasCharge = (chargingData.getUsedServiceUnit() != null);
            boolean allowedReservation = ratingResult.isSessionBased();
            boolean allowedEvent = ratingResult.isEventBased();
            boolean hasSession = (BMChargingDataSessionId != null && !BMChargingDataSessionId.isEmpty());

            if (log.isDebugEnabled()) {
                log.debug("Service [{}] hasReservation [{}] hasCharge [{}] allowedReservation [{}] allowedEvent [{}] hasSession [{}]", new Object[]{chargingData.getRatingKey().getServiceCode(), hasReservation, hasCharge, allowedReservation, allowedEvent, hasSession});
            }

            Date timestampDate = chargingData.getEventTimestamp() == null ? new Date() : Utils.getJavaDate(chargingData.getEventTimestamp());

            //Check  if SIM unbundling is enabled?
            if (BaseUtils.getBooleanProperty("env.sales.sim.unbundling.logic.enabled", false)) {
                checkSIMUnbundlingConstraints(chargingData, serviceInstance);
            }

            if (serviceInstance.getProductInstanceId() > 0
                    && serviceInstance.getProductInstanceLastActivityDate() == null
                    && serviceInstance.getProductInstanceLogicalId() == serviceInstance.getProductInstanceId()) {

                // Only when the logical and PI id are the same - i.e. ignore PI deletions and additions
                String code = BaseUtils.getProperty("env.bm.pi.firstuse.code", "");
                if (!code.isEmpty()) {
                    String ok;
                    try {
                        // Must return the string "true" is is ok to be used for first time
                        ok = (String) Javassist.runCode(new Class[]{}, code,
                                em,
                                log,
                                chargingData.getServiceInstanceIdentifier(),
                                serviceInstance.getProductInstanceId(),
                                serviceInstance.getServiceInstanceId(),
                                accountIdToCharge,
                                chargingData.getUserEquipment()
                        );
                    } catch (Throwable e) {
                        log.warn("Error checking if PI can be used for first time", e);
                        new ExceptionManager(log).reportError(e);
                        ok = "Error running javassist";
                    }
                    if (!ok.equals("true")) {
                        PlatformEventManager.createEvent("BM", "FIRSTUSE_BLOCK", String.valueOf(serviceInstance.getProductInstanceLogicalId()), chargingData.getUserEquipment() + "|" + ok);
                        throw new Exception("Product Instance is not allowed to be used -- " + serviceInstance.getProductInstanceId() + " - " + ok);
                    }
                }
            }

            /*
             * Some charging rules...
             *
             * If the request has reservation data then there must be a session
             * id
             *
             * If this service allows for session only, then there must be a
             * session id
             *
             * If this service allows for event only, then there must not be a
             * session id
             *
             * You can only reserve if allowed to do so
             *
             * If this service allows event and session charging and has no
             * reservation data then we need to decide which one to apply. If it
             * contains a session id, we assume its session based charging
             *
             */
            if (hasCharge && hasReservation && chargingData.getRequestedServiceUnit().isCheckOnly()) {
                throw new Exception("You cannot charge and then only check for a reservation");
            }

            if (hasReservation && !hasSession) {
                throw new Exception("A reservation must include a session id");
            }
            if (allowedReservation && !allowedEvent && !hasSession) {
                throw new Exception("This request must include a session id as it is only allowed session based charging");
            }
            if (allowedEvent && !allowedReservation && hasSession) {
                throw new Exception("This request must not include a session id as it is only allowed event based charging");
            }
            if (!allowedReservation && hasReservation) {
                throw new Exception("This request is not allowed reservation data");
            }

            boolean treatAsSessionBased = false;
            if (allowedReservation && hasSession) {
                treatAsSessionBased = true;
            }
            ChargingPipelineResult chargingPipelineResult = null;

            if (hasCharge && treatAsSessionBased) {
                // Session based changing
                log.debug("This is a session charging request");
                chargingPipelineResult = ChargingPipeline.sessionCharge(
                        em,
                        acc,
                        BMChargingDataSessionId,
                        serviceInstance,
                        ratingResult,
                        chargingData.getUsedServiceUnit().getUnitQuantity(),
                        chargingData.getRatingKey(),
                        chargingData.getUserEquipment(),
                        chargingData.getDescription(),
                        chargingData.getUsedServiceUnit().getTerminationCode(),
                        Utils.getJavaDate(chargingData.getEventTimestamp()),
                        chargingData.getUsedServiceUnit().getTerminationCode() != null ? "FI" : "IP",
                        chargingData.getLocation() == null ? "" : chargingData.getLocation(),
                        chargingData.getIPAddress(),
                        chargingData.getServiceInstanceIdentifier().getIdentifierType() + "=" + chargingData.getServiceInstanceIdentifier().getIdentifier(),
                        isRetrial,
                        chargingData.getUsedServiceUnit().getUnitType());
            } else if (hasCharge && !treatAsSessionBased) {
                // Event based charging
                log.debug("This is an event charging request");
                chargingPipelineResult = ChargingPipeline.eventCharge(
                        em,
                        acc,
                        BMChargingDataSessionId,
                        serviceInstance,
                        ratingResult,
                        chargingData.getUsedServiceUnit().getUnitQuantity(),
                        chargingData.getRatingKey(),
                        chargingData.getUserEquipment(),
                        chargingData.getDescription(),
                        Utils.getJavaDate(chargingData.getEventTimestamp()),
                        "FI",
                        chargingData.getLocation() == null ? "" : chargingData.getLocation(),
                        chargingData.getIPAddress(),
                        chargingData.getServiceInstanceIdentifier().getIdentifierType() + "=" + chargingData.getServiceInstanceIdentifier().getIdentifier(),
                        isRetrial,
                        chargingData.getUsedServiceUnit().getUnitType());
            }

            if (hasCharge) {
                // statistics on charging volumes per second
                BaseUtils.addStatisticSample("BM_" + chargingData.getRatingKey().getServiceCode(), BaseUtils.STATISTIC_TYPE.unitspersecond, chargingData.getUsedServiceUnit().getUnitQuantity().longValue() * 8, 120000); // report in bits not bytes. 2 minute average
            }

            if (hasReservation) {
                log.debug("There is reservation data in this request");
                int reserveSecs = chargingData.getRequestedServiceUnit().getReservationSecs() <= 0 ? 300 : chargingData.getRequestedServiceUnit().getReservationSecs();
                byte[] requestByteArray = null;
                if (chargingData.getRequestedServiceUnit().isTriggerCharged()) {
                    log.debug("Request indicates that the reservation may be charged for by simply sending a charge with the session id. Sending request into engine for persisting");
                    requestByteArray = Utils.toBytes(chargingData);
                    log.debug("Finished serialising request to byte array");
                }
                grantedServiceUnit = ChargingPipeline.reserve(
                        em,
                        acc,
                        BMChargingDataSessionId,
                        serviceInstance,
                        ratingResult,
                        chargingData.getRequestedServiceUnit().getUnitQuantity(),
                        chargingData.getRatingKey(),
                        chargingData.getUserEquipment(),
                        chargingData.getDescription(),
                        timestampDate,
                        reserveSecs,
                        requestByteArray,
                        isRetrial,
                        chargingData.getRequestedServiceUnit().isCheckOnly(),
                        chargingData.getRequestedServiceUnit().getUnitType(),
                        chargingData.getLocation() == null ? "" : chargingData.getLocation());
                grantedServiceUnit.setUnitType(chargingData.getRequestedServiceUnit().getUnitType());
            }

            if (grantedServiceUnit != null) {
                grantedServiceUnit.setChargingDataIndex(chargingData.getChargingDataIndex());
            }

            try {
                if (chargingData.getChargingDataIndex() == 0 && chargingData.getIPAddress() != null && !chargingData.getIPAddress().isEmpty()) {
                    // In case location on an IP is needed in a fast way
                    // Only necessary for first charge within a request for performance reasons
                    log.debug("Putting a record in remote cache indicating that IP [{}] is located at [{}]", chargingData.getIPAddress(), chargingData.getLocation());
                    Utils.setIPLocation(chargingData.getIPAddress(), chargingData.getLocation());
                }
            } catch (Exception e) {
                log.warn("Error putting SI location in remote cache", e);
            }

            if (hasCharge && serviceInstance.getProductInstanceId() > 0 && serviceInstance.getServiceInstanceId() > 0) {
                boolean revenueGenerating = false;
                if (chargingPipelineResult != null && (chargingPipelineResult.getConsiderAsRevenueGenerating()
                        || (chargingPipelineResult.getRevenueCents() != null && DAO.isPositive(chargingPipelineResult.getRevenueCents())))) {
                    revenueGenerating = true;
                }
                updateActivityStats(em, serviceInstance, timestampDate, revenueGenerating, chargingData.getUserEquipment());
            }

        } catch (Exception e) {
            throw processError(BMError.class, e);
        }
        return grantedServiceUnit;
    }

    private void checkSIMUnbundlingConstraints(ChargingData chargingData, ServiceInstance serviceInstance) throws Exception {
        //SmileVoice does  not pass IMEI number, they pass: MAC=00:00:00:00:00:00 so we by-pass
        if (!chargingData.getUserEquipment().startsWith("MAC=")) { //by-pass the logic

            List<String> testAccounts = BaseUtils.getPropertyAsList("env.bm.sim.unbundling.test.account.ids");

            if (testAccounts == null || testAccounts.isEmpty() || testAccounts.contains(String.valueOf(serviceInstance.getAccountId()))) {
                String lockedDeviceIMEI = Utils.getValueFromCRDelimitedAVPString(serviceInstance.getInfo(), "LockedToDeviceIMEI");

                // Do not do the SIM unbundling logic if the IMEI is empty or null
                if (lockedDeviceIMEI != null && !lockedDeviceIMEI.isEmpty()) {

                    // If the IMEI are equal, then allow to proceed
                    if (Utils.checkifImeiIsEquivalentToImeisv(lockedDeviceIMEI, chargingData.getUserEquipment())) {
                        log.debug("Customer is using the correct device as per LockedToDeviceIMEI");
                    } else { // Check if user has purchased enough bundles
                        // Check if customer has already purchase minimum of 5GB
                        BigDecimal minRequiredAmountCents = BaseUtils.getBigDecimalProperty("env.bm.min.sim.unbundling.amount.cents", new BigDecimal(500000));

                        BigDecimal totalDataSizePurchased = DAO.getTotalMonetaryAmountCentsForAllPurchasedBundlesOnAccount(em, serviceInstance.getAccountId());

                        if (totalDataSizePurchased != null
                                && totalDataSizePurchased.compareTo(minRequiredAmountCents) >= 0) { //Customer has purchased  the minimum required to use any device
                            try {
                                // Remove the restriction on locked IMEI for all service instances on this account
                                String siIDs = ""; // Services that were unlocked
                                List<ServiceInstance> serviceInstances = DAO.getServiceInstancesForProduct(em, serviceInstance.getProductInstanceId());
                                for (ServiceInstance si : serviceInstances) {
                                    String imei = Utils.getValueFromCRDelimitedAVPString(si.getInfo(), "LockedToDeviceIMEI");
                                    String siInfo = Utils.setValueInCRDelimitedAVPString(si.getInfo(), "LockedToDeviceIMEI", ""); // Set it to blank
                                    DAO.setSIInfo(em, si.getServiceInstanceId(), siInfo);
                                    siIDs = (siIDs.isEmpty() ? "" : siIDs + ",IMEI=" + imei + ";") + si.getServiceInstanceId();
                                }
                                // Log event here as requested  by BI for reporting
                                com.smilecoms.commons.sca.SCAWrapper SCAWrapper = com.smilecoms.commons.sca.SCAWrapper.getAdminInstance();
                                com.smilecoms.commons.sca.Event eventData = new com.smilecoms.commons.sca.Event();
                                eventData.setEventType("BM");
                                eventData.setEventSubType("UNLOCK_IMEI");
                                eventData.setEventKey(String.valueOf(serviceInstance.getProductInstanceId()));
                                eventData.setEventData("admin|SiIDs=" + siIDs);
                                //this ensures event is created async
                                eventData.setSCAContext(new com.smilecoms.commons.sca.SCAContext());
                                eventData.getSCAContext().setAsync(java.lang.Boolean.TRUE);
                                eventData.setUniqueKey("UNLOCK_IMEI_" + serviceInstance.getProductInstanceId());

                                SCAWrapper.createEvent(eventData);
                            } catch (Exception ex) {
                                throw new Exception("Product Instance is not allowed to be used with device -- Error while unlocking service instance " + serviceInstance.getProductInstanceId() + " - IMEI:" + chargingData.getUserEquipment()
                                        + " Description: " + ex.getMessage());
                            }
                        } else {
                            PlatformEventManager.createEvent("BM", "IMEI_BLOCKED", String.valueOf(serviceInstance.getProductInstanceLogicalId()), "IncomingIMEI:" + chargingData.getUserEquipment() + "|LockedToDeviceIMEI:" + lockedDeviceIMEI);
                            throw new Exception("Product Instance is not allowed to be used with device -- " + serviceInstance.getProductInstanceId() + " - IMEI:" + chargingData.getUserEquipment());
                        }
                    }
                }
            }
        }
    }

    private void doSocialMediaChargeProcessing(ChargingRequest chargingRequest) throws BMError {
        String serviceCode = "txtype.socialmedia.tax";
        try {

            if (chargingRequest.getChargingData().size() > 0 && chargingRequest.getChargingData().get(0).getRatingKey() != null
                    && !chargingRequest.getChargingData().get(0).getRatingKey().getServiceCode().equals("txtype.socialmedia.tax.unlimited")) { //To ignore the scocial media tax when charging for Unlimited Social Media Tax

                ChargingData chargingData = chargingRequest.getChargingData().get(0);
                if (chargingData.getRatingKey() == null
                        && chargingData.getRequestedServiceUnit() == null
                        && (chargingData.getUsedServiceUnit() == null || (DAO.isZero(chargingData.getUsedServiceUnit().getUnitQuantity()) && chargingData.getUsedServiceUnit().getUnitType() == null))
                        && chargingData.getSessionId() != null) {
                    log.debug("This is a simple charge for a previous reservation. All we will use is the account information and session id. Rest will be retrieved from the reservation");
                    boolean justClearReservation = chargingData.getUsedServiceUnit() != null && (DAO.isZero(chargingData.getUsedServiceUnit().getUnitQuantity()) && chargingData.getUsedServiceUnit().getUnitType() == null);
                    ChargingPipeline.sessionCharge(em, chargingData.getSessionId(), false, justClearReservation);
                    return;
                }

                String rateGroup = null;
                ChargingData curSocialMediaCharge = chargingRequest.getChargingData().get(0);
                RatingKey curSocialMediaRatingKey = null;
                if (curSocialMediaCharge != null) {
                    curSocialMediaRatingKey = curSocialMediaCharge.getRatingKey();
                    if (curSocialMediaRatingKey != null) {
                        rateGroup = curSocialMediaRatingKey.getRatingGroup();
                    }
                }

                // - Check if this is a social media charge
                if (rateGroup != null && rateGroup.equals(BaseUtils.getProperty("env.bm.rateandbill.socialmedia.rategroup", null))) {
                    // List<String> testAccounts = BaseUtils.getPropertyAsList("env.bm.social.media.tax.test.service.instance.ids");
                    List<String> testAccounts = BaseUtils.getPropertyAsList("env.bm.social.media.tax.test.account.ids");

                    ServiceInstance serviceInstance;
                    try {
                        serviceInstance = DAO.getServiceInstanceForIdentifierAndServiceCode(em, curSocialMediaCharge.getServiceInstanceIdentifier(), curSocialMediaCharge.getRatingKey().getServiceCode());
                    } catch (Exception ex) {
                        log.error("Error while trying to retrieve service for Social Media taxing - [{}]", ex.getMessage());
                        //Just return.. dont throw exception for now
                        return;
                        //throw ex;
                    }

                    if (testAccounts == null || testAccounts.isEmpty() || testAccounts.contains(String.valueOf(serviceInstance.getAccountId()))) {
                        // Check if this is the first usage of social mediat today?

                        if (!DAO.doesServiceHaveSocialMediaChargeForToday(em, serviceInstance.getAccountId(), serviceInstance.getServiceInstanceId(), serviceCode, rateGroup)) {

                            // Dust Unit credit do not get charged on daily basis, they are charged upfront when they provision.
                            if (!DAO.doesAccountHaveUnitCreditOfType(em, serviceInstance.getAccountId(), "DustUnitCredit")) {

                                //Determine the applicable social media tax rate for this product
                                Date socialMediaChargeEventTimeStamp = new Date();
                                SimpleDateFormat sdf = new SimpleDateFormat("_yyyy/MM/dd_HH:mm:ss");

                                String socialMediaChargingSessionId = curSocialMediaCharge.getSessionId().trim().concat(sdf.format(socialMediaChargeEventTimeStamp) + "_SMTAX");
                                RatingKey socialMediaRatingKey = new RatingKey();

                                socialMediaRatingKey.setFrom(curSocialMediaRatingKey.getFrom());
                                socialMediaRatingKey.setTo(curSocialMediaRatingKey.getTo());
                                socialMediaRatingKey.setRatingGroup(curSocialMediaRatingKey.getRatingGroup());
                                socialMediaRatingKey.setServiceCode(serviceCode);

                                //We are referencing the rating key of the social media charge that came in so that the bundles used for the social tax
                                // will be the same at the ones to be charged for the actual social media.
                                RatingResult ratingResult = RatingManager.getRate(em, curSocialMediaCharge.getRatingKey(), serviceInstance, Utils.getJavaDate(curSocialMediaCharge.getEventTimestamp()));

                                // Get applicable unit credits
                                IAccount acc = AccountFactory.getAccount(em, serviceInstance.getAccountId(), false);

                                List<IUnitCredit> unitCredits = UnitCreditManager.getApplicableUnitCredits(em,
                                        acc,
                                        socialMediaChargingSessionId,
                                        serviceInstance,
                                        ratingResult,
                                        curSocialMediaCharge.getRatingKey(),
                                        curSocialMediaCharge.getUserEquipment(),
                                        curSocialMediaCharge.getDescription(),
                                        Utils.getJavaDate(curSocialMediaCharge.getEventTimestamp()),
                                        (curSocialMediaCharge.getUsedServiceUnit() != null ? curSocialMediaCharge.getUsedServiceUnit().getUnitType()
                                        : (curSocialMediaCharge.getRequestedServiceUnit() != null ? curSocialMediaCharge.getRequestedServiceUnit().getUnitType() : "OCTET")),
                                        curSocialMediaCharge.getLocation(),
                                        null); // We do not know the units yet.

                                // If there are no bundles to charge, we must use the standard 200 shillings and let the system try to charge from the account.
                                // Get the highest priority bundle.
                                IUnitCredit highestPriorityUnitCredit = null;
                                int highestPriority = 0;
                                boolean doesCustomerHaveActiveUnlimitedUnitCredit = false;

                                for (IUnitCredit curUC : unitCredits) {
                                    boolean isAllowedForSocialMediaTax = false;
                                    long unitsForSocialMediaTax = UnitCreditManager.getLongPropertyFromConfig(curUC.getUnitCreditSpecification(), "DailyNumberOfBytesForSocialMediaTax");
                                    if (unitsForSocialMediaTax > 0) {
                                        isAllowedForSocialMediaTax = true;
                                    }

                                    if (curUC.getPriority() >= highestPriority && isAllowedForSocialMediaTax) {
                                        highestPriorityUnitCredit = curUC;
                                        highestPriority = curUC.getPriority();
                                    }
                                    //As per UG rule -  if a customer has an active Unlimited bundle, they should not pay for social media again since they 
                                    // are charged the full lump sum at purchase time.
                                    if (curUC.getUnitCreditSpecification().getWrapperClass().equals("DustUnitCredit")) {
                                        doesCustomerHaveActiveUnlimitedUnitCredit = true;
                                    }
                                }

                                if (!doesCustomerHaveActiveUnlimitedUnitCredit) { //If customer does not have an active unlimited unit credit, we will not add the charges for Social Media here
                                    BigDecimal unitQuantity = null;
                                    String unitType = null;
                                    ChargingData socialMediaReservation = new ChargingData();

                                    if (highestPriorityUnitCredit == null) {  // No bundle found
                                        // Use  monetary charge...
                                        unitQuantity = new BigDecimal(20000);
                                        unitType = "SMTAX"; //SM Tax is the monetary value for social media
                                        socialMediaReservation.setDescription("Social Media Tax");
                                    } else {
                                        //A bundle was found, use its configuration setting - DailyNumberOfBytesForSocialMediaTax
                                        unitQuantity = new BigDecimal(UnitCreditManager.getLongPropertyFromConfig(highestPriorityUnitCredit.getUnitCreditSpecification(), "DailyNumberOfBytesForSocialMediaTax"));
                                        unitType = "OCTET";  //We will deduct the number of bytes as specified by the NumberOfBytesForSocialMediaTax property of the highest priority bundle.
                                        socialMediaReservation.setDescription("Data");
                                    }

                                    // Add charge for a new social media service
                                    ChargingData socialMediaTaxCharge = new ChargingData();
                                    socialMediaTaxCharge.setChargingDataIndex(1);
                                    socialMediaTaxCharge.setSessionId(socialMediaChargingSessionId);

                                    //Append the charge to the list here. Add the charge here
                                    chargingRequest.getChargingData().add(0, socialMediaTaxCharge);

                                    // Add reservation for a new social media service
                                    socialMediaReservation.setEventTimestamp(Utils.getDateAsXMLGregorianCalendar(socialMediaChargeEventTimeStamp));
                                    socialMediaReservation.setIPAddress(curSocialMediaCharge.getIPAddress());
                                    socialMediaReservation.setLocation(curSocialMediaCharge.getLocation());
                                    socialMediaReservation.setSessionId(socialMediaChargingSessionId);
                                    socialMediaReservation.setChargingDataIndex(1);
                                    socialMediaReservation.setRatingKey(socialMediaRatingKey);
                                    socialMediaReservation.setServiceInstanceIdentifier(curSocialMediaCharge.getServiceInstanceIdentifier());
                                    socialMediaReservation.setUserEquipment(curSocialMediaCharge.getUserEquipment());

                                    RequestedServiceUnit rsu = new RequestedServiceUnit();
                                    rsu.setReservationSecs(10);
                                    rsu.setUnitQuantity(unitQuantity);
                                    rsu.setUnitType(unitType);
                                    rsu.setTriggerCharged(true);
                                    socialMediaReservation.setRequestedServiceUnit(rsu);
                                    //Add the charge here
                                    chargingRequest.getChargingData().add(0, socialMediaReservation);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw processError(BMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
    }

    private void updateActivityStats(EntityManager em, ServiceInstance serviceInstance, Date timestampDate, boolean revenueGenerating, String imei) {
        try {
            DAO.updateProductInstanceBeenUsed(em, serviceInstance, timestampDate, revenueGenerating, imei);
            DAO.updateServiceInstanceBeenUsed(em, serviceInstance, timestampDate, revenueGenerating);
        } catch (Exception e) {
            log.warn("Error updating activity stats", e);
        }
    }

    @Override
    public AccountList getAccounts(AccountQuery accountQuery) throws BMError {
        setContext(accountQuery, wsctx);

        if (log.isDebugEnabled()) {
            logStart();
        }
        AccountList ret = new AccountList();
        try {
            IAccount acc;
            if (accountQuery.getUnitCreditInstanceId() > 0) {
                acc = AccountFactory.getAccount(em, DAO.getUnitCreditInstance(em, accountQuery.getUnitCreditInstanceId()).getAccountId(), true);
            } else {
                acc = AccountFactory.getAccount(em, accountQuery.getAccountId(), true);
            }
            com.smilecoms.xml.schema.bm.Account xmlAcc = getXMLAccount(acc, accountQuery.getVerbosity());
            ret.getAccounts().add(xmlAcc);
            ret.setNumberOfAccounts(ret.getAccounts().size());

        } catch (Exception e) {
            throw processError(BMError.class,
                    e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return ret;
    }

    @Override
    public UnitCreditInstanceList getUnitCreditInstances(UnitCreditInstanceQuery unitCreditQuery) throws BMError {
        setContext(unitCreditQuery, wsctx);

        if (log.isDebugEnabled()) {
            logStart();
        }
        UnitCreditInstanceList ret = new UnitCreditInstanceList();
        try {
            if (unitCreditQuery.getUnitCreditInstanceId() > 0) {
                ret.getUnitCreditInstances().add(getXMLUnitCreditInstance(DAO.getUnitCreditInstance(em, unitCreditQuery.getUnitCreditInstanceId())));
            } else if (unitCreditQuery.getSaleRowId() > 0 && (unitCreditQuery.getWrapperClass() != null && !unitCreditQuery.getWrapperClass().isEmpty())) {
                List<UnitCreditInstance> uciList = DAO.getOldestUnitCreditsUsingSaleRowIdAndType(em, unitCreditQuery.getSaleRowId(), unitCreditQuery.getWrapperClass());
                for (UnitCreditInstance uc : uciList) {
                    if (uc != null) {
                        ret.getUnitCreditInstances().add(getXMLUnitCreditInstance(uc));
                    }
                }
            }

            ret.setNumberOfUnitCreditInstances(ret.getUnitCreditInstances().size());

        } catch (Exception e) {
            throw processError(BMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return ret;
    }

    @Override
    public Done transferBalance(BalanceTransferData balanceTransferData) throws BMError {
        setContext(balanceTransferData, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        try {

            Set<Long> accountsInRequestList = new HashSet();
            for (BalanceTransferLine line : balanceTransferData.getBalanceTransferLines()) {
                accountsInRequestList.add(line.getSourceAccountId());
                accountsInRequestList.add(line.getTargetAccountId());
            }

            checkForUniqueness(balanceTransferData.getPlatformContext().getTxId(), accountsInRequestList);

            for (BalanceTransferLine line : balanceTransferData.getBalanceTransferLines()) {
                transferBalance(line, balanceTransferData.getPlatformContext().getTxId(), balanceTransferData.getPlatformContext().getOriginatingIP());

            }
        } catch (Exception e) {
            throw processError(BMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    private void transferBalance(BalanceTransferLine balanceTransferLine, String txid, String srcIPAddress) throws Exception {
        if (balanceTransferLine.getSourceAccountId() == balanceTransferLine.getTargetAccountId()) {
            throw new Exception("Source and Target account must be different");
        }
        if (balanceTransferLine.getAmountInCents() < 0) {
            throw new Exception("Transfer amount must be zero or positive");
        }

        if (balanceTransferLine.getTransferType() == null || balanceTransferLine.getTransferType().isEmpty()) {
            throw new Exception("Transfer type is null or empty");
        }

        IAccount srcAccount = AccountFactory.getAccount(em, balanceTransferLine.getSourceAccountId(), false);
        IAccount dstAccount = AccountFactory.getAccount(em, balanceTransferLine.getTargetAccountId(), false);
        log.debug("Doing transfer of [{}]c from account [{}] to account [{}] and Info [{}]", new Object[]{balanceTransferLine.getAmountInCents(), srcAccount.getAccountId(), dstAccount.getAccountId(), balanceTransferLine.getInfo()});
        BigDecimal amnt = BigDecimal.valueOf(balanceTransferLine.getAmountInCents());
        srcAccount.transferAmountToAccount(dstAccount, amnt);
        long[] ahIds = writeHistoryForAccountTransfer(srcAccount, dstAccount, amnt, txid, balanceTransferLine.getTransferType(), balanceTransferLine.getSaleId(), balanceTransferLine.getDescription(), srcIPAddress, balanceTransferLine.getInfo());
        createEvent(srcAccount.getAccountId());

        EventHelper.sendAccountEvent(srcAccount, EventHelper.AccSubTypes.DEBIT, ahIds[0]);
        EventHelper.sendAccountEvent(dstAccount, EventHelper.AccSubTypes.CREDIT, ahIds[1]);

        doAdditionalPostTransferProcessing(srcAccount, dstAccount, amnt, balanceTransferLine.getTransferType());
    }

    @Override
    public Done reverseTransactions(TransactionReversalData transactionReversalData) throws BMError {
        setContext(transactionReversalData, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        try {
            for (String txid : transactionReversalData.getTransferExtTxIds()) {
                reverseTransfer(txid, transactionReversalData.getPlatformContext().getTxId());
            }
            for (String txid : transactionReversalData.getUnitCreditExtTxIds()) {
                reverseUnitCredit(txid, transactionReversalData.getPlatformContext().getTxId());
            }
            for (String txid : transactionReversalData.getChargeExtTxIds()) {
                reverseCharge(txid, transactionReversalData.getPlatformContext().getTxId());

            }
        } catch (Exception e) {
            throw processError(BMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    private void reverseCharge(String txid, String requestsTxId) throws Exception {
        log.debug("Getting transfer records for external transaction id [{}]", txid);
        List<AccountHistory> ahRows = DAO.getAccountHistoryInReverseOrderForTxId(em, txid);
        if (ahRows.isEmpty()) {
            throw new Exception("No charge to reverse with TxId -- " + txid);
        }

        for (AccountHistory ah : ahRows) {
            if (ah.getTransactionType().endsWith(".reversed")) {
                log.debug("Row [{}] is already reversed so ignoring", ah.getId());
                continue;
            }
            IAccount acc = AccountFactory.getAccount(em, ah.getAccountId(), false);
            if (ah.getTransactionType().startsWith("txtype.tfr")) {
                throw new Exception("Trying to reverse a transaction which is a transfer");
            }
            if (ah.getTransactionType().startsWith("txtype.uc")) {
                throw new Exception("Trying to reverse a transaction which is a unit credit purchase");
            }
            if (!ah.getStatus().equals("FI")) {
                throw new Exception("Trying to reverse a transaction which is in progress");
            }
            if (!DAO.isZero(ah.getRevenueCents()) || !DAO.isZero(ah.getUnitCreditBaselineUnits()) || !DAO.isZero(ah.getUnitCreditUnits())) {
                throw new Exception("Cannot reverse a charge that used unit credits or recognised revenue");
            }
            if (ah.getTransactionType().contains("reversal")) {
                throw new Exception("Trying to reverse a transaction which is part of a reversal");
            }
            if (DAO.isPositive(ah.getAccountCents())) {
                throw new Exception("Something is wrong. A charge cannot be positive");
            }

            log.debug("Crediting account with [{}]c for the reversal", ah.getAccountCents().negate());
            acc.creditBalance(ah.getAccountCents().negate());
            log.debug("Reversing charge row [{}]", ah.getId());
            DAO.createAccountHistoryForEvent(
                    em,
                    "",
                    "",
                    "",
                    ah.getAccountCents(), // We are crediting. the DAO negates so - X - = +
                    acc,
                    new Date(),
                    "Reversal of charge row " + ah.getId(),
                    BigDecimal.ZERO,
                    "",
                    ah.getTransactionType() + ".reversal",
                    "FI",
                    0,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    ah.getUnearnedRevenueCents() == null ? BigDecimal.ZERO : ah.getUnearnedRevenueCents().negate(), // Would be zero but negate what was there just in case
                    BigDecimal.ZERO,
                    "",
                    "",
                    "", null, null, null, null, null, ah.getInfo(), null);

            // Next increase the account balance back to what it was
            ah.setTransactionType(ah.getTransactionType() + ".reversed");
            em.persist(ah);
        }
        em.flush();
    }

    private void reverseTransfer(String txid, String requestsTxId) throws Exception {
        log.debug("Getting transfer records for external transaction id [{}]", txid);
        List<AccountHistory> ahRows = DAO.getAccountHistoryInReverseOrderForTxId(em, txid);
        for (AccountHistory ah : ahRows) {

            if (ah.getTransactionType().endsWith(".reversed")) {
                log.debug("Row [{}] is already reversed so ignoring", ah.getId());
                continue;
            }
            if (ah.getTransactionType().endsWith(".cbtu") && !requestsTxId.endsWith("_POS")) {
                // Credit Bureau 
                throw new Exception("Clearing Bureau transfers can only be resersed by cancelling the associated sale");
            }
            if (!ah.getTransactionType().startsWith("txtype.tfr.")) {
                throw new Exception("Trying to reverse a transaction which is not a transfer");
            }
            if (ah.getTransactionType().contains("reversal")) {
                throw new Exception("Trying to reverse a transaction which is part of a reversal");
            }
            if (ah.getTransactionType().startsWith("txtype.tfr.debit")) {
                log.debug("Row [{}] is not a credit. Will ignore as we will only reverse credits. Will however set it to reversed", ah.getId());
                ah.setTransactionType(ah.getTransactionType() + ".reversed");
                em.persist(ah);
                continue;
            }
            if (DAO.isNegative(ah.getAccountCents())) {
                throw new Exception("Something is wrong. A credit cannot be negative");
            }
            log.debug(" Reversing transfer row [{}]", ah.getId());
            BalanceTransferLine balanceTransferLine = new BalanceTransferLine();
            balanceTransferLine.setAmountInCents(ah.getAccountCents().doubleValue());
            balanceTransferLine.setDescription("Reversal of Id " + ah.getId());
            balanceTransferLine.setSourceAccountId(ah.getAccountId());
            balanceTransferLine.setTargetAccountId(Long.parseLong(ah.getDestination()));
            balanceTransferLine.setTransferType(ah.getTransactionType().replace("txtype.tfr.credit.", "") + ".reversal");
            transferBalance(balanceTransferLine, requestsTxId, "");
            ah.setTransactionType(ah.getTransactionType() + ".reversed");
            em.persist(ah);
        }

        double exciseTaxRate = BaseUtils.getDoubleProperty("env.sales.excise.tax.percent", 0);
        if (exciseTaxRate > 0) {
            doExciseDutyGLReversal(txid);
            log.debug("finished with excise duty journal");
        }

        em.flush();
    }

    private void reverseUnitCredit(String txid, String requestsTxId) throws Exception {
        log.debug("Reversing UC for txid [{}]", txid);
        if (txid.isEmpty()) {
            throw new Exception("Cannot reverse a unit credit without the exttxid");
        }
        // Get the unit_credit_Instance row/s
        List<UnitCreditInstance> uciList = DAO.getUnitCreditInstancesByExtTxId(em, txid);
        if (uciList.isEmpty()) {
            log.debug("Unit credit does not exist so no reversal necessary");
            return;
        }

        long accountId = 0;
        for (UnitCreditInstance uci : uciList) {
            if (Utils.getValueFromCRDelimitedAVPString(uci.getInfo(), "Deleted") != null) {
                throw new Exception("Unit credit is already deleted");
            }
            if (accountId == 0) {
                accountId = uci.getAccountId();
            }
            if (accountId != uci.getAccountId()) {
                // should not be possible but just be safe
                throw new Exception("Cannot reverse a unit credit that spans multiple accounts");
            }
            if (DAO.getUnitCreditSpecification(em, uci.getUnitCreditSpecificationId()).getUnitType().equals("VAS")) {
                throw new Exception("Cannot reverse VAS unit credits");
            }
            if (!Utils.isInTheFuture(uci.getExpiryDate())) {
                throw new Exception("Cannot reverse an expired unit credit");
            }
        }

        // Get an exclusive lock on the account
        IAccount acc = AccountFactory.getAccount(em, accountId, false);

        // Get the accounts reservations and make sure none of them are for the UC's being deleted
        List<Reservation> resList = DAO.getAccountsReservationList(em, accountId);

        for (UnitCreditInstance uci : uciList) {
            BigDecimal unitsUsed = uci.getUnitsAtStart().subtract(uci.getUnitsRemaining());
            if (unitsUsed.compareTo(BaseUtils.getBigDecimalProperty("env.bm.uc.reversal.max.used", new BigDecimal(0))) > 0) {
                throw new Exception("Cannot reverse a unit credit that has been used -- " + unitsUsed + " units used");
            }
            for (Reservation res : resList) {
                if (res.getReservationPK().getUnitCreditInstanceId() == uci.getUnitCreditInstanceId()) {
                    throw new Exception("Cannot delete a unit credit that has a reservation against it");
                }
            }

            // RecRevDaily=true
            UnitCreditSpecification ucs = DAO.getUnitCreditSpecification(em, uci.getUnitCreditSpecificationId());

            boolean isRecordingRevenueDaily = Utils.getBooleanValueFromCRDelimitedAVPString(ucs.getConfiguration(), "RecRevDaily");

            if (isRecordingRevenueDaily) { //Time based bundles are not allowed to be rolled back or cancelled after the revenue start date'd day has passed.
                //Check if the revenue first date is in the future.
                //if (!Utils.isInTheFuture(Utils.getEndOfDay(uci.getRevenueFirstDate()))) {
                //  throw new Exception("Unit credits that recognise revenue daily can only be reversed before the end of their first revenue date");
                //}/
                //As agreed with Caroline - for now, all time based bundles are not allowed to be deleted if they have any usage > 0 bits.
                unitsUsed = uci.getUnitsAtStart().subtract(uci.getUnitsRemaining());
                if (unitsUsed.compareTo(new BigDecimal(0)) > 0) {
                    throw new Exception("Cannot delete a unit credit that has been used -- " + unitsUsed + " units used");
                }

                //Reverse the time based revenue:
                BigDecimal timeBasedRevenueSoFar = DAO.getTotalTimeBasedUnitCreditRevenueToReverse(em, uci.getUnitCreditInstanceId());
                CreateStandardGLData glData = new CreateStandardGLData();
                glData.setGlAmount(timeBasedRevenueSoFar.doubleValue());
                glData.setPrimaryKey(uci.getUnitCreditInstanceId());
                glData.setGlCreditAccount(Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "ReversalOrDeletionGLCreditAccount"));
                glData.setGlDebitAccount(Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "ReversalOrDeletionGLDebitAccount"));
                glData.setGlDescription("Reversal SR" + uci.getSaleRowId());
                glData.setTableName(Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "ReversalOrDeletionGLX3TransactionCode"));
                glData.setX3GlTransactionCode(Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "ReversalOrDeletionGLX3TransactionCode"));
                SCAWrapper.getAdminInstance().createStandardGL(glData);
            }
        }

        // Ok, we have done all our checks and are good to go
        // We need to delete the row/s from unit_credit_instance
        for (UnitCreditInstance uci : uciList) {

            UnitCreditSpecification ucs = DAO.getUnitCreditSpecification(em, uci.getUnitCreditSpecificationId());

            DAO.deleteUnitCreditInstance(em, uci.getUnitCreditInstanceId());
            log.debug("Deleted unit credit instance id [{}]", uci.getUnitCreditInstanceId());

            // If this is an unlimited bundle also reverse the OTT tax journal?
            if (BaseUtils.getBooleanProperty("env.bm.social.media.tax.logic.enabled", false)
                    && ucs.getWrapperClass().equals("DustUnitCredit")) {
                ReverseGLData request = new ReverseGLData();
                request.setPrimaryKey(uci.getSaleRowId());
                request.setTableName("OTTBUNU");
                request.setTransactionType("GLEntry");
                log.debug("Reversing OTT GL for unlimited unit credit id [{}], primary key [{}], table name [{}], transaction type [{}]",
                        new Object[]{uci.getUnitCreditInstanceId(), request.getPrimaryKey(), request.getTableName(), request.getTransactionType()});
                SCAWrapper.getAdminInstance().reverseGL(request);
            }
        }

        double exciseTaxRate = BaseUtils.getDoubleProperty("env.sales.excise.tax.percent", 0);
        if (exciseTaxRate > 0) {
            doExciseDutyGLReversal(txid);
            log.debug("finished with excise duty journal");
        }

        List<AccountHistory> accountHistoryList = DAO.getAccountHistoryInReverseOrderForTxId(em, txid);

        for (AccountHistory ah : accountHistoryList) {

            if (ah.getAccountId() != accountId) {
                throw new Exception("Account history account id does not match unit credit account id");
            }

            if (ah.getTransactionType().endsWith(".reversed")) {
                log.debug("Row [{}] is already reversed so ignoring", ah.getId());
                continue;
            }

            // Next reverse each account_history row
            ah.setTransactionType(ah.getTransactionType() + ".reversed");

            DAO.createAccountHistoryForEvent(
                    em,
                    requestsTxId,
                    "",
                    "",
                    BigDecimal.ZERO,
                    acc,
                    new Date(),
                    "Reversal of unit credit purchase row " + ah.getId(),
                    BigDecimal.ZERO,
                    "",
                    "txtype.uc.reversal",
                    "FI",
                    0,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    ah.getUnearnedRevenueCents() == null ? BigDecimal.ZERO : ah.getUnearnedRevenueCents().negate(), // Unearned changes by the anti of what it did when it was created
                    BigDecimal.ZERO,
                    "",
                    "",
                    "", null, null, null, null, null, ah.getInfo(), null);

        }
    }

    private void doExciseDutyGLReversal(String txid) {

        String txidRevUC = "UNIQUE-UC-SaleLine-";
        String txidRevAIR = "UNIQUE-TFR-SaleLine-";
        if (!txid.contains(txidRevUC) && !txid.contains(txidRevAIR)) {
            return;
        }

        String saleLineId = txid.split("\\-")[3];

        ReverseGLData request = new ReverseGLData();
        request.setPrimaryKey(Integer.parseInt(saleLineId));
        request.setTableName("EXCDT");
        request.setTransactionType("GLEntry");
        log.debug("Reversing ExciseDuty GL for transaction id [{}], primary key [{}], table name [{}], transaction type [{}]",
                new Object[]{txid, request.getPrimaryKey(), request.getTableName(), request.getTransactionType()});
        try {
            SCAWrapper.getAdminInstance().reverseGL(request);
        } catch (Exception ex) {
            log.warn("Failed to reverse excise tax journal: ", ex);
        }
    }

    private long[] writeHistoryForAccountTransfer(IAccount srcAcc, IAccount destAcc, BigDecimal amntCents, String txid, String txType, int saleId, String description, String srcIPAddress, String info) {
        Date now = new Date();
        long[] ids = new long[2];
        String debitDesc, creditDesc, debitTxType, creditTxType;
        if (saleId > 0) {
            debitDesc = "Account Transfer Debit (" + txType.toUpperCase() + "-" + saleId + ")";
            creditDesc = "Account Transfer Credit (" + txType.toUpperCase() + "-" + saleId + ")";
        } else if (description == null || description.isEmpty()) {
            debitDesc = "Account Transfer Debit (" + txType.toUpperCase() + ")";
            creditDesc = "Account Transfer Credit (" + txType.toUpperCase() + ")";
        } else {
            if (description.length() > 200) {
                description = description.substring(0, 200);
            }
            debitDesc = "Account Transfer Debit (" + txType.toUpperCase() + ") (" + description + ")";
            creditDesc = "Account Transfer Credit (" + txType.toUpperCase() + ") (" + description + ")";
        }
        debitTxType = "txtype.tfr.debit." + txType;
        creditTxType = "txtype.tfr.credit." + txType;
        // Account history row for source account losing money
        AccountHistory ahDebit = DAO.createAccountHistoryForEvent(
                em,
                txid,
                "",
                String.valueOf(destAcc.getAccountId()),
                amntCents,
                srcAcc,
                now,
                debitDesc,
                BigDecimal.ZERO,
                "",
                debitTxType,
                "",
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO, // Deal with unearned on the credit leg
                BigDecimal.ZERO,
                "",
                srcIPAddress,
                "", null, null, null, null, null, info, null);

        ids[0] = ahDebit.getId();

        // Account history row for target account gaining money
        BigDecimal unearned; // Increases unearned if its from a system account or clearing bureau account. Decreases unearned if in the opposite direction
        if (creditTxType.equals("txtype.tfr.credit.cbtu")
                || creditTxType.equals("txtype.tfr.credit.dstu")
                || creditTxType.equals("txtype.tfr.credit.pstu")
                || creditTxType.equals("txtype.tfr.credit.pscp")) {
            unearned = amntCents;
            log.debug("This transfer increases unearned revenue");
        } else if (creditTxType.equals("txtype.tfr.credit.cbtu.reversal")
                || creditTxType.equals("txtype.tfr.credit.dstu.reversal")
                || creditTxType.equals("txtype.tfr.credit.pstu.reversal")
                || creditTxType.equals("txtype.tfr.credit.pscp.reversal")) {
            unearned = amntCents.negate();
            log.debug("This transfer decreases unearned revenue");
        } else if (creditTxType.equals("txtype.tfr.credit.psct") || creditTxType.equals("txtype.tfr.credit.psct.reversal")) {
            if (destAcc.getAccountId() < 1100000000) {
                // Payment from ICP back to Smile (nett Off was negative)
                unearned = amntCents.negate();
                log.debug("This transfer decreases unearned revenue");
            } else {
                unearned = amntCents;
                log.debug("This transfer increases unearned revenue");
            }
        } else {
            unearned = BigDecimal.ZERO;
            log.debug("This transfer does not change unearned revenue");
        }

        AccountHistory ahCredit = DAO.createAccountHistoryForEvent(
                em,
                txid,
                "",
                String.valueOf(srcAcc.getAccountId()),
                amntCents.negate(), // This will be made +'ve in createAccountHistoryForEvent
                destAcc,
                now,
                creditDesc,
                BigDecimal.ZERO,
                "",
                creditTxType,
                "",
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                unearned,
                BigDecimal.ZERO,
                "",
                srcIPAddress,
                "", null, null, null, null, null, info, null);
        ids[1] = ahCredit.getId();
        return ids;
    }

    @Override
    public com.smilecoms.xml.schema.bm.AccountHistory getAccountHistory(AccountHistoryQuery accountHistoryQuery) throws BMError {
        setContext(accountHistoryQuery, wsctx);

        if (log.isDebugEnabled()) {
            logStart();
        }
        com.smilecoms.xml.schema.bm.AccountHistory ah = new com.smilecoms.xml.schema.bm.AccountHistory();
        try {
            List<AccountHistory> ahRows = DAO.getAccountHistory(em, accountHistoryQuery);
            for (AccountHistory ahRow : ahRows) {
                TransactionRecord xmlRecord = getXMLTransactionRecord(ahRow, accountHistoryQuery.getVerbosity());
                ah.getTransactionRecords().add(xmlRecord);
            }
            ah.setResultsReturned(ah.getTransactionRecords().size());

        } catch (Exception e) {
            throw processError(BMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return ah;
    }

    @Override
    public RatePlan getRatePlan(PlatformInteger ratePlanId) throws BMError {
        setContext(ratePlanId, wsctx);

        if (log.isDebugEnabled()) {
            logStart();
        }
        com.smilecoms.xml.schema.bm.RatePlan xmlRp = null;
        try {
            com.smilecoms.bm.db.model.RatePlan dbRp = DAO.getRatePlan(em, ratePlanId.getInteger());
            xmlRp = getXMLRatePlan(dbRp);
            List<com.smilecoms.bm.db.model.RatePlanAvp> avpList = DAO.getRatePlansAvps(em, ratePlanId.getInteger());
            embedAVPsInXMLRatePlan(xmlRp, avpList);

        } catch (Exception e) {
            throw processError(BMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return xmlRp;
    }

    @Override
    public Done addServiceInstanceMappings(ServiceInstanceMappingList serviceInstanceMappingList) throws BMError {
        setContext(serviceInstanceMappingList, wsctx);

        if (log.isDebugEnabled()) {
            logStart();
        }
        try {
            // Delete all existing mappings and then add the ones provided
            DAO.deleteServiceInstancesMappings(em, serviceInstanceMappingList.getServiceInstanceId());
            for (ServiceInstanceMapping serviceInstanceMapping : serviceInstanceMappingList.getServiceInstanceMappings()) {
                DAO.createServiceInstanceMapping(
                        em,
                        serviceInstanceMappingList.getServiceInstanceId(),
                        serviceInstanceMapping.getIdentifier(),
                        serviceInstanceMapping.getIdentifierType());

            }
        } catch (Exception e) {
            throw processError(BMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public com.smilecoms.xml.schema.bm.Account createAccount(com.smilecoms.xml.schema.bm.Account createAccountRequest) throws BMError {
        setContext(createAccountRequest, wsctx);

        if (log.isDebugEnabled()) {
            logStart();
        }
        com.smilecoms.xml.schema.bm.Account xmlAcc = null;
        try {
            IAccount acc = AccountFactory.createAccount(em, ACCOUNT_TYPE.PREPAID);
            xmlAcc = getXMLAccount(acc, "ACCOUNT");
            createEvent(acc.getAccountId());

        } catch (Exception e) {
            throw processError(BMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return xmlAcc;
    }
    
    @Override
    public Done createScheduledAccountHistory(com.smilecoms.xml.schema.bm.ScheduledAccountHistory sched) throws BMError {
        setContext(sched, wsctx);

        if (log.isDebugEnabled()) {
            logStart();
        }
        
        try {
                DAO.setScheduledAccountHistory(em, sched.getAccountId(), sched.getEmailTo(), sched.getFrequency(), sched.getCreatedByProfileId());
                
        } catch (Exception e) {
            throw processError(BMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public Done modifyAccount(com.smilecoms.xml.schema.bm.Account modifyAccountRequest) throws BMError {
        setContext(modifyAccountRequest, wsctx);

        if (log.isDebugEnabled()) {
            logStart();
        }
        try {
            IAccount acc = AccountFactory.getAccount(em, modifyAccountRequest.getAccountId(), false);
            acc.setStatus(modifyAccountRequest.getStatus(), modifyAccountRequest.getPlatformContext().getOriginatingIdentity());
            createEvent(modifyAccountRequest.getAccountId());

        } catch (Exception e) {
            throw processError(BMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public Done modifyServiceInstanceAccount(ServiceInstanceUpdateData modifyServiceInstanceAccountRequest) throws BMError {
        setContext(modifyServiceInstanceAccountRequest, wsctx);

        if (log.isDebugEnabled()) {
            logStart();
        }
        try {
            if (modifyServiceInstanceAccountRequest.getAccountId() > 0 && modifyServiceInstanceAccountRequest.getServiceInstanceId() > 0) {
                log.debug("Going to set up service for account [{}] and serviceInstanceId [{}]", modifyServiceInstanceAccountRequest.getAccountId(), modifyServiceInstanceAccountRequest.getServiceInstanceId());
                DAO.updateServiceInstancesUnitCreditAccount(em, modifyServiceInstanceAccountRequest.getAccountId(), modifyServiceInstanceAccountRequest.getServiceInstanceId());
            }
            createEvent(modifyServiceInstanceAccountRequest.getAccountId());

        } catch (Exception e) {
            throw processError(BMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public Done provisionUnitCredit(ProvisionUnitCreditRequest provisionUnitCreditRequest) throws BMError {
        setContext(provisionUnitCreditRequest, wsctx);

        if (log.isDebugEnabled()) {
            logStart();
        }
        try {

            provisionUnitCredit(em, provisionUnitCreditRequest);

        } catch (Exception e) {
            throw processError(BMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
            if (provisionUnitCreditRequest.isVerifyOnly()) {
                log.debug("This was a validation request so we will roll back the transaction");
                JPAUtils.setRollbackOnly();
            }
        }
        return makeDone();
    }

    public void provisionUnitCredit(EntityManager emLocal, ProvisionUnitCreditRequest provisionUnitCreditRequest) throws Exception {
        em = emLocal;

        if (!provisionUnitCreditRequest.isSkipUniqueTest()) {
            Set<Long> accountsInRequestList = new HashSet();
            for (ProvisionUnitCreditLine line : provisionUnitCreditRequest.getProvisionUnitCreditLines()) {
                accountsInRequestList.add(line.getAccountId());
            }
            checkForUniqueness(provisionUnitCreditRequest.getPlatformContext().getTxId(), accountsInRequestList);
        }

        if (BaseUtils.getBooleanProperty("env.bm.provision.check.enabled", false)) {
            log.debug("Checking UnitCredit provision");
            List<Integer> ucsIdList = new ArrayList<>();
            for (ProvisionUnitCreditLine line : provisionUnitCreditRequest.getProvisionUnitCreditLines()) {
                if (line.getNumberToProvision() <= 0) {
                    throw new Exception("Must provision 1 or more occurences of a unit credit");
                }

                if (line.getUnitCreditSpecificationId() == 0) {

                    if (line.getUnitCreditName() != null && !line.getUnitCreditName().isEmpty()) {
                        log.debug("Provision request is by name not id. Looking up Id based on name [{}]", line.getUnitCreditName());
                        UnitCreditSpecification ucs = DAO.getUnitCreditSpecificationByName(em, line.getUnitCreditName());
                        line.setUnitCreditSpecificationId(ucs.getUnitCreditSpecificationId());
                        log.debug("Unit credit [{}] has Id [{}]", line.getUnitCreditName(), line.getUnitCreditSpecificationId());
                    } else if (line.getItemNumber() != null && !line.getItemNumber().isEmpty()) {
                        log.debug("Provision request is by item number not id. Looking up Id based on item number [{}]", line.getItemNumber());
                        UnitCreditSpecification ucs = DAO.getUnitCreditSpecificationByItemNumber(em, line.getItemNumber());
                        line.setUnitCreditSpecificationId(ucs.getUnitCreditSpecificationId());
                        log.debug("Unit credit [{}] has Id [{}]", line.getItemNumber(), line.getUnitCreditSpecificationId());
                    }
                }

                if (line.getUnitCreditSpecificationId() == 0) {
                    throw new Exception("Invalid unit credit -- Name:" + line.getUnitCreditName() + " ItemNumber:" + line.getItemNumber());
                }
                log.debug("UnitCreditSpecificationId [{}]", line.getUnitCreditSpecificationId());
                ucsIdList.add(line.getUnitCreditSpecificationId());
            }
            for (ProvisionUnitCreditLine line : provisionUnitCreditRequest.getProvisionUnitCreditLines()) {
                log.debug("UnitCreditSpecificationId [{}]", line.getUnitCreditSpecificationId());
                if (line.getNumberToProvision() <= 0) {
                    throw new Exception("Must provision 1 or more occurences of a unit credit");
                }

                if (line.getUnitCreditSpecificationId() == 0) {

                    if (line.getUnitCreditName() != null && !line.getUnitCreditName().isEmpty()) {
                        log.debug("Provision request is by name not id. Looking up Id based on name [{}]", line.getUnitCreditName());
                        UnitCreditSpecification ucs = DAO.getUnitCreditSpecificationByName(em, line.getUnitCreditName());
                        line.setUnitCreditSpecificationId(ucs.getUnitCreditSpecificationId());
                        log.debug("Unit credit [{}] has Id [{}]", line.getUnitCreditName(), line.getUnitCreditSpecificationId());
                    } else if (line.getItemNumber() != null && !line.getItemNumber().isEmpty()) {
                        log.debug("Provision request is by item number not id. Looking up Id based on item number [{}]", line.getItemNumber());
                        UnitCreditSpecification ucs = DAO.getUnitCreditSpecificationByItemNumber(em, line.getItemNumber());
                        line.setUnitCreditSpecificationId(ucs.getUnitCreditSpecificationId());
                        log.debug("Unit credit [{}] has Id [{}]", line.getItemNumber(), line.getUnitCreditSpecificationId());
                    }
                }

                if (line.getUnitCreditSpecificationId() == 0) {
                    throw new Exception("Invalid unit credit -- Name:" + line.getUnitCreditName() + " ItemNumber:" + line.getItemNumber());
                }
                IAccount acc = AccountFactory.getAccount(em, line.getAccountId(), false);
                String conf = UnitCreditManager.getPropertyFromConfig(DAO.getUnitCreditSpecification(em, line.getUnitCreditSpecificationId()), "CanBeSoldWhenSpecIDExist");
                if (!(conf == null || conf.length() <= 0)) {
                    log.debug("UnitCreditSpecificationCongiguration [{}]", conf);
                    List<IUnitCredit> clientExistingUnitCreditCredInstances = UnitCreditManager.getAccountsActiveUnitCredits(em, acc);
                    List<String> specIDsForUpsize = Utils.getListFromCommaDelimitedString(conf);
                    boolean mustExistSpecIdExists = false;

                    log.debug("Checking UnitCredit in the account to provision");
                    for (String mustExistSpecId : specIDsForUpsize) {
                        for (Integer ucsId : ucsIdList) {
                            if (ucsId == Integer.parseInt(mustExistSpecId)) {
                                mustExistSpecIdExists = true;
                                break;
                            }
                        }

                        if (mustExistSpecIdExists) {
                            break;
                        }
                    }

                    if (!mustExistSpecIdExists) {
                        for (String mustExistSpecId : specIDsForUpsize) {
                            for (IUnitCredit clientUnitCred : clientExistingUnitCreditCredInstances) {
                                if (clientUnitCred.getUnitCreditSpecification().getUnitCreditSpecificationId() == Integer.parseInt(mustExistSpecId)) {
                                    mustExistSpecIdExists = true;
                                    break;
                                }
                            }

                            if (mustExistSpecIdExists) {
                                break;
                            }
                        }
                    }

                    if (!mustExistSpecIdExists) {
                        log.warn("Error: Requested to provision add-on product, without main product on account. Requires one of the following SpecIDs on acc: ", specIDsForUpsize);
                        throw new Exception("Missing main product on account. Cannot provision add-on product without main product on account.");
                    }
                }
            }
        }

        for (ProvisionUnitCreditLine line : provisionUnitCreditRequest.getProvisionUnitCreditLines()) {

            if (line.getNumberToProvision() <= 0) {
                throw new Exception("Must provision 1 or more occurences of a unit credit");
            }

            if (line.getUnitCreditSpecificationId() == 0) {

                if (line.getUnitCreditName() != null && !line.getUnitCreditName().isEmpty()) {
                    log.debug("Provision request is by name not id. Looking up Id based on name [{}]", line.getUnitCreditName());
                    UnitCreditSpecification ucs = DAO.getUnitCreditSpecificationByName(em, line.getUnitCreditName());
                    line.setUnitCreditSpecificationId(ucs.getUnitCreditSpecificationId());
                    log.debug("Unit credit [{}] has Id [{}]", line.getUnitCreditName(), line.getUnitCreditSpecificationId());
                } else if (line.getItemNumber() != null && !line.getItemNumber().isEmpty()) {
                    log.debug("Provision request is by item number not id. Looking up Id based on item number [{}]", line.getItemNumber());
                    UnitCreditSpecification ucs = DAO.getUnitCreditSpecificationByItemNumber(em, line.getItemNumber());
                    line.setUnitCreditSpecificationId(ucs.getUnitCreditSpecificationId());
                    log.debug("Unit credit [{}] has Id [{}]", line.getItemNumber(), line.getUnitCreditSpecificationId());
                }
            }

            if (line.getUnitCreditSpecificationId() == 0) {
                throw new Exception("Invalid unit credit -- Name:" + line.getUnitCreditName() + " ItemNumber:" + line.getItemNumber());
            }

            log.debug("Amount paid at POS per unit credit was [{}]", line.getPOSCentsPaidEach());
            IAccount acc = AccountFactory.getAccount(em, line.getAccountId(), false);
            IUnitCredit uc = UnitCreditManager.provisionUnitCredit(
                    em,
                    line.getUnitCreditSpecificationId(),
                    line.getProductInstanceId(), // 0 means the first applicable SI will be used if it does not allow sharing
                    acc,
                    line.getNumberToProvision(),
                    line.getDaysGapBetweenStart(),
                    provisionUnitCreditRequest.isVerifyOnly(),
                    provisionUnitCreditRequest.getPlatformContext().getTxId(),
                    line.getPOSCentsPaidEach(),
                    line.getPOSCentsDiscountEach(),
                    Utils.getJavaDate(line.getStartDate()),
                    provisionUnitCreditRequest.getSaleLineId(),
                    line.getInfo());

            String description;
            if (uc.getUnitCreditInstanceId() > 0 && line.getNumberToProvision() == 1) {
                description = "Purchase " + uc.getUnitCreditName() + " (" + uc.getUnitCreditInstanceId() + ")";
            } else {
                description = "Purchase " + uc.getUnitCreditName();
            }
            if (line.getNumberToProvision() > 1) {
                description += " (" + line.getNumberToProvision() + "X)";
            }

            String info = "SaleLineId=" + provisionUnitCreditRequest.getSaleLineId() + "\r\n" + uc.getPropertyFromConfig("Reporting");

            // Unearned is zero if it was paid for from airtime, otherwise its the total that could be recognised when its used 
            BigDecimal unearned;
            if (provisionUnitCreditRequest.isCreditUnearnedRevenue()) {
                log.debug("This unit credit must credit unearned revenue as it was not paid for by airtime");
                try {
                    unearned = uc.getUnitCreditInstance().getRevenueCentsPerUnit().multiply(uc.getUnitCreditInstance().getUnitsAtStart());
                } catch (UnsupportedOperationException uso) {
                    log.debug("UC does not have units or revenue. Can ignore");
                    unearned = BigDecimal.ZERO;
                }
            } else {
                unearned = BigDecimal.ZERO;
            }

            DAO.createAccountHistoryForEvent(
                    em,
                    provisionUnitCreditRequest.getPlatformContext().getTxId(),
                    "",
                    "",
                    BigDecimal.ZERO,
                    acc,
                    uc.getPurchaseDate(),
                    description,
                    BigDecimal.ZERO,
                    "",
                    "txtype.uc.purchase",
                    "FI",
                    0,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    unearned,
                    BigDecimal.ZERO,
                    "",
                    provisionUnitCreditRequest.getPlatformContext().getOriginatingIP(),
                    "", null, null, null, null, null, info, null);

            if (!provisionUnitCreditRequest.isVerifyOnly()) {
                try {
                    if (uc.getProductInstanceId() > 0
                            && DAO.isPositive(uc.getUnitCreditInstance().getPOSCentsCharged())
                            && !uc.getUnitCreditSpecification().getItemNumber().startsWith("BUNK")) {
                        // Exclude KIts so that new SIMs are not set as active the moment their kit bundle is added
                        log.debug("Making sure PIId [{}] is considered as revenue generating as its paid for and not part of a kit");
                        ServiceInstance serviceInstance = DAO.getProductInstanceOnEmptyServiceInstance(em, uc.getProductInstanceId());
                        DAO.updateProductInstanceBeenUsed(em, serviceInstance, uc.getPurchaseDate(), true, null);
                    }
                } catch (Exception e) {
                    log.warn("Error updating PI been used for UC purchase", e);
                }
                createEvent(uc.getUnitCreditInstanceId());
            }

        } // end for loop
    }

    private void deleteUnitCredit(int ucId, boolean recurse) throws Exception {

        log.debug("Deleting unit credit instance id [{}]", ucId);
        IUnitCredit uc = UnitCreditManager.getLockedWrappedUnitCreditInstance(em, ucId, null);
        IAccount acc = AccountFactory.getAccount(em, uc.getAccountId(), false);
        UnitCreditInstance uci = uc.getUnitCreditInstance();
        if (!Utils.isInTheFuture(uci.getExpiryDate())) {
            throw new Exception("Cannot delete an expired unit credit");
        }

        UnitCreditSpecification ucs = uc.getUnitCreditSpecification();
        if (ucs.getUnitType().equals("VAS")) {
            throw new Exception("Cannot delete a VAS unit credit");
        }

        if (!Utils.isDateInTimeframe(uci.getPurchaseDate(), BaseUtils.getIntProperty("env.bm.uc.deletion.window.days", 7), Calendar.DATE)) {
            throw new Exception("Unit credit is too old to delete -- max days old allowed is " + BaseUtils.getIntProperty("env.bm.uc.deletion.window.days", 7));
        }

        // RecRevDaily=true
        boolean isRecordingRevenueDaily = uc.getBooleanPropertyFromConfig("RecRevDaily");

        if (isRecordingRevenueDaily) { //Time based bundles are not allowed to be rolled back or cancelled after the revenue start date'd day has passed.
            //Check if the revenue first date is in the future.
            //if (!Utils.isInTheFuture(Utils.getEndOfDay(uci.getRevenueFirstDate()))) {
            //  throw new Exception("Unit credits that recognise revenue daily can only be deleted before the end of their first revenue date");
            //}
            //As agreed with Caroline - for now, all time based bundles are not allowed to be deleted if they have any usage > 0 bits.
            BigDecimal unitsUsed = uci.getUnitsAtStart().subtract(uci.getUnitsRemaining());
            if (unitsUsed.compareTo(new BigDecimal(0)) > 0) {
                throw new Exception("Cannot delete a unit credit that has been used -- " + unitsUsed + " units used");
            }
        }

        /*if (uc.getBooleanPropertyFromConfig("RecRevDaily") && !Utils.areDatesOnSameDay(uci.getPurchaseDate(), new Date())) {
            throw new Exception("Unit credits that recognise revenue daily can only be deleted on the date of purchase");
        }*/
        if (uc.getBooleanPropertyFromConfig("ModifyBarred")) {
            throw new Exception("Unit credit of this type is barred from modifications");
        }

        String wasProvisionedAsExtraUC = Utils.getValueFromCRDelimitedAVPString(uci.getInfo(), "IsExtraUC");
        String wasProvisionedAsBonusUC = Utils.getValueFromCRDelimitedAVPString(uci.getInfo(), "IsBonusUC");

        if (!uc.getBooleanPropertyFromConfig("DeletionAllowed")) {
            throw new Exception("Unit credit of this type is barred from deletion");
        }

        // As per IFRS extra unit credits and bonuses must not be deleted
        if ((wasProvisionedAsExtraUC != null && wasProvisionedAsExtraUC.equals("true"))
                || (wasProvisionedAsBonusUC != null && wasProvisionedAsBonusUC.equals("true"))) {
            throw new Exception("Unit credit of this type is barred from deletion");
        }

        if (DAO.isNegative(uci.getUnitsRemaining())) {
            throw new Exception("Unit credit has negative units left");
        }

        // Get the accounts reservations and make sure none of them are for the UC's being deleted
        List<Reservation> resList = DAO.getAccountsReservationList(em, uc.getAccountId());

        BigDecimal unitsUsed = uci.getUnitsAtStart().subtract(uci.getUnitsRemaining());
        if (unitsUsed.compareTo(BaseUtils.getBigDecimalProperty("env.bm.uc.deletion.max.used", new BigDecimal(0))) > 0) {
            throw new Exception("Cannot delete a unit credit that has been used -- " + unitsUsed + " units used");
        }

        for (Reservation res : resList) {
            if (res.getReservationPK().getUnitCreditInstanceId() == uci.getUnitCreditInstanceId()) {
                throw new Exception("Cannot delete a unit credit that has a reservation against it");
            }
        }
        String isSplit = Utils.getValueFromCRDelimitedAVPString(uci.getInfo(), "Split");
        if (isSplit != null && isSplit.equals("true")) {
            throw new Exception("Cannot delete a split unit credit");
        }

        // If this is an unlimited bundle also reverse the OTT tax journal?
        if (BaseUtils.getBooleanProperty("env.bm.social.media.tax.logic.enabled", false)
                && ucs.getWrapperClass().equals("DustUnitCredit")) {
            ReverseGLData request = new ReverseGLData();
            request.setPrimaryKey(uci.getSaleRowId());
            request.setTableName("OTTBUNU");
            request.setTransactionType("GLEntry");
            log.debug("Reversing OTT GL for unlimited unit credit id [{}], primary key [{}], table name [{}], transaction type [{}]",
                    new Object[]{uci.getUnitCreditInstanceId(), request.getPrimaryKey(), request.getTableName(), request.getTransactionType()});
            SCAWrapper.getAdminInstance().reverseGL(request);
        }
        //As discussed with Caroline on 29.11.2018: Timebased unitcredits must give a full refund of what was paid provided the above rules are ok.
        BigDecimal centsValueOfUnitsLeft = null;
        if (isRecordingRevenueDaily) {
            centsValueOfUnitsLeft = uci.getPOSCentsCharged();
            log.debug("Cents value to credit the customer account is [{}]", centsValueOfUnitsLeft);
            //Reverse the time based revenue:
            BigDecimal timeBasedRevenueSoFar = DAO.getTotalTimeBasedUnitCreditRevenueToReverse(em, uci.getUnitCreditInstanceId());
            CreateStandardGLData glData = new CreateStandardGLData();
            glData.setPrimaryKey(uci.getUnitCreditInstanceId());
            glData.setGlAmount(timeBasedRevenueSoFar.doubleValue());
            glData.setGlCreditAccount(Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "ReversalOrDeletionGLCreditAccount"));
            glData.setGlDebitAccount(Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "ReversalOrDeletionGLDebitAccount"));
            glData.setGlDescription("Deletion SR" + uci.getSaleRowId());
            glData.setTableName(Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "ReversalOrDeletionGLX3TransactionCode"));
            glData.setX3GlTransactionCode(Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "ReversalOrDeletionGLX3TransactionCode"));
            SCAWrapper.getAdminInstance().createStandardGL(glData);
        } else {
            uci.setInfo(Utils.setValueInCRDelimitedAVPString(uci.getInfo(), "Deleted", "true"));
            BigDecimal majorCurrencyLeftRounded = DAO.divide(uci.getUnitsRemaining().multiply(uci.getRevenueCentsPerUnit()), new BigDecimal(100)).setScale(0, RoundingMode.CEILING);
            centsValueOfUnitsLeft = majorCurrencyLeftRounded.multiply(new BigDecimal(100));
            log.debug("Cents value of units left is [{}]", centsValueOfUnitsLeft);
        }

        uci.setUnitsRemaining(BigDecimal.ZERO);
        uci.setExpiryDate(new Date());
        uci.setEndDate(new Date());
        em.persist(uci);

        acc.creditBalance(centsValueOfUnitsLeft);

        DAO.createAccountHistoryForEvent(
                em,
                "",
                "",
                "",
                centsValueOfUnitsLeft.negate(), // We are crediting. the DAO negates so - X - = +
                acc,
                new Date(),
                "Deletion of UC " + uci.getUnitCreditInstanceId(),
                BigDecimal.ZERO,
                "",
                "txtype.uc.deletion",
                "FI",
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO, // Unearned does not change as the value is going into the account from unearned data
                BigDecimal.ZERO,
                "",
                "",
                "", null, null, null, null, null, null, null);

        em.flush();
        log.debug("Deleted unit credit instance id [{}]", ucId);

        if (recurse) {
            // Also delete free bundles given with the paid one
            int othersDeleted = 0;
            if (uci.getSaleRowId() > 0 && !uci.getExtTxid().isEmpty()) {
                for (UnitCreditInstance uciSameRow : DAO.getUnitCreditInstancesBySaleRowIdAndExtTxId(em, uci.getSaleRowId(), uci.getExtTxid())) {
                    log.debug("Looking at UCI [{}] to delete as well", uciSameRow.getUnitCreditInstanceId());
                    if (uciSameRow.getUnitCreditInstanceId().equals(uci.getUnitCreditInstanceId())) {
                        // looking at myself
                        continue;
                    }
                    if (DAO.isPositive(uciSameRow.getPOSCentsCharged())) {
                        // Dont delete paid for in the same sale
                        continue;
                    }
                    String isDeleted = Utils.getValueFromCRDelimitedAVPString(uciSameRow.getInfo(), "Deleted");
                    if (isDeleted != null && isDeleted.equals("true")) {
                        // already deleted
                        continue;
                    }

                    othersDeleted++;
                    if (othersDeleted > 100) {
                        // Failsafe
                        throw new Exception("How can this be that there are so many UC for a sale row -- " + uci.getSaleRowId());
                    }
                    try {
                        deleteUnitCredit(uciSameRow.getUnitCreditInstanceId(), false);
                    } catch (Exception e) {
                        log.warn("Could not delete sibling but will continue anyway", e);
                    }
                }
            }
        }

    }

    @Override
    public Done modifyUnitCredit(com.smilecoms.xml.schema.bm.UnitCreditInstance modifyUnitCreditRequest) throws BMError {
        setContext(modifyUnitCreditRequest, wsctx);

        if (log.isDebugEnabled()) {
            logStart();
        }
        try {

            if (modifyUnitCreditRequest.getCurrentUnitsRemaining() == -1) {
                deleteUnitCredit(modifyUnitCreditRequest.getUnitCreditInstanceId(), true);
            } else {
                IUnitCredit uc = UnitCreditManager.getLockedWrappedUnitCreditInstance(em, modifyUnitCreditRequest.getUnitCreditInstanceId(), null);
                UnitCreditInstance uci = uc.getUnitCreditInstance();
                if (!Utils.isInTheFuture(uci.getExpiryDate())) {
                    throw new Exception("Cannot modify an expired unit credit");
                }
                if (!Utils.isInTheFuture(Utils.getJavaDate(modifyUnitCreditRequest.getExpiryDate()))) {
                    throw new Exception("Cannot set an expiry date to the past");
                }

                UnitCreditSpecification ucs = uc.getUnitCreditSpecification();
                if (!BaseUtils.getBooleanProperty("env.bm.vas.allow.switch.services", false) && (modifyUnitCreditRequest.getProductInstanceId() > 0 && modifyUnitCreditRequest.getProductInstanceId() != uci.getProductInstanceId() && ucs.getUnitType().equals("VAS"))) {
                    throw new Exception("Cannot move a VAS unit credit between services");
                }

                if (uc.getBooleanPropertyFromConfig("ModifyBarred")) {
                    throw new Exception("Unit credit of this type is barred from modifications");
                }

                long accountId = modifyUnitCreditRequest.getAccountId() == 0 ? uci.getAccountId() : modifyUnitCreditRequest.getAccountId();

                int piId;
                if (!uc.getBooleanPropertyFromConfig("AllowSharing") && modifyUnitCreditRequest.getProductInstanceId() == 0) {
                    piId = uci.getProductInstanceId();
                } else {
                    piId = modifyUnitCreditRequest.getProductInstanceId();
                }

                if (piId > 0) {
                    List<ServiceInstance> serviceInstancesOnAccount = DAO.getServiceInstancesForAccount(em, accountId);
                    boolean foundOne = false;
                    for (ServiceInstance si : serviceInstancesOnAccount) {
                        if (si.getProductInstanceId() == piId) {
                            foundOne = true;
                            break;
                        }
                    }
                    if (!foundOne) {
                        throw new Exception("Product Instance does not exist on that account");
                    }
                }

                if (modifyUnitCreditRequest.getAccountId() > 0) {

                    if ((modifyUnitCreditRequest.getAccountId() != uc.getAccountId())
                            && uc.getBooleanPropertyFromConfig("ChangeAccountBarred")) {
                        throw new Exception("Unit credit of this type is barred from account changes");
                    }
                    uci.setAccountId(modifyUnitCreditRequest.getAccountId());
                }

                boolean isRecordingRevenueDaily = uc.getBooleanPropertyFromConfig("RecRevDaily");

                if (isRecordingRevenueDaily) { // Dates on time Based bundles are not allowed to be changed if revenue first dateis in the past..
                    if ((uci.getRevenueFirstDate() == null || !Utils.isInTheFuture(uci.getRevenueFirstDate())) && (!uci.getStartDate().equals(Utils.getJavaDate(modifyUnitCreditRequest.getStartDate()))
                            || !uci.getEndDate().equals(Utils.getJavaDate(modifyUnitCreditRequest.getEndDate()))
                            || !uci.getExpiryDate().equals(Utils.getJavaDate(modifyUnitCreditRequest.getExpiryDate())))) {
                        throw new Exception("Unit credit of this type is barred from any date changes");
                    }
                    // Modify revenue per day as agreed with Caroline over the OTT taxt in Uganda.
                    if (BaseUtils.getBooleanProperty("env.bm.social.media.tax.logic.enabled", false)) {
                        if (modifyUnitCreditRequest.getRevenueCentsPerDay() > 0
                                && modifyUnitCreditRequest.getRevenueCentsPerDay() != (uci.getRevenueCentsPerDay() == null ? 0.0 : uci.getRevenueCentsPerDay().doubleValue())) {
                            uci.setRevenueCentsPerDay(new BigDecimal(modifyUnitCreditRequest.getRevenueCentsPerDay()));
                        }
                    }
                }

                uci.setProductInstanceId(piId);
                if (modifyUnitCreditRequest.getStartDate() != null) {
                    uci.setStartDate(Utils.getJavaDate(modifyUnitCreditRequest.getStartDate()));
                }
                if (modifyUnitCreditRequest.getEndDate() != null) {
                    uci.setEndDate(Utils.getJavaDate(modifyUnitCreditRequest.getEndDate()));
                }
                if (modifyUnitCreditRequest.getExpiryDate() != null) {
                    uci.setExpiryDate(Utils.getJavaDate(modifyUnitCreditRequest.getExpiryDate()));
                }
                if (modifyUnitCreditRequest.getInfo() != null) {
                    uci.setInfo(modifyUnitCreditRequest.getInfo());
                }
                if (uci.getEndDate() == null || uci.getEndDate().after(uci.getExpiryDate())) {
                    uci.setEndDate(uci.getExpiryDate());
                }

                if (uci.getStartDate().after(uci.getEndDate()) || uci.getStartDate().after(uci.getExpiryDate())) {
                    throw new Exception("Expiry or end date cannot be before start date");
                }
                em.persist(uci);
                em.flush();
            }
            createEvent(modifyUnitCreditRequest.getUnitCreditInstanceId());

        } catch (Exception e) {
            throw processError(BMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public Done deleteAccount(PlatformLong deleteAccountRequest) throws BMError {
        setContext(deleteAccountRequest, wsctx);

        if (log.isDebugEnabled()) {
            logStart();
        }
        try {
            DAO.deleteAccount(em, deleteAccountRequest.getLong());
            createEvent(deleteAccountRequest.getLong());

        } catch (Exception e) {
            throw processError(BMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public ServiceInstanceMappingList getServiceInstanceMappings(PlatformInteger serviceInstanceId) throws BMError {
        setContext(serviceInstanceId, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        ServiceInstanceMappingList mappingsXML = new ServiceInstanceMappingList();
        mappingsXML.setServiceInstanceId(serviceInstanceId.getInteger());
        try {
            List<com.smilecoms.bm.db.model.ServiceInstanceMapping> mappingsDB = DAO.getServiceInstanceMappings(em, serviceInstanceId.getInteger());
            for (com.smilecoms.bm.db.model.ServiceInstanceMapping mappingDB : mappingsDB) {
                ServiceInstanceMapping mappingXML = new ServiceInstanceMapping();
                mappingXML.setIdentifier(mappingDB.getServiceInstanceMappingPK().getIdentifier());
                mappingXML.setIdentifierType(mappingDB.getServiceInstanceMappingPK().getIdentifierType());
                mappingsXML.getServiceInstanceMappings().add(mappingXML);

            }
        } catch (Exception e) {
            throw processError(BMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return mappingsXML;
    }

    @Override
    public Done deleteServiceInstanceMappings(ServiceInstanceMappingList serviceInstanceMappingListToDelete) throws BMError {
        setContext(serviceInstanceMappingListToDelete, wsctx);

        if (log.isDebugEnabled()) {
            logStart();
        }
        try {
            if (serviceInstanceMappingListToDelete.getServiceInstanceMappings().isEmpty()) {
                log.debug("No elements in the list so this means we must just delete all mappings associated to the service instance id");
                DAO.deleteServiceInstancesMappings(em, serviceInstanceMappingListToDelete.getServiceInstanceId());
            } else {
                for (ServiceInstanceMapping serviceInstanceMapping : serviceInstanceMappingListToDelete.getServiceInstanceMappings()) {
                    DAO.deleteServiceInstanceMapping(
                            em,
                            serviceInstanceMappingListToDelete.getServiceInstanceId(),
                            serviceInstanceMapping.getIdentifier(),
                            serviceInstanceMapping.getIdentifierType());
                }
            }
            createEvent(serviceInstanceMappingListToDelete.getServiceInstanceId());

        } catch (Exception e) {
            throw processError(BMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public Done replaceServiceInstanceMappings(ServiceInstanceMappingsReplacementData serviceInstanceMappingsReplacementData) throws BMError {
        setContext(serviceInstanceMappingsReplacementData, wsctx);

        if (log.isDebugEnabled()) {
            logStart();
        }
        try {
            log.debug("Replacing SI mappings [{}][{}] is changing to [{}][{}]", new Object[]{
                serviceInstanceMappingsReplacementData.getOldServiceInstanceMapping().getIdentifier(),
                serviceInstanceMappingsReplacementData.getOldServiceInstanceMapping().getIdentifierType(),
                serviceInstanceMappingsReplacementData.getNewServiceInstanceMapping().getIdentifier(),
                serviceInstanceMappingsReplacementData.getNewServiceInstanceMapping().getIdentifierType()});

            DAO.replaceMappings(em, serviceInstanceMappingsReplacementData.getOldServiceInstanceMapping().getIdentifier(),
                    serviceInstanceMappingsReplacementData.getOldServiceInstanceMapping().getIdentifierType(),
                    serviceInstanceMappingsReplacementData.getNewServiceInstanceMapping().getIdentifier(),
                    serviceInstanceMappingsReplacementData.getNewServiceInstanceMapping().getIdentifierType());

            createEvent(serviceInstanceMappingsReplacementData.getOldServiceInstanceMapping().getIdentifier());

        } catch (Exception e) {
            throw processError(BMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    private com.smilecoms.xml.schema.bm.Account getXMLAccount(IAccount acc, String verbosity) throws Exception {
        com.smilecoms.xml.schema.bm.Account xmlAcc = new com.smilecoms.xml.schema.bm.Account();
        xmlAcc.setAvailableBalanceInCents(AccountFactory.getAccount(em, acc.getAccountId(), true).getAvailableBalanceCents().doubleValue());
        xmlAcc.setCurrentBalanceInCents(acc.getCurrentBalanceCents().doubleValue());
        xmlAcc.setAccountId(acc.getAccountId());
        xmlAcc.setStatus(acc.getStatus());
        if (verbosity.contains("UNITCREDITS")) {
            List<UnitCreditInstance> uciList = DAO.getUnitCreditInstances(em, acc.getAccountId());
            for (UnitCreditInstance uci : uciList) {
                xmlAcc.getUnitCreditInstances().add(getXMLUnitCreditInstance(uci));
            }
        }
        if (verbosity.contains("RESERVATIONS")) {
            List<Reservation> resList = DAO.getAccountsReservationList(em, acc.getAccountId(), BaseUtils.getIntProperty("env.bm.accounts.reservations.query.limit", 50));
            for (Reservation res : resList) {
                xmlAcc.getReservations().add(getXMLReservation(res));
            }
        }
        return xmlAcc;
    }

    private com.smilecoms.xml.schema.bm.Reservation getXMLReservation(Reservation dbReservation) {
        com.smilecoms.xml.schema.bm.Reservation xmlReservation = new com.smilecoms.xml.schema.bm.Reservation();
        xmlReservation.setAccountId(dbReservation.getReservationPK().getAccountId());
        xmlReservation.setAmountInCents(dbReservation.getAmountCents().doubleValue());
        xmlReservation.setExpiryDate(Utils.getDateAsXMLGregorianCalendar(dbReservation.getReservationExpiryTimestamp()));
        xmlReservation.setReservationDate(Utils.getDateAsXMLGregorianCalendar(dbReservation.getReservationEventTimestamp()));
        xmlReservation.setSessionId(dbReservation.getReservationPK().getSessionId());
        xmlReservation.setUnitCreditInstanceId(dbReservation.getReservationPK().getUnitCreditInstanceId());
        xmlReservation.setUnitCreditUnits(dbReservation.getAmountUnitCredits().doubleValue());

        List<AccountHistory> ahList = DAO.getAccountHistoryInReverseOrderForTxId(em, dbReservation.getReservationPK().getSessionId());
        if (!ahList.isEmpty()) {
            xmlReservation.setDescription(ahList.get(0).getDescription());
        } else {
            String sessionId = dbReservation.getReservationPK().getSessionId().toLowerCase();
            log.debug("Reservation has no row in account history. Going to decide on the description based on the sessionId [{}]", sessionId);

            if (sessionId.contains("_sms_")) {
                xmlReservation.setDescription("SMS");
            } else if (sessionId.contains("scscf")) {
                xmlReservation.setDescription("Voice");
            } else if (sessionId.contains("pg")) {
                xmlReservation.setDescription("Data");
            } else {
                xmlReservation.setDescription("Unknown");
            }
        }
        return xmlReservation;
    }

    private com.smilecoms.xml.schema.bm.UnitCreditInstance getXMLUnitCreditInstance(UnitCreditInstance uci) {
        com.smilecoms.xml.schema.bm.UnitCreditInstance xmlUCI = new com.smilecoms.xml.schema.bm.UnitCreditInstance();
        xmlUCI.setAccountId(uci.getAccountId());
        xmlUCI.setExpiryDate(Utils.getDateAsXMLGregorianCalendar(uci.getExpiryDate()));
        xmlUCI.setUnitCreditInstanceId(uci.getUnitCreditInstanceId());
        UnitCreditSpecification ucs = DAO.getUnitCreditSpecification(em, uci.getUnitCreditSpecificationId());
        xmlUCI.setName(ucs.getUnitCreditName());
        xmlUCI.setUnitType(ucs.getUnitType());
        xmlUCI.setPurchaseDate(Utils.getDateAsXMLGregorianCalendar(uci.getPurchaseDate()));
        xmlUCI.setStartDate(Utils.getDateAsXMLGregorianCalendar(uci.getStartDate()));
        xmlUCI.setEndDate(Utils.getDateAsXMLGregorianCalendar(uci.getEndDate() == null ? uci.getExpiryDate() : uci.getEndDate()));
        xmlUCI.setUnitCreditSpecificationId(uci.getUnitCreditSpecificationId());
        xmlUCI.setSaleLineId(uci.getSaleRowId());
        xmlUCI.setRevenueCentsPerDay(uci.getRevenueCentsPerDay() == null ? 0 : uci.getRevenueCentsPerDay().doubleValue());
        xmlUCI.setRevenueCentsPerUnit(uci.getRevenueCentsPerUnit() == null ? 0 : uci.getRevenueCentsPerUnit().doubleValue());
        xmlUCI.setPOSCentsCharged(uci.getPOSCentsCharged() == null ? 0 : uci.getPOSCentsCharged().doubleValue());
        xmlUCI.setCurrentUnitsRemaining(uci.getUnitsRemaining().doubleValue());
        xmlUCI.setAvailableUnitsRemaining(DAO.getUnitCreditsAvailableUnits(em, uci).doubleValue());
        xmlUCI.setProductInstanceId(uci.getProductInstanceId());
        xmlUCI.setExtTxId(uci.getExtTxid() == null ? "" : uci.getExtTxid());
        xmlUCI.setInfo(uci.getInfo() == null ? "" : uci.getInfo());
        xmlUCI.setUnitsAtStart(uci.getUnitsAtStart().doubleValue());
        xmlUCI.setAuxCounter1(uci.getAuxCounter1() == null ? 0.0d : uci.getAuxCounter1().doubleValue());
        return xmlUCI;
    }

    private TransactionRecord getXMLTransactionRecord(AccountHistory ahRow, String verbosity) {
        TransactionRecord rec = new TransactionRecord();
        rec.setAccountId(ahRow.getAccountId());
        rec.setAmountInCents(ahRow.getAccountCents().doubleValue());
        rec.setAccountBalanceRemainingInCents(ahRow.getAccountBalanceRemaining().doubleValue());
        rec.setStartDate(Utils.getDateAsXMLGregorianCalendar(ahRow.getStartDate()));
        rec.setEndDate(Utils.getDateAsXMLGregorianCalendar(ahRow.getEndDate()));
        rec.setDescription(ahRow.getDescription());
        rec.setDestination(ahRow.getDestination());
        rec.setExtTxId(ahRow.getExtTxId());
        rec.setTransactionRecordId(ahRow.getId());
        rec.setServiceInstanceId(ahRow.getServiceInstanceId());
        rec.setSource(ahRow.getSource());
        rec.setSourceDevice(ahRow.getSourceDevice());
        rec.setLocation(ahRow.getLocation());
        rec.setStatus(ahRow.getStatus());
        rec.setTermCode(ahRow.getTermCode());
        rec.setTransactionType(ahRow.getTransactionType());
        rec.setTotalUnits(ahRow.getTotalUnits().doubleValue());
        rec.setUnitCreditUnits(ahRow.getUnitCreditUnits().doubleValue());
        rec.setIPAddress(ahRow.getIPAddress());
        rec.setInfo(ahRow.getInfo());
        rec.setServiceInstanceIdentifier(ahRow.getServiceInstanceIdentifier());
        rec.setUnitCreditBaselineUnits(ahRow.getUnitCreditBaselineUnits() == null ? 0 : ahRow.getUnitCreditBaselineUnits().doubleValue());
        if (verbosity != null && verbosity.contains("DETAIL")) {
            log.debug("Getting charging detail for record [{}]", rec.getTransactionRecordId());
            String details = DAO.getChargingDetail(em, rec.getTransactionRecordId());
            String detail = Utils.zip(details);
            rec.setChargingDetail(detail);
        }
        return rec;
    }

    private RatePlan getXMLRatePlan(com.smilecoms.bm.db.model.RatePlan dbRp) {
        RatePlan xmlRp = new RatePlan();
        xmlRp.setDescription(dbRp.getRatePlanDescription());
        xmlRp.setRatePlanId(dbRp.getRatePlanId());
        xmlRp.setName(dbRp.getRatePlanName());
        xmlRp.setEventBased(Utils.booleanValue(dbRp.getEventBased()));
        xmlRp.setRatingEngineClass(dbRp.getRatingEngineClass());
        xmlRp.setSessionBased(Utils.booleanValue(dbRp.getSessionBased()));
        return xmlRp;
    }

    private void embedAVPsInXMLRatePlan(RatePlan xmlRp, List<RatePlanAvp> avpList) {
        List<AVP> xmlAVPList = xmlRp.getAVPs();
        for (RatePlanAvp dbAvp : avpList) {
            AVP xmlAvp = new AVP();
            xmlAvp.setAttribute(dbAvp.getRatePlanAvpPK().getAttribute());
            xmlAvp.setValue(dbAvp.getValue());
            xmlAVPList.add(xmlAvp);
        }
    }

    @Override
    public Done isUp(String isUpRequest) throws BMError {
        if (!BaseUtils.isPropsAvailable()) {
            throw createError(BMError.class, "Properties are not available so this platform will be reported as down");
        }
        return makeDone();
    }

    /**
     * Utility method to make a complex boolean object with value TRUE.
     *
     * @return The resulting complex object
     */
    private com.smilecoms.xml.schema.bm.Done makeDone() {
        com.smilecoms.xml.schema.bm.Done done = new com.smilecoms.xml.schema.bm.Done();
        done.setDone(com.smilecoms.xml.schema.bm.StDone.TRUE);
        return done;
    }

    private void checkForUniqueness(String txId, Set<Long> accounts) throws Exception {

        if (txId == null) {
            return;
        }
        if (txId.startsWith("UNIQUE-")) {
            if (accounts != null) {
                for (long acc : accounts) {
                    log.debug("Locking account [{}]", acc);
                    DAO.getLockedAccount(em, acc);
                    log.debug("Locked account [{}]", acc);
                }
            }
            log.debug("This external transaction Id should be unique. Checking if [{}] exists in account history", txId);
            try {
                List<AccountHistory> rows = DAO.getAccountHistoryInReverseOrderForTxId(em, txId);
                for (AccountHistory ah : rows) {
                    // If we get here, then the transaction exists
                    if (!ah.getTransactionType().endsWith(".reversed")) {
                        throw new Exception("External transaction Id should be unique but is not -- " + txId);
                    } else {
                        log.debug("The row [{}] has been reversed so its ok to proceed", ah.getId());
                    }
                }
            } catch (javax.persistence.NoResultException e) {
                // This is good - it means no row exists
                log.debug("Row not found so the tx id does not exist. Can proceed with transaction");
            }
        }
    }

    private void doAdditionalPostTransferProcessing(IAccount srcAccount, IAccount dstAccount, BigDecimal amnt, String transferType) throws Exception {
        log.debug("In doAdditionalPostTransferProcessing");

        /*
         * 5. Business rules
         * (a) Customer tops up his airtime account balance with UGX 25,000 or more.
         * (b) The validity of all active paid data bundles at the time of the top-up is automatically extended by another 30 days.
         * (c) The validity of any free data bundle given to customer cannot be extended.
         * (d) The validity of any paid data bundle can only be extended once (i.e. maximum total validity of any paid bundle is 60 days).
         */
        if (transferType.equals("dstu")
                || transferType.equals("cstu")
                || transferType.equals("idstu")
                || transferType.equals("ttu")
                || transferType.equals("cbtu")
                || transferType.equals("daat")
                || transferType.equals("dars")) {

            log.debug("Transfer type is [{}] which qualifies for unit credit extension", transferType);
            if (amnt.doubleValue() >= BaseUtils.getDoubleProperty("env.bm.unitcredit.expiry.extend.topup.amount.cents", Double.MAX_VALUE)) {
                log.debug("This topup amount qualifies for extending the validity time all active, started, paid-for unit credits (only once per UC)");
                List<IUnitCredit> activeUCs = UnitCreditManager.getAccountsActiveUnitCredits(em, dstAccount);
                for (IUnitCredit uci : activeUCs) {
                    log.debug("Looking at UCI [{}] to see if it can be extended", uci.getUnitCreditInstanceId());
                    String extensionCount = uci.getInfoValue("ExtensionCount");
                    if (extensionCount == null || extensionCount.equals("0")) {
                        log.debug("UCI has never been extended");
                        if (!uci.neverBeenUsed()) {
                            log.debug("UC has started to be used");
                            if (uci.paidFor()) {
                                log.debug("This UCI had been paid for. All business rules say this UCI can be extended by 30 days");
                                uci.setInfoValue("ExtensionCount", "1");
                                uci.extendExpiryAndEndDate(BaseUtils.getIntProperty("env.bm.unitcredit.expiry.extend.days", 30), false, true, true, null);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public TransferGraph getTransferGraph(TransferGraphQuery transferGraphQuery) throws BMError {
        setContext(transferGraphQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        TransferGraph result = new TransferGraph();
        try {
            com.smilecoms.bm.util.TransferGraph graph = new com.smilecoms.bm.util.TransferGraph(em);
            result.setGraph(graph.getTransferGraph(transferGraphQuery.getRootAccountId(),
                    Utils.getJavaDate(transferGraphQuery.getStartDate()),
                    Utils.getJavaDate(transferGraphQuery.getEndDate()),
                    transferGraphQuery.getRecursions(),
                    transferGraphQuery.getDebitType(),
                    transferGraphQuery.getRegexMatch()));

        } catch (Exception e) {
            throw processError(BMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return result;
    }

    @Override
    public Done updatePortingData(PortingData portingData) throws BMError {
        setContext(portingData, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        try {
            InterconnectPartner ic = DAO.getInterconnectPartnerByCode(em, portingData.getInterconnectPartnerCode());
            log.debug("Interconnect partner [{}] has Id [{}]", ic.getPartnerName(), ic.getInterconnectPartnerId());
            for (long num = portingData.getStartE164(); num <= portingData.getEndE164(); num++) {
                log.debug("Adding [{}] as belonging to [{}]", num, ic.getPartnerName());
                DAO.insertPortedNumber(em, num, ic.getInterconnectPartnerId());

            }
        } catch (Exception e) {
            throw processError(BMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public Done splitUnitCredit(SplitUnitCreditData splitUnitCreditRequest) throws BMError {
        setContext(splitUnitCreditRequest, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        try {
            UnitCreditManager.splitUnitCredit(em, splitUnitCreditRequest.getUnitCreditInstanceId(),
                    splitUnitCreditRequest.getTargetAccountId(),
                    splitUnitCreditRequest.getUnits(),
                    splitUnitCreditRequest.getTargetProductInstanceId());
            createEvent(splitUnitCreditRequest.getUnitCreditInstanceId());

        } catch (Exception e) {
            throw processError(BMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public AccountSummary getAccountSummary(AccountSummaryQuery accountSummaryQuery) throws BMError {
        setContext(accountSummaryQuery, wsctx);

        if (log.isDebugEnabled()) {
            logStart();
        }
        com.smilecoms.xml.schema.bm.AccountSummary as = new com.smilecoms.xml.schema.bm.AccountSummary();
        try {
            List<Object[]> asRows = DAO.getAccountSummary(em, accountSummaryQuery);
            log.debug("Summary query returned [{}] rows", asRows.size());
            for (Object[] asRow : asRows) {
                PeriodSummary xmlRecord = getXMLPeriodSummaryRecord(asRow);
                as.getPeriodSummaries().add(xmlRecord);
            }
            as.setResultsReturned(as.getPeriodSummaries().size());

        } catch (Exception e) {
            throw processError(BMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return as;
    }

    private PeriodSummary getXMLPeriodSummaryRecord(Object[] asRow) {
        PeriodSummary xmlSummary = new PeriodSummary();
        xmlSummary.setPeriod((String) asRow[0]);

        String txTypeUnfriendly = (String) asRow[1];
        String txTypeFriendly;
        if (txTypeUnfriendly.contains(".32251")) {
            txTypeFriendly = "Data";
        } else if (txTypeUnfriendly.contains(".32260")) {
            txTypeFriendly = "Voice";
        } else {
            txTypeFriendly = txTypeUnfriendly;
        }
        xmlSummary.setTransactionType(txTypeFriendly);
        xmlSummary.setTotalUnits(Utils.getObjectAsDouble(asRow[2], 0));
        xmlSummary.setAmountInCents(Utils.getObjectAsDouble(asRow[3], 0));
        xmlSummary.setUnitCreditUnits(Utils.getObjectAsDouble(asRow[4], 0));
        xmlSummary.setUnitCreditBaselineUnits(Utils.getObjectAsDouble(asRow[5], 0));
        return xmlSummary;
    }

    public boolean createEventVoiceBundleRequiresDataBundle(Object inputMessage, Logger log) throws Exception {
        log.debug("createEventVoiceBundleRequiresDataBundle");
        com.smilecoms.xml.schema.bm.ChargingRequest cr = (com.smilecoms.xml.schema.bm.ChargingRequest) inputMessage;
        String serviceInstanceIdentifier = ((com.smilecoms.xml.schema.bm.ChargingData) (cr.getChargingData().get(0))).getServiceInstanceIdentifier().getIdentifier();
        log.debug("serviceInstanceIdentifier " + serviceInstanceIdentifier);

        com.smilecoms.commons.sca.Customer cust = null;

        int pIId = -1;
        com.smilecoms.commons.sca.SCAWrapper SCAWrapper = com.smilecoms.commons.sca.SCAWrapper.getAdminInstance();
        try {
            com.smilecoms.commons.sca.ServiceInstanceQuery siq = new com.smilecoms.commons.sca.ServiceInstanceQuery();
            siq.setVerbosity(com.smilecoms.commons.sca.StServiceInstanceLookupVerbosity.MAIN);
            siq.setIdentifier(serviceInstanceIdentifier);
            siq.setIdentifierType("END_USER_SIP_URI");
            log.debug("Getting the service instance for SIP URI " + serviceInstanceIdentifier);
            com.smilecoms.commons.sca.ServiceInstance si = SCAWrapper.getServiceInstance(siq);
            pIId = si.getProductInstanceId();
            if (si != null) {
                log.debug("SI found with customer id " + si.getCustomerId());
                cust = SCAWrapper.getCustomer(si.getCustomerId(), com.smilecoms.commons.sca.StCustomerLookupVerbosity.CUSTOMER);
            } else {
                log.debug("SI not found");
            }
        } catch (Exception e) {
            log.debug("No customer for serviceInstanceIdentifier " + serviceInstanceIdentifier + " " + e.toString());
        }

        if (cust != null && pIId != -1) {
            log.debug("Have found a customer id " + cust.getCustomerId() + " so will log event");
            String date = new SimpleDateFormat("H-dd-MM-yyyy").format(new Date());
            String subType = "VOICE_BUNDLE_REQUIRES_DATA_BUNDLE";
            com.smilecoms.commons.sca.Event eventData = new com.smilecoms.commons.sca.Event();
            eventData.setEventType("CL_VOICE");
            eventData.setEventSubType(subType);
            eventData.setEventKey(String.valueOf(cust.getCustomerId()));
            eventData.setEventData("CustId=" + cust.getCustomerId() + "\r\nPIId=" + pIId);
            //this ensures event is created async
            eventData.setSCAContext(new com.smilecoms.commons.sca.SCAContext());
            eventData.getSCAContext().setAsync(java.lang.Boolean.TRUE);
            eventData.setUniqueKey("CL_VOICE_" + subType + "_" + cust.getCustomerId() + "_" + pIId + "_" + date);
            SCAWrapper.createEvent(eventData);
        } else {
            log.debug("Customer id not found");
        }
        return true;
    }

    //NG first use Code env.bm.pi.firstuse.code
    public String piFirstUseNG(javax.persistence.EntityManager em, Logger log, com.smilecoms.xml.schema.bm.ServiceInstanceIdentifier siIdentifier, int productInstanceId, int serviceInstanceId, long accountIdToCharge, String userEquipment) throws Exception {
        log.debug("Checking if product instance is allowed to be used for the first time");

        String query = "SELECT COUNT(D.DEVICE) "
                + "FROM product_instance PI "
                + "JOIN sale_row SIM_SALE_ROW on PI.PHYSICAL_ID=SIM_SALE_ROW.SERIAL_NUMBER "
                + "JOIN sale S on (S.SALE_ID = SIM_SALE_ROW.SALE_ID AND S.STATUS='PD') "
                + "JOIN sale_row KIT_SALE_ROW on KIT_SALE_ROW.SALE_ROW_ID = SIM_SALE_ROW.PARENT_SALE_ROW_ID "
                + "JOIN device_seen D on (D.DEVICE=? AND (D.FIRST_SEEN_DATETIME < now()-interval 6 hour OR D.FIRST_SEEN_DATETIME is null)) "
                + "WHERE KIT_SALE_ROW.ITEM_NUMBER IN ('KIT1287', 'KIT1288', 'KIT1289', 'KIT1290', 'KIT1293') "
                + "AND PI.PRODUCT_INSTANCE_ID = ?";

        javax.persistence.Query q = em.createNativeQuery(query);
        q.setParameter(1, userEquipment);
        q.setParameter(2, new Integer(productInstanceId));
        long cnt = ((java.lang.Long) q.getSingleResult()).longValue();
        if (cnt >= 1) {
            log.debug("We have seen this device before - this product must be first used on a new device");
            return "Device seen before";
        }

        log.debug("This PI is allowed to attach for the first time");
        return "true";
    }

    @Override
    public MaximumExpiryDateOfUnitCreditOnAccountReply getMaximumExpiryDateOfUnitCreditOnAccount(MaximumExpiryDateOfUnitCreditOnAccountQuery maximumExpiryDateOfUnitCreditOnAccountQuery) throws BMError {

        Date maxExpirtyDate = DAO.getMaximumExpiryDateOfUnitCreditWithThisTypeExcludingCurrentOne(em, maximumExpiryDateOfUnitCreditOnAccountQuery.getWrapperClass(),
                maximumExpiryDateOfUnitCreditOnAccountQuery.getAccountId(),
                maximumExpiryDateOfUnitCreditOnAccountQuery.getExcludeUnitCreditInstanceId());
        MaximumExpiryDateOfUnitCreditOnAccountReply reply = new MaximumExpiryDateOfUnitCreditOnAccountReply();
        reply.setMaximumExpiryDate(Utils.getDateAsXMLGregorianCalendar(maxExpirtyDate));

        return reply;
    }

    public int getCustomerIdByAccountId(long accountId) throws Exception {
        try {

            if (accountId <= 1200000000) {
                throw new Exception("No such customer -- [AccountId: " + accountId + "]");
            }

            Set<String> accountsNotallowed;
            try {
                accountsNotallowed = BaseUtils.getPropertyAsSet("env.tpgw.accounts.validate.fail");
            } catch (Exception e) {
                log.debug("env.tpgw.accounts.validate.fail does not exist");
                accountsNotallowed = new HashSet<>();
            }

            if (accountsNotallowed.contains(String.valueOf(accountId))) {
                throw new Exception("No such customer -- [AccountId: " + accountId + "]");
            }

            // PCB - ensure account can take a credit and buy a bundle or else say its invalid
            Account acc = SCAWrapper.getAdminInstance().getAccount(accountId, StAccountLookupVerbosity.ACCOUNT);
            if ((1 & acc.getStatus()) != 0 || (2 & acc.getStatus()) != 0) {
                throw new Exception("Invalid customer account detected -- Account Status is " + acc.getStatus() + " which does not allow debiting and crediting");
            }

            ServiceInstanceQuery serviceInstanceQuery = new ServiceInstanceQuery();

            serviceInstanceQuery.setAccountId(accountId);
            serviceInstanceQuery.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
            ServiceInstanceList serviceInstances = SCAWrapper.getUserSpecificInstance().getServiceInstances(serviceInstanceQuery);

            // - Get the first service instance, extract the customer and use that to pull the profile
            if (serviceInstances.getServiceInstances().size() <= 0) {
                throw new Exception("No such customer -- [AccountId: " + accountId + "]");
            }

            //Check that account has no service instance >= 1000 - to prevent Staff accounts from transacting
            for (com.smilecoms.commons.sca.ServiceInstance pi : serviceInstances.getServiceInstances()) {
                if (pi.getServiceSpecificationId() >= 1000) {
                    throw new Exception("Invalid customer account detected -- [ServiceInstanceId " + pi.getServiceInstanceId()
                            + ", ServiceInstanceSpecificationId: " + pi.getServiceSpecificationId()
                            + ", AccountId: " + accountId);
                }
            }

            com.smilecoms.commons.sca.ServiceInstance si = serviceInstances.getServiceInstances().get(0);
            CustomerQuery customerQuery = new CustomerQuery();
            customerQuery.setCustomerId(si.getCustomerId());

            log.debug("Looking up customer with id [{}]", customerQuery.getCustomerId());
            customerQuery.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
            Customer customer = SCAWrapper.getUserSpecificInstance().getCustomer(customerQuery);
            return customer.getCustomerId();

        } catch (Exception ex) {
            // throw ex;
            log.warn("Error while trying to validate account with acccount id - [{}] - error message : [{}]", new Object[]{accountId, ex.getMessage()});
            throw new Exception(ex);
        }

    }

}
