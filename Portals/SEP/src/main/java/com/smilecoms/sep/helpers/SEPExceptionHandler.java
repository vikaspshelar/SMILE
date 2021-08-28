package com.smilecoms.sep.helpers;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.ExceptionManager;
import com.smilecoms.commons.sca.*;
import com.smilecoms.commons.stripes.InsufficientPrivilegesException;
import com.smilecoms.commons.stripes.SmileActionBean;
import java.io.IOException;
import java.util.Enumeration;
import javax.servlet.RequestDispatcher;

import org.slf4j.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.config.Configuration;
import net.sourceforge.stripes.controller.StripesConstants;
import net.sourceforge.stripes.controller.StripesRequestWrapper;
import net.sourceforge.stripes.exception.ExceptionHandler;
import net.sourceforge.stripes.util.CryptoUtil;

public class SEPExceptionHandler implements ExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SEPExceptionHandler.class.getName());

    @Override
    public void init(Configuration configuration) throws Exception {
    }

    @Override
    public void handle(Throwable e, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            //Dig deep boys, we got an error to find...
            Throwable underlying = e.getCause();
            while (underlying != null && underlying.getCause() != null) {
                underlying = underlying.getCause();
            }
            if (underlying == null) {
                //There is no deeper level error, use e
                underlying = e;
            }

            // If its a SCABusinessError, then we must get the source page resolution and display the error on that page in a friendly manner
            if (underlying instanceof SCABusinessError) {
                SCABusinessError sbe = (SCABusinessError) underlying;
                if (sbe.getErrorCode().equals(Errors.ERROR_CODE_SCA_ENTITY_NOT_AUTHORISED)) {
                    underlying = new InsufficientPrivilegesException("LIMITED DATA ACCESS");
                } else {
                    // First get source page. _sourePage parameter is encrypted by stripes so we need to decrypt it
                    String encryptedSourcePage = request.getParameter("_sourcePage");
                    String decryptedSourcePage = "/index.jsp";
                    if (encryptedSourcePage != null) {
                        decryptedSourcePage = CryptoUtil.decrypt(encryptedSourcePage);
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Displaying uncaught business error. _sourcePage to display error on is: " + decryptedSourcePage);
                    }
                    //Populate the errors into the stripes error mechanism
                    SmileActionBean actionBean = (SmileActionBean) request.getAttribute(StripesConstants.REQ_ATTR_ACTION_BEAN);
                    actionBean.embedErrorIntoStipesActionBeanErrorMechanism((SCABusinessError) underlying);
                    RequestDispatcher rd = request.getRequestDispatcher(decryptedSourcePage);
                    if (rd == null) {
                        throw new Exception("Caller got SCABusinessError " + underlying.toString() + " but we cannot get a dispatcher for the source page");
                    }
                    rd.forward(request, response);
                    return;
                }
            }

            //See if its due to insufficient priviledges
            if (underlying instanceof InsufficientPrivilegesException) {
                // Send to no permissions page
                SmileActionBean actionBean = (SmileActionBean) request.getAttribute(StripesConstants.REQ_ATTR_ACTION_BEAN);
                actionBean.getContext().getValidationErrors().clear();
                InsufficientPrivilegesException ipe = (InsufficientPrivilegesException) underlying;
                log.debug("InsufficientPrivilegesException: sending user to /error_pages/no_permissions.jsp");
                RequestDispatcher rd = request.getRequestDispatcher("/error_pages/no_permissions.jsp?resource=" + ipe.getResource());
                if (rd == null) {
                    throw new Exception("Caller got InsufficientPrivilegesException " + underlying.toString() + " but we cannot get a dispatcher");
                }
                rd.forward(request, response);
                return;
            }

            if (underlying instanceof net.sourceforge.stripes.exception.SourcePageNotFoundException) {
                // Send to no permissions page
                log.warn("SourcePageNotFoundException: sending user to /index.jsp");
                response.sendRedirect(response.encodeRedirectURL(request.getContextPath() + "/index.jsp"));
                return;
            }

            if (underlying instanceof com.smilecoms.commons.stripes.DoublePostException) {
                // Send to no permissions page
                log.warn("DoublePostException");
                RequestDispatcher rd = request.getRequestDispatcher("/error_pages/double_post.jsp");
                if (rd == null) {
                    throw new Exception("Caller got DoublePostException " + underlying.toString() + " but we cannot get a dispatcher");
                }
                rd.forward(request, response);
                return;
            }

            String errDesc;
            String errCode = "NA";
            String errType = "NA";
            String errTrace = "";
            String errSCARequest = "NA";

            //Populate stack trace
            StackTraceElement[] stUnder = underlying.getStackTrace();
            // Only show first 10 lines
            for (int i = 0; i < stUnder.length && i < 10; i++) {
                errTrace += stUnder[i] + "\n";
            }

            //Other info
            if (underlying instanceof SCASystemError) {
                //Its a uSCA Err
                errCode = ((SCAErr) underlying).getErrorCode();
                errDesc = ((SCAErr) underlying).getErrorDesc();
                errType = SCAConstants.SYSTEM_ERROR;
                errSCARequest = ((SCAErr) underlying).getRequest();
            } else {
                //Its some other error
                errDesc = underlying.toString();
            }

            log.warn("######################## PORTAL ERROR ########################");
            log.warn("Error Description: " + errDesc + "\n");
            log.warn("Error Code: " + errCode + "\n");
            log.warn("Error Type: " + errType + "\n");
            log.warn("Error Trace:\n" + errTrace + "\n");
            if (!errSCARequest.equalsIgnoreCase("NA")) {
                log.warn("SCA Request that caused failure:\n" + errSCARequest + "\n");
            }
            logRequestDetail(request);
            log.warn("##############################################################");

            request.setAttribute("error_desc", errDesc);
            request.setAttribute("error_code", errCode);
            request.setAttribute("error_type", errType);
            request.setAttribute("error_trace", errTrace);
            request.setAttribute("error_scarequest", errSCARequest.replaceAll("<", "&lt;").replaceAll(">", "&gt;"));

            // Clear out any errors so they dont try and appear in the /layout/errors.jsp page
            ActionBean bean = (ActionBean) request.getAttribute(StripesConstants.REQ_ATTR_ACTION_BEAN);
            if (bean != null) {
                bean.getContext().getValidationErrors().clear();
            }

            if (!(underlying instanceof SCAErr)) {
                // If it was a sca error then it would have been reported already by the platform
                ExceptionManager em = new ExceptionManager(this.getClass().getName());
                em.reportError(e);
            }

            StripesRequestWrapper srw = null;
            try {
                srw = StripesRequestWrapper.findStripesWrapper(request);
            } catch (Exception se) {
            }
            if (srw != null) {
                log.debug("Forwarding to show_handled_error.jsp");
                try {
                    request.getRequestDispatcher("/error_pages/show_handled_error.jsp").forward(request, response);
                } catch (Exception ex) {
                    log.warn("Error: ", ex);
                    try {
                        response.getWriter().println("Error Description: " + errDesc);
                        response.getWriter().println("Error Code: " + errCode);
                        response.getWriter().println("Error Type: " + errType);
                        response.flushBuffer();
                        log.error("This request could not be processed on sep_show_handled_error, so the pretty error page could not be displayed");
                    } catch (IllegalStateException is) {
                        log.error("Cannot write any data back to the client. Nothing more we can do [{}]", is.getMessage());
                    }
                }
            } else {
                response.getWriter().println("Error Description: " + errDesc);
                response.getWriter().println("Error Code: " + errCode);
                response.getWriter().println("Error Type: " + errType);
                response.flushBuffer();
                log.error("This request did not go through the stripes request wrapper, so the pretty error page could not be displayed");
            }

        } catch (Exception mainErr) {
            log.error("Error dealing with exception. There is nothing more we can do", mainErr);
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "SEP", "Error processing exception in Exception Handler: " + mainErr.toString());
            response.sendRedirect(response.encodeRedirectURL(request.getContextPath() + "/index.jsp"));
        }
    }

    private void logRequestDetail(HttpServletRequest httpReq) {
        try {
            String requestString = httpReq.getRequestURI() + (httpReq.getQueryString() == null ? "" : ("?" + httpReq.getQueryString()));
            log.warn("Request is " + requestString + ". Requesting customer is " + httpReq.getRemoteUser());
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
            String sessionLocation = "<not passed in>";
            String valid = "invalid.";
            if (httpReq.isRequestedSessionIdValid()) {
                valid = "valid.";
            }
            if (httpReq.isRequestedSessionIdFromCookie()) {
                sessionLocation = "cookie";
            }
            if (httpReq.isRequestedSessionIdFromURL()) {
                sessionLocation = "URL";
            }
            log.warn("The session id passed in was in a " + sessionLocation + " and was: " + httpReq.getRequestedSessionId() + ". Its " + valid);
        } catch (Exception e) {
            log.warn("Error logging request: ", e);
        }
    }
}
