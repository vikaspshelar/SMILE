/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.engine;

import java.io.Serializable;

/**
 *
 * @author paul
 */
public class RetryTrigger implements Serializable{

    public static final int UNSET = -1;
    public static final int SIP_REGISTER = 1;
    private int type = UNSET;
    private String triggerKey;

    public void setTriggerType(int type) {
        this.type = type;
    }

    public int getTriggerType() {
        return type;
    }

    @Override
    public String toString() {
        switch (type) {
            case UNSET:
                return "UNSET";
            case SIP_REGISTER:
                return "SIP_REGISTER";
        }
        return "UNSET";
    }

    public String getTriggerKey() {
        return triggerKey;
    }

    public void setTriggerKey(String triggerKey) {
        this.triggerKey = triggerKey;
    }
    
}
