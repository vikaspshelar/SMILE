/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.cm;

import com.smilecoms.cm.db.op.DAO;
import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;
import com.smilecoms.commons.platform.PlatformEventManager;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author richard
 */
@Singleton
@Startup
@Local({BaseListener.class})
public class CustomerLifecycleDaemon implements BaseListener {

    private static final Logger log = LoggerFactory.getLogger(CustomerLifecycleDaemon.class);
    private EntityManagerFactory emf = null;
    private EntityManager em;
    private static ScheduledFuture runner1, runner2 = null;
    private static final SimpleDateFormat sdfCLStart = new SimpleDateFormat("yyMM");

    @Override
    public void propsAreReadyTrigger() {
        runner1 = Async.scheduleAtFixedRate(new SmileBaseRunnable("CM.customerLifecycle") {
            @Override
            public void run() {
                trigger();
            }
        }, 10000, 10 * 60000, 20 * 60000);

        runner2 = Async.scheduleAtFixedRate(new SmileBaseRunnable("CM.KYCLifecycle") {
            @Override
            public void run() {
                triggerKYC();
            }
        }, 20000, 10 * 60000, 20 * 60000);
    }

    @PostConstruct
    public void startUp() {
        BaseUtils.registerForPropsAvailability(this);
        emf = JPAUtils.getEMF("CMPU_RL");
    }

    @PreDestroy
    public void shutDown() {
        BaseUtils.deregisterForPropsAvailability(this);
        Async.cancel(runner1);
        Async.cancel(runner2);
        BaseUtils.deregisterForPropsChanges(this);
        JPAUtils.closeEMF(emf);
    }

    @Override
    public void propsHaveChangedTrigger() {
    }

    private void trigger() {
        try {
            if (log.isDebugEnabled()) {
                log.debug("CustomerLifecycleDaemon triggered by thread {} on class {}", new Object[]{Thread.currentThread().getId(), this.toString()});
            }

            if (BaseUtils.getBooleanProperty("env.cm.cl.daemon.enabled", true)) {
                //  5|day
                // 10|day
                // 90|minute
                //120|minute
                Set<String> periods = BaseUtils.getPropertyAsSet("env.cm.cl.daemon.period.onnetwork.runs");

                for (String ped : periods) {
                    String unit = ped.split("\\|")[0];
                    String unitType = ped.split("\\|")[1];
                    periodOnNetworkLC(Integer.parseInt(unit), unitType);
                }

                daysAfterSmileVoiceFirstUseLC(3);

                afterSecondVoiceCallLC();

                if (BaseUtils.getBooleanProperty("env.cm.cl.no.networkactivity.notification.enabled", false)) {
                    Set<String> runs = BaseUtils.getPropertyAsSet("env.cm.cl.daemon.nonetwork.activity.runs");
                    for (String ped : runs) {
                        doXXXDaysAndNoNetworkActivityLC(Integer.parseInt(ped));
                    }
                }
            }

            if (BaseUtils.getBooleanProperty("env.customer.lifecycle.sms.enabled", false)) {
                newCustomerOnNetwork(24);
                newCustomerOnNetwork(420);
                newCustomerActivatedSmileVoice(24);
            }
            if (BaseUtils.getBooleanProperty("env.customer.lifecycle.visa.expire.enabled.true", false)) {
                Set<String> periods = BaseUtils.getPropertyAsSet("env.customer.lifecycle.visa.expire.set");
                for (String ped : periods) {
                    String days = ped.split("\\|")[0];
                    String subtype = ped.split("\\|")[1];
                    customerPreVisaExpitryAlert(Integer.parseInt(days), subtype);
                }
            }
        } catch (Exception e) {
            log.warn("Error in a runner: ", e);
        }
    }

    private void triggerKYC() {
        try {

            if (log.isDebugEnabled()) {
                log.warn("CustomerLifecycleDaemon triggered by thread {} on class {} to do {}", new Object[]{Thread.currentThread().getId(), this.toString(), "triggerKYC"});
            }
            if (BaseUtils.getBooleanProperty("env.cm.cl.daemon.enabled", true) && BaseUtils.getBooleanProperty("env.cm.cl.daemon.kyc.enabled", false)) {
                afterKYCGraceCL();
                doPreGraceHourWarning();
            }
        } catch (Exception e) {
            log.warn("Error in runner2: ", e);
        }
    }

