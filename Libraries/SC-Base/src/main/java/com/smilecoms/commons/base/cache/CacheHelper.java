/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.base.cache;

import com.hazelcast.core.IMap;
import com.hazelcast.monitor.LocalMapStats;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.hazelcast.HazelcastHelper;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.*;

/**
 *
 * @author PCB
 */
public class CacheHelper {

    private static final Logger log = LoggerFactory.getLogger(CacheHelper.class);
    private static long dontcachetill = 0;
    private static IMap<String, Object> hcCache;

    static {
        Async.scheduleAtFixedRate(new SmileBaseRunnable("Base.CacheHelper") {
            @Override
            public void run() {
                removeStaleLocalCacheEntries();
                printRemoteCacheStats();
            }
        }, 10000, 10000);
    }

    private static IMap<String, Object> getCache() {
        // If there is any issue with hazelcast then we must fail immediately and return null from cache
        if (System.currentTimeMillis() < dontcachetill) {
            return null;
        }
        if (hcCache != null) {
            return hcCache;
        }
        try {
            hcCache = HazelcastHelper.getBaseHazelcastInstance().getMap("SmileBaseCache");
            return hcCache;
        } catch (Exception e) {
            dontcachetill = System.currentTimeMillis() + 60000;
            log.warn("Error gettng cache map from Hazelcast.Will turn off cache for 1 minute: ", e);
            return null;
        }
    }

    public static Object getFromRemoteCache(String objectKey) {
        long start = System.currentTimeMillis();
        IMap<String, Object> cache = getCache();
        if (cache == null) {
            return null;
        }
        try {
            Object o = cache.get(objectKey);
            return o;
        } catch (Exception e) {
            dontcachetill = System.currentTimeMillis() + 60000;
            throw new CacheError(e.toString());
        } finally {
            long latency = System.currentTimeMillis() - start;
            BaseUtils.addStatisticSample("RemoteCacheLookup", BaseUtils.STATISTIC_TYPE.latency, latency);
        }
    }

    public static void putInRemoteCacheSync(String objectKey, Object o, int time, TimeUnit tu) {
        if (o == null) {
            log.warn("Attempt to put null object into remote cache");
            return;
        }
        long start = System.currentTimeMillis();
        IMap<String, Object> cache = getCache();
        if (cache == null) {
            return;
        }
        try {
            cache.put(objectKey, o, time, tu);
        } catch (Exception e) {
            dontcachetill = System.currentTimeMillis() + 60000;
            log.warn("Got an error putting object synchronously into remote cache. Error was: " + e.toString());
            throw new CacheError(e.toString());
        } finally {
            long latency = System.currentTimeMillis() - start;
            BaseUtils.addStatisticSample("RemoteCacheSyncPut", BaseUtils.STATISTIC_TYPE.latency, latency);
        }
    }

    public static void putInRemoteCacheSync(String objectKey, Object o, int timeInSecs) {
        putInRemoteCacheSync(objectKey, o, timeInSecs, TimeUnit.SECONDS);
    }

    public static void putInRemoteCache(String objectKey, Object o, int time, TimeUnit tu) {
        if (o == null) {
            log.warn("Attempt to put null object into remote cache");
            return;
        }
        long start = System.currentTimeMillis();
        IMap<String, Object> cache = getCache();
        if (cache == null) {
            return;
        }
        try {
            cache.putAsync(objectKey, o, time, tu);
        } catch (Exception e) {
            dontcachetill = System.currentTimeMillis() + 60000;
            log.warn("Got an error putting object asynchronously into remote cache. Error was: " + e.toString());
            throw new CacheError(e.toString());
        } finally {
            long latency = System.currentTimeMillis() - start;
            BaseUtils.addStatisticSample("RemoteCacheAsyncPut", BaseUtils.STATISTIC_TYPE.latency, latency);
        }
    }

    public static void putInRemoteCache(String objectKey, Object o, int timeInSecs) {
        putInRemoteCache(objectKey, o, timeInSecs, TimeUnit.SECONDS);
    }

    public static void removeFromRemoteCache(String objectKey) {
        IMap<String, Object> cache = getCache();
        if (cache == null) {
            return;
        }
        try {
            cache.delete(objectKey);
        } catch (Exception e) {
            dontcachetill = System.currentTimeMillis() + 60000;
            throw new CacheError(e.toString());
        }
    }

