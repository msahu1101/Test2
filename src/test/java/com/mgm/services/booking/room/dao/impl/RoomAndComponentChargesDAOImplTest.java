package com.mgm.services.booking.room.dao.impl;

import static org.mockito.Mockito.when;

import java.io.File;

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
import com.mgm.services.booking.room.model.reservation.RoomReservation;

@RunWith(MockitoJUnitRunner.class)
public class RoomAndComponentChargesDAOImplTest extends BaseRoomBookingTest {

	@Mock
	private RoomAndComponentChargesDAOStrategyACRSImpl acrsStrategy;

	@Mock
	private ReferenceDataDAOHelper referenceDataDAOHelper;

	@InjectMocks
	private RoomAndComponentChargesDAOImpl roomAndComponentChargesDAOImpl  = new RoomAndComponentChargesDAOImpl();

	static Logger logger = LoggerFactory.getLogger(RoomAndComponentChargesDAOImplTest.class);

	private RoomReservation roomReservationRequest() {
		File file = new File(getClass().getResource("/roomReservation-validRequest.json").getPath());
		RoomReservation data = convertRBSReq(file, RoomReservation.class);
		return data;
	}

	private RoomReservation roomAndComponentCharges() {
		File file = new File(getClass().getResource("/room-component-charges.json").getPath());
		RoomReservation data = convertRBSReq(file, RoomReservation.class);
		return data;
	}

	@Test
	public void calculateRoomAndComponentChargesTest() {

		try {
			
			RoomReservation roomReservationRequest = roomReservationRequest();
			when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
			when(acrsStrategy.calculateRoomAndComponentCharges(ArgumentMatchers.any())).thenReturn(roomAndComponentCharges());

			RoomReservation roomreservationResponse = roomAndComponentChargesDAOImpl.calculateRoomAndComponentCharges(roomReservationRequest);
			Assert.assertNotNull(roomreservationResponse);

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("calculateRoomAndComponentChargesTest Failed");
			System.err.println(e.getMessage());
			logger.error(e.getMessage());
		}
	}

}
