package com.mgm.services.booking.room.dao.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.model.response.TokenResponse;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.model.authorization.AuthorizationTransactionRequest;
import com.mgm.services.common.model.authorization.AuthorizationTransactionResponse;

@RunWith(MockitoJUnitRunner.class)
public class AccertifyDAOImplTest extends BaseRoomBookingTest  {

	@Mock
	private static DomainProperties domainProperties;
	@Mock
    private static RestTemplate client;
	@Mock
	private static URLProperties urlProperties;

	@Mock
	private static IDMSTokenDAO idmsTokenDAO;
	@Mock
	private static SecretsProperties secretsProperties;
	
	 @InjectMocks
	    private static AccertifyDAOImpl accertifyDAOImpl;
	    static Logger logger = LoggerFactory.getLogger(AccertifyDAOImplTest.class);

	    @BeforeClass
	    public static void init() throws SSLException {
	        client = Mockito.mock(RestTemplate.class);
	        idmsTokenDAO = Mockito.mock(IDMSTokenDAO.class);
	        domainProperties = new DomainProperties();
	        domainProperties.setAccertify(("https://accertify-service.com/service/{accertifyEnvironment}"));
	        urlProperties = new URLProperties();
	         CommonUtil commonUtil = Mockito.spy(CommonUtil.class);

	        urlProperties.setAccertifyAuthorization("/v2/authorize");
	        urlProperties.setAccertifyConfirmation("/v2/confirm");

	        accertifyDAOImpl = new AccertifyDAOImpl(client,domainProperties,urlProperties,secretsProperties,idmsTokenDAO);

	        MockHttpServletRequest request = new MockHttpServletRequest();
	         RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
	    }
	    
	    @Test
	    public void accertifyAuthorizeTest() {
	         
	    	 TokenResponse tokenResponse = new TokenResponse();
	         tokenResponse.setAccessToken("accessToken");
	         when(idmsTokenDAO.generateToken()).thenReturn(tokenResponse);
	         AuthorizationTransactionRequest transactionRequest = new AuthorizationTransactionRequest();
	        
	         ResponseEntity<AuthorizationTransactionRequest> entity = ResponseEntity.ok(transactionRequest);
	         AuthorizationTransactionResponse response =new AuthorizationTransactionResponse();
	         when(client.postForEntity(anyString(),any(HttpEntity.class), eq(AuthorizationTransactionResponse.class))).thenReturn(ResponseEntity.ok(response));
	         
	         AuthorizationTransactionResponse response1 = accertifyDAOImpl.authorize(transactionRequest);
	         assertNotNull(response1);
	         verify(client).postForEntity(anyString(),any(HttpEntity.class), eq(AuthorizationTransactionResponse.class));
	    }
	    
	    @Test
	    public void accertifyConfirmTest() {
	    	TokenResponse tokenResponse = new TokenResponse();
	         tokenResponse.setAccessToken("accessToken");
	         when(idmsTokenDAO.generateToken()).thenReturn(tokenResponse);
	         AuthorizationTransactionRequest transactionRequest = new AuthorizationTransactionRequest();
	         HttpHeaders threadHeaders = new HttpHeaders();
	         AuthorizationTransactionResponse response =new AuthorizationTransactionResponse();

	         ResponseEntity<AuthorizationTransactionRequest> entity =ResponseEntity.ok(transactionRequest);
	         when(client.postForEntity(anyString(),any(HttpEntity.class), eq(AuthorizationTransactionResponse.class))).thenReturn(ResponseEntity.ok(response));
	         accertifyDAOImpl.confirm(transactionRequest, threadHeaders);
	         verify(client).postForEntity(anyString(),any(HttpEntity.class), eq(AuthorizationTransactionResponse.class));
      
	    }
}
