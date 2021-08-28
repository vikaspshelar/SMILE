/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mnp.soap;


import com.smilecoms.commons.util.ExceptionManager;
import com.smilecoms.commons.util.FriendlyException;

import java.util.Collections;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.MessageContext;
import java.util.Set;
import javax.xml.namespace.QName;
import javax.xml.soap.Detail;
import javax.xml.soap.DetailEntry;
import org.slf4j.*;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.ProtocolException;

/**
 *
 * @author paul
 */
public class MNPSoapFaultHandler implements SOAPHandler {

    private static final Logger log = LoggerFactory.getLogger(MNPSoapFaultHandler.class.getName());
    private static final String namespacePrefix = "MNPError";
    private static final String namespaceURI = "http://xml.smilecoms.com/schema/MNP";
    
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
        try {
            log.debug("In MNPSoapFaultHandler.handleFault(...);");
            
            SOAPMessage soapMessage = ((SOAPMessageContext) context).getMessage(); 
            SOAPBody body = soapMessage.getSOAPBody();
            SOAPFault fault = body.getFault();
            Detail detail = fault.getDetail();
            
            ProtocolException pe = (ProtocolException) context.get("ProtocolException");
                        
            if(fault.getDetail() == null) { // - TPGWError element not set, therefore create a new one.
                // Create a new detail element ...
                detail = fault.addDetail();
                DetailEntry entry = detail.addDetailEntry(new QName(namespaceURI, "MNPError", namespacePrefix));
                entry.setValue(pe.getMessage());
                // - Save changes
                soapMessage.saveChanges();
            }
            
        } catch(SOAPException ex) {
            log.warn("Error: ", ex);
        }
        return false;
    }

    @Override
    public void close(MessageContext context) {
    }
    
    public boolean handle(MessageContext context) {
        return true;
    }
}
