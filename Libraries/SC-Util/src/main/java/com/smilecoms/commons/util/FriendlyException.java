package com.smilecoms.commons.util;

import com.smilecoms.commons.base.BaseUtils;

/**
 *
 * @author PCB
 */
public class FriendlyException {        
    
    // Main exception
    protected String topLevelConfiguredErrorCode;
    protected String topLevelConfiguredDescription;
    protected String topLevelConfiguredSeverity;
    protected String topLevelConfiguredErrorType;    
    protected String topLevelConfiguredResolution;   
    protected String topLevelConfiguredAction;
    protected String topLevelErrorMessage;
    protected String topLevelExceptionClassName;
    protected String topLevelMethodName;
    protected String topLevelClassName;   
    protected int topLevelLineNumber;
    protected boolean topLevelConfiguredMustRollback;
    protected Throwable topLevelException;
    // Deepest level cause
    protected Throwable rootCauseException;
    protected String rootCauseTechnicalDescription;
    protected String rootCauseMethodName;
    protected String rootCauseClassName;
    protected String rootCauseExceptionClassName;
    protected int rootCauseLineNumber;


    protected String inputMsg;
    
    public String getLogLine() {
        return getErrorMessage();
    }
    
    public String getErrorDesc() {
        return getErrorMessage();
    }
    
    public String getErrorType() {
        return topLevelConfiguredErrorType;
    }
    
    public String getErrorCode() {
        return topLevelConfiguredErrorCode;
    }
    
    public Throwable getCause() {
        return rootCauseException;
    }
    
    public boolean getMustRollback() {
        return topLevelConfiguredMustRollback;
    }
    
    private String getErrorMessage() {
        String txDesc;
        if (this.topLevelConfiguredMustRollback) {
            txDesc = "be rolled back.";
        } else {
            txDesc = "not be rolled back.";
        }
        String main =  "ERROR MESSAGE[Error of type " + this.topLevelExceptionClassName + " was thrown in " + this.topLevelClassName + "." + this.topLevelMethodName + "(line " + this.topLevelLineNumber + 
                ") with description (" + this.topLevelErrorMessage +  ") on server instance " + BaseUtils.FQ_SERVER_NAME + ". Its error configuration describes it as (" + this.topLevelConfiguredDescription + ") having Error Code:" + this.topLevelConfiguredErrorCode +
                ". Its a " + this.topLevelConfiguredErrorType + " error type and any active transactions must " + txDesc; 

        String under = "";
        if (this.rootCauseException != null) {
            under = " The underlying cause of type " + this.rootCauseExceptionClassName + " was thrown in " + this.rootCauseClassName + "." + this.rootCauseMethodName + "(line " + this.rootCauseLineNumber +
                ") with description (" + this.rootCauseTechnicalDescription + ")";
        }
        return main + under + "]" +  " The configured resolution is: " + this.topLevelConfiguredResolution;
    }
    
    public String getTrapMessage() {
        String main =  "Error of type " + this.topLevelExceptionClassName + " was thrown in " + this.topLevelClassName + "." + this.topLevelMethodName + "(line " + this.topLevelLineNumber + 
                ") with description (" + this.topLevelErrorMessage + ") on server instance " + BaseUtils.FQ_SERVER_NAME + ". Its error configuration describes it as (" + this.topLevelConfiguredDescription + ") having Error Code:" + this.topLevelConfiguredErrorCode +
                ". Its a " + this.topLevelConfiguredErrorType + " error type.";

        String under = "";
        if (this.rootCauseException != null) {
            under = " The underlying cause of type " + this.rootCauseExceptionClassName + " was thrown in " + this.rootCauseClassName + "." + this.rootCauseMethodName + "(line " + this.rootCauseLineNumber +
                ") with description (" + this.rootCauseTechnicalDescription + ")";
        }
        if (inputMsg != null) {
            under += " Request Message:[" + inputMsg + "]";
        }
        return main + under +  " The configured resolution is: " + this.topLevelConfiguredResolution;
    }
}
