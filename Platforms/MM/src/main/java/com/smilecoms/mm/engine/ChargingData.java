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
public class ChargingData implements Serializable {

    private String chargingSessionId;
    private String accountIdentifier;
    private String accountIdentifierType;

    public String getChargingSessionId() {
        return chargingSessionId;
    }

    public void setChargingSessionId(String chargingSessionId) {
        this.chargingSessionId = chargingSessionId;
    }

    public String getAccountIdentifier() {
        return accountIdentifier;
    }

    public void setAccountIdentifier(String accountIdentifier) {
        this.accountIdentifier = accountIdentifier;
    }

    public String getAccountIdentifierType() {
        return accountIdentifierType;
    }

    public void setAccountIdentifierType(String accountIdentifierType) {
        this.accountIdentifierType = accountIdentifierType;
    }
    
    
}
