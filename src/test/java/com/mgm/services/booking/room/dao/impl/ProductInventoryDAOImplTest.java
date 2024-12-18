package com.mgm.services.booking.room.dao.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.dao.impl.ProductInventoryDAOImpl.RestTemplateResponseErrorHandler;
import com.mgm.services.booking.room.model.inventory.BookedItemList;
import com.mgm.services.booking.room.model.inventory.CommitInventoryReq;
import com.mgm.services.booking.room.model.inventory.HoldInventoryReq;
import com.mgm.services.booking.room.model.inventory.HoldInventoryRes;
import com.mgm.services.booking.room.model.inventory.InventoryGetRes;
import com.mgm.services.booking.room.model.inventory.ReleaseInventoryReq;
import com.mgm.services.booking.room.model.inventory.RollbackInventoryReq;
import com.mgm.services.booking.room.model.response.TokenResponse;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;

class ProductInventoryDAOImplTest {

	@Mock
	URLProperties urlProperties;
	@Mock
	DomainProperties domainProperties;
	@Mock
	RestTemplate client;
	@Mock
	IDMSTokenDAO idmsTokenDAO;
	@Mock
	ApplicationProperties applicationProperties;
	
	@InjectMocks
	ProductInventoryDAOImpl productInventoryDAOImpl;
	
	@BeforeEach
	void init() {
		MockitoAnnotations.initMocks(this);
		
		applicationProperties.setApigeeEnvironment("apigeeEnvironment");
		domainProperties.setInventoryService("inventoryService");
		urlProperties.setGetInventory("getInventory");
		
		TokenResponse tknRes = new TokenResponse();
        tknRes.setAccessToken("1234");
        Mockito.doReturn(tknRes).when(idmsTokenDAO).generateToken();
    	
    	MockHttpServletRequest request = new MockHttpServletRequest();
    	request.addHeader(ServiceConstant.X_MGM_CHANNEL, "channel");
    	request.addHeader(ServiceConstant.X_MGM_SOURCE, "source");
    	RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		productInventoryDAOImpl = new ProductInventoryDAOImpl(urlProperties, 
				client, applicationProperties, domainProperties, idmsTokenDAO);
		
    	ReflectionTestUtils.setField(productInventoryDAOImpl, "client", client);
	}
	
	@Test
	void testGetInventory() {
		ResponseEntity<InventoryGetRes> res = ResponseEntity.ok(new InventoryGetRes());
		when(client.exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<InventoryGetRes>>any(), Mockito.anyMap()))
		.thenReturn(res);
		
