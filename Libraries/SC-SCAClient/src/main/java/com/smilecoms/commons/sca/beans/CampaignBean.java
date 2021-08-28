/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.sca.beans;

import com.currencyfair.onesignal.OneSignal;
import com.currencyfair.onesignal.model.notification.CreateNotificationResponse;
import com.currencyfair.onesignal.model.notification.NotificationRequest;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.AVP;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.Done;
import com.smilecoms.commons.sca.Event;
import com.smilecoms.commons.sca.ProductInstance;
import com.smilecoms.commons.sca.ProductServiceInstanceMapping;
import com.smilecoms.commons.sca.PurchaseUnitCreditRequest;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.ShortMessage;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.StProductInstanceLookupVerbosity;
import com.smilecoms.commons.util.IMAPUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.selfcare.NotificationMessage;
import com.smilecoms.selfcare.SelfcareService;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class CampaignBean {

    private final String campaignName;
    private final String uniqueIdentifer;
    private final String info;
    private int productInstanceId;
    private static final Logger log = LoggerFactory.getLogger(CampaignBean.class);

    public static List<CampaignBean> getCustomersCampaigns(String phoneNumber) {
        // PCB - Not implemented yet
        CustomerBean cust = CustomerBean.getCustomerByPhoneNumber(phoneNumber, StCustomerLookupVerbosity.CUSTOMER);
        return null;
    }

    public CampaignBean(String campaignName, int productInstanceId, String uniqueIdentifer) {
        this(campaignName, productInstanceId, uniqueIdentifer, "null");
    }

    public CampaignBean(String campaignName, int productInstanceId, String uniqueIdentifer, String info) {
        log.debug("CampaignBean constructor Name [{}] PI [{}] Unique Id [{}] Info [{}]", new Object[]{campaignName, productInstanceId, uniqueIdentifer, info});
        if (campaignName == null || campaignName.isEmpty()) {
            throw new IllegalArgumentException("Invalid campaign name");
        }
        if (productInstanceId <= 0) {
            throw new IllegalArgumentException("Invalid product instance id");
        }
        if (uniqueIdentifer == null) {
            throw new IllegalArgumentException("Invalid uniqueIdentifer");
        }
        this.campaignName = campaignName;
        this.productInstanceId = productInstanceId;
        this.uniqueIdentifer = uniqueIdentifer;
        this.info = (info == null ? "null" : info);
    }

    public boolean provisionUnitCredit(Integer piId, long AccountId, int unitCreditSpecificationId, int numberToProvision, String uniqueId) {

        PurchaseUnitCreditRequest pucr = new PurchaseUnitCreditRequest();
        pucr.setProductInstanceId(piId);
        pucr.setAccountId(AccountId);
        pucr.setUnitCreditSpecificationId(unitCreditSpecificationId);
        pucr.setNumberToPurchase(numberToProvision);
        pucr.setUniqueId(uniqueId);
        pucr.setInfo(info);
        try {
            com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().purchaseUnitCredit(pucr);
        } catch (java.lang.Exception e) {
            log.warn("Error trying to provision bundle, most likely a duplicate");
            return false;
        }
        return true;

    }

    public boolean provisionUnitCredit(Integer piId, long AccountId, int unitCreditSpecificationId, int numberToProvision, String uniqueId, String info) {

        PurchaseUnitCreditRequest pucr = new PurchaseUnitCreditRequest();
        pucr.setProductInstanceId(piId);
        pucr.setAccountId(AccountId);
        pucr.setUnitCreditSpecificationId(unitCreditSpecificationId);
        pucr.setNumberToPurchase(numberToProvision);
        pucr.setUniqueId(uniqueId);
        pucr.setInfo(info);
        try {
            com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().purchaseUnitCredit(pucr);
        } catch (java.lang.Exception e) {
            log.warn("Error trying to provision bundle, most likely a duplicate");
            return false;
        }
        return true;

    }

    public boolean sendEmail(String from, String to, String subject, String text) {
        try {
            String alwaysFrom = BaseUtils.getProperty("env.cm.email.always.from", "");
            if (from == null || from.isEmpty()) {
                from = "noreply@smilecoms.com";
            }

            from = alwaysFrom.isEmpty() ? from : alwaysFrom;

            IMAPUtils.sendEmail(from, to, subject, text);
            return true;
        } catch (Exception e) {
            log.warn("Error sending Email during campaign", e);
            return false;
        }
    }
    
    public boolean sendAppNotification(int customerId, String messageBody, String messageTitle, 
             String image, Map<String, String> notificationData){
        log.debug("called sendAppNotification");
        
        if (BaseUtils.getBooleanProperty("env.selfcare.send.notifications", false)) {
            log.debug("notification service is on");

            log.debug("messageBody [{}] and  messageTitle [{}] and image [{}] and buttons [{}]", messageBody, messageTitle, image);
            try {
                if (messageBody.isEmpty()) {
                    log.debug("App Notification body is empty so not sending");
                    return false;
                }
                SelfcareService service = new SelfcareService();
                String customerAppId = service.getAppId(customerId);
                if (customerAppId == null || customerAppId.isEmpty()) {
                    log.info("selfcare application is not available for customer [{}]", customerId);
                    return false;
                }

                String notificationId = service.sendPushNotification(customerId, messageBody, messageTitle, image, null, notificationData);
                log.debug("notification sent with id [{}]",notificationId);
            } catch (Exception ex) {
                log.error("error in sendAppNotification :", ex);
            }
        }
        return true;
    }

    public boolean sendSMS(String from, String to, String text, String campaignId) {

        log.debug("Doing sendSMS from [{}], to [{}], text [{}], campaignId [{}]", new Object[]{from, to, text, campaignId});
        if (to == null || to.isEmpty()) {
            log.warn("To field is empty or null");
            return false;
        }
        if (from == null || from.isEmpty()) {
            log.warn("From field is empty or null");
            return false;
        }
        if (text == null || text.isEmpty()) {
            log.warn("Text field is empty or null");
            return false;
        }

        if (to.contains("@")) {
            try {
                IMAPUtils.sendEmail("noreply@smilecoms.com", to, "Campaign SMS", "Would have sent SMS with Campaign Id " + campaignId + " from " + from + " with text: " + text);
                return true;
            } catch (Exception e) {
                log.warn("Error sending SMS by Email during campaign", e);
                return false;
            }
        }

        boolean isFromNumber = true;
        try {
            java.lang.Long.parseLong(from);
        } catch (java.lang.NumberFormatException e) {
            isFromNumber = false;
        }
        if (!isFromNumber) {
            log.warn("From number is not valid");
            return false;
        }
        boolean isToNumber = true;
        try {
            java.lang.Long.parseLong(to);
        } catch (java.lang.NumberFormatException e) {
            isToNumber = false;
        }
        if (!isToNumber) {
            log.warn("To number is not valid");
            return false;
        }

        ShortMessage sm = new ShortMessage();
        sm.setFrom(from);
        sm.setTo(to);
        if (campaignId != null && !campaignId.isEmpty()) {
            log.debug("Campaign id is [{}]", campaignId);
            sm.setCampaignId(campaignId);
        }
        sm.setBody(text);
        sm.setDataCodingScheme((byte) 0x03);
        try {
            SCAWrapper.getAdminInstance().sendShortMessage(sm);
        } catch (java.lang.Exception e) {
            log.warn("Error sending SMS during campaign to [{}], most likely unable to route", to);
            return false;
        }
        return true;
    }

    public void inviteViaSMS(String linkPrefix, String linkSuffix, String from, String to) {
        log.debug("Doing inviteViaSMS linkPrefix [{}] linkSuffix [{}] from [{}] to [{}]", new Object[]{linkPrefix, linkSuffix, from, to});
        String url = "https://" + BaseUtils.getProperty("env.portal.url") + "/sra/campaigns/" + campaignName + "/optin?ui=" + uniqueIdentifer + "&piid=" + productInstanceId + "&info=" + info + "&hash=" + getHash();
        url = Utils.shortenURLWithSmile(url);
        ShortMessage sm = new ShortMessage();
        sm.setFrom(from);
        sm.setTo(to);
        sm.setBody(linkPrefix + url + linkSuffix);
        sm.setDataCodingScheme((byte) 0x03);
        SCAWrapper.getAdminInstance().sendShortMessage(sm);
        log.debug("Sent SMS [{}] from [{}] to [{}]", new Object[]{sm.getBody(), from, to});
    }

    public void inviteViaSMSWithRedirect(String linkPrefix, String linkSuffix, String from, String to, boolean mustSendEmail, String subject) {
        log.debug("Doing inviteViaSMS linkPrefix [{}] linkSuffix [{}] from [{}] to [{}]", new Object[]{linkPrefix, linkSuffix, from, to});

        Calendar calendar = Calendar.getInstance();

        int campaignExpiryPeriod = BaseUtils.getIntProperty("env.campaign.optinurl.expiry.period" + campaignName, 0);
        int expirySeconds = (3600 * 24 * 14);
        
        if (campaignExpiryPeriod > 0) {
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            calendar.add(Calendar.DATE, campaignExpiryPeriod);
            long diff = (calendar.getTime().getTime() - new Date().getTime());
            Long sec = TimeUnit.SECONDS.convert(diff, TimeUnit.MILLISECONDS);
            expirySeconds = sec.intValue();
        }

        String url = "https://" + BaseUtils.getProperty("env.portal.url") + "/sra/campaigns/" + campaignName + "/optin?ui=" + uniqueIdentifer + "&piid=" + productInstanceId + "&info=" + info + "&hash=" + getHash();
        url = Utils.shortenURLWithSmile(url, expirySeconds);
        
        ShortMessage sm = new ShortMessage();
        sm.setFrom(from);
        sm.setTo(to);
        String secondRedirect = BaseUtils.getProperty("env.campaign.redirect.url." + campaignName, "https://smilecoms.com?ok=true");
        String urlEncode = url;
        try {
            urlEncode = URLEncoder.encode(url, "UTF-8").replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
        } catch (UnsupportedEncodingException ex) {
            log.warn("Failed to URLEncode [{}], [{}]", url, ex);
        }
        
        secondRedirect = secondRedirect + "/?url=" + urlEncode;
        secondRedirect = Utils.shortenURLWithSmile(secondRedirect, expirySeconds);
        String body = linkPrefix + secondRedirect + linkSuffix;
        String andTo = null;
        
        if (body.startsWith("[AND:")) {
            andTo = getProductInstancePhoneNumber();
            log.debug("And to is [{}]", andTo);
            body = body.replaceFirst("\\[AND:.*\\]", "");
            log.debug("Body is now [{}]", body);
        }

        sm.setBody(body);
        sm.setDataCodingScheme((byte) 0x03);
        SCAWrapper.getAdminInstance().sendShortMessage(sm);
        log.debug("Sent SMS [{}] from [{}] to [{}]", new Object[]{sm.getBody(), from, to});

        if (andTo != null && !andTo.isEmpty() && !andTo.equals("NA")) {
            sm = new ShortMessage();
            sm.setBody(body);
            sm.setFrom(from);
            sm.setTo(andTo);
            sm.setDataCodingScheme((byte) 0x03);
            log.debug("Sending sms from [{}] to [{}] text [{}]", new Object[]{from, andTo, body});
            SCAWrapper.getAdminInstance().sendShortMessage(sm);
        }

        if (mustSendEmail) {
            sendEmail(null, getCustomer().getEmailAddress(), subject, linkPrefix + secondRedirect + linkSuffix);
        }
    }

    public void inviteViaSMSWithRedirect(String linkPrefix, String linkSuffix, String from, String to) {
        inviteViaSMSWithRedirect(linkPrefix, linkSuffix, from, to, false, null);
    }

//    public void inviteViaEmail(String subjectResource, String bodyResource, String from, String to, Object[] bodyParams) {
//        log.debug("Doing inviteViaEmail with body resource [{}] from [{}] to [{}]", new Object[]{subjectResource, from, to});
//        String url = "https://" + BaseUtils.getProperty("env.portal.url") + "/sra/campaigns/" + campaignName + "/optin?ui=" + uniqueIdentifer + "&piid=" + productInstanceId + "&info=" + info + "&hash=" + getHash();
//        url = Utils.shortenURLWithSmile(url);
//        String body = LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), bodyResource, bodyParams);
//        body = body.replace("_optinlink_", url);
//        String subject = LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), subjectResource);
//        try {
//            IMAPUtils.sendEmail(from, to, subject, body);
//        } catch (Exception e) {
//            log.warn("Error sending campaign email", e);
//        }
//
//    }
    public String processURLOptIn(String hash) throws Exception {
        try {
            log.debug("Doing processOptIn hash [{}]", hash);
            String expectedHash = getHash();
            if (!expectedHash.equals(hash)) {
                log.debug("Got [{}] Expected [{}]", hash, expectedHash);
                throw new Exception("Invalid hash");
            }
            Event event = new Event();
            event.setEventType("OPTIN");
            event.setEventSubType(campaignName);
            event.setEventKey(String.valueOf(productInstanceId));
            event.setEventData(uniqueIdentifer + "_" + productInstanceId + "_" + info);
            event.setUniqueKey(campaignName + "_" + event.getEventData());
            Done d = SCAWrapper.getAdminInstance().createEvent(event);
            if (d.getDone().equals(com.smilecoms.commons.sca.StDone.FALSE)) {
                throw new Exception("Event not created. Probably already opted in");
            }
            String redirectLink = BaseUtils.getProperty("env.campaign.redirect.ok." + campaignName, "https://smilecoms.com?ok=true");
            if (BaseUtils.getBooleanProperty("env.campaign.redirect.link.append.params." + campaignName, false)) {
                Customer cust = getCustomer();
                redirectLink = redirectLink + "/?status=successful&email=" + cust.getEmailAddress();
            }
            return redirectLink;
        } catch (Exception ex) {
            log.warn("Error processing campaign optin", ex);
            String redirectLink = BaseUtils.getProperty("env.campaign.redirect.err." + campaignName, "https://smilecoms.com?ok=false");
            if (BaseUtils.getBooleanProperty("env.campaign.redirect.link.append.params." + campaignName, false)) {
                Customer cust = getCustomer();
                redirectLink = redirectLink + "/?status=failed&email=" + cust.getEmailAddress();
            }
            return redirectLink;
        }
    }

    public void processSMSOptIn(String smsText) {
        Event event = new Event();
        event.setEventType("OPTIN");
        event.setEventSubType(campaignName);
        event.setEventKey(String.valueOf(productInstanceId));
        event.setEventData(uniqueIdentifer + "_" + productInstanceId + "_" + info + "_" + smsText);
        event.setUniqueKey(campaignName + "_" + uniqueIdentifer + "_" + productInstanceId);
        SCAWrapper.getAdminInstance().createEvent(event);
    }

    private String getHash() {
        String key = uniqueIdentifer + "_" + productInstanceId + "_" + info;
        String stringToHash = "e32H%98xnQs_98t" + campaignName + BaseUtils.getProperty("env.opco") + key + "NUwi^8()UhTFs";
        String res = Utils.oneWayHashImprovedHex(stringToHash).substring(2, 22);
        log.debug("Campaign [{}] Key [{}] Hash [{}]", new Object[]{campaignName, key, res});
        return res;
    }

    private Customer getCustomer() {
        ProductInstance pi = SCAWrapper.getAdminInstance().getProductInstance(productInstanceId, StProductInstanceLookupVerbosity.MAIN);
        Customer cust = SCAWrapper.getAdminInstance().getCustomer(pi.getCustomerId(), StCustomerLookupVerbosity.CUSTOMER);
        return cust;
    }

    private String getProductInstancePhoneNumber() {
        if (productInstanceId <= 0) {
            return "NA";
        }

        try {
            String phone = "NA";
            ServiceInstance si = getVoiceServiceInstanceForProductInstanceId();
            if (si == null) {
                return "NA";
            }
            for (AVP avp : si.getAVPs()) {
                if (avp.getAttribute().equalsIgnoreCase("PublicIdentity")) {
                    phone = Utils.getFriendlyPhoneNumber(avp.getValue());
                    break;
                }
            }
            return phone;
        } catch (Exception e) {
            log.warn("Error getting service instance Phone Number: ", e);
            return "NA";
        }
    }

    private ServiceInstance getVoiceServiceInstanceForProductInstanceId() {
        List<ServiceInstance> siList = getServiceInstanceListWithAVPsForProductInstanceId();
        for (ServiceInstance si : siList) {
            for (AVP avp : si.getAVPs()) {
                if (avp.getAttribute().equals("ProvisioningFlow") && avp.getValue().equals("voice")) {
                    return si;
                }
            }
        }
        return null;
    }

    private List<ServiceInstance> getServiceInstanceListWithAVPsForProductInstanceId() {
        List<ServiceInstance> siList = null;
        if (siList == null) {
            siList = new ArrayList();
            ProductInstance pi = SCAWrapper.getAdminInstance().getProductInstance(productInstanceId, StProductInstanceLookupVerbosity.MAIN_SVC_SVCAVP);
            for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
                siList.add(m.getServiceInstance());
            }
        }
        return siList;
    }

}
