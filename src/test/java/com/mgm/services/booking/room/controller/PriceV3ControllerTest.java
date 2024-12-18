package com.mgm.services.booking.room.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

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
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.model.request.CalendarPriceV3Request;
import com.mgm.services.booking.room.model.request.RoomAvailabilityV3Request;
import com.mgm.services.booking.room.model.response.CalendarPriceV3Response;
import com.mgm.services.booking.room.model.response.RoomAvailabilityCombinedResponse;
import com.mgm.services.booking.room.service.CalendarPriceV3Service;
import com.mgm.services.booking.room.service.RoomAvailabilityV3Service;
import com.mgm.services.booking.room.validator.TokenValidator;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.ValidationException;

/**
 * Unit test class to validate the request object with various param.
 * 
 * @author laknaray
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class PriceV3ControllerTest extends BaseRoomBookingTest {

    @InjectMocks
    private PriceV3Controller priceV3Controller;

    @Mock
    private CalendarPriceV3Service calendarV3Service;
    
    @Mock
    private TokenValidator tokenValidator;

    private LocalValidatorFactoryBean localValidatorFactory;
    
    @Mock
    private RoomAvailabilityV3Service availabilityV3Service;

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
    public void getCalendarPrices_WithoutDates_validateErrorMessage() {
        @Valid CalendarPriceV3Request request = loadRequest();
        request.setStartDate(null);
        request.setEndDate(null);
        runTest(request, ErrorCode.INVALID_DATES);
    }

    @Test
    public void getCalendarPrices_WithInvalidStartDate_validateErrorMessage() {
        @Valid CalendarPriceV3Request request = loadRequest();
        request.setStartDate(getFutureLocalDate(-15));
        request.setEndDate(getFutureLocalDate(30));
        runTest(request, ErrorCode.INVALID_DATES);
    }

    @Test
    public void getCalendarPrices_WithInvalidEndDate_validateErrorMessage() {
        @Valid CalendarPriceV3Request request = loadRequest();
        request.setStartDate(getFutureLocalDate(15));
        request.setEndDate(getFutureLocalDate(-30));
        runTest(request, ErrorCode.INVALID_DATES);
    }

    @Test
    public void getCalendarPrices_WithInvalidPropertyId_validateErrorMessage() {
        @Valid CalendarPriceV3Request request = loadRequest();
        request.setStartDate(getFutureLocalDate(15));
        request.setEndDate(getFutureLocalDate(30));
        request.setPropertyId("111-2222-333");
        runTest(request, ErrorCode.INVALID_PROPERTY);
    }

    @Test
    public void getCalendarPrices_WithNullPropertyId_validateErrorMessage() {
        @Valid CalendarPriceV3Request request = loadRequest();
        request.setStartDate(getFutureLocalDate(15));
        request.setEndDate(getFutureLocalDate(30));
        request.setPropertyId(null);
        runTest(request, ErrorCode.INVALID_PROPERTY);
    }

    @Test
    public void getCalendarPrices_WithInvalidNumAdults_validateErrorMessage() {
        @Valid CalendarPriceV3Request request = loadRequest();
        request.setStartDate(getFutureLocalDate(15));
        request.setEndDate(getFutureLocalDate(30));
        request.setNumAdults(0);
        runTest(request, ErrorCode.INVALID_NUM_ADULTS);
    }

    @Test
    public void getCalendarPrices_WithNegativeNumAdults_validateErrorMessage() {
        @Valid CalendarPriceV3Request request = loadRequest();
        request.setStartDate(getFutureLocalDate(15));
        request.setEndDate(getFutureLocalDate(30));
        request.setNumAdults(-1);
        runTest(request, ErrorCode.INVALID_NUM_ADULTS);
    }

    private @Valid CalendarPriceV3Request loadRequest() {
        return convert(
                new File(getClass().getResource("/getCalendarPrices-v3Request.json").getPath()),
                CalendarPriceV3Request.class);
    }
    
    @Test
    public void getCalendarPrices_positive() {
        @Valid CalendarPriceV3Request request = loadRequest();
        request.setStartDate(getFutureLocalDate(15));
        request.setEndDate(getFutureLocalDate(30));
        request.setNumAdults(1);
        runTest_Positive(request);
    }

    @Test
    public void getCalendarPrices_invalid_customer() {
        @Valid CalendarPriceV3Request request = loadRequest();
        request.setStartDate(getFutureLocalDate(15));
        request.setEndDate(getFutureLocalDate(30));
        request.setPerpetualPricing(true);
        request.setCustomerId(-1);
        runTest(request, ErrorCode.INVALID_CUSTOMER);
    }

    private void runTest(@Valid CalendarPriceV3Request request, ErrorCode errorCode) {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "CalendarPriceV3Request");
        boolean validationExceptionOccured = false;

        try {
            localValidatorFactory.validate(request, result);
            priceV3Controller.getCalendarPrices(TestConstant.ICE, request, result, mockRequest, null);
        } catch (ValidationException e) {
            validationExceptionOccured = true;
            assertEquals(errorCode.getErrorCode(), e.getErrorCodes().get(0));
        } finally {
            assertTrue("Controller should throw ValidationException", validationExceptionOccured);
        }
    } 
    
    private void runTest_Positive(@Valid CalendarPriceV3Request request) {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "CalendarPriceV3Request");
        localValidatorFactory.validate(request, result);
        List<CalendarPriceV3Response> response=new ArrayList<>();
        CalendarPriceV3Response calendarPriceV3Response=new CalendarPriceV3Response();
        response.add(calendarPriceV3Response);
        Mockito.when(calendarV3Service.getLOSBasedCalendarPrices(request)).thenReturn(response);
        List<CalendarPriceV3Response> calendarPriceV3ResponseList=priceV3Controller.getCalendarPrices(TestConstant.ICE, request, result, mockRequest, null);
        assertTrue(!calendarPriceV3ResponseList.isEmpty());
    }

    private @Valid RoomAvailabilityV3Request loadRequest_getRoomAvailability() {
        return convert(
                new File(getClass().getResource("/getRoomAvailability-v3Request.json").getPath()),
                RoomAvailabilityV3Request.class);
    }

    @Test
    public void getRoomAvailability_positive() {
        @Valid RoomAvailabilityV3Request request = loadRequest_getRoomAvailability();
        request.setCheckInDate(getFutureLocalDate(15));
        request.setCheckOutDate(getFutureLocalDate(30));
        runTest_Positive_getRoomAvailability(request);
    }

    private void runTest_Positive_getRoomAvailability(@Valid RoomAvailabilityV3Request request) {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "CalendarPriceV3Request");
        localValidatorFactory.validate(request, result);
        RoomAvailabilityCombinedResponse response=new RoomAvailabilityCombinedResponse();
        response.setAvailability(null);
        Mockito.when(availabilityV3Service.getRoomAvailability(request)).thenReturn(response);
        RoomAvailabilityCombinedResponse roomAvailabilityCombinedResponse= priceV3Controller.getRoomAvailability(TestConstant.ICE, request, result, mockRequest, null);
        assertTrue(null!=roomAvailabilityCombinedResponse);
    }

    @Test
    public void getRoomAvailability_WithoutCheckinDate_validateErrorMessage() {
    	 @Valid RoomAvailabilityV3Request availabilityRequest = loadRequest_getRoomAvailability();
        availabilityRequest.setCheckInDate(null);
        runRoomAvailabilityTest(availabilityRequest, ErrorCode.INVALID_DATES);
    }

    @Test
    public void getRoomAvailability_WithoutCheckoutDate_validateErrorMessage() {
    	 @Valid RoomAvailabilityV3Request availabilityRequest = loadRequest_getRoomAvailability();
        availabilityRequest.setCheckOutDate(null);
        runRoomAvailabilityTest(availabilityRequest, ErrorCode.INVALID_DATES);
    }

    @Test
    public void getRoomAvailability_WithInvalidCheckinDate_validateErrorMessage() {
    	 @Valid RoomAvailabilityV3Request availabilityRequest = loadRequest_getRoomAvailability();
        availabilityRequest.setCheckInDate(getFutureLocalDate(-15));
        runRoomAvailabilityTest(availabilityRequest, ErrorCode.INVALID_DATES);
    }

    @Test
    public void getRoomAvailability_WithInvalidCheckoutDate_validateErrorMessage() {
    	 @Valid RoomAvailabilityV3Request availabilityRequest = loadRequest_getRoomAvailability();
        availabilityRequest.setCheckOutDate(getFutureLocalDate(-30));
        runRoomAvailabilityTest(availabilityRequest, ErrorCode.INVALID_DATES);
    }

    @Test
    public void getRoomAvailability_WithInvalidPropertyId_validateErrorMessage() {
    	 @Valid RoomAvailabilityV3Request availabilityRequest = loadRequest_getRoomAvailability();
        availabilityRequest.setPropertyId("111-222-333");
        availabilityRequest.setCheckInDate(getFutureLocalDate(10));
        availabilityRequest.setCheckOutDate(getFutureLocalDate(30));
        runRoomAvailabilityTest(availabilityRequest, ErrorCode.INVALID_PROPERTY);
    }

    @Test
    public void getRoomAvailability_WithNullPropertyId_validateErrorMessage() {
    	 @Valid RoomAvailabilityV3Request availabilityRequest = loadRequest_getRoomAvailability();
        availabilityRequest.setPropertyId(null);
        availabilityRequest.setCheckInDate(getFutureLocalDate(10));
        availabilityRequest.setCheckOutDate(getFutureLocalDate(30));
        runRoomAvailabilityTest(availabilityRequest, ErrorCode.INVALID_PROPERTY);
    }

    @Test
    public void getRoomAvailability_WithInvalidNumAdults_validateErrorMessage() {
    	 @Valid RoomAvailabilityV3Request availabilityRequest = loadRequest_getRoomAvailability();
        availabilityRequest.setNumAdults(0);
        availabilityRequest.setCheckInDate(getFutureLocalDate(10));
        availabilityRequest.setCheckOutDate(getFutureLocalDate(30));
        runRoomAvailabilityTest(availabilityRequest, ErrorCode.INVALID_NUM_ADULTS);
    }

    @Test
    public void getRoomAvailability_WithNegativeNumAdults_validateErrorMessage() {
    	 @Valid RoomAvailabilityV3Request request = loadRequest_getRoomAvailability();
    	 request.setNumAdults(-2);
    	 request.setCheckInDate(getFutureLocalDate(10));
    	 request.setCheckOutDate(getFutureLocalDate(30));
        runRoomAvailabilityTest(request, ErrorCode.INVALID_NUM_ADULTS);
    }

    private void runRoomAvailabilityTest(RoomAvailabilityV3Request request, ErrorCode errorCode) {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "RoomAvailabilityV2Request");
        boolean validationExceptionOccured = false;

        try {
            localValidatorFactory.validate(request, result);
            priceV3Controller.getRoomAvailability(TestConstant.ICE, request, result, mockRequest, null);
        } catch (ValidationException e) {
            validationExceptionOccured = true;
            assertEquals(errorCode.getErrorCode(), e.getErrorCodes().get(0));
        } finally {
            assertTrue("Controller should throw ValidationException", validationExceptionOccured);
        }
    }

}
