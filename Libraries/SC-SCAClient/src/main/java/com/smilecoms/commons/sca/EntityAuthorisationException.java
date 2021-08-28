/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.sca;

import com.smilecoms.commons.base.BaseUtils;
import java.util.Set;

/**
 *
 * @author paul
 */
public class EntityAuthorisationException extends Exception {

    String msg = null;
    String identity;
    String methodName;
    String entityType;
    long entityKey;
    Set<String> roles;
    boolean sendTrap;

    public EntityAuthorisationException(boolean sendTrap) {
        this.sendTrap = sendTrap;
    }

    public void processException() {
        if (sendTrap) {
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "SCA", getMessage());
        }
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public void setEntityKey(long entityKey) {
        this.entityKey = entityKey;
    }

    @Override
    public String getMessage() {
        if (msg != null) {
            return msg;
        }
        msg = identity + " tried to access " + entityType + " " + entityKey + " via a SCA call to " + methodName + ". This was forbidden. Callers roles [";
        for (String role : roles) {
            msg = msg + role + " ";
        }
        msg = msg.trim() + "]";
        return msg;
    }
}
