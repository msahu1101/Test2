/**
 * 
 */
package com.mgm.services.booking.room.dao.config.helper;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.properties.SecretsProperties;

import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * Custom request interceptor for preferences feign client.
 *
 */
public class PreferencesRequestInterceptor implements RequestInterceptor {

    private String preferencesApiToken;

    /**
     * Creates request interceptor object with the given secret props.

     * @param secretProps secret props
     */
    public PreferencesRequestInterceptor(SecretsProperties secretProps) {
        preferencesApiToken = secretProps.getSecretValue("preferences-key");
    }

    @Override
    public void apply(RequestTemplate template) {
        template.header(ServiceConstant.HEADER_OCP_APIM_SUBSCRIPTION_KEY, preferencesApiToken);
    }

}
