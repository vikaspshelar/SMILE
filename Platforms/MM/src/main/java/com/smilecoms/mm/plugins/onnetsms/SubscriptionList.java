/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.plugins.onnetsms;

import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.mm.plugins.onnetsms.model.IpsmgwSubscription;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jaybeepee
 */
public class SubscriptionList {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionList.class);
    private ConcurrentHashMap<String, IpsmgwSubscription> subsViaImpuList;
    private ConcurrentHashMap<String, IpsmgwSubscription> subsViaDialogList;
    private ConcurrentHashMap<String, IpsmgwSubscription> subsViaImplicitImpuList;
    //the array list must be sorted by expires asc
    private List<IpsmgwSubscription> subs;
    @PersistenceContext(unitName = "MMPU")
    private EntityManagerFactory emf;

    public SubscriptionList() {
        subsViaDialogList = new ConcurrentHashMap<String, IpsmgwSubscription>();
        subsViaImplicitImpuList = new ConcurrentHashMap<String, IpsmgwSubscription>();
        subsViaImpuList = new ConcurrentHashMap<String, IpsmgwSubscription>();
        subs = Collections.synchronizedList(new LinkedList<IpsmgwSubscription>());
    }

    SubscriptionList(EntityManagerFactory emf) {
        this();
        this.emf = emf;
    }

    /**
     * Check if we have a subscription for the AOR aor
     *
     * @param aor - AOR of Subscription (presentity URI).
     * @return true if exists, fals if not
     */
    public boolean exists(String aor, String dialogID) {
        boolean ret = true;

        if (aor != null) {
            if (subsViaImpuList.get(aor) == null) {
                return false;
            }
        }
        if (dialogID != null) {
            if (subsViaDialogList.get(dialogID) == null) {
                return false;
            }
        }

        if (aor == null && dialogID == null) {
            log.warn("checking for subscription but passing in null for both presentity and dialog id");
            return false;
        }
        return ret;
    }

    public boolean add(IpsmgwSubscription s) {
        log.debug("Adding/Updating subscription with IMPU [{}]", s.getPresentityUri());
        /*make sure we don't already have a subscription for this data - TODO: maybe we should abort if the subsc already exists, or maybe even update?*/

        if (s.getDialogId() != null && (subsViaDialogList.get(s.getDialogId()) != null)) {
            log.warn("Trying to add a subscription that already exists in memory..... with dialogId [{}] for Presentity [{}]", new Object[]{s.getDialogId(), s.getPresentityUri()});
//            subs.remove(subsViaDialogList.get(s.getDialogID()));
            return false;
        }//initial dialog's won't have a dialogID yet.
        if (s.getDialogId() != null) {
            subsViaDialogList.put(s.getDialogId(), s);
        }

        if (subsViaImpuList.get(s.getPresentityUri()) != null) {
//            log.warn("Trying to insert Subscription with duplicate IMPU [{}]", s.getTargetSIPAddress().getURI().toString());
            log.warn("Trying to add a subscription that already exists in memory..... with dialogId [{}] for Presentity [{}]", new Object[]{s.getDialogId(), s.getPresentityUri()});
//            subs.remove(subsViaImpuList.get(s.getTargetSIPAddress().getURI().toString()));
            return false;
        }
        subsViaImpuList.put(s.getPresentityUri(), s);

        Calendar c = Calendar.getInstance();
        c.add( Calendar.SECOND, 60);
        if (s.getExpires() == null) {
            log.debug("Subscription expires not set, assuming in init state [{}]", s.getState().toString());
            s.setExpires(c.getTime()); //TODO make property - threshold for subsctiption to become active.
        }

        //the array list must be sorted by expires asc
        if (subs.isEmpty()) {
            subs.add(s);
        } else {
            boolean flag = false;
            for (int i = 0; i < subs.size(); i++) {
                if (subs.get(i).getExpires().after(s.getExpires())) {
//                if (s.getExpiresEpoch() < subs.get(i).getExpiresEpoch()) {
                    subs.add(i, s);
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                subs.add(s);
            }
        }
        
        return true;
    }

    public void linkImplicitImpu(String implicitImpu, IpsmgwSubscription s) {
        subsViaImplicitImpuList.put(implicitImpu, s);
    }

    public void unlinkImplicitImpu(String implicitImpu, Subscription s) {
        subsViaImplicitImpuList.remove(implicitImpu, s);
    }

    public void remove(Subscription s) {
        if (s == null) {
            log.error("STRANGE, Subscription is null");
            return;
        }

        if (s.getDialogID() != null) {
            subsViaDialogList.remove(s.getDialogID(), s);
        } else {
            log.debug("Trying to remove a subscriptioni without a dialogID - IMPU [{}]", s.getTargetSIPAddress().getURI().toString());
        }
        subsViaImpuList.remove(s.getTargetSIPAddress().getURI().toString(), s);
        subs.remove(s);

        //we also need to remove the implicit links - so go through list of impus and remove the links from the implicitimpuhash
        Iterator it = s.getImpuList().entrySet().iterator();
        Entry<String, Impu> implicitImpuEntry;
        Impu impu;

        while (it.hasNext()) {
            implicitImpuEntry = (Entry<String, Impu>) it.next();
            impu = implicitImpuEntry.getValue();
//            unlinkImplicitImpu(impu.getAor(), s);
        }

        EntityManager em = JPAUtils.getEM(emf);
        try {
            log.debug("REMOVING SUBSCRIPTION [{}] FROM DB", s.getTargetSIPAddress().getURI().toString());
            JPAUtils.beginTransaction(em);
            IpsmgwSubscription dbSubs = (IpsmgwSubscription) em.createNamedQuery("IpsmgwSubscription.findByPresentityUri").setParameter("presentityUri", s.getTargetSIPAddress().getURI().toString()).getSingleResult();
            em.remove(dbSubs);
        } catch (Exception ex) {
            log.error("Unable to delete subscription with presentity URI [{}] from DB [{}]", new Object[]{s.getTargetSIPAddress().getURI().toString(), ex.getMessage()});
            log.warn("Error: ", ex);
        } finally {
            JPAUtils.commitTransactionAndClose(em);
        }
    }

    public IpsmgwSubscription getByDialogID(String dialogID) {
        return subsViaDialogList.get(dialogID);
    }

    public IpsmgwSubscription getByImpu(String Impu) {
        log.debug("Searching Subscription dialogs for IMPU [{}]", Impu);
        IpsmgwSubscription ret = subsViaImpuList.get(Impu);   //TODO - if it is expired remote it and return null
        if (ret == null) {
            log.debug("Couldn't find IMPU [{}] in memory...", Impu);
        }
        return ret;
    }

    public IpsmgwSubscription getByImplicityImpu(String implicitImpu) {
        return subsViaImplicitImpuList.get(implicitImpu);
    }

    public List<IpsmgwSubscription> getSubscriptions() {
        return subs;
    }
    
}
