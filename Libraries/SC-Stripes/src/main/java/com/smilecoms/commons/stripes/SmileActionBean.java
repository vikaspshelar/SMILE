package com.smilecoms.commons.stripes;

import com.google.gson.Gson;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.*;
import com.smilecoms.commons.base.cache.CacheHelper;
import com.smilecoms.commons.localisation.LocalisationHelper;
import com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper;
import com.smilecoms.commons.util.Utils;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.validation.SimpleError;
import org.slf4j.*;

/**
 * This class implements the Stripes ActionBean with the intent that all our own
 * action beans extend SmileActionBean This allows for helpers and other stuff
 * to go in SmileActionBean as opposed to each ActionBean we write The result is
 * less code and more standardisation.
 *
 * An example is how SmileActionBean has a local sca variable for calling into
 * the SCA delegate. Another is for standardised helper functions
 *
 * @author PCB
 */
public class SmileActionBean extends SCATypes implements ActionBean {

    private ActionBeanContext context;
    public static final String AT = "@";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String RESOURCES_KEY_PREFIX = "env.portal.allowed.resources.";    // Used to access SCA
    /**
     * ***********************************************************************************
     * // Session accessors - isolate all session work to this area to keep it
     * under control
     * ***********************************************************************************
     */
    public static final String SESSION_KEY_CCAGENT_EXTENSION = "ccaext";
    public static final String SESSION_KEY_CCAGENT_LASTEVENT = "0";
    public static final String SESSION_KEY_IP_ADDRESS = "ip";
    public static final String SESSION_KEY_USERAGENT = "ua";
    public static final String SESSION_KEY_CUSTOMER_ID = "cid";
    public static final String SESSION_KEY_WAREHOUSE_ID = "whid";
    public static final String SESSION_KEY_STATE = "state";
    public static final String SESSION_KEY_CUSTOMER_ORG_ID = "orgid";
    protected Logger log = LoggerFactory.getLogger(this.getClass());
    private static final Logger mylog = LoggerFactory.getLogger(SmileActionBean.class);
    private int pageStart = 0;
    private int pageSize = 0;
    private int pageMax = 0;
    private static final Gson gson = new Gson();

    public void setIPAddressInSession() {
        this.getContext().getRequest().getSession().setAttribute(SESSION_KEY_IP_ADDRESS, getOriginatingIP());
    }

    public String getIPAddressFromSession() {
        return (String) this.getContext().getRequest().getSession().getAttribute(SESSION_KEY_IP_ADDRESS);
    }

    /**
     * get the current logged in extension for CTI purposes. This is either
     * retrieved from Session or from a cookie
     *
     * @return Extension number, eg "61006"
     */
    public String getLoggedInCCAgentExtension() {
        String extension = (String) (this.getContext().getRequest().getSession().getAttribute(SESSION_KEY_CCAGENT_EXTENSION));
        if (extension == null || extension.isEmpty()) {
            //lets look at the cookie
            Cookie cookie = getCookie(SESSION_KEY_CCAGENT_EXTENSION);
            if (cookie != null) {
                extension = cookie.getValue();
            }
        }
        return extension;
    }

    public static String getLoggedInCCAgentExtension(HttpServletRequest request) {
        String extension = (String) (request.getSession().getAttribute(SESSION_KEY_CCAGENT_EXTENSION));
        if (extension == null || extension.isEmpty()) {
            //lets look at the cookie
            Cookie cookie = getCookie(request, SESSION_KEY_CCAGENT_EXTENSION);
            if (cookie != null) {
                extension = cookie.getValue();
            }
        }
        return extension;
    }

    public String setWarehouseIdInSession() {
        CustomerQuery query = new CustomerQuery();
        query.setSSOIdentity(getUser());
        query.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        query.setResultLimit(1);
        Customer loggedInCustomer = SCAWrapper.getUserSpecificInstance().getCustomer(query);
        HttpSession session = getContext().getRequest().getSession();
        session.setAttribute(SESSION_KEY_WAREHOUSE_ID, loggedInCustomer.getWarehouseId());
        return loggedInCustomer.getWarehouseId();
    }

    public String getWarehouseIdFromSession() {
        String whId = (String) getContext().getRequest().getSession().getAttribute(SESSION_KEY_WAREHOUSE_ID);
        if (whId == null) {
            return setWarehouseIdInSession();
        } else {
            return whId;
        }
    }

    private int setUserCustomerIdInSession() {
        CustomerQuery query = new CustomerQuery();
        query.setSSOIdentity(getUser());
        query.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        query.setResultLimit(1);
        Customer loggedInCustomer = SCAWrapper.getUserSpecificInstance().getCustomer(query);
        HttpSession session = getContext().getRequest().getSession();
        session.setAttribute(SESSION_KEY_CUSTOMER_ID, (Integer) loggedInCustomer.getCustomerId());
        return loggedInCustomer.getCustomerId();
    }

    public static void setUserCustomerIdInSession(HttpSession session, int customerId) {
        session.setAttribute(SESSION_KEY_CUSTOMER_ID, (Integer) customerId);
    }

