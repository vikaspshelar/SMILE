
package com.smilecoms.commons.sca;



/**
 * An Exception class to store error info returned when calling SCA. The embedded error info in SCAError_Exception
 * is put into this object for simpler access
 * 
 * @author PCB
 */
public class SCABusinessError extends SCAErr {

    public SCABusinessError() {
        super(SCAConstants.BUSINESS_ERROR);
    }
    
    public SCABusinessError(Throwable cause) {
        super(cause, SCAConstants.BUSINESS_ERROR);
    }
}