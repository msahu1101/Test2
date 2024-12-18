package com.mgm.services.booking.room.dao.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.io.File;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLException;

import com.mgm.services.booking.room.model.ReservationSystemType;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.crs.guestprofiles.OrganizationalSearchResponse;
import com.mgm.services.booking.room.model.request.OrganizationSearchV2Request;
import com.mgm.services.booking.room.model.response.ACRSAuthTokenResponse;
import com.mgm.services.booking.room.model.response.OrganizationSearchV2Response;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.util.CommonUtil;

public class IATADAOImplTest extends BaseRoomBookingTest {

	@Mock
	private static RestTemplate client;

	@InjectMocks
	private static DomainProperties domainProperties;

	@InjectMocks
	private static ApplicationProperties applicationProperties;

	@InjectMocks
	private static RestTemplateBuilder restTemplateBuilder;

	@InjectMocks
	private static URLProperties urlProperties;

	@InjectMocks
	private static AcrsProperties acrsProperties;

	@InjectMocks
	private static SecretsProperties secretsProperties;
	
	@Mock
	private static IATADAOImpl iataDAOImpl;
	
	@InjectMocks
    private static ReferenceDataDAOHelper referenceDataDAOHelper;
	
	@InjectMocks
    private static ACRSOAuthTokenDAOImpl acrsOAuthTokenDAOImpl;

	static Logger logger = LoggerFactory.getLogger(IATADAOImplTest.class);

	@BeforeClass
	public static void init() throws SSLException, ParseException {
		client = Mockito.mock(RestTemplate.class);
		domainProperties = new DomainProperties();
		domainProperties.setPhoenix("http://vldmpphx01t.mgmmirage.org:8080");
		referenceDataDAOHelper = Mockito.mock(ReferenceDataDAOHelper.class);
		acrsOAuthTokenDAOImpl = Mockito.mock(ACRSOAuthTokenDAOImpl.class);
		domainProperties.setCrs("https://cfts.hospitality.api.amadeus.com");
		restTemplateBuilder = Mockito.mock(RestTemplateBuilder.class);
		applicationProperties = Mockito.mock(ApplicationProperties.class);
		secretsProperties = Mockito.mock(SecretsProperties.class);
		acrsProperties = new AcrsProperties();
		acrsProperties.setModifyDateStartPath("modifyDateStartPath");
		acrsProperties.setModifyDateEndPath("modifyDateEndPath");
		acrsProperties.setModifySpecialRequestPath("modifySpecialRequestPath");
		acrsProperties.setLiveCRS(true);
		acrsProperties.setLiveCRSIata(true);
		urlProperties = new URLProperties();
		urlProperties.setIataValidation("/phoenix/api/travel-agent/v1/by-agent-id/{iataCode}");
		urlProperties.setAcrsOrganizationSearch(
				"/hotel-platform/{acrsEnvironment}/mgm/{acrsVersion}/hotel/organizations/{acrsChainCode}/search");
		CommonUtil commonUtil = Mockito.spy(CommonUtil.class);
		try {
			when(commonUtil.getRetryableRestTemplate(restTemplateBuilder, applicationProperties.isSslInsecure(),
					acrsProperties.isLiveCRS(),applicationProperties.getConnectionPerRouteDaoImpl(),
					applicationProperties.getMaxConnectionPerDaoImpl(),
					applicationProperties.getConnectionTimeout(),
					applicationProperties.getReadTimeOut(),
					applicationProperties.getSocketTimeOut(),1,applicationProperties.getCrsRestTTL())).thenReturn(client);
			iataDAOImpl = new IATADAOImpl(restTemplateBuilder, domainProperties, applicationProperties, acrsProperties,
					urlProperties, referenceDataDAOHelper,acrsOAuthTokenDAOImpl,secretsProperties);
		} catch (Exception e) {
			logger.error(e.getMessage());
			logger.error("Cause: " + e.getCause());
		}
	}
	
	private void setMockAuthToken() {
		Map<String, ACRSAuthTokenResponse> acrsAuthTokenResponseMap = new HashMap<String, ACRSAuthTokenResponse>();
		ACRSAuthTokenResponse tokenRes = new ACRSAuthTokenResponse();
		tokenRes.setToken("token");
		acrsAuthTokenResponseMap.put("ICECC", tokenRes);
        when(acrsOAuthTokenDAOImpl.generateToken()).thenReturn(acrsAuthTokenResponseMap);
    }
	
	private void setMockReferenceDataDAOHelper() {
        when(referenceDataDAOHelper.retrieveAcrsPropertyID(Mockito.anyString())).thenReturn("ACRS");
        when(referenceDataDAOHelper.retrieveRatePlanDetail(Mockito.anyString(), Mockito.anyString())).thenReturn("ACRS");
        when(referenceDataDAOHelper.retrieveRoomTypeDetail(Mockito.anyString(), Mockito.anyString())).thenReturn("ACRS");
        when(referenceDataDAOHelper.getAcrsVendor(Mockito.any())).thenReturn("ICECC");
		when(referenceDataDAOHelper.isAcrsEnabled()).thenReturn(true);
    }

