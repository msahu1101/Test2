package com.mgm.services.booking.room.dao.impl;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.PartnerAuthTokenDAO;
import com.mgm.services.booking.room.model.response.PartnerAuthTokenResponse;
import com.mgm.services.booking.room.model.response.TokenResponse;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.PartnerProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.util.CommonUtil;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class PartnerAuthTokenDAOImpl implements PartnerAuthTokenDAO{
	
	private DomainProperties domainProperties;
	private RestTemplate client;
	private TokenResponse tokenResponse;
	private URLProperties urlProperties;
	private ApplicationProperties applicationProperties;
	private SecretsProperties secretsProperties;
	private final int tokenRefreshRateInMS;
	private PartnerProperties partnerProperties;

	public PartnerAuthTokenDAOImpl(DomainProperties domainProperties, RestTemplateBuilder builder,
            SecretsProperties secretsProperties, ApplicationProperties applicationProperties, 
            URLProperties urlProperties, PartnerProperties partnerProperties) {
		this.client = CommonUtil.getRetryableRestTemplate(builder, true, true,
				partnerProperties.getClientMaxConnPerRoute(),
				partnerProperties.getClientMaxConn(),
				partnerProperties.getConnectionTimeOut(),
				partnerProperties.getReadTimeOut(),
				partnerProperties.getSocketTimeOut(),
				partnerProperties.getRetryCount(),
				partnerProperties.getTtl());
        this.domainProperties = domainProperties;
        this.urlProperties = urlProperties;
        this.applicationProperties = applicationProperties;
        this.secretsProperties = secretsProperties;
        this.partnerProperties = partnerProperties;
        this.client.setErrorHandler(new RestTemplateResponseErrorHandler());
		this.tokenRefreshRateInMS = partnerProperties.getTokenRefreshRateInMS();

	}
	
	@Override
	public TokenResponse generateAuthToken() {
		if (!partnerProperties.isEnabled()) {
			throw new SystemException(ErrorCode.SYSTEM_ERROR, new RuntimeException("Partner is disabled in configuration."));
		}
		// For some token doesn't exist, refresh it on demand
        if (null == tokenResponse) {
            refreshToken();
        }
        if(null == tokenResponse){
        	throw new SystemException(ErrorCode.SYSTEM_ERROR,new Throwable("Partner - Error while generating token."));
		}
        return tokenResponse;
	}

	@Scheduled(fixedRateString = "#{scheduleCalculator.calc()}")
	public void refreshToken() {
		log.info("Partner - Refresh token");
		if(partnerProperties.isEnabled()) {
			// have to retrieve from secretsProperties
			final HttpHeaders headers = CommonUtil.createPartnerAuthRequestHeader(applicationProperties.getPartnerBasicAuthUsername(),
					secretsProperties.getSecretValue(partnerProperties.getBasicAuthPasswordKey()));
			final MultiValueMap<String, String> credentialsMap = new LinkedMultiValueMap<>();
			credentialsMap.add(ServiceConstant.HEADER_GRANT_TYPE, ServiceConstant.PARTNER_GRANTTYPE_VALUE);
			credentialsMap.add(ServiceConstant.HEADER_SCOPE, ServiceConstant.PARTNER_SCOPE_VALUE);
			Map.Entry<String, String> clientRefId = CommonUtil.getPartnerClientRef();
			if(clientRefId != null) {
				headers.set(clientRefId.getKey(), clientRefId.getValue());
			}
			final HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(credentialsMap, headers);

			Map<String, String> uriParams = new HashMap<>();
			uriParams.put(ServiceConstant.PARTNER_VERSION_PARAM, applicationProperties.getPartnerVersion());
			//@TODO put in debug
			log.info("Sending request to Partner token, transactionId : {}", request.getHeaders().get(ServiceConstant.X_PARTNER_CORRELATION_ID));
			log.info("Sending request to Partner token, Request body : {}", CommonUtil.convertObjectToJsonString(request.getBody()));
			log.info("Calling Partner token API : URI {} ", urlProperties.getPartnerAccountCustomerBasicInfo());

			log.debug("Partner - Sending request to generate token");
			try {
				final ResponseEntity<TokenResponse> response = client.exchange(
						domainProperties.getPartnerAccountBasic().concat(urlProperties.getPartnerAccountAuth()),
						HttpMethod.POST,
						request,
						TokenResponse.class,
						uriParams);

				tokenResponse = response.getBody();
				log.info("Partner - token response header : {}",response.getHeaders());
				log.info("Partner - Successfully generated token. Refreshing in {} MS.", this.tokenRefreshRateInMS);
			} catch (ResourceAccessException ex) {
				log.warn("Partner - Error while refreshing token - {}", ex.getMessage());
				tokenResponse = null;
			}
		} else {
			log.debug("Partner Auth token not refreshed as partner is disabled by configuration.");
			tokenResponse = null;
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
				log.error("Token error received from Partner: header: {} body: {}", httpResponse.getHeaders().toString(),
						response);
		 }
	}

}
