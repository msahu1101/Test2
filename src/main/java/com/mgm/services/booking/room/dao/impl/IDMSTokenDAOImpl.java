package com.mgm.services.booking.room.dao.impl;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.mgm.services.booking.room.exception.ACRSErrorDetails;
import com.mgm.services.booking.room.exception.ACRSErrorUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.model.response.IDMSTokenResponse;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.util.CommonUtil;

import lombok.extern.log4j.Log4j2;

/**
 * DAO class to generate an IDMS token when needed
 *
 */
@Component
@Log4j2
public class IDMSTokenDAOImpl implements IDMSTokenDAO {

    private DomainProperties domainProperties;
    private RestTemplate client;
    private SecretsProperties secretsProperties;

    private IDMSTokenResponse tokenResponse;
    private String tokenScope;

    private static final long TIMESTAMP_MULTIPLIER = 1000;
    
    private static boolean isRefreshIntervalSet = false;

    /**
     * IDMSTokenDAOImpl constructor
     * 
     * @param domainProperties
     *            the domain properties
     * @param builder
     *            the RestTemplate builder
     * @param secretsProperties
     *            the secret Properties
     * @param applicationProperties
     *            the application properties
     * @param tokenScope
     *            value of 'idms.token.scope' property and replacing , (comma)
     *            with space
     */
    public IDMSTokenDAOImpl(DomainProperties domainProperties, RestTemplateBuilder builder,
            SecretsProperties secretsProperties, ApplicationProperties applicationProperties,
            @Value("#{'${idms.token.scope}'.replace(',',' ')}") String tokenScope) {
        this.client = CommonUtil.getRetryableRestTemplate(builder, applicationProperties.isSslInsecure(), true,
                applicationProperties.getConnectionPerRouteDaoImpl(),
                applicationProperties.getMaxConnectionPerDaoImpl(),
                applicationProperties.getConnectionTimeout(),
                applicationProperties.getReadTimeOut(),
                applicationProperties.getSocketTimeOut(),
                1,
                applicationProperties.getCommonRestTTL());
        this.domainProperties = domainProperties;
        this.secretsProperties = secretsProperties;
        this.tokenScope = tokenScope;
        this.client.setErrorHandler(new RestTemplateResponseErrorHandler());
        log.info("IDMS - Creating service tokens on startup");
        Map<String, String> env = System.getenv();
        log.info("IDMS - Environment Variables: {}", env);
        // generate initial service token on startup
        refreshToken();

        // Refresh token again 10 mins before it expires
        if(tokenResponse!=null && !isRefreshIntervalSet) {
        	long val = (tokenResponse.getExpiresIn() - 600) * TIMESTAMP_MULTIPLIER;
        	new Timer().schedule(new TokenRefreshTask(), val, val);
        	isRefreshIntervalSet = true;
        	log.info("IDMS - isRefreshIntervalSet to True");
        } else if(isRefreshIntervalSet) {
        	log.info("IDMS - Refresh Timer has been set");
        }
    }

    private void refreshToken() {

        final HttpHeaders headers = CommonUtil.createIdmsRequestHeader();

        // have to retrieve from secretsProperties
        final MultiValueMap<String, String> credentialsMap = new LinkedMultiValueMap<>();
        credentialsMap.add(ServiceConstant.HEADER_CLIENT_ID,
                secretsProperties.getSecretValue(ServiceConstant.IDMS_CLIENT_ID));
        credentialsMap.add(ServiceConstant.HEADER_CLIENT_SECRET,
                secretsProperties.getSecretValue(ServiceConstant.IDMS_CLIENT_SECRET));
        credentialsMap.add(ServiceConstant.HEADER_GRANT_TYPE, ServiceConstant.GRANT_TYPE_VALUE);
        credentialsMap.add(ServiceConstant.HEADER_SCOPE, tokenScope);

        final HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(credentialsMap, headers);

        log.info("IDMS - Sending request to IDMS to generate token");
        try{
            final ResponseEntity<IDMSTokenResponse> response = client.exchange(domainProperties.getIdms(), HttpMethod.POST,
                    request, IDMSTokenResponse.class);

            log.info("IDMS - Received response from IDMS for generate token: {}", response);
            tokenResponse = response.getBody();
            if(tokenResponse!=null && !isRefreshIntervalSet) {
            	log.info("IDMS - Refresh interval was not set, setting now");
            	long val = (tokenResponse.getExpiresIn() - 600) * TIMESTAMP_MULTIPLIER;
            	new Timer().schedule(new TokenRefreshTask(), val, val);
            	isRefreshIntervalSet = true;
            }
        }catch (Exception ex) {
            log.error("IDMS - Error while refreshing token-{}", ex.getMessage());
        }
    }

    @Override
    public synchronized IDMSTokenResponse generateToken() {

        // For some token doesn't exist, refresh it on demand
        if (null == tokenResponse) {
            log.info("IDMS - Creating service tokens on demand");
            refreshToken();
        }
        if(null == tokenResponse){
            throw new SystemException(ErrorCode.SYSTEM_ERROR,new Throwable("IDMS - Error while generating token."));
        }
        return tokenResponse;
    }

    public class TokenRefreshTask extends TimerTask {

        @Override
        public void run() {
            refreshToken();
        }

    }

    static class RestTemplateResponseErrorHandler implements ResponseErrorHandler {

        @Override
        public boolean hasError(ClientHttpResponse httpResponse) throws IOException {
            return httpResponse.getStatusCode().isError();
        }

        @Override
        public void handleError(ClientHttpResponse httpResponse) throws IOException {
            String response = StreamUtils.copyToString(httpResponse.getBody(), Charset.defaultCharset());
            log.error("Error received IDMSToken: header: {} body: {}", httpResponse.getHeaders().toString(), response);
            ThreadContext.put(ServiceConstant.HTTP_STATUS_CODE, String.valueOf(httpResponse.getStatusCode()));
            try {
                LocalDateTime start = LocalDateTime.parse(ThreadContext.get(ServiceConstant.TIME_TYPE));
                long duration = ChronoUnit.MILLIS.between(start, LocalDateTime.now());
                ThreadContext.put(ServiceConstant.DURATION_TYPE, String.valueOf(duration));
                log.info("Custom Dimensions updated after IDMSToken call");
            } catch (Exception e) {
                // Do nothing
            }

        }
    }


}