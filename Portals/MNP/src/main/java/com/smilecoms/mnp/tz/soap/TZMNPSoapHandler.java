/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mnp.tz.soap;

import com.smilecoms.commons.sca.AuthenticationQuery;
import com.smilecoms.commons.sca.CallersRequestContext;
import com.smilecoms.commons.sca.CustomerQuery;
import com.smilecoms.commons.sca.Event;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.util.Codec;
import com.smilecoms.commons.util.Utils;
import java.io.StringWriter;
import java.util.Collections;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.MessageContext;
import java.util.Set;
import org.slf4j.*;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.ProtocolException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import st.systor.np.commontypes.AccessFaultType;
import st.systor.np.commontypes.TechnicalFaultType;
import st.systor.np.sp.SpAccessFault;
import st.systor.np.sp.SpTechnicalFault;

/**
 *
 * @author paul
 */
public class TZMNPSoapHandler implements SOAPHandler {

    private static final Logger log = LoggerFactory.getLogger(TZMNPSoapHandler.class.getName());
    private static final String ENCODING = "utf-8";
    private static final String NPCDB_IP_ADDRESS = "NPCDBIP";
    private static final String namespaceURI = "http://np.systor.st/sp";

    @Override
    public Set getHeaders() {
        return Collections.emptySet();
    }

    @Override
    public boolean handleMessage(MessageContext context) {
        return handle(context);
    }

    @Override
    public boolean handleFault(MessageContext context) {
        return true;
    }

    @Override
    public void close(MessageContext context) {
    }

