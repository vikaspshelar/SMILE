
package com.smilecoms.tpgw.api;

public class TPGWApiError extends Exception {

    protected String errorDesc;
    protected String errorType;
    protected String errorCode;
    
    public String getErrorDesc() {
        return errorDesc;
    }
    
    public void setErrorDesc(String value) {
        this.errorDesc = value;
    }
   
    public String getErrorType() {
        return errorType;
    }
   
    public void setErrorType(String value) {
        this.errorType = value;
    }
   
    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String value) {
        this.errorCode = value;
    }
}
