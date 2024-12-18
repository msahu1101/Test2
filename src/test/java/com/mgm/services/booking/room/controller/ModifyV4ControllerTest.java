package com.mgm.services.booking.room.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DirectFieldBindingResult;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.model.request.ModifyRoomReservationRequest;
import com.mgm.services.booking.room.model.request.PaymentRoomReservationRequest;
import com.mgm.services.booking.room.model.request.PreviewCommitRequest;
import com.mgm.services.booking.room.model.response.ModifyRoomReservationResponse;
import com.mgm.services.booking.room.model.response.RoomReservationV2Response;
import com.mgm.services.booking.room.service.ModifyReservationService;
import com.mgm.services.booking.room.validator.ModifyRoomReservationRequestValidator;
import com.mgm.services.booking.room.validator.PreviewCommitRequestValidator;
import com.mgm.services.booking.room.validator.RefundRequestValidator;
import com.mgm.services.booking.room.validator.TokenValidator;
import com.mgm.services.booking.room.validator.helper.ValidationHelper;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.ValidationException;

@RunWith(MockitoJUnitRunner.class)
public class ModifyV4ControllerTest extends BaseRoomBookingTest {
	@InjectMocks
	private ModifyV4Controller modifyV4Controller;

	@Mock
	private TokenValidator tokenValidator;

	@Mock
	private RefundRequestValidator refundValidator;


	@Mock
	private ValidationHelper helper;
	@Mock
	ModifyReservationService modifyReservationService;

	private LocalValidatorFactoryBean localValidatorFactory;
	@InjectMocks
	private ModifyRoomReservationRequestValidator modifyRoomReservationRequestValidator;

	@InjectMocks
	private PreviewCommitRequestValidator previewCommitRequestValidator;


	@Before
	public void setUp() {
		localValidatorFactory = new LocalValidatorFactoryBean();
		localValidatorFactory.setProviderClass(HibernateValidator.class);
		localValidatorFactory.afterPropertiesSet();

		MockHttpServletRequest request = new MockHttpServletRequest();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		ReflectionTestUtils.setField(modifyV4Controller, "modifyRoomValidator", modifyRoomReservationRequestValidator);
		ReflectionTestUtils.setField(modifyV4Controller, "commitValidator", previewCommitRequestValidator);

	}
	private <T> T getObject(String fileName, Class<T> target) {
		File file = new File(getClass().getResource(fileName).getPath());
		return convert(file, target);
	}
	private PaymentRoomReservationRequest getCommitRequest_ForRefundDeposit() {
		return getObject("/paymentwidgetv4/commit/refund_deposit/commit-v4-refunddeposit-request.json", PaymentRoomReservationRequest.class);
	}
	private PreviewCommitRequest loadPreviewCommitRequest() {
		return getObject("/previewCommitRequest.json", PreviewCommitRequest.class);
	}
	private ModifyRoomReservationRequest getModifyPendingRequest() {
		return getObject("/modifyPendingV4-request.json",ModifyRoomReservationRequest.class);
	}
	private ModifyRoomReservationResponse getModifyPendingResponse() {
		return getObject("/modifyPendingV4-response.json",ModifyRoomReservationResponse.class);
	}

	@After
	public void tearDown() {
		localValidatorFactory = null;
	}
	private ModifyRoomReservationResponse getModifyRoomReservationResponse() {
		ModifyRoomReservationResponse modifyRoomReservationResponse=new ModifyRoomReservationResponse();
		RoomReservationV2Response roomReservation=new RoomReservationV2Response();
		roomReservation.setAmountDue(10.10);
		modifyRoomReservationResponse.setRoomReservation(roomReservation);
		return modifyRoomReservationResponse;
	}

	@Test
	public void CommitRefundTest() {

		PaymentRoomReservationRequest request = getCommitRequest_ForRefundDeposit();
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		HttpServletResponse mockResponse = mock(HttpServletResponse.class);
		BindingResult result = new DirectFieldBindingResult(request, "PaymentRoomReservationRequest");
		localValidatorFactory.validate(request, result);
		Mockito.when(modifyReservationService.commitPaymentReservation(Mockito.any())).thenReturn(getModifyRoomReservationResponse());
		ModifyRoomReservationResponse response= modifyV4Controller.commitRefund(TestConstant.ICE, request, mockRequest, mockResponse);
		assertEquals(getModifyRoomReservationResponse(),response);
		assertNotNull(response);
	}
	@Test
    public void reservationModifyPreviewPending_WithNoConfirmationNumber_validateErrorMessage() {
        when(helper.hasServiceRoleAccess()).thenReturn(false);
        PreviewCommitRequest request = loadPreviewCommitRequest();
        request.setConfirmationNumber(null);
        runTest(request, ErrorCode.NO_CONFIRMATION_NUMBER);
    }

