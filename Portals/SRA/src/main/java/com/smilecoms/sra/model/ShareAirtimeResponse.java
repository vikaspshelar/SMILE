/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.model;

import com.smilecoms.commons.sca.Account;

/**
 *
 * @author rajeshkumar
 */
public class ShareAirtimeResponse {
    
    private long accountId;
    private double availableBalanceInCents;
    private double currentBalanceInCents;
    private int status;
    
    public ShareAirtimeResponse(Account acc){
        this.accountId = acc.getAccountId();
        this.availableBalanceInCents = acc.getAvailableBalanceInCents();
        this.currentBalanceInCents = acc.getCurrentBalanceInCents();
        this.status = acc.getStatus();
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public double getAvailableBalanceInCents() {
        return availableBalanceInCents;
    }

    public void setAvailableBalanceInCents(double availableBalanceInCents) {
        this.availableBalanceInCents = availableBalanceInCents;
    }

    public double getCurrentBalanceInCents() {
        return currentBalanceInCents;
    }

    public void setCurrentBalanceInCents(double currentBalanceInCents) {
        this.currentBalanceInCents = currentBalanceInCents;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }     
}
