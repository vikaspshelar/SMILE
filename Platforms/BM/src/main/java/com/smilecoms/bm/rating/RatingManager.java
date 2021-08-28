/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.rating;

import com.smilecoms.bm.db.model.RatePlan;
import com.smilecoms.bm.db.model.RatePlanAvp;
import com.smilecoms.bm.db.model.ServiceInstance;
import com.smilecoms.bm.db.op.DAO;
import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.slf4j.*;

/**
 *
 * @author paul
 */
@Singleton
@Startup
public class RatingManager implements BaseListener {

    private static final Logger log = LoggerFactory.getLogger(RatingManager.class);
    private static EntityManagerFactory emfRates;
    private static ClassLoader engineClassLoader;
    // Caches
    private static final Map<Integer, RatePlan> ratePlanCache = new ConcurrentHashMap<>();
    private static final Map<String, Integer> specToRatePlanCache = new ConcurrentHashMap<>();
    private static final Map<Integer, List<RatePlanAvp>> ratePlanAVPCache = new ConcurrentHashMap<>();
    private static final Map<String, IRatingEngine> engineToEngineInstanceMappings = new ConcurrentHashMap<>();

    public static RatingResult getRate(EntityManager em, RatingKey ratingKey, ServiceInstance serviceInstance, Date eventDate) {

        if (ratingKey.getServiceCode().equals("txtype.sale.purchase")) {
            log.debug("This is a txtype.sale.purchase request. Shortcut rating process");
            RatingResult rr = new RatingResult();
            rr.setDescription("");
            rr.setEventBased(true);
            rr.setRetailRateCentsPerUnit(BigDecimal.ONE);
            rr.setSessionBased(true);
            return rr;
        }
        
        if (ratingKey.getServiceCode().equals("txtype.socialmedia.tax") || ratingKey.getServiceCode().equals("txtype.socialmedia.tax.unlimited")) {
            log.debug("This is a txtype.socialmedia.tax request. Shortcut rating process");
            RatingResult rr = new RatingResult();
            rr.setDescription("");
            rr.setEventBased(true);
            rr.setRetailRateCentsPerUnit(BigDecimal.ONE);
            rr.setSessionBased(true);
            return rr;
        }

        log.debug("Getting rating engine for Prod Spec Id [{}] Service Spec Id [{}]", serviceInstance.getProductSpecificationId(), serviceInstance.getServiceSpecificationId());

        String key = serviceInstance.getProductSpecificationId() + "_" + serviceInstance.getServiceSpecificationId();
        Integer planId = specToRatePlanCache.get(key);
        if (planId == null) {
            planId = DAO.getProdSvcSpecRatePlanId(em, serviceInstance.getProductSpecificationId(), serviceInstance.getServiceSpecificationId());
            specToRatePlanCache.put(key, planId);
        }

        RatePlan plan = ratePlanCache.get(planId);
        if (plan == null) {
            plan = DAO.getRatePlan(em, planId);
            em.detach(plan);
            ratePlanCache.put(planId, plan);
        }

        if (log.isDebugEnabled()) {
            log.debug("Rate plan id is [{}] which stipulates rating engine class [{}]", planId, plan.getRatingEngineClass());
        }

        List<RatePlanAvp> avps = ratePlanAVPCache.get(planId);
        if (avps == null) {
            avps = DAO.getRatePlansAvps(em, planId);
            for (RatePlanAvp avp : avps) {
                em.detach(avp);
            }
            ratePlanAVPCache.put(planId, avps);
        }
        IRatingEngine engine = getRatingEngine(plan.getRatingEngineClass());
        RatingResult rr = engine.rate(serviceInstance, ratingKey, plan, avps, eventDate);
        rr.setEventBased(plan.getEventBased().equalsIgnoreCase("Y"));
        rr.setSessionBased(plan.getSessionBased().equalsIgnoreCase("Y"));
        return rr;
    }

    public static IRatingEngine getRatingEngine(String ratingEngineType) {
        IRatingEngine engine = engineToEngineInstanceMappings.get(ratingEngineType);
        if (engine == null) {
            try {
                log.debug("Loading rating engine of type [{}]", ratingEngineType);
                Class ratingEngineClass = engineClassLoader.loadClass(ratingEngineType);
                engine = (IRatingEngine) ratingEngineClass.newInstance();
                engine.onStart(emfRates);
                engineToEngineInstanceMappings.put(ratingEngineType, engine);
                log.debug("Added a rating engine of type [{}] into engineToEngineInstanceMappings", ratingEngineType);
            } catch (Exception e) {
                log.warn("Error: ", e);
                log.warn("Could not initialise rating engine of type [{}] due to [{}]", ratingEngineType, e);
            }
        }
        return engine;
    }

    private void reloadEngineConfigs() {
        for (IRatingEngine engine : engineToEngineInstanceMappings.values()) {
            try {
                engine.reloadConfig(emfRates);
            } catch (Throwable e) {
                log.warn("A rating engine threw an error when reloading its config");
                log.warn("Error: ", e);
            }
        }
    }

    @PostConstruct
    public void startUp() {
        emfRates = JPAUtils.getEMF("BMPU_RL");
        BaseUtils.registerForPropsAvailability(this);
        engineClassLoader = this.getClass().getClassLoader();
    }

    @PreDestroy
    public void cleanup() {
        log.debug("Stopping RatingManager");
        for (IRatingEngine engine : engineToEngineInstanceMappings.values()) {
            try {
                engine.shutDown();
            } catch (Throwable e) {
                log.warn("A rating engine threw an error when shutting down");
                log.warn("Error: ", e);
            }
        }
        BaseUtils.deregisterForPropsChanges(this);
        JPAUtils.closeEMF(emfRates);
    }

    @Override
    public void propsAreReadyTrigger() {
        init();
        BaseUtils.deregisterForPropsAvailability(this);
        BaseUtils.registerForPropsChanges(this);
    }

    @Override
    public void propsHaveChangedTrigger() {
        init();
    }

    public void init() {
        try {
            log.debug("RatingManager is calling engines to ask them to reload their config");
            reloadEngineConfigs();
            log.debug("RatingManager is cleaning rate plan and rate plan avp caches");
            specToRatePlanCache.clear();
            ratePlanCache.clear();
            ratePlanAVPCache.clear();
        } catch (Exception e) {
            log.warn("Error initialising rating manager: ", e);
        }
    }
}
