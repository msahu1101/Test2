package com.mgm.services.booking.room.service.cache.impl;

import java.util.*;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.service.cache.rediscache.service.PhoenixComponentsRedisCacheService;
import com.mgm.services.booking.room.service.cache.rediscache.service.impl.RedisCacheReadServiceImpl;
import net.sf.ehcache.Cache;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Result;
import net.sf.ehcache.search.Results;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.PhoenixComponentsDAO;
import com.mgm.services.booking.room.model.phoenix.RoomComponent;
import com.mgm.services.booking.room.properties.CacheProperties;
import com.mgm.services.booking.room.service.cache.PhoenixComponentsCacheService;

import lombok.extern.log4j.Log4j2;

/**
 * Implementation class for PhoenixComponentsCacheService providing functions for
 * caching and returning components cache.
 */
@Component
@Log4j2
public class PhoenixComponentsCacheServiceImpl extends AbstractCacheService implements PhoenixComponentsCacheService {

    private static final String CACHE_NAME = "components";

    @Autowired
    private CacheProperties cacheProperties;

    @Autowired
    private PhoenixComponentsDAO phoenixComponentsDao;

    @Autowired
    private PhoenixComponentsRedisCacheService redisCacheReadServiceImpl;

    @Override
    public RoomComponent getComponent(String componentId) {
        if(cacheProperties.isRedisEnabled()){
          return  Optional.ofNullable(redisCacheReadServiceImpl.getComponent(componentId))
                  .orElseGet(()-> (RoomComponent) getCachedObject(componentId));
        }else{
            return (RoomComponent) getCachedObject(componentId);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.service.cache.impl.AbstractCacheService#
     * getCacheName()
     */
    @Override
    public String getCacheName() {
        return CACHE_NAME;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.service.cache.impl.AbstractCacheService#
     * fetchData(java.lang.String)
     */
    @Override
    protected Map<Object, Object> fetchData(String key) {
        Map<Object, Object> componentsMap = new HashMap<>();
        try {
            // using some block statements since this is background process
            // anyway
            log.info("[EhCacheLogger]|'{}' Cache -> Fetching room components ", getCacheName());
            Map<String, RoomComponent> components = phoenixComponentsDao.getRoomComponents();
            log.info("[EhCacheLogger]|'{}' Cache -> Successfully fetched room components ", getCacheName());
            if (MapUtils.isNotEmpty(components)) {
                componentsMap.putAll(components);
            }
        } catch (Exception e) {
            failureCount++;
            log.error("[EhCacheLogger]|'{}' Cache -> Exception while retrieving room components from Phoenix",
                    getCacheName(), e);
        }
        return componentsMap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.service.cache.impl.AbstractCacheService#
     * getRefreshPeriodInSeconds()
     */
    @Override
    protected long getRefreshPeriodInSeconds() {
        return cacheProperties.getRoomComponentsRefreshFreqInSecs();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.service.cache.impl.AbstractCacheService#
     * getRetryAttempts()
     */
    @Override
    protected int getRetryAttempts() {
        return 1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.service.cache.impl.AbstractCacheService#
     * getKeys()
     */
    @Override
    protected List<String> getKeys() {
        final List<String> keys = new ArrayList<>();
        keys.add("static");
        // Just using a static key list for non-key dependent cache services
        return keys;
    }

    @Override
    public List<RoomComponent> getComponentsByExternalCode(String externalCode) {
        if(cacheProperties.isRedisEnabled()){
            return Optional.ofNullable(redisCacheReadServiceImpl.getComponentsByExternalCode(externalCode))
                    .orElseGet(()-> getEhComponentsByExternalCode(externalCode));
        }else{
            return getEhComponentsByExternalCode(externalCode);
        }
    }
    private List<RoomComponent> getEhComponentsByExternalCode(String externalCode) {
        final Cache cache = getCache();
        final Attribute<String> externalCodeAttr = cache.getSearchAttribute("externalCode");
        final Results results = cache.createQuery().addCriteria(externalCodeAttr.eq(externalCode)).includeValues().execute();
        Optional<Results> resultsValue = Optional.ofNullable(results);
        List<RoomComponent> roomComponentList = new ArrayList<>();
        if (resultsValue.isPresent() && resultsValue.get().size() > 0) {
            log.info("External Code {} contains {} components", externalCode, resultsValue.get().size());
            List<Result> resultList = resultsValue.get().all();
            for (Result result : resultList) {
                roomComponentList.add((RoomComponent) result.getValue());
            }
            if(cacheProperties.isRedisEnabled() && CollectionUtils.isNotEmpty(roomComponentList)){
                log.info("Data found in ehCache but not in Redis for - {}", ServiceConstant.COMPONENT_STR +":"+externalCode);
            }
        }
        return roomComponentList;
    }

}
