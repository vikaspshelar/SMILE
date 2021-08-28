/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.hwf.db.op;

import com.smilecoms.hwf.db.model.HwfProcessDefinition;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;

/**
 *
 * @author paul
 */
public class DAO {
    
    
    public static HwfProcessDefinition getLockedDefinition(EntityManager em, String resource) {
        return em.find(HwfProcessDefinition.class, resource, LockModeType.PESSIMISTIC_READ);
    }
    
    public static List<String> getProcessDefinitionsInState(EntityManager em, String state) {
        Query q = em.createNativeQuery("SELECT RESOURCE_NAME FROM hwf_process_definition where STATUS = ?");
        q.setParameter(1, state);
        return q.getResultList();
    }
    
}
