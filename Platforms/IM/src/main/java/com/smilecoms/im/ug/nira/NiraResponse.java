/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.ug.nira;

/**
 *
 * @author mukosi
 */
public class NiraResponse {
    
    private String   cardStatus;
    private boolean  successful;
    private boolean  matchingStatus;
    private String   transactionStatus;
    private String   error;
    private String   executionCost;
    private String   passwordDaysLeft;
 
    public String getCardStatus() {
        return cardStatus;
    }

    public void setCardStatus(String cardStatus) {
        this.cardStatus = cardStatus;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean status) {
        this.successful = status;
    }
    
    public boolean isMatchingStatus() {
        return matchingStatus;
    }

    public void setMatchingStatus(boolean status) {
        this.matchingStatus = status;
    }

    public String getTransactionStatus() {
        return transactionStatus;
    }

    public void setTransactionStatus(String transactionStatus) {
        this.transactionStatus = transactionStatus;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getExecutionCost() {
        return executionCost;
    }

    public void setExecutionCost(String executionCost) {
        this.executionCost = executionCost;
    }

    public String getPasswordDaysLeft() {
        return passwordDaysLeft;
    }

    public void setPasswordDaysLeft(String passwordDaysLeft) {
        this.passwordDaysLeft = passwordDaysLeft;
    }
    
    public String toString() {
        return "NiraResponse - [TransactionStatus: " + transactionStatus  + ", " + 
                "CardStatus: " + cardStatus + ", " +  
                "Error: " + error + ", " + 
                "ExecutionCost: " + executionCost + ", " +
                "PasswordDaysLeft: " + passwordDaysLeft + 
                "Matching Status:" + matchingStatus + "]";
    }
       
}
