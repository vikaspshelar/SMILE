/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos.paymentgateway;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.CustomerQuery;
import com.smilecoms.commons.sca.JiraField;
import com.smilecoms.commons.sca.NewTTIssue;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ShortMessage;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.TTIssue;
import com.smilecoms.commons.base.cache.CacheHelper;
import com.smilecoms.commons.sca.MindMapFields;
import com.smilecoms.commons.sca.UpdatePropertyRequest;
import com.smilecoms.commons.util.IMAPUtils;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sabza
 */
public abstract class AbstractPaymentGateway {

    protected Logger log = LoggerFactory.getLogger(this.getClass());
    protected static final Lock paymentGatewayLock = new java.util.concurrent.locks.ReentrantLock();

    protected void sendSMS(String to, String body) throws Exception {
        String alwaysTo = BaseUtils.getProperty("env.pos.paymentgateway.sms.notification.always.to", "");
        String from = BaseUtils.getProperty("env.pos.paymentgateway.sms.notification.from", "");
        ShortMessage sm = new ShortMessage();
        sm.setBody(body);
        sm.setFrom(from);
        sm.setTo(alwaysTo.isEmpty() ? to : alwaysTo);
        if (!sm.getTo().isEmpty()) {
            log.debug("Sending payment gateway error notification sms from [{}] to [{}] text [{}]", new Object[]{from, to, body});
            SCAWrapper.getAdminInstance().sendShortMessage(sm);
        } else {
            log.debug("To is empty. Not sending");
        }
    }

    protected String createTicket(NewTTIssue tt) {
        TTIssue issue = SCAWrapper.getAdminInstance().createTroubleTicketIssue(tt);
        return issue.getID();
    }

    protected void sendEmail(String from, String to, String subject, String body) {
        try {
            IMAPUtils.sendEmail(from, to, subject, body);
        } catch (Exception ex) {
            log.warn("Failed to send email to {}, reason: {}", to, ex.toString());
        }
    }

    protected boolean checkIfGatewayInAvailabilityAlreadyReported(String cacheKey) {
        boolean isReported = false;
        boolean reCreateTicket = false;
        long reportedTime = 0;
        int expireIn = 0;
        JsonParser jsonParser = new JsonParser();
        JsonObject issueDetailsAsJsonObject;

        String cachedMsg = (String) CacheHelper.getFromRemoteCache(cacheKey);
        if (cachedMsg == null) {
            return isReported;
        }
        issueDetailsAsJsonObject = jsonParser.parse(cachedMsg).getAsJsonObject();

        for (Map.Entry element : issueDetailsAsJsonObject.entrySet()) {
            if (element.getKey().equals("Ticket")) {
                JsonPrimitive asJsonPrimitive = issueDetailsAsJsonObject.getAsJsonPrimitive("Ticket");
                if (asJsonPrimitive.getAsString().isEmpty()) {
                    reCreateTicket = true;
                }
            }
            if (element.getKey().equals("StartDateTime")) {
                JsonPrimitive asJsonPrimitive = issueDetailsAsJsonObject.getAsJsonPrimitive("StartDateTime");
                reportedTime = asJsonPrimitive.getAsLong();
            }
            if (element.getKey().equals("ExpiresIn")) {
                JsonPrimitive asJsonPrimitive = issueDetailsAsJsonObject.getAsJsonPrimitive("ExpiresIn");
                expireIn = asJsonPrimitive.getAsInt();
            }
        }
        if ((System.currentTimeMillis() < (reportedTime + expireIn * 1000L)) || !reCreateTicket) {
            isReported = true;
            log.debug("Issue was last reported [{}] but still occuring. Not going to report it again for now", new Date(reportedTime));
        }
        log.debug("Has issue been reported to customer-care already: [{}]", isReported);
        return isReported;
    }

    protected final void createTicketForGatewayInavailability(String cacheKey, String errorMessage) {//Sub-classes MUST NOT change/override the algorithm

        try {

            NewTTIssue tt = new NewTTIssue();
            tt.setMindMapFields(new MindMapFields());

            addPaymentGatewaySpecificsOnTicket(tt, errorMessage);

            String watchers = "";
            Set<String> configWatchers = BaseUtils.getPropertyAsSet("env.pos.paymentgateway.isup.watchers");
            for (String watcher : configWatchers) {
                if (watchers.isEmpty()) {
                    watchers = watcher;
                    continue;
                }
                watchers += ":" + watcher;
            }
            if (!watchers.isEmpty()) {
                JiraField jiraF = new JiraField();
                jiraF.setFieldName("TT_FIXED_FIELD_Watchers");
                jiraF.setFieldType("TT_FIXED_FIELD");
                jiraF.setFieldValue(watchers);
                tt.getMindMapFields().getJiraField().add(jiraF);
            }

            tt.setCustomerId("0");
            String ticketID = createTicket(tt);
            log.debug("Ticket created succssefuly: {}, going store details on cache", ticketID);

            int expiresIn = BaseUtils.getIntProperty("env.pos.paymentgateway.notification.expiry.seconds", 4500);
            JsonObject tokenAsJsonObject;
            String msgInfo;
            tokenAsJsonObject = new JsonObject();
            tokenAsJsonObject.addProperty("ExpiresIn", expiresIn);
            tokenAsJsonObject.addProperty("Ticket", ticketID);
            tokenAsJsonObject.addProperty("StartDateTime", System.currentTimeMillis());
            tokenAsJsonObject.addProperty("ErrorMessage", errorMessage);
            msgInfo = tokenAsJsonObject.toString();
            CacheHelper.putInRemoteCache(cacheKey, msgInfo, expiresIn);
            log.debug("Stored in memached as: {}", msgInfo);
        } catch (Exception ex) {
            log.warn("Failed to report Gateway inavailability to customer care, reason: {}", ex.toString());
        }
    }

