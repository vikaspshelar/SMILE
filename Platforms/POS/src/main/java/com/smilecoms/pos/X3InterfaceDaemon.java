/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos;

import com.adonix.www.WSS.CAdxResultXml;
import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.hazelcast.HazelcastHelper;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;
import com.smilecoms.commons.platform.PlatformEventManager;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.pos.db.model.CashIn;
import com.smilecoms.pos.db.model.CashInRow;
import com.smilecoms.pos.db.model.Sale;
import com.smilecoms.pos.db.model.SaleReturn;
import com.smilecoms.pos.db.model.SaleReturnRow;
import com.smilecoms.pos.db.model.SaleRow;
import com.smilecoms.pos.db.model.X3OfflineSubitems;
import com.smilecoms.pos.db.model.X3RequestState;
import com.smilecoms.pos.db.model.X3TransactionState;
import com.smilecoms.pos.db.op.DAO;
import com.smilecoms.xml.schema.pos.InventoryItem;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
@Singleton
@Startup
@Local({BaseListener.class})
public class X3InterfaceDaemon implements BaseListener {

    private static final Logger log = LoggerFactory.getLogger(X3InterfaceDaemon.class);
    private EntityManagerFactory emf = null;
    private static final String CHECK_FOR_PAYMENTS = "cfp";
    private static final String SEND_TRANSACTIONS = "st";
    private static final String GET_OFFLINE_SNAPSHOT = "gos";
    private static final String CLEAR_ERRORS_FOR_RETRY = "ce";
    private static int BATCH_SIZE = 10;
    public static final String X3_TRANSACTION_TYPE_CASH_SALE = "Cash Sale";
    public static final String X3_TRANSACTION_TYPE_AIRTIME_SALE = "Airtime Sale";
    public static final String X3_TRANSACTION_TYPE_CARD_PAYMENT_SALE = "Card Payment Sale";
    public static final String X3_TRANSACTION_TYPE_CLEARING_BUREAU_SALE = "Clearing Bureau Sale";
    public static final String X3_TRANSACTION_TYPE_CREDIT_NOTE_SALE = "Credit Note Sale";
    public static final String X3_TRANSACTION_TYPE_CREDIT_ACCOUNT_SALE = "Credit Account Sale";
    public static final String X3_TRANSACTION_TYPE_SALE_PENDING_PAYMENT = "Sale Pending Payment";
    public static final String X3_TRANSACTION_TYPE_QUOTE_WITH_RESERVATION = "Quote With Reservation";
    public static final String X3_TRANSACTION_TYPE_STOCK_LOAN = "Stock Loan";
    public static final String X3_TRANSACTION_TYPE_STOCK_LOAN_COMPLETION = "Stock Loan Completion";
    public static final String X3_TRANSACTION_TYPE_PAYMENT = "Payment";
    public static final String X3_TRANSACTION_TYPE_REVERSAL = "Reversal";
    public static final String X3_TRANSACTION_TYPE_RETURN = "Return";
    public static final String X3_TRANSACTION_TYPE_LINE_REPLACEMENT = "Line Replacement";
    public static final String X3_TRANSACTION_TYPE_LINE_RETURN = "Line Return";
    public static final String X3_TRANSACTION_TYPE_GL_ENTRY = "GLEntry";
    public static final String X3_TRANSACTION_TYPE_CASHIN = "CashIn";
    public static final String X3_TRANSACTION_TYPE_PSC = "PSCTGLEntry";
    public static final String X3_TRANSACTION_TYPE_CARD_INTEGRATION_GL = "Card Integration GL";
    public static final String X3_TRANSACTION_TYPE_CREDIT_FACILITY_SALE = "Credit Facility Sale";
    public static final String X3_TRANSACTION_TYPE_CARD_INTEGRATION_SALE = "Card Integration Sale";
    public static final String X3_TRANSACTION_STATUS_PENDING = "PE";
    public static final String X3_TRANSACTION_STATUS_FINISHED = "FI";
    public static final String X3_TRANSACTION_STATUS_IN_PROGRESS = "IP";
    public static final String X3_TRANSACTION_STATUS_REPEAT = "RE";
    public static final String X3_TRANSACTION_STATUS_ERROR = "ER";
    public static final String TABLE_SALE = "sale";
    public static final String TABLE_RETURN_REPLACEMENT = "return_replacement";
    public static final String TABLE_CASHIN = "cash_in";
    public static final String TABLE_PSC_TRANSACTION = "account_history";
    public static final String TABLE_SALE_RETURN = "sale_return";
    private static ScheduledFuture runner1 = null;
    private static ScheduledFuture runner2 = null;
    private static ScheduledFuture runner3 = null;
    private static ScheduledFuture runner4 = null;

