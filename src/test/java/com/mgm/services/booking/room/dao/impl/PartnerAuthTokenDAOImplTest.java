package com.mgm.services.booking.room.dao.impl;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.SSLException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.response.TokenResponse;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.PartnerProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.util.CommonUtil;

@RunWith(MockitoJUnitRunner.class)
public class PartnerAuthTokenDAOImplTest extends BaseRoomBookingTest {

	@Mock
	private static DomainProperties domainProperties; 

	@Mock
	private static RestTemplate client;

	@Mock
	private TokenResponse tokenResponse;

	@Mock
	private static URLProperties urlProperties;

	@Mock
	private static ApplicationProperties applicationProperties;

	@Mock
	private static SecretsProperties secretsProperties;

	@Mock
	private static PartnerProperties partnerProperties;
	private static RestTemplateBuilder builder;


	@InjectMocks
	private static PartnerAuthTokenDAOImpl partnerAuthTokenDAOImpl;
	private static HttpHeaders headers;
	private static Map<String, String> azureKeyVaultSecrets = new HashMap<>();

	static Logger logger = LoggerFactory.getLogger(PartnerAuthTokenDAOImplTest.class);


	@BeforeClass
	public static void init() throws SSLException, ParseException {
		client = Mockito.mock(RestTemplate.class);
		builder = Mockito.mock(RestTemplateBuilder.class);
		applicationProperties = Mockito.mock(ApplicationProperties.class);
		secretsProperties = Mockito.mock(SecretsProperties.class);
		applicationProperties.setPartnerBasicAuthUsername("");
		partnerProperties = Mockito.mock(PartnerProperties.class);
		CommonUtil commonUtil = Mockito.spy(CommonUtil.class);
		when(CommonUtil.getRetryableRestTemplate(builder, true,true,
				partnerProperties.getClientMaxConnPerRoute(),
				partnerProperties.getClientMaxConn(),
				partnerProperties.getConnectionTimeOut(),
				partnerProperties.getReadTimeOut(),
				partnerProperties.getSocketTimeOut(),
				partnerProperties.getRetryCount(),
				partnerProperties.getTtl())).thenReturn(client);

		partnerAuthTokenDAOImpl =new PartnerAuthTokenDAOImpl(domainProperties,builder,secretsProperties,applicationProperties,urlProperties,partnerProperties);

		MockHttpServletRequest request = new MockHttpServletRequest();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));       

		headers = new HttpHeaders();
		headers.add(ServiceConstant.HEADER_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_APPLICATION_JSON);
		headers.add(ServiceConstant.HEADER_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER + "23412345");
		headers.add(ServiceConstant.X_MGM_CORRELATION_ID, UUID.randomUUID().toString());
		headers.add(ServiceConstant.X_MGM_TRANSACTION_ID, UUID.randomUUID().toString());
		headers.add(ServiceConstant.HEADER_USER_AGENT, UUID.randomUUID().toString());
		headers.add(ServiceConstant.HEADER_FRAUD_AGENT_TOKEN, UUID.randomUUID().toString());

		ReflectionTestUtils.setField(partnerAuthTokenDAOImpl, "client", client);

	}

	@Test
	public void refreshTokenTest() {
		when(partnerProperties.isEnabled()).thenReturn(true);
		when(domainProperties.getPartnerAccountBasic()).thenReturn("");
		when(urlProperties.getPartnerAccountAuth()).thenReturn("");

		when(secretsProperties.getSecretValue(any())).thenReturn("");	
		TokenResponse tokenResponse = new TokenResponse();
		tokenResponse.setAccessToken("token");

		HttpHeaders	headers = new HttpHeaders();
		headers.add(ServiceConstant.HEADER_GRANT_TYPE, ServiceConstant.PARTNER_GRANTTYPE_VALUE);
		headers.add(ServiceConstant.HEADER_SCOPE,ServiceConstant.PARTNER_SCOPE_VALUE);

		ResponseEntity<TokenResponse> entity = ResponseEntity.ok(tokenResponse);
		when(client.exchange(ArgumentMatchers.anyString(), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<TokenResponse>>any(),
				Mockito.anyMap()))
		.thenReturn(entity);
		partnerAuthTokenDAOImpl.refreshToken();

	}

	@Test
	public void ResourceAccessExceptionTest() {
		Mockito.doThrow(ResourceAccessException.class)
		.when(client).exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<TokenResponse>>any(), Mockito.anyMap());
		when(partnerProperties.isEnabled()).thenReturn(true);
		when(domainProperties.getPartnerAccountBasic()).thenReturn("");
		when(urlProperties.getPartnerAccountAuth()).thenReturn("");

		partnerAuthTokenDAOImpl.refreshToken();
		Mockito.verify(applicationProperties, Mockito.atLeastOnce()).getPartnerVersion();
	}
	
	@Test
	public void refreshToken_NegativeTest() {
		partnerAuthTokenDAOImpl.refreshToken();
		Mockito.verify(applicationProperties, Mockito.never()).getPartnerVersion();
	}	
	
	@Test(expected = Exception.class)
	public void generateAuthTokenTest() throws Exception {
		when(partnerProperties.isEnabled()).thenReturn(false);	
		TokenResponse response = partnerAuthTokenDAOImpl.generateAuthToken();
		assertNotNull (response);		
	}
}
