package com.mgm.services.booking.room.dao.impl;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mgm.services.booking.room.service.impl.CommonServiceImpl;
import org.apache.commons.lang.time.DateUtils;
import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mgm.services.booking.room.BaseAcrsRoomBookingTest;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.exception.ACRSErrorDetails;
import com.mgm.services.booking.room.model.crs.reservation.ReservationPendingRes;
import com.mgm.services.booking.room.model.crs.searchoffers.BodyParameterPricing;
import com.mgm.services.booking.room.model.crs.searchoffers.SuccessfulPricing;
import com.mgm.services.booking.room.model.crs.searchoffers.SuccessfulSingleAvailability;
import com.mgm.services.booking.room.model.request.AuroraPriceRequest;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.model.response.AuroraPricesResponse;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;

class RoomAndComponentChargesDAOStrategyACRSImplTest extends BaseAcrsRoomBookingTest {
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
    private RoomPriceDAOStrategyACRSImpl pricingDao;
    
    @InjectMocks
    private RoomAndComponentChargesDAOStrategyACRSImpl roomAndComponentChargesDAOStrategyACRSImpl;
    @Mock
    private CommonServiceImpl commonService;
    static Logger logger = LoggerFactory.getLogger(RoomAndComponentChargesDAOStrategyACRSImplTest.class);

    private HttpEntity<?> makePendingRoomReservationResponse() {
        ResponseEntity<?> response = new ResponseEntity<ReservationPendingRes>(
                convertCrs("/acrs/createreservation/crs-create-pending.json", ReservationPendingRes.class),
                HttpStatus.CREATED);
        return response;
    }

    private HttpEntity<?> getPriceOffer() {
        ResponseEntity<?> response = new ResponseEntity<SuccessfulPricing>(
                convertCrs("/price-offer.json", SuccessfulPricing.class),
                HttpStatus.OK);
        return response;
    }

    private void setMockForCreatePendingSuccess() {
        when(client.postForEntity(ArgumentMatchers.contains("pending"), ArgumentMatchers.any(),
                ArgumentMatchers.<Class<ReservationPendingRes>>any(), Mockito.anyMap()))
                .thenReturn((ResponseEntity<ReservationPendingRes>) makePendingRoomReservationResponse());
    }

    /**
     * Return Single Availability from JSON mock file.
     */
    private HttpEntity<?> getCrsSingleRoomAvail() {
        File file = new File(getClass().getResource("/single-room-avail_groupBlockCode.json").getPath());
        ResponseEntity<?> response = new ResponseEntity<SuccessfulSingleAvailability>(
                convertCrs(file, SuccessfulSingleAvailability.class), HttpStatus.OK);
        return response;
    }

