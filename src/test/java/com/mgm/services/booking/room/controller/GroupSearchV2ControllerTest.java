package com.mgm.services.booking.room.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
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
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.model.request.GroupSearchV2Request;
import com.mgm.services.booking.room.model.response.GroupSearchV2Response;
import com.mgm.services.booking.room.service.GroupSearchV2Service;
import com.mgm.services.booking.room.validator.TokenValidator;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.ValidationException;
import com.mgm.services.common.model.ServicesSession;

/**
 * Unit test class to validate the request object with various param.
 * 
 * @author jayveera
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class GroupSearchV2ControllerTest extends BaseRoomBookingTest {

	@InjectMocks
	private GroupSearchV2Controller GroupSearchV2Controller;

	@Mock
	private GroupSearchV2Service groupSearchService;
	
	@Mock
    private TokenValidator tokenValidator;
	
	@Mock
	protected ServicesSession sSession;

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
    public void shouldThrowExceptionOnInvalidDate() {
		GroupSearchV2Request request = new GroupSearchV2Request();
		request.setCustomerId(122344);
		request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        runTest(request, ErrorCode.INVALID_TRAVEL_DATE);
    }
	
	@Test
    public void shouldThrowExceptionOnInvalidPrpertyId() {
		GroupSearchV2Request request = new GroupSearchV2Request();
		request.setCustomerId(122344);
		request.setEndDate(getFutureLocalDate(15).toString());
		request.setStartDate(getFutureLocalDate(30).toString());
        runTest(request, ErrorCode.INVALID_PROPERTY);
    }
    
    
    private void runTest(GroupSearchV2Request request, ErrorCode errorCode) {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "GroupSearchV2Request");
        boolean validationExceptionOccured = false;

        try {
            localValidatorFactory.validate(request, result);
            GroupSearchV2Controller.searchGroup(TestConstant.ICE, request, result, mockRequest);
        } catch (ValidationException e) {
            validationExceptionOccured = true;
            assertEquals("Error Code should match: ", errorCode.getErrorCode(), e.getErrorCodes().get(0));
        } finally {
            assertTrue("Controller should throw ValidationException", validationExceptionOccured);
        }
    }
    
    @Test
    public void searchGroupPositiveTest() {
    	GroupSearchV2Request request = new GroupSearchV2Request();
		request.setCustomerId(122344);
		request.setMlifeNumber("12234");
		request.setEndDate(getFutureLocalDate(30).toString());
		request.setStartDate(getFutureLocalDate(15).toString());
		request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
		
		
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "GroupSearchV2Request");
        localValidatorFactory.validate(request, result);
        GroupSearchV2Response groupSearchV2Response=new GroupSearchV2Response();
		groupSearchV2Response.setActiveFlag(true);
		List<GroupSearchV2Response> groupSearchV2ResponseList=new ArrayList<>();
		groupSearchV2ResponseList.add(groupSearchV2Response);
		Mockito.when(groupSearchService.searchGroup(request)).thenReturn(groupSearchV2ResponseList);
		List<GroupSearchV2Response> response= GroupSearchV2Controller.searchGroup(TestConstant.ICE, request, result, mockRequest);
		assertEquals(groupSearchV2ResponseList,response);
    }
    
    @Test(expected=ValidationException.class)
    public void searchGroupNegativeTest() {
    	GroupSearchV2Request request = new GroupSearchV2Request();
		request.setCustomerId(122344);
		request.setMlifeNumber("12234");
		request.setEndDate(getFutureLocalDate(15).toString());
		request.setStartDate(getFutureLocalDate(30).toString());
		request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
		
		
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "GroupSearchV2Request");
        localValidatorFactory.validate(request, result);
        GroupSearchV2Response groupSearchV2Response=new GroupSearchV2Response();
		groupSearchV2Response.setActiveFlag(true);
		GroupSearchV2Controller.searchGroup(TestConstant.ICE, request, result, mockRequest);
    }
}
