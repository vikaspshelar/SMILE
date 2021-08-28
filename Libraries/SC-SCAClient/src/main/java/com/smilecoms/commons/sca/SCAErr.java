
package com.smilecoms.commons.sca;

/**
 * An Exception class to store error info returned when calling SCA. The embedded error info in SCAError_Exception
 * is put into this object for simpler access
 * 
 * @author PCB
 */
public abstract class SCAErr extends RuntimeException {
    private String sErrorCode = "";
    private String sErrorDesc = "";
    private String sMessage = "";
    private String sErrorType = null;
    
    public SCAErr(Throwable cause, String type) {
        super(cause);
        sErrorType = type;
    }
    public SCAErr(String type) {
        super();
        sErrorType = type;
    }

    public String getRequest() {
        return sMessage;
    }

    public void setRequest(String message) {
        this.sMessage = message;
    }

    public String getErrorCode() {
        return sErrorCode;
    }

    public void setErrorCode(String sErrorCode) {
        this.sErrorCode = sErrorCode;
    }

    public String getErrorDesc() {
        return sErrorDesc;
    }

    public void setErrorDesc(String sErrorDesc) {
        this.sErrorDesc = sErrorDesc;
        
    }    
    
    /**
     * Get the stack trace in a nice String format
     *
     * @return String Stack trace as a String
     */
    public String getStackTraceString() {
        StackTraceElement[] st = this.getStackTrace();
        
        int l = st.length;
        StringBuilder ret = new StringBuilder();
        for (int i=1;i<l;i++) {
            ret.append(st[i]).append("\n");
        }
        return ret.toString();
    }
    
    /**
     * Override toString to make it nicer
     *
     * @return String error information laid out nicely in format "Type [" + sErrorType + "] Code [" + sErrorCode + "] Desc [" + sErrorDesc + "]"
     */
    @Override
    public String toString() {
        return "Type [" + sErrorType + "] Code [" + sErrorCode + "] Desc [" + sErrorDesc + "] Request [" + sMessage + "]";
    }

    @Override
    public String getMessage() {
        return toString();
    }
    
    
    
}
