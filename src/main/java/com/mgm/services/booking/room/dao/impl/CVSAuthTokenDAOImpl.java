package com.mgm.services.booking.room.dao.impl;

import java.nio.charset.Charset;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAwsCloudEnvironment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.CVSAuthTokenDAO;
import com.mgm.services.booking.room.model.response.IDMSTokenResponse;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.util.CommonUtil;

import lombok.Data;
import lombok.extern.log4j.Log4j2;

/**
 * DAO class to generate an IDMS token when needed
 *
 */
@Component
@Log4j2
@ConditionalOnMissingAwsCloudEnvironment
public class CVSAuthTokenDAOImpl implements CVSAuthTokenDAO {

    private DomainProperties domainProperties;
    private RestTemplate client;
    private SecretsProperties secretsProperties;

    private CVSToken token;
    private String tokenScope;

    private static final long TIMESTAMP_MULTIPLIER = 1000;
    private static final long EXPIRATION_BUFFER_TIME_SEC = 1;

    @Data
    public static class CVSToken extends IDMSTokenResponse {
        private long createdTime = System.currentTimeMillis();
    }

    public CVSAuthTokenDAOImpl(DomainProperties domainProperties, RestTemplateBuilder builder, SecretsProperties secretsProperties,
                               ApplicationProperties applicationProperties, AcrsProperties acrsProperties,
                               @Value("#{'${cvs.token.scope}'}") String tokenScope) {

		/*
		 * this.client = CommonUtil.getRestTemplate(builder, true, true);
		 * this.domainProperties = domainProperties; this.secretsProperties =
		 * secretsProperties; this.tokenScope = tokenScope;
		 * 
		 * log.info("CVS Auth - Creating service tokens on startup"); Map<String,
		 * String> env = System.getenv();
		 * log.info("CVS Auth - Environment Variables: {}", env);
		 * 
		 * // generate initial service token on startup //refreshToken();
		 */
    }

    private void refreshToken() {

		/*
		 * final HttpHeaders headers = CommonUtil.createIdmsRequestHeader(); final
		 * String auth = String.format("%s:%s",
		 * secretsProperties.getSecretValue(ServiceConstant.CVS_CLIENT_ID),
		 * secretsProperties.getSecretValue(ServiceConstant.CVS_CLIENT_SECRET));
		 * headers.set(ServiceConstant.HEADER_KEY_AUTHORIZATION,
		 * ServiceConstant.HEADER_AUTH_BASIC + new
		 * String(Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII")))));
		 * 
		 * // have to retrieve from secretsProperties final MultiValueMap<String,
		 * String> credentialsMap = new LinkedMultiValueMap<>();
		 * credentialsMap.add(ServiceConstant.HEADER_GRANT_TYPE,
		 * ServiceConstant.GRANT_TYPE_VALUE);
		 * credentialsMap.add(ServiceConstant.HEADER_SCOPE, tokenScope); final
		 * HttpEntity<MultiValueMap<String, String>> request = new
		 * HttpEntity<>(credentialsMap, headers);
		 * 
		 * log.info("CVS Auth - Sending request to generate token {}", request); final
		 * ResponseEntity<CVSToken> response =
		 * client.exchange(domainProperties.getCvsIdentity(), HttpMethod.POST, request,
		 * CVSToken.class);
		 * log.info("CVS Auth - Received response of generate token: {}",
		 * response.getBody()); token = response.getBody();
		 */
    }

    @Override
    public synchronized CVSToken generateToken(boolean forceStale) {
        // For some token doesn't exist, refresh it on demand
		/*
		 * if (forceStale || hasExpired(token)) {
		 * log.info("CVS Auth - Creating service tokens on demand"); refreshToken(); }
		 */
        return token;
    }

	/*
	 * private boolean hasExpired(CVSToken token) { if (token == null) { return
	 * true; }
	 * 
	 * int expiresIn = token.getExpiresIn(); if ((System.currentTimeMillis() -
	 * token.getCreatedTime()) > ((expiresIn - EXPIRATION_BUFFER_TIME_SEC) *
	 * TIMESTAMP_MULTIPLIER)) { return true; } return false; }
	 */

}