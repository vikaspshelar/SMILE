/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.et.db.op;

import com.smilecoms.et.db.model.EventData;
import com.smilecoms.et.db.model.EventSubscription;
import com.smilecoms.et.db.model.EventSubscriptionField;
import com.smilecoms.et.db.model.EventTemplate;
import com.smilecoms.xml.schema.et.SubscriptionField;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class DAO {

    private static final Logger log = LoggerFactory.getLogger(DAO.class);

    public static List<Long> getPendingEventIds(EntityManager em, int max, boolean skipOrderBy) {
        String query;
        if (skipOrderBy) {
            log.debug("Skipping ordering of events");
            query = "select event_data_id from event_data USE INDEX (STATUS) where STATUS='N' and EVENT_TIMESTAMP <= now() LIMIT ?";
        } else {
            query = "select event_data_id from event_data USE INDEX (STATUS) where STATUS='N' and EVENT_TIMESTAMP <= now() ORDER BY EVENT_TIMESTAMP ASC LIMIT ?";
        }
        Query q = em.createNativeQuery(query);
        q.setParameter(1, max);
        return (List<Long>) q.getResultList();
    }

    public static List<EventSubscription> getSimilarSubscriptions(EntityManager em, EventSubscription sub) {
        Query q = em.createNativeQuery("select * from event_subscription where TYPE=? and SUB_TYPE=?  and EVENT_KEY=?"
                + " and DATA_MATCH=? and EVENT_TEMPLATE_ID=? and REPEATABLE=?", EventSubscription.class);
        q.setParameter(1, sub.getType());
        q.setParameter(2, sub.getSubType());
        q.setParameter(3, sub.getEventKey());
        q.setParameter(4, sub.getDataMatch());
        q.setParameter(5, sub.getEventTemplateId());
        q.setParameter(6, sub.getRepeatable());
        return (List<EventSubscription>) q.getResultList();
    }

    public static List<Integer> getEventsSubscriptionIds(EntityManager em, String type, String subType, String eventKey) {
        Query q = em.createNativeQuery("select event_subscription_id from event_subscription where TYPE=? and (SUB_TYPE=? or SUB_TYPE=?) and (EVENT_KEY=? or EVENT_KEY=?) and EXPIRY_TIMESTAMP >= now()");
        q.setParameter(1, type);
        q.setParameter(2, subType);
        q.setParameter(3, "*");
        q.setParameter(4, eventKey);
        q.setParameter(5, "*");
        return (List<Integer>) q.getResultList();
    }

    public static List<EventSubscriptionField> getSubscriptionsFields(EntityManager em, Integer eventSubscriptionId) {
        Query q = em.createNativeQuery("select * from event_subscription_field where event_subscription_id=?", EventSubscriptionField.class);
        q.setParameter(1, eventSubscriptionId);
        return (List<EventSubscriptionField>) q.getResultList();
    }

    public static EventTemplate getEventTemplate(EntityManager em, Integer eventTemplateId) {
        Query q = em.createNativeQuery("select EVENT_TEMPLATE_ID, DESTINATION, DATA, PROTOCOL from event_template where EVENT_TEMPLATE_ID=?", EventTemplate.class);
        q.setParameter(1, eventTemplateId);
        return (EventTemplate) q.getSingleResult();
    }

    public static void deleteExpiredSubscriptions(EntityManager em) {
        try {
            Date now = new Date();
            Query q = em.createNativeQuery("delete F from event_subscription_field F join event_subscription S on (F.EVENT_SUBSCRIPTION_ID = S.EVENT_SUBSCRIPTION_ID) WHERE S.EXPIRY_TIMESTAMP < ?");
            q.setParameter(1, now);
            int deleted = q.executeUpdate();
            log.debug("Deleted [{}] expired fields", deleted);
            q = em.createNativeQuery("delete S from event_subscription S where S.EXPIRY_TIMESTAMP < ?");
            q.setParameter(1, now);
            deleted = q.executeUpdate();
            log.debug("Deleted [{}] expired subscriptions", deleted);
        } catch (Exception e) {
            log.warn("Error deleting expired subscriptions. Will ignore. [{}]", e.toString());
        }
    }

    public static void deleteSubscription(EntityManager em, int eventSubscriptionId) {
        Query q = em.createNativeQuery("delete from event_subscription_field where EVENT_SUBSCRIPTION_ID=?");
        q.setParameter(1, eventSubscriptionId);
        int fieldsDeleted = q.executeUpdate();
        q = em.createNativeQuery("delete from event_subscription where EVENT_SUBSCRIPTION_ID=?");
        q.setParameter(1, eventSubscriptionId);
        int subsDeleted = q.executeUpdate();
        log.debug("Deleted [{}] fields and [{}] subscriptions for event subscription id [{}]", new Object[]{fieldsDeleted, subsDeleted, eventSubscriptionId});
    }

    public static EventSubscription getSameSubscription(EntityManager em, com.smilecoms.xml.schema.et.EventSubscription newEventSubscription) {

        StringBuilder sql = new StringBuilder();
        sql.append("select EVENT_SUBSCRIPTION_ID FROM ( select F.EVENT_SUBSCRIPTION_ID, COUNT(*) as CNT from ");
        sql.append("event_subscription S, event_subscription_field F where S.TYPE=? AND S.SUB_TYPE=? ");
        sql.append("AND S.EVENT_KEY=? AND S.DATA_MATCH=? AND S.EVENT_TEMPLATE_ID=? AND S.REPEATABLE=? ");
        sql.append("AND S.EVENT_SUBSCRIPTION_ID = F.EVENT_SUBSCRIPTION_ID ");

        List<SubscriptionField> fields = newEventSubscription.getSubscriptionFieldList().getSubscriptionFields();
        int fieldCnt = fields.size();

        if (fieldCnt > 0) {
            sql.append("AND (");
        }
        for (int i = 1; i <= fieldCnt; i++) {
            sql.append("(F.FIELD_NAME=? AND F.REPLACEMENT_TYPE=? AND F.REPLACEMENT_DATA=?)");
            if (i != fieldCnt) {
                sql.append(" OR ");
            }
        }
        if (fieldCnt > 0) {
            sql.append(") ");
        }
        sql.append("GROUP BY F.EVENT_SUBSCRIPTION_ID) as tmp where CNT=?");

        log.debug("Complex SQL for duplicate lookup is [{}]", sql);

        Query q = em.createNativeQuery(sql.toString());
        q.setParameter(1, newEventSubscription.getEventType());
        q.setParameter(2, newEventSubscription.getEventSubType());
        q.setParameter(3, newEventSubscription.getEventKey());
        q.setParameter(4, newEventSubscription.getDataMatch());
        q.setParameter(5, newEventSubscription.getTemplateId());
        q.setParameter(6, newEventSubscription.isRepeatable() ? "Y" : "N");
        int lastField = 6;
        for (int i = 0; i < fieldCnt; i++) {
            q.setParameter(lastField + 1, fields.get(i).getFieldName());
            lastField++;
            q.setParameter(lastField + 1, fields.get(i).getReplacementType());
            lastField++;
            q.setParameter(lastField + 1, fields.get(i).getReplacementData());
            lastField++;
        }
        q.setParameter(lastField + 1, fieldCnt);

        List<Integer> existingIDs = q.getResultList();
        if (existingIDs.size() > 1) {
            log.warn("Something is wrong in that there are duplicate event subscriptions in the event table! Going to delete them");
            for (int i = 1; i < existingIDs.size(); i++) {
                deleteSubscription(em, existingIDs.get(i));
            }
        }
        if (existingIDs.size() >= 1) {
            //' Get first one - there should only ever be 0 or 1
            Integer existingID = existingIDs.get(0);
            log.debug("Found an existing identical subscription with ID [{}]", existingID);
            return em.find(EventSubscription.class, existingID);
        }
        return null;
    }

    public static List<EventData> getEvents(EntityManager em, String eventType, String eventSubType, String eventKey, Date startDate, Date endDate, int resultLimit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM event_data E where ");
        if (eventType != null && !eventType.isEmpty()) {
            sql.append("E.TYPE=? AND ");
        }
        if (eventSubType != null && !eventSubType.isEmpty()) {
            sql.append("E.SUB_TYPE=? AND ");
        }
        if (eventKey != null && !eventKey.isEmpty()) {
            sql.append("E.EVENT_KEY=? AND ");
        }
        if (startDate != null) {
            sql.append("E.EVENT_TIMESTAMP >= ? AND ");
        }
        if (endDate != null) {
            sql.append("E.EVENT_TIMESTAMP <= ? AND ");
        }
        // Remove the trailing "AND "
        sql.setLength(sql.length() - 4);
        sql.append("ORDER BY E.EVENT_TIMESTAMP DESC ");
        sql.append("LIMIT ?");

        log.debug("SQL Query for event data is [{}]", sql);

        Query q = em.createNativeQuery(sql.toString(), EventData.class);
        int position = 0;

        if (eventType != null && !eventType.isEmpty()) {
            q.setParameter(++position, eventType);
        }
        if (eventSubType != null && !eventSubType.isEmpty()) {
            q.setParameter(++position, eventSubType);
        }
        if (eventKey != null && !eventKey.isEmpty()) {
            q.setParameter(++position, eventKey);
        }
        if (startDate != null) {
            q.setParameter(++position, startDate);
        }
        if (endDate != null) {
            q.setParameter(++position, endDate);
        }

        q.setParameter(++position, resultLimit);

        return q.getResultList();
    }

    public static void deleteEvent(EntityManager em, long id) {
        Query q = em.createNativeQuery("delete from event_data where EVENT_DATA_ID=? and STATUS='N' and EVENT_TIMESTAMP > now()");
        q.setParameter(1, id);
        q.executeUpdate();
    }
}
