package com.mgm.services.booking.room.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
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
import com.mgm.services.booking.room.model.RoomComponent;
import com.mgm.services.booking.room.model.request.PackageComponentRequest;
import com.mgm.services.booking.room.model.request.RoomComponentV2Request;
import com.mgm.services.booking.room.service.ComponentService;
import com.mgm.services.booking.room.validator.TokenValidator;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.ValidationException;

/**
 * This class contains the unit tests of RoomDetailsController.
 * 
 * @author laknaray
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class RoomDetailsControllerTest extends BaseRoomBookingTest {

    @InjectMocks
    private RoomDetailsController roomDetailsController;
    
    @Mock
    private TokenValidator tokenValidator;

    private LocalValidatorFactoryBean localValidatorFactory;
    
    @Mock
    ComponentService componentService;

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
    public void roomDetails_withoutPropertyId_validateErrorMessage() {
        RoomComponentV2Request request = buildRequest();
        request.setPropertyId(null);
        request.setCheckInDate(LocalDate.now().plusDays(1));
        request.setCheckOutDate(LocalDate.now().plusDays(2));
        runTest(request, ErrorCode.INVALID_PROPERTY);
    }

    @Test
    public void roomDetails_withoutRoomTypeId_validateErrorMessage() {
        RoomComponentV2Request request = buildRequest();
        request.setRoomTypeId(null);
        request.setCheckInDate(LocalDate.now().plusDays(1));
        request.setCheckOutDate(LocalDate.now().plusDays(2));
        runTest(request, ErrorCode.INVALID_ROOMTYPE);
    }

    @Test
    public void roomDetails_withoutTravelStartDate_validateErrorMessage() {
        RoomComponentV2Request request = buildRequest();
        request.setCheckInDate(null);
        runTest(request, ErrorCode.INVALID_DATES);
    }

    @Test
    public void roomDetails_withoutTravelEndDate_validateErrorMessage() {
        RoomComponentV2Request request = buildRequest();
        request.setCheckOutDate(null);
        runTest(request, ErrorCode.INVALID_DATES);
    }

    @Test
    public void roomDetails_withIncorrectTravelDates_validateErrorMessage() {
        RoomComponentV2Request request = buildRequest();
        request.setCheckOutDate(getFutureLocalDate(40));
        runTest(request, ErrorCode.INVALID_DATES);
    }

    private RoomComponentV2Request buildRequest() {
        RoomComponentV2Request request = new RoomComponentV2Request();
        request.setCheckInDate(getFutureLocalDate(45));
        request.setCheckOutDate(getFutureLocalDate(45));
        request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        request.setRoomTypeId("b1fed3fd-4934-4cab-949d-90575ccfb9de");
        return request;
    }
    
    private void runTest(RoomComponentV2Request request, ErrorCode errorCode) {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "RoomComponentV2Request");
        boolean validationExceptionOccured = false;

        try {
            localValidatorFactory.validate(request, result);
            roomDetailsController.getAvailableRoomComponents(TestConstant.ICE, request, result, mockRequest);
        } catch (ValidationException e) {
            validationExceptionOccured = true;
            assertEquals("Error Code should match: ", errorCode.getErrorCode(), e.getErrorCodes().get(0));
        } finally {
            assertTrue("Controller should throw ValidationException", validationExceptionOccured);
        }
    }
    
    @Test
    public void getAvailableRoomComponentsTest() {
    	RoomComponentV2Request request=new RoomComponentV2Request();
    	request.setCheckInDate(getFutureLocalDate(40));
        request.setCheckOutDate(getFutureLocalDate(45));
        request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        request.setRoomTypeId("b1fed3fd-4934-4cab-949d-90575ccfb9de");
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "RoomComponentV2Request");
        localValidatorFactory.validate(request, result);
        List<RoomComponent> roomComponentList=new ArrayList<>();
        Mockito.when(componentService.getAvailableRoomComponents(request)).thenReturn(roomComponentList);
        List<RoomComponent> response=roomDetailsController.getAvailableRoomComponents(TestConstant.ICE, request, result, mockRequest);
        assertEquals(roomComponentList,response);
    }
    
    @Test
    public void getPackageComponentsTest() {
    	/*PackageComponentRequest pkgComponentRequest = new PackageComponentRequest();
    	pkgComponentRequest.setChannel("ice");
    	 HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
         BindingResult bindingResult = new DirectFieldBindingResult(pkgComponentRequest, "PackageComponentRequest");
         localValidatorFactory.validate(httpServletRequest, bindingResult);
         List<PackageComponentResponse> packageComponentResponseList = new ArrayList<>();      
         Mockito.when(componentService.getAvailablePackageComponents(pkgComponentRequest)).thenReturn(packageComponentResponseList);
    	List<PackageComponentResponse> response = roomDetailsController.getPackageComponents("ice", pkgComponentRequest, bindingResult, httpServletRequest);
    	assertEquals(packageComponentResponseList,response);
    	*/

    }

}
