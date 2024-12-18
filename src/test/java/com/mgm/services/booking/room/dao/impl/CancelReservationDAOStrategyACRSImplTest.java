package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.BaseAcrsRoomBookingTest;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.dao.PaymentDAO;
import com.mgm.services.booking.room.exception.ACRSErrorDetails;
import com.mgm.services.booking.room.mapper.RoomReservationPendingResMapper;
import com.mgm.services.booking.room.mapper.RoomReservationPendingResMapperImpl;
import com.mgm.services.booking.room.model.crs.reservation.*;
import com.mgm.services.booking.room.model.paymentservice.AuthResponse;
import com.mgm.services.booking.room.model.paymentservice.CaptureResponse;
import com.mgm.services.booking.room.model.paymentservice.RefundResponse;
import com.mgm.services.booking.room.model.request.CancelV2Request;
import com.mgm.services.booking.room.model.request.ReleaseV2Request;
import com.mgm.services.booking.room.model.reservation.*;
import com.mgm.services.booking.room.model.response.TokenResponse;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.transformer.RoomReservationTransformer;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.PropertyConfig;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.*;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Implementation class for CancelReservationDAOStrategy for all services
 * related to room reservation cancellation.
 */

public class CancelReservationDAOStrategyACRSImplTest extends BaseAcrsRoomBookingTest {

	private static RestTemplate client;

	private static DomainProperties domainProperties;

	private static RestTemplateBuilder restTemplateBuilder;

	private static AcrsProperties acrsProperties;

	private static CancelReservationDAOStrategyACRSImpl cancelReservationDAOStrategyACRSImpl;

	private static RoomPriceDAOStrategyACRSImpl roomPriceDAOStrategyACRSImpl;

	private static RoomReservationPendingResMapper pendingResMapper;

	private static IDMSTokenDAO idmsTokenDAO;

	private static PaymentDAO paymentDao;

	private static PropertyConfig propertyConfig;

	static Logger logger = LoggerFactory.getLogger(CancelReservationDAOStrategyACRSImplTest.class);

	@BeforeEach
	@Override
	public void init() {
		super.init();
		Configurator.setAllLevels("", Level.ALL);
		client = Mockito.mock(RestTemplate.class);
		idmsTokenDAO = Mockito.mock(IDMSTokenDAO.class);
		paymentDao = Mockito.mock(PaymentDAO.class);
		domainProperties = new DomainProperties();
		domainProperties.setCrs("");
		restTemplateBuilder = Mockito.mock(RestTemplateBuilder.class);
		applicationProperties.setCrsUcpRetrieveResvEnvironment("q");
		applicationProperties.setTimezone(Collections.singletonMap("MV021", "America/Los_Angeles"));
		pendingResMapper = Mockito.mock(RoomReservationPendingResMapper.class);
		roomPriceDAOStrategyACRSImpl = Mockito.mock(RoomPriceDAOStrategyACRSImpl.class);
		propertyConfig = Mockito.mock(PropertyConfig.class);
		acrsProperties = new AcrsProperties();
		acrsProperties.setModifySpecialRequestPath("modifySpecialRequestPath");
		acrsProperties.setModifyPartyConfirmationNumberPath("modifyPartyConfirmationNumberPath");
		acrsProperties.setLiveCRS(true);
		List<String> pseudoExceptionPropertiesList = new ArrayList<>();
		pseudoExceptionPropertiesList.add("ACRS");
		acrsProperties.setPseudoExceptionProperties(null);
		urlProperties.setCrsUcpRetrieveResvUrl("/v1/crs/reservation/retrieve?confirmationNumber={confirmationNumber}");
		urlProperties.setAcrsReservationsConfCommit(
				"/hotel-platform/{AcrsEnvironment}/mgm/{AcrsReservationsVersion}/hotel/reservations/{acrsChainCode}/{confirmationNumber}/commit");
		urlProperties.setAcrsRetrievePendingReservation("/v1/crs/reservation/retrieve?confirmationNumber={confirmationNumber}/last");
		urlProperties.setAcrsSearchReservations("search");
		urlProperties.setAcrsCancelPendingReservation("pending");
		urlProperties.setAcrsReservationsConfPending("pending");
		CommonUtil commonUtil = Mockito.spy(CommonUtil.class);
		when(commonUtil.getRetryableRestTemplate(restTemplateBuilder, applicationProperties.isSslInsecure(),
				acrsProperties.isLiveCRS(), applicationProperties.getAcrsConnectionPerRouteDaoImpl(),
				applicationProperties.getAcrsMaxConnectionPerDaoImpl(), applicationProperties.getConnectionTimeout(),
				applicationProperties.getReadTimeOut(), applicationProperties.getSocketTimeOut(),1,applicationProperties.getCrsRestTTL())).thenReturn(client);

		cancelReservationDAOStrategyACRSImpl = new CancelReservationDAOStrategyACRSImpl(urlProperties, domainProperties,
				applicationProperties, acrsProperties, restTemplateBuilder, referenceDataDAOHelper,
				acrsOAuthTokenDAOImpl, roomPriceDAOStrategyACRSImpl, propertyConfig);

		cancelReservationDAOStrategyACRSImpl.setIdmsTokenDAO(idmsTokenDAO);
		cancelReservationDAOStrategyACRSImpl.setPaymentDao(paymentDao);
		cancelReservationDAOStrategyACRSImpl.client = client;

		cancelReservationDAOStrategyACRSImpl.setPendingResMapper(pendingResMapper);

		MockHttpServletRequest request = new MockHttpServletRequest();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
	}

