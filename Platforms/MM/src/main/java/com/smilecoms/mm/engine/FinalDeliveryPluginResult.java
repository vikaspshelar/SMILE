/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.engine;

/**
 *
 * @author paul
 */
public class FinalDeliveryPluginResult extends DeliveryPluginResult{
    
    private RetryTrigger triggerEvent = null;
    private boolean mustRetry = false;
    private String pluginClassName;
    private boolean permanentDeliveryFailure = false;

    public String getPluginClassName() {
        return pluginClassName;
    }

    public void setPluginClassName(String pluginClassName) {
        this.pluginClassName = pluginClassName;
    }

    public boolean mustRetry() {
        return mustRetry;
    }

    public void setMustRetry(boolean mustRetry) {
        this.mustRetry = mustRetry;
    }

    public RetryTrigger getRetryTrigger() {
        return triggerEvent;
    }

    public void setRetryTrigger(RetryTrigger triggerEvent) {
        this.triggerEvent = triggerEvent;
    }

    public boolean isPermanentDeliveryFailure() {
        return permanentDeliveryFailure;
    }

    public void setPermanentDeliveryFailure(boolean permanentDeliveryFailure) {
        this.permanentDeliveryFailure = permanentDeliveryFailure;
    }

}
