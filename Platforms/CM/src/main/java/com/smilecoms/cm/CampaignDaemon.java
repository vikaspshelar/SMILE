/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.cm;

import com.smilecoms.cm.db.model.Campaign;
import com.smilecoms.cm.db.model.CampaignRun;
import com.smilecoms.cm.db.op.DAO;
import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.PurchaseUnitCreditRequest;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ShortMessage;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.beans.CampaignBean;
import com.smilecoms.commons.sca.beans.NonUserSpecificCachedDataHelper;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Javassist;
import com.smilecoms.commons.util.Utils;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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
 * @author lesiba
 */
@Singleton
@Startup
@Local({BaseListener.class})
public class CampaignDaemon implements BaseListener {

    private static final Logger log = LoggerFactory.getLogger(CampaignDaemon.class);
    private static final String CHECK_FOR_ACTIVE_CAMPAIGNS = "cfac";
    private static final String CHECK_FOR_CAMPAIGN_TRIGGER = "cfct";
    private static final String CHECK_FOR_CAMPAIGN_ENTICEMENTS = "cfen";
    private EntityManagerFactory emf = null;
    private ClassLoader classLoader;
    private static boolean canRun = true;
    private static ScheduledFuture runner1 = null;
    private static ScheduledFuture runner2 = null;
    private static ScheduledFuture runner3 = null;

    @Override
    public void propsAreReadyTrigger() {
        runner1 = Async.scheduleAtFixedRate(new SmileBaseRunnable("CM.CheckForCampaignEnrolments") {
            @Override
            public void run() {
                trigger(CHECK_FOR_ACTIVE_CAMPAIGNS);
            }
        }, Utils.getRandomNumber(2 * 60000, 5 * 60000), 5 * 60000, 10 * 60000);
        //}, 30000, 30000);

        runner2 = Async.scheduleAtFixedRate(new SmileBaseRunnable("CM.CheckForCampaignTriggers") {
            @Override
            public void run() {
                trigger(CHECK_FOR_CAMPAIGN_TRIGGER);
            }
        }, 50000, 2 * 60000, 4 * 60000);

        runner3 = Async.scheduleAtFixedRate(new SmileBaseRunnable("CM.CheckForCampaignEnticements") {
            @Override
            public void run() {
                trigger(CHECK_FOR_CAMPAIGN_ENTICEMENTS);
            }
        }, 60000, 2 * 60000, 4 * 60000);
        BaseUtils.registerForPropsChanges(this);
    }

    @PostConstruct
    public void startUp() {
        classLoader = this.getClass().getClassLoader();
        BaseUtils.registerForPropsAvailability(this);
        emf = JPAUtils.getEMF("CMPU_RL");
    }

    @PreDestroy
    public void shutDown() {
        canRun = false;
        BaseUtils.deregisterForPropsAvailability(this);
        Async.cancel(runner1);
        Async.cancel(runner2);
        Async.cancel(runner3);
        BaseUtils.deregisterForPropsChanges(this);
        JPAUtils.closeEMF(emf);
    }

    @Override
    public void propsHaveChangedTrigger() {
    }

