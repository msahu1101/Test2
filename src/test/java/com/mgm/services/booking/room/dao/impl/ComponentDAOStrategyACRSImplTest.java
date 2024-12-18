package com.mgm.services.booking.room.dao.impl;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLException;

import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.exception.ACRSErrorDetails;
import com.mgm.services.booking.room.model.crs.searchoffers.BodyParameterPricing;
import com.mgm.services.booking.room.model.crs.searchoffers.SuccessfulPricing;
import com.mgm.services.booking.room.model.phoenix.RoomComponent;
import com.mgm.services.booking.room.model.request.RoomComponentRequest;
import com.mgm.services.booking.room.model.reservation.RoomRequest;
import com.mgm.services.booking.room.model.response.ACRSAuthTokenResponse;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;

public class ComponentDAOStrategyACRSImplTest extends BaseRoomBookingTest {

    @Mock
    private static RestTemplate client;

    @Mock
    private static DomainProperties domainProperties;

    @Mock
    private static ApplicationProperties applicationProperties;

    @Mock
    private static RestTemplateBuilder restTemplateBuilder;

    @Mock
    private static URLProperties urlProperties;

    @Mock
    private static AcrsProperties acrsProperties;

    @Mock
    private static ReferenceDataDAOHelper referenceDataDAOHelper;

    @Mock
    private static RoomPriceDAOStrategyACRSImpl roomPriceDAOStrategyACRSImpl;
    
    @InjectMocks
    private static ACRSOAuthTokenDAOImpl acrsOAuthTokenDAOImpl;

    @InjectMocks
    private static ComponentDAOStrategyACRSImpl componentDAOStrategyACRSImpl;

    static Logger logger = LoggerFactory.getLogger(ComponentDAOStrategyACRSImplTest.class);

    /**
     * Return Retrieve Pricing from JSON mock file.
     */
    private HttpEntity<?> getCrsRetrievePricing() {
        File file = new File(getClass().getResource("/retrieve-pricing.json").getPath());
        ResponseEntity<?> response = new ResponseEntity<SuccessfulPricing>(
                convertCrs(file, SuccessfulPricing.class), HttpStatus.OK);
        return response;
    }

    private void setMockForRetrievePricing() {
        when(roomPriceDAOStrategyACRSImpl.acrsSearchOffers(ArgumentMatchers.any(BodyParameterPricing.class),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean()))
                .thenReturn((SuccessfulPricing) getCrsRetrievePricing().getBody());
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
        when(referenceDataDAOHelper.retrieveRoomTypeDetail(Mockito.anyString(),Mockito.anyString())).thenReturn("ACRS");
        when(referenceDataDAOHelper.retrieveRatePlanDetail(Mockito.anyString(), Mockito.anyString())).thenReturn("TDAAA");
        when(referenceDataDAOHelper.getAcrsVendor(Mockito.any())).thenReturn("ICECC");
    }

    @BeforeClass
    public static void init() throws SSLException, ParseException {
        client = Mockito.mock(RestTemplate.class);
        domainProperties = new DomainProperties();
        domainProperties.setCrs("");
        restTemplateBuilder = Mockito.mock(RestTemplateBuilder.class);
        applicationProperties = Mockito.mock(ApplicationProperties.class);
        referenceDataDAOHelper = Mockito.mock(ReferenceDataDAOHelper.class);
        acrsOAuthTokenDAOImpl = Mockito.mock(ACRSOAuthTokenDAOImpl.class);
        roomPriceDAOStrategyACRSImpl = Mockito.mock(RoomPriceDAOStrategyACRSImpl.class);
        acrsProperties = new AcrsProperties();
        acrsProperties.setIceUser("testIceUser");
        acrsProperties.setChainCode("testChainCode");
        acrsProperties.setDefaultSearchOfferRatePlanCode("testRate");
        acrsProperties.setLiveCRS(true);
        urlProperties = new URLProperties();
        urlProperties.setAcrsSearchOffers(
                "/hotel-platform/{acrsEnvironment}/mgm/v5/hotel/offers/searches/{acrsChainCode}/{property_code}/{ratePlanCode}?startDate={start_date}&duration={duration}");
        CommonUtil commonUtil = Mockito.spy(CommonUtil.class);
        try {
            when(commonUtil.getRetryableRestTemplate(restTemplateBuilder, applicationProperties.isSslInsecure(), acrsProperties.isLiveCRS(),
                    applicationProperties.getAcrsConnectionPerRouteDaoImpl(),
                    applicationProperties.getAcrsMaxConnectionPerDaoImpl(),
                    applicationProperties.getConnectionTimeout(),
                    applicationProperties.getReadTimeOut(),
                    applicationProperties.getSocketTimeOut(),1,applicationProperties.getCrsRestTTL())).thenReturn(client);
            componentDAOStrategyACRSImpl = new ComponentDAOStrategyACRSImpl(urlProperties, domainProperties, applicationProperties,
                    acrsProperties, restTemplateBuilder,referenceDataDAOHelper, acrsOAuthTokenDAOImpl, roomPriceDAOStrategyACRSImpl);
        } catch (SSLException e) {
            logger.error(e.getMessage());
            logger.error("Cause: " + e.getCause());
        }
    }
    

