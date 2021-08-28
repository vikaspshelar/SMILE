package com.smilecoms.et;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.platform.SmileWebService;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.et.db.model.EventData;
import com.smilecoms.et.db.op.DAO;
import com.smilecoms.xml.et.ETError;
import com.smilecoms.xml.et.ETSoap;
import com.smilecoms.xml.schema.et.Done;
import com.smilecoms.xml.schema.et.Event;
import com.smilecoms.xml.schema.et.EventList;
import com.smilecoms.xml.schema.et.EventQuery;
import com.smilecoms.xml.schema.et.EventSubscription;
import com.smilecoms.xml.schema.et.PlatformInteger;
import com.smilecoms.xml.schema.et.PlatformLong;
import com.smilecoms.xml.schema.et.StDone;
import com.smilecoms.xml.schema.et.SubscriptionField;
import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * The Event Management (EM) project provides logic to manage events. Eg: We can
 * generate an event when an agent logs into a phone so that we can record where
 * the agent is working from. The project does not wrap any other backend
 * platforms as the event management functionality is specific to Smile and is
 * completely custom built. The project uses JPA to do much of its database work
 * where it stores all the event management data in the event table in the
 * SmileDB mysql database. <br/> <br/> The event table has the following fields:
 * <UL> <LI>EVENT_ID : Uniquely identifies events</LI> <LI>SMILE_ID : The
 * customer/agent that the event relates to</LI> <LI>EVENT_DATA : Pipe-delimited
 * data relevant to the event</LI> <LI>EVENT_TYPE : The type of event</LI>
 * <LI>DATE : A timestamp for the event</LI> </UL> <br/>
 *
 *
 * <br/><br/>
 *
 * @author PCB
 */
@WebService(serviceName = "ET", portName = "ETSoap", endpointInterface = "com.smilecoms.xml.et.ETSoap", targetNamespace = "http://xml.smilecoms.com/ET", wsdlLocation = "ETServiceDefinition.wsdl")
@Stateless
@HandlerChain(file = "/handler.xml")
public class EventManager extends SmileWebService implements ETSoap {

    @javax.annotation.Resource
    javax.xml.ws.WebServiceContext wsctx;
    @PersistenceContext(unitName = "ETPU")
    private EntityManager em;
    private static final Date wayFuture = new Date();

    static {
        wayFuture.setTime(16725225600000L); // GMT Year 2500!!!
    }

