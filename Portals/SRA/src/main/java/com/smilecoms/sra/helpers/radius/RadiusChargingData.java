/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.helpers.radius;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 *
 * @author paul
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RadiusChargingData {

    RadiusAVP userName;
    RadiusAVP NASIPAddress;
    RadiusAVP NASIdentifier;
    RadiusAVP NASPortType;
    RadiusAVP framedIPAddress;
    RadiusAVP calledStationId;
    RadiusAVP acctStatusType;
    RadiusAVP acctInputOctets;
    RadiusAVP acctOutputOctets;
    RadiusAVP eventTimestamp;
    RadiusAVP acctUniqueSessionId;
    RadiusAVP callingStationId;
    RadiusAVP acctTerminateCause;

    @JsonProperty("User-Name")
    public RadiusAVP getUserName() {
        return userName;
    }

    @JsonProperty("User-Name")
    public void setUserName(RadiusAVP userName) {
        this.userName = userName;
    }

    @JsonProperty("NAS-IP-Address")
    public RadiusAVP getNASIPAddress() {
        return NASIPAddress;
    }

    @JsonProperty("NAS-IP-Address")
    public void setNASIPAddress(RadiusAVP NASIPAddress) {
        this.NASIPAddress = NASIPAddress;
    }

    @JsonProperty("NAS-Identifier")
    public RadiusAVP getNASIdentifier() {
        return NASIdentifier;
    }

    @JsonProperty("NAS-Identifier")
    public void setNASIdentifier(RadiusAVP NASIdentifier) {
        this.NASIdentifier = NASIdentifier;
    }

    @JsonProperty("NAS-Port-Type")
    public RadiusAVP getNASPortType() {
        return NASPortType;
    }

    @JsonProperty("NAS-Port-Type")
    public void setNASPortType(RadiusAVP NASPortType) {
        this.NASPortType = NASPortType;
    }

    @JsonProperty("Framed-IP-Address")
    public RadiusAVP getFramedIPAddress() {
        return framedIPAddress;
    }

    @JsonProperty("Framed-IP-Address")
    public void setFramedIPAddress(RadiusAVP framedIPAddress) {
        this.framedIPAddress = framedIPAddress;
    }

    @JsonProperty("Called-Station-Id")
    public RadiusAVP getCalledStationId() {
        return calledStationId;
    }

    @JsonProperty("Called-Station-Id")
    public void setCalledStationId(RadiusAVP calledStationId) {
        this.calledStationId = calledStationId;
    }

    @JsonProperty("Calling-Station-Id")
    public RadiusAVP getCallingStationId() {
        return callingStationId;
    }

    @JsonProperty("Calling-Station-Id")
    public void setCallingStationId(RadiusAVP callingStationId) {
        this.callingStationId = callingStationId;
    }

    @JsonProperty("Acct-Status-Type")
    public RadiusAVP getAcctStatusType() {
        return acctStatusType;
    }

    @JsonProperty("Acct-Status-Type")
    public void setAcctStatusType(RadiusAVP acctStatusType) {
        this.acctStatusType = acctStatusType;
    }

    @JsonProperty("Acct-Input-Octets")
    public RadiusAVP getAcctInputOctets() {
        return acctInputOctets;
    }

    @JsonProperty("Acct-Input-Octets")
    public void setAcctInputOctets(RadiusAVP acctInputOctets) {
        this.acctInputOctets = acctInputOctets;
    }

    @JsonProperty("Acct-Output-Octets")
    public RadiusAVP getAcctOutputOctets() {
        return acctOutputOctets;
    }

    @JsonProperty("Acct-Output-Octets")
    public void setAcctOutputOctets(RadiusAVP acctOutputOctets) {
        this.acctOutputOctets = acctOutputOctets;
    }

    @JsonProperty("Event-Timestamp")
    public RadiusAVP getEventTimestamp() {
        return eventTimestamp;
    }

    @JsonProperty("Event-Timestamp")
    public void setEventTimestamp(RadiusAVP eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    @JsonProperty("Acct-Unique-Session-Id")
    public RadiusAVP getAcctUniqueSessionId() {
        return acctUniqueSessionId;
    }

    @JsonProperty("Acct-Unique-Session-Id")
    public void setAcctUniqueSessionId(RadiusAVP acctUniqueSessionId) {
        this.acctUniqueSessionId = acctUniqueSessionId;
    }

    @JsonProperty("Acct-Terminate-Cause")
    public RadiusAVP getAcctTerminateCause() {
        return acctTerminateCause;
    }

    @JsonProperty("Acct-Terminate-Cause")
    public void setAcctTerminateCause(RadiusAVP acctTerminateCause) {
        this.acctTerminateCause = acctTerminateCause;
    }

    
}
