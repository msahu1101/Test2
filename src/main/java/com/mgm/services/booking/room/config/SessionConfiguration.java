package com.mgm.services.booking.room.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.HttpSessionIdResolver;

import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.common.config.CustomWebSessionIdResolver;

/**
 * Configuration class to set application specific settings for Redis session
 * management. Header 'x-state-token' will be used as session ID resolver
 * instead of default SESSION cookie
 *
 */
@ConditionalOnProperty(
        prefix = "spring",
        name = "session.store-type",
        havingValue = "redis")
@Configuration
@EnableRedisHttpSession(
        maxInactiveIntervalInSeconds = 3600,
        redisNamespace = "booking")
public class SessionConfiguration {

    @Autowired
    private ApplicationProperties appProperties;

    /**
     * Using x-state-token as session id resolver instead of default cookie
     * resolution.
     * 
     * @return Returns CustomWebSessionIdResolver
     */
    @Bean
    public HttpSessionIdResolver webSessionIdResolver() {

        CustomWebSessionIdResolver resolver = new CustomWebSessionIdResolver(appProperties.getCookieSameSite(),
                appProperties.isCookieSecure(), appProperties.getSharedCookieDomain());
        resolver.setCookieChannels(appProperties.getCookieChannels());
        return resolver;
    }

    /**
     * Do nothing implementation for ConfigureRedisAction
     * 
     * @return Do nothing implementation for ConfigureRedisAction
     */
    @Bean
    public static ConfigureRedisAction configureRedisAction() {
        return ConfigureRedisAction.NO_OP;
    }
}
