
package com.smilecoms.commons.sca;

/**
 *
 * @author PCB
 */
public class Errors {

    // SCA Delegate Errors
    public static final String ERROR_CODE_SCA_NOTUP = "SCAD-0000";
    public static final String ERROR_CODE_SCA_NULLIN = "SCAD-0001";
    public static final String ERROR_CODE_SCA_NULLOUT = "SCAD-0002";
    public static final String ERROR_CODE_SCA_INVALID_MESSAGE = "SCAD-0003";
    public static final String ERROR_CODE_SCA_INVALID_METHOD = "SCAD-0004";
    public static final String ERROR_CODE_SCA_UNKNOWN = "SCAD-0005";
    public static final String ERROR_CODE_SCA_NO_RESULT = "SCAD-0007";
    // SCA Delegate message validation
    public static final String ERROR_CODE_SCHEMA_INIT_FAILED = "SCAD-0006";
     
    //Unknown
    public static final String ERROR_CODE_UNKNOWN = "UNKN-0000";
    
    // Entity Authorisation
    public static final String ERROR_CODE_SCA_ENTITY_NOT_AUTHORISED = "SCAD-0008";
    public static final String ERROR_CODE_CANNOT_POST_OBVISCATED_DATA = "SCAD-0009";
    public static final String ERROR_CODE_SUSPICIOUS_XSS = "SCAD-0010";
}
