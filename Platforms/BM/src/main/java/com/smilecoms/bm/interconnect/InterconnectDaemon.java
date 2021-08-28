package com.smilecoms.bm.interconnect;

import com.smilecoms.bm.db.model.AccountHistory;
import com.smilecoms.bm.db.model.InterconnectHistory;
import com.smilecoms.bm.db.model.RatePlan;
import com.smilecoms.bm.db.model.ServiceInstance;
import com.smilecoms.bm.db.op.DAO;
import com.smilecoms.bm.rating.IRatingEngine;
import com.smilecoms.bm.rating.RatingManager;
import com.smilecoms.bm.rating.RatingResult;
import com.smilecoms.commons.base.lifecycle.BaseListener;
import org.slf4j.*;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

@Singleton
@Startup
@Local({BaseListener.class})
public class InterconnectDaemon implements BaseListener {

    private static final Logger log = LoggerFactory.getLogger(InterconnectDaemon.class);
    private static EntityManagerFactory emf = null;
    private static ScheduledFuture runner1 = null;
    private static ScheduledFuture runner2 = null;

    @PostConstruct
    public void startUp() {
        emf = JPAUtils.getEMF("BMPU_RL");
        BaseUtils.registerForPropsAvailability(this);
    }

    @Override
    public void propsAreReadyTrigger() {
        log.warn("Interconnect Daemon is starting up as properties are ready");
        BaseUtils.deregisterForPropsAvailability(this);
        BaseUtils.registerForPropsChanges(this);
        runner1 = Async.scheduleAtFixedRate(new SmileBaseRunnable("BM.interconnectRating") {
            @Override
            public void run() {
                trigger("RATE");
            }
        }, 10000, 10 * 60000, 15 * 60000);
        runner2 = Async.scheduleAtFixedRate(new SmileBaseRunnable("BM.interconnectReRating") {
            @Override
            public void run() {
                trigger("RERATE");
            }
        }, 10000, 10 * 60000, 15 * 60000);
    }

    @PreDestroy
    public void shutDown() {
        BaseUtils.deregisterForPropsChanges(this);
        Async.cancel(runner1);
        Async.cancel(runner2);
        JPAUtils.closeEMF(emf);
    }

    @Override
    public void propsHaveChangedTrigger() {
    }

