package com.smilecoms.scp.action;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.AVP;
import com.smilecoms.commons.sca.AVPListAccessor;
import com.smilecoms.commons.sca.AuthenticationQuery;
import com.smilecoms.commons.sca.AuthenticationResult;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.CustomerQuery;
import com.smilecoms.commons.sca.Event;
import com.smilecoms.commons.sca.IMSNestedIdentityAssociation;
import com.smilecoms.commons.sca.IMSPrivateIdentity;
import com.smilecoms.commons.sca.IMSPublicIdentity;
import com.smilecoms.commons.sca.IMSSubscriptionQuery;
import com.smilecoms.commons.sca.ProductInstance;
import com.smilecoms.commons.sca.ProductOrder;
import com.smilecoms.commons.sca.ProductServiceInstanceMapping;
import com.smilecoms.commons.sca.SCABusinessError;
import com.smilecoms.commons.sca.SCAContext;
import com.smilecoms.commons.sca.SCASystemError;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.SSOPasswordResetData;
import com.smilecoms.commons.sca.Sale;
import com.smilecoms.commons.sca.ServiceActivationData;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.ServiceInstanceOrder;
import com.smilecoms.commons.sca.ServiceInstanceQuery;
import com.smilecoms.commons.sca.StAction;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.StDone;
import com.smilecoms.commons.sca.StSaleLookupVerbosity;
import com.smilecoms.commons.sca.StServiceInstanceLookupVerbosity;
import com.smilecoms.commons.stripes.SmileActionBean;
import com.smilecoms.commons.base.cache.CacheHelper;
import com.smilecoms.commons.sca.ServiceInstanceList;
import com.smilecoms.commons.util.Codec;
import com.smilecoms.commons.util.Utils;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.DontValidate;
import net.sourceforge.stripes.action.ErrorResolution;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import org.apache.commons.lang.StringEscapeUtils;

/**
 *
 * @author lesiba
 */
public class LoginActionBean extends SmileActionBean {

    private List<List> permissions;

    @Override
    public List getPermissions() {
        return permissions;
    }

    private String username;
    private String password;

    public void setPassword(java.lang.String password) {
        this.password = password;
    }

    public void setUsername(java.lang.String username) {
        this.username = username;
    }

    public void setPermissions(List permissions) {
        this.permissions = permissions;
    }

    private String nextAction;

    public String getNextAction() {
        return nextAction;
    }

    public Resolution showRecharge() {
        nextAction = "recharge";
        return login();
    }

    public void setNextAction(String nextAction) {
        this.nextAction = nextAction;
    }

    public Resolution logOut() {
        // Invalidate session
        invalidateSession();
        disableTracking();
        // Send to login
        return new RedirectResolution("/login.jsp?noauto=true");
    }

