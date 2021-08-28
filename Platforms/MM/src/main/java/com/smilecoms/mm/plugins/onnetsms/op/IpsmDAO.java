/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.plugins.onnetsms.op;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.ExceptionManager;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.mm.plugins.onnetsms.PresentityLock;
import com.smilecoms.mm.plugins.onnetsms.model.IpsmgwContact;
import com.smilecoms.mm.plugins.onnetsms.model.IpsmgwImpu;
import com.smilecoms.mm.plugins.onnetsms.model.IpsmgwSubscription;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jaybeepee
 */
public class IpsmDAO {

    private static final Logger log = LoggerFactory.getLogger(IpsmDAO.class);
    private static final String QUERY_GET_CONTACTS_FOR_IMPU = "select C.* from ipsmgw_impu I, ipsmgw_contact C, ipsmgw_impu_contact L where I.ID = L.IMPU_ID AND C.ID = L.CONTACT_ID AND I.uri = ? GROUP BY C.URI ORDER BY C.EXPIRES DESC;";

    public static IpsmgwImpu getImpuFromDBCreateIfMissing(EntityManager em, String aor) {
        try {
            log.debug("looking for IMPU: [{}] in DB", aor);
            IpsmgwImpu dbImpu = (IpsmgwImpu) em.createNamedQuery("IpsmgwImpu.findByUri").setParameter("uri", aor).getSingleResult();
            log.debug("Found IMPU [{}] in DB", aor);
            return dbImpu;
        } catch (NoResultException ex) {
            log.debug("IMPU [{}] not found in DB", aor);
            Query q = em.createNativeQuery("INSERT IGNORE INTO ipsmgw_impu (URI) values(?)");
            q.setParameter(1, aor);
            try {
                q.executeUpdate();
            } catch (Exception e) {
                log.warn("Error adding IMPU. Probably a deadlock");
                JPAUtils.restartTransaction(em);
            }
            return getImpuFromDBCreateIfMissing(em, aor);
        } catch (NonUniqueResultException ex) {
            log.error("IMPU [{}] has multiple entries (NonUniqueResultException......", aor);
        } catch (Exception ex) {
            log.error("error searching DB for IMPU [{}]", ex.getMessage());
            log.warn("Error: ", ex);
        }
        return null;
    }

    /**
     * Get all of the subscriptions for this IPSM server that are expiring
     * within threshold1 seconds OR ANY subscriptions within threshold2 seconds.
     *
     * @param em
     * @param uri - URI of watcher contact to compare against, typically URI of
     * this IPSMgw...
     * @param threshold1 - threshold to get my own subscriptions about to expire
     * @param threshold2 - threshold to get ANY subscription about to expire
     * @return
     */
    public static List<Long> getActiveSubscriptionsExpiringWithin(EntityManager em, String uri, int threshold1, int threshold2, int fetchSize) {
        if (threshold2 > threshold1) {
            log.warn("Subscription expiry threshold seems wrong, please check threshold2 [{}] > threshold1 [{}]", threshold2, threshold1);
        }

        Query q = em.createNativeQuery("SELECT ID FROM ipsmgw_subscription WHERE (STATE='active' OR STATE='failed') AND ((watcher_uri = ? AND expires < DATE_ADD(now(), INTERVAL ? SECOND)) OR (expires < DATE_ADD(now(), INTERVAL ? SECOND))) ORDER BY expires ASC LIMIT ?");
        q.setParameter(1, uri);
        q.setParameter(2, threshold1);
        q.setParameter(3, threshold2);
        q.setParameter(4, fetchSize);
        return q.getResultList();
    }

    public static IpsmgwContact getContact(EntityManager em, String contactUri, Boolean createIfNotFound) {
        IpsmgwContact newContact = null;
        log.debug("checking DB for contact [{}]", contactUri);
        try {
            TypedQuery<IpsmgwContact> x = em.createNamedQuery("IpsmgwContact.findByUri", IpsmgwContact.class);
//            x.setLockMode(LockModeType.PESSIMISTIC_WRITE);        //TODO - not sure we actually need this lock....
            x.setParameter("uri", contactUri);
            newContact = x.getSingleResult();
        } catch (NoResultException ex) {
            log.debug("[{}] not found in DB", contactUri);
        } catch (Exception ex) {
            log.error("error trying to query for contact [{}] in DB", contactUri);
            log.warn("Error: ", ex);
            return null;
        }

        if (createIfNotFound && (newContact == null)) {
            log.debug("contact with URI [{}] not found in DB - adding", contactUri);
            newContact = new IpsmgwContact();
            newContact.setUri(contactUri);
            newContact.setSmsFormat(IpsmgwContact.SmsFormatType.sip.name());
            try {
                em.persist(newContact);
                em.flush();
//                em.lock(newContact, LockModeType.PESSIMISTIC_WRITE);
//                JPAUtils.persistAndFlush(em, newContact);
            } catch (Exception ex) {
                log.warn("Failed to create contact in DB [{}] with exception [{}] - assuming it's already there....", new Object[]{contactUri, ex.getMessage()});
//                log.warn("Error: ", ex);
            }
        }

        return newContact;
    }

