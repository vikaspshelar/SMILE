/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.resource;

import com.currencyfair.onesignal.OneSignal;
import com.currencyfair.onesignal.model.notification.Button;
import com.currencyfair.onesignal.model.notification.CreateNotificationResponse;
import com.currencyfair.onesignal.model.notification.NotificationRequest;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.localisation.LocalisationHelper;
import com.smilecoms.commons.sca.Done;
import com.smilecoms.commons.sca.StDone;
import com.smilecoms.selfcare.NotificationMessage;
import com.smilecoms.selfcare.SelfcareService;
import org.apache.commons.text.StringSubstitutor;

import com.smilecoms.commons.platform.PlatformEventManager;
import com.smilecoms.commons.util.Utils;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author rajeshkumar
 */
@Path("test")
public class TestsResource {

    private static final Logger log = LoggerFactory.getLogger(TestsResource.class);

    private static final String TESTER_HEADER = "247bb307-8684-4bd7-8d61-54f3f68f8e46";

    @Path("/pushnotfcation/{customerId}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Done testSendPushNotification(@PathParam("customerId") int customerId,
            @QueryParam("type") String type, @QueryParam("subtype") String subtype) throws Exception {
        log.debug("called testSendPushNotification");
        Done done = new Done();
        done.setDone(StDone.FALSE);
        if (type == null || type.trim().isEmpty()) {
            log.info("type parameter is null or empty ");
            return done;
        }
        /*Map<String, String> headers = Collections.list(request.getHeaderNames()).stream().collect(Collectors.toMap(h -> h, request::getHeader));
        String testerToken = headers.get("x-tester-tolen");
        
        if(testerToken == null || !testerToken.equals(TESTER_HEADER)){
            log.info("tester token is null or empty");
            return done;
        }*/
        sendAppNotification(customerId, type, subtype, LocalisationHelper.getLocaleForLanguage("en"));
        done.setDone(StDone.TRUE);
        return done;
    }

    @Path("/addevent")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Done testAddEvent(@HeaderParam("x-tester-token") String testerToken,
            @QueryParam("accId") String accId, @QueryParam("piid") String piid, @QueryParam("uciid") String uciid,
            @QueryParam("subType") String subType) throws Exception {
        log.debug("called testAddEvent");
        Done done = new Done();
        done.setDone(StDone.FALSE);

        if (testerToken == null || !testerToken.equals(TESTER_HEADER)) {
            log.info("tester token is null or empty");
            return done;
        }
        String data = "AccId=" + accId + "\r\nPIId=" + piid + "\r\nUCId=" + uciid;
        boolean isEventCreated = PlatformEventManager.createEvent(
                "CL_UC",
                subType,
                accId,
                data,
                "CL_UC_" + subType + "_" + uciid);
        log.debug("event created successfully");
        if (isEventCreated) {
            done.setDone(StDone.TRUE);
        }
        return done;
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Done getMessage(@HeaderParam("x-tester-token") String testerToken) throws Exception {
        log.debug("called testAddEvent");
        Done done = new Done();
        done.setDone(StDone.FALSE);

        if (testerToken == null || !testerToken.equals(TESTER_HEADER)) {
            log.info("tester token is null or empty");
            return done;
        }
        done.setDone(StDone.TRUE);
        log.debug("event created successfully");
        return done;
    }
    
    @Path("/smileproperty")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String,String> getProperty(@QueryParam("property") String property, @QueryParam("subproperty") String subproperty) throws Exception {
        log.debug("called getProperty");
        Map<String, String> map = new HashMap<>();
        
        if(property == null || property.isEmpty()){
            return map;
        }

        if(subproperty == null || subproperty.isEmpty()){
            map.put(property, BaseUtils.getProperty(property));
        } else {
            map.put(subproperty, BaseUtils.getSubProperty(property, subproperty));
        }
        return map;
    }

    private String sendAppNotification(int customerId, String type, String subType, Locale locale) {
        log.debug("called sendAppNotification");

        String notificationId = null;
        log.debug("notification service is on");
        String messageKey = "customer.lifecycle." + type.toLowerCase() + "." + subType.toLowerCase() + ".notification";
        String messageResource = LocalisationHelper.getLocalisedString(locale, messageKey);
        log.debug("messageResource :" + messageResource);
        if (messageResource == null) {
            log.error("App notification resource is not defined for the key [{}]", messageKey);
            return null;
        }
        Map<String, String> notificationData = getNotificationData();
        String[] msgResorces = messageResource.split("[\r\n]+");
        Map<String, String> msgResourceMap = new HashMap<>();
        for (String msgPart : msgResorces) {
            String[] msgPartSplit = msgPart.split("=");
            if (msgPartSplit.length != 2) {
                continue;
            }
            msgResourceMap.put(msgPartSplit[0], msgPartSplit[1]);
        }

        log.debug("msgResourceMap is [{}]", msgResourceMap);
        StringSubstitutor sub = new StringSubstitutor(notificationData);
        String messageBody = sub.replace(msgResourceMap.get("body"));
        String messageTitle = msgResourceMap.get("title");
        String image = msgResourceMap.get("image");
        String buttons = msgResourceMap.get("buttons");
        log.debug("messageBody [{}] and  messageTitle [{}] and image [{}] and buttons [{}]", messageBody, messageTitle, image, buttons);
        try {
            if (messageBody.isEmpty()) {
                log.debug("App Notification body is empty so not sending");
                return null;
            }
            SelfcareService service = new SelfcareService();
            //boolean isPush = Boolean.valueOf(msgResourceMap.get("push"));
            //boolean isInApp = Boolean.valueOf(msgResourceMap.get("inApp"));

            //log.debug("isPush = [{}] and isInApp [{}]",isPush, isInApp);
            Map<String, String> dataMap = new HashMap<>();
            dataMap.put("type", msgResourceMap.get("type"));
            dataMap.put("info", msgResourceMap.get("info"));
            notificationId = service.sendPushNotification(customerId, messageBody, messageTitle, image, buttons, dataMap);

            log.debug("notificationId = [{}]", notificationId);
        } catch (Exception ex) {
            log.error("error in sendAppNotification :", ex);
        }

        return notificationId;
    }

    private Map<String, String> getNotificationData() {
        Map<String, String> dataMap = new HashMap<>();

        dataMap.put("customer.firstname", "Rajesh");
        dataMap.put("customer.middlename", "Kumar");
        dataMap.put("customer.lastname", "Singh");

        dataMap.put("organisation.name", "Smile communications");

        dataMap.put("account.id", "10034004356");
        dataMap.put("account.balance", "1500.00");

        dataMap.put("product.phonenumber", "07795329934");

        dataMap.put("uc.name", "500MB data plan");

        return dataMap;
    }
    
    

}