	private void setMockReferenceDataDAOHelper() {
		setMockForRoomPropertyCode();
	}

	/**
	 * Return retrieve reservation from JSON mock file.
	 */
	private HttpEntity<?> getCrsRetrieveReservation() {
		File file = new File(getClass().getResource("/retrieve_reservation_shared.json").getPath());
		ResponseEntity<?> response = new ResponseEntity<>(convertCrs(file, ReservationRetrieveResReservation.class),
				HttpStatus.OK);
		return response;

	}

	/**
	 * Return retrieve reservation from JSON mock file.
	 */
	private HttpEntity<?> getCrsRetrieveReservation_refund() {
		File file = new File(getClass().getResource("/retrieve_reservation_shared_refund.json").getPath());
		ResponseEntity<?> response = new ResponseEntity<>(convertCrs(file, ReservationRetrieveResReservation.class),
				HttpStatus.OK);
		return response;

	}

	private void setMockForRetrieveReservation_refund() {
		when(client.exchange(ArgumentMatchers.contains("reservation/retrieve"), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationRetrieveResReservation>>any(),
				Mockito.anyMap()))
						.thenReturn((ResponseEntity<ReservationRetrieveResReservation>) getCrsRetrieveReservation_refund());
	}

	private void setMockForRetrieveReservation() {
		when(client.exchange(ArgumentMatchers.contains("reservation/retrieve"), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationRetrieveResReservation>>any(),
				Mockito.anyMap()))
						.thenReturn((ResponseEntity<ReservationRetrieveResReservation>) getCrsRetrieveReservation());
	}
	private void setMockForRetrievePendingReservation() {
		when(client.exchange(ArgumentMatchers.contains("/last"), ArgumentMatchers.any(HttpMethod.class),
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

	private void setMockForSearchReservation() {
		when(client.exchange(ArgumentMatchers.contains("search"), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationSearchResPostSearchReservations>>any(),
				Mockito.anyMap())).thenReturn(
						(ResponseEntity<ReservationSearchResPostSearchReservations>) getCrsSearchReservation());
	}

	/**
	 * Return cancel pending from JSON mock file.
	 */
	private HttpEntity<?> getCrsCancelPendingReservation() {
		File file = new File(getClass().getResource("/cancel_pending_reservation.json").getPath());
		ResponseEntity<?> response = new ResponseEntity<ReservationRes>(convertCrs(file, ReservationRes.class),
				HttpStatus.OK);
		return response;

	}

	private void setMockForCancelPendingReservation() {
		when(client.exchange(ArgumentMatchers.contains("pending"), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationRes>>any(), Mockito.anyMap()))
						.thenReturn((ResponseEntity<ReservationRes>) getCrsCancelPendingReservation());
	}

	/**
	 * Return commit pending from JSON mock file.
	 */
	private HttpEntity<?> getCrsCommitPendingReservation() {
		File file = new File(getClass().getResource("/commit_pending_reservation.json").getPath());
		ResponseEntity<?> response = new ResponseEntity<ReservationRes>(convertCrs(file, ReservationRes.class),
				HttpStatus.OK);
		return response;

	}

	private void setMockForCommitPendingReservation() {
		when(client.exchange(ArgumentMatchers.contains("commit"), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationRes>>any(), Mockito.anyMap()))
						.thenReturn((ResponseEntity<ReservationRes>) getCrsCommitPendingReservation());
	}

	private CancelV2Request createRequest() throws ParseException {
		CancelV2Request request = new CancelV2Request();
		request.setSource("mgmresorts");
		request.setCustomerId(23458789);
		request.setItineraryId("623458789");
		request.setPropertyId("ARIA1");
		request.setConfirmationNumber("1234");
		return request;
	}

	@Test
	public void testCancelReservation_shared() {
		try {
			setMockAuthToken();
			setMockReferenceDataDAOHelper();
			// retrieve res
			setMockForRetrieveReservation();
			setMockForRetrievePendingReservation();
			setReservationModifyPendingRes();
			setMockForPendingResvToHotelReservationRes();
			// search res
			setMockForSearchReservation();
			// pending success
			setMockForCancelPendingReservation();
			// commit pending success
			setMockForCommitPendingReservation();
			// payment success
			mockPaymentSuccess();
			// mock Token
			TokenResponse tknRes = new TokenResponse();
			tknRes.setAccessToken("1234");
			Mockito.doReturn(tknRes).when(idmsTokenDAO).generateToken();

			// mock findReservation
			ReservationRetrieveResReservation reservationPendingRes = (ReservationRetrieveResReservation) getCrsRetrieveReservation()
					.getBody();
			Mockito.doReturn(reservationPendingRes).when(paymentDao).sendRetrieveRequestToPaymentExchangeToken(
					ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());

			CancelV2Request request = createRequest();
			ReservationRetrieveResReservation reservationRetrieveRes = (ReservationRetrieveResReservation) getCrsRetrieveReservation()
					.getBody();
			Assert.assertNotNull(reservationRetrieveRes);
			request.setExistingReservation(
					RoomReservationTransformer.transform(reservationRetrieveRes, acrsProperties));
			RoomReservation reservation = cancelReservationDAOStrategyACRSImpl.cancelReservation(request);
			Assert.assertNotNull(reservation);
			Assert.assertNotNull(reservation.getConfirmationNumber());
			Assert.assertEquals(ReservationState.Cancelled, reservation.getState());
			Assert.assertEquals("6535292112", reservation.getConfirmationNumber());
			// Assert.assertEquals("40IOQ8FYU3", reservation.getShareId());
			// Assert.assertEquals("5498755564", reservation.getShareWiths()[0]);

		} catch (Exception e) {
			e.printStackTrace();
			// System.out.println(e.getMessage());
			logger.error(e.getMessage());
			Assert.fail("cancel reservation Failed");
		}
	}

	private void mockPaymentSuccess() {
		AuthResponse authRes = new AuthResponse();
		authRes.setStatusMessage(ServiceConstant.APPROVED);
		authRes.setAuthRequestId("1531653");

		CaptureResponse capRes = new CaptureResponse();
		capRes.setStatusMessage(ServiceConstant.APPROVED);

		RefundResponse refundRes = new RefundResponse();
		refundRes.setStatusMessage(ServiceConstant.APPROVED);
		refundRes.setAmount("100");

		Mockito.doReturn(authRes).when(paymentDao).authorizePayment(ArgumentMatchers.any());
		Mockito.doReturn(capRes).when(paymentDao).capturePayment(ArgumentMatchers.any());
		Mockito.doReturn(refundRes).when(paymentDao).refundPayment(ArgumentMatchers.any());
	}

	private CancelV2Request createRequest_withProfile() throws ParseException {
		CancelV2Request request = new CancelV2Request();
		request.setSource("mgmresorts");
		request.setCustomerId(23458789);
		request.setItineraryId("623458789");
		request.setPropertyId("ARIA1");
		request.setConfirmationNumber("1234");

		RoomReservation existingReservation = new RoomReservation();
		existingReservation.setPromo("TEST");
		ReservationProfile profile = new ReservationProfile();
		profile.setMlifeNo(123);
		existingReservation.setProfile(profile);
		request.setExistingReservation(existingReservation);
		return request;
	}

	@Test
	public void testCancelReservation_with_existingReservation() {
		try {
			setMockAuthToken();
			setMockReferenceDataDAOHelper();
			// retrieve res
			setMockForRetrieveReservation_refund();
			setMockForRetrievePendingReservation();
			// search res
			setMockForSearchReservation();
			// pending success
			setMockForCancelPendingReservation();
			// commit pending success
			setMockForCommitPendingReservation();

			TokenResponse tknRes = new TokenResponse();
			tknRes.setAccessToken("1234");
			Mockito.doReturn(tknRes).when(idmsTokenDAO).generateToken();

			when(referenceDataDAOHelper.retrieveMerchantID(Mockito.anyString())).thenReturn("MV001");

			// mock payment success
			mockPaymentSuccess();
			// mock find reservation
			ReservationRetrieveResReservation reservationPendingRes = (ReservationRetrieveResReservation) getCrsRetrieveReservation_refund()
					.getBody();
			Mockito.doReturn(reservationPendingRes).when(paymentDao).sendRetrieveRequestToPaymentExchangeToken(
					ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());

			CancelV2Request request = createRequest_withProfile();
			ReservationRetrieveResReservation reservationRetrieveRes = (ReservationRetrieveResReservation) getCrsRetrieveReservation_refund()
					.getBody();
			Assert.assertNotNull(reservationRetrieveRes);

			setReservationModifyPendingRes();
			setMockForPendingResvToHotelReservationRes();

			RoomReservation reservation = cancelReservationDAOStrategyACRSImpl.cancelReservation(request);
			Assert.assertNotNull(reservation);
			Assert.assertNotNull(reservation.getConfirmationNumber());
			Assert.assertEquals(ReservationState.Cancelled, reservation.getState());
			Assert.assertEquals("6535292112", reservation.getConfirmationNumber());
			// Assert.assertEquals("40IOQ8FYU3", reservation.getShareId());
			// Assert.assertEquals("5498755564", reservation.getShareWiths()[0]);

		} catch (Exception e) {
			e.printStackTrace();
			// System.out.println(e.getMessage());
			logger.error(e.getMessage());
			Assert.fail("cancel reservation Failed");
		}
	}

	@Test
	public void testCancelReservation_without_existingReservation() {
		try {
			setMockAuthToken();
			setMockReferenceDataDAOHelper();
			// retrieve res
			setMockForRetrieveReservation_refund();
			setMockForRetrievePendingReservation();
			// search res
			setMockForSearchReservation();
			// pending success
			setMockForCancelPendingReservation();
			// commit pending success
			setMockForCommitPendingReservation();

			TokenResponse tknRes = new TokenResponse();
			tknRes.setAccessToken("1234");
			Mockito.doReturn(tknRes).when(idmsTokenDAO).generateToken();

			when(referenceDataDAOHelper.retrieveMerchantID(Mockito.anyString())).thenReturn("MV001");

			// payment success
			mockPaymentSuccess();
			// mock find reservation
			ReservationRetrieveResReservation reservationPendingRes = (ReservationRetrieveResReservation) getCrsRetrieveReservation_refund()
					.getBody();
			Mockito.doReturn(reservationPendingRes).when(paymentDao).sendRetrieveRequestToPaymentExchangeToken(
					ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());

			CancelV2Request request = createRequest();
			ReservationRetrieveResReservation reservationRetrieveRes = (ReservationRetrieveResReservation) getCrsRetrieveReservation_refund()
					.getBody();
			Assert.assertNotNull(reservationRetrieveRes);

			setReservationModifyPendingRes();
			setMockForPendingResvToHotelReservationRes();

			RoomReservation reservation = cancelReservationDAOStrategyACRSImpl.cancelReservation(request);
			Assert.assertNotNull(reservation);
			Assert.assertNotNull(reservation.getConfirmationNumber());
			Assert.assertEquals(ReservationState.Cancelled, reservation.getState());
			Assert.assertEquals("6535292112", reservation.getConfirmationNumber());
			// Assert.assertEquals("40IOQ8FYU3", reservation.getShareId());
			// Assert.assertEquals("5498755564", reservation.getShareWiths()[0]);

		} catch (Exception e) {
			e.printStackTrace();
			// System.out.println(e.getMessage());
			logger.error(e.getMessage());
			Assert.fail("cancel reservation Failed");
		}
	}

	private void setReservationModifyPendingRes() {
		ReservationModifyPendingRes reservationPendingRes = makePendingRoomReservationResponseFromPayment().getBody();
		Mockito.doReturn(reservationPendingRes).when(paymentDao).sendRequestToPaymentExchangeToken(
				ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
				ArgumentMatchers.anyBoolean());
	}

	private HttpEntity<ReservationModifyPendingRes> makePendingRoomReservationResponseFromPayment() {
		ResponseEntity<ReservationModifyPendingRes> response = new ResponseEntity<ReservationModifyPendingRes>(
				getReservationModifyPendingRes(), HttpStatus.OK);
		return response;
	}

	private ReservationModifyPendingRes getReservationModifyPendingRes() {
		HotelReservationPendingRes pendingRes = getHotelReservationPendingRes();

		ReservationModifyPendingRes reservationModifyPendingRes = new ReservationModifyPendingRes();
		ReservationModifyPendingResData data = new ReservationModifyPendingResData();
		PointOfSale creator = new PointOfSale();
		creator.setOrigin("US");
		creator.setVendorCode("VendorCode");
		creator.setVendorName("VendorName");
		data.setCreator(creator);

		PointOfSale requestor = new PointOfSale();
		requestor.setOrigin("US");
		requestor.setVendorCode("VendorCode");
		requestor.setVendorName("VendorName");
		data.setRequestor(requestor);

		data.setHotelReservation(pendingRes);
		reservationModifyPendingRes.setData(data);
		return reservationModifyPendingRes;
	}

	private void setMockForPendingResvToHotelReservationRes() {

		when(pendingResMapper.pendingResvToHotelReservationRes(Mockito.any()))
				.thenReturn(hotelReservationResResponse());
	}

	private HotelReservationRes getHotelReservationResResponse() {
		File file = new File(getClass().getResource("/cancel_pending_reservation.json").getPath());
		return convertCrs(file, HotelReservationRes.class);
	}

	/**
	 * Return commit pending from JSON mock file.
	 */
	private HotelReservationPendingRes getHotelReservationPendingRes() {
		ReservationModifyPendingRes response = (ReservationModifyPendingRes) makePendingRoomReservationResponse()
				.getBody();
		return response.getData().getHotelReservation();

	}

	private HttpEntity<?> makePendingRoomReservationResponse() {
		ResponseEntity<?> response = new ResponseEntity<ReservationModifyPendingRes>(
				convertCrs("/acrs/modifyreservation/crs-modify-pending.json", ReservationModifyPendingRes.class),
				HttpStatus.OK);
		return response;
	}

	private HotelReservationRes hotelReservationResResponse() {
		RoomReservationPendingResMapperImpl impl = new RoomReservationPendingResMapperImpl();
		ReservationModifyPendingRes response = (ReservationModifyPendingRes) makePendingRoomReservationResponse()
				.getBody();
		HotelReservationPendingRes pendingRes = response.getData().getHotelReservation();
		return impl.pendingResvToHotelReservationRes(pendingRes);
	}

	@Test
	public void ignoreReservationTest() {
		try {
			ReleaseV2Request cancelRequest = new ReleaseV2Request();
			cancelRequest.setConfirmationNumber("1234567");
			cancelRequest.setSource("ICECC");
			setMockReferenceDataDAOHelper();
			setMockAuthToken();

			HttpHeaders headers = new HttpHeaders();
			headers.add(ServiceConstant.HEADER_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_APPLICATION_JSON);
			headers.add(ServiceConstant.HEADER_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER + "23412345");
			headers.add(ServiceConstant.X_MGM_CORRELATION_ID, UUID.randomUUID().toString());
			headers.add(ServiceConstant.X_MGM_TRANSACTION_ID, UUID.randomUUID().toString());
			headers.add(ServiceConstant.HEADER_USER_AGENT, UUID.randomUUID().toString());
			headers.add(ServiceConstant.HEADER_FRAUD_AGENT_TOKEN, UUID.randomUUID().toString());


			when(client.exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class), ArgumentMatchers.any(),
					ArgumentMatchers.<Class<ReservationRes>>any(), Mockito.anyMap()))
					.thenReturn((ResponseEntity<ReservationRes>) getCrsCommitPendingReservation());

			boolean response = cancelReservationDAOStrategyACRSImpl.ignoreReservation(cancelRequest);
			assertEquals(true, response);
		} catch (Exception e) {
			e.printStackTrace();
			// System.out.println(e.getMessage());
			logger.error(e.getMessage());
			Assert.fail("cancel reservation Failed");
		}
	}

	@Test
	public void ignoreReservationTest_f1() {
		try {
			ReleaseV2Request cancelRequest = new ReleaseV2Request();
			cancelRequest.setF1Package(true);

			boolean response = cancelReservationDAOStrategyACRSImpl.ignoreReservation(cancelRequest);
			assertEquals(true, response);
		} catch (Exception e) {
			e.printStackTrace();
			// System.out.println(e.getMessage());
			logger.error(e.getMessage());
			Assert.fail("cancel reservation Failed");
		}
	}

	@Test
	public void testHasError() throws IOException {
		CancelReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = new CancelReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
		boolean result = errorHandler.hasError(httpResponse);
		assertTrue(result);
	}

	@Test
	public void testHandleErrorSYSTEM_ERROR() throws IOException {
		CancelReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = new CancelReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
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
	public void testHandleErrorBusinessExceptionRESERVATION_ALREADY_CANCELLED() throws IOException {
		CancelReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = new CancelReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSErrorDetails acrsError = new ACRSErrorDetails();
		acrsError.setTitle("There is no pending image to be committed.");
		String acrsSearchOffersErrorResJson = CommonUtil.convertObjectToJsonString(acrsError);
		InputStream is = new ByteArrayInputStream(acrsSearchOffersErrorResJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);

		// Assertions
		BusinessException ex = assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.RESERVATION_ALREADY_CANCELLED, ex.getErrorCode());	
	}
	
	@Test
	public void testHandleErrorBusinessExceptionRESERVATION_NOT_FOUND() throws IOException {
		CancelReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = new CancelReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSErrorDetails acrsError = new ACRSErrorDetails();
		acrsError.setTitle("No reservation available for the input confirmation number.");
		String acrsSearchOffersErrorResJson = CommonUtil.convertObjectToJsonString(acrsError);
		InputStream is = new ByteArrayInputStream(acrsSearchOffersErrorResJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);

		// Assertions
		BusinessException ex = assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.RESERVATION_NOT_FOUND, ex.getErrorCode());	
	}
	
