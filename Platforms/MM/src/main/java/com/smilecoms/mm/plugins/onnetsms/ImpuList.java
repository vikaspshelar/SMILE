/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.plugins.onnetsms;

import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.mm.plugins.onnetsms.model.IpsmgwContact;
import com.smilecoms.mm.plugins.onnetsms.model.IpsmgwImpu;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jaybeepee
 */
public class ImpuList {

//    private ConcurrentHashMap<String, Impu> impus;
    private static final Logger log = LoggerFactory.getLogger(ImpuList.class);
    private final EntityManagerFactory emf;

    public ImpuList(EntityManagerFactory emf) {
        log.debug("Initialising IMPU list");
        this.emf = emf;
//        impus = new ConcurrentHashMap<String, Impu>();
    }

    /**
     * Add a contact for an IMPU - if the IMPU AOR does no exist a new IMPU is
     * created added to the list and then updated with the new contact
     *
     * @param impuAOR
     * @param newContact
     */
    public void addContact(String impuAOR, ImpuContact newContact) {
        log.debug("Asked to add contact [{}] to IMPU [{}] with path [{}]", new Object[]{newContact.getUri(), impuAOR, newContact.getPathList()});
        IpsmgwImpu dbImpu;
        IpsmgwContact dbContact;
        EntityManager em = JPAUtils.getEM(emf);

        dbImpu = getImpu(impuAOR);
        if (dbImpu == null) {
            dbImpu = addImpuToDb(impuAOR);
        }

        try {
            JPAUtils.beginTransaction(em);

            try {
                dbContact = (IpsmgwContact) em.createNamedQuery("IpsmgwContact.findByUri").setParameter("uri", newContact.getUri()).getSingleResult();
            } catch (Exception e) {
                dbContact = null;
            }
            if (dbContact == null) {
                dbContact = new IpsmgwContact();
                dbContact.setUri(newContact.getUri());
                dbContact.setSmsFormat(newContact.getSmsType().name());
                dbContact.setPath(newContact.getPathList().getHeaderValue());
                em.persist(dbContact);
                em.flush();
            }
            dbImpu.getIpsmgwContactCollection().add(dbContact);
            em.persist(dbImpu);
            em.flush();
        } catch (Exception ex) {
            log.error("Unable to add IMPU/CONTACT to DB [{}]", ex.getMessage());
            log.warn("Error: ", ex);
        } finally {
            JPAUtils.commitTransactionAndClose(em);
        }
    }

    public void removeContact(String impuAOR, ImpuContact contact) {
        log.debug("Asked to remove contact [{}] from IMPU [{}]", contact.getUri(), impuAOR);

        IpsmgwImpu dbImpu = getImpu(impuAOR);

        if (dbImpu == null) {
            log.debug("no IMPU exists for: [{}], so cant remove contact [{}]", impuAOR, contact.getUri());
            return;
        }
        String contactToDel = null;

        for (IpsmgwContact c : dbImpu.getIpsmgwContactCollection()) {
            if (c.getUri().equalsIgnoreCase(contact.getUri())) {
                contactToDel = c.getUri();
            }
        }

        if (contactToDel != null) {
            log.debug("removing contact [{}] from IMPU [{}]", contact.getUri(), impuAOR);

            EntityManager em = JPAUtils.getEM(emf);
            try {
                JPAUtils.beginTransaction(em);
                IpsmgwContact dbContact = (IpsmgwContact) em.createNamedQuery("IpsmgwContact.findByUri").setParameter("uri", contactToDel).getSingleResult();
                dbImpu.getIpsmgwContactCollection().remove(dbContact);
                em.persist(dbImpu);
                if (dbImpu.getIpsmgwContactCollection().isEmpty()) {
                    em.remove(dbImpu);
                }
                em.flush();
                em.refresh(dbContact);
                if (dbContact.getIpsmgwImpuCollection().isEmpty()) {
                    log.debug("contact not associated to any IMPUs, deleting");
                    em.remove(dbContact);
                } else {
                    log.debug("contact still has [{}] IMPUs assocaited so will not delete", dbContact.getIpsmgwImpuCollection().size());
                }
                em.flush();
            } catch (Exception e) {
                log.error("failed to remove contact/impu from DB [{}]", e.getMessage());
                log.warn("Error: ", e);
                JPAUtils.rollbackTransaction(em);
            } finally {
                JPAUtils.commitTransactionAndClose(em);
            }
        } else {
            log.error("Could not find contact to delete");
        }
    }

    void removeContact(String aor, String uri) {
        ImpuContact c = new ImpuContact(uri);
        removeContact(aor, c);
    }

    /**
     * Get IMPU from list of active IMPUs. If IMPU does not exist in memory (we
     * are not the master of the IMPU), check DB. This is typically called when
     * trying to terminate an SMS to an IMPU (we need to get all the associated
     * contacts
     *
     * @param aor
     * @return
     */
    public IpsmgwImpu getImpu(String aor) {
        log.debug("looking for IMPU [{}]", aor);
        IpsmgwImpu dbImpu = getImpuFromDB(aor);
        return dbImpu;
    }

    private IpsmgwImpu addImpuToDb(String aor) {
        IpsmgwImpu ret = null;

        EntityManager em = JPAUtils.getEM(emf);
        try {
            log.debug("Adding IMPU: [{}] to DB", aor);
            JPAUtils.beginTransaction(em);
            IpsmgwImpu dbImpu = new IpsmgwImpu();
            dbImpu.setUri(aor);
            em.persist(dbImpu);
            em.flush();
            em.refresh(dbImpu);
            ret = dbImpu;
        } catch (Exception ex) {
            log.error("Unable to add subscription to DB [{}]", ex.getMessage());
            log.warn("Error: ", ex);
        } finally {
            JPAUtils.commitTransactionAndClose(em);
        }

        return ret;
    }

    private IpsmgwImpu getImpuFromDB(String aor) {
        EntityManager em = JPAUtils.getEM(emf);
        try {
            log.debug("looking for IMPU: [{}] in DB", aor);
            JPAUtils.beginTransaction(em);
            IpsmgwImpu dbImpu = (IpsmgwImpu) em.createNamedQuery("IpsmgwImpu.findByUri").setParameter("uri", aor).getSingleResult();
            log.debug("Found IMPU [{}] in DB", aor);
            return dbImpu;
        } catch (NoResultException ex) {
            log.debug("IMPU [{}] not foudn in DB", aor);
        } catch (Exception ex) {
            log.error("error searching DB for IMPU [{}]", ex.getMessage());
            log.warn("Error: ", ex);
        } finally {
            JPAUtils.commitTransactionAndClose(em);
        }

        return null;
    }
}
