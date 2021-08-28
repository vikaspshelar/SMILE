/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.db.interconnect.op;

/**
 *
 * @author paul
 */
public class RoutingInfo {
    private final String fromTrunkId;
    private final String toTrunkId;
    private final String messageClass;
    
    public RoutingInfo(String fromTrunkId, String toTrunkId, String messageClass) {
        this.fromTrunkId = fromTrunkId;
        this.toTrunkId = toTrunkId;
        this.messageClass = messageClass;
    }

    public String getFromTrunkId() {
        return fromTrunkId;
    }

    public String getToTrunkId() {
        return toTrunkId;
    }

    public String getMessageClass() {
        return messageClass;
    }

}
