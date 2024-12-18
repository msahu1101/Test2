/**
 * 
 */
package com.mgm.services.booking.room.dao.config.helper;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.properties.SecretsProperties;

import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * Custom request interceptor for Okta feign client.
 *
 */
public class OktaRequestInterceptor implements RequestInterceptor {

    private String oktaApiToken;

    /**
     * Creates request interceptor object with the given secret props.

     * @param secretProps secret props
     */
    public OktaRequestInterceptor(SecretsProperties secretProps) {
        oktaApiToken = secretProps.getSecretValue("okta-api-token");
    }

    @Override
    public void apply(RequestTemplate template) {

        template.header(ServiceConstant.HEADER_AUTHORIZATION, "SSWS " + oktaApiToken);
    }

}
