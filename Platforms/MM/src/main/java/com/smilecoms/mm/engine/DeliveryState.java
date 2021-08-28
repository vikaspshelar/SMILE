/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.engine;

import java.io.Serializable;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class DeliveryState implements Serializable {
    private static final long serialVersionUID=1L;
    private static final Logger log = LoggerFactory.getLogger(DeliveryState.class);
    private String pluginClass;
    private int status;
    private RetryTrigger trigger = null;
    private static final int PENDING = 0;
    private static final int INPROGRESS = 1;
    private static final int FAILED = 2;
    private static final int DONE = 3;
    
    
    public String getStatus() {
        switch (status) {
            case PENDING:
                return "PENDING";
            case INPROGRESS:
                return "INPROGRESS";
            case FAILED:
                return "FAILED";
            case DONE:
                return "DONE";
        }
        return "UNKNOWN";
    }
    private int failCount = 0;

    public String getPluginClass() {
        return pluginClass;
    }

    public void setPluginClass(String pluginClass) {
        this.pluginClass = pluginClass;
    }

    public boolean isPending() {
        return status == PENDING;
    }

    public boolean isInProgress() {
        return status == INPROGRESS;
    }

    public boolean isFailed() {
        return status == FAILED;
    }

    public void setDone() {
        this.status = DONE;
    }

    public void setFailed() {
        this.status = FAILED;
        failCount++;
    }
    
    public void setRetryTrigger(RetryTrigger trigger) {
        this.trigger = trigger;
    }

    public RetryTrigger getRetryTrigger() {
        return trigger;
    }

    
    public void setPending() {
        this.status = PENDING;
    }

    public void setInProgress() {
        this.status = INPROGRESS;
    }

    public int getFailCount() {
        return failCount;
    }
}
