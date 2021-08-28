package com.smilecoms.pos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.reflect.TypeToken;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.localisation.LocalisationHelper;
import com.smilecoms.commons.localisation.PDFUtils;
import com.smilecoms.commons.platform.PlatformEventManager;
import com.smilecoms.commons.platform.SmileWebService;
import com.smilecoms.commons.sca.AVP;
import com.smilecoms.commons.sca.Account;
import com.smilecoms.commons.sca.AccountQuery;
import com.smilecoms.commons.sca.BalanceTransferLine;
import com.smilecoms.commons.sca.Address;
import com.smilecoms.commons.sca.BalanceTransferData;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.CustomerCommunicationData;
import com.smilecoms.commons.sca.CustomerQuery;
import com.smilecoms.commons.sca.IMSSubscriptionQuery;
import com.smilecoms.commons.sca.JiraField;
import com.smilecoms.commons.sca.MindMapFields;
import com.smilecoms.commons.sca.NewStickyNote;
import com.smilecoms.commons.sca.NewStickyNoteField;
import com.smilecoms.commons.sca.NewTTIssue;
import com.smilecoms.commons.sca.Organisation;
import com.smilecoms.commons.sca.OrganisationQuery;
import com.smilecoms.commons.sca.ProductInstance;
import com.smilecoms.commons.sca.ProductInstanceQuery;
import com.smilecoms.commons.sca.ProductOrder;
import com.smilecoms.commons.sca.ProductServiceInstanceMapping;
import com.smilecoms.commons.sca.SCABusinessError;
import com.smilecoms.commons.sca.SCAContext;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.ServiceInstanceOrder;
import com.smilecoms.commons.sca.ServiceInstanceQuery;
import com.smilecoms.commons.sca.StAccountLookupVerbosity;
import com.smilecoms.commons.sca.StAction;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.StIMSSubscriptionLookupVerbosity;
import com.smilecoms.commons.sca.StOrganisationLookupVerbosity;
import com.smilecoms.commons.sca.StProductInstanceLookupVerbosity;
import com.smilecoms.commons.sca.StServiceInstanceLookupVerbosity;
import com.smilecoms.commons.sca.StUnitCreditSpecificationLookupVerbosity;
import com.smilecoms.commons.sca.TransactionReversalData;
import com.smilecoms.commons.sca.UnitCreditInstance;
import com.smilecoms.commons.sca.UnitCreditInstanceQuery;
import com.smilecoms.commons.sca.UnitCreditSpecification;
import com.smilecoms.commons.sca.UnitCreditSpecificationList;
import com.smilecoms.commons.sca.UnitCreditSpecificationQuery;
import com.smilecoms.commons.sca.direct.bm.ChargingData;
import com.smilecoms.commons.sca.direct.bm.ChargingRequest;
import com.smilecoms.commons.sca.direct.bm.ChargingResult;
import com.smilecoms.commons.sca.direct.bm.MaximumExpiryDateOfUnitCreditOnAccountQuery;
import com.smilecoms.commons.sca.direct.bm.MaximumExpiryDateOfUnitCreditOnAccountReply;
import com.smilecoms.commons.sca.direct.bm.PlatformContext;
import com.smilecoms.commons.sca.direct.bm.ProvisionUnitCreditLine;
import com.smilecoms.commons.sca.direct.bm.ProvisionUnitCreditRequest;
import com.smilecoms.commons.sca.direct.bm.RatingKey;
import com.smilecoms.commons.sca.direct.bm.RequestedServiceUnit;
import com.smilecoms.commons.sca.direct.bm.ServiceInstanceIdentifier;
import com.smilecoms.commons.sca.direct.bm.UnitCreditInstanceList;
import com.smilecoms.commons.sca.direct.pvs.PrepaidStripCountQuery;
import com.smilecoms.commons.util.Codec;
import com.smilecoms.commons.util.ExceptionManager;
import com.smilecoms.commons.util.IMAPUtils;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import static com.smilecoms.pos.X3Helper.getFieldValueFromExtraInfo;
import com.smilecoms.pos.db.model.CashIn;
import com.smilecoms.pos.db.model.CashInRow;
import com.smilecoms.pos.db.model.PromotionCodeApproval;
import com.smilecoms.pos.db.model.ReturnReplacement;
import com.smilecoms.pos.db.model.SaleReturn;
import com.smilecoms.pos.db.model.SaleReturnRow;
import com.smilecoms.pos.db.model.SaleRow;
import com.smilecoms.pos.db.op.DAO;
import com.smilecoms.pos.paymentgateway.PaymentGatewayDaemon;
import com.smilecoms.pos.paymentgateway.PaymentGatewayPlugin;
import com.smilecoms.xml.pos.POSError;
import com.smilecoms.xml.pos.POSSoap;
import com.smilecoms.xml.schema.pos.CashInData;
import com.smilecoms.xml.schema.pos.CashInList;
import com.smilecoms.xml.schema.pos.CashInQuery;
import com.smilecoms.xml.schema.pos.Contract;
import com.smilecoms.xml.schema.pos.ContractList;
import com.smilecoms.xml.schema.pos.ContractQuery;
import com.smilecoms.xml.schema.pos.CreateStandardGLData;
import com.smilecoms.xml.schema.pos.CreateStandardGLOut;
import com.smilecoms.xml.schema.pos.CreditNote;
import com.smilecoms.xml.schema.pos.CreditNoteList;
import com.smilecoms.xml.schema.pos.CreditNoteQuery;
import com.smilecoms.xml.schema.pos.Done;
import com.smilecoms.xml.schema.pos.InventoryItem;
import com.smilecoms.xml.schema.pos.InventoryList;
import com.smilecoms.xml.schema.pos.InventoryQuery;
import com.smilecoms.xml.schema.pos.LineItem;
import com.smilecoms.xml.schema.pos.ObjectFactory;
import com.smilecoms.xml.schema.pos.PaymentNotificationData;
import com.smilecoms.xml.schema.pos.PlatformInteger;
import com.smilecoms.xml.schema.pos.PromotionCodeApprovalData;
import com.smilecoms.xml.schema.pos.Return;
import com.smilecoms.xml.schema.pos.ReturnData;
import com.smilecoms.xml.schema.pos.ReverseGLData;
import com.smilecoms.xml.schema.pos.ReverseGLOut;
import com.smilecoms.xml.schema.pos.Sale;
import com.smilecoms.xml.schema.pos.SaleLine;
import com.smilecoms.xml.schema.pos.SaleModificationData;
import com.smilecoms.xml.schema.pos.SalePostProcessingData;
import com.smilecoms.xml.schema.pos.SalesList;
import com.smilecoms.xml.schema.pos.SalesQuery;
import com.smilecoms.xml.schema.pos.SoldStockLocation;
import com.smilecoms.xml.schema.pos.SoldStockLocationData;
import com.smilecoms.xml.schema.pos.SoldStockLocationList;
import com.smilecoms.xml.schema.pos.SoldStockLocationQuery;
import com.smilecoms.xml.schema.pos.StDone;
import com.smilecoms.xml.schema.pos.StripCountQuery;
import com.smilecoms.xml.schema.pos.UpSizeInventoryQuery;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Startup;
import javax.ejb.Stateless;
import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import org.apache.commons.lang.StringEscapeUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.FileOutputStream;
import java.io.File;


/**
 *
 * @author paul
 */
@WebService(serviceName = "POS", portName = "POSSoap", endpointInterface = "com.smilecoms.xml.pos.POSSoap", targetNamespace = "http://xml.smilecoms.com/POS", wsdlLocation = "POSServiceDefinition.wsdl")
@Stateless
@Startup
@HandlerChain(file = "/handler.xml")
public class POSManager extends SmileWebService implements POSSoap {

    @javax.annotation.Resource
    javax.xml.ws.WebServiceContext wsctx;
    @PersistenceContext(unitName = "POSPU")
    private EntityManager emJTA;
    public static final String PAYMENT_METHOD_CASH = "Cash";
    public static final String PAYMENT_METHOD_DIRECT_AIRTIME = "Direct Airtime";
    public static final String PAYMENT_METHOD_BANK_TRANSFER = "Bank Transfer";
    public static final String PAYMENT_METHOD_STAFF = "Staff";
    public static final String PAYMENT_METHOD_LOAN = "Loan";
    public static final String PAYMENT_METHOD_CONTRACT = "Contract";
    public static final String PAYMENT_METHOD_CHEQUE = "Cheque";
    public static final String PAYMENT_METHOD_CREDIT_ACCOUNT = "Credit Account";
    public static final String PAYMENT_METHOD_CREDIT_NOTE = "Credit Note";
    public static final String PAYMENT_METHOD_QUOTE = "Quote";
    public static final String PAYMENT_METHOD_SHOP_PICKUP = "Shop Pickup";
    public static final String PAYMENT_METHOD_CLEARING_BUREAU = "Clearing Bureau";
    public static final String PAYMENT_METHOD_CARD_PAYMENT = "Card Payment";
    public static final String PAYMENT_METHOD_PAYMENT_GATEWAY = "Payment Gateway";
    public static final String PAYMENT_METHOD_DELIVERY_SERVICE = "Delivery Service";
    public static final String PAYMENT_METHOD_AIRTIME = "Airtime";
    public static final String PAYMENT_METHOD_CARD_INTEGRATION = "Card Integration";
    public static final String PAYMENT_METHOD_CREDIT_FACILITY = "Credit Facility";
    public static final String RETURN = "Return";
    // Make sure the list below is also on the search sale jsp in SEP
    public static final String PAYMENT_STATUS_PAID = "PD";
    public static final String PAYMENT_STATUS_FAILED = "FA";
    public static final String PAYMENT_STATUS_PENDING_PAYMENT = "PP";
    public static final String PAYMENT_STATUS_DELAYED_PAYMENT = "DP";
    public static final String PAYMENT_STATUS_QUOTE = "QT";
    public static final String PAYMENT_STATUS_SHOP_PICKUP = "SP";
    public static final String PAYMENT_STATUS_CONTRACT_WAITING = "WT";
    public static final String PAYMENT_STATUS_CONTRACT_GO = "GO";
    public static final String PAYMENT_STATUS_CONTRACT_PENDING_GO = "PG";
    public static final String PAYMENT_STATUS_STAFF_OR_LOAN = "ST";
    public static final String PAYMENT_STATUS_PENDING_VERIFICATION = "PV";
    public static final String PAYMENT_STATUS_STAFF_OR_LOAN_COMPLETE = "LC";
    public static final String PAYMENT_STATUS_RESEND_SALE_TO_X3 = "X3SL";
    public static final String PAYMENT_STATUS_RESEND_CASHIN_TO_X3 = "X3CI";
    public static final String PAYMENT_STATUS_INVOICE_REVERSAL = "RV";
    public static final String PAYMENT_STATUS_DELETED = "DE";
    public static final String PAYMENT_STATUS_REGENERATE = "RG";
    public static final String PAYMENT_STATUS_PENDING_DELIVERY = "PL"; // Pending Logistics
    public static final String PAYMENT_STATUS_GW_FAIL_GW = "FG";
    public static final String PAYMENT_STATUS_GW_FAIL_SMILE = "FS";

    private static final String POS_ADDRESS = "POS Address";

    private EntityManagerFactory emfRL = null;

    /**
     * Function which is used to determine if this instance of POS is available.
     * As POS relies entirely on a database and as we are not testing the
     * database, this function simply returns that POS is available.
     *
     * @param isUpRequest
     * @return always returns TRUE
     * @throws com.smilecoms.xml.pos.POSError
     */
    @Override
    public Done isUp(String isUpRequest) throws POSError {
        if (!BaseUtils.isPropsAvailable()) {
            throw createError(POSError.class, "Properties are not available so this platform will be reported as down");
        }
        return makeDone();
    }

    @PostConstruct
    public void startUp() {
        log.debug("POSManager Startup");
        emfRL = JPAUtils.getEMF("POSPU_RL");
    }

    @PreDestroy
    public void shutDown() {
        JPAUtils.closeEMF(emfRL);
    }

    /**
     * Utility method to make a complex boolean object with value TRUE.
     *
     * @return The resulting complex object
     */
    private com.smilecoms.xml.schema.pos.Done makeDone() {
        com.smilecoms.xml.schema.pos.Done done = new com.smilecoms.xml.schema.pos.Done();
        done.setDone(com.smilecoms.xml.schema.pos.StDone.TRUE);
        return done;
    }

    @Override
    public PlatformInteger getStripCountInSale(StripCountQuery stripCountQuery) throws POSError {
        setContext(stripCountQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        PlatformInteger cnt = new PlatformInteger();
        try {
            throw new java.lang.UnsupportedOperationException();
        } catch (Exception e) {
            throw processError(POSError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        //return cnt;
    }

    @Override
    public InventoryList getInventory(InventoryQuery inventoryQuery) throws POSError {
        setContext(inventoryQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        InventoryList inventoryList;
        InventorySystem inventorySystem = null;
        try {
            if (inventoryQuery.getWarehouseId().isEmpty()) {
                throw new Exception("Inventory query requires a warehouse id");
            }
            if (inventoryQuery.getCurrency() == null || inventoryQuery.getCurrency().isEmpty()) {
                inventoryQuery.setCurrency(BaseUtils.getProperty("env.currency.official.symbol"));
            }
            log.debug("Currency is [{}]", inventoryQuery.getCurrency());
            inventorySystem = InventorySystemManager.getInventorySystem();
            inventoryList = inventorySystem.getInventory(emJTA, inventoryQuery);
            inventoryList.setNumberOfInventoryItems(inventoryList.getInventoryItems().size());
            log.debug("POSManager getInventory is returning [{}] items", inventoryList.getInventoryItems().size());
        } catch (Exception e) {
            throw processError(POSError.class, e);
        } finally {
            if (inventorySystem != null) {
                inventorySystem.close();
            }
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return inventoryList;
    }

    @Override
    public Sale generateQuote(Sale saleDataForQuote) throws POSError {
        setContext(saleDataForQuote, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        InventorySystem inventorySystem = null;
        try {
            Set<String> exempt = null;
            try {
                exempt = BaseUtils.getPropertyAsSet("env.pos.cashin.exempt.customer.ids");
            } catch (com.smilecoms.commons.base.props.PropertyFetchException e) {
                log.debug("env.pos.cashin.exempt.customer.ids does not exist");
            }

            if (exempt == null || !exempt.contains(String.valueOf(saleDataForQuote.getSalesPersonCustomerId()))) {
                // Dont bother for admin sales - better performance

                log.debug("Getting salespersons non cashed in sales");
                Collection<com.smilecoms.pos.db.model.Sale> sales = DAO.getNonCashedInPaidCashOrCardPaymentSales(emJTA, saleDataForQuote.getSalesPersonCustomerId());
                log.debug("Got salespersons non cashed in sales [{}]", sales.size());

                if (!sales.isEmpty()) {
                    log.debug("This sales person has outstanding cashins. Looking at what the oldest one is");

                    int specialGraceDays = BaseUtils.getIntProperty("env.pos.cashin.extra.grace.days.sunday.and.public.holiday", 0);
                    int daysToCashin = BaseUtils.getIntProperty("env.pos.cashin.grace.days", 1000);
                    int hoursToCashIn = BaseUtils.getIntProperty("env.pos.cashin.grace.hours", 0); // Default to 10:00 cutoff for cashins - everything before this time must be cashed-in.
                    long oldestAllowedMillisAfterEpoch = new Date().getTime() - Integer.valueOf(daysToCashin).longValue() * 86400000l;

                    if (specialGraceDays != 0) {
                        // http://jira.smilecoms.com/browse/HBT-6968
                        log.debug("Using new complex cashin date calculation");
                        for (com.smilecoms.pos.db.model.Sale nonCashedInSale : sales) {
                            Date saleDate = nonCashedInSale.getSaleDateTime();
                            Date saleDatetoUse;
                            if (Utils.getDateDayOfWeek(saleDate) == Calendar.SATURDAY || Utils.isDateADayBeforeAPublicHoliday(saleDate)) {
                                log.debug("[{}] is a Saturday or a day before a public holiday. Adding [{}] days to it", saleDate, specialGraceDays);
                                Calendar c = Calendar.getInstance();
                                c.setTime(saleDate);
                                c.add(Calendar.DATE, specialGraceDays);
                                saleDatetoUse = c.getTime();
                            } else {
                                saleDatetoUse = saleDate;
                            }
                            log.debug("Using sale date [{}] for sale id [{}]", saleDatetoUse, nonCashedInSale.getSaleId());
                            if (saleDatetoUse.getTime() < oldestAllowedMillisAfterEpoch) {
                                throw new Exception("Sales have been disallowed due to one or more long outstanding cashins -- SalesPerson Customer Id: " + saleDataForQuote.getSalesPersonCustomerId() + " Sale Id: " + nonCashedInSale.getSaleId());
                            }
                        }
                    } else {
                        com.smilecoms.pos.db.model.Sale oldestNonCashedInSale = sales.iterator().next();
                        log.debug("Oldest sale is Id [{}] and was made on [{}]", oldestNonCashedInSale.getSaleId(), oldestNonCashedInSale.getSaleDateTime());

                        log.debug("CashIn grace days are set to [{}]", daysToCashin);
                        log.debug("CashIn grace hours are set to [{}]", hoursToCashIn);

                        if (hoursToCashIn == 0) { //Use old logic as before (based on CashIn grace days)
                            if (oldestNonCashedInSale.getSaleDateTime().getTime() < oldestAllowedMillisAfterEpoch) {
                                throw new Exception("Sales have been disallowed due to one or more long outstanding cashins -- SalesPerson Customer Id: " + saleDataForQuote.getSalesPersonCustomerId() + " Sale Id: " + oldestNonCashedInSale.getSaleId());
                            }
                        } else { // New logic as per http://jira.smilecoms.com/browse/HBT-7157
                            /* > [Thursday, August 17, 2017 9:35:02 AM Caro] so logic is lock warehouse at 10am if there are historical sales not cashed in by 10am
                            > [Thursday, August 17, 2017 9:35:46 AM Caro] to unlock warehouse all historical sales and current day sales  done until warehouse was lock needs to be cashed
                            > [Thursday, August 17, 2017 9:37:20 AM Caro] warehouse should not be locked if cashin for historical sales is done before 10am , when cashin is done during this period sales person is not forced to cashin current day sales
                            > [Thursday, August 17, 2017 9:37:41 AM Caro] historical equal to all sales done until yesterday 23h39
                            > [Thursday, August 17, 2017 9:38:05 AM Caro] current day = sales done from 24h00 to 09h59
                            > [Thursday, August 17, 2017 9:38:13 AM Caro] I hope its clear */
                            // Allow sales until the cut-off time, get the midnight timeslot;
                            Calendar ca = Calendar.getInstance();
                            ca.set(Calendar.HOUR_OF_DAY, hoursToCashIn);
                            ca.set(Calendar.MILLISECOND, 0);
                            ca.set(Calendar.SECOND, 0);
                            ca.set(Calendar.MINUTE, 0);

                            log.debug("Cutoff time for cashin today is [{}]", ca.getTime());
                            Date midNight = Utils.getBeginningOfDay(new Date());

                            log.warn("Oldest non-cashed in sale id is [{}]", oldestNonCashedInSale.getSaleDateTime());
                            log.error("Is date before holiday? = " + Utils.isDateADayBeforeAPublicHoliday(oldestNonCashedInSale.getSaleDateTime()));
                            //Check if current time has passed the grace hours;
                            if (!Utils.isInTheFuture(ca.getTime())
                                    && !Utils.isDateAPublicHoliday(ca.getTime())
                                    && !(Utils.isDateOverAWeekend(ca.getTime()))) { //Apply the new cash-in logic.
                                oldestAllowedMillisAfterEpoch = midNight.getTime(); //Beginning of today.
                                //Do not apply logic on weekends and public holidays:
                                /*if (Utils.isDateOverAWeekend(ca.getTime()) || Utils.isDateAPublicHoliday(ca.getTime())) {
                                    log.debug("[{}] is a Saturday, Sunday or a Public Holiday, skipping the cash-in rules check.", ca.getTime());
                                }*/

                                log.debug("Oldest Non-Cashed-In Sale EPOCH = [{}], OldestAllowedMillisAfterEpoch = [{}]",
                                        oldestNonCashedInSale.getSaleDateTime().getTime(), oldestAllowedMillisAfterEpoch);
                                if (oldestNonCashedInSale.getSaleDateTime().getTime() < oldestAllowedMillisAfterEpoch) { //There is a pending cashin for a sale done before today.
                                    // Exclude sales made today from midnight, these sales will be checked tomorrow after the cutoff time.
                                    throw new Exception("Sales have been disallowed due to one or more long outstanding cashins -- SalesPerson Customer Id: " + saleDataForQuote.getSalesPersonCustomerId() + " Sale Id: " + oldestNonCashedInSale.getSaleId());
                                } else { // All the Check if all the cash-ins where done today.
                                    Date latestCashInTimeForSalesDoneBeforeToday = DAO.getLatestCashInDateAndTimeOfSalesDoneBeforeToday(emJTA, saleDataForQuote.getSalesPersonCustomerId());
                                    log.debug("Latest cashin dateTime for sales done before today is [{}]", latestCashInTimeForSalesDoneBeforeToday);
                                    if (latestCashInTimeForSalesDoneBeforeToday != null) {
                                        if ((latestCashInTimeForSalesDoneBeforeToday.getTime() > ca.getTime().getTime())
                                                && // Done after the cutoff time today
                                                (oldestNonCashedInSale.getSaleDateTime().getTime() < ca.getTime().getTime())) { //All today's sales must be cashed in too.
                                            // Cashins where done after cut-off time and there is a pending cash-in for a sale done today.
                                            throw new Exception("Sales have been disallowed due to one or more long outstanding cashins -- SalesPerson Customer Id: " + saleDataForQuote.getSalesPersonCustomerId() + " Sale Id: " + oldestNonCashedInSale.getSaleId());
                                        }
                                    }

                                }
                            } else //Any other day before cut-off time
                            if (oldestNonCashedInSale.getSaleDateTime().before(Utils.getPastDateFromDate(Calendar.HOUR, 24, midNight))
                                    && !(Utils.getDateDayOfWeek(oldestNonCashedInSale.getSaleDateTime()) == Calendar.FRIDAY
                                    || (Utils.isDateADayBeforeAPublicHoliday(oldestNonCashedInSale.getSaleDateTime())
                                    && Utils.getNumWorkingDaysBetweenDates(oldestNonCashedInSale.getSaleDateTime(), ca.getTime()) == 0))) {
                                throw new Exception("Sales have been disallowed due to one or more long outstanding cashins -- SalesPerson Customer Id: " + saleDataForQuote.getSalesPersonCustomerId() + " Sale Id: " + oldestNonCashedInSale.getSaleId());
                            }

                            /*if (Utils.isDateOverAWeekend(ca.getTime()) || Utils.isDateAPublicHoliday(ca.getTime())) {
                                log.debug("[{}] is a Saturday, Sunday or a Public Holiday, skipping the cash-in rules check.", ca.getTime());
                            } else
                            
                            /*else // As per Caro - Skype: remember even when we relax the rules like in morning before cut off if you have cashin of more than 24 hours you should not be allowed to make sale Is this working in NG?
                            if (oldestNonCashedInSale.getSaleDateTime().getTime() < oldestAllowedMillisAfterEpoch) {
                                throw new Exception("Sales have been disallowed due to one or more long outstanding cashins -- SalesPerson Customer Id: " + saleDataForQuote.getSalesPersonCustomerId() + " Sale Id: " + oldestNonCashedInSale.getSaleId());
                            } */
                        }
                    }

                }

                if (!BaseUtils.getProperty("env.sales.barred.profile.ids", "").isEmpty()) {
                    Set<String> profIds = BaseUtils.getPropertyFromSQLAsSetWithoutCache("env.sales.barred.profile.ids");
                    if (profIds.contains(String.valueOf(saleDataForQuote.getSalesPersonCustomerId()))) {
                        throw new Exception("Sales have been disallowed for this salesperson");
                    }
                }
            }

            if (saleDataForQuote.getTenderedCurrency() == null || saleDataForQuote.getTenderedCurrency().isEmpty()) {
                saleDataForQuote.setTenderedCurrency(BaseUtils.getProperty("env.currency.official.symbol"));
            }

            // Check if warehouse is locked? http://jira.smilecoms.com/browse/HBT-7157
            if (DAO.checkIfStockLocationIsLocked(emJTA, saleDataForQuote.getWarehouseId())) {
                log.debug("Warehouse [{}] is locked in X3", saleDataForQuote.getWarehouseId());
                throw new Exception("Cannot make a sale from a locked warehouse -- Warehouse Id: " + saleDataForQuote.getWarehouseId());
            } else {
                log.debug("Warehouse [{}] is not locked in X3", saleDataForQuote.getWarehouseId());
            }

            inventorySystem = InventorySystemManager.getInventorySystem();
            saleDataForQuote.setSaleDate(Utils.getDateAsXMLGregorianCalendar(new Date()));
            fillInItemDetails(inventorySystem, saleDataForQuote, false, emJTA);
            doDiscounting(saleDataForQuote, emJTA);

            if (BaseUtils.getBooleanProperty("env.pos.ifrs15.enabled", false)) {
                verifyAllSaleLinesHaveAPrice(saleDataForQuote);
            }

            if (saleDataForQuote.getExpiryDate() == null) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DATE, 7);
                cal.add(Calendar.MINUTE, -1); // Prevent rounding issues
                saleDataForQuote.setExpiryDate(Utils.getDateAsXMLGregorianCalendar(cal.getTime()));
            }
            createEvent(saleDataForQuote.getSaleId());
        } catch (Exception e) {
            throw processError(POSError.class, e);
        } finally {
            if (inventorySystem != null) {
                inventorySystem.close();
            }
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return saleDataForQuote;
    }

    private void verifyAllSaleLinesHaveAPrice(Sale xmlSale) throws Exception {
        // Aas agreed with Caroline - HBT-8857 All products with exception of BUNKPB or BUNPB should have a price for them to be sold
        for (SaleLine line : xmlSale.getSaleLines()) {
            if (line.getInventoryItem().getPriceInCentsExcl() <= 0
                    && !(line.getInventoryItem().getItemNumber().startsWith("BUNKPB")
                    || line.getInventoryItem().getItemNumber().startsWith("BUNPB")
                    || line.getInventoryItem().getItemNumber().startsWith("BUNC"))) {
                throw new Exception("Cannot sell a product which does not have a price -- " + line.getInventoryItem().getItemNumber());
            }
        }
    }

    private boolean isSalePermission(EntityManager em, int customerId) {
        log.info("checking sales permission for " + customerId);
        if (customerId <= 0) {
            return false;
        }
        return DAO.isOnlyCustomerPermission(em, customerId);
    }

    @Override
    public Sale processSale(Sale xmlSale) throws POSError {
        log.info("Inside processSale");
        setContext(xmlSale, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        EntityManager emRL = null;
        InventorySystem inventorySystem = null;
        PostProcessingResult postProcessingResult = new PostProcessingResult();

        try {
            log.debug("Starting resource local transaction");
            emRL = JPAUtils.getEM(emfRL);
            JPAUtils.beginTransaction(emRL);
            int lines = 0;
            for (SaleLine line : xmlSale.getSaleLines()) {
                lines++;
                lines = lines + line.getSubSaleLines().size();
            }
            log.debug("Sale has [{}] lines", lines);
            if (lines > BaseUtils.getIntProperty("env.pos.sale.max.lines", 1500)) {
                throw new Exception("Sale has too many lines -- " + lines);
            }

            inventorySystem = InventorySystemManager.getInventorySystem();
            if (xmlSale.getSaleDate() == null) {
                xmlSale.setSaleDate(Utils.getDateAsXMLGregorianCalendar(new Date()));
            } else {
                log.debug("Got passed in sale date of [{}]", xmlSale.getSaleDate());
            }
            if (xmlSale.getExtTxId() == null) {
                xmlSale.setExtTxId(xmlSale.getPlatformContext().getTxId());
            }
            if (xmlSale.getUniqueId() != null && xmlSale.getUniqueId().isEmpty()) {
                xmlSale.setUniqueId(null);
            }
            log.debug("Sales ExtTxId is [{}]", xmlSale.getExtTxId());
            if (xmlSale.getExpiryDate() == null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(Utils.getJavaDate(xmlSale.getSaleDate()));
                cal.add(Calendar.DATE, 7);
                xmlSale.setExpiryDate(Utils.getDateAsXMLGregorianCalendar(cal.getTime()));
            }

            if (xmlSale.getTenderedCurrency() == null || xmlSale.getTenderedCurrency().isEmpty()) {
                xmlSale.setTenderedCurrency(BaseUtils.getProperty("env.currency.official.symbol"));
            }
            if (!xmlSale.getTenderedCurrency().equals(BaseUtils.getProperty("env.currency.official.symbol"))
                    && !xmlSale.getPaymentMethod().equals(PAYMENT_METHOD_CREDIT_ACCOUNT)
                    && !xmlSale.getPaymentMethod().equals(PAYMENT_METHOD_CONTRACT)
                    && !xmlSale.getPaymentMethod().equals(PAYMENT_METHOD_QUOTE)) {
                throw new Exception("Only local currency allowed for non credit account sales");
            }
            if (xmlSale.getPromotionCode() == null) {
                xmlSale.setPromotionCode("");
            }
            if (xmlSale.getPurchaseOrderData() == null) {
                xmlSale.setPurchaseOrderData("");
            }

            String comment = xmlSale.getPlatformContext().getComment() == null ? null : xmlSale.getPlatformContext().getComment().replace("\r\n", " ").replace("\n", " ");
            if (comment != null && !comment.isEmpty()) {
                if (xmlSale.getExtraInfo() == null) {
                    xmlSale.setExtraInfo("");
                }
                xmlSale.setExtraInfo(xmlSale.getExtraInfo() + "\r\nComment=" + comment);
            }
            xmlSale.setTenderedCurrencyExchangeRate(getExchangeRate(BaseUtils.getProperty("env.currency.official.symbol"), xmlSale.getTenderedCurrency()));

            if (X3Helper.isSaleGeneratedFromQuote(xmlSale.getExtraInfo()) || isSaleGeneratedFromShopPickup(xmlSale.getExtraInfo())) {
                fillInItemDetailsFromDB(xmlSale, emRL); // Extract line item details from the database quote and fill-in the new sale line item
            } else {
                fillInItemDetails(inventorySystem, xmlSale, true, emRL);
            }

            checkInventoryLevels(emRL, inventorySystem, xmlSale, emRL);
            doDiscounting(xmlSale, emRL);

            if (BaseUtils.getBooleanProperty("env.pos.ifrs15.enabled", false)) {
                // Aas agreed with Caroline - HBT-8857 All products with exception of BUNKPB or BUNPB should have a price for them to be sold
                verifyAllSaleLinesHaveAPrice(xmlSale);
            }

            if (xmlSale.getTotalLessWithholdingTaxCents() <= -1) { //compensate for rounding
                throw new Exception("A sale cannot be negative");
            }

            boolean isDuplicate = false;
            Set<String> duplicateAllowedCustIds = null;
            try {
                duplicateAllowedCustIds = BaseUtils.getPropertyAsSet("env.pos.duplicate.sales.allowed.salesperson.cust.ids");
            } catch (Exception e) {
            }
            if (!xmlSale.isIgnoreDuplicateSale() && !(duplicateAllowedCustIds != null && duplicateAllowedCustIds.contains(String.valueOf(xmlSale.getSalesPersonCustomerId())))) {
                try {
                    int min = BaseUtils.getIntProperty("env.pos.minutes.between.identical.sales", 10);
                    log.debug("Checking if this sale could be a duplicate of the last one to the same customer");
                    com.smilecoms.pos.db.model.Sale lastSaleToCustomer = DAO.getMostRecentSaleForCustomer(emRL, xmlSale.getRecipientCustomerId(), min);
                    if (Utils.isDateInTimeframe(lastSaleToCustomer.getSaleDateTime(), min, Calendar.MINUTE)
                            && Utils.round(lastSaleToCustomer.getSaleTotalCentsIncl().doubleValue(), 0) == Utils.round(xmlSale.getSaleTotalCentsIncl(), 0)
                            && lastSaleToCustomer.getRecipientAccountId() == xmlSale.getRecipientAccountId()
                            && !lastSaleToCustomer.getPaymentMethod().equals(PAYMENT_METHOD_CONTRACT)
                            && (lastSaleToCustomer.getStatus().equals(PAYMENT_STATUS_PAID)
                            || lastSaleToCustomer.getStatus().equals(PAYMENT_STATUS_PENDING_PAYMENT)
                            || lastSaleToCustomer.getStatus().equals(PAYMENT_STATUS_PENDING_DELIVERY))) {
                        isDuplicate = true;
                    }
                } catch (javax.persistence.NoResultException e) {
                    log.debug("Customer has never been sold to before");
                } catch (Exception e) {
                    JPAUtils.restartTransaction(emRL);
                    log.warn("Some error occured checking the last sale. Will allow the sale to continue: Error [{}]. Tx rollback Only: [{}]", e.toString(), emRL.getTransaction().getRollbackOnly());
                }
            } else {
                log.debug("Not doing duplicate sale check");
            }

            if (isDuplicate) {
                throw new Exception("This sale appears to be a duplicate of another recent sale");
            }

            if (xmlSale.getSaleLines().isEmpty()) {
                throw new Exception("A sale cannot be empty");
            }

            Customer salesPerson = getCustomer(xmlSale.getSalesPersonCustomerId());

            // This is for debugging issues with sales person addresses;
            log.debug("Sales Person [{}] has [{}] addresses.", salesPerson.getCustomerId(), salesPerson.getAddresses().size());
            for (Address add : salesPerson.getAddresses()) {
                log.debug("Sales Person [{}] address with id [{}] has state [{}]", salesPerson.getCustomerId(), add.getAddressId(), add.getState());
            }

            if (BaseUtils.getBooleanProperty("env.sale.dependiing.bundle.check.enabled", false)) {
                List<Integer> ucsIdList = new ArrayList<>();
                for (SaleLine xmlSaleLine : xmlSale.getSaleLines()) {
                    if (xmlSaleLine.getInventoryItem().getItemNumber().startsWith("BUN")) {
                        log.debug("SaleLine ItemNumber [{}] processSale.", xmlSaleLine.getInventoryItem().getItemNumber());
                        UnitCreditSpecification ucs = SCAWrapper.getAdminInstance().getUnitCreditSpecification(xmlSaleLine.getInventoryItem().getItemNumber());
                        ucsIdList.add(ucs.getUnitCreditSpecificationId());
                        log.debug("SaleLine UnitCreditSpecificationId [{}] processSale.", ucs.getUnitCreditSpecificationId());
                    }
                }

                for (SaleLine xmlSaleLine : xmlSale.getSaleLines()) {
                    if (xmlSaleLine.getInventoryItem().getItemNumber().startsWith("BUN")) {
                        log.debug("SaleLine ItemNumber [{}] processSale.", xmlSaleLine.getInventoryItem().getItemNumber());
                        UnitCreditSpecification ucs = SCAWrapper.getAdminInstance().getUnitCreditSpecification(xmlSaleLine.getInventoryItem().getItemNumber());
                        String conf = getPropertyFromConfig(ucs, "CanBeSoldWhenSpecIDExist");
                        if (!(conf == null || conf.length() <= 0)) {
                            long accountId = xmlSale.getRecipientAccountId();
                            log.debug("SaleLine accountId [{}] processSale.", accountId);
                            List<Integer> unitCreditCredInstanceIds = DAO.getAccountsActiveUnitCredits(emRL, accountId);
                            List<String> specIDsForUpsize = Utils.getListFromCommaDelimitedString(conf);
                            boolean mustExistSpecIdExists = false;
                            for (String mustExistSpecId : specIDsForUpsize) {
                                for (Integer ucsId : ucsIdList) {
                                    if (ucsId == Integer.parseInt(mustExistSpecId)) {
                                        mustExistSpecIdExists = true;
                                        break;
                                    }
                                }

                                if (mustExistSpecIdExists) {
                                    break;
                                }
                            }

                            if (unitCreditCredInstanceIds != null) {
                                if (!mustExistSpecIdExists) {
                                    for (String mustExistSpecId : specIDsForUpsize) {
                                        for (Integer clientUnitCreditId : unitCreditCredInstanceIds) {
                                            log.debug("SaleLine clientUnitCreditId [{}] processSale.", clientUnitCreditId);
                                            if (clientUnitCreditId == Integer.parseInt(mustExistSpecId)) {
                                                mustExistSpecIdExists = true;
                                                break;
                                            }
                                        }

                                        if (mustExistSpecIdExists) {
                                            break;
                                        }
                                    }
                                }
                            }

                            if (!mustExistSpecIdExists) {
                                log.warn("Error: No permission for sale add-on product, without main product on account or together. Requires one of the following SpecIDs on acc: ", specIDsForUpsize);
                                throw new Exception("No permission for sale add-on product without main product on account.");
                            }
                        }
                    }
                }

            }

            switch (xmlSale.getPaymentMethod()) {

                case PAYMENT_METHOD_CASH:
                    xmlSale.setChangeCents(xmlSale.getAmountTenderedCents() - xmlSale.getTotalLessWithholdingTaxCents());
                    if (xmlSale.getChangeCents() <= -1) { //compensate for rounding
                        throw new Exception("Change from a sale cannot be negative");
                    }
                    xmlSale.setStatus(PAYMENT_STATUS_PAID); // Paid

                    //check if sales persion has sales permission
                    if (isSalePermission(emRL, xmlSale.getSalesPersonCustomerId())) {
                        throw new Exception("No permission for sale");
                    }

                    Set<String> exempt = null;
                    try {
                        exempt = BaseUtils.getPropertyAsSet("env.pos.cashin.exempt.customer.ids");
                    } catch (com.smilecoms.commons.base.props.PropertyFetchException e) {
                        log.debug("env.pos.cashin.exempt.customer.ids does not exist");
                    }

                    if (exempt == null || !exempt.contains(String.valueOf(xmlSale.getSalesPersonCustomerId()))) {
                        double ucAndAirtimeCents = 0;
                        for (SaleLine line : xmlSale.getSaleLines()) {
                            if (line.getLineTotalCentsIncl() > 0 && (isUnitCredit(line.getInventoryItem()) || isAirtime(line.getInventoryItem()))) {
                                ucAndAirtimeCents += line.getLineTotalCentsIncl();
                            }
                        }
                        if (ucAndAirtimeCents > 0 && xmlSale.getSalesPersonAccountId() > 1100000000l) {
                            double outstandingUCAndAirtimeCents = getSalesPersonOutstandingUCAndAirtimeSalesCents(emRL, xmlSale.getSalesPersonCustomerId()).doubleValue();
                            log.debug("Salesperson has [{}]c of unit credit/airtime purchases not cashed in", outstandingUCAndAirtimeCents);
                            Account spAccount = SCAWrapper.getAdminInstance().getAccount(xmlSale.getSalesPersonAccountId(), StAccountLookupVerbosity.ACCOUNT);
                            double availBalCents = spAccount.getAvailableBalanceInCents();
                            log.debug("Account [{}] has available balance of [{}]c", spAccount.getAccountId(), availBalCents);
                            double netAvail = availBalCents - outstandingUCAndAirtimeCents - ucAndAirtimeCents;
                            log.debug("If this sale is processed then nett available would be [{}]", netAvail);

                            if (netAvail < 0 && BaseUtils.getBooleanProperty("env.pos.enforce.uc.float", true)) {
                                throw new Exception("Unit credit cash sales limit exceeded -- " + Utils.round(outstandingUCAndAirtimeCents / 100, 2) + " of unit credit/airtime purchases are not cashed in. Nett position would be " + Utils.round(netAvail / 100, 2) + " if this sale was allowed");
                            }
                        }
                    }
                    break;
                case PAYMENT_METHOD_DIRECT_AIRTIME:
                    log.debug("Making payment through direct airtime account");
                    xmlSale.setChangeCents(xmlSale.getAmountTenderedCents() - xmlSale.getTotalLessWithholdingTaxCents());
                    xmlSale.setStatus(PAYMENT_STATUS_PAID); // Paid

                    exempt = null;
                    try {
                        exempt = BaseUtils.getPropertyAsSet("env.pos.cashin.exempt.customer.ids");
                    } catch (com.smilecoms.commons.base.props.PropertyFetchException e) {
                        log.debug("env.pos.cashin.exempt.customer.ids does not exist");
                    }

                    if (exempt == null || !exempt.contains(String.valueOf(xmlSale.getSalesPersonCustomerId()))) {
                        double ucAndAirtimeCents = 0;
                        for (SaleLine line : xmlSale.getSaleLines()) {
                            if (line.getLineTotalCentsIncl() > 0 && (isUnitCredit(line.getInventoryItem()) || isAirtime(line.getInventoryItem()))) {
                                ucAndAirtimeCents += line.getLineTotalCentsIncl();
                            }
                        }
                        if (ucAndAirtimeCents > 0 && xmlSale.getSalesPersonAccountId() > 1100000000l) {
                            double outstandingUCAndAirtimeCents = getSalesPersonOutstandingUCAndAirtimeSalesCents(emRL, xmlSale.getSalesPersonCustomerId()).doubleValue();
                            log.debug("Salesperson has [{}]c of unit credit/airtime purchases not cashed in", outstandingUCAndAirtimeCents);
                            Account spAccount = SCAWrapper.getAdminInstance().getAccount(xmlSale.getSalesPersonAccountId(), StAccountLookupVerbosity.ACCOUNT);
                            double availBalCents = spAccount.getAvailableBalanceInCents();
                            log.debug("Account [{}] has available balance of [{}]c", spAccount.getAccountId(), availBalCents);
                            double netAvail = availBalCents - outstandingUCAndAirtimeCents - ucAndAirtimeCents;
                            log.debug("If this sale is processed then net available would be [{}]", netAvail);

                            if (netAvail < 0 && BaseUtils.getBooleanProperty("env.pos.enforce.uc.float", true)) {
                                throw new Exception("Unit credit cash sales limit exceeded -- " + Utils.round(outstandingUCAndAirtimeCents / 100, 2) + " of unit credit/airtime purchases are not cashed in. Nett position would be " + Utils.round(netAvail / 100, 2) + " if this sale was allowed");
                            }
                        }
                    }
                    break;

                case PAYMENT_METHOD_CONTRACT:
                    // Check if all the line items are allowed for this contract purchase
                    com.smilecoms.pos.db.model.Contract contract = DAO.getContract(emRL, xmlSale.getContractId());
                    checkIfContractStaffMemberIsAllowed(contract, xmlSale);
                    checkIfContractLineItemsAreAllowed(contract, xmlSale);
                    if (xmlSale.getAmountTenderedCents() != 0) {
                        throw new Exception("Tendered amount must be zero for a contract sale");
                    }
                    xmlSale.setChangeCents(0);
                    xmlSale.setStatus("WT"); // Waiting
                    break;

                case PAYMENT_METHOD_CARD_PAYMENT:
                    // For Card Payment we work on the fact that the customer is charged the exact amount
                    xmlSale.setChangeCents(0);
                    xmlSale.setAmountTenderedCents(xmlSale.getTotalLessWithholdingTaxCents());
                    xmlSale.setStatus(PAYMENT_STATUS_PAID); // Paid

                    exempt = null;
                    try {
                        exempt = BaseUtils.getPropertyAsSet("env.pos.cashin.exempt.customer.ids");
                    } catch (com.smilecoms.commons.base.props.PropertyFetchException e) {
                        log.debug("env.pos.cashin.exempt.customer.ids does not exist");
                    }

                    if (exempt == null || !exempt.contains(String.valueOf(xmlSale.getSalesPersonCustomerId()))) {
                        double ucAndAirtimeCents = 0;
                        for (SaleLine line : xmlSale.getSaleLines()) {
                            if (line.getLineTotalCentsIncl() > 0 && (isUnitCredit(line.getInventoryItem()) || isAirtime(line.getInventoryItem()))) {
                                ucAndAirtimeCents += line.getLineTotalCentsIncl();
                            }
                        }
                        if (ucAndAirtimeCents > 0 && xmlSale.getSalesPersonAccountId() > 1100000000l) {
                            double outstandingUCAndAirtimeCents = getSalesPersonOutstandingUCAndAirtimeSalesCents(emRL, xmlSale.getSalesPersonCustomerId()).doubleValue();
                            log.debug("Salesperson has [{}]c of unit credit/airtime purchases not cashed in", outstandingUCAndAirtimeCents);
                            Account spAccount = SCAWrapper.getAdminInstance().getAccount(xmlSale.getSalesPersonAccountId(), StAccountLookupVerbosity.ACCOUNT);
                            double availBalCents = spAccount.getAvailableBalanceInCents();
                            log.debug("Account [{}] has available balance of [{}]c", spAccount.getAccountId(), availBalCents);
                            double netAvail = availBalCents - outstandingUCAndAirtimeCents - ucAndAirtimeCents;
                            log.debug("If this sale is processed then nett available would be [{}]", netAvail);

                            if (netAvail < 0 && BaseUtils.getBooleanProperty("env.pos.enforce.uc.float", true)) {
                                throw new Exception("Unit credit cash sales limit exceeded -- " + Utils.round(outstandingUCAndAirtimeCents / 100, 2) + " of unit credit/airtime purchases are not cashed in. Nett position would be " + Utils.round(netAvail / 100, 2) + " if this sale was allowed");
                            }
                        }
                    }
                    break;

                case PAYMENT_METHOD_CARD_INTEGRATION:
                    if (xmlSale.getAmountTenderedCents() != 0) {
                        throw new Exception("Tendered amount must be zero for a card integration sale");
                    }
                    xmlSale.setAmountTenderedCents(0);
                    xmlSale.setChangeCents(0);
                    xmlSale.setStatus(PAYMENT_STATUS_PENDING_PAYMENT); // Pending Payment
                    break;

                case PAYMENT_METHOD_PAYMENT_GATEWAY:
                    // Payment gateway sales are polled until paid. They then become clearing bureau sales
                    if (xmlSale.getAmountTenderedCents() != 0) {
                        throw new Exception("Tendered amount must be zero for a payment gateway sale");
                    }
                    PaymentGatewayPlugin plugin = PaymentGatewayDaemon.getPlugin(emRL, xmlSale.getPaymentGatewayCode());
                    if (!plugin.isUp()) {
                        throw new Exception("Payment partner is down -- " + xmlSale.getPaymentGatewayCode());
                    }
                    xmlSale.setSalesPersonAccountId(plugin.getAccountId());
                    xmlSale.setAmountTenderedCents(0);
                    CreditAccountNumberHelper.populateCreditAccountNumberForSale(xmlSale);
                    if (xmlSale.getCreditAccountNumber().isEmpty()) {
                        throw new Exception("That customer does not have a credit account");
                    }
                    xmlSale.setChangeCents(0);
                    xmlSale.setStatus(PAYMENT_STATUS_PENDING_PAYMENT); // Pending Payment
                    break;

                case PAYMENT_METHOD_DELIVERY_SERVICE:
                    // For delivery service, we create a pending delivery sale (PL for pending logistics) and then wait for the SIM to be provisioned
                    if (xmlSale.getAmountTenderedCents() != 0) {
                        throw new Exception("Tendered amount must be zero for a delivery service sale");
                    }
                    CreditAccountNumberHelper.populateCreditAccountNumberForSale(xmlSale);
                    if (xmlSale.getCreditAccountNumber().isEmpty()) {
                        throw new Exception("That customer does not have a credit account");
                    }
                    xmlSale.setSalesPersonAccountId(BaseUtils.getLongProperty("env.pos.delivery.service.account.number", 1000000006));
                    xmlSale.setChangeCents(0);
                    xmlSale.setStatus(PAYMENT_STATUS_PENDING_DELIVERY); // Pending Delivery (PL)
                    break;

                case PAYMENT_METHOD_CLEARING_BUREAU:
                    if (xmlSale.getAmountTenderedCents() != 0) {
                        throw new Exception("Tendered amount must be zero for a clearing bureau sale");
                    }
                    xmlSale.setChangeCents(0);
                    xmlSale.setAmountTenderedCents(xmlSale.getTotalLessWithholdingTaxCents());
                    xmlSale.setStatus(PAYMENT_STATUS_PAID);
                    xmlSale.setExtraInfo(Utils.setValueInCRDelimitedAVPString(xmlSale.getExtraInfo(), "PaidDate", Utils.getDateAsString(new Date(), "yyyy/MM/dd")));
                    if (xmlSale.getCreditAccountNumber() == null || xmlSale.getCreditAccountNumber().isEmpty()) {
                        // Check if we can auto populate the credit account number
                        CreditAccountNumberHelper.populateCreditAccountNumberForSale(xmlSale);
                    }

                    Set doChecks = null;
                    try {
                        doChecks = BaseUtils.getPropertyAsSet("env.pos.clearing.bureau.do.credit.checks");
                    } catch (Exception e) {
                        log.warn("Error getting env.pos.clearing.bureau.do.credit.checks Will assume its empty");
                    }

                    if (doChecks != null
                            && xmlSale.getTotalLessWithholdingTaxCents() > 0
                            && BaseUtils.getPropertyAsSet("env.pos.clearing.bureau.do.credit.checks").contains(xmlSale.getCreditAccountNumber())) {
                        boolean hasCredit;
                        try {
                            hasCredit = X3Helper.isCreditAccountAbleToTakeDebit(emRL, xmlSale.getCreditAccountNumber(), xmlSale.getTotalLessWithholdingTaxCents() * xmlSale.getTenderedCurrencyExchangeRate());
                        } catch (Exception e) {
                            hasCredit = true;
                            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "Error checking credit balance for clearing bureau sale. Going to allow anyway: " + e.toString());
                        }
                        if (!hasCredit) {
                            throw new Exception("Credit limit or payment limit exceeded");
                        }
                    }

                    break;

                case PAYMENT_METHOD_CREDIT_ACCOUNT:

                    if (xmlSale.getAmountTenderedCents() != 0) {
                        throw new Exception("Tendered amount must be zero for a credit account");
                    }
                    xmlSale.setChangeCents(0);
                    xmlSale.setStatus(PAYMENT_STATUS_PAID); // Paid
                    if (xmlSale.getRecipientCustomerId() == 0) {
                        throw new Exception("Credit account payments must have a recipient");
                    }
                    if (xmlSale.getCreditAccountNumber() == null || xmlSale.getCreditAccountNumber().isEmpty()) {
                        // Check if we can auto populate the credit account number based on whats in the sale
                        CreditAccountNumberHelper.populateCreditAccountNumberForSale(xmlSale);
                    }
                    if (xmlSale.getTenderedCurrency().equals("USD")) {
                        xmlSale.setCreditAccountNumber(xmlSale.getCreditAccountNumber() + "U");
                    } else if (xmlSale.getTenderedCurrency().equals("EUR")) {
                        xmlSale.setCreditAccountNumber(xmlSale.getCreditAccountNumber() + "E");
                    }
                    log.debug("Credit account number is [{}]", xmlSale.getCreditAccountNumber());
                    if (xmlSale.getTotalLessWithholdingTaxCents() > 0
                            && !xmlSale.getCreditAccountNumber().equals(X3Helper.props.getProperty("CustomerLoanCustomerCode"))
                            && !xmlSale.getCreditAccountNumber().equals(X3Helper.props.getProperty("StaffLoanCustomerCode"))
                            && !xmlSale.getCreditAccountNumber().equals(X3Helper.props.getProperty("ConsolidatedGiftCustomerCode"))
                            && !X3Helper.isCreditAccountAbleToTakeDebit(emRL, xmlSale.getCreditAccountNumber(), xmlSale.getTotalLessWithholdingTaxCents() * xmlSale.getTenderedCurrencyExchangeRate())) {
                        throw new Exception("Credit limit or payment limit exceeded");
                    }
                    break;

                case PAYMENT_METHOD_QUOTE:

                    if (xmlSale.getAmountTenderedCents() != 0) {
                        throw new Exception("Tendered amount must be zero for a quote");
                    }
                    xmlSale.setChangeCents(0);
                    xmlSale.setStatus(PAYMENT_STATUS_QUOTE); // Quote
                    break;

                case PAYMENT_METHOD_SHOP_PICKUP:

                    if (xmlSale.getAmountTenderedCents() != 0) {
                        throw new Exception("Tendered amount must be zero for a shop pickup");
                    }
                    xmlSale.setChangeCents(0);
                    xmlSale.setSalesPersonAccountId(BaseUtils.getLongProperty("env.pos.shop.pickup.account.number", 1000000006));
                    xmlSale.setStatus(PAYMENT_STATUS_SHOP_PICKUP); // Shop pickup. Like a quote but not sent to X3
                    break;

                case PAYMENT_METHOD_CREDIT_FACILITY:
                    if (xmlSale.getAmountTenderedCents() != 0) {
                        throw new Exception("Tendered amount must be zero for a credit facility");
                    }
                    if (xmlSale.getPaymentTransactionData().isEmpty()) {
                        throw new Exception("Credit facility loan reference must be provided");
                    }
                    log.debug("This sale is being paid for with a credit facility. The credit facility loan reference is [{}]", xmlSale.getPaymentTransactionData());
                    Set<String> creditFacilityAllowedKits = new HashSet<>();
                    boolean regulateCreditFacilityKits = BaseUtils.getBooleanProperty("env.pos.regulate.credit.facility.payment.kits", true);
                    if (regulateCreditFacilityKits) {
                        creditFacilityAllowedKits = BaseUtils.getPropertyAsSet("env.pos.credit.facility.payment.allowed.kits");
                    }

                    for (SaleLine line : xmlSale.getSaleLines()) {
                        if (isAirtime(line.getInventoryItem()) && !xmlSale.getPlatformContext().getOriginatingIdentity().equals("admin")) {
                            throw new Exception("Airtime cannot be included in a credit facility payment");
                        }
                        if (isUnitCredit(line.getInventoryItem()) && !xmlSale.getPlatformContext().getOriginatingIdentity().equals("admin")) {
                            throw new Exception("Bundles cannot be included in a credit facility payment");
                        }
                        String kitItemNumber = line.getInventoryItem().getItemNumber();
                        if (regulateCreditFacilityKits) {
                            if (!creditFacilityAllowedKits.contains(line.getInventoryItem().getItemNumber())) {
                                throw new Exception("This kit is not allowed to be purchased by credit facility payment method -- " + kitItemNumber);
                            }
                        }
                    }

                    if (DAO.hasCreditFacilityLoanReferenceBeenUsed(emRL, xmlSale.getPaymentTransactionData())) {
                        throw new Exception("Credit facility loan reference has already been used");
                    }

                    log.debug("Credit account number is [{}]", xmlSale.getCreditAccountNumber());
                    if (xmlSale.getTotalLessWithholdingTaxCents() > 0 && !X3Helper.isCreditAccountAbleToTakeDebit(emRL, xmlSale.getCreditAccountNumber(), xmlSale.getTotalLessWithholdingTaxCents() * xmlSale.getTenderedCurrencyExchangeRate())) {
                        throw new Exception("Credit limit or payment limit exceeded");
                    }
                    xmlSale.setChangeCents(0);
                    xmlSale.setStatus(PAYMENT_STATUS_PAID);

                    break;

                case PAYMENT_METHOD_AIRTIME:

                    if (xmlSale.getAmountTenderedCents() != 0) {
                        throw new Exception("Tendered amount must be zero for airtime payment method");
                    }

                    xmlSale.setChangeCents(0);
                    xmlSale.setStatus(PAYMENT_STATUS_PAID);

                    break;

                case PAYMENT_METHOD_CREDIT_NOTE:

                    if (xmlSale.getAmountTenderedCents() != 0) {
                        throw new Exception("Tendered amount must be zero for a credit note");
                    }
                    if (xmlSale.getPaymentTransactionData().isEmpty()) {
                        throw new Exception("Credit note number must be provided");
                    }
                    int creditNoteId = Integer.parseInt(xmlSale.getPaymentTransactionData().trim());
                    log.debug("This sale is being paid for with a credit note. The credit note id is [{}]", creditNoteId);
                    if (DAO.hasCreditNoteBeenUsed(emRL, xmlSale.getPaymentTransactionData())) {
                        throw new Exception("Credit note has already been used");
                    }
                    // PCB 2014-06-06: Business rules agreed by Pieter, Heleen, Rafael - The person who purchased the original item must be the recipient of the new sale
                    int origCustId = DAO.getSale(emRL, DAO.getReturn(emRL, creditNoteId).getSaleId()).getRecipientCustomerId();
                    if (origCustId != xmlSale.getRecipientCustomerId()) {
                        throw new Exception("A credit note can only be used by the customer who purchased the original item -- Original customer id " + origCustId + " this customer id " + xmlSale.getRecipientCustomerId());
                    }

                    // As requested by Caroline HBT-8740
                    int origOrgId = DAO.getSale(emRL, DAO.getReturn(emRL, creditNoteId).getSaleId()).getRecipientOrganisationId();
                    if (origOrgId != xmlSale.getRecipientOrganisationId()) {
                        throw new Exception("A credit note can only be used by the organisation which purchased the original item -- Original Organisation id " + origOrgId + " this Organisation id " + xmlSale.getRecipientOrganisationId());
                    }
                    log.debug("Verifying that the items on this sale are equivalent replacements for the items that were returned");
                    List<CreditNoteRow> creditNoteRows = getCreditNoteRows(creditNoteId, emRL);
                    xmlSale.setSaleTotalCentsExcl(0);
                    xmlSale.setSaleTotalCentsIncl(0);
                    xmlSale.setSaleTotalDiscountOnExclCents(0);
                    xmlSale.setSaleTotalDiscountOnInclCents(0);
                    xmlSale.setSaleTotalTaxCents(0);
                    for (SaleLine line : xmlSale.getSaleLines()) {
                        boolean foundMatch = false;
                        if (isAirtime(line.getInventoryItem()) && !xmlSale.getPlatformContext().getOriginatingIdentity().equals("admin")) {
                            throw new Exception("Airtime cannot be included in a credit note payment");
                        }

                        if (isP2PProduct(line)) {
                            throw new Exception("Credit note payment method not permitted on this product -- " + line.getInventoryItem().getItemNumber());
                        }
                        // Recalcualte line prices based on the original price of the sale that had the return
                        for (CreditNoteRow creditNoteRow : creditNoteRows) {
                            if (creditNoteRow.quantityLeftForCredit >= line.getQuantity() && isEquivalentForReplacement(line.getInventoryItem().getItemNumber(), creditNoteRow.itemNumber, salesPerson.getSecurityGroups())) {
                                creditNoteRow.quantityLeftForCredit -= line.getQuantity();
                                foundMatch = true;
                                log.debug("Found a credit note row with item number [{}] and per item sales price [{}]cents incl which covers the new invoice item number [{}]. Going to set the original items sale price", new Object[]{creditNoteRow.itemNumber, creditNoteRow.perItemSalesPriceInCentsIncl, line.getInventoryItem().getItemNumber()});
                                line.setLineTotalCentsExcl(creditNoteRow.perItemSalesPriceInCentsExcl.multiply(new BigDecimal(line.getQuantity())).doubleValue());
                                line.setLineTotalCentsIncl(creditNoteRow.perItemSalesPriceInCentsIncl.multiply(new BigDecimal(line.getQuantity())).doubleValue());
                                line.setLineTotalDiscountOnExclCents(creditNoteRow.perItemDiscountInCentsExcl.multiply(new BigDecimal(line.getQuantity())).doubleValue());
                                line.setLineTotalDiscountOnInclCents(creditNoteRow.perItemDiscountInCentsIncl.multiply(new BigDecimal(line.getQuantity())).doubleValue());
                                xmlSale.setSaleTotalCentsExcl(xmlSale.getSaleTotalCentsExcl() + line.getLineTotalCentsExcl());
                                xmlSale.setSaleTotalCentsIncl(xmlSale.getSaleTotalCentsIncl() + line.getLineTotalCentsIncl());
                                xmlSale.setSaleTotalDiscountOnExclCents(xmlSale.getSaleTotalDiscountOnExclCents() + line.getLineTotalDiscountOnExclCents());
                                xmlSale.setSaleTotalDiscountOnInclCents(xmlSale.getSaleTotalDiscountOnInclCents() + line.getLineTotalDiscountOnInclCents());
                                break;
                            }
                        }
                        if (!foundMatch && !xmlSale.getPlatformContext().getOriginatingIdentity().equals("admin")) {
                            throw new Exception("Credit note does not cover all the items in the sale");
                        }
                    }
                    xmlSale.setSaleTotalTaxCents(xmlSale.getSaleTotalCentsIncl() - xmlSale.getSaleTotalCentsExcl());

                    if (BaseUtils.getBooleanProperty("env.pos.withholding.tax.on.gross")) {
                        xmlSale.setWithholdingTaxCents(xmlSale.getSaleTotalCentsExcl() * xmlSale.getWithholdingTaxRate() / 100d);
                    } else {
                        xmlSale.setWithholdingTaxCents(xmlSale.getSaleTotalCentsIncl() * xmlSale.getWithholdingTaxRate() / 100d);
                    }
                    xmlSale.setTotalLessWithholdingTaxCents(xmlSale.getSaleTotalCentsIncl() - xmlSale.getWithholdingTaxCents());
                    for (CreditNoteRow creditNoteRow : creditNoteRows) {
                        if (creditNoteRow.quantityLeftForCredit != 0 && !xmlSale.getPlatformContext().getOriginatingIdentity().equals("admin")) {
                            throw new Exception("Credit note covers more items than the sale");
                        }
                    }

                    xmlSale.setChangeCents(0);
                    xmlSale.setStatus(PAYMENT_STATUS_PAID);
                    break;

                case PAYMENT_METHOD_LOAN:
                    for (SaleLine line : xmlSale.getSaleLines()) {
                        if (!line.getSubSaleLines().isEmpty()) {
                            throw new Exception("Kits cannot be loaned to customers");
                        }
                    }
                case PAYMENT_METHOD_STAFF:

                    if (!xmlSale.isTaxExempt()) {
                        throw new Exception("Loans for trials or staff must be flagged as tax exempt");
                    }

                    if (xmlSale.getAmountTenderedCents() != 0) {
                        throw new Exception("Tendered amount must be zero for staff or trial");
                    }
                    for (SaleLine line : xmlSale.getSaleLines()) {
                        if (isAirtime(line.getInventoryItem())) {
                            throw new Exception("Airtime cannot be included in a stock loan");
                        }
                        if (isUnitCredit(line.getInventoryItem())) {
                            throw new Exception("Unit Credit cannot be included in a stock loan");
                        }
                        for (SaleLine subLine : line.getSubSaleLines()) {
                            if (isAirtime(subLine.getInventoryItem())) {
                                throw new Exception("Airtime cannot be included in a stock loan");
                            }
                            if (isUnitCredit(subLine.getInventoryItem())) {
                                throw new Exception("Unit Credit cannot be included in a stock loan");
                            }
                        }
                    }
                    if (xmlSale.getRecipientCustomerId() == 0) {
                        throw new Exception("Stock loans must have a recipient");
                    }
                    xmlSale.setChangeCents(0);
                    xmlSale.setStatus(PAYMENT_STATUS_STAFF_OR_LOAN); // Stock Transfer or loan / Staff or Trial
                    break;

                case PAYMENT_METHOD_BANK_TRANSFER:
                case PAYMENT_METHOD_CHEQUE:

                    if (xmlSale.getAmountTenderedCents() != 0) {
                        throw new Exception("Tendered amount must be zero for a non-cash sale");
                    }
                    xmlSale.setChangeCents(0);

                    Calendar cal = Calendar.getInstance();
                    cal.setTime(Utils.getJavaDate(xmlSale.getSaleDate()));
                    cal.add(Calendar.DATE, BaseUtils.getIntProperty("env.pos.delayedpayment.threshold.days", 7));
                    if (Utils.getJavaDate(xmlSale.getExpiryDate()).after(cal.getTime())) {
                        log.debug("This sale has an expiry date that is more than 7 days after the sale date. We will put it in a special status saying its pending payment delayed (DP)");
                        xmlSale.setStatus(PAYMENT_STATUS_DELAYED_PAYMENT); // Delayed Payment
                    } else {
                        xmlSale.setStatus(PAYMENT_STATUS_PENDING_PAYMENT); // Pending Payment
                    }
                    break;

                default:
                    throw new Exception("Unknown payment method -- " + xmlSale.getPaymentMethod());

            } // End switching on the payment type

            log.debug("Setting transaction and delivery fees on sale");
            FeeCalculator feeCalc = new FeeCalculator(xmlSale, emRL);
            xmlSale.setTransactionFeeCents(feeCalc.getTransactionFeeCents());
            xmlSale.setTransactionFeeModel(feeCalc.getTransactionFeeModel());
            xmlSale.setDeliveryFeeCents(feeCalc.getDeliveryFeeCents());
            xmlSale.setDeliveryFeeModel(feeCalc.getDeliveryFeeModel());
            log.debug("Delivery Model [{}] Delivery Cents [{}] Tx Fee Model [{}] Tx Fee Cents [{}]",
                    new Object[]{xmlSale.getDeliveryFeeModel(), xmlSale.getDeliveryFeeCents(), xmlSale.getTransactionFeeModel(), xmlSale.getTransactionFeeCents()});

            Customer cust = getCustomer(xmlSale.getRecipientCustomerId());
            Organisation org = getOrganisation(xmlSale.getRecipientOrganisationId());
            int heldByOrganisationId = 0;

            if (xmlSale.getRecipientOrganisationId() > 0) { // Automatically assign stock to Franchises (held by organisation) when they buy from us. 
                if (POSManager.isFranchise(xmlSale.getRecipientOrganisationId())) {
                    heldByOrganisationId = xmlSale.getRecipientOrganisationId();
                }
            }

            String state = null;
            try {
                state = BaseUtils.getSubProperty("env.pos.overridden.locations", xmlSale.getWarehouseId());
                log.debug("Overridden state for [{}] is [{}]", xmlSale.getWarehouseId(), state);
            } catch (Exception e) {
                log.debug("env.pos.overridden.locations does not exist");
            }
            if (state == null) {
                state = "Unknown";
                log.debug("Sales Person [{}] still has [{}] addresses.", salesPerson.getCustomerId(), salesPerson.getAddresses().size());
                log.debug("Populating the sales persons location with the state of the first address we find on their profile");
                for (Address add : salesPerson.getAddresses()) {
                    log.debug("Sales Person [{}] address with id [{}] has state [{}]", salesPerson.getCustomerId(), add.getAddressId(), add.getState());
                    if (!add.getState().isEmpty()) {
                        state = add.getState();
                        break;
                    }
                }
            }

            //Check if we must get GPS Coordinates?
            // Quote, Cash, Bank Transfer, Cheque, Credit Account, Staff,Loan, Credit Note, Card Payment
            if (BaseUtils.getBooleanProperty("env.pos.sale.save.gps.coordinates", false)
                    && (xmlSale.getPaymentMethod().equals(PAYMENT_METHOD_QUOTE)
                    || xmlSale.getPaymentMethod().equals(PAYMENT_METHOD_CASH)
                    || xmlSale.getPaymentMethod().equals(PAYMENT_METHOD_BANK_TRANSFER)
                    || xmlSale.getPaymentMethod().equals(PAYMENT_METHOD_CHEQUE)
                    || xmlSale.getPaymentMethod().equals(PAYMENT_METHOD_CREDIT_ACCOUNT)
                    || xmlSale.getPaymentMethod().equals(PAYMENT_METHOD_LOAN)
                    || xmlSale.getPaymentMethod().equals(PAYMENT_METHOD_STAFF)
                    || xmlSale.getPaymentMethod().equals(PAYMENT_METHOD_CREDIT_NOTE)
                    || xmlSale.getPaymentMethod().equals(PAYMENT_METHOD_CARD_PAYMENT))) { //Any of these methods can be used by a sales person so we can try to extract their GPS location.

                String gpsLocation = DAO.getSalesPersonGPSCoorinates(emRL, xmlSale.getSalesPersonCustomerId());
                xmlSale.setGpsCoordinates(gpsLocation);
            }

            log.debug("Sales persons state is [{}]", state);
            List<String> newDimensions = inventorySystem.getDimensions(xmlSale.getChannel(), state);
            String posState = getCustomerState(salesPerson, POS_ADDRESS);
            List<String> posDimentions = null;
            if (posState != null) {
                posDimentions = inventorySystem.getDimensions(xmlSale.getChannel(), posState);
            }
            // Use POS dimentions if it is present else use physical address 
            if (posDimentions != null && newDimensions.size() != 2 && posDimentions.size() == 2) {
                newDimensions = posDimentions;
            }
            if (newDimensions.size() != 2) {
                // Normal customers may not have an address and airtime payment method wont go to X3 anyway
                if (!xmlSale.getPaymentMethod().equals(PAYMENT_METHOD_AIRTIME)) {
                    throw new Exception("Invalid channel or location data -- channel [" + xmlSale.getChannel() + "] and sale location [" + state + "].");
                }
            } else {
                log.debug("Setting sales channel to [{}] and location to [{}]", newDimensions.get(0), newDimensions.get(1));
                xmlSale.setSaleLocation(newDimensions.get(1));
                xmlSale.setChannel(newDimensions.get(0));
            }
            xmlSale.setRecipientName(cust.getFirstName() + " " + cust.getLastName());
            xmlSale.setRecipientPhoneNumber(cust.getAlternativeContact1());
            xmlSale.setOrganisationName(org == null ? "" : org.getOrganisationName());
            xmlSale.setOrganisationChannel(org == null ? "" : org.getChannelCode());

            com.smilecoms.pos.db.model.Sale dbSale;
            if (X3Helper.isSaleGeneratedFromQuote(xmlSale.getExtraInfo()) || isSaleGeneratedFromShopPickup(xmlSale.getExtraInfo())) {
                dbSale = convertQuoteToSale(xmlSale, emRL);
            } else {
                dbSale = writeSale(xmlSale, heldByOrganisationId, emRL);
            }

            xmlSale = getSale(dbSale.getSaleId(), emRL);

            // Store the invoice
            if (!xmlSale.getPaymentMethod().equals(PAYMENT_METHOD_PAYMENT_GATEWAY)
                    && !xmlSale.getPaymentMethod().equals(PAYMENT_METHOD_AIRTIME)
                    && !xmlSale.getPaymentMethod().equals(PAYMENT_METHOD_DELIVERY_SERVICE)
                    && !xmlSale.getPaymentMethod().equals(PAYMENT_METHOD_SHOP_PICKUP)
                    && !xmlSale.getPaymentMethod().equals(PAYMENT_METHOD_CONTRACT)
                    && !isDirectAirtimeSale(xmlSale.getRecipientAccountId())) {
                List <SaleRow> saleRows = DAO.getSaleRowsBySaleId(emRL, xmlSale.getSaleId());
                String xml;
                byte[] pdf;                
                xml = getInvoiceXML(xmlSale, cust, org, salesPerson,saleRows);
                pdf = generateInvoice(xml);
                xmlSale.setInvoicePDFBase64(Utils.encodeBase64(pdf));
                dbSale.setInvoicePDF(pdf);
                byte[] pdfSmall = generateSmallInvoice(xml);
                xmlSale.setSmallInvoicePDFBase64(Utils.encodeBase64(pdfSmall));
                dbSale.setSmallInvoicePDF(pdfSmall);
            }

            if (xmlSale.getPromotionCode() != null && xmlSale.getPromotionCode().startsWith("Request Approval: ")) {
                try {
                    PromotionCodeApproval approval = getApproval(xmlSale, emRL);
                    log.debug("Approval sale id is [{}]", approval.getSaleId());
                    if (approval.getSaleId() != null) {
                        throw new Exception("Approval hash is already used on another sale -- " + approval.getSaleId());
                    }
                    approval.setSaleId(xmlSale.getSaleId());
                    log.debug("Updating approval with the sale id");
                    emRL.persist(approval);
                } catch (javax.persistence.NoResultException e) {
                    log.debug("Error getting approval for promo code. Approval not found");
                    sendApprovalRequest(xmlSale, cust, org, salesPerson, comment);
                    throw new Exception("Use of promotion code is not approved -- " + xmlSale.getPromotionCode());
                }
            }

            dbSale.setLastModified(new Date());
            emRL.persist(dbSale);
            emRL.flush();

            if (xmlSale.getPaymentMethod().equals(PAYMENT_METHOD_AIRTIME)) {
                // Reserve the funds
                reserveForPayByAirtime(xmlSale);
            }

            if (!xmlSale.getPaymentMethod().equals(PAYMENT_METHOD_CONTRACT)) {
                SalePostProcessingData salePostProcessingData = new SalePostProcessingData();
                salePostProcessingData.setAccountId(dbSale.getRecipientAccountId());
                salePostProcessingData.setSaleId(dbSale.getSaleId());
                // Allow certain replacement kits to auto-provision data bundles.
                for (SaleLine saleLine : xmlSale.getSaleLines()) {
                    if (saleLine.getInventoryItem().getItemNumber().startsWith("KIT")) {
                        if (BaseUtils.getPropertyAsList("env.pos.kits.allowed.to.autoprovision.bundle").contains(saleLine.getInventoryItem().getItemNumber())) {
                            int productInstanceId = getProductInstanceIdForAccountId(xmlSale.getRecipientAccountId());
                            salePostProcessingData.setProductInstanceId(productInstanceId);
                            log.debug("All bundles under KIT [{}] of sale [{}] will be provisioned under product instance [{}]",
                                    new Object[]{saleLine.getInventoryItem().getItemNumber(),
                                        xmlSale.getSaleId(),
                                        productInstanceId
                                    });
                        }
                    }
                }
                enrichAndPostProcessSale(emRL, salePostProcessingData, postProcessingResult, true);
            }

            if (xmlSale.getPaymentMethod().equals(PAYMENT_METHOD_AIRTIME)) {
                log.debug("This sale is paid for by airtime so we can now remove the airtime from the paying account");
                payByAirtime(xmlSale, postProcessingResult);
            }

            if (xmlSale.getPaymentMethod().equals(PAYMENT_METHOD_DELIVERY_SERVICE)) {
                sendDeliveryNote(xmlSale, inventorySystem);
            }

            boolean committedTX = false;
            try {
                committedTX = JPAUtils.commitTransaction(emRL);
            } catch (Exception e) {
                log.warn("Error committing transaction:", e);
            }

            if (!committedTX) {
                rollbackPostProcessing(postProcessingResult);
                throw new Exception("Error committing sales transaction");
            }

            if (dbSale.getInvoicePDF() != null) {
                sendInvoice(xmlSale);
            }
            createEvent(xmlSale.getSaleId());
        } catch (Exception e) {
            rollbackPostProcessing(postProcessingResult);
            JPAUtils.rollbackTransaction(emRL);
            throw processError(POSError.class, e);
        } finally {
            if (inventorySystem != null) {
                inventorySystem.close();
            }
            if (log.isDebugEnabled()) {
                logEnd();
            }
            JPAUtils.closeEM(emRL);
        }
        return xmlSale;
    }

    /**
     * Return customer state for his address type.
     *
     *
     * @param customer
     * @param addressType
     * @return
     */
    private String getCustomerState(Customer customer, String addressType) {
        for (Address address : customer.getAddresses()) {
            if (address.getType().equalsIgnoreCase(addressType)) {
                return address.getState();
            }
        }
        return null;

    }

    private BigDecimal getSalesPersonOutstandingUCAndAirtimeSalesCents(EntityManager em, int customerProfileId) throws Exception {
        log.debug("Getting sales persons outstanding unit credit and airtime sales");
        BigDecimal ret = BigDecimal.ZERO;
        Collection<com.smilecoms.pos.db.model.Sale> sales = DAO.getNonCashedInPaidCashOrCardPaymentSales(em, customerProfileId);
        //sales.addAll(DAO.getSalesOnPendingBankDepositCashIn(em, customerProfileId));
        for (com.smilecoms.pos.db.model.Sale dbSale : sales) {
            Sale xmlSale = getXMLSale(em, dbSale, "LINES");
            for (SaleLine line : xmlSale.getSaleLines()) {
                if (isUnitCredit(line.getInventoryItem())) {
                    ret = ret.add(new BigDecimal(line.getLineTotalCentsIncl()));
                } else if (isAirtime(line.getInventoryItem())) {
                    if (line.getProvisioningData() == null || !line.getProvisioningData().contains("PostProcessed=true")) {
                        // Only look at airtime sales that have not been post processed and hence already take off the salespersons account
                        ret = ret.add(new BigDecimal(line.getLineTotalCentsIncl()));
                    }
                }
            }
        }
        log.debug("Got sales persons outstanding unit credit and airtime sales [{}]c", ret);
        return ret;
    }

    @Override
    public SalesList getSales(SalesQuery salesQuery) throws POSError {
        setContext(salesQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        SalesList salesList = new SalesList();
        try {

            if (salesQuery.getVerbosity() == null || salesQuery.getVerbosity().isEmpty()) {
                salesQuery.setVerbosity("SALE_LINES");
            }

            if (salesQuery.getSerialNumber() != null && !salesQuery.getSerialNumber().trim().isEmpty()) {
                log.debug("Query is for a sale including serial number [{}]", salesQuery.getSerialNumber());
                Collection<com.smilecoms.pos.db.model.Sale> sales = DAO.getSalesBySerialNumber(emJTA, salesQuery.getSerialNumber());
                for (com.smilecoms.pos.db.model.Sale dbSale : sales) {
                    Sale xmlSale = getXMLSale(emJTA, dbSale, salesQuery.getVerbosity());
                    salesList.getSales().add(xmlSale);
                }
            } else if (salesQuery.getPurchaseOrderData() != null && !salesQuery.getPurchaseOrderData().isEmpty()) {
                log.debug("Query is for sales by purchase order number");
                Collection<com.smilecoms.pos.db.model.Sale> sales = DAO.getSalesByPurchaseOrderData(emJTA, salesQuery.getPurchaseOrderData());
                for (com.smilecoms.pos.db.model.Sale dbSale : sales) {
                    Sale xmlSale = getXMLSale(emJTA, dbSale, salesQuery.getVerbosity());
                    salesList.getSales().add(xmlSale);
                }
            } else if (salesQuery.getStatus() != null && !salesQuery.getStatus().isEmpty()) { //search all sales using sale status
                log.debug("Query is for all sales with status [{}]", salesQuery.getStatus());
                Collection<com.smilecoms.pos.db.model.Sale> sales = DAO.getSalesByStatus(emJTA, salesQuery.getStatus(), Utils.getJavaDate(salesQuery.getDateFrom()), Utils.getJavaDate(salesQuery.getDateTo()));
                for (com.smilecoms.pos.db.model.Sale dbSale : sales) {
                    Sale xmlSale = getXMLSale(emJTA, dbSale, salesQuery.getVerbosity());
                    salesList.getSales().add(xmlSale);
                }
            } else if (salesQuery.getRecipientCustomerId() > 0) {
                log.debug("Query is for a customers sales");
                Collection<com.smilecoms.pos.db.model.Sale> sales = DAO.getSalesForCustomer(emJTA, salesQuery.getRecipientCustomerId(), Utils.getJavaDate(salesQuery.getDateFrom()), Utils.getJavaDate(salesQuery.getDateTo()));
                for (com.smilecoms.pos.db.model.Sale dbSale : sales) {
                    Sale xmlSale = getXMLSale(emJTA, dbSale, salesQuery.getVerbosity());
                    salesList.getSales().add(xmlSale);
                }
            } else if (salesQuery.getContractId() > 0) {
                log.debug("Query is for a contracts sales");
                Collection<com.smilecoms.pos.db.model.Sale> sales = DAO.getSalesForContract(emJTA, salesQuery.getContractId(), Utils.getJavaDate(salesQuery.getDateFrom()), Utils.getJavaDate(salesQuery.getDateTo()));
                for (com.smilecoms.pos.db.model.Sale dbSale : sales) {
                    Sale xmlSale = getXMLSale(emJTA, dbSale, salesQuery.getVerbosity());
                    salesList.getSales().add(xmlSale);
                }
            } else if (salesQuery.getSaleLineId() > 0) {
                log.debug("Query is for a sale line id");
                com.smilecoms.pos.db.model.Sale dbSale = DAO.getSaleByLineId(emJTA, salesQuery.getSaleLineId());
                Sale xmlSale = getXMLSale(emJTA, dbSale, salesQuery.getVerbosity());
                salesList.getSales().add(xmlSale);
            } else if (salesQuery.getSalesIds().isEmpty()) {
                log.debug("Query is for all PD cash or Card Payment sales not cashed in");
                Collection<com.smilecoms.pos.db.model.Sale> sales = DAO.getNonCashedInPaidCashOrCardPaymentSales(emJTA, salesQuery.getSalesPersonCustomerId());
                for (com.smilecoms.pos.db.model.Sale dbSale : sales) {
                    boolean canSkip = false;
                    try {
                        CashIn cashInBySaleId = DAO.getCashInBySaleId(emJTA, dbSale.getSaleId());
                        if (cashInBySaleId != null && cashInBySaleId.getStatus().equals("BDP")) {
                            canSkip = true;
                        }
                    } catch (Exception ex) {
                    }

                    if (canSkip) {
                        continue;
                    }

                    Sale xmlSale = getXMLSale(emJTA, dbSale, salesQuery.getVerbosity());
                    salesList.getSales().add(xmlSale);
                }
            } else {
                log.debug("Query is for a list of sales id's");
                for (int id : salesQuery.getSalesIds()) {
                    try {
                        com.smilecoms.pos.db.model.Sale dbSale = DAO.getSale(emJTA, id);
                        Sale xmlSale = getXMLSale(emJTA, dbSale, salesQuery.getVerbosity());
                        salesList.getSales().add(xmlSale);
                    } catch (javax.persistence.NoResultException e) {
                        log.debug("No sale found for sale id [{}]", id);
                    }
                }
            }
            salesList.setNumberOfSales(salesList.getSales().size());
        } catch (Exception e) {
            throw processError(POSError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return salesList;
    }

    @Override
    public CashInData processCashIn(CashInData cashInData) throws POSError {
        setContext(cashInData, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }

        CashInData cashedInData;
        try {
            switch (cashInData.getCashInType()) {
                case "office":
                    cashInData.setStatus("OFC"); // Office Complete
                    break;
                case "bankdeposit":
                    /*
                     * These will sit as pending and not go to X3 until a payment comes through into the bank and the X3InterfaceDaemon sees this (ci... reference number)
                     * When the payment is seen then the cashin will change to BDC, airtime transfers are done and then the cashin GL will be sent by X3InterfaceDaemon
                     */
                    // cashInData.
                    if (cashInData.getStatus() == null || cashInData.getStatus().trim().isEmpty()) {// This is a new bankdeposit cashin
                        cashInData.setStatus("BDP"); // Bank Deposit Pending
                        cashInData.setCashReceiptedInCents(0);
                        // cashInData.setSalesAdministratorCustomerId(0);
                    } else if (cashInData.getStatus().equals("BDP")) {// This is the second leg of the bankdeposit cashin, set status to BDC (Bank Deposit Completed)
                        EntityManager em = JPAUtils.getEM(emfRL);
                        try {
                            log.debug("Starting transaction on entity manager for bank deposit cash-in");
                            JPAUtils.beginTransaction(em);
                            log.debug("This is a cash-in bank deposit with reference [{}]", cashInData.getCashInId());
                            String extId = cashInData.getExtTxId() == null ? "" : cashInData.getExtTxId();
                            processAirtimeTransfersAfterCashInDeposit(em, cashInData.getCashInId(), cashInData.getCashRequiredInCents(), extId);
                            log.debug("Committing transaction for bank deposit cash-in");
                            JPAUtils.commitTransaction(em);

                            cashInData.setStatus("BDC");

                        } catch (Exception e) {
                            JPAUtils.rollbackTransaction(em);
                            log.debug("Exception processing bank deposit cash-in. Transaction has been rolled back: [{}]", e.toString());
                        }
                    }
                    break;
                default:
                    throw new Exception("Invalid cash in type");
            }

            // If bankdeposit cashin and status = 'BDC'
            log.debug("Processing cash-in with id {}, the cash in type is {} and status is {}", new Object[]{cashInData.getCashInId(), cashInData.getCashInType(), cashInData.getStatus()});

            if (cashInData.getCashInType().equals("bankdeposit") && cashInData.getStatus().equals("BDC")) { // Bank deposit completed
                // Only update the cashin status to 'BDC'  so that it gets picked-up and sent to X3
                com.smilecoms.pos.db.model.CashIn cashIn = DAO.modifyCashIn(emJTA, cashInData);
                cashedInData = getXMLCashInData(emJTA, cashIn);
            } else { // It's a new cashin - handle as normal.   

                com.smilecoms.pos.db.model.CashIn cashIn = DAO.createCashIn(emJTA, cashInData.getCashReceiptedInCents(),
                        cashInData.getCashRequiredInCents(), cashInData.getSalesAdministratorCustomerId(),
                        cashInData.getSalesPersonCustomerId(), cashInData.getPlatformContext().getTxId(),
                        cashInData.getStatus(), cashInData.getCashInType());
                for (int saleId : cashInData.getSalesIds()) {
                    com.smilecoms.pos.db.model.Sale dbSale = DAO.getSale(emJTA, saleId);
                    if ((!dbSale.getPaymentMethod().equals(PAYMENT_METHOD_CASH)
                            && !dbSale.getPaymentMethod().equals(PAYMENT_METHOD_CARD_PAYMENT))
                            || !dbSale.getStatus().equals(PAYMENT_STATUS_PAID)) {
                        throw new Exception("Only cash sales in status PD can be cashed in");
                    }
                    DAO.createCashInRow(emJTA, cashIn.getCashInId(), saleId);
                }
                cashedInData = getXMLCashInData(emJTA, cashIn);
                processAirtimeTransfersAfterCashIn(emJTA, cashedInData);
                createEvent(cashInData.getSalesPersonCustomerId());
            }
        } catch (Exception e) {
            JPAUtils.setRollbackOnly();
            throw processError(POSError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return cashedInData;
    }

    @Override
    public Done modifySale(SaleModificationData saleModificationData) throws POSError {
        setContext(saleModificationData, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        InventorySystem inventorySystem = null;
        try {
            inventorySystem = InventorySystemManager.getInventorySystem();
            if (saleModificationData.getNewPaymentStatus() != null
                    && (saleModificationData.getNewPaymentStatus().equals(PAYMENT_STATUS_PENDING_VERIFICATION) || saleModificationData.getNewPaymentStatus().equals(PAYMENT_STATUS_PAID))) {
                log.debug("This modification is to indicate that a payment has been made against the sale");
                List<Integer> saleList = new ArrayList();
                saleList.add(saleModificationData.getSaleId());
                processPayment(emJTA, saleList, saleModificationData.getPaymentInCents(), saleModificationData.getPaymentTransactionData(), saleModificationData.getNewPaymentStatus(), false, "");
            } else if (saleModificationData.getNewPaymentStatus() != null && saleModificationData.getNewPaymentStatus().equals(PAYMENT_STATUS_INVOICE_REVERSAL)) {
                log.debug("This modification is to indicate that the sale has been reversed (cancelled)");
                reverseSale(emJTA, saleModificationData.getSaleId(), saleModificationData.getPlatformContext());
            } else if (saleModificationData.getNewPaymentStatus() != null && saleModificationData.getNewPaymentStatus().equals(PAYMENT_STATUS_STAFF_OR_LOAN_COMPLETE)) {
                log.debug("This modification is to indicate that the sale was a loan and has been returned");
                processLoanCompletion(saleModificationData.getSaleId(), emJTA);
            } else if (saleModificationData.getNewPaymentStatus() != null && saleModificationData.getNewPaymentStatus().equals(PAYMENT_STATUS_RESEND_SALE_TO_X3)) {
                log.debug("This modification is to indicate that the sale must be resent to X3");
                resendSaleToX3(emJTA, saleModificationData.getSaleId());
            } else if (saleModificationData.getNewPaymentStatus() != null && saleModificationData.getNewPaymentStatus().equals(PAYMENT_STATUS_RESEND_CASHIN_TO_X3)) {
                log.debug("This modification is to indicate that the sales cashin must be resent to X3");
                resendCashInToX3(emJTA, saleModificationData.getSaleId());
            } else if (saleModificationData.getNewPaymentStatus() != null && saleModificationData.getNewPaymentStatus().equals(PAYMENT_STATUS_REGENERATE)) {
                log.debug("This modification is to indicate that the sales invoice must be regenerated");
                String paymentMethod = getSale(saleModificationData.getSaleId(), emJTA).getPaymentMethod();
                if (!paymentMethod.equals(PAYMENT_METHOD_PAYMENT_GATEWAY)
                        && !paymentMethod.equals(PAYMENT_METHOD_AIRTIME)
                        && !paymentMethod.equals(PAYMENT_METHOD_DELIVERY_SERVICE)
                        && !paymentMethod.equals(PAYMENT_METHOD_SHOP_PICKUP)
                        && !paymentMethod.equals(PAYMENT_METHOD_CONTRACT)) {
                    regenerateAndStoreInvoice(saleModificationData.getSaleId(), emJTA);
                } else {
                    log.debug("Cannot regenerate an invoice for the payment method");
                }
            } else if (saleModificationData.getNewPaymentStatus() != null && saleModificationData.getNewPaymentStatus().equals(PAYMENT_STATUS_PENDING_PAYMENT)) {
                log.debug("This modification is to indicate that the sales invoice must be retried if its a payment gateway transaction");
                putBackInGatewayQueue(saleModificationData.getSaleId(), emJTA);
            } else if (saleModificationData.getPaymentTransactionData() != null && !saleModificationData.getPaymentTransactionData().isEmpty()) {
                log.debug("This sales [{}] payment transaction data has changed to [{}]", saleModificationData.getSaleId(), saleModificationData.getPaymentTransactionData());
                modifyPaymentTransactionData(saleModificationData.getSaleId(), saleModificationData.getPaymentTransactionData(), emJTA);
            } else if (saleModificationData.getDeliveryCustomerId() > 0) {
                log.debug("This sale must be delivered to customer id [{}]", saleModificationData.getDeliveryCustomerId());
                sendDeliveryNote(saleModificationData.getSaleId(), saleModificationData.getDeliveryCustomerId(), emJTA, inventorySystem);
            }

            if (saleModificationData.getTillId() != null && !saleModificationData.getTillId().isEmpty()) {
                log.debug("This sales [{}] till id has changed to [{}]", saleModificationData.getSaleId(), saleModificationData.getTillId());
                modifyTillId(saleModificationData.getSaleId(), saleModificationData.getTillId(), emJTA);
            }

            createEvent(saleModificationData.getSaleId());
        } catch (Exception e) {
            JPAUtils.setRollbackOnly();
            throw processError(POSError.class, e);
        } finally {
            if (inventorySystem != null) {
                inventorySystem.close();
            }
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public CreditNoteList getCreditNotes(CreditNoteQuery creditNoteQuery) throws POSError {
        setContext(creditNoteQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        CreditNoteList ret = new CreditNoteList();
        try {
            SaleReturn dbReturn = DAO.getReturn(emJTA, creditNoteQuery.getCreditNoteId());
            CreditNote creditNote = new CreditNote();
            creditNote.setCreditNoteId(dbReturn.getSaleReturnId());
            creditNote.setDescription(dbReturn.getDescription());
            creditNote.setReasonCode(dbReturn.getReasonCode());
            creditNote.setSalesPersonCustomerId(dbReturn.getSalesPersonCustomerId());
            creditNote.setCreditNotePDFBase64(Utils.encodeBase64(dbReturn.getSaleReturnPDF()));
            creditNote.setSaleId(dbReturn.getSaleId());
            ret.getCreditNotes().add(creditNote);
            ret.setNumberOfCreditNotes(ret.getCreditNotes().size());
        } catch (Exception e) {
            throw processError(POSError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return ret;
    }

    @Override
    public Done setSoldStockLocations(SoldStockLocationData soldStockLocationData) throws POSError {
        setContext(soldStockLocationData, wsctx);
        if (log.isDebugEnabled()) { 
            logStart();
        }
        try {

            for (SoldStockLocation location : soldStockLocationData.getSoldStockLocations()) {
                if (location.getHeldByOrganisationId() <= 0) {
                    continue;
                }                
                
                if (location.isUsedAsReplacement()) {
                    
                    if(isSuperDealer(location.getSoldToOrganisationId())) {
                        DAO.moveSoldStockUsedAsReplacementFromHeldBy(emJTA, location.getItemNumber(), location.getSerialNumber(), location.getHeldByOrganisationId(), location.getSoldToOrganisationId());
                    }else {
                        DAO.setSoldStockUsedAsReplacement(emJTA, location.getItemNumber(), location.getSerialNumber(), location.getHeldByOrganisationId(), location.getSoldToOrganisationId());
                    }
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        log.debug("Item Number [{}] Serial Number [{}] has been used as a replacement", location.getItemNumber(), location.getSerialNumber());
                    
                } else {
                    if(isSuperDealer(location.getSoldToOrganisationId())) {
                        log.debug("Setting sold stock location data [{}][{}][{}]", new Object[]{location.getItemNumber(), location.getSerialNumber(), location.getHeldByOrganisationId()});
                        DAO.moveSoldStockLocationFromHeldBy(emJTA, location.getItemNumber(), location.getSerialNumber(), location.getHeldByOrganisationId(), location.getSoldToOrganisationId());
                    } else {                        
                        log.debug("Setting sold stock location data [{}][{}][{}]", new Object[]{location.getItemNumber(), location.getSerialNumber(), location.getHeldByOrganisationId()});
                        DAO.setSoldStockLocation(emJTA, location.getItemNumber(), location.getSerialNumber(), location.getHeldByOrganisationId(), location.getSoldToOrganisationId());
                    }
                }
            }

        } catch (Exception e) {
            throw processError(POSError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public SoldStockLocationList getSoldStockLocations(SoldStockLocationQuery soldStockLocationQuery) throws POSError {
        setContext(soldStockLocationQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        SoldStockLocationList locations = new SoldStockLocationList();
        List<SaleRow> saleRows;
        try {
            if (soldStockLocationQuery.getSerialNumber() != null && !soldStockLocationQuery.getSerialNumber().isEmpty()) {
                log.debug("Sold stock query is for stock by serialNumber [{}]", soldStockLocationQuery.getSerialNumber());
                saleRows = DAO.getSoldStockLocationsBySerialNumber(emJTA, soldStockLocationQuery.getSerialNumber());
            } else if (soldStockLocationQuery.getSoldToOrganisationId() != null && soldStockLocationQuery.getSoldToOrganisationId() > 0) {
                log.debug("Sold stock query is for stock SoldTo org id [{}], held By [{}]", soldStockLocationQuery.getSoldToOrganisationId(), soldStockLocationQuery.getHeldByOrganisationId());
                saleRows = DAO.getSoldStockLocationsBySoldTo(emJTA, soldStockLocationQuery.getSoldToOrganisationId(), soldStockLocationQuery.getHeldByOrganisationId());
            } else if (soldStockLocationQuery.getHeldByOrganisationId() != null && soldStockLocationQuery.getHeldByOrganisationId() > 0) {
                log.debug("Sold stock query is for stock held by org id [{}]", soldStockLocationQuery.getHeldByOrganisationId());
                saleRows = DAO.getSoldStockLocationsByHeldBy(emJTA, soldStockLocationQuery.getHeldByOrganisationId());
            } else {
                throw new Exception("Invalid query data");
            }
            log.debug("Finished getting data from sale_row table");
            for (SaleRow row : saleRows) {
                log.debug("Getting sale_row in the loop: {}", row.getItemNumber());
                // Make sure the sale row is from a sale sold to a super dealer. 
                int soldToOrgId = DAO.getSaleByLineId(emJTA, row.getSaleRowId()).getRecipientOrganisationId();
                if (!(isMegaDealer(soldToOrgId) || isSuperDealer(soldToOrgId) || isFranchise(soldToOrgId))) {
                    log.debug("This sale row is not in a sale to a mega/super dealer [{}]. Skipping", row.getSaleRowId());
                    continue;
                }
                if (isUnitCredit(row)) {
                    log.debug("This is a unit credit");
                    continue;
                }

                locations.getSoldStockLocations().add(getXMLSoldStockLocation(emJTA, row));
            }
            locations.setNumberOfSoldStockLocations(locations.getSoldStockLocations().size());
            log.debug("Returning [{}] items", locations.getNumberOfSoldStockLocations());
        } catch (Exception e) {
            throw processError(POSError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return locations;
    }

    @Override
    public Done approvePromotionCode(PromotionCodeApprovalData promotionCodeApprovalData) throws POSError {
        setContext(promotionCodeApprovalData, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        try {

            PromotionCodeApproval approval = new PromotionCodeApproval();
            approval.setApprovalCustomerId(promotionCodeApprovalData.getCustomerId());
            approval.setApprovalHash(promotionCodeApprovalData.getHash());
            approval.setApprovalDateTime(new Date());
            emJTA.persist(approval);
            emJTA.flush();

            Customer sp = getCustomer(promotionCodeApprovalData.getSalesPersonCustomerId());
            Customer ap = getCustomer(promotionCodeApprovalData.getCustomerId());
            String body = "Hi " + sp.getFirstName() + ",<br/><br/>I have approved your sale with approval code starting with " + approval.getApprovalHash()
                    + ". You can now go ahead and make the sale. Make sure its identical to the sale I approved or else you will end up sending another approval request.<br/><br/>Regards,<br/><br/>"
                    + ap.getFirstName() + " " + ap.getLastName();
            IMAPUtils.sendEmail(ap.getEmailAddress(), sp.getEmailAddress(), null, null, "Promotion Code Approved", body);
            createEvent(approval.getApprovalCustomerId());
        } catch (Exception e) {
            throw processError(POSError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public CreditNote processReturn(ReturnData returnData) throws POSError {
        setContext(returnData, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        CreditNote creditNote = new CreditNote();
        InventorySystem inventorySystem = null;
        try {
            Sale sale = getSale(returnData.getSaleId(), emJTA);
            if (!sale.getPaymentMethod().equals(PAYMENT_METHOD_CASH)
                    && !sale.getPaymentMethod().equals(PAYMENT_METHOD_BANK_TRANSFER)
                    && !sale.getPaymentMethod().equals(PAYMENT_METHOD_CARD_PAYMENT)
                    && !sale.getPaymentMethod().equals(PAYMENT_METHOD_PAYMENT_GATEWAY)
                    && !sale.getPaymentMethod().equals(PAYMENT_METHOD_CHEQUE)
                    && !sale.getPaymentMethod().equals(PAYMENT_METHOD_CREDIT_NOTE) // PCB Added this 2013/10/24... why not allow it ;-)
                    && !sale.getPaymentMethod().equals(PAYMENT_METHOD_CREDIT_ACCOUNT)
                    && !sale.getPaymentMethod().equals(PAYMENT_METHOD_CARD_INTEGRATION)
                    && !sale.getPaymentMethod().equals(PAYMENT_METHOD_CREDIT_FACILITY)) {
                throw new Exception("Cannot do a return on a sale with this payment type");
            }

            inventorySystem = InventorySystemManager.getInventorySystem();
            if (!inventorySystem.doesLocationExist(returnData.getReturnLocation())) {
                throw new Exception("Invalid location to return the stock back to -- " + returnData.getReturnLocation());
            }

            Date saleDate = Utils.getJavaDate(sale.getSaleDate());
            Calendar X3StartDate = Calendar.getInstance();
            X3StartDate.set(2013, 5, 26, 0, 0, 0); // Month is 0 base
            if (saleDate.before(X3StartDate.getTime())) {
                throw new Exception("Cannot process returns on sales made prior to 26 June 2013 -- " + X3StartDate + " is before " + saleDate);
            }

            if (returnData.getLineItems().isEmpty()) {
                throw new Exception("Nothing has been included in the return");
            }
            if (!sale.getStatus().equals(PAYMENT_STATUS_PAID)) {
                throw new Exception("Cannot do a return on a sale which is not paid");
            }

            List<SaleRow> salesRowsBeingReturned = new ArrayList<>();
            Map<Integer, Object[]> p2pData = new HashMap<>();
            SaleReturn dbReturn = DAO.createReturn(emJTA, returnData.getSaleId(), returnData.getSalesPersonCustomerId(), returnData.getReasonCode(), returnData.getDescription(), returnData.getReturnLocation());

            List<LineItem> lineItems = returnData.getLineItems();
            for (LineItem line : lineItems) {
                int lineId = line.getLineId();
                Object[] rowP2PData = null;
                SaleRow row = DAO.getSaleRowBySaleRowId(emJTA, lineId);
                if (isAirtime(row)) {
                    throw new Exception("Airtime cannot be returned");
                }

                boolean isRefurbishedForLowUtilizationSites = false;
                // Check it it is a refurbished item.
                if (BaseUtils.getPropertyAsList("env.allowed.refurbished.devices.for.lowutilizationsites").contains(row.getSerialNumber())) {
                    isRefurbishedForLowUtilizationSites = true;
                }

                boolean isArefurbishedItem = false;
                // Check it it is a refurbished item.
                if (BaseUtils.getPropertyAsList("env.allowed.refurbished.devices.for.replacement").contains(row.getSerialNumber())) {
                    isArefurbishedItem = true;
                }

                // In case a refurbished device is returned as been faulty, the RSM should be the only person to return the faulty refurbished Mifi device back to the system.
                Customer cust = SCAWrapper.getAdminInstance().getCustomer(returnData.getSalesPersonCustomerId(), com.smilecoms.commons.sca.StCustomerLookupVerbosity.CUSTOMER);
                int roleCnt = cust.getSecurityGroups().size();

                boolean isAllowedToReturnRefurbshedItems = false;
                for (int i = 0; i < roleCnt; i++) {
                    String role = (String) cust.getSecurityGroups().get(i);
                    if (BaseUtils.getPropertyAsList("env.roles.allowed.to.return.refurbished.devices").contains(role)) {
                        isAllowedToReturnRefurbshedItems = true;
                        break;
                    }
                }

                if (isArefurbishedItem && !isAllowedToReturnRefurbshedItems) {
                    throw new Exception("You are not allowed to return a refurbished item -- serial number " + row.getSerialNumber());
                }

                if (isRefurbishedForLowUtilizationSites) {
                    throw new Exception("You are not allowed to return a refurbished item for low utilisation sites -- serial number " + row.getSerialNumber());
                }

                // Check if line item was used on KIT returns processes
                if (DAO.checkIfItemUsedAsKitReplacement(emJTA, row.getSerialNumber())) {
                    throw new Exception("Serial number has been used on a manual KIT replacement -- serial number (" + row.getSerialNumber() + ")");
                }

                if ((isUnitCredit(row) && !isP2PProduct(row)) && !returnData.getPlatformContext().getOriginatingIdentity().equals("admin")) {
                    throw new Exception("Unit Credit cannot be returned");
                }
                // Check warranties ...
                Customer salesPerson = getCustomer(returnData.getSalesPersonCustomerId());

                Calendar warrantyEndDate = Calendar.getInstance();

                if (BaseUtils.getBooleanProperty("env.pos.warranty.checks.enabled", false)) {
                    if (!isWarrantyStillValid(emJTA, sale, lineId, warrantyEndDate)
                            && (!Utils.isAnyOfUserRolesIncludedInRolesPropertyList(salesPerson.getSecurityGroups(), "env.pos.replacement.warranty.expired.allowed.roles")
                            && !returnData.getPlatformContext().getOriginatingIdentity().equals("admin"))) {
                        throw new Exception("A return is not allowed on items with expired warranty -- Warranty for item " + row.getSerialNumber() + " expired on " + warrantyEndDate.getTime());
                    }
                }
                if (isP2PProduct(row) && !sale.getPaymentMethod().equals(PAYMENT_METHOD_CREDIT_ACCOUNT)) {
                    throw new Exception("Only sales with credit account payment method allowed to be returned");
                }

                boolean isP2POn = isP2PCalendarInvoicingOn(row);
                if (isP2POn) {
                    log.debug("Applying P2P calendar invoicing configs");
                    if (row.getProvisioningData() != null && row.getProvisioningData().contains("P2PInvoicingPeriod")) {
                        String dateAsString = Utils.getValueFromCRDelimitedAVPString(row.getProvisioningData(), "P2PInvoicingPeriod");
                        Date invcPeriod = Utils.getStringAsDate(dateAsString, "yyyy/MM/dd");

                        int daysInMonth = Utils.getMaxDaysInMonth(invcPeriod);
                        String p2pExpiry = Utils.getValueFromCRDelimitedAVPString(row.getProvisioningData(), "P2PExpiryDate");
                        log.debug("P2P Configs :: P2PInvoicingPeriod={};P2PExpiryDate={};DaysInMonth={};ReturnedQuantity={}", new Object[]{dateAsString, p2pExpiry, daysInMonth, line.getQuantity()});
                        if (line.getQuantity() == null
                                || line.getQuantity() <= 0
                                || line.getQuantity() > daysInMonth
                                || line.getQuantity() > row.getQuantity()) {
                            throw new Exception("Quantity captured for P2P calendar invoicing return is not valid");
                        }
                    }

                    DAO.createReturnRow(emJTA, dbReturn.getSaleReturnId(), row.getSaleRowId(), line.getQuantity());
                    rowP2PData = new Object[3];
                    rowP2PData[0] = row.getUnitPriceCentsIncl().multiply(new BigDecimal(line.getQuantity()));
                    rowP2PData[1] = line.getQuantity();
                    log.debug("Row quantity is [{}] and returned quantity is [{}], total disc incl [{}]", row.getQuantity(), line.getQuantity(), new Object[]{row.getTotalDiscountOnInclCents()});
                    BigDecimal discInclPerRowQuantity = row.getTotalDiscountOnInclCents().divide(new BigDecimal(row.getQuantity()), RoundingMode.HALF_UP);
                    log.debug("Calculated discount per row quantity is [{}]", discInclPerRowQuantity.doubleValue());
                    BigDecimal discForQuantityReturned = discInclPerRowQuantity.multiply(new BigDecimal(line.getQuantity()));
                    log.debug("Total calculated discount for returned quantity is [{}]", discForQuantityReturned.doubleValue());
                    rowP2PData[2] = discForQuantityReturned;

                } else {
                    DAO.createReturnRow(emJTA, dbReturn.getSaleReturnId(), row.getSaleRowId(), row.getQuantity());
                }

                if (rowP2PData != null) {
                    p2pData.put(row.getSaleRowId(), rowP2PData);
                }
                salesRowsBeingReturned.add(row);
            }

            generateAndPersistCreditNote(sale, salesRowsBeingReturned, dbReturn, emJTA, p2pData);
            sendCreditNote(sale, salesRowsBeingReturned, dbReturn);
            creditNote.setCreditNoteId(dbReturn.getSaleReturnId());
            creditNote.setDescription(returnData.getDescription());
            creditNote.setReasonCode(returnData.getReasonCode());
            creditNote.setSalesPersonCustomerId(returnData.getSalesPersonCustomerId());
            creditNote.setCreditNotePDFBase64(Utils.encodeBase64(dbReturn.getSaleReturnPDF()));
            creditNote.setSaleId(returnData.getSaleId());
            createEvent(returnData.getSaleId());
        } catch (Exception e) {
            throw processError(POSError.class, e);
        } finally {
            if (inventorySystem != null) {
                inventorySystem.close();
            }
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return creditNote;
    }

    // HELPERS
    private void doDiscounting(Sale sale, EntityManager emLocal) throws Exception {
        log.debug("In doDiscounting with sale: [{}]", sale.getSaleDate());
        if (isDirectAirtimeSale(sale.getRecipientAccountId())) {
            sale.setPromotionCode("5% Off Airtime");
        }
        DiscountEngine de = new DiscountEngine();
        de.doDiscounting(emLocal, sale);
    }

    private PromotionCodeApproval getApproval(Sale sale, EntityManager emLocal) {
        log.debug("Getting approval for the promotion code [{}]", sale.getPromotionCode());
        String hash = getSaleHash(sale);
        PromotionCodeApproval approval = DAO.getPromotionCodeApproval(emLocal, hash);
        log.debug("This promotion code was approved by [{}] on [{}]", approval.getApprovalCustomerId());
        return approval;
    }

    private void sendApprovalRequest(Sale sale, Customer cust, Organisation org, Customer salesPerson, String comment) throws Exception {
        sale.setSaleId(0);
        String xml = getInvoiceXML(sale, cust, org, salesPerson);
        byte[] invoice;
        invoice = generateDummyInvoice(xml);
        
        String hash = getSaleHash(sale);
        Customer accountManager = getCustomer(salesPerson.getAccountManagerCustomerProfileId());
        String body = "Hi " + accountManager.getFirstName() + ",<br/><br/>There is a request for you from " + salesPerson.getFirstName()
                + " to approve the use of promotion code \"" + sale.getPromotionCode().replace("Request Approval: ", "")
                + "\" on a sale as attached. The comment for the approval request is.<br/><br/><b>"
                + comment
                + "</b><br/><br/>"
                + "Please log in to SEP and approve using code: <br/><br/> " + hash
                + "_" + Codec.stringToEncryptedHexString(sale.getPromotionCode().replace("Request Approval: ", ""))
                + "_" + Codec.stringToEncryptedHexString(String.valueOf(salesPerson.getCustomerId()));
        IMAPUtils.sendEmail(salesPerson.getEmailAddress(),
                accountManager.getEmailAddress(),
                salesPerson.getEmailAddress(), null, "Promotion Code Approval Request", body, "sale.pdf",
                invoice);
    }

    private String getSaleHash(Sale sale) {
        StringBuilder stringToHash = new StringBuilder();
        stringToHash.append(sale.getSalesPersonCustomerId());
        stringToHash.append(sale.getChannel());
        stringToHash.append(sale.getCreditAccountNumber());
        stringToHash.append(sale.getPromotionCode());
        stringToHash.append(sale.getRecipientAccountId());
        stringToHash.append(sale.getRecipientCustomerId());
        stringToHash.append(sale.getSaleDate().getYear());
        stringToHash.append(sale.getSaleDate().getMonth());
        stringToHash.append(sale.getSaleDate().getDay());
        stringToHash.append(sale.getWarehouseId());
        stringToHash.append(sale.getSaleLines().size());
        stringToHash.append(sale.getSaleTotalCentsIncl());
        for (SaleLine l : sale.getSaleLines()) {
            stringToHash.append(l.getInventoryItem().getItemNumber());
            stringToHash.append(l.getInventoryItem().getSerialNumber());
            for (SaleLine sl : l.getSubSaleLines()) {
                stringToHash.append(sl.getInventoryItem().getItemNumber());
                stringToHash.append(sl.getInventoryItem().getSerialNumber());
            }
        }
        String ret = Utils.oneWayHashImprovedHex(stringToHash.toString());
        log.debug("String to hash is [{}] and hash is [{}]", stringToHash, ret);
        return ret;
    }

    private void fillInItemDetails(InventorySystem inventorySystem, Sale sale, boolean expectSerialNumbers, EntityManager emLocal) throws Exception {
        log.debug("In fillInItemDetails for sale with [{}] lines", sale.getSaleLines().size());

        if (sale.getWarehouseId().startsWith("AUTO-")) {
            sale.setWarehouseId(getWarehouseForLocation(inventorySystem, sale.getWarehouseId().substring(5)));
        }

        //Remove the -EXTRA- unit credits if they are there
        // Remove elements smaller than 10 using 
        // Iterator.remove() 
        Iterator itr = sale.getSaleLines().iterator();
        while (itr.hasNext()) {
            SaleLine curLine = (SaleLine) itr.next();
            if (curLine.getInventoryItem().getSerialNumber().equals("-EXTRAUC-")
                    || curLine.getInventoryItem().getSerialNumber().equals("-BONUSUC-")
                    || curLine.getInventoryItem().getSerialNumber().equals("-COMBOUC-")) {
                itr.remove();
            }
        }

        List<SaleLine> additionalLines = new ArrayList<>();

        for (SaleLine line : sale.getSaleLines()) {
            boolean mustClone = false;
            if (line.getInventoryItem().getSerialNumber().equals("AUTO")) {
                mustClone = true;
            }
            for (SaleLine subLine : line.getSubSaleLines()) {
                if (subLine.getInventoryItem().getSerialNumber().equals("AUTO")) {
                    mustClone = true;
                }
            }
            if (mustClone) {
                long cloneCount = line.getQuantity() - 1;
                line.setQuantity(1);
                for (int i = 0; i < cloneCount; i++) {
                    log.debug("Making a clone of line [{}]", line.getLineNumber());
                    additionalLines.add(cloneSaleLine(line));
                }
            }

        }

        sale.getSaleLines().addAll(additionalLines);

        List<SaleLine> extraUCLines = new ArrayList<>();

        List<SaleLine> newSaleLines = new ArrayList<>();

        newSaleLines.addAll(sale.getSaleLines());

        int i = 0;

        for (SaleLine line : sale.getSaleLines()) {

            String itemNumber = line.getInventoryItem().getItemNumber();
            String serialNumber = line.getInventoryItem().getSerialNumber();
            if (serialNumber.equals("AUTO")) {
                log.debug("Item number [{}] needs an auto populated serial number", itemNumber);
                serialNumber = getAutoPopulateSerial(inventorySystem, emLocal, itemNumber, sale.getWarehouseId());
                line.getInventoryItem().setSerialNumber(serialNumber);
            }

            if (isNonX3Item(line.getInventoryItem())) {
                log.debug("This is an item which is not kept in X3 [{}]", serialNumber);
                double taxRate = BaseUtils.getDoubleProperty("env.sales.tax.percent");
                // Calculate the tax excl price for the caller
                line.getInventoryItem().setPriceInCentsExcl(line.getInventoryItem().getPriceInCentsIncl() / (1.0 + taxRate / 100.0));
                return;
            }

            log.debug("Filling in item details for Serial [{}] and Item Number [{}] and Quantity [{}]", new Object[]{serialNumber, itemNumber, line.getQuantity()});
            line.setInventoryItem(inventorySystem.getInventoryItem(emLocal, itemNumber, sale.getWarehouseId(), sale.getSalesPersonCustomerId(), sale.getRecipientAccountId(), sale.getTenderedCurrency()));
            line.getInventoryItem().setSerialNumber(serialNumber);

            if (isP2PCalendarInvoicingOn(line)) {
                if (sale.getExtraInfo() != null && sale.getExtraInfo().contains("P2PInvoicingPeriod")
                        && BaseUtils.getBooleanProperty("env.pos.p2p.daily.unitprice.calculation.enabled", true)) {
                    log.debug("Changing item pricing for p2p product");
                    String dateAsString = Utils.getValueFromCRDelimitedAVPString(sale.getExtraInfo(), "P2PInvoicingPeriod");
                    int daysInMonth = Utils.getMaxDaysInMonth(Utils.getStringAsDate(dateAsString, "yyyy/MM/dd"));
                    line.getInventoryItem().setPriceInCentsExcl(line.getInventoryItem().getPriceInCentsExcl() / daysInMonth);
                    line.getInventoryItem().setPriceInCentsIncl(line.getInventoryItem().getPriceInCentsIncl() / daysInMonth);
                }
            }

            if (BaseUtils.getDoubleProperty("env.sales.excise.tax.percent", 0) > 0 && !isExeciseDutyNonStockItem(line.getInventoryItem()) && !((ExtendedInventoryItem) line.getInventoryItem()).isIsKittedItem()) {
                  
                if (sale.getPaymentMethod() == null || !sale.getPaymentMethod().equals(POSManager.PAYMENT_METHOD_AIRTIME)) {

                    double exciseTaxRate = BaseUtils.getDoubleProperty("env.sales.excise.tax.percent", 0);
                    //exciseDuty = (PriceInCentsExcl * (exciseTaxRate / (100 + exciseTaxRate)))
                        
                    double exciseTaxAmount = line.getInventoryItem().getPriceInCentsExcl() * (exciseTaxRate / 100);
                    
                    line.getInventoryItem().setPriceInCentsExcl(line.getInventoryItem().getPriceInCentsExcl() - exciseTaxAmount);
                    log.debug("Calculated excise duty is [{}] excl amount now is [{}], item number [{}]", new Object[]{exciseTaxAmount, line.getInventoryItem().getPriceInCentsExcl(), line.getInventoryItem().getItemNumber()});
                    line.setProvisioningData(Utils.setValueInCRDelimitedAVPString(line.getProvisioningData(), "ExciseDuty", String.valueOf((exciseTaxAmount * line.getQuantity()) / 100)));
                }
            }

            if (isUnitCredit(line.getInventoryItem())
                    && BaseUtils.getBooleanProperty("env.pos.ifrs15.enabled", false)) { //And IFRS 15 is enabled and we are on the first pass into fillItemDetails . ... add the bonus/promo bundles and do the pro-rata here.
                //Check for any promo bundles associated with this bundle.
                // Add extra unit credits as per the ExtraUnitCreditSpecId configuration setting.

                extraUCLines = lookForBonusBundlesAddThemAndDoProRata(emLocal, inventorySystem, sale, line);
                log.debug("Number of extra sale lines to add for line item [{}] is [{}], index i [{}]", line.getInventoryItem().getItemNumber(),
                        extraUCLines.size(), i);
                newSaleLines.addAll(i + 1, extraUCLines);
                i += extraUCLines.size();
                log.debug("Number of extra sale lines afrer adding for line item [{}] is [{}], index i [{}]", line.getInventoryItem().getItemNumber(),
                        newSaleLines.size(), i);

            }

            i++;

            if (!((ExtendedInventoryItem) line.getInventoryItem()).isIsKittedItem()) {
                log.debug("This item is not a kitted item. Not looking for sub items just to improve performance. Verifying serial number is valid");
                if (!isAirtime(line.getInventoryItem())
                        && !isUnitCredit(line.getInventoryItem())
                        && !isNonStockItem(line.getInventoryItem())) {
                }
                continue;
            }
            List<SaleLine> subLines = getSubSaleLines(inventorySystem, sale, line, emLocal);
            log.debug("Populating sub lines with the serial numbers passed in for the sale");

            for (SaleLine newSubLine : subLines) {
                if (!newSubLine.getInventoryItem().getSerialNumber().isEmpty()) {
                    continue;
                }
                for (SaleLine passedInSubLine : line.getSubSaleLines()) {
                    if (newSubLine.getInventoryItem().getItemNumber().equals(passedInSubLine.getInventoryItem().getItemNumber())) {
                        if (passedInSubLine.getInventoryItem().getSerialNumber().equals("AUTO")) {
                            log.debug("Sub Item number [{}] needs an auto populated serial number", itemNumber);
                            passedInSubLine.getInventoryItem().setSerialNumber(getAutoPopulateSerial(inventorySystem, emLocal, passedInSubLine.getInventoryItem().getItemNumber(), sale.getWarehouseId()));
                        }
                        newSubLine.getInventoryItem().setSerialNumber(passedInSubLine.getInventoryItem().getSerialNumber());
                        break;
                    }
                }
                if (expectSerialNumbers && newSubLine.getInventoryItem().getSerialNumber().isEmpty()) {
                    throw new Exception("Missing serial number for a sub item -- " + newSubLine.getInventoryItem().getItemNumber());
                }
            }

            if (BaseUtils.getDoubleProperty("env.sales.excise.tax.percent", 0) > 0) {
                log.warn("Excise222222");
                double lineExciseTaxAmountToDeduct = 0;
                double lineExciseTaxAmount = 0;
                for (SaleLine newSubLine : subLines) {
                    if (sale.getPaymentMethod() != null && sale.getPaymentMethod().equals(POSManager.PAYMENT_METHOD_AIRTIME)) {
                        continue;
                    }
                    if (!newSubLine.getInventoryItem().getSerialNumber().equals("BUNDLE")) {
                        continue;
                    }

                    if (newSubLine.getInventoryItem().getPriceInCentsExcl() <= 0) {
                        continue;
                    }

                    double exciseTaxRate = BaseUtils.getDoubleProperty("env.sales.excise.tax.percent", 0);
                    //exciseDuty = (PriceInCentsExcl * (exciseTaxRate / (100 + exciseTaxRate)))
                    double exciseTaxAmount = (newSubLine.getInventoryItem().getPriceInCentsExcl() * ((exciseTaxRate / (100 + exciseTaxRate))));
                    newSubLine.getInventoryItem().setPriceInCentsExcl(newSubLine.getInventoryItem().getPriceInCentsExcl() - exciseTaxAmount);
                    log.warn("Calculated SUBITEM excise duty is [{}] excl amount now is [{}], item number [{}]", new Object[]{exciseTaxAmount, newSubLine.getInventoryItem().getPriceInCentsExcl(), newSubLine.getInventoryItem().getItemNumber()});
                    lineExciseTaxAmount += exciseTaxAmount;
                    lineExciseTaxAmountToDeduct += exciseTaxAmount * newSubLine.getQuantity();
                    newSubLine.setProvisioningData(Utils.setValueInCRDelimitedAVPString(line.getProvisioningData(), "SubItemExciseDuty", String.valueOf((exciseTaxAmount * newSubLine.getQuantity()) / 100)));
                }

                if (lineExciseTaxAmount > 0) {
                    line.getInventoryItem().setPriceInCentsExcl(line.getInventoryItem().getPriceInCentsExcl() - lineExciseTaxAmount);
                    line.setProvisioningData(Utils.setValueInCRDelimitedAVPString(line.getProvisioningData(), "ExciseDuty", String.valueOf(lineExciseTaxAmountToDeduct / 100)));
                    log.debug("After excise duty parent kitted item has total calculated excl of {}, total SubItemExciseDuty {}, rate is {}", line.getInventoryItem().getPriceInCentsExcl(), String.valueOf(lineExciseTaxAmountToDeduct / 100), lineExciseTaxAmount);
                }
            }

            line.getSubSaleLines().clear();
            line.getSubSaleLines().addAll(subLines);
        }

        log.debug("Number of sale lines before clearing is [{}]", sale.getSaleLines().size());
        sale.getSaleLines().clear();
        sale.getSaleLines().addAll(newSaleLines);
        log.debug("Number of sale lines after clearing is now [{}]", sale.getSaleLines().size());

    }

    private List<SaleLine> lookForBonusBundlesAddThemAndDoProRata(EntityManager emLocal, InventorySystem inventorySystem, Sale sale, SaleLine line) throws Exception {

        // Get the list of all extra unit credits on this customer's account.
        List<SaleLine> saleLines = new ArrayList<>();

        UnitCreditSpecificationQuery qUcSpec = new UnitCreditSpecificationQuery();
        qUcSpec.setItemNumber(line.getInventoryItem().getItemNumber());
        qUcSpec.setVerbosity(StUnitCreditSpecificationLookupVerbosity.MAIN);
        UnitCreditSpecificationList ucsList = SCAWrapper.getAdminInstance().getUnitCreditSpecifications(qUcSpec);

        StringBuffer listOfAllBonusAndExtraSpecIds = new StringBuffer();

        UnitCreditSpecification ucsForPurchase = ucsList.getUnitCreditSpecifications().get(0);

        String extraSpecIds = Utils.getValueFromCRDelimitedAVPString(ucsForPurchase.getConfiguration(), "ExtraUnitCreditSpecId");
        boolean isContainerUnit = false;
        //UnitCreditSpecIdsToProvision=264,265
        if ((extraSpecIds == null || extraSpecIds.isEmpty())
                && ucsForPurchase.getWrapperClass().equals("ContainerUnitCredit")) {
            // Check for container
            isContainerUnit = true;
            extraSpecIds = Utils.getValueFromCRDelimitedAVPString(ucsForPurchase.getConfiguration(), "UnitCreditSpecIdsToProvision");
        }

        int numExtraSpecIds = 0;

        if (extraSpecIds != null && extraSpecIds.trim().length() > 0) {
            listOfAllBonusAndExtraSpecIds.append(extraSpecIds);
            numExtraSpecIds = extraSpecIds.split(",").length;
        } else { //Check for container unit credit.
            extraSpecIds = "";
            numExtraSpecIds = 0;
        }

        log.debug("extraSpecIds is [{}]", extraSpecIds);

        log.debug("Got [{}] extra unit credit ids. Going to do a pro-rata and add them to the sale.", numExtraSpecIds);

        // First verify if we can add these extra unit credits to the sale...
        ProvisionUnitCreditRequest ucReq = new ProvisionUnitCreditRequest();

        //ucReq.getExtraUnitCreditSpecIds().addAll(new ArrayList<>());
        ucReq.setPlatformContext(new PlatformContext());
        // Populate account history with the txid of the sale so it can be traced back to the sale
        ucReq.getPlatformContext().setTxId("UNIQUE-UC-SaleLine-" + line.getLineId()); // Make sure it can only be done once
        ucReq.setVerifyOnly(true);
        ucReq.setCreditUnearnedRevenue(false);
        ucReq.setSaleLineId(line.getLineId());
        ProvisionUnitCreditLine pucLine = new ProvisionUnitCreditLine();
        pucLine.setAccountId(sale.getRecipientAccountId());
        pucLine.setNumberToProvision((int) line.getQuantity());
        pucLine.setItemNumber(line.getInventoryItem().getItemNumber());
        // pucLine.setProductInstanceId();
        //pucLine.setDaysGapBetweenStart(gap == null ? 0 : Integer.parseInt(gap));
        // To confirm - recognise revenue at the full standard price of the bundle - confirmed with Mike in Feb 2017
        pucLine.setPOSCentsPaidEach(line.getLineTotalCentsIncl());
        pucLine.setPOSCentsDiscountEach(0);
        ucReq.getProvisionUnitCreditLines().add(pucLine);

        log.debug("Calling BM to see is we can provision all extra unit credits.");
        try {
            SCAWrapper.getAdminInstance().provisionUnitCredit_Direct(ucReq); //Verify if we can provision all extra unit credits?
        } catch (Exception e) {
            log.warn("Got exception calling SCA to test provision extra UCs: [{}]", e.toString());
            throw e;
        }

        log.debug("Test provisioning was successfull, continue to add any bonus related bundles, pro-rata and add extra unit credits to the sale.");

        // Get the list of active unit credit instances on the recipient's account.
        AccountQuery acQuery = new AccountQuery();
        AccountQuery aq = new AccountQuery();
        aq.setAccountId(sale.getRecipientAccountId());
        aq.setVerbosity(StAccountLookupVerbosity.ACCOUNT_UNITCREDITS);
        Account acc = SCAWrapper.getAdminInstance().getAccount(aq);

        //Find any BonusUnitCredit
        UnitCreditSpecificationQuery curUcSpec = new UnitCreditSpecificationQuery();
        UnitCreditSpecification curUcs;
        StringBuffer listOfAllBonusSpecIds = new StringBuffer();
        String curListBonusSpecIds = "";

        log.debug("Recipient account id [{}] has [{}] active unit credits, will now check if they have a bonus.", sale.getRecipientAccountId(), acc.getUnitCreditInstances().size());
        for (UnitCreditInstance uci : acc.getUnitCreditInstances()) {
            curUcSpec.setUnitCreditSpecificationId(uci.getUnitCreditSpecificationId());
            curUcSpec.setVerbosity(StUnitCreditSpecificationLookupVerbosity.MAIN);
            UnitCreditSpecificationList curUcsList = SCAWrapper.getAdminInstance().getUnitCreditSpecifications(curUcSpec);

            curUcs = curUcsList.getUnitCreditSpecifications().get(0);
            //Check if ucs is a bonus
            if (curUcs.getWrapperClass().equals("BonusBundleUnitCredit")) {
                log.debug("Found bonus unit credit instance [{}] under recipient account [{}].", uci.getUnitCreditInstanceId(), sale.getRecipientAccountId());
                curListBonusSpecIds = Utils.getValueFromCRDelimitedAVPString(curUcs.getConfiguration(), "BonusForUC" + ucsForPurchase.getUnitCreditSpecificationId());
                if (curListBonusSpecIds == null || curListBonusSpecIds.isEmpty()) {
                    continue;
                }
                listOfAllBonusSpecIds.append(curListBonusSpecIds).append(",");

                boolean allowMultipleBonuses = Utils.getBooleanValueFromCRDelimitedAVPString(curUcs.getConfiguration(), "AllowMultipleBonuses");

                if (allowMultipleBonuses) {
                    log.debug("This bonus allows more bonuses to follow on");
                } else {
                    break; //Exit the loop and do not look at other bonuses.
                }
            }

        }

        log.debug("Bonus bundles to be added to the sale for recipient account [{}] are [{}]", sale.getRecipientAccountId(), listOfAllBonusSpecIds);
        if (listOfAllBonusSpecIds.length() > 0) { // We found some bonus bundles
            listOfAllBonusSpecIds.setLength(listOfAllBonusSpecIds.length() - 1); //Remove trailing ,
            listOfAllBonusAndExtraSpecIds.append(",").append(listOfAllBonusSpecIds);
        }

        if (listOfAllBonusAndExtraSpecIds == null || listOfAllBonusAndExtraSpecIds.length() <= 0) {
            log.debug("There are no bonus or extra unit credits to be added for recipient account id [{}].", sale.getRecipientAccountId());
            return saleLines; //Do nothing
        } else { //We have some extra stuff to add on the sale
            log.debug("The following unit credit spec ids will be added to the sale for recipient account id [{}], extra unit credits [{}], bonuses [{}].",
                    new Object[]{sale.getRecipientAccountId(), extraSpecIds, listOfAllBonusSpecIds.toString()});
        }

        log.debug("The full list of all extra bundles to add to the sale is [{}]", listOfAllBonusAndExtraSpecIds);
        String[] specIds = listOfAllBonusAndExtraSpecIds.toString().split(",");

        if (specIds.length <= 0) {// Just double checking
            return saleLines;
        }

        UnitCreditSpecificationQuery qExtraUcSpec = null;
        UnitCreditSpecification extraUCSpec = null;
        String itemNumber = null;

        double totalExtraUnitCreditsCentsExcl;
        double totalExtraUnitCreditsCentsIncl;

        if (isContainerUnit) {
            totalExtraUnitCreditsCentsExcl = 0.00;
            totalExtraUnitCreditsCentsIncl = 0.00;
        } else {
            totalExtraUnitCreditsCentsExcl = line.getInventoryItem().getPriceInCentsExcl();
            totalExtraUnitCreditsCentsIncl = line.getInventoryItem().getPriceInCentsIncl();
        }

        for (String specId : specIds) {
            specId = specId.trim();
            if (specId.isEmpty()) {
                continue;
            }
            int extraUCSpecId = Integer.valueOf(specId);
            if (extraUCSpecId <= 0) {
                continue;
            }
            // Get the spec of the extra unit credit.
            qExtraUcSpec = new UnitCreditSpecificationQuery();
            qExtraUcSpec.setUnitCreditSpecificationId(extraUCSpecId);
            qExtraUcSpec.setVerbosity(StUnitCreditSpecificationLookupVerbosity.MAIN);
            UnitCreditSpecificationList extraUcsList = SCAWrapper.getAdminInstance().getUnitCreditSpecifications(qExtraUcSpec);
            extraUCSpec = extraUcsList.getUnitCreditSpecifications().get(0);

            // Get the inventory item;
            itemNumber = extraUCSpec.getItemNumber();
            InventoryItem inventoryItem = inventorySystem.getInventoryItem(emLocal, itemNumber, sale.getWarehouseId(), sale.getSalesPersonCustomerId(), sale.getRecipientAccountId(), sale.getTenderedCurrency());

            totalExtraUnitCreditsCentsExcl += inventoryItem.getPriceInCentsExcl();
            totalExtraUnitCreditsCentsIncl += inventoryItem.getPriceInCentsIncl();

            SaleLine saleLine = new SaleLine();
            saleLine.setInventoryItem(inventoryItem);
            saleLine.setLineTotalCentsExcl(inventoryItem.getPriceInCentsExcl());
            saleLine.setLineTotalCentsIncl(inventoryItem.getPriceInCentsIncl());
            saleLine.setQuantity(line.getQuantity());
            saleLines.add(saleLine);

        }

        //log.debug
        log.debug("Added [{}] extra line items to the sale.", saleLines.size());
        log.debug("The total excl for all items is: totalExtraUnitCreditsCentsExcl = [{}]", totalExtraUnitCreditsCentsExcl);

        //Calculate all prorata here
        int i = 0;
        for (SaleLine saleLine : saleLines) {

            log.debug("Calculating pro-rata for line item [{}] using line.getInventoryItem().getPriceInCentsExcl() [{}],  totalExtraUnitCreditsCentsExcl [{}], saleLine.getLineTotalCentsExcl [{}].",
                    new Object[]{line.getInventoryItem().getItemNumber(), line.getInventoryItem().getPriceInCentsExcl(), totalExtraUnitCreditsCentsExcl, saleLine.getLineTotalCentsExcl()});

            saleLine.getInventoryItem().setPriceInCentsExcl(line.getInventoryItem().getPriceInCentsExcl() / totalExtraUnitCreditsCentsExcl * saleLine.getInventoryItem().getPriceInCentsExcl());
            saleLine.getInventoryItem().setPriceInCentsIncl(line.getInventoryItem().getPriceInCentsIncl() / totalExtraUnitCreditsCentsIncl * saleLine.getInventoryItem().getPriceInCentsIncl());
            if (i < numExtraSpecIds) {
                if (isContainerUnit) {
                    saleLine.getInventoryItem().setSerialNumber("-COMBOUC-");
                    saleLine.setProvisioningData((saleLine.getProvisioningData() == null ? "" : saleLine.getProvisioningData()) + "\r\nIsComboUC=true");
                    saleLine.setProvisioningData((saleLine.getProvisioningData() == null ? "" : saleLine.getProvisioningData()) + "\r\nParentItem=" + line.getInventoryItem().getItemNumber());
                } else {
                    saleLine.getInventoryItem().setSerialNumber("-EXTRAUC-");
                    saleLine.setProvisioningData((saleLine.getProvisioningData() == null ? "" : saleLine.getProvisioningData()) + "\r\nIsExtraUC=true");
                }
            } else { //It is a bonus
                saleLine.getInventoryItem().setSerialNumber("-BONUSUC-");
                saleLine.setProvisioningData((saleLine.getProvisioningData() == null ? "" : saleLine.getProvisioningData()) + "\r\nIsBonusUC=true");
            }
            saleLine.setProvisioningData(saleLine.getProvisioningData() + "\r\nIsProRata=true");
            saleLine.setQuantity(line.getQuantity());

            log.debug("Unit price (excl) for line item [{}] has been set to [{}]c.", saleLine.getInventoryItem().getItemNumber(), saleLine.getInventoryItem().getPriceInCentsExcl());
            // saleLine.setLineTotalCentsExcl(line.getLineTotalCentsExcl()/totalExtraUnitCreditsCentsExcl * saleLine.getLineTotalCentsExcl());
            // saleLine.setLineTotalCentsIncl(line.getLineTotalCentsIncl()/totalExtraUnitCreditsCentsIncl * saleLine.getLineTotalCentsIncl());
            saleLine.setLineTotalDiscountOnExclCents(0);
            saleLine.setLineTotalDiscountOnInclCents(0);
            saleLine.setProvisioningData(saleLine.getProvisioningData() + "\r\nParentLineId=" + line.getLineNumber());
            i++;
        }
        // Set pro-rata of the main item.
        log.debug("Calculating pro-rata for line item [{}] using line.getInventoryItem().getPriceInCentsExcl [{}],  totalExtraUnitCreditsCentsExcl [{}], saleLine.getLineTotalCentsExcl [{}].",
                new Object[]{line.getInventoryItem().getPriceInCentsExcl(), totalExtraUnitCreditsCentsExcl, line.getLineTotalCentsExcl()});

        if (isContainerUnit) {
            line.getInventoryItem().setPriceInCentsExcl(0.00);
            line.getInventoryItem().setPriceInCentsIncl(0.00);
            line.setProvisioningData(line.getProvisioningData() + "\r\nIsProRata=true");
        } else {

            line.getInventoryItem().setPriceInCentsExcl(line.getInventoryItem().getPriceInCentsExcl() / totalExtraUnitCreditsCentsExcl * line.getInventoryItem().getPriceInCentsExcl());
            line.getInventoryItem().setPriceInCentsIncl(line.getInventoryItem().getPriceInCentsIncl() / totalExtraUnitCreditsCentsIncl * line.getInventoryItem().getPriceInCentsIncl());
            line.setProvisioningData(line.getProvisioningData() + "\r\nIsProRata=true");
        }

        log.debug("Unit price (excl) for the main line item [{}] has been set to [{}]c.", line.getInventoryItem().getItemNumber(), line.getInventoryItem().getPriceInCentsExcl());
        log.debug("Unit price (incl) for the main line item [{}] has been set to [{}]c.", line.getInventoryItem().getItemNumber(), line.getInventoryItem().getPriceInCentsIncl());

        // line.setLineTotalCentsExcl(line.getLineTotalCentsExcl()/totalExtraUnitCreditsCentsIncl * line.getLineTotalCentsExcl());
        // saleLines.add(line);
        log.debug("Returning [{}] extra line items after pro-rata.", saleLines.size());
        return saleLines;
    }

    /*private void applyDiscountingIntoLine(SaleLine saleLine, DiscountData discData, double taxRate) {
        log.debug("Tax rate to use for this item [{}] is: [{}]", saleLine.getInventoryItem().getItemNumber(), taxRate);
        // Get per item after tax price - this is what X3 eventually gets
        double preDiscountPostTieringPerItemPriceAtSalesTaxRate = X3Helper.getCentsRoundedForPOS(saleLine.getInventoryItem().getPriceInCentsExcl() * (1 - discData.getTieredPricingPercentageOff() / 100d) * taxRate);
        log.debug("Item price at the tax rate of the sale [{}]. That is the price X3 will eventually be given", preDiscountPostTieringPerItemPriceAtSalesTaxRate);

        // PCB - Hack to fix X3 not accepting more than X decimal places. We must work with the same decimal places as X3
        saleLine.getInventoryItem().setPriceInCentsIncl(X3Helper.getCentsRoundedForPOS(saleLine.getInventoryItem().getPriceInCentsIncl() * (1 - discData.getTieredPricingPercentageOff() / 100d)));
        saleLine.getInventoryItem().setPriceInCentsExcl(X3Helper.getCentsRoundedForPOS(saleLine.getInventoryItem().getPriceInCentsExcl() * (1 - discData.getTieredPricingPercentageOff() / 100d)));
        saleLine.setLineTotalCentsExcl(saleLine.getQuantity() * saleLine.getInventoryItem().getPriceInCentsExcl() * (1 - discData.getDiscountPercentageOff() / 100d));
        saleLine.setLineTotalCentsIncl(saleLine.getQuantity() * preDiscountPostTieringPerItemPriceAtSalesTaxRate * (1 - discData.getDiscountPercentageOff() / 100d));
        saleLine.setLineTotalDiscountOnExclCents(saleLine.getQuantity() * saleLine.getInventoryItem().getPriceInCentsExcl() - saleLine.getLineTotalCentsExcl());
        saleLine.setLineTotalDiscountOnInclCents(saleLine.getQuantity() * preDiscountPostTieringPerItemPriceAtSalesTaxRate - saleLine.getLineTotalCentsIncl());
        X3Helper.logSaleLine(saleLine, "Sale Line Info After Pricing");
    }*/
    private void fillInItemDetailsFromDB(Sale sale, EntityManager emLocal) throws Exception {
        log.debug("In fillInItemDetailsFromDB");
        for (SaleLine line : sale.getSaleLines()) {
            log.debug("Populating sale line from DB for line Id [{}]. Item Number [{}]", line.getLineId(), line.getInventoryItem().getItemNumber());
            populateSaleLineFromSaleRow(emLocal, line, DAO.getSaleRowBySaleRowId(emLocal, line.getLineId()), sale.getTenderedCurrency());
        }
    }

    private List<SaleLine> getSubSaleLines(InventorySystem inventorySystem, Sale sale, SaleLine parentLine, EntityManager emLocal) throws Exception {
        List<SaleLine> subSaleLines = new ArrayList<>();
        String itemNumber = parentLine.getInventoryItem().getItemNumber();
        String serialNumber = parentLine.getInventoryItem().getSerialNumber();
        log.debug("In getSubSaleLines for item number [{}] serial number [{}]", itemNumber, serialNumber);
        List<InventoryItem> subItems = inventorySystem.getSubItems(emLocal, itemNumber, serialNumber, sale.getWarehouseId(), sale.getSalesPersonCustomerId(), sale.getRecipientAccountId(), sale.getTenderedCurrency());
        double subItemsTotalCentsIncl = 0;
        double subItemsTotalCentsExcl = 0;
        log.debug("Inventory system returned [{}] sub items for [{}]", subItems.size(), itemNumber);
        for (InventoryItem subItem : subItems) {
            log.debug("Sub item found with item number [{}] price incl [{}]cents and serial number [{}]", new Object[]{subItem.getItemNumber(), subItem.getPriceInCentsIncl(), subItem.getSerialNumber()});
            SaleLine subLine = makeSubSaleLine(sale, parentLine, subItem);
            subSaleLines.add(subLine);
            subItemsTotalCentsIncl += subItem.getPriceInCentsIncl();
            subItemsTotalCentsExcl += subItem.getPriceInCentsExcl();
        }
        if (subItems.size() > 0) {
            log.debug("Parent kitted item has total calculated cost of [{}]c incl", subItemsTotalCentsIncl);
            parentLine.getInventoryItem().setPriceInCentsIncl(subItemsTotalCentsIncl);
            parentLine.getInventoryItem().setPriceInCentsExcl(subItemsTotalCentsExcl);
        }
        return subSaleLines;
    }

    private SaleLine makeSubSaleLine(Sale sale, SaleLine parentLine, InventoryItem subItem) throws Exception {
        SaleLine child = new SaleLine();
        child.setInventoryItem(subItem);
        child.setLineNumber(parentLine.getLineNumber());
        child.setLineTotalCentsExcl(parentLine.getQuantity() * subItem.getPriceInCentsExcl());
        child.setLineTotalCentsIncl(parentLine.getQuantity() * subItem.getPriceInCentsIncl());
        child.setLineTotalDiscountOnExclCents(0); // Kitted items cannot be discounted (business rule as agreed with Heleen)
        child.setLineTotalDiscountOnInclCents(0); // Kitted items cannot be discounted (business rule as agreed with Heleen)
        child.setQuantity(parentLine.getQuantity());
        //child.getSubSaleLines().addAll(getSubSaleLines(sale, child)); commented out as we wont have sub items in sub items so lets just avoid the overhead of the extra calls
        return child;
    }

    public Sale getXMLSale(EntityManager em, com.smilecoms.pos.db.model.Sale dbSale, String verbosity) throws Exception {
        Sale xmlSale = new Sale();
        xmlSale.setAmountTenderedCents(dbSale.getAmountTenderedCents().doubleValue());
        xmlSale.setChangeCents(dbSale.getChangeCents().doubleValue());
        xmlSale.setExtTxId(dbSale.getExtTxid());
        xmlSale.setUniqueId(dbSale.getUniqueId());
        xmlSale.setOrganisationName(dbSale.getOrganisationName());
        xmlSale.setPaymentMethod(dbSale.getPaymentMethod());
        xmlSale.setPaymentTransactionData(dbSale.getPaymentTransactionData() == null ? "" : dbSale.getPaymentTransactionData());
        xmlSale.setRecipientAccountId(dbSale.getRecipientAccountId());
        xmlSale.setRecipientCustomerId(dbSale.getRecipientCustomerId());
        xmlSale.setRecipientName(dbSale.getRecipientName());
        xmlSale.setRecipientOrganisationId(dbSale.getRecipientOrganisationId());
        xmlSale.setRecipientPhoneNumber(dbSale.getRecipientPhoneNumber());
        xmlSale.setSaleDate(Utils.getDateAsXMLGregorianCalendar(dbSale.getSaleDateTime()));
        xmlSale.setLastModifiedDate(Utils.getDateAsXMLGregorianCalendar(dbSale.getLastModified()));
        xmlSale.setSaleId(dbSale.getSaleId());
        xmlSale.setSaleLocation(dbSale.getSaleLocation());
        xmlSale.setSaleTotalCentsExcl(dbSale.getSaleTotalCentsExcl().doubleValue());
        xmlSale.setSaleTotalCentsIncl(dbSale.getSaleTotalCentsIncl().doubleValue());
        xmlSale.setSaleTotalDiscountOnExclCents(dbSale.getSaleTotalDiscountOnExclCents().doubleValue());
        xmlSale.setSaleTotalDiscountOnInclCents(dbSale.getSaleTotalDiscountOnInclCents().doubleValue());
        xmlSale.setSaleTotalTaxCents(dbSale.getSaleTotalTaxCents().doubleValue());
        xmlSale.setSalesPersonAccountId(dbSale.getSalesPersonAccountId());
        xmlSale.setSalesPersonCustomerId(dbSale.getSalesPersonCustomerId());
        xmlSale.setStatus(dbSale.getStatus());
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
        xmlSale.setExpiryDate(Utils.getDateAsXMLGregorianCalendar(dbSale.getExpiryDateTime() == null ? new Date() : dbSale.getExpiryDateTime()));
        xmlSale.setFulfilmentLastCheckDateTime(Utils.getDateAsXMLGregorianCalendar(dbSale.getFulfilmentLastCheckDateTime()));
        xmlSale.setFulfilmentPausedTillDateTime(Utils.getDateAsXMLGregorianCalendar(dbSale.getFulfilmentPausedTillDateTime()));
        xmlSale.setFulfilmentScheduleInfo(dbSale.getFulfilmentScheduleInfo());
        xmlSale.setContractSaleId(dbSale.getContractSaleId() == null ? 0 : dbSale.getContractSaleId());
        xmlSale.setContractId(dbSale.getContractId() == null ? 0 : dbSale.getContractId());
        xmlSale.setTransactionFeeCents(dbSale.getTransactionFeeCents() == null ? 0 : dbSale.getTransactionFeeCents().doubleValue());
        xmlSale.setTransactionFeeModel(dbSale.getTransactionFeeModel());
        xmlSale.setDeliveryFeeCents(dbSale.getDeliveryFeeCents() == null ? 0 : dbSale.getDeliveryFeeCents().doubleValue());
        xmlSale.setDeliveryFeeModel(dbSale.getDeliveryFeeModel());
        xmlSale.setExtraInfo(dbSale.getExtraInfo());

        if (dbSale.getPaymentGatewayCode() != null && !dbSale.getPaymentGatewayCode().isEmpty()) {

            PaymentGatewayPlugin plugin = PaymentGatewayDaemon.getPlugin(em, dbSale.getPaymentGatewayCode());
            xmlSale.setPaymentGatewayURL(plugin.getGatewayURL(dbSale));
            xmlSale.setPaymentGatewayURLData(plugin.getGatewayURLData(dbSale));
            xmlSale.setLandingURL(plugin.getLandingURL(dbSale));
            xmlSale.setCallbackURL(dbSale.getCallbackURL());
            xmlSale.setPaymentGatewayCode(dbSale.getPaymentGatewayCode());
            xmlSale.setPaymentGatewayExtraData(dbSale.getPaymentGatewayExtraData());
            xmlSale.setPaymentGatewayLastPollDate(Utils.getDateAsXMLGregorianCalendar(dbSale.getPaymentGatewayLastPollDate()));
            xmlSale.setPaymentGatewayNextPollDate(Utils.getDateAsXMLGregorianCalendar(dbSale.getPaymentGatewayNextPollDate()));
            xmlSale.setPaymentGatewayPollCount(dbSale.getPaymentGatewayPollCount());
            xmlSale.setPaymentGatewayResponse(dbSale.getPaymentGatewayResponse());

        }

        if (verbosity.contains("DOCUMENTS")) {
            xmlSale.setInvoicePDFBase64(Utils.encodeBase64(dbSale.getInvoicePDF()));
            xmlSale.setReversalPDFBase64(Utils.encodeBase64(dbSale.getReversalPDF()));
            xmlSale.setSmallInvoicePDFBase64(Utils.encodeBase64(dbSale.getSmallInvoicePDF()));
        }

        // Get sales rows that have no parent
        if (verbosity.contains("LINES")) {
            for (SaleRow dbSaleRow : DAO.getSalesRows(em, dbSale.getSaleId(), 0)) {
                xmlSale.getSaleLines().add(getXMLSaleLine(em, dbSaleRow, dbSale.getTenderedCurrency(), verbosity));
            }
        }

        xmlSale.setCashInDate(Utils.getDateAsXMLGregorianCalendar(DAO.getCashInDate(em, dbSale.getSaleId())));

        if (verbosity.contains("FINANCEDATA")) {
            xmlSale.setFinanceData(DAO.getX3RequestDataForSale(em, dbSale.getSaleId()));
        }

        return xmlSale;
    }

    private void populateSaleLineFromSaleRow(EntityManager em, SaleLine line, SaleRow dbSaleRow, String currency) throws Exception {
        log.debug("In populateSaleLineFromSaleRow. Sale row Id [{}]", dbSaleRow.getSaleRowId());
        line.setLineId(dbSaleRow.getSaleRowId());
        line.setInventoryItem(new InventoryItem());
        line.getInventoryItem().setDescription(dbSaleRow.getDescription());
        line.getInventoryItem().setItemNumber(dbSaleRow.getItemNumber());
        line.getInventoryItem().setPriceInCentsExcl(dbSaleRow.getUnitPriceCentsExcl().doubleValue());
        line.getInventoryItem().setPriceInCentsIncl(dbSaleRow.getUnitPriceCentsIncl().doubleValue());
        line.getInventoryItem().setSerialNumber(dbSaleRow.getSerialNumber());
        line.getInventoryItem().setStockLevel(dbSaleRow.getQuantity()); // Mukosi modified this line in order to cater for generating sale from quote without getting inventory from X3
        line.getInventoryItem().setWarehouseId(dbSaleRow.getWarehouseId());
        line.getInventoryItem().setCurrency(currency);
        line.setLineNumber(dbSaleRow.getLineNumber());
        line.setComment(dbSaleRow.getComment());
        line.setLineTotalCentsExcl(dbSaleRow.getTotalCentsExcl().doubleValue());
        line.setLineTotalCentsIncl(dbSaleRow.getTotalCentsIncl().doubleValue());
        line.setLineTotalDiscountOnExclCents(dbSaleRow.getTotalDiscountOnExclCents().doubleValue());
        line.setLineTotalDiscountOnInclCents(dbSaleRow.getTotalDiscountOnInclCents().doubleValue());
        line.setQuantity(dbSaleRow.getQuantity());
        // Recurring function. Add sub rows to this row
        Collection<SaleRow> subRows = DAO.getSalesRows(em, dbSaleRow.getSaleId(), dbSaleRow.getSaleRowId());
        line.getSubSaleLines().clear(); // Remove whats there already
        for (SaleRow subRow : subRows) {
            line.getSubSaleLines().add(getXMLSaleLine(em, subRow, currency, "SALE_LINES_DOCUMENTS"));
        }

    }

    private CashInData getXMLCashInData(EntityManager em, CashIn cashIn) {
        CashInData cid = new CashInData();
        cid.setCashInDate(Utils.getDateAsXMLGregorianCalendar(cashIn.getCashInDateTime()));
        cid.setCashInId(cashIn.getCashInId());
        cid.setCashInType(cashIn.getCashInType());
        cid.setCashReceiptedInCents(cashIn.getCashReceiptedInCents().doubleValue());
        cid.setCashRequiredInCents(cashIn.getCashRequiredInCents().doubleValue());
        cid.setSalesAdministratorCustomerId(cashIn.getSalesAdministratorCustomerId());
        cid.setSalesPersonCustomerId(cashIn.getSalesPersonCustomerId());
        cid.setStatus(cashIn.getStatus());
        for (CashInRow row : DAO.getCashInRows(em, cashIn.getCashInId())) {
            cid.getSalesIds().add(row.getSaleId());
        }
        return cid;
    }

    private SaleLine getXMLSaleLine(EntityManager em, SaleRow dbSaleRow, String currency, String verbosity) throws Exception {
        SaleLine line = new SaleLine();
        line.setLineId(dbSaleRow.getSaleRowId());
        line.setInventoryItem(new InventoryItem());
        line.getInventoryItem().setDescription(dbSaleRow.getDescription());
        line.getInventoryItem().setItemNumber(dbSaleRow.getItemNumber());
        line.getInventoryItem().setPriceInCentsExcl(dbSaleRow.getUnitPriceCentsExcl().doubleValue());
        line.getInventoryItem().setPriceInCentsIncl(dbSaleRow.getUnitPriceCentsIncl().doubleValue());
        line.getInventoryItem().setSerialNumber(dbSaleRow.getSerialNumber());
        line.getInventoryItem().setStockLevel(dbSaleRow.getQuantity()); // Mukosi modified this line in order to cater for generating sale from quote without getting inventory from X3
        line.getInventoryItem().setWarehouseId(dbSaleRow.getWarehouseId());
        line.getInventoryItem().setCurrency(currency);
        line.setLineNumber(dbSaleRow.getLineNumber());
        line.setComment(dbSaleRow.getComment());
        line.setParentLineId(dbSaleRow.getParentSaleRowId());
        line.setLineTotalCentsExcl(dbSaleRow.getTotalCentsExcl().doubleValue());
        line.setLineTotalCentsIncl(dbSaleRow.getTotalCentsIncl().doubleValue());
        line.setLineTotalDiscountOnExclCents(dbSaleRow.getTotalDiscountOnExclCents().doubleValue());
        line.setLineTotalDiscountOnInclCents(dbSaleRow.getTotalDiscountOnInclCents().doubleValue());
        line.setQuantity(dbSaleRow.getQuantity());
        line.setProvisioningData(dbSaleRow.getProvisioningData());
        // Recurring function. Add sub rows to this row
        Collection<SaleRow> subRows = DAO.getSalesRows(em, dbSaleRow.getSaleId(), dbSaleRow.getSaleRowId());
        for (SaleRow subRow : subRows) {
            subRow.setParentSaleRowId(dbSaleRow.getSaleRowId());
            line.getSubSaleLines().add(getXMLSaleLine(em, subRow, currency, verbosity));
        }

        Collection<ReturnReplacement> returns = DAO.getReturnReplacements(em, dbSaleRow.getSaleRowId());
        for (ReturnReplacement dbReturn : returns) {
            line.getReturns().add(getXMLReturnReplacement(dbReturn, verbosity));
        }
        return line;
    }

    private Return getXMLReturnReplacement(ReturnReplacement dbReturn, String verbosity) throws Exception {
        Return xmlReturn = new Return();

        xmlReturn.setCreatedByCustomerId(dbReturn.getCreatedByCustomerProfileId());
        xmlReturn.setCreatedDate(Utils.getDateAsXMLGregorianCalendar(dbReturn.getCreatedDateTime()));
        xmlReturn.setLocation(dbReturn.getLocation());
        xmlReturn.setParentReturnId(dbReturn.getParentReturnReplacementId());
        xmlReturn.setReasonCode(dbReturn.getReasonCode());
        xmlReturn.setReturnedItemNumber(dbReturn.getReturnedItemNumber());
        InventoryItem iItem = new InventoryItem();
        xmlReturn.setReplacementItem(iItem);
        xmlReturn.setReturnId(dbReturn.getReturnReplacementId());
        xmlReturn.setSaleLineId(dbReturn.getSaleRowId());
        xmlReturn.setDescription(dbReturn.getDescription());
        xmlReturn.setReturnedSerialNumber(dbReturn.getReturnedSerialNumber());
        iItem.setItemNumber(dbReturn.getReplacementItemNumber());
        iItem.setSerialNumber(dbReturn.getReplacementSerialNumber());

        if (verbosity.contains("RETURNSDOCUMENTS")) {
            xmlReturn.setReturnReplacementPDFBase64(Utils.encodeBase64(dbReturn.getReturnReplacementPDF()));
        }

        return xmlReturn;
    }

    private Sale getSale(int saleId, EntityManager emLocal) throws Exception {
        log.debug("Retrieving a sale from the DB");
        com.smilecoms.pos.db.model.Sale dbSale = DAO.getSale(emLocal, saleId);
        Sale xmlSale = getXMLSale(emLocal, dbSale, "SALE_LINES");
        return xmlSale;
    }

    private Sale getSaleBySaleLineId(int saleLineId, EntityManager emLocal) throws Exception {
        log.debug("Retrieving a sale by saleLineId from the DB");
        com.smilecoms.pos.db.model.Sale dbSale = DAO.getSaleByLineId(emJTA, saleLineId);
        Sale xmlSale = getXMLSale(emLocal, dbSale, "SALE_LINES");
        return xmlSale;
    }

    public com.smilecoms.pos.db.model.Sale writeSale(Sale sale, int heldByOrganisationId, EntityManager emLocal) throws Exception {
        log.debug("Writing a sale to the DB");
        com.smilecoms.pos.db.model.Sale dbSale = DAO.createSale(emLocal, sale);
        for (SaleLine line : sale.getSaleLines()) {
            createSaleRow(dbSale, line, 0, heldByOrganisationId, emLocal);
        }
        return dbSale;
    }

    public com.smilecoms.pos.db.model.Sale convertQuoteToSale(Sale newSale, EntityManager emLocal) {
        log.debug("Converting a quote to a sale");
        return DAO.updateSale(emLocal, newSale);
    }

    private void createSaleRow(com.smilecoms.pos.db.model.Sale dbSale, SaleLine line, int parentRowId, int heldByOrganisationId, EntityManager emLocal) {

        SaleRow row = DAO.createSaleRow(emLocal, dbSale, line, parentRowId, heldByOrganisationId);
        for (SaleLine subLine : line.getSubSaleLines()) {
            createSaleRow(dbSale, subLine, row.getSaleRowId(), heldByOrganisationId, emLocal);
        }
    }

    private void checkInventoryLevels(EntityManager em, InventorySystem inventorySystem, Sale newSale, EntityManager emLocal) throws Exception {
        log.debug("In checkInventoryLevels for sale with [{}] lines", newSale.getSaleLines().size());
        Set<String> serialNumbers = new HashSet<>();
        List<String[]> serialsToVerify = new ArrayList<>();

        for (SaleLine line : newSale.getSaleLines()) {
            // clean up all leading or trailing spaces and cr or lf
            line.getInventoryItem().setSerialNumber(line.getInventoryItem().getSerialNumber().trim().replaceAll("(\\r|\\n)", ""));

            log.debug("Looking at sale line [{}] [{}]", line.getLineId(), line.getInventoryItem().getSerialNumber());
            if (isAirtime(line.getInventoryItem())
                    || isUnitCredit(line.getInventoryItem())
                    || isNonX3Item(line.getInventoryItem())
                    || isNonStockItem(line.getInventoryItem())) { // Do not check inventory stock for airtime (Airtime line item stock is not managed)
                continue;
            }

            if (line.getQuantity() > line.getInventoryItem().getStockLevel()) {
                throw new Exception("Insufficient stock available for item -- " + line.getInventoryItem().getItemNumber());
            }

            if (!line.getInventoryItem().getSerialNumber().isEmpty()) {
                if (serialNumbers.contains(line.getInventoryItem().getSerialNumber())) {
                    log.debug("SerialNumbers is [{}]", serialNumbers);
                    throw new Exception("Cannot have the same serial number appear more than once on a sale -- Serial Number is " + line.getInventoryItem().getSerialNumber());
                }
                serialNumbers.add(line.getInventoryItem().getSerialNumber());
                String[] item = new String[]{line.getInventoryItem().getSerialNumber(), line.getInventoryItem().getItemNumber()};
                serialsToVerify.add(item);
                log.debug("Added lines serial [{}] to serialNumbers", line.getInventoryItem().getSerialNumber());
            }

            for (SaleLine subLine : line.getSubSaleLines()) {
                log.debug("Looking at sub sale line [{}] [{}]", subLine.getLineId(), subLine.getInventoryItem().getSerialNumber());
                if (subLine.getInventoryItem().getSerialNumber().isEmpty() || isAirtime(subLine.getInventoryItem()) || isUnitCredit(subLine.getInventoryItem()) || isNonStockItem((subLine.getInventoryItem()))) { // Do not check inventory stock for airtime (Airtime line item stock is not managed)
                    continue;
                }
                if (serialNumbers.contains(subLine.getInventoryItem().getSerialNumber())) {
                    log.debug("SerialNumbers is [{}]", serialNumbers);
                    throw new Exception("Cannot have the same serial number appear more than once on a sale -- Serial Number is " + subLine.getInventoryItem().getSerialNumber());
                }
                serialNumbers.add(subLine.getInventoryItem().getSerialNumber());
                String[] item = new String[]{subLine.getInventoryItem().getSerialNumber(), subLine.getInventoryItem().getItemNumber()};
                serialsToVerify.add(item);
                log.debug("Added sublines serial [{}] to serialNumbers", subLine.getInventoryItem().getSerialNumber());
            }

        }

        if (serialNumbers.size() <= 20) {
            for (String serial : serialNumbers) {
                log.debug("Checking if serial number [{}] has already been sold", serial);
                String status = DAO.getLastSaleStatusForSerialNumber(em, serial);
                if (status == null) {
                    continue;
                }
                log.debug("Serial [{}] has a last sale of status [{}]", serial, status);
                if (status.equals(PAYMENT_STATUS_PAID)
                        || status.equals(PAYMENT_STATUS_PENDING_PAYMENT)
                        || status.equals(PAYMENT_STATUS_DELAYED_PAYMENT)
                        || status.equals(PAYMENT_STATUS_PENDING_VERIFICATION)
                        || status.equals(PAYMENT_STATUS_PENDING_DELIVERY)) {

                    throw new Exception("Serial number has already been sold and cannot be sold twice -- " + serial);
                }
            }
        } else {
            log.debug("Optimisation for large sales. Check all serials at once but loose ability to report errors on individual serials");
            List<String> statuses = DAO.getLastSaleStatusesForSerialNumbers(em, serialNumbers);

            for (String status : statuses) {
                if (status.equals(PAYMENT_STATUS_PAID)
                        || status.equals(PAYMENT_STATUS_PENDING_PAYMENT)
                        || status.equals(PAYMENT_STATUS_DELAYED_PAYMENT)
                        || status.equals(PAYMENT_STATUS_PENDING_VERIFICATION)
                        || status.equals(PAYMENT_STATUS_PENDING_DELIVERY)) {

                    throw new Exception("Serial number has already been sold and cannot be sold twice");
                }
            }
        }
        if (!serialNumbers.isEmpty()) {
            try {
                inventorySystem.verifySerialNumbers(emLocal, serialsToVerify, newSale.getWarehouseId());
            } catch (Exception e) {
                throw new Exception(e.getMessage());
            }
        }
    }

    private void generateSaleReversal(Sale sale, String userId) throws Exception {
        log.debug("Generating sale reversal PDF");
        StringBuilder xml = new StringBuilder();
        xml.append("<Reversal>");
        xml.append(marshallObject(sale));
        xml.append("<Customer>");
        Customer cust = getCustomer(sale.getRecipientCustomerId());
        Organisation org = getOrganisation(sale.getRecipientOrganisationId());
        xml.append(getCustomerXML(cust, org, sale.getRecipientAccountId()));
        xml.append("</Customer>");
        xml.append("<SalesPerson>");
        Customer salesPerson = getCustomer(sale.getSalesPersonCustomerId());
        xml.append(getSalesPersonXML(salesPerson, sale.getSalesPersonAccountId()));
        xml.append("</SalesPerson>");
        xml.append("<SaleCanceler>");
        if (userId != null && !userId.isEmpty()) {
            Customer canceler = getCustomer(userId);
            xml.append(getSalesPersonXML(canceler, 0));
            String exInf = sale.getExtraInfo() == null ? "" : sale.getExtraInfo();
            sale.setExtraInfo(exInf + "\r\n" + "CancelledByCustomerID=" + canceler.getCustomerId());
        }
        xml.append("</SaleCanceler>");
        xml.append("<CreditAccountNumber>");
        xml.append(StringEscapeUtils.escapeXml(sale.getCreditAccountNumber()));
        xml.append("</CreditAccountNumber>");
        xml.append("</Reversal>");
        log.debug("Reversal XML is [{}]", xml);
        byte[] pdf = PDFUtils.generateLocalisedPDF("sales.reversal.email.pdf.attachment.xslt", xml.toString(), LocalisationHelper.getDefaultLocale(), getClass().getClassLoader());
        sale.setReversalPDFBase64(Utils.encodeBase64(pdf));
    }

    private void storeSaleReversal(Sale sale, EntityManager emLocal) throws Exception {
        log.debug("Writing reversal PDF to the sale");
        com.smilecoms.pos.db.model.Sale dbSale = DAO.getSale(emLocal, sale.getSaleId());
        dbSale.setReversalPDF(Utils.decodeBase64(sale.getReversalPDFBase64()));
        dbSale.setLastModified(new Date());
        emLocal.persist(dbSale);
        emLocal.flush();
    }

    private void sendSaleReversal(Sale sale) throws Exception {
        log.debug("Sending reversal PDF to customer via email");
        if (sale.getReversalPDFBase64() == null) {
            log.debug("No reversal to send");
            return;
        }
        if (sale.getRecipientCustomerId() == 0) {
            log.debug("This was an anonymous sale to the invoice cannot be emailed");
            return;
        }
        try {
            CustomerCommunicationData email = new CustomerCommunicationData();
            email.setAttachmentBase64(sale.getReversalPDFBase64());
            email.setSubjectResourceName("sales.reversal.email.subject");
            email.setBodyResourceName("sales.reversal.email.body");
            email.setAttachmentFileName("Smile Reversal - " + sale.getSaleId() + ".pdf");
            email.setCustomerId(sale.getRecipientCustomerId());
            email.setBCCAddress(BaseUtils.getProperty("env.sales.invoice.bcc.email.address"));
            email.setBlocking(false);
            SCAWrapper.getAdminInstance().sendCustomerCommunication(email);
            log.debug("Sent a sale reversal");
        } catch (Exception e) {
            log.warn("Error sending sales reversal", e);
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "POS", "Error sending sales reversal: " + e.toString());
        }
    }

    private void regenerateAndStoreInvoice(int saleId, EntityManager emLocal) throws Exception {
        Sale sale = getSale(saleId, emLocal);
        Customer cust = getCustomer(sale.getRecipientCustomerId());
        Organisation org = getOrganisation(sale.getRecipientOrganisationId());
        Customer salesPerson = getCustomer(sale.getSalesPersonCustomerId());
        List <SaleRow> saleRows = DAO.getSaleRowsBySaleId(emLocal, sale.getSaleId());
        String xml = getInvoiceXML(sale, cust, org, salesPerson);
        
        byte[] pdf, pdfSmall;        
        pdf = generateInvoice(xml);
        pdfSmall = generateSmallInvoice(xml);
        
        com.smilecoms.pos.db.model.Sale dbSale = DAO.getSale(emLocal, saleId);
        dbSale.setInvoicePDF(pdf);
        dbSale.setSmallInvoicePDF(pdfSmall);
        dbSale.setLastModified(new Date());
        emLocal.persist(dbSale);
        emLocal.flush();
    }

    private void modifyTillId(int saleId, String tillId, EntityManager emLocal) throws Exception {
        Date cashinDt = DAO.getCashInDate(emLocal, saleId);
        if (cashinDt != null) {
            throw new Exception("Cannot change the till id of a cashed in sale");
        }
        com.smilecoms.pos.db.model.Sale dbSale = DAO.getSale(emLocal, saleId);
        dbSale.setTillId(tillId);
        dbSale.setLastModified(new Date());
        emLocal.persist(dbSale);
        emLocal.flush();
    }

    private void modifyPaymentTransactionData(int saleId, String txData, EntityManager emLocal) throws Exception {
        Date cashinDt = DAO.getCashInDate(emLocal, saleId);
        if (cashinDt != null) {
            throw new Exception("Cannot change the payment transaction data of a cashed in sale");
        }
        com.smilecoms.pos.db.model.Sale dbSale = DAO.getSale(emLocal, saleId);
        dbSale.setPaymentTransactionData(txData);
        dbSale.setLastModified(new Date());
        emLocal.persist(dbSale);
        emLocal.flush();
    }

    private String getInvoiceXML(Sale sale, Customer cust, Organisation org, Customer salesPerson) {
        log.debug("Compiling a sale invoice");
        StringBuilder xml = new StringBuilder();
        xml.append("<Invoice>");
        removeProrataCalc(sale);
        parseComments(sale);
        xml.append(marshallObject(sale));
        xml.append("<Customer>");
        xml.append(getCustomerXML(cust, org, sale.getRecipientAccountId()));
        xml.append("</Customer>");
        xml.append("<SalesPerson>");
        xml.append(getSalesPersonXML(salesPerson, sale.getSalesPersonAccountId()));
        xml.append("</SalesPerson>");
        xml.append("<Validity>");
        xml.append(Utils.getDaysBetweenDates(Utils.getJavaDate(sale.getExpiryDate()), Utils.getJavaDate(sale.getSaleDate())));
        xml.append("</Validity>");
        xml.append("<CreditAccountNumber>");
        xml.append(StringEscapeUtils.escapeXml(sale.getCreditAccountNumber()));
        xml.append("</CreditAccountNumber>");
        xml.append("</Invoice>");
        log.debug("Sales Invoice XML is [{}]", xml);       
        return xml.toString();
    }

    private void removeProrataCalc(Sale sale) {
        for (SaleLine parentSL : sale.getSaleLines()) {
            if (!Utils.getBooleanValueFromCRDelimitedAVPString(parentSL.getProvisioningData(), "IsProRata")) {
                continue;
            }

            String parentLineIdConfig = Utils.getValueFromCRDelimitedAVPString(parentSL.getProvisioningData(), "ParentLineId");
            if (parentLineIdConfig != null && !parentLineIdConfig.isEmpty()) {
                log.debug("IsProRata child with row id [{}] and line number [{}], going to skip", parentSL.getLineId(), parentSL.getLineNumber());
                continue;
            }

            double lineTotalCentsExcl = parentSL.getLineTotalCentsExcl();
            double lineTotalDiscountOnExclCents = parentSL.getLineTotalDiscountOnExclCents();
            double priceInCentsExcl = parentSL.getInventoryItem().getPriceInCentsExcl();
            log.debug("Parent:: TotalCentsExcl [{}], and TotalDiscOnExclCents [{}]", lineTotalCentsExcl, lineTotalDiscountOnExclCents);

            for (SaleLine childSL : sale.getSaleLines()) {

                if (!Utils.getBooleanValueFromCRDelimitedAVPString(childSL.getProvisioningData(), "IsProRata")) {
                    log.debug("No ProRata for [{}]", childSL.getLineId());
                    continue;
                }
                log.debug("Looking for a child, current child line ID [{}]", childSL.getLineId());
                String parentLineId = Utils.getValueFromCRDelimitedAVPString(childSL.getProvisioningData(), "ParentLineId");
                if (parentLineId == null || parentLineId.isEmpty()) {
                    continue;
                }

                if (Integer.parseInt(parentLineId) == parentSL.getLineNumber()) {
                    lineTotalCentsExcl = lineTotalCentsExcl + childSL.getLineTotalCentsExcl();
                    lineTotalDiscountOnExclCents = lineTotalDiscountOnExclCents + childSL.getLineTotalDiscountOnExclCents();
                    priceInCentsExcl = priceInCentsExcl + childSL.getInventoryItem().getPriceInCentsExcl();
                    //Default to 0
                    log.debug("ProRata BEFORE:: TotalCentsExcl [{}], and TotalDiscOnExclCents [{}]", childSL.getLineTotalCentsExcl(), childSL.getLineTotalDiscountOnExclCents());
                    childSL.setLineTotalCentsExcl(0);
                    childSL.setLineTotalDiscountOnExclCents(0);
                    childSL.getInventoryItem().setPriceInCentsExcl(0);
                    log.debug("ProRata AFTER:: TotalCentsExcl [{}], and TotalDiscOnExclCents [{}]", childSL.getLineTotalCentsExcl(), childSL.getLineTotalDiscountOnExclCents());
                }
            }
            parentSL.setLineTotalCentsExcl(lineTotalCentsExcl);
            parentSL.setLineTotalDiscountOnExclCents(lineTotalDiscountOnExclCents);
            parentSL.getInventoryItem().setPriceInCentsExcl(priceInCentsExcl);
            log.debug("COMPLETE ProRata CALC:: TotalCentsExcl [{}], and TotalDiscOnExclCents [{}]", lineTotalCentsExcl, lineTotalDiscountOnExclCents);
        }
    }

    private byte[] generateInvoice(String xml) throws Exception {
        
        File qrCode = null;
        if(xml.contains("qrCode")) {  //We must change it to image first to write to PDF   
            StringBuilder sb = new StringBuilder(xml.toString());
            int qrCodeFrom = xml.toString().indexOf("qrCode>")+7;
            String qrData = xml.toString().substring(qrCodeFrom,xml.toString().indexOf("</qrCode>"));


            log.warn("QRDATA is {}", qrData);
            qrCode = File.createTempFile(java.util.UUID.randomUUID().toString(), ".png");
            
            String qrCodeData = qrData.toString();
            String filePath = qrCode.getPath();
            String charset = "UTF-8"; 
            Map<EncodeHintType, ErrorCorrectionLevel> hintMap = new HashMap<EncodeHintType, ErrorCorrectionLevel>();
            hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
            try {                                            
                BitMatrix matrix = new MultiFormatWriter().encode(
                new String(qrCodeData.getBytes(charset), charset),
                BarcodeFormat.QR_CODE, 100, 100, hintMap);
                MatrixToImageWriter.writeToFile(matrix, filePath.substring(filePath
                .lastIndexOf('.') + 1), new File(filePath));

                qrCodeFrom = sb.toString().indexOf("</qrCode>")+9;
                sb.delete(sb.toString().indexOf("<qrCode"), qrCodeFrom);

                qrCodeFrom = sb.toString().indexOf("<summary>")+9;
                sb.insert(qrCodeFrom, "<qrCode>" + filePath +  "</qrCode>");

                xml = sb.toString();
                log.warn("NewXML is {}", xml);

                return PDFUtils.generateLocalisedPDF("sales.invoice.email.pdf.attachment.xslt", xml, LocalisationHelper.getDefaultLocale(), getClass().getClassLoader());

            } finally {        
                if(qrCode != null) {
                    qrCode.delete();
                }
            }

        } else {        
            return PDFUtils.generateLocalisedPDF("sales.invoice.email.pdf.attachment.xslt", xml, LocalisationHelper.getDefaultLocale(), getClass().getClassLoader());
        }
    }
    

    private byte[] generateSmallInvoice(String xml) throws Exception {
        if (LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), "sales.invoice.email.pdf.attachment.small.xslt").startsWith("?")) {
            return null;
        }
        return PDFUtils.generateLocalisedPDF("sales.invoice.email.pdf.attachment.small.xslt", xml, LocalisationHelper.getDefaultLocale(), getClass().getClassLoader());
    }
    
    private byte[] generateDummyInvoice(String xml) throws Exception {
        return PDFUtils.generateLocalisedPDF("sales.dummy.invoice.email.pdf.attachment.xslt", xml, LocalisationHelper.getDefaultLocale(), getClass().getClassLoader());
    }
        
    private void sendInvoice(Sale newSale) throws Exception {
        if (newSale.getRecipientCustomerId() == 0) {
            log.debug("This is an anonymous sale to the invoice cannot be emailed");
            return;
        }
        if (!BaseUtils.getBooleanProperty("env.pos.send.pdf.invoices.at.sale", true)) {
            log.debug("env.pos.send.pdf.invoices.at.sale says we must not send invoices to customers");
            return;
        }

        if (!BaseUtils.getBooleanProperty("env.pos.send.pdf.invoices.when.zero.due", false) && newSale.getSaleTotalCentsIncl() == 0) {
            log.debug("env.pos.send.pdf.invoices.when.zero.due is false and the invoice has zero due");
            return;
        }

        try {
            CustomerCommunicationData email = new CustomerCommunicationData();
            email.setAttachmentBase64(newSale.getInvoicePDFBase64());
            email.setSubjectResourceName("sales.invoice.email.subject");
            email.setBodyResourceName("sales.invoice.email.body");
            email.getBodyParameters().add(getSaleDescription(newSale));
            email.getBodyParameters().add("" + newSale.getSaleTotalCentsIncl());
            email.getBodyParameters().add("" + newSale.getRecipientAccountId());
            email.getSubjectParameters().addAll(email.getBodyParameters());
            String attachmentPrefix = LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), "sales.invoice.filename.prefix");
            email.setAttachmentFileName(attachmentPrefix + newSale.getSaleId() + ".pdf");
            email.setCustomerId(newSale.getRecipientCustomerId());
            email.setBCCAddress(BaseUtils.getProperty("env.sales.invoice.bcc.email.address"));
            email.setBlocking(false);
            SCAWrapper.getAdminInstance().sendCustomerCommunication(email);
            log.debug("Sent a sale invoice");
        } catch (Exception e) {
            log.warn("Error sending sales invoice", e);
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "POS", "Error sending sales invoice: " + e.toString());
        }
    }

    private void generateAndPersistCreditNote(Sale sale, List<SaleRow> salesRowsBeingReturned, SaleReturn dbReturn, EntityManager emLocal) throws Exception {
        generateAndPersistCreditNote(sale, salesRowsBeingReturned, dbReturn, emLocal, null);
    }

    private void generateAndPersistCreditNote(Sale sale, List<SaleRow> salesRowsBeingReturned, SaleReturn dbReturn, EntityManager emLocal, Map<Integer, Object[]> p2pData) throws Exception {
        log.debug("Generating credit note");
        StringBuilder xml = new StringBuilder();
        xml.append("<CreditNote>");
        xml.append("<CreditNoteId>");
        xml.append(dbReturn.getSaleReturnId());
        xml.append("</CreditNoteId>");
        xml.append("<Description>");
        xml.append(StringEscapeUtils.escapeXml(dbReturn.getDescription()));
        xml.append("</Description>");
        xml.append("<ReasonCode>");
        xml.append(StringEscapeUtils.escapeXml(dbReturn.getReasonCode()));
        xml.append("</ReasonCode>");
        xml.append("<DateTime>");
        xml.append(dbReturn.getSaleReturnDateTime());
        xml.append("</DateTime>");
        xml.append("<SalesRowsReturned>");
        double total = 0;
        for (SaleRow saleRow : salesRowsBeingReturned) {
            //Set the returned quantity: FIN-47
            BigDecimal totalCentsIncl = saleRow.getTotalCentsIncl();
            BigDecimal totalDiscCentsIncl = saleRow.getTotalDiscountOnInclCents();
            long quantity = saleRow.getQuantity();

            Object[] p2pRowData = p2pData.get(saleRow.getSaleRowId());

            if (p2pRowData != null) {
                totalCentsIncl = (BigDecimal) p2pRowData[0];
                quantity = (Long) p2pRowData[1];
                totalDiscCentsIncl = (BigDecimal) p2pRowData[2];
                totalCentsIncl = totalCentsIncl.subtract(totalDiscCentsIncl);
                log.debug("P2P Sale row quantity and price set to: {}, {} respectively", quantity, totalCentsIncl);
            }

            xml.append("<SaleRow>");
            xml.append("<ItemNumber>");
            xml.append(saleRow.getItemNumber());
            xml.append("</ItemNumber>");
            xml.append("<SerialNumber>");
            xml.append(StringEscapeUtils.escapeXml(saleRow.getSerialNumber()));
            xml.append("</SerialNumber>");
            xml.append("<Quantity>");
            xml.append(quantity);
            xml.append("</Quantity>");
            xml.append("<Description>");
            xml.append(StringEscapeUtils.escapeXml(saleRow.getDescription()));
            xml.append("</Description>");
            xml.append("<TotalCentsIncl>");
            log.debug("TotalCentsIncl [{}], TotalDiscCentsIncl [{}], Less Dis [{}]", new Object[]{totalCentsIncl.doubleValue(), totalDiscCentsIncl.doubleValue(), (totalCentsIncl.doubleValue() - totalDiscCentsIncl.doubleValue())});
            xml.append(totalCentsIncl.doubleValue());
            xml.append("</TotalCentsIncl>");
            xml.append("</SaleRow>");
            total += totalCentsIncl.doubleValue();
        }
        xml.append("</SalesRowsReturned>");
        xml.append("<CreditNoteTotalCents>");
        xml.append(total);
        xml.append("</CreditNoteTotalCents>");
        xml.append(marshallObject(sale));
        xml.append("<Customer>");
        Customer cust = getCustomer(sale.getRecipientCustomerId());
        Organisation org = getOrganisation(sale.getRecipientOrganisationId());
        xml.append(getCustomerXML(cust, org, sale.getRecipientAccountId()));
        xml.append("</Customer>");
        xml.append("<SalesPerson>");
        Customer salesPerson = getCustomer(dbReturn.getSalesPersonCustomerId());
        xml.append(getSalesPersonXML(salesPerson, 0));
        xml.append("</SalesPerson>");
        xml.append("<CreditAccountNumber>");
        xml.append(StringEscapeUtils.escapeXml(sale.getCreditAccountNumber()));
        xml.append("</CreditAccountNumber>");
        xml.append("</CreditNote>");
        log.debug("Credit Note XML is [{}]", xml);
        byte[] pdf = PDFUtils.generateLocalisedPDF("sales.creditnote.email.pdf.attachment.xslt", xml.toString(), LocalisationHelper.getDefaultLocale(), getClass().getClassLoader());
        dbReturn.setSaleReturnPDF(pdf);
        log.debug("Persisting credit note to sale return row");
        emLocal.persist(dbReturn);
        emLocal.flush();
    }

    private void sendCreditNote(Sale sale, List<SaleRow> salesRowsBeingReturned, SaleReturn dbReturn) throws Exception {
        log.debug("Sending credit note to customer via email");
        if (sale.getRecipientCustomerId() == 0) {
            log.debug("This is an anonymous sale to the credit note cannot be emailed");
            return;
        }
        try {
            CustomerCommunicationData email = new CustomerCommunicationData();
            email.setAttachmentBase64(Utils.encodeBase64(dbReturn.getSaleReturnPDF()));
            email.setSubjectResourceName("sales.creditnote.email.subject");
            email.setBodyResourceName("sales.creditnote.email.body");
            email.setAttachmentFileName("Credit Note - " + dbReturn.getSaleReturnId() + ".pdf");
            email.setCustomerId(sale.getRecipientCustomerId());
            email.setBCCAddress(BaseUtils.getProperty("env.sales.invoice.bcc.email.address"));
            email.setBlocking(false);
            SCAWrapper.getAdminInstance().sendCustomerCommunication(email);
            log.debug("Sent a credit note");
        } catch (Exception e) {
            log.warn("Error sending credit note", e);
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "POS", "Error sending credit note: " + e.toString());
        }
    }

    private void checkIfContractLineItemsAreAllowed(com.smilecoms.pos.db.model.Contract cr, Sale xmlSale) throws Exception {

        List<String> fulfilmentItemsAllowed = new ArrayList();
        fulfilmentItemsAllowed.addAll(Utils.getListFromCRDelimitedString(cr.getFulfilmentItemsAllowed()));

        // Loop for each line item and check if it is contained in the list of allowed line items, 
        // bombing out immetidately if we find an item that is not allowed.
        for (SaleLine line : xmlSale.getSaleLines()) {
            boolean found = false;
            for (String allowedItem : fulfilmentItemsAllowed) {
                if (line.getInventoryItem().getItemNumber().startsWith(allowedItem)) {
                    found = true;
                }
            }

            if (!found) { // Item number is not allowed for sale on this contract!
                throw new Exception("Item not allowed to be fulfilled on this contract -- [Contract Id = " + cr.getContractId() + ", Item Number = " + line.getInventoryItem().getItemNumber() + "]");
            }
        }
    }

    private void checkIfContractStaffMemberIsAllowed(com.smilecoms.pos.db.model.Contract cr, Sale xmlSale) throws Exception {

        // Retrieve the contract ...
        // Retrieve the actual contract ... for this sale
        List<String> allowedStaffMembers = new ArrayList();
        allowedStaffMembers.addAll(Utils.getListFromCRDelimitedString(cr.getStaffMembersAllowed()));

        // Loop for each line item and check if it is contained in the list of allowed line items, 
        // bombing out immetidately if we find an item that is not allowed.
        boolean found = false;
        for (String allowedStaffMemberCustomerId : allowedStaffMembers) {
            if (xmlSale.getRecipientCustomerId() == Integer.parseInt(allowedStaffMemberCustomerId)) {
                found = true;
            }
        }

        if (!found) { // Item number is not allowed for sale on this contract!
            throw new Exception("Customer not allowed to make a sale on this contract -- Contract Id:" + cr.getContractId() + " Customer Id:" + xmlSale.getRecipientCustomerId());
        }
    }

    private Organisation getOrganisation(int orgId) {
        log.debug("Getting organisation data for the recipient [{}]", orgId);
        if (orgId <= 0) {
            return null;
        }
        OrganisationQuery orgQ = new OrganisationQuery();
        orgQ.setOrganisationId(orgId);
        orgQ.setResultLimit(1);
        orgQ.setVerbosity(StOrganisationLookupVerbosity.MAIN);
        Organisation org = SCAWrapper.getAdminInstance().getOrganisation(orgQ);
        return org;
    }

    public static Customer getCustomer(int custId) {
        // log.debug("Getting customer data for the recipient [{}]", custId);
        CustomerQuery custQ = new CustomerQuery();
        custQ.setCustomerId(custId);
        custQ.setResultLimit(1);
        custQ.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS);
        Customer cust = SCAWrapper.getAdminInstance().getCustomer(custQ);
        return cust;
    }

    public static Customer getCustomer(String userId) {
        CustomerQuery custQ = new CustomerQuery();
        custQ.setSSOIdentity(userId);
        custQ.setResultLimit(1);
        custQ.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS);
        Customer cust = SCAWrapper.getAdminInstance().getCustomer(custQ);
        return cust;
    }

    public static Customer getCustomer(long accountId) {
        // log.debug("Getting customer data for the recipient [{}]", custId);
        ServiceInstanceQuery siq = new ServiceInstanceQuery();
        siq.setAccountId(accountId);
        siq.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
        ServiceInstance si = SCAWrapper.getAdminInstance().getServiceInstance(siq);
        return getCustomer(si.getCustomerId());
    }

    public static int getProductInstanceIdForAccountId(long accountId) {
        ServiceInstanceQuery siq = new ServiceInstanceQuery();
        siq.setAccountId(accountId);
        siq.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
        ServiceInstance si = SCAWrapper.getAdminInstance().getServiceInstance(siq);
        return si.getProductInstanceId();
    }

    private String getCustomerXML(Customer cust, Organisation org, long accountId) {
        StringBuilder xml = new StringBuilder();
        if (cust == null) {
            xml.append("<Name>");
            xml.append("Cash Sale");
            xml.append("</Name>");
            return xml.toString();
        }

        xml.append("<Name>");
        xml.append(StringEscapeUtils.escapeXml(cust.getFirstName()));
        xml.append(" ");
        xml.append(StringEscapeUtils.escapeXml(cust.getLastName()));
        xml.append("</Name>");
        xml.append("<Phone>");
        xml.append(StringEscapeUtils.escapeXml(cust.getAlternativeContact1()));
        xml.append("</Phone>");
        xml.append("<Email>");
        xml.append(StringEscapeUtils.escapeXml(cust.getEmailAddress()));
        xml.append("</Email>");
        xml.append("<AccountId>");
        xml.append(accountId);
        xml.append("</AccountId>");

        if (org != null) {
            xml.append("<OrganisationName>");
            xml.append(StringEscapeUtils.escapeXml(org.getOrganisationName()));
            xml.append("</OrganisationName>");
            xml.append("<CompanyNumber>");
            xml.append(StringEscapeUtils.escapeXml(org.getCompanyNumber()));
            xml.append("</CompanyNumber>");
            xml.append("<TaxNumber>");
            xml.append(StringEscapeUtils.escapeXml(org.getTaxNumber()));
            xml.append("</TaxNumber>");

            log.debug("Using a company address on the invoice");
            Address physical = null;
            Address delivery = null;
            Address fallback = null;
            for (Address add : org.getAddresses()) {
                // Favour delivery address
                if (add.getType().equalsIgnoreCase("Physical Address")) {
                    physical = add;
                } else if (add.getType().equalsIgnoreCase("Delivery Address")) {
                    delivery = add;
                }
                fallback = add;
            }
            Address addToUse;
            if (delivery != null) {
                addToUse = delivery;
            } else if (physical != null) {
                addToUse = physical;
            } else {
                addToUse = fallback;
            }
            if (addToUse != null) {
                xml.append("<Line1>");
                xml.append(StringEscapeUtils.escapeXml(addToUse.getLine1()));
                xml.append("</Line1>");
                xml.append("<Line2>");
                xml.append(StringEscapeUtils.escapeXml(addToUse.getLine2()));
                xml.append("</Line2>");
                xml.append("<Zone>");
                xml.append(StringEscapeUtils.escapeXml(addToUse.getZone()));
                xml.append("</Zone>");
                xml.append("<Town>");
                xml.append(StringEscapeUtils.escapeXml(addToUse.getTown()));
                xml.append("</Town>");
                xml.append("<State>");
                xml.append(StringEscapeUtils.escapeXml(addToUse.getState()));
                xml.append("</State>");
                xml.append("<Country>");
                xml.append(StringEscapeUtils.escapeXml(addToUse.getCountry()));
                xml.append("</Country>");
                xml.append("<Code>");
                xml.append(StringEscapeUtils.escapeXml(addToUse.getCode()));
                xml.append("</Code>");
            }

        } else {

            log.debug("Using a customer address on the invoice");
            Address physical = null;
            Address delivery = null;
            Address fallback = null;
            for (Address add : cust.getAddresses()) {
                // Favour delivery address
                if (add.getType().equalsIgnoreCase("Physical Address")) {
                    physical = add;
                } else if (add.getType().equalsIgnoreCase("Delivery Address")) {
                    delivery = add;
                }
                fallback = add;
            }
            Address addToUse;
            if (delivery != null) {
                addToUse = delivery;
            } else if (physical != null) {
                addToUse = physical;
            } else {
                addToUse = fallback;
            }
            if (addToUse != null) {
                xml.append("<Line1>");
                xml.append(StringEscapeUtils.escapeXml(addToUse.getLine1()));
                xml.append("</Line1>");
                xml.append("<Line2>");
                xml.append(StringEscapeUtils.escapeXml(addToUse.getLine2()));
                xml.append("</Line2>");
                xml.append("<Zone>");
                xml.append(StringEscapeUtils.escapeXml(addToUse.getZone()));
                xml.append("</Zone>");
                xml.append("<Town>");
                xml.append(StringEscapeUtils.escapeXml(addToUse.getTown()));
                xml.append("</Town>");
                xml.append("<State>");
                xml.append(StringEscapeUtils.escapeXml(addToUse.getState()));
                xml.append("</State>");
                xml.append("<Country>");
                xml.append(StringEscapeUtils.escapeXml(addToUse.getCountry()));
                xml.append("</Country>");
                xml.append("<Code>");
                xml.append(StringEscapeUtils.escapeXml(addToUse.getCode()));
                xml.append("</Code>");
            }
        }
        return xml.toString();
    }

    private String getSalesPersonXML(Customer cust, long accountId) {
        StringBuilder xml = new StringBuilder();
        xml.append("<Name>");
        xml.append(StringEscapeUtils.escapeXml(cust.getFirstName()));
        xml.append(" ");
        xml.append(StringEscapeUtils.escapeXml(cust.getLastName()));
        xml.append("</Name>");
        xml.append("<Phone>");
        xml.append(StringEscapeUtils.escapeXml(cust.getAlternativeContact1()));
        xml.append("</Phone>");
        xml.append("<AccountId>");
        xml.append(accountId);
        xml.append("</AccountId>");
        return xml.toString();
    }

    private String marshallObject(Object inputObject) {

        log.debug("Marshalling object to xml...");
        if (inputObject == null) {
            log.debug("Cannot marshall a null object - will return null");
            return null;
        }

        ObjectFactory objectFactory = new ObjectFactory();
        String xml = "";
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(inputObject.getClass().getPackage().getName(), getClass().getClassLoader());
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            JAXBElement element;
            Method mCreate = null;
            Method[] allMethods = objectFactory.getClass().getDeclaredMethods();
            for (Method m : allMethods) {
                Class<?>[] pType = m.getParameterTypes();
                for (Class<?> pType1 : pType) {
                    if (pType1.equals(inputObject.getClass())) {
                        mCreate = m;
                        break;
                    }
                }
            }

            if (mCreate != null) {
                element = (JAXBElement) mCreate.invoke(objectFactory, new Object[]{inputObject});
                if (element != null) {
                    StringWriter writer = new StringWriter();
                    marshaller.marshal(element, writer);
                    xml = writer.toString();
                }
            }
        } catch (Exception e) {
            log.warn("Error marshalling request object to a string. Will return null. Error: " + e.toString());
            log.warn("Error: ", e);
        }
        xml = xml.replaceAll(Pattern.quote("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"), "");
        xml = xml.replaceFirst(".*" + Pattern.quote(" xmlns=\"http://xml.smilecoms.com/schema/POS\">"), "<Sale>");
        xml = xml.replaceAll(Pattern.quote("</NewSale>"), "</Sale>");
        xml = xml.replaceAll(Pattern.quote("</Quote>"), "</Sale>");
        xml = xml.replaceAll(Pattern.quote("</SaleDataForQuote>"), "</Sale>");
        log.debug("Finished marshalling object to xml. Result is [{}]", xml);
        return xml;
    }

    @Override
    public Done postProcessSale(SalePostProcessingData salePostProcessingData) throws POSError {
        setContext(salePostProcessingData, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        PostProcessingResult postProcessingResult = new PostProcessingResult();
        try {
            enrichAndPostProcessSale(emJTA, salePostProcessingData, postProcessingResult, false);
            createEvent(salePostProcessingData.getSaleId());
        } catch (Exception e) {
            rollbackPostProcessing(postProcessingResult);
            JPAUtils.setRollbackOnly();
            throw processError(POSError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    private void enrichAndPostProcessSale(EntityManager em, SalePostProcessingData salePostProcessingData, PostProcessingResult postProcessingResult, boolean firstTimeBeingPostProcessed) throws Exception {
        // First get the sale

        log.debug("In enrichAndPostProcessSale: Sale Id[{}] Line Id[{}] Account Id[{}] Serial[{}] ProductInstanceId[{}] Kit Item Number[{}] Device Serial Number[{}] IgnoreStatus[{}]",
                new Object[]{salePostProcessingData.getSaleId(),
                    salePostProcessingData.getSaleLineId(),
                    salePostProcessingData.getAccountId(),
                    salePostProcessingData.getSerialNumber(),
                    salePostProcessingData.getProductInstanceId(),
                    salePostProcessingData.getKitItemNumber(),
                    salePostProcessingData.getDeviceSerialNumber(),
                    salePostProcessingData.isIgnoreStatus()});

        com.smilecoms.pos.db.model.Sale dbSale = DAO.getSale(em, salePostProcessingData.getSaleId());

        if (salePostProcessingData.getAccountId() > 0
                && dbSale.getStatus().equals(PAYMENT_STATUS_PENDING_DELIVERY)
                && dbSale.getPaymentMethod().equals(PAYMENT_METHOD_DELIVERY_SERVICE)) {
            log.debug("A pending delivery sale has now been provisioned. We must now modify the sale to a credit account sale and modify the warehouse to the location the stock is currently in");
            dbSale.setRecipientAccountId(salePostProcessingData.getAccountId());
            dealWithDelivery(em, dbSale);
        }

        Sale xmlSale = getXMLSale(em, dbSale, "SALE_LINES");

        // Next enrich the provisioning data of each root line with whats been passed in
        // Enrich with acc and pi for the serial number - this is the scenario when post processed from processOrder BPEL
        if (salePostProcessingData.getSerialNumber() != null && !salePostProcessingData.getSerialNumber().isEmpty()) {
            SaleLine xmlSaleLine = getRootSaleLineFromSale(xmlSale, salePostProcessingData.getSerialNumber());
            if (!isPostProcessedAlready(xmlSaleLine)) {
                if (salePostProcessingData.getAccountId() > 0) {
                    xmlSaleLine.setProvisioningData(Utils.setValueInCRDelimitedAVPString(xmlSaleLine.getProvisioningData(), "ToAccountId", String.valueOf(salePostProcessingData.getAccountId())));
                }
                if (salePostProcessingData.getProductInstanceId() > 0) {
                    xmlSaleLine.setProvisioningData(Utils.setValueInCRDelimitedAVPString(xmlSaleLine.getProvisioningData(), "ProductInstanceId", String.valueOf(salePostProcessingData.getProductInstanceId())));
                }
                if (salePostProcessingData.getKitItemNumber() != null && !salePostProcessingData.getKitItemNumber().isEmpty()) {
                    xmlSaleLine.setProvisioningData(Utils.setValueInCRDelimitedAVPString(xmlSaleLine.getProvisioningData(), "KitItemNumber", salePostProcessingData.getKitItemNumber()));
                }
                if (salePostProcessingData.getDeviceSerialNumber() != null && !salePostProcessingData.getDeviceSerialNumber().isEmpty()) {
                    xmlSaleLine.setProvisioningData(Utils.setValueInCRDelimitedAVPString(xmlSaleLine.getProvisioningData(), "DeviceSerialNumber", salePostProcessingData.getDeviceSerialNumber()));
                }
                xmlSaleLine.setProvisioningData(Utils.setValueInCRDelimitedAVPString(xmlSaleLine.getProvisioningData(), "SIMSerialNumber", salePostProcessingData.getSerialNumber()));
            }

            // Set device serial number onto the non-SIM services of the product.
            // ddddd
            String deviceSerialNumber = salePostProcessingData.getDeviceSerialNumber();

            if (BaseUtils.getBooleanProperty("env.sales.sim.unbundling.logic.enabled", false) && xmlSaleLine != null) {

                if (deviceSerialNumber == null || deviceSerialNumber.isEmpty()) { // Device Serial not supplied -  could  be KIT provisioned under direct sales here.  

                    deviceSerialNumber = DAO.getDeviceSerialNumberUsingSimSerialNumber(em, salePostProcessingData.getSerialNumber(),
                            salePostProcessingData.getSaleId(),
                            BaseUtils.getProperty("env.sales.kit.devices.itemnumber.prefixes", "'MIF','ROU'"));
                }

                if (deviceSerialNumber != null && !deviceSerialNumber.isEmpty()) {
                    ProductInstanceQuery piq = new ProductInstanceQuery();
                    piq.setProductInstanceId(salePostProcessingData.getProductInstanceId());
                    piq.setVerbosity(StProductInstanceLookupVerbosity.MAIN_SVC_SVCAVP);
                    ProductInstance productInstance = SCAWrapper.getAdminInstance().getProductInstance(piq);

                    ProductOrder pOrder = new ProductOrder();
                    pOrder.setAction(StAction.NONE);

                    for (ProductServiceInstanceMapping psiMapping : productInstance.getProductServiceInstanceMappings()) {

                        ServiceInstance si = psiMapping.getServiceInstance();

                        //Set IMEI on all services
                        if (si.getServiceSpecificationId() >= 1) {
                            // Utils.setValueInCRDelimitedAVPString(si.get, RETURN, RETURN)
                            ServiceInstanceOrder siOrder = new ServiceInstanceOrder();
                            // Set the IMEI
                            String imei = DAO.getDeviceIMEIUsingSerialNumber(em, deviceSerialNumber);
                            log.warn("The IMEI number for device serial number [{}] is [{}].", deviceSerialNumber, imei);
                            if (imei != null && !imei.isEmpty()) { //IMEI found 
                                if (si.getAVPs() != null) {
                                    for (AVP avp : si.getAVPs()) {
                                        if (avp != null && avp.getAttribute() != null) {
                                            if (avp.getAttribute().equalsIgnoreCase("LockedToDeviceIMEI")) {
                                                log.warn("Setting value of attribute LockedToDeviceIMEI of service instance [{}] to [{}].", si.getServiceInstanceId(), imei);
                                                avp.setValue(imei);
                                                siOrder.setAction(StAction.UPDATE);

                                                pOrder.setProductInstanceId(si.getProductInstanceId());
                                                //si.setStatus("AC"); //Activate the service instance - in case this is an emergency restore
                                                siOrder.setServiceInstance(si);
                                                X3Helper.formatAVPsForSendingToSCA(si.getAVPs(), si.getServiceSpecificationId(), true);
                                                pOrder.getServiceInstanceOrders().add(siOrder);
                                            }
                                        }
                                    }
                                }
                            } else {
                                log.warn("SIM unbundling is enabled but no IMEI number found for device serial number [{}] in the devices table. Product Instance Id [{}]",
                                        salePostProcessingData.getDeviceSerialNumber(), salePostProcessingData.getProductInstanceId());
                            }
                        }
                    }

                    if (pOrder.getServiceInstanceOrders().size() > 0) {
                        log.warn("Going to call processOrder on SCA to update locked IMEI for [{}] services under product instance [{}].",
                                pOrder.getServiceInstanceOrders(), salePostProcessingData.getProductInstanceId());
                        SCAWrapper.getAdminInstance().processOrder(pOrder);
                    } else {
                        log.warn("SIM unbubling is enabled but we have no data update IMEI for services under product [{}]", salePostProcessingData.getProductInstanceId());
                    }
                } else {
                    log.warn("SIM unbundling is enabled but we do not have device serial number to lock product instance [{}] to.", salePostProcessingData.getProductInstanceId());
                }
            }
        }

        // Enrich same for the sale line
        if (salePostProcessingData.getSaleLineId() > 0) {
            SaleLine xmlSaleLine = getRootSaleLineFromSale(xmlSale, salePostProcessingData.getSaleLineId());
            if (!isPostProcessedAlready(xmlSaleLine)) {
                if (salePostProcessingData.getAccountId() > 0) {
                    xmlSaleLine.setProvisioningData(Utils.setValueInCRDelimitedAVPString(xmlSaleLine.getProvisioningData(), "ToAccountId", String.valueOf(salePostProcessingData.getAccountId())));
                }
                if (salePostProcessingData.getProductInstanceId() > 0) {
                    xmlSaleLine.setProvisioningData(Utils.setValueInCRDelimitedAVPString(xmlSaleLine.getProvisioningData(), "ProductInstanceId", String.valueOf(salePostProcessingData.getProductInstanceId())));
                }
            }
        }

        if (salePostProcessingData.getAccountId() > 0) {
            log.debug("Setting the account Id into the sale");
            dbSale.setRecipientAccountId(salePostProcessingData.getAccountId());
            xmlSale.setRecipientAccountId(salePostProcessingData.getAccountId());
        }

        // Indicates if any kits are yet to be post processed
        boolean foundAKit = false;

        for (SaleLine xmlSaleLine : xmlSale.getSaleLines()) {
            if (isPostProcessedAlready(xmlSaleLine)) {
                continue;
            }
            if (salePostProcessingData.getAccountId() > 0 && xmlSaleLine.getSubSaleLines().isEmpty()) {
                // If we were passed an account id then set that on every root line that does not have one already and is not a kit that will be provisioned later and have an account id specified
                // Dont override what is there as it could have been stipulated at sale time
                // The overriding account id is only for rows that dont have an account set already in their provisioning data
                String currentAccountId = Utils.getValueFromCRDelimitedAVPString(xmlSaleLine.getProvisioningData(), "ToAccountId");
                if (currentAccountId == null || currentAccountId.isEmpty()) {
                    xmlSaleLine.setProvisioningData(Utils.setValueInCRDelimitedAVPString(xmlSaleLine.getProvisioningData(), "ToAccountId", String.valueOf(salePostProcessingData.getAccountId())));
                }
            }
            if (!xmlSaleLine.getSubSaleLines().isEmpty()) {
                foundAKit = true;
            }
            if (salePostProcessingData.getProductInstanceId() > 0
                    && (xmlSaleLine.getSubSaleLines().isEmpty() || salePostProcessingData.getSerialNumber() == null || salePostProcessingData.getSerialNumber().isEmpty())) {
                // If we were passed a product instance id then set that on every root line so that things like bundles in a sale are put on the first product provisioned in it
                // If we were passed a product instance id and no serial number then assume that product instance must apply to all Kits as well
                String currentProductInstanceId = Utils.getValueFromCRDelimitedAVPString(xmlSaleLine.getProvisioningData(), "ProductInstanceId");
                if (currentProductInstanceId == null || currentProductInstanceId.isEmpty()) {
                    log.debug("Setting product instance id to [{}] on sale line [{}]", salePostProcessingData.getProductInstanceId(), xmlSaleLine.getLineId());
                    xmlSaleLine.setProvisioningData(Utils.setValueInCRDelimitedAVPString(xmlSaleLine.getProvisioningData(), "ProductInstanceId", String.valueOf(salePostProcessingData.getProductInstanceId())));
                }
            }
        }

        // If the sale has no kits then tell each line that
        if (!foundAKit) {
            log.debug("This sale has no kits");
            for (SaleLine xmlSaleLine : xmlSale.getSaleLines()) {
                xmlSaleLine.setProvisioningData(Utils.setValueInCRDelimitedAVPString(xmlSaleLine.getProvisioningData(), "NoKits", "true"));
            }
        }

        log.debug("Finished enriching the sale with everything we have. Now going to give all the lines a chance to be post processed");
        // This function will persist the provisiong enriched above data into each sale line and sub line that is post processed
        // If no post processing occurs on the line then no changes are persisted on the line
        postProcessAllSaleLines(em, xmlSale, postProcessingResult, salePostProcessingData.isIgnoreStatus(), firstTimeBeingPostProcessed);
        dbSale.setLastModified(new Date());
        em.persist(dbSale);
    }

    private SaleLine getRootSaleLineFromSale(Sale xmlSale, int lineId) throws Exception {
        for (SaleLine line : xmlSale.getSaleLines()) {
            if (line.getLineId() == lineId || getSaleLineFromSaleLine(line, lineId) != null) {
                // Root sale line
                return line;
            }
        }
        throw new Exception("Sale row not found in the sale");
    }

    private SaleLine getSaleLineFromSaleLine(SaleLine line, int lineId) {
        for (SaleLine subline : line.getSubSaleLines()) {
            if (subline.getLineId() == lineId) {
                return subline;
            } else {
                SaleLine subsubline = getSaleLineFromSaleLine(subline, lineId);
                if (subsubline != null) {
                    return subsubline;
                }
            }
        }
        return null;
    }

    private SaleLine getRootSaleLineFromSale(Sale xmlSale, String serial) throws Exception {
        for (SaleLine line : xmlSale.getSaleLines()) {
            if (line.getInventoryItem().getSerialNumber().equalsIgnoreCase(serial) || getSaleLineFromSaleLine(line, serial) != null) {
                // Root sale line
                return line;
            }
        }
        throw new Exception("Sale serial not found in the sale");
    }

    private SaleLine getSaleLineFromSaleLine(SaleLine line, String serial) {
        for (SaleLine subline : line.getSubSaleLines()) {
            if (subline.getInventoryItem().getSerialNumber().equalsIgnoreCase(serial)) {
                return subline;
            } else {
                SaleLine subsubline = getSaleLineFromSaleLine(subline, serial);
                if (subsubline != null) {
                    return subsubline;
                }
            }
        }
        return null;
    }

    private void postProcessAllSaleLines(EntityManager em, Sale xmlSale, PostProcessingResult postProcessingResult, boolean ignoreStatus, boolean firstTimeBeingPostProcessed) throws Exception {
        log.debug("Doing post processing for Sale [{}] Pass 1", xmlSale.getSaleId());
        for (SaleLine xmlSaleLine : xmlSale.getSaleLines()) {
            postProcessSaleLine(em, xmlSale, xmlSaleLine, postProcessingResult, ignoreStatus, firstTimeBeingPostProcessed);
        } // End looping through sale rows

        // Do another pass in case a row far down in the sale allows for a previous row to now be provisioned
        log.debug("Doing post processing for Sale [{}] Pass 2", xmlSale.getSaleId());
        for (SaleLine xmlSaleLine : xmlSale.getSaleLines()) {
            postProcessSaleLine(em, xmlSale, xmlSaleLine, postProcessingResult, ignoreStatus, false);
        } // End looping through sale rows

    }

    private void postProcessSaleLine(EntityManager em, Sale xmlSale, SaleLine xmlSaleLine, PostProcessingResult postProcessingResult, boolean ignoreStatus, boolean firstTimeBeingPostProcessed) throws Exception {

        if (isPostProcessedAlready(xmlSaleLine)) {
            //log.debug("This sale line has already been post processed. Ignoring [{}]", xmlSaleLine.getLineId());
            return;
        }

        log.debug("In postProcessSaleLine for sale line [{}] and provisioning data [{}]", xmlSaleLine.getLineId(), xmlSaleLine.getProvisioningData());

        long fromAccount = xmlSale.getSalesPersonAccountId();
        long toAccount = xmlSale.getRecipientAccountId();
        int productInstanceId = 0;
        int lineNumber = 0;
        // Get the provisioning data values if they override the sale itself

        String sFromAccId = Utils.getValueFromCRDelimitedAVPString(xmlSaleLine.getProvisioningData(), "FromAccountId");
        if (sFromAccId != null && !sFromAccId.isEmpty()) {
            fromAccount = Long.parseLong(sFromAccId);
            //log.debug("From Account Id has been overridden as [{}]", fromAccount);
        }
        String sToAccId = Utils.getValueFromCRDelimitedAVPString(xmlSaleLine.getProvisioningData(), "ToAccountId");
        if (sToAccId != null && !sToAccId.isEmpty()) {
            toAccount = Long.parseLong(sToAccId);
            //log.debug("To Account Id has been overridden as [{}]", toAccount);
        }
        String sPIId = Utils.getValueFromCRDelimitedAVPString(xmlSaleLine.getProvisioningData(), "ProductInstanceId");
        if (sPIId != null && !sPIId.isEmpty()) {
            productInstanceId = Integer.parseInt(sPIId);
            //log.debug("Product Instance Id has been overridden as [{}]", productInstanceId);
        }

        String kitItemNumber = Utils.getValueFromCRDelimitedAVPString(xmlSaleLine.getProvisioningData(), "KitItemNumber");
        if (kitItemNumber != null && !kitItemNumber.isEmpty()) {
            //log.debug("kitItemNumber has been overridden as [{}]", kitItemNumber);
        }

        String deviceSerialNumber = Utils.getValueFromCRDelimitedAVPString(xmlSaleLine.getProvisioningData(), "DeviceSerialNumber");
        if (deviceSerialNumber != null && !deviceSerialNumber.isEmpty()) {
            //log.debug("DeviceSerialNumber has been overridden as [{}]", deviceSerialNumber);
        }
        String p2PCalendarInvoicing = Utils.getValueFromCRDelimitedAVPString(xmlSaleLine.getProvisioningData(), "P2PCalendarInvoicing");
        if (p2PCalendarInvoicing != null && !p2PCalendarInvoicing.isEmpty()) {
            xmlSaleLine.setQuantity(1);
            log.debug("Quantity to provision has been overridden to 1 because P2PCalendarInvoicing is on");
        }
        // HBT-7184- Create Enterprise Internet Access as a Product
        String dedicatedInternetBundle = Utils.getValueFromCRDelimitedAVPString(xmlSaleLine.getProvisioningData(), "ProvDedicatedInternetBundle");
        if (dedicatedInternetBundle != null && !dedicatedInternetBundle.isEmpty()) {
            xmlSaleLine.setQuantity(1);
            log.debug("Quantity to provision has been overridden to 1 because of dedicatedInternetBundle -BENTDI");
        }

        String sLineNumber = Utils.getValueFromCRDelimitedAVPString(xmlSaleLine.getProvisioningData(), "LineNumber");
        if (sLineNumber != null && !sLineNumber.isEmpty()) {
            lineNumber = Integer.parseInt(sLineNumber);
            //log.debug("Line number to provision based on has been overridden to [{}]", lineNumber);
        }

        String SIMSerialNumber = Utils.getValueFromCRDelimitedAVPString(xmlSaleLine.getProvisioningData(), "SIMSerialNumber");

        if (lineNumber > 0) {
            boolean foundOne = false;
            // The sale must be provisioned on whatever account/product was created as a result of provisioning a SIM under the line number in question
            // Get the SIM sale line
            for (SaleLine line : xmlSale.getSaleLines()) {
                if (line.getLineNumber() == lineNumber) {
                    for (SaleLine subline : line.getSubSaleLines()) {
                        // Find the first sub item that has a product instance id and account id
                        sPIId = Utils.getValueFromCRDelimitedAVPString(subline.getProvisioningData(), "ProductInstanceId");
                        sToAccId = Utils.getValueFromCRDelimitedAVPString(subline.getProvisioningData(), "ToAccountId");
                        if (sPIId != null && sToAccId != null && isPostProcessedAlready(subline)) {
                            productInstanceId = Integer.parseInt(sPIId);
                            toAccount = Long.parseLong(sToAccId);
                            log.debug("Going to use toAccount [{}] and product instance id [{}] as per line [{}] which has provisioning data [{}]",
                                    new Object[]{toAccount, productInstanceId, subline.getLineId(), subline.getProvisioningData()});
                            xmlSaleLine.setProvisioningData(Utils.setValueInCRDelimitedAVPString(xmlSaleLine.getProvisioningData(), "ToAccountId", String.valueOf(toAccount)));
                            xmlSaleLine.setProvisioningData(Utils.setValueInCRDelimitedAVPString(xmlSaleLine.getProvisioningData(), "ProductInstanceId", String.valueOf(productInstanceId)));
                            foundOne = true;
                            break;
                        }
                    }
                    break;
                }
            }
            if (!foundOne) {
                log.debug("This sale has no line with number [{}] that has been provisioned. Going to hold off post processing", lineNumber);
                return;
            }

        }

        String sNoKits = Utils.getValueFromCRDelimitedAVPString(xmlSaleLine.getProvisioningData(), "NoKits");
        boolean noKits = false;
        if (sNoKits != null && sNoKits.equals("true")) {
            noKits = true;
        }

        boolean onlyVerify = false;
        if (!ignoreStatus
                && !xmlSale.getStatus().equals(PAYMENT_STATUS_PAID)
                && !xmlSale.getStatus().equals(PAYMENT_STATUS_STAFF_OR_LOAN)) {
            log.debug("This sale has not been paid yet so post processing will be deferred. We will however just test that it could work - i.e. the transfers are valid");
            onlyVerify = true;
        }

        int resultsProcessedBefore = postProcessingResult.transferTxIds.size() + postProcessingResult.ucTxIds.size();
        boolean didSomething = false;

        if (fromAccount > 0 && toAccount > 0 && isAirtime(xmlSaleLine.getInventoryItem())) {
            if (xmlSale.getPaymentMethod().equals(PAYMENT_METHOD_AIRTIME)) {
                log.debug("This sale is paid for by airtime so you cant buy airtime. No real reason why not but no point allowing it");
                throw new Exception("Cannot buy airtime when paying by airtime");
            }
            transferLinesAirtime(em, xmlSale, xmlSaleLine, fromAccount, toAccount, onlyVerify, postProcessingResult);
        } else if (toAccount > 0 && (productInstanceId > 0 || noKits) && isUnitCredit(xmlSaleLine.getInventoryItem())) {
            provisionLinesUC(xmlSale, xmlSaleLine, toAccount, productInstanceId, onlyVerify, postProcessingResult);
        } else if (productInstanceId > 0
                && isSIM(xmlSaleLine)
                && SIMSerialNumber != null
                && xmlSaleLine.getInventoryItem().getSerialNumber() != null
                && xmlSaleLine.getInventoryItem().getSerialNumber().equals(SIMSerialNumber)) {
            log.debug("This is a provisioned SIM [{}]", SIMSerialNumber);
            if (toAccount > 0 && kitItemNumber != null && !kitItemNumber.isEmpty()) {
                log.debug("The provisioned SIM has a specific KIT determined at provisioning time");
                try {
                    provisionKitsUCs(em, xmlSale, xmlSaleLine, toAccount, productInstanceId, kitItemNumber, deviceSerialNumber, onlyVerify, postProcessingResult);
                } catch (Exception e) {
                    new ExceptionManager(log).reportError(e);
                }
            }
            if (!onlyVerify) {
                try {
                    PlatformEventManager.createEvent("POS", "PaidSIMSale", String.valueOf(xmlSale.getRecipientCustomerId()), xmlSale.getRecipientCustomerId() + "|" + xmlSale.getSaleId() + "|" + productInstanceId, "PaidSIMSale_" + xmlSaleLine.getLineId());
                } catch (Exception e) {
                    log.warn("Error sending sold SIM event", e);
                }
            }
            // Make sure the SIM sale line is marked as having been post processed
            didSomething = true;
        } else {
            // log.debug("This sale line has nothing to post process [{}]", xmlSaleLine.getInventoryItem().getItemNumber());
        }

        if (!didSomething) {
            didSomething = ((postProcessingResult.transferTxIds.size() + postProcessingResult.ucTxIds.size()) != resultsProcessedBefore);
        }

        log.debug("Didsomething for sale line [{}]? [{}]", xmlSaleLine.getLineId(), didSomething);

        for (SaleLine subLine : xmlSaleLine.getSubSaleLines()) {
            //log.debug("Calling to post process sub line [{}]", subLine.getLineId());
            // We must enrich the sub lines provisioning data with that of the parent line - except for the PostProcessed attribute which should remain unchanged
            String postProcessed = Utils.getValueFromCRDelimitedAVPString(subLine.getProvisioningData(), "PostProcessed");
            String enrichedData = Utils.setValueInCRDelimitedAVPString(xmlSaleLine.getProvisioningData(), "PostProcessed", postProcessed);
            subLine.setProvisioningData(enrichedData);
            postProcessSaleLine(em, xmlSale, subLine, postProcessingResult, ignoreStatus, firstTimeBeingPostProcessed);
        }
        // Do after the sub lines so they dont get flagged as having been post processed

        if (didSomething) {
            String pd = Utils.setValueInCRDelimitedAVPString(xmlSaleLine.getProvisioningData(), "PostProcessed", "true");
            pd = Utils.setValueInCRDelimitedAVPString(pd, "PPDate", Utils.getDateAsString(new Date(), "yyyy/MM/dd HH:mm:ss"));
            xmlSaleLine.setProvisioningData(pd);
        }

        if ((firstTimeBeingPostProcessed || Utils.getBooleanValueFromCRDelimitedAVPString(xmlSaleLine.getProvisioningData(), "PostProcessed"))
                && (isUnitCredit(xmlSaleLine.getInventoryItem()) || isAirtime(xmlSaleLine.getInventoryItem()) || isSIM(xmlSaleLine))) {
            // Only bother if its the first time enriching the sale or its now been post processed
            DAO.putProvisioningDataIntoSaleLine(em, xmlSaleLine.getLineId(), xmlSaleLine.getProvisioningData());
        }
    }

    private boolean isDirectAirtimeSale(long accountNo) {
        if (accountNo <= 0) {
            return false;
        }
        boolean isDirectAirtimeEnabled = BaseUtils.getBooleanProperty("env.direct.airtime.account.sales", false);
        if (!isDirectAirtimeEnabled) {
            return false;
        }
        return isDirectAirtimeAccount(accountNo);
    }

    private boolean isDirectAirtimeAccount(long accountId) {
        ServiceInstanceQuery siquery = new ServiceInstanceQuery();
        siquery.setSCAContext(new SCAContext());
        siquery.getSCAContext().setTxId("UNIQUE-ACC-QUERY-" + accountId);
        siquery.setAccountId(accountId);
        siquery.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
        ServiceInstance si = SCAWrapper.getAdminInstance().getServiceInstance(siquery);
        log.debug("service specification id for [{}] is [{}]", accountId, si.getServiceSpecificationId());
        return 1007 == si.getServiceSpecificationId();
    }

    private void transferLinesAirtime(EntityManager em, Sale xmlSale, SaleLine xmlSaleLine, long fromAccount, long toAccount, boolean onlyVerify, PostProcessingResult postProcessingResult) throws Exception {
        log.debug("In transferLinesAirtime");
        double freeAmntCents = xmlSaleLine.getLineTotalDiscountOnInclCents(); // Amount partner gets for free - i.e. the discount
        double totalcents = xmlSaleLine.getQuantity() * 100d; // Total airtime quantity 
        double baseAmntCents = totalcents - freeAmntCents; // The total quantity less the discount. If tax exempt, then this will be more than Smile is actually paid
        if (totalcents <= 0) {
            throw new Exception("Invalid sale amount");
        }

        BalanceTransferData btd = new BalanceTransferData();
        btd.setSCAContext(new SCAContext());
        // Populate account history with the txid of the sale so it can be traced back to the sale
        btd.getSCAContext().setTxId("UNIQUE-TFR-SaleLine-" + xmlSaleLine.getLineId()); // Make sure it can only be done once

        // The transfer amount is based on the quantity and not the price as this allows for discounting
        log.debug("Transferring [{}]cents in total of which [{}] is paid for and [{}] is free/commission", new Object[]{totalcents, baseAmntCents, freeAmntCents});
        if (fromAccount < 1100000000 || toAccount < 1100000000) {
            throw new Exception("Invalid sale account for airtime sale");
        }

        if (baseAmntCents < 0) {
            throw new Exception("Invalid sale amount");
        }

        boolean isDirectAirtimeSale = isDirectAirtimeSale(toAccount) || isDirectAirtimeSale(fromAccount);
        if (!xmlSale.getPaymentMethod().equals(PAYMENT_METHOD_CASH)
                && !xmlSale.getPaymentMethod().equals(PAYMENT_METHOD_CARD_PAYMENT)
                && !isDirectAirtimeSale) {
            log.debug("This was not a cash nor card payment sale so the sales account must be topped up first");
            btd.setAmountInCents(onlyVerify ? 0 : baseAmntCents);
            btd.setSourceAccountId(1000000004);
            btd.setTargetAccountId(fromAccount);
            btd.setDescription("Redemption for sale " + xmlSale.getSaleId());
        }

        if (baseAmntCents > 0 || onlyVerify) {
            BalanceTransferLine transferLine = new BalanceTransferLine();
            transferLine.setAmountInCents(onlyVerify ? 0 : baseAmntCents);
            transferLine.setSourceAccountId(fromAccount);
            transferLine.setTargetAccountId(toAccount);
            transferLine.setSaleId(xmlSale.getSaleId());
            btd.getAdditionalTransferLines().add(transferLine);
        }
        if (freeAmntCents > 0) {
            log.debug("There is free airtime included in this sale. It will be transferred from the commission paying account 1000000003");
            BalanceTransferLine commissionTransferLine = new BalanceTransferLine();
            commissionTransferLine.setAmountInCents(onlyVerify ? 0 : freeAmntCents);
            commissionTransferLine.setSourceAccountId(1000000003);
            commissionTransferLine.setTargetAccountId(toAccount);
            commissionTransferLine.setSaleId(xmlSale.getSaleId());
            btd.getAdditionalTransferLines().add(commissionTransferLine);

            // env.pos.vat.input.and.wht.on.icp.commission.enabled
            // As per HBT-8072 - The current ICP commission set up is that once we process the sales the VAT on the commission should be posted to GL account 2380001.The current configuration the VAT input is posting to GL 4380002 which is not correct as the Net off is not accepted 
            if (BaseUtils.getBooleanProperty("env.pos.vat.input.and.wht.on.icp.commission.enabled", false) && !isDirectAirtimeSale) {
                // 434 782 616./1.18
                double taxRate = BaseUtils.getDoubleProperty("env.sales.tax.percent");
                double whTax = BaseUtils.getDoubleProperty("env.sales.withholding.tax.percent");
                double commissionAmountExVAT = ((freeAmntCents / 100d) / (1 + taxRate / 100.0d));

                double whtOnCommnissionExVAT = commissionAmountExVAT * (whTax / 100d);
                double inputVatOnCommissionAmount = commissionAmountExVAT * (taxRate / 100.0d);

                BalanceTransferLine whtCommissionTransferLine = new BalanceTransferLine();

                whtCommissionTransferLine.setAmountInCents(onlyVerify ? 0 : whtOnCommnissionExVAT * 100); //Must be cents
                whtCommissionTransferLine.setSourceAccountId(toAccount);
                whtCommissionTransferLine.setTargetAccountId(1000000003);
                whtCommissionTransferLine.setSaleId(0);
                whtCommissionTransferLine.setDescription("WHTonCommission-SaleId:" + xmlSale.getSaleId());
                // btd.getAdditionalTransferLines().add(whtCommissionTransferLine); 

                // Create GL
                //   sendWHTaxOnCommissionJournal(em, whtOnCommnissionExVAT, "WHOLD-" + xmlSale.getSaleId(), xmlSaleLine.getLineId());
                sendInputVATOnCommissionJournal(em, inputVatOnCommissionAmount, "GLVAT-" + xmlSale.getSaleId(), xmlSaleLine.getLineId());
            }
        }

        log.debug("About to call SCA to do transfers. Is this only a verification: [{}]", onlyVerify);
        try {
            SCAWrapper.getAdminInstance().transferBalance(btd);
            if (!onlyVerify) {
                postProcessingResult.transferTxIds.add(btd.getSCAContext().getTxId());
                log.debug("Transfers unique txid is [{}]", btd.getSCAContext().getTxId());
            }

            double exciseTaxRate = BaseUtils.getDoubleProperty("env.sales.excise.tax.percent", 0);
            boolean handleExciseTax = (exciseTaxRate > 0 && !isExeciseDutyNonStockItem(xmlSaleLine.getInventoryItem()));

            if (!onlyVerify && handleExciseTax) {
                sendExciseTaxJournal(xmlSale, xmlSaleLine);
                log.debug("finished with excise duty journal");
            }

        } catch (SCABusinessError sbe) {
            if (onlyVerify && sbe.getErrorCode().equals("SCA-0015")) {
                log.debug("Transfer verification succeeded");
            } else {
                throw sbe;
            }
        }
    }

    private void payByAirtime(Sale xmlSale, PostProcessingResult postProcessingResult) throws Exception {
        log.debug("In payByAirtime to remove [{}]c from Account [{}]", xmlSale.getTotalLessWithholdingTaxCents(), xmlSale.getSalesPersonAccountId());
        if (xmlSale.getTotalLessWithholdingTaxCents() == 0) {
            log.debug("Sale is for 0 so no need to charge anything");
            return;
        }
        ChargingRequest cr = new ChargingRequest();
        cr.setPlatformContext(new com.smilecoms.commons.sca.direct.bm.PlatformContext());
        cr.getPlatformContext().setTxId("UNIQUE-CHG-Sale-" + xmlSale.getSaleId());
        ChargingData cd = new ChargingData();
        cr.getChargingData().add(cd);
        cd.setChargingDataIndex(0);
        cd.setSessionId(cr.getPlatformContext().getTxId());
        try {
            ChargingResult res = SCAWrapper.getAdminInstance().rateAndBill_Direct(cr);
            if (!res.getGrantedServiceUnits().isEmpty() && res.getGrantedServiceUnits().get(0).getErrorCode() != null && !res.getGrantedServiceUnits().get(0).getErrorCode().isEmpty()) {
                throw new Exception("Error charging for sale by airtime -- " + res.getGrantedServiceUnits().get(0).getErrorCode());
            }
        } catch (Exception e) {
            log.warn("Error paying for sale by airtime: [{}]", e.toString());
            throw e;
        }
        postProcessingResult.chargeTxIds.add(cd.getSessionId() + "_9000");
        log.debug("Charges unique txid is [{}]. BM's Ext TxId will have _9000 appended to it", cd.getSessionId());
    }

    private void reserveForPayByAirtime(Sale xmlSale) throws Exception {
        log.debug("In reserveForPayByAirtime to reserve [{}]c on Account [{}]", xmlSale.getTotalLessWithholdingTaxCents(), xmlSale.getSalesPersonAccountId());

        if (xmlSale.getTotalLessWithholdingTaxCents() == 0) {
            log.debug("Sale is for 0 so no need to reserve anything");
            return;
        }
        ChargingRequest cr = new ChargingRequest();
        cr.setPlatformContext(new com.smilecoms.commons.sca.direct.bm.PlatformContext());
        cr.getPlatformContext().setTxId("UNIQUE-CHG-Sale-" + xmlSale.getSaleId());
        ChargingData cd = new ChargingData();
        cr.getChargingData().add(cd);
        cd.setSessionId(cr.getPlatformContext().getTxId());
        cd.setChargingDataIndex(0);
        cd.setDescription("Payment for Sale " + xmlSale.getSaleId());
        cd.setEventTimestamp(xmlSale.getSaleDate());
        cd.setIPAddress("");
        cd.setLocation("");
        cd.setRatingKey(new RatingKey());
        // Tell BM that it must not recognise revenue for this event
        cd.getRatingKey().setServiceCode("txtype.sale.purchase");
        cd.getRatingKey().setRatingGroup("9000"); // For arbitrary monetary charges
        // http://jira.smilecoms.com/browse/HBT-6758
        cd.getRatingKey().setTo(String.valueOf(xmlSale.getRecipientAccountId()));
        cd.setServiceInstanceIdentifier(new ServiceInstanceIdentifier());
        cd.getServiceInstanceIdentifier().setIdentifierType("ACCOUNT");
        cd.getServiceInstanceIdentifier().setIdentifier(String.valueOf(xmlSale.getSalesPersonAccountId()));
        cd.setUserEquipment("");
        cd.setRequestedServiceUnit(new RequestedServiceUnit());
        cd.getRequestedServiceUnit().setReservationSecs(10);
        cd.getRequestedServiceUnit().setTriggerCharged(true);
        cd.getRequestedServiceUnit().setUnitQuantity(new BigDecimal(xmlSale.getTotalLessWithholdingTaxCents()).setScale(0, RoundingMode.HALF_EVEN));
        cd.getRequestedServiceUnit().setUnitType("CENT");
        try {
            ChargingResult res = SCAWrapper.getAdminInstance().rateAndBill_Direct(cr);
            if (!res.getGrantedServiceUnits().isEmpty()) {
                if (res.getGrantedServiceUnits().get(0).getErrorCode() != null && !res.getGrantedServiceUnits().get(0).getErrorCode().isEmpty()) {
                    throw new Exception("Error reserving for sale by airtime -- " + res.getGrantedServiceUnits().get(0).getErrorCode());
                } else if (res.getGrantedServiceUnits().get(0).getUnitQuantity().compareTo(cd.getRequestedServiceUnit().getUnitQuantity()) != 0) {
                    throw new Exception("Error reserving for sale by airtime -- Insufficient funds");
                }
            }
        } catch (Exception e) {
            log.warn("Error paying for sale by airtime: [{}]", e.toString());
            throw e;
        }
    }

    private void provisionKitsUCs(EntityManager em, Sale xmlSale, SaleLine xmlSaleLine, long toAccount, int productInstanceId, String kitItemNumber, String deviceSerialNumber, boolean onlyVerify, PostProcessingResult postProcessingResult) throws Exception {
        log.debug("This is a provision-time kit for a SIM provisioned for product instance id [{}] and Account [{}] and Kit [{}] and ICCID [{}]", new Object[]{productInstanceId, toAccount, kitItemNumber, xmlSaleLine.getInventoryItem().getSerialNumber()});

        if (xmlSaleLine.getQuantity() != 1 || !xmlSaleLine.getInventoryItem().getItemNumber().startsWith("SIM") || kitItemNumber == null || kitItemNumber.isEmpty()) {
            throw new Exception("Invalid use of provisionKitsUCs");
        }

        if (!(isMegaDealer(xmlSale.getRecipientOrganisationId()) || isSuperDealer(xmlSale.getRecipientOrganisationId()) || isFranchise(xmlSale.getRecipientOrganisationId()))) {
            throw new Exception("Invalid use of provisionKitsUCs -- Org Id " + xmlSale.getRecipientOrganisationId() + " is not a super dealer");
        }

        com.smilecoms.pos.db.model.SaleRow simrow = DAO.getSaleRowBySaleRowId(em, xmlSaleLine.getLineId());
        if (simrow.getParentSaleRowId() != 0) {
            throw new Exception("Cannot specify the kit to provision with a SIM when it was already sold as a kit -- Sale row Id:" + simrow.getSaleRowId());
        }

        String iccid = xmlSaleLine.getInventoryItem().getSerialNumber();

        log.debug("Looking for product instance just created on SIM [{}] to see which Org created it", iccid);
        ProductInstanceQuery piq = new ProductInstanceQuery();
        piq.setPhysicalId(iccid);
        piq.setVerbosity(StProductInstanceLookupVerbosity.MAIN);
        ProductInstance pi = SCAWrapper.getAdminInstance().getProductInstance(piq);

        int orgId = pi.getCreatedByOrganisationId();
        log.debug("SIM was provisioned by Org Id [{}]", orgId);
        long acc = getICPNettOutAccount(orgId);
        double discount = getICPDiscount(orgId);
        double discountGivenWhenICPorSDBoughtAirtime = discount;
        double originalDiscountOnAirtimePurchasedIncVat = 0;

        ProvisionUnitCreditRequest ucReq = new ProvisionUnitCreditRequest();
        ucReq.setPlatformContext(new PlatformContext());
        // Populate account history with the txid of the sale so it can be traced back to the sale
        ucReq.getPlatformContext().setTxId("UNIQUE-UC-SaleLine-" + xmlSaleLine.getLineId()); // Make sure it can only be done once
        ucReq.setVerifyOnly(onlyVerify);
        ucReq.setCreditUnearnedRevenue(!xmlSale.getPaymentMethod().equals(PAYMENT_METHOD_AIRTIME));
        ucReq.setSaleLineId(xmlSaleLine.getLineId());
        String gap = Utils.getValueFromCRDelimitedAVPString(xmlSaleLine.getProvisioningData(), "DaysGapBetweenStart");

        // Get the Unit credits that come with the Kit
        InventorySystem inventorySystem = InventorySystemManager.getInventorySystem();
        List<InventoryItem> kitItems;
        try {
            kitItems = inventorySystem.getSubItems(em, kitItemNumber, null, xmlSale.getWarehouseId(), 0, 0, xmlSale.getTenderedCurrency());
        } finally {
            inventorySystem.close();
        }
        boolean foundDevice = false;

        // A few things we need to track
        // The amount incl the SD paid for the SIM pre discount
        // (row.getTotalDiscountOnInclCents().doubleValue() + row.getTotalCentsIncl().doubleValue());
        // double simSoldBySmileForCentsPreDiscount = xmlSaleLine.getInventoryItem().getPriceInCentsIncl();
        //HBT-8396
        double simSoldBySmileForCentsPreDiscount = (xmlSaleLine.getLineTotalDiscountOnInclCents() + xmlSaleLine.getLineTotalCentsIncl());

        // The origical discount given to the SD for the SIM;
        double originalDiscountOnSim = (xmlSaleLine.getLineTotalDiscountOnInclCents() / simSoldBySmileForCentsPreDiscount) * 100;

        // The amount incl the SD paid for the device before discount
        double deviceSoldBySmileForCentsPreDiscount = 0;

        //  The original discount amount given to the device when selling to the SD. 
        double originalDiscountOnDeviceAndBundle = 0;

        // The amount incl the SD paid for the Super Dealer UC before discount
        double superDealerUCSoldBySmileForCentsPreDiscount = 0;
        // The amount incl for bundles we will recognise as revenue (current market price)
        double bunMarketPriceCents = 0;
        // The market price incl for the SIM
        double simMarketPriceCents = 0;
        // The market price incl for the device
        double deviceMarketPriceCents = 0;
        // The excl amount paid for the SIM and Device by the super dealer pre discount
        // double deviceAndSIMSoldToSDForCentsExclPreDiscount = xmlSaleLine.getInventoryItem().getPriceInCentsExcl();
        double deviceAndSIMSoldToSDForCentsExclPreDiscount = 0; // xmlSaleLine.getInventoryItem().getPriceInCentsExcl();
        double simSoldToSDForCentsExclPreDiscount = xmlSaleLine.getInventoryItem().getPriceInCentsExcl();
        String simItemNumber = xmlSaleLine.getInventoryItem().getItemNumber();

        double deviceSoldToSDForCentsExclPreDiscount = 0;
        double icpDiscountCents = 0;
        double specialICPNettOffDiscountForDeviceAndBundle = 0;

        // double 
        String deviceItemNumber = null;

        String icpFreeToCustomerKitMatch = BaseUtils.getProperty("env.pos.icp.free.kits.override", "XXX");
        boolean freeOverride = Utils.matchesWithPatternCache(kitItemNumber, icpFreeToCustomerKitMatch);

        for (InventoryItem item : kitItems) {
            log.debug("Dealing with Kit subitem [{}]", item.getItemNumber());

            if (item.getItemNumber().startsWith("SIM")) {
                log.debug("This is the SIM card");
                if (simMarketPriceCents != 0) {
                    throw new Exception("A kit cannot have more than one sim");
                }
                simMarketPriceCents = freeOverride ? 0 : item.getPriceInCentsIncl();
                continue;
            }
            if (!isUnitCredit(item)) {
                deviceItemNumber = item.getItemNumber();
                log.debug("This is not a unit credit. It must be a device with serial [{}] and should be item number [{}]", deviceSerialNumber, deviceItemNumber);
                com.smilecoms.pos.db.model.SaleRow row = DAO.getUnprocessedSaleRowByItemAndSerial(em, deviceItemNumber, deviceSerialNumber);
                log.debug("Found original sale row id [{}] for the device", row.getSaleRowId());
                if (!row.getItemNumber().equals(deviceItemNumber)) {
                    throw new Exception("A serial number was specified for a kit that is not part of the kit -- " + row.getItemNumber() + " should be " + deviceItemNumber);
                }
                if (!onlyVerify) {
                    // Set PostProcessed=true on the device serial
                    row.setProvisioningData(Utils.setValueInCRDelimitedAVPString(row.getProvisioningData(), "KittedIn", kitItemNumber));
                    row.setProvisioningData(Utils.setValueInCRDelimitedAVPString(row.getProvisioningData(), "KittedSIMRowId", String.valueOf(xmlSaleLine.getLineId())));
                    row.setProvisioningData(Utils.setValueInCRDelimitedAVPString(row.getProvisioningData(), "PostProcessed", "true"));
                    row.setProvisioningData(Utils.setValueInCRDelimitedAVPString(row.getProvisioningData(), "PPDate", Utils.getDateAsString(new Date(), "yyyy/MM/dd HH:mm:ss")));
                    em.persist(row);
                    em.flush();
                    log.debug("Modified rows provisioning data to [{}]", row.getProvisioningData());
                }
                foundDevice = true;
                deviceMarketPriceCents += freeOverride ? 0 : item.getPriceInCentsIncl();
                // deviceSoldBySmileForCentsPreDiscount += row.getUnitPriceCentsIncl().doubleValue();
                //HBT-8396
                deviceSoldBySmileForCentsPreDiscount += (row.getTotalDiscountOnInclCents().doubleValue() + row.getTotalCentsIncl().doubleValue());
                originalDiscountOnDeviceAndBundle += row.getTotalDiscountOnInclCents().doubleValue();

                if (isICP(orgId)) {
                    specialICPNettOffDiscountForDeviceAndBundle = getSpecialICPNettOutDiscountForItemNumber(item.getItemNumber());
                }
                // deviceAndSIMSoldToSDForCentsExclPreDiscount += row.getUnitPriceCentsExcl().doubleValue();
                deviceSoldToSDForCentsExclPreDiscount += row.getUnitPriceCentsExcl().doubleValue();

                log.debug("Checking if the device came with a super dealer bundle as an unearned revenue placeholder");
                Collection<com.smilecoms.pos.db.model.SaleRow> allRows = DAO.getSalesRows(em, row.getSaleId(), row.getParentSaleRowId());
                for (com.smilecoms.pos.db.model.SaleRow otherRow : allRows) {
                    if (otherRow.getItemNumber().equals("BUNKSD0001")) {
                        log.debug("Found Super Dealer UC on sale row id [{}]", otherRow.getSaleRowId());
                        superDealerUCSoldBySmileForCentsPreDiscount += otherRow.getUnitPriceCentsIncl().doubleValue();
                    }
                }

                continue;
            }
            // This is a unit credit

            ProvisionUnitCreditLine pucLine = new ProvisionUnitCreditLine();
            pucLine.setAccountId(toAccount);
            pucLine.setNumberToProvision((int) xmlSaleLine.getQuantity());
            pucLine.setItemNumber(item.getItemNumber());
            pucLine.setProductInstanceId(productInstanceId);
            pucLine.setDaysGapBetweenStart(gap == null ? 0 : Integer.parseInt(gap));
            // To confirm - recognise revenue at the full standard price of the bundle - confirmed with Mike in Feb 2017
            pucLine.setPOSCentsPaidEach(freeOverride ? 0 : item.getPriceInCentsIncl());
            pucLine.setPOSCentsDiscountEach(0);
            ucReq.getProvisionUnitCreditLines().add(pucLine);
            log.debug("Added line to provision unit credit spec for item number [{}] on account [{}]", pucLine.getItemNumber(), toAccount);

            bunMarketPriceCents += freeOverride ? 0 : item.getPriceInCentsIncl();
        }

        if (deviceSoldBySmileForCentsPreDiscount > 0) {
            originalDiscountOnDeviceAndBundle = (originalDiscountOnDeviceAndBundle / deviceSoldBySmileForCentsPreDiscount) * 100;
        } else {
            originalDiscountOnDeviceAndBundle = discount; //Default to the original discount of the ICP/SD.
        }

        //Overwite the discount here based on ICP and line item number (as described by HBT-10196)
        //Overwrite device discount based on item number
        if (isICP(orgId)) {
            if (specialICPNettOffDiscountForDeviceAndBundle > 0.00) {
                //discount = specialICPNettOffDiscountForDeviceAndBundle;
                originalDiscountOnDeviceAndBundle = specialICPNettOffDiscountForDeviceAndBundle; //Default to the original discount of the ICP/SD.
            } else {
                originalDiscountOnDeviceAndBundle = discount;
            }
            originalDiscountOnSim = discount;
        }

        if (!foundDevice && deviceSerialNumber != null && !deviceSerialNumber.isEmpty()) {
            throw new Exception("A device serial number was specified for a kit that does not come with a device");
        }

        log.debug("Calling BM");
        try {
            SCAWrapper.getAdminInstance().provisionUnitCredit_Direct(ucReq);
            if (!onlyVerify && BaseUtils.getBooleanProperty("env.bm.social.media.tax.logic.enabled", false)) {
                chargeForSocialMediaTaxOnUnlimitedUnitCredit(xmlSale, xmlSaleLine);
            }

            double exciseTaxRate = BaseUtils.getDoubleProperty("env.sales.excise.tax.percent", 0);
            boolean handleExciseTax = (exciseTaxRate > 0 && !isExeciseDutyNonStockItem(xmlSaleLine.getInventoryItem()));
            if (!onlyVerify && handleExciseTax) {
                sendExciseTaxJournal(xmlSale, xmlSaleLine);
            }

        } catch (Exception e) {
            log.warn("Got exception calling SCA to provision UC: [{}]", e.toString());
            throw e;
        }
        if (!onlyVerify) {
            postProcessingResult.ucTxIds.add(ucReq.getPlatformContext().getTxId());
            double nettOutCents = 0;

            String pd = xmlSaleLine.getProvisioningData();

            SaleRow sRow = DAO.getSaleRowBySaleRowId(em, xmlSaleLine.getLineId());
            
            if (isMegaDealer(sRow.getHeldByOrganisationId())) {
                discountGivenWhenICPorSDBoughtAirtime = BaseUtils.getDoubleProperty("env.pos.megadealer.airtime.discount", 12);
            } else if (isSuperDealer(sRow.getHeldByOrganisationId())) {
                discountGivenWhenICPorSDBoughtAirtime = BaseUtils.getDoubleProperty("env.pos.superdealer.airtime.discount", 10);
            } else if (isFranchise(sRow.getHeldByOrganisationId())) {
                discountGivenWhenICPorSDBoughtAirtime = BaseUtils.getDoubleProperty("env.pos.franchise.airtime.discount", 10);
            } else {
                // For normal ICP
                discountGivenWhenICPorSDBoughtAirtime = BaseUtils.getDoubleProperty("env.pos.icp.airtime.discount", 7);

            }

            if (isMegaDealer(sRow.getHeldByOrganisationId()) || isSuperDealer(sRow.getHeldByOrganisationId()) || isFranchise(sRow.getHeldByOrganisationId())) {
                log.debug("provisionKitsUCs financial results simSoldBySmileForCentsPreDiscount[{}] deviceSoldBySmileForCentsPreDiscount[{}] superDealerUCSoldBySmileForCentsPreDiscount[{}] bunMarketPriceCents[{}] simMarketPriceCents[{}] deviceMarketPriceCents[{}] deviceAndSIMSoldToSDForCentsExclPreDiscount[{}] originalDiscountOnDeviceAndBundle[{}] originalDiscountOnSim[{}]",
                        new Object[]{simSoldBySmileForCentsPreDiscount, deviceSoldBySmileForCentsPreDiscount, superDealerUCSoldBySmileForCentsPreDiscount, bunMarketPriceCents, simMarketPriceCents, deviceMarketPriceCents, deviceAndSIMSoldToSDForCentsExclPreDiscount});
                nettOutCents = getSDNettOut(deviceSoldBySmileForCentsPreDiscount, simSoldBySmileForCentsPreDiscount, superDealerUCSoldBySmileForCentsPreDiscount, bunMarketPriceCents, simMarketPriceCents, deviceMarketPriceCents, originalDiscountOnDeviceAndBundle, originalDiscountOnSim);

                discount = originalDiscountOnDeviceAndBundle;

            } else { // For normal ICP
                log.debug("provisionKitsUCs financial results simSoldBySmileForCentsPreDiscount[{}] deviceSoldBySmileForCentsPreDiscount[{}] superDealerUCSoldBySmileForCentsPreDiscount[{}] bunMarketPriceCents[{}] simMarketPriceCents[{}] deviceMarketPriceCents[{}] deviceAndSIMSoldToSDForCentsExclPreDiscount[{}]", new Object[]{simSoldBySmileForCentsPreDiscount, deviceSoldBySmileForCentsPreDiscount, superDealerUCSoldBySmileForCentsPreDiscount, bunMarketPriceCents, simMarketPriceCents, deviceMarketPriceCents, deviceAndSIMSoldToSDForCentsExclPreDiscount});
                nettOutCents = getICPNettOut(deviceSoldBySmileForCentsPreDiscount, simSoldBySmileForCentsPreDiscount, superDealerUCSoldBySmileForCentsPreDiscount, bunMarketPriceCents, simMarketPriceCents, deviceMarketPriceCents, discount, originalDiscountOnDeviceAndBundle);
            }

            // As agreed with Caroline 07/09/2018, we must deduct the airtime + the discount that was given to superdealer when they purchased the airtime.
            if (nettOutCents < 0) {
                originalDiscountOnAirtimePurchasedIncVat = nettOutCents * (discountGivenWhenICPorSDBoughtAirtime / 100d);
                nettOutCents = nettOutCents + originalDiscountOnAirtimePurchasedIncVat; //Credit the original airtime discount portion
            } else {
                originalDiscountOnAirtimePurchasedIncVat = 0; // No discount to include when we are owing the superdealer or ICP.
            }

            pd = Utils.setValueInCRDelimitedAVPString(pd, "ODD", String.valueOf(Utils.round(originalDiscountOnDeviceAndBundle, 2)));
            pd = Utils.setValueInCRDelimitedAVPString(pd, "OSD", String.valueOf(Utils.round(originalDiscountOnSim, 2)));

            pd = Utils.setValueInCRDelimitedAVPString(pd, "OAD", String.valueOf(discountGivenWhenICPorSDBoughtAirtime)); // ICP Nett out account
            pd = Utils.setValueInCRDelimitedAVPString(pd, "OSP", String.valueOf(Utils.round(simSoldBySmileForCentsPreDiscount, 2)));
            pd = Utils.setValueInCRDelimitedAVPString(pd, "ODP", String.valueOf(Utils.round(deviceSoldBySmileForCentsPreDiscount, 2)));
            pd = Utils.setValueInCRDelimitedAVPString(pd, "OBP", String.valueOf(Utils.round(superDealerUCSoldBySmileForCentsPreDiscount, 2)));
            pd = Utils.setValueInCRDelimitedAVPString(pd, "BMP", String.valueOf(Utils.round(bunMarketPriceCents, 2)));
            pd = Utils.setValueInCRDelimitedAVPString(pd, "SMP", String.valueOf(Utils.round(simMarketPriceCents, 2)));
            pd = Utils.setValueInCRDelimitedAVPString(pd, "DMP", String.valueOf(Utils.round(deviceMarketPriceCents, 2)));
            pd = Utils.setValueInCRDelimitedAVPString(pd, "INO", String.valueOf(Utils.round(nettOutCents, 2))); // ICP Nett out
            pd = Utils.setValueInCRDelimitedAVPString(pd, "ACC", String.valueOf(acc)); // ICP Nett out account

            xmlSaleLine.setProvisioningData(pd);
            log.debug("Set provisioning data on SIM sale line to [{}]", pd);

            log.debug("ICP/SD Who sold to customer should nett out [{}]c", nettOutCents);
            BalanceTransferData btd = new BalanceTransferData();
            btd.setSCAContext(new SCAContext());
            btd.getSCAContext().setTxId("UNIQUE-ICPNO-" + xmlSaleLine.getLineId());
            btd.setAmountInCents(Math.abs(nettOutCents));
            btd.setDescription("Nett Out for Activating " + kitItemNumber + " on " + xmlSaleLine.getInventoryItem().getSerialNumber());

            if (Math.abs(nettOutCents) < 1) { //As agreeed with Caroline on 9/04/2017, nettoffs less than 1c are not posted.
                log.info("NettOff amount is [{}] < 1, there no transfer and no GL will be posted.", nettOutCents);
                return;
            }
            if (nettOutCents > 0) {
                btd.setSourceAccountId(1000000003);
                btd.setTargetAccountId(acc);
                log.debug("Transferring [{}]cents from [{}] to [{}]", new Object[]{btd.getAmountInCents(), btd.getSourceAccountId(), btd.getTargetAccountId()});
                SCAWrapper.getAdminInstance().transferBalance(btd);
                postProcessingResult.transferTxIds.add(btd.getSCAContext().getTxId());
            } else if (nettOutCents < 0) {
                btd.setSourceAccountId(acc);
                btd.setTargetAccountId(1000000003);
                log.debug("Transferring [{}]cents from [{}] to [{}]", new Object[]{btd.getAmountInCents(), btd.getSourceAccountId(), btd.getTargetAccountId()});
                SCAWrapper.getAdminInstance().transferBalance(btd);
                postProcessingResult.transferTxIds.add(btd.getSCAContext().getTxId());
            } else {
                log.debug("Amount is zero so no transfer necessary");
            }

            // Do journal entries
            double taxRate = BaseUtils.getDoubleProperty("env.sales.tax.percent");
            double bunMarketPriceCentsExcl = bunMarketPriceCents / (1 + taxRate / 100.0d);
            double superDealerUCSoldBySmileForCentsPreDiscountExcl = superDealerUCSoldBySmileForCentsPreDiscount / (1 + taxRate / 100.0d);
            double creditUnearnedCentsExcl = (nettOutCents / (1 + taxRate / 100.0d)) + bunMarketPriceCentsExcl - superDealerUCSoldBySmileForCentsPreDiscountExcl; // Airtime transferred less VAT plus bundles that will come through as revenue
            double kitMarketPriceCentsExcl = (bunMarketPriceCents + simMarketPriceCents + deviceMarketPriceCents) / (1 + taxRate / 100.0d);
            // double deviceRevenueCentsExcl = kitRetailCentsExcl - bunMarketPriceCentsExcl - bothSoldBySmileForCentsExclPreDiscount; // The original price before 11% Super dealer discount . bothSoldBySmileForCentsExclPreDiscount is what Smile sold the device and sim for to the SD
            double deviceRevenueCentsExcl = deviceMarketPriceCents / (1 + taxRate / 100.0d) - deviceSoldToSDForCentsExclPreDiscount;// - deviceAndSIMSoldToSDForCentsExclPreDiscount;

            double simRevenueCentsExcl = simMarketPriceCents / (1 + taxRate / 100.0d) - simSoldToSDForCentsExclPreDiscount;

            double icpReducedPurchasePriceExcl = (discount / 100.0d) * (superDealerUCSoldBySmileForCentsPreDiscountExcl - bunMarketPriceCentsExcl)
                    - (originalDiscountOnAirtimePurchasedIncVat / (1 + taxRate / 100.0d)); //Debit the original airtime discount portion
            double deviceAndSIMMarketPriceCentsExcl = kitMarketPriceCentsExcl - bunMarketPriceCentsExcl;

            if (isMegaDealer(sRow.getHeldByOrganisationId()) || isSuperDealer(sRow.getHeldByOrganisationId()) || isFranchise(sRow.getHeldByOrganisationId())) {
                icpDiscountCents = (discount / 100.0d) * (deviceSoldBySmileForCentsPreDiscount - deviceMarketPriceCents) + (originalDiscountOnSim / 100d) * (simSoldBySmileForCentsPreDiscount - simMarketPriceCents); // Discount is the ICP discount % e.g. 8%
                icpDiscountCents = icpDiscountCents / (1 + taxRate / 100.0d);
            } else {
                // icpDiscountCents = (discount / 100.0d) * ((deviceSoldToSDForCentsExclPreDiscount + simSoldToSDForCentsExclPreDiscount) - deviceAndSIMMarketPriceCentsExcl);
                icpDiscountCents = (originalDiscountOnDeviceAndBundle / 100.0d) * (deviceSoldBySmileForCentsPreDiscount - deviceMarketPriceCents) + (discount / 100d) * (simSoldBySmileForCentsPreDiscount - simMarketPriceCents); // Discount is the ICP discount % e.g. 8%
                icpDiscountCents = icpDiscountCents / (1 + taxRate / 100.0d);
            } // Discount is the ICP discount % e.g. 8%

            // 0.08*(500+13984.95 - 10515.05+500+13984.95 - 10515.05)
            // Lets say we reduced the retail kit price to 0:
            // creditUnearnedCentsExcl would be +'ve as the ICP got airtime from us. I.e. CREDIT UNEARNED as the ICP has airtime
            // deviceRevenueCentsExcl would be -'ve I.e. DEBIT DEVICE REVENUE as we ended up not being paid much for the device
            // icpDiscountCents would be +'ve I.e. CREDIT ICP DISCOUNT. A credit here means we paid less in discount. A large -'ve means we are loosing money
            sendICPNettOutJournal(em, creditUnearnedCentsExcl, deviceRevenueCentsExcl, icpReducedPurchasePriceExcl, icpDiscountCents,
                    deviceItemNumber, simRevenueCentsExcl, simItemNumber, "ICPNO-" + xmlSaleLine.getLineId(), xmlSaleLine.getLineId());

        }

        log.debug("Unit Credits unique txid is [{}]", ucReq.getPlatformContext().getTxId());
    }

    private void provisionLinesUC(Sale xmlSale, SaleLine xmlSaleLine, long toAccount, int productInstanceId, boolean onlyVerify, PostProcessingResult postProcessingResult) throws Exception {

        boolean isDynamicBonusBundle = xmlSaleLine.getInventoryItem().getItemNumber().startsWith("BUNPD");

        if (xmlSaleLine.getQuantity() > 100
                && !isDynamicBonusBundle) { // Dynamic bundles have quantity set as units (bytes) size).
            // Prevent issues of salespeople entering thousands by mistake and timing out
            throw new Exception("Cannot purchase that many unit credits at once");
        }

        long dyamicSize = 0;
        // If this bundle is a dynamic bundle, store quantity into a variable and set the line quantity to 1;
        if (isDynamicBonusBundle) {
            dyamicSize = xmlSaleLine.getQuantity();
            xmlSaleLine.setQuantity(1);
        }

        log.debug("This is a unit credit for product instance id [{}]", productInstanceId);
        ProvisionUnitCreditRequest ucReq = new ProvisionUnitCreditRequest();
        ucReq.setPlatformContext(new PlatformContext());
        // Populate account history with the txid of the sale so it can be traced back to the sale
        ucReq.getPlatformContext().setTxId("UNIQUE-UC-SaleLine-" + xmlSaleLine.getLineId()); // Make sure it can only be done once
        ProvisionUnitCreditLine pucLine = new ProvisionUnitCreditLine();
        pucLine.setAccountId(toAccount);
        ucReq.setVerifyOnly(onlyVerify);
        ucReq.setCreditUnearnedRevenue(!xmlSale.getPaymentMethod().equals(PAYMENT_METHOD_AIRTIME));
        ucReq.setSaleLineId(xmlSaleLine.getLineId());
        pucLine.setNumberToProvision((int) xmlSaleLine.getQuantity());
        String gap = Utils.getValueFromCRDelimitedAVPString(xmlSaleLine.getProvisioningData(), "DaysGapBetweenStart");
        pucLine.setDaysGapBetweenStart(gap == null ? 0 : Integer.parseInt(gap));

        pucLine.setInfo(xmlSaleLine.getProvisioningData());

        if (isDynamicBonusBundle) {
            pucLine.setInfo(Utils.setValueInCRDelimitedAVPString(pucLine.getInfo(), "DynamicUCSizeInMB", Long.toString(dyamicSize)));
        }

        double exRate = xmlSale.getTenderedCurrencyExchangeRate() == 0 ? 1 : xmlSale.getTenderedCurrencyExchangeRate();
        log.debug("Sales exchange rate is [{}]", exRate);
        // 2015/10/25
        // PCB - Our revenue recognition in BM is in value incl VAT so if a sale is vat exempt then we must still tell BM they paid VAT to keep the 
        // calculations right or else we will later when sending to X3 remove VAT off an amount that already excludes VAT
        double taxRate = BaseUtils.getDoubleProperty("env.sales.tax.percent");
        double exciseTaxRate = BaseUtils.getDoubleProperty("env.sales.excise.tax.percent", 0);
        double centsEachIncl = (xmlSaleLine.getLineTotalCentsExcl() * (1 + taxRate / 100.0d) * exRate) / xmlSaleLine.getQuantity();
        double discountCentsEachIncl = (xmlSaleLine.getLineTotalDiscountOnExclCents() * (1 + taxRate / 100.0d) * exRate) / xmlSaleLine.getQuantity();

        boolean handleExciseTax = (exciseTaxRate > 0 && !isExeciseDutyNonStockItem(xmlSaleLine.getInventoryItem()));

        if (handleExciseTax) {
            log.debug("Dong tax adjustment for excise duty");
            centsEachIncl = (xmlSaleLine.getLineTotalCentsExcl() * (1 + (exciseTaxRate) / 100.0d) * exRate) / xmlSaleLine.getQuantity();
            centsEachIncl = (centsEachIncl * (1 + (taxRate) / 100.0d));
            discountCentsEachIncl = (xmlSaleLine.getLineTotalDiscountOnExclCents() * (1 + (exciseTaxRate) / 100.0d) * exRate) / xmlSaleLine.getQuantity();
            discountCentsEachIncl = (discountCentsEachIncl * (1 + (taxRate) / 100.0d));
        }

        log.debug("POSCentsPaidEach [{}], and POSCentsDiscountEach[{}]", centsEachIncl, discountCentsEachIncl);

        pucLine.setPOSCentsPaidEach(centsEachIncl);
        pucLine.setPOSCentsDiscountEach(discountCentsEachIncl);
        pucLine.setItemNumber(xmlSaleLine.getInventoryItem().getItemNumber());
        pucLine.setProductInstanceId(productInstanceId);
        ucReq.getProvisionUnitCreditLines().add(pucLine);
        log.debug("About to call BM Direct to provision unit credit spec for item number [{}] on account [{}]", xmlSaleLine.getInventoryItem().getItemNumber(), toAccount);
        try {
            SCAWrapper.getAdminInstance().provisionUnitCredit_Direct(ucReq);
            if (!onlyVerify && BaseUtils.getBooleanProperty("env.bm.social.media.tax.logic.enabled", false)) {
                chargeForSocialMediaTaxOnUnlimitedUnitCredit(xmlSale, xmlSaleLine);
            }
            if (handleExciseTax) {
                sendExciseTaxJournal(xmlSale, xmlSaleLine);
            }
        } catch (Exception e) {
            log.warn("Got exception calling SCA to provision UC: [{}]", e.toString());
            throw e;
        }
        if (!onlyVerify) {
            postProcessingResult.ucTxIds.add(ucReq.getPlatformContext().getTxId());
        }
        log.debug("Unit Credits unique txid is [{}]", ucReq.getPlatformContext().getTxId());
    }

    public void chargeForSocialMediaTaxOnUnlimitedUnitCredit(Sale xmlSale, SaleLine xmlSaleLine) {
        // Do the social media things here ...
        int sleepTime = BaseUtils.getIntProperty("env.bm.rateandbill.socialmedia.sleep.millis", 500); //Default to500 millis.
        if (sleepTime > 0) {
            log.warn("env.bm.rateandbill.socialmedia.sleep.millis [{}]ms. Going to sleep for that time to simulate slow processing", sleepTime);
            Utils.sleep(sleepTime);
            log.warn("Finished sleeping");
        }

        String wrapperClasses = "'DustUnitCredit','',''";

        log.debug("Going to check if unitcredit [{}] under sale id [{}]  is an unlimited bundle and therefore charge for full  "
                + "social media in advance!", xmlSaleLine.getInventoryItem().getItemNumber(), xmlSale.getSaleId());
        // Locate the unlimited bundle for this sale_row_id;
        // Get the latest UnitCreditInstanceQuery just provisioned
        com.smilecoms.commons.sca.direct.bm.UnitCreditInstanceQuery ucq = new com.smilecoms.commons.sca.direct.bm.UnitCreditInstanceQuery();
        //ucq.s
        ucq.setSaleRowId(xmlSaleLine.getLineId());
        ucq.setUnitCreditInstanceId(0);
        ucq.setWrapperClass(wrapperClasses);

        UnitCreditInstanceList uciList = SCAWrapper.getAdminInstance().getUnitCreditInstances_Direct(ucq);

        if (uciList.getNumberOfUnitCreditInstances() > 0) {
            com.smilecoms.commons.sca.direct.bm.UnitCreditInstance uc = uciList.getUnitCreditInstances().get(0);
            //If this is a DustUnitCredit and social media tax is configured, then create a charge for the social media as follows.
            //Make sure social media tax is charged on the listed accounts or to everybody if the list is empty.
            List<String> testAccounts = BaseUtils.getPropertyAsList("env.bm.social.media.tax.test.account.ids");
            if (testAccounts == null || testAccounts.isEmpty() || testAccounts.contains(String.valueOf(uc.getAccountId()))) {
                try {
                    int numDaysForSocialMediaAccess = 0;
                    //Date maxExpirtyDate = DAO.getMaximumExpiryDateOfUnitCreditWithThisTypeExcludingCurrentOne(em, "DustUnitCredit", uc.getAccountId(), uc.getUnitCreditInstanceId());
                    MaximumExpiryDateOfUnitCreditOnAccountQuery maxDateRequest = new MaximumExpiryDateOfUnitCreditOnAccountQuery();

                    maxDateRequest.setAccountId(uc.getAccountId());
                    maxDateRequest.setExcludeUnitCreditInstanceId(uc.getUnitCreditInstanceId());
                    maxDateRequest.setWrapperClass(wrapperClasses);

                    MaximumExpiryDateOfUnitCreditOnAccountReply maxDateReply = SCAWrapper.getAdminInstance().getMaximumExpiryDateOfUnitCreditOnAccount_Direct(maxDateRequest);

                    Date maxExpiryDate = Utils.getJavaDate(maxDateReply.getMaximumExpiryDate());

                    // Compute the number of days that must be used for social media charging
                    Date ucStartDate = Utils.getJavaDate(uc.getStartDate());
                    Date ucExpiryDate = Utils.getJavaDate(uc.getExpiryDate());

                    log.debug("Maximum expiry date is: [{}]", maxExpiryDate);

                    if (maxExpiryDate == null) {
                        numDaysForSocialMediaAccess = Utils.getDaysBetweenDates(ucStartDate, ucExpiryDate);
                    } else {
                        if (maxExpiryDate.before(ucStartDate)) {
                            numDaysForSocialMediaAccess = Utils.getDaysBetweenDates(maxExpiryDate, ucExpiryDate);
                        } else {
                            numDaysForSocialMediaAccess = Utils.getDaysBetweenDates(maxExpiryDate, ucExpiryDate);
                        }
                    }

                    double initialSocialMediaBytesChargeOnDustUnitCredit = 0.0;

                    //Get unit credit specification of the unlimited bundle
                    UnitCreditSpecificationQuery qExtraUcSpec = new UnitCreditSpecificationQuery();
                    qExtraUcSpec.setUnitCreditSpecificationId(uc.getUnitCreditSpecificationId());
                    qExtraUcSpec.setVerbosity(StUnitCreditSpecificationLookupVerbosity.MAIN);
                    UnitCreditSpecificationList ucsList = SCAWrapper.getAdminInstance().getUnitCreditSpecifications(qExtraUcSpec);
                    UnitCreditSpecification ucs = ucsList.getUnitCreditSpecifications().get(0);

                    double dailyNumberOfBytesForSocialMediaTax = Utils.getDoubleValueFromCRDelimitedAVPString(ucs.getConfiguration(), "DailyNumberOfBytesForSocialMediaTax");

                    if (dailyNumberOfBytesForSocialMediaTax > 0) {
                        initialSocialMediaBytesChargeOnDustUnitCredit = numDaysForSocialMediaAccess * dailyNumberOfBytesForSocialMediaTax;
                        log.warn("The pre-charge for social media on unit credit instance [{}] will be [{}] bytes, this is based on daily charge of [{}] bytes and total number of access days is [{}].",
                                new Object[]{uc.getUnitCreditInstanceId(), initialSocialMediaBytesChargeOnDustUnitCredit,
                                    dailyNumberOfBytesForSocialMediaTax, numDaysForSocialMediaAccess});
                    } else {
                        log.warn("DustUnitCredit instance [{}] is not configured with DailyNumberOfBytesForSocialMediaTax, spec id is [{}]", uc.getUnitCreditInstanceId(),
                                ucs.getUnitCreditSpecificationId());
                    }

                    // Check if social media charge is Zero here ...
                    if (initialSocialMediaBytesChargeOnDustUnitCredit <= 0) {
                        log.warn("Social media charge for unit credit [{}] was calculated to be < 0, will not create charge, numDaysForSocialMediaAccess = [{}], property DailyNumberOfBytesForSocialMediaTax = [{}])",
                                new Object[]{uc.getUnitCreditInstanceId(), initialSocialMediaBytesChargeOnDustUnitCredit, dailyNumberOfBytesForSocialMediaTax});
                    } else { //Do the social media charging here
                        String serviceCode = "txtype.socialmedia.tax.unlimited";
                        // Now submit the charge into BM
                        ChargingRequest cr = new ChargingRequest();
                        cr.setPlatformContext(new com.smilecoms.commons.sca.direct.bm.PlatformContext());
                        cr.getPlatformContext().setTxId("UNLIMITED-SOCIAL-MEDIA-CHG-UCI-" + uc.getUnitCreditInstanceId());

                        ChargingData cd = new ChargingData();
                        cr.getChargingData().add(cd);
                        cd.setChargingDataIndex(1);
                        cd.setSessionId(cr.getPlatformContext().getTxId());

                        // The reservation
                        ChargingData cdReservation = new ChargingData();
                        cr.getChargingData().add(0, cdReservation);
                        cdReservation.setSessionId(cr.getPlatformContext().getTxId());
                        cdReservation.setChargingDataIndex(0);
                        cdReservation.setDescription("Social Media Tax Charge on Unlimited UCI " + uc.getUnitCreditInstanceId() + " for " + numDaysForSocialMediaAccess + " days");
                        // cdReservation.setEventTimestamp(Utils.getDateAsXMLGregorianCalendar(uc.getPurchaseDate()));
                        cdReservation.setEventTimestamp(Utils.getDateAsXMLGregorianCalendar(new Date()));
                        cdReservation.setIPAddress("");
                        cdReservation.setLocation("");
                        cdReservation.setRatingKey(new RatingKey());
                        // Tell BM that it must not recognise revenue for this event
                        cdReservation.getRatingKey().setServiceCode(serviceCode);
                        cdReservation.getRatingKey().setRatingGroup(BaseUtils.getProperty("env.bm.rateandbill.socialmedia.rategroup", null)); // For arbitrary monetary charges
                        // cd.getRatingKey().setTo(String.valueOf(xmlSale.getRecipientAccountId()));
                        cdReservation.setServiceInstanceIdentifier(new ServiceInstanceIdentifier());

                        String[] serviceIdentifier = DAO.getSimIdentifierAndIdentifierTypeForAccount(emJTA, uc.getAccountId());

                        cdReservation.getServiceInstanceIdentifier().setIdentifierType(serviceIdentifier[0]);
                        cdReservation.getServiceInstanceIdentifier().setIdentifier(serviceIdentifier[1]);
                        cdReservation.setUserEquipment("");
                        cdReservation.setRequestedServiceUnit(new RequestedServiceUnit());
                        cdReservation.getRequestedServiceUnit().setReservationSecs(10);
                        cdReservation.getRequestedServiceUnit().setTriggerCharged(true);
                        cdReservation.getRequestedServiceUnit().setUnitQuantity(new BigDecimal(initialSocialMediaBytesChargeOnDustUnitCredit));
                        cdReservation.getRequestedServiceUnit().setUnitType("OCTET");
                        // Send the charge.

                        try {
                            ChargingResult res = SCAWrapper.getAdminInstance().rateAndBill_Direct(cr);
                            if (!res.getGrantedServiceUnits().isEmpty() && res.getGrantedServiceUnits().get(0).getErrorCode() != null && !res.getGrantedServiceUnits().get(0).getErrorCode().isEmpty()) {
                                throw new Exception("Error charging for social media tax -- " + res.getGrantedServiceUnits().get(0).getErrorCode());
                            }
                        } catch (Exception e) {
                            log.warn("Error charging for social media tax: [{}]", e.toString());
                            throw e;
                        }

                        // Social media charging was all good, let's try to  reduce the RevenuePer day for the unlimited bundle (as per HBT-8743 and agreed with Caroline)
                        if (Utils.getBooleanValueFromCRDelimitedAVPString(ucs.getConfiguration(), "RecRevDaily")
                                && numDaysForSocialMediaAccess > 0) {
                            double amountToReduceRevenuePerDay = (uc.getPOSCentsCharged() / uc.getUnitsAtStart()) * initialSocialMediaBytesChargeOnDustUnitCredit / ucs.getValidityDays();
                            log.debug("Reducing the Revenue Cents Per Day for unit credit instance [{}], from [{}] by [{}] to [{}]",
                                    new Object[]{uc.getUnitCreditInstanceId(), uc.getRevenueCentsPerDay(), amountToReduceRevenuePerDay,
                                        (uc.getRevenueCentsPerDay() - amountToReduceRevenuePerDay)});
                            uc.setRevenueCentsPerDay(uc.getRevenueCentsPerDay() - amountToReduceRevenuePerDay);
                            SCAWrapper.getAdminInstance().modifyUnitCredit_Direct(uc);
                        }

                        //Send OTT journal to X3
                        double ottPayable = numDaysForSocialMediaAccess * 20000; //To be converted to major currency
                        double ottTaxDeducted = (uc.getPOSCentsCharged() / uc.getUnitsAtStart()) * initialSocialMediaBytesChargeOnDustUnitCredit;
                        sendUnlimitedOttJournal(ottTaxDeducted, "OTT" + xmlSaleLine.getLineId(), xmlSaleLine.getLineId(), ottPayable);
                        //, initialSocialMediaBytesChargeOnDustUnitCredit
                    }
                } catch (Exception ex) {
                    log.error("Error while trying to charge for social media tax for unit credit instance id [{}], [{}]", uc.getUnitCreditInstanceId(), ex);
                } finally {

                }
            }
        }
    }

    private void sendUnlimitedOttJournal(double ottAmount, String glDescription, int saleRowId, double ottPayable) throws Exception {
        //, double socialmediatax
        // As agreed with Mike on 21/7/2015
        // CR Device revenue account for the item number with deviceRevenueCentsExcl
        // CR 4410002 with creditUnearnedCentsExcl
        // DB ICP Discount Acc XXXXXX with icpDiscountCents
        try {
            log.debug("sendUnlimitedOttJournal ottAmount [{}] Description [{}] saleRowID [{}] ottPayable [{}]",
                    new Object[]{ottAmount, glDescription, saleRowId, ottPayable});

            // Credit 4380005 (OTT tax payable)
            // Debit 4410002 (Unearned Revenue)
            // Debit 7210019 (Promo Data Expense) (When OTT < OTT Payable)
            // Credit 7210019   "  "     "        (When OTT > OTT Payable)
            // GL Description = glDescription
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String glEntryDate = sdf.format(new Date());
            String tableName = "OTTBUNU"; // TWC = TPGW Withholding Tax on Commissions

            StringBuilder extraInfo = new StringBuilder();
            extraInfo.append("GL_TYPE=UnlimitedOttGL\r\n")
                    .append("X3_GL_TRANSACTION_CODE=OTTSM\r\n")
                    .append("GL_ENTRY_DATE=" + glEntryDate + "\r\n")
                    .append("GL_DESCRIPTION=" + glDescription + "\r\n")
                    .append("GL_DIMENSION1=91OADH01\r\n")
                    .append("CREDIT_ACCOUNT1=4380005\r\n")
                    .append("CREDIT_AMOUNT1=" + ottPayable + "\r\n") // OOT PAYAbLE
                    .append("DEBIT_ACCOUNT1=4410002\r\n")
                    .append("DEBIT_AMOUNT1=" + ottAmount + "\r\n"); //OTT TAX

            if (ottAmount <= ottPayable) {
                extraInfo.append("DEBIT_ACCOUNT2=7210019\r\n")
                        .append("DEBIT_AMOUNT2=" + (ottPayable - ottAmount) + "\r\n");
            }

            if (ottAmount >= ottPayable) {
                extraInfo.append("CREDIT_ACCOUNT2=7210019\r\n")
                        .append("CREDIT_AMOUNT2=" + (ottAmount - ottPayable) + "\r\n");
            }

//            if (socialmediatax >= 0) {
//                extraInfo.append("SOCIAL_MEDIA_TAX=" + socialmediatax + "\r\n");
//            }
            DAO.insertGLEntry(emJTA, X3InterfaceDaemon.X3_TRANSACTION_TYPE_GL_ENTRY, saleRowId, tableName, extraInfo.toString());
        } catch (Exception ex) {
            log.error("Error while trying to create OTT tax journal ottAmount [{}] Description [{}] saleRowID [{}] ottPayable [{}] - error [{}]",
                    new Object[]{ottAmount, glDescription, saleRowId, ottPayable, ex.getMessage()});
            throw ex;
        }
    }

    private boolean isPostProcessedAlready(SaleLine xmlSaleLine) {
        String postProcessed = Utils.getValueFromCRDelimitedAVPString(xmlSaleLine.getProvisioningData(), "PostProcessed");
        return (postProcessed != null && postProcessed.equals("true"));
    }

    protected void processAirtimeTransfersAfterCashInDeposit(EntityManager emLocal, int cashInId, double paymentCents) throws Exception {
        processAirtimeTransfersAfterCashInDeposit(emLocal, cashInId, paymentCents, null);
    }

    protected void processAirtimeTransfersAfterCashInDeposit(EntityManager emLocal, int cashInId, double paymentCents, String extId) throws Exception {
        log.debug("In processAirtimeTransfersAfterCashIn for cash in id [{}] and amount [{}]cents", cashInId, paymentCents);
        CashIn ci = DAO.getLockedCashIn(emLocal, cashInId);
        if (Math.abs(ci.getCashRequiredInCents().doubleValue() - paymentCents) > 1) {
            throw new Exception("Cashin amount differs");
        }
        if (!ci.getStatus().equals("BDP")) {
            throw new Exception("Cash in by bank deposit has already been processed");
        }
        if (extId != null && !extId.isEmpty()) {
            ci.setExtTxId(extId);
        }
        ci.setCashReceiptedInCents(BigDecimal.valueOf(paymentCents));
        ci.setStatus("BDC"); // Bank deposit Complete
        emLocal.persist(ci);
        emLocal.flush();
        CashInData cid = getXMLCashInData(emLocal, ci);
        processAirtimeTransfersAfterCashIn(emLocal, cid);
    }

    private void processAirtimeTransfersAfterCashIn(EntityManager emLocal, CashInData cashInData) throws Exception {

        // Only replenish cash ins that are in status complete
        if (!cashInData.getStatus().endsWith("C")) {
            log.debug("Status is [{}] so replenishment wont happen yet", cashInData.getStatus());
            return;
        }

        //Give a 1c leniency
        if (BaseUtils.getBooleanProperty("env.pos.cashins.transfer.airtime.on.match.only", true) && Math.abs(cashInData.getCashReceiptedInCents() - cashInData.getCashRequiredInCents()) > 1) {
            log.debug("Incorrect cash in amount receipted. Wont do transfers back to sales persons account");
            String data = String.format("%d|%d|%f|%f", cashInData.getSalesPersonCustomerId(), cashInData.getSalesAdministratorCustomerId(), cashInData.getCashReceiptedInCents(), cashInData.getCashRequiredInCents());
            PlatformEventManager.createEvent("CASHIN_SHORTFALL", "CashIn", String.valueOf(cashInData.getSalesPersonCustomerId()), data);
            return;
        }

        log.debug("Cash receipted equals cash required or env.pos.cashins.transfer.airtime.on.match.only is false. Will transfer airtime sales money back to sales accounts");
        BigDecimal totalCents = new BigDecimal(0);
        Map<Long, BigDecimal> transfersBack = new HashMap<>();
        for (int saleId : cashInData.getSalesIds()) {
            Sale sale = getSale(saleId, emLocal);
            if (!sale.getPaymentMethod().equals(PAYMENT_METHOD_CASH) && !sale.getPaymentMethod().equals(PAYMENT_METHOD_CARD_PAYMENT)) {
                throw new Exception("Only cash sales can be cashed in");
            }
            for (SaleLine line : sale.getSaleLines()) {
                if (!isAirtime(line.getInventoryItem())) {
                    continue;
                }
                log.debug("This line is for an airtime sale");
                Long salesPersonAccountId = sale.getSalesPersonAccountId();
                BigDecimal centsForAccount = transfersBack.get(salesPersonAccountId);
                // PCB 2014-04-16. We must transfer back the amount that was actually debited from the sales person. IE THE AFTER DISCOUNT AMOUNT.
                // Any disocunt would have been paid by a commission account
                // BigDecimal lineCents = new BigDecimal(line.getQuantity() * 100);
                double freeAmntCents = line.getLineTotalDiscountOnInclCents(); // Amount partner gets for free - i.e. the discount
                double totalcents = line.getQuantity() * 100d; // Total airtime quantity 
                BigDecimal baseAmntCents = new BigDecimal(totalcents - freeAmntCents); // The total quantity less the discount. If tax exempt, then this will be more than Smile is actually paid
                if (centsForAccount == null) {
                    centsForAccount = baseAmntCents;
                } else {
                    centsForAccount = centsForAccount.add(baseAmntCents);
                }
                transfersBack.put(salesPersonAccountId, centsForAccount);
                totalCents = totalCents.add(baseAmntCents);
                log.debug("Running total for account [{}] is now [{}]", salesPersonAccountId, centsForAccount);
            }
        }

        if (totalCents.equals(BigDecimal.ZERO)) {
            log.debug("No transfers required");
            return;
        }

        long srcAccount = BaseUtils.getLongProperty("env.sales.airtime.replenishment.account.id");
        AccountQuery aq = new AccountQuery();
        aq.setAccountId(srcAccount);
        aq.setVerbosity(StAccountLookupVerbosity.ACCOUNT);
        Account acc = SCAWrapper.getAdminInstance().getAccount(aq);

        if (acc.getAvailableBalanceInCents() < totalCents.doubleValue()) {
            throw new Exception("Sales replenishment account is too low to transfer funds back to the sale person");
        }

        BalanceTransferData btd = new BalanceTransferData();
        btd.setSCAContext(new SCAContext());
        btd.getSCAContext().setTxId("UNIQUE-CI-" + cashInData.getCashInId());

        for (Long account : transfersBack.keySet()) {
            BigDecimal cents = transfersBack.get(account);
            BalanceTransferLine line = new BalanceTransferLine();
            line.setSourceAccountId(srcAccount);
            line.setTargetAccountId(account);
            line.setAmountInCents(cents.doubleValue());
            btd.getAdditionalTransferLines().add(line);
            log.debug("Transferring [{}]cents from [{}] to [{}]", new Object[]{line.getAmountInCents(), line.getSourceAccountId(), line.getTargetAccountId()});
        }
        SCAWrapper.getAdminInstance().transferBalance(btd);

    }

    public void processPayment(EntityManager em, List<Integer> salesIds, double paymentInCents, String paymentTransactionData, String newStatus, boolean isAutomated, String gatewayCode) throws Exception {
        if (paymentTransactionData.isEmpty()) {
            throw new Exception("Payment transaction data cannot be blank");
        }
        BigDecimal totalCustomerPaysCents = BigDecimal.ZERO;
        String firstStatus = null;
        String salesString = "";

        for (int saleId : salesIds) {
            log.debug("Verifying business rules to process payment on sale id [{}]", saleId);
            salesString += saleId + " ";
            com.smilecoms.pos.db.model.Sale dbSale = DAO.getLockedSale(em, saleId);
            if (firstStatus == null) {
                firstStatus = dbSale.getStatus();
            }
            if (!firstStatus.equals(dbSale.getStatus())) {
                throw new Exception("All sales under one payment must be in the same status");
            }
            totalCustomerPaysCents = totalCustomerPaysCents.add(dbSale.getSaleTotalCentsIncl().subtract(dbSale.getWithholdingTaxCents()));
            if (!dbSale.getPaymentMethod().equals(PAYMENT_METHOD_CHEQUE)
                    && !dbSale.getPaymentMethod().equals(PAYMENT_METHOD_BANK_TRANSFER)
                    && !dbSale.getPaymentMethod().equals(PAYMENT_METHOD_CARD_INTEGRATION)
                    && !dbSale.getPaymentMethod().equals(PAYMENT_METHOD_PAYMENT_GATEWAY)) {
                throw new Exception("Payment cannot be processed on a cash sale");
            }
            if (dbSale.getAmountTenderedCents().doubleValue() != 0
                    && (dbSale.getStatus().equals(PAYMENT_STATUS_PENDING_PAYMENT) || dbSale.getStatus().equals(PAYMENT_STATUS_DELAYED_PAYMENT))) {
                log.debug("Amount tendered is [{}]", dbSale.getAmountTenderedCents());
                throw new Exception("Payment cannot be processed on a sale that has a non zero tendered amount");
            }
            if (!dbSale.getStatus().equals(PAYMENT_STATUS_PENDING_PAYMENT)
                    && !dbSale.getStatus().equals(PAYMENT_STATUS_DELAYED_PAYMENT)
                    && !dbSale.getStatus().equals(PAYMENT_STATUS_PENDING_VERIFICATION)) {
                throw new Exception("Invalid sale status to process payment");
            }
            if ((dbSale.getStatus().equals(PAYMENT_STATUS_PENDING_PAYMENT) || dbSale.getStatus().equals(PAYMENT_STATUS_DELAYED_PAYMENT)) && !newStatus.equals(PAYMENT_STATUS_PENDING_VERIFICATION) && !isAutomated) {
                throw new Exception("Sales pending payment must move to status pending verification");
            }
            if (dbSale.getStatus().equals(PAYMENT_STATUS_PENDING_VERIFICATION) && !newStatus.equals(PAYMENT_STATUS_PAID)) {
                throw new Exception("Sales pending verification must move to status paid");
            }
        }
        log.debug("Expected payment [{}]c and got [{}]", totalCustomerPaysCents, paymentInCents);
        BigDecimal paymentDifferenceInCents = totalCustomerPaysCents.subtract(new BigDecimal(paymentInCents)).abs();
        BigDecimal percentDifference = paymentDifferenceInCents.divide(totalCustomerPaysCents, RoundingMode.HALF_UP).multiply(new BigDecimal(100));

        log.debug("Payment difference is [{}]cents and percentage difference is [{}]", paymentDifferenceInCents, percentDifference);
        if (paymentDifferenceInCents.compareTo(BaseUtils.getBigDecimalProperty("env.pos.allowed.payment.difference.cents", new BigDecimal(100))) > 0
                || percentDifference.compareTo(BaseUtils.getBigDecimalProperty("env.pos.allowed.payment.difference.percent", new BigDecimal(0.1))) > 0) {

            for (int saleId : salesIds) {
                com.smilecoms.pos.db.model.Sale dbSale = DAO.getLockedSale(em, saleId);
                if (Utils.getValueFromCRDelimitedAVPString(dbSale.getExtraInfo(), "PaymentMismatchTicket") != null) {
                    log.debug("Jira ticket was sent previously so just throw error");
                    throw new Exception("Payment amount differs from sum of sales amounts");
                }
                dbSale.setExtraInfo(Utils.setValueInCRDelimitedAVPString(dbSale.getExtraInfo(), "PaymentMismatchTicket", "true"));
                em.persist(dbSale);
                JPAUtils.commitTransaction(em);
            }

            NewTTIssue tt = new NewTTIssue();
            tt.setCustomerId("0");
            tt.setMindMapFields(new MindMapFields());
            JiraField f = new JiraField();
            f.setFieldName("TT_FIXED_FIELD_Description");
            f.setFieldType("TT_FIXED_FIELD");
            f.setFieldValue("Incorrect deposit/s for sale/s: " + salesString.trim() + ". Expected deposits totalling " + totalCustomerPaysCents.divide(new BigDecimal(100)).setScale(2, RoundingMode.HALF_EVEN) + " but got " + Math.round(paymentInCents) / 100);
            tt.getMindMapFields().getJiraField().add(f);

            f = new JiraField();
            f.setFieldName("TT_FIXED_FIELD_Issue Type");
            f.setFieldType("TT_FIXED_FIELD");
            f.setFieldValue("Customer Incident");
            tt.getMindMapFields().getJiraField().add(f);

            f = new JiraField();
            f.setFieldName("TT_FIXED_FIELD_Summary");
            f.setFieldType("TT_FIXED_FIELD");
            f.setFieldValue("Incorrect deposit/s for sale/s: " + salesString.trim());
            tt.getMindMapFields().getJiraField().add(f);

            f = new JiraField();
            f.setFieldName("TT_FIXED_FIELD_Project");
            f.setFieldType("TT_FIXED_FIELD");
            f.setFieldValue(BaseUtils.getProperty("env.locale.country.for.language.en") + "FIN");
            tt.getMindMapFields().getJiraField().add(f);

            f = new JiraField();
            f.setFieldName("SEP Reporter");
            f.setFieldType("TT_FIXED_FIELD");
            f.setFieldValue("admin admin");
            tt.getMindMapFields().getJiraField().add(f);

            f = new JiraField();
            f.setFieldName("Incident Channel");
            f.setFieldType("TT_FIXED_FIELD");
            f.setFieldValue("System");
            tt.getMindMapFields().getJiraField().add(f);
            log.debug("Creating Jira ticket with summary [{}]", "Incorrect deposit/s for sale/s: " + salesString.trim());
            SCAWrapper.getAdminInstance().createTroubleTicketIssue(tt);
            log.debug("Finished creating Jira ticket with summary [{}]", "Incorrect deposit/s for sale/s: " + salesString.trim());

            throw new Exception("Payment amount differs from sum of sales amounts");
        }

        log.debug("Checks are done. These sales can have a payment made against them");

        BigDecimal amountCentsLeftFromPayment = new BigDecimal(paymentInCents);
        PostProcessingResult postProcessingResult = new PostProcessingResult();
        InventorySystem inventorySystem = InventorySystemManager.getInventorySystem();
        try {
            for (int saleId : salesIds) {
                log.debug("Doing payment processing on sale id [{}]", saleId);
                com.smilecoms.pos.db.model.Sale dbSale = DAO.getLockedSale(em, saleId);
                BigDecimal amountForThisSale = dbSale.getSaleTotalCentsIncl().subtract(dbSale.getWithholdingTaxCents());
                BigDecimal amountAllocatedToThisSale;
                if (amountCentsLeftFromPayment.compareTo(amountForThisSale) >= 0) {
                    amountAllocatedToThisSale = amountForThisSale;
                    log.debug("Cents Left from payment is greater or equal than the amount on this sale so this sale can be paid in full. Amount for this sale will be [{}]", amountAllocatedToThisSale);
                } else {
                    amountAllocatedToThisSale = amountCentsLeftFromPayment;
                    log.debug("Cents Left from payment is less than the amount on this sale so this sale cannot be paid in full. Amount for this sale will be [{}]", amountAllocatedToThisSale);
                }
                amountCentsLeftFromPayment = amountCentsLeftFromPayment.subtract(amountAllocatedToThisSale);
                dbSale.setAmountTenderedCents(amountAllocatedToThisSale);
                dbSale.setPaymentTransactionData(paymentTransactionData);
                boolean wasViaPaymentGateway = false;
                if (!isAutomated && (dbSale.getStatus().equals(PAYMENT_STATUS_PENDING_PAYMENT) || dbSale.getStatus().equals(PAYMENT_STATUS_DELAYED_PAYMENT))) {
                    log.debug("Status must move from pending payment (PP) or Delayed Payment (DP) to pending verification (PV)");
                    dbSale.setStatus(PAYMENT_STATUS_PENDING_VERIFICATION);
                    dbSale.setLastModified(new Date());
                    em.persist(dbSale);
                    em.flush();
                } else if (isAutomated || dbSale.getStatus().equals(PAYMENT_STATUS_PENDING_VERIFICATION)) {
                    log.debug("Status must move from pending verification (PV) to paid (PD) or this is an automated payment and need not be verified");
                    if (dbSale.getPaymentMethod().equals(PAYMENT_METHOD_PAYMENT_GATEWAY)) {
                        dbSale.setPaymentMethod(PAYMENT_METHOD_CLEARING_BUREAU);
                        wasViaPaymentGateway = true;
                        dbSale.setExtraInfo(Utils.setValueInCRDelimitedAVPString(dbSale.getExtraInfo(), "PaidDate", Utils.getDateAsString(new Date(), "yyyy/MM/dd")));
                    }
                    if (gatewayCode != null && !gatewayCode.isEmpty() && (dbSale.getPaymentMethod().equals(PAYMENT_METHOD_CARD_INTEGRATION) || dbSale.getPaymentMethod().equals(PAYMENT_METHOD_BANK_TRANSFER) || dbSale.getPaymentMethod().equals(PAYMENT_METHOD_CHEQUE))) {
                        dbSale.setPaymentGatewayCode(gatewayCode);
                    }
                    dbSale.setStatus(PAYMENT_STATUS_PAID);

                    dbSale.setLastModified(new Date());
                    em.persist(dbSale);
                    em.flush();
                    log.debug("System has processed payment. Now going to do post processing on the sale");
                    Sale xmlSale = getXMLSale(em, dbSale, "SALE_LINES");
                    postProcessAllSaleLines(em, xmlSale, postProcessingResult, false, false);

                    log.debug("Resetting transaction and delivery fees on sale now that payment details may be in the sale that allow for better calculations - e.g. credit card facility (visa , mastercard etc)");
                    FeeCalculator feeCalc = new FeeCalculator(xmlSale, em);
                    log.debug("Delivery Model [{}] Delivery Cents [{}] Tx Fee Model [{}] Tx Fee Cents [{}]",
                            new Object[]{xmlSale.getDeliveryFeeModel(), xmlSale.getDeliveryFeeCents(), xmlSale.getTransactionFeeModel(), xmlSale.getTransactionFeeCents()});
                    dbSale.setTransactionFeeCents(new BigDecimal(feeCalc.getTransactionFeeCents()));
                    dbSale.setTransactionFeeModel(feeCalc.getTransactionFeeModel());
                    dbSale.setDeliveryFeeCents(new BigDecimal(feeCalc.getDeliveryFeeCents()));
                    dbSale.setDeliveryFeeModel(feeCalc.getDeliveryFeeModel());
                    em.persist(dbSale);
                    em.flush();

                    if (dbSale.getSalesPersonCustomerId() > 1) {
                        // Dont send to admin
                        PlatformEventManager.createEvent(
                                "POS",
                                "PAYMENT_PROCESSED",
                                String.valueOf(saleId),
                                dbSale.getRecipientCustomerId() + "|" + dbSale.getSalesPersonCustomerId() + "|" + dbSale.getSaleId());
                    }
                }
                log.debug("Done payment processing on sale id [{}]", saleId);

                // If in processpayment and is clearing bureau then it was a payment gateway
                if (dbSale.getPaymentMethod().equals(PAYMENT_METHOD_CLEARING_BUREAU)) {
                    log.debug("This sale was made as a payment gateway payment type. Must now generate and send the invoice to the customer");
                    regenerateAndStoreInvoice(dbSale.getSaleId(), em);
                    Sale xmlSale = getXMLSale(em, dbSale, "SALE_LINES_DOCUMENTS");
                    sendInvoice(xmlSale);
                    boolean hasStockItem = false;
                    if (wasViaPaymentGateway) {
                        log.debug("This is a payment gateway sale on a webshop channel. Going to send delivery note if there are any stock items in the sale");
                        for (SaleLine xmlSaleLine : xmlSale.getSaleLines()) {
                            if (!isAirtime(xmlSaleLine.getInventoryItem()) && !isNonStockItem(xmlSaleLine.getInventoryItem()) && !isUnitCredit(xmlSaleLine.getInventoryItem())) {
                                hasStockItem = true;
                                break;
                            }
                        }
                    }
                    if (hasStockItem && wasViaPaymentGateway) {
                        log.debug("The sale has a stock item paid via payment gateway so a delivery note must be sent");
                        try {
                            sendDeliveryNote(xmlSale, inventorySystem);
                        } catch (Exception e) {
                            log.warn("Error sending delivery note", e);
                        }
                    } else {
                        log.debug("The sale has no stock items or was not via a payment gateway so a delivery note must not be sent");
                    }
                }

            } // End looping through sale ids
        } catch (Exception e) {
            rollbackPostProcessing(postProcessingResult);
            throw e;
        } finally {
            if (inventorySystem != null) {
                inventorySystem.close();
            }
        }
    }

    private void processLoanCompletion(int saleId, EntityManager emLocal) throws Exception {
        com.smilecoms.pos.db.model.Sale dbSale = DAO.getSale(emLocal, saleId);
        if (!dbSale.getPaymentMethod().equals(PAYMENT_METHOD_LOAN)) {
            throw new Exception("This sale was not a loan");
        }
        log.debug("Setting sale status to loan completion");
        dbSale.setStatus(PAYMENT_STATUS_STAFF_OR_LOAN_COMPLETE);
        dbSale.setLastModified(new Date());
        emLocal.persist(dbSale);
        emLocal.flush();
    }

    public void reverseSale(EntityManager emLocal, int saleId) throws Exception {
        com.smilecoms.xml.schema.pos.PlatformContext platformContext = new com.smilecoms.xml.schema.pos.PlatformContext();
        platformContext.setOriginatingIdentity("admin");
        platformContext.setOriginatingIP("0.0.0.0");
        reverseSale(emLocal, saleId, platformContext);
    }

    /**
     * You cant reverse a sale if it: - is not of payment type cash, credit
     * account, bank transfer, cheque - has been cashed in already or - is a non
     * cash sale and non credit account sale and has been paid
     *
     * @param emLocal
     * @param saleId
     * @param platformContext
     * @throws Exception
     */
    public void reverseSale(EntityManager emLocal, int saleId, com.smilecoms.xml.schema.pos.PlatformContext platformContext) throws Exception {
        com.smilecoms.pos.db.model.Sale dbSale = DAO.getSale(emLocal, saleId);

        if (!dbSale.getPaymentMethod().equals(PAYMENT_METHOD_CASH)
                && !dbSale.getPaymentMethod().equals(PAYMENT_METHOD_BANK_TRANSFER)
                && !dbSale.getPaymentMethod().equals(PAYMENT_METHOD_CARD_PAYMENT)
                && !dbSale.getPaymentMethod().equals(PAYMENT_METHOD_AIRTIME)
                && !dbSale.getPaymentMethod().equals(PAYMENT_METHOD_PAYMENT_GATEWAY)
                && !dbSale.getPaymentMethod().equals(PAYMENT_METHOD_CHEQUE)
                && !dbSale.getPaymentMethod().equals(PAYMENT_METHOD_CREDIT_ACCOUNT)
                && !dbSale.getPaymentMethod().equals(PAYMENT_METHOD_CREDIT_FACILITY)
                && !dbSale.getPaymentMethod().equals(PAYMENT_METHOD_QUOTE)
                && !dbSale.getPaymentMethod().equals(PAYMENT_METHOD_SHOP_PICKUP)
                && !dbSale.getPaymentMethod().equals(PAYMENT_METHOD_DELIVERY_SERVICE)
                && !dbSale.getPaymentMethod().equals(PAYMENT_METHOD_CLEARING_BUREAU)
                && !dbSale.getPaymentMethod().equals(PAYMENT_METHOD_CARD_INTEGRATION)
                && !dbSale.getPaymentMethod().equals(PAYMENT_METHOD_CONTRACT)) {
            throw new Exception("Cannot reverse a sale with this payment type -- " + dbSale.getPaymentMethod());
        }

        if (!dbSale.getStatus().equals(PAYMENT_STATUS_PAID)
                && !dbSale.getStatus().equals(PAYMENT_STATUS_PENDING_PAYMENT)
                && !dbSale.getStatus().equals(PAYMENT_STATUS_DELAYED_PAYMENT)
                && !dbSale.getStatus().equals(PAYMENT_STATUS_QUOTE)
                && !dbSale.getStatus().equals(PAYMENT_STATUS_SHOP_PICKUP)
                && !dbSale.getStatus().equals(PAYMENT_STATUS_PENDING_DELIVERY)
                && !dbSale.getStatus().equals(PAYMENT_STATUS_CONTRACT_WAITING)) {
            throw new Exception("Cannot reverse a sale with this status -- " + dbSale.getStatus());
        }

        if (dbSale.getStatus().equals(PAYMENT_STATUS_PAID)
                && !dbSale.getPaymentMethod().equals(PAYMENT_METHOD_CASH)
                && !dbSale.getPaymentMethod().equals(PAYMENT_METHOD_AIRTIME)
                && !dbSale.getPaymentMethod().equals(PAYMENT_METHOD_CLEARING_BUREAU) // Allow or not???
                && !dbSale.getPaymentMethod().equals(PAYMENT_METHOD_CREDIT_ACCOUNT)
                && !dbSale.getPaymentMethod().equals(PAYMENT_METHOD_CREDIT_FACILITY)
                && !dbSale.getPaymentMethod().equals(PAYMENT_METHOD_CARD_PAYMENT)) {
            throw new Exception("Cannot reverse a paid sale of this payment type -- " + dbSale.getPaymentMethod());
        }
        if (DAO.hasSaleBeenCashedIn(emLocal, saleId)) {
            throw new Exception("Cannot reverse a sale that has been cashed in");
        }
        //HBT-8052 -A sale shouldn't be reversed if there is return already done on it.  
        if (DAO.hasSaleHaveReturnedDevice(emLocal, saleId)) {
            throw new Exception("Cannot reverse a sale that has a returned device/s.");
        }

        if (!dbSale.getStatus().equals(PAYMENT_STATUS_DELAYED_PAYMENT)
                && !dbSale.getPaymentMethod().equals(PAYMENT_METHOD_CONTRACT)
                && !Utils.isDateInTimeframe(dbSale.getSaleDateTime(), BaseUtils.getIntProperty("env.pos.max.sale.age.days.cancel", 7), Calendar.DATE)) {
            throw new Exception("Cannot reverse a sale that is very old -- max days old is " + BaseUtils.getIntProperty("env.pos.max.sale.age.days.cancel", 7) + " and sale date is " + dbSale.getSaleDateTime());
        }

        boolean hasGiftBundle = false;
        boolean hasVoucherBox = false;

        for (SaleRow row : DAO.getSalesRowsAndSubRows(emLocal, saleId)) {
            if (isSIM(row)) {
                IMSSubscriptionQuery sq = new IMSSubscriptionQuery();
                sq.setIntegratedCircuitCardIdentifier(row.getSerialNumber());
                sq.setVerbosity(StIMSSubscriptionLookupVerbosity.IMSU);
                boolean isUsed = false;
                try {
                    log.debug("Checking if SIM with ICCID [{}] is being used", row.getSerialNumber());
                    SCAWrapper.getAdminInstance().getIMSSubscription(sq);
                    log.debug("The SIM is being used");
                    isUsed = true;
                } catch (Exception e) {
                    log.debug("ICCID [{}] has no IMSU so its unused", row.getSerialNumber());
                }
                if (isUsed) {
                    throw new Exception("Cannot reverse a sale if the SIM card is in use");
                }
            }
            if (row.getItemNumber().startsWith("BUNP")) {
                hasGiftBundle = true;
            }

            if (row.getItemNumber().startsWith("VOU")) {
                hasVoucherBox = true;
            }
        }

        if (hasVoucherBox) {
            //Cannot cancel a sale that has voucher boxes and has a voucher with status 'DC' or 'RE' redeemed.
            PrepaidStripCountQuery prepaidStripCountQuery = new PrepaidStripCountQuery();
            prepaidStripCountQuery.setStatus(" 'DC', 'RE' ");
            prepaidStripCountQuery.setInvoiceData(String.valueOf(dbSale.getSaleId()));
            com.smilecoms.commons.sca.direct.pvs.PlatformInteger count = SCAWrapper.getAdminInstance().getPrepaidStripCount_Direct(prepaidStripCountQuery);
            if (count.getInteger() > 0) {
                throw new Exception("Cannot reverse a sale of voucher boxes that has some voucher strips in the market (DC) or redeemed (RE).");
            }
        }

        boolean paidSale = false;
        if (dbSale.getStatus().equals(PAYMENT_STATUS_PAID)) {
            if (hasGiftBundle && !Utils.isDateToday(dbSale.getSaleDateTime())) {
                // Promotional bundles are broken at the end of the day in a GL to X3. As per request from Caroline, dont allow a promo bundle to be reversed if its already been broken
                throw new Exception("Cannot reverse a paid sale for a gift bundle unless it was done today");
            }
            log.debug("Any airtime in this sale must be reversed");
            paidSale = true;
        }

        if (dbSale.getStatus().equals(PAYMENT_STATUS_DELAYED_PAYMENT)
                || dbSale.getStatus().equals(PAYMENT_STATUS_PENDING_DELIVERY)
                || dbSale.getStatus().equals(PAYMENT_STATUS_CONTRACT_WAITING)) {
            log.debug("Setting sale status to deleted (DE) as its of status delayed payment or pending delivery or contract waiting and hence need not be reversed in X3");
            dbSale.setStatus(PAYMENT_STATUS_DELETED);
        } else {
            log.debug("Setting sale status to reversed (RV)");
            dbSale.setStatus(PAYMENT_STATUS_INVOICE_REVERSAL);
        }
        dbSale.setLastModified(new Date());
        emLocal.persist(dbSale);
        emLocal.flush();
        Sale sale = getSale(saleId, emLocal);
        if (!sale.getPaymentMethod().equals(PAYMENT_METHOD_AIRTIME)) {
            String identity = null;
            if (platformContext != null && platformContext.getOriginatingIdentity() != null && !platformContext.getOriginatingIdentity().isEmpty()) {
                identity = platformContext.getOriginatingIdentity();
            }
            generateSaleReversal(sale, identity);
            storeSaleReversal(sale, emLocal);
        }
        TransactionReversalData revData = new TransactionReversalData();
        if (paidSale) {

            for (SaleRow row : DAO.getSalesRowsAndSubRows(emLocal, saleId)) {

                if (isAirtime(row)) {
                    String txidRev = "UNIQUE-TFR-SaleLine-" + row.getSaleRowId();
                    log.debug("This sale row is paid and has airtime in it. The airtime transfer with exttxid [{}] must be reversed", txidRev);
                    revData.getTransferExtTxIds().add(txidRev);
                } else if (isUnitCredit(row)) {
                    String txidRev = "UNIQUE-UC-SaleLine-" + row.getSaleRowId();
                    log.debug("This sale row is paid and has a UC in it. The UC provisioning with exttxid [{}] must be reversed", txidRev);
                    revData.getUnitCreditExtTxIds().add(txidRev);
                } else {
                    log.debug("This row id [{}] has nothing to reverse on it", row.getSaleRowId());
                }

            }
        }
        if (paidSale && sale.getPaymentMethod().equals(PAYMENT_METHOD_AIRTIME) && sale.getSaleTotalCentsIncl() != 0) {
            String txidRev = "UNIQUE-CHG-Sale-" + sale.getSaleId() + "_9000";
            log.debug("This is a airtime payment method. Going to reverse the charge for TxId [{}]", txidRev);
            revData.getChargeExtTxIds().add(txidRev);
        }
        if (!revData.getChargeExtTxIds().isEmpty() || !revData.getTransferExtTxIds().isEmpty() || !revData.getUnitCreditExtTxIds().isEmpty()) {
            log.debug("Calling SCA to reverse transactions");
            revData.setSCAContext(new SCAContext());
            revData.getSCAContext().setTxId(Utils.getUUID() + "_POS");
            SCAWrapper.getAdminInstance().reverseTransactions(revData);
        }
        sendSaleReversal(sale);
    }

    private List<CreditNoteRow> getCreditNoteRows(int creditNoteId, EntityManager emLocal) {
        List<CreditNoteRow> ret = new ArrayList<>();
        Collection<SaleReturnRow> returnRows = DAO.getReturnRows(emLocal, creditNoteId);
        for (SaleReturnRow saleReturnRow : returnRows) {
            CreditNoteRow cnr = new CreditNoteRow();
            SaleRow saleRow = DAO.getSaleRowBySaleRowId(emLocal, saleReturnRow.getSaleRowId());
            cnr.itemNumber = saleRow.getItemNumber();
            cnr.quantityLeftForCredit = saleRow.getQuantity();
            cnr.perItemDiscountInCentsExcl = saleRow.getTotalDiscountOnExclCents().divide(new BigDecimal(saleRow.getQuantity()), RoundingMode.HALF_UP);
            cnr.perItemDiscountInCentsIncl = saleRow.getTotalDiscountOnInclCents().divide(new BigDecimal(saleRow.getQuantity()), RoundingMode.HALF_UP);
            cnr.perItemSalesPriceInCentsExcl = saleRow.getTotalCentsExcl().divide(new BigDecimal(saleRow.getQuantity()), RoundingMode.HALF_UP);
            cnr.perItemSalesPriceInCentsIncl = saleRow.getTotalCentsIncl().divide(new BigDecimal(saleRow.getQuantity()), RoundingMode.HALF_UP);
            ret.add(cnr);
        }
        return ret;
    }

    public boolean isEquivalentForReplacement(String newItemNumber, String oldItemNumber, List<String> callersGroups) {
        if (newItemNumber.equals(oldItemNumber)) {
            return true;
        }

        /*
         E.g. 
        
       
        
         MIF6000-MIF6002
         MIF6000-MIF6003 = CorporateSalesManager, DirectSalesManager, RegionalCorporateSalesManager, RegionalDirectSalesManager, RegionalRetailSalesManager, RetailSalesManager, RegionalSDManager, SDNationalHead
         MIF6000-MIF6004
         MIF6000-ROU7006 = CorporateSalesManager, DirectSalesManager, RegionalCorporateSalesManager, RegionalDirectSalesManager, RegionalRetailSalesManager, RetailSalesManager, RegionalSDManager, SDNationalHead
         DON4000-MIF6000 = SDNationalHead
         DON4000-MIF6002 = SDNationalHead
         DON4000-MIF6003 = SDNationalHead
         DON4000-MIF6004 = SDNationalHead
         MIF6002-MIF6000
         MIF6002-MIF6003 = CorporateSalesManager, DirectSalesManager, RegionalCorporateSalesManager, RegionalDirectSalesManager, RegionalRetailSalesManager, RetailSalesManager, RegionalSDManager, SDNationalHead
         MIF6002-MIF6004
         MIF6002-ROU7006 = CorporateSalesManager, DirectSalesManager, RegionalCorporateSalesManager, RegionalDirectSalesManager, RegionalRetailSalesManager, RetailSalesManager, RegionalSDManager, SDNationalHead
         MIF6003-MIF6000
         MIF6003-MIF6002
         MIF6003-MIF6004
         MIF6003-ROU7006 = CorporateSalesManager, DirectSalesManager, RegionalCorporateSalesManager, RegionalDirectSalesManager, RegionalRetailSalesManager, RetailSalesManager, RegionalSDManager, SDNationalHead
         MIF6004-MIF6000
         MIF6004-MIF6002
         MIF6004-MIF6003 = CorporateSalesManager, DirectSalesManager, RegionalCorporateSalesManager, RegionalDirectSalesManager, RegionalRetailSalesManager, RetailSalesManager, RegionalSDManager, SDNationalHead
         MIF6004-ROU7006 = CorporateSalesManager, DirectSalesManager, RegionalCorporateSalesManager, RegionalDirectSalesManager, RegionalRetailSalesManager, RetailSalesManager, RegionalSDManager, SDNationalHead
         ROU7000-ROU7002
         ROU7000-ROU7004
         ROU7000-ROU7006
         ROU7002-ROU7000
         ROU7002-ROU7004
         ROU7002-ROU7006
         ROU7004-ROU7000
         ROU7004-ROU7002
         ROU7004-ROU7006
         ROU7006-MIF6000 = CorporateSalesManager, DirectSalesManager, RegionalCorporateSalesManager, RegionalDirectSalesManager, RegionalRetailSalesManager, RetailSalesManager, RegionalSDManager, SDNationalHead
         ROU7006-MIG6002 = CorporateSalesManager, DirectSalesManager, RegionalCorporateSalesManager, RegionalDirectSalesManager, RegionalRetailSalesManager, RetailSalesManager, RegionalSDManager, SDNationalHead
         ROU7006-MIF6003 = CorporateSalesManager, DirectSalesManager, RegionalCorporateSalesManager, RegionalDirectSalesManager, RegionalRetailSalesManager, RetailSalesManager, RegionalSDManager, SDNationalHead
         ROU7006-MIF6004 = CorporateSalesManager, DirectSalesManager, RegionalCorporateSalesManager, RegionalDirectSalesManager, RegionalRetailSalesManager, RetailSalesManager, RegionalSDManager, SDNationalHead
         ROU7006-ROU7000
         ROU7006-ROU7002
         ROU7006-ROU7004
         SIM8000-SIM8001
         SIM8000-SIM8003
         SIM8000-SIM8004
         SIM8001-SIM8000
         SIM8001-SIM8003
         SIM8001-SIM8004
         SIM8003-SIM8000
         SIM8003-SIM8001
         SIM8003-SIM8004
         SIM8004-SIM8000
         SIM8004-SIM8001
         SIM8004-SIM8003
         SAMD9001-SAMD9002
         SAMD9001-SAME9000 = SDNationalHead
         SAMD9001-SAMG900F 
         SAMD9001-SAMI9505
         SAMD9002-SAMD9001 = CorporateSalesManager, DirectSalesManager, RegionalCorporateSalesManager, RegionalDirectSalesManager, RegionalRetailSalesManager, RetailSalesManager, RegionalSDManager, SDNationalHead
         SAMD9002-SAME9000 = SDNationalHead
         SAMD9002-SAMG900F
         SAMD9002-SAMI9505
         SAME9000-SAMD9001
         SAME9000-SAMD9002
         SAME9000-SAMG900F
         SAME9000-SAMI9505
         SAMG900F-SAMD9001 = SDNationalHead
         SAMG900F-SAMD9002 = SDNationalHead
         SAMG900F-SAME9000 = SDNationalHead
         SAMG900F-SAMI9505
         
         SAMI9505-SAMD9001 = SDNationalHead
         SAMI9505-SAMD9002 = SDNationalHead
         SAMI9505-SAME9000 = SDNationalHead
         SAMI9505-SAMG900F = CorporateSalesManager, DirectSalesManager, RegionalCorporateSalesManager, RegionalDirectSalesManager, RegionalRetailSalesManager, RetailSalesManager, RegionalSDManager, SDNationalHead

        
         */
        log.debug("Caller has security groups [{}]", callersGroups);
        Set<String> rows = Utils.getRoleBasedSubsetOfPropertyAsSet("env.pos.equivalent.items.for.replacement", callersGroups);
        for (String row : rows) {
            log.debug("Looking at config [{}]", row);
            try {
                String broken = row.split("-")[0];
                String replacement = row.split("-")[1];
                if (newItemNumber.equals(replacement) && oldItemNumber.equals(broken)) {
                    log.debug("Replacement is allowed");
                    return true;
                }
            } catch (Exception e) {
                log.warn("Issue in config env.pos.equivalent.items.for.replacement for row [" + row + "]", e);
            }
        }
        log.debug("Replacement is not allowed");
        return false;
    }

    public static boolean isP2PCalendarInvoicingOn(SaleLine line) {
        //See FIN-47 for details
        if (line.getInventoryItem().getItemNumber().startsWith("BENTMW")
                && BaseUtils.getBooleanProperty("env.pos.p2p.calendar.invoincing.enabled", true)
                && (line.getProvisioningData() != null && line.getProvisioningData().contains("P2PCalendarInvoicing"))) {
            String p2pInvoicing = Utils.getValueFromCRDelimitedAVPString(line.getProvisioningData(), "P2PCalendarInvoicing");
            if (p2pInvoicing != null && !p2pInvoicing.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isP2PCalendarInvoicingOn(SaleRow row) {
        //See FIN-47 for details
        if (row.getItemNumber().startsWith("BENTMW")
                && BaseUtils.getBooleanProperty("env.pos.p2p.calendar.invoincing.enabled", true)
                && (row.getProvisioningData() != null && row.getProvisioningData().contains("P2PCalendarInvoicing"))) {
            String p2pInvoicing = Utils.getValueFromCRDelimitedAVPString(row.getProvisioningData(), "P2PCalendarInvoicing");
            if (p2pInvoicing != null && !p2pInvoicing.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isP2PProduct(SaleRow row) {
        if (row.getItemNumber().startsWith("BENTMW")) {
            return true;
        }
        return false;
    }

    public static boolean isP2PProduct(SaleLine line) {
        //See FIN-47 for details
        if (line.getInventoryItem().getItemNumber().startsWith("BENTMW")) {
            return true;
        }
        return false;
    }

    public static boolean isDedicatedInternetBundle(SaleLine line) {
        //
        if (line.getInventoryItem().getItemNumber().startsWith("BENTDI")) {

            return true;
        }
        return false;
    }

    public static boolean isDedicatedInternetBundle(String itemNumber) {
        if (itemNumber.startsWith("BENTDI")) {
            return true;
        }
        return false;
    }

    public static boolean isAirtime(InventoryItem inventoryItem) {
        return isAirtime(inventoryItem.getSerialNumber());
    }

    public static boolean isAirtime(String serialNumber) {
        return serialNumber.equalsIgnoreCase("airtime");
    }

    // For $1 units
    public static boolean isInterconnectMajorCurrency(String itemNumber) {
        return false;  // All interconnect sale items are currently 1c (minor) currency
    }

    // For 1c units
    public static boolean isInterconnectMinorCurrency(String itemNumber) {
        // The following items use Minor Currency (1c each).
        return (itemNumber.startsWith("INVL")
                || // Interconnect Local Currency Voice Item 
                itemNumber.startsWith("INVIU")
                || // Interconnect USD Voice Item
                itemNumber.startsWith("INVIE")
                || // Interconnect EURO Voice Item
                itemNumber.startsWith("INSL")
                || // Interconnect Local SMS Item
                itemNumber.startsWith("INSIE")
                || // Interconnect EURO SMS Item
                itemNumber.startsWith("INSIU")) // Interconnect USD SMS Item
                || itemNumber.startsWith("INTV");
    }

    public static boolean isAirtime(SaleRow row) {
        return isAirtime(row.getSerialNumber());
    }

    public static boolean isUnitCredit(SaleRow row) {
        return isItemNumberUnitCredit(row.getItemNumber());
    }

    public static boolean isUnitCredit(InventoryItem inventoryItem) {
        return isItemNumberUnitCredit(inventoryItem.getItemNumber());
    }

    public static boolean isItemNumberUnitCredit(String itemNumber) {
        if (itemNumber.startsWith("BUN")) {
            return true;
        }
        List<String[]> itemNumberList = BaseUtils.getPropertyFromSQL("global.pos.uc.item.numbers");
        for (String[] row : itemNumberList) {
            if (row[0].equals(itemNumber)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSIM(String itemNumber) {
        return itemNumber.toUpperCase().startsWith("SIM");
    }

    public static boolean isSIM(SaleRow row) {
        return isSIM(row.getItemNumber());
    }

    public static boolean isSIM(SaleLine line) {
        return isSIM(line.getInventoryItem().getItemNumber());
    }

    private boolean isNonX3Item(InventoryItem inventoryItem) {
        return isNonX3Item(inventoryItem.getItemNumber(), inventoryItem.getSerialNumber());
    }

    private boolean isNonX3Item(String itemNumber, String serialNumber) {
        return (itemNumber.endsWith("_NS") || serialNumber.endsWith("_NS"));
    }

    private boolean isNonStockItem(InventoryItem inventoryItem) {
        try {
            Set<String> nonstock = BaseUtils.getPropertyAsSet("env.pos.nonstock.item.prefixes");
            for (String prefix : nonstock) {
                if (inventoryItem.getItemNumber().startsWith(prefix)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("Property env.pos.nonstock.item.prefixes does not exist");
        }
        return false;
    }

    private boolean isExeciseDutyNonStockItem(InventoryItem inventoryItem) {
        try {
            Set<String> nonstock = BaseUtils.getPropertyAsSet("env.pos.execise.duty.nonstock.item.prefixes");
            for (String prefix : nonstock) {
                if (inventoryItem.getItemNumber().startsWith(prefix)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("Property env.pos.execise.duty.nonstock.item.prefixes does not exist");
        }
        return false;
    }

    private boolean isNonStockItem(SaleRow saleRow) {
        try {
            Set<String> nonstock = BaseUtils.getPropertyAsSet("env.pos.nonstock.item.prefixes");
            for (String prefix : nonstock) {
                if (saleRow.getItemNumber().startsWith(prefix)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("Property env.pos.nonstock.item.prefixes does not exist");
        }
        return false;
    }

    void rollbackPostProcessing(PostProcessingResult postProcessingResult) {
        log.debug("In rollbackPostProcessing");

        TransactionReversalData revData = new TransactionReversalData();

        for (String transferTxId : postProcessingResult.transferTxIds) {
            if (transferTxId != null) {
                log.warn("Going to reverse associated balance transfers for Txid [{}]", transferTxId);
                revData.getTransferExtTxIds().add(transferTxId);
            }
        }
        for (String ucTxId : postProcessingResult.ucTxIds) {
            if (ucTxId != null) {
                log.warn("Going to reverse associated unit credit purchases for Txid [{}]", ucTxId);
                revData.getUnitCreditExtTxIds().add(ucTxId);
            }
        }
        for (String chargeTxId : postProcessingResult.chargeTxIds) {
            if (chargeTxId != null) {
                log.warn("Going to reverse associated charge for Txid [{}]", chargeTxId);
                revData.getChargeExtTxIds().add(chargeTxId);
            }
        }
        if (!revData.getChargeExtTxIds().isEmpty() || !revData.getTransferExtTxIds().isEmpty() || !revData.getUnitCreditExtTxIds().isEmpty()) {
            log.debug("Calling SCA to reverse transactions");
            revData.setSCAContext(new SCAContext());
            revData.getSCAContext().setTxId(Utils.getUUID() + "_POS");
            SCAWrapper.getAdminInstance().reverseTransactions(revData);
        }
        log.debug("Finished rollbackPostProcessing");
    }

    private void dealWithDelivery(EntityManager emLocal, com.smilecoms.pos.db.model.Sale dbSale) throws Exception {
        log.debug("In dealWithDelivery");
        if (!dbSale.getStatus().equals(PAYMENT_STATUS_PENDING_DELIVERY)
                || !dbSale.getPaymentMethod().equals(PAYMENT_METHOD_DELIVERY_SERVICE)) {
            throw new Exception("Invalid use of dealWithDelivery");
        }
        log.debug("Going to change payment method to credit account");
        dbSale.setPaymentMethod(PAYMENT_METHOD_CREDIT_ACCOUNT);
        dbSale.setStatus(PAYMENT_STATUS_PAID);
        Collection<SaleRow> rows = DAO.getSalesRowsAndSubRows(emLocal, dbSale.getSaleId());
        String warehouseId = getStockLocation(emLocal, rows);
        if (warehouseId == null) {
            log.debug("There were no stock items in this sale. No need to change the warehouse id from [{}]", dbSale.getWarehouseId());
        } else {
            log.debug("The stock in the sale has moved from [{}] to [{}]", dbSale.getWarehouseId(), warehouseId);
            for (SaleRow row : rows) {
                if (!row.getWarehouseId().isEmpty()) {
                    log.debug("Setting sale row [{}] to warehouse id [{}]", row.getSaleRowId(), warehouseId);
                    row.setWarehouseId(warehouseId);
                    emLocal.persist(row);
                }
            }
            dbSale.setWarehouseId(warehouseId);
        }

        // Can be removed along with the property when new webshop goes live as then the sale will have the correct details to start with
        if (BaseUtils.getPropertyAsSet("env.pos.webshop.channels").contains(dbSale.getChannel())) {
            log.debug("This sale must have the details of the end customer on the invoice");
            Customer recipientOfDelivery = getCustomer(dbSale.getRecipientAccountId());
            dbSale.setOrganisationName("");
            dbSale.setRecipientCustomerId(recipientOfDelivery.getCustomerId());
            dbSale.setRecipientName(recipientOfDelivery.getFirstName() + " " + recipientOfDelivery.getLastName());
            dbSale.setRecipientOrganisationId(0);
            dbSale.setRecipientPhoneNumber(recipientOfDelivery.getAlternativeContact1());
        }

        emLocal.persist(dbSale);
        emLocal.flush();

        regenerateAndStoreInvoice(dbSale.getSaleId(), emLocal);
        sendInvoice(getXMLSale(emLocal, dbSale, "SALE_LINES_DOCUMENTS"));
    }

    private String getStockLocation(EntityManager emLocal, Collection<SaleRow> rows) throws Exception {
        InventorySystem inventorySystem = null;
        try {
            for (SaleRow row : rows) {
                if (isAirtime(row.getSerialNumber())
                        || isUnitCredit(row)
                        || isNonX3Item(row.getItemNumber(), row.getSerialNumber())
                        || isNonStockItem(row)
                        || row.getSerialNumber().isEmpty()) {
                    continue;
                }
                inventorySystem = InventorySystemManager.getInventorySystem();
                return inventorySystem.getInventoryWarehouseId(row.getSerialNumber());
            }
        } finally {
            if (inventorySystem != null) {
                inventorySystem.close();
            }
        }
        return null;
    }

    @Override
    public CashInList getCashIns(CashInQuery cashInQuery) throws POSError {
        setContext(cashInQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        CashInList results = new CashInList();
        try {
            int salesAdministratorCustomerId = ((cashInQuery.getSalesAdministratorCustomerId() == null) ? 0 : cashInQuery.getSalesAdministratorCustomerId());
            String status = (cashInQuery.getStatus() == null ? "" : cashInQuery.getStatus());
            String cashInType = (cashInQuery.getCashInType() == null ? "" : cashInQuery.getCashInType());

            int cashInId = ((cashInQuery.getCashInId() == null) ? 0 : cashInQuery.getCashInId());

            if (cashInId > 0) { // Search for cashIns by CashInId
                CashIn cIn = DAO.getCashIn(emJTA, cashInId);
                results.getCashInDataList().add(getXMLCashInData(emJTA, cIn));
                results.setNumberOfCashIns(results.getCashInDataList().size());
            } else if (salesAdministratorCustomerId > 0 && !status.isEmpty() && !cashInType.isEmpty()) {
                Collection<CashIn> cashIns = DAO.getCashInsBySalesAdministratorCustomerIdStatusAndCashInType(emJTA, salesAdministratorCustomerId, cashInType, status);
                // Set saleids list for each cashin
                log.debug("GetCashIns found [{}] cashins.", cashIns.size());
                for (CashIn cashIn : cashIns) {
                    results.getCashInDataList().add(getXMLCashInData(emJTA, cashIn));
                }
                results.setNumberOfCashIns(results.getCashInDataList().size());
            } else if (salesAdministratorCustomerId == 0 && !status.isEmpty() && !cashInType.isEmpty()) {
                Collection<CashIn> cashIns = DAO.getCashInsByStatusAndCashInType(emJTA, cashInType, status);
                log.debug("GetCashIns by type and status found [{}] cashins.", cashIns.size());
                for (CashIn cashIn : cashIns) {
                    results.getCashInDataList().add(getXMLCashInData(emJTA, cashIn));
                }
                results.setNumberOfCashIns(results.getCashInDataList().size());
            } else { // Do not know how to handle this search
                throw new Exception("Don't know how to search for cashins!");
            }
        } catch (Exception e) {
            throw processError(POSError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        log.debug("GetCashIns results found [{}] cashins.", results.getCashInDataList().size());
        return results;

    }

    @Override
    public PlatformInteger addContract(Contract newContract) throws POSError {
        setContext(newContract, wsctx);

        if (log.isDebugEnabled()) {
            logStart();
        }

        PlatformInteger contractId = new PlatformInteger();
        com.smilecoms.pos.db.model.Contract dbContract = new com.smilecoms.pos.db.model.Contract();

        try {
            if (newContract.getPaymentMethod().equals(PAYMENT_METHOD_AIRTIME) && newContract.getAccountId() == 0) {
                throw new Exception("Airtime payment method needs an account id to charge");
            }
            if (newContract.getPaymentMethod().equals(PAYMENT_METHOD_CREDIT_ACCOUNT) && (newContract.getCreditAccountNumber() == null || newContract.getCreditAccountNumber().isEmpty())) {
                throw new Exception("Credit Account payment method needs a credit account number to charge");
            }
            syncXMLContractIntodbContract(newContract, dbContract);

            dbContract.setStatus("AC");
            dbContract.setLastModifiedDateTime(new Date());
            dbContract.setCreatedDateTime(new Date());
            // Set the user who created this contract ...
            dbContract.setCreatedByCustomerProfileId(newContract.getCreatedByCustomerId());

            try {
                emJTA.persist(dbContract);
                emJTA.flush();
            } catch (javax.persistence.PersistenceException e) {
                log.warn("Error persisting new contract: {}", e.toString());
                throw new Exception("Duplicate contract");
            }
            emJTA.refresh(dbContract);
            contractId.setInteger(dbContract.getContractId());
            // Now set the attached documents - if there were any attached...
            log.debug("Contract [{}] has [{}] documents to be attached...", dbContract.getContractId(), newContract.getContractDocuments().size());
            if (!newContract.getContractDocuments().isEmpty()) {
                DAO.setContractDocuments(emJTA, newContract.getContractDocuments(), dbContract.getContractId());
            }

            createEvent(dbContract.getContractId());
            StringBuilder data = new StringBuilder();
            data.append(dbContract.getContractId());
            PlatformEventManager.createEvent("NEW_CONTRACT", "", String.valueOf(dbContract.getContractId()), data.toString());

        } catch (Exception e) {
            throw processError(POSError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return contractId;
    }

    @Override
    public ContractList getContracts(ContractQuery contractQuery) throws POSError {

        setContext(contractQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }

        ContractList contractList = new ContractList();
        try {

            int contractId = ((contractQuery.getContractId() == null) ? 0 : contractQuery.getContractId());
            int customerProfileId = ((contractQuery.getCustomerId() == null) ? 0 : contractQuery.getCustomerId());
            int organisationId = ((contractQuery.getOrganisationId() == null) ? 0 : contractQuery.getOrganisationId());

            // Preference on search by contractId ...
            if (contractId > 0) {
                log.debug("Searching for a contract with contract id [{}]", contractId);
                com.smilecoms.pos.db.model.Contract dbContract = DAO.getContract(emJTA, contractId);
                Contract xmlContract = new Contract();
                syncDBContractIntoXMLContract(emJTA, dbContract, xmlContract);
                contractList.getContracts().add(xmlContract);
                contractList.setNumberOfContracts(1);
            } else if (customerProfileId > 0) { // Search for contacts by customer profile id
                log.debug("Searching for contracts associated with customer with profile id [{}]", customerProfileId);
                for (com.smilecoms.pos.db.model.Contract dbContract : DAO.getContractsByCustomerProfileId(emJTA, customerProfileId)) {
                    Contract xmlContract = new Contract();
                    syncDBContractIntoXMLContract(emJTA, dbContract, xmlContract);
                    contractList.getContracts().add(xmlContract);
                }
                contractList.setNumberOfContracts(contractList.getContracts().size());
            } else if (organisationId > 0) { // Then search by Organisation Id ...
                log.debug("Searching for contracts associated with customer with profile id [{}]", customerProfileId);
                for (com.smilecoms.pos.db.model.Contract dbContract : DAO.getContractsByOrganisationId(emJTA, organisationId)) {
                    Contract xmlContract = new Contract();
                    syncDBContractIntoXMLContract(emJTA, dbContract, xmlContract);

                    contractList.getContracts().add(xmlContract);
                }
                contractList.setNumberOfContracts(contractList.getContracts().size());
            } else { // System is confused!
                throw new Exception("Don't know how to search for contracts!");
            }

        } catch (Exception e) {
            throw processError(POSError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return contractList;
    }

    @Override
    public Done deleteContract(PlatformInteger contractId) throws POSError {
        setContext(contractId, wsctx);

        if (log.isDebugEnabled()) {
            logStart();
        }

        try {
            com.smilecoms.pos.db.model.Contract dbContract = emJTA.find(com.smilecoms.pos.db.model.Contract.class, contractId.getInteger());
            dbContract.setStatus(CONTRACT_STATUS.DE.toString());
            dbContract.setLastModifiedDateTime(new Date());
            emJTA.persist(dbContract);
            emJTA.flush();
            createEvent(contractId.getInteger());
        } catch (Exception e) {
            throw processError(POSError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public Done modifyContract(Contract modifiedContract) throws POSError {
        setContext(modifiedContract, wsctx);

        if (log.isDebugEnabled()) {
            logStart();
        }

        try {
            com.smilecoms.pos.db.model.Contract dbContract = emJTA.find(com.smilecoms.pos.db.model.Contract.class, modifiedContract.getContractId());
            syncXMLContractIntodbContract(modifiedContract, dbContract);
            dbContract.setLastModifiedDateTime(new Date());
            try {
                emJTA.persist(dbContract);
                emJTA.flush();
            } catch (javax.persistence.PersistenceException e) {
                log.warn("Error persisting contract: {}", e.toString());
                throw new Exception("Duplicate contract");
            }

            log.debug("Contract [{}] has [{}] documents to be attached...", modifiedContract.getContractId(), modifiedContract.getContractDocuments().size());

            if (!modifiedContract.getContractDocuments().isEmpty()) {
                DAO.setContractDocuments(emJTA, modifiedContract.getContractDocuments(), modifiedContract.getContractId());
            }
            createEvent(dbContract.getContractId());
        } catch (Exception e) {
            throw processError(POSError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    private void putBackInGatewayQueue(int saleId, EntityManager em) {
        com.smilecoms.pos.db.model.Sale dbSale = DAO.getSale(em, saleId);
        if (dbSale.getPaymentMethod().equals(PAYMENT_METHOD_PAYMENT_GATEWAY)) {
            log.debug("This sale is a payment gateway sale with status [{}]", dbSale.getStatus());
            if (dbSale.getStatus().equals(PAYMENT_STATUS_GW_FAIL_SMILE)
                    || dbSale.getStatus().equals(PAYMENT_STATUS_GW_FAIL_GW)
                    || dbSale.getStatus().equals(PAYMENT_STATUS_INVOICE_REVERSAL)) {
                dbSale.setStatus(PAYMENT_STATUS_PENDING_PAYMENT);
                log.debug("The sale has a failure or is reversed so we can put it back into polling");
                dbSale.setPaymentGatewayNextPollDate(null);
                dbSale.setPaymentGatewayPollCount(0);
                dbSale.setPaymentGatewayExtraData(null);
                em.persist(dbSale);
                em.flush();
            }
        }
    }

    private double getExchangeRate(String localCurrency, String tenderedCurrency) throws Exception {
        if (localCurrency.equals(tenderedCurrency)) {
            return 1;
        }
        log.debug("Getting exchange rate for currency [{}] to local currency [{}]", tenderedCurrency, localCurrency);
        double rate = X3Helper.getExchangeRate(localCurrency, tenderedCurrency);
        log.debug("Exchange rate is [{}]", rate);
        return rate;
    }

    @Override
    public Done processReturnOrReplacement(Return returnOrReplacement) throws POSError {
        InventorySystem inventorySystem = null;
        try {
            inventorySystem = InventorySystemManager.getInventorySystem();
            Sale sale = getSaleBySaleLineId(returnOrReplacement.getSaleLineId(), emJTA);
            Date saleDateTime = Utils.getJavaDate(sale.getSaleDate());
            if (returnOrReplacement.getReplacementItem() == null) { // Then this is a new Return ....

                if (!sale.getPaymentMethod().equals(PAYMENT_METHOD_CASH)
                        && !sale.getPaymentMethod().equals(PAYMENT_METHOD_BANK_TRANSFER)
                        && !sale.getPaymentMethod().equals(PAYMENT_METHOD_CARD_PAYMENT)
                        && !sale.getPaymentMethod().equals(PAYMENT_METHOD_PAYMENT_GATEWAY)
                        && !sale.getPaymentMethod().equals(PAYMENT_METHOD_CHEQUE)
                        && !sale.getPaymentMethod().equals(PAYMENT_METHOD_CREDIT_NOTE) // PCB Added this 2013/10/24... why not allow it ;-)
                        && !sale.getPaymentMethod().equals(PAYMENT_METHOD_CREDIT_ACCOUNT)
                        && !sale.getPaymentMethod().equals(PAYMENT_METHOD_CARD_INTEGRATION)
                        && !sale.getPaymentMethod().equals(PAYMENT_METHOD_CREDIT_FACILITY)) {
                    throw new Exception("Cannot do a return on a sale with this payment type");
                }

                if (!inventorySystem.doesLocationExist(returnOrReplacement.getLocation())) {
                    throw new Exception("Invalid location to return the stock back to -- " + returnOrReplacement.getLocation());
                }

                Calendar X3StartDate = Calendar.getInstance();
                X3StartDate.set(2013, 5, 26, 0, 0, 0); // Month is 0 base

                if (saleDateTime.before(X3StartDate.getTime())) {
                    throw new Exception("Cannot process returns on sales made prior to 26 June 2013 -- " + X3StartDate + " is before " + saleDateTime);
                }

                if (!sale.getStatus().equals(PAYMENT_STATUS_PAID)) {
                    throw new Exception("Cannot do a return on a sale which is not paid");
                }

                SaleRow row = DAO.getSaleRowBySaleRowId(emJTA, returnOrReplacement.getSaleLineId());
                if (isAirtime(row)) {
                    throw new Exception("Airtime cannot be returned");
                }

                if (isUnitCredit(row) && !returnOrReplacement.getPlatformContext().getOriginatingIdentity().equals("admin")) {
                    throw new Exception("Unit Credit cannot be returned");
                }

                log.debug("Going to create a new return for sale_row_id [{}].", returnOrReplacement.getSaleLineId());
                ReturnReplacement dbReturn = DAO.createReturnOrReplacement(emJTA, null, returnOrReplacement.getSaleLineId(), returnOrReplacement.getCreatedByCustomerId(), returnOrReplacement.getReasonCode(), returnOrReplacement.getDescription(), returnOrReplacement.getLocation(), null, null, returnOrReplacement.getReturnedSerialNumber(), returnOrReplacement.getReturnedItemNumber(), null);

                generateAndPersistReturn(sale, row, dbReturn, emJTA);
                sendReturn(sale, dbReturn);
                returnOrReplacement.setReturnId(dbReturn.getReturnReplacementId());

                createEvent(returnOrReplacement.getSaleLineId());
            } else { // Create a replacement here

                Customer salesPerson = getCustomer(returnOrReplacement.getCreatedByCustomerId());

                if (!isEquivalentForReplacement(returnOrReplacement.getReplacementItem().getItemNumber(), returnOrReplacement.getReturnedItemNumber(), salesPerson.getSecurityGroups())) {
                    throw new Exception("Invalid item replacement attempted -- Item number [" + returnOrReplacement.getReturnedItemNumber() + "] cannot be replaced by item number [" + returnOrReplacement.getReplacementItem().getItemNumber() + "]");
                }

                // com.smilecoms.pos.db.model.Sale sale = DAO.getSaleByLineId(emJTA, returnOrReplacement.getSaleLineId());
                if (!inventorySystem.doesLocationExist(returnOrReplacement.getLocation())) {
                    throw new Exception("Invalid location to use for the stock replacement -- " + returnOrReplacement.getLocation());
                }

                Calendar X3StartDate = Calendar.getInstance();
                X3StartDate.set(2013, 5, 26, 0, 0, 0); // Month is 0 base

                if (saleDateTime.before(X3StartDate.getTime())) {
                    throw new Exception("Cannot process returns on sales made prior to 26 June 2013 -- " + X3StartDate.getTime() + " is before " + saleDateTime);
                }

                // ToDo: Do Warranty Dates Validations here ... user admin should have previledges to allow replacement outside warranty periods.
                Calendar warrantyEndDate = Calendar.getInstance();
                if (!isWarrantyStillValid(emJTA, sale, returnOrReplacement.getSaleLineId(), warrantyEndDate)
                        && !returnOrReplacement.getPlatformContext().getOriginatingIdentity().equals("admin")) {
                    throw new Exception("Item warranty has expired -- Expiry date was " + warrantyEndDate.getTime());
                }

                if (!sale.getStatus().equals(PAYMENT_STATUS_PAID)) {
                    throw new Exception("Cannot do a replacement on a sale which is not paid");
                }

                log.debug("Going to create a  replacement for sale_row_id [{}].", returnOrReplacement.getSaleLineId());
                ReturnReplacement dbReturn = DAO.createReturnOrReplacement(emJTA, returnOrReplacement.getParentReturnId(), returnOrReplacement.getSaleLineId(), returnOrReplacement.getCreatedByCustomerId(), returnOrReplacement.getReasonCode(), returnOrReplacement.getDescription(), returnOrReplacement.getLocation(), returnOrReplacement.getReplacementItem().getItemNumber(), returnOrReplacement.getReplacementItem().getSerialNumber(), returnOrReplacement.getReturnedSerialNumber(), returnOrReplacement.getReturnedItemNumber(), returnOrReplacement.getReplacementItem().getDescription());

                SaleRow row = DAO.getSaleRowBySaleRowId(emJTA, returnOrReplacement.getSaleLineId());

                generateAndPersistReplacement(sale, row, dbReturn, emJTA);
                sendReplacement(sale, dbReturn);

                returnOrReplacement.setReturnId(dbReturn.getReturnReplacementId());

                createEvent(returnOrReplacement.getSaleLineId());

            }
        } catch (Exception e) {
            throw processError(POSError.class, e);
        } finally {
            if (inventorySystem != null) {
                inventorySystem.close();
            }
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public Done processPaymentNotification(PaymentNotificationData paymentNotificationData) throws POSError {
        setContext(paymentNotificationData, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        Done done = new Done();
        EntityManager emRL = JPAUtils.getEM(emfRL);
        try {
            log.debug("Starting transaction on entity manager for processPaymentNotificationData [SALE_ID:{}, AMT:{}, GATEWAY_CODE:{}, EXT_ID:{}]",
                    new Object[]{paymentNotificationData.getSaleId(), paymentNotificationData.getPaymentInCents(),
                        paymentNotificationData.getPaymentGatewayCode(), paymentNotificationData.getPaymentGatewayTransactionId()});

            JPAUtils.beginTransaction(emRL);
            PaymentGatewayPlugin plugin = PaymentGatewayDaemon.getPlugin(emRL, paymentNotificationData.getPaymentGatewayCode());
            plugin.processPaymentNotification(
                    paymentNotificationData.getSaleId(),
                    paymentNotificationData.getPaymentInCents(),
                    paymentNotificationData.getPaymentGatewayTransactionId(),
                    paymentNotificationData.getPaymentGatewayExtraData() == null ? "" : paymentNotificationData.getPaymentGatewayExtraData()
            );

            if (emRL.isOpen() && emRL.getTransaction().isActive()) {
                log.debug("Committing transaction for processPaymentNotification");
                JPAUtils.commitTransaction(emRL);
            }

            done.setDone(StDone.TRUE);
        } catch (Exception e) {
            JPAUtils.rollbackTransaction(emRL);
            throw processError(POSError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return done;
    }

    private boolean isWarrantyStillValid(EntityManager em, Sale sale, int saleRowId, Calendar warrantyEndDate) {
        Calendar now = Calendar.getInstance();
        // First check warranty based on original sales date. 
        Date wDate = null;

        // If the device was bought through a super dealer or an ICP, then use the date that it was provisioned.
        SaleRow saleRow = DAO.getSaleRowBySaleRowId(em, saleRowId);
        if (saleRow.getProvisioningData() != null && saleRow.getProvisioningData().contains("PPDate")) {
            wDate = Utils.getDateFromString(Utils.getValueFromCRDelimitedAVPString(saleRow.getProvisioningData(), "PPDate"), "yyyy/MM/dd HH:mm:ss");
            if (wDate == null) {
                wDate = Utils.getJavaDate(sale.getSaleDate());
            }
        } else {
            wDate = Utils.getJavaDate(sale.getSaleDate());
        }

        warrantyEndDate.setTime(wDate);
        //Add the configured warranty period
        warrantyEndDate.add(Calendar.MONTH, BaseUtils.getIntProperty("env.pos.standard.warranty.months", 12));
        log.error("isWarrantyStillValid: Warranty end date is: [{}], now is [{}]", warrantyEndDate.getTime(), now.getTime());
        // Check if original warrantly has expired? 
        if (warrantyEndDate.getTime().before(now.getTime())) {

            return false; //Warranty has expired...

            /* //Original warranty has expired, check the replacement warranty of the most recently replacement item.
            Date replacementWarrantyEndDate = DAO.getWarrantyEndDateOfMostRecentReplacementItem(em, saleRowId);
            if (replacementWarrantyEndDate == null) { // No replacement was done before - so simply return false, indicating the warranty has expired.
                return false;
            } else {
                //Check if the replacement warranty is still valid?
                warrantyEndDate.setTime(replacementWarrantyEndDate);
                warrantyEndDate.add(Calendar.MONTH, BaseUtils.getIntProperty("env.pos.replacement.warranty.months", 3));
                log.debug("isWarrantyStillValid: Warranty end date is: [{}], now is [{}]", warrantyEndDate.getTime(), now.getTime());
                return warrantyEndDate.getTime().before(now.getTime());
            } */
        } else {
            log.debug("Warranty for item is still valid and will expire on [{}]", warrantyEndDate.getTime());
            return true; // We are still within the orginal 12 months warranty period.
        }
    }

    private void generateAndPersistReturn(Sale sale, SaleRow saleRowBeingReturned, ReturnReplacement dbReturn, EntityManager emLocal) throws Exception {
        log.debug("Generating sale return");
        StringBuilder xml = new StringBuilder();
        xml.append("<CreditNote>");
        xml.append("<CreditNoteId>");
        xml.append(dbReturn.getReturnReplacementId());
        xml.append("</CreditNoteId>");
        xml.append("<Description>");
        xml.append(StringEscapeUtils.escapeXml(dbReturn.getDescription()));
        xml.append("</Description>");
        xml.append("<ReasonCode>");
        xml.append(StringEscapeUtils.escapeXml(dbReturn.getReasonCode()));
        xml.append("</ReasonCode>");
        xml.append("<DateTime>");
        xml.append(dbReturn.getCreatedDateTime());
        xml.append("</DateTime>");
        xml.append("<SalesRowsReturned>");
        double total = 0;

        // Row specific
        xml.append("<SaleRow>");
        xml.append("<ItemNumber>");
        xml.append(dbReturn.getReturnedItemNumber());
        xml.append("</ItemNumber>");
        xml.append("<SerialNumber>");
        xml.append(StringEscapeUtils.escapeXml(dbReturn.getReturnedSerialNumber()));
        xml.append("</SerialNumber>");
        xml.append("<Quantity>");
        xml.append(saleRowBeingReturned.getQuantity());
        xml.append("</Quantity>");
        xml.append("<Description>");
        xml.append(StringEscapeUtils.escapeXml(saleRowBeingReturned.getDescription()));
        xml.append("</Description>");
        xml.append("<TotalCentsIncl>");
        xml.append(saleRowBeingReturned.getTotalCentsIncl());
        xml.append("</TotalCentsIncl>");
        xml.append("</SaleRow>");
        total += saleRowBeingReturned.getTotalCentsIncl().doubleValue();

        xml.append("</SalesRowsReturned>");

        xml.append("<CreditNoteTotalCents>");
        xml.append(total);
        xml.append("</CreditNoteTotalCents>");
        xml.append(marshallObject(sale));
        xml.append("<Customer>");
        Customer cust = getCustomer(sale.getRecipientCustomerId());
        Organisation org = getOrganisation(sale.getRecipientOrganisationId());
        xml.append(getCustomerXML(cust, org, sale.getRecipientAccountId()));
        xml.append("</Customer>");
        xml.append("<SalesPerson>");
        Customer salesPerson = getCustomer(dbReturn.getCreatedByCustomerProfileId());
        xml.append(getSalesPersonXML(salesPerson, 0));
        xml.append("</SalesPerson>");
        xml.append("<CreditAccountNumber>");
        xml.append(StringEscapeUtils.escapeXml(sale.getCreditAccountNumber()));
        xml.append("</CreditAccountNumber>");
        xml.append("</CreditNote>");
        log.debug("Credit Note XML is [{}]", xml);
        byte[] pdf = PDFUtils.generateLocalisedPDF("sales.return.email.pdf.attachment.xslt", xml.toString(), LocalisationHelper.getDefaultLocale(), getClass().getClassLoader());
        dbReturn.setReturnReplacementPDF(pdf);
        log.debug("Persisting return to sale return row");
        emLocal.persist(dbReturn);
        emLocal.flush();
    }

    private void generateAndPersistReplacement(Sale sale, SaleRow saleRowBeingReturned, ReturnReplacement dbReturn, EntityManager emLocal) throws Exception {
        log.debug("Generating sale return");
        StringBuilder xml = new StringBuilder();
        xml.append("<CreditNote>");
        xml.append("<CreditNoteId>");
        xml.append(dbReturn.getReturnReplacementId());
        xml.append("</CreditNoteId>");
        xml.append("<Description>");
        xml.append(StringEscapeUtils.escapeXml(dbReturn.getDescription()));
        xml.append("</Description>");
        xml.append("<ReasonCode>");
        xml.append(StringEscapeUtils.escapeXml(dbReturn.getReasonCode()));
        xml.append("</ReasonCode>");
        xml.append("<DateTime>");
        xml.append(dbReturn.getCreatedDateTime());
        xml.append("</DateTime>");
        xml.append("<SalesRowsReturned>");
        double total = 0;

        // Row specific
        xml.append("<SaleRow>");
        xml.append("<ItemNumber>");
        xml.append(dbReturn.getReturnedItemNumber());
        xml.append("</ItemNumber>");
        xml.append("<SerialNumber>");
        xml.append(StringEscapeUtils.escapeXml(dbReturn.getReturnedSerialNumber()));
        xml.append("</SerialNumber>");
        xml.append("<Quantity>");
        xml.append(saleRowBeingReturned.getQuantity());
        xml.append("</Quantity>");
        xml.append("<Description>");
        xml.append(StringEscapeUtils.escapeXml(saleRowBeingReturned.getDescription()));
        xml.append("</Description>");
        xml.append("<TotalCentsIncl>");
        xml.append(saleRowBeingReturned.getTotalCentsIncl());
        xml.append("</TotalCentsIncl>");
        xml.append("</SaleRow>");
        total += saleRowBeingReturned.getTotalCentsIncl().doubleValue();

        xml.append("</SalesRowsReturned>");

        xml.append("<SalesRowsReplacement>");
        // Row specific
        xml.append("<SaleRow>");
        xml.append("<ItemNumber>");
        xml.append(dbReturn.getReplacementItemNumber());
        xml.append("</ItemNumber>");
        xml.append("<SerialNumber>");
        xml.append(StringEscapeUtils.escapeXml(dbReturn.getReplacementSerialNumber()));
        xml.append("</SerialNumber>");
        xml.append("<Quantity>");
        xml.append(saleRowBeingReturned.getQuantity());
        xml.append("</Quantity>");
        xml.append("<Description>");
        xml.append(StringEscapeUtils.escapeXml(dbReturn.getReplacementItemDescription()));
        xml.append("</Description>");
        xml.append("<TotalCentsIncl>");
        xml.append(saleRowBeingReturned.getTotalCentsIncl());
        xml.append("</TotalCentsIncl>");
        xml.append("</SaleRow>");
        total += saleRowBeingReturned.getTotalCentsIncl().doubleValue();

        xml.append("</SalesRowsReplacement>");

        xml.append("<CreditNoteTotalCents>");
        xml.append(total);
        xml.append("</CreditNoteTotalCents>");
        xml.append(marshallObject(sale));
        xml.append("<Customer>");
        Customer cust = getCustomer(sale.getRecipientCustomerId());
        Organisation org = getOrganisation(sale.getRecipientOrganisationId());
        xml.append(getCustomerXML(cust, org, sale.getRecipientAccountId()));
        xml.append("</Customer>");
        xml.append("<SalesPerson>");
        Customer salesPerson = getCustomer(dbReturn.getCreatedByCustomerProfileId());
        xml.append(getSalesPersonXML(salesPerson, 0));
        xml.append("</SalesPerson>");
        xml.append("<CreditAccountNumber>");
        xml.append(StringEscapeUtils.escapeXml(sale.getCreditAccountNumber()));
        xml.append("</CreditAccountNumber>");
        xml.append("</CreditNote>");
        log.debug("Credit Note XML is [{}]", xml);
        byte[] pdf = PDFUtils.generateLocalisedPDF("sales.replacement.email.pdf.attachment.xslt", xml.toString(), LocalisationHelper.getDefaultLocale(), getClass().getClassLoader());
        dbReturn.setReturnReplacementPDF(pdf);
        log.debug("Persisting replacement to sale return row");
        emLocal.persist(dbReturn);
        emLocal.flush();
    }

    private void sendReturn(Sale sale, ReturnReplacement dbReturn) throws Exception {
        log.debug("Sending credit note to customer via email");
        if (sale.getRecipientCustomerId() == 0) {
            log.debug("This is an anonymous sale to the credit note cannot be emailed");
            return;
        }
        try {
            CustomerCommunicationData email = new CustomerCommunicationData();
            email.setAttachmentBase64(Utils.encodeBase64(dbReturn.getReturnReplacementPDF()));
            email.setSubjectResourceName("sales.return.email.subject");
            email.setBodyResourceName("sales.return.email.body");
            email.setAttachmentFileName("Warranty Return - " + dbReturn.getReturnReplacementId() + ".pdf");
            email.setCustomerId(sale.getRecipientCustomerId());
            email.setBCCAddress(BaseUtils.getProperty("env.sales.invoice.bcc.email.address"));
            email.setBlocking(false);
            SCAWrapper.getAdminInstance().sendCustomerCommunication(email);
            log.debug("Sent a sale return");
        } catch (Exception e) {
            log.warn("Error sending credit note", e);
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "POS", "Error sending credit note: " + e.toString());
        }
    }

    private void sendReplacement(Sale sale, ReturnReplacement dbReturn) throws Exception {
        log.debug("Sending credit note to customer via email");
        if (sale.getRecipientCustomerId() == 0) {
            log.debug("This is an anonymous sale to the credit note cannot be emailed");
            return;
        }
        try {
            CustomerCommunicationData email = new CustomerCommunicationData();
            email.setAttachmentBase64(Utils.encodeBase64(dbReturn.getReturnReplacementPDF()));
            email.setSubjectResourceName("sales.replacement.email.subject");
            email.setBodyResourceName("sales.replacement.email.body");
            email.setAttachmentFileName("Warranty Replacement - " + dbReturn.getReturnReplacementId() + ".pdf");
            email.setCustomerId(sale.getRecipientCustomerId());
            email.setBCCAddress(BaseUtils.getProperty("env.sales.invoice.bcc.email.address"));
            email.setBlocking(false);
            SCAWrapper.getAdminInstance().sendCustomerCommunication(email);
            log.debug("Sent a sale return");
        } catch (Exception e) {
            log.warn("Error sending credit note", e);
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "POS", "Error sending credit note: " + e.toString());
        }
    }

    // For when called via modify sale
    private void sendDeliveryNote(int saleId, int deliveryCustomerId, EntityManager emLocal, InventorySystem inventorySystem) throws Exception {
        log.debug("In sendDeliveryNote for sale id [{}] and customer id[{}]", saleId, deliveryCustomerId);
        StringBuilder xml = new StringBuilder();
        Sale sale = getSale(saleId, emLocal);
        if (!sale.getPaymentMethod().equals(PAYMENT_METHOD_DELIVERY_SERVICE) && !sale.getPaymentMethod().equals(PAYMENT_METHOD_SHOP_PICKUP)) {
            throw new Exception("This is not a delivery service nor shop pickup sale -- Payment method is " + sale.getPaymentMethod());
        }
        xml.append("<Invoice>");
        xml.append(marshallObject(sale));
        xml.append("<Customer>");
        Customer cust = getCustomer(deliveryCustomerId);
        xml.append(getCustomerXML(cust, null, 0));
        xml.append("</Customer>");
        xml.append("<Location>");
        xml.append(getLocationForWarehouse(inventorySystem, sale.getWarehouseId()));
        xml.append("</Location>");
        xml.append("</Invoice>");
        log.debug("Delivery note XML is [{}]", xml);
        byte[] pdf = PDFUtils.generateLocalisedPDF("delivery.note.email.pdf.attachment.xslt", xml.toString(), LocalisationHelper.getDefaultLocale(), getClass().getClassLoader());

        IMAPUtils.sendEmail(BaseUtils.getProperty("env.pos.deliverynote.from.email.address", "deliverynotes@smilecoms.com"), BaseUtils.getProperty("env.pos.deliverynote.to.email.address"), null, null, "Delivery note for sale " + saleId, "Attached is a delivery note", "Delivery note " + saleId + ".pdf", pdf);
        log.debug("Sent a sale delivery note");

    }

    // For when sale is made
    private void sendDeliveryNote(Sale sale, InventorySystem inventorySystem) throws Exception {
        log.debug("In sendDeliveryNote for sale id [{}] and customer id[{}]", sale.getSaleId(), sale.getRecipientCustomerId());
        StringBuilder xml = new StringBuilder();
        xml.append("<Invoice>");
        xml.append(marshallObject(sale));
        xml.append("<Customer>");
        Customer cust = getCustomer(sale.getRecipientCustomerId());
        xml.append(getCustomerXML(cust, null, 0));
        xml.append("</Customer>");
        xml.append("<Location>");
        xml.append(getLocationForWarehouse(inventorySystem, sale.getWarehouseId()));
        xml.append("</Location>");
        xml.append("</Invoice>");
        log.debug("Delivery note XML is [{}]", xml);
        byte[] pdf = PDFUtils.generateLocalisedPDF("delivery.note.email.pdf.attachment.xslt", xml.toString(), LocalisationHelper.getDefaultLocale(), getClass().getClassLoader());
        IMAPUtils.sendEmail(BaseUtils.getProperty("env.pos.deliverynote.from.email.address", "deliverynotes@smilecoms.com"), BaseUtils.getProperty("env.pos.deliverynote.to.email.address"), null, null, "Delivery note for sale " + sale.getSaleId(), "Attached is a delivery note", "Delivery note " + sale.getSaleId() + ".pdf", pdf);
        log.debug("Sent a sale delivery note");
    }

    private void resendSaleToX3(EntityManager em, int saleId) {
        int updates = 0;
        updates += DAO.deleteFailedX3TransactionStateForSale(em, saleId);
        updates += DAO.deleteFailedX3RequestStateForSale(em, saleId);
        log.debug("Updates [{}]", updates);
        if (updates > 0) {
            com.smilecoms.pos.db.model.Sale sale = DAO.getSale(em, saleId);
            sale.setLastModified(new Date());
            em.persist(sale);
        }
    }

    private void resendCashInToX3(EntityManager em, int saleId) {
        int updates = 0;
        CashIn cashin = DAO.getCashInBySaleId(em, saleId);
        updates += DAO.deleteFailedX3TransactionStateForCashIn(em, cashin.getCashInId());
        updates += DAO.deleteFailedX3RequestStateForCashIn(em, cashin.getCashInId());
        log.debug("Updates [{}]", updates);
        if (updates > 0) {
            cashin.setCashInDateTime(new Date());
            em.persist(cashin);
        }
    }

    private String getAutoPopulateSerial(InventorySystem inventorySystem, EntityManager em, String itemNumber, String warehouseId) throws Exception {
        return inventorySystem.getAvailableSerialNumber(em, itemNumber, warehouseId);
    }

    private boolean isSaleGeneratedFromShopPickup(String saleExtraInfo) throws Exception {
        String shopPickupId = getFieldValueFromExtraInfo(saleExtraInfo, "ShopPickupId");
        return (shopPickupId != null && shopPickupId.length() > 0);
    }

    private String getWarehouseForLocation(InventorySystem inventorySystem, String location) throws Exception {
        String warehouseId = inventorySystem.getWarehouseForLocation(location);
        log.debug("Warehouse Id for location [{}] is [{}]", location, warehouseId);
        return warehouseId;
    }

    private String getLocationForWarehouse(InventorySystem inventorySystem, String warehouseId) throws Exception {
        String location = inventorySystem.getLocationForWarehouse(warehouseId);
        log.debug("Location for warehouse id [{}] is [{}]", warehouseId, location);
        return location;
    }

    private SaleLine cloneSaleLine(SaleLine line) {
        SaleLine cloned = new SaleLine();
        cloned.setInventoryItem(cloneInventoryItem(line.getInventoryItem()));
        cloned.setQuantity(line.getQuantity());
        for (SaleLine subLine : line.getSubSaleLines()) {
            cloned.getSubSaleLines().add(cloneSaleLine(subLine));
        }
        return cloned;
    }

    private InventoryItem cloneInventoryItem(InventoryItem inventoryItem) {
        InventoryItem cloned = new InventoryItem();
        cloned.setCurrency(inventoryItem.getCurrency());
        cloned.setDescription(inventoryItem.getDescription());
        cloned.setItemNumber(inventoryItem.getItemNumber());
        cloned.setSerialNumber(inventoryItem.getSerialNumber());
        cloned.setWarehouseId(inventoryItem.getWarehouseId());
        return cloned;
    }

    private void parseComments(Sale sale) {
        for (SaleLine l : sale.getSaleLines()) {
            if (l.getComment() != null) {
                l.setComment(parseComment(l.getComment()));
            }
        }
    }

    private String parseComment(String comment) {
        Date now = new Date();
        comment = comment.replaceAll("MMMM", new SimpleDateFormat("MMMM").format(now));
        comment = comment.replaceAll("MMM", new SimpleDateFormat("MMM").format(now));
        comment = comment.replaceAll("yyyy", new SimpleDateFormat("yyyy").format(now));
        return comment;
    }

    private SoldStockLocation getXMLSoldStockLocation(EntityManager em, SaleRow row) throws Exception {
        SoldStockLocation soldLocation = new SoldStockLocation();
        com.smilecoms.pos.db.model.Sale dbSale = DAO.getSaleByLineId(em, row.getSaleRowId());
        soldLocation.setDescription(row.getDescription());
        soldLocation.setHeldByOrganisationId(row.getHeldByOrganisationId() == null ? 0 : row.getHeldByOrganisationId());
        soldLocation.setItemNumber(row.getItemNumber());
        soldLocation.setSerialNumber(row.getSerialNumber());
        soldLocation.setSoldToOrganisationId(dbSale.getRecipientOrganisationId());
        soldLocation.setPriceInCentsIncl(row.getUnitPriceCentsIncl().doubleValue());
        soldLocation.setSaleDate(Utils.getDateAsXMLGregorianCalendar(dbSale.getSaleDateTime()));
     //   soldLocation.setProvisionData(row.getProvisioningData());
        
        log.debug("Calling sale row by ParentSaleRowId: {}", row.getParentSaleRowId());
        com.smilecoms.pos.db.model.SaleRow kitRow;
        if (row.getParentSaleRowId() > 0) {
            kitRow = DAO.getSaleRowBySaleRowId(em, row.getParentSaleRowId());
            log.debug("Found sale row by ParentSaleRowId, setting KitPrice with new sale row for stock: {}--{}", kitRow.getItemNumber(), kitRow.getUnitPriceCentsIncl().doubleValue());
            soldLocation.setKitPrice(kitRow.getUnitPriceCentsIncl().doubleValue());
        } else {
            log.debug("No ParentSaleRowId, going to set KitPrice with same sale row, for stock: {}--{}", row.getItemNumber(), row.getUnitPriceCentsIncl().doubleValue());
            soldLocation.setKitPrice(row.getUnitPriceCentsIncl().doubleValue());
        }

        return soldLocation;
    }

    private long getICPNettOutAccount(int orgId) throws Exception {
        long acc;
        // Look in the discount code comments to get the info
        String discCode = BaseUtils.getProperty("env.pos.discount.code");
        // 12345 ACC: 1307000001 DISC: 15%
        int configStart = discCode.indexOf(" " + orgId + " ACC: ");
        String bit = discCode.substring(configStart);
        int accStart = bit.indexOf(" ACC: ") + 6;
        bit = bit.substring(accStart).trim();
        acc = Long.parseLong(bit.substring(0, 10));
        log.debug("Org Id [{}] must have nett out payments made to [{}]", orgId, acc);
        return acc;
    }

    private double getICPDiscount(int orgId) throws Exception {
        // Look in the discount code comments to get the info
        String discCode = BaseUtils.getProperty("env.pos.discount.code");
        // 12345 ACC: 1307000001 DISC: 15% TYPE: SD
        // 12347 ACC: 1307000002 DISC: 10% TYPE: ICP
        int configStart = discCode.indexOf(" " + orgId + " ACC: ");
        if (configStart == -1) {
            throw new Exception("Salespersons Organisation is not set up as an ICP under a Super Dealer -- " + orgId);
        }
        String bit = discCode.substring(configStart);
        int discStart = bit.indexOf(" DISC: ") + 7;
        bit = bit.substring(discStart).trim();
        double disc = Utils.getFirstNumericPartOfString(bit);
        log.debug("Org Id [{}] has discount of [{}]%", orgId, disc);
        return disc;
    }

    private double getSpecialICPNettOutDiscountForItemNumber(String itemNumber) {
        Map<String, String> specialDiscountMapping; // = new HashMap<>();
        double dDiscount = 0.00;

        try {
            specialDiscountMapping = BaseUtils.getPropertyAsMap("env.pos.nettoff.items.special.discount.mapping");
            if (specialDiscountMapping != null) {
                String stDiscount = specialDiscountMapping.get(itemNumber);

                if (stDiscount != null && !stDiscount.isEmpty()) {
                    dDiscount = Double.valueOf(stDiscount);
                    log.warn("The special nettout discount for line item [{}] will be set to [{}]", itemNumber, dDiscount);
                }
            }
        } catch (Exception ex) {
            log.error("Error while trying to get special nettout discount for item [{}]", itemNumber);
        }
        return dDiscount;
    }
    
    public static boolean isSuperDealer(int orgId) {
        if (orgId == 0) {
            return false;
        }
        // Look in the discount code comments to get the info
        String discCode = BaseUtils.getProperty("env.pos.discount.code");
        // 12345 ACC: 1307000001 DISC: 15% TYPE: SD
        // 12347 ACC: 1307000002 DISC: 10% TYPE: ICP
        int configStart = discCode.indexOf(" " + orgId + " ACC: ");
        if (configStart == -1) {
            return false;
        }
        String bit = discCode.substring(configStart);
        int typeStart = bit.indexOf(" TYPE: ") + 7;
        if (typeStart == -1) {
            return false;
        }
        bit = bit.substring(typeStart).trim();
        return bit.startsWith("SD");
    }

    public static boolean isMegaDealer(int orgId) {
        if (orgId == 0) {
            return false;
        }
        // Look in the discount code comments to get the info
        String discCode = BaseUtils.getProperty("env.pos.discount.code");
        // 12345 ACC: 1307000001 DISC: 15% TYPE: SD
        // 12347 ACC: 1307000002 DISC: 10% TYPE: ICP
        int configStart = discCode.indexOf(" " + orgId + " ACC: ");
        if (configStart == -1) {
            return false;
        }
        String bit = discCode.substring(configStart);
        int typeStart = bit.indexOf(" TYPE: ") + 7;
        if (typeStart == -1) {
            return false;
        }
        bit = bit.substring(typeStart).trim();
        return bit.startsWith("MD");
    }

    public static boolean isICP(int orgId) {
        if (orgId == 0) {
            return false;
        }
        // Look in the discount code comments to get the info
        String discCode = BaseUtils.getProperty("env.pos.discount.code");
        // 12345 ACC: 1307000001 DISC: 15% TYPE: SD
        // 12347 ACC: 1307000002 DISC: 10% TYPE: ICP
        int configStart = discCode.indexOf(" " + orgId + " ACC: ");
        if (configStart == -1) {
            return false;
        }
        String bit = discCode.substring(configStart);
        int typeStart = bit.indexOf(" TYPE: ") + 7;
        if (typeStart == -1) {
            return false;
        }
        bit = bit.substring(typeStart).trim();
        return bit.startsWith("ICP");
    }

    public static boolean isFranchise(int orgId) {
        if (orgId == 0) {
            return false;
        }
        // Look in the discount code comments to get the info
        String discCode = BaseUtils.getProperty("env.pos.discount.code");
        // 12345 ACC: 1307000001 DISC: 15% TYPE: SD
        // 12347 ACC: 1307000002 DISC: 10% TYPE: ICP
        // 12347 ACC: 1307000002 DISC: 10% TYPE: FRA
        int configStart = discCode.indexOf(" " + orgId + " ACC: ");
        if (configStart == -1) {
            return false;
        }
        String bit = discCode.substring(configStart);
        int typeStart = bit.indexOf(" TYPE: ") + 7;
        if (typeStart == -1) {
            return false;
        }
        bit = bit.substring(typeStart).trim();
        return bit.startsWith("FRA");
    }

    // Is this an indirect sales channel?
    public static boolean isIndirectSales(String channel) {
        if (channel == null) {
            return false;
        }

        return channel.startsWith("26P");
    }

    private double getSDNettOut(double deviceSoldBySmileForCentsPreDiscount, double simSoldBySmileForCentsPreDiscount,
            double superDealerUCSoldBySmileForCentsPreDiscount, double bunMarketPriceCents,
            double simMarketPriceCents, double deviceMarketPriceCents,
            double sdDeviceAndBundleDiscountPercent, double sdSimDiscountPercent) {

        double deviceAndBundleDiscountFactor = (1 - (sdDeviceAndBundleDiscountPercent / 100));
        log.debug("Device discount factor [{}]", deviceAndBundleDiscountFactor);

        double sdSimDiscountFactor = (1 - (sdSimDiscountPercent / 100));
        log.debug("SIM discount factor [{}]", sdSimDiscountFactor);

        double originalSalePricePreDiscount = deviceAndBundleDiscountFactor * (deviceSoldBySmileForCentsPreDiscount + superDealerUCSoldBySmileForCentsPreDiscount) + sdSimDiscountFactor * simSoldBySmileForCentsPreDiscount; // ODSP_Pre_Disc
        log.debug("Original discounted sale price [{}]", originalSalePricePreDiscount);

        // As agreed with Mike on 21/7/2016 Airtime to transfer = D X RP - RP + ODSP_Pre_Disc X ( 1 - D)
        double totalCustomerPaid = deviceAndBundleDiscountFactor * (bunMarketPriceCents + deviceMarketPriceCents) + sdSimDiscountFactor * simMarketPriceCents; // RP
        log.debug("Discounted total paid by customer [{}]", totalCustomerPaid);

        double totalSmileOwesICP = originalSalePricePreDiscount - totalCustomerPaid;

        log.debug("Calculation [{}]", totalSmileOwesICP);
        return Utils.round(totalSmileOwesICP, 0);

    }

    private double getICPNettOut(double deviceSoldBySmileForCentsPreDiscount, double simSoldBySmileForCentsPreDiscount,
            double superDealerUCSoldBySmileForCentsPreDiscount, double bunMarketPriceCents,
            double simMarketPriceCents, double deviceMarketPriceCents, double icpDiscountPercent, double originalDiscountOnDeviceAndBundle) {

        // As agreed with Mike on 21/7/2016 Airtime to transfer = D X RP - RP + ODSP_Pre_Disc X ( 1 - D)
        /* double totalCustomerPaid = bunMarketPriceCents + simMarketPriceCents + deviceMarketPriceCents; // RP
        log.debug("Total customer paid [{}]", totalCustomerPaid);

        double originalSalePricePreDiscount = deviceSoldBySmileForCentsPreDiscount + simSoldBySmileForCentsPreDiscount + superDealerUCSoldBySmileForCentsPreDiscount; // ODSP_Pre_Disc
        log.debug("Original sale price pre discount[{}]", originalSalePricePreDiscount);

        double discountFactor = icpDiscountPercent / 100;
        log.debug("Discount factor [{}]", discountFactor);
        double totalSmileOwesICP = discountFactor * (bunMarketPriceCents + simMarketPriceCents + deviceMarketPriceCents) 
                - totalCustomerPaid + originalSalePricePreDiscount * (1 - discountFactor);

        log.debug("Calculation [{}]", totalSmileOwesICP); */
        double simNetting = (simSoldBySmileForCentsPreDiscount - simMarketPriceCents) * (1 - icpDiscountPercent / 100);
        log.debug("SIM Nett [{}]", simNetting);

        double deviceAndBundleNetting = ((superDealerUCSoldBySmileForCentsPreDiscount + deviceSoldBySmileForCentsPreDiscount) - (bunMarketPriceCents + deviceMarketPriceCents)) * (1 - originalDiscountOnDeviceAndBundle / 100);
        log.debug("Device and Bundle Nett [{}]", deviceAndBundleNetting);

        double totalSmileOwesICP = simNetting + deviceAndBundleNetting;
        log.debug("Total NettOut [{}]", totalSmileOwesICP);

        return Utils.round(totalSmileOwesICP, 0);

    }

    private double getICPNettOutOld(double deviceSoldBySmileForCentsPreDiscount, double simSoldBySmileForCentsPreDiscount,
            double superDealerUCSoldBySmileForCentsPreDiscount, double bunMarketPriceCents,
            double simMarketPriceCents, double deviceMarketPriceCents, double icpDiscountPercent, double originalDiscountOnDeviceAndBundle) {

        // As agreed with Mike on 21/7/2016 Airtime to transfer = D X RP - RP + ODSP_Pre_Disc X ( 1 - D)
        double totalCustomerPaid = bunMarketPriceCents + simMarketPriceCents + deviceMarketPriceCents; // RP
        log.debug("Total customer paid [{}]", totalCustomerPaid);

        double originalSalePricePreDiscount = deviceSoldBySmileForCentsPreDiscount + simSoldBySmileForCentsPreDiscount + superDealerUCSoldBySmileForCentsPreDiscount; // ODSP_Pre_Disc
        log.debug("Original sale price pre discount[{}]", originalSalePricePreDiscount);

        double discountFactor = icpDiscountPercent / 100;
        log.debug("Discount factor [{}]", discountFactor);
        double totalSmileOwesICP = discountFactor * totalCustomerPaid - totalCustomerPaid + originalSalePricePreDiscount * (1 - discountFactor);

        log.debug("Calculation [{}]", totalSmileOwesICP);

        return Utils.round(totalSmileOwesICP, 0);

    }

    private void sendWHTaxOnCommissionJournal(EntityManager em, double whtAmount, String glDescription, int saleRowId) throws Exception {
        // As agreed with Mike on 21/7/2015
        // CR Device revenue account for the item number with deviceRevenueCentsExcl
        // CR 4410002 with creditUnearnedCentsExcl
        // DB ICP Discount Acc XXXXXX with icpDiscountCents

        // Credit 4380004 (Withholding tax payable)
        // Debit 4410002 (Unearned Revenue)
        // GL Description = glDescription
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String glEntryDate = sdf.format(new Date());
        String tableName = "TWC" + saleRowId; // TWC = TPGW Withholding Tax on Commissions

        StringBuilder extraInfo = new StringBuilder();
        extraInfo.append("GL_TYPE=TPGWWhTaxOnCommission\r\n")
                .append("X3_GL_TRANSACTION_CODE=WHOLD\r\n")
                .append("GL_ENTRY_DATE=" + glEntryDate + "\r\n")
                .append("GL_DESCRIPTION=" + glDescription + "\r\n")
                .append("GL_DIMENSION1=91OADH01\r\n")
                .append("CREDIT_ACCOUNT=4380004\r\n")
                .append("GL_AMOUNT=" + whtAmount + "\r\n")
                .append("DEBIT_ACCOUNT=4410002\r\n");

        // DAO.insertGLEntry(em, X3InterfaceDaemon.X3_TRANSACTION_TYPE_GL_ENTRY, saleRowId, tableName, extraInfo.toString());
    }

    private void sendInputVATOnCommissionJournal(EntityManager em, double commVatInputAmount, String glDescription, int saleId) throws Exception {
        log.debug("sendInputVATOnCommissionJournal [{}] [{}]", new Object[]{commVatInputAmount, glDescription});

        // Debit 2380001 (Vat on Commission)
        // Credit 4380002 (Vat on Commission)
        // GL Description = glDescription
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String glEntryDate = sdf.format(new Date());
        String tableName = "IVC" + saleId; // TWC = TPGW Withholding Tax on Commissions

        StringBuilder extraInfo = new StringBuilder();
        extraInfo.append("GL_TYPE=TPGWInputVATOnCommission\r\n")
                .append("X3_GL_TRANSACTION_CODE=GLVAT\r\n")
                .append("GL_ENTRY_DATE=" + glEntryDate + "\r\n")
                .append("GL_DESCRIPTION=" + glDescription + "\r\n")
                .append("GL_DIMENSION1=91OADH01\r\n")
                .append("CREDIT_ACCOUNT=4380002\r\n")
                .append("GL_AMOUNT=" + commVatInputAmount + "\r\n")
                .append("DEBIT_ACCOUNT=2380001\r\n");

        DAO.insertGLEntry(em, X3InterfaceDaemon.X3_TRANSACTION_TYPE_GL_ENTRY, saleId, tableName, extraInfo.toString());

    }

    private void sendICPNettOutJournal(EntityManager em, double creditUnearnedCentsExcl, double deviceRevenueCentsExcl, double icpReducedPurchasePriceExcl,
            double icpDiscountCents, String deviceItemNumber, double simRevenueCentsExcl, String simItemNumber, String glDescription, int saleRowId) throws Exception {
        // As agreed with Mike on 21/7/2015
        // CR Device revenue account for the item number with deviceRevenueCentsExcl
        // CR 4410002 with creditUnearnedCentsExcl
        // DB ICP Discount Acc XXXXXX with icpDiscountCents

        // sendICPNettOutJournal [50439.147074816094] [0.0] [-46716.291836720855] [-8862.105183672085] [5138.792468705961] [null] [SIM8002] [ICPNO-12560871]
        log.debug("sendICPNettOutJournal [{}] [{}] [{}] [{}] [{}] [{}] [{}] [{}]", new Object[]{creditUnearnedCentsExcl, deviceRevenueCentsExcl, simRevenueCentsExcl, icpReducedPurchasePriceExcl, icpDiscountCents, deviceItemNumber, simItemNumber, glDescription});

        double shouldBeZero = deviceRevenueCentsExcl + simRevenueCentsExcl + icpDiscountCents + creditUnearnedCentsExcl + icpReducedPurchasePriceExcl;
        if (Math.abs(shouldBeZero) > 1) {
            throw new Exception("Error in ICP Nett Out Journal - amounts do not balance -- " + shouldBeZero);
        }

        // Get rid of any rounding issues
        icpDiscountCents = (creditUnearnedCentsExcl + deviceRevenueCentsExcl + icpReducedPurchasePriceExcl + simRevenueCentsExcl) * -1;
        log.debug("icpDiscountCents is now [{}]", icpDiscountCents);

        long deviceAccount;
        long simAccount;
        String productDimension, simDimension;

        if (deviceItemNumber == null) {
            log.debug("There is no device in the kit so we should alter unearned");
            // As agreed with Mike on 9/9 2016
            deviceAccount = 4410002;
            productDimension = BaseUtils.getProperty("env.pos.netoff.gl.bundle.product.dim", "");
        } else {
            deviceAccount = X3Helper.getDeviceRevenueAccount(deviceItemNumber);
            productDimension = X3Helper.getProductDimension(deviceItemNumber);
        }

        if (simItemNumber == null) {
            log.debug("There is no device in the kit so we should alter unearned");
            // As agreed with Mike on 09/05/2018
            simAccount = 4410002;
            simDimension = BaseUtils.getProperty("env.pos.netoff.gl.bundle.product.dim", "");
        } else {
            simAccount = X3Helper.getDeviceRevenueAccount(simItemNumber);
            simDimension = X3Helper.getProductDimension(simItemNumber);
        }

        // CR 4410002 with creditUnearnedCentsExcl
        // CR deviceAccount with deviceRevenueCentsExcl
        // DB icpDiscAccount with icpDiscountCents
        // GL Description = glDescription
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String glEntryDate = sdf.format(new Date());
        String tableName = "INO" + saleRowId; // IN = ICP Nett Off

        StringBuilder extraInfo = new StringBuilder();
        extraInfo.append("GL_TYPE=ICPNettOutGL\n")
                .append("X3_GL_TRANSACTION_CODE=ICPTU\n")
                .append("GL_ENTRY_DATE=" + glEntryDate + "\n")
                .append("GL_DESCRIPTION=" + glDescription + "\n")
                .append("GL_DIMENSION1=91OADH01\n")
                .append("GL_DIMENSION4=" + productDimension + "\n");

        int dbtIndex = 0;
        int cdtIndex = 0;

        if (creditUnearnedCentsExcl >= 0) { //Using Double.toString(...) to avoid exponentials like E-5)
            cdtIndex++;
            extraInfo.append("CREDIT_ACCOUNT" + cdtIndex + "=4410002\n");
            extraInfo.append("GL_CREDIT_AMOUNT" + cdtIndex + "=" + Double.toString(creditUnearnedCentsExcl) + "\n");
        } else {
            dbtIndex++;
            extraInfo.append("DEBIT_ACCOUNT" + dbtIndex + "=4410002\n");
            extraInfo.append("GL_DEBIT_AMOUNT" + dbtIndex + "=" + Double.toString(creditUnearnedCentsExcl * -1) + "\n");
        }

        if (icpDiscountCents >= 0) {
            cdtIndex++;
            extraInfo.append("CREDIT_ACCOUNT" + cdtIndex + "=5610010\n");
            extraInfo.append("GL_CREDIT_AMOUNT" + cdtIndex + "=" + Double.toString(icpDiscountCents) + "\n");
        } else {
            dbtIndex++;
            extraInfo.append("DEBIT_ACCOUNT" + dbtIndex + "=5610010\n");
            extraInfo.append("GL_DEBIT_AMOUNT" + dbtIndex + "=" + Double.toString(icpDiscountCents * -1) + "\n");
        }

        if (deviceRevenueCentsExcl >= 0) {
            cdtIndex++;
            extraInfo.append("CREDIT_ACCOUNT" + cdtIndex + "=" + deviceAccount + "\n");
            extraInfo.append("GL_CREDIT_AMOUNT" + cdtIndex + "=" + Double.toString(deviceRevenueCentsExcl) + "\n");
        } else {
            dbtIndex++;
            extraInfo.append("DEBIT_ACCOUNT" + dbtIndex + "=" + deviceAccount + "\n");
            extraInfo.append("GL_DEBIT_AMOUNT" + dbtIndex + "=" + Double.toString(deviceRevenueCentsExcl * -1) + "\n");
        }

        if (simRevenueCentsExcl >= 0) {
            cdtIndex++;
            extraInfo.append("CREDIT_ACCOUNT" + cdtIndex + "=" + simAccount + "\n");
            extraInfo.append("GL_CREDIT_AMOUNT" + cdtIndex + "=" + Double.toString(simRevenueCentsExcl) + "\n");
        } else {
            dbtIndex++;
            extraInfo.append("DEBIT_ACCOUNT" + dbtIndex + "=" + simAccount + "\n");
            extraInfo.append("GL_DEBIT_AMOUNT" + dbtIndex + "=" + Double.toString(simRevenueCentsExcl * -1) + "\n");
        }

        if (icpReducedPurchasePriceExcl >= 0) {
            cdtIndex++;
            extraInfo.append("CREDIT_ACCOUNT" + cdtIndex + "=5330001" + "\n");
            extraInfo.append("GL_CREDIT_AMOUNT" + cdtIndex + "=" + Double.toString(icpReducedPurchasePriceExcl) + "\n");
        } else {
            dbtIndex++;
            extraInfo.append("DEBIT_ACCOUNT" + dbtIndex + "=5330001" + "\n");
            extraInfo.append("GL_DEBIT_AMOUNT" + dbtIndex + "=" + Double.toString(icpReducedPurchasePriceExcl * -1) + "\n");
        }

        //Kamogelo; approved by Mike: All INO GLs with zero amount shouln't create a GL
        if (icpReducedPurchasePriceExcl == 0 && deviceRevenueCentsExcl == 0 && icpDiscountCents == 0 && creditUnearnedCentsExcl == 0) {
            log.debug("GLs with Zero amounts should not be created,GL_CREDIT_AMOUNT: " + icpReducedPurchasePriceExcl);
            throw new Exception("GLs with Zero amounts should not be created,GL_CREDIT_AMOUNT: " + icpReducedPurchasePriceExcl);
        } else {
            DAO.insertGLEntry(em, X3InterfaceDaemon.X3_TRANSACTION_TYPE_GL_ENTRY, saleRowId, tableName, extraInfo.toString());
        }

    }

    private String getSaleDescription(Sale sale) {
        String description = sale.getSaleLines().get(0).getInventoryItem().getDescription();
        if (sale.getSaleLines().size() > 1) {
            description = description + " and " + (sale.getSaleLines().size() - 1) + " other items";
        }
        return description;
    }

    @Override
    public InventoryList getUpSizeInventory(UpSizeInventoryQuery upSizeInventoryQuery) throws POSError {

        try {
            InventoryList upSizeList = new InventoryList();

            List<InventoryItem> upsizeInventoryList = X3Helper.getUpSizeSubItems(emJTA, upSizeInventoryQuery);

            upSizeList.getInventoryItems().addAll(upsizeInventoryList);
            upSizeList.setNumberOfInventoryItems(upsizeInventoryList.size());
            return upSizeList;

        } catch (Exception ex) {
            throw processError(POSError.class, ex);
        }
    }

    @Override
    public CreateStandardGLOut createStandardGL(CreateStandardGLData createStandardGLData) throws POSError {
        if (createStandardGLData.getGlAmount() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String glEntryDate = sdf.format(new Date());
            String tableName = createStandardGLData.getTableName(); // TWC = TPGW Withholding Tax on Commissions
            StringBuilder extraInfo = new StringBuilder();
            extraInfo.append("X3_GL_TRANSACTION_CODE=").append(createStandardGLData.getX3GlTransactionCode())
                    .append("\r\n")
                    .append("GL_ENTRY_DATE=").append(glEntryDate)
                    .append("\r\n")
                    .append("GL_DESCRIPTION=").append(createStandardGLData.getGlDescription())
                    .append("\r\n")
                    .append("GL_DIMENSION1=91OADH01")
                    .append("\r\n")
                    .append("CREDIT_ACCOUNT=").append(createStandardGLData.getGlCreditAccount())
                    .append("\r\n")
                    .append("GL_AMOUNT=").append(createStandardGLData.getGlAmount())
                    .append("\r\n")
                    .append("DEBIT_ACCOUNT=").append(createStandardGLData.getGlDebitAccount())
                    .append("\r\n");

            DAO.insertGLEntry(emJTA, X3InterfaceDaemon.X3_TRANSACTION_TYPE_GL_ENTRY, createStandardGLData.getPrimaryKey(), tableName, extraInfo.toString());
        } else {
            log.warn("createStandardGL - GL amount for code [{}] is zero.", createStandardGLData.getX3GlTransactionCode());
        }
        CreateStandardGLOut rsp = new CreateStandardGLOut();
        rsp.setStatus("Done");
        return rsp;
    }

    @Override
    public ReverseGLOut reverseGL(ReverseGLData reverseGLData) throws POSError {

        X3Helper.reverseX3GL(emJTA, reverseGLData.getPrimaryKey(), reverseGLData.getTableName(), reverseGLData.getTransactionType());

        ReverseGLOut response = new ReverseGLOut();
        response.setStatus("Done");

        return response;

    }

    private void sendExciseTaxJournal(Sale xmlSale, SaleLine xmlSaleLine) {

        String val = Utils.getValueFromCRDelimitedAVPString(xmlSaleLine.getProvisioningData(), "ExciseDuty");
        if (val == null || val.isEmpty()) {
            log.debug("No excise duty for this item");
            return;
        }
        if (!xmlSale.getStatus().equals(PAYMENT_STATUS_PAID)) {
            return;
        }
        if (xmlSale.getPaymentMethod().equals(POSManager.PAYMENT_METHOD_AIRTIME)) {
            return;
        }
        int saleRowId = xmlSaleLine.getParentLineId() == 0 ? xmlSaleLine.getLineId() : xmlSaleLine.getParentLineId();
        CreateStandardGLData createStandardGLData = new CreateStandardGLData();
        createStandardGLData.setGlAmount(Double.parseDouble(val));
        createStandardGLData.setGlCreditAccount("4380006");
        createStandardGLData.setGlDebitAccount("4410002");
        createStandardGLData.setGlDescription("EXCDT-" + saleRowId);
        createStandardGLData.setPrimaryKey(saleRowId);
        createStandardGLData.setTableName("EXCDT");
        createStandardGLData.setX3GlTransactionCode("EXCDT");
        try {
            createStandardGL(createStandardGLData);
        } catch (Exception ex) {
            log.error("Error while trying to create Excise tax journal exciseAmount [{}] Description [{}] saleRowID [{}] - error [{}]",
                    new Object[]{createStandardGLData.getGlAmount(), createStandardGLData.getGlDescription(), saleRowId, ex.getMessage()});
        }
    }

    public static enum CONTRACT_STATUS {

        AC, DE;
    };

    private void syncXMLContractIntodbContract(com.smilecoms.xml.schema.pos.Contract xmlContract, com.smilecoms.pos.db.model.Contract dbContract) throws Exception {

        dbContract.setContractId(xmlContract.getContractId());
        dbContract.setCreatedByCustomerProfileId(xmlContract.getCreatedByCustomerId());
        dbContract.setCustomerProfileId(xmlContract.getCustomerProfileId() > 0 ? xmlContract.getCustomerProfileId() : null);
        dbContract.setOrganisationId(xmlContract.getOrganisationId() > 0 ? xmlContract.getOrganisationId() : null);
        dbContract.setContractName(xmlContract.getContractName());
        dbContract.setContractStartDateTime(Utils.getJavaDate(xmlContract.getContractStartDateTime()));
        dbContract.setContractEndDateTime(Utils.getJavaDate(xmlContract.getContractEndDateTime()));
        dbContract.setInvoiceCycleDay(xmlContract.getInvoiceCycleDay());
        dbContract.setContractId(xmlContract.getContractId());
        dbContract.setStatus(xmlContract.getStatus());
        dbContract.setAccountId(xmlContract.getAccountId());
        dbContract.setPaymentMethod(xmlContract.getPaymentMethod());
        dbContract.setCreditAccountNumber(xmlContract.getCreditAccountNumber());
        dbContract.setFulfilmentItemsAllowed(xmlContract.getFulfilmentItemsAllowed() == null ? "" : xmlContract.getFulfilmentItemsAllowed());
        dbContract.setStaffMembersAllowed(xmlContract.getStaffMembersAllowed() == null ? "" : xmlContract.getStaffMembersAllowed());
    }

    private void syncDBContractIntoXMLContract(EntityManager em, com.smilecoms.pos.db.model.Contract dbContract, com.smilecoms.xml.schema.pos.Contract xmlContract) throws Exception {
        xmlContract.setContractId(dbContract.getContractId());
        xmlContract.setCustomerProfileId(dbContract.getCustomerProfileId() == null ? 0 : dbContract.getCustomerProfileId());
        xmlContract.setOrganisationId(dbContract.getOrganisationId() == null ? 0 : dbContract.getOrganisationId());
        xmlContract.setCreatedByCustomerId(dbContract.getCreatedByCustomerProfileId());
        xmlContract.setContractName(dbContract.getContractName());
        xmlContract.setContractStartDateTime(Utils.getDateAsXMLGregorianCalendar(dbContract.getContractStartDateTime()));
        xmlContract.setCreatedDateTime(Utils.getDateAsXMLGregorianCalendar(dbContract.getCreatedDateTime()));
        xmlContract.setContractEndDateTime(Utils.getDateAsXMLGregorianCalendar(dbContract.getContractEndDateTime()));
        xmlContract.setInvoiceCycleDay(dbContract.getInvoiceCycleDay());
        xmlContract.setContractId(dbContract.getContractId());
        xmlContract.setStatus(dbContract.getStatus());
        xmlContract.setAccountId(dbContract.getAccountId());
        xmlContract.setPaymentMethod(dbContract.getPaymentMethod());
        xmlContract.setLastModifiedDateTime(Utils.getDateAsXMLGregorianCalendar(dbContract.getLastModifiedDateTime()));
        xmlContract.setCreditAccountNumber(dbContract.getCreditAccountNumber());
        xmlContract.getContractDocuments().addAll(DAO.getContractDocuments(em, dbContract.getContractId()));
        xmlContract.setFulfilmentItemsAllowed(dbContract.getFulfilmentItemsAllowed() == null ? "" : dbContract.getFulfilmentItemsAllowed());
        xmlContract.setStaffMembersAllowed(dbContract.getStaffMembersAllowed() == null ? "" : dbContract.getStaffMembersAllowed());
    }

    public static String getPropertyFromConfig(UnitCreditSpecification ucs, String propName) {
        Map<String, String> config = new HashMap<>();
        StringTokenizer stValues = new StringTokenizer(ucs.getConfiguration(), "\r\n");
        while (stValues.hasMoreTokens()) {
            String row = stValues.nextToken();
            if (!row.isEmpty()) {
                String[] bits = row.split("=");
                String name = bits[0].trim();
                String value;
                if (bits.length == 1) {
                    value = "";
                } else {
                    value = bits[1].trim();
                }
                config.put(name, value);
            }
        }
        return config.get(propName);
    }
    
    private List<String> QueryInvoiceDetails(String invoiceNumberToBeEncoded) throws MalformedURLException, IOException {
        List<String> fieldArray = new ArrayList<>();

        URL url = new URL("http://127.0.0.1:9880/efristcs/ws/tcsapp/getInformation");
        HttpURLConnection openConnec = (HttpURLConnection) url.openConnection();

        openConnec.setRequestMethod("POST");
        openConnec.setRequestProperty("Content-Type", "application/json; utf-8");
        openConnec.setRequestProperty("Accept", "application/json");
        openConnec.setDoOutput(true);

        DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime requestTime = LocalDateTime.now();
        String encodedContent = Base64.getEncoder().encodeToString(invoiceNumberToBeEncoded.getBytes());

        String jsonRequest
                = "{"
                + "  \"data\":"
                + "  {"
                + "      \"content\":" + encodedContent
                + "      \"signature\": \"\","
                + "      \"dataDescription\" :"
                + "    {"
                + "      \"codeType\" : \"0\","
                + "      \"encryptCode\" : \"1\""
                + "      \"zipCode\" : \"1\""
                + "    }"
                + " }"
                + ","
                + "  \"globalInfo\":"
                + "  {"
                + "      \"appId\": \"encodedContent\","
                + "      \"version\": \"\","
                + "      \"dataexchangeId\" :"
                + "      \"interfaceCode\" : \"0\","
                + "      \"requestCode\" : \"1\""
                + "      \"requestTime\":" + dateTimeFormat.format(requestTime)
                + "      \"responseCode\" : \"0\","
                + "      \"userName\" : \"1\""
                + "      \"deviceMAC\" : \"1\""
                + "      \"deviceNo\" : \"0\","
                + "      \"tin\" : \"1\""
                + "      \"brn\" : \"1\""
                + "      \"taxpayerID\" : \"0\","
                + "      \"longitude\" : \"1\""
                + "      \"latitude\" : \"1\""
                + "        \"extendField\" :"
                + "    {"
                + "      \"responseDateFormat\" : \"dd/MM/yyyy\"\","
                + "      \"responseTimeFormat\" : \"dd/MM/yyyy HH:mm:ss\""
                + "    }"
                + " }"
                + ","
                + "        \"returnStateInfo\" :"
                + "    {"
                + "      \"returnCode\" : \"0\","
                + "      \"returnMessage\" : \"1\""
                + "    }"
                + ",";

        try (OutputStream os = openConnec.getOutputStream()) {
            byte[] input = jsonRequest.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(openConnec.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            String transactionDetails = (response.toString());

            //read json data & look for content.
            transactionDetails = StringEscapeUtils.unescapeHtml(transactionDetails);
            transactionDetails = transactionDetails.replaceAll("\\|", Pattern.quote("|"));

            try {
                JSONParser jsonParser;
                jsonParser = new JSONParser();
                JSONObject transactionAsJSONObject = (JSONObject) jsonParser.parse(transactionDetails);
                
                JSONObject jsonObj = new JSONObject();
                        if (transactionAsJSONObject.get("data") != null) {
                            jsonObj = (JSONObject) transactionAsJSONObject.get("data");

                            //Now from the jsonObj you can get whatever you are looking for For Example: InvoiceNumber = jsonObj.get("invoice_number");
                            JSONObject jsonObjValue;
                            String encodedResponse = jsonObj.get("content").toString();
                            byte[] decodedBytes = Base64.getDecoder().decode(encodedResponse);
                            String decodedString = new String(decodedBytes);
                            JSONObject decodedStringAsJSONObject = (JSONObject) jsonParser.parse(decodedString);

                            
                                
                                if (decodedStringAsJSONObject.get("Fiscal Document Number") != null) {
                                    jsonObjValue = (JSONObject) decodedStringAsJSONObject.get("Fiscal Document Number");
                                    String fiscalDocumentNumber = jsonObjValue.get("Fiscal Document Number").toString();
                                    fieldArray.add(fiscalDocumentNumber);
                                }
                                
                                
                            if (decodedStringAsJSONObject.get("Verification Code") != null) {
                                jsonObjValue = (JSONObject) decodedStringAsJSONObject.get("Verification Code");
                                String verificationCode = jsonObjValue.get("Verification Code").toString();
                                fieldArray.add(verificationCode);
                            }

                        }


            } catch (Exception e) {
                System.out.println("Something went wrong.");
            }
        }

        return fieldArray;

    }

    private List<String> UploadInvoiceDetails(String saleDetails) throws MalformedURLException, IOException {
        List<String> fieldArray = new ArrayList<>();
        DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime requestTime = LocalDateTime.now();
        String encodedContent = Base64.getEncoder().encodeToString(saleDetails.getBytes());
        String jsonRequest
                = "{"
                + "  \"data\":"
                + "  {"
                + "      \"content\":" + encodedContent
                + "      \"signature\": \"\","
                + "      \"dataDescription\" :"
                + "    {"
                + "      \"codeType\" : \"0\","
                + "      \"encryptCode\" : \"1\""
                + "      \"zipCode\" : \"1\""
                + "    }"
                + " }"
                + ","
                + "  \"globalInfo\":"
                + "  {"
                + "      \"appId\": \"encodedContent\","
                + "      \"version\": \"\","
                + "      \"dataexchangeId\" :"
                + "      \"interfaceCode\" : \"0\","
                + "      \"requestCode\" : \"1\""
                + "      \"requestTime\":" + dateTimeFormat.format(requestTime)
                + "      \"responseCode\" : \"0\","
                + "      \"userName\" : \"1\""
                + "      \"deviceMAC\" : \"1\""
                + "      \"deviceNo\" : \"0\","
                + "      \"tin\" : \"1\""
                + "      \"brn\" : \"1\""
                + "      \"taxpayerID\" : \"0\","
                + "      \"longitude\" : \"1\""
                + "      \"latitude\" : \"1\""
                + "        \"extendField\" :"
                + "    {"
                + "      \"responseDateFormat\" : \"dd/MM/yyyy\"\","
                + "      \"responseTimeFormat\" : \"dd/MM/yyyy HH:mm:ss\""
                + "    }"
                + " }"
                + ","
                + "        \"returnStateInfo\" :"
                + "    {"
                + "      \"returnCode\" : \"0\","
                + "      \"returnMessage\" : \"1\""
                + "    }"
                + ",";
        URL url = new URL("http://127.0.0.1:9880/efristcs/ws/tcsapp/getInformation");
        HttpURLConnection openConnec = (HttpURLConnection) url.openConnection();
        openConnec.setRequestMethod("POST");
        openConnec.setRequestProperty("Content-Type", "application/json; utf-8");
        openConnec.setRequestProperty("Accept", "application/json");
        openConnec.setDoOutput(true);
        
        try (OutputStream os = openConnec.getOutputStream()) {
            byte[] input = jsonRequest.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(openConnec.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            String transactionDetails = (response.toString());

            //read json data & look for content.
            transactionDetails = StringEscapeUtils.unescapeHtml(transactionDetails);
            transactionDetails = transactionDetails.replaceAll("\\|", Pattern.quote("|"));

            try {
                JSONParser jsonParser = new JSONParser();
                JSONObject transactionAsJSONObject = (JSONObject)jsonParser.parse(transactionDetails);
                
                JSONObject jsonObj = new JSONObject();

                        if (transactionAsJSONObject.get("data") != null) {
                            jsonObj = (JSONObject) transactionAsJSONObject.get("data");

                            //Now from the jsonObj you can get whatever you are looking for For Example: InvoiceNumber = jsonObj.get("invoice_number");
                            JSONObject jsonObjValue;
                            String encodedResponse = jsonObj.get("content").toString();
                            byte[] decodedBytes = Base64.getDecoder().decode(encodedResponse);
                            String decodedString = new String(decodedBytes);
                            JSONObject decodedStringAsJSONObject = (JSONObject) jsonParser.parse(decodedString);

                            
                            if (decodedStringAsJSONObject.get("Fiscal Document Number") != null) {
                                jsonObjValue = (JSONObject) decodedStringAsJSONObject.get("Fiscal Document Number");
                                String fiscalDocumentNumber = jsonObjValue.get("Fiscal Document Number").toString();
                                fieldArray.add(fiscalDocumentNumber);
                            }

                            if (decodedStringAsJSONObject.get("Verification Code") != null) {
                                jsonObjValue = (JSONObject) decodedStringAsJSONObject.get("Verification Code");
                                String verificationCode = jsonObjValue.get("Verification Code").toString();
                                fieldArray.add(verificationCode);
                            }

                            

                        }


            } catch (Exception e) {
                System.out.println("Something went wrong.");
            }
        }

        return fieldArray;

    }

    private String getEfrisDataXML(List <SaleRow> rowsofSale, Customer theBuyer, Sale entireSale) throws Exception {
        DecimalFormat df2 = new DecimalFormat("#.##");
    try {
        int referenceNo = entireSale.getSaleId();
        XMLGregorianCalendar issuedDate = entireSale.getSaleDate();
        double totalPrice = entireSale.getSaleTotalCentsIncl();
        double totalTax = entireSale.getSaleTotalTaxCents();
        long operator = entireSale.getSalesPersonAccountId();        
        String currencyCode = entireSale.getTenderedCurrency();
        double netAmount = totalPrice - totalTax;
        String customerName = theBuyer.getFirstName();
        String emailAddress = theBuyer.getEmailAddress();
        JSONArray allItems = new JSONArray();
        JSONArray allTaxDetails = new JSONArray();
        JSONArray allPaywayModes = new JSONArray();
        int itemCount = rowsofSale.size();
        JSONParser jsonParser = new JSONParser();
        //loop through items and add to string
        
        
        int orderNumber=0;
        for (SaleRow currentSale:rowsofSale){
            String goodsName = currentSale.getDescription();            
            BigDecimal discountTotal = (BigDecimal)currentSale.getTotalDiscountOnInclCents();
            BigDecimal unitPrice = (BigDecimal)currentSale.getUnitPriceCentsIncl();
            long qty = currentSale.getQuantity();
            String discountFlag;
            if (discountTotal.setScale(2, BigDecimal.ROUND_HALF_EVEN).equals(new BigDecimal("0.00").setScale(2, BigDecimal.ROUND_HALF_EVEN))){                
                discountFlag = "2";                
            }
            else {                
                discountFlag = "0";
            }
        
        JSONObject efrisProductData = getEfrisProductData(goodsName);
        
        if(efrisProductData==null) {
            log.warn("Something went wrong getting Efris productInfo");            
            throw new Exception ("Invoice not Created. Efris Product info not returned!  Create invoice manually");
        }
        
        String taxRate = String.valueOf(df2.format(Double.parseDouble(BaseUtils.getProperty("env.sales.tax.percent", "18"))/100));
            
        JSONObject listOfItems = new JSONObject();
        listOfItems.put("item", goodsName);
        listOfItems.put("itemCode",efrisProductData.get("goodsCode"));// goodsCode);
        listOfItems.put("qty", String.valueOf(qty));
        listOfItems.put("unitOfMeasure", efrisProductData.get("measureUnit"));
        listOfItems.put("unitPrice", String.valueOf(unitPrice.divide(new BigDecimal(100))));
        listOfItems.put("total", String.valueOf(df2.format(totalPrice/100)));
        listOfItems.put("taxRate", taxRate);
        listOfItems.put("tax", String.valueOf(df2.format(totalTax/100)));
        listOfItems.put("discountTotal", "");
        listOfItems.put("discountTaxRate", "0.00");
        listOfItems.put("orderNumber", orderNumber);
        listOfItems.put("discountFlag",String.valueOf(discountFlag));
        listOfItems.put("deemedFlag", "2");
        listOfItems.put("exciseFlag", "2");
        listOfItems.put("categoryId", "");
        listOfItems.put("categoryName", "");
        listOfItems.put("goodsCategoryId", efrisProductData.get("commodityCategoryCode"));
        listOfItems.put("goodsCategoryName", "");
        listOfItems.put("exciseRate", "");
        listOfItems.put("exciseRule", "");
        listOfItems.put("exciseTax", "");
        listOfItems.put("pack", "");
        listOfItems.put("stick", "");
        listOfItems.put("exciseUnit", "");
        listOfItems.put("exciseCurrency", "");
        listOfItems.put("exciseRateName", "");  
        
        String cleanlistOfItems = listOfItems.toString().replace("\\"," ");                        
        log.debug("ListOfItems : {}", cleanlistOfItems);
        
        orderNumber++;
        allItems.add((JSONObject) jsonParser.parse(cleanlistOfItems));
        }
       
        
        JSONObject listOfTaxDetails = new JSONObject();
        listOfTaxDetails.put("taxCategory", "Standard");
        listOfTaxDetails.put("netAmount", String.valueOf(df2.format(netAmount/100)));
        listOfTaxDetails.put("taxRate", String.valueOf(Double.parseDouble(BaseUtils.getProperty("env.sales.tax.percent"))/100));
        listOfTaxDetails.put("taxAmount", String.valueOf(df2.format(totalTax/100)));
        listOfTaxDetails.put("grossAmount", String.valueOf(df2.format(totalPrice/100)));
        listOfTaxDetails.put("exciseUnit", "");
        listOfTaxDetails.put("exciseCurrency", "");
        listOfTaxDetails.put("taxRateName", "TestRate");
        
        allTaxDetails.add((JSONObject) jsonParser.parse(listOfTaxDetails.toString().replace("\\"," ")));
         
         String PaymentMode;
         switch (entireSale.getPaymentMethod()) {
             
            case PAYMENT_METHOD_LOAN:
                PaymentMode = "101"; 
                JSONObject loanMode = new JSONObject();
                loanMode.put("paymentMode", PaymentMode);
                loanMode.put("paymentAmount", String.valueOf(df2.format(totalPrice/100)));
                loanMode.put("orderNumber", String.valueOf(entireSale.getSaleId()));
                allPaywayModes.add((JSONObject) jsonParser.parse(loanMode.toString().replace("\\"," ")));
                break;
            
            case PAYMENT_METHOD_STAFF:
                PaymentMode = "101"; 
                JSONObject staffMode = new JSONObject();
                    staffMode.put("paymentMode", PaymentMode);
                    staffMode.put("paymentAmount", String.valueOf(df2.format(totalPrice/100)));
                    staffMode.put("orderNumber", String.valueOf(entireSale.getSaleId()));
                allPaywayModes.add((JSONObject) jsonParser.parse(staffMode.toString().replace("\\"," ")));
                break;
            case PAYMENT_METHOD_CREDIT_ACCOUNT :
                PaymentMode = "101"; 
                JSONObject creditAccount = new JSONObject();
                    creditAccount.put("paymentMode", PaymentMode);
                    creditAccount.put("paymentAmount", String.valueOf(df2.format(totalPrice/100)));
                    creditAccount.put("orderNumber", String.valueOf(entireSale.getSaleId()));
                allPaywayModes.add((JSONObject) jsonParser.parse(creditAccount.toString().replace("\\"," ")));
                break;
            case PAYMENT_METHOD_DIRECT_AIRTIME:
                PaymentMode = "101"; 
                JSONObject directAirtime = new JSONObject();
                    directAirtime.put("paymentMode", PaymentMode);
                    directAirtime.put("paymentAmount", String.valueOf(df2.format(totalPrice/100)));
                    directAirtime.put("orderNumber", String.valueOf(entireSale.getSaleId()));
                allPaywayModes.add((JSONObject) jsonParser.parse(directAirtime.toString().replace("\\"," ")));
                break;
            case PAYMENT_METHOD_AIRTIME:
                PaymentMode = "101"; 
                JSONObject airtimeMode = new JSONObject();
                    airtimeMode.put("paymentMode", PaymentMode);
                    airtimeMode.put("paymentAmount", String.valueOf(df2.format(totalPrice/100)));
                    airtimeMode.put("orderNumber", String.valueOf(entireSale.getSaleId()));
                allPaywayModes.add((JSONObject) jsonParser.parse(airtimeMode.toString().replace("\\"," ")));
                break;
             case PAYMENT_METHOD_CASH:
                PaymentMode = "102"; 
                JSONObject cashMode = new JSONObject();
                    cashMode.put("paymentMode", PaymentMode);
                    cashMode.put("paymentAmount", String.valueOf(df2.format(totalPrice/100)));
                    cashMode.put("orderNumber", String.valueOf(entireSale.getSaleId()));
                allPaywayModes.add((JSONObject) jsonParser.parse(cashMode.toString().replace("\\"," ")));
                break;
            case PAYMENT_METHOD_CHEQUE :
                PaymentMode = "103";
                JSONObject chequeMode = new JSONObject();
                chequeMode.put("paymentMode", PaymentMode);
                chequeMode.put("paymentAmount", String.valueOf(df2.format(totalPrice/100)));
                chequeMode.put("orderNumber", String.valueOf(entireSale.getSaleId()));
                allPaywayModes.add((JSONObject) jsonParser.parse(chequeMode.toString().replace("\\"," ")));
                 break;           
            case PAYMENT_METHOD_CLEARING_BUREAU:
                PaymentMode = "105";
                JSONObject mobileMoney = new JSONObject();
                    mobileMoney.put("paymentMode", PaymentMode);
                    mobileMoney.put("paymentAmount", String.valueOf(df2.format(totalPrice/100)));
                    mobileMoney.put("orderNumber", String.valueOf(entireSale.getSaleId()));
                allPaywayModes.add((JSONObject) jsonParser.parse(mobileMoney.toString().replace("\\"," ")));
                break;
            case PAYMENT_METHOD_CARD_PAYMENT:
                PaymentMode = "106";
                JSONObject cardMode = new JSONObject();
                    cardMode.put("paymentMode", PaymentMode);
                    cardMode.put("paymentAmount", String.valueOf(df2.format(totalPrice/100)));
                    cardMode.put("orderNumber", String.valueOf(entireSale.getSaleId()));
                allPaywayModes.add((JSONObject) jsonParser.parse(cardMode.toString().replace("\\"," ")));
                break;
            case PAYMENT_METHOD_BANK_TRANSFER:
                PaymentMode = "107";
                JSONObject eftMode = new JSONObject();
                    eftMode.put("paymentMode", PaymentMode);
                    eftMode.put("paymentAmount", String.valueOf(df2.format(totalPrice/100)));
                    eftMode.put("orderNumber", String.valueOf(entireSale.getSaleId())); 
                allPaywayModes.add((JSONObject) jsonParser.parse(eftMode.toString().replace("\\"," ")));
               break;    
            case PAYMENT_METHOD_CONTRACT:
                PaymentMode = "107";
                JSONObject contractMode = new JSONObject();
                    contractMode.put("paymentMode", PaymentMode);
                    contractMode.put("paymentAmount", String.valueOf(df2.format(totalPrice/100)));
                    contractMode.put("orderNumber", String.valueOf(entireSale.getSaleId()));   
                allPaywayModes.add((JSONObject) jsonParser.parse(contractMode.toString().replace("\\"," ")));
                break;
         }
         
        JSONObject smileDetails = new JSONObject();
            smileDetails.put("tin", "1000185668");
            smileDetails.put("ninBrn", "");
            smileDetails.put("legalName", "SMILE COMMUNICATIONS UGANDA LIMITED");
            smileDetails.put("businessName", "SMILE COMMUNICATIONS UGANDA LIMITED");
            smileDetails.put("address", "10-12 BUKOTO CORPORATION RISE BUKOTO KAMPALA NAKAWA,DIVISION NAKAWA DIVISION NAGURU II");
            smileDetails.put("mobilePhone", "");
            smileDetails.put("linePhone", "");
            smileDetails.put("emailAddress", emailAddress);
            smileDetails.put("placeOfBusiness", "Uganda");
            smileDetails.put("referenceNo", String.valueOf(referenceNo));
            smileDetails.put("branchId", "");
            smileDetails.put("isCheckReferenceNo", "0");
            
        JSONObject invoiceNumberObj = new JSONObject();
            invoiceNumberObj.put("invoiceNo", String.valueOf(entireSale.getSaleId()));
            invoiceNumberObj.put("antifakeCode",  UUID.randomUUID().toString().replace("-", ""));
            invoiceNumberObj.put("deviceNo", "TCSbff66f784616645");
            invoiceNumberObj.put("issuedDate", "2021-04-17 18:43:12");
            invoiceNumberObj.put("operator", String.valueOf(operator));
            invoiceNumberObj.put("currency", String.valueOf(entireSale.getTenderedCurrency()));
            invoiceNumberObj.put("oriInvoiceId", "1");
            invoiceNumberObj.put("invoiceType", "1");
            invoiceNumberObj.put("invoiceKind", "1");
            invoiceNumberObj.put("dataSource", "103");
            invoiceNumberObj.put("invoiceIndustryCode", "105");
            invoiceNumberObj.put("isBatch", "0");
            
        JSONObject buyerDetailsObj = new JSONObject();
            buyerDetailsObj.put("buyerTin", "");
            buyerDetailsObj.put("buyerNinBrn", "");
            buyerDetailsObj.put("buyerPassportNum","");
            buyerDetailsObj.put("buyerLegalName", customerName);
            buyerDetailsObj.put("buyerBusinessName", "");
            buyerDetailsObj.put("buyerAddress", "");
            buyerDetailsObj.put("buyerEmail", theBuyer.getEmailAddress());
            buyerDetailsObj.put("buyerMobilePhone", theBuyer.getAlternativeContact1());
            buyerDetailsObj.put("buyerLinePhone", theBuyer.getAlternativeContact1());
            buyerDetailsObj.put("buyerPlaceOfBusi", "");
            buyerDetailsObj.put("buyerType", "1");
            buyerDetailsObj.put("buyerCitizenship", "");
            buyerDetailsObj.put("buyerSector", "1");
            buyerDetailsObj.put("buyerReferenceNo", "");
            
         
        JSONObject buyerExtendObj = new JSONObject();
          buyerExtendObj.put("propertyType", "abc");
        buyerExtendObj.put("district", "");
        buyerExtendObj.put("municipalityCounty", "");
        buyerExtendObj.put("divisionSubcounty", "");
        buyerExtendObj.put("town", "");
        buyerExtendObj.put("cellVillage", "");
        buyerExtendObj.put("effectiveRegistrationDate", "");
        buyerExtendObj.put("meterStatus", "");
        
        JSONObject invoiceSummary = new JSONObject();
        invoiceSummary.put("netAmount", String.valueOf(df2.format(netAmount/100)));
        invoiceSummary.put("taxAmount", String.valueOf(df2.format(totalTax/100)));
        invoiceSummary.put("grossAmount", String.valueOf(df2.format(totalPrice/100)));
        invoiceSummary.put("itemCount", String.valueOf(itemCount));
        invoiceSummary.put("modeCode", "0");
        invoiceSummary.put("remarks", "Smile Comm Purchase");
        invoiceSummary.put("qrCode", "");
        
        JSONObject invoiceExtend = new JSONObject();
        invoiceExtend.put("reason", "");
        invoiceExtend.put("reasonCode", "");
        
        JSONObject importServicesSeller = new JSONObject();
        importServicesSeller.put("importBusinessName", "");
        importServicesSeller.put("importEmailAddress", "");
        importServicesSeller.put("importContactNumber", "");
        importServicesSeller.put("importAddress", "");
        importServicesSeller.put("importInvoiceDate", issuedDate.toString());
        importServicesSeller.put("importAttachmentName", "");
        importServicesSeller.put("importAttachmentContent", "");
        
        JSONObject invoiceToUPload = new JSONObject();
            invoiceToUPload.put("sellerDetails",smileDetails);                           
            invoiceToUPload.put("basicInformation", invoiceNumberObj);                
            invoiceToUPload.put("buyerDetails", buyerDetailsObj);                
            invoiceToUPload.put("buyerExtend", buyerExtendObj);            
            invoiceToUPload.put("goodsDetails",allItems);
            invoiceToUPload.put("taxDetails", allTaxDetails);                
            invoiceToUPload.put("summary", invoiceSummary);
            invoiceToUPload.put("payWay", allPaywayModes);                
            invoiceToUPload.put("extend", invoiceExtend);                
            invoiceToUPload.put("importServicesSeller", importServicesSeller);
        
        String cleanerString = invoiceToUPload.toString().replace("\\","");
      
        log.warn("DataContentToSend {}", cleanerString);
        
        StringBuilder finalCurrentDate = new StringBuilder("\"");
        DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime requestTime = LocalDateTime.now();
        String currentDate = dateTimeFormat.format(requestTime);
        finalCurrentDate.append(currentDate);
        finalCurrentDate.append("\"");        
        byte[] finalEncodedContent = cleanerString.getBytes();  
        StringBuilder finaltaxpayerID = new StringBuilder("\"");
        Integer taxpayerID = theBuyer.getCustomerId();
        String taxpayerIDString = taxpayerID.toString();
        finaltaxpayerID.append(taxpayerIDString);
        finaltaxpayerID.append("\"");
        StringBuilder finalUserName = new StringBuilder("\"");
        String userName = theBuyer.getFirstName() + " " + theBuyer.getLastName();
        finalUserName.append(userName);
        finalUserName.append("\"");
        
        String jsonRequest
                = "{"
                + "\"data\":{"
                + "\"content\":\""+ Base64.getEncoder().encodeToString(cleanerString.getBytes()) +"\","
                + "\"signature\":\"\","
                + "\"dataDescription\":"
                + "{"
                + "\"codeType\":\"1\","
                + "\"encryptCode\":\"2\","
                + "\"zipCode\":\"0\""
                + "}"
                + " }"
                + ","
                + "\"globalInfo\":"
                + "{"
                + "\"appId\":\"AP04\","
                + "\"version\":\"1.1.20191201\","
                + "\"dataexchangeId\":\"" + UUID.randomUUID().toString().replace("-", "")+ "\","
                + "\"interfaceCode\":\"T109\","
                + "\"requestCode\":\"TP\","
                + "\"requestTime\":"+finalCurrentDate +","
                + "\"responseCode\":\"TA\","
                + "\"userName\":" + finalUserName +","
                + "\"deviceMAC\":\"00:50:56:ac:68:dd\","
                + "\"deviceNo\":\"TCSbff66f784616645\","
                + "\"tin\":\"1000185668\","
                + "\"brn\":\"\","
                + "\"taxpayerID\":"+finaltaxpayerID+","
                + "\"longitude\":\"116.397128\","
                + "\"latitude\":\"39.2916527\","
                + "\"extendField\":"
                + "{"
                + "\"responseDateFormat\":\"dd/MM/yyyy\","
                + "\"responseTimeFormat\":\"dd/MM/yyyy HH:mm:ss\""
                + "}"
                + "}"
                + ","
                + "\"returnStateInfo\":"
                + "{"
                + "\"returnCode\":\"\","
                + "\"returnMessage\":\"\""
                + "}"
                + "}";
        
        log.warn("INPUT JSON {}", jsonRequest);
        
        URL url = new URL ("http://10.28.66.15:9880/efristcs/ws/tcsapp/getInformation");
        
        String charset = "UTF-8"; 
        HttpURLConnection openConnec = (HttpURLConnection)url.openConnection();
        openConnec.setRequestMethod("POST");
        openConnec.setDoOutput(true);
        openConnec.setRequestProperty("Accept-Charset", charset);
        openConnec.setRequestProperty("Content-Type", "application/json" ); 
        
        OutputStream ost = openConnec.getOutputStream();
        ost.write(jsonRequest.getBytes());
        ost.flush();
        
        if (openConnec.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new Exception("Failed : HTTP error code : " + openConnec.getResponseCode() + " - "
                        + openConnec.getResponseMessage());
                
        } 
        
        BufferedReader br = new BufferedReader(new InputStreamReader(
                                    (openConnec.getInputStream())));

                    
        JSONParser parser = new JSONParser(); 
        StringBuilder efrisResponse= new StringBuilder(); 
        String responseLine = null;
        
        while ((responseLine = br.readLine()) != null) {                        
            try {
                  efrisResponse.append(responseLine.trim());
            } catch (Exception e) { log.warn("Problem parsing response [{}]", efrisResponse);}
        } 
        
        StringBuilder xml = new StringBuilder(); 
            
            try {       
                
                JSONObject transactionAsJSONObject = (JSONObject) parser.parse(new String(efrisResponse));;  
                
                        if (transactionAsJSONObject.get("data") != null) {                            
                            JSONObject jsonObj = (JSONObject) transactionAsJSONObject.get("data");                            
                            String encodedResponse = jsonObj.get("content").toString();
                            
                            byte[] decodedBytes =Base64.getMimeDecoder().decode(encodedResponse);
                            
                            JSONObject decodedStringAsJSONObject = new JSONObject();
                            try {
                                    decodedStringAsJSONObject = (JSONObject) parser.parse(new String(decodedBytes));
                                    
                                    
                            } catch (Exception e) {
                                    e.printStackTrace(); 
                                    throw new Exception("Something went wrong creating EFRIS Invoice:.... " + openConnec.getResponseMessage());
                            }
                                                           
                        /*                
                            if (decodedStringAsJSONObject.get("sellerDetails") != null) {
                                JSONObject sellerDetails = (JSONObject) decodedStringAsJSONObject.get("sellerDetails");
                                xml.append("<sellerDetails>");
                                    xml.append("<ninBRN>");
                                        if(sellerDetails.get("ninBrn")!=null) {
                                            xml.append(StringEscapeUtils.escapeXml(sellerDetails.get("ninBrn").toString().trim().replace("\"","")));                                                
                                        } else {
                                            xml.append("** Not Provided **");
                                        }
                                    xml.append("</ninBRN>");                                        
                                    xml.append("<tradeName>");
                                        if(sellerDetails.get("legalName")!=null) {
                                            xml.append(StringEscapeUtils.escapeXml(sellerDetails.get("legalName").toString().trim().replace("\"","")));
                                        } else {
                                            xml.append("** Not Provided **");
                                        }
                                    xml.append("</tradeName>");
                                    xml.append("<address>");
                                        if(sellerDetails.get("address")!=null) {
                                            xml.append(StringEscapeUtils.escapeXml(sellerDetails.get("address").toString().trim().replace("\"","")));
                                        } else {
                                            xml.append("** Not Provided **");
                                        }
                                        
                                    xml.append("</address>");
                                    xml.append("<referenceNo>");
                                    if(sellerDetails.get("referenceNo")!=null) {
                                            xml.append(StringEscapeUtils.escapeXml(sellerDetails.get("referenceNo").toString().trim().replace("\"","")));
                                        } else {
                                            xml.append("** Not Provided **");
                                        }
                                        
                                    xml.append("</referenceNo>");
                                xml.append("</sellerDetails>");
                                log.warn("Added sellerDetails");
                            }   */
                            
                            if (decodedStringAsJSONObject.get("basicInformation") != null) {
                               JSONObject basicInformation = (JSONObject) decodedStringAsJSONObject.get("basicInformation");
                               xml.append("<uraInformation>");
                                   xml.append("<documentType>");
                                        if(basicInformation.get("invoiceType")!=null) {
                                            String docType="";
                                            if(basicInformation.get("invoiceType").equals("1")) {
                                                docType="Invoice";
                                            }
                                            if(basicInformation.get("invoiceType").equals("4")) {
                                                docType="Receipt";
                                            }
                                            xml.append(StringEscapeUtils.escapeXml(docType.replace("\"","")));                                                
                                        } else {
                                            xml.append("** Not Provided **");
                                        }
                                        
                                       
                                   xml.append("</documentType>");                                        
                                   xml.append("<issueDate>");
                                        if(basicInformation.get("issuedDate")!=null) {
                                            xml.append(StringEscapeUtils.escapeXml(basicInformation.get("issuedDate").toString().trim().replace("\"","")));                                                
                                        } else {
                                            xml.append("** Not Provided **");
                                        }
                                   xml.append("</issueDate>");                                   
                                   
                                   xml.append("<fiscalDocumentNumber>");
                                    if(basicInformation.get("invoiceNo")!=null) {
                                            xml.append(StringEscapeUtils.escapeXml(basicInformation.get("invoiceNo").toString().trim().replace("\"","")));                                                
                                        } else {
                                            xml.append("** Not Provided **");
                                        }
                                       
                                   xml.append("</fiscalDocumentNumber>");
                                   xml.append("<deviceNumber>");
                                   if(basicInformation.get("deviceNo")!=null) {
                                             xml.append(StringEscapeUtils.escapeXml(basicInformation.get("deviceNo").toString().trim().replace("\"","")));                                                
                                        } else {
                                            xml.append("** Not Provided **");
                                        }
                                   
                                   xml.append("</deviceNumber>");
                                   xml.append("<verificationCode>");
                                   if(basicInformation.get("antifakeCode")!=null) {
                                            xml.append(StringEscapeUtils.escapeXml(basicInformation.get("antifakeCode").toString().trim().replace("\"","")));                                                
                                        } else {
                                            xml.append("** Not Provided **");
                                        }
                                        
                                       
                                   xml.append("</verificationCode>");
                               xml.append("</uraInformation>");
                               log.warn("Added uraInformation");
                           }
                         /*   
                            if (decodedStringAsJSONObject.get("buyerDetails") != null) {
                                JSONObject buyerDetails = (JSONObject) decodedStringAsJSONObject.get("buyerDetails");
                                xml.append("<buyerDetails>");
                                    xml.append("<buyerTin>");
                                    if(buyerDetails.get("buyerTin")!=null) {
                                        xml.append(StringEscapeUtils.escapeXml(buyerDetails.get("buyerTin").toString().trim().replace("\"","")));                                                
                                    } else {
                                        xml.append("** Not Provided **");
                                    }
                                    xml.append("</buyerTin>");  
                                    xml.append("<ninBRN>");
                                    if(buyerDetails.get("buyerNinBrn")!=null) {
                                        xml.append(StringEscapeUtils.escapeXml(buyerDetails.get("buyerNinBrn").toString().trim().replace("\"","")));                                                
                                    } else {
                                        xml.append("** Not Provided **");
                                    }
                                        
                                    xml.append("</ninBRN>");                                        
                                    xml.append("<buyerName>");
                                    if(buyerDetails.get("buyerLegalName")!=null) {
                                        xml.append(StringEscapeUtils.escapeXml(buyerDetails.get("buyerLegalName").toString().trim().replace("\"","")));                                                
                                    } else {
                                        xml.append("** Not Provided **");
                                    }
                                        
                                    xml.append("</buyerName>");
                                    xml.append("<buyerTel>");
                                    if(buyerDetails.get("buyerLinePhone")!=null) {
                                        xml.append(StringEscapeUtils.escapeXml(buyerDetails.get("buyerLinePhone").toString().trim().replace("\"","")));                                                
                                    } else {
                                        xml.append("** Not Provided **");
                                    }                                    
                                        
                                    xml.append("</buyerTel>");
                                xml.append("</buyerDetails>");
                                log.warn("Added buyerDetails");
                            }
                           
                                
                            
                                if (decodedStringAsJSONObject.get("goodsDetails") != null) {
                                    JSONArray goodsDetails = (JSONArray) decodedStringAsJSONObject.get("goodsDetails");               

                                    xml.append("<goodsDetails>");
                                    
                                    for (Object item : goodsDetails) {
                                        if(item instanceof JSONObject)
                                        {   JSONObject saleItem=  (JSONObject)item;
                                            xml.append("<product>");
                                            xml.append("<item>");
                                                xml.append(StringEscapeUtils.escapeXml(saleItem.get(new String("item")).toString().trim().replace("\"","")));                                                
                                            xml.append("</item>");                                        
                                            xml.append("<qty>");
                                                xml.append(StringEscapeUtils.escapeXml(saleItem.get("qty").toString().trim().replace("\"","")));                                                
                                            xml.append("</qty>");  
                                            xml.append("<unit>");
                                                xml.append(StringEscapeUtils.escapeXml(String.valueOf(df2.format(Double.parseDouble(saleItem.get("unitPrice").toString())/100)).trim().replace("\"","")+" UGX"));                                                
                                            xml.append("</unit>"); 
                                            xml.append("<total>");
                                                xml.append(StringEscapeUtils.escapeXml(String.valueOf(df2.format(Double.parseDouble(saleItem.get("total").toString())/100)).trim().replace("\"","")+" UGX"));                                                
                                            xml.append("</total>");
                                            xml.append("</product>");
                                        }
                                    }
                                    xml.append("</goodsDetails>");
                                    log.warn("Added goodsDetails");
                                }
                                     
                                
                                    if (decodedStringAsJSONObject.get("taxDetails") != null) {
                                        log.warn("Adding taxDetails now");
                                        JSONArray taxDetails = (JSONArray) decodedStringAsJSONObject.get("taxDetails");
                                        xml.append("<taxDetails>");                    
                                            for (Object item : taxDetails) {
                                                if(item instanceof JSONObject) {
                                                    JSONObject taxItem=  (JSONObject)item;
                                                    xml.append("<taxItem>");
                                                    xml.append("<taxRate>");
                                                        if(taxItem.get("taxRate")!=null) {                                                            
                                                            xml.append(StringEscapeUtils.escapeXml(String.valueOf(df2.format(Double.parseDouble(taxItem.get("taxRate").toString())*100)).trim().replace("\"","")+"%"));                                                
                                                        } else {
                                                            xml.append("** Not Provided **");
                                                        }  
                                                        
                                                    xml.append("</taxRate>");                                        
                                                    xml.append("<netAmount>");
                                                    if(taxItem.get("netAmount")!=null) {
                                                        xml.append(StringEscapeUtils.escapeXml(String.valueOf(df2.format(Double.parseDouble(taxItem.get("netAmount").toString())/100)).trim().replace("\"","")+" UGX"));                                                
                                                        } else {
                                                            xml.append("** Not Provided **");
                                                        }  
                                                        
                                                    xml.append("</netAmount>");
                                                    xml.append("<taxAmount>");
                                                    if(taxItem.get("taxAmount")!=null) {
                                                        xml.append(StringEscapeUtils.escapeXml(String.valueOf(df2.format(Double.parseDouble(taxItem.get("taxAmount").toString())/100)).trim().trim().replace("\"","")+" UGX"));                                                
                                                        } else {
                                                            xml.append("** Not Provided **");
                                                        }  
                                                        
                                                        
                                                    xml.append("</taxAmount>");
                                                    xml.append("<grossAmount>");
                                                    if(taxItem.get("grossAmount")!=null) {
                                                            xml.append(StringEscapeUtils.escapeXml(String.valueOf(df2.format(Double.parseDouble(taxItem.get("grossAmount").toString())/100)).trim().trim().replace("\"","")+" UGX")); 
                                                        } else {
                                                            xml.append("** Not Provided **");
                                                        }  
                                                        
                                                        
                                                    xml.append("</grossAmount>");
                                                    xml.append("</taxItem>");

                                                }
                                            }
                                        xml.append("</taxDetails>");
                                        log.warn("Added taxDetails");
                                    } */                                     
                               
                                if (decodedStringAsJSONObject.get("summary") != null) {
                                   JSONObject buyerDetails = (JSONObject) decodedStringAsJSONObject.get("summary");

                                   xml.append("<summary>");
                                       /*xml.append("<netAmount>");
                                           xml.append(StringEscapeUtils.escapeXml(String.valueOf(df2.format(Double.parseDouble(buyerDetails.get("netAmount").toString())/100)).trim().replace("\"","")+" UGX"));                                                
                                       xml.append("</netAmount>");                                        
                                       xml.append("<taxAmount>");
                                           xml.append(StringEscapeUtils.escapeXml(String.valueOf(df2.format(Double.parseDouble(buyerDetails.get("taxAmount").toString())/100)).trim().replace("\"","")+" UGX"));                                                
                                       xml.append("</taxAmount>");
                                       xml.append("<grossAmount>");
                                           xml.append(StringEscapeUtils.escapeXml(String.valueOf(df2.format(Double.parseDouble(buyerDetails.get("grossAmount").toString())/100)).trim().replace("\"","")+" UGX"));                                                
                                       xml.append("</grossAmount>");
                                       xml.append("<remarks>");
                                           xml.append(StringEscapeUtils.escapeXml(buyerDetails.get("remarks").toString().trim().replace("\"","")));                                                
                                       xml.append("</remarks>");*/
                                        xml.append("<qrCode>");
                                       
                                        xml.append(StringEscapeUtils.escapeXml(buyerDetails.get("qrCode").toString().trim().replace("\"","")));                                                
                                       xml.append("</qrCode>");
                                   xml.append("</summary>");
                                   log.warn("Added summary");
                                }
                                      
                       /*     if (decodedStringAsJSONObject.get("payWay") != null) {
                                JSONArray payModes = (JSONArray) decodedStringAsJSONObject.get("payWay");         
                                xml.append("<payModes>");
                                    for(Object item : payModes) {
                                        if(item instanceof JSONObject) {
                                            JSONObject payMode = (JSONObject) item;
                                        xml.append("<mode>");
                                        xml.append("<paymentMode>");
                                            switch (entireSale.getPaymentMethod()) {
             
                                                    case PAYMENT_METHOD_LOAN:
                                                        xml.append(StringEscapeUtils.escapeXml("Loan".trim().replace("\"",""))); 
                                                        break;

                                                    case PAYMENT_METHOD_STAFF:
                                                        xml.append(StringEscapeUtils.escapeXml("Staff".trim().replace("\"",""))); 
                                                        break;
                                                    case PAYMENT_METHOD_CREDIT_ACCOUNT :
                                                        xml.append(StringEscapeUtils.escapeXml("Credit Account".trim().replace("\"",""))); 
                                                        break;
                                                    case PAYMENT_METHOD_DIRECT_AIRTIME:
                                                        xml.append(StringEscapeUtils.escapeXml("Direct Airtime".trim().replace("\"",""))); 
                                                        break;
                                                    case PAYMENT_METHOD_AIRTIME:
                                                        xml.append(StringEscapeUtils.escapeXml("Airtime".trim().replace("\"",""))); 
                                                        break;
                                                     case PAYMENT_METHOD_CASH:
                                                        xml.append(StringEscapeUtils.escapeXml("Cash".trim().replace("\"",""))); 
                                                        break;
                                                    case PAYMENT_METHOD_CHEQUE :
                                                        xml.append(StringEscapeUtils.escapeXml("Cheque".trim().replace("\"",""))); 
                                                         break;           
                                                    case PAYMENT_METHOD_CLEARING_BUREAU:
                                                        xml.append(StringEscapeUtils.escapeXml("Clearing Bureau".trim().replace("\"",""))); 
                                                        break;
                                                    case PAYMENT_METHOD_CARD_PAYMENT:
                                                        xml.append(StringEscapeUtils.escapeXml("Card Payment".trim().replace("\"",""))); 
                                                        break;
                                                    case PAYMENT_METHOD_BANK_TRANSFER:
                                                        xml.append(StringEscapeUtils.escapeXml("Bank Transfer".trim().replace("\"",""))); 
                                                       break;    
                                                    case PAYMENT_METHOD_CONTRACT:
                                                        xml.append(StringEscapeUtils.escapeXml("Contract".trim().replace("\"",""))); 
                                                        break;
                                                 }                                                                                           
                                        xml.append("</paymentMode>");                                        
                                        xml.append("<paymentAmount>");
                                            xml.append(StringEscapeUtils.escapeXml(String.valueOf(df2.format(Double.parseDouble(payMode.get("paymentAmount").toString())/100)).trim().replace("\"","")+" UGX"));                                                
                                        xml.append("</paymentAmount>");
                                        xml.append("<orderNumber>");
                                            xml.append(StringEscapeUtils.escapeXml(payMode.get("orderNumber").toString().trim().replace("\"","")));                                                
                                        xml.append("</orderNumber>"); 
                                        xml.append("</mode>");
                                        }
                                    }
                                xml.append("</payModes>");
                                log.warn("Added payModes");
                            }  */
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                        log.warn("Something went wrong processing Response from EFRIS, code [{}]", openConnec.getResponseCode());
                        
                        throw new Exception("Something went wrong processing Response from EFRIS: " + openConnec.getResponseMessage());
            
                }
                  
            //Let's create the xml using data from Efris

            return xml.toString();
        } catch (Exception e) {
            e.printStackTrace();
            log.warn("Error occured creating Efris Invoice: [{}]", e.getStackTrace());
             throw processError(POSError.class, e);
        }
    }
    
    private String getInvoiceXML(Sale sale, Customer cust, Organisation org, Customer salesPerson, List <SaleRow> currentRow) throws Exception, POSError, ProtocolException, IOException, MalformedURLException  {
        log.debug("Compiling a sale invoice");
        StringBuilder xml = new StringBuilder();
        xml.append("<Invoice>");
        removeProrataCalc(sale);
        parseComments(sale);
        xml.append(marshallObject(sale));
        xml.append("<Customer>");
        xml.append(getCustomerXML(cust, org, sale.getRecipientAccountId()));
        xml.append("</Customer>");
        xml.append("<SalesPerson>");
        xml.append(getSalesPersonXML(salesPerson, sale.getSalesPersonAccountId()));
        xml.append("</SalesPerson>");
        xml.append("<Validity>");
        xml.append(Utils.getDaysBetweenDates(Utils.getJavaDate(sale.getExpiryDate()), Utils.getJavaDate(sale.getSaleDate())));
        xml.append("</Validity>");
        xml.append("<CreditAccountNumber>");
        xml.append(StringEscapeUtils.escapeXml(sale.getCreditAccountNumber()));
        xml.append("</CreditAccountNumber>");
        
        if(BaseUtils.getBooleanProperty("env.sales.use.efris.invoice",false)) {
            String uraData =getEfrisDataXML(currentRow,cust,sale);
            if(uraData==null) {
                throw new Exception("Failed to get info from URA. Please retry");
            }            
            xml.append(uraData);
        }
        xml.append("</Invoice>");
        log.debug("Sales Invoice XML is [{}]", xml);        
        
        return xml.toString();
    }
    
    
    public JSONObject getEfrisProductData (String productName) throws Exception {
        
        log.warn("IN getEfrisProductData");
        JSONObject productData = new JSONObject();
            productData.put("goodsName",productName.trim());                           
            productData.put("pageNo", "1");                
            productData.put("pageSize", "3");                
            
        
        String cleanerString = productData.toString().replace("\\","");
        
        DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime requestTime = LocalDateTime.now();        
        
        String jsonRequest
        = "{"
        + "\"data\":{"
        + "\"content\":\""+ Base64.getEncoder().encodeToString(cleanerString.getBytes()) +"\","
        + "\"signature\":\"\","
        + "\"dataDescription\":"
        + "{"
        + "\"codeType\":\"1\","
        + "\"encryptCode\":\"2\","
        + "\"zipCode\":\"0\""
        + "}"
        + " }"
        + ","
        + "\"globalInfo\":"
        + "{"
        + "\"appId\":\"AP04\","
        + "\"version\":\"1.1.20191201\","
        + "\"dataexchangeId\":\"" + UUID.randomUUID().toString().replace("-", "")+ "\","
        + "\"interfaceCode\":\"T127\","
        + "\"requestCode\":\"TP\","
        + "\"requestTime\":\"" + dateTimeFormat.format(requestTime) + "\","
        + "\"responseCode\":\"TA\","
        + "\"userName\":\"admin admin\","
        + "\"deviceMAC\":\"00:50:56:ac:68:dd\","
        + "\"deviceNo\":\"TCSbff66f784616645\","
        + "\"tin\":\"1000185668\","
        + "\"brn\":\"\","
        + "\"taxpayerID\":\"1\","
        + "\"longitude\":\"116.397128\","
        + "\"latitude\":\"39.2916527\","
        + "\"extendField\":"
        + "{"
        + "\"responseDateFormat\":\"dd/MM/yyyy\","
        + "\"responseTimeFormat\":\"dd/MM/yyyy HH:mm:ss\""
        + "}"
        + "}"
        + ","
        + "\"returnStateInfo\":"
        + "{"
        + "\"returnCode\":\"\","
        + "\"returnMessage\":\"\""
        + "}"
        + "}";
        
        
        log.warn("INPUT JSON {}" + jsonRequest);
        
        
        try {

            URL url = new URL ("http://10.28.66.15:9880/efristcs/ws/tcsapp/getInformation");

            String charset = "UTF-8"; 
            HttpURLConnection openConnec = (HttpURLConnection)url.openConnection();
            openConnec.setRequestMethod("POST");
            openConnec.setDoOutput(true);
            openConnec.setRequestProperty("Accept-Charset", charset);
            openConnec.setRequestProperty("Content-Type", "application/json" ); 

            OutputStream ost = openConnec.getOutputStream();
            ost.write(jsonRequest.getBytes());
            ost.flush();

            if (openConnec.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new Exception("Failed : HTTP error code : " + openConnec.getResponseCode() + " - "
                            + openConnec.getResponseMessage());

            } 

            BufferedReader br = new BufferedReader(new InputStreamReader(
                                        (openConnec.getInputStream())));


            JSONParser parser = new JSONParser(); 
            StringBuilder efrisResponse= new StringBuilder(); 
            String responseLine = null;

            while ((responseLine = br.readLine()) != null) {                        
                try {
                      efrisResponse.append(responseLine.trim());
                } catch (Exception e) { log.warn("Problem parsing response [{}]" + efrisResponse);}
            } 

            log.warn("RESPONSE: " + new String(efrisResponse));

            JSONObject transactionAsJSONObject = (JSONObject) parser.parse(new String(efrisResponse)); 

            if (transactionAsJSONObject.get("data") != null) {                            
                JSONObject jsonObj = (JSONObject) transactionAsJSONObject.get("data");                            
                String encodedResponse = jsonObj.get("content").toString();

                byte[] decodedBytes =Base64.getMimeDecoder().decode(encodedResponse);

                JSONObject decodedStringAsJSONObject = new JSONObject();
                try {
                        decodedStringAsJSONObject = (JSONObject) parser.parse(new String(decodedBytes));

                        System.out.println("CONTENT: " + decodedStringAsJSONObject.toString());
                } catch (Exception e) {
                        e.printStackTrace();                                    
                }

                if (decodedStringAsJSONObject.get("records") != null) {
                    JSONArray records = new JSONArray();
                    if(decodedStringAsJSONObject.get("records") instanceof JSONArray) {
                            records = (JSONArray) decodedStringAsJSONObject.get("records");
                            for(Object item : records) {
                                if(item instanceof JSONObject) {
                                    JSONObject productItem = (JSONObject) item;

                                    if(productItem.get("goodsName").equals(productName)) {                                    
                                        return productItem;
                                    }
                                }

                            }
                        //If we get here, product was not found in Efris
                        throw new Exception ("EFRIS Error: Product not found in EFRIS - " + productName.trim());
                    }

                }
            } else {
                throw new Exception ("EFRIS Error: No data returned for product - " + productName.trim());
            }
        } catch (Exception e) {
            log.warn("Error! " + e.getMessage());
            e.printStackTrace();                
            throw processError(POSError.class, e);
        }
       return null; 
    }
  
}

    class CreditNoteRow {

        BigDecimal perItemDiscountInCentsExcl;
        BigDecimal perItemDiscountInCentsIncl;
        BigDecimal perItemSalesPriceInCentsExcl;
        BigDecimal perItemSalesPriceInCentsIncl;
        String itemNumber;
        long quantityLeftForCredit;
    }

    class PostProcessingResult {

        Set<String> transferTxIds = new HashSet();
        Set<String> ucTxIds = new HashSet();
        Set<String> chargeTxIds = new HashSet();
    }

