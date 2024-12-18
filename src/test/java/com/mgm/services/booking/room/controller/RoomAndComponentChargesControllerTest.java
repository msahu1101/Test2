package com.mgm.services.booking.room.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;

import com.mgm.services.booking.room.model.request.CalculateRoomChargesRequest;
import com.mgm.services.booking.room.model.response.CalculateRoomChargesResponse;
import com.mgm.services.booking.room.properties.AuroraProperties;
import com.mgm.services.booking.room.service.RoomAndComponentChargesService;
import com.mgm.services.booking.room.validator.RoomAndComponentChargesRequestValidator;
import com.mgm.services.booking.room.validator.TokenValidator;

class RoomAndComponentChargesControllerTest {

	@Mock
	private RoomAndComponentChargesService roomAndComponentChargesService;
	@Mock
	private TokenValidator tokenValidator;
	@Mock
	RoomAndComponentChargesRequestValidator roomAndComponentChargesRequestValidator;
	@Mock
	private AuroraProperties properties;
	   
	@InjectMocks
	RoomAndComponentChargesController roomAndComponentChargesController;

	@BeforeEach
	void init() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	void testCalculateRoomAndComponentCharges() throws Exception {
		Mockito.doNothing().when(tokenValidator).validateServiceToken(Mockito.any(), Mockito.any());
		
		CalculateRoomChargesResponse calculateRoomChargesResponse = new CalculateRoomChargesResponse();
		calculateRoomChargesResponse.setAmountDue(300);
		calculateRoomChargesResponse.setId("testId");
		Mockito.when(roomAndComponentChargesService.calculateRoomAndComponentCharges(Mockito.any()))
				.thenReturn(calculateRoomChargesResponse);
		Mockito.doNothing().when(roomAndComponentChargesRequestValidator).validate(Mockito.any(), Mockito.any());
		
		AuroraProperties.JWBCustomer randomJWBCustomer = new AuroraProperties.JWBCustomer();
		randomJWBCustomer.setCustId(Long.valueOf("1"));
		randomJWBCustomer.setMlifeNo("123");
		Mockito.when(properties.getRandomJWBCustomer()).thenReturn(randomJWBCustomer);
		
		CalculateRoomChargesRequest calculateRoomChargesRequest = new CalculateRoomChargesRequest();
		HttpServletRequest servletRequest = new MockHttpServletRequest();
		
		CalculateRoomChargesResponse response = 
				roomAndComponentChargesController.calculateRoomAndComponentCharges(
						"Bearer Token", "source", "channel", calculateRoomChargesRequest, servletRequest, "true");
		
		assertNotNull(response);
		assertEquals(300, response.getAmountDue());
		assertEquals("testId", response.getId());
	}
}
