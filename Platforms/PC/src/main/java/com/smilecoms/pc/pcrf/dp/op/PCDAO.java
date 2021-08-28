/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pc.pcrf.dp.op;

import com.smilecoms.pc.pcrf.db.model.DBAFSession;
import com.smilecoms.pc.pcrf.db.model.DBIPCANSession;
import com.smilecoms.pc.pcrf.db.model.DBPCCRule;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.slf4j.*;

/**
 *
 * @author richard
 */
public class PCDAO {

    private static final Logger log = LoggerFactory.getLogger(PCDAO.class);

    public static List<DBIPCANSession> getIPCANSessionsByEndUserPrivate(EntityManager em, String endUserPrivate) {
        List<DBIPCANSession> IPCANSessions = null;
        //IPCANSession DBIPCANSession = null;

        try {
            Query q = em.createNativeQuery("SELECT * FROM ipcan_sessions WHERE end_user_private=?", DBIPCANSession.class);
            q.setParameter(1, endUserPrivate);
            //IPCANSession = (DBIPCANSession) q.getSingleResult();
            IPCANSessions = (List<DBIPCANSession>) q.getResultList();
        } catch (Exception e) {
            log.warn("Exception caught when retrieving IPCANSession [{}]", e);
        }
        //return DBIPCANSession;
        return IPCANSessions;
    }

    public static List<DBAFSession> getAFSessionsFromBindingIdentifier(EntityManager em, String bindingIdentifier) {

        List<DBAFSession> AFSessions = null;

        try {
            Query q = em.createNativeQuery("SELECT * FROM af_sessions WHERE binding_identifier=?", DBAFSession.class);
            q.setParameter(1, bindingIdentifier);
            AFSessions = (List<DBAFSession>) q.getResultList();
        } catch (Exception e) {
            log.warn("Exception caught when retrieving AFSessions [{}]", e);
        }
        return AFSessions;
    }

    public static List<DBPCCRule> getPCCRulesFromBindingIdentifier(EntityManager em, String bindingIdentifier) {
        List<DBPCCRule> PCCRules = null;

        try {
            Query q = em.createNativeQuery("SELECT * FROM pcc_rules WHERE binding_identifier=?", DBPCCRule.class);
            q.setParameter(1, bindingIdentifier);
            PCCRules = (List<DBPCCRule>) q.getResultList();
        } catch (Exception e) {
            log.warn("Exception caught when retrieving PCCRules [{}]", e);
        }
        return PCCRules;
    }
}
