/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.am.np;

import com.smilecoms.commons.base.BaseUtils;
import java.util.Set;

/**
 *
 * @author mukosi
 */
public class InvalidStateTransitionException extends Exception {

    String message = null; // Exception Message
    String npOrderID;
    String currentState;
    String inputMessageType;
    boolean sendTrap;

    public String getNpOrderID() {
        return npOrderID;
    }

    public void setNpOrderID(String npOrderID) {
        this.npOrderID = npOrderID;
    }

    public String getCurrentState() {
        return currentState;
    }

    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }

    public String getInputMessageType() {
        return inputMessageType;
    }

    public void setInputMessageType(String inputMessageType) {
        this.inputMessageType = inputMessageType;
    }
    
    public InvalidStateTransitionException(boolean sendTrap, String npOrderID, String currentState, String inputMessageType) {
        this.sendTrap = sendTrap;
        this.npOrderID = npOrderID;
        this.currentState = currentState;
        this.inputMessageType = inputMessageType;
    }

    public void processException() {
        if (sendTrap) {
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "MNP", getMessage());
        }
    }


    @Override
    public String getMessage() {
        if (message != null) {
            return message;
        }
        message = "Invalid state transition detected, do not know how to transition from state " + currentState + ", using the received message type [" + inputMessageType + "] for NPOrderID [" + npOrderID + "]";
        return message;
    }
}
