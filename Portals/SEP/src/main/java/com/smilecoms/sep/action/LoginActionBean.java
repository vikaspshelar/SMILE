package com.smilecoms.sep.action;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.CustomerQuery;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.stripes.*;
import com.smilecoms.commons.util.Utils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.sourceforge.stripes.action.*;

/**
 * Stripes action bean for doing a SIP register
 *
 * @author PCB
 */
public class LoginActionBean extends SmileActionBean {

    /* *************************************************************************************
     Actions to do something with the instance data and then send the browser somewhere
     ************************************************************************************** */
    private List<Set<String>> permissions;
    private String username;
    private String password;

    public void setPassword(java.lang.String password) {
        this.password = password;
    }

    public void setUsername(java.lang.String username) {
        this.username = username;
    }

    public java.lang.String getUsername() {
        return username;
    }

    public Resolution isup() {
        invalidateSession();
        return new StreamingResolution("text", new StringReader("Yes SEP is UP"));
    }
    
    public Resolution showPermissions() {
        return getDDForwardResolution("/permissions/permissions.jsp");
    }

    public Resolution logOut() {
        // Invalidate session
        invalidateSession();
        disableTracking();
        // Send to login
        return new RedirectResolution("/login.jsp");
    }

    public Resolution abuse() {
        // Invalidate session
        invalidateSession();
        // Send to login
        BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "HAPROXY", "User at IP " + Utils.getRemoteIPAddress(getRequest()) + " is abusing the web sites");
        return new RedirectResolution(BaseUtils.getProperty("env.portals.brute.force.redirection.url", "http://google.com"), false);
    }

    public Resolution showSendResetLink() {
        return getDDForwardResolution("/request_reset.jsp");
    }

    public Resolution sendResetLink() {
        try {
            log.warn("Sending reset link for [{}]", getSSOPasswordResetLinkData().getIdentifier());
            SCAWrapper.getAdminInstance().sendSSOPasswordResetLink(getSSOPasswordResetLinkData());
            setPageMessage("reset.link.sent");
            return getDDForwardResolution("/login.jsp");
        } catch (Exception e) {
            localiseErrorAndAddToGlobalErrors("user.not.found.for.password.reset");
            return showSendResetLink();
        }
    }

    @DefaultHandler
    @DontValidate
    public Resolution login() {
        clearValidationErrors();
        disableTracking(); // Ensure tracking is off cause we get a blank page if you log in with tracking enabled

        if (username != null && username.isEmpty()) {
            localiseErrorAndAddToGlobalErrors("invalid.login.credentials");
            return getDDForwardResolution("/login_error.jsp");
        }

        if (username == null || password == null) {
            // Somehow got to login action without submitting the login form
            return getDDForwardResolution("/login.jsp");
        }
        
        try {
            checkCSRF();
            getRequest().login(username, password);
        } catch (Exception e) {
            if (password.isEmpty()) {
                // Check if this is a first time login
                log.debug("Checking if [{}] has an empty password", username);
                CustomerQuery customerQuery = new CustomerQuery();
                customerQuery.setSSOIdentity(username);
                customerQuery.setResultLimit(1);
                customerQuery.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
                try {
                    setCustomer(SCAWrapper.getAdminInstance().getCustomer(customerQuery));
                    if (getCustomer().getSSODigest().isEmpty()) {
                        log.debug("Customer tried to log in with a blank password, and the customers password in the backend is blank");
                        return getDDForwardResolution("/reset_password.jsp");
                    }
                } catch (Exception ex) {
                    log.debug("Error logging in user: ", ex);
                    e = ex;
                }
            }

            log.debug("Error logging in user: ", e);
            localiseErrorAndAddToGlobalErrors("invalid.login.credentials");
            return getDDForwardResolution("/login_error.jsp");
        }

        return new RedirectResolution("/index.jsp");
    }

    public Resolution goHome() {
        return getDDForwardResolution("/index.jsp");
    }

    public Resolution performXSLTTransform() {
        log.debug("performXSLTTransform");

        String errorResponse = "";
        String response = "";
        String xslt = "";
        try {
            BufferedReader br = getRequest().getReader();
            String line;
            while ((line = br.readLine()) != null) {
                log.debug("br line: [{}]", line);
                xslt = xslt + line;
            }
        } catch (IOException ex) {
            errorResponse = "Can't get XML input stream";
            log.debug(errorResponse);
            return new StreamingResolution("text", new StringReader(errorResponse));
        }
        if (xslt.isEmpty()) {
            errorResponse = "No XSLT passed";
            log.debug(errorResponse);
            return new StreamingResolution("text", new StringReader(errorResponse));
        }

        String toReplace = "<env.email.template.mainhead>";
        if (xslt.contains(toReplace)) {
            String replacementText = BaseUtils.getProperty(toReplace.substring(1, toReplace.length() - 1));
            log.debug("Replacing: [{}] with: [{}]", toReplace, replacementText);
            xslt = xslt.replace(toReplace, replacementText);
        }

        toReplace = "<env.email.template.salutation1>";
        if (xslt.contains(toReplace)) {
            String replacementText = BaseUtils.getProperty(toReplace.substring(1, toReplace.length() - 1));
            log.debug("Replacing: [{}] with: [{}]", toReplace, replacementText);
            xslt = xslt.replace(toReplace, replacementText);
        }

        toReplace = "<env.email.template.salutation2>";
        if (xslt.contains(toReplace)) {
            String replacementText = BaseUtils.getProperty(toReplace.substring(1, toReplace.length() - 1));
            log.debug("Replacing: [{}] with: [{}]", toReplace, replacementText);
            xslt = xslt.replace(toReplace, replacementText);
        }

        toReplace = "<env.email.template.mainfooter>";
        if (xslt.contains(toReplace)) {
            String replacementText = BaseUtils.getProperty(toReplace.substring(1, toReplace.length() - 1));
            log.debug("Replacing: [{}] with: [{}]", toReplace, replacementText);
            xslt = xslt.replace(toReplace, replacementText);
        }

        toReplace = "env.email.template.mainfooter.no.customer.details";
        if (xslt.contains(toReplace)) {
            String replacementText = BaseUtils.getProperty(toReplace.substring(1, toReplace.length() - 1));
            log.debug("Replacing: [{}] with: [{}]", toReplace, replacementText);
            xslt = xslt.replace(toReplace, replacementText);
        }

        toReplace = "<env.email.template.banner1>";
        if (xslt.contains(toReplace)) {
            String replacementText = BaseUtils.getProperty(toReplace.substring(1, toReplace.length() - 1));
            log.debug("Replacing: [{}] with: [{}]", toReplace, replacementText);
            xslt = xslt.replace(toReplace, replacementText);
        }

        toReplace = "<env.email.template.banner2>";
        if (xslt.contains(toReplace)) {
            String replacementText = BaseUtils.getProperty(toReplace.substring(1, toReplace.length() - 1));
            log.debug("Replacing: [{}] with: [{}]", toReplace, replacementText);
            xslt = xslt.replace(toReplace, replacementText);
        }

        toReplace = "<env.email.template.bodyhead>";
        if (xslt.contains(toReplace)) {
            String replacementText = BaseUtils.getProperty(toReplace.substring(1, toReplace.length() - 1));
            log.debug("Replacing: [{}] with: [{}]", toReplace, replacementText);
            xslt = xslt.replace(toReplace, replacementText);
        }

        log.debug("XSLT: [{}]", xslt);

        String contextXML = BaseUtils.getProperty("env.customerlifecycle.test.xml", "");
        if (contextXML.isEmpty()) {
            errorResponse = "No contextXML defined";
            log.debug(errorResponse);
            return new StreamingResolution("text", new StringReader(errorResponse));
        }
        log.debug("ContextXML: [{}]", contextXML);

        try {
            response = Utils.doXSLTransform(contextXML, xslt, getClass().getClassLoader());
        } catch (Exception ex) {
            errorResponse = ex.toString();
            log.debug("Error transforming: [{}]", errorResponse);
            return new StreamingResolution("text", new StringReader(errorResponse));
        }
        log.debug("Transform: [{}]", response);
        return new StreamingResolution("text", new StringReader(response));
    }

}
