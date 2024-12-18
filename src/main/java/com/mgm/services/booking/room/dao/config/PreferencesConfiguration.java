package com.mgm.services.booking.room.dao.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mgm.services.booking.room.dao.config.helper.PreferencesRequestInterceptor;
import com.mgm.services.booking.room.properties.SecretsProperties;

/**
 * Preferences configuration used for Preferences feign client.
 *
 */
@Configuration
public class PreferencesConfiguration {

    /**
     * Creates the request interceptor by taking the secret props..
     *
     * @param secretProps secret props
     * @return request interceptor object
     */
    @Bean
    public PreferencesRequestInterceptor preferencesRequestInterceptor(SecretsProperties secretProps) {
        return new PreferencesRequestInterceptor(secretProps);
    }

}
