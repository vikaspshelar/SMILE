/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos;

import com.adonix.www.WSS.CAdxCallContext;
import com.adonix.www.WSS.CAdxMessage;
import com.adonix.www.WSS.CAdxParamKeyValue;
import com.adonix.www.WSS.CAdxResultXml;
import com.adonix.www.WSS.CAdxTechnicalInfos;
import com.adonix.www.WSS.CAdxWebServiceXmlCC;
import com.adonix.www.WSS.CAdxWebServiceXmlCCServiceLocator;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.Organisation;
import com.smilecoms.commons.sca.OrganisationList;
import com.smilecoms.commons.sca.OrganisationQuery;
import com.smilecoms.commons.sca.ProductInstanceList;
import com.smilecoms.commons.sca.ProductInstanceQuery;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.ServiceInstanceList;
import com.smilecoms.commons.sca.ServiceInstanceQuery;
import com.smilecoms.commons.sca.StOrganisationLookupVerbosity;
import com.smilecoms.commons.sca.StProductInstanceLookupVerbosity;
import com.smilecoms.commons.sca.StServiceInstanceLookupVerbosity;
import com.smilecoms.commons.base.cache.CacheHelper;
import com.smilecoms.commons.sca.AVP;
import com.smilecoms.commons.sca.ServiceSpecification;
import com.smilecoms.commons.sca.StUnitCreditSpecificationLookupVerbosity;
import com.smilecoms.commons.sca.UnitCreditSpecification;
import com.smilecoms.commons.sca.UnitCreditSpecificationList;
import com.smilecoms.commons.sca.UnitCreditSpecificationQuery;
import com.smilecoms.commons.sca.beans.NonUserSpecificCachedDataHelper;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.pos.db.model.AccountHistory;
import com.smilecoms.pos.db.model.CashIn;
import com.smilecoms.pos.db.model.CashInRow;
import com.smilecoms.pos.db.model.ReturnReplacement;
import com.smilecoms.pos.db.model.Sale;
import com.smilecoms.pos.db.model.SaleReturn;
import com.smilecoms.pos.db.model.SaleReturnRow;
import com.smilecoms.pos.db.model.SaleRow;
import com.smilecoms.pos.db.model.X3OfflineSubitems;
import com.smilecoms.pos.db.model.X3RequestState;
import com.smilecoms.pos.db.model.X3TransactionState;
import com.smilecoms.pos.db.op.DAO;
import com.smilecoms.xml.pos.POSError;
import com.smilecoms.xml.schema.pos.InventoryItem;
import com.smilecoms.xml.schema.pos.SaleLine;
import com.smilecoms.xml.schema.pos.UpSizeInventoryQuery;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.apache.axis.client.Stub;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * HERE IS WHAT I RAN TO GENERATE THE X3 JAXRPC STUBS
 *
 * paul@paul-hp ~/NetBeansProjects/HOBIT_2.03/javaroot/lib/axis $ java -cp
 * ./axis.jar:./commons-discovery-0.2.jar:jaxrpc.jar:saaj.jar:wsdl4j-1.5.1.jar:../commonsLogging/commons-logging-1.1.jar
 * org.apache.axis.wsdl.WSDL2Java
 * ../../../../../Downloads/CAdxWebServiceXmlCC.wsdl
 *
 * Take note though that we have code changes on CAdxResultXml.java that must be
 * reapplied f the stubs are built again
 */
/**
 * This class has all the methods that map directly to the functionality X3
 * exposes to us
 *
 * @author paul
 */
public class X3Helper {

    private static final Logger log = LoggerFactory.getLogger(X3Helper.class);
    public static final int RESULT_CODE_SYSTEM_ERROR = 100000;
    public static final int RESULT_CODE_SUCCESS = 1;
    public static final int RESULT_CODE_PROCESSED_BEFORE = 200000;
    public static final int RESULT_CODE_RECORD_EXISTS = 200001;
    public static final String REQUEST_TYPE_INVOICE_AND_PAYMENT = "IP";
    public static final String REQUEST_TYPE_QUOTE = "QT";
    public static final String REQUEST_TYPE_LOAN = "LN";
    public static final String REQUEST_TYPE_LOAN_COMPLETION = "LC";
    public static final String REQUEST_TYPE_SALES_ORDER = "SO";
    public static final String REQUEST_TYPE_INVOICE = "IV";
    public static final String REQUEST_TYPE_PAYMENT = "PA";
    public static final String REQUEST_TYPE_CREDIT_NOTE = "CN";
    public static final String REQUEST_TYPE_GL_ENTRY = "GL";
    public static final String REQUEST_TYPE_LINE_RETURN = "LR";
    private static CAdxWebServiceXmlCC x3Service = null;
    private static CAdxWebServiceXmlCC x3ShortTimeoutService = null;
    public static Properties props = null;
    private static CAdxCallContext CAdxCallContext = null;
    public static final int TYPE_QUOTE_WITH_STOCK_RESERVATION = 1;
    public static final int TYPE_CASH_SALE_WITH_RECEIPT = 2;
    public static final int TYPE_CREDIT_SALE_WITH_PENDING_PAYMENT = 3;
    public static final int TYPE_CREDIT_SALE_WITH_PAYMENT_PROCESS = 4;
    public static final int TYPE_EFT_WITH_PENDING_PAYMENT = 5;
    public static final int TYPE_EFT_SALE_WITH_PAYMENT_PROCESS = 6;
    public static final int TYPE_LOAN = 7;
    public static final int TYPE_LOAN_RETURN = 8;
    public static final int TYPE_PAYMENT = 9;
    public static final int TYPE_CREDIT_NOTE = 10;
    public static final int TYPE_GL = 11;
    public static final int TYPE_CREDIT_NOTE_SALE = 12;
    // public static final int TYPE_LINE_RETURN = 14;
    public static final int TYPE_AIRTIME_SALE_WITH_RECEIPT = 13;
    public static final int TYPE_PAYMENT_CARD_INTEGRATION = 15;
    public static final int TYPE_CARD_INTEGRATION_WITH_PENDING_PAYMENT = 16;

    public static final int TYPE_SALE_RETURN = 14;
    public static final int AMOUNT_TYPE_AVAILABLE_BALANCE = 1; // AVAILABLE_BALANCE = CREDIT_LIMIT - CURRENT_BALANCE (How much is still available to spend)
    public static final int AMOUNT_TYPE_CREDIT_LIMIT = 2;      // The maximum a credit limit for a credit customer, if credit limit is 0, it means no control on the customer and therefore they can buy anything
    public static final int AMOUNT_TYPE_CURRENT_BALANCE = 3;   // How much a credit customer has spent so far, this is defined as the total value of all invoices made to the customer up to now

    /**
     * ********************************************
     *
     * WEB SERVICE HELPERS
     *
     *********************************************
     */
    static CAdxResultXml createInvoice(EntityManager em, EntityManagerFactory emf, X3TransactionState transactionState, Sale sale, Collection<SaleRow> salesRows, int txType) throws Exception {
        log.debug("In createInvoice");
        CAdxResultXml result;
        if (txType == TYPE_AIRTIME_SALE_WITH_RECEIPT) {
            throw new UnsupportedOperationException("TYPE_AIRTIME_SALE_WITH_RECEIPT is not supported yet");
        }

        X3RequestState requestState = DAO.createX3RequestInOwnTransactionScope(emf, transactionState, REQUEST_TYPE_INVOICE_AND_PAYMENT, X3InterfaceDaemon.X3_TRANSACTION_STATUS_IN_PROGRESS);
        if (requestState.getStatus().equals(X3InterfaceDaemon.X3_TRANSACTION_STATUS_FINISHED)) {
            log.warn("This request has been processed already in X3. We wont send it again");
            result = new CAdxResultXml();
            result.setStatus(RESULT_CODE_PROCESSED_BEFORE);
            result.setX3RecordId(requestState.getX3RecordId());
            return result;
        }

        X3DataStream ds = new X3DataStream();

        if (X3Helper.isSaleGeneratedFromQuote(sale.getExtraInfo())) { // This is a Cash or Credit Sale from a Quote
            log.debug("Sale generated from a quote, EXTRA_INFO field value [{}] will call ZSEP modify() in X3", sale.getExtraInfo());
            ds.addGroup("ZSE0_1");
            ds.getLastGroup().addField("SEPID", props.getProperty("CountrySiteCode") + sale.getSaleId()); // SEP Unique Id
            ds.addGroup("ZSE1_2");
            ds.getLastGroup().addField("SEPTYP", txType);
            CAdxParamKeyValue[] objKey = new CAdxParamKeyValue[1];
            CAdxParamKeyValue sepId = new CAdxParamKeyValue();
            sepId.setKey("SEPID");
            sepId.setValue(props.getProperty("CountrySiteCode") + String.valueOf(sale.getSaleId()));
            objKey[0] = sepId;

            result = modify("ZSEP", objKey, ds.getXML());

        } else { // Cash or Credit Sale without a Quote

            ds.addGroup("ZSE0_1");

            ds.getLastGroup().addField("SEPID", props.getProperty("CountrySiteCode") + sale.getSaleId()); // SEP Unique Id
            ds.getLastGroup().addField("BPCNUM", getPartnerCode(em, sale));
            ds.getLastGroup().addField("CUR", sale.getTenderedCurrency());
            ds.getLastGroup().addField("SALFCY", props.getProperty("CountrySiteCode"));
            // PCB - As requested by finance. Use the date we gave the value for payment gateway transactions
            ds.getLastGroup().addField("ORDDAT", sale.getPaymentMethod().equals(POSManager.PAYMENT_METHOD_CLEARING_BUREAU) ? sale.getLastModified() : sale.getSaleDateTime());
            ds.getLastGroup().addField("CUSORDREF", (sale.getPurchaseOrderData() == null) ? "" : sale.getPurchaseOrderData());
            ds.getLastGroup().addField("SOHTYP", "SOI"); // Sales order type
            ds.getLastGroup().addField("SOHNUM", ""); // Return field. Sales Order Number
            ds.getLastGroup().addField("WHTAMT", sale.getWithholdingTaxCents().doubleValue() / 100); //Invoice withholding tax

            ds.addGroup("ZSE1_2");
            ds.getLastGroup().addField("SEPTYP", txType);
            ds.getLastGroup().addField("SEPSTA", 1); // Unused for now

            ds.addGroup("ZSE1_3");
            ds.getLastGroup().addField("INVDAT", sale.getPaymentMethod().equals(POSManager.PAYMENT_METHOD_CLEARING_BUREAU) ? sale.getLastModified() : sale.getSaleDateTime());
            ds.getLastGroup().addField("SIHNUM", ""); // Invoice number - returned
            ds.getLastGroup().addField("INVTYP", 1);  // Invoice type - returned. 1 = Invoice 2 = credit note
            ds.getLastGroup().addField("PAYDAT", ""); // Payment date. Leave empty so it defaults to today
            ds.getLastGroup().addField("SHIDAT", ""); // Shipment date. Leave empty so it defaults to today
            ds.getLastGroup().addField("SDHNUM", ""); // Loan Shipment number - returned
            ds.getLastGroup().addField("RTNDAT", ""); // Return date. Leave empty so it defaults to today
            ds.getLastGroup().addField("SRHNUM", ""); // Loan Return number - returned
            ds.getLastGroup().addField("PAYREF", props.getProperty("CountryPrefix") + sale.getSaleId()); //Payment reference to be used by customers when making a payment at the bank.
//            ds.getLastGroup().addField("INVREF_0", sale.getExtTxid());
            if (sale.getPaymentMethod().equals(POSManager.PAYMENT_METHOD_CREDIT_NOTE)) {
                log.debug("Payment method is by credit note. Our credit note id is [{}] looking up X3 credit note number", sale.getPaymentTransactionData());
                String x3CreditNoteNumber = DAO.getX3RecordId(em,
                        X3InterfaceDaemon.TABLE_SALE_RETURN,
                        Integer.parseInt(sale.getPaymentTransactionData()),
                        REQUEST_TYPE_CREDIT_NOTE);
                log.debug("X3 Credit Note Number is [{}]", x3CreditNoteNumber);
                if (x3CreditNoteNumber == null || x3CreditNoteNumber.isEmpty()) {
                    log.warn("No X3 Record Id for Credit Note Number [{}]", sale.getPaymentTransactionData());
                    result = new CAdxResultXml();
                    result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_ERROR);
                    addMessageIntoResult(result, "No X3 Record Id for Credit Note Number " + sale.getPaymentTransactionData(), "X3 error");
                    DAO.setX3RequestFinishedInOwnTransactionScope(emf, requestState, result);
                    throwExceptionIfError(result);
                }
                ds.getLastGroup().addField("PYHNUM", x3CreditNoteNumber); // X3 knows that the credit note will be in this field
            } else {
                ds.getLastGroup().addField("PYHNUM", "");
            }

            boolean vatExempt = sale.getTaxExempt().equals("Y");

            //         BigDecimal txFeesPerRow = sale.getTransactionFeeCents() == null ? null : sale.getTransactionFeeCents().divide(new BigDecimal(100)).divide(new BigDecimal(salesRows.size()), RoundingMode.HALF_EVEN);
            ds.addTable("ZSE1_1");
            for (SaleRow row : salesRows) {
                boolean hasSubItems = doesItemHaveSubItems(row, salesRows); // kitted items should not have any pricing on them when sending to X3. Only the sub items would have the pricing
                ds.getLastTable().addLine();
                ds.getLastTable().getLastLine().addField("ITMREF", row.getItemNumber()); // Item number
                ds.getLastTable().getLastLine().addField("SAU", "EA"); // Unit of measure
                ds.getLastTable().getLastLine().addField("QTY", row.getQuantity());
                ds.getLastTable().getLastLine().addField("LOT", ""); // Lot number. Send as blank. If LOT is used, will be returned
                ds.getLastTable().getLastLine().addField("SERNUM", row.getSerialNumber());
                ds.getLastTable().getLastLine().addField("LOCTYP", ""); // Send as blank. If used, will be returned
                ds.getLastTable().getLastLine().addField("LOC", ""); // Send as blank. If used, will be returned
                ds.getLastTable().getLastLine().addField("GROPRI", hasSubItems ? 0 : (row.getTotalCentsIncl().add(row.getTotalDiscountOnInclCents()).doubleValue() / (100l * row.getQuantity()))); // Per item price after tax before discounting
                ds.getLastTable().getLastLine().addField("NETPRI", 0); // Not used
                ds.getLastTable().getLastLine().addField("LINAMT", 0); // Not used
                ds.getLastTable().getLastLine().addField("LINATI", 0); // Not used
                ds.getLastTable().getLastLine().addField("CCE1", sale.getChannel()); // Dimension 1 = channel
                ds.getLastTable().getLastLine().addField("CCE2", sale.getSaleLocation()); // Dimension 2 = province/state

                String department = Utils.getValueFromCRDelimitedAVPString(sale.getExtraInfo(), "Department");
                if (department != null && !department.isEmpty()) {
                    ds.getLastTable().getLastLine().addField("CCE3", department); // Dimension 3 = Department
                } else {
                    ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); // Dimension 3 = Department
                }
                if (!sale.getOrganisationChannel().isEmpty() && !BaseUtils.getProperty("env.pos.dimension.code.organisationchannel", "").isEmpty()) {
                    // TODO : Dont use property when we know the dimension code to use
                    ds.getLastTable().getLastLine().addField(BaseUtils.getProperty("env.pos.dimension.code.organisationchannel"), sale.getOrganisationChannel());
                }
                ds.getLastTable().getLastLine().addField("SOPLIN", ""); // X3 Sales order line number - returned
                ds.getLastTable().getLastLine().addField("SIDLIN", ""); // X3 Invoice line number - returned
                ds.getLastTable().getLastLine().addField("SDDLIN", ""); // X3 Loan shipment line number - returned
                ds.getLastTable().getLastLine().addField("SRDLIN", ""); // X3 Loan return line number - returned
                ds.getLastTable().getLastLine().addField("RTNQTY", 0); //Return Quantity: RTNQTY: All transaction should be 0, except for Credit Notes and Loan Returns 
                populateDiscount(ds, hasSubItems, row, sale);
                ds.getLastTable().getLastLine().addField("FLGVAT", vatExempt ? "1" : "2");
//                ds.getLastTable().getLastLine().addField("OTTAMT", Utils.getDoubleValueFromCRDelimitedAVPString(sale.getExtraInfo(), "DEBIT_AMOUNT1"));
//                ds.getLastTable().getLastLine().addField("SMTAMT", Utils.getDoubleValueFromCRDelimitedAVPString(sale.getExtraInfo(), "SOCIAL_MEDIA_TAX"));

//                if (txFeesPerRow != null) {
//                    log.debug("Populating row with TX fees of [{}]", txFeesPerRow);
//                    ds.getLastTable().getLastLine().addField("DISCRGVAL4", txFeesPerRow);
//                }
            }

            result = save("ZSEP", ds.getXML());
        }

        result.setX3RecordId(getX3RecordIdNumber(result.getResultXml(), txType));
        if (result.getStatus() != RESULT_CODE_RECORD_EXISTS && result.getX3RecordId() == null) {
            log.warn("No invoice number returned so there must have been an error");
            result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_ERROR);
            addMessageIntoResult(result, "No invoice was created", "X3 error");
        }

        DAO.setX3RequestFinishedInOwnTransactionScope(emf, requestState, result);

        throwExceptionIfError(result);

        log.debug("Finished createInvoice");
        return result;
    }

    static CAdxResultXml createPayment(EntityManager em, EntityManagerFactory emf, X3TransactionState transactionState, Sale sale, int txType) throws Exception {
        log.debug("In createPayment");
        CAdxResultXml result;

        X3RequestState requestState = DAO.createX3RequestInOwnTransactionScope(emf, transactionState, REQUEST_TYPE_PAYMENT, X3InterfaceDaemon.X3_TRANSACTION_STATUS_IN_PROGRESS);
        if (requestState.getStatus().equals(X3InterfaceDaemon.X3_TRANSACTION_STATUS_FINISHED)) {
            log.warn("This request has been processed already in X3. We wont send it again");
            result = new CAdxResultXml();
            result.setStatus(RESULT_CODE_PROCESSED_BEFORE);
            result.setX3RecordId(requestState.getX3RecordId());
            return result;
        }

        X3DataStream ds = new X3DataStream();
        ds.addGroup("ZSE0_1");
        ds.getLastGroup().addField("SEPID", props.getProperty("CountrySiteCode") + sale.getSaleId()); // SEP Unique Id
        ds.addGroup("ZSE1_2");
        ds.getLastGroup().addField("SEPTYP", txType);
        ds.addGroup("ZSE1_3");
        String ptd = sale.getPaymentTransactionData();
        ptd = StringEscapeUtils.escapeXml(ptd);
        if (ptd != null && ptd.length() > 100) {
            ptd = ptd.substring(0, 100); // X3 only allows 100 characters for this
        }
        ds.getLastGroup().addField("PAYREF", ptd);
        ds.getLastGroup().addField("PAYDAT", new Date());

        CAdxParamKeyValue[] objKey = new CAdxParamKeyValue[1];
        CAdxParamKeyValue sepId = new CAdxParamKeyValue();
        sepId.setKey("SEPID");
        sepId.setValue(props.getProperty("CountrySiteCode") + String.valueOf(sale.getSaleId()));
        objKey[0] = sepId;
        result = modify("ZSEP", objKey, ds.getXML());
        result.setX3RecordId(getX3RecordIdNumber(result.getResultXml(), txType));
        if (result.getX3RecordId() == null) {
            log.warn("No payment number returned so there must have been an error");
            result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_ERROR);
            addMessageIntoResult(result, "No payment number created", "X3 error");
        }
        DAO.setX3RequestFinishedInOwnTransactionScope(emf, requestState, result);
        throwExceptionIfError(result);
        log.debug("Finished createPayment");
        return result;
    }

    static CAdxResultXml createLineReturn(EntityManager em, EntityManagerFactory emf, X3TransactionState transactionState, ReturnReplacement dbReturn, int txType) throws Exception {
        log.debug("In createLineReturn");
        CAdxResultXml result;

        // Retrieve the original sale for this return so we can populate other required fields for X3
        com.smilecoms.pos.db.model.Sale sale = DAO.getSaleByLineId(em, dbReturn.getSaleRowId());

        X3RequestState requestState = DAO.createX3RequestInOwnTransactionScope(emf, transactionState, REQUEST_TYPE_LINE_RETURN, X3InterfaceDaemon.X3_TRANSACTION_STATUS_IN_PROGRESS);
        if (requestState.getStatus().equals(X3InterfaceDaemon.X3_TRANSACTION_STATUS_FINISHED)) {
            log.warn("This request has been processed already in X3. We wont send it again");
            result = new CAdxResultXml();
            result.setStatus(RESULT_CODE_PROCESSED_BEFORE);
            result.setX3RecordId(requestState.getX3RecordId());
            return result;
        }

        X3DataStream ds = new X3DataStream();

        ds.addGroup("ZSE0_1");

        ds.getLastGroup().addField("SEPID", props.getProperty("CountrySiteCode") + dbReturn.getReturnReplacementId()); // SEP Unique Id
        ds.getLastGroup().addField("BPCNUM", getPartnerCode(em, sale));
        ds.getLastGroup().addField("CUR", sale.getTenderedCurrency());
        ds.getLastGroup().addField("SALFCY", props.getProperty("CountrySiteCode"));
        // PCB - As requested by finance. Use the date we gave the value for payment gateway transactions
        ds.getLastGroup().addField("ORDDAT", sale.getPaymentMethod().equals(POSManager.PAYMENT_METHOD_CLEARING_BUREAU) ? sale.getLastModified() : sale.getSaleDateTime());
        ds.getLastGroup().addField("CUSORDREF", (sale.getPurchaseOrderData() == null) ? "" : sale.getPurchaseOrderData());
        ds.getLastGroup().addField("SOHTYP", "SOI"); // Sales order type
        ds.getLastGroup().addField("SOHNUM", ""); // Return field. Sales Order Number
        ds.getLastGroup().addField("WHTAMT", sale.getWithholdingTaxCents().doubleValue() / 100); //Invoice withholding tax

        ds.addGroup("ZSE1_2");
        ds.getLastGroup().addField("SEPTYP", txType);
        ds.getLastGroup().addField("SEPSTA", 1); // Unused for now

        ds.addGroup("ZSE1_3");
        ds.getLastGroup().addField("INVDAT", sale.getPaymentMethod().equals(POSManager.PAYMENT_METHOD_CLEARING_BUREAU) ? sale.getLastModified() : sale.getSaleDateTime());
        ds.getLastGroup().addField("SIHNUM", ""); // Invoice number - returned
        ds.getLastGroup().addField("INVTYP", 1);  // Invoice type - returned. 1 = Invoice 2 = credit note
        ds.getLastGroup().addField("PAYDAT", ""); // Payment date. Leave empty so it defaults to today
        ds.getLastGroup().addField("SHIDAT", ""); // Shipment date. Leave empty so it defaults to today
        ds.getLastGroup().addField("SDHNUM", ""); // Loan Shipment number - returned
        ds.getLastGroup().addField("RTNDAT", ""); // Return date. Leave empty so it defaults to today
        ds.getLastGroup().addField("SRHNUM", ""); // Loan Return number - returned
        ds.getLastGroup().addField("PAYREF", ""); //Payment reference to be used by customers when making a payment at the bank.
//            ds.getLastGroup().addField("INVREF_0", sale.getExtTxid());
        ds.getLastGroup().addField("PYHNUM", "");

        boolean vatExempt = sale.getTaxExempt().equals("Y");

        //BigDecimal txFeesPerRow = sale.getTransactionFeeCents() == null ? null : sale.getTransactionFeeCents().divide(new BigDecimal(100)).divide(new BigDecimal(salesRows.size()), RoundingMode.HALF_EVEN);
        ds.addTable("ZSE1_1");
        ds.getLastTable().addLine();
        ds.getLastTable().getLastLine().addField("ITMREF", dbReturn.getReturnedItemNumber()); // Item number
        ds.getLastTable().getLastLine().addField("SAU", "UN"); // Unit of measure
        ds.getLastTable().getLastLine().addField("QTY", 1);
        ds.getLastTable().getLastLine().addField("LOT", ""); // Lot number. Send as blank. If LOT is used, will be returned
        ds.getLastTable().getLastLine().addField("SERNUM", dbReturn.getReturnedSerialNumber());
        ds.getLastTable().getLastLine().addField("LOCTYP", ""); // Send as blank. If used, will be returned
        ds.getLastTable().getLastLine().addField("LOC", dbReturn.getLocation()); // Send as blank. If used, will be returned
        ds.getLastTable().getLastLine().addField("GROPRI", 0); // Per item price after tax before discounting
        ds.getLastTable().getLastLine().addField("NETPRI", 0); // Not used
        ds.getLastTable().getLastLine().addField("LINAMT", 0); // Not used
        ds.getLastTable().getLastLine().addField("LINATI", 0); // Not used
        ds.getLastTable().getLastLine().addField("CCE1", sale.getChannel()); // Dimension 1 = channel
        ds.getLastTable().getLastLine().addField("CCE2", sale.getSaleLocation()); // Dimension 2 = province/state

        String department = Utils.getValueFromCRDelimitedAVPString(sale.getExtraInfo(), "Department");
        if (department != null && !department.isEmpty()) {
            ds.getLastTable().getLastLine().addField("CCE3", department); // Dimension 3 = Department
        } else {
            ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); // Dimension 3 = Department
        }
        if (!sale.getOrganisationChannel().isEmpty() && !BaseUtils.getProperty("env.pos.dimension.code.organisationchannel", "").isEmpty()) {
            // TODO : Dont use property when we know the dimension code to use
            ds.getLastTable().getLastLine().addField(BaseUtils.getProperty("env.pos.dimension.code.organisationchannel"), sale.getOrganisationChannel());
        }

        ds.getLastTable().getLastLine().addField("SOPLIN", ""); // X3 Sales order line number - returned
        ds.getLastTable().getLastLine().addField("SIDLIN", ""); // X3 Invoice line number - returned
        ds.getLastTable().getLastLine().addField("SDDLIN", ""); // X3 Loan shipment line number - returned
        ds.getLastTable().getLastLine().addField("SRDLIN", ""); // X3 Loan return line number - returned
        ds.getLastTable().getLastLine().addField("RTNQTY", 0); //Return Quantity: RTNQTY: All transaction should be 0, except for Credit Notes and Loan Returns 
        ds.getLastTable().getLastLine().addField("RETTYPE", "QC"); //Return Quantity: RTNQTY: All transaction should be 0, except for Credit Notes and Loan Returns 
        ds.getLastTable().getLastLine().addField("FLGVAT", vatExempt ? "1" : "2");
