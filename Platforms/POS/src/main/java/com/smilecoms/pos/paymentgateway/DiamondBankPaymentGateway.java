/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos.paymentgateway;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.paymentgateway.DiamondBankHelper;
import com.smilecoms.commons.paymentgateway.diamondbank.ITransactionStatusCheck;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.JiraField;
import com.smilecoms.commons.sca.MindMapFields;
import com.smilecoms.commons.sca.NewTTIssue;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.pos.db.model.Sale;
import java.math.BigDecimal;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.EntityManager;
import org.apache.commons.lang.StringEscapeUtils;

/**
 *
 * @author sabza
 */
public class DiamondBankPaymentGateway extends AbstractPaymentGateway implements PaymentGatewayPlugin {

    private static final Pattern PARAM_VALUE_PATTERN = Pattern.compile("<[a-zA-Z0-9_]+>");
    private static final String DIAMOND_BANK_CACHE_KEY = "diamond_bank_error_key";
    private static int ISUP_FAIL_COUNTER = 0;

    @Override
    public PaymentGatewayResult getPaymentGatewayResult(Sale dbSale) {
        log.debug("Entering getPaymentGatewayResult for DiamondBankPaymentGateway");

        PaymentGatewayResult res = new PaymentGatewayResult();

        try {

            log.debug("Soap client created successfully. Going to post to endpoint");
            String transactionDetails;

            try {
                ITransactionStatusCheck diamondBankProxy = getDiamondBankProxy();
                transactionDetails = diamondBankProxy.getTransactionDetails(String.valueOf(dbSale.getSaleId()), merchantId);
                transactionDetails = StringEscapeUtils.unescapeHtml(transactionDetails);
                log.debug("Soap request returned successfully for getTransactionDetails. Transaction details returned[{}]", transactionDetails);
            } catch (Exception ex) {
                transactionDetails = ex.toString();
                log.warn("For some reason we got an error communicating with payment_gateway: {}", transactionDetails);
                res.setSuccess(false);
                res.setTryAgainLater(true);
                String avp = "fullResponse" + "=" + transactionDetails;
                res.setGatewayResponse(avp);
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "Error in communicating with Diamond Bank: " + transactionDetails);
                DiamondBankPaymentGateway.ISUP_FAIL_COUNTER++;
                boolean isup = DiamondBankPaymentGateway.ISUP_FAIL_COUNTER <= BaseUtils.getIntProperty("env.pos.paymentgateway.isup.fail.counter", 5);
                publishGatewayInAvailabilityNotice(getName(), isup, DIAMOND_BANK_CACHE_KEY, ex.toString());
                return res;
            }

            String returnedStatus;
            boolean mustReportTransaction = false;

            /*Examples of various transactions and their format as received from DiamondBankPaymentGateway based on integration guide: "INTEGRATION GUIDE | JANUARY 2012 | VERSION 1.6" */
            //merchant_ID&          03991&                          03991&                             03991&                                03991&                         03991&
            //order_ID&             2081&                           2089&                              2185&                                 2191&                          2465&
            //transaction_ref&      2014091511315040T&              2014091512140276T&                 2014091615595362T&                    2014091616231839T&             2014092316161465T&
            //payment_gate&         MasterCard&                     No Payment Gateway Selected&       MasterCard&                           Visa&                          Visa&
            //status&               Failed&                         NotProcessed&                      Successful&                           Failed&                        Successful&
            //response_code&        Z6&                             04&                                00&                                   DECLINED&                      001&
            //response_description& Customer cancellation&          &                                  Approved Successful&                  DECLINED&                      Approved, no balances available&
            //date_time&            15-September-2014 11:31:50&     15-September-2014 12:14:02&        16-September-2014 15:59:53&           16-September-2014 16:23:18&    23-September-2014 16:16:14&
            //amount&               25.00&                          25.00&                             100.00&                               100.00&                        100.00&
            //currency_code         NGN                             NGN                                                                      NGN
            //ANDOR
            //payment_ref&                                                                             STANBIC|WEB|DBP|16-09-2014|020021&                                   902151 A&
            //currency_code         NGN                             NGN                                NGN                                                                  NGN
            try {
                String[] transactionDetailsAsArray = transactionDetails.split("&");

                if (transactionDetailsAsArray.length == 2) {

                    String attribute;
                    StringBuilder avpBuilder = new StringBuilder();

                    String returnedResponseCode = transactionDetailsAsArray[0];
                    attribute = "responseCode" + "=";
                    avpBuilder.append(attribute);
                    avpBuilder.append(returnedResponseCode);
                    avpBuilder.append("\r\n");

                    String returnedResponseDescription = transactionDetailsAsArray[1];
                    attribute = "responseDescription" + "=";
                    avpBuilder.append(attribute);
                    avpBuilder.append(returnedResponseDescription);
                    avpBuilder.append("\r\n");

                    attribute = "fullResponse=" + transactionDetails;
                    avpBuilder.append(attribute);

                    if (transactionDetails.contains("Timeout") || transactionDetails.contains("SQL")) {
                        DiamondBankPaymentGateway.ISUP_FAIL_COUNTER++;
                    }

                    //Maybe the thread was too quick to process the request as the user was probably reading terms&conditions or deciding on the payment method,
                    //if that was not the reason then it will become too old to be tried any more. Eventually setting status to EX
                    //E00&[No Transaction Record
                    if (returnedResponseDescription.contains("No Transaction Record")) {
                        res.setGatewayResponse(avpBuilder.toString());
                        res.setSuccess(false);
                        res.setTryAgainLater(true);
                        if (dbSale.getPaymentGatewayPollCount() > 45) {//15min=30x10sec + 15x60sec
                            //The transaction should have transitioned to other status by now, otherwise Diamond Bank really doesn't know about this record
                            //So we should be safe with 10min, otherwise SEP requeue would come to the rescue
                            res.setTryAgainLater(false);
                        }
                        //Reset
                        DiamondBankPaymentGateway.ISUP_FAIL_COUNTER = 0;
                        return res;
                    }
                }

                if (transactionDetailsAsArray.length < 9) {
                    log.warn("Diamond bank returned an invalid response. Possibly an error on their side [{}]", transactionDetails);
                    res.setGatewayResponse(transactionDetails);
                    res.setSuccess(false);
                    res.setTryAgainLater(true);
                    return res;
                }
                //Reset ISUP COUNTER
                DiamondBankPaymentGateway.ISUP_FAIL_COUNTER = 0;
                StringBuilder avpBuilder = new StringBuilder();
                String attribute;

                String returnedMerchantId = transactionDetailsAsArray[0];
                attribute = "merchantId" + "=";
                avpBuilder.append(attribute);
                avpBuilder.append(returnedMerchantId);
                avpBuilder.append("\r\n");

                String returnedOrderId = transactionDetailsAsArray[1];
                attribute = "orderId" + "=";
                avpBuilder.append(attribute);
                avpBuilder.append(returnedOrderId);
                avpBuilder.append("\r\n");

                String returnedTransactionRef = transactionDetailsAsArray[2];
                attribute = "transactionRef" + "=";
                avpBuilder.append(attribute);
                avpBuilder.append(returnedTransactionRef);
                avpBuilder.append("\r\n");

                String paymentGatewayName = transactionDetailsAsArray[3];
                attribute = "paymentGate" + "=";
                avpBuilder.append(attribute);
                avpBuilder.append(paymentGatewayName);
                res.setInfo(paymentGatewayName);//Payment gate
                avpBuilder.append("\r\n");

                returnedStatus = transactionDetailsAsArray[4];
                attribute = "status" + "=";
                avpBuilder.append(attribute);
                avpBuilder.append(returnedStatus);
                avpBuilder.append("\r\n");

                //Helps to keep track of the transition of statuses during the life cycle of this transaction
                String gatewayResponse = dbSale.getPaymentGatewayResponse() == null ? "" : dbSale.getPaymentGatewayResponse();
                if (Utils.getValueFromCRDelimitedAVPString(gatewayResponse, "statusHistory") != null) {
                    String statusHistory = Utils.getValueFromCRDelimitedAVPString(gatewayResponse, "statusHistory");
                    if (!statusHistory.contains(returnedStatus)) {//Store distinct statuses
                        attribute = "statusHistory" + "=";
                        avpBuilder.append(attribute);
                        avpBuilder.append(statusHistory).append(",").append(returnedStatus);
                        avpBuilder.append("\r\n");
                    }
                } else {
                    attribute = "statusHistory" + "=";
                    avpBuilder.append(attribute);
                    avpBuilder.append(returnedStatus);
                    avpBuilder.append("\r\n");
                }

                String returnedResponseCode = transactionDetailsAsArray[5];
                attribute = "responseCode" + "=";
                avpBuilder.append(attribute);
                avpBuilder.append(returnedResponseCode);
                avpBuilder.append("\r\n");

                String returnedResponseDescription = transactionDetailsAsArray[6];
                attribute = "responseDescription" + "=";
                avpBuilder.append(attribute);
                avpBuilder.append(returnedResponseDescription);
                avpBuilder.append("\r\n");
                if (returnedResponseDescription.equalsIgnoreCase("Transaction error") || returnedResponseDescription.equalsIgnoreCase("Transaction TimeOut")) {//See HBT-5197
                    mustReportTransaction = true;
                }

                String returnedDatetime = transactionDetailsAsArray[7];
                attribute = "dateTime" + "=";
                avpBuilder.append(attribute);
                avpBuilder.append(returnedDatetime);
                avpBuilder.append("\r\n");

                String returnedAmount = transactionDetailsAsArray[8];
                attribute = "amount" + "=";
                avpBuilder.append(attribute);
                avpBuilder.append(returnedAmount);
                avpBuilder.append("\r\n");

                //When no payment has been made the is no paymentRef, and the array size defaults to 10
                if (transactionDetailsAsArray.length > 10) {
                    String returnedPaymentRef = transactionDetailsAsArray[9];
                    attribute = "paymentRef" + "=";
                    avpBuilder.append(attribute);
                    avpBuilder.append(returnedPaymentRef);
                    avpBuilder.append("\r\n");

                    String returnedCurrency = transactionDetailsAsArray[10];
                    attribute = "currencyCode" + "=";
                    avpBuilder.append(attribute);
                    avpBuilder.append(returnedCurrency);
                    avpBuilder.append("\r\n");
                } else {
                    String returnedCurrency = transactionDetailsAsArray[9];
                    attribute = "currencyCode" + "=";
                    avpBuilder.append(attribute);
                    avpBuilder.append(returnedCurrency);
                    avpBuilder.append("\r\n");
                }

                attribute = "fullResponse" + "=";
                avpBuilder.append(attribute);
                avpBuilder.append(transactionDetails);

                res.setGatewayResponse(avpBuilder.toString());
                res.setTransferredAmountCents(Double.parseDouble(returnedAmount) * 100);
                res.setPaymentGatewayTransactionId(returnedTransactionRef);
            } catch (Exception ex) {
                log.warn("DiamondBankPaymentGateway error [{}]", ex.toString());
                log.warn("Error: ", ex);
                res.setGatewayResponse(transactionDetails);
                res.setSuccess(false);
                res.setTryAgainLater(true);
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "DiamondBankPaymentGateway failed to process[" + transactionDetails + "] reason: " + ex.toString());
                return res;
            }

