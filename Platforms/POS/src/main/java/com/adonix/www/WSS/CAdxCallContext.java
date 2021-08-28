/**
 * CAdxCallContext.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.adonix.www.WSS;
 
public class CAdxCallContext  implements java.io.Serializable {
    private java.lang.String codeLang;

    private java.lang.String poolAlias;

    private java.lang.String poolId;

    private java.lang.String requestConfig;

    public CAdxCallContext() {
    }

    public CAdxCallContext(
           java.lang.String codeLang,
           java.lang.String poolAlias,
           java.lang.String poolId,
           java.lang.String requestConfig) {
           this.codeLang = codeLang;
           this.poolAlias = poolAlias;
           this.poolId = poolId;
           this.requestConfig = requestConfig;
    }


    /**
     * Gets the codeLang value for this CAdxCallContext.
     * 
     * @return codeLang
     */
    public java.lang.String getCodeLang() {
        return codeLang;
    }


    /**
     * Sets the codeLang value for this CAdxCallContext.
     * 
     * @param codeLang
     */
    public void setCodeLang(java.lang.String codeLang) {
        this.codeLang = codeLang;
    }


    /**
     * Gets the poolAlias value for this CAdxCallContext.
     * 
     * @return poolAlias
     */
    public java.lang.String getPoolAlias() {
        return poolAlias;
    }


    /**
     * Sets the poolAlias value for this CAdxCallContext.
     * 
     * @param poolAlias
     */
    public void setPoolAlias(java.lang.String poolAlias) {
        this.poolAlias = poolAlias;
    }


    /**
     * Gets the poolId value for this CAdxCallContext.
     * 
     * @return poolId
     */
    public java.lang.String getPoolId() {
        return poolId;
    }


    /**
     * Sets the poolId value for this CAdxCallContext.
     * 
     * @param poolId
     */
    public void setPoolId(java.lang.String poolId) {
        this.poolId = poolId;
    }


    /**
     * Gets the requestConfig value for this CAdxCallContext.
     * 
     * @return requestConfig
     */
    public java.lang.String getRequestConfig() {
        return requestConfig;
    }


    /**
     * Sets the requestConfig value for this CAdxCallContext.
     * 
     * @param requestConfig
     */
    public void setRequestConfig(java.lang.String requestConfig) {
        this.requestConfig = requestConfig;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof CAdxCallContext)) return false;
        CAdxCallContext other = (CAdxCallContext) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.codeLang==null && other.getCodeLang()==null) || 
             (this.codeLang!=null &&
              this.codeLang.equals(other.getCodeLang()))) &&
            ((this.poolAlias==null && other.getPoolAlias()==null) || 
             (this.poolAlias!=null &&
              this.poolAlias.equals(other.getPoolAlias()))) &&
            ((this.poolId==null && other.getPoolId()==null) || 
             (this.poolId!=null &&
              this.poolId.equals(other.getPoolId()))) &&
            ((this.requestConfig==null && other.getRequestConfig()==null) || 
             (this.requestConfig!=null &&
              this.requestConfig.equals(other.getRequestConfig())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        if (getCodeLang() != null) {
            _hashCode += getCodeLang().hashCode();
        }
        if (getPoolAlias() != null) {
            _hashCode += getPoolAlias().hashCode();
        }
        if (getPoolId() != null) {
            _hashCode += getPoolId().hashCode();
        }
        if (getRequestConfig() != null) {
            _hashCode += getRequestConfig().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(CAdxCallContext.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxCallContext"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("codeLang");
        elemField.setXmlName(new javax.xml.namespace.QName("", "codeLang"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("poolAlias");
        elemField.setXmlName(new javax.xml.namespace.QName("", "poolAlias"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("poolId");
        elemField.setXmlName(new javax.xml.namespace.QName("", "poolId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("requestConfig");
        elemField.setXmlName(new javax.xml.namespace.QName("", "requestConfig"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanSerializer(
            _javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanDeserializer(
            _javaType, _xmlType, typeDesc);
    }

}
