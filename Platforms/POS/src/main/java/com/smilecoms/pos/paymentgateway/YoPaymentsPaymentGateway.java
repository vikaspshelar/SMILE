/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos.paymentgateway;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.paymentgateway.YoPayments;
import com.smilecoms.commons.sca.JiraField;
import com.smilecoms.commons.sca.MindMapFields;
import com.smilecoms.commons.sca.NewTTIssue;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.pos.db.model.Sale;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
import org.apache.commons.lang.StringEscapeUtils;

/**
 *
 * @author sabza
 */
public class YoPaymentsPaymentGateway extends AbstractPaymentGateway implements PaymentGatewayPlugin {

    private static final String YO_PAYMENTS_ISUP_CACHE_KEY = "yopayments_isup_error_key";
    private static int ISUP_FAIL_COUNTER = 0;
    private static final Map<String, String> headers = new HashMap<>();

    @Override
    public PaymentGatewayResult getPaymentGatewayResult(Sale dbSale) {
        PaymentGatewayResult res = new PaymentGatewayResult();
        String transactionDetails = "";
        String req = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<AutoCreate>\n"
                + "<Request>\n"
                + "<APIUsername>" + APIUsername + "</APIUsername>\n"
                + "<APIPassword>" + APIPassword + "</APIPassword>\n"
                + "<Method>actransactioncheckstatus</Method>\n"
                //+ "<TransactionReference></TransactionReference>\n"
                + "<PrivateTransactionReference>" + dbSale.getSaleId() + "</PrivateTransactionReference>\n"
                //+ "<DepositTransactionType></DepositTransactionType>\n"
                + "</Request>\n"
                + "</AutoCreate>";
        try {

            if (dbSale.getPaymentGatewayPollCount() < 5) {
                res.setTryAgainLater(true);
                res.setGatewayResponse("statusHistory=NOT_SENT");
                res.setSuccess(false);
                return res;
            }

            try {
                YoPayments yo = new YoPayments();
                transactionDetails = StringEscapeUtils.unescapeXml(yo.getTransactionDetails(statusQueryURL, headers, req));
                log.debug("HTTP client request returned successfully for getTransactionDetails. Transaction details returned[{}]", transactionDetails);
            } catch (Exception ex) {
                log.warn("System error in Yo! Payments plugin: ", ex);
                res.setGatewayResponse(ex.toString());
                res.setSuccess(false);
                res.setTryAgainLater(true);
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "Error in YoPayments PaymentGateway plugin: " + ex.toString());
                YoPaymentsPaymentGateway.ISUP_FAIL_COUNTER++;
                
                boolean isup = YoPaymentsPaymentGateway.ISUP_FAIL_COUNTER <= BaseUtils.getIntProperty("env.pos.paymentgateway.isup.fail.counter", 5);
                publishGatewayInAvailabilityNotice(getName(), isup, YO_PAYMENTS_ISUP_CACHE_KEY, ex.toString());
                return res;
            }

            /*
            <?xml version="1.0" encoding="UTF-8"?>
            <AutoCreate>
                <Response>
                    <Status>ERROR</Status>
                    <StatusCode>-30</StatusCode>
                    <StatusMessage>The transaction was not found in the system. Please verify your transaction reference.</StatusMessage>
                </Response>
            </AutoCreate>
            
            <?xml version="1.0" encoding="UTF-8"?>
            <AutoCreate>
                <Response>
                    <Status>OK</Status>
                    <StatusCode>0</StatusCode>
                    <TransactionStatus>SUCCEEDED</TransactionStatus>
                    <TransactionReference>OEGMNetmTftDR9eVL6mqgmpvQCr7riz5lT2onx0Doift1TVu9lyzy0c93irpcEvFd154be6802d76f0c4cb158404ca5b8f5</TransactionReference>
                    <MNOTransactionReferenceId>Sandbox-Transaction-Id-RecvPmt</MNOTransactionReferenceId>
                    <Amount>3432</Amount>
                    <AmountFormatted>UGX 3,432/=</AmountFormatted>
                    <CurrencyCode>UGX-MTNMM</CurrencyCode>
                    <TransactionInitiationDate>2016-11-28 13:36:41</TransactionInitiationDate>
                    <TransactionCompletionDate>2016-11-28 13:37:17</TransactionCompletionDate>
                    <IssuedReceiptNumber>5</IssuedReceiptNumber>
                </Response>
            </AutoCreate>
             */
            YoPaymentsPaymentGateway.ISUP_FAIL_COUNTER = 0;
            updateGatewayStatus("env.scp.yo.partner.integration.config", true, getName());
            String statusCode = Utils.getBetween(transactionDetails, "<StatusCode>", "</StatusCode>");
            String status = Utils.getBetween(transactionDetails, "<Status>", "</Status>");
            if (statusCode == null) {
                log.debug("Yo! Payments did not return a status in their response. Assuming its pending");
                statusCode = "NUL";
            }

            if (status.equals("ERROR")) {
                res.setGatewayResponse(transactionDetails);
                res.setSuccess(false);
                res.setTryAgainLater(true);

                if (statusCode.equals("-30")) {//-30 : The transaction was not found in the system. Please verify your transaction reference.
                    if (dbSale.getPaymentGatewayPollCount() > 45) {//15min=30x10sec + 15x60sec
                        //The transaction should have transitioned to other status by now, otherwise Yo! Payments really doesn't know about this record
                        //So we should be safe with 10min, otherwise SEP requeue would come to the rescue
                        res.setTryAgainLater(false);
                    }
                } else if (statusCode.equals("-999")) {
                    String statusMessage = Utils.getBetween(transactionDetails, "<StatusMessage>", "</StatusMessage>");
                    log.warn("XML causing problems: {}", req);
                    BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "POS", "YoPayments plugin says status is (-999 invalid XML), statusMessage: " + statusMessage == null ? "" : statusMessage);
                }

                String transactionStatus = Utils.getBetween(transactionDetails, "<TransactionStatus>", "</TransactionStatus>");
                transactionStatus = transactionStatus == null ? "" : transactionStatus;

                if (transactionStatus.equalsIgnoreCase("INDETERMINATE")) {
                    log.warn("Yo! Payments transaction has an INDETERMINATE status, will search for codes that can possible change to success, and flag it as pending");
                    String[] pendingPaymentStatus = pendingPaymentStatuses.split(",");
                    res.setTryAgainLater(false);
                    for (String pendingStatus : pendingPaymentStatus) {
                        if (pendingStatus.equals(statusCode)) {
                            res.setTryAgainLater(true);
                            log.debug("The is a probability that this transaction with sale id {}, can transition to success: {}, {}, {}", new Object[]{dbSale.getSaleId(), statusCode, status, transactionStatus});
                            break;
                        }
                    }
                    log.warn("The is a probability that this transaction with sale id {}, can transition to fail: {}, {}, {}", new Object[]{dbSale.getSaleId(), statusCode, status, transactionStatus});

                } else if (transactionStatus.equalsIgnoreCase("FAILED")) {
                    log.debug("Yo! Payments  transaction failed completely, not going to retry it: {}", statusCode);
                    res.setTryAgainLater(false);
                }
                return res;
            }

