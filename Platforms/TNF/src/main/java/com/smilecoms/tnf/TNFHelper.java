/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.tnf;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.tnf.soapclient.AttributeNames;
import com.smilecoms.tnf.soapclient.AttributionRequest;
import com.smilecoms.tnf.soapclient.AttributionResponse;
import com.smilecoms.tnf.soapclient.BaseRequest;
import com.smilecoms.tnf.soapclient.ComplexRequest;
import com.smilecoms.tnf.soapclient.ComplexResponse;
import com.smilecoms.tnf.soapclient.CustomerCare;
import com.smilecoms.tnf.soapclient.CustomerCareService;
import com.smilecoms.tnf.soapclient.MetricReportIds;
import com.smilecoms.tnf.soapclient.MetricRequest;
import com.smilecoms.tnf.soapclient.MetricRequestParameter;
import com.smilecoms.tnf.soapclient.MetricRequestParameterSet;
import com.smilecoms.tnf.soapclient.MetricResponse;
import com.smilecoms.tnf.soapclient.MetricTimeRange;
import com.smilecoms.xml.schema.tnf.Done;
import com.smilecoms.xml.schema.tnf.TNFData;
import com.smilecoms.xml.schema.tnf.TNFQuery;
import java.io.StringWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.locks.Lock;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.soap.Detail;
import javax.xml.soap.DetailEntry;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sabza
 */
public class TNFHelper {

    private static final Logger log = LoggerFactory.getLogger(TNFHelper.class);
    private static CustomerCare tnfCCEService = null;
    private static TNFCCESoapHandler tnfCCESpHandler = null;
    public static Properties tnfConfigs;
    private static final Lock tnfLock = new java.util.concurrent.locks.ReentrantLock();
    private static long propsLastChecked = 0;

    public static TNFData getAttributionDataAsXmlString(TNFQuery tnfQuery) throws Exception {

        String resultAsString;
        TNFData data = new TNFData();
        AttributionResponse attributionResponse;
        AttributionRequest attributionRequest = new AttributionRequest();
        attributionRequest.setSubscriberId(tnfQuery.getLogicalSimId());

        if (tnfQuery.getAttributeNames() != null) {
            log.debug("attributionQuery.getAttributeNames is not null, going to add attributes to attribution request");
            com.smilecoms.xml.schema.tnf.AttributeNames attribList = tnfQuery.getAttributeNames();
            AttributeNames attribName = new AttributeNames();

            for (String attrib : attribList.getAttributeName()) {
                attribName.getAttributeName().add(attrib);
            }

            attributionRequest.setAttributeNames(attribName);
            log.debug("Finished adding AttributeList to attribution request");
        }

        try {
            log.debug("calling internal 'attribution' method");
            attributionResponse = attribution(attributionRequest);
            log.debug("calling internal 'attribution' method was successful");
            resultAsString = marshallSoapObjectToString(attributionResponse);
            data.setTNFXmlData(resultAsString);
        } catch (Exception ex) {
            log.warn("Error encountered calling internal 'attribution': {}", ex.getMessage());
            throw ex;
        }

        return data;
    }

    public static TNFData getMetricDataAsXmlString(TNFQuery tnfQuery) throws Exception {

        TNFData data = new TNFData();
        MetricResponse metricResponse;
        MetricRequest metricRequest = new MetricRequest();

        //Report ID
        metricRequest.setMetricReportId(tnfQuery.getReportIds().getReportId().get(0));
        metricRequest.setSubscriberId(tnfQuery.getLogicalSimId());

        //Specify additional parameters if requested report supports them.
        if (tnfQuery.getParameterSet() != null && !tnfQuery.getParameterSet().getParameter().isEmpty()) {
            log.debug("ParameterSet is not null, going to add set to metric request");
            com.smilecoms.xml.schema.tnf.ParameterSet parameterSet = tnfQuery.getParameterSet();
            MetricRequestParameterSet requestParamSet = new MetricRequestParameterSet();

            for (com.smilecoms.xml.schema.tnf.Parameter param : parameterSet.getParameter()) {
                MetricRequestParameter requestParam = new MetricRequestParameter();
                requestParam.setMetricRequestParameterKey(param.getParameterKey());
                requestParam.setMetricRequestParameterVal(param.getParameterVal());
                requestParamSet.getMetricRequestParameter().add(requestParam);
            }
            if (requestParamSet.getMetricRequestParameter().size() > 0) {
                metricRequest.setMetricRequestParameterSet(requestParamSet);
            }
            log.debug("Finished adding MetricRequestParameterSet to metric request");
        }

        //Time range
        MetricTimeRange timeRage = new MetricTimeRange();
        convertToJHBDate(tnfQuery.getTimeRange().getStartTime());
        convertToJHBDate(tnfQuery.getTimeRange().getEndTime());
        timeRage.setStartTime(tnfQuery.getTimeRange().getStartTime());
        timeRage.setEndTime(tnfQuery.getTimeRange().getEndTime());
        timeRage.setGranularity(tnfQuery.getTimeRange().getGranularity());

        metricRequest.setMetricTimeRange(timeRage);

        try {
            log.debug("calling internal 'metric' method");
            metricResponse = metric(metricRequest);
            log.debug("calling internal 'metric' method was successful");
            data.setTNFXmlData(marshallSoapObjectToString(metricResponse));
        } catch (Exception ex) {
            log.warn("Error encountered calling internal 'metric': {}", ex.getMessage());
            throw ex;
        }

        return data;
    }