    /**
     * Create an event in the event table.
     *
     * @param createEvent : Event type, Event data, smile ID
     * @return : Done if the event was created successfully
     * @throws com.smilecoms.xml.et.ETError : If there was an error inserting
     * the event into the table
     */
    @Override
    public com.smilecoms.xml.schema.et.Done createEvent(com.smilecoms.xml.schema.et.Event createEvent) throws ETError {
        setContext(createEvent, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        boolean created;
        try {
            Date date;
            if (createEvent.getDate() != null) {
                date = Utils.getJavaDate(createEvent.getDate());
            } else {
                date = new Date();
            }
            created = com.smilecoms.commons.platform.PlatformEventManager.createEvent(createEvent.getEventType(), createEvent.getEventSubType(), createEvent.getEventKey(), createEvent.getEventData(), createEvent.getUniqueKey(), date);
        } catch (Exception ex) {
            throw processError(ETError.class, ex);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        Done d = makeDone();
        if (!created) {
            d.setDone(StDone.FALSE);
        }
        return d;
    }

    /**
     * Utility method to make a complex boolean object with value TRUE.
     *
     * @return The resulting complex object
     */
    private com.smilecoms.xml.schema.et.Done makeDone() {
        com.smilecoms.xml.schema.et.Done done = new com.smilecoms.xml.schema.et.Done();
        done.setDone(com.smilecoms.xml.schema.et.StDone.TRUE);
        return done;
    }

    @Override
    public Done isUp(String isUpRequest) throws ETError {
        if (!BaseUtils.isPropsAvailable()) {
            throw createError(ETError.class, "Properties are not available so this platform will be reported as down");
        }
        return makeDone();
    }

    @Override
    public Done createEventSubscription(EventSubscription newEventSubscription) throws ETError {
        setContext(newEventSubscription, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        try {
            com.smilecoms.et.db.model.EventSubscription dbSub = new com.smilecoms.et.db.model.EventSubscription();
            dbSub.setType(newEventSubscription.getEventType());
            dbSub.setSubType(newEventSubscription.getEventSubType());
            dbSub.setEventKey(newEventSubscription.getEventKey());
            dbSub.setDataMatch(newEventSubscription.getDataMatch());
            dbSub.setEventTemplateId(newEventSubscription.getTemplateId());
            dbSub.setExpiryTimestamp(newEventSubscription.getExpiryDateTime() == null ? wayFuture : Utils.getJavaDate(newEventSubscription.getExpiryDateTime()));
            dbSub.setRepeatable(newEventSubscription.isRepeatable() ? "Y" : "N");

            log.debug("Checking if this is a duplicate - if so the expiry date will be set to the greater of the two");
            com.smilecoms.et.db.model.EventSubscription existingSub = DAO.getSameSubscription(em, newEventSubscription);
            if (existingSub != null) {
                log.debug("There was an existing subscription found");
                if (dbSub.getExpiryTimestamp().after(existingSub.getExpiryTimestamp())) {
                    Date greaterDate = dbSub.getExpiryTimestamp();
                    log.debug("Of the two, [{}] is the greater expiry date which is on the new subscription. Updating existing one with new date", greaterDate);
                    existingSub.setExpiryTimestamp(greaterDate);
                    em.persist(existingSub);
                }
            } else {
                log.debug("There was no existing subscription found. Persisting new one");
                em.persist(dbSub);
                for (SubscriptionField field : newEventSubscription.getSubscriptionFieldList().getSubscriptionFields()) {
                    com.smilecoms.et.db.model.EventSubscriptionField dbField = new com.smilecoms.et.db.model.EventSubscriptionField();
                    dbField.setEventSubscription(dbSub);
                    dbField.setFieldName(field.getFieldName());
                    dbField.setReplacementData(field.getReplacementData());
                    dbField.setReplacementType(field.getReplacementType());
                    em.persist(dbField);
                }
                log.debug("Finished persisting new subscription");
            }
        } catch (Exception ex) {
            throw processError(ETError.class, ex);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public EventList getEvents(EventQuery eventQuery) throws ETError {
        setContext(eventQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        EventList eventList = new EventList();
        try {
            List<EventData> dbEvents = DAO.getEvents(em, eventQuery.getEventType(), eventQuery.getEventSubType(), eventQuery.getEventKey(),
                    Utils.getJavaDate(eventQuery.getDateFrom()), Utils.getJavaDate(eventQuery.getDateTo()), eventQuery.getResultLimit());
            for (EventData event : dbEvents) {
                eventList.getEvents().add(getXMLEvent(event));
            }
            eventList.setNumberOfEvents(eventList.getEvents().size());
        } catch (Exception ex) {
            throw processError(ETError.class, ex);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return eventList;
    }
    
    private Event getXMLEvent(EventData dbEvent) {
        Event xmlEvent = new Event();
        xmlEvent.setDate(Utils.getDateAsXMLGregorianCalendar(dbEvent.getEventTimestamp()));
        xmlEvent.setEventData(dbEvent.getData());
        xmlEvent.setEventKey(dbEvent.getEventKey());
        xmlEvent.setEventSubType(dbEvent.getSubType());
        xmlEvent.setEventType(dbEvent.getType());
        xmlEvent.setUniqueKey(dbEvent.getUniqueKey());
        xmlEvent.setEventId(dbEvent.getEventDataId());
        return xmlEvent;
    }

    @Override
    public Done deleteFutureEvent(PlatformLong eventId) throws ETError {
        setContext(eventId, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        try {
            DAO.deleteEvent(em, eventId.getLong());
        } catch (Exception ex) {
            throw processError(ETError.class, ex);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }
}