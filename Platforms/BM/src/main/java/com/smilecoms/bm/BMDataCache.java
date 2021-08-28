/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm;

import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.base.BaseUtils;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import org.slf4j.*;

/**
 *
 * @author paul
 */
@Singleton
@Startup
public class BMDataCache implements BaseListener {
    
    private static final Logger log = LoggerFactory.getLogger(BMDataCache.class);

    public static final Map<String, String> serviceCodeServiceSpecIdCache = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<Integer, com.smilecoms.bm.db.model.UnitCreditSpecification> unitCreditSpecificationCacheById = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, com.smilecoms.bm.db.model.UnitCreditSpecification> unitCreditSpecificationCacheByName = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, com.smilecoms.bm.db.model.UnitCreditSpecification> unitCreditSpecificationCacheByItemNumber = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<Integer, Map<String,String>> unitCreditConfigCacheBySpecId = new ConcurrentHashMap<>();

    @PostConstruct
    public void startUp() {
        BaseUtils.registerForPropsChanges(this);
    }

    @PreDestroy
    public void shutDown() {
        BaseUtils.deregisterForPropsChanges(this);
    }

    @Override
    public void propsAreReadyTrigger() {
    }

    @Override
    public void propsHaveChangedTrigger() {
        log.warn("Clearing out BM caches");
        serviceCodeServiceSpecIdCache.clear();
        unitCreditSpecificationCacheById.clear();
        unitCreditSpecificationCacheByName.clear();
        unitCreditSpecificationCacheByItemNumber.clear();
        unitCreditConfigCacheBySpecId.clear();
    }
}
