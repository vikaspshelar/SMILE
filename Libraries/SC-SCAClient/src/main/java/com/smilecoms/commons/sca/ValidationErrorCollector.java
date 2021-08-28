 
package com.smilecoms.commons.sca;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.*;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Stores the results of a schema validation
 * @author PCB
 */
public class ValidationErrorCollector implements ErrorHandler  {
    
    private static final String CLASS = ValidationErrorCollector.class.getName();
    private static final Logger log =  LoggerFactory.getLogger(CLASS);
    private final List parseErrors = new ArrayList();
    private static final String ELEMENT_DEMARCATOR = "of element '";
    private static final String SINGLE_QUOTE = "'";
    private static final String ERROR_DELIMETER = "|";
    private static final String VALIDATION_DOT = "validation.error.";
    private static final int ELEMENT_DEMARCATOR_LENGTH = ELEMENT_DEMARCATOR.length();
    
    @Override
    public void warning(SAXParseException exception) throws SAXException {
        addError(exception.getMessage());
    }
    
    public void addOwnError(String msg) {
        addError(msg);
    }
    @Override
    public void error(SAXParseException exception) throws SAXException {
        addError(exception.getMessage());
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
        throw exception;
    }
            
    public List getCollectedParseExceptions() {
        return parseErrors;
    }

    @SuppressWarnings("unchecked")
    private void addError(String exceptionMsg) {  
        String simplified = simplify(exceptionMsg);
        if (simplified != null) {
            parseErrors.add(simplified);
        }
        
    }
    
    public int getErrorCount() {        
        return parseErrors.size();
    }
    
    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append(SCAConstants.VALIDATION_ERRORS);
        String exception;
        Iterator it = parseErrors.iterator();
        while (it.hasNext()) {
            exception = (String)it.next();
            ret.append(exception).append("\n");            
        }
        return ret.toString();
    }
    
    /**
     * Returns the validation errors in a friendly list format
     * 
     * @return String containing HTML of validation errors in a list using li tags
     */
     
    @SuppressWarnings("unchecked")
    public String getAllMessages() {
        StringBuilder ret = new StringBuilder();
        Iterator it = parseErrors.iterator();
        while (it.hasNext()) {
            ret.append((String)it.next());
            if (it.hasNext()) {
                ret.append(ERROR_DELIMETER);
            }
        }        
        return ret.toString();
    }

    /**
     * Gets rid of nasty stuff in the validation messages to make it more readable by users
     * 
     * @param msg The nasty validation message
     * @return A nicer message
     */
    private String simplify(String msg) {
        if (log.isDebugEnabled()) {
            log.debug("Unsanitised validation error: " + msg);
        }
        try {
            // We want to get the element name that failed validation
            int elementStart = msg.indexOf(ELEMENT_DEMARCATOR);
            if (elementStart == -1) {
                return null;
            }
            msg = msg.substring(elementStart + ELEMENT_DEMARCATOR_LENGTH);
            msg = msg.toLowerCase();
            int elementEnd = msg.indexOf(SINGLE_QUOTE);
            msg = VALIDATION_DOT + msg.substring(0,elementEnd);               
            if (log.isDebugEnabled()) {
                log.debug("Sanitised validation error: " + msg);
            }            
        } catch (Exception e) {
            log.debug("Error santising SAX parse error: " + e.toString());
            return null;
        }
        return msg;
    }
}
