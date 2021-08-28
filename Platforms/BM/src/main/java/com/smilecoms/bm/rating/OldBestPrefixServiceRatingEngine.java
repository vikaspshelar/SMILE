///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//package com.smilecoms.bm.rating;
//
//import com.smilecoms.bm.db.model.PortedNumber;
//import com.smilecoms.bm.db.model.ServiceRate;
//import com.smilecoms.bm.db.model.RatePlan;
//import com.smilecoms.bm.db.model.RatePlanAvp;
//import com.smilecoms.bm.db.op.DAO;
//
//import com.smilecoms.commons.base.BaseUtils;
//import com.smilecoms.commons.util.JPAUtils;
//import com.smilecoms.commons.util.Stopwatch;
//import com.smilecoms.commons.util.Utils;
//import com.smilecoms.xml.schema.bm.RatingKey;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.locks.Lock;
//import java.util.concurrent.locks.ReentrantLock;
//import javax.persistence.EntityManager;
//import javax.persistence.EntityManagerFactory;
//import org.slf4j.*;
//
///**
// *
// * @author paul
// */
//public class OldBestPrefixServiceRatingEngine implements IRatingEngine {
//
//    private final Map<String, PrefixMapNode[]> perTrunkSetAndServicePrefixMaps = new ConcurrentHashMap();
//    private static final Logger log = LoggerFactory.getLogger(OldBestPrefixServiceRatingEngine.class);
//    private EntityManagerFactory emf = null;
//    private Map<Long, Integer> portedNumberMap = null;
//
//    @Override
//    public RatingResult rate(int serviceInstanceID, RatingKey ratingKey, RatePlan plan, List<RatePlanAvp> avps, Date eventDate) {
//        if (log.isDebugEnabled()) {
//            log.debug("ServiceRatingEngine getting service rate for incoming trunk [{}], outgoing trunk [{}], service code [{}], source [{}], destination [{}], leg [{}]", new Object[]{ratingKey.getIncomingTrunk(), ratingKey.getOutgoingTrunk(), ratingKey.getServiceCode(), ratingKey.getFrom(), ratingKey.getTo(), ratingKey.getLeg()});
//            Stopwatch.start();
//        }
//
//        long fromNumeric = Utils.getFirstNumericPartOfString(ratingKey.getFrom());
//        long toNumeric = Utils.getFirstNumericPartOfString(ratingKey.getTo());
//
//        RatingResult res = new RatingResult();
//        if (ratingKey.getIncomingTrunk() == null || ratingKey.getIncomingTrunk().isEmpty()
//                || ratingKey.getOutgoingTrunk() == null || ratingKey.getOutgoingTrunk().isEmpty()) {
//            throw new RuntimeException("Missing trunk -- Incoming Trunk is " + ratingKey.getIncomingTrunk() + " and Outgoing Trunk is " + ratingKey.getOutgoingTrunk());
//        }
//        ServiceRate rateToUse = null;
//
//        PrefixMapNode[] rootPrefixMaps = getCachedRootPrefixMapsForTrunkSetLegAndService(
//                ratingKey.getIncomingTrunk(),
//                ratingKey.getOutgoingTrunk(),
//                ratingKey.getServiceCode(),
//                ratingKey.getLeg());
//
//        List<ServiceRate> ratesMatchedOnSource;
//        List<ServiceRate> ratesMatchedOnDestination;
//
//        Integer fromICPartner = getPortedNumberMap().get(fromNumeric);
//        Integer toICPartner = getPortedNumberMap().get(toNumeric);
//
//        if (fromICPartner != null) {
//            log.debug("The source number has ported to operator [{}]", fromICPartner);
//            ratesMatchedOnSource = getRatesForTrunkSetLegAndFromICPartner(
//                    eventDate,
//                    ratingKey.getIncomingTrunk(),
//                    ratingKey.getOutgoingTrunk(),
//                    ratingKey.getServiceCode(),
//                    ratingKey.getLeg(),
//                    fromICPartner);
//        } else {
////            ratesMatchedOnSource = rootPrefixMaps[0].getBestMatch(String.valueOf(fromNumeric)).getAllLeafData();
//        }
//
//        if (toICPartner != null) {
//            log.debug("The destination number has ported to operator [{}]", toICPartner);
//            ratesMatchedOnDestination = getRatesForTrunkSetLegAndToICPartner(
//                    eventDate,
//                    ratingKey.getIncomingTrunk(),
//                    ratingKey.getOutgoingTrunk(),
//                    ratingKey.getServiceCode(),
//                    ratingKey.getLeg(),
//                    toICPartner);
//        } else {
// //           ratesMatchedOnDestination = rootPrefixMaps[1].getBestMatch(String.valueOf(toNumeric)).getAllLeafData();
//        }
//
//        String rpId = String.valueOf(plan.getRatePlanId());
//        log.debug("Rate plan Id is [{}]", rpId);
//        log.debug("We have [{}] rates based on source and [{}] on destination", ratesMatchedOnSource.size(), ratesMatchedOnDestination.size());
//        // Now we have all the possible rates, we must find the one rate that intersects both
//        for (ServiceRate rateOnDest : ratesMatchedOnDestination) {
//            if (!Utils.isBetween(eventDate, rateOnDest.getDateFrom(), rateOnDest.getDateTo())) {
//                continue;
//            }
//            if (!rateOnDest.getRatePlanMatch().isEmpty() && !Utils.matchesWithPatternCache(rpId, rateOnDest.getRatePlanMatch())) {
//                continue;
//            }
//            for (ServiceRate rateOnSource : ratesMatchedOnSource) {
//                if (!Utils.isBetween(eventDate, rateOnSource.getDateFrom(), rateOnSource.getDateTo())) {
//                    continue;
//                }
//                if (!rateOnSource.getRatePlanMatch().isEmpty() && !Utils.matchesWithPatternCache(rpId, rateOnSource.getRatePlanMatch())) {
//                    continue;
//                }
//                if (rateOnDest.getServiceRateId().equals(rateOnSource.getServiceRateId())) {
//                    log.debug("Found intersecting rate id [{}]", rateOnDest.getServiceRateId());
//                    if (rateToUse != null
//                            && (!rateToUse.getFromInterconnectCentsPerUnit().equals(rateOnDest.getFromInterconnectCentsPerUnit())
//                            || !rateToUse.getToInterconnectCentsPerUnit().equals(rateOnDest.getToInterconnectCentsPerUnit())
//                            || !rateToUse.getRetailCentsPerUnit().equals(rateOnDest.getRetailCentsPerUnit()))) {
//                        String err = "Multiple rates exist for [" + ratingKey.getFrom() + "] to [" + ratingKey.getTo()
//                                + "] service [" + ratingKey.getServiceCode()
//                                + "] incoming trunk [" + ratingKey.getIncomingTrunk()
//                                + "] outgoing trunk [" + ratingKey.getOutgoingTrunk()
//                                + "] leg [" + ratingKey.getLeg()
//                                + "] TX date [" + eventDate
//                                + "] RatesMatchesOnSource [" + ratesMatchedOnSource.size()
//                                + "] RatesMatchesOnDestination [" + ratesMatchedOnDestination.size() + "]";
//                        BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "BM", err);
//                        throw new RuntimeException("Multiple rates found -- " + err);
//                    }
//                    if (rateToUse == null) {
//                        rateToUse = rateOnDest;
//                    }
//                }
//            }
//        }
//        if (rateToUse == null) {
//            String err = "Cannot find a rate for [" + ratingKey.getFrom() + "] to [" + ratingKey.getTo()
//                    + "] service [" + ratingKey.getServiceCode()
//                    + "] incoming trunk [" + ratingKey.getIncomingTrunk()
//                    + "] outgoing trunk [" + ratingKey.getOutgoingTrunk()
//                    + "] leg [" + ratingKey.getLeg()
//                    + "] TX date [" + eventDate
//                    + "] RatesMatchesOnSource [" + ratesMatchedOnSource.size()
//                    + "] RatesMatchesOnDestination [" + ratesMatchedOnDestination.size() + "]";
//            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "BM", err);
//            throw new RuntimeException("Cannot find a rate -- " + err);
//        }
//        res.setFromInterconnectRateCentsPerUnit(rateToUse.getFromInterconnectCentsPerUnit());
//        res.setToInterconnectRateCentsPerUnit(rateToUse.getToInterconnectCentsPerUnit());
//        res.setDescription(rateToUse.getDescription());
//        res.setRateId(rateToUse.getServiceRateId());
//        res.setFromInterconnectCurrency(rateToUse.getFromInterconnectRateCurrency());
//        res.setToInterconnectCurrency(rateToUse.getToInterconnectRateCurrency());
//        res.setRatingHint(rateToUse.getRatingHint());
//        log.debug("Rating hint is [{}]", res.getRatingHint());
//
//        String overriddenRatingGroup = res.getPropertyFromHint("OverriddenRatingGroup");
//        if (overriddenRatingGroup != null) {
//            log.debug("This requests rating data has overridden the rating group from [{}] to [{}]", ratingKey.getRatingGroup(), overriddenRatingGroup);
//            ratingKey.setRatingGroup(overriddenRatingGroup);
//        } else {
//            throw new RuntimeException("Invalid rate configuration. Service rate must have an OverriddenRatingGroup -- Service Rate Id is " + rateToUse.getServiceRateId());
//        }
//
//        res.setRetailRateCentsPerUnit(rateToUse.getRetailCentsPerUnit());
//
//        if (log.isDebugEnabled()) {
//            Stopwatch.stop();
//            log.debug("ServiceRatingEngine got retail rate for destination [{}] of [{}]c/unit and took {}", new Object[]{ratingKey.getTo(), res.getRetailRateCentsPerUnit(), Stopwatch.millisString()});
//        }
//        return res;
//    }
//
//    private PrefixMapNode[] getCachedRootPrefixMapsForTrunkSetLegAndService(String incomingTrunk, String outgoingTrunk, String serviceCode, String leg) {
//        String key = incomingTrunk + "|" + outgoingTrunk + "|" + serviceCode + "|" + leg;
//        PrefixMapNode[] maps = perTrunkSetAndServicePrefixMaps.get(key);
//        if (maps == null) {
//            maps = getPrefixMapsForTrunkSetLegAndServiceFromDB(incomingTrunk, outgoingTrunk, serviceCode, leg);
//            perTrunkSetAndServicePrefixMaps.put(key, maps);
//        }
//        return maps;
//    }
//
//    private PrefixMapNode[] getPrefixMapsForTrunkSetLegAndServiceFromDB(String incomingTrunk, String outgoingTrunk, String serviceCode, String leg) {
//        EntityManager em = null;
//        PrefixMapNode sourcePrefixMap = new PrefixMapNode();
//        PrefixMapNode destinationPrefixMap = new PrefixMapNode();
//        try {
//            // Use em to get config from table and put into prefix tree
//            em = JPAUtils.getEM(emf);
//            List<ServiceRate> dbRates = DAO.getRatesForTrunkSetLegAndService(em, incomingTrunk, outgoingTrunk, serviceCode, leg);
//            for (ServiceRate aRate : dbRates) {
//                sourcePrefixMap.populate(String.valueOf(aRate.getFromPrefix()), aRate);
//                destinationPrefixMap.populate(String.valueOf(aRate.getToPrefix()), aRate);
//            }
//        } finally {
//            try {
//                JPAUtils.closeEM(em);
//            } catch (Exception ex) {
//                log.warn("Failed to close EM: " + ex.toString());
//            }
//        }
//        return new PrefixMapNode[]{sourcePrefixMap, destinationPrefixMap};
//    }
//
//    @Override
//    public void reloadConfig(EntityManagerFactory emf) {
//        this.emf = emf;
//        reloadConfig();
//    }
//
//    private void reloadConfig() {
//        log.debug("BestPrefixServiceRatingEngine reloading config");
//        for (String key : perTrunkSetAndServicePrefixMaps.keySet()) {
//            String[] bits = key.split("\\|", 4);
//            perTrunkSetAndServicePrefixMaps.put(key, getPrefixMapsForTrunkSetLegAndServiceFromDB(bits[0], bits[1], bits[2], bits[3]));
//        }
//        refreshPortedNumberMap();
//        log.debug("BestPrefixServiceRatingEngine done reloading config");
//    }
//
//    @Override
//    public void onStart(EntityManagerFactory emf) {
//        this.emf = emf;
//        reloadConfig();
//        BaseUtils.registerForScheduling(this, 120000, Utils.getRandomNumber(50 * 1000, 70 * 1000), "");
//    }
//
//    private Map<Long, Integer> getPortedNumberMap() {
//        return portedNumberMap;
//    }
//
//    private void refreshPortedNumberMap() {
//        EntityManager em = null;
//        try {
//            Map tmpPortedNumberMap = new ConcurrentHashMap();
//            // Use em to get config from table and put into prefix tree
//            em = JPAUtils.getEM(emf);
//            for (PortedNumber num : DAO.getPortedNumbers(em)) {
//                tmpPortedNumberMap.put(num.getNumber(), num.getInterconnectPartnerId());
//            }
//            portedNumberMap = tmpPortedNumberMap;
//        } finally {
//            try {
//                JPAUtils.closeEM(em);
//            } catch (Exception ex) {
//                log.warn("Failed to close EM: " + ex.toString());
//            }
//        }
//    }
//
//    private List<ServiceRate> getRatesForTrunkSetLegAndFromICPartner(Date eventDate, String incomingTrunk, String outgoingTrunk, String serviceCode, String leg, Integer fromICPartner) {
//        EntityManager em = null;
//        try {
//            // Use em to get config from table and put into prefix tree
//            em = JPAUtils.getEM(emf);
//            return DAO.getRatesForTrunkSetLegAndFromICPartner(
//                    em,
//                    eventDate,
//                    incomingTrunk,
//                    outgoingTrunk,
//                    serviceCode,
//                    leg,
//                    fromICPartner);
//        } finally {
//            try {
//                JPAUtils.closeEM(em);
//            } catch (Exception ex) {
//                log.warn("Failed to close EM: " + ex.toString());
//            }
//        }
//
//    }
//
//    private List<ServiceRate> getRatesForTrunkSetLegAndToICPartner(Date eventDate, String incomingTrunk, String outgoingTrunk, String serviceCode, String leg, Integer toICPartner) {
//        EntityManager em = null;
//        try {
//            // Use em to get config from table and put into prefix tree
//            em = JPAUtils.getEM(emf);
//            return DAO.getRatesForTrunkSetLegAndToICPartner(
//                    em,
//                    eventDate,
//                    incomingTrunk,
//                    outgoingTrunk,
//                    serviceCode,
//                    leg,
//                    toICPartner);
//        } finally {
//            try {
//                JPAUtils.closeEM(em);
//            } catch (Exception ex) {
//                log.warn("Failed to close EM: " + ex.toString());
//            }
//        }
//
//    }
//
//    private static int cacheHourOfDayRefreshed = -1;
//    private static final Lock lock = new ReentrantLock();
//
//    private void refreshCacheIfRequired() {
//        Calendar now = Calendar.getInstance();
//        int currentHourOfDay = now.get(Calendar.HOUR_OF_DAY);
//        if (cacheHourOfDayRefreshed != currentHourOfDay) {
//            log.debug("Refresh may be required");
//        } else {
//            return;
//        }
//        if (!lock.tryLock()) {
//            // Lock is held by another thread is doing a refresh
//            if (cacheHourOfDayRefreshed == -1) {
//                // Rating config has never been done. Must wait
//                lock.lock();
//            } else {
//                return;
//            }
//        }
//        try {
//            if (cacheHourOfDayRefreshed == currentHourOfDay) {
//                log.debug("Refresh is not actually required - must have been done by another thread");
//                return;
//            }
//            reloadConfig();
//            cacheHourOfDayRefreshed = currentHourOfDay;
//        } finally {
//            lock.unlock();
//        }
//    }
//
//    @Override
//    public void shutDown() {
//        log.debug("Removing myself from scheduling");
//        Async.cancel(runner1);
//    }
//
//    @Override
//    private void trigger() {
//        refreshCacheIfRequired();
//    }
//
//}
