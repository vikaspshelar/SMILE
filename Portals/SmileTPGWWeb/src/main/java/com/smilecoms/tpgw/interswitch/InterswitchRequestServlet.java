/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.tpgw.interswitch;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.BalanceTransferData;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.Done;
import com.smilecoms.commons.sca.PurchaseUnitCreditRequest;
import com.smilecoms.commons.sca.SCAContext;
import com.smilecoms.commons.sca.StDone;
import com.smilecoms.commons.util.Codec;
import com.smilecoms.commons.util.ExceptionManager;
import com.smilecoms.commons.util.FriendlyException;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.tpgw.api.TPGWApi;
import com.smilecoms.tpgw.api.TPGWApiError;
import com.smilecoms.tpgw.api.ValidateReferenceIdResult;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.slf4j.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.lang.StringEscapeUtils;
import org.xml.sax.InputSource;

/**
 *
 * @author mukosi
 */
public class InterswitchRequestServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(InterswitchRequestServlet.class);

    @Override
    public void init() throws ServletException {

        /*mDocFactory = DocumentBuilderFactory.newInstance();

         try {
         mDocBuilder = mDocFactory.newDocumentBuilder();
         factory = XPathFactory.newInstance();
         xPath = factory.newXPath();
         } catch (Exception ex) {
         throw new ServletException(ex);
         } */
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        log.debug("In doPost on InterswitchXMLRequestHandler servlet.");
        String responseDocumentTypeName = "Unknown";
        boolean writtenToStream = false;
        try {
            // Allocate a new xPath and mDocBuilder for each request to avoid threading (concurrency) issues
            XPath xPath = XPathFactory.newInstance().newXPath();
            DocumentBuilder mDocBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            String callersIP = Utils.getRemoteIPAddress(request);
            log.debug("Caller's IP address is [{}]", callersIP);

            // Check to see if content type is 'text\xml'
            if (request.getContentType() != null
                    && request.getContentType().equals("text/xml")) {

                Document document;
                // Create an InputStream to read in the XML data to be parsed
                InputStream is = request.getInputStream();

                String encoding = BaseUtils.getProperty("env.tpgw.interswitch.character.encoding", "ISO-8859-1");
                Reader reader = new InputStreamReader(is, encoding);//If you have a string, in memory, or in a file, you have to know what encoding it is in else you cannot interpret it or display it to users correctly.
                InputSource insrc = new InputSource(reader);
                insrc.setEncoding(encoding);

                document = mDocBuilder.parse(insrc);

                Node mainContentNode = document.getDocumentElement();
                String requestName = mainContentNode.getNodeName(); // getLocalName();

                log.warn(xmlDocumentToString(document));

                log.debug("InterswitchXMLRequestHandler doPost - Received request: " + requestName);

                XPathExpression xpathExp;

                // Extract login details for Interswitch from the message.
                xpathExp = xPath.compile("/" + requestName + "/ServiceUsername/text()");
                String username = (String) xpathExp.evaluate(document, XPathConstants.STRING);

                xpathExp = xPath.compile("/" + requestName + "/ServicePassword/text()");
                String password = (String) xpathExp.evaluate(document, XPathConstants.STRING);
                Integer customerId = null;

                // Authenticate user
                try {
                    try {
                        // - Check if interswitch is already logged in?
                        customerId = TPGWApi.getInstance().validateSession(username, requestName);
                    } catch (Throwable ex) {
                        // - If not logged in, authenticate and cache the customerId
                        TPGWApi.getInstance().authenticateUser(username, username, password);
                        // - Get the cached customerId
                        customerId = TPGWApi.getInstance().validateSession(username, requestName);
                    }
                } catch (Exception ex) {

                    log.error(xmlDocumentToString(document));

                    if (requestName.equals("PaymentNotificationRequest")) {
                        response.getWriter().write(makePaymentNotificationResponse(document, 1));
                    } else if (requestName.equals("CustomerInformationRequest")) {
                        response.getWriter().write(makeCustomerInformationResponse(document, null, "0", 1, 0));
                    }
                    writtenToStream = true;
                    throw new Exception("Authentication for Interswitch user (" + username + ", " + password + ") failed.");
                }
                //END OF AUTHENTICATION LOGIC

                if (requestName.equals("PaymentNotificationRequest")) {
                    responseDocumentTypeName = "PaymentNotificationResponse";
                    // Do authorisation for the received PaymentNotificationRequest here
                    com.smilecoms.commons.sca.Done isDone;
                    try {

                        TPGWApi.getInstance().authoriseUser(customerId, document, null);

                        String custReferenceNode = "/PaymentNotificationRequest/Payments/Payment/CustReference/text()";//CustReference is mandatory according Interswitch API Spec
                        xpathExp = xPath.compile(custReferenceNode);
                        String custReference = (String) xpathExp.evaluate(document, XPathConstants.STRING);

                        if (custReference == null) {
                            throw new Exception("Element [" + custReferenceNode + "] not specified, CustReference is mandotory according Interswitch API Spec v4.1.");
                        }

                        String itemCodeNode = "/PaymentNotificationRequest/Payments/Payment/PaymentItems/PaymentItem/ItemCode/text()";
                        xpathExp = xPath.compile(itemCodeNode);
                        String itemCode = (String) xpathExp.evaluate(document, XPathConstants.STRING);

                        String itemNameNode = "/PaymentNotificationRequest/Payments/Payment/PaymentItems/PaymentItem/ItemName/text()";
                        xpathExp = xPath.compile(itemNameNode);
                        String itemName = (String) xpathExp.evaluate(document, XPathConstants.STRING);

                        if ((itemCode != null && !itemCode.isEmpty() && itemCode.equalsIgnoreCase("SALE")) || (itemName != null && !itemName.isEmpty() && itemName.equalsIgnoreCase("SALE"))) {
                            isDone = processTransaction(xPath, document, custReference);
                        } else {
                            isDone = processRecharge(xPath, document, username);
                        }

                        if (isDone.getDone() == com.smilecoms.commons.sca.StDone.TRUE) {
                            response.getWriter().write(makePaymentNotificationResponse(document, 0));
                        } else {
                            response.getWriter().write(makePaymentNotificationResponse(document, 1));
                        }
                    } catch (Exception ex) {
                        log.error(xmlDocumentToString(document));
                        response.getWriter().write(makePaymentNotificationResponse(document, 1));
                        throw ex;
                    }

                } else if (requestName.equals("CustomerInformationRequest")) {
                    responseDocumentTypeName = "CustomerInformationResponse";
                    // Do authorisation for the received CustomerInformationRequest here
                    try {
                        TPGWApi.getInstance().authoriseUser(customerId, document, null);
                    } catch (Exception ex) {
                        log.error(xmlDocumentToString(document));
                        response.getWriter().write(makeCustomerInformationResponse(document, null, "-1", 1, 0));
                        throw ex;
                    }

                    // CustomerInformationRequest
                    // Extract reference ...
                    xpathExp = xPath.compile("/CustomerInformationRequest/CustReference/text()");
                    String custReference = (String) xpathExp.evaluate(document, XPathConstants.STRING);

                    //Transaction type
                    xpathExp = xPath.compile("/CustomerInformationRequest/PaymentItemCode/text()");
                    String paymentItemCode = (String) xpathExp.evaluate(document, XPathConstants.STRING);

                    doCustReferenceValidation(custReference, response, document, paymentItemCode);
                }
            } else {

                BufferedReader br = null;
                StringBuilder sb = new StringBuilder();
                String content;
                try (InputStreamReader isr = new InputStreamReader(request.getInputStream())) {
                    br = new BufferedReader(isr);
                    while ((content = br.readLine()) != null) {
                        sb.append(content);
                    }
                } finally {
                    if (br != null) {
                        br.close();
                    }
                }
                log.error("Content received:[{}]", sb.toString());
                throw new Exception("Request content type is invalid, 'text/xml' is expected, got content type:" + request.getContentType());
            }
        } catch (Exception e) {

            // If the exception is a TPGWApiError, do not report it again, because it has already been logged by the TPGWApi class, otherwise report exception.
            if (e instanceof TPGWApiError) {
                log.error("InterswitchRequestServlet Error: [{}]", ((TPGWApiError) e).getErrorDesc());
            } else {
                String req;
                try {
                    req = Codec.binToHexString(Utils.parseStreamToByteArray(request.getInputStream()));
                } catch (Exception e1) {
                    req = e1.toString();
                }
                log.error("InterswitchRequestServlet Error: [{}] for request hex [{}]", e.getMessage(), req);
                ExceptionManager exm = new ExceptionManager(this.getClass().getName());
                FriendlyException fe = exm.getFriendlyException(e, e.getMessage());
                exm.reportError(fe);
            }
            if (!writtenToStream) {
                if (responseDocumentTypeName.equalsIgnoreCase("Unknown")) {
                    response.getWriter().write(makeErrorResponse(responseDocumentTypeName));
                }
            }
        } finally {

            response.setContentType("text/xml");
            response.setCharacterEncoding("UTF-8");
            log.debug("InterswitchXMLRequestHandler doPost - Sending response");
            response.flushBuffer();
            log.debug("Removing threads context");
            TPGWApi.getInstance().removeThreadsRequestContext();
        }
    }

    private BalanceTransferData makeBalanceTransferData(Document document) throws Exception {

        BalanceTransferData balanceTransferData = new BalanceTransferData();
        XPathExpression xpathExp;
        XPath xPath = XPathFactory.newInstance().newXPath();
        xpathExp = xPath.compile("/PaymentNotificationRequest/Payments/Payment/ReceiptNo/text()");
        String uniqueTxId = (String) xpathExp.evaluate(document, XPathConstants.STRING);

        //This is a unique identifier for a transaction, it should never be empty
        if (uniqueTxId == null || uniqueTxId.isEmpty()) {
            String errorMessage = "Element [ReceiptNo] is empty, ReceiptNo value is mandotory according Interswitch API Spec v4.1.";
            String bodyPart = ",<br/><br/><strong>Interswitch:</strong> Missing receipt no. on payment notification, customers will not get their airtime. Contact Interswitch"
                    + "<br/><strong>Caused by:</strong><br/> " + errorMessage
                    + "<br/>";
            String subject = "Interswitch: Missing receipt no on payment notification";

            sendEmailNotification(subject, bodyPart);

            throw new Exception("Element ReceiptNo is empty, ReceiptNo value is mandotory according Interswitch API Spec v4.1.");
        }
        // - Set amount in cents ...
        xpathExp = xPath.compile("/PaymentNotificationRequest/Payments/Payment/Amount/text()");
        Double amountInCents = ((Double) xpathExp.evaluate(document, XPathConstants.NUMBER)) * 100; // Converting to cents since Interswitch sends amount in major currency

        xpathExp = xPath.compile("/PaymentNotificationRequest/Payments/Payment/CustReference/text()");
        Double targetAccountId = (Double) xpathExp.evaluate(document, XPathConstants.NUMBER);

        xpathExp = xPath.compile("/PaymentNotificationRequest/ServiceUsername/text()");
        String username = (String) xpathExp.evaluate(document, XPathConstants.STRING);

        balanceTransferData.setSCAContext(new SCAContext());
        balanceTransferData.setAmountInCents(amountInCents);
        balanceTransferData.setTargetAccountId(targetAccountId.longValue());
        balanceTransferData.setSourceAccountId(TPGWApi.getInstance().getThirdPartyAccountMapping(username));
        balanceTransferData.getSCAContext().setTxId("UNIQUE-" + uniqueTxId + "-" + balanceTransferData.getSourceAccountId());

        return balanceTransferData;
    }

    private PurchaseUnitCreditRequest makePurchaseUnitCreditRequestData(Document document) throws Exception {

        PurchaseUnitCreditRequest request = new PurchaseUnitCreditRequest();

        XPathExpression xpathExp;
        XPath xPath = XPathFactory.newInstance().newXPath();
        xpathExp = xPath.compile("/PaymentNotificationRequest/Payments/Payment/ReceiptNo/text()");
        String receiptNo = (String) xpathExp.evaluate(document, XPathConstants.STRING);

        //This is a unique identifier for a transaction, it should never be empty
        if (receiptNo == null || receiptNo.isEmpty()) {
            String errorMessage = "Element [ReceiptNo] is empty, ReceiptNo value is mandotory according Interswitch API Spec v4.1.";
            String bodyPart = ",<br/><br/><strong>Interswitch:</strong> Missing receipt no. on payment notification, customers will not get their data bundle. Contact Interswitch"
                    + "<br/><strong>Caused by:</strong><br/> " + errorMessage
                    + "<br/>";
            String subject = "Interswitch: Missing receipt no on payment notification";

            sendEmailNotification(subject, bodyPart);

            throw new Exception("Element ReceiptNo is empty, ReceiptNo value is mandotory according Interswitch API Spec v4.1.");
        }
        // - Set amount in sents ...
        xpathExp = xPath.compile("/PaymentNotificationRequest/Payments/Payment/PaymentItems/PaymentItem/ItemQuantity/text()");

        String quantity = (String) xpathExp.evaluate(document, XPathConstants.STRING); // Converting to cents since Interswitch sends amount in major currency
        int iQuantity;
        if (quantity.isEmpty()) { // If interswitch did not set the quantity field, then set it to 1 - Quicktellet does
            // not set the ItemQuantiy field according to Adaobi@Interswitch.
            iQuantity = 1;
        } else {
            iQuantity = Integer.parseInt(quantity);
        }
        // Set the tendered amount
        xpathExp = xPath.compile("/PaymentNotificationRequest/Payments/Payment/PaymentItems/PaymentItem/ItemAmount/text()");
        Double tenderedAmount = ((Double) xpathExp.evaluate(document, XPathConstants.NUMBER)) * 100;// Converting to cents since Interswitch sends amount in major currency

        // int quantity = ((Double) xpathExp.evaluate(document, XPathConstants.NUMBER)).intValue(); // Converting to cents since Interswitch sends amount in major currency
        xpathExp = xPath.compile("/PaymentNotificationRequest/Payments/Payment/CustReference/text()");
        Double targetAccountId = (Double) xpathExp.evaluate(document, XPathConstants.NUMBER);

        // Get UnitCreditSpecification
        xpathExp = xPath.compile("/PaymentNotificationRequest/Payments/Payment/PaymentItems/PaymentItem/ItemCode/text()");
        String itemCode = (String) xpathExp.evaluate(document, XPathConstants.STRING);

        xpathExp = xPath.compile("/PaymentNotificationRequest/ServiceUsername/text()");
        String username = (String) xpathExp.evaluate(document, XPathConstants.STRING);

        // Strip the first "BUN" characters
        int bundleTypeCode = Integer.valueOf(itemCode.substring("BUN".length()));

        request.setSCAContext(new SCAContext());
        request.setUnitCreditSpecificationId(bundleTypeCode);
        request.setAccountId(targetAccountId.longValue());
        request.setNumberToPurchase(iQuantity);
        request.getSCAContext().setTxId(receiptNo);
        request.setUniqueId(receiptNo + "-" + TPGWApi.getInstance().getThirdPartyAccountMapping(username));
        return request;
    }

    private double getTenderedAmount(Document document) throws Exception {

        XPathExpression xpathExp;
        XPath xPath = XPathFactory.newInstance().newXPath();
        // Set the tendered amount
        xpathExp = xPath.compile("/PaymentNotificationRequest/Payments/Payment/PaymentItems/PaymentItem/ItemAmount/text()");
        Double tenderedAmount = ((Double) xpathExp.evaluate(document, XPathConstants.NUMBER)) * 100;// Converting to cents since Interswitch sends amount in major currency
        return tenderedAmount;
    }

    private String makePaymentNotificationResponse(Document originalRequestDoc, int status) throws Exception {

        // Exctract merchant reference 
        XPath xPath = XPathFactory.newInstance().newXPath();
        XPathExpression xpathExp = xPath.compile("/PaymentNotificationRequest/Payments/Payment/PaymentLogId/text()");

        String paymentLogId = (String) xpathExp.evaluate(originalRequestDoc, XPathConstants.STRING);

        String response = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>"
                + "<PaymentNotificationResponse>"
                + "<Payments>"
                + "<Payment>"
                + "<PaymentLogId>" + paymentLogId + "</PaymentLogId>"
                + "<Status>" + status + "</Status>"
                + "</Payment>"
                + "</Payments>"
                + "</PaymentNotificationResponse>";

        if (status == 1) {
            log.warn("Returning failed response with status [{}] [{}]", status, response);
        } else {
            log.debug("Returning successful response with status [{}] [{}]", status, response);
        }
        return response;
    }

    private String makeCustomerInformationResponse(Document originalRequestDoc, Customer customer, String custReference, int status, double amountDue) throws Exception {

        // Exctract merchant reference 
        XPath xPath = XPathFactory.newInstance().newXPath();
        XPathExpression xpathExp = xPath.compile("/CustomerInformationRequest/MerchantReference/text()");
        String merchantReference = (String) xpathExp.evaluate(originalRequestDoc, XPathConstants.STRING);

        String response = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>"
                + "<CustomerInformationResponse>"
                + "<MerchantReference>" + merchantReference + "</MerchantReference>"
                + "<Customers>"
                + "<Customer>"
                + "<Status>" + status + "</Status>"
                + "<CustReference>" + custReference + "</CustReference>";

        if (customer != null) {
            response += "<FirstName>" + StringEscapeUtils.escapeXml(customer.getFirstName()) + "</FirstName>"
                    + "<LastName>" + StringEscapeUtils.escapeXml(customer.getLastName()) + "</LastName>"
                    + "<OtherName>" + StringEscapeUtils.escapeXml(customer.getMiddleName()) + "</OtherName>"
                    + "<Email>" + StringEscapeUtils.escapeXml(customer.getEmailAddress()) + "</Email>"
                    + "<Phone>" + StringEscapeUtils.escapeXml(customer.getAlternativeContact1()) + "</Phone>";

            if (amountDue > 0) {
                DecimalFormat df = new DecimalFormat("#.00");
                String amt = df.format(Utils.round((amountDue / 100), 0));
                response += "<Amount>" + amt + "</Amount>";
                response += "<PaymentItems> <Item>";
                response += "<Price>" + amt + "</Price>";
                String productName = "SALE";
                if (custReference.toLowerCase().startsWith("ci")) {
                    //productName = "CASH_IN"; //DONT USE THIS AS PER ISW request
                }
                response += "<ProductName>" + productName + "</ProductName>";
                response += "<ProductCode>" + custReference + "</ProductCode>";
                response += "<Tax>0</Tax>";
                response += "<Quantity>1</Quantity>";
                //response += "<Session></Session>";
                response += "<SubTotal>" + amt + "</SubTotal>";
                response += "<Total>" + amt + "</Total>";
                response += "</Item> </PaymentItems>";
            }
        }

        response += "    </Customer>"
                + "  </Customers>"
                + "</CustomerInformationResponse>";

        return response;
    }

    private String xmlDocumentToString(Document doc) throws Exception {
        if (doc == null) {
            return "XML Document is NULL";
        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        //initialize StreamResult with File object to save to file
        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source = new DOMSource(doc);
        transformer.transform(source, result);
        return result.getWriter().toString();
    }

    private String makeErrorResponse(String respondeDocumentTypeName) {
        return "<" + respondeDocumentTypeName + ">"
                + "<Status>1</Status>"
                + "</" + respondeDocumentTypeName + ">";
    }

    private void doCustReferenceValidation(String custReference, HttpServletResponse response, Document document, String paymentItemCode) throws Exception {
        Customer customer = null;
        double amountDue = 0;
        int status = 0;
        try {

            if (paymentItemCode != null && !paymentItemCode.isEmpty() && paymentItemCode.equalsIgnoreCase("SALE")) {
                ValidateReferenceIdResult vRR = validateSaleReference(custReference);
                if (vRR.getValidationResult().equals("SUCCESS")) {
                    customer = vRR.getCustomer();
                    amountDue = vRR.getAmountInCents();
                } else {
                    status = 1;
                }
            } else {
                customer = validateAccountId(custReference);
                DecimalFormat df = new DecimalFormat("#");
                Double accountId = Double.parseDouble(custReference);
                custReference = df.format(accountId);
            }
            response.getWriter().write(makeCustomerInformationResponse(document, customer, custReference, status, amountDue));

        } catch (Exception ex) {
            log.error(xmlDocumentToString(document));
            response.getWriter().write(makeCustomerInformationResponse(document, null, custReference, 1, 0));
            throw ex;
        }
    }

    private ValidateReferenceIdResult validateSaleReference(String saleReference) throws Exception {
        return TPGWApi.getInstance().validateReferenceID(saleReference);
    }

    private Customer validateAccountId(String accountIdReference) throws Exception {
        long accountId;
        try {
            accountId = Long.parseLong(accountIdReference);
        } catch (Exception e) {
            log.warn("Invalid account id [{}]", accountIdReference);
            accountId = 0;
        }
        return TPGWApi.getInstance().validateAccount(accountId);
    }

    private Done processRecharge(XPath xPath, Document document, String username) throws Exception {
        log.debug("In InterswitchRequestServlet.processRecharge");
        Done isDone = null;
        XPathExpression xpathExp;
        // Extract <ItemCode/> = "BUN101" for bundles, = "SA" for Smile Airtime
        String itemCodeNode = "/PaymentNotificationRequest/Payments/Payment/PaymentItems/PaymentItem/ItemCode/text()";
        xpathExp = xPath.compile(itemCodeNode);
        String itemType = (String) xpathExp.evaluate(document, XPathConstants.STRING);

        if (itemType == null) {
            throw new Exception("Element [" + itemCodeNode + "] not specified, do not know how to process this payment request.");
        }

        String channelName = "/PaymentNotificationRequest/Payments/Payment/ChannelName/text()";
        xpathExp = xPath.compile(channelName);
        String channel = (String) xpathExp.evaluate(document, XPathConstants.STRING);

        if (itemType.startsWith("BUN")) { // It's a bundle Purchase
            PurchaseUnitCreditRequest ucr = makePurchaseUnitCreditRequestData(document);
            double tenderedAmount = getTenderedAmount(document);
            isDone = TPGWApi.getInstance().doPurchaseBundle(ucr, null, username, tenderedAmount, channel);

        } else if (itemType.equalsIgnoreCase("SA")) { // Smile Airtime Purchase
            // It's a normal AIRTIME purchase
            // Authorisation successfull - Do PaymentNotification
            String receiptNo = "/PaymentNotificationRequest/Payments/Payment/ReceiptNo/text()";
            xpathExp = xPath.compile(receiptNo);
            String receipt = (String) xpathExp.evaluate(document, XPathConstants.STRING);
            isDone = TPGWApi.getInstance().doBalanceTransfer(makeBalanceTransferData(document), channel, receipt);
            // Balance transfer was successfull
        } else { // Unknown Payment Type
            throw new Exception("Unknown payment type [ItemCode=" + itemType + "] specified, do not know how to process this request.");
        }
        return isDone;
    }

    private Done processTransaction(XPath xPath, Document document, String custReference) throws Exception {
        log.debug("In InterswitchRequestServlet.processTransaction, processing transaction with reference: {}", custReference);
        Done isDone;
        XPathExpression xpathExp;

        String isReversalNode = "/PaymentNotificationRequest/Payments/Payment/IsReversal/text()";
        xpathExp = xPath.compile(isReversalNode);
        String val = (String) xpathExp.evaluate(document, XPathConstants.STRING);
        boolean isReversal = Boolean.parseBoolean(val);
        String paymentNotificationXML = xmlDocumentToString(document);

        if (isReversal) {
            log.debug("Request for a reversal initiated: [{}]", val);
            TPGWApi.getInstance().createEvent("REVERSAL", custReference, paymentNotificationXML);
            isDone = new Done();
            isDone.setDone(StDone.TRUE);//Must return true at all times.
            return isDone;
        }

        String receiptNode = "/PaymentNotificationRequest/Payments/Payment/ReceiptNo/text()";
        xpathExp = xPath.compile(receiptNode);
        String receipt = (String) xpathExp.evaluate(document, XPathConstants.STRING);

        //This is a unique identifier for a transaction, it should never be empty
        if (receipt == null || receipt.isEmpty()) {
            String errorMessage = "Element [ReceiptNo] is empty, ReceiptNo value is mandotory according Interswitch API Spec v4.1.";
            String bodyPart = ",<br/><br/><strong>Interswitch:</strong> Missing receipt no. on payment notification, customers will not get value/cash-in is going to fail. Contact Interswitch"
                    + "<br/><strong>Caused by:</strong><br/> " + errorMessage
                    + "<br/>";
            String subject = "Interswitch: Missing ReceiptNo on payment notification";

            sendEmailNotification(subject, bodyPart);

            throw new Exception("Element ReceiptNo is empty, ReceiptNo value is mandotory according to Interswitch API Spec v4.1.");
        }

        String amountNode = "/PaymentNotificationRequest/Payments/Payment/Amount/text()";
        xpathExp = xPath.compile(amountNode);
        double cashReceipted = (Double) xpathExp.evaluate(document, XPathConstants.NUMBER);
        double amt = Utils.round((cashReceipted * 100), 0);

        String banknameNode = "/PaymentNotificationRequest/Payments/Payment/BankName/text()";
        xpathExp = xPath.compile(banknameNode);
        String bankname = (String) xpathExp.evaluate(document, XPathConstants.STRING);
        String bank = bankname == null ? "Interswitch" : bankname;
        isDone = TPGWApi.getInstance().processTransaction(custReference, amt, receipt, bank, "Interswitch");

        TPGWApi.getInstance().createEvent("processTransaction", custReference, paymentNotificationXML);//Save XML on event data to help in trouble-shooting

        return isDone;
    }

    private void sendEmailNotification(String subject, String message) {
        if (!BaseUtils.getBooleanProperty("env.tpgw.paymentgateway.send.email.notification", false)) {
            return;
        }
        Set<String> configWatchers = BaseUtils.getPropertyAsSet("env.tpgw.paymentgateway.email.watchers");
        String from = BaseUtils.getProperty("env.tpgw.paymentgateway.email.notification.from", "admin@smilecoms.com");
        for (String to : configWatchers) {
            String fullBody = "Hi " + to + message;
            TPGWApi.getInstance().sendEmail(from, to, subject, fullBody);
        }
    }

}
