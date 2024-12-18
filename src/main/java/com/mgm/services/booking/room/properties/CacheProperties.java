package com.mgm.services.booking.room.properties;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

import javax.annotation.PostConstruct;

/**
 * Configuration class to read cache related properties from
 * application.properties file with "cache" prefix
 */
@Component
@ConfigurationProperties(
        prefix = "cache")
@Log4j2
public @Data class CacheProperties {

    private int coreThreadPoolSize;
    private int roomProgramRefreshFreqInSecs;
    private int roomRefreshFreqInSecs;
    private int propertyRefreshFreqInSecs;
    private int propertyContentRefreshFreqInSecs;
    private int emailRefreshFreqInSecs;
    private int globalTimeoutInSecs;
    private int signupEmailRefreshFreqInSecs;
    private int roomComponentsRefreshFreqInSecs;
    private boolean redisEnabled;
    private String accessKeyName;
    private String url;
    private int port;
    private int redisTimeOut;
    private boolean ssl;
    private int maxConnection;
    private int maxIdle;
    private int minIdle;
    private long maxWaitMillis;
    private boolean blockWhenExhausted;

    @PostConstruct
    private void init(){
        String enabledStr = System.getenv("redisEnabled");
        log.info("RedisEnabled in config properties is {}",redisEnabled);
        log.info("RedisEnabled in system config is {}",enabledStr);
        if(StringUtils.isNotEmpty(enabledStr)){
                this.setRedisEnabled(Boolean.valueOf(enabledStr));
        }
    }


}
