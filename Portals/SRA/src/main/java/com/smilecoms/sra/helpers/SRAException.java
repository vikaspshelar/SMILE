/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.smilecoms.commons.sca.SCABusinessError;
import com.smilecoms.commons.sca.SCAConstants;
import com.smilecoms.commons.sca.SCAErr;
import com.smilecoms.commons.sca.SCASystemError;
import com.smilecoms.commons.util.ExceptionManager;
import com.smilecoms.commons.util.Utils;
import java.io.IOException;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SRAException extends WebApplicationException {

    public static final String BUSINESS_ERROR = SCAConstants.BUSINESS_ERROR;
    public static final String SYSTEM_ERROR = SCAConstants.SYSTEM_ERROR;
    public static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(SRAException.class);

    static {
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true);
    }

    public SRAException(Throwable t) {
        super(Response.status(Response.Status.BAD_REQUEST)
                .entity(new SRAError(t))
                .type(MediaType.APPLICATION_JSON)
                .build());
    }

    public SRAException(String message, String type) {
        super(Response.status(Response.Status.BAD_REQUEST)
                .entity(new SRAError(message, type))
                .type(MediaType.APPLICATION_JSON)
                .build());
    }
    
    public SRAException(String message, String errorType, String errorCode, Response.Status httpStatusCode) throws IOException {
        super(Response.status(httpStatusCode)
                .entity(getErrorAsJSON(message, errorType, errorCode))
                .type(MediaType.APPLICATION_JSON)
                .build());
    }

    public SRAException(Status status) {
        super(Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity("{}")
                .build());
    }

    public static String getErrorAsJSON(String message, String type, String code) throws IOException {
        SRAError err = new SRAError(message, type, code);
        return mapper.writeValueAsString(err);
    }

}

class SRAError {

    private static final Logger log = LoggerFactory.getLogger(SRAError.class);
    private String errorDesc;
    private String errorType;
    private String errorCode;
    private String errorTrace;
    private String SCARequest;

    SRAError(String errorMessage, String errorType, String errorCode) {
        this.errorDesc = errorMessage;
        this.errorType = errorType;
        this.errorCode = errorCode;
    }

    SRAError(String errorMessage, String errorType) {
        this.errorDesc = errorMessage;
        this.errorType = errorType;
        this.errorCode = "SRA-0001";
    }

    SRAError(Throwable t) {
        Throwable underlying = Utils.getDeepestCause(t);
        if (underlying instanceof SCAErr) {
            SCAErr scaErr = (SCAErr) underlying;
            initialiseforSCAError(scaErr);
        } else if (underlying instanceof InsufficientPrivilegesException) {
            errorDesc = underlying.toString();
            errorType = SRAException.BUSINESS_ERROR;
            errorCode = "SRA-0004";
            SCARequest = "NA";
        } else {
            errorDesc = underlying.toString();
            errorType = SRAException.SYSTEM_ERROR;
            errorCode = "SRA-0000";
            SCARequest = "NA";
        }

        if (errorType.equals(SRAException.SYSTEM_ERROR)) {
            errorTrace = "";
            //Populate stack trace
            StackTraceElement[] stUnder = underlying.getStackTrace();
            // Only show first 10 lines
            for (int i = 0; i < stUnder.length && i < 10; i++) {
                errorTrace += stUnder[i] + "\n";
            }

            log.warn("######################## SRA ERROR ########################");
            log.warn("Error Description: " + errorDesc + "\n");
            log.warn("Error Code: " + errorCode + "\n");
            log.warn("Error Type: " + errorType + "\n");
            log.warn("Error Trace:\n" + errorTrace + "\n");
            if (!SCARequest.equals("NA")) {
                log.warn("SCA Request that caused failure:\n" + SCARequest + "\n");
            }
            try {
                logRequestDetail(Token.getRequestsToken().getRequestsRequest());
            } catch (Exception e) {
                log.warn("Cannot get the request to log [{}]", e.getMessage());
            }
            log.warn("##############################################################");
        }
        
        if (!(underlying instanceof SCAErr)) {
            // If it was a sca error then it would have been reported already by the platform
            ExceptionManager em = new ExceptionManager(this.getClass().getName());
            em.reportError(t);
        }

    }

    public String getErrorDesc() {
        return errorDesc;
    }

    public void setErrorDesc(String errorDesc) {
        this.errorDesc = errorDesc;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    private void logRequestDetail(HttpServletRequest httpReq) {
        if (httpReq == null) {
            log.warn("HTTP Request is null so nothing to log");
            return;
        }
        try {
            String requestString = httpReq.getRequestURI() + (httpReq.getQueryString() == null ? "" : ("?" + httpReq.getQueryString()));
            log.warn("Request is " + requestString + ". Requesting customer is " + Token.getRequestsToken().getUsername());
            Enumeration e = httpReq.getParameterNames();
            log.warn("-- Start Form Elements --");
            while (e.hasMoreElements()) {
                String name = (String) e.nextElement();
                log.warn(name + " : " + httpReq.getParameter(name));
            }
            log.warn("--  End Form Elements  --\n");

            e = httpReq.getHeaderNames();
            log.warn("-- Start Headers --");
            while (e.hasMoreElements()) {
                String name = (String) e.nextElement();
                log.warn(name + " : " + httpReq.getHeader(name));
            }
            log.warn("--  End Headers  --");
        } catch (Exception e) {
            log.warn("Error logging request: ", e);
        }
    }

    private void initialiseforSCAError(SCAErr scaError) {
        if (scaError instanceof SCABusinessError) {
            errorType = SCAConstants.BUSINESS_ERROR;
            SCARequest = "NA";
        } else if (scaError instanceof SCASystemError) {
            errorType = SCAConstants.SYSTEM_ERROR;
            SCARequest = scaError.getRequest();
        }
        errorDesc = scaError.getErrorDesc();
        errorCode = scaError.getErrorCode();
    }

}
