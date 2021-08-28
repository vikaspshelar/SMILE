/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mnp.soap;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;
import com.smilecoms.commons.base.sd.SOAPService;
import com.smilecoms.commons.base.sd.ServiceDiscoveryAgent;
import com.smilecoms.commons.sca.AuthenticationQuery;
import com.smilecoms.commons.sca.CallersRequestContext;
import com.smilecoms.commons.sca.CustomerQuery;
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
import java.util.concurrent.ScheduledFuture;
import org.slf4j.*;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.ProtocolException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 *
 * @author paul
 */
public class MNPSoapHandler implements SOAPHandler {

    private static final Logger log = LoggerFactory.getLogger(MNPSoapHandler.class.getName());
    private static final String NPCDB_IP_ADDRESS = "NPCDBIP";
    private static final String namespaceURI = "http://ws.inpac.telcordia.com";
    private static int listeningPort = 0;
    private static ScheduledFuture runner1 = null;

    static {
        runner1 = Async.scheduleAtFixedRate(new SmileBaseRunnable("MNP.SoapHandler") {
            @Override
            public void run() {
                new MNPSoapHandler().trigger();
            }
        }, 5000, 60000);
    }

    private void trigger() {
        try {
            SOAPService service = new SOAPService("MNP", "http", getListeningPort(), "");
            service.setWeight(5);
            service.setClientHostnameRegexMatch(".*");
            service.setTestData("");
            service.setTestResponseCriteria("");
            service.setGapBetweenTestsMillis(BaseUtils.getIntPropertyFailFast("env.sd.portals.test.millis", 3000));
            ServiceDiscoveryAgent.getInstance().publishService(service);
        } catch (Exception e) {
            log.warn("Error in trigger:", e);
        }
    }

    public static int getListeningPort() {
        if (listeningPort > 0) {
            return listeningPort;
        }
        listeningPort = 8003;
        if (System.getProperty("HTTP_BIND_PORT") != null) {
            listeningPort = Integer.parseInt(System.getProperty("HTTP_BIND_PORT"));
        }
        log.warn("Websites are listening on port {}", listeningPort);
        return listeningPort;
    }

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
        return false;
    }

    @Override
    public void close(MessageContext context) {
    }

    public boolean handle(MessageContext context) {
        Document document = null; // Really needed at this level?
        SOAPBody soapBody = null; // Really needed at this level?

        try {

            SOAPMessageContext soapContext = (SOAPMessageContext) context;
            SOAPMessage message = soapContext.getMessage();
            soapBody = message.getSOAPBody();
            document = soapBody.extractContentAsDocument();
            Node mainContentNode = document.getDocumentElement();

            printXMLDocument(document); // Print the received XML message if we are in debug mode

            log.debug("NGMNPSoapHandler is in handle message");
            boolean inbound = !(Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
            if (inbound) {

                javax.servlet.http.HttpServletRequest servletRequest = (javax.servlet.http.HttpServletRequest) context.get(MessageContext.SERVLET_REQUEST);
                String callersIP = Utils.getRemoteIPAddress(servletRequest);

                log.debug("Caller's IP address is [{}]", callersIP);

                // - Add IP address to context values ...
                context.put(NPCDB_IP_ADDRESS, callersIP);

                String isUpName = namespaceURI + ":IsUpRequest";

                /*Document document = (Document) soapContext.getMessage().getSOAPBody().getOwnerDocument();
                Node bodyNode = document.getElementsByTagNameNS("http://schemas.xmlsoap.org/soap/envelope/", "Body").item(0);
                Node mainContentNode = bodyNode.getFirstChild(); 
                document = mainContentNode.getOwnerDocument(); */
                String docType = mainContentNode.getNamespaceURI() + ":" + mainContentNode.getLocalName();

                log.debug("Received document type: [{}]", docType);

                // Extract username and password.
                String password = null;
                String username = null;
                Node curNode;
                for (int i = 0; i < mainContentNode.getChildNodes().getLength(); i++) {
                    curNode = mainContentNode.getChildNodes().item(i);
                    log.error(">>>>>>> curNode.getLocalName() = " + curNode.getLocalName());
                    if (curNode.getLocalName() != null) {
                        if (curNode.getLocalName().equals("password")) {
                            password = curNode.getTextContent();
                        } else if (curNode.getLocalName().equals("userID")) {
                            username = curNode.getTextContent();
                        }
                    }
                }

                if (username == null || username.length() <= 0) {
                    throw new Exception("Username not supplied!");
                }

                if (password == null || password.length() <= 0) {
                    throw new Exception("Password not supplied!");
                }

                log.debug("Logging in user [{}] to SCA", username);
                if (authenticateUser(username, new String(Utils.decodeBase64(password)))) {
                    log.debug("User [{}] successfully logged in.", username);
                } else {
                    throw new Exception("Login for user [" + username + "] failed.");
                }
            }
            soapBody.addDocument(document); // Is this not adding the document twice? 
        } catch (Exception e) {
            if ((soapBody != null) && (document != null)) {
                try {
                    soapBody.addDocument(document); // Is this not adding the document twice?
                } catch (SOAPException ex) {
                    log.error("SOAP Error while processing incoming message in the handler: " + ex.toString());
                }
            }
            // - All other unhandled problems. 
            log.error("Error in AuthorisationHandler: [{}]", e.toString());
            log.warn("Error: ", e);
            ProtocolException pe = new ProtocolException(e);
            context.put("ProtocolException", pe); // Pass this exception to the next handler (TPGWSoapFaultHandler) for processing
            throw pe;
        }
        return true;
    }

    private boolean authenticateUser(String username, String password) throws Exception {

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

    private void printXMLDocument(Document document) throws Exception {
        log.debug(xmlDocumentToString(document));
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

}
