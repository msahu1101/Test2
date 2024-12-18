package com.mgm.services.booking.room.service.cache.rediscache.redis;

import com.mgm.services.booking.room.properties.CacheProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

@Log4j2
@Component
public @Data class JedisFactory {
    CacheProperties cacheProperties;
    SecretsProperties secretProperties;
    private static JedisPool jedisPool;
    @Autowired
    public void JedisFactory(CacheProperties cacheProperties, SecretsProperties secretProperties) {
        this.cacheProperties = cacheProperties;
        this.secretProperties = secretProperties;
        log.info("RedisEnabled::{}",cacheProperties.isRedisEnabled());
        if(null == jedisPool && cacheProperties.isRedisEnabled()) {
            jedisPool = createJedisPool();
        }
    }

    private JedisPoolConfig buildPoolConfig() {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(cacheProperties.getMaxConnection());
        poolConfig.setMaxIdle(cacheProperties.getMaxIdle());
        poolConfig.setMinIdle(cacheProperties.getMinIdle());
        //Controls whether or not the connection is tested before it is returned from the pool
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        //Set to true to periodically validate idle connections
        poolConfig.setTestWhileIdle(true);
        //waiting time to get resource in case max pool size reached out
        poolConfig.setMaxWaitMillis(cacheProperties.getMaxWaitMillis());
        poolConfig.setNumTestsPerEvictionRun(3);
        // Block once max connection reached. for production keep this true
        poolConfig.setBlockWhenExhausted(cacheProperties.isBlockWhenExhausted());
        return poolConfig;
    }

    private JedisPool getJedisPool() {
        if(null == jedisPool){
            jedisPool = createJedisPool();
        }
        return jedisPool;
    }

    public Jedis getJedis(){
        try{
            return getJedisPool().getResource();
        }catch (JedisConnectionException ex){
            log.error("Error while getting jedis from the pool-{}",ex.getMessage());
            log.info("Closing the existing jedis pool and retrying to re create.");
            jedisPool.close();
            jedisPool = createJedisPool();
        }
        return jedisPool.getResource();
    }

    private JedisPool createJedisPool(){
        JedisPool pool = null;
        log.info("Connecting to jedis pool for {}",cacheProperties.getUrl());
        JedisPoolConfig poolConfig = buildPoolConfig();
        try {
            String accessKey = secretProperties.getSecretValue(cacheProperties.getAccessKeyName());
            pool = new JedisPool(poolConfig,
                    cacheProperties.getUrl(),
                    cacheProperties.getPort(),
                    cacheProperties.getRedisTimeOut(),
                    accessKey,
                    cacheProperties.isSsl());

        } catch (Exception jex) {
            log.error("Error while creating redis connection pool-{}", jex.getMessage());
        }
        return pool;
    }

}
