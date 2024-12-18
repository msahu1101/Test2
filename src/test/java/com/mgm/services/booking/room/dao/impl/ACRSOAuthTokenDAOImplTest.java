package com.mgm.services.booking.room.dao.impl;

import static org.junit.Assert.assertNotNull;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.ACRSOAuthTokenDAO;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.request.ACRSTokenRequest;
import com.mgm.services.booking.room.model.response.ACRSAuthTokenResponse;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.util.CommonUtil;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ACRSOAuthTokenDAOImplTest extends BaseRoomBookingTest {

	@Mock
	private static DomainProperties domainProperties;
	@Mock
	private static RestTemplate client;
	@Mock
	private static URLProperties urlProperties;
	@Mock
	private static AcrsProperties acrsProperties;
	@Mock
	private static SecretsProperties secretsProperties;
	@InjectMocks
	private static ReferenceDataDAOHelper referenceDataDAOHelper;
	@Mock
	private static RetryTemplate retryTemplate;
	@Mock
	private static ApplicationProperties applicationProperties;
	@Mock
	private static RestTemplateBuilder restTemplateBuilder;
	private static HttpHeaders headers;

	@InjectMocks
	private static ACRSOAuthTokenDAOImpl acrsOAuthTokenDAOImpl;
	static Logger logger = LoggerFactory.getLogger(ACRSOAuthTokenDAOImplTest.class);

	@BeforeClass
	public static void init() throws SSLException {
		client = Mockito.mock(RestTemplate.class);
		domainProperties = Mockito.mock(DomainProperties.class);
		secretsProperties = Mockito.mock(SecretsProperties.class);
		urlProperties = Mockito.mock(URLProperties.class);
		applicationProperties=Mockito.mock(ApplicationProperties.class);
		acrsProperties = new AcrsProperties();
		acrsProperties.setEnvironment("test");
		restTemplateBuilder = Mockito.mock(RestTemplateBuilder.class);
		referenceDataDAOHelper = Mockito.mock(ReferenceDataDAOHelper.class);

		CommonUtil commonUtil = Mockito.spy(CommonUtil.class);

		MockHttpServletRequest request = new MockHttpServletRequest();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		acrsOAuthTokenDAOImpl = new ACRSOAuthTokenDAOImpl(urlProperties,domainProperties,client,acrsProperties,secretsProperties,applicationProperties,restTemplateBuilder,referenceDataDAOHelper);

	}

	@Test
	public void generateTokenWebTest() throws Throwable {
		ACRSTokenRequest tokenRequest = new ACRSTokenRequest();
		String clientId ="room_booking_service";
		String clientSecret ="NotARealClientSecretBecauseThisIsAUnitTest";
		tokenRequest.setClientId(clientId);
		tokenRequest.setClientSecret(clientSecret);
		ACRSAuthTokenResponse ACRSAuthTokenResponse = new ACRSAuthTokenResponse();

		when(domainProperties.getCrs()).thenReturn("");
		when(urlProperties.getAcrsAuthToken()).thenReturn("");
		when(secretsProperties.getSecretValue(any())).thenReturn("");	

		ResponseEntity<ACRSAuthTokenResponse> entity = ResponseEntity.ok(ACRSAuthTokenResponse);
		when(retryTemplate.execute(any(RetryCallback.class))).thenAnswer(inv -> entity);	

		HttpHeaders	headers = new HttpHeaders();
		headers.add(ServiceConstant.ACRS_AUTH_CLIENT_SECRET_WEB,ServiceConstant.HEADER_CLIENT_SECRET );
		headers.add(ServiceConstant.ACRS_AUTH_CLIENT_ID_WEB, ServiceConstant.HEADER_CLIENT_ID);
		headers.add(ServiceConstant.ACRS_ENVIRONMENT, ServiceConstant.ENVIRONMENT_PH);

		ResponseEntity<ACRSAuthTokenResponse> entity1 = new ResponseEntity<>(ACRSAuthTokenResponse,headers,HttpStatus.OK);
		when(client.exchange(anyString(),any(HttpMethod.class),any(HttpEntity.class),eq(ACRSAuthTokenResponse.class))).thenReturn(entity1);
		ACRSAuthTokenResponse response = acrsOAuthTokenDAOImpl.generateTokenWeb();
		assertNotNull(response);
	}

	@Test
	public void generateTokenIceTest() throws Throwable {

		ACRSTokenRequest tokenRequest = new ACRSTokenRequest();
		tokenRequest.setClientId("room_booking_service");
		tokenRequest.setClientSecret("NotARealClientSecretBecauseThisIsAUnitTest");
		ACRSAuthTokenResponse ACRSAuthTokenResponse = new ACRSAuthTokenResponse();

		when(domainProperties.getCrs()).thenReturn("");
		when(urlProperties.getAcrsAuthToken()).thenReturn("");
		when(secretsProperties.getSecretValue(any())).thenReturn("");	

		ResponseEntity<ACRSAuthTokenResponse> entity = ResponseEntity.ok(ACRSAuthTokenResponse);
		when(retryTemplate.execute(any(RetryCallback.class))).thenAnswer(inv -> entity);

		HttpHeaders	headers = new HttpHeaders();
		headers.add(ServiceConstant.ACRS_AUTH_CLIENT_SECRET_ICE,ServiceConstant.HEADER_CLIENT_SECRET );
		headers.add(ServiceConstant.ACRS_AUTH_CLIENT_ID_ICE, ServiceConstant.HEADER_CLIENT_ID);

		ResponseEntity<ACRSAuthTokenResponse> entity1 = new ResponseEntity<>(ACRSAuthTokenResponse,headers,HttpStatus.OK);
		when(client.exchange(anyString(),any(HttpMethod.class),any(HttpEntity.class),eq(ACRSAuthTokenResponse.class))).thenReturn(entity1);

		ACRSAuthTokenResponse response = acrsOAuthTokenDAOImpl.generateTokenIce();
		assertNotNull(response);
	}  
	
	@Test
	public void refreshTokenTest() throws Throwable {
		
		when(referenceDataDAOHelper.isAcrsEnabled()).thenReturn(true);

		ACRSAuthTokenResponse tokenResponseFromIce = new ACRSAuthTokenResponse();
		ACRSAuthTokenResponse tokenResponseFromWeb = new ACRSAuthTokenResponse();		 
		tokenResponseFromIce.setToken("");
		tokenResponseFromWeb.setToken("");

		ResponseEntity<ACRSAuthTokenResponse> entity = ResponseEntity.ok(tokenResponseFromIce);
		when(retryTemplate.execute(any(RetryCallback.class))).thenAnswer(inv -> entity);
		when(acrsOAuthTokenDAOImpl.generateTokenIce()).thenReturn(tokenResponseFromIce);
		when(acrsOAuthTokenDAOImpl.generateTokenWeb()).thenReturn(tokenResponseFromWeb);

		acrsOAuthTokenDAOImpl.refreshToken();
	}
}
