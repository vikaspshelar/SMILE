package com.smilecoms.pos.contract;

import com.smilecoms.commons.base.lifecycle.BaseListener;

import org.slf4j.*;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;
import com.smilecoms.commons.sca.SCABusinessError;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.util.ExceptionManager;
import com.smilecoms.commons.util.IMAPUtils;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.pos.POSManager;
import com.smilecoms.pos.db.model.Contract;
import com.smilecoms.pos.db.model.Sale;
import com.smilecoms.pos.db.model.SaleRow;
import com.smilecoms.pos.db.op.DAO;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

@Singleton
@Startup
@Local({BaseListener.class})
public class ContractFulfilmentDaemon implements BaseListener {

    private static final Logger log = LoggerFactory.getLogger(ContractFulfilmentDaemon.class);
    private static EntityManagerFactory emf = null;
    private EntityManager em;
    private static ScheduledFuture runner1 = null;

    @PostConstruct
    public void startUp() {
        emf = JPAUtils.getEMF("POSPU_RL");
        BaseUtils.registerForPropsAvailability(this);
    }

    @Override
    public void propsAreReadyTrigger() {
        log.warn("Contract fulfilment daemon is starting up as properties are ready");
        BaseUtils.deregisterForPropsAvailability(this);
        BaseUtils.registerForPropsChanges(this);
        runner1 = Async.scheduleAtFixedRate(new SmileBaseRunnable("POS.contractFulfilment") {
            @Override
            public void run() {
                trigger();
            }
        }, 60000, 3 * 60000, 5 * 60000);
    }

    @PreDestroy
    public void shutDown() {
        BaseUtils.deregisterForPropsChanges(this);
        Async.cancel(runner1);
        JPAUtils.closeEMF(emf);
    }

    @Override
    public void propsHaveChangedTrigger() {
    }

