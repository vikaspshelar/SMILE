/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.rating;

import com.smilecoms.bm.db.model.ServiceRate;
import com.smilecoms.bm.db.model.RatePlan;
import com.smilecoms.bm.db.model.RatePlanAvp;
import com.smilecoms.bm.db.model.ServiceInstance;
import com.smilecoms.bm.db.op.DAO;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Stopwatch;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class BestPrefixServiceRatingEngine implements IRatingEngine {

    private final Map<String, PrefixMapNode[]> perTrunkSetAndServicePrefixMaps = new ConcurrentHashMap();
    private static final Logger log = LoggerFactory.getLogger(BestPrefixServiceRatingEngine.class);
    private EntityManagerFactory emf = null;
    private static int maxShortCodeLength = 4;
    private static ScheduledFuture runner1 = null;

    @Override
    public RatingResult rate(ServiceInstance serviceInstance, RatingKey ratingKey, RatePlan plan, List<RatePlanAvp> avps, Date eventDate) {
        if (log.isDebugEnabled()) {
            log.debug("ServiceRatingEngine getting service rate for incoming trunk [{}], outgoing trunk [{}], service code [{}], source [{}], destination [{}], leg [{}]", new Object[]{ratingKey.getIncomingTrunk(), ratingKey.getOutgoingTrunk(), ratingKey.getServiceCode(), ratingKey.getFrom(), ratingKey.getTo(), ratingKey.getLeg()});
            Stopwatch.start();
        }

        long fromNumeric;
        long toNumeric;

        try {
            fromNumeric = Utils.getFirstNumericPartOfString(ratingKey.getFrom());
        } catch (NumberFormatException e) {
            fromNumeric = Long.parseLong(BaseUtils.getProperty("env.e164.country.code") + "000000000");
            log.debug("From number [{}] is not numeric. Defaulting to [{}]", ratingKey.getFrom(), fromNumeric);
        }
        try {
            toNumeric = Utils.getFirstNumericPartOfString(ratingKey.getTo());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid destination -- Source " + ratingKey.getFrom() + " Destination " + ratingKey.getTo());
        }

        boolean mustFindShortcodeRate = (String.valueOf(fromNumeric).length() <= maxShortCodeLength || String.valueOf(toNumeric).length() <= maxShortCodeLength);
        log.debug("Must find a short code rate? [{}]", mustFindShortcodeRate);

        RatingResult res = new RatingResult();
        if (ratingKey.getIncomingTrunk() == null || ratingKey.getIncomingTrunk().isEmpty()
                || ratingKey.getOutgoingTrunk() == null || ratingKey.getOutgoingTrunk().isEmpty()) {
            throw new RuntimeException("Missing trunk -- Incoming Trunk is " + ratingKey.getIncomingTrunk() + " and Outgoing Trunk is " + ratingKey.getOutgoingTrunk());
        }

        PrefixMapNode[] rootPrefixMaps = getCachedRootPrefixMapsForTrunkSetLegAndService(
                ratingKey.getIncomingTrunk(),
                ratingKey.getOutgoingTrunk(),
                ratingKey.getServiceCode(),
                ratingKey.getLeg(),
                mustFindShortcodeRate);

        Map<Integer, ServiceRate> ratesMatchedOnSource = rootPrefixMaps[0].getBestMatch(String.valueOf(fromNumeric)).getAllLeafData();
        Map<Integer, ServiceRate> ratesMatchedOnDestination = rootPrefixMaps[1].getBestMatch(String.valueOf(toNumeric)).getAllLeafData();

        String rpId = String.valueOf(plan.getRatePlanId());
        log.debug("Rate plan Id is [{}]", rpId);
        log.debug("We have [{}] rates based on source and [{}] on destination", ratesMatchedOnSource.size(), ratesMatchedOnDestination.size());

        // Get the intersection of the two maps
        List<ServiceRate> intersectingServiceRates = new ArrayList<>();

        if (ratesMatchedOnSource.size() < ratesMatchedOnDestination.size()) {
            // Loop through source map as its smaller
            for (Integer rateId : ratesMatchedOnSource.keySet()) {
                ServiceRate dstRate = ratesMatchedOnDestination.get(rateId);
                if (dstRate != null) {
                    intersectingServiceRates.add(dstRate);
                }
            }
        } else {
            for (Integer rateId : ratesMatchedOnDestination.keySet()) {
                ServiceRate srcRate = ratesMatchedOnSource.get(rateId);
                if (srcRate != null) {
                    intersectingServiceRates.add(srcRate);
                }
            }
        }

        ServiceRate rateToUse = null;

        // Now we have all the possible rates, we must narrow down by dates etc
        for (ServiceRate rate : intersectingServiceRates) {
            log.debug("Found intersecting rate id [{}]", rate.getServiceRateId());

            if (!Utils.isBetween(eventDate, rate.getDateFrom(), rate.getDateTo())) {
                log.debug("Rate is not in this date");
                continue;
            }
            if (!rate.getRatePlanMatch().isEmpty() && !Utils.matchesWithPatternCache(rpId, rate.getRatePlanMatch())) {
                log.debug("Rate is not for this rate plan");
                continue;
            }
            if (DAO.isNegative(rate.getRetailCentsPerUnit())) {
                log.debug("Rate is negative. Ignoring");
                continue;
            }

            if (rateToUse != null
                    && (!rateToUse.getFromInterconnectCentsPerUnit().equals(rate.getFromInterconnectCentsPerUnit())
                    || !rateToUse.getToInterconnectCentsPerUnit().equals(rate.getToInterconnectCentsPerUnit())
                    || !rateToUse.getRetailCentsPerUnit().equals(rate.getRetailCentsPerUnit()))) {
                String err = "Multiple rates exist for [" + ratingKey.getFrom() + "] to [" + ratingKey.getTo()
                        + "] service [" + ratingKey.getServiceCode()
                        + "] incoming trunk [" + ratingKey.getIncomingTrunk()
                        + "] outgoing trunk [" + ratingKey.getOutgoingTrunk()
                        + "] leg [" + ratingKey.getLeg()
                        + "] TX date [" + eventDate
                        + "] RatesMatchesOnSource [" + ratesMatchedOnSource.size()
                        + "] RatesMatchesOnDestination [" + ratesMatchedOnDestination.size() + "]";
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "BM", err);
                throw new RuntimeException("Multiple rates found -- " + err);
            }
            if (rateToUse == null) {
                rateToUse = rate;
            }
        }

        if (rateToUse == null) {
            String err = "Cannot find a rate for [" + ratingKey.getFrom() + "] to [" + ratingKey.getTo()
                    + "] service [" + ratingKey.getServiceCode()
                    + "] incoming trunk [" + ratingKey.getIncomingTrunk()
                    + "] outgoing trunk [" + ratingKey.getOutgoingTrunk()
                    + "] leg [" + ratingKey.getLeg()
                    + "] TX date [" + eventDate
                    + "] RatesMatchesOnSource [" + ratesMatchedOnSource.size()
                    + "] RatesMatchesOnDestination [" + ratesMatchedOnDestination.size() + "]";
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "BM", err);
            throw new RuntimeException("Cannot find a rate -- " + err);
        }
        res.setFromInterconnectRateCentsPerUnit(rateToUse.getFromInterconnectCentsPerUnit());
        res.setToInterconnectRateCentsPerUnit(rateToUse.getToInterconnectCentsPerUnit());
        res.setDescription(rateToUse.getDescription());
        res.setRateId(rateToUse.getServiceRateId());
        res.setFromInterconnectCurrency(rateToUse.getFromInterconnectRateCurrency());
        res.setToInterconnectCurrency(rateToUse.getToInterconnectRateCurrency());
        res.setRatingHint(rateToUse.getRatingHint());
        log.debug("Rating hint is [{}]", res.getRatingHint());

        String overriddenRatingGroup = res.getPropertyFromHint("OverriddenRatingGroup");
        if (overriddenRatingGroup != null) {
            log.debug("This requests rating data has overridden the rating group from [{}] to [{}]", ratingKey.getRatingGroup(), overriddenRatingGroup);
            ratingKey.setRatingGroup(overriddenRatingGroup);
        } else {
            throw new RuntimeException("Invalid rate configuration. Service rate must have an OverriddenRatingGroup -- Service Rate Id is " + rateToUse.getServiceRateId());
        }

        res.setRetailRateCentsPerUnit(rateToUse.getRetailCentsPerUnit());

        if (DAO.isNegative(res.getRetailRateCentsPerUnit())) {
            throw new RuntimeException("Destination is barred");
        }

        if (log.isDebugEnabled()) {
            Stopwatch.stop();
            log.debug("ServiceRatingEngine got retail rate for destination [{}] of [{}]c/unit and took {}", new Object[]{ratingKey.getTo(), res.getRetailRateCentsPerUnit(), Stopwatch.millisString()});
        }
        return res;
    }

    private PrefixMapNode[] getCachedRootPrefixMapsForTrunkSetLegAndService(String incomingTrunk, String outgoingTrunk, String serviceCode, String leg, boolean mustFindShortcodeRate) {
        String key = incomingTrunk + "|" + outgoingTrunk + "|" + serviceCode + "|" + leg + "|" + mustFindShortcodeRate;
        PrefixMapNode[] maps = perTrunkSetAndServicePrefixMaps.get(key);
        if (maps == null) {
            maps = getPrefixMapsForTrunkSetLegAndServiceFromDB(incomingTrunk, outgoingTrunk, serviceCode, leg, mustFindShortcodeRate);
            perTrunkSetAndServicePrefixMaps.put(key, maps);
        }
        return maps;
    }

    private PrefixMapNode[] getPrefixMapsForTrunkSetLegAndServiceFromDB(String incomingTrunk, String outgoingTrunk, String serviceCode, String leg, boolean mustFindShortcodeRate) {
        EntityManager em = null;
        PrefixMapNode sourcePrefixMap = new PrefixMapNode();
        PrefixMapNode destinationPrefixMap = new PrefixMapNode();
        try {
            // Use em to get config from table and put into prefix tree
            em = JPAUtils.getEM(emf);
            List<ServiceRate> dbRates = DAO.getRatesForTrunkSetLegAndService(em, incomingTrunk, outgoingTrunk, serviceCode, leg, mustFindShortcodeRate);
            for (ServiceRate aRate : dbRates) {
                sourcePrefixMap.populate(String.valueOf(aRate.getFromPrefix()), aRate);
                destinationPrefixMap.populate(String.valueOf(aRate.getToPrefix()), aRate);
            }
        } finally {
            try {
                JPAUtils.closeEM(em);
            } catch (Exception ex) {
                log.warn("Failed to close EM: " + ex.toString());
            }
        }
        return new PrefixMapNode[]{sourcePrefixMap, destinationPrefixMap};
    }

    @Override
    public void reloadConfig(EntityManagerFactory emf) {
        this.emf = emf;
        reloadConfig();
    }

    private void reloadConfig() {
        log.debug("BestPrefixServiceRatingEngine reloading config");
        for (String key : perTrunkSetAndServicePrefixMaps.keySet()) {
            String[] bits = key.split("\\|", 5);
            perTrunkSetAndServicePrefixMaps.put(key, getPrefixMapsForTrunkSetLegAndServiceFromDB(bits[0], bits[1], bits[2], bits[3], bits[4].equals("true")));
        }
        int ccLength = BaseUtils.getProperty("env.e164.country.code").length();
        maxShortCodeLength = BaseUtils.getIntProperty("env.mm.shortcode.max.digits", 5) + ccLength;
        log.debug("BestPrefixServiceRatingEngine done reloading config");
    }

    @Override
    public void onStart(EntityManagerFactory emf) {
        this.emf = emf;
        reloadConfig();
        runner1 = Async.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                refreshCacheIfRequired();
            }
        }, 120000, 50000, 70000);
    }

    private static int cacheHourOfDayRefreshed = -1;
    private static final Lock lock = new ReentrantLock();

    private void refreshCacheIfRequired() {
        try {
            Calendar now = Calendar.getInstance();
            int currentHourOfDay = now.get(Calendar.HOUR_OF_DAY);
            if (cacheHourOfDayRefreshed != currentHourOfDay) {
                log.debug("Refresh may be required");
            } else {
                return;
            }
            if (!lock.tryLock()) {
                // Lock is held by another thread is doing a refresh
                if (cacheHourOfDayRefreshed == -1) {
                    // Rating config has never been done. Must wait
                    lock.lock();
                } else {
                    return;
                }
            }
            try {
                if (cacheHourOfDayRefreshed == currentHourOfDay) {
                    log.debug("Refresh is not actually required - must have been done by another thread");
                    return;
                }
                reloadConfig();
                cacheHourOfDayRefreshed = currentHourOfDay;
            } finally {
                lock.unlock();
            }
        } catch (Exception e) {
            log.warn("Error: ", e);
        }
    }

    @Override
    public void shutDown() {
        log.debug("Removing myself from scheduling");
        Async.cancel(runner1);
    }

}
