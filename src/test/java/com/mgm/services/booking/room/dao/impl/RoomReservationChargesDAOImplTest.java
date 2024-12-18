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
public class RoomReservationChargesDAOImplTest extends BaseRoomBookingTest {
	
	@Mock
	private RoomReservationChargesDAOStrategyACRSImpl acrsStrategy;
	
	@Mock
	private ReferenceDataDAOHelper referenceDataDAOHelper;

	@InjectMocks
	private RoomReservationChargesDAOImpl roomReservationChargesDAOImpl  = new RoomReservationChargesDAOImpl();

	static Logger logger = LoggerFactory.getLogger(RoomAndComponentChargesDAOImplTest.class);

	private RoomReservation roomReservationChargesRequest() {
		File file = new File(getClass().getResource("/roomreservationchargesrequest-basic.json").getPath());
		RoomReservation data = convertRBSReq(file, RoomReservation.class);
		return data;
	}

	private RoomReservation calculateRoomReservationCharges() {
		File file = new File(getClass().getResource("/roomReservationCharges-response.json").getPath());
		RoomReservation data = convertRBSReq(file, RoomReservation.class);
		return data;
	}
	
	@Test
	public void calculateRoomReservationChargesTest() {

		try {
			
			RoomReservation roomReservationRequest = roomReservationChargesRequest();
			when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
			when(acrsStrategy.calculateRoomReservationCharges(ArgumentMatchers.any())).thenReturn(calculateRoomReservationCharges());

			RoomReservation roomreservationChargesResponse = roomReservationChargesDAOImpl.calculateRoomReservationCharges(roomReservationRequest);
			Assert.assertNotNull(roomreservationChargesResponse);

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("calculateRoomReservationChargesTest Failed");
			System.err.println(e.getMessage());
			logger.error(e.getMessage());
		}
	}
	
}
