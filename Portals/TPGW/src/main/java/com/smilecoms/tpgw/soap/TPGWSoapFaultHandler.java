/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.tpgw.soap;


import com.smilecoms.commons.util.ExceptionManager;
import com.smilecoms.commons.util.FriendlyException;

import com.smilecoms.xml.schema.tpgw.TPGWError;

import java.util.Collections;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.MessageContext;
import java.util.Set;
import javax.xml.namespace.QName;
import javax.xml.soap.Detail;
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
public class TPGWSoapFaultHandler implements SOAPHandler {

    private static final Logger log = LoggerFactory.getLogger(TPGWSoapFaultHandler.class.getName());
    private static final String namespacePrefix = "TPGWError";
    private static final String namespaceURI = "http://xml.smilecoms.com/schema/TPGW";
    
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
            log.debug("In TPGWSoapFaultHandler.handleFault(...);");
            
            SOAPMessage soapMessage = ((SOAPMessageContext) context).getMessage(); 
            SOAPBody body = soapMessage.getSOAPBody();
            SOAPFault fault = body.getFault();
            Detail detail = fault.getDetail();
            
            ProtocolException pe = (ProtocolException) context.get("ProtocolException");
                        
            if(fault.getDetail() == null) { // - TPGWError element not set, therefore create a new one.
                // Create a new detail element ...
                
                //Cast ProtocolException to Exception so that the exception message is used in the error lookup.
                TPGWError tpgwError = getTPGWError (new Exception(pe), pe.getMessage());
                detail = fault.addDetail();
                SOAPElement tpgwErrorElement = detail.addChildElement(new QName(namespaceURI, "TPGWError", namespacePrefix));
                tpgwErrorElement.setAttribute("xmlns", namespaceURI); // Default namespace ...
                SOAPElement tpgwErrorDesc = tpgwErrorElement.addChildElement("ErrorDesc");
                tpgwErrorDesc.addTextNode(tpgwError.getErrorDesc());
                SOAPElement tpgwErrorType = tpgwErrorElement.addChildElement("ErrorType");
                tpgwErrorType.addTextNode(tpgwError.getErrorType());
                SOAPElement tpgwErrorCode = tpgwErrorElement.addChildElement("ErrorCode");
                tpgwErrorCode.addTextNode(tpgwError.getErrorCode());   
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
    
    private TPGWError getTPGWError (Throwable e, Object inputMsg) {
        ExceptionManager exm = new ExceptionManager(this.getClass().getName());
        FriendlyException fe = exm.getFriendlyException(e, inputMsg);
        exm.reportError(fe);
        TPGWError tpgwError = null;
        try {
            tpgwError = new TPGWError();
            tpgwError.setErrorCode(fe.getErrorCode());
            tpgwError.setErrorDesc(fe.getErrorDesc());
            tpgwError.setErrorType(fe.getErrorType());
        } catch (Exception ex) {
            log.warn("Error: ", ex);
        }
        return tpgwError;
    }
            
}
