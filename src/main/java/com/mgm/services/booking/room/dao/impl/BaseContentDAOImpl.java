package com.mgm.services.booking.room.dao.impl;

import java.io.IOException;
import java.nio.charset.Charset;

import com.mgm.services.booking.room.util.CommonUtil;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;

import lombok.extern.log4j.Log4j2;

/**
 * Base implementation class for ContentDAO to fetch marketing content.
 */
@Component
@Log4j2
public class BaseContentDAOImpl {

    private URLProperties urlProperties;

    private DomainProperties domainProperties;

    private RestTemplate client;

    private ApplicationProperties applicationProperties;

    /**
     * Constructor which also injects all the dependencies. Using constructor
     * based injection since spring's auto-configured WebClient. Builder is not
     * thread-safe and need to get a new instance for each injection point.
     * 
     * @param urlProperties
     *            URL Properties
     * @param domainProperties
     *            Domain Properties
     * @param clientHttpRequestFactory
     *            Customer client factory
     * @param appProps
     *            Application Properties
     */
    public BaseContentDAOImpl(URLProperties urlProperties, DomainProperties domainProperties,ApplicationProperties appProps) {
        this.urlProperties = urlProperties;
        this.domainProperties = domainProperties;
        this.client = new RestTemplate( new BufferingClientHttpRequestFactory(
                CommonUtil.clientHttpRequestFactory(appProps.isSslInsecure(),
                        appProps.getContentMaxConnectionPerDaoImpl(),
                        appProps.getConnectionPerRouteDaoImpl(),
                        appProps.getConnectionTimeoutContent(),
                        appProps.getReadTimeOutContent(),
                        appProps.getSocketTimeOutContent(),
                        1,
                        appProps.getContentRestTTL(),false))
        );
        this.client.setErrorHandler(new RestTemplateResponseErrorHandler());
        this.applicationProperties = appProps;

    }

    protected RestTemplate getClient() {
        return client;
    }

    protected URLProperties getUrlProperties() {
        return urlProperties;
    }

    public DomainProperties getDomainProperties() {
        return domainProperties;
    }

    public void setDomainProperties(DomainProperties domainProperties) {
        this.domainProperties = domainProperties;
    }

    public ApplicationProperties getApplicationProperties() {
        return applicationProperties;
    }

    public void setApplicationProperties(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    static class RestTemplateResponseErrorHandler implements ResponseErrorHandler {

        @Override
        public boolean hasError(ClientHttpResponse httpResponse) throws IOException {
            return httpResponse.getStatusCode().isError();
        }

        @Override
        public void handleError(ClientHttpResponse httpResponse) throws IOException {
            String response = StreamUtils.copyToString(httpResponse.getBody(), Charset.defaultCharset());
            log.error("Error received from content - Status: {}, Body: {}", httpResponse.getRawStatusCode(), response);

            if(httpResponse.getStatusCode().is4xxClientError()) {
                // no handle, suppressing it
            } else {
                throw new SystemException(ErrorCode.CONTENT_ERROR, null);
            }

        }
    }
}
