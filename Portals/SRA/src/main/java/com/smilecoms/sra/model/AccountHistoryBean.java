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
public class AccountHistoryBean {
    
    private List<TransactionRecordBean> transactionRecords;
    private int resultsReturned;

    public List<TransactionRecordBean> getTransactionRecords() {
        return transactionRecords;
    }

    public void setTransactionRecords(List<TransactionRecordBean> transactionRecords) {
        this.transactionRecords = transactionRecords;
    }

    public int getResultsReturned() {
        return resultsReturned;
    }

    public void setResultsReturned(int resultsReturned) {
        this.resultsReturned = resultsReturned;
    }
    
    
    
}