    private void trigger(String callbackParam) {
        try {
            log.debug("X3InterfaceDaemon triggered with callback param [{}]", callbackParam);
            if (!X3Helper.ready()) {
                log.warn("X3 Helper is not ready to process requests yet. Returning");
                return;
            }
            if (!BaseUtils.getBooleanProperty("env.pos.x3daemon.mustrun")) {
                log.warn("X3 Interface daemon is set not to run");
                return;
            }
            if (BaseUtils.getBooleanProperty("env.pos.x3.offline", false)) {
                log.warn("X3 Interface is in offline mode");
                return;
            }

            LazyX3Connection lazyConn = null;
            try {

                BATCH_SIZE = BaseUtils.getIntProperty("env.pos.x3.daemon.batch.size", 200);
                lazyConn = new LazyX3Connection(true);
                if (lazyConn.isConnectionDown()) {
                    log.warn("Not going to do interface daemon processing as X3 connection is down");
                    return;
                }
                if (callbackParam.equals(CHECK_FOR_PAYMENTS)) {
                    checkForPayments(lazyConn);
                } else if (callbackParam.equals(GET_OFFLINE_SNAPSHOT) && BaseUtils.getBooleanProperty("env.pos.x3.take.offline.snapshots", true)) {
                    populateOfflineSnapshot(lazyConn);
                  //  updateKitsDataFromSnapshot(lazyConn);
                } else if (callbackParam.equals(CLEAR_ERRORS_FOR_RETRY)) {
                    clearErrorsForRetry(lazyConn);
                } else if (callbackParam.equals(SEND_TRANSACTIONS)) {

                    Set<String> allowedTxTypes = BaseUtils.getPropertyAsSet("env.pos.x3.daemon.sendtypes");

                    // Send cash sales
                    if (allowedTxTypes.contains("sendCashSales")) {
                        int salesSent;
                        do {
                            salesSent = sendCashSales(lazyConn);
                            log.debug("Sent [{}] sales in the loop", salesSent);
                        } while (salesSent > 5 && BaseUtils.getBooleanProperty("env.pos.x3.offline", false) == false && BaseUtils.getBooleanProperty("env.pos.x3daemon.mustrun", true) == true);
                    }

                    if (allowedTxTypes.contains("cancelExpiredSales")) {
                        cancelExpiredSales(lazyConn);
                    }

                    // Send airtime sales
                    if (allowedTxTypes.contains("sendAirtimeSales")) {
                        int salesSent;
                        do {
                            salesSent = sendAirtimeSales(lazyConn);
                            log.debug("Sent [{}] sales in the loop", salesSent);
                        } while (salesSent > 5 && BaseUtils.getBooleanProperty("env.pos.x3.offline", false) == false && BaseUtils.getBooleanProperty("env.pos.x3daemon.mustrun", true) == true);

                    }

                    // Send card payment sales
                    if (allowedTxTypes.contains("sendCardPaymentSales")) {
                        int salesSent;
                        do {
                            salesSent = sendCardPaymentSales(lazyConn);
                            log.debug("Sent [{}] sales in the loop", salesSent);
                        } while (salesSent > 5 && BaseUtils.getBooleanProperty("env.pos.x3.offline", false) == false && BaseUtils.getBooleanProperty("env.pos.x3daemon.mustrun", true) == true);
                    }

                    // Send credit note sales
                    if (allowedTxTypes.contains("sendCreditNoteSales")) {
                        sendCreditNoteSales(lazyConn);
                    }

                    // Send credit sales
                    if (allowedTxTypes.contains("sendCreditAccountSales")) {
                        int salesSent;
                        do {
                            salesSent = sendCreditAccountSales(lazyConn);
                            log.debug("Sent [{}] sales in the loop", salesSent);
                        } while (salesSent > 5 && BaseUtils.getBooleanProperty("env.pos.x3.offline", false) == false && BaseUtils.getBooleanProperty("env.pos.x3daemon.mustrun", true) == true);
                    }

                    // Send pending payment sales
                    if (allowedTxTypes.contains("sendPendingPaymentSales")) {
                        sendPendingPaymentSales(lazyConn);
                    }

                    // Send quotes requiring stock reservation
                    if (allowedTxTypes.contains("sendQuotesWithStockReservation")) {
                        sendQuotesWithStockReservation(lazyConn);
                    }

                    // Send stock loans
                    if (allowedTxTypes.contains("sendStockLoans")) {
                        sendStockLoans(lazyConn);
                    }

                    // Send stock loan completions
                    if (allowedTxTypes.contains("sendStockLoanCompletions")) {
                        sendStockLoanCompletions(lazyConn);
                    }

                    // Send payment matches
                    if (allowedTxTypes.contains("sendPayments")) {
                        sendPayments(lazyConn);
                    }

                    // Send sale reversals
                    if (allowedTxTypes.contains("sendSaleReversals")) {
                        sendSaleReversals(lazyConn);
                    }

                    // Send returns
                    if (allowedTxTypes.contains("sendPaidReturns")) {
                        sendPaidReturns(lazyConn);
                    }

                    // Send returns
                    if (allowedTxTypes.contains("sendLineReturns")) {
                        sendLineReturns(lazyConn);
                    }

                    if (allowedTxTypes.contains("sendLineReplacements")) {
                        sendLineReplacements(lazyConn);
                    }

                    // Send general ledger entries
                    if (allowedTxTypes.contains("sendGLEntries")) {
                        sendGLEntries(lazyConn);
                    }

                    // Send cash-in entries
                    if (allowedTxTypes.contains("sendCashInEntries")) {
                        sendCashInEntries(lazyConn);
                    }
                    //Send Partner Sales Commission transaction entries
                    if (allowedTxTypes.contains("sendPSCTransactionEntries")) {
                        sendPSCTEntries(lazyConn);
                    }

                    // Send Clearing Bureau Sales
                    if (allowedTxTypes.contains("sendClearingBureauSales")) {
                        int salesSent;
                        do {
                            salesSent = sendClearingBureauSales(lazyConn);
                            log.debug("Sent [{}] sales in the loop", salesSent);
                        } while (salesSent > 5 && BaseUtils.getBooleanProperty("env.pos.x3.offline", false) == false && BaseUtils.getBooleanProperty("env.pos.x3daemon.mustrun", true) == true);
                    }

                    // Send Credit Facility Sales
                    if (allowedTxTypes.contains("sendCreditFacilitySales")) {
                        int salesSent;
                        do {
                            salesSent = sendCreditFacilityPaymentSales(lazyConn);
                            log.debug("Sent [{}] sales in the loop", salesSent);
                        } while (salesSent > 5 && BaseUtils.getBooleanProperty("env.pos.x3.offline", false) == false && BaseUtils.getBooleanProperty("env.pos.x3daemon.mustrun", true) == true);
                    }

//                // Send general ledger entries for card integration
//                if (allowedTxTypes.contains("SendCardIntegrationGLEntries")) {
//                    sendCardIntegrationGLEntries(lazyConn);
//                }
                }
            } catch (Exception e) {
                log.warn("Error in X3 Daemon", e);
            } finally {
                if (lazyConn != null) {
                    try {
                        lazyConn.close();
                    } catch (Exception e) {
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error: ", e);
        }
    }

    @Override
    public void propsAreReadyTrigger() {
        X3Helper.initialise();
        boolean devMode = false;
        try {
            devMode = BaseUtils.getBooleanProperty("env.development.mode");
        } catch (Exception e) {
            log.debug("Error checking if we are in dev mode. Will assume we are not [{}]", e.toString());
        }
        if (devMode) {
            runner1 = Async.scheduleAtFixedRate(new SmileBaseRunnable("POS.IntfDaemon.CHECK_FOR_PAYMENTS") {
                @Override
                public void run() {
                    trigger(CHECK_FOR_PAYMENTS);
                }
            }, 20000, 2 * 60000, 3 * 60000);
            runner2 = Async.scheduleAtFixedRate(new SmileBaseRunnable("POS.IntfDaemon.SEND_TRANSACTIONS") {
                @Override
                public void run() {
                    trigger(SEND_TRANSACTIONS);
                }
            }, 20000, 20000, 30000);

            runner3 = Async.scheduleAtFixedRate(new SmileBaseRunnable("POS.IntfDaemon.CLEAR_ERRORS_FOR_RETRY") {
                @Override
                public void run() {
                    trigger(CLEAR_ERRORS_FOR_RETRY);
                }
            }, 20000, 4 * 60000, 6 * 60000);
            runner4 = Async.scheduleAtFixedRate(new SmileBaseRunnable("POS.IntfDaemon.GET_OFFLINE_SNAPSHOT") {
                @Override
                public void run() {
                    trigger(GET_OFFLINE_SNAPSHOT);
                }
            }, 20000, 1800000); // Mukosi: Changed to 30 minutes due to unnecessary slave loads.
        } else {
            runner1 = Async.scheduleAtFixedRate(new SmileBaseRunnable("POS.IntfDaemon.CHECK_FOR_PAYMENTS") {
                @Override
                public void run() {
                    trigger(CHECK_FOR_PAYMENTS);
                }
            }, 70000, 15 * 60000, 25 * 60000);
            runner2 = Async.scheduleAtFixedRate(new SmileBaseRunnable("POS.IntfDaemon.SEND_TRANSACTIONS") {
                @Override
                public void run() {
                    trigger(SEND_TRANSACTIONS);
                }
            }, 80000, 8 * 60000, 12 * 60000);
            runner3 = Async.scheduleAtFixedRate(new SmileBaseRunnable("POS.IntfDaemon.CLEAR_ERRORS_FOR_RETRY") {
                @Override
                public void run() {
                    trigger(CLEAR_ERRORS_FOR_RETRY);
                }
            }, 90000, 20 * 60000, 40 * 60000);
            runner4 = Async.scheduleAtFixedRate(new SmileBaseRunnable("POS.IntfDaemon.GET_OFFLINE_SNAPSHOT") {
                @Override
                public void run() {
                    trigger(GET_OFFLINE_SNAPSHOT);
                }
            }, 300000, 20 * 60000, 30 * 60000);

        }

        BaseUtils.registerForPropsChanges(this);
    }

    @Override
    public void propsHaveChangedTrigger() {
        X3Helper.initialise();
    }

    @PostConstruct
    public void startUp() {
        BaseUtils.registerForPropsAvailability(this);
        emf = JPAUtils.getEMF("POSPU_RL");
    }

    @PreDestroy
    public void shutDown() {
        BaseUtils.deregisterForPropsAvailability(this);
        Async.cancel(runner1);
        Async.cancel(runner2);
        Async.cancel(runner3);
        Async.cancel(runner4);
        BaseUtils.deregisterForPropsChanges(this);
        JPAUtils.closeEMF(emf);
    }

    private List<Integer> getSaleIdsOutPaymentReference(String paymentRef) {
        String prefix = X3Helper.props.getProperty("CountryPrefix");
        int prefixLength = prefix.length();
        List<Integer> salesIds = new ArrayList();
        log.debug("Extracting Sales Ids out of payment reference [{}]", paymentRef);
        paymentRef = paymentRef.replace(" ", "").replace("\"", "");
        for (String ref : paymentRef.split(",")) {
            if (ref.startsWith(prefix)) {
                ref = ref.substring(prefixLength);
                try {
                    salesIds.add(Integer.parseInt(ref));
                    log.debug("Found Sales Id [{}]", ref);
                } catch (Exception e) {
                    log.debug("[{}] is not an integer", ref);
                }
            }
        }
        log.debug("Found [{}] Sales Ids in reference [{}]", salesIds.size(), paymentRef);
        return salesIds;
    }

    private void checkForPayments(LazyX3Connection lazyConn) {
        log.debug("Checking for unallocated payments in X3");
        EntityManager em = null;
        try {
            // Look in X3 for unallocated payments.
            Collection<X3PaymentData> unallocatedPayments = X3Helper.getUnallocatedX3Payments(lazyConn);
            em = JPAUtils.getEM(emf);
            for (X3PaymentData payment : unallocatedPayments) {
                try {
                    log.debug("Starting transaction on entity manager");
                    JPAUtils.beginTransaction(em);

                    if (payment.depositReference.toLowerCase().startsWith("ci")) {
                        log.debug("This is a cashin bank deposit with reference [{}]", payment.depositReference);
                        new POSManager().processAirtimeTransfersAfterCashInDeposit(em, Integer.parseInt(payment.depositReference.substring(2).trim()), payment.paymentInCents);
                    } else {
                        log.debug("Processing unallocated payment with reference [{}] amount [{}]cents payment transaction data [{}]", new Object[]{payment.depositReference, payment.paymentInCents, payment.paymentTransactionData});
                        List<Integer> salesIds = getSaleIdsOutPaymentReference(payment.depositReference);
                        if (salesIds.isEmpty()) {
                            continue;
                        }
                        new POSManager().processPayment(em, salesIds, payment.paymentInCents, payment.paymentTransactionData, "PD", true, null);
                    }
                    log.debug("Committing transaction");
                    JPAUtils.commitTransaction(em);
                } catch (Exception e) {
                    JPAUtils.rollbackTransaction(em);
                    if (!e.getMessage().contains("Invalid sale status to process payment")
                            && !e.getMessage().contains("amount differs")
                            && !e.getMessage().contains("Payment cannot be processed on a cash sale")
                            && !e.getMessage().contains("Cash in by bank deposit has already been processed")
                            && !e.getMessage().contains("getSingleResult() did not retrieve any entities")) {
                        log.warn("Exception processing payment. Transaction has been rolled back", e);
                        BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "Error processing unallocated payment: " + e.toString() + ", Deposit Reference:" + payment.depositReference);
                    } else {
                        log.debug("Exception processing payment. Transaction has been rolled back: [{}]", e.toString());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Exception checking for payments: [{}]", e.toString());
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "Error getting unallocated payments: " + e.toString());
        } finally {
            JPAUtils.closeEM(em);
        }
    }

    private void clearErrorsForRetry(LazyX3Connection lazyConn) {
        log.debug("Clearing errors for retry in X3");
        EntityManager em = null;
        try {
            int saleId = 0;
            em = JPAUtils.getEM(emf);
            Set<String> retryMatches = BaseUtils.getPropertyAsSet("global.x3.retry.messages.matches");
            JPAUtils.beginTransaction(em);
            Collection<X3RequestState> errors = DAO.getRecentFailedX3Requests(em);
            for (X3RequestState error : errors) {
                log.debug("Found a request in status [{}] for primary key [{}] in table [{}]", new Object[]{error.getStatus(), error.getX3RequestStatePK().getPrimaryKey(), error.getX3RequestStatePK().getTableName()});
                Date origionalDate = null;
                boolean onlySetTransactionBackToPending = false;
                // Check that the origional transaction occured in the last env.pos.x3.errors.retry.days days. If its older than that, dont retry it and rather leave it in the failed state.
                if (error.getX3RequestStatePK().getTableName().equals("sale")) {
                    Sale sale = DAO.getSale(em, error.getX3RequestStatePK().getPrimaryKey());
                    saleId = sale.getSaleId();

                    if (error.getX3RequestStatePK().getTransactionType().equals(X3_TRANSACTION_TYPE_STOCK_LOAN_COMPLETION)) {
                        // Always retry loan completions as the sale could be very old
                        Calendar cal = Calendar.getInstance();
                        cal.add(Calendar.MINUTE, -1);
                        origionalDate = cal.getTime();

                    } else {
                        origionalDate = sale.getSaleDateTime();
                    }
                } else if (error.getX3RequestStatePK().getTableName().equals("cash_in")) {
                    CashIn cashin = DAO.getCashIn(em, error.getX3RequestStatePK().getPrimaryKey());
                    origionalDate = cashin.getCashInDateTime();
                } else if (error.getX3RequestStatePK().getTableName().equals("sale_return")) {
                    SaleReturn ret = DAO.getReturn(em, error.getX3RequestStatePK().getPrimaryKey());
                    origionalDate = ret.getSaleReturnDateTime();
                } else if (error.getX3RequestStatePK().getTransactionType().equals("GLEntry")) {
                    // Always retry GL's. Pretend GL was sent a minute ago
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.MINUTE, -1);
                    origionalDate = cal.getTime();
                    onlySetTransactionBackToPending = true;
                }

                if (origionalDate != null && Utils.isDateInTimeframe(origionalDate, BaseUtils.getIntProperty("env.pos.x3.errors.retry.days"), Calendar.DATE)) {
                    log.debug("State can be retried as the original date was [{}]", origionalDate);
                } else {
                    log.debug("State is too old to be retried as the original date was [{}]", origionalDate);
                    continue;
                }
                log.debug("Looking at failed request [{}] with messages [{}]", error.getX3RequestStatePK(), error.getMessages());

                boolean mustRetry = false;
                for (String match : retryMatches) {
                    log.debug("checking if [{}] contains [{}]", error.getMessages(), match);
                    if (error.getMessages() == null || error.getMessages().isEmpty() || error.getMessages().contains(match)) {
                        mustRetry = true;
                        log.debug("This error was blank or matched [{}] so will be retried", match);
                        break;
                    } else {
                        log.debug("Error does not contain the string");
                    }
                }
                if (!mustRetry) {
                    log.debug("This request need not be retried");
                    continue;
                }

                try {
                    X3TransactionState state = DAO.getX3TransactionForRequest(em, error);
                    //If it is a sale, check if record existss in X3 and update X3_RECORD_IF is so. Otherwise continue with the normal retry as per below.
                    if (error.getX3RequestStatePK().getTableName().equals("sale") && error.getMessages() != null
                            && error.getMessages().contains("Record already exists") && error.getMessages().contains("Transaction stopped")) {
                        String x3DocNum = X3Helper.checkIfSaleExistandDocumentsGeneratedInX3(lazyConn, saleId);
                        String errorMessage = null;
                        if (x3DocNum != null) { // Document already exists in X3.
                            errorMessage = String.format("Document found in X3, setting X3_RECORD_ID to %s.", x3DocNum);
                            log.debug(errorMessage);

                            //Update the request state
                            error.setStatus(X3_TRANSACTION_STATUS_FINISHED);
                            error.setX3RecordId(x3DocNum);
                            error.setMessages(error.getMessages() + "\n\n" + errorMessage);
                            em.persist(error);
                            //Mark transaction state as FI too
                            state.setStatus(X3_TRANSACTION_STATUS_FINISHED);
                            em.persist(state);
                            em.flush();
                            continue;
                        }
                    }

                    if (onlySetTransactionBackToPending) {
                        log.debug("Found failed transaction for the request. Transaction is [{}]. It will be changed to PE", state);
                        state.setStatus(X3_TRANSACTION_STATUS_PENDING);
                        em.persist(state);
                    } else {
                        log.debug("Found failed transaction for the request. Transaction is [{}]. It will be deleted", state);
                        em.remove(state);
                    }

                } catch (javax.persistence.NoResultException nre) {
                    log.debug("The request did not have a transaction so no transaction need be deleted/updated");
                }
                log.debug("Deleting the request so it will be retried");
                em.remove(error);
                em.flush();
            }
        } catch (Exception e) {
            log.warn("Exception clearing errors for retry in X3:", e);
        } finally {
            JPAUtils.commitTransactionAndClose(em);
        }
    }

    private void cancelExpiredSales(LazyX3Connection lazyConn) {
        log.debug("Expiring old quotes and invoices");
        EntityManager em = null;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            log.debug("Getting a list of sales to expire");
            Collection<Integer> expiryList = DAO.getExpiredSales(em);
            JPAUtils.commitTransaction(em);
            for (Integer saleId : expiryList) {
                try {
                    JPAUtils.beginTransaction(em);
                    log.debug("Calling reverseSale for sale id [{}]", saleId);
                    new POSManager().reverseSale(em, saleId);
                } catch (Exception e) {
                    log.debug("Error expiring quote/invoice [{}]. Its probably been expired by another process. Will ignore : [{}]", saleId, e.toString());
                } finally {
                    JPAUtils.commitTransaction(em);
                }
            }
        } catch (Exception e) {
            log.warn("Error expiring quotes/invoices. Will ignore", e);
        } finally {
            JPAUtils.closeEM(em);
        }
        log.debug("Finished expiring old quotes and invoices");
    }

    //********************************************************
    // Functions to fetch transactions to process
    //********************************************************
    private int sendCashSales(LazyX3Connection lazyConn) {
        log.debug("Sending Cash Sales Transactions to X3");
        EntityManager em = null;
        int salesSent = 0;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            log.debug("Getting a list of cash sales that have not been sent to X3");
            Collection<com.smilecoms.pos.db.model.Sale> unprocessedCashSales = DAO.getSalesByPaymentMethodAndStatusWithNoX3TransactionOfType(
                    em,
                    POSManager.PAYMENT_METHOD_CASH,
                    POSManager.PAYMENT_STATUS_PAID,
                    X3_TRANSACTION_TYPE_CASH_SALE,
                    BATCH_SIZE,
                    BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days"));
            JPAUtils.commitTransaction(em);
            log.debug("There are [{}] paid cash sales rows that need to be sent to X3", unprocessedCashSales.size());
            for (com.smilecoms.pos.db.model.Sale sale : unprocessedCashSales) {
                try {
                    JPAUtils.beginTransaction(em);
                    Collection<X3TransactionState> states = DAO.getX3TransactionsByType(em, sale.getSaleId(), TABLE_SALE, X3_TRANSACTION_TYPE_CASH_SALE);
                    boolean sentSale = sendCashSaleToX3(em, sale, states);
                    if (sentSale) {
                        salesSent++;
                    }
                } catch (Exception e) {
                    log.debug("Error dealing with unprocessed cash sale", e);
                } finally {
                    JPAUtils.commitTransaction(em);
                }
            }
        } catch (Exception e) {
            log.warn("Error in X3InterfaceDaemon. Will ignore : [{}]", e.toString());
        } finally {
            JPAUtils.closeEM(em);
        }
        return salesSent;
    }

    private int sendAirtimeSales(LazyX3Connection lazyConn) {
        log.debug("Sending Airtime Sales Transactions to X3");
        EntityManager em = null;
        int salesSent = 0;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            log.debug("Getting a list of airtime sales that have not been sent to X3");
            Collection<com.smilecoms.pos.db.model.Sale> unprocessedAirtimeSales = DAO.getSalesByPaymentMethodAndStatusWithNoX3TransactionOfType(
                    em,
                    POSManager.PAYMENT_METHOD_AIRTIME,
                    POSManager.PAYMENT_STATUS_PAID,
                    X3_TRANSACTION_TYPE_AIRTIME_SALE,
                    BATCH_SIZE,
                    BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days"));
            JPAUtils.commitTransaction(em);
            log.debug("There are [{}] paid airtime sales rows that need to be sent to X3", unprocessedAirtimeSales.size());
            for (com.smilecoms.pos.db.model.Sale sale : unprocessedAirtimeSales) {
                try {
                    JPAUtils.beginTransaction(em);
                    Collection<X3TransactionState> states = DAO.getX3TransactionsByType(em, sale.getSaleId(), TABLE_SALE, X3_TRANSACTION_TYPE_AIRTIME_SALE);
                    boolean sentSale = sendAirtimeSaleToX3(em, sale, states);
                    if (sentSale) {
                        salesSent++;
                    }
                } catch (Exception e) {
                    log.debug("Error dealing with unprocessed airtime sale", e);
                } finally {
                    JPAUtils.commitTransaction(em);
                }
            }
        } catch (Exception e) {
            log.warn("Error in X3InterfaceDaemon. Will ignore : [{}]", e.toString());
        } finally {
            JPAUtils.closeEM(em);
        }
        return salesSent;
    }

    private int sendCardPaymentSales(LazyX3Connection lazyConn) {
        log.debug("Sending Card Payment Sales Transactions to X3");
        EntityManager em = null;
        int salesSent = 0;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            log.debug("Getting a list of card payment sales that have not been sent to X3");
            Collection<com.smilecoms.pos.db.model.Sale> unprocessedCardPaymentSales = DAO.getSalesByPaymentMethodAndStatusWithNoX3TransactionOfType(
                    em,
                    POSManager.PAYMENT_METHOD_CARD_PAYMENT,
                    POSManager.PAYMENT_STATUS_PAID,
                    X3_TRANSACTION_TYPE_CARD_PAYMENT_SALE,
                    BATCH_SIZE,
                    BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days"));
            JPAUtils.commitTransaction(em);
            log.debug("There are [{}] paid card payment sales rows that need to be sent to X3", unprocessedCardPaymentSales.size());
            for (com.smilecoms.pos.db.model.Sale sale : unprocessedCardPaymentSales) {
                try {
                    JPAUtils.beginTransaction(em);
                    Collection<X3TransactionState> states = DAO.getX3TransactionsByType(em, sale.getSaleId(), TABLE_SALE, X3_TRANSACTION_TYPE_CARD_PAYMENT_SALE);
                    boolean sentSale = sendCardPaymentSaleToX3(em, sale, states);
                    if (sentSale) {
                        salesSent++;
                    }
                } catch (Exception e) {
                    log.debug("Error dealing with unprocessed card payment sale", e);
                } finally {
                    JPAUtils.commitTransaction(em);
                }
            }
        } catch (Exception e) {
            log.warn("Error in X3InterfaceDaemon. Will ignore : [{}]", e.toString());
        } finally {
            JPAUtils.closeEM(em);
        }
        return salesSent;
    }

    private int sendCreditFacilityPaymentSales(LazyX3Connection lazyConn) {
        log.debug("Sending Credit Facility Sales Transactions to X3");
        EntityManager em = null;
        int salesSent = 0;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            log.debug("Getting a list of credit facility payment sales that have not been sent to X3");
            Collection<com.smilecoms.pos.db.model.Sale> unprocessedCreditFacilityPaymentSales = DAO.getSalesByPaymentMethodAndStatusWithNoX3TransactionOfType(
                    em,
                    POSManager.PAYMENT_METHOD_CREDIT_FACILITY,
                    POSManager.PAYMENT_STATUS_PAID,
                    X3_TRANSACTION_TYPE_CREDIT_FACILITY_SALE,
                    BATCH_SIZE,
                    BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days"));
            JPAUtils.commitTransaction(em);
            log.debug("There are [{}] paid credit facility sales rows that need to be sent to X3", unprocessedCreditFacilityPaymentSales.size());
            for (com.smilecoms.pos.db.model.Sale sale : unprocessedCreditFacilityPaymentSales) {
                try {
                    JPAUtils.beginTransaction(em);
                    Collection<X3TransactionState> states = DAO.getX3TransactionsByType(em, sale.getSaleId(), TABLE_SALE, X3_TRANSACTION_TYPE_CREDIT_FACILITY_SALE);
                    boolean sentSale = sendCreditFacilityPaymentSaleToX3(em, sale, states);
                    if (sentSale) {
                        salesSent++;
                    }
                } catch (Exception e) {
                    log.debug("Error dealing with unprocessed credit facility payment sale", e);
                } finally {
                    JPAUtils.commitTransaction(em);
                }
            }
        } catch (Exception e) {
            log.warn("Error in X3InterfaceDaemon. Will ignore : [{}]", e.toString());
        } finally {
            JPAUtils.closeEM(em);
        }
        return salesSent;
    }

    private int sendClearingBureauSales(LazyX3Connection lazyConn) {
        log.debug("Sending Clearing Bureau Sales Transactions to X3");
        EntityManager em = null;
        int salesSent = 0;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            log.debug("Getting a list of clearing bureau sales that have not been sent to X3");
            Collection<com.smilecoms.pos.db.model.Sale> unprocessedPaymentGatewaySales = DAO.getSalesByPaymentMethodAndStatusWithNoX3TransactionOfType(
                    em,
                    POSManager.PAYMENT_METHOD_CLEARING_BUREAU,
                    POSManager.PAYMENT_STATUS_PAID,
                    X3_TRANSACTION_TYPE_CLEARING_BUREAU_SALE,
                    BATCH_SIZE,
                    BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days"));
            JPAUtils.commitTransaction(em);
            log.debug("There are [{}] clearing bureau sales that need to be sent to X3", unprocessedPaymentGatewaySales.size());
            for (com.smilecoms.pos.db.model.Sale sale : unprocessedPaymentGatewaySales) {
                try {
                    JPAUtils.beginTransaction(em);
                    Collection<X3TransactionState> states = DAO.getX3TransactionsByType(em, sale.getSaleId(), TABLE_SALE, X3_TRANSACTION_TYPE_CLEARING_BUREAU_SALE);
                    boolean sentSale = sendClearingBureauSaleToX3(em, sale, states);
                    if (sentSale) {
                        salesSent++;
                    }
                } catch (Exception e) {
                    log.debug("Error dealing with unprocessed clearing bureau sale", e);
                } finally {
                    JPAUtils.commitTransaction(em);
                }
            }
        } catch (Exception e) {
            log.warn("Error in X3InterfaceDaemon. Will ignore : [{}]", e.toString());
        } finally {
            JPAUtils.closeEM(em);
        }
        return salesSent;
    }

    private void sendCreditNoteSales(LazyX3Connection lazyConn) {
        log.debug("Sending Credit Note Sales Transactions to X3");
        EntityManager em = null;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            log.debug("Getting a list of credit note sales that have not been sent to X3");
            Collection<com.smilecoms.pos.db.model.Sale> unprocessedCreditNoteSales = DAO.getSalesByPaymentMethodAndStatusWithNoX3TransactionOfType(
                    em,
                    POSManager.PAYMENT_METHOD_CREDIT_NOTE,
                    POSManager.PAYMENT_STATUS_PAID,
                    X3_TRANSACTION_TYPE_CREDIT_NOTE_SALE,
                    BATCH_SIZE,
                    BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days"));
            JPAUtils.commitTransaction(em);
            log.debug("There are [{}] paid credit note sales rows that need to be sent to X3", unprocessedCreditNoteSales.size());
            for (com.smilecoms.pos.db.model.Sale sale : unprocessedCreditNoteSales) {
                try {
                    JPAUtils.beginTransaction(em);
                    Collection<X3TransactionState> states = DAO.getX3TransactionsByType(em, sale.getSaleId(), TABLE_SALE, X3_TRANSACTION_TYPE_CREDIT_NOTE_SALE);
                    sendCreditNoteSaleToX3(em, sale, states);
                } catch (Exception e) {
                    log.debug("Error dealing with unprocessed credit note sale", e);
                } finally {
                    JPAUtils.commitTransaction(em);
                }
            }
        } catch (Exception e) {
            log.warn("Error in X3InterfaceDaemon. Will ignore : [{}]", e.toString());
        } finally {
            JPAUtils.closeEM(em);
        }
    }

    private int sendCreditAccountSales(LazyX3Connection lazyConn) {
        log.debug("Sending Credit Account Sales Transactions to X3");
        EntityManager em = null;
        int salesSent = 0;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            log.debug("Getting a list of credit account sales that have not been sent to X3");
            Collection<com.smilecoms.pos.db.model.Sale> unprocessedCreditAccountSales = DAO.getSalesByPaymentMethodAndStatusWithNoX3TransactionOfType(
                    em,
                    POSManager.PAYMENT_METHOD_CREDIT_ACCOUNT,
                    POSManager.PAYMENT_STATUS_PAID,
                    X3_TRANSACTION_TYPE_CREDIT_ACCOUNT_SALE,
                    BATCH_SIZE,
                    BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days"));
            JPAUtils.commitTransaction(em);
            log.debug("There are [{}] credit account sales rows that need to be sent to X3", unprocessedCreditAccountSales.size());
            for (com.smilecoms.pos.db.model.Sale sale : unprocessedCreditAccountSales) {
                try {
                    JPAUtils.beginTransaction(em);
                    Collection<X3TransactionState> states = DAO.getX3TransactionStateRows(em, sale.getSaleId(), TABLE_SALE);
                    boolean sentSale = sendCreditAccountSaleToX3(em, sale, states);
                    if (sentSale) {
                        salesSent++;
                    }
                } catch (Exception e) {
                    log.debug("Error dealing with unprocessed sale", e);
                } finally {
                    JPAUtils.commitTransaction(em);
                }
            }
        } catch (Exception e) {
            log.warn("Error in X3InterfaceDaemon. Will ignore : [{}]", e.toString());
        } finally {
            JPAUtils.closeEM(em);
        }
        return salesSent;
    }

    private void sendPendingPaymentSales(LazyX3Connection lazyConn) {
        log.debug("Sending Pending Payment Sales Transactions to X3");
        EntityManager em = null;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            log.debug("Getting a list of pending payment sales that have not been sent to X3");
            Collection<com.smilecoms.pos.db.model.Sale> unprocessedPendingPaymentSales = DAO.getSalesByPaymentMethodAndStatusWithNoX3TransactionOfType(
                    em,
                    POSManager.PAYMENT_METHOD_BANK_TRANSFER,
                    POSManager.PAYMENT_STATUS_PENDING_PAYMENT,
                    X3_TRANSACTION_TYPE_SALE_PENDING_PAYMENT,
                    BATCH_SIZE,
                    BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days"));
            unprocessedPendingPaymentSales.addAll(DAO.getSalesByPaymentMethodAndStatusWithNoX3TransactionOfType(
                    em,
                    POSManager.PAYMENT_METHOD_CHEQUE,
                    POSManager.PAYMENT_STATUS_PENDING_PAYMENT,
                    X3_TRANSACTION_TYPE_SALE_PENDING_PAYMENT,
                    BATCH_SIZE,
                    BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days")));
            unprocessedPendingPaymentSales.addAll(DAO.getSalesByPaymentMethodAndStatusWithNoX3TransactionOfType(
                    em,
                    POSManager.PAYMENT_METHOD_CARD_INTEGRATION,
                    POSManager.PAYMENT_STATUS_PENDING_PAYMENT,
                    X3_TRANSACTION_TYPE_SALE_PENDING_PAYMENT,
                    BATCH_SIZE,
                    BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days")));
            JPAUtils.commitTransaction(em);
            log.debug("There are [{}] pending payment sales rows that need to be sent to X3", unprocessedPendingPaymentSales.size());
            for (com.smilecoms.pos.db.model.Sale sale : unprocessedPendingPaymentSales) {
                try {
                    JPAUtils.beginTransaction(em);
                    Collection<X3TransactionState> states = DAO.getX3TransactionStateRows(em, sale.getSaleId(), TABLE_SALE);
                    sendPendingPaymentSaleToX3(em, sale, states);
                } catch (Exception e) {
                    log.debug("Error dealing with unprocessed sale", e);
                } finally {
                    JPAUtils.commitTransaction(em);
                }
            }
        } catch (Exception e) {
            log.warn("Error in X3InterfaceDaemon. Will ignore : [{}]", e.toString());
        } finally {
            JPAUtils.closeEM(em);
        }
    }

    private void sendQuotesWithStockReservation(LazyX3Connection lazyConn) {
        log.debug("Sending Quotes With Stock Reservation Transactions to X3");
        EntityManager em = null;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            log.debug("Getting a list of quotes that have not been sent to X3");
            Collection<com.smilecoms.pos.db.model.Sale> unprocessedQuotes = DAO.getSalesByPaymentMethodAndStatusWithNoX3TransactionOfType(
                    em,
                    POSManager.PAYMENT_METHOD_QUOTE,
                    POSManager.PAYMENT_STATUS_QUOTE,
                    X3_TRANSACTION_TYPE_QUOTE_WITH_RESERVATION,
                    BATCH_SIZE,
                    BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days"));
            JPAUtils.commitTransaction(em);
            log.debug("There are [{}] quote rows that need to be sent to X3", unprocessedQuotes.size());
            for (com.smilecoms.pos.db.model.Sale sale : unprocessedQuotes) {
                try {
                    JPAUtils.beginTransaction(em);
                    Collection<X3TransactionState> states = DAO.getX3TransactionStateRows(em, sale.getSaleId(), TABLE_SALE);
                    sendQuoteToX3(em, sale, states);
                } catch (Exception e) {
                    log.debug("Error dealing with unprocessed sale", e);
                } finally {
                    JPAUtils.commitTransaction(em);
                }
            }
        } catch (Exception e) {
            log.warn("Error in X3InterfaceDaemon. Will ignore : [{}]", e.toString());
        } finally {
            JPAUtils.closeEM(em);
        }
    }

    private void sendStockLoans(LazyX3Connection lazyConn) {
        log.debug("Sending Stock Loan Transactions to X3");
        EntityManager em = null;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            log.debug("Getting a list of stock loans that have not been sent to X3");
            Collection<com.smilecoms.pos.db.model.Sale> unprocessedStockLoans = DAO.getSalesByPaymentMethodAndStatusWithNoX3TransactionOfType(
                    em,
                    POSManager.PAYMENT_METHOD_STAFF,
                    POSManager.PAYMENT_STATUS_STAFF_OR_LOAN,
                    X3_TRANSACTION_TYPE_STOCK_LOAN,
                    BATCH_SIZE,
                    BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days"));
            unprocessedStockLoans.addAll(DAO.getSalesByPaymentMethodAndStatusWithNoX3TransactionOfType(
                    em,
                    POSManager.PAYMENT_METHOD_LOAN,
                    POSManager.PAYMENT_STATUS_STAFF_OR_LOAN,
                    X3_TRANSACTION_TYPE_STOCK_LOAN,
                    BATCH_SIZE,
                    BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days")));
            JPAUtils.commitTransaction(em);
            log.debug("There are [{}] stock loans that need to be sent to X3", unprocessedStockLoans.size());
            for (com.smilecoms.pos.db.model.Sale sale : unprocessedStockLoans) {
                try {
                    JPAUtils.beginTransaction(em);
                    Collection<X3TransactionState> states = DAO.getX3TransactionStateRows(em, sale.getSaleId(), TABLE_SALE);
                    sendStockLoanToX3(em, sale, states);
                } catch (Exception e) {
                    log.debug("Error dealing with unprocessed sale", e);
                } finally {
                    JPAUtils.commitTransaction(em);
                }
            }
        } catch (Exception e) {
            log.warn("Error in X3InterfaceDaemon. Will ignore : [{}]", e.toString());
        } finally {
            JPAUtils.closeEM(em);
        }
    }

    private void sendCardIntegrationGLEntries(LazyX3Connection lazyConn) {
        log.debug("Sending card integration GLs to X3");
        EntityManager em = null;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            log.debug("Getting a list of Paid Payment Gateway Sales whos GL's have not been sent to X3");
            Collection<com.smilecoms.pos.db.model.Sale> unprocessedCardIntegrationGLs = DAO.getSalesByPaymentMethodAndStatusWithNoX3TransactionOfType(
                    em,
                    POSManager.PAYMENT_METHOD_PAYMENT_GATEWAY,
                    POSManager.PAYMENT_STATUS_PAID,
                    X3_TRANSACTION_TYPE_CARD_INTEGRATION_GL,
                    BATCH_SIZE,
                    BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days"));
            JPAUtils.commitTransaction(em);
            log.debug("There are [{}] Paid Payment Gateway Sales whos GL's have not been sent that need to be sent to X3", unprocessedCardIntegrationGLs.size());
            for (com.smilecoms.pos.db.model.Sale sale : unprocessedCardIntegrationGLs) {
                try {
                    JPAUtils.beginTransaction(em);
                    Collection<X3TransactionState> states = DAO.getX3TransactionStateRows(em, sale.getSaleId(), TABLE_SALE);
                    sendCardIntegrationGLEntryToX3(em, lazyConn, sale, states);
                } catch (Exception e) {
                    log.debug("Error dealing with unprocessed Payment Gateway Sale GL", e);
                } finally {
                    JPAUtils.commitTransaction(em);
                }
            }
        } catch (Exception e) {
            log.warn("Error in X3InterfaceDaemon. Will ignore : [{}]", e.toString());
        } finally {
            JPAUtils.closeEM(em);
        }
    }

    private void sendStockLoanCompletions(LazyX3Connection lazyConn) {
        log.debug("Sending Stock Loan Completions Transactions to X3");
        EntityManager em = null;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            log.debug("Getting a list of stock loan completions that have not been sent to X3");
            Collection<com.smilecoms.pos.db.model.Sale> unprocessedStockLoanCompletions = DAO.getSalesByPaymentMethodAndStatusWithNoX3TransactionOfType(
                    em,
                    POSManager.PAYMENT_METHOD_STAFF,
                    POSManager.PAYMENT_STATUS_STAFF_OR_LOAN_COMPLETE,
                    X3_TRANSACTION_TYPE_STOCK_LOAN_COMPLETION,
                    BATCH_SIZE);
            unprocessedStockLoanCompletions.addAll(DAO.getSalesByPaymentMethodAndStatusWithNoX3TransactionOfType(
                    em,
                    POSManager.PAYMENT_METHOD_LOAN,
                    POSManager.PAYMENT_STATUS_STAFF_OR_LOAN_COMPLETE,
                    X3_TRANSACTION_TYPE_STOCK_LOAN_COMPLETION,
                    BATCH_SIZE));
            JPAUtils.commitTransaction(em);
            log.debug("There are [{}] stock loan completions that need to be sent to X3", unprocessedStockLoanCompletions.size());
            for (com.smilecoms.pos.db.model.Sale sale : unprocessedStockLoanCompletions) {
                try {
                    JPAUtils.beginTransaction(em);
                    Collection<X3TransactionState> states = DAO.getX3TransactionStateRows(em, sale.getSaleId(), TABLE_SALE);
                    sendStockLoanCompletionToX3(em, sale, states);
                } catch (Exception e) {
                    log.debug("Error dealing with unprocessed sale", e);
                } finally {
                    JPAUtils.commitTransaction(em);
                }
            }
        } catch (Exception e) {
            log.warn("Error in X3InterfaceDaemon. Will ignore : [{}]", e.toString());
        } finally {
            JPAUtils.closeEM(em);
        }
    }

    private void sendPayments(LazyX3Connection lazyConn) {
        log.debug("Sending Paid Cheque, Bank Transfer and Card Integration Sales Transactions to X3");
        EntityManager em = null;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            log.debug("Getting a list of payments that have not been sent to X3");
            Collection<com.smilecoms.pos.db.model.Sale> unprocessedPayments = DAO.getSalesByPaymentMethodAndStatusWithNoX3TransactionOfType(
                    em,
                    POSManager.PAYMENT_METHOD_BANK_TRANSFER,
                    POSManager.PAYMENT_STATUS_PAID,
                    X3_TRANSACTION_TYPE_PAYMENT,
                    BATCH_SIZE,
                    BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days"));
            unprocessedPayments.addAll(DAO.getSalesByPaymentMethodAndStatusWithNoX3TransactionOfType(
                    em,
                    POSManager.PAYMENT_METHOD_CHEQUE,
                    POSManager.PAYMENT_STATUS_PAID,
                    X3_TRANSACTION_TYPE_PAYMENT,
                    BATCH_SIZE,
                    BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days")));
            unprocessedPayments.addAll(DAO.getSalesByPaymentMethodAndStatusWithNoX3TransactionOfType(
                    em,
                    POSManager.PAYMENT_METHOD_CARD_INTEGRATION,
                    POSManager.PAYMENT_STATUS_PAID,
                    X3_TRANSACTION_TYPE_PAYMENT,
                    BATCH_SIZE,
                    BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days")));
            JPAUtils.commitTransaction(em);
            log.debug("There are [{}] payments that need to be sent to X3", unprocessedPayments.size());
            for (com.smilecoms.pos.db.model.Sale sale : unprocessedPayments) {
                try {
                    JPAUtils.beginTransaction(em);
                    Collection<X3TransactionState> states = DAO.getX3TransactionStateRows(em, sale.getSaleId(), TABLE_SALE);
                    sendPaymentToX3(em, sale, states);
                } catch (Exception e) {
                    log.debug("Error dealing with unprocessed sale", e);
                } finally {
                    JPAUtils.commitTransaction(em);
                }
            }
        } catch (Exception e) {
            log.warn("Error in X3InterfaceDaemon. Will ignore : [{}]", e.toString());
        } finally {
            JPAUtils.closeEM(em);
        }
    }

    // Customer chooses to not take Smile up on the invoice or it times out (was no paid after X days)
    private void sendSaleReversals(LazyX3Connection lazyConn) {
        log.debug("Sending Sale Reversal Transactions to X3");
        EntityManager em = null;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            log.debug("Getting a list of reversals that have not been sent to X3");
            Collection<com.smilecoms.pos.db.model.Sale> unprocessedReversals = DAO.getSalesByPaymentMethodAndStatusWithNoX3TransactionOfType(
                    em,
                    POSManager.PAYMENT_METHOD_CREDIT_ACCOUNT,
                    POSManager.PAYMENT_STATUS_INVOICE_REVERSAL,
                    X3_TRANSACTION_TYPE_REVERSAL,
                    BATCH_SIZE,
                    BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days")); // RV = reverse
            unprocessedReversals.addAll(DAO.getSalesByPaymentMethodAndStatusWithNoX3TransactionOfType(
                    em,
                    POSManager.PAYMENT_METHOD_CASH,
                    POSManager.PAYMENT_STATUS_INVOICE_REVERSAL,
                    X3_TRANSACTION_TYPE_REVERSAL,
                    BATCH_SIZE,
                    BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days")));
            unprocessedReversals.addAll(DAO.getSalesByPaymentMethodAndStatusWithNoX3TransactionOfType(
                    em,
                    POSManager.PAYMENT_METHOD_BANK_TRANSFER,
                    POSManager.PAYMENT_STATUS_INVOICE_REVERSAL,
                    X3_TRANSACTION_TYPE_REVERSAL,
                    BATCH_SIZE,
                    BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days")));
            unprocessedReversals.addAll(DAO.getSalesByPaymentMethodAndStatusWithNoX3TransactionOfType(
                    em,
                    POSManager.PAYMENT_METHOD_CHEQUE,
                    POSManager.PAYMENT_STATUS_INVOICE_REVERSAL,
                    X3_TRANSACTION_TYPE_REVERSAL,
                    BATCH_SIZE,
                    BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days")));
            unprocessedReversals.addAll(DAO.getSalesByPaymentMethodAndStatusWithNoX3TransactionOfType(
                    em,
                    POSManager.PAYMENT_METHOD_QUOTE,
                    POSManager.PAYMENT_STATUS_INVOICE_REVERSAL,
                    X3_TRANSACTION_TYPE_REVERSAL,
                    BATCH_SIZE,
                    BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days")));
            unprocessedReversals.addAll(DAO.getSalesByPaymentMethodAndStatusWithNoX3TransactionOfType(
                    em,
                    POSManager.PAYMENT_METHOD_CREDIT_NOTE,
                    POSManager.PAYMENT_STATUS_INVOICE_REVERSAL,
                    X3_TRANSACTION_TYPE_REVERSAL,
                    BATCH_SIZE,
                    BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days")));
            unprocessedReversals.addAll(DAO.getSalesByPaymentMethodAndStatusWithNoX3TransactionOfType(
                    em,
                    POSManager.PAYMENT_METHOD_CARD_PAYMENT,
                    POSManager.PAYMENT_STATUS_INVOICE_REVERSAL,
                    X3_TRANSACTION_TYPE_REVERSAL,
                    BATCH_SIZE,
                    BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days")));
            unprocessedReversals.addAll(DAO.getSalesByPaymentMethodAndStatusWithNoX3TransactionOfType(
                    em,
                    POSManager.PAYMENT_METHOD_CLEARING_BUREAU,
                    POSManager.PAYMENT_STATUS_INVOICE_REVERSAL,
                    X3_TRANSACTION_TYPE_REVERSAL,
                    BATCH_SIZE,
                    BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days")));
            unprocessedReversals.addAll(DAO.getSalesByPaymentMethodAndStatusWithNoX3TransactionOfType(
                    em,
                    POSManager.PAYMENT_METHOD_CARD_INTEGRATION,
                    POSManager.PAYMENT_STATUS_INVOICE_REVERSAL,
                    X3_TRANSACTION_TYPE_REVERSAL,
                    BATCH_SIZE,
                    BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days")));
            unprocessedReversals.addAll(DAO.getSalesByPaymentMethodAndStatusWithNoX3TransactionOfType(
                    em,
                    POSManager.PAYMENT_METHOD_CREDIT_FACILITY,
                    POSManager.PAYMENT_STATUS_INVOICE_REVERSAL,
                    X3_TRANSACTION_TYPE_REVERSAL,
                    BATCH_SIZE,
                    BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days")));
            JPAUtils.commitTransaction(em);
            log.debug("There are [{}] reversals that need to be sent to X3", unprocessedReversals.size());
            for (com.smilecoms.pos.db.model.Sale sale : unprocessedReversals) {
                try {
                    JPAUtils.beginTransaction(em);
                    Collection<X3TransactionState> states = DAO.getX3TransactionStateRows(em, sale.getSaleId(), TABLE_SALE);
                    sendReversalToX3(em, lazyConn, sale, states);
                } catch (Exception e) {
                    log.debug("Error dealing with unprocessed sale", e);
                } finally {
                    JPAUtils.commitTransaction(em);
                }
            }
        } catch (Exception e) {
            log.warn("Error in X3InterfaceDaemon. Will ignore : [{}]", e.toString());
        } finally {
            JPAUtils.closeEM(em);
        }
    }

    // This is for when a customer returns the device cause its faulty and gets a new one instead. 
    // The return must generate a credit note that can be used to get a new device in a new sale at no cost
    private void sendPaidReturns(LazyX3Connection lazyConn) {
        log.debug("Sending Paid Returns Transactions to X3");
        EntityManager em = null;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            log.debug("Getting a list of returns that have not been sent to X3");
            Collection<com.smilecoms.pos.db.model.SaleReturn> unprocessedReturns = DAO.getReturnsWithNoX3Transaction(em, BATCH_SIZE);
            JPAUtils.commitTransaction(em);
            log.debug("There are [{}] returns that need to be sent to X3", unprocessedReturns.size());
            for (com.smilecoms.pos.db.model.SaleReturn dbReturn : unprocessedReturns) {
                try {
                    JPAUtils.beginTransaction(em);
                    log.debug("Getting an exclusive lock on the return with Id [{}] so that we avoid processing a return twice", dbReturn.getSaleReturnId());
                    dbReturn = DAO.getLockedReturn(em, dbReturn.getSaleReturnId());
                    log.debug("Got an exclusive lock on the return with Id [{}]. Getting current transaction state for this return", dbReturn.getSaleReturnId());
                    Collection<X3TransactionState> states = DAO.getX3TransactionStateRows(em, dbReturn.getSaleReturnId(), TABLE_SALE_RETURN);
                    sendReturnToX3(em, lazyConn, dbReturn, states);
                } catch (Exception e) {
                    log.debug("Error dealing with unprocessed sale", e);
                } finally {
                    JPAUtils.commitTransaction(em);
                }
            }
        } catch (Exception e) {
            log.warn("Error in X3InterfaceDaemon. Will ignore : [{}]", e.toString());
        } finally {
            JPAUtils.closeEM(em);
        }
    }

    private void sendLineReturns(LazyX3Connection lazyConn) {
        log.debug("Sending Paid Returns Transactions to X3");
        EntityManager em = null;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            log.debug("Getting a list of line returns that have not been sent to X3");
            Collection<com.smilecoms.pos.db.model.ReturnReplacement> unprocessedLineReturns = DAO.getLineReturnsWithNoX3Transaction(em, BATCH_SIZE);
            JPAUtils.commitTransaction(em);
            log.debug("There are [{}] line returns that need to be sent to X3", unprocessedLineReturns.size());
            for (com.smilecoms.pos.db.model.ReturnReplacement dbReturn : unprocessedLineReturns) {
                try {
                    JPAUtils.beginTransaction(em);
                    log.debug("Getting an exclusive lock on the line return with Id [{}] so that we avoid processing a return twice", dbReturn.getReturnReplacementId());
                    dbReturn = DAO.getReturnOrReplacement(em, dbReturn.getReturnReplacementId());
                    log.debug("Got an exclusive lock on the line return with Id [{}]. Getting current transaction state for this return", dbReturn.getReturnReplacementId());
                    Collection<X3TransactionState> states = DAO.getX3TransactionStateRows(em, dbReturn.getReturnReplacementId(), TABLE_SALE_RETURN);
                    sendLineReturnToX3(em, lazyConn, dbReturn, states);
                } catch (Exception e) {
                    log.debug("Error dealing with unprocessed sale", e);
                } finally {
                    JPAUtils.commitTransaction(em);
                }
            }
        } catch (Exception e) {
            log.warn("Error in X3InterfaceDaemon. Will ignore : [{}]", e.toString());
        } finally {
            JPAUtils.closeEM(em);
        }
    }

    private void sendLineReplacements(LazyX3Connection lazyConn) {
        log.debug("Sending Replacement Transactions to X3");
        EntityManager em = null;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            log.debug("Getting a list of line replacements that have not been sent to X3");
            Collection<com.smilecoms.pos.db.model.ReturnReplacement> unprocessedLineReturns = DAO.getLineReplacementsWithNoX3Transaction(em, BATCH_SIZE);
            JPAUtils.commitTransaction(em);
            log.debug("There are [{}] line replacements that need to be sent to X3", unprocessedLineReturns.size());
            for (com.smilecoms.pos.db.model.ReturnReplacement dbReturn : unprocessedLineReturns) {
                try {
                    JPAUtils.beginTransaction(em);
                    log.debug("Getting an exclusive lock on the line replacement with Id [{}] so that we avoid processing a return twice", dbReturn.getReturnReplacementId());
                    dbReturn = DAO.getReturnOrReplacement(em, dbReturn.getReturnReplacementId());
                    log.debug("Got an exclusive lock on the line replacement with Id [{}]. Getting current transaction state for this replacement", dbReturn.getReturnReplacementId());
                    Collection<X3TransactionState> states = DAO.getX3TransactionStateRows(em, dbReturn.getReturnReplacementId(), TABLE_SALE_RETURN);
                    sendLineReplacementToX3(em, lazyConn, dbReturn, states);
                } catch (Exception e) {
                    log.debug("Error dealing with unprocessed sale", e);
                } finally {
                    JPAUtils.commitTransaction(em);
                }
            }
        } catch (Exception e) {
            log.warn("Error in X3InterfaceDaemon. Will ignore : [{}]", e.toString());
        } finally {
            JPAUtils.closeEM(em);
        }
    }

    private void sendGLEntries(LazyX3Connection lazyConn) {
        log.debug("Sending GL Entry Transactions to X3");
        EntityManager em = null;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            log.debug("Getting a list of GL Entries that have not been sent to X3");
            Collection<X3TransactionState> unprocessedGLEntries = DAO.getX3TransactionsByTypeAndStatus(
                    em,
                    X3_TRANSACTION_TYPE_GL_ENTRY,
                    X3_TRANSACTION_STATUS_PENDING,
                    BATCH_SIZE);
            JPAUtils.commitTransaction(em);
            log.debug("There are [{}] GL Entries that need to be sent to X3", unprocessedGLEntries.size());
            for (X3TransactionState state : unprocessedGLEntries) {
                try {
                    JPAUtils.beginTransaction(em);
                    log.debug("Getting an exclusive lock on the X3 state row with primary key [{}] so that we avoid processing a transaction twice", state.getX3TransactionStatePK());
                    state = DAO.getLockedState(em, state);
                    log.debug("Got an exclusive lock on the X3 state row primary key [{}]", state.getX3TransactionStatePK());
                    sendGLEntryToX3(lazyConn, em, state);
                } catch (Exception e) {
                    log.debug("Error dealing with unprocessed GL entry", e);
                } finally {
                    JPAUtils.commitTransaction(em);
                }
            }
        } catch (Exception e) {
            log.warn("Error in X3InterfaceDaemon. Will ignore : [{}]", e.toString());
        } finally {
            JPAUtils.closeEM(em);
        }
    }

    private void sendCashInEntries(LazyX3Connection lazyConn) {
        log.debug("Sending CashIn Transactions to X3");
        EntityManager em = null;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            log.debug("Getting a list of CashIn that have not been sent to X3");
            Collection<com.smilecoms.pos.db.model.CashIn> unprocessedCashIns = DAO.getCompletedCashInsWithNoX3Transaction(em, BATCH_SIZE);
            JPAUtils.commitTransaction(em);
            log.debug("There are [{}] CashIn that need to be sent to X3", unprocessedCashIns.size());
            for (com.smilecoms.pos.db.model.CashIn dbCashIn : unprocessedCashIns) {
                try {
                    JPAUtils.beginTransaction(em);
                    log.debug("Getting an exclusive lock on the CashIn with Id [{}] so that we avoid processing a CashIn twice", dbCashIn.getCashInId());
                    dbCashIn = DAO.getLockedCashIn(em, dbCashIn.getCashInId());
                    log.debug("Got an exclusive lock on the CashIn with Id [{}]. Getting current transaction state for this CashIn", dbCashIn.getCashInId());
                    Collection<X3TransactionState> states = DAO.getX3TransactionStateRows(em, dbCashIn.getCashInId(), TABLE_CASHIN);
                    sendCashInGLEntryToX3(em, lazyConn, dbCashIn, states);
                } catch (Exception e) {
                    log.debug("Error dealing with cashin", e);
                } finally {
                    JPAUtils.commitTransaction(em);
                }
            }
        } catch (Exception e) {
            log.warn("Error in X3InterfaceDaemon. Will ignore : [{}]", e.toString());
        } finally {
            JPAUtils.closeEM(em);
        }
    }

    private void sendPSCTEntries(LazyX3Connection lazyConn) {
        log.debug("Sending PSCT (Partner Sales Commission Transfer) Transactions to X3");
        EntityManager em = null;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            log.debug("Getting a list of PSCT transactions that have not been sent to X3 since env.pos.x3.daemon.lookback.days days");
            Collection<com.smilecoms.pos.db.model.AccountHistory> unprocessedPSCTransactions = DAO.getPendingPSCTTransactions(em, BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days"));
            JPAUtils.commitTransaction(em);
            log.debug("There are [{}] PSCT transactions that need to be sent to X3", unprocessedPSCTransactions.size());
            for (com.smilecoms.pos.db.model.AccountHistory dbPSCTransaction : unprocessedPSCTransactions) {
                try {
                    JPAUtils.beginTransaction(em);
                    log.debug("Getting an exclusive lock on the PSC Transaction with Id [{}] so that we avoid processing a PSC Transaction twice", dbPSCTransaction.getId());
                    dbPSCTransaction = DAO.getLockedPSCTransaction(em, dbPSCTransaction.getId().intValue());
                    log.debug("Got an exclusive lock on the PSC Transaction with Id [{}]. Getting current transaction state for this PSC Transaction", dbPSCTransaction.getId());
                    Collection<X3TransactionState> states = DAO.getX3TransactionStateRows(em, dbPSCTransaction.getId().intValue(), TABLE_PSC_TRANSACTION);
                    sendPSCTransactionGLEntryToX3(em, lazyConn, dbPSCTransaction, states);
                } catch (Exception e) {
                    log.debug("Error dealing with unprocessed PSC Transaction GL", e);
                } finally {
                    JPAUtils.commitTransaction(em);
                }
            }
        } catch (Exception e) {
            log.warn("Error in X3InterfaceDaemon. Will ignore : [{}]", e.toString());
        } finally {
            JPAUtils.closeEM(em);
        }
    }

    //********************************************************
    // Functions to deal with an individual transaction of a certain transaction type
    // These transactions are POS view of the world and are business events that POS sees
    // The use X3Helper which in turn provides what X3 provides
    //********************************************************
    private boolean sendCashSaleToX3(EntityManager em, com.smilecoms.pos.db.model.Sale sale, Collection<X3TransactionState> states) throws Exception {
        if (!states.isEmpty()) {
            // Transaction has been processed in X3 already - probably by another thread somewhere
            return false;
        }
        Collection<SaleRow> salesRows = DAO.getSalesRowsAndSubRows(em, sale.getSaleId());
        log.debug("Processing Cash Sale with id [{}]", sale.getSaleId());
        X3TransactionState transactionState = DAO.createX3TransactionInOwnTransactionScope(emf, sale.getSaleId(), TABLE_SALE, X3_TRANSACTION_TYPE_CASH_SALE, X3_TRANSACTION_STATUS_IN_PROGRESS, "");
        String transactionStatus = X3_TRANSACTION_STATUS_ERROR;
        try {
            CAdxResultXml invResult = X3Helper.createInvoice(em, emf, transactionState, sale, salesRows, X3Helper.TYPE_CASH_SALE_WITH_RECEIPT);
            transactionStatus = X3_TRANSACTION_STATUS_FINISHED;
        } finally {
            DAO.setX3TransactionResultInOwnTransactionScope(emf, transactionState, transactionStatus);
        }

        log.debug("Finished processing sale row with id [{}]", sale.getSaleId());
        return true;
    }

    private boolean sendAirtimeSaleToX3(EntityManager em, com.smilecoms.pos.db.model.Sale sale, Collection<X3TransactionState> states) throws Exception {
        if (!states.isEmpty()) {
            // Transaction has been processed in X3 already - probably by another thread somewhere
            return false;
        }
        Collection<SaleRow> salesRows = DAO.getSalesRowsAndSubRows(em, sale.getSaleId());
        log.debug("Processing Cash Sale with id [{}]", sale.getSaleId());
        X3TransactionState transactionState = DAO.createX3TransactionInOwnTransactionScope(emf, sale.getSaleId(), TABLE_SALE, X3_TRANSACTION_TYPE_AIRTIME_SALE, X3_TRANSACTION_STATUS_IN_PROGRESS, "");
        String transactionStatus = X3_TRANSACTION_STATUS_ERROR;
        try {
            CAdxResultXml invResult = X3Helper.createInvoice(em, emf, transactionState, sale, salesRows, X3Helper.TYPE_AIRTIME_SALE_WITH_RECEIPT);
            transactionStatus = X3_TRANSACTION_STATUS_FINISHED;
        } finally {
            DAO.setX3TransactionResultInOwnTransactionScope(emf, transactionState, transactionStatus);
        }
        log.debug("Finished processing sale row with id [{}]", sale.getSaleId());
        return true;
    }

    private boolean sendCardPaymentSaleToX3(EntityManager em, com.smilecoms.pos.db.model.Sale sale, Collection<X3TransactionState> states) throws Exception {
        if (!states.isEmpty()) {
            // Transaction has been processed in X3 already - probably by another thread somewhere
            return false;
        }
        Collection<SaleRow> salesRows = DAO.getSalesRowsAndSubRows(em, sale.getSaleId());
        log.debug("Processing Card Payment Sale with id [{}]", sale.getSaleId());
        X3TransactionState transactionState = DAO.createX3TransactionInOwnTransactionScope(emf, sale.getSaleId(), TABLE_SALE, X3_TRANSACTION_TYPE_CARD_PAYMENT_SALE, X3_TRANSACTION_STATUS_IN_PROGRESS, "");
        String transactionStatus = X3_TRANSACTION_STATUS_ERROR;
        try {
            CAdxResultXml invResult = X3Helper.createInvoice(em, emf, transactionState, sale, salesRows, X3Helper.TYPE_CASH_SALE_WITH_RECEIPT);
            transactionStatus = X3_TRANSACTION_STATUS_FINISHED;
        } finally {
            DAO.setX3TransactionResultInOwnTransactionScope(emf, transactionState, transactionStatus);
        }

        log.debug("Finished processing sale row with id [{}]", sale.getSaleId());
        return true;
    }

    private boolean sendCreditFacilityPaymentSaleToX3(EntityManager em, com.smilecoms.pos.db.model.Sale sale, Collection<X3TransactionState> states) throws Exception {
        if (!states.isEmpty()) {
            // Transaction has been processed in X3 already - probably by another thread somewhere
            return false;
        }
        Collection<SaleRow> salesRows = DAO.getSalesRowsAndSubRows(em, sale.getSaleId());
        log.debug("Processing Credit Facility Payment Sale with id [{}]", sale.getSaleId());
        X3TransactionState transactionState = DAO.createX3TransactionInOwnTransactionScope(emf, sale.getSaleId(), TABLE_SALE, X3_TRANSACTION_TYPE_CREDIT_FACILITY_SALE, X3_TRANSACTION_STATUS_IN_PROGRESS, "");
        String transactionStatus = X3_TRANSACTION_STATUS_ERROR;
        try {
            CAdxResultXml invResult = X3Helper.createInvoice(em, emf, transactionState, sale, salesRows, X3Helper.TYPE_CREDIT_SALE_WITH_PAYMENT_PROCESS); //For field 'TYPE_CREDIT_SALE_WITH_PAYMENT_PROCESS' use same as Credit Account
            transactionStatus = X3_TRANSACTION_STATUS_FINISHED;
        } finally {
            DAO.setX3TransactionResultInOwnTransactionScope(emf, transactionState, transactionStatus);
        }

        log.debug("Finished processing sale row with id [{}]", sale.getSaleId());
        return true;
    }

    private boolean sendClearingBureauSaleToX3(EntityManager em, com.smilecoms.pos.db.model.Sale sale, Collection<X3TransactionState> states) throws Exception {
        if (!states.isEmpty()) {
            // Transaction has been processed in X3 already - probably by another thread somewhere
            return false;
        }
        Collection<SaleRow> salesRows = DAO.getSalesRowsAndSubRows(em, sale.getSaleId());
        log.debug("Processing Clearing Bureau Sale with id [{}]", sale.getSaleId());
        X3TransactionState transactionState = DAO.createX3TransactionInOwnTransactionScope(emf, sale.getSaleId(), TABLE_SALE, X3_TRANSACTION_TYPE_CLEARING_BUREAU_SALE, X3_TRANSACTION_STATUS_IN_PROGRESS, "");
        String transactionStatus = X3_TRANSACTION_STATUS_ERROR;
        try {
            CAdxResultXml invResult = X3Helper.createInvoice(em, emf, transactionState, sale, salesRows, X3Helper.TYPE_CREDIT_SALE_WITH_PAYMENT_PROCESS);
            transactionStatus = X3_TRANSACTION_STATUS_FINISHED;
        } finally {
            DAO.setX3TransactionResultInOwnTransactionScope(emf, transactionState, transactionStatus);
        }
        log.debug("Finished processing sale row with id [{}]", sale.getSaleId());
        return true;
    }

    private void sendCreditNoteSaleToX3(EntityManager em, com.smilecoms.pos.db.model.Sale sale, Collection<X3TransactionState> states) throws Exception {
        if (!states.isEmpty()) {
            // Transaction has been processed in X3 already - probably by another thread somewhere
            return;
        }
        Collection<SaleRow> salesRows = DAO.getSalesRowsAndSubRows(em, sale.getSaleId());
        log.debug("Processing Credit Note Sale with id [{}]", sale.getSaleId());
        X3TransactionState transactionState = DAO.createX3TransactionInOwnTransactionScope(emf, sale.getSaleId(), TABLE_SALE, X3_TRANSACTION_TYPE_CREDIT_NOTE_SALE, X3_TRANSACTION_STATUS_IN_PROGRESS, "");
        String transactionStatus = X3_TRANSACTION_STATUS_ERROR;
        try {
            CAdxResultXml invResult = X3Helper.createInvoice(em, emf, transactionState, sale, salesRows, X3Helper.TYPE_CREDIT_NOTE_SALE);
            transactionStatus = X3_TRANSACTION_STATUS_FINISHED;
        } finally {
            DAO.setX3TransactionResultInOwnTransactionScope(emf, transactionState, transactionStatus);
        }

        log.debug("Finished processing sale row with id [{}]", sale.getSaleId());
    }

    private boolean sendCreditAccountSaleToX3(EntityManager em, com.smilecoms.pos.db.model.Sale sale, Collection<X3TransactionState> states) throws Exception {
        if (!states.isEmpty()) {
            // Transaction has been processed in X3 already - probably by another thread somewhere
            return false;
        }
        Collection<SaleRow> salesRows = DAO.getSalesRowsAndSubRows(em, sale.getSaleId());
        log.debug("Processing Credit Account Sale with id [{}]", sale.getSaleId());
        X3TransactionState transactionState = DAO.createX3TransactionInOwnTransactionScope(emf, sale.getSaleId(), TABLE_SALE, X3_TRANSACTION_TYPE_CREDIT_ACCOUNT_SALE, X3_TRANSACTION_STATUS_IN_PROGRESS, "");
        String transactionStatus = X3_TRANSACTION_STATUS_ERROR;
        try {
            CAdxResultXml invResult = X3Helper.createInvoice(em, emf, transactionState, sale, salesRows, X3Helper.TYPE_CREDIT_SALE_WITH_PAYMENT_PROCESS);
            transactionStatus = X3_TRANSACTION_STATUS_FINISHED;
        } finally {
            DAO.setX3TransactionResultInOwnTransactionScope(emf, transactionState, transactionStatus);
        }
        log.debug("Finished processing sale row with id [{}]", sale.getSaleId());
        return true;
    }

    private void sendPendingPaymentSaleToX3(EntityManager em, com.smilecoms.pos.db.model.Sale sale, Collection<X3TransactionState> states) throws Exception {
        if (!states.isEmpty()) {
            // Transaction has been processed in X3 already - probably by another thread somewhere
            return;
        }
        Collection<SaleRow> salesRows = DAO.getSalesRowsAndSubRows(em, sale.getSaleId());
        log.debug("Processing Pending Payment Sale with id [{}]", sale.getSaleId());
        String transactionStatus = X3_TRANSACTION_STATUS_ERROR;

        X3TransactionState transactionState = DAO.createX3TransactionInOwnTransactionScope(emf, sale.getSaleId(), TABLE_SALE, X3_TRANSACTION_TYPE_SALE_PENDING_PAYMENT, X3_TRANSACTION_STATUS_IN_PROGRESS, "");

        int type;
        switch (sale.getPaymentMethod()) {
            case POSManager.PAYMENT_METHOD_CARD_INTEGRATION:
                type = X3Helper.TYPE_CARD_INTEGRATION_WITH_PENDING_PAYMENT;
                break;
            default:
                type = X3Helper.TYPE_EFT_WITH_PENDING_PAYMENT;
        }

        try {
            CAdxResultXml invResult = X3Helper.createInvoice(em, emf, transactionState, sale, salesRows, type);
            transactionStatus = X3_TRANSACTION_STATUS_FINISHED;
        } finally {
            DAO.setX3TransactionResultInOwnTransactionScope(emf, transactionState, transactionStatus);
        }
        log.debug("Finished processing sale row with id [{}]", sale.getSaleId());
    }

    private void sendQuoteToX3(EntityManager em, com.smilecoms.pos.db.model.Sale sale, Collection<X3TransactionState> states) throws Exception {
        if (!states.isEmpty()) {
            // Transaction has been processed in X3 already - probably by another thread somewhere
            return;
        }
        Collection<SaleRow> salesRows = DAO.getSalesRowsAndSubRows(em, sale.getSaleId());
        log.debug("Processing Quote with id [{}]", sale.getSaleId());
        String transactionStatus = X3_TRANSACTION_STATUS_ERROR;

        X3TransactionState transactionState = DAO.createX3TransactionInOwnTransactionScope(emf, sale.getSaleId(), TABLE_SALE, X3_TRANSACTION_TYPE_QUOTE_WITH_RESERVATION, X3_TRANSACTION_STATUS_IN_PROGRESS, "");
        try {
            CAdxResultXml invResult = X3Helper.createQuote(em, emf, transactionState, sale, salesRows, X3Helper.TYPE_QUOTE_WITH_STOCK_RESERVATION);
            transactionStatus = X3_TRANSACTION_STATUS_FINISHED;
        } finally {
            DAO.setX3TransactionResultInOwnTransactionScope(emf, transactionState, transactionStatus);
        }
        log.debug("Finished processing sale row with id [{}]", sale.getSaleId());
    }

    private void sendStockLoanToX3(EntityManager em, com.smilecoms.pos.db.model.Sale sale, Collection<X3TransactionState> states) throws Exception {
        if (!states.isEmpty()) {
            // Transaction has been processed in X3 already - probably by another thread somewhere
            return;
        }
        Collection<SaleRow> salesRows = DAO.getSalesRowsAndSubRows(em, sale.getSaleId());
        log.debug("Processing Stock Loan with id [{}]", sale.getSaleId());
        String transactionStatus = X3_TRANSACTION_STATUS_ERROR;
        X3TransactionState transactionState = DAO.createX3TransactionInOwnTransactionScope(emf, sale.getSaleId(), TABLE_SALE, X3_TRANSACTION_TYPE_STOCK_LOAN, X3_TRANSACTION_STATUS_IN_PROGRESS, "");
        try {
            CAdxResultXml invResult = X3Helper.createLoan(em, emf, transactionState, sale, salesRows, X3Helper.TYPE_LOAN);
            transactionStatus = X3_TRANSACTION_STATUS_FINISHED;
        } finally {
            DAO.setX3TransactionResultInOwnTransactionScope(emf, transactionState, transactionStatus);
        }
        log.debug("Finished processing sale row with id [{}]", sale.getSaleId());
    }

    private void sendCardIntegrationGLEntryToX3(EntityManager em, LazyX3Connection lazyConn, com.smilecoms.pos.db.model.Sale sale, Collection<X3TransactionState> states) throws Exception {
        if (statesHasState(states, X3_TRANSACTION_TYPE_CARD_INTEGRATION_GL)) {
            // Transaction has been processed in X3 already
            return;
        }
        Collection<SaleRow> salesRows = DAO.getSalesRowsAndSubRows(em, sale.getSaleId());
        log.debug("Processing Card Integration Payment GL with id [{}]", sale.getSaleId());
        String transactionStatus = X3_TRANSACTION_STATUS_ERROR;
        X3TransactionState transactionState = DAO.createX3TransactionInOwnTransactionScope(emf, sale.getSaleId(), TABLE_SALE, X3_TRANSACTION_TYPE_CARD_INTEGRATION_GL, X3_TRANSACTION_STATUS_IN_PROGRESS, "");
        try {
            CAdxResultXml invResult = X3Helper.createGLEntryForCardIntegrationSale(em, lazyConn, emf, transactionState, sale, salesRows);
            transactionStatus = X3_TRANSACTION_STATUS_FINISHED;
        } finally {
            DAO.setX3TransactionResultInOwnTransactionScope(emf, transactionState, transactionStatus);
        }
        log.debug("Finished processing sale row with id [{}]", sale.getSaleId());
    }

    private void sendStockLoanCompletionToX3(EntityManager em, com.smilecoms.pos.db.model.Sale sale, Collection<X3TransactionState> states) throws Exception {
        if (statesHasState(states, X3_TRANSACTION_TYPE_STOCK_LOAN_COMPLETION)) {
            // Transaction has been processed in X3 already
            return;
        }

        if (!statesHasState(states, X3_TRANSACTION_TYPE_STOCK_LOAN)) {
            // Loan still needs to be sent to X3
            log.debug("This sale's loan has not been sent to X3 yes. Going to send that now instead of trying to send the completion");
            sendStockLoanToX3(em, sale, states);
            return;
        }

        if (isSaleIntegrationInProgress(states)) {
            return;
        }

        Collection<SaleRow> salesRows = DAO.getSalesRowsAndSubRows(em, sale.getSaleId());
        log.debug("Processing Stock Loan Completion with id [{}]", sale.getSaleId());
        String transactionStatus = X3_TRANSACTION_STATUS_ERROR;
        X3TransactionState transactionState = DAO.createX3TransactionInOwnTransactionScope(emf, sale.getSaleId(), TABLE_SALE, X3_TRANSACTION_TYPE_STOCK_LOAN_COMPLETION, X3_TRANSACTION_STATUS_IN_PROGRESS, "");
        try {
            CAdxResultXml invResult = X3Helper.createLoanCompletion(em, emf, transactionState, sale, salesRows, X3Helper.TYPE_LOAN_RETURN);
            transactionStatus = X3_TRANSACTION_STATUS_FINISHED;
        } finally {
            DAO.setX3TransactionResultInOwnTransactionScope(emf, transactionState, transactionStatus);
        }
        log.debug("Finished processing sale row with id [{}]", sale.getSaleId());
    }

    private void sendPaymentToX3(EntityManager em, com.smilecoms.pos.db.model.Sale sale, Collection<X3TransactionState> states) throws Exception {
        if (statesHasState(states, X3_TRANSACTION_TYPE_PAYMENT)) {
            // Transaction has been processed in X3 already 
            return;
        }

        if (!statesHasState(states, X3_TRANSACTION_TYPE_SALE_PENDING_PAYMENT)) {
            // Invoice still needs to be sent to X3
            log.debug("This paid sale's invoice has not been sent to X3. Invoice must be sent prior to payment being sent. Going to send the invoice now");
            sendPendingPaymentSaleToX3(em, sale, states);
            return;
        }

        if (isSaleIntegrationInProgress(states)) {
            return;
        }

        log.debug("Processing Payment with id [{}]", sale.getSaleId());
        String transactionStatus = X3_TRANSACTION_STATUS_ERROR;
        X3TransactionState transactionState = DAO.createX3TransactionInOwnTransactionScope(emf, sale.getSaleId(), TABLE_SALE, X3_TRANSACTION_TYPE_PAYMENT, X3_TRANSACTION_STATUS_IN_PROGRESS, "");

        int type;
        switch (sale.getPaymentMethod()) {
            case POSManager.PAYMENT_METHOD_CARD_INTEGRATION:
                type = X3Helper.TYPE_CARD_INTEGRATION_WITH_PENDING_PAYMENT;
                break;
            default:
                type = X3Helper.TYPE_PAYMENT;
        }
        try {
            CAdxResultXml invResult = X3Helper.createPayment(em, emf, transactionState, sale, type);
            transactionStatus = X3_TRANSACTION_STATUS_FINISHED;
        } finally {
            DAO.setX3TransactionResultInOwnTransactionScope(emf, transactionState, transactionStatus);
        }
        log.debug("Finished processing sale row with id [{}]", sale.getSaleId());
    }

    private void sendReversalToX3(EntityManager em, LazyX3Connection lazyConn, com.smilecoms.pos.db.model.Sale sale, Collection<X3TransactionState> states) throws Exception {
        if (statesHasState(states, X3_TRANSACTION_TYPE_REVERSAL)) {
            // Transaction has been processed in X3 already
            return;
        }

        if (states.isEmpty()) {
            // Sale's prior transaction has not been sent yet
            log.debug("This sales initial invoice has not been sent to X3 yet. Going to send it now. Its payment method was [{}]", sale.getPaymentMethod());
            switch (sale.getPaymentMethod()) {
                case POSManager.PAYMENT_METHOD_BANK_TRANSFER:
                    this.sendPendingPaymentSaleToX3(em, sale, states);
                    break;
                case POSManager.PAYMENT_METHOD_CASH:
                    this.sendCashSaleToX3(em, sale, states);
                    break;
                case POSManager.PAYMENT_METHOD_CHEQUE:
                    this.sendPendingPaymentSaleToX3(em, sale, states);
                    break;
                case POSManager.PAYMENT_METHOD_CREDIT_FACILITY:
                    this.sendCreditFacilityPaymentSaleToX3(em, sale, states);
                    break;
                case POSManager.PAYMENT_METHOD_CREDIT_ACCOUNT:
                    this.sendCreditAccountSaleToX3(em, sale, states);
                    break;
                case POSManager.PAYMENT_METHOD_CARD_INTEGRATION:
                    this.sendPendingPaymentSaleToX3(em, sale, states);
                    break;
                case POSManager.PAYMENT_METHOD_CARD_PAYMENT:
                    this.sendCardPaymentSaleToX3(em, sale, states);
                    break;
                case POSManager.PAYMENT_METHOD_QUOTE:
                    this.sendQuoteToX3(em, sale, states);
                    break;
                case POSManager.PAYMENT_METHOD_CLEARING_BUREAU:
                    this.sendClearingBureauSaleToX3(em, sale, states);
                    break;
            }
            return;
        }

        if (isSaleIntegrationInProgress(states)) {
            return;
        }

        Collection<SaleRow> salesRows = DAO.getSalesRowsAndSubRows(em, sale.getSaleId());
        log.debug("Processing Invoice Cancellation with id [{}]", sale.getSaleId());
        String transactionStatus = X3_TRANSACTION_STATUS_ERROR;
        X3TransactionState transactionState = DAO.createX3TransactionInOwnTransactionScope(emf, sale.getSaleId(), TABLE_SALE, X3_TRANSACTION_TYPE_REVERSAL, X3_TRANSACTION_STATUS_IN_PROGRESS, "");
        try {
            X3Helper.createCreditNoteForInvoice(em, lazyConn, emf, transactionState, sale, salesRows);
            transactionStatus = X3_TRANSACTION_STATUS_FINISHED;
        } finally {
            DAO.setX3TransactionResultInOwnTransactionScope(emf, transactionState, transactionStatus);
        }
        log.debug("Finished processing sale row with id [{}]", sale.getSaleId());
    }

    private void sendLineReturnToX3(EntityManager em, LazyX3Connection lazyConn, com.smilecoms.pos.db.model.ReturnReplacement dbReturn, Collection<X3TransactionState> states) throws Exception {
        if (!states.isEmpty()) {
            // Transaction has been processed in X3 already - probably by another thread somewhere
            return;
        }

        log.debug("Processing return with id [{}]", dbReturn.getReturnReplacementId());
        String transactionStatus = X3_TRANSACTION_STATUS_ERROR;
        X3TransactionState transactionState = DAO.createX3TransactionInOwnTransactionScope(emf, dbReturn.getReturnReplacementId(), TABLE_RETURN_REPLACEMENT, X3_TRANSACTION_TYPE_LINE_RETURN, X3_TRANSACTION_STATUS_IN_PROGRESS, "");
        try {
            X3Helper.createLineReturn(em, emf, transactionState, dbReturn, X3Helper.TYPE_SALE_RETURN);
            transactionStatus = X3_TRANSACTION_STATUS_FINISHED;
        } finally {
            DAO.setX3TransactionResultInOwnTransactionScope(emf, transactionState, transactionStatus);
        }
        log.debug("Finished processing line return with id [{}]", dbReturn.getReturnReplacementId());
    }

    private void sendLineReplacementToX3(EntityManager em, LazyX3Connection lazyConn, com.smilecoms.pos.db.model.ReturnReplacement dbReturn, Collection<X3TransactionState> states) throws Exception {
        if (!states.isEmpty()) {
            // Transaction has been processed in X3 already - probably by another thread somewhere
            return;
        }

        log.debug("Processing line replacement with id [{}]", dbReturn.getReturnReplacementId());
        String transactionStatus = X3_TRANSACTION_STATUS_ERROR;
        X3TransactionState transactionState = DAO.createX3TransactionInOwnTransactionScope(emf, dbReturn.getReturnReplacementId(), TABLE_RETURN_REPLACEMENT, X3_TRANSACTION_TYPE_LINE_RETURN, X3_TRANSACTION_STATUS_IN_PROGRESS, "");
        try {
            X3Helper.createLineReplacement(em, emf, transactionState, dbReturn, X3Helper.TYPE_SALE_RETURN);
            transactionStatus = X3_TRANSACTION_STATUS_FINISHED;
        } finally {
            DAO.setX3TransactionResultInOwnTransactionScope(emf, transactionState, transactionStatus);
        }
        log.debug("Finished processing line return with id [{}]", dbReturn.getReturnReplacementId());
    }

    private void sendReturnToX3(EntityManager em, LazyX3Connection lazyConn, com.smilecoms.pos.db.model.SaleReturn dbReturn, Collection<X3TransactionState> states) throws Exception {
        if (!states.isEmpty()) {
            // Transaction has been processed in X3 already - probably by another thread somewhere
            return;
        }

        Collection<SaleReturnRow> returnRows = DAO.getReturnRows(em, dbReturn.getSaleReturnId());
        log.debug("Processing return with id [{}]", dbReturn.getSaleReturnId());
        String transactionStatus = X3_TRANSACTION_STATUS_ERROR;
        X3TransactionState transactionState = DAO.createX3TransactionInOwnTransactionScope(emf, dbReturn.getSaleReturnId(), TABLE_SALE_RETURN, X3_TRANSACTION_TYPE_RETURN, X3_TRANSACTION_STATUS_IN_PROGRESS, "");
        try {
            X3Helper.createCreditNoteForInvoice(em, lazyConn, emf, transactionState, dbReturn, returnRows);
            transactionStatus = X3_TRANSACTION_STATUS_FINISHED;
        } finally {
            DAO.setX3TransactionResultInOwnTransactionScope(emf, transactionState, transactionStatus);
        }
        log.debug("Finished processing re ruturn with id [{}]", dbReturn.getSaleReturnId());
    }

    private void sendPSCTransactionGLEntryToX3(EntityManager em, LazyX3Connection lazyConn, com.smilecoms.pos.db.model.AccountHistory dbPSCTransaction, Collection<X3TransactionState> states) throws Exception {
        if (!states.isEmpty()) {
            // Transaction has been processed in X3 already - probably by another thread somewhere
            return;
        }
        log.debug("Processing PSC Transaction GL entry with id [{}]", dbPSCTransaction.getId());
        String transactionStatus = X3_TRANSACTION_STATUS_ERROR;
        X3TransactionState transactionState = DAO.createX3TransactionInOwnTransactionScope(emf, dbPSCTransaction.getId().intValue(), TABLE_PSC_TRANSACTION, X3_TRANSACTION_TYPE_PSC, X3_TRANSACTION_STATUS_IN_PROGRESS, "");
        try {
            // X3Helper.createGLEntryForCashIn(em, lazyConn, emf, transactionState, dbCashIn, cashInRows);
            X3Helper.createGLEntryForPSCTransaction(em, lazyConn, emf, transactionState, dbPSCTransaction);
            transactionStatus = X3_TRANSACTION_STATUS_FINISHED;
        } finally {
            DAO.setX3TransactionResultInOwnTransactionScope(emf, transactionState, transactionStatus);
        }
        log.debug("Finished processing PSC transaction GL entry with id [{}]", dbPSCTransaction.getId());
    }

    private void sendCashInGLEntryToX3(EntityManager em, LazyX3Connection lazyConn, com.smilecoms.pos.db.model.CashIn dbCashIn, Collection<X3TransactionState> states) throws Exception {
        if (!states.isEmpty()) {
            // Transaction has been processed in X3 already - probably by another thread somewhere
            return;
        }
        Collection<CashInRow> cashInRows = DAO.getCashInRows(em, dbCashIn.getCashInId());
        log.debug("Processing CashIn GL entry with id [{}]", dbCashIn.getCashInId());
        String transactionStatus = X3_TRANSACTION_STATUS_ERROR;
        X3TransactionState transactionState = DAO.createX3TransactionInOwnTransactionScope(emf, dbCashIn.getCashInId(), TABLE_CASHIN, X3_TRANSACTION_TYPE_CASHIN, X3_TRANSACTION_STATUS_IN_PROGRESS, "");
        try {
            X3Helper.createGLEntryForCashIn(em, lazyConn, emf, transactionState, dbCashIn, cashInRows);
            transactionStatus = X3_TRANSACTION_STATUS_FINISHED;
        } finally {
            DAO.setX3TransactionResultInOwnTransactionScope(emf, transactionState, transactionStatus);
        }
        log.debug("Finished processing CashIn GL entry with id [{}]", dbCashIn.getCashInId());
    }

    private void sendGLEntryToX3(LazyX3Connection lazyConn, EntityManager em, X3TransactionState state) throws Exception {
        log.debug("Received GL Entry with status [{}]", state.getStatus());
        if (!state.getStatus().equals(X3_TRANSACTION_STATUS_PENDING)) {
            // Transaction has been processed in X3 already - probably by another thread somewhere
            return;
        }
        log.debug("Processing GL Entry with data [{}]", state.getExtraInfo());
        String transactionStatus = X3_TRANSACTION_STATUS_ERROR;
        try {
            X3Helper.createGLEntry(lazyConn, em, emf, state);
            transactionStatus = X3_TRANSACTION_STATUS_FINISHED;
        } finally {
            state.setEndDateTime(new Date());
            state.setStatus(transactionStatus);
            em.persist(state);
            em.flush();
        }

        log.debug("Finished processing GL entry with data [{}]", state.getExtraInfo());
    }

    private boolean statesHasState(Collection<X3TransactionState> states, String type) {
        for (X3TransactionState state : states) {
            if (state.getX3TransactionStatePK().getTransactionType().equals(type)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSaleIntegrationInProgress(Collection<X3TransactionState> states) {
        boolean stillInProgress = false;
        for (X3TransactionState state : states) {
            if (state.getStatus().equals(X3_TRANSACTION_STATUS_IN_PROGRESS)) {
                stillInProgress = true;
                log.debug("This sale is currently being sent to X3");
                break;
            }
        }
        return stillInProgress;
    }

    private void populateOfflineSnapshot(LazyX3Connection lazyConn) {

        EntityManager em = null;
        String readQuery;
        String insertQuery;
        PreparedStatement psX3 = null;
        PreparedStatement psX3Kits = null;
        Query q;
        ResultSet rs;
        int cnt;
        long start;
        Lock lock = HazelcastHelper.getGlobalLock("POSOfflineSnapshot");
        log.debug("Getting global lock");
        if (!lock.tryLock()) {
            log.debug("Another VM is busy in populateOfflineSnapshot. Exiting");
            return;
        }
        log.debug("Got global lock");
        try {
            long overallStart = System.currentTimeMillis();
            em = JPAUtils.getEM(emf);
            int totalRecords = 0;

            log.debug("Taking offline snapshot of inventory");
            start = System.currentTimeMillis();
            JPAUtils.beginTransaction(em);
            readQuery = BaseUtils.getProperty("env.pos.x3.inventory.snapshot.query");
            insertQuery = BaseUtils.getProperty("env.pos.x3.inventory.snapshot.insert");
            psX3 = lazyConn.prepareStatement(readQuery);
            rs = psX3.executeQuery();
            log.debug("X3 query has finished");
            q = em.createNativeQuery("delete from x3_offline_inventory");
            q.executeUpdate();
            log.debug("Truncation has finished");
            q = em.createNativeQuery(insertQuery);
            cnt = 0;
            while (rs.next()) {
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    q.setParameter(i, rs.getObject(i));
                }
                q.executeUpdate();
                cnt++;
                totalRecords++;
            }
            JPAUtils.commitTransaction(em);
            log.debug("Added [{}] offline inventory rows in [{}]ms", cnt, System.currentTimeMillis() - start);
            start = System.currentTimeMillis();

            if (BaseUtils.getBooleanProperty("env.pos.ifrs15.enabled", false)) {
                log.debug("Synching x3_offline_inventory bundle prices with unit_credit_specification as per IRRS requirement");
                JPAUtils.beginTransaction(em);
                readQuery = BaseUtils.getProperty("env.pos.ifrs15.x3.synch.offline.snapshot.bundles.prices.with.ucs.query", "update x3_offline_inventory XOI \n"
                        + "         join unit_credit_specification UCS on (XOI.ITEM_NUMBER=UCS.ITEM_NUMBER AND XOI.CURRENCY=? AND UCS.CONFIGURATION NOT LIKE '%IsABaseForPricing=true%') \n"
                        + "                set XOI.PRICE =round((UCS.PRICE_CENTS/100)/(select 1 + PROPERTY_VALUE/100 from property where PROPERTY_NAME = 'env.sales.tax.percent')) \n"
                        + "                WHERE XOI.PRICE != round((UCS.PRICE_CENTS/100)/(select 1 + PROPERTY_VALUE/100 from property where PROPERTY_NAME = 'env.sales.tax.percent'));");
                q = em.createNativeQuery(readQuery);
                q.setParameter(1, BaseUtils.getProperty("env.currency.official.symbol"));
                cnt = q.executeUpdate();
                JPAUtils.commitTransaction(em);
                log.debug("Synched [{}] bundle prices in offline snapshot in [{}]ms", cnt, System.currentTimeMillis() - start);
            }

            log.debug("Taking offline snapshot of dimensions");
            start = System.currentTimeMillis();
            readQuery = BaseUtils.getProperty("env.pos.x3.dimension.snapshot.query");
            insertQuery = BaseUtils.getProperty("env.pos.x3.dimension.snapshot.insert");
            psX3 = lazyConn.prepareStatement(readQuery);
            rs = psX3.executeQuery();
            log.debug("X3 query has finished");
            JPAUtils.beginTransaction(em);
            q = em.createNativeQuery("delete from x3_offline_dimensions");
            q.executeUpdate();
            log.debug("Truncation has finished");
            q = em.createNativeQuery(insertQuery);
            cnt = 0;
            while (rs.next()) {
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    q.setParameter(i, rs.getObject(i));
                }
                q.executeUpdate();
                cnt++;
                totalRecords++;
            }
            JPAUtils.commitTransaction(em);
            log.debug("Added [{}] offline dimension rows in [{}]ms", cnt, System.currentTimeMillis() - start);

            log.debug("Taking offline snapshot of subitem data");
            start = System.currentTimeMillis();
            JPAUtils.beginTransaction(em);

            //Get all KITS
            if (BaseUtils.getBooleanProperty("env.pos.ifrs15.enabled", false)) {
                log.debug("IFSR is enabled - populating x3 offline subitems using the pro-rata method.");

                //Get all KITS items from price list
                readQuery = BaseUtils.getProperty("env.pos.x3.kits.snapshot.query");
                psX3Kits = lazyConn.prepareStatement(readQuery);
                rs = psX3Kits.executeQuery();
                // ITM.ITMREF_0 as ITEMNO, TX1.TEXTE_0 as DESCRIPTION, ISNULL(SPL.PRI_0,0) as PRICECENTSEXCL, SPL.CUR_0 as CURRENCY,1 as STOCKLEVEL, '' AS LOCATION, DTY_0 as BOX_SIZE
                List<InventoryItem> x3KitItems = new ArrayList<>();
                while (rs.next()) {
                    InventoryItem kitItem = new InventoryItem();
                    kitItem.setItemNumber(rs.getString(1).trim());
                    kitItem.setDescription(rs.getString(2).trim());
                    kitItem.setPriceInCentsExcl(rs.getDouble(3) * 100);
                    kitItem.setCurrency(rs.getString(4).trim());
                    x3KitItems.add(kitItem);
                }
                log.debug("Retrieved [{}] KITs items from X3,  going to do pro-rata now.", x3KitItems.size());

                readQuery = BaseUtils.getProperty("env.pos.x3.subitem.snapshot.query.ifrs15"); // Pull subitems from the BOMS and including their standalone price form the price lists.
                psX3 = lazyConn.prepareStatement(readQuery);
                rs = psX3.executeQuery();
                log.debug("X3 query has finished");
                q = em.createNativeQuery("delete from x3_offline_subitems");
                q.executeUpdate();
                log.debug("Truncation has finished");
                cnt = 0;

                List<X3OfflineSubitems> x3OfflineSubitems = new ArrayList<>();
                while (rs.next()) {
                    X3OfflineSubitems subitem = new X3OfflineSubitems();
                    subitem.setKit(rs.getString(1));
                    subitem.setComponentItemNumber(rs.getString(2));
                    subitem.setDescription(rs.getString(3));
                    subitem.setPrice((rs.getString(4) == null) ? null : BigDecimal.valueOf(rs.getDouble(4)));
                    subitem.setCurrency(rs.getString(5));
                    subitem.setCategory(rs.getString(6));
                    x3OfflineSubitems.add(subitem);
                }

                double totalSubitemsStandalonePrices = 0.00;
                //Clone it ...
                List<X3OfflineSubitems> cleanX3OfflineSubitems = new ArrayList(x3OfflineSubitems);
                //Clean-up the list by applying some rules from finance
                //The rule is, all subitems must have a non-zero amount - the only item allowed to have 0 price is BUNPB or BUNKPB
                for (X3OfflineSubitems subItem : x3OfflineSubitems) {
                    if ((subItem.getPrice() == null)
                            || (subItem.getPrice().doubleValue() <= 0 && !(subItem.getComponentItemNumber().startsWith("BUNPB") || subItem.getComponentItemNumber().startsWith("BUNKPB")))) {
                        // Remove all subItems of this kit from the list;
                        if (subItem.getPrice() == null) {
                            log.debug("KIT [{}] is being removed from the x3_offline_subitems because its subitem [{}] is not on the pricelist, price [{}], currency [{}].",
                                    new Object[]{subItem.getKit(), subItem.getComponentItemNumber(), subItem.getPrice(), subItem.getCurrency()});
                        } else {
                            log.debug("KIT [{}] is being removed from the x3_offline_subitems because one of its non-BUNPB/BUNKPB subitem [{}] does not have a price [{}], currency [{}].",
                                    new Object[]{subItem.getKit(), subItem.getComponentItemNumber(), subItem.getPrice(), subItem.getCurrency()});
                        }

                        // Remove the KIT from the list too.
                        int i = 0;
                        for (InventoryItem kit : x3KitItems) {
                            if (kit.getItemNumber().equals(subItem.getKit())
                                    && kit.getCurrency().equals(subItem.getCurrency())) {
                                x3KitItems.remove(i);
                                break;
                            }
                            i++;
                        }
                    }
                }

                // Only add the items that are related to the remaining KITS
                List<X3OfflineSubitems> finalListOfX3OfflineSubitems = new ArrayList();
                for (InventoryItem kit : x3KitItems) {
                    for (X3OfflineSubitems cpSubItem : cleanX3OfflineSubitems) {
                        if (kit.getItemNumber().equals(cpSubItem.getKit())
                                && kit.getCurrency().equals(cpSubItem.getCurrency())) {
                            finalListOfX3OfflineSubitems.add(cpSubItem);
                        }
                    }
                }

                //Calculate pro-rata prices for all kits/sub-items
                for (InventoryItem kit : x3KitItems) {
                    //Get the total of standalone prices
                    totalSubitemsStandalonePrices = 0.00;
                    for (X3OfflineSubitems subItem : finalListOfX3OfflineSubitems) {
                        if (subItem.getKit().equals(kit.getItemNumber()) && subItem.getCurrency().equals(kit.getCurrency())) { //This is the subitem of this kit.
                            //TODO -  check if sub-item is a bundle and get the price from its equivalent item. 
                            totalSubitemsStandalonePrices += subItem.getPrice().doubleValue();
                        }
                    }

                    // Update the pro-rata for all subitems of this KIT ...
                    if (totalSubitemsStandalonePrices <= 0) { // In case a kit was included and it does not have subitems in the BOMs
                        log.debug("The total standalone price for subitems of kit [{}] is zero. This kit is  excluded from the offline kits.", kit.getItemNumber());
                        continue;
                    }

                    // Update pro-rata here.
                    for (X3OfflineSubitems subItem : finalListOfX3OfflineSubitems) {
                        if (subItem.getKit().equals(kit.getItemNumber()) && subItem.getCurrency().equals(kit.getCurrency())) { //Set pro-rata price
                            try {
                                double dblProRataPrice = ((kit.getPriceInCentsExcl() / 100) / totalSubitemsStandalonePrices) * subItem.getPrice().doubleValue();
                                BigDecimal proRataPrice = new BigDecimal(dblProRataPrice);
                                subItem.setPrice(proRataPrice);
                            } catch (Exception ex) {
                                log.error("Error while calculating pro-rata for kit [{}] sub-item [{}] using fields, kit.getPriceInCentsExcl = {},  totalSubitemsStandalonePricesCents = {} "
                                        + ", subItem.getPrice = {}", new Object[]{kit.getItemNumber(), subItem.getComponentItemNumber(), kit.getPriceInCentsExcl(), totalSubitemsStandalonePrices, subItem.getPrice()});
                                throw ex;
                            }
                        }
                    }

                }
                //Persist all subitems.
                for (X3OfflineSubitems subItem : finalListOfX3OfflineSubitems) {
                    em.persist(subItem);
                    em.flush();
                }
                log.debug("Added [{}] offline subitem (using pro-rata method) rows in [{}]ms", finalListOfX3OfflineSubitems.size(), System.currentTimeMillis() - start);

            } else { //Use the legacy offline subitems.
                readQuery = BaseUtils.getProperty("env.pos.x3.subitem.snapshot.query");
                insertQuery = BaseUtils.getProperty("env.pos.x3.subitem.snapshot.insert");
                psX3 = lazyConn.prepareStatement(readQuery);
                rs = psX3.executeQuery();
                log.debug("X3 query has finished");
                q = em.createNativeQuery("delete from x3_offline_subitems");
                q.executeUpdate();
                log.debug("Truncation has finished");
                q = em.createNativeQuery(insertQuery);
                cnt = 0;
                while (rs.next()) {
                    // BOM.ITMREF_0 AS KIT, BOM.CPNITMREF_0 AS COMPONENT_ITEM_NUMBER, TX1.TEXTE_0 AS [DESCRIPTION],BOM.XCOMPPRI_0 AS PRICE, BOMH.XCPY_0 as CURRENCY
                    // Use legacy
                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                        q.setParameter(i, rs.getObject(i));
                    }
                    q.executeUpdate();
                    cnt++;
                    totalRecords++;
                }
                log.debug("Added [{}] offline subitem rows in [{}]ms", cnt, System.currentTimeMillis() - start);

            }

            JPAUtils.commitTransaction(em);
            PlatformEventManager.createEvent("POS", "OfflineSnapshot", "", "Offline records written in snapshot=" + totalRecords + " from server " + BaseUtils.getHostNameFromKernel() + " taking " + (System.currentTimeMillis() - overallStart) + "ms");
            
            if(BaseUtils.getBooleanProperty("env.do.kits.update", false)) {
                updateKitsDataFromSnapshot(lazyConn);
            }

        } catch (Exception e) {
            log.debug("Error getting snapshot [{}]", Utils.getStackTrace(e));
            JPAUtils.rollbackTransaction(em);
            if (!e.toString().contains("Lock wait timeout") && !e.toString().contains("Error Code: 1032") && !e.toString().contains("Deadlock found")) {
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "Error populating offline snapshot: " + e.toString());
            }
        } finally {
            try {
                if (psX3 != null) {
                    psX3.close();
                }
            } catch (Exception ex) {
            }

            try {
                if (psX3Kits != null) {
                    psX3Kits.close();
                }
            } catch (Exception ex) {
            }
            JPAUtils.closeEM(em);
            lock.unlock();
        }
    }
    
    
    //////////////////////
    private void updateKitsDataFromSnapshot(LazyX3Connection lazyConn) {
        
        EntityManager em = null;
        Query q1, q2, itemsDescriptionsQuery,superDealerQuery ;
        
        try {
        
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            
            log.debug("Removing old values");
            q1 = em.createNativeQuery("delete from SmileDB.superdealer_kits");
            q1.executeUpdate();            
            q2 = em.createNativeQuery("delete from SmileDB.pos_kits_itemnumber_description");            
            q2.executeUpdate();
            log.debug("Done Cleaning old Data!");
            
            log.debug("inserting new values...");
            itemsDescriptionsQuery = em.createNativeQuery("insert into SmileDB.pos_kits_itemnumber_description select OI.ITEM_NUMBER, OI.DESCRIPTION, 100*round(OI.PRICE*( 1 + (select property_value from property where PROPERTY_NAME='env.sales.tax.percent')/100)) as PRICE_INCL from x3_offline_inventory OI JOIN (select distinct OS.KIT from x3_offline_subitems OS where OS.CATEGORY = 'NKITS') OSI ON OI.ITEM_NUMBER = OSI.KIT where PRICE > 0 and CURRENCY = 'NGN' ORDER BY OI.ITEM_NUMBER ASC");
            itemsDescriptionsQuery.executeUpdate();
            superDealerQuery = em.createNativeQuery("insert into SmileDB.superdealer_kits select OI.ITEM_NUMBER as ITEM_NUMBER, OI.DESCRIPTION as DESCRIPTION, 100*round(OI.PRICE*( 1 + (select property_value from property where PROPERTY_NAME='env.sales.tax.percent')/100)) as PRICE_INCL from x3_offline_inventory OI JOIN (select distinct OS.KIT from x3_offline_subitems OS where OS.CATEGORY = 'NSKIT') OSI ON OI.ITEM_NUMBER = OSI.KIT where PRICE > 0 and CURRENCY = 'NGN' ORDER BY OI.ITEM_NUMBER ASC");
            superDealerQuery.executeUpdate();
            log.debug("Done Updating Kits!");
            
            JPAUtils.commitTransaction(em);
            
        } catch (Exception e) {
            log.debug("Error getting snapshot data from kits [{}]", Utils.getStackTrace(e));
            JPAUtils.rollbackTransaction(em);
            if (!e.toString().contains("Lock wait timeout") && !e.toString().contains("Error Code: 1032") && !e.toString().contains("Deadlock found")) {
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "Error populating offline kits frm snapshot: " + e.toString());
            }
        } finally {
            JPAUtils.closeEM(em);            
        }
    }
}
