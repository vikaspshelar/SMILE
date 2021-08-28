package com.smilecoms.pos.saleconsolidation;

import com.smilecoms.commons.base.lifecycle.BaseListener;

import org.slf4j.*;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;
import com.smilecoms.commons.sca.InventoryItem;
import com.smilecoms.commons.sca.SCAContext;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.Sale;
import com.smilecoms.commons.sca.SaleLine;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.pos.POSManager;
import com.smilecoms.pos.X3Helper;
import com.smilecoms.pos.db.op.DAO;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
public class SaleConsolidationDaemon implements BaseListener {

    private static final Logger log = LoggerFactory.getLogger(SaleConsolidationDaemon.class);
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
        log.warn("Sale consolidation daemon is starting up as properties are ready");
        BaseUtils.deregisterForPropsAvailability(this);
        BaseUtils.registerForPropsChanges(this);
        runner1 = Async.scheduleAtFixedRate(new SmileBaseRunnable("POS.SaleConsolidation") {
            @Override
            public void run() {
                trigger();
            }
        }, 10000, 2 * 60000, 4 * 60000);
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
        log.debug("Sale consolidation daemon triggered to do consolidation processing");
        em = JPAUtils.getEM(emf);
        try {
            if (!BaseUtils.getBooleanProperty("env.pos.sale.consolidation.daemon.mustrun", false)) {
                log.debug("Sale consolidation daemon is off");
                return;
            }
            doProcessing();
        } catch (Exception e) {
            log.warn("Error in sale consolidation daemon [{}]", e.toString());
            log.warn("Error: ", e);
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "Error in sale consolidation daemon: " + e.toString());
        } finally {
            JPAUtils.closeEM(em);
        }
    }

    private int doProcessing() throws Exception {
        int cnt = 0;
        try {
            JPAUtils.beginTransaction(em);
            long start = System.currentTimeMillis();
            Utils.getGlobalLock(em, getClass().getName());
            log.debug("Getting list of unit credit instances that have a negative sale line id");
            List<Object[]> results = DAO.getYesterdaysPendingConsolidatedUnitCreditData(em);
            // Results will have:
            // Unit credit instance id
            // ITEM_NUMBER
            // Amount Cents

            if (results.isEmpty()) {
                log.debug("No consolidated sale to process");
                return cnt;
            }
            cnt = results.size();
            // Item number and sum
            Map<String, Double> consolidationValues = new HashMap<>();
            // Item number and uc id list
            Map<String, List<Integer>> consolidationUCIds = new HashMap<>();
            for (Object[] row : results) {
                int ucId = (Integer) row[0];
                String itemNumber = (String) row[1];
                double amountCents = ((BigDecimal) row[2]).doubleValue();
                log.debug("Got unit credit instance [{}] Item Number [{}] Value [{}]c", new Object[]{ucId, itemNumber, amountCents});
                Double val = consolidationValues.get(itemNumber);
                if (val == null) {
                    val = amountCents;
                } else {
                    val += amountCents;
                }
                consolidationValues.put(itemNumber, val);

                List<Integer> ucIds = consolidationUCIds.get(itemNumber);
                if (ucIds == null) {
                    ucIds = new ArrayList<>();
                    consolidationUCIds.put(itemNumber, ucIds);
                }
                ucIds.add(ucId);
            }

            for (String itemNumber : consolidationUCIds.keySet()) {
                log.debug("Updating [{}] unit credits with sale row id -2 in order to lock them before making the sale", consolidationUCIds.get(itemNumber).size());
                DAO.updateUnitCreditsWithSaleLineId(em, -2, consolidationUCIds.get(itemNumber), -1);
            }
            
            Sale sale = makeSale(consolidationValues);
            log.debug("Sale made with sale id [{}]", sale.getSaleId());
            for (String itemNumber : consolidationUCIds.keySet()) {
                int saleLineId = getSaleLineId(sale, itemNumber);
                if (saleLineId == 0 && consolidationValues.get(itemNumber) > 0) {
                    throw new Exception("We have an impossible situation here");
                }
                log.debug("Updating [{}] unit credits with sale row id [{}]", consolidationUCIds.get(itemNumber).size(), saleLineId);
                DAO.updateUnitCreditsWithSaleLineId(em, saleLineId, consolidationUCIds.get(itemNumber), -2);
            }
            
            long end = System.currentTimeMillis();
            JPAUtils.commitTransaction(em);
            log.debug("Finished sale consolidation run. Run took [{}]ms to process [{}] unit credits", end - start, results.size());
            
        } catch (Exception e) {
            JPAUtils.rollbackTransaction(em);
            throw e;
        }
        return cnt;
    }

    private Sale makeSale(Map<String, Double> itemNumbers) throws Exception {
        Sale sale = new Sale();
        sale.setSCAContext(new SCAContext());
        sale.getSCAContext().setOriginatingIP("0.0.0.0");
        sale.getSCAContext().setTxId(Utils.getUUID());
        sale.setChannel(BaseUtils.getProperty("env.pos.consolidated.sales.channel"));
        sale.setPaymentMethod(POSManager.PAYMENT_METHOD_CREDIT_ACCOUNT);
        sale.setSalesPersonAccountId(1000000005);
        sale.setRecipientCustomerId(1);
        sale.setCreditAccountNumber(X3Helper.props.getProperty("ConsolidatedGiftCustomerCode"));
        sale.setSalesPersonCustomerId(1);
        sale.setWarehouseId("");
        Date yesterday = Utils.getPastDate(Calendar.DATE, 1);
        yesterday = Utils.getEndOfDay(yesterday);
        sale.setSaleDate(Utils.getDateAsXMLGregorianCalendar(yesterday));
        sale.setUniqueId("ConsolidatedSale-" + Utils.getDateAsString(yesterday, "yyyy/MM/dd"));
        for (Entry<String, Double> entry : itemNumbers.entrySet()) {
            String itemNumber = entry.getKey();
            double cents = entry.getValue();
            SaleLine line = new SaleLine();
            line.setInventoryItem(new InventoryItem());
            line.getInventoryItem().setItemNumber(itemNumber);
            line.getInventoryItem().setSerialNumber("");
            line.setQuantity((long) Utils.round(cents/100, 0));
            sale.getSaleLines().add(line);
            log.debug("Sale line added for item number [{}] and quantity [{}]", itemNumber, line.getQuantity());
        }
        return SCAWrapper.getAdminInstance().processSale(sale);
    }

    private int getSaleLineId(Sale sale, String itemNumber) throws Exception {
        for ( SaleLine row : sale.getSaleLines()) {
            if (row.getInventoryItem().getItemNumber().equals(itemNumber)) {
                return row.getLineId();
            }
        }
        log.debug("The sale has no line for item number [{}]. returning 0", itemNumber);
        return 0;
    }

}