//        ds.getLastTable().getLastLine().addField("OTTAMT", getBigDecimalFieldValueFromExtraInfo(sale.getExtraInfo(), "DEBIT_AMOUNT1"));
//        ds.getLastTable().getLastLine().addField("SMTAMT", getBigDecimalFieldValueFromExtraInfo(sale.getExtraInfo(), "SOCIAL_MEDIA_TAX"));

        result = save("ZSEP", ds.getXML());

        result.setX3RecordId(getX3RecordIdNumber(result.getResultXml(), txType));
        if (result.getStatus()
                != RESULT_CODE_RECORD_EXISTS && result.getX3RecordId() == null) {
            log.warn("No invoice number returned so there must have been an error");
            result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_ERROR);
            addMessageIntoResult(result, "No invoice was created", "X3 error");
        }

        DAO.setX3RequestFinishedInOwnTransactionScope(emf, requestState, result);

        throwExceptionIfError(result);

        log.debug("Finished createLineReturn");
        return result;
    }

    static CAdxResultXml createLineReplacement(EntityManager em, EntityManagerFactory emf, X3TransactionState transactionState, ReturnReplacement dbReturn, int txType) throws Exception {
        log.debug("In createLineReplacement");
        CAdxResultXml result = null;

        // Retrieve the original sale for this return so we can populate other required fields for X3
        com.smilecoms.pos.db.model.Sale sale = DAO.getSaleByLineId(em, dbReturn.getSaleRowId());

        X3RequestState requestState = DAO.createX3RequestInOwnTransactionScope(emf, transactionState, REQUEST_TYPE_LINE_RETURN, X3InterfaceDaemon.X3_TRANSACTION_STATUS_IN_PROGRESS);
        if (requestState.getStatus().equals(X3InterfaceDaemon.X3_TRANSACTION_STATUS_FINISHED)) {
            log.warn("This request has been processed already in X3. We wont send it again");
            result = new CAdxResultXml();
            result.setStatus(RESULT_CODE_PROCESSED_BEFORE);
            result.setX3RecordId(requestState.getX3RecordId());
            return result;
        }

        X3DataStream ds = new X3DataStream();

        log.warn("TODO: Awaiting mapping for sending replacement to X3 from Wessel at Synergy");

        DAO.setX3RequestFinishedInOwnTransactionScope(emf, requestState, result);

        throwExceptionIfError(result);

        log.debug("Finished createLineReplacement");
        return result;
    }

    public static CAdxResultXml createCreditNoteForInvoice(EntityManager em, LazyX3Connection lazyConn, EntityManagerFactory emf, X3TransactionState transactionState, SaleReturn dbReturn, Collection<SaleReturnRow> returnRows) throws Exception {
        log.debug("In createCreditNoteForInvoice - for returns of specific items within a sale - to be proceeded with an invoice for the replacement that will be paid for by the credit note");
        CAdxResultXml result;

        // Get the X3 invoice number from when the invoice was created for the original sale
        log.debug("Getting invoice number from request [{}][{}][{}]",
                new Object[]{X3InterfaceDaemon.TABLE_SALE,
                    dbReturn.getSaleId(),
                    REQUEST_TYPE_INVOICE + ", " + REQUEST_TYPE_INVOICE_AND_PAYMENT});

        X3RequestState requestState = DAO.createX3RequestInOwnTransactionScope(emf, transactionState, REQUEST_TYPE_CREDIT_NOTE, X3InterfaceDaemon.X3_TRANSACTION_STATUS_IN_PROGRESS);
        if (requestState.getStatus().equals(X3InterfaceDaemon.X3_TRANSACTION_STATUS_FINISHED)) {
            log.warn("This request has been processed already in X3. We wont send it again");
            result = new CAdxResultXml();
            result.setStatus(RESULT_CODE_PROCESSED_BEFORE);
            result.setX3RecordId(requestState.getX3RecordId());
            return result;
        }

        Sale sale = DAO.getSale(em, dbReturn.getSaleId()); // Retrieve the original sale
        Collection<SaleRow> salesRows = DAO.getSalesRowsAndSubRows(em, sale.getSaleId());

        X3DataStream ds = new X3DataStream();
        ds.addGroup("ZSE0_1");
        ds.getLastGroup().addField("SEPID", props.getProperty("CountrySiteCode") + sale.getSaleId()); // SEP Unique Id

        ds.addGroup("ZSE1_2");
        ds.getLastGroup().addField("SEPTYP", TYPE_CREDIT_NOTE); //10

        ds.addGroup("ZSE1_3");
        ds.getLastGroup().addField("XCRNDAT", dbReturn.getSaleReturnDateTime()); // Date the return was processed on

        ds.addTable("ZSE1_1");

        String x3CreditNoteNumber = null;

        for (SaleReturnRow rRow : returnRows) {
            int rowIdBeingReturned = rRow.getSaleRowId();
            int origLineNumberOnInvoice = 0;
            long origQuantity = 0;
            for (SaleRow origRow : salesRows) {
                origLineNumberOnInvoice++;
                if (origRow.getSaleRowId() == rowIdBeingReturned) {
                    origQuantity = origRow.getQuantity();
                    if (POSManager.isP2PCalendarInvoicingOn(origRow)) {
                        origQuantity = rRow.getReturnedQuantity();
                    }
                    break;
                }
            }

            // Check if a credit note already exists in X3.
            x3CreditNoteNumber = getX3CreditNoteNumber(lazyConn, props.getProperty("CountrySiteCode") + sale.getSaleId(), origLineNumberOnInvoice);

            if (x3CreditNoteNumber != null) {
                break; // A credit note already exists - no need to loop further ...
            }

            ds.getLastTable().addLine(origLineNumberOnInvoice);
            ds.getLastTable().getLastLine().addField("RTNQTY", origQuantity); //Return Quantity: RTNQTY: All transaction should be 0, except for Credit Notes and Loan Returns 
            ds.getLastTable().getLastLine().addField("RTNTYP", 2); // 2 = Sales Return. The stock item that is returned will go in to status "Q" to the location (sub-warehouse) specified in the RTNLOC field
            ds.getLastTable().getLastLine().addField("RTNLOC", dbReturn.getReturnLocation()); // Return Location (on line level)  -    If RTNTYP = 2, then this field is mandatory. It should contain the location (sub-warehouse) of where the stock was receipted.
            ds.getLastTable().getLastLine().addField("CCE1", sale.getChannel()); // CCE1 dimension file must be origional sales channel as per Mike and Mary on 7/9/2015
            // Mukosi: Added the below field as requested by AccTech to solve the multiple credit note on the same sale problem.
            ds.getLastTable().getLastLine().addField("RTNFLG", 1);
        }

        if (x3CreditNoteNumber == null) { // No credit note exists in X3 yet ...
            CAdxParamKeyValue[] objKey = new CAdxParamKeyValue[1];
            CAdxParamKeyValue sepId = new CAdxParamKeyValue();
            sepId.setKey("SEPID");
            sepId.setValue(props.getProperty("CountrySiteCode") + String.valueOf(sale.getSaleId()));
            objKey[0] = sepId;
            result = modify("ZSEP", objKey, ds.getXML());
            result.setX3RecordId(getX3RecordIdNumber(result.getResultXml(), TYPE_CREDIT_NOTE));
        } else { // A credit note already exists in X3 ...
            result = new CAdxResultXml();
            CAdxMessage msg = new CAdxMessage();
            msg.setType("INFO");
            msg.setMessage("Credit note already exists in X3  - X3 Credit Note Number is [" + x3CreditNoteNumber + "].");
            result.setMessages(new CAdxMessage[]{msg});
            result.setX3RecordId(x3CreditNoteNumber);
            result.setStatusCode("FI");
            result.setStatus(1010);
            result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_FINISHED);
        }

        if (result.getX3RecordId() == null) {
            log.warn("No credit note number returned so there must have been an error");
            result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_ERROR);
            addMessageIntoResult(result, "No credit note was created", "X3 error");
        }
        DAO.setX3RequestFinishedInOwnTransactionScope(emf, requestState, result);
        throwExceptionIfError(result);
        log.debug("Finished createCreditNoteForInvoice - for returns");
        return result;
    }

    public static CAdxResultXml createCreditNoteForInvoiceNEW(EntityManager em, LazyX3Connection lazyConn, EntityManagerFactory emf, X3TransactionState transactionState, SaleReturn dbReturn, Collection<SaleReturnRow> returnRows) throws Exception {
        log.debug("In createCreditNoteForInvoiceNEW - for returns of specific items within a sale - to be proceeded with an invoice for the replacement that will be paid for by the credit note");
        CAdxResultXml result;

        // Get the X3 invoice number from when the invoice was created for the original sale
        log.debug("Getting invoice number from request [{}][{}][{}]",
                new Object[]{X3InterfaceDaemon.TABLE_SALE,
                    dbReturn.getSaleId(),
                    REQUEST_TYPE_INVOICE + ", " + REQUEST_TYPE_INVOICE_AND_PAYMENT});

        X3RequestState requestState = DAO.createX3RequestInOwnTransactionScope(emf, transactionState, REQUEST_TYPE_CREDIT_NOTE, X3InterfaceDaemon.X3_TRANSACTION_STATUS_IN_PROGRESS);
        if (requestState.getStatus().equals(X3InterfaceDaemon.X3_TRANSACTION_STATUS_FINISHED)) {
            log.warn("This request has been processed already in X3. We wont send it again");
            result = new CAdxResultXml();
            result.setStatus(RESULT_CODE_PROCESSED_BEFORE);
            result.setX3RecordId(requestState.getX3RecordId());
            return result;
        }

        Sale sale = DAO.getSale(em, dbReturn.getSaleId()); // Retrieve the original sale
        Collection<SaleRow> salesRows = DAO.getSalesRowsAndSubRows(em, sale.getSaleId());

        X3DataStream ds = new X3DataStream();
        ds.addGroup("ZSE0_1");
        ds.getLastGroup().addField("SEPID", props.getProperty("CountrySiteCode") + sale.getSaleId()); // SEP Unique Id

        ds.addGroup("ZSE1_2");
        ds.getLastGroup().addField("SEPTYP", TYPE_SALE_RETURN); //14

        ds.addGroup("ZSE1_3");
        ds.getLastGroup().addField("XCRNDAT", dbReturn.getSaleReturnDateTime()); // Date the return was processed on

        ds.addTable("ZSE1_1");

        String x3CreditNoteNumber = null;

        for (SaleReturnRow rRow : returnRows) {
            int rowIdBeingReturned = rRow.getSaleRowId();
            int origLineNumberOnInvoice = 0;
            long origQuantity = 0;
            for (SaleRow origRow : salesRows) {
                origLineNumberOnInvoice++;
                if (origRow.getSaleRowId() == rowIdBeingReturned) {
                    origQuantity = origRow.getQuantity();
                    if (POSManager.isP2PCalendarInvoicingOn(origRow)) {
                        origQuantity = rRow.getReturnedQuantity();
                    }
                    break;
                }
            }

            // Check if a credit note already exists in X3.
            x3CreditNoteNumber = getX3CreditNoteNumber(lazyConn, props.getProperty("CountrySiteCode") + sale.getSaleId(), origLineNumberOnInvoice);

            if (x3CreditNoteNumber != null) {
                break; // A credit note already exists - no need to loop further ...
            }

            ds.getLastTable().addLine(origLineNumberOnInvoice);
            ds.getLastTable().getLastLine().addField("RTNQTY", origQuantity); //Return Quantity: RTNQTY: All transaction should be 0, except for Credit Notes and Loan Returns 
            ds.getLastTable().getLastLine().addField("RTNTYP", 2); // 2 = Sales Return. The stock item that is returned will go in to status "Q" to the location (sub-warehouse) specified in the RTNLOC field
            ds.getLastTable().getLastLine().addField("RTNLOC", dbReturn.getReturnLocation()); // Return Location (on line level)  -    If RTNTYP = 2, then this field is mandatory. It should contain the location (sub-warehouse) of where the stock was receipted.
            ds.getLastTable().getLastLine().addField("CCE1", sale.getChannel()); // CCE1 dimension file must be origional sales channel as per Mike and Mary on 7/9/2015
            // Mukosi: Added the below field as requested by AccTech to solve the multiple credit note on the same sale problem.
            ds.getLastTable().getLastLine().addField("RTNFLG", 1);
        }

        if (x3CreditNoteNumber == null) { // No credit note exists in X3 yet ...
            CAdxParamKeyValue[] objKey = new CAdxParamKeyValue[1];
            CAdxParamKeyValue sepId = new CAdxParamKeyValue();
            sepId.setKey("SEPID");
            sepId.setValue(props.getProperty("CountrySiteCode") + String.valueOf(sale.getSaleId()));
            objKey[0] = sepId;
            result = modify("ZSEP", objKey, ds.getXML());
            result.setX3RecordId(getX3RecordIdNumber(result.getResultXml(), TYPE_CREDIT_NOTE));
        } else { // A credit note already exists in X3 ...
            result = new CAdxResultXml();
            CAdxMessage msg = new CAdxMessage();
            msg.setType("INFO");
            msg.setMessage("Credit note already exists in X3  - X3 Credit Note Number is [" + x3CreditNoteNumber + "].");
            result.setMessages(new CAdxMessage[]{msg});
            result.setX3RecordId(x3CreditNoteNumber);
            result.setStatusCode("FI");
            result.setStatus(1010);
            result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_FINISHED);
        }

        if (result.getX3RecordId() == null) {
            log.warn("No credit note number returned so there must have been an error");
            result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_ERROR);
            addMessageIntoResult(result, "No credit note was created", "X3 error");
        }
        DAO.setX3RequestFinishedInOwnTransactionScope(emf, requestState, result);
        throwExceptionIfError(result);
        log.debug("Finished createCreditNoteForInvoice - for returns");
        return result;
    }

    public static CAdxResultXml createCreditNoteForInvoice(EntityManager em, LazyX3Connection lazyConn, EntityManagerFactory emf, X3TransactionState transactionState, Sale sale, Collection<SaleRow> salesRows) throws Exception {
        log.debug("In createCreditNoteForInvoice - for cancellation of an entire invoice");
        CAdxResultXml result;

        // Get the X3 invoice number from when the invoice was created for the original sale
        log.debug("Getting invoice number from request [{}][{}][{}]",
                new Object[]{X3InterfaceDaemon.TABLE_SALE,
                    sale.getSaleId(),
                    REQUEST_TYPE_INVOICE + ", " + REQUEST_TYPE_INVOICE_AND_PAYMENT});

        X3RequestState requestState = DAO.createX3RequestInOwnTransactionScope(emf, transactionState, REQUEST_TYPE_CREDIT_NOTE, X3InterfaceDaemon.X3_TRANSACTION_STATUS_IN_PROGRESS);
        if (requestState.getStatus().equals(X3InterfaceDaemon.X3_TRANSACTION_STATUS_FINISHED)) {
            log.warn("This request has been processed already in X3. We wont send it again");
            result = new CAdxResultXml();
            result.setStatus(RESULT_CODE_PROCESSED_BEFORE);
            result.setX3RecordId(requestState.getX3RecordId());
            return result;
        }

        X3DataStream ds = new X3DataStream();
        ds.addGroup("ZSE0_1");
        ds.getLastGroup().addField("SEPID", props.getProperty("CountrySiteCode") + sale.getSaleId()); // SEP Unique Id

        ds.addGroup("ZSE1_2");
        ds.getLastGroup().addField("SEPTYP", TYPE_CREDIT_NOTE); //10

        ds.addGroup("ZSE1_3");
        ds.getLastGroup().addField("XCRNDAT", new Date()); // Default credit note to today

        String x3CreditNoteNumber = null;
        String x3PaymentNumber;
        int origLineNumberOnInvoice = 0;
        boolean foundNonAirtimeLine = false;

        ds.addTable("ZSE1_1");
        for (SaleRow row : salesRows) {
            if (sale.getPaymentMethod().equals(POSManager.PAYMENT_METHOD_QUOTE) && (POSManager.isAirtime(row.getSerialNumber()) || POSManager.isUnitCredit(row))) {
                continue; // Do not send cancellation of AIRTIME and BUNDLE quotes to X3 - these are non-managed stock items.
            } else {

                int rowIdBeingReturned = row.getSaleRowId();
                origLineNumberOnInvoice = 0;
                long origQuantity = 0;
                for (SaleRow origRow : salesRows) {
                    origLineNumberOnInvoice++;
                    if (origRow.getSaleRowId() == rowIdBeingReturned) {
                        origQuantity = origRow.getQuantity();
                        if (POSManager.isP2PCalendarInvoicingOn(origRow)) {
                            try {
                                SaleReturnRow saleReturnRow = DAO.getSaleReturnRowBySaleRowId(em, rowIdBeingReturned);
                                origQuantity = saleReturnRow.getReturnedQuantity();
                            } catch (Exception ex) {
                                log.debug("P2P Cancellation, going to use quantity from sale row");
                            }
                        }
                        break;
                    }
                }

                // Check if a credit note already exists in X3.
                x3CreditNoteNumber = getX3CreditNoteNumber(lazyConn, props.getProperty("CountrySiteCode") + sale.getSaleId(), origLineNumberOnInvoice);

                if (x3CreditNoteNumber != null) {
                    break; // A credit note already exists - no need to loop further ...
                }

                ds.getLastTable().addLine(origLineNumberOnInvoice);
                ds.getLastTable().getLastLine().addField("RTNQTY", origQuantity); //Return Quantity: RTNQTY: All transaction should be 0, except for Credit Notes and Loan Returns 
                ds.getLastTable().getLastLine().addField("RTNTYP", 3); // 3 = Sales Cancellation. Stock is returned to the original sales location with a status "A"
                ds.getLastTable().getLastLine().addField("RTNFLG", 1);
            }

            if (!POSManager.isAirtime(row.getSerialNumber())) {
                foundNonAirtimeLine = true;
            }
        }

        // sale.getStatus().equals(REQUEST_TYPE_QUOTE)
        if (sale.getPaymentMethod().equals(POSManager.PAYMENT_METHOD_QUOTE) && !foundNonAirtimeLine) { // We are trying to cancel an AIRTIME only or BUNDLE Only QUOTE therefore 
            // do not send to X3 ... mark it a successful and exit ...
            result = new CAdxResultXml();
            result.setX3RecordId(x3CreditNoteNumber);
            addMessageIntoResult(result, "Reversal of AIRTIME/BUNDLE only Quotes should not be sent to X3", "INFO");
            result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_FINISHED);
            DAO.setX3RequestFinishedInOwnTransactionScope(emf, requestState, result);
            return result;
        }

        if (x3CreditNoteNumber == null) { // No reversal exist in X3 yet ...
            CAdxParamKeyValue[] objKey = new CAdxParamKeyValue[1];
            CAdxParamKeyValue sepId = new CAdxParamKeyValue();
            sepId.setKey("SEPID");
            sepId.setValue(props.getProperty("CountrySiteCode") + String.valueOf(sale.getSaleId()));
            objKey[0] = sepId;
            result = modify("ZSEP", objKey, ds.getXML());
            result.setX3RecordId(getX3RecordIdNumber(result.getResultXml(), TYPE_CREDIT_NOTE));

            x3PaymentNumber = getX3RecordIdNumber(result.getResultXml(), TYPE_PAYMENT);

            if (result.getX3RecordId() == null) {
                log.warn("No credit note number returned so there must have been an error");
                result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_ERROR);
                addMessageIntoResult(result, "No credit note was created", "X3 error");
            }

        } else { // Credit note for reversal already exists in X3 - Get the accociated payment number ...
            result = new CAdxResultXml();
            result.setX3RecordId(x3CreditNoteNumber);
            result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_FINISHED);
            addMessageIntoResult(result, "Credit note for reversal already exists in X3  - X3 Credit Note Number is [" + x3CreditNoteNumber + "].", "INFO");

            // Get Payment Number ...
            x3PaymentNumber = getX3PaymentNumber(lazyConn, props.getProperty("CountrySiteCode") + sale.getSaleId(), origLineNumberOnInvoice);
        }

        if (sale.getPaymentMethod().equals(POSManager.PAYMENT_METHOD_CASH)
                || sale.getPaymentMethod().equals(POSManager.PAYMENT_METHOD_CARD_PAYMENT)) {
            log.debug("This is a reversal of a sale that had a payment in X3 so we must reverse the payment in X3 as well.");
            try {
                reversePayment(lazyConn, x3PaymentNumber, sale.getSaleId(), result);
                result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_FINISHED);
            } catch (Exception e) {
                log.warn("Exception while trying to reverse payment in X3: " + e.toString());
                result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_ERROR);
                addMessageIntoResult(result, "Error reversing payment: " + e.toString(), "X3 error");
            }
        } else {
            log.debug("This is not a cash sale so no payment reversal is necessary");
        }
        DAO.setX3RequestFinishedInOwnTransactionScope(emf, requestState, result);
        throwExceptionIfError(result);
        log.debug("Finished createCreditNoteForInvoice - for cancellation of an entire invoice");
        return result;
    }

    private static void reversePayment(LazyX3Connection lazyConn, String x3PaymentNumber, int saleId, CAdxResultXml result) throws Exception {
        log.debug("In reversePayment - as part of cancellation of an entire invoice [{}], X3 record id [{}]", String.valueOf(saleId), x3PaymentNumber);

        if (x3PaymentNumber == null) {
            throw new Exception("x3PaymentNumber is null");
        }

        log.debug("Sleeping for 10s before cancelling payment so that X3 can finish committing its previous transaction");
        Utils.sleep(10000);
        log.debug("Finished sleeping for 10s");

        // If we get to this point - it means the reversal of the invoice was successfull - now do the reversal of the payment.
        // 1. Reverse Payment
        // First check if Payment Has been reversed already?
        if (checkIfPaymentExists(lazyConn, x3PaymentNumber)) {
            X3DataStream ds = new X3DataStream();
            ds.addField("LNUM", x3PaymentNumber); // Credit Note Record Id
            ds.addField("LMSG", ""); // SEP Unique Id
            result = run("CANPAY", ds.getXML(), false);

            if (result.getResultXml() == null) {
                throw new Exception("Error cancelling payment in X3. Result XML is null");
            }
            // Check if reversal was successfull
            if (!result.getResultXml().contains("<FLD NAME=\"LMSG\" TYPE=\"Char\"/>")) { // Emply LMSG = SUCCESS According to AccTECH
                throw new Exception("Error while reversing payment in X3. Response XML does not conain expected string. It contains:" + result.getResultXml());
            }
            // This is a hack to handle X3 delays on deleting payment ...
            boolean paymentDeletionSuccessful = false;
            int retryCount = 0;

            do {
                Utils.sleep(1000);
                // 2. Delete the payment:
                CAdxParamKeyValue[] objKey = new CAdxParamKeyValue[1];
                CAdxParamKeyValue paymentNumber = new CAdxParamKeyValue();
                paymentNumber.setKey("NUM");
                paymentNumber.setValue(x3PaymentNumber);
                objKey[0] = paymentNumber;
                result = delete("PAYR", objKey);
                if (result.getResultXml() == null) {
                    // throw new Exception("Error cancelling payment in X3. Result XML is null");
                    log.error("Error cancelling payment [{}] in X3. Result XML is null", x3PaymentNumber);
                    result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_ERROR);
                } else if (!result.getResultXml().contains("PAYLOT")) {
                    // throw new Exception("Error while deleting payment in X3. Response XML does not conain expected string. It contains:" + result.getResultXml());
                    log.error("Error while deleting payment in X3. Response XML does not conain expected string. It contains:" + result.getResultXml());
                    result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_ERROR);
                } else { // Response looks good ... let's exit the loop
                    paymentDeletionSuccessful = true;
                    // 3. Delete the payment:
                    // Update SEP integration table:
                    removeX3PaymentNumber(lazyConn, saleId);
                    result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_FINISHED);
                }

                retryCount++;

                if (!paymentDeletionSuccessful) {
                    log.error("Sleeping for 5 seconds while cancelling payment [{}], retryCount = [{}]", new Object[]{x3PaymentNumber, retryCount});
                    Utils.sleep(5000);
                }
            } while (!paymentDeletionSuccessful && retryCount < 12);
        } else { // Payment already deleted in X3 so just set Result as Successfull;
            if (result == null) {
                result = new CAdxResultXml();
            }
            removeX3PaymentNumber(lazyConn, saleId);
            addMessageIntoResult(result, "Payment " + x3PaymentNumber + " already deleted in X3.", "INFO");
            result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_FINISHED);
        }
        throwExceptionIfError(result);
    }

    public static CAdxResultXml createQuote(EntityManager em, EntityManagerFactory emf, X3TransactionState transactionState, Sale sale, Collection<SaleRow> salesRows, int txType) throws Exception {
        log.debug("In createQuote - this is used to reserve stock in X3");
        CAdxResultXml result;

        X3RequestState requestState = DAO.createX3RequestInOwnTransactionScope(emf, transactionState, REQUEST_TYPE_QUOTE, X3InterfaceDaemon.X3_TRANSACTION_STATUS_IN_PROGRESS);
        if (requestState.getStatus().equals(X3InterfaceDaemon.X3_TRANSACTION_STATUS_FINISHED)) {
            log.warn("This request has been processed already in X3. We wont send it again");
            result = new CAdxResultXml();
            result.setStatus(RESULT_CODE_PROCESSED_BEFORE);
            result.setX3RecordId(requestState.getX3RecordId());
            return result;
        }

        X3DataStream ds = new X3DataStream();
        ds.addGroup("ZSE0_1");
        ds.getLastGroup().addField("SEPID", props.getProperty("CountrySiteCode") + sale.getSaleId()); // SEP Unique Id
        ds.getLastGroup().addField("BPCNUM", getPartnerCode(em, sale));
        ds.getLastGroup().addField("CUR", sale.getTenderedCurrency());
        ds.getLastGroup().addField("SALFCY", props.getProperty("CountrySiteCode"));
        ds.getLastGroup().addField("ORDDAT", sale.getSaleDateTime());
        ds.getLastGroup().addField("CUSORDREF", props.getProperty("CountryPrefix") + sale.getSaleId());
        ds.getLastGroup().addField("SOHTYP", "SOI"); // Sales order type
        ds.getLastGroup().addField("SOHNUM", ""); // Return field. Sales Order Number

        ds.addGroup("ZSE1_2");
        ds.getLastGroup().addField("SEPTYP", txType);
        ds.getLastGroup().addField("SEPSTA", 1); // Unused for now

        ds.addGroup("ZSE1_3");
        ds.getLastGroup().addField("INVDAT", sale.getSaleDateTime());
        ds.getLastGroup().addField("SIHNUM", ""); // Invoice number - returned
        ds.getLastGroup().addField("INVTYP", 1); // Invoice type - returned. 1 = Invoice 2 = credit note
        ds.getLastGroup().addField("PAYDAT", ""); // Payment date. Leave empty so it defaults to today
        ds.getLastGroup().addField("PYHNUM", ""); // Payment number - returned
        ds.getLastGroup().addField("SHIDAT", ""); // Shipment date. Leave empty so it defaults to today
        ds.getLastGroup().addField("SDHNUM", ""); // Loan Shipment number - returned
        ds.getLastGroup().addField("RTNDAT", ""); // Return date. Leave empty so it defaults to today
        ds.getLastGroup().addField("SRHNUM", ""); // Loan Return number - returned

        boolean foundNonAirtimeLine = false;

        ds.addTable("ZSE1_1");
        for (SaleRow row : salesRows) {
            if (POSManager.isAirtime(row) || POSManager.isUnitCredit(row)) {
                continue; // Do not send AIRTIME and BUNDLE items for reservation in X3 - these are non-managed stock items.
            } else {
                ds.getLastTable().addLine();
                boolean hasSubItems = doesItemHaveSubItems(row, salesRows); // kitted items should not have any pricing on them when sending to X3. Only the sub items would have the pricing
                ds.getLastTable().getLastLine().addField("ITMREF", row.getItemNumber()); // Item number
                ds.getLastTable().getLastLine().addField("SAU", "EA"); // Unit of measure
                ds.getLastTable().getLastLine().addField("QTY", row.getQuantity());
                ds.getLastTable().getLastLine().addField("LOT", ""); // Lot number. Send as blank. If LOT is used, will be returned
                ds.getLastTable().getLastLine().addField("SERNUM", row.getSerialNumber());
                ds.getLastTable().getLastLine().addField("LOCTYP", ""); // Send as blank. If used, will be returned
                ds.getLastTable().getLastLine().addField("LOC", ""); // Send as blank. If used, will be returned
                ds.getLastTable().getLastLine().addField("GROPRI", hasSubItems ? 0 : (row.getTotalCentsIncl().add(row.getTotalDiscountOnInclCents()).doubleValue() / (100l * row.getQuantity()))); // Per item prive after tax before discounting
                ds.getLastTable().getLastLine().addField("NETPRI", 0); // Not used
                ds.getLastTable().getLastLine().addField("LINAMT", 0); // Not used
                ds.getLastTable().getLastLine().addField("LINATI", 0); // Not used
                ds.getLastTable().getLastLine().addField("CCE1", sale.getChannel()); // Dmension 1 = channel
                ds.getLastTable().getLastLine().addField("CCE2", sale.getSaleLocation()); // Dimension 2 = province/state
                ds.getLastTable().getLastLine().addField("SOPLIN", ""); // X3 Sales order line number - returned
                ds.getLastTable().getLastLine().addField("SIDLIN", ""); // X3 Invoice line number - returned
                ds.getLastTable().getLastLine().addField("SDDLIN", ""); // X3 Loan shipment line number - returned
                ds.getLastTable().getLastLine().addField("SRDLIN", ""); // X3 Loan return line number - returned
                ds.getLastTable().getLastLine().addField("RTNQTY", 0); //Return Quantity: RTNQTY: All transaction should be 0, except for Credit Notes and Loan Returns 
                populateDiscount(ds, hasSubItems, row, sale);
                ds.getLastTable().getLastLine().addField("FLGVAT", sale.getTaxExempt().equals("Y") ? "1" : "2");
//                ds.getLastTable().getLastLine().addField("OTTAMT", Utils.getDoubleValueFromCRDelimitedAVPString(sale.getExtraInfo(), "DEBIT_AMOUNT1"));
//                ds.getLastTable().getLastLine().addField("SMTAMT", Utils.getDoubleValueFromCRDelimitedAVPString(sale.getExtraInfo(), "SOCIAL_MEDIA_TAX"));
            }
            if (!POSManager.isAirtime(row.getSerialNumber())) {
                foundNonAirtimeLine = true;
            }
        }
        if (foundNonAirtimeLine) {
            result = save("ZSEP", ds.getXML());
            result.setX3RecordId(getX3RecordIdNumber(result.getResultXml(), txType));
            if (result.getX3RecordId() == null) {
                log.warn("No sales order number returned so there must have been an error");
                result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_ERROR);
                addMessageIntoResult(result, "No sales order was created", "X3 error");
            }
        } else {
            log.debug("This quote only contained airtime so no need to send anything to X3");
            result = new CAdxResultXml();
            result.setX3RecordId("NA");
            result.setStatus(RESULT_CODE_SUCCESS);
            result.setResultXml("Not sent to X3 as this quote only contained airtime");
            addMessageIntoResult(result, "Not sent to X3 as this quote only contained airtime", "None");
            result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_FINISHED);
        }
        DAO.setX3RequestFinishedInOwnTransactionScope(emf, requestState, result);
        throwExceptionIfError(result);
        log.debug("Finished createQuote");
        return result;
    }

    public static CAdxResultXml createLoan(EntityManager em, EntityManagerFactory emf, X3TransactionState transactionState, Sale sale, Collection<SaleRow> salesRows, int txType) throws Exception {
        log.debug("In createLoan - this is used to mark stock as being on Loan in X3");
        CAdxResultXml result;

        X3RequestState requestState = DAO.createX3RequestInOwnTransactionScope(emf, transactionState, REQUEST_TYPE_LOAN, X3InterfaceDaemon.X3_TRANSACTION_STATUS_IN_PROGRESS);
        if (requestState.getStatus().equals(X3InterfaceDaemon.X3_TRANSACTION_STATUS_FINISHED)) {
            log.warn("This request has been processed already in X3. We wont send it again");
            result = new CAdxResultXml();
            result.setStatus(RESULT_CODE_PROCESSED_BEFORE);
            result.setX3RecordId(requestState.getX3RecordId());
            return result;
        }

        // This XML has not been tested at all. Its based entirely on the XML for a cash sale - hoping its very similar
        X3DataStream ds = new X3DataStream();
        ds.addGroup("ZSE0_1");
        ds.getLastGroup().addField("SEPID", props.getProperty("CountrySiteCode") + sale.getSaleId()); // SEP Unique Id
        // I'm assuming we pass a different BCP for a loan and whether its staff or trial. I have not configured either in env.x3.props yet
        ds.getLastGroup().addField("BPCNUM", sale.getPaymentMethod().equals(POSManager.PAYMENT_METHOD_STAFF) ? props.getProperty("StaffLoanCustomerCode") : props.getProperty("CustomerLoanCustomerCode"));
        ds.getLastGroup().addField("CUR", sale.getTenderedCurrency());
        ds.getLastGroup().addField("SALFCY", props.getProperty("CountrySiteCode"));
        ds.getLastGroup().addField("ORDDAT", sale.getSaleDateTime());
        ds.getLastGroup().addField("CUSORDREF", props.getProperty("CountryPrefix") + sale.getSaleId());
        ds.getLastGroup().addField("SOHTYP", "SOI"); // Sales order type
        ds.getLastGroup().addField("SOHNUM", ""); // Return field. Sales Order Number

        ds.addGroup("ZSE1_2");
        ds.getLastGroup().addField("SEPTYP", txType);
        ds.getLastGroup().addField("SEPSTA", 1); // Unused for now

        ds.addGroup("ZSE1_3");
        ds.getLastGroup().addField("INVDAT", sale.getSaleDateTime());
        ds.getLastGroup().addField("SIHNUM", ""); // Invoice number - returned
        ds.getLastGroup().addField("INVTYP", 1); // Invoice type - returned. 1 = Invoice 2 = credit note
        ds.getLastGroup().addField("PAYDAT", ""); // Payment date. Leave empty so it defaults to today
        ds.getLastGroup().addField("PYHNUM", ""); // Payment number - returned
        ds.getLastGroup().addField("SHIDAT", ""); // Shipment date. Leave empty so it defaults to today
        ds.getLastGroup().addField("SDHNUM", ""); // Loan Shipment number - returned
        ds.getLastGroup().addField("RTNDAT", ""); // Return date. Leave empty so it defaults to today
        ds.getLastGroup().addField("SRHNUM", ""); // Loan Return number - returned

        ds.addTable("ZSE1_1");
        for (SaleRow row : salesRows) {
            ds.getLastTable().addLine();
            boolean hasSubItems = doesItemHaveSubItems(row, salesRows); // kitted items should not have any pricing on them when sending to X3. Only the sub items would have the pricing
            ds.getLastTable().getLastLine().addField("ITMREF", row.getItemNumber()); // Item number
            ds.getLastTable().getLastLine().addField("SAU", "EA"); // Unit of measure
            ds.getLastTable().getLastLine().addField("QTY", row.getQuantity());
            ds.getLastTable().getLastLine().addField("LOT", ""); // Lot number. Send as blank. If LOT is used, will be returned
            ds.getLastTable().getLastLine().addField("SERNUM", row.getSerialNumber());
            ds.getLastTable().getLastLine().addField("LOCTYP", ""); // Send as blank. If used, will be returned
            ds.getLastTable().getLastLine().addField("LOC", ""); // Send as blank. If used, will be returned
            ds.getLastTable().getLastLine().addField("GROPRI", hasSubItems ? 0 : (row.getTotalCentsIncl().add(row.getTotalDiscountOnInclCents()).doubleValue() / (100l * row.getQuantity()))); // Per item prive after tax before discounting
            ds.getLastTable().getLastLine().addField("NETPRI", 0); // Not used
            ds.getLastTable().getLastLine().addField("LINAMT", 0); // Not used
            ds.getLastTable().getLastLine().addField("LINATI", 0); // Not used
            ds.getLastTable().getLastLine().addField("CCE1", sale.getChannel()); // Dimension 1 = channel
            ds.getLastTable().getLastLine().addField("CCE2", sale.getSaleLocation()); // Dimension 2 = province/state
            ds.getLastTable().getLastLine().addField("SOPLIN", ""); // X3 Sales order line number - returned
            // ds.getLastTable().getLastLine().addField("SOHNUM", ""); // X3 Sales order number - returned
            ds.getLastTable().getLastLine().addField("SIDLIN", ""); // X3 Invoice line number - returned
            ds.getLastTable().getLastLine().addField("SDDLIN", ""); // X3 Loan shipment line number - returned
            ds.getLastTable().getLastLine().addField("SRDLIN", ""); // X3 Loan return line number - returned
            ds.getLastTable().getLastLine().addField("RTNQTY", 0); //Return Quantity: RTNQTY: All transaction should be 0, except for Credit Notes and Loan Returns 
            populateDiscount(ds, hasSubItems, row, sale);
            ds.getLastTable().getLastLine().addField("FLGVAT", sale.getTaxExempt().equals("Y") ? "1" : "2");
//            ds.getLastTable().getLastLine().addField("OTTAMT", Utils.getDoubleValueFromCRDelimitedAVPString(sale.getExtraInfo(), "DEBIT_AMOUNT1"));
//            ds.getLastTable().getLastLine().addField("SMTAMT", Utils.getDoubleValueFromCRDelimitedAVPString(sale.getExtraInfo(), "SOCIAL_MEDIA_TAX"));
        }

        result = save("ZSEP", ds.getXML());
        result.setX3RecordId(getX3RecordIdNumber(result.getResultXml(), txType));
        if (result.getX3RecordId() == null) {
            log.warn("No loan number returned so there must have been an error");
            result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_ERROR);
            addMessageIntoResult(result, "No loan number was created", "X3 error");
        }
        DAO.setX3RequestFinishedInOwnTransactionScope(emf, requestState, result);
        throwExceptionIfError(result);
        log.debug("Finished createLoan");
        return result;
    }

    public static CAdxResultXml createLoanCompletion(EntityManager em, EntityManagerFactory emf, X3TransactionState transactionState, Sale sale, Collection<SaleRow> salesRows, int txType) throws Exception {
        log.debug("In createLoanCompletion");
        CAdxResultXml result;

        X3RequestState requestState = DAO.createX3RequestInOwnTransactionScope(emf, transactionState, REQUEST_TYPE_LOAN_COMPLETION, X3InterfaceDaemon.X3_TRANSACTION_STATUS_IN_PROGRESS);
        if (requestState.getStatus().equals(X3InterfaceDaemon.X3_TRANSACTION_STATUS_FINISHED)) {
            log.warn("This request has been processed already in X3. We wont send it again");
            result = new CAdxResultXml();
            result.setStatus(RESULT_CODE_PROCESSED_BEFORE);
            result.setX3RecordId(requestState.getX3RecordId());
            return result;
        }

        // TODO - verify what fields to pass. This has not been tested
        X3DataStream ds = new X3DataStream();
        ds.addGroup("ZSE0_1");
        ds.getLastGroup().addField("SEPID", props.getProperty("CountrySiteCode") + sale.getSaleId()); // SEP Unique Id
        ds.addGroup("ZSE1_2");
        ds.getLastGroup().addField("SEPTYP", txType);

        ds.addGroup("ZSE1_3");
        ds.getLastGroup().addField("RTNDAT", new Date());

        ds.addTable("ZSE1_1");
        for (SaleRow row : salesRows) {
            int rowIdBeingReturned = row.getSaleRowId();
            int origLineNumberOnInvoice = 0;
            long origQuantity = 0;
            for (SaleRow origRow : salesRows) {
                origLineNumberOnInvoice++;
                if (origRow.getSaleRowId() == rowIdBeingReturned) {
                    origQuantity = origRow.getQuantity();
                    break;
                }
            }
            ds.getLastTable().addLine(origLineNumberOnInvoice);
            ds.getLastTable().getLastLine().addField("RTNQTY", origQuantity); //Return Quantity: RTNQTY: All transaction should be 0, except for Credit Notes and Loan Returns 
            ds.getLastTable().getLastLine().addField("RTNTYP", 3); // 3 = Sales Cancellation. Stock is returned to the original sales location with a status "A"
        }

        CAdxParamKeyValue[] objKey = new CAdxParamKeyValue[1];
        CAdxParamKeyValue sepId = new CAdxParamKeyValue();
        sepId.setKey("SEPID");
        sepId.setValue(props.getProperty("CountrySiteCode") + String.valueOf(sale.getSaleId()));
        objKey[0] = sepId;
        result = modify("ZSEP", objKey, ds.getXML());
        result.setX3RecordId(getX3RecordIdNumber(result.getResultXml(), txType));
        if (result.getX3RecordId() == null) {
            log.warn("No loan completion number returned so there must have been an error");
            result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_ERROR);
            addMessageIntoResult(result, "No loan completion number was created", "X3 error");
        }
        DAO.setX3RequestFinishedInOwnTransactionScope(emf, requestState, result);
        throwExceptionIfError(result);
        log.debug("Finished createLoanCompletion");
        return result;
    }

    // Set the VAT and Discount related fields
    // DISCRGVAL1 = Discount on tax incl amount. Populated when the VAT legislation says that VAT is payable on the NET amount after discount
    // DISCRGVAL2 = Discount on tax incl amount. Populated when the VAT legislation says that VAT is payable on the GROSS amount before discount
    // DISCRGVAL3 = A commission discount for airtime
    // FLGVAT:1 = Customer does not pay VAT. E.g. an embassy and hence Smile does not either
    // FLGVAT:2 = Customer pays VAT.
    // DISCRGVAL5 - Set when a device is sold at a lower price - this contains the difference between normal price and the discounted price.
    // DISCRGVAL6 - Set when Airtime is sold at lower price - this contains the difference between normal price and the discounted price.
    private static void populateDiscount(X3DataStream ds, boolean hasSubItems, SaleRow row, Sale sale) {
        boolean vatOnGross = props.getProperty("VATCalculatedOnGrossPrice").equalsIgnoreCase("true");
        boolean vatOnNett = !vatOnGross;
        double discountOnTaxInclAmnt = (hasSubItems ? 0 : (row.getTotalDiscountOnInclCents().doubleValue() / 100));
        if (POSManager.isAirtime(row)) {
//            boolean airtimeCanBeSoldAtLowerPrice = BaseUtils.getBooleanProperty("env.sales.airtime.at.low.price", false);
//            if (airtimeCanBeSoldAtLowerPrice) {
//                double taxRate = BaseUtils.getDoubleProperty("env.sales.tax.percent");
//                double diffInPrice = row.getQuantity() / (1 + (taxRate / 100)) - row.getTotalDiscountOnExclCents().doubleValue() / 100 - row.getTotalCentsExcl().doubleValue() / 100;
//                ds.getLastTable().getLastLine().addField("DISCRGVAL6", diffInPrice);
//            }
            ds.getLastTable().getLastLine().addField("DISCRGVAL3", discountOnTaxInclAmnt);
            //} else if (vatOnNett && POSManager.isSuperDealer(sale.getRecipientOrganisationId()) && !POSManager.isUnitCredit(row)) {
        } else if (!POSManager.isUnitCredit(row) && (POSManager.isSuperDealer(sale.getRecipientOrganisationId()) || POSManager.isFranchise(sale.getRecipientOrganisationId()))) {
            ds.getLastTable().getLastLine().addField("DISCRGVAL5", discountOnTaxInclAmnt);
        } else if (vatOnGross) { // For East Africa
            // As agreed with Caroline - due to limitations on X3 segments, we can only use DISCRGVAL2 for East Africa.
            ds.getLastTable().getLastLine().addField("DISCRGVAL2", discountOnTaxInclAmnt);

            /* if(!BaseUtils.getBooleanProperty("env.pos.x3.use.new.discount.structure", false)) {
                ds.getLastTable().
            
            
            
            
            
            getLastLine().addField("DISCRGVAL2", discountOnTaxInclAmnt);
            } else {
                /* if(POSManager.isAirtime(row) || POSManager.isUnitCredit(row)) {
                    ds.getLastTable().getLastLine().addField("DISCRGVAL2", discountOnTaxInclAmnt);
                } else {
                    ds.getLastTable().getLastLine().addField("DISCRGVAL1", discountOnTaxInclAmnt);
                } 
            } */
        } else if (vatOnNett) {
            //As requedted by Caroline: HBT-2701 - Please put a condition that will differenciate between direct sales and indirrect sales. If it's a direct sale, use DISCRGVAL3 but if it's an indirect sale, you can continue using DISCRGVAL6
            //ds.getLastTable().getLastLine().addField("DISCRGVAL6", discountOnTaxInclAmnt);

            if (!BaseUtils.getBooleanProperty("env.pos.x3.use.new.discount.structure", false)) {

                if (POSManager.isIndirectSales(sale.getChannel())) { // For Indirect Sales use segment DISCRGVAL6
                    ds.getLastTable().getLastLine().addField("DISCRGVAL6", discountOnTaxInclAmnt);
                } else if (sale.getTaxExempt().equals("Y")) { // For Staff
                    ds.getLastTable().getLastLine().addField("DISCRGVAL2", discountOnTaxInclAmnt);
                } else { // Everything else is for Direct Sales,  use segment DISCRGVAL4
                    ds.getLastTable().getLastLine().addField("DISCRGVAL4", discountOnTaxInclAmnt);
                }

            } else { //Use new logic
                if (POSManager.isIndirectSales(sale.getChannel())) { // For Indirect Sales use segment DISCRGVAL6
                    ds.getLastTable().getLastLine().addField("DISCRGVAL6", discountOnTaxInclAmnt);
                } else if (sale.getTaxExempt().equals("Y") && (POSManager.isAirtime(row) || POSManager.isUnitCredit(row))) { // For Staff
                    ds.getLastTable().getLastLine().addField("DISCRGVAL2", discountOnTaxInclAmnt);
                } else if (POSManager.isAirtime(row) || POSManager.isUnitCredit(row)) { // Everything else is for Direct Sales,  use segment DISCRGVAL4
                    ds.getLastTable().getLastLine().addField("DISCRGVAL4", discountOnTaxInclAmnt);
                } else { // As request by Caroline on JIRA HBT we must use DISCRGVAL1 when the discount is on the device
                    ds.getLastTable().getLastLine().addField("DISCRGVAL1", discountOnTaxInclAmnt);
                }
            }
        }
    }

    static CAdxResultXml createGLEntry(LazyX3Connection lazyConn, EntityManager em, EntityManagerFactory emf, X3TransactionState transactionState) throws Exception {
        log.debug("In send GL entry");
        CAdxResultXml result;
        String strGLTransactionCode;
        String strGLType;

        X3RequestState requestState = DAO.createX3RequestInOwnTransactionScope(emf, transactionState, REQUEST_TYPE_GL_ENTRY, X3InterfaceDaemon.X3_TRANSACTION_STATUS_IN_PROGRESS);
        if (requestState.getStatus().equals(X3InterfaceDaemon.X3_TRANSACTION_STATUS_FINISHED)) {
            log.warn("This request has been processed already in X3. We wont send it again");
            result = new CAdxResultXml();
            result.setStatus(RESULT_CODE_PROCESSED_BEFORE);
            result.setX3RecordId(requestState.getX3RecordId());
            return result;
        }

        strGLTransactionCode = getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "X3_GL_TRANSACTION_CODE");
        strGLType = getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_TYPE");
        boolean sendClearingBureauGL = ((strGLType != null) && strGLType.equalsIgnoreCase("ClearingBureauGL"));
        boolean sendICPCommissionsGL = ((strGLType != null) && strGLType.equalsIgnoreCase("ICPCommissionsGL"));
        boolean icpNettOutGL = ((strGLType != null) && strGLType.equalsIgnoreCase("ICPNettOutGL"));
        boolean unlimitedOTTGL = ((strGLType != null) && strGLType.equalsIgnoreCase("UnlimitedOttGL"));

        X3DataStream ds = new X3DataStream();

        ds.addGroup("HAE0_1");
        ds.getLastGroup().addField("FCY", props.getProperty("CountrySiteCode")); //Site --> Mandatory - check for invoices
        ds.getLastGroup().addField("TYP", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "X3_GL_TRANSACTION_CODE"));
        ds.getLastGroup().addField("NUM", ""); // Not required, can be blank
        ds.getLastGroup().addField("ACCDAT", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_ENTRY_DATE")); //  Accounting Date --> Mandatory

        ds.addGroup("HAE0_2");
        ds.getLastGroup().addField("JOU", ""); //Leave blank
        ds.getLastGroup().addField("CAT", "1"); //Always set to 1 
        ds.getLastGroup().addField("STA", "1"); //Always set to 1 
       // ds.getLastGroup().addField("DACDIA", "STDCO"); //Always STDCO

        ds.addGroup("HAE1_1");
        ds.getLastGroup().addField("DESVCR", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION")); // Document Description --> Alpha numeric 30 characters

        ds.addGroup("HAE1_3");
        ds.getLastGroup().addField("BPRVCR", transactionState.getX3TransactionStatePK().getTableName() + transactionState.getX3TransactionStatePK().getPrimaryKey()); //Source Document --> can leave blank
        ds.getLastGroup().addField("BPRDATVCR", ""); //Source Date --> can leave blank

        ds.addGroup("HAE1_5");
        ds.getLastGroup().addField("RATDAT", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_ENTRY_DATE")); //Exchange Rate Date --> Date of te currency to be used 

        // Set document currency
        String currency = getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_CURRENCY");

        // Check if currency was specified in the GL, otherwise use the system currency property.
        if (currency != null) {
            ds.getLastGroup().addField("CUR", currency);
        } else { // Use system props ...
            ds.getLastGroup().addField("CUR", BaseUtils.getProperty("env.currency.official.symbol"));
        }  //Document Currency --> Mandatory

        ds.addTable("HAE2_1");

        if (sendClearingBureauGL) {
            // Clearing Bureau Type:
            String strCBType = getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "CLEARING_BUREAU_TYPE");

            boolean sendCBTypeA = ((strCBType != null) && strCBType.equalsIgnoreCase("A"));
            boolean sendCBTypeB = ((strCBType != null) && strCBType.equalsIgnoreCase("B"));

            if (sendCBTypeA) {
                BigDecimal transactionFee = getBigDecimalFieldValueFromExtraInfo(transactionState.getExtraInfo(), "TRANSACTION_FEES");
                BigDecimal totalValueIncVat = getBigDecimalFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_AMOUNT");
                // - Take out the VAT portion
                BigDecimal totalValueExcVat = getValueExclVAT(totalValueIncVat);
                // double vatAmount = totalValueIncVat - totalValueExcVat;
                BigDecimal agentDueToSmile = totalValueIncVat.subtract(transactionFee);
                BigDecimal vatAmount = getLastValueForBalancedGL(new BigDecimal[]{totalValueExcVat}, new BigDecimal[]{transactionFee, agentDueToSmile});

                // Fix sign for a reversal
                if (totalValueExcVat.compareTo(BigDecimal.ZERO) < 0) {
                    vatAmount = vatAmount.negate();
                }

                // Airtime CREDIT LINE
                ds.getLastTable().addLine(); // CREDIT LINE
                ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
                ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "AIRTIME_CREDIT_ACCOUNT")); //Account Debit - see finance GL accounts
                ds.getLastTable().getLastLine().addField("BPR", ""); //Business Partner --> Leave Empty 
                ds.getLastTable().getLastLine().addField("CDT", totalValueExcVat); // With VAT
                ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " CR"); //Line Description --> Description of at line level 30 chars
                ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
                ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
                ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
                ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4  
                ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
                ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
                ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7  

                // VAT CREDIT LINE
                ds.getLastTable().addLine(); // CREDIT LINE
                ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
                ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "VAT_CREDIT_ACCOUNT")); //Account Debit - see finance GL accounts
                ds.getLastTable().getLastLine().addField("BPR", ""); //Business Partner --> Leave Empty 
                ds.getLastTable().getLastLine().addField("CDT", vatAmount); // With VAT
                ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " CR"); //Line Description --> Description of at line level 30 chars
                ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
                ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
                ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
                ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4  
                ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
                ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
                ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7  

                // TRANSACTION FEE DEBIT LINE
                ds.getLastTable().addLine(); // CREDIT LINE
                ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
                ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "TRANSACTION_FEE_DEBIT_ACCOUNT")); //Account Debit - see finance GL accounts
                ds.getLastTable().getLastLine().addField("BPR", ""); //Business Partner --> Leave Empty 
                ds.getLastTable().getLastLine().addField("DEB", transactionFee); // With VAT
                ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " TRF"); //Line Description --> Description of at line level 30 chars
                ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
                ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
                ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
                ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4  
                ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
                ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
                ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7  

                // AGENT AMOUNT DUE TO SMILE DEBIT LINE
                ds.getLastTable().addLine(); // CREDIT LINE
                ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
                ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "SMILE_AGENT_DUE_DEBIT_ACCOUNT")); //Account Debit - see finance GL accounts
                ds.getLastTable().getLastLine().addField("BPR", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "CLEARING_BUREAU_BPR_NUM")); //Business Partner --> Leave Empty 
                // ds.getLastTable().getLastLine().addField("DEB", agentDueToSmile); // With VAT
                ds.getLastTable().getLastLine().addField("CDT", transactionFee); // With VAT
                ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " TRF"); //Line Description --> Description of at line level 30 chars
                ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
                ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
                ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
                ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4  
                ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
                ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
                ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7
            } else if (sendCBTypeB) {
                // CB Type B - such as Yo! Uganda
                BigDecimal transactionFee = new BigDecimal(getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "TRANSACTION_FEES")).setScale(X3Field.getX3DecimalPlaces(), BigDecimal.ROUND_HALF_EVEN);
                BigDecimal totalValueIncVat = new BigDecimal(getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_AMOUNT")).setScale(X3Field.getX3DecimalPlaces(), BigDecimal.ROUND_HALF_EVEN);
                // - Take out the VAT portion
                BigDecimal totalValueExcVat = getValueExclVAT(totalValueIncVat);
                BigDecimal vatAmount = totalValueIncVat.subtract(totalValueExcVat);
                BigDecimal cbCommissionExVAT = getValueExclVAT(transactionFee);
                BigDecimal inputVAT = getLastValueForBalancedGL(new BigDecimal[]{totalValueExcVat, vatAmount, transactionFee}, new BigDecimal[]{totalValueIncVat, cbCommissionExVAT});

                // Fix sign for a reversal
                if (totalValueExcVat.compareTo(BigDecimal.ZERO) < 0) {
                    inputVAT = inputVAT.negate();
                }

                String bpr = getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "CLEARING_BUREAU_BPR_NUM");

                // AGENT AMOUNT DUE TO SMILE DEBIT LINE
                ds.getLastTable().addLine(); // CREDIT LINE
                ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
                ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "SMILE_AGENT_DUE_DEBIT_ACCOUNT")); //Account Debit - see finance GL accounts
                ds.getLastTable().getLastLine().addField("BPR", bpr); //Business Partner --> Leave Empty 
                ds.getLastTable().getLastLine().addField("DEB", totalValueIncVat); // With VAT
                ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " TRF"); //Line Description --> Description of at line level 30 chars
                ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
                ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
                ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
                ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4  
                ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
                ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
                ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7

                // Airtime CREDIT LINE
                ds.getLastTable().addLine(); // CREDIT LINE
                ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
                ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "AIRTIME_CREDIT_ACCOUNT")); //Account Debit - see finance GL accounts
                ds.getLastTable().getLastLine().addField("BPR", bpr); //Business Partner --> Leave Empty 
                ds.getLastTable().getLastLine().addField("CDT", totalValueExcVat); // With VAT
                ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " CR"); //Line Description --> Description of at line level 30 chars
                ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
                ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
                ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
                ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4  
                ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
                ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
                ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7  

                // VAT CREDIT LINE
                ds.getLastTable().addLine(); // CREDIT LINE
                ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
                ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "VAT_CREDIT_ACCOUNT")); //Account Debit - see finance GL accounts
                ds.getLastTable().getLastLine().addField("BPR", bpr); //Business Partner --> Leave Empty 
                ds.getLastTable().getLastLine().addField("CDT", vatAmount); // With VAT
                ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " CR"); //Line Description --> Description of at line level 30 chars
                ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
                ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
                ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
                ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4  
                ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
                ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6    
                ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7  

                // Do CB Commission Excl VAT
                ds.getLastTable().addLine();
                ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
                ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "TRANSACTION_FEE_EX_VAT_DEBIT_ACCOUNT")); //Account Debit - see finance GL accounts
                ds.getLastTable().getLastLine().addField("BPR", bpr); //Business Partner --> Leave Empty 
                ds.getLastTable().getLastLine().addField("DEB", cbCommissionExVAT); // Ex VAT
                ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " TRF"); //Line Description --> Description of at line level 30 chars
                ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
                ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
                ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
                ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4  
                ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
                ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
                ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7 

                // Do Input VAT
                ds.getLastTable().addLine();
                ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
                ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "INPUT_VAT_DEBIT_ACCOUNT"));
                ds.getLastTable().getLastLine().addField("BPR", bpr); //Business Partner --> Leave Empty 
                ds.getLastTable().getLastLine().addField("DEB", inputVAT); // Ex VAT
                ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " TRF"); //Line Description --> Description of at line level 30 chars
                ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
                ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
                ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
                ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4  
                ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
                ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
                ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7 

                //DO CB Commision with VAT (CREDIT)
                ds.getLastTable().addLine(); // CREDIT LINE
                ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
                ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "VENDOR_CREDIT_ACCOUNT")); //Account Debit - see finance GL accounts
                ds.getLastTable().getLastLine().addField("BPR", bpr); //Business Partner --> Leave Empty 
                ds.getLastTable().getLastLine().addField("CDT", transactionFee); // With VAT
                ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " TRF"); //Line Description --> Description of at line level 30 chars
                ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
                ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
                ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
                ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4  
                ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
                ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
                ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7

            } else {
                result = new CAdxResultXml();
                result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_ERROR);
                addMessageIntoResult(result, "Invalid Clearing Bureau GL Type:" + strCBType, "POS Error");
                DAO.setX3RequestFinishedInOwnTransactionScope(emf, requestState, result);
                throwExceptionIfError(result);
                return result;
            }

        } else if (sendICPCommissionsGL) {

            // ACQUISITION_COMMISSION
            BigDecimal acquisitionCommissionAmountInclVat = getBigDecimalFieldValueFromExtraInfo(transactionState.getExtraInfo(), "ACQUISITION_COMMISSION_AMOUNT_INC_VAT");
            // - Take out the VAT portion
            BigDecimal acquisitionCommissionAmountExclVat = getValueExclVAT(acquisitionCommissionAmountInclVat);
            BigDecimal acquisitionCommissionVatAmount = getLastValueForBalancedGL(new BigDecimal[]{acquisitionCommissionAmountInclVat}, new BigDecimal[]{acquisitionCommissionAmountExclVat});

            // RETENSION_COMMISSION_AMOUNT
            BigDecimal retensionCommissionAmountInclVat = getBigDecimalFieldValueFromExtraInfo(transactionState.getExtraInfo(), "RETENSION_COMMISSION_AMOUNT_INC_VAT");
            // - Take out the VAT portion
            BigDecimal retensionCommissionAmountExclVat = getValueExclVAT(retensionCommissionAmountInclVat);
            BigDecimal retensionCommissionVatAmount = getLastValueForBalancedGL(new BigDecimal[]{retensionCommissionAmountInclVat}, new BigDecimal[]{retensionCommissionAmountExclVat});

            ds.getLastTable().addLine(); // DEBIT LINE - ACQUISITION_COMMISSION_EXCL_VAT
            ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
            ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "ACQUISITION_COMMISSION_EXCL_VAT_DEBIT_ACCOUNT")); //Account Debit - see finance GL accounts
            ds.getLastTable().getLastLine().addField("BPR", getICPBPRNum(Long.valueOf(getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "ICP_ACCOUNT_ID")))); //Business Partner  
            ds.getLastTable().getLastLine().addField("DEB", acquisitionCommissionAmountExclVat); // With no VAT
            ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " CR"); //Line Description --> Description of at line level 30 chars
            ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
            ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
            ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
            ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4  
            ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
            ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
            ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7  

            // DEBIT LINE - VAT INPUT
            ds.getLastTable().addLine(); // CREDIT LINE - ACQUISITION_COMMISSION
            ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
            ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "INPUT_VAT_DEBIT_ACCOUNT")); //Account Debit - see finance GL accounts
            ds.getLastTable().getLastLine().addField("BPR", getICPBPRNum(Long.valueOf(getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "ICP_ACCOUNT_ID")))); //Business Partner --> Leave Empty 
            ds.getLastTable().getLastLine().addField("DEB", acquisitionCommissionVatAmount); // 
            ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " CR"); //Line Description --> Description of at line level 30 chars
            ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
            ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
            ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
            ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4  
            ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
            ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
            ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7

            // CREDIT ACQ COMMISSION
            ds.getLastTable().addLine();
            ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
            ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "ACQUISITION_COMMISSION_INCL_VAT_CREDIT_ACCOUNT"));
            ds.getLastTable().getLastLine().addField("BPR", getICPBPRNum(Long.valueOf(getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "ICP_ACCOUNT_ID")))); //Business Partner --> Leave Empty 
            ds.getLastTable().getLastLine().addField("CDT", acquisitionCommissionAmountInclVat); // Ex VAT
            ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " TRF"); //Line Description --> Description of at line level 30 chars
            ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
            ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
            ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
            ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4  
            ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
            ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
            ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7 

            ds.getLastTable().addLine(); // DEBIT LINE - ACQUISITION_COMMISSION_EXCL_VAT
            ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
            ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "RETENSION_COMMISSION_EXCL_VAT_DEBIT_ACCOUNT")); //Account Debit - see finance GL accounts
            ds.getLastTable().getLastLine().addField("BPR", getICPBPRNum(Long.valueOf(getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "ICP_ACCOUNT_ID")))); //Business Partner  
            ds.getLastTable().getLastLine().addField("DEB", retensionCommissionAmountExclVat); // With no VAT
            ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " CR"); //Line Description --> Description of at line level 30 chars
            ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
            ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
            ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
            ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4  
            ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
            ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
            ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7  

            // DEBIT LINE - VAT INPUT
            ds.getLastTable().addLine(); // CREDIT LINE - ACQUISITION_COMMISSION
            ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
            ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "INPUT_VAT_DEBIT_ACCOUNT")); //Account Debit - see finance GL accounts
            ds.getLastTable().getLastLine().addField("BPR", getICPBPRNum(Long.valueOf(getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "ICP_ACCOUNT_ID")))); //Business Partner --> Leave Empty 
            ds.getLastTable().getLastLine().addField("DEB", retensionCommissionVatAmount); // 
            ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " CR"); //Line Description --> Description of at line level 30 chars
            ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
            ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
            ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
            ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4  
            ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
            ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
            ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7

            // CREDIT ACQ COMMISSION
            ds.getLastTable().addLine();
            ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
            ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "RETENSION_COMMISSION_INCL_VAT_CREDIT_ACCOUNT"));
            ds.getLastTable().getLastLine().addField("BPR", getICPBPRNum(Long.valueOf(getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "ICP_ACCOUNT_ID")))); //Business Partner --> Leave Empty 
            ds.getLastTable().getLastLine().addField("CDT", retensionCommissionAmountInclVat); // Ex VAT
            ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " TRF"); //Line Description --> Description of at line level 30 chars
            ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
            ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
            ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
            ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4  
            ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
            ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
            ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7 

        } else if (unlimitedOTTGL) {
            //  NB : Amounts are  all coming in cents, must divide by 100 before sending to X3
            BigDecimal debitAmount1 = getBigDecimalFieldValueFromExtraInfo(transactionState.getExtraInfo(), "DEBIT_AMOUNT1").divide(new BigDecimal(100), RoundingMode.HALF_EVEN);
            BigDecimal debitAmount2 = getBigDecimalFieldValueFromExtraInfo(transactionState.getExtraInfo(), "DEBIT_AMOUNT2").divide(new BigDecimal(100), RoundingMode.HALF_EVEN);

            BigDecimal creditAmount1 = getBigDecimalFieldValueFromExtraInfo(transactionState.getExtraInfo(), "CREDIT_AMOUNT1").divide(new BigDecimal(100), RoundingMode.HALF_EVEN);
            BigDecimal creditAmount2 = getBigDecimalFieldValueFromExtraInfo(transactionState.getExtraInfo(), "CREDIT_AMOUNT2").divide(new BigDecimal(100), RoundingMode.HALF_EVEN);

            // Credit
            if (creditAmount1.compareTo(BigDecimal.ZERO) > 0) {

                creditAmount1 = creditAmount1.setScale(2, RoundingMode.HALF_EVEN);

                ds.getLastTable().addLine(); // 1. CR 4410002 with creditUnearnedCentsExcl
                ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
                ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "CREDIT_ACCOUNT1")); //Account Debit - see finance GL accounts
                ds.getLastTable().getLastLine().addField("BPR", ""); //Business Partner --> Leave Empty 
                ds.getLastTable().getLastLine().addField("CDT", creditAmount1.toPlainString()); //Credit Amount --> Only populate if its a debit line
                ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " CR"); //Line Description --> Description of at line level 30 chars
                ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
                ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
                ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3
                ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4
                ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
                ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
                ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7 
            }

            if (creditAmount2.compareTo(BigDecimal.ZERO) > 0) {

                creditAmount2 = creditAmount2.setScale(2, RoundingMode.HALF_EVEN);

                ds.getLastTable().addLine(); // 1. CR 4410002 with creditUnearnedCentsExcl
                ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
                ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "CREDIT_ACCOUNT2")); //Account Debit - see finance GL accounts
                ds.getLastTable().getLastLine().addField("BPR", ""); //Business Partner --> Leave Empty 
                ds.getLastTable().getLastLine().addField("CDT", creditAmount2.toPlainString()); //Credit Amount --> Only populate if its a debit line
                ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " CR"); //Line Description --> Description of at line level 30 chars
                ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
                ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
                ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
                ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4
                ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
                ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
                ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7 
            }

            // Debit
            debitAmount1 = debitAmount1.setScale(2, RoundingMode.HALF_EVEN);

            if (debitAmount1.compareTo(BigDecimal.ZERO) > 0) {
                ds.getLastTable().addLine(); // Line 1. DB icpDiscAccount with icpDiscountCents 
                ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
                ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "DEBIT_ACCOUNT1")); //Account Debit - see finance GL accounts
                ds.getLastTable().getLastLine().addField("BPR", ""); //Business Partner --> Leave Empty 
                ds.getLastTable().getLastLine().addField("DEB", debitAmount1.toPlainString()); // Exclude VAT on debit line since an input VAT debit line will be included below. - to keep the journal balanced.
                ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " DB"); //Line Description --> Description of at line level 30 chars
                ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
                ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
                ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3 
                ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4 
                ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
                ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
                ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7 
            }

            debitAmount2 = debitAmount2.setScale(2, RoundingMode.HALF_EVEN);

            if (debitAmount2.compareTo(BigDecimal.ZERO) > 0) {
                ds.getLastTable().addLine(); // Line 1. DB icpDiscAccount with icpDiscountCents 
                ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
                ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "DEBIT_ACCOUNT2")); //Account Debit - see finance GL accounts
                ds.getLastTable().getLastLine().addField("BPR", ""); //Business Partner --> Leave Empty 
                ds.getLastTable().getLastLine().addField("DEB", debitAmount2.toPlainString()); // Exclude VAT on debit line since an input VAT debit line will be included below. - to keep the journal balanced.
                ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " DB"); //Line Description --> Description of at line level 30 chars
                ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
                ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
                ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
                ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4
                ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
                ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
                ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7   
            }

        } else if (icpNettOutGL) { // For ICP Nett Off Journals

            //  NB : Amounts are  all coming in cents, must divide by 100 before sending to X3
            BigDecimal debitAmount1 = getBigDecimalFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DEBIT_AMOUNT1").divide(new BigDecimal(100), RoundingMode.HALF_EVEN);
            BigDecimal debitAmount2 = getBigDecimalFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DEBIT_AMOUNT2").divide(new BigDecimal(100), RoundingMode.HALF_EVEN);
            BigDecimal debitAmount3 = getBigDecimalFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DEBIT_AMOUNT3").divide(new BigDecimal(100), RoundingMode.HALF_EVEN);
            BigDecimal debitAmount4 = getBigDecimalFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DEBIT_AMOUNT4").divide(new BigDecimal(100), RoundingMode.HALF_EVEN);

            BigDecimal creditAmount1 = getBigDecimalFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_CREDIT_AMOUNT1").divide(new BigDecimal(100), RoundingMode.HALF_EVEN);
            BigDecimal creditAmount2 = getBigDecimalFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_CREDIT_AMOUNT2").divide(new BigDecimal(100), RoundingMode.HALF_EVEN);
            BigDecimal creditAmount3 = getBigDecimalFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_CREDIT_AMOUNT3").divide(new BigDecimal(100), RoundingMode.HALF_EVEN);
            BigDecimal creditAmount4 = getBigDecimalFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_CREDIT_AMOUNT4").divide(new BigDecimal(100), RoundingMode.HALF_EVEN);

            // To balance the GL
            creditAmount1 = creditAmount1.setScale(2, RoundingMode.HALF_EVEN);
            creditAmount2 = creditAmount2.setScale(2, RoundingMode.HALF_EVEN);
            creditAmount3 = creditAmount3.setScale(2, RoundingMode.HALF_EVEN);
            creditAmount4 = creditAmount4.setScale(2, RoundingMode.HALF_EVEN);

            debitAmount1 = debitAmount1.setScale(2, RoundingMode.HALF_EVEN);
            debitAmount2 = debitAmount2.setScale(2, RoundingMode.HALF_EVEN);
            debitAmount3 = debitAmount3.setScale(2, RoundingMode.HALF_EVEN);
            debitAmount4 = debitAmount4.setScale(2, RoundingMode.HALF_EVEN);

            BigDecimal glDiff = getDiffForBalancedGL(new BigDecimal[]{creditAmount1, creditAmount2, creditAmount3, creditAmount4},
                    new BigDecimal[]{debitAmount1, debitAmount2, debitAmount3, debitAmount4});
            glDiff.setScale(2, RoundingMode.HALF_EVEN);

            if (glDiff.compareTo(BigDecimal.ZERO) < 0) { // Debits are more than credits 
                creditAmount1 = creditAmount1.add(glDiff.abs()).setScale(2, RoundingMode.HALF_EVEN);
            } else { //Credits are more or equal to Debits.
                debitAmount1 = debitAmount1.add(glDiff).setScale(2, RoundingMode.HALF_EVEN);
            }

            // debitAmount1 = debitAmount1.setScale(2, RoundingMode.HALF_EVEN);
            log.debug("The GLDiff for GL entty primary_key [{}] is [{}]", transactionState.getX3TransactionStatePK().getPrimaryKey(), glDiff.toPlainString());
            if (creditAmount1.compareTo(BigDecimal.ZERO) > 0) {

                ds.getLastTable().addLine(); // 1. CR 4410002 with creditUnearnedCentsExcl
                ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
                ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "CREDIT_ACCOUNT1")); //Account Debit - see finance GL accounts
                ds.getLastTable().getLastLine().addField("BPR", ""); //Business Partner --> Leave Empty 
                ds.getLastTable().getLastLine().addField("CDT", creditAmount1.toPlainString()); //Credit Amount --> Only populate if its a debit line
                ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " CR"); //Line Description --> Description of at line level 30 chars
                ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
                ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
                ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
                ds.getLastTable().getLastLine().addField("CCE4", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION4")); //Dimension 4
                ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
                ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
                ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7 
            }

            if (creditAmount2.compareTo(BigDecimal.ZERO) > 0) {

                ds.getLastTable().addLine(); // Line 2: CR deviceAccount with deviceRevenueCentsExcl
                ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
                ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "CREDIT_ACCOUNT2")); //Account Debit - see finance GL accounts
                ds.getLastTable().getLastLine().addField("BPR", ""); //Business Partner --> Leave Empty 
                ds.getLastTable().getLastLine().addField("CDT", creditAmount2.toPlainString()); //Credit Amount --> Only populate if its a debit line
                ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " CR"); //Line Description --> Description of at line level 30 chars
                ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
                ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
                ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
                ds.getLastTable().getLastLine().addField("CCE4", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION4")); //Dimension 4
                ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
                ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
                ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7 
            }

            if (creditAmount3.compareTo(BigDecimal.ZERO) > 0) {

                ds.getLastTable().addLine(); // Line 2: CR deviceAccount with deviceRevenueCentsExcl
                ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
                ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "CREDIT_ACCOUNT3")); //Account Debit - see finance GL accounts
                ds.getLastTable().getLastLine().addField("BPR", ""); //Business Partner --> Leave Empty 
                ds.getLastTable().getLastLine().addField("CDT", creditAmount3.toPlainString()); //Credit Amount --> Only populate if its a debit line
                ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " CR"); //Line Description --> Description of at line level 30 chars
                ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
                ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
                ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
                ds.getLastTable().getLastLine().addField("CCE4", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION4")); //Dimension 4
                ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
                ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
                ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7 
            }

            if (creditAmount4.compareTo(BigDecimal.ZERO) > 0) {

                ds.getLastTable().addLine(); // Line 2: CR deviceAccount with deviceRevenueCentsExcl
                ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
                ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "CREDIT_ACCOUNT4")); //Account Debit - see finance GL accounts
                ds.getLastTable().getLastLine().addField("BPR", ""); //Business Partner --> Leave Empty 
                ds.getLastTable().getLastLine().addField("CDT", creditAmount4.toPlainString()); //Credit Amount --> Only populate if its a debit line
                ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " CR"); //Line Description --> Description of at line level 30 chars
                ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
                ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
                ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
                ds.getLastTable().getLastLine().addField("CCE4", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION4")); //Dimension 4
                ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
                ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
                ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7 
            }

            if (debitAmount1.compareTo(BigDecimal.ZERO) > 0) {

                // debitAmount1 = debitAmount1.setScale(2, RoundingMode.HALF_EVEN);
                // debitAmount1 = debitAmount1.add(glDiff).abs().setScale(2, RoundingMode.HALF_EVEN);
                ds.getLastTable().addLine(); // Line 1. DB icpDiscAccount with icpDiscountCents 
                ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
                ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "DEBIT_ACCOUNT1")); //Account Debit - see finance GL accounts
                ds.getLastTable().getLastLine().addField("BPR", ""); //Business Partner --> Leave Empty 
                ds.getLastTable().getLastLine().addField("DEB", debitAmount1.toPlainString()); // Adjust the GL with balances so we balance.
                ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " DB"); //Line Description --> Description of at line level 30 chars
                ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
                ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
                ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
                ds.getLastTable().getLastLine().addField("CCE4", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION4")); //Dimension 4
                ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
                ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
                ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7  
            }

            /*
            if ((debitAmount1.compareTo(BigDecimal.ZERO) > 0) && (glDiff.compareTo(BigDecimal.ZERO) < 0)) { // Make it a CREDIT
                
                debitAmount1 = debitAmount1.add(glDiff).abs().setScale(2, RoundingMode.HALF_EVEN);
                
                ds.getLastTable().addLine(); // Line 1. DB icpDiscAccount with icpDiscountCents 
                ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
                ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "DEBIT_ACCOUNT1")); //Account Debit - see finance GL accounts
                ds.getLastTable().getLastLine().addField("BPR", ""); //Business Partner --> Leave Empty 
                ds.getLastTable().getLastLine().addField("CDT", debitAmount1.toPlainString()); // Adjust the GL with balances so we balance.
                ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " DB"); //Line Description --> Description of at line level 30 chars
                ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
                ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
                ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
                ds.getLastTable().getLastLine().addField("CCE4", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION4")); //Dimension 4
                ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
                ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
                ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7  
            }
            
            if ((debitAmount1.compareTo(BigDecimal.ZERO) > 0) || (glDiff.compareTo(BigDecimal.ZERO) > 0)) {
                
                // debitAmount1 = debitAmount1.setScale(2, RoundingMode.HALF_EVEN);
                debitAmount1 = debitAmount1.add(glDiff).abs().setScale(2, RoundingMode.HALF_EVEN);
                
                ds.getLastTable().addLine(); // Line 1. DB icpDiscAccount with icpDiscountCents 
                ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
                ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "DEBIT_ACCOUNT1")); //Account Debit - see finance GL accounts
                ds.getLastTable().getLastLine().addField("BPR", ""); //Business Partner --> Leave Empty 
                ds.getLastTable().getLastLine().addField("DEB", debitAmount1.toPlainString()); // Adjust the GL with balances so we balance.
                ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " DB"); //Line Description --> Description of at line level 30 chars
                ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
                ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
                ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
                ds.getLastTable().getLastLine().addField("CCE4", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION4")); //Dimension 4
                ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
                ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
                ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7  
            } */
            if (debitAmount2.compareTo(BigDecimal.ZERO) > 0) {

                ds.getLastTable().addLine(); // Line 1. DB icpDiscAccount with icpDiscountCents 
                ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
                ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "DEBIT_ACCOUNT2")); //Account Debit - see finance GL accounts
                ds.getLastTable().getLastLine().addField("BPR", ""); //Business Partner --> Leave Empty 
                ds.getLastTable().getLastLine().addField("DEB", debitAmount2.toPlainString()); // Exclude VAT on debit line since an input VAT debit line will be included below. - to keep the journal balanced.
                ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " DB"); //Line Description --> Description of at line level 30 chars
                ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
                ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
                ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
                ds.getLastTable().getLastLine().addField("CCE4", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION4")); //Dimension 4
                ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
                ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
                ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7  
            }

            if (debitAmount3.compareTo(BigDecimal.ZERO) > 0) {

                ds.getLastTable().addLine(); // Line 1. DB icpDiscAccount with icpDiscountCents 
                ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
                ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "DEBIT_ACCOUNT3")); //Account Debit - see finance GL accounts
                ds.getLastTable().getLastLine().addField("BPR", ""); //Business Partner --> Leave Empty 
                ds.getLastTable().getLastLine().addField("DEB", debitAmount3.toPlainString()); // Exclude VAT on debit line since an input VAT debit line will be included below. - to keep the journal balanced.
                ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " DB"); //Line Description --> Description of at line level 30 chars
                ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
                ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
                ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
                ds.getLastTable().getLastLine().addField("CCE4", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION4")); //Dimension 4 
                ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
                ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
                ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7  
            }

            if (debitAmount4.compareTo(BigDecimal.ZERO) > 0) {

                ds.getLastTable().addLine(); // Line 1. DB icpDiscAccount with icpDiscountCents 
                ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
                ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "DEBIT_ACCOUNT4")); //Account Debit - see finance GL accounts
                ds.getLastTable().getLastLine().addField("BPR", ""); //Business Partner --> Leave Empty 
                ds.getLastTable().getLastLine().addField("DEB", debitAmount4.toPlainString()); // Exclude VAT on debit line since an input VAT debit line will be included below. - to keep the journal balanced.
                ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " DB"); //Line Description --> Description of at line level 30 chars
                ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
                ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
                ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
                ds.getLastTable().getLastLine().addField("CCE4", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION4")); //Dimension 4 
                ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
                ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
                ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7  
            }

        } else { // For Normal GLs and Voice InterConnect GLs

            String strSendVatSegment = getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "SEND_VAT_SEGMENT");
            boolean sendVatSegment = ((strSendVatSegment != null) && strSendVatSegment.equalsIgnoreCase("YES"));
            boolean sendInputVat = false;

            if (sendVatSegment) {
                // Check if is for input VAT?
                String strInputVatSegment = getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "TREAT_AS_INPUT_VAT");
                sendInputVat = ((strInputVatSegment != null) && strInputVatSegment.equalsIgnoreCase("YES"));
            }

            BigDecimal glAmount = getBigDecimalFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_AMOUNT");
            BigDecimal glAmountExVat = getValueExclVAT(glAmount);
            BigDecimal glVatAmount = getLastValueForBalancedGL(new BigDecimal[]{glAmount}, new BigDecimal[]{glAmountExVat});

            String bpr = getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_BP_NUM");

            ds.getLastTable().addLine(); // DEBIT LINE
            ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
            ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "DEBIT_ACCOUNT")); //Account Debit - see finance GL accounts
            ds.getLastTable().getLastLine().addField("BPR", bpr); //Business Partner --> Leave Empty 

            if (sendInputVat) {
                ds.getLastTable().getLastLine().addField("DEB", glAmountExVat); // Exclude VAT on debit line since an input VAT debit line will be included below. - to keep the journal balanced.
            } else {
                ds.getLastTable().getLastLine().addField("DEB", glAmount); //Debit Amount --> Only populate if its a debit line
            }

            ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " DB"); //Line Description --> Description of at line level 30 chars
            ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
            ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
            ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
            ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4  
            ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
            ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
            ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7  

            ds.getLastTable().addLine(); // CREDIT LINE
            ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
            ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "CREDIT_ACCOUNT")); //Account Debit - see finance GL accounts
            ds.getLastTable().getLastLine().addField("BPR", bpr); //Business Partner --> Leave Empty 
            // Check if VAT must be excluded from the CREDIT LINE
            if (sendVatSegment && !sendInputVat) { // Send the output VAT segment to X3 so, exclude VAT from the cREDIT amount
                ds.getLastTable().getLastLine().addField("CDT", glAmountExVat); //Credit Amount --> Only populate if its a debit line
            } else { // - Include VAT
                ds.getLastTable().getLastLine().addField("CDT", glAmount); // With VAT
            }

            ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + " CR"); //Line Description --> Description of at line level 30 chars
            ds.getLastTable().getLastLine().addField("CCE1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DIMENSION1")); //Dimension 1 
            ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
            ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
            ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4  
            ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
            ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
            ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7  

            if (sendVatSegment) {

                ds.getLastTable().addLine(); // Add the VAT LINE
                ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
                ds.getLastTable().getLastLine().addField("ACC1", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "VAT_ACCOUNT")); //Account Debit - see finance GL accounts
                ds.getLastTable().getLastLine().addField("BPR", bpr); //Business Partner --> Leave Empty 

                if (sendInputVat) { // Treat as Input VAT ... do a bebit and not a credit ...
                    ds.getLastTable().getLastLine().addField("DEB", glVatAmount); //VAT Amount --> Only populate if sendVATSegment is true
                } else {  // Normal VAT - Output VAT do a bebit
                    ds.getLastTable().getLastLine().addField("CDT", glVatAmount); //VAT Amount --> Only populate if sendVATSegment is true
                }

                ds.getLastTable().getLastLine().addField("DES", getFieldValueFromExtraInfo(transactionState.getExtraInfo(), "GL_DESCRIPTION") + "VAT"); //Line Description --> Description of at line level 30 chars
                ds.getLastTable().getLastLine().addField("CCE1", ""); //Dimension 1 
                ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
                ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
                ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4  
                ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
                ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
                ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7  

            }
        }

        // Mukosi - first check if GL exists in X3 - to cater for network timeout problems
        // where SEP timesout after sending a transaciton to X3, X3 processes the transaction successfully but
        // SEP retries after a timeout.
        String glDocNumber = checkGLExistInX3(lazyConn, strGLTransactionCode, transactionState.getX3TransactionStatePK().getTableName() + transactionState.getX3TransactionStatePK().getPrimaryKey(), props.getProperty("CountrySiteCode"));
        if (glDocNumber == null) { // New GL

            result = save("WSGAS", ds.getXML()); // Create the GL Entry
            result.setX3RecordId(getX3RecordIdNumber(result.getResultXml(), X3Helper.TYPE_GL));
            if (result.getX3RecordId() == null) {
                log.warn("No GL number returned so there must have been an error");
                result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_ERROR);
                addMessageIntoResult(result, "No GL number was created", "X3 error");
            }
        } else { //Else GL Already processed in X3
            result = new CAdxResultXml();
            CAdxMessage msg = new CAdxMessage();
            msg.setType("INFO");
            msg.setMessage("GL already exists in X3  - X3 Document Number is [" + glDocNumber + "].");
            result.setMessages(new CAdxMessage[]{msg});
            result.setX3RecordId(glDocNumber);
            result.setStatusCode("FI");
            result.setStatus(1010);
            result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_FINISHED);
        }

        DAO.setX3RequestFinishedInOwnTransactionScope(emf, requestState, result);
        throwExceptionIfError(result);
        log.debug("Finished sending GL Entry");
        return result;
    }

    private static String getICPBPRNum(long accountId) throws Exception {
        ServiceInstanceQuery serviceInstanceQuery = new ServiceInstanceQuery();
        serviceInstanceQuery.setAccountId(accountId);
        serviceInstanceQuery.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
        ServiceInstanceList serviceInstances = SCAWrapper.getAdminInstance().getServiceInstances(serviceInstanceQuery);

        if (serviceInstances.getServiceInstances().size() <= 0) {
            throw new Exception("No such customer -- [AccountId: " + accountId + "]");
        }

        ServiceInstance si = serviceInstances.getServiceInstances().get(0);
        ProductInstanceQuery productInstanceQuery = new ProductInstanceQuery();
        productInstanceQuery.setProductInstanceId(si.getProductInstanceId());
        productInstanceQuery.setVerbosity(StProductInstanceLookupVerbosity.MAIN);

        ProductInstanceList productInstances = SCAWrapper.getAdminInstance().getProductInstances(productInstanceQuery);
        if (productInstances.getProductInstances().size() <= 0) {
            throw new Exception("No product associated with account -- [AccountId: " + accountId + "]");
        }

        // Finally, get orgnisation
        OrganisationQuery orgQuery = new OrganisationQuery();
        orgQuery.setOrganisationId(productInstances.getProductInstances().get(0).getOrganisationId());
        orgQuery.setVerbosity(StOrganisationLookupVerbosity.MAIN);

        OrganisationList orgList = SCAWrapper.getAdminInstance().getOrganisations(orgQuery);

        if (orgList.getOrganisations().size() <= 0) {
            throw new Exception("No organisation associated with account -- [AccountId: " + accountId + "]");
        }
        Organisation org = orgList.getOrganisations().get(0);

        String bprNum = org.getCreditAccountNumber();

        if (bprNum == null || (bprNum.trim().length() <= 0)) {
            throw new Exception("Credit Account Number for ICP Organisation [" + org.getOrganisationName() + "] not set properly.");
        }

        return orgList.getOrganisations().get(0).getCreditAccountNumber();
    }

    static CAdxResultXml createGLEntryForPSCTransaction(EntityManager em, LazyX3Connection lazyConn, EntityManagerFactory emf, X3TransactionState transactionState, AccountHistory dbPSCTransaction) throws Exception {
        log.debug("In send GL entry for PSC Transaction");
        CAdxResultXml result;

        X3RequestState requestState = DAO.createX3RequestInOwnTransactionScope(emf, transactionState, REQUEST_TYPE_GL_ENTRY, X3InterfaceDaemon.X3_TRANSACTION_STATUS_IN_PROGRESS);
        if (requestState.getStatus().equals(X3InterfaceDaemon.X3_TRANSACTION_STATUS_FINISHED)) {
            log.warn("This request has been processed already in X3. We wont send it again");
            result = new CAdxResultXml();
            result.setStatus(RESULT_CODE_PROCESSED_BEFORE);
            result.setX3RecordId(requestState.getX3RecordId());
            return result;
        }

        double glAmount = dbPSCTransaction.getAccountCents().doubleValue() / 100.0;
        X3DataStream ds = new X3DataStream();
        String strGLTransactionCode = props.getProperty("PSCTGLTransactionCode");
        ds.addGroup("HAE0_1");
        ds.getLastGroup().addField("FCY", props.getProperty("CountrySiteCode")); //Site --> Mandatory - check for invoices
        ds.getLastGroup().addField("TYP", props.getProperty("PSCTGLTransactionCode")); // GL Transaction Code
        ds.getLastGroup().addField("NUM", ""); // Not required, can be blank
        ds.getLastGroup().addField("ACCDAT", dbPSCTransaction.getEndDate()); //  Accounting Date --> Mandatory

        ds.addGroup("HAE0_2");
        ds.getLastGroup().addField("JOU", ""); //Leave blank
        ds.getLastGroup().addField("CAT", "1"); //Always set to 1 
        ds.getLastGroup().addField("STA", "1"); //Always set to 1 
      //  ds.getLastGroup().addField("DACDIA", "STDCO"); //Always STDCO

        ds.addGroup("HAE1_1");
        ds.getLastGroup().addField("DESVCR", "PSC Transaction GL"); // Document Description --> Alpha numeric 30 characters

        ds.addGroup("HAE1_3");
        ds.getLastGroup().addField("BPRVCR", transactionState.getX3TransactionStatePK().getTableName() + transactionState.getX3TransactionStatePK().getPrimaryKey()); //Source Document --> can leave blank
        ds.getLastGroup().addField("BPRDATVCR", ""); //Source Date --> can leave blank

        ds.addGroup("HAE1_5");
        ds.getLastGroup().addField("RATDAT", dbPSCTransaction.getEndDate()); //Exchange Rate Date --> Date of te currency to be used 
        ds.getLastGroup().addField("CUR", BaseUtils.getProperty("env.currency.official.symbol"));  //Document Currency --> Mandatory

        ds.addTable("HAE2_1");

        ds.getLastTable().addLine(); // DEBIT LINE
        ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
        ds.getLastTable().getLastLine().addField("ACC1", props.getProperty("PSCTransactionAPAccount")); //Account Debit - see finance GL accounts
        ds.getLastTable().getLastLine().addField("BPR", getICPBPRNum(dbPSCTransaction.getAccountId())); //Business Partner -->  
        ds.getLastTable().getLastLine().addField("DEB", glAmount); //Debit Amount --> Only populate if its a debit line
        ds.getLastTable().getLastLine().addField("DES", "PSC Transaction DB"); //Line Description --> Description of at line level 30 chars
        ds.getLastTable().getLastLine().addField("CCE1", props.getProperty("AdhocChannel")); //Dimension 1 
        ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
        ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
        ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4  
        ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
        ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
        ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7  

        ds.getLastTable().addLine(); // CREDIT LINE
        ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
        ds.getLastTable().getLastLine().addField("ACC1", props.getProperty("WalletAccount")); //Account Debit - see finance GL accounts
        ds.getLastTable().getLastLine().addField("BPR", ""); //Business Partner --> Leave Empty 
        ds.getLastTable().getLastLine().addField("CDT", glAmount); // With VAT
        ds.getLastTable().getLastLine().addField("DES", "PSC Transaction CR"); //Line Description --> Description of at line level 30 chars
        ds.getLastTable().getLastLine().addField("CCE1", props.getProperty("AdhocChannel")); //Dimension 1 
        ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
        ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
        ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4  
        ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
        ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
        ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7  

        String glDocNumber = checkGLExistInX3(lazyConn, strGLTransactionCode, transactionState.getX3TransactionStatePK().getTableName() + transactionState.getX3TransactionStatePK().getPrimaryKey(), props.getProperty("CountrySiteCode"));
        if (glDocNumber == null) { // New GL

            result = save("WSGAS", ds.getXML()); // Create the GL Entry
            result.setX3RecordId(getX3RecordIdNumber(result.getResultXml(), X3Helper.TYPE_GL));
            if (result.getX3RecordId() == null) {
                log.warn("No GL number returned so there must have been an error");
                result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_ERROR);
                addMessageIntoResult(result, "No GL number was created", "X3 error");
            }

        } else { // Else GL Already processed in X3
            result = new CAdxResultXml();
            CAdxMessage msg = new CAdxMessage();
            msg.setType("INFO");
            msg.setMessage("GL already exists in X3  - X3 Document Number is [" + glDocNumber + "].");
            result.setMessages(new CAdxMessage[]{msg});
            result.setX3RecordId(glDocNumber);
            result.setStatusCode("FI");
            result.setStatus(1010);
            result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_FINISHED);
        }

        DAO.setX3RequestFinishedInOwnTransactionScope(emf, requestState, result);
        throwExceptionIfError(result);
        log.debug("Finished sending PSC Transaction GL Entry");
        return result;
    }

    static CAdxResultXml createGLEntryForCashIn(EntityManager em, LazyX3Connection lazyConn, EntityManagerFactory emf, X3TransactionState transactionState, CashIn cashIn, Collection<CashInRow> cashInRows) throws Exception {
        log.debug("In send GL entry for CashIn");
        CAdxResultXml result;

        X3RequestState requestState = DAO.createX3RequestInOwnTransactionScope(emf, transactionState, REQUEST_TYPE_GL_ENTRY, X3InterfaceDaemon.X3_TRANSACTION_STATUS_IN_PROGRESS);
        if (requestState.getStatus().equals(X3InterfaceDaemon.X3_TRANSACTION_STATUS_FINISHED)) {
            log.warn("This request has been processed already in X3. We wont send it again");
            result = new CAdxResultXml();
            result.setStatus(RESULT_CODE_PROCESSED_BEFORE);
            result.setX3RecordId(requestState.getX3RecordId());
            return result;
        }

        X3DataStream ds = new X3DataStream();

        ds.addGroup("HAE0_1");
        ds.getLastGroup().addField("FCY", props.getProperty("CountrySiteCode")); //Site --> Mandatory - check for invoices
        ds.getLastGroup().addField("TYP", props.getProperty("CashInGLTransactionCode"));
        ds.getLastGroup().addField("NUM", ""); // Not required, can be blank
        ds.getLastGroup().addField("ACCDAT", cashIn.getCashInDateTime()); //  Accounting Date --> Mandatory

        ds.addGroup("HAE0_2");
        ds.getLastGroup().addField("JOU", ""); // Leave blank
        ds.getLastGroup().addField("CAT", "1"); // Always set to 1 
        ds.getLastGroup().addField("STA", "1"); // Always set to 1 
       // ds.getLastGroup().addField("DACDIA", "STDCO"); // Always STDCO

        ds.addGroup("HAE1_1");
        ds.getLastGroup().addField("DESVCR", "CashIn GL Entry"); // Document Description --> Alpha numeric 30 characters

        ds.addGroup("HAE1_3");
        ds.getLastGroup().addField("BPRVCR", "CASHIN" + cashIn.getCashInId()); //Source Document --> can leave blank
        ds.getLastGroup().addField("BPRDATVCR", ""); //Source Date --> can leave blank

        ds.addGroup("HAE1_6");
        ds.getLastGroup().addField("REF", "ci" + cashIn.getCashInId()); // Reference field for matching

        ds.addGroup("HAE1_5");
        ds.getLastGroup().addField("RATDAT", cashIn.getCashInDateTime()); //Exchange Rate Date --> Date of te currency to be used 
        ds.getLastGroup().addField("CUR", BaseUtils.getProperty("env.currency.official.symbol"));  //Document Currency --> Mandatory

        // Split credit into one credit per Sale and pass the sale Id in FREREF
        // The cashed in amount must be split across the sales starting with the oldest sale. Any shortfall would appear in the newer sales
        Map<Integer, Object[]> cashInData = new HashMap<>();
        BigDecimal cashInAmntLeftCents = cashIn.getCashReceiptedInCents();
        BigDecimal totalDebitCentsCashCollection = null;
        BigDecimal totalDebitCentsBank = null;
        BigDecimal totalDebitCentsCardTransactionFee = BigDecimal.ZERO;
        Map<Integer, Object[]> bankCashInDebitData = new HashMap(); // Each Row Looks like - SaleId, [TillId, BankSaleAmntLessFees]
        int lastCashSaleId = 0;
        String lastChannel = null;
        for (CashInRow cashInRow : cashInRows) {
            Sale sale = DAO.getSale(em, cashInRow.getSaleId());
            lastChannel = sale.getChannel();
            BigDecimal thisSalesTotalAmntCents = sale.getSaleTotalCentsIncl().subtract(sale.getWithholdingTaxCents());
            log.debug("Sale [{}] has a total amount of [{}]", sale.getSaleId(), thisSalesTotalAmntCents);
            Object[] saleData = null;
            if (sale.getPaymentMethod().equals(POSManager.PAYMENT_METHOD_CASH)) {
                lastCashSaleId = sale.getSaleId();
                BigDecimal thisSalesCashInAmntCents;
                if (cashInAmntLeftCents.compareTo(thisSalesTotalAmntCents) >= 0) {
                    thisSalesCashInAmntCents = thisSalesTotalAmntCents;
                    log.debug("Sale [{}] will be given a cash in amount of  [{}] as there is enough in the cashin", sale.getSaleId(), thisSalesCashInAmntCents);
                } else {
                    thisSalesCashInAmntCents = cashInAmntLeftCents;
                    log.debug("Sale [{}] will be given a cash in amount of  [{}] as there is not enough in the cash in", sale.getSaleId(), thisSalesCashInAmntCents);
                }
                cashInAmntLeftCents = cashInAmntLeftCents.subtract(thisSalesCashInAmntCents);
                saleData = new Object[2];
                saleData[0] = thisSalesCashInAmntCents.setScale(20);
                saleData[1] = sale.getChannel();
                if (cashIn.getCashInType().equals("office")) {
                    totalDebitCentsCashCollection = cashIn.getCashReceiptedInCents();
                    log.debug("Cash Sale [{}] office cashin amnt is [{}]", new Object[]{cashInRow.getSaleId(), thisSalesCashInAmntCents});
                } else if (cashIn.getCashInType().equals("bankdeposit")) {
                    if (totalDebitCentsBank == null) {
                        totalDebitCentsBank = BigDecimal.ZERO;
                    }
                    totalDebitCentsBank = totalDebitCentsBank.add(thisSalesCashInAmntCents);
                    log.debug("Cash Sale [{}] bank deposit cashin amnt is [{}] and total thus far for bank is [{}]", new Object[]{cashInRow.getSaleId(), thisSalesCashInAmntCents, totalDebitCentsBank});
                } else {
                    throw new Exception("Unknown cashin type -- " + cashIn.getCashInType());
                }

            } else if (sale.getPaymentMethod().equals(POSManager.PAYMENT_METHOD_CARD_PAYMENT)) {
                // Consolidate matching amount per bank so GL's can be per bank
                Object[] bankSaleData = new Object[3];

                saleData = new Object[2];
                saleData[0] = thisSalesTotalAmntCents.setScale(20);
                saleData[1] = sale.getChannel();
                BigDecimal txFeeCents = getCardTransactionFeeCents(thisSalesTotalAmntCents, getCardMachinesBankName(sale.getTillId()));
                thisSalesTotalAmntCents = thisSalesTotalAmntCents.setScale(X3Field.getX3DecimalPlaces() - 2, RoundingMode.HALF_EVEN); // Use rounded amount as X3 would see it

                BigDecimal amntLessFees = thisSalesTotalAmntCents.subtract(txFeeCents).setScale(X3Field.getX3DecimalPlaces() - 2, RoundingMode.HALF_EVEN);
                BigDecimal feesToUseCents = thisSalesTotalAmntCents.subtract(amntLessFees);

                bankSaleData[0] = sale.getTillId();
                bankSaleData[1] = amntLessFees;
                bankSaleData[2] = sale.getPaymentTransactionData(); // Set the Card Numner ....
                totalDebitCentsCardTransactionFee = totalDebitCentsCardTransactionFee.add(feesToUseCents);
                log.debug("Card Payment Sale [{}] cashin amnt is [{}] and total thus far for TX Fees is [{}] and Card Matching is [{}] for bank [{}]", new Object[]{cashInRow.getSaleId(), thisSalesTotalAmntCents, totalDebitCentsCardTransactionFee, bankSaleData[1], sale.getTillId()});
                bankCashInDebitData.put(cashInRow.getSaleId(), bankSaleData);

            }

            if (saleData != null) {
                cashInData.put(cashInRow.getSaleId(), saleData);
            }
        }

        ds.addTable("HAE2_1");
        if (totalDebitCentsCashCollection != null) {
            log.debug("Populating debit line for cash collections");
            ds.getLastTable().addLine(); // DEBIT LINE - Debit Cash Collection
            ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
            ds.getLastTable().getLastLine().addField("ACC1", props.getProperty("CashInGLDebitAccount")); //Account Debit - see finance GL accounts
            ds.getLastTable().getLastLine().addField("BPR", ""); //Business Partner --> Leave Empty 
            ds.getLastTable().getLastLine().addField("DEB", convertCentsToMajor(totalDebitCentsCashCollection)); //Debit Amount --> Only populate if its a debit line
            ds.getLastTable().getLastLine().addField("DES", "CashIn GL Entry DB"); //Line Description --> Description of at line level 30 chars
            ds.getLastTable().getLastLine().addField("CCE1", lastChannel); //Dimension 1 - use the last used channel of the salesperson
            ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
            ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
            ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4  
            ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
            ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
            ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7  
            ds.getLastTable().getLastLine().addField("FREREF", cashIn.getCashInId()); // Cash in Id 
        }

        if (totalDebitCentsBank != null) {
            log.debug("Populating debit line for bank deposit");
            ds.getLastTable().addLine(); // DEBIT LINE - Debit Bank
            ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
            ds.getLastTable().getLastLine().addField("ACC1", props.getProperty("CashInBankDepositGLDebitAccount")); //Account Debit - see finance GL accounts
            ds.getLastTable().getLastLine().addField("BPR", ""); //Business Partner --> Leave Empty 
            ds.getLastTable().getLastLine().addField("DEB", convertCentsToMajor(totalDebitCentsBank)); //Debit Amount --> Only populate if its a debit line
            ds.getLastTable().getLastLine().addField("DES", "CashIn GL Entry DB"); //Line Description --> Description of at line level 30 chars
            ds.getLastTable().getLastLine().addField("CCE1", lastChannel); //Dimension 1 - use the last used channel of the salesperson
            ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
            ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
            ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4  
            ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
            ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
            ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7  
            ds.getLastTable().getLastLine().addField("FREREF", "ci" + cashIn.getCashInId()); // ci + Cash in Id 
        }

        if (totalDebitCentsCardTransactionFee.compareTo(BigDecimal.ZERO) > 0) {
            log.debug("Populating debit line for card payment transaction fees");
            ds.getLastTable().addLine(); // DEBIT LINE - Debit card payment TX fees
            ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
            ds.getLastTable().getLastLine().addField("ACC1", props.getProperty("CashInGLDebitAccountCardTXFees")); //Account Debit - see finance GL accounts
            ds.getLastTable().getLastLine().addField("BPR", ""); //Business Partner --> Leave Empty 
            ds.getLastTable().getLastLine().addField("DEB", convertCentsToMajor(totalDebitCentsCardTransactionFee)); //Debit Amount --> Only populate if its a debit line
            ds.getLastTable().getLastLine().addField("DES", "CashIn GL Entry DB - TX Fees"); //Line Description --> Description of at line level 30 chars
            ds.getLastTable().getLastLine().addField("CCE1", lastChannel); //Dimension 1 - use the last used channel of the salesperson
            ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
            ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
            ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4  
            ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
            ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
            ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7  
            ds.getLastTable().getLastLine().addField("FREREF", cashIn.getCashInId()); // Cash in Id 
        }

        for (Integer saleId : bankCashInDebitData.keySet()) {
            BigDecimal cashInAmnt = (BigDecimal) bankCashInDebitData.get(saleId)[1];
            String bank = (String) bankCashInDebitData.get(saleId)[0];
            String cardNum = (String) bankCashInDebitData.get(saleId)[2];

            if (cashInAmnt != null && cashInAmnt.compareTo(BigDecimal.ZERO) > 0) {
                log.debug("Populating debit line for card payment sale id [{}], under cashin id [{}]", saleId, cashIn.getCashInId());
                ds.getLastTable().addLine(); // DEBIT LINE - Debit Card Matching
                ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
                ds.getLastTable().getLastLine().addField("ACC1", getCardTransactionMatchingAccount(bank)); //Account Debit - see finance GL accounts
                ds.getLastTable().getLastLine().addField("BPR", ""); //Business Partner --> Leave Empty 
                ds.getLastTable().getLastLine().addField("DEB", convertCentsToMajor(cashInAmnt)); //Debit Amount --> Only populate if its a debit line
                ds.getLastTable().getLastLine().addField("DES", "CashIn GL Entry DB - CP Match"); //Line Description --> Description of at line level 30 chars
                ds.getLastTable().getLastLine().addField("CCE1", lastChannel); //Dimension 1 - use the last used channel of the salesperson
                ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
                ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
                ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4  
                ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
                ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
                ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7  
                //String key = props.getProperty("CountrySiteCode") + saleId;
                ds.getLastTable().getLastLine().addField("FREREF", cardNum); // Sale Id 
            }
        }

        for (Integer saleId : cashInData.keySet()) {
            BigDecimal thisSalesCashInAmntCents = (BigDecimal) cashInData.get(saleId)[0];
            String channel = (String) cashInData.get(saleId)[1];
            if (thisSalesCashInAmntCents.compareTo(BigDecimal.ZERO) == 0) {
                // Skip zero GL entires as per request from PB
                continue;
            }

            if (saleId == lastCashSaleId && cashInAmntLeftCents.compareTo(BigDecimal.ZERO) > 0) {
                log.debug("We have processed all cash in rows and yet we still have some cash [{}]c that was brough in and has not been allocated to a sale. We will allocate it to this first cash sale", cashInAmntLeftCents);
                thisSalesCashInAmntCents = thisSalesCashInAmntCents.add(cashInAmntLeftCents);
            }
            log.debug("thisSalesCashInAmntCents [{}] with scale [{}]", thisSalesCashInAmntCents, thisSalesCashInAmntCents.scale());
            ds.getLastTable().addLine(); // CREDIT LINE - Credit Sales Control
            ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
            ds.getLastTable().getLastLine().addField("ACC1", props.getProperty("CashInGLCreditAccount")); //Account Debit - see finance GL accounts
            ds.getLastTable().getLastLine().addField("BPR", ""); //Business Partner --> Leave Empty 
            ds.getLastTable().getLastLine().addField("CDT", convertCentsToMajor(thisSalesCashInAmntCents)); //Credit Amount --> Only populate if its a debit line
            ds.getLastTable().getLastLine().addField("DES", "CashIn GL Entry CR " + props.getProperty("CountrySiteCode") + saleId); //Line Description --> Description of at line level 30 chars
            ds.getLastTable().getLastLine().addField("CCE1", channel); //Dimension 1 
            ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
            ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
            ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4  
            ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
            ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
            ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7  
            String key = props.getProperty("CountrySiteCode") + saleId;
            ds.getLastTable().getLastLine().addField("FREREF", key); // Sale Id 
            boolean existsAlready = doesCashInExistInX3(lazyConn, String.valueOf(cashIn.getCashInId()), cashIn.getCashInDateTime());
            if (existsAlready) {
                log.warn("This cashin [{}] has been processed already in X3. We wont send it again", key);
                result = new CAdxResultXml();
                result.setX3RecordId("NA");
                result.setStatus(RESULT_CODE_SUCCESS);
                result.setResultXml("Skipped as this cashin is in X3 already");
                addMessageIntoResult(result, "Skipped as this cashin is in X3 already", "None");
                result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_FINISHED);
                DAO.setX3RequestFinishedInOwnTransactionScope(emf, requestState, result);
                return result;
            }
        }

        result = save("WSGAS", ds.getXML()); // Create the GL Entry
        result.setX3RecordId(getX3RecordIdNumber(result.getResultXml(), X3Helper.TYPE_GL));
        if (result.getX3RecordId() == null) {
            log.warn("No GL number returned so there must have been an error");
            result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_ERROR);
            addMessageIntoResult(result, "No GL number was created", "X3 error");
        }
        DAO.setX3RequestFinishedInOwnTransactionScope(emf, requestState, result);
        throwExceptionIfError(result);
        log.debug("Finished sending GL Entry for CashIn");
        return result;
    }

    /**
     * - Debit card Tx fees - Debit Card Matching - Credit Sales Control
     *
     * @param em
     * @param emf
     * @param transactionState
     * @param sale
     * @param salesRows
     * @return
     */
    static CAdxResultXml createGLEntryForCardIntegrationSale(EntityManager em, LazyX3Connection lazyConn, EntityManagerFactory emf, X3TransactionState transactionState, Sale sale, Collection<SaleRow> salesRows) throws Exception {
        log.debug("In send GL entry for Card Integration Sale");
        CAdxResultXml result;

        X3RequestState requestState = DAO.createX3RequestInOwnTransactionScope(emf, transactionState, REQUEST_TYPE_GL_ENTRY, X3InterfaceDaemon.X3_TRANSACTION_STATUS_IN_PROGRESS);
        if (requestState.getStatus().equals(X3InterfaceDaemon.X3_TRANSACTION_STATUS_FINISHED)) {
            log.warn("This request has been processed already in X3. We wont send it again");
            result = new CAdxResultXml();
            result.setStatus(RESULT_CODE_PROCESSED_BEFORE);
            result.setX3RecordId(requestState.getX3RecordId());
            return result;
        }

        BigDecimal thisSalesTotalAmntCents = sale.getSaleTotalCentsIncl().subtract(sale.getWithholdingTaxCents());
        BigDecimal txFeeCents = getCardTransactionFeeCents(thisSalesTotalAmntCents, getCardMachinesBankName(sale.getPaymentGatewayCode()));
        thisSalesTotalAmntCents = thisSalesTotalAmntCents.setScale(X3Field.getX3DecimalPlaces() - 2, RoundingMode.HALF_EVEN); // Use rounded amount as X3 would see it
        BigDecimal amntLessFees = thisSalesTotalAmntCents.subtract(txFeeCents).setScale(X3Field.getX3DecimalPlaces() - 2, RoundingMode.HALF_EVEN);
        BigDecimal feesToUseCents = thisSalesTotalAmntCents.subtract(amntLessFees);
        log.debug("Card Integration Payment Sale [{}] sale amnt is [{}] and TX Fees is [{}] and Card Matching is [{}] for bank [{}]", new Object[]{sale.getSaleId(), thisSalesTotalAmntCents, feesToUseCents, amntLessFees, getCardMachinesBankName(sale.getPaymentGatewayCode())});

        X3DataStream ds = new X3DataStream();

        ds.addGroup("HAE0_1");
        ds.getLastGroup().addField("FCY", props.getProperty("CountrySiteCode")); //Site --> Mandatory - check for invoices
        ds.getLastGroup().addField("TYP", props.getProperty("CashInGLTransactionCode"));
        ds.getLastGroup().addField("NUM", ""); // Not required, can be blank
        ds.getLastGroup().addField("ACCDAT", sale.getSaleDateTime()); //  Accounting Date --> Mandatory

        ds.addGroup("HAE0_2");
        ds.getLastGroup().addField("JOU", ""); // Leave blank
        ds.getLastGroup().addField("CAT", "1"); // Always set to 1 
        ds.getLastGroup().addField("STA", "1"); // Always set to 1 
        //ds.getLastGroup().addField("DACDIA", "STDCO"); // Always STDCO

        ds.addGroup("HAE1_1");
        ds.getLastGroup().addField("DESVCR", "CashIn GL Entry"); // Document Description --> Alpha numeric 30 characters

        ds.addGroup("HAE1_3");
        ds.getLastGroup().addField("BPRVCR", "CIS" + sale.getSaleId()); //Source Document --> can leave blank
        ds.getLastGroup().addField("BPRDATVCR", ""); //Source Date --> can leave blank

        ds.addGroup("HAE1_5");
        ds.getLastGroup().addField("RATDAT", sale.getSaleDateTime()); //Exchange Rate Date --> Date of te currency to be used 
        ds.getLastGroup().addField("CUR", BaseUtils.getProperty("env.currency.official.symbol"));  //Document Currency --> Mandatory

        ds.addTable("HAE2_1");

        log.debug("Populating debit line for card payment transaction fees");
        ds.getLastTable().addLine(); // DEBIT LINE - Debit card payment TX fees
        ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
        ds.getLastTable().getLastLine().addField("ACC1", props.getProperty("CashInGLDebitAccountCardTXFees")); //Account Debit - see finance GL accounts
        ds.getLastTable().getLastLine().addField("BPR", ""); //Business Partner --> Leave Empty 
        ds.getLastTable().getLastLine().addField("DEB", convertCentsToMajor(feesToUseCents)); //Debit Amount --> Only populate if its a debit line
        ds.getLastTable().getLastLine().addField("DES", "CashIn GL Entry DB - TX Fees"); //Line Description --> Description of at line level 30 chars
        ds.getLastTable().getLastLine().addField("CCE1", sale.getChannel()); //Dimension 1 
        ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
        ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
        ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4  
        ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
        ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
        ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7  
        ds.getLastTable().getLastLine().addField("FREREF", sale.getSaleId()); // Sale in Id 

        log.debug("Populating debit line for card payment matching account for bank [{}]", getCardMachinesBankName(sale.getPaymentGatewayCode()));
        ds.getLastTable().addLine(); // DEBIT LINE - Debit Card Matching
        ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
        ds.getLastTable().getLastLine().addField("ACC1", getCardTransactionMatchingAccount(getCardMachinesBankName(sale.getPaymentGatewayCode()))); //Account Debit - see finance GL accounts
        ds.getLastTable().getLastLine().addField("BPR", ""); //Business Partner --> Leave Empty 
        ds.getLastTable().getLastLine().addField("DEB", convertCentsToMajor(amntLessFees)); //Debit Amount --> Only populate if its a debit line
        ds.getLastTable().getLastLine().addField("DES", "CashIn GL Entry DB - CP Match"); //Line Description --> Description of at line level 30 chars
        ds.getLastTable().getLastLine().addField("CCE1", sale.getChannel()); //Dimension 1 
        ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
        ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
        ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4  
        ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
        ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
        ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7  
        ds.getLastTable().getLastLine().addField("FREREF", sale.getPaymentTransactionData()); // Raference Id that will be on the deposit

        ds.getLastTable().addLine(); // CREDIT LINE - Credit Sales Control
        ds.getLastTable().getLastLine().addField("FCYLIN", props.getProperty("CountrySiteCode")); //Line Site --> same as FCY field above
        ds.getLastTable().getLastLine().addField("ACC1", props.getProperty("CashInGLCreditAccount")); //Account Debit - see finance GL accounts
        ds.getLastTable().getLastLine().addField("BPR", ""); //Business Partner --> Leave Empty 
        ds.getLastTable().getLastLine().addField("CDT", convertCentsToMajor(thisSalesTotalAmntCents)); //Credit Amount --> Only populate if its a debit line
        ds.getLastTable().getLastLine().addField("DES", "CashIn GL Entry CR " + props.getProperty("CountrySiteCode") + sale.getSaleId()); //Line Description --> Description of at line level 30 chars
        ds.getLastTable().getLastLine().addField("CCE1", sale.getChannel()); //Dimension 1 
        ds.getLastTable().getLastLine().addField("CCE2", ""); //Dimension 2
        ds.getLastTable().getLastLine().addField("CCE3", getDepartment()); //Dimension 3  
        ds.getLastTable().getLastLine().addField("CCE4", ""); //Dimension 4  
        ds.getLastTable().getLastLine().addField("CCE5", ""); //Dimension 5  
        ds.getLastTable().getLastLine().addField("CCE6", ""); //Dimension 6  
        ds.getLastTable().getLastLine().addField("CCE7", ""); //Dimension 7  
        String key = props.getProperty("CountrySiteCode") + sale.getSaleId();
        ds.getLastTable().getLastLine().addField("FREREF", key); // Sale Id 

        boolean existsAlready = doesCashInExistInX3(lazyConn, String.valueOf(sale.getSaleId()), sale.getSaleDateTime());
        if (existsAlready) {
            log.warn("This cashin [{}] has been processed already in X3. We wont send it again", key);
            result = new CAdxResultXml();
            result.setX3RecordId("NA");
            result.setStatus(RESULT_CODE_SUCCESS);
            result.setResultXml("Skipped as this cashin is in X3 already");
            addMessageIntoResult(result, "Skipped as this cashin is in X3 already", "None");
            result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_FINISHED);
            DAO.setX3RequestFinishedInOwnTransactionScope(emf, requestState, result);
            return result;
        }

        result = save("WSGAS", ds.getXML()); // Create the GL Entry
        result.setX3RecordId(getX3RecordIdNumber(result.getResultXml(), X3Helper.TYPE_GL));
        if (result.getX3RecordId() == null) {
            log.warn("No GL number returned so there must have been an error");
            result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_ERROR);
            addMessageIntoResult(result, "No GL number was created", "X3 error");
        }
        DAO.setX3RequestFinishedInOwnTransactionScope(emf, requestState, result);
        throwExceptionIfError(result);
        log.debug("Finished sending GL Entry for Card Integration Payment");
        return result;
    }

    private static String getCardTransactionMatchingAccount(String bank) {
        String rules = BaseUtils.getProperty("env.pos.cardpayment.transactionfees", "");
        String bankRule = Utils.getValueFromCRDelimitedAVPString(rules, bank);
        if (bankRule == null) {
            throw new RuntimeException("No configuration for bank -- " + bank);
        } else {
            return bankRule.split("\\|")[2];
        }
    }

    private static BigDecimal getCardTransactionFeeCents(BigDecimal amountCents, String bank) {
        BigDecimal ret;
        BigDecimal percent;
        BigDecimal maxCents;
        // e.g.:
        // eco bank=1.5|20000
        // mybank=1.25|30000
        String rules = BaseUtils.getProperty("env.pos.cardpayment.transactionfees", "");
        String bankRule = Utils.getValueFromCRDelimitedAVPString(rules, bank);
        if (bankRule == null) {
            log.debug("No card transaction fees rule for bank [{}] in env.pos.cardpayment.transactionfees. Using defaults", bank);
            percent = new BigDecimal(1.25);
            maxCents = new BigDecimal(200000);
        } else {
            log.debug("Bank card transaction fees rule for [{}] is [{}]", bank, bankRule);
            percent = new BigDecimal(Double.parseDouble(bankRule.split("\\|")[0]));
            maxCents = new BigDecimal(Double.parseDouble(bankRule.split("\\|")[1]));
        }

        BigDecimal fee = amountCents.multiply(percent).divide(new BigDecimal(100), RoundingMode.HALF_EVEN);
        if (fee.compareTo(maxCents) > 0) {
            ret = maxCents;
        } else {
            ret = fee;
        }
        log.debug("Card payment transaction fee on [{}]cents is [{}]cents", amountCents, ret);

        return ret;

    }

    public static String getFieldValueFromExtraInfo(String extraInfo, String fieldName) throws Exception {
        if (extraInfo != null) {
            StringTokenizer stValues = new StringTokenizer(extraInfo, "\r\n");
            while (stValues.hasMoreTokens()) {
                String val = stValues.nextToken().trim();
                if (!val.isEmpty()) {
                    String[] fieldVal = val.split("=");
                    if (fieldVal[0].equalsIgnoreCase(fieldName)) {
                        if (fieldVal.length > 1) { //Prevent java.lang.ArrayIndexOutOfBoundsException: 1 errors
                            return fieldVal[1].trim();
                        } else { // Return empty string because no value was set for this field.
                            return "";
                        }
                    }
                }
            }
        }
        return null;
    }

    public static BigDecimal getBigDecimalFieldValueFromExtraInfo(String extraInfo, String fieldName) throws Exception {
        if (extraInfo != null) {
            StringTokenizer stValues = new StringTokenizer(extraInfo, "\r\n");
            while (stValues.hasMoreTokens()) {
                String val = stValues.nextToken().trim();
                if (!val.isEmpty()) {
                    String[] fieldVal = val.split("=");
                    if (fieldVal[0].equalsIgnoreCase(fieldName)) {
                        return new BigDecimal(fieldVal[1].trim());
                    }
                }
            }
        }
        return BigDecimal.ZERO; // Return ZERO if field not found.
    }

    public static boolean isSaleGeneratedFromQuote(String saleExtraInfo) throws Exception {
        String quoteId = getFieldValueFromExtraInfo(saleExtraInfo, "QuoteId");
        return (quoteId != null && quoteId.length() > 0);
    }

    private static boolean doesItemHaveSubItems(SaleRow row, Collection<SaleRow> salesRows) {
        if (row.getParentSaleRowId() != 0) {
            // If the item has a parent, it cannot have sub items
            return false;
        }
        for (SaleRow row2 : salesRows) {
            if (row.getSaleRowId() == row2.getParentSaleRowId()) {
                // There is another row in the sale which is a child of this row
                return true;
            }
        }
        return false;
    }

    private static void checkForShutdown() {
        if (BaseUtils.IN_SHUTDOWN) {
            throw new java.lang.IllegalStateException("JVM is in shutdown so X3 calls disallowed");
        }
    }

    private static CAdxResultXml run(String publicName, String xml, boolean shortTimeout) throws Exception {
        CAdxResultXml result;
        long start = System.currentTimeMillis();
        try {
            if (log.isDebugEnabled()) {
                log.debug("Calling run on X3 web service [{}] passing [{}]", publicName, xml);
            }
            if (shortTimeout) {
                result = getShortTimeoutX3Service().run(getCAdxCallContext(), publicName, xml);
            } else {
                result = getX3Service().run(getCAdxCallContext(), publicName, xml);
            }
        } catch (Exception e) {
            log.warn("Error calling run on X3: ", e);
            result = new CAdxResultXml();
            result.setStatus(RESULT_CODE_SYSTEM_ERROR);
            result.setResultXml(e.toString());
            CAdxMessage msg = new CAdxMessage();
            msg.setMessage(e.toString() + ". Server:" + BaseUtils.getHostNameFromKernel());
            msg.setType("System error");
            result.setMessages(new CAdxMessage[]{msg});
        }
        BaseUtils.addStatisticSample("X3Run-" + publicName, BaseUtils.STATISTIC_TYPE.latency, System.currentTimeMillis() - start, 600000);
        result.logResult(log);
        return result;
    }

    private static CAdxResultXml save(String publicName, String xml) {
        CAdxResultXml result;
        long start = System.currentTimeMillis();
        try {
            if (log.isDebugEnabled()) {
                log.debug("Calling save on X3 web service [{}] passing [{}]", publicName, xml);
            }
            result = getX3Service().save(getCAdxCallContext(), publicName, xml);
            setStatusCodeOnResult(result);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Exception in save: ", e);
            }
            result = new CAdxResultXml();
            result.setStatus(RESULT_CODE_SYSTEM_ERROR);
            result.setResultXml(e.toString());
            CAdxMessage msg = new CAdxMessage();
            msg.setMessage(e.toString() + ". Server:" + BaseUtils.getHostNameFromKernel());
            msg.setType("System error");
            result.setMessages(new CAdxMessage[]{msg});
            result.setTechnicalInfos(new CAdxTechnicalInfos());
            result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_ERROR);
        }
        BaseUtils.addStatisticSample("X3Save-" + publicName, BaseUtils.STATISTIC_TYPE.latency, System.currentTimeMillis() - start, 600000);
        result.logResult(log);
        return result;
    }

    private static CAdxResultXml read(String publicName, CAdxParamKeyValue[] objKey) throws Exception {
        CAdxResultXml result;
        long start = System.currentTimeMillis();
        try {
            if (log.isDebugEnabled()) {
                log.debug("Calling read on X3 web service [{}] passing [{}]", publicName, getCAdxParamKeyValueAsString(objKey));
            }
            result = getX3Service().read(getCAdxCallContext(), publicName, objKey);
        } catch (Exception e) {
            result = new CAdxResultXml();
            result.setStatus(RESULT_CODE_SYSTEM_ERROR);
            result.setResultXml(e.toString());
            CAdxMessage msg = new CAdxMessage();
            msg.setMessage(e.toString() + ". Server:" + BaseUtils.getHostNameFromKernel());
            msg.setType("System error");
            result.setMessages(new CAdxMessage[]{msg});
        }
        BaseUtils.addStatisticSample("X3Read-" + publicName, BaseUtils.STATISTIC_TYPE.latency, System.currentTimeMillis() - start, 600000);
        result.logResult(log);
        return result;
    }

    private static CAdxResultXml delete(String publicName, CAdxParamKeyValue[] objKey) throws Exception {
        CAdxResultXml result;
        long start = System.currentTimeMillis();
        try {
            if (log.isDebugEnabled()) {
                log.debug("Calling delete on X3 web service [{}] passing [{}]", publicName, getCAdxParamKeyValueAsString(objKey));
            }
            result = getX3Service().delete(getCAdxCallContext(), publicName, objKey);
        } catch (Exception e) {
            result = new CAdxResultXml();
            result.setStatus(RESULT_CODE_SYSTEM_ERROR);
            result.setResultXml(e.toString());
            CAdxMessage msg = new CAdxMessage();
            msg.setMessage(e.toString() + ". Server:" + BaseUtils.getHostNameFromKernel());
            msg.setType("System error");
            result.setMessages(new CAdxMessage[]{msg});
        }
        BaseUtils.addStatisticSample("X3Delete-" + publicName, BaseUtils.STATISTIC_TYPE.latency, System.currentTimeMillis() - start, 600000);
        result.logResult(log);
        return result;
    }

    private static CAdxResultXml modify(String publicName, CAdxParamKeyValue[] objKey, String xml) throws Exception {
        CAdxResultXml result;
        long start = System.currentTimeMillis();
        try {
            if (log.isDebugEnabled()) {
                log.debug("Calling modify on X3 web service [{}] passing objKey [{}] and XML [{}]", new Object[]{publicName, getCAdxParamKeyValueAsString(objKey), xml});
            }
            result = getX3Service().modify(getCAdxCallContext(), publicName, objKey, xml);
            String sepId = Utils.getBetween(xml, "<FLD NAME=\"SEPID\">", "</FLD>");
            setStatusCodeOnResult(result);
        } catch (Exception e) {
            result = new CAdxResultXml();
            result.setStatus(RESULT_CODE_SYSTEM_ERROR);
            result.setResultXml(e.toString());
            CAdxMessage msg = new CAdxMessage();
            msg.setMessage(e.toString() + ". Server:" + BaseUtils.getHostNameFromKernel());
            msg.setType("System error");
            result.setMessages(new CAdxMessage[]{msg});
        }
        BaseUtils.addStatisticSample("X3Modify-" + publicName, BaseUtils.STATISTIC_TYPE.latency, System.currentTimeMillis() - start, 600000);
        result.logResult(log);
        return result;
    }

    private static CAdxResultXml query(String publicName, CAdxParamKeyValue[] objKey, int number) throws Exception {
        CAdxResultXml result;
        long start = System.currentTimeMillis();
        try {
            if (log.isDebugEnabled()) {
                log.debug("Calling query on X3 web service [{}] passing objKey [{}] and number [{}]", new Object[]{publicName, getCAdxParamKeyValueAsString(objKey), number});
            }
            result = getX3Service().query(getCAdxCallContext(), publicName, objKey, number);
        } catch (Exception e) {
            result = new CAdxResultXml();
            result.setStatus(RESULT_CODE_SYSTEM_ERROR);
            result.setResultXml(e.toString());
            CAdxMessage msg = new CAdxMessage();
            msg.setMessage(e.toString() + ". Server:" + BaseUtils.getHostNameFromKernel());
            msg.setType("System error");
            result.setMessages(new CAdxMessage[]{msg});
        }
        BaseUtils.addStatisticSample("X3Query-" + publicName, BaseUtils.STATISTIC_TYPE.latency, System.currentTimeMillis() - start, 600000);
        result.logResult(log);
        return result;
    }

    public static boolean ready() {
        if (BaseUtils.IN_SHUTDOWN) {
            return false;
        }
        try {
            getX3Service();
            getCAdxCallContext();
            return true;
        } catch (Exception e) {
            log.warn("Error when checking if X3 interface is ready :" + e.toString());
        }
        return false;
    }

    private static CAdxWebServiceXmlCC getX3Service() throws Exception {
        checkForShutdown();
        if (x3Service != null) {
            ((Stub) x3Service).setTimeout(Integer.parseInt(props.getProperty("TimeOutSecs")) * 1000);        
            ((Stub) x3Service).setUsername(props.getProperty("CodeUser"));
            ((Stub) x3Service).setPassword(props.getProperty("Password"));
            ((Stub) x3Service)._setProperty("PreAuthorise", true);
            return x3Service;
        }
        if (props == null) {
            throw new Exception("X3 interface is not initialised yet - props is null");
        }

        CAdxWebServiceXmlCCServiceLocator serviceLocator = new CAdxWebServiceXmlCCServiceLocator();
        serviceLocator.setCAdxWebServiceXmlCCEndpointAddress(props.getProperty("URL"));
        x3Service = serviceLocator.getCAdxWebServiceXmlCC();        
        ((Stub) x3Service).setTimeout(Integer.parseInt(props.getProperty("TimeOutSecs")) * 1000);        
        ((Stub) x3Service).setUsername(props.getProperty("CodeUser"));
        ((Stub) x3Service).setPassword(props.getProperty("Password"));
        ((Stub) x3Service)._setProperty("PreAuthorise", true);
        
        return x3Service;
    }

    private static CAdxWebServiceXmlCC getShortTimeoutX3Service() throws Exception {
        checkForShutdown();
        if (x3ShortTimeoutService != null) {
            ((Stub) x3ShortTimeoutService).setTimeout(Integer.parseInt(props.getProperty("ShortTimeOutSecs")) * 1000);
            ((Stub) x3ShortTimeoutService).setUsername(props.getProperty("CodeUser"));
            ((Stub) x3ShortTimeoutService).setPassword(props.getProperty("Password"));
            ((Stub) x3ShortTimeoutService)._setProperty("PreAuthorise", true);
            return x3ShortTimeoutService;
        }
        if (props == null) {
            throw new Exception("X3 interface is not initialised yet - props is null");
        }

        CAdxWebServiceXmlCCServiceLocator serviceLocator = new CAdxWebServiceXmlCCServiceLocator();        
        serviceLocator.setCAdxWebServiceXmlCCEndpointAddress(props.getProperty("URL"));
        x3ShortTimeoutService = serviceLocator.getCAdxWebServiceXmlCC();
        ((Stub) x3ShortTimeoutService).setTimeout(Integer.parseInt(props.getProperty("ShortTimeOutSecs")) * 1000);
	((Stub) x3ShortTimeoutService).setUsername(props.getProperty("CodeUser"));
        ((Stub) x3ShortTimeoutService).setPassword(props.getProperty("Password"));
        ((Stub) x3ShortTimeoutService)._setProperty("PreAuthorise", true);
        return x3ShortTimeoutService;
    }

    static void removeX3PaymentNumber(LazyX3Connection lazyConn, int saleId) throws Exception {
        log.debug("Removing payment number form X3 ZSEP table where SEPID = [{}]", props.getProperty("CountrySiteCode") + String.valueOf(saleId));
        PreparedStatement ps = null;
        try {
            ps = lazyConn.prepareStatement(BaseUtils.getProperty("env.pos.x3.set.payment.number.query"));
            ps.setString(1, ""); // To clear the payment number
            ps.setString(2, props.getProperty("CountrySiteCode") + String.valueOf(saleId));
            log.debug("About to run query to update payment number on the ZSEP table.");
            long start = System.currentTimeMillis();
            ps.executeUpdate();
            log.debug("Finished running query to update payment number on the ZSEP table. Query took [{}]ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.warn("Error calling MSSQL to update payment number on the ZSEP table: [{}].", e.toString());
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS-X3", "Error updating X3 payment number: " + e.toString() + ", SEPID:" + saleId);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ex) {
            }
        }
    }

    static int getX3BoxSize(LazyX3Connection lazyConn, String itemNumber) throws Exception {
        log.debug("Getting the size of boxed item number [{}] from X3", itemNumber);
        PreparedStatement ps = null;
        int boxSize = 0;
        try {
            ps = lazyConn.prepareStatement(BaseUtils.getProperty("env.pos.x3.get.item.box.size.query", "select DTY_0 from SC001.ITMMASTER ITM where ITM.ITMREF_0 = ?"));
            ps.setString(1, itemNumber); // Box item number
            log.debug("About to run query to get box size from ITMMASTER table.");
            long start = System.currentTimeMillis();
            ResultSet rs = ps.executeQuery();
            log.debug("Finished running query to get box size. Query took [{}]ms", System.currentTimeMillis() - start);

            while (rs.next()) {
                boxSize = rs.getInt(1);
                break;
            }
            log.debug("Box size for item [{}] is [{}].", itemNumber, boxSize);
        } catch (Exception e) {
            log.warn("Error calling MSSQL to get Box size from ITMMASTER table: [{}].", e.toString());
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS-X3", "Error getting box size for item " + itemNumber + ", ERROR:" + e.toString());
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ex) {
            }
        }
        return boxSize;
    }

    public static void initialise() {
        props = new Properties();
        try {
            log.warn("Here are the X3 Web service Properties as contained in env.x3.config \n");
            log.warn(BaseUtils.getProperty("env.x3.config"));
            props.load(BaseUtils.getPropertyAsStream("env.x3.config"));
            CAdxCallContext cc = new CAdxCallContext(); // Instance of CAdxCallContext
            cc.setCodeLang(props.getProperty("CodeLang"));
            /*cc.setCodeUser(props.getProperty("CodeUser"));
            cc.setPassword(props.getProperty("Password"));            */
            cc.setPoolAlias(props.getProperty("PoolAlias"));
            
            cc.setRequestConfig(props.getProperty("RequestConfig")); // Request configuration string
            CAdxCallContext = cc;
            x3Service = null;
            x3ShortTimeoutService = null;
        } catch (Exception ex) {
            log.error("Failed to load properties from env.x3.config: ", ex);
        }
    }

    private static CAdxCallContext getCAdxCallContext() throws Exception {
        if (CAdxCallContext == null) {
            throw new Exception("X3 interface is not initialised yet - initProperties has not succeeded yet");
        }
        return CAdxCallContext;
    }

    private static String getCAdxParamKeyValueAsString(CAdxParamKeyValue[] objKey) {
        StringBuilder sb = new StringBuilder();
        for (CAdxParamKeyValue keyVal : objKey) {
            sb.append("[");
            sb.append(keyVal.getKey());
            sb.append("=");
            sb.append(keyVal.getValue());
            sb.append("]");
        }
        return sb.toString();
    }

    public static String getX3RecordIdNumber(String resultXml, int txType) {
        try {
            String ret = null;
            // Not all of these have been verified. We should choose the best record id to retrieve and store per transaction type
            switch (txType) {
                case X3Helper.TYPE_QUOTE_WITH_STOCK_RESERVATION:
                    ret = Utils.getBetween(resultXml, "<FLD NAME=\"SOHNUM\" TYPE=\"Char\">", "</FLD>");
                    log.debug("Sales Order Number is [{}]", ret);
                    return ret;
                case X3Helper.TYPE_CASH_SALE_WITH_RECEIPT:
                    ret = Utils.getBetween(resultXml, "<FLD NAME=\"SIHNUM\" TYPE=\"Char\">", "</FLD>");
                    log.debug("TYPE_CASH_SALE_WITH_RECEIPT Invoice Number is [{}] for input [{}]", ret, resultXml);
                    return ret;
                case X3Helper.TYPE_CREDIT_NOTE_SALE:
                    ret = Utils.getBetween(resultXml, "<FLD NAME=\"SIHNUM\" TYPE=\"Char\">", "</FLD>");
                    log.debug("TYPE_CREDIT_NOTE_SALE Invoice Number is [{}] for input [{}]", ret, resultXml);
                    return ret;
                case X3Helper.TYPE_CREDIT_SALE_WITH_PENDING_PAYMENT:
                    ret = Utils.getBetween(resultXml, "<FLD NAME=\"SIHNUM\" TYPE=\"Char\">", "</FLD>");
                    log.debug("TYPE_CREDIT_SALE_WITH_PENDING_PAYMENT Invoice Number is [{}] for input [{}]", ret, resultXml);
                    return ret;
                case X3Helper.TYPE_CREDIT_SALE_WITH_PAYMENT_PROCESS:
                    ret = Utils.getBetween(resultXml, "<FLD NAME=\"SIHNUM\" TYPE=\"Char\">", "</FLD>");
                    log.debug("TYPE_CREDIT_SALE_WITH_PAYMENT_PROCESS Invoice Number is [{}] for input [{}]", ret, resultXml);
                    return ret;
                case X3Helper.TYPE_EFT_WITH_PENDING_PAYMENT:
                    ret = Utils.getBetween(resultXml, "<FLD NAME=\"SIHNUM\" TYPE=\"Char\">", "</FLD>");
                    log.debug("TYPE_EFT_WITH_PENDING_PAYMENT Invoice Number is [{}] for input [{}]", ret, resultXml);
                    return ret;
                case X3Helper.TYPE_EFT_SALE_WITH_PAYMENT_PROCESS:
                    ret = Utils.getBetween(resultXml, "<FLD NAME=\"SIHNUM\" TYPE=\"Char\">", "</FLD>");
                    log.debug("TYPE_EFT_SALE_WITH_PAYMENT_PROCESS Invoice Number is [{}] for input [{}]", ret, resultXml);
                    return ret;
                case X3Helper.TYPE_LOAN:
                    ret = Utils.getBetween(resultXml, "<FLD NAME=\"SDHNUM\" TYPE=\"Char\">", "</FLD>");
                    log.debug("TYPE_LOAN Invoice Number is [{}] for input [{}]", ret, resultXml);
                    return ret;
                case X3Helper.TYPE_LOAN_RETURN:
                    ret = Utils.getBetween(resultXml, "<FLD NAME=\"SRHNUM\" TYPE=\"Char\">", "</FLD>");
                    log.debug("TYPE_LOAN_RETURN Invoice Number is [{}]", ret);
                    return ret;
                case X3Helper.TYPE_PAYMENT:
                    ret = Utils.getBetween(resultXml, "<FLD NAME=\"PYHNUM\" TYPE=\"Char\">", "</FLD>");
                    log.debug("Payment Number is [{}]", ret);
                    return ret;
                case X3Helper.TYPE_GL:
                    ret = Utils.getBetween(resultXml, "<FLD NAME=\"NUM\" TYPE=\"Char\">", "</FLD>");
                    log.debug("GL Entry Number is [{}]", ret);
                    return ret;
                case X3Helper.TYPE_CREDIT_NOTE:
                    ret = Utils.getLastBetween(resultXml, "<FLD NAME=\"ZCRNNUM\" TYPE=\"Char\">", "</FLD>");
                    log.debug("Credit Note Number is [{}]", ret);
                    return ret;
                case X3Helper.TYPE_PAYMENT_CARD_INTEGRATION:
                    ret = Utils.getLastBetween(resultXml, "<FLD NAME=\"PYHNUM\" TYPE=\"Char\">", "</FLD>");
                    log.debug("Payment Number is [{}]", ret);
                    return ret;
                case X3Helper.TYPE_CARD_INTEGRATION_WITH_PENDING_PAYMENT:
                    ret = Utils.getLastBetween(resultXml, "<FLD NAME=\"SIHNUM\" TYPE=\"Char\">", "</FLD>");
                    log.debug("TYPE_CARD_INTEGRATION_WITH_PENDING_PAYMENT Invoice Number is [{}] for input [{}]", ret, resultXml);
                    return ret;
                default:
                    return ret;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static void setStatusCodeOnResult(CAdxResultXml result) {
        for (CAdxMessage msg : result.getMessages()) {
            if (msg.getMessage().contains("Record already exists") && msg.getMessage().contains("Transaction stopped")) {
                String errMsg = String.format("Request has already been processed in X3. Setting status to ER (to force a retry), result code to [%s] and X3_RECORD_ID to [{%s}]", RESULT_CODE_RECORD_EXISTS, result.getX3RecordId());
                log.debug(errMsg);

                result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_ERROR);
                result.setStatus(RESULT_CODE_RECORD_EXISTS);

                result.setMessages(Arrays.copyOf(result.getMessages(), result.getMessages().length + 1));

                //Adding the errMsg to result messages
                CAdxMessage tmp = new CAdxMessage();
                tmp.setMessage(errMsg);
                tmp.setType(msg.getType());
                result.getMessages()[result.getMessages().length - 1] = tmp;

                return;
            }
        }
        if (result.getResultXml() == null) {
            result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_ERROR);
        } else {
            result.setStatusCode(X3InterfaceDaemon.X3_TRANSACTION_STATUS_FINISHED);
        }
    }

    private static void throwExceptionIfError(CAdxResultXml result) throws Exception {
        if (!result.getStatusCode().equals(X3InterfaceDaemon.X3_TRANSACTION_STATUS_FINISHED)) {
            throw new Exception("Error processing request in X3: " + result.getErrorString());
        }
    }

    private static void addMessageIntoResult(CAdxResultXml result, String message, String type) {
        int newSize = result.getMessages() == null ? 1 : result.getMessages().length + 1;
        CAdxMessage[] newMessages = new CAdxMessage[newSize];
        CAdxMessage msg = new CAdxMessage();
        msg.setMessage(message);
        msg.setType(type);
        for (int i = 0; i < newSize; i++) {
            if (i == (newSize - 1)) {
                newMessages[i] = msg;
            } else {
                newMessages[i] = result.getMessages()[i];
            }
        }
        result.setMessages(newMessages);
    }

    /**
     * **********************************
     *
     * GENERAL X3 DATABASE LOOKUPS
     *
     ***********************************
     */
    public static Collection<X3PaymentData> getUnallocatedX3Payments(LazyX3Connection lazyConn) throws Exception {
        log.debug("Getting X3 unallocated payments");
        List<X3PaymentData> paymentDataList = new ArrayList<>();
        PreparedStatement ps = null;
        try {
            ps = lazyConn.prepareStatement(BaseUtils.getProperty("env.pos.x3.payments.query"));
            ps.setString(1, props.getProperty("CountrySiteCode"));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                X3PaymentData data = new X3PaymentData();
                data.depositReference = rs.getString(1);
                data.paymentInCents = rs.getDouble(2) * 100;
                data.paymentTransactionData = rs.getString(3);
                paymentDataList.add(data);
                log.debug("Found an unallocated payment with reference [{}] amount [{}]cents payment transaction data [{}]", new Object[]{data.depositReference, data.paymentInCents, data.paymentTransactionData});
            }
            log.debug("Found [{}] unallocated payments to process", paymentDataList.size());
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ex) {
            }
        }
        return paymentDataList;
    }

    public static Double getX3ReturnedAmount(String resultXml, int amtType) {
        try {
            Double ret = null;
            // Not all of these have been verified. We should choose the best record id to retrieve and store per transaction type
            switch (amtType) {
                case X3Helper.AMOUNT_TYPE_AVAILABLE_BALANCE: // What they can still shop for
                    ret = new Double(Utils.getBetween(resultXml, "<FLD NAME=\"LAVAIL\" TYPE=\"Decimal\">", "</FLD>"));
                    log.debug("Available Balance Amount is [{}]", ret);
                    return ret;
                case X3Helper.AMOUNT_TYPE_CREDIT_LIMIT: // Maximum spend allowed
                    ret = new Double(Utils.getBetween(resultXml, "<FLD NAME=\"LLIMIT\" TYPE=\"Decimal\">", "</FLD>"));
                    log.debug("Credit Limit Amount [{}]", ret);
                    return ret;
                case X3Helper.AMOUNT_TYPE_CURRENT_BALANCE: // How much they have spent so far
                    ret = new Double(Utils.getBetween(resultXml, "<FLD NAME=\"LBALANCE\" TYPE=\"Decimal\">", "</FLD>"));
                    log.debug("Current Balance Amount [{}]", ret);
                default:
                    return ret;
            }
        } catch (Exception e) {
            log.warn("Error getting type [{}] from [{}]. Returning null", amtType, resultXml);
            return null;
        }
    }

    private static List<InventoryItem> getClonedList(List<InventoryItem> toClone) {
        List<InventoryItem> items = new ArrayList<>();
        for (InventoryItem itemFromCache : toClone) {
            ExtendedInventoryItem clonedItem = new ExtendedInventoryItem();
            clonedItem.setDescription(itemFromCache.getDescription());
            clonedItem.setItemNumber(itemFromCache.getItemNumber());
            clonedItem.setPriceInCentsExcl(itemFromCache.getPriceInCentsExcl());
            clonedItem.setPriceInCentsIncl(itemFromCache.getPriceInCentsIncl());
            clonedItem.setSerialNumber(itemFromCache.getSerialNumber());
            clonedItem.setStockLevel(itemFromCache.getStockLevel());
            clonedItem.setWarehouseId(itemFromCache.getWarehouseId());
            clonedItem.setCurrency(itemFromCache.getCurrency());
            clonedItem.setBoxSize(itemFromCache.getBoxSize());
            if (itemFromCache instanceof ExtendedInventoryItem) {
                clonedItem.setIsKittedItem(((ExtendedInventoryItem) itemFromCache).isIsKittedItem());
            }
            items.add(clonedItem);
        }
        return items;
    }

    private static boolean doesCashInExistInX3(LazyX3Connection lazyConn, String key, Date cashInTxDate) throws Exception {
        log.debug("Checking if X3 has a GL with key = [{}]", key);
        PreparedStatement ps = null;
        boolean exists = false;
        try {
            ps = lazyConn.prepareStatement(BaseUtils.getProperty("env.pos.x3.cashin.exists.query", "select count(*) FROM SC001.GACCENTRYD WHERE ACCDAT_0 = ? AND TYP_0='CASUP' and LEDTYP_0=1 AND FREREF_0 =?"));
            ps.setString(1, Utils.getDateAsString(cashInTxDate, "yyyy/MM/dd", null));
            ps.setString(2, key);
            log.debug("About to run query to check for existing cashin");
            ResultSet rs = ps.executeQuery();
            rs.next();
            exists = (rs.getInt(1) > 0);
            log.debug("Finished running query");
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ex) {
            }
        }
        return exists;
    }

    private static String checkGLExistInX3(LazyX3Connection lazyConn, String glType, String key, String siteCode) throws Exception {
        log.debug("Checking if X3 has a GL of type:[{}], key:[{}] and site code [{}]", new Object[]{glType, key, siteCode});
        PreparedStatement ps = null;
        String glDocNumber = null;

        try {
            ps = lazyConn.prepareStatement(BaseUtils.getProperty("env.pos.x3.gl.exists.query", "SELECT H.NUM_0 FROM SC001.GACCENTRY H WHERE H.TYP_0=? AND BPRVCR_0 = ? AND FCY_0=?"));
            ps.setString(1, glType);
            ps.setString(2, key);
            ps.setString(3, siteCode);
            log.debug("About to run query to check for existing GL");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                glDocNumber = rs.getString(1);
                log.debug("GL exists in X3 for GL type:[{}], key:[{}] and site code [{}], and X3 Document Number is:[{}]", new Object[]{glType, key, siteCode, glDocNumber});
            } else {
                log.debug("No GL exists for GL type:[{}], key:[{}] and site code [{}] in X3", new Object[]{glType, key, siteCode});

            }
            log.debug("Finished running query");
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ex) {
            }
        }
        return glDocNumber;
    }

    private static String getX3CreditNoteNumber(LazyX3Connection lazyConn, String sepId, int lineItem) throws Exception {
        log.debug("Checking if Credit Note for sale_id [{}] and line item number [{}] exists in X3?", sepId, lineItem);
        PreparedStatement ps = null;
        String x3CreditNoteNumber = null;
        try {
            ps = lazyConn.prepareStatement(BaseUtils.getProperty("env.pos.x3.credit_note.exists.query", "SELECT D.CRNNUM_0[CRN_NR] FROM SP3.ZSEPD D WHERE D.CRNNUM_0!=''  AND D.SEPID_0=? AND D.SEPLIN_0=(? * 1000)"));
            ps.setString(1, sepId);
            ps.setInt(2, lineItem);
            log.debug("About to run query to check for existing credit note");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                x3CreditNoteNumber = rs.getString(1);
                log.debug("Credit Note exists in X3 for sale_id [{}] and line item number [{}] X3 Credit Note Number [{}]?", new Object[]{sepId, lineItem, x3CreditNoteNumber});
            } else {
                log.debug("No Credit Note exists in X3 for sale_id [{}] and line item number [{}]", sepId, lineItem);
            }
            log.debug("Finished running query");
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ex) {
            }
        }
        return x3CreditNoteNumber;
    }

    /* Get X3 Payment Number - to be used when making a cash sale reversal and when a credit note already exists in X3 - 
     we need to retrieve the payment associated with the cash sale so we can cancel and delete the payment.
     */
    private static String getX3PaymentNumber(LazyX3Connection lazyConn, String sepId, int lineItem) throws Exception {
        log.debug("Retrieving Payment Number associated with cash sale [{}] and line item number [{}].", sepId, lineItem);
        PreparedStatement ps = null;
        String x3PaymentNumber = null;
        try {
            ps = lazyConn.prepareStatement(BaseUtils.getProperty("env.pos.x3.get.payment_number.query", "SELECT H.PYHNUM_0 FROM SC001.ZSEPD D LEFT JOIN SC001.ZSEPH H ON D.SEPID_0=H.SEPID_0 WHERE D.CRNNUM_0!=''  AND D.SEPID_0=? AND D.SEPLIN_0=(? * 1000)"));
            ps.setString(1, sepId);
            ps.setInt(2, lineItem);
            log.debug("About to run query to get existing payment number in X3");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                x3PaymentNumber = rs.getString(1);
                log.debug("Payment Number exists in X3 for sale_id [{}] and line item number [{}] X3 - Payment Number [{}]?", new Object[]{sepId, lineItem, x3PaymentNumber});
            } else {
                log.debug("No Payment Number exists in X3 for sale_id [{}] and line item number [{}]", sepId, lineItem);
            }
            log.debug("Finished running query");
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ex) {
            }
        }
        return x3PaymentNumber;
    }

    /*Check if Payment Exists? */
    private static boolean checkIfPaymentExists(LazyX3Connection lazyConn, String paymentNumber) throws Exception {
        log.debug("Checking if Payment exists in X3? [{}].", paymentNumber);
        PreparedStatement ps = null;
        String x3PaymentNumber = null;
        boolean exists = false;
        try {
            ps = lazyConn.prepareStatement(BaseUtils.getProperty("env.pos.x3.payment.exists.query", "SELECT * FROM SC001.PAYMENTH WHERE NUM_0=?"));
            ps.setString(1, paymentNumber);
            log.debug("About to run query to check if payment exists in X3");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                log.debug("Payment Number exists in X3 [{}]", paymentNumber);
                exists = true;
            } else {
                log.debug("No Payment exists in X3 with Payment Number [{}]", paymentNumber);
                exists = false;
            }
            log.debug("Finished running query");
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ex) {
            }
        }
        return exists;
    }

    // Check if sale exists in X3 and if it has documents generated, return the x3_RECORD_ID.
    public static String checkIfSaleExistandDocumentsGeneratedInX3(LazyX3Connection lazyConn, int saleId) throws Exception {

        String sepId = props.getProperty("CountrySiteCode") + saleId;

        log.debug("Checking if sale id [{}] is in X3 and its documents already generated?", sepId);
        PreparedStatement ps = null;
        String x3DocumentNumber = null;

        try {
            ps = lazyConn.prepareStatement(BaseUtils.getProperty("env.pos.x3.sale.exists.query", "select SEPID_0, SIHNUM_0 as INVOICENO, PYHNUM_0 as PAYNO from SC001.ZSEPH where SEPID_0=? and DATEDIFF(day,ORDDAT_0,getdate()) < ?;"));
            ps.setString(1, sepId);
            ps.setInt(2, BaseUtils.getIntProperty("env.pos.x3.daemon.lookback.days"));
            log.debug("About to run query to check for existing sale record in X3's intergration table");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                x3DocumentNumber = rs.getString(2);

                if (x3DocumentNumber == null || x3DocumentNumber.trim().length() <= 0) {
                    x3DocumentNumber = null;
                }

                log.debug("Invoice exists in X3 for sale:[{}], and X3 Document Number is:[{}]", new Object[]{sepId, x3DocumentNumber});
            } else {
                log.debug("Invoice exists for sale:[{}] in X3", new Object[]{sepId});
            }
            log.debug("Finished running query");
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ex) {
            }
        }
        return x3DocumentNumber;
    }

    /**
     * ****************************************************************************************************
     *
     * FUNCTIONS BELOW HERE NEED TO BE MADE FAILSAFE SO THAT THEY RETURN VALID
     * DATA EVEN IF THERE IS NO CONNECTIVITY TO X3
     *
     *
     ******************************************************************************************************
     */
    public static boolean isCreditAccountAbleToTakeDebit(EntityManager em, String creditAccountNumber, double saleTotalCentsIncl) throws Exception {

        log.debug("Checking if credit account number [{}] is allowed credit of [{}]cents", creditAccountNumber, saleTotalCentsIncl);

        if (BaseUtils.getBooleanProperty("env.pos.x3.skip.credit.checks", false) || BaseUtils.getBooleanProperty("env.pos.x3.offline", false)) {
            log.warn("X3 is offline. Allowing credit account [{}] to take debit [{}]", creditAccountNumber, saleTotalCentsIncl);
            return true;
        }

        CAdxResultXml result;

        X3DataStream ds = new X3DataStream();
        ds.addField("LBPCNUM", creditAccountNumber);
        ds.addField("LCPY", BaseUtils.getProperty("env.currency.official.symbol"));
        result = run(BaseUtils.getProperty("env.pos.x3.credit.limit.webservice", "ZCUSBAL"), ds.getXML(), true);
        if (result.getResultXml() == null || result.getStatus() == RESULT_CODE_SYSTEM_ERROR) {
            throw new Exception("Error calling X3 to get credit limit. Result: " + result.getResultXml() + ". StatusCode: " + result.getStatusCode());
        }
        Double x3Amnt = getX3ReturnedAmount(result.getResultXml(), X3Helper.AMOUNT_TYPE_CREDIT_LIMIT);
        if (x3Amnt == null) {
            throw new Exception("Error calling X3 to get credit limit. ResultXML was invalid: " + result.getResultXml());
        }
        double limitCents = 100 * x3Amnt;
        double availableBalanceInCents = Double.MAX_VALUE;
        if (limitCents > 0) {
            log.debug("Account has a limit");
            availableBalanceInCents = 100 * getX3ReturnedAmount(result.getResultXml(), X3Helper.AMOUNT_TYPE_AVAILABLE_BALANCE);
        } else if (limitCents == 0) {
            log.debug("Account has no limit");
        } else {
            throw new Exception("Credit Account not found in X3 -- " + creditAccountNumber);
        }

        return (saleTotalCentsIncl <= availableBalanceInCents);
    }

    /**
     * INVENTORY MANAGEMENT HELPERS
     */
    static List<String> getDimensions(LazyX3Connection lazyConn, String channel, String saleLocation) throws Exception {
        log.debug("Getting X3 dimensions for channel [{}] and sales state [{}]", channel, saleLocation);

        if (lazyConn.isConnectionDown()) {
            log.warn("X3 DB Connection is down. Going to get dimensions offline");
            return getDimensionsOffline(channel, saleLocation);
        }

        PreparedStatement ps = null;
        List<String> translatedDimensions = new ArrayList<>();
        try {
            ps = lazyConn.prepareStatement(BaseUtils.getProperty("env.pos.x3.dimension.query"));
            ps.setString(1, channel);
            ps.setString(2, saleLocation);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                translatedDimensions.add(rs.getString(1));
                log.debug("Channel is [{}]", translatedDimensions.get(0));
            }
            if (rs.next()) {
                translatedDimensions.add(rs.getString(1));
                log.debug("Sale state dimension code is [{}]", translatedDimensions.get(1));
            }
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ex) {
            }
        }
        return translatedDimensions;
    }

    static InventoryItem getInventoryItemFromX3ItemMaster(LazyX3Connection lazyConn, String itemNumber, String currency) throws Exception {

        PreparedStatement ps = null;
        InventoryItem inventoryItem = null;

        //if (lazyConn.isConnectionDown()) { ---- TODO
        //    log.warn("X3 DB Connection is down. Going to get sub items offline");
        //    return getSubItemsOffline(itemNumber, warehouseId, currency);
        //}
        try {
            //Get KIT price from pricelist
            ps = lazyConn.prepareStatement(BaseUtils.getProperty("env.pos.x3.itemmaster.query"));
            ps.setString(1, itemNumber); // KIT Item number
            ps.setString(2, currency);
            ResultSet rs = ps.executeQuery();
            double taxRate = BaseUtils.getDoubleProperty("env.sales.tax.percent");

            while (rs.next()) {
                inventoryItem = new InventoryItem();
                inventoryItem.setItemNumber(rs.getString(1).trim());
                inventoryItem.setDescription(rs.getString(2).trim());
                inventoryItem.setPriceInCentsExcl(rs.getDouble(3) * 100);
                inventoryItem.setPriceInCentsIncl(inventoryItem.getPriceInCentsExcl() * (1 + taxRate / 100.0d));
                inventoryItem.setCurrency(rs.getString(4).trim());
                break; //Just take the first one if many returned;
            }

        } catch (Exception ex) {
            log.error("Error while tryting to retrieve item [{}] from X3 item master [{}].", itemNumber, ex.toString());
            throw ex;
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ex) {
            }
        }

        return inventoryItem;
    }

    static InventoryItem getUnitCreditInventoryItemPrice(InventoryItem item) throws Exception {
        log.debug("In getUnitCreditInventoryItemPrice for bundle item [{}] as per IFRS rules.", item.getItemNumber());
        if (POSManager.isUnitCredit(item)) {
            //Check if it is a child or parent
            UnitCreditSpecificationList ucsList = new UnitCreditSpecificationList();
            UnitCreditSpecificationQuery q = new UnitCreditSpecificationQuery();
            q.setItemNumber(item.getItemNumber());
            q.setVerbosity(StUnitCreditSpecificationLookupVerbosity.MAIN);

            try {
                ucsList = SCAWrapper.getAdminInstance().getUnitCreditSpecifications(q);
            } catch (Exception ex) {
                log.error("Error while trying to retrieve unit credit specification for item number - [{}] - [{}].", item.getItemNumber(), ex.getMessage());
                throw new Exception("Error while trying to retrieve unit credit specification for item number -- " + item.getItemNumber() + ", error " + ex.getMessage());
            }

            if (ucsList.getNumberOfUnitCreditSpecifications() <= 0) {
                log.error("No unit credit specification exists for item number  [{}].", item.getItemNumber());
            } else {
                UnitCreditSpecification ucs = ucsList.getUnitCreditSpecifications().get(0);
                //Check if it is a parent or a child.
                String equivalentParentSpecId = Utils.getValueFromCRDelimitedAVPString(ucsList.getUnitCreditSpecifications().get(0).getConfiguration(), "EquivalentParentSpecId");
                boolean isParent = ucsList.getUnitCreditSpecifications().get(0).getConfiguration().contains("IsABaseForPricing=true");

                //Confirm this is a valid parent
                verifyBundleIsALegitimateParent(ucsList.getUnitCreditSpecifications().get(0).getItemNumber(), isParent);

                if (equivalentParentSpecId == null && !isParent) {
                    throw new Exception("Every bundle must either be a base or inherit a price from an equivalent base -- unit credit specification id " + ucs.getUnitCreditSpecificationId());
                }

                if (equivalentParentSpecId != null && isParent) {
                    throw new Exception("A bundle cannot be a base and also inherit from another base -- unit credit specification id " + ucs.getUnitCreditSpecificationId());
                }

                if (isParent) {
                    //  Use the price derived from x3 price list.
                    return item;
                }

                if (equivalentParentSpecId != null) { // This is a child bundle
                    int iParentSpecId = 0;
                    try {
                        iParentSpecId = Integer.valueOf(equivalentParentSpecId);
                    } catch (NumberFormatException ex) {
                        throw new Exception("Unit credit configured with an invalid equivalent base -- EquivalentParentSpecId = " + equivalentParentSpecId);
                    }

                    UnitCreditSpecificationList parentUcsList = new UnitCreditSpecificationList();
                    UnitCreditSpecificationQuery qParent = new UnitCreditSpecificationQuery();
                    qParent.setUnitCreditSpecificationId(iParentSpecId);
                    qParent.setVerbosity(StUnitCreditSpecificationLookupVerbosity.MAIN);

                    try {
                        parentUcsList = SCAWrapper.getAdminInstance().getUnitCreditSpecifications(qParent);
                    } catch (Exception ex) {
                        log.error("Error while trying to retrieve unit credit specification [{}] equivalnce for item number - [{}] - [{}].",
                                new Object[]{iParentSpecId, item.getItemNumber(), ex.getMessage()});
                        throw new Exception("Error while trying to retrieve unit credit specification -- spec id " + iParentSpecId + ", error " + ex.getMessage());
                    }

                    if (parentUcsList.getNumberOfUnitCreditSpecifications() <= 0) {
                        log.error("No unit credit specification exists for item number  [{}].", item.getItemNumber());
                        throw new Exception("Invalid equivalent base spec id configured -- item number " + item.getItemNumber()
                                + ",  equivalent spec id " + iParentSpecId);
                    } else {
                        UnitCreditSpecification equivalentParent = parentUcsList.getUnitCreditSpecifications().get(0);

                        boolean isEquivalentARealParent = equivalentParent.getConfiguration().contains("IsABaseForPricing=true");

                        if (!isEquivalentARealParent) {
                            throw new Exception("Unit credit configured with an equivalent base which is not marked as a base and therefore cannot be used for pricing -- item number " + item.getItemNumber()
                                    + ",  equivalent base spec id " + equivalentParent.getUnitCreditSpecificationId());
                        }

                        verifyBundleIsALegitimateParent(equivalentParent.getItemNumber(), isEquivalentARealParent);

                        if (equivalentParent.getPriceInCents() <= 0) {
                            throw new Exception("Unit credit configured with an equivalent base that does not have a valid price -- item number " + item.getItemNumber()
                                    + ",  equivalent base spec id " + iParentSpecId);
                        }

                        //TODO -- prevent bunk's to be used as base for pricing.
                        //  BUNK BUNST BuNP ... must never be base for pricing
                        // they must always inherit.
                        // Check if they are really equivalent
                        if (ucs.getValidityDays() != equivalentParent.getValidityDays()) {
                            throw new Exception("Unit credit [" + ucs.getItemNumber() + " is not equivalent to base [" + equivalentParent.getItemNumber() + "], validity days are not equal.");
                        }

                        if (!ucs.getFilterClass().equals(equivalentParent.getFilterClass())) {
                            throw new Exception("Unit credit [" + ucs.getItemNumber() + " is not equivalent to base [" + equivalentParent.getItemNumber() + "], filter classes are different.");
                        }

                        if (!ucs.getWrapperClass().equals(equivalentParent.getWrapperClass())) {
                            throw new Exception("Unit credit [" + ucs.getItemNumber() + " is not equivalent to base [" + equivalentParent.getItemNumber() + "], wrapper classes are different.");
                        }

                        if (ucs.getUnits() != equivalentParent.getUnits()) {
                            throw new Exception("Unit credit [" + ucs.getItemNumber() + " is not equivalent to base [" + equivalentParent.getItemNumber() + "], units are different.");
                        }

                        if (ucs.getUsableDays() != equivalentParent.getUsableDays()) {
                            throw new Exception("Unit credit [" + ucs.getItemNumber() + " is not equivalent to base [" + equivalentParent.getItemNumber() + "], units are different.");
                        }

                        double taxRate = BaseUtils.getDoubleProperty("env.sales.tax.percent");
                        item.setPriceInCentsExcl(equivalentParent.getPriceInCents() / (1 + (taxRate / 100.0d)));
                        item.setPriceInCentsIncl(equivalentParent.getPriceInCents());

                    }
                }
            }
        }
        return item;
    }

    static void verifyBundleIsALegitimateParent(String itemNumber, boolean isParent) throws Exception {
        if (isParent) {
            // Rule 1: A agreed with Caroline for ifrs -- BUNK, BUNP, BUNSP,BUNST cannot be configured as parent
            if (itemNumber.startsWith("BUNK")
                    || itemNumber.startsWith("BUNP")
                    || itemNumber.startsWith("BUNSP")
                    || itemNumber.startsWith("BUNST")) {
                throw new Exception("Bundles of type (BUNK, BUNP, BUNSP or BUNST) are not allowed to be configured as base for pricing -- " + itemNumber);
            }
        }
    }

    static List<InventoryItem> getSubItems(LazyX3Connection lazyConn, String itemNumber, String warehouseId, String currency) throws Exception {
        log.debug("Getting X3 sub items for item number [{}]", itemNumber);
        if (lazyConn.isConnectionDown()) {
            log.warn("X3 DB Connection is down. Going to get sub items offline");
            return getSubItemsOffline(itemNumber, warehouseId, currency);
        }

        PreparedStatement ps = null;
        PreparedStatement ps0 = null;

        String cacheKey = itemNumber + "_" + currency; // sub items are not warehouse specific so no need to cache per warehouse
        List<InventoryItem> subItems = CacheHelper.getFromLocalCache(cacheKey, ArrayList.class);
        if (subItems != null) {
            log.debug("Found a result from the local cache. Going to clone it and pass back, We clone cause the caller may modify the item and we dont want that in our cache");
            return getClonedList(subItems);
        } else {
            subItems = new ArrayList<>();
        }

        // Not found in cache. Get from X3
        // List<InventoryItem> subItems = new ArrayList<>();
        double kitStandalonePriceInCentsExcl = 0.00;

        try {
            if (BaseUtils.getBooleanProperty("env.pos.ifrs15.enabled", false)) { // This is only required for IFSR15
                //Get KIT price from itemmaster (pricelist)
                InventoryItem kitInventoryItem = getInventoryItemFromX3ItemMaster(lazyConn, itemNumber, currency);

                if (kitInventoryItem == null) { // Kit not on price list ...
                    log.error("Did not find KIT [{}] in the pricelist", itemNumber);
                    throw new Exception("KIT not found on pricelist -- kit [" + itemNumber + "] sub-warehouse [" + warehouseId + "]");
                }
                kitStandalonePriceInCentsExcl = kitInventoryItem.getPriceInCentsExcl();
                ps = lazyConn.prepareStatement(BaseUtils.getProperty("env.pos.x3.subitem.query.ifrs15"));

            } else { // Use Legacy
                ps = lazyConn.prepareStatement(BaseUtils.getProperty("env.pos.x3.subitem.query"));
            }

            ps.setString(1, itemNumber); // KIT Item number
            ps.setString(2, currency);

            ResultSet rs = ps.executeQuery();
            double taxRate = BaseUtils.getDoubleProperty("env.sales.tax.percent");
            double totalStandalonePriceForSubItemsInCents = 0.00;

            List<InventoryItem> allInventory = new ArrayList<>();

            InventorySystem inventorySystem = InventorySystemManager.getInventorySystem();

            String curItemNumber;
            while (rs.next()) {
                curItemNumber = rs.getString(1).trim();
                ExtendedInventoryItem i = new ExtendedInventoryItem();
                i.setItemNumber(curItemNumber);
                i.setDescription(rs.getString(2).trim());
                if (i.getItemNumber().startsWith("BUN")) {
                    // Dont ask for the serial number of a bundle
                    i.setSerialNumber("BUNDLE");
                } else {
                    i.setSerialNumber("");
                }
                i.setWarehouseId("");

                if (rs.getString(3) == null) { // Not on price list, possibly item on the BOM and not on price list
                    i.setPriceInCentsExcl(-1);
                    i.setPriceInCentsIncl(-1);
                } else {
                    if (BaseUtils.getBooleanProperty("env.pos.ifrs15.enabled", false)
                            && POSManager.isUnitCredit(i)
                            && !(i.getItemNumber().startsWith("BUNPB") || i.getItemNumber().startsWith("BUNKPB"))) {
                        i = (ExtendedInventoryItem) X3Helper.getUnitCreditInventoryItemPrice(i);
                    } else {
                        i.setPriceInCentsExcl(rs.getDouble(3) * 100);
                        i.setPriceInCentsIncl(i.getPriceInCentsExcl() * (1 + taxRate / 100.0d));
                    }
                }

                i.setStockLevel(1);
                i.setCurrency(rs.getString(4));
                subItems.add(i);
                //Accumutate the total just in case IFRS 15 is enabled.
                totalStandalonePriceForSubItemsInCents += i.getPriceInCentsExcl();
                log.debug("Got X3 sub item with item number: [{}] serial number: [{}] and price [{}]cents Incl", new Object[]{i.getItemNumber(), i.getSerialNumber(), i.getPriceInCentsIncl()});
            }
            // Iterate again through the list and set pro-rata prices as per IFSR 15
            if (BaseUtils.getBooleanProperty("env.pos.ifrs15.enabled", false)) {
                //Apply some rules
                if (totalStandalonePriceForSubItemsInCents <= 0) { //Total of KIT sub items cannot be <= Zero 
                    // Return empty list of subitems
                    log.debug("The total standalone price for subitems of kit [{}] is zero, kit will be excluded from the available inventory list.", itemNumber);
                    throw new Exception("The total of standalone prices for subitems of kit is zero -- kit[" + itemNumber + "].");
                    // return new ArrayList<>();
                }

                for (InventoryItem subItem : subItems) {

                    if ((subItem.getPriceInCentsExcl() == -1) || (subItem.getPriceInCentsExcl() <= 0 && !(subItem.getItemNumber().startsWith("BUNPB") || subItem.getItemNumber().startsWith("BUNKPB")))) {
                        // Remove all subItems of this kit from the list;
                        if (subItem.getPriceInCentsExcl() == -1) {
                            log.debug("KIT [{}] is being removed from the x3_offline_subitems because its subitem [{}] is not on the pricelist, price [{}], currency [{}].",
                                    new Object[]{itemNumber, subItem.getItemNumber(), subItem.getPriceInCentsExcl(), subItem.getCurrency()});
                            throw new Exception("Kit subitem not on price list -- subitem " + subItem.getItemNumber() + ", kit.");
                        } else {
                            log.debug("KIT [{}] is being removed from the x3_offline_subitems because one of its non-BUNPB/BUNKPB subitem [{}] does not have a price [{}], currency [{}].",
                                    new Object[]{itemNumber, subItem.getItemNumber(), subItem.getPriceInCentsExcl(), subItem.getCurrency()});
                            throw new Exception("Invalid subitem configuration -- kit " + itemNumber + " subitem " + subItem.getItemNumber() + " does not have a price.");
                        }

                        // return new ArrayList<>(); //Empty list of subitems 
                    }
                    subItem.setPriceInCentsExcl(kitStandalonePriceInCentsExcl / totalStandalonePriceForSubItemsInCents * subItem.getPriceInCentsExcl()); // Set prorata price
                    subItem.setPriceInCentsIncl(subItem.getPriceInCentsExcl() * (1 + taxRate / 100.0d));
                }
            }
            log.debug("X3 kitted item [{}] had [{}] sub items.", itemNumber, subItems.size());
            CacheHelper.putInLocalCache(cacheKey, subItems, 600);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ex) {
            }

            try {
                if (ps0 != null) {
                    ps0.close();
                }
            } catch (Exception ex) {
            }

        }

        return getClonedList(subItems);
    }

    public static void populateInventory(LazyX3Connection lazyConn, String warehouseId, String stringMatch, List<InventoryItem> itemsInWarehouse, String currency) throws Exception {

        log.debug("Getting X3 inventory for warehouse Id [{}] and string match [{}] and currency [{}]", new Object[]{warehouseId, stringMatch, currency});

        String cacheKey = warehouseId + "_" + stringMatch + "_" + currency;
        List<InventoryItem> items = CacheHelper.getFromLocalCache(cacheKey, ArrayList.class);
        if (items != null) {
            log.debug("Found a result from the local cache");
            itemsInWarehouse.addAll(getClonedList(items));
            return;
        }

        // List<InventoryItem> items ;
        // Stringmatch will be in uppercase
        if (stringMatch.startsWith("BUN") || stringMatch.startsWith("AIR")) {
            log.debug("Looking up a unit credit or airtime. Take a shortcut and look in offline inventory instead of X3 itself");
            populateInventoryOffline(warehouseId, stringMatch, itemsInWarehouse, currency);
            if (!itemsInWarehouse.isEmpty()) {
                CacheHelper.putInLocalCache(cacheKey, getClonedList(itemsInWarehouse), 600);
                return;
            } else {
                log.debug("Didnt find anything in offline DB. Looking in X3");
            }
        }

        if (lazyConn.isConnectionDown()) {
            log.warn("X3 DB Connection is down. Going to populate inventory offline");
            populateInventoryOffline(warehouseId, stringMatch, itemsInWarehouse, currency);
            return;
        }

        PreparedStatement ps = null;
        items = new ArrayList<>();

        try {
            ps = lazyConn.prepareStatement(BaseUtils.getProperty("env.pos.x3.inventory.query"));
            String match = "%" + stringMatch + "%";
            ps.setString(1, warehouseId);
            ps.setString(2, match);
            ps.setString(3, match);
            ps.setString(4, match);
            ps.setString(5, currency);
            log.warn("About to run query to get Inventory: [{}]", ps.toString());
            long start = System.currentTimeMillis();
            ResultSet rs = ps.executeQuery();
            log.debug("Finished running query to get Inventory. Query took [{}]ms", System.currentTimeMillis() - start);
            double taxRate = BaseUtils.getDoubleProperty("env.sales.tax.percent");
            while (rs.next()) {
                ExtendedInventoryItem i = new ExtendedInventoryItem();
                i.setSerialNumber(rs.getString(1).trim());
                i.setDescription(rs.getString(2).trim());
                i.setItemNumber(rs.getString(3).trim());
                i.setWarehouseId(rs.getString(4).trim());
                i.setCurrency(rs.getString(6).trim());
                i.setBoxSize(rs.getInt(9));

                log.debug("X3 Sent price of [{}]major currency units excl tax", rs.getDouble(5));
                if (POSManager.isInterconnectMinorCurrency(i.getItemNumber())) {
                    i.setPriceInCentsExcl(1 / (1 + (taxRate / 100.0d))); // Set interconnect items price to 1 cent.
                } else if (POSManager.isInterconnectMajorCurrency(i.getItemNumber())) {
                    i.setPriceInCentsExcl(100 / (1 + (taxRate / 100.0d))); // Set interconnect items price to 1 dollar.
                } else if (POSManager.isAirtime(i.getSerialNumber())) { // || POSManager.isInterconnect(i.getItemNumber())) { // Set the price to 1/1.8 due to X3 rounding problem (X3 only returns a maximum of four decimal places)
                    i.setPriceInCentsExcl(100 / (1 + (taxRate / 100.0d)));
                } else if (POSManager.isDedicatedInternetBundle(i.getItemNumber())) { // || POSManager.isDedicatedInternetBundle(i.getItemNumber())) { // Set the price to 1/1.8 due to X3 rounding problem (X3 only returns a maximum of four decimal places)
                    i.setPriceInCentsExcl(100 / (1 + (taxRate / 100.0d)));
                } else if (isP2PItem(i.getItemNumber())) { // Recalculate P2P items price due to X3 rounding problem (X3 is limited to 4 decimal places)
                    i.setPriceInCentsExcl(100 / (1 + (taxRate / 100.0d)));
                } else if (POSManager.isUnitCredit(i)) {

                    if (BaseUtils.getBooleanProperty("env.pos.ifrs15.enabled", false)) { // For IFSR price must always come from the unit credit specification and not the price list
                        // Verify the bundle pricing hierachy is configured correctly.
                        //Check if it is a child or parent
                        UnitCreditSpecificationList ucsList = new UnitCreditSpecificationList();
                        UnitCreditSpecificationQuery q = new UnitCreditSpecificationQuery();
                        q.setItemNumber(i.getItemNumber());
                        q.setVerbosity(StUnitCreditSpecificationLookupVerbosity.MAIN);

                        try {
                            ucsList = SCAWrapper.getAdminInstance().getUnitCreditSpecifications(q);
                        } catch (Exception ex) {
                            log.error("Error while trying to retrieve unit credit specification for item number - [{}] - [{}].", i.getItemNumber(), ex.getMessage());
                        }

                        if (ucsList.getNumberOfUnitCreditSpecifications() > 0) {

                            UnitCreditSpecification ucs = ucsList.getUnitCreditSpecifications().get(0);
                            i.setPriceInCentsExcl(ucs.getPriceInCents() / (1 + (taxRate / 100.0d)));
                            i.setPriceInCentsIncl(ucs.getPriceInCents());

                        } else {
                            log.error("No unit credit specification exists for item number  [{}].", i.getItemNumber());
                            // What price to show when there  is no spec defined for this bundle?
                            // Exclude it from the list. Use the classical way for now ... bundles will not drop in SEP anyway since there is no spec for it.
                            if (rs.getDouble(5) < 1.0d && rs.getDouble(5) > 0) { // If we know this is a price for 1 currency unit and rounded, then lets recalculate it ourselves
                                i.setPriceInCentsExcl(100 / (1 + (taxRate / 100.0d)));
                            } else {
                                // Round bundles
                                i.setPriceInCentsExcl(Utils.round(rs.getDouble(5) * 100 * (1 + (taxRate / 100.0d)), 0) / (1 + (taxRate / 100.0d)));
                                log.debug("Rounded [{}]c to [{}]c", rs.getDouble(5) * 100, i.getPriceInCentsExcl());
                            }
                        }
                    } else { //USe the classical way         
                        if (rs.getDouble(5) < 1.0d && rs.getDouble(5) > 0) { // If we know this is a price for 1 currency unit and rounded, then lets recalculate it ourselves
                            i.setPriceInCentsExcl(100 / (1 + (taxRate / 100.0d)));
                        } else {
                            // Round bundles
                            i.setPriceInCentsExcl(Utils.round(rs.getDouble(5) * 100 * (1 + (taxRate / 100.0d)), 0) / (1 + (taxRate / 100.0d)));
                            log.debug("Rounded [{}]c to [{}]c", rs.getDouble(5) * 100, i.getPriceInCentsExcl());
                        }
                    }
                } else if (i.getItemNumber().startsWith("KIT")) {

                    // As per finance - no KIT pro-rata busines rules will be checked when selecting KITS to be sold on the make sale screen in SEP. Business  rules will be ccheck 
                    // when submit for pricing is done.
                    if (BaseUtils.getBooleanProperty("env.pos.ifrs15.enabled", false)) {
                        // We will just use the KIT price from X3 as is,  pro-rata will be calculated on submit for pricing.
                        i.setPriceInCentsExcl(rs.getDouble(5) * 100);
                        i.setIsKittedItem(true);
                    } else {
                        log.debug("Item starts with KIT. This could be a kitted item. Will try and calculate the price from the sub items");
                        double priceCentsExcl = 0;
                        List<InventoryItem> subItems = getSubItems(lazyConn, i.getItemNumber(), warehouseId, currency);
                        if (!subItems.isEmpty()) {
                            for (InventoryItem subItem : subItems) {
                                priceCentsExcl += subItem.getPriceInCentsExcl();
                            }
                            i.setIsKittedItem(true);
                        } else {
                            priceCentsExcl = rs.getDouble(5) * 100;
                        }
                        i.setPriceInCentsExcl(priceCentsExcl);
                    }
                } else {
                    i.setPriceInCentsExcl(rs.getDouble(5) * 100);
                }

                List<String> lstVatExemptItems = BaseUtils.getPropertyAsList("env.pos.vat.exempt.items");

                if (lstVatExemptItems != null && lstVatExemptItems.contains(i.getItemNumber())) {
                    i.setPriceInCentsIncl(i.getPriceInCentsExcl());
                } else {
                    i.setPriceInCentsIncl(i.getPriceInCentsExcl() * (1 + taxRate / 100.0d));
                }
                i.setStockLevel(1);
                items.add(i);
                log.debug("Got X3 item with serial number: [{}] item number: [{}] and price [{}]cents Incl", new Object[]{i.getSerialNumber(), i.getItemNumber(), i.getPriceInCentsIncl()});
            }
            itemsInWarehouse.addAll(getClonedList(items));
            CacheHelper.putInLocalCache(cacheKey, items, 600);
            log.debug("X3 had [{}] items", items.size());
        } catch (Exception e) {
            log.warn("Error calling MSSQL to get Inventory from X3: [{}]. Wont return any inventory but will continue", e.toString());
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "POS-X3", "Error getting X3 Inventory: " + e.toString());
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ex) {
            }
        }
    }

    public static List<InventoryItem> getUpSizeSubItems(EntityManager emLocal, UpSizeInventoryQuery upSizeInventoryQuery) throws Exception {
        // List<InventoryItem> items = new ArrayList<>();

        String mainBundleItemNumber = upSizeInventoryQuery.getMainBundleItemNumber();
        String cacheKey = "UpSize_For_" + mainBundleItemNumber;

        List<InventoryItem> items = CacheHelper.getFromLocalCache(cacheKey, ArrayList.class);

        if (items != null) {
            log.debug("Found a result from the local cache");
            return items;
        } else {
            items = new ArrayList<>();
        }

        double taxRate = BaseUtils.getDoubleProperty("env.sales.tax.percent");

        List<InventoryItem> upsizeBundleItems = new ArrayList<>();
        UnitCreditSpecificationList ucsList = new UnitCreditSpecificationList();
        // Look for ExtraUpsizeSpecIds
        UnitCreditSpecificationQuery q = new UnitCreditSpecificationQuery();
        q.setItemNumber(mainBundleItemNumber);
        q.setVerbosity(StUnitCreditSpecificationLookupVerbosity.MAIN);

        try {
            ucsList = SCAWrapper.getAdminInstance().getUnitCreditSpecifications(q);
        } catch (Exception ex) {
            log.error("Error while trying to retrieve unit credit specification for item number - [{}] - [{}].", mainBundleItemNumber, ex.getMessage());
            return upsizeBundleItems; //Empty list
        }

        if (ucsList.getNumberOfUnitCreditSpecifications() <= 0) {
            log.debug("Upsize: Item [{}] is not a unit credit", mainBundleItemNumber);
        } else {
            String strUpSizeSpecs = Utils.getValueFromCRDelimitedAVPString(ucsList.getUnitCreditSpecifications().get(0).getConfiguration(), "ExtraUpsizeSpecIds");
            String strUpSizeItems = Utils.getValueFromCRDelimitedAVPString(ucsList.getUnitCreditSpecifications().get(0).getConfiguration(), "ExtraUpsizeItems");

            if (strUpSizeSpecs == null && strUpSizeItems == null) { //Nothing to add
                return upsizeBundleItems;
            }

            if (strUpSizeSpecs != null) {
                String[] arrayUpSizeSpecs = strUpSizeSpecs.split(",");
                items = new ArrayList<>();

                for (String specId : arrayUpSizeSpecs) {

                    specId = specId.trim();
                    if (specId.isEmpty()) {
                        continue;
                    }

                    int upsizeUCSpecId = Integer.valueOf(specId);
                    if (upsizeUCSpecId <= 0) {
                        continue;
                    }

                    //Retrieve the spec of the upsize sub-item
                    UnitCreditSpecificationQuery qUpzise = new UnitCreditSpecificationQuery();
                    qUpzise.setUnitCreditSpecificationId(upsizeUCSpecId);

                    qUpzise.setVerbosity(StUnitCreditSpecificationLookupVerbosity.MAIN);
                    ucsList = SCAWrapper.getAdminInstance().getUnitCreditSpecifications(qUpzise);

                    if (ucsList.getNumberOfUnitCreditSpecifications() > 0) {
                        InventoryItem subItem = new InventoryItem();
                        subItem.setCurrency("");
                        subItem.setItemNumber(ucsList.getUnitCreditSpecifications().get(0).getItemNumber());
                        subItem.setDescription(ucsList.getUnitCreditSpecifications().get(0).getName());
                        subItem.setPriceInCentsIncl(ucsList.getUnitCreditSpecifications().get(0).getPriceInCents());
                        subItem.setSerialNumber("");
                        subItem.setPriceInCentsExcl(ucsList.getUnitCreditSpecifications().get(0).getPriceInCents() / (1 + (taxRate / 100.0d)));
                        subItem.setWarehouseId("");
                        subItem.setStockLevel(0);

                        items.add(subItem);
                    } else {
                        log.debug("Upsize extra unit credit specification id [{}] is not a unit credit", upsizeUCSpecId);
                    }
                }
                upsizeBundleItems.addAll(getClonedList(items));
                if (upsizeBundleItems.size() > 0) {
                    CacheHelper.putInLocalCache(cacheKey, upsizeBundleItems, 600);
                }
                log.debug("Upsize sub-items unit credit specs for bundle [{}] is of size [{}]", mainBundleItemNumber, upsizeBundleItems.size());
            }

            if (strUpSizeItems != null) {
                String[] arrayUpSizeItemNumbers = strUpSizeItems.split(",");
                items = new ArrayList<>();
                InventorySystem inventorySystem = null;
                try {
                    inventorySystem = InventorySystemManager.getInventorySystem();

                    for (String extraItemNumber : arrayUpSizeItemNumbers) {
                        InventoryItem subItem = inventorySystem.getInventoryItem(emLocal, extraItemNumber,
                                upSizeInventoryQuery.getWarehouseId(), upSizeInventoryQuery.getSalesPersonCustomerId(),
                                upSizeInventoryQuery.getRecipientAccountId(), upSizeInventoryQuery.getCurrency());
                        subItem.setStockLevel(1);
                        items.add(subItem);
                    }
                    upsizeBundleItems.addAll(getClonedList(items));
                } catch (Exception e) {
                    throw e;
                } finally {
                    if (inventorySystem != null) {
                        inventorySystem.close();
                    }
                }
                log.debug("Upsize sub-items for bundle [{}] is of size [{}]", mainBundleItemNumber, upsizeBundleItems.size());
            }
        }

        return upsizeBundleItems;
    }

    public static boolean doesLocationExist(LazyX3Connection lazyConn, String location) throws Exception {
        boolean doesLocationExist = false;
        PreparedStatement ps = null;
        try {
            if (lazyConn.isConnectionDown()) {
                throw new Exception("Cant connect to X3");
            }
            // SELECT LOC_0 FROM SC001.STOLOC WHERE LOC_0=?
            ps = lazyConn.prepareStatement(BaseUtils.getProperty("env.pos.x3.location.query", "SELECT LOC_0 FROM SC001.STOLOC WHERE LOC_0=?"));
            ps.setString(1, location);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                doesLocationExist = true;
            }
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ex) {
            }
        }
        return doesLocationExist;
    }

    static String getInventoryWarehouseId(LazyX3Connection lazyConn, String serialNumber) throws Exception {
        log.debug("Getting the warehouse id of serial number [{}]", serialNumber);
        if (lazyConn.isConnectionDown()) {
            log.warn("X3 DB Connection is null. Cannot continue");
            throw new Exception("X3 is offline");
        }

        PreparedStatement ps = null;
        String warehouseId = null;
        try {
            ps = lazyConn.prepareStatement(BaseUtils.getProperty("env.pos.x3.itemwarehouse.query", "SELECT STO.LOC_0 FROM SC001.STOCK STO WHERE STO.SERNUM_0 = ?"));
            ps.setString(1, serialNumber);
            log.debug("About to run query to get serial number warehouse id");
            long start = System.currentTimeMillis();
            ResultSet rs = ps.executeQuery();
            log.debug("Finished running query to get warehouse id. Query took [{}]ms", System.currentTimeMillis() - start);
            while (rs.next()) {
                warehouseId = rs.getString(1);
                break;
            }
            if (warehouseId == null) {
                throw new Exception("Serial number not found in X3 -- " + serialNumber);
            }
            log.debug("Warehouse Id is [{}]", warehouseId);

        } catch (Exception e) {
            log.warn("Error calling MSSQL to get warehouse id for serial number from X3: [{}]", e.toString());
            throw e;
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ex) {
            }
        }
        return warehouseId;
    }

    public static double getExchangeRate(String localCurrency, String otherCurrency) throws Exception {
        Connection conn = new LazyX3Connection().getConnection(true);
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(BaseUtils.getProperty("env.pos.x3.exchange.rate.query", "select CHGRAT_0 from SC001.TABCHANGE where CHGSTRDAT_0=CAST(GETDATE() AS DATE) and CURDEN_0=? and CUR_0=?"));
            ps.setString(1, otherCurrency);
            ps.setString(2, localCurrency);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getDouble(1);
        } catch (Exception e) {
            log.warn("Error calling MSSQL to get exchange rate from X3: [{}]", e.toString());
            throw e;
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ex) {
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception ex) {
                }
            }
        }
    }

    public static void verifySerialNumber(LazyX3Connection lazyConn, EntityManager em, String serialNumber, String warehouseId, String itemNumber) throws Exception {
        log.debug("Checking that serial number [{}] has X3 inventory for warehouse Id [{}] and item number [{}]", new Object[]{serialNumber, warehouseId, itemNumber});

        if (lazyConn.isConnectionDown()) {
            log.warn("X3 DB Connection is down. Going to verify serial number offline");
            verifySerialNumberOffline(serialNumber, warehouseId, itemNumber);
            return;
        }

        PreparedStatement ps = null;
        int qty = 0;
        try {
            ps = lazyConn.prepareStatement(BaseUtils.getProperty("env.pos.x3.serialnumber.query"));
            ps.setString(1, warehouseId);
            ps.setString(2, serialNumber);
            ps.setString(3, itemNumber);
            log.debug("About to run query to get Inventory quantity for serial number");
            long start = System.currentTimeMillis();
            ResultSet rs = ps.executeQuery();
            log.debug("Finished running query to get Inventory quantity for serial number. Query took [{}]ms", System.currentTimeMillis() - start);
            while (rs.next()) {
                qty += rs.getDouble(3);
                log.debug("Got X3 item qty of [{}]", rs.getDouble(3));
            }
            log.debug("X3 Total qty is [{}]", qty);

        } catch (Exception e) {
            log.warn("Error calling MSSQL to get stock for serial number from X3: [{}]. Will say stock was not available", e.toString());
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "POS-X3", "Error checking X3 Serial Number: " + e.toString());
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ex) {
            }
        }
        if (qty == 0) {
            throw new Exception("No stock for serial number -- Serial " + serialNumber + " in location " + warehouseId);
        }
    }

    static void verifySerialNumbers(LazyX3Connection lazyConn, EntityManager em, List<String[]> serialNumbers, String warehouseId) throws Exception {
        log.debug("Checking that a list of serial numbers has X3 inventory for warehouse Id [{}]", warehouseId);

        if (serialNumbers.isEmpty()) {
            log.debug("No serials in list to verify");
            return;
        }
        if (lazyConn.isConnectionDown()) {
            log.warn("X3 DB Connection is down. Going to verify serial numbers offline");
            for (String[] serialNumberAndItemNumber : serialNumbers) {
                String serialNumber = serialNumberAndItemNumber[0];
                String itemNumber = serialNumberAndItemNumber[1];
                verifySerialNumberOffline(serialNumber, warehouseId, itemNumber);
            }
            return;
        }

        PreparedStatement ps = null;
        Set<String> foundStockList = new HashSet<>();

        try {
            String query = BaseUtils.getProperty("env.pos.x3.serialnumbers.query", "SELECT "
                    + "STO.ITMREF_0 as ITEMNO, "
                    + "STO.SERNUM_0 as SERIAL, "
                    + "STO.QTYSTUACT_0-STO.CUMALLQTY_0 as STOCKLEVEL "
                    + "FROM SC001.STOCK STO "
                    + "WHERE QTYSTU_0-CUMALLQTA_0 > 0 "
                    + "AND STO.STA_0 LIKE 'A%' "
                    + "AND STO.CUNLOKFLG_0<>2 "
                    + "AND STO.LOC_0 =? "
                    + "AND (");
            query = query + getItemFilter(serialNumbers) + ")";
            log.debug("Full inventory query is [{}]", query);
            ps = lazyConn.prepareStatement(query);
            ps.setString(1, warehouseId);
            log.debug("About to run query to get Inventory quantities for [{}] serial numbers", serialNumbers.size());
            long start = System.currentTimeMillis();
            ResultSet rs = ps.executeQuery();
            log.debug("Finished running query to get Inventory quantities for serial numbers. Query took [{}]ms", System.currentTimeMillis() - start);

            while (rs.next()) {
                double itemsQuantity = rs.getDouble(3);
                if (itemsQuantity > 0) {
                    foundStockList.add(rs.getString(1) + "_" + rs.getString(2));
                }
                log.debug("Got X3 item qty of [{}] for [{}]", itemsQuantity, rs.getString(2));
            }

        } catch (Exception e) {
            log.warn("Error calling MSSQL to get stock for serial numbers from X3: [{}]. Will say stock was not available", e.toString());
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "POS-X3", "Error checking X3 Serial Number: " + e.toString());
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ex) {
            }
        }

        StringBuilder notfound = new StringBuilder();
        for (String[] serialNumberAndItemNumber : serialNumbers) {
            String serialNumber = serialNumberAndItemNumber[0];
            String itemNumber = serialNumberAndItemNumber[1];
            if (!foundStockList.contains(itemNumber + "_" + serialNumber)) {
                notfound.append("[Serial:");
                notfound.append(serialNumber);
                notfound.append(" Item:");
                notfound.append(itemNumber);
                notfound.append("]");
            }
        }
        if (foundStockList.size() != serialNumbers.size()) {
            throw new Exception("No stock for serial number -- " + notfound.toString() + " in location " + warehouseId);
        }
    }

    private static String getItemFilter(List<String[]> serialNumbers) {
        StringBuilder sql = new StringBuilder();
        int i = 0;
        for (String[] serialNumberAndItemNumber : serialNumbers) {
            i++;
            String serialNumber = serialNumberAndItemNumber[0];
            String itemNumber = serialNumberAndItemNumber[1];
            sql.append("(STO.SERNUM_0='");
            sql.append(serialNumber);
            sql.append("' AND STO.ITMREF_0='");
            sql.append(itemNumber);
            sql.append("')");
            if (i != serialNumbers.size()) {
                sql.append(" OR ");
            }
        }
        return sql.toString();
    }

    /*
     * 
     * OFFLINE VERSIONS OF X3 QUERIES
     * 
     */
    public static List<String> getDimensionsOffline(String channel, String saleLocation) throws Exception {
        log.debug("Getting offline X3 dimensions for channel [{}] and sales state [{}]", channel, saleLocation);
        PreparedStatement ps = null;
        Connection conn = getConnectionForOffline();
        List<String> translatedDimensions = new ArrayList<>();
        try {
            ps = conn.prepareStatement(BaseUtils.getProperty("env.pos.x3.dimension.query.offline"));
            ps.setString(1, channel);
            ps.setString(2, saleLocation);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                translatedDimensions.add(rs.getString(1));
                log.debug("Offline Channel is [{}]", translatedDimensions.get(0));
            }
            if (rs.next()) {
                translatedDimensions.add(rs.getString(1));
                log.debug("Offline Sale state dimension code is [{}]", translatedDimensions.get(1));
            }
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ex) {
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception ex) {
            }
        }
        return translatedDimensions;
    }

    private static List<InventoryItem> getSubItemsOffline(String itemNumber, String warehouseId, String currency) throws Exception {
        log.debug("Getting Offline X3 sub items for item number [{}]", itemNumber);
        PreparedStatement ps = null;
        // Not found in cache. Get from X3
        List<InventoryItem> subItems = new ArrayList<>();
        Connection conn = getConnectionForOffline();
        try {
            ps = conn.prepareStatement(BaseUtils.getProperty("env.pos.x3.subitem.query.offline"));
            ps.setString(1, itemNumber);
            ps.setString(2, currency);
            ResultSet rs = ps.executeQuery();
            double taxRate = BaseUtils.getDoubleProperty("env.sales.tax.percent");
            while (rs.next()) {
                ExtendedInventoryItem i = new ExtendedInventoryItem();
                i.setItemNumber(rs.getString(1).trim());
                i.setDescription(rs.getString(2).trim());
                if (i.getItemNumber().startsWith("BUN")) {
                    // Dont ask for the serial number of a bundle
                    i.setSerialNumber("BUNDLE");
                } else {
                    i.setSerialNumber("");
                }
                i.setWarehouseId("");
                i.setPriceInCentsExcl(rs.getDouble(3) * 100);
                i.setPriceInCentsIncl(i.getPriceInCentsExcl() * (1 + taxRate / 100.0d));
                i.setStockLevel(1);
                i.setCurrency(rs.getString(4));
                subItems.add(i);
                log.debug("Got Offline X3 sub item with item number: [{}] serial number: [{}] and price [{}]cents Incl", new Object[]{i.getItemNumber(), i.getSerialNumber(), i.getPriceInCentsIncl()});
            }
            log.debug("X3 offline kitted item had [{}] sub items", subItems.size());
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ex) {
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception ex) {
            }
        }
        return subItems;
    }

    private static void populateInventoryOffline(String warehouseId, String stringMatch, List<InventoryItem> itemsInWarehouse, String currency) throws Exception {
        log.debug("Getting offline X3 inventory for warehouse Id [{}] and string match [{}]", warehouseId, stringMatch);

        PreparedStatement ps = null;
        List<InventoryItem> items = new ArrayList<>();
        Connection conn = getConnectionForOffline();

        try {
            ps = conn.prepareStatement(BaseUtils.getProperty("env.pos.x3.inventory.query.offline"));
            String match = "%" + stringMatch + "%";
            ps.setString(1, warehouseId);
            ps.setString(2, match);
            ps.setString(3, match);
            ps.setString(4, match);
            ps.setString(5, currency);
            log.warn("About to run query to get InventoryOffline: [{}]", ps.toString());
            long start = System.currentTimeMillis();
            ResultSet rs = ps.executeQuery();
            log.debug("Finished running query to get Inventory. Query took [{}]ms", System.currentTimeMillis() - start);
            double taxRate = BaseUtils.getDoubleProperty("env.sales.tax.percent");
            while (rs.next()) {
                ExtendedInventoryItem i = new ExtendedInventoryItem();
                i.setSerialNumber(rs.getString(1).trim());
                i.setDescription(rs.getString(2).trim());
                i.setItemNumber(rs.getString(3).trim());
                i.setWarehouseId(rs.getString(4).trim());
                i.setCurrency(rs.getString(6).trim());
                i.setBoxSize(rs.getInt(7));

                log.debug("X3 Sent price of [{}]major currency units excl tax", rs.getDouble(5));
                if (POSManager.isInterconnectMinorCurrency(i.getItemNumber())) {
                    i.setPriceInCentsExcl(1 / (1 + (taxRate / 100.0d))); // Set interconnect items price to 1 cent.
                } else if (POSManager.isInterconnectMajorCurrency(i.getItemNumber())) {
                    i.setPriceInCentsExcl(100 / (1 + (taxRate / 100.0d))); // Set interconnect items price to 1 dollar.
                } else if (POSManager.isAirtime(i.getSerialNumber())) { // || POSManager.isInterconnect(i.getItemNumber())) { // Set the price to 1/1.8 due to X3 rounding problem (X3 only returns a maximum of four decimal places)
                    i.setPriceInCentsExcl(100 / (1 + (taxRate / 100.0d)));
                } else if (POSManager.isUnitCredit(i)) {

                    if (rs.getDouble(5) < 1.0d && rs.getDouble(5) > 0) { // If we know this is a price for 1 currency unit and rounded, then lets recalcualte it ourselves
                        i.setPriceInCentsExcl(100 / (1 + (taxRate / 100.0d)));
                    } else {
                        // Round bundles
                        i.setPriceInCentsExcl(Utils.round(rs.getDouble(5) * 100 * (1 + (taxRate / 100.0d)), 0) / (1 + (taxRate / 100.0d)));
                        log.debug("Rounded [{}]c to [{}]c", rs.getDouble(5) * 100, i.getPriceInCentsExcl());
                    }

                } else if (i.getItemNumber().startsWith("KIT")) {

                    log.debug("Item starts with KIT. This could be a kitted item. Will try and calculate the price from the sub items");
                    double priceCentsExcl = 0;
                    List<InventoryItem> subItems = getSubItemsOffline(i.getItemNumber(), warehouseId, currency);
                    if (!subItems.isEmpty()) {
                        for (InventoryItem subItem : subItems) {
                            priceCentsExcl += subItem.getPriceInCentsExcl();
                        }
                        i.setIsKittedItem(true);
                    } else {
                        priceCentsExcl = rs.getDouble(5) * 100;
                    }
                    i.setPriceInCentsExcl(priceCentsExcl);
                } else {
                    i.setPriceInCentsExcl(rs.getDouble(5) * 100);
                }
                i.setPriceInCentsIncl(i.getPriceInCentsExcl() * (1 + taxRate / 100.0d));
                log.debug("Price incl is [{}]c", i.getPriceInCentsIncl());
                i.setStockLevel(1);
                items.add(i);
                log.debug("Got X3 item with serial number: [{}] item number: [{}] and price [{}]cents Incl", new Object[]{i.getSerialNumber(), i.getItemNumber(), i.getPriceInCentsIncl()});
            }
            itemsInWarehouse.addAll(items);
            log.debug("Offline X3 had [{}] items", items.size());
        } catch (Exception e) {
            log.warn("Error calling MYSQL to get Inventory from X3 Offline: [{}]. Wont return any inventory but will continue", e.toString());
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "POS-X3", "Error getting X3 Inventory Offline: " + e.toString());
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ex) {
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception ex) {
            }
        }

    }

    private static void verifySerialNumberOffline(String serialNumber, String warehouseId, String itemNumber) throws Exception {
        log.debug("Checking offline that serial number [{}] has X3 inventory for warehouse Id [{}] and item number [{}]", new Object[]{serialNumber, warehouseId, itemNumber});
        PreparedStatement ps = null;
        int qty = 0;
        Connection conn = getConnectionForOffline();
        try {
            ps = conn.prepareStatement(BaseUtils.getProperty("env.pos.x3.serialnumber.query.offline"));
            ps.setString(1, warehouseId);
            ps.setString(2, serialNumber);
            ps.setString(3, itemNumber);
            log.debug("About to run offline query to get Inventory quantity for serial number");
            long start = System.currentTimeMillis();
            ResultSet rs = ps.executeQuery();
            log.debug("Finished running offline query to get Inventory quantity for serial number. Query took [{}]ms", System.currentTimeMillis() - start);
            while (rs.next()) {
                String x3itemNumber = rs.getString(1);
                String x3serialNumber = rs.getString(2);
                if (x3itemNumber.equals(itemNumber) && x3serialNumber.equals(serialNumber)) {
                    qty += rs.getDouble(3);
                    log.debug("Got X3 offline item qty of [{}]", rs.getDouble(3));
                } else {
                    log.debug("Got X3 offline item qty with wrong case of [{}]", rs.getDouble(3));
                }
            }
            log.debug("X3 Total offline qty is [{}]", qty);

        } catch (Exception e) {
            log.warn("Error calling MYSQL to get offline stock for serial number from X3: [{}]. Will say stock was not available", e.toString());
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "POS-X3", "Error checking X3 Serial Number Offline: " + e.toString());
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ex) {
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception ex) {
            }
        }
        if (qty == 0) {
            throw new Exception("No stock for serial number -- Serial " + serialNumber + " in location " + warehouseId);
        }
    }

    public static Connection getConnectionForOffline() throws Exception {
        String dsName = "jdbc/SmileDB";
        long startTime = 0;
        if (log.isDebugEnabled()) {
            log.debug("Getting a connection from pool with JNDI name [{}]", dsName);
            startTime = System.currentTimeMillis();
        }
        Connection conn = JPAUtils.getNonJTAConnection(dsName);
        if (log.isDebugEnabled()) {
            long time = System.currentTimeMillis() - startTime;
            log.debug("Successfully got connection using datasource [{}]. Took [{}]ms", dsName, time);
        }
        return conn;
    }

    private static String getDepartment() {
        String department = props.getProperty("DepartmentDimension");
        if (department == null) {
            department = "";
        }
        return department;
    }

    private static BigDecimal getLastValueForBalancedGL(BigDecimal[] credits, BigDecimal[] debits) {
        int decimals = X3Field.getX3DecimalPlaces();
        BigDecimal totalCredits = BigDecimal.ZERO;
        BigDecimal totalDebits = BigDecimal.ZERO;
        for (BigDecimal credit : credits) {
            totalCredits = totalCredits.add(credit.setScale(decimals, RoundingMode.HALF_EVEN));
        }
        log.debug("Total credits [{}]", totalCredits);
        for (BigDecimal debit : debits) {
            totalDebits = totalDebits.add(debit.setScale(decimals, RoundingMode.HALF_EVEN));
        }
        log.debug("Total debits [{}]", totalDebits);
        BigDecimal res = totalCredits.subtract(totalDebits).abs();
        log.debug("getLastValueForBalancedGL result is [{}]", res);
        return res;
    }

    private static BigDecimal getDiffForBalancedGL(BigDecimal[] credits, BigDecimal[] debits) {
        int decimals = X3Field.getX3DecimalPlaces();
        BigDecimal totalCredits = BigDecimal.ZERO;
        BigDecimal totalDebits = BigDecimal.ZERO;
        for (BigDecimal credit : credits) {
            totalCredits = totalCredits.add(credit.setScale(decimals, RoundingMode.HALF_EVEN));
        }
        log.debug("Total credits [{}]", totalCredits);
        for (BigDecimal debit : debits) {
            totalDebits = totalDebits.add(debit.setScale(decimals, RoundingMode.HALF_EVEN));
        }
        log.debug("Total debits [{}]", totalDebits);
        BigDecimal res = totalCredits.subtract(totalDebits);
        log.debug("getDiffForBalancedGL result is [{}]", res);
        return res;
    }

    private static BigDecimal getValueExclVAT(BigDecimal valueInclVat) {
        double taxRate = BaseUtils.getDoubleProperty("env.sales.tax.percent");
        BigDecimal taxFactor = new BigDecimal(1.0 + taxRate / 100.0d);
        return valueInclVat.divide(taxFactor, 20, BigDecimal.ROUND_HALF_EVEN);
    }

    private static BigDecimal convertCentsToMajor(BigDecimal cents) {
        return cents.divide(new BigDecimal(100), RoundingMode.HALF_EVEN);
    }

    private static String getPartnerCode(EntityManager em, Sale sale) throws Exception {
        if (sale.getSalesPersonAccountId() == 1000000003) {
            String bcpNum = props.getProperty("ICPAPCustomerCode");
            log.debug("This is a commission payment sale - BCP Num will be [{}]", bcpNum);
            return bcpNum;
        }
        if (sale.getPaymentMethod().equals(POSManager.PAYMENT_METHOD_CREDIT_ACCOUNT) || sale.getPaymentMethod().equals(POSManager.PAYMENT_METHOD_CLEARING_BUREAU) || sale.getPaymentMethod().equals(POSManager.PAYMENT_METHOD_CREDIT_FACILITY)) {
            log.debug("This is a credit account sale BCPNUM will be [{}]", sale.getCreditAccountNumber());
            return sale.getCreditAccountNumber();
        }
        //As requested by Teboho on ticket http://jira.smilecoms.com/browse/ERP-97 - A replacement sale which is linked to a credit account sale must also
        // use the credit_account that was used in the original sale.
        if (sale.getPaymentMethod().equals(POSManager.PAYMENT_METHOD_CREDIT_NOTE)) {// Replacemenet to an organisation
            String returnId = sale.getPaymentTransactionData();
            if (returnId != null && !returnId.isEmpty()) {
                SaleReturn creditNote = DAO.getReturn(em, Integer.valueOf(returnId)); // Retrieving credit note
                if (creditNote != null) {
                    // Retrieve Original Sale
                    Sale origSale = DAO.getSale(em, creditNote.getSaleId());
                    if (origSale.getPaymentMethod().equals(POSManager.PAYMENT_METHOD_CREDIT_FACILITY) || origSale.getPaymentMethod().equals(POSManager.PAYMENT_METHOD_CREDIT_ACCOUNT) || sale.getPaymentMethod().equals(POSManager.PAYMENT_METHOD_CLEARING_BUREAU)) {
                        log.debug("The original sale was a credit account sale, so BCPNUM will be [{}]", origSale.getCreditAccountNumber());
                        return origSale.getCreditAccountNumber();
                    }
                }
            }
        }

        String bcpNum = props.getProperty("CashCustomerCode");
        log.debug("This is a cash sale BCPNUM will be [{}]", bcpNum);
        return bcpNum;
    }

    private static String getCardMachinesBankName(String tillId) {
        return tillId;
    }

    private static final Map<String, Long> deviceRevenueAccountMap = new ConcurrentHashMap<>();

    public static long getDeviceRevenueAccount(String deviceItemNumber) throws Exception {
        Long acc = deviceRevenueAccountMap.get(deviceItemNumber);
        if (acc == null) {
            // Get account
            LazyX3Connection lazyConn = new LazyX3Connection();
            Connection con = lazyConn.getConnection(true);
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement(BaseUtils.getProperty("env.pos.x3.device.rev.acc.query", "select CONVERT(bigint,B.ACC_1) from SC001.ITMMASTER A INNER JOIN SC001.GACCCODE B ON A.ACCCOD_0 = B.ACCCOD_0 where STOMGTCOD_0 = '2' and ITMREF_0=?"));
                ps.setString(1, deviceItemNumber);
                log.debug("About to run query to get device revenue account for item number [{}]", deviceItemNumber);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    acc = rs.getLong(1);
                    log.debug("Got account number [{}]", acc);
                } else {
                    throw new Exception("Cannot determine the X3 revenue account for item number -- " + deviceItemNumber);
                }
                log.debug("Finished running query");
            } catch (Exception e) {
                log.warn("Error getting device revenue account", e);
                throw e;
            } finally {
                try {
                    if (ps != null) {
                        ps.close();
                    }
                } catch (Exception ex) {
                }
                lazyConn.close();
            }
            deviceRevenueAccountMap.put(deviceItemNumber, acc);
        }
        return acc;
    }

    private static final Map<String, String> deviceDimensionMap = new ConcurrentHashMap<>();

    static String getProductDimension(String deviceItemNumber) throws Exception {
        String dimension = deviceDimensionMap.get(deviceItemNumber);
        if (dimension == null) {
            // Get account
            LazyX3Connection lazyConn = new LazyX3Connection();
            Connection con = lazyConn.getConnection(true);
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement(BaseUtils.getProperty("env.pos.x3.device.dimension.query", "select CCE_3 from SC001.ITMMASTER where STOMGTCOD_0 = 2 and ITMREF_0=?"));
                ps.setString(1, deviceItemNumber);
                log.debug("About to run query to get device product dimension for item number [{}]", deviceItemNumber);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    dimension = rs.getString(1);
                    log.debug("Got dimension [{}]", dimension);
                } else {
                    throw new Exception("Cannot determine the X3 product dimension for item number -- " + deviceItemNumber);
                }
                log.debug("Finished running query");
            } catch (Exception e) {
                log.warn("Error getting device product dimension", e);
                throw e;
            } finally {
                try {
                    if (ps != null) {
                        ps.close();
                    }
                } catch (Exception ex) {
                }
                lazyConn.close();
            }
            deviceDimensionMap.put(deviceItemNumber, dimension);
        }
        return dimension;
    }

    private static boolean isP2PItem(String itemNumber) {
        try {
            if (itemNumber == null) {
                return false;
            }

            Set<String> nonstock = BaseUtils.getPropertyAsSet("env.pos.p2p.equipment.items");
            return nonstock.contains(itemNumber);

        } catch (Exception e) {
            log.warn("Property env.pos.p2p.equipment.items does not exist");
        }
        return false;
    }

    public static void formatAVPsForSendingToSCA(List<AVP> avPs, int serviceSpecificationId, boolean populatePhotoData) {

        if (avPs == null) {
            return;
        }

        for (AVP avp : avPs) {
            if (avp != null && avp.getAttribute() != null) {
                AVP avpConfig = getAVPConfig(avp.getAttribute(), serviceSpecificationId);
                if (avpConfig != null) {
                    avp.setInputType(avpConfig.getInputType());
                    if (avp.getAttribute().equalsIgnoreCase("PublicIdentity")) {
                        avp.setValue(Utils.getPublicIdentityForPhoneNumber(avp.getValue()));
                    }
                }
            }
        }
    }

    private static AVP getAVPConfig(String attributeName, int serviceSpecificationId) {
        ServiceSpecification ss = NonUserSpecificCachedDataHelper.getServiceSpecification(serviceSpecificationId);
        for (AVP ssAVP : ss.getAVPs()) {
            if (attributeName.equals(ssAVP.getAttribute())) {
                return ssAVP;
            }
        }
        return null;
    }

    public static String reverseX3GL(EntityManager em, int primaryKey, String tableName, String transactionType) {

        Collection<X3TransactionState> existingGLs = DAO.getX3TransactionsByType(em, primaryKey, tableName, transactionType);
        log.debug("ReverseX3GLs got [{}] number of GLs to reverse using PrimaryKey [{}], TableName [{}] and TransactionType [{}]",
                new Object[]{primaryKey, tableName, transactionType});

        for (X3TransactionState curGL : existingGLs) {
            if (curGL.getStatus() != null && curGL.getStatus().equals("FI")) { //This was a successfull GL so we can reverse it.
                String glInfo = curGL.getExtraInfo();
                log.debug("GL info BEFORE reversal changes are applied: [{}]", glInfo);
                if (glInfo != null && !glInfo.isEmpty()) {
                    // -- Swap CREDIT with DEBITs and create new GL here
                    glInfo = glInfo.replaceAll("CREDIT_", "CRX_");
                    glInfo = glInfo.replaceAll("DEBIT_", "CREDIT_");
                    glInfo = glInfo.replaceAll("CRX_", "DEBIT_");

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                    String glEntryDate = sdf.format(new Date());
                    glInfo = glInfo.replaceAll("GL_ENTRY_DATE.*", "GL_ENTRY_DATE=" + glEntryDate); // GL_ENTRY_DATE=20190711

                    glInfo = glInfo.replace("GL_DESCRIPTION=", "GL_DESCRIPTION=RV-");

                    log.debug("GL info AFTER reversal changes are applied: [{}]", glInfo);
                    DAO.insertGLEntry(em, X3InterfaceDaemon.X3_TRANSACTION_TYPE_GL_ENTRY, primaryKey, "RV-" + tableName, glInfo);
                    log.debug("Successfully inserted record to reverse GL - PrimaryKey [{}], TableName [{}] and TransactionType [{}], Status [{}]",
                            new Object[]{curGL.getX3TransactionStatePK().getPrimaryKey(), curGL.getX3TransactionStatePK().getTableName(), curGL.getX3TransactionStatePK().getTransactionType(), curGL.getStatus()});
                } else {
                    log.error("ReverseGL, cannot reverse a GL with no ExtraInfo data - PrimaryKey [{}], TableName [{}] and TransactionType [{}], Status [{}]",
                            new Object[]{curGL.getX3TransactionStatePK().getPrimaryKey(), curGL.getX3TransactionStatePK().getTableName(), curGL.getX3TransactionStatePK().getTransactionType(), curGL.getStatus()});
                }
            } else {
                log.error("ReverseGL, cannot reverse a non successfull GL - PrimaryKey [{}], TableName [{}] and TransactionType [{}], Status [{}]",
                        new Object[]{curGL.getX3TransactionStatePK().getPrimaryKey(), curGL.getX3TransactionStatePK().getTableName(), curGL.getX3TransactionStatePK().getTransactionType(), curGL.getStatus()});
            }
        }

        return "done";
    }

    public static double getCentsRoundedForPOS(double val) {
        return Utils.round(val, BaseUtils.getIntProperty("env.pos.decimal.places.on.majorcurrency", X3Field.getX3DecimalPlaces()) - 2); // Subtract 2 as we are rounding cents
    }

    public static void logSaleLine(SaleLine saleLine, String msg) {
        if (log.isDebugEnabled()) {
            log.debug(msg + ". Item [{}] with quantity [{}] and Serial [{}]", new Object[]{saleLine.getInventoryItem().getItemNumber(), saleLine.getQuantity(), saleLine.getInventoryItem().getSerialNumber()});
            log.debug("Item PriceInCentsExcl [{}]", saleLine.getInventoryItem().getPriceInCentsExcl());
            log.debug("Item PriceInCentsIncl [{}]", saleLine.getInventoryItem().getPriceInCentsIncl());
            log.debug("SaleLineCentsExcl [{}]", saleLine.getLineTotalCentsExcl());
            log.debug("SaleLineCentsIncl [{}]", saleLine.getLineTotalCentsIncl());
            log.debug("SaleLineDiscountOnExclCents [{}]", saleLine.getLineTotalDiscountOnExclCents());
            log.debug("SaleLineDiscountOnInclCents [{}]", saleLine.getLineTotalDiscountOnInclCents());
        }
    }
}