    public static TNFData getComplexDataAsXmlString(TNFQuery tnfQuery) throws Exception {

        TNFData data = new TNFData();
        ComplexResponse complexResponse;
        ComplexRequest complexRequest = new ComplexRequest();

        complexRequest.setSubscriberId(tnfQuery.getLogicalSimId());

        if (tnfQuery.getAttributeNames() != null && !tnfQuery.getAttributeNames().getAttributeName().isEmpty()) {
            log.debug("Complex tnfQuery.getAttributeNames is not null, going to assign it");
            com.smilecoms.xml.schema.tnf.AttributeNames an = tnfQuery.getAttributeNames();
            AttributeNames attribName = new AttributeNames();
            for (String attrib : an.getAttributeName()) {
                attribName.getAttributeName().add(attrib);
            }
            complexRequest.setAttributeNames(attribName);
        }

        if (tnfQuery.getReportIds() != null) {
            log.warn("Complex tnfQuery.getMetricReportIds is not null, going to add list of report ids");
            MetricReportIds reportIdName = new MetricReportIds();
            for (String reportId : tnfQuery.getReportIds().getReportId()) {
                reportIdName.getMetricReportId().add(reportId);
                log.debug("Found report id {}", reportId);
            }
            complexRequest.setMetricReportIds(reportIdName);
        }

        //Time range is compulsory
        com.smilecoms.xml.schema.tnf.TimeRange metricTimeRange = tnfQuery.getTimeRange();
        MetricTimeRange timeRange = new MetricTimeRange();
        convertToJHBDate(metricTimeRange.getEndTime());
        convertToJHBDate(metricTimeRange.getStartTime());
        timeRange.setEndTime(metricTimeRange.getEndTime());
        timeRange.setStartTime(metricTimeRange.getStartTime());
        timeRange.setGranularity(metricTimeRange.getGranularity());

        complexRequest.setMetricTimeRange(timeRange);

        if (tnfQuery.getParameterSet() != null && !tnfQuery.getParameterSet().getParameter().isEmpty()) {
            log.debug("Complex tnfQuery.getMetricRequestParameterSet is not null, going to add attributes filter");
            com.smilecoms.xml.schema.tnf.ParameterSet requestParameterSet = tnfQuery.getParameterSet();
            MetricRequestParameterSet paramSet = new MetricRequestParameterSet();
            for (com.smilecoms.xml.schema.tnf.Parameter requestParam : requestParameterSet.getParameter()) {
                MetricRequestParameter param = new MetricRequestParameter();
                param.setMetricRequestParameterKey(requestParam.getParameterKey());
                param.setMetricRequestParameterVal(requestParam.getParameterVal());
                paramSet.getMetricRequestParameter().add(param);
            }
            complexRequest.setMetricRequestParameterSet(paramSet);
        }

        try {

            log.debug("calling internal 'complex' method");
            complexResponse = complex(complexRequest);
            log.debug("calling internal 'complex' method was successful");

            data.setTNFXmlData(marshallSoapObjectToString(complexResponse));

        } catch (Exception ex) {
            log.warn("Error encountered calling internal 'complex': {}", ex.getMessage());
            throw ex;
        }

        return data;
    }

