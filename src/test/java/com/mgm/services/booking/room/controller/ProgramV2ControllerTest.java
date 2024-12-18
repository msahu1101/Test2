package com.mgm.services.booking.room.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
import org.springframework.validation.BindingResult;
import org.springframework.validation.DirectFieldBindingResult;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.model.request.ApplicableProgramsRequest;
import com.mgm.services.booking.room.model.request.CustomerOffersRequest;
import com.mgm.services.booking.room.model.request.PerpetualProgramRequest;
import com.mgm.services.booking.room.model.request.RoomProgramV2Request;
import com.mgm.services.booking.room.model.request.RoomProgramValidateRequest;
import com.mgm.services.booking.room.model.request.RoomSegmentRequest;
import com.mgm.services.booking.room.model.response.ApplicableProgramsResponse;
import com.mgm.services.booking.room.model.response.CustomerOfferResponse;
import com.mgm.services.booking.room.model.response.RoomOfferDetails;
import com.mgm.services.booking.room.model.response.RoomSegmentResponse;
import com.mgm.services.booking.room.model.response.RoomSegmentResponse.Program;
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
public class ProgramV2ControllerTest extends BaseRoomBookingTest {

	@InjectMocks
	private ProgramV2Controller programV2Controller;

	@Mock
	private RoomProgramService roomProgramService;
	
	@Mock
    private TokenValidator tokenValidator;
	
	@Mock
	Validator perpetualProgramsValidator;
	
	@Mock
	Validator validatePromoValidator;
	
	@Mock
	Validator roomProgramValidator;
	
	@Mock
	Validator roomSegmentValidator;

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
	public void getApplicablePrograms_WithInvalidTripDates_validateErrorMessage() {
		runTest("invalidtripdates", ErrorCode.INVALID_DATES);
	}

	@Test
	public void getApplicablePrograms_WithInvalidBookDate_validateErrorMessage() {
		runTest("invalidbookdate", ErrorCode.INVALID_BOOK_DATE);
	}

	@Test
	public void getApplicablePrograms_WithInvalidTravelDate_validateErrorMessage() {
		runTest("invalidtraveldate", ErrorCode.INVALID_TRAVEL_DATE);
	}

	@Test
	public void getApplicablePrograms_WithInvalidNumberOfGuests_validateErrorMessage() {
		runTest("invalidnumadults", ErrorCode.INVALID_NUM_ADULTS);
	}

	@Test
	public void getApplicablePrograms_WithInvalidPropertyId_validateErrorMessage() {
		runTest("invalidpropertyid", ErrorCode.INVALID_PROPERTY);
	}

	@Test
	public void getApplicablePrograms_WithInvalidRoomTypeId_validateErrorMessage() {
		runTest("invalidroomtypeid", ErrorCode.INVALID_ROOMTYPE);
	}

	private void runTest(String scenario, ErrorCode errorCode) {
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		ApplicableProgramsRequest request = convert(
				new File(
						getClass().getResource(String.format("/getapplicableprograms-%s.json", scenario)).getPath()),
				ApplicableProgramsRequest.class);
		BindingResult result = new DirectFieldBindingResult(request, "ApplicableProgramsRequest");
		try {
		    localValidatorFactory.validate(request, result);
			programV2Controller.getApplicablePrograms(TestConstant.ICE, request, result, mockRequest, null);
			assertTrue(1==0);
		} catch (ValidationException e) {
			assertTrue(e.getErrorCodes().contains(errorCode.getErrorCode()));
		}

	}
	
	@Test
    public void shouldThrowExceptionOnMissingCustomerId() {
	    PerpetualProgramRequest request = new PerpetualProgramRequest();
        runTest(request, ErrorCode.INVALID_CUSTOMER);
    }
	
	@Test
    public void shouldThrowExceptionOnInvalidCustomerId() {
        PerpetualProgramRequest request = new PerpetualProgramRequest();
        request.setCustomerId(-1);
        runTest(request, ErrorCode.INVALID_CUSTOMER);
    }
	
	@Test
    public void shouldThrowExceptionOnInvalidPropertyId() {
        PerpetualProgramRequest request = new PerpetualProgramRequest();
        request.setCustomerId(123);
        request.setPropertyIds(Collections.singletonList("invalidPropertyId"));
        runTest(request, ErrorCode.INVALID_PROPERTY_ID);
    }
	
	private void runTest(PerpetualProgramRequest request, ErrorCode errorCode) {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "PerpetualProgramRequest");
        boolean validationExceptionOccured = false;

