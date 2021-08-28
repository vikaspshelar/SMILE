/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.model;

/**
 *
 * @author rajeshkumar
 */
public class TransactionRecordBean {

    private long transactionRecordId;

    private String source;

    private String destination;

    private String startDate;
    private String endDate;

    private String extTxId;

    private double amountInCents;

    private String sourceDevice;

    private double totalUnits;

    private String termCode;

    private String transactionType;

    private double accountBalanceRemainingInCents;

    private String description;

    private String status;

    private long accountId;

    private int serviceInstanceId;

    private double unitCreditUnits;

    private double unitCreditBaselineUnits;

    private String chargingDetail;

    private String location;

    private String ipAddress;
    private String serviceInstanceIdentifier;
    private String info;

    public long getTransactionRecordId() {
        return transactionRecordId;
    }

    public void setTransactionRecordId(long transactionRecordId) {
        this.transactionRecordId = transactionRecordId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getExtTxId() {
        return extTxId;
    }

    public void setExtTxId(String extTxId) {
        this.extTxId = extTxId;
    }

    public double getAmountInCents() {
        return amountInCents;
    }

    public void setAmountInCents(double amountInCents) {
        this.amountInCents = amountInCents;
    }

    public String getSourceDevice() {
        return sourceDevice;
    }

    public void setSourceDevice(String sourceDevice) {
        this.sourceDevice = sourceDevice;
    }

    public double getTotalUnits() {
        return totalUnits;
    }

    public void setTotalUnits(double totalUnits) {
        this.totalUnits = totalUnits;
    }

    public String getTermCode() {
        return termCode;
    }

    public void setTermCode(String termCode) {
        this.termCode = termCode;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public double getAccountBalanceRemainingInCents() {
        return accountBalanceRemainingInCents;
    }

    public void setAccountBalanceRemainingInCents(double accountBalanceRemainingInCents) {
        this.accountBalanceRemainingInCents = accountBalanceRemainingInCents;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public int getServiceInstanceId() {
        return serviceInstanceId;
    }

    public void setServiceInstanceId(int serviceInstanceId) {
        this.serviceInstanceId = serviceInstanceId;
    }

    public double getUnitCreditUnits() {
        return unitCreditUnits;
    }

    public void setUnitCreditUnits(double unitCreditUnits) {
        this.unitCreditUnits = unitCreditUnits;
    }

    public double getUnitCreditBaselineUnits() {
        return unitCreditBaselineUnits;
    }

    public void setUnitCreditBaselineUnits(double unitCreditBaselineUnits) {
        this.unitCreditBaselineUnits = unitCreditBaselineUnits;
    }

    public String getChargingDetail() {
        return chargingDetail;
    }

    public void setChargingDetail(String chargingDetail) {
        this.chargingDetail = chargingDetail;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getServiceInstanceIdentifier() {
        return serviceInstanceIdentifier;
    }

    public void setServiceInstanceIdentifier(String serviceInstanceIdentifier) {
        this.serviceInstanceIdentifier = serviceInstanceIdentifier;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
    
    
}
