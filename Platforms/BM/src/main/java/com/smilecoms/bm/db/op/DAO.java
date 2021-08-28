/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.db.op;

import com.smilecoms.bm.BMDataCache;
import com.smilecoms.bm.charging.IAccount;
import com.smilecoms.bm.charging.ChargingDetailDaemon;
import com.smilecoms.bm.charging.ChargingDetailRecord;
import com.smilecoms.bm.db.model.Account;
import com.smilecoms.bm.db.model.AccountHistory;
import com.smilecoms.bm.db.model.DailyFupInfo;
import com.smilecoms.bm.db.model.InterconnectHistory;
import com.smilecoms.bm.db.model.InterconnectPartner;
import com.smilecoms.bm.db.model.PortedNumber;
import com.smilecoms.bm.db.model.RatePlan;
import com.smilecoms.bm.db.model.RatePlanAvp;
import com.smilecoms.bm.db.model.Reservation;
import com.smilecoms.bm.db.model.ServiceInstance;
import com.smilecoms.bm.db.model.ServiceInstanceMapping;
import com.smilecoms.bm.db.model.ServiceInstanceMappingPK;
import com.smilecoms.bm.db.model.ServiceRate;
import com.smilecoms.bm.db.model.UnitCreditInstance;
import com.smilecoms.bm.db.model.UnitCreditSpecification;
import com.smilecoms.bm.unitcredits.UCChargeResult;
import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.cache.CacheHelper;
import com.smilecoms.commons.sca.ScheduledAccountHistory;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Stopwatch;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.bm.AccountHistoryQuery;
import com.smilecoms.xml.schema.bm.AccountSummaryQuery;
import com.smilecoms.xml.schema.bm.ServiceInstanceIdentifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import org.slf4j.*;

/**
 *
 * @author paul
 */
@Singleton
@Startup
public class DAO implements BaseListener {

    private static final Logger log = LoggerFactory.getLogger(DAO.class);
    private static EntityManagerFactory emf = null;

    private static final String PI_HISTORY_INSERT = "INSERT INTO SmileDB.product_instance_history\n"
            + "(PRODUCT_INSTANCE_ID, PRODUCT_SPECIFICATION_ID, CUSTOMER_PROFILE_ID, ORGANISATION_ID, STATUS, SEGMENT, CREATED_BY_CUSTOMER_PROFILE_ID, \n"
            + "CREATED_BY_ORGANISATION_ID, CREATED_DATETIME, PROMOTION_CODE, LAST_MODIFIED, FRIENDLY_NAME, \n"
            + "LOGICAL_ID, PHYSICAL_ID, LAST_ACTIVITY_DATETIME, FIRST_ACTIVITY_DATETIME, LAST_RECONNECTION_DATETIME, \n"
            + "LAST_IMEI, REFERRAL_CODE, PAUSED_ACTIVITY_DATETIME, FIRST_RG_ACTIVITY_DATETIME, LAST_RG_ACTIVITY_DATETIME, \n"
            + "PAUSED_RG_ACTIVITY_DATETIME, LAST_RG_RECONNECTION_DATETIME, INSERTED_DATETIME) \n"
            + "SELECT *, NOW() from product_instance where PRODUCT_INSTANCE_ID=?";

    /**
     * *****************************************
     * ACCOUNT RELATED FUNCTIONS *****************************************
     */
    public static Account createAccount(EntityManager em, int status) {
        Account acc = null;
        boolean notCreated = true;
        int tries = 0;
        boolean failedOnce = false;
        while (notCreated) {
            long id = 0;
            try {
                if (BaseUtils.getBooleanProperty("env.random.account.ids")) {
                    id = Utils.getRandomNumber(1100000000, 2129999999);
                } else {
                    id = getNewAccountNumber(em, new Date());
                }
                log.debug("Got a new account number to try persisting [{}]", id);
                acc = persistNewAccountInOwnTx(id, status);
                notCreated = false;
            } catch (javax.persistence.PersistenceException e) {
                // Concurrecy issue may cause number to be used in the mean time
                em.clear();
                failedOnce = true;
                tries++;
                if (tries > 20) {
                    log.warn("Tried 20 times to create a new account number and it still fails");
                    throw e;
                }
                log.warn("[{}]. Account number already exists [{}]. Trying again with a new one", e.toString(), id);
            }
        }
        log.debug("Successfully created account Id [{}]", acc.getAccountId());
        if (failedOnce) {
            log.warn("Successfully created account Id [{}] even though we got a failure earlier", acc.getAccountId());
        }
        return acc;
    }
    private static final SimpleDateFormat sdfAccountStart = new SimpleDateFormat("yyMM");

