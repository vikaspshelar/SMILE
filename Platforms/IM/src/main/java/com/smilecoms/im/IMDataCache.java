/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im;

import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.im.db.model.Ifc;
import com.smilecoms.im.db.model.SpIfc;
import com.smilecoms.im.db.op.HSSDAO;
import com.smilecoms.xml.schema.im.IMSChargingInformation;
import com.smilecoms.xml.schema.im.IMSServiceProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Local;
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
@Local({BaseListener.class})
public class IMDataCache implements BaseListener {

    private static final Logger log = LoggerFactory.getLogger(IMDataCache.class);
    private EntityManagerFactory emf = null;
    public static final ConcurrentHashMap<Integer, IMSChargingInformation> chargingInfoCache = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<Integer, SCSCFSet> preferredSCSCFsCache = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<Integer, IMSServiceProfile> serviceProfileCache = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<Integer, String> visitedNetworkIdentityCache = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<Integer, List<Ifc>> IfcCache = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, SpIfc> SpIfcCache = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<Integer, List<Integer>> sharedIfcSetIdsCache = new ConcurrentHashMap<>();
    private static ScheduledFuture runner1 = null;

    @PostConstruct
    public void startUp() {
        BaseUtils.registerForPropsAvailability(this);
        emf = JPAUtils.getEMF("IMPU_RL");
        trigger();
    }

    @PreDestroy
    public void shutDown() {
        BaseUtils.deregisterForPropsAvailability(this);
        BaseUtils.deregisterForPropsChanges(this);
        Async.cancel(runner1);
        JPAUtils.closeEMF(emf);
    }

    @Override
    public void propsAreReadyTrigger() {
        runner1 = Async.scheduleAtFixedRate(new SmileBaseRunnable("IMDataCache") {
            @Override
            public void run() {
                trigger();
            }
        }, 0, 30000);
        BaseUtils.registerForPropsChanges(this);
    }

    @Override
    public void propsHaveChangedTrigger() {
        log.debug("Clearing out IM caches");
        chargingInfoCache.clear();
        serviceProfileCache.clear();
        visitedNetworkIdentityCache.clear();
        IfcCache.clear();
        SpIfcCache.clear();
        sharedIfcSetIdsCache.clear();
    }

    private void trigger() {
        try {
            log.debug("In IMDataCache trigger");
            EntityManager em = null;
            int currentSetID = -1;
            int currentSetIDPercentage = 0;
            int currentSetLoad = 0;
            int setAdded = 1;
            List<SCSCF> list = null;
            SCSCFSet scscfSet;
            int scscfSetID;
            String scscfName;
            int scscfEnabled;
            long currentScscfLoad;

            List<Object[]> scscfs;
            try {
                em = JPAUtils.getEM(emf);
                JPAUtils.beginTransaction(em);
                scscfs = HSSDAO.getAllSCSCFsWithLoadData(em);
            } finally {
                JPAUtils.commitTransactionAndClose(em);
            }

            for (Object[] scscfData : scscfs) {
                try {
                    setAdded = 0;
                    scscfSetID = (Integer) scscfData[0];
                    scscfName = (String) scscfData[1];
                    scscfEnabled = (Integer) scscfData[2];      //is actually used as a probability (percentage load the node can take)
                    //don't really need this priority column anymore - leave to remind to remove from sql schema
//                    priority = (Integer) scscfData[3];
                    currentScscfLoad = (Long) scscfData[4];
                    log.debug("Processing S-CSCF [{}]", scscfName);
                    if (currentSetID == -1) {
                        currentSetID = scscfSetID;
                        list = new ArrayList<>();
                        currentSetIDPercentage += scscfEnabled;
                        list.add(new SCSCF(scscfName, scscfEnabled, (int) currentScscfLoad, currentSetIDPercentage));
                        currentSetLoad += currentScscfLoad;
                    } else if (scscfSetID != currentSetID) {
                        log.debug("new set found - old set [{}], new set [{}]", new Object[]{scscfSetID, currentSetID});
                        log.debug("setid changed and percentages add up to [{}]", currentSetIDPercentage);
                        if (currentSetIDPercentage != 100) {
                            log.info("load percentages for S-CSCFs in Set: [{}] do not add up to 100 - [{}]", new Object[]{currentSetID, currentSetIDPercentage});
                        }
                        scscfSet = new SCSCFSet(currentSetID);
                        scscfSet.setTotalLoad(currentSetLoad);
                        scscfSet.setScscfList(list);
                        scscfSet.calculateSetStats();
                        preferredSCSCFsCache.put(currentSetID, scscfSet);
                        list = new ArrayList<>();
                        list.add(new SCSCF(scscfName, scscfEnabled, (int) currentScscfLoad, currentSetIDPercentage));
                        currentSetIDPercentage = scscfEnabled;
                        currentSetLoad = (int) currentScscfLoad;
                        currentSetID = scscfSetID;
                    } else {
                        log.debug("same set processing SCSCF - Set: [{}]", currentSetID);
                        currentSetIDPercentage += scscfEnabled;
                        currentSetLoad += currentScscfLoad;
                        list.add(new SCSCF(scscfName, scscfEnabled, (int) currentScscfLoad, currentSetIDPercentage));
                    }
                } catch (Exception e) {
                    log.warn("Error building cache of S-CSCFs [{}]", e.getMessage());
                    log.warn("Error: ", e);
                }
            }
            if (setAdded == 0) {
                scscfSet = new SCSCFSet(currentSetID);
                scscfSet.setTotalLoad(currentSetLoad);
                scscfSet.setScscfList(list);
                scscfSet.calculateSetStats();
                preferredSCSCFsCache.put(currentSetID, scscfSet);
            }

            if (log.isDebugEnabled()) {
                printScsfStats();
            }
        } catch (Exception e) {
            log.warn("Error: ", e);
        }
    }

    static List<SCSCF> getSCSCFListOrderedByLoadAsc(int id) {
        return preferredSCSCFsCache.get(id).getSCSCFListOrderedByLoadAsc();
    }

    static List<SCSCF> getSCSCFList(int id) {
        return preferredSCSCFsCache.get(id).getScscfList();
    }

    private void printScsfStats() {
        for (SCSCFSet scscfSet : IMDataCache.preferredSCSCFsCache.values()) {
            log.debug("#########################");
            log.debug("SCSCF SET [{}] - AllCSCFsUp: [{}], Balanced: [{}]", new Object[]{scscfSet.getID(), scscfSet.isAllCSCFsUp(), scscfSet.isBalanced()});
            log.debug("\tTotal active subscriptions for Set: [{}]", scscfSet.getTotalLoad());

            for (SCSCF s : scscfSet.getScscfList()) {
                log.debug("\tSCSCF: [{}] - Available: [{}]", new Object[]{s.getScscfName(), s.isAvailable()});
                log.debug("\t\tMaxLoadPercentage: [{}]", s.getMaxLoadPercentage());
                log.debug("\t\tWeight: [{}]", s.getProbability());
                log.debug("\t\tCurrentLoad: [{}]", s.getCurrentLoad());
                log.debug("\t\tCurrentLoadPercentage: [{}]", s.getCurrentLoadPercentage());
                log.debug("\t\tDeltaRequired: [{}]", s.getDeltarequired());
            }
            log.debug("#########################");
        }
    }

}