		InventoryGetRes result = null;
		result = productInventoryDAOImpl.getInventory("productCode", true);
		assertNotNull(result);
	}
	
	@Test
	void testGetInventoryException() {		
		Mockito.doThrow(RestClientException.class)
			.when(client).exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<HoldInventoryRes>>any(), Mockito.anyMap());
		
		assertThrows(SystemException.class, 
				() -> productInventoryDAOImpl.getInventory("productCode", true));
	}
	
	@Test
	void testHoldInventory() {
		ResponseEntity<HoldInventoryRes> res = ResponseEntity.ok(new HoldInventoryRes());
		when(client.exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<HoldInventoryRes>>any(), Mockito.anyMap()))
		.thenReturn(res);
		
		HoldInventoryReq request = new HoldInventoryReq();
		productInventoryDAOImpl.holdInventory(request);
		Mockito.verify(urlProperties).getHoldInventory();
	}
	
	@Test
	void testHoldInventoryException() {
		Mockito.doThrow(RestClientException.class)
			.when(client).exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<HoldInventoryRes>>any(), Mockito.anyMap());
		
		HoldInventoryReq request = new HoldInventoryReq();
		assertThrows(SystemException.class, 
				() -> productInventoryDAOImpl.holdInventory(request));
	}
	
	@Test
	void testReleaseInventory() {
		ResponseEntity<Void> res = new ResponseEntity<>(HttpStatus.ACCEPTED);
		when(client.exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<Void>>any(), Mockito.anyMap()))
		.thenReturn(res);
		
		ReleaseInventoryReq request = new ReleaseInventoryReq();
		productInventoryDAOImpl.releaseInventory(request);
		Mockito.verify(urlProperties).getReleaseInventory();
	}
	
	@Test
	void testReleaseInventoryException() {
		Mockito.doThrow(RestClientException.class)
			.when(client).exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<HoldInventoryRes>>any(), Mockito.anyMap());
	
		ReleaseInventoryReq request = new ReleaseInventoryReq();
		assertThrows(SystemException.class, 
				() -> productInventoryDAOImpl.releaseInventory(request));
	}
	
	@Test
	void testCommitInventory() {
		ResponseEntity<Void> res = new ResponseEntity<>(HttpStatus.ACCEPTED);
		when(client.exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<Void>>any(), Mockito.anyMap()))
		.thenReturn(res);
		
		CommitInventoryReq request = new CommitInventoryReq();
		String result = productInventoryDAOImpl.commitInventory(request);
		assertEquals("Success", result);
	}
	
	@Test
	void testCommitInventoryException() {
		Mockito.doThrow(RestClientException.class)
			.when(client).exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<HoldInventoryRes>>any(), Mockito.anyMap());
	
		CommitInventoryReq request = new CommitInventoryReq();
		String result = productInventoryDAOImpl.commitInventory(request);
		assertNull(result);
	}
	
	@Test
	void testRollBackInventory() {
		ResponseEntity<Void> res = new ResponseEntity<>(HttpStatus.ACCEPTED);
		when(client.exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<Void>>any(), Mockito.anyMap()))
		.thenReturn(res);
		
		RollbackInventoryReq request =  Mockito.mock(RollbackInventoryReq.class);
		productInventoryDAOImpl.rollBackInventory(request);
		Mockito.verify(urlProperties).getRollbackInventory();
	}
	
	@Test
	void testRollbackInventoryException() {
		Mockito.doThrow(RestClientException.class)
			.when(client).exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<HoldInventoryRes>>any(), Mockito.anyMap());
	
		RollbackInventoryReq request = Mockito.mock(RollbackInventoryReq.class);
		productInventoryDAOImpl.rollBackInventory(request);
		Mockito.verify(request, Mockito.atLeastOnce()).getConfirmationNumber();
	}
	
	@Test
	void testGetInventoryStatus() {
		ResponseEntity<BookedItemList> res = ResponseEntity.ok(new BookedItemList());
		when(client.exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<BookedItemList>>any(), Mockito.anyMap()))
		.thenReturn(res);
		
		BookedItemList result = null;
		result = productInventoryDAOImpl.getInventoryStatus("0I62SI1940", "holdId");
		assertNotNull(result);
	}
	
	@Test
	void testGetInventoryStatusException() {
		BookedItemList result = null;
		result = productInventoryDAOImpl.getInventoryStatus("", "holdId");
		assertNull(result);
	}
	
	@Test
	void testHandleErrorUNAUTHORIZED() throws IOException {
		RestTemplateResponseErrorHandler errorHandler = new RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		when(httpResponse.getBody()).thenReturn(null);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.UNAUTHORIZED);

		// Assertion
		BusinessException ex = assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.TRANSACTION_NOT_AUTHORIZED, ex.getErrorCode());
	}
	
	@Test
	void testHandleErrorINVALID_HOLDID() throws IOException {
		RestTemplateResponseErrorHandler errorHandler = new RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		when(httpResponse.getBody()).thenReturn( new ByteArrayInputStream("640-2-400".getBytes()));
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);

		// Assertion
		BusinessException ex = assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.RESERVATION_NOT_FOUND, ex.getErrorCode());
	}
	
	@Test
	void testHandleErrorElse() throws IOException {
		RestTemplateResponseErrorHandler errorHandler = new RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		when(httpResponse.getBody()).thenReturn(null);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);

		// Assertion
		BusinessException ex = assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.SYSTEM_ERROR, ex.getErrorCode());
	}
	
	@Test
	void testHandleErrorSYSTEM_ERROR() throws IOException {
		RestTemplateResponseErrorHandler errorHandler = new RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		when(httpResponse.getBody()).thenReturn(null);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.SERVICE_UNAVAILABLE);

		// Assertion
		SystemException ex = assertThrows(SystemException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.SYSTEM_ERROR, ex.getErrorCode());
	}
	
	@Test
	void testHasError() throws IOException {
		RestTemplateResponseErrorHandler errorHandler = new RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
		boolean result = errorHandler.hasError(httpResponse);
		
		// Assertion
		assertTrue(result);
	}
}
