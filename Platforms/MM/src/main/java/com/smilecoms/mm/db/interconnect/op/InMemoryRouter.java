/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.db.interconnect.op;

import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.ExceptionManager;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.mm.db.interconnectroute.model.PortedNumber;
import com.smilecoms.mm.db.interconnectroute.model.ServiceRate;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
@Singleton
@Startup
public class InMemoryRouter implements BaseListener {

    private static final Lock lock = new ReentrantLock();
    private static final Logger log = LoggerFactory.getLogger(InMemoryRouter.class);
    private static ClassLoader myClassLoader;
    private static EntityManagerFactory emf = null;
    private static final Lock emfInitLock = new ReentrantLock();

    public static RoutingInfo getRoutingInfo(EntityManager em, String source, String destination, String leg, String serviceCode, String incomingTrunkId) throws Exception {

        // NOTE THAT SMS ROUTING IGNORES THE A NUMBER AND LOOKS AT THE SOURCE TRUNK AND DESTINATION NUMBER TO KNOW WHERE TO SEND IT
        source = Utils.getFriendlyPhoneNumberKeepingCountryCode(source);
        destination = Utils.getFriendlyPhoneNumberKeepingCountryCode(destination);

        log.debug("In InMemoryRouter for Source [{}], Destination [{}], Leg [{}], ServiceCode [{}], IncomingTrunk [{}]", new Object[]{source, destination, leg, serviceCode, incomingTrunkId});

        String messageClass = null;
        int ccLength = BaseUtils.getProperty("env.e164.country.code").length();
        int maxShortCodeLength = BaseUtils.getIntProperty("env.mm.shortcode.max.digits", 5) + ccLength;
        Set<String> shortCodePlugin = BaseUtils.getPropertyAsSet("env.mm.treat.as.shortcode");
        if (destination.length() <= maxShortCodeLength || shortCodePlugin.contains(destination)) {
            log.debug("This destination must be treated as a shortcode as its in env.mm.treat.as.shortcode or is short");
            messageClass = "ShortCodeSMSMessage";
        }

        Set<String> offnetPlugin = BaseUtils.getPropertyAsSet("env.mm.treat.as.offnet");
        if (offnetPlugin.contains(destination)) {
            log.debug("This destination must be treated as an offnet SMS as its in env.mm.treat.as.offnet");
            messageClass = "OffnetSMSMessage";
        }

        boolean lookForShortcodeRates = false;

        if (source.length() <= maxShortCodeLength || destination.length() <= maxShortCodeLength) {
            log.debug("From or to number is a short code so we will only use shortcode rates for routing");
            lookForShortcodeRates = true;
        }

        if (!incomingTrunkId.equals("0") && !leg.equals("T")) {
            // Messages coming from offnet must be on the terminating leg
            throw new Exception("Cannot route a number from offnet which is not a terminating leg");
        } else if (incomingTrunkId.equals("0") && !leg.equals("O")) {
            // Terminating messages that originated onnet are not routed on the terminating leg so this should not happen
            throw new Exception("A message originating onnet must be on the originating leg");
        }

        int destinationPartnerId = getBestPrefixMatchPartnerForLeg(em, destination, leg, lookForShortcodeRates);

        int overridingDestinationPartnerId = -1;
        try {
            if (log.isDebugEnabled()) {
                log.debug("Checking if destinationPartnerId [{}] has an overriding partner for SMS", destinationPartnerId);
            }
            overridingDestinationPartnerId = Integer.parseInt(BaseUtils.getSubProperty("env.mm.sms.override.partner", Integer.toString(destinationPartnerId)));
        } catch (Exception e) {
            log.debug("env.mm.sms.override.partner does not exist");
        }

        if (overridingDestinationPartnerId != -1) {
            if (log.isDebugEnabled()) {
                log.debug("destinationPartnerId [{}] has been overridden to [{}]", destinationPartnerId, overridingDestinationPartnerId);
            }
            destinationPartnerId = overridingDestinationPartnerId;
        }

        String outgoingTrunkId = getBestTrunkForPartner(em, destinationPartnerId);
        return new RoutingInfo(incomingTrunkId, outgoingTrunkId, messageClass);
    }

