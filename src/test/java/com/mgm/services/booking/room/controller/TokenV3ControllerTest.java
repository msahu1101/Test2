package com.mgm.services.booking.room.controller;

import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.hibernate.validator.HibernateValidator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.response.TokenV2Response;
import com.mgm.services.booking.room.model.response.TokenV2Response.Profile;
import com.mgm.services.booking.room.service.CustomerService;
import com.mgm.services.booking.room.service.TokenService;
import com.mgm.services.booking.room.validator.TokenValidator;
import com.mgm.services.common.exception.ValidationException;
import com.mgm.services.common.model.ServicesSession;

/**
 * Unit test class to validate the request object with various param.
 * 
 * @author laknaray
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class TokenV3ControllerTest extends BaseRoomBookingTest {

	@InjectMocks
	private TokenV3Controller tokenV3Controller;

	@Mock
	private CustomerService customerService;
	
	@Mock
    private TokenValidator tokenValidator;
	
	@Mock
	TokenService tokenService;
	
	@Mock
	protected ServicesSession sSession;
	
	@Mock
	RoomCartController cartController;

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
   public void generateTokenTest() {
    	HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    	HttpSession sessionMock = Mockito.mock(HttpSession.class);
        Mockito.when(mockRequest.getSession()).thenReturn(sessionMock);
		Map<String, String> headers=new HashMap<>();
		headers.put(ServiceConstant.HEADER_OKTA_ACCESS_TOKEN, "Test");
		TokenV2Response response=new TokenV2Response();
		response.setAccessToken("Test");
		response.setTransientFlag(true);
		Mockito.when(tokenService.generateV2Token(Mockito.any())).thenReturn(response);
		Mockito.doNothing().when(cartController).repriceRoomsOnLogin();
		tokenV3Controller.generateToken(mockRequest,headers, "Test", "Test");
   }
    
    @Test
    public void generateTokenTestWithOsid() {
     	HttpServletRequest mockRequest = mock(HttpServletRequest.class);
     	HttpSession sessionMock = Mockito.mock(HttpSession.class);
        Mockito.when(mockRequest.getSession()).thenReturn(sessionMock);
 		Map<String, String> headers=new HashMap<>();
 		headers.put(ServiceConstant.HEADER_OKTA_ACCESS_TOKEN, "Test");
 		headers.put(ServiceConstant.HEADER_OKTA_SESSION_ID, "Test");
 		TokenV2Response response=new TokenV2Response();
 		response.setAccessToken("Test");
 		response.setTransientFlag(true);
 		Profile profile=new Profile();
 		profile.setMlifeNumber("123456");
 		response.setProfile(profile);
 		Mockito.when(tokenService.generateV2Token(Mockito.any())).thenReturn(response);
 		Mockito.doNothing().when(cartController).repriceRoomsOnLogin();
 		tokenV3Controller.generateToken(mockRequest,headers, "Test", "Test");
    }
    
    @Test
    public void generateTokenTestWithoutOsid() {
     	HttpServletRequest mockRequest = mock(HttpServletRequest.class);
     	HttpSession sessionMock = Mockito.mock(HttpSession.class);
        Mockito.when(mockRequest.getSession()).thenReturn(sessionMock);
 		Map<String, String> headers=new HashMap<>();
 		headers.put(ServiceConstant.SESSION_AGE, "1200");
 		tokenV3Controller.generateToken(mockRequest,headers, null, null);
    }
    
    @Test(expected=ValidationException.class)
    public void updateTokenTestStateNull() {
     	HttpServletRequest mockRequest = mock(HttpServletRequest.class);
 		Map<String, String> headers=new HashMap<>();
 		tokenV3Controller.updateToken(mockRequest,headers, "Test", null, "Test");
    }
    
    @Test
    public void updateTokenTest() {
     	HttpServletRequest mockRequest = mock(HttpServletRequest.class);
 		Map<String, String> headers=new HashMap<>();
 		TokenV2Response response=new TokenV2Response();
 		response.setAccessToken("Test");
 		response.setTransientFlag(true);
 		Profile profile=new Profile();
 		profile.setMlifeNumber("123456");
 		response.setProfile(profile);
 		Mockito.when(tokenService.generateV2Token(Mockito.any())).thenReturn(response);
 		tokenV3Controller.updateToken(mockRequest,headers, "Test", "Test", "Test");
    }

}
