package com.mgm.services.booking.room.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Configuration class to read tibco related properties from
 * application.properties file with "tbm" prefix
 */
@Component
@ConfigurationProperties(
        prefix = "myvegas.jms")
public @Data class JMSProperties {

    private String serverUrl;
    private int sessionCacheSize;
    private boolean sessionTransacted;
    private int topicReadTimeOut;
    private String jndiName;
    private String initialContextFactory;

}
