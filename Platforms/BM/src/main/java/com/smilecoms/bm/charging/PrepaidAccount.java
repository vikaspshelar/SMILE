/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.charging;

import com.smilecoms.bm.EventHelper;
import com.smilecoms.bm.db.model.Account;
import com.smilecoms.bm.db.model.ServiceInstance;
import com.smilecoms.bm.db.op.DAO;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.JPAUtils;
import java.math.BigDecimal;
import java.util.Date;
import javax.persistence.EntityManager;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class PrepaidAccount extends BaseAccount {

    private static final Logger log = LoggerFactory.getLogger(PrepaidAccount.class);
    private final Account acc;
    private final EntityManager em;
    private final boolean isLockedInDB;

    public PrepaidAccount(EntityManager em, long accountId, boolean readOnly) throws Exception {
        if (readOnly) {
            isLockedInDB = false;
            acc = DAO.getUnlockedAccount(em, accountId);
        } else {
            isLockedInDB = true;
            acc = DAO.getLockedAccount(em, accountId);
        }
        this.em = em;
    }

    @Override
    public boolean isLockedInDB() {
        return isLockedInDB;
    }

    @Override
    public long getAccountId() {
        return acc.getAccountId();
    }

    @Override
    public void setStatus(int newStatus, String user) throws Exception {
        super.setStatus(newStatus, user);
        acc.setStatus(newStatus);
        em.persist(acc);
        em.flush();
    }

    @Override
    public int getStatus() {
        return acc.getStatus();
    }

    /**
     * PRIVATE FUNCTIONS
     */
    /**
     * NB: anything that changes the balance or reserves must set
     * availablebalance to null so that its recalculated if requested again
     */
    private void clearBalanceCache() {
        availableBalance = null;
    }

    @Override
    public BigDecimal getCurrentBalanceCents() {
        return acc.getBalanceCents();
    }
    private BigDecimal availableBalance;

    @Override
    public BigDecimal getAvailableBalanceCents() {
        /**
         * NB: anything that changes the balance or reserves must set available
         * balance to null so that its recalculated if requested again
         */
        if (availableBalance == null) {
            BigDecimal reservationAmnt = DAO.getAccountsReservations(em, acc.getAccountId());
            BigDecimal currentBalance = getCurrentBalanceCents();
            availableBalance = currentBalance.subtract(reservationAmnt);
            if (log.isDebugEnabled()) {
                log.debug("Account [{}] has an available balance of [{}]c. Reserved is [{}]c and Current is [{}]c", new Object[]{acc.getAccountId(), availableBalance, reservationAmnt, currentBalance});
            }
            if (DAO.isNegative(availableBalance)) {
                log.debug("An available balance is negative [{}]. Returing zero.", availableBalance);
                availableBalance = BigDecimal.ZERO;
            }
        }
        return availableBalance;
    }

    @Override
    public void transferAmountToAccount(IAccount destAcc, BigDecimal amntCents) throws InsufficientFundsException, Exception {
        try {
            super.transferAmountToAccount(destAcc, amntCents);
        } catch (Exception e) {
            // Never allow comitting
            JPAUtils.setRollbackOnly();
            throw e;
        }
        debitBalance(amntCents);
        destAcc.creditBalance(amntCents);
    }

    @Override
    public void debitBalance(BigDecimal amntCents) throws InsufficientFundsException, Exception {
        try {
            super.debitBalance(amntCents);
        } catch (Exception e) {
            // Never allow comitting
            JPAUtils.setRollbackOnly();
            throw e;
        }
        if (DAO.isZero(amntCents)) {
            log.debug("Amount to decrement iz zero. Just returning");
            return;
        }
        if (amntCents.compareTo(getAvailableBalanceCents()) > 0) {
            log.debug("Cannot decrement - not enough available balance. Available balance is [{}]c and amount is [{}]c", getAvailableBalanceCents(), amntCents);
            throw new InsufficientFundsException();
        }
        debitBalanceAsMuchAsAllowed(amntCents);
    }

    @Override
    public BigDecimal chargeForUsage(BigDecimal amntCents, String ratingGroup) throws Exception {
        if (DAO.isZeroOrNegative(amntCents)) {
            log.debug("Charging amount is [{}] which is zero or negative so just returning", amntCents);
            return BigDecimal.ZERO;
        }

        try {
            super.chargeForUsage(amntCents, ratingGroup);
        } catch (Exception e) {
            int warningCents = BaseUtils.getIntProperty("env.bm.negativebalance.warning.from.cents");
            if (amntCents.compareTo(BigDecimal.valueOf(warningCents)) >= 0) {
                log.warn("Trying to decrement [{}] cents from Account with Id [{}]. But it is locked!", amntCents, acc.getAccountId());
                BaseUtils.sendTrapToOpsManagement(
                        BaseUtils.MAJOR,
                        "BM",
                        "Revenue Leakage! Account Id " + acc.getAccountId() + " was decremented by " + amntCents + "c but it is locked from charges");
            }
            return BigDecimal.ZERO;
        }

        // Alter balance required as unit credits did not cover the required amount
        log.debug("[{}]c still needs to be charged out of bundle", amntCents);
        BigDecimal accoutBalanceBeforeDecrement = getCurrentBalanceCents();
        debitBalanceAsMuchAsAllowed(amntCents);
        if (log.isDebugEnabled()) {
            log.debug("New account balance will be [{}]", getCurrentBalanceCents());
        }

        BigDecimal amntDebited = accoutBalanceBeforeDecrement.subtract(getCurrentBalanceCents());

        // We want the amount actually debited. This may be less than anticipated if the account went negative
        log.debug("chargeForUsage charged [{}] on account [{}]", amntDebited, getAccountId());
        return amntDebited;

    }

    @Override
    public boolean canTakeACharge(String ratingGroup) {
        try {
            super.reserve(null, null, null, null, 0, null, ratingGroup, false);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Account cannot take a charge [{}]", e.toString());
            }
            return false;
        }
        log.debug("Accounts available balance is [{}]", getAvailableBalanceCents());
        if (DAO.isZeroOrNegative(getAvailableBalanceCents())) {
            return false;
        }
        return true;
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
            boolean checkOnly) {

        ReserveResult resResult = new ReserveResult();

        if (DAO.isZeroOrNegative(getAvailableBalanceCents()) || DAO.isNegative(centsToReserve)) {
            // A negative amount (i.e. negative rate indicates that OOB charging is not allowed)
            if (log.isDebugEnabled() && DAO.isNegative(centsToReserve)) {
                log.debug("Cents to reserve is negative [{}]", centsToReserve.doubleValue());
            } else if (log.isDebugEnabled()) {
                log.debug("Account available balance in cents is zero or negative [{}]", getAvailableBalanceCents());
            }
            resResult.setCentsReserved(BigDecimal.ZERO);
            resResult.setStillHasBalanceLeftToReserve(false);
            return resResult;
        }

        try {
            super.reserve(centsToReserve, sessionId, eventTimetamp, request, reservationSecs, serviceInstance, ratingGroup, checkOnly);
        } catch (Exception e) {
            log.debug("Account cannot accept a reservation so we will indicate that zero could be reserved");
            resResult.setCentsReserved(BigDecimal.ZERO);
            resResult.setStillHasBalanceLeftToReserve(false);
            return resResult;
        }

        // If we get here, then this account has cents left
        if (getAvailableBalanceCents().compareTo(centsToReserve) > 0) {
            // Balance has more cents available than required. Reserve the required cents
            resResult.setCentsReserved(centsToReserve);
            resResult.setStillHasBalanceLeftToReserve(true);
        } else {
            // Balance has just enough or not enough
            resResult.setCentsReserved(getAvailableBalanceCents());
            resResult.setStillHasBalanceLeftToReserve(false);
        }

        if (DAO.isPositive(resResult.getCentsReserved())) {
            resResult.setReservationWasCreated();
            try {
                DAO.createOrUpdateReservation(
                        em,
                        acc.getAccountId(),
                        sessionId,
                        -1,
                        resResult.getCentsReserved(),
                        eventTimetamp,
                        request,
                        reservationSecs,
                        BigDecimal.ZERO,
                        checkOnly);

            } finally {
                clearBalanceCache();
            }
            
            // Clear everything but sticky DPI rules
            ServiceRules rules =  new ServiceRules();
            rules.setClearAllButStickyDPIRules(true);
            rules.setMaxBpsDown(-1L);
            rules.setMaxBpsUp(-1L);
            rules.setQci(-1);
            ServiceSpecHelper.applyServiceRules(serviceInstance, rules);
        }

        log.debug("Reserved [{}] against account [{}]", resResult.getCentsReserved(), getAccountId());
        return resResult;

    }

    /**
     * This is the only function that should ever increment an account balance
     * by any means
     *
     * @param em
     * @param acc
     * @param amntCents
     * @return
     * @throws Exception
     */
    @Override
    public void creditBalance(BigDecimal amntCents) throws Exception {
        super.creditBalance(amntCents);
        acc.setBalanceCents(getCurrentBalanceCents().add(amntCents));
        persistBalanceChange();
    }

    /**
     * This is the only function that should ever decrement an account balance
     * by any means Functions calling this one must have already checked that
     * the operation is allowed on the account
     *
     * @param em
     * @param acc
     * @param amntCents
     * @return
     * @throws Exception
     */
    private void debitBalanceAsMuchAsAllowed(BigDecimal amntCents) {

        boolean accountStartedAtZero = DAO.isZeroOrNegative(getCurrentBalanceCents());

        acc.setBalanceCents(getCurrentBalanceCents().subtract(amntCents));

        if (DAO.isNegative(getCurrentBalanceCents())) {

            int warningCents = BaseUtils.getIntProperty("env.bm.negativebalance.warning.from.cents");
            if (getCurrentBalanceCents().abs().compareTo(BigDecimal.valueOf(warningCents)) >= 0) {
                log.warn("Decrementing [{}] cents from Account with Id [{}]. This will make it negative!", amntCents, acc.getAccountId());
                BaseUtils.sendTrapToOpsManagement(
                        BaseUtils.MAJOR,
                        "BM",
                        "Revenue Leakage! Account Id " + acc.getAccountId() + " was decremented by " + amntCents + "c and balance would now be " + getCurrentBalanceCents() + "c");
            } else {
                log.debug("Decrementing [{}] cents from Account with Id [{}]. This will make it negative!", amntCents, acc.getAccountId());
            }
            if (!BaseUtils.getBooleanProperty("global.bm.negativebalance.allowed")) {
                log.debug("global.bm.negativebalance.allowed says negative balances are not allowed, so setting balance to zero");
                acc.setBalanceCents(BigDecimal.ZERO);
            }

        }

        persistBalanceChange();
        
        if(BaseUtils.getBooleanProperty("env.bm.log.account.depleted.event", false)) {
            if(!accountStartedAtZero && DAO.isZeroOrNegative(getCurrentBalanceCents())) {
                EventHelper.sendAccountEvent(this, EventHelper.AccSubTypes.DEPLETED, 0);
            }
        }
    }

    /**
     * This is the only function that should ever change an account balance by
     * any means
     *
     * @param em
     * @param acc
     * @throws Exception
     */
    private void persistBalanceChange() {
        clearBalanceCache(); // make it get updated next time
        em.persist(acc);
    }
}
