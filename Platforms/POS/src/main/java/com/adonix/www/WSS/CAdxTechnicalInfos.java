/**
 * CAdxTechnicalInfos.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.adonix.www.WSS;
 
public class CAdxTechnicalInfos  implements java.io.Serializable {
    private boolean busy;

    private boolean changeLanguage;

    private boolean changeUserId;

    private boolean flushAdx;

    private double loadWebsDuration;

    private int nbDistributionCycle;

    private double poolDistribDuration;

    private int poolEntryIdx;

    private double poolExecDuration;

    private double poolRequestDuration;

    private double poolWaitDuration;

    private java.lang.String processReport;

    private int processReportSize;

    private boolean reloadWebs;

    private boolean resumitAfterDBOpen;

    private int rowInDistribStack;

    private double totalDuration;

    private java.lang.String traceRequest;

    private int traceRequestSize;

    public CAdxTechnicalInfos() {
    }

    public CAdxTechnicalInfos(
           boolean busy,
           boolean changeLanguage,
           boolean changeUserId,
           boolean flushAdx,
           double loadWebsDuration,
           int nbDistributionCycle,
           double poolDistribDuration,
           int poolEntryIdx,
           double poolExecDuration,
           double poolRequestDuration,
           double poolWaitDuration,
           java.lang.String processReport,
           int processReportSize,
           boolean reloadWebs,
           boolean resumitAfterDBOpen,
           int rowInDistribStack,
           double totalDuration,
           java.lang.String traceRequest,
           int traceRequestSize) {
           this.busy = busy;
           this.changeLanguage = changeLanguage;
           this.changeUserId = changeUserId;
           this.flushAdx = flushAdx;
           this.loadWebsDuration = loadWebsDuration;
           this.nbDistributionCycle = nbDistributionCycle;
           this.poolDistribDuration = poolDistribDuration;
           this.poolEntryIdx = poolEntryIdx;
           this.poolExecDuration = poolExecDuration;
           this.poolRequestDuration = poolRequestDuration;
           this.poolWaitDuration = poolWaitDuration;
           this.processReport = processReport;
           this.processReportSize = processReportSize;
           this.reloadWebs = reloadWebs;
           this.resumitAfterDBOpen = resumitAfterDBOpen;
           this.rowInDistribStack = rowInDistribStack;
           this.totalDuration = totalDuration;
           this.traceRequest = traceRequest;
           this.traceRequestSize = traceRequestSize;
    }


    /**
     * Gets the busy value for this CAdxTechnicalInfos.
     * 
     * @return busy
     */
    public boolean isBusy() {
        return busy;
    }


    /**
     * Sets the busy value for this CAdxTechnicalInfos.
     * 
     * @param busy
     */
    public void setBusy(boolean busy) {
        this.busy = busy;
    }


    /**
     * Gets the changeLanguage value for this CAdxTechnicalInfos.
     * 
     * @return changeLanguage
     */
    public boolean isChangeLanguage() {
        return changeLanguage;
    }


    /**
     * Sets the changeLanguage value for this CAdxTechnicalInfos.
     * 
     * @param changeLanguage
     */
    public void setChangeLanguage(boolean changeLanguage) {
        this.changeLanguage = changeLanguage;
    }


    /**
     * Gets the changeUserId value for this CAdxTechnicalInfos.
     * 
     * @return changeUserId
     */
    public boolean isChangeUserId() {
        return changeUserId;
    }


    /**
     * Sets the changeUserId value for this CAdxTechnicalInfos.
     * 
     * @param changeUserId
     */
    public void setChangeUserId(boolean changeUserId) {
        this.changeUserId = changeUserId;
    }


    /**
     * Gets the flushAdx value for this CAdxTechnicalInfos.
     * 
     * @return flushAdx
     */
    public boolean isFlushAdx() {
        return flushAdx;
    }


    /**
     * Sets the flushAdx value for this CAdxTechnicalInfos.
     * 
     * @param flushAdx
     */
    public void setFlushAdx(boolean flushAdx) {
        this.flushAdx = flushAdx;
    }


    /**
     * Gets the loadWebsDuration value for this CAdxTechnicalInfos.
     * 
     * @return loadWebsDuration
     */
    public double getLoadWebsDuration() {
        return loadWebsDuration;
    }


    /**
     * Sets the loadWebsDuration value for this CAdxTechnicalInfos.
     * 
     * @param loadWebsDuration
     */
    public void setLoadWebsDuration(double loadWebsDuration) {
        this.loadWebsDuration = loadWebsDuration;
    }


    /**
     * Gets the nbDistributionCycle value for this CAdxTechnicalInfos.
     * 
     * @return nbDistributionCycle
     */
    public int getNbDistributionCycle() {
        return nbDistributionCycle;
    }


    /**
     * Sets the nbDistributionCycle value for this CAdxTechnicalInfos.
     * 
     * @param nbDistributionCycle
     */
    public void setNbDistributionCycle(int nbDistributionCycle) {
        this.nbDistributionCycle = nbDistributionCycle;
    }


    /**
     * Gets the poolDistribDuration value for this CAdxTechnicalInfos.
     * 
     * @return poolDistribDuration
     */
    public double getPoolDistribDuration() {
        return poolDistribDuration;
    }


    /**
     * Sets the poolDistribDuration value for this CAdxTechnicalInfos.
     * 
     * @param poolDistribDuration
     */
    public void setPoolDistribDuration(double poolDistribDuration) {
        this.poolDistribDuration = poolDistribDuration;
    }


    /**
     * Gets the poolEntryIdx value for this CAdxTechnicalInfos.
     * 
     * @return poolEntryIdx
     */
    public int getPoolEntryIdx() {
        return poolEntryIdx;
    }


    /**
     * Sets the poolEntryIdx value for this CAdxTechnicalInfos.
     * 
     * @param poolEntryIdx
     */
    public void setPoolEntryIdx(int poolEntryIdx) {
        this.poolEntryIdx = poolEntryIdx;
    }


    /**
     * Gets the poolExecDuration value for this CAdxTechnicalInfos.
     * 
     * @return poolExecDuration
     */
    public double getPoolExecDuration() {
        return poolExecDuration;
    }


    /**
     * Sets the poolExecDuration value for this CAdxTechnicalInfos.
     * 
     * @param poolExecDuration
     */
    public void setPoolExecDuration(double poolExecDuration) {
        this.poolExecDuration = poolExecDuration;
    }


    /**
     * Gets the poolRequestDuration value for this CAdxTechnicalInfos.
     * 
     * @return poolRequestDuration
     */
    public double getPoolRequestDuration() {
        return poolRequestDuration;
    }


    /**
     * Sets the poolRequestDuration value for this CAdxTechnicalInfos.
     * 
     * @param poolRequestDuration
     */
    public void setPoolRequestDuration(double poolRequestDuration) {
        this.poolRequestDuration = poolRequestDuration;
    }


    /**
     * Gets the poolWaitDuration value for this CAdxTechnicalInfos.
     * 
     * @return poolWaitDuration
     */
    public double getPoolWaitDuration() {
        return poolWaitDuration;
    }


    /**
     * Sets the poolWaitDuration value for this CAdxTechnicalInfos.
     * 
     * @param poolWaitDuration
     */
    public void setPoolWaitDuration(double poolWaitDuration) {
        this.poolWaitDuration = poolWaitDuration;
    }


    /**
     * Gets the processReport value for this CAdxTechnicalInfos.
     * 
     * @return processReport
     */
    public java.lang.String getProcessReport() {
        return processReport;
    }


    /**
     * Sets the processReport value for this CAdxTechnicalInfos.
     * 
     * @param processReport
     */
    public void setProcessReport(java.lang.String processReport) {
        this.processReport = processReport;
    }


    /**
     * Gets the processReportSize value for this CAdxTechnicalInfos.
     * 
     * @return processReportSize
     */
    public int getProcessReportSize() {
        return processReportSize;
    }


    /**
     * Sets the processReportSize value for this CAdxTechnicalInfos.
     * 
     * @param processReportSize
     */
    public void setProcessReportSize(int processReportSize) {
        this.processReportSize = processReportSize;
    }


    /**
     * Gets the reloadWebs value for this CAdxTechnicalInfos.
     * 
     * @return reloadWebs
     */
    public boolean isReloadWebs() {
        return reloadWebs;
    }


    /**
     * Sets the reloadWebs value for this CAdxTechnicalInfos.
     * 
     * @param reloadWebs
     */
    public void setReloadWebs(boolean reloadWebs) {
        this.reloadWebs = reloadWebs;
    }


    /**
     * Gets the resumitAfterDBOpen value for this CAdxTechnicalInfos.
     * 
     * @return resumitAfterDBOpen
     */
    public boolean isResumitAfterDBOpen() {
        return resumitAfterDBOpen;
    }


    /**
     * Sets the resumitAfterDBOpen value for this CAdxTechnicalInfos.
     * 
     * @param resumitAfterDBOpen
     */
    public void setResumitAfterDBOpen(boolean resumitAfterDBOpen) {
        this.resumitAfterDBOpen = resumitAfterDBOpen;
    }


    /**
     * Gets the rowInDistribStack value for this CAdxTechnicalInfos.
     * 
     * @return rowInDistribStack
     */
    public int getRowInDistribStack() {
        return rowInDistribStack;
    }


    /**
     * Sets the rowInDistribStack value for this CAdxTechnicalInfos.
     * 
     * @param rowInDistribStack
     */
    public void setRowInDistribStack(int rowInDistribStack) {
        this.rowInDistribStack = rowInDistribStack;
    }


    /**
     * Gets the totalDuration value for this CAdxTechnicalInfos.
     * 
     * @return totalDuration
     */
    public double getTotalDuration() {
        return totalDuration;
    }


    /**
     * Sets the totalDuration value for this CAdxTechnicalInfos.
     * 
     * @param totalDuration
     */
    public void setTotalDuration(double totalDuration) {
        this.totalDuration = totalDuration;
    }


    /**
     * Gets the traceRequest value for this CAdxTechnicalInfos.
     * 
     * @return traceRequest
     */
    public java.lang.String getTraceRequest() {
        return traceRequest;
    }


    /**
     * Sets the traceRequest value for this CAdxTechnicalInfos.
     * 
     * @param traceRequest
     */
    public void setTraceRequest(java.lang.String traceRequest) {
        this.traceRequest = traceRequest;
    }


    /**
     * Gets the traceRequestSize value for this CAdxTechnicalInfos.
     * 
     * @return traceRequestSize
     */
    public int getTraceRequestSize() {
        return traceRequestSize;
    }


    /**
     * Sets the traceRequestSize value for this CAdxTechnicalInfos.
     * 
     * @param traceRequestSize
     */
    public void setTraceRequestSize(int traceRequestSize) {
        this.traceRequestSize = traceRequestSize;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof CAdxTechnicalInfos)) return false;
        CAdxTechnicalInfos other = (CAdxTechnicalInfos) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            this.busy == other.isBusy() &&
            this.changeLanguage == other.isChangeLanguage() &&
            this.changeUserId == other.isChangeUserId() &&
            this.flushAdx == other.isFlushAdx() &&
            this.loadWebsDuration == other.getLoadWebsDuration() &&
            this.nbDistributionCycle == other.getNbDistributionCycle() &&
            this.poolDistribDuration == other.getPoolDistribDuration() &&
            this.poolEntryIdx == other.getPoolEntryIdx() &&
            this.poolExecDuration == other.getPoolExecDuration() &&
            this.poolRequestDuration == other.getPoolRequestDuration() &&
            this.poolWaitDuration == other.getPoolWaitDuration() &&
            ((this.processReport==null && other.getProcessReport()==null) || 
             (this.processReport!=null &&
              this.processReport.equals(other.getProcessReport()))) &&
            this.processReportSize == other.getProcessReportSize() &&
            this.reloadWebs == other.isReloadWebs() &&
            this.resumitAfterDBOpen == other.isResumitAfterDBOpen() &&
            this.rowInDistribStack == other.getRowInDistribStack() &&
            this.totalDuration == other.getTotalDuration() &&
            ((this.traceRequest==null && other.getTraceRequest()==null) || 
             (this.traceRequest!=null &&
              this.traceRequest.equals(other.getTraceRequest()))) &&
            this.traceRequestSize == other.getTraceRequestSize();
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
        _hashCode += (isBusy() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        _hashCode += (isChangeLanguage() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        _hashCode += (isChangeUserId() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        _hashCode += (isFlushAdx() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        _hashCode += new Double(getLoadWebsDuration()).hashCode();
        _hashCode += getNbDistributionCycle();
        _hashCode += new Double(getPoolDistribDuration()).hashCode();
        _hashCode += getPoolEntryIdx();
        _hashCode += new Double(getPoolExecDuration()).hashCode();
        _hashCode += new Double(getPoolRequestDuration()).hashCode();
        _hashCode += new Double(getPoolWaitDuration()).hashCode();
        if (getProcessReport() != null) {
            _hashCode += getProcessReport().hashCode();
        }
        _hashCode += getProcessReportSize();
        _hashCode += (isReloadWebs() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        _hashCode += (isResumitAfterDBOpen() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        _hashCode += getRowInDistribStack();
        _hashCode += new Double(getTotalDuration()).hashCode();
        if (getTraceRequest() != null) {
            _hashCode += getTraceRequest().hashCode();
        }
        _hashCode += getTraceRequestSize();
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(CAdxTechnicalInfos.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.adonix.com/WSS", "CAdxTechnicalInfos"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("busy");
        elemField.setXmlName(new javax.xml.namespace.QName("", "busy"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("changeLanguage");
        elemField.setXmlName(new javax.xml.namespace.QName("", "changeLanguage"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("changeUserId");
        elemField.setXmlName(new javax.xml.namespace.QName("", "changeUserId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("flushAdx");
        elemField.setXmlName(new javax.xml.namespace.QName("", "flushAdx"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("loadWebsDuration");
        elemField.setXmlName(new javax.xml.namespace.QName("", "loadWebsDuration"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "double"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("nbDistributionCycle");
        elemField.setXmlName(new javax.xml.namespace.QName("", "nbDistributionCycle"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("poolDistribDuration");
        elemField.setXmlName(new javax.xml.namespace.QName("", "poolDistribDuration"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "double"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("poolEntryIdx");
        elemField.setXmlName(new javax.xml.namespace.QName("", "poolEntryIdx"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("poolExecDuration");
        elemField.setXmlName(new javax.xml.namespace.QName("", "poolExecDuration"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "double"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("poolRequestDuration");
        elemField.setXmlName(new javax.xml.namespace.QName("", "poolRequestDuration"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "double"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("poolWaitDuration");
        elemField.setXmlName(new javax.xml.namespace.QName("", "poolWaitDuration"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "double"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("processReport");
        elemField.setXmlName(new javax.xml.namespace.QName("", "processReport"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("processReportSize");
        elemField.setXmlName(new javax.xml.namespace.QName("", "processReportSize"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("reloadWebs");
        elemField.setXmlName(new javax.xml.namespace.QName("", "reloadWebs"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("resumitAfterDBOpen");
        elemField.setXmlName(new javax.xml.namespace.QName("", "resumitAfterDBOpen"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("rowInDistribStack");
        elemField.setXmlName(new javax.xml.namespace.QName("", "rowInDistribStack"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("totalDuration");
        elemField.setXmlName(new javax.xml.namespace.QName("", "totalDuration"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "double"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("traceRequest");
        elemField.setXmlName(new javax.xml.namespace.QName("", "traceRequest"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("traceRequestSize");
        elemField.setXmlName(new javax.xml.namespace.QName("", "traceRequestSize"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
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