    /**
     * Check RoomComponentAvailability.
     */
    @Test
    public void getRoomComponentAvailabilitySuccessTest_NoProgramId() {

        try {
            RoomComponentRequest request = new RoomComponentRequest();
            request.setRoomTypeId("ROOMCD-v-DPRQ-d-PROP-v-MV021");
            request.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
            request.setTravelEndDate(new Date());
            request.setTravelStartDate(new Date());
            request.setSource("ICECC");

            setMockReferenceDataDAOHelper();
            setMockAuthToken();
            setMockForRetrievePricing();

            List<RoomRequest> response = componentDAOStrategyACRSImpl.getRoomComponentAvailability(request);
            Assert.assertNotNull(response);
            Assert.assertNotNull(response.get(0).getId());
            assertTrue(response.get(0).isSelected());
            assertFalse(response.get(0).isNightlyCharge());
            Assert.assertNotNull(response.get(0).getDescription());
            Assert.assertNotNull(response.get(0).getTaxRate());

            Assert.assertNotNull(response.get(1).getId());
            assertTrue(response.get(1).isSelected());
            assertTrue(response.get(1).isNightlyCharge());
            Assert.assertNotNull(response.get(1).getDescription());
            Assert.assertNotNull(response.get(1).getTaxRate());
        } catch (Exception e) {
            Assert.fail("RoomComponentAvailabilitySuccessTest Failed");
            logger.error(e.getMessage());
            logger.error("Cause: " + e.getCause());
        }
    }

    /**
     * Check RoomComponentAvailability.
     */
    @Test
    public void getRoomComponentAvailabilitySuccessTest_ProgramIdGuid() {

        try {
            RoomComponentRequest request = new RoomComponentRequest();
            request.setRoomTypeId("ROOMCD-v-DPRQ-d-PROP-v-MV021");
            request.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
            request.setProgramId("RPCD-v-TSTPREVL-d-PROP-v-MV021");
            request.setTravelEndDate(new Date());
            request.setTravelStartDate(new Date());
            request.setSource("ICECC");

            setMockReferenceDataDAOHelper();
            setMockAuthToken();
            setMockForRetrievePricing();

            List<RoomRequest> response = componentDAOStrategyACRSImpl.getRoomComponentAvailability(request);
            Assert.assertNotNull(response);
            Assert.assertNotNull(response.get(0).getId());
            assertTrue(response.get(0).isSelected());
            assertFalse(response.get(0).isNightlyCharge());
            Assert.assertNotNull(response.get(0).getDescription());
            Assert.assertNotNull(response.get(0).getTaxRate());

            Assert.assertNotNull(response.get(1).getId());
            assertTrue(response.get(1).isSelected());
            assertTrue(response.get(1).isNightlyCharge());
            Assert.assertNotNull(response.get(1).getDescription());
            Assert.assertNotNull(response.get(1).getTaxRate());

        } catch (Exception e) {
            Assert.fail("RoomComponentAvailabilitySuccessTest Failed");
            logger.error(e.getMessage());
            logger.error("Cause: " + e.getCause());
        }
    }