    public static List<IpsmgwContact> getContactsForImpu(String aor, EntityManagerFactory emf) {
        List<IpsmgwContact> contacts;

        EntityManager em = JPAUtils.getEM(emf);
        try {
            JPAUtils.beginTransaction(em);
            Query q = em.createNativeQuery(QUERY_GET_CONTACTS_FOR_IMPU, IpsmgwContact.class);
            q.setParameter(1, aor);
            contacts = q.getResultList();
            return contacts;
        } catch (NoResultException ex) {
            log.debug("IMPU [{}] not found in DB", aor);
        } catch (NonUniqueResultException ex) {
            log.error("IMPU [{}] has multiple entries (NonUniqueResultException......", aor);
        } catch (Exception ex) {
            log.error("error searching for contacts for IMPU: [{}] - [{}]", new Object[]{aor, ex.getMessage()});
            log.warn("Error: ", ex);
        } finally {
            JPAUtils.commitTransactionAndClose(em);
        }

        return null;
    }

    public static IpsmgwSubscription getLockedSubscription(EntityManager em, long id) {
        PresentityLock lock = null;
        IpsmgwSubscription subscription = null;
        try {
            // FIrst find out what presentity URI to lock
            Query qPU = em.createNativeQuery("SELECT presentity_uri FROM ipsmgw_subscription WHERE ID = ?");
            qPU.setParameter(1, id);
            String presentityUri = (String) qPU.getSingleResult();
            Query q = em.createNativeQuery("SELECT * FROM ipsmgw_subscription WHERE ID = ? FOR UPDATE", IpsmgwSubscription.class);
            q.setParameter(1, id);
            lock = new PresentityLock(presentityUri);
            subscription = (IpsmgwSubscription) q.getSingleResult();
            subscription.setLock(lock);
            return subscription;
        } catch (NoResultException e) {
        } finally {
            if (subscription == null && lock != null) {
                lock.unlock();
            }
        }
        return null;
    }

    public static IpsmgwSubscription getLockedSubscription(EntityManager em, String presentityUri, String callID) {
        IpsmgwSubscription subscription = null;
        PresentityLock lock = new PresentityLock(presentityUri);
        try {
            Query q = em.createNativeQuery("SELECT * FROM ipsmgw_subscription WHERE presentity_uri = ? AND call_ID = ? FOR UPDATE", IpsmgwSubscription.class);
            q.setParameter(1, presentityUri);
            q.setParameter(2, callID);
            subscription = (IpsmgwSubscription) q.getSingleResult();
            log.debug("Found subscription with presentity [{}] and call-id [{}] in state [{}]", new Object[]{presentityUri, callID, subscription.getState()});
            subscription.setLock(lock);
            return subscription;
        } catch (NoResultException ex) {
            log.debug("Can't find Subscription for presentity [{}] and Call-ID [{}] in DB", new Object[]{presentityUri, callID});
        } catch (NonUniqueResultException ex) {
            log.debug("Non-unique result getting Subscription for presentity [{}] and Call-ID [{}] in DB", new Object[]{presentityUri, callID});
        } finally {
            if (subscription == null) {
                lock.unlock();
                log.debug("Subscription with presentity [{}] and call-id [{}] NOT FOUND", new Object[]{presentityUri, callID});
            }
        }
        return null;
    }

