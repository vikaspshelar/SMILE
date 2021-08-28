/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class SmileRequestWrapper extends HttpServletRequestWrapper {

    
    private static final Logger log = LoggerFactory.getLogger(SmileRequestWrapper.class);
    HttpServletRequest request = null;

    public SmileRequestWrapper(HttpServletRequest request) {
        super(request);
        this.request = request;
    }
    
    

}
