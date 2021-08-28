/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.engine;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.Codec;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.SimpleDelayQueue;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.mm.db.model.MessageQueue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class EngineQueue extends SimpleDelayQueue implements Runnable {

    private static EntityManagerFactory emf = null;
    private static final Logger log = LoggerFactory.getLogger(EngineQueue.class);
    private boolean mustStop = false;
    private String label = null;
    private boolean stopped = false;

    public static int getMaxAllowedMemoryMessages() {
        return BaseUtils.getIntProperty("global.mm.persistent.queue.threshold.size", 5000) * 2;
    }

    EngineQueue(EntityManagerFactory emf, String label) {
        super();
        this.label = label;
        EngineQueue.emf = emf;
        log.debug("Engine Queue has EMF [{}]", emf.toString());
        Thread t = new Thread(this);
        t.setContextClassLoader(this.getClass().getClassLoader());
        t.setName("Smile-MM-EngineQueuePopper");
        log.warn("[{}]Starting Engine Queue popping thread", label);
        t.start();
    }

    public void shutDown() {
        mustStop = true;
        int tries = 0;
        while (!stopped && tries < 1000) {
            tries++;
            log.warn("[{}]Waiting for engine queue popper to shutdown", label);
            Utils.sleep(10);
        }
        Map<Object, Long> items = super.getAllForced();
        int persisted = 0;
        log.warn("[{}]Due to shutdown, any messages in memory queue will be persisted to DB queue. There are [{}] such messages", label, items.size());
        for (Object msgObj : items.keySet()) {
            BaseMessage msg = (BaseMessage) msgObj;
            log.debug("[{}]Moving message with ID [{}] to persistent queue due to shutdown", label, msg.getMessageId());
            Long delay = items.get(msgObj);
            int delayInt = delay.intValue() < 0 ? 0 : delay.intValue();
            try {
                putOnPersistentQueue(msg, delayInt);
                persisted++;
                log.debug("[{}]Done moving message with ID [{}] to persistent queue due to shutdown", label, msg.getMessageId());
            } catch (Exception e) {
                log.warn("[{}]Error persisting message from memory queue to DB queue during shutdown: [{}]", label, e.toString());
            }
        }
        log.warn("[{}]Messages in memory queue have been persisted to DB queue. There were [{}] such messages", label, persisted);
    }

    void popToMemory(String messageId, long millisDelay) {
        log.debug("[{}]Popping message with ID [{}] off the persistent queue if it can be found", label, messageId);
        EntityManager em = null;
        MessageQueue m = null;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            m = em.find(MessageQueue.class, messageId);
            if (m == null) {
                log.debug("[{}]Message with Id [{}] has already been popped off the persistent queue", label, messageId);
                return;
            }
            if (!m.getQueueLabel().equals(label)) {
                log.debug("[{}]Message does not belong to this queue - ignoring it", label);
                return;
            }
            BaseMessage msg = deserialize(m.getMessage());
            Query q = em.createNativeQuery("delete from message_queue where MESSAGE_ID=?");
            q.setParameter(1, messageId);
            int deleted = 0;
            try {
                deleted = q.executeUpdate();
                JPAUtils.commitTransaction(em);
            } catch (Exception e) {
                if (!e.toString().contains("Deadlock")) {
                    throw e;
                }
            }
            if (deleted == 0) {
                log.debug("Another thread already dequeued this message");
                return;
            }
            // Only put on queue when we are certain its removed off the database
            if (msg.getPriority() == null) {
                msg.setPriority(BaseMessage.Priority.MEDIUM);
            }
            switch (msg.getPriority()) {
                case LOW:
                    super.put(msg, millisDelay, Priority.LOW);
                    break;
                case MEDIUM:
                    super.put(msg, millisDelay, Priority.MEDIUM);
                    break;
                case HIGH:
                    super.put(msg, millisDelay, Priority.HIGH);
            }
            log.debug("[{}]Successfully popped message Id [{}] from persistent queue to memory queue", label, messageId);
        } catch (Exception e) {
            log.warn("[{}]Error popping object to memory queue [{}]. Message Id [{}]", new Object[]{label, e.toString(), messageId});
            if (e.toString().contains("Deadlock")) {
                JPAUtils.rollbackTransaction(em);
                return;
            }
            String msg = "Error popping message off DB queue - " + e.toString() + " MessageId: " + messageId;
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "MM", msg);
            if (m != null && em != null && e instanceof java.io.OptionalDataException) {
                byte[] msgData = m.getMessage();
                em.remove(m);
                JPAUtils.commitTransaction(em);
                java.io.OptionalDataException ode = (java.io.OptionalDataException) e;
                log.warn("Message Hex: [{}] [{}] [{}]", new Object[]{Codec.binToHexString(msgData), ode.eof, ode.length});
            } else {
                JPAUtils.rollbackTransaction(em);
            }
        } finally {
            JPAUtils.closeEM(em);
        }
    }

    @Override
    public void put(Object o, long millisDelay, Priority p) {

        if (mustStop) {
            throw new RuntimeException("Engine Queue is Stopping and cannot accept new messages");
        }

        BaseMessage bMsg = (BaseMessage) o;

        /**
         * If the delay is longer than a certain threshold, then the message
         * must be put in a persistent queue and that queue then read for ready
         * messages and pulled off the persistent queue and put in the
         * SimpleDelayQueue
         */
        int size = this.size();
        if (millisDelay <= BaseUtils.getIntProperty("global.mm.persistent.queue.threshold.millis", 30000)
                && size < BaseUtils.getIntProperty("global.mm.persistent.queue.threshold.size", 5000)) {
            log.debug("[{}]Message delay is less than the threshold and memory queue is not full so memory queue is being used. Queue size is [{}]", label, size);
            super.put(bMsg, millisDelay, p);
        } else {
            try {
                putOnPersistentQueue(bMsg, (int) millisDelay);
            } catch (Exception e) {
                log.warn("[{}]Error putting message on persistent queue, will put it on memory queue instead [{}]", label, e.toString());
                if (this.size() <= getMaxAllowedMemoryMessages()) {
                    super.put(bMsg, millisDelay, p);
                } else {
                    log.error("Memory queue is over [{}] so message will be dumped altogether!!!", getMaxAllowedMemoryMessages());
                }
            }
        }
    }

    private void putOnPersistentQueue(BaseMessage msg, int millisDelay) throws Exception {
        log.debug("Putting message [{}] onto persistent queue with delay [{}]ms", msg, millisDelay);
        EntityManager em = JPAUtils.getEM(emf);
        try {
            JPAUtils.beginTransaction(em);
            MessageQueue msgOnQueue = new MessageQueue();
            msgOnQueue.setMessageId(msg.getMessageId());
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            cal.add(Calendar.MILLISECOND, millisDelay);
            if (log.isDebugEnabled()) {
                log.debug("Dequeue timestamp for message[{}] is [{}]", msg, cal.getTime());
            }
            msgOnQueue.setDequeueTimestamp(cal.getTime());
            msgOnQueue.setMessage(serialize(msg));
            msgOnQueue.setQueueLabel(label);
            em.persist(msgOnQueue);
        } catch (Exception e) {
            throw e;
        } finally {
            JPAUtils.commitTransactionAndClose(em);
            log.debug("Finished putting message [{}] on persistent queue", msg);
        }
    }

    protected static byte[] serialize(BaseMessage o) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out;
        out = new ObjectOutputStream(baos);
        out.writeObject(o);
        out.close();
        return baos.toByteArray();
    }

    protected static BaseMessage deserialize(byte[] b) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = null;
        ObjectInputStream in = null;
        try {
            bais = new ByteArrayInputStream(b);
            in = new ObjectInputStream(bais);
            return (BaseMessage) in.readObject();
        } finally {
            if (in != null) {
                in.close();
            }
            if (bais != null) {
                bais.close();
            }
        }
    }

    @Override
    public void run() {
        long lastSentQueueSize = 0;
        Utils.sleep(10000); // Wait for plugins to all start
        while (!mustStop) {
            EntityManager em = null;
            boolean sleepForAWhile = false;
            try {
                em = JPAUtils.getEM(emf);
                log.debug("[{}]Engine queue popper is at top of loop", label);
                Query q = em.createNativeQuery("select message_id from message_queue where QUEUE_LABEL=? and DEQUEUE_TIMESTAMP <= now() LIMIT 5000");
                q.setParameter(1, label);
                log.debug("[{}]Engine queue popper is about to get list of messages due to be put on memory queue", label);
                List<String> ids = q.getResultList();
                sleepForAWhile = ids.isEmpty();
                log.debug("[{}]Engine queue popper is about to loop through [{}] results", label, ids.size());
                int maxMemQueue = BaseUtils.getIntProperty("global.mm.persistent.queue.threshold.size", 5000);
                for (String id : ids) {
                    if (mustStop) {
                        break;
                    }
                    if (super.size() >= maxMemQueue) {
                        log.debug("Memory queue is greater than [{}] so this DB row will be left for later", maxMemQueue);
                        break;
                    }
                    log.debug("[{}]EngineQueue popping thread found a message to move to memory queue [{}]", label, id);
                    popToMemory(id, 0);
                }
                long now = System.currentTimeMillis();
                if (now - lastSentQueueSize > 30000) {
                    lastSentQueueSize = now;
                    BaseUtils.sendStatistic(BaseUtils.getHostNameFromKernel(), "MM Message Queue", "count", this.size(), "");
                }
            } catch (Exception e) {
                log.warn("Error: ", e);
                log.warn("[{}]Error in popping thread [{}]", label, e.toString());
            } finally {
                JPAUtils.closeEM(em);
            }

            if (!mustStop) {
                if (sleepForAWhile) {
                    for (int i = 0; i < 100; i++) {
                        if (mustStop) {
                            break;
                        }
                        Utils.sleep(50); // Sleep for 5s longer if we did nothing as clearly load is low and we dont need to check that often
                    }
                } else {
                    Utils.sleep(1000);
                }
            }
        }
        log.warn("[{}]EngineQueue popping thread has now stopped", label);
        stopped = true;
    }

    int getMemoryQueueSize() {
        return super.size();
    }
}
