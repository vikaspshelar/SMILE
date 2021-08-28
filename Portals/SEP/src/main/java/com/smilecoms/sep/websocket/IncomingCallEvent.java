/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sep.websocket;

/**
 * This class encapsulates data for an incoming call event as triggered by Asterisk manager interface
 * @author jaybeepee
 */
public class IncomingCallEvent {
    private long epoch;
    private String extension;
    private String callerID;

    public IncomingCallEvent(long epoch, String extension, String callerID) {
        this.epoch = epoch;
        this.extension = extension;
        this.callerID = callerID;
    }
    
    public String getCallerID() {
        return callerID;
    }

    public void setCallerID(String callerID) {
        this.callerID = callerID;
    }

    public long getEpoch() {
        return epoch;
    }

    public void setEpoch(long epoch) {
        this.epoch = epoch;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }
    
    
}
