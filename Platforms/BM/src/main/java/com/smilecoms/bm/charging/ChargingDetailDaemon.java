package com.smilecoms.bm.charging;

import com.smilecoms.commons.base.lifecycle.BaseListener;
import org.slf4j.*;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.ExceptionManager;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import java.util.concurrent.LinkedBlockingQueue;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManagerFactory;

@Singleton
@Startup
public class ChargingDetailDaemon implements BaseListener {

    private static final Logger log = LoggerFactory.getLogger(ChargingDetailDaemon.class);
    private static EntityManagerFactory emf = null;
    private ChargingDetailWorker[] workers;

    private static final LinkedBlockingQueue<ChargingDetailRecord> workQueue = new LinkedBlockingQueue<ChargingDetailRecord>(100000);

    public static void enqueueForWriting(ChargingDetailRecord cdr) {
        try {
            workQueue.add(cdr);
            if (log.isDebugEnabled()) {
                log.debug("Charging Detail queue size is now [{}]", workQueue.size());
            }
            if (workQueue.size() > 10000) {
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, ChargingDetailDaemon.class.getName(), "Charging detail queue has exceeded 10000 records");
            }
        } catch (Exception e) {
            log.warn("Error enqueueing ChargingDetailRecord for writing: [{}]", e.toString());
            new ExceptionManager(log).reportError(e);
        }
    }

    @PostConstruct
    public void startUp() {
        emf = JPAUtils.getEMF("BMPU_RL");
        BaseUtils.registerForPropsAvailability(this);
    }

    @Override
    public void propsAreReadyTrigger() {
        log.warn("Charging Detail Daemon is starting up as properties are ready");
        BaseUtils.deregisterForPropsAvailability(this);
        BaseUtils.registerForPropsChanges(this);
        startupWorkers();
    }

    @PreDestroy
    public void shutDown() {
        BaseUtils.deregisterForPropsChanges(this);
        shutdownWorkers();
        JPAUtils.closeEMF(emf);
    }

    @Override
    public void propsHaveChangedTrigger() {
        if (workers.length != BaseUtils.getIntProperty("env.bm.charging.detail.worker.thread.count", 1)) {
            log.warn("BM Charging Detail thread pool size configuration has changed from [{}] to [{}] so pool will be restarted", workers.length, BaseUtils.getIntProperty("env.bm.charging.detail.worker.thread.count", 1));
            shutdownWorkers();
            startupWorkers();
        } else {
            for (ChargingDetailWorker worker : workers) {
                worker.reloadConfig();
            }
        }
    }

    private void startupWorkers() {
        log.warn("Starting Charging Detail Daemon workers");

        if (emf == null) {
            log.warn("EMF is null??? Closing and reopening it");
            JPAUtils.closeEMF(emf);
            emf = JPAUtils.getEMF("BMPU_RL");
        }

        int workerThreadCount = BaseUtils.getIntProperty("env.bm.charging.detail.worker.thread.count", 1);
        workers = new ChargingDetailWorker[workerThreadCount];
        for (int i = 0; i < workerThreadCount; i++) {
            workers[i] = new ChargingDetailWorker(emf, workQueue);
            Thread t = new Thread(workers[i]);
            t.setName("Smile-BM-ChargingDetailWorker-" + i);
            t.start();
            log.debug("Started thread [{}]", t.getName());
        }
        log.warn("Charging Detail Daemon workers startup complete with [{}] threads", workerThreadCount);

    }

    private void shutdownWorkers() {
        if (workers == null) {
            return;
        }
        log.warn("Charging Detail Daemon is shutting down all workers...");
        for (ChargingDetailWorker worker : workers) {
            worker.shutDown();
        }
        boolean allAreStopped = false;
        int loops = 0;
        while (!allAreStopped && loops < 1000) {
            loops++;
            allAreStopped = true;
            for (ChargingDetailWorker worker : workers) {
                if (!worker.isStopped()) {
                    log.warn("Waiting for a charging detail worker [{}] to stop", worker);
                    Utils.sleep(10);
                    allAreStopped = false;
                    break;
                }
            }
        }
        log.warn("Charging Detail Daemon workers shutdown complete. Did all stop? [{}]", allAreStopped);
    }

}
