package com.mgm.services.booking.room.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
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
import com.mgm.services.booking.room.model.request.CustomerOffersV3Request;
import com.mgm.services.booking.room.model.request.RoomSegmentRequest;
import com.mgm.services.booking.room.model.response.CustomerOffer;
import com.mgm.services.booking.room.model.response.CustomerOfferV3Response;
import com.mgm.services.booking.room.model.response.RoomSegmentResponse;
import com.mgm.services.booking.room.model.response.RoomSegmentResponse.Program;
import com.mgm.services.booking.room.service.RoomProgramService;
import com.mgm.services.booking.room.validator.TokenValidator;

/**
 * Unit test class to validate the request object with various param.
 * 
 * @author laknaray
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ProgramV3ControllerTest extends BaseRoomBookingTest {

	@InjectMocks
	private ProgramV3Controller programV3Controller;

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
   public void runTestGetCustomerOffersTest() {
    	CustomerOffersV3Request request=new CustomerOffersV3Request();
    	request.setPropertyId("111-222-333");
    	request.setCustomerId(123456);
    	request.setMlifeNumber("123456");
    	List<String> perpetualEligiblePropertyIds=new ArrayList<>();
    	perpetualEligiblePropertyIds.add("111-222-333");
    	request.setPerpetualEligiblePropertyIds(perpetualEligiblePropertyIds);
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "CustomerOffersRequest");
       localValidatorFactory.validate(request, result);
       CustomerOfferV3Response response=new CustomerOfferV3Response();
       List<CustomerOffer> offers=new ArrayList<>();
       CustomerOffer customerOffer=new CustomerOffer();
       customerOffer.setPromo("123");
       offers.add(customerOffer);
       response.setOffers(offers);
       Mockito.when(roomProgramService.getCustomerOffers(request)).thenReturn(response);
       CustomerOfferV3Response customerOfferResponse=programV3Controller
    		   .getCustomerOffers(TestConstant.ICE,"web", request, result, mockRequest);
       assertEquals(response,customerOfferResponse);
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
        Mockito.when(roomProgramService.getRoomSegment(request.getSegment(), null,"ice")).thenReturn(roomSegmentResponse);
        RoomSegmentResponse response=programV3Controller
     		   .getRoomSegment(TestConstant.ICE, request, result, mockRequest);
        assertEquals(response,roomSegmentResponse);
    }
}
