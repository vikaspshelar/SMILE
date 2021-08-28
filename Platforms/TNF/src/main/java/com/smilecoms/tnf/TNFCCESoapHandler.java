/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.tnf;

import com.smilecoms.commons.util.Utils;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 *
 * @author sabza
 */
public class TNFCCESoapHandler implements SOAPHandler<SOAPMessageContext> {

    private final String username;
    private final String password;
    private final int tokenExpiry;
    private static final String ENCODING = "utf-8";
    private static final Logger log = LoggerFactory.getLogger(TNFCCESoapHandler.class);
    private static final String KEY_UUID = "tnfcontextkey";
    private static final String ELEMENT_FAULT_CODE = "faultcode";
    private static final String ERROR_TYPE_CLIENT = ":Client";
    private static final String ERROR_TYPE_SERVER = ":Server";
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final Map<String, SOAPMessageContext> tnfSoapMessageContextMap = new ConcurrentHashMap<>(10);
    private final String WSS_SX_URI = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    private final String WSS_UTL_URI = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
    private final String WSS_PWD_TYPE_URI = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText";

    public TNFCCESoapHandler(String username, String password, int tokenExpiry) {
        this.username = username;
        this.password = password;
        this.tokenExpiry = tokenExpiry;
    }

    @Override
    public Set<QName> getHeaders() {
        log.debug("In TNF CCE getHeaders");
        final QName securityHeader = new QName(WSS_SX_URI, "Security", "wsse");
        final HashSet<QName> headers = new HashSet<>();
        headers.add(securityHeader);
        log.debug("Exiting TNF CCE getHeaders");
        return headers;
    }

