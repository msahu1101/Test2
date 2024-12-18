package com.mgm.services.booking.room.dao.impl;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import javax.net.ssl.SSLException;

import com.mgm.services.booking.room.util.ReservationUtil;
import org.assertj.core.util.VisibleForTesting;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
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
import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.dao.PaymentDAO;
import com.mgm.services.booking.room.dao.RefDataDAO;
import com.mgm.services.booking.room.model.crs.reservation.ReservationRetrieveResReservation;
import com.mgm.services.booking.room.model.crs.reservation.ReservationSearchResPostSearchReservations;
import com.mgm.services.booking.room.model.refdata.AlertAndTraceSearchRefDataRes;
import com.mgm.services.booking.room.model.request.FindReservationV2Request;
import com.mgm.services.booking.room.model.reservation.ReservationState;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.TokenResponse;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;

class FindReservationDAOStrategyACRSImplTest extends BaseAcrsRoomBookingTest {

	private static RestTemplate client;
	private static RefDataDAO refDataDAO;

	private static DomainProperties domainProperties;

	private static ApplicationProperties applicationProperties;

	private static RestTemplateBuilder restTemplateBuilder;

	private static URLProperties urlProperties;

	private static AcrsProperties acrsProperties;

	private static FindReservationDAOStrategyACRSImpl findReservationDAOStrategyACRSImpl;

	private static RoomPriceDAOStrategyACRSImpl roomPriceDAOStrategyACRSImpl;

	private static IDMSTokenDAO idmsTokenDAO;

    @InjectMocks
    private static PaymentDAO paymentDao;

	static Logger logger = LoggerFactory.getLogger(FindReservationDAOStrategyACRSImplTest.class);

	@VisibleForTesting
	String getEnvironmentVariable(String envVar) {
		return System.getenv(envVar);
	}
	@BeforeEach
	public void init() {
		super.init();
		client = Mockito.mock(RestTemplate.class);
		idmsTokenDAO = Mockito.mock(IDMSTokenDAO.class);
		 paymentDao = Mockito.mock(PaymentDAO.class);
		domainProperties = new DomainProperties();
		domainProperties.setCrs("");
		refDataDAO = Mockito.mock(RefDataDAO.class);
		restTemplateBuilder = Mockito.mock(RestTemplateBuilder.class);
		applicationProperties = Mockito.mock(ApplicationProperties.class);
		applicationProperties.setPermanentInfoLogEnabled(true);
		roomPriceDAOStrategyACRSImpl = Mockito.mock(RoomPriceDAOStrategyACRSImpl.class);
		acrsProperties = new AcrsProperties();
		acrsProperties.setLiveCRS(true);
		acrsProperties.setWhiteListMarketCodeList(Arrays.asList("CBRANCH","CE","CINT","CINTR","CPKE","CPKR","CSLA","CSLE","CSLI","CSLP","CSLX","CSTX","CTGA","CTGE","CTGI","CTGP","CTGX","OERS","ONWN","OOWN","ORWN","TCOR","TENT","TFIT","TPKG","TSMG","CO"));
		urlProperties = new URLProperties();
		urlProperties.setAcrsRetrieveReservation("/hotel-platform/cit/mgm/v6/hotel/reservations/MGM/{cfNumber}");
		applicationProperties.setCrsUcpRetrieveResvEnvironment("test");
		domainProperties.setCrsUcpRetrieveResv("");
		urlProperties.setCrsUcpRetrieveResvUrl("/hotel-platform/cit/mgm/v6/hotel/reservations/MGM/{cfNumber}");
		urlProperties.setAcrsSearchReservations("/hotel-platform/cit/mgm/v6/hotel/reservations/MGM/search");
		CommonUtil commonUtil = Mockito.spy(CommonUtil.class);
		when(commonUtil.getRetryableRestTemplate(restTemplateBuilder, applicationProperties.isSslInsecure(),
				acrsProperties.isLiveCRS(),applicationProperties.getAcrsConnectionPerRouteDaoImpl(),
				applicationProperties.getAcrsMaxConnectionPerDaoImpl(),
				applicationProperties.getConnectionTimeout(),
				applicationProperties.getReadTimeOut(),
				applicationProperties.getSocketTimeOut(),1, applicationProperties.getCrsRestTTL())).thenReturn(client);
		try {
			findReservationDAOStrategyACRSImpl = new FindReservationDAOStrategyACRSImpl(urlProperties, domainProperties,
			applicationProperties, acrsProperties, restTemplateBuilder,
			referenceDataDAOHelper, acrsOAuthTokenDAOImpl, roomPriceDAOStrategyACRSImpl);
		} catch (SSLException e) {
			throw new RuntimeException(e);
		}
		findReservationDAOStrategyACRSImpl.setIdmsTokenDAO(idmsTokenDAO);
		  findReservationDAOStrategyACRSImpl.setPaymentDao(paymentDao);
		findReservationDAOStrategyACRSImpl.setRefDataDAO(refDataDAO);
	}