    private void afterKYCGraceCL() {
        try {

            if (log.isDebugEnabled()) {
                log.debug("CLifecycleDaemon running afterKYCGraceCL");
            }
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            List<Object[]> productInstanceIdsCreatedlast48Hrs = DAO.getProductInstancesforKYCVerification(em, BaseUtils.getIntProperty("env.cm.kyc.grace.hours", 48));
            log.debug("[{}] PIs whose KYC status is unverified when grace hour reached after subscription", productInstanceIdsCreatedlast48Hrs.size());

            for (Object[] row : productInstanceIdsCreatedlast48Hrs) {
                //SI.PRODUCT_INSTANCE_ID, SI.SERVICE_INSTANCE_ID, CP.CUSTOMER_PROFILE_ID, SI.ACCOUNT_ID, SI.CREATED_DATETIME, CP.KYC_STATUS, SI.STATUS
                int piId = (Integer) row[0];
                int siId = (Integer) row[1];
                int cpId = (Integer) row[2];
                long accId = (Long) row[3];
                Date siDt = (Date) row[4];
                String kycStatus = (String) row[5];
                String siStatus = (String) row[6];

                // Requirement for Nigeria to  not allow SIMs that are not fully registered. Kyc in Nigeria is done at customer level.
                if (BaseUtils.getProperty("env.kyc.level", "service").equalsIgnoreCase("customer")) {
                    Calendar startDate = Calendar.getInstance();
                    String dt = BaseUtils.getProperty("env.cm.kyc.enforced.date", "20500101");
                    // Note - on the month below field, we minus 1 for Java compliance
                    startDate.set(Integer.parseInt(dt.substring(0, 4)), Integer.parseInt(dt.substring(4, 6)) - 1, Integer.parseInt(dt.substring(6, 8)), 0, 0, 0);

                    Calendar cutOffDate = Calendar.getInstance();
                    cutOffDate.setTime(siDt);
                    cutOffDate.add(Calendar.HOUR, BaseUtils.getIntProperty("env.cm.kyc.grace.hours", 48)); // By default allow 7 days and then block afterwards.

                    StringBuilder sb = new StringBuilder();
                    sb.append("PIId=").append(piId).append("\r\n");
                    sb.append("CustId=").append(cpId).append("\r\n");
                    sb.append("AccId=").append(accId);

                    if (!Utils.isInTheFuture(cutOffDate.getTime()) && siDt.after(startDate.getTime())) {
                        log.debug("Customer KYCStatus is [{}] and created date is [{}] so the service status is being set to TD.", kycStatus, siDt);
                        if (!siStatus.equals(CatalogManager.SERVICE_INSTANCE_STATUS.TD.toString())) {
                            DAO.tempDeactivateAllSIsInProductInstance(em, piId);

                            String subType = "TMP_DEACTIVATE";
                            PlatformEventManager.createEvent("CL_KYC", subType,
                                    String.valueOf(piId),
                                    sb.toString(),
                                    "CL_KYC" + subType + "_" + piId);
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.warn("Error in KYCLifecycleDaemon. Will ignore : [{}]", e.toString());
            log.warn("Error: ", e);
        } finally {
            JPAUtils.commitTransaction(em);
            try {
                JPAUtils.closeEM(em);
            } catch (Exception ex) {
                log.warn("Failed to commit transaction in KYCLifecycleDaemon: " + ex.toString());
            }
        }
    }

    private void doPreGraceHourWarning() {
        try {

            if (log.isDebugEnabled()) {
                log.debug("KYCLifecycleDaemon running doPreGraceHourWarning");
            }
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            List<Object[]> productInstanceIdsForKYC = DAO.getProductInstancesforKYCVerification(em, BaseUtils.getIntProperty("env.cm.kyc.grace.hours.warning", 24));
            log.debug("[{}] PIs whose KYC status is unverified when pre-grace hour warning reached after subscription", productInstanceIdsForKYC.size());

            for (Object[] row : productInstanceIdsForKYC) {
                //SI.PRODUCT_INSTANCE_ID, SI.SERVICE_INSTANCE_ID, CP.CUSTOMER_PROFILE_ID, SI.ACCOUNT_ID, SI.CREATED_DATETIME, CP.KYC_STATUS, SI.STATUS
                int piId = (Integer) row[0];
                int siId = (Integer) row[1];
                int cpId = (Integer) row[2];
                long accId = (Long) row[3];
                Date siDt = (Date) row[4];
                String kycStatus = (String) row[5];
                String siStatus = (String) row[6];

                // Requirement for Nigeria to  not allow SIMs that are not fully registered. Kyc in Nigeria is done at customer level.
                if (BaseUtils.getProperty("env.kyc.level", "service").equalsIgnoreCase("customer")) {
                    Calendar startDate = Calendar.getInstance();
                    String dt = BaseUtils.getProperty("env.cm.kyc.enforced.date", "20500101");
                    // Note - on the month below field, we minus 1 for Java compliance
                    startDate.set(Integer.parseInt(dt.substring(0, 4)), Integer.parseInt(dt.substring(4, 6)) - 1, Integer.parseInt(dt.substring(6, 8)), 0, 0, 0);

                    Calendar WarniDate = Calendar.getInstance();
                    WarniDate.setTime(siDt);
                    WarniDate.add(Calendar.HOUR, BaseUtils.getIntProperty("env.cm.kyc.grace.hours.warning", 24)); // 24Hrs after customer KYC status still U/P.
                    StringBuilder sb = new StringBuilder();
                    sb.append("PIId=").append(piId).append("\r\n");
                    sb.append("CustId=").append(cpId).append("\r\n");
                    sb.append("AccId=").append(accId);

                    if (!Utils.isInTheFuture(WarniDate.getTime()) && siDt.after(startDate.getTime())) {
                        log.debug("Customer KYCStatus is [{}] and created date is [{}], sending a warning if SI status is not TD.", kycStatus, siDt);
                        if (!siStatus.equals(CatalogManager.SERVICE_INSTANCE_STATUS.TD.toString())) {
                            String subType = "TMP_DEACTIVATE_WARNING";
                            PlatformEventManager.createEvent("CL_KYC", subType,
                                    String.valueOf(piId),
                                    sb.toString(),
                                    "CL_KYC" + subType + "_" + piId);
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.warn("Error in KYCLifecycleDaemon. Will ignore : [{}]", e.toString());
            log.warn("Error: ", e);
        } finally {
            JPAUtils.commitTransaction(em);
            try {
                JPAUtils.closeEM(em);
            } catch (Exception ex) {
                log.warn("Failed to commit transaction in KYCLifecycleDaemon: " + ex.toString());
            }
        }
    }

    private void afterSecondVoiceCallLC() {
        try {

            if (log.isDebugEnabled()) {
                log.debug("CustomerLifecycleDaemon running afterSecondVoiceCallLC");
            }

            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);

            List<Integer> productInstanceIdsCalledInLastTenMinutes = DAO.getProductInstancesOutgoingVoiceCallTimePeriod(em, 10);
            log.debug("Have list of PIs who made an outgoing call in the last ten minutes");
            for (Integer productInstanceId : productInstanceIdsCalledInLastTenMinutes) {
                //now need to check if this is the 2nd call

                if (DAO.hasProductInstanceMadeTwoCalls(em, productInstanceId)) {
                    String subType = "SMILEVOICE_2ND_CALL";
                    log.debug("This PI [{}] has made 2 voice calls, logging CL subtype: [{}]", productInstanceId, subType);
                    PlatformEventManager.createEvent("CL_VOICE", subType,
                            productInstanceId.toString(),
                            "PIId=" + productInstanceId.toString(),
                            "CL_VOICE_" + subType + "_" + productInstanceId.toString());

                }
            }
        } catch (Exception e) {
            log.warn("Error in CustomerLifecycleDaemon. Will ignore : [{}]", e.toString());
            log.warn("Error: ", e);
        } finally {
            JPAUtils.commitTransaction(em);
            try {
                JPAUtils.closeEM(em);
            } catch (Exception ex) {
                log.warn("Failed to commit transaction in CustomerLifecycleDaemon: " + ex.toString());
            }
        }
    }

    private void daysAfterSmileVoiceFirstUseLC(int days) {
        try {

            if (log.isDebugEnabled()) {
                log.debug("CustomerLifecycleDaemon running daysAfterSmileVoiceFirstUseLC for [{}] days", days);
            }

            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);

            List<Integer> productInstanceIdsDaysAfterSmileVoiceFirstUse = DAO.getProductInstancesTimePeriodAfterFirstSmileVoiceActivity(em, days);
            log.debug("Have list of PIs who first used SmileVoice [{}] days ago", days);
            for (Integer productInstanceId : productInstanceIdsDaysAfterSmileVoiceFirstUse) {
                String subType = days + "_DAYS_AFTER_SMILEVOICE_ACTIVATED";
                log.debug("This PI [{}] had first Smile activity [{}] days ago", productInstanceId, days);
                log.debug("Logging subtype [{}]", subType);

                PlatformEventManager.createEvent("CL_VOICE", subType,
                        productInstanceId.toString(),
                        "PIId=" + productInstanceId.toString(),
                        "CL_VOICE_" + subType + "_" + productInstanceId.toString());
            }
        } catch (Exception e) {
            log.warn("Error in CustomerLifecycleDaemon. Will ignore : [{}]", e.toString());
            log.warn("Error: ", e);
        } finally {
            JPAUtils.commitTransaction(em);
            try {
                JPAUtils.closeEM(em);
            } catch (Exception ex) {
                log.warn("Failed to commit transaction in CustomerLifecycleDaemon: " + ex.toString());
            }
        }
    }

    private void periodOnNetworkLC(int period, String periodType) {
        try {

            if (log.isDebugEnabled()) {
                log.debug("CustomerLifecycleDaemon running daysOnNetworkLC for [{}] [{}]", period, periodType);
            }

            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);

            List<Integer> productInstanceIdsOnNetworkForTimePeriod = DAO.getProductInstancesOnNetworkForTimePeriod(em, period, periodType);
            log.debug("Have list of PIs who have been on network for [{}] [{}]  - checking which have no SmileVoice activity", period, periodType);
            for (Integer productInstanceId : productInstanceIdsOnNetworkForTimePeriod) {

                if (periodType.equalsIgnoreCase("day")) {
                    if (DAO.doesProductInstanceHaveNoSmileVoiceActivity(em, productInstanceId)) {
                        log.debug("This PI [{}] has no SmileVoice activity.  Need to check if customer has unlimited bundle", productInstanceId);

                        //if (DAO.doesProductInstanceHaveUnlimitedBundle(em, productInstanceId)) {
                        if (DAO.doesProductInstanceHaveUnlimitedPremiumBundle(em, productInstanceId)) {
                            String subType = period + "_DAYS_ON_NETWORK_NO_SMILEVOICE_UL";
                            log.debug("This PI [{}] has unlimited bundle, logging CL subtype: [{}]", productInstanceId, subType);
                            PlatformEventManager.createEvent("CL_VOICE", subType,
                                    productInstanceId.toString(),
                                    "PIId=" + productInstanceId.toString(),
                                    "CL_VOICE_" + subType + "_" + productInstanceId.toString());

                        } else {
                            String subType = period + "_DAYS_ON_NETWORK_NO_SMILEVOICE";
                            log.debug("This PI [{}] has no unlimited bundle, logging CL subtype: [{}]", productInstanceId, subType);
                            PlatformEventManager.createEvent("CL_VOICE", subType,
                                    productInstanceId.toString(),
                                    "PIId=" + productInstanceId.toString(),
                                    "CL_VOICE_" + subType + "_" + productInstanceId.toString());
                        }

                    }
                } else if (periodType.equalsIgnoreCase("minute")) {
                    String subType = period + "_MINUTES_ON_NETWORK";
                    log.debug("Logging CL subtype for this PI [{}]", subType, productInstanceId);
                    PlatformEventManager.createEvent("CL_CUST", subType,
                            productInstanceId.toString(),
                            "PIId=" + productInstanceId.toString(),
                            "CL_CUST_" + subType + "_" + productInstanceId.toString());
                }
            }
        } catch (Exception e) {
            log.warn("Error in CustomerLifecycleDaemon. Will ignore : [{}]", e.toString());
            log.warn("Error: ", e);
        } finally {
            JPAUtils.commitTransaction(em);
            try {
                JPAUtils.closeEM(em);
            } catch (Exception ex) {
                log.warn("Failed to commit transaction in CustomerLifecycleDaemon: " + ex.toString());
            }
        }
    }

    private void doXXXDaysAndNoNetworkActivityLC(int days) {
        try {

            if (log.isDebugEnabled()) {
                log.debug("CustomerLifecycleDaemon running do90DaysAndNoNetworkActivityLC");
            }

            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            String subTypeStartBit = sdfCLStart.format(new Date());

            List<Integer> productInstanceIdsWithNoNetworkActivity = DAO.getProductInstancesWithNoNetworkActivityForTimePeriod(em, days * 24);
            Collections.shuffle(productInstanceIdsWithNoNetworkActivity); // help prevent clashes
            for (Integer productInstanceId : productInstanceIdsWithNoNetworkActivity) {
                String subType = days + "_DAYS_NO_NETWORK_ACTIVITY";
                log.debug("Logging CL subtype [{}] for this PI [{}]", subType, productInstanceId);
                PlatformEventManager.createEvent("CL_CUST", subType,
                        productInstanceId.toString(),
                        "PIId=" + productInstanceId.toString(),
                        "CL_CUST_" + subTypeStartBit + "_" + subType + "_" + productInstanceId.toString());
            }
        } catch (Exception e) {
            log.warn("Error in CustomerLifecycleDaemon. Will ignore : [{}]", e.toString());
            log.warn("Error: ", e);
        } finally {
            JPAUtils.commitTransaction(em);
            try {
                JPAUtils.closeEM(em);
            } catch (Exception ex) {
                log.warn("Failed to commit transaction in CustomerLifecycleDaemon: " + ex.toString());
            }
        }
    }

    private void newCustomerOnNetwork(int hours) {
        try {

            if (log.isDebugEnabled()) {
                log.debug("KYCLifecycleDaemon running new customer on network");
            }
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            List<Object[]> productInstanceIdsForNewCustomer = DAO.getProductInstancesforNewCustomer(em, hours);
            log.debug("[{}] PIs whose KYC status is verified customer created newly before hour", productInstanceIdsForNewCustomer.size());

            for (Object[] row : productInstanceIdsForNewCustomer) {

                int days = hours / 24;
                int piId = (Integer) row[0];
                int cpId = (Integer) row[1];
                String kycStatus = (String) row[2];
                String piStatus = (String) row[3];

                StringBuilder sb = new StringBuilder();
                sb.append("PIId=").append(piId).append("\r\n");
                sb.append("CustId=").append(cpId).append("\r\n");

                log.debug("New Customer KYCStatus is [{}] , sending a message to activate smilevoice.", kycStatus);
                if (days == 1) {
                    if (piStatus.equals(CatalogManager.PRODUCT_INSTANCE_STATUS.AC.toString())) {
                        String subType = "NEW_CUSTOMER_ON_NETWORK";
                        PlatformEventManager.createEvent("CL_VOICE", subType,
                                String.valueOf(piId),
                                sb.toString(),
                                "CL_VOICE_" + subType + "_" + piId + "_" + cpId);
                    }
                } else if (days == 7) {
                    if (piStatus.equals(CatalogManager.PRODUCT_INSTANCE_STATUS.AC.toString())) {
//                        String subType = "days_DAYS_ON_NETWORK_NO_ACTIVITY";
                        String subType = "NEW_CUSTOMER_ON_NETWORK";
                        PlatformEventManager.createEvent("CL_VOICE", subType,
                                String.valueOf(piId),
                                sb.toString(),
                                "CL_VOICE_" + subType + "_" + piId + "_" + cpId);
                    }
                }

            }

        } catch (Exception e) {
            log.warn("Error in KYCLifecycleDaemon. Will ignore : [{}]", e.toString());
            log.warn("Error: ", e);
        } finally {
            JPAUtils.commitTransaction(em);
            try {
                JPAUtils.closeEM(em);
            } catch (Exception ex) {
                log.warn("Failed to commit transaction in KYCLifecycleDaemon: " + ex.toString());
            }
        }
    }

    private void newCustomerActivatedSmileVoice(int hours) {
        try {

            if (log.isDebugEnabled()) {
                log.debug("KYCLifecycleDaemon running new customer on network and activated smile voice");
            }
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            List<Object[]> piIdsActivatedSmileVoice = DAO.getNewCustomerActivatedSmileVoice(em, hours);
            log.debug("[{}] PIs whose KYC status is verified customer created newly before hour", piIdsActivatedSmileVoice.size());

            for (Object[] row : piIdsActivatedSmileVoice) {

                int days = hours / 24;
                int piId = (Integer) row[0];
                int cpId = (Integer) row[1];
                String kycStatus = (String) row[2];
                String piStatus = (String) row[3];

                StringBuilder sb = new StringBuilder();
                sb.append("PIId=").append(piId).append("\r\n");
                sb.append("CustId=").append(cpId).append("\r\n");

                log.debug("New Customer KYCStatus is [{}] varified, sending a message to customer for activated smilevoice.", kycStatus);
                if (days == 1) {
                    if (piStatus.equals(CatalogManager.PRODUCT_INSTANCE_STATUS.AC.toString())) {
                        String subType = "NEW_SMILE_VOICE_ACTIVATED";
                        PlatformEventManager.createEvent("CL_VOICE", subType,
                                String.valueOf(piId),
                                sb.toString(),
                                "CL_VOICE_" + subType + "_" + piId + "_" + cpId);
                    }
                }

            }

        } catch (Exception e) {
            log.warn("Error in KYCLifecycleDaemon. Will ignore : [{}]", e.toString());
            log.warn("Error: ", e);
        } finally {
            JPAUtils.commitTransaction(em);
            try {
                JPAUtils.closeEM(em);
            } catch (Exception ex) {
                log.warn("Failed to commit transaction in KYCLifecycleDaemon: " + ex.toString());
            }
        }
    }

    private void customerPreVisaExpitryAlert(int days, String subType) {
        log.debug("visa expires");
        try {

            if (log.isDebugEnabled()) {
                log.debug("CustemerLifecycleDaemon running for the customer whose visa expires");
            }
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            List<Object[]> customerIds = DAO.getPreVisaExpiryCustomers(em, days);
            log.debug("[{}] Number of Custumer whose KYC status is verified and visa expires ", customerIds.size());

            for (Object[] row : customerIds) {
                int cpId = (Integer) row[0];
                String kycStatus = (String) row[1];
                String piStatus = (String) row[2];
                String expiryDate = (String) row[3];
                log.debug("Customer KYCStatus is [{}] varified, sending a message on visa expiry.", kycStatus);

                if (kycStatus.equalsIgnoreCase("V")) {
                    PlatformEventManager.createEvent("CL_UC", subType,
                            String.valueOf(cpId),
                            "CustId=" + cpId,
                            "CL_UC_" + subType + "_" + cpId);
                }
            }

        } catch (Exception e) {
            log.warn("Error in CustomerLifecycleDaemon for visa expiry check. Will ignore : [{}]", e.toString());
            log.warn("Error: ", e);
        } finally {
            JPAUtils.commitTransaction(em);
            try {
                JPAUtils.closeEM(em);
            } catch (Exception ex) {
                log.warn("Failed to commit transaction in CustomerLifecycleDaemon for visa expiry check: " + ex.toString());
            }
        }
    }

}