    @DefaultHandler
    @DontValidate
    public Resolution login() {

        if (!BaseUtils.getProperty("env.portal.autologin.imei.regex").equalsIgnoreCase("disabled")
                && username == null && password == null && getParameter("noauto") == null) {
            try {
                log.debug("Attempting auto login for IP address [{}]", getOriginatingIP());
                ServiceInstanceQuery siq = new ServiceInstanceQuery();
                siq.setVerbosity(StServiceInstanceLookupVerbosity.MAIN_SVCAVP);
                siq.setIPAddress(getOriginatingIP());
                ServiceInstance si = SCAWrapper.getAdminInstance().getServiceInstance(siq);
                AVPListAccessor avps = new AVPListAccessor(si.getAVPs());
                String imei = avps.getValueAsString("IMEISV");
                if (Utils.matches(imei, BaseUtils.getProperty("env.portal.autologin.imei.regex"))) {
                    CustomerQuery cq = new CustomerQuery();
                    cq.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
                    cq.setCustomerId(si.getCustomerId());
                    cq.setResultLimit(1);
                    Customer cust = SCAWrapper.getAdminInstance().getCustomer(cq);
                    username = cust.getSSOIdentity();
                    password = cust.getSSODigest();
                    log.debug("Doing auto login for user [{}]", username);
                } else {
                    log.debug("IMEISV [{}] does not match auto login pattern", imei);
                    return getDDForwardResolution("/login.jsp");
                }
            } catch (Exception e) {
                log.debug("Error trying to log in with callers IP", e);
                return getDDForwardResolution("/login.jsp");
            }
        }

        if (username != null && username.isEmpty()) {
            localiseErrorAndAddToGlobalErrors("invalid.login.credentials");
            return getDDForwardResolution("/error_pages/login_error.jsp");
        }

        if (username == null || password == null) {
            invalidateSession();
            return getDDForwardResolution("/login.jsp");
        }
        checkCSRF();
        try {
            //String srcIP = Utils.getRemoteIPAddress(getRequest());
            CustomerQuery customerQuery = new CustomerQuery();
            customerQuery.setResultLimit(1);
            customerQuery.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);

            switch (getAuthType(username)) {
                case "EMAIL":
                    customerQuery.setEmailAddress(username);
                    try {
                        username = SCAWrapper.getAdminInstance().getCustomer(customerQuery).getSSOIdentity();
                    } catch (Exception ex) {
                        log.debug("Error getting user by email address for login: ", ex);
                    }
                    break;
                case "PHONE":
                    try {
                        //Fail if not SmileVoice Number
                        String publicIdentity = Utils.getCleanDestination(username);

                        ServiceInstanceQuery siq = new ServiceInstanceQuery();
                        siq.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
                        siq.setIdentifierType("END_USER_SIP_URI");
                        siq.setIdentifier(Utils.getPublicIdentityForPhoneNumber(publicIdentity));
                        ServiceInstanceList sil = SCAWrapper.getAdminInstance().getServiceInstances(siq);

                        if (sil != null && sil.getNumberOfServiceInstances() > 0) {
                            customerQuery.setCustomerId(sil.getServiceInstances().get(0).getCustomerId());
                            username = SCAWrapper.getAdminInstance().getCustomer(customerQuery).getSSOIdentity();
                        }

                    } catch (Exception ex) {
                        log.debug("Error getting user by alternative contact for login: ", ex);
                    }
                    break;
                case "IDENTITY_NUMBER":
                    customerQuery.setIdentityNumber(username);
                    try {
                        username = SCAWrapper.getAdminInstance().getCustomer(customerQuery).getSSOIdentity();
                    } catch (Exception ex) {
                        log.debug("Error getting user by national identity: ", ex);
                    }
                    break;
                default:
                    log.debug("Going to use default username for authentication");
            }

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
            return getDDForwardResolution("/error_pages/login_error.jsp");
        }
        try {
            String track = (String) CacheHelper.getFromRemoteCache("RequestTrack_" + getContext().getRequest().getRemoteUser().toLowerCase());
            if (track != null) {
                CacheHelper.removeFromRemoteCache("RequestTrack_" + getContext().getRequest().getRemoteUser().toLowerCase());
                setTrackerUserName(track);
                return getDDForwardResolution("/request_tracking.jsp");
            }
        } catch (Exception e) {
            log.warn("Error checking if tracking is requested:[{}]", e.toString());
        }
        log.debug("We have a next action of [{}]", getNextAction());
        if (getNextAction() != null && !getNextAction().isEmpty()) {
            try {
                String[] config = BaseUtils.getSubProperty("env.scp.nextaction.config", getNextAction()).split("\\|");
                String clazz = config[0];
                String action = config[1];
                return getDDForwardResolution((Class<? extends ActionBean>) this.getClass().getClassLoader().loadClass(clazz), action);
            } catch (Exception e) {
                log.warn("Error in config for env.scp.nextaction.config and next action [{}]: [{}]", getNextAction(), e.toString());
            }
        }
        log.debug("Sending to retrieveAllUserServicesAccounts on AccountActionBean");
        return getDDForwardResolution(AccountActionBean.class, "retrieveAllUserServicesAccounts");
    }
    private String trackerUserName;

    public java.lang.String getTrackerUserName() {
        return trackerUserName;
    }

    public void setTrackerUserName(java.lang.String trackerUserName) {
        this.trackerUserName = trackerUserName;
    }

    public Resolution disallowTracking() {
        disableTracking();
        return getDDForwardResolution(AccountActionBean.class, "retrieveAllUserServicesAccounts");
    }

    public Resolution isup() {
        invalidateSession();
        return new StreamingResolution("text", new StringReader("Yes SCP is UP"));
    }

    public Resolution allowTracking() {
        enableTracking();
        return new RedirectResolution("/index.jsp");
    }

    //fogetten password methods
    public Resolution showSendResetLink() {
        return getDDForwardResolution("/request_reset.jsp");
    }

    public Resolution sendResetLink() {
        try {
            log.debug("Sending reset link for [{}]", getSSOPasswordResetLinkData().getIdentifier());

            CustomerQuery customerQuery = new CustomerQuery();
            customerQuery.setResultLimit(1);
            customerQuery.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);

            switch (getAuthType(getSSOPasswordResetLinkData().getIdentifier())) {
                case "EMAIL":
                    customerQuery.setEmailAddress(getSSOPasswordResetLinkData().getIdentifier());
                    try {
                        getSSOPasswordResetLinkData().setIdentifier(SCAWrapper.getAdminInstance().getCustomer(customerQuery).getSSOIdentity());
                    } catch (Exception ex) {
                        log.debug("Error getting user by email address for reset link: ", ex);
                    }
                    break;
                case "PHONE":
                    try {
                        //Fail if not SmileVoice Number
                        String publicIdentity = Utils.getCleanDestination(getSSOPasswordResetLinkData().getIdentifier());
                        if (publicIdentity == null || publicIdentity.isEmpty()) {
                            localiseErrorAndAddToGlobalErrors("user.not.found.for.password.reset");
                            return showSendResetLink();
                        }
                        ServiceInstanceQuery siq = new ServiceInstanceQuery();
                        siq.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
                        siq.setIdentifierType("END_USER_SIP_URI");
                        siq.setIdentifier(Utils.getPublicIdentityForPhoneNumber(publicIdentity));
                        ServiceInstanceList sil = SCAWrapper.getAdminInstance().getServiceInstances(siq);

                        if (sil != null && sil.getNumberOfServiceInstances() > 0) {
                            customerQuery.setCustomerId(sil.getServiceInstances().get(0).getCustomerId());
                            getSSOPasswordResetLinkData().setIdentifier(SCAWrapper.getAdminInstance().getCustomer(customerQuery).getSSOIdentity());
                        }

                    } catch (Exception ex) {
                        log.warn("Error getting user by alternative contact for reset link: ", ex);
                    }
                    break;
                case "IDENTITY_NUMBER":
                    customerQuery.setIdentityNumber(getSSOPasswordResetLinkData().getIdentifier());
                    try {
                        getSSOPasswordResetLinkData().setIdentifier(SCAWrapper.getAdminInstance().getCustomer(customerQuery).getSSOIdentity());
                    } catch (Exception ex) {
                        log.debug("Error getting user by national identity: ", ex);
                    }
                    break;
                default:
                    log.debug("Going to use default username for reset link");
            }

            SCAWrapper.getAdminInstance().sendSSOPasswordResetLink(getSSOPasswordResetLinkData());
            setPageMessage("reset.link.sent");
            return getDDForwardResolution("/login.jsp");
        } catch (Exception e) {
            localiseErrorAndAddToGlobalErrors("user.not.found.for.password.reset");
            return showSendResetLink();
        }
    }
    // showresetPassword. Made "rp" to make the link sent to users shorter

    public Resolution showSetNewPassword() {
        return getDDForwardResolution("create_new_password.jsp");
    }

    public Resolution rp() {

        String guid = getParameter("guid");
        setSSOPasswordResetData(new SSOPasswordResetData());
        getSSOPasswordResetData().setGUID(guid);
        setCustomer(new Customer());
        getCustomer().setCustomerId(0);
        return getDDForwardResolution("/reset_password.jsp");
    }

    public Resolution resetPassword() {
        checkCSRF();
        if (getParameter("password1").isEmpty() || getParameter("password2").isEmpty()) {
            localiseErrorAndAddToGlobalErrors("passwords.reset.short");
            return getDDForwardResolution("/reset_password.jsp");
        }

        if (!getParameter("password1").equals(getParameter("password2"))) {
            localiseErrorAndAddToGlobalErrors("passwords.dont.match");
            return getDDForwardResolution("/reset_password.jsp");
        }
        String passwd = getParameter("password1");
        if (getSSOPasswordResetData().getGUID().isEmpty() && getCustomer() != null && getCustomer().getCustomerId() > 0) {
            // A reset without a valid guid can only happen if the customers password in the backend is blank
            CustomerQuery customerQuery = new CustomerQuery();
            customerQuery.setCustomerId(getCustomer().getCustomerId());
            customerQuery.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
            Customer cust = SCAWrapper.getAdminInstance().getCustomer(customerQuery);//for login puposes only
            if (cust.getSSODigest().isEmpty()) {
                log.debug("Allowing password reset without a GUID as the customer has a blank password in the backend");
                try {
                    cust.setSSODigest(Utils.hashPasswordWithComplexityCheck(passwd));
                } catch (Exception ex) {
                    localiseErrorAndAddToGlobalErrors("password.too.simple");
                    return getDDForwardResolution("/reset_password.jsp");
                }
                SCAWrapper.getAdminInstance().modifyCustomer(cust);
                setPageMessage("password.reset");
            } else {
                localiseErrorAndAddToGlobalErrors("invalid.password.reset.data");
            }

        } else {
            try {
                getSSOPasswordResetData().setNewSSODigest(Utils.hashPasswordWithComplexityCheck(passwd));
            } catch (Exception ex) {
                localiseErrorAndAddToGlobalErrors("password.too.simple");
                return getDDForwardResolution("/reset_password.jsp");
            }
            SCAWrapper.getAdminInstance().resetSSOPassword(getSSOPasswordResetData());
            setPageMessage("password.reset");
        }
        return getDDForwardResolution("/login.jsp");
    }
    public String reason;

    public java.lang.String getReason() {
        return reason;
    }

    public void setReason(java.lang.String reason) {
        this.reason = reason;
    }

    public Resolution showNoCreditPage() {
        return getDDForwardResolution("/no_credit.jsp");
    }

    public Resolution showGetActivationCode() {
        return getDDForwardResolution("/voice/request_activation_code.jsp");
    }

    public Resolution rateCall() {
        String id = this.getParameter("i");
        String rating = this.getParameter("r");
        if (id != null && rating != null) {
            try {
                Event event = new Event();
                event.setEventData(rating);
                event.setEventKey(id);
                event.setEventType("RATING");
                event.setEventSubType("CALL");
                event.setUniqueKey("CALL_RATE_" + id);
                SCAWrapper.getAdminInstance().createEvent(event);
            } catch (Exception e) {
                log.warn("Error rating call", e);
            }
        }
        return new StreamingResolution("text", new StringReader("<?xml version=\"1.0\" encoding=\"UTF-8\" ?><Rating>" + rating + "</Rating>"));
    }

    public Resolution sendActivationCode() {
        log.debug("In sendActivationCode");
        CustomerQuery q = new CustomerQuery();
        String identifier = getParameter("identifier");
        if (getCustomerQuery() != null) {
            q = getCustomerQuery();
            identifier = q.getEmailAddress();
        }

        q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES_SVCAVP);
        Customer cust;
        log.warn("Identifier is {}", identifier);
        try {
            if (!identifier.contains("@")) {
                try {
                    //Fail if not SmileVoice Number

                    String publicIdentity = Utils.getCleanDestination(identifier);
                    if (publicIdentity == null || publicIdentity.isEmpty()) {
                        return new net.sourceforge.stripes.action.ErrorResolution(404, "Number Not Found");
                    }
                    ServiceInstanceQuery siq = new ServiceInstanceQuery();
                    siq.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
                    siq.setIdentifierType("END_USER_SIP_URI");
                    siq.setIdentifier(Utils.getPublicIdentityForPhoneNumber(publicIdentity));
                    ServiceInstanceList sil = SCAWrapper.getAdminInstance().getServiceInstances(siq);

                    if (sil != null && sil.getNumberOfServiceInstances() > 0) {
                        q.setCustomerId(sil.getServiceInstances().get(0).getCustomerId());
                    }

                    Event event = new Event();
                    event.setEventType("CL_VOICE");
                    event.setEventSubType("PROD_ACT_CODE");
                    event.setEventKey(String.valueOf(sil.getServiceInstances().get(0).getProductInstanceId()));
                    event.setEventData("CustId=" + sil.getServiceInstances().get(0).getCustomerId() + "\r\nPIId=" + sil.getServiceInstances().get(0).getProductInstanceId());

                    SCAWrapper.getAdminInstance().createEvent(event);

                    event = new Event();
                    event.setEventType("CL_VOICE");
                    event.setEventSubType("ACT_CODE");
                    event.setEventKey(String.valueOf(sil.getServiceInstances().get(0).getProductInstanceId()));
                    event.setEventData("CustId=" + sil.getServiceInstances().get(0).getCustomerId() + "\r\nPIId=" + sil.getServiceInstances().get(0).getProductInstanceId());

                    SCAWrapper.getAdminInstance().createEvent(event);

                } catch (Exception ex) {
                    return new net.sourceforge.stripes.action.ErrorResolution(404, "Number Not Found");
                }

                return getDDForwardResolution("/voice/request_activation_code.jsp");
            }

            q.setEmailAddress(identifier);
            cust = SCAWrapper.getAdminInstance().getCustomer(q);

        } catch (SCABusinessError e) {
            return new net.sourceforge.stripes.action.ErrorResolution(404, "Email Not Found");
        }

        int firstProductInstanceId = 0;
        for (ProductInstance pi : cust.getProductInstances()) {
            for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
                ServiceInstance si = m.getServiceInstance();
                if (si.getCustomerId() == cust.getCustomerId() && (si.getServiceSpecificationId() == 100 || si.getServiceSpecificationId() == 110)) {

                    // For when we need to send email without silverpop
                    for (AVP avp : si.getAVPs()) {
                        log.debug("Looking at AVP [{}]", avp.getAttribute());
                        if (avp.getAttribute().equals("PublicIdentity")) {
                            log.debug("Found publicIdentity [{}]", avp.getValue());
                            ServiceActivationData sad = getServiceActivationData(avp.getValue());
                            IMSPrivateIdentity firstIMPI = sad.getIMSSubscription().getIMSPrivateIdentities().get(0);
                            IMSPublicIdentity sipUnbarredIMPU = getSIPUnbarredIMPU(firstIMPI);
                            String num = Utils.getFriendlyPhoneNumber(sipUnbarredIMPU.getIdentity());

                            if (BaseUtils.getBooleanProperty("env.voice.act.use.first.pi.as.default", true) && firstProductInstanceId == 0) {
                                firstProductInstanceId = pi.getProductInstanceId();
                            }

                            Event event = new Event();
                            event.setEventType("CL_VOICE");
                            event.setEventSubType("PROD_ACT_CODE");
                            event.setEventKey(String.valueOf(pi.getProductInstanceId()));
                            event.setEventData("CustId=" + cust.getCustomerId() + "\r\nPIId=" + pi.getProductInstanceId());
                            SCAWrapper.getAdminInstance().createEvent(event);
                            break;
                        }
                    }
                }
            }
        }

        Event event = new Event();
        event.setEventType("CL_VOICE");
        event.setEventSubType("ACT_CODE");
        event.setEventKey(String.valueOf(cust.getCustomerId()));
        if (firstProductInstanceId > 0) {
            event.setEventData("CustId=" + cust.getCustomerId() + "\r\nPIId=" + firstProductInstanceId);
        } else {
            event.setEventData("CustId=" + String.valueOf(cust.getCustomerId()));
        }

        SCAWrapper.getAdminInstance().createEvent(event);

        return getDDForwardResolution("/voice/request_activation_code.jsp");
    }

     boolean activationDataSent = false;

    public boolean isActivationDataSent() {
        return activationDataSent;
    }

    public void setActivationDataSent(boolean activationDataSent) {
        this.activationDataSent = activationDataSent;
    }

    private ServiceActivationData getServiceActivationData(String impu) {
        IMSSubscriptionQuery q = new IMSSubscriptionQuery();
        q.setIMSPublicIdentity(impu);
        ServiceActivationData sad = SCAWrapper.getAdminInstance().getServiceActivationData(q);
        return sad;
    }

    private IMSPublicIdentity getSIPUnbarredIMPU(IMSPrivateIdentity impi) {
        IMSPublicIdentity sipUnbarredIMPU = null;
        // Returned first unbarred sip impu
        for (IMSNestedIdentityAssociation assoc : impi.getImplicitIMSPublicIdentitySets().get(0).getAssociatedIMSPublicIdentities()) {
            sipUnbarredIMPU = assoc.getIMSPublicIdentity();
            if (sipUnbarredIMPU.getBarring() != 0) {
                continue;
            }
            if (sipUnbarredIMPU.getIdentity().startsWith("sip:")) {
                break;
            }
        }
        return sipUnbarredIMPU;
    }

    public Resolution getZoiperConfig() {

        String msisdn = getParameter("u");
        String passedActivationCode = getParameter("p");
        String passedAppKey = getParameter("c");

        if (passedAppKey == null || passedAppKey.isEmpty()) {
            log.debug("The passed in activation code will be stored so it can be used for future authentications");
            passedAppKey = passedActivationCode;
        }
        String impu = null;
        ServiceActivationData sad;

        try {

            impu = Utils.getPublicIdentityForPhoneNumber(msisdn);

            try {
                if (Utils.matchesWithPatternCache(impu, BaseUtils.getProperty("env.zoiper.no.activation.code.regex", "X"))) {
                    log.warn("IMPU [{}] is allowed to log in to Zoiper without a password", impu);
                    sad = getServiceActivationData(impu);
                    passedActivationCode = sad.getActivationCode();
                    passedAppKey = null;
                }
            } catch (java.util.regex.PatternSyntaxException ex) {
                log.warn("The pattern syntax is incorrect.... ignoring [{}]", BaseUtils.getProperty("env.zoiper.no.activation.code.regex", "X"));
            }

            AuthenticationQuery q = new AuthenticationQuery();
            q.setIMSPublicIdentity(impu);
            q.setActivationCode(passedActivationCode);
            q.setEncryptedAppKey(passedAppKey == null ? null : Utils.oneWayHashImproved(passedAppKey));
            AuthenticationResult res = SCAWrapper.getAdminInstance().authenticate(q);
            if (!res.getDone().equals(StDone.TRUE)) {
                throw new SCABusinessError();
            }

            sad = getServiceActivationData(impu);
            IMSPrivateIdentity firstIMPI = sad.getIMSSubscription().getIMSPrivateIdentities().get(0);
            IMSPublicIdentity sipUnbarredIMPU = getSIPUnbarredIMPU(firstIMPI);
            if (sipUnbarredIMPU == null) {
                throw new SCABusinessError();
            }
            String impuIdentity = sipUnbarredIMPU.getIdentity();
            String publicK = Codec.encryptedHexStringToDecryptedString(firstIMPI.getEncryptedPublicKey());
            String userName = "+" + Utils.getFriendlyPhoneNumberKeepingCountryCode(impuIdentity);
            String authUserName = impuIdentity.substring(4);
            String accountName = Utils.getFriendlyPhoneNumber(impuIdentity);
            String templateVersion = "1";

            try {
                for (String line : BaseUtils.getPropertyAsList("env.zoiper.xml.template.number.version.mappings")) {
                    String[] bits = line.split("=");
                    if (Utils.matchesWithPatternCache(msisdn, bits[0])) {
                        templateVersion = bits[1];
                        break;
                    }
                }
            } catch (Exception e) {
                log.warn("Error getting zoiper config version. Will default to 1: [{}]", e.toString());
            }

            if (templateVersion.equals(BaseUtils.getProperty("env.zoiper.catch.all.template.version", "1"))) {
                log.debug("The assigned template version is the default catch all version - so we check for a staff specific template version");
                int staffTemplateVersion = BaseUtils.getIntProperty("env.zoiper.staff.template.version", -1);
                if (staffTemplateVersion != -1) {
                    log.debug("There is a staff specific template version to use: [{}], so we check if this number is a staff number", staffTemplateVersion);
                    boolean isStaff = false;
                    String normalisedNumber = Utils.getNumberInInternationalE164Format(msisdn);
                    log.debug("Normalised number passed in: [{}]", normalisedNumber);
                    Set<String> staffNumbers = BaseUtils.getPropertyFromSQLAsSet("env.staff.numbers.only");
                    if (staffNumbers != null && staffNumbers.contains(normalisedNumber)) {
                        isStaff = true;
                    }

                    if (isStaff) {
                        if (log.isDebugEnabled()) {
                            log.debug("Passed MSISDN [{}] is a staff number so we use staff specific template [{}]", msisdn, staffTemplateVersion);
                        }
                        templateVersion = "" + staffTemplateVersion;
                    }

                }
            }

            String template = BaseUtils.getProperty("env.zoiper.xml.template." + templateVersion);
            template = template.replace("_ACCOUNT_NAME_", accountName);
            template = template.replace("_USERNAME_", userName);
            template = template.replace("_PASSWORD_", publicK);
            template = template.replace("_IMPU_", authUserName);
            return new StreamingResolution("text", new StringReader(template));

        } catch (SCASystemError sse) {
            log.warn("System error authenticating Zoiper. Make sure user is not forced to re register: [{}] - user [{}]", new Object[]{sse.toString(), msisdn});
            return new ErrorResolution(500, "Error fetching provisioning XML");

        } catch (SCABusinessError sbe) {
            log.debug("Business error authenticating Zoiper. User must re register: [{}] - user [{}]", new Object[]{sbe.toString(), msisdn});
            String errResponse = "Incorrect Password";
            try {
                if (impu != null && Utils.matchesWithPatternCache(impu, BaseUtils.getProperty("env.zoiper.show.expected.password.regex", "X^"))) {
                    sad = getServiceActivationData(impu);
                    errResponse = "Incorrect password. Expected " + sad.getActivationCode();
                }
            } catch (Exception e2) {
            }
            return new StreamingResolution("text", new StringReader("<?xml version=\"1.0\" encoding=\"UTF-8\" ?><error>" + StringEscapeUtils.escapeXml(errResponse) + "</error>"));
        }
    }

    public Resolution diamondBankRedirect() {

        String orderId = getParameter("OrderID");
        if (orderId == null || orderId.isEmpty()) {
            log.warn("Expected an order id but got none");
            return getDDForwardResolution("/login.jsp");
        }
        log.debug("Looking up diamond bank payment gateway transaction status with id [{}]", orderId);
        Sale data = SCAWrapper.getAdminInstance().getSale(Integer.parseInt(orderId), StSaleLookupVerbosity.SALE);
        if (data.getLandingURL() == null) {
            log.warn("Landing URL is null");
            return getDDForwardResolution("/login.jsp");
        }
        if (!data.getLandingURL().contains("?")) {
            data.setLandingURL(data.getLandingURL() + "?");
        } else {
            data.setLandingURL(data.getLandingURL() + "&");
        }
        for (Entry<String, String[]> param : getRequest().getParameterMap().entrySet()) {
            String paramName = param.getKey();
            if (paramName.equalsIgnoreCase("diamondBankRedirect")) {
                continue;//Skip to avoid webpage redirect loop e.g. [/PaymentGateway.action?processBankTransaction&OrderID=4847&diamondBankRedirect=&processBankTransaction=&TransactionReference=2015072808432275T]
            }
            String[] valArray = param.getValue();
            String val;
            if (valArray != null && valArray.length > 0) {
                val = valArray[0];
                data.setLandingURL(data.getLandingURL() + paramName + "=" + val + "&");
            }
        }
        // Remove trailing & or ?
        data.setLandingURL(data.getLandingURL().substring(0, data.getLandingURL().length() - 1));
        log.debug("Landing URL to redirect to is: [{}]", data.getLandingURL());

        return new RedirectResolution(data.getLandingURL(), !data.getLandingURL().startsWith("http"));

    }

    public Resolution postPaymentRedirect() {

        String orderId = "";

        for (Enumeration en = getRequest().getParameterNames(); en.hasMoreElements();) {

            String name = (String) en.nextElement();

            if (name.equalsIgnoreCase("saleId")) {
                String value = getRequest().getParameter(name);
                orderId = value;
                break;
            }
            if (name.equalsIgnoreCase("OrderID")) {
                String value = getRequest().getParameter(name);
                orderId = value;
            }
            if (name.equalsIgnoreCase("pesapal_merchant_reference")) {
                String value = getRequest().getParameter(name);
                orderId = value;
            }
            if (name.equalsIgnoreCase("TransactionID")) {
                String value = getRequest().getParameter(name);
                orderId = value;
            }
        }

        if (orderId == null || orderId.isEmpty()) {
            log.debug("Parameters to do a proper search are not included or empty, not going to process this transaction any further.");
            return getDDForwardResolution("/login.jsp");
        }

        log.debug("Looking up payment gateway transaction status with id [{}]", orderId);
        Sale data = SCAWrapper.getAdminInstance().getSale(Integer.parseInt(orderId), StSaleLookupVerbosity.SALE);
        if (data.getLandingURL() == null) {
            log.warn("Landing URL is null");
            return getDDForwardResolution("/login.jsp");
        }
        if (!data.getLandingURL().contains("?")) {
            data.setLandingURL(data.getLandingURL() + "?");
        } else {
            data.setLandingURL(data.getLandingURL() + "&");
        }
        for (Entry<String, String[]> param : getRequest().getParameterMap().entrySet()) {
            String paramName = param.getKey();
            log.debug("Processing gateway parameter: {}", paramName); //To remove warning, for testing purposes ONLY
            if (paramName.equalsIgnoreCase("postPaymentRedirect") || paramName.equalsIgnoreCase("payRef")) {
                //Skip to avoid webpage redirect loop e.g. [/PaymentGateway.action?processBankTransaction&OrderID=4847&diamondBankRedirect=&processBankTransaction=&TransactionReference=2015072808432275T]
                //Skip payRef, it contains URL unwise/unsafe ( RFC-1738 (URLs) and RFC-2396 (URIs)) characters: 
                continue;
            }
            String[] valArray = param.getValue();
            String val;
            if (valArray != null && valArray.length > 0) {
                val = valArray[0];
                data.setLandingURL(data.getLandingURL() + paramName + "=" + val + "&");
            }
        }
        // Remove trailing & or ?
        data.setLandingURL(data.getLandingURL().substring(0, data.getLandingURL().length() - 1));
        log.debug("Landing URL to redirect to is: [{}]", data.getLandingURL());

        return new RedirectResolution(data.getLandingURL(), !data.getLandingURL().startsWith("http"));

    }

