/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.base.sd;

import com.hazelcast.core.IMap;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.hazelcast.HazelcastHelper;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;
import com.smilecoms.commons.base.sd.IService.STATUS;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class ServiceDiscoveryAgent implements BaseListener {

    private static ServiceDiscoveryAgent myInstance;
    private static final Logger log = LoggerFactory.getLogger(ServiceDiscoveryAgent.class);
    private static final Random random = new Random(System.currentTimeMillis());
    private static final String SD_CONF_FILE = "/etc/smile/sd.conf";
    private static final Lock lock = new ReentrantLock();

    private Map<String, Set<IService>> currentUpServices = new HashMap<>(100);
    // Our biggest snapshot of services. Used for worst case scenario fallback
    private Map<String, Set<IService>> biggestEverCurrentUpServices = new HashMap<>(100);
    private int biggestEverEntries = 0;
    private ScheduledFuture future1;
    private ScheduledFuture future2;
    private int updateStatusMS = 2000;
    private int pullSeviceMS = 120000;
    private final HealthCheckPredicate healthCheckPredicate = new HealthCheckPredicate();

    private ServiceDiscoveryAgent() {
        init();
    }

    @Override
    public void propsAreReadyTrigger() {
    }

    @Override
    public void propsHaveChangedTrigger() {
        int updateStatusMSOrig = updateStatusMS;
        int pullSeviceMSOrig = pullSeviceMS;
        updateStatusMS = BaseUtils.getIntProperty("env.sd.status.update.delay.ms", updateStatusMS);
        pullSeviceMS = BaseUtils.getIntProperty("env.sd.pull.delay.ms", pullSeviceMS);
        if (pullSeviceMSOrig != pullSeviceMS || updateStatusMSOrig != updateStatusMS) {
            log.warn("SD delays have changed. Starting background threads again. updateStatusMS [{}] pullSeviceMS [{}]", updateStatusMS, pullSeviceMS);
            kickOffAsync();
        }

    }

    private void kickOffAsync() {
        if (future1 != null) {
            Async.cancel(future1);
        }
        future1 = Async.scheduleWithFixedDelay(new SmileBaseRunnable("SD.updateServiceStatuses") {
            @Override
            public void run() {
                try {
                    if (BaseUtils.IN_SHUTDOWN) {
                        log.warn("Not doing service status updates as we are in shutdown");
                        return;
                    }
                    if (HazelcastHelper.isHelperBusy()) {
                        log.warn("Not doing service status updates as HazelcastHelper is busy doing initialisation stuff");
                        return;
                    }
                    updateServiceStatuses();
                } catch (Exception e) {
                    log.warn("Error in a runner: ", e);
                }
            }

        }, 1000, updateStatusMS);

        if (future2 != null) {
            Async.cancel(future2);
        }

        Async.makeHappenOnceOnlyInThisJVM(new Runnable() {
            @Override
            public void run() {
                pullServices();
                if (currentUpServices.isEmpty()) {
                    throw new RuntimeException("Try again please");
                }
            }
        }, "INIT_SD", 2000);

        future2 = Async.scheduleAtFixedRate(new SmileBaseRunnable("SD.pullServices") {
            @Override
            public void run() {
                // We only do this in case the publish/subscribe is not working for some reason
                pullServices();
            }
        }, pullSeviceMS, pullSeviceMS);

    }

    private void init() {
        kickOffAsync();
        BaseUtils.registerForPropsChanges(this);
        BaseUtils.registerForJVMShutdown(new Thread() {
            @Override
            public void run() {
                setAllMyServicesAsGoingDown();
            }
        }
        );
        Async.makeHappenOnceOnlyInThisJVM(new Runnable() {
            @Override
            public void run() {
                HazelcastHelper.getBaseHazelcastInstance().<Boolean>getTopic("SD_CHANGE").addMessageListener(new SDMessageListener());
                log.warn("Successfully subscribed to service discovery change topic");
            }

        }, "SD_TOPIC_SUBSCRIBE", 2000);
    }

    public static ServiceDiscoveryAgent getInstance() {
        if (myInstance == null) {
            lock.lock();
            try {
                if (myInstance == null) {
                    myInstance = new ServiceDiscoveryAgent();
                }
            } finally {
                lock.unlock();
            }
        }
        return myInstance;
    }

    public void publishServiceForced(IService service) {
        publishService(service, true, System.currentTimeMillis());
    }

    private boolean isHazelCastInstanceUp() {
        return HazelcastHelper.isBaseHazelcastInstanceUp();
    }

    public void publishService(IService service) {
        publishService(service, false, System.currentTimeMillis());
    }
    
    private void publishService(final IService service, boolean forceStatusChange, final long origionalPublishTime) {
        if (BaseUtils.IN_SHUTDOWN) {
            return;
        }
        if (System.currentTimeMillis() - origionalPublishTime > 30000) {
            log.debug("Service publish request is too old. Ignoring");
            return;
        }
        log.debug("Request to publish service [{}]", service);
        if (!isHazelCastInstanceUp()) {
            if (!forceStatusChange) {
                log.warn("Hazelcast is not up. Will try publish [{}] again in 3s", service);
                Async.submitInTheFuture(new SmileBaseRunnable("SD.publishService") {
                    @Override
                    public void run() {
                        publishService(service, false, origionalPublishTime);
                    }
                }, 3000);
            }
            return;
        }

        IMap<String, IService> services = getServiceMap();

        // Be cautious and if the service is in hazelcast but not in a class this version of code can deserialise then we remove it and add the new one
        IService existing = null;
        try {
            existing = services.get(service.getKey());
        } catch (Exception e) {
            log.warn("Error getting existing service from hazelcast. Going to remove it: ", e);
            services.delete(service.getKey());
        }

        if (existing != null) {
            log.debug("Found existing services [{}]", existing);
            if (forceStatusChange) {
                log.warn("Got a request to force an update in hazelcast [{}]", service);
            } else if (existing.isGoingDown()) {
                // If the JVM was shutting down then IN_SHUTDOWN would be true and we would not be able to get here
                log.warn("Updating [{}] in hazelcast as it was going down but is now back up", service);
            } else if (!existing.isUp()) {
                // Normal publishing cannot override a DOWN or a PAUSED
                log.debug("Got a non forced request to publish service but its currently not up. Updating config but leaving status alone Currently: [{}] Requested: [{}]", existing, service);
                service.setStatus(STATUS.valueOf(existing.getStatus()));
            } else {
                log.debug("Service is still up [{}]", service);
            }
            service.inheritHistory(existing);
        } else {
            service.setStatus(STATUS.DOWN);
            log.warn("Adding [{}] to hazelcast as it did not exist. Note its defaulted as DOWN till its tested as being up", service);
        }
        service.persistIfDifferent(services, existing);

    }

    private IService pickRandomService(Set<IService> svcList, boolean useWeighting) {
        if (svcList.isEmpty()) {
            return null;
        }
        if (svcList.size() == 1) {
            // Performance shortcut
            return svcList.iterator().next();
        }
        int weightCount = 0;
        if (!useWeighting) {
            weightCount = svcList.size();
        } else {
            for (IService svc : svcList) {
                weightCount += svc.getWeight();
            }
        }
        if (weightCount == 0) {
            return null;
        }
        int percentPerWeightPoint = 100 / weightCount;

        int lowerBound = 0;
        int upperBound;
        int percentagePick = random.nextInt(100); //a random percentage
        IService svcToReturn = null;
        int thisEndpointsWeight = 1;
        for (IService svc : svcList) {
            svcToReturn = svc;
            if (useWeighting) {
                thisEndpointsWeight = svc.getWeight();
            }
            upperBound = lowerBound + (thisEndpointsWeight * percentPerWeightPoint);
            if (percentagePick >= lowerBound && percentagePick < upperBound) {
                // This endpoint is in the range of the random percentage
                break;
            }
            lowerBound = upperBound;
        }
        return svcToReturn;
    }

    public Map<String, Set<IService>> getCurrentUpServices() {
        return currentUpServices;
    }

    private void logServices(Map<String, Set<IService>> services) {
        try {
            for (String serviceName : services.keySet()) {
                for (IService service : services.get(serviceName)) {
                    log.warn("Current Up Service: [{}]", service);
                }
            }
        } catch (Exception e) {
            log.warn("Error: ", e);
        }
    }

    public IService getServiceBestEffort(String serviceName) throws Exception {
        try {
            return getAvailableService(serviceName);
        } catch (Exception e) {
            log.warn("Could not get a service normally. Going to try best effort");
        }
        return getServiceFromPossibleOptions(biggestEverCurrentUpServices, serviceName);
    }

    public IService getAvailableService(String serviceName) throws Exception {
        return getServiceFromPossibleOptions(currentUpServices, serviceName);
    }

    private IService getServiceFromPossibleOptions(Map<String, Set<IService>> services, String serviceName) throws Exception {
        log.debug("Request for service [{}]", serviceName);
        Set<IService> possibleOptions = services.get(serviceName);
        if (possibleOptions == null || possibleOptions.isEmpty()) {
            throw new Exception("No service is available for -- " + serviceName);
        }

        IService svcToReturn;
        Set<IService> tmpSet = new HashSet<>(100);

        // Try 1 : Pick from list of services that are this version and available to this host and non zero weight
        for (IService svc : possibleOptions) {
            if (svc.isAvailable(true, true)) {
                tmpSet.add(svc);
            }
        }
        svcToReturn = pickRandomService(tmpSet, true);
        if (svcToReturn != null) {
            return svcToReturn;
        }
        log.debug("A service was requested and none were available for this host, version and non-zero weight [{}]. Being more leniant on host", serviceName);

        // Try 2 : Pick from list of services ignoring host but considering weight and version
        tmpSet.clear();
        for (IService svc : possibleOptions) {
            if (svc.isAvailable(true, false)) {
                tmpSet.add(svc);
            }
        }
        svcToReturn = pickRandomService(tmpSet, true);
        if (svcToReturn != null) {
            return svcToReturn;
        }
        log.debug("A service was requested and none were available with non-zero weight for this version on any host [{}]. Being more leniant by ignoring weight", serviceName);

        // Try 3 : Now ignore weighting
        svcToReturn = pickRandomService(tmpSet, false);
        if (svcToReturn != null) {
            return svcToReturn;
        }
        log.debug("A service [{}] was requested and none were available for this version anywhere. Now checking for any version or weight...", serviceName);

        // Try 4 : Pick from list of services that are any version and correct host with any weight
        tmpSet.clear();
        for (IService svc : possibleOptions) {
            if (svc.isAvailable(false, true)) {
                tmpSet.add(svc);
            }
        }
        svcToReturn = pickRandomService(tmpSet, false);
        if (svcToReturn != null) {
            return svcToReturn;
        }

        // Try 5 : Pick from list of services that are any version and available on any host with any weight
        tmpSet.clear();
        for (IService svc : possibleOptions) {
            if (svc.isAvailable(false, false)) {
                tmpSet.add(svc);
            }
        }
        svcToReturn = pickRandomService(tmpSet, false);
        if (svcToReturn != null) {
            log.debug("Managed to find a service for [{}] by being super leniant", serviceName);
            return svcToReturn;
        }
        log.warn("A service was requested and none were available at all [{}]. Here is our current service list:", serviceName);
        logServices(services);

        BaseUtils.sendTrapToOpsManagement(
                BaseUtils.MAJOR,
                serviceName,
                "A service was requested but none are available -- " + serviceName);

        throw new Exception("No service is available for -- " + serviceName);
    }

    public void resetServiceList() {
        getServiceMap().clear();
        publishServiceChange();
    }

    public void deleteService(String key) {
        getServiceMap().delete(key);
        publishServiceChange();
    }

    public IService getService(String key) {
        return (IService) getServiceMap().get(key);
    }

    public IMap<String, IService> getServiceMap() {
        return HazelcastHelper.getBaseHazelcastInstance().getMap("Services");
    }

    public Map<String, IService> getServiceMapCopy() {
        Set<Entry<String, IService>> entries;
        entries = getServiceMap().entrySet();
        Map<String, IService> ret = new HashMap<>(entries.size());
        for (Entry<String, IService> entry : entries) {
            ret.put(entry.getKey(), entry.getValue());
        }
        return ret;
    }

    ScheduledFuture publishFuture = null;

    void publishServiceChange() {
        // Delay a bit to give things time to settle and cancel anything in the queue
        if (publishFuture != null && !publishFuture.isDone()) {
            log.debug("Cancelling future in the queue");
            publishFuture.cancel(false);
        }
        log.debug("Adding future to the queue");
        publishFuture = Async.submitInTheFuture(new SmileBaseRunnable("SD.ServiceChangePublish") {
            @Override
            public void run() {
                log.debug("Publishing event that a service has changed and everyone needs to know");
                HazelcastHelper.getBaseHazelcastInstance().getTopic("SD_CHANGE").publish(true);
            }
        }, 2000);
    }

    private void updateServiceStatuses() {
        log.debug("In updateServiceStatuses");
        if (System.getProperty("SD_NO_TEST") != null) {
            log.debug("Testing is off as parameter SD_NO_TEST != null");
            return;
        }
        IMap<String, IService> services = getServiceMap();

        Set<String> servicesNeedingTesting = services.keySet(healthCheckPredicate);
        log.debug("Got [{}] services that may require testing", servicesNeedingTesting.size());
        List<String> keysShuffled = new ArrayList<>(servicesNeedingTesting.size());
        keysShuffled.addAll(servicesNeedingTesting);
        Collections.shuffle(keysShuffled);
        for (String svcKey : keysShuffled) {
            try {
                IService svc = services.get(svcKey);
                if (svc.isStale()) {
                    log.warn("Removing stale service from hazelcast [{}]", svc);
                    services.delete(svc.getKey());
                    continue;
                }
                svc.doHealthCheck(services);
            } catch (Exception e) {
                log.warn("A service in hazelcast has some issue. Ignoring it: [{}]", e.toString());
            }
        }
        log.debug("Finished updateServiceStatuses");
    }

    void pullServices() {

        if (checkForPanicMode()) {
            return;
        }
        if (HazelcastHelper.isHelperBusy()) {
            log.warn("Not doing service pull as HazelcastHelper is busy doing initialisation stuff");
            return;
        }
        // Keep list in local heap for performance reasons
        if (!isHazelCastInstanceUp()) {
            log.debug("Hazelcast is down. Returning");
            return;
        }
        log.debug("Pulling latest snapshot of services from hazelcast");
        Map<String, IService> services = getServiceMapCopy();
        int svcSize = services.size();
        if (svcSize == 0) {
            log.warn("Hazelcast service list is empty! Going to keep what we have");
            return;
        }
        Map<String, Set<IService>> currentUpServicesTmp = new HashMap<>(svcSize);
        int serviceCnt = 0;
        for (IService svc : services.values()) {
            try {
                if (svc.isStale()) {
                    log.warn("Removing stale service from hazelcast [{}]", svc);
                    getServiceMap().delete(svc.getKey());
                    continue;
                }
                if (!svc.isUp()) {
                    if (!BaseUtils.IN_GRACE_PERIOD) {
                        log.info("Service is not UP [{}] so wont put it in current services list", svc);
                    }
                    continue;
                }
                Set<IService> serviceSet = currentUpServicesTmp.get(svc.getServiceName());
                if (serviceSet == null) {
                    serviceSet = new HashSet<>();
                    currentUpServicesTmp.put(svc.getServiceName(), serviceSet);
                }
                serviceSet.add(svc);
                serviceCnt++;
                log.debug("Putting service into local map [{}]", svc);
            } catch (Exception e) {
                log.warn("A service in hazelcast has some issue. Ignoring it: [{}]", e.toString());
            }
        }
        if (services.isEmpty()) {
            log.warn("Services map in hazelcast is empty!");
        }
        if (!currentUpServicesTmp.isEmpty()) {
            currentUpServices = currentUpServicesTmp;
            log.debug("Populated local copy of services map with [{}] service names", currentUpServicesTmp.size());

            if (serviceCnt > biggestEverEntries) {
                biggestEverCurrentUpServices.clear();
                biggestEverCurrentUpServices.putAll(currentUpServices);
                ObjectOutputStream oos = null;
                try {
                    File f = new File(SD_CONF_FILE);
                    if (f.exists()) {
                        f.delete();
                    }
                    log.warn("Persisting our best ever service list to file [{}]", SD_CONF_FILE);
                    oos = new ObjectOutputStream(new FileOutputStream(SD_CONF_FILE));
                    oos.writeObject(biggestEverCurrentUpServices);
                    biggestEverEntries = serviceCnt;
                } catch (Exception e) {
                    log.warn("Error persisting service list: ", e);
                } finally {
                    if (oos != null) {
                        try {
                            oos.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private boolean checkForPanicMode() {
        File f = new File("/etc/smile/sd_panic_mem");
        if (f.exists() && !biggestEverCurrentUpServices.isEmpty()) {
            log.warn("I AM IN PANIC MODE. USING THE BEST SET OF SERVICES I EVER HAD IN THIS JVM INSTEAD OF HAZELCAST");
            currentUpServices = biggestEverCurrentUpServices;
            return true;
        }

        f = new File("/etc/smile/sd_panic_file");
        if (f.exists()) {
            log.warn("I AM IN PANIC MODE. USING THE BEST SET OF SERVICES I PERSISTED ON DISK INSTEAD OF HAZELCAST");
            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(new FileInputStream(SD_CONF_FILE));
                biggestEverCurrentUpServices = (Map<String, Set<IService>>) ois.readObject();
                currentUpServices = biggestEverCurrentUpServices;
                return true;
            } catch (Exception e) {
                log.warn("Error: ", e);
            } finally {
                if (ois != null) {
                    try {
                        ois.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
        return false;
    }

    public boolean setAllMyServicesAsGoingDown() {
        boolean wasSomethingInHazelCast = false;
        try {
            if (!isHazelCastInstanceUp()) {
                log.warn("Base Hazelcast instance is not up");
                return wasSomethingInHazelCast;
            }
            log.error("Setting my resources in hazelcast as down");
            IMap<String, IService> services = getServiceMap();
            for (String svcKey : services.keySet()) {
                try {
                    IService svc = services.get(svcKey);
                    if (svc.isInTheJVM(BaseUtils.JVM_ID) && !svc.getStatus().equals(STATUS.PAUSED.toString())) {
                        log.error("Putting in hazelcast as down [{}]", svc);
                        svc.setGoingDown();
                        svc.persist(services);
                        wasSomethingInHazelCast = true;
                    }
                } catch (Exception e) {
                    log.warn("A service in hazelcast has some issue. Ignoring it: [{}]", e.toString());
                }
            }
            publishServiceChange();
        } catch (Exception e) {
            log.warn("Error setting myself as down: [{}]", e.toString());
        }
        return wasSomethingInHazelCast;
    }

}