    @Override
    public void close(MessageContext context) {
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {

        //Header example: 
        /*
         <SOAP-ENV:Header>
         <Security xmlns="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
         <UsernameToken>
         <Username>cce</Username>
         <Password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText">ccep</Password>
         </UsernameToken>
         <Timestamp xmlns="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd">
         <Created>2014-07-17T09:11:30.547Z</Created>
         <Expires>2014-07-17T09:21:30.547Z</Expires>
         </Timestamp>
         </Security>
         </SOAP-ENV:Header>
         */
        boolean outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        if (outbound) {

            final QName securityQName = new QName(WSS_SX_URI, "Security");
            final QName timestampQName = new QName(WSS_UTL_URI, "Timestamp");

            try {

                log.debug("Adding security header elements for SOAPMessageContext");

                SOAPMessage message = context.getMessage();
                SOAPHeader header = message.getSOAPHeader();

                if (header == null) {
                    header = message.getSOAPPart().getEnvelope().addHeader();
                }

                SOAPHeaderElement tlHeader = header.addHeaderElement(securityQName);
                //UsernameToken: enables a client to log-in to the Web service. It contains the username and password required by the Web service.
                SOAPElement userNameToken = tlHeader.addChildElement("UsernameToken");

                SOAPElement userNameElement = userNameToken.addChildElement("Username"); //userNameToken.addChildElement("Username", "wsse");
                userNameElement.addTextNode(username);

                SOAPElement passwordElement = userNameToken.addChildElement("Password");
                passwordElement.setAttribute("Type", WSS_PWD_TYPE_URI);
                passwordElement.addTextNode(password);

                //Created and Expires elements, specify the range of time during which this message is valid
                SOAPElement timestampToken = tlHeader.addChildElement(timestampQName);
                SOAPElement createdElement = timestampToken.addChildElement("Created");

                Calendar calendar = Calendar.getInstance();
                sdf.setTimeZone(TimeZone.getTimeZone("GMT+2:00"));//When issuing queries with strings as dates, we must convert the date to JHB time as that is the time used by TNF
                createdElement.addTextNode(sdf.format(calendar.getTime()));

                SOAPElement expiresElement = timestampToken.addChildElement("Expires");
                calendar.add(Calendar.MINUTE, tokenExpiry);
                expiresElement.addTextNode(sdf.format(calendar.getTime()));

                log.debug("Finished adding security header elements for SOAPMessageContext");

            } catch (SOAPException ex) {
                log.warn("Failed to initialise/add security header elements for SOAPMessageContext");
                log.warn("Error: ", ex);
                return false;
            }
        }

        return handle(context, false);
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return handle(context, true);
    }

    private boolean handle(SOAPMessageContext context, boolean isFault) {

        log.debug("TNFCCESoapHandler in CCE message handle");

        SOAPMessageContext lastContext = null;
        boolean outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        try {

            if (outbound) {
                /* Generate a unique ID, store this context in a HashMap with this ID and inject the ID into the MessageContext */
                String uuid = Utils.getUUID();
                tnfSoapMessageContextMap.put(uuid, context);
                context.put(KEY_UUID, uuid);
                context.put(BindingProvider.SOAPACTION_USE_PROPERTY, Boolean.TRUE);
                context.put(BindingProvider.SOAPACTION_URI_PROPERTY, "");

            } else {
                /* Retrieve the original context for this "response" */
                String contextKey = (String) context.get(KEY_UUID);
                lastContext = tnfSoapMessageContextMap.remove(contextKey);
            }
          

            if ((log.isDebugEnabled()) || (isFault)) {
                SOAPMessage message = context.getMessage();
                Document doc = getTNFDomDocument(message);
                boolean isClientFault = false;
                boolean isServerFault = false;

                /* If we pick up a FAULT, determine if it is a CLIENT/SERVER fault */
                if ((isFault) && (lastContext != null)) {
                    if (doc != null) {

                        String faultCode;
                        NodeList nodeList = doc.getElementsByTagName(ELEMENT_FAULT_CODE);
                        if (nodeList != null) {
                            int nodeListSize = nodeList.getLength();
                            if (nodeListSize > 0) {
                                Node faultCodeNode = nodeList.item(0);
                                if (faultCodeNode != null) {
                                    NodeList faultCodeNodeChildren = faultCodeNode.getChildNodes();
                                    if (faultCodeNodeChildren != null) {
                                        int faultCodeNodeChildrenSize = faultCodeNodeChildren.getLength();
                                        if (faultCodeNodeChildrenSize > 0) {
                                            Node faultCodeNodeChild = faultCodeNodeChildren.item(0);
                                            if (faultCodeNodeChild != null) {
                                                faultCode = faultCodeNodeChild.getNodeValue();

                                                if (log.isDebugEnabled()) {
                                                    log.debug("TNF CCE FaultCode: [" + faultCode + "]");
                                                }
                                                if (faultCode.endsWith(ERROR_TYPE_CLIENT)) {
                                                    isClientFault = true;//The SOAPMessage object was not formed correctly or did not contain the information needed to succeed.
                                                }
                                                if (faultCode.endsWith(ERROR_TYPE_SERVER)) {
                                                    isServerFault = true;//The SOAPMessage object could not be processed because of a processing error, not because of a problem with the message itself.
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (log.isDebugEnabled()) {
                    if (((isClientFault) || (isServerFault)) && (lastContext != null)) {
                        SOAPMessage lastMessage = ((SOAPMessageContext) lastContext).getMessage();
                        doc = getTNFDomDocument(lastMessage);
                        dumpTNFSOAPMessage(doc, Boolean.TRUE, false, Level.WARNING);
                    } else {
                        dumpTNFSOAPMessage(doc, outbound, true, Level.FINEST);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Error was encountered while processing SOAPMessage in TNFCCESoapHandler, going to continue anyway! Error message: [{}]", ex.getMessage());
        } finally {
            if (!outbound) {
                log.debug("Removing TNF CCE context from map in finally block of TNFCCESoapHandler");
                String contextKey = (String) context.get(KEY_UUID);
                if (contextKey != null) {
                    tnfSoapMessageContextMap.remove(contextKey);
                }
            }
        }

        return true;
    }

    private void dumpTNFSOAPMessage(Document doc, boolean outbound, boolean showCurrentMessage, Level logLevel) {

        if (doc == null) {
            log.debug("Cannot dump TNFSOAPMessage, Document is null");
            return;
        }
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(doc);
            transformer.transform(source, result);

            String msgString = result.getWriter().toString();

            String preMsg;
            if (outbound) {
                if (showCurrentMessage) {
                    preMsg = "****** TNF CCE HANDLER OUTBOUND SOAP MESSAGE ******";
                } else {
                    preMsg = "***** TNF CCE HANDLER OUTBOUND SOAP MESSAGE (SOAP FAULT CAUSE) *****";
                }
            } else {
                preMsg = "******* TNF CCE INBOUND SOAP MESSAGE  *******";
            }

            if (logLevel.equals(Level.FINEST)) {
                log.debug(preMsg);
                log.debug(msgString);
                log.debug("******** END OF TNF CCE HANDLER SOAP MESSAGE ********");
            }

            if (logLevel.equals(Level.WARNING)) {
                log.warn(preMsg);
                log.warn(msgString);
                log.warn("******** END OF TNF CCE HANDLER SOAP MESSAGE *********");
            }

        } catch (Exception ex) {
            log.warn("Error: ", ex);
        }
    }

    private Document getTNFDomDocument(SOAPMessage msg) {

        if (msg == null) {
            return null;
        }
        Document doc = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            msg.writeTo(baos);
            String msgString = baos.toString(getTNFMessageEncoding(msg));
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(msgString));
            doc = db.parse(is);
        } catch (Exception ex) {
            log.warn("Error: ", ex);
        }
        return doc;
    }

    private String getTNFMessageEncoding(SOAPMessage msg) throws SOAPException {

        String encoding = ENCODING;
        if (msg.getProperty(SOAPMessage.CHARACTER_SET_ENCODING) != null) {
            encoding = msg.getProperty(SOAPMessage.CHARACTER_SET_ENCODING).toString();
        }
        return encoding;
    }
}