    private void trigger() {
        try {
            log.debug("Contract fulfilment daemon triggered to do contract fulfilment processing");
            em = JPAUtils.getEM(emf);
            try {
                if (!BaseUtils.getBooleanProperty("env.contract.daemon.mustrun", true)) {
                    log.debug("Contract daemon is off");
                    return;
                }
                doProcessing();
            } catch (Exception e) {
                log.warn("Error in contract fulfilment daemon [{}]", e.toString());
                log.warn("Error: ", e);
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "Error in contract fulfilment daemon: " + e.toString());
            } finally {
                JPAUtils.closeEM(em);
            }
        } catch (Exception e) {
            log.warn("Error: ", e);
        }
    }

    private int doProcessing() throws Exception {
        int cnt = 0;
        try {
            JPAUtils.beginTransaction(em);
            long start = System.currentTimeMillis();
            List<Integer> list = DAO.getOldOrInProgressContractFulfilmentSaleIds(em);
            Collections.shuffle(list); // Lower chance of clashes of multiple threads
            JPAUtils.commitTransaction(em);

            for (int saleId : list) {
                try {
                    cnt++;
                    log.debug("Looking at contract sale id [{}]", saleId);
                    JPAUtils.beginTransaction(em);
                    Sale contractSale = DAO.getLockedSale(em, saleId);
                    Date lastExecuted = DAO.getLastContractSaleFulfilmentDate(em, contractSale.getSaleId());
                    // Do whatever needs to be done for the current status and last executed date
                    performNextAction(em, lastExecuted, contractSale);
                    contractSale.setFulfilmentLastCheckDateTime(new Date());
                    em.persist(contractSale);
                    JPAUtils.commitTransactionAndClear(em);
                    log.debug("Finished looking at contract sale id [{}]", saleId);
                } catch (javax.persistence.PessimisticLockException ple) {
                    JPAUtils.rollbackTransaction(em);
                    log.debug("PessimisticLockException in contract fulfilment daemon for a sale [{}]. Another thread must be processing it", ple.toString());
                } catch (Exception e) {
                    JPAUtils.rollbackTransaction(em);
                    log.warn("Error in contract fulfilment daemon for a sale [{}] [{}]", saleId, e.toString());
                    log.warn("Error: ", e);
                    BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "Error in contract fulfilment daemon for sale Id: " + saleId + " : " + e.toString());
                }
            } // End loop

            long end = System.currentTimeMillis();
            log.debug("Finished contract fulfilment run. Run took [{}]ms to process [{}] records", end - start, cnt);
        } catch (Exception e) {
            JPAUtils.rollbackTransaction(em);
            throw e;
        }
        return cnt;
    }

    private void performNextAction(EntityManager em, Date lastExecuted, Sale contractSale) throws Exception {
        /* 
         Fulfilment Schedule info layout is as follows (can be any combination of these. Each is an "and" condition):
         HOD=X - Indicates it must run on a hour of the day where 1 is Sunday. E.g. HOD=16
         DOW=X - Indicates it must run on a certain day of the week where 1 is Sunday. E.g. DOW=4
         DOM=X - Indicates it must run on a certain day of the month. E.g. DOM=20
         DAYSGAP=X - Indicates it must run every X days
         AB=X - Indicates it must run if the account balance is less than X cents. E.g.AB=500000
         */

        log.debug("This contract sale last had a fulfilment done at [{}] and was last checked to see if it needed a run at [{}]", lastExecuted, contractSale.getFulfilmentLastCheckDateTime());

        Contract contract = DAO.getContract(em, contractSale.getContractId());
        Date nowDt = new Date();
        if (nowDt.before(contract.getContractStartDateTime())) {
            log.debug("Contract has not started yet");
            contractSale.setFulfilmentPausedTillDateTime(contract.getContractStartDateTime());
            return;
        }

        if (nowDt.after(contract.getContractEndDateTime())) {
            log.debug("This contract has ended. Reversing the contract sale");
            new POSManager().reverseSale(em, contractSale.getSaleId());
            return;
        }

        // If the last execution was today then dont proceed any further
        if (Utils.isDateToday(lastExecuted)) {
            log.debug("This schedule was last executed today so not proceeding any further");
            return;
        }

        // So by now we know this schedule was last executed yesterday or before and is within a valid contract period
        switch (contractSale.getStatus()) {

            case POSManager.PAYMENT_STATUS_CONTRACT_WAITING:

                log.debug("This schedule is in WT (Waiting) status and has not executed today. Lets see if it must move to PG (Pending Go)");
                Calendar now = Calendar.getInstance();
                boolean mustProcess = true;
                boolean foundConfig = false;
                boolean forced = false;
                for (String line : Utils.getSetFromCRDelimitedString(contractSale.getFulfilmentScheduleInfo())) {
                    if (line.startsWith("HOD=")) {
                        int hod = Integer.parseInt(line.substring(4));
                        if (now.get(Calendar.HOUR_OF_DAY) >= hod) {
                            log.debug("Now is greater than or equal to the correct hour of day");
                        } else {
                            log.debug("Now is not the correct hour of day");
                            mustProcess = false;
                        }
                        foundConfig = true;
                    } else if (line.startsWith("DOW=")) {
                        int dow = Integer.parseInt(line.substring(4));
                        if (now.get(Calendar.DAY_OF_WEEK) == dow) {
                            log.debug("Today is the correct day of week");
                        } else {
                            log.debug("Today is not the correct day of week");
                            mustProcess = false;
                        }
                        foundConfig = true;
                    } else if (line.startsWith("DOM=")) {
                        int dom = Integer.parseInt(line.substring(4));
                        if (now.get(Calendar.DAY_OF_MONTH) == dom) {
                            log.debug("Today is the correct day of the month");
                        } else {
                            log.debug("Today is not the correct day of the month");
                            mustProcess = false;
                        }
                        foundConfig = true;
                    } else if (line.startsWith("DAYSGAP=")) {
                        int gap = Integer.parseInt(line.substring(8));
                        int daysSinceSaleCreated = Utils.getDaysBetweenDates(contractSale.getSaleDateTime(), new Date());
                        if (daysSinceSaleCreated == 0) {
                            log.debug("Sale was done today. Must go ahead");
                        } else {
                            int modulus = daysSinceSaleCreated % gap;
                            if (modulus == 0) {
                                log.debug("Its been [{}] days since the sale was created and the gap should be [{}]. So we must go ahead", daysSinceSaleCreated, gap);
                            } else {
                                log.debug("Its been [{}] days since the sale was created and the gap should be [{}]. So we must not go ahead", daysSinceSaleCreated, gap);
                                mustProcess = false;
                            }
                        }
                        foundConfig = true;
                    } else if (line.startsWith("AB=") && contractSale.getRecipientAccountId() > 0) {
                        long ab = Long.parseLong(line.substring(3));
                        double accountBalCents = DAO.getAccountBalance(em, contractSale.getRecipientAccountId());
                        if (accountBalCents < ab) {
                            log.debug("Account balance is [{}] which is less than [{}]", accountBalCents, ab);
                        } else {
                            log.debug("Account balance is [{}] which is more than [{}]", accountBalCents, ab);
                            mustProcess = false;
                        }
                        foundConfig = true;
                    } else if (line.equals("FORCETODAY")) {
                        log.warn("Sale must be scheduled now");
                        forced = true;
                        // Remove the forced indicator
                        Set<String> config = Utils.getSetFromCRDelimitedString(contractSale.getFulfilmentScheduleInfo());
                        config.remove("FORCETODAY");
                        contractSale.setFulfilmentScheduleInfo(Utils.makeCRDelimitedStringFromList(config));
                    } else if (!line.isEmpty()) {
                        log.warn("Got an unknown schedule config element: [{}]", line);
                        mustProcess = false;
                    }
                }

                if ((mustProcess && foundConfig) || forced) {
                    log.debug("This contract sale must be processed in terms of timing and/or account balance");
                    sendSaleOptOutMessage(em, contractSale);
                    Calendar pauseTill = Calendar.getInstance();
                    pauseTill.add(Calendar.MINUTE, BaseUtils.getIntProperty("env.pos.contract.schedule.optout.wait.minutes", 30));
                    contractSale.setFulfilmentPausedTillDateTime(pauseTill.getTime());
                    contractSale.setStatus(POSManager.PAYMENT_STATUS_CONTRACT_PENDING_GO);
                } else {
                    log.debug("This schedule must not be processed any further for now");
                }
                break;

            case POSManager.PAYMENT_STATUS_CONTRACT_PENDING_GO:

                log.debug("This contract sale is in status PG (Pending Go). But we must have gone past the pause date of [{}]", contractSale.getFulfilmentPausedTillDateTime());
                if (contractSale.getFulfilmentPausedTillDateTime() == null || new Date().after(contractSale.getFulfilmentPausedTillDateTime())) {
                    // just checked to be sure
                    contractSale.setStatus("GO");
                } else {
                    log.debug("This contract sale is in PG status and yet the pause date is not up yet. Weird that this is even possible... ignoring");
                }
                break;

            case POSManager.PAYMENT_STATUS_CONTRACT_GO:

                log.debug("This contract is in status GO. Lets create a new sale for it");
                processContractSale(em, contractSale);
                contractSale.setStatus(POSManager.PAYMENT_STATUS_CONTRACT_WAITING);
                break;
        }

    }

    private void sendSaleOptOutMessage(EntityManager em, Sale contractSale) {
        log.debug("In sendSaleOptOutMessage for contract sale id [{}]", contractSale.getSaleId());
    }

    private void processContractSale(EntityManager em, Sale contractSale) throws Exception {
        log.debug("In processContractSale for contract sale id [{}]", contractSale.getSaleId());
        Contract contract = DAO.getContract(em, contractSale.getContractId());
        com.smilecoms.commons.sca.Sale scaSale = makeNewSCASaleFromDBContractSale(em, contractSale);
        scaSale.setPaymentMethod(contract.getPaymentMethod());
        if (scaSale.getPaymentMethod().equals("Airtime")) {
            scaSale.setSalesPersonAccountId(contract.getAccountId());
            if (scaSale.getSalesPersonAccountId() <= 0) {
                throw new Exception("Airtime contract sale has no account id to charge");
            }
            log.debug("This is an airtime payment method. Sales person account is being set to the contract account [{}]", contract.getAccountId());
        }
        scaSale.setCreditAccountNumber(contract.getCreditAccountNumber());
        try {
            SCAWrapper.getAdminInstance().processSale(scaSale);
        } catch (SCABusinessError sbe) {
            try {
                String body = "A contract sale with sale id " + contractSale.getSaleId() + " got an error being processed on its schedule. Error info is as follows: " + sbe.getErrorDesc();
                IMAPUtils.sendEmail(BaseUtils.getProperty("env.smtp.customercomms.from", "admin@smilecoms.com"), BaseUtils.getProperty("env.sales.invoice.bcc.email.address"), "Contract Sale Failure - " + contractSale.getSaleId(), body);
                new ExceptionManager(log).reportError(sbe);
            } catch (Exception e1) {
                log.warn("Error: ", e1);
            }
        } catch (Exception e) {
            new ExceptionManager(log).reportError(e);
        }
        log.debug("Called SCA");
    }

    public static com.smilecoms.commons.sca.Sale makeNewSCASaleFromDBContractSale(EntityManager em, com.smilecoms.pos.db.model.Sale dbSale) throws Exception {
        com.smilecoms.commons.sca.Sale xmlSale = new com.smilecoms.commons.sca.Sale();
        xmlSale.setAmountTenderedCents(dbSale.getAmountTenderedCents().doubleValue());
        xmlSale.setChangeCents(dbSale.getChangeCents().doubleValue());
        // Payment method picked up from contract sale
        xmlSale.setRecipientAccountId(dbSale.getRecipientAccountId());
        xmlSale.setRecipientCustomerId(dbSale.getRecipientCustomerId());
        xmlSale.setRecipientOrganisationId(dbSale.getRecipientOrganisationId());
        xmlSale.setSaleLocation(dbSale.getSaleLocation());
        xmlSale.setSaleTotalCentsExcl(dbSale.getSaleTotalCentsExcl().doubleValue());
        xmlSale.setSaleTotalCentsIncl(dbSale.getSaleTotalCentsIncl().doubleValue());
        xmlSale.setSaleTotalDiscountOnExclCents(dbSale.getSaleTotalDiscountOnExclCents().doubleValue());
        xmlSale.setSaleTotalDiscountOnInclCents(dbSale.getSaleTotalDiscountOnInclCents().doubleValue());
        xmlSale.setSaleTotalTaxCents(dbSale.getSaleTotalTaxCents().doubleValue());
        xmlSale.setSalesPersonAccountId(dbSale.getSalesPersonAccountId());
        xmlSale.setSalesPersonCustomerId(dbSale.getSalesPersonCustomerId());
        xmlSale.setTenderedCurrency(dbSale.getTenderedCurrency());
        xmlSale.setTenderedCurrencyExchangeRate(dbSale.getTenderedCurrencyExchangeRate().doubleValue());
        xmlSale.setTillId(dbSale.getTillId());
        xmlSale.setChannel(dbSale.getChannel());
        xmlSale.setOrganisationChannel(dbSale.getOrganisationChannel());
        xmlSale.setWarehouseId(dbSale.getWarehouseId());
        xmlSale.setPromotionCode(dbSale.getPromotionCode());
        xmlSale.setPurchaseOrderData(dbSale.getPurchaseOrderData());
        xmlSale.setTaxExempt(dbSale.getTaxExempt().equals("Y"));
        xmlSale.setCreditAccountNumber(dbSale.getCreditAccountNumber());
        xmlSale.setWithholdingTaxCents(dbSale.getWithholdingTaxCents().doubleValue());
        xmlSale.setTotalLessWithholdingTaxCents(xmlSale.getSaleTotalCentsIncl() - xmlSale.getWithholdingTaxCents());
        xmlSale.setExtraInfo(dbSale.getExtraInfo() == null ? "" : dbSale.getExtraInfo());
        xmlSale.setContractSaleId(dbSale.getSaleId());
        xmlSale.setContractId(dbSale.getContractId());
        for (SaleRow dbSaleRow : DAO.getSalesRows(em, dbSale.getSaleId(), 0)) {
            xmlSale.getSaleLines().add(makeNewSCASaleLineFromDBSaleLine(em, dbSaleRow));
        }
        xmlSale.setCashInDate(Utils.getDateAsXMLGregorianCalendar(DAO.getCashInDate(em, dbSale.getSaleId())));

        return xmlSale;
    }

    public static com.smilecoms.commons.sca.SaleLine makeNewSCASaleLineFromDBSaleLine(EntityManager em, SaleRow dbSaleRow) {
        log.debug("In getXMLSaleLine for sale row Id [{}]", dbSaleRow.getSaleRowId());
        com.smilecoms.commons.sca.SaleLine line = new com.smilecoms.commons.sca.SaleLine();
        line.setInventoryItem(new com.smilecoms.commons.sca.InventoryItem());
        line.getInventoryItem().setDescription(dbSaleRow.getDescription());
        line.getInventoryItem().setItemNumber(dbSaleRow.getItemNumber());
        line.getInventoryItem().setPriceInCentsExcl(dbSaleRow.getUnitPriceCentsExcl().doubleValue());
        line.getInventoryItem().setPriceInCentsIncl(dbSaleRow.getUnitPriceCentsIncl().doubleValue());
        line.getInventoryItem().setSerialNumber(dbSaleRow.getSerialNumber());
        line.getInventoryItem().setStockLevel(dbSaleRow.getQuantity());
        line.getInventoryItem().setWarehouseId(dbSaleRow.getWarehouseId());
        line.setLineNumber(dbSaleRow.getLineNumber());
        line.setComment(dbSaleRow.getComment());
        line.setLineTotalCentsExcl(dbSaleRow.getTotalCentsExcl().doubleValue());
        line.setLineTotalCentsIncl(dbSaleRow.getTotalCentsIncl().doubleValue());
        line.setLineTotalDiscountOnExclCents(dbSaleRow.getTotalDiscountOnExclCents().doubleValue());
        line.setLineTotalDiscountOnInclCents(dbSaleRow.getTotalDiscountOnInclCents().doubleValue());
        line.setQuantity(dbSaleRow.getQuantity());
        line.setProvisioningData(dbSaleRow.getProvisioningData());
        // Recurring function. Add sub rows to this row
        Collection<SaleRow> subRows = DAO.getSalesRows(em, dbSaleRow.getSaleId(), dbSaleRow.getSaleRowId());
        for (SaleRow subRow : subRows) {
            line.getSubSaleLines().add(makeNewSCASaleLineFromDBSaleLine(em, subRow));
        }
        return line;
    }
}
