
package com.smilecoms.commons.sca;


/**
 * An Exception class to store error info returned when calling SCA. The embedded error info in SCAError_Exception
 * is put into this object for simpler access
 * 
 * @author PCB
 */
public class SCASystemError extends SCAErr {
    
    public SCASystemError() {
        super(SCAConstants.SYSTEM_ERROR);
    }
    
    public SCASystemError(Throwable cause) {
        super(cause, SCAConstants.SYSTEM_ERROR);
    }
}