    /**
     * Try and get the subscription from the DB - we look based on presentityUri
     * and WatcherUri and scscfUri. If it doesn't exist we will create a new one
     *
     * @param presentityUri
     * @param WatcherURI - URI of IPSM server instance (GF3)
     * @return
     */
    public static IpsmgwSubscription getLockedSubscription(EntityManager em, String presentityUri, String WatcherUri, String ScscfUri, String callId) throws Exception {
        IpsmgwSubscription subscription = null;
        Query query = null;
        PresentityLock lock = new PresentityLock(presentityUri);
        try {
            //search based on presentity AND scscURI in case this is a new subscription from a different s-cscf
            //at the moment we will leave the old subscription to expire.... (TODO: maybe we should unsubscribe?).
            query = em.createNativeQuery("SELECT * FROM ipsmgw_subscription WHERE presentity_uri = ? AND SCSCF_URI = ? FOR UPDATE", IpsmgwSubscription.class);
            query.setParameter(1, presentityUri);
            query.setParameter(2, ScscfUri);
            subscription = (IpsmgwSubscription) query.getSingleResult();
            subscription.setLock(lock);
        } catch (NoResultException ex) {
            log.debug("no result in DB for presentityUri [{}] - adding", presentityUri);
            try {
                Query q = em.createNativeQuery("INSERT IGNORE INTO ipsmgw_subscription (PRESENTITY_URI, WATCHER_URI, SCSCF_URI, STATE, CALL_ID, CSEQ, RETRIES) VALUES (?, ?, ?, ?, ?, ?, ?)");
                q.setParameter(1, presentityUri);
                q.setParameter(2, WatcherUri);
                q.setParameter(3, ScscfUri);
                q.setParameter(4, IpsmgwSubscription.State.init.name());
                q.setParameter(5, callId);
                q.setParameter(6, 0);
                q.setParameter(7, 0);
                q.executeUpdate();
                log.debug("persisted new subscription in DB for presentity [{}]", presentityUri);
                subscription = (IpsmgwSubscription) query.getSingleResult();
                subscription.setLock(lock);
            } catch (Exception e) {
                log.error("Failed to create new subscription in DB for presentity [{}]", presentityUri);
                throw e;
            }
        } catch (NonUniqueResultException ex) {
            lock.unlock();
            log.error("More than one row returned for Presentity URI [{}]... this should not happend", presentityUri);
            log.warn("Error: ", ex);
        } finally {
            if (subscription == null) {
                lock.unlock();
            }
        }

        return subscription;
    }

    /**
     * remove all IMPUs associated to the subscription - typically used when one
     * IMPU is terminated and we need to terminate the entire implicit set
     *
     * @param em - EntityManager
     * @param s - Subscription - assume locked
     */
    public static void removeImpusFromSubscription(EntityManager em, IpsmgwSubscription s) {
        log.debug("In removeImpusFromSubscription [{}]", s.getId());
        Query delImpuContactByImpu = em.createNativeQuery("delete from ipsmgw_impu_contact where impu_id=?");
        Query delImpuContactByContact = em.createNativeQuery("delete from ipsmgw_impu_contact where contact_id=?");
        Query delSubscriptionImpu = em.createNativeQuery("delete from ipsmgw_subscription_impu where impu_id=?");
        Query delImpu = em.createNativeQuery("delete from ipsmgw_impu where id=?");
        Query delContact = em.createNativeQuery("delete from ipsmgw_contact where id=?");
        Query q = em.createNativeQuery("select SI.IMPU_ID, IC.CONTACT_ID from ipsmgw_subscription_impu SI join ipsmgw_impu_contact IC on IC.IMPU_ID = SI.IMPU_ID where SI.SUBSCRIPTION_ID=?");
        q.setParameter(1, s.getId());
        List<Object[]> resList = (List<Object[]>) q.getResultList();
        Set<Long> impus = new HashSet<>();
        Set<Long> contacts = new HashSet<>();
        for (Object[] row : resList) {
            impus.add((Long) row[0]);
            contacts.add((Long) row[1]);
        }
        log.debug("Deleting impus [{}] and contacts [{}]", impus, contacts);

        for (long impu : impus) {
            log.debug("Deleting impu_id [{}]", impu);
            delImpuContactByImpu.setParameter(1, impu);
            delSubscriptionImpu.setParameter(1, impu);
            delImpu.setParameter(1, impu);
            delImpuContactByImpu.executeUpdate();
            delSubscriptionImpu.executeUpdate();
            delImpu.executeUpdate();
        }
        for (long contact : contacts) {
            log.debug("Deleting contact_id [{}]", contact);
            delImpuContactByContact.setParameter(1, contact);
            delContact.setParameter(1, contact);
            delImpuContactByContact.executeUpdate();
            delContact.executeUpdate();
        }
        log.debug("Finished removeImpusFromSubscription [{}]", s.getId());
    }

    public static void addImpuToSubscription(EntityManager em, long subId, long impuid) {
        Query q = em.createNativeQuery("INSERT IGNORE INTO ipsmgw_subscription_impu values(?,?)");
        q.setParameter(1, subId);
        q.setParameter(2, impuid);
        q.executeUpdate();
    }

}
