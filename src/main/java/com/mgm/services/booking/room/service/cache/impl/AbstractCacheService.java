package com.mgm.services.booking.room.service.cache.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.mgm.services.booking.room.properties.ApplicationProperties;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;

import com.mgm.services.booking.room.properties.CacheProperties;

import lombok.extern.log4j.Log4j2;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

/**
 * Abstract class for all common cache service related functionality. All cache
 * service classes should extend this class and override required functionality
 * to build the cache.
 */
@Log4j2
public abstract class AbstractCacheService {
    
    private static final String RESPONSE_STR = "response";
    private static final String CACHE_EMPTY_STR = "cacheEmpty";

    @Autowired
    private EhCacheManagerFactoryBean ehCacheManager;

    /** The executor service. */
    @Autowired
    private ScheduledExecutorService executorService;
    
    @Autowired
    private CacheProperties cacheProperties;

    @Autowired
    private ApplicationProperties applicationProperties;

    /** The load data in progress. */
    private boolean loadDataInProgress;

    /** The failure count. */
    protected int failureCount;

    /**
     * Gets the cache mgr.
     *
     * @return the cache mgr
     */
    private CacheManager getCacheMgr() {
        return ehCacheManager.getObject();
    }

    /**
     * Gets the cached object.
     *
     * @param key
     *            the key
     * @return the cached object
     */
    public Object getCachedObject(Object key) {
        log.debug("[EhCacheLogger]|'{}' Cache -> Getting cached object with the key {} ", getCacheName(), key);
        final Element element = getCacheMgr().getCache(getCacheName()).get(key);
        if (null == element) {
            return null;
        }
        if(cacheProperties.isRedisEnabled()){
            log.info("Data found in ehCache but not in Redis for - {}",key);
        }
        return element.getObjectValue();
    }

    /**
     * Refresh cache.
     *
     * @return the string
     */
    public String refreshCache() {
        log.info("[EhCacheLogger]|'{}' Cache -> Refreshing the cache.", getCacheName());
        try {
            final Map<String, Object> resultMap = loadDataToCache();
            return resultMap != null && resultMap.containsKey(RESPONSE_STR) ? resultMap.get(RESPONSE_STR).toString()
                    : null;
        } catch (RuntimeException ex) {
            log.error("[EhCacheLogger]|'{}' Cache -> Error refreshing data for cache, got exception: {} ",
                    getCacheName(), ex, ex);
            return "Error refreshing data for cache, got exception: " + ex.getMessage();
        }
    }

    /**
     * First load data to cache on server start up.
     */
    public void firstLoadDataToCache() {
        int attempts = getRetryAttempts();
        boolean isCacheEmpty = true;
        while (attempts != 0 && isCacheEmpty && applicationProperties.isPhoenixCacheEnabled()) {
            try {
                log.info("[EhCacheLogger]|'{}' Cache -> cache loading at start up.", getCacheName());
                final Map<String, Object> resultMap = loadDataToCache();
                isCacheEmpty = (Boolean) resultMap.get(CACHE_EMPTY_STR);
            } catch (Exception ex) {
                // printing error summary as well as stack trace
                log.error("[EhCacheLogger]|'{}' Cache -> Error loading data for cache: {} ", getCacheName(), ex);
            }
            attempts--;
        }

        reloadDataToCache();
    }

