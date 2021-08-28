package com.smilecoms.bm.unitcredits;

import com.smilecoms.bm.EventHelper;
import com.smilecoms.bm.unitcredits.wrappers.IUnitCredit;
import com.smilecoms.bm.db.model.UnitCreditInstance;
import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import org.slf4j.*;

// PCB 202 - Warn of expiring bundles
@Singleton
@Startup
@Local({BaseListener.class})
public class UCLifecycleDaemon implements BaseListener {

    private static final Logger log = LoggerFactory.getLogger(UCLifecycleDaemon.class.getName());
    private EntityManagerFactory emf = null;
    private EntityManager em;
    private static ScheduledFuture runner1 = null;

    @PostConstruct
    public void startUp() {
        BaseUtils.registerForPropsAvailability(this);
        emf = JPAUtils.getEMF("BMPU_RL");
    }

    @Override
    public void propsAreReadyTrigger() {
        runner1 = Async.scheduleAtFixedRate(new SmileBaseRunnable("BM.UCLifecycle") {
            @Override
            public void run() {
                trigger();
            }
        }, 180000, 3000 * 1000, 3600 * 1000);
    }

    @PreDestroy
    public void shutDown() {
        BaseUtils.deregisterForPropsAvailability(this);
        Async.cancel(runner1);
        JPAUtils.closeEMF(emf);
    }

    /**
     * Note that the functions in the life cycle are likely to trigger lifecycle
     * events more than once per UCI. The UCI's must cope with this gracefully
     *
     */
    private void trigger() {
        try {
            if (log.isDebugEnabled()) {
                log.debug("UCLifecycleDaemon triggered by thread {} on class {}", new Object[]{Thread.currentThread().getId(), this.toString()});
            }

            do4HoursPreExpiryWarnings();
            do8HoursPreExpiryWarnings();
            do30DaysPreExpiryWarnings();
            do1WeekPreExpiryWarnings();
            do3DaysPreExpiryWarnings();
            do2DaysPreExpiryWarnings();
            do1DayPreExpiryWarnings();
            doPostExpiryProcessing();
            doPostEndDateProcessing();
            
            if (BaseUtils.getBooleanProperty("env.bm.check.bundle.constraints", true)) {
                doCheckUnitCreditConstraints();
            }

            do3DaysAndNoRechargeWarnings();
            do5DaysAndNoRechargeWarnings();
            do10DaysAndNoRechargeWarnings();
            do20DaysAndNoRechargeWarnings();
            do30DaysAndNoRechargeWarnings();
            if (log.isDebugEnabled()) {
                log.debug("UCLifecycleDaemon trigger finished by thread {} on class {}", new Object[]{Thread.currentThread().getId(), this.toString()});
            }
        } catch (Exception e) {
            log.warn("Error: ", e);
        }
    }

