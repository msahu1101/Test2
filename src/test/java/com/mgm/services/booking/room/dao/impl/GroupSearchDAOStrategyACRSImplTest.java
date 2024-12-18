package com.mgm.services.booking.room.dao.impl;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLException;

import com.mgm.services.booking.room.model.crs.groupretrieve.GroupSearchResGroupBookingReservationSearch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import com.mgm.services.booking.room.BaseAcrsRoomBookingTest;
import com.mgm.services.booking.room.model.request.GroupSearchV2Request;
import com.mgm.services.booking.room.model.response.GroupSearchV2Response;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.PropertyConfig;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;

class GroupSearchDAOStrategyACRSImplTest extends BaseAcrsRoomBookingTest {

	private static GroupSearchDAOStrategyACRSImpl groupSearchDAOStrategyACRS;
	private static PropertyConfig propertyConfig;
	private static RestTemplate restTemplateMockClient;
	
	@InjectMocks
	private static SecretsProperties secretsProperties;

	private Logger logger = LoggerFactory.getLogger(GroupSearchDAOStrategyACRSImplTest.class);

	@BeforeEach
	public void init() {
		super.init();
		propertyConfig = Mockito.mock(PropertyConfig.class);
		// set urlProperties
		urlProperties.setAcrsGroupSearch(
				"/hotel-platform/{acrsEnvironment}/mgm/{acrsVersion}/group-and-allotments/chains/{acrsChainCode}/groups/search");
		// set applicationProperties
		applicationProperties.setSslInsecure(true);
		secretsProperties = Mockito.mock(SecretsProperties.class);
		// set acrsProperties
		AcrsProperties acrsProperties = new AcrsProperties();
		acrsProperties.setLiveCRS(true);
		acrsProperties.setMaxAcrsCommentLength(51);
		acrsProperties.setEnvironment("mockEnv");
		acrsProperties.setSearchVersion("v1");
		acrsProperties.setChainCode("MGMMock");
		// set domainProperties
		DomainProperties domainProperties = new DomainProperties();
		domainProperties.setCrs("");
		RestTemplateBuilder restTemplateMockBuilder = Mockito.mock(RestTemplateBuilder.class);
		restTemplateMockClient = Mockito.mock(RestTemplate.class);
		when(CommonUtil.getRetryableRestTemplate(restTemplateMockBuilder, applicationProperties.isSslInsecure(),
				acrsProperties.isLiveCRS(),applicationProperties.getAcrsConnectionPerRouteDaoImpl(),
				applicationProperties.getAcrsMaxConnectionPerDaoImpl(),
				applicationProperties.getConnectionTimeout(),
				applicationProperties.getReadTimeOut(),
				applicationProperties.getSocketTimeOut(),1, applicationProperties.getCrsRestTTL())).thenReturn(restTemplateMockClient);
		try {
			groupSearchDAOStrategyACRS = new GroupSearchDAOStrategyACRSImpl(urlProperties, domainProperties,
					applicationProperties, acrsProperties, restTemplateMockBuilder, referenceDataDAOHelper,
					acrsOAuthTokenDAOImpl, propertyConfig, secretsProperties);
		} catch (SSLException e) {
			logger.error("Unexpected encountered while init() in GroupSearchDAOStrategyACRSImplTest: ", e);
			throw new RuntimeException(e);
		}
	}

	@Test
	void searchGroup() throws ParseException {
		setMockAuthToken("mockToken", "ICECC");
		setMockForRoomPropertyCode();
		setMockForGroupSearchSuccessFromFile("/acrs/groupSearch/crs-group-search-resp.json");
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(eq("MV290"))).thenReturn(true);
		when(propertyConfig.getPropertyValuesMap()).thenReturn(createPropertyValueMap());
		LocalDate startDate = LocalDate.now();
		GroupSearchV2Request request = createGroupSearchReq("MockGroupCode", "MV291", startDate,
				startDate.plus(1, ChronoUnit.DAYS), "ICECC");
		List<GroupSearchV2Response> response = null;
		try {
			response = groupSearchDAOStrategyACRS.searchGroup(request);
		} catch (BusinessException ex) {
			logger.error("Error encountered during ACRS groupSearch Test Case: ", ex);
			fail();
		}
		assertNotNull(response);
		assertEquals(1, response.size());
		GroupSearchV2Response responseObject = response.get(0);
		assertEquals("MockGroupCode", responseObject.getGroupCode());
		assertEquals("Mock Group Name", responseObject.getName());
		assertEquals("MA", responseObject.getOperaGuaranteeCode());
		assertEquals("RL", responseObject.getReservationMethod());
		assertEquals("Group", responseObject.getCategory());
		assertEquals(fmt.parse("2023-07-12"), responseObject.getPeriodStartDate());
		assertEquals(fmt.parse("2023-07-16"), responseObject.getPeriodEndDate());
	}

	private GroupSearchV2Request createGroupSearchReq(String groupName, String propertyId, LocalDate startDate,
													  LocalDate endDate, String source) {
		GroupSearchV2Request request = new GroupSearchV2Request();
		request.setGroupName(groupName);
		request.setPropertyId(propertyId);
		request.setStartDate(startDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
		request.setEndDate(endDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
		request.setSource(source);
		return request;
	}

	private Map<String, PropertyConfig.PropertyValue> createPropertyValueMap() {
		Map<String, PropertyConfig.PropertyValue> returnMap = new HashMap<>();
		PropertyConfig.PropertyValue propertyValue = new PropertyConfig.PropertyValue();
		propertyValue.setMasterPropertyCode("MV290");
		returnMap.put("MV291", propertyValue);
		return returnMap;
	}

	private void setMockForGroupSearchSuccess(ResponseEntity<GroupSearchResGroupBookingReservationSearch> response) {
		when(restTemplateMockClient.exchange(eq("" + urlProperties.getAcrsGroupSearch()), eq(HttpMethod.POST),
						any(HttpEntity.class),
				eq(GroupSearchResGroupBookingReservationSearch.class), any(Map.class)))
				.thenReturn(response);
	}

	private void setMockForGroupSearchSuccessFromFile(String jsonFileName) {
		setMockForGroupSearchSuccess(new ResponseEntity<>(
				convertCrs(jsonFileName, GroupSearchResGroupBookingReservationSearch.class),
				HttpStatus.CREATED));
	}
	
	@Test
	void testHasError() throws IOException {
		GroupSearchDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = 
				new GroupSearchDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
		boolean result = errorHandler.hasError(httpResponse);
		
		// Assertion
		assertTrue(result);
	}
	
	@Test
	void testHandleError() throws IOException {
		GroupSearchDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = 
				new GroupSearchDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		when(httpResponse.getBody()).thenReturn(null);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.SERVICE_UNAVAILABLE);
		
		// Assertions
		SystemException ex = assertThrows(SystemException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.SYSTEM_ERROR, ex.getErrorCode());
	}
	
	@Test
	void testHandleErrorElse() throws IOException {
		GroupSearchDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = 
				new GroupSearchDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		when(httpResponse.getBody()).thenReturn(null);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
		
		// Assertions
		SystemException ex = assertThrows(SystemException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.SYSTEM_ERROR, ex.getErrorCode());
	}
}