/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.plugins.onnetsms;

import com.smilecoms.mm.plugins.onnetsms.reginfo.ietf.params.xml.ns.reginfo.Reginfo;
import gov.nist.javax.sip.header.ims.PathList;
import java.io.Serializable;
import java.util.HashMap;
import javax.sip.address.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jaybeepee
 */
public class Subscription implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(Subscription.class);
    private Address scscfSIPAddress;
    private Address targetSIPAddress;
    private final HashMap<String, Impu> impus;
    private String localTag;
    private String remoteTag;

//    private ImpuList impuList;
    public enum State {

        init, subscribing, active, failed, inactive, unsubscribing
    };
    private State state;
    private int retries;
    private String dialogID;
    private String branchID;
    private long expiresEpoch;
    private Reginfo regInfo;
    private int cseq;
    private String callID;
    private PathList path;

    public String getCallID() {
        return callID;
    }

    public void setCallID(String callID) {
        this.callID = callID;
    }

    String getRemoteTag() {
        return remoteTag;
    }

    int getCSeq() {
        return cseq;
    }

    void setCSeq(int cseq) {
        this.cseq = cseq;
    }

    int getNextCSeq() {
        cseq++;
        return cseq;
    }

    void setRemoteTag(String remoteTag) {
        this.remoteTag = remoteTag;
    }

    public Subscription(Address scscfSIPAddress, Address targetSIPAddress, long expires) {
        this.scscfSIPAddress = scscfSIPAddress;
        this.targetSIPAddress = targetSIPAddress;
        this.expiresEpoch = System.currentTimeMillis() + (expires * 1000);
        this.state = State.init;
        impus = new HashMap<>();
    }

    public int getNumRegistrations() {
        return impus.size();
    }

    public Address getScscfSIPAddress() {
        return scscfSIPAddress;
    }

    public void setScscfSIPAddress(Address scscfSIPAddress) {
        this.scscfSIPAddress = scscfSIPAddress;
    }

    public Address getTargetSIPAddress() {
        return targetSIPAddress;
    }

    public void setTargetSIPAddress(Address targetSIPAddress) {
        this.targetSIPAddress = targetSIPAddress;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        //TODO update subscription in DB
        this.state = state;
    }

    public void incrementRetries() {
        this.retries++;
    }

    void resetRetries() {
        this.retries = 0;
    }

    public String getDialogID() {
        return dialogID;
    }

    public void setDialogID(String dialogID) {
        this.dialogID = dialogID;
    }

    public String getBranchID() {
        return branchID;
    }

    public void setBranchID(String branchId) {
        this.branchID = branchId;
    }

    public long getExpiresEpoch() {
        return expiresEpoch;
    }

    public void setExpires(long expiresInSecs) {
        this.expiresEpoch = System.currentTimeMillis() + (expiresInSecs * 1000);
    }

    public void setExpiresEpoch(long epoch) {
        this.expiresEpoch = epoch;
    }

    public void addImpu(Impu impu) {
        impus.put(impu.getAor(), impu);
    }

    public Impu getImpu(String aor) {
        return impus.get(aor);
    }

    public void removeImpu(Impu impu) {
        impus.remove(impu.getAor());
    }

    public HashMap<String, Impu> getImpuList() {
        return impus;
    }

    public Reginfo getRegInfo() {
        return regInfo;
    }

    void resetSubscription() {
        log.debug("Resetting Subscription [{}] in state [{}]", this.getTargetSIPAddress().getURI().toString(), this.getState().toString());
        this.callID = null;
        this.remoteTag = null;
        this.cseq = 0;
    }

    public PathList getPath() {
        return path;
    }

    public void setPath(PathList path) {
        this.path = path;
    }

    public String getLocalTag() {
        return this.localTag;
    }

    public void setLocalTag(String localTag) {
        this.localTag = localTag;
    }

}