    private void trigger(String event) {
        try {
            if (!BaseUtils.getBooleanProperty("env.bm.interconnect.daemon.mustrun", true)) {
                log.debug("Interconnect daemon is set to not run");
                return;
            }
            log.debug("Interconnect Daemon triggered to do interconnect rating processing");
            EntityManager em = JPAUtils.getEM(emf);
            try {
                if (event.equals("RATE")) {
                    Date ratedTo;
                    do {
                        ratedTo = doRating(em);
                        log.debug("Rated up to [{}]", ratedTo);
                    } while (ratedTo != null && !Utils.isDateInTimeframe(ratedTo, 1, Calendar.HOUR));
                } else if (event.equals("RERATE")) {
                    int cnt;
                    do {
                        cnt = doReRating(em);
                        log.debug("ReRated  [{}]", cnt);
                    } while (cnt > 0);
                }
            } catch (Exception e) {
                log.warn("Error in interconnect rating daemon [{}]", e.toString());
                log.warn("Error: ", e);
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "BM", "Error in interconnect rating daemon: " + e.toString());
            } finally {
                JPAUtils.closeEM(em);
            }
        } catch (Exception e) {
            log.warn("Error: ", e);
        }
    }

    private Date doRating(EntityManager em) throws Exception {
        Date eventsEndingToExcl = null;
        try {
            JPAUtils.beginTransaction(em);
            InterconnectHistory ih;
            long start = System.currentTimeMillis();
            try {
                ih = DAO.createInterconnectHistory(em);
                JPAUtils.commitTransaction(em);
            } catch (Exception e) {
                try {
                    JPAUtils.rollbackTransaction(em);
                    JPAUtils.beginTransaction(em);
                    log.debug("Interconnect daemon is already processing somewhere else [{}]", e.toString());
                    try {
                        ih = DAO.getRunningInterconnectHistory(em);
                    } catch (javax.persistence.NoResultException nre) {
                        log.debug("Run no longer exists");
                        return eventsEndingToExcl;
                    }
                    if (!Utils.isDateInTimeframe(ih.getRunStartDatetime(), 2, Calendar.HOUR)) {
                        log.debug("Current running interconnect process must be stuck. Going to remove it");
                        DAO.updateInterconnectHistoryEndOfRun(em, ih, -1);
                    }
                } catch (Exception e2) {
                    log.warn("Error removing stuck interconnect run");
                    BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "BM", "Error removing stuck interconnect run: " + e2.toString());
                } finally {
                    JPAUtils.commitTransaction(em);
                }
                return eventsEndingToExcl;
            }

            JPAUtils.beginTransaction(em);

            log.debug("Interconnect daemon is not processing somewhere else. Will process now");
            log.debug("Getting date to start processing from. The dates we look at in account_history are the end date of any session");
            Date eventsEndingFromIncl = DAO.getLastProcessedEventDate(em);
            // To must be at the end of the same day
            eventsEndingToExcl = Utils.getBeginningOfNextDay(eventsEndingFromIncl);
            if (Utils.isInTheFuture(eventsEndingToExcl)) {
                // Dont rate  calls that are only just finished as its possible for a call to end and then take a few seconds to get to BM. The end date is based on the SCSCF date not the date the record is written
                // Its also possible for a non-finshed session to go to NF when the reservation expires
                // So only look at rows that ended more than 5 minutes ago
                eventsEndingToExcl = Utils.getPastDate(Calendar.SECOND, BaseUtils.getIntProperty("env.bm.interconnect.rating.upto.seconds.ago", 300));
            }
            log.debug("This run will process sessions ending between [{}] incl and [{}] excl", eventsEndingFromIncl, eventsEndingToExcl);
            DAO.updateInterconnectHistoryStartOfRun(em, ih, eventsEndingFromIncl, eventsEndingToExcl);
            JPAUtils.commitTransaction(em);
            JPAUtils.beginTransaction(em);
            log.debug("Getting rate plan data into memory");

            IRatingEngine engine = RatingManager.getRatingEngine("com.smilecoms.bm.rating.BestPrefixServiceRatingEngine");
            RatePlan plan = new RatePlan();
            plan.setRatePlanId(10000);

            List<AccountHistory> eventsToRate = DAO.getEventsToRate(em, eventsEndingFromIncl, eventsEndingToExcl);
            log.debug("We have [{}] events to rate", eventsToRate.size());
            int cnt = 0;
            int rowCnt = 0;
            int interimCommitCnt = 0;
            for (AccountHistory event : eventsToRate) {
                rowCnt++;
                try {
                    log.debug("Processing account history row [{}] of [{}]", rowCnt, eventsToRate.size());

                    if (DAO.isInterconnectEventLocked(em, event.getId())) {
                        log.debug("Account history row [{}] is locked from re-rating. Ignoring", event.getId());
                        continue;
                    }

                    RatingKey ratingKey = new RatingKey();
                    ratingKey.setFrom(event.getSource());
                    ratingKey.setTo(event.getDestination());
                    ratingKey.setIncomingTrunk(event.getIncomingTrunk());
                    ratingKey.setOutgoingTrunk(event.getOutgoingTrunk());
                    ratingKey.setServiceCode(event.getTransactionType());
                    ratingKey.setLeg(event.getLeg());

                    ServiceInstance serviceInstance = new ServiceInstance();
                    serviceInstance.setServiceInstanceId(event.getServiceInstanceId());
                    RatingResult ratingResult = engine.rate(serviceInstance, ratingKey, plan, null, event.getStartDate());
                    log.debug("Account history id [{}] will use service rate id [{}]", event.getId(), ratingResult.getRateId());
                    BigDecimal fromInterconnectAmountCents = ratingResult.getFromInterconnectRateCentsPerUnit().multiply(event.getTotalUnits());
                    BigDecimal toInterconnectAmountCents = ratingResult.getToInterconnectRateCentsPerUnit().multiply(event.getTotalUnits());

                    DAO.createInterconnectEvent(em,
                            event.getId(),
                            fromInterconnectAmountCents,
                            toInterconnectAmountCents,
                            ih.getInterconnectHistoryId(),
                            event.getTotalUnits().intValue(),
                            ratingResult.getFromInterconnectCurrency(),
                            ratingResult.getToInterconnectCurrency(),
                            event.getStartDate(),
                            event.getEndDate());
                    interimCommitCnt++;

                    if (interimCommitCnt >= 1000) {
                        log.debug("Doing interim commit");
                        JPAUtils.commitTransactionAndClear(em);
                        JPAUtils.beginTransaction(em);
                        interimCommitCnt = 0;
                    }
                    cnt++;
                } catch (Exception e) {
                    String msg = "Error processing interconnect rating -- Account History Id [" + event.getId() + "] From [" + event.getSource()
                            + "] To [" + event.getDestination() + "] Incoming Trunk [" + event.getIncomingTrunk() + "] Outgoing Trunk [" + event.getOutgoingTrunk()
                            + "] Leg [" + event.getLeg() + "] Service Code [" + event.getTransactionType()
                            + "] Caused by: [" + e.toString() + "]";
                    BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "BM", msg);
                    if (!BaseUtils.getBooleanProperty("env.bm.interconnect.rating.continue.on.error", false)) {
                        throw new Exception(msg);
                    }
                }
            }

            DAO.updateInterconnectHistoryEndOfRun(em, ih, cnt);
            log.debug("Committing transaction");
            JPAUtils.commitTransaction(em);
            long end = System.currentTimeMillis();
            log.debug("Finished interconnect rating run. Run took [{}]ms to process [{}] records", end - start, cnt);
        } catch (Exception e) {
            log.warn("Error in interconnect rating. Going to close failed history row", e);
            try {
                JPAUtils.rollbackTransaction(em);
                JPAUtils.beginTransaction(em);
                DAO.closeFailedInterconnectHistory(em, e.toString());
                JPAUtils.commitTransactionAndClear(em);
            } catch (Exception ex) {
                log.warn("Error closing failed interconnect history", ex);
            }
            throw e;
        }
        return eventsEndingToExcl;
    }

    private int doReRating(EntityManager em) throws Exception {
        int cnt = 0;
        try {
            JPAUtils.beginTransaction(em);

            List<AccountHistory> eventsToRate = DAO.getEventsToReRate(em);
            if (eventsToRate.isEmpty()) {
                log.debug("No re-rating to do");
                JPAUtils.commitTransaction(em);
                return 0;
            }

            InterconnectHistory ih;
            long start = System.currentTimeMillis();
            try {
                ih = DAO.createInterconnectHistory(em);
                JPAUtils.commitTransaction(em);
            } catch (Exception e) {
                try {
                    JPAUtils.rollbackTransaction(em);
                    JPAUtils.beginTransaction(em);
                    log.debug("Interconnect daemon is already processing somewhere else [{}]", e.toString());
                    try {
                        ih = DAO.getRunningInterconnectHistory(em);
                    } catch (javax.persistence.NoResultException nre) {
                        log.debug("Run no longer exists");
                        return cnt;
                    }
                    if (!Utils.isDateInTimeframe(ih.getRunStartDatetime(), 2, Calendar.HOUR)) {
                        log.debug("Current running interconnect process must be stuck. Going to remove it");
                        DAO.updateInterconnectHistoryEndOfRun(em, ih, -1);
                    }
                } catch (Exception e2) {
                    log.warn("Error removing stuck interconnect run");
                    BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "BM", "Error removing stuck interconnect run: " + e2.toString());
                } finally {
                    JPAUtils.commitTransaction(em);
                }
                return cnt;
            }

            JPAUtils.beginTransaction(em);

            log.debug("Interconnect re rating daemon is not processing somewhere else. Will process now");

            Date oldest = new Date();
            Date newest = new Date(0);
            for (AccountHistory ah : eventsToRate) {
                if (ah.getStartDate().before(oldest)) {
                    oldest = ah.getStartDate();
                }
                if (ah.getStartDate().after(newest)) {
                    newest = ah.getStartDate();
                }
            }

            DAO.updateInterconnectHistoryStartOfRun(em, ih, oldest, newest);

            JPAUtils.commitTransaction(em);
            JPAUtils.beginTransaction(em);
            log.debug("Getting rate plan data into memory");

            IRatingEngine engine = RatingManager.getRatingEngine("com.smilecoms.bm.rating.BestPrefixServiceRatingEngine");
            RatePlan plan = new RatePlan();
            plan.setRatePlanId(10000);

            log.debug("We have [{}] events to re-rate", eventsToRate.size());
            int rowCnt = 0;
            int interimCommitCnt = 0;
            for (AccountHistory event : eventsToRate) {
                rowCnt++;
                try {
                    log.debug("Processing account history row [{}] of [{}]", rowCnt, eventsToRate.size());

                    RatingKey ratingKey = new RatingKey();
                    ratingKey.setFrom(event.getSource());
                    ratingKey.setTo(event.getDestination());
                    ratingKey.setIncomingTrunk(event.getIncomingTrunk());
                    ratingKey.setOutgoingTrunk(event.getOutgoingTrunk());
                    ratingKey.setServiceCode(event.getTransactionType());
                    ratingKey.setLeg(event.getLeg());

                    ServiceInstance serviceInstance = new ServiceInstance();
                    serviceInstance.setServiceInstanceId(event.getServiceInstanceId());
                    RatingResult ratingResult = engine.rate(serviceInstance, ratingKey, plan, null, event.getStartDate());
                    log.debug("Account history id [{}] will use service rate id [{}]", event.getId(), ratingResult.getRateId());
                    BigDecimal fromInterconnectAmountCents = ratingResult.getFromInterconnectRateCentsPerUnit().multiply(event.getTotalUnits());
                    BigDecimal toInterconnectAmountCents = ratingResult.getToInterconnectRateCentsPerUnit().multiply(event.getTotalUnits());
                    cnt++;
                    DAO.createInterconnectEvent(em,
                            event.getId(),
                            fromInterconnectAmountCents,
                            toInterconnectAmountCents,
                            ih.getInterconnectHistoryId(),
                            event.getTotalUnits().intValue(),
                            ratingResult.getFromInterconnectCurrency(),
                            ratingResult.getToInterconnectCurrency(), event.getStartDate(), event.getEndDate());
                    interimCommitCnt++;

                    if (interimCommitCnt >= 1000) {
                        log.debug("Doing interim commit");
                        JPAUtils.commitTransactionAndClear(em);
                        JPAUtils.beginTransaction(em);
                        interimCommitCnt = 0;
                    }
                } catch (Exception e) {
                    String msg = "Error processing interconnect rating -- Account History Id [" + event.getId() + "] From [" + event.getSource()
                            + "] To [" + event.getDestination() + "] Incoming Trunk [" + event.getIncomingTrunk() + "] Outgoing Trunk [" + event.getOutgoingTrunk()
                            + "] Leg [" + event.getLeg() + "] Service Code [" + event.getTransactionType()
                            + "] Caused by: [" + e.toString() + "]";
                    BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "BM", msg);
                    if (!BaseUtils.getBooleanProperty("env.bm.interconnect.rating.continue.on.error", false)) {
                        throw new Exception(msg);
                    }
                }
            }

            DAO.updateInterconnectHistoryEndOfRun(em, ih, cnt);
            log.debug("Committing transaction");
            JPAUtils.commitTransaction(em);
            long end = System.currentTimeMillis();
            log.debug("Finished interconnect re-rating run. Run took [{}]ms to process [{}] records", end - start, cnt);
        } catch (Exception e) {
            log.warn("Error in interconnect re-rating. Going to close failed history row", e);
            try {
                JPAUtils.rollbackTransaction(em);
                JPAUtils.beginTransaction(em);
                DAO.closeFailedInterconnectHistory(em, e.toString());
                JPAUtils.commitTransactionAndClear(em);
            } catch (Exception ex) {
                log.warn("Error closing failed interconnect history", ex);
            }
            throw e;
        }
        return cnt;
    }
}
