package com.mgm.services.booking.room.dao.impl;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.TokenDAO;
import com.mgm.services.booking.room.model.request.TokenRequest;
import com.mgm.services.booking.room.model.response.TokenResponse;
import com.mgm.services.booking.room.model.response.TokenV2Response;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;

import lombok.extern.log4j.Log4j2;

/**
 * Implementation class which integrates with Okta to validate client
 * credentials and returns token response which includes access_token
 *
 */
@Component
@Log4j2
@EnableRetry
public class TokenDAOImpl implements TokenDAO {

    private URLProperties urlProperties;
    private DomainProperties domainProperties;
    private RestTemplate client;

    private SecretsProperties secretsProperties;
    private final int TIME_OUT = 1;
    private final int MAX_RETRY = 5;
    private final int BACKOFF_DELAY = 200;

    /**
     * Constructor which also injects all the dependencies. Using constructor
     * based injection since spring's auto-configured WebClient. Builder is not
     * thread-safe and need to get a new instance for each injection point.
     * 
     * @param urlProperties
     *            URL Properties
     * @param domainProperties
     *            Domain Properties
     * @param builder
     *            Spring's auto-configured rest template builder
     * @param secretsProperties
     *            Secrets Properties
     */
    public TokenDAOImpl(URLProperties urlProperties, DomainProperties domainProperties, RestTemplateBuilder builder,
            SecretsProperties secretsProperties) {
        this.urlProperties = urlProperties;
        this.client = builder.setConnectTimeout(Duration.ofSeconds(TIME_OUT)).build();
        this.client.setErrorHandler(new RestTemplateResponseErrorHandler());
        this.domainProperties = domainProperties;
        this.secretsProperties = secretsProperties;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.TokenDAO#generateToken(com.mgm.services
     * .booking.room.model.request.TokenRequest)
     */
    @Override
    @Retryable( value = IOException.class, 
    maxAttempts = MAX_RETRY, backoff = @Backoff(delay = BACKOFF_DELAY))
    public TokenResponse generateToken(TokenRequest tokenRequest) {

        HttpHeaders headers = new HttpHeaders();
        headers.putAll(
                CommonUtil.createAuthorizationHeader(tokenRequest.getClientId(), tokenRequest.getClientSecret()));

        HttpEntity<?> request = new HttpEntity<>(headers);

        log.info("Sending request to Okta to generate token");

        TokenResponse response = client.postForEntity(domainProperties.getOkta().concat(urlProperties.getOktaToken()),
                request, TokenResponse.class).getBody();

        log.info("Received response from Okta for generate token: {}", response);

        return response;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.TokenDAO#validateOktaSession(java.lang.
     * String)
     */
    @Override
    @Retryable( value = IOException.class, 
    maxAttempts = MAX_RETRY, backoff = @Backoff(delay = BACKOFF_DELAY))
    public TokenV2Response validateOktaSession(String oktaSessionId) {

        HttpHeaders headers = new HttpHeaders();
        headers.putAll(CommonUtil.createOktaSessionAuthorizationHeader(
                secretsProperties.getSecretValue(ServiceConstant.OKTA_API_TOKEN)));

        HttpEntity<?> request = new HttpEntity<>(headers);

        log.info("Sending request to Okta to validation okta session");

        TokenV2Response response = client.exchange(
                domainProperties.getOkta()
                        .concat(urlProperties.getOktaSessionValidation().trim().concat(oktaSessionId)),
                HttpMethod.GET, request, TokenV2Response.class).getBody();

        log.info("Received response from Okta for validate okta session: {}", response);

        return response;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.TokenDAO#fetchUserDetails(java.lang.
     * String)
     */
    @Override
    @Retryable( value = IOException.class, 
    maxAttempts = MAX_RETRY, backoff = @Backoff(delay = BACKOFF_DELAY))
    public TokenV2Response fetchUserDetails(String emailId) {

        HttpHeaders headers = new HttpHeaders();
        headers.putAll(CommonUtil.createOktaSessionAuthorizationHeader(
                secretsProperties.getSecretValue(ServiceConstant.OKTA_API_TOKEN)));

        HttpEntity<?> request = new HttpEntity<>(headers);

        log.info("Sending request to Okta to fetchUserDetails");

        TokenV2Response response = client
                .exchange(domainProperties.getOkta().concat(urlProperties.getOktaUserDetails().trim().concat(emailId)),
                        HttpMethod.GET, request, TokenV2Response.class)
                .getBody();

        log.info("Received response from Okta for fetchUserDetails: {}", response);

        return response;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.TokenDAO#validateOktaAccessToken(java.
     * lang.String)
     */
    @Override
    @Retryable( value = IOException.class, 
    maxAttempts = MAX_RETRY, backoff = @Backoff(delay = BACKOFF_DELAY))
    public TokenV2Response validateOktaAccessToken(String accessToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.putAll(CommonUtil.createAccessTokenAuthorizationHeader(accessToken));

        HttpEntity<?> request = new HttpEntity<>(headers);

        log.info("Sending request to Okta to validateOktaAccessToken");

        TokenV2Response response = client
                .postForEntity(domainProperties.getOkta().concat(urlProperties.getOktaAccessTokenValidation().trim()),
                        request, TokenV2Response.class)
                .getBody();

        log.info("Received response from Okta for validateOktaAccessToken: {}", response);

        return response;

    }

    class RestTemplateResponseErrorHandler implements ResponseErrorHandler {

        @Override
        public boolean hasError(ClientHttpResponse httpResponse) throws IOException {

            return httpResponse.getStatusCode().isError();
        }

        @Override
        public void handleError(URI url, HttpMethod method, ClientHttpResponse httpResponse) throws IOException {
            handleError(httpResponse);

            log.info(url.getPath());
            log.info(urlProperties.getOktaToken());
            if (url.getPath().contains(urlProperties.getOktaSessionValidation())) {
                throw new BusinessException(ErrorCode.INVALID_USER_SESSION);
            } else if (url.getPath().contains(urlProperties.getOktaUserDetails())) {
                throw new BusinessException(ErrorCode.INVALID_USER_INFO);
            } else if (url.getPath().contains(urlProperties.getOktaAccessTokenValidation())) {
                throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);
            } else {
                throw new BusinessException(ErrorCode.INVALID_CLIENT_CREDENTIALS);
            }
        }

        @Override
        public void handleError(ClientHttpResponse httpResponse) throws IOException {

            log.error("Response from error: {}", httpResponse.getBody());

        }
    }

}