    public static void initialise() {
        //(D SLD)
        log.warn("TNF Web service in init method");

        if (tnfConfigs != null) {
            tnfConfigs = null;
        }
        tnfConfigs = new Properties();

        try {
            log.warn("TNF Web service Properties as contained in env.tnf.config \n");
            log.warn(BaseUtils.getProperty("env.tnf.config"));
            tnfConfigs.load(BaseUtils.getPropertyAsStream("env.tnf.config"));
            if (tnfCCESpHandler != null) {
                tnfCCESpHandler = null;
            }
            tnfCCESpHandler = new TNFCCESoapHandler(tnfConfigs.getProperty("CCE_Username"), tnfConfigs.getProperty("CCE_Password"), Integer.parseInt(tnfConfigs.getProperty("CCE_TokenExpiresMin")));
            tnfCCEService = null;
        } catch (Exception ex) {
            log.error("Failed to load properties from env.tnf.config: {}", ex);
        }
        log.warn("TNF Web service exiting init method");
    }

    private static CustomerCare getTNFCCEService() throws Exception {
        //(D SLD) lazy instantiation

        if (tnfCCEService != null && (System.currentTimeMillis() < (propsLastChecked + 60000))) {

            if (log.isDebugEnabled()) {
                int tnfSvcId = System.identityHashCode(tnfCCEService); //Returns the same hash code for the given object.
                log.debug("Expecting only one global TNF CCE service object to be instatiated, the id of this object is: {}", tnfSvcId);
            }

            return tnfCCEService;
        }

        log.debug("Initialising TNF CCE service");
        try {

            if (!tnfLock.tryLock()) {//Acquires the lock only if it is not held by another thread at the time of invocation.
                log.debug("Another thread is already initialising TNF CCE service. Won't try to do the same");
                return null;
            }

            initialise();

            //Don't depend on the service being up and running just to be able to create a client. No remote requests to the live WSDL/XSDs will be made at runtime
            //Also, helps to be able to manage a consistent versioning scheme for the WSDL descriptions
            Done dummy;
            dummy = new Done();
            URL url = dummy.getClass().getClassLoader().getResource("/cce.wsdl");

            String endpointAddress = tnfConfigs.getProperty("CCE_EndpointAddress"); //e.g. CCE_EndpointAddress=http://10.0.1.92:8201/customer-care-endpoint
            log.debug("TNF CCE Endpoint address: {}", endpointAddress);

            CustomerCareService service = new CustomerCareService(url);
            tnfCCEService = service.getCustomerCareSoap11();

            log.debug("TNF CCE Service object is created, going to add binding properties");

            //((BindingProvider) ttService).getRequestContext().put("com.sun.xml.ws.client.ContentNegotiation", "optimistic");//TODO: read more to understand
            ((BindingProvider) tnfCCEService).getRequestContext().put("javax.xml.ws.client.connectionTimeout", Integer.parseInt(tnfConfigs.getProperty("CCE_ConnectTimeOutSecs")) * 1000);
            ((BindingProvider) tnfCCEService).getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.parseInt(tnfConfigs.getProperty("CCE_RequestTimeOutSecs")) * 1000);
            ((BindingProvider) tnfCCEService).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointAddress);
            ((BindingProvider) tnfCCEService).getBinding().setHandlerChain(Arrays.<Handler>asList(tnfCCESpHandler));

            propsLastChecked = System.currentTimeMillis();