            //Diamond Bank possible statuses: Successful, Failed, Pending, Cancelled, Not Processed, Invalid Merchant, Inactive Merchant, Invalid Order ID, Duplicate Order ID, Invalid Amount
            if (returnedStatus.equalsIgnoreCase("Successful")) {
                log.debug("Diamond bank transaction completed successfully: {}", returnedStatus);
                res.setSuccess(true);
                res.setTryAgainLater(false);
            } else if (returnedStatus.equalsIgnoreCase("Pending")) {
                log.debug("Diamond bank transaction is in progress, will be retried: {}", returnedStatus);
                res.setSuccess(false);
                res.setTryAgainLater(true);
            } else if (returnedStatus.equalsIgnoreCase("NotProcessed")) {
                //Maybe the thread was too quick to process the request, as the user was still busy or internet connection was slow or Diamond Bank still contacting payment gateway
                //If that was not the reason then it will become too old to be tried any more. Eventually setting status to EX
                log.debug("Diamond bank transaction might not be complete, will be retried: {}", returnedStatus);
                res.setSuccess(false);
                res.setTryAgainLater(true);
                if (dbSale.getPaymentGatewayPollCount() > 40) {//10min
                    //The transaction should have transitioned to other status by now.
                    //So we should be safe with 10min, otherwise SEP requeue would come to the rescue
                    res.setTryAgainLater(false);
                }
            } else {
                log.warn("Diamond bank transaction failed completely for sale id [{}], cannot be retried ever again: [{}]", dbSale.getSaleId(), returnedStatus);
                res.setSuccess(false);
                res.setTryAgainLater(false);
                if (mustReportTransaction) {
                    sendNotificationOnGatewayTransactionError(dbSale.getRecipientCustomerId(), transactionDetails);
                    sendSMSOnGatewayTransactionError(dbSale.getSaleId());
                    sendEmailOnGatewayTransactionError(dbSale.getRecipientCustomerId(), transactionDetails);
                }
            }

        } catch (Exception ex) {
            log.warn("System error in diamond bank plugin: ", ex);
            res.setGatewayResponse(ex.toString());
            res.setSuccess(false);
            res.setTryAgainLater(true);
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "Error in Diamond Bank PaymentGateway plugin: " + ex.toString());
        }

        log.debug("Exiting getPaymentGatewayResult for DiamondBankPaymentGateway");
        return res;
    }

    private ITransactionStatusCheck getDiamondBankProxy() {
        return DiamondBankHelper.getDiamondBankProxy();
    }

    @Override
    public void init(EntityManager em) {
        merchantId = BaseUtils.getSubProperty("env.pgw.diamond.config", "ProfileID");
        currencyCode = BaseUtils.getSubProperty("env.pgw.diamond.config", "CurrencyCode");
        paymentGatewayPostURL = BaseUtils.getSubProperty("env.pgw.diamond.config", "PaymentGatewayPostURL");
    }

    private static String merchantId;
    private static String currencyCode;
    private static String paymentGatewayPostURL;

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
        sb.append("amt=");
        sb.append(dbSale.getSaleTotalCentsIncl().divide(new BigDecimal(100)));
        sb.append(",orderId=");
        sb.append(dbSale.getSaleId());
        sb.append(",prod=");
        sb.append("Smile data and voice services");
        sb.append(",mercId=");
        sb.append(merchantId);
        sb.append(",currCode=");
        sb.append(currencyCode);
        sb.append(",email=<customer_email>");
        String ret = removePlaceHolder(sb.toString(), dbSale.getRecipientCustomerId());
        return ret;
    }

    @Override
    public long getAccountId() {
        return Long.valueOf(BaseUtils.getSubProperty("env.pgw.diamond.config", "AccountId"));
    }

    @Override
    public void processPaymentNotification(int saleId, double cashReciepted, String transactionId, String paymentGatewayExtraData) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private String removePlaceHolder(String data, int customerId) {
        try {
            int indx = data.indexOf("<");
            if (indx == -1) {
                return data;
            }
            log.debug("Before replacement: [{}]", data);
            Matcher m = PARAM_VALUE_PATTERN.matcher(data);
            Customer customer = getCustomer(customerId);
            while (m.find()) {
                String parameterValueWithBrackets = m.group();
                log.debug("Property with matching pattern found: [{}]", parameterValueWithBrackets);
                String parameterValuePlaceholder = parameterValueWithBrackets.replace("<", "").replace(">", "");
                log.debug("Parameter value placeholder name is [{}]", parameterValuePlaceholder);
                if (parameterValuePlaceholder.equals("customer_email")) {
                    String parameterValue = customer.getEmailAddress();
                    log.debug("Replacing [{}] with [{}]", parameterValueWithBrackets, parameterValue);
                    data = data.replace(parameterValueWithBrackets, parameterValue);
                }
            }
        } catch (Exception e) {
            log.warn("Error doing parameter value replacement: {}", e);
        }
        return data;
    }

    @Override
    public void sendSMSForGatewayInavailability() {
        try {
            Set<String> configWatchers = BaseUtils.getPropertyAsSet("env.pos.paymentgateway.sms.isup.watchers");
            String sms = "Diamond bank interface alert: Failing to get proper response when communicating with payment gateway. Please monitor environment to determine issue severity.";
            for (String watcher : configWatchers) {
                sendSMS(watcher, sms);
            }
        } catch (Exception ex) {
        }
    }

    private void sendSMSOnGatewayTransactionError(int saleId) {
        try {
            if (!BaseUtils.getBooleanProperty("env.pos.paymentgateway.send.transactionerror.sms", true)) {
                return;
            }
            Set<String> configWatchers = BaseUtils.getPropertyAsSet("env.pos.paymentgateway.sms.transactionerror.watchers");
            String sms = "Diamond bank transaction error: Transaction with SALE_ID " + saleId + " failed, you can view the transaction on SEP for details.";
            for (String watcher : configWatchers) {
                sendSMS(watcher, sms);
            }
        } catch (Exception ex) {
        }
    }

    @Override
    public void addPaymentGatewaySpecificsOnTicket(NewTTIssue tt, String errorMessage) {
        JiraField jiraF = new JiraField();

        jiraF.setFieldName("TT_FIXED_FIELD_Description");
        jiraF.setFieldType("TT_FIXED_FIELD");
        StringBuilder description = new StringBuilder("{panel:title=(!) ");
        description.append("Warning: Diamond Bank is currently not available to query transaction statuses");
        description.append("|borderStyle=dashed|titleBGColor=red}");
        description.append("Diamond Bank interface to query transaction statuses is currently not available\n")
                .append("Customers using MySmile or Xpress recharge to recharge would be affected\n").append("{panel}");
        jiraF.setFieldValue(description.toString());
        tt.getMindMapFields().getJiraField().add(jiraF);

        jiraF = new JiraField();
        jiraF.setFieldName("TT_FIXED_FIELD_Summary");
        jiraF.setFieldType("TT_FIXED_FIELD");
        String summary = "Diamond Bank API interface instability";
        jiraF.setFieldValue(summary);
        tt.getMindMapFields().getJiraField().add(jiraF);

        jiraF = new JiraField();
        jiraF.setFieldName("TT_FIXED_FIELD_Issue Type");
        jiraF.setFieldType("TT_FIXED_FIELD");
        jiraF.setFieldValue("Fault");
        tt.getMindMapFields().getJiraField().add(jiraF);

        jiraF = new JiraField();
        jiraF.setFieldName("TT_FIXED_FIELD_Project");
        jiraF.setFieldType("TT_FIXED_FIELD");
        jiraF.setFieldValue("NGIT");
        tt.getMindMapFields().getJiraField().add(jiraF);

    }

    private void sendNotificationOnGatewayTransactionError(int recipientCustomerId, String transactionDetails) {
        if (!BaseUtils.getBooleanProperty("env.pos.paymentgateway.create.transactionerror.ticket", true)) {
            return;
        }
        try {
            NewTTIssue tt = new NewTTIssue();
            tt.setMindMapFields(new MindMapFields());
            JiraField jiraF = new JiraField();

            jiraF.setFieldName("TT_FIXED_FIELD_Description");
            jiraF.setFieldType("TT_FIXED_FIELD");
            StringBuilder description = new StringBuilder("{panel:title=(!) ");
            description.append("Diamond Bank transaction was unsuccessful");
            description.append("|borderStyle=dashed|titleBGColor=red}");
            description.append("# Transaction details:\n").append("merchant_ID&order_ID&transaction_ref&payment_gate&status&response_code&response_description&date_time&amount&payment_ref&currency_code\n").append(transactionDetails).append("{panel}");
            jiraF.setFieldValue(description.toString());
            tt.getMindMapFields().getJiraField().add(jiraF);

            jiraF = new JiraField();
            jiraF.setFieldName("TT_FIXED_FIELD_Summary");
            jiraF.setFieldType("TT_FIXED_FIELD");
            String summary = "Diamond Bank transaction error on MySmile or Xpress Recharge [CustomerID " + recipientCustomerId + "]";
            jiraF.setFieldValue(summary);
            tt.getMindMapFields().getJiraField().add(jiraF);

            jiraF = new JiraField();
            jiraF.setFieldName("TT_FIXED_FIELD_Issue Type");
            jiraF.setFieldType("TT_FIXED_FIELD");
            jiraF.setFieldValue("Fault");
            tt.getMindMapFields().getJiraField().add(jiraF);

            jiraF = new JiraField();
            jiraF.setFieldName("TT_FIXED_FIELD_Project");
            jiraF.setFieldType("TT_FIXED_FIELD");
            jiraF.setFieldValue("NGIT");
            tt.getMindMapFields().getJiraField().add(jiraF);

            tt.setCustomerId("0");
            String issueID = createTicket(tt);
            log.debug("Transaction failure reported, ID: {}", issueID);
        } catch (Exception e) {
            log.warn("Failed to report status FG, reason: {}", e.toString());
        }
    }

    private void sendEmailOnGatewayTransactionError(int recipientCustomerId, String transactionDetails) {
        if (!BaseUtils.getBooleanProperty("env.pos.paymentgateway.send.transactionerror.email", false)) {
            return;
        }
        try {
            Set<String> configWatchers = BaseUtils.getPropertyAsSet("env.pos.paymentgateway.email.isup.watchers");
            String from = BaseUtils.getProperty("env.pos.paymentgateway.email.notification.from", "admin@smilecoms.com");
            String bodyPart = ",<br/><br/>Diamond Bank transaction was unsuccessful, <strong>transaction details format:</strong> merchant_ID&order_ID&transaction_ref&payment_gate&status&response_code&response_description&date_time&amount&payment_ref&currency_code"
                    + "<br/><strong>" + transactionDetails + "</strong><br/>"
                    + "<br/><br/>Use order_ID to view more details on SEP.<br/><br/>";

            String subject = "Diamond Bank transaction error on MySmile or Xpress Recharge [CustomerID " + recipientCustomerId + "]";
            for (String to : configWatchers) {
                String fullBody = "Hi " + to + bodyPart;
                sendEmail(from, to, subject, fullBody);
            }
        } catch (Exception ex) {
        }
    }

    @Override
    public boolean isUp() {
        boolean isup = DiamondBankPaymentGateway.ISUP_FAIL_COUNTER <= BaseUtils.getIntProperty("env.pos.paymentgateway.isup.fail.counter", 5);
        BaseUtils.sendStatistic("DiamondBank", "PaymentGateway", "isup", isup ? 1 : 0, "POS");
        return isup;
    }

    @Override
    public String getName() {
        return "DiamondBank";
    }

}
