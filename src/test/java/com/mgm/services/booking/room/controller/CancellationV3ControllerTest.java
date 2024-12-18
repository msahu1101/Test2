package com.mgm.services.booking.room.controller;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DirectFieldBindingResult;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.model.request.CancelV3Request;
import com.mgm.services.booking.room.model.request.ReleaseV2Request;
import com.mgm.services.booking.room.model.request.ReleaseV3Request;
import com.mgm.services.booking.room.model.response.CancelRoomReservationV2Response;
import com.mgm.services.booking.room.model.response.RoomReservationV2Response;
import com.mgm.services.booking.room.service.CancelService;
import com.mgm.services.booking.room.validator.CancelV3RequestValidator;
import com.mgm.services.booking.room.validator.TokenValidator;
import com.mgm.services.booking.room.validator.helper.ValidationHelper;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.ValidationException;

/**
 * Unit test class to validate the request object with various param.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class CancellationV3ControllerTest extends BaseRoomBookingTest {

	@InjectMocks
	private CancellationV3Controller cancellationV3Controller;

	@InjectMocks
	private CancelV3RequestValidator validator;

	@Mock
    private ValidationHelper helper;
	
	@Mock
    private TokenValidator tokenValidator;
	
	@Mock
	CancelService cancelService;

    private LocalValidatorFactoryBean localValidatorFactory;

    @Before
    public void setUp() {
        localValidatorFactory = new LocalValidatorFactoryBean();
        localValidatorFactory.setProviderClass(HibernateValidator.class);
        localValidatorFactory.afterPropertiesSet();

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        ReflectionTestUtils.setField(cancellationV3Controller, "validator", validator);
    }

    @After
    public void tearDown() {
        localValidatorFactory = null;
    }

    @Test
    public void cancel_MissingConfirmationNumber_validateErrorMessage() {
        when(helper.isTokenAGuestToken()).thenReturn(true);

        CancelV3Request cancelRequest = new CancelV3Request();
        runTest(ErrorCode.NO_CONFIRMATION_NUMBER, cancelRequest, TestConstant.ICE);
    }

    @Test
    public void cancel_webClientWithServiceTokenNoFirstName_validateErrorMessage() {

        CancelV3Request cancelRequest = new CancelV3Request();
        cancelRequest.setConfirmationNumber("12345");
        runTest(ErrorCode.NO_FIRST_NAME, cancelRequest, TestConstant.MGM_RESORTS);
    }
    
    @Test
    public void cancel_webClientWithServiceTokenNoLastName_validateErrorMessage() {

        CancelV3Request cancelRequest = new CancelV3Request();
        cancelRequest.setConfirmationNumber("12345");
        cancelRequest.setFirstName("Test");
        runTest(ErrorCode.NO_LAST_NAME, cancelRequest, TestConstant.MGM_RESORTS);
    }

    private void runTest(ErrorCode errorCode, CancelV3Request request, String source) {
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		BindingResult result = new DirectFieldBindingResult(request, "CancelV3Request");
		boolean validationExceptionOccured = false;
		try {
		    localValidatorFactory.validate(request, result);
			cancellationV3Controller.cancel(source, request, result, mockRequest);
		} catch (ValidationException e) {
		    validationExceptionOccured = true;
			assertTrue(e.getErrorCodes().contains(errorCode.getErrorCode()));
		} finally {
            assertTrue("Controller should throw ValidationException", validationExceptionOccured);
        }
	}
    
    @Test
    public void cancelTestPositive() {
    	CancelV3Request request=new CancelV3Request();
		request.setCustomerId(123456);
		request.setCancellationReason("Program canceled");
		request.setConfirmationNumber("123");
		request.setFirstName("Test");
		request.setLastName("case");
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		BindingResult result = new DirectFieldBindingResult(request, "CancelV3Request");
		localValidatorFactory.validate(request, result);
		Mockito.when(cancelService.cancelReservation(request, null)).thenReturn(getCancelRoomReservationV2Response());
		CancelRoomReservationV2Response response=cancellationV3Controller.cancel(TestConstant.ICE, request, result, mockRequest);
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
		ReleaseV3Request request = new ReleaseV3Request();
		request.setCustomerId(123456);
		request.setPropertyId("123");
		request.setConfirmationNumber("123");
		BindingResult result = new DirectFieldBindingResult(request, "CancelV2Request");
		localValidatorFactory.validate(request, result);
		Mockito.when(cancelService.ignoreReservationV3(request)).thenReturn(true);
		cancellationV3Controller
				.ignore(TestConstant.ICE, request, result, mockRequest,mockResponse, null);
    }
	
	@Test
    public void ignoreTest_ignoreReservationFalse() {
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		HttpServletResponse mockResponse= mock(HttpServletResponse.class);
		ReleaseV3Request request = new ReleaseV3Request();
		request.setCustomerId(123456);
		request.setPropertyId("123");
		request.setConfirmationNumber("123");
		BindingResult result = new DirectFieldBindingResult(request, "CancelV2Request");
		localValidatorFactory.validate(request, result);
		Mockito.when(cancelService.ignoreReservationV3(request)).thenReturn(false);
		cancellationV3Controller
				.ignore(TestConstant.ICE, request, result, mockRequest,mockResponse, null);
    }
	
	@Test
    public void cancelF1Test() {
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		CancelV3Request request=new CancelV3Request();
		request.setConfirmationNumber("123456");
		request.setFirstName("test");
		request.setLastName("case");
		BindingResult result = new DirectFieldBindingResult(request, "CancelV2Request");
		localValidatorFactory.validate(request, result);
		Mockito.when(cancelService.cancelReservationF1(request,null)).thenReturn(getCancelRoomReservationV2Response());
		CancelRoomReservationV2Response response=cancellationV3Controller
				.cancelF1(TestConstant.ICE, request, result, mockRequest);
		assertEquals(getCancelRoomReservationV2Response(),response);
    }
}
