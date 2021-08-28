/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.plugins.offnetsms.smsc;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.JPAUtils;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class DAO {
    
    private static final Logger log = LoggerFactory.getLogger(DAO.class);
    
    public static List<InterconnectSMSC> getAllRemoteSMSCs(EntityManagerFactory emf) {
        
        EntityManager em = JPAUtils.getEM(emf);
        try {
            JPAUtils.beginTransaction(em);
            Query q = em.createNativeQuery("select * from interconnect_smsc", InterconnectSMSC.class);
            List<InterconnectSMSC> ret = q.getResultList();
            for (InterconnectSMSC is : ret) {
                em.detach(is);
            }
            return ret;
        } finally {
            JPAUtils.commitTransactionAndClose(em);
        }
        
    }
    
    public static InterconnectSMSC getSMSCConfigById(EntityManagerFactory emf, int id) {
        
        EntityManager em = JPAUtils.getEM(emf);
        try {
            JPAUtils.beginTransaction(em);
            Query q = em.createNativeQuery("select * from interconnect_smsc where interconnect_smsc_id=?", InterconnectSMSC.class);
            q.setParameter(1, id);
            InterconnectSMSC is = (InterconnectSMSC) q.getSingleResult();
            em.detach(is);
            return is;
        } finally {
            JPAUtils.commitTransactionAndClose(em);
        }
        
    }

    //add like |systemId| here amnd also set systemId to what was requested
    public static InterconnectSMSC getSMSCConfigBySystemId(EntityManagerFactory emf, String systemId) {
        EntityManager em = JPAUtils.getEM(emf);
        try {
            String systemIdPattern = "%|" + systemId + "|%";
            JPAUtils.beginTransaction(em);
            Query q = em.createNativeQuery("select * from interconnect_smsc where system_id=? or system_id like ?", InterconnectSMSC.class);
            q.setParameter(1, systemId);
            q.setParameter(2, systemIdPattern);
            InterconnectSMSC is = (InterconnectSMSC) q.getSingleResult();
            em.detach(is);
            //we set system Id to what was requested
            is.setSystemId(systemId);
            return is;
        } finally {
            JPAUtils.commitTransactionAndClose(em);
        }
    }
    
    public static int storeBind(EntityManagerFactory emf, int interconnectSmscId) {
        EntityManager em = JPAUtils.getEM(emf);
        try {
            JPAUtils.beginTransaction(em);
            InterconnectSMSCBind bind = new InterconnectSMSCBind();
            bind.setInterconnectSmscId(interconnectSmscId);
            bind.setLocalHost(BaseUtils.getHostNameFromKernel());
            bind.setLastCheckDatetime(new Date());
            em.persist(bind);
            em.flush();
            em.refresh(bind);
            log.debug("Stored interconnect smsc bind [{}]", bind.getInterconnectSmscBindId());
            return bind.getInterconnectSmscBindId();
        } finally {
            JPAUtils.commitTransactionAndClose(em);
        }
        
    }
    
    public static void removeBind(EntityManagerFactory emf, int interconnectSMSCBindId) {
        log.debug("Removing interconnect smsc bind [{}]", interconnectSMSCBindId);
        EntityManager em = JPAUtils.getEM(emf);
        try {
            JPAUtils.beginTransaction(em);
            Query q = em.createNativeQuery("delete from interconnect_smsc_bind where interconnect_smsc_bind_id = ? or last_check_datetime < now() - interval 1 day");
            q.setParameter(1, interconnectSMSCBindId);
            q.executeUpdate();
        } finally {
            JPAUtils.commitTransactionAndClose(em);
        }
    }
    
    public static void clearAllHostsBinds(EntityManagerFactory emf) {
        log.debug("Removing all binds for this host");
        EntityManager em = JPAUtils.getEM(emf);
        try {
            JPAUtils.beginTransaction(em);
            Query q = em.createNativeQuery("delete from interconnect_smsc_bind where local_host = ?");
            q.setParameter(1, BaseUtils.getHostNameFromKernel());
            q.executeUpdate();
        } finally {
            JPAUtils.commitTransactionAndClose(em);
        }
    }
    
    public static int updateBind(EntityManagerFactory emf, int interconnectSmscBindId, int interconnectSmscId) {
        log.debug("Updating interconnect smsc bind [{}]", interconnectSmscBindId);
        EntityManager em = JPAUtils.getEM(emf);
        try {
            JPAUtils.beginTransaction(em);
            Query q = em.createNativeQuery("update interconnect_smsc_bind set last_check_datetime=now() where interconnect_smsc_bind_id = ?");
            q.setParameter(1, interconnectSmscBindId);
            int updates = q.executeUpdate();
            if (updates == 0) {
                log.debug("Bind no longer exists. Will add a new one");
                int newBindId = storeBind(emf,interconnectSmscId);
                return newBindId;
            }
            return interconnectSmscBindId;
        } finally {
            JPAUtils.commitTransactionAndClose(em);
        }
    }
    
    static String getBindByTrunkId(EntityManagerFactory emf, String destinationTrunkId) {
        log.debug("Getting a bind to trunk id [{}]", destinationTrunkId);
        EntityManager em = JPAUtils.getEM(emf);
        try {
            JPAUtils.beginTransaction(em);
            Query q = em.createNativeQuery("select B.LOCAL_HOST from interconnect_smsc_bind B join interconnect_smsc S "
                    + "on S.INTERCONNECT_SMSC_ID = B.INTERCONNECT_SMSC_ID "
                    + "where S.INTERNAL_TRUNK_ID=? and B.LOCAL_HOST != ? AND B.LAST_CHECK_DATETIME >= now() - interval ? minute "
                    + "ORDER BY B.LAST_CHECK_DATETIME DESC LIMIT 1");
            q.setParameter(1, destinationTrunkId);
            q.setParameter(2, BaseUtils.getHostNameFromKernel());
            q.setParameter(3, BaseUtils.getIntProperty("env.mm.offnetsms.reroute.to.last.checked.minutes", 5));
            String host = (String) q.getSingleResult();
            log.debug("Host [{}] has a connection to trunk [{}]", host, destinationTrunkId);
            return host;
        } catch (Exception e) {
            log.debug("Could not get host with connection to smsc: [{}]", e.toString());
            return null;
        } finally {
            JPAUtils.commitTransactionAndClose(em);
        }
    }
    
    static long getBindCountByTrunkIdOnAnyHost(EntityManagerFactory emf, String destinationTrunkId) {
        log.debug("Getting a bind to trunk id [{}]", destinationTrunkId);
        EntityManager em = JPAUtils.getEM(emf);
        try {
            JPAUtils.beginTransaction(em);
            Query q = em.createNativeQuery("select count(*) from interconnect_smsc_bind B join interconnect_smsc S "
                    + "on S.INTERCONNECT_SMSC_ID = B.INTERCONNECT_SMSC_ID "
                    + "where S.INTERNAL_TRUNK_ID=? AND B.LAST_CHECK_DATETIME >= now() - interval ? minute");
            q.setParameter(1, destinationTrunkId);
            q.setParameter(2, BaseUtils.getIntProperty("env.mm.offnetsms.reroute.to.last.checked.minutes", 5));
            return (Long) q.getSingleResult();
        } finally {
            JPAUtils.commitTransactionAndClose(em);
        }
    }
    
    static long getBindCountByTrunkIdOnThisHost(EntityManagerFactory emf, String destinationTrunkId) {
        log.debug("Getting a bind to trunk id [{}]", destinationTrunkId);
        EntityManager em = JPAUtils.getEM(emf);
        try {
            JPAUtils.beginTransaction(em);
            Query q = em.createNativeQuery("select B.LOCAL_HOST from interconnect_smsc_bind B join interconnect_smsc S "
                    + "on S.INTERCONNECT_SMSC_ID = B.INTERCONNECT_SMSC_ID "
                    + "where S.INTERNAL_TRUNK_ID=? and B.LOCAL_HOST=? AND B.LAST_CHECK_DATETIME >= now() - interval ? minute "
                    + "ORDER BY B.LAST_CHECK_DATETIME DESC LIMIT 1");
            q.setParameter(1, destinationTrunkId);
            q.setParameter(2, BaseUtils.getHostNameFromKernel());
            q.setParameter(3, BaseUtils.getIntProperty("env.mm.offnetsms.reroute.to.last.checked.minutes", 5));
            return (Long) q.getSingleResult();
        } finally {
            JPAUtils.commitTransactionAndClose(em);
        }
    }
}
