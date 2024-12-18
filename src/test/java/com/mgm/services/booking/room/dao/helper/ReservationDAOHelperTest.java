package com.mgm.services.booking.room.dao.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mgm.services.booking.room.model.PurchasedComponent;
import com.mgm.services.booking.room.service.cache.rediscache.service.PropertyPkgComponentCacheService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.mgm.services.booking.room.model.reservation.ReservationProfile;
import com.mgm.services.booking.room.model.reservation.RoomPrice;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.common.model.ProfilePhone;
import com.mgmresorts.aurora.messages.AddCustomerRequest;
import com.mgmresorts.aurora.messages.CreateCustomerItineraryRequest;

class ReservationDAOHelperTest {

	@Mock
	PropertyPkgComponentCacheService propertyPkgComponentCacheService;
	@Mock
	ReferenceDataDAOHelper referenceDataDAOHelper;

	@InjectMocks
	ReservationDAOHelper reservationDAOHelper;
		
	@BeforeEach
	void init() {
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	void testFindDominantProgram() {
		List<RoomPrice> bookings = new ArrayList<>();
		String programId1 = "RPCD-v-TFRSA-d-PROP-v-MV021";
		String programId2 = "GRPCD-v-GRP1-d-PROP-v-MV021";
		
		RoomPrice booking1 = new RoomPrice();
		booking1.setProgramId(programId1);
		booking1.setProgramIdIsRateTable(false);
		RoomPrice booking2 = new RoomPrice();
		booking2.setProgramId(programId2);
		booking2.setProgramIdIsRateTable(false);
		RoomPrice booking3 = new RoomPrice();
		booking3.setProgramId(programId1);
		booking3.setProgramIdIsRateTable(false);
		
		bookings.add(booking1);
		bookings.add(booking2);
		bookings.add(booking3);
		
		String dominantProgram = reservationDAOHelper.findDominantProgram(bookings);
		
		// Assertions
		assertNotNull(dominantProgram);
		assertEquals("RPCD-v-TFRSA-d-PROP-v-MV021", dominantProgram);
	}
	
	@Test
	void testPrepareAddCustomerRequest() {
		RoomReservation reservation = this.createRoomReservation();
		AddCustomerRequest addCustomerRequest = 
				reservationDAOHelper.prepareAddCustomerRequest(reservation);
		
		// Assertions
		assertNotNull(addCustomerRequest);
		assertNotNull(addCustomerRequest.getCustomer());
		assertEquals(1, addCustomerRequest.getCustomer().getPhoneNumbers().length);
		assertEquals("1234567892", addCustomerRequest.getCustomer().getPhoneNumbers()[0].getNumber());
	}
	
	@Test
	void testPrepareCreateCustomerItineraryRequest() {
		RoomReservation reservation = this.createRoomReservation();
		CreateCustomerItineraryRequest createCustomerItineraryRequest = 
				reservationDAOHelper.prepareCreateCustomerItineraryRequest(reservation, 1111);
		
		// Assertions
		assertNotNull(createCustomerItineraryRequest);
		assertNotNull(createCustomerItineraryRequest.getTripParams());
		assertEquals(1111, createCustomerItineraryRequest.getCustomerId());
		assertEquals(0, 
				createCustomerItineraryRequest.getTripParams().getArrivalDate()
				.compareTo(reservation.getCheckInDate())
			);
		assertEquals(0, 
				createCustomerItineraryRequest.getTripParams().getDepartureDate()
				.compareTo(reservation.getCheckOutDate())
			);
		assertEquals(reservation.getNumAdults(), createCustomerItineraryRequest.getTripParams().getNumAdults());
	}

	@Test
	void testCheckIfExistingPkgReservation() {
		Mockito.when(propertyPkgComponentCacheService.getPkgComponentCodeByPropertyId(Mockito.anyString()))
				.thenReturn(Arrays.asList("pkg1","pkg2","pkg3","pkg4","pkg5"));
		Mockito.when(referenceDataDAOHelper.retrieveGsePropertyID(Mockito.anyString()))
				.thenReturn("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
		List<PurchasedComponent> purchasedComponents = new ArrayList<>();
		PurchasedComponent component = new PurchasedComponent();
		component.setId("COMPONENTCD-v-pkg2-d-TYP-v-COMPONENT-d-PROP-v-MV021-d-NRPCD-v-CRIB");
		purchasedComponents.add(component);
		assertTrue(reservationDAOHelper.checkIfExistingPkgReservation("MV021", purchasedComponents));
	}
	
	private RoomReservation createRoomReservation() {
		String propertyId = "dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad";
		String programId = "RPCD-v-TFRSA-d-PROP-v-MV021";
		
		RoomReservation roomReservation = new RoomReservation();
		roomReservation.setPropertyId(propertyId);
		roomReservation.setRoomTypeId("roomTypeId");
		roomReservation.setProgramId(programId);
		List<RoomPrice> bookings = new ArrayList<>();
		RoomPrice booking = new RoomPrice();
		booking.setOverrideProgramId(programId);
		booking.setProgramId("programIdNotUuid");
		bookings.add(booking);
		roomReservation.setBookings(bookings);
		
		ReservationProfile profile = new ReservationProfile();
		List<ProfilePhone> phoneNumbers = new ArrayList<>();
		ProfilePhone number = new ProfilePhone();
		number.setNumber("1234567892");
		phoneNumbers.add(number);
		profile.setPhoneNumbers(phoneNumbers);
		roomReservation.setProfile(profile);
		
		Date checkInDate = new GregorianCalendar(2023, Calendar.NOVEMBER, 15).getTime();
		Date checkOutDate = new GregorianCalendar(2023, Calendar.NOVEMBER, 17).getTime();
		roomReservation.setCheckInDate(checkInDate);
		roomReservation.setCheckOutDate(checkOutDate);
		roomReservation.setNumAdults(2);
		
		return roomReservation;
	}
}
