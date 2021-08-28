/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.telcoregulator.helpers;

//import com.smilecoms.commons.util.Utils;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
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
public class RegulatorSOAPHandler implements SOAPHandler<SOAPMessageContext> {

    private static final String ENCODING = "utf-8";
    private static final Logger log = LoggerFactory.getLogger(RegulatorSOAPHandler.class);
    private static final String KEY_UUID = "contextkey";
    private static final String ELEMENT_FAULT_CODE = "faultcode";
    private static final String ERROR_TYPE_CLIENT = ":Client";
    private static final String ERROR_TYPE_SERVER = ":Server";
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final Map<String, SOAPMessageContext> soapMessageContextMap = new ConcurrentHashMap<>(10);
    private final String WSS_SX_URI = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    private final String WSS_UTL_URI = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
    private final String WSS_PWD_TYPE_URI = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText";
    private final String WSS_ADDRESSING_URI = "http://schemas.xmlsoap.org/ws/2004/08/addressing";

    public RegulatorSOAPHandler() {
    }

    @Override
    public Set<QName> getHeaders() {
        log.debug("In RegulatorSOAPHandler getHeaders");
        final HashSet<QName> headers = new HashSet<>();

        /*
        
             <S:Header>  
                <wsa:MessageID>   
                  http://example.com/someuniquestring  
                </wsa:MessageID>  
                <wsa:ReplyTo>
                  <wsa:Address>http://example.com/Myclient</wsa:Address>
                </wsa:ReplyTo>
                <wsa:To>   
                  http://example.com/fabrikam/Purchasing  
                </wsa:To>  
                <wsa:Action>   
                  http://example.com/fabrikam/SubmitPO  
                </wsa:Action>  
             <S:Header>  
        
         */
        headers.add(new QName(WSS_ADDRESSING_URI, "Action"));
        headers.add(new QName(WSS_ADDRESSING_URI, "MessageID"));
        headers.add(new QName(WSS_ADDRESSING_URI, "ReplyTo"));
        headers.add(new QName(WSS_ADDRESSING_URI, "To"));
        log.warn("Done setting up headers for WS Addressing");
        return headers;
    }

    @Override
    public void close(MessageContext context) {
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        boolean outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        if (outbound) {
            log.warn("Request direction is outbound");
            
            try {
                //context.put(MessageContext.HTTP_REQUEST_HEADERS, Collections.singletonMap("Content-Type", Collections.singletonList("application/soap+xml; charset=utf-8")));
                //Content-Type: application/soap+xml;charset=UTF-8;action="http://tempuri.org/IATCService/GetEvaluators" 
                SOAPMessage message = context.getMessage();
                if (message.getSOAPPart().getEnvelope().getHeader() == null) {
                    message.getSOAPPart().getEnvelope().addHeader();
                }
                log.warn("Setting up Action header for WS Addressing");
                SOAPHeader header = message.getSOAPPart().getEnvelope().getHeader();
                SOAPHeaderElement actionElement = header.addHeaderElement(new QName(WSS_ADDRESSING_URI, "Action"));
                actionElement.setMustUnderstand(true);

                String action = (String) context.get("javax.xml.ws.soap.http.soapaction.uri");
                log.warn("Action header for WS Addressing has URL: ", action);
                context.put("javax.xml.ws.soap.http.soapaction.uri", null);
                actionElement.addTextNode(action);

                log.warn("Setting up MessageID header for WS Addressing");
                header.addHeaderElement(new QName(WSS_ADDRESSING_URI, "MessageID")).addTextNode("uuid:" + UUID.randomUUID().toString());

                log.warn("Setting up ReplyTo header for WS Addressing");
                SOAPHeaderElement replyToElement = header.addHeaderElement(new QName(WSS_ADDRESSING_URI, "ReplyTo"));
                SOAPElement addressElement = replyToElement.addChildElement(new QName(WSS_ADDRESSING_URI, "Address"));
                addressElement.addTextNode("http://www.w3.org/2004/08/addressing/anonymous");

                log.warn("Setting up To header for WS Addressing");
                SOAPHeaderElement toElement = header.addHeaderElement(new QName(WSS_ADDRESSING_URI, "To"));
                toElement.setMustUnderstand(true);
                String endpoint = (String) context.get("javax.xml.ws.service.endpoint.address");
                toElement.addTextNode(endpoint);
                log.warn("To header for WS Addressing has URL: ", endpoint);

            } catch (SOAPException ex) {
                log.error("Encountered error setting up headers for WS Addressing: ", ex);
            }
            
            log.warn("Setting up and preparing headers for WS Addressing");

        }
        return handle(context, false);
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return handle(context, true);
    }

