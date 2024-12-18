package com.mgm.services.booking.room.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
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
import com.mgm.services.booking.room.model.request.CalendarPriceV2Request;
import com.mgm.services.booking.room.model.request.ResortPriceV2Request;
import com.mgm.services.booking.room.model.request.ResortPriceWithTaxV2Request;
import com.mgm.services.booking.room.model.request.RoomAvailabilityV2Request;
import com.mgm.services.booking.room.model.request.dto.MultiDateDTO;
import com.mgm.services.booking.room.model.request.dto.ResortPriceWithTaxDTO;
import com.mgm.services.booking.room.model.response.CalendarPriceV2Response;
import com.mgm.services.booking.room.model.response.MultiDateResortPrice;
import com.mgm.services.booking.room.model.response.MultiDateResortPriceResponse;
import com.mgm.services.booking.room.model.response.ResortPriceResponse;
import com.mgm.services.booking.room.model.response.RoomAvailabilityCombinedResponse;
import com.mgm.services.booking.room.service.CalendarPriceV2Service;
import com.mgm.services.booking.room.service.ResortPriceV2Service;
import com.mgm.services.booking.room.service.RoomAvailabilityV2Service;
import com.mgm.services.booking.room.validator.RBSTokenScopes;
import com.mgm.services.booking.room.validator.TokenValidator;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.ValidationException;

