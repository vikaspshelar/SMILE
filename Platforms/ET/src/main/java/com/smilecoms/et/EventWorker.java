/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.et;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.sd.ServiceDiscoveryAgent;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.util.ExceptionManager;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Javassist;
import com.smilecoms.commons.util.Stopwatch;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.et.db.model.*;
import com.smilecoms.et.db.op.DAO;
import com.smilecoms.et.fieldreplacers.IEventFieldReplacer;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodRetryHandler;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.params.HttpParams;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class EventWorker implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(EventWorker.class);
    private boolean mustStop = false;
    private boolean isStopped = false;
    EntityManagerFactory emf;
    private static final Map<Integer, EventTemplate> eventTemplateCache = new ConcurrentHashMap<>();
    private int pauseMillis;
    private int batchSize;
    // Make last expiry start as a random datestamp somewhere in the last hour so the threads start expiring out of sync
    private long lastExpiryDelete = System.currentTimeMillis() - new Random(System.currentTimeMillis()).nextInt(3600000);

    EventWorker(EntityManagerFactory emf) {
        this.emf = emf;
        reloadConfig();
    }

    boolean isStopped() {
        return isStopped;
    }

    public final void reloadConfig() {
        eventTemplateCache.clear();
        pauseMillis = BaseUtils.getIntProperty("env.et.worker.loop.pause.millis");
        batchSize = BaseUtils.getIntProperty("env.et.worker.event.batch.size");
    }

    @Override
    public void run() {
        int numRowsFound = 0;
        while (!mustStop && emf != null) {
            try {
                Stopwatch.start();
                numRowsFound = process(numRowsFound == batchSize);
                Stopwatch.stop();
                log.debug("Processing loop took [{}] for [{}] rows", Stopwatch.millisString(), numRowsFound);
                if (!mustStop) {
                    if (numRowsFound == batchSize) {
                        log.debug("The last run picked up the maximum rows it could so lets not sleep at all");
                    } else if (numRowsFound == 0) {
                        log.debug("The last run picked up no rows. Will sleep for quite a long while");
                        for (int i = 0; i < 100; i++) {
                            if (mustStop) {
                                break;
                            }
                            Utils.sleep((pauseMillis + 5000) / 100); // Sleep for 5s longer if we did nothing as clearly load is low and we dont need to check that often
                        }
                    } else {
                        log.debug("The last run picked up some rows. Will sleep for a short while");
                        Utils.sleep(pauseMillis);
                    }
                }
            } catch (Exception e) {
                log.warn("Error in Event Worker Thread: [{}]", e.toString());
            }
        }
        isStopped = true;
        log.warn("EventWorker has stopped [{}]", Thread.currentThread().getName());
    }

    public void shutDown() {
        mustStop = true;
    }

    private int process(boolean skipOrderBy) {
        EntityManager em = null;
        int ret = 0;
        try {
            log.debug("Event worker is checking for new events to process");
            em = JPAUtils.getEM(emf);
            // skipOrderBy is used as an optimisation. If there are lots of rows then we should not bother ordering as it puts a lot of stress on the DB
            List<Long> pendingEventIds = DAO.getPendingEventIds(em, batchSize, skipOrderBy);
            ret = pendingEventIds.size();

            // Lets try and avoid collissions
            Collections.shuffle(pendingEventIds);

            for (Long id : pendingEventIds) {
                if (mustStop) {
                    break;
                }
                EventData event = null;
                try {
                    log.debug("Getting a locked version of event [{}]", id);
                    JPAUtils.beginTransaction(em);
                    event = em.find(EventData.class, id, LockModeType.PESSIMISTIC_READ);
                    if (!event.getStatus().equals("N")) {
                        log.debug("This event [{}] has subsequently been processed by another thread. Will ignore it", event.getEventDataId());
                        continue;
                    }
                    log.debug("Updating event to Complete so we can commit transaction ASAP");
                    event.setStatus("C");
                    event.setProcessedTimestamp(new Date());
                    em.persist(event);
                } catch (Exception e) {
                    log.warn("Error getting lock on event data id [{}]: [{}], Will move on to next event", id, e.toString());
                    continue;
                } finally {
                    JPAUtils.commitTransaction(em);
                }
                if (event == null) {
                    continue;
                }

                if (BaseUtils.getBooleanProperty("env.et.mustnot.process.subscrtions", false)) {
                    Set<String> excludeTypes = null;
                    Set<String> excludeSubTypes = null;

                    try {
                        excludeTypes = BaseUtils.getPropertyAsSet("env.et.worker.event.exclude.types");
                        excludeSubTypes = BaseUtils.getPropertyAsSet("env.et.worker.event.exclude.subtypes");
                    } catch (com.smilecoms.commons.base.props.PropertyFetchException e) {
                        log.debug("env.pos.cashin.exempt.customer.ids does not exist");
                        excludeTypes = null;
                        excludeSubTypes = null;
                    }

                    if (excludeTypes != null && excludeSubTypes != null) {
                        if (excludeTypes.contains(event.getType()) && excludeSubTypes.contains(event.getSubType())) {
                            log.warn("Going to exclude subscription processing for TYPE [{}] and SUB_TYPE [{}]", event.getType(), event.getSubType());
                            continue;
                        }
                    }
                }

                log.debug("Processing pending event with id [{}] and type [{}]", event.getEventDataId(), event.getType());
                JPAUtils.beginTransaction(em);
                List<Integer> subscriptionIds = DAO.getEventsSubscriptionIds(em, event.getType(), event.getSubType(), event.getEventKey());
                int cnt = 0;
                int total = subscriptionIds.size();
                log.debug("Found [{}] possible subscriptions to event ID [{}]", total, event.getEventDataId());
                JPAUtils.commitTransaction(em);
                for (Integer subscriptionId : subscriptionIds) {
                    if (mustStop && ((total - cnt) > 100)) {
                        // only break out if there are lots to do
                        log.warn("[{}] event subscriptions will not be processed for this event due to shutdown", (total - cnt));
                        break;
                    }
                    cnt++;
                    log.debug("Processing subscription [{}] of [{}] for this event", cnt, total);

                    try {
                        JPAUtils.beginTransaction(em);
                        EventSubscription subscription = em.find(EventSubscription.class, subscriptionId);
                        if (subscription != null) {
                            if (!subscription.getRepeatable().equals("Y")) {
                                //lock subscription to prevent duplicate processing
                                log.debug("Getting a lock on the subscription as its not repeatable and thus should only be used once");
                                subscription = em.find(EventSubscription.class, subscriptionId, LockModeType.PESSIMISTIC_READ);
                                log.debug("Got a lock on the subscription as its not repeatable and thus should only be used once");
                            }
                            if (subscription != null) {
                                boolean didSend = processSubscription(em, subscription, event);
                                if (didSend && !subscription.getRepeatable().equals("Y")) {
                                    log.debug("This subscription is once off and will now be removed");
                                    DAO.deleteSubscription(em, subscriptionId);
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Error trying to process subscription for event", e);
                        BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR,
                                "ET",
                                "Error processing subscription id " + subscriptionId + " for event data id " + event.getEventDataId() + " : " + e.toString());
                    } finally {
                        JPAUtils.commitTransactionAndClear(em);
                    }

                } // end looping through possible subscriptions for event
            } // end looping through new events            

            try {
                if (!mustStop && (System.currentTimeMillis() - lastExpiryDelete > 3600000)) {
                    log.debug("[{}] Starting delete of expired subscriptions", Thread.currentThread().getName());
                    // Only expire every hour or so per thread
                    JPAUtils.beginTransaction(em);
                    DAO.deleteExpiredSubscriptions(em);
                    JPAUtils.commitTransaction(em);
                    log.debug("[{}] Finished delete of expired subscriptions", Thread.currentThread().getName());
                    lastExpiryDelete = System.currentTimeMillis();
                }
            } catch (Exception e) {
                log.warn("Error deleting expired events: [{}]", e.toString());
            }
        } catch (Exception e) {
            log.warn("Error in EventWorker Process()", e);
        } finally {
            JPAUtils.closeEM(em);
        }
        return ret;
    }

    private boolean processSubscription(EntityManager em, EventSubscription subscription, EventData event) {
        boolean mustSend = true;
        try {
            List<EventSubscriptionField> fields = DAO.getSubscriptionsFields(em, subscription.getEventSubscriptionId());

            if (!subscription.getDataMatch().isEmpty()) {

                String dataMatch = subscription.getDataMatch().replace("\\|", "_REPLACE_PIPE_");
                for (String rule : dataMatch.split("\\|")) { // All rules must pass to send
                    if (rule.startsWith("jdbc/")) {
                        int colon = rule.indexOf(":");
                        String dsName = rule.substring(0, colon);
                        String sql = rule.substring(colon + 1);
                        sql = getTemplateDataWithFieldReplacements(sql, event.getData(), fields, false);
                        log.debug("This subscription has a SQL query match of [{}] that will be tested. A result of >=1 means it passes", sql);
                        if (!checkSubscriptionWithSQL(dsName, sql)) {
                            mustSend = false;
                            break;
                        }
                    } else {
                        log.debug("This subscription has a data matching regular expression of [{}] that will be tested", rule);
                        rule = rule.replace("_REPLACE_PIPE_", "\\|");
                        if (Utils.matchesWithPatternCache(event.getData(), rule)) {
                            log.debug("Data matches regular expression - sending for notification processing");
                        } else {
                            log.debug("Data does not match regular expression");
                            mustSend = false;
                            break;
                        }
                    }
                }

            } else {
                log.debug("Subscription does not have a regular expression or sql to test against the data - sending for notification processing");
            }
            if (mustSend) {
                sendNotificationForSubscription(em, subscription, event, fields);
            }
        } catch (Exception e) {
            log.warn("Error processing subscription [{}] [{}]", subscription.getEventSubscriptionId(), e.toString());
            new ExceptionManager(log).reportError(e);
        }
        return mustSend;
    }

    private void sendNotificationForSubscription(EntityManager em, EventSubscription subscription, EventData event, List<EventSubscriptionField> fields) {
        try {
            EventTemplate template = eventTemplateCache.get(subscription.getEventTemplateId());
            if (template == null) {
                template = DAO.getEventTemplate(em, subscription.getEventTemplateId());
                eventTemplateCache.put(subscription.getEventTemplateId(), template);
            }
            String notifyData = getTemplateDataWithFieldReplacements(template.getData(), event.getData(), fields, template.getProtocol().equals("HTTP"));
            sendNotification(event, template.getProtocol(), template.getDestination(), notifyData);

        } catch (Exception e) {
            log.warn("Error sending notification for subscription [{}] [{}]", subscription.getEventSubscriptionId(), e.toString());
            log.warn("Error: ", e);
        }

    }

    private String getTemplateDataWithFieldReplacements(String notifyData, String eventData, List<EventSubscriptionField> fields, boolean escapeXML) {
        for (EventSubscriptionField field : fields) {
            if (log.isDebugEnabled()) {
                log.debug("Processing field of type [{}] in template. Field Name [{}] Field Data [{}]", new Object[]{field.getReplacementType(), field.getFieldName(), field.getReplacementData()});
            }
            String[] bits = field.getReplacementData().split("\\|", 2);
            String firstBitBeforePipe = bits[0];
            String replaceWith; // The string that must be put into the template wherever the field.getFieldName is found

            switch (field.getReplacementType()) {
                case "P":
                    //Property replacement in template
                    replaceWith = getPropertyValue(firstBitBeforePipe, escapeXML);
                    break;
                case "R":
                    //Regex search in event data and replace in template
                    replaceWith = getRegexValue(firstBitBeforePipe, eventData, escapeXML);
                    break;
                case "D":
                    //direct search in event data and replace in template
                    replaceWith = getDirectValue(firstBitBeforePipe, escapeXML);
                    break;
                case "|":
                    //Split event data on pipe delimeter, select the requested index (as per replacement data value) and replace in template
                    replaceWith = getSplitValue(Integer.parseInt(firstBitBeforePipe), eventData, escapeXML);
                    break;
                case "J":
                    //Pass event data to Java class
                    replaceWith = getClassCallValue(firstBitBeforePipe, eventData, escapeXML);
                    break;
                case "X":
                    //Pass event data to XSLT
                    replaceWith = getXSLTValue(firstBitBeforePipe, eventData, escapeXML);
                    break;
                case "A":
                    //Pass all in event data
                    replaceWith = eventData;
                    break;
                default:
                    replaceWith = "";
            }

            if (bits.length == 2) {

                String complexCode = bits[1];

                if (complexCode.startsWith("java:")) {
                    try {
                        complexCode = complexCode.substring(5);

                        replaceWith = (String) Javassist.runCode(
                                new Class[]{this.getClass(), com.smilecoms.commons.util.Utils.class},
                                complexCode,
                                replaceWith);
                        log.debug("ReplaceWith is now [{}]", replaceWith);
                    } catch (Exception e) {
                        log.warn("Error running event field formatting code for replace data [{}]: [{}]", replaceWith, Utils.getDeepestCause(e).toString());
                    }
                } else if (complexCode.startsWith("jdbc/")) {
                    int colon = complexCode.indexOf(":");
                    String dsName = complexCode.substring(0, colon);
                    String sql = complexCode.substring(colon + 1);
                    replaceWith = runSQLWithOneParam(dsName, sql, Long.parseLong(replaceWith));
                } else if (complexCode.startsWith("fr:")) {
                    try {
                        complexCode = complexCode.substring(3);
                        replaceWith = getClassCallValue(complexCode, replaceWith, escapeXML);
                        log.debug("ReplaceWith is now [{}]", replaceWith);
                    } catch (Exception e) {
                        log.warn("Error running event field replacer code for replace data [{}]: [{}]", replaceWith, Utils.getDeepestCause(e).toString());
                    }
                }

            }

            log.debug("Replacing [{}] with [{}]", field.getFieldName(), replaceWith);
            notifyData = notifyData.replaceAll(field.getFieldName(), replaceWith);

        }
        // In case the data needs a UUID
        notifyData = notifyData.replaceAll("_MAKE_UUID_", Utils.getUUID());
        return notifyData;
    }

    private String getPropertyValue(String propertyName, boolean escapeXML) {
        String value = BaseUtils.getProperty(propertyName);
        String replaceWith;
        if (escapeXML) {
            replaceWith = StringEscapeUtils.escapeXml(value);
        } else {
            replaceWith = value;
        }
        return replaceWith;
    }

    private String getRegexValue(String regex, String data, boolean escapeXML) {
        String replacementData = findRegexInString(regex, data);
        String replaceWith;
        if (escapeXML) {
            replaceWith = StringEscapeUtils.escapeXml(replacementData);
        } else {
            replaceWith = replacementData;
        }
        return replaceWith;
    }

    private String getDirectValue(String replacementData, boolean escapeXML) {
        String replaceWith;
        if (escapeXML) {
            replaceWith = StringEscapeUtils.escapeXml(replacementData);
        } else {
            replaceWith = replacementData;
        }
        return replaceWith;
    }

    private String getSplitValue(int index, String data, boolean escapeXML) {
        String[] bits = data.split("\\|");
        String replaceWith;

        if (bits.length > index && escapeXML) {
            replaceWith = StringEscapeUtils.escapeXml(bits[index]);
        } else if (bits.length > index) {
            replaceWith = bits[index];
        } else {
            // In case invalid index is asked for
            replaceWith = "";
        }
        return replaceWith;
    }

    private String getXSLTValue(String xslt, String xml, boolean escapeXML) {
        try {
            String replacementData = Utils.doXSLTransform(xml, xslt, getClass().getClassLoader());
            String replaceWith;
            if (escapeXML) {
                replaceWith = StringEscapeUtils.escapeXml(replacementData);
            } else {
                replaceWith = replacementData;
            }
            return replaceWith;
        } catch (Exception ex) {
            log.warn("Error using XSLT to get field replacement value", ex);
        }
        return "";
    }

    private String getClassCallValue(String className, String data, boolean escapeXML) {
        try {
            IEventFieldReplacer lookupClass;
            lookupClass = (IEventFieldReplacer) EventWorker.class.getClassLoader().loadClass(className).newInstance();
            String fieldValue = lookupClass.getValue(data);
            String replaceWith;
            if (escapeXML) {
                replaceWith = StringEscapeUtils.escapeXml(fieldValue);
            } else {
                replaceWith = fieldValue;
            }
            return replaceWith;
        } catch (Exception ex) {
            log.warn("Error using class to get field replacement value", ex);
        }
        return "";
    }

    private String findRegexInString(String regex, String data) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(data);
        m.find();
        return m.group();
    }

    private void sendNotification(EventData event, String protocol, String destination, String notifyData) throws Exception {
        switch (protocol) {
            case "LOG":
                sendNotificationLOG(destination, notifyData);
                break;
            case "HTTP":
                sendNotificationHTTP(destination, notifyData);
                break;
            case "JDBC":
                sendNotificationJDBC(destination, notifyData);
                break;
            case "JAVA":
                sendNotificationJavassist(destination, notifyData, event);
                break;
        }
    }

    private void sendNotificationJavassist(String destination, String notifyData, EventData event) {
        log.debug("Sending event notification via javassist code");
        List<String> paramsList = Utils.getListFromCommaDelimitedString(notifyData);
        Object[] params = paramsList.toArray();
        try {
            SCAWrapper.setThreadsRequestContextAsAdmin();
            Javassist.runCode(new Class[]{this.getClass()}, destination, event, new EventHelper(), params);
        } catch (Throwable e) {
            log.warn("Error running event javassist code: ", Utils.getDeepestCause(e));
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "ET", "Error running event javassist code with params: " + notifyData + " : " + Utils.getDeepestCause(e).toString() + " Stack Trace: " + Utils.getStackTrace(e));
        } finally {
            SCAWrapper.removeThreadsRequestContext();
        }
        log.debug("Sent event notification via javassist code");
    }

    private void sendNotificationLOG(String destination, String notifyData) {
        log.warn("LOG NOTIFICATION: [{}] to DESTINATION: [{}]", notifyData, destination);
    }

    private void sendNotificationHTTP(String destination, String notifyData) throws Exception {
        boolean useProxy = false;
        if (destination.startsWith("proxy:")) {
            useProxy = true;
            destination = destination.replace("proxy:", "");
        }
        String ep;
        if (destination.equals("SCA")) {
            log.debug("This notification must be sent to an available SCA instance. Will pick one");
            ep = ServiceDiscoveryAgent.getInstance().getAvailableService("SCA").getURL();
        } else if (!destination.startsWith("http")) {
            log.debug("This notification must be sent to an available instance of platform [{}]", destination);
            ep = ServiceDiscoveryAgent.getInstance().getAvailableService(destination).getURL();
        } else {
            log.debug("This notification must be sent to the actual provided destination [{}]", destination);
            // PCB 202 - Allow multiple endpoints and load balance across them
            String[] epArray = destination.split("\\|");
            int random = new Random().nextInt(epArray.length);
            ep = epArray[random];
        }
        log.debug("URL [{}] will be sent [{}]", ep, notifyData);
        doXMLPost(ep, notifyData, useProxy);
        log.debug("Finished sending notification to URL [{}]", ep);
    }

    /**
     * This function is used to post given data to a specified URL.
     *
     * @param strURL the URL to which the data should be POSTed.
     * @param data the data to be POSTed.
     * @return the response received from the POST operation.
     */
    private void doXMLPost(String strURL, String data, boolean useProxy) {
        PostMethod post = null;
        HttpClient httpclient = null;
        try {
            // Get file to be posted
            post = new PostMethod(strURL);
            RequestEntity entity = new ByteArrayRequestEntity(data.getBytes(), "text/xml; charset=utf-8");

            post.setRequestEntity(entity);
            // Get HTTP client
            httpclient = new HttpClient();
            // use  SimpleHttpConnectionManager cause one can close its ports and prevent close-wait states
            httpclient.setHttpConnectionManager(new SimpleHttpConnectionManager());
            HttpMethodRetryHandler myretryhandler = new HttpMethodRetryHandler() {
                @Override
                public boolean retryMethod(final HttpMethod method, final IOException exception, int executionCount) {
                    // do not retry
                    return false;
                }
            };

            HttpParams httpParams = new HttpClientParams();
            httpParams.setParameter(HttpClientParams.RETRY_HANDLER, myretryhandler);
            httpParams.setIntParameter(HttpClientParams.SO_TIMEOUT, 30000);
            httpParams.setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, 5000);
            httpParams.setParameter(HttpClientParams.PREEMPTIVE_AUTHENTICATION, Boolean.FALSE);
            httpclient.setParams((HttpClientParams) httpParams);

            if (useProxy) {
                String proxyHost = BaseUtils.getProperty("env.http.proxy.host", "");
                int proxyPort = BaseUtils.getIntProperty("env.http.proxy.port", 0);
                if (!proxyHost.isEmpty() && proxyPort > 0) {
                    log.debug("Post will use a proxy server [{}][{}]", proxyHost, proxyPort);
                    httpclient.getHostConfiguration().setProxy(proxyHost, proxyPort);
                } else {
                    httpclient.getHostConfiguration().setProxyHost(null);
                }
            } else {
                httpclient.getHostConfiguration().setProxyHost(null);
            }
            if (log.isDebugEnabled()) {
                log.debug("About to post to method...");
            }
            int result = httpclient.executeMethod(post);
            // Display status code
            if (log.isDebugEnabled()) {
                log.debug("Finished post to method");
                log.debug("HTTP Post Response status code: " + result);
                log.debug("Response body: ");
                log.debug(post.getResponseBodyAsString());
            }
            if (result != 200 && !post.getResponseBodyAsString().equals("OK")) {
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "ET", "Non 200 result code returned when processing event with HTTP Notification. [URL: " + strURL + "] [Request: " + data + "] [Response: " + post.getResponseBodyAsString() + "]");
            }

        } catch (Exception e) {
            log.warn("Error posting to address " + strURL + " : " + e.toString());
            new ExceptionManager(log).reportError(e, "POST URL: " + strURL);
        } finally {
            // Release current connection to the connection pool once you are done
            try {
                post.releaseConnection();
            } catch (Exception ex) {
                log.warn("Error releasing http post connection:" + ex.toString());
            }
            try {
                ((SimpleHttpConnectionManager) httpclient.getHttpConnectionManager()).shutdown();
            } catch (Exception ex) {
                log.warn("Error releasing http client connection:" + ex.toString());
            }
        }
    }

    private void sendNotificationJDBC(String dsName, String query) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection(dsName);
            ps = conn.prepareStatement(query);
            long start = 0;
            if (log.isDebugEnabled()) {
                log.debug("About to run JDBC event subscription: " + query + " on datasource name " + dsName);
                start = System.currentTimeMillis();
            }
            int updates = ps.executeUpdate();
            conn.commit();
            if (log.isDebugEnabled()) {
                log.debug("Finished running JDBC event subscription. SQL took [{}]ms. Rows updated [{}]", (System.currentTimeMillis() - start), updates);
            }

        } catch (Exception e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                log.warn("Error rolling back transaction", ex);
            }
            String err = e.toString();
            if (err.contains("Duplicate entry")) {
                log.debug("Error running JDBC event subscription: " + query + ". " + err);
            } else {
                log.warn("Error running JDBC event subscription: " + query + ". " + err);
                new ExceptionManager(log).reportError(e);
            }

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ex) {
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception ex) {
            }
        }
    }

    private boolean checkSubscriptionWithSQL(String dsName, String query) {
        Connection conn = null;
        PreparedStatement ps = null;
        boolean res = false;
        try {
            conn = getConnection(dsName);
            ps = conn.prepareStatement(query);
            if (log.isDebugEnabled()) {
                log.debug("About to run JDBC subscription check : " + query + " on datasource name " + dsName);
            }
            ResultSet resset = ps.executeQuery();
            if (!resset.next()) {
                res = false;
            } else {
                long queryres = resset.getLong(1);
                log.debug("Query result is [{}] >= 1 means subscription applies", queryres);
                if (queryres >= 1) {
                    res = true;
                }
            }
            conn.commit();

        } catch (Exception e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                log.warn("Error rolling back transaction", ex);
            }
            String err = e.toString();
            log.warn("Error running JDBC event subscription: " + query + ". " + err);
            new ExceptionManager(log).reportError(e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ex) {
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception ex) {
            }
        }
        return res;
    }

    private String runSQLWithOneParam(String dsName, String query, long param) {
        Connection conn = null;
        PreparedStatement ps = null;
        boolean res = false;
        String queryres = "";
        try {
            conn = getConnection(dsName);
            ps = conn.prepareStatement(query);
            ps.setLong(1, param);
            if (log.isDebugEnabled()) {
                log.debug("About to run query : " + query + " on datasource name " + dsName + " with parameter " + param);
            }
            ResultSet resset = ps.executeQuery();
            if (!resset.next()) {
                queryres = null;
            } else {
                queryres = resset.getString(1);
            }
            conn.commit();
        } catch (Exception e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                log.warn("Error rolling back transaction", ex);
            }
            String err = e.toString();
            log.warn("Error running JDBC event subscription: " + query + ". " + err);
            new ExceptionManager(log).reportError(e);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ex) {
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception ex) {
            }
        }
        return queryres;
    }

    private Connection getConnection(String dsName) throws Exception {
        Connection conn = JPAUtils.getNonJTAConnection(dsName);
        if (log.isDebugEnabled()) {
            log.debug("Successfully got connection using datasource " + dsName);
        }
        return conn;
    }

    public String format(String s) {
        double d = Double.parseDouble(s);
        d = d / 100;
        d = java.lang.Math.round(d);
        return String.valueOf(d);
    }

    public String add1GBForBirthday(java.util.List paramsList) throws Exception {

        if (1 == 1) {
            return "";
        }

        String sProductInstanceId = (String) paramsList.get(0);
        int piId = Integer.valueOf(sProductInstanceId);

        com.smilecoms.commons.sca.ProductInstance pi = com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().getProductInstance(piId, com.smilecoms.commons.sca.StProductInstanceLookupVerbosity.MAIN_SVC);

        com.smilecoms.commons.sca.Customer cust = com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().getCustomer(pi.getCustomerId(), com.smilecoms.commons.sca.StCustomerLookupVerbosity.CUSTOMER);
        String day = new java.text.SimpleDateFormat("MMdd").format(new java.util.Date());
        if (!day.equals(cust.getDateOfBirth().substring(4))) {
            return "Not birthday";
        }

        com.smilecoms.commons.sca.ProductServiceInstanceMapping mapping = (com.smilecoms.commons.sca.ProductServiceInstanceMapping) pi.getProductServiceInstanceMappings().get(0);
        com.smilecoms.commons.sca.ServiceInstance si = mapping.getServiceInstance();

        com.smilecoms.commons.sca.PurchaseUnitCreditRequest pucr = new com.smilecoms.commons.sca.PurchaseUnitCreditRequest();
        pucr.setProductInstanceId(piId);
        pucr.setAccountId(si.getAccountId());
        pucr.setUnitCreditSpecificationId(108);
        pucr.setNumberToPurchase(1);
        String year = new java.text.SimpleDateFormat("yyyy").format(new java.util.Date());
        pucr.setUniqueId("CAMP_BDAY_" + sProductInstanceId + "_" + year);
        com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().purchaseUnitCredit(pucr);
        return "Done";
    }
}
