/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.et;

import com.currencyfair.onesignal.OneSignal;
import com.currencyfair.onesignal.model.notification.Button;
import com.currencyfair.onesignal.model.notification.CreateNotificationResponse;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.localisation.LocalisationHelper;
import com.smilecoms.commons.sca.AccountHistoryQuery;
import com.smilecoms.commons.sca.IMSSubscriptionQuery;
import com.smilecoms.commons.sca.SCABusinessError;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceActivationData;
import com.smilecoms.commons.sca.ShortMessage;
import com.smilecoms.commons.sca.StAccountHistoryVerbosity;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.TransactionRecord;
import com.smilecoms.commons.sca.beans.AccountBean;
import com.smilecoms.commons.sca.beans.CustomerBean;
import com.smilecoms.commons.sca.beans.OrganisationBean;
import com.smilecoms.commons.sca.beans.ProductBean;
import com.smilecoms.commons.sca.beans.UnitCreditBean;
import com.smilecoms.commons.util.IMAPUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.et.db.model.EventData;

import com.currencyfair.onesignal.model.notification.NotificationRequest;
import com.smilecoms.selfcare.NotificationMessage;
import com.smilecoms.selfcare.SelfcareService;
import org.apache.commons.text.StringSubstitutor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class EventHelper {

    private static final Logger log = LoggerFactory.getLogger(EventHelper.class);

    public void sendCustomerNotification(EventData event) {

        try {
            log.debug("In sendCustomerNotification for event data id [{}]", event.getEventDataId());

            if (event.getType().equals("CL_TEST") && event.getSubType().equals("TEST")) {

                String contextXML = BaseUtils.getProperty("env.customerlifecycle.test.xml", "");
                testAllEvents(Utils.getValueFromCRDelimitedAVPString(event.getData(), "CustomerID"),
                        Utils.getValueFromCRDelimitedAVPString(event.getData(), "EMail"),
                        Utils.getValueFromCRDelimitedAVPString(event.getData(), "Phone"),
                        Utils.getValueFromCRDelimitedAVPString(event.getData(), "Language"),
                        contextXML);

            } else {

                int custId = getEventCustomer(event);
                long accNumber = getEventAccount(event);
                int piId = getEventProductInstance(event);
                long ahId = getEventAccountHistory(event);
                CustomerBean cust = null;
                AccountBean acc = null;
                ProductBean pi = null;
                OrganisationBean org = null;
                TransactionRecord tr = null;
                String activationCode = "";

                if (custId > 0) {
                    log.debug("We have a customer id. Getting customer bean");
                    cust = CustomerBean.getCustomerById(custId, StCustomerLookupVerbosity.CUSTOMER);
                    if ((cust.getOptInLevel() & 1) == 0) {
                        log.debug("Customers opt in  level is [{}] so not sending lifecycle messages", cust.getOptInLevel());
                        return;
                    }
                }
                if (accNumber > 0) {
                    log.debug("We have an account number. Getting account bean");
                    acc = AccountBean.getAccountById(accNumber);
                }
                if (piId > 0) {
                    log.debug("We have a product instance. Getting product bean");
                    try {
                        pi = ProductBean.getProductInstanceById(piId);
                    } catch (SCABusinessError e) {
                        log.debug("Could not find PI - this is not necessarily an issue as the PI could have been validly removed - will no longer process this event");
                        return;
                    }
                }
                if (ahId > 0) {
                    log.debug("We have an account history id. Getting account history");
                    AccountHistoryQuery ahQuery = new AccountHistoryQuery();
                    ahQuery.setTransactionRecordId(ahId);
                    ahQuery.setResultLimit(1);
                    ahQuery.setVerbosity(StAccountHistoryVerbosity.RECORDS);
                    tr = SCAWrapper.getAdminInstance().getAccountHistory(ahQuery).getTransactionRecords().get(0);
                    log.debug("Got transaction history record with id [{}] and extxid [{}]", tr.getTransactionRecordId(), tr.getExtTxId());
                }

                log.debug("Getting unit credit bean");
                UnitCreditBean uc = getEventUnitCreditInstance(event);

                if (cust == null && pi != null) {
                    log.debug("No customer yet but we have a product instance so getting the products owner");
                    try {
                        pi = ProductBean.getProductInstanceById(piId);
                    } catch (SCABusinessError e) {
                        log.debug("Could not find PI - this is not necessarily an issue as the PI could have been validly removed - will no longer process this event");
                        return;
                    }
                    cust = CustomerBean.getCustomerById(pi.getCustomerId(), StCustomerLookupVerbosity.CUSTOMER);
                    if ((cust.getOptInLevel() & 1) == 0) {
                        log.debug("Customers opt in  level is [{}] so not sending lifecycle messages", cust.getOptInLevel());
                        return;
                    }
                }

                if (cust == null && acc != null) {
                    log.debug("We have an account number but not a customer id. Going to get customer from account");
                    try {
                        cust = CustomerBean.getCustomerByAccountNumber(acc.getAccountId(), StCustomerLookupVerbosity.CUSTOMER);
                    } catch (SCABusinessError e) {
                        log.debug("Could not find customer by account id  - this is not necessarily an issue as the services on the account could have been validly removed - will no longer process this event");
                        return;
                    }
                    if ((cust.getOptInLevel() & 1) == 0) {
                        log.debug("Customers opt in  level is [{}] so not sending lifecycle messages", cust.getOptInLevel());
                        return;
                    }
                }

                if (pi != null && pi.getOrganisationId() > 0) {
                    org = OrganisationBean.getOrganisationById(pi.getOrganisationId());
                }

                if (pi != null && acc == null && !pi.getServices().isEmpty()) {
                    log.debug("We have a product instance but not account");
                    long accId = pi.getServices().get(0).getAccountId();
                    acc = AccountBean.getAccountById(accId);
                }

                if (cust == null) {
                    throw new Exception("Cannot determine the customer from the event data");
                }

                //We only add the activation code if the subType is PROD_ACT_CODE as it is only needed for that subType
                if (event.getSubType().equals("PROD_ACT_CODE") && pi != null && pi.getProductPhoneNumber() != null && !pi.getProductPhoneNumber().isEmpty()) {
                    IMSSubscriptionQuery q = new IMSSubscriptionQuery();
                    String publicIdentity = Utils.getPublicIdentityForPhoneNumber(pi.getProductPhoneNumber());
                    log.debug("Getting activation code for IMPU [{}]", publicIdentity);
                    q.setIMSPublicIdentity(publicIdentity);
                    ServiceActivationData sad = SCAWrapper.getAdminInstance().getServiceActivationData(q);
                    activationCode = sad.getActivationCode();
                    log.debug("Activation code is [{}]", activationCode);
                }

                String contextXML = getContextXML(cust, org, acc, pi, uc, tr, activationCode);
                Map<String, String> notificationData = getNotificationData(cust, org, acc, pi, uc, tr);
                String cc = null;
                Set<String> skipCCOrgs = null;
                try {
                    skipCCOrgs = BaseUtils.getPropertyAsSet("env.et.dont.cc.org.ids");
                } catch (Exception e) {
                    log.debug("No property env.et.dont.cc.org.ids");
                }
                if (org != null && !event.getSubType().equals("PROD_ACT_CODE") && org.getOrganisationId() != 1 && (skipCCOrgs == null || !BaseUtils.getPropertyAsSet("env.et.dont.cc.org.ids").contains(String.valueOf(org.getOrganisationId())))) {
                    cc = org.getEmailAddress();
                }

                log.debug("Context XML is [{}]", contextXML);
                Locale locale = LocalisationHelper.getLocaleForLanguage(cust.getLanguage());
                log.debug("This customer has locale [{}]", locale.toString());

                Set<String> testEventCustomerIds = BaseUtils.getPropertyAsSet("env.et.test.customerids.for.lifecycle.messages");
                if (testEventCustomerIds != null && !testEventCustomerIds.isEmpty() && !testEventCustomerIds.contains(Integer.toString(cust.getCustomerId()))) {
                    log.debug("There is a valid list of test customer Ids for lifecycle messages and this customer id [{}] is not in it - so we don't send messages", cust.getCustomerId());
                    return;
                }

                Set<String> testEventOrgIds = BaseUtils.getPropertyAsSet("env.et.test.orgids.for.lifecycle.messages");
                if (testEventOrgIds != null && !testEventOrgIds.isEmpty() && org != null && !testEventOrgIds.contains(Integer.toString(org.getOrganisationId()))) {
                    log.debug("There is a valid list of test organisation Ids for lifecycle messages and this organisation id [{}] is not in it - so we don't send messages", org.getOrganisationId());
                    return;
                }

                sendMessages(event.getType(), event.getSubType(), contextXML, cust.getCustomerId(), cust.getEmailAddress(), cc, cust.getAlternativeContact1(), locale);
                // Send selfcare app notification to the customer
                sendAppNotification(cust.getCustomerId(), event.getType(), event.getSubType(), notificationData, locale);
                
            }

            log.debug("Finished sendCustomerNotification for event data id [{}][{}]", event.getEventDataId(), Thread.currentThread().getName());

        } catch (SCABusinessError be) {
            log.debug("SCABusinessError in sendCustomerNotification", be);
        } catch (Exception e) {
            log.warn("Error in sendCustomerNotification", e);
            e.printStackTrace();
            log.warn("Data causing error: Id [{}] Type [{}] SubType [{}] Data [{}] Thread [{}]", new Object[]{event.getEventDataId(), event.getType(), event.getSubType(), event.getData(), Thread.currentThread().getName()});
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "ET", "Error processing customer notification for event data id " + event.getEventDataId() + " Type " + event.getType() + " SubType " + event.getSubType() + " : " + Utils.getDeepestCause(e).getMessage());
        }

    }

    private int getEventCustomer(EventData event) {
        log.debug("Getting customer for event");
        String custId = Utils.getValueFromCRDelimitedAVPString(event.getData(), "CustId");
        log.debug("Customer id is [{}]", custId);
        return custId == null ? 0 : Integer.parseInt(custId);
    }

    private long getEventAccount(EventData event) {
        log.debug("Getting account for event");
        String accountId = Utils.getValueFromCRDelimitedAVPString(event.getData(), "AccId");
        log.debug("Account number is [{}]", accountId);
        return accountId == null ? 0 : Long.parseLong(accountId);
    }

    private int getEventProductInstance(EventData event) {
        log.debug("Getting product instance for event");
        String productInstanceId = Utils.getValueFromCRDelimitedAVPString(event.getData(), "PIId");
        log.debug("Product instance id is [{}]", productInstanceId);
        return productInstanceId == null ? 0 : Integer.parseInt(productInstanceId);
    }

    private long getEventAccountHistory(EventData event) {
        log.debug("Getting account history record for event");
        String ahId = Utils.getValueFromCRDelimitedAVPString(event.getData(), "AHId");
        log.debug("Account history id is [{}]", ahId);
        return ahId == null ? 0 : Long.parseLong(ahId);
    }

    private UnitCreditBean getEventUnitCreditInstance(EventData event) {
        String ucid = Utils.getValueFromCRDelimitedAVPString(event.getData(), "UCId");
        log.debug("Getting event unit credit [{}]", ucid);
        if (ucid == null) {
            return null;
        }
        int ucidInt = Integer.valueOf(ucid);
        return UnitCreditBean.getUnitCreditById(ucidInt);

    }

    private String getContextXML(CustomerBean cust, OrganisationBean org, AccountBean acc, ProductBean pi, UnitCreditBean uc, TransactionRecord tr, String activationCode) throws Exception {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" standalone=\"yes\"?>\n");
        xml.append("<EventContext>");

        if (cust != null) {
            String custXML = marshallBeanToXML(cust, "Customer").replace("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>", "");
            log.debug("Customer xml [{}]", custXML);
            xml.append(custXML);
        } else {
            log.debug("Customer is null so wont be included in XML");
        }
        if (org != null) {
            String orgXML = marshallBeanToXML(org, "Organisation").replace("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>", "");
            log.debug("Organisation xml [{}]", orgXML);
            xml.append(orgXML);
        } else {
            log.debug("Organisation is null so wont be included in XML");
        }
        if (acc != null) {
            String accXML = marshallBeanToXML(acc, "Account").replace("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>", "");
            log.debug("Account xml [{}]", accXML);
            xml.append(accXML);
        } else {
            log.debug("Account is null so wont be included in XML");
        }
        if (pi != null) {
            log.debug("Product customerid [{}]", pi.getCustomerId());
            String piXML = marshallBeanToXML(pi, "ProductInstance").replace("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>", "");
            log.debug("Product xml [{}]", piXML);
            xml.append(piXML);
        } else {
            log.debug("Product Instance is null so wont be included in XML");
        }
        if (uc != null) {
            log.debug("UnitCredit accountid [{}]", uc.getAccountId());
            String ucXML = marshallBeanToXML(uc, "UnitCredit").replace("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>", "");
            log.debug("UnitCredit xml [{}]", ucXML);
            xml.append(ucXML);
        } else {
            log.debug("Unit Credit is null so wont be included in XML");
        }
        if (tr != null) {
            String trXML = Utils.marshallSoapObjectToString(tr).replace("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?>", "");
            log.debug("Transaction record xml [{}]", trXML);
            xml.append(trXML);
        } else {
            log.debug("Transaction record is null so wont be included in XML");
        }

        if (!activationCode.isEmpty()) {
            String xmlActCode = "<ActivationCode>" + activationCode + "</ActivationCode>";
            xml.append(xmlActCode);
        }

        xml.append("</EventContext>");
        String ret = xml.toString()
                .replace("xmlns:ns2=\"http://xml.smilecoms.com/schema/SCA\"", "")
                .replace("xmlns=\"http://xml.smilecoms.com/schema/SCA\"", "")
                .replace("<ns2:", "<")
                .replace("</ns2:", "</");
        return ret;
    }

    private void sendEmail(String from, String to, String cc, String contextXML, String xsltEmailSubject, String xsltEmailBody) throws Exception {
        String subject = transform(contextXML, xsltEmailSubject);
        if (subject.contains("DO_NOT_SEND")) {
            log.debug("Result should not be sent");
            return;
        }
        String body = transform(contextXML, xsltEmailBody);
        if (body.contains("DO_NOT_SEND")) {
            log.debug("Result should not be sent");
            return;
        }
        log.debug("Sending event email from [{}] to [{}] subject [{}] body [{}]", new Object[]{from, to, subject, body});
        String bcc = null;
        try {
            bcc = BaseUtils.getProperty("env.et.emails.bcc");
        } catch (Exception e) {
        }
        String overriddenTo = BaseUtils.getProperty("env.et.emails.always.to", "");

        if (!overriddenTo.isEmpty()) {
            cc = null;
        }

        IMAPUtils.sendEmail(from, overriddenTo.isEmpty() ? to : overriddenTo, cc, bcc, subject, body);
    }

    private void sendSMS(String from, String to, String contextXML, String xsltSMS, String campaignID) throws Exception {
        String body = transform(contextXML, xsltSMS);
        if (body.contains("DO_NOT_SEND")) {
            log.debug("Result should not be sent");
            return;
        }
        body = body.trim();
        String andTo = null;
        if (body.startsWith("[TO:")) {
            to = Utils.getFirstNumericPartOfStringAsString(body);
            log.debug("To has been overridden to [{}]", to);
            body = body.replaceFirst("\\[TO:.*\\]", "");
            log.debug("Body is now [{}]", body);
        } else if (body.startsWith("[AND:")) {
            andTo = Utils.getFirstNumericPartOfStringAsString(body);
            log.debug("And to is [{}]", andTo);
            body = body.replaceFirst("\\[AND:.*\\]", "");
            log.debug("Body is now [{}]", body);
        }
        if (body.isEmpty()) {
            log.debug("SMS body is empty so not sending");
            return;
        }
        String alwaysTo = BaseUtils.getProperty("env.et.sms.always.to", "");
        ShortMessage sm = new ShortMessage();
        sm.setBody(body);
        sm.setFrom(from);
        sm.setTo(alwaysTo.isEmpty() ? to : alwaysTo);
        if (campaignID != null) {
            sm.setCampaignId(campaignID);
        }

        if (!sm.getTo().isEmpty()) {
            log.debug("Sending event sms from [{}] to [{}] text [{}]", new Object[]{from, to, body});
            SCAWrapper.getAdminInstance().sendShortMessage(sm);
        } else {
            log.debug("To is empty. Not sending");
        }
        if (andTo != null && !andTo.isEmpty() && alwaysTo.isEmpty()) {
            sm = new ShortMessage();
            sm.setBody(body);
            sm.setFrom(from);
            sm.setTo(andTo);
            log.debug("Sending event sms from [{}] to [{}] text [{}]", new Object[]{from, andTo, body});
            SCAWrapper.getAdminInstance().sendShortMessage(sm);
        }
    }
    
    private void sendAppNotification(int customerId, String type, String subType, Map<String, String> notificationData, Locale locale) {
        log.debug("called sendAppNotification");
        
        if (BaseUtils.getBooleanProperty("env.selfcare.send.notifications", false)) {
            log.debug("notification service is on");
            String messageKey = "customer.lifecycle." + type.toLowerCase() + "." + subType.toLowerCase() + ".notification";
            String messageResource = LocalisationHelper.getLocalisedString(locale, messageKey);
            log.debug("messageResource :"+messageResource);
            if (messageResource == null) {
                log.error("App notification resource is not defined for the key [{}]", messageKey);
                return;
            }

            String[] msgResorces = messageResource.split("[\r\n]+");
            Map<String, String> msgResourceMap = new HashMap<>();
            for (String msgPart : msgResorces) {
                String[] msgPartSplit = msgPart.split("=");
                if (msgPartSplit.length != 2) {
                    continue;
                }
                msgResourceMap.put(msgPartSplit[0].trim(), msgPartSplit[1].trim());
            }

            if(msgResourceMap.isEmpty()){
                log.debug("message body resource is missing");
                return;
            }
            StringSubstitutor sub = new StringSubstitutor(notificationData);
            String messageBody = sub.replace(msgResourceMap.get("body"));
            String messageTitle = msgResourceMap.get("title");
            String image = msgResourceMap.get("image");
            String buttons = msgResourceMap.get("buttons");
            log.debug("messageBody [{}] and  messageTitle [{}] and image [{}] and buttons [{}]", messageBody, messageTitle, image, buttons);
            try {
                if (messageBody == null || messageBody.isEmpty()) {
                    log.debug("App Notification body is empty so not sending");
                    return;
                }
                SelfcareService service = new SelfcareService();
                String customerAppId = service.getAppId(customerId);
                if (customerAppId == null || customerAppId.isEmpty()) {
                    log.info("selfcare application is not available for customer [{}]", customerId);
                    return;
                }
                
             
            //boolean isPush = Boolean.valueOf(msgResourceMap.get("push"));
            //boolean isInApp = Boolean.valueOf(msgResourceMap.get("inApp"));

            //log.debug("isPush = [{}] and isInApp [{}]",isPush, isInApp);
            Map<String, String> dataMap = new HashMap<>();
            dataMap.put("type", msgResourceMap.get("type"));
            dataMap.put("info", msgResourceMap.get("info"));
            String notificationId = service.sendPushNotification(customerId, messageBody, messageTitle, image, buttons, dataMap);
            log.debug("notification sent with id [{}]",notificationId);
            } catch (Exception ex) {
                log.error("error in sendAppNotification :", ex);
            }
        }
    }

    private String transform(String xml, String xslt) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("Transform XML: [{}]", xml);
            log.debug("Transform XSLT: [{}]", xslt);
        }
        return Utils.doXSLTransform(xml, xslt, getClass().getClassLoader());
    }

    private String marshallBeanToXML(Object ob, String type) throws Exception {
        return Utils.marshallBeanToXML(ob, "http://xml.smilecoms.com/schema/SCA", type);
    }

    private void testAllEvents(String customerId, String email, String phone, String language, String contextXML) throws Exception {

        for (String[] row : BaseUtils.getPropertyFromSQL("global.customerlifecycle.tests")) {
            String type = row[0];
            String subType = row[1];
            log.debug("Running test for Type[{}] and subType[{}]", type, subType);
            try {
                int cid = -1;
                if(customerId != null && !customerId.isEmpty()){
                    cid = Integer.valueOf(customerId);
                }
                sendMessages(type, subType, contextXML, cid, email, null, phone, LocalisationHelper.getLocaleForLanguage(language));
            } catch (Exception e) {
                log.warn("Error in testAllEvents sendMessages", e);
                log.warn("Data causing error: Type [{}] SubType [{}] XML [{}] Thread [{}]", new Object[]{type, subType, contextXML, Thread.currentThread().getName()});
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "ET", "Error processing test customer notification  Type " + type + " SubType " + subType + " : " + Utils.getDeepestCause(e).getMessage());
            }
        }

    }

    private void sendMessages(String type, String subType, String contextXML,int cuatomerId, String email, String cc, String phoneNumber, Locale locale) throws Exception {
        String prefix = "customer.lifecycle." + type.toLowerCase() + "." + subType.toLowerCase();
        log.debug("Getting resources for prefix [{}]", prefix);
        String xsltEmailSubject = LocalisationHelper.getLocalisedString(locale, prefix + ".email.subject");
        String xsltEmailBody = LocalisationHelper.getLocalisedString(locale, prefix + ".email.body");

        if (subType.contains("ACT_CODE")) {
            if (!xsltEmailSubject.startsWith("?") && !xsltEmailBody.startsWith("?")) {
                sendEmail(BaseUtils.getProperty("env.smtp.customercomms.from.for.act.code"), email, cc, contextXML, xsltEmailSubject, xsltEmailBody);
            } else {
                log.debug("This event has no associated email for Activation code");
            }
        } else {
            if (!xsltEmailSubject.startsWith("?") && !xsltEmailBody.startsWith("?")) {
                sendEmail(BaseUtils.getProperty("env.smtp.customercomms.from"), email, cc, contextXML, xsltEmailSubject, xsltEmailBody);
            } else {
                log.debug("This event has no associated email");
            }
        }

        String xsltSMS = LocalisationHelper.getLocalisedString(locale, prefix + ".sms");
        if (!xsltSMS.startsWith("?")) {

            if (BaseUtils.getBooleanProperty("env.development.mode", false)) {
                String body = transform(contextXML, xsltSMS);
                if (!body.contains("DO_NOT_SEND")) {
                    if (subType.contains("ACT_CODE")) {
                        IMAPUtils.sendEmail(BaseUtils.getProperty("env.smtp.customercomms.from.for.act.code"), email, null, null, "SMS - " + type + " " + subType,
                                "Would have sent SMS from " + BaseUtils.getProperty("env.smtp.customercomms.from.for.act.code") + " to " + phoneNumber + " and text [" + body + "] of length " + body.length() + " characters");
                    } else {
                        IMAPUtils.sendEmail(BaseUtils.getProperty("env.smtp.customercomms.from"), email, null, null, "SMS - " + type + " " + subType,
                                "Would have sent SMS from " + BaseUtils.getProperty("env.sms.customercomms.from") + " to " + phoneNumber + " and text [" + body + "] of length " + body.length() + " characters");
                    }
                }
            } else {
                String campaignId = null;
                if (type.toLowerCase().startsWith("cl_")) {
                    log.debug("Setting sms as campaign for event type [{}]", type);
                    campaignId = type;
                }
                sendSMS(BaseUtils.getProperty("env.sms.customercomms.from"), phoneNumber, contextXML, xsltSMS, campaignId);
            }   
        } else {
            log.debug("This event has no associated SMS");
        }
        
        
    }
    
    public void go(com.smilecoms.et.db.model.EventData event, com.smilecoms.et.EventHelper helper, Object[] na) {
        helper.sendCustomerNotification(event);
        if (event.getType().equals("CL_PRODUCT") && event.getSubType().equalsIgnoreCase("new")) {
            event.setSubType("1hour_daily_free_youtube");
            helper.sendCustomerNotification(event);
        }
    }

    private Map<String, String> getNotificationData(CustomerBean cust, OrganisationBean org, AccountBean acc, ProductBean pi, UnitCreditBean uc, TransactionRecord tr) {
        Map<String, String> dataMap = new HashMap<>();
        if(cust != null){
            dataMap.put("customer.firstname",cust.getFirstName());
            dataMap.put("customer.middlename",cust.getMiddleName());
            dataMap.put("customer.lastname",cust.getLastName());
        }
        
        if(org != null){
            dataMap.put("organisation.name", org.getOrganisationName());
        }
        
        if(acc != null){
            dataMap.put("account.id", String.valueOf(acc.getAccountId()));
            dataMap.put("account.balance", String.valueOf(acc.getAvailableBalanceInCents()));
        }
        
        if(pi != null) {
            dataMap.put("product.phonenumber", pi.getProductPhoneNumber());
        }
        if(uc != null){
            dataMap.put("uc.name", uc.getName());
        }
        return dataMap;
    }


}
