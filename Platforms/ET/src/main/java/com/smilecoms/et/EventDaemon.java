package com.smilecoms.et;

import com.smilecoms.commons.base.lifecycle.BaseListener;
import org.slf4j.*;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManagerFactory;

@Singleton
@Startup
public class EventDaemon implements BaseListener {

    private static final Logger log = LoggerFactory.getLogger(EventDaemon.class);
    private static EntityManagerFactory emf = null;
    private EventWorker[] workers;
    private ClassLoader classLoader;

    @PostConstruct
    public void startUp() {
        classLoader = this.getClass().getClassLoader();
        emf = JPAUtils.getEMF("ETPU_RL");
        BaseUtils.registerForPropsAvailability(this);
    }

    @Override
    public void propsAreReadyTrigger() {
        log.warn("Event Daemon is starting up as properties are ready");
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
        if (workers.length != BaseUtils.getIntProperty("env.et.worker.thread.count", 2)) {
            log.warn("ET thread pool size configuration has changed from [{}] to [{}] so pool will be restarted", workers.length, BaseUtils.getIntProperty("env.et.worker.thread.count", 2));
            shutdownWorkers();
            startupWorkers();
        } else {
            for (EventWorker worker : workers) {
                worker.reloadConfig();
            }
        }
    }

    private void startupWorkers() {
        log.warn("Starting Event Daemon workers");
        int workerThreadCount = BaseUtils.getIntProperty("env.et.worker.thread.count", 2);
        workers = new EventWorker[workerThreadCount];
        for (int i = 0; i < workerThreadCount; i++) {
            workers[i] = new EventWorker(emf);
            Thread t = new Thread(workers[i]);
            t.setContextClassLoader(classLoader);
            t.setName("Smile-ET-Worker-" + i);
            t.start();
            log.debug("Started thread [{}]", t.getName());
        }
        log.warn("Event Daemon workers startup complete with [{}] threads", workerThreadCount);

    }

    private void shutdownWorkers() {
        if (workers == null) {
            return;
        }
        log.warn("Event Daemon is shutting down all workers...");
        for (EventWorker worker : workers) {
            if (workers != null) {
                worker.shutDown();
            }
        }
        boolean allAreStopped = false;
        int loops = 0;
        while (!allAreStopped && loops < 1000) {
            loops++;
            allAreStopped = true;
            for (EventWorker worker : workers) {
                if (!worker.isStopped()) {
                    log.warn("Waiting for an event worker to stop");
                    Utils.sleep(10);
                    allAreStopped = false;
                    break;
                }
            }
        }
        log.warn("Event Daemon workers shutdown complete. Did all stop? [{}]", allAreStopped);
    }

}
