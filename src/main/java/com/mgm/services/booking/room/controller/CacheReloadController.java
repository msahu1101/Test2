package com.mgm.services.booking.room.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.validation.constraints.Size;

import com.mgm.services.booking.room.properties.ApplicationProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mgm.services.booking.room.service.cache.impl.AbstractCacheService;

import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Controller to provide utility operations on ehcache elements like read, count
 * or refresh.
 *
 */
@RestController
@RequestMapping(
        value = "/v1/cache")
@Log4j2
public class CacheReloadController {

    private static final String NO_SERVICE = "No cache service found for this name";
    private static final String LINE_SEPARATOR = "line.separator";
    private static final String INITIAL_CHAR_COUNT = "200";
    private static final String REPLACE_LINE = "[\\r\\n]";

    /** The app context. */
    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private ApplicationProperties applicationProperties;

    /** The caching service beans. */
    private Map<String, AbstractCacheService> cachingServiceBeans;

    /**
     * Performs the refresh operation on requested cache.
     *
     * @param name
     *            cache name
     * @return Returns status of refresh opertion
     */
    @PostMapping("/{name}/refresh")
    public Mono<String> refreshCache(@PathVariable @Size(max = 15) String name) {
        
        final AbstractCacheService cacheService = cachingServiceBeans.get(sanitize(name));
        log.info("Refresh cache operation called for name {}", sanitize(name));

        if (null == cacheService) {
            return Mono.just(NO_SERVICE);
        }

        final StringBuilder msg = new StringBuilder(INITIAL_CHAR_COUNT);
        final String lineSeparator = System.getProperty(LINE_SEPARATOR);

        try {
            String response = null;
            if (applicationProperties.isPhoenixCacheEnabled()) {
                response = cacheService.refreshCache();
            }
            msg.append(response).append(lineSeparator);
            msg.append("Please check the logs if the # of failures is not zero.").append(lineSeparator);
            msg.append("Completed refresh operation on ").append(name).append(" cache.");
            log.info(msg.toString().replaceAll(REPLACE_LINE, StringUtils.EMPTY));

        } catch (RuntimeException e) {
            msg.append("Error performing refresh operation on ").append(name).append(" cache: ");
            log.error(msg.toString().replaceAll(REPLACE_LINE, StringUtils.EMPTY), e);
        }

        return Mono.just(msg.toString());

    }

    /**
     * Performs the read operation on requested cache and returns the results.
     * 
     * @param name
     *            Cache Name
     * @return List of items from cache
     */
    @GetMapping("/{name}/read")
    public Flux<String> readCache(@PathVariable @Size(max = 15) String name) {

        final AbstractCacheService cacheService = cachingServiceBeans.get(sanitize(name));
        log.info("Read cache operation called for name {}", sanitize(name));

        if (null == cacheService) {
            return Flux.just(NO_SERVICE);
        }

        final List<?> keys = cacheService.getCacheKeys();
        final String lineSeparator = System.getProperty(LINE_SEPARATOR);
        return Flux.create(emitter -> {
            for (final Object key : keys) {
                emitter.next("Key: " + key + ", Value: " + (cacheService).getCachedObject(key) + lineSeparator);
            }
            final String endMessage = "Completed read operation on " + name + " cache.";
            emitter.next(endMessage);
            emitter.complete();
        });

    }

    /**
     * Performs the count operation on requested cache and returns the count of
     * elements.
     * 
     * @param name
     *            Cache name
     * @return Count of items in requested cache
     */
    @GetMapping("/{name}/count")
    public Mono<String> countCache(@PathVariable @Size(max = 15) String name) {

        final AbstractCacheService cacheService = cachingServiceBeans.get(sanitize(name));
        log.info("Count cache operation called for name {}", sanitize(name));

        if (null == cacheService) {
            return Mono.just(NO_SERVICE);
        }

        final StringBuilder msg = new StringBuilder(INITIAL_CHAR_COUNT);

        try {
            final String lineSeparator = System.getProperty(LINE_SEPARATOR);
            final List<?> keys = cacheService.getCacheKeys();
            msg.append(String.format("%s cache has %s elements.%s", name, keys.size(), lineSeparator));
            msg.append(String.format("Completed count operation on %s cache", name));
            log.info(msg.toString().replaceAll(REPLACE_LINE, StringUtils.EMPTY));

        } catch (RuntimeException e) {
            msg.append("Error performing count operation on ").append(name).append(" cache: ");
            log.error(msg.toString().replaceAll(REPLACE_LINE, StringUtils.EMPTY), e);
        }

        return Mono.just(msg.toString());
    }

    /**
     * Reads and sets the caching service beans for quick access later without
     * having to read every time.
     */
    @PostConstruct
    private void setCachingServiceBeans() {
        final Map<String, AbstractCacheService> beans = appContext.getBeansOfType(AbstractCacheService.class);
        cachingServiceBeans = new HashMap<String, AbstractCacheService>();
        if (beans != null) {
            for (final AbstractCacheService cacheService : beans.values()) {
                cachingServiceBeans.put(cacheService.getCacheName().toLowerCase(), cacheService);
            }
        }
    }

    private String sanitize(String name) {
        return StringUtils.trimToEmpty(name).replaceAll("[^a-zA-Z0-9]", StringUtils.EMPTY);
    }

}