    @Test
    public void reservationModifyPreviewPending_WithNoPreviewReservationTotal_validateErrorMessage() {
        when(helper.hasServiceRoleAccess()).thenReturn(false);
        PreviewCommitRequest request = loadPreviewCommitRequest();
        request.setPreviewReservationTotal(null);
        runTest(request, ErrorCode.MODIFY_VIOLATION_NO_TOTALS);
    }

    @Test
    public void reservationModifyPreviewPending_WithNoPreviewReservationDeposit_validateErrorMessage() {
        when(helper.hasServiceRoleAccess()).thenReturn(false);
        PreviewCommitRequest request = loadPreviewCommitRequest();
        request.setPreviewReservationDeposit(null);
        runTest(request, ErrorCode.MODIFY_VIOLATION_NO_TOTALS);
    }

    @Test
    public void reservationModifyPreviewPending_WithNoFirstName_validateErrorMessage() {
        when(helper.hasServiceRoleAccess()).thenReturn(false);
        PreviewCommitRequest request = loadPreviewCommitRequest();
        request.setFirstName(null);
        runTest(request, ErrorCode.INVALID_NAME);
    }

    @Test
    public void reservationModifyPreviewPending_WithNoLastName_validateErrorMessage() {
        when(helper.hasServiceRoleAccess()).thenReturn(false);
        PreviewCommitRequest request = loadPreviewCommitRequest();
        request.setLastName(null);
        runTest(request, ErrorCode.INVALID_NAME);
    }

    @Test
    public void reservationModifyPreviewPending_WithInvalidCvv_validateErrorMessage() {
        when(helper.hasServiceRoleAccess()).thenReturn(false);
        PreviewCommitRequest request = loadPreviewCommitRequest();
        request.setCvv("abc");
        runTest(request, ErrorCode.INVALID_CVV);
    }

    private void runTest(PreviewCommitRequest request, ErrorCode errorCode) {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        BindingResult result = new DirectFieldBindingResult(request, "PreviewCommitRequest");
        boolean validationExceptionOccured = false;

        try {
            localValidatorFactory.validate(request, result);
            modifyV4Controller.reservationModifyPreviewPending(TestConstant.MGM_RESORTS, request, mockRequest, mockResponse);
        } catch (ValidationException e) {
            validationExceptionOccured = true;
            assertEquals("Error Code should match: ", errorCode.getErrorCode(), e.getErrorCodes().get(0));
        } finally {
            assertTrue("Controller should throw ValidationException", validationExceptionOccured);
        }
    }
  @Test 
  public void reservationModifyPreviewPendingTest() {
	  PreviewCommitRequest request = loadPreviewCommitRequest();
      HttpServletRequest mockRequest = mock(HttpServletRequest.class);
      HttpServletResponse mockResponse = mock(HttpServletResponse.class);
      BindingResult result = new DirectFieldBindingResult(request, "PreviewCommitRequest");
      localValidatorFactory.validate(request, result);
      Mockito.when(modifyReservationService.reservationModifyPendingV5(request, null)).thenReturn(getModifyRoomReservationResponse());
      ModifyRoomReservationResponse response=modifyV4Controller.reservationModifyPreviewPending(TestConstant.MGM_RESORTS, request, mockRequest, mockResponse);
      assertEquals(getModifyRoomReservationResponse(),response);
  }
  
  @Test 
  public void reservationModifyPendingTest() {
	  ModifyRoomReservationRequest request = getModifyPendingRequest();
      HttpServletRequest mockRequest = mock(HttpServletRequest.class);
      BindingResult result = new DirectFieldBindingResult(request, "ModifyRoomReservationRequest");
      localValidatorFactory.validate(request, result);
      Mockito.when(modifyReservationService.reservationModifyPendingV4(Mockito.any())).thenReturn(getModifyPendingResponse());
      ModifyRoomReservationResponse response=modifyV4Controller.reservationModifyPending(TestConstant.MGM_RESORTS, request, mockRequest);
      assertNotNull(response);
      assertNotNull(response.getRoomReservation().getBookings());
      assertNotNull(response.getRoomReservation().getPurchasedComponents());
      assertEquals(getModifyPendingResponse().getRoomReservation().getConfirmationNumber(),response.getRoomReservation().getConfirmationNumber());
  }
}