    public boolean handle(MessageContext context) {
        Document document; // Really needed at this level?
        SOAPBody soapBody; // Really needed at this level?
        String   npOrderId = null;
        String   messageId = null;
        String   strXmlDoc = null;
        String   messageType = null;
        String   requestXML = null;
         
        TZMNPNamespaceContext tzmnpNamespaceContext = new TZMNPNamespaceContext();
        XPathFactory factory = XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
        xPath.setNamespaceContext(tzmnpNamespaceContext);
        boolean inbound;
        boolean outbound = false;
        Node mainContentNode;
        
        try {          
            
            XPathExpression xpathExp;
            
            SOAPMessageContext soapContext = (SOAPMessageContext) context;
            
            SOAPMessage message = soapContext.getMessage();
            soapBody = message.getSOAPBody();
            document = soapBody.extractContentAsDocument();
            mainContentNode = document.getDocumentElement();
                        
            log.debug("TZMNPSoapHandler is in handle message");
            inbound = !(Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
            outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
            
            if (inbound) {
                
                xpathExp = xPath.compile("//sp:NPOrderID/text()");
                npOrderId = (String) xpathExp.evaluate(document, XPathConstants.STRING);
                
                xpathExp = xPath.compile("//sp:MessageID/text()");
                messageId = (String) xpathExp.evaluate(document, XPathConstants.STRING);
                
                context.put("NPOrderID", npOrderId); // To be used downstream
                context.put("MessageID", messageId); // To be used downstream
                context.put("MessageType", mainContentNode.getLocalName());
                
                javax.servlet.http.HttpServletRequest servletRequest = (javax.servlet.http.HttpServletRequest) context.get(MessageContext.SERVLET_REQUEST);
                String callersIP = Utils.getRemoteIPAddress(servletRequest);

                log.debug("Caller's IP address is [{}]", callersIP);

                // - Add IP address to context values ...
                context.put(NPCDB_IP_ADDRESS, callersIP);

                String isUpName = namespaceURI + ":IsUpRequest";

                String docType = mainContentNode.getNamespaceURI() + ":" + mainContentNode.getLocalName();
                
                log.debug("TZMNP Received document type: [{}]", docType);
                strXmlDoc = printXMLDocument(document); // Print the received XML message if we are in debug mode
                context.put("RequestXML", strXmlDoc);  
                
                String basicAuth = servletRequest.getHeader("Authorization");
                
                doAuthentication(basicAuth);                
                
            } else {
                //We are on the outbound
                strXmlDoc = printXMLDocument(document); 
            }
            log.debug("Going to process the document further ....");
            soapBody.addDocument(document); // Is this not adding the document twice? 
            log.debug("Document processed ...");
        } catch (Exception stf) {
            log.error("Error while handling message [{}]", stf.getMessage());
            if((stf instanceof SpTechnicalFault) || (stf instanceof SpAccessFault)) {
                ProtocolException pe = new ProtocolException(stf);
                context.put("ProtocolException", pe);
                throw pe;
            } else {
                String message = (stf.getMessage() != null ? stf.getMessage() : stf.toString());
                TechnicalFaultType tft = new TechnicalFaultType();
                tft.setErrorCode("10111");
                tft.setDescription(message);
                
                SpTechnicalFault spTf = new SpTechnicalFault(stf.getMessage(), tft);
                ProtocolException pe = new ProtocolException(spTf);
                context.put("ProtocolException", pe);
                throw pe;
            }
        } finally {
            
            if(outbound) { // Only save the event on inbound leg only ...
                // Save the document as an event.
                try {
                    
                    if(context.containsKey("MessageType")){
                       messageType = (String) context.get("MessageType");
                    }
                    
                    String eventSubType;
                    if(messageType != null) {
                         eventSubType = messageType;
                    } else {
                         eventSubType = "TZMNP";
                    }
                    
                    if(context.containsKey("NPOrderID")){
                        npOrderId = (String) context.get("NPOrderID");
                    }

                    if(context.containsKey("MessageID")){
                        messageId = (String) context.get("MessageID");
                    }
                    
                    if(context.containsKey("RequestXML")){
                        requestXML = (String) context.get("RequestXML");
                    }                    
                    
                    createEvent(eventSubType, npOrderId, messageId, requestXML, strXmlDoc, null);
                    
                } catch (Exception ex) {
                    log.error("Error while trying to save the received event for MNP.", ex);
                }
            } 
        }
        return true;
    }
    
    private boolean doAuthentication(String authData) throws SpAccessFault, Exception {
        // Do BASIC authentication
        AccessFaultType aft = new AccessFaultType();
        if(authData == null || authData.isEmpty()){
            log.error("Authentication credentials not supplied");
            String message = "Login failed";
            aft.setErrorCode("10111");
            aft.setDescription(message);
            throw new SpAccessFault(message, aft);
        } else {
            if (!authData.toUpperCase().startsWith("BASIC ")) {
                log.error("TZMNP only allow BASIC authentication.");
                String message = "Login failed";
                aft.setErrorCode("10111");
                aft.setDescription(message);
                throw new SpAccessFault(message, aft);
            } else {
                String encodedCredentials = authData.substring(6);
                String strCredentials = new String(Utils.decodeBase64(encodedCredentials));
                
                String [] credentials = strCredentials.split(":");
                
                if(credentials.length != 2) {
                    log.error("TZMNP: Incorrect format of the login details {}", strCredentials);
                    String message = "Login failed";
                    aft.setErrorCode("10111");
                    aft.setDescription(message);
                    throw new SpAccessFault(message, aft);
                } else {
                    log.debug("Logging in user [{}] to SCA", credentials[0]);
                    if(authenticateUser(credentials[0], credentials[1])) {
                        log.debug("User [{}] successfully logged in.", credentials[0]);
                        return true;
                    } else {
                        String message = "Login for user ["+ credentials[0] + "] failed.";
                        aft.setErrorCode("10111");
                        aft.setDescription(message);
                        throw new SpAccessFault(message, aft);
                    }
                }
            }
       }
    }
    
    private boolean authenticateUser(String username, String password) throws Exception  {

        AuthenticationQuery authenticationQuery = new AuthenticationQuery();
        com.smilecoms.commons.sca.AuthenticationResult scaAuthResult;

        authenticationQuery.setSSOIdentity(username);
        authenticationQuery.setSSOEncryptedPassword(Codec.stringToEncryptedHexString(password));

        try {
            boolean done;
            scaAuthResult = SCAWrapper.getAdminInstance().authenticate(authenticationQuery);
            done = scaAuthResult.getDone().equals(com.smilecoms.commons.sca.StDone.TRUE);
            if (!done) {
                // Failed authentication
                log.info("Invalid credentials while logging in user in SCA");
                throw new Exception("Invalid login credentials supplied");
            }

            // Get customer Id for the username
            CustomerQuery customerQuery = new CustomerQuery();
            customerQuery.setSSOIdentity(username);
            customerQuery.setResultLimit(1);
            customerQuery.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
            Integer customerId = SCAWrapper.getAdminInstance().getCustomer(customerQuery).getCustomerId();
            CallersRequestContext ctx = new CallersRequestContext(username, "0.0.0.0", customerId, scaAuthResult.getSecurityGroups());
            initialiseSCAForCaller(ctx);
            return true;

        } catch (Exception ex) {
            log.info("Error logging in user in NGMNP: " + ex.toString());
            throw ex; // getTPGWError(ex, ex.getMessage());
        }
    }
    
    private void initialiseSCAForCaller(CallersRequestContext ctx) {
        SCAWrapper.getUserSpecificInstance().setThreadsRequestContext(ctx);
    }
    
    private String printXMLDocument(Document document) throws Exception {
        String strDoc = xmlDocumentToString(document);
        log.debug(strDoc);
        return strDoc;
    }
    
    private String xmlDocumentToString(Document doc) throws Exception {
        if(doc == null)
            return "XML Document is NULL";
        
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        //initialize StreamResult with File object to save to file
        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source = new DOMSource(doc);
        transformer.transform(source, result);
        return result.getWriter().toString();
    }
    
    private void createEvent(String eventSubType, String orderId, String messageId, String requestXML, String responseXML, String faultXML) {
        
        try {
            String eventKey = orderId + "-" + messageId;
            
            String eventData =  ((requestXML == null) ? "" : "REQUEST:\n" + requestXML + "\n\n") + 
                                ((responseXML == null) ? "" : "RESPONSE:\n" + responseXML) + "\n\n" + 
                                ((faultXML == null) ? "" : "FAULT:\n" + faultXML);
            
            Event event = new Event();
            event.setEventSubType(eventSubType);
            event.setEventType("TZMNP");
            event.setEventKey(eventKey);
            event.setEventData(eventData);
            log.debug("Saving TZMNP event with key - {}", eventKey);
            SCAWrapper.getAdminInstance().createEvent(event);
        } catch (Exception ex) {
            log.warn("Failed to create event via SCA call, cause: {}", ex.toString());
        }
    }
}