	@Test
	public void testOrganizationSearch_org_name() {

		mockOrganizationSearchResponse();
		setMockReferenceDataDAOHelper();
		setMockAuthToken();
		OrganizationSearchV2Request organizationSearchRequest = new OrganizationSearchV2Request();
		organizationSearchRequest.setOrgName("Amadeus");

		List<OrganizationSearchV2Response> organizationSearchV2Response = iataDAOImpl
				.organizationSearch(organizationSearchRequest);

		assertNotNull(organizationSearchV2Response);
		assertEquals(1, organizationSearchV2Response.size());
		assertEquals("Amadeus", organizationSearchV2Response.get(0).getFullName());
		assertEquals("1A", organizationSearchV2Response.get(0).getIataCode().get(0));
		//assertEquals("00373010", organizationSearchV2Response.get(10).getIataCode().get(0));
		assertEquals("Amadeus", organizationSearchV2Response.get(0).getShortName());
		//assertEquals("© Amadeus IT Group SA ", organizationSearchV2Response.get(0).getFullName());
		//assertEquals("UAT", organizationSearchV2Response.get(20).getShortName());

	}

	@Test
	public void testOrganizationSearch_org_name_invalid() {
		setMockReferenceDataDAOHelper();
		setMockAuthToken();
		mockOrganizationSearchResponse_empty();
		OrganizationSearchV2Request organizationSearchRequest = new OrganizationSearchV2Request();
		organizationSearchRequest.setOrgName("xxx");

		List<OrganizationSearchV2Response> organizationSearchV2Response = iataDAOImpl
				.organizationSearch(organizationSearchRequest);

		assertNotNull(organizationSearchV2Response);
		assertEquals(0, organizationSearchV2Response.size());
	}

	@Test
	public void testOrganizationSearch_iata_codes() {

		mockOrganizationSearchResponse_iata();
		setMockReferenceDataDAOHelper();
		setMockAuthToken();
		OrganizationSearchV2Request organizationSearchRequest = new OrganizationSearchV2Request();
		organizationSearchRequest.setIataCode("1A");

		List<OrganizationSearchV2Response> organizationSearchV2Response = iataDAOImpl
				.organizationSearch(organizationSearchRequest);

		assertNotNull(organizationSearchV2Response);
		assertEquals(1, organizationSearchV2Response.size());
		assertEquals("Amadeus", organizationSearchV2Response.get(0).getFullName());
		assertEquals("1A", organizationSearchV2Response.get(0).getIataCode().get(0));
		assertEquals("Amadeus", organizationSearchV2Response.get(0).getShortName());
		//assertEquals("1A", organizationSearchV2Response.get(20).getIataCode().get(0));

	}

	@Test
	public void testOrganizationSearch_iata_codes_invalid() {

		mockOrganizationSearchResponse_empty();
		setMockReferenceDataDAOHelper();
		setMockAuthToken();
		OrganizationSearchV2Request organizationSearchRequest = new OrganizationSearchV2Request();
		organizationSearchRequest.setIataCode("xx");

		List<OrganizationSearchV2Response> organizationSearchV2Response = iataDAOImpl
				.organizationSearch(organizationSearchRequest);

		assertNotNull(organizationSearchV2Response);
		assertEquals(0, organizationSearchV2Response.size());

	}

	private HttpEntity<?> emptyOrganizationalSearchResponse() {
		File file = new File(getClass().getResource("/organization-search-empty.json").getPath());
		ResponseEntity<?> response = new ResponseEntity<OrganizationalSearchResponse>(
				convertCrs(file, OrganizationalSearchResponse.class), HttpStatus.OK);
		return response;
	}

	private void mockOrganizationSearchResponse_empty() {
		when(client.exchange(ArgumentMatchers.contains("organizations"), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<OrganizationalSearchResponse>>any(), Mockito.anyMap()))
						.thenReturn((ResponseEntity<OrganizationalSearchResponse>) emptyOrganizationalSearchResponse());

	}

	private void mockOrganizationSearchResponse() {
		when(client.exchange(ArgumentMatchers.contains("organizations"), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<OrganizationalSearchResponse>>any(), Mockito.anyMap()))
						.thenReturn((ResponseEntity<OrganizationalSearchResponse>) getOrganizationalSearchResponse());

	}

	private HttpEntity<?> getOrganizationalSearchResponse() {
		File file = new File(getClass().getResource("/organization-search-name.json").getPath());
		ResponseEntity<?> response = new ResponseEntity<OrganizationalSearchResponse>(
				convertCrs(file, OrganizationalSearchResponse.class), HttpStatus.OK);
		return response;
	}

	private void mockOrganizationSearchResponse_iata() {
		when(client.exchange(ArgumentMatchers.contains("organizations"), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<OrganizationalSearchResponse>>any(), Mockito.anyMap()))
						.thenReturn(
								(ResponseEntity<OrganizationalSearchResponse>) getOrganizationalSearchResponse_iata());
	}

	private HttpEntity<?> getOrganizationalSearchResponse_iata() {
		File file = new File(getClass().getResource("/organization-search-iatacode.json").getPath());
		ResponseEntity<?> response = new ResponseEntity<OrganizationalSearchResponse>(
				convertCrs(file, OrganizationalSearchResponse.class), HttpStatus.OK);
		return response;
	}

}
