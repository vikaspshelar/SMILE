/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.smilecoms.commons.sca;

/**
 *
 * @author paul
 */
public class SCAConstants {

    public static final String SCA_CLIENT_ID = com.smilecoms.commons.base.BaseUtils.SCA_CLIENT_ID;
    public static final String SCA_HOST_PASSED_IN = com.smilecoms.commons.base.BaseUtils.SCA_BOOTSTRAP_HOST;
    public static final String SYSTEM_ERROR = "system";
    public static final String BUSINESS_ERROR = "business";
    public static final String NULL = "null";
    public static final String INTEGER = "java.lang.Integer";
    public static final String STRING = "java.lang.string";
    public static final String SCA_NAMESPACE = "http://xml.smilecoms.com/SCA";
    public static final String SCA_SCHEMA_NAMESPACE = "http://xml.smilecoms.com/schema/SCA";
    public static final String VALIDATION_ERRORS = "\nError\n";
    public static final String SCA_ENDPOINT_KEY = "env.sca.endpoints";
    public static final String CONNECT_TIMEOUT_KEY = "global.sca.timeout.connectmillis";
    public static final String DEFAULT_CONNECT_TIMEOUT = "10000";
    public static final String REQUEST_TIMEOUT_KEY = "global.sca.timeout.responsemillis";
    public static final String DEFAULT_REQUEST_TIMEOUT = "40000";
    public static final String MUST_PRINT_STACKTRACE_KEY = "env.exceptions.printstacktrace";
    public static final String DEFAULT_MUST_PRINT_STACKTRACE = "false";
            
}