    /**
     * Reload data to cache.
     */
    private void reloadDataToCache() {
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                if (applicationProperties.isPhoenixCacheEnabled()) {
                    refreshCache();
                }
            }
        };
        if (applicationProperties.isPhoenixCacheEnabled()) {
            executorService.scheduleAtFixedRate(task, getRefreshPeriodInSeconds(), getRefreshPeriodInSeconds(),
                    TimeUnit.SECONDS);
            log.info("[EhCacheLogger]|'{}' Cache -> cache refresh task scheduled to run every {} seconds",
                    getCacheName(), getRefreshPeriodInSeconds());
        }
    }

    /**
     * Load data to cache.
     *
     * @return the map
     * @throws Exception
     *             the exception
     */
    private Map<String, Object> loadDataToCache() {

        log.info("[EhCacheLogger]|'{}' Cache -> In loadDataToCache", getCacheName());
        final Map<String, Object> resultMap = new HashMap<>();

        if (loadDataInProgress) {
            log.error("[EhCacheLogger]|'{}' Cache -> Cache load already in progress. ignoring this invocation.",
                    getCacheName());
            resultMap.put(CACHE_EMPTY_STR, false);
            resultMap.put(RESPONSE_STR, "Cache load already in progress. ignoring this invocation.");
            return resultMap;
        }

        loadDataInProgress = true;

        try {

            final Map<Object, Object> cacheObj = new ConcurrentHashMap<Object, Object>();
            final List<String> keys = getKeys();
            if (CollectionUtils.isNotEmpty(keys)) {
                // reset failure count
                resetFailures();
                final List<Future<Map<Object, Object>>> tasks = new ArrayList<Future<Map<Object, Object>>>();
                for (final String key : keys) {
                    final Callable<Map<Object, Object>> task = new Callable<Map<Object, Object>>() {
                        @Override
                        public Map<Object, Object> call() throws Exception {
                            return fetchData(key);
                        }
                    };
                    log.info("[EhCacheLogger]|'{}' Cache -> In loadDataToCache, added task for key: {} ",
                            getCacheName(), key);
                    tasks.add(executorService.submit(task));
                }
                getAndCache(tasks, cacheObj);
            }

            boolean isCacheEmpty = false;
            if (cacheObj != null && !cacheObj.isEmpty()) {
                final long startTime = System.currentTimeMillis();
                for (final Entry<Object, Object> cacheEntry : cacheObj.entrySet()) {
                    addObjectInCache(cacheEntry.getKey(), cacheEntry.getValue());
                }
                log.info("[EhCacheLogger]|'{}' Cache -> Put {} items to '{}' cache in {} ms. # of failures: {}",
                        getCacheName(), getCache().getSize(), getCacheName(), (System.currentTimeMillis() - startTime),
                        getFailureCount());
                resultMap.put(RESPONSE_STR, "Put " + getCache().getSize() + " items to '" + getCacheName()
                        + "' cache. # of failures: " + getFailureCount());
                if (getCache().getSize() == 0) {
                    isCacheEmpty = true;
                }
            } else {
                isCacheEmpty = true;
            }
            resultMap.put(CACHE_EMPTY_STR, isCacheEmpty);

            return resultMap;
        } finally {
            loadDataInProgress = false;
        }
    }
    
    private void getAndCache(List<Future<Map<Object, Object>>> tasks, Map<Object, Object> cacheObj) {
        for (final Future<Map<Object, Object>> future : tasks) {
            Map<Object, Object> response = null;
            try {
                response = future.get(cacheProperties.getGlobalTimeoutInSecs(), TimeUnit.SECONDS);

                // This catch is to prevent failure from one key failing
                // the entire cache built up
            } catch (CancellationException | InterruptedException | ExecutionException | TimeoutException e) {
                log.error("[EhCacheLogger]|'{}' Cache -> Error building cache: {} ", getCacheName(), e, e);
            }
            if (response != null) {
                cacheObj.putAll(response);
            }
        }
    }

    /**
     * Adds the object in cache.
     *
     * @param key
     *            the key
     * @param value
     *            the value
     */
    protected void addObjectInCache(Object key, Object value) {
        log.debug("[EhCacheLogger]|'{}' Cache -> Adding object in cache with the key {} ", getCacheName(), key);
        final Cache cache = getCacheMgr().getCache(getCacheName());
        cache.put(new Element(key, value));
    }

    /**
     * Gets the cache.
     *
     * @return the cache
     */
    protected Cache getCache() {
        return getCacheMgr().getCache(getCacheName());
    }

    /**
     * Gets the cache keys.
     *
     * @return the cache keys
     */
    @SuppressWarnings("unchecked")
    public List<String> getCacheKeys() {
        return getCacheMgr().getCache(getCacheName()).getKeys();
    }

    /**
     * Gets the cache name.
     *
     * @return the cache name
     */
    public abstract String getCacheName();

    /**
     * Fetch data to be cached.
     *
     * @param key
     *            the key
     * @return the map
     * @throws Exception
     *             the exception
     */
    protected abstract Map<Object, Object> fetchData(String key);

    /**
     * Gets the refresh period in seconds.
     *
     * @return the refresh period in seconds
     */
    protected abstract long getRefreshPeriodInSeconds();

    /**
     * Gets the retry attempts.
     *
     * @return the retry attempts
     */
    protected abstract int getRetryAttempts();

    /**
     * Gets the keys.
     *
     * @return the keys
     */
    protected abstract List<String> getKeys();

    /**
     * Gets the failure count.
     *
     * @return the failure count
     */
    protected int getFailureCount() {
        return this.failureCount;
    }

    /**
     * Reset failures.
     */
    protected void resetFailures() {
        this.failureCount = 0;
    }

}