        try {
            localValidatorFactory.validate(request, result);
            programV2Controller.getDefaultPerpetualPrograms(TestConstant.ICE, request, result, mockRequest, null);
        } catch (ValidationException e) {
            validationExceptionOccured = true;
            assertEquals("Error Code should match: ", errorCode.getErrorCode(), e.getErrorCodes().get(0));
        } finally {
            assertTrue("Controller should throw ValidationException", validationExceptionOccured);
        }
    }
	
	@Test
	public void getDefaultPerpetualProgramsTest() {
		PerpetualProgramRequest request=new PerpetualProgramRequest();
		request.setCustomerId(123456);
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "PerpetualProgramRequest");
        localValidatorFactory.validate(request, result);
        programV2Controller.getDefaultPerpetualPrograms(TestConstant.ICE, request, result, mockRequest, null);
    }
	
	@Test
    public void shouldThrowExceptionOnMissingAllParamsForValidateRoomOffer() {
	    RoomProgramValidateRequest request = new RoomProgramValidateRequest();
        runTest(request, ErrorCode.NO_PROGRAM);
    }
	
	@Test
    public void shouldThrowExceptionOnMissingPropertyIdForValidateRoomOffer() {
        RoomProgramValidateRequest request = new RoomProgramValidateRequest();
        request.setPromoCode("promoCode");
        runTest(request, ErrorCode.NO_PROPERTY);
    }
	
	@Test
    public void shouldThrowExceptionOnInvalidPropertyIdForValidateRoomOffer() {
        RoomProgramValidateRequest request = new RoomProgramValidateRequest();
        request.setPropertyId("propertyId");
        request.setProgramId("5c629ee4-ec38-4a07-a8d8-b435e0ef4069");
        runTest(request, ErrorCode.INVALID_PROPERTY_ID);
    }
	
	@Test
    public void shouldThrowExceptionOnInvalidProgramIdForValidateRoomOffer() {
        RoomProgramValidateRequest request = new RoomProgramValidateRequest();
        request.setProgramId("programId");
        runTest(request, ErrorCode.INVALID_PROGRAM_ID);
    }
	
	@Test
	public void validateRoomOfferTest() {
		RoomProgramValidateRequest request=new RoomProgramValidateRequest();
		request.setProgramId("programId");
		request.setProgramId("5c629ee4-ec38-4a07-a8d8-b435e0ef4069");
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "RoomProgramValidateRequest");
        localValidatorFactory.validate(request, result);
        programV2Controller.validateRoomOffer(TestConstant.ICE, request, result, mockRequest, null);
    }
    
    
    private void runTest(RoomProgramValidateRequest request, ErrorCode errorCode) {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "RoomProgramValidateRequest");
        boolean validationExceptionOccured = false;

        try {
            localValidatorFactory.validate(request, result);
            programV2Controller.validateRoomOffer(TestConstant.ICE, request, result, mockRequest, null);
        } catch (ValidationException e) {
            validationExceptionOccured = true;
            assertEquals("Error Code should match: ", errorCode.getErrorCode(), e.getErrorCodes().get(0));
        } finally {
            assertTrue("Controller should throw ValidationException", validationExceptionOccured);
        }
    }
    
    @Test
    public void shouldThrowExceptionOnInvalidPropertyIdForApplicablePrograms() {
    	ApplicableProgramsRequest request = new ApplicableProgramsRequest();
        runTest(request, ErrorCode.INVALID_PROPERTY);
    }
    
    @Test
   public void getApplicablePrograms_positive_test() {
    	ApplicableProgramsRequest request=new ApplicableProgramsRequest();
    	request.setPropertyId("111-222-333");
    	List<String> perpetualEligiblePropertyIds=new ArrayList<>();
    	perpetualEligiblePropertyIds.add("111-222-333");
    	request.setPerpetualEligiblePropertyIds(perpetualEligiblePropertyIds);
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "ApplicableProgramsRequest");
       localValidatorFactory.validate(request, result);
       ApplicableProgramsResponse response=new ApplicableProgramsResponse();
       response.setProgramIds(null);
       Mockito.when(roomProgramService.getApplicablePrograms(request)).thenReturn(response);
       ApplicableProgramsResponse applicableProgramsResponse=programV2Controller
    		   .getApplicablePrograms(TestConstant.ICE, request, null, mockRequest, TestConstant.ENABLE_JWB + "=true");
       assertEquals(response,applicableProgramsResponse);
   }
    
    private void runTest(ApplicableProgramsRequest request, ErrorCode errorCode) {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "RoomProgramValidateRequest");
        boolean validationExceptionOccured = false;

        try {
            localValidatorFactory.validate(request, result);
            programV2Controller.getApplicablePrograms(TestConstant.ICE, request, result, mockRequest, null);
        } catch (ValidationException e) {
            validationExceptionOccured = true;
            assertEquals("Error Code should match: ", errorCode.getErrorCode(), e.getErrorCodes().get(0));
        } finally {
            assertTrue("Controller should throw ValidationException", validationExceptionOccured);
        }
    }
    
    @Test
   public void runTestGetCustomerOffersTest() {
    	CustomerOffersRequest request=new CustomerOffersRequest();
    	request.setPropertyId("111-222-333");
    	request.setCustomerId(123456);
    	request.setMlifeNumber("123456");
    	List<String> perpetualEligiblePropertyIds=new ArrayList<>();
    	perpetualEligiblePropertyIds.add("111-222-333");
    	request.setPerpetualEligiblePropertyIds(perpetualEligiblePropertyIds);
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "CustomerOffersRequest");
       localValidatorFactory.validate(request, result);
       CustomerOfferResponse response=new CustomerOfferResponse();
       response.setOffers(null);
       Mockito.when(roomProgramService.getCustomerOffers(request)).thenReturn(response);
       CustomerOfferResponse customerOfferResponse=programV2Controller
    		   .getCustomerOffers(TestConstant.ICE, request, null, mockRequest, TestConstant.ENABLE_JWB + "=true");
       assertEquals(response,customerOfferResponse);
   }
    
    @Test
    public void getRoomProgramTest() {
    	RoomProgramV2Request request=new RoomProgramV2Request();
     	request.setCustomerId(123456);
     	request.setMlifeNumber("123456");
     	request.setStartDate(new Date().toString());
     	request.setEndDate(new Date().toString());
     	List<String> programIds=new ArrayList<>();
     	programIds.add("123456");
     	request.setProgramIds(programIds);
     	List<String> perpetualEligiblePropertyIds=new ArrayList<>();
     	perpetualEligiblePropertyIds.add("111-222-333");
     	request.setPerpetualEligiblePropertyIds(perpetualEligiblePropertyIds);
         HttpServletRequest mockRequest = mock(HttpServletRequest.class);
         BindingResult result = new DirectFieldBindingResult(request, "CustomerOffersRequest");
        localValidatorFactory.validate(request, result);
        List<RoomOfferDetails> roomOfferDetailsList=new ArrayList<>();
        RoomOfferDetails roomOfferDetails=new RoomOfferDetails();
        roomOfferDetails.setDescription("room test");
        roomOfferDetailsList.add(roomOfferDetails);
        Mockito.when(roomProgramService.getProgram(request)).thenReturn(roomOfferDetailsList);
        List<RoomOfferDetails> response=programV2Controller
     		   .getRoomProgram(TestConstant.ICE, request, null, mockRequest, TestConstant.ENABLE_JWB + "=true");
        assertEquals(response,roomOfferDetailsList);
    }
    
    @Test
    public void getRoomSegmentTest() {
    	RoomSegmentRequest request=new RoomSegmentRequest();
     	request.setCustomerId(123456);
     	request.setSegment("Segment");
     	request.setSource("Source");
     	request.setMlifeNumber("123456");
     	List<String> perpetualEligiblePropertyIds=new ArrayList<>();
     	perpetualEligiblePropertyIds.add("111-222-333");
     	request.setPerpetualEligiblePropertyIds(perpetualEligiblePropertyIds);
         HttpServletRequest mockRequest = mock(HttpServletRequest.class);
         BindingResult result = new DirectFieldBindingResult(request, "CustomerOffersRequest");
        localValidatorFactory.validate(request, result);
        RoomSegmentResponse roomSegmentResponse=new RoomSegmentResponse();
        roomSegmentResponse.setSegment("Segment");
        roomSegmentResponse.setMinTravelPeriodStart(new Date());
        roomSegmentResponse.setMaxTravelPeriodEnd(new Date());
        List<Program> programList=new ArrayList<>();
        roomSegmentResponse.setPrograms(programList);
        Mockito.when(roomProgramService.getRoomSegment(request.getSegment(), "ice")).thenReturn(roomSegmentResponse);
        RoomSegmentResponse response=programV2Controller
     		   .getRoomSegment(TestConstant.ICE, request, result, mockRequest);
        assertEquals(response,roomSegmentResponse);
    }
    
    
}