    /**
     * Check RoomComponentAvailability.
     */
    @Test
    public void getRoomComponentAvailabilitySuccessTest_ProgramId() {

        try {
            RoomComponentRequest request = new RoomComponentRequest();
            request.setRoomTypeId("ROOMCD-v-DPRQ-d-PROP-v-MV021");
            request.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
            request.setProgramId("RPCD-v-TSTPREVL-d-PROP-v-MV021");
            request.setTravelEndDate(new Date());
            request.setTravelStartDate(new Date());
            request.setSource("ICECC");

            setMockReferenceDataDAOHelper();
            setMockAuthToken();
            setMockForRetrievePricing();

            List<RoomRequest> response = componentDAOStrategyACRSImpl.getRoomComponentAvailability(request);
            Assert.assertNotNull(response);
            Assert.assertNotNull(response.get(0).getId());
            assertTrue(response.get(0).isSelected());
            assertFalse(response.get(0).isNightlyCharge());
            Assert.assertNotNull(response.get(0).getDescription());
            Assert.assertNotNull(response.get(0).getTaxRate());

            Assert.assertNotNull(response.get(1).getId());
            assertTrue(response.get(1).isSelected());
            assertTrue(response.get(1).isNightlyCharge());
            Assert.assertNotNull(response.get(1).getDescription());
            Assert.assertNotNull(response.get(1).getTaxRate());

        } catch (Exception e) {
            Assert.fail("RoomComponentAvailabilitySuccessTest Failed");
            logger.error(e.getMessage());
            logger.error("Cause: " + e.getCause());
        }
    }
    
    @Test
    public void getRoomComponentAvailability_Failure_Test() {

    	RoomComponentRequest request = new RoomComponentRequest();
    	request.setRoomTypeId("ROOMCD-v-DPRQ-d-PROP-v-MV021");
    	request.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
    	request.setProgramId("RPCD-v-test-d-PROP-v-MV021");
    	request.setTravelEndDate(new Date());
    	request.setTravelStartDate(new Date());
    	request.setSource("ICECC");

    	setMockReferenceDataDAOHelper();
    	setMockAuthToken();
    	setMockForRetrievePricing();

    	BusinessException businessException = new BusinessException(ErrorCode.INVALID_PROGRAM_ID); 
    	when(componentDAOStrategyACRSImpl.getRoomComponentAvailability(request)).thenThrow(businessException);

    	List<RoomRequest> response = componentDAOStrategyACRSImpl.getRoomComponentAvailability(request);

    	Assert.assertNotNull(response);
    	Assertions.assertEquals(ErrorCode.INVALID_PROGRAM_ID, businessException.getErrorCode());

    }
    
    @Test
    public void getRoomComponentByIdTest() { 	   	
		RoomComponent response = componentDAOStrategyACRSImpl.getRoomComponentById("COMPONENTCD-v-PROCESSFEE-d-TYP-v-COMPONENT-d-PROP-v-MV021-d-NRPCD-v-CCAUTHFEE");
		Assert.assertNotNull(response);
    }
    
    @Test
    public void getRoomComponentByIdTest_RoomTypeId() { 	   	
		RoomRequest response = componentDAOStrategyACRSImpl.getRoomComponentById("COMPONENTCD-v-PROCESSFEE-d-TYP-v-COMPONENT-d-PROP-v-MV021-d-NRPCD-v-CCAUTHFEE","ROOMCD-v-DPRQ-d-PROP-v-MV021");
		Assert.assertNull(response);
    }
    
    @Test
    public void getRoomComponentByCodeTest() {		
		String roomTypeId = "ROOMCD-v-DPRQ-d-PROP-v-MV021";
		String propertyId ="dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad";
		String code = "SN004";
		String ratePlanId = "RPCD-v-ELLIOTTFBPT055-d-PROP-v-MV021";
		String mlifeNumber = "33334444";
		String source = "ICE";
		Date checkInDate = new Date();  
		Date checkOutDate = new Date();  

        setMockReferenceDataDAOHelper();
        setMockAuthToken();
        setMockForRetrievePricing();
        
		RoomComponent response = componentDAOStrategyACRSImpl.getRoomComponentByCode(propertyId, code, roomTypeId,
				ratePlanId, checkInDate, checkOutDate, mlifeNumber,  source);

		Assert.assertNotNull(response);  	
    }
    
