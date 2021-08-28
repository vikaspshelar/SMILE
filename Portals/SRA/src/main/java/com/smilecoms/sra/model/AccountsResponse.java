/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.model;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author rajeshkumar
 */
public class AccountsResponse {
    
    private List<AppAccount> accounts = new ArrayList<>(); 
    private int notifications;

    public int getNotifications() {
        return notifications;
    }

    public void setNotifications(int notifications) {
        this.notifications = notifications;
    }
    public List<AppAccount> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<AppAccount> accounts) {
        this.accounts = accounts;
    }   
    
}
