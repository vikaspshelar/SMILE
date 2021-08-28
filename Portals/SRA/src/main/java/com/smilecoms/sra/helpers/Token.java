/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.helpers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.AuthenticationQuery;
import com.smilecoms.commons.sca.AuthenticationResult;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.CustomerQuery;
import com.smilecoms.commons.sca.SCABusinessError;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.base.cache.CacheHelper;
import com.smilecoms.commons.sca.Event;
import com.smilecoms.commons.sca.ServiceInstanceList;
import com.smilecoms.commons.sca.ServiceInstanceQuery;
import com.smilecoms.commons.sca.StServiceInstanceLookupVerbosity;
import com.smilecoms.commons.util.Codec;
import com.smilecoms.commons.util.Utils;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public final class Token implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(Token.class);
    private static transient final String TOKEN_PARAM_NAME = "X-token";
    private String username;
    private String tokenUUID;
    private String originatingIP;
    private double version;
    private Date expires;
    private List<String> groups;
    private static transient final int validitySecs = 60 * 30;
    private int customerId;
    private String requestChannel;
    private static final transient ThreadLocal<Token> requestsToken = new ThreadLocal();
    private transient HttpServletRequest requestsRequest;
    private transient boolean brandNew;
    public static final transient ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true);
    }

    @JsonIgnore
    public static Token getRequestsToken() {
        Token token = requestsToken.get();
        if (token == null) {
            throw new java.lang.UnsupportedOperationException("Cannot call getRequestsToken for requests that have not gone though the SRAFilter");
        }
        return token;
    }

    @JsonIgnore
    public HttpServletRequest getRequestsRequest() {
        return requestsRequest;
    }

    public Token(HttpServletRequest httpRequest) throws Exception {
        log.debug("Context Path is [{}] URI is [{}]", httpRequest.getContextPath(), httpRequest.getRequestURI());
        if (httpRequest.getRequestURI().equals(httpRequest.getContextPath() + "/tokens")
                && httpRequest.getParameter("username") != null
                && httpRequest.getParameter("password") != null
                && httpRequest.getParameter("srav") != null) {
            createToken(httpRequest.getParameter("username"), httpRequest.getParameter("password"), Double.parseDouble(httpRequest.getParameter("srav")), Utils.getRemoteIPAddress(httpRequest), httpRequest.getParameter("channel"));
        } else {
            inititialiseToken(httpRequest);
        }
    }

    public void inititialiseToken(HttpServletRequest httpRequest) throws TokenException {
        tokenUUID = httpRequest.getHeader(Token.TOKEN_PARAM_NAME);
        if (tokenUUID == null) {
            tokenUUID = httpRequest.getParameter(Token.TOKEN_PARAM_NAME);
        }

        if (tokenUUID == null
                && BaseUtils.getBooleanProperty("env.sra.remoteip.allowed.tocall.without.token", false)
                && BaseUtils.getBooleanProperty("env.sra.thirdparty.static.tokens.enabled", false)) {

            String remoteIP = Utils.getRemoteIPAddress(httpRequest);
            log.debug("Checking if remote IP [{}] is allowed to call without token", remoteIP);
            Set<String> remoteIPTokenMapping = BaseUtils.getPropertyAsSet("env.sra.thirdparty.remoteip.token.mapping");

            for (String bits : remoteIPTokenMapping) {
                String[] bitsArray = bits.split("\\|");
                if (bitsArray[0].equals(remoteIP)) {
                    log.debug("Found match for third party remote ip token mapping [{}]", bits);
                    tokenUUID = bitsArray[1];
                    break;
                }
            }
        }
        String guestToken = BaseUtils.getProperty("env.sra.guest.token", "");
        boolean isGuestToken = guestToken.equals(tokenUUID);
        log.debug("guest token in db [{}] and isGuestToken[{}]",guestToken, isGuestToken);
        if (tokenUUID == null || isGuestToken) {
            log.debug("No token found. Checking if this URI [{}] requires a token", httpRequest.getRequestURI());
            String tokenFreeURIs = BaseUtils.getProperty("global.sra.tokenfree.uri.regex", "X^"); // X^ will never match anything
            List<String> tokenFreeURIArray = Utils.getListFromCRDelimitedString(tokenFreeURIs);
            boolean tokenFreeURIFound = false;
            for (String tokenFreeUri : tokenFreeURIArray) {
                if (Utils.matchesWithPatternCache(httpRequest.getRequestURI(), tokenFreeUri)) {
                    tokenFreeURIFound = true;
                    this.groups = Arrays.asList(new String[]{"Administrator"});
                    this.username = "admin";
                    break;
                }
            }
            String guestURI = BaseUtils.getProperty("sra.guest.uri.regex", "X^"); // X^ will never match anything
            List<String> guestURIArray = Utils.getListFromCRDelimitedString(guestURI);
            log.debug("guestURIArray is [{}]", guestURIArray);
            boolean isGuestURI = false;
            for (String usesturi : guestURIArray) {
                if (Utils.matchesWithPatternCache(httpRequest.getRequestURI(), usesturi) && isGuestToken) {
                    isGuestURI = true;
                    log.debug("request received for guest user");
                    this.groups = Arrays.asList(new String[]{"Administrator"});
                    this.username = "admin";
                    break;
                }
            }
            log.debug("isGuestURI is [{}]",isGuestURI);
            if (tokenFreeURIFound || isGuestURI) {
                updateExpiry();
                this.tokenUUID = "";
                this.originatingIP = Utils.getRemoteIPAddress(httpRequest);
                this.requestsRequest = httpRequest;
                this.brandNew = false;
                if (httpRequest.getHeader("srav") != null) {
                    this.version = Double.parseDouble(httpRequest.getHeader("srav"));
                } else {
                    this.version = BaseUtils.getDoubleProperty("env.sra.openaccess.version", 1.0);
                }
                requestsToken.set(this);
                log.debug("Request is using a newly created temp token with version [{}] for tokenfree URI [{}]", version, httpRequest.getRequestURI());
                return;
            }
            throw new TokenException("No token parameter nor header found");
        }

        try {
            Set<String> staticAdminTokens = BaseUtils.getPropertyAsSet("env.sra.admin.static.tokens");
            if (staticAdminTokens.contains(tokenUUID)) {
                updateExpiry();
                this.groups = Arrays.asList(new String[]{"Administrator"});
                this.tokenUUID = "";
                this.username = "admin";
                this.originatingIP = Utils.getRemoteIPAddress(httpRequest);
                this.requestsRequest = httpRequest;
                this.brandNew = false;
                if (httpRequest.getHeader("srav") != null) {
                    this.version = Double.parseDouble(httpRequest.getHeader("srav"));
                } else {
                    this.version = BaseUtils.getDoubleProperty("env.sra.openaccess.version", 1.0);
                }
                requestsToken.set(this);
                log.debug("Request is using a static admin token");
                return;
            }
        } catch (Exception e) {
        }

        if (BaseUtils.getBooleanProperty("env.sra.thirdparty.static.tokens.enabled", false)) {
            log.debug("Going to look for third party static tokens in config matching [{}]", tokenUUID);
            try {
                Set<String> staticThirdPartyCallerTokens = BaseUtils.getPropertyAsSet("env.sra.thirdparty.caller.static.tokens");
                String usrname = "";
                String pass = "";
                boolean foundThirdPartyToken = false;

                for (String bits : staticThirdPartyCallerTokens) {
                    String[] bitsArray = bits.split("\\|");
                    if (bitsArray[0].equals(tokenUUID)) {
                        log.debug("Found match for third party static token in config [{}]", bits);
                        usrname = bitsArray[1];
                        pass = bitsArray[2];
                        foundThirdPartyToken = true;
                        break;
                    }
                }

                if (foundThirdPartyToken) {

                    if (usrname.isEmpty()) {
                        throw new TokenException("Invalid username or password");
                    }

                    log.debug("Authenticating user [{}] as configured in props for token [{}]", usrname, tokenUUID);
                    AuthenticationQuery authQuery = new AuthenticationQuery();
                    authQuery.setSSOIdentity(usrname);
                    authQuery.setSSOEncryptedPassword(Codec.stringToEncryptedHexString(pass));
                    AuthenticationResult authResult;

                    try {
                        authResult = SCAWrapper.getAdminInstance().authenticate(authQuery);
                    } catch (SCABusinessError sbe) {
                        log.debug("User not found: [{}]", sbe.toString());
                        throw new TokenException("Invalid username or password");
                    }
                    boolean authenticated = authResult.getDone().equals(com.smilecoms.commons.sca.StDone.TRUE);
                    if (!authenticated) {
                        throw new TokenException("Invalid username or password");
                    }

                    log.debug("Authentication completed successfuly");
                    updateExpiry();
                    this.groups = authResult.getSecurityGroups();
                    this.tokenUUID = "";
                    this.username = usrname;
                    this.originatingIP = Utils.getRemoteIPAddress(httpRequest);
                    this.requestsRequest = httpRequest;
                    this.brandNew = false;
                    if (httpRequest.getHeader("srav") != null) {
                        this.version = Double.parseDouble(httpRequest.getHeader("srav"));
                    } else {
                        this.version = BaseUtils.getDoubleProperty("env.sra.openaccess.version", 1.0);
                    }
                    requestsToken.set(this);
                    log.debug("Request is using a static token for third party caller");
                    return;
                }
            } catch (Exception e) {
            }
        }

        Token tokenFromCache = (Token) Utils.toObject((byte[]) CacheHelper.getFromRemoteCache(tokenUUID));

        if (tokenFromCache == null) {
            if (tokenUUID.equals("openaccess") && BaseUtils.getBooleanProperty("env.development.mode", false)) {
                updateExpiry();
                this.groups = Arrays.asList(new String[]{"Administrator"});
                this.tokenUUID = "openaccess";
                this.username = "admin";
                this.originatingIP = Utils.getRemoteIPAddress(httpRequest);
                this.requestsRequest = httpRequest;
                this.brandNew = false;
                if (httpRequest.getHeader("srav") != null) {
                    this.version = Double.parseDouble(httpRequest.getHeader("srav"));
                } else {
                    this.version = BaseUtils.getDoubleProperty("env.sra.openaccess.version", 1.0);
                }
                requestsToken.set(this);
                CacheHelper.putInRemoteCache(tokenUUID, Utils.toBytes(this), validitySecs);
                log.debug("Request is using a newly created openaccess token with version [{}]", version);
                return;
            } else {
                throw new TokenException("Token is invalid or expired -- " + tokenUUID);
            }
        }

        if (tokenFromCache.getExpires().before(new Date())) {
            throw new TokenException("Token exists but has expired -- " + tokenUUID + " " + tokenFromCache.getExpires());
        }
        this.groups = tokenFromCache.getGroups();
        this.tokenUUID = tokenFromCache.getTokenUUID();
        this.username = tokenFromCache.getUsername();
        this.customerId = tokenFromCache.getCustomerId();
        this.originatingIP = Utils.getRemoteIPAddress(httpRequest);
        this.requestsRequest = httpRequest;
        this.brandNew = false;
        this.version = tokenFromCache.getVersion();
        this.requestChannel = tokenFromCache.getRequestChannel();
        int validity = (this.version == 2.0 ? (30*24*60*60) : validitySecs);
        updateExpiry(validity);
        requestsToken.set(this);
        if (log.isDebugEnabled()) {
            log.debug("Request is from user [{}] with groups [{}] IP [{}] using version [{}]", new Object[]{username, groups, originatingIP, version});
        }
        CacheHelper.putInRemoteCache(tokenUUID, Utils.toBytes(this), validity);
    }

    private void createToken(String username, String password, double version, String ip, String channel) throws TokenException {
        this.username = username;

        try {
            String allowedIps = BaseUtils.getSubProperty("env.sra.ip.restrictions", username);
            if (allowedIps != null) {
                log.debug("User [{}] is allowed to access SRA from IPs matching [{}]", username, allowedIps);
                if (!Utils.matches(ip, allowedIps)) {
                    throw new TokenException("User not allowed to access SRA from IP -- " + username + "@" + ip);
                }
            }
        } catch (Exception e) {
            log.debug("Error checking IP access for username [{}]: [{}]", username, e.toString());
        }

        log.debug("username in request is "+username);
        if(isNumeric(username)){
            this.username = username = getUserNameUsingSmileNumber(username);
            log.debug("username found is "+username);
        }
        if(null == channel || channel.isEmpty()){
            channel = "WebPortal";
        }
        AuthenticationQuery authQuery = new AuthenticationQuery();
        authQuery.setSSOIdentity(username);
        authQuery.setSSOEncryptedPassword(Codec.stringToEncryptedHexString(password));
        AuthenticationResult authResult;
        try {
            authResult = SCAWrapper.getAdminInstance().authenticate(authQuery);
        } catch (SCABusinessError sbe) {
            log.error("User not found: [{}]", sbe.toString());
            if(sbe.toString().contains("Customer account temporarily locked")){
                throw new UserLockedException(SRAUtil.getMesssage("sra.error.account.locked"));
            } else {
                throw new TokenException(SRAUtil.getMesssage("sra.error.invalid.login"));
            }
        }
        boolean authenticated = authResult.getDone().equals(com.smilecoms.commons.sca.StDone.TRUE);
        if (!authenticated) {
            throw new TokenException(SRAUtil.getMesssage("sra.error.invalid.login"));
        }
        groups = authResult.getSecurityGroups();
        tokenUUID = Utils.getUUID();
        this.version = version;
        this.originatingIP = ip;
        this.requestChannel = channel;
        int validity = (version == 2.0) ? (30*24*60*60) : validitySecs;
        log.debug("validy in seconds : "+validity);
        updateExpiry(validity);
        CustomerQuery cq = new CustomerQuery();
        cq.setSSOIdentity(username);
        cq.setResultLimit(1);
        cq.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        Customer cust = SCAWrapper.getAdminInstance().getCustomer(cq);
        customerId = cust.getCustomerId();
        requestsToken.set(this);
        this.brandNew = true;
        CacheHelper.putInRemoteCacheSync(tokenUUID, Utils.toBytes(this), validity);
        
        if(version == 2.0){
            // log customer for reporting all Selfcare App users
            createTokenGenEvent(cust, validity);
        }
    }

    @JsonIgnore
    public boolean isBrandNew() {
        return brandNew;
    }

    private void updateExpiry() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, validitySecs);
        expires = cal.getTime();
    }

    private void updateExpiry(int seconds){
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, seconds);
        expires = cal.getTime();
    }
    
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTokenUUID() {
        return tokenUUID;
    }

    public void setTokenUUID(String tokenUUID) {
        this.tokenUUID = tokenUUID;
    }

    public Date getExpires() {
        return expires;
    }

    public void setExpires(Date expires) {
        this.expires = expires;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getOriginatingIP() {
        return originatingIP;
    }

    public void setOriginatingIP(String originatingIP) {
        this.originatingIP = originatingIP;
    }

    public double getVersion() {
        return version;
    }

    public String getRequestChannel() {
        return requestChannel;
    }

    public void setRequestChannel(String requestChannel) {
        this.requestChannel = requestChannel;
    }

    @JsonIgnore
    public String getJSON() throws IOException {
        return mapper.writeValueAsString(this).replace("Token", "token");
    }
    
    private boolean isNumeric(String string) {
        Pattern phonePattern = Pattern.compile("([0-9]*)");
        Matcher m = phonePattern.matcher(string.trim());
        return m.matches();
    }
    
    private String getUserNameUsingSmileNumber(String smileNumber){
        ServiceInstanceQuery siq = new ServiceInstanceQuery();
        siq.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
        siq.setIdentifierType("END_USER_SIP_URI");
        siq.setIdentifier(Utils.getPublicIdentityForPhoneNumber(smileNumber));
        int cusId;
        String userName = null;
        try {
            ServiceInstanceList sil = SCAWrapper.getAdminInstance().getServiceInstances(siq);
            if (sil == null || sil.getServiceInstances() == null || sil.getServiceInstances().isEmpty()) {
                return null;
            }
            cusId = sil.getServiceInstances().get(0).getCustomerId();
            Customer customer = getCustomerById(cusId, StCustomerLookupVerbosity.CUSTOMER);
            userName =  customer.getSSOIdentity();
        } catch (Exception ex) {
            log.error("customer id not found for smile voice number [{}]", smileNumber);
        }
        return userName;
    }
    
    private Customer getCustomerById(int customerId, StCustomerLookupVerbosity verbosity) {
        CustomerQuery cq = new CustomerQuery();
        cq.setCustomerId(customerId);
        cq.setVerbosity(verbosity);
        cq.setProductInstanceOffset(0);
        cq.setProductInstanceResultLimit(1);
        Customer cust = SCAWrapper.getAdminInstance().getCustomer(cq);
        return cust;
    }

    private void createTokenGenEvent(Customer cust, int validity) {
        Event eventData = new Event();
        eventData.setEventType("SRA");
        eventData.setEventSubType("Selfcare_login");
        eventData.setEventKey(String.valueOf(cust.getCustomerId()));
        eventData.setEventData(String.valueOf(validity));
        SCAWrapper.getAdminInstance().createEvent(eventData);
    }
}
