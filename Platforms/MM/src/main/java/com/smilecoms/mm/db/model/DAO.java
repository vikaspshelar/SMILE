/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.db.model;

import com.smilecoms.commons.util.JPAUtils;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author richard.good
 */
public class DAO {

    private static final Logger log = LoggerFactory.getLogger(DAO.class);

    public static void updateShortMessage(EntityManagerFactory emf, String messageId, String status) {
        EntityManager em = JPAUtils.getEM(emf);
        Query q = em.createNativeQuery("UPDATE sms SET STATUS = ? WHERE MESSAGE_ID = ? LIMIT 1");
        q.setParameter(1, status);
        q.setParameter(2, messageId);
        try {
            JPAUtils.beginTransaction(em);
            q.executeUpdate();
        } catch (Exception e) {
            log.debug("Error updating short message [{}] with messageId [{}] - probably was a short code so wasn't added in the first place", e.toString(), messageId);
        } finally {
            JPAUtils.commitTransactionAndClose(em);
        }
    }
}
