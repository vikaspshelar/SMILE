package com.smilecoms.commons.filter;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.cache.CacheHelper;
import com.smilecoms.commons.base.ops.Syslog;
import com.smilecoms.commons.base.ops.SyslogDefs;
import com.smilecoms.commons.sca.CallersRequestContext;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.stripes.SmileActionBean;
import com.smilecoms.commons.util.Utils;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.regex.Pattern;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Does 2 main things: 1) Logs page load times and send events to ops when slow
 * pages are encountered 2) Only allows requests to go downstream if the filter
 * thinks that all is well so as to prevent flooding a broken system with
 * requests
 *
 * @author PCB
 */
public class SmileUtilityFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(SmileUtilityFilter.class);
    private static final String PROP_KEY_MAX_TIME = "global.http.maxtimemillis.warning";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }

    /**
     * Returns the IP address of the user requesting the page
     *
     * @return IPAddress
     */
    private String getOriginatingIP(HttpServletRequest request) {
        return Utils.getRemoteIPAddress(request);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpResp = (HttpServletResponse) response;
        SmileResponseWrapper smileResp = new SmileResponseWrapper(httpReq, httpResp);
        // Test if we can get a property. If not, then all is not well and the request should not be serviced.
        if (!isAllOk()) {
            HttpServletResponse resp = (HttpServletResponse) response;
            resp.sendError(HttpServletResponse.SC_CONFLICT);
            return;
        }

        if (log.isDebugEnabled()) {
            logRequestDetail(httpReq, Level.FINEST);
        }

        if (httpReq.getDispatcherType().equals(DispatcherType.REQUEST)) {
            // Clear SCA calling information so that details of all SCA calls from this point on (on this thread) are collected in SCAWrapper for reporting
            SCAWrapper.getUserSpecificInstance().clearSCACallInfo();
        }

        String IP = getOriginatingIP(httpReq);

        if (BaseUtils.getBooleanProperty("global.portal.checksessionhijacking")) {
            String sessionIP = (String) httpReq.getSession().getAttribute(SmileActionBean.SESSION_KEY_IP_ADDRESS);

            if (sessionIP == null) {
                httpReq.getSession().setAttribute(SmileActionBean.SESSION_KEY_IP_ADDRESS, IP);
                sessionIP = IP;
            }

            if (IP != null && (!IP.equals(sessionIP))) {
                // Try and prevent session hijacking by verifying that the users IP and User Agent have not changed
                log.warn("A user has attempted a session hijacking as the ip has changed for a session. Requests IP [{}], Expected IP [{}]. Requests Session ID [{}]",
                        new Object[]{IP, sessionIP, httpReq.getRequestedSessionId()});

                SmileActionBean.invalidateSession(httpReq);
                httpResp.sendRedirect("/");
                return;
            }
        }

        if (httpReq.getRemoteUser() != null && httpReq.getSession().getAttribute("FAILOVER") == null) {
            // This is an authenticated session. Lets make sure we have something in the session so its replicated
            log.debug("Setting session to require failover [{}]", httpReq.getSession().getId());
            httpReq.getSession().setAttribute("FAILOVER", true);
        }

        if (httpReq.getRemoteUser() != null) {
            httpReq.getSession().setMaxInactiveInterval(1800);
        } else {
            httpReq.getSession().setMaxInactiveInterval(300);
        }

        if (BaseUtils.getBooleanProperty("global.portal.enforce.singlemachine.login", false)
                && httpReq.getRemoteUser() != null
                && !httpReq.getRemoteUser().equals("admin")
                && httpReq.getContextPath().equals("/sep"))
        {
            
            String sessionId = httpReq.getRequestedSessionId();
            String ssoId = httpReq.getRemoteUser();
            String sessionIP;
            String sessionIPandID;
            String ua = httpReq.getHeader("user-agent") == null ? "unknown" : httpReq.getHeader("user-agent");

            try {
                sessionIPandID = (String) CacheHelper.getFromRemoteCache(ssoId + "_SINGLE_MACHINE_LOGIN");

                if (sessionIPandID == null) {

                    sessionIP = (String) httpReq.getSession().getAttribute(SmileActionBean.SESSION_KEY_IP_ADDRESS);
                    if (sessionIP == null) {
                        sessionIP = IP;
                    }
                    sessionIPandID = sessionIP + "|" + sessionId + "|" + ua;
                    log.debug("Putting sessionIPandID [{}] in remote cache for user [{}]", sessionIPandID, ssoId);
                    CacheHelper.putInRemoteCacheSync(ssoId + "_SINGLE_MACHINE_LOGIN", sessionIPandID, httpReq.getSession().getMaxInactiveInterval());
                }

                String[] data = sessionIPandID.split(Pattern.quote("|"));

                if (isSameUserInDiffMachine(IP, sessionId, data)) {
                    // Try and prevent login from a different machine by verifying that the users IP has not changed for the current session
                    log.warn("A user has attempted a login from different machine. Requests IP [{}], Expected IP [{}]. Requests Session ID [{}]",
                            new Object[]{IP, data[0], httpReq.getRequestedSessionId()});
                    try {
                        CacheHelper.removeFromRemoteCache(httpReq.getSession().getId());
                    } catch (Exception e) {
                        log.warn("Error removing session info from remote cache for different machine login attempt: ", e);
                    }
                    try {
                        httpReq.getSession().invalidate();
                    } catch (Exception e) {
                        log.debug("Error in invalidateSession for different machine login attempt: [{}]", e.toString());
                    }

                    HttpServletResponse resp = (HttpServletResponse) response;
                    resp.sendError(444, "Login from multiple machines forbidden");
                    return;
                }
            } catch (Exception e) {
                log.warn("Error validating preventing multi-machine login. Will continue as normal: {}", e.toString());
            }
        }

        long start = System.currentTimeMillis();
        try {
            log.debug("Calling SCA setThreadsServletRequest");
            SCAWrapper.getUserSpecificInstance().setThreadsRequestContext(new CallersRequestContext(httpReq));
            chain.doFilter(request, smileResp);
        } finally {
            log.debug("Calling SCA removeThreadsServletRequest");
            SCAWrapper.removeThreadsRequestContext();
        }

        long end = System.currentTimeMillis();
        long callTime = end - start;

        String requestString = null;
        if (log.isDebugEnabled()) {
            requestString = httpReq.getRequestURI() + (httpReq.getQueryString() == null ? "" : ("?" + httpReq.getQueryString()));
            log.debug("Finished processing request " + requestString + ". Requesting customer was " + httpReq.getRemoteUser() + " at IP " + IP + " Request took " + callTime + "ms");
        }

        // Send request info to syslog
        if (BaseUtils.getBooleanProperty("env.send.httprequest.syslog", false)) {
            String requestPath = httpReq.getMethod() + " " + httpReq.getRequestURI() + " " + httpReq.getProtocol();
            logRequest(IP, httpReq.getRemoteUser(), requestPath, "200", "1024", httpReq.getHeader("referrer"), httpReq.getHeader("user-agent"), "ms=" + callTime);
        }

        int maxTime = 30000;
        try {
            maxTime = BaseUtils.getIntProperty(PROP_KEY_MAX_TIME);
        } catch (Exception e) {
            log.warn("Error getting Smile Property " + PROP_KEY_MAX_TIME + ". Defaulting to 30000ms");
        }

        if (callTime > maxTime) {
            if (requestString == null) {
                requestString = httpReq.getRequestURI() + (httpReq.getQueryString() == null ? "" : ("?" + httpReq.getQueryString()));
            }
            if (!mustNotLogSlow(requestString)) {
                String level = BaseUtils.MINOR;
                if (httpReq.getMethod().equalsIgnoreCase("POST")) {
                    // Posts can be slow if the client takes a long time to post the data so log at a lower level
                    level = BaseUtils.INFO;
                }
                log.warn("Request took " + callTime + "ms. Max call time of " + maxTime + "ms exceeded processing " + httpReq.getMethod() + " request " + requestString + ". Requesting customer was " + httpReq.getRemoteUser() + " at IP " + IP + ". Request details:");
                logRequestDetail(httpReq, Level.WARNING);
                BaseUtils.sendTrapToOpsManagement(
                        level,
                        "Web Server",
                        "HTTP Request took " + callTime + "ms. Max call time of " + maxTime + "ms exceeded processing " + httpReq.getMethod() + " request " + requestString + ". Requesting customer was " + httpReq.getRemoteUser() + " at IP " + IP + ". Look in server log for request details");
            }
        }

    }

    private boolean isAllOk() {
        boolean ret = true;
        if (!BaseUtils.isSCAAvailable() || !BaseUtils.isPropsAvailable()) {
            log.warn("This server has no SCA endpoint available or property framework is not available. Server will report itself as being down");
            ret = false;
        }
        return ret;
    }

    private void writeToSyslog(String msg) {
        try {
            Syslog.sendSyslog(BaseUtils.getProperty("env.syslog.hostname"), "SMILE-WEB", SyslogDefs.LOG_LOCAL4, SyslogDefs.LOG_INFO, msg);
        } catch (Exception e) {
            log.error("Error", e);
        }
    }

    private void logRequest(String ip, String customer, String request, String statuscode, String bytes, String referrer, String ua, String cookies) {

        Format formatter = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z");
        Date date = new Date();
        String datetime = formatter.format(date);

        StringBuilder buf = new StringBuilder();
        buf.append(ip);
        buf.append(" - ");
        buf.append(customer == null ? "-" : customer);
        //01/Jul/2002:12:11:52 +0000
        buf.append(" [").append(datetime).append("]");
        buf.append(" \"").append(request).append("\" ");
        buf.append(statuscode);
        buf.append(" ");
        buf.append(bytes);
        buf.append(" \"").append(referrer == null ? "" : referrer).append("\" ");
        buf.append("\"").append(ua == null ? "" : ua).append("\" ");
        buf.append("\"").append(cookies).append("\"");

        writeToSyslog(buf.toString());
    }

    /**
     * Compiles the given regular expression and attempts to match the given
     * input against it.
     *
     * @param field The character sequence to be matched
     * @param regex The expression to be compiled
     * @return boolean value
     */
    private boolean mustNotLogSlow(String page) {
        String regex;
        try {
            regex = BaseUtils.getProperty("global.http.maxtimemillis.skip.regex");
        } catch (Exception e) {
            log.warn("Could not get property global.http.maxtimemillis.skip.regex. Will default to XXX");
            regex = "XXX";
        }
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(page).find();
    }

    private void logRequestDetail(HttpServletRequest httpReq, Level l) {
        try {
            String requestString = httpReq.getRequestURI() + (httpReq.getQueryString() == null ? "" : ("?" + httpReq.getQueryString()));
            log("Request is " + requestString + ". Requesting customer is " + httpReq.getRemoteUser() + " at IP " + getOriginatingIP(httpReq), l);
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
            String sessionLocation = "<not passed in>";
            String valid = "invalid.";
            if (httpReq.isRequestedSessionIdValid()) {
                valid = "valid.";
            }
            String id = null;
            if (httpReq.isRequestedSessionIdFromCookie()) {
                sessionLocation = "cookie";
                id = getCookieValue(httpReq, "JSESSIONID");
            }
            if (httpReq.isRequestedSessionIdFromURL()) {
                sessionLocation = "URL";
                id = httpReq.getRequestedSessionId();
            }
            log("The session id passed in was in a " + sessionLocation + " and was: " + id + ". Its " + valid, l);
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

    public static String getCookieValue(HttpServletRequest request, String name) {
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
        return cookie == null ? null : cookie.getValue();
    }

    private boolean isSameUserInDiffMachine(String IP, String sessionId, String[] data) {
        if (IP != null && (!IP.equals(data[0]))) {
            // Try and prevent login from a different machine by verifying that the users IP has not changed for the current session
            return true;
        }
        //If IP is good then check the sessionId if still the same
        return (!sessionId.equals(data[1]));
    }

}
