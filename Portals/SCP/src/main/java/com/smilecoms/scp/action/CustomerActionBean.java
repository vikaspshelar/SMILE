package com.smilecoms.scp.action;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.localisation.LocalisationHelper;
import com.smilecoms.commons.sca.*;
import com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper;
import com.smilecoms.commons.stripes.SmileActionBean;
import com.smilecoms.commons.util.Utils;
import java.util.ArrayList;
import java.util.List;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;

/**
 *
 * @author lesiba
 */
public class CustomerActionBean extends SmileActionBean {

    boolean optInMarketing;
    boolean optInPostCall;

    public boolean getOptInMarketing() {
        return optInMarketing;
    }

    public void setOptInMarketing(boolean optInMarketing) {
        this.optInMarketing = optInMarketing;
    }

    public boolean getOptInPostCall() {
        return optInPostCall;
    }

    public void setOptInPostCall(boolean optInPostCall) {
        this.optInPostCall = optInPostCall;
    }

    @DefaultHandler
    public Resolution retrieveCustomerDefault() {

        setCustomer(UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER));
        getCustomer().setDateOfBirth(Utils.addSlashesToDate(getCustomer().getDateOfBirth()));
        return getDDForwardResolution("/index.jsp");
    }

    public List<String> getFaqList() {
        return faqList;
    }

    public void setFaqList(List<String> strings) {
        this.faqList = strings;
    }
    private List<String> faqList;

    public Resolution showFAQ() {

        try {
            String propertyCheck = BaseUtils.getProperty("env.portal.faq.url");
            return new RedirectResolution(propertyCheck, false);
        } catch (Exception e) {
            log.debug("[{}]", e.getLocalizedMessage());
            faqList = new ArrayList<>();
            int i = 1;

            while (!(LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), "scp.faq." + i).isEmpty() || LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), "scp.faq." + i).startsWith("?"))) {
                faqList.add(LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), "scp.faq." + i));
                i++;
            }

            return getDDForwardResolution("/help/FAQ.jsp");
        }
    }

    public Resolution retrieveCustomer() {

        setCustomerQuery(new CustomerQuery());
        if (getContext().getRequest().getRemoteUser() != null && getCustomerQuery().getSSOIdentity() == null) {
            getCustomerQuery().setSSOIdentity(getUser());
        }
        getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES);
        getCustomerQuery().setResultLimit(1);

        setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomerQuery()));
        getCustomer().setDateOfBirth(Utils.addSlashesToDate(getCustomer().getDateOfBirth()));
        if (!getCustomer().getPassportExpiryDate().equalsIgnoreCase("")) {
            getCustomer().setPassportExpiryDate(Utils.addSlashesToDate(getCustomer().getPassportExpiryDate()));
        }

        if (getCustomer().getAccountManagerCustomerProfileId() > 0) {
            //Requesting access to account manager of the customer logged in so they know who is managing their account
            Customer am = SCAWrapper.getAdminInstance().getCustomer(getCustomer().getAccountManagerCustomerProfileId(), StCustomerLookupVerbosity.CUSTOMER);
            accountManagerName = am.getFirstName() + " " + am.getLastName();
        } else {
            accountManagerName = "Not Assigned";
        }

        setOptInPostCall((getCustomer().getOptInLevel() & 2) == 2);
        setOptInMarketing((getCustomer().getOptInLevel() & 4) == 4);

        return getDDForwardResolution("/customer/view_customer.jsp");
    }

    public Resolution updateOptIn() {
        setCustomer(UserSpecificCachedDataHelper.getCustomer(getCustomer().getCustomerId(), StCustomerLookupVerbosity.CUSTOMER));
        int newOptInLevel = getCustomer().getOptInLevel();
        int newValue = getOptInMarketing() ? 1 : 0;
        int val = 4;
        if (newValue == 0) {
            val = ~val;
            newOptInLevel = val & newOptInLevel;
        } else {
            newOptInLevel = val | newOptInLevel;
        }
        
        newValue = getOptInPostCall() ? 1 : 0;
        val = 2;
        if (newValue == 0) {
            val = ~val;
            newOptInLevel = val & newOptInLevel;
        } else {
            newOptInLevel = val | newOptInLevel;
        }
        getCustomer().setOptInLevel(newOptInLevel);
        SCAWrapper.getUserSpecificInstance().modifyCustomer(getCustomer());
        return retrieveCustomer();
    }
    
    public Resolution updateOptInPostCall() {
        setCustomer(UserSpecificCachedDataHelper.getCustomer(getCustomer().getCustomerId(), StCustomerLookupVerbosity.CUSTOMER));
        int currentOptInLevel = getCustomer().getOptInLevel();
        log.debug("Current Opt in level is [{}]", currentOptInLevel);
        int newValue = getOptInPostCall() ? 1 : 0;
        int newOptInLevel;
        int val = 2;
        if (newValue == 0) {
            val = ~val;
            newOptInLevel = val & currentOptInLevel;
        } else {
            newOptInLevel = val | currentOptInLevel;
        }
        getCustomer().setOptInLevel(newOptInLevel);
        SCAWrapper.getUserSpecificInstance().modifyCustomer(getCustomer());
        return retrieveCustomer();
    }

    private String accountManagerName;

    public String getAccountManagerName() {
        return accountManagerName;
    }

    public Resolution retrieveCustomerForProductInstances() {
        //not used
        return getDDForwardResolution("/customer/customer_product_instances.jsp");
    }

    public Resolution retrieveCustomerForServiceInstaces() {
        //not used
        return getDDForwardResolution("/customer/customer_service_instances.jsp");
    }

    public Resolution updateCustomerPassword() {

        checkCSRF();
        if (!getParameter("newPassword").equals(getParameter("confirmPassword"))) {

            setCustomer(UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER));
            getCustomer().setDateOfBirth(Utils.addSlashesToDate(getCustomer().getDateOfBirth()));
            localiseErrorAndAddToGlobalErrors("passwords.dont.match");
            return getDDForwardResolution("/customer/edit_customer_password.jsp");
        }

        Customer tmpCustomer = UserSpecificCachedDataHelper.getCustomer(getCustomer().getCustomerId(), StCustomerLookupVerbosity.CUSTOMER);
        String newPass = getParameter("newPassword");
        try {
            tmpCustomer.setSSODigest(Utils.hashPasswordWithComplexityCheck(newPass));
        } catch (Exception ex) {
            localiseErrorAndAddToGlobalErrors("password.too.simple");
            return getDDForwardResolution("/customer/edit_customer_password.jsp");
        }
        tmpCustomer.setVersion(getCustomer().getVersion());

        SCAWrapper.getUserSpecificInstance().modifyCustomer(tmpCustomer);
        setPageMessage("password.reset.successful");
        return retrieveCustomer();
    }

    public Resolution showUpdateCustomerPassword() {

        setCustomer(UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER));
        return getDDForwardResolution("/customer/edit_customer_password.jsp");
    }

    private String contactSalesInfo;
    private String contactWalkInfo;
    private String contactCustCare;

    public String getContactSalesInfo() {
        return contactSalesInfo;
    }

    public String getContactCustCare() {
        return contactCustCare;
    }

    public String getContactWalkInfo() {
        return contactWalkInfo;
    }

    public Resolution showContactDetails() {

        //allows the ommission of missing details in the contact us page
        contactCustCare = LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), "scp.contactus.custcare");
        contactWalkInfo = LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), "scp.contactus.walkin");
        contactSalesInfo = LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), "scp.contactus.sales");
        return getDDForwardResolution("/help/contact_us.jsp");
    }

}
