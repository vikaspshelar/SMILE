/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.rating;

import com.smilecoms.bm.db.model.RatePlan;
import com.smilecoms.bm.db.model.RatePlanAvp;
import com.smilecoms.bm.db.model.ServiceInstance;
import com.smilecoms.commons.util.Stopwatch;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManagerFactory;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class DummyRatingEngine implements IRatingEngine {

    private static final Logger log = LoggerFactory.getLogger(DummyRatingEngine.class);

    @Override
    public RatingResult rate(ServiceInstance serviceInstance, RatingKey ratingKey, RatePlan plan, List<RatePlanAvp> avps, Date eventDate) {
        if (log.isDebugEnabled()) {            
            log.debug("DummyRatingEngine getting voice rate for destination [{}]", ratingKey.getTo());
            Stopwatch.start();
        }
        RatingResult res = new RatingResult();
        res.setRetailRateCentsPerUnit(new BigDecimal(1));
        if (log.isDebugEnabled()) {
            Stopwatch.stop();
            log.debug("DummyRatingEngine got voice rate for destination [{}] of [{}] and took {}", new Object[]{ratingKey.getTo(), res.getRetailRateCentsPerUnit(),Stopwatch.millisString()});
        }
        return res;
    }

    @Override
    public void reloadConfig(EntityManagerFactory emf) {
        log.debug("DummyRatingEngine reloading configuration");
    }

    @Override
    public void onStart(EntityManagerFactory emf) {
        reloadConfig(emf);
    }

    @Override
    public void shutDown() {
    }

}
