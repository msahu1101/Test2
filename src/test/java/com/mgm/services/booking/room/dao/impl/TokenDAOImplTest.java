package com.mgm.services.booking.room.dao.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import javax.net.ssl.SSLException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.request.TokenRequest;
import com.mgm.services.booking.room.model.response.TokenResponse;
import com.mgm.services.booking.room.model.response.TokenV2Response;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.util.CommonUtil;

@RunWith(MockitoJUnitRunner.class)
public class TokenDAOImplTest extends BaseRoomBookingTest{

	@Mock
	private static DomainProperties domainProperties;
	@Mock
	private static RestTemplate client;
	@Mock
	private static URLProperties urlProperties;
	@Mock
	private static SecretsProperties secretsProperties;
	@Mock
	private static RestTemplateBuilder restTemplateBuilder;
	private static HttpHeaders headers;



	@InjectMocks
	private static TokenDAOImpl tokenDAOImpl;
	static Logger logger = LoggerFactory.getLogger(TokenDAOImplTest.class);

	@BeforeClass
	public static void init() throws SSLException {
		client = Mockito.mock(RestTemplate.class);
		domainProperties = Mockito.mock(DomainProperties.class);
		secretsProperties = Mockito.mock(SecretsProperties.class);
		urlProperties = Mockito.mock(URLProperties.class);
		restTemplateBuilder = Mockito.mock(RestTemplateBuilder.class);
		CommonUtil commonUtil = Mockito.spy(CommonUtil.class);

		when(restTemplateBuilder.setConnectTimeout(Mockito.any())).thenReturn(restTemplateBuilder);
		when(restTemplateBuilder.build()).thenReturn(client);

		tokenDAOImpl = new TokenDAOImpl(urlProperties,domainProperties,restTemplateBuilder,secretsProperties);

		MockHttpServletRequest request = new MockHttpServletRequest();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
	}

	@Test
	public void generateTokenTest() {
		TokenRequest tokenRequest = new TokenRequest();
		TokenResponse tokenResponse = new TokenResponse();
		tokenResponse.setAccessToken("accessToken");

		when(domainProperties.getOkta()).thenReturn("okta");
		when(urlProperties.getOktaToken()).thenReturn("accessToken");

		headers = new HttpHeaders();
		headers.add(ServiceConstant.HEADER_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER + "23412345");
		headers.add(ServiceConstant.HEADER_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_APPLICATION_JSON);

		String clientId ="room_booking_service";
		String clientSecret ="NotARealClientSecretBecauseThisIsAUnitTest";
		tokenRequest.setClientId(clientId);
		tokenRequest.setClientSecret(clientSecret);
		ResponseEntity<TokenResponse> entity =ResponseEntity.ok(tokenResponse);
		when(client.postForEntity(anyString(),any(HttpEntity.class),eq(TokenResponse.class))).thenReturn((entity));

		TokenResponse response = tokenDAOImpl.generateToken(tokenRequest);
		assertNotNull(response);
		assertEquals("accessToken",response.getAccessToken());
	}

	@Test
	public void validateOktaSessionTest() {
		String oktaSessionId = "Bearer";
		String oktaUrl = "sampleUrl";
		String oktasessionValidationUrl ="sampleOktaSessionUrl";
		TokenV2Response tokenV2Response = new TokenV2Response();
		tokenV2Response.setAccessToken("accessToken");

		headers = new HttpHeaders();
		headers.add(ServiceConstant.OKTA_API_TOKEN, ServiceConstant.HEADER_OKTA_ACCESS_TOKEN );

		when(domainProperties.getOkta()).thenReturn(oktaUrl);
		when(urlProperties.getOktaSessionValidation()).thenReturn(oktasessionValidationUrl);
		when(secretsProperties.getSecretValue(any())).thenReturn("");

		ResponseEntity<TokenV2Response> entity = ResponseEntity.ok(tokenV2Response);
		when(client.exchange(anyString(),eq(HttpMethod.GET),any(),eq(TokenV2Response.class))).thenReturn(entity);

		TokenV2Response response = tokenDAOImpl.validateOktaSession(oktaSessionId);
		assertNotNull(response);
        assertEquals("accessToken",response.getAccessToken());
	}

	@Test
	public void fetchUserDetailsTest() {
		String emailId = "bewoju@afia.pro";
		TokenV2Response tokenV2Response = new TokenV2Response();
		tokenV2Response.setAccessToken("accessToken");
		headers = new HttpHeaders();
		headers.add(ServiceConstant.OKTA_API_TOKEN, ServiceConstant.HEADER_OKTA_ACCESS_TOKEN );

		when(domainProperties.getOkta()).thenReturn("");
		when(urlProperties.getOktaUserDetails()).thenReturn("");
		when(secretsProperties.getSecretValue(any())).thenReturn("");

		ResponseEntity<TokenV2Response> entity = ResponseEntity.ok(tokenV2Response);
		when(client.exchange(anyString(),eq(HttpMethod.GET),any(),eq(TokenV2Response.class))).thenReturn(entity);

		TokenV2Response response = tokenDAOImpl.fetchUserDetails(emailId);
		assertNotNull(response);
		assertEquals("accessToken",response.getAccessToken());
	}

	@Test
	public void validateOktaAccessTokenTest() {
		String accessToken ="accessToken";
		TokenV2Response tokenV2Response = new TokenV2Response();
		tokenV2Response.setAccessToken("accessToken");

		headers = new HttpHeaders();
		headers.add(ServiceConstant.HEADER_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER + "23412345");
		headers.add(ServiceConstant.HEADER_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_APPLICATION_JSON);
		headers.add(accessToken, accessToken);
		when(domainProperties.getOkta()).thenReturn("");
		when(urlProperties.getOktaAccessTokenValidation()).thenReturn("");

		ResponseEntity<TokenV2Response> entity = ResponseEntity.ok(tokenV2Response);
		when(client.postForEntity(anyString(),any(HttpEntity.class),eq(TokenV2Response.class))).thenReturn((entity));
		TokenV2Response response = tokenDAOImpl.validateOktaAccessToken(accessToken);
		assertNotNull(response);
		assertEquals("accessToken",response.getAccessToken());
	}
}
