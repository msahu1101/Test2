package com.mgm.services.booking.room.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Configuration class to read properties from application.properties file with
 * "eventgrid.publishevent" prefix
 *
 */
@Component
@ConfigurationProperties(
        prefix = "eventgrid.publishevent")
public @Data class EventGridProperties {

    private String topic;
    private String topicCredentialsKey;
    private boolean enabled;
    private String schemaVersion;
    private String callbackUrl;
    private String environment;
    private int retryCount;
    private long retryInterval;

}
