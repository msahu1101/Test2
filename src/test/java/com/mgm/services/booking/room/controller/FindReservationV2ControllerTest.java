package com.mgm.services.booking.room.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.validator.HibernateValidator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DirectFieldBindingResult;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.model.request.FindReservationV2Request;
import com.mgm.services.booking.room.model.request.RoomReservationBasicInfoRequest;
import com.mgm.services.booking.room.model.response.GetRoomReservationResponse;
import com.mgm.services.booking.room.model.response.ReservationBasicInfo;
import com.mgm.services.booking.room.model.response.ReservationsBasicInfoResponse;
import com.mgm.services.booking.room.model.response.RoomReservationV2Response;
import com.mgm.services.booking.room.service.FindReservationService;
import com.mgm.services.booking.room.validator.FindReservationV2RequestValidator;
import com.mgm.services.booking.room.validator.TokenValidator;
import com.mgm.services.booking.room.validator.helper.ValidationHelper;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.ValidationException;

/**
 * This class contains the unit tests of FindReservationV2Controller.
 * 
 * @author laknaray
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class FindReservationV2ControllerTest extends BaseRoomBookingTest {

	@InjectMocks
	private FindReservationV2Controller findReservationV2Controller;

	@Mock
	private TokenValidator tokenValidator;

	@InjectMocks
	private FindReservationV2RequestValidator findReservationV2RequestValidator;

	@Mock
	private ValidationHelper helper;

	@Mock
	FindReservationService findReservationService;

	private LocalValidatorFactoryBean localValidatorFactory;

	@Before
	public void setUp() {
		localValidatorFactory = new LocalValidatorFactoryBean();
		localValidatorFactory.setProviderClass(HibernateValidator.class);
		localValidatorFactory.afterPropertiesSet();

		ReflectionTestUtils.setField(findReservationV2Controller, "validator", findReservationV2RequestValidator);
	}

	@After
	public void tearDown() {
		localValidatorFactory = null;
	}

	@Test
    @Tag("Trips")
    @DisplayName("Test if true holds")
    public void findRoomReservation_withElevatedAccess_withoutConfirmationNumber_validateErrorMessage() {
        //when(helper.hasElevatedAccess()).thenReturn(true);
        when(helper.hasServiceRoleAccess()).thenReturn(true);
        FindReservationV2Request request = buildRequest();
        request.setConfirmationNumber(null);
        runTest(request, ErrorCode.NO_CONFIRMATION_NUMBER);
    }

	@Test
    @Tag("Trips")
    public void findRoomReservation_withoutElevatedAccess_withoutConfirmationNumber_validateErrorMessage() {
        //when(helper.hasElevatedAccess()).thenReturn(false);
        when(helper.hasServiceRoleAccess()).thenReturn(false);
        FindReservationV2Request request = buildRequest();
        request.setConfirmationNumber(null);
        runTest(request, ErrorCode.NO_CONFIRMATION_NUMBER);
    }

	@Test
    @Tag("Trips")
    public void findRoomReservation_withoutElevatedAccess_withoutFirstName_validateErrorMessage() {
        //when(helper.hasElevatedAccess()).thenReturn(false);
        when(helper.hasServiceRoleAccess()).thenReturn(false);
        FindReservationV2Request request = buildRequest();
        request.setFirstName(null);
        runTest(request, ErrorCode.NO_FIRST_NAME);
    }

	@Test
    @Tag("Trips")
    public void findRoomReservation_withoutElevatedAccess_withoutLastName_validateErrorMessage() {
        //when(helper.hasElevatedAccess()).thenReturn(false);
        when(helper.hasServiceRoleAccess()).thenReturn(false);
        FindReservationV2Request request = buildRequest();
        request.setLastName(null);
        runTest(request, ErrorCode.NO_LAST_NAME);
    }

	private FindReservationV2Request buildRequest() {
		FindReservationV2Request request = new FindReservationV2Request();
		request.setConfirmationNumber("1234");
		request.setFirstName("first");
		request.setLastName("last");
		return request;
	}

	private void runTest(FindReservationV2Request findReservationV2Request, ErrorCode errorCode) {
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		BindingResult result = new DirectFieldBindingResult(findReservationV2Request, "FindReservationV2Request");
		boolean validationExceptionOccured = false;

		try {
			localValidatorFactory.validate(findReservationV2Request, result);
			findReservationV2Controller.findRoomReservation(TestConstant.ICE, findReservationV2Request, mockRequest,
					result);
		} catch (ValidationException e) {
			validationExceptionOccured = true;
			assertEquals("Error Code should match: ", errorCode.getErrorCode(), e.getErrorCodes().get(0));
		} finally {
			assertTrue("Controller should throw ValidationException", validationExceptionOccured);
		}
	}

	@Test
	public void findRoomReservationTest() {
		FindReservationV2Request findReservationV2Request = new FindReservationV2Request();
		findReservationV2Request.setConfirmationNumber("123456");
		findReservationV2Request.setFirstName("Test");
		findReservationV2Request.setLastName("Case");
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		BindingResult result = new DirectFieldBindingResult(findReservationV2Request, "FindReservationV2Request");
		localValidatorFactory.validate(findReservationV2Request, result);
		GetRoomReservationResponse getRoomReservationResponse = new GetRoomReservationResponse();
		RoomReservationV2Response roomReservationResponse = new RoomReservationV2Response();
		roomReservationResponse.setBookDate(new Date());
		getRoomReservationResponse.setRoomReservation(roomReservationResponse);
		Mockito.when(findReservationService.findRoomReservationResponse(findReservationV2Request))
				.thenReturn(getRoomReservationResponse);
		GetRoomReservationResponse response = findReservationV2Controller.findRoomReservation(TestConstant.ICE,
				findReservationV2Request, mockRequest, result);
		assertEquals(getRoomReservationResponse, response);
	}

	@Test
	public void getPartyOrShareWithReservationsTest_invalid_party_reservation() {
		RoomReservationBasicInfoRequest request = new RoomReservationBasicInfoRequest();
		getPartyOrShareWithReservationsTest(request, ErrorCode.INVALID_PARTY_RESERVATION_REQUEST);
	}

	private void getPartyOrShareWithReservationsTest(RoomReservationBasicInfoRequest roomReservationBasicInfoRequest,
			ErrorCode errorCode) {
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		BindingResult result = new DirectFieldBindingResult(roomReservationBasicInfoRequest,
				"RoomReservationBasicInfoRequest");
		boolean validationExceptionOccured = false;

		try {
			localValidatorFactory.validate(roomReservationBasicInfoRequest, result);
			findReservationV2Controller.getPartyOrShareWithReservations(TestConstant.ICE,
					roomReservationBasicInfoRequest, result, mockRequest, "Token");
		} catch (ValidationException e) {
			validationExceptionOccured = true;
			assertEquals("Error Code should match: ", errorCode.getErrorCode(), e.getErrorCodes().get(0));
		} finally {
			assertTrue("Controller should throw ValidationException", validationExceptionOccured);
		}
	}

	@Test
	public void getPartyOrShareWithReservationsPositiveTest() {
		RoomReservationBasicInfoRequest roomReservationBasicInfoRequest = new RoomReservationBasicInfoRequest();
		roomReservationBasicInfoRequest.setConfirmationNumber("123456");
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		BindingResult result = new DirectFieldBindingResult(roomReservationBasicInfoRequest,
				"RoomReservationBasicInfoRequest");
		localValidatorFactory.validate(roomReservationBasicInfoRequest, result);
		ReservationsBasicInfoResponse reservationsBasicInfoResponse=new ReservationsBasicInfoResponse();
		List<ReservationBasicInfo> reservationAdditionalInfo=new ArrayList<>();
		ReservationBasicInfo reservationBasicInfo=new ReservationBasicInfo();
		reservationBasicInfo.setCheckInDate(new Date().toString());
		reservationAdditionalInfo.add(reservationBasicInfo);
		reservationsBasicInfoResponse.setReservationAdditionalInfo(null);
		Mockito.when(findReservationService.getReservationBasicInfoList(roomReservationBasicInfoRequest)).thenReturn(reservationsBasicInfoResponse);
		ReservationsBasicInfoResponse response = findReservationV2Controller.getPartyOrShareWithReservations(
				TestConstant.ICE, roomReservationBasicInfoRequest, result, mockRequest, "Token");
		assertEquals(reservationsBasicInfoResponse,response);

	}

}
