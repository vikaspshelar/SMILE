package com.smilecoms.sra.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.AccountQuery;
import com.smilecoms.commons.sca.CallersRequestContext;
import com.smilecoms.commons.sca.CampaignData;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.CustomerCommunicationData;
import com.smilecoms.commons.sca.CustomerQuery;
import com.smilecoms.commons.sca.Event;
import com.smilecoms.commons.sca.EventList;
import com.smilecoms.commons.sca.EventQuery;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceInstanceList;
import com.smilecoms.commons.sca.ServiceInstanceQuery;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.StServiceInstanceLookupVerbosity;
import com.smilecoms.commons.sca.beans.CustomerBean;
import com.smilecoms.commons.util.ExceptionManager;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.sra.model.OTPNotification;
import com.smilecoms.sra.model.SMSMessage;
import com.smilecoms.sra.model.RechargeOffer;
import com.smilecoms.sra.model.RegisterCustomerRequest;
import com.smilecoms.sra.model.SmileCustomer;
import com.smilecoms.sra.model.PaymentGateway;
import com.smilecoms.sra.model.OTPVerificationRequest;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response; 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.smilecoms.commons.sca.ShortMessage;
import com.smilecoms.commons.sca.StAccountLookupVerbosity;
import java.util.ArrayList;
import java.util.List;

