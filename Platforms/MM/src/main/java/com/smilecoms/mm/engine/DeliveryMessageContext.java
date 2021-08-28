/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.engine;

import com.smilecoms.commons.base.BaseUtils;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class DeliveryMessageContext implements Serializable {

    private static final long serialVersionUID = 1L;
    private List<DeliveryState> pipelineStates;
    private long expires;
    private static final Logger log = LoggerFactory.getLogger(DeliveryMessageContext.class);
    static final int NEW = 0;
    static final int PROCESSING = 1;
    static final int READY_FOR_RETRY = 2;
    static final int FINISHED = 3;
    static final int RETRY = 4;
    private int messageStatus = NEW;
    private transient Lock lock = new ReentrantLock();
    private short retriesAfterTimeout = 0;
    private boolean mustCharge = true;
    private boolean onlyPendingDeliveryReport = false;

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        lock = new ReentrantLock();
    }
    
    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }

    int getMessageStatus() {
        return messageStatus;
    }

    boolean expiresBefore(long msAfterEpoch) {
        return expires < msAfterEpoch;
    }

    void setExpiryRelative(long ms) {
        this.expires = System.currentTimeMillis() + ms;
    }

    void setPluginSuccess(String name) {
        for (DeliveryState state : pipelineStates) {
            if (state.getPluginClass().equals(name)) {
                state.setDone();
                updateMessageStatus();
                break;
            }
        }
    }

    void setPluginFailure(String name, RetryTrigger trigger) {
        for (DeliveryState state : pipelineStates) {
            if (state.getPluginClass().equals(name)) {
                state.setFailed();
                state.setRetryTrigger(trigger);
                updateMessageStatus();
                break;
            }
        }
    }

    void setPluginProcessing(String name) {
        for (DeliveryState state : pipelineStates) {
            if (state.getPluginClass().equals(name)) {
                state.setInProgress();
                updateMessageStatus();
                break;
            }
        }
    }

    void setPipelineConfig(List<String> plugins) {
        if (pipelineStates == null) {
            pipelineStates = new ArrayList<>();
            for (String plugin : plugins) {
                DeliveryState state = new DeliveryState();
                state.setPluginClass(plugin);
                state.setPending();
                pipelineStates.add(state);
            }
        }
    }

    boolean hasConfig() {
        return pipelineStates != null;
    }

    List<String> getPendingPluginNames() {
        List<String> ret = new ArrayList<>();
        for (DeliveryState state : pipelineStates) {
            if (state.isPending()) {
                ret.add(state.getPluginClass());
            }
        }
        return ret;
    }

    List<RetryTrigger> getRetryTriggers() {
        List<RetryTrigger> ret = new ArrayList<>();
        for (DeliveryState state : pipelineStates) {
            if (state.isPending() && state.getRetryTrigger() != null) {
                ret.add(state.getRetryTrigger());
            }
        }
        return ret;
    }

    int getPluginFailureCount(String plugin) {
        for (DeliveryState state : pipelineStates) {
            if (state.getPluginClass().equals(plugin)) {
                return state.getFailCount();
            }
        }
        return 0;
    }

    private void updateMessageStatus() {
        if (messageStatus == READY_FOR_RETRY) {
            throw new RuntimeException("This message cannot change status from READY_FOR_RETRY");
        } else if (messageStatus == FINISHED) {
            throw new RuntimeException("This message cannot change status from FINISHED");
        }

        boolean anyFailed = false;
        for (DeliveryState state : pipelineStates) {
            if (state.isInProgress() || state.isPending()) {
                messageStatus = PROCESSING;
                return;
            } else if (state.isFailed()) {
                anyFailed = true;
            }
        }
        if (anyFailed) {
            messageStatus = READY_FOR_RETRY;
        } else {
            //logStates();
            messageStatus = FINISHED;
        }
    }

    void prepareForRequeue() {
        messageStatus = RETRY;
        for (DeliveryState state : pipelineStates) {
            if (state.isFailed()) {
                state.setPending();
            }
        }
    }

    void prepareForRerouting() {
        messageStatus = RETRY;
        for (DeliveryState state : pipelineStates) {
            if (state.isFailed() || state.isInProgress()) {
                state.setPending();
            }
        }
    }

    void prepareForDeliveryReport() {
        onlyPendingDeliveryReport = true;
    }

    boolean isOnlyPendingDeliveryReport() {
        return onlyPendingDeliveryReport;
    }

    void prepareForRequeueAfterTimeout() throws Exception {
        messageStatus = RETRY;
        int retryLimit = BaseUtils.getIntProperty("env.mm.plugin.timeout.retries", 3);
        if (retriesAfterTimeout >= retryLimit) {
            throw new Exception("Timeout Retries exceeded");
        }
        retriesAfterTimeout++;
        log.debug("This is retry [{}] out of max [{}]", retriesAfterTimeout, retryLimit);
        for (DeliveryState state : pipelineStates) {
            if (state.isFailed() || state.isInProgress()) {
                log.debug("After timeout, plugin [{}] will be reprocessed", state.getPluginClass());
                state.setPending();
            }
        }
    }

    @Override
    public String toString() {
        String statusString;
        switch (messageStatus) {
            case NEW:
                statusString = "NEW";
                break;
            case PROCESSING:
                statusString = "PROCESSING";
                break;
            case READY_FOR_RETRY:
                statusString = "READY_FOR_RETRY";
                break;
            case FINISHED:
                statusString = "FINISHED";
                break;
            case RETRY:
                statusString = "RETRY";
                break;
            default:
                statusString = "UNKNOWN - " + messageStatus;
        }
        String ret = "Delivery Status is [" + statusString + "]";
        if (pipelineStates != null) {
            for (DeliveryState state : pipelineStates) {
                ret += " Plugin [" + state.getPluginClass() + "] is in state [" + state.getStatus() + "]";
            }
        }
        return ret;
    }

    int getCurrentFailureCount() {
        int ret = 0;
        for (DeliveryState state : pipelineStates) {
            ret += state.getFailCount();
        }
        return ret;
    }

    public boolean mustCharge() {
        return mustCharge;
    }

    public void setMustCharge(boolean mustCharge) {
        this.mustCharge = mustCharge;
    }

}
