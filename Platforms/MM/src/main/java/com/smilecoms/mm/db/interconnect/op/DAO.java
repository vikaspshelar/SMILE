/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.db.interconnect.op;

import com.smilecoms.mm.db.interconnectroute.model.PortedNumber;
import com.smilecoms.mm.db.interconnectroute.model.ServiceRate;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jaybeepee
 */
public class DAO {

    private static final Logger log = LoggerFactory.getLogger(DAO.class);

    public static List<ServiceRate> getSMSRates(EntityManager em) {
        Query q = em.createNativeQuery("SELECT R.* FROM service_rate R WHERE R.SERVICE_CODE='SMS' AND R.RATING_HINT NOT LIKE '%IgnoreForRouting=true%' ", ServiceRate.class);
        return q.getResultList();
    }

    public static String getBestTrunkForPartner(EntityManager em, int partnerId) throws Exception {
        Query q = em.createNativeQuery("SELECT T.INTERNAL_ID "
                + "FROM interconnect_trunk T "
                + "LEFT JOIN interconnect_route RT ON T.EXTERNAL_ID = RT.EXTERNAL_ID "     
                + "LEFT JOIN interconnect_smsc SMSC ON T.INTERNAL_ID = SMSC.INTERNAL_TRUNK_ID "
                + "WHERE T.INTERCONNECT_PARTNER_ID = ? AND T.SERVICE_CODES like '%SMS%' "
                + "AND (SMSC.CONFIG is null or SMSC.CONFIG not like '%receiveOnly=true%') "
                + "ORDER BY RT.PRIORITY DESC LIMIT 1");
        q.setParameter(1, partnerId);
        try {
            return (String) q.getSingleResult();
        } catch (Exception e) {
            throw new Exception("Cannot find a trunk connected to partner for SMS -- " + partnerId);
        }
    }
    
    public static List<PortedNumber> getPortedNumbers(EntityManager em, int limit, long greaterthan) {
        Query q = em.createNativeQuery("SELECT NUMBER, INTERCONNECT_PARTNER_ID FROM ported_number where NUMBER > ? ORDER BY NUMBER LIMIT ?", PortedNumber.class);
        q.setParameter(1, greaterthan);
        q.setParameter(2, limit);
        log.debug("Getting ported numbers starting from [{}]", greaterthan);
        List<PortedNumber> res = q.getResultList();
        log.debug("Got ported numbers starting from [{}]", greaterthan);
        return res;
    }

}