    private static void printRemoteCacheStats() {
        try {
            IMap<String, Object> cache = getCache();
            if (cache == null) {
                return;
            }
            LocalMapStats s = cache.getLocalMapStats();
            BaseUtils.sendStatistic(BaseUtils.FQ_SERVER_NAME, "Cache_Bytes", "byte", s.getHeapCost(), "CacheHelper");
            BaseUtils.sendStatistic(BaseUtils.FQ_SERVER_NAME, "Local_Cache_Entries", "count", localCache.size(), "CacheHelper");
            if (log.isDebugEnabled()) {
                log.debug("Hazelcast Cache Stats for this JVM: getOwnedEntryCount [{}] getBackupEntryCount [{}] getBackupCount [{}] getOwnedEntryMemoryCost [{}] getBackupEntryMemoryCost [{}] getHeapCost [{}]",
                        new Object[]{s.getOwnedEntryCount(), s.getBackupEntryCount(), s.getBackupCount(), s.getOwnedEntryMemoryCost(), s.getBackupEntryMemoryCost(), s.getHeapCost()});
            }
        } catch (Exception e) {
            log.warn("Error: ", e);
        }
    }

    private static final ConcurrentHashMap<String, LocalCacheEntry> localCache = new ConcurrentHashMap<>();

    public static void putInLocalCache(String objectKey, Object o, int timeInSecs) {
        LocalCacheEntry entry = new LocalCacheEntry(objectKey, o, timeInSecs);
        localCache.put(entry.key, entry);
        log.debug("Put object in local cache with key [{}]", entry.key);
    }

    public static void putInLocalCache(long objectKey, Object o, int timeInSecs) {
        putInLocalCache(String.valueOf(objectKey), o, timeInSecs);
    }

    @SuppressWarnings("unchecked")
    public static <C> C getFromLocalCache(long objectKey, Class<C> classType) {
        return getFromLocalCache(String.valueOf(objectKey), classType);
    }

    @SuppressWarnings("unchecked")
    public static <C> C getFromLocalCache(String objectKey, Class<C> classType) {
        ClassLoader cl = classType.getClassLoader();
        String key = objectKey + classType.getName() + (cl == null ? "basecl" : cl.hashCode());
        log.debug("Got object from local cache with key [{}]", key);
        LocalCacheEntry res = localCache.get(key);
        if (res != null && res.isFresh()) {
            try {
                return (C) res.data;
            } catch (Throwable e) {
                log.warn("Error casting object in cache. Will return null", e);
                return null;
            }
        } else {
            return null;
        }
    }

    public static void removeFromLocalCache(long objectKey, Class classType) {
        removeFromLocalCache(Long.toString(objectKey), classType);
    }
    
    public static void removeFromLocalCache(String objectKey, Class classType) {
        ClassLoader cl = classType.getClassLoader();
        String key = objectKey + classType.getName() + (cl == null ? "basecl" : cl.hashCode());
        localCache.remove(key);
    }
    
    public static <C> C  removeAndGetFromLocalCache(String objectKey, Class<C> classType) {
        ClassLoader cl = classType.getClassLoader();
        String key = objectKey + classType.getName() + (cl == null ? "basecl" : cl.hashCode());
        log.debug("Got object from local cache with key [{}]", key);
        LocalCacheEntry res = localCache.remove(key);
        if (res != null && res.isFresh()) {
            try {
                return (C) res.data;
            } catch (Throwable e) {
                log.warn("Error casting object in cache. Will return null", e);
                return null;
            }
        } else {
            return null;
        }
    }

    private static void removeStaleLocalCacheEntries() {
        int reapCnt = 0;
        try {
            if (log.isDebugEnabled()) {
                log.debug("Reaping local cache. Size is [{}]", localCache.size());
            }
            Iterator<Entry<String, LocalCacheEntry>> it = localCache.entrySet().iterator();
            while (it.hasNext()) {
                Entry<String, LocalCacheEntry> entry = it.next();
                LocalCacheEntry cachedEntry = entry.getValue();
                if (!cachedEntry.isFresh()) {
                    it.remove();
                    reapCnt++;
                }
            }
        } catch (Exception e) {
            log.error("Error in local cache reaper:", e);
        }
        log.debug("Finished reaping local cache. Size is [{}] and Reaped [{}]", localCache.size(), reapCnt);
    }

}

class LocalCacheEntry {

    protected Object data;
    protected long expiresAt;
    protected String key;

    LocalCacheEntry(String objectKey, Object o, int cacheSecs) {
        expiresAt = System.currentTimeMillis() + cacheSecs * 1000;
        data = o;
        ClassLoader cl = o.getClass().getClassLoader();
        key = objectKey + o.getClass().getName() + (cl == null ? "basecl" : cl.hashCode());
    }

    boolean isFresh() {
        return System.currentTimeMillis() < expiresAt;
    }
}
