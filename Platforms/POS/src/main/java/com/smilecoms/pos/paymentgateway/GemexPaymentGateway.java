/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos.paymentgateway;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.paymentgateway.GemexRestClientWrapper;
import com.smilecoms.commons.sca.JiraField;
import com.smilecoms.commons.sca.MindMapFields;
import com.smilecoms.commons.sca.NewTTIssue;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.pos.db.model.Sale;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author sabza
 */
public class GemexPaymentGateway extends AbstractPaymentGateway implements PaymentGatewayPlugin {

    private static final String GEMEX_ISUP_CACHE_KEY = "gemex_isup_error_key";
    private static GemexRestClientWrapper sharedClient = null;
    private static long connectionLastReaped = 0;
    private static int ISUP_FAIL_COUNTER = 0;

    @Override
    public PaymentGatewayResult getPaymentGatewayResult(Sale dbSale) {
        log.debug("Entering getPaymentGatewayResult for GemexPaymentGateway");

        PaymentGatewayResult res = new PaymentGatewayResult();
        String transactionDetails = "";

        try {

            /*
             <Result xmlns="http://schemas.datacontract.org/2004/07/GemexService" xmlns:i="http://www.w3.org/2001/XMLSchema-instance">
             <ListOfOrder/>
             <ResponseResult>0</ResponseResult>
             </Result>
                
             <Result xmlns="http://schemas.datacontract.org/2004/07/GemexService" xmlns:i="http://www.w3.org/2001/XMLSchema-instance">
             <ListOfOrder>
             <Order>
             <Amount>1800.00</Amount>
             <Description>Payment Transaction Successfull.</Description>
             <OrderID>4539</OrderID>
             <PayMethod>Mobile Money</PayMethod>
             <Product>1GB Data Bundle</Product>
             <ResponseTime>24/06/2015 05:03:56</ResponseTime>
             <Status>Successful</Status>
             <UniqueTransactionId>SUG18357794</UniqueTransactionId>
             </Order>
             </ListOfOrder>
             <ResponseResult>1</ResponseResult>
             </Result>
                
             <Result xmlns="http://schemas.datacontract.org/2004/07/GemexService" xmlns:i="http://www.w3.org/2001/XMLSchema-instance">
             <ListOfOrder>
             <Order>
             <Amount>4000.00</Amount>
             <Description/>
             <OrderID>4563</OrderID>
             <PayMethod>PayPal</PayMethod>
             <Product>SMILE AIRTIME</Product>
             <ResponseTime>01/01/1900 00:00:00</ResponseTime>
             <Status>Pending</Status>
             <UniqueTransactionId>SUG98511623</UniqueTransactionId>
             </Order>
             </ListOfOrder>
             <ResponseResult>1</ResponseResult>
             </Result>
                
             */
            try {
                transactionDetails = getSharedRestClient().getTransactionDetails(String.valueOf(dbSale.getSaleId()));//Methods to create instances of WebResource are thread-safe.
                log.debug("Rest client request returned successfully for getTransactionDetails. Transaction details returned[{}]", transactionDetails);
            } catch (Exception ex) {
                log.warn("System error in Gemex plugin: ", ex);
                res.setGatewayResponse(ex.toString());
                res.setSuccess(false);
                res.setTryAgainLater(true);
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "Error in Gemex PaymentGateway plugin: " + ex.toString());
                GemexPaymentGateway.ISUP_FAIL_COUNTER++;
                
                boolean isup = GemexPaymentGateway.ISUP_FAIL_COUNTER <= BaseUtils.getIntProperty("env.pos.paymentgateway.isup.fail.counter", 5);
                publishGatewayInAvailabilityNotice(getName(), isup, GEMEX_ISUP_CACHE_KEY, ex.toString());
                return res;
            }

            String responseResult;
            GemexPaymentGateway.ISUP_FAIL_COUNTER = 0;
            updateGatewayStatus("env.scp.gemex.partner.integration.config", true, getName());

            try {
                responseResult = Utils.getBetween(transactionDetails, "<ResponseResult>", "</ResponseResult>");
                if (responseResult == null || responseResult.isEmpty()) {
                    throw new Exception("Gemex returned an invalid response. Possibly an error on their side");
                }
            } catch (Exception ex) {
                log.warn("Gemex returned an invalid response. Possibly an error on their side [{}]", transactionDetails);
                res.setGatewayResponse(transactionDetails);
                res.setSuccess(false);
                res.setTryAgainLater(true);
                return res;
            }

            if (Integer.parseInt(responseResult) == 0) {

                String attribute;
                StringBuilder avpBuilder = new StringBuilder();
                attribute = "fullResponse=" + transactionDetails;
                avpBuilder.append(attribute);

                //Maybe the thread was too quick to process the request as the user was probably reading terms&conditions or deciding on the payment method,
                //if that was not the reason then it will become too old to be tried any more. Eventually setting status to EX
                res.setGatewayResponse(avpBuilder.toString());
                res.setSuccess(false);
                res.setTryAgainLater(true);
                if (dbSale.getPaymentGatewayPollCount() > 45) {//15min=30x10sec + 15x60sec
                    //The transaction should have transitioned to other status by now, otherwise Gemex really doesn't know about this transaction
                    //So we should be safe with 10min, otherwise SEP requeue would come to the rescue
                    res.setTryAgainLater(false);
                }
                return res;
            }

            String originalResponse = transactionDetails;
            if (Integer.parseInt(responseResult) > 1) {
                log.warn("Received response in format not agreed upon, going to process this response using Java DOM XML Parser");

                //Below is a sample response from Gemex that is not formatted properly: Refer to http://jira.smilecoms.com/browse/HBT-3201 for details
                /*
                 <Result xmlns="http://schemas.datacontract.org/2004/07/GemexService" xmlns:i="http://www.w3.org/2001/XMLSchema-instance">
                 <ListOfOrder>
                 <Order>
                 <Amount>250000.00</Amount>
                 <Description/>
                 <OrderID>314921</OrderID>
                 <PayMethod/>
                 <Product>20GB Data Bundle</Product>
                 <ResponseTime/>
                 <Status>Pending</Status>
                 <UniqueTransactionId/>
                 </Order>
                 <Order>
                 <Amount>250000.00</Amount>
                 <Description>Payment Transaction Successfull.</Description>
                 <OrderID>314921</OrderID>
                 <PayMethod>Mobile Money</PayMethod>
                 <Product>20GB Data Bundle</Product>
                 <ResponseTime>16/10/2015 07:41:02</ResponseTime>
                 <Status>Successful</Status>
                 <UniqueTransactionId>SUG65077538</UniqueTransactionId>
                 </Order>
                 </ListOfOrder>
                 <ResponseResult>2</ResponseResult>
                 </Result>
                 */
                log.debug("Creating a DOM XML Parser");
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();

                log.debug("Created document builder object succesfully. Will use it to parse XML string and create a document object.");
                Document xmlDocument = builder.parse(new ByteArrayInputStream(transactionDetails.getBytes()));

                log.debug("Document object was created succesful. Creating an XPath object using XPathFactory");
                XPath xPath = XPathFactory.newInstance().newXPath();

                log.debug("XPath object created and ready to be used.");
                String expression = "/Result/child::ListOfOrder/child::Order[(Status='Successful' or Status='successful') and OrderID='" + dbSale.getSaleId() + "']";
                Object result = xPath.evaluate(expression, xmlDocument, XPathConstants.NODESET);
                boolean foundSuccesfulTransaction = false;
                log.debug("Finished evaluating expression using the XPath object.");

                if (result instanceof NodeList) {
                    if (log.isDebugEnabled()) {
                        log.debug("Found node using XPath expresion: /Result/child::ListOfOrder/child::Order[(Status='Successful' or Status='successful') and OrderID='" + dbSale.getSaleId() + "']");
                    }
                    NodeList nodeList = (NodeList) result;
                    int length = nodeList.getLength();
                    for (int i = 0; i < length; i++) {
                        Node node = nodeList.item(i);
                        short nodeType = node.getNodeType();
                        if (Node.ELEMENT_NODE == nodeType) {
                            transactionDetails = parseElementToString((Element) node);
                            foundSuccesfulTransaction = true;
                        }
                    }
                }

                if (!foundSuccesfulTransaction) {
                    expression = "/Result/child::ListOfOrder/child::Order[last()]";
                    result = xPath.evaluate(expression, xmlDocument, XPathConstants.NODESET);
                    if (result instanceof NodeList) {
                        log.debug("Found node using XPath expresion: /Result/child::ListOfOrder/child::Order[last()]");
                        NodeList nodeList = (NodeList) result;
                        int length = nodeList.getLength();
                        for (int i = 0; i < length; i++) {
                            Node node = nodeList.item(i);
                            short nodeType = node.getNodeType();
                            if (Node.ELEMENT_NODE == nodeType) {
                                transactionDetails = parseElementToString((Element) node);
                            }
                        }
                    }
                }
                log.debug("Finished processing response using xPath expression, results: {}", transactionDetails);
            }

            StringBuilder avpBuilder = new StringBuilder();
            String attribute;

            String status = Utils.getBetween(transactionDetails, "<Status>", "</Status>");
            if (status == null) {
                log.debug("Gemex did not return a status in their response. Assuming its pending");
                status = "Pending";
            }
            String uniqueTransactionId = Utils.getBetween(transactionDetails, "<UniqueTransactionId>", "</UniqueTransactionId>");
            String responseTime = Utils.getBetween(transactionDetails, "<ResponseTime>", "</ResponseTime>");
            String amount = Utils.getBetween(transactionDetails, "<Amount>", "</Amount>");
            String paymentGatewayName = Utils.getBetween(transactionDetails, "<PayMethod>", "</PayMethod>");
            String orderId = Utils.getBetween(transactionDetails, "<OrderID>", "</OrderID>");
            String responseDescription = Utils.getBetween(transactionDetails, "<Description>", "</Description>");

            attribute = "orderId" + "=";
            avpBuilder.append(attribute);
            avpBuilder.append(orderId);
            avpBuilder.append("\r\n");

            attribute = "amount" + "=";
            avpBuilder.append(attribute);
            avpBuilder.append(amount);
            avpBuilder.append("\r\n");

            attribute = "dateTime" + "=";
            avpBuilder.append(attribute);
            avpBuilder.append(responseTime);
            avpBuilder.append("\r\n");

            attribute = "status" + "=";
            avpBuilder.append(attribute);
            avpBuilder.append(status);
            avpBuilder.append("\r\n");

            //Helps to keep track of the transition of statuses during the life cycle of this transaction
            String gatewayResponse = dbSale.getPaymentGatewayResponse() == null ? "" : dbSale.getPaymentGatewayResponse();
            if (Utils.getValueFromCRDelimitedAVPString(gatewayResponse, "statusHistory") != null) {
                String statusHistory = Utils.getValueFromCRDelimitedAVPString(gatewayResponse, "statusHistory");
                if (!statusHistory.contains(status)) {//Store distinct statuses
                    attribute = "statusHistory" + "=";
                    avpBuilder.append(attribute);
                    avpBuilder.append(statusHistory).append(",").append(status);
                    avpBuilder.append("\r\n");
                }
            } else {
                attribute = "statusHistory" + "=";
                avpBuilder.append(attribute);
                avpBuilder.append(status);
                avpBuilder.append("\r\n");
            }

            attribute = "transactionRef" + "=";
            avpBuilder.append(attribute);
            avpBuilder.append(uniqueTransactionId);
            avpBuilder.append("\r\n");

            attribute = "paymentGate" + "=";
            avpBuilder.append(attribute);
            avpBuilder.append(paymentGatewayName);
            res.setInfo(paymentGatewayName == null ? "" : paymentGatewayName.replaceAll("\\s", ""));
            avpBuilder.append("\r\n");

            attribute = "responseDescription" + "=";
            avpBuilder.append(attribute);
            avpBuilder.append(responseDescription);
            avpBuilder.append("\r\n");

            attribute = "fullResponse" + "=";
            avpBuilder.append(attribute);
            avpBuilder.append(originalResponse);

            res.setPaymentGatewayTransactionId(uniqueTransactionId);
            res.setTransferredAmountCents(Double.parseDouble(amount) * 100);
            res.setGatewayResponse(avpBuilder.toString());

            //Gemex possible statuses: Successful, Failed, Pending, Cancelled
            if (status.equalsIgnoreCase("Successful")) {
                log.debug("Gemex transaction completed successfully: {}", status);
                res.setSuccess(true);
                res.setTryAgainLater(false);
            } else if (status.equalsIgnoreCase("Pending")) {
                log.debug("Gemex transaction is in progress, will be retried: {}", status);
                res.setSuccess(false);
                res.setTryAgainLater(true);
            } else {
                log.warn("Gemex transaction failed completely, cannot be retried ever again: {}", status);
                res.setSuccess(false);
                res.setTryAgainLater(false);
            }

        } catch (Exception ex) {
            String errorString = ex.toString();
            log.warn("GemexPaymentGateway: For some reason we got an error", ex);
            res.setSuccess(false);
            res.setTryAgainLater(true);
            String avp = "fullResponse" + "=" + transactionDetails;
            res.setGatewayResponse(avp);
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "GemexPaymentGateway failed to process[" + transactionDetails + "] reason: " + errorString);
            return res;
        }

        log.debug("Exiting getPaymentGatewayResult for GemexPaymentGateway");
        return res;
    }

    private GemexRestClientWrapper getSharedRestClient() throws Exception {
        //https://jersey.java.net/nonav/apidocs/1.9/jersey/com/sun/jersey/api/client/Client.html
        //NOTE:
        //Because clients instances are expensive resources, if you are creating multiple Web resources, it is recommended that you re-use a single client instance whenever possible
        //Methods to create instances of WebResource are thread-safe. Methods that modify configuration and or filters are not guaranteed to be thread-safe.
        //The creation of a Client instance is an expensive operation and the instance may make use of and retain many resources.
        //It is therefore recommended that a Client instance is reused for the creation of WebResource instances that require the same configuration settings.

        log.debug("In getGemexSharedRestClient");
        if (System.currentTimeMillis() > (connectionLastReaped + 600000)) {
            closeRestConnection();
        }
        if (sharedClient == null) {
            sharedClient = createRestClient();
        } else {
            log.debug("Reusing existing Gemex shared rest client");
        }
        log.debug("Finished getGemexSharedRestClient");
        return sharedClient;
    }

    private void closeRestConnection() {
        log.debug("Reaping shared Gemex rest client");
        sharedClient = null;
    }

    private GemexRestClientWrapper createRestClient() throws Exception {
        log.debug("Creating GemexRestClientWrapper shared client");
        connectionLastReaped = System.currentTimeMillis();
        String gemexURL = BaseUtils.getSubProperty("env.pgw.gemex.config", "GemexURL");
        String gemexPassword = BaseUtils.getSubProperty("env.pgw.gemex.config", "GemexPassword");
        String gemexUsername = BaseUtils.getSubProperty("env.pgw.gemex.config", "GemexUsername");
        int gemexConnectTimeOutMillis = BaseUtils.getIntSubProperty("env.pgw.gemex.config", "GemexConnectTimeOutMillis");
        int gemexRequestTimeOutMillis = BaseUtils.getIntSubProperty("env.pgw.gemex.config", "GemexRequestTimeOutMillis");
        GemexRestClientWrapper client = new GemexRestClientWrapper(gemexURL, gemexUsername, gemexPassword, gemexConnectTimeOutMillis, gemexRequestTimeOutMillis);
        log.debug("Finished creating GemexRestClientWrapper shared client");
        return client;
    }

    private static String callbackURL;
    private static String paymentGatewayPostURL;

    @Override
    public void init(EntityManager em) {
        callbackURL = BaseUtils.getSubProperty("env.pgw.gemex.config", "CallbackURL");
        paymentGatewayPostURL = BaseUtils.getSubProperty("env.pgw.gemex.config", "PaymentGatewayPostURL");
    }

    @Override
    public String getGatewayURL(Sale dbSale) {
        return paymentGatewayPostURL;
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
        sb.append("Smile Shop");
        sb.append(",callback=");
        sb.append(callbackURL.replaceAll("OrderId#", "OrderId#" + dbSale.getSaleId()));//e.g. https://154.73.71.2/scp/PaymentGateway.action?processBankTransaction#&saleId#1234
        sb.append(",name=<customer_fullname>");
        return sb.toString();
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
    public long getAccountId() {
        return Long.valueOf(BaseUtils.getSubProperty("env.pgw.gemex.config", "AccountId"));
    }

    private String parseElementToString(Element elementNode) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(elementNode), new StreamResult(output));
        return new String(output.toByteArray());
    }

    @Override
    public void processPaymentNotification(int saleId, double cashReciepted, String transactionId, String paymentGatewayExtraData) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addPaymentGatewaySpecificsOnTicket(NewTTIssue tt, String errorMessage) {
        JiraField jiraF = new JiraField();

        jiraF.setFieldName("TT_FIXED_FIELD_Description");
        jiraF.setFieldType("TT_FIXED_FIELD");
        StringBuilder description = new StringBuilder("{panel:title=(!) ");
        description.append("Warning: Gemex interface is is reporting instability");
        description.append("|borderStyle=dashed|titleBGColor=red}");
        description.append("Gemex interface to query transaction statuses is is reporting instability\n")
                .append("Customers using MySmile to recharge would be affected if issue persist\nCaused by: \n").append(errorMessage).append("{panel}");
        jiraF.setFieldValue(description.toString());
        tt.getMindMapFields().getJiraField().add(jiraF);

        jiraF = new JiraField();
        jiraF.setFieldName("TT_FIXED_FIELD_Summary");
        jiraF.setFieldType("TT_FIXED_FIELD");
        String summary = "Gemex API interface instability";
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
    public boolean isUp() {
        boolean isup = GemexPaymentGateway.ISUP_FAIL_COUNTER <= BaseUtils.getIntProperty("env.pos.paymentgateway.isup.fail.counter", 5);
        BaseUtils.sendStatistic("Gemex", "PaymentGateway", "isup", isup ? 1 : 0, "POS");
        updateGatewayStatus("env.scp.gemex.partner.integration.config", isup, getName());
        return isup;
    }

    @Override
    public String getName() {
        return "Gemex";
    }

}