    private boolean handle(SOAPMessageContext context, boolean isFault) {

        log.debug("RegulatorSOAPHandler in message handle");

        SOAPMessageContext lastContext = null;
        boolean outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        try {

            if (outbound) {
                /* Generate a unique ID, store this context in a HashMap with this ID and inject the ID into the MessageContext */
                String uuid = java.util.UUID.randomUUID().toString();
                soapMessageContextMap.put(uuid, context);
                context.put(KEY_UUID, uuid);
                //context.put(BindingProvider.SOAPACTION_USE_PROPERTY, Boolean.TRUE);
                //context.put(BindingProvider.SOAPACTION_URI_PROPERTY, "");

            } else {
                /* Retrieve the original context for this "response" */
                String contextKey = (String) context.get(KEY_UUID);
                lastContext = soapMessageContextMap.remove(contextKey);
            }

            if ((log.isWarnEnabled()) || (isFault)) {
                SOAPMessage message = context.getMessage();
                Document doc = getDomDocument(message);
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
                                                    log.debug("RegulatorSOAPHandler FaultCode: [" + faultCode + "]");
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
                if (log.isWarnEnabled()) {
                    if (((isClientFault) || (isServerFault)) && (lastContext != null)) {
                        SOAPMessage lastMessage = ((SOAPMessageContext) lastContext).getMessage();
                        doc = getDomDocument(lastMessage);
                        dumpSOAPMessage(doc, Boolean.TRUE, false, Level.WARNING);
                    } else {
                        dumpSOAPMessage(doc, outbound, true, Level.WARNING);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Error was encountered while processing SOAPMessage in RegulatorSOAPHandler, going to continue anyway! Error message: [{}]", ex.getMessage());
        } finally {
            if (!outbound) {
                log.debug("Removing regulator soap message context from map in finally block of RegulatorSOAPHandler");
                String contextKey = (String) context.get(KEY_UUID);
                if (contextKey != null) {
                    soapMessageContextMap.remove(contextKey);
                }
            }
        }

        return true;
    }

    private void dumpSOAPMessage(Document doc, boolean outbound, boolean showCurrentMessage, Level logLevel) {

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
                    preMsg = "****** REGULATOR SOAP HANDLER OUTBOUND SOAP MESSAGE ******";
                } else {
                    preMsg = "***** REGULATOR SOAP HANDLER OUTBOUND SOAP MESSAGE (SOAP FAULT CAUSE) *****";
                }
            } else {
                preMsg = "******* REGULATOR SOAP INBOUND SOAP MESSAGE  *******";
            }

            if (logLevel.equals(Level.FINEST)) {
                log.debug(preMsg);
                log.debug(msgString);
                log.debug("******** END OF REGULATOR HANDLER SOAP MESSAGE ********");
            }

            if (logLevel.equals(Level.WARNING)) {
                log.warn(preMsg);
                log.warn(msgString);
                log.warn("******** END OF REGULATOR HANDLER SOAP MESSAGE *********");
            }

        } catch (Exception ex) {
            log.warn("Error: ", ex);
        }
    }

    private Document getDomDocument(SOAPMessage msg) {

        if (msg == null) {
            return null;
        }
        Document doc = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            msg.writeTo(baos);
            String msgString = baos.toString(getMessageEncoding(msg));
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

    private String getMessageEncoding(SOAPMessage msg) throws SOAPException {

        String encoding = ENCODING;
        if (msg.getProperty(SOAPMessage.CHARACTER_SET_ENCODING) != null) {
            encoding = msg.getProperty(SOAPMessage.CHARACTER_SET_ENCODING).toString();
        }
        return encoding;
    }
}
