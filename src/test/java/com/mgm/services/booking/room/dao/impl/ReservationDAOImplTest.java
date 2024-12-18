package com.mgm.services.booking.room.dao.impl;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.request.RoomCartRequest;
import com.mgm.services.booking.room.model.reservation.PartyRoomReservation;
import com.mgm.services.booking.room.model.reservation.RoomReservation;

@RunWith(MockitoJUnitRunner.class)
public class ReservationDAOImplTest extends BaseRoomBookingTest{

	@Mock
	private static ReservationDAOStrategyACRSImpl acrsStrategy;
	
	@Mock
	private ReferenceDataDAOHelper referenceDataDAOHelper;

	@InjectMocks
	private ReservationDAOImpl reservationDAOImpl  = new ReservationDAOImpl();

	static Logger logger = LoggerFactory.getLogger(ReservationDAOImplTest.class);
	
	private RoomReservation getRoomReservationReq() {
		File file = new File(getClass().getResource("/roomReservation-validRequest.json").getPath());
		RoomReservation data = convertRBSReq(file, RoomReservation.class);
		return data;
	}

	private RoomReservation getRoomReservationResponse() {
		File file = new File(getClass().getResource("/reservation-booked.json").getPath());
		RoomReservation data = convertRBSReq(file, RoomReservation.class);
		return data;
	}
	
	private RoomReservation getPartyRoomReservationReq() {
		File file = new File(getClass().getResource("/partyRoomReservation-validRequest.json").getPath());
		RoomReservation data = convertRBSReq(file, RoomReservation.class);
		return data;
	}
	
	private PartyRoomReservation getPartyRoomReservationResponse() {
		File file = new File(getClass().getResource("/reservation-booked.json").getPath());
		PartyRoomReservation data = convertRBSReq(file, PartyRoomReservation.class);
		return data;
	}
	
	@Test
	public void prepareRoomCartItemTest() {
		        
        RoomCartRequest roomCartRequest = new RoomCartRequest();
		roomCartRequest.setCheckInDate(LocalDate.parse("2024-03-25"));
		roomCartRequest.setCheckOutDate(LocalDate.parse("2024-03-30"));
		roomCartRequest.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
		List<String> auroraItineraryIds = new ArrayList<String>();
		auroraItineraryIds.add("itenaryId1");
		auroraItineraryIds.add("itenaryId2");
		roomCartRequest.setAuroraItineraryIds(auroraItineraryIds);
		;
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
		when(acrsStrategy.prepareRoomCartItem(ArgumentMatchers.any())).thenReturn(getRoomReservationReq());
		RoomReservation response = reservationDAOImpl.prepareRoomCartItem(roomCartRequest);

		Assert.assertNotNull(response);
	}
	
	@Test
	public void updateRoomReservationTest() {
		
		RoomReservation roomReservationRequest = getRoomReservationReq();
		
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
		when(acrsStrategy.updateRoomReservation(ArgumentMatchers.any())).thenReturn(getRoomReservationResponse());
		RoomReservation response = reservationDAOImpl.updateRoomReservation(roomReservationRequest);

		Assert.assertNotNull(response);
	}
	
	@Test
	public void makeRoomReservationTest() {
		
		RoomReservation roomReservationRequest = getRoomReservationReq();
		
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
		when(acrsStrategy.makeRoomReservation(ArgumentMatchers.any())).thenReturn(getRoomReservationResponse());
		RoomReservation response = reservationDAOImpl.makeRoomReservation(roomReservationRequest);

		Assert.assertNotNull(response);
	}
	
	@Test
	public void makeRoomReservationV2Test() {
		
		RoomReservation roomReservationRequest = getRoomReservationReq();
		
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
		when(acrsStrategy.makeRoomReservationV2(ArgumentMatchers.any())).thenReturn(getRoomReservationResponse());
		RoomReservation response = reservationDAOImpl.makeRoomReservationV2(roomReservationRequest);

		Assert.assertNotNull(response);
	}
	
	@Test
	public void saveRoomReservationTest() {
		
		RoomReservation roomReservationRequest = getRoomReservationReq();
		
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
		when(acrsStrategy.saveRoomReservation(ArgumentMatchers.any())).thenReturn(getRoomReservationResponse());
		RoomReservation response = reservationDAOImpl.saveRoomReservation(roomReservationRequest);

		Assert.assertNotNull(response);
	}
	
	@Test
	public void makePartyRoomReservationTest() {
		RoomReservation roomReservationRequest = getPartyRoomReservationReq();
		
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
		when(acrsStrategy.makePartyRoomReservation(ArgumentMatchers.any(),anyBoolean())).thenReturn(getPartyRoomReservationResponse());
		PartyRoomReservation response = reservationDAOImpl.makePartyRoomReservation(roomReservationRequest,false);

		Assert.assertNotNull(response);
	}
		
}
