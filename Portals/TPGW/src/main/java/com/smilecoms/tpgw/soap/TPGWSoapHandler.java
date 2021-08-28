/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.tpgw.soap;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;
import com.smilecoms.commons.base.sd.SOAPService;
import com.smilecoms.commons.base.sd.ServiceDiscoveryAgent;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.tpgw.api.TPGWApi;
import java.io.StringWriter;
import java.util.Collections;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.MessageContext;
import java.util.Set;
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
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 *
 * @author paul
 */
public class TPGWSoapHandler implements SOAPHandler {

    private static final Logger log = LoggerFactory.getLogger(TPGWSoapHandler.class.getName());
    private static final String THIRD_PARTY_IP_ADDRESS = "ThirdPartyIP";
    private static final String namespaceURI = "http://xml.smilecoms.com/schema/TPGW";
    private static final String PD = "<soapenv:Envelope xsi:schemaLocation=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:x=\"http://xml.smilecoms.com/schema/TPGW\"><soapenv:Body><x:IsUpRequest>Hello</x:IsUpRequest></soapenv:Body></soapenv:Envelope>";
    
    static {
        Async.scheduleAtFixedRate(new SmileBaseRunnable("TPGW.SoapHandler") {
            @Override
            public void run() {
                new TPGWSoapHandler().trigger();
            }
        }, 5000, 60000);
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
            TPGWNamespaceContext tpgwNamespaceContext = new TPGWNamespaceContext();
            XPathFactory factory = XPathFactory.newInstance();
            XPath xPath = factory.newXPath();
            xPath.setNamespaceContext(tpgwNamespaceContext);

            SOAPMessageContext soapContext = (SOAPMessageContext) context;
            SOAPMessage message = soapContext.getMessage();
            soapBody = message.getSOAPBody();
            document = soapBody.extractContentAsDocument();
            Node mainContentNode = document.getDocumentElement();
            boolean inbound = !(Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

            if (log.isInfoEnabled()) {
                String doc = xmlDocumentToString(document);
                // Ignore isup requests and responses
                if (!doc.contains("IsUpRequest") && !doc.contains("<Done xmlns=\"http://xml.smilecoms.com/schema/TPGW\">")) {
                    log.info("TPGW: {} [{}]", inbound ? "Inbound request" : "Outbound response", doc);
                }
            }

            if (inbound) {

                javax.servlet.http.HttpServletRequest servletRequest = (javax.servlet.http.HttpServletRequest) context.get(MessageContext.SERVLET_REQUEST);
                String callersIP = Utils.getRemoteIPAddress(servletRequest);

                log.debug("Caller's IP address is [{}]", callersIP);

                // - Add IP address to context values ...
                context.put(THIRD_PARTY_IP_ADDRESS, callersIP);

                String authenticateName = namespaceURI + ":Authenticate";
                String isUpName = namespaceURI + ":IsUpRequest";

                /* Document document = (Document) soapContext.getMessage().getSOAPBody().getOwnerDocument();
                Node bodyNode = document.getElementsByTagNameNS("http://schemas.xmlsoap.org/soap/envelope/", "Body").item(0);
                Node mainContentNode = bodyNode.getFirstChild(); 
                document = mainContentNode.getOwnerDocument(); */
                String docType = mainContentNode.getNamespaceURI() + ":" + mainContentNode.getLocalName();

                log.debug("Received document type: [{}]", docType);

                /* 
                 * If the user is Authenticating, do not do Authorisation,
                 * just exit successfully to let the EJB do the Authentication process ...
                 */
                XPathExpression xpathExp;
                if (docType.equals(authenticateName)) {
                    // - Authentication message received
                    // -- Get the username
                    if (log.isDebugEnabled()) {
                        xpathExp = xPath.compile("/tpgw:Authenticate/tpgw:Username/text()");
                        String username = (String) xpathExp.evaluate(document, XPathConstants.STRING);
                        log.debug("Authentication request received for user: [{}]", username);
                    }
                } else if (docType.equals(isUpName)) { // - No authorisation required for isUp request
                    log.debug("isUp request received");
                } else {
                    // - Do the Authorisation code here
                    // 1. Extract the SessionId from the incoming message.
                    xpathExp = xPath.compile("//tpgw:TPGWContext/tpgw:SessionId/text()");
                    String sessionId = (String) xpathExp.evaluate(document, XPathConstants.STRING);
                    log.debug("TPGW Soap Handler received request with SessionId: [{}]", sessionId);

                    if (sessionId.isEmpty()) {
                        throw new ProtocolException("SessionId not specified in the request -- DocType [" + docType + "]");
                    }

                    // 2. Resolve the SessionId to a customer profile id
                    Integer customerId = null;
                    try {
                        // - Check if third party is already logged in? If yes, update the existing session, set a new expiry time, and return customerId ...
                        customerId = TPGWApi.getInstance().validateSession(sessionId, docType);
                    } catch (Throwable ex) {
                        log.error("Invalid or expired session - SessionId [{}]", sessionId);
                        throw new ProtocolException("Invalid or expired session -- SessionId [" + sessionId + "]");
                        // throw new ProtocolException(ex);
                    }

                    TPGWApi.getInstance().authoriseUser(customerId, document, tpgwNamespaceContext);
                }
            } else {
                log.debug("This is a response. Removing threads context");
                TPGWApi.getInstance().removeThreadsRequestContext();
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

    private void trigger() {
        try {
            SOAPService service = new SOAPService("TPGW", "http", getListeningPort(), "/TPGW/ThirdPartyGateway");
            service.setWeight(5);
            service.setClientHostnameRegexMatch(".*");
            service.setTestData(PD);
            service.setTestResponseCriteria("<Done>true</Done>");
            service.setGapBetweenTestsMillis(BaseUtils.getIntPropertyFailFast("env.sd.portals.test.millis",3000));
            service.setTestTimeoutMillis(1000);
            ServiceDiscoveryAgent.getInstance().publishService(service);
        } catch (Exception ex) {
            log.warn("Error publishing website service: ", ex);
        }
    }
    private static int listeningPort = 0;

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

}
