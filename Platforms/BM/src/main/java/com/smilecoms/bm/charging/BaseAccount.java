/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.charging;

import com.smilecoms.bm.db.model.ServiceInstance;
import com.smilecoms.bm.db.op.DAO;
import java.math.BigDecimal;
import java.util.Date;

/**
 *
 * @author paul
 */
public abstract class BaseAccount implements IAccount {

    @Override
    public abstract long getAccountId();

    @Override
    public abstract BigDecimal getCurrentBalanceCents();

    @Override
    public abstract BigDecimal getAvailableBalanceCents();

    @Override
    public abstract int getStatus();

    @Override
    public abstract boolean isLockedInDB();

    @Override
    public abstract boolean canTakeACharge(String ratingGroup);

    // FUNCTIONS THAT CAN CHANGE THE CURRENT OR AVAILABLE BALANCE OR STATUS AND MUST BE VERIFIED AGAINST RESTRICTIONS
    @Override
    public BigDecimal chargeForUsage(BigDecimal amntCents, String ratingGroup) throws Exception {
        if (DAO.isZero(amntCents)) {
            return null;
        }

        // RG 2000 - 2999 Is Onnet Voice/Video/SMS
        // RG 3000 - 3999 Is Local Voice/Video/SMS
        // RG 4000 - 4999 Is International Voice/Video/SMS
        // RG 9000 - 9999 Is General charges by account debit (e.g. for buying bundles)
        if (ratingGroup != null && ratingGroup.length() == 4) {
            char firstChar = ratingGroup.charAt(0);
            switch (firstChar) {
                case '2':
                    checkOperationType(ACCOUNT_OPERATION_TYPE.ONNET_WALLET_CHARGE);
                    break;
                case '3':
                    checkOperationType(ACCOUNT_OPERATION_TYPE.LOCAL_WALLET_CHARGE);
                    break;
                case '4':
                    checkOperationType(ACCOUNT_OPERATION_TYPE.INTERNATIONAL_WALLET_CHARGE);
                    break;
                case '9':
                    checkOperationType(ACCOUNT_OPERATION_TYPE.DEBIT);
                    return null; // Dont bother checking monetary charge
                default:
                    break;
            }
        } else if (ratingGroup != null) {
            checkOperationType(ACCOUNT_OPERATION_TYPE.DATA_WALLET_CHARGE);
        }
        checkOperationType(ACCOUNT_OPERATION_TYPE.MONETARY_CHARGE);
        return null;
    }

    @Override
    public ReserveResult reserve(
            BigDecimal centsToReserve,
            String sessionId,
            Date eventTimetamp,
            byte[] request,
            int reservationSecs,
            ServiceInstance serviceInstance,
            String ratingGroup,
            boolean checkOnly) throws Exception {
        if (centsToReserve != null && DAO.isZero(centsToReserve)) {
            return null;
        }

        // RG 2000 - 2999 Is Onnet Voice/Video/SMS
        // RG 3000 - 3999 Is Local Voice/Video/SMS
        // RG 4000 - 4999 Is International Voice/Video/SMS
        // RG 9000 - 9999 Is General charges by account debit (e.g. for buying bundles)
        if (ratingGroup != null && ratingGroup.length() == 4) {
            char firstChar = ratingGroup.charAt(0);
            switch (firstChar) {
                case '2':
                    checkOperationType(ACCOUNT_OPERATION_TYPE.ONNET_WALLET_CHARGE);
                    break;
                case '3':
                    checkOperationType(ACCOUNT_OPERATION_TYPE.LOCAL_WALLET_CHARGE);
                    break;
                case '4':
                    checkOperationType(ACCOUNT_OPERATION_TYPE.INTERNATIONAL_WALLET_CHARGE);
                    break;
                case '9':
                    checkOperationType(ACCOUNT_OPERATION_TYPE.DEBIT);
                    return null; // Dont bother checking monetary charge
                default:
                    break;
            }
        } else if (ratingGroup != null) {
            checkOperationType(ACCOUNT_OPERATION_TYPE.DATA_WALLET_CHARGE);
        }

        checkOperationType(ACCOUNT_OPERATION_TYPE.MONETARY_CHARGE);

        return null;
    }

    @Override
    public void debitBalance(BigDecimal amntCents) throws InsufficientFundsException, Exception {
        if (DAO.isZero(amntCents)) {
            return;
        }
        checkOperationType(ACCOUNT_OPERATION_TYPE.DEBIT);
    }

    @Override
    public void creditBalance(BigDecimal amntCents) throws Exception {
        if (DAO.isZero(amntCents)) {
            return;
        }
        checkOperationType(ACCOUNT_OPERATION_TYPE.CREDIT);
    }

    @Override
    public void transferAmountToAccount(IAccount destAcc, BigDecimal amntCents) throws InsufficientFundsException, Exception {
        if (DAO.isZero(amntCents)) {
            return;
        }
        checkOperationType(ACCOUNT_OPERATION_TYPE.TRANSFER);
    }

    @Override
    public void setStatus(int newStatus, String user) throws Exception {
        if (user != null && user.equalsIgnoreCase("admin")) {
            checkOperationType(ACCOUNT_OPERATION_TYPE.ADMIN_STATUS_CHANGE);
        } else {
            checkOperationType(ACCOUNT_OPERATION_TYPE.USER_STATUS_CHANGE);
        }
    }