    private static int getBestPrefixMatchPartnerForLeg(EntityManager em, String destination, String leg, boolean lookForShortcodeRates) throws Exception {
        log.debug("In getBestPrefixMatchPartnerForLeg for [{}][{}][{}]", new Object[]{destination, leg, lookForShortcodeRates});
        refreshCacheIfNecessary(em);
        long destinatlonAsLong;
        try {
            destinatlonAsLong = Utils.getFirstNumericPartOfString(destination);
        } catch (NumberFormatException nfe) {
            throw new Exception("Invalid destination. Destination must have a numeric part -- " + destination);
        }
        Integer partnerId = getPartnerIfNumberIsPorted(destinatlonAsLong);

        if (partnerId != null) {
            log.debug("Destination [{}] has ported to [{}]", destination, partnerId);
            return partnerId;
        } else {
            log.debug("Number has not been ported");
        }

        Map<Integer, ServiceRate> rates = null;
        boolean isDefinitelyTerminatingOnSmile = false;
        if (leg.equals("O")) {
            if (lookForShortcodeRates) {
                rates = prefixMapForOLegShortCode.getBestMatch(destination).getAllLeafData();
            } else {
                rates = prefixMapForOLegNonShortCode.getBestMatch(destination).getAllLeafData();
            }
        } else if (leg.equals("T")) {
            // We can assume that a terminating SMS cannot be destined offnet. We can use this to take a shortcut
            isDefinitelyTerminatingOnSmile = true;
            if (lookForShortcodeRates) {
                rates = prefixMapForTLegShortCode.getBestMatch(destination).getAllLeafData();
            } else {
                rates = prefixMapForTLegNonShortCode.getBestMatch(destination).getAllLeafData();
            }
        }

        if (rates == null) {
            throw new Exception("No route found for message -- Destination:" + destination + " Leg:" + leg + " Shortcode:" + lookForShortcodeRates);
        }

        log.debug("[{}] possible rates found", rates.size());

        int highestPriority = -1;
        ServiceRate highestPriorityrate = null;
        Date now = new Date();
        for (ServiceRate rate : rates.values()) {
            if (log.isDebugEnabled()) {
                log.debug("Looking at service rate id [{}][{}]", rate.getServiceRateId(), rate.getDescription());
            }
            if (rate.getDateFrom().after(now)) {
                continue;
            }
            if (rate.getDateTo().before(now)) {
                continue;
            }
            if (rate.getPriority() > highestPriority) {
                highestPriorityrate = rate;
                highestPriority = rate.getPriority();
                if (isDefinitelyTerminatingOnSmile) {
                    log.debug("Take a shortcut. We know we have a match for a SMS terminating on Smile. No need to loop through all matches");
                    break;
                }
            }
        }
        if (highestPriorityrate == null) {
            throw new Exception("No route found for message -- Destination:" + destination + " Leg:" + leg + " Shortcode:" + lookForShortcodeRates);
        }
        partnerId = highestPriorityrate.getToInterconnectPartnerId();
        log.debug("Destination [{}] must be routed to partner id [{}] for Shortcode [{}]", new Object[]{destination, partnerId, lookForShortcodeRates});
        return partnerId;
    }

    private static String getBestTrunkForPartner(EntityManager em, int destinationPartnerId) throws Exception {
        log.debug("In getBestTrunkForPartner for [{}]", destinationPartnerId);
        String trunk = partnerTrunkMap.get(destinationPartnerId);
        if (trunk == null) {
            trunk = DAO.getBestTrunkForPartner(em, destinationPartnerId);
            partnerTrunkMap.put(destinationPartnerId, trunk);
        }
        return trunk;
    }

    private static PrefixMapNode prefixMapForOLegNonShortCode = new PrefixMapNode();
    private static PrefixMapNode prefixMapForTLegNonShortCode = new PrefixMapNode();
    private static PrefixMapNode prefixMapForOLegShortCode = new PrefixMapNode();
    private static PrefixMapNode prefixMapForTLegShortCode = new PrefixMapNode();
    private static final Map<Integer, String> partnerTrunkMap = new ConcurrentHashMap<>();

    private static void refreshCache(EntityManager em) {
        log.debug("Refreshing SMS routing cache");
        PrefixMapNode prefixMapForOLegNonShortCodeTmp = new PrefixMapNode();
        PrefixMapNode prefixMapForTLegNonShortCodeTmp = new PrefixMapNode();
        PrefixMapNode prefixMapForOLegShortCodeTmp = new PrefixMapNode();
        PrefixMapNode prefixMapForTLegShortCodeTmp = new PrefixMapNode();
        List<ServiceRate> dbRates = DAO.getSMSRates(em);
        for (ServiceRate aRate : dbRates) {
            String hint = aRate.getRatingHint().toLowerCase();
            if (aRate.getLeg().equals("O")) {
                if (hint.contains("shortcode=true")) {
                    log.debug("Service rate [{}] is going into prefixMapForOLegShortCodeTmp", aRate.getServiceRateId());
                    prefixMapForOLegShortCodeTmp.populate(String.valueOf(aRate.getToPrefix()), aRate);
                } else {
                    log.debug("Service rate [{}] is going into prefixMapForOLegNonShortCodeTmp", aRate.getServiceRateId());
                    prefixMapForOLegNonShortCodeTmp.populate(String.valueOf(aRate.getToPrefix()), aRate);
                }
            } else if (aRate.getLeg().equals("T")) {
                if (hint.contains("shortcode=true")) {
                    log.debug("Service rate [{}] is going into prefixMapForTLegShortCodeTmp", aRate.getServiceRateId());
                    prefixMapForTLegShortCodeTmp.populate(String.valueOf(aRate.getToPrefix()), aRate);
                } else {
                    log.debug("Service rate [{}] is going into prefixMapForTLegNonShortCodeTmp", aRate.getServiceRateId());
                    prefixMapForTLegNonShortCodeTmp.populate(String.valueOf(aRate.getToPrefix()), aRate);
                }
            }
        }
        prefixMapForOLegNonShortCode = prefixMapForOLegNonShortCodeTmp;
        prefixMapForTLegNonShortCode = prefixMapForTLegNonShortCodeTmp;
        prefixMapForOLegShortCode = prefixMapForOLegShortCodeTmp;
        prefixMapForTLegShortCode = prefixMapForTLegShortCodeTmp;
        log.debug("Finished refreshing SMS routing cache");
    }

