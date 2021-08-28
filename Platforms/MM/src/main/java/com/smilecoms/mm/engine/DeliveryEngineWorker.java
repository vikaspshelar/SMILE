/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.engine;

import com.smilecoms.commons.util.Utils;
import java.util.concurrent.TimeUnit;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class DeliveryEngineWorker implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(DeliveryEngineWorker.class);
    private final EngineQueue queue;
    private final DeliveryEngine engine;
    private boolean mustStop = false;
    private boolean stopped = false;

    public DeliveryEngineWorker(EngineQueue queue, DeliveryEngine engine) {
        this.queue = queue;
        this.engine = engine;
    }

    @Override
    public void run() {
        while (!mustStop) {
            try {
                if (queue == null) {
                    log.warn("Queue is null");
                    return;
                }
                BaseMessage msg = (BaseMessage) queue.poll(1000, TimeUnit.MILLISECONDS);
                if (msg == null) {
                    continue;
                }
                engine.pipelineMessage(msg);
            } catch (Exception e) {
                log.warn("Error: ", e);
                log.warn("Error processing message from delivery queue [{}]", e.toString());
            }
        }
        log.warn("Delivery engine worker has stopped [{}]", Thread.currentThread().getName());
        stopped = true;
    }

    void stop() {
        mustStop = true;
        int tries = 0;
        while (!stopped && tries < 100) {
            tries++;
            log.warn("Waiting for delivery worker to shutdown");
            Utils.sleep(100);
        }
    }
}
