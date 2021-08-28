/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mnp.tz.soap;


import com.smilecoms.commons.sca.Event;
import com.smilecoms.commons.sca.SCAWrapper;
import java.io.StringWriter;

import java.util.Collections;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.MessageContext;
import java.util.Set;
import javax.xml.soap.Detail;
import org.slf4j.*;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;

/**
 *
 * @author paul
 */
public class TZMNPSoapFaultHandler implements SOAPHandler {

    private static final Logger log = LoggerFactory.getLogger(TZMNPSoapFaultHandler.class.getName());
    
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
        log.debug("In handleFault");
        
        Document document = null;
        String   strXmlDoc = null;
        String   npOrderID = null;
        String   messageID = null;
        String   requestXML = null;
        String   messageType = null;
        
        try {
            if(context.containsKey("NPOrderID")){
                npOrderID = (String) context.get("NPOrderID");
            }
            
            if(context.containsKey("MessageID")){
                messageID = (String) context.get("MessageID");
            }
            
            if(context.containsKey("MessageType")){
                messageType = (String) context.get("MessageType");
            }
            
            log.debug("In TZMNPSoapFaultHandler.handleFault(...);");
            // ((LogicalMessageContext) context).get; 
            SOAPMessage soapMessage = ((SOAPMessageContext) context).getMessage(); 
            SOAPBody body = soapMessage.getSOAPBody();
            SOAPFault fault = body.getFault();
            Detail detail = fault.getDetail();
            
            document = body.extractContentAsDocument();
            
            strXmlDoc = printXMLDocument(document); 
            
            body.addDocument(document); // Is this not adding the document twice? 
            
        } catch(Exception ex) {
            log.warn("Error: ", ex);
        } finally {
            try {
                    String eventSubType;
                    if(context.containsKey("RequestXML")){
                        requestXML = (String) context.get("RequestXML");
                    }
                    if(messageType != null) {
                         eventSubType = messageType;
                    } else {
                         eventSubType = "TZMNP";
                    }
                    
                    createEvent(eventSubType, npOrderID, messageID, requestXML, null, strXmlDoc);
                    
                } catch (Exception ex) {
                    log.error("Error while trying to save the received event for MNP.", ex);
                }
        }      
        return true;
    }

    @Override
    public void close(MessageContext context) {
    }
    
    public boolean handle(MessageContext context) {
        return true;
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