    protected final void publishGatewayInAvailabilityNotice(String gatewayName, boolean isup, String cacheKey, String errorMessage) {

        String propName = "env.scp." + gatewayName.toLowerCase() + ".partner.integration.config";
        
        updateGatewayStatus(propName, isup, gatewayName);

        if (isup) {
            return;
        }
        if (!BaseUtils.getBooleanProperty("env.pos.paymentgateway.publish.inavailability", true)) {
            return;
        }
        try {
            if (checkIfGatewayInAvailabilityAlreadyReported(cacheKey)) {
                return;
            }
            createTicketForGatewayInavailability(cacheKey, errorMessage);
            sendSMSForGatewayInavailability();
            sendEmailForGatewayInavailability(errorMessage);
        } catch (Exception ex) {
            log.warn("Failed to report [{}] inavailability to customer care, reason: {}", gatewayName, ex.toString());
        }
    }

    public abstract void addPaymentGatewaySpecificsOnTicket(NewTTIssue tt, String errorMessage);

    public void sendEmailForGatewayInavailability(String errorMessage) {
        //hook method
    }

    public void sendSMSForGatewayInavailability() {
        //hook method
    }

    protected Customer getCustomer(int custId) {
        CustomerQuery custQ = new CustomerQuery();
        custQ.setCustomerId(custId);
        custQ.setResultLimit(1);
        custQ.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        Customer cust = SCAWrapper.getAdminInstance().getCustomer(custQ);
        return cust;
    }

    protected String getObjectAsJsonString(Object obj) {
        Gson gson = new Gson();
        String jsonString = "";
        if (obj instanceof JsonElement) {
            JsonElement je = (JsonElement) obj;//maintains the original object state, prevents array object being encapsulated with new json object/element
            jsonString = gson.toJson(je);
        } else {
            jsonString = gson.toJson(obj);
        }
        log.debug("Payment gateway Json String is [{}]", jsonString);
        return jsonString;
    }

    protected JsonObject getJsonObjectFromJsonString(String obj) {
        JsonObject rqJson = new JsonParser().parse(obj).getAsJsonObject();
        log.debug("Successfully parsed json string to json object");
        return rqJson;
    }

    protected void updateGatewayStatus(String propName, boolean isup, String name) {
        try {

            if (propName == null || propName.isEmpty()) {
                return;
            }
            if (!BaseUtils.getBooleanProperty("env.pos.payment.gateway.auto.update.status", true)) {
                return;
            }
            Set<String> gatewayConfig = BaseUtils.getPropertyAsSet(propName);
            StringBuilder sb = new StringBuilder();
            boolean updateProps = false;

            for (String prop : gatewayConfig) {

                int onlineStart = prop.indexOf("Status=Online");
                int offlineStart = prop.indexOf("Status=Offline");
                log.debug("Gateway[{}], line to process {}", name, prop);
                if (onlineStart != -1 && isup) {
                    return;
                }

                if (offlineStart != -1 && !isup) {
                    return;
                }

                if (onlineStart != -1 && !isup) {
                    prop = prop.replaceAll("Status=Online", "Status=Offline");
                    updateProps = true;
                }
                if (offlineStart != -1 && isup) {
                    prop = prop.replaceAll("Status=Offline", "Status=Online");
                    updateProps = true;
                }
                sb.append(prop).append("\r\n");
            }

            if (!updateProps) {
                return;
            }

            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!paymentGatewayLock.tryLock()) {
                            log.debug("Another thread is already updating payment gateway plugin availability status. Wont try to do the same");
                            return;
                        }
                        long start = System.currentTimeMillis();

                        try {
                            log.debug("Going to update gateway plugins availability status [{}] -> [{}]", name, sb.toString());
                            UpdatePropertyRequest updateRequest = new UpdatePropertyRequest();
                            updateRequest.setClient("default");
                            updateRequest.setPropertyName(propName);
                            updateRequest.setPropertyValue(sb.toString());
                            SCAWrapper.getAdminInstance().updateProperty(updateRequest);
                            log.debug("Successfully updated payment gateway plugin status change");
                        } finally {
                            paymentGatewayLock.unlock();
                            long end = System.currentTimeMillis();
                            log.warn("Payment gateway plugin availability status update call took [{}]ms", end - start);
                        }
                    } catch (Throwable e) {
                        log.warn("Failed to update payment gateway plugin availability status change [{}]", e.toString());
                        log.warn("Error: ", e);
                        BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "POS", "Thread Smile-POS-PaymentGateway-AvailabilityUpdater-" + name + " failed to refresh props for status change, reason: " + e.toString());
                    }
                }
            });
            t.setName("Smile-POS-PaymentGateway-AvailabilityUpdater-" + name);
            log.debug("Starting thread to refresh payment gateway plugin availability status");
            t.start();
            log.debug("Started thread to refresh payment gateway plugin availability status");

        } catch (Exception e) {
            log.warn("Error updating payment gateway plugin status");
            e.printStackTrace();
        }
    }

}