    @Test
	public void testHasError() throws IOException {
    	ComponentDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = new ComponentDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
		boolean result = errorHandler.hasError(httpResponse);
		assertTrue(result);
	}

	@Test
	public void testHandleErrorSYSTEM_ERROR() throws IOException {
		ComponentDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = new ComponentDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSErrorDetails acrsError = new ACRSErrorDetails();
		String acrsSearchOffersErrorResJson = CommonUtil.convertObjectToJsonString(acrsError);
		InputStream is = new ByteArrayInputStream(acrsSearchOffersErrorResJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.SERVICE_UNAVAILABLE);

		// Assertions
		SystemException ex = assertThrows(SystemException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.SYSTEM_ERROR, ex.getErrorCode());	
	}

	@Test
	public void testHandleErrorBusinessExceptionINVALID_PROGRAM_ID() throws IOException {
		ComponentDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = new ComponentDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSErrorDetails acrsError = new ACRSErrorDetails();
		acrsError.setCode(50006);
		String acrsSearchOffersErrorResJson = CommonUtil.convertObjectToJsonString(acrsError);
		InputStream is = new ByteArrayInputStream(acrsSearchOffersErrorResJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);

		// Assertions
		BusinessException ex = assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.INVALID_PROGRAM_ID, ex.getErrorCode());	
	}
	
	@Test
	public void testHandleErrorBusinessExceptionINVALID_DATES() throws IOException {
		ComponentDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = new ComponentDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSErrorDetails acrsError = new ACRSErrorDetails();
		acrsError.setCode(50012);
		String acrsSearchOffersErrorResJson = CommonUtil.convertObjectToJsonString(acrsError);
		InputStream is = new ByteArrayInputStream(acrsSearchOffersErrorResJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);

		// Assertions
		BusinessException ex = assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.INVALID_DATES, ex.getErrorCode());	
	}
	
	@Test
	public void testHandleErrorBusinessExceptionINVALID_CHANNEL_HEADER() throws IOException {
		ComponentDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = new ComponentDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSErrorDetails acrsError = new ACRSErrorDetails();
		acrsError.setCode(50020);
		String acrsSearchOffersErrorResJson = CommonUtil.convertObjectToJsonString(acrsError);
		InputStream is = new ByteArrayInputStream(acrsSearchOffersErrorResJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);

		// Assertions
		BusinessException ex = assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.INVALID_CHANNEL_HEADER, ex.getErrorCode());	
	}
	
	@Test
	public void testHandleErrorBusinessExceptionINVALID_PROPERTY() throws IOException {
		ComponentDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = new ComponentDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSErrorDetails acrsError = new ACRSErrorDetails();
		acrsError.setCode(50010);
		String acrsSearchOffersErrorResJson = CommonUtil.convertObjectToJsonString(acrsError);
		InputStream is = new ByteArrayInputStream(acrsSearchOffersErrorResJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);

		// Assertions
		BusinessException ex = assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.INVALID_PROPERTY, ex.getErrorCode());	
	}
	
	@Test
	public void testHandleErrorBusinessExceptionINVALID_ROOMTYPE() throws IOException {
		ComponentDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = new ComponentDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSErrorDetails acrsError = new ACRSErrorDetails();
		acrsError.setCode(50002);
		String acrsSearchOffersErrorResJson = CommonUtil.convertObjectToJsonString(acrsError);
		InputStream is = new ByteArrayInputStream(acrsSearchOffersErrorResJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);

		// Assertions
		BusinessException ex = assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.INVALID_ROOMTYPE, ex.getErrorCode());	
	}
	
	@Test
	public void testHandleErrorElse() throws IOException {
		ComponentDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = new ComponentDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSErrorDetails acrsError = new ACRSErrorDetails();
		acrsError.setCode(6000);
		acrsError.setTitle("Bad Request");
		String acrsSearchOffersErrorResJson = CommonUtil.convertObjectToJsonString(acrsError);
		InputStream is = new ByteArrayInputStream(acrsSearchOffersErrorResJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
		
		// Assertions
		BusinessException ex = assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.AURORA_FUNCTIONAL_EXCEPTION, ex.getErrorCode());	
	}
}