    private void do30DaysPreExpiryWarnings() {
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            /**
             * Look for rows that expire between 30 days time and a few hours
             * before that
             */
            Query q = em.createNativeQuery("select * from unit_credit_instance I "
                    + "where I.EXPIRY_DATE <= now() + interval 720 hour and I.EXPIRY_DATE > now() + interval 718 hour", UnitCreditInstance.class);
            List<UnitCreditInstance> aboutToExpireList = q.getResultList();
            Collections.shuffle(aboutToExpireList); // help prevent clashes
            for (UnitCreditInstance uci : aboutToExpireList) {
                try {
                    IUnitCredit uc = UnitCreditManager.getWrappedUnitCreditInstance(em, uci, null);
                    uc.do30DaysPreExpiryProcessing();
                } catch (Exception e) {
                    log.debug("Something went wrong notifying of 30 day pre expired UC: [{}]", e.toString());
                }
            }
        } catch (Exception e) {
            log.warn("Error in UCLifecycleDaemon doing 30 day pre expiry warning processing. Will ignore : [{}]", e.toString());
        } finally {
            try {
                JPAUtils.commitTransactionAndClose(em);
            } catch (Exception ex) {
                log.warn("Failed to commit transaction in do30DaysPreExpiryWarnings: " + ex.toString());
            }
        }
    }

    private void do3DaysPreExpiryWarnings() {
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            /**
             * Look for rows that expire between 3 days time and a few hours
             * before that
             */
            Query q = em.createNativeQuery("select * from unit_credit_instance I "
                    + "where I.EXPIRY_DATE <= now() + interval 72 hour and I.EXPIRY_DATE > now() + interval 70 hour", UnitCreditInstance.class);
            List<UnitCreditInstance> aboutToExpireList = q.getResultList();
            Collections.shuffle(aboutToExpireList); // help prevent clashes
            for (UnitCreditInstance uci : aboutToExpireList) {
                try {
                    IUnitCredit uc = UnitCreditManager.getWrappedUnitCreditInstance(em, uci, null);
                    uc.do3DaysPreExpiryProcessing();
                } catch (Exception e) {
                    log.debug("Something went wrong notifying of 3 day pre expired UC: [{}]", e.toString());
                }
            }
        } catch (Exception e) {
            log.warn("Error in UCLifecycleDaemon doing 3 day pre expiry warning processing. Will ignore : [{}]", e.toString());
        } finally {
            try {
                JPAUtils.commitTransactionAndClose(em);
            } catch (Exception ex) {
                log.warn("Failed to commit transaction in do3DaysPreExpiryWarnings: " + ex.toString());
            }
        }
    }

    private void do2DaysPreExpiryWarnings() {
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            /**
             * Look for rows that expire between 2 days time and a few hours
             * before that
             */
            Query q = em.createNativeQuery("select * from unit_credit_instance I "
                    + "where I.EXPIRY_DATE <= now() + interval 48 hour and I.EXPIRY_DATE > now() + interval 46 hour", UnitCreditInstance.class);
            List<UnitCreditInstance> aboutToExpireList = q.getResultList();
            Collections.shuffle(aboutToExpireList); // help prevent clashes
            for (UnitCreditInstance uci : aboutToExpireList) {
                try {
                    IUnitCredit uc = UnitCreditManager.getWrappedUnitCreditInstance(em, uci, null);
                    uc.do2DaysPreExpiryProcessing();
                } catch (Exception e) {
                    log.debug("Something went wrong notifying of 2 day pre expired UC: [{}]", e.toString());
                }
            }
        } catch (Exception e) {
            log.warn("Error in UCLifecycleDaemon doing 2 day pre expiry warning processing. Will ignore : [{}]", e.toString());
        } finally {
            try {
                JPAUtils.commitTransactionAndClose(em);
            } catch (Exception ex) {
                log.warn("Failed to commit transaction in do2DaysPreExpiryWarnings: " + ex.toString());
            }
        }
    }

    private void do1DayPreExpiryWarnings() {
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            /**
             * Look for rows that expire between 3 days time and a few hours
             * before that
             */
            Query q = em.createNativeQuery("select * from unit_credit_instance I "
                    + "where I.EXPIRY_DATE <= now() + interval 24 hour and I.EXPIRY_DATE > now() + interval 22 hour", UnitCreditInstance.class);
            List<UnitCreditInstance> aboutToExpireList = q.getResultList();
            Collections.shuffle(aboutToExpireList); // help prevent clashes
            for (UnitCreditInstance uci : aboutToExpireList) {
                try {
                    IUnitCredit uc = UnitCreditManager.getWrappedUnitCreditInstance(em, uci, null);
                    uc.do1DayPreExpiryProcessing();
                } catch (Exception e) {
                    log.debug("Something went wrong notifying of 1 day pre expired UC: [{}]", e.toString());
                }
            }
        } catch (Exception e) {
            log.warn("Error in UCLifecycleDaemon doing 1 day pre expiry warning processing. Will ignore : [{}]", e.toString());
        } finally {
            try {
                JPAUtils.commitTransactionAndClose(em);
            } catch (Exception ex) {
                log.warn("Failed to commit transaction in do1DayPreExpiryWarnings: " + ex.toString());
            }
        }
    }

    private void do8HoursPreExpiryWarnings() {
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            /**
             * Look for rows that expire between 4 hours time and a few hours
             * before that
             */
            String specIds = "";
            List<String> resList = Utils.getListFromCRDelimitedString(BaseUtils.getProperty("env.bm.uclifecycle.rollover.gracehour.ucspec.toexclude", "0"));
            for (String res : resList) {
                specIds = specIds + res + ",";
            }
            if (!specIds.isEmpty()) {
                specIds = specIds.substring(0, specIds.length() - 1); // remove trailing ,   123,456, should be substring(0,7)
            }

            if (specIds.isEmpty()) {
                specIds = "0";
            }
            Query q = em.createNativeQuery("select * from unit_credit_instance I "
                    + "where I.EXPIRY_DATE <= now() + interval 8 hour and I.EXPIRY_DATE > now() + interval 6 hour and I.UNITS_REMAINING > 0 and I.UNIT_CREDIT_SPECIFICATION_ID not in (" + specIds + ")", UnitCreditInstance.class);
            List<UnitCreditInstance> aboutToExpireList = q.getResultList();
            Collections.shuffle(aboutToExpireList); // help prevent clashes
            for (UnitCreditInstance uci : aboutToExpireList) {
                try {
                    IUnitCredit uc = UnitCreditManager.getWrappedUnitCreditInstance(em, uci, null);
                    uc.do8HourPreExpiryProcessing();
                } catch (Exception e) {
                    log.debug("Something went wrong notifying of 8 hour pre expired UC: [{}]", e.toString());
                }
            }
        } catch (Exception e) {
            log.warn("Error in UCLifecycleDaemon doing 8 hour pre expiry warning processing. Will ignore : [{}]", e.toString());
        } finally {
            try {
                JPAUtils.commitTransactionAndClose(em);
            } catch (Exception ex) {
                log.warn("Failed to commit transaction in do8HoursPreExpiryWarnings: " + ex.toString());
            }
        }
    }

    private void do4HoursPreExpiryWarnings() {
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            /**
             * Look for rows that expire between 4 hours time and a few hours
             * before that
             */
            Query q = em.createNativeQuery("select * from unit_credit_instance I "
                    + "where I.EXPIRY_DATE <= now() + interval 4 hour and I.EXPIRY_DATE > now() + interval 2 hour", UnitCreditInstance.class);
            List<UnitCreditInstance> aboutToExpireList = q.getResultList();
            Collections.shuffle(aboutToExpireList); // help prevent clashes
            for (UnitCreditInstance uci : aboutToExpireList) {
                try {
                    IUnitCredit uc = UnitCreditManager.getWrappedUnitCreditInstance(em, uci, null);
                    uc.do4HourPreExpiryProcessing();
                } catch (Exception e) {
                    log.debug("Something went wrong notifying of 4 hour pre expired UC: [{}]", e.toString());
                }
            }
        } catch (Exception e) {
            log.warn("Error in UCLifecycleDaemon doing 4 hour pre expiry warning processing. Will ignore : [{}]", e.toString());
        } finally {
            try {
                JPAUtils.commitTransactionAndClose(em);
            } catch (Exception ex) {
                log.warn("Failed to commit transaction in do8HoursPreExpiryWarnings: " + ex.toString());
            }
        }
    }

    private void do1WeekPreExpiryWarnings() {
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            /**
             * For active (paid for bundles), send an alert to customer when the
             * bundle is 50% or less used and the bundle expiry date is 7 days
             * from its expiry date. Remind the customer about the expiry date
             * and prompt the customer to maximise the benefits out of his paid
             * bundle and use more.
             *
             * Look for rows that expire between 7 days time and a few hours
             * before that
             */
            Query q = em.createNativeQuery("select * from unit_credit_instance I "
                    + "where I.EXPIRY_DATE <= now() + interval 168 hour and I.EXPIRY_DATE > now() + interval 166 hour", UnitCreditInstance.class);
            List<UnitCreditInstance> aboutToExpireList = q.getResultList();
            Collections.shuffle(aboutToExpireList); // help prevent clashes
            for (UnitCreditInstance uci : aboutToExpireList) {
                try {
                    IUnitCredit uc = UnitCreditManager.getWrappedUnitCreditInstance(em, uci, null);
                    uc.do1WeekPreExpiryProcessing();
                } catch (Exception e) {
                    log.debug("Something went wrong notifying of almost expired UC: [{}]", e.toString());
                }
            }
        } catch (Exception e) {
            log.warn("Error in UCLifecycleDaemon doing expiry warning processing. Will ignore : [{}]", e.toString());
        } finally {
            try {
                JPAUtils.commitTransactionAndClose(em);
            } catch (Exception ex) {
                log.warn("Failed to commit transaction in do1WeekPreExpiryWarnings: " + ex.toString());
            }
        }
    }

    private void doPostExpiryProcessing() {
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            // Get all UC's that expired in the last 2 hours
            Query q = em.createNativeQuery("select * from unit_credit_instance I "
                    + "where I.EXPIRY_DATE < now() and I.EXPIRY_DATE > now() - interval 2 hour ", UnitCreditInstance.class);
            List<UnitCreditInstance> expiredRecentlyList = q.getResultList();
            Collections.shuffle(expiredRecentlyList); // help prevent clashes
            for (UnitCreditInstance uci : expiredRecentlyList) {
                try {
                    IUnitCredit uc = UnitCreditManager.getWrappedUnitCreditInstance(em, uci, null);
                    uc.doPostExpiryProcessing();
                } catch (Exception e) {
                    log.debug("Something went wrong notifying of UC expiry: [{}]", e.toString());
                }
            }
        } catch (Exception e) {
            log.warn("Error in UCLifecycleDaemon doing expiry processing. Will ignore : [{}]", e.toString());
        } finally {
            try {
                JPAUtils.commitTransactionAndClose(em);
            } catch (Exception ex) {
                log.warn("Failed to commit transaction in doPostExpiryProcessing: " + ex.toString());
            }
        }
    }

    private void doPostEndDateProcessing() {
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            // Get all UC's that have their end date in the last 2 hours
            Query q = em.createNativeQuery("select * from unit_credit_instance I "
                    + "where I.END_DATE < now() and I.END_DATE > now() - interval 2 hour ", UnitCreditInstance.class);
            List<UnitCreditInstance> expiredRecentlyList = q.getResultList();
            Collections.shuffle(expiredRecentlyList); // help prevent clashes
            for (UnitCreditInstance uci : expiredRecentlyList) {
                try {
                    IUnitCredit uc = UnitCreditManager.getWrappedUnitCreditInstance(em, uci, null);
                    uc.doPostEndDateProcessing();
                } catch (Exception e) {
                    log.debug("Something went wrong notifying of UC soft expiry: [{}]", e.toString());
                }
            }
        } catch (Exception e) {
            log.warn("Error in UCLifecycleDaemon doing soft expiry processing. Will ignore : [{}]", e.toString());
        } finally {
            try {
                JPAUtils.commitTransactionAndClose(em);
            } catch (Exception ex) {
                log.warn("Failed to commit transaction in doPostEndDateProcessing: " + ex.toString());
            }
        }
    }

    private void doCheckUnitCreditConstraints() {
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            // Get all UC's that are valid that have config to check bundle constraints
            Query q = em.createNativeQuery("SELECT * FROM unit_credit_instance I "
                    + "       JOIN unit_credit_specification UCS  ON UCS.UNIT_CREDIT_SPECIFICATION_ID =  I.UNIT_CREDIT_SPECIFICATION_ID "
                    + " WHERE     I.EXPIRY_DATE > now() AND UCS.CONFIGURATION LIKE '%HasBundleConstraints=true%'", UnitCreditInstance.class);
            List<UnitCreditInstance> hasBundleConstraintsList = q.getResultList();
            Collections.shuffle(hasBundleConstraintsList); // help prevent clashes
            for (UnitCreditInstance uci : hasBundleConstraintsList) {
                try {
                    IUnitCredit uc = UnitCreditManager.getWrappedUnitCreditInstance(em, uci, null);
                    uc.doCheckUnitCreditConstraints();
                } catch (Exception e) {
                    log.debug("Something went wrong checking bundle constraints: [{}]", e.toString());
                }
            }
        } catch (Exception e) {
            log.warn("Error in UCLifecycleDaemon doing bundle constraints processing. Will ignore : [{}]", e.toString());
        } finally {
            try {
                JPAUtils.commitTransactionAndClose(em);
            } catch (Exception ex) {
                log.warn("Failed to commit transaction in doCheckBundleConstraints: " + ex.toString());
            }
        }
    }

    private void do3DaysAndNoRechargeWarnings() {
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            // Get all UC's that expired in the last 3 days and the SIM has no other available UC's
            Query q = em.createNativeQuery("SELECT * "
                    + "FROM unit_credit_instance I "
                    + "LEFT JOIN unit_credit_instance HAS_UC ON (I.PRODUCT_INSTANCE_ID=HAS_UC.PRODUCT_INSTANCE_ID AND HAS_UC.EXPIRY_DATE > now() "
                    + "AND HAS_UC.UNITS_REMAINING > 0) "
                    + "WHERE "
                    + "I.EXPIRY_DATE > now() - INTERVAL 72 HOUR "
                    + "AND I.EXPIRY_DATE < now() - INTERVAL 70 HOUR "
                    + "AND HAS_UC.PRODUCT_INSTANCE_ID IS NULL", UnitCreditInstance.class);
            List<UnitCreditInstance> expiredRecentlyList = q.getResultList();
            Collections.shuffle(expiredRecentlyList); // help prevent clashes
            for (UnitCreditInstance uci : expiredRecentlyList) {
                try {
                    IUnitCredit uc = UnitCreditManager.getWrappedUnitCreditInstance(em, uci, null);
                    EventHelper.sendUnitCreditEvent(uc, uc.getPropertyFromConfig("NoRechargeEventSubType"));
                    EventHelper.sendUnitCreditEvent(uc, uc.getPropertyFromConfig("NoRechargeEventSubType1"));
                    EventHelper.sendUnitCreditEvent(uc, uc.getPropertyFromConfig("NoRechargeEventSubType2"));
                } catch (Exception e) {
                    log.debug("Something went wrong notifying of UC not recharging: [{}]", e.toString());
                }
            }
        } catch (Exception e) {
            log.warn("Error in UCLifecycleDaemon doing non recharging processing. Will ignore : [{}]", e.toString());
        } finally {
            try {
                JPAUtils.commitTransactionAndClose(em);
            } catch (Exception ex) {
                log.warn("Failed to commit transaction in do3DaysAndNoRechargeWarnings: " + ex.toString());
            }
        }
    }

    // 5 days no recharge after data expiry
    private void do5DaysAndNoRechargeWarnings() {
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            // Get all UC's that expired in the last 5 days and the SIM has no other available UC's
            Query q = em.createNativeQuery("SELECT * "
                    + "FROM unit_credit_instance I "
                    + "LEFT JOIN unit_credit_instance HAS_UC ON (I.PRODUCT_INSTANCE_ID=HAS_UC.PRODUCT_INSTANCE_ID AND HAS_UC.EXPIRY_DATE > now() "
                    + "AND HAS_UC.UNITS_REMAINING > 0) "
                    + "WHERE "
                    + "I.EXPIRY_DATE > now() - INTERVAL 120 HOUR "
                    + "AND I.EXPIRY_DATE < now() - INTERVAL 118 HOUR "
                    + "AND HAS_UC.PRODUCT_INSTANCE_ID IS NULL", UnitCreditInstance.class);
            List<UnitCreditInstance> expiredRecentlyList = q.getResultList();
            Collections.shuffle(expiredRecentlyList); // help prevent clashes
            for (UnitCreditInstance uci : expiredRecentlyList) {
                try {
                    IUnitCredit uc = UnitCreditManager.getWrappedUnitCreditInstance(em, uci, null);
                    EventHelper.sendUnitCreditEvent(uc, uc.getPropertyFromConfig("NoRecharge5DayEventSubType"));
                    EventHelper.sendUnitCreditEvent(uc, uc.getPropertyFromConfig("NoRecharge5DayEventSubType1"));
                    EventHelper.sendUnitCreditEvent(uc, uc.getPropertyFromConfig("NoRecharge5DayEventSubType2"));
                } catch (Exception e) {
                    log.debug("Something went wrong notifying of UC not recharging: [{}]", e.toString());
                }
            }
        } catch (Exception e) {
            log.warn("Error in UCLifecycleDaemon doing non recharging processing. Will ignore : [{}]", e.toString());
        } finally {
            try {
                JPAUtils.commitTransactionAndClose(em);
            } catch (Exception ex) {
                log.warn("Failed to commit transaction in do5DaysAndNoRechargeWarnings: " + ex.toString());
            }
        }
    }

    // 10 days no recharge after data expiry
    private void do10DaysAndNoRechargeWarnings() {
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            // Get all UC's that expired in the last 3 days and the SIM has no other available UC's
            Query q = em.createNativeQuery("SELECT * "
                    + "FROM unit_credit_instance I "
                    + "LEFT JOIN unit_credit_instance HAS_UC ON (I.PRODUCT_INSTANCE_ID=HAS_UC.PRODUCT_INSTANCE_ID AND HAS_UC.EXPIRY_DATE > now() "
                    + "AND HAS_UC.UNITS_REMAINING > 0) "
                    + "WHERE "
                    + "I.EXPIRY_DATE > now() - INTERVAL 240 HOUR "
                    + "AND I.EXPIRY_DATE < now() - INTERVAL 238 HOUR "
                    + "AND HAS_UC.PRODUCT_INSTANCE_ID IS NULL", UnitCreditInstance.class);
            List<UnitCreditInstance> expiredRecentlyList = q.getResultList();
            Collections.shuffle(expiredRecentlyList); // help prevent clashes
            for (UnitCreditInstance uci : expiredRecentlyList) {
                try {
                    IUnitCredit uc = UnitCreditManager.getWrappedUnitCreditInstance(em, uci, null);
                    EventHelper.sendUnitCreditEvent(uc, uc.getPropertyFromConfig("NoRecharge10DayEventSubType"));
                    EventHelper.sendUnitCreditEvent(uc, uc.getPropertyFromConfig("NoRecharge10DayEventSubType1"));
                    EventHelper.sendUnitCreditEvent(uc, uc.getPropertyFromConfig("NoRecharge10DayEventSubType2"));
                } catch (Exception e) {
                    log.debug("Something went wrong notifying of UC not recharging: [{}]", e.toString());
                }
            }
        } catch (Exception e) {
            log.warn("Error in UCLifecycleDaemon doing non recharging processing. Will ignore : [{}]", e.toString());
        } finally {
            try {
                JPAUtils.commitTransactionAndClose(em);
            } catch (Exception ex) {
                log.warn("Failed to commit transaction in do10DaysAndNoRechargeWarnings: " + ex.toString());
            }
        }
    }

    // 20 days no recharge after data expiry
    private void do20DaysAndNoRechargeWarnings() {
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            // Get all UC's that expired in the last 3 days and the SIM has no other available UC's
            Query q = em.createNativeQuery("SELECT * "
                    + "FROM unit_credit_instance I "
                    + "LEFT JOIN unit_credit_instance HAS_UC ON (I.PRODUCT_INSTANCE_ID=HAS_UC.PRODUCT_INSTANCE_ID AND HAS_UC.EXPIRY_DATE > now() "
                    + "AND HAS_UC.UNITS_REMAINING > 0) "
                    + "WHERE "
                    + "I.EXPIRY_DATE > now() - INTERVAL 480 HOUR "
                    + "AND I.EXPIRY_DATE < now() - INTERVAL 478 HOUR "
                    + "AND HAS_UC.PRODUCT_INSTANCE_ID IS NULL", UnitCreditInstance.class);
            List<UnitCreditInstance> expiredRecentlyList = q.getResultList();
            Collections.shuffle(expiredRecentlyList); // help prevent clashes
            for (UnitCreditInstance uci : expiredRecentlyList) {
                try {
                    IUnitCredit uc = UnitCreditManager.getWrappedUnitCreditInstance(em, uci, null);
                    EventHelper.sendUnitCreditEvent(uc, uc.getPropertyFromConfig("NoRecharge20DayEventSubType"));
                    EventHelper.sendUnitCreditEvent(uc, uc.getPropertyFromConfig("NoRecharge20DayEventSubType1"));
                    EventHelper.sendUnitCreditEvent(uc, uc.getPropertyFromConfig("NoRecharge20DayEventSubType2"));
                } catch (Exception e) {
                    log.debug("Something went wrong notifying of UC not recharging: [{}]", e.toString());
                }
            }
        } catch (Exception e) {
            log.warn("Error in UCLifecycleDaemon doing non recharging processing. Will ignore : [{}]", e.toString());
        } finally {
            try {
                JPAUtils.commitTransactionAndClose(em);
            } catch (Exception ex) {
                log.warn("Failed to commit transaction in do20DaysAndNoRechargeWarnings: " + ex.toString());
            }
        }
    }

    // 25 days no recharge after data expiry
    private void do30DaysAndNoRechargeWarnings() {
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            // Get all UC's that expired in the last 3 days and the SIM has no other available UC's
            Query q = em.createNativeQuery("SELECT * "
                    + "FROM unit_credit_instance I "
                    + "LEFT JOIN unit_credit_instance HAS_UC ON (I.PRODUCT_INSTANCE_ID=HAS_UC.PRODUCT_INSTANCE_ID AND HAS_UC.EXPIRY_DATE > now() "
                    + "AND HAS_UC.UNITS_REMAINING > 0) "
                    + "WHERE "
                    + "I.EXPIRY_DATE > now() - INTERVAL 720 HOUR "
                    + "AND I.EXPIRY_DATE < now() - INTERVAL 718 HOUR "
                    + "AND HAS_UC.PRODUCT_INSTANCE_ID IS NULL", UnitCreditInstance.class);
            List<UnitCreditInstance> expiredRecentlyList = q.getResultList();
            Collections.shuffle(expiredRecentlyList); // help prevent clashes
            for (UnitCreditInstance uci : expiredRecentlyList) {
                try {
                    IUnitCredit uc = UnitCreditManager.getWrappedUnitCreditInstance(em, uci, null);
                    EventHelper.sendUnitCreditEvent(uc, uc.getPropertyFromConfig("NoRecharge30DayEventSubType"));
                    EventHelper.sendUnitCreditEvent(uc, uc.getPropertyFromConfig("NoRecharge30DayEventSubType1"));
                    EventHelper.sendUnitCreditEvent(uc, uc.getPropertyFromConfig("NoRecharge30DayEventSubType2"));
                } catch (Exception e) {
                    log.debug("Something went wrong notifying of UC not recharging: [{}]", e.toString());
                }
            }
        } catch (Exception e) {
            log.warn("Error in UCLifecycleDaemon doing non recharging processing. Will ignore : [{}]", e.toString());
        } finally {
            try {
                JPAUtils.commitTransactionAndClose(em);
            } catch (Exception ex) {
                log.warn("Failed to commit transaction in do30DaysAndNoRechargeWarnings: " + ex.toString());
            }
        }
    }

    @Override
    public void propsHaveChangedTrigger() {
    }
}
