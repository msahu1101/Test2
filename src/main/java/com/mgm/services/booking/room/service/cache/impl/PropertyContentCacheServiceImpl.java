package com.mgm.services.booking.room.service.cache.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.service.cache.rediscache.service.PropertyContentRedisCacheService;
import com.mgm.services.booking.room.service.cache.rediscache.service.impl.RedisCacheReadServiceImpl;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.PropertyContentDAO;
import com.mgm.services.booking.room.model.content.Property;
import com.mgm.services.booking.room.properties.CacheProperties;
import com.mgm.services.booking.room.service.cache.PropertyContentCacheService;

import lombok.extern.log4j.Log4j2;
import net.sf.ehcache.Cache;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Query;
import net.sf.ehcache.search.Result;
import net.sf.ehcache.search.Results;

/**
 * Implementation class for PropertyContentCacheService providing functions for
 * caching and returning property content cache.
 */
@Component
@Log4j2
public class PropertyContentCacheServiceImpl extends AbstractCacheService implements PropertyContentCacheService {

    private static final String CACHE_NAME = "propertyContent";

    @Autowired
    private CacheProperties cacheProperties;

    @Autowired
    private PropertyContentDAO propertyDao;

    @Autowired
    private PropertyContentRedisCacheService redisCacheReadServiceImpl;
    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.service.cache.PropertyContentCacheService#
     * getProperty(java.lang.String)
     */
    @Override
    public Property getProperty(String propertyId) {
        if(cacheProperties.isRedisEnabled()){
            return Optional.ofNullable(redisCacheReadServiceImpl.getProperty(propertyId))
                    .orElseGet(()-> (Property) getCachedObject(propertyId));
        }else {
            return (Property) getCachedObject(propertyId);
        }
    }
    
    /*
     * (non-Javadoc)
     * @see com.mgm.services.booking.room.service.cache.PropertyContentCacheService#getPropertyByRegion(boolean)
     */
    @Override
    public List<Property> getPropertyByRegion(String region) {
        if(cacheProperties.isRedisEnabled()){
            return Optional.ofNullable(redisCacheReadServiceImpl.getPropertyByRegion(region))
                    .orElseGet(()->getEhPropertyByRegion(region));
        }else{
            return getEhPropertyByRegion(region);
        }
    }
    private List<Property> getEhPropertyByRegion(String region) {
        final Cache cache = getCache();
        final Attribute<String> regionAttr = cache.getSearchAttribute("region");
        Query query = null;
        
        if (StringUtils.isEmpty(region) || region.equalsIgnoreCase("all")) {
            // return offers across all regions
            query = cache.createQuery();
        } else if (region.equalsIgnoreCase("LV")) {
            // return only LV resorts offers
            query = cache.createQuery().addCriteria(regionAttr.eq("LV"));
        } else if (region.equalsIgnoreCase("NONLV")) {
            // return only non-LV resorts offers
            query = cache.createQuery().addCriteria(regionAttr.ne("LV"));
        }
        
        final Results results = query.includeValues().execute();
        Optional<Results> resultsValue = Optional.ofNullable(results);
        List<Property> propertyList = new ArrayList<>();
        if (resultsValue.isPresent() && resultsValue.get().size() > 0) {
            List<Result> resultList = resultsValue.get().all();

            resultList.forEach(result -> propertyList.add((Property) result.getValue()));

        }
        if(cacheProperties.isRedisEnabled() && CollectionUtils.isNotEmpty(propertyList)){
            log.info("Data found in ehCache but not in Redis for - {}", ServiceConstant.REGION_STR +":"+region);
        }
        return propertyList;
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
        Map<Object, Object> propertyMap = new HashMap<>();
        try {
            // using some block statements since this is background process
            // anyway
            log.info("[EhCacheLogger]|'{}' Cache -> Fetching Properites content", getCacheName());
            List<Property> propertyList = propertyDao.getAllPropertiesContent();
            log.info("[EhCacheLogger]|'{}' Cache -> Successfully fetched Properties content", getCacheName());

            for (Property property : propertyList) {
                propertyMap.put(property.getId(), property);
            }
        } catch (Exception e) {
            failureCount++;
            log.error("[EhCacheLogger]|'{}' Cache -> Exception while retrieving properties content from AEM",
                    getCacheName(), e);
        }
        return propertyMap;
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
        return cacheProperties.getPropertyContentRefreshFreqInSecs();
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

}