	@Test
	public void testHandleErrorElse() throws IOException {
		CancelReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = new CancelReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSErrorDetails acrsError = new ACRSErrorDetails();
		String acrsSearchOffersErrorResJson = CommonUtil.convertObjectToJsonString(acrsError);
		InputStream is = new ByteArrayInputStream(acrsSearchOffersErrorResJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
		
		// Assertions
		BusinessException ex = assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.AURORA_FUNCTIONAL_EXCEPTION, ex.getErrorCode());
	}

	@Test
	public void testCreateModificationChangesForExpiredRefund() {
		// Arrange ACRS properties used by test
		acrsProperties.setModifyForcedSellPath("ModifyForcedSellPath");
		acrsProperties.setModifyDepositPaymentsPath("ModifyDepositPaymentsPath");
		// Arrange creditCardCharges with expired card
		List<CreditCardCharge> creditCardCharges = createMockRefundCreditCardChargesWithExpiredCard();
		// Arrange refundPayments as empty list as we are just testing expired card in this test
		List<Payment> refundPayments = new ArrayList<>();

		try {
			//Act
			List<ModificationChangesItem> modificationChangesItems =
					cancelReservationDAOStrategyACRSImpl.createModificationChangesForSettledRefunds(
							creditCardCharges, refundPayments);

			//Assert response is correct
			Assertions.assertTrue(CollectionUtils.isNotEmpty(modificationChangesItems));

			Optional<ModificationChangesItem> paymentModificationChangeOptional = modificationChangesItems.stream()
					.filter(item -> "ModifyDepositPaymentsPath".equalsIgnoreCase(item.getPath()))
					.findFirst();
			Assertions.assertTrue(paymentModificationChangeOptional.isPresent());
			Assertions.assertTrue(paymentModificationChangeOptional.get().getValue() instanceof PaymentTransactionReq);

			PaymentTransactionReq paymentTransactionReq = (PaymentTransactionReq) paymentModificationChangeOptional.get()
					.getValue();
			Assertions.assertEquals(PaymentStatus.PAYMENT_RECEIVED, paymentTransactionReq.getPaymentStatus());
			Assertions.assertEquals(PaymentIntent.REFUND, paymentTransactionReq.getPaymentIntent());
			Assertions.assertEquals("100.0", paymentTransactionReq.getAmount());
			Assertions.assertEquals("1223", paymentTransactionReq.getPaymentCard().getExpireDate());
		} catch (Exception e) {
			Assertions.fail("testCreateModificationChangesForExpiredRefund failed due to unexpected exception: ", e);
		}
	}
}
