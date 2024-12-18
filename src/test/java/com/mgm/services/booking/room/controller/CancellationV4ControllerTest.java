package com.mgm.services.booking.room.controller;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import javax.servlet.http.HttpServletRequest;
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
import com.mgm.services.booking.room.model.request.CancelV4Request;
import com.mgm.services.booking.room.model.response.CancelRoomReservationV2Response;
import com.mgm.services.booking.room.service.CancelService;
import com.mgm.services.booking.room.validator.CancelV4RequestValidator;
import com.mgm.services.booking.room.validator.TokenValidator;
import com.mgm.services.booking.room.validator.helper.ValidationHelper;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.ValidationException;

/**
 * Unit test class to validate the request object with various param.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class CancellationV4ControllerTest extends BaseRoomBookingTest {

	@InjectMocks
	private CancellationV4Controller cancellationV4Controller;

	@InjectMocks
	private CancelV4RequestValidator validator;

	@Mock
    private ValidationHelper helper;
	
	@Mock
    private TokenValidator tokenValidator;
	
	@Mock
	CancelService cancelService;

    private LocalValidatorFactoryBean localValidatorFactory;
    
    private CancelV4Request getCancelV4Request() {
        File file = new File(getClass().getResource("/cancelPreview-v4-request.json").getPath());
        return convert(file, CancelV4Request.class);

    }

    private CancelRoomReservationV2Response getCancelPreviewResponse() {
        File file = new File(getClass().getResource("/cancelPreview-v4-response.json").getPath());
        return convert(file, CancelRoomReservationV2Response.class);

    }
    
    private CancelRoomReservationV2Response getCancelCommitResponse() {
        File file = new File(getClass().getResource("/cancelCommit-v4-response.json").getPath());
        return convert(file, CancelRoomReservationV2Response.class);

    }

    @Before
    public void setUp() {
        localValidatorFactory = new LocalValidatorFactoryBean();
        localValidatorFactory.setProviderClass(HibernateValidator.class);
        localValidatorFactory.afterPropertiesSet();

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        ReflectionTestUtils.setField(cancellationV4Controller, "validator", validator);
    }

    @After
    public void tearDown() {
        localValidatorFactory = null;
    }

    @Test
    public void cancel_MissingConfirmationNumber_validateErrorMessage() {
        when(helper.isTokenAGuestToken()).thenReturn(true);

        CancelV4Request cancelRequest = new CancelV4Request();
        runTest(ErrorCode.NO_CONFIRMATION_NUMBER, cancelRequest, TestConstant.ICE);
    }

    @Test
    public void cancel_webClientWithServiceTokenNoFirstName_validateErrorMessage() {

        CancelV4Request cancelRequest = new CancelV4Request();
        cancelRequest.setConfirmationNumber("12345");
        runTest(ErrorCode.NO_FIRST_NAME, cancelRequest, TestConstant.MGM_RESORTS);
    }
    
    @Test
    public void cancel_webClientWithServiceTokenNoLastName_validateErrorMessage() {

        CancelV4Request cancelRequest = new CancelV4Request();
        cancelRequest.setConfirmationNumber("12345");
        cancelRequest.setFirstName("Test");
        runTest(ErrorCode.NO_LAST_NAME, cancelRequest, TestConstant.MGM_RESORTS);
    }

    private void runTest(ErrorCode errorCode, CancelV4Request request, String source) {
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		BindingResult result = new DirectFieldBindingResult(request, "CancelV4Request");
		boolean validationExceptionOccured = false;
		try {
		    localValidatorFactory.validate(request, result);
		    cancellationV4Controller.cancelCommit(source, request, result, mockRequest);
		} catch (ValidationException e) {
		    validationExceptionOccured = true;
			assertTrue(e.getErrorCodes().contains(errorCode.getErrorCode()));
		} finally {
            assertTrue("Controller should throw ValidationException", validationExceptionOccured);
        }
	}
    
    @Test
    public void cancelPreviewTest() {
    	CancelV4Request request= getCancelV4Request();
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		BindingResult result = new DirectFieldBindingResult(request, "CancelV4Request");
		localValidatorFactory.validate(mockRequest, result);
		Mockito.when(cancelService.cancelPreviewReservation(request, null)).thenReturn(getCancelPreviewResponse());
		CancelRoomReservationV2Response response=cancellationV4Controller.cancelPreviewReservation(TestConstant.ICE, request, result, mockRequest);
		runAssertions(response);
    }
    
    @Test
    public void cancelCommitTest() {
    	CancelV4Request request= getCancelV4Request();
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		BindingResult result = new DirectFieldBindingResult(request, "CancelV4Request");
		localValidatorFactory.validate(mockRequest, result);
		Mockito.when(cancelService.cancelCommitReservation(request, null)).thenReturn(getCancelCommitResponse());
		CancelRoomReservationV2Response response = cancellationV4Controller.cancelCommit(TestConstant.ICE, request, result, mockRequest);
		runAssertions(response);
    }
    
    private void runAssertions(CancelRoomReservationV2Response response) {
    	assertNotNull(response);
		assertEquals("15T3R6HP40", response.getRoomReservation().getConfirmationNumber());
		assertEquals("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad", response.getRoomReservation().getPropertyId());
		assertEquals(204.08, response.getRoomReservation().getAmountDue());
		assertEquals(204.08, response.getRoomReservation().getDepositDetails().getRefundAmount());
		assertEquals(719.13, response.getRoomReservation().getDepositDetails().getForfeitAmount());
		assertEquals("Cancelled", response.getRoomReservation().getState().toString());
		assertNotNull(response.getRoomReservation().getSpecialRequests());
		assertNotNull(response.getRoomReservation().getPurchasedComponents());
		assertEquals("COMPONENTCD-v-CFBP1-d-TYP-v-COMPONENT-d-PROP-v-MV021-d-NRPCD-v-CFBP1", response.getRoomReservation().getPurchasedComponents().get(0).getId());
		assertTrue(response.getRoomReservation().getPurchasedComponents().get(0).isNonEditable());
		assertTrue(response.getRoomReservation().getPurchasedComponents().get(0).getIsPkgComponent());
    	
    }
    
}
