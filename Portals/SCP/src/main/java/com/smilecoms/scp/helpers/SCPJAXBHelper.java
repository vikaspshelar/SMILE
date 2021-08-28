/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.scp.helpers;

import com.smilecoms.commons.util.Utils;
import java.io.StringReader;
import javax.xml.bind.Unmarshaller;
import com.smilecoms.scp.helpers.ext.tnf.*;
import java.io.StringWriter;
import javax.xml.bind.Marshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sabza
 */
public class SCPJAXBHelper {

    private static final Logger log = LoggerFactory.getLogger(SCPJAXBHelper.class);

    public static AttributionResponse getPIsTNFAttributes(String xmlString) throws Exception {

        AttributionResponse attributionResponse;

        try {
            attributionResponse = (AttributionResponse) unmarshallXmlStringToObject(xmlString, AttributionResponse.class);
        } catch (Exception ex) {
            log.warn("Error encountered calling unmarshallXmlStringToObject 'attribution': {}", ex.getMessage());
            throw ex;
        }

        return attributionResponse;
    }

    public static MetricResponse getPIsTNFDataUsageReport(String xmlString) throws Exception {

        MetricResponse metricResponse;
        try {
            metricResponse = (MetricResponse) unmarshallXmlStringToObject(xmlString, MetricResponse.class);
        } catch (Exception ex) {
            log.warn("Error encountered calling unmarshallXmlStringToObject 'metric': {}", ex.getMessage());
            throw ex;
        }

        return metricResponse;
    }

    public static ComplexResponse getPIsReportsAndAttributes(String xmlString) throws Exception {
        ComplexResponse complexResponse;
        try {
            complexResponse = (ComplexResponse) unmarshallXmlStringToObject(xmlString, ComplexResponse.class);
        } catch (Exception ex) {
            log.warn("Error encountered calling unmarshallXmlStringToObject 'complex': {}", ex.getMessage());
            throw ex;
        }

        return complexResponse;
    }

    public static String marshallSoapObjectToString(Object inputObject) throws Exception {

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

    public static Object unmarshallXmlStringToObject(String xmlString, Class clazz) {

        if (log.isDebugEnabled()) {
            log.debug("Unmarshalling xml string...");
        }
        if (xmlString == null || xmlString.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot unmarshall a empty string - will return null");
            }
            return null;
        }

        Object object;
        try {
            Unmarshaller unmarshaller = Utils.getJAXBUnmarshaller(clazz);
            synchronized (unmarshaller) {
                object = unmarshaller.unmarshal(new StringReader(xmlString));
            }
        } catch (Exception e) {
            log.warn("Error Unmarshalling", e);
            object = "<Error Parsing>";
        }
        if (log.isDebugEnabled()) {
            log.debug("Finished unmarshalling xml to object");
        }
        return object;
    }

}
