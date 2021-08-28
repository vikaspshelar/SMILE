/**
 * CAdxWebServiceXmlCC.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.adonix.www.WSS;
 
public interface CAdxWebServiceXmlCC extends java.rmi.Remote {

    /**
     * Run X3 sub program
     */
    public com.adonix.www.WSS.CAdxResultXml run(com.adonix.www.WSS.CAdxCallContext callContext, java.lang.String publicName, java.lang.String inputXml) throws java.rmi.RemoteException;

    /**
     * Create X3 object
     */
    public com.adonix.www.WSS.CAdxResultXml save(com.adonix.www.WSS.CAdxCallContext callContext, java.lang.String publicName, java.lang.String objectXml) throws java.rmi.RemoteException;

    /**
     * Delete X3 object
     */
    public com.adonix.www.WSS.CAdxResultXml delete(com.adonix.www.WSS.CAdxCallContext callContext, java.lang.String publicName, com.adonix.www.WSS.CAdxParamKeyValue[] objectKeys) throws java.rmi.RemoteException;

    /**
     * Read X3 object
     */
    public com.adonix.www.WSS.CAdxResultXml read(com.adonix.www.WSS.CAdxCallContext callContext, java.lang.String publicName, com.adonix.www.WSS.CAdxParamKeyValue[] objectKeys) throws java.rmi.RemoteException;

    /**
     * Get X3 objects list
     */
    public com.adonix.www.WSS.CAdxResultXml query(com.adonix.www.WSS.CAdxCallContext callContext, java.lang.String publicName, com.adonix.www.WSS.CAdxParamKeyValue[] objectKeys, int listSize) throws java.rmi.RemoteException;

    /**
     * Get X3 web service description regarding publication done in
     * GESAWE
     */
    public com.adonix.www.WSS.CAdxResultXml getDescription(com.adonix.www.WSS.CAdxCallContext callContext, java.lang.String publicName) throws java.rmi.RemoteException;

    /**
     * Update X3 object
     */
    public com.adonix.www.WSS.CAdxResultXml modify(com.adonix.www.WSS.CAdxCallContext callContext, java.lang.String publicName, com.adonix.www.WSS.CAdxParamKeyValue[] objectKeys, java.lang.String objectXml) throws java.rmi.RemoteException;

    /**
     * Execute specific action on X3 object providing XML flow
     */
    public com.adonix.www.WSS.CAdxResultXml actionObject(com.adonix.www.WSS.CAdxCallContext callContext, java.lang.String publicName, java.lang.String actionCode, java.lang.String objectXml) throws java.rmi.RemoteException;

    /**
     * Execute specific action on X3 object providing keys
     */
    public com.adonix.www.WSS.CAdxResultXml actionObjectKeys(com.adonix.www.WSS.CAdxCallContext callContext, java.lang.String publicName, java.lang.String actionCode, com.adonix.www.WSS.CAdxParamKeyValue[] objectKeys) throws java.rmi.RemoteException;

    /**
     * Get X3 web service schema regarding publication done in GESAWE
     */
    public com.adonix.www.WSS.CAdxResultXml getDataXmlSchema(com.adonix.www.WSS.CAdxCallContext callContext, java.lang.String publicName) throws java.rmi.RemoteException;

    /**
     * NOT YET IMPLEMENTED !!!
     */
    public com.adonix.www.WSS.CAdxResultXml insertLines(com.adonix.www.WSS.CAdxCallContext callContext, java.lang.String publicName, com.adonix.www.WSS.CAdxParamKeyValue[] objectKeys, java.lang.String blocKey, java.lang.String lineKey, java.lang.String lineXml) throws java.rmi.RemoteException;

    /**
     * Remove lines from X3 object table
     */
    public com.adonix.www.WSS.CAdxResultXml deleteLines(com.adonix.www.WSS.CAdxCallContext callContext, java.lang.String publicName, com.adonix.www.WSS.CAdxParamKeyValue[] objectKeys, java.lang.String blocKey, java.lang.String[] lineKeys) throws java.rmi.RemoteException;
}
