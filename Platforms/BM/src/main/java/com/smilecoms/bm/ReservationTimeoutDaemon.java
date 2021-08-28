package com.smilecoms.bm;

import com.smilecoms.bm.db.model.AccountHistory;
import com.smilecoms.bm.db.model.Reservation;
import com.smilecoms.bm.db.op.DAO;
import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;
import com.smilecoms.commons.util.JPAUtils;
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
import javax.persistence.LockModeType;
import javax.persistence.Query;
import org.slf4j.*;

@Singleton
@Startup
@Local({BaseListener.class})
public class ReservationTimeoutDaemon implements BaseListener {

    private static final Logger log = LoggerFactory.getLogger(ReservationTimeoutDaemon.class.getName());
    private EntityManagerFactory emf = null;
    private EntityManager em;
    private static ScheduledFuture runner = null;

    @PostConstruct
    public void startUp() {
        BaseUtils.registerForPropsAvailability(this);
        emf = JPAUtils.getEMF("BMPU_RL");
    }

    @Override
    public void propsAreReadyTrigger() {
        runner = Async.scheduleAtFixedRate(new SmileBaseRunnable("BM.reservationTimeoutDaemon") {
            @Override
            public void run() {
                trigger();
            }
        }, 10000, 50000, 70000);
    }

    @PreDestroy
    public void shutDown() {
        BaseUtils.deregisterForPropsAvailability(this);
        if (runner != null) {
            runner.cancel(false);
        }
        JPAUtils.closeEMF(emf);
    }

    /**
     * Run any batches that are due
     */
    private void trigger() {
        try {

            if (log.isDebugEnabled()) {
                log.debug("ReservationTimeoutDaemon triggered by thread {} on class {}", new Object[]{Thread.currentThread().getId(), this.toString()});
            }

            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            Query q = em.createNativeQuery("select * from reservation where RESERVATION_EXPIRY_TIMESTAMP < NOW() - INTERVAL ? SECOND limit 1000", Reservation.class); // give a 2 minute window for late sessions
            q.setParameter(1, BaseUtils.getIntProperty("env.bm.reservation.expiry.grace.seconds", 600));
            List<Reservation> expiredList = q.getResultList();
            JPAUtils.commitTransaction(em);
            Collections.shuffle(expiredList); // Prevent lock contentions from many JVM's running this
            for (Reservation expired : expiredList) {
                try {
                    JPAUtils.beginTransaction(em);
                    em.refresh(expired, LockModeType.PESSIMISTIC_READ);
                    if (log.isDebugEnabled()) {
                        log.debug("Dealing with an expired reservation for Session [{}] that Expired at [{}] for Account [{}] and Amount [{}]c and Unit Credits [{}] of Instance [{}]", new Object[]{expired.getReservationPK().getSessionId(), expired.getReservationExpiryTimestamp(), expired.getReservationPK().getAccountId(), expired.getAmountCents(), expired.getAmountUnitCredits(), expired.getReservationPK().getUnitCreditInstanceId()});
                    }
                    try {
                        AccountHistory ah = DAO.getOldestAccountHistoryByExtTxId(em, expired.getReservationPK().getSessionId());
                        ah.setStatus("NF");
                        em.persist(ah);
                        em.flush();
                    } catch (javax.persistence.NoResultException e) {
                        log.warn("This expired session did not even have a related billing history record!");
                    }
                    em.remove(expired);
                    JPAUtils.commitTransaction(em);
                    String msg = String.format("BM has an expired account reservation that is being removed. What happened? Session [%s] that Expired at [%s] for Account [%d] and Amount [%f]c", expired.getReservationPK().getSessionId(), expired.getReservationExpiryTimestamp().toString(), expired.getReservationPK().getAccountId(), expired.getAmountCents().doubleValue());
                    BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "BM", msg);
                    log.warn(msg);
                } catch (Exception e) {
                    log.debug("Something else expired the reservation: [{}]", e.toString());
                }
            }
        } catch (Exception e) {
            log.warn("Error in ReservationTimeoutDaemon. Will ignore : [{}]", e.toString());
        } finally {
            try {
                JPAUtils.closeEM(em);
            } catch (Exception ex) {
                log.warn("Failed to commit transaction in ReservationTimeoutDaemon: " + ex.toString());
            }
        }

    }

    @Override
    public void propsHaveChangedTrigger() {
    }
}
