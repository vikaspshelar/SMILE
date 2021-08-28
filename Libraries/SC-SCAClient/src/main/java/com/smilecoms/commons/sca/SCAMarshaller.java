package com.smilecoms.commons.sca;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import org.slf4j.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

/**
 * Utility Class to marshall xml beans into XML. Use by the SCA Validator and
 * for logging XML being sent to SCA. Uses JAXB
 *
 * @author PCB
 */
public class SCAMarshaller {

    private static final HashMap contextMap = new HashMap();
    private static final String CLASS = SCAMarshaller.class.getName();
    private static final Logger log = LoggerFactory.getLogger(CLASS);

    /**
     * Gets a marshaller for a specific class. Stores the JAXB Context in a map
     * for caching purposes.
     *
     * @param c the Class that needs a marshaller
     * @return The JAXB Marshaller
     */
    @SuppressWarnings("unchecked")
    private static Marshaller getMarshaller(Class c) throws JAXBException {
        JAXBContext context = (JAXBContext) contextMap.get(c.getName());

        if (context == null) {
            // Not in context cache map - create it and place in cache
            context = JAXBContext.newInstance(c);

            synchronized (contextMap) {
                //Could be written to with multiple threads. Make thread safe
                contextMap.put(c.getName(), context);
            }
        }
        // Marshaller is not thread safe so cant cache it. Must create new one
        Marshaller ret = context.createMarshaller();
        ret.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        return ret;
    }

    /**
     * Helper to log an object as XML
     *
     * @param objString the object XML
     * @param desc The description of the object - can be null
     */
    private static void logObject(String objString, String desc) {
        log.debug("------------------------- XML - BEGIN -------------------------");
        if (desc != null) {
            log.debug(desc);
        }
        log.debug(objString);
        log.debug("-------------------------- XML - END --------------------------");
    }

    /**
     * Helper to log an object as XML
     *
     * @param obj The object to log
     * @param desc Description of the object to include at the top of the log
     */
    public static void logObject(Object obj, String desc) {
        logObject(marshalToByteArray(obj, SCAConstants.SCA_SCHEMA_NAMESPACE).toString(), desc);
    }

    public static void logObject(Object obj, String desc, String namespace) {
        logObject(marshalToByteArray(obj, namespace).toString(), desc);
    }

    /**
     * Marshal an object to an XML Source
     *
     * @param javaObj the object to marshal
     * @return Resulting XML Source object
     */
    public static Source marshalToSource(Object javaObj) {
        ByteArrayInputStream in = new ByteArrayInputStream(marshalToByteArray(javaObj, SCAConstants.SCA_SCHEMA_NAMESPACE).toByteArray());
        Source xml = new StreamSource(in);
        return xml;
    }

    /**
     * Marshal an object to a String
     *
     * @param javaObj the object to marshal
     * @return Resulting String
     */
    public static String marshalToString(Object javaObj) {
        String res = marshalToByteArray(javaObj, SCAConstants.SCA_SCHEMA_NAMESPACE).toString();
        if (res.length() > 10000) {
            res = res.substring(0, 10000) + "...TOO LARGE TO DISPLAY FULLY...";
        }
        return res;
    }

    public static String marshalToString(Object javaObj, String namespace) {
        String res =  marshalToByteArray(javaObj, namespace).toString();
        if (res.length() > 10000) {
            res = res.substring(0, 10000) + "...TOO LARGE TO DISPLAY FULLY...";
        }
        return res;
    }

    /**
     * Marshal an object to a ByteArrayOutputStream
     *
     * @param javaObj the object to marshal
     * @return Resulting ByteArrayOutputStream
     */
    @SuppressWarnings("unchecked")
    private static ByteArrayOutputStream marshalToByteArray(Object javaObj, String namespace) {

        ByteArrayOutputStream resultingXML = new ByteArrayOutputStream();
        //Marshals the JAXB bean into XML for validation
        // Root element is the name of the class
        if (javaObj == null) {
            try {
                resultingXML.write("null".getBytes());
            } catch (IOException ex) {
                log.warn("Could not write null to result : " + ex.toString());
            }
            return resultingXML;
        }
        String rootElement = javaObj.getClass().getName();
        rootElement = rootElement.substring(rootElement.lastIndexOf(".") + 1);

        try {
            getMarshaller(javaObj.getClass()).marshal(new JAXBElement(new QName(namespace, rootElement), javaObj.getClass(), javaObj), resultingXML);

        } catch (JAXBException ex) {
            log.warn("Error marshaling Object to XML : " + ex.toString());
        }

        return resultingXML;

    }

}
