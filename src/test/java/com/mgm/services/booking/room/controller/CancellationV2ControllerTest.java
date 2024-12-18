package com.mgm.services.booking.room.controller;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.File;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.validator.HibernateValidator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DirectFieldBindingResult;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.model.request.CancelV2Request;
import com.mgm.services.booking.room.model.request.ReleaseV2Request;
import com.mgm.services.booking.room.model.response.CancelRoomReservationV2Response;
import com.mgm.services.booking.room.model.response.RoomReservationV2Response;
import com.mgm.services.booking.room.service.CancelService;
import com.mgm.services.booking.room.validator.TokenValidator;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.ValidationException;

/**
 * Unit test class to validate the request object with various param.
 * 
 * @author jayveera
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class CancellationV2ControllerTest extends BaseRoomBookingTest {

	@InjectMocks
	private CancellationV2Controller cancellationV2Controller;
	
	@Mock
    private TokenValidator tokenValidator;
	
	@Mock
	Validator validator;
	
	@Mock
	CancelService cancelService;

    private LocalValidatorFactoryBean localValidatorFactory;

    @Before
    public void setUp() {
        localValidatorFactory = new LocalValidatorFactoryBean();
        localValidatorFactory.setProviderClass(HibernateValidator.class);
        localValidatorFactory.afterPropertiesSet();
    }

    @After
    public void tearDown() {
        localValidatorFactory = null;
    }
    
    @Test
    public void cancelReservation_WithInvalidItineraryId_validateErrorMessage() {
        runTest("missingitineraryid", ErrorCode.INVALID_ITINERARY_ID);
    }

    @Test
    public void cancelReservation_MissingReservationIdAndConfirmationNumber_validateErrorMessage() {
        runTest("missingreservationconfirmnumber", ErrorCode.INVALID_RESERVATION_ID_CONFIRMATION_NUMBER);
    }

    @Test
    public void cancelReservation_MissingCustomerId_validateErrorMessage() {
        runTest("missingcustomerid", ErrorCode.INVALID_CUSTOMER_ID);
    }

    private void runTest(String scenario, ErrorCode errorCode) {
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		CancelV2Request request = convert(
				new File(
						getClass().getResource(String.format("/cancelreservationrequest-%s.json", scenario)).getPath()),
				CancelV2Request.class);
		BindingResult result = new DirectFieldBindingResult(request, "CancelV2Request");
		boolean validationExceptionOccured = false;
		try {
		    localValidatorFactory.validate(request, result);
			cancellationV2Controller.cancel(TestConstant.ICE, request, result, mockRequest, null);
		} catch (ValidationException e) {
		    validationExceptionOccured = true;
			assertTrue(e.getErrorCodes().contains(errorCode.getErrorCode()));
		} finally {
            assertTrue("Controller should throw ValidationException", validationExceptionOccured);
        }
	}
    
    @Test
    public void cancelTest() {
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		CancelV2Request request = new CancelV2Request();
		request.setItineraryId("123");
		request.setCustomerId(123456);
		request.setCancellationReason("Program canceled");
		request.setConfirmationNumber("123");
		BindingResult result = new DirectFieldBindingResult(request, "CancelV2Request");
		localValidatorFactory.validate(request, result);
		Mockito.when(cancelService.cancelReservation(request, false)).thenReturn(getCancelRoomReservationV2Response());
		CancelRoomReservationV2Response response=cancellationV2Controller
				.cancel(TestConstant.ICE, request, result, mockRequest, null);
		assertEquals(getCancelRoomReservationV2Response(),response);
    }

	private CancelRoomReservationV2Response getCancelRoomReservationV2Response() {
		CancelRoomReservationV2Response cancelRoomReservationV2Response=new CancelRoomReservationV2Response();
		RoomReservationV2Response roomReservationV2Response=new RoomReservationV2Response();
		roomReservationV2Response.setAmountDue(1.23);
		cancelRoomReservationV2Response.setRoomReservation(roomReservationV2Response);
		return cancelRoomReservationV2Response;
	}
	
	@Test
    public void ignoreTest() {
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		HttpServletResponse mockResponse= mock(HttpServletResponse.class);
		ReleaseV2Request request = new ReleaseV2Request();
		request.setCustomerId(123456);
		request.setPropertyId("123");
		request.setConfirmationNumber("123");
		BindingResult result = new DirectFieldBindingResult(request, "CancelV2Request");
		localValidatorFactory.validate(request, result);
		Mockito.when(cancelService.ignoreReservation(request)).thenReturn(true);
		cancellationV2Controller
				.ignore(TestConstant.ICE, request, result, mockRequest,mockResponse, null);
    }
	
	@Test
    public void ignoreTest_ignoreReservationFalse() {
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		HttpServletResponse mockResponse= mock(HttpServletResponse.class);
		ReleaseV2Request request = new ReleaseV2Request();
		request.setCustomerId(123456);
		request.setPropertyId("123");
		request.setConfirmationNumber("123");
		BindingResult result = new DirectFieldBindingResult(request, "CancelV2Request");
		localValidatorFactory.validate(request, result);
		Mockito.when(cancelService.ignoreReservation(request)).thenReturn(false);
		cancellationV2Controller
				.ignore(TestConstant.ICE, request, result, mockRequest,mockResponse, null);
    }
	
	@Test
    public void ignoreTestBindingResultNull() {
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		HttpServletResponse mockResponse= mock(HttpServletResponse.class);
		ReleaseV2Request request = new ReleaseV2Request();
		request.setCustomerId(123456);
		request.setPropertyId("123");
		request.setConfirmationNumber("123");
		BindingResult result = new DirectFieldBindingResult(request, "CancelV2Request");
		localValidatorFactory.validate(request, result);
		Mockito.when(cancelService.ignoreReservation(request)).thenReturn(false);
		cancellationV2Controller
				.ignore(TestConstant.ICE, request, null, mockRequest,mockResponse, null);
    }
}