            log.debug("Finished adding binding configs");

        } catch (Exception ex) {
            log.debug("Error occured whilst trying to initialise TNF CCE service");
            log.warn("Error: ", ex);
            tnfCCEService = null;
            throw new RuntimeException(ex);
        } finally {
            tnfLock.unlock();
        }

        log.debug("TNF CCE Service is initialised successfully and ready to use");
        return tnfCCEService;
    }

    /**
     * Metric operation provides access to individual predefined reports for
     * given msisdn. The reports can include information such as distribution of
     * volume per applications or devices, customer experience score and others
     * KPIs available as per the relevant dataset.
     */
    private static MetricResponse metric(MetricRequest metricRequest) throws Exception {

        MetricResponse result = null;
        try {
            if (log.isDebugEnabled()) {
                log.debug("Calling external 'metric' on CCE web service passing report id [{}], for msisdn [{}]", metricRequest.getMetricReportId(), metricRequest.getSubscriberId());
            }
            long start = System.currentTimeMillis();
            result = getTNFCCEService().metric(metricRequest);
            long end = System.currentTimeMillis();
            long callDuration = end - start;
            if (log.isDebugEnabled()) {
                log.debug("Finished calling external 'metric' on CCE web service. Request took " + callDuration + "ms");
            }
        } catch (Exception ex) {

            Throwable underlyingCause = ex.getCause();
            while (underlyingCause != null && underlyingCause.getCause() != null) {
                underlyingCause = underlyingCause.getCause();
            }
            if (underlyingCause == null) {
                underlyingCause = ex;
            }
            if (underlyingCause instanceof java.net.SocketTimeoutException) {
                java.net.SocketTimeoutException socketException = (java.net.SocketTimeoutException) underlyingCause;
                throw socketException;
            }
            if (underlyingCause instanceof javax.xml.ws.soap.SOAPFaultException) {
                javax.xml.ws.soap.SOAPFaultException soapFault = (javax.xml.ws.soap.SOAPFaultException) underlyingCause;
                String errMsg = processSOAPFault(soapFault, metricRequest, "metric");
                if (errMsg.isEmpty()) {
                    errMsg = underlyingCause.toString();
                }
                if (errMsg.contains("No data available")) {
                    throw new Exception("No results found from TNF");
                }
                throw new Exception(errMsg);
            }
            throw ex;
        }
        return result;
    }

    /**
     * Complex operation combines the Attribution and Metric operations
     * responses for given MSISDN. Optionally you can specify attribute filter,
     * list of report ids and/or additional metric parameters. If no filter is
     * specified the full list of pre-defined reports will be returned.
     */
    private static ComplexResponse complex(ComplexRequest complexRequest) throws Exception {

        ComplexResponse result = null;
        try {
            if (log.isDebugEnabled()) {
                log.debug("Calling 'complex' on CCE web service, for msisdn [{}]", complexRequest.getSubscriberId());
            }
            long start = System.currentTimeMillis();
            result = getTNFCCEService().complex(complexRequest);
            long end = System.currentTimeMillis();
            long callDuration = end - start;
            if (log.isDebugEnabled()) {
                log.debug("Finished calling external 'complex' on CCE web service. Request took " + callDuration + "ms");
            }

        } catch (Exception ex) {

            Throwable underlyingCause = ex.getCause();
            while (underlyingCause != null && underlyingCause.getCause() != null) {
                underlyingCause = underlyingCause.getCause();
            }
            if (underlyingCause == null) {
                underlyingCause = ex;
            }
            if (underlyingCause instanceof java.net.SocketTimeoutException) {
                java.net.SocketTimeoutException socketException = (java.net.SocketTimeoutException) underlyingCause;
                throw socketException;
            }
            if (underlyingCause instanceof javax.xml.ws.soap.SOAPFaultException) {
                javax.xml.ws.soap.SOAPFaultException soapFault = (javax.xml.ws.soap.SOAPFaultException) underlyingCause;

                String errMsg = processSOAPFault(soapFault, complexRequest, "complex");
                if (errMsg.isEmpty()) {
                    errMsg = underlyingCause.toString();
                }
                if (errMsg.contains("No data available")) {
                    throw new Exception("No results found from TNF");
                }
                throw new Exception(errMsg);
            }
            throw ex;
        }
        return result;
    }

    /**
     * Attribution operation fetches all available subscriber attributes or
     * filter them by name. Retrieved customer attributes are as available from
     * Vantage Application Server, (such as age, gender) or automatically
     * discovered (such as Device iOS / Android OS Version). The set of
     * available attributes can be restricted for security reasons.
     */
    private static AttributionResponse attribution(AttributionRequest attributionRequest) throws Exception {

        AttributionResponse result = null;
        try {
            if (log.isDebugEnabled()) {
                log.debug("Calling 'attribution' on CCE web service, for msisdn/logicalsim [{}]", attributionRequest.getSubscriberId());
            }

            long start = System.currentTimeMillis();
            result = getTNFCCEService().attribution(attributionRequest);
            long end = System.currentTimeMillis();
            long callDuration = end - start;

            if (log.isDebugEnabled()) {
                log.debug("Finished calling external 'attribution' on CCE web service. Request took " + callDuration + "ms");
            }

        } catch (Exception ex) {
            Throwable underlyingCause = ex.getCause();
            while (underlyingCause != null && underlyingCause.getCause() != null) {
                underlyingCause = underlyingCause.getCause();
            }
            if (underlyingCause == null) {
                underlyingCause = ex;
            }
            if (underlyingCause instanceof java.net.SocketTimeoutException) {
                java.net.SocketTimeoutException socketException = (java.net.SocketTimeoutException) underlyingCause;
                throw socketException;
            }
            if (underlyingCause instanceof javax.xml.ws.soap.SOAPFaultException) {
                javax.xml.ws.soap.SOAPFaultException soapFault = (javax.xml.ws.soap.SOAPFaultException) underlyingCause;

                String errMsg = processSOAPFault(soapFault, attributionRequest, "attribution");
                if (errMsg.isEmpty()) {
                    errMsg = underlyingCause.toString();
                }
                if (errMsg.contains("No data available")) {
                    throw new Exception("No results found from TNF");
                }
                throw new Exception(errMsg);
            }
            throw ex;

        }
        return result;
    }

    private static String processSOAPFault(javax.xml.ws.soap.SOAPFaultException soapFault, BaseRequest baseRequest, String methodName) {
        String errMsg = "";
        try {

            String endPoint;
            endPoint = tnfConfigs.getProperty("CCE_EndpointAddress");
            if (endPoint.isEmpty()) {
                endPoint = "TNF CCE ENDPOINT";
            }
            String faultCode = soapFault.getFault().getFaultCode();
            StringBuilder sbDetail = new StringBuilder();
            String faultDetail = "";
            String faultString = soapFault.getFault().getFaultString();

            if (soapFault.getFault().hasDetail()) {
                Detail newDetail = soapFault.getFault().getDetail();
                if (newDetail != null) {
                    Iterator entries = newDetail.getDetailEntries();
                    while (entries.hasNext()) {
                        DetailEntry newEntry = (DetailEntry) entries.next();
                        String value = newEntry.getValue();
                        sbDetail.append(value).append("#");
                    }
                    String detailList = sbDetail.toString();
                    if (!detailList.isEmpty()) {
                        faultDetail = detailList.substring(0, detailList.lastIndexOf("#"));
                    }
                }
            }

            if (faultDetail.isEmpty()) {
                faultDetail = "NO FAULT DETAIL";
            }

            if (faultCode.contains("Client")) {
                errMsg = "TNF CCE CLIENT ERROR: [" + faultString + "] DETAIL: [" + faultDetail + "]  Endpoint [" + endPoint + "] Method [" + methodName + "] Passing [" + baseRequest.getClass().getName() + "] ]";
            } else {
                errMsg = "TNF CCE SERVER ERROR: [" + faultString + "] DETAIL: [" + faultDetail + "]  Endpoint [" + endPoint + "] Method [" + methodName + "] Passing [" + baseRequest.getClass().getName() + "] ]";
            }

        } catch (Exception ex1) {
            log.warn("Error compiling soap error string", ex1);
        }
        return errMsg;
    }

    private static String marshallSoapObjectToString(Object inputObject) {

        if (log.isDebugEnabled()) {
            log.debug("Marshalling object to xml...");
        }
        if (inputObject == null) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot marshall a null object - will return null");
            }
            return null;
        }

        String xml;
        try {

            Class inputClass = inputObject.getClass();

            StringWriter writer = new StringWriter();
            Marshaller marshaller = Utils.getJAXBMarshallerForSoap(inputClass);
            synchronized (marshaller) {
                marshaller.marshal(inputObject, writer);
            }
            xml = writer.toString();
        } catch (Exception e) {
            log.debug("Error marshalling request object to a string. Will return <Error Parsing>. Error: [{}]", e.toString());
            log.warn("Error: ", e);
            xml = "<Error Parsing>";
        }
        if (log.isDebugEnabled()) {
            log.debug("Finished marshalling object to xml. Result is [{}]", xml);
        }
        return xml;
    }

    /*
     * When issuing queries, we must convert the date to JHB time as that is the time used by TNF
     * http://jira.smilecoms.com/browse/HBT-979   
     */
    private static void convertToJHBDate(XMLGregorianCalendar gcDate) {
        TimeZone timeZone = TimeZone.getTimeZone("GMT+2:00");
        int offSet = (timeZone.getRawOffset() / 1000) / 60;
        gcDate.setTimezone(offSet);
    }
}