//    @DontValidate
//    public Resolution wifi() {
//        return getDDForwardResolution("/login_wifi.jsp");
//    }
//
//    @DontValidate
//    public Resolution wifiLogin() {
//        if (username != null && username.isEmpty()) {
//            localiseErrorAndAddToGlobalErrors("invalid.login.credentials");
//            return getDDForwardResolution("/login_wifi.jsp");
//        }
//
//        if (username == null || password == null) {
//            invalidateSession();
//            return getDDForwardResolution("/login_wifi.jsp");
//        }
//        try {
//            getRequest().login(username, password);
//        } catch (Exception e) {
//            if (password.isEmpty()) {
//                // Check if this is a first time login
//                log.debug("Checking if [{}] has an empty password", username);
//                CustomerQuery customerQuery = new CustomerQuery();
//                customerQuery.setSSOIdentity(username);
//                customerQuery.setResultLimit(1);
//                customerQuery.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
//                try {
//                    setCustomer(SCAWrapper.getAdminInstance().getCustomer(customerQuery));
//                    if (getCustomer().getSSODigest().isEmpty()) {
//                        log.debug("Customer tried to log in with a blank password, and the customers password in the backend is blank");
//                        return getDDForwardResolution("/reset_password.jsp");
//                    }
//                } catch (Exception ex) {
//                    log.debug("Error logging in user: ", ex);
//                    e = ex;
//                }
//
//            }
//            log.debug("Error logging in user: ", e);
//            localiseErrorAndAddToGlobalErrors("invalid.login.credentials");
//            return getDDForwardResolution("/error_pages/login_error.jsp");
//        }
//
//        // Link IP address to service instance
//        String ip = Utils.getRemoteIPAddress(getRequest());
//        log.debug("IP is [{}]", ip);
//
//        clearWiFiIP(ip);
//
//        CustomerQuery cq = new CustomerQuery();
//        cq.setSSOIdentity(username);
//        cq.setResultLimit(1);
//        cq.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES);
//        Customer c = SCAWrapper.getAdminInstance().getCustomer(cq);
//        boolean found = false;
//        for (ProductInstance pi : c.getProductInstances()) {
//
//            for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
//                ServiceInstance si = m.getServiceInstance();
//                if (si.getServiceSpecificationId() == 38) {
//                    log.debug("We have found si [{}]", si.getServiceInstanceId());
//
//                    ProductOrder po = new ProductOrder();
//                    // Call as admin so that this is permitted
//                    po.setSCAContext(new SCAContext());
//                    po.setAction(StAction.NONE);
//                    po.setProductInstanceId(si.getProductInstanceId());
//                    ServiceInstanceOrder sio = new ServiceInstanceOrder();
//                    sio.setAction(StAction.UPDATE);
//                    sio.setServiceInstance(si);
//                    AVP ipAVP = new AVP();
//                    ipAVP.setAttribute("WiFiIP");
//                    ipAVP.setValue(ip);
//                    sio.getServiceInstance().getAVPs().add(ipAVP);
//                    po.getServiceInstanceOrders().add(sio);
//                    SCAWrapper.getAdminInstance().processOrder(po);
//
//                    found = true;
//                    break;
//                }
//            }
//            if (found) {
//                break;
//            }
//        }
//
//        return getDDForwardResolution("/loggedin_wifi.jsp");
//
//    }
    private void clearWiFiIP(String ip) {
        try {
            ServiceInstanceQuery siq = new ServiceInstanceQuery();
            siq.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
            siq.setIdentifierType("WIFI_IP");
            siq.setIdentifier(ip);
            ServiceInstance si = SCAWrapper.getAdminInstance().getServiceInstance(siq);
            ProductOrder po = new ProductOrder();
            // Call as admin so that this is permitted
            po.setSCAContext(new SCAContext());
            po.setAction(StAction.NONE);
            po.setProductInstanceId(si.getProductInstanceId());
            ServiceInstanceOrder sio = new ServiceInstanceOrder();
            sio.setAction(StAction.UPDATE);
            sio.setServiceInstance(si);
            AVP ipAVP = new AVP();
            ipAVP.setAttribute("WiFiIP");
            ipAVP.setValue("");
            sio.getServiceInstance().getAVPs().add(ipAVP);
            po.getServiceInstanceOrders().add(sio);
            SCAWrapper.getAdminInstance().processOrder(po);
        } catch (SCABusinessError sbe) {
            log.debug("No SI found");
        } catch (Exception e) {
            log.warn("Error clearing IP ", e);
        }
    }

    private String getAuthType(String string) {
        if (isEmail(string)) {
            return "EMAIL";
        }
        try {
            string = Utils.getCleanDestination(string);
        } catch (Exception ex) {
        }

        if (isNumeric(string)) {
            return "PHONE";
        }
        return "";
    }

    private Pattern emailPattern = Pattern.compile(BaseUtils.getProperty("env.scp.email.validation.regex",
            "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\"
            + "x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")"
            + "@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}"
            + "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])"));
    private static final Pattern phonePattern = Pattern.compile("([0-9]*)");

    private boolean isEmail(final String string) {
        Matcher m = emailPattern.matcher(string);
        return m.matches();
    }

    private boolean isNumeric(final String string) {
        Matcher m = phonePattern.matcher(string);
        return m.matches();
    }

}
