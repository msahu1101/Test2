package com.mgm.services.booking.room.dao.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mgm.services.booking.room.dao.config.helper.OktaRequestInterceptor;
import com.mgm.services.booking.room.properties.SecretsProperties;

/**
 * Okta configuration used for OKTA feign client.
 *
 */
@Configuration
public class OktaConfiguration {

    /**
     * Creates the request interceptor by taking the secret props..
     *
     * @param secretProps secret props
     * @return request interceptor object
     */
    @Bean
    public OktaRequestInterceptor oktaRequestInterceptor(SecretsProperties secretProps) {
        return new OktaRequestInterceptor(secretProps);
    }

}