    private void trigger(String callBackParam) {
        log.debug("Campaign triggered with callback param [{}]", callBackParam);
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            if (callBackParam.equalsIgnoreCase(CHECK_FOR_ACTIVE_CAMPAIGNS)) {
                log.debug("Checking for active campaigns");
                checkAndCreateCampaignRunsForActiveCampaigns();
                log.debug("Checking for campaign run removals");
                checkAndRemoveCampaignRunsForActiveCampaigns();
            } else if (callBackParam.equalsIgnoreCase(CHECK_FOR_CAMPAIGN_TRIGGER)) {
                log.debug("Checking for trigger...");
                int runsChecked;
                int limit = 0;
                do {
                    limit++;
                    runsChecked = checkIfParticipantsStatisfyCampaignTrigger();
                    log.debug("The campaign runs checked [{}] product instances in loop [{}]", runsChecked, limit);
                } while (runsChecked > 10 && limit < 100 && canRun);
            } else if (callBackParam.equalsIgnoreCase(CHECK_FOR_CAMPAIGN_ENTICEMENTS)) {
                log.debug("Checking for enticements...");
                int enticementsChecked;
                int limit = 0;
                do {
                    limit++;
                    enticementsChecked = checkIfEnticementTriggersMustBeSent();
                    log.debug("The campaign enticements checked [{}] product instances in loop [{}]", enticementsChecked, limit);
                } while (enticementsChecked > 10 && limit < 100 && canRun);
            }
        } catch (Exception e) {
            log.warn("[{}]", e.getMessage());
        }
    }

    private void checkAndCreateCampaignRunsForActiveCampaigns() {
        log.debug("Checking for Active Campaigns.");
        EntityManager em = null;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            List<Campaign> activeCampaigns = DAO.getActiveCampaigns(em);//getActiveCampaignIds            
            Collections.shuffle(activeCampaigns);
            JPAUtils.commitTransactionAndClear(em);

            for (Campaign campaign : activeCampaigns) {
                try {
                    log.debug("Checking campaign [{}]", campaign.getCampaignPK().getName());
                    JPAUtils.beginTransaction(em);
                    if (campaign.getParticipantQuery().isEmpty()) {
                        log.debug("This campaign has no participant query. Its probably trigger query based");
                        continue;
                    }
                    if (Utils.getBooleanValueFromCRDelimitedAVPString(campaign.getActionConfig(), "GetParticipantsOnce")
                            && DAO.getCampaignParticipantsCount(em, campaign) > 0) {
                        log.debug("This campaign has already got its list of participants and GetParticipantsOnce=true");
                        continue;
                    }
                    List<Object[]> enrolDataList = DAO.getCampaignEnrolData(em, campaign);
                    JPAUtils.commitTransactionAndClear(em);
                    Collections.shuffle(enrolDataList);
                    String action = Utils.getValueFromCRDelimitedAVPString(campaign.getActionConfig(), "Enrolment");
                    String enrolmentCode = null;
                    if (action != null) {
                        enrolmentCode = BaseUtils.getProperty(action);
                    }
                    for (Object[] enrolData : enrolDataList) {
                        EnrolResult enrolResult = makeEnrolResult(enrolData);
                        try {
                            if (enrolResult == null) {
                                continue;
                            }
                            long start = System.currentTimeMillis();
                            JPAUtils.beginTransaction(em);
                            int inserts = DAO.createCampaignRunEntry(em, campaign.getCampaignPK().getCampaignId(), enrolResult.productInstanceId,
                                    enrolResult.runType);
                            JPAUtils.commitTransactionAndClear(em);
                            if (inserts == 1 && enrolmentCode != null) {
                                log.debug("Running Java Assist Enrolment Code");
                                com.smilecoms.cm.db.model.ProductInstance pi = DAO.getProductInstanceById(em, enrolResult.productInstanceId);
                                java.util.List<com.smilecoms.cm.db.model.ServiceInstance> siList = DAO.getServiceInstancesByProductInstanceId(em, enrolResult.productInstanceId);
                                Customer customer = SCAWrapper.getAdminInstance().getCustomer(pi.getCustomerProfileId(), StCustomerLookupVerbosity.CUSTOMER);
                                try {
                                    SCAWrapper.setThreadsRequestContextAsAdmin();
                                    String runCode;
                                    if (enrolResult.smsMsg == null && enrolResult.emailFrom == null) {
                                        runCode = (String) Javassist.runCode(new Class[]{this.getClass(), java.util.Calendar.class}, enrolmentCode, campaign, pi, siList, customer, log);
                                        log.debug("Javassist Enrolment Code Ran With Result [{}]", runCode);
                                    } else {
                                        runCode = (String) Javassist.runCode(new Class[]{this.getClass(), java.util.Calendar.class}, enrolmentCode, campaign, siList, customer, enrolResult, log);
                                    }
                                    log.debug("Javassist Enrolment Code Ran With Result [{}]", runCode);
                                } finally {
                                    SCAWrapper.removeThreadsRequestContext();
                                }
                            }
                            long latency = System.currentTimeMillis() - start;
                            BaseUtils.addStatisticSample("CM.createCampaignRun." + campaign.getCampaignPK().getCampaignId(), BaseUtils.STATISTIC_TYPE.latency, latency);
                        } catch (Throwable e) {
                            log.warn("An error occured processing an enrolment in a campaign", Utils.getDeepestCause(e));
                            if (!e.toString().contains("PessimisticLockException") && !e.toString().contains("Lock wait timeout exceeded")) {
                                Throwable t = Utils.getDeepestCause(e);
                                String err = t.getMessage();
                                if (err == null) {
                                    err = t.toString();
                                }
                                err = err + " Stack Trace: " + Utils.getStackTrace(t);
                                String error = "An error occured doing an enrolment for product instance: " + enrolResult.productInstanceId
                                        + " in campaign id: " + campaign.getCampaignPK().getCampaignId() + " : " + err;
                                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "CM", error);
                            }
                        }

                    }
                } catch (Exception e) {
                    log.warn("An error occured getting product instances in a campaign", e);
                    BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "CM", "An error occured getting product instances in a campaign: " + e.toString());
                }
            }
        } catch (Exception e) {
            log.warn("Error processing campaigns", e);
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "CM", "An error occured processing campaigns: " + e.toString());
            JPAUtils.rollbackTransaction(em);
        } finally {
            JPAUtils.closeEM(em);
        }
    }

    private void checkAndRemoveCampaignRunsForActiveCampaigns() {
        log.debug("Checking for Active Campaign runs to remove.");
        EntityManager em = null;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            List<Campaign> activeCampaigns = DAO.getActiveCampaigns(em);//getActiveCampaignIds            
            Collections.shuffle(activeCampaigns);
            JPAUtils.commitTransactionAndClear(em);

            for (Campaign campaign : activeCampaigns) {
                try {
                    log.debug("Checking campaign [{}] for removals", campaign.getCampaignPK().getName());
                    JPAUtils.beginTransaction(em);
                    if (campaign.getRemovalQuery().isEmpty()) {
                        log.debug("This campaign has no removal query.");
                        continue;
                    }
                    List<Object[]> removalsList = DAO.getCampaignRemovals(em, campaign);
                    JPAUtils.commitTransactionAndClear(em);
                    Collections.shuffle(removalsList);
                    String action = Utils.getValueFromCRDelimitedAVPString(campaign.getActionConfig(), "Removal");
                    String removalCode = null;
                    if (action != null) {
                        removalCode = BaseUtils.getProperty(action);
                    }
                    for (Object[] removalData : removalsList) {
                        RemovalResult removalResult = makeRemovalResult(removalData);
                        try {
                            if (removalResult == null) {
                                continue;
                            }
                            long start = System.currentTimeMillis();
                            JPAUtils.beginTransaction(em);
                            int updates = DAO.removeCampaignRunEntry(em, campaign.getCampaignPK().getCampaignId(), removalResult.productInstanceId);
                            JPAUtils.commitTransactionAndClear(em);
                            if (updates == 1 && removalCode != null) {
                                log.debug("Running Java Assist Removal Code");
                                com.smilecoms.cm.db.model.ProductInstance pi = DAO.getProductInstanceById(em, removalResult.productInstanceId);
                                java.util.List<com.smilecoms.cm.db.model.ServiceInstance> siList = DAO.getServiceInstancesByProductInstanceId(em, removalResult.productInstanceId);
                                Customer customer = SCAWrapper.getAdminInstance().getCustomer(pi.getCustomerProfileId(), StCustomerLookupVerbosity.CUSTOMER);
                                try {
                                    SCAWrapper.setThreadsRequestContextAsAdmin();
                                    String runCode = (String) Javassist.runCode(new Class[]{this.getClass(), java.util.Calendar.class}, removalCode, campaign, siList, customer, removalResult, log);
                                    log.debug("Javassist Removal Code Ran With Result [{}]", runCode);
                                } finally {
                                    SCAWrapper.removeThreadsRequestContext();
                                }
                            }
                            long latency = System.currentTimeMillis() - start;
                            BaseUtils.addStatisticSample("CM.removeCampaignRun." + campaign.getCampaignPK().getCampaignId(), BaseUtils.STATISTIC_TYPE.latency, latency);
                        } catch (Throwable e) {
                            log.warn("An error occured processing a removal from a campaign", Utils.getDeepestCause(e));
                            if (!e.toString().contains("PessimisticLockException") && !e.toString().contains("Lock wait timeout exceeded")) {
                                Throwable t = Utils.getDeepestCause(e);
                                String err = t.getMessage();
                                if (err == null) {
                                    err = t.toString();
                                }
                                err = err + " Stack Trace: " + Utils.getStackTrace(t);
                                String error = "An error occured doing a removal for a product instance: " + removalResult.productInstanceId
                                        + " in campaign id: " + campaign.getCampaignPK().getCampaignId() + " : " + err;
                                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "CM", error);
                            }
                        }

                    }
                } catch (Exception e) {
                    log.warn("An error occured removing product instances from a campaign", e);
                    BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "CM", "An error occured removing product instances from a campaign: " + e.toString());
                }
            }
        } catch (Exception e) {
            log.warn("Error processing campaigns", e);
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "CM", "An error occured processing campaign removals: " + e.toString());
            JPAUtils.rollbackTransaction(em);
        } finally {
            JPAUtils.closeEM(em);
        }
    }

    private EnrolResult makeEnrolResult(Object[] sqlResult) {
        EnrolResult enrolResult = new EnrolResult();

        log.debug("Parsing enrol result [{}]", sqlResult);
        int productInstanceId = 0;
        try {
            productInstanceId = (int) sqlResult[0];
        } catch (Exception e) {
            log.warn("Expected a number for product instance id. Class [{}]", sqlResult[0].getClass().getName(), e);
            return null;
        }

        enrolResult.productInstanceId = productInstanceId;
        if (sqlResult.length > 9) {
            enrolResult.emailFrom = (String) sqlResult[1];
            enrolResult.emailTo = (String) sqlResult[2];
            enrolResult.emailTemplate = (String) sqlResult[3];
            enrolResult.smsFrom = (String) sqlResult[4];
            enrolResult.smsTo = (String) sqlResult[5];
            enrolResult.smsMsg = (String) sqlResult[6];
            enrolResult.runType = (String) sqlResult[7];
            enrolResult.emailTextContentPram = (String) sqlResult[8];
            enrolResult.emailSubjectParam = (String) sqlResult[9];
        } else
        if (sqlResult.length > 8) {
            enrolResult.emailFrom = (String) sqlResult[1];
            enrolResult.emailTo = (String) sqlResult[2];
            enrolResult.emailTemplate = (String) sqlResult[3];
            enrolResult.smsFrom = (String) sqlResult[4];
            enrolResult.smsTo = (String) sqlResult[5];
            enrolResult.smsMsg = (String) sqlResult[6];
            enrolResult.runType = (String) sqlResult[7];
            enrolResult.emailTextContentPram = (String) sqlResult[8];
        } else
        if (sqlResult.length > 7) {
            enrolResult.emailFrom = (String) sqlResult[1];
            enrolResult.emailTo = (String) sqlResult[2];
            enrolResult.emailTemplate = (String) sqlResult[3];
            enrolResult.smsFrom = (String) sqlResult[4];
            enrolResult.smsTo = (String) sqlResult[5];
            enrolResult.smsMsg = (String) sqlResult[6];
            enrolResult.runType = (String) sqlResult[7];
        } else if (sqlResult.length > 1) {
            if (((String) sqlResult[2]).contains("@")) {
                enrolResult.emailFrom = (String) sqlResult[1];
                enrolResult.emailTo = (String) sqlResult[2];
                enrolResult.emailTemplate = (String) sqlResult[3];
                enrolResult.runType = (String) sqlResult[4];
            } else {
                enrolResult.smsFrom = (String) sqlResult[1];
                enrolResult.smsTo = (String) sqlResult[2];
                enrolResult.smsMsg = (String) sqlResult[3];
                enrolResult.runType = (String) sqlResult[4];
            }
        }
        log.debug("Created enrolresult [{}]", enrolResult);
        return enrolResult;
    }

    private RemovalResult makeRemovalResult(Object[] sqlResult) {
        RemovalResult removalResult = new RemovalResult();

        log.debug("Parsing removal result [{}]", sqlResult);
        int productInstanceId;
        try {
            productInstanceId = (int) sqlResult[0];
        } catch (Exception e) {
            log.warn("Expected a number for product instance id. Class [{}]", sqlResult[0].getClass().getName(), e);
            return null;
        }

        removalResult.productInstanceId = productInstanceId;
        if (sqlResult.length > 8) {
            removalResult.emailFrom = (String) sqlResult[1];
            removalResult.emailTo = (String) sqlResult[2];
            removalResult.emailTemplate = (String) sqlResult[3];
            removalResult.smsFrom = (String) sqlResult[4];
            removalResult.smsTo = (String) sqlResult[5];
            removalResult.smsMsg = (String) sqlResult[6];
            removalResult.emailTextContentPram = (String) sqlResult[7];
            removalResult.emailSubjectParam = (String) sqlResult[8];
        } else 
        if (sqlResult.length > 7) {
            removalResult.emailFrom = (String) sqlResult[1];
            removalResult.emailTo = (String) sqlResult[2];
            removalResult.emailTemplate = (String) sqlResult[3];
            removalResult.smsFrom = (String) sqlResult[4];
            removalResult.smsTo = (String) sqlResult[5];
            removalResult.smsMsg = (String) sqlResult[6];
            removalResult.emailTextContentPram = (String) sqlResult[7];
        } else 
        if (sqlResult.length > 6) {
            removalResult.emailFrom = (String) sqlResult[1];
            removalResult.emailTo = (String) sqlResult[2];
            removalResult.emailTemplate = (String) sqlResult[3];
            removalResult.smsFrom = (String) sqlResult[4];
            removalResult.smsTo = (String) sqlResult[5];
            removalResult.smsMsg = (String) sqlResult[6];
        } else if (sqlResult.length > 1) {
            if (((String) sqlResult[2]).contains("@")) {
                removalResult.emailFrom = (String) sqlResult[1];
                removalResult.emailTo = (String) sqlResult[2];
                removalResult.emailTemplate = (String) sqlResult[3];
            } else {
                removalResult.smsFrom = (String) sqlResult[1];
                removalResult.smsTo = (String) sqlResult[2];
                removalResult.smsMsg = (String) sqlResult[3];
            }
        }
        log.debug("Created removalResult [{}]", removalResult);
        return removalResult;
    }

    private TriggerResult makeTriggerResult(Object[] sqlResult) {

        TriggerResult triggerResult = new TriggerResult();
        log.debug("Parsing trigger result [{}]", sqlResult);

        int productInstanceId;
        try {
            productInstanceId = (int) sqlResult[0];
        } catch (Exception e) {
            log.warn("Expected a number for product instance id. Class [{}]", sqlResult[0].getClass().getName(), e);
            return null;
        }
        String resString;
        if (sqlResult[1] instanceof String) {
            resString = (String) sqlResult[1];
        } else {
            resString = String.valueOf(sqlResult[1]);
        }
        triggerResult.productInstanceId = productInstanceId;
        triggerResult.triggerResultString = resString;
        
        if (sqlResult.length > 11) {
            triggerResult.emailFrom = (String) sqlResult[2];
            triggerResult.emailTo = (String) sqlResult[3];
            triggerResult.emailTemplate = (String) sqlResult[4];
            triggerResult.smsFrom = (String) sqlResult[5];
            triggerResult.smsTo = (String) sqlResult[6];
            triggerResult.smsMsg = (String) sqlResult[7];
            if (sqlResult[8] instanceof String) {
                triggerResult.ucSpecIdsToProvision = new ArrayList<>();
                for (String specId : Utils.getListFromCommaDelimitedString((String) sqlResult[8])) {
                    triggerResult.ucSpecIdsToProvision.add(Integer.valueOf(specId));
                }
            } else {
                triggerResult.ucSpecIdToProvision = ((Long) sqlResult[8]).intValue();
            }
            triggerResult.enticementKey = (String) sqlResult[9];
            triggerResult.emailTextContentPram = (String) sqlResult[10];
            triggerResult.emailSubjectParam = (String) sqlResult[11];
        } else
        if (sqlResult.length > 10) {
            triggerResult.emailFrom = (String) sqlResult[2];
            triggerResult.emailTo = (String) sqlResult[3];
            triggerResult.emailTemplate = (String) sqlResult[4];
            triggerResult.smsFrom = (String) sqlResult[5];
            triggerResult.smsTo = (String) sqlResult[6];
            triggerResult.smsMsg = (String) sqlResult[7];
            if (sqlResult[8] instanceof String) {
                triggerResult.ucSpecIdsToProvision = new ArrayList<>();
                for (String specId : Utils.getListFromCommaDelimitedString((String) sqlResult[8])) {
                    triggerResult.ucSpecIdsToProvision.add(Integer.valueOf(specId));
                }
            } else {
                triggerResult.ucSpecIdToProvision = ((Long) sqlResult[8]).intValue();
            }
            triggerResult.enticementKey = (String) sqlResult[9];
            triggerResult.emailTextContentPram = (String) sqlResult[10];
        } else
        if (sqlResult.length > 9) {
            triggerResult.emailFrom = (String) sqlResult[2];
            triggerResult.emailTo = (String) sqlResult[3];
            triggerResult.emailTemplate = (String) sqlResult[4];
            triggerResult.smsFrom = (String) sqlResult[5];
            triggerResult.smsTo = (String) sqlResult[6];
            triggerResult.smsMsg = (String) sqlResult[7];
            if (sqlResult[8] instanceof String) {
                triggerResult.ucSpecIdsToProvision = new ArrayList<>();
                for (String specId : Utils.getListFromCommaDelimitedString((String) sqlResult[8])) {
                    triggerResult.ucSpecIdsToProvision.add(Integer.valueOf(specId));
                }
            } else {
                triggerResult.ucSpecIdToProvision = ((Long) sqlResult[8]).intValue();
            }
            triggerResult.enticementKey = (String) sqlResult[9];
        } else {
            if (sqlResult.length > 2) {
                triggerResult.smsFrom = (String) sqlResult[2];
                triggerResult.smsTo = (String) sqlResult[3];
                triggerResult.smsMsg = (String) sqlResult[4];
                if (sqlResult[5] instanceof String) {
                    triggerResult.ucSpecIdsToProvision = new ArrayList<>();
                    for (String specId : Utils.getListFromCommaDelimitedString((String) sqlResult[5])) {
                        triggerResult.ucSpecIdsToProvision.add(Integer.valueOf(specId));
                    }
                } else {
                    triggerResult.ucSpecIdToProvision = ((Long) sqlResult[5]).intValue();
                }
            }
            if (sqlResult.length >= 7) {
                triggerResult.enticementKey = (String) sqlResult[6];
            }
        }
        log.debug("Created triggerResult [{}]", triggerResult);
        return triggerResult;
    }

    private EnticementResult makeEnticementResult(Object[] sqlResult) {

        EnticementResult enticementResult = new EnticementResult();
        log.debug("Parsing enticement result [{}]", sqlResult);

        int productInstanceId = 0;
        try {
            productInstanceId = (int) sqlResult[0];
        } catch (Exception e) {
            log.warn("Expected a number for product instance id. Class [{}]", sqlResult[0].getClass().getName(), e);
            return null;
        }
        String resString;
        if (sqlResult[1] instanceof String) {
            resString = (String) sqlResult[1];
        } else {
            resString = String.valueOf(sqlResult[1]);
        }

        enticementResult.productInstanceId = productInstanceId;
        enticementResult.enticementResultString = resString;

        if (sqlResult.length > 10) {
            enticementResult.emailFrom = (String) sqlResult[2];
            enticementResult.emailTo = (String) sqlResult[3];
            enticementResult.emailTemplate = (String) sqlResult[4];
            enticementResult.smsFrom = (String) sqlResult[5];
            enticementResult.smsTo = (String) sqlResult[6];
            enticementResult.smsMsg = (String) sqlResult[7];
            enticementResult.triggerKey = (String) sqlResult[8];
            enticementResult.emailTextContentPram = (String) sqlResult[9];
            enticementResult.emailSubjectParam = (String) sqlResult[10];
        } else
        if (sqlResult.length > 9) {
            enticementResult.emailFrom = (String) sqlResult[2];
            enticementResult.emailTo = (String) sqlResult[3];
            enticementResult.emailTemplate = (String) sqlResult[4];
            enticementResult.smsFrom = (String) sqlResult[5];
            enticementResult.smsTo = (String) sqlResult[6];
            enticementResult.smsMsg = (String) sqlResult[7];
            enticementResult.triggerKey = (String) sqlResult[8];
            enticementResult.emailTextContentPram = (String) sqlResult[9];
        } else    
        if (sqlResult.length > 8) {
            enticementResult.emailFrom = (String) sqlResult[2];
            enticementResult.emailTo = (String) sqlResult[3];
            enticementResult.emailTemplate = (String) sqlResult[4];
            enticementResult.smsFrom = (String) sqlResult[5];
            enticementResult.smsTo = (String) sqlResult[6];
            enticementResult.smsMsg = (String) sqlResult[7];
            enticementResult.triggerKey = (String) sqlResult[8];
        } else {
            enticementResult.smsFrom = (String) sqlResult[2];
            enticementResult.smsTo = (String) sqlResult[3];
            enticementResult.smsMsg = (String) sqlResult[4];
            enticementResult.triggerKey = (String) sqlResult[5];
        }
        log.debug("Created enticementResult [{}]", enticementResult);
        return enticementResult;
    }

    private void processEnticement(EntityManager em, Campaign campaign, CampaignRun campaignRun, EnticementResult enticementResult) throws Exception {
        log.debug("Processing Enticement on Campaign Id [{}] for PI [{}]", campaignRun.getCampaignId(), campaignRun.getProductInstanceId());

        if (BaseUtils.getBooleanProperty("env.cm.campaign.killswitch", false)) {
            log.warn("Campaign daemon killswitch is on");
            return;
        }

        long start = System.currentTimeMillis();
        CampaignRun lockedCampaignRun = null;
        try {
            JPAUtils.beginTransaction(em);
            log.debug("Getting locked campaign run");
            lockedCampaignRun = DAO.getLockedCampaignRun(em, campaignRun.getCampaignId(), campaignRun.getProductInstanceId());
            log.debug("Got locked campaign run");
            if (lockedCampaignRun == null) {
                log.debug("Product instance is not in this campaign. Ignoring");
                return;
            }
            String status = lockedCampaignRun.getStatus();
            log.debug("Campaign run id [{}] has status [{}]", lockedCampaignRun.getCampaignRunId(), lockedCampaignRun.getStatus());
            if (status.equals(CAMPAIGN_RUN_STATUSES.FI.toString()) || status.equals(CAMPAIGN_RUN_STATUSES.RV.toString())) {
                log.debug("Campaign run has been finished by another thread");
                return;
            }

            if (!DAO.enticementIdExists(em, enticementResult.enticementResultString, lockedCampaignRun.getCampaignRunId())) {
                DAO.createCampaignEnticementId(em, lockedCampaignRun.getCampaignRunId(), enticementResult.enticementResultString, enticementResult.triggerKey);
            } else {
                log.debug("It appears CampaignRunId [{}] with CampaignEnticementId [{}] has already been processed", lockedCampaignRun.getCampaignRunId(), enticementResult);
                return;
            }
            try {
                String runsConfig = lockedCampaignRun.getActionData() + "\r\n" + campaign.getActionConfig();
                String action = Utils.getValueFromCRDelimitedAVPString(runsConfig, "EnticementAction");
                log.debug("Running Java Assist Code for enticement");
                String enticementCode = BaseUtils.getProperty(action);
                com.smilecoms.cm.db.model.ProductInstance pi = DAO.getProductInstanceById(em, lockedCampaignRun.getProductInstanceId());
                java.util.List<com.smilecoms.cm.db.model.ServiceInstance> siList = DAO.getServiceInstancesByProductInstanceId(em, lockedCampaignRun.getProductInstanceId());
                Customer customer = SCAWrapper.getAdminInstance().getCustomer(pi.getCustomerProfileId(), StCustomerLookupVerbosity.CUSTOMER);
                try {
                    SCAWrapper.setThreadsRequestContextAsAdmin();
                    String runCode;
                    runCode = (String) Javassist.runCode(new Class[]{this.getClass(), java.util.Calendar.class},
                            enticementCode, campaign, lockedCampaignRun, siList, customer, enticementResult,
                            log);
                    log.debug("Javassist Code Ran With Result [{}]", runCode);
                } finally {
                    SCAWrapper.removeThreadsRequestContext();
                }
            } catch (javax.persistence.EntityNotFoundException e) {
                log.warn("Product Instance [{}] does not exist. Going to act like it completed", lockedCampaignRun.getProductInstanceId());
            }

        } catch (Exception e) {
            if (!em.isOpen()) {
                log.debug("Cannot continue with a closed EntityManager");
                throw new Exception("EntityManager is closed");
            }
            if (!e.toString().contains("PessimisticLockException") && !e.toString().contains("Lock wait timeout exceeded")) {
                log.warn("Error processing enticement run: ", e);
                Throwable t = Utils.getDeepestCause(e);
                String err = t.getMessage();
                if (err == null) {
                    err = t.toString();
                }
                err = err + " Stack Trace: " + Utils.getStackTrace(t);
                if (lockedCampaignRun == null) {
                    lockedCampaignRun = campaignRun;
                }
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "CM", "Error processing enticement run " + lockedCampaignRun.getCampaignRunId() + " for product instance id " + lockedCampaignRun.getProductInstanceId() + ": " + err);
            } else {
                log.debug("Got a PessimisticLockException");
            }
        } finally {
            JPAUtils.commitTransactionAndClear(em);
        }
        long latency = System.currentTimeMillis() - start;
        BaseUtils.addStatisticSample("CM.processEnticementRun." + campaign.getCampaignPK().getCampaignId(), BaseUtils.STATISTIC_TYPE.latency, latency);
    }

    public static enum CAMPAIGN_RUN_STATUSES {
        /*NEW=NW, FINISHED=FI, RV=REMOVED */

        NW, FI, RV
    };

    private int checkIfParticipantsStatisfyCampaignTrigger() {
        log.debug("In Check if participants satisfy Campaign trigger.");
        EntityManager em = null;
        int runsChecked = 0;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            List<Campaign> activeCampaigns = DAO.getActiveCampaigns(em);
            Collections.shuffle(activeCampaigns);
            JPAUtils.commitTransactionAndClear(em);

            log.debug("Found [{}] Active Campaigns", activeCampaigns.size());
            for (Campaign activeCampaign : activeCampaigns) {
                if (!em.isOpen()) {
                    log.debug("EM is closed");
                    break;
                }
                String activeHourMinuteRegex = Utils.getValueFromCRDelimitedAVPString(activeCampaign.getActionConfig(), "HourMinuteRegex");
                if (activeHourMinuteRegex != null) {
                    String hourMinute = Utils.getDateAsString(new Date(), "HHmm");
                    if (!Utils.matchesWithPatternCache(hourMinute, activeHourMinuteRegex)) {
                        log.debug("This campaign must not check for triggers at the current hour/minute of day [{}]", hourMinute);
                        continue;
                    }
                }
                String triggerDriver = Utils.getValueFromCRDelimitedAVPString(activeCampaign.getActionConfig(), "TriggerDriver");
                if (triggerDriver == null || triggerDriver.equals("CampaignRun")) {
                    runsChecked += processBatchOfCampaignRuns(em, activeCampaign);
                } else if (triggerDriver.equals("TriggerQuery")) {
                    runsChecked += processBatchOfTriggers(em, activeCampaign);
                } else {
                    throw new Exception("Invalid parameter for TriggerDriver -- " + triggerDriver);
                }

            }
        } catch (Exception e) {
            log.warn("[{}]", e.getMessage());
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "CM", "Error processing campaigns: " + e.toString());
            JPAUtils.rollbackTransaction(em);
        } finally {
            JPAUtils.closeEM(em);
        }
        return runsChecked;
    }

    private int checkIfEnticementTriggersMustBeSent() {
        log.debug("In checkIfEnticementTriggersMustBeSent for each campaign");
        EntityManager em = null;
        int runsChecked = 0;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            List<Campaign> activeCampaigns = DAO.getActiveCampaigns(em);
            Collections.shuffle(activeCampaigns);
            JPAUtils.commitTransactionAndClear(em);

            log.debug("Found [{}] Active Campaigns", activeCampaigns.size());
            for (Campaign activeCampaign : activeCampaigns) {
                if (!em.isOpen()) {
                    log.debug("EM is closed");
                    break;
                }
                if (activeCampaign.getEnticementQuery().isEmpty()) {
                    log.debug("This campaign has no enticement query.");
                    continue;
                }
                String activeHourMinuteRegex = Utils.getValueFromCRDelimitedAVPString(activeCampaign.getActionConfig(), "HourMinuteRegex");
                if (activeHourMinuteRegex != null) {
                    String hourMinute = Utils.getDateAsString(new Date(), "HHmm");
                    if (!Utils.matchesWithPatternCache(hourMinute, activeHourMinuteRegex)) {
                        log.debug("This campaign must not check for enticements at the current hour/minute of day [{}]", hourMinute);
                        continue;
                    }
                }
                runsChecked += processCampaignsEnticementTrigger(em, activeCampaign);
            }
        } catch (Exception e) {
            log.warn("[{}]", e.getMessage());
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "CM", "Error processing campaigns: " + e.toString());
            JPAUtils.rollbackTransaction(em);
        } finally {
            JPAUtils.closeEM(em);
        }
        return runsChecked;
    }

    private int processBatchOfCampaignRuns(EntityManager em, Campaign activeCampaign) throws Exception {
        if (activeCampaign.getTriggerQuery().isEmpty()) {
            log.debug("No trigger query so nothing to do");
            return 0;
        }
        int runsChecked = 0;
        try {
            JPAUtils.beginTransaction(em);
            int checkIntervalSecs = Integer.parseInt(Utils.getValueFromCRDelimitedAVPString(activeCampaign.getActionConfig(), "TriggerCheckInterval"));
            List<CampaignRun> campaignRuns = DAO.getCampaignRunsByStatus(em, activeCampaign.getCampaignPK().getCampaignId(), CAMPAIGN_RUN_STATUSES.NW.toString(), checkIntervalSecs);
            JPAUtils.commitTransactionAndClear(em);
            runsChecked = campaignRuns.size();
            Collections.shuffle(campaignRuns);
            log.debug("Found [{}] CampaignRun Entries that need processing one at a time for Campaign [{}]", campaignRuns.size(), activeCampaign.getCampaignPK().getName());
            for (CampaignRun campRun : campaignRuns) {
                long start = 0;
                if (log.isDebugEnabled()) {
                    start = System.currentTimeMillis();
                }
                processCampaignRun(em, activeCampaign, campRun, activeCampaign.getTriggerQuery(), null);
                if (log.isDebugEnabled()) {
                    log.debug("Took [{}]ms to process campaign run [{}]", System.currentTimeMillis() - start, campRun.getCampaignRunId());
                }
            }
        } catch (Exception e) {
            if (!em.isOpen()) {
                log.debug("Cannot continue with a closed EntityManager");
                throw new Exception("EntityManager is closed");
            }
            log.warn("Error: [{}]", e.toString());
            JPAUtils.rollbackTransaction(em);
        }
        return runsChecked;
    }

    private int processBatchOfTriggers(EntityManager em, Campaign activeCampaign) throws Exception {
        int runsChecked = 0;
        try {
            JPAUtils.beginTransaction(em);
            
            List<Object[]> triggerResults = DAO.getTriggerResults(em, activeCampaign.getTriggerQuery());
            int listSize = triggerResults.size();
            JPAUtils.commitTransactionAndClear(em);
            log.debug("Found [{}] product instance ids that need trigger processing for Campaign [{}]", listSize, activeCampaign.getCampaignPK().getName());
            for (Object[] triggerResult : triggerResults) {

                TriggerResult triggerResultObj = makeTriggerResult(triggerResult);

                long start = 0;
                if (log.isDebugEnabled()) {
                    log.debug("[{}] of [{}]. [{}] ", new Object[]{runsChecked + 1, listSize, triggerResultObj});
                    start = System.currentTimeMillis();
                }
                CampaignRun campRun = new CampaignRun();
                campRun.setCampaignId(activeCampaign.getCampaignPK().getCampaignId());
                campRun.setProductInstanceId(triggerResultObj.productInstanceId);

                processCampaignRun(em, activeCampaign, campRun, null, triggerResultObj);
                if (log.isDebugEnabled()) {
                    log.debug("Took [{}]ms to process [{}]", new Object[]{System.currentTimeMillis() - start, triggerResultObj});
                }
                runsChecked++;
            }
        } catch (Exception e) {
            if (!em.isOpen()) {
                log.debug("Cannot continue with a closed EntityManager");
                throw new Exception("EntityManager is closed");
            }
            log.warn("Error in processBatchOfTriggers", e);
            Throwable t = Utils.getDeepestCause(e);
            String err = t.getMessage();
            if (err == null) {
                err = t.toString();
            }
            err = err + " Stack Trace: " + Utils.getStackTrace(t);
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "CM", "Error processing trigger batch on campaign " + activeCampaign.getCampaignPK().getName() + ": " + err);
            JPAUtils.rollbackTransaction(em);
        }
        return runsChecked;
    }

    private int processCampaignsEnticementTrigger(EntityManager em, Campaign activeCampaign) throws Exception {
        int runsChecked = 0;
        try {
            JPAUtils.beginTransaction(em);
            List<Object[]> enticementResults = DAO.getEnticementResults(em, activeCampaign.getEnticementQuery());
            int listSize = enticementResults.size();
            JPAUtils.commitTransactionAndClear(em);
            log.debug("Found [{}] product instance ids that need enticement processing for Campaign [{}]", listSize, activeCampaign.getCampaignPK().getName());
            for (Object[] enticementResult : enticementResults) {

                EnticementResult enticementResultObj = makeEnticementResult(enticementResult);

                long start = 0;
                if (log.isDebugEnabled()) {
                    log.debug("[{}] of [{}]. [{}] ", new Object[]{runsChecked + 1, listSize, enticementResultObj});
                    start = System.currentTimeMillis();
                }
                CampaignRun campRun = new CampaignRun();
                campRun.setCampaignId(activeCampaign.getCampaignPK().getCampaignId());
                campRun.setProductInstanceId(enticementResultObj.productInstanceId);

                processEnticement(em, activeCampaign, campRun, enticementResultObj);
                if (log.isDebugEnabled()) {
                    log.debug("Took [{}]ms to process enticement [{}]", new Object[]{System.currentTimeMillis() - start, enticementResultObj});
                }
                runsChecked++;
            }
        } catch (Exception e) {
            if (!em.isOpen()) {
                log.debug("Cannot continue with a closed EntityManager");
                throw new Exception("EntityManager is closed");
            }
            log.warn("Error in processCampaignsEnticementTrigger", e);
            Throwable t = Utils.getDeepestCause(e);
            String err = t.getMessage();
            if (err == null) {
                err = t.toString();
            }
            err = err + " Stack Trace: " + Utils.getStackTrace(t);
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "CM", "Error processing trigger enticement batch on campaign " + activeCampaign.getCampaignPK().getName() + ": " + err);
            JPAUtils.rollbackTransaction(em);
        }
        return runsChecked;
    }

    private void processCampaignRun(EntityManager em, Campaign campaign, CampaignRun campaignRun, String triggerQuery, TriggerResult triggerResult) throws Exception {
        log.debug("Processing Campaign Id [{}] for PI [{}]", campaignRun.getCampaignId(), campaignRun.getProductInstanceId());

        if (BaseUtils.getBooleanProperty("env.cm.campaign.killswitch", false)) {
            log.warn("Campaign daemon killswitch is on");
            return;
        }

        long start = System.currentTimeMillis();
        CampaignRun lockedCampaignRun = null;
        try {
            JPAUtils.beginTransaction(em);
            log.debug("Getting locked campaign run");
            lockedCampaignRun = DAO.getLockedCampaignRun(em, campaignRun.getCampaignId(), campaignRun.getProductInstanceId());
            log.debug("Got locked campaign run");
            if (lockedCampaignRun == null) {
                if (Utils.getBooleanValueFromCRDelimitedAVPString(campaign.getActionConfig(), "AddRunIfMissing")) {
                    // Complex to avoid deadlock as per  http://stackoverflow.com/questions/42547629/insert-row-if-not-exists-without-deadlock
                    JPAUtils.rollbackTransaction(em);
                    JPAUtils.beginTransaction(em);
                    int added = DAO.createCampaignRunEntry(em, campaignRun.getCampaignId(), campaignRun.getProductInstanceId(),
                            Utils.getValueFromCRDelimitedAVPString(campaign.getActionConfig(), "RunType"));
                    if (added == 0) {
                        log.debug("Another thread is dealing with campaign run. Ignoring");
                        return;
                    }
                    lockedCampaignRun = DAO.getLockedCampaignRun(em, campaignRun.getCampaignId(), campaignRun.getProductInstanceId());
                } else {
                    log.debug("Product instance is not in this campaign. Ignoring");
                    return;
                }
            }
            String status = lockedCampaignRun.getStatus();
            log.debug("Campaign run id [{}] has status [{}]", lockedCampaignRun.getCampaignRunId(), lockedCampaignRun.getStatus());
            if (status.equals(CAMPAIGN_RUN_STATUSES.FI.toString()) || status.equals(CAMPAIGN_RUN_STATUSES.RV.toString())) {
                log.debug("Campaign run has been finished by another thread");
                return;
            }

            if (triggerResult == null) {
                int checkIntervalSecs = Integer.parseInt(Utils.getValueFromCRDelimitedAVPString(campaign.getActionConfig(), "TriggerCheckInterval"));
                if (Utils.isDateInTimeframe(lockedCampaignRun.getLastCheckDateTime(), checkIntervalSecs, Calendar.SECOND)) {
                    log.debug("Another thread has processed this run recently. Will ignore");
                    return;
                }
                triggerResult = new TriggerResult();
                triggerResult.productInstanceId = lockedCampaignRun.getProductInstanceId();
                triggerResult.triggerResultString = DAO.doesCampaignParticipantSatisfyTrigger(em, triggerQuery, lockedCampaignRun.getProductInstanceId());
            } else {
                log.debug("Have trigger result already [{}]", triggerResult.triggerResultString);
            }

            if (!triggerResult.triggerResultString.isEmpty()) {
                lockedCampaignRun.setActionData(triggerResult.triggerResultString);
                log.debug("CampaignRun Id [{}] satisfied", lockedCampaignRun.getCampaignRunId());
                String runsConfig = lockedCampaignRun.getActionData() + "\r\n" + campaign.getActionConfig();
                String action = Utils.getValueFromCRDelimitedAVPString(runsConfig, "Action");
                boolean process = true;
                if (Utils.getBooleanValueFromCRDelimitedAVPString(runsConfig, "Repeat")) {
                    if (!DAO.triggerIdExists(em, triggerResult.triggerResultString, lockedCampaignRun.getCampaignRunId())) {
                        DAO.createCampaignTriggerId(em, lockedCampaignRun.getCampaignRunId(), triggerResult.triggerResultString, triggerResult.enticementKey);
                    } else {
                        log.debug("It appears CampaignRunId [{}] with CampaignTriggerId [{}] has already been processed", lockedCampaignRun.getCampaignRunId(), triggerResult);
                        process = false;
                    }
                    status = CAMPAIGN_RUN_STATUSES.NW.toString();
                } else {
                    status = CAMPAIGN_RUN_STATUSES.FI.toString();
                }
                if (process) {
                    try {
                        log.debug("Running Java Assist Code");
                        String campaignCode = BaseUtils.getProperty(action);
                        com.smilecoms.cm.db.model.ProductInstance pi = DAO.getProductInstanceById(em, lockedCampaignRun.getProductInstanceId());
                        java.util.List<com.smilecoms.cm.db.model.ServiceInstance> siList = DAO.getServiceInstancesByProductInstanceId(em, lockedCampaignRun.getProductInstanceId());
                        Customer customer = SCAWrapper.getAdminInstance().getCustomer(pi.getCustomerProfileId(), StCustomerLookupVerbosity.CUSTOMER);
                        try {
                            SCAWrapper.setThreadsRequestContextAsAdmin();
                            String runCode;
                            if (triggerResult.smsMsg == null) {
                                runCode = (String) Javassist.runCode(new Class[]{this.getClass(), java.util.Calendar.class}, campaignCode, lockedCampaignRun, pi, siList, customer, triggerResult.triggerResultString, log);
                            } else {
                                runCode = (String) Javassist.runCode(new Class[]{this.getClass(), java.util.Calendar.class},
                                        campaignCode, campaign, lockedCampaignRun, siList, customer, triggerResult,
                                        log);
                            }
                            log.debug("Javassist Code Ran With Result [{}]", runCode);
                        } finally {
                            SCAWrapper.removeThreadsRequestContext();
                        }
                    } catch (javax.persistence.EntityNotFoundException e) {
                        log.warn("Product Instance [{}] does not exist. Going to act like it completed", lockedCampaignRun.getProductInstanceId());
                    }
                }
            } else {
                log.debug("CampaignRun Id [{}] doesn't satisfy campaign trigger", lockedCampaignRun.getCampaignRunId());
            }

            lockedCampaignRun.setStatus(status);
            lockedCampaignRun.setLastCheckDateTime(new Date());
            em.persist(lockedCampaignRun);
            log.debug("Modified locked campaign run status to [{}]", status);
        } catch (Exception e) {
            if (!em.isOpen()) {
                log.debug("Cannot continue with a closed EntityManager");
                throw new Exception("EntityManager is closed");
            }
            if (!e.toString().contains("PessimisticLockException") && !e.toString().contains("Lock wait timeout exceeded")) {
                log.warn("Error processing campaign run: ", e);
                Throwable t = Utils.getDeepestCause(e);
                String err = t.getMessage();
                if (err == null) {
                    err = t.toString();
                }
                err = err + " Stack Trace: " + Utils.getStackTrace(t);
                if (lockedCampaignRun == null) {
                    lockedCampaignRun = campaignRun;
                }
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "CM", "Error processing campaign run " + lockedCampaignRun.getCampaignRunId() + " for product instance id " + lockedCampaignRun.getProductInstanceId() + ": " + err);
            } else {
                log.debug("Got a PessimisticLockException");
            }
        } finally {
            JPAUtils.commitTransactionAndClear(em);
        }
        long latency = System.currentTimeMillis() - start;
        BaseUtils.addStatisticSample("CM.processCampaignRun." + campaign.getCampaignPK().getCampaignId(), BaseUtils.STATISTIC_TYPE.latency, latency);
    }

    public String runCampaignActionCode(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList) throws Exception {

        java.util.List roles = new java.util.ArrayList();
        roles.add("Administrator");
        com.smilecoms.commons.sca.CallersRequestContext crc = new com.smilecoms.commons.sca.CallersRequestContext("admin", "0.0.0.0", 1, roles);
        com.smilecoms.commons.sca.SCAWrapper.getUserSpecificInstance().setThreadsRequestContext(crc);
        String phoneNumber = com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getProductInstancePhoneNumber(pi.getProductInstanceId());
        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        com.smilecoms.commons.sca.PurchaseUnitCreditRequest pucr = new com.smilecoms.commons.sca.PurchaseUnitCreditRequest();
        pucr.setProductInstanceId(pi.getProductInstanceId());
        pucr.setAccountId(si.getAccountId());
        pucr.setUnitCreditSpecificationId(230);
        pucr.setNumberToPurchase(1);
        pucr.setUniqueId("CAMPAIGN_" + lockedCampaignRun.getCampaignId() + "_" + pi.getProductInstanceId());
        com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().purchaseUnitCredit(pucr);

        com.smilecoms.commons.sca.ShortMessage sm = new com.smilecoms.commons.sca.ShortMessage();
        sm.setFrom("1111");
        sm.setTo(phoneNumber);
        sm.setBody("Congratulations! You are ready to go with SmileVoice. We have given you 30 free minutes and 30 free SMS's to get going. Smile. Now you can!");
        com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().sendShortMessage(sm);
        return "Done";
    }

    public String tzVoiceActivation(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, com.smilecoms.commons.sca.Customer cust, String trigger, Logger log) throws Exception {
        java.util.List roles = new java.util.ArrayList();
        roles.add("Administrator");
        com.smilecoms.commons.sca.CallersRequestContext crc = new com.smilecoms.commons.sca.CallersRequestContext("admin", "0.0.0.0", 1, roles);
        com.smilecoms.commons.sca.SCAWrapper.getUserSpecificInstance().setThreadsRequestContext(crc);
        String phoneNumber = com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getProductInstancePhoneNumber(pi.getProductInstanceId().intValue());
        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        // Get the accounts unit credits
        com.smilecoms.commons.sca.Account acc = com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getAccount(si.getAccountId(), com.smilecoms.commons.sca.StAccountLookupVerbosity.ACCOUNT_UNITCREDITS);
        int cnt = acc.getUnitCreditInstances().size();
        boolean haslite = false;
        boolean hasnorm = false;
        for (int i = 0; i < cnt; i++) {
            com.smilecoms.commons.sca.UnitCreditInstance uci = (com.smilecoms.commons.sca.UnitCreditInstance) acc.getUnitCreditInstances().get(i);
            if ((uci.getCurrentUnitsRemaining().longValue() <= 0l && (uci.getUnitCreditSpecificationId().intValue() != 235
                    && uci.getUnitCreditSpecificationId().intValue() != 263
                    && uci.getUnitCreditSpecificationId().intValue() != 264
                    && uci.getUnitCreditSpecificationId().intValue() != 265
                    && uci.getUnitCreditSpecificationId().intValue() != 266
                    && uci.getUnitCreditSpecificationId().intValue() != 268
                    && uci.getUnitCreditSpecificationId().intValue() != 269
                    && uci.getUnitCreditSpecificationId().intValue() != 270
                    && uci.getUnitCreditSpecificationId().intValue() != 271)) || uci.getProductInstanceId().intValue() != pi.getProductInstanceId().intValue()) {
                continue;
            }
            int s = uci.getUnitCreditSpecificationId().intValue();
            if (s == 220 || s == 221 || s == 222) {
                haslite = true;
            } else if (s == 103 || s == 104 || s == 105 || s == 106 || s == 121 || s == 123 || s == 125 || s == 131 || s == 133 || s == 136 || s == 139 || s == 142 || s == 143 || s == 144 || s == 145 || s == 146 || s == 150 || s == 212 || s == 213 || s == 214 || s == 215 || s == 216 || s == 217 || s == 235 || s == 263 || s == 264 || s == 265 || s == 266 || s == 268 || s == 269 || s == 270 || s == 271) {
                hasnorm = true;
            }
        }
        if (!haslite && !hasnorm) {
            return "Done";
        }
        int specToGive;
        if (hasnorm) {
            specToGive = 238;
        } else {
            specToGive = 240;
        }

        com.smilecoms.commons.sca.PurchaseUnitCreditRequest pucr = new com.smilecoms.commons.sca.PurchaseUnitCreditRequest();
        pucr.setProductInstanceId(pi.getProductInstanceId());
        pucr.setAccountId(si.getAccountId());
        pucr.setUnitCreditSpecificationId(specToGive);
        pucr.setDaysGapBetweenStart(-1);
        pucr.setNumberToPurchase(30);
        pucr.setUniqueId("CAMP_" + lockedCampaignRun.getCampaignId() + "_" + pi.getProductInstanceId());
        try {
            com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().purchaseUnitCredit(pucr);
        } catch (java.lang.Exception e) {
            log.warn("Error trying to provision bundle, most likely a duplicate");
            return "Done";
        }

        ShortMessage sm = new ShortMessage();
        sm.setFrom("0662100100");
        sm.setTo(phoneNumber);
        sm.setBody("Welcome to SmileVoice! You have 10mins + 10SMS FREE to any local numbers. Anyone can call you & you can call anyone so share your Smile no " + phoneNumber + " #NowYouCan");
        sm.setDataCodingScheme((byte) 0x03);
        SCAWrapper.getAdminInstance().sendShortMessage(sm);

        return "Done";
    }

    public String ugVoiceActivation(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, com.smilecoms.commons.sca.Customer cust, String trigger, Logger log) throws Exception {

        java.util.List roles = new java.util.ArrayList();
        roles.add("Administrator");
        com.smilecoms.commons.sca.CallersRequestContext crc = new com.smilecoms.commons.sca.CallersRequestContext("admin", "0.0.0.0", 1, roles);
        com.smilecoms.commons.sca.SCAWrapper.getUserSpecificInstance().setThreadsRequestContext(crc);
        String phoneNumber = com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getProductInstancePhoneNumber(pi.getProductInstanceId().intValue());
        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        // Get the accounts unit credits
        com.smilecoms.commons.sca.Account acc = com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getAccount(si.getAccountId(), com.smilecoms.commons.sca.StAccountLookupVerbosity.ACCOUNT_UNITCREDITS);
        int cnt = acc.getUnitCreditInstances().size();
        boolean hasliteornw = false;
        boolean hasnorm = false;
        for (int i = 0; i < cnt; i++) {
            com.smilecoms.commons.sca.UnitCreditInstance uci = (com.smilecoms.commons.sca.UnitCreditInstance) acc.getUnitCreditInstances().get(i);
            int s = uci.getUnitCreditSpecificationId().intValue();
            if ((uci.getCurrentUnitsRemaining().longValue() <= 0l && s != 235) || uci.getProductInstanceId().intValue() != pi.getProductInstanceId().intValue()) {
                continue;
            }

            if (s == 221 || s == 222 || s == 145 || s == 146 || s == 147) {
                hasliteornw = true;
            } else if (s == 100 || s == 102 || s == 103 || s == 104 || s == 105 || s == 106 || s == 126 || s == 135 || s == 137 || s == 148 || s == 150 || s == 159 || s == 235 || s == 236 || s == 237 || s == 243 || s == 265) {
                hasnorm = true;
            }
        }
        if (!hasliteornw && !hasnorm) {
            return "Done";
        }
        int specToGive;
        if (hasnorm) {
            specToGive = 238;
        } else {
            specToGive = 240;
        }

        com.smilecoms.commons.sca.PurchaseUnitCreditRequest pucr = new com.smilecoms.commons.sca.PurchaseUnitCreditRequest();
        pucr.setProductInstanceId(pi.getProductInstanceId());
        pucr.setAccountId(si.getAccountId());
        pucr.setUnitCreditSpecificationId(specToGive);
        pucr.setNumberToPurchase(1);
        pucr.setUniqueId("CAMP_" + lockedCampaignRun.getCampaignId() + "_" + pi.getProductInstanceId());
        try {
            com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().purchaseUnitCredit(pucr);
        } catch (java.lang.Exception e) {
            log.warn("Error trying to provision bundle, most likely a duplicate");
            return "Done";
        }
        return "Done";
    }

    public String tz5GBPromo(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList) throws Exception {

        java.util.List roles = new java.util.ArrayList();
        roles.add("Administrator");
        com.smilecoms.commons.sca.CallersRequestContext crc = new com.smilecoms.commons.sca.CallersRequestContext("admin", "0.0.0.0", 1, roles);
        com.smilecoms.commons.sca.SCAWrapper.getUserSpecificInstance().setThreadsRequestContext(crc);

        com.smilecoms.commons.sca.Customer cust = com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getCustomer(pi.getCustomerProfileId(), com.smilecoms.commons.sca.StCustomerLookupVerbosity.CUSTOMER);

        int roleCnt = cust.getSecurityGroups().size();
        boolean found = false;
        for (int i = 0; i < roleCnt; i++) {
            String role = (String) cust.getSecurityGroups().get(i);
            if (role.equals("Promo3")) {
                found = true;
            }
        }
        if (found == false) {
            cust.getSecurityGroups().add("Promo3");
            com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().modifyCustomer(cust);
            com.smilecoms.commons.sca.ShortMessage sm = new com.smilecoms.commons.sca.ShortMessage();
            sm.setFrom("sip:+255662100100@tz.smilecoms.com");
            sm.setTo("+27834427179");
            sm.setBody("Congratulations! You can get 50% off a 5GB SmileData Bundle!");
            com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().sendShortMessage(sm);
        }

        return "Done";
    }

    public String tz5GBPromoRemoval(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList) throws Exception {

        java.util.List roles = new java.util.ArrayList();
        roles.add("Administrator");
        com.smilecoms.commons.sca.CallersRequestContext crc = new com.smilecoms.commons.sca.CallersRequestContext("admin", "0.0.0.0", 1, roles);
        com.smilecoms.commons.sca.SCAWrapper.getUserSpecificInstance().setThreadsRequestContext(crc);

        com.smilecoms.commons.sca.Customer cust = com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getCustomer(pi.getCustomerProfileId(), com.smilecoms.commons.sca.StCustomerLookupVerbosity.CUSTOMER);
        int roleCnt = cust.getSecurityGroups().size();
        java.util.List newRoles = new java.util.ArrayList();
        for (int i = 0; i < roleCnt; i++) {
            String role = (String) cust.getSecurityGroups().get(i);
            if (!role.equals("Promo3")) {
                newRoles.add(role);
            }
        }
        cust.getSecurityGroups().clear();
        cust.getSecurityGroups().addAll(newRoles);
        com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().modifyCustomer(cust);

        return "Done";
    }

    public String runVoiceCampaignActionCode(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, com.smilecoms.commons.sca.Customer cust, String trigger, Logger log) throws Exception {

        if (1 == 2) {
            return "";
        }
        java.util.List roles = new java.util.ArrayList();
        roles.add("Administrator");
        com.smilecoms.commons.sca.CallersRequestContext crc = new com.smilecoms.commons.sca.CallersRequestContext("admin", "0.0.0.0", 1, roles);
        com.smilecoms.commons.sca.SCAWrapper.getUserSpecificInstance().setThreadsRequestContext(crc);

        // Only give free minutes if there is no unlimited bundle
        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        com.smilecoms.commons.sca.Account acc = com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getAccount(si.getAccountId(), com.smilecoms.commons.sca.StAccountLookupVerbosity.ACCOUNT_UNITCREDITS);
        int cnt = acc.getUnitCreditInstances().size();
        boolean hasUnlimited = false;
        for (int i = 0; i < cnt; i++) {
            com.smilecoms.commons.sca.UnitCreditInstance uci = (com.smilecoms.commons.sca.UnitCreditInstance) acc.getUnitCreditInstances().get(i);
            int s = uci.getUnitCreditSpecificationId().intValue();
            if (s == 235 || s == 238) {
                hasUnlimited = true;
                break;
            }
        }
        if (hasUnlimited) {
            return "Done";
        }
        String phoneNumber = com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getProductInstancePhoneNumber(pi.getProductInstanceId().intValue());
        com.smilecoms.commons.sca.PurchaseUnitCreditRequest pucr = new com.smilecoms.commons.sca.PurchaseUnitCreditRequest();
        pucr.setProductInstanceId(pi.getProductInstanceId());
        pucr.setAccountId(si.getAccountId());
        pucr.setUnitCreditSpecificationId(230);
        pucr.setNumberToPurchase(1);
        pucr.setUniqueId("CAMP_" + lockedCampaignRun.getCampaignId() + "_" + pi.getProductInstanceId());

        try {
            com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().purchaseUnitCredit(pucr);
        } catch (Exception e) {
            log.warn("Probably a duplicate sale, returning Done");
            return "Done";
        }
        com.smilecoms.commons.sca.ShortMessage sm = new com.smilecoms.commons.sca.ShortMessage();
        sm.setFrom("343");
        sm.setTo(phoneNumber);
        sm.setBody("Welcome to using SmileVoice. You have 30 FREE mins and 30 SMSs, valid for 30 days, to call and SMS any number in Nigeria #YouDeserveMore");
        sm.setDataCodingScheme((byte) 0x03);
        com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().sendShortMessage(sm);
        return "Done";
    }

    public String runUGRevenueInitiative1(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, com.smilecoms.commons.sca.Customer cust, String triggerResult) throws Exception {

        if (1 == 2) {
            return "";
        }
        java.util.List roles = new java.util.ArrayList();
        roles.add("Administrator");
        com.smilecoms.commons.sca.CallersRequestContext crc = new com.smilecoms.commons.sca.CallersRequestContext("admin", "0.0.0.0", 1, roles);
        com.smilecoms.commons.sca.SCAWrapper.getUserSpecificInstance().setThreadsRequestContext(crc);

        int ucInstId = (int) Integer.valueOf(triggerResult).intValue();
        int specIdToAdd = 0;

        // Only give free minutes if there is no unlimited bundle
        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        com.smilecoms.commons.sca.Account acc = com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getAccount(si.getAccountId(), com.smilecoms.commons.sca.StAccountLookupVerbosity.ACCOUNT_UNITCREDITS);
        int cnt = acc.getUnitCreditInstances().size();
        for (int i = 0; i < cnt; i++) {
            com.smilecoms.commons.sca.UnitCreditInstance uci = (com.smilecoms.commons.sca.UnitCreditInstance) acc.getUnitCreditInstances().get(i);
            int thisUCInstId = uci.getUnitCreditInstanceId();
            if (ucInstId != thisUCInstId) {
                continue;
            }
            int specId = uci.getUnitCreditSpecificationId().intValue();
            if (specId == 100) {
                specIdToAdd = 107;
            } else if (specId == 102) {
                specIdToAdd = 115;
            } else if (specId == 103) {
                specIdToAdd = 110;
            } else if (specId == 104) {
                specIdToAdd = 254;
            } else if (specId == 105) {
                specIdToAdd = 255;
            } else if (specId == 145) {
                specIdToAdd = 257;
            } else if (specId == 146) {
                specIdToAdd = 258;
            } else if (specId == 147) {
                specIdToAdd = 259;
            } else if (specId == 150) {
                specIdToAdd = 109;
            } else if (specId == 159) {
                specIdToAdd = 256;
            } else if (specId == 221) {
                specIdToAdd = 108;
            } else if (specId == 222) {
                specIdToAdd = 260;
            } else if (specId == 235) {
                specIdToAdd = 261;
            } else if (specId == 242) {
                specIdToAdd = 262;
            } else if (specId == 246) {
                specIdToAdd = 260;
            }
            break;
        }
        if (specIdToAdd > 0) {
            com.smilecoms.commons.sca.PurchaseUnitCreditRequest pucr = new com.smilecoms.commons.sca.PurchaseUnitCreditRequest();
            pucr.setProductInstanceId(pi.getProductInstanceId());
            pucr.setAccountId(si.getAccountId());
            pucr.setUnitCreditSpecificationId(specIdToAdd);
            pucr.setNumberToPurchase(1);
            pucr.setUniqueId("CAMP_" + lockedCampaignRun.getCampaignId() + "_" + triggerResult);
            com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().purchaseUnitCredit(pucr);
        }
        return "Done";
    }
    
   public String runKyooKyaDoubleCode(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, 
        com.smilecoms.cm.db.model.ProductInstance pi, List siList, Customer cust, String triggerResult, Logger log) throws Exception {
        
       String campaignId = "KYOOKYA_DOUBLE";
        int bonusSpecId = 395;
        String campaignDate = triggerResult.split("_")[0];
        long accountId = Long.parseLong(triggerResult.split("_")[1]);
        int dynamicUCSizeInMB = Integer.parseInt(triggerResult.split("_")[2]);
        
        if(bonusSpecId > 0) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("ddMMyyyyHHmmSS");
        
            String timestamp = sdf.format(new java.util.Date());
            
            // String bonusSize = com.smilecoms.commons.util.Utils.displayVolumeAsStringInGB(ucs.getUnits(), "byte") + "GB ";
            CampaignBean cb = new CampaignBean("kyookya", pi.getProductInstanceId().intValue(), pi.getProductInstanceId().intValue() + timestamp, "BonusUCSpecId=" + bonusSpecId + ",DynamicUCSizeInMB=" + dynamicUCSizeInMB);
    
            String message  = "[AND:]Congratulations! Your current bundle balance has been double with KyookyaDouble bonus data. Click ";
            String suffix = " to claim your BONUS. #KyookyaDouble. Smile";
            
            cb.inviteViaSMSWithRedirect(message, suffix, "256720100100", cust.getAlternativeContact1(), true, "#KyookyaDouble");
	// cb.inviteViaSMSWithRedirect(message, suffix, "256720100100", cust.getAlternativeContact1());
            
        }
        return "Done";
    }
   
   public String runKyooKyaDoubleCodeOLD(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, 
        com.smilecoms.cm.db.model.ProductInstance pi, List siList, Customer cust, String triggerResult, Logger log) throws Exception {
        
       String campaignId = "KYOOKYA_DOUBLE";
        int bonusSpecId = 395;
        String campaignDate = triggerResult.split("_")[0];
        long accountId = Long.parseLong(triggerResult.split("_")[1]);
        int sizeInMB = Integer.parseInt(triggerResult.split("_")[2]);
        
        CampaignBean cb = new CampaignBean("KYOOKYA_DOUBLE_", pi.getProductInstanceId().intValue(),"NA"); // "BonusUCSpecId=" + bonusSpecId);

        if (!cb.provisionUnitCredit(pi.getProductInstanceId(), accountId, bonusSpecId, sizeInMB, campaignId + "_" + pi.getProductInstanceId() + "_" + triggerResult + "_" + bonusSpecId)) {
            log.warn("Error trying to provision bundle, most likely a duplicate");
            return "Done";
        }
        return "Done";
    }
   
   public String runKyooKyaMondayCode(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, 
        com.smilecoms.cm.db.model.ProductInstance pi, List siList, Customer cust, String triggerResult, Logger log) throws Exception {
        
        int bonusSpecId = 0;
        int purchasedSpecId = Integer.parseInt(triggerResult.split("_")[1]); // Unit credit spec that was purchased
        String purchasedBundleName = triggerResult.split("_")[2];
        
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("ddMMyyyyHHmmSS");
        
        String parentUCI=triggerResult.split("_")[3];
        
        String timestamp = sdf.format(new java.util.Date());
        // 100,221,222,102,150,103,104,105,159,361

        if (purchasedSpecId == 100) {
            bonusSpecId = 392;
        } else if (purchasedSpecId == 221) {
            bonusSpecId = 393;
        } else if (purchasedSpecId == 104) {
            bonusSpecId = 396;
        } else if (purchasedSpecId == 105) {
            bonusSpecId = 397;
        } else if (purchasedSpecId == 159) {
            bonusSpecId = 398;
        } else if (purchasedSpecId == 361) {
            bonusSpecId = 399;
        } else if (purchasedSpecId == 145) {
            bonusSpecId = 400;
        } else if (purchasedSpecId == 146) {
            bonusSpecId = 401;
        } else if (purchasedSpecId == 147) {
            bonusSpecId = 402;
        } else if (purchasedSpecId == 222) {
            bonusSpecId = 403;
        }else if (purchasedSpecId == 102) {
            bonusSpecId = 404;
        }else if (purchasedSpecId == 150) {
            bonusSpecId = 405;
        }else if (purchasedSpecId == 103) {
            bonusSpecId = 406;
        } else {
            bonusSpecId = 0;
        }
        
        if(bonusSpecId > 0) {
            com.smilecoms.commons.sca.beans.UnitCreditBean ucBean = com.smilecoms.commons.sca.beans.UnitCreditBean.getUnitCreditById(Integer.parseInt(parentUCI));
            java.text.SimpleDateFormat sdfExp = new java.text.SimpleDateFormat("dd/MM");         
            String expDate = sdfExp.format(new Date(ucBean.getExpiryDate()));
            
            com.smilecoms.commons.sca.UnitCreditSpecification ucs = com.smilecoms.commons.sca.beans.NonUserSpecificCachedDataHelper.getUnitCreditSpecification(bonusSpecId);
            
            String bonusSize = com.smilecoms.commons.util.Utils.displayVolumeAsStringInGB(ucs.getUnits(), "byte") + "GB ";
            CampaignBean cb = new CampaignBean("kyookya", pi.getProductInstanceId().intValue(), pi.getProductInstanceId().intValue() + timestamp, "BonusUCSpecId=" + bonusSpecId + ",ParentUCI=" + parentUCI);

            String message  = "Your new " + purchasedBundleName + " expires on "  + expDate + ". It is KyookyaMonday, so you also get 10X your recharged data! Click ";
            String suffix = " to claim " + bonusSize + "BONUS. Smile";

            cb.inviteViaSMSWithRedirect(message, suffix, "256720100100", cust.getAlternativeContact1(), true, "#KyookyaMonday");
	// cb.inviteViaSMSWithRedirect(message, suffix, "256720100100", cust.getAlternativeContact1());
            return triggerResult;
        }
        
        return "Done";
    }
   
   public String runKyooKyaMondayCodedd(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, 
        com.smilecoms.cm.db.model.ProductInstance pi, List siList, Customer cust, String triggerResult, Logger log) throws Exception {
        
        int bonusSpecId = 0;
        int purchasedSpecId = Integer.parseInt(triggerResult.split("_")[1]); // Unit credit spec that was purchased
        String purchasedBundleName = triggerResult.split("_")[2];
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("ddMMyyyyHHmmSS");  
        String parentUCI=triggerResult.split("_")[3];
        
        String timestamp = sdf.format(new java.util.Date());
        // 100,221,222,102,150,103,104,105,159,361

        if (purchasedSpecId == 100) {
            bonusSpecId = 392;
        } else if (purchasedSpecId == 221) {
            bonusSpecId = 393;
        } else if (purchasedSpecId == 104) {
            bonusSpecId = 396;
        } else if (purchasedSpecId == 105) {
            bonusSpecId = 397;
        } else if (purchasedSpecId == 159) {
            bonusSpecId = 398;
        } else if (purchasedSpecId == 361) {
            bonusSpecId = 399;
        } else if (purchasedSpecId == 145) {
            bonusSpecId = 400;
        } else if (purchasedSpecId == 146) {
            bonusSpecId = 401;
        } else if (purchasedSpecId == 147) {
            bonusSpecId = 402;
        } else if (purchasedSpecId == 222) {
            bonusSpecId = 403;
        }else if (purchasedSpecId == 102) {
            bonusSpecId = 404;
        }else if (purchasedSpecId == 150) {
            bonusSpecId = 405;
        }else if (purchasedSpecId == 103) {
            bonusSpecId = 406;
        } else {
            bonusSpecId = 0;
        }
        
        if(bonusSpecId > 0) {
            com.smilecoms.commons.sca.beans.UnitCreditBean ucBean = com.smilecoms.commons.sca.beans.UnitCreditBean.getUnitCreditById(Integer.parseInt(parentUCI));
            java.text.SimpleDateFormat sdfExp = new java.text.SimpleDateFormat("dd/MM");         
            String expDate = sdfExp.format(new Date(ucBean.getExpiryDate()));
            
            com.smilecoms.commons.sca.UnitCreditSpecification ucs = com.smilecoms.commons.sca.beans.NonUserSpecificCachedDataHelper.getUnitCreditSpecification(bonusSpecId);
            
            String bonusSize = com.smilecoms.commons.util.Utils.displayVolumeAsStringInGB(ucs.getUnits(), "byte") + "GB ";
            CampaignBean cb = new CampaignBean("kyookya", pi.getProductInstanceId().intValue(), pi.getProductInstanceId().intValue() + timestamp, "BonusUCSpecId=" + bonusSpecId + ",ParentUCI=" + parentUCI);

            String message  = "[AND:]Your new " + purchasedBundleName + " expires on "  + expDate + ". It is KyookyaMonday, so you also get 10X your recharged data! Click ";
            String suffix = " to claim " + bonusSize + "BONUS. Smile";

            cb.inviteViaSMSWithRedirect(message, suffix, "256720100100", cust.getAlternativeContact1(), true, "#KyookyaMonday");
	// cb.inviteViaSMSWithRedirect(message, suffix, "256720100100", cust.getAlternativeContact1());
            return triggerResult;
        }
        
        return "Done";
    }
   
   public String runKyooKyaMondayCodeTest(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, 
        com.smilecoms.cm.db.model.ProductInstance pi, List siList, Customer cust, String triggerResult, Logger log) throws Exception {
        
        int bonusSpecId = 0;
        int purchasedSpecId = Integer.parseInt(triggerResult.split("_")[1]); // Unit credit spec that was purchased
        String purchasedBundleName = triggerResult.split("_")[2];
        String expirtyDate = triggerResult.split("_")[3];
        String parentUCI=triggerResult.split("_")[4];
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("ddMMyyyyHHmmSS");  
        
        String timestamp = sdf.format(new java.util.Date());
        
        if (purchasedSpecId == 100) {
            bonusSpecId = 392;
        } else if (purchasedSpecId == 102) {
            bonusSpecId = 115;
        } else {
            bonusSpecId = 0;
        }
                
        if(bonusSpecId > 0) {
            
            CampaignBean cb = new CampaignBean("kyookya", pi.getProductInstanceId().intValue(), pi.getProductInstanceId().intValue() + timestamp, "BonusUCSpecId=" + bonusSpecId);
            com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
            
            if (!cb.provisionUnitCredit(pi.getProductInstanceId(), si.getAccountId(), bonusSpecId, 1, "CAMP3_ACTION_12_KyookyaMonday_" + pi.getProductInstanceId() + "_" + bonusSpecId, "ParentUCI=" + parentUCI)) {
                log.warn("Error trying to provision bundle, most likely a duplicate");
                return "Done";
            }
                        
            String message  = "Your new " + purchasedBundleName + " expires on "  + expirtyDate + ". It is KyookyaMonday, so you also get 10X your recharged data! Click ";
            String suffix = " to claim <??GB> BONUS. Smile";

            cb.inviteViaSMSWithRedirect(message, suffix, "256720100100", cust.getAlternativeContact1());
            return triggerResult;
        }
        
        return "Done";
    }
   
   public String runKyooKyaMondayCodeff(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, 
        com.smilecoms.cm.db.model.ProductInstance pi, List siList, Customer cust, String triggerResult, Logger log) throws Exception {
        
        int bonusSpecId = 0;
        int purchasedSpecId = Integer.parseInt(triggerResult.split("_")[1]); // Unit credit spec that was purchased
        String purchasedBundleName = triggerResult.split("_")[2];
        String expirtyDate = triggerResult.split("_")[3];
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("ddMMyyyyHHmmSS");  
        String parentUCI=triggerResult.split("_")[4];
        
        String timestamp = sdf.format(new java.util.Date());
        // 100,221,222,102,150,103,104,105,159,361

        if (purchasedSpecId == 100) {
            bonusSpecId = 392;
        } else if (purchasedSpecId == 221) {
            bonusSpecId = 393;
        } else if (purchasedSpecId == 104) {
            bonusSpecId = 396;
        } else if (purchasedSpecId == 105) {
            bonusSpecId = 397;
        } else if (purchasedSpecId == 159) {
            bonusSpecId = 398;
        } else if (purchasedSpecId == 361) {
            bonusSpecId = 399;
        } else if (purchasedSpecId == 145) {
            bonusSpecId = 400;
        } else if (purchasedSpecId == 146) {
            bonusSpecId = 401;
        } else if (purchasedSpecId == 147) {
            bonusSpecId = 402;
        } else if (purchasedSpecId == 222) {
            bonusSpecId = 403;
        }else if (purchasedSpecId == 102) {
            bonusSpecId = 404;
        }else if (purchasedSpecId == 150) {
            bonusSpecId = 405;
        }else if (purchasedSpecId == 103) {
            bonusSpecId = 406;
        } else {
            bonusSpecId = 0;
        }
        
        if(bonusSpecId > 0) {
            com.smilecoms.commons.sca.UnitCreditSpecification ucs = com.smilecoms.commons.sca.beans.NonUserSpecificCachedDataHelper.getUnitCreditSpecification(bonusSpecId);
            
            String bonusSize = com.smilecoms.commons.util.Utils.displayVolumeAsStringInGB(ucs.getUnits(), "byte") + "GB ";
            CampaignBean cb = new CampaignBean("kyookya", pi.getProductInstanceId().intValue(), pi.getProductInstanceId().intValue() + timestamp, "BonusUCSpecId=" + bonusSpecId + ",ParentUCI=" + parentUCI);

            String message  = "Your new " + purchasedBundleName + " expires on "  + expirtyDate + ". It is KyookyaMonday, so you also get 10X your recharged data! Click ";
            String suffix = " to claim " + bonusSize + "BONUS. Smile";

            cb.inviteViaSMSWithRedirect(message, suffix, "256720100100", cust.getAlternativeContact1());
            return triggerResult;
        }
        
        return "Done";
    }
    

    public String runFirstAnnversaryActionCode(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun,
            com.smilecoms.cm.db.model.ProductInstance pi, List siList, Customer cust, String triggerResult) throws Exception {
        CampaignBean cb = new CampaignBean("FAV", pi.getProductInstanceId().intValue(), "NA");
        cb.inviteViaSMS("Congratulations " + cust.getFirstName() + " on your 1 year anniversary with Smile. Get your 1GB gift bundle here: ", "", "256720100100", cust.getAlternativeContact1());
        return triggerResult;
    }

    public String give1GB(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust, String triggerResult) throws Exception {
        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        PurchaseUnitCreditRequest pucr = new PurchaseUnitCreditRequest();
        pucr.setProductInstanceId(pi.getProductInstanceId());
        pucr.setAccountId(si.getAccountId());
        pucr.setUnitCreditSpecificationId(108);
        pucr.setNumberToPurchase(1);
        pucr.setUniqueId("CAMP_" + lockedCampaignRun.getCampaignId() + "_" + pi.getProductInstanceId());
        com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().purchaseUnitCredit(pucr);
        return "Done";
    }

    public String notifyToOptIn(com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust) throws Exception {
        com.smilecoms.commons.sca.ShortMessage sm = new com.smilecoms.commons.sca.ShortMessage();
        sm.setFrom("0662000110");
        sm.setTo(cust.getAlternativeContact1());
        sm.setBody("Dear Smile Customer, we hope you are now enjoying your SuperFast internet. Thank you for your patience. Please reply with " + pi.getProductInstanceId() + " to enjoy a 1GB free data bundle on us.");
        sm.setDataCodingScheme((byte) 0x03);
        com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().sendShortMessage(sm);
        return "Done";
    }

    public String giveGB(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust, String triggerResult) throws Exception {
        int s = Integer.parseInt(triggerResult);
        int toProv = 0;
        if (s == 102 || s == 125 || s == 150) {
            toProv = 108;
        } else if (s == 103 || s == 126 || s == 233 || s == 127 || s == 104 || s == 235 || s == 105 || s == 128 || s == 129) {
            toProv = 115;
        }
        if (toProv == 0) {
            return "Done";
        }
        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        PurchaseUnitCreditRequest pucr = new PurchaseUnitCreditRequest();
        pucr.setProductInstanceId(pi.getProductInstanceId());
        pucr.setAccountId(si.getAccountId());
        pucr.setUnitCreditSpecificationId(toProv);
        pucr.setNumberToPurchase(1);
        pucr.setUniqueId("CAMP_" + lockedCampaignRun.getCampaignId() + "_" + pi.getProductInstanceId());
        com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().purchaseUnitCredit(pucr);
        return "Done";
    }

    public String notifyToOptIn(com.smilecoms.cm.db.model.Campaign campaign, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust, Logger log) throws Exception {
        ShortMessage sm = new ShortMessage();
        sm.setFrom("07020100100");
        sm.setTo(cust.getAlternativeContact1());
        sm.setBody("Dear " + cust.getFirstName() + ", recharge SIM " + pi.getPhysicalId() + " with 10GB and receive 3GB free. The Smile Team.");
        sm.setDataCodingScheme((byte) 0x03);
        SCAWrapper.getAdminInstance().sendShortMessage(sm);
        return "Done";
    }

    public String giveGB(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust, String triggerResult, Logger log) throws Exception {
        int s = Integer.parseInt(triggerResult);
        int toProv = 0;
        if (s == 102) {
            toProv = 115; // 3GB
        } else if (s == 103 || s == 150 || s == 126 || s == 233 || s == 127 || s == 104 || s == 235 || s == 105 || s == 128 || s == 129) {
            toProv = 109; // 5GB
        } else if (s == 220) {
            toProv = 108; // 1GB
        }
        if (toProv == 0) {
            return "Done";
        }
        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        PurchaseUnitCreditRequest pucr = new PurchaseUnitCreditRequest();
        pucr.setProductInstanceId(pi.getProductInstanceId());
        pucr.setAccountId(si.getAccountId());
        pucr.setUnitCreditSpecificationId(toProv);
        pucr.setNumberToPurchase(1);
        pucr.setUniqueId("CAMP_" + lockedCampaignRun.getCampaignId() + "_" + pi.getProductInstanceId());
        com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().purchaseUnitCredit(pucr);
        return "Done";
    }

    public String giveDataOnBirthday(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust, String triggerResult, Logger log) throws Exception {
        log.warn("Its is customers birthday [{}]", Integer.valueOf(cust.getCustomerId()));
        int specId;
        if (cust.getFirstName().toLowerCase().startsWith("a")) {
            specId = 108;
        } else {
            specId = 258;
        }

        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        PurchaseUnitCreditRequest pucr = new PurchaseUnitCreditRequest();
        pucr.setProductInstanceId(pi.getProductInstanceId());
        pucr.setAccountId(si.getAccountId());
        pucr.setUnitCreditSpecificationId(specId);
        pucr.setNumberToPurchase(1);
        pucr.setUniqueId("CAMP_" + lockedCampaignRun.getCampaignId() + "_" + pi.getProductInstanceId() + "_" + triggerResult);
        SCAWrapper.getAdminInstance().purchaseUnitCredit(pucr);
        return "Done";
    }

    public String runFirstAnnversaryActionCode(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun,
            com.smilecoms.cm.db.model.ProductInstance pi, List siList, Customer cust, String triggerResult, Logger log) throws Exception {
        CampaignBean cb = new CampaignBean("FAV", pi.getProductInstanceId().intValue(), "NA");
        cb.inviteViaSMS("Congratulations " + cust.getFirstName() + " on your 1 year anniversary with Smile. Get your 1GB gift bundle here: ", "", "256720100100", cust.getAlternativeContact1());
        return triggerResult;
    }

    public String notifyDormantWinBack2(com.smilecoms.cm.db.model.Campaign campaign, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust, Logger log) throws Exception {
        String campaignId = "DORMANT_WINBACK2";
        ShortMessage sm = new ShortMessage();
        sm.setFrom("07020100100");
        sm.setTo(cust.getAlternativeContact1());
        sm.setBody("Recharge your inactive line with 5GB or more and get 100% bonus up to 20GB valid for 15 days and usable MidNites & Weekends. For more info, visit Smile.com.ng!");
        sm.setCampaignId(campaignId);
        sm.setDataCodingScheme((byte) 0x03);
        SCAWrapper.getAdminInstance().sendShortMessage(sm);

        String phoneNumber = (String) com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getProductInstancePhoneNumber(pi.getProductInstanceId().intValue());
        boolean isNumber = true;
        try {
            java.lang.Long.parseLong(phoneNumber);
        } catch (java.lang.NumberFormatException e) {
            isNumber = false;
        }

        if (isNumber) {
            ShortMessage sm1 = new ShortMessage();
            sm1.setFrom("07020100100");
            sm1.setTo(phoneNumber);
            sm1.setCampaignId(campaignId);
            sm1.setBody("Recharge your inactive line with 5GB or more and get 100% bonus up to 20GB valid for 15 days and usable MidNites & Weekends. For more info, visit Smile.com.ng!");
            sm1.setDataCodingScheme((byte) 0x03);
            SCAWrapper.getAdminInstance().sendShortMessage(sm1);
        }

        return "Done";
    }

    public String notifyChurnWinBack2(com.smilecoms.cm.db.model.Campaign campaign, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust, Logger log) throws Exception {
        ShortMessage sm = new ShortMessage();
        sm.setFrom("07020100100");
        sm.setTo(cust.getAlternativeContact1());
        sm.setBody("Recharge today and get 200% of bundle value as a bonus usable ANYTIME and valid for 30 days. Thank you for choosing Smile. To recharge, visit Smile.com.ng!");
        sm.setDataCodingScheme((byte) 0x03);
        SCAWrapper.getAdminInstance().sendShortMessage(sm);

        String phoneNumber = (String) com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getProductInstancePhoneNumber(pi.getProductInstanceId().intValue());
        boolean isNumber = true;
        try {
            java.lang.Long.parseLong(phoneNumber);
        } catch (java.lang.NumberFormatException e) {
            isNumber = false;
        }

        if (isNumber) {
            ShortMessage sm1 = new ShortMessage();
            sm1.setFrom("07020100100");
            sm1.setTo(phoneNumber);
            sm1.setBody("Recharge today and get 200% of bundle value as a bonus usable ANYTIME and valid for 30 days. Thank you for choosing Smile. To recharge, visit Smile.com.ng!");
            sm1.setDataCodingScheme((byte) 0x03);
            SCAWrapper.getAdminInstance().sendShortMessage(sm1);
        }

        return "Done";
    }

    public String notifySmsLowValueStimulateUsageCampaign(com.smilecoms.cm.db.model.Campaign campaign, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust, Logger log) throws Exception {
        ShortMessage sm = new ShortMessage();
        sm.setFrom("07020100100");
        sm.setTo(cust.getAlternativeContact1());
        sm.setBody("Use your Smile Device everyday this week (mon-fri) and get 1GB Bonus data (for use Sunday only). Thank you for choosing Smile. To recharge, visit Smile.com.ng!");
        sm.setDataCodingScheme((byte) 0x03);
        SCAWrapper.getAdminInstance().sendShortMessage(sm);

        String phoneNumber = com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getProductInstancePhoneNumber(pi.getProductInstanceId().intValue());
        ShortMessage sm1 = new ShortMessage();
        sm1.setFrom("343");
        sm1.setTo(phoneNumber);
        sm1.setBody("Use your Smile Device everyday (M-F) and get Bonus data (for use Sunday only). Thank you for choosing Smile. To recharge, visit Smile.com.ng!");
        sm1.setDataCodingScheme((byte) 0x03);
        SCAWrapper.getAdminInstance().sendShortMessage(sm1);
        return "Done";
    }

    public String notifySmsHighValueStimulateUsageCampaign(com.smilecoms.cm.db.model.Campaign campaign, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust, Logger log) throws Exception {
        ShortMessage sm = new ShortMessage();
        sm.setFrom("07020100100");
        sm.setTo(cust.getAlternativeContact1());
        sm.setBody("Use your Smile Device everyday this week (mon-fri) and get 3GB Bonus data (for use Sunday only). Thank you for choosing Smile. To recharge, visit Smile.com.ng!");
        sm.setDataCodingScheme((byte) 0x03);
        SCAWrapper.getAdminInstance().sendShortMessage(sm);

        String phoneNumber = com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getProductInstancePhoneNumber(pi.getProductInstanceId().intValue());
        ShortMessage sm1 = new ShortMessage();
        sm1.setFrom("343");
        sm1.setTo(phoneNumber);
        sm1.setBody("Use your Smile Device everyday (M-F) and get Bonus data (for use Sunday only). Thank you for choosing Smile. To recharge, visit Smile.com.ng!");
        sm1.setDataCodingScheme((byte) 0x03);
        SCAWrapper.getAdminInstance().sendShortMessage(sm1);
        return "Done";
    }

    public String giveGBChurnWinBack2(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust, String triggerResult, Logger log) throws Exception {
        int s = Integer.parseInt(triggerResult);
        int toProv = 0;

        if (s == 220) {
            toProv = 108;
        } else if (s == 102) {
            toProv = 115;
        } else if (s == 150 || s == 125) {
            toProv = 109;
        } else if (s == 103 || s == 126) {
            toProv = 110;
        } else if (s == 104 || s == 127 || s == 232) {
            toProv = 259;
        } else if (s == 105 || s == 234) {
            toProv = 260;
        } else if (s == 128) {
            toProv = 261;
        } else if (s == 129) {
            toProv = 262;
        }

        if (toProv == 0) {
            return "Done";
        }
        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        PurchaseUnitCreditRequest pucr = new PurchaseUnitCreditRequest();
        pucr.setProductInstanceId(pi.getProductInstanceId());
        pucr.setAccountId(si.getAccountId());
        pucr.setUnitCreditSpecificationId(toProv);
        pucr.setNumberToPurchase(2);
        pucr.setUniqueId("CAMP_" + lockedCampaignRun.getCampaignId() + "_" + pi.getProductInstanceId());
        com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().purchaseUnitCredit(pucr);
        return "Done";
    }

    public String giveGBDormantWinBack2(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust, String triggerResult, Logger log) throws Exception {
        int s = Integer.parseInt(triggerResult);
        int toProv = 0;

        if (s == 150) {
            toProv = 267;
        } else if (s == 103) {
            toProv = 268;
        } else if (s == 104) {
            toProv = 269;
        } //      removed because the 50GB and 100GB midnight and weekend gift bundles were not approved so we do up to 20GB
        //        else if (s == 105) {
        //            toProv = 270;
        //        } else if (s == 128) {
        //            toProv = 271;
        //        }
        else if (s == 274) {
            toProv = 268;
        } else if (s == 105) {
            toProv = 269;
        } else if (s == 128) {
            toProv = 269;
        } else if (s == 129) {
            toProv = 269;
        } else if (s == 273) {
            toProv = 269;
        }

        if (toProv == 0) {
            return "Done";
        }
        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        PurchaseUnitCreditRequest pucr = new PurchaseUnitCreditRequest();
        pucr.setProductInstanceId(pi.getProductInstanceId());
        pucr.setAccountId(si.getAccountId());
        pucr.setUnitCreditSpecificationId(toProv);
        pucr.setNumberToPurchase(1);
        pucr.setUniqueId("CAMP_" + lockedCampaignRun.getCampaignId() + "_" + pi.getProductInstanceId());
        com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().purchaseUnitCredit(pucr);
        return "Done";
    }

    public String giveGBAndNotifyExciteTheBase(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust, String triggerResult, Logger log) throws Exception {
        int s = Integer.parseInt(triggerResult);
        int toProv = 0;
        String bundle = "";
        if (s == 128 || s == 129 || s == 132) {
            toProv = 110;
            bundle = "10GB";
        } else if (s == 105) {
            toProv = 109;
            bundle = "5GB";
        } else if (s == 104) {
            toProv = 258;
            bundle = "2GB";
        } else if (s == 103) {
            toProv = 108;
            bundle = "1GB";
        } else if (s == 150 || s == 224) {
            toProv = 107;
            bundle = "500MB";
        }

        if (toProv == 0) {
            return "Done";
        }
        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        PurchaseUnitCreditRequest pucr = new PurchaseUnitCreditRequest();
        pucr.setProductInstanceId(pi.getProductInstanceId());
        pucr.setAccountId(si.getAccountId());
        pucr.setUnitCreditSpecificationId(toProv);
        pucr.setNumberToPurchase(1);
        pucr.setUniqueId("CAMP_" + lockedCampaignRun.getCampaignId() + "_" + pi.getProductInstanceId());
        com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().purchaseUnitCredit(pucr);

        String text = "You have been selected to enjoy " + bundle + " of data as a thank you gift from Smile. Thank you for choosing Smile. #NowYouCan!";

        ShortMessage sm = new ShortMessage();
        sm.setFrom("07020100100");
        sm.setTo(cust.getAlternativeContact1());
        sm.setBody(text);
        sm.setDataCodingScheme((byte) 0x03);
        SCAWrapper.getAdminInstance().sendShortMessage(sm);

        String phoneNumber = (String) com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getProductInstancePhoneNumber(pi.getProductInstanceId().intValue());
        boolean isNumber = true;
        try {
            java.lang.Long.parseLong(phoneNumber);
        } catch (java.lang.NumberFormatException e) {
            isNumber = false;
        }

        if (isNumber) {
            ShortMessage sm1 = new ShortMessage();
            sm1.setFrom("07020100100");
            sm1.setTo(phoneNumber);
            sm1.setBody(text);
            sm1.setDataCodingScheme((byte) 0x03);
            SCAWrapper.getAdminInstance().sendShortMessage(sm1);
        }

        return "Done";
    }

    public String createCL3DaysAfterSmileVoiceActivated(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust, String triggerResult, Logger log) throws Exception {

        com.smilecoms.commons.platform.PlatformEventManager.createEvent("CL_VOICE", "3_DAYS_AFTER_SMILEVOICE_ACTIVATED",
                String.valueOf(pi.getProductInstanceId()),
                "PIId=" + pi.getProductInstanceId());

        return "Done";
    }

    public String createCL5DaysOnNetworkNoSmilevoice(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust, String triggerResult, Logger log) throws Exception {

        String subType = "5_DAYS_ON_NETWORK_NO_SMILEVOICE";
        if (triggerResult.equalsIgnoreCase("unlimited")) {
            subType = "5_DAYS_ON_NETWORK_NO_SMILEVOICE_UL";
        }

        com.smilecoms.commons.platform.PlatformEventManager.createEvent("CL_VOICE", subType,
                String.valueOf(pi.getProductInstanceId()),
                "PIId=" + pi.getProductInstanceId());

        return "Done";
    }

    public String runSIMBundlingActionCode(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, com.smilecoms.commons.sca.Customer cust, String trigger, Logger log) throws Exception {
        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        com.smilecoms.commons.sca.PurchaseUnitCreditRequest pucr = new com.smilecoms.commons.sca.PurchaseUnitCreditRequest();
        pucr.setProductInstanceId(pi.getProductInstanceId());
        pucr.setAccountId(si.getAccountId());
        pucr.setUnitCreditSpecificationId(230);
        pucr.setNumberToPurchase(1);
        pucr.setUniqueId("CAMP_" + lockedCampaignRun.getCampaignId() + "_" + pi.getProductInstanceId());
        com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().purchaseUnitCredit(pucr);
        return "Done";
    }

    //  campaign, pi, siList, customer, log
    public String runNetworkDownEnrolment(com.smilecoms.cm.db.model.Campaign campaign,
            com.smilecoms.cm.db.model.ProductInstance pi, List siList, Customer cust, Logger log) throws Exception {
        CampaignBean cb = new CampaignBean("ND1", pi.getProductInstanceId().intValue(), "NA");
        cb.inviteViaSMS("Dear Smile Customer. For the inconvenience faced due to network downtime, kindly click here ", " to redeem your free bundle. Enjoy the service.", "256720100100", cust.getAlternativeContact1());
        //  cb.inviteViaEmail("nwdown.email.subject", "nwdown.email.body", "customercare@smile.co.ug", cust.getEmailAddress(), new Object[]{cust.getFirstName()});
        return "";
    }

    public String runNetworkDownOptedIn(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, com.smilecoms.commons.sca.Customer cust, String trigger, Logger log) throws Exception {
        log.warn("Unit credit spec id is [{}]", trigger);
        int ucid = Integer.parseInt(trigger);
        String ucName = NonUserSpecificCachedDataHelper.getUnitCreditSpecification(ucid).getName();
        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        com.smilecoms.commons.sca.PurchaseUnitCreditRequest pucr = new com.smilecoms.commons.sca.PurchaseUnitCreditRequest();
        pucr.setProductInstanceId(pi.getProductInstanceId());
        pucr.setAccountId(si.getAccountId());
        pucr.setUnitCreditSpecificationId(ucid);
        pucr.setNumberToPurchase(1);
        pucr.setUniqueId("CAMP_" + lockedCampaignRun.getCampaignId() + "_" + pi.getProductInstanceId());
        com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().purchaseUnitCredit(pucr);
        ShortMessage sm1 = new ShortMessage();
        sm1.setFrom("256720100100");
        sm1.setTo(cust.getAlternativeContact1());
        sm1.setBody("Your " + ucName + " has been successfully added to account " + si.getAccountId());
        log.warn("Sending SMS");
        SCAWrapper.getAdminInstance().sendShortMessage(sm1);
        log.warn("Sent SMS");
        return "Done";
    }

    public String Inactive30DaysActionCodeTZ(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, com.smilecoms.commons.sca.Customer cust, String trigger, Logger log) throws Exception {

        if (siList.isEmpty()) {
            log.debug("Product instance has no services so won't continue processing");
            return "Done";
        }

        String campaignId = "Inactive30DaysTZ";
        CampaignBean cb = new CampaignBean(campaignId, pi.getProductInstanceId().intValue(), "NA");
        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        String accId = "" + si.getAccountId();
        String text = "We miss you! Recharge today with 5GB only Tsh42,500 on acc:" + accId + ". You'll GET 30GB FREE to use from MidNite to 6AM, valid 30 days. Smile #NowYouCan";

        String to = cust.getAlternativeContact1();
        log.debug("Sending [{}] campaign message to alternative contact 1: [{}]", campaignId, to);
        if (!cb.sendSMS("255662100100", to, text, campaignId)) {
            log.warn("Failed to send campaign message to [{}]", to);
        }

        to = (String) com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getProductInstancePhoneNumber(pi.getProductInstanceId().intValue());
        log.debug("Sending [{}] campaign message to Smile number: [{}]", campaignId, to);
        if (!cb.sendSMS("255662100100", to, text, campaignId)) {
            log.warn("Failed to send campaign message to [{}]", to);
        }

        return "Done";
    }

    public String Inactive40DaysActionCodeTZ(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, com.smilecoms.commons.sca.Customer cust, String trigger, Logger log) throws Exception {

        if (siList.isEmpty()) {
            log.debug("Product instance has no services so won't continue processing");
            return "Done";
        }

        String campaignId = "Inactive40DaysTZ";
        CampaignBean cb = new CampaignBean(campaignId, pi.getProductInstanceId().intValue(), "NA");
        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        String accId = "" + si.getAccountId();

        String text = "If you recharge today with 5GB at only Tsh42,500 on acc: " + accId + ". you'll GET 30GB FREE to use from MidNite to 6AM, valid 30 days. Smile #NowYouCan";

        String to = cust.getAlternativeContact1();
        log.debug("Sending [{}] campaign message to alternative contact 1: [{}]", campaignId, to);
        if (!cb.sendSMS("255662100100", to, text, campaignId)) {
            log.warn("Failed to send campaign message to [{}]", to);
        }

        to = (String) com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getProductInstancePhoneNumber(pi.getProductInstanceId().intValue());
        log.debug("Sending [{}] campaign message to Smile number: [{}]", campaignId, to);
        if (!cb.sendSMS("255662100100", to, text, campaignId)) {
            log.warn("Failed to send campaign message to [{}]", to);
        }

        return "Done";
    }

    public String Inactive60DaysActionCodeTZ(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, com.smilecoms.commons.sca.Customer cust, String trigger, Logger log) throws Exception {

        if (siList.isEmpty()) {
            log.debug("Product instance has no services so won't continue processing");
            return "Done";
        }

        String campaignId = "Inactive60DaysTZ";
        CampaignBean cb = new CampaignBean(campaignId, pi.getProductInstanceId().intValue(), "NA");
        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        String accId = "" + si.getAccountId();

        String text = "Did you know you'll get 30GB FREE to use from MidNite to 6AM, valid 30 days, when you recharge with 5GB at only Tsh42,500 on acc: " + accId + ". Smile #NowYouCan";

        String to = cust.getAlternativeContact1();
        log.debug("Sending [{}] campaign message to alternative contact 1: [{}]", campaignId, to);
        if (!cb.sendSMS("255662100100", to, text, campaignId)) {
            log.warn("Failed to send campaign message to [{}]", to);
        }

        to = (String) com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getProductInstancePhoneNumber(pi.getProductInstanceId().intValue());
        log.debug("Sending [{}] campaign message to Smile number: [{}]", campaignId, to);
        if (!cb.sendSMS("255662100100", to, text, campaignId)) {
            log.warn("Failed to send campaign message to [{}]", to);
        }

        return "Done";
    }

    public String Inactive80DaysActionCodeTZ(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, com.smilecoms.commons.sca.Customer cust, String trigger, Logger log) throws Exception {

        String campaignId = "Inactive80DaysTZ";
        CampaignBean cb = new CampaignBean(campaignId, pi.getProductInstanceId().intValue(), "NA");
        if (siList.isEmpty()) {
            log.debug("Product instance has no services so won't continue processing");
            return "Done";
        }
        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        String accId = "" + si.getAccountId();

        String text = "GET your FREE 30GB to use from MidNite to 6AM, valid 30 days, when you recharge today with 5GB at only Tsh42,500 on acc: " + accId + ". Smile #NowYouCan";

        String to = cust.getAlternativeContact1();
        log.debug("Sending [{}] campaign message to alternative contact 1: [{}]", campaignId, to);
        if (!cb.sendSMS("255662100100", to, text, campaignId)) {
            log.warn("Failed to send campaign message to [{}]", to);
        }

        to = (String) com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getProductInstancePhoneNumber(pi.getProductInstanceId().intValue());
        log.debug("Sending [{}] campaign message to Smile number: [{}]", campaignId, to);
        if (!cb.sendSMS("255662100100", to, text, campaignId)) {
            log.warn("Failed to send campaign message to [{}]", to);
        }

        return "Done";
    }

    public String NgEnrolMore4MoreCampaign(com.smilecoms.cm.db.model.Campaign campaign, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust, Logger log) throws Exception {

        if ((cust.getOptInLevel() & 4) == 0) {
            log.debug("Customers opt in  level exludes marketing so not sending campaign messages");
            return "Done";
        }

        String campaignId = "More4More";
        CampaignBean cb = new CampaignBean(campaignId, pi.getProductInstanceId().intValue(), "NA");

        String text = "Did you know that if you use your Smile device today, you qualify for a BONUS to use tomorrow from 6am to noon? More value! To Opt Out send STOP to 2442 (FREE)";

        String to = cust.getAlternativeContact1();
        log.debug("Sending [{}] campaign message to alternative contact 1: [{}]", campaignId, to);
        if (!cb.sendSMS("07020444444", to, text, campaignId)) {
            log.warn("Failed to send campaign message to [{}]", to);
        }

        to = (String) com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getProductInstancePhoneNumber(pi.getProductInstanceId().intValue());
        log.debug("Sending [{}] campaign message to Smile number: [{}]", campaignId, to);
        if (!cb.sendSMS("07020444444", to, text, campaignId)) {
            log.warn("Failed to send campaign message to [{}]", to);
        }

        return "Done";
    }

    public String NgGiveGBMore4More(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust, String triggerResult, Logger log) throws Exception {
        log.debug("processing giveGBMore4More");

        if (triggerResult == null) {
            return "Done";
        }

        String campaignId = "More4More";
        CampaignBean cb = new CampaignBean(campaignId, pi.getProductInstanceId().intValue(), "NA");

        double usage = Double.parseDouble(triggerResult);
        double firstThreshold = 536870912.0;
        double secondThreshold = 1073741824.0;
        int toProv = 0;

        String text = "";
        if (usage < firstThreshold) {
            text = "Use more than 500MB today and immediately get BONUS data for use tomorrow. More Value with Smile! Send STOP to 2442 (FREE) to Opt Out";
            if ((cust.getOptInLevel() & 4) == 0) {
                log.debug("Customers opt in level exludes marketing so not sending campaign messages");
                return "Done";
            }
        } else if (usage >= firstThreshold && usage < secondThreshold) {
            text = "Congratulations! You have 100MB Smile BONUS data to use today from 6am to noon because you used more than 500MB yesterday. Use more today and get more tomorrow.";
            toProv = 287;
        } else {
            text = "Congratulations! You have 200MB BONUS data to use today from 6am to noon because you used more than 1GB yesterday. Use more again today and get more tomorrow.";
            toProv = 288;
        }

        if (toProv > 0) {
            log.debug("Going to provision bonus bundle");
            String date = new java.text.SimpleDateFormat("dd-MM-yyyy").format(new Date());
            com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
            PurchaseUnitCreditRequest pucr = new PurchaseUnitCreditRequest();
            pucr.setProductInstanceId(pi.getProductInstanceId());
            pucr.setAccountId(si.getAccountId());
            pucr.setUnitCreditSpecificationId(toProv);
            pucr.setNumberToPurchase(1);
            pucr.setUniqueId("CAMP_" + lockedCampaignRun.getCampaignId() + "_" + pi.getProductInstanceId() + "_" + date);
            try {
                com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().purchaseUnitCredit(pucr);
            } catch (java.lang.Exception e) {
                log.warn("Error trying to provision bundle, most likely a duplicate");
                return "Done";
            }
        }

        if (!text.isEmpty()) {

            String to = cust.getAlternativeContact1();
            log.debug("Sending [{}] campaign message to alternative contact 1: [{}]", campaignId, to);
            if (!cb.sendSMS("07020444444", to, text, campaignId)) {
                log.warn("Failed to send campaign message to [{}]", to);
            }

            to = (String) com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getProductInstancePhoneNumber(pi.getProductInstanceId().intValue());
            log.debug("Sending [{}] campaign message to Smile number: [{}]", campaignId, to);
            if (!cb.sendSMS("07020444444", to, text, campaignId)) {
                log.warn("Failed to send campaign message to [{}]", to);
            }
        }

        return "Done";
    }

    public String NgGiveGBDormantWinBack3(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust, String triggerResult, Logger log) throws Exception {
        int s = Integer.parseInt(triggerResult);
        int toProv1 = 0;
        int toProv2 = 0;
        int amountToProvision1 = 1;
        int amountToProvision2 = 1;
        int validDays = 7;

        String campaignId = "DORMANT_WINBACK3";
        CampaignBean cb = new CampaignBean(campaignId, pi.getProductInstanceId().intValue(), "NA");
        int bonusBundleSpecId = 108;

        log.debug("Provisioning bonus on recharge bundle");
        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        if (!cb.provisionUnitCredit(pi.getProductInstanceId(), si.getAccountId(), bonusBundleSpecId, 1, "CAMP3_ACTION_12_" + campaignId + "_" + pi.getProductInstanceId() + "_" + bonusBundleSpecId)) {
            log.warn("Error trying to provision bundle, most likely a duplicate");
            return "Done";
        }

        String bonusAmount = "1GB";

        switch (s) {
            case 220:
                toProv1 = 289;
                bonusAmount = "1GB";
                break;
            case 280:
                toProv1 = 290;
                bonusAmount = "2GB";
                break;
            case 102:
                toProv1 = 291;
                bonusAmount = "3GB";
                break;
            case 150:
                toProv1 = 292;
                bonusAmount = "5GB";
                break;
            case 274:
                toProv1 = 302;
                bonusAmount = "7GB";
                break;
            case 103:
                toProv1 = 293;
                bonusAmount = "10GB";
                break;
            case 273:
                toProv1 = 294;
                bonusAmount = "15GB";
                break;
            case 104:
                toProv1 = 295;
                bonusAmount = "20GB";
                break;
            case 105:
                toProv1 = 295;
                bonusAmount = "50GB";
                break;
//            case 128:
//                toProv1 = 261;
//                bonusAmount = "100GB";
//                break;
//            case 129:
//                toProv1 = 262;
//                bonusAmount = "200GB";
//                break;
//            case 125:
//                toProv1 = 267;
//                validDays = 15;
//                bonusAmount = "5GB";
//                break;
//            case 126:
//                toProv1 = 268;
//                validDays = 15;
//                bonusAmount = "10GB";
//                break;
//            case 127:
//                toProv1 = 269;
//                validDays = 15;
//                bonusAmount = "20GB";
//                break;

            default:
                break;
        }

        if (toProv1 == 0) {
            return "Done";
        }

        if (!cb.provisionUnitCredit(pi.getProductInstanceId(), si.getAccountId(), toProv1, amountToProvision1, "CAMP3_ACTION_PROV1A" + lockedCampaignRun.getCampaignId() + "_" + pi.getProductInstanceId())) {
            log.warn("Error trying to provision bundle, most likely a duplicate");
            return "Done";
        }

        if (toProv2 != 0) {
            if (!cb.provisionUnitCredit(pi.getProductInstanceId(), si.getAccountId(), toProv2, amountToProvision2, "CAMP3_ACTION_PROV2A" + lockedCampaignRun.getCampaignId() + "_" + pi.getProductInstanceId())) {
                log.warn("Error trying to provision bundle, most likely a duplicate");
                return "Done";
            }
        }

        String text = "Welcome back to SuperFast internet! You have " + bonusAmount + "GB BONUS data, valid " + validDays + " days. Use your paid data first for BONUS data to kick in. Opt Out send STOP to 2442(FREE)";

        String to = cust.getAlternativeContact1();
        log.debug("Sending [{}] campaign message to alternative contact 1: [{}]", campaignId, to);
        if (!cb.sendSMS("07020444444", to, text, campaignId)) {
            log.warn("Failed to send campaign message to [{}]", to);
        }

        to = (String) com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getProductInstancePhoneNumber(pi.getProductInstanceId().intValue());
        log.debug("Sending [{}] campaign message to Smile number: [{}]", campaignId, to);
        if (!cb.sendSMS("07020444444", to, text, campaignId)) {
            log.warn("Failed to send campaign message to [{}]", to);
        }

        return "Done";
    }

    public String NGNotifyDormantWinBack3(com.smilecoms.cm.db.model.Campaign campaign, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust, Logger log) throws Exception {

        String campaignId = "DORMANT_WINBACK3";
        CampaignBean cb = new CampaignBean(campaignId, pi.getProductInstanceId().intValue(), "NA");

        int toProv = 108;
        log.debug("Going to provision gift bundle");
        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        if (!cb.provisionUnitCredit(pi.getProductInstanceId(), si.getAccountId(), toProv, 1, "CAMP3AAA_ENROL_" + campaignId + "_" + pi.getProductInstanceId() + "_" + toProv)) {
            log.warn("Error trying to provision bundle, most likely a duplicate");
            return "Done";
        }

        if ((cust.getOptInLevel() & 4) == 0) {
            log.debug("Customers opt in  level exludes marketing so not sending campaign messages");
            return "Done";
        }

        return "Done";
    }

    public String NgGiveUnderutilisedBtsRecharge(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust, String triggerResult, Logger log) throws Exception {
        int s = 0;
        try {
            s = Integer.parseInt(triggerResult);
        } catch (java.lang.NumberFormatException e) {
            return "Done";
        }

        String campaignId = "UNDERUTILISED_BTS_RECHARGE";
        CampaignBean cb = new CampaignBean(campaignId, pi.getProductInstanceId().intValue(), "NA");

        int toProv1 = 0;
        int toProv2 = 0;
        int amountToProvision1 = 1;
        int amountToProvision2 = 1;
        int validDays = 30;

        String bonusAmount = "1GB";

        switch (s) {
//            case 220:
//                toProv1 = 108;
//                bonusAmount = "1GB";
//                break;
//            case 280:
//                toProv1 = 258;
//                bonusAmount = "2GB";
//                break;
//            case 102:
//                toProv1 = 115;
//                bonusAmount = "3GB";
//                break;
            case 150:
                toProv1 = 115;
                bonusAmount = "3GB";
                break;
            case 274:
                toProv1 = 115;
                toProv2 = 108;
                bonusAmount = "4GB";
                break;
            case 103:
                toProv1 = 109;
                bonusAmount = "5GB";
                break;
            case 273:
                toProv1 = 115;
                toProv2 = 109;
                bonusAmount = "8GB";
                break;
            case 104:
                toProv1 = 110;
                bonusAmount = "10GB";
                break;
            case 105:
                toProv1 = 259;
                toProv2 = 109;
                bonusAmount = "25GB";
                break;
            case 128:
                toProv1 = 260;
                bonusAmount = "50GB";
                break;
            case 129:
                toProv1 = 261;
                bonusAmount = "100GB";
                break;
//            case 125:
//                toProv1 = 267;
//                validDays = 15;
//                bonusAmount = "5GB";
//                break;
//            case 126:
//                toProv1 = 268;
//                validDays = 15;
//                bonusAmount = "10GB";
//                break;
//            case 127:
//                toProv1 = 269;
//                validDays = 15;
//                bonusAmount = "20GB";
//                break;

            default:
                break;
        }

        if (toProv1 == 0) {
            return "Done";
        }

        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        PurchaseUnitCreditRequest pucr = new PurchaseUnitCreditRequest();
        pucr.setProductInstanceId(pi.getProductInstanceId());
        pucr.setAccountId(si.getAccountId());
        pucr.setUnitCreditSpecificationId(toProv1);
        pucr.setNumberToPurchase(amountToProvision1);
        pucr.setUniqueId("CAMP_" + lockedCampaignRun.getCampaignId() + "_" + pi.getProductInstanceId());
        try {
            com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().purchaseUnitCredit(pucr);
        } catch (java.lang.Exception e) {
            log.warn("Error trying to provision bundle, most likely a duplicate");
            return "Done";
        }

        if (toProv2 != 0) {
            si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
            pucr = new PurchaseUnitCreditRequest();
            pucr.setProductInstanceId(pi.getProductInstanceId());
            pucr.setAccountId(si.getAccountId());
            pucr.setUnitCreditSpecificationId(toProv2);
            pucr.setNumberToPurchase(amountToProvision2);
            pucr.setUniqueId("CAMP_" + lockedCampaignRun.getCampaignId() + "_" + pi.getProductInstanceId());
            try {
                com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().purchaseUnitCredit(pucr);
            } catch (java.lang.Exception e) {
                log.warn("Error trying to provision bundle, most likely a duplicate");
                return "Done";
            }
        }

        String text = "Your Smile recharge earned you a " + bonusAmount + "GB bonus, usable only after your paid bundle. Bonus GB expires in " + validDays + " days. Hurry, and enjoy your free data before it expires.";

        String to = cust.getAlternativeContact1();
        log.debug("Sending [{}] campaign message to alternative contact 1: [{}]", campaignId, to);
        if (!cb.sendSMS("07020444444", to, text, campaignId)) {
            log.warn("Failed to send campaign message to [{}]", to);
        }

        to = (String) com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getProductInstancePhoneNumber(pi.getProductInstanceId().intValue());
        log.debug("Sending [{}] campaign message to Smile number: [{}]", campaignId, to);
        if (!cb.sendSMS("07020444444", to, text, campaignId)) {
            log.warn("Failed to send campaign message to [{}]", to);
        }

        return "Done";
    }

    public String NgNotifyUnderutilisedBtsRecharge(com.smilecoms.cm.db.model.Campaign campaign, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust, Logger log) throws Exception {

        if ((cust.getOptInLevel() & 4) == 0) {
            log.debug("Customers opt in  level exludes marketing so not sending campaign messages");
            return "Done";
        }

        String campaignId = "UNDERUTILISED_BTS_RECHARGE";
        CampaignBean cb = new CampaignBean(campaignId, pi.getProductInstanceId().intValue(), "NA");

        String text = "Congratulations! Get 50% bonus on your next recharge, offer valid for 30 days. Visit www.smile.com.ng/recharge";

        String to = cust.getAlternativeContact1();
        log.debug("Sending [{}] campaign message to alternative contact 1: [{}]", campaignId, to);
        if (!cb.sendSMS("07020444444", to, text, campaignId)) {
            log.warn("Failed to send campaign message to [{}]", to);
        }

        to = (String) com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getProductInstancePhoneNumber(pi.getProductInstanceId().intValue());
        log.debug("Sending [{}] campaign message to Smile number: [{}]", campaignId, to);
        if (!cb.sendSMS("07020444444", to, text, campaignId)) {
            log.warn("Failed to send campaign message to [{}]", to);
        }
        return "Done";
    }

    public String NgActionBirthday(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust, String triggerResult, Logger log) throws Exception {
        if (pi.getProductSpecification().getProductSpecificationId().intValue() != 100) {
            log.debug("Currently only for staff PI 100, provided PI is [{}]", pi.getProductSpecification().getProductSpecificationId());
            return "Done";
        }

        String campaignId = "BIRTHDAY";
        CampaignBean cb = new CampaignBean(campaignId, pi.getProductInstanceId().intValue(), "NA");

        String[] messageList = {"Smile Wishes you a happy birthday. Thank you for being a part of the Smile Family where everything is possible, even your hearts desires! Smile, #NowYouCan",
            "Smile Celebrates your special day with you, Happy Birthday!",
            "An Awesome Birthday wish to an even Awesome-r colleague! We would give you load of gifts but you already have your staff bundle! Smile, you know you want to!",
            "A birthday for a special person! Now get done with work and take us out to lunch! You truly deserve the best " + cust.getFirstName() + "; Happy birthday from your Smile Family!",
            "Today is no ordinary day, today is a birthday of a great person and very valuable employee, happy birthday to you!",
            "Like fine wine " + cust.getFirstName() + ", we hope you get better year after year! Happy Birthday from your Smile Family! #NowYouCan",
            "Happy Birthday to you " + cust.getFirstName() + ". Here's wishing you a great year ahead. May the most fulfilling of your dreams come true. Smile #NowYouCan",
            "Today is no ordinary day! It's your birthday " + cust.getFirstName() + ", and here at Smile, it's considered a celebration. Happy birthday to you!",
            "More than Ice cream, cakes and presents wrapped in satin sashes, your friends at Smile want to wish you a very happy birthday. #NowYouCan make a wish!",
            "Here at Smile, your birthday calls for a celebration! Buckle up and get ready for the best birthday you'd have this year! Smile, #NowYouCan"
        };

        String text = messageList[new java.util.Random().nextInt(messageList.length)];
        log.debug("Selected text to send: [{}]", text);

        String to = cust.getAlternativeContact1();
        log.debug("Sending [{}] campaign message to alternative contact 1: [{}]", campaignId, to);
        if (!cb.sendSMS("07020444444", to, text, campaignId)) {
            log.warn("Failed to send campaign message to [{}]", to);
        }

        to = (String) com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getProductInstancePhoneNumber(pi.getProductInstanceId().intValue());
        log.debug("Sending [{}] campaign message to Smile number: [{}]", campaignId, to);
        if (!cb.sendSMS("07020444444", to, text, campaignId)) {
            log.warn("Failed to send campaign message to [{}]", to);
        }

        int toProv = 335;
        log.debug("Going to provision birthday gift bundle");
        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        if (!cb.provisionUnitCredit(pi.getProductInstanceId(), si.getAccountId(), toProv, 1, "BIRTHDAY_GIFT_" + campaignId + "_" + pi.getProductInstanceId() + "_" + toProv)) {
            log.warn("Error trying to provision birthday bundle, most likely a duplicate");
            return "Done";
        }

        return "Done";
    }

    public String NgActionCustomerBirthday(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust, String triggerResult, Logger log) throws Exception {
        if (pi.getProductSpecification().getProductSpecificationId().intValue() == 100) {
            log.debug("This campaign excludes staff PIs. [{}]", pi.getProductSpecification().getProductSpecificationId());
            return "Done";
        }

        String campaignId = "BIRTHDAY";
        CampaignBean cb = new CampaignBean(campaignId, pi.getProductInstanceId().intValue(), "NA");

        try {
            int toProv = 335;
            log.debug("Going to provision birthday gift bundle");
            com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
            if (!cb.provisionUnitCredit(pi.getProductInstanceId(), si.getAccountId(), toProv, 1, "BIRTHDAY_GIFT_" + campaignId + "_" + pi.getProductInstanceId() + "_" + toProv)) {
                log.warn("Error trying to provision birthday bundle, most likely a duplicate");
                return "Done";
            }
        } catch (java.lang.Exception e) {
            log.warn("Error trying to provision birthfay bundle, most likely a duplicate");
            return "Done";
        }
        return "Done";
    }

    public String NgActionOnReconnection(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust, String triggerResult, Logger log) throws Exception {

        String campaignId = "RECONNECTION";
        CampaignBean cb = new CampaignBean(campaignId, pi.getProductInstanceId().intValue(), "NA");

        String text = "Thank you for reconnecting with Smile! We are glad to have you back.. Please share your thoughts with us - customercare@smile.com.ng or call us 07020444444";
        log.debug("Text to send: [{}]", text);

        String to = cust.getAlternativeContact1();
        log.debug("Sending [{}] campaign message to alternative contact 1: [{}]", campaignId, to);
        if (!cb.sendSMS("07020444444", to, text, campaignId)) {
            log.warn("Failed to send campaign message to [{}]", to);
        }

        to = (String) com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getProductInstancePhoneNumber(pi.getProductInstanceId().intValue());
        log.debug("Sending [{}] campaign message to Smile number: [{}]", campaignId, to);
        if (!cb.sendSMS("07020444444", to, text, campaignId)) {
            log.warn("Failed to send campaign message to [{}]", to);
        }
        return "Done";
    }

    public String NgEnrolUsageGroup1(com.smilecoms.cm.db.model.Campaign campaign, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust, Logger log) throws Exception {

        if ((cust.getOptInLevel() & 4) == 0) {
            log.debug("Customers opt in  level exludes marketing so not sending campaign messages");
            return "Done";
        }

        String campaignId = "USAGE_CAMPAIGN_GROUP1";
        CampaignBean cb = new CampaignBean(campaignId, pi.getProductInstanceId().intValue(), "NA");

        String text = "A limited offer just for you! Get up to 50% discount on the 7GB data plan. To recharge, visit smile.com.ng/mysmile or please call 07020444444. Smile #NowYouCan";

        String to = cust.getAlternativeContact1();
        log.debug("Sending [{}] campaign message to alternative contact 1: [{}]", campaignId, to);
        if (!cb.sendSMS("07020444444", to, text, campaignId)) {
            log.warn("Failed to send campaign message to [{}]", to);
        }

        to = (String) com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getProductInstancePhoneNumber(pi.getProductInstanceId().intValue());
        log.debug("Sending [{}] campaign message to Smile number: [{}]", campaignId, to);
        if (!cb.sendSMS("07020444444", to, text, campaignId)) {
            log.warn("Failed to send campaign message to [{}]", to);
        }
        return "Done";
    }

    public String NgEnrolUsageGroup2(com.smilecoms.cm.db.model.Campaign campaign, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust, Logger log) throws Exception {

        if ((cust.getOptInLevel() & 4) == 0) {
            log.debug("Customers opt in  level exludes marketing so not sending campaign messages");
            return "Done";
        }

        String campaignId = "USAGE_CAMPAIGN_GROUP2";
        CampaignBean cb = new CampaignBean(campaignId, pi.getProductInstanceId().intValue(), "NA");

        String text = "A limited offer just for you! Get up to 50% discount on the 15GB data plan. To recharge visit smile.com.ng/mysmile or please call 07020444444. Smile #NowYouCan";

        String to = cust.getAlternativeContact1();
        log.debug("Sending [{}] campaign message to alternative contact 1: [{}]", campaignId, to);
        if (!cb.sendSMS("07020444444", to, text, campaignId)) {
            log.warn("Failed to send campaign message to [{}]", to);
        }

        to = (String) com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getProductInstancePhoneNumber(pi.getProductInstanceId().intValue());
        log.debug("Sending [{}] campaign message to Smile number: [{}]", campaignId, to);
        if (!cb.sendSMS("07020444444", to, text, campaignId)) {
            log.warn("Failed to send campaign message to [{}]", to);
        }
        return "Done";
    }

    public String NgDoubleDataWeekendPromo(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust, String triggerResult, Logger log) throws Exception {
        int s = Integer.parseInt(triggerResult);
        int toProv1 = 0;
        int toProv2 = 0;
        int amountToProvision1 = 1;
        int amountToProvision2 = 1;

        String campaignId = "DOUBLEDATA_WEEKEND_OFFER";
        CampaignBean cb = new CampaignBean(campaignId, pi.getProductInstanceId().intValue(), "NA");
        int bonusBundleSpecId = 347; // The bonus configuration bundle.

        log.debug("Provisioning bonus on recharge bundle");
        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        if (!cb.provisionUnitCredit(pi.getProductInstanceId(), si.getAccountId(), bonusBundleSpecId, 1, "DOUBLEDATA_WEEKEND_OFFER_" + campaignId + "_" + pi.getProductInstanceId() + "_" + bonusBundleSpecId)) {
            log.warn("Error trying to provision bundle, most likely a duplicate");
            return "Done";
        }

        String bonusAmount = "1GB";

        switch (s) {
            case 220:
                toProv1 = 108;
                bonusAmount = "1GB";
                break;
            case 280:
                toProv1 = 258;
                bonusAmount = "2GB";
                break;
            case 102:
                toProv1 = 115;
                bonusAmount = "3GB";
                break;
            case 274:
                toProv1 = 109;
                toProv2 = 258;
                bonusAmount = "7GB";
                break;
            case 273:
                toProv1 = 109;
                amountToProvision1 = 3;
                bonusAmount = "15GB";
                break;
            default:
                break;
        }

        if (toProv1 == 0) {
            return "Done";
        }

        if (!cb.provisionUnitCredit(pi.getProductInstanceId(), si.getAccountId(), toProv1, amountToProvision1, "DOUBLEDATA_WEEKEND_OFFER_" + lockedCampaignRun.getCampaignId() + "_" + pi.getProductInstanceId())) {
            log.warn("Error trying to provision bundle, most likely a duplicate");
            return "Done";
        }

        if (toProv2 != 0) {
            if (!cb.provisionUnitCredit(pi.getProductInstanceId(), si.getAccountId(), toProv2, amountToProvision2, "DOUBLEDATA_WEEKEND_OFFER_" + lockedCampaignRun.getCampaignId() + "_" + pi.getProductInstanceId())) {
                log.warn("Error trying to provision bundle, most likely a duplicate");
                return "Done";
            }
        }

        return "Done";
    }

    public static String NgUCUsageGroup1(javax.persistence.EntityManager em, Logger log, Set ucSpecIds, com.smilecoms.cm.db.model.ProductInstance pi, com.smilecoms.cm.db.model.Campaign dbCampaign) {
        log.debug("NgUCUsageGroup1: Added UCS: 329, 330");
        ucSpecIds.add(new Integer(329));
        ucSpecIds.add(new Integer(330));
        return "Done";
    }

    public static String NgUCUsageGroup2(javax.persistence.EntityManager em, Logger log, Set ucSpecIds, com.smilecoms.cm.db.model.ProductInstance pi, com.smilecoms.cm.db.model.Campaign dbCampaign) {
        log.debug("NgUCUsageGroup2: Added UCS: 331, 332, 333");
        ucSpecIds.add(new Integer(331));
        ucSpecIds.add(new Integer(332));
        ucSpecIds.add(new Integer(333));
        return "Done";
    }

    public static String processStandardEnrol(com.smilecoms.cm.db.model.Campaign campaign,
            List siList,
            Customer customer,
            com.smilecoms.cm.EnrolResult enrolResult,
            Logger log) {

        CampaignBean cb = new CampaignBean(
                campaign.getCampaignPK().getName(),
                enrolResult.productInstanceId,
                campaign.getCampaignPK().getName() + "-" + String.valueOf(enrolResult.productInstanceId));

        if (enrolResult.productInstanceId > 0
                && enrolResult.runType != null
                && enrolResult.smsFrom != null
                && enrolResult.smsTo != null
                && enrolResult.smsMsg != null
                && !enrolResult.smsMsg.isEmpty()) {
            String testTo = Utils.getValueFromCRDelimitedAVPString(campaign.getActionConfig(), "TestTo");
            String smsTo = enrolResult.smsTo;
            if (testTo != null && !testTo.isEmpty()) {
                smsTo = testTo;
                log.debug("This is a test campaign. SMS will go to [{}]", smsTo);
            }

            log.warn("Sending processStandardEnrol sms [{}] from [{}] to [{}] for PI [{}]",
                    new Object[]{enrolResult.smsMsg, enrolResult.smsFrom, smsTo, String.valueOf(enrolResult.productInstanceId)});
            cb.sendSMS(enrolResult.smsFrom, smsTo, enrolResult.smsMsg, "Enrol-" + String.valueOf(campaign.getCampaignPK().getCampaignId()));
        }
              
        
        if (enrolResult.productInstanceId > 0
                && enrolResult.runType != null
                && enrolResult.emailFrom != null
                && enrolResult.emailTo != null
                && enrolResult.emailTemplate != null
                && !enrolResult.emailTemplate.isEmpty()) {

            String prefix = "customer.campaigns." + enrolResult.emailTemplate.toLowerCase();
            log.debug("Getting resources for prefix [{}]", prefix);

            Locale locale = com.smilecoms.commons.localisation.LocalisationHelper.getLocaleForLanguage(customer.getLanguage());

            String xsltEmailSubject = com.smilecoms.commons.localisation.LocalisationHelper.getLocalisedString(locale, prefix + ".email.subject");
            String xsltEmailBody = com.smilecoms.commons.localisation.LocalisationHelper.getLocalisedString(locale, prefix + ".email.body");

            
            
            if (!xsltEmailSubject.startsWith("?") && !xsltEmailBody.startsWith("?")) {
                cb.sendEmail(enrolResult.smsFrom, enrolResult.emailTo, xsltEmailSubject, xsltEmailBody);
            } else {
                log.error("This event has no associated email");
            }
        }

        return "Done";
    }

    public static String processStandardActionOld(
            com.smilecoms.cm.db.model.Campaign campaign,
            com.smilecoms.cm.db.model.CampaignRun campaignRun,
            List siList,
            Customer customer,
            com.smilecoms.cm.TriggerResult triggerResult,
            Logger log) {

        CampaignBean cb = new CampaignBean(
                campaign.getCampaignPK().getName(),
                triggerResult.productInstanceId,
                campaign.getCampaignPK().getName() + "-" + String.valueOf(triggerResult.productInstanceId));
        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        try {
            String testTo = Utils.getValueFromCRDelimitedAVPString(campaign.getActionConfig(), "TestTo");
            if (testTo != null && !testTo.isEmpty()) {
                log.debug("This is a test campaign. SMS will go to [{}] and bundles wont be provisioned", testTo);
            }

            if (triggerResult.ucSpecIdToProvision > 0) {

                if (testTo == null || testTo.isEmpty()) {
                    log.warn("Provisioning UC [{}] on PI [{}] Account [{}]",
                            new Object[]{String.valueOf(triggerResult.ucSpecIdToProvision), String.valueOf(triggerResult.productInstanceId), String.valueOf(si.getAccountId())});
                    cb.provisionUnitCredit(new Integer(triggerResult.productInstanceId), si.getAccountId(), triggerResult.ucSpecIdToProvision, 1,
                            "Camp-" + String.valueOf(campaign.getCampaignPK().getCampaignId()) + "-" + String.valueOf(campaignRun.getCampaignRunId()) + "-" + triggerResult.triggerResultString);
                } else if (testTo.contains("@")) {
                    cb.sendEmail(triggerResult.smsFrom, testTo, "Campaign UC Provisioning",
                            "Would have provisioned UC Id " + String.valueOf(triggerResult.ucSpecIdToProvision) + " on product instance " + String.valueOf(triggerResult.productInstanceId)
                            + " on account " + String.valueOf(si.getAccountId()) + " as part of campaign " + campaign.getCampaignPK().getName());
                }
            }

            if (triggerResult.smsFrom != null
                    && triggerResult.smsTo != null
                    && triggerResult.smsMsg != null
                    && !triggerResult.smsMsg.isEmpty()) {
                String smsTo = triggerResult.smsTo;
                if (testTo != null && !testTo.isEmpty()) {
                    smsTo = testTo;
                }
                log.warn("Sending processStandardAction sms [{}] from [{}] to [{}] for PI [{}]",
                        new Object[]{triggerResult.smsMsg, triggerResult.smsFrom, smsTo, String.valueOf(triggerResult.productInstanceId)});
                cb.sendSMS(triggerResult.smsFrom, smsTo, triggerResult.smsMsg, "Trigger-" + String.valueOf(campaign.getCampaignPK().getCampaignId()));
            }
        } catch (Exception e) {
            log.warn("Error provisioning campaign UC: ", e);
        }
        return "Done";
    }

    public static String processStandardAction(
            com.smilecoms.cm.db.model.Campaign campaign,
            com.smilecoms.cm.db.model.CampaignRun campaignRun,
            List siList,
            Customer customer,
            com.smilecoms.cm.TriggerResult triggerResult,
            Logger log) {

        log.warn("XXXXXXXXX in processStandardAction");
        CampaignBean cb = new CampaignBean(
                campaign.getCampaignPK().getName(),
                triggerResult.productInstanceId,
                campaign.getCampaignPK().getName() + "-" + String.valueOf(triggerResult.productInstanceId));
        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        try {
            String testTo = Utils.getValueFromCRDelimitedAVPString(campaign.getActionConfig(), "TestTo");
            if (testTo != null && !testTo.isEmpty()) {
                log.debug("This is a test campaign. SMS will go to [{}] and bundles wont be provisioned", testTo);
            }

            if (triggerResult.ucSpecIdToProvision > 0) {

                if (testTo == null || testTo.isEmpty()) {
                    log.warn("Provisioning UC [{}] on PI [{}] Account [{}]",
                            new Object[]{String.valueOf(triggerResult.ucSpecIdToProvision), String.valueOf(triggerResult.productInstanceId), String.valueOf(si.getAccountId())});
                    cb.provisionUnitCredit(new Integer(triggerResult.productInstanceId), si.getAccountId(), triggerResult.ucSpecIdToProvision, 1,
                            "Camp-" + String.valueOf(campaign.getCampaignPK().getCampaignId()) + "-" + String.valueOf(campaignRun.getCampaignRunId()) + "-" + triggerResult.triggerResultString);
                } else if (testTo.contains("@")) {
                    cb.sendEmail(triggerResult.smsFrom, testTo, "Campaign UC Provisioning",
                            "Would have provisioned UC Id " + String.valueOf(triggerResult.ucSpecIdToProvision) + " on product instance " + String.valueOf(triggerResult.productInstanceId)
                            + " on account " + String.valueOf(si.getAccountId()) + " as part of campaign " + campaign.getCampaignPK().getName());
                }
            } else if (triggerResult.ucSpecIdsToProvision != null && !triggerResult.ucSpecIdsToProvision.isEmpty()) {
                for (int i = 0; i < triggerResult.ucSpecIdsToProvision.size(); i++) {
                    int specId = ((Integer) triggerResult.ucSpecIdsToProvision.get(i)).intValue();
                    if (testTo == null || testTo.isEmpty()) {
                        log.warn("Provisioning UC [{}] on PI [{}] Account [{}]",
                                new Object[]{String.valueOf(specId), String.valueOf(triggerResult.productInstanceId), String.valueOf(si.getAccountId())});
                        cb.provisionUnitCredit(new Integer(triggerResult.productInstanceId), si.getAccountId(), specId, 1,
                                "Camp-" + String.valueOf(campaign.getCampaignPK().getCampaignId()) + "-" + String.valueOf(campaignRun.getCampaignRunId()) + "-" + triggerResult.triggerResultString + "-" + specId);
                    } else if (testTo.contains("@")) {
                        cb.sendEmail(triggerResult.smsFrom, testTo, "Campaign UC Provisioning",
                                "Would have provisioned UC Id " + String.valueOf(specId) + " on product instance " + String.valueOf(triggerResult.productInstanceId)
                                + " on account " + String.valueOf(si.getAccountId()) + " as part of campaign " + campaign.getCampaignPK().getName());
                    }
                }
            }

            if (triggerResult.smsFrom != null
                    && triggerResult.smsTo != null
                    && triggerResult.smsMsg != null
                    && !triggerResult.smsMsg.isEmpty()) {
                String smsTo = triggerResult.smsTo;
                if (testTo != null && !testTo.isEmpty()) {
                    smsTo = testTo;
                }
                log.warn("Sending processStandardAction sms [{}] from [{}] to [{}] for PI [{}]",
                        new Object[]{triggerResult.smsMsg, triggerResult.smsFrom, smsTo, String.valueOf(triggerResult.productInstanceId)});
                cb.sendSMS(triggerResult.smsFrom, smsTo, triggerResult.smsMsg, "Trigger-" + String.valueOf(campaign.getCampaignPK().getCampaignId()));
            }
        } catch (Exception e) {
            log.warn("Error provisioning campaign UC: ", e);
        }
        return "Done";
    }

     
    public static String processStandardActionNew(
            com.smilecoms.cm.db.model.Campaign campaign,
            com.smilecoms.cm.db.model.CampaignRun campaignRun,
            List siList,
            Customer customer,
            com.smilecoms.cm.TriggerResult triggerResult,
            Logger log) {

        log.warn("XXXXXXXXX in processStandardAction");
        CampaignBean cb = new CampaignBean(
                campaign.getCampaignPK().getName(),
                triggerResult.productInstanceId,
                campaign.getCampaignPK().getName() + "-" + String.valueOf(triggerResult.productInstanceId));
        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        try {
            String testTo = Utils.getValueFromCRDelimitedAVPString(campaign.getActionConfig(), "TestTo");
            if (testTo != null && !testTo.isEmpty()) {
                log.debug("This is a test campaign. SMS will go to [{}] and bundles wont be provisioned", testTo);
            }

            if (triggerResult.ucSpecIdToProvision > 0) {

                if (testTo == null || testTo.isEmpty()) {
                    log.warn("Provisioning UC [{}] on PI [{}] Account [{}]",
                            new Object[]{String.valueOf(triggerResult.ucSpecIdToProvision), String.valueOf(triggerResult.productInstanceId), String.valueOf(si.getAccountId())});
                    cb.provisionUnitCredit(new Integer(triggerResult.productInstanceId), si.getAccountId(), triggerResult.ucSpecIdToProvision, 1,
                            "Camp-" + String.valueOf(campaign.getCampaignPK().getCampaignId()) + "-" + String.valueOf(campaignRun.getCampaignRunId()) + "-" + triggerResult.triggerResultString);
                } else if (testTo.contains("@")) {
                    cb.sendEmail(triggerResult.smsFrom, testTo, "Campaign UC Provisioning",
                            "Would have provisioned UC Id " + String.valueOf(triggerResult.ucSpecIdToProvision) + " on product instance " + String.valueOf(triggerResult.productInstanceId)
                            + " on account " + String.valueOf(si.getAccountId()) + " as part of campaign " + campaign.getCampaignPK().getName());
                }
            } else if (triggerResult.ucSpecIdsToProvision != null && !triggerResult.ucSpecIdsToProvision.isEmpty()) {
                for (int i = 0; i < triggerResult.ucSpecIdsToProvision.size(); i++) {
                    int specId = ((Integer) triggerResult.ucSpecIdsToProvision.get(i)).intValue();
                    if (testTo == null || testTo.isEmpty()) {
                        log.warn("Provisioning UC [{}] on PI [{}] Account [{}]",
                                new Object[]{String.valueOf(specId), String.valueOf(triggerResult.productInstanceId), String.valueOf(si.getAccountId())});
                        cb.provisionUnitCredit(new Integer(triggerResult.productInstanceId), si.getAccountId(), specId, 1,
                                "Camp-" + String.valueOf(campaign.getCampaignPK().getCampaignId()) + "-" + String.valueOf(campaignRun.getCampaignRunId()) + "-" + triggerResult.triggerResultString + "-" + specId);
                    } else if (testTo.contains("@")) {
                        cb.sendEmail(triggerResult.smsFrom, testTo, "Campaign UC Provisioning",
                                "Would have provisioned UC Id " + String.valueOf(specId) + " on product instance " + String.valueOf(triggerResult.productInstanceId)
                                + " on account " + String.valueOf(si.getAccountId()) + " as part of campaign " + campaign.getCampaignPK().getName());
                    }
                }
            }

            if (triggerResult.smsFrom != null
                    && triggerResult.smsTo != null
                    && triggerResult.smsMsg != null
                    && !triggerResult.smsMsg.isEmpty()) {
                String smsTo = triggerResult.smsTo;
                if (testTo != null && !testTo.isEmpty()) {
                    smsTo = testTo;
                }
                log.warn("Sending processStandardAction sms [{}] from [{}] to [{}] for PI [{}]",
                        new Object[]{triggerResult.smsMsg, triggerResult.smsFrom, smsTo, String.valueOf(triggerResult.productInstanceId)});
                cb.sendSMS(triggerResult.smsFrom, smsTo, triggerResult.smsMsg, "Trigger-" + String.valueOf(campaign.getCampaignPK().getCampaignId()));
            }
            
            if (triggerResult.emailFrom != null
                && triggerResult.emailTo != null
                && triggerResult.emailTemplate != null
                && !triggerResult.emailTemplate.isEmpty()) {

                String prefix = "customer.campaigns." + triggerResult.emailTemplate.toLowerCase();
                log.debug("Getting resources for prefix [{}]", prefix);

                Locale locale = com.smilecoms.commons.localisation.LocalisationHelper.getLocaleForLanguage(customer.getLanguage());

                String xsltEmailSubject = com.smilecoms.commons.localisation.LocalisationHelper.getLocalisedString(locale, prefix + ".email.subject");
                String xsltEmailBody = com.smilecoms.commons.localisation.LocalisationHelper.getLocalisedString(locale, prefix + ".email.body");



                if (!xsltEmailSubject.startsWith("?") && !xsltEmailBody.startsWith("?")) {
                    cb.sendEmail(triggerResult.smsFrom, triggerResult.emailTo, xsltEmailSubject, xsltEmailBody);
                } else {
                    log.error("This event has no associated email");
                }
            }
            
        } catch (Exception e) {
            log.warn("Error provisioning campaign UC: ", e);
        }
        return "Done";
    }
    
    public String TzEnrolChristmasOffer(com.smilecoms.cm.db.model.Campaign campaign, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust, Logger log) throws Exception {

        String campaignId = "CHRISTMAS_OFFER";
        CampaignBean cb = new CampaignBean(campaignId, pi.getProductInstanceId().intValue(), "NA");
        int bonusBundleSpecId = 305; // The bonus configuration bundle.

        log.debug("Provisioning bonus on recharge bundle");
        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        if (!cb.provisionUnitCredit(pi.getProductInstanceId(), si.getAccountId(), bonusBundleSpecId, 1, "CHRISTMAS_OFFER" + campaignId + "_" + pi.getProductInstanceId() + "_" + bonusBundleSpecId)) {
            log.warn("Error trying to provision bundle, most likely a duplicate");
            return "Done";
        }

        if ((cust.getOptInLevel() & 4) == 0) {
            log.debug("Customers opt in  level exludes marketing so not sending campaign messages");
            return "Done";
        }

        String text = "Welcome to the best 4G LTE service in Tanzania. Remember when you recharge with Anytime data you get BONUS data for all recharges over 3 months. Enjoy! Smile";

        String to = cust.getAlternativeContact1();
        log.debug("Sending [{}] campaign message to alternative contact 1: [{}]", campaignId, to);
        if (!cb.sendSMS("0662100100", to, text, campaignId)) {
            log.warn("Failed to send campaign message to [{}]", to);
        }

        to = (String) com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getProductInstancePhoneNumber(pi.getProductInstanceId().intValue());
        log.debug("Sending [{}] campaign message to Smile number: [{}]", campaignId, to);

        if (!cb.sendSMS("0662100100", to, text, campaignId)) {
            log.warn("Failed to send campaign message to [{}]", to);
        }
        return "Done";
    }

    public static String NgAddONOffers(javax.persistence.EntityManager em, Logger log, Set ucSpecIds, com.smilecoms.cm.db.model.ProductInstance pi, com.smilecoms.cm.db.model.Campaign dbCampaign) {

        javax.persistence.Query q = em.createNativeQuery("select count(*) as NumExistingUCIs, 0 as nothing "
                + "from (select account_id from service_instance where product_instance_id=? limit 1) as AC join unit_credit_instance UC_PURCHASED "
                + "on (UC_PURCHASED.unit_credit_specification_id in (220,280,102,274,273) and UC_PURCHASED.ACCOUNT_ID=AC.account_id and UC_PURCHASED.EXPIRY_DATE >  now());");

        q.setParameter(1, pi.getProductInstanceId());

        java.util.List lst = q.getResultList();

        Object row[] = (Object[]) lst.get(0);

        // java.math.BigDecimal count330 = (java.math.BigDecimal) row[0];
        Long count330 = (Long) row[0];

        if (count330.longValue() > 0) { // Product has an active monthly bundle of any type (220,280,102,274,273)
            ucSpecIds.add(new Integer(369));
            ucSpecIds.add(new Integer(370));
            ucSpecIds.add(new Integer(371));
            ucSpecIds.add(new Integer(372));
            ucSpecIds.add(new Integer(373));
            log.warn("NgAddONOffers: Added AddON UCSs: 369, 370, 371, 372 and 373");
        } else {
            log.warn("NgAddONOffers: Product Instance  " + pi.getProductInstanceId() + " does not qualify for AddONs - Count = " + count330);
        }

        return "Done";
    }

    public static String processStandardAction1(
            com.smilecoms.cm.db.model.Campaign campaign,
            com.smilecoms.cm.db.model.CampaignRun campaignRun,
            List siList,
            Customer customer,
            com.smilecoms.cm.TriggerResult triggerResult,
            Logger log) {

        CampaignBean cb = new CampaignBean(
                campaign.getCampaignPK().getName(),
                triggerResult.productInstanceId,
                campaign.getCampaignPK().getName() + "-" + String.valueOf(triggerResult.productInstanceId));
        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        try {
            String testTo = Utils.getValueFromCRDelimitedAVPString(campaign.getActionConfig(), "TestTo");
            if (testTo != null && !testTo.isEmpty()) {
                log.debug("This is a test campaign. SMS will go to [{}] and bundles wont be provisioned", testTo);
            }

            if (triggerResult.ucSpecIdToProvision > 0) {

                if (testTo == null || testTo.isEmpty()) {
                    log.warn("Provisioning UC [{}] on PI [{}] Account [{}]",
                            new Object[]{String.valueOf(triggerResult.ucSpecIdToProvision), String.valueOf(triggerResult.productInstanceId), String.valueOf(si.getAccountId())});
                    cb.provisionUnitCredit(new Integer(triggerResult.productInstanceId), si.getAccountId(), triggerResult.ucSpecIdToProvision, 1,
                            "Camp-" + String.valueOf(campaign.getCampaignPK().getCampaignId()) + "-" + String.valueOf(campaignRun.getCampaignRunId()) + "-" + triggerResult.triggerResultString);
                } else if (testTo.contains("@")) {
                    cb.sendEmail(triggerResult.smsFrom, testTo, "Campaign UC Provisioning",
                            "Would have provisioned UC Id " + String.valueOf(triggerResult.ucSpecIdToProvision) + " on product instance " + String.valueOf(triggerResult.productInstanceId)
                            + " on account " + String.valueOf(si.getAccountId()) + " as part of campaign " + campaign.getCampaignPK().getName());
                }
            }

            if (triggerResult.smsFrom != null
                    && triggerResult.smsTo != null
                    && triggerResult.smsMsg != null
                    && !triggerResult.smsMsg.isEmpty()) {
                String smsTo = triggerResult.smsTo;
                if (testTo != null && !testTo.isEmpty()) {
                    smsTo = testTo;
                }
                log.warn("Sending processStandardAction sms [{}] from [{}] to [{}] for PI [{}]",
                        new Object[]{triggerResult.smsMsg, triggerResult.smsFrom, smsTo, String.valueOf(triggerResult.productInstanceId)});
                cb.sendSMS(triggerResult.smsFrom, smsTo, triggerResult.smsMsg, "Trigger-" + String.valueOf(campaign.getCampaignPK().getCampaignId()));
            }
        } catch (Exception e) {
            log.warn("Error provisioning campaign UC: ", e);
        }
        return "Done";
    }

    public static String NgAddONOffers222(javax.persistence.EntityManager em, Logger log, Set ucSpecIds, com.smilecoms.cm.db.model.ProductInstance pi, com.smilecoms.cm.db.model.Campaign dbCampaign) {

        javax.persistence.Query q = em.createNativeQuery("select count(*) as NumExistingUCIs, 0 as nothing "
                + "from (select account_id from service_instance where product_instance_id=? limit 1) as AC join unit_credit_instance UC_PURCHASED "
                + "on (UC_PURCHASED.unit_credit_specification_id in (220,280,102,274,273) and UC_PURCHASED.ACCOUNT_ID=AC.account_id and UC_PURCHASED.EXPIRY_DATE >  now());");

        q.setParameter(1, pi.getProductInstanceId());

        java.util.List lst = q.getResultList();

        Object row[] = (Object[]) lst.get(0);

        //  java.math.BigDecimal count330 = (java.math.BigDecimal) row[0];
        Long count330 = (Long) row[0];

        if (count330.longValue() > 0) { // Product has an active monthly bundle of any type (220,280,102,274,273)
            ucSpecIds.add(new Integer(369));
            ucSpecIds.add(new Integer(370));
            ucSpecIds.add(new Integer(371));
            ucSpecIds.add(new Integer(372));
            ucSpecIds.add(new Integer(373));
            log.warn("NgAddONOffers: Added AddON UCSs: 369, 370, 371, 372 and 373");
        } else {
            log.warn("NgAddONOffers: Product Instance  " + pi.getProductInstanceId() + " does not qualify for AddONs - Count = " + count330);
        }

        return "Done";
    }

    public static String NgAddONOffersFFF(javax.persistence.EntityManager em, Logger log, Set ucSpecIds, com.smilecoms.cm.db.model.ProductInstance pi, com.smilecoms.cm.db.model.Campaign dbCampaign) {

        javax.persistence.Query q = em.createNativeQuery("select count(*) as NumExistingUCIs, 0 as nothing "
                + " from (select account_id,CREATED_DATE_TIME as ENROLMENT_DATE "
                + " from service_instance SI join campaign_run CR on (SI.product_instance_id=? and SI.product_instance_id=CR.product_instance_id and CR.campaign_id=74) limit 1 "
                + " ) as AC join unit_credit_instance UC_PURCHASED "
                + " on (UC_PURCHASED.unit_credit_specification_id in (220,280,102,274,273) and UC_PURCHASED.ACCOUNT_ID=AC.account_id and UC_PURCHASED.EXPIRY_DATE >  now()) "
                + " left join unit_credit_instance ANY_30_DAY_PLANS_PURCHASED on (AC.account_id = ANY_30_DAY_PLANS_PURCHASED.account_id and "
                + " ANY_30_DAY_PLANS_PURCHASED.unit_credit_specification_id in (102,103,104,105,125,126,127,128,129,220,232,233,273,274,280,356,357,358,359,360) "
                + " and ANY_30_DAY_PLANS_PURCHASED.purchase_date > AC.ENROLMENT_DATE) "
                + " where ANY_30_DAY_PLANS_PURCHASED.unit_credit_specification_id is NULL;");

        q.setParameter(1, pi.getProductInstanceId());

        java.util.List lst = q.getResultList();

        Object row[] = (Object[]) lst.get(0);

        //  java.math.BigDecimal count330 = (java.math.BigDecimal) row[0];
        Long count330 = (Long) row[0];

        if (count330.longValue() > 0) { // Product has an active monthly bundle of any type (220,280,102,274,273)
            ucSpecIds.add(new Integer(369));
            ucSpecIds.add(new Integer(370));
            ucSpecIds.add(new Integer(371));
            ucSpecIds.add(new Integer(372));
            ucSpecIds.add(new Integer(373));
            log.warn("NgAddONOffers: Added AddON UCSs: 369, 370, 371, 372 and 373");
        } else {
            log.warn("NgAddONOffers: Product Instance  " + pi.getProductInstanceId() + " does not qualify for AddONs - Count = " + count330);
        }

        return "Done";
    }

    
    public String FestivePromotionAction(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust, String triggerResult, Logger log) throws Exception {
        // Give BUNKPB5030 and BUNPG9090 to new customers 
        String campaignId = "1_HOUR_FREE_YOUTUBE";
        CampaignBean cb = new CampaignBean(campaignId, pi.getProductInstanceId().intValue(), "NA");
         
        int bonusBundleSpecId = 384;

        log.warn("Provisioning 1 Hours Free Youtube bundle "+ bonusBundleSpecId + " on making sale_id - " + triggerResult);
        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        if (!cb.provisionUnitCredit(pi.getProductInstanceId(), si.getAccountId(), bonusBundleSpecId, 1, "1_HOUR_FREE_YOUTUBE_" + campaignId + "_" + pi.getProductInstanceId() + "_" + triggerResult + "_" + bonusBundleSpecId)) {
            log.warn("Error trying to provision bundle, most likely a duplicate");
            return "Done";
        }
        
        bonusBundleSpecId = 385;
        log.warn("Provisioning 3 months of 100% bonus bundle "+ bonusBundleSpecId + " on making sale_id - " + triggerResult);
        if (!cb.provisionUnitCredit(pi.getProductInstanceId(), si.getAccountId(), bonusBundleSpecId, 1, "FESTIVE_100_BONUS_" + campaignId + "_" + pi.getProductInstanceId() + "_" + triggerResult + "_" + bonusBundleSpecId)) {
            log.warn("Error trying to provision bundle, most likely a duplicate");
            return "Done";
        }
        
	if (siList.isEmpty()) {
	    log.debug("Product instance has no services so won't continue processing");
	    return "Done";
	}
	
        String accId = "" + si.getAccountId();

	String text = "Get your Groove on this festive season and enjoy 100% Bonus on any Recharge of any monthly bundle. Smile, your SuperFast online entertainment partner";

	String to = cust.getAlternativeContact1();
	log.debug("Sending [{}] campaign message to alternative contact 1: [{}]", campaignId, to);
	if (!cb.sendSMS("255662100100", to, text, campaignId)) {
	    log.warn("Failed to send campaign message to [{}]", to);
	}

	to = (String) com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getProductInstancePhoneNumber(pi.getProductInstanceId().intValue());
	log.debug("Sending [{}] campaign message to Smile number: [{}]", campaignId, to);
	if (!cb.sendSMS("255662100100", to, text, campaignId)) {
	    log.warn("Failed to send campaign message to [{}]", to);
	        }

        return "Done";
    }
    
    public String Valentine2018PromoAction(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust, String triggerResult, Logger log) throws Exception {

        String campaignId = "VALENTINE_CAMPAIGN";
        CampaignBean cb = new CampaignBean(campaignId, pi.getProductInstanceId().intValue(), "NA");

        int bonusBundleSpecId = 109;

        log.warn("Provisioning bonus bundle " + bonusBundleSpecId + " on making sale_id - " + triggerResult);
        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        
        if (!cb.provisionUnitCredit(pi.getProductInstanceId(), si.getAccountId(), bonusBundleSpecId, 1, "VALENTINE_ACTION_" + campaignId + "_" + pi.getProductInstanceId() + "_" + triggerResult + "_" + bonusBundleSpecId)) {
            log.warn("Error trying to provision bundle, most likely a duplicate");
            return "Done";
        }

        return "Done";
    }

    public static String processStandardEnrolNew(com.smilecoms.cm.db.model.Campaign campaign,
            List siList,
            Customer customer,
            com.smilecoms.cm.EnrolResult enrolResult,
            Logger log) {

        log.warn("XXXXXXXXXX In processStandardEnrolNew");
        CampaignBean cb = new CampaignBean(
                campaign.getCampaignPK().getName(),
                enrolResult.productInstanceId,
                campaign.getCampaignPK().getName() + "-" + String.valueOf(enrolResult.productInstanceId));

        
        if (enrolResult.productInstanceId > 0
                && enrolResult.runType != null
                && enrolResult.smsFrom != null
                && enrolResult.smsTo != null
                && enrolResult.smsMsg != null
                && !enrolResult.smsMsg.isEmpty()) {
            String testTo = Utils.getValueFromCRDelimitedAVPString(campaign.getActionConfig(), "TestTo");
            String smsTo = enrolResult.smsTo;
            if (testTo != null && !testTo.isEmpty()) {
                smsTo = testTo;
                log.debug("This is a test campaign. SMS will go to [{}]", smsTo);
            }
            log.warn("Sending processStandardEnrol sms [{}] from [{}] to [{}] for PI [{}]",
                    new Object[]{enrolResult.smsMsg, enrolResult.smsFrom, smsTo, String.valueOf(enrolResult.productInstanceId)});
            cb.sendSMS(enrolResult.smsFrom, smsTo, enrolResult.smsMsg, "Enrol-" + String.valueOf(campaign.getCampaignPK().getCampaignId()));
        }

        if (enrolResult.productInstanceId > 0
                && enrolResult.runType != null
                && enrolResult.emailFrom != null
                && enrolResult.emailTo != null
                && enrolResult.emailTemplate != null) {

            String testTo = Utils.getValueFromCRDelimitedAVPString(campaign.getActionConfig(), "TestToEmail");
            String emailTo = enrolResult.emailTo;
            if (testTo != null && !testTo.isEmpty()) {
                emailTo = testTo;
                log.debug("This is a test campaign. Email will go to [{}]", emailTo);
            }

            String prefix = "env.cm.email.template." + enrolResult.emailTemplate.toLowerCase();
            String subjectResource  = prefix + ".email.subject";
            String bodyResource = prefix + ".email.body";
            
            String emailSubject = com.smilecoms.commons.base.BaseUtils.getProperty(subjectResource, "");
            String emailBody = "";
            
            log.warn("XXXXXXXXXXXX Email format property prefix is [{}]", prefix);

            if (enrolResult.emailTextContentPram != null
                && !enrolResult.emailTextContentPram.isEmpty()) {
               Locale locale = com.smilecoms.commons.localisation.LocalisationHelper.getLocaleForLanguage(customer.getLanguage());
                // String xsltEmailBody = com.smilecoms.commons.localisation.LocalisationHelper.getLocalisedString(locale, prefix + ".email.body");
               emailBody = com.smilecoms.commons.localisation.LocalisationHelper.getLocalisedStringAllowingDuplicatePlaceholders(locale, bodyResource, 
                    new Object[] {enrolResult.emailTextContentPram});
            } else  {
                 emailBody = com.smilecoms.commons.base.BaseUtils.getProperty(bodyResource, "");
            }
            
            //Override subject with that was passed in.
            if (enrolResult.emailSubjectParam != null
                && !enrolResult.emailSubjectParam.isEmpty()) {
                emailSubject = enrolResult.emailSubjectParam;
            }
                                   
            if (!emailSubject.isEmpty() && !emailBody.isEmpty()) {
                log.warn("Sending email");
                cb.sendEmail(enrolResult.emailFrom, emailTo, emailSubject, emailBody);
                log.warn("Sent email");
            } else {
                log.error("XXXXXXXXXX This event has no associated email [{}][{}]", emailSubject, emailBody);
            }

        }
        return "Done";
    }

    public static String processStandardRemovalNew(com.smilecoms.cm.db.model.Campaign campaign,
            List siList,
            Customer customer,
            com.smilecoms.cm.RemovalResult removalResult,
            Logger log) {

        CampaignBean cb = new CampaignBean(
                campaign.getCampaignPK().getName(),
                removalResult.productInstanceId,
                campaign.getCampaignPK().getName() + "-" + String.valueOf(removalResult.productInstanceId));

        if (removalResult.productInstanceId > 0
                && removalResult.smsFrom != null
                && removalResult.smsTo != null
                && removalResult.smsMsg != null
                && !removalResult.smsMsg.isEmpty()) {
            String testTo = Utils.getValueFromCRDelimitedAVPString(campaign.getActionConfig(), "TestTo");
            String smsTo = removalResult.smsTo;
            if (testTo != null && !testTo.isEmpty()) {
                smsTo = testTo;
                log.debug("This is a test campaign. Removal SMS will go to [{}]", smsTo);
            }
            log.warn("Sending processStandardRemoval sms [{}] from [{}] to [{}] for PI [{}]",
                    new Object[]{removalResult.smsMsg, removalResult.smsFrom, smsTo, String.valueOf(removalResult.productInstanceId)});
            cb.sendSMS(removalResult.smsFrom, smsTo, removalResult.smsMsg, "Removal-" + String.valueOf(campaign.getCampaignPK().getCampaignId()));
        }

        if (removalResult.productInstanceId > 0
                && removalResult.emailFrom != null
                && removalResult.emailTo != null
                && removalResult.emailTemplate != null) {

            String testTo = Utils.getValueFromCRDelimitedAVPString(campaign.getActionConfig(), "TestToEmail");
            String emailTo = removalResult.emailTo;
            if (testTo != null && !testTo.isEmpty()) {
                emailTo = testTo;
                log.debug("This is a test campaign removal. Email will go to [{}]", emailTo);
            }
            String prefix = "env.cm.email.template." + removalResult.emailTemplate.toLowerCase();

            log.warn("XXXXXXXXXXXX Email format property prefix is [{}]", prefix);

            String emailSubject = com.smilecoms.commons.base.BaseUtils.getProperty(prefix + ".email.subject", "");
            String emailBody = com.smilecoms.commons.base.BaseUtils.getProperty(prefix + ".email.body", "");

            if (!emailSubject.isEmpty() && !emailBody.isEmpty()) {
                log.warn("Sending email");
                cb.sendEmail(removalResult.emailFrom, emailTo, emailSubject, emailBody);
                log.warn("Sent email");
            } else {
                log.error("XXXXXXXXXX This event has no associated email [{}][{}]", emailSubject, emailBody);
            }

        }
        return "Done";
    }

    public static String processStandardEnticement(com.smilecoms.cm.db.model.Campaign campaign,
            com.smilecoms.cm.db.model.CampaignRun campaignRun,
            List siList,
            Customer customer,
            com.smilecoms.cm.EnticementResult enticementResult,
            Logger log) {

        log.warn("XXXXXXXX in processStandardEnticement");
        CampaignBean cb = new CampaignBean(
                campaign.getCampaignPK().getName(),
                enticementResult.productInstanceId,
                campaign.getCampaignPK().getName() + "-" + String.valueOf(enticementResult.productInstanceId));

        if (enticementResult.productInstanceId > 0
                && enticementResult.smsFrom != null
                && enticementResult.smsTo != null
                && enticementResult.smsMsg != null
                && !enticementResult.smsMsg.isEmpty()) {
            String testTo = Utils.getValueFromCRDelimitedAVPString(campaign.getActionConfig(), "TestTo");
            String smsTo = enticementResult.smsTo;
            if (testTo != null && !testTo.isEmpty()) {
                smsTo = testTo;
                log.debug("This is a test campaign. Enticement SMS will go to [{}]", smsTo);
            }
            log.warn("Sending processStandardEnticement sms [{}] from [{}] to [{}] for PI [{}]",
                    new Object[]{enticementResult.smsMsg, enticementResult.smsFrom, smsTo, String.valueOf(enticementResult.productInstanceId)});
            cb.sendSMS(enticementResult.smsFrom, smsTo, enticementResult.smsMsg, "Entice-" + String.valueOf(campaign.getCampaignPK().getCampaignId()));
        }

        return "Done";
    }
    
            
    
    public static String processStandardEnticementNew(com.smilecoms.cm.db.model.Campaign campaign,
            com.smilecoms.cm.db.model.CampaignRun campaignRun,
            List siList,
            Customer customer,
            com.smilecoms.cm.EnticementResult enticementResult,
            Logger log) {

        log.warn("XXXXXXXX in processStandardEnticement");
        CampaignBean cb = new CampaignBean(
                campaign.getCampaignPK().getName(),
                enticementResult.productInstanceId,
                campaign.getCampaignPK().getName() + "-" + String.valueOf(enticementResult.productInstanceId));

        if (enticementResult.productInstanceId > 0
                && enticementResult.smsFrom != null
                && enticementResult.smsTo != null
                && enticementResult.smsMsg != null
                && !enticementResult.smsMsg.isEmpty()) {
            String testTo = Utils.getValueFromCRDelimitedAVPString(campaign.getActionConfig(), "TestTo");
            String smsTo = enticementResult.smsTo;
            if (testTo != null && !testTo.isEmpty()) {
                smsTo = testTo;
                log.debug("This is a test campaign. Enticement SMS will go to [{}]", smsTo);
            }
            log.warn("Sending processStandardEnticement sms [{}] from [{}] to [{}] for PI [{}]",
                    new Object[]{enticementResult.smsMsg, enticementResult.smsFrom, smsTo, String.valueOf(enticementResult.productInstanceId)});
            cb.sendSMS(enticementResult.smsFrom, smsTo, enticementResult.smsMsg, "Entice-" + String.valueOf(campaign.getCampaignPK().getCampaignId()));
        }
        
        if (enticementResult.productInstanceId > 0
                && enticementResult.emailFrom != null
                && enticementResult.emailTo != null
                && enticementResult.emailTemplate != null
                && !enticementResult.emailTemplate.isEmpty()) {

                String prefix = "customer.campaigns." + enticementResult.emailTemplate.toLowerCase();
                log.debug("Getting resources for prefix [{}]", prefix);

                Locale locale = com.smilecoms.commons.localisation.LocalisationHelper.getLocaleForLanguage(customer.getLanguage());

                String xsltEmailSubject = com.smilecoms.commons.localisation.LocalisationHelper.getLocalisedString(locale, prefix + ".email.subject");
                String xsltEmailBody = com.smilecoms.commons.localisation.LocalisationHelper.getLocalisedString(locale, prefix + ".email.body");



                if (!xsltEmailSubject.startsWith("?") && !xsltEmailBody.startsWith("?")) {
                    cb.sendEmail(enticementResult.smsFrom, enticementResult.emailTo, xsltEmailSubject, xsltEmailBody);
                } else {
                    log.error("This event has no associated email");
                }
            }

        return "Done";
    }
    
    public static String NgUC1DayUnlimitedDaily(javax.persistence.EntityManager em, Logger log, Set ucSpecIds, com.smilecoms.cm.db.model.ProductInstance pi, com.smilecoms.cm.db.model.Campaign dbCampaign) {
        log.warn("NgUC1DayUnlimitedDaily: Added UCS: 444");
        ucSpecIds.add(new Integer(444));       
	log.warn("NgUC1DayUnlimitedDaily: Done");       
        return "Done";
    }
    
    
    public String UGFestiveEnrol(com.smilecoms.cm.db.model.Campaign campaign, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust, Logger log) throws Exception {

        // Give BUNKPB5030 and BUNPG9090 to new customers 
        String campaignId = "1_HOUR_FREE_YOUTUBE";
        CampaignBean cb = new CampaignBean(campaignId, pi.getProductInstanceId().intValue(), "NA");
         
        int bonusBundleSpecId = 384;

        log.warn("Provisioning 1 Hours Free Youtube bundle "+ bonusBundleSpecId + " on provision of product instance id: " + pi.getProductInstanceId());
        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        if (!cb.provisionUnitCredit(pi.getProductInstanceId(), si.getAccountId(), bonusBundleSpecId, 1, "1_HOUR_FREE_YOUTUBE_" + campaignId + "_" + pi.getProductInstanceId() + "_" + bonusBundleSpecId)) {
            log.warn("Error trying to provision bundle, most likely a duplicate");
            return "Done";
        }
        
        bonusBundleSpecId = 385;
        log.warn("Provisioning 3 months of 100% bonus bundle "+ bonusBundleSpecId + " on provision of product instance id: " + pi.getProductInstanceId());
        if (!cb.provisionUnitCredit(pi.getProductInstanceId(), si.getAccountId(), bonusBundleSpecId, 1, "FESTIVE_100_BONUS_" + campaignId + "_" + pi.getProductInstanceId() + "_" + bonusBundleSpecId)) {
            log.warn("Error trying to provision bundle, most likely a duplicate");
            return "Done";
        }
        
	if (siList.isEmpty()) {
	    log.debug("Product instance has no services so won't continue processing");
	    return "Done";
	}
	
        String accId = "" + si.getAccountId();

	String text = "Get your Groove on this festive season and enjoy 100% Bonus on any Recharge of any monthly bundle. Smile, your SuperFast online entertainment partner";

	String to = cust.getAlternativeContact1();
	log.debug("Sending [{}] campaign message to alternative contact 1: [{}]", campaignId, to);
	if (!cb.sendSMS("255662100100", to, text, campaignId)) {
	    log.warn("Failed to send campaign message to [{}]", to);
	}

	to = (String) com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getProductInstancePhoneNumber(pi.getProductInstanceId().intValue());
	log.debug("Sending [{}] campaign message to Smile number: [{}]", campaignId, to);
	if (!cb.sendSMS("255662100100", to, text, campaignId)) {
	    log.warn("Failed to send campaign message to [{}]", to);
	        }

        return "Done";
    }
    
    
    public String FreeOnnetAction(com.smilecoms.cm.db.model.CampaignRun lockedCampaignRun, com.smilecoms.cm.db.model.ProductInstance pi, java.util.List siList, Customer cust, String triggerResult, Logger log) throws Exception {
        
        String campaignId = "FreeOnnet_CAMPAIGN";
        CampaignBean cb = new CampaignBean(campaignId, pi.getProductInstanceId().intValue(), "NA");
         
        int extraUnitSpecId = 465;

        
        log.warn("Provisioning extra unit credit bundle "+ extraUnitSpecId + " on making sale_id - " + triggerResult);
        com.smilecoms.cm.db.model.ServiceInstance si = (com.smilecoms.cm.db.model.ServiceInstance) siList.get(0);
        if (!cb.provisionUnitCredit(pi.getProductInstanceId(), si.getAccountId(), extraUnitSpecId, 1, "FreeOnnet_ACTION_" + campaignId + "_" + pi.getProductInstanceId() + "_" + triggerResult + "_" + extraUnitSpecId)) {
            log.warn("Error trying to provision bundle, most likely a duplicate");
            return "Done";
        }
        
        return "Done";
    }
    

}