            String uniqueTransactionId = Utils.getBetween(transactionDetails, "<TransactionReference>", "</TransactionReference>");
            String responseTime = Utils.getBetween(transactionDetails, "<TransactionCompletionDate>", "</TransactionCompletionDate>");
            String amount = Utils.getBetween(transactionDetails, "<Amount>", "</Amount>");
            String mnoRefId = Utils.getBetween(transactionDetails, "<MNOTransactionReferenceId>", "</MNOTransactionReferenceId>");
            String transactionStatus = Utils.getBetween(transactionDetails, "<TransactionStatus>", "</TransactionStatus>");
            String orderId = String.valueOf(dbSale.getSaleId());

            StringBuilder avpBuilder = new StringBuilder();
            avpBuilder.append("orderId=");
            avpBuilder.append(orderId);
            avpBuilder.append("\r\n");

            avpBuilder.append("amount=");
            avpBuilder.append(amount);
            avpBuilder.append("\r\n");

            avpBuilder.append("TransactionCompletionDate=");
            avpBuilder.append(responseTime);
            avpBuilder.append("\r\n");

            avpBuilder.append("status=");
            avpBuilder.append(statusCode);
            avpBuilder.append("\r\n");

            //Helps to keep track of the transition of statuses during the life cycle of this transaction
            String gatewayResponse = dbSale.getPaymentGatewayResponse() == null ? "" : dbSale.getPaymentGatewayResponse();
            if (Utils.getValueFromCRDelimitedAVPString(gatewayResponse, "statusHistory") != null) {
                String statusHistory = Utils.getValueFromCRDelimitedAVPString(gatewayResponse, "statusHistory");
                if (!statusHistory.contains(statusCode)) {
                    avpBuilder.append("statusHistory=");
                    avpBuilder.append(statusHistory).append(",").append(statusCode);
                    avpBuilder.append("\r\n");
                }
            } else {
                avpBuilder.append("statusHistory=");
                avpBuilder.append(statusCode);
                avpBuilder.append("\r\n");
            }

            avpBuilder.append("transactionRef=");
            avpBuilder.append(mnoRefId);
            avpBuilder.append("\r\n");

            avpBuilder.append("paymentGate=");
            avpBuilder.append(mnoRefId);
            res.setInfo(mnoRefId == null ? "" : mnoRefId.replaceAll("\\s", ""));
            avpBuilder.append("\r\n");

