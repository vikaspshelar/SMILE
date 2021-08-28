/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.cm;

import com.smilecoms.cm.db.model.Promotion;
import com.smilecoms.cm.db.op.DAO;
import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.cm.AVP;
import com.smilecoms.xml.schema.cm.ProductSpecification;
import com.smilecoms.xml.schema.cm.UnitCreditSpecification;
import java.util.Collection;
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
public class CMDataCache implements BaseListener {

    private static final Logger log = LoggerFactory.getLogger(CMDataCache.class);
    public static final Map<Integer, List<AVP>> serviceSpecAVPCache = new ConcurrentHashMap<>();
    public static final Map<String, UnitCreditSpecification> unitCreditSpecificationCache = new ConcurrentHashMap<>();
    public static final Map<String, ProductSpecification> productSpecificationCache = new ConcurrentHashMap<>();
    private EntityManagerFactory emf = null;

    @PostConstruct
    public void startUp() {
        emf = JPAUtils.getEMF("CMPU_RL");
        BaseUtils.registerForPropsChanges(this);
    }

    @PreDestroy
    public void shutDown() {
        BaseUtils.deregisterForPropsChanges(this);
        JPAUtils.closeEMF(emf);
    }

    @Override
    public void propsAreReadyTrigger() {
    }

    @Override
    public void propsHaveChangedTrigger() {

        EntityManager em = null;
        try {
            log.debug("Syncing unit credit pricing with X3 offline cache");
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            DAO.syncUnitCreditPrices(em);
            JPAUtils.commitTransaction(em);
            JPAUtils.beginTransaction(em);
            if(BaseUtils.getBooleanProperty("env.pos.ifrs15.enabled",  false)) {
                updateBundlePricesUsingEquivalentParentBundles(em);
            }
            
        } catch (Exception e) {
            log.warn("Error updating UC pricing: {}", e.toString());
        } finally {
            JPAUtils.commitTransactionAndClose(em);
        }
        log.debug("Clearing out CM caches");
        serviceSpecAVPCache.clear();
        unitCreditSpecificationCache.clear();
        productSpecificationCache.clear();
    }
    
    public void updateBundlePricesUsingEquivalentParentBundles(EntityManager em) {
        
        Collection <com.smilecoms.cm.db.model.UnitCreditSpecification> unitCreditSpecsWithEquivalentParents = DAO.getAllAvailableUnitCreditSpecificationsWithEquivalentParents(em);
        
        for(com.smilecoms.cm.db.model.UnitCreditSpecification ucSpecWithEqParent : unitCreditSpecsWithEquivalentParents) {
            
            // Get the parent unit credit spec id ...
            String strEquivalentParentSpecId = Utils.getValueFromCRDelimitedAVPString(ucSpecWithEqParent.getConfiguration(), "EquivalentParentSpecId");
            if(strEquivalentParentSpecId !=  null) { // Bundle must get its price from the parent.
                int iEquivalentParentSpecId = Integer.parseInt(strEquivalentParentSpecId);
                
                com.smilecoms.cm.db.model.UnitCreditSpecification equivalentParent = null;
                
                try {
                    equivalentParent = DAO.getUnitCreditSpecificationById(em, iEquivalentParentSpecId);
                } catch (Exception ex) {
                    log.error("Bad configuration  for Unit credit specification [{}], equivalent parent unit credit spec [{}] does not exist.",  ucSpecWithEqParent.getUnitCreditSpecificationId(),
                            strEquivalentParentSpecId);
                    continue;
                }
                
                if(equivalentParent != null && (ucSpecWithEqParent.getValidityDays() == equivalentParent.getValidityDays()) && 
                        ucSpecWithEqParent.getFilterClass().equals(equivalentParent.getFilterClass()) && 
                            ucSpecWithEqParent.getWrapperClass().equals(equivalentParent.getWrapperClass()) && 
                                ucSpecWithEqParent.getUnits() == equivalentParent.getUnits() &&
                                    ucSpecWithEqParent.getUsableDays() == equivalentParent.getUsableDays()
                        ) {
                    log.debug("Setting the price of unit credit specification [{}], to the price of its equivalent parent spec id [{}] and price is [{}].",
                                new Object [] {ucSpecWithEqParent.getUnitCreditSpecificationId(), equivalentParent.getUnitCreditSpecificationId(),
                                    equivalentParent.getPriceCents()});
                    ucSpecWithEqParent.setPriceCents(equivalentParent.getPriceCents());
                    em.persist(ucSpecWithEqParent);
                    em.flush();
                } else { // Log error ...
                   log.error("Unit credit specification [{}] has been configured to use equivalent parent spec [{}] but they are not equivalent; check the following: [" +
                           "ValidityDays: {} vs {}; " +
                           "FilterClasses: {} vs {}; " +
                           "WrapperClasses: {} vs {}; " + 
                           "Units: {} vs {}; "  +
                           "UsableDays: {} vs {}].", 
                           new Object [] {ucSpecWithEqParent.getUnitCreditSpecificationId(), equivalentParent.getUnitCreditSpecificationId(),
                               ucSpecWithEqParent.getValidityDays(), equivalentParent.getValidityDays(),
                               ucSpecWithEqParent.getFilterClass(), equivalentParent.getFilterClass(),
                               ucSpecWithEqParent.getWrapperClass(), equivalentParent.getWrapperClass(),
                               ucSpecWithEqParent.getUnits(), equivalentParent.getUnits(),
                               ucSpecWithEqParent.getUsableDays(), equivalentParent.getUsableDays()});
                }
            }       
        }
        
        
    }
}
