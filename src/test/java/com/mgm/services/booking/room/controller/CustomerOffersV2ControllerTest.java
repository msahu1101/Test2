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

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.model.request.CustomerOffersRequest;
import com.mgm.services.booking.room.service.RoomProgramService;
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
public class CustomerOffersV2ControllerTest extends BaseRoomBookingTest {

    @InjectMocks
    private ProgramV2Controller programV2Controller;

    @Mock
    private RoomProgramService roomProgramService;
    
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
    public void getCustomerOffers_WithInvalidPropertyId_validateErrorMessage() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        CustomerOffersRequest customerOffersRequest = new CustomerOffersRequest();
        customerOffersRequest.setPropertyId("1111-2222-333");

        BindingResult result = new DirectFieldBindingResult(customerOffersRequest, "CustomerOffersRequest");
        try {
            localValidatorFactory.validate(customerOffersRequest, result);
            programV2Controller.getCustomerOffers(TestConstant.ICE, customerOffersRequest, result, mockRequest, null);
            assertTrue(1 == 0);// if the control reaches here, the test fails.
        } catch (ValidationException e) {
            assertTrue(e.getErrorCodes().contains(ErrorCode.INVALID_PROPERTY.getErrorCode()));
        }
    }
}
