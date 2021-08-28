/**
 * CAdxWebServiceXmlCCSoapBindingStub.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.adonix.www.WSS;
 
import java.util.Iterator;

import javax.xml.soap.MimeHeaders;

public class CAdxWebServiceXmlCCSoapBindingStub extends org.apache.axis.client.Stub implements com.adonix.www.WSS.CAdxWebServiceXmlCC {
    private java.util.Vector cachedSerClasses = new java.util.Vector();
    private java.util.Vector cachedSerQNames = new java.util.Vector();
    private java.util.Vector cachedSerFactories = new java.util.Vector();
    private java.util.Vector cachedDeserFactories = new java.util.Vector();

    static org.apache.axis.description.OperationDesc [] _operations;

    static {
        _operations = new org.apache.axis.description.OperationDesc[12];
        _initOperationDesc1();
        _initOperationDesc2();
    }

    private static void _initOperationDesc1(){
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("run");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "callContext"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxCallContext"), com.adonix.www.WSS.CAdxCallContext.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "publicName"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "inputXml"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxResultXml"));
        oper.setReturnClass(com.adonix.www.WSS.CAdxResultXml.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "runReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        _operations[0] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("save");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "callContext"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxCallContext"), com.adonix.www.WSS.CAdxCallContext.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "publicName"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "objectXml"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxResultXml"));
        oper.setReturnClass(com.adonix.www.WSS.CAdxResultXml.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "saveReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        _operations[1] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("delete");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "callContext"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxCallContext"), com.adonix.www.WSS.CAdxCallContext.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "publicName"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "objectKeys"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.adonix.com/WSS", "ArrayOfCAdxParamKeyValue"), com.adonix.www.WSS.CAdxParamKeyValue[].class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxResultXml"));
        oper.setReturnClass(com.adonix.www.WSS.CAdxResultXml.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "deleteReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        _operations[2] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("read");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "callContext"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxCallContext"), com.adonix.www.WSS.CAdxCallContext.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "publicName"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "objectKeys"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.adonix.com/WSS", "ArrayOfCAdxParamKeyValue"), com.adonix.www.WSS.CAdxParamKeyValue[].class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxResultXml"));
        oper.setReturnClass(com.adonix.www.WSS.CAdxResultXml.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "readReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        _operations[3] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("query");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "callContext"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxCallContext"), com.adonix.www.WSS.CAdxCallContext.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "publicName"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "objectKeys"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.adonix.com/WSS", "ArrayOfCAdxParamKeyValue"), com.adonix.www.WSS.CAdxParamKeyValue[].class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "listSize"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxResultXml"));
        oper.setReturnClass(com.adonix.www.WSS.CAdxResultXml.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "queryReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        _operations[4] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getDescription");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "callContext"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxCallContext"), com.adonix.www.WSS.CAdxCallContext.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "publicName"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxResultXml"));
        oper.setReturnClass(com.adonix.www.WSS.CAdxResultXml.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "getDescriptionReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        _operations[5] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("modify");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "callContext"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxCallContext"), com.adonix.www.WSS.CAdxCallContext.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "publicName"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "objectKeys"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.adonix.com/WSS", "ArrayOfCAdxParamKeyValue"), com.adonix.www.WSS.CAdxParamKeyValue[].class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "objectXml"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxResultXml"));
        oper.setReturnClass(com.adonix.www.WSS.CAdxResultXml.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "modifyReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        _operations[6] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("actionObject");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "callContext"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxCallContext"), com.adonix.www.WSS.CAdxCallContext.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "publicName"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "actionCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "objectXml"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxResultXml"));
        oper.setReturnClass(com.adonix.www.WSS.CAdxResultXml.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "actionObjectReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        _operations[7] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("actionObjectKeys");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "callContext"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxCallContext"), com.adonix.www.WSS.CAdxCallContext.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "publicName"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "actionCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "objectKeys"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.adonix.com/WSS", "ArrayOfCAdxParamKeyValue"), com.adonix.www.WSS.CAdxParamKeyValue[].class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxResultXml"));
        oper.setReturnClass(com.adonix.www.WSS.CAdxResultXml.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "actionObjectKeysReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        _operations[8] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getDataXmlSchema");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "callContext"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxCallContext"), com.adonix.www.WSS.CAdxCallContext.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "publicName"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxResultXml"));
        oper.setReturnClass(com.adonix.www.WSS.CAdxResultXml.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "getDataXmlSchemaReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        _operations[9] = oper;

    }

    private static void _initOperationDesc2(){
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("insertLines");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "callContext"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxCallContext"), com.adonix.www.WSS.CAdxCallContext.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "publicName"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "objectKeys"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.adonix.com/WSS", "ArrayOfCAdxParamKeyValue"), com.adonix.www.WSS.CAdxParamKeyValue[].class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "blocKey"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "lineKey"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "lineXml"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxResultXml"));
        oper.setReturnClass(com.adonix.www.WSS.CAdxResultXml.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "insertLinesReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        _operations[10] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("deleteLines");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "callContext"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxCallContext"), com.adonix.www.WSS.CAdxCallContext.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "publicName"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "objectKeys"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.adonix.com/WSS", "ArrayOfCAdxParamKeyValue"), com.adonix.www.WSS.CAdxParamKeyValue[].class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "blocKey"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "lineKeys"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.adonix.com/WSS", "ArrayOf_xsd_string"), java.lang.String[].class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxResultXml"));
        oper.setReturnClass(com.adonix.www.WSS.CAdxResultXml.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "deleteLinesReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        _operations[11] = oper;

    }

    public CAdxWebServiceXmlCCSoapBindingStub() throws org.apache.axis.AxisFault {
         this(null);
    }

    public CAdxWebServiceXmlCCSoapBindingStub(java.net.URL endpointURL, javax.xml.rpc.Service service) throws org.apache.axis.AxisFault {
         this(service);
         super.cachedEndpoint = endpointURL;
    }

    public CAdxWebServiceXmlCCSoapBindingStub(javax.xml.rpc.Service service) throws org.apache.axis.AxisFault {
        if (service == null) {
            super.service = new org.apache.axis.client.Service();
        } else {
            super.service = service;
        }
        ((org.apache.axis.client.Service)super.service).setTypeMappingVersion("1.2");
            java.lang.Class cls;
            javax.xml.namespace.QName qName;
            javax.xml.namespace.QName qName2;
            java.lang.Class beansf = org.apache.axis.encoding.ser.BeanSerializerFactory.class;
            java.lang.Class beandf = org.apache.axis.encoding.ser.BeanDeserializerFactory.class;
            java.lang.Class enumsf = org.apache.axis.encoding.ser.EnumSerializerFactory.class;
            java.lang.Class enumdf = org.apache.axis.encoding.ser.EnumDeserializerFactory.class;
            java.lang.Class arraysf = org.apache.axis.encoding.ser.ArraySerializerFactory.class;
            java.lang.Class arraydf = org.apache.axis.encoding.ser.ArrayDeserializerFactory.class;
            java.lang.Class simplesf = org.apache.axis.encoding.ser.SimpleSerializerFactory.class;
            java.lang.Class simpledf = org.apache.axis.encoding.ser.SimpleDeserializerFactory.class;
            java.lang.Class simplelistsf = org.apache.axis.encoding.ser.SimpleListSerializerFactory.class;
            java.lang.Class simplelistdf = org.apache.axis.encoding.ser.SimpleListDeserializerFactory.class;
            qName = new javax.xml.namespace.QName("http://www.adonix.com/WSS", "ArrayOf_xsd_string");
            cachedSerQNames.add(qName);
            cls = java.lang.String[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string");
            qName2 = null;
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://www.adonix.com/WSS", "ArrayOfCAdxMessage");
            cachedSerQNames.add(qName);
            cls = com.adonix.www.WSS.CAdxMessage[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxMessage");
            qName2 = null;
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://www.adonix.com/WSS", "ArrayOfCAdxParamKeyValue");
            cachedSerQNames.add(qName);
            cls = com.adonix.www.WSS.CAdxParamKeyValue[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxParamKeyValue");
            qName2 = null;
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxCallContext");
            cachedSerQNames.add(qName);
            cls = com.adonix.www.WSS.CAdxCallContext.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxMessage");
            cachedSerQNames.add(qName);
            cls = com.adonix.www.WSS.CAdxMessage.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxParamKeyValue");
            cachedSerQNames.add(qName);
            cls = com.adonix.www.WSS.CAdxParamKeyValue.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxResultXml");
            cachedSerQNames.add(qName);
            cls = com.adonix.www.WSS.CAdxResultXml.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxTechnicalInfos");
            cachedSerQNames.add(qName);
            cls = com.adonix.www.WSS.CAdxTechnicalInfos.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

    }

    protected org.apache.axis.client.Call createCall() throws java.rmi.RemoteException {
        try {
            org.apache.axis.client.Call _call = super._createCall();
            if (super.maintainSessionSet) {
                _call.setMaintainSession(super.maintainSession);
            }
            if (super.cachedUsername != null) {
                _call.setUsername(super.cachedUsername);
            }
            if (super.cachedPassword != null) {
                _call.setPassword(super.cachedPassword);
            }
            if (super.cachedEndpoint != null) {
                _call.setTargetEndpointAddress(super.cachedEndpoint);
            }
            if (super.cachedTimeout != null) {
                _call.setTimeout(super.cachedTimeout);
            }
            if (super.cachedPortName != null) {
                _call.setPortName(super.cachedPortName);
            }
            java.util.Enumeration keys = super.cachedProperties.keys();
            while (keys.hasMoreElements()) {
                java.lang.String key = (java.lang.String) keys.nextElement();
                _call.setProperty(key, super.cachedProperties.get(key));
            }
            // All the type mapping information is registered
            // when the first call is made.
            // The type mapping information is actually registered in
            // the TypeMappingRegistry of the service, which
            // is the reason why registration is only needed for the first call.
            synchronized (this) {
                if (firstCall()) {
                    // must set encoding style before registering serializers
                    _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
                    _call.setEncodingStyle(org.apache.axis.Constants.URI_SOAP11_ENC);
                    for (int i = 0; i < cachedSerFactories.size(); ++i) {
                        java.lang.Class cls = (java.lang.Class) cachedSerClasses.get(i);
                        javax.xml.namespace.QName qName =
                                (javax.xml.namespace.QName) cachedSerQNames.get(i);
                        java.lang.Object x = cachedSerFactories.get(i);
                        if (x instanceof Class) {
                            java.lang.Class sf = (java.lang.Class)
                                 cachedSerFactories.get(i);
                            java.lang.Class df = (java.lang.Class)
                                 cachedDeserFactories.get(i);
                            _call.registerTypeMapping(cls, qName, sf, df, false);
                        }
                        else if (x instanceof javax.xml.rpc.encoding.SerializerFactory) {
                            org.apache.axis.encoding.SerializerFactory sf = (org.apache.axis.encoding.SerializerFactory)
                                 cachedSerFactories.get(i);
                            org.apache.axis.encoding.DeserializerFactory df = (org.apache.axis.encoding.DeserializerFactory)
                                 cachedDeserFactories.get(i);
                            _call.registerTypeMapping(cls, qName, sf, df, false);
                        }
                    }
                }
            }
            return _call;
        }
        catch (java.lang.Throwable _t) {
            throw new org.apache.axis.AxisFault("Failure trying to get the Call object", _t);
        }
    }

    public com.adonix.www.WSS.CAdxResultXml run(com.adonix.www.WSS.CAdxCallContext callContext, java.lang.String publicName, java.lang.String inputXml) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[0]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://www.adonix.com/WSS", "run"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {callContext, publicName, inputXml});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.adonix.www.WSS.CAdxResultXml) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.adonix.www.WSS.CAdxResultXml) org.apache.axis.utils.JavaUtils.convert(_resp, com.adonix.www.WSS.CAdxResultXml.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
  throw axisFaultException;
}
    }

    public com.adonix.www.WSS.CAdxResultXml save(com.adonix.www.WSS.CAdxCallContext callContext, java.lang.String publicName, java.lang.String objectXml) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[1]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://www.adonix.com/WSS", "save"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {       
	 	java.lang.Object _resp = _call.invoke(new java.lang.Object[] {callContext, publicName, objectXml});
 		
 		
 		
        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {            	
                return (com.adonix.www.WSS.CAdxResultXml) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.adonix.www.WSS.CAdxResultXml) org.apache.axis.utils.JavaUtils.convert(_resp, com.adonix.www.WSS.CAdxResultXml.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {	 
	  throw axisFaultException;
}
    }

    public com.adonix.www.WSS.CAdxResultXml delete(com.adonix.www.WSS.CAdxCallContext callContext, java.lang.String publicName, com.adonix.www.WSS.CAdxParamKeyValue[] objectKeys) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[2]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://www.adonix.com/WSS", "delete"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {callContext, publicName, objectKeys});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.adonix.www.WSS.CAdxResultXml) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.adonix.www.WSS.CAdxResultXml) org.apache.axis.utils.JavaUtils.convert(_resp, com.adonix.www.WSS.CAdxResultXml.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {	
  throw axisFaultException;
}
    }

    public com.adonix.www.WSS.CAdxResultXml read(com.adonix.www.WSS.CAdxCallContext callContext, java.lang.String publicName, com.adonix.www.WSS.CAdxParamKeyValue[] objectKeys) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[3]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://www.adonix.com/WSS", "read"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {callContext, publicName, objectKeys});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.adonix.www.WSS.CAdxResultXml) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.adonix.www.WSS.CAdxResultXml) org.apache.axis.utils.JavaUtils.convert(_resp, com.adonix.www.WSS.CAdxResultXml.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
  throw axisFaultException;
}
    }

    public com.adonix.www.WSS.CAdxResultXml query(com.adonix.www.WSS.CAdxCallContext callContext, java.lang.String publicName, com.adonix.www.WSS.CAdxParamKeyValue[] objectKeys, int listSize) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[4]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://www.adonix.com/WSS", "query"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {callContext, publicName, objectKeys, new java.lang.Integer(listSize)});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.adonix.www.WSS.CAdxResultXml) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.adonix.www.WSS.CAdxResultXml) org.apache.axis.utils.JavaUtils.convert(_resp, com.adonix.www.WSS.CAdxResultXml.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
  throw axisFaultException;
}
    }

    public com.adonix.www.WSS.CAdxResultXml getDescription(com.adonix.www.WSS.CAdxCallContext callContext, java.lang.String publicName) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[5]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://www.adonix.com/WSS", "getDescription"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {callContext, publicName});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.adonix.www.WSS.CAdxResultXml) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.adonix.www.WSS.CAdxResultXml) org.apache.axis.utils.JavaUtils.convert(_resp, com.adonix.www.WSS.CAdxResultXml.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
  throw axisFaultException;
}
    }

    public com.adonix.www.WSS.CAdxResultXml modify(com.adonix.www.WSS.CAdxCallContext callContext, java.lang.String publicName, com.adonix.www.WSS.CAdxParamKeyValue[] objectKeys, java.lang.String objectXml) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[6]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://www.adonix.com/WSS", "modify"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {callContext, publicName, objectKeys, objectXml});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.adonix.www.WSS.CAdxResultXml) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.adonix.www.WSS.CAdxResultXml) org.apache.axis.utils.JavaUtils.convert(_resp, com.adonix.www.WSS.CAdxResultXml.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
  throw axisFaultException;
}
    }

    public com.adonix.www.WSS.CAdxResultXml actionObject(com.adonix.www.WSS.CAdxCallContext callContext, java.lang.String publicName, java.lang.String actionCode, java.lang.String objectXml) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[7]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://www.adonix.com/WSS", "actionObject"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {callContext, publicName, actionCode, objectXml});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.adonix.www.WSS.CAdxResultXml) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.adonix.www.WSS.CAdxResultXml) org.apache.axis.utils.JavaUtils.convert(_resp, com.adonix.www.WSS.CAdxResultXml.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
  throw axisFaultException;
}
    }

    public com.adonix.www.WSS.CAdxResultXml actionObjectKeys(com.adonix.www.WSS.CAdxCallContext callContext, java.lang.String publicName, java.lang.String actionCode, com.adonix.www.WSS.CAdxParamKeyValue[] objectKeys) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[8]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://www.adonix.com/WSS", "actionObjectKeys"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {callContext, publicName, actionCode, objectKeys});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.adonix.www.WSS.CAdxResultXml) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.adonix.www.WSS.CAdxResultXml) org.apache.axis.utils.JavaUtils.convert(_resp, com.adonix.www.WSS.CAdxResultXml.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
  throw axisFaultException;
}
    }

    public com.adonix.www.WSS.CAdxResultXml getDataXmlSchema(com.adonix.www.WSS.CAdxCallContext callContext, java.lang.String publicName) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[9]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://www.adonix.com/WSS", "getDataXmlSchema"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {callContext, publicName});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.adonix.www.WSS.CAdxResultXml) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.adonix.www.WSS.CAdxResultXml) org.apache.axis.utils.JavaUtils.convert(_resp, com.adonix.www.WSS.CAdxResultXml.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
  throw axisFaultException;
}
    }

    public com.adonix.www.WSS.CAdxResultXml insertLines(com.adonix.www.WSS.CAdxCallContext callContext, java.lang.String publicName, com.adonix.www.WSS.CAdxParamKeyValue[] objectKeys, java.lang.String blocKey, java.lang.String lineKey, java.lang.String lineXml) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[10]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://www.adonix.com/WSS", "insertLines"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {callContext, publicName, objectKeys, blocKey, lineKey, lineXml});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.adonix.www.WSS.CAdxResultXml) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.adonix.www.WSS.CAdxResultXml) org.apache.axis.utils.JavaUtils.convert(_resp, com.adonix.www.WSS.CAdxResultXml.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
  throw axisFaultException;
}
    }

    public com.adonix.www.WSS.CAdxResultXml deleteLines(com.adonix.www.WSS.CAdxCallContext callContext, java.lang.String publicName, com.adonix.www.WSS.CAdxParamKeyValue[] objectKeys, java.lang.String blocKey, java.lang.String[] lineKeys) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[11]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://www.adonix.com/WSS", "deleteLines"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {callContext, publicName, objectKeys, blocKey, lineKeys});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.adonix.www.WSS.CAdxResultXml) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.adonix.www.WSS.CAdxResultXml) org.apache.axis.utils.JavaUtils.convert(_resp, com.adonix.www.WSS.CAdxResultXml.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
  throw axisFaultException;
}
    }

}