	/**
	 * Return retrieve reservation from JSON mock file.
	 */
	private HttpEntity<?> getCrsRetrieveReservation() {
		File file = new File(getClass().getResource("/retrieve_reservation_shared.json").getPath());
		ResponseEntity<?> response = new ResponseEntity<>(
				convertCrs(file, ReservationRetrieveResReservation.class), HttpStatus.OK);
		return response;

	}
	private ResponseEntity<AlertAndTraceSearchRefDataRes> getRefDataResponse(String jsonFileName){
		return new ResponseEntity<>(
				convertCrs(jsonFileName, AlertAndTraceSearchRefDataRes.class),
				HttpStatus.OK);
	}
	private void mockForRefData(String fileName){
		ResponseEntity<AlertAndTraceSearchRefDataRes> response = getRefDataResponse(fileName);
		setMockForGetRefDataSuccess(response);
	}
	private void setMockForGetRefDataSuccess(ResponseEntity<AlertAndTraceSearchRefDataRes> response ) {
		when(refDataDAO.searchRefDataEntity(ArgumentMatchers.any())).thenReturn(response.getBody());
	}

	private void setMockForRetrieveReservation() {
		when(client.exchange(ArgumentMatchers.contains("/exchangetoken"), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationRetrieveResReservation>>any(),
				Mockito.anyMap()))
						.thenReturn((ResponseEntity<ReservationRetrieveResReservation>) getCrsRetrieveReservation());
	}

	/**
	 * Return search reservation from JSON mock file.
	 */
	private HttpEntity<?> getCrsSearchReservation() {
		File file = new File(getClass().getResource("/search_reservation_shared.json").getPath());
		ResponseEntity<?> response = new ResponseEntity<ReservationSearchResPostSearchReservations>(
				convertCrs(file, ReservationSearchResPostSearchReservations.class), HttpStatus.OK);
		return response;
	}

	private ResponseEntity<ReservationSearchResPostSearchReservations> getAcrsSearchReservationNotFound() {
		return new ResponseEntity<>(convertCrs("/search_reservation_not_found.json",
				ReservationSearchResPostSearchReservations.class), HttpStatus.OK);
	}

	private void setMockForSearchReservation() {
		when(client.exchange(ArgumentMatchers.contains("search"), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationSearchResPostSearchReservations>>any(),
				Mockito.anyMap())).thenReturn(
						(ResponseEntity<ReservationSearchResPostSearchReservations>) getCrsSearchReservation());
	}	

	private FindReservationV2Request createRequest() {
		FindReservationV2Request request = new FindReservationV2Request();
		request.setSource("mgmresorts");
		request.setCustomerId(23458789);
		request.setPropertyId("ARIA1");
		request.setConfirmationNumber("1234");
		return request;
	}

	@Test
	void testFindReservation_shared() {
		try {
			setMockAuthToken();
			setMockForRoomPropertyCode();
			ReservationRetrieveResReservation reservationPendingRes = (ReservationRetrieveResReservation) getCrsRetrieveReservation().getBody();
	            Mockito.doReturn(reservationPendingRes).when(paymentDao)
	            .sendRetrieveRequestToPaymentExchangeToken(ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any());
	            // search res
			setMockForSearchReservation();
			// mock for refData(traces and special request) API call
			mockForRefData("/acrs/createreservation/response/refData-response.json");

			setPropertyTaxAndChargesConfig(acrsProperties);
			TokenResponse tknRes = new TokenResponse();
	        tknRes.setAccessToken("1234");
	        Mockito.doReturn(tknRes).when(idmsTokenDAO).generateToken();       
			FindReservationV2Request request = createRequest();
			RoomReservation reservation = findReservationDAOStrategyACRSImpl.findRoomReservation(request);
			Assertions.assertNotNull(reservation);
			Assertions.assertNotNull(reservation.getConfirmationNumber());
			Assertions.assertEquals(ReservationState.Cancelled, reservation.getState());
			Assertions.assertEquals("3938152666", reservation.getPrimarySharerConfirmationNumber());
			Assertions.assertEquals("40IOQ8FYU3", reservation.getShareId());
			Assertions.assertEquals("5498755564", reservation.getShareWiths()[1]);
			Assertions.assertEquals("Full", reservation.getShareWithType().name());
		} catch (Exception e) {
			logger.error(e.getMessage());
			Assertions.fail("find reservation Failed");
		}
	}

