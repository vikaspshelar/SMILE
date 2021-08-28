/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.plugins.onnetsms;

import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.mm.plugins.onnetsms.model.IpsmgwSubscription;
import com.smilecoms.mm.plugins.onnetsms.op.IpsmDAO;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jaybeepee
 */
public class PendingSubscriptionThread extends Thread {

    private static final Logger log = LoggerFactory.getLogger(PendingSubscriptionThread.class);
    private static boolean mustShutdown = false;
    private final ArrayBlockingQueue<Long> pendingSubscriptionDialogs;

    public PendingSubscriptionThread(int id, ArrayBlockingQueue<Long> pendingSubscriptionDialogs) {
        this.setName("Smile-MM-PendingSubscriptionWorker-" + id);
        this.pendingSubscriptionDialogs = pendingSubscriptionDialogs;
    }

    public static void shutDown() {
        mustShutdown = true;
    }

    @Override
    public void run() {

        long start, end;

        while (!mustShutdown) {

            log.debug("Pending Subscription thread running");
            IpsmgwSubscription s = null;
            boolean addToPending = false;
            EntityManager em = null;
            try {
                Long sId;
                try {
                    sId = pendingSubscriptionDialogs.poll(1000, TimeUnit.MILLISECONDS);
                    if (sId == null) {
                        continue;
                    }
                } catch (InterruptedException ex) {
                    continue;
                }
                em = JPAUtils.getEM(OnnetSMSDeliveryPlugin.getEMF());
                start = System.currentTimeMillis();
                JPAUtils.beginTransaction(em);
                s = IpsmDAO.getLockedSubscription(em, sId);
                if (s == null) {
                    continue;
                }

                OnnetSMSDeliveryPlugin.removeFromPendingPresentityURIs(s);
                log.debug("Have a pending subscription for IMPU [{}] and Call-ID [{}] and in state [{}] with ID [{}]", new Object[]{s.getPresentityUri(), s.getCallId(), s.getState(), s.getId()});

                if (((s.getStateEnum() == IpsmgwSubscription.State.pending) || (s.getStateEnum() == IpsmgwSubscription.State.resubscribing)) && (s.getRetries() <= 4)) {/*(s.getState() == Subscription.State.subscribing &&*/

                    if (OnnetSMSDeliveryPlugin.subscribeToIMPU(s, false)) {
                        log.debug("Successfully sent Subscribe for IMPU: [{}], Call-ID: [{}]", new Object[]{s.getPresentityUri(), s.getCallId()});
                        s.setStateEnum(IpsmgwSubscription.State.subscribing);
                    } else {
                        log.error("Failed to send Subscribe to IMPU [{}]... will try again", s.getPresentityUri());
                        s.incrementRetries();
                        if (s.getRetries() > 4) {
                            log.error("failed to send SUBSCRIBE 5 times.... giving up");
                            s.setState(IpsmgwSubscription.State.failed.name());
                        } else {
                            log.debug("Adding pending subscription IMPU: [{}], Call-ID: [{}] for subscribe back to blocking queue for retry #[{}]", new Object[]{
                                s.getPresentityUri(), s.getCallId(), s.getRetries()});
                            addToPending = true;
                        }
                    }
                    em.persist(s);

                } else if (s.getStateEnum() == IpsmgwSubscription.State.unsubscribe) {
                    //unsubscribe
                    log.debug("unsubscribing IMPU [{}], Call-ID: [{}]", new Object[]{s.getPresentityUri(), s.getCallId()});
                    s.setState(IpsmgwSubscription.State.unsubscribing.name());
                    em.flush();
                    if (OnnetSMSDeliveryPlugin.subscribeToIMPU(s, true)) {
                        log.debug("Successfully sent un-subscribe for IMPU: [{}], Call-ID: [{}]", new Object[]{s.getPresentityUri(), s.getCallId()});
                    } else {
                        log.error("Failed to send un-subscribe for IMPU: [{}], Call-ID: [{}]... will try again", new Object[]{s.getPresentityUri(), s.getCallId()});
                        s.incrementRetries();
                        if (s.getRetries() > 4) {
                            log.error("failed to send UN-SUBSCRIBE 5 times.... giving up");
                            s.setState(IpsmgwSubscription.State.inactive.name());
                        } else {
                            log.debug("Adding pending subscription IMPU: [{}], Call-ID: [{}] for unsubscribe back to blocking queue for retry #[{}]", new Object[]{
                                s.getPresentityUri(), s.getCallId(), s.getRetries()});
                            s.setState(IpsmgwSubscription.State.unsubscribe.name());
                            addToPending = true;
                        }
                    }
                    em.persist(s);
                } else {
                    log.debug("Subscription to [{}] with ID [{}] in a strange state in the pending queue. State is [{}], Call-Id is [{}]. It will be ignored...", new Object[]{s.getPresentityUri(), s.getId(), s.getStateEnum().name(), s.getCallId()});
                }

            } catch (Exception ex) {
                log.error("Exception in Pending subscription thread - [{}]", ex.getMessage());
                continue;
            } finally {
                try {
                    if (em != null) {
                        JPAUtils.commitTransactionAndClose(em);
                    }
                } catch (Exception e) {
                    log.warn("Error committing transaction", e);
                }
                try {
                    if (addToPending) {
                        OnnetSMSDeliveryPlugin.addToPendingSubscriptionDialogs(s);
                    }
                } catch (Exception e) {
                    log.warn("Error", e);
                }
                if (s != null) {
                    s.unlock();
                }
            }

            end = System.currentTimeMillis();
            end = end - start;
            if (end > 100) {
                log.debug("pendingsubscription thread processing took [{}] millis", end);
            }
        }

        log.warn("Pending subscription thread shutting down");
    }

}
