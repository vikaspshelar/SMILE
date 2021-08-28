/**
 * CAdxResultXml.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.adonix.www.WSS;
 
import org.slf4j.Logger;

public class CAdxResultXml  implements java.io.Serializable {
    private com.adonix.www.WSS.CAdxMessage[] messages;

    private java.lang.String resultXml;

    private int status;

    private com.adonix.www.WSS.CAdxTechnicalInfos technicalInfos;

    public CAdxResultXml() {
    }

    public CAdxResultXml(
           com.adonix.www.WSS.CAdxMessage[] messages,
           java.lang.String resultXml,
           int status,
           com.adonix.www.WSS.CAdxTechnicalInfos technicalInfos) {
           this.messages = messages;
           this.resultXml = resultXml;
           this.status = status;
           this.technicalInfos = technicalInfos;
    }


    /**
     * Gets the messages value for this CAdxResultXml.
     * 
     * @return messages
     */
    public com.adonix.www.WSS.CAdxMessage[] getMessages() {
        return messages;
    }


    /**
     * Sets the messages value for this CAdxResultXml.
     * 
     * @param messages
     */
    public void setMessages(com.adonix.www.WSS.CAdxMessage[] messages) {
        this.messages = messages;
    }


    /**
     * Gets the resultXml value for this CAdxResultXml.
     * 
     * @return resultXml
     */
    public java.lang.String getResultXml() {
        return resultXml;
    }


    /**
     * Sets the resultXml value for this CAdxResultXml.
     * 
     * @param resultXml
     */
    public void setResultXml(java.lang.String resultXml) {
        this.resultXml = resultXml;
    }


    /**
     * Gets the status value for this CAdxResultXml.
     * 
     * @return status
     */
    public int getStatus() {
        return status;
    }


    /**
     * Sets the status value for this CAdxResultXml.
     * 
     * @param status
     */
    public void setStatus(int status) {
        this.status = status;
    }


    /**
     * Gets the technicalInfos value for this CAdxResultXml.
     * 
     * @return technicalInfos
     */
    public com.adonix.www.WSS.CAdxTechnicalInfos getTechnicalInfos() {
        return technicalInfos;
    }


    /**
     * Sets the technicalInfos value for this CAdxResultXml.
     * 
     * @param technicalInfos
     */
    public void setTechnicalInfos(com.adonix.www.WSS.CAdxTechnicalInfos technicalInfos) {
        this.technicalInfos = technicalInfos;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof CAdxResultXml)) return false;
        CAdxResultXml other = (CAdxResultXml) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.messages==null && other.getMessages()==null) || 
             (this.messages!=null &&
              java.util.Arrays.equals(this.messages, other.getMessages()))) &&
            ((this.resultXml==null && other.getResultXml()==null) || 
             (this.resultXml!=null &&
              this.resultXml.equals(other.getResultXml()))) &&
            this.status == other.getStatus() &&
            ((this.technicalInfos==null && other.getTechnicalInfos()==null) || 
             (this.technicalInfos!=null &&
              this.technicalInfos.equals(other.getTechnicalInfos())));
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
        if (getMessages() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getMessages());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getMessages(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getResultXml() != null) {
            _hashCode += getResultXml().hashCode();
        }
        _hashCode += getStatus();
        if (getTechnicalInfos() != null) {
            _hashCode += getTechnicalInfos().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(CAdxResultXml.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxResultXml"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("messages");
        elemField.setXmlName(new javax.xml.namespace.QName("", "messages"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxMessage"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("resultXml");
        elemField.setXmlName(new javax.xml.namespace.QName("", "resultXml"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("status");
        elemField.setXmlName(new javax.xml.namespace.QName("", "status"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("technicalInfos");
        elemField.setXmlName(new javax.xml.namespace.QName("", "technicalInfos"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxTechnicalInfos"));
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
    
    //SMILE CUSTOM CODE
    private String X3RecordId;
    private String statusCode;

    public String getX3RecordId() {
        return X3RecordId;
    }

    public void setX3RecordId(String X3RecordId) {
        this.X3RecordId = X3RecordId;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public void logResult(Logger log) {
        if (log.isDebugEnabled()) {
            log.debug("X3 Request result: Status [{}] Result [{}]", getStatus(), getResultXml());
            for (CAdxMessage msg : getMessages()) {
                log.debug("Message Type [{}] Message [{}]", msg.getType(), msg.getMessage());
            }
            CAdxTechnicalInfos ti = getTechnicalInfos();
            if (ti == null) {
                return;
            }
            log.debug("LoadWebsDuration [{}]", ti.getLoadWebsDuration());
            log.debug("NbDistributionCycle [{}]", ti.getNbDistributionCycle());
            log.debug("PoolDistribDuration [{}]", ti.getPoolDistribDuration());
            log.debug("PoolEntryIdx [{}]", ti.getPoolEntryIdx());
            log.debug("PoolExecDuration [{}]", ti.getPoolExecDuration());
            log.debug("PoolRequestDuration [{}]", ti.getPoolRequestDuration());
            log.debug("PoolWaitDuration [{}]", ti.getPoolWaitDuration());
            log.debug("ProcessReport [{}]", ti.getProcessReport());
            log.debug("TotalDuration [{}]", ti.getTotalDuration());
            log.debug("TraceRequest [{}]", ti.getTraceRequest());
        }
    }

    public String getErrorString() {
        StringBuilder sb = new StringBuilder();
        sb.append("X3 Request result: Status [");
        sb.append(getStatus());
        sb.append("] Result [");
        sb.append(getResultXml());
        sb.append("] ");
        for (CAdxMessage msg : getMessages()) {
            sb.append("Message Type [");
            sb.append(msg.getType());
            sb.append("] Message [");
            sb.append(msg.getMessage());
            sb.append("]");
        }
        return sb.toString();
    }

}
