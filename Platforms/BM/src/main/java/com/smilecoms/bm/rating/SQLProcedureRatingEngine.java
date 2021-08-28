/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.rating;

import com.smilecoms.bm.db.model.RatePlan;
import com.smilecoms.bm.db.model.RatePlanAvp;
import com.smilecoms.bm.db.model.ServiceInstance;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Stopwatch;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
//import org.eclipse.persistence.jpa.JpaEntityManager;
//import org.eclipse.persistence.queries.DataReadQuery;
//import org.eclipse.persistence.queries.StoredProcedureCall;
//import org.eclipse.persistence.sessions.DatabaseRecord;
//import org.eclipse.persistence.sessions.Session;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class SQLProcedureRatingEngine implements IRatingEngine {

    private static final Logger log = LoggerFactory.getLogger(SQLProcedureRatingEngine.class);
    private EntityManagerFactory emf = null;

    @Override
    public RatingResult rate(ServiceInstance serviceInstance, RatingKey ratingKey, RatePlan plan, List<RatePlanAvp> avps, Date eventDate) {
        if (log.isDebugEnabled()) {
            log.debug("SQLProcedureRatingEngine getting rate for destination [{}]", ratingKey.getTo());
            Stopwatch.start();
        }

        EntityManager em = null;
        RatingResult res = null;
        try {

            // Get the stored procedure name
            String procName = null;
            for (RatePlanAvp avp : avps) {
                if (avp.getRatePlanAvpPK().getAttribute().equalsIgnoreCase("StoredProcedureName")) {
                    procName = avp.getValue();
                    log.debug("Procedure name is [{}]", procName);
                    break;
                }
            }
            if (procName == null) {
                throw new RuntimeException("StoredProcedureName AVP Missing for Rate Plan Configuration");
            }

            res = callProcedure(
                    procName,
                    ratingKey.getFrom(),
                    ratingKey.getTo(),
                    ratingKey.getServiceCode(),
                    plan.getRatePlanId());

        } finally {
            try {
                JPAUtils.closeEM(em);
            } catch (Exception ex) {
                log.warn("Failed to close EM: " + ex.toString());
            }
        }

        if (log.isDebugEnabled()) {
            Stopwatch.stop();
            log.debug("SQLProcedureRatingEngine got rate for destination [{}] of [{}] and took {}", new Object[]{ratingKey.getTo(), res.getRetailRateCentsPerUnit(), Stopwatch.millisString()});
        }
        return res;
    }

    @Override
    public void reloadConfig(EntityManagerFactory emf) {
        log.debug("SQLProcedureRatingEngine reloading configuration");
        this.emf = emf;
    }

    @Override
    public void onStart(EntityManagerFactory emf) {
        reloadConfig(emf);
    }

    private RatingResult callProcedure(String procName, String from, String to, String serviceCode, Integer ratePlanId) {

        RatingResult rr = null;
        EntityManager em = null;
        try {
            em = JPAUtils.getEM(emf);

//            StoredProcedureCall spcall = new StoredProcedureCall();
//            spcall.setProcedureName(procName);
//            spcall.addNamedArgument("FROM_USER");
//            spcall.addNamedArgument("TO_USER");
//            spcall.addNamedArgument("SERVICE_CODE");
//            spcall.addNamedArgument("RATE_PLAN_ID");
//            spcall.addNamedOutputArgument("RATE_PER_UNIT", "RATE_PER_UNIT", BigDecimal.class);
//            spcall.addNamedOutputArgument("UNIT_TYPE", "UNIT_TYPE", String.class);
//
//            DataReadQuery query = new DataReadQuery();
//            query.setCall(spcall);
//            query.addArgument("FROM_USER");
//            query.addArgument("TO_USER");
//            query.addArgument("SERVICE_CODE");
//            query.addArgument("RATE_PLAN_ID");
//
//            List args = new ArrayList();
//            args.add(from);
//            args.add(to);
//            args.add(serviceCode);
//            args.add(ratePlanId);
//
//            Session session = ((JpaEntityManager) em.getDelegate()).getActiveSession();
//
//            List results = (List) session.executeQuery(query, args);
//            DatabaseRecord record = (DatabaseRecord) results.get(0);
//            BigDecimal RateCentsPerUnit = (BigDecimal) record.get("RATE_PER_UNIT");
//            String unitType = (String)record.get("UNIT_TYPE");
//
//            rr = new RatingResult();
//            rr.setRetailRateCentsPerUnit(RateCentsPerUnit);
        } finally {
            try {
                JPAUtils.closeEM(em);
            } catch (Exception ex) {
                log.warn("Failed to close em: " + ex.toString());
            }
        }
        return rr;
    }

    @Override
    public void shutDown() {
    }
}