    private void setMockForPriceOfferSuccess() {
        when(client.exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<SuccessfulPricing>>any(), Mockito.anyMap()))
                .thenReturn((ResponseEntity<SuccessfulPricing>) getPriceOffer());
    }

    @BeforeEach
    public void init() {
        super.init();
        client = Mockito.mock(RestTemplate.class);
        domainProperties = new DomainProperties();
        domainProperties.setCrs("");
        restTemplateBuilder = Mockito.mock(RestTemplateBuilder.class);
        applicationProperties = Mockito.mock(ApplicationProperties.class);
        referenceDataDAOHelper = Mockito.mock(ReferenceDataDAOHelper.class);
        acrsOAuthTokenDAOImpl = Mockito.mock(ACRSOAuthTokenDAOImpl.class);
        pricingDao = Mockito.mock(RoomPriceDAOStrategyACRSImpl.class);
        commonService= Mockito.mock(CommonServiceImpl.class);
        acrsProperties = new AcrsProperties();
        acrsProperties.setModifySpecialRequestPath("modifySpecialRequestPath");
        acrsProperties.setModifyPartyConfirmationNumberPath("modifyPartyConfirmationNumberPath");
        acrsProperties.setLiveCRS(true);
        acrsProperties.setMaxAcrsCommentLength(51);
        acrsProperties.setDefaultBasePriceRatePlan("PREVL");
        acrsProperties.setSuppresWebComponentPatterns(Arrays.asList("ICE_","_ICE"));
        acrsProperties.setWhiteListMarketCodeList(Arrays.asList("CBRANCH","CE","CINT","CINTR","CPKE","CPKR","CSLA","CSLE","CSLI","CSLP","CSLX","CSTX","CTGA","CTGE","CTGI","CTGP","CTGX","OERS","ONWN","OOWN","ORWN","TCOR","TENT","TFIT","TPKG","TSMG","CO"));
        Set<String> allowedTiltleList = new HashSet<>();
        allowedTiltleList.addAll(Arrays.asList("Mr","Mrs","Ms","Dr","Madam","Miss","Other"));
        acrsProperties.setAllowedTitleList(allowedTiltleList);
        urlProperties = new URLProperties();
        MockHttpServletRequest request = new MockHttpServletRequest(); 
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        urlProperties.setAcrsReservationsCreatePending(
                "/hotel-platform/{AcrsEnvironment}/mgm/{AcrsReservationsVersion}/hotel/reservations/{acrsChainCode}/pending");
        urlProperties.setAcrsReservationsConfCommit(
                "/hotel-platform/{AcrsEnvironment}/mgm/{AcrsReservationsVersion}/hotel/reservations/{acrsChainCode}/{confirmationNumber}/commit");
        urlProperties.setAcrsReservationsConfPending(
                "/hotel-platform/{acrsEnvironment}/mgm/{acrsVersion}/hotel/reservations/{acrsChainCode}/confirmationNumber/pending");
        CommonUtil commonUtil = Mockito.spy(CommonUtil.class);
        when(commonUtil.getRetryableRestTemplate(restTemplateBuilder, applicationProperties.isSslInsecure(),
                acrsProperties.isLiveCRS(),applicationProperties.getAcrsConnectionPerRouteDaoImpl(),
                applicationProperties.getAcrsMaxConnectionPerDaoImpl(),
                applicationProperties.getConnectionTimeout(),
                applicationProperties.getReadTimeOut(),
                applicationProperties.getSocketTimeOut(),1,applicationProperties.getCrsRestTTL())).thenReturn(client);
        doNothing().when(commonService).checkBookingLimitApplied(Mockito.any());
        roomAndComponentChargesDAOStrategyACRSImpl = new RoomAndComponentChargesDAOStrategyACRSImpl(urlProperties,
                domainProperties, applicationProperties, acrsProperties, restTemplateBuilder, referenceDataDAOHelper, acrsOAuthTokenDAOImpl, pricingDao);
    }

    @Test
    void getRoomAndComponentChargeForTransientSuccess() {
        try {
            // Setup Mocks
            setMockForRoomPropertyCode();
            setMockAuthToken();
            // pending success
            setMockForCreatePendingSuccess();

            // Mock on pricingDao.getSearchOffers that returns a SuccessfulPricing Object
            when(pricingDao.acrsSearchOffers(ArgumentMatchers.any(BodyParameterPricing.class),
                    ArgumentMatchers.any(String.class), ArgumentMatchers.any(String.class),
                    ArgumentMatchers.any(String.class), ArgumentMatchers.any(String.class),
                    ArgumentMatchers.any(String.class), ArgumentMatchers.any(boolean.class)))
                    .thenReturn((SuccessfulPricing) getPriceOffer().getBody());

            Date checkInDate = fmt.parse("2021-05-06");
            Date checkOutDate = fmt.parse("2021-05-08");
            RoomReservation request = createRoomReservation(checkInDate, checkOutDate);
            request.setBookings(null);
            mockPriceCall();
            RoomReservation reservation = roomAndComponentChargesDAOStrategyACRSImpl.calculateRoomAndComponentCharges(request);
            Assertions.assertNotNull(reservation);
            Assertions.assertTrue(DateUtils.isSameDay(request.getCheckInDate(), reservation.getCheckInDate()));
            Assertions.assertTrue(DateUtils.isSameDay(request.getCheckOutDate(), reservation.getCheckOutDate()));
            Assertions.assertNotNull(reservation.getAvailableComponents());
            Assertions.assertEquals(25.0, reservation.getAvailableComponents().get(0).getPrices().get(0).getAmount(), 0);
        } catch (Exception e) {
            logger.error(e.getMessage());
            logger.error("Caused " + e.getCause());
            Assertions.fail("getRoomAndComponentChargeForTransientSuccess Failed");
        }
    }

    @Test
    void getRoomAndComponentChargeForMlifeSuccess() {
        try {
            setMockForRoomPropertyCode();
            setMockAuthToken();
            // pending success
            setMockForCreatePendingSuccess();
            // Mock on pricingDao.getSearchOffers that returns a SuccessfulPricing Object
            when(pricingDao.acrsSearchOffers(ArgumentMatchers.any(BodyParameterPricing.class),
                    ArgumentMatchers.any(String.class), ArgumentMatchers.any(String.class),
                    ArgumentMatchers.any(String.class), ArgumentMatchers.any(String.class),
                    ArgumentMatchers.any(String.class), ArgumentMatchers.any(boolean.class)))
                    .thenReturn((SuccessfulPricing) getPriceOffer().getBody());
            doNothing().when(commonService).checkBookingLimitApplied(Mockito.any());
            mockPriceCall();

            Date checkInDate = fmt.parse("2020-11-29");
            Date checkOutDate = fmt.parse("2020-11-30");
            String propertyId = "dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad";
            String roomTypeId = "KNGN";
            String programId = "RPCD-v-COMPS001-d-PROP-v-MV021";
            List<String> specialRequests = new ArrayList<>();
            RoomReservation request = createRoomReservation(checkInDate, checkOutDate, propertyId, roomTypeId,
                    programId, specialRequests);
            // Set additional parameters on request
            request.setPerpetualPricing(true);
            request.getProfile().setMlifeNo(123456);
            request.setBookings(null);
            // Send request
            ReflectionTestUtils.setField(roomAndComponentChargesDAOStrategyACRSImpl,"commonService",commonService);
            RoomReservation reservation = roomAndComponentChargesDAOStrategyACRSImpl.calculateRoomAndComponentCharges(request);
            Assertions.assertNotNull(reservation);
            Assertions.assertTrue(DateUtils.isSameDay(request.getCheckInDate(), reservation.getCheckInDate()));
            Assertions.assertNotNull(reservation.getAvailableComponents());
            Assertions.assertEquals(25.0, reservation.getAvailableComponents().get(0).getPrices().get(0).getAmount(),
                    0);
            Assertions.assertEquals(123456, reservation.getProfile().getMlifeNo(), 0);
        } catch (Exception e) {
            logger.error(e.getMessage());
            logger.error("Caused " + e.getCause());
            logger.error("Caused " + e.getCause());
            Assertions.fail("getRoomAndComponentChargeForMlifeSuccess Failed :"+e.getMessage());
        }
    }

    private void mockPriceCall() throws ParseException {
        // Mock pricingDao.getRoomPricesV2
        AuroraPricesResponse pricingResponse = new AuroraPricesResponse();
        List<AuroraPriceResponse> auroraPrices = new ArrayList<>();
        AuroraPriceResponse auroraPriceResponse = new AuroraPriceResponse();
        auroraPriceResponse.setProgramId("TestPrg");
        auroraPriceResponse.setRoomTypeId("KNGN");
        auroraPriceResponse.setDate(fmt.parse("2020-11-29"));
        auroraPriceResponse.setBasePrice(1);
        auroraPriceResponse.setDiscountedPrice(1);
        auroraPriceResponse.setResortFee(1);
        auroraPrices.add(auroraPriceResponse);
        pricingResponse.setAuroraPrices(auroraPrices);

        when(pricingDao.getRoomPricesV2(Mockito.any())).thenReturn(pricingResponse);
    }

    @Test
    void holdReservationWithGroupBlockForTransientSuccess() {
        try {
            // Setup Mocks
            setMockForRoomPropertyCode();
            setMockAuthToken();
            // Mock on pricingDao.getSearchOffers that returns a SuccessfulPricing Object
            when(pricingDao.acrsSearchOffers(ArgumentMatchers.any(BodyParameterPricing.class),
                    ArgumentMatchers.any(String.class), ArgumentMatchers.any(String.class),
                    ArgumentMatchers.any(String.class), ArgumentMatchers.any(String.class),
                    ArgumentMatchers.any(String.class), ArgumentMatchers.any(boolean.class)))
                    .thenReturn((SuccessfulPricing) getPriceOffer().getBody());
            mockPriceCall();
            when(pricingDao.getAcrsSingleAvailability(ArgumentMatchers.any(AuroraPriceRequest.class)))
                    .thenReturn((SuccessfulSingleAvailability) getCrsSingleRoomAvail().getBody());

            Date checkInDate = fmt.parse("2022-06-14");
            Date checkOutDate = fmt.parse("2022-06-15");
            RoomReservation request = createRoomReservation(checkInDate, checkOutDate);
            request.setProgramId("GRPCD-v-ZAERO614P1HDE-d-PROP-v-MV275"); // group block
            request.setBookings(null);

            RoomReservation reservation = roomAndComponentChargesDAOStrategyACRSImpl.calculateRoomAndComponentCharges(request);
            Assertions.assertNotNull(reservation);
            Assertions.assertTrue(DateUtils.isSameDay(request.getCheckInDate(), reservation.getCheckInDate()));
            Assertions.assertTrue(DateUtils.isSameDay(request.getCheckOutDate(), reservation.getCheckOutDate()));
            Assertions.assertNotNull(reservation.getAvailableComponents());
            Assertions.assertEquals(reservation.getBookings().get(0).getProgramId(),"ZAERO614P1HDE");
            Assertions.assertTrue(reservation.getIsGroupCode());
            Assertions.assertEquals(25.0, reservation.getAvailableComponents().get(0).getPrices().get(0).getAmount(), 0);
        } catch (Exception e) {
            logger.error("holdReservationWithGroupBlockForTransientSuccess failed due to exception: ", e);
            Assertions.fail("holdReservationWithGroupBlockForTransientSuccess Failed");
        }
    }
    
    @Test
    void holdAPI_SupressNonPublicComponent_EmptyList_Success() {
        try {
            // Setup Mocks
            setMockForRoomPropertyCode();
            setMockAuthToken();
            // pending success
            setMockForCreatePendingSuccess();

            // Mock on pricingDao.getSearchOffers that returns a SuccessfulPricing Object
            when(pricingDao.acrsSearchOffers(ArgumentMatchers.any(BodyParameterPricing.class),
                    ArgumentMatchers.any(String.class), ArgumentMatchers.any(String.class),
                    ArgumentMatchers.any(String.class), ArgumentMatchers.any(String.class),
                    ArgumentMatchers.any(String.class), ArgumentMatchers.any(boolean.class)))
                    .thenReturn((SuccessfulPricing) getPriceOffer().getBody());
            changePropertyValue(Collections.emptyList());
            Date checkInDate = fmt.parse("2021-05-06");
            Date checkOutDate = fmt.parse("2021-05-08");
            RoomReservation request = createRoomReservation(checkInDate, checkOutDate);
            request.setBookings(null);
            mockPriceCall();
            RoomReservation reservation = roomAndComponentChargesDAOStrategyACRSImpl.calculateRoomAndComponentCharges(request);
            Assertions.assertNotNull(reservation);
            Assertions.assertTrue(DateUtils.isSameDay(request.getCheckInDate(), reservation.getCheckInDate()));
            Assertions.assertTrue(DateUtils.isSameDay(request.getCheckOutDate(), reservation.getCheckOutDate()));
            Assertions.assertNotNull(reservation.getAvailableComponents());
            Assertions.assertEquals(25.0, reservation.getAvailableComponents().get(0).getPrices().get(0).getAmount(), 0);
            Assertions.assertEquals(4, reservation.getAvailableComponents().size());
        } catch (Exception e) {
            logger.error(e.getMessage());
            logger.error("Caused " + e.getCause());
            Assertions.fail("getRoomAndComponentChargeForTransientSuccess Failed");
        }
    }
    
    @Test
    void holdAPI_SupressNonPublicComponent_ValueMatching_Success() {
        try {
            // Setup Mocks
            setMockForRoomPropertyCode();
            setMockAuthToken();
            // pending success
            setMockForCreatePendingSuccess();

            // Mock on pricingDao.getSearchOffers that returns a SuccessfulPricing Object
            when(pricingDao.acrsSearchOffers(ArgumentMatchers.any(BodyParameterPricing.class),
                    ArgumentMatchers.any(String.class), ArgumentMatchers.any(String.class),
                    ArgumentMatchers.any(String.class), ArgumentMatchers.any(String.class),
                    ArgumentMatchers.any(String.class), ArgumentMatchers.any(boolean.class)))
                    .thenReturn((SuccessfulPricing) getPriceOffer().getBody());
            changePropertyValue(Arrays.asList("PREVL"));
            Date checkInDate = fmt.parse("2021-05-06");
            Date checkOutDate = fmt.parse("2021-05-08");
            RoomReservation request = createRoomReservation(checkInDate, checkOutDate);
            request.setBookings(null);
            mockPriceCall();
            RoomReservation reservation = roomAndComponentChargesDAOStrategyACRSImpl.calculateRoomAndComponentCharges(request);
            Assertions.assertNotNull(reservation);
            Assertions.assertTrue(DateUtils.isSameDay(request.getCheckInDate(), reservation.getCheckInDate()));
            Assertions.assertTrue(DateUtils.isSameDay(request.getCheckOutDate(), reservation.getCheckOutDate()));
            Assertions.assertNotNull(reservation.getAvailableComponents());
            Assertions.assertEquals(27.0, reservation.getAvailableComponents().get(0).getPrices().get(0).getAmount(), 0);
            Assertions.assertFalse(reservation.getAvailableComponents().stream().filter(o -> o.getId().contains("PREVL")).findFirst().isPresent());
            Assertions.assertEquals(2, reservation.getAvailableComponents().size());
        } catch (Exception e) {
            logger.error(e.getMessage());
            logger.error("Caused " + e.getCause());
            Assertions.fail("getRoomAndComponentChargeForTransientSuccess Failed");
        }
    }
    
    @Test
    void holdAPI_SupressNonPublicComponent_NullPropertyVal_Success() {
        try {
            // Setup Mocks
            setMockForRoomPropertyCode();
            setMockAuthToken();
            // pending success
            setMockForCreatePendingSuccess();

            // Mock on pricingDao.getSearchOffers that returns a SuccessfulPricing Object
            when(pricingDao.acrsSearchOffers(ArgumentMatchers.any(BodyParameterPricing.class),
                    ArgumentMatchers.any(String.class), ArgumentMatchers.any(String.class),
                    ArgumentMatchers.any(String.class), ArgumentMatchers.any(String.class),
                    ArgumentMatchers.any(String.class), ArgumentMatchers.any(boolean.class)))
                    .thenReturn((SuccessfulPricing) getPriceOffer().getBody());
            changePropertyValue(null);
            Date checkInDate = fmt.parse("2021-05-06");
            Date checkOutDate = fmt.parse("2021-05-08");
            RoomReservation request = createRoomReservation(checkInDate, checkOutDate);
            request.setBookings(null);
            mockPriceCall();
            RoomReservation reservation = roomAndComponentChargesDAOStrategyACRSImpl.calculateRoomAndComponentCharges(request);
            Assertions.assertNotNull(reservation);
            Assertions.assertTrue(DateUtils.isSameDay(request.getCheckInDate(), reservation.getCheckInDate()));
            Assertions.assertTrue(DateUtils.isSameDay(request.getCheckOutDate(), reservation.getCheckOutDate()));
            Assertions.assertNotNull(reservation.getAvailableComponents());
            Assertions.assertEquals(25.0, reservation.getAvailableComponents().get(0).getPrices().get(0).getAmount(), 0);
            Assertions.assertEquals(4, reservation.getAvailableComponents().size());
        } catch (Exception e) {
            logger.error(e.getMessage());
            logger.error("Caused " + e.getCause());
            Assertions.fail("getRoomAndComponentChargeForTransientSuccess Failed");
        }
    }
    
    public void changePropertyValue(List<String> propertyValue) {
    	acrsProperties.setSuppresWebComponentPatterns(propertyValue);
    	roomAndComponentChargesDAOStrategyACRSImpl = new RoomAndComponentChargesDAOStrategyACRSImpl(urlProperties,
                domainProperties, applicationProperties, acrsProperties, restTemplateBuilder, referenceDataDAOHelper, acrsOAuthTokenDAOImpl, pricingDao);
    }

    
    @Test
    void testHandleErrorBusinessExceptionDATES_UNAVAILABLE() throws IOException {
    	RoomAndComponentChargesDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = 
    			new RoomAndComponentChargesDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSErrorDetails acrsError = new ACRSErrorDetails();
		acrsError.setTitle("<UnableToPriceTrip>");
		String acrsErrorJson = CommonUtil.convertObjectToJsonString(acrsError);
		InputStream is = new ByteArrayInputStream(acrsErrorJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);

		// Assertions
		BusinessException ex = assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.DATES_UNAVAILABLE, ex.getErrorCode());
	}
    
    @Test
    void testHandleErrorBusinessExceptionRESERVATION_NOT_FOUND() throws IOException {
    	RoomAndComponentChargesDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = 
    			new RoomAndComponentChargesDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSErrorDetails acrsError = new ACRSErrorDetails();
		acrsError.setTitle("<BookingNotFound>");
		String acrsErrorJson = CommonUtil.convertObjectToJsonString(acrsError);
		InputStream is = new ByteArrayInputStream(acrsErrorJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);

		// Assertions
		BusinessException ex = assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.RESERVATION_NOT_FOUND, ex.getErrorCode());
	}
    
    @Test
    void testHandleErrorBusinessExceptionElse() throws IOException {
    	RoomAndComponentChargesDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = 
    			new RoomAndComponentChargesDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSErrorDetails acrsError = new ACRSErrorDetails();
		acrsError.setTitle("Some Error Occured");
		String acrsErrorJson = CommonUtil.convertObjectToJsonString(acrsError);
		InputStream is = new ByteArrayInputStream(acrsErrorJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.CONTINUE);

		// Assertions
		BusinessException ex = assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.AURORA_FUNCTIONAL_EXCEPTION, ex.getErrorCode());
	}
    
    @Test
    void testHandleErrorSystemExceptionSYSTEM_ERROR() throws IOException {
    	RoomAndComponentChargesDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = 
    			new RoomAndComponentChargesDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSErrorDetails acrsError = new ACRSErrorDetails();
		String acrsErrorJson = CommonUtil.convertObjectToJsonString(acrsError);
		InputStream is = new ByteArrayInputStream(acrsErrorJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.SERVICE_UNAVAILABLE);
		
		// Assertions
		SystemException ex = assertThrows(SystemException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.SYSTEM_ERROR, ex.getErrorCode());
	}
    
    @Test
	void testHasError() throws IOException {
    	RoomAndComponentChargesDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = 
    			new RoomAndComponentChargesDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
		boolean result = errorHandler.hasError(httpResponse);
		assertTrue(result);
	}
}