    private String setUserStateInSession() {
        CustomerQuery query = new CustomerQuery();
        query.setSSOIdentity(getUser());
        query.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS);
        query.setResultLimit(1);
        Customer loggedInCustomer = SCAWrapper.getUserSpecificInstance().getCustomer(query);
        HttpSession session = getContext().getRequest().getSession();
        String state = null;
        try {
            state = loggedInCustomer.getAddresses().get(0).getState();
            session.setAttribute(SESSION_KEY_STATE, state);
        } catch (Exception e) {
            log.warn("Logged in user does not have a state", e);
        }
        return state;
    }

    public int getUserCustomerIdFromSession() {
        Integer custId = (Integer) getContext().getRequest().getSession().getAttribute(SESSION_KEY_CUSTOMER_ID);
        if (custId == null) {
            return setUserCustomerIdInSession();
        } else {
            return custId;
        }
    }

    public String getUserStateFromSession() {
        String state = (String) getContext().getRequest().getSession().getAttribute(SESSION_KEY_STATE);
        if (state == null) {
            return setUserStateInSession();
        } else {
            return state;
        }
    }

    public void setCallCentreLastEventEpoch(String epoch) {
        HttpSession session = getContext().getRequest().getSession();
        session.setAttribute(SESSION_KEY_CCAGENT_LASTEVENT, epoch);
        // Put in memecached in case of session failover
        CacheHelper.putInRemoteCache(SESSION_KEY_CCAGENT_LASTEVENT + getUserCustomerIdFromSession(), epoch, 3600 * 24); // 1 day is probably max possible logged in time
    }

    public String getCallCentreLastEventEpoch() {
        HttpSession session = getContext().getRequest().getSession();
        String epoch = (String) session.getAttribute(SESSION_KEY_CCAGENT_LASTEVENT);
        if (epoch == null) {
            // look in memecached in case this is a session failover
            epoch = (String) CacheHelper.getFromRemoteCache(SESSION_KEY_CCAGENT_LASTEVENT + getUserCustomerIdFromSession());
            if (epoch != null) {
                session.setAttribute(SESSION_KEY_CCAGENT_LASTEVENT, epoch);
            }
        }
        if (epoch != null && epoch.isEmpty()) {
            return null;
        }
        return epoch;
    }

    public void setCallCentreAgentExtensionInSession(String extension) {
        HttpSession session = getContext().getRequest().getSession();
        session.setAttribute(SESSION_KEY_CCAGENT_EXTENSION, extension);
        // Put in memecached in case of session failover
        CacheHelper.putInRemoteCache(SESSION_KEY_CCAGENT_EXTENSION + getUserCustomerIdFromSession(), extension, 3600 * 24); // 1 day is probably max possible logged in time
    }

    public void removeCallCentreAgentExtensionFromSession() {
        HttpSession session = getContext().getRequest().getSession();
        session.removeAttribute(SESSION_KEY_CCAGENT_EXTENSION);
        // Dont remove from remote cache in case the user has multiple logins
    }

    public String getCallCentreAgentExtensionFromSession() {
        HttpSession session = getContext().getRequest().getSession();
        String extension = (String) session.getAttribute(SESSION_KEY_CCAGENT_EXTENSION);
        if (extension == null) {
            // look in remote cache in case this is a session failover
            extension = (String) CacheHelper.getFromRemoteCache(SESSION_KEY_CCAGENT_EXTENSION + getUserCustomerIdFromSession());
            if (extension != null) {
                session.setAttribute(SESSION_KEY_CCAGENT_EXTENSION, extension);
            }
        }
        if (extension != null && extension.isEmpty()) {
            return null;
        }
        return extension;
    }

    public String getUserTownBasedOnIPAddress() {
        try {
            String ip = getOriginatingIP();
            log.debug("Looking for town of IP [{}]", ip);
            String location = (String) CacheHelper.getFromRemoteCache("IPLocation_" + ip);
            if (location == null) {
                log.debug("Could not get location from remote cache. Returning unknown");
                return "unknown";
            }
            String town = Utils.getSectorsTown(location);
            log.debug("Town of IP [{}] Location [{}] is [{}]", new Object[]{ip, location, town});
            return town;
        } catch (Exception e) {
            log.debug("Error getting user town [{}]", e.toString());
            return "unknown";
        }
    }

    /**
     * Delete the session from memory
     *
     * @param request
     */
    public static void invalidateSession(HttpServletRequest request) {
        if (request.getSession() == null) {
            return;
        }
        try {
            mylog.debug("Removing session with id [{}] from remote cache", request.getSession().getId());
            CacheHelper.removeFromRemoteCache(request.getSession().getId());

            if (request.getRemoteUser() != null && BaseUtils.getBooleanProperty("global.portal.enforce.singlemachine.login", false)) {
                mylog.debug("Removing user's IP from remote cache", request.getSession().getId());
                CacheHelper.removeFromRemoteCache(request.getRemoteUser() + "_SINGLE_MACHINE_LOGIN");
            }
        } catch (Exception e) {
            mylog.warn("Error removing session info from remote cache: ", e);
        }
        try {
            request.getSession().invalidate();
        } catch (Exception e) {
            mylog.debug("Error in invalidateSession: [{}]", e.toString());
        }
    }

    /**
     * Delete the session from memory
     */
    public void invalidateSession() {
        invalidateSession(getContext().getRequest());
    }

    /**
     * Verify that the form data posted contains the correct session ID
     *
     * @return
     */
    public void checkCSRF() throws InsufficientPrivilegesException {
        String formSessionIdWithCounter = (String) getContext().getRequest().getParameter("FORMJSESSIONID");
        log.debug("FORMJSESSIONID is [{}]", formSessionIdWithCounter);
        if (formSessionIdWithCounter == null) {
            log.warn("Form without FORMJSESSIONID detected");
            throw new InsufficientPrivilegesException("Cross Site Request Forgery");
        }

        String[] bits = formSessionIdWithCounter.split("_");
        String formSessionId = bits[0];
        String sessId = getContext().getRequest().getSession().getId();
        if (!formSessionId.equals(sessId)) {
            log.warn("Form with incorrect JSESSIONID detected. Submitted was [{}] required was [{}]", formSessionId, sessId);
            throw new InsufficientPrivilegesException("Cross Site Request Forgery");
        }

        Integer lastCounterUsed = (Integer) getContext().getRequest().getSession().getAttribute("LASTFORMCOUNTERUSED");
        log.debug("LASTFORMCOUNTERUSED is [{}]", lastCounterUsed);
        if (lastCounterUsed != null && lastCounterUsed == Integer.MAX_VALUE) {
            lastCounterUsed = 0;
        }
        int counterOnSubmittedForm = Integer.parseInt(bits[1]);
        log.debug("counterOnSubmittedForm is [{}]", counterOnSubmittedForm);
        if (lastCounterUsed != null && counterOnSubmittedForm <= lastCounterUsed) {
            log.warn("Double post detected. Log on debug/finest level to see stack trace");
            if (log.isDebugEnabled()) {
                log.debug("Stacktrace: ", new Exception());
            }
            throw new DoublePostException();
        }

        getContext().getRequest().getSession().setAttribute("LASTFORMCOUNTERUSED", counterOnSubmittedForm);
    }

    // *************************************************************************************
    // End Session accessors
    // *************************************************************************************
    public String getSCACallInfo() {
        return SCAWrapper.getUserSpecificInstance().getSCACallInfo();
    }

    public HttpServletRequest getRequest() {
        return getContext().getRequest();
    }

    public HttpServletResponse getResponse() {
        return getContext().getResponse();
    }

    public int getPageStart() {
        return pageStart;
    }

    public void setPageStart(int pageStart) {
        this.pageStart = pageStart;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getPageMax() {
        return pageMax;
    }

    public void setPageMax(int pageMax) {
        this.pageMax = pageMax;
    }

    public Resolution pageAction() {
        Resolution resolution = null;
        String action = getContext().getRequest().getParameter("action");

        Class params[] = {};
        Object paramsObj[] = {};

        try {
            Method actionMethod = this.getClass().getMethod(action, params);
            resolution = (Resolution) actionMethod.invoke(this, paramsObj);
        } catch (Exception e) {
            log.warn("Error: ", e);
        }

        return resolution;
    }

    public Resolution pageNext() {
        pageStart += pageSize;
        if (pageStart == pageMax) {
            pageStart -= pageSize;
        }
        if (pageStart > pageMax) {
            pageStart = pageMax - (pageMax % pageSize);
        }
        return pageAction();
    }

    public Resolution pageBack() {
        pageStart -= pageSize;
        if (pageStart < 0) {
            pageStart = 0;
        }
        return pageAction();
    }

    public Resolution pageFirst() {
        pageStart = 0;
        return pageAction();
    }

    public Resolution pageLast() {
        pageStart = pageMax - (pageMax % pageSize);
        if (pageStart == pageMax) {
            pageStart = pageMax - pageSize;
        }
        return pageAction();
    }
    // *************************************************************************************
    // LOCALIZATION STUFF
    // *************************************************************************************

    /**
     * Returns the users Locale. First it returns the Locale in the session if
     * their is one. If not, it gets the language from the locale passed in the
     * HTTP request and returns the standard Locale we use for the language (by
     * calling LocalisationHelper.getLocaleForLanguage(language))
     *
     * @return Locale
     */
    public Locale getLocale() {
        String language = this.getContext().getRequest().getLocale().getLanguage();
        // getLocaleForLanguage returns default locale from properties if the language passed in is not supported
        Locale locale = LocalisationHelper.getLocaleForLanguage(language);
        return locale;
    }

    /**
     * Returns the localised (translated) string for the key as per the
     * resources table in SmileDB. Uses the Locale returned by getLocale - i.e.
     * the one in the users session, and if it doesn;t exist, then the Locale
     * for the language specified in the locale passed in the HTTP request
     *
     * @param key
     * @return Resource String
     */
    public String getLocalisedString(String key) {
        return LocalisationHelper.getLocalisedString(getLocale(), key);
    }

    /**
     * Returns the localised (translated) string for the key as per the
     * resources table in SmileDB. Uses the Locale returned by getLocale - i.e.
     * the one in the users session, and if it doesn;t exist, then the Locale
     * for the language specified in the locale passed in the HTTP request
     *
     * @param key
     * @param parameter
     * @return Resource String
     */
    public String getLocalisedString(String key, Object... parameter) {
        return LocalisationHelper.getLocalisedString(getLocale(), key, parameter);
    }

    /**
     * Returns a list of the Locales allowed by calling
     * LocalisationHelper.getAllowedLocales. Effectively its a list of locales
     * generated by looping through the languages in
     * env.portal.languages.available and calling getLocaleForLanguage on each
     * one
     *
     * @return List of Locales
     */
    public List<Locale> getAllowedLocales() {
        return LocalisationHelper.getAllowedLocales();
    }

    /**
     * Returns a list of the languages (e.g. en, zu etc) allowed. Effectively
     * its a list of values in property env.portal.languages.available
     *
     * @return List of languages
     */
    public List<String> getAllowedLanguages() {
        return LocalisationHelper.getAllowedLanguages();
    }

    /**
     * Returns the value of property env.default.portal.language (e.g. en)
     *
     * @return language in short notation e.g. en
     */
    public String getDefaultLanguage() {
        return LocalisationHelper.getDefaultLanguage();
    }
    // *************************************************************************************
    // END LOCALIZATION STUFF
    // *************************************************************************************

    /**
     * Send a trap to ops at level MAJOR
     *
     * @param msg The message to send
     */
    public void sendOperationTrap(String msg) {
        BaseUtils.sendTrapToOpsManagement(
                BaseUtils.MAJOR,
                "Web Server",
                msg);
    }

    /**
     * Returns the Stripes context associated with the request
     *
     * @return context
     */
    @Override
    public ActionBeanContext getContext() {
        return context;
    }
    /**
     * Sets the Stripes context on the actionbean
     *
     * @param context
     */
    private static final ThreadLocal<HttpServletRequest> requestStore = new ThreadLocal();

    @Override
    public void setContext(ActionBeanContext context) {
        this.context = context;
        requestStore.set(context.getRequest());
    }

    public static HttpServletRequest getThreadsServletRequest() {
        return requestStore.get();
    }

    public String getParameter(String paramName) {
        return getRequest().getParameter(paramName);
    }

    /**
     * Tries to create an instance of a different action bean and copy over all
     * the setters with the getters on this action bean
     *
     * @param <T>
     * @param abClass
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T extends ActionBean> T cloneToActionBean(Class<? extends ActionBean> abClass) {
        try {
            ActionBean ab = abClass.newInstance();
            ab.setContext(context);
            for (Method m : abClass.getMethods()) {
                if (!m.getName().startsWith("set")) {
                    continue;
                }
                // This is a setter
                try {
                    Method getterMethodOnThisClass = getClass().getMethod(m.getName().replaceFirst("set", "get"));
                    Object valueFromThisClass = getterMethodOnThisClass.invoke(this);
                    if (valueFromThisClass != null) {
                        m.invoke(ab, valueFromThisClass);
                    }
                } catch (Exception e) {
                    log.debug("Could not clone setter method [{}] Reason: [{}]", m.getName(), e.toString());
                }
            }
            return (T) ab;
        } catch (Exception e) {
            log.warn("Error: ", e);
            throw new RuntimeException("Error instantiating action bean");
        }
    }

    /**
     * Returns the IP address of the user requesting the page
     *
     * @return IPAddress
     */
    public String getOriginatingIP() {
        return Utils.getRemoteIPAddress(getRequest());
    }

    public void disableTracking() {
        Cookie removeTrackingCookie = new Cookie("AllowTracking", "false");
        if (getUser() != null) {
            CacheHelper.removeFromRemoteCache("Track_" + getUser().toLowerCase());
        }
        getContext().getResponse().addCookie(removeTrackingCookie);
    }

    public void enableTracking() {
        Cookie trackingCookie = new Cookie("AllowTracking", "true");
        CacheHelper.putInRemoteCache("Track_" + getUser().toLowerCase(), "Tracking has been enabled but no page has been viewed yet", 600);
        getContext().getResponse().addCookie(trackingCookie);
    }

    /**
     * Get the translated version of the resource key using the users Locale,
     * and adds it to the list of errors generated for the page. The generated
     * errors will typically be displayed on the top of the generated page for
     * the user to read
     *
     * @param errKey
     */
    public void localiseErrorAndAddToGlobalErrors(String errKey, String extraErrorInfo) {
        if (extraErrorInfo == null || extraErrorInfo.isEmpty()) {
            localiseErrorAndAddToGlobalErrors(errKey);
        } else {
            extraErrorInfo = extraErrorInfo.split("\\) on server instance")[0];
            this.addGlobalError(getLocalisedString(errKey) + " -- " + extraErrorInfo);
        }
    }

    public void localiseErrorAndAddToGlobalErrors(String errKey) {
        this.addGlobalError(getLocalisedString(errKey));
    }

    public void embedErrorIntoStipesActionBeanErrorMechanism(SCAErr e) {
        // Check if these are validation errors. If so split them apart
        // and add them one by one
        // If is not a validation error then try to get a localised error for the error code. If that fails, then use the unfriendly error
        log.debug("In embedErrorIntoStipesActionBeanErrorMechanism. Error desc is [{}]", e.getErrorDesc());
        if (e.getErrorCode().equals(Errors.ERROR_CODE_SCA_INVALID_MESSAGE)) {
            // Business errors are Validation errors and are delimited with a |
            String[] errs = e.getErrorDesc().split("\\|");
            for (int i = 0; i < errs.length; i++) {
                log.debug("Calling localiseErrorAndAddToGlobalErrors for [{}]", errs[i]);
                localiseErrorAndAddToGlobalErrors(errs[i]);
            }
        } else {
            // Not validation errors
            log.debug("Calling localiseErrorAndAddToGlobalErrors for [{}]", "error." + e.getErrorCode());
            if (e.getErrorDesc().contains("--")) {
                String extraInfo = e.getErrorDesc().split("--")[1].trim();
                localiseErrorAndAddToGlobalErrors("error." + e.getErrorCode(), extraInfo);
            } else {
                localiseErrorAndAddToGlobalErrors("error." + e.getErrorCode());
            }

        }
    }
    private String SCATXID;

    /**
     * Returns the unique TXID used in the last call to SCA
     *
     * @return TXID
     */
    public String getSCATXID() {
        return SCATXID;
    }

    private void setSCATXID(String SCATXID) {
        this.SCATXID = SCATXID;
    }

    /**
     * Generates an XML Bean SCAString object for the given simple String
     *
     * @param s
     * @return SCAString object
     */
    public static SCAString makeSCAString(String s) {
        SCAString ret = new SCAString();
        ret.setString(s);
        return ret;
    }

    /**
     * Generates an XML Bean SCAInteger object for the given simple int
     *
     * @param i
     * @return SCAInteger object
     */
    public static SCAInteger makeSCAInteger(int i) {
        SCAInteger ret = new SCAInteger();
        ret.setInteger(i);
        return ret;
    }

    public static SCALong makeSCALong(long l) {
        SCALong ret = new SCALong();
        ret.setLong(l);
        return ret;
    }

    /**
     * The user logged in to the java session
     *
     * @return User e.g. pcb (ie returns the SSOIdentity)
     */
    public String getUser() {
        String usr = context.getRequest().getRemoteUser();
        return usr;
    }

    private int getUserCustomerId() {
        CustomerQuery query = new CustomerQuery();
        query.setSSOIdentity(getUser());
        query.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        query.setResultLimit(1);
        Customer loggedInCustomer = SCAWrapper.getUserSpecificInstance().getCustomer(query);
        return loggedInCustomer.getCustomerId();
    }

    /**
     * Same as getUser() but this one will return the string passed in if nobody
     * is logged in
     *
     * @param notLoggedInName
     * @return String
     */
    public String getUser(String notLoggedInName) {
        String user = getUser();
        if (user == null) {
            return notLoggedInName;
        }
        return user;
    }
    private String tickerMessage;
    private Object[] tickerMessageParameters;

    public void addMessageToTicker(String tickerMessage, Object... params) {
        this.tickerMessage = tickerMessage;
        this.tickerMessageParameters = params;
    }

    public java.lang.String getTickerMessage() {
        return tickerMessage;
    }

    public Object[] getTickerMessageParameters() {
        return tickerMessageParameters;
    }
    private String pageMessage; // Message on the page indicating last action status e.g. "Customer added successfully"
    private Object[] pageMessageParameters;

    /**
     * Returns the String set with setPageMessage, unless if there are and
     * business errors in which case null is returned
     *
     * @return String or null
     */
    public String getPageMessage() {
        return pageMessage;
    }

    /**
     * Sets the message key on the page indicating last action status e.g.
     * "customer.added.successfully"
     *
     * @param pageMessage
     */
    public void setPageMessage(String pageMessage, Object... params) {
        this.pageMessage = pageMessage;
        this.pageMessageParameters = params;
    }

    /**
     * Returns the values passed in setPageMessageParameters
     *
     * @return Array
     */
    public Object[] getPageMessageParameters() {
        return pageMessageParameters;
    }

    /**
     * Returns the user agent of the calling device
     *
     * @return String User agent as it appears in the user agent header of the
     * HTTP request
     */
    public String getUserAgent() {
        return getContext().getRequest().getHeader(USER_AGENT_HEADER);
    }

    /**
     * Returns the path to the directory that has the JSP pages used to render
     * the responses for the callers user agent
     *
     * @param request
     * @return String
     */
    public static String getUserAgentDirectory(HttpServletRequest request) {
        String userAgent = request.getHeader(USER_AGENT_HEADER);
        return getUserAgentDirectory(userAgent);
    }

    /**
     * Returns the path to the directory that has the JSP pages used to render
     * the responses for the callers user agent
     *
     * @param userAgent
     * @return User agent directory
     */
    public static String getUserAgentDirectory(String userAgent) {
        String deviceFolder = null;
        try {
            if (userAgent.contains("Mobile")) {
                deviceFolder = BaseUtils.getProperty("global.mobile.device.directory");
            }
            //deviceFolder = BaseUtils.getProperty(GLOBAL + userAgent);           

        } catch (Exception e) {
        }
        if (deviceFolder == null) {
            // Fall back to use the default device folder
            deviceFolder = BaseUtils.getProperty("global.default.device.directory");
        }
        return deviceFolder;
    }

    /**
     * Create an event for audit trail purposes
     *
     * @param eventType
     * @param data
     */
    protected void createEvent(String eventType, String data) {
        //Event event = new Event();
        //event.setEventType(eventType);
        //event.setEventData(data);
        //event.setIdentityForEvent(getUser());
        // PCB TODO SCAWrapper.createEvent(event);
    }

    public boolean getAreValidationErrors() {
        if (getContext().getValidationErrors() != null && !getContext().getValidationErrors().isEmpty()) {
            return true;
        }
        return false;
    }

    public void addErrorToValidationErrors(SCABusinessError e) {
        localiseErrorAndAddToGlobalErrors("error." + e.getErrorCode());
    }

    public void clearValidationErrors() {
        getContext().getValidationErrors().clear();
    }

    /**
     * Adds the error to the list of errors Stripes is storing for the page
     *
     * @param desc
     */
    private void addGlobalError(String desc) {
        try {
            log.debug("Adding global validation error with msg [{}] into stripes validation error list", desc);
            SimpleError e = new SimpleError(desc);
            getContext().getValidationErrors().addGlobalError(e);
            // Allow clicking the back button and trying again without getting a double post error
            getContext().getRequest().getSession().setAttribute("LASTFORMCOUNTERUSED", 0);
        } catch (Exception e) {
            log.warn("Error adding global error in Stripes: " + e.toString());
        }
    }
    private static String ctxPath = null;

    public Resolution getDDForwardResolution(Class<? extends ActionBean> beanType, String event) {
        ForwardResolution fr = new ForwardResolution(beanType, event);
        return fr;
    }

    public Resolution getDDForwardResolution(Class<? extends ActionBean> beanType, String event, String pageMessage) {
        ForwardResolution forward = new ForwardResolution(beanType, event);
        forward.addParameter("pageMessage", pageMessage);
        return forward;
    }

    /**
     * To be used instead of the standard Stripes ForwardResolution() Our
     * version does user agent matching and returns a device specific Resolution
     * (only for /scp pages - returns defaultjsp for others). returns the source
     * page if there are any business errors for the page. To avoid this, call
     * clearBusinessErrors
     *
     * @param defaultjsp The standard JSP page for browsers E.g.
     * /customer_management/view_customer.jsp
     */
    public Resolution getDDForwardResolution(String defaultjsp) {

        if (ctxPath == null && this.getContext() != null) {
            ctxPath = this.getContext().getServletContext().getContextPath();
        }
        // Only use device dependent rendering on SCP
        if (!ctxPath.startsWith("/scp") && !ctxPath.startsWith("/tmpscp")) {
            return new ForwardResolution(defaultjsp);
        }

        Set specificPages = null;
        try {
            specificPages = BaseUtils.getPropertyAsSet("env.scp.countryspecific.pages");
        } catch (Exception e) {
        }
        if (specificPages != null && specificPages.contains(defaultjsp)) {
            //defaultjsp = defaultjsp.replace(".jsp", "_" + BaseUtils.getProperty("env.locale.country.for.language.en") + ".jsp");
            defaultjsp = defaultjsp.replace(".jsp", "_" + "UG" + ".jsp");
        }
        String deviceFolder = getUserAgentDirectory(getUserAgent());
        String jsp = deviceFolder + defaultjsp;

        return new ForwardResolution(jsp);
    }

    public String getDDForwardResolutionAsString(String defaultjsp) {
        return defaultjsp;
    }
    /*
     Entity Action stuff
     Uses reflection to call an action based on a form submitting a request
     to performEntityAction passing a parameter entityAction which is the action to call
     Used to call actions in a drop down list
     */
    private String entityAction;

    public String getEntityAction() {
        return entityAction;
    }

    public void setEntityAction(String entityAction) {
        this.entityAction = entityAction;
    }

    public Resolution performEntityAction() throws Throwable {
        Method m = null;
        Resolution res = null;
        // Find the method on the action bean
        try {
            m = this.getClass().getMethod(getEntityAction());
        } catch (Exception e) {
            throw new SCASystemError();
        }
        // Call the method
        try {
            res = (Resolution) m.invoke(this);
        } catch (Exception e) {
            // Get the underlying exception and throw it (as would happen with the normal action bean calls
            Throwable underlying = e.getCause();
            if (underlying == null) {
                throw e;
            } else {
                throw underlying;
            }
        }
        return res;
    }

    public List<Set<String>> getPermissions() {
        List<Set<String>> permissions = new ArrayList<Set<String>>();
        for (String role : getUsersRoles()) {
            permissions.add(getRolesPermissions(role));
        }
        return permissions;
    }

    /**
     * returns a list of the friendly roles that the logged in user is in (but
     * only ones that exist in web.xml) e.g. no Customer role in SEP!
     *
     * @return List of Strings
     */
    public Set<String> getUsersRoles() {
        return getUsersRoles(getContext().getRequest());
    }

    public Set<String> getUsersRoles(HttpServletRequest request) {
        Set<String> ret = new HashSet<String>();
        for (String role : getRolesInWebXML()) {
            if (request.isUserInRole(role)) {
                ret.add(Utils.getFriendlyRoleName(role));
            }
        }
        return ret;
    }

    /**
     * Returns true if any the logged in users roles is found in the \r\n
     * delimited list provided
     *
     * @param roles
     * @return
     */
    public boolean isAllowed(String roles) {
        Set usersRoles = getUsersRoles();
        StringTokenizer stValues = new StringTokenizer(roles, "\r\n");
        while (stValues.hasMoreTokens()) {
            String role = stValues.nextToken().trim();
            if (!role.isEmpty() && usersRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAllowedEmptyAllowsAll(String roles) {
        if (roles.isEmpty()) {
            return true;
        }
        Set usersRoles = getUsersRoles();
        StringTokenizer stValues = new StringTokenizer(roles, "\r\n");
        while (stValues.hasMoreTokens()) {
            String role = stValues.nextToken().trim();
            if (!role.isEmpty() && usersRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a list of permissions as per property
     * env.portal.allowed.resources."role_name"
     *
     * @param role
     * @return list
     */
    public Set<String> getRolesPermissions(String role) {
        return BaseUtils.getPropertyAsSet(RESOURCES_KEY_PREFIX + role);
    }

    /**
     * Throws a InsufficientPrivilegesException f the user cant access the
     * resource. Uses getRolesPermissions etc.
     *
     * @param resource
     * @throws com.smilecoms.commons.stripes.InsufficientPrivilegesException
     */
    public void checkPermissions(Object resourceObj) throws InsufficientPrivilegesException {
        String resource = resourceObj.toString();
        if (log.isDebugEnabled()) {
            log.debug("About to check permissions for user " + getUser() + " accessing resource " + resource);
        }
        List<String> webXMLRoles = getRolesInWebXML();
        for (String role : webXMLRoles) {
            if (getContext().getRequest().isUserInRole(role)) {
                String friendlyRole = Utils.getFriendlyRoleName(role);
                if (isRoleAllowedResource(friendlyRole, resource)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Sufficient permissions for user " + getUser() + " accessing resource " + resource + " using role " + friendlyRole);
                    }
                    return;
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Insufficient permissions for user " + getUser() + " accessing resource " + resource);
        }
        throw new InsufficientPrivilegesException(resource);
    }

    public boolean hasPermissions(Object resourceObj) {
        try {
            checkPermissions(resourceObj);
        } catch (InsufficientPrivilegesException isp) {
            return false;
        }
        return true;
    }
    public static List<String> rolesinWebXML = null;

    private List<java.lang.String> getRolesInWebXML() {
        if (rolesinWebXML != null) {
            return rolesinWebXML;
        }
        log.debug("Getting roles from web.xml");

        ByteArrayInputStream fIS = null;
        try {
            fIS = (ByteArrayInputStream) getContext().getServletContext().getResourceAsStream("/WEB-INF/web.xml");
            if (fIS == null) {
                log.warn("Cannot locate web.xml to read security roles");
            }
            DataInputStream in = new DataInputStream(fIS);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            rolesinWebXML = new CopyOnWriteArrayList<>();
            //Read File Line By Line
            while ((strLine = br.readLine()) != null) {
                // Print the content on the console
                int roleNameLoc = strLine.indexOf("<role-name>");
                if (roleNameLoc != -1) {
                    int roleStart = roleNameLoc + 11;
                    int endTag = strLine.indexOf("</role-name>");
                    String role = strLine.substring(roleStart, endTag);
                    log.debug("Found a role in web.xml called [{}]", role);
                    rolesinWebXML.add(role);
                }
                if (strLine.contains("</auth-constraint>")) {
                    // finished parsing roles
                    break;
                }
            }
        } catch (Exception e) {
            rolesinWebXML = null;
            log.warn("error", e);
        } finally {
            try {
                if (fIS != null) {
                    fIS.close();
                }
            } catch (IOException ex) {
            }
        }
        return rolesinWebXML;
    }

    /**
     * Role is the friendly role name e.g. Administrator
     *
     * @param role
     * @param resource
     * @return
     */
    private boolean isRoleAllowedResource(String role, String resource) {
        return getRolesPermissions(role).contains(resource);
    }

    /**
     * Check if the logged-in user has Indirect Channel Partner's roles e.g.
     * ICPManager
     *
     * @return
     */
    public boolean getIsIndirectChannelPartner() {
        return getIsIndirectChannelPartner(getContext().getRequest());
    }

    public static boolean getIsIndirectChannelPartner(HttpServletRequest request) {
        for (String icpRole : BaseUtils.getPropertyAsList("env.icp.allowed.roles")) {
            if (request.isUserInRole(Utils.getUnfriendlyRoleName(icpRole))) {
                return true;
            }
        }
        return false;
    }

    public boolean getIsDeliveryPerson() {
        return getIsDeliveryPerson(getContext().getRequest());
    }

    public static boolean getIsDeliveryPerson(HttpServletRequest request) {
        return request.isUserInRole(Utils.getUnfriendlyRoleName("Delivery"));
    }

    public int getOrganistionIdOfUserInSession() {
        Integer orgId = (Integer) getContext().getRequest().getSession().getAttribute(SESSION_KEY_CUSTOMER_ORG_ID);
        if (orgId == null) {
            return setOrganistionIdOfUserInSession();
        } else {
            return orgId;
        }
    }

    private int setOrganistionIdOfUserInSession() {
        CustomerQuery query = new CustomerQuery();
        query.setSSOIdentity(getUser());
        query.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        query.setResultLimit(1);
        Customer loggedInCustomer = SCAWrapper.getUserSpecificInstance().getCustomer(query);
        int orgId = 0;
        HttpSession session = getContext().getRequest().getSession();

        for (CustomerRole role : loggedInCustomer.getCustomerRoles()) {
            orgId = role.getOrganisationId();
            break;
        }
        session.setAttribute(SESSION_KEY_CUSTOMER_ORG_ID, (Integer) orgId);
        return orgId;
    }

     // Confirmation form
    private boolean confirmed;
    private int systemOtp;
    private boolean otpSupplied=false;
    private String otp;
    private String postConfirmationAction;
    private String confirmationMessageKey;
    private String postConfirmationSubmit;

    public java.lang.String getPostConfirmationSubmit() {
        return postConfirmationSubmit;
    }

    public java.lang.String getConfirmationMessageKey() {
        return confirmationMessageKey;
    }

    public java.lang.String getPostConfirmationAction() {
        return postConfirmationAction;
    }

    public boolean getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public boolean notConfirmed() {
        return !confirmed;
    }

    public java.lang.String getOtp() {
        return otp;
    }

    public void setOtp(java.lang.String otp) {
        this.otp = otp;
    }    

    public boolean isOtpSupplied() {
        return otpSupplied;
    }

    public void setOtpSupplied(boolean otpSupplied) {
        this.otpSupplied = otpSupplied;
    }

    public int getSystemOtp() {
        return systemOtp;
    }

    public void setSystemOtp(int systemOtp) {
        this.systemOtp = systemOtp;
    }
    
    
    public Resolution otpConfirm(String sendOtpTo, String... parameter) {
        StackTraceElement element = Thread.currentThread().getStackTrace()[2];
        postConfirmationSubmit = element.getMethodName();
        confirmationMessageKey = "otp.confirm." + postConfirmationSubmit;
        
        
        String className = element.getClassName();
        int lastDot = className.lastIndexOf(".");
        postConfirmationAction = "/" + className.substring(lastDot + 1).replace("ActionBean", ".action");
        
        //Populate parameters
        confirmationMessageParams = new ArrayList<>();
        confirmationMessageParams.addAll(Arrays.asList(parameter));
        
        HttpSession session = getContext().getRequest().getSession(); 
        session.setAttribute("sendOtpTo",sendOtpTo);
        
        generateOtp();
        setConfirmed(false);
        return getDDForwardResolution("/layout/otpconfirmation.jsp");
        
    }
    
     
    public void generateOtp() {    
    //Set session otp
        setConfirmed(false);
        Random r = new Random( System.currentTimeMillis() );    
        HttpSession session = getContext().getRequest().getSession();               
        session.setAttribute("systemOtp", ((1 + r.nextInt(2)) * 10000 + r.nextInt(10000)));
        
        Event eventData = new Event();
        eventData.setEventType("SRA");
        eventData.setEventSubType("OTP_Notification");
        eventData.setEventKey(session.getAttribute("sendOtpTo").toString());
        eventData.setEventData(session.getAttribute("systemOtp").toString());
        SCAWrapper.getAdminInstance().createEvent(eventData);                
        
    }
     
    public boolean  verifiedOTP(String systemOTP, String userOTP) {    
    //Set session otp
        boolean result=true;
        setConfirmed(false);
        
        if(systemOTP==null || userOTP.trim().length()==0 || (Integer.parseInt(systemOTP.toString().trim()) != Integer.parseInt(userOTP.trim()))) {
            result = false;
        }
        
        return result;
    }
        
    public Resolution confirm(String... parameter) {
        StackTraceElement element = Thread.currentThread().getStackTrace()[2];
        postConfirmationSubmit = element.getMethodName();
        confirmationMessageKey = "confirm." + postConfirmationSubmit;
        String className = element.getClassName();
        int lastDot = className.lastIndexOf(".");
        postConfirmationAction = "/" + className.substring(lastDot + 1).replace("ActionBean", ".action");

        //Populate parameters
        confirmationMessageParams = new ArrayList<>();
        confirmationMessageParams.addAll(Arrays.asList(parameter));
        return getDDForwardResolution("/layout/confirmation.jsp");
    }
    
    
    private List<String> confirmationMessageParams;

    public List<String> getConfirmationMessageParams() {
        return confirmationMessageParams;
    }
    
    
    
    
    //------------------

    public StreamingResolution getJSONResolution(Object obj) {
        String jsonString = gson.toJson(obj);
        log.debug("Json String is [{}]", jsonString);
        return new StreamingResolution("application/json", jsonString);
    }

    /**
     * Create a new cookie and attach it to the HTTP response object of the
     * servlet.
     *
     * @param name - name of the cookie parameter
     * @param value - param value
     * @param maxAge - max age in seconds
     */
    public void createCookie(String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(maxAge);
        getContext().getResponse().addCookie(cookie);
    }

    /**
     * Get cookie with requested name from request object
     *
     * @param name - name of cookie to find
     * @return cookie if found, null if not found
     */
    public Cookie getCookie(String name) {
        return getCookie(getContext().getRequest(), name);
    }

    public static Cookie getCookie(HttpServletRequest request, String name) {
        Cookie cookie = null;
        if (request == null || request.getCookies() == null) {
            return null;
        }
        for (Cookie c : request.getCookies()) {
            if (name != null && name.equals(c.getName())) {
                cookie = c;
                break;
            }
        }
        return cookie;
    }

    public void deleteCookie(String name) {
        createCookie(name, "", 0);
    }
    
    public boolean isMegaDealer(int orgId) {
        if (orgId == 0) {
            return false;
        }
        // Look in the discount code comments to get the info
        String discCode = BaseUtils.getProperty("env.pos.discount.code");
        // 12345 ACC: 1307000001 DISC: 15% TYPE: MD
        int configStart = discCode.indexOf(" " + orgId + " ACC: ");
        if (configStart == -1) {
            return false;
        }
        String bit = discCode.substring(configStart);
        int typeStart = bit.indexOf(" TYPE: ") + 7;
        if (typeStart == -1) {
            return false;
        }
        bit = bit.substring(typeStart).trim();
        return bit.startsWith("MD");
    }

    public boolean isSuperDealer(int orgId) {
        if (orgId == 0) {
            return false;
        }
        // Look in the discount code comments to get the info
        String discCode = BaseUtils.getProperty("env.pos.discount.code");
        // 12345 ACC: 1307000001 DISC: 15% TYPE: SD
        // 12347 ACC: 1307000002 DISC: 10% TYPE: ICP
        int configStart = discCode.indexOf(" " + orgId + " ACC: ");
        if (configStart == -1) {
            return false;
        }
        String bit = discCode.substring(configStart);
        int typeStart = bit.indexOf(" TYPE: ") + 7;
        if (typeStart == -1) {
            return false;
        }
        bit = bit.substring(typeStart).trim();
        return bit.startsWith("SD");
    }

    public boolean isFranchise(int orgId) {
        if (orgId == 0) {
            return false;
        }
        // Look in the discount code comments to get the info
        String discCode = BaseUtils.getProperty("env.pos.discount.code");
        // 12345 ACC: 1307000001 DISC: 15% TYPE: SD
        // 12347 ACC: 1307000002 DISC: 10% TYPE: ICP
        // 12347 ACC: 1307000002 DISC: 10% TYPE: FRA
        int configStart = discCode.indexOf(" " + orgId + " ACC: ");
        if (configStart == -1) {
            return false;
        }
        String bit = discCode.substring(configStart);
        int typeStart = bit.indexOf(" TYPE: ") + 7;
        if (typeStart == -1) {
            return false;
        }
        bit = bit.substring(typeStart).trim();
        return bit.startsWith("FRA");
    }
    
    public boolean isICP(int orgId) {
        if (orgId == 0) {
            return false;
        }
        // Look in the discount code comments to get the info
        String discCode = BaseUtils.getProperty("env.pos.discount.code");
        // 12345 ACC: 1307000001 DISC: 15% TYPE: SD
        // 12347 ACC: 1307000002 DISC: 10% TYPE: ICP
        int configStart = discCode.indexOf(" " + orgId + " ACC: ");
        if (configStart == -1) {
            return false;
        }
        String bit = discCode.substring(configStart);
        int typeStart = bit.indexOf(" TYPE: ") + 7;
        if (typeStart == -1) {
            return false;
        }
        bit = bit.substring(typeStart).trim();
        return bit.startsWith("ICP");
    }
}
