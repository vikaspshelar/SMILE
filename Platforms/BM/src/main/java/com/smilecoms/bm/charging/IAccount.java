/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.charging;

import com.smilecoms.bm.db.model.ServiceInstance;
import java.math.BigDecimal;
import java.util.Date;

/**
 *
 * @author paul
 */
public interface IAccount {

    public abstract long getAccountId();

    public abstract BigDecimal getCurrentBalanceCents();

    public abstract BigDecimal getAvailableBalanceCents();

    public abstract int getStatus();

    public abstract boolean isLockedInDB();

    public abstract boolean canTakeACharge(String ratingGroup);

    public BigDecimal chargeForUsage(BigDecimal amntCents, String ratingGroup) throws Exception;

    public ReserveResult reserve(
            BigDecimal centsToReserve,
            String sessionId,
            Date eventTimetamp,
            byte[] request,
            int reservationSecs,
            ServiceInstance serviceInstance,
            String ratingGroup,
            boolean checkOnly) throws Exception;

    public void debitBalance(BigDecimal amntCents) throws InsufficientFundsException, Exception;

    public void creditBalance(BigDecimal amntCents) throws Exception;

    public void transferAmountToAccount(IAccount destAcc, BigDecimal amntCents) throws InsufficientFundsException, Exception;

    public void setStatus(int newStatus, String user) throws Exception;

    public boolean supportsOperationType(ACCOUNT_OPERATION_TYPE opType);

    public boolean unitCreditChargingBarred();
    
    public static enum ACCOUNT_OPERATION_TYPE {

        CREDIT,
        DEBIT,
        UNIT_CREDIT_CHARGE,
        MONETARY_CHARGE,
        USER_STATUS_CHANGE,
        ADMIN_STATUS_CHANGE,
        TRANSFER,
        ONNET_WALLET_CHARGE,
        LOCAL_WALLET_CHARGE,
        INTERNATIONAL_WALLET_CHARGE,
        DATA_WALLET_CHARGE,
        SMILE_ON
    };

}
