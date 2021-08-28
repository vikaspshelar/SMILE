/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.charging;

import com.smilecoms.bm.db.op.DAO;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.ExceptionManager;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Stopwatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class ChargingDetailWorker implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ChargingDetailWorker.class);
    private boolean mustStop = false;
    private boolean isStopped = false;
    EntityManagerFactory emf;
    LinkedBlockingQueue<ChargingDetailRecord> workQueue;

    ChargingDetailWorker(EntityManagerFactory emf, LinkedBlockingQueue<ChargingDetailRecord> workQueue) {
        this.emf = emf;
        this.workQueue = workQueue;
        reloadConfig();
    }

    boolean isStopped() {
        return isStopped;
    }

    public final void reloadConfig() {
    }

    @Override
    public void run() {
        while (!mustStop && emf != null) {
            try {
                process(100);
            } catch (Exception e) {
                log.warn("Error: ", e);
                log.warn("Error in Charging Detail Worker Thread: [{}]", e.toString());
                new ExceptionManager(log).reportError(e);
            }
        }
        if (!mustStop && emf == null) {
            log.warn("For some reason EMF is null and we have not been told to stop");
        }

        // Try our best to empty out the queue if we are exiting and the queue is not empty
        // If after 100000 writes its still not empty, then just exit anyway
        int cnt = 0;
        while (emf != null && !workQueue.isEmpty() && cnt < 100000) {
            cnt++;
            log.warn("Processing the last bit of the charging detail queue. Count is [{}]", cnt);
            process(0);
        }
        if (!workQueue.isEmpty()) {
            log.warn("Exiting charging detail worker thread while the queue still has requests in it. Queue size is [{}]", workQueue.size());
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, ChargingDetailDaemon.class.getName(), "Charging detail worker exited without queue being empty");
        }

        isStopped = true;
        log.warn("ChargingDetailWorker [{}] has stopped", Thread.currentThread().getName());
    }

    public void shutDown() {
        mustStop = true;
    }

    private void process(int waitMillis) {
        try {
            //log.debug("Charging Detail worker is checking for new record in queue");
            ChargingDetailRecord cdr = workQueue.poll(waitMillis, TimeUnit.MILLISECONDS);
            if (cdr != null) {
                processCDR(cdr);
            }
        } catch (Exception e) {
            log.warn("Error trying to write charging detail [{}]", e.toString());
            new ExceptionManager(log).reportError(e);
        }
    }

    private void processCDR(ChargingDetailRecord cdr) {
        if (log.isDebugEnabled()) {
            log.debug("In processCDR");
            Stopwatch.start();
        }
        EntityManager em = null;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            DAO.writeCDR(em, cdr, false);
        } catch (Exception e) {
            log.warn("Error persisting new charging detail record in DB [{}]" + e.toString());
            new ExceptionManager(log).reportError(e);
            try {
                log.warn("Putting CDR back into queue as it was not written properly");
                workQueue.put(cdr);
            } catch (InterruptedException ex) {
                log.warn("Error requeueing cdr: " + e.toString());
            }
        } finally {
            try {
                JPAUtils.commitTransactionAndClose(em);
            } catch (Exception ex) {
                log.warn("Failed to commit transaction after adding charging detail : " + ex.toString());
                new ExceptionManager(log).reportError(ex);
            }
            if (log.isDebugEnabled()) {
                Stopwatch.stop();
                log.debug("Writing charging detail record took [{}]", Stopwatch.millisString());
            }
        }
    }

    
}
