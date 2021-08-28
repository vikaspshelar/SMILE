/**
 * CAdxWebServiceXmlCCService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.adonix.www.WSS;
 
public interface CAdxWebServiceXmlCCService extends javax.xml.rpc.Service {

/**
 * This SOAP web service allows to call X3 sub programs and/or to
 * manipulate X3 objects trough CRUD and specifics methods
 */
    public java.lang.String getCAdxWebServiceXmlCCAddress();

    public com.adonix.www.WSS.CAdxWebServiceXmlCC getCAdxWebServiceXmlCC() throws javax.xml.rpc.ServiceException;

    public com.adonix.www.WSS.CAdxWebServiceXmlCC getCAdxWebServiceXmlCC(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}