	@Test
	void searchReservationNotFound() {
		setMockAuthToken();
		setMockForRoomPropertyCode();
		FindReservationV2Request request = createRequest();

		// search res
		when(client.exchange(ArgumentMatchers.endsWith(urlProperties.getAcrsSearchReservations()),
				ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(HttpEntity.class),
				(Class) ArgumentMatchers.any(),
				(Map<String, String>) ArgumentMatchers.any()))
				.thenReturn(getAcrsSearchReservationNotFound());
		try {
			String confirmationNo =
					findReservationDAOStrategyACRSImpl.searchRoomReservationByExternalConfirmationNo(request);
			logger.error("searchReservation should have failed and instead returned: {}", confirmationNo);
			Assertions.fail("Exception expected to be thrown during findRoomReservation() call.");
		} catch (BusinessException businessException) {
			Assertions.assertEquals(ErrorCode.RESERVATION_NOT_FOUND, businessException.getErrorCode());
		}
	}
	
	// Test case for payment widget changes - paymentTxnId in response
	@Test
	void testFindReservation_skipPaymentProcess() {
		try {
			setMockAuthToken();
			setMockForRoomPropertyCode();
			ReservationRetrieveResReservation reservationPendingRes = (ReservationRetrieveResReservation) getCrsRetrieveReservation().getBody();
	            Mockito.doReturn(reservationPendingRes).when(paymentDao)
	            .sendRetrieveRequestToPaymentExchangeToken(ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any());
	            
			setMockForSearchReservation();

			setPropertyTaxAndChargesConfig(acrsProperties);
			TokenResponse tknRes = new TokenResponse();
	        tknRes.setAccessToken("1234");
	        Mockito.doReturn(tknRes).when(idmsTokenDAO).generateToken();       
			FindReservationV2Request request = createRequest();
			RoomReservation reservation = findReservationDAOStrategyACRSImpl.findRoomReservation(request);
			Assertions.assertNotNull(reservation);
			Assertions.assertNotNull(reservation.getConfirmationNumber());
			//Payment txn Id check
			Assertions.assertNotNull(reservation.getPayments().get(0).getPaymentTxnId());
		} catch (Exception e) {
			logger.error(e.getMessage());
			Assertions.fail("Find reservation Failed "+e.getMessage());
		}
	}

	@Test
	void testHandleErrorSYSTEM_ERROR() throws IOException {
		FindReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = 
				new FindReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		when(httpResponse.getBody()).thenReturn(null);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.SERVICE_UNAVAILABLE);

		// Assertions
		SystemException ex = assertThrows(SystemException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.SYSTEM_ERROR, ex.getErrorCode());
	}
	
	@Test
	void testHandleErrorINVALID_CARDINFO() throws IOException {
		FindReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = 
				new FindReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		String body = "<_invalid_cardInfo>";
		when(httpResponse.getBody()).thenReturn( new ByteArrayInputStream(body.getBytes()));
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);

		// Assertions
		BusinessException ex = assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.INVALID_CARDINFO, ex.getErrorCode());
	}
	
	@Test
	void testHandleErrorRESERVATION_BLACKLISTED() throws IOException {
		FindReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = 
				new FindReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		String body = "14021";
		when(httpResponse.getBody()).thenReturn( new ByteArrayInputStream(body.getBytes()));
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);

		// Assertions
		BusinessException ex = assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.RESERVATION_BLACKLISTED, ex.getErrorCode());
	}
	
	@Test
	void testHandleErrorRESERVATION_NOT_FOUND() throws IOException {
		FindReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = 
				new FindReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		String body = "<BookingNotFound>";
		when(httpResponse.getBody()).thenReturn( new ByteArrayInputStream(body.getBytes()));
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);

		// Assertions
		BusinessException ex = assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.RESERVATION_NOT_FOUND, ex.getErrorCode());
	}
	
	@Test
	void testHandleErrorElse() throws IOException {
		FindReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = 
				new FindReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		when(httpResponse.getBody()).thenReturn(null);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);

		// Assertions
		BusinessException ex = assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.RESERVATION_NOT_FOUND, ex.getErrorCode());
	}
	
	@Test
	void testHasError() throws IOException {
		FindReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = 
				new FindReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
		boolean result = errorHandler.hasError(httpResponse);
		
		// Assertions
		assertTrue(result);
	}
}