public class SRAFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(SRAFilter.class);
    
    public static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        try {

            boolean allOk = isAllOk();

            if (httpRequest.getRequestURI().startsWith(httpRequest.getContextPath() + "/isup") && allOk) {
                response.setContentType(MediaType.TEXT_PLAIN);
                response.getWriter().print("<Done>true</Done>");
                return;
            }

            if (httpRequest.getRequestURI().contains("/forgetpassword")){
                    sendResetPasswordLink(request, response);
                    return;       
            }
            if (httpRequest.getRequestURI().contains("/registerbyotp")){
                    registerUser(request, response,true);
                    return;       
            }
            if (httpRequest.getRequestURI().contains("/changepwdbyotp")){
                    registerUser(request, response,false);
                    return;       
            }
            if (httpRequest.getRequestURI().contains("/otpnotification")){
                    otpNotificaton(request, response);
                    return;       
            }
            
            if (httpRequest.getRequestURI().contains("/sendotp")){
                    sendOTPMessage(request, response);
                    return;       
            }
            
            if(httpRequest.getRequestURI().contains("/smilecustomer")){
                getSmileCustomer(request, response);
                return;
            }
            
            if (httpRequest.getRequestURI().contains("/offer/create")){
                    createRechargeOffer(request, response);
                    return;       
            }
            if (httpRequest.getRequestURI().contains("/paymentgateway")){
                    getPaymentGateway(request, response);
                    return;       
            }
            
             if (httpRequest.getRequestURI().contains("/account/getCustomerId")){
                    retrieveAccountCustomerId(request, response);
                    return;       
            }
             
            if (httpRequest.getRequestURI().contains("/verifyotp")){
                    verifyOTP(request, response);
                    return;       
            }
            
            if (!allOk) {
                HttpServletResponse resp = (HttpServletResponse) response;
                resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                return;
            }

            // Bit of a hack to put tiny URL functionality here but it works
            if (httpRequest.getRequestURI().startsWith(httpRequest.getContextPath() + "/tiny/")) {
                String tinyURL = httpRequest.getParameter("t");
                String longURL = Utils.lengthenURL(tinyURL);
                log.debug("Long URL for [{}] is [{}]", tinyURL, longURL);
                if (longURL == null) {
                    httpResponse.sendRedirect("https://" + BaseUtils.getProperty("env.portal.url"));
                } else {
                    httpResponse.sendRedirect(Utils.lengthenURL(tinyURL));
                }
                return;
            }

            Token token = new Token(httpRequest);
            if (token.isBrandNew()) {
                response.setContentType(MediaType.APPLICATION_JSON);
                response.getWriter().print(token.getJSON());
                return;
            }
            SCAWrapper.getUserSpecificInstance().setThreadsRequestContext(new CallersRequestContext(token.getUsername(), token.getOriginatingIP(), 0, token.getGroups()));

            if (log.isDebugEnabled()) {
                logRequestDetail((HttpServletRequest) request, Level.FINEST);
            }

            chain.doFilter(request, response);

        } catch (TokenException te) {
            try {
                log.warn("Token exception", te);
                httpResponse.setStatus(Status.UNAUTHORIZED.getStatusCode());
                httpResponse.setContentType(MediaType.APPLICATION_JSON);
                httpResponse.getWriter().print(SRAException.getErrorAsJSON(te.getMessage(), SRAException.BUSINESS_ERROR, "SRA-0003"));
            } catch (IOException e) {
                log.warn("Error dealing with error in filter", e);
            }
        } catch (UserLockedException ulex){
            log.warn("Token UserLockedException", ulex);
            httpResponse.setStatus(Status.UNAUTHORIZED.getStatusCode());
            httpResponse.setContentType(MediaType.APPLICATION_JSON);
            httpResponse.getWriter().print(SRAException.getErrorAsJSON(ulex.getMessage(), SRAException.BUSINESS_ERROR, "SRA-0027"));
        }
        catch (Exception ex) {
            try {
                log.warn("Unhandled Error", ex);
                httpResponse.setStatus(Status.INTERNAL_SERVER_ERROR.getStatusCode());
                httpResponse.setContentType(MediaType.APPLICATION_JSON);
                httpResponse.getWriter().print(SRAException.getErrorAsJSON(ex.toString(), SRAException.BUSINESS_ERROR, "SRA-0002"));
                ExceptionManager em = new ExceptionManager(getClass().getName());
                em.reportError(ex);
            } catch (IOException e) {
                log.warn("Error dealing with error in filter", e);
            }
        } finally {
            SCAWrapper.removeThreadsRequestContext();
        }
        log.debug("Out SRA doFilter for URL [{}]", httpRequest.getRequestURL());
    }

    private boolean isAllOk() {
        boolean ret = true;
        if (!BaseUtils.isSCAAvailable() || !BaseUtils.isPropsAvailable()) {
            log.warn("This server has no SCA endpoint available or property framework is not available. Server will report itself as being down");
            ret = false;
        }
        return ret;
    }

    private void logRequestDetail(HttpServletRequest httpReq, Level l) {
        try {
            String requestString = httpReq.getRequestURI() + (httpReq.getQueryString() == null ? "" : ("?" + httpReq.getQueryString()));
            log("Request is " + requestString + ". Requesting customer is " + httpReq.getRemoteUser() + " at IP " + Utils.getRemoteIPAddress(httpReq), l);
            Enumeration e = httpReq.getParameterNames();
            log("-- Start Form Elements --", l);
            while (e.hasMoreElements()) {
                String name = (String) e.nextElement();
                log(name + " : " + httpReq.getParameter(name), l);
            }
            log("--  End Form Elements  --\n", l);

            e = httpReq.getHeaderNames();
            log("-- Start Headers --", l);
            while (e.hasMoreElements()) {
                String name = (String) e.nextElement();
                log(name + " : " + httpReq.getHeader(name), l);
            }
            log("--  End Headers  --", l);

        } catch (Exception e) {
            log.warn("Error logging request: ", e);
        }
    }

    private void log(String msg, Level l) {
        if (l.equals(Level.FINEST)) {
            log.debug(msg);
        } else if (l.equals(Level.WARNING)) {
            log.warn(msg);
        } else if (l.equals(Level.SEVERE)) {
            log.error(msg);
        }
    }

    private void sendResetPasswordLink(ServletRequest request, ServletResponse response) {
        String identity = request.getParameter("identity");
        log.debug("sending reset password link to [{}]", identity);
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setContentType(MediaType.APPLICATION_JSON);
        if (identity == null || identity.isEmpty()) {
            try {
                httpResponse.setStatus(Status.BAD_REQUEST.getStatusCode());
                httpResponse.getWriter().print(SRAException.getErrorAsJSON("username or emai address is not provided", SRAException.BUSINESS_ERROR, "SRA-0047"));
                return;
            } catch (IOException ex) {
                log.error("Exception: ", ex);
            }
        }
        try {
            CustomerBean.sendPasswordResetLink(identity);
            httpResponse.setStatus(Status.OK.getStatusCode());
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().print(mapper.writeValueAsString(new Done()));
        } catch (Exception ex) {
            httpResponse.setStatus(Status.NOT_FOUND.getStatusCode());
            try {
                httpResponse.getWriter().print(SRAException.getErrorAsJSON("Invalid username or email address", SRAException.BUSINESS_ERROR, "SRA-0082"));
            } catch (IOException e) {
                log.error("Exception: ", e);
            }
            log.error("Exception: ", ex);
        }
    }
    
    private void registerUser(ServletRequest request, ServletResponse response, boolean isRegistration) {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setContentType(MediaType.APPLICATION_JSON);
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        if(!httpRequest.getMethod().equals(HttpMethod.POST)){
            httpResponse.setStatus(Status.BAD_REQUEST.getStatusCode());
            return;
        }
        RegisterCustomerRequest requestBody = null;
        try {
            requestBody = mapper.readValue(request.getInputStream(), RegisterCustomerRequest.class);
        } catch (IOException ex){
            log.error("error :",ex);
        }
        
        if(requestBody == null || requestBody.getCustomerId() < 0 || requestBody.getIdentity() == null
                || requestBody.getKey() == null) {
            httpResponse.setStatus(Status.BAD_REQUEST.getStatusCode());
            return;
        }
        
        if(requestBody.getNewPassword() == null || !requestBody.getNewPassword().equals(requestBody.getConfirmPassword())) {
            //check for guest registration
            if(requestBody.getCustomerId() == 0 && verifyOTP(requestBody.getIdentity(), requestBody.getKey())){
               addGuestCustomer(requestBody);
               httpResponse.setStatus(Status.OK.getStatusCode());
               try {
                   httpResponse.getWriter().print(mapper.writeValueAsString(new Done()));
                   log.debug("guest with contact number [{}] registered using OTP", requestBody.getIdentity());
                   return;
               }catch(IOException ex){
                   log.error("customer registration error",ex);
               }
            } else {
               log.debug("validation of guest customer failed");
               httpResponse.setStatus(Status.BAD_REQUEST.getStatusCode()); 
            }
            return;
        }
        int customerId = requestBody.getCustomerId();
        
        Customer customer = null;
        CustomerQuery customerQuery = new CustomerQuery();
        customerQuery.setResultLimit(1);
        customerQuery.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        customerQuery.setCustomerId(customerId);
        try {
            customer = SCAWrapper.getAdminInstance().getCustomer(customerQuery);
        } catch (Exception ex) {
            log.debug("Error getting user by email address for login: ", ex);
        } 
        if(customer == null) {
            try {
                httpResponse.setStatus(Status.NOT_FOUND.getStatusCode());
                httpResponse.getWriter().print(SRAException.getErrorAsJSON("invalid identity", SRAException.BUSINESS_ERROR, "SRA-0093"));
            } catch (IOException ex){
                log.error("invalid identity",ex);
            }
            return;
        }
        
        //check if customer is already registered with mysmile.
        if(!customer.getSSODigest().isEmpty() && isRegistration){
            try {
                httpResponse.setStatus(Status.BAD_REQUEST.getStatusCode());
                httpResponse.getWriter().print(SRAException.getErrorAsJSON("This username is already registered. Please try again.", SRAException.BUSINESS_ERROR, "SRA-0013"));
            } catch (IOException ex){
                log.error("This username is already registered. Please try again.",ex);
            }
            return;
        }
        
        //boolean isValidRegistration =  validateRegistrationReq(requestBody);
        boolean isValidRegistration = verifyOTP(requestBody.getIdentity(), requestBody.getKey());
        if(!isValidRegistration){
            try {
            httpResponse.setStatus(Status.BAD_REQUEST.getStatusCode());
            httpResponse.getWriter().print(SRAException.getErrorAsJSON("invalid request", SRAException.BUSINESS_ERROR, "SRA-0005"));
            } catch (IOException ex){
                log.error("invalid request",ex);
            }
            return;
        }

        try {
            setPassword(customer,requestBody);
            httpResponse.setStatus(Status.OK.getStatusCode());
            httpResponse.getWriter().print(mapper.writeValueAsString(new Done()));
        } catch (Exception ex) {
            try {
                httpResponse.setStatus(Status.INTERNAL_SERVER_ERROR.getStatusCode());
                httpResponse.getWriter().print(SRAException.getErrorAsJSON("internal server error", SRAException.BUSINESS_ERROR, "SRA-0006"));
            } catch (IOException e) {
                log.error("Exception: ", e);
            }
            log.error("registerUser Exception: ", ex);
        }
    }
   
    private void getSmileCustomer(ServletRequest request, ServletResponse response){
        String identity = request.getParameter("identity");
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setContentType(MediaType.APPLICATION_JSON);
        if (identity == null || identity.isEmpty()) {
            try {
                httpResponse.setStatus(Status.BAD_REQUEST.getStatusCode());
                httpResponse.getWriter().print(SRAException.getErrorAsJSON("username or emai address is not provided", SRAException.BUSINESS_ERROR, "SRA-0073"));
                return;
            } catch (IOException ex) {
                log.error("getSmileCustomer Exception: ", ex);
            }
        }
        
        CustomerQuery customerQuery = new CustomerQuery();
        customerQuery.setResultLimit(1);
        customerQuery.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        Customer customer = null;
        try {
            switch (SRAUtil.getAuthType(identity)) { 
            case "EMAIL":
                    customerQuery.setEmailAddress(identity);
                    customer = SCAWrapper.getAdminInstance().getCustomer(customerQuery);
                    break;
            case "PHONE":
                    //try {
                        //Fail if not SmileVoice Number
                        String publicIdentity = Utils.getCleanDestination(identity);

                        ServiceInstanceQuery siq = new ServiceInstanceQuery();
                        siq.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
                        siq.setIdentifierType("END_USER_SIP_URI");
                        siq.setIdentifier(Utils.getPublicIdentityForPhoneNumber(publicIdentity));
                        ServiceInstanceList sil = SCAWrapper.getAdminInstance().getServiceInstances(siq);

                        if (sil != null && sil.getNumberOfServiceInstances() > 0) {
                            customerQuery.setCustomerId(sil.getServiceInstances().get(0).getCustomerId());
                            customer = SCAWrapper.getAdminInstance().getCustomer(customerQuery);
                        }
                    break;
            case "IDENTITY_NUMBER":
                    customerQuery.setIdentityNumber(identity);
                    customer = SCAWrapper.getAdminInstance().getCustomer(customerQuery);
                    break;
                default:
                    log.debug("Going to use default username for authentication");
                    customerQuery.setSSOIdentity(identity);
                    customer = SCAWrapper.getAdminInstance().getCustomer(customerQuery);
            }
            if(customer == null){
                httpResponse.setStatus(Status.NOT_FOUND.getStatusCode());
                httpResponse.getWriter().print(SRAException.getErrorAsJSON("The SmileNumber/Username entered is invalid. Please try again  or contact Customer Care for assistance.", SRAException.BUSINESS_ERROR, "SRA-0053"));
                return;
            }
        } catch(Exception ex){
            try {
                log.error("error in getSmileCustomer:",ex);
                httpResponse.setStatus(Status.NOT_FOUND.getStatusCode());
                httpResponse.getWriter().print(SRAException.getErrorAsJSON("The SmileNumber/Username entered is invalid. Please try again  or contact Customer Care for assistance.", SRAException.BUSINESS_ERROR, "SRA-0053"));
            } catch (IOException e){
                log.error("invalid identity",e);
            }
            return;
        }
        
        try {
            httpResponse.setStatus(Status.OK.getStatusCode());
            SmileCustomer cust = new SmileCustomer();
            cust.setCustomerId(customer.getCustomerId());
            cust.setFirstName(customer.getFirstName());
            cust.setMiddleName(customer.getMiddleName());
            cust.setLastName(customer.getLastName());
            cust.setTitle(customer.getTitle());
            cust.setEmailAddress(customer.getEmailAddress());
            cust.setAlternativeContact1(customer.getAlternativeContact1());
            cust.setAlternativeContact2(customer.getAlternativeContact2());
            httpResponse.getWriter().print(mapper.writeValueAsString(cust));
        } catch (IOException ex) {
            httpResponse.setStatus(Status.NOT_FOUND.getStatusCode());
            try {
                httpResponse.getWriter().print(SRAException.getErrorAsJSON("server error", SRAException.SYSTEM_ERROR, "SRA-0001"));
            } catch (IOException e) {
                log.error("Exception: ", e);
            }
            log.error("Exception: ", ex);
        }
    }

    private void otpNotificaton(ServletRequest request, ServletResponse response) {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String method = httpRequest.getMethod();
        if(!method.equals(HttpMethod.POST)){
            httpResponse.setStatus(Status.BAD_REQUEST.getStatusCode());
            return;
        }
        httpResponse.setContentType(MediaType.APPLICATION_JSON);
        String remoteIp = request.getRemoteAddr();
        Set<String> allowedIps = BaseUtils.getPropertyAsSet("env.sra.otpserver.ip");
        log.debug("remote ip [{}] and allowed Ip [{}]", remoteIp, allowedIps);
        if (allowedIps == null || allowedIps.isEmpty() || !allowedIps.contains(remoteIp)) {
            try {
                httpResponse.setStatus(Status.UNAUTHORIZED.getStatusCode());
                httpResponse.getWriter().print(SRAException.getErrorAsJSON("not authorized", SRAException.SYSTEM_ERROR, "SRA-0008"));
            } catch (IOException ex) {
                log.error("Exception: ", ex);
            }
            return;
        }
        Map<String, String> headers = Collections.list(httpRequest.getHeaderNames()).stream().collect(Collectors.toMap(h -> h, httpRequest::getHeader));
        String action = headers.get("x-sra-action");
        log.debug("all headers are :"+headers+" and action is :"+action);
        if (!"OTP-GEN-NOTIFY".equals(action)) {
            try {
                httpResponse.setStatus(Status.BAD_REQUEST.getStatusCode());
                httpResponse.getWriter().print(SRAException.getErrorAsJSON("invalid request", SRAException.SYSTEM_ERROR, "SRA-0009"));
            } catch (IOException ex) {
                log.error("Exception: ",ex);
            }
            return;
        }
        OTPNotification notification = null;
        try {
            notification = mapper.readValue(request.getInputStream(), OTPNotification.class);
        } catch (IOException ex){
            log.error("error :",ex);
        }
        
        try {
            if(notification == null || notification.getIdentity() == null || notification.getKey() == null){
                httpResponse.setStatus(Status.BAD_REQUEST.getStatusCode());
                return;
            }
            createOTPEvent(notification);
            httpResponse.setStatus(Status.OK.getStatusCode());
            httpResponse.getWriter().print(mapper.writeValueAsString(new Done()));
        } catch (IOException ex) {
            log.error("Exception: ", ex);
        }
    }

    private void createOTPEvent(OTPNotification notification) {
        Event eventData = new Event();
        eventData.setEventType("SRA");
        eventData.setEventSubType("OTP_Notification");
        eventData.setEventKey(notification.getIdentity());
        eventData.setEventData(notification.getKey());
        SCAWrapper.getAdminInstance().createEvent(eventData);
        log.debug("event created for "+notification);
    }
    
    /**
     * 
     * @param eventKey GSM mobile number
     * @param eventData OTP 
     */
    private void createOTPEvent(String eventKey, String eventData) {
        Event otpEvent = new Event();
        otpEvent.setEventType("SRA");
        otpEvent.setEventSubType("OTP_Notification");
        otpEvent.setEventKey(eventKey); // GSM Number
        otpEvent.setEventData(eventData); // OTP data
        SCAWrapper.getAdminInstance().createEvent(otpEvent);
        log.debug("event created for {}", eventKey);
    }

    /*private boolean validateRegistrationReq(RegisterCustomerRequest requestBody) {
        EventQuery eq = new EventQuery();
        eq.setEventType("SRA");
        eq.setEventSubType("OTP_Notification");
        eq.setEventKey(requestBody.getIdentity());
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -15);
        eq.setDateFrom(Utils.getDateAsXMLGregorianCalendar(cal.getTime())); // get event from last 15 Minute
        eq.setDateTo(Utils.getDateAsXMLGregorianCalendar(new Date()));
        eq.setResultLimit(3);//check last 3 keys
        EventList el = SCAWrapper.getAdminInstance().getEvents(eq);
        log.debug("found eventlist. size : "+el.getEvents().size());
        if (el.getEvents().stream().anyMatch((e) -> (e.getEventData().equals(requestBody.getKey())))) {
            return true;
        }
        return false;
    }*/
    
     private boolean verifyOTP(String identity, String key) {
        EventQuery eq = new EventQuery();
        eq.setEventType("SRA");
        eq.setEventSubType("OTP_Notification");
        eq.setEventKey(identity);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -15);
        eq.setDateFrom(Utils.getDateAsXMLGregorianCalendar(cal.getTime())); // get event from last 15 Minute
        eq.setDateTo(Utils.getDateAsXMLGregorianCalendar(new Date()));
        eq.setResultLimit(3);//check last 3 keys
        EventList el = SCAWrapper.getAdminInstance().getEvents(eq);
        log.debug("found eventlist. size : "+el.getEvents().size());
        if (el.getEvents().stream().anyMatch((e) -> (e.getEventData().equals(key)))) {
            return true;
        }
        return false;
    }

    private void setPassword(Customer customer, RegisterCustomerRequest requestBody) throws Exception{
       customer.setSSODigest(Utils.hashPasswordWithComplexityCheck(requestBody.getNewPassword()));
       SCAWrapper.getAdminInstance().modifyCustomer(customer);
    }
        
    
    private void sendOTPMessage(ServletRequest request, ServletResponse response) throws Exception {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String method = httpRequest.getMethod();
        if(!method.equals(HttpMethod.POST)){
            httpResponse.setStatus(Status.BAD_REQUEST.getStatusCode());
            return;
        }
        httpResponse.setContentType(MediaType.APPLICATION_JSON);   
        String notifyToken = BaseUtils.getProperty("env.sra.notify.token");
        
        Map<String, String> headers = Collections.list(httpRequest.getHeaderNames()).stream().collect(Collectors.toMap(h -> h, httpRequest::getHeader));
        String token = headers.get("x-token");
        
        log.warn("all headers are :"+headers+" and token is :"+token + ", notify token is: " + notifyToken);
        if (!notifyToken.equals(token)) {
            try {
                httpResponse.setStatus(Status.FORBIDDEN.getStatusCode());
                httpResponse.getWriter().print(SRAException.getErrorAsJSON("Access Token Invalid", SRAException.SYSTEM_ERROR, "SRA-0009"));
            } catch (IOException ex) {
                log.error("Exception: ", ex);
            }
            return;
        }
        SMSMessage notification = null;
        Response resp;
        try {
            notification = mapper.readValue(request.getInputStream(), SMSMessage.class);
        } catch (IOException ex){
            log.error("error :",ex);
        }
        // Generate OTP if it is missing in the request.
        if(notification.getKey() == null || notification.getKey().isEmpty()){
            notification.setKey(String.valueOf(SRAUtil.generateOTP()));
        }
        
        //Generate SMS body if it is missing in request
        if(notification.getBody() == null || notification.getBody().isEmpty()){
            String smsBodyFormat = SRAUtil.getMesssage("sra.otp.sms.body");
            String smsBody = String.format(smsBodyFormat, notification.getKey());
            notification.setBody(smsBody);
            log.debug("sms body for OTP is [{}]",smsBody);
        }
        
        
        createOTPEvent(notification.getTo(), notification.getKey());
        try {
            if(notification.getTo() == null || notification.getBody() == null){
                httpResponse.setStatus(Status.BAD_REQUEST.getStatusCode());
                return;
            }
            
            String otpsmsFrom = BaseUtils.getProperty("env.app.sms.otp.from");
            if(otpsmsFrom != null && !otpsmsFrom.isEmpty()){
                notification.setFrom(otpsmsFrom);
            }
            
            log.debug("notification message for SMS [{}]",notification);
            resp = sendSMS(notification);
            if(notification.getCustomerId() != null && !notification.getCustomerId().isEmpty() 
                && notification.getKey() != null && !notification.getKey().isEmpty()){
                sendOTPEmail(notification.getCustomerId(), notification.getKey());
            }
            httpResponse.setStatus(resp.getStatus());
            httpResponse.getWriter().print(mapper.writeValueAsString(new Done()));
        } catch (IOException ex) {
            log.error("Send OTP Exception: ", ex);
        }
    }
    
    
    /**
     * verify OTP
     * @param request
     * @param response
     * @throws Exception 
     */
    private void verifyOTP(ServletRequest request, ServletResponse response) throws Exception {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String method = httpRequest.getMethod();
        if(!method.equals(HttpMethod.POST)){
            httpResponse.setStatus(Status.BAD_REQUEST.getStatusCode());
            return;
        }
        httpResponse.setContentType(MediaType.APPLICATION_JSON);   
        String notifyToken = BaseUtils.getProperty("env.sra.notify.token");
        
        Map<String, String> headers = Collections.list(httpRequest.getHeaderNames()).stream().collect(Collectors.toMap(h -> h, httpRequest::getHeader));
        String token = headers.get("x-token");
        
        log.warn("all headers are :"+headers+" and token is :"+token + ", notify token is: " + notifyToken);
        if (!notifyToken.equals(token)) {
            try {
                httpResponse.setStatus(Status.FORBIDDEN.getStatusCode());
                httpResponse.getWriter().print(SRAException.getErrorAsJSON("Access Token Invalid", SRAException.SYSTEM_ERROR, "SRA-0009"));
            } catch (IOException ex) {
                log.error("Exception: ", ex);
            }
            return;
        }
        OTPVerificationRequest requestBody = null;
        try {
            requestBody = mapper.readValue(request.getInputStream(), OTPVerificationRequest.class);
        } catch (IOException ex){
            log.error("error :",ex);
            httpResponse.setStatus(Status.INTERNAL_SERVER_ERROR.getStatusCode());
            httpResponse.getWriter().print(SRAException.getErrorAsJSON("Server error", SRAException.SYSTEM_ERROR, "SRA-2239"));
            return;
        }
        
        
        boolean isValidOTP = verifyOTP(requestBody.getMobileNumber(), requestBody.getOtp());
        if(!isValidOTP) {
            httpResponse.setStatus(Status.BAD_REQUEST.getStatusCode());
            httpResponse.getWriter().print(SRAException.getErrorAsJSON("Invalid OTP", SRAException.SYSTEM_ERROR, "SRA-5209"));
        } else {
            httpResponse.setStatus(Status.OK.getStatusCode());
            httpResponse.getWriter().print(mapper.writeValueAsString(requestBody));
        }
    }
    
    public Response sendSMS(SMSMessage otpNotification) throws Exception {        
        String resp = "";
        String from = otpNotification.getFrom();
        String to = otpNotification.getTo();
        String body = otpNotification.getBody();
        
        ShortMessage sm = new ShortMessage();
        sm.setBody(body);
        sm.setFrom(from);
        sm.setTo(to);
        
        if (!sm.getFrom().isEmpty() && !sm.getTo().isEmpty()) {
            log.debug("Sending notification sms from [{}] to [{}] text [{}]", new Object[]{from, to, body});
            try {
                SCAWrapper.getAdminInstance().sendShortMessage(sm);
                resp = "SMS Sent";
            } catch (Exception ex) {
                log.error("error in sendSMS", ex);
                resp = "SMS send failed." + ex.getMessage();
                log.warn("Failed to send notification sms from [{}] to [{}] text [{}] Reason: [{}]", new Object[]{from, to, body, ex.getMessage()});
            }
        }
        return Response.status(200).entity(resp).build();
    }
    
        
    private void sendOTPEmail(String customerId, String otp){
        log.debug("sending OTP notification email. customerId [{}] and otp [{}]", customerId, otp);
        try {
            CustomerCommunicationData email = new CustomerCommunicationData();
            email.setSubjectResourceName("otp.registration.email.subject");
            email.setBodyResourceName("otp.registration.email.body");
            email.getBodyParameters().add(otp);
            email.setCustomerId(Integer.valueOf(customerId));
            email.setBlocking(false);
            SCAWrapper.getAdminInstance().sendCustomerCommunication(email);
            log.debug("OTP notification sent to customer");
        } catch (NumberFormatException ex){
            log.error("sendOTPemail error.",ex);
        }
    }
    
    private void createRechargeOffer(ServletRequest request, ServletResponse response) throws Exception {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String method = httpRequest.getMethod();
        if(!method.equals(HttpMethod.POST)){
            httpResponse.setStatus(Status.BAD_REQUEST.getStatusCode());
            return;
        }
        httpResponse.setContentType(MediaType.APPLICATION_JSON);   
        String notifyToken = BaseUtils.getProperty("env.sra.churn.offer.token");
        
        Map<String, String> headers = Collections.list(httpRequest.getHeaderNames()).stream().collect(Collectors.toMap(h -> h, httpRequest::getHeader));
        String token = headers.get("x-token");
        
        log.warn("all headers are :"+headers+" and token is :"+token + ", notify token is: " + notifyToken);
        if (!notifyToken.equals(token)) {
            try {
                httpResponse.setStatus(Status.FORBIDDEN.getStatusCode());
                httpResponse.getWriter().print(SRAException.getErrorAsJSON("Access Denied", SRAException.SYSTEM_ERROR, "SRA-0009"));
            } catch (IOException ex) {
                log.error("Exception: ", ex);
            }
            return;
        }
        
        
        RechargeOffer rechargeOffer = null;
        
        try {
            rechargeOffer = mapper.readValue(request.getInputStream(), RechargeOffer.class);
            CampaignData campaignData = new CampaignData();
            campaignData.setCampaignId(rechargeOffer.getCampaignId());
            campaignData.setProductInstanceIds(Utils.zip(rechargeOffer.getProductInstanceId().replace("\r\n", ",")));
            
            SCAWrapper.getAdminInstance().storeCampaignData(campaignData);
            
            httpResponse.setStatus(Status.CREATED.getStatusCode());
            httpResponse.getWriter().print(mapper.writeValueAsString(rechargeOffer));
            
        } catch (IOException ex){
            log.error("error :",ex);
            httpResponse.setStatus(Status.SERVICE_UNAVAILABLE.getStatusCode());
            httpResponse.getWriter().print(mapper.writeValueAsString(rechargeOffer));
        }  
    }

    private void addGuestCustomer(RegisterCustomerRequest requestBody) {
        Event eventData = new Event();
        eventData.setEventType("SRA");
        eventData.setEventSubType("OTP_Notification_Guest");
        eventData.setEventKey(requestBody.getIdentity());
        eventData.setEventData(requestBody.getKey());
        SCAWrapper.getAdminInstance().createEvent(eventData);
        log.debug("event created for "+requestBody.getIdentity());
    }
    
    private void getPaymentGateway(ServletRequest request, ServletResponse response) throws IOException{
        Set<String> gateways = BaseUtils.getPropertyAsSet("env.sra.paymentgateways");
        
        List<PaymentGateway> pwlist = new ArrayList<>();
        for(String gw : gateways){
            String gwconfig = "env.scp."+gw.toLowerCase()+".partner.integration.config";
            String gwprop = BaseUtils.getProperty(gwconfig, null);
            boolean isgwup = false;
            if( gwprop != null && !gwprop.isEmpty() ){
                String[] propSplits = gwprop.split("\r\n");
                for(String splitStr : propSplits){
                    if(splitStr.contains("Status=Online")){
                        isgwup = true;
                        break;
                    }
                }
            }
            PaymentGateway pgresp = new PaymentGateway();
            pgresp.setGatewayCode(gw);
            pgresp.setIsup(isgwup);
            pwlist.add(pgresp);
        }
        
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setContentType(MediaType.APPLICATION_JSON);
        httpResponse.setStatus(Status.OK.getStatusCode());
        httpResponse.getWriter().print(mapper.writeValueAsString(pwlist));
    }
    
    private void retrieveAccountCustomerId(ServletRequest request, ServletResponse response){
        String accountId = request.getParameter("accountId");
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setContentType(MediaType.APPLICATION_JSON);
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String method = httpRequest.getMethod();
        if(!method.equals(HttpMethod.POST)){
            httpResponse.setStatus(Status.BAD_REQUEST.getStatusCode());
            return;
        }
        
        String notifyToken = BaseUtils.getProperty("env.sra.ussd.caller.static.tokens");
        
        Map<String, String> headers = Collections.list(httpRequest.getHeaderNames()).stream().collect(Collectors.toMap(h -> h, httpRequest::getHeader));
        String token = headers.get("x-token");
        
        log.warn("all headers are :"+headers+" and token is :"+token + ", notify token is: " + notifyToken);
        if (!notifyToken.equals(token)) {
            try {
                httpResponse.setStatus(Status.FORBIDDEN.getStatusCode());
                httpResponse.getWriter().print(SRAException.getErrorAsJSON("Access Denied", SRAException.SYSTEM_ERROR, "SRA-0009"));
            } catch (IOException ex) {
                log.error("Exception: ", ex);
            }
            return;
        }
        
        
        if (accountId == null || accountId.isEmpty()) {
            try {
                httpResponse.setStatus(Status.BAD_REQUEST.getStatusCode());
                httpResponse.getWriter().print(SRAException.getErrorAsJSON("accountId not provided", SRAException.BUSINESS_ERROR, "SRA-0073"));
                return;
            } catch (IOException ex) {
                log.error("retrieveAccountCustomerId Exception: ", ex);
            }
        }
        AccountQuery accountQuery = new AccountQuery();
        accountQuery.setAccountId(Long.parseLong(accountId));        
        accountQuery.setVerbosity(StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
        int custId = -1;        
        
        try {
            custId = SCAWrapper.getAdminInstance().getAccount(accountQuery).getServiceInstances().get(0).getCustomerId();
            
            
            if(custId == -1){
                httpResponse.setStatus(Status.NOT_FOUND.getStatusCode());
                httpResponse.getWriter().print(SRAException.getErrorAsJSON("No customer found for this account Id.", SRAException.BUSINESS_ERROR, "SRA-0053"));
                return;
            }
        } catch(Exception ex){
            try {
                log.error("error in retrieveAccountCustomerId:",ex);
                httpResponse.setStatus(Status.NOT_FOUND.getStatusCode());
                httpResponse.getWriter().print(SRAException.getErrorAsJSON("No customer found for the supplied account Id.", SRAException.BUSINESS_ERROR, "SRA-0053"));
            } catch (IOException e){
                log.error("invalid customerId",e);
            }
            return;
        }
        
        try {
            httpResponse.setStatus(Status.OK.getStatusCode());
            httpResponse.getWriter().print(mapper.writeValueAsString(custId));
        } catch (IOException ex) {
            httpResponse.setStatus(Status.NOT_FOUND.getStatusCode());
            try {
                httpResponse.getWriter().print(SRAException.getErrorAsJSON("server error", SRAException.SYSTEM_ERROR, "SRA-0001"));
            } catch (IOException e) {
                log.error("Exception: ", e);
            }
            log.error("Exception: ", ex);
        }
    }
    
}
