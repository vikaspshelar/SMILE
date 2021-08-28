/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.model;

import java.util.List;

/**
 *
 * @author rajeshkumar
 */
public class ShareUnitCreditRequest {

    private long sourceAccountId;
    private List<Long> targetAccounts;
    private int unitCreditInstanceId;
    private int targetProductInstanceId;   
    //Units in Bytes
    private double units;

    public List<Long> getTargetAccounts() {
        return targetAccounts;
    }

    public void setTargetAccounts(List<Long> targetAccounts) {
        this.targetAccounts = targetAccounts;
    }

    public int getUnitCreditInstanceId() {
        return unitCreditInstanceId;
    }

    public void setUnitCreditInstanceId(int unitCreditInstanceId) {
        this.unitCreditInstanceId = unitCreditInstanceId;
    }

    public int getTargetProductInstanceId() {
        return targetProductInstanceId;
    }

    public void setTargetProductInstanceId(int targetProductInstanceId) {
        this.targetProductInstanceId = targetProductInstanceId;
    }

    public double getUnits() {
        return units;
    }

    public void setUnits(double units) {
        this.units = units;
    }

    public long getSourceAccountId() {
        return sourceAccountId;
    }

    public void setSourceAccountId(long sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
    }
}
