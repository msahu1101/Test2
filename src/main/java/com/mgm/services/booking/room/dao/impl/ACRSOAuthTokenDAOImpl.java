package com.mgm.services.booking.room.dao.impl;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.ACRSOAuthTokenDAO;
import com.mgm.services.booking.room.model.request.ACRSTokenRequest;
import com.mgm.services.booking.room.model.response.ACRSAuthTokenResponse;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.util.CommonUtil;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class ACRSOAuthTokenDAOImpl implements ACRSOAuthTokenDAO {

	private URLProperties urlProperties;
	private DomainProperties domainProperties;
	private RestTemplate client;
	@Autowired
	private RetryTemplate retryTemplate;
	private AcrsProperties acrsProperties;
	private SecretsProperties secretsProperties;
	private ReferenceDataDAOHelper referenceDataDAOHelper;

	private static final String DMPWB = "DMPWB";
	private static final String WEBBE = "WEBBE";
	private static final String ICECC = "ICECC";

	static final int tokenRefreshRateInMS =1500000 ; // 1,500,000ms = 25min
	private final int MAX_RETRY = 5;
	private final int BACKOFF_DELAY = 200;

	private final Map<String, ACRSAuthTokenResponse> acrsOAuthTokenMap = new ConcurrentHashMap<>();

	public ACRSOAuthTokenDAOImpl(URLProperties urlProperties, DomainProperties domainProperties, RestTemplate client,
			AcrsProperties acrsProperties, SecretsProperties secretsProperties,
			ApplicationProperties applicationProperties, RestTemplateBuilder builder, ReferenceDataDAOHelper referenceDataDAOHelper) {

		this.urlProperties = urlProperties;
		this.domainProperties = domainProperties;
		this.client = CommonUtil.getRetryableRestTemplate(builder, applicationProperties.isSslInsecure(),
				acrsProperties.isLiveCRS(),applicationProperties.getConnectionPerRouteDaoImpl(),
				applicationProperties.getMaxConnectionPerDaoImpl(),
				applicationProperties.getConnectionTimeoutACRS(),
				applicationProperties.getReadTimeOutACRS(),
				applicationProperties.getSocketTimeOutACRS(),
				1,
				applicationProperties.getCrsRestTTL());
		this.client.setErrorHandler(new RestTemplateResponseErrorHandler());
		this.acrsProperties = acrsProperties;
		this.secretsProperties = secretsProperties;
		this.referenceDataDAOHelper = referenceDataDAOHelper;
	}
	
	/**
	 * token will expire in 30 mins. Refresh token again 5 mins. 
	 * before it expires 30-5 = 25 mins
	 */
	@Scheduled(fixedRate = tokenRefreshRateInMS)
	public void refreshToken() {
		log.info("Refreshing OAuth Token every 25 Minutes");
		if (referenceDataDAOHelper.isAcrsEnabled()) {
			ACRSAuthTokenResponse tokenResponseFromIce = generateTokenIce();
			log.debug("Printing acrs OAuth Token received for ICECC:  {}", tokenResponseFromIce);

			ACRSAuthTokenResponse tokenResponseFromWeb = generateTokenWeb();
			log.debug("Printing acrs OAuth Token received for DMPWB and WEBBE:  {}", tokenResponseFromWeb);

			// update ICE token
			acrsOAuthTokenMap.put(ICECC, tokenResponseFromIce);
			// update web tokens
			acrsOAuthTokenMap.put(DMPWB, tokenResponseFromWeb);
			acrsOAuthTokenMap.put(WEBBE, tokenResponseFromWeb);

			if (log.isDebugEnabled()) {
				log.debug("Printing acrs OAuth Token Map acrsOAuthTokenMap: {}",
						CommonUtil.convertObjectToJsonString(acrsOAuthTokenMap));
			}
		} else {
			log.debug("Skipping OAuth Token call as no property is enabled in ACRS");
		}
	}

	/**
	 * @return
	 */
	public ACRSAuthTokenResponse generateTokenWeb() {
		ACRSTokenRequest tokenRequest = new ACRSTokenRequest();
		tokenRequest.setClientSecret(secretsProperties.getSecretValue(ServiceConstant.ACRS_AUTH_CLIENT_SECRET_WEB));
		tokenRequest.setClientId(secretsProperties.getSecretValue(ServiceConstant.ACRS_AUTH_CLIENT_ID_WEB));

		tokenRequest.setGrantType(ServiceConstant.GRANT_TYPE_VALUE);

		HttpEntity<ACRSTokenRequest> request = new HttpEntity<ACRSTokenRequest>(tokenRequest);

		Map<String, String> uriParams = createURIParams();
			log.info("Sending request to ACRS to generate OAuth token - web");
			log.debug("Sending request to ACRS OAuth token - web, Request headers {}:", CommonUtil.convertObjectToJsonString(request.getHeaders()));
            log.debug("Sending request to ACRS OAuth token - web, Request body {}: ", CommonUtil.convertObjectToJsonString(request.getBody()));
           	log.debug("Sending request to ACRS OAuth token - web, Request query parameters: "+uriParams);
			final ResponseEntity<ACRSAuthTokenResponse> response = getACRSToken("web",uriParams,request);
			log.info("Received headers from acrs token API - web: {}",
					CommonUtil.convertObjectToJsonString(response.getHeaders()));
			log.debug("Received response from acrs token API - web: {}",
					CommonUtil.convertObjectToJsonString(response.getBody()));
		return response.getBody();
	}

	private ResponseEntity<ACRSAuthTokenResponse> getACRSToken(String channel, Map<String, String> uriParams, HttpEntity<ACRSTokenRequest> request){
		return retryTemplate.execute(arg -> {
			if (log.isDebugEnabled()) {
				log.debug("Sending request to ACRS to generate OAuth token - {}",channel);
				log.debug("Sending request to ACRS OAuth token - {}, Request headers {}:",channel, CommonUtil.convertObjectToJsonString(request.getHeaders()));
				log.debug("Sending request to ACRS OAuth token - {}, Request body {}: ", channel,CommonUtil.convertObjectToJsonString(request.getBody()));
				log.debug("Sending request to ACRS OAuth token - {}, Request query parameters: {} ",channel, uriParams);
			}
			ResponseEntity<ACRSAuthTokenResponse> response = client.exchange(
					domainProperties.getCrs() + urlProperties.getAcrsAuthToken(), HttpMethod.POST, request,
					ACRSAuthTokenResponse.class, uriParams);
			log.debug("Received headers from acrs token API - {}: {}", channel,
					CommonUtil.convertObjectToJsonString(response.getHeaders()));
			log.debug("Received response from acrs token API - {}: {}", channel,
					CommonUtil.convertObjectToJsonString(response.getBody()));
			return response;
		});
	}

	/**
	 * @return
	 */
	public ACRSAuthTokenResponse generateTokenIce() throws RestClientException {

		ACRSTokenRequest tokenRequest = new ACRSTokenRequest();
		tokenRequest.setClientSecret(secretsProperties.getSecretValue(ServiceConstant.ACRS_AUTH_CLIENT_SECRET_ICE));
		tokenRequest.setClientId(secretsProperties.getSecretValue(ServiceConstant.ACRS_AUTH_CLIENT_ID_ICE));

		tokenRequest.setGrantType(ServiceConstant.GRANT_TYPE_VALUE);

		HttpEntity<ACRSTokenRequest> request = new HttpEntity<ACRSTokenRequest>(tokenRequest);

		Map<String, String> uriParams = createURIParams();
			log.info("Sending request to ACRS to generate OAuth token - ice");
			log.debug("Sending request to ACRS OAuth token - ice, Request headers {}:", CommonUtil.convertObjectToJsonString(request.getHeaders()));
            log.debug("Sending request to ACRS OAuth token - ice, Request body {}: ", CommonUtil.convertObjectToJsonString(request.getBody()));
           	log.debug("Sending request to ACRS OAuth token - ice, Request query parameters: "+uriParams);

			final ResponseEntity<ACRSAuthTokenResponse> response = getACRSToken("ice",uriParams,request);


			log.info("Received headers from acrs token API -ice: {}",
					CommonUtil.convertObjectToJsonString(response.getHeaders()));
			log.debug("Received response from acrs token API -ice: {}",
					CommonUtil.convertObjectToJsonString(response.getBody()));

		return response.getBody();
	}

	private Map<String, String> createURIParams() {
		Map<String, String> uriParams = new HashMap<String, String>();
		if (StringUtils.isNotEmpty(acrsProperties.getEnvironment())) {
			uriParams.put(ServiceConstant.ACRS_ENVIRONMENT, acrsProperties.getEnvironment());
		}
		uriParams.put(ServiceConstant.ACRS_VERSION, acrsProperties.getOauth2Version());
		return uriParams;
	}

	@Override
	public Map<String, ACRSAuthTokenResponse> generateToken() {
		// if token doesn't exist, refresh it on demand
		if (acrsOAuthTokenMap.isEmpty() || acrsOAuthTokenMap.values().isEmpty()) {
			log.debug("Calling refreshToken() from generateToken().");
			refreshToken();
			log.debug("Acrs OAuth Token Map Size After Refresh: {}", acrsOAuthTokenMap.size());
		}
		return acrsOAuthTokenMap;
	}

	static class RestTemplateResponseErrorHandler implements ResponseErrorHandler {

		@Override
		public boolean hasError(ClientHttpResponse httpResponse) throws IOException {
			return httpResponse.getStatusCode().isError();
		}

		@Override
		public void handleError(ClientHttpResponse httpResponse) throws IOException {
			String response = StreamUtils.copyToString(httpResponse.getBody(), Charset.defaultCharset());
			log.error("Token error received from Amadeus: header: {} body: {}", httpResponse.getHeaders().toString(),
					response);
		}
	}

}
