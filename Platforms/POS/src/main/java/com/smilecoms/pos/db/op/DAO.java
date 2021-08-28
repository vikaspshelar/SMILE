/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos.db.op;

import com.adonix.www.WSS.CAdxMessage;
import com.adonix.www.WSS.CAdxResultXml;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.HashUtils;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.pos.POSManager;
import com.smilecoms.pos.X3InterfaceDaemon;
import com.smilecoms.pos.db.model.*;
import com.smilecoms.xml.schema.pos.CashInData;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class DAO {

    private static final Logger log = LoggerFactory.getLogger(DAO.class);

    public static Sale createSale(EntityManager em, com.smilecoms.xml.schema.pos.Sale xmlSale) throws Exception {

        try {
            Sale sale = new Sale();
            sale.setAmountTenderedCents(new BigDecimal(xmlSale.getAmountTenderedCents()));
            sale.setChangeCents(new BigDecimal(xmlSale.getChangeCents()));
            sale.setGpsCoordinates(xmlSale.getGpsCoordinates());
            sale.setExtTxid(xmlSale.getExtTxId() == null ? "" : xmlSale.getExtTxId());
            sale.setUniqueId((xmlSale.getUniqueId() != null && !xmlSale.getUniqueId().isEmpty()) ? xmlSale.getUniqueId() : null);
            sale.setOrganisationName(xmlSale.getOrganisationName() == null ? "" : xmlSale.getOrganisationName());
            sale.setPaymentMethod(xmlSale.getPaymentMethod());
            if (xmlSale.getPaymentTransactionData() != null) {
                sale.setPaymentTransactionData(xmlSale.getPaymentTransactionData().isEmpty() ? null : xmlSale.getPaymentTransactionData());
            }
            sale.setRecipientAccountId(xmlSale.getRecipientAccountId());
            sale.setRecipientCustomerId(xmlSale.getRecipientCustomerId());
            sale.setRecipientName(xmlSale.getRecipientName() == null ? "" : xmlSale.getRecipientName());
            sale.setRecipientOrganisationId(xmlSale.getRecipientOrganisationId());
            sale.setRecipientPhoneNumber(xmlSale.getRecipientPhoneNumber() == null ? "" : xmlSale.getRecipientPhoneNumber());
            sale.setSaleDateTime(Utils.getJavaDate(xmlSale.getSaleDate()));
            sale.setSaleLocation(xmlSale.getSaleLocation() == null ? "" : xmlSale.getSaleLocation());
            sale.setSaleTotalCentsExcl(new BigDecimal(xmlSale.getSaleTotalCentsExcl()));
            sale.setSaleTotalCentsIncl(new BigDecimal(xmlSale.getSaleTotalCentsIncl()));
            sale.setSaleTotalDiscountOnExclCents(new BigDecimal(xmlSale.getSaleTotalDiscountOnExclCents()));
            sale.setSaleTotalDiscountOnInclCents(new BigDecimal(xmlSale.getSaleTotalDiscountOnInclCents()));
            sale.setSaleTotalTaxCents(new BigDecimal(xmlSale.getSaleTotalTaxCents()));
            sale.setSalesPersonAccountId(xmlSale.getSalesPersonAccountId());
            sale.setSalesPersonCustomerId(xmlSale.getSalesPersonCustomerId());
            sale.setStatus(xmlSale.getStatus());
            sale.setTenderedCurrency(xmlSale.getTenderedCurrency());
            sale.setTenderedCurrencyExchangeRate(new BigDecimal(xmlSale.getTenderedCurrencyExchangeRate()));
            sale.setTillId(xmlSale.getTillId() == null ? "" : xmlSale.getTillId());
            sale.setChannel(xmlSale.getChannel());
            sale.setPromotionCode(xmlSale.getPromotionCode());
            sale.setOrganisationChannel(xmlSale.getOrganisationChannel() == null ? "" : xmlSale.getOrganisationChannel());
            sale.setWarehouseId(xmlSale.getWarehouseId());
            sale.setPurchaseOrderData(xmlSale.getPurchaseOrderData());
            sale.setTaxExempt(xmlSale.isTaxExempt() ? "Y" : "N");
            sale.setCreditAccountNumber(xmlSale.getCreditAccountNumber());
            sale.setExtraInfo(xmlSale.getExtraInfo() == null ? "" : xmlSale.getExtraInfo());
            sale.setExpiryDateTime(Utils.getJavaDate(xmlSale.getExpiryDate()));
            sale.setWithholdingTaxCents(new BigDecimal(xmlSale.getWithholdingTaxCents()));
            sale.setLastModified(new Date());
            sale.setFulfilmentScheduleInfo(xmlSale.getFulfilmentScheduleInfo());
            sale.setContractSaleId(xmlSale.getContractSaleId());
            sale.setContractId(xmlSale.getContractId());
            sale.setDeliveryFeeCents(new BigDecimal(xmlSale.getDeliveryFeeCents()));
            sale.setTransactionFeeCents(new BigDecimal(xmlSale.getTransactionFeeCents()));
            sale.setTransactionFeeModel(xmlSale.getTransactionFeeModel());
            sale.setDeliveryFeeModel(xmlSale.getDeliveryFeeModel());
            sale.setPaymentGatewayCode(xmlSale.getPaymentGatewayCode());
            sale.setLandingURL(xmlSale.getLandingURL() == null ? null : xmlSale.getLandingURL());
            sale.setCallbackURL(xmlSale.getCallbackURL() == null ? null : xmlSale.getCallbackURL());
            em.persist(sale);
            em.flush();
            em.refresh(sale);
            return sale;
        } catch (Exception e) {
            if (e.toString().contains("Duplicate entry")) {
                throw new Exception("Duplicate unique id -- " + xmlSale.getUniqueId());
            }
            throw e;
        }
    }

    public static Sale updateSale(EntityManager em, com.smilecoms.xml.schema.pos.Sale xmlSale) {
        Sale sale = em.find(Sale.class, xmlSale.getSaleId());
        sale.setAmountTenderedCents(new BigDecimal(xmlSale.getAmountTenderedCents()));
        sale.setChangeCents(new BigDecimal(xmlSale.getChangeCents()));
        sale.setExtTxid(xmlSale.getExtTxId());
        sale.setUniqueId((xmlSale.getUniqueId() != null && !xmlSale.getUniqueId().isEmpty()) ? xmlSale.getUniqueId() : null);
        sale.setOrganisationName(xmlSale.getOrganisationName() == null ? "" : xmlSale.getOrganisationName());
        sale.setPaymentMethod(xmlSale.getPaymentMethod());
        sale.setPaymentTransactionData(xmlSale.getPaymentTransactionData().isEmpty() ? null : xmlSale.getPaymentTransactionData());
        sale.setRecipientAccountId(xmlSale.getRecipientAccountId());
        sale.setRecipientCustomerId(xmlSale.getRecipientCustomerId());
        sale.setRecipientName(xmlSale.getRecipientName() == null ? "" : xmlSale.getRecipientName());
        sale.setRecipientOrganisationId(xmlSale.getRecipientOrganisationId());
        sale.setRecipientPhoneNumber(xmlSale.getRecipientPhoneNumber() == null ? "" : xmlSale.getRecipientPhoneNumber());
        sale.setSaleDateTime(Utils.getJavaDate(xmlSale.getSaleDate()));
        sale.setSaleLocation(xmlSale.getSaleLocation() == null ? "" : xmlSale.getSaleLocation());
        sale.setSaleTotalCentsExcl(new BigDecimal(xmlSale.getSaleTotalCentsExcl()));
        sale.setSaleTotalCentsIncl(new BigDecimal(xmlSale.getSaleTotalCentsIncl()));
        sale.setSaleTotalDiscountOnExclCents(new BigDecimal(xmlSale.getSaleTotalDiscountOnExclCents()));
        sale.setSaleTotalDiscountOnInclCents(new BigDecimal(xmlSale.getSaleTotalDiscountOnInclCents()));
        sale.setSaleTotalTaxCents(new BigDecimal(xmlSale.getSaleTotalTaxCents()));
        sale.setSalesPersonAccountId(xmlSale.getSalesPersonAccountId());
        sale.setSalesPersonCustomerId(xmlSale.getSalesPersonCustomerId());
        sale.setStatus(xmlSale.getStatus());
        sale.setTenderedCurrency(xmlSale.getTenderedCurrency());
        sale.setTenderedCurrencyExchangeRate(new BigDecimal(xmlSale.getTenderedCurrencyExchangeRate()));
        sale.setTillId(xmlSale.getTillId() == null ? "" : xmlSale.getTillId());
        sale.setChannel(xmlSale.getChannel());
        sale.setPromotionCode(xmlSale.getPromotionCode());
        sale.setWarehouseId(xmlSale.getWarehouseId());
        sale.setPurchaseOrderData(xmlSale.getPurchaseOrderData());
        sale.setCreditAccountNumber(xmlSale.getCreditAccountNumber());
        sale.setContractSaleId(xmlSale.getContractSaleId());
        sale.setExtraInfo(xmlSale.getExtraInfo());
        sale.setExpiryDateTime(Utils.getJavaDate(xmlSale.getExpiryDate()));
        sale.setLastModified(new Date());
        em.flush();
        return sale;
    }

    public static SaleRow createSaleRow(EntityManager em, Sale dbSale, com.smilecoms.xml.schema.pos.SaleLine xmlSaleLine, int parentRowId, int heldByOrganisationId) {
        SaleRow saleRow = new SaleRow();
        saleRow.setDescription(xmlSaleLine.getInventoryItem().getDescription());
        saleRow.setItemNumber(xmlSaleLine.getInventoryItem().getItemNumber());
        saleRow.setLineNumber(xmlSaleLine.getLineNumber());
        saleRow.setQuantity(xmlSaleLine.getQuantity());
        saleRow.setComment(xmlSaleLine.getComment() == null || xmlSaleLine.getComment().isEmpty() ? null : xmlSaleLine.getComment());
        saleRow.setSaleId(dbSale.getSaleId());
        saleRow.setSerialNumber(xmlSaleLine.getInventoryItem().getSerialNumber());
        saleRow.setTotalCentsExcl(new BigDecimal(xmlSaleLine.getLineTotalCentsExcl()));
        saleRow.setTotalCentsIncl(new BigDecimal(xmlSaleLine.getLineTotalCentsIncl()));
        saleRow.setTotalDiscountOnExclCents(new BigDecimal(xmlSaleLine.getLineTotalDiscountOnExclCents()));
        saleRow.setTotalDiscountOnInclCents(new BigDecimal(xmlSaleLine.getLineTotalDiscountOnInclCents()));
        saleRow.setUnitPriceCentsExcl(new BigDecimal(xmlSaleLine.getInventoryItem().getPriceInCentsExcl()));
        saleRow.setUnitPriceCentsIncl(new BigDecimal(xmlSaleLine.getInventoryItem().getPriceInCentsIncl()));
        saleRow.setWarehouseId(xmlSaleLine.getInventoryItem().getWarehouseId());
        saleRow.setParentSaleRowId(parentRowId);
        saleRow.setProvisioningData(xmlSaleLine.getProvisioningData() == null ? "" : xmlSaleLine.getProvisioningData());
        saleRow.setHeldByOrganisationId(heldByOrganisationId);
        em.persist(saleRow);
        em.flush();
        em.refresh(saleRow);
        return saleRow;
    }

    public static CashIn createCashIn(EntityManager em, double cashReceiptedInCents, double cashRequiredInCents,
            int salesAdministratorCustomerId, int salesPersonCustomerId, String txid, String status, String cashInType) {
        CashIn cashIn = new CashIn();
        cashIn.setCashInDateTime(new Date());
        cashIn.setCashReceiptedInCents(BigDecimal.valueOf(cashReceiptedInCents));
        cashIn.setCashRequiredInCents(BigDecimal.valueOf(cashRequiredInCents));
        cashIn.setSalesAdministratorCustomerId(salesAdministratorCustomerId);
        cashIn.setSalesPersonCustomerId(salesPersonCustomerId);
        cashIn.setExtTxId(txid);
        cashIn.setStatus(status);
        cashIn.setCashInType(cashInType);
        em.persist(cashIn);
        em.flush();
        em.refresh(cashIn);
        return cashIn;
    }

    public static CashIn modifyCashIn(EntityManager em, CashInData cashInData) throws Exception {

        com.smilecoms.pos.db.model.CashIn cashIn = em.find(com.smilecoms.pos.db.model.CashIn.class, cashInData.getCashInId());
        cashIn.setStatus(cashInData.getStatus());
        cashIn.setCashInDateTime(new Date());
        cashIn.setSalesAdministratorCustomerId(cashInData.getSalesAdministratorCustomerId());
        cashIn.setBankName(cashInData.getBankName());

        try {
            em.persist(cashIn);
            em.flush();
        } catch (javax.persistence.PersistenceException e) {
            log.warn("Error persisting cashIn: {}", e.toString());
            throw new Exception("Duplicate contract");
        }

        return cashIn;
    }

    public static void createCashInRow(EntityManager em, int cashInId, int saleId) {
        CashInRow cashInRow = new CashInRow();
        cashInRow.setCashInId(cashInId);
        cashInRow.setSaleId(saleId);
        em.persist(cashInRow);
    }

    public static Date getLatestCashInDateAndTimeOfSalesDoneBeforeToday(EntityManager em, int salesPersonCustomerId) {
        try {
            Query q = em.createNativeQuery("select cast(max(cash_in_date_time) as DATETIME) from cash_in CI join cash_in_row CiR on (CI.cash_in_id = CiR.cash_in_id and CI.sales_person_customer_id=?) "
                    + " join  sale S on (S.sale_id = CiR.sale_id  and S.sale_date_time < curdate());");
            q.setParameter(1, salesPersonCustomerId);
            return (Date) q.getSingleResult();
        } catch (Exception ex) {
            log.error("Error while retrieving latest cashin time for user [{}], error [{}]", salesPersonCustomerId, ex);
            return null;
        }
    }

    public static Collection<Sale> getNonCashedInPaidCashOrCardPaymentSales(EntityManager em, int salesPersonCustomerId) {
        Query q = em.createNativeQuery(
                "select sale.* from sale left join cash_in_row on (sale.sale_id=cash_in_row.sale_id) "
                + "left join cash_in on (cash_in_row.cash_in_id = cash_in.cash_in_id) "
                + "where sale.SALES_PERSON_CUSTOMER_ID=? and sale.STATUS=? "
                + "and (sale.PAYMENT_METHOD=? or sale.PAYMENT_METHOD=?) and sale.SALE_DATE_TIME > now() - interval ? day "
                + "and sale.SALE_TOTAL_CENTS_INCL > 0 and (cash_in_row.sale_id is null or cash_in.status='BDP') "
                + "order by sale.SALE_DATE_TIME", Sale.class);
        /*
        Query q = em.createNativeQuery(
                "select sale.* from sale left join cash_in_row on (sale.sale_id=cash_in_row.sale_id) "
                + "where sale.SALES_PERSON_CUSTOMER_ID=? and sale.STATUS=? "
                + "and (sale.PAYMENT_METHOD=? or sale.PAYMENT_METHOD=?) "
                + "and sale.SALE_DATE_TIME > now() - interval ? day "
                + "and sale.SALE_TOTAL_CENTS_INCL > 0 "
                + "and cash_in_row.sale_id is null "
                + "order by sale.SALE_DATE_TIME", Sale.class); */

        q.setParameter(1, salesPersonCustomerId);
        q.setParameter(2, POSManager.PAYMENT_STATUS_PAID);
        q.setParameter(3, POSManager.PAYMENT_METHOD_CASH);
        q.setParameter(4, POSManager.PAYMENT_METHOD_CARD_PAYMENT);
        q.setParameter(5, BaseUtils.getIntProperty("env.pos.cashin.lookback.days", 30));
        return q.getResultList();
    }

    @Deprecated
    public static Collection<Sale> getSalesOnPendingBankDepositCashIn(EntityManager em, int salesPersonCustomerId) {
        Query q = em.createNativeQuery(
                "select sale.* from sale left join cash_in_row on (sale.sale_id=cash_in_row.sale_id) "
                + "join cash_in on (cash_in_row.cash_in_id=cash_in.cash_in_id) "
                + "where sale.SALES_PERSON_CUSTOMER_ID=? and sale.STATUS=? "
                + "and sale.SALE_DATE_TIME > now() - interval ? day "
                + "and sale.SALE_TOTAL_CENTS_INCL > 0 "
                + "and cash_in.status=? "
                + "order by sale.SALE_DATE_TIME", Sale.class);
        q.setParameter(1, salesPersonCustomerId);
        q.setParameter(2, POSManager.PAYMENT_STATUS_PAID);
        q.setParameter(3, BaseUtils.getIntProperty("env.pos.cashin.lookback.days", 30));
        q.setParameter(4, "BDP");//bank deposit pending
        return q.getResultList();
    }

    public static Collection<SaleRow> getSalesRows(EntityManager em, int saleId, int parentSaleRowId) {
        Query q = em.createNativeQuery("select * from sale_row where SALE_ID=? and PARENT_SALE_ROW_ID=? order by line_number, sale_row_id", SaleRow.class);
        q.setParameter(1, saleId);
        q.setParameter(2, parentSaleRowId);
        return q.getResultList();
    }

    public static Collection<SaleRow> getSalesRowsAndSubRows(EntityManager em, int saleId) {
        Query q = em.createNativeQuery("select * from sale_row where SALE_ID=? order by line_number, sale_row_id", SaleRow.class);
        q.setParameter(1, saleId);
        return q.getResultList();
    }

    public static Sale getSale(EntityManager em, int id) {
        Query q = em.createNativeQuery("select * from sale where SALE_ID=?", Sale.class);
        q.setParameter(1, id);
        return (Sale) q.getSingleResult();
    }

    public static Sale getSaleByLineId(EntityManager em, int lineId) {
        Query q = em.createNativeQuery("select S.* from sale S join sale_row R on R.sale_id=S.sale_id and R.SALE_ROW_ID=?", Sale.class);
        q.setParameter(1, lineId);
        return (Sale) q.getSingleResult();
    }

    public static Sale getLockedSale(EntityManager em, int id) {
        Query q = em.createNativeQuery("select * from sale where SALE_ID=? FOR UPDATE", Sale.class);
        q.setParameter(1, id);
        return (Sale) q.getSingleResult();
    }

    public static SaleReturn getReturn(EntityManager em, int id) {
        Query q = em.createNativeQuery("select * from sale_return where SALE_RETURN_ID=?", SaleReturn.class);
        q.setParameter(1, id);
        return (SaleReturn) q.getSingleResult();
    }

    public static CashIn getCashIn(EntityManager em, int id) {
        Query q = em.createNativeQuery("select * from cash_in where cash_in_id=?", CashIn.class);
        q.setParameter(1, id);
        return (CashIn) q.getSingleResult();
    }

    public static CashIn getLockedCashIn(EntityManager em, int id) {
        Query q = em.createNativeQuery("select * from cash_in where cash_in_id=? for update", CashIn.class);
        q.setParameter(1, id);
        return (CashIn) q.getSingleResult();
    }

    public static SaleReturn getLockedReturn(EntityManager em, int returnId) {
        Query q = em.createNativeQuery("select * from sale_return where sale_return_id=? for update", SaleReturn.class);
        q.setParameter(1, returnId);
        return (SaleReturn) q.getSingleResult();
    }

    public static ReturnReplacement getReturnOrReplacement(EntityManager em, int returnReplacementId) {
        Query q = em.createNativeQuery("select * from return_replacement where return_replacement_id=? for update", ReturnReplacement.class);
        q.setParameter(1, returnReplacementId);
        return (ReturnReplacement) q.getSingleResult();
    }

    public static AccountHistory getLockedPSCTransaction(EntityManager em, int id) {
        Query q = em.createNativeQuery("select * from account_history where id=? for update", AccountHistory.class);
        q.setParameter(1, id);
        return (AccountHistory) q.getSingleResult();
    }

    public static X3TransactionState getLockedState(EntityManager em, X3TransactionState state) {
        X3TransactionState stateLocked = em.find(X3TransactionState.class, state.getX3TransactionStatePK());
        em.refresh(stateLocked, LockModeType.PESSIMISTIC_READ);
        return stateLocked;
    }

    public static int deleteFailedX3RequestStateForSale(EntityManager em, int saleId) {
        Query q = em.createNativeQuery("delete from x3_request_state where table_name='sale' and primary_key=? and status='ER'");
        q.setParameter(1, saleId);
        return q.executeUpdate();
    }

    public static int deleteFailedX3TransactionStateForSale(EntityManager em, int saleId) {
        Query q = em.createNativeQuery("delete from x3_transaction_state where table_name='sale' and primary_key=? and status='ER'");
        q.setParameter(1, saleId);
        return q.executeUpdate();
    }

    public static int deleteFailedX3TransactionStateForCashIn(EntityManager em, int cashInId) {
        Query q = em.createNativeQuery("delete from x3_transaction_state where table_name='cash_in' and primary_key=? and status='ER'");
        q.setParameter(1, cashInId);
        return q.executeUpdate();
    }

    public static int deleteFailedX3RequestStateForCashIn(EntityManager em, int cashInId) {
        Query q = em.createNativeQuery("delete from x3_request_state where table_name='cash_in' and primary_key=? and status='ER'");
        q.setParameter(1, cashInId);
        return q.executeUpdate();
    }

    public static String getX3RequestDataForSale(EntityManager em, int saleId) {
        Query q = em.createNativeQuery("select * from x3_request_state where table_name='sale' and primary_key=? order by start_date_time", X3RequestState.class);
        q.setParameter(1, saleId);
        StringBuilder data = new StringBuilder();
        for (X3RequestState state : (Collection<X3RequestState>) q.getResultList()) {
            data.append("[");
            data.append(state.getX3RequestStatePK().getTransactionType());
            data.append(" sent to X3 on ");
            data.append(state.getStartDateTime());
            data.append(" and has status ");
            data.append(state.getStatus());
            data.append(". X3 Record Id is ");
            data.append(state.getX3RecordId());
            if (state.getMessages() != null && !state.getMessages().isEmpty()) {
                data.append(" and returned message: ");
                data.append(Utils.removeNonASCIIChars(state.getMessages()));
            }
            data.append("] ");
        }
        if (data.length() == 0) {
            data.append("Nothing sent to X3 yet");
        }
        return data.toString();
    }

    public static Collection<CashIn> getCashInsBySalesAdministratorCustomerIdStatusAndCashInType(EntityManager em, int saleAdministratorCustomerId, String cashInType, String status) {
        Query q = em.createNativeQuery("select C.* from cash_in C where (C.sales_administrator_customer_id=? or C.sales_person_customer_id=?) and C.CASH_IN_TYPE = ? and C.status=?;", CashIn.class);
        q.setParameter(1, saleAdministratorCustomerId);
        q.setParameter(2, saleAdministratorCustomerId);
        q.setParameter(3, cashInType);
        q.setParameter(4, status);
        return q.getResultList();
    }

    public static Collection<CashIn> getCashInsByStatusAndCashInType(EntityManager em, String cashInType, String status) {
        Query q = em.createNativeQuery("select C.* from cash_in C where C.CASH_IN_TYPE = ? and C.status=?;", CashIn.class);
        q.setParameter(1, cashInType);
        q.setParameter(2, status);
        return q.getResultList();
    }

    public static CashIn getCashInBySaleId(EntityManager em, int saleId) {
        Query q = em.createNativeQuery("select C.* from cash_in C join cash_in_row R on C.cash_in_id = R.cash_in_id  where R.sale_id=?", CashIn.class);
        q.setParameter(1, saleId);
        return (CashIn) q.getSingleResult();
    }

    public static Collection<Sale> getSalesBySerialNumber(EntityManager em, String serial) {
        // Limit in case somone looks for a sale with a blank serial or something
        Query q = em.createNativeQuery("select S.* from sale S, sale_row R where R.sale_id=S.sale_id and R.serial_number = ? order by S.SALE_DATE_TIME LIMIT 100", Sale.class);
        q.setParameter(1, serial);
        return q.getResultList();
    }

    public static Sale getMostRecentSaleBySerialNumber(EntityManager em, String serial) {
        Query q = em.createNativeQuery("select S.* from sale S, sale_row R where R.sale_id=S.sale_id and R.serial_number = ? order by S.SALE_DATE_TIME DESC LIMIT 1", Sale.class);
        q.setParameter(1, serial);
        return (Sale) q.getSingleResult();
    }

    public static String getLastSaleStatusForSerialNumber(EntityManager em, String serial) {
        // Dont include sales that the item was returned in
        Query q = em.createNativeQuery("select S.STATUS from sale S join sale_row R on R.sale_id=S.sale_id left join sale_return_row SRR on SRR.sale_row_id=R.sale_row_id where R.serial_number = ? and SRR.sale_row_id is null and S.sale_date_time > now() - INTERVAL ? DAY order by S.sale_date_time desc limit 1");
        q.setParameter(1, serial);
        q.setParameter(2, BaseUtils.getIntProperty("env.pos.item.sold.lookback.days", 90)); // Default to 90 days
        String status = null;
        try {
            status = (String) q.getSingleResult();
        } catch (Exception e) {
            log.debug("Serial number has not been sold");
        }
        return status;
    }

    public static List<String> getLastSaleStatusesForSerialNumbers(EntityManager em, Set<String> serialNumbers) {
        StringBuilder serials = new StringBuilder();
        for (String serial : serialNumbers) {
            serials.append("'");
            serials.append(serial);
            serials.append("',");
        }
        serials.setLength(serials.length() - 1); // remove trailing ,
        Query q = em.createNativeQuery("select distinct S.STATUS from sale S join "
                + "(SELECT max(S.SALE_ID) as SALE_ID "
                + "FROM sale S "
                + "JOIN sale_row R ON R.sale_id = S.sale_id "
                + "LEFT JOIN sale_return_row SRR ON SRR.sale_row_id = R.sale_row_id "
                + "WHERE R.serial_number in (" + serials.toString() + ") "
                + "AND SRR.sale_row_id IS NULL "
                + "AND S.sale_date_time > now() - INTERVAL ? DAY "
                + "group by R.SERIAL_NUMBER "
                + ") as tmp on tmp.SALE_ID = S.SALE_ID");
        q.setParameter(1, BaseUtils.getIntProperty("env.pos.item.sold.lookback.days", 90)); // Default to 90 days
        return q.getResultList();
    }

    public static String getRandomSerialNumber(EntityManager em, String itemNumber, String warehouseId) {
        Query q = em.createNativeQuery("select SERIAL_NUMBER from x3_offline_inventory where item_number=? and location=? ORDER BY RAND() LIMIT 1");
        q.setParameter(1, itemNumber);
        q.setParameter(2, warehouseId);
        return (String) q.getSingleResult();
    }

    public static Collection<Sale> getSalesForCustomer(EntityManager em, int customerId, Date from, Date to) {
        Query q = em.createNativeQuery("select * from sale where RECIPIENT_CUSTOMER_ID=? and SALE_DATE_TIME >= ? and SALE_DATE_TIME <= ? order by SALE_DATE_TIME LIMIT 50", Sale.class);
        q.setParameter(1, customerId);
        q.setParameter(2, from);
        q.setParameter(3, to);
        return q.getResultList();
    }

    public static Collection<Sale> getSalesForContract(EntityManager em, int contractId, Date from, Date to) {
        Query q = em.createNativeQuery("select * from sale where CONTRACT_ID=? and SALE_DATE_TIME >= ? and SALE_DATE_TIME <= ? order by SALE_DATE_TIME LIMIT 50", Sale.class);
        q.setParameter(1, contractId);
        q.setParameter(2, from);
        q.setParameter(3, to);
        return q.getResultList();
    }

    public static Collection<Sale> getSalesByPurchaseOrderData(EntityManager em, String poData) {
        Query q = em.createNativeQuery("select * from sale where PURCHASE_ORDER_DATA=? order by SALE_DATE_TIME", Sale.class);
        q.setParameter(1, poData);
        return q.getResultList();
    }

    public static Sale getMostRecentSaleForCustomer(EntityManager em, int customerId, int sinceMinutes) {
        Query q = em.createNativeQuery("select * from sale where RECIPIENT_CUSTOMER_ID=? and SALE_DATE_TIME >= now() - interval ? minute order by SALE_DATE_TIME DESC LIMIT 1", Sale.class);
        q.setParameter(1, customerId);
        q.setParameter(2, sinceMinutes);
        return (Sale) q.getSingleResult();
    }

    public static Collection<Sale> getSalesByStatus(EntityManager em, String status, Date from, Date to) {
        Query q = em.createNativeQuery("select * from sale where STATUS = ? and SALE_DATE_TIME >= ? and SALE_DATE_TIME <= ? order by SALE_DATE_TIME LIMIT 50", Sale.class);
        q.setParameter(1, status);
        q.setParameter(2, from);
        q.setParameter(3, to);
        return q.getResultList();
    }

    public static Collection<Sale> getSalesByPaymentMethodAndStatusWithNoX3TransactionOfType(EntityManager em, String paymentMethod, String status, String transactionType, int limit, int lookbackDays) {
        // For performance reasons, we wont look at all the sales - only join with the last weeks sales. No point joining both tables with all data
        Query q = em.createNativeQuery("select S.* from sale S left join x3_transaction_state X on (X.PRIMARY_KEY=S.SALE_ID and X.TABLE_NAME='sale' and X.TRANSACTION_TYPE=?) where X.PRIMARY_KEY is null and S.channel != '' and S.LAST_MODIFIED > now() - interval ? day and S.PAYMENT_METHOD=? and S.STATUS=? LIMIT ?", Sale.class);
        q.setParameter(1, transactionType);
        q.setParameter(2, lookbackDays);
        q.setParameter(3, paymentMethod);
        q.setParameter(4, status);
        q.setParameter(5, limit);
        List<Sale> salesList = q.getResultList();
        Collections.shuffle(salesList);
        return salesList;
    }

    public static Collection<Sale> getSalesByPaymentMethodAndStatusWithNoX3TransactionOfType(EntityManager em, String paymentMethod, String status, String transactionType, int limit) {
        // For performance reasons, we wont look at all the sales - only join with the last weeks sales. No point joining both tables with all data
        Query q = em.createNativeQuery("select S.* from sale S left join x3_transaction_state X on (X.PRIMARY_KEY=S.SALE_ID and X.TABLE_NAME='sale' and X.TRANSACTION_TYPE=?) where X.PRIMARY_KEY is null and S.channel != '' and S.PAYMENT_METHOD=? and S.STATUS=? LIMIT ?", Sale.class);
        q.setParameter(1, transactionType);
        q.setParameter(2, paymentMethod);
        q.setParameter(3, status);
        q.setParameter(4, limit);
        List<Sale> salesList = q.getResultList();
        return salesList;
    }

    public static Collection<Sale> getActiveContractSales(EntityManager em, String paymentMethod, String status, int limit) {
        // For performance reasons, we wont look at all the sales - only join with the last weeks sales. No point joining both tables with all data
        Query q = em.createNativeQuery("select S.* from sale S where S.PAYMENT_METHOD=? and S.STATUS=? order by S.SALE_DATE_TIME LIMIT ?", Sale.class);
        q.setParameter(1, paymentMethod);
        q.setParameter(2, status);
        q.setParameter(3, limit);
        List<Sale> salesList = q.getResultList();
        return salesList;
    }

    public static Collection<SaleReturn> getReturnsWithNoX3Transaction(EntityManager em, int limit) {
        // For performance reasons, we wont look at all the sales - only join with the last weeks sales. No point joining both tables with all data
        Query q = em.createNativeQuery("select R.* from sale_return R left join x3_transaction_state X on (X.PRIMARY_KEY=R.SALE_RETURN_ID and X.TABLE_NAME='sale_return' and X.TRANSACTION_TYPE=?) where X.PRIMARY_KEY is null  and R.SALE_RETURN_DATE_TIME > now() - interval ? day order by R.SALE_RETURN_DATE_TIME LIMIT ?", SaleReturn.class);
        q.setParameter(1, X3InterfaceDaemon.X3_TRANSACTION_TYPE_RETURN);
        q.setParameter(2, BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days"));
        q.setParameter(3, limit);
        List<SaleReturn> returnsList = q.getResultList();
        return returnsList;
    }

    public static Collection<ReturnReplacement> getLineReturnsWithNoX3Transaction(EntityManager em, int limit) {
        // For performance reasons, we wont look at all the sales - only join with the last weeks sales. No point joining both tables with all data
        Query q = em.createNativeQuery("select R.* from return_replacement R left join x3_transaction_state X on (X.PRIMARY_KEY=R.RETURN_REPLACEMENT_ID and X.TABLE_NAME='return_replacement' and X.TRANSACTION_TYPE=? and R.REPLACEMENT_ITEM_NUMBER is null) \n"
                + "where X.PRIMARY_KEY is null  and R.LAST_MODIFIED > now() - interval ? day order by R.CREATED_DATE_TIME LIMIT ?", ReturnReplacement.class);
        q.setParameter(1, X3InterfaceDaemon.X3_TRANSACTION_TYPE_LINE_RETURN);
        q.setParameter(2, BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days"));
        q.setParameter(3, limit);
        List<ReturnReplacement> returnsList = q.getResultList();
        return returnsList;
    }

    public static Collection<ReturnReplacement> getLineReplacementsWithNoX3Transaction(EntityManager em, int limit) {
        // For performance reasons, we wont look at all the sales - only join with the last weeks sales. No point joining both tables with all data
        Query q = em.createNativeQuery("select R.* from return_replacement R left join x3_transaction_state X on (X.PRIMARY_KEY=R.RETURN_REPLACEMENT_ID and X.TABLE_NAME='return_replacement' and X.TRANSACTION_TYPE=? and R.REPLACEMENT_ITEM_NUMBER is not null) \n"
                + "where X.PRIMARY_KEY is null  and R.LAST_MODIFIED > now() - interval ? day order by R.CREATED_DATE_TIME LIMIT ?", ReturnReplacement.class);
        q.setParameter(1, X3InterfaceDaemon.X3_TRANSACTION_TYPE_LINE_REPLACEMENT);
        q.setParameter(2, BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days"));
        q.setParameter(3, limit);
        List<ReturnReplacement> returnsList = q.getResultList();
        return returnsList;
    }

    public static Collection<CashIn> getCompletedCashInsWithNoX3Transaction(EntityManager em, int limit) {
        Query q = em.createNativeQuery("select R.* from cash_in R left join x3_transaction_state X on (X.PRIMARY_KEY=R.CASH_IN_ID and X.TABLE_NAME='cash_in' and X.TRANSACTION_TYPE=?) where X.PRIMARY_KEY is null and R.CASH_IN_DATE_TIME > now() - interval ? day and R.CASH_IN_DATE_TIME >= '2013/06/26' and right(R.status, 1) = 'C' order by R.CASH_IN_DATE_TIME LIMIT ?", CashIn.class);
        q.setParameter(1, X3InterfaceDaemon.X3_TRANSACTION_TYPE_CASHIN);
        q.setParameter(2, BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days"));
        q.setParameter(3, limit);
        List<CashIn> returnsList = q.getResultList();
        return returnsList;
    }

    public static Collection<X3TransactionState> getX3TransactionStateRows(EntityManager em, int primaryKey, String tableName) {
        Query q = em.createNativeQuery("select * from x3_transaction_state X where X.PRIMARY_KEY=? and X.TABLE_NAME=?", X3TransactionState.class);
        q.setParameter(1, primaryKey);
        q.setParameter(2, tableName);
        return q.getResultList();
    }

    public static Collection<X3TransactionState> getX3TransactionsByType(EntityManager em, int primaryKey, String tableName, String transactionType) {
        Query q = em.createNativeQuery("select * from x3_transaction_state X where X.PRIMARY_KEY=? and X.TABLE_NAME=? and X.TRANSACTION_TYPE=?", X3TransactionState.class);
        q.setParameter(1, primaryKey);
        q.setParameter(2, tableName);
        q.setParameter(3, transactionType);
        return q.getResultList();
    }

    public static Collection<X3TransactionState> getX3TransactionsByTypeAndStatus(EntityManager em, String transactionType, String status, int limit) {
        Query q = em.createNativeQuery("select * from x3_transaction_state X where X.TRANSACTION_TYPE=? and X.STATUS=? LIMIT ?", X3TransactionState.class);
        q.setParameter(1, transactionType);
        q.setParameter(2, status);
        q.setParameter(3, limit);
        return q.getResultList();
    }

    public static X3TransactionState getX3TransactionForRequest(EntityManager em, X3RequestState request) {
        Query q = em.createNativeQuery("select * from x3_transaction_state where TRANSACTION_TYPE=? and TABLE_NAME=? and PRIMARY_KEY = ? and STATUS='ER'", X3TransactionState.class);
        q.setParameter(1, request.getX3RequestStatePK().getTransactionType());
        q.setParameter(2, request.getX3RequestStatePK().getTableName());
        q.setParameter(3, request.getX3RequestStatePK().getPrimaryKey());
        return (X3TransactionState) q.getSingleResult();
    }

    public static List<Integer> getOldOrInProgressContractFulfilmentSaleIds(EntityManager em) {
        Query q = em.createNativeQuery("SELECT SALE_ID"
                + "  FROM sale"
                + " WHERE     PAYMENT_METHOD = 'Contract'"
                + "       AND STATUS != 'DE'"
                + "       AND (   FULFILMENT_LAST_CHECK_DATE_TIME IS NULL"
                + "            OR FULFILMENT_LAST_CHECK_DATE_TIME < now() - INTERVAL ? MINUTE"
                + "            OR STATUS IN ('PG', 'GO'))"
                + "       AND (   FULFILMENT_PAUSED_TILL_DATE_TIME IS NULL"
                + "            OR FULFILMENT_PAUSED_TILL_DATE_TIME < now())"
                + " LIMIT 1000");
        q.setParameter(1, BaseUtils.getIntProperty("env.pos.contract.schedule.check.frequency.minutes", 10));
        return q.getResultList();
    }

    public static Date getLastContractSaleFulfilmentDate(EntityManager em, int saleId) {
        // Dont bother looking at rows older than 6 months
        Query q = em.createNativeQuery("select IFNULL(MAX(SALE_DATE_TIME),STR_TO_DATE('01/01/1970', '%m/%d/%Y')) from sale where CONTRACT_SALE_ID=?");
        q.setParameter(1, saleId);
        return (Date) q.getSingleResult();
    }

    public static Collection<AccountHistory> getPendingPSCTTransactions(EntityManager em, int daysSinceLastProcessed) {
        // Get the list of pending PARTNER_SALES_COMMISSION_TRANSFER psct – Partner is paid a commission in airtime.
        Query q = em.createNativeQuery("SELECT H.* "
                + "from account_history H left join x3_request_state XRS on (H.ID = XRS.PRIMARY_KEY "
                + "                                                      and XRS.TABLE_NAME='account_history' "
                + "                                                      and XRS.TRANSACTION_TYPE='PSCTGLEntry') "
                + "where XRS.PRIMARY_KEY is null "
                + "and H.TRANSACTION_TYPE='txtype.tfr.credit.psct' "
                + "and H.END_DATE > now() - interval ? day "
                + "order by H.END_DATE asc;", AccountHistory.class);
        q.setParameter(1, daysSinceLastProcessed);
        return q.getResultList();
    }

    public static X3TransactionState createX3TransactionInOwnTransactionScope(EntityManagerFactory emf, int primaryKey, String tableName, String transactionType, String status, String extraInfo) {
        EntityManager em = null;
        X3TransactionState state = null;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            X3TransactionStatePK statePK = new X3TransactionStatePK();
            statePK.setPrimaryKey(primaryKey);
            statePK.setTableName(tableName);
            statePK.setTransactionType(transactionType);
            state = new X3TransactionState();
            state.setEndDateTime(null);
            state.setExtraInfo(extraInfo);
            state.setStartDateTime(new Date());
            state.setStatus(status);
            state.setX3TransactionStatePK(statePK);
            em.persist(state);
            em.flush();
        } finally {
            JPAUtils.commitTransactionAndClose(em);
        }
        return state;
    }

    public static void setX3TransactionResultInOwnTransactionScope(EntityManagerFactory emf, X3TransactionState s, String status) {
        EntityManager em = null;
        try {
            log.debug("Updating X3 transaction state [{}] to [{}]", s, status);
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            Query q = em.createNativeQuery("update x3_transaction_state set STATUS=?, END_DATE_TIME=now() where PRIMARY_KEY=? and TABLE_NAME=? and TRANSACTION_TYPE=?");
            q.setParameter(1, status);
            q.setParameter(2, s.getX3TransactionStatePK().getPrimaryKey());
            q.setParameter(3, s.getX3TransactionStatePK().getTableName());
            q.setParameter(4, s.getX3TransactionStatePK().getTransactionType());
            q.executeUpdate();
        } finally {
            JPAUtils.commitTransactionAndClose(em);
        }
    }

    public static X3RequestState createX3RequestInOwnTransactionScope(EntityManagerFactory emf, X3TransactionState transactionState, String requestType, String status) {
        EntityManager em = null;
        X3RequestState state = null;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            X3RequestStatePK statePK = new X3RequestStatePK();
            statePK.setPrimaryKey(transactionState.getX3TransactionStatePK().getPrimaryKey());
            statePK.setTableName(transactionState.getX3TransactionStatePK().getTableName());
            statePK.setTransactionType(transactionState.getX3TransactionStatePK().getTransactionType());
            statePK.setRequestType(requestType);
            state = new X3RequestState();
            state.setEndDateTime(null);
            state.setStartDateTime(new Date());
            state.setStatus(status);
            state.setX3RequestStatePK(statePK);
            try {
                em.persist(state);
                em.flush();
            } catch (Exception pe) {
                log.debug("Exception persisting request data - probably processed before [{}]", pe.toString());
                em.clear();
                state = JPAUtils.findAndThrowENFE(em, X3RequestState.class, statePK);
                log.debug("Request statue of previous request is [{}]", state.getStatus());
            }
        } finally {
            JPAUtils.commitTransactionAndClose(em);
        }
        return state;
    }

    public static String getX3RecordId(EntityManager em, String tableName, int primaryKey, String requestType) {
        Query q = em.createNativeQuery("select X3_RECORD_ID from x3_request_state X where X.PRIMARY_KEY=? and X.TABLE_NAME=? and X.REQUEST_TYPE=?");
        q.setParameter(1, primaryKey);
        q.setParameter(2, tableName);
        q.setParameter(3, requestType);
        return (String) q.getSingleResult();
    }

    public static String getDeviceIMEIUsingSerialNumber(EntityManager em, String deviceSerialNumber) {
        try {
            Query q = em.createNativeQuery("select IMEI from devices X where SERIAL_NUMBER=? ");
            q.setParameter(1, deviceSerialNumber);
            return (String) q.getSingleResult();
        } catch (Exception ex) {
            return null;
        }
    }

    public static String getDeviceSerialNumberUsingSimSerialNumber(EntityManager em, String simSerialNumber, int saleId, String devicePrefixes) {
        try {
            Query q = em.createNativeQuery("select DEVICE.serial_number "
                    + " from sale_row SIM join sale_row DEVICE on (SIM.SERIAL_NUMBER=? and SIM.SALE_ID=? and substring(DEVICE.ITEM_NUMBER,  1,  3) in (" + devicePrefixes
                    + ") and DEVICE.PARENT_SALE_ROW_ID = SIM.PARENT_SALE_ROW_ID and DEVICE.SALE_ID=?) limit 1;");

            q.setParameter(1, simSerialNumber);
            q.setParameter(2, saleId);
            q.setParameter(3, saleId);
            return (String) q.getSingleResult();
        } catch (Exception ex) {
            log.error("Unable to locate device for SIM serial [{}]", simSerialNumber);
            return null;
        }
    }

    public static void setX3RequestFinishedInOwnTransactionScope(EntityManagerFactory emf, X3RequestState requestState, CAdxResultXml result) {
        log.debug("Updating x3_request_state to [{}]", result.getStatusCode());
        EntityManager em = null;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            Query q = em.createNativeQuery("update x3_request_state set STATUS=?, END_DATE_TIME=now(), RESULT_CODE=?, RESULT_XML=?, MESSAGES=?, X3_RECORD_ID=? where PRIMARY_KEY=? and TABLE_NAME=? and TRANSACTION_TYPE=? and REQUEST_TYPE=?");
            q.setParameter(1, result.getStatusCode());
            q.setParameter(2, result.getStatus());
            if (result.getResultXml() != null && result.getResultXml().length() > 5000) {
                result.setResultXml(result.getResultXml().substring(0, 5000));
            }
            q.setParameter(3, result.getResultXml());
            StringBuilder messages = new StringBuilder();
            for (CAdxMessage msg : result.getMessages()) {
                messages.append(msg.getType());
                messages.append("=");
                messages.append(msg.getMessage());
                messages.append("\r\n");
            }
            messages.append("[Request Sent from POS at: ").append(BaseUtils.getHostNameFromKernel()).append("]");
            q.setParameter(4, messages.toString());
            q.setParameter(5, result.getX3RecordId());
            q.setParameter(6, requestState.getX3RequestStatePK().getPrimaryKey());
            q.setParameter(7, requestState.getX3RequestStatePK().getTableName());
            q.setParameter(8, requestState.getX3RequestStatePK().getTransactionType());
            q.setParameter(9, requestState.getX3RequestStatePK().getRequestType());

            q.executeUpdate();
        } finally {
            JPAUtils.commitTransactionAndClose(em);
        }
        log.debug("Finished updating x3_request_state to [{}]", result.getStatusCode());
    }

    public static boolean hasSaleBeenCashedIn(EntityManager em, int saleId) {
        Query q = em.createNativeQuery("select count(R.SALE_ID) from cash_in_row R where R.SALE_ID = ?");
        q.setParameter(1, saleId);
        Long cnt = (Long) q.getSingleResult();
        return cnt > 0;
    }

    public static boolean checkIfItemUsedAsKitReplacement(EntityManager em, String serialNumber) {
        if (serialNumber == null || serialNumber.isEmpty()) {
            return false;
        }

        Query q = em.createNativeQuery("select count(R.SALE_ID) from sale R where R.PURCHASE_ORDER_DATA = ?");
        q.setParameter(1, serialNumber);
        Long cnt = (Long) q.getSingleResult();
        return cnt > 0;
    }

    //HBT-8052 -A sale shouldn't be reversed if there is return already done on it. 
    public static boolean hasSaleHaveReturnedDevice(EntityManager em, int saleId) {
        Query q = em.createNativeQuery("select count(SR.SALE_ID) from sale_return SR where SR.SALE_ID = ?");
        q.setParameter(1, saleId);
        Long cnt = (Long) q.getSingleResult();
        return cnt > 0;
    }

    public static Collection<X3RequestState> getRecentFailedX3Requests(EntityManager em) {
        // Find all requests that have failed in the last day. The request may be for a sale or other transaction which has been failing for many days though.
        Query q = em.createNativeQuery("select * from x3_request_state where START_DATE_TIME > now() - interval 1 day and  START_DATE_TIME < now() - interval 30 minute and (STATUS='ER' OR STATUS='IP')", X3RequestState.class);
        return q.getResultList();
    }

    public static Date getCashInDate(EntityManager em, Integer saleId) {
        try {
            Query q = em.createNativeQuery("select C.CASH_IN_DATE_TIME from cash_in C, cash_in_row R where R.SALE_ID = ? and C.cash_in_id = R.cash_in_id and C.STATUS != ?");
            q.setParameter(1, saleId);
            q.setParameter(2, "BDP");
            return (Date) q.getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    public static SaleReturn createReturn(EntityManager em, int saleId, int salesPersonCustomerId, String reasonCode, String description, String location) {
        SaleReturn dbReturn = new SaleReturn();
        dbReturn.setDescription(description);
        dbReturn.setReasonCode(reasonCode);
        dbReturn.setSaleReturnDateTime(new Date());
        dbReturn.setSaleId(saleId);
        dbReturn.setSalesPersonCustomerId(salesPersonCustomerId);
        dbReturn.setReturnLocation(location);
        em.persist(dbReturn);
        em.flush();
        em.refresh(dbReturn);
        return dbReturn;
    }

    public static SaleRow getSaleRowBySaleRowId(EntityManager em, int lineId) {
        Query q = em.createNativeQuery("select * from sale_row where SALE_ROW_ID=?", SaleRow.class);
        q.setParameter(1, lineId);
        return (SaleRow) q.getSingleResult();
    }
    
    public static List<SaleRow> getSaleRowsBySaleId(EntityManager em, int saleId) {
        Query q = em.createNativeQuery("select * from sale_row where SALE_ID=?", SaleRow.class);
        q.setParameter(1, saleId);
        return q.getResultList();
    }
    
    public static void createReturnRow(EntityManager em, Integer returnId, Integer saleRowId, Long quantity) {
        SaleReturnRow dbReturnRow = new SaleReturnRow();
        dbReturnRow.setSaleReturnId(returnId);
        dbReturnRow.setSaleRowId(saleRowId);
        dbReturnRow.setReturnedQuantity(quantity);
        em.persist(dbReturnRow);
        em.flush();
    }

    public static boolean hasCreditFacilityLoanReferenceBeenUsed(EntityManager em, String creditReference) {
        Query q = em.createNativeQuery("select count(S.SALE_ID) from sale S where S.PAYMENT_METHOD=? and S.PAYMENT_TRANSACTION_DATA=? and S.STATUS=?");
        q.setParameter(1, POSManager.PAYMENT_METHOD_CREDIT_FACILITY);
        q.setParameter(2, creditReference);
        q.setParameter(3, POSManager.PAYMENT_STATUS_PAID);
        Long cnt = (Long) q.getSingleResult();
        return cnt > 0;
    }

    public static boolean hasCreditNoteBeenUsed(EntityManager em, String creditNoteId) {
        Query q = em.createNativeQuery("select count(S.SALE_ID) from sale S where S.PAYMENT_METHOD=? and S.PAYMENT_TRANSACTION_DATA=? and S.STATUS=?");
        q.setParameter(1, POSManager.PAYMENT_METHOD_CREDIT_NOTE);
        q.setParameter(2, creditNoteId);
        q.setParameter(3, POSManager.PAYMENT_STATUS_PAID);
        Long cnt = (Long) q.getSingleResult();
        return cnt > 0;
    }

    public static Collection<SaleReturnRow> getReturnRows(EntityManager em, Integer returnId) {
        Query q = em.createNativeQuery("select * from sale_return_row where sale_return_id=? order by sale_return_row_id", SaleReturnRow.class);
        q.setParameter(1, returnId);
        return q.getResultList();
    }

    public static SaleReturnRow getSaleReturnRowBySaleRowId(EntityManager em, Integer saleRowId) {
        Query q = em.createNativeQuery("select * from sale_return_row where SALE_ROW_ID=?", SaleReturnRow.class);
        q.setParameter(1, saleRowId);
        return (SaleReturnRow) q.getSingleResult();
    }

    public static Collection<CashInRow> getCashInRows(EntityManager em, Integer cashInId) {
        Query q = em.createNativeQuery("select R.* from cash_in_row R, sale S where R.cash_in_id=? and R.SALE_ID = S.SALE_ID order by S.SALE_DATE_TIME", CashInRow.class);
        q.setParameter(1, cashInId);
        return q.getResultList();
    }

    public static Collection<Integer> getExpiredSales(EntityManager em) {
        Query q = em.createNativeQuery("select sale_id from sale where expiry_date_time < now() and expiry_date_time > now() - interval 2 day and (status = ? or status = ? or status = ?) and channel != ''");
        q.setParameter(1, POSManager.PAYMENT_STATUS_QUOTE);
        q.setParameter(1, POSManager.PAYMENT_STATUS_SHOP_PICKUP);
        q.setParameter(2, POSManager.PAYMENT_STATUS_PENDING_PAYMENT);
        q.setParameter(3, POSManager.PAYMENT_STATUS_DELAYED_PAYMENT);
        return q.getResultList();
    }
    //TRA Stuff

    public static Collection<Sale> getSalesByTRAStatus(EntityManager em, String status, int limit) {
        Query q = em.createNativeQuery("SELECT S.* from sale S LEFT JOIN tra_state T ON S.SALE_ID = T.SALE_ID WHERE T.STATUS = ? AND S.SALE_DATE_TIME > now() - interval ? day AND T.SALE_ID IS NOT NULL ORDER BY S.SALE_DATE_TIME ASC LIMIT ?", Sale.class);
        q.setParameter(1, status);
        q.setParameter(2, BaseUtils.getIntProperty("env.tra.sales.invoice.lookup.days", 3) + 2);//look back two more days in case sales were missed
        q.setParameter(3, limit);
        return (List<Sale>) q.getResultList();

    }

    public static Collection<Sale> getSalesByPaymentMethodWithNoTraState(EntityManager em, String paymentType, int limit) {
        Query q = em.createNativeQuery("SELECT S.* from sale S LEFT JOIN tra_state T ON S.SALE_ID = T.SALE_ID WHERE S.STATUS NOT IN ('ST', 'QT','RV','CB') AND S.PAYMENT_METHOD = ? AND S.SALE_DATE_TIME > now() - interval ? day AND SALE_TOTAL_CENTS_INCL!=0 AND T.SALE_ID IS NULL ORDER BY S.SALE_DATE_TIME ASC LIMIT ?", Sale.class);
        q.setParameter(1, paymentType);
        q.setParameter(2, BaseUtils.getIntProperty("env.tra.sales.invoice.lookup.days"));
        q.setParameter(3, limit);

        return (List<Sale>) q.getResultList();
    }

    public static TraState createTRAStateInOwnTranscationScope(EntityManager em, int saleId, String status) {
        TraState state = null;
        try {
            log.debug("Creating TRA State Entry for SALE_ID [{}]", saleId);
            JPAUtils.beginTransaction(em);
            state = new TraState();
            state.setSaleId(saleId);
            state.setStatus(status);
            state.setEndDateTime(null);
            state.setStartDateTime(new Date());
            try {
                em.persist(state);
                em.flush();
            } catch (Exception e) {
                log.warn("Exception persisting request data - probably processed before [{}]", e.toString());
                em.clear();
                state = JPAUtils.findAndThrowENFE(em, TraState.class, state.getSaleId());
                log.warn("Request status of previous request is [{}]", state.getStatus());
            }
        } finally {
            JPAUtils.commitTransaction(em);
            JPAUtils.beginTransaction(em);
        }
        return state;
    }

    public static void setTRAStateInOwnTransactionScope(EntityManager em, TraState s, String status) {
        try {
            log.debug("Updating Sales Invoice/Quote TRA State Entry for Sale ID [{}] from status [{}] to [{}]", new Object[]{s.getSaleId(), s.getStatus(), status});
            JPAUtils.beginTransaction(em);
            Query q = em.createNativeQuery("update tra_state set STATUS=?, END_DATE_TIME=now() where SALE_ID=?");
            q.setParameter(1, status);
            q.setParameter(2, s.getSaleId());
            q.executeUpdate();
        } finally {
            JPAUtils.commitTransaction(em);
            JPAUtils.beginTransaction(em);
        }
    }

    public static TraState getLockedTraStateBySaleId(EntityManager em, int saleId) {
        TraState state = getTRAStateBySaleId(em, saleId);
        em.refresh(state, LockModeType.PESSIMISTIC_READ);
        return state;
    }

    public static TraState getTRAStateBySaleId(EntityManager em, int saleId) {
        Query q = em.createNativeQuery("select * from tra_state where sale_id=?", TraState.class);
        q.setParameter(1, saleId);
        TraState ret = (TraState) q.getSingleResult();
        return ret;
    }

    public static Collection<TraState> getTraStatesByStatus(EntityManager em, String status, int limit) {
        Query q = em.createNativeQuery("select * from tra_state where status=? ORDER BY END_DATE_TIME DESC LIMIT ?");
        q.setParameter(1, status);
        q.setParameter(2, limit);
        return (Collection<TraState>) q.getResultList();
    }

    //TRA Stuff End
    public static void putProvisioningDataIntoSaleLine(EntityManager em, int saleLineId, String provisioningData) {
        log.debug("In putProvisioningDataIntoSaleLine [{}] [{}]", saleLineId, provisioningData);
        Query q = em.createNativeQuery("update sale_row set PROVISIONING_DATA=? where SALE_ROW_ID=?");
        q.setParameter(1, provisioningData);
        q.setParameter(2, saleLineId);
        q.executeUpdate();
    }

    public static Contract getContract(EntityManager em, int contractId) {
        Query q = em.createNativeQuery("select * from contract where contract_id=?", Contract.class);
        q.setParameter(1, contractId);
        Contract ret;
        try {
            ret = (Contract) q.getSingleResult();
        } catch (NoResultException e) {
            ret = null;
        }
        return ret;
    }

    public static List<Integer> getPaymentGatewayPendingSaleIds(EntityManager em) {
        String sql = "select sale_id from sale where PAYMENT_METHOD=? and STATUS=? and (PAYMENT_GATEWAY_NEXT_POLL_DATE is null or PAYMENT_GATEWAY_NEXT_POLL_DATE <= now()) order by PAYMENT_GATEWAY_LAST_POLL_DATE ASC LIMIT 200";
        Query q = em.createNativeQuery(sql);
        q.setParameter(1, POSManager.PAYMENT_METHOD_PAYMENT_GATEWAY);
        q.setParameter(2, POSManager.PAYMENT_STATUS_PENDING_PAYMENT);
        return q.getResultList();
    }

    public static double getAccountBalance(EntityManager em, long accountId) {
        Query q = em.createNativeQuery("select balance_cents from account where account_id=?");
        q.setParameter(1, accountId);
        return ((BigDecimal) q.getSingleResult()).doubleValue();
    }

    public static List<com.smilecoms.xml.schema.pos.Photograph> getContractDocuments(EntityManager em, int contractId) throws Exception {
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.photograph WHERE CONTRACT_ID=?", Photograph.class);
        q.setParameter(1, contractId);
        List<Photograph> photos = q.getResultList();
        List<com.smilecoms.xml.schema.pos.Photograph> ret = new ArrayList<>();
        for (Photograph photo : photos) {
            com.smilecoms.xml.schema.pos.Photograph cp = new com.smilecoms.xml.schema.pos.Photograph();
            cp.setPhotoGuid(photo.getPhotoGuid());
            cp.setPhotoType(photo.getPhotoType());
            cp.setData(Utils.encodeBase64(photo.getData()));
            ret.add(cp);
            log.debug("Got a photo from DB with GUID [{}]", cp.getPhotoGuid());
        }
        return ret;
    }

    public static void insertGLEntry(EntityManager em, String transType, int primaryKey, String tableName, String glData) {
        X3TransactionStatePK pk = new X3TransactionStatePK();
        pk.setPrimaryKey(primaryKey);
        pk.setTableName(tableName);
        pk.setTransactionType(transType);

        // Check if exists.
        Query q2 = em.createNativeQuery("select * From x3_transaction_state where transaction_type=? and table_name=? and primary_key=?");
        q2.setParameter(1, pk.getTransactionType());
        q2.setParameter(2, pk.getTableName());
        q2.setParameter(3, pk.getPrimaryKey());

        List res = q2.getResultList();

        if (!res.isEmpty()) {
            return; // Do nothing -GL is already in there.
        }

        X3TransactionState state = new X3TransactionState();
        state.setX3TransactionStatePK(pk);
        state.setStartDateTime(new Date());
        state.setStatus(X3InterfaceDaemon.X3_TRANSACTION_STATUS_PENDING);
        state.setExtraInfo(glData);
        em.persist(state);
        em.flush();

    }

    public static void setContractDocuments(EntityManager em, List<com.smilecoms.xml.schema.pos.Photograph> contractDocuments, int contractId) throws Exception {
        Query q = em.createNativeQuery("DELETE FROM SmileDB.photograph WHERE CONTRACT_ID = ?");
        q.setParameter(1, contractId);
        q.executeUpdate();
        for (com.smilecoms.xml.schema.pos.Photograph p : contractDocuments) {
            if (!p.getPhotoGuid().isEmpty() && !p.getPhotoType().isEmpty()) {
                Photograph photo = new Photograph();
                photo.setContractId(contractId);
                photo.setPhotoGuid(p.getPhotoGuid());
                photo.setPhotoType(p.getPhotoType());
                if (p.getData() == null || p.getData().length() < 1000) {
                    throw new Exception("Invalid/empty photograph -- " + photo.getPhotoType());
                }
                photo.setData(Utils.decodeBase64(p.getData()));
                photo.setPhotoHash(HashUtils.md5(photo.getData()));
                Query q2 = em.createNativeQuery("select concat(ifnull(contract_id,''), ifnull(customer_profile_id,''),ifnull(organisation_id,'')) FROM SmileDB.photograph WHERE PHOTO_HASH = ? limit 1");
                q2.setParameter(1, photo.getPhotoHash());
                List res = q2.getResultList();
                if (!res.isEmpty()) {
                    throw new Exception("Duplicate photograph -- " + photo.getPhotoType() + " exists on a customer, contract or organisation with Id " + res.get(0));
                }
                em.persist(photo);
            }
        }
        em.flush();
    }

    public static Iterable<Contract> getContractsByCustomerProfileId(EntityManager em, int customerId) {
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.contract WHERE CUSTOMER_PROFILE_ID=? AND STATUS='AC'", Contract.class);
        q.setParameter(1, customerId);
        return (List<Contract>) q.getResultList();
    }

    public static Iterable<Contract> getContractsByOrganisationId(EntityManager em, int customerId) {
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.contract WHERE ORGANISATION_ID=? AND STATUS='AC'", Contract.class);
        q.setParameter(1, customerId);
        return (List<Contract>) q.getResultList();
    }

    public static ReturnReplacement createReturnOrReplacement(EntityManager em, Integer parentReturnId, int saleRowId, int salesPersonCustomerId, String reasonCode, String description, String location, String replacementItemNumber, String replacementSerialNumber, String returnedSerialNumber, String returnedItemNumber, String replacementItemDescription) {

        ReturnReplacement dbReturnReplacement = new ReturnReplacement();
        dbReturnReplacement.setCreatedByCustomerProfileId(salesPersonCustomerId);
        dbReturnReplacement.setCreatedDateTime(new Date());
        dbReturnReplacement.setLocation(location);
        dbReturnReplacement.setParentReturnReplacementId(parentReturnId);
        dbReturnReplacement.setReplacementItemNumber(replacementItemNumber);
        dbReturnReplacement.setReplacementSerialNumber(replacementSerialNumber);
        dbReturnReplacement.setSaleRowId(saleRowId);
        dbReturnReplacement.setReasonCode(reasonCode);
        dbReturnReplacement.setDescription(description);
        dbReturnReplacement.setReturnedItemNumber(returnedItemNumber);
        dbReturnReplacement.setReturnedSerialNumber(returnedSerialNumber);
        dbReturnReplacement.setLastModified(new Date());
        dbReturnReplacement.setReplacementItemDescription(replacementItemDescription);

        em.persist(dbReturnReplacement);
        em.flush();
        em.refresh(dbReturnReplacement);
        return dbReturnReplacement;

    }

    public static Date getWarrantyEndDateOfMostRecentReplacementItem(EntityManager em, int saleLineId) {
        try {
            Query q = em.createNativeQuery("select created_date_time as warranty_end_date from return_replacement where sale_row_id=? and replacement_item_number is not null order by created_date_time desc limit 1;");
            q.setParameter(1, saleLineId);
            return (Date) q.getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    public static Collection<ReturnReplacement> getReturnReplacements(EntityManager em, int saleRowId) {
        Query q = em.createNativeQuery("select * from return_replacement where sale_row_id=? order by created_date_time asc;", ReturnReplacement.class);
        q.setParameter(1, saleRowId);
        return q.getResultList();
    }

    public static void setSoldStockLocation(EntityManager em, String itemNumber, String serialNumber, int heldByOrganisationId, int soldToOrganisationId) {
        // Last part of the query: Super dealer SIMs are never in kits. Prevent old pre-superdealer sales from being treated like super dealer SIMs
        Query q = em.createNativeQuery("update sale_row R join sale S on S.sale_id = R.sale_id set R.HELD_BY_ORGANISATION_ID=?, R.HELD_BY_LAST_MODIFIED=now() "
                + "where R.SERIAL_NUMBER=? and R.ITEM_NUMBER=? and S.STATUS in ('PD') and S.RECIPIENT_ORGANISATION_ID=? "
                + "and (R.ITEM_NUMBER not like 'SIM%' or R.PARENT_SALE_ROW_ID=0 or R.DESCRIPTION like 'ESim%')");
        q.setParameter(1, heldByOrganisationId);
        q.setParameter(2, serialNumber);
        q.setParameter(3, itemNumber);
        q.setParameter(4, soldToOrganisationId);
        int updates = q.executeUpdate();
        if (updates != 1) {
            throw new RuntimeException("Stock is not available to have its location set -- Serial:" + serialNumber + " ItemNumber:" + itemNumber + " Updates:" + updates);
        }
    }
    
    public static void moveSoldStockLocationFromHeldBy(EntityManager em, String itemNumber, String serialNumber, int heldByOrganisationId, int soldToOrganisationId) {        
        
        String moveStockInfo = "MovedFrom=" +soldToOrganisationId+"\r\nMovedOn=" + Utils.getDateAsString(new Date(), "yyyy/MM/dd HH:mm:ss"); 
        Query q = em.createNativeQuery("update sale_row R join sale S on S.sale_id = R.sale_id set R.HELD_BY_ORGANISATION_ID=?, R.HELD_BY_LAST_MODIFIED=now(), R.PROVISIONING_DATA = IF(R.PROVISIONING_DATA is null, ?, CONCAT(R.PROVISIONING_DATA, '\r\n"+ moveStockInfo +"'))"
                + "where R.SERIAL_NUMBER=? and R.ITEM_NUMBER=? and S.STATUS in ('PD') and R.HELD_BY_ORGANISATION_ID=? "
                + "and (R.ITEM_NUMBER not like 'SIM%' or R.PARENT_SALE_ROW_ID=0 or R.DESCRIPTION like 'ESim%')");
        q.setParameter(1, heldByOrganisationId);        
        q.setParameter(2, moveStockInfo);                             
        q.setParameter(3, serialNumber);        
        q.setParameter(4, itemNumber);        
        q.setParameter(5, soldToOrganisationId);
        
        int updates = 0;
        try {
                updates = q.executeUpdate();
                em.flush();
        }catch (Exception e) {
            log.warn("Update Failed: {}", e);
            if (updates != 1) {
                throw new RuntimeException("Stock is not available to have its location set -- Serial:" + serialNumber + " ItemNumber:" + itemNumber + " Updates:" + updates);
            }
        }
        
    }

    public static void setSoldStockUsedAsReplacement(EntityManager em, String itemNumber, String serialNumber, int heldByOrganisationId, int soldToOrganisationId) {
        // Last part of the query: Super dealer SIMs are never in kits. Prevent old pre-superdealer sales from being treated like super dealer SIMs
        Query q = em.createNativeQuery("update sale_row R join sale S on S.sale_id = R.sale_id set R.PROVISIONING_DATA = IF(R.PROVISIONING_DATA is null, ?, CONCAT(R.PROVISIONING_DATA, '\r\n?'))"
                + "where R.HELD_BY_ORGANISATION_ID=? AND R.SERIAL_NUMBER=? and R.ITEM_NUMBER=? and S.STATUS in ('PD') and S.RECIPIENT_ORGANISATION_ID=? "
                + "and (R.ITEM_NUMBER not like 'SIM%' or R.PARENT_SALE_ROW_ID=0)");
        q.setParameter(1, "PostProcessed=true\r\nReplacement\r\n" + Utils.getDateAsString(new Date(), "yyyy/MM/dd HH:mm:ss"));
        q.setParameter(2, "PostProcessed=true\r\nReplacement\r\n" + Utils.getDateAsString(new Date(), "yyyy/MM/dd HH:mm:ss"));
        q.setParameter(3, heldByOrganisationId);
        q.setParameter(4, serialNumber);
        q.setParameter(5, itemNumber);
        q.setParameter(6, soldToOrganisationId);
        int updates = q.executeUpdate();
        if (updates != 1) {
            throw new RuntimeException("Stock is not available to have its location set -- Serial:" + serialNumber + " ItemNumber:" + itemNumber + " Updates:" + updates);
        }
    }
    
    public static void moveSoldStockUsedAsReplacementFromHeldBy(EntityManager em, String itemNumber, String serialNumber, int heldByOrganisationId, int soldToOrganisationId) {
        String moveStockInfo = "MovedFrom=" +soldToOrganisationId+"\r\nMovedOn=" + Utils.getDateAsString(new Date(), "yyyy/MM/dd HH:mm:ss"); 
        
        Query q = em.createNativeQuery("update sale_row R join sale S on S.sale_id = R.sale_id set R.PROVISIONING_DATA=? "
                + "where R.HELD_BY_ORGANISATION_ID=? AND R.SERIAL_NUMBER=? and R.ITEM_NUMBER=? and S.STATUS in ('PD') and R.HELD_BY_ORGANISATION_ID=? "
                + "and (R.ITEM_NUMBER not like 'SIM%' or R.PARENT_SALE_ROW_ID=0)");
        q.setParameter(1, "PostProcessed=true\r\nReplacement\r\n" + Utils.getDateAsString(new Date(), "yyyy/MM/dd HH:mm:ss") + "\r\n" + moveStockInfo);
        q.setParameter(2, heldByOrganisationId);
        q.setParameter(3, serialNumber);
        q.setParameter(4, itemNumber);
        q.setParameter(5, soldToOrganisationId);
        int updates =0;
        try {
                updates = q.executeUpdate();
                em.flush();
        }catch (Exception e) {
            log.warn("Update Failed: {}", e);
            if (updates != 1) {
                throw new RuntimeException("Stock is not available to have its location set -- Serial:" + serialNumber + " ItemNumber:" + itemNumber + " Updates:" + updates);
            }
        }
        
    }

    public static List<SaleRow> getSoldStockLocationsBySoldTo(EntityManager em, int soldToOrganisationId, int heldByOrganisationId) {
        Query q = em.createNativeQuery("select R.* from sale_row R join sale S on S.SALE_ID = R.SALE_ID where S.RECIPIENT_ORGANISATION_ID=? and R.HELD_BY_ORGANISATION_ID=? and PROVISIONING_DATA NOT LIKE '%PostProcessed=true%' and S.STATUS in ('PD') and R.SERIAL_NUMBER != 'AIRTIME' and R.SERIAL_NUMBER != '' and R.SERIAL_NUMBER NOT LIKE 'BUN%' and S.PAYMENT_METHOD != 'Credit Note'", SaleRow.class);
        q.setParameter(1, soldToOrganisationId);
        q.setParameter(2, heldByOrganisationId);
        return q.getResultList();
    }

    public static List<SaleRow> getSoldStockLocationsByHeldBy(EntityManager em, int heldByOrganisationId) {
        Query q = em.createNativeQuery("select R.* from sale_row R join sale S on S.SALE_ID = R.SALE_ID where R.HELD_BY_ORGANISATION_ID=? and PROVISIONING_DATA NOT LIKE '%PostProcessed=true%' and S.STATUS in ('PD') and R.SERIAL_NUMBER != 'AIRTIME' and R.SERIAL_NUMBER != '' and S.PAYMENT_METHOD != 'Credit Note'", SaleRow.class);
        q.setParameter(1, heldByOrganisationId);
        return q.getResultList();
    }

    public static List<SaleRow> getSoldStockLocationsBySerialNumber(EntityManager em, String serialNumber) {
        Query q = em.createNativeQuery("select R.* from sale_row R join sale S on S.SALE_ID = R.SALE_ID where R.SERIAL_NUMBER=? and R.PROVISIONING_DATA NOT LIKE '%PostProcessed=true%' and S.STATUS in ('PD') and S.PAYMENT_METHOD != 'Credit Note'", SaleRow.class);
        q.setParameter(1, serialNumber);
        return q.getResultList();
    }

    public static SaleRow getUnprocessedSaleRowByItemAndSerial(EntityManager em, String itemNumber, String deviceSerialNumber) {
        Query q = em.createNativeQuery("select R.* from sale_row R join sale S on S.SALE_ID = R.SALE_ID where R.SERIAL_NUMBER=? and R.ITEM_NUMBER=? and R.PROVISIONING_DATA NOT LIKE '%PostProcessed=true%' and S.STATUS in ('PD')", SaleRow.class);
        q.setParameter(1, deviceSerialNumber);
        q.setParameter(2, itemNumber);
        return (SaleRow) q.getSingleResult();
    }

    public static String[] getSimIdentifierAndIdentifierTypeForAccount(EntityManager em, long accountId) {
        String[] identifier = new String[2];

        try {
            Query q = em.createNativeQuery("select SIM.IDENTIFIER_TYPE, SIM.IDENTIFIER from "
                    + " service_instance SI,  "
                    + " service_instance_mapping SIM,  "
                    + " product_instance PI  "
                    + " WHERE SI.ACCOUNT_ID=?  "
                    + " AND SIM.SERVICE_INSTANCE_ID = SI.SERVICE_INSTANCE_ID  "
                    + " AND PI.PRODUCT_INSTANCE_ID = SI.PRODUCT_INSTANCE_ID  "
                    + " AND SI.STATUS = 'AC'  "
                    + " AND SI.SERVICE_SPECIFICATION_ID = 1 limit 1;");
            q.setParameter(1, accountId);
            Object[] res = (Object[]) q.getSingleResult();
            identifier[0] = (String) res[0]; // IdentifierType
            identifier[1] = (String) res[1]; // IMPI

        } catch (Exception ex) {
            log.error("Error while trying to retrieve service code", ex);
        }
        return identifier;
    }

    public static String getSalesPersonGPSCoorinates(EntityManager em, int customerProfileId) {
        /* The GPS coordinate is taken as  the GPS coordinate of the most recent sector where the sales person 
        has been since the start of day. This assumes the sales person is Onnet and using a Smile device and therefore we can track their location
        using our Network database.*/

        Query q = em.createNativeQuery("select SECTOR.GPS_COORD FROM \n"
                + "(Select AH.location, AH.START_DATE\n"
                + "From   account_history AH, service_instance SI\n"
                + "where  AH.service_instance_id = SI.service_instance_id\n"
                + "  AND  SI.customer_profile_id = ?\n"
                + "  AND  AH.start_date >= curdate()\n"
                + "GROUP BY AH.location order by AH.START_DATE desc limit 1) as LATEST_SESSION_START\n"
                + "join Network.sector_location SECTOR on (SECTOR.TAI_ECGI = LATEST_SESSION_START.location)");
        q.setParameter(1, customerProfileId);

        String gpsCoords = null;

        try {
            gpsCoords = (String) q.getSingleResult();
        } catch (Exception e) {
            log.debug("GPS Coordinates not found for sales person [{}]", customerProfileId);
            gpsCoords = "NA";
        }
        return gpsCoords;
    }

    public static PromotionCodeApproval getPromotionCodeApproval(EntityManager em, String hash) {
        Query q = em.createNativeQuery("select * from promotion_code_approval where approval_hash=?", PromotionCodeApproval.class);
        q.setParameter(1, hash);
        return (PromotionCodeApproval) q.getSingleResult();
    }

    public static List<Object[]> getYesterdaysPendingConsolidatedUnitCreditData(EntityManager em) {

        // Results will have:
        // Unit credit instance id
        // ITEM_NUMBER
        // Amount Cents
        Query q = em.createNativeQuery("select UCI.UNIT_CREDIT_INSTANCE_ID, "
                + "SUBSTRING_INDEX(SUBSTRING_INDEX(UCS.CONFIGURATION , 'ConsolidatedGiftItemNumber=', -1),'\\r\\n',1) as ITEM_NUMBER, "
                + "UCI.POS_CENTS_DISCOUNT - UCI.POS_CENTS_CHARGED as CENTS "
                + "from unit_credit_instance UCI join unit_credit_specification UCS on UCI.UNIT_CREDIT_SPECIFICATION_ID = UCS.UNIT_CREDIT_SPECIFICATION_ID "
                + "where UCI.SALE_ROW_ID=-1 and UCI.PURCHASE_DATE >= CURDATE() - interval 1 day and UCI.PURCHASE_DATE < CURDATE()");
        return q.getResultList();
    }

    public static void updateUnitCreditsWithSaleLineId(EntityManager em, int saleLineId, List<Integer> ucIds, int currentLineId) throws Exception {
        StringBuilder list = new StringBuilder();
        for (int ucId : ucIds) {
            list.append(ucId).append(",");
        }
        list.setLength(list.length() - 1); //Remove trailing ,
        log.debug("List of ucIds is [{}]", list);
        Query q = em.createNativeQuery("update unit_credit_instance set SALE_ROW_ID=? where UNIT_CREDIT_INSTANCE_ID IN (" + list.toString() + ") and SALE_ROW_ID=?");
        q.setParameter(1, saleLineId);
        q.setParameter(2, currentLineId);
        int updated = q.executeUpdate();
        log.debug("Finished updating list of unit credits");
        if (updated != ucIds.size()) {
            throw new Exception("Somehow the wrong number of unit credits were updated -- " + updated + " instead of " + ucIds.size());
        }
    }

    public static int getParentSaleRowId(EntityManager em, String itemNumber, String serialNumber, int soldToOrganisationId) {
        Query q = em.createNativeQuery("select R.PARENT_SALE_ROW_ID from sale_row R join sale S on S.sale_id = R.sale_id "
                + "where R.SERIAL_NUMBER=? and R.ITEM_NUMBER=? and S.STATUS in ('PD') and S.RECIPIENT_ORGANISATION_ID=?");
        q.setParameter(1, serialNumber);
        q.setParameter(2, itemNumber);
        q.setParameter(3, soldToOrganisationId);
        return (int) q.getSingleResult();
    }

    public static boolean checkIfStockLocationIsLocked(EntityManager em, String stockLocation) {
        Query q = em.createNativeQuery("select * from x3_offline_dimensions where TYPE='CHA' and CODE=? and X3_STATUS='2';");
        q.setParameter(1, stockLocation);

        return (q.getResultList().size() > 0);
    }

    public static boolean isOnlyCustomerPermission(EntityManager em, int customerId) {

        Query q = em.createNativeQuery("select count(*) from security_group_membership where CUSTOMER_PROFILE_ID =?");
        q.setParameter(1, customerId);
        Object result = q.getSingleResult();
        log.debug("isOnlyCustomerPermission query result " + result);
        if (result == null || result.toString().equals("0")) {
            return false;
        }
        long permissions = (long) q.getSingleResult();
        return permissions == 1;
    }

     public static List<Integer> getAccountsActiveUnitCredits(EntityManager em, long accountId) {
        try {
            Query q = em.createNativeQuery("SELECT UCI.UNIT_CREDIT_SPECIFICATION_ID FROM "
                    + "unit_credit_instance UCI "
                    + "WHERE UCI.ACCOUNT_ID = ? "
                    + "AND UCI.EXPIRY_DATE > NOW()");
            q.setParameter(1, accountId);
            return q.getResultList();
        } catch (Exception e) {
            return null;
        }
    }
       
     public static Organisation getOrganisationByOrganisationId(EntityManager em, int organisationId) {
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.organisation WHERE ORGANISATION_ID=?", Organisation.class);
        q.setParameter(1, organisationId);
        return (Organisation) q.getSingleResult();
    }
        
    public static CustomerProfile getCustomerProfileById(EntityManager em, int customerId) {
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.customer_profile WHERE CUSTOMER_PROFILE_ID=? AND STATUS='AC'", CustomerProfile.class);
        q.setParameter(1, customerId);
        return (CustomerProfile) q.getSingleResult();
    }
}
