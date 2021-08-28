package com.smilecoms.commons.sca;

import com.smilecoms.commons.base.BaseUtils;
import java.io.InputStream;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.*;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

/**
 * Helper class to validate XML being sent to SCA against the SCA Schema
 * 
 * @author PCB
 */
public class SCAValidator {

    private static Schema schema;
    private static final String CLASS = SCAValidator.class.getName();
    private static final Logger log = LoggerFactory.getLogger(CLASS);
    private static boolean initialised = false;
    private static final ReentrantLock lock = new ReentrantLock();

    /**
     * Validate a object against the SCA schema and return the validation errors in a ValidationErrorCollector
     * 
     * @param scaObject The object to validate
     * @return A ValidationErrorCollector containing any validation errors. getErrorCount will be > 0 if there were any errors
     */
    public static ValidationErrorCollector validateSCAMessage(Object scaObject) {
        boolean mustvalidate = false;
        try {
            mustvalidate = BaseUtils.getBooleanPropertyFailFast("env.sca.must.validate", false);
        } catch (Exception e) {
            log.debug("Wont validate as we could not get property from sca to see if validation is required");
        }

        if (!mustvalidate) {
            return new ValidationErrorCollector();
        }

        if (log.isDebugEnabled()) {
            log.debug("ENTERING SCAValidator : validateSCAMessage");
        }

        // Only initialise successfully once
        if (!initialised && !lock.isLocked()) {
            lock.lock();
            try {
                if (!initialised) {
                    initialise();
                }
            } finally {
                lock.unlock();
            }
        }
        
        if (!initialised) {
            // dont validate if we cant
            return new ValidationErrorCollector();
        }

        ValidationErrorCollector errs = new ValidationErrorCollector();

        //Check for null
        if (scaObject == null) {
            log.warn("The data going to SCA is null! Reporting this as an exception");
            errs.addOwnError("Cannot send null data to SCA");
            return errs;
        }

        String objType = scaObject.getClass().getName();

        Validator validator = schema.newValidator();

        validator.setErrorHandler(errs);

        try {
            if (log.isDebugEnabled()) {
                log.debug("About to validate the XML going to SCA by marshalling object of type " + objType);
            }
            long s = System.currentTimeMillis();
            Source src = SCAMarshaller.marshalToSource(scaObject);
            validator.validate(src);
            if (log.isDebugEnabled()) {
                if (errs.getErrorCount() > 0) {
                    SCAMarshaller.logObject(scaObject, "XML That Failed Validation:");
                }
                log.debug("Finished validating. Validation took " + (System.currentTimeMillis() - s) + "ms");
            }
        } catch (Exception ex) {
            log.warn("Validation failed with fatal error: " + ex.toString());
            errs.addOwnError("Unknown error occured while validating data : " + ex.toString());
        }

        if (log.isDebugEnabled()) {
            log.debug("EXITING SCAValidator : validateSCAMessage. Validation picked up " + errs.getErrorCount() + " errors in the request");
        }
        return errs;
    }

    /**
     * Compile the SCA schema and cache the result. The cache is refreshed based on global.sca.schemarefreshsecs parameter
     */
    private static void initialise() {
        InputStream is = null;
        try {
            ClassLoader cldr = SCAValidator.class.getClassLoader();
            String schemaLocation = "SCASchema.xsd";
            log.debug("Compiling SCA Schema for validation - retrieving schema from " + schemaLocation);
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            is = cldr.getResourceAsStream(schemaLocation);
            Source source = new StreamSource(is);                        
            schema = factory.newSchema(source);
            initialised = true;
            log.debug("Finished compiling SCA Schema for validation.");
        } catch (Exception ex) {
            log.warn("Error initialising SCA Validator. Will try again on next call. " + ex.toString());
            initialised = false;
        } finally {
            try {
                is.close();
            } catch (Exception ex) {
            }
        }

    }
}
