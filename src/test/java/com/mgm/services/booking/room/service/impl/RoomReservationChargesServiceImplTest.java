package com.mgm.services.booking.room.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.dao.RoomReservationChargesDAO;
import com.mgm.services.booking.room.mapper.RoomReservationChargesRequestMapper;
import com.mgm.services.booking.room.mapper.RoomReservationChargesResponseMapper;
import com.mgm.services.booking.room.model.request.RoomReservationChargesRequest;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.RoomReservationChargesResponse;

/**
 * Unit test class for service methods in RoomReservationChargesService.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class RoomReservationChargesServiceImplTest extends BaseRoomBookingTest {

	@Mock
	private RoomReservationChargesDAO roomReservationChargesDAO;

	@InjectMocks
	private RoomReservationChargesServiceImpl roomReservationChargesServiceImpl;

	@Mock
	private RoomReservationChargesRequestMapper reservationChargesRequestMapper;

	@Mock
	private RoomReservationChargesResponseMapper reservationChargesResponseMapper;

	@Mock
	private CommonServiceImpl commonService;

	private RoomReservationChargesRequest getChargesRequest() {
		return convert(new File(getClass().getResource("/roomReservationRequest-charges.json").getPath()),
				RoomReservationChargesRequest.class);
	}

	private RoomReservation getRoomReservationRequest() {
		return convert(new File(getClass().getResource("/roomReservationRequest-charges.json").getPath()),
				RoomReservation.class);
	}

	private RoomReservationChargesResponse getRoomReservationChargesResponse() {
		return convert(new File(getClass().getResource("/roomReservationResponse-charges.json").getPath()),
				RoomReservationChargesResponse.class);
	}

	@Test
	public void calculateRoomReservationChargesTest_with_mlifeNumber() {

		when(reservationChargesRequestMapper.roomReservationChargesRequestToModel(Mockito.any())).thenReturn(getRoomReservationRequest());
		when(reservationChargesResponseMapper.reservationModelToRoomReservationChargesResponse(Mockito.any())).thenReturn(getRoomReservationChargesResponse());

		RoomReservationChargesRequest roomReservationChargesRequest = getChargesRequest();
		roomReservationChargesRequest.setMlifeNumber("1080506");

		RoomReservationChargesResponse response = roomReservationChargesServiceImpl.calculateRoomReservationCharges(roomReservationChargesRequest);
		runAssertions(response);

	}
	
	@Test
	public void calculateRoomReservationChargesTest_without_programId() {

		when(reservationChargesRequestMapper.roomReservationChargesRequestToModel(Mockito.any())).thenReturn(getRoomReservationRequest());
		when(reservationChargesResponseMapper.reservationModelToRoomReservationChargesResponse(Mockito.any())).thenReturn(getRoomReservationChargesResponse());

		RoomReservationChargesRequest roomReservationChargesRequest = getChargesRequest();
		roomReservationChargesRequest.setProgramId(null);

		RoomReservationChargesResponse response = roomReservationChargesServiceImpl.calculateRoomReservationCharges(roomReservationChargesRequest);
		runAssertions(response);
	}
	
	private void runAssertions(RoomReservationChargesResponse response) {
		assertEquals("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad", response.getPropertyId());
		assertEquals(246.99, response.getChargesAndTaxes().getCharges().get(0).getAmount(),0);
		assertEquals(33.05, response.getChargesAndTaxes().getTaxesAndFees().get(0).getAmount(),0);
		assertEquals(246.99, response.getChargesAndTaxes().getCharges().get(1).getAmount(),0);
		assertEquals(33.05, response.getChargesAndTaxes().getTaxesAndFees().get(1).getAmount(),0);
		assertNotNull(response.getAvailableComponents());
		assertEquals("DOGFEE", response.getAvailableComponents().get(0).getCode());
	}
	
}
