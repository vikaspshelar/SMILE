/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos;

import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.pos.db.model.Sale;
import com.smilecoms.pos.db.model.TraState;
import com.smilecoms.pos.db.op.DAO;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author lesiba,Abhilash
 */
@Singleton
@Startup
@Local({BaseListener.class})
public class TRADaemon implements BaseListener {

    private static final Logger log = LoggerFactory.getLogger(TRADaemon.class);
    private static final String SEND_INVOICES_FOR_SIGNING = "sd";
    private static final String EMAIL_SIGNED_INVOICES = "esi";
    public static final String TRA_STATUS_PENDING = "PE";
    public static final String TRA_STATUS_FINISHED = "FI";
    public static final String TRA_STATUS_IN_PROGRESS = "IP";
    public static final String TRA_STATUS_STORED_SIGNED = "SS";
    private static ScheduledFuture runner1 = null;
    private static ScheduledFuture runner2 = null;
    private static ScheduledFuture runner3 = null;
    /*
     * OLD Status lifecycle:
     * 1) PE - Being sent to the signing server over FTP
     * 2) IP - Being signed
     * 3) SS - Signed and stored to the DB
     * 4) FI - Email sent
     */
    /*
     * NEW Status lifecycle:
     * 1) PE - Before REST call to ESD device is executed 
     * 2) IP - After REST call to ESD device is executed
     * 3) SS - Signed and stored to the DB
     * 4) FI - Email sent
     */
    private EntityManagerFactory emf = null;

    private void trigger(String callBackParam) {
        try {
            log.debug("TRADaemon triggered with callback param [{}]", callBackParam);
            if (!BaseUtils.getBooleanProperty("env.tra.must.run", false)) {
                log.debug("TRA Daemon not allowed to run");
                return;
            }
            if (!TRAHelper.ready()) {
                log.debug("TRAHelper not ready to process requests");
                return;
            }
            try {
                if (callBackParam.equalsIgnoreCase(SEND_INVOICES_FOR_SIGNING)) {
                    for (String paymentType : BaseUtils.getPropertyAsSet("env.tra.invoice.payment.types.to.be.signed")) {
                        sendUnsignedSaleInvoicesToESD(paymentType);
                    }
                } else if (callBackParam.equalsIgnoreCase(EMAIL_SIGNED_INVOICES)) {
                    emailSignedInvoices();
                }
            } catch (Exception e) {
                log.warn("[{}]", e.getMessage());
            }
        } catch (Exception e) {
            log.warn("Error in a runner: ", e);
        }
    }

    @Override
    public void propsAreReadyTrigger() {
        runner1 = Async.scheduleAtFixedRate(new SmileBaseRunnable("POS.TRADaemon") {
            @Override
            public void run() {
                trigger(SEND_INVOICES_FOR_SIGNING);
            }
        }, 30000, 30 * 1000, 40 * 1000);
        runner2 = Async.scheduleAtFixedRate(new SmileBaseRunnable("POS.TRADaemon") {
            @Override
            public void run() {
                trigger(EMAIL_SIGNED_INVOICES);
            }
        }, 45000, 30 * 1000, 40 * 1000);
        BaseUtils.registerForPropsChanges(this);

    }

    @Override
    public void propsHaveChangedTrigger() {
    }

    @PostConstruct
    public void startUp() {
        log.debug("Registering props");
        BaseUtils.registerForPropsAvailability(this);
        emf = JPAUtils.getEMF("POSPU_RL");
    }
    private static boolean mustRun = true;

    @PreDestroy
    public void shutDown() {
        mustRun = false;
        BaseUtils.deregisterForPropsAvailability(this);
        Async.cancel(runner1);
        Async.cancel(runner2);
        BaseUtils.deregisterForPropsChanges(this);
        JPAUtils.closeEMF(emf);
    }

    public void emailSignedInvoices() {
        log.debug("Emailing signed invoices...");
        EntityManager em = null;
        try {
            em = JPAUtils.getEM(emf);
            Collection<Sale> sales = DAO.getSalesByTRAStatus(em, TRA_STATUS_STORED_SIGNED, BaseUtils.getIntProperty("env.tra.invoice.batch.size", 10));
            if (sales.size() > 0) {
                for (Sale s : sales) {
                    if (!mustRun) {
                        log.debug("Exiting loop as we are shutting down");
                        break;
                    }
                    log.debug("Emailing Customer Invoice Id [{}] for [{}]", s.getSaleId(), s.getRecipientName());
                    boolean invoiceSent = TRAHelper.sendInvoice(new POSManager().getXMLSale(em, s, "SALE_LINES_DOCUMENTS"));
                    if (invoiceSent) {
                        JPAUtils.beginTransaction(em);
                        TraState state = DAO.getLockedTraStateBySaleId(em, s.getSaleId());
                        DAO.setTRAStateInOwnTransactionScope(em, state, TRA_STATUS_FINISHED);
                        if (em.getTransaction() != null && em.getTransaction().isActive() && !em.getTransaction().getRollbackOnly()) {
                            JPAUtils.commitTransaction(em);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("There was an error sending customer invoices : [{}]", e.getMessage());
            JPAUtils.rollbackTransaction(em);
        } finally {
            JPAUtils.closeEM(em);
        }
    }
   
    private void sendUnsignedSaleInvoicesToESD(String paymentType) {
        log.debug("Sending unsigned sales invoices of payment type [{}] for signing...", paymentType);
        EntityManager em = null;
        
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            List<com.smilecoms.pos.db.model.Sale> unprocessedSales = (List<com.smilecoms.pos.db.model.Sale>) DAO.getSalesByPaymentMethodWithNoTraState(
                    em, paymentType, BaseUtils.getIntProperty("env.tra.invoice.batch.size", 50));
            JPAUtils.commitTransaction(em);
            
            log.debug("There are [{}] sales invoices of payment type [{}] to be sent to TRA for signing", unprocessedSales.size(), paymentType);
            for (com.smilecoms.pos.db.model.Sale s : unprocessedSales) {
                if (!mustRun) {
                    log.debug("Exiting loop as we are shutting down");
                    break;
                }
                log.debug("Sale ID : [{}]", s.getSaleId());
                try {
                    JPAUtils.beginTransaction(em);
                    TRAHelper.sendSaleToESDForSigning(em, s);
                    JPAUtils.commitTransaction(em);
                } catch (Exception e) {
                    log.warn("Error occured whilst sending sales invoice.");
                } 
//                finally {
//                    JPAUtils.commitTransaction(em);
//                }
            }
        } catch (Exception e) {
            log.warn("Error sending sales : Issue [{}]", e);
        } finally {
            JPAUtils.closeEM(em);
        }
    }
}
