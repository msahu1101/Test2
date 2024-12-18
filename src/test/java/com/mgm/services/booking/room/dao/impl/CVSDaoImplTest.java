package com.mgm.services.booking.room.dao.impl;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.model.response.CVSResponse;
import com.mgm.services.booking.room.model.response.TokenResponse;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.util.PropertyConfig;
import com.mgm.services.booking.room.util.PropertyConfig.PropertyValue;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;

import lombok.extern.log4j.Log4j2;


@Log4j2
public class CVSDaoImplTest extends BaseRoomBookingTest {

	@Mock
	private static IDMSTokenDAO idmsTokenDAO;

	@Mock
	private static RestTemplate client;

	@Mock
	private static DomainProperties domainProperties;

	@Mock
	private static ApplicationProperties applicationProperties;

	@Mock
	private static RestTemplateBuilder restTemplateBuilder;

	@Mock
	private static PropertyConfig propertyConfig;

	@Mock
	private static AcrsProperties acrsProperties;

	@InjectMocks
	private static CVSDaoImpl cVSDaoImpl;

	@BeforeEach
	public void setup() {
		client = Mockito.mock(RestTemplate.class);
		idmsTokenDAO = Mockito.mock(IDMSTokenDAO.class);

		domainProperties = new DomainProperties();
		domainProperties.setCrs("https://cfts.hospitality.api.amadeus.com");
		domainProperties.setCrsUcpRetrieveResv("");
		domainProperties.setCvs("test");

		applicationProperties = Mockito.mock(ApplicationProperties.class);
		applicationProperties = new ApplicationProperties();
		applicationProperties.setCrsUcpRetrieveResvEnvironment("test");
		propertyConfig = Mockito.mock(PropertyConfig.class);

		restTemplateBuilder = Mockito.mock(RestTemplateBuilder.class);

		acrsProperties = Mockito.mock(AcrsProperties.class);
		acrsProperties.setLiveCRS(true);

		try {
			cVSDaoImpl = new CVSDaoImpl(propertyConfig,domainProperties,
					acrsProperties,
					applicationProperties,restTemplateBuilder);
			cVSDaoImpl.setIdmsTokenDao(idmsTokenDAO);
			cVSDaoImpl.client = client;
		} catch(Exception e)
		{
			log.error(e.getMessage());
			log.error("Caused " + e.getCause());
		}

	}


	private void setMockAuthToken() {
		TokenResponse tknRes = new TokenResponse();
		tknRes.setAccessToken("1234");
		Mockito.doReturn(tknRes).when(idmsTokenDAO).generateToken();
	}

	@Test
	public void testGetCustomerValues_Success() {
		setMockAuthToken();

		String mLifeNumber = "123456";

		CVSResponse cvsResponse = new CVSResponse();
		CVSResponse.CVSCustomer customer = new CVSResponse.CVSCustomer();
		CVSResponse.CVSCustomerValue value1 = new CVSResponse.CVSCustomerValue();
		customer.setCustomerValues(new CVSResponse.CVSCustomerValue[]{value1});
		value1.setProperty("190");
		value1.setGsePropertyIds(Arrays.asList("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad"));
		cvsResponse.setCustomer(customer);

		PropertyConfig.PropertyValue propertyValue = new PropertyConfig.PropertyValue();
		List<PropertyValue> propertyValues = new ArrayList<PropertyConfig.PropertyValue>();
		propertyValue.setOperaCode("180");
		propertyValue.setGsePropertyIds(Arrays.asList("dc00e77f-d6bb-4dd7-a8ea"));
		propertyValue.setGseMerchantID("ARIA2");
		propertyValue.setPatronSiteId(8);
		propertyValues.add(propertyValue);

		when(propertyConfig.getPropertyValues()).thenReturn(propertyValues);

		when(client.exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<CVSResponse>>any(), Mockito.anyMap()))
		.thenReturn(new ResponseEntity<>(cvsResponse, HttpStatus.OK));


		try {
			CVSResponse response = cVSDaoImpl.getCustomerValues(mLifeNumber);
			assertNotNull(response);
		}catch(BusinessException be) {
			assertEquals(ErrorCode.UNABLE_TO_PRICE, be.getErrorCode());
		} catch (Exception ex) {
			fail("Caught unexpected Exception during GetCustomerValues Test.");
		}
	}


	@Test
	void testGetCustomerValues_MLifeNumberNotFound() {
		setMockAuthToken();
		// Mock the response entity with a 404 status code
		when(client.exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),ArgumentMatchers.any(),
				eq(CVSResponse.class)))
		.thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));

		// Call the method
		CVSResponse response = cVSDaoImpl.getCustomerValues("123456");

		// Verify that the response is null
		assertNull(response);
	}

	@Test
	void testGetCustomerValues_BusinessException() {
		setMockAuthToken();

		// Mock the response entity with a 500 status code
		when(client.exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),ArgumentMatchers.any(), eq(CVSResponse.class)))
		.thenThrow(new BusinessException(ErrorCode.SYSTEM_ERROR, "Test error message"));

		// Call the method
		CVSResponse response = cVSDaoImpl.getCustomerValues("123456");

		// Verify that the response is null
		assertNull(response);
	}

}