/**
 * Unit test class to validate the request object with various param.
 * 
 * @author vararora
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class PriceV2ControllerTest extends BaseRoomBookingTest {

    @InjectMocks
    private PriceV2Controller priceV2Controller;

    @Mock
    private RoomAvailabilityV2Service availabilityV2Service;

    @Mock
    private CalendarPriceV2Service calendarV2Service;
    
    @Mock
    private TokenValidator tokenValidator;
    
    @Mock
    ResortPriceV2Service resortPriceV2Service;

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
    public void getCalendarPrices_WithoutDates_validateErrorMessage() {
        CalendarPriceV2Request request = loadRequest("withoutDates");
        runTest(request, ErrorCode.INVALID_DATES);
    }

    @Test
    public void getCalendarPrices_WithInvalidStartDate_validateErrorMessage() {
        CalendarPriceV2Request request = loadRequest("withoutDates");
        request.setStartDate(getFutureLocalDate(-15));
        request.setEndDate(getFutureLocalDate(30));
        runTest(request, ErrorCode.INVALID_DATES);
    }

    @Test
    public void getCalendarPrices_WithInvalidEndDate_validateErrorMessage() {
        CalendarPriceV2Request request = loadRequest("withoutDates");
        request.setStartDate(getFutureLocalDate(15));
        request.setEndDate(getFutureLocalDate(-30));
        runTest(request, ErrorCode.INVALID_DATES);
    }

    @Test
    public void getCalendarPrices_WithInvalidPropertyId_validateErrorMessage() {
        CalendarPriceV2Request request = loadRequest("invalidPropertyId");
        request.setStartDate(getFutureLocalDate(15));
        request.setEndDate(getFutureLocalDate(30));
        runTest(request, ErrorCode.INVALID_PROPERTY);
    }

    @Test
    public void getCalendarPrices_WithNullPropertyId_validateErrorMessage() {
        CalendarPriceV2Request request = loadRequest("invalidPropertyId");
        request.setStartDate(getFutureLocalDate(15));
        request.setEndDate(getFutureLocalDate(30));
        request.setPropertyId(null);
        runTest(request, ErrorCode.INVALID_PROPERTY);
    }

    @Test
    public void getCalendarPrices_WithInvalidNumAdults_validateErrorMessage() {
        CalendarPriceV2Request request = loadRequest("withoutDates");
        request.setStartDate(getFutureLocalDate(15));
        request.setEndDate(getFutureLocalDate(30));
        request.setNumAdults(0);
        runTest(request, ErrorCode.INVALID_NUM_ADULTS);
    }

    @Test
    public void getCalendarPrices_WithNegativeNumAdults_validateErrorMessage() {
        CalendarPriceV2Request request = loadRequest("withoutDates");
        request.setStartDate(getFutureLocalDate(15));
        request.setEndDate(getFutureLocalDate(30));
        request.setNumAdults(-1);
        runTest(request, ErrorCode.INVALID_NUM_ADULTS);
    }

    private CalendarPriceV2Request loadRequest(String scenario) {
        return convert(
                new File(getClass().getResource(String.format("/getCalendarPrices-%s.json", scenario)).getPath()),
                CalendarPriceV2Request.class);
    }

    private void runTest(CalendarPriceV2Request request, ErrorCode errorCode) {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "CalendarPriceV2Request");
        boolean validationExceptionOccured = false;

        try {
            localValidatorFactory.validate(request, result);
            priceV2Controller.getCalendarPrices(TestConstant.ICE, request, result, mockRequest, null);
        } catch (ValidationException e) {
            validationExceptionOccured = true;
            assertEquals(errorCode.getErrorCode(), e.getErrorCodes().get(0));
        } finally {
            assertTrue("Controller should throw ValidationException", validationExceptionOccured);
        }
    }

    @Test
    public void getRoomAvailability_WithoutCheckinDate_validateErrorMessage() {
        RoomAvailabilityV2Request availabilityRequest = buildRoomAvailabilityRequest();
        availabilityRequest.setCheckInDate(null);
        runRoomAvailabilityTest(availabilityRequest, ErrorCode.INVALID_DATES);
    }
    
    @Test
    public void getCalendarPrices_positive() {
        @Valid CalendarPriceV2Request request = loadRequest("withoutDates");
        request.setStartDate(getFutureLocalDate(15));
        request.setEndDate(getFutureLocalDate(30));
        request.setNumAdults(1);
        runTest_positive(request);
    }

    private void runTest_positive(CalendarPriceV2Request request) {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "CalendarPriceV2Request");
        localValidatorFactory.validate(request, result);
        List<CalendarPriceV2Response> calendarPriceV2ResponseList=new ArrayList<>();
        CalendarPriceV2Response calendarPriceV2Response=new CalendarPriceV2Response();
        calendarPriceV2Response.setDate(new Date());
        calendarPriceV2ResponseList.add(calendarPriceV2Response);
        Mockito.when(calendarV2Service.getCalendarPrices(request)).thenReturn(calendarPriceV2ResponseList);
        List<CalendarPriceV2Response> response=priceV2Controller.getCalendarPrices(TestConstant.ICE, request, result, mockRequest, null);
        assertTrue(!response.isEmpty());
    }

    @Test
    public void getRoomAvailability_WithoutCheckoutDate_validateErrorMessage() {
        RoomAvailabilityV2Request availabilityRequest = buildRoomAvailabilityRequest();
        availabilityRequest.setCheckOutDate(null);
        runRoomAvailabilityTest(availabilityRequest, ErrorCode.INVALID_DATES);
    }

    @Test
    public void getRoomAvailability_WithInvalidCheckinDate_validateErrorMessage() {
        RoomAvailabilityV2Request availabilityRequest = buildRoomAvailabilityRequest();
        availabilityRequest.setCheckInDate(getFutureLocalDate(-15));
        runRoomAvailabilityTest(availabilityRequest, ErrorCode.INVALID_DATES);
    }

    @Test
    public void getRoomAvailability_WithInvalidCheckoutDate_validateErrorMessage() {
        RoomAvailabilityV2Request availabilityRequest = buildRoomAvailabilityRequest();
        availabilityRequest.setCheckOutDate(getFutureLocalDate(-30));
        runRoomAvailabilityTest(availabilityRequest, ErrorCode.INVALID_DATES);
    }

    @Test
    public void getRoomAvailability_WithInvalidPropertyId_validateErrorMessage() {
        RoomAvailabilityV2Request availabilityRequest = buildRoomAvailabilityRequest();
        availabilityRequest.setPropertyId("111-222-333");
        runRoomAvailabilityTest(availabilityRequest, ErrorCode.INVALID_PROPERTY);
    }

    @Test
    public void getRoomAvailability_WithNullPropertyId_validateErrorMessage() {
        RoomAvailabilityV2Request availabilityRequest = buildRoomAvailabilityRequest();
        availabilityRequest.setPropertyId(null);
        runRoomAvailabilityTest(availabilityRequest, ErrorCode.INVALID_PROPERTY);
    }

    @Test
    public void getRoomAvailability_WithInvalidNumAdults_validateErrorMessage() {
        RoomAvailabilityV2Request availabilityRequest = buildRoomAvailabilityRequest();
        availabilityRequest.setNumAdults(0);
        runRoomAvailabilityTest(availabilityRequest, ErrorCode.INVALID_NUM_ADULTS);
    }

    @Test
    public void getRoomAvailability_WithNegativeNumAdults_validateErrorMessage() {
        RoomAvailabilityV2Request availabilityRequest = buildRoomAvailabilityRequest();
        availabilityRequest.setNumAdults(-2);
        runRoomAvailabilityTest(availabilityRequest, ErrorCode.INVALID_NUM_ADULTS);
    }
    
    @Test
    public void getRoomAvailability_positive() {
    	 RoomAvailabilityV2Request request = buildRoomAvailabilityRequest();
        request.setCheckInDate(getFutureLocalDate(15));
        request.setCustomerId(123456);
        request.setMlifeNumber("123456");
        request.setCheckOutDate(getFutureLocalDate(30));
        runRoomAvailabilityTest_positive(request);
    }
    
    private void runRoomAvailabilityTest_positive(RoomAvailabilityV2Request request) {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "RoomAvailabilityV2Request");
        localValidatorFactory.validate(request, result);
        RoomAvailabilityCombinedResponse roomAvailabilityCombinedResponse=new RoomAvailabilityCombinedResponse();
        roomAvailabilityCombinedResponse.setAvailability(null);
        Mockito.when(availabilityV2Service.getRoomAvailability(request)).thenReturn(roomAvailabilityCombinedResponse);
        RoomAvailabilityCombinedResponse roomAvailabilityCombinedResponse1=priceV2Controller.getRoomAvailability(TestConstant.ICE, request, result, mockRequest, null);
        assertTrue(null!=roomAvailabilityCombinedResponse1);
    }

    private RoomAvailabilityV2Request buildRoomAvailabilityRequest() {
        RoomAvailabilityV2Request availabilityRequest = new RoomAvailabilityV2Request();
        availabilityRequest.setCustomerId(-1);
        availabilityRequest.setCheckInDate(getFutureLocalDate(15));
        availabilityRequest.setCheckOutDate(getFutureLocalDate(30));
        availabilityRequest.setNumAdults(2);
        availabilityRequest.setNumChildren(1);
        availabilityRequest.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        return availabilityRequest;
    }

    private void runRoomAvailabilityTest(RoomAvailabilityV2Request request, ErrorCode errorCode) {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "RoomAvailabilityV2Request");
        boolean validationExceptionOccured = false;

        try {
            localValidatorFactory.validate(request, result);
            priceV2Controller.getRoomAvailability(TestConstant.ICE, request, result, mockRequest, null);
        } catch (ValidationException e) {
            validationExceptionOccured = true;
            assertEquals(errorCode.getErrorCode(), e.getErrorCodes().get(0));
        } finally {
            assertTrue("Controller should throw ValidationException", validationExceptionOccured);
        }
    }

    //Report Price V2 related tests starts from here
    @Test
    public void getResortPrices_WithInvalidCheckinDate_validateErrorMessage() {
        ResortPriceV2Request request = buildResortPriceV2Request();
        request.setCheckInDate(getFutureLocalDate(-15));
        runResortPriceTest(request, ErrorCode.INVALID_DATES);
    }

    @Test
    public void getResortPrices_WithInvalidCheckoutDate_validateErrorMessage() {
        ResortPriceV2Request request = buildResortPriceV2Request();
        request.setCheckOutDate(getFutureLocalDate(-30));
        runResortPriceTest(request, ErrorCode.INVALID_DATES);
    }

    @Test
    public void getResortPrices_WithCheckIdDateLaterthanCheckOut_validateErrorMessage() {
        ResortPriceV2Request request = buildResortPriceV2Request();
        request.setCheckInDate(getFutureLocalDate(49));
        runResortPriceTest(request, ErrorCode.INVALID_DATES);
    }

    @Test
    public void getResortPrices_WithInvalidNumAdults_validateErrorMessage() {
        ResortPriceV2Request request = buildResortPriceV2Request();
        request.setNumAdults(0);
        runResortPriceTest(request, ErrorCode.INVALID_NUM_ADULTS);
    }

    @Test
    public void getResortPrices_WithInvalidPerpetualRequest_validateErrorMessage() {
        ResortPriceV2Request request = buildResortPriceV2Request();
        request.setPerpetualPricing(true);
        request.setCustomerId(-1);
        runResortPriceTest(request, ErrorCode.INVALID_CUSTOMER);
    }

    private ResortPriceV2Request buildResortPriceV2Request() {
        ResortPriceV2Request request = new ResortPriceV2Request();
        request.setCheckInDate(getFutureLocalDate(45));
        request.setCheckOutDate(getFutureLocalDate(48));
        request.setNumAdults(2);
        request.setPromo("ZNVLCLS");
        return request;
    }

    private void runResortPriceTest(ResortPriceV2Request request, ErrorCode errorCode) {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "ResortPriceV2Request");
        boolean validationExceptionOccured = false;

        try {
            localValidatorFactory.validate(request, result);
            priceV2Controller.getResortsAvailability(TestConstant.MGM_RESORTS, request, result, mockRequest, null);
        } catch (ValidationException e) {
            validationExceptionOccured = true;
            assertEquals(errorCode.getErrorCode(), e.getErrorCodes().get(0));
        } finally {
            assertTrue("Controller should throw ValidationException", validationExceptionOccured);
        }
    }
    
    @Test
    public void getResortPrices_positive() {
        ResortPriceV2Request request = buildResortPriceV2Request();
        request.setCustomerId(123456);
        request.setMlifeNumber("123456");
        runResortPriceTest_positive(request);
    }
    
    @Test
    public void getResortPrices_positive_PerpetualPricing() {
        ResortPriceV2Request request = buildResortPriceV2Request();
        request.setCustomerId(123456);
        request.setMlifeNumber("123456");
        request.setPerpetualPricing(true);
        runResortPriceTest_positive(request);
    }
    
    private void runResortPriceTest_positive(ResortPriceV2Request request) {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "ResortPriceV2Request");
        localValidatorFactory.validate(request, result);
        List<ResortPriceResponse> resortPriceResponseList=new ArrayList<>();
        ResortPriceResponse resortPriceResponse=new ResortPriceResponse();
        resortPriceResponseList.add(resortPriceResponse);
        if(request.isPerpetualPricing() && (StringUtils.isEmpty(request.getProgramId())
                && StringUtils.isEmpty(request.getSegment()) && StringUtils.isEmpty(request.getGroupCode()))) {
        	Mockito.when(resortPriceV2Service.getResortPerpetualPrices(request)).thenReturn(resortPriceResponseList);
        }else {
        	Mockito.when(resortPriceV2Service.getResortPrices(request)).thenReturn(resortPriceResponseList);
        }
        List<ResortPriceResponse> response=priceV2Controller.getResortsAvailability(TestConstant.MGM_RESORTS, request, result, mockRequest, null);
        assertTrue(!response.isEmpty());
    }
    
    @Test
    public void getResortsAvailabilityWithTaxAmtTest() {
    	ResortPriceWithTaxV2Request request = new ResortPriceWithTaxV2Request();
    	ResortPriceWithTaxDTO pricingRequest = new ResortPriceWithTaxDTO();
    	pricingRequest.setCustomerId(123);

    	String source = "test";
    	String enableJwb="true";
    	HttpServletRequest mockRequest = mock(HttpServletRequest.class);

    	BindingResult result = new DirectFieldBindingResult(request, "ResortPriceWithTaxV2Request");
    	localValidatorFactory.validate(request, result);

    	List<MultiDateDTO> multiDateDTOList = new ArrayList<>();

    	MultiDateDTO multiDateDTO = new MultiDateDTO();
    	multiDateDTO.setCheckIn(LocalDate.parse("2024-03-25"));
    	multiDateDTO.setCheckOut(LocalDate.parse("2024-03-30"));

    	multiDateDTOList.add(multiDateDTO);
    	pricingRequest.setDates(multiDateDTOList);
    	request.setRequest(pricingRequest);

    	List<ResortPriceWithTaxDTO> pricingRequestWithDatesList = new ArrayList<>();

    	ResortPriceWithTaxDTO resortPriceWithTaxDTO = new ResortPriceWithTaxDTO();
    	pricingRequestWithDatesList.add(resortPriceWithTaxDTO);

    	Mockito.doNothing().when(tokenValidator).validateToken(mockRequest,RBSTokenScopes.GET_ROOM_AVAILABILITY);

    	MultiDateResortPriceResponse response = priceV2Controller.getResortsAvailabilityWithTaxAmt(source, request, result, mockRequest, enableJwb);
    	assertNotNull(response);

    }

}