            avpBuilder.append("responseDescription=");
            avpBuilder.append(transactionStatus);
            avpBuilder.append("\r\n");

            avpBuilder.append("fullResponse=");
            avpBuilder.append(transactionDetails);

            res.setPaymentGatewayTransactionId(uniqueTransactionId);
            res.setTransferredAmountCents(Double.parseDouble(amount) * 100);
            res.setGatewayResponse(avpBuilder.toString());

            if (statusCode.equalsIgnoreCase("0")) {
                log.debug("Yo! Payments transaction completed successfully: [statusCode {}, status {}, transactionStatus {}]", new Object[]{statusCode, status, transactionStatus});
                res.setSuccess(false);
                res.setTryAgainLater(true);
                if (transactionStatus.equals("SUCCEEDED")) {
                    res.setSuccess(true);
                    res.setTryAgainLater(false);
                }
            } else if (!statusCode.equalsIgnoreCase("0")) {

                if (transactionStatus.equalsIgnoreCase("PENDING")) {
                    log.debug("Yo! Payments  transaction is in progress, will be retried: {}", statusCode);
                    res.setSuccess(false);
                    res.setTryAgainLater(true);

                } else if (transactionStatus.equalsIgnoreCase("INDETERMINATE")) {

                    log.warn("Yo! Payments transaction has an INDETERMINATE status, will search for codes that can possible change to success, and flag it as pending");
                    String[] pendingPaymentStatus = pendingPaymentStatuses.split(",");
                    res.setSuccess(false);
                    res.setTryAgainLater(false);
                    for (String pendingStatus : pendingPaymentStatus) {
                        if (pendingStatus.equals(statusCode)) {
                            res.setTryAgainLater(true);
                            log.debug("The is a probability that this transaction with sale id {}, can transition to success: {}, {}, {}", new Object[]{orderId, statusCode, status, transactionStatus});
                            break;
                        }
                    }
                    log.warn("The is a probability that this transaction with sale id {}, can transition to fail: {}, {}, {}", new Object[]{orderId, statusCode, status, transactionStatus});

                } else if (transactionStatus.equalsIgnoreCase("FAILED")) {
                    log.debug("Yo! Payments  transaction failed completely, not going to try it again:  {}, {}", statusCode, transactionStatus);
                    res.setSuccess(false);
                    res.setTryAgainLater(false);
                }
            }

        } catch (Exception ex) {
            String errorString = ex.toString();
            log.warn("YoPaymentsPaymentGateway: For some reason we got an error", ex);
            res.setSuccess(false);
            res.setTryAgainLater(true);
            String avp = "fullResponse=" + transactionDetails;
            res.setGatewayResponse(avp);
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "YoPaymentsPaymentGateway failed to process[" + transactionDetails + "] reason: " + errorString);
            return res;
        }

        return res;
    }

    private static String currencyCode;
    private static String paymentGatewayPostURL;
    private static String callbackURL;
    private static String accountId;
    private static String APIUsername;
    private static String APIPassword;
    private static String merchantId;
    private static String bid;
    private static String statusQueryURL;
    private static String pendingPaymentStatuses;

    @Override
    public void init(EntityManager em) {
        currencyCode = BaseUtils.getSubProperty("env.pgw.yopayments.config", "CurrencyCode");
        merchantId = BaseUtils.getSubProperty("env.pgw.yopayments.config", "MerchantId");
        paymentGatewayPostURL = BaseUtils.getSubProperty("env.pgw.yopayments.config", "PaymentGatewayPostURL");
        callbackURL = BaseUtils.getSubProperty("env.pgw.yopayments.config", "CallbackURL");
        accountId = BaseUtils.getSubProperty("env.pgw.yopayments.config", "AccountId");
        bid = BaseUtils.getSubProperty("env.pgw.yopayments.config", "Bid");
        statusQueryURL = BaseUtils.getSubProperty("env.pgw.yopayments.config", "StatusQueryURL");
        APIUsername = BaseUtils.getSubProperty("env.pgw.yopayments.config", "APIUsername");
        APIPassword = BaseUtils.getSubProperty("env.pgw.yopayments.config", "APIPassword");
        pendingPaymentStatuses = BaseUtils.getSubProperty("env.pgw.yopayments.config", "PendingPaymentStatuses");//Currently not sure which are valid statuses to monitor for Pending, going to build some artificial intelligence to handle pending transactions
        headers.put("Content-Type", "text/xml");
        headers.put("Content-Transfer-Encoding", "text");
    }

    @Override
    public String getGatewayURL(Sale dbSale) {
        return paymentGatewayPostURL;
    }

    @Override
    public String getLandingURL(Sale dbSale) {
        if (dbSale.getLandingURL().contains("?")) {
            return dbSale.getLandingURL() + "&" + "saleId=" + dbSale.getSaleId();
        } else {
            return dbSale.getLandingURL() + "?" + "saleId=" + dbSale.getSaleId();
        }
    }

    @Override
    public String getGatewayURLData(Sale dbSale) {
        StringBuilder sb = new StringBuilder();
        sb.append("POST: ");
        sb.append("amount=");
        sb.append(dbSale.getSaleTotalCentsIncl().divide(new BigDecimal(100)));
        sb.append(",reference=");
        sb.append(dbSale.getSaleId());
        sb.append(",account=");
        sb.append(merchantId);
        sb.append(",narrative=");
        sb.append("Smile Shop");
        sb.append(",provider_reference_text=");//can be same as 'narrative'
        sb.append("Smile Shop");
        sb.append(",currency=");
        sb.append(currencyCode);
        sb.append(",bid=");
        sb.append(bid);
        sb.append(",return=");
        callbackURL = callbackURL.replaceAll("OrderId#", "OrderId#" + String.valueOf(dbSale.getSaleId()));
        sb.append(callbackURL);//e.g. https://154.73.71.2/scp/PaymentGateway.action?processBankTransaction#&OrderId#1234

        return sb.toString();
    }

    @Override
    public long getAccountId() {
        return Long.valueOf(accountId);
    }

    @Override
    public boolean isUp() {
        boolean isup = YoPaymentsPaymentGateway.ISUP_FAIL_COUNTER <= BaseUtils.getIntProperty("env.pos.paymentgateway.isup.fail.counter", 5);
        BaseUtils.sendStatistic("YoPayments", "PaymentGateway", "isup", isup ? 1 : 0, "POS");
        updateGatewayStatus("env.scp.yo.partner.integration.config", isup, getName());
        return isup;
    }

    @Override
    public void processPaymentNotification(int saleId, double cashReciepted, String transactionId, String paymentGatewayExtraData) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void publishGatewayInAvailabilityNotice1(String errorMessage) {
        boolean isup = YoPaymentsPaymentGateway.ISUP_FAIL_COUNTER <= BaseUtils.getIntProperty("env.pos.paymentgateway.isup.fail.counter", 5);
        updateGatewayStatus("env.scp.yo.partner.integration.config", isup, getName());
        if (isup) {
            return;
        }
        if (!BaseUtils.getBooleanProperty("env.paymentgateway.notify.customercare", true)) {
            return;
        }
        try {
            if (checkIfGatewayInAvailabilityAlreadyReported(YoPaymentsPaymentGateway.YO_PAYMENTS_ISUP_CACHE_KEY)) {
                return;
            }
            createTicketForGatewayInavailability(YO_PAYMENTS_ISUP_CACHE_KEY, errorMessage);
            sendSMSForGatewayInavailability();
        } catch (Exception ex) {
            log.warn("Failed to report YoPayments inavailability to customer care, reason: {}", ex.toString());
        }
    }

    @Override
    public void addPaymentGatewaySpecificsOnTicket(NewTTIssue tt, String errorMessage) {
        JiraField jiraF = new JiraField();

        jiraF.setFieldName("TT_FIXED_FIELD_Description");
        jiraF.setFieldType("TT_FIXED_FIELD");
        StringBuilder description = new StringBuilder("{panel:title=(!) ");
        description.append("Warning: YoPayments is currently not available to query transaction statuses");
        description.append("|borderStyle=dashed|titleBGColor=red}");
        description.append("YoPayments interface to query transaction statuses is currently not available\n")
                .append("Customers using MySmile to recharge would be affected\n").append("{panel}");
        jiraF.setFieldValue(description.toString());
        tt.getMindMapFields().getJiraField().add(jiraF);

        jiraF = new JiraField();
        jiraF.setFieldName("TT_FIXED_FIELD_Summary");
        jiraF.setFieldType("TT_FIXED_FIELD");
        String summary = "YoPayments API interface instability";
        jiraF.setFieldValue(summary);
        tt.getMindMapFields().getJiraField().add(jiraF);

        jiraF = new JiraField();
        jiraF.setFieldName("TT_FIXED_FIELD_Issue Type");
        jiraF.setFieldType("TT_FIXED_FIELD");
        jiraF.setFieldValue("Customer Incident");
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
    }

    @Override
    public void sendSMSForGatewayInavailability() {
        try {
            Set<String> configWatchers = BaseUtils.getPropertyAsSet("env.pos.paymentgateway.sms.isup.watchers");
            String sms = "YoPayments interface alert: Failing to get transaction status response when communicating with payment gateway. Please monitor environment to determine issue severity.";
            for (String watcher : configWatchers) {
                sendSMS(watcher, sms);
            }
        } catch (Exception ex) {
        }
    }

    @Override
    public String getName() {
        return "YoPayments";
    }

}
