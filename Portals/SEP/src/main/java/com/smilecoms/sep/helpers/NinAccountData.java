/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sep.helpers;

/**
 *
 * @author xolani.mabuza
 */
public class NinAccountData {
    private int customerId;
    private long accountId;
    private String status;
    private String nin;
    private String accountType;
    private int accountOrg;

    public NinAccountData() {
    }

        
        public int getCustomerId() {
            return customerId;
        }

        public void setCustomerId(int customerId) {
            this.customerId = customerId;
        }

        public long getAccountId() {
            return accountId;
        }

        public void setAccountId(long accountId) {
            this.accountId = accountId;
        }

        public java.lang.String getStatus() {
            return status;
        }

        public void setStatus(java.lang.String status) {
            this.status = status;
        }

        public String getNin() {
            return nin;
        }

        public void setNin(String nin) {
            this.nin = nin;
        }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public int getAccountOrg() {
        return accountOrg;
    }

    public void setAccountOrg(int accountOrg) {
        this.accountOrg = accountOrg;
    }

    @Override
    public String toString() {
        return "NinAccountData{" + "customerId=" + customerId + ", accountId=" + accountId + ", status=" + status + ", nin=" + nin + ", accountType=" + accountType + ", accountOrg=" + accountOrg + '}';
    }
    
    
    
}