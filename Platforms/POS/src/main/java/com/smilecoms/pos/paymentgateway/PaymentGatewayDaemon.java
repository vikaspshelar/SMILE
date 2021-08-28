package com.smilecoms.pos.paymentgateway;

import com.smilecoms.commons.base.lifecycle.BaseListener;

import org.slf4j.*;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.CustomerQuery;
import com.smilecoms.commons.sca.JiraField;
import com.smilecoms.commons.sca.MindMapFields;
import com.smilecoms.commons.sca.NewTTIssue;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.TTIssue;
import com.smilecoms.commons.sca.TTJiraUserList;
import com.smilecoms.commons.sca.TTJiraUserQuery;
import com.smilecoms.commons.sca.TTUser;
import com.smilecoms.commons.util.ExceptionManager;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.pos.POSManager;
import com.smilecoms.pos.db.model.Sale;
import com.smilecoms.pos.db.op.DAO;
import java.io.IOException;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
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
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodRetryHandler;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.params.HttpParams;

@Singleton
@Startup
@Local({BaseListener.class})
public class PaymentGatewayDaemon implements BaseListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayDaemon.class);
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
        log.warn("PaymentGateway Daemon is starting up as properties are ready");
        BaseUtils.deregisterForPropsAvailability(this);
        BaseUtils.registerForPropsChanges(this);
        runner1 = Async.scheduleAtFixedRate(new SmileBaseRunnable("POS.paymentGatewayDaemon") {
            @Override
            public void run() {
                trigger();
            }
        }, 30000, 10000, 15000);
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
            if (!BaseUtils.getBooleanProperty("env.pos.paymentgateway.mustrun", true)) {
                log.debug("PaymentGateway is set to not run");
                return;
            }
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            log.debug("POS PaymentGateway Daemon triggered to do payment gateway processing");
            em = JPAUtils.getEM(emf);
            try {
                // repeat right away if we processed a lot
                while (doProcessing() > 50) {
                }
            } catch (Exception e) {
                log.warn("Error in POS PaymentGateway daemon [{}]", e.toString());
                log.warn("Error: ", e);
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "Error in PaymentGateway daemon: " + e.toString());
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
            List<Integer> list = DAO.getPaymentGatewayPendingSaleIds(em);
            Collections.shuffle(list); // Lower chance of clashes of multiple threads
            JPAUtils.commitTransactionAndClear(em);

            for (int saleId : list) {
                boolean calledPostProcessing = false;
                try {
                    JPAUtils.beginTransaction(em);
                    log.debug("Getting locked sale to process [{}]", saleId);
                    Sale lockedSale = DAO.getLockedSale(em, saleId);                    

                    if (!lockedSale.getStatus().equals(POSManager.PAYMENT_STATUS_PENDING_PAYMENT)) {
                        log.debug("Sale already processed by another JVM");
                        JPAUtils.rollbackTransaction(em);
                        continue;
                    }

                    if (lockedSale.getPaymentGatewayPollCount() > BaseUtils.getIntProperty("env.pos.paymentgateway.max.pollcount", 200)
                            && lockedSale.getSaleDateTime().before(Utils.getPastDate(Calendar.MINUTE, 
                                    BaseUtils.getIntProperty("env.pos.paymentgateway.max.poll.sale.age.minutes", 120)))) {
                        log.debug("This sale is too old to be tried any longer. Setting status to RV");
                        lockedSale.setStatus(POSManager.PAYMENT_STATUS_INVOICE_REVERSAL);
                        lockedSale.setPaymentGatewayLastPollDate(new Date());
                        lockedSale.setPaymentGatewayNextPollDate(null);
                        lockedSale.setPaymentGatewayPollCount(lockedSale.getPaymentGatewayPollCount() + 1);
                        em.persist(lockedSale);
                        em.flush();
                        doCallback(lockedSale);
                        JPAUtils.commitTransactionAndClear(em);
                        continue;
                    }

                    cnt++;
                    PaymentGatewayPlugin plugin = getPlugin(em, lockedSale.getPaymentGatewayCode());
                    long startPGW = System.currentTimeMillis();
                    PaymentGatewayResult result = plugin.getPaymentGatewayResult(lockedSale);
                    long latency = System.currentTimeMillis() - startPGW;
                    BaseUtils.addStatisticSample("POS.PaymentGatewayPoll." + plugin.getName(), BaseUtils.STATISTIC_TYPE.latency, latency);
                    if (result.isSuccess()) {
                        log.debug("[{}] was successful. Result was [{}]", lockedSale.getSaleId(), result.getGatewayResponse());
                        lockedSale.setExtraInfo(Utils.setValueInCRDelimitedAVPString(lockedSale.getExtraInfo(), "PaymentGatewayInfo", result.getInfo()));
                        lockedSale.setPaymentGatewayNextPollDate(null);
                        em.persist(lockedSale);
                        em.flush();
                        postProcessSale(lockedSale, result);
                        calledPostProcessing = true;
                        em.refresh(lockedSale);
                        doCallback(lockedSale);
                    } else {
                        log.debug("[{}] is not successful yet. Result was [{}]", lockedSale.getSaleId(), result.getGatewayResponse());
                        if (result.mustTryAgainLater()) {
                            lockedSale.setPaymentGatewayNextPollDate(Utils.getFutureDate(Calendar.SECOND, getPauseSeconds(lockedSale.getPaymentGatewayPollCount())));
                            log.debug("We must try this again later");
                        } else {
                            log.debug("Permanent failure - wont try again later");
                            lockedSale.setStatus(POSManager.PAYMENT_STATUS_GW_FAIL_GW); // Failed on gateways side
                            doCallback(lockedSale);
                        }
                    }

                    log.debug("Updating sale with poll data");
                    lockedSale.setPaymentGatewayLastPollDate(new Date());
                    lockedSale.setPaymentGatewayResponse(result.getGatewayResponse());
                    // Field is used for auto matching
                    lockedSale.setExtTxid(result.getPaymentGatewayTransactionId());
                    lockedSale.setPaymentGatewayPollCount(lockedSale.getPaymentGatewayPollCount() + 1);
                    em.persist(lockedSale);
                    em.flush();
                    JPAUtils.commitTransactionAndClear(em);

                } catch (javax.persistence.PessimisticLockException ple) {
                    JPAUtils.rollbackTransaction(em);
                    log.debug("PessimisticLockException in PaymentGateway daemon for a row [{}]. Another thread must be processing it", ple.toString());
                } catch (java.lang.IllegalStateException ese) {
                    JPAUtils.rollbackTransaction(em);
                    log.debug("IllegalStateException in PaymentGateway daemon for a row [{}]. Probably in shutdown", ese.toString());
                    if (calledPostProcessing) {
                        log.warn("Error: ", ese);
                        BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "Error in PaymentGateway daemon. Sale was post processed but then got an exception committing. Sale Id " + saleId + " : " + ese.toString());
                    }
                } catch (Exception e) {
                    if (e.toString().contains("Lock wait timeout exceeded")) {
                        JPAUtils.rollbackTransaction(em);
                        log.debug("Lock wait timeout exceeped in PaymentGateway daemon for a row [{}]. Another thread must be processing it", e.toString());
                    } else {
                        log.warn("Error in PaymentGateway daemon for a sale [{}] [{}]. Transaction has been rolled back", saleId, e.toString());
                        JPAUtils.rollbackTransaction(em);
                        JPAUtils.beginTransaction(em);
                        Sale lockedSale = DAO.getLockedSale(em, saleId);
                        if (lockedSale.getStatus().equals(POSManager.PAYMENT_STATUS_PENDING_PAYMENT)) {
                            lockedSale.setStatus(POSManager.PAYMENT_STATUS_GW_FAIL_SMILE); // Failed on Smiles side
                            lockedSale.setPaymentGatewayExtraData(e.toString());
                            em.persist(lockedSale);
                            em.flush();
                        }
                        JPAUtils.commitTransaction(em);
                        log.debug("Sale status was changed to [{}]", lockedSale.getStatus());
                        notifyCustomerCareOnStatusFS(lockedSale);
                        log.warn("Error: ", e);
                        BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "Error in PaymentGateway daemon for Sale Id " + saleId + " : " + e.toString());
                    }
                }
            } // End loop

            long end = System.currentTimeMillis();
            log.debug("Finished PaymentGateway run. Run took [{}]ms to process [{}] records", end - start, cnt);
        } catch (Exception e) {
            JPAUtils.rollbackTransaction(em);
            throw e;
        }
        return cnt;
    }

    private void doCallback(Sale dbSale) {
        String url = dbSale.getCallbackURL();
        if (url != null && !url.isEmpty()) {
            log.debug("This sale has a callback URL [{}] which will be sent status [{}]", url, dbSale.getStatus());
            if (!url.contains("?")) {
                url = url + "?orderid=";
            } else {
                url = url + "&orderid=";
            }
            url = url + dbSale.getSaleId();
            url = url + "&gwtxid=" + dbSale.getExtTxid();
            url = url + "&amntcents=" + dbSale.getSaleTotalCentsIncl().setScale(2, RoundingMode.HALF_EVEN);
            url = url + "&status=" + dbSale.getStatus();
            callURL(url);
        } else {
            log.debug("This sale has no callback url");
        }
    }

    private int getPauseSeconds(int pollCount) {
        if (pollCount < 30) {
            // Every 10 seconds for first 5 minutes . 30X10=300s=5min.
            return 10;
        } else if (pollCount < 180) {
            // every minute from 5 minutes to 155min. 180-30=150 reps 150X60s= 150min
            return 60;
        }
        // Every 10min thereafter
        return 600;
    }

    private void postProcessSale(Sale dbSale, PaymentGatewayResult result) throws Exception {
        List<Integer> saleAsList = new ArrayList<>();
        saleAsList.add(dbSale.getSaleId());
        new POSManager().processPayment(em, saleAsList, result.getTransferredAmountCents(), result.getPaymentGatewayTransactionId(), "PD", true, "");
    }

    private void notifyCustomerCareOnStatusFS(Sale dbSale) {
        if (!BaseUtils.getBooleanProperty("env.notify.customercare.about.paymentstatus.fs", true)) {
            return;
        }
        try {
            final String failureReasonExtractorHelper = "Request [<?xml";
            String smileFailureReason = dbSale.getPaymentGatewayExtraData();
            String failedRequest = dbSale.getPaymentGatewayExtraData();

            String gatewayResponse = dbSale.getPaymentGatewayResponse();
            String dateTime = getValueOfAVP("dateTime", gatewayResponse);
            String paymentRef = getValueOfAVP("paymentRef", gatewayResponse);
            String status = getValueOfAVP("status", gatewayResponse);

            if (status == null || !status.equalsIgnoreCase("Successful")) {
                //The is no reason to report to customer care if the transaction was not successful;
                return;
            }

            double amountInCents = dbSale.getSaleTotalCentsIncl().doubleValue();
            long recipientAccount = dbSale.getRecipientAccountId();

            NewTTIssue tt = new NewTTIssue();
            tt.setMindMapFields(new MindMapFields());
            JiraField jiraF = new JiraField();

            Customer cust = getCustomer(dbSale.getRecipientCustomerId());

            jiraF.setFieldName("TT_FIXED_FIELD_Description");
            jiraF.setFieldType("TT_FIXED_FIELD");
            StringBuilder description = new StringBuilder("{panel:title=(!) A ");
            description.append("Payment Gateway request was unsuccessful from Smile's side.");
            if (smileFailureReason != null && !smileFailureReason.isEmpty() && smileFailureReason.contains(failureReasonExtractorHelper)) {
                smileFailureReason = smileFailureReason.substring(0, smileFailureReason.indexOf(failureReasonExtractorHelper));
                failedRequest = failedRequest.substring(failedRequest.indexOf("<"), failedRequest.lastIndexOf(">") + 1);
            } else {
                failedRequest = "empty";
            }
            description.append("|borderStyle=dashed|titleBGColor=red}");

            description.append("# Transaction details:\n").append("#* *Order ID:* ").append(dbSale.getSaleId()).append("\n").append("#* *Transaction Reference:* ").append(dbSale.getExtTxid()).append("\n");
            description.append("#* *Bank Payment Reference:* ").append(paymentRef).append("\n").append("#* *Bank response:* ").append(status).append("\n").append("#* *Amount Paid:* ").append(amountInCents / 100d).append("\n");
            description.append("#* *Payment Gateway Code:* ").append(dbSale.getPaymentGatewayCode()).append("\n").append("#* *Destination Account:* ").append(recipientAccount).append("\n");
            description.append("#* *Date bank processed transaction:* ").append(dateTime).append("\n");

            description.append("----").append("\n# Smile failure reason:\n").append("#* ").append(smileFailureReason).append("\n# SOAP request:\n").append("{code:xml}").append(failedRequest).append("{code}").append("{panel}");
            jiraF.setFieldValue(description.toString());
            tt.getMindMapFields().getJiraField().add(jiraF);

            jiraF = new JiraField();
            jiraF.setFieldName("TT_FIXED_FIELD_Issue Type");
            jiraF.setFieldType("TT_FIXED_FIELD");
            jiraF.setFieldValue("Customer Incident");
            tt.getMindMapFields().getJiraField().add(jiraF);

            jiraF = new JiraField();
            jiraF.setFieldName("TT_FIXED_FIELD_Summary");
            jiraF.setFieldType("TT_FIXED_FIELD");
            String summary = "MySmile [CustomerID " + cust.getCustomerId() + "]: A ";
            summary += "Payment Gateway transaction was unsuccessful from Smile's side.";
            jiraF.setFieldValue(summary);
            tt.getMindMapFields().getJiraField().add(jiraF);

            jiraF = new JiraField();
            jiraF.setFieldName("TT_FIXED_FIELD_Project");
            jiraF.setFieldType("TT_FIXED_FIELD");
            jiraF.setFieldValue(BaseUtils.getProperty("env.jira.customer.care.project.key"));
            tt.getMindMapFields().getJiraField().add(jiraF);

            jiraF = new JiraField();
            jiraF.setFieldName("SEP Reporter");
            jiraF.setFieldType("TT_FIXED_FIELD");
            jiraF.setFieldValue("admin admin");
            tt.getMindMapFields().getJiraField().add(jiraF);

            jiraF = new JiraField();
            jiraF.setFieldName("Incident Channel");
            jiraF.setFieldType("TT_FIXED_FIELD");
            jiraF.setFieldValue("System");
            tt.getMindMapFields().getJiraField().add(jiraF);

            jiraF = new JiraField();
            jiraF.setFieldName("Smile Customer Email");
            jiraF.setFieldType("TT_FIXED_FIELD");
            jiraF.setFieldValue(cust.getEmailAddress());
            tt.getMindMapFields().getJiraField().add(jiraF);

            jiraF = new JiraField();
            jiraF.setFieldName("Smile Customer Name");
            jiraF.setFieldType("TT_FIXED_FIELD");
            jiraF.setFieldValue(cust.getFirstName() + " " + cust.getLastName());
            tt.getMindMapFields().getJiraField().add(jiraF);

            jiraF = new JiraField();
            jiraF.setFieldName("Smile Customer Phone");
            jiraF.setFieldType("TT_FIXED_FIELD");
            jiraF.setFieldValue(cust.getAlternativeContact1());
            tt.getMindMapFields().getJiraField().add(jiraF);

            String watchers = "";
            Set<String> configWatchers = BaseUtils.getPropertyAsSet("env.paymentgateway.fs.watchers");
            for (String watcher : configWatchers) {
                if (watchers.isEmpty()) {
                    watchers = watcher;
                    continue;
                }
                watchers += ":" + watcher;
            }
            int accountManagerId = cust.getAccountManagerCustomerProfileId(); //Notify account manager as well if possible.

            if (accountManagerId > 0) {
                Customer accountManager = getCustomer(accountManagerId);
                String username = getJiraUsername(accountManager.getEmailAddress());
                if (!username.isEmpty()) {
                    if (!watchers.isEmpty()) {
                        watchers += ":" + username;
                    } else {
                        watchers = username;
                    }
                }
            }

            log.warn("Usernames to use for creating watchers: {}", watchers);
            jiraF = new JiraField();
            jiraF.setFieldName("TT_FIXED_FIELD_Watchers");
            jiraF.setFieldType("TT_FIXED_FIELD");
            jiraF.setFieldValue(watchers);
            tt.getMindMapFields().getJiraField().add(jiraF);

            tt.setCustomerId(String.valueOf(cust.getCustomerId()));
            TTIssue issue = SCAWrapper.getAdminInstance().createTroubleTicketIssue(tt);
            dbSale.setPaymentGatewayResponse(Utils.setValueInCRDelimitedAVPString(dbSale.getPaymentGatewayResponse(), "issueId", issue.getID()));
        } catch (Exception e) {
            log.warn("Failed to report status FS to customer care, reason: {}", e.toString());
        }
    }

    private Customer getCustomer(int custId) {
        CustomerQuery custQ = new CustomerQuery();
        custQ.setCustomerId(custId);
        custQ.setResultLimit(1);
        custQ.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        return SCAWrapper.getAdminInstance().getCustomer(custQ);
    }

    private String getJiraUsername(String email) {
        TTJiraUserQuery tj = new TTJiraUserQuery();
        tj.setEmailAddress(email);
        tj.setResultLimit(1);
        TTJiraUserList jiraUsers = new TTJiraUserList();
        try {
            jiraUsers = SCAWrapper.getAdminInstance().getJiraUsers(tj);
        } catch (Exception ex) {
            log.warn("SCA failed to find watchers username");
        }
        String username = "";
        for (TTUser usr : jiraUsers.getJiraUsers()) {
            username = usr.getUserID();
        }
        return username;
    }

    private String getValueOfAVP(String attribute, String crDelimited) {
        List<String> avpList = Utils.getListFromCRDelimitedString(crDelimited);
        String search = attribute + "=";
        for (String avp : avpList) {
            String value;
            if (avp.startsWith(search)) {
                try {
                    value = avp.split("\\=")[1];
                } catch (ArrayIndexOutOfBoundsException ex) {
                    value = "";
                }
                return value;
            }
        }
        return "unknown";
    }

    private void callURL(String strURL) {
        String ret = null;
        GetMethod method = null;
        HttpClient httpclient = null;
        try {
            // Get file to be posted
            method = new GetMethod(strURL);
            // Get HTTP client
            httpclient = new HttpClient();
            // use  SimpleHttpConnectionManager cause one can close its ports and prevent close-wait states
            httpclient.setHttpConnectionManager(new SimpleHttpConnectionManager());
            HttpMethodRetryHandler myretryhandler = new HttpMethodRetryHandler() {
                @Override
                public boolean retryMethod(final HttpMethod method, final IOException exception, int executionCount) {
                    // do not retry
                    return executionCount <= 3;
                }
            };

            HttpParams httpParams = new HttpClientParams();
            httpParams.setParameter(HttpClientParams.RETRY_HANDLER, myretryhandler);
            httpParams.setIntParameter(HttpClientParams.SO_TIMEOUT, 60000);
            httpParams.setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, 10000);
            httpParams.setParameter(HttpClientParams.PREEMPTIVE_AUTHENTICATION, Boolean.FALSE);
            httpclient.setParams((HttpClientParams) httpParams);
            httpclient.getHostConfiguration().setProxyHost(null);
            if (log.isDebugEnabled()) {
                log.debug("About to call method...[{}]", strURL);
            }
            int result = httpclient.executeMethod(method);
            // Display status code
            if (log.isDebugEnabled()) {
                log.debug("Finished get to method");
                log.debug("HTTP Get Response status code: " + result);
                log.debug("Response body: ");
                log.debug(method.getResponseBodyAsString());
            }
            if (result != 200) {
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "ET", "Non 200 result code returned when doing callback on URL [URL: " + strURL + "] [Response: " + method.getResponseBodyAsString() + "]");
            }

        } catch (Exception e) {
            log.warn("Error posting to address " + strURL + " : " + e.toString());
            new ExceptionManager(log).reportError(e);
        } finally {
            // Release current connection to the connection pool once you are done
            try {
                if (method != null) {
                    method.releaseConnection();
                }
            } catch (Exception ex) {
                log.warn("Error releasing http get connection:" + ex.toString());
            }
            try {
                if (httpclient != null) {
                    ((SimpleHttpConnectionManager) httpclient.getHttpConnectionManager()).shutdown();
                }
            } catch (Exception ex) {
                log.warn("Error releasing http client connection:" + ex.toString());
            }
        }
    }

    public static PaymentGatewayPlugin getPlugin(EntityManager em, String paymentGatewayCode) throws Exception {
        String pluginClass = BaseUtils.getSubProperty("env.pgw." + paymentGatewayCode.toLowerCase() + ".config", "GatewayPluginClass");
        PaymentGatewayPlugin plugin = (PaymentGatewayPlugin) PaymentGatewayPlugin.class.getClassLoader().loadClass("com.smilecoms.pos.paymentgateway." + pluginClass).newInstance();
        plugin.init(em);
        return plugin;
    }

}