    // HELPERS
    private void locked(ACCOUNT_OPERATION_TYPE type) throws Exception {
        throw new Exception("Account is locked -- Current Account Status is " + getStatus() + " and attempting to do " + type.toString() + ". dbLock held is " + isLockedInDB() + ". Verify the Account Status is allowed to do this");
    }

    @Override
    public boolean supportsOperationType(ACCOUNT_OPERATION_TYPE opType) {
        try {
            checkOperationType(opType);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void checkOperationType(ACCOUNT_OPERATION_TYPE opType) throws Exception {

        if (!isLockedInDB()) {
            // If is is not locked then you cannot do anything 
            locked(opType);
        }

        int s = getStatus();

        switch (opType) {

            case CREDIT:
                if ((s & AccountRestrictions.CREDIT_BARRED) != 0) {
                    locked(opType);
                }
                break;

            case DEBIT:
                if ((s & AccountRestrictions.DEBIT_BARRED) != 0) {
                    locked(opType);
                }
                break;

            case UNIT_CREDIT_CHARGE:
                if ((s & AccountRestrictions.UNIT_CREDIT_CHARGE_BARRED) != 0) {
                    locked(opType);
                }
                break;

            case MONETARY_CHARGE:
                if ((s & AccountRestrictions.MONETARY_CHARGE_BARRED) != 0) {
                    locked(opType);
                }
                break;

            case USER_STATUS_CHANGE:
                if ((s & AccountRestrictions.USER_STATUS_CHANGE_BARRED) != 0) {
                    locked(opType);
                }
                break;

            case ADMIN_STATUS_CHANGE:
                if ((s & AccountRestrictions.ADMIN_STATUS_CHANGE_BARRED) != 0) {
                    locked(opType);
                }
                break;

            case TRANSFER:
                if ((s & AccountRestrictions.TRANSFER_BARRED) != 0) {
                    locked(opType);
                }
                break;
            case ONNET_WALLET_CHARGE:
                if ((s & AccountRestrictions.ONNET_WALLET_BARRED) != 0) {
                    locked(opType);
                }
                break;
            case LOCAL_WALLET_CHARGE:
                if ((s & AccountRestrictions.LOCAL_WALLET_BARRED) != 0) {
                    locked(opType);
                }
                break;
            case INTERNATIONAL_WALLET_CHARGE:
                if ((s & AccountRestrictions.INTERNATIONAL_WALLET_BARRED) != 0) {
                    locked(opType);
                }
                break;
            case DATA_WALLET_CHARGE:
                if ((s & AccountRestrictions.DATA_WALLET_BARRED) != 0) {
                    locked(opType);
                }
                break;
            case SMILE_ON:
                if ((s & AccountRestrictions.SMILE_ON) != 0) {
                    locked(opType);
                }
                break;
        }
    }

    @Override
    public boolean unitCreditChargingBarred() {
        return ((getStatus() & AccountRestrictions.UNIT_CREDIT_CHARGE_BARRED) != 0);
    }

    private class AccountRestrictions {

        public static final int CREDIT_BARRED = 1; // e.g. receiving me2u transfers, topping up etc
        public static final int DEBIT_BARRED = 2; // e.g. sending me2u transfers, buying bundles etc
        public static final int UNIT_CREDIT_CHARGE_BARRED = 4; // e.g. in bundle usage
        public static final int MONETARY_CHARGE_BARRED = 8; // e.g. out of bundle usage (does not stop rate group 9000 charges
        public static final int USER_STATUS_CHANGE_BARRED = 16; // e.g. once this is set, then the account status cannot be changed by non admin
        public static final int ADMIN_STATUS_CHANGE_BARRED = 32; // e.g. once this is set, then the account status cannot be changed by any means
        public static final int TRANSFER_BARRED = 64;
        public static final int PROVISIONING_CHANGES_BARRED = 128;
        public static final int ONNET_WALLET_BARRED = 256;
        public static final int LOCAL_WALLET_BARRED = 512;
        public static final int INTERNATIONAL_WALLET_BARRED = 1024;
        public static final int DATA_WALLET_BARRED = 2048;
        public static final int SMILE_ON = 4096;
    }
    /**
     * The account status is an int used with a bit mask as per the values above
     * i.e. bit 1 turns on credit barring, bit 2 turns on debit barring etc etc.
     * Bit one is on the far right in the examples below Some typical
     * combinations:
     *
     * 000000 = 0 = allow all 111111 = 63 = account totally locked down from
     * user and admin. Nobody can change the status themselves anymore 011111 =
     * 31 = account totally locked down from user. User cannot change the status
     * themselves anymore 001110 = 14 = recharging allowed but no other way of
     * changing account balance 001000 = 8 = everything but out of bundle
     * charges are allowed 011011 = 27 = only in bundle allowed and cant change
     * the status (e.g for staff) 11011111 = 223 = Locked Down be changed by
     * admin So in summary: 0 -> allow all 8 -> All except out of bundle 14 ->
     * No spending, only recharging
     *
     * 101100000000
     *
     * 10011100 = 156 = Only Transfers, debits, credits (e.g. for system
     * accounts) 01011000 = 88 = Only get credit, use bundles and buy bundles
     * e.g. for corps 01001000 = 72 No OOB and No Me2U out
     */
}
