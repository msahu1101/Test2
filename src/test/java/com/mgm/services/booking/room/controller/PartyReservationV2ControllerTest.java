package com.mgm.services.booking.room.controller;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.validator.HibernateValidator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DirectFieldBindingResult;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.model.request.RoomReservationBasicInfoRequest;
import com.mgm.services.booking.room.validator.TokenValidator;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.ValidationException;

@RunWith(MockitoJUnitRunner.class)
public class PartyReservationV2ControllerTest {

    @InjectMocks
    private FindReservationV2Controller findReservationV2Controller;
    
    @Mock
    private TokenValidator tokenValidator;

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
    public void getPartyOrShareWithReservations_WithConfirmationNumberAndOperaPartyCode_ValidateException() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        RoomReservationBasicInfoRequest request = new RoomReservationBasicInfoRequest();
        request.setConfirmationNumber("MODE4410");
        request.setOperaPartyCode("OP");
        BindingResult result = new DirectFieldBindingResult(request, "RoomReservationBasicInfoRequest");
        boolean validationExceptionOccured = false;
        try {
            localValidatorFactory.validate(request, result);
            findReservationV2Controller.getPartyOrShareWithReservations(TestConstant.ICE, request, result, mockRequest,
                    null);
        } catch (ValidationException e) {
            validationExceptionOccured = true;
            assertTrue(e.getErrorCodes().contains(ErrorCode.INVALID_PARTY_RESERVATION_REQUEST.getErrorCode()));
        } finally {
            assertTrue("Controller should throw ValidationException", validationExceptionOccured);
        }
    }

    @Test
    public void getPartyOrShareWithReservations_WithNoParams_ValidateException() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        RoomReservationBasicInfoRequest request = new RoomReservationBasicInfoRequest();
        BindingResult result = new DirectFieldBindingResult(request, "RoomReservationBasicInfoRequest");
        boolean validationExceptionOccured = false;
        try {
            localValidatorFactory.validate(request, result);
            findReservationV2Controller.getPartyOrShareWithReservations(TestConstant.ICE, request, result, mockRequest,
                    null);
        } catch (ValidationException e) {
            validationExceptionOccured = true;
            assertTrue(e.getErrorCodes().contains(ErrorCode.INVALID_PARTY_RESERVATION_REQUEST.getErrorCode()));
        } finally {
            assertTrue("Controller should throw ValidationException", validationExceptionOccured);
        }
    }

}