    private static Account persistNewAccountInOwnTx(long id, int status) {
        EntityManager em = null;
        Account acc;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            acc = new Account();
            acc.setAccountId(id);
            acc.setStatus(status);
            acc.setBalanceCents(BigDecimal.ZERO);
            em.persist(acc);
            em.flush();
            em.refresh(acc);
            JPAUtils.commitTransaction(em);
        } finally {
            JPAUtils.closeEM(em);
        }
        return acc;
    }

    private static long getNewAccountNumber(EntityManager em, Date d) {
        String startBit = sdfAccountStart.format(d);
        Query q = em.createNativeQuery("SELECT ifnull(MAX(A.ACCOUNT_ID),0) FROM account A WHERE A.ACCOUNT_ID >= ? and A.ACCOUNT_ID < ?");
        q.setParameter(1, Long.parseLong(startBit + "000000"));
        q.setParameter(2, Long.parseLong((Integer.parseInt(startBit) + 100) + "000000")); // Only look forward 1 year
        long currentDaysMax = (Long) q.getSingleResult();
        if (currentDaysMax == Long.parseLong(startBit + "999999")) {
            // Overflow condition! more than 999999 accounts for this month. Overflow to the next day
            log.warn("Account numbers are overflowing to the next month");
            return getNewAccountNumber(em, Utils.getFutureDate(GregorianCalendar.MONTH, 1));
        }
        if (currentDaysMax == 0) {
            // No accounts created today
            return Long.parseLong(startBit + "000000");
        } else {
            return currentDaysMax + 1;
        }
    }

    public static void deleteAccount(EntityManager em, long accountId) throws Exception {

        Query q = em.createNativeQuery("SELECT count(*) FROM reservation R WHERE R.ACCOUNT_ID = ?");
        q.setParameter(1, accountId);
        Long cnt = (Long) q.getSingleResult();
        if (cnt != 0) {
            throw new Exception("Cannot delete an account that has reservations");
        }

        q = em.createNativeQuery("SELECT count(*) FROM unit_credit_instance WHERE ACCOUNT_ID = ?");
        q.setParameter(1, accountId);
        cnt = (Long) q.getSingleResult();
        if (cnt != 0) {
            throw new Exception("Cannot delete an account that has unit credits");
        }

        q = em.createNativeQuery("SELECT count(*) FROM service_instance WHERE ACCOUNT_ID = ?");
        q.setParameter(1, accountId);
        cnt = (Long) q.getSingleResult();
        if (cnt != 0) {
            throw new Exception("Cannot delete an account that has service instances");
        }

        q = em.createNativeQuery("SELECT count(*) FROM account_history WHERE ACCOUNT_ID = ?");
        q.setParameter(1, accountId);
        cnt = (Long) q.getSingleResult();
        if (cnt != 0) {
            throw new Exception("Cannot delete an account that has account history");
        }

        try {
            Account acc = JPAUtils.findAndThrowENFE(em, Account.class, accountId);
            if (isPositive(acc.getBalanceCents())) {
                throw new Exception("Cannot delete an account with a positive balance");
            }
            em.remove(acc);
            em.flush();
        } catch (javax.persistence.EntityNotFoundException enfe) {
            log.warn("Account does not exist and hence cannot be deleted. Will continue without error -- " + accountId);
        }

    }

    public static void deleteUnitCreditInstance(EntityManager em, int unitCreditInstanceId) {
        Query q = em.createNativeQuery("DELETE FROM unit_credit_instance where UNIT_CREDIT_INSTANCE_ID = ?");
        q.setParameter(1, unitCreditInstanceId);
        q.executeUpdate();
    }

    public static Account getLockedAccount(EntityManager em, long accountID) {
        long start = 0;
        if (log.isDebugEnabled()) {
            log.debug("Getting locked account [{}]", accountID);
            start = System.currentTimeMillis();
        }
        Account acc = JPAUtils.findAndThrowENFE(em, Account.class, accountID, LockModeType.PESSIMISTIC_WRITE);
        if (log.isDebugEnabled()) {
            log.debug("Got locked account [{}] which took [{}]ms", accountID, System.currentTimeMillis() - start);
        }
        return acc;
    }

    public static Account getUnlockedAccount(EntityManager em, long accountID) {
        return JPAUtils.findAndThrowENFE(em, Account.class, accountID, LockModeType.NONE);
    }

    public static BigDecimal getAccountsReservations(EntityManager em, long accId) {
        Query q = em.createNativeQuery("SELECT IFNULL(SUM(R.AMOUNT_CENTS),0) FROM reservation R WHERE R.ACCOUNT_ID = ?");
        q.setParameter(1, accId);
        BigDecimal res = (BigDecimal) q.getSingleResult();
        if (log.isDebugEnabled()) {
            log.debug("Account [{}] has reservations worth [{}]c", accId, res);
        }
        return res;
    }

    public static BigDecimal getTotalMonetaryAmountCentsForAllPurchasedBundlesOnAccount(EntityManager em, long accId) {
        Query q = em.createNativeQuery("select sum(UCI.POS_CENTS_CHARGED) "
                + "from unit_credit_instance UCI join unit_credit_specification UCS on (UCI.account_id=? "
                + "and UCI.UNIT_CREDIT_SPECIFICATION_ID = UCS.UNIT_CREDIT_SPECIFICATION_ID and UCS.UNIT_TYPE='Byte' and UCI.INFO not like '%Deleted=true%'"
                + "         and UCS.ITEM_NUMBER not like 'BUNK%');");
        q.setParameter(1, accId);
        BigDecimal res = (BigDecimal) q.getSingleResult();
        if (log.isDebugEnabled()) {
            log.debug("Account [{}] has purchased a total of [{}] bytes.", accId, res);
        }
        return res;
    }

    public static BigDecimal getTotalTimeBasedUnitCreditRevenueToReverse(EntityManager em, int ucid) {

        Query q = em.createNativeQuery("select  ifnull(round(sum(UCI.REVENUE_CENTS_PER_DAY * timestampdiff(SECOND, UCI.REVENUE_FIRST_DATE, least(UCI.REVENUE_LAST_DATE, curdate()))/86400))/100, 0) as REVENUE "
                + "from unit_credit_instance UCI "
                + "JOIN unit_credit_specification UCS on (UCS.unit_credit_specification_id = UCI.unit_credit_specification_id and UCI.UNIT_CREDIT_INSTANCE_ID = ?) "
                + "where UCI.REVENUE_FIRST_DATE <= curdate() AND UCS.CONFIGURATION like '%Reporting=Data_%' AND UCI.INFO NOT LIKE '%Deleted=true%'");
        q.setParameter(1, ucid);
        BigDecimal res = (BigDecimal) q.getSingleResult();
        if (log.isDebugEnabled()) {
            log.debug("Total  Revenue for UC [{}] to be reversed is [{}] major units.", ucid, res);
        }
        return res;

    }

    public static List<Reservation> getAccountsReservationList(EntityManager em, long accId) {
        Query q = em.createNativeQuery("SELECT * FROM reservation R WHERE R.ACCOUNT_ID = ?", Reservation.class);
        q.setParameter(1, accId);
        return q.getResultList();
    }

    public static List<Reservation> getAccountsReservationList(EntityManager em, long accId, int limit) {
        Query q = em.createNativeQuery("SELECT * FROM reservation R WHERE R.ACCOUNT_ID = ? LIMIT ?", Reservation.class);
        q.setParameter(1, accId);
        q.setParameter(2, limit);
        return q.getResultList();
    }

    public static List<Reservation> getSessionsReservationListStartingWith(EntityManager em, String sessionId) {
        Query q = em.createNativeQuery("SELECT * FROM reservation R WHERE R.SESSION_ID like ?", Reservation.class);
        q.setParameter(1, sessionId + "_%");
        return q.getResultList();
    }

    public static Reservation getUCReservation(EntityManager em, long accId, String sessionId, int ucId) {
        Query q = em.createNativeQuery("SELECT * FROM reservation R WHERE R.SESSION_ID=? and R.ACCOUNT_ID=? and R.UNIT_CREDIT_INSTANCE_ID=?", Reservation.class);
        q.setParameter(1, sessionId);
        q.setParameter(2, accId);
        q.setParameter(3, ucId);
        return (Reservation) q.getSingleResult();
    }

    public static double getAccountsBytesRemaining(EntityManager em, long accId) {
        log.debug("Getting data balance on account [{}]", accId);
        Query q = em.createNativeQuery("select sum(UCI.UNITS_REMAINING) from unit_credit_instance UCI "
                + "join unit_credit_specification UCS on UCI.UNIT_CREDIT_SPECIFICATION_ID = UCS.UNIT_CREDIT_SPECIFICATION_ID "
                + "where UCI.ACCOUNT_ID=? and UCI.START_DATE <= now() "
                + "and UCI.EXPIRY_DATE > now() and UCS.UNIT_TYPE = 'Byte' "
                + "AND (UCI.END_DATE > NOW() OR UCI.END_DATE IS NULL) "
                + "and UCS.CONFIGURATION not like '%DisplayBalance=false%' "
                + "and UCI.UNITS_REMAINING > 0 "
                + "UNION "
                + "select count(*) from unit_credit_instance UCI "
                + "join unit_credit_specification UCS on UCI.UNIT_CREDIT_SPECIFICATION_ID = UCS.UNIT_CREDIT_SPECIFICATION_ID "
                + "where UCI.ACCOUNT_ID=? and UCI.START_DATE <= now() "
                + "and UCI.EXPIRY_DATE > now() and UCS.UNIT_TYPE = 'Byte' "
                + "AND (UCI.END_DATE > NOW() OR UCI.END_DATE IS NULL) "
                + "and UCS.CONFIGURATION like '%DisplayBalance=false%'");
        q.setParameter(1, accId);
        q.setParameter(2, accId);
        List<BigDecimal> resList = q.getResultList();
        if (resList.isEmpty()) {
            return 0;
        }
        if (resList.size() == 2 && DAO.isPositive(resList.get(1))) {
            log.debug("Account has unlimited unit credits. Returning Double.MAX_VALUE. Unlimited count is [{}]", resList.get(1));
            return Double.MAX_VALUE;
        } else if (resList.get(0) != null) {
            double bytes = resList.get(0).doubleValue();
            log.debug("Account has [{}] bytes remaining", bytes);
            return bytes;
        } else {
            return 0;
        }
    }

    public static int getAccountsSecondsRemaining(EntityManager em, long accId) {
        log.debug("Getting voice balance on account [{}]", accId);
        Query q = em.createNativeQuery("select sum(UCI.UNITS_REMAINING) from unit_credit_instance UCI "
                + "join unit_credit_specification UCS on UCI.UNIT_CREDIT_SPECIFICATION_ID = UCS.UNIT_CREDIT_SPECIFICATION_ID "
                + "where UCI.ACCOUNT_ID=? and UCI.START_DATE <= now() "
                + "and UCI.EXPIRY_DATE > now() and UCS.UNIT_TYPE = 'Second' "
                + "AND (UCI.END_DATE > NOW() OR UCI.END_DATE IS NULL) "
                + "and UCS.CONFIGURATION not like '%FreeOnnetVoice=true%' "
                + "and UCI.UNITS_REMAINING > 0 ");
        q.setParameter(1, accId);
        List<BigDecimal> resList = q.getResultList();
        if (resList.isEmpty()) {
            return 0;
        }
        if (resList.get(0) != null) {
            int seconds = resList.get(0).intValue();
            log.debug("Account has [{}] voice seconds remaining", seconds);
            return seconds;
        } else {
            return 0;
        }
    }

    public static int getAccountsInternationalSecondsRemaining(EntityManager em, long accId) {
        log.debug("Getting voice balance on account [{}]", accId);
        Query q = em.createNativeQuery("select sum(UCI.UNITS_REMAINING) from unit_credit_instance UCI "
                + "join unit_credit_specification UCS on UCI.UNIT_CREDIT_SPECIFICATION_ID = UCS.UNIT_CREDIT_SPECIFICATION_ID "
                + "where UCI.ACCOUNT_ID=? and UCI.START_DATE <= now() "
                + "and UCI.EXPIRY_DATE > now() and UCS.UNIT_TYPE = 'Second' "
                + "AND (UCI.END_DATE > NOW() OR UCI.END_DATE IS NULL) "
                + "and UCS.CONFIGURATION not like '%FreeOnnetVoice=true%' "
                + "and UCI.UNITS_REMAINING > 0 and UCS.UNIT_CREDIT_NAME like '%International%'");
        q.setParameter(1, accId);
        List<BigDecimal> resList = q.getResultList();
        if (resList.isEmpty()) {
            return 0;
        }
        if (resList.get(0) != null) {
            int seconds = resList.get(0).intValue();
            log.debug("Account has [{}] voice seconds remaining", seconds);
            return seconds;
        } else {
            return 0;
        }
    }

    public static int getAccountsLocalSecondsRemaining(EntityManager em, long accId) {
        log.debug("Getting voice balance on account [{}]", accId);
        Query q = em.createNativeQuery("select sum(UCI.UNITS_REMAINING) from unit_credit_instance UCI "
                + "join unit_credit_specification UCS on UCI.UNIT_CREDIT_SPECIFICATION_ID = UCS.UNIT_CREDIT_SPECIFICATION_ID "
                + "where UCI.ACCOUNT_ID=? and UCI.START_DATE <= now() "
                + "and UCI.EXPIRY_DATE > now() and UCS.UNIT_TYPE = 'Second' "
                + "AND (UCI.END_DATE > NOW() OR UCI.END_DATE IS NULL) "
                + "and UCS.CONFIGURATION not like '%FreeOnnetVoice=true%' "
                + "and UCI.UNITS_REMAINING > 0 and UCS.UNIT_CREDIT_NAME not like '%International%'");
        q.setParameter(1, accId);
        List<BigDecimal> resList = q.getResultList();
        if (resList.isEmpty()) {
            return 0;
        }
        if (resList.get(0) != null) {
            int seconds = resList.get(0).intValue();
            log.debug("Account has [{}] voice seconds remaining", seconds);
            return seconds;
        } else {
            return 0;
        }
    }

    public static List<UnitCreditInstance> getUnitCreditInstanceList(EntityManager em, long accHistoryId) {
        log.debug("Getting UnitCreditInstance based on Account History Id [{}]", accHistoryId);
        List<UnitCreditInstance> uciList;
        try {
            Query q = em.createNativeQuery("SELECT * from unit_credit_instance uci where uci.UNIT_CREDIT_INSTANCE_ID IN "
                    + "(select DISTINCT(ucic.UNIT_CREDIT_INSTANCE_ID) from unit_credit_instance_charge ucic "
                    + "where ucic.ACCOUNT_HISTORY_ID = ?)", UnitCreditInstance.class);
            q.setParameter(1, accHistoryId);
            uciList = q.getResultList();
        } catch (Exception e) {
            return null;
        }
        return uciList;
    }

    public static boolean isProductInstanceOlderThanPeriodInDays(EntityManager em, Integer productInstanceId, int periodLength) {
        boolean isOlderThanPeriod;
        String period;
        Query q = em.createNativeQuery("select if(if(PII.FIRST_ACTIVITY_DATETIME is null, now(), PII.FIRST_ACTIVITY_DATETIME) > DATE_SUB(now(), INTERVAL ? DAY), 'NEW', 'OLD') AS AGE from product_instance PII where PII.product_instance_id = ?");
        q.setParameter(1, periodLength);
        q.setParameter(2, productInstanceId);
        try {
            period = (String) q.getSingleResult();
        } catch (javax.persistence.NoResultException nre) {
            period = "OLD";
        }
        if (period.equalsIgnoreCase("NEW")) {
            log.debug("This PI [{}] is less than period");
            isOlderThanPeriod = false;
        } else {
            log.debug("This PI [{}] is older than period");
            isOlderThanPeriod = true;
        }
        return isOlderThanPeriod;
    }

    public static void createOrUpdateReservation(EntityManager em, long accId, String sessionId, int ucId, BigDecimal cents, Date eventTimestamp, byte[] request, int reservationSecs, BigDecimal units, boolean checkOnly) {

        if (checkOnly) {
            log.debug("This is just a rating check. Wont do anything");
            return;
        }
        Calendar expiryTimestamp = Calendar.getInstance();
        expiryTimestamp.setTime(eventTimestamp);
        expiryTimestamp.add(Calendar.SECOND, reservationSecs);

        String sql = "INSERT INTO reservation (ACCOUNT_ID, SESSION_ID, UNIT_CREDIT_INSTANCE_ID, AMOUNT_CENTS, "
                + "AMOUNT_UNIT_CREDITS, RESERVATION_EVENT_TIMESTAMP, RESERVATION_EXPIRY_TIMESTAMP, REQUEST) "
                + "VALUES (?,?,?,?,?,?,?,?) "
                + "ON DUPLICATE KEY UPDATE "
                + "AMOUNT_CENTS=?, AMOUNT_UNIT_CREDITS=?, RESERVATION_EVENT_TIMESTAMP=?, RESERVATION_EXPIRY_TIMESTAMP=?, REQUEST=?";
        Query q = em.createNativeQuery(sql);
        q.setParameter(1, accId);
        q.setParameter(2, sessionId);
        q.setParameter(3, ucId);
        q.setParameter(4, cents);
        q.setParameter(5, units);
        q.setParameter(6, eventTimestamp);
        q.setParameter(7, expiryTimestamp.getTime());
        q.setParameter(8, request);
        // Parameters for update on exists
        q.setParameter(9, cents);
        q.setParameter(10, units);
        q.setParameter(11, eventTimestamp);
        q.setParameter(12, expiryTimestamp.getTime());
        q.setParameter(13, request);
        q.executeUpdate();
        em.flush();
        if (log.isDebugEnabled()) {
            log.debug("Reservation inserted for [{}]c [{}]units. AccId: [{}] UCId: [{}] SessionId: [{}] Expiry: [{}]", new Object[]{cents, units, accId, ucId, sessionId, expiryTimestamp.getTime()});
        }
    }

    public static void updateReservation(EntityManager em, long accId, String sessionId, int ucId, BigDecimal cents, Date eventTimestamp, byte[] request, int reservationSecs, BigDecimal units) {

        Query q = em.createNativeQuery("UPDATE reservation SET AMOUNT_CENTS = ?, AMOUNT_UNIT_CREDITS = ?,"
                + " RESERVATION_EVENT_TIMESTAMP = ?, RESERVATION_EXPIRY_TIMESTAMP = ?, REQUEST = ? WHERE "
                + "ACCOUNT_ID = ? AND SESSION_ID = ? AND UNIT_CREDIT_INSTANCE_ID = ?");
        Calendar expiryTimestamp = Calendar.getInstance();
        expiryTimestamp.setTime(eventTimestamp);
        expiryTimestamp.add(Calendar.SECOND, reservationSecs);
        q.setParameter(1, cents);
        q.setParameter(2, units);
        q.setParameter(3, eventTimestamp);
        q.setParameter(4, expiryTimestamp.getTime());
        q.setParameter(5, request);
        q.setParameter(6, accId);
        q.setParameter(7, sessionId);
        q.setParameter(8, ucId);
        q.executeUpdate();
        em.flush();
        if (log.isDebugEnabled()) {
            log.debug("Reservation updated for [{}]c [{}]units. AccId: [{}] UCId: [{}] SessionId: [{}] Expiry: [{}]", new Object[]{cents, units, accId, ucId, sessionId, expiryTimestamp.getTime()});
        }
    }

    public static void deleteReservationsBySessionId(EntityManager em, String sessionId) {
        // Remove sessions reservations
        Query q = em.createNativeQuery("DELETE FROM reservation where SESSION_ID = ?");
        q.setParameter(1, sessionId);
        int removals = q.executeUpdate();
        if (log.isDebugEnabled()) {
            log.debug("Deleted [{}] rows from reservation table for session id [{}]", removals, sessionId);
        }
    }

    public static void deleteReservation(EntityManager em, String sessionId, long accountId, int ucInstanceId) {
        // Remove sessions reservations
        Query q = em.createNativeQuery("DELETE FROM reservation where SESSION_ID = ? and ACCOUNT_ID=? and UNIT_CREDIT_INSTANCE_ID=?");
        q.setParameter(1, sessionId);
        q.setParameter(2, accountId);
        q.setParameter(3, ucInstanceId);

        int removals = q.executeUpdate();
        if (log.isDebugEnabled()) {
            log.debug("Deleted [{}] rows from reservation table for session id [{}]", removals, sessionId);
        }
    }

    public static void deleteReservationsByAccountId(EntityManager em, long accId) {
        // Remove sessions reservations
        Query q = em.createNativeQuery("DELETE FROM reservation where ACCOUNT_ID = ?");
        q.setParameter(1, accId);
        int removals = q.executeUpdate();
        if (log.isDebugEnabled()) {
            log.debug("Deleted [{}] rows from reservation table for account id [{}]", removals, accId);
        }
    }

    public static long getAccountToBeCharged(EntityManager em, String from, String to, long serviceInstanceAccountId) {
        // TODO : Reverse charging algorithm
        return serviceInstanceAccountId;
    }

    /**
     * **********************************
     * DAILY FUP RELATED FUNCTIONS **********************************
     */
    public static DailyFupInfo getDailyFupStartOfDayUnits(EntityManager em, long accountId, long unitCreditSpecId) {

        Query q = em.createNativeQuery("SELECT DF.* from daily_fup_info DF where DF.ACCOUNT_ID=? and DF.UNIT_CREDIT_SPECIFICATION_ID=?", DailyFupInfo.class);
        q.setParameter(1, accountId);
        q.setParameter(2, unitCreditSpecId);
        return (DailyFupInfo) q.getSingleResult();
    }

    public static void setDailyFupStartOfDayUnits(EntityManager em, long accountId, long unitCreditSpecId, long startOfDayUnits) {
        String sql = "INSERT INTO daily_fup_info (ACCOUNT_ID, UNIT_CREDIT_SPECIFICATION_ID, START_OF_DAY_UNITS, LAST_MODIFIED) "
                + "VALUES (?,?,?,now()) "
                + "ON DUPLICATE KEY UPDATE "
                + "START_OF_DAY_UNITS=?, LAST_MODIFIED=now() ";
        Query q = em.createNativeQuery(sql);
        q.setParameter(1, accountId);
        q.setParameter(2, unitCreditSpecId);
        q.setParameter(3, startOfDayUnits);
        // Parameters for update on exists
        q.setParameter(4, startOfDayUnits);
        q.executeUpdate();
        em.flush();

        if (log.isDebugEnabled()) {
            log.debug("Updated DailyFupStartUnits for AccId: [{}] to StartOfDayUnits: [{}], ModDateTime: [{}] ", new Object[]{accountId, startOfDayUnits});
        }
    }

    public static void removeDailyFupSettings(EntityManager em, long accountId, long unitCreditSpecId) {
        String sql = "DELETE FROM daily_fup_info "
                + "WHERE ACCOUNT_ID=? and UNIT_CREDIT_SPECIFICATION_ID=?) ";

        Query q = em.createNativeQuery(sql);
        q.setParameter(1, accountId);
        q.setParameter(2, unitCreditSpecId);
        q.executeUpdate();
        em.flush();

        if (log.isDebugEnabled()) {
            log.debug("Removed Settings for DailyFupStartUnits for AccId: [{}] unitCreditSpecId: [{}], ModDateTime: [{}] ", new Object[]{accountId, unitCreditSpecId});
        }
    }

    /**
     * *****************************************
     * ACCOUNT HISTORY RELATED FUNCTIONS
     * *****************************************
     */
    public static AccountHistory getAccountHistoryByExtTxIdAndServiceInstance(EntityManager em, String txid, int serviceInstanceId) {
        Query q = em.createNativeQuery("SELECT H.* from account_history H where H.EXT_TXID=? and H.SERVICE_INSTANCE_ID=?", AccountHistory.class);
        q.setParameter(1, txid);
        q.setParameter(2, serviceInstanceId);
        return (AccountHistory) q.getSingleResult();
    }

    public static AccountHistory getOldestAccountHistoryByExtTxId(EntityManager em, String txid) {
        Query q = em.createNativeQuery("SELECT H.* from account_history H where H.EXT_TXID=? ORDER BY H.START_DATE ASC LIMIT 1", AccountHistory.class);
        q.setParameter(1, txid);
        return (AccountHistory) q.getSingleResult();
    }

    public static void writeDeviceSeen(EntityManager em, String srcDevice, Date eventTimetamp) throws Exception {
        if (srcDevice != null && !srcDevice.isEmpty()) {
            Query q = em.createNativeQuery("insert ignore into device_seen ("
                    + "DEVICE, "
                    + "FIRST_SEEN_DATETIME)"
                    + "values(?,?)");
            q.setParameter(1, srcDevice);
            q.setParameter(2, eventTimetamp);
            q.executeUpdate();
        }
    }

    public static AccountHistory createOrUpdateAccountHistoryForSession(
            EntityManager em,
            String sessionId,
            String src,
            String dest,
            BigDecimal amnt,
            IAccount acc,
            Date startDate,
            Date eventTimetamp,
            String description,
            BigDecimal unitsUsed,
            String srcDevice,
            String termCode,
            String txType,
            String status,
            int serviceInstanceID,
            BigDecimal unitCreditUnits,
            BigDecimal revenueCents,
            BigDecimal unearnedRevenueCents,
            BigDecimal freeRevenueCents,
            String location,
            String IPAddress,
            String serviceInstanceIdentifier,
            boolean isRetrial,
            String incomingTrunk,
            String outgoingTrunk,
            String leg,
            BigDecimal unitCreditBaselineUnits,
            String rateGroup,
            String info,
            List<UCChargeResult> unitCreditChanges) throws Exception {

        AccountHistory ah;
        Date lastEventDate = null;
        try {
            // PCB: changed to also include SI Id as its possible for a session to swap SI mid-way if there is a product deletion and new addition. We dont want the new info to be on the old account history row
            ah = getAccountHistoryByExtTxIdAndServiceInstance(em, sessionId, serviceInstanceID);
            // Existing session - update billed amount and set latest balance
            ah.setAccountCents(ah.getAccountCents().subtract(amnt));
            ah.setAccountBalanceRemaining(acc.getCurrentBalanceCents());
            ah.setTotalUnits(ah.getTotalUnits().add(unitsUsed));
            ah.setUnitCreditUnits(ah.getUnitCreditUnits().subtract(unitCreditUnits));
            if (ah.getUnitCreditBaselineUnits() == null) {
                // for cutover
                ah.setUnitCreditBaselineUnits(BigDecimal.ZERO);
            }
            ah.setUnitCreditBaselineUnits(ah.getUnitCreditBaselineUnits().subtract(unitCreditBaselineUnits));
            ah.setRevenueCents(ah.getRevenueCents().add(revenueCents));
            ah.setUnearnedRevenueCents(ah.getUnearnedRevenueCents() == null ? unearnedRevenueCents : ah.getUnearnedRevenueCents().add(unearnedRevenueCents));
            ah.setFreeRevenueCents(ah.getFreeRevenueCents().add(freeRevenueCents));
            ah.setStatus(status);
            ah.setTermCode(termCode);
            ah.setInfo(info);
            lastEventDate = ah.getEndDate();
            // Dont allow setting it lower (e.g. for charging reprocessing out of order)
            ah.setEndDate(lastEventDate.after(eventTimetamp) ? lastEventDate : eventTimetamp);
            em.persist(ah);
            em.flush();
        } catch (javax.persistence.NoResultException nre) {
            // New session
            DAO.writeDeviceSeen(em, srcDevice, eventTimetamp);
            ah = new AccountHistory();
            ah.setAccountCents(amnt.negate());
            ah.setAccountBalanceRemaining(acc.getCurrentBalanceCents());
            ah.setTotalUnits(unitsUsed);
            ah.setStatus(status);
            ah.setTermCode(termCode);
            ah.setUnitCreditUnits(unitCreditUnits.negate());
            ah.setUnitCreditBaselineUnits(unitCreditBaselineUnits.negate());
            ah.setRevenueCents(revenueCents);
            ah.setUnearnedRevenueCents(unearnedRevenueCents);
            ah.setFreeRevenueCents(freeRevenueCents);

            // The fields below should not change after session setup so are only populated on creation of a new row
            ah.setStartDate(startDate);
            ah.setEndDate(eventTimetamp);
            ah.setSource(src == null ? "" : src);
            ah.setDestination(dest == null ? "" : dest);
            ah.setExtTxId(sessionId);
            ah.setDescription(description);
            ah.setSourceDevice(srcDevice == null ? "" : srcDevice);
            ah.setTransactionType(txType);
            ah.setAccountId(acc.getAccountId());
            ah.setServiceInstanceId(serviceInstanceID);
            ah.setLocation(location == null ? "" : location);
            ah.setIPAddress(IPAddress == null ? "" : IPAddress);
            ah.setServiceInstanceIdentifier(serviceInstanceIdentifier == null ? "" : serviceInstanceIdentifier);
            ah.setIncomingTrunk(incomingTrunk == null ? "" : incomingTrunk);
            ah.setOutgoingTrunk(outgoingTrunk == null ? "" : outgoingTrunk);
            ah.setRateGroup(rateGroup == null ? "" : rateGroup);
            ah.setLeg(leg == null ? "" : leg);
            ah.setInfo(info);
            em.persist(ah);
            em.flush();
            em.refresh(ah);
        }

        if (log.isDebugEnabled()) {
            Stopwatch.start();
        }
        ChargingDetailRecord cdr = new ChargingDetailRecord();
        cdr.accountId = ah.getAccountId();
        cdr.accountHistoryId = ah.getId();
        // Data from this event within the session
        cdr.chargeDateTime = eventTimetamp;
        cdr.eventBalanceRemaining = ah.getAccountBalanceRemaining();
        cdr.eventUnits = unitsUsed;
        cdr.eventLocation = (location == null ? "" : location);
        if (lastEventDate != null) {
            cdr.eventDurationMillis = ah.getEndDate().getTime() - lastEventDate.getTime();
        } else {
            cdr.eventDurationMillis = -1;
        }

        cdr.eventRevenueCents = revenueCents;
        cdr.eventFreeRevenueCents = freeRevenueCents;
        cdr.eventUnitCreditUnits = unitCreditUnits.negate();
        cdr.eventUnitCreditBaselineUnits = unitCreditBaselineUnits.negate();
        cdr.eventAccountCents = amnt.negate();
        cdr.serviceInstanceId = ah.getServiceInstanceId();

//        if (txType.startsWith("ext.01") && cdr.eventDurationMillis >= 0
//                && (cdr.eventUnits.doubleValue() - cdr.eventDurationMillis / 1000) > 1) {
//            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "BM", "A voice call has charged more than its interim duration: ID:" + cdr.accountHistoryId + " TXId:" + ah.getExtTxId());
//        }
        if (isRetrial || BaseUtils.getBooleanProperty("env.bm.chargingdetail.write.synchronous", true)) {
            log.debug("Charging detail records are being written synchronously as part of this transaction");
            long chargingDetailId = DAO.writeCDR(em, cdr, isRetrial);
            DAO.writeUnitCreditCharges(em, ah.getId(), chargingDetailId, unitCreditChanges);
        } else {
            log.debug("Charging detail records are being written asynchronously in their own transaction");
            ChargingDetailDaemon.enqueueForWriting(cdr);
        }
        if (log.isDebugEnabled()) {
            Stopwatch.stop();
            log.debug("Processing charging detail record took [{}]", Stopwatch.millisString());
        }
        return ah;
    }
    private static final String CDRquery = "insert into charging_detail ("
            + "ACCOUNT_HISTORY_ID, "
            + "CHARGE_DATE_TIME,"
            + "EVENT_BALANCE_REMAINING,"
            + "EVENT_UNITS,"
            + "EVENT_LOCATION,"
            + "EVENT_DURATION_MILLIS,"
            + "EVENT_REVENUE_CENTS,"
            + "EVENT_UNIT_CREDIT_UNITS,"
            + "EVENT_UNIT_CREDIT_BASELINE_UNITS,"
            + "EVENT_ACCOUNT_CENTS,"
            + "EVENT_FREE_REVENUE_CENTS)"
            + "values(?,?,?,?,?,?,?,?,?,?,?)";

    public static long writeCDR(EntityManager em, ChargingDetailRecord cdr, boolean isRetrial) throws Exception {

        if (isRetrial) {
            log.debug("This is a charging retrial. Going to ensure this is not a duplicate of a previous charge");
            // If the event has been charged then acc_hist end_date must be greater than or equal to the date. Allow 1 minute grace just in case
            Query q = em.createNativeQuery("select count(*) from charging_detail D join account_history H on (H.ID = D.ACCOUNT_HISTORY_ID) "
                    + "where D.charge_date_time=? and D.event_location=? and D.event_units=? and H.end_date > ? - interval 1 minute and H.account_id=? and H.service_instance_id=?");
            q.setParameter(1, cdr.chargeDateTime);
            q.setParameter(2, cdr.eventLocation);
            q.setParameter(3, cdr.eventUnits);
            q.setParameter(4, cdr.chargeDateTime);
            q.setParameter(5, cdr.accountId);
            q.setParameter(6, cdr.serviceInstanceId);
            long res = (Long) q.getSingleResult();
            if (res > 0) {
                log.warn("This is a duplicate retrial");
                JPAUtils.setRollbackOnly();
                throw new Exception("Duplicate retrial");
            } else {
                log.debug("This retrial has not been processed before");
            }
        }
        Query q = em.createNativeQuery(CDRquery);
        q.setParameter(1, cdr.accountHistoryId);
        q.setParameter(2, cdr.chargeDateTime);
        q.setParameter(3, cdr.eventBalanceRemaining.doubleValue());
        q.setParameter(4, cdr.eventUnits.doubleValue());
        q.setParameter(5, cdr.eventLocation);
        q.setParameter(6, cdr.eventDurationMillis);
        q.setParameter(7, cdr.eventRevenueCents.doubleValue());
        q.setParameter(8, cdr.eventUnitCreditUnits.doubleValue());
        q.setParameter(9, cdr.eventUnitCreditBaselineUnits.doubleValue());
        q.setParameter(10, cdr.eventAccountCents.doubleValue());
        q.setParameter(11, cdr.eventFreeRevenueCents.doubleValue());
        q.executeUpdate();

        try {
            q = em.createNativeQuery("SELECT LAST_INSERT_ID()");
            return ((BigInteger) q.getSingleResult()).longValue();
        } catch (Exception e) {
            log.warn("Error:", e);
            return 0;
        }
    }

    public static String getChargingDetail(EntityManager em, long transactionRecordId) {
        StringBuilder ret = new StringBuilder();
        //get more stuff in charging detail - we use this in transaction history graph
        Query q = em.createNativeQuery("select concat(UNIX_TIMESTAMP(CHARGE_DATE_TIME)*1000,\",\",  round(EVENT_UNITS,1),\",\", round(EVENT_UNIT_CREDIT_UNITS,1)) from charging_detail where ACCOUNT_HISTORY_ID = ? order by CHARGE_DATE_TIME asc");
        q.setParameter(1, transactionRecordId);
        List res = q.getResultList();
        if (res.isEmpty()) {
            return "";
        }
        // Different mysql versions return byte[] or String!
        Object typeCheck = res.get(0);
        if (typeCheck instanceof java.lang.String) {
            for (Object row : res) {
                ret.append((String) row);
                ret.append("\n");
            }
        } else {
            for (Object row : res) {
                ret.append(new String((byte[]) row));
                ret.append("\n");
            }
        }

        return ret.toString();
    }

    public static AccountHistory createAccountHistoryForEvent(
            EntityManager em,
            String sessionId,
            String src,
            String dest,
            BigDecimal amnt,
            IAccount acc,
            Date ts,
            String description,
            BigDecimal eventUnits,
            String srcDevice,
            String txType,
            String status,
            int serviceInstanceID,
            BigDecimal unitCreditUnits,
            BigDecimal revenueCents,
            BigDecimal unearnedRevenueCents,
            BigDecimal freeRevenueCents,
            String location,
            String IPAddress,
            String serviceInstanceIdentifier,
            String incomingTrunk,
            String outgoingTrunk,
            String leg,
            BigDecimal unitCreditBaselineUnits,
            String rateGroup,
            String info,
            List<UCChargeResult> unitCreditChanges) {

        AccountHistory ah = new AccountHistory();
        ah.setAccountCents(amnt.negate());
        ah.setTotalUnits(eventUnits);
        ah.setUnitCreditUnits(unitCreditUnits.negate());
        ah.setUnitCreditBaselineUnits(unitCreditBaselineUnits == null ? BigDecimal.ZERO : unitCreditBaselineUnits.negate());
        ah.setStatus(status);
        ah.setTermCode("");
        ah.setStartDate(ts);
        ah.setEndDate(ts);
        ah.setSource(src == null ? "" : src);
        ah.setDestination(dest == null ? "" : dest);
        ah.setExtTxId(sessionId == null ? "" : sessionId);
        ah.setAccountBalanceRemaining(acc.getCurrentBalanceCents());
        ah.setDescription(description);
        ah.setSourceDevice(srcDevice == null ? "" : srcDevice);
        ah.setTransactionType(txType);
        ah.setAccountId(acc.getAccountId());
        ah.setServiceInstanceId(serviceInstanceID);
        ah.setRevenueCents(revenueCents);
        ah.setUnearnedRevenueCents(unearnedRevenueCents);
        ah.setFreeRevenueCents(freeRevenueCents);
        ah.setLocation(location == null ? "" : location);
        ah.setIPAddress(IPAddress == null ? "" : IPAddress);
        ah.setServiceInstanceIdentifier(serviceInstanceIdentifier == null ? "" : serviceInstanceIdentifier);
        ah.setIncomingTrunk(incomingTrunk == null ? "" : incomingTrunk);
        ah.setOutgoingTrunk(outgoingTrunk == null ? "" : outgoingTrunk);
        ah.setRateGroup(rateGroup == null ? "" : rateGroup);
        ah.setLeg(leg == null ? "" : leg);
        ah.setInfo(info);
        em.persist(ah);
        em.flush();
        em.refresh(ah);
        DAO.writeUnitCreditCharges(em, ah.getId(), null, unitCreditChanges);
        return ah;
    }

    public static List<AccountHistory> getAccountHistoryInReverseOrderForTxId(EntityManager em, String txid) {
        Query q = em.createNativeQuery("SELECT * FROM account_history H where H.EXT_TXID=? order by H.id desc", AccountHistory.class);
        q.setParameter(1, txid);
        return q.getResultList();
    }

    public static List<AccountHistory> getAccountHistory(EntityManager em, AccountHistoryQuery accountHistoryQuery) {

        StringBuilder sql = new StringBuilder("SELECT * FROM account_history H where ");
        boolean filterByDate = true;
        if (accountHistoryQuery.getTransactionRecordId() != null && accountHistoryQuery.getTransactionRecordId() > 0) {
            filterByDate = false;
            sql.append("H.ID=? AND ");
        }
        if (accountHistoryQuery.getAccountId() > 0) {
            sql.append("H.ACCOUNT_ID=? AND ");
        }
        if (accountHistoryQuery.getExtTxId() != null && !accountHistoryQuery.getExtTxId().isEmpty()) {
            filterByDate = false;
            if (accountHistoryQuery.getExtTxId().contains("%")) {
                sql.append("H.EXT_TXID LIKE ? AND ");
            } else {
                sql.append("H.EXT_TXID=? AND ");
            }
        }

        //the date filtering has been change to support range overlapping. we can never return fewer records than before
        //BUT, we can return more if there is a range overlap. This should fix the problem of "missing bundles"
        if (filterByDate && accountHistoryQuery.getDateFrom() != null) {
            sql.append("H.END_DATE>=? AND ");
        }
        if (filterByDate && accountHistoryQuery.getDateTo() != null) {
            sql.append("H.START_DATE<=? AND ");
        }

        if (accountHistoryQuery.getSource() != null && !accountHistoryQuery.getSource().isEmpty()) {
            sql.append("H.SOURCE=? AND ");
        }
        if (accountHistoryQuery.getTransactionType() != null && !accountHistoryQuery.getTransactionType().isEmpty()) {
            sql.append("H.TRANSACTION_TYPE like ? AND ");
        }
        if (!accountHistoryQuery.getServiceInstanceIds().isEmpty()) {
            int cnt = 0;
            sql.append("( ");
            for (int siId : accountHistoryQuery.getServiceInstanceIds()) {
                cnt++;
                if (cnt == accountHistoryQuery.getServiceInstanceIds().size()) {
                    sql.append("H.SERVICE_INSTANCE_ID= ? ");
                } else {
                    sql.append("H.SERVICE_INSTANCE_ID= ? OR ");
                }
            }
            sql.append(") AND ");
        }
        sql.append("H.EXT_TXID NOT RLIKE '"); // Dont show rate groups 1000 to 1499
        sql.append(BaseUtils.getProperty("env.bm.accounthistory.exttxid.notlike", "_1[0-4]..$"));
        sql.append("' ");
        sql.append("ORDER BY H.START_DATE DESC, H.ID DESC ");
        sql.append("LIMIT ?");
        sql.append(" OFFSET ?");

        log.debug("SQL Query for TX History is [{}]", sql);

        Query q = em.createNativeQuery(sql.toString(), AccountHistory.class);
        int position = 0;

        if (accountHistoryQuery.getTransactionRecordId() != null && accountHistoryQuery.getTransactionRecordId() > 0) {
            q.setParameter(++position, accountHistoryQuery.getTransactionRecordId());
        }
        if (accountHistoryQuery.getAccountId() > 0) {
            q.setParameter(++position, accountHistoryQuery.getAccountId());
        }
        if (accountHistoryQuery.getExtTxId() != null && !accountHistoryQuery.getExtTxId().isEmpty()) {
            q.setParameter(++position, accountHistoryQuery.getExtTxId());
        }
        if (accountHistoryQuery.getDateFrom() != null && filterByDate) {
            q.setParameter(++position, Utils.getJavaDate(accountHistoryQuery.getDateFrom()));
        }
        if (accountHistoryQuery.getDateTo() != null && filterByDate) {
            q.setParameter(++position, Utils.getJavaDate(accountHistoryQuery.getDateTo()));
        }
        if (accountHistoryQuery.getSource() != null && !accountHistoryQuery.getSource().isEmpty()) {
            q.setParameter(++position, accountHistoryQuery.getSource());
        }
        if (accountHistoryQuery.getTransactionType() != null && !accountHistoryQuery.getTransactionType().isEmpty()) {
            q.setParameter(++position, "%" + accountHistoryQuery.getTransactionType() + "%");
        }
        if (!accountHistoryQuery.getServiceInstanceIds().isEmpty()) {
            for (int siId : accountHistoryQuery.getServiceInstanceIds()) {
                q.setParameter(++position, siId);
            }
        }

        q.setParameter(++position, accountHistoryQuery.getResultLimit());
        int offset =  accountHistoryQuery.getOffset() == null ? 0 :  accountHistoryQuery.getOffset();
        log.debug("offset is :"+offset);
        q.setParameter(++position, offset);
        log.debug("executed query = [{}]",q.toString());
        return q.getResultList();
    }

    public static List<Object[]> getAccountSummary(EntityManager em, AccountSummaryQuery accountSummaryQuery) throws Exception {
        Query q;
        if (accountSummaryQuery.getVerbosity().equalsIgnoreCase("hourly")) {
            log.debug("Grouping is hourly");
            q = em.createNativeQuery("SELECT DATE_FORMAT(D.CHARGE_DATE_TIME, '%y%m%d %H'), "
                    + "if(H.TRANSACTION_TYPE like '%32251%','Data',if(H.TRANSACTION_TYPE like '%32260%','Voice', H.TRANSACTION_TYPE)), "
                    + "sum(D.EVENT_UNITS), "
                    + "sum(D.EVENT_ACCOUNT_CENTS), "
                    + "sum(D.EVENT_UNIT_CREDIT_UNITS) * -1, "
                    + "ifnull(sum(D.EVENT_UNIT_CREDIT_BASELINE_UNITS) * -1, 0) "
                    + "FROM account_history H "
                    + "JOIN charging_detail D ON (D.ACCOUNT_HISTORY_ID = H.ID) "
                    + "WHERE H.ACCOUNT_ID = ? "
                    + "AND H.TRANSACTION_TYPE not like 'txtype.%' "
                    + "AND H.END_DATE >= ? "
                    + "AND H.START_DATE <= ? "
                    + "AND D.CHARGE_DATE_TIME >= ? "
                    + "AND D.CHARGE_DATE_TIME <= ? "
                    + "AND H.EXT_TXID NOT LIKE '%\\\\_1___' "
                    + getServiceInstanceQueryPart(accountSummaryQuery.getServiceInstanceIds())
                    + "GROUP BY DATE_FORMAT(D.CHARGE_DATE_TIME, '%y%m%d %H'), if(H.TRANSACTION_TYPE like '%32251%','Data',if(H.TRANSACTION_TYPE like '%32260%','Voice', H.TRANSACTION_TYPE)) "
                    + "ORDER BY DATE_FORMAT(D.CHARGE_DATE_TIME, '%y%m%d %H'), if(H.TRANSACTION_TYPE like '%32251%','Data',if(H.TRANSACTION_TYPE like '%32260%','Voice', H.TRANSACTION_TYPE))");
        } else if (accountSummaryQuery.getVerbosity().equalsIgnoreCase("daily")) {
            log.debug("Grouping is daily");
            q = em.createNativeQuery("SELECT DATE_FORMAT(D.CHARGE_DATE_TIME, '%y%m%d'), "
                    + "if(H.TRANSACTION_TYPE like '%32251%','Data',if(H.TRANSACTION_TYPE like '%32260%','Voice', H.TRANSACTION_TYPE)), "
                    + "sum(D.EVENT_UNITS), "
                    + "sum(D.EVENT_ACCOUNT_CENTS), "
                    + "sum(D.EVENT_UNIT_CREDIT_UNITS) * -1, "
                    + "ifnull(sum(D.EVENT_UNIT_CREDIT_BASELINE_UNITS) * -1, 0) "
                    + "FROM account_history H "
                    + "JOIN " + BaseUtils.getProperty("env.bm.account.summary.daily.table", "charging_detail") + " D ON (D.ACCOUNT_HISTORY_ID = H.ID) "
                    + "WHERE H.ACCOUNT_ID = ? "
                    + "AND H.TRANSACTION_TYPE not like 'txtype.%' "
                    + "AND H.END_DATE >= ? "
                    + "AND H.START_DATE <= ? "
                    + "AND D.CHARGE_DATE_TIME >= ? "
                    + "AND D.CHARGE_DATE_TIME <= ? "
                    + "AND H.EXT_TXID NOT LIKE '%\\\\_1___' "
                    + getServiceInstanceQueryPart(accountSummaryQuery.getServiceInstanceIds())
                    + "GROUP BY DATE_FORMAT(D.CHARGE_DATE_TIME, '%y%m%d'), if(H.TRANSACTION_TYPE like '%32251%','Data',if(H.TRANSACTION_TYPE like '%32260%','Voice', H.TRANSACTION_TYPE)) "
                    + "ORDER BY DATE_FORMAT(D.CHARGE_DATE_TIME, '%y%m%d'), if(H.TRANSACTION_TYPE like '%32251%','Data',if(H.TRANSACTION_TYPE like '%32260%','Voice', H.TRANSACTION_TYPE))");
        } else if (accountSummaryQuery.getVerbosity().equalsIgnoreCase("monthly")) {
            log.debug("Grouping is monthly");
            q = em.createNativeQuery("SELECT DATE_FORMAT(D.CHARGE_DATE_TIME, '%y%m'), "
                    + "if(H.TRANSACTION_TYPE like '%32251%','Data',if(H.TRANSACTION_TYPE like '%32260%','Voice', H.TRANSACTION_TYPE)), "
                    + "sum(D.EVENT_UNITS), "
                    + "sum(D.EVENT_ACCOUNT_CENTS), "
                    + "sum(D.EVENT_UNIT_CREDIT_UNITS) * -1, "
                    + "ifnull(sum(D.EVENT_UNIT_CREDIT_BASELINE_UNITS) * -1, 0) "
                    + "FROM account_history H "
                    + "JOIN " + BaseUtils.getProperty("env.bm.account.summary.monthly.table", "charging_detail") + " D ON (D.ACCOUNT_HISTORY_ID = H.ID) "
                    + "WHERE H.ACCOUNT_ID = ? "
                    + "AND H.TRANSACTION_TYPE not like 'txtype.%' "
                    + "AND H.END_DATE >= ? "
                    + "AND H.START_DATE <= ? "
                    + "AND D.CHARGE_DATE_TIME >= ? "
                    + "AND D.CHARGE_DATE_TIME <= ? "
                    + "AND H.EXT_TXID NOT LIKE '%\\\\_1___' "
                    + getServiceInstanceQueryPart(accountSummaryQuery.getServiceInstanceIds())
                    + "GROUP BY DATE_FORMAT(D.CHARGE_DATE_TIME, '%y%m'), if(H.TRANSACTION_TYPE like '%32251%','Data',if(H.TRANSACTION_TYPE like '%32260%','Voice', H.TRANSACTION_TYPE)) "
                    + "ORDER BY DATE_FORMAT(D.CHARGE_DATE_TIME, '%y%m'), if(H.TRANSACTION_TYPE like '%32251%','Data',if(H.TRANSACTION_TYPE like '%32260%','Voice', H.TRANSACTION_TYPE))");
        } else {
            throw new Exception("Invalid verbosity -- " + accountSummaryQuery.getVerbosity());
        }
        int position = 0;
        q.setParameter(++position, accountSummaryQuery.getAccountId());
        q.setParameter(++position, Utils.getJavaDate(accountSummaryQuery.getDateFrom()));
        q.setParameter(++position, Utils.getJavaDate(accountSummaryQuery.getDateTo()));
        q.setParameter(++position, Utils.getJavaDate(accountSummaryQuery.getDateFrom()));
        q.setParameter(++position, Utils.getJavaDate(accountSummaryQuery.getDateTo()));
        for (int siId : accountSummaryQuery.getServiceInstanceIds()) {
            q.setParameter(++position, siId);
        }
        return q.getResultList();
    }

    private static String getServiceInstanceQueryPart(List<Integer> serviceInstanceIds) {
        if (!serviceInstanceIds.isEmpty()) {
            StringBuilder sql = new StringBuilder();
            int cnt = 0;
            sql.append("AND ( ");
            for (int siId : serviceInstanceIds) {
                cnt++;
                if (cnt == serviceInstanceIds.size()) {
                    sql.append("H.SERVICE_INSTANCE_ID= ? ");
                } else {
                    sql.append("H.SERVICE_INSTANCE_ID= ? OR ");
                }
            }
            sql.append(") ");
            return sql.toString();
        } else {
            return "";
        }
    }

    public static Integer getCustomerProfileId(EntityManager em, String loggedInUser) {
        if (loggedInUser == null) {
            return 0;
        }
        try {
            Query q = em.createNativeQuery("select C.CUSTOMER_PROFILE_ID from customer_profile C where C.SSO_IDENTITY=?");
            q.setParameter(1, loggedInUser);
            return (Integer) q.getSingleResult();
        } catch (Exception e) {
            log.debug("Error getting customer by sso id id [{}]:", loggedInUser, e);
            return 0;
        }
    }

    public static BigDecimal getApproximateUnitsUsedForSIOnAccountSinceDate(EntityManager em, int serviceInstanceID, long accountId, Calendar date) {
        // Not entirely accurate as a long session starting just before the date wont be included, but this gives the customer the advantage if anything 
        // as this query would return less rather than more
        Query q = em.createNativeQuery("SELECT IFNULL(SUM(TOTAL_UNITS),0) FROM account_history where account_id = ? and service_instance_id = ? and start_date >= ?");
        q.setParameter(1, accountId);
        q.setParameter(2, serviceInstanceID);
        q.setParameter(3, date);
        return (BigDecimal) q.getSingleResult();
    }

    public static BigDecimal getActualUnitsUsedForSIOnAccountSinceDate(EntityManager em, int serviceInstanceID, long accountId, Calendar date) {
        Query q = em.createNativeQuery("select  IFNULL(SUM(D.EVENT_UNITS),0) from charging_detail D, account_history H where H.ID = D.ACCOUNT_HISTORY_ID "
                + "and D.CHARGE_DATE_TIME >= ? "
                + "and H.SERVICE_INSTANCE_ID = ? "
                + "and H.ACCOUNT_ID = ? "
                + "and H.END_DATE >= ?");
        q.setParameter(1, date);
        q.setParameter(2, serviceInstanceID);
        q.setParameter(3, accountId);
        q.setParameter(4, date);
        return (BigDecimal) q.getSingleResult();
    }

    public static List<ServiceRate> getRatesForTrunkSetLegAndService(EntityManager em, String incomingTrunk, String outgoingTrunk, String serviceCode, String leg, boolean mustFindShortcodeRate) {
        Query q = em.createNativeQuery("SELECT R.* FROM service_rate R "
                + "JOIN interconnect_trunk TF on TF.INTERCONNECT_PARTNER_ID = R.FROM_INTERCONNECT_PARTNER_ID "
                + "JOIN interconnect_trunk TT on TT.INTERCONNECT_PARTNER_ID = R.TO_INTERCONNECT_PARTNER_ID "
                + "WHERE TF.INTERNAL_ID=? "
                + "AND TT.INTERNAL_ID=? "
                + (mustFindShortcodeRate ? "AND R.RATING_HINT like '%Shortcode=true%' " : "AND R.RATING_HINT not like '%Shortcode=true%' ")
                + "AND R.SERVICE_CODE=? AND R.LEG=?", ServiceRate.class);
        q.setParameter(1, incomingTrunk);
        q.setParameter(2, outgoingTrunk);
        q.setParameter(3, serviceCode);
        q.setParameter(4, leg);
        return q.getResultList();
    }

    /**
     * Incoming trunk is the source network (Smile or Roaming partner) Outgoing
     * trunk is the destination network (Smile or interconnect partner)
     *
     * @param em
     * @param eventDate
     * @param incomingTrunk
     * @param outgoingTrunk
     * @param serviceCode
     * @param leg
     * @param icpFrom
     * @return
     */
    public static List<ServiceRate> getRatesForTrunkSetLegAndFromICPartner(EntityManager em, Date eventDate, String incomingTrunk, String outgoingTrunk, String serviceCode, String leg, int icpFrom, boolean mustFindShortcodeRate) {
        Query q = em.createNativeQuery("select * from service_rate R "
                + "JOIN interconnect_trunk TF on TF.INTERCONNECT_PARTNER_ID = R.FROM_INTERCONNECT_PARTNER_ID "
                + "JOIN interconnect_trunk TT on TT.INTERCONNECT_PARTNER_ID = R.TO_INTERCONNECT_PARTNER_ID "
                + "WHERE TF.INTERNAL_ID=? "
                + "AND TT.INTERNAL_ID=? "
                + "AND SERVICE_CODE=? "
                + "AND LEG=? "
                + "AND DATE_FROM <= ? "
                + "AND DATE_TO > ? "
                + "AND FROM_INTERCONNECT_PARTNER_ID = ? "
                + "AND FROM_PREFIX like ? "
                + (mustFindShortcodeRate ? "AND R.RATING_HINT like '%Shortcode=true%' " : "AND R.RATING_HINT not like '%Shortcode=true%' ")
                + "ORDER BY PRIORITY desc", ServiceRate.class);
        q.setParameter(1, incomingTrunk);
        q.setParameter(2, outgoingTrunk);
        q.setParameter(3, serviceCode);
        q.setParameter(4, leg);
        q.setParameter(5, eventDate);
        q.setParameter(6, eventDate);
        q.setParameter(7, icpFrom);
        String startsWithCountryCodeRegex = BaseUtils.getProperty("env.e164.country.code", "234") + "%";
        q.setParameter(8, startsWithCountryCodeRegex);
        return q.getResultList();
    }

    public static List<ServiceRate> getRatesForTrunkSetLegAndToICPartner(EntityManager em, Date eventDate, String incomingTrunk, String outgoingTrunk, String serviceCode, String leg, int icpTo, boolean mustFindShortcodeRate) {
        Query q = em.createNativeQuery("select * from service_rate R "
                + "JOIN interconnect_trunk TF on TF.INTERCONNECT_PARTNER_ID = R.FROM_INTERCONNECT_PARTNER_ID "
                + "JOIN interconnect_trunk TT on TT.INTERCONNECT_PARTNER_ID = R.TO_INTERCONNECT_PARTNER_ID "
                + "WHERE TF.INTERNAL_ID=? "
                + "AND TT.INTERNAL_ID=? "
                + "AND SERVICE_CODE=? "
                + "AND LEG=? "
                + "AND DATE_FROM <= ? "
                + "AND DATE_TO > ? "
                + "AND TO_INTERCONNECT_PARTNER_ID = ? "
                + (mustFindShortcodeRate ? "AND R.RATING_HINT like '%Shortcode=true%' " : "AND R.RATING_HINT not like '%Shortcode=true%' ")
                + "ORDER BY PRIORITY desc", ServiceRate.class);
        q.setParameter(1, incomingTrunk);
        q.setParameter(2, outgoingTrunk);
        q.setParameter(3, serviceCode);
        q.setParameter(4, leg);
        q.setParameter(5, eventDate);
        q.setParameter(6, eventDate);
        q.setParameter(7, icpTo);
        return q.getResultList();
    }

    public static List<PortedNumber> getPortedNumbers(EntityManager em) {
        Query q = em.createNativeQuery("SELECT * FROM ported_number", PortedNumber.class);
        return q.getResultList();
    }

    public static InterconnectPartner getInterconnectPartnerByName(EntityManager em, String interconnectPartnerName) {
        Query q = em.createNativeQuery("SELECT * FROM interconnect_partner where partner_name = ?", InterconnectPartner.class);
        q.setParameter(1, interconnectPartnerName);
        return (InterconnectPartner) q.getSingleResult();
    }

    public static InterconnectPartner getInterconnectPartnerByCode(EntityManager em, String interconnectPartnerCode) {
        Query q = em.createNativeQuery("SELECT * FROM interconnect_partner where interconnect_partner_code = ?", InterconnectPartner.class);
        q.setParameter(1, interconnectPartnerCode);
        return (InterconnectPartner) q.getSingleResult();
    }

    public static void insertPortedNumber(EntityManager em, long num, int interconnectPartnerId) {
        Query q = em.createNativeQuery("INSERT INTO ported_number (NUMBER, INTERCONNECT_PARTNER_ID) "
                + "VALUES (?,?) "
                + "ON DUPLICATE KEY UPDATE "
                + "INTERCONNECT_PARTNER_ID=?");
        q.setParameter(1, num);
        q.setParameter(2, interconnectPartnerId);
        q.setParameter(3, interconnectPartnerId);
        q.executeUpdate();
        em.flush();
    }

    /**
     * *****************************************
     * UNIT CREDIT FUNCTIONS *****************************************
     */
    public static BigDecimal getUnitCreditsAvailableUnits(EntityManager em, UnitCreditInstance uci) {
        BigDecimal reservations = getUCReservationUnits(em, uci.getUnitCreditInstanceId());
        return uci.getUnitsRemaining().subtract(reservations);
    }

    private static BigDecimal getUCReservationUnits(EntityManager em, int uciId) {
        BigDecimal units = CacheHelper.getFromLocalCache("UCRES_" + uciId, BigDecimal.class);
        if (units == null) {
            Query q = em.createNativeQuery("SELECT IFNULL(SUM(AMOUNT_UNIT_CREDITS),0), COUNT(*) as CNT FROM reservation where UNIT_CREDIT_INSTANCE_ID = ?");
            q.setParameter(1, uciId);
            Object[] res = (Object[]) q.getSingleResult();
            units = (BigDecimal) res[0];
            long cnt = (long) res[1];
            if (cnt > 10 && cnt > BaseUtils.getIntProperty("env.bm.uc.reservation.cache.when.exceeds", 100)) {
                int cacheTime = BaseUtils.getIntProperty("env.bm.uc.reservation.cache.secs", 10);
                if (cacheTime > 0) {
                    log.debug("This UC [{}] has [{}] reservations  against it so going to cache its reservation units for a while for performance reasons", uciId, cnt);
                    CacheHelper.putInLocalCache("UCRES_" + uciId, units, cacheTime);
                }
            }
        } else {
            log.debug("We got [{}] units from cache for UCId [{}]", units, uciId);
        }
        return units;
    }

    public static boolean isInBonusUCSIList(EntityManager em, int serviceInstanceId) {
        Query q = em.createNativeQuery("update services_for_uc_bonus set status='DE' where status='PE' and serviceInstanceId=?");
        q.setParameter(1, serviceInstanceId);
        int updates = q.executeUpdate();
        return updates > 0;
    }

    public static List<UnitCreditInstance> getUnitCreditInstances(EntityManager em, long accId) {
        Query q = em.createNativeQuery("SELECT UCI.* FROM "
                + "unit_credit_instance UCI "
                + "WHERE UCI.ACCOUNT_ID = ? "
                + "AND UCI.EXPIRY_DATE > NOW() ORDER BY UCI.PURCHASE_DATE, UCI.UNIT_CREDIT_INSTANCE_ID", UnitCreditInstance.class);
        q.setParameter(1, accId);
        List<UnitCreditInstance> uciList = q.getResultList();

        return uciList;
    }

    public static List<UnitCreditInstance> getUnitCreditInstancesByExtTxId(EntityManager em, String extTxId) {
        Query q = em.createNativeQuery("SELECT UCI.* FROM unit_credit_instance UCI WHERE UCI.EXT_TXID = ?", UnitCreditInstance.class);
        q.setParameter(1, extTxId);
        List<UnitCreditInstance> uciList = q.getResultList();
        return uciList;
    }

    public static List<UnitCreditInstance> getUnitCreditInstancesBySaleRowIdAndExtTxId(EntityManager em, int saleRowId, String extTxId) {
        Query q = em.createNativeQuery("SELECT UCI.* FROM unit_credit_instance UCI WHERE UCI.SALE_ROW_ID = ? AND UCI.EXT_TXID = ?", UnitCreditInstance.class);
        q.setParameter(1, saleRowId);
        q.setParameter(2, extTxId);
        List<UnitCreditInstance> uciList = q.getResultList();
        return uciList;
    }

    public static Date getMaxExpiryDateByUnitCreditWrapperClass(EntityManager em, long accId, String name) {
        Query q = em.createNativeQuery("SELECT ifnull(max(EXPIRY_DATE),now()) FROM unit_credit_instance I, unit_credit_specification S"
                + " where S.UNIT_CREDIT_SPECIFICATION_ID = I.UNIT_CREDIT_SPECIFICATION_ID and I.ACCOUNT_ID = ? and S.WRAPPER_CLASS = ?");
        q.setParameter(1, accId);
        q.setParameter(2, name);
        return (Date) q.getSingleResult();
    }

    public static Date getMaxExpiryDateByUnitCreditSpecId(EntityManager em, long accId, int specId) {
        Query q = em.createNativeQuery("SELECT ifnull(max(EXPIRY_DATE),now()) FROM unit_credit_instance I"
                + " where I.ACCOUNT_ID = ? and I.UNIT_CREDIT_SPECIFICATION_ID = ?");
        q.setParameter(1, accId);
        q.setParameter(2, specId);
        return (Date) q.getSingleResult();
    }

    public static Date getMaxExpiryDateByUnitCreditSpecIdAndSaleRowId(EntityManager em, long accId, int specId, int saleLineId) {
        Query q = em.createNativeQuery("SELECT ifnull(max(EXPIRY_DATE),now()) FROM unit_credit_instance I"
                + " where I.ACCOUNT_ID = ? and I.UNIT_CREDIT_SPECIFICATION_ID = ? and I.SALE_ROW_ID = ?");
        q.setParameter(1, accId);
        q.setParameter(2, specId);
        q.setParameter(3, saleLineId);

        return (Date) q.getSingleResult();
    }

    public static long getCountOfOtherAvailableUnitCreditsOnSI(EntityManager em, int unitCreditSpecificationId, int productInstanceId, int unitCreditInstanceId) {
        Query q = em.createNativeQuery("SELECT count(*) FROM unit_credit_instance"
                + " where UNIT_CREDIT_SPECIFICATION_ID = ? and PRODUCT_INSTANCE_ID = ? and UNIT_CREDIT_INSTANCE_ID != ? and EXPIRY_DATE > now() and START_DATE <= now()");
        q.setParameter(1, unitCreditSpecificationId);
        q.setParameter(2, productInstanceId);
        q.setParameter(3, unitCreditInstanceId);
        return (Long) q.getSingleResult();
    }

    public static UnitCreditInstance createUnitCreditInstance(EntityManager em, int specId, IAccount acc, Date validUntil, double units, double revenueCentsPerUnit, double baselineCentsPerUnit,
            int productInstanceId, double posCentsCharged, double posCentsDiscount, Date startDate, String info, String extTxid, int saleLineId, Date endDate,
            Date revenueFirstDate, Date revenueLastDate, Double revenueCentsPerDay) {
        log.debug("Creating UCI in database");
        UnitCreditInstance uci = new UnitCreditInstance();
        uci.setAccountId(acc.getAccountId());
        uci.setExpiryDate(validUntil);
        uci.setEndDate(endDate);
        uci.setPurchaseDate(new Date());
        uci.setUnitCreditSpecificationId(specId);
        uci.setUnitsRemaining(new BigDecimal(units));
        uci.setRevenueCentsPerUnit(new BigDecimal(Double.isInfinite(revenueCentsPerUnit) || Double.isNaN(revenueCentsPerUnit) ? 0 : revenueCentsPerUnit));
        uci.setProductInstanceId(productInstanceId);
        uci.setFreeCentsPerUnit(BigDecimal.ZERO);
        uci.setPOSCentsCharged(new BigDecimal(posCentsCharged));
        uci.setPOSCentsDiscount(new BigDecimal(posCentsDiscount));
        uci.setBaselineCentsPerUnit(new BigDecimal(baselineCentsPerUnit));
        uci.setUnitsAtStart(new BigDecimal(units));
        uci.setStartDate(startDate == null ? uci.getPurchaseDate() : startDate);
        uci.setInfo(info == null ? "" : info);
        uci.setExtTxid(extTxid == null ? "" : extTxid);
        uci.setProvisionedByCustomerProfileId(0); // Column can be removed when history is no longer required. Join to sale to see salesperson
        uci.setSaleRowId(saleLineId);
        uci.setRevenueFirstDate(revenueFirstDate);
        uci.setRevenueLastDate(revenueLastDate);
        uci.setRevenueCentsPerDay(revenueCentsPerDay == null ? null : new BigDecimal(revenueCentsPerDay));
        em.persist(uci);
        em.flush();
        em.refresh(uci);
        log.debug("Created UCI in database [{}]", uci.getUnitCreditInstanceId());
        return uci;
    }

    /**
     * *****************************************
     * RATING FUNCTIONS *****************************************
     */
    public static List<RatePlanAvp> getRatePlansAvps(EntityManager em, int planId) {
        Query q = em.createNativeQuery("SELECT * FROM rate_plan_avp WHERE RATE_PLAN_ID = ?", RatePlanAvp.class);
        q.setParameter(1, planId);
        return q.getResultList();
    }

    public static RatePlan getRatePlan(EntityManager em, int planid) {
        Query q = em.createNativeQuery("SELECT * FROM rate_plan WHERE RATE_PLAN_ID = ?", RatePlan.class);
        q.setParameter(1, planid);
        return (RatePlan) q.getSingleResult();
    }

    public static void createServiceInstanceMapping(EntityManager em, int serviceInstanceId, String identity, String identityType) {
        ServiceInstanceMappingPK key = new ServiceInstanceMappingPK();
        key.setIdentifier(identity);
        key.setIdentifierType(identityType);
        key.setServiceInstanceId(serviceInstanceId);
        ServiceInstanceMapping m = new ServiceInstanceMapping();
        m.setServiceInstanceMappingPK(key);
        em.persist(m);
    }

    public static void deleteServiceInstanceMapping(EntityManager em, int serviceInstanceId, String identity, String identityType) {
        ServiceInstanceMappingPK key = new ServiceInstanceMappingPK();
        key.setIdentifier(identity);
        key.setIdentifierType(identityType);
        key.setServiceInstanceId(serviceInstanceId);
        ServiceInstanceMapping m = JPAUtils.findAndThrowENFE(em, ServiceInstanceMapping.class, key);
        em.remove(m);
        em.flush();
    }

    public static void deleteServiceInstancesMappings(EntityManager em, int serviceInstanceId) {
        Query q = em.createNativeQuery("DELETE FROM service_instance_mapping WHERE SERVICE_INSTANCE_ID = ?");
        q.setParameter(1, serviceInstanceId);
        q.executeUpdate();
        em.flush();
    }

    public static void updateServiceInstancesUnitCreditAccount(EntityManager em, long accountId, int serviceInstanceId) {
        log.debug("Going to execute: update unit_credit_instance set account_id='{}' where product_instance_id=(select product_instance_id from service_instance where service_instance_id={})  and expiry_date > now()", accountId, serviceInstanceId);
        Query q = em.createNativeQuery("update unit_credit_instance set account_id=? where product_instance_id=(select product_instance_id from service_instance where service_instance_id=?)  and expiry_date > now()");
        q.setParameter(1, accountId);
        q.setParameter(2, serviceInstanceId);
        q.executeUpdate();
        em.flush();
    }

    /**
     * *****************************************
     * UTILITY FUNCTIONS *****************************************
     */
    public static boolean isNegative(BigDecimal d) {
        return (d.compareTo(BigDecimal.ZERO) < 0);
    }

    public static boolean isZeroOrNegative(BigDecimal d) {
        return (d.compareTo(BigDecimal.ZERO) <= 0);
    }

    public static boolean isPositive(BigDecimal d) {
        return (d.compareTo(BigDecimal.ZERO) > 0);
    }

    public static boolean isZeroOrPositive(BigDecimal d) {
        return (d.compareTo(BigDecimal.ZERO) >= 0);
    }

    public static boolean isZero(BigDecimal d) {
        return (d.compareTo(BigDecimal.ZERO) == 0);
    }

    public static List<ServiceInstanceMapping> getServiceInstanceMappings(EntityManager em, int siId) {
        Query q = em.createNativeQuery("SELECT * FROM service_instance_mapping where SERVICE_INSTANCE_ID = ?", ServiceInstanceMapping.class);
        q.setParameter(1, siId);
        return q.getResultList();
    }

    public static void replaceMappings(EntityManager em, String oldidentifier, String oldidentifierType, String newidentifier, String newidentifierType) {
        Query q = em.createNativeQuery("UPDATE service_instance_mapping m set m.IDENTIFIER=?, m.IDENTIFIER_TYPE=? WHERE "
                + " m.IDENTIFIER=? AND m.IDENTIFIER_TYPE=?");
        q.setParameter(1, newidentifier);
        q.setParameter(2, newidentifierType);
        q.setParameter(3, oldidentifier);
        q.setParameter(4, oldidentifierType);
        int cnt = q.executeUpdate();
        log.debug("Replaced [{}] mappings", cnt);
    }

    /*
     *
     * Caching for performance. Reload properties in order for cache to be
     * cleared
     *
     */
    public static UnitCreditSpecification getUnitCreditSpecification(EntityManager em, int specId) {
        UnitCreditSpecification ucs = BMDataCache.unitCreditSpecificationCacheById.get(specId);

        if (ucs == null) {

            Query q = em.createNativeQuery("SELECT * FROM unit_credit_specification where UNIT_CREDIT_SPECIFICATION_ID = ?", UnitCreditSpecification.class);
            q.setParameter(1, specId);

            try {
                ucs = (UnitCreditSpecification) q.getSingleResult();
                em.detach(ucs);
                BMDataCache.unitCreditSpecificationCacheById.put(specId, ucs);
            } catch (Exception e) {
                log.warn("XXX - ucs == null, TRIED to run:  SELECT * FROM unit_credit_specification where UNIT_CREDIT_SPECIFICATION_ID = {}, returned {}", specId, ucs);
                ucs = (UnitCreditSpecification) q.getSingleResult();
                em.detach(ucs);
                BMDataCache.unitCreditSpecificationCacheById.put(specId, ucs);
            }
        }

        return ucs;
    }

    public static boolean doesServiceHaveSocialMediaChargeForToday(EntityManager em, long accountId, int serviceInstanceId, String txType, String rateGroup) throws Exception {
        Query q = em.createNativeQuery("select count(*) from account_history where account_id=? and start_date > now() - interval 24 hour and service_instance_id=? and transaction_type=? and rate_group=?;");
        q.setParameter(1, accountId);
        q.setParameter(2, serviceInstanceId);
        q.setParameter(3, txType);
        q.setParameter(4, rateGroup);

        try {
            long count = (Long) q.getSingleResult();
            return (count > 0);
        } catch (Exception e) {
            log.warn("Error while checking is service [{}] has social media charge for today, Error [{}]", serviceInstanceId, e.toString());
            throw e;
        }
    }

    public static Date getAccountsLatestProductInstanceCreatedDate(EntityManager em, long accId) {

        String key = "ACC_LATEST_PROD_CRE_DATE_" + accId;
        Date cachedDate = CacheHelper.getFromLocalCache(key, Date.class);
        if (cachedDate != null) {
            return cachedDate;
        }

        Query q = em.createNativeQuery("select max(PrI.CREATED_DATETIME) from product_instance PrI join service_instance SI on PrI.PRODUCT_INSTANCE_ID = SI.PRODUCT_INSTANCE_ID "
                + "and SI.account_id = ?;");
        q.setParameter(1, accId);
        Date ret = null;
        try {
            ret = (Date) q.getSingleResult();
        } catch (Exception e) {
            log.debug("No product instances");
        }

        return ret;
    }

    public static boolean doesAccountHaveUnitCreditOfType(EntityManager em, long accountId, String type) {

        Query q = em.createNativeQuery("select count(*) from unit_credit_instance UCI join unit_credit_specification UCS\n "
                + " on (UCI.UNIT_CREDIT_SPECIFICATION_ID = UCS.UNIT_CREDIT_SPECIFICATION_ID \n "
                + "    and UCI.account_id=? and UCS.wrapper_class=? and UCI.expiry_date >  now());");
        q.setParameter(1, accountId);
        q.setParameter(2, type);

        try {
            Long count = (Long) q.getSingleResult();
            return (count > 0);
        } catch (Exception e) {
            log.error("No unit credits", e);
        }
        return false;

    }

    public static Date getMaximumExpiryDateOfUnitCreditWithThisTypeExcludingCurrentOne(EntityManager em, String wrapperClasses, long accountId, int excludeUnitCredit) {
        Query q = em.createNativeQuery("select max(UCI.expiry_date) \n"
                + "from unit_credit_instance UCI join unit_credit_specification UCS \n"
                + "on (UCI.unit_credit_specification_id = UCS.unit_credit_specification_id and UCI.expiry_date > now() and UCS.wrapper_class in (" + wrapperClasses + ") \n"
                + "and account_id=? and UCI.unit_credit_instance_id != ?);");

        q.setParameter(1, accountId);
        q.setParameter(2, excludeUnitCredit);

        Date ret = null;
        try {
            ret = (Date) q.getSingleResult();
        } catch (Exception e) {
            log.error("No unit credits", e);
        }
        return ret;
    }

    public static UnitCreditSpecification getUnitCreditSpecificationByName(EntityManager em, String name) {
        UnitCreditSpecification ucs = BMDataCache.unitCreditSpecificationCacheByName.get(name);
        if (ucs == null) {
            Query q = em.createNativeQuery("SELECT * FROM unit_credit_specification where UNIT_CREDIT_NAME = ? AND AVAILABLE_FROM <= now() and AVAILABLE_TO > now() ORDER BY PRICE_CENTS DESC LIMIT 1", UnitCreditSpecification.class);
            q.setParameter(1, name);
            ucs = (UnitCreditSpecification) q.getSingleResult();
            em.detach(ucs);
            BMDataCache.unitCreditSpecificationCacheByName.put(name, ucs);
        }
        return ucs;
    }

    public static UnitCreditSpecification getUnitCreditSpecificationByItemNumber(EntityManager em, String itemNumber) {
        UnitCreditSpecification ucs = BMDataCache.unitCreditSpecificationCacheByItemNumber.get(itemNumber);
        if (ucs == null) {
            Query q = em.createNativeQuery("SELECT * FROM unit_credit_specification where ITEM_NUMBER = ? ORDER BY UNIT_CREDIT_SPECIFICATION_ID LIMIT 1", UnitCreditSpecification.class);
            q.setParameter(1, itemNumber);
            ucs = (UnitCreditSpecification) q.getSingleResult();
            em.detach(ucs);
            BMDataCache.unitCreditSpecificationCacheByItemNumber.put(itemNumber, ucs);
        }
        return ucs;
    }

    public static void extendUnitCreditExpiryAndEndDate(EntityManager em, Integer unitCreditInstanceId, Date currentExpiryDate, Date newExpiryDate) {
        // Include current date in the query so we dont extend twice by mistake with many threads
        Query q = em.createNativeQuery("update unit_credit_instance set expiry_date=?, end_date=? where unit_credit_instance_id=? and expiry_date=?");
        q.setParameter(1, newExpiryDate);
        q.setParameter(2, newExpiryDate);
        q.setParameter(3, unitCreditInstanceId);
        q.setParameter(4, currentExpiryDate);
        q.executeUpdate();
        em.flush();
    }

    public static void updateProductInstanceBeenUsed(EntityManager em, ServiceInstance serviceInstance, Date newLastUsedDate, boolean revenueGenerating, String imei) {

        if (revenueGenerating && serviceInstance.getProductInstanceLastRGActivityDate() != null && Utils.areDatesOnSameDay(newLastUsedDate, serviceInstance.getProductInstanceLastRGActivityDate())) {
            // Nothing to do
            return;
        }
        if (!revenueGenerating && serviceInstance.getProductInstanceLastActivityDate() != null && Utils.areDatesOnSameDay(newLastUsedDate, serviceInstance.getProductInstanceLastActivityDate())) {
            // Nothing to do
            return;
        }

        Query q = em.createNativeQuery("SELECT FIRST_ACTIVITY_DATETIME, LAST_ACTIVITY_DATETIME, PAUSED_ACTIVITY_DATETIME, "
                + "LAST_RECONNECTION_DATETIME, FIRST_RG_ACTIVITY_DATETIME, LAST_RG_ACTIVITY_DATETIME, PAUSED_RG_ACTIVITY_DATETIME, "
                + "LAST_RG_RECONNECTION_DATETIME, LAST_IMEI FROM product_instance  WHERE PRODUCT_INSTANCE_ID=?");
        q.setParameter(1, serviceInstance.getProductInstanceId());
        Object[] row = (Object[]) q.getSingleResult();
        Date firstActivity = (Date) row[0];
        Date lastActivity = (Date) row[1];
        Date pausedActivity = (Date) row[2];
        Date lastRecon = (Date) row[3];
        Date firstRGActivity = (Date) row[4];
        Date lastRGActivity = (Date) row[5];
        Date pausedRGActivity = (Date) row[6];
        Date lastRGRecon = (Date) row[7];
        String lastIMEI = (String) row[8];

        if (log.isDebugEnabled()) {
            log.debug("Before Stats Update: Revenue generating traffic [{}]. Product instance [{}] has dates firstActivity [{}] lastActivity [{}] pausedActivity [{}] lastRecon [{}] firstRGActivity [{}] lastRGActivity [{}] pausedRGActivity [{}] lastRGRecon [{}] lastIMEI [{}]", new Object[]{revenueGenerating, serviceInstance.getProductInstanceId(), firstActivity, lastActivity, pausedActivity, lastRecon, firstRGActivity, lastRGActivity, pausedRGActivity, lastRGRecon, lastIMEI});
        }
        Date newFirstActivity, newLastActivity, newPausedActivity, newLastRecon, newFirstRGActivity, newLastRGActivity, newPausedRGActivity, newLastRGRecon;
        String newLastIMEI;
        long beginningOfPreviousDay = Utils.getBeginningOfPreviousDay(newLastUsedDate).getTime();

        if (imei != null && imei.startsWith("IMEISV=")) {
            newLastIMEI = imei;
        } else {
            newLastIMEI = lastIMEI;
        }

        if (lastActivity == null) {
            log.debug("Product Instance has never been used before. This is the first time (although the logical SIM may have been used before)");
            // Lets see if there is a previous PI for this logical SIM
            q = em.createNativeQuery("select max(P1.LAST_ACTIVITY_DATETIME) from product_instance P1 join product_instance P2 on P1.LOGICAL_ID=P2.LOGICAL_ID where P2.PRODUCT_INSTANCE_ID=?");
            q.setParameter(1, serviceInstance.getProductInstanceId());
            lastActivity = (Date) q.getSingleResult();
        }
        // Any Activity dates
        // If first activity date is null, set it to newLastUsedDate
        // If last activity date is before yesterday, set last paused date to last activity date
        // Set last activity date to newLastUsedDate
        if (firstActivity == null) {
            newFirstActivity = newLastUsedDate;
        } else {
            newFirstActivity = firstActivity;
        }
        newLastActivity = newLastUsedDate;
        if (lastActivity != null && lastActivity.getTime() < beginningOfPreviousDay) {
            newPausedActivity = lastActivity;
            newLastRecon = newLastUsedDate;
        } else {
            // Dont change
            newPausedActivity = pausedActivity;
            newLastRecon = lastRecon;
        }

        if (revenueGenerating) {
            if (firstRGActivity == null) {
                newFirstRGActivity = newLastUsedDate;
            } else {
                newFirstRGActivity = firstRGActivity;
            }
            newLastRGActivity = newLastUsedDate;
            if (lastRGActivity != null && lastRGActivity.getTime() < beginningOfPreviousDay) {
                newPausedRGActivity = lastRGActivity;
                newLastRGRecon = newLastUsedDate;
            } else {
                newPausedRGActivity = pausedRGActivity;
                newLastRGRecon = lastRGRecon;
            }
        } else {
            // Dont change RG dates
            newFirstRGActivity = firstRGActivity;
            newLastRGActivity = lastRGActivity;
            newPausedRGActivity = pausedRGActivity;
            newLastRGRecon = lastRGRecon;
        }

        if (log.isDebugEnabled()) {
            log.debug("After Stats Update: Revenue generating traffic [{}]. Product instance [{}] will have dates firstActivity [{}] lastActivity [{}] pausedActivity [{}] lastRecon [{}] firstRGActivity [{}] lastRGActivity [{}] pausedRGActivity [{}] lastRGRecon [{}] lastIMEI [{}]", new Object[]{revenueGenerating, serviceInstance.getProductInstanceId(), newFirstActivity, newLastActivity, newPausedActivity, newLastRecon, newFirstRGActivity, newLastRGActivity, newPausedRGActivity, newLastRGRecon, newLastIMEI});
        }

        createProductInstanceHistory(em, serviceInstance.getProductInstanceId());

        q = em.createNativeQuery("UPDATE product_instance SET FIRST_ACTIVITY_DATETIME=?, LAST_ACTIVITY_DATETIME=?, PAUSED_ACTIVITY_DATETIME=?, LAST_RECONNECTION_DATETIME=?, "
                + "FIRST_RG_ACTIVITY_DATETIME=?, LAST_RG_ACTIVITY_DATETIME=?, PAUSED_RG_ACTIVITY_DATETIME=?, LAST_RG_RECONNECTION_DATETIME=?, "
                + "LAST_IMEI=? WHERE PRODUCT_INSTANCE_ID=?");
        q.setParameter(1, newFirstActivity);
        q.setParameter(2, newLastActivity);
        q.setParameter(3, newPausedActivity);
        q.setParameter(4, newLastRecon);
        q.setParameter(5, newFirstRGActivity);
        q.setParameter(6, newLastRGActivity);
        q.setParameter(7, newPausedRGActivity);
        q.setParameter(8, newLastRGRecon);
        q.setParameter(9, newLastIMEI);
        q.setParameter(10, serviceInstance.getProductInstanceId());
        log.debug("Updating stats on PI");
        q.executeUpdate();
        em.flush();
        log.debug("Updated stats on PI");
    }

    public static void createProductInstanceHistory(EntityManager em, int productInstanceId) {
        if (BaseUtils.getBooleanProperty("env.product.instance.history.keep", false)) {
            log.debug("Deleting old product instance history");
            Query deleteOld = em.createNativeQuery("DELETE from product_instance_history where product_instance_id=? and INSERTED_DATETIME < NOW() - INTERVAL ? DAY");
            deleteOld.setParameter(1, productInstanceId);
            deleteOld.setParameter(2, BaseUtils.getIntProperty("env.product.instance.history.days.keep", 35));
            deleteOld.executeUpdate();
            em.flush();
            log.debug("Inserting product instance history");
            Query historyInsert = em.createNativeQuery(PI_HISTORY_INSERT);
            historyInsert.setParameter(1, productInstanceId);
            historyInsert.executeUpdate();
            em.flush();
            log.debug("Inserted product instance history");
        }
    }

    public static void updateServiceInstanceBeenUsed(EntityManager em, ServiceInstance serviceInstance, Date newLastUsedDate, boolean revenueGenerating) {

        if (revenueGenerating && serviceInstance.getLastRGActivityDate() != null && Utils.areDatesOnSameDay(newLastUsedDate, serviceInstance.getLastRGActivityDate())) {
            // Nothing to do
            return;
        }
        if (!revenueGenerating && serviceInstance.getLastActivityDate() != null && Utils.areDatesOnSameDay(newLastUsedDate, serviceInstance.getLastActivityDate())) {
            // Nothing to do
            return;
        }

        Query q = em.createNativeQuery("SELECT FIRST_ACTIVITY_DATETIME, LAST_ACTIVITY_DATETIME, PAUSED_ACTIVITY_DATETIME, "
                + "LAST_RECONNECTION_DATETIME, FIRST_RG_ACTIVITY_DATETIME, LAST_RG_ACTIVITY_DATETIME, PAUSED_RG_ACTIVITY_DATETIME, "
                + "LAST_RG_RECONNECTION_DATETIME FROM service_instance  WHERE SERVICE_INSTANCE_ID=?");
        q.setParameter(1, serviceInstance.getServiceInstanceId());
        Object[] row = (Object[]) q.getSingleResult();
        Date firstActivity = (Date) row[0];
        Date lastActivity = (Date) row[1];
        Date pausedActivity = (Date) row[2];
        Date lastRecon = (Date) row[3];
        Date firstRGActivity = (Date) row[4];
        Date lastRGActivity = (Date) row[5];
        Date pausedRGActivity = (Date) row[6];
        Date lastRGRecon = (Date) row[7];

        if (log.isDebugEnabled()) {
            log.debug("Before Stats Update: Revenue generating traffic [{}]. Service instance [{}] has dates firstActivity [{}] lastActivity [{}] pausedActivity [{}] lastRecon [{}] firstRGActivity [{}] lastRGActivity [{}] pausedRGActivity [{}] lastRGRecon [{}] lastIMEI [{}]", new Object[]{revenueGenerating, serviceInstance.getServiceInstanceId(), firstActivity, lastActivity, pausedActivity, lastRecon, firstRGActivity, lastRGActivity, pausedRGActivity, lastRGRecon});
        }
        Date newFirstActivity, newLastActivity, newPausedActivity, newLastRecon, newFirstRGActivity, newLastRGActivity, newPausedRGActivity, newLastRGRecon;
        long beginningOfPreviousDay = Utils.getBeginningOfPreviousDay(newLastUsedDate).getTime();

        // Any Activity dates
        // If first activity date is null, set it to newLastUsedDate
        // If last activity date is before yesterday, set last paused date to last activity date
        // Set last activity date to newLastUsedDate
        if (firstActivity == null) {
            newFirstActivity = newLastUsedDate;
        } else {
            newFirstActivity = firstActivity;
        }
        newLastActivity = newLastUsedDate;
        if (lastActivity != null && lastActivity.getTime() < beginningOfPreviousDay) {
            newPausedActivity = lastActivity;
            newLastRecon = newLastUsedDate;
        } else {
            // Dont change
            newPausedActivity = pausedActivity;
            newLastRecon = lastRecon;
        }

        if (revenueGenerating) {
            if (firstRGActivity == null) {
                newFirstRGActivity = newLastUsedDate;
            } else {
                newFirstRGActivity = firstRGActivity;
            }
            newLastRGActivity = newLastUsedDate;
            if (lastRGActivity != null && lastRGActivity.getTime() < beginningOfPreviousDay) {
                newPausedRGActivity = lastRGActivity;
                newLastRGRecon = newLastUsedDate;
            } else {
                newPausedRGActivity = pausedRGActivity;
                newLastRGRecon = lastRGRecon;
            }
        } else {
            // Dont change RG dates
            newFirstRGActivity = firstRGActivity;
            newLastRGActivity = lastRGActivity;
            newPausedRGActivity = pausedRGActivity;
            newLastRGRecon = lastRGRecon;
        }

        if (log.isDebugEnabled()) {
            log.debug("After Stats Update: Revenue generating traffic [{}]. Service instance [{}] will have dates firstActivity [{}] lastActivity [{}] pausedActivity [{}] lastRecon [{}] firstRGActivity [{}] lastRGActivity [{}] pausedRGActivity [{}] lastRGRecon [{}] lastIMEI [{}]", new Object[]{revenueGenerating, serviceInstance.getServiceInstanceId(), newFirstActivity, newLastActivity, newPausedActivity, newLastRecon, newFirstRGActivity, newLastRGActivity, newPausedRGActivity, newLastRGRecon});
        }

        q = em.createNativeQuery("UPDATE service_instance SET FIRST_ACTIVITY_DATETIME=?, LAST_ACTIVITY_DATETIME=?, PAUSED_ACTIVITY_DATETIME=?, LAST_RECONNECTION_DATETIME=?, "
                + "FIRST_RG_ACTIVITY_DATETIME=?, LAST_RG_ACTIVITY_DATETIME=?, PAUSED_RG_ACTIVITY_DATETIME=?, LAST_RG_RECONNECTION_DATETIME=? "
                + "WHERE SERVICE_INSTANCE_ID=?");
        q.setParameter(1, newFirstActivity);
        q.setParameter(2, newLastActivity);
        q.setParameter(3, newPausedActivity);
        q.setParameter(4, newLastRecon);
        q.setParameter(5, newFirstRGActivity);
        q.setParameter(6, newLastRGActivity);
        q.setParameter(7, newPausedRGActivity);
        q.setParameter(8, newLastRGRecon);
        q.setParameter(9, serviceInstance.getServiceInstanceId());
        log.debug("Updating stats on SI");
        q.executeUpdate();
        em.flush();
        log.debug("Updated stats on SI");
    }

    public static String getNetworkConfig(EntityManager em, String param) {
        Query q = em.createNativeQuery("select VALUE from Network.parameter where NAME=?");
        q.setParameter(1, param);
        return (String) q.getSingleResult();
    }

    public static int getSectorCongestion(EntityManager em, String location) {
        Query q = em.createNativeQuery("select UPLINK_PERCENT, DOWNLINK_PERCENT from Network.sector_congestion where TAI_ECGI=?");
        q.setParameter(1, location);
        Object[] res = (Object[]) q.getSingleResult();
        int up = (int) res[0];
        int down = (int) res[1];
        return Math.max(up, down);
    }

    public static int getProductInstanceOrganisationId(EntityManager em, int productInstanceId) {
        Query q = em.createNativeQuery("select organisation_id from product_instance where product_instance_id=?");
        q.setParameter(1, productInstanceId);
        return (int) q.getSingleResult();
    }

    public static AccountHistory getAccountHistoryByServiceInstanceIdAndExtTxIdId(EntityManager em, String exttxid, int serviceInstanceId) {
        Query q = em.createNativeQuery("select * from account_history where service_instance_id=? and ext_txid=?", AccountHistory.class);
        q.setParameter(1, serviceInstanceId);
        q.setParameter(2, exttxid);
        try {
            return (AccountHistory) q.getSingleResult();
        } catch (javax.persistence.NoResultException nre) {
            return null;
        }
    }

    private static void writeUnitCreditCharges(EntityManager em, Long ahid, Long cdid, List<UCChargeResult> unitCreditChanges) {
        if (!BaseUtils.getBooleanProperty("env.bm.write.uci.charge", false)) {
            return;
        }
        if (unitCreditChanges == null || unitCreditChanges.isEmpty()) {
            return;
        }
        for (UCChargeResult uccr : unitCreditChanges) {
            if (uccr.getUnitCreditInstanceId() == 0) {
                continue;
            }
            if (!BaseUtils.getBooleanProperty("env.bm.new.uccharge.logic", false)) {
                if (DAO.isPositive(uccr.getBaselineUnitsCharged())) {
                    writeUnitCreditCharge(em, ahid, cdid, uccr.getUnitCreditInstanceId(), uccr.getUnitsRemaining(), uccr.getBaselineUnitsCharged(), uccr.getRevenueCents());
                } else if (DAO.isPositive(uccr.getUnitsCharged())) {
                    writeUnitCreditCharge(em, ahid, cdid, uccr.getUnitCreditInstanceId(), uccr.getUnitsRemaining(), uccr.getUnitsCharged(), uccr.getRevenueCents());
                }
            } else if (DAO.isPositive(uccr.getUnitsCharged()) || DAO.isPositive(uccr.getBaselineUnitsCharged())) {
                writeUnitCreditChargeNew(em, ahid, cdid, uccr.getUnitCreditInstanceId(), uccr.getUnitsRemaining(), uccr.getUnitsCharged(), uccr.getBaselineUnitsCharged(), uccr.getRevenueCents());
            }
        }
    }

    private static void writeUnitCreditCharge(EntityManager em, Long ahid, Long cdid, int uciid, BigDecimal unitsRemaining, BigDecimal unitsCharged, BigDecimal revenueCents) {
        try {
            log.debug("Writing UCI charge");
            Query q = em.createNativeQuery("INSERT INTO unit_credit_instance_charge (ACCOUNT_HISTORY_ID,CHARGING_DETAIL_ID,UNIT_CREDIT_INSTANCE_ID,UNITS_REMAINING,UNITS_CHARGED,REVENUE_CENTS) values(?,?,?,?,?,?)");
            q.setParameter(1, ahid);
            q.setParameter(2, cdid);
            q.setParameter(3, uciid);
            q.setParameter(4, unitsRemaining);
            q.setParameter(5, unitsCharged);
            q.setParameter(6, revenueCents);
            q.executeUpdate();
            log.debug("Wrote UCI charge");
        } catch (Exception e) {
            log.warn("Error: ", e);
        }
    }

    private static void writeUnitCreditChargeNew(EntityManager em, Long ahid, Long cdid, int uciid, BigDecimal unitsRemaining, BigDecimal unitsCharged, BigDecimal baselineUnitsCharged, BigDecimal revenueCents) {
        try {
            log.debug("Writing UCI charge - new");
            Query q = em.createNativeQuery("INSERT INTO unit_credit_instance_charge (ACCOUNT_HISTORY_ID,CHARGING_DETAIL_ID,UNIT_CREDIT_INSTANCE_ID,UNITS_REMAINING,UNITS_CHARGED,BASELINE_UNITS_CHARGED,REVENUE_CENTS) values(?,?,?,?,?,?,?)");
            q.setParameter(1, ahid);
            q.setParameter(2, cdid);
            q.setParameter(3, uciid);
            q.setParameter(4, unitsRemaining);
            q.setParameter(5, unitsCharged);
            q.setParameter(6, baselineUnitsCharged);
            q.setParameter(7, revenueCents);
            q.executeUpdate();
            log.debug("Wrote UCI charge - new");
        } catch (Exception e) {
            log.warn("Error: ", e);
        }
    }

    public static Date getAccountsLastDataOrVoiceBundleRevenueGeneration(EntityManager em, IAccount acc) {
        String key = "LAST_DATA_REV_" + acc.getAccountId();
        Date cachedDate = CacheHelper.getFromLocalCache(key, Date.class);
        if (cachedDate != null) {
            return cachedDate;
        }

        Query q = em.createNativeQuery("select max(UCI.LAST_USED_DATE) from unit_credit_instance UCI "
                + "join unit_credit_specification UCS on UCI.UNIT_CREDIT_SPECIFICATION_ID = UCS.UNIT_CREDIT_SPECIFICATION_ID "
                + "where UCI.ACCOUNT_ID=? and UCS.UNIT_TYPE in ('Byte','Second')  and (UCI.CROSSOVER_DATE is null or UCI.CROSSOVER_DATE > now() - interval 30 day)");
        q.setParameter(1, acc.getAccountId());
        Date ret = null;
        try {
            ret = (Date) q.getSingleResult();
        } catch (Exception e) {
            log.debug("No data unit credits");
        }
        if (ret == null) {
            ret = new Date(0);
        }
        log.debug("Max date is [{}]", ret);
        CacheHelper.putInLocalCache(key, ret, 2);
        return ret;
    }

    public static Date getAccountsLastDataBundleRevenueGeneration(EntityManager em, IAccount acc) {
        String key = "LAST_DATA_REV_" + acc.getAccountId();
        Date cachedDate = CacheHelper.getFromLocalCache(key, Date.class);
        if (cachedDate != null) {
            return cachedDate;
        }

        Query q = em.createNativeQuery("select max(UCI.LAST_USED_DATE) from unit_credit_instance UCI "
                + "join unit_credit_specification UCS on UCI.UNIT_CREDIT_SPECIFICATION_ID = UCS.UNIT_CREDIT_SPECIFICATION_ID "
                + "where UCI.ACCOUNT_ID=? and UCS.UNIT_TYPE in ('Byte')  and (UCI.CROSSOVER_DATE is null or UCI.CROSSOVER_DATE > now() - interval 30 day)");
        q.setParameter(1, acc.getAccountId());
        Date ret = null;
        try {
            ret = (Date) q.getSingleResult();
        } catch (Exception e) {
            log.debug("No data unit credits");
        }
        if (ret == null) {
            ret = new Date(0);
        }
        log.debug("Max date is [{}]", ret);
        CacheHelper.putInLocalCache(key, ret, 2);
        return ret;
    }

    @PostConstruct
    public void startUp() {
        BaseUtils.registerForPropsChanges(this);
        emf = JPAUtils.getEMF("BMPU_RL");
    }

    @PreDestroy
    public void shutDown() {
        BaseUtils.deregisterForPropsChanges(this);
        JPAUtils.closeEMF(emf);
    }

    @Override
    public void propsAreReadyTrigger() {
    }

    @Override
    public void propsHaveChangedTrigger() {
        log.debug("Properties have changed");
    }

    /**
     * *****************************************
     *
     * THESE FUNCTIONS USE CATALOG MANAGER TABLES IDEALLY, WE SHOULD CALL CM,
     * BUT FOR PERFORMANCE REASONS, WE ARE TAKING A SHORTCUT
     *
     ******************************************
     */
    public static Integer getProdSvcSpecRatePlanId(EntityManager em, int prodSpecId, int svcSpecId) {
        Query q = em.createNativeQuery("SELECT RATE_PLAN_ID FROM "
                + "product_service_mapping "
                + "WHERE PRODUCT_SPECIFICATION_ID=? AND SERVICE_SPECIFICATION_ID=?");
        q.setParameter(1, prodSpecId);
        q.setParameter(2, svcSpecId);
        return (Integer) q.getSingleResult();
    }

    public static int getServiceInstancesDataServiceInstanceId(EntityManager em, int serviceInstanceID) {
        Query q = em.createNativeQuery("select SI_DATA.SERVICE_INSTANCE_ID from service_instance SI_NON_DATA "
                + "join service_instance SI_DATA on (SI_NON_DATA.PRODUCT_INSTANCE_ID = SI_DATA.PRODUCT_INSTANCE_ID) "
                + "join service_specification S on (SI_DATA.SERVICE_SPECIFICATION_ID = S.SERVICE_SPECIFICATION_ID) "
                + "where SI_NON_DATA.SERVICE_INSTANCE_ID = ? AND S.SERVICE_CODE like '%32251%' and SI_DATA.STATUS = 'AC'");
        q.setParameter(1, serviceInstanceID);
        return (Integer) q.getSingleResult();
    }

    private static String getServiceSpecIdsForServiceCode(EntityManager em, String serviceCode) {

        Map<String, String> cache = BMDataCache.serviceCodeServiceSpecIdCache;
        String specIds = cache.get(serviceCode);
        if (specIds == null) {
            log.debug("Populating serviceCodeServiceSpecIdCache with service code [{}]", serviceCode);
            specIds = "";
            boolean matchOnRegex = BaseUtils.getBooleanProperty("env.bm.servicecode.is.regex", true);
            Query q = em.createNativeQuery("select service_specification_id, service_code from service_specification");
            List<Object[]> resList = (List<Object[]>) q.getResultList();
            for (Object[] res : resList) {
                Integer svcSpecId = (Integer) res[0];
                String svcCode = (String) res[1];
                if ((matchOnRegex && Utils.matchesWithPatternCache(serviceCode, svcCode))
                        || (!matchOnRegex && svcCode.equals(serviceCode))) {
                    specIds = specIds + svcSpecId + ",";
                }
            }
            if (!specIds.isEmpty()) {
                specIds = specIds.substring(0, specIds.length() - 1); // remove trailing ,   123,456, should be substring(0,7)
            }
            cache.put(serviceCode, specIds);
            log.debug("Populated serviceCodeServiceSpecIdCache with service code [{}] mapping to spec Id's [{}]", serviceCode, specIds);
        }
        return specIds;
    }

    private static String getServiceSpecIdsForDataServices(EntityManager em) {

        String serviceCode = "32251"; //32251 is common in all OPCOs for data services

        Map<String, String> cache = BMDataCache.serviceCodeServiceSpecIdCache;
        String specIds = cache.get(serviceCode);
        if (specIds == null) {
            log.debug("Populating serviceCodeServiceSpecIdCache with data service code [{}]", serviceCode);
            specIds = "";
            Query q = em.createNativeQuery("select service_specification_id, service_code from service_specification where service_code like '%"
                    + serviceCode + "%'");
            List<Object[]> resList = (List<Object[]>) q.getResultList();
            for (Object[] res : resList) {
                Integer svcSpecId = (Integer) res[0];
                String svcCode = (String) res[1];
                specIds = specIds + svcSpecId + ",";
            }
            if (!specIds.isEmpty()) {
                specIds = specIds.substring(0, specIds.length() - 1); // remove trailing ,   123,456, should be substring(0,7)
            }
            cache.put(serviceCode, specIds);
            log.debug("Populated serviceCodeServiceSpecIdCache with data service code [{}] mapping to spec Id's [{}]", serviceCode, specIds);
        }
        return specIds;
    }

    public static ServiceInstance getServiceInstanceForIdentifierAndServiceCode(EntityManager em, ServiceInstanceIdentifier serviceInstanceIdentifier, String serviceCode) {

        if (log.isDebugEnabled()) {
            log.debug("Getting service identifier for ID [{}] Type [{}] Service Code [{}]", new Object[]{serviceInstanceIdentifier.getIdentifier(), serviceInstanceIdentifier.getIdentifierType(), serviceCode});
        }

        if (serviceCode.equals("txtype.sale.purchase") && serviceInstanceIdentifier.getIdentifierType().equals("ACCOUNT")) {
            log.debug("This is a direct account monetary charge. Returning a SI with just an account Id on it");
            ServiceInstance si = new ServiceInstance();
            si.setAccountId(Long.parseLong(serviceInstanceIdentifier.getIdentifier()));
            si.setInfo("");
            si.setProductInstanceId(0);
            si.setServiceInstanceId(0);
            si.setServiceSpecificationId(0);
            si.setProductSpecificationId(0);
            return si;
        }

        // For social media tax - return all data services since they can access social media
        String specIdList;
        if (serviceCode.equals("txtype.socialmedia.tax") || serviceCode.equals("txtype.socialmedia.tax.unlimited")) {
            // 32251 for data services
            specIdList = getServiceSpecIdsForDataServices(em);
        } else {
            specIdList = getServiceSpecIdsForServiceCode(em, serviceCode);
        }

        if (specIdList.isEmpty()) {
            log.warn("Unsupported service code [{}]", serviceCode);
            throw new javax.persistence.NoResultException();
        }
        ServiceInstance si = new ServiceInstance();
        Query q = em.createNativeQuery("select SI.ACCOUNT_ID, SI.SERVICE_INSTANCE_ID, SI.INFO, SI.PRODUCT_INSTANCE_ID, "
                + "SI.SERVICE_SPECIFICATION_ID, PI.PRODUCT_SPECIFICATION_ID, PI.LOGICAL_ID, "
                + "SI.LAST_ACTIVITY_DATETIME, PI.LAST_ACTIVITY_DATETIME, "
                + "SI.LAST_RG_ACTIVITY_DATETIME, PI.LAST_RG_ACTIVITY_DATETIME, PI.LOGICAL_ID from "
                + "service_instance SI, "
                + "service_instance_mapping SIM, "
                + "product_instance PI "
                + "WHERE SIM.IDENTIFIER = ? AND SIM.IDENTIFIER_TYPE = ? "
                + "AND SIM.SERVICE_INSTANCE_ID = SI.SERVICE_INSTANCE_ID "
                + "AND PI.PRODUCT_INSTANCE_ID = SI.PRODUCT_INSTANCE_ID "
                + "AND SI.STATUS = 'AC' and SI.SERVICE_SPECIFICATION_ID in (" + specIdList + ")"); // PCB - Optimised to not join on service_specification
        q.setParameter(1, serviceInstanceIdentifier.getIdentifier());
        q.setParameter(2, serviceInstanceIdentifier.getIdentifierType());
        long start = System.currentTimeMillis();
        Object[] res = (Object[]) q.getSingleResult();
        long millis = System.currentTimeMillis() - start;
        log.debug("getServiceInstanceForIdentifierAndServiceCode query took [{}]ms", millis);
        si.setAccountId((Long) res[0]);
        si.setServiceInstanceId((Integer) res[1]);
        si.setInfo((String) (res[2]));
        si.setProductInstanceId((Integer) res[3]);
        si.setServiceSpecificationId((Integer) res[4]);
        si.setProductSpecificationId((Integer) res[5]);
        si.setProductInstanceLogicalId((int) res[6]);
        si.setLastActivityDate((Date) res[7]);
        si.setProductInstanceLastActivityDate((Date) res[8]);
        si.setLastRGActivityDate((Date) res[9]);
        si.setProductInstanceLastRGActivityDate((Date) res[10]);
        si.setProductInstanceLogicalId((int) res[11]);
        log.debug("Got service instance [{}]", si);
        return si;
    }

    public static List<UnitCreditInstance> getApplicableUnitCreditsBasedOnProductAndServiceOnly(EntityManager em, IAccount acc, int productSpecificationId, int serviceInstanceID) {
        Query q = em.createNativeQuery("SELECT UCI.* FROM "
                + "service_instance SI, "
                + "unit_credit_service_mapping UCSM, "
                + "unit_credit_instance UCI "
                + "WHERE UCI.ACCOUNT_ID = ? "
                + "AND UCI.UNIT_CREDIT_SPECIFICATION_ID = UCSM.UNIT_CREDIT_SPECIFICATION_ID "
                + "AND SI.SERVICE_INSTANCE_ID = ? "
                + "AND SI.SERVICE_SPECIFICATION_ID = UCSM.SERVICE_SPECIFICATION_ID "
                + "AND UCI.EXPIRY_DATE > NOW() "
                + "AND (UCI.END_DATE > NOW() OR UCI.END_DATE IS NULL) "
                + "AND (UCI.PRODUCT_INSTANCE_ID=SI.PRODUCT_INSTANCE_ID OR UCI.PRODUCT_INSTANCE_ID=0) " // If the SI id is 0, then the UCI can apply to any SI
                + "AND (UCSM.PRODUCT_SPECIFICATION_ID = 0 OR UCSM.PRODUCT_SPECIFICATION_ID = ?) "
                + "AND SI.STATUS = 'AC' AND UCI.START_DATE <= now() ORDER BY UCI.PURCHASE_DATE, UCI.UNIT_CREDIT_INSTANCE_ID", UnitCreditInstance.class);
        q.setParameter(1, acc.getAccountId());
        q.setParameter(2, serviceInstanceID);
        q.setParameter(3, productSpecificationId);
        List<UnitCreditInstance> uciList = q.getResultList();
        log.debug("Unit credit query returned [{}] results", uciList.size());
        return uciList;
    }

    public static List<UnitCreditInstance> getActiveUnitCreditsBasedOnSaleIdRow(EntityManager em, int saleRowId) {
        Query q = em.createNativeQuery("SELECT UCI.* FROM "
                + "unit_credit_instance UCI "
                + "WHERE UCI.SALE_ROW_ID = ? "
                + "AND UCI.EXPIRY_DATE > NOW() "
                + "AND UCI.UNITS_REMAINING > 0 ", UnitCreditInstance.class);

        q.setParameter(1, saleRowId);
        List<UnitCreditInstance> uciList = q.getResultList();
        log.debug("Unit credit query returned [{}] results", uciList.size());
        return uciList;
    }

    public static List<UnitCreditInstance> getOldestUnitCreditsUsingSaleRowIdAndType(EntityManager em, int saleRowId, String wrapperClasses) {
        Query q = em.createNativeQuery("select UCI.* from unit_credit_instance UCI join unit_credit_specification UCS "
                + " on (UCI.unit_credit_specification_id = UCS.unit_credit_specification_id "
                + "        and UCS.wrapper_class in (" + wrapperClasses + ") "
                + "        and UCI.SALE_ROW_ID = ?) order by UCI.PURCHASE_DATE desc limit 1;", UnitCreditInstance.class);

        q.setParameter(1, saleRowId);
        List<UnitCreditInstance> uciList = q.getResultList();
        log.debug("Unit credit query returned [{}] results", uciList.size());
        return uciList;
    }

    public static List<UnitCreditInstance> getAccountsActiveUnitCredits(EntityManager em, IAccount acc) {
        Query q = em.createNativeQuery("SELECT UCI.* FROM "
                + "unit_credit_instance UCI "
                + "WHERE UCI.ACCOUNT_ID = ? "
                + "AND UCI.EXPIRY_DATE > NOW() ", UnitCreditInstance.class);
        q.setParameter(1, acc.getAccountId());
        List<UnitCreditInstance> uciList = q.getResultList();
        return uciList;
    }

    public static List<UnitCreditInstance> getCustomersActiveUnitCredits(EntityManager em, int piId) {
        Query q = em.createNativeQuery("SELECT UCI.* FROM "
                + "product_instance PI "
                + "join product_instance CUST_PI on CUST_PI.CUSTOMER_PROFILE_ID = PI.CUSTOMER_PROFILE_ID "
                + "join unit_credit_instance UCI on UCI.PRODUCT_INSTANCE_ID = CUST_PI.PRODUCT_INSTANCE_ID "
                + "WHERE PI.PRODUCT_INSTANCE_ID = ? "
                + "AND UCI.EXPIRY_DATE > NOW()", UnitCreditInstance.class);
        q.setParameter(1, piId);
        List<UnitCreditInstance> uciList = q.getResultList();
        return uciList;
    }

    public static Date getMaxExpiryDateByUnitCreditSpecIds(EntityManager em, long accId, String specIds) {
        Query q = em.createNativeQuery("SELECT max(EXPIRY_DATE) FROM unit_credit_instance I"
                + " where I.ACCOUNT_ID = ? and I.UNIT_CREDIT_SPECIFICATION_ID  in (" + specIds + ") and EXPIRY_DATE > now()");
        q.setParameter(1, accId);
        // q.setParameter(2, specIds);

        Date ret = null;
        try {
            ret = (Date) q.getSingleResult();
        } catch (Exception ex) {
            return null;
        }
        return ret;
    }

    public static Date getMaxEndDateByUnitCreditSpecIds(EntityManager em, long accId, String specIds) {
        Query q = em.createNativeQuery("SELECT max(END_DATE) FROM unit_credit_instance I"
                + " where I.ACCOUNT_ID = ? and I.UNIT_CREDIT_SPECIFICATION_ID  in (" + specIds + ") and END_DATE > now()");
        q.setParameter(1, accId);
        // q.setParameter(2, specIds);
        Date ret = null;

        try {
            ret = (Date) q.getSingleResult();
        } catch (Exception ex) {
            return null;
        }

        return ret;
    }

    public static Date getAccountsLastDataOrVoiceBundleExpiry(EntityManager em, IAccount acc) {
        String key = "LAST_DATA_EXP_" + acc.getAccountId();
        Date cachedDate = CacheHelper.getFromLocalCache(key, Date.class);
        if (cachedDate != null) {
            return cachedDate;
        }

        Query q = em.createNativeQuery("select max(UCI.expiry_date) from unit_credit_instance UCI "
                + "join unit_credit_specification UCS on UCI.UNIT_CREDIT_SPECIFICATION_ID = UCS.UNIT_CREDIT_SPECIFICATION_ID "
                + "where UCI.ACCOUNT_ID=? and UCS.UNIT_TYPE in ('Byte','Second');");
        q.setParameter(1, acc.getAccountId());
        Date ret = null;
        try {
            ret = (Date) q.getSingleResult();
        } catch (Exception e) {
            log.debug("No data unit credits");
        }
        if (ret == null) {
            ret = new Date(0);
        }
        log.debug("Max date is [{}]", ret);
        CacheHelper.putInLocalCache(key, ret, 2);
        return ret;
    }

    public static Date getAccountsLastDataBundleExpiry(EntityManager em, IAccount acc) {
        String key = "LAST_DATA_EXP_" + acc.getAccountId();
        Date cachedDate = CacheHelper.getFromLocalCache(key, Date.class);
        if (cachedDate != null) {
            return cachedDate;
        }

        Query q = em.createNativeQuery("select max(UCI.expiry_date) from unit_credit_instance UCI "
                + "join unit_credit_specification UCS on UCI.UNIT_CREDIT_SPECIFICATION_ID = UCS.UNIT_CREDIT_SPECIFICATION_ID "
                + "where UCI.ACCOUNT_ID=? and UCS.UNIT_TYPE in ('Byte');");
        q.setParameter(1, acc.getAccountId());
        Date ret = null;
        try {
            ret = (Date) q.getSingleResult();
        } catch (Exception e) {
            log.debug("No data unit credits");
        }
        if (ret == null) {
            ret = new Date(0);
        }
        log.debug("Max date is [{}]", ret);
        CacheHelper.putInLocalCache(key, ret, 2);
        return ret;
    }

    public static UnitCreditInstance getLockedUnitCreditInstance(EntityManager em, int unitCreditInstanceId) {
        return JPAUtils.findAndThrowENFE(em, UnitCreditInstance.class, unitCreditInstanceId, LockModeType.PESSIMISTIC_WRITE);
    }

    public static UnitCreditInstance getUnitCreditInstance(EntityManager em, int unitCreditInstanceId) {
        return JPAUtils.findAndThrowENFE(em, UnitCreditInstance.class, unitCreditInstanceId);
    }

    public static List<ServiceInstance> getServiceInstancesForAccount(EntityManager em, long accId) {
        Query q = em.createNativeQuery("SELECT service_instance_id, account_id, product_instance_id, service_specification_id,info FROM service_instance where ACCOUNT_ID=? and STATUS != 'DE'");
        q.setParameter(1, accId);
        List<Object[]> sis = q.getResultList();
        List<ServiceInstance> ret = new ArrayList<>();
        for (Object[] arr : sis) {
            ServiceInstance si = new ServiceInstance();
            si.setServiceInstanceId((Integer) arr[0]);
            si.setAccountId((Long) arr[1]);
            si.setProductInstanceId((Integer) arr[2]);
            si.setServiceSpecificationId((Integer) arr[3]);
            si.setInfo((String) arr[4]);
            ret.add(si);
        }
        return ret;
    }

    public static List<ServiceInstance> getServiceInstancesForProduct(EntityManager em, long pId) {
        Query q = em.createNativeQuery("SELECT service_instance_id, account_id, product_instance_id, service_specification_id,info FROM service_instance where PRODUCT_INSTANCE_ID=? and STATUS != 'DE'");
        q.setParameter(1, pId);
        List<Object[]> sis = q.getResultList();
        List<ServiceInstance> ret = new ArrayList<>();
        for (Object[] arr : sis) {
            ServiceInstance si = new ServiceInstance();
            si.setServiceInstanceId((Integer) arr[0]);
            si.setAccountId((Long) arr[1]);
            si.setProductInstanceId((Integer) arr[2]);
            si.setServiceSpecificationId((Integer) arr[3]);
            si.setInfo((String) arr[4]);
            ret.add(si);
        }
        return ret;
    }

    public static long getCountOfProductInstancesOnAccountThatCouldUseUnitCreditSpec(EntityManager em, long accId, int ucSpecId) {
        Query q = em.createNativeQuery("select count(distinct SI.PRODUCT_INSTANCE_ID) from "
                + "service_instance SI "
                + "join product_instance PI on PI.product_instance_id = SI.product_instance_id "
                + "join unit_credit_service_mapping M on (M.SERVICE_SPECIFICATION_ID = SI.SERVICE_SPECIFICATION_ID AND "
                + "(M.PRODUCT_SPECIFICATION_ID=0 OR M.PRODUCT_SPECIFICATION_ID=PI.PRODUCT_SPECIFICATION_ID)) "
                + "where SI.ACCOUNT_ID=? "
                + "and M.UNIT_CREDIT_SPECIFICATION_ID=? and SI.STATUS != 'DE' and PI.STATUS != 'DE'");
        q.setParameter(1, accId);
        q.setParameter(2, ucSpecId);
        return (Long) q.getSingleResult();
    }

    public static ServiceInstance getDataServiceInstanceForProductInstance(EntityManager em, int productInstanceId) {
        Query q = em.createNativeQuery("select SI_DATA.SERVICE_INSTANCE_ID, SI_DATA.ACCOUNT_ID, SI_DATA.PRODUCT_INSTANCE_ID, SI_DATA.service_specification_id, SI_DATA.INFO from service_instance SI_DATA "
                + "join service_specification S on (SI_DATA.SERVICE_SPECIFICATION_ID = S.SERVICE_SPECIFICATION_ID) "
                + "where SI_DATA.PRODUCT_INSTANCE_ID = ? AND S.SERVICE_CODE like '%32251%' and SI_DATA.STATUS = 'AC' limit 1");
        q.setParameter(1, productInstanceId);
        List<Object[]> sis = q.getResultList();
        Object[] arr = sis.get(0);
        ServiceInstance si = new ServiceInstance();
        si.setServiceInstanceId((Integer) arr[0]);
        si.setAccountId((Long) arr[1]);
        si.setProductInstanceId((Integer) arr[2]);
        si.setServiceSpecificationId((Integer) arr[3]);
        si.setInfo((String) arr[4]);
        return si;
    }

    public static ServiceInstance getSIMServiceInstanceForProductInstance(EntityManager em, int productInstanceId) throws Exception {
        Query q = em.createNativeQuery("select SI_SIM.SERVICE_INSTANCE_ID, SI_SIM.ACCOUNT_ID, SI_SIM.PRODUCT_INSTANCE_ID, "
                + "SI_SIM.SERVICE_SPECIFICATION_ID from service_instance SI_SIM "
                + "where SI_SIM.PRODUCT_INSTANCE_ID = ? AND SI_SIM.SERVICE_SPECIFICATION_ID=1 AND SI_SIM.STATUS = 'AC' limit 1");
        q.setParameter(1, productInstanceId);
        List<Object[]> sis = q.getResultList();
        if (sis.isEmpty()) {
            throw new Exception("Product instance does not have an active SIM service -- " + productInstanceId);
        }
        Object[] arr = sis.get(0);
        ServiceInstance si = new ServiceInstance();
        si.setServiceInstanceId((Integer) arr[0]);
        si.setAccountId((Long) arr[1]);
        si.setProductInstanceId((Integer) arr[2]);
        si.setServiceSpecificationId((Integer) arr[3]);
        return si;
    }

    public static ServiceInstance getProductInstanceOnEmptyServiceInstance(EntityManager em, int productInstanceId) throws Exception {
        ServiceInstance si = new ServiceInstance();
        Query q = em.createNativeQuery("select PI.PRODUCT_INSTANCE_ID, "
                + "PI.PRODUCT_SPECIFICATION_ID, PI.LOGICAL_ID, "
                + "PI.LAST_ACTIVITY_DATETIME, "
                + "PI.LAST_RG_ACTIVITY_DATETIME, PI.LOGICAL_ID from "
                + "product_instance PI "
                + "WHERE PI.PRODUCT_INSTANCE_ID = ? ");
        q.setParameter(1, productInstanceId);
        Object[] res = (Object[]) q.getSingleResult();
        si.setProductInstanceId((Integer) res[0]);
        si.setProductSpecificationId((Integer) res[1]);
        si.setProductInstanceLogicalId((int) res[2]);
        si.setProductInstanceLastActivityDate((Date) res[3]);
        si.setProductInstanceLastRGActivityDate((Date) res[4]);
        si.setProductInstanceLogicalId((int) res[5]);
        log.debug("Got service instance [{}]", si);
        return si;
    }

    public static int getLastActiveOrTempDeactiveProductInstanceIdForAccountAndUnitCreditSpec(EntityManager em, long accId, int ucSpecId) {
        Query q = em.createNativeQuery("SELECT SI.product_instance_id FROM "
                + "service_instance SI, product_instance PI, unit_credit_service_mapping UCSM "
                + "where SI.ACCOUNT_ID=? and UCSM.UNIT_CREDIT_SPECIFICATION_ID=? AND "
                + "(UCSM.PRODUCT_SPECIFICATION_ID=0 OR UCSM.PRODUCT_SPECIFICATION_ID=PI.PRODUCT_SPECIFICATION_ID) "
                + "AND SI.PRODUCT_INSTANCE_ID=PI.PRODUCT_INSTANCE_ID "
                + "AND UCSM.SERVICE_SPECIFICATION_ID=SI.SERVICE_SPECIFICATION_ID and SI.STATUS in ('AC','TD') order by SI.STATUS ASC, SI.CREATED_DATETIME DESC LIMIT 1");
        q.setParameter(1, accId);
        q.setParameter(2, ucSpecId);
        try {
            return (Integer) q.getSingleResult();
        } catch (javax.persistence.NoResultException nre) {
            return -1;
        }
    }

    public static boolean doesSIHaveSvcSpec(EntityManager em, int serviceInstanceID, int svcSpecId) {
        return (getSISvcSpecId(em, serviceInstanceID) == svcSpecId);
    }

    public static String getSIInfo(EntityManager em, int serviceInstanceID) {
        Query q = em.createNativeQuery("SELECT info FROM service_instance where service_instance_id=?");
        q.setParameter(1, serviceInstanceID);
        return (String) q.getSingleResult();
    }

    public static void setSIInfo(EntityManager em, int serviceInstanceID, String siInfo) {
        Query q = em.createNativeQuery("update service_instance set info=? where service_instance_id=?");
        q.setParameter(1, siInfo);
        q.setParameter(2, serviceInstanceID);

        q.executeUpdate();

    }

    public static int getSISvcSpecId(EntityManager em, int serviceInstanceID) {
        Query q = em.createNativeQuery("SELECT service_specification_id FROM service_instance where service_instance_id=?");
        q.setParameter(1, serviceInstanceID);
        return (Integer) q.getSingleResult();
    }

    /**
     * INTERCONNECT FUNCTIONALITY
     */
    public static InterconnectHistory createInterconnectHistory(EntityManager em) {

        InterconnectHistory ih = new InterconnectHistory();
        ih.setRunStartDatetime(new Date());
        ih.setIsRunning('Y');
        em.persist(ih);
        em.flush(); // Would get duplicate key if already running
        em.refresh(ih);
        return ih;
    }

    public static void updateInterconnectHistoryStartOfRun(EntityManager em, InterconnectHistory ih, Date eventsFrom, Date eventsTo) {
        ih.setEventStartDatetime(eventsFrom);
        ih.setEventEndDatetime(eventsTo);
        em.persist(ih);
    }

    public static void updateInterconnectHistoryEndOfRun(EntityManager em, InterconnectHistory ih, int processed) {
        ih.setEventsProcessed(processed);
        ih.setIsRunning(null);
        ih.setRunEndDatetime(new Date());
        em.persist(ih);
        em.flush();
    }

    public static Date getLastProcessedEventDate(EntityManager em) {
        Query q = em.createNativeQuery("SELECT ifnull(max(event_end_datetime),now() - interval 1 year) from interconnect_history");
        return (Date) q.getSingleResult();
    }

    public static InterconnectHistory getRunningInterconnectHistory(EntityManager em) {
        Query q = em.createNativeQuery("SELECT * from interconnect_history where is_running='Y'", InterconnectHistory.class);
        return (InterconnectHistory) q.getSingleResult();
    }

    public static List<AccountHistory> getEventsToRate(EntityManager em, Date eventsEndingFromIncl, Date eventsEndingToExcl) {
        // Use start date for its index. I assume no calls will last more than 12 hours. Play off between performance and skipping records
        Query q = em.createNativeQuery("select * from account_history where "
                + "start_date > ? - interval ? hour and end_date >= ? and start_date < ? and end_date < ? and destination != '' and INCOMING_TRUNK != '' "
                + "and OUTGOING_TRUNK != '' and TOTAL_UNITS != 0", AccountHistory.class);
        q.setParameter(1, eventsEndingFromIncl);
        q.setParameter(2, BaseUtils.getIntProperty("env.bm.interconnect.max.session.hours", 12));
        q.setParameter(3, eventsEndingFromIncl);
        q.setParameter(4, eventsEndingToExcl);
        q.setParameter(5, eventsEndingToExcl);
        return q.getResultList();
    }

    public static List<AccountHistory> getEventsToReRate(EntityManager em) {
        // Use start date for its index. I assume no calls will last more than 12 hours. Play off between performance and skipping records
        Query q = em.createNativeQuery("select H.* from account_history H join interconnect_event E on E.ACCOUNT_HISTORY_ID = H.ID where E.STATUS='R' LIMIT 10000", AccountHistory.class);
        return q.getResultList();
    }

    public static boolean isInterconnectEventLocked(EntityManager em, long accountHistId) {

        Query q = em.createNativeQuery("select STATUS from interconnect_event where ACCOUNT_HISTORY_ID = ?");
        q.setParameter(1, BigInteger.valueOf(accountHistId));
        try {
            String status = (String) q.getSingleResult();
            return status.equals("L");
        } catch (Exception e) {
            return false;
        }
    }

    public static void createInterconnectEvent(EntityManager em, long accountHistId, BigDecimal fromInterconnectCents, BigDecimal toInterconnectCents, int interconnectHistId,
            int units, String fromCur, String toCur, Date startDate, Date endDate) {
        Query q = em.createNativeQuery("REPLACE INTO interconnect_event (RATED_DATETIME, "
                + "FROM_INTERCONNECT_CENTS, TO_INTERCONNECT_CENTS, UNITS, ACCOUNT_HISTORY_ID, INTERCONNECT_HISTORY_ID, "
                + "FROM_INTERCONNECT_CURRENCY, TO_INTERCONNECT_CURRENCY, STATUS, EVENT_START_DATETIME, EVENT_END_DATETIME) "
                + "VALUES "
                + "(now(),?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        q.setParameter(1, fromInterconnectCents);
        q.setParameter(2, toInterconnectCents);
        q.setParameter(3, units);
        q.setParameter(4, BigInteger.valueOf(accountHistId));
        q.setParameter(5, interconnectHistId);
        q.setParameter(6, fromCur);
        q.setParameter(7, toCur);
        q.setParameter(8, null);
        q.setParameter(9, startDate);
        q.setParameter(10, endDate);
        q.executeUpdate();
    }

    public static List<ServiceRate> getInterconnectRates(EntityManager em) {
        Query q = em.createNativeQuery("select * from service_rate", ServiceRate.class);
        return q.getResultList();
    }

    public static void closeFailedInterconnectHistory(EntityManager em, String err) {
        Query q = em.createNativeQuery("update interconnect_history set run_end_datetime=now(), is_running=null, events_processed=-1, event_end_datetime=null, event_start_datetime=null, ERROR_DETAIL=? where is_running='Y'");
        q.setParameter(1, err);
        q.executeUpdate();
    }

    public static boolean doesOrganisationHaveRequiredUnitCredit(EntityManager em, Integer unitCreditInstanceId, int requiredUnitCreditSpecId) {
        Query q = em.createNativeQuery("SELECT count(OTHER_UCI.UNIT_CREDIT_INSTANCE_ID) FROM unit_credit_instance UCI "
                + "JOIN product_instance PI ON PI.PRODUCT_INSTANCE_ID = UCI.PRODUCT_INSTANCE_ID "
                + "JOIN product_instance OTHER_PI ON OTHER_PI.ORGANISATION_ID = PI.ORGANISATION_ID "
                + "JOIN unit_credit_instance OTHER_UCI ON OTHER_UCI.PRODUCT_INSTANCE_ID = OTHER_PI.PRODUCT_INSTANCE_ID "
                + "JOIN unit_credit_specification OTHER_UCS ON OTHER_UCS.UNIT_CREDIT_SPECIFICATION_ID = OTHER_UCI.UNIT_CREDIT_SPECIFICATION_ID "
                + "WHERE UCI.UNIT_CREDIT_INSTANCE_ID = ? AND OTHER_UCS.UNIT_CREDIT_SPECIFICATION_ID = ?");
        q.setParameter(1, unitCreditInstanceId);
        q.setParameter(2, requiredUnitCreditSpecId);
        Long updates = (Long) q.getSingleResult();
        return updates > 0;
    }

    private static final int MATH_PRECISION_LARGE_NUM = 10; // The minimum number of decimal places for division for numbers >= 1
    private static final int MATH_PRECISION_SMALL_NUM = 20; // The minimum number of decimal places for division for numbers < 1

    /**
     * Lossless division ensuring that we keep the max decimal places of the top
     * and bottom and never less than MATH_PRECISION
     *
     * @param top
     * @param bottom
     * @return
     */
    public static BigDecimal divide(BigDecimal top, BigDecimal bottom) {
        if (top.compareTo(BigDecimal.ZERO) == 0) {
            // Shortcut
            return BigDecimal.ZERO;
        }
        int scale = Math.max(MATH_PRECISION_LARGE_NUM, Math.max(top.scale(), bottom.scale()));
        BigDecimal result = top.divide(bottom, scale, RoundingMode.HALF_EVEN);
        if (result.abs().compareTo(BigDecimal.ONE) < 0 && scale < MATH_PRECISION_SMALL_NUM) {
            // if dealing with small numbers then increase our precision
            result = top.divide(bottom, MATH_PRECISION_SMALL_NUM, RoundingMode.HALF_EVEN);
        }
        if (log.isDebugEnabled()) {
            log.debug("Dividing [{}] by [{}] gives [{}]", new Object[]{top, bottom, result});
        }
        return result;
    }

    public static double getAccountsOffNetSecondsRemaining(EntityManager em, long accId) {
        log.debug("Getting voice balance on account [{}]", accId);
        Query q = em.createNativeQuery("select sum(UCI.UNITS_REMAINING) from unit_credit_instance UCI "
                + "JOIN unit_credit_specification UCS ON (UCI.unit_credit_specification_id=UCS.unit_credit_specification_id) "
                + "where UCI.end_date>now() and UNIT_TYPE='Second' and UCS.configuration not like '%Staff=true%' "
                + "and configuration like '%WhiteListRatingGroupRegex=[2,3][0-9]{3}%' and UCI.ACCOUNT_ID = ?");
        q.setParameter(1, accId);
        List<BigDecimal> resList = q.getResultList();
        if (resList.isEmpty()) {
            return 0;
        }
        if (resList.get(0) != null) {
            double seconds = resList.get(0).doubleValue();
            log.debug("Account has [{}] voice seconds remaining", seconds);
            return seconds;
        } else {
            return 0;
        }
    }

    public static List<UnitCreditInstance> getActiveMonthlyBundles(EntityManager em, long accId) {
        Query q = em.createNativeQuery("Select * from unit_credit_instance where account_id = ? and expiry_date > now()", UnitCreditInstance.class);
        q.setParameter(1, accId);
        return q.getResultList();
    }

    public static double getAccountsRemainingDataFromDataBundle(EntityManager em, long accId) {
        log.debug("Getting voice balance on account [{}]", accId);
        Query q = em.createNativeQuery("select sum(UCI.UNITS_REMAINING) from unit_credit_instance UCI "
                + "JOIN unit_credit_specification UCS ON (UCI.unit_credit_specification_id=UCS.unit_credit_specification_id) "
                + "where UCI.end_date>now() and ACCOUNT_ID = ? and UCS.UNIT_TYPE='Byte' and UCS.configuration not like '%Staff=true%' "
                + "order by UCI.PURCHASE_DATE");
        q.setParameter(1, accId);
        List<BigDecimal> resList = q.getResultList();
        if (resList.isEmpty()) {
            return 0;
        }
        if (resList.get(0) != null) {
            double seconds = resList.get(0).doubleValue();
            log.debug("Account has [{}] voice seconds remaining", seconds);
            return seconds;
        } else {
            return 0;
        }
    }
    
    public static void setScheduledAccountHistory(EntityManager em, long accountId, String emailTo, String frequency, long createdBy) throws Exception {
        Query q = em.createNativeQuery("DELETE FROM SmileDB.scheduled_account_history WHERE ACCOUNT_ID = ? and EMAIL_TO=?");
        q.setParameter(1, accountId);
        q.setParameter(2, emailTo);
        q.executeUpdate();
        
        Query q2 = em.createNativeQuery("INSERT INTO SmileDB.scheduled_account_history(account_id,frequency, email_to,created_by_profile_id)"
                + " VALUES(?,?,?,?)");
        q2.setParameter(1, accountId);
        q2.setParameter(2, frequency);
        q2.setParameter(3, emailTo);
        q2.setParameter(4, createdBy);
        q2.executeUpdate();
        
        em.flush();
    }

}
