/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.scp.helpers.etranzact;

import com.smilecoms.commons.sca.BalanceTransferData;
import com.smilecoms.commons.sca.SCAContext;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.base.cache.CacheHelper;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class ETransactionManager {
    
    private static final Logger log = LoggerFactory.getLogger(ETransactionManager.class);
    
    public static TransactionData startTransaction(double amountInCents, long recipientAccountId, HttpServletRequest request) throws Exception {
        log.debug("In startTransaction for amount [{}]c and recipient account [{}]", amountInCents, recipientAccountId);
        TransactionData data = new TransactionData(amountInCents, recipientAccountId, request);
        log.debug("Created Transaction. Data is [{}]", data);
        CacheHelper.putInLocalCache(data.getTransactionId(), data, 600);
        return data;
    }
    
    public static TransactionData commitTransaction(int success, String finalCheckSum, String transactionId) throws Exception {
        TransactionData data = getTransaction(transactionId);
        data.setSuccess(success);
        if (data.isFinalCheckSumCorrect(finalCheckSum)) {
            log.debug("Final checksum is correct. Going to transfer for data [{}]", data);
            BalanceTransferData btd = new BalanceTransferData();
            btd.setSCAContext(new SCAContext());
            btd.getSCAContext().setTxId(data.getTransactionId());
            btd.setAmountInCents(data.getAmountInCents());
            btd.setSourceAccountId(data.getETranzactAccountId());
            btd.setTargetAccountId(data.getRecipientAccountId());
            SCAWrapper.getUserSpecificInstance().transferBalance(btd);
            log.debug("Finished doing transfer");
        } else {
            log.warn("Expected checksum [{}] does not match passed in checksum [{}] for status [{}]. Data is [{}]", new Object[]{data.getCorrectFinalCheckSum(), finalCheckSum, success, data});
        }
        return data;
    }
    
    private static TransactionData getTransaction(String transactionId) throws Exception {
        log.debug("Looking for a transaction with Id [{}]", transactionId);
        TransactionData data = CacheHelper.getFromLocalCache(transactionId, TransactionData.class);
        log.debug("Got the transaction data [{}] for transaction Id [{}]", data, transactionId);
        return data;
    }
    
}