    private static Integer getPartnerIfNumberIsPorted(long destination) {
        return getPortedNumberMap().get(destination);
    }

    private static Map<Long, Integer> portedNumberMap = new HashMap<>();

    private static boolean loadingPortedNumbers = false;

    private static Map<Long, Integer> getPortedNumberMap() {
        return portedNumberMap;
    }

    private static void refreshPortedNumberMap() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                if (loadingPortedNumbers) {
                    return;
                }
                loadingPortedNumbers = true;
                // Wait till other property refreshes have done their work to prevent too much load
                Utils.sleep(Utils.getRandomNumber(10000, 60000));
                EntityManager myem = null;
                try {
                    long start = System.currentTimeMillis();
                    log.warn("In thread doing ported number reload");
                    myem = JPAUtils.getEM(emf);
                    int limit = 1000;
                    long lastNum = 0;
                    Map<Long, Integer> tmpPortedNumberMap;
                    if (portedNumberMap.isEmpty()) {
                        tmpPortedNumberMap = portedNumberMap;
                    } else {
                        tmpPortedNumberMap = new HashMap<>(limit);
                    }
                    // Pace ourselves so as not to kill JVM
                    boolean gotRes;
                    do {
                        if (BaseUtils.IN_SHUTDOWN) {
                            return;
                        }
                        Utils.sleep(50);
                        JPAUtils.beginTransaction(myem);
                        List<PortedNumber> numbers = DAO.getPortedNumbers(myem, limit, lastNum);
                        gotRes = !numbers.isEmpty();
                        for (PortedNumber num : numbers) {
                            tmpPortedNumberMap.put(num.getNumber(), num.getInterconnectPartnerId());
                            lastNum = num.getNumber();
                        }
                        JPAUtils.commitTransactionAndClear(myem);
                        log.debug("Loaded ported numbers. We now have [{}]", tmpPortedNumberMap.size());
                    } while (gotRes);

                    portedNumberMap = tmpPortedNumberMap;
                    long latency = System.currentTimeMillis() - start;
                    log.warn("Out thread doing ported number reload. Map size [{}] and took [{}]ms", portedNumberMap.size(), latency);
                } catch (Exception e) {
                    new ExceptionManager(log).reportError(e);
                } finally {
                    if (myem != null) {
                        JPAUtils.closeEM(myem);
                    }
                }
                loadingPortedNumbers = false;
            }
        });
        t.setContextClassLoader(myClassLoader);
        t.start();
    }

    private static int cacheHourOfDayRefreshed = -1;

    private static void refreshCacheIfNecessary(EntityManager em) {
        int currentHourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
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
            reloadConfig(em);
            cacheHourOfDayRefreshed = currentHourOfDay;
        } finally {
            lock.unlock();
        }
    }

    private static void reloadConfig(EntityManager em) {
        refreshCache(em);
        partnerTrunkMap.clear();
        refreshPortedNumberMap();
    }

    @Override
    public void propsHaveChangedTrigger() {
        cacheHourOfDayRefreshed = 100;
    }

    @PostConstruct
    public void startUp() {
        myClassLoader = this.getClass().getClassLoader();
        BaseUtils.registerForPropsAvailability(this);
        if (emf == null) {
            emfInitLock.lock();
            try {
                if (emf == null) {
                    emf = JPAUtils.getEMF("MMPU_RL");
                }
            } finally {
                emfInitLock.unlock();
            }
        }
        EntityManager em = null;
        try {
            em = JPAUtils.getEM(emf);
            refreshCacheIfNecessary(em);
        } catch (Exception e) {
            new ExceptionManager(log).reportError(e);
        } finally {
            if (em != null) {
                JPAUtils.closeEM(em);
            }
        }
    }

    @Override
    public void propsAreReadyTrigger() {
        if (emf == null) {
            emfInitLock.lock();
            try {
                if (emf == null) {
                    emf = JPAUtils.getEMF("MMPU_RL");
                }
            } finally {
                emfInitLock.unlock();
            }
        }
        BaseUtils.deregisterForPropsAvailability(this);
        BaseUtils.registerForPropsChanges(this);
    }

    @PreDestroy
    public void shutDown() {
        BaseUtils.deregisterForPropsChanges(this);
        log.warn("Closing EMF on memory router");
        JPAUtils.closeEMF(emf);
    }
}